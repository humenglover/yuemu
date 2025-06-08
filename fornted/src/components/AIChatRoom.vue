<template>
  <div class="ai-chat-room">
    <!-- 顶部思考状态 -->
    <div class="thinking-status" v-if="loading">
      <a-spin size="small" />
      <span>AI 思考中...</span>
    </div>

    <div class="chat-content">
      <!-- 消息列表区域 -->
      <div class="messages-wrapper">
        <div class="messages-container">
          <!-- 历史消息加载区域 -->
          <div class="header-actions">
            <template v-if="!hasMore && messages.length > 0 && isAtTop">
              <div class="first-message-tip">已经到顶啦</div>
            </template>
            <template v-else-if="hasMore && isAtTop">
              <a-button
                type="link"
                size="small"
                :loading="loadingHistory"
                @click="loadHistory"
                class="load-more-btn"
              >
                <template #icon><DownOutlined /></template>
                加载更多
              </a-button>
            </template>
          </div>

          <!-- 消息列表 -->
          <div class="chat-messages" ref="messageContainer" @scroll="handleScroll">
            <div v-if="loadingHistory" class="loading-history">
              <a-spin size="small" />
              <span>加载历史消息...</span>
            </div>

            <template v-for="(msg, index) in messages" :key="msg.id">
              <div v-if="shouldShowTimestamp(msg, messages[index - 1])" class="timestamp-divider">
                {{ formatMessageDivider(msg.createTime) }}
              </div>
              <div class="message" :class="{ 'message-self': !msg.isAI }">
                <div class="message-wrapper">
                  <a-avatar
                    :src="msg.isAI ? 'https://yuemu-picture-1328106169.cos.ap-chongqing.myqcloud.com/public/1866450683272450049/2025-03-14_ZKdnsKKV4Z3Rxvcx.webp' : loginUser?.userAvatar"
                    class="avatar"
                  />
                  <div class="message-content">
                    <div class="message-wrapper-content" v-if="msg.isAI">
                      <div class="message-text" v-html="formatMarkdown(msg.content)"></div>
                      <div class="message-actions">
                        <a-button
                          type="text"
                          class="copy-btn"
                          @click="copyMessage(msg.content)"
                        >
                          <template v-if="!msg.copied">
                            <CopyOutlined />
                            <span>复制</span>
                          </template>
                          <template v-else>
                            <CheckOutlined />
                            <span>已复制</span>
                          </template>
                        </a-button>
                      </div>
                    </div>
                    <div class="message-text" v-else>{{ msg.content }}</div>
                  </div>
                </div>
              </div>
            </template>
          </div>
        </div>
      </div>

      <!-- 输入区域 -->
      <div class="chat-input">
        <div class="input-area">
          <a-input-group compact class="input-group">
            <a-input
              v-model:value="inputMessage"
              placeholder="输入消息..."
              @keyup.enter.prevent="sendMessage"
              :disabled="loading"
              class="message-input"
              ref="messageInput"
              @blur="handleInputBlur"
            />
            <a-button
              type="primary"
              @click.prevent="sendMessage"
              :loading="loading"
              class="send-button"
            >
              发送
            </a-button>
          </a-input-group>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useLoginUserStore } from '@/stores/useLoginUserStore'
import { message } from 'ant-design-vue'
import dayjs from 'dayjs'
import { DownOutlined, CopyOutlined, CheckOutlined } from '@ant-design/icons-vue'
import { getChatHistoryUsingPost, sendUsingPost } from '@/api/aiChatController'
import MarkdownContent from './MarkdownContent.vue'

const loginUserStore = useLoginUserStore()
const loginUser = computed(() => loginUserStore.loginUser)

const messages = ref<any[]>([])
const inputMessage = ref('')
const messageContainer = ref<HTMLElement | null>(null)
const messageInput = ref<HTMLElement | null>(null)
const loading = ref(false)

// 分页相关
const current = ref(1)
const pageSize = ref(20)
const hasMore = ref(true)
const loadingHistory = ref(false)
const isAtTop = ref(false)

// 加载历史消息
const loadHistory = async () => {
  if (loadingHistory.value) return
  try {
    loadingHistory.value = true
    const res = await getChatHistoryUsingPost({
      current: current.value,
      pageSize: pageSize.value
    })

    if (res?.data) {
      // 处理分页数据
      const { records, total, pages } = res.data
      if (records && Array.isArray(records)) {
        const historyMessages = records.map(msg => ({
          id: Date.now() + Math.random(), // 由于后端没有返回id，这里生成一个
          content: msg.content,
          createTime: msg.createTime,
          isAI: msg.role === 'assistant'
        }))
        messages.value = [...historyMessages.reverse(), ...messages.value]
        hasMore.value = current.value < Number(pages)
        current.value++
      }
    }
  } catch (error) {
    // message.error('加载历史消息失败')
  } finally {
    loadingHistory.value = false
  }
}

// 发送消息
const sendMessage = async () => {
  if (!inputMessage.value.trim() || loading.value) return

  const userMessage = {
    id: Date.now(),
    content: inputMessage.value,
    createTime: dayjs().format('YYYY-MM-DD HH:mm:ss'),
    isAI: false
  }

  messages.value.push(userMessage)
  const userInput = inputMessage.value
  inputMessage.value = ''
  scrollToBottom()

  try {
    loading.value = true
    const res = await sendUsingPost({ query: userInput })

    // 处理消息上限的情况
    if (res?.data.code === 50001) {
      message.error({
        content: '今日消息已达上限(100条)，请明天再来',
        duration: 3
      })
      // 移除用户消息，因为没有成功发送
      messages.value.pop()
      return
    }

    if (res?.data) {
      const aiMessage = {
        id: Date.now() + 1,
        content: res.data,
        createTime: dayjs().format('YYYY-MM-DD HH:mm:ss'),
        isAI: true
      }
      messages.value.push(aiMessage)
      scrollToBottom()
    }
  } catch (error) {
    // 发送失败时也移除用户消息
    messages.value.pop()
    message.error('发送消息失败')
  } finally {
    loading.value = false
  }
}

// 其他辅助方法
const scrollToBottom = () => {
  if (messageContainer.value) {
    setTimeout(() => {
      messageContainer.value!.scrollTop = messageContainer.value!.scrollHeight
    }, 50)
  }
}

const handleScroll = (e: Event) => {
  const target = e.target as HTMLElement
  isAtTop.value = target.scrollTop <= 1

  if (target.scrollTop <= 30 && !loadingHistory.value && hasMore.value) {
    loadHistory()
  }
}

const handleInputBlur = (e: Event) => {
  const target = e.target as HTMLInputElement
  setTimeout(() => {
    target.focus()
  }, 100)
}

const shouldShowTimestamp = (currentMsg: any, prevMsg: any) => {
  if (!prevMsg) return true
  const currentTime = dayjs(currentMsg.createTime)
  const prevTime = dayjs(prevMsg.createTime)
  return currentTime.diff(prevTime, 'minute') >= 5
}

const formatMessageDivider = (time: string) => {
  const msgTime = dayjs(time)
  const now = dayjs()

  if (msgTime.isSame(now, 'day')) {
    return msgTime.format('HH:mm')
  }
  if (msgTime.isSame(now.subtract(1, 'day'), 'day')) {
    return '昨天 ' + msgTime.format('HH:mm')
  }
  if (msgTime.isSame(now.subtract(2, 'day'), 'day')) {
    return '前天 ' + msgTime.format('HH:mm')
  }
  if (msgTime.isSame(now, 'year')) {
    return msgTime.format('MM-DD HH:mm')
  }
  return msgTime.format('YYYY-MM-DD HH:mm')
}

// 格式化 Markdown 内容
const formatMarkdown = (content: string) => {
  if (!content) return ''

  return content
    // 移除多余的空行
    .replace(/\n\s*\n/g, '\n\n')
    // 处理标题 (### 这种格式)
    .replace(/^(#{1,6})\s+(.+)$/gm, (_, level, text) => {
      const fontSize = Math.max(18 - level.length * 2, 14)
      return `<div style="font-size: ${fontSize}px; font-weight: 600; margin: 16px 0 8px;">${text}</div>`
    })
    // 代码块
    .replace(/```(\w+)?\n([\s\S]+?)```/g, (_, lang, code) => {
      const language = lang || 'plaintext'
      return `<pre class="code-block"><div class="code-header"><span class="language">${language}</span></div><code>${code.trim()}</code></pre>`
    })
    // 行内代码
    .replace(/`([^`]+)`/g, '<code class="inline-code">$1</code>')
    // 列表项
    .replace(/^(\s*)[•*-]\s+(.+)$/gm, '<div style="padding: 4px 0;">$1• $2</div>')
    // 数字列表
    .replace(/^(\s*)\d+\.\s+(.+)$/gm, '<div style="padding: 4px 0;">$1$2</div>')
    // 引用块
    .replace(/^>\s+(.+)$/gm, '<div style="padding: 8px 16px; margin: 8px 0; border-left: 4px solid #e2e8f0; background: #f8fafc;">$1</div>')
    // 粗体
    .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
    // 斜体
    .replace(/\*(.*?)\*/g, '<em>$1</em>')
    // 链接
    .replace(/\[([^\]]+)\]\(([^)]+)\)/g, '<a href="$2" target="_blank">$1</a>')
    // 换行
    .replace(/\n/g, '<div style="margin: 8px 0;"></div>')
}

// 添加复制功能
const copyMessage = async (content: string) => {
  try {
    await navigator.clipboard.writeText(content)
    message.success('复制成功')
  } catch (err) {
    message.error('复制失败')
  }
}

onMounted(async () => {
  // 立即加载历史消息
  await loadHistory()
  // 滚动到底部
  scrollToBottom()
})
</script>

<style scoped>
.thinking-status {
  position: fixed;
  top: 20px;
  left: 50%;
  transform: translateX(-50%);
  background: rgba(114, 46, 209, 0.1);
  padding: 8px 16px;
  border-radius: 20px;
  display: flex;
  align-items: center;
  gap: 8px;
  color: #722ed1;
  font-size: 14px;
  z-index: 1000;
  backdrop-filter: blur(4px);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.loading-history {
  text-align: center;
  padding: 8px;
  color: #999;
  font-size: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.ai-chat-room {
  flex: 1;
  display: flex;
  flex-direction: column;
  height: 100%;
  max-width: 1000px;
  margin: 0 auto;
  width: 100%;
  padding: 0 8px;
  position: relative;
}

.chat-content {
  display: flex;
  flex-direction: column;
  height: 94%; /* 移动端默认高度 */
  background: rgba(255, 255, 255, 0.8);
  border-radius: 16px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
  backdrop-filter: blur(10px);
  overflow: hidden;
  margin-bottom: 20px;
}

/* PC端高度调整 */
@media screen and (min-width: 768px) {

}

.messages-wrapper {
  flex: 1;
  overflow: hidden;
  position: relative;
}

.messages-container {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
  margin-bottom: 0;
}

.message {
  margin-bottom: 16px;
  animation: slideIn 0.3s ease;
  padding: 0 4px;
}

.message-wrapper {
  display: flex;
  gap: 4px;
  align-items: flex-start;
  max-width: 80%;
  position: relative;
}

.avatar {
  flex-shrink: 0;
  width: 40px !important;
  height: 40px !important;
  border: 2px solid rgba(255, 255, 255, 0.8);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.message-self {
  display: flex;
  justify-content: flex-end;

  .message-wrapper {
    flex-direction: row-reverse;
  }

  .message-content {
    background: rgba(114, 46, 209, 0.1);
    border-color: rgba(114, 46, 209, 0.2);
  }

  .avatar {
    margin-right: 0;
    margin-left: 12px;
  }
}

.message-content {
  background: white;
  padding: 2px 16px;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  border: 1px solid rgba(0, 0, 0, 0.06);
  min-width: 200px;
  flex: 1;
  min-width: 0;
}

.message-text {
  white-space: pre-wrap;
  word-break: break-word;
  line-height: 1.6;
  font-size: 14px;
  color: #2c3e50;
  padding: 8px 12px;

  .code-block {
    margin: 8px 0;
    background: #282c34;
    border-radius: 8px;
    overflow: hidden;

    .code-header {
      padding: 8px 12px;
      background: rgba(255, 255, 255, 0.05);
      border-bottom: 1px solid rgba(255, 255, 255, 0.1);

      .language {
        color: #abb2bf;
        font-size: 12px;
        text-transform: uppercase;
      }
    }

    code {
      display: block;
      padding: 12px;
      color: #abb2bf;
      font-family: 'Fira Code', monospace;
      font-size: 13px;
      line-height: 1.5;
      overflow-x: auto;
      white-space: pre;
    }
  }

  .inline-code {
    background: rgba(0, 0, 0, 0.04);
    padding: 2px 6px;
    border-radius: 4px;
    font-family: 'Fira Code', monospace;
    font-size: 13px;
    color: #476582;
  }

  strong {
    font-weight: 600;
    color: #2c3e50;
  }

  em {
    font-style: italic;
    color: #476582;
  }

  a {
    color: #3b82f6;
    text-decoration: none;

    &:hover {
      text-decoration: underline;
    }
  }
}

.message-self {
  .message-content {
    background: rgba(114, 46, 209, 0.1);
    border-color: rgba(114, 46, 209, 0.2);

    .code-block {
      background: rgba(40, 44, 52, 0.8);
    }

    .inline-code {
      background: rgba(255, 255, 255, 0.2);
      color: #2c3e50;
    }
  }
}

.chat-input {
  padding: 16px;
  background: white;
  border-top: 1px solid rgba(0, 0, 0, 0.06);
  position: relative;
}

.input-area {
  max-width: 800px;
  margin: 0 auto;
  display: flex;
  align-items: center;
}

.input-group {
  width: 100%;
  display: flex !important;
  align-items: center;
}

.message-input {
  flex: 1;
  border-radius: 20px 0 0 20px !important;
  height: 30px !important;

  :deep(.ant-input) {
    height: 30px;
    padding: 4px 16px;
    border-radius: 20px 0 0 20px;
    border: 1px solid #d9d9d9;
    border-right: none;
    font-size: 14px;
    line-height: 30px;

    &:focus {
      border-color: #722ed1;
      box-shadow: 0 0 0 2px rgba(114, 46, 209, 0.1);
    }
  }
}

.send-button {
  width: 80px !important;
  height: 30px !important;
  border-radius: 0 20px 20px 0 !important;
  background: #722ed1 !important;
  border-color: #722ed1 !important;
  font-size: 14px;
  display: flex;
  align-items: center;
  justify-content: center;

  &:hover {
    background: #531dab !important;
    border-color: #531dab !important;
  }
}

/* 时间分割线样式 */
.timestamp-divider {
  text-align: center;
  margin: 16px 0;
  color: #999;
  font-size: 12px;
}

/* 加载更多按钮样式 */
.load-more-btn {
  color: #722ed1;

  &:hover {
    color: #531dab;
  }
}

.first-message-tip {
  color: #999;
  font-size: 12px;
  text-align: center;
}

/* 移动端适配 */
@media screen and (max-width: 768px) {
  .ai-chat-room {
    padding: 0 8px;
  }

  .chat-content {
    margin-bottom: 10px;
  }

  .chat-input {
    padding: 10px;
  }

  .send-button {
    width: 60px !important;
    height: 30px !important;
  }

  .message-wrapper {
    max-width: 95%;
    gap: 8px;
  }

  .message {
    padding: 0 4px;
  }

  .avatar {
    width: 32px !important;
    height: 32px !important;
  }

  .message-content {
    flex: 1;
    min-width: 0;
  }

  .message-text {
    padding: 8px 12px;
  }

  .message-actions {
    padding-top: 6px;
    margin-top: 6px;
  }

  .message-self .message-wrapper {
    justify-content: flex-end;
    margin-left: auto;
  }
}

@keyframes slideIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.message-wrapper-content {
  position: relative;
}

.message-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 6px;
  padding-top: 6px;
  border-top: 1px solid rgba(0, 0, 0, 0.06);
}

.copy-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  color: #94a3b8;
  font-size: 13px;
  padding: 4px 8px;
  border-radius: 4px;
  transition: all 0.2s ease;

  &:hover {
    color: #3b82f6;
    background: rgba(59, 130, 246, 0.1);
  }

  .anticon {
    font-size: 14px;
  }
}

.message-self {
  .message-actions {
    border-top-color: rgba(114, 46, 209, 0.1);
  }

  .copy-btn {
    color: rgba(114, 46, 209, 0.6);

    &:hover {
      color: #722ed1;
      background: rgba(114, 46, 209, 0.1);
    }
  }
}
</style>
