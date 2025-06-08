<template>
  <div class="chat-room">
    <div class="chat-content">
      <div class="messages-container">
        <!-- 顶部提示 -->
        <div class="header-actions">
          <template v-if="!hasMore && messages.length > 0 && isAtTop">
            <div class="first-message-tip">已经到顶啦</div>
          </template>
          <template v-else-if="hasMore && isAtTop">
            <button
              class="load-more-button"
              :class="{ loading: loadingHistory }"
              @click="loadHistory"
            >
              <span class="icon">↓</span>
              加载更多
            </button>
          </template>
        </div>

        <div class="chat-messages" ref="messageContainer" @scroll="handleScroll">
          <!-- 添加空状态提示 -->
          <div v-if="messages.length === 0" class="empty-state">
            <div class="empty-icon">💭</div>
            <div class="empty-text">暂无消息，快来开始聊天吧~</div>
          </div>
          <template v-else v-for="(msg, index) in messages" :key="msg.id">
            <div v-if="shouldShowTimestamp(msg, messages[index - 1])" class="timestamp-divider">
              {{ formatMessageDivider(msg.createTime) }}
            </div>
            <div class="message"
                 :class="{ 'message-self': msg.sender?.id === loginUser?.id }"
            >
              <div class="message-wrapper">
                <div
                  class="user-avatar"
                  @click="handleAvatarClick(msg.sender)"
                  :style="{ backgroundImage: `url(${msg.sender?.userAvatar})` }"
                ></div>
                <div class="message-content">
                  <div class="message-header" v-if="props.type !== 'private'">
                    <span class="sender-name">{{ msg.sender?.userName }}</span>
                  </div>
                  <div class="message-text" @click="replyToMessage(msg)">{{ msg.content }}</div>
                  <!-- 回复消息显示 -->
                  <div v-if="msg.replyMessage" class="reply-content">
                    <span class="reply-text">
                      <template v-if="props.type !== 'private'">
                        回复 {{ msg.replyMessage.sender?.userName }}:
                      </template>
                      <template v-else>回复:</template>
                    </span>
                    {{ msg.replyMessage.content }}
                  </div>
                </div>
              </div>
            </div>
          </template>
        </div>

        <div class="chat-input">
          <!-- 显示当前回复的消息 -->
          <div v-if="replyTo" class="reply-preview">
            <span>回复 <span v-if="props.type !== 'private'">{{ replyTo.sender?.userName }}</span>: "{{ replyTo.content }}"</span>
            <button class="cancel-reply-button" @click="cancelReply">取消回复</button>
          </div>

          <div class="input-area">
            <button
              class="emoji-trigger"
              :class="{ active: showEmojiPicker }"
              @click="toggleEmojiPicker"
            >
              😊
            </button>

            <div class="input-group">
              <input
                v-model="inputMessage"
                type="text"
                class="message-input"
                placeholder="输入消息..."
                @keyup.enter.prevent="sendMessage"
                :disabled="!connected"
                ref="messageInput"
                @blur="handleInputBlur"
              />
              <button
                class="send-button"
                @click.prevent="sendMessage"
                :disabled="!connected"
              >
                发送
              </button>
            </div>
          </div>

          <!-- 表情选择器 -->
          <div v-if="showEmojiPicker" class="emoji-picker-container">
            <EmojiPicker @select="onEmojiSelect" />
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed, nextTick } from 'vue'
import { useLoginUserStore } from '@/stores/useLoginUserStore'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import dayjs from 'dayjs'
import ChatWebSocket from '@/utils/chatWebSocket'
import { getDefaultAvatar } from '@/utils/userUtils'

import type { ChatMessage } from '@/types/chat'
import { SmileOutlined, DownOutlined } from '@ant-design/icons-vue'
import EmojiPicker from './EmojiPicker.vue'

const props = defineProps<{
  pictureId?: number | string
  spaceId?: number | string
  privateChatId?: number | string
  type?: 'chat' | 'space' | 'private'
}>()

const loginUserStore = useLoginUserStore()
const loginUser = computed(() => loginUserStore.loginUser)

const chatWs = ref<ChatWebSocket | null>(null)
const connected = ref(false)
const messages = ref<ChatMessage[]>([])
const inputMessage = ref('')
const messageContainer = ref<HTMLElement | null>(null)
const messageInput = ref<HTMLElement | null>(null)

// 分页相关
const current = ref(1)
const pageSize = ref(20)
const hasMore = ref(true)
const loadingHistory = ref(false)

// 回复相关
const replyTo = ref<ChatMessage | null>(null)

// 表情相关
const showEmojiPicker = ref(false)

// 添加是否在顶部的状态
const isAtTop = ref(false)

// 定义 emit
const emit = defineEmits<{
  (e: 'update:onlineUsers', users: any[]): void
  (e: 'update:onlineCount', count: number): void
  (e: 'message', msg: any): void
}>()

// 添加在线用户相关的状态
const onlineCount = ref(0)
const onlineUsers = ref<any[]>([])

const router = useRouter()

// 切换表情选择器显示状态
const toggleEmojiPicker = () => {
  showEmojiPicker.value = !showEmojiPicker.value
}

// 选择表情
const onEmojiSelect = (emoji: string) => {
  inputMessage.value += emoji
  showEmojiPicker.value = false
}

// 加载历史消息
const loadHistory = async () => {
  if (loadingHistory.value) return
  try {
    loadingHistory.value = true
    const params = {
      ...(props.pictureId ? { pictureId: String(props.pictureId) } : {}),
      ...(props.spaceId ? { spaceId: String(props.spaceId) } : {}),
      ...(props.privateChatId ? { privateChatId: String(props.privateChatId) } : {}),
      page: current.value
    }
    chatWs.value?.sendMessage({
      type: 'loadMore',
      ...params
    })
  } finally {
    loadingHistory.value = false
  }
}

// 设置要回复的消息
const replyToMessage = (msg: ChatMessage) => {
  if (msg && msg.id) {
    replyTo.value = {
      id: String(msg.id),
      content: msg.content,
      sender: {
        id: String(msg.sender?.id),
        userName: msg.sender?.userName || '',
        userAvatar: msg.sender?.userAvatar || getDefaultAvatar(msg.sender?.userName)
      }
    }
  }
}

// 取消回复
const cancelReply = () => {
  replyTo.value = null
}

// 初始化WebSocket连接
const initWebSocket = () => {
  if (!loginUser.value) {
    message.error('请先登录')
    return
  }

  chatWs.value = new ChatWebSocket({
    pictureId: props.pictureId,
    spaceId: props.spaceId,
    privateChatId: props.privateChatId,
    type: props.type || 'chat'
  })

  // console.log('chatWs.value:', chatWs.value)

  // 注册事件处理器
  chatWs.value.on('open', () => {
    connected.value = true
    // message.success('聊天室连接成功')
    // 连接成功后，后端会自动发送历史消息
  })

  chatWs.value.on('message', (msg) => {
    if (msg.type === 'history') {
      // 处理初始历史消息
      messages.value = msg.messages
        .filter(m => m.content?.trim())
        .map(m => ({
          ...m,
          sender: m.sender ? {
            ...m.sender,
            userAvatar: m.sender.userAvatar || getDefaultAvatar(m.sender.userName)
          } : null
        }))
        .reverse()
      hasMore.value = msg.messages.length === pageSize.value
      current.value++
      loadingHistory.value = false
      scrollToBottom()
    } else if (msg.type === 'moreHistory') {
      // 处理加载更多的历史消息
      const oldScrollHeight = messageContainer.value?.scrollHeight || 0
      const oldScrollTop = messageContainer.value?.scrollTop || 0

      messages.value = [...msg.messages
        .filter(m => m.content?.trim())
        .map(m => ({
          ...m,
          sender: m.sender ? {
            ...m.sender,
            userAvatar: m.sender.userAvatar || getDefaultAvatar(m.sender.userName)
          } : null
        }))
        .reverse(), ...messages.value]
      hasMore.value = msg.hasMore
      current.value++
      loadingHistory.value = false

      // 恢复滚动位置
      nextTick(() => {
        if (messageContainer.value) {
          const newScrollHeight = messageContainer.value.scrollHeight
          const heightDiff = newScrollHeight - oldScrollHeight
          messageContainer.value.scrollTop = oldScrollTop + heightDiff
        }
      })
    } else if (msg.type === 'onlineUsers') {
      // 发送在线用户信息到父组件
      emit('message', msg)
    } else {
      // 处理普通消息
      if (!messages.value.some(m => m.id === msg.id) && msg.content?.trim()) {
        const newMessage = {
          ...msg,
          sender: msg.sender ? {
            ...msg.sender,
            userAvatar: msg.sender.userAvatar || getDefaultAvatar(msg.sender.userName)
          } : null,
          createTime: msg.createTime || dayjs().format('YYYY-MM-DD HH:mm:ss')
        }
        messages.value.push(newMessage)
        scrollToBottom()
      }
    }
  })

  chatWs.value.on('close', () => {
    connected.value = false
    // message.warning('聊天室连接已断开')
  })

  chatWs.value.on('error', () => {
    connected.value = false
    // message.error('聊天室连接失败')
    loadingHistory.value = false
  })

  // 建立连接
  chatWs.value.connect()
}

// 处理输入框失焦
const handleInputBlur = (e: Event) => {
  // 防止输入框失去焦点
  const target = e.target as HTMLInputElement
  setTimeout(() => {
    target.focus()
  }, 100)
}

// 修改发送消息函数
const sendMessage = () => {
  if (!inputMessage.value.trim() || !chatWs.value || !connected.value) {
    return
  }

  const chatMessage = {
    type: props.type === 'private' ? 1 : (props.type === 'space' ? 3 : 2),
    content: inputMessage.value.trim(),
    ...(props.pictureId ? { pictureId: props.pictureId.toString() } : {}),
    ...(props.spaceId ? { spaceId: props.spaceId.toString() } : {}),
    ...(props.privateChatId ? { privateChatId: props.privateChatId.toString() } : {}),
    senderId: loginUser.value?.id.toString(),
    createTime: dayjs().format('YYYY-MM-DD HH:mm:ss'),
    replyId: replyTo.value?.id ? replyTo.value.id.toString() : null,
    rootId: replyTo.value?.rootId ? replyTo.value.rootId.toString() :
      replyTo.value?.id ? replyTo.value.id.toString() : null
  }

  const success = chatWs.value.sendMessage(chatMessage)
  if (success) {
    inputMessage.value = ''
    replyTo.value = null
    // 保持输入框焦点
    if (messageInput.value) {
      (messageInput.value as HTMLInputElement).focus()
    }
  } else {
    message.error('发送失败，请重试')
  }
}

// 滚动到底部
const scrollToBottom = () => {
  if (messageContainer.value) {
    setTimeout(() => {
      messageContainer.value!.scrollTop = messageContainer.value!.scrollHeight
    }, 50)
  }
}

// 格式化时间
const formatTime = (time: string | undefined) => {
  if (!time) {
    return ''
  }
  const date = dayjs(time)
  return date.isValid() ? date.format('YY-MM-DD') : ''
}

// 修改滚动处理函数
const handleScroll = (e: Event) => {
  const target = e.target as HTMLElement
  // 判断是否在顶部，允许有1px的误差
  isAtTop.value = target.scrollTop <= 1

  // 当滚动到接近顶部时自动加载更多
  if (target.scrollTop <= 30 && !loadingHistory.value && hasMore.value) {
    loadHistory()
  }
}

// 添加判断是否显示时间分割线的方法
const shouldShowTimestamp = (currentMsg: any, prevMsg: any) => {
  if (!prevMsg) return true

  const currentTime = dayjs(currentMsg.createTime)
  const prevTime = dayjs(prevMsg.createTime)

  // 如果时间差超过5分钟，显示时间分割线
  return currentTime.diff(prevTime, 'minute') >= 5
}

// 添加格式化时间分割线文本的方法
const formatMessageDivider = (time: string) => {
  const msgTime = dayjs(time)
  const now = dayjs()

  // 如果是今天的消息
  if (msgTime.isSame(now, 'day')) {
    return msgTime.format('HH:mm')
  }

  // 如果是昨天的消息
  if (msgTime.isSame(now.subtract(1, 'day'), 'day')) {
    return '昨天 ' + msgTime.format('HH:mm')
  }

  // 如果是前天的消息
  if (msgTime.isSame(now.subtract(2, 'day'), 'day')) {
    return '前天 ' + msgTime.format('HH:mm')
  }

  // 如果是今年的消息
  if (msgTime.isSame(now, 'year')) {
    return msgTime.format('MM-DD HH:mm')
  }

  // 其他情况显示完整日期
  return msgTime.format('YYYY-MM-DD HH:mm')
}

// 处理头像点击
const handleAvatarClick = (user: any) => {
  if (!user?.id) return
  router.push({
    path: `/user/${user.id}`,
    query: {
      userName: user.userName,
      userAvatar: user.userAvatar || getDefaultAvatar(user.userName),
      userAccount: user.userAccount,
      createTime: user.createTime
    }
  })
}

// 暴露方法给父组件
defineExpose({
  initWebSocket,
  disconnect: () => {
    if (chatWs.value) {
      chatWs.value.disconnect()
    }
  }
})

onMounted(() => {
  initWebSocket()
})

onUnmounted(() => {
  if (chatWs.value) {
    chatWs.value.disconnect()
  }
  emit('update:onlineCount', 0)
  emit('update:onlineUsers', [])
})
</script>

<style scoped>
.chat-room {
  display: flex;
  flex-direction: column;
  height: 100%;
  flex: 1;
  overflow: hidden;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 16px;
}

.chat-content {
  display: flex;
  flex-direction: column;
  height: 100%;
  padding: 12px;
  overflow: hidden;
}

.messages-container {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: transparent;
  position: relative;
  overflow: hidden;
  gap: 12px;
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  background: rgba(255, 255, 255, 0.08);
  border-radius: 16px;
  position: relative;
  z-index: 0;
  border: 1px solid rgba(255, 255, 255, 0.1);
}

.message {
  display: flex;
  flex-direction: column;
  margin-bottom: 16px;
  animation: slideIn 0.3s ease;
  width: 100%;
}

.message-wrapper {
  display: flex;
  gap: 12px;
  align-items: flex-start;
  width: 100%;
}

.message-content {
  flex: 1;
  background: rgba(255, 255, 255, 0.98);
  padding: 12px 16px;
  border-radius: 16px;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.03);
  position: relative;
  transition: transform 0.2s ease;
  border: 1px solid rgba(0, 0, 0, 0.05);
  width: calc(100% - 48px);
}

.message-header {
  display: flex;
  margin-bottom: 8px;
  align-items: center;
}

.sender-name {
  font-weight: 600;
  color: #333;
  font-size: 14px;
  text-shadow: none;
}

.message-text {
  word-break: break-all;
  cursor: pointer;
  line-height: 1.5;
  color: #333;
  transition: background-color 0.2s ease;
  text-align: left;
}

.message-text:hover {
  background: rgba(255, 255, 255, 0.1);
  border-radius: 4px;
  padding: 2px 4px;
  margin: -2px -4px;
}

.reply-content {
  margin-top: 12px;
  padding: 10px;
  background: rgba(255, 255, 255, 0.95);
  border-radius: 8px;
  font-size: 13px;
  border-left: 3px solid #ff8e53;
  transition: all 0.3s ease;
  color: #333;
  text-align: left;
}

.reply-text {
  color: #ff8e53;
  margin-right: 6px;
  font-weight: 500;
}

.chat-input {
  padding: 16px;
  position: relative;
  background: rgba(255, 255, 255, 0.08);
  border-radius: 16px;
  box-shadow: 0 -2px 10px rgba(0, 0, 0, 0.05);
  margin-top: 0;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  width: 100%;
  max-width: 100%;
  box-sizing: border-box;
  border: 1px solid rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(8px);
}

.reply-preview {
  margin-bottom: 12px;
  padding: 8px 16px;
  border-radius: 12px;
  background: rgba(255, 142, 83, 0.15);
  border-left: 3px solid #ff8e53;
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.9);
  width: 100%;
}

.input-area {
  position: relative;
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
  max-width: 100%;
  box-sizing: border-box;
}

.emoji-trigger {
  background: none;
  border: none;
  font-size: 20px;
  padding: 8px;
  cursor: pointer;
  transition: all 0.3s ease;
  border-radius: 50%;
  color: rgba(255, 255, 255, 0.8);
}

.emoji-trigger:hover,
.emoji-trigger.active {
  background: rgba(255, 142, 83, 0.2);
  transform: scale(1.1);
  color: #ff8e53;
}

.input-group {
  display: flex;
  flex: 1;
  background: rgba(0, 0, 0, 0.2);
  padding: 4px;
  border-radius: 16px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  width: 100%;
  max-width: 100%;
  box-sizing: border-box;
}

.message-input {
  flex: 1;
  padding: 12px 4px;
  border-radius: 12px;
  border: none;
  background: rgba(255, 255, 255, 0.05);
  color: #fff;
  font-size: 14px;
  transition: all 0.3s ease;
  width: 100%;
  min-width: 0; /* 防止输入框溢出 */
  box-sizing: border-box;
}

.message-input:focus {
  outline: none;
  background: rgba(255, 255, 255, 0.1);
}

.message-input::placeholder {
  color: rgba(255, 255, 255, 0.5);
}

.send-button {
  padding: 8px 24px;
  border-radius: 12px;
  border: none;
  background: linear-gradient(135deg, #ff8e53 0%, #ff6b6b 100%);
  color: white;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
  min-width: 100px;
  font-size: 14px;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.1);
}

.send-button:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(255, 142, 83, 0.25);
}

.send-button:active {
  transform: translateY(0);
}

.send-button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  background: rgba(255, 255, 255, 0.1);
}

.cancel-reply-button {
  padding: 4px 12px;
  border-radius: 12px;
  border: none;
  background: rgba(255, 255, 255, 0.1);
  color: rgba(255, 255, 255, 0.9);
  font-size: 12px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.cancel-reply-button:hover {
  background: rgba(255, 255, 255, 0.2);
}

.user-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background-size: cover;
  background-position: center;
  cursor: pointer;
  transition: all 0.3s ease;
  border: 2px solid rgba(255, 255, 255, 0.2);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.user-avatar:hover {
  transform: scale(1.1);
  border-color: #ff8e53;
  box-shadow: 0 4px 12px rgba(255, 142, 83, 0.2);
}

/* 自己发送的消息样式 */
.message-self {
  align-items: flex-end;
}

.message-self .message-wrapper {
  flex-direction: row-reverse;
}

.message-self .message-content {
  background: #ff8e53;
  border-color: rgba(255, 142, 83, 0.1);
  box-shadow: 0 2px 6px rgba(255, 142, 83, 0.1);
}

.message-self .message-header {
  flex-direction: row-reverse;
}

.message-self .sender-name {
  color: #fff;
}

.message-self .message-text {
  color: #fff;
  text-align: left;
}

.message-self .reply-content {
  text-align: left;
  background: rgba(255, 255, 255, 0.95);
  color: #333;
}

/* 时间分割线样式 */
.timestamp-divider {
  text-align: center;
  margin: 16px 0;
  color: rgba(255, 142, 83, 0.6) !important;
  font-size: 12px;
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
}

.timestamp-divider::before,
.timestamp-divider::after {
  content: '';
  flex: 1;
  height: 1px;
  background: rgba(255, 142, 83, 0.1);
}

/* 移动端适配 */
@media screen and (max-width: 768px) {
  .chat-room {
    border-radius: 0;
    background: transparent;
  }

  .chat-content {
    padding: 8px;
  }

  .chat-messages {
    padding: 12px;
    background: rgba(255, 255, 255, 0.02);
    backdrop-filter: none;
    border: none;
  }

  .message {
    margin-bottom: 12px;
    animation: none;
  }

  .message-content {
    padding: 10px 14px;
    backdrop-filter: none;
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
  }

  .message-self .message-content {
    box-shadow: 0 1px 3px rgba(255, 142, 83, 0.1);
  }

  .chat-input {
    padding: 12px;
    background: rgba(255, 255, 255, 0.95);
    backdrop-filter: none;
    border: none;
    box-shadow: 0 -1px 3px rgba(0, 0, 0, 0.05);
    width: 100%;
    margin: 0;
  }

  .input-area {
    gap: 8px;
  }

  .input-group {
    background: rgba(0, 0, 0, 0.05);
    border: none;
  }

  .message-input {
    background: transparent;
    color: #333;
    padding: 10px 4px;
  }

  .send-button {
    padding: 8px 16px;
    min-width: 70px;
  }

  .reply-preview {
    background: rgba(255, 142, 83, 0.1);
    color: #333;
  }

  .emoji-trigger {
    color: rgba(0, 0, 0, 0.6);
  }

  .emoji-trigger:hover,
  .emoji-trigger.active {
    background: rgba(255, 142, 83, 0.1);
    color: #ff8e53;
  }

  .user-avatar {
    width: 32px;
    height: 32px;
    border: 1px solid rgba(0, 0, 0, 0.1);
    box-shadow: none;
  }

  .user-avatar:hover {
    transform: none;
    border-color: #ff8e53;
    box-shadow: none;
  }

  .timestamp-divider {
    margin: 12px 0;
    color: rgba(255, 142, 83, 0.6)!important;
  }

  .timestamp-divider::before,
  .timestamp-divider::after {
    background: rgba(255, 142, 83, 0.6)!important;
  }

  /* 优化滚动性能 */
  .chat-messages {
    -webkit-overflow-scrolling: touch;
    overscroll-behavior: contain;
    will-change: transform;
  }

  /* 禁用不必要的动画 */
  * {
    animation: none !important;
    transition: none !important;
  }

  /* 优化渲染性能 */
  .message-wrapper,
  .message-content,
  .chat-input {
    transform: translateZ(0);
    backface-visibility: hidden;
    perspective: 1000px;
  }
}

/* 小屏幕设备的额外优化 */
@media screen and (max-width: 480px) {
  .chat-input {
    padding: 8px;
  }

  .input-area {
    gap: 6px;
  }

  .emoji-trigger {
    padding: 6px;
    font-size: 18px;
  }

  .send-button {
    padding: 8px 12px;
    min-width: 60px;
    font-size: 13px;
  }
}

/* 优化表情选择器在移动端的性能 */
@media screen and (max-width: 768px) {
  .emoji-picker-container {
    position: fixed;
    left: 0;
    right: 0;
    bottom: 0;
    margin: 0;
    border-radius: 16px 16px 0 0;
    background: rgba(255, 255, 255, 0.98);
    backdrop-filter: none;
    border-bottom: none;
    animation: none;
    box-shadow: 0 -2px 20px rgba(0, 0, 0, 0.1);
  }
}

/* 滚动条美化 */
.chat-messages::-webkit-scrollbar {
  width: 4px;
}

.chat-messages::-webkit-scrollbar-track {
  background: transparent;
}

.chat-messages::-webkit-scrollbar-thumb {
  background: rgba(255, 255, 255, 0.2);
  border-radius: 2px;
}

.chat-messages::-webkit-scrollbar-thumb:hover {
  background: rgba(255, 255, 255, 0.3);
}

/* 加载更多按钮样式 */
.load-more-button {
  padding: 8px 20px;
  border-radius: 20px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  background: rgba(255, 255, 255, 0.08);
  color: rgba(255, 255, 255, 0.9);
  font-size: 13px;
  cursor: pointer;
  transition: all 0.3s ease;
  display: flex;
  align-items: center;
  gap: 8px;
  backdrop-filter: blur(8px);
}

.load-more-button:hover {
  background: rgba(255, 255, 255, 0.12);
  transform: translateY(-1px);
}

.load-more-button.loading {
  opacity: 0.7;
  cursor: wait;
}

.first-message-tip {
  color: rgba(255, 255, 255, 0.6);
  font-size: 12px;
  text-align: center;
  padding: 8px;
}

/* 表情选择器容器样式 */
.emoji-picker-container {
  position: absolute;
  bottom: 100%;
  left: 0;
  margin-bottom: 8px;
  z-index: 1000;
  background: rgba(0, 0, 0, 0.8);
  border-radius: 12px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(12px);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.2);
  animation: slideUp 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

@keyframes slideUp {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* 添加空状态样式 */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: rgba(255, 255, 255, 0.6);
  gap: 16px;
}

.empty-icon {
  font-size: 48px;
  animation: float 3s ease-in-out infinite;
}

.empty-text {
  font-size: 14px;
}

@keyframes float {
  0%, 100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(-10px);
  }
}

/* 移动端适配空状态样式 */
@media screen and (max-width: 768px) {
  .empty-state {
    color: rgba(255, 255, 255, 0.8);
  }

  .empty-icon {
    font-size: 40px;
  }

  .empty-text {
    font-size: 13px;
  }
}
</style>
