<template>
  <div class="chat-room">
    <div class="chat-content">
      <div class="messages-container">
        <!-- 顶部加载指示器 -->
        <div v-if="loadingHistory && !initialLoading" class="top-loading-indicator">
          <a-spin size="small" />
          <span>正在加载历史消息...</span>
        </div>

        <!-- 消息列表 -->
        <div class="chat-messages" ref="messageContainer" @scroll.passive="handleScroll">
          <!-- 加载状态 -->
          <div v-if="initialLoading" class="loading-state">
            <a-spin size="large" :indicator="spinnerIcon" />
          </div>

          <!-- 空状态 -->
          <div v-else-if="!messages.length" class="empty-state">
            <div class="empty-icon">💭</div>
            <p>暂无消息，快来开始聊天吧~</p>
          </div>

          <!-- 消息列表 -->
          <template v-else>
            <!-- 已经到顶提示 -->
            <div v-if=" !loadingHistory && !checkingMore" class="system-message">
              <div class="no-more-message">
                <span>已经到顶了</span>
                <a-button
                  type="link"
                  size="small"
                  :loading="manualLoadingHistory"
                  @click="manualLoadHistory"
                  class="reload-btn"
                >
                  <template #icon><ReloadOutlined /></template>
                  重新加载
                </a-button>
              </div>
            </div>

            <template v-for="(msg, index) in messages" :key="msg.id">
              <!-- 时间分割线 -->
              <div v-if="shouldShowTimestamp(msg, messages[index - 1])" class="timestamp-divider">
                {{ formatMessageDivider(msg.createTime) }}
              </div>

              <!-- 消息项 -->
              <div
                class="message-item"
                :class="{
                  'message-self': msg.sender?.id === loginUser?.id,
                  'message-system': msg.senderId === '0',
                  'message-recalled': msg.content === '消息已撤回',
                  'message-highlight': msg.id === selectedMessage?.id
                }"
                :data-message-id="msg.id"
              >
                <!-- 头像 -->
                <div
                  v-if="msg.content !== '消息已撤回'"
                  class="message-avatar"
                  :style="{ backgroundImage: msg.senderId === '0' ?
                    'url(https://static.yuemutuku.com/space/1877028360151445505/2025-07-02_k9wqCp9awJ9iPEaC_thumbnail.webp)' :
                    `url(${msg.sender?.userAvatar})`
                  }"
                  @click="handleAvatarClick(msg.senderId === '0' ? null : msg.sender)"
                />

                <!-- 内容区 -->
                <div class="message-body">
                  <!-- 用户名 -->
                  <div v-if="msg.content !== '消息已撤回'" class="message-username">
                    {{ msg.senderId === '0' ? '悦木小助手' : msg.sender?.userName || '用户' }}
                    <span v-if="msg.senderId !== '0'" class="location">{{ msg.messageLocation || '未知位置' }}</span>
                  </div>

                  <!-- 消息内容 -->
                  <div class="message-main" @click="showMessageActions(msg, $event)">
                    <!-- 文本 -->
                    <div v-if="msg.messageType === 'text' || !msg.messageType" class="text-bubble" v-html="msg.content === '消息已撤回' ? `${msg.senderId === '0' ? '悦木小助手' : msg.sender?.userName || '用户'}撤回了一条消息` : (msg.senderId === '0' ? parseMarkdown(msg.content) : msg.content)"></div>

                    <!-- 图片 -->
                    <div v-else-if="msg.messageType === 'image'" class="image-bubble">
                      <div v-if="msg.content === '消息已撤回'" class="text-bubble">
                        {{ msg.senderId === '0' ? '悦木小助手' : msg.sender?.userName || '用户' }}撤回了一条消息
                      </div>
                      <Image
                        v-else
                        :src="msg.messageUrl"
                        :alt="msg.content || '图片'"
                        class="message-image"
                        :preview="{ src: msg.messageUrl, onVisibleChange: handlePreviewVisibleChange }"
                        @load="handleImageLoaded"
                        @error="handleImageLoaded"
                        @mounted="handleImageStartLoading"
                      />
                    </div>

                    <!-- 音频 -->
                    <div v-else-if="msg.messageType === 'audio'" class="audio-bubble">
                      <div v-if="msg.content === '消息已撤回'" class="text-bubble">
                        {{ msg.senderId === '0' ? '悦木小助手' : msg.sender?.userName || '用户' }}撤回了一条消息
                      </div>
                      <AudioBubble
                        v-else
                        :url="msg.messageUrl"
                        :is-self="msg.sender?.id === loginUser?.id"
                      />
                    </div>

                    <!-- 视频 -->
                    <div v-else-if="msg.messageType === 'video'" class="video-bubble">
                      <video controls :src="msg.messageUrl" @play="handleVideoPlay" />
                    </div>

                    <!-- 系统消息内容 -->
                    <div v-else-if="msg.senderId === '0'" class="system-content" v-html="parseMarkdown(msg.content)" />

                    <!-- 操作按钮 -->
                    <div v-if="selectedMessage?.id === msg.id && !msg.isRecalled && selectedMessage?.senderId !== '0'" class="message-actions">
                      <button type="button" class="action-btn" @click.stop="handleReply(msg)">
                        回复
                      </button>
                      <button type="button" class="action-btn" @click.stop="handleCopy(msg)">
                        复制
                      </button>
                      <button type="button" class="action-btn" @click.stop="handleTranslate(msg)">
                        翻译
                      </button>
                      <button type="button" class="action-btn" @click.stop="handleSearch(msg)">
                        搜索
                      </button>
                      <button
                        v-if="canRecallMessage(msg)"
                        type="button"
                        class="action-btn"
                        @click.stop="handleRecall(msg)"
                      >
                        撤回
                      </button>
                      <button type="button" class="action-btn" @click="() => openReportModal(selectedMessage?.senderId)" >
                        举报
                      </button>
                    </div>
                  </div>

                  <!-- 回复引用 -->
                  <div v-if="msg.replyMessage" class="reply-quote" @click="scrollToMessage(msg.replyMessage.id)">
                    <span class="reply-label">
                      回复{{ props.type !== 'private' ? ` ${msg.replyMessage.senderId === '0' ? '悦木小助手' : msg.replyMessage.sender?.userName || '用户'}` : '' }}:
                    </span>
                    <span v-html="msg.replyMessage.senderId === '0' ? parseMarkdown(msg.replyMessage.content) : msg.replyMessage.content"></span>
                  </div>
                </div>
              </div>
            </template>
          </template>
        </div>

        <!-- 新消息提示 -->
        <div v-if="scrollState.hasNewMessage" class="new-message-tip" @click="scrollToNewMessage">
          <DownOutlined />
          <span>{{ scrollState.unreadCount }}条新消息</span>
        </div>

        <!-- 输入区域 -->
        <div class="chat-input">
          <!-- 回复预览 -->
          <div v-if="replyTo" class="reply-preview">
            <span>
              回复<span v-if="props.type !== 'private'">{{ replyTo.senderId === '0' ? '悦木小助手' : replyTo.sender?.userName || '用户' }}</span>:
              <span v-html="replyTo.senderId === '0' ? parseMarkdown(replyTo.content) : replyTo.content"></span>
            </span>
            <button class="cancel-reply" @click="cancelReply" type="button">取消回复</button>
          </div>

          <div class="input-wrapper" @mousedown.capture="handleButtonMousedown">
            <div class="emoji-container">
              <button class="emoji-btn" :class="{ active: showEmojiPicker }" @click="toggleEmojiPicker" type="button">
                😊
              </button>
              <!-- 表情面板 -->
              <div v-if="showEmojiPicker" class="emoji-panel">
                <EmojiPicker @select="onEmojiSelect" />
              </div>
            </div>
            <div class="message-input-container">
              <input
                ref="messageInput"
                v-model="inputMessage"
                class="message-input"
                placeholder="输入消息..."
                @keyup.enter="sendMessage"
                @input="handleInput"
                :disabled="!loginUser"
              />
              <div class="input-actions" v-show="!inputMessage || inputMessage.trim() === ''">
                <a-upload :show-upload-list="false" :before-upload="handleImageUpload" accept="image/*">
                  <i class="fas fa-camera img-icon"></i>
                </a-upload>
                <!--                <button-->
                <!--                  class="voice-btn"-->
                <!--                  :class="{ recording: isRecording }"-->
                <!--                  @mousedown="startRecording"-->
                <!--                  @mouseup="stopRecording"-->
                <!--                  @mouseleave="stopRecording"-->
                <!--                  type="button"-->
                <!--                  title="按住录音"-->
                <!--                >-->
                <!--                  <AudioOutlined />-->
                <!--                  <span v-if="isRecording" class="recording-time">{{ recordingTime }}s</span>-->
                <!--                </button>-->
              </div>
            </div>
            <button class="send-btn" :disabled="(!inputMessage.trim() && !isRecording) || !loginUser" @click="sendMessage" type="button">
              <LoadingOutlined v-if="loadingHistory" spin />
              <img v-else src="@/assets/icons/send.svg" class="send-icon" />
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- @提及 -->
    <div v-if="showMentionBox && String(props.spaceId) === '-2'" class="mention-box" :style="mentionBoxStyle">
      <div class="mention-item" @click="selectMention">
        <div class="mention-avatar">
          <img src="https://static.yuemutuku.com/space/1877028360151445505/2025-07-02_k9wqCp9awJ9iPEaC_thumbnail.webp" alt="悦木小助手" />
        </div>
        <div class="mention-info">
          <div class="mention-name">悦木小助手</div>
          <div class="mention-desc">AI助手</div>
        </div>
      </div>
    </div>

    <!-- 在线用户弹框 -->
    <teleport to="body">
      <div v-if="showUserModal" class="user-modal-overlay" @click="toggleUserModal">
        <div class="user-modal-content" @click.stop>
          <div class="modal-header">
            <h3 class="modal-title">
              在线用户 ({{ onlineUsers.length }})
            </h3>
            <button class="modal-close" @click="toggleUserModal" type="button">
              ✕
            </button>
          </div>
          <div class="modal-body">
            <div v-if="!onlineUsers.length" class="empty-users">
              <div class="empty-icon">👥</div>
              <p>暂无在线用户</p>
            </div>
            <div v-else class="users-scroll">
              <div
                v-for="user in onlineUsers"
                :key="user.id"
                class="user-item"
                @click="handleAvatarClick(user)"
              >
                <div class="user-avatar" :style="{ backgroundImage: `url(${user.userAvatar})` }">
                  <div class="online-dot"></div>
                </div>
                <div class="user-meta">
                  <div class="user-name">{{ user.userName }}</div>
                  <div class="user-role">{{ user.userRole === 'admin' ? '管理员' : '用户' }}</div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </teleport>

    <!-- 语音录制 -->
    <div v-if="isRecording" class="voice-modal">
      <div class="voice-content">
        <div class="wave-bars">
          <div class="wave-bar" v-for="i in 12" :key="i" />
        </div>
        <div class="voice-info">
          <span class="voice-time">{{ recordingTime }}s</span>
          <span class="voice-tip">松开发送，上滑取消</span>
        </div>
      </div>
    </div>

    <!-- 消息操作菜单 -->
    <teleport to="body">
      <div v-if="showActions && selectedMessage?.senderId !== '0'" class="action-menu" :style="actionMenuStyle">
        <button class="action-item" @click="() => handleReply(selectedMessage)" type="button">
          <span>回复</span>
        </button>
        <button class="action-item" @click="() => handleCopy(selectedMessage)" type="button">
          <span>复制</span>
        </button>
        <button class="action-item" @click="() => handleTranslate(selectedMessage)" type="button">
          <span>翻译</span>
        </button>
        <button class="action-item" @click="() => handleSearch(selectedMessage)" type="button">
          <span>搜索</span>
        </button>
        <button class="action-item" @click="() => openReportModal(selectedMessage?.senderId)" type="button">
          <span>举报</span>
        </button>
      </div>
    </teleport>

    <!-- 举报模态框 -->
    <ReportModal
      ref="reportModalRef"
      :target-type="reportTargetType"
      :target-id="reportTargetId"
    />
  </div>
</template>

<script setup lang="ts">
// ============ 逻辑部分保持不变，完全原样保留 ============
import { ref, onMounted, onUnmounted, computed, nextTick, onBeforeUnmount, h } from 'vue'
import { useLoginUserStore } from '@/stores/useLoginUserStore'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import dayjs from 'dayjs'
import ChatWebSocket from '@/utils/chatWebSocket'
import { getDefaultAvatar } from '@/utils/userUtils'
import { parseMarkdown } from '@/utils/markdownParser'
import { uploadPostImageUsingPost } from '@/api/pictureController'
import {
  ReloadOutlined,
  TeamOutlined,
  LoadingOutlined,
  DownOutlined,
  AudioOutlined,
  VideoCameraOutlined,
  NotificationOutlined
} from '@ant-design/icons-vue'
import EmojiPicker from './EmojiPicker.vue'
import { uploadAudioUsingPost } from '@/api/audioFileController'
import AudioBubble from './AudioBubble.vue'
import { useDebounceFn } from '@vueuse/core'
import { ImageCompressor } from '@/utils/imageCompressor'
import { Image } from 'ant-design-vue'
import ReportModal from '@/components/ReportModal.vue'

// 所有接口定义保持不变
interface ChatMessage {
  id: string | number
  type: number | 'loadMore' | 'getOnlineUsers' | 'RECALL' | 'heartbeat'
  content?: string
  messageId?: number | string
  pictureId?: string | number
  spaceId?: string | number
  privateChatId?: string | number
  senderId?: string | number
  sender?: {
    id: string | number
    userName: string
    userAvatar?: string
    userRole?: string
  }
  status?: 'RECALL' | string
  createTime?: string
  replyId?: string | number
  rootId?: string | number
  replyMessage?: ChatMessage
  isAtAI?: boolean
  messageType?: string
  messageUrl?: string
  messageSize?: number
  messageLocation?: string
  [key: string]: unknown
}

interface WebSocketMessage {
  type: 'history' | 'moreHistory' | 'onlineUsers' | 'message' | 'RECALL'
  messages?: ChatMessage[]
  message?: ChatMessage
  onlineUsers?: OnlineUser[]
  hasMore?: boolean
  messageId?: string | number
  id?: string | number
  content?: string
  sender?: {
    id: string | number
    userName: string
    userAvatar?: string
    userRole?: string
  }
  createTime?: string
  messageType?: string
  messageUrl?: string
  messageSize?: number
  messageLocation?: string
}

interface OnlineUser {
  id: string | number
  userName: string
  userAvatar: string
  userRole: string
  userAccount: string
  createTime: string
}

interface MessageData {
  type: 'message' | 'CHAT' | 'RECALL' | number
  content: string
  messageType?: string
  messageLocation?: string
  messageUrl?: string
  messageSize?: number
  pictureId?: string
  spaceId?: string
  privateChatId?: string
  replyId?: string | number
  rootId?: string | number
  isAtAI?: boolean
  id?: string | number
}

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
const messageInput = ref<HTMLInputElement | null>(null)

// 用于跟踪因异步内容（如图片）导致的滚动位置修正
const imagesLoadingCount = ref(0)
const scrollAfterImagesLoaded = ref(false)
const initialHistoryLoaded = ref(false)

// 分页相关
const current = ref(1)
const pageSize = ref(30)
const hasMore = ref(true)
const loadingHistory = ref(false)

// 回复相关
const replyTo = ref<MessageData | null>(null)

// 表情相关
const showEmojiPicker = ref(false)

// 滚动状态
const scrollState = ref({
  position: 0,
  lastHeight: 0,
  isAtTop: false,
  isAtBottom: true,
  hasNewMessage: false,
  unreadCount: 0
})

// 检查延迟状态
const checkingMore = ref(false)
const checkTimer = ref<ReturnType<typeof setTimeout> | null>(null)

// 手动加载相关
const manualLoadingHistory = ref(false)
const manualPageSize = ref(50)

// 重连逻辑
const reconnectTimer = ref<ReturnType<typeof setTimeout> | null>(null)
const maxReconnectAttempts = 5
const reconnectAttempts = ref(0)

// 在线用户弹框状态
const showUserModal = ref(false)

// 类型定义
const onlineUsers = ref<OnlineUser[]>([])

// 消息队列 - 新增：用于在连接建立前暂存消息
const messageQueue = ref<MessageData[]>([])

const emit = defineEmits<{
  (e: 'update:onlineUsers', users: OnlineUser[]): void
  (e: 'update:onlineCount', count: number): void
  (e: 'message', msg: WebSocketMessage): void
  (e: 'connected'): void
  (e: 'disconnected'): void
}>()

const router = useRouter()

// 切换在线用户弹框
const toggleUserModal = () => {
  showUserModal.value = !showUserModal.value
}

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
    checkingMore.value = true

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

    if (checkTimer.value) {
      clearTimeout(checkTimer.value)
    }
    checkTimer.value = setTimeout(() => {
      checkingMore.value = false
      if (messages.value.length === 0 || !hasMore.value) {
        hasMore.value = false
      }
      loadingHistory.value = false
    }, 3000)
  } catch {
    loadingHistory.value = false
    checkingMore.value = false
  }
}

// 设置要回复的消息
const replyToMessage = (msg: ChatMessage) => {
  if (msg && msg.id) {
    replyTo.value = {
      id: String(msg.id),
      type: msg.type,
      content: msg.content,
      pictureId: msg.pictureId || '',
      senderId: msg.senderId,
      status: msg.status,
      replyId: msg.replyId,
      rootId: msg.rootId,
      sender: msg.sender ? {
        id: String(msg.sender.id),
        userName: msg.sender.userName || '',
        userAvatar: msg.sender.userAvatar || getDefaultAvatar(msg.sender.userName)
      } : undefined
    }
  }
}

// 取消回复
const cancelReply = () => {
  replyTo.value = null
}

// 阻止按钮点击导致的输入框失焦
const handleButtonMousedown = (e: MouseEvent) => {
  const target = e.target as HTMLElement
  if (target.closest('.send-btn, .action-button, .emoji-trigger')) {
    e.preventDefault()
    messageInput.value?.focus()
  }
}

// 发送消息 - 修改：允许在连接建立前发送消息
const sendMessage = async () => {
  // 移除了 connected.value 的检查，改为检查 loginUser
  if (!loginUser.value || (!inputMessage.value.trim() && !isRecording.value)) {
    return
  }

  const wasFocused = document.activeElement === messageInput.value

  try {
    const location = await getLocation()

    const messageData: ChatMessage = {
      type: props.spaceId === -2 ? 3 : 'message',
      content: inputMessage.value.trim(),
      messageType: 'text',
      messageLocation: location,
      ...(props.pictureId ? { pictureId: String(props.pictureId) } : {}),
      ...(props.spaceId ? { spaceId: String(props.spaceId) } : {}),
      ...(props.privateChatId ? { privateChatId: String(props.privateChatId) } : {})
    };

    if (replyTo.value) {
      messageData.replyId = replyTo.value.id
      messageData.rootId = replyTo.value.rootId || replyTo.value.id
    }

    messageData.isAtAI = inputMessage.value.includes('@悦木小助手')

    // 如果WebSocket已连接，直接发送消息
    if (chatWs.value?.isConnected()) {
      chatWs.value.sendMessage(messageData)
    } else {
      // 如果WebSocket未连接，将消息加入队列
      messageQueue.value.push(messageData)
      // 如果WebSocket未初始化，则初始化
      if (!chatWs.value) {
        initWebSocket()
      }
      // 如果WebSocket已初始化但未连接，则尝试连接
      else if (!chatWs.value.isConnected() && !chatWs.value.isConnecting()) {
        chatWs.value.connect()
      }
      message.info('消息已提交，连接建立后将自动发送')
    }

    inputMessage.value = ''
    replyTo.value = null

    if (wasFocused) {
      messageInput.value?.focus()
    }
  } catch (error) {
    console.error('发送消息失败:', error)
    message.error('发送失败，请重试')
    messageInput.value?.focus()
  }
}

// 滚动处理
const handleScroll = (e: Event) => {
  const target = e.target as HTMLElement
  scrollState.value.position = target.scrollTop
  scrollState.value.lastHeight = target.scrollHeight
  scrollState.value.isAtTop = target.scrollTop <= 1

  const isNearBottom = target.scrollHeight - target.scrollTop - target.clientHeight < 50
  scrollState.value.isAtBottom = isNearBottom

  if (isNearBottom) {
    scrollState.value.hasNewMessage = false
  }

  if (target.scrollTop <= 30 && !loadingHistory.value && hasMore.value) {
    loadHistory()
  }

  // 添加触顶检测，即使 hasMore 为 false 也允许手动重新加载
  if (target.scrollTop <= 5 && !loadingHistory.value && !hasMore.value && !checkingMore.value) {
    // 用户手动触顶时允许重新检查是否有新数据
    hasMore.value = true
    // 立即触发加载检查
    loadHistory()
  }
}

// 滚动到底部
const scrollToBottom = (force: boolean = false) => {
  if (!messageContainer.value) return

  if (force || scrollState.value.isAtBottom) {
    setTimeout(() => {
      if (messageContainer.value) {
        messageContainer.value.scrollTop = messageContainer.value.scrollHeight
        scrollState.value.position = messageContainer.value.scrollTop
        scrollState.value.lastHeight = messageContainer.value.scrollHeight
        scrollState.value.hasNewMessage = false
        scrollState.value.unreadCount = 0
      }
    }, 50)
  } else {
    const lastMessage = messages.value[messages.value.length - 1]
    if (lastMessage && lastMessage.sender?.id !== loginUser.value?.id) {
      scrollState.value.hasNewMessage = true
      scrollState.value.unreadCount++
    }
  }
}

// 添加新消息提示
const scrollToNewMessage = () => {
  scrollToBottom(true)
}

// 时间显示判断：调整为 60 分钟一次，显著降低频率
const shouldShowTimestamp = (current: ChatMessage, prev: ChatMessage | undefined): boolean => {
  if (!prev) return true
  return dayjs(current.createTime).diff(dayjs(prev.createTime), 'minute') >= 60
}

// 格式化时间
const formatMessageDivider = (time: string) => {
  const t = dayjs(time)
  const now = dayjs()
  if (t.isSame(now, 'day')) return t.format('HH:mm')
  if (t.isSame(now.subtract(1, 'day'), 'day')) return '昨天 ' + t.format('HH:mm')
  if (t.isSame(now.subtract(2, 'day'), 'day')) return '前天 ' + t.format('HH:mm')
  if (t.isSame(now, 'year')) return t.format('MM-DD HH:mm')
  return t.format('YYYY-MM-DD HH:mm')
}

// 处理头像点击
interface UserProfile {
  id: string | number
  userName: string
  userAvatar?: string
  userAccount?: string
  createTime?: string
}

const handleAvatarClick = (user: UserProfile | null) => {
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

// 手动加载历史消息
const manualLoadHistory = async () => {
  if (manualLoadingHistory.value) return
  try {
    manualLoadingHistory.value = true
    checkingMore.value = true

    const params = {
      ...(props.pictureId ? { pictureId: String(props.pictureId) } : {}),
      ...(props.spaceId ? { spaceId: String(props.spaceId) } : {}),
      ...(props.privateChatId ? { privateChatId: String(props.privateChatId) } : {}),
      page: current.value,
      pageSize: manualPageSize.value
    }
    chatWs.value?.sendMessage({
      type: 'loadMore',
      ...params
    })

    if (checkTimer.value) {
      clearTimeout(checkTimer.value)
    }
    checkTimer.value = setTimeout(() => {
      checkingMore.value = false
      manualLoadingHistory.value = false
      if (messages.value.length === 0 || !hasMore.value) {
        hasMore.value = false
      }
    }, 5000)
  } catch {
    manualLoadingHistory.value = false
    checkingMore.value = false
  }
}

// 重连逻辑
const reconnect = () => {
  if (reconnectAttempts.value >= maxReconnectAttempts) {
    console.log('达到最大重连次数，停止重连')
    message.error('连接已断开，请刷新页面重试')
    return
  }

  if (reconnectTimer.value) {
    clearTimeout(reconnectTimer.value)
  }

  if (connected.value && chatWs.value?.isConnected()) {
    console.log('WebSocket已连接，不需要重连')
    return
  }

  reconnectTimer.value = setTimeout(() => {
    console.log('尝试重新连接...')
    reconnectAttempts.value++
    initWebSocket()
  }, 3000)
}

// WebSocket 初始化
const initWebSocket = () => {
  if (!loginUser.value) {
    message.error('请先登录')
    return
  }

  if (chatWs.value?.isConnected()) {
    console.log('[聊天室] WebSocket连接已存在且正常')
    connected.value = true
    return
  }

  if (chatWs.value) {
    console.log('[聊天室] 销毁旧的WebSocket连接')
    chatWs.value.destroy()
    chatWs.value = null
  }

  current.value = 1
  hasMore.value = true
  loadingHistory.value = false
  checkingMore.value = false
  messages.value = []
  initialLoading.value = true

  let chatType: 'chat' | 'space' | 'private' = 'chat'
  if (props.spaceId) {
    chatType = 'space'
  } else if (props.privateChatId) {
    chatType = 'private'
  }

  console.log('[聊天室] 创建新的WebSocket连接')
  chatWs.value = new ChatWebSocket({
    pictureId: props.pictureId,
    spaceId: props.spaceId,
    privateChatId: props.privateChatId,
    type: chatType
  })

  chatWs.value.on('open', () => {
    console.log('[聊天室] WebSocket连接成功')
    connected.value = true
    reconnectAttempts.value = 0
    emit('connected')

    // 连接成功后发送队列中的消息
    if (messageQueue.value.length > 0) {
      console.log(`[聊天室] 发送队列中的 ${messageQueue.value.length} 条消息`)
      messageQueue.value.forEach(msg => {
        chatWs.value?.sendMessage(msg)
      })
      messageQueue.value = []
      message.success('消息已发送')
    }
  })

  chatWs.value.on('close', () => {
    console.log('[聊天室] WebSocket连接已断开')
    connected.value = false
    emit('disconnected')
    if (!destroyed.value && document.visibilityState === 'visible') {
      reconnect()
    }
  })

  chatWs.value.on('error', () => {
    console.log('[聊天室] WebSocket连接发生错误')
    connected.value = false
    if (!destroyed.value && document.visibilityState === 'visible') {
      reconnect()
    }
  })

  chatWs.value.on('message', (msg: WebSocketMessage) => {
    if (msg.type === 'history') {
      console.log('收到初始历史消息:', msg.messages)
      if (msg.messages) {
        messages.value = msg.messages
          .filter((m: ChatMessage) => m.content?.trim())
          .map((m: ChatMessage) => ({
            ...m,
            sender: m.sender ? {
              ...m.sender,
              userAvatar: m.sender.userAvatar || getDefaultAvatar(m.sender.userName)
            } : null
          }))
          .reverse()
      }

      initialLoading.value = false

      if (checkTimer.value) {
        clearTimeout(checkTimer.value)
      }
      hasMore.value = msg.messages ? msg.messages.length >= pageSize.value : false
      current.value++
      loadingHistory.value = false
      checkingMore.value = false
      // 设置首次加载完成标志
      initialHistoryLoaded.value = true

      // 关键改动：使用新的安全滚动函数
      // 延迟执行滚动，确保图片有足够时间开始加载
      setTimeout(() => {
        safeScrollToBottom(true)
        // 再次延迟执行，确保所有图片都开始加载
        setTimeout(() => {
          safeScrollToBottom(true)
        }, 300)
      }, 150)
      manualLoadingHistory.value = false
    } else if (msg.type === 'moreHistory') {
      console.log('收到更多历史消息:', msg.messages)
      const oldScrollHeight = messageContainer.value?.scrollHeight || 0
      const oldScrollTop = messageContainer.value?.scrollTop || 0

      const newMessages = msg.messages
        .filter((m: ChatMessage) => m.content?.trim())
        .map((m: ChatMessage) => ({
          ...m,
          sender: m.sender ? {
            ...m.sender,
            userAvatar: m.sender.userAvatar || getDefaultAvatar(m.sender.userName)
          } : null
        }))
        .reverse()

      if (checkTimer.value) {
        clearTimeout(checkTimer.value)
      }
      checkingMore.value = false
      manualLoadingHistory.value = false

      if (newMessages.length === 0) {
        loadingHistory.value = false
        return
      }

      messages.value = [...newMessages, ...messages.value]
      hasMore.value = msg.hasMore &&
        (manualLoadingHistory.value ?
          newMessages.length >= manualPageSize.value :
          newMessages.length >= pageSize.value)
      current.value++
      loadingHistory.value = false

      nextTick(() => {
        if (messageContainer.value) {
          const newScrollHeight = messageContainer.value.scrollHeight
          const heightDiff = newScrollHeight - oldScrollHeight
          messageContainer.value.scrollTop = oldScrollTop + heightDiff
        }
      })
    } else if (msg.type === 'RECALL' && msg.messageId) {
      console.log('收到撤回消息:', msg.messageId)
      const messageIndex = messages.value.findIndex(m => String(m.id) === String(msg.messageId))
      if (messageIndex !== -1) {
        messages.value[messageIndex].status = 'recalled'
        messages.value[messageIndex].content = '消息已撤回'
      }
      emit('message', msg)
    } else if (msg.type === 'message' && msg.message) {
      const newMessage = msg.message
      if (!messages.value.some(m => m.id === newMessage.id) && newMessage.content?.trim()) {
        messages.value.push({
          ...newMessage,
          sender: newMessage.sender ? {
            ...newMessage.sender,
            userAvatar: newMessage.sender.userAvatar || getDefaultAvatar(newMessage.sender.userName)
          } : null,
          createTime: newMessage.createTime || dayjs().format('YYYY-MM-DD HH:mm:ss')
        })
        if (newMessage.sender?.id !== loginUser.value?.id) {
          safeScrollToBottom(false)
        } else {
          safeScrollToBottom(true)
        }
      }
    } else if (msg.type === 'onlineUsers') {
      onlineUsers.value = msg.onlineUsers || []
      emit('update:onlineUsers', msg.onlineUsers || [])
      emit('update:onlineCount', msg.onlineUsers?.length || 0)
      emit('message', msg)
    } else {
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
        if (msg.sender?.id !== loginUser.value?.id) {
          safeScrollToBottom(false)
        } else {
          safeScrollToBottom(true)
        }
      }
    }
  })

  chatWs.value.connect()
}

defineExpose({
  initWebSocket,
  disconnect: () => {
    if (chatWs.value) {
      chatWs.value.disconnect()
    }
  }
})

// 页面可见性变化监听
const handleVisibilityChange = () => {
  const now = Date.now()
  if (now - lastVisibilityChange.value < MIN_VISIBILITY_INTERVAL) {
    return
  }
  lastVisibilityChange.value = now

  if (document.visibilityState === 'visible') {
    if (chatWs.value?.isConnected()) {
      console.log('[聊天室] 页面恢复可见，WebSocket连接正常')
      return
    }
    console.log('[聊天室] 页面恢复可见，检测到WebSocket未连接，准备重新连接')
    reconnectAttempts.value = 0
    initWebSocket()
  }
}

onMounted(() => {
  lastVisibilityChange.value = Date.now()
  document.addEventListener('visibilitychange', handleVisibilityChange)
  initWebSocket()
  chatWs.value?.on('message', handleRecallEvent)
  window.addEventListener('resize', handleResize)
  handleResize()
})

onUnmounted(() => {
  document.removeEventListener('visibilitychange', handleVisibilityChange)
  window.removeEventListener('resize', handleResize)
  if (reconnectTimer.value) {
    clearTimeout(reconnectTimer.value)
  }
  if (chatWs.value) {
    chatWs.value.disconnect()
  }
  if (checkTimer.value) {
    clearTimeout(checkTimer.value)
  }
  emit('update:onlineCount', 0)
  emit('update:onlineUsers', [])
})

// @提及相关的状态
const showMentionBox = ref(false)
const mentionBoxStyle = ref({})
const mentionStartIndex = ref(-1)

// 处理输入事件
const handleInput = (e: Event) => {
  const target = e.target as HTMLInputElement
  const value = target.value
  const cursorPosition = target.selectionStart || 0

  if (value[cursorPosition - 1] === '@') {
    mentionStartIndex.value = cursorPosition - 1
    showMentionBox.value = true

    const inputRect = target.getBoundingClientRect()
    const isMobile = window.innerWidth <= 768

    if (isMobile) {
      mentionBoxStyle.value = {
        position: 'fixed',
        left: '16px',
        right: '16px',
        bottom: '80px',
        width: 'auto',
        top: 'auto',
        zIndex: 1000
      }
    } else {
      // 计算@符号在输入框中的位置
      const textBeforeAt = value.substring(0, cursorPosition)
      const tempSpan = document.createElement('span')
      // 复制输入框的样式
      const inputStyle = window.getComputedStyle(target)
      tempSpan.style.font = inputStyle.font
      tempSpan.style.fontFamily = inputStyle.fontFamily
      tempSpan.style.fontSize = inputStyle.fontSize
      tempSpan.style.fontWeight = inputStyle.fontWeight
      tempSpan.style.letterSpacing = inputStyle.letterSpacing
      tempSpan.style.position = 'absolute'
      tempSpan.style.visibility = 'hidden'
      tempSpan.style.whiteSpace = 'pre'
      tempSpan.textContent = textBeforeAt

      document.body.appendChild(tempSpan)
      const spanRect = tempSpan.getBoundingClientRect()

      // 计算@符号相对于视口的位置
      const atPositionTop = inputRect.top
      const atPositionLeft = inputRect.left + spanRect.width

      // 移除临时元素
      document.body.removeChild(tempSpan)

      // 让提及框显示在@符号的左下方
      mentionBoxStyle.value = {
        position: 'fixed',
        top: `${atPositionTop - 310}px`,
        left: `${atPositionLeft}px`,
        width: '240px',
        zIndex: 9999,
        maxHeight: '300px',
        overflowY: 'auto'
      }

      // 确保提及框不会超出视窗顶部边界
      if (atPositionTop - 310 < 10) {
        mentionBoxStyle.value.top = `${inputRect.bottom + 8}px`
      }

      // 确保提及框不会超出视窗右侧边界
      if (atPositionLeft + 240 > window.innerWidth) {
        mentionBoxStyle.value.left = `${Math.max(0, inputRect.left)}px`
      }
    }
  } else if (showMentionBox.value) {
    const textFromMentionStart = value.slice(mentionStartIndex.value, cursorPosition)
    if (!textFromMentionStart.startsWith('@') || textFromMentionStart.includes(' ')) {
      showMentionBox.value = false
    }
  }
}

// 选择提及
const selectMention = () => {
  if (mentionStartIndex.value >= 0) {
    const beforeMention = inputMessage.value.slice(0, mentionStartIndex.value)
    const afterMention = inputMessage.value.slice(mentionStartIndex.value + 1)
    inputMessage.value = beforeMention + '@悦木小助手 ' + afterMention
  }
  showMentionBox.value = false
  mentionStartIndex.value = -1
  messageInput.value?.focus()
}

// 处理输入框失焦事件
const handleInputBlur = () => {
  setTimeout(() => {
    showMentionBox.value = false
  }, 200)

  if (isRecording.value) {
    return
  }
}

// 消息操作菜单
const showActions = ref(false)
const actionMenuStyle = ref({})
const selectedMessage = ref<ChatMessage | null>(null)

// 举报相关变量
const reportModalRef = ref()
const reportTargetType = ref<string>('')
const reportTargetId = ref<string>('')

// 打开举报模态框函数
const openReportModal = (userId: string | number) => {
  if (!userId) {
    message.error('无法获取用户信息，无法举报')
    showActions.value = false
    selectedMessage.value = null
    return
  }

  if (reportModalRef.value) {
    // 设置举报目标类型和ID
    reportTargetType.value = '3'  // 用户类型
    reportTargetId.value = userId.toString()

    // 传递参数并打开模态框
    reportModalRef.value.openModal(reportTargetType.value, reportTargetId.value)
  } else {
    message.error('举报组件未加载')
  }
  showActions.value = false
  selectedMessage.value = null
}

const showMessageActions = (msg: ChatMessage, event: MouseEvent) => {
  if (!event || !msg || msg.content === '消息已撤回' || msg.senderId === '0') return

  event.preventDefault()
  event.stopPropagation()

  if (selectedMessage.value?.id === msg.id) {
    selectedMessage.value = null
  } else {
    selectedMessage.value = msg
  }

  const closeMenu = (e: MouseEvent) => {
    const target = e.target as HTMLElement
    // 检查点击目标是否在图片预览相关元素中
    const isPreviewElement = target.closest('.ant-image-preview') ||
      target.closest('.ant-image-preview-mask') ||
      target.closest('.ant-image-preview-operations') ||
      target.closest('.ant-image-preview-img') ||
      target.classList.contains('ant-image-preview-wrap');

    if (!target.closest('.message-actions') &&
      !target.closest('.message-main') &&
      !isPreviewElement) {
      selectedMessage.value = null
      document.removeEventListener('click', closeMenu)
    }
  }

  document.removeEventListener('click', closeMenu)
  setTimeout(() => {
    document.addEventListener('click', closeMenu)
  }, 0)
}

const handleReply = (msg?: ChatMessage) => {
  const messageToReply = msg || selectedMessage.value;
  if (messageToReply) {
    replyToMessage(messageToReply)
    showActions.value = false
    selectedMessage.value = null
  }
}

const handleCopy = async (msg?: ChatMessage) => {
  const messageToCopy = msg || selectedMessage.value;
  if (messageToCopy?.content) {
    try {
      await navigator.clipboard.writeText(messageToCopy.content)
      message.success('复制成功')
    } catch (err) {
      message.error('复制失败，请手动复制')
    }
  }
  showActions.value = false
  selectedMessage.value = null
}

const handleTranslate = async (msg?: ChatMessage) => {
  const messageToTranslate = msg || selectedMessage.value;
  if (!messageToTranslate?.content) return
  try {
    const text = encodeURIComponent(messageToTranslate.content)
    window.open(`https://fanyi.baidu.com/#zh/en/${text}`, '_blank')
  } catch (err) {
    message.error('翻译失败，请稍后重试')
  }
  showActions.value = false
  selectedMessage.value = null
}

const handleSearch = async (msg?: ChatMessage) => {
  const messageToSearch = msg || selectedMessage.value;
  if (!messageToSearch?.content) return
  const searchText = encodeURIComponent(messageToSearch.content)
  window.open(`https://www.baidu.com/s?wd=${searchText}`, '_blank')
  showActions.value = false
  selectedMessage.value = null
}

// 响应式处理
const isMobile = ref(window.innerWidth <= 768)

const handleResize = () => {
  isMobile.value = window.innerWidth <= 768
}

// 检查消息是否可以撤回
const canRecallMessage = (msg: ChatMessage) => {
  if (!msg || !loginUser.value) return false
  if (msg.senderId !== loginUser.value.id) return false
  const messageTime = dayjs(msg.createTime)
  const now = dayjs()
  const diffSeconds = now.diff(messageTime, 'second')
  return diffSeconds <= 60 && msg.content !== '消息已撤回'
}

// 处理消息撤回
const handleRecall = (msg: ChatMessage) => {
  if (!chatWs.value || !msg.id) return
  try {
    chatWs.value.recallMessage(msg.id)
    showActions.value = false
    selectedMessage.value = null
  } catch {
    message.error('消息撤回失败，请重试')
  }
}

// 监听消息撤回事件
const handleRecallEvent = (data: WebSocketMessage) => {
  if (data.type === 'RECALL' && data.messageId && data.message) {
    const index = messages.value.findIndex(msg => msg.id === data.messageId)
    if (index !== -1) {
      messages.value[index] = data.message
    }
  }
}

// 组件卸载
onBeforeUnmount(() => {
  console.log('[聊天室] 组件卸载，清理WebSocket连接')
  destroyed.value = true
  if (chatWs.value) {
    chatWs.value.destroy()
    chatWs.value = null
  }
  if (reconnectTimer.value) {
    clearTimeout(reconnectTimer.value)
  }
  if (checkTimer.value) {
    clearTimeout(checkTimer.value)
  }
  emit('update:onlineCount', 0)
  emit('update:onlineUsers', [])
})

// 初始加载状态
const initialLoading = ref(true)

// 连接状态追踪
const lastVisibilityChange = ref(Date.now())
const MIN_VISIBILITY_INTERVAL = 1000

// 组件销毁标记
const destroyed = ref(false)

/**
 * 安全地滚动到底部
 * 确保在所有异步内容（如图片）加载完毕后执行
 */
const safeScrollToBottom = async (force: boolean = true) => {
  // 首次直接尝试滚动
  await nextTick()
  scrollToBottom(force)

  // 如果有图片正在加载，则等待它们加载完成
  if (imagesLoadingCount.value > 0) {
    scrollAfterImagesLoaded.value = true
  } else {
    // 如果没有图片在加载，再次确保滚动到底部
    await nextTick()
    scrollToBottom(force)
  }
}

/**
 * 图片开始加载时调用
 */
const handleImageStartLoading = () => {
  imagesLoadingCount.value++
  // console.log('图片开始加载，当前加载数：', imagesLoadingCount.value)
}

/**
 * 图片加载完成或失败时调用
 */
const handleImageLoaded = () => {
  imagesLoadingCount.value--
  // console.log('图片加载完成，剩余加载数：', imagesLoadingCount.value)

  // 即使计数器为0，也稍微延迟一下再滚动，确保图片完全渲染
  if (imagesLoadingCount.value <= 0 && scrollAfterImagesLoaded.value) {
    scrollAfterImagesLoaded.value = false
    // 使用 setTimeout 确保 DOM 已完全更新
    setTimeout(() => {
      scrollToBottom(true)
    }, 200)
  } else if (imagesLoadingCount.value <= 0) {
    // 即使不需要滚动，也确保在所有图片加载完成后再次尝试滚动
    // 增加延迟时间，确保图片完全渲染
    setTimeout(() => {
      scrollToBottom(true)
    }, 200)

    // 如果是首次加载且有图片，额外执行一次滚动确保到底部
    if (initialHistoryLoaded.value) {
      setTimeout(() => {
        scrollToBottom(true)
      }, 400)
    }
  }
}

/**
 * 处理图片预览可见性变化
 * 防止预览关闭时影响消息操作菜单
 */
const handlePreviewVisibleChange = (visible: boolean) => {
  // 当图片预览关闭时，不隐藏消息操作菜单
  if (!visible && selectedMessage.value) {
    // 保持消息操作菜单的显示状态
    return
  }
}

// 录音相关
const isRecording = ref(false)
const recorder = ref<MediaRecorder | null>(null)
const recordingTime = ref(0)
const recordingTimer = ref<number | null>(null)
const audioChunks = ref<Blob[]>([])
const mediaStream = ref<MediaStream | null>(null)

const spinnerIcon = h(LoadingOutlined, {
  style: { fontSize: '24px', color: '#b2a284', marginBottom: '12px' }
})

const checkAudioDuration = (blob: Blob): Promise<boolean> => {
  return new Promise((resolve) => {
    const audio = new Audio(URL.createObjectURL(blob))
    audio.onloadedmetadata = () => {
      if (audio.duration < 1) {
        message.warning('录音时间太短，请至少录制1秒')
        resolve(false)
      } else {
        resolve(true)
      }
    }
    audio.onerror = () => {
      message.error('音频检查失败')
      resolve(false)
    }
  })
}

const handleAudioData = async (audioBlob: Blob) => {
  try {
    const isValidDuration = await checkAudioDuration(audioBlob)
    if (!isValidDuration) {
      return
    }

    const file = new File([audioBlob], `audio_${Date.now()}.mp3`, {
      type: 'audio/mpeg',
      lastModified: Date.now()
    })

    const response = await uploadAudioUsingPost({}, {}, file, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    })

    if (response.data?.code === 0 && response.data?.data) {
      const location = await getLocation()
      const audioMessage = {
        type: props.spaceId === -2 ? 3 : 'message',
        content: '语音消息',
        messageType: 'audio',
        messageUrl: response.data.data.fileUrl,
        messageSize: response.data.data.fileSize,
        messageLocation: location,
        ...(props.pictureId ? { pictureId: String(props.pictureId) } : {}),
        ...(props.spaceId ? { spaceId: String(props.spaceId) } : {}),
        ...(props.privateChatId ? { privateChatId: String(props.privateChatId) } : {})
      }

      // 如果WebSocket已连接，直接发送消息
      if (chatWs.value?.isConnected()) {
        chatWs.value.sendMessage(audioMessage)
      } else {
        // 如果WebSocket未连接，将消息加入队列
        messageQueue.value.push(audioMessage)
        // 如果WebSocket未初始化，则初始化
        if (!chatWs.value) {
          initWebSocket()
        }
        // 如果WebSocket已初始化但未连接，则尝试连接
        else if (!chatWs.value.isConnected() && !chatWs.value.isConnecting()) {
          chatWs.value.connect()
        }
        message.info('语音消息已提交，连接建立后将自动发送')
      }
    } else {
      console.error('音频上传响应格式错误:', response)
      message.error('音频上传失败')
    }
  } catch (error) {
    console.error('音频处理失败:', error)
    message.error('音频处理失败')
  }
}

const cleanupRecording = () => {
  if (mediaStream.value) {
    mediaStream.value.getTracks().forEach(track => track.stop())
    mediaStream.value = null
  }

  if (recordingTimer.value) {
    clearInterval(recordingTimer.value)
    recordingTimer.value = null
  }

  recorder.value = null
  isRecording.value = false
  recordingTime.value = 0
  audioChunks.value = []
}

const debouncedStartRecording = useDebounceFn(async () => {
  if (isRecording.value) return

  try {
    cleanupRecording()

    const stream = await navigator.mediaDevices.getUserMedia({
      audio: {
        channelCount: 1,
        sampleRate: 44100,
        echoCancellation: true,
        noiseSuppression: true
      }
    })

    mediaStream.value = stream

    const mediaRecorder = new MediaRecorder(stream, {
      mimeType: 'audio/webm',
      audioBitsPerSecond: 128000
    })

    mediaRecorder.ondataavailable = (e) => {
      if (e.data.size > 0) {
        audioChunks.value.push(e.data)
      }
    }

    mediaRecorder.onstop = async () => {
      const audioBlob = new Blob(audioChunks.value, { type: 'audio/webm' })
      await handleAudioData(audioBlob)
      cleanupRecording()
    }

    recorder.value = mediaRecorder
    mediaRecorder.start()
    isRecording.value = true

    recordingTimer.value = window.setInterval(() => {
      recordingTime.value++
      if (recordingTime.value >= 60) {
        stopRecording()
      }
    }, 1000)

  } catch (err) {
    console.error('录音失败:', err)
    message.error('录音失败，请检查麦克风权限')
    cleanupRecording()
  }
}, 300)

const startRecording = () => {
  debouncedStartRecording()
}

const stopRecording = () => {
  if (recorder.value && recorder.value.state === 'recording') {
    try {
      recorder.value.stop()
      messageInput.value?.focus()
    } catch (error) {
      console.error('停止录音失败:', error)
      message.error('停止录音失败')
      cleanupRecording()
      messageInput.value?.focus()
    }
  } else {
    cleanupRecording()
    messageInput.value?.focus()
  }
}

const handleImageUpload = async (file: File) => {
  try {
    const compressedFile = await ImageCompressor.compress(file, {
      maxWidth: 1280,
      maxHeight: 1280,
      maxSize: 500 * 1024
    })

    const location = await getLocation()
    const res = await uploadPostImageUsingPost({}, {}, compressedFile)
    if (res.data.code === 0) {
      const messageData = {
        type: props.spaceId === -2 ? 3 : 'message',
        content: file.name,
        messageType: 'image',
        messageUrl: res.data.data.url,
        messageSize: compressedFile.size,
        messageLocation: location,
        ...(props.pictureId ? { pictureId: String(props.pictureId) } : {}),
        ...(props.spaceId ? { spaceId: String(props.spaceId) } : {}),
        ...(props.privateChatId ? { privateChatId: String(props.privateChatId) } : {})
      }

      // 如果WebSocket已连接，直接发送消息
      if (chatWs.value?.isConnected()) {
        chatWs.value.sendMessage(messageData)
      } else {
        // 如果WebSocket未连接，将消息加入队列
        messageQueue.value.push(messageData)
        // 如果WebSocket未初始化，则初始化
        if (!chatWs.value) {
          initWebSocket()
        }
        // 如果WebSocket已初始化但未连接，则尝试连接
        else if (!chatWs.value.isConnected() && !chatWs.value.isConnecting()) {
          chatWs.value.connect()
        }
        message.info('图片已提交，连接建立后将自动发送')
      }
    } else {
      message.error('图片上传失败')
    }
  } catch (error) {
    console.error('图片处理失败:', error)
    message.error('图片处理失败')
  }
  return false
}

const getLocation = () => {
  return new Promise((resolve) => {
    let defaultLocation = '北京'
    fetch('https://api.myip.la/cn?json')
      .then(response => response.json())
      .then(data => {
        if (data && data.location && data.location.province) {
          resolve(data.location.province)
        } else {
          resolve(defaultLocation)
        }
      })
      .catch(() => {
        resolve(defaultLocation)
      })
  })
}

const handleVideoPlay = (event: Event) => {
  document.querySelectorAll('video').forEach((video) => {
    if (video !== event.target) {
      video.pause()
      video.currentTime = 0
    }
  })
}

const scrollToMessage = (messageId: string | number) => {
  const messageElement = document.querySelector(`[data-message-id="${messageId}"]`)
  if (messageElement) {
    messageElement.scrollIntoView({ behavior: 'smooth', block: 'center' })
  }
}

</script>

<style scoped lang="scss">
@mixin hardware-accelerate {
  transform: translateZ(0);
  will-change: transform;
}

// 聊天室容器
.chat-room {
  display: flex;
  background: var(--header-background);
  color: #575353;
  flex-direction: column;
  height: 100%;
  position: relative;
  @include hardware-accelerate;
  contain: layout style paint;
}

.chat-content {
  display: flex;
  flex: 1;
  min-height: 0;
  padding: 12px;
  gap: 12px;
  @include hardware-accelerate;
}

.messages-container {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: transparent;
  position: relative;
  overflow: hidden;
  gap: 12px;
  flex: 1; /* 让消息容器占据剩余空间 */
  min-width: 0; /* 防止flex子元素溢出 */
}

// 顶部加载指示器
.top-loading-indicator {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 8px;
  background: #f0f8ff;
  border-radius: 8px;
  margin: 4px 12px;
  gap: 8px;
  font-size: 12px;
  color: #1890ff;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  animation: fadeIn 0.3s ease;
  @include hardware-accelerate;

  :deep(.ant-spin) {
    color: #1890ff;
  }
}

.system-message{
  margin: auto;
  display: flex;
  justify-content: center;
  align-items: center;
}

.reload-btn{
  margin: auto;
}

.no-more-message {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 8px;
  color: #999;
  font-size: 12px;

  span {
    white-space: nowrap;
  }
}

.announcement-btn,
.online-users-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 6px 10px;
  border: none;
  border-radius: 16px;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s ease;
  @include hardware-accelerate;

  &:active {
    transform: scale(0.95);
  }
}

.announcement-btn {
  background: #ff8e53;
  color: #fff;
}

.online-users-btn {
  background: #1890ff;
  color: #fff;
}

// 消息列表容器
.chat-messages {
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  padding: 0 12px 12px;
  -webkit-overflow-scrolling: touch;
  overscroll-behavior: contain;
  @include hardware-accelerate;
  contain: strict;

  &::-webkit-scrollbar {
    width: 4px;
  }
  &::-webkit-scrollbar-thumb {
    background: #ccc;
    border-radius: 2px;
  }
}

// 加载状态
.loading-state {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  gap: 8px;
  color: #999;
}

// 空状态
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: #ccc;
  gap: 8px;

  .empty-icon {
    font-size: 48px;
  }
}

// 时间分割线 - 美化版
.timestamp-divider {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  margin: 32px 0;
  font-size: 11px;
  color: #ccc;
  letter-spacing: 1px;
  text-transform: uppercase;
  user-select: none;

  &::before,
  &::after {
    content: '·';
    font-size: 16px;
    color: #eee;
  }
}

// 消息项 - 性能关键，保持扁平结构
.message-item {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
  animation: slideIn 0.3s ease;
  @include hardware-accelerate;
  contain: layout style paint;

  &.message-self {
    flex-direction: row-reverse;

    .message-body {
      align-items: flex-end;
    }

    .text-bubble {
      background: #e3f2fd;
      border: 1px solid #bbdefb;
      border-radius: 18px 2px 18px 18px; /* 指向自己头像（右上侧） */
      box-shadow: 0 4px 12px rgba(187, 222, 251, 0.2);
    }
  }

  &.message-recalled {
    .message-main {
      opacity: 0.6;
    }

    // 撤回消息居中显示
    .text-bubble {
      text-align: center;
      color: #999;
      font-style: italic;
      align-self: center;
      max-width: 200px;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
    }

    // 使整个撤回消息项居中
    .message-body {
      align-items: center;
      width: 100%;
      max-width: 100%;
    }

    // 隐藏头像
    .message-avatar {
      display: none;
    }
  }
}

.message-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background-size: cover;
  background-position: center;
  cursor: pointer;
  flex-shrink: 0;
  @include hardware-accelerate;
  transition: transform 0.2s ease;

  &:active {
    transform: scale(0.9);
  }
}

.message-body {
  display: flex;
  flex-direction: column;
  gap: 4px;
  max-width: 70%;
  min-width: 0;
}

.message-username {
  font-size: 12px;
  color: #666;
  margin-bottom: 2px;

  .location {
    margin-left: 8px;
    font-size: 11px;
    color: #bbb;
    background: transparent;
    padding: 0;
    opacity: 0.6;
  }
}

.message-main {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}

// 消息气泡 - 核心性能优化点
.text-bubble {
  padding: 8px 14px;
  background: #f5f5f5;
  border-radius: 2px 18px 18px 18px; /* 指向对方头像（左上侧） */
  font-size: 14px;
  line-height: 1.5;
  color: #333;
  word-break: break-word;
  word-wrap: break-word;
  overflow-wrap: break-word;
  position: relative;
  @include hardware-accelerate;
}

.image-bubble,
.audio-bubble,
.video-bubble {
  max-width: 220px;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  @include hardware-accelerate;

  // 针对不同侧的消息应用非对称圆角
  .message-self & {
    border-radius: 18px 2px 18px 18px;
  }
  .message-item:not(.message-self) & {
    border-radius: 2px 18px 18px 18px;
  }

  img,
  video {
    display: block;
    width: 100%;
    height: auto;
    cursor: pointer;
  }
}

// 消息操作按钮
.message-actions {
  display: flex;
  gap: 4px;
  margin-top: 4px;
  padding: 4px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.1);
  @include hardware-accelerate;

  .action-btn {
    display: flex;
    align-items: center;
    justify-content: center;
    min-width: 40px; /* 增加最小宽度 */
    height: 28px;
    padding: 0 8px; /* 增加左右内边距 */
    border: none;
    border-radius: 4px;
    background: transparent;
    color: #666;
    cursor: pointer;
    transition: all 0.2s ease;
    font-size: 12px; /* 设置字体大小 */

    &:active {
      background: #f0f0f0;
      transform: scale(0.9);
    }
  }
}

// 回复引用
.reply-quote {
  margin-top: 4px;
  padding: 4px 8px;
  background: #f0f0f0;
  border-radius: 6px;
  font-size: 12px;
  color: #666;
  cursor: pointer;
  overflow: hidden;
  @include hardware-accelerate;
  transition: background 0.2s ease;

  &:active {
    background: #e0e0e0;
  }

  .reply-label {
    color: #999;
    margin-right: 4px;
  }

  // 限制回复内容的显示宽度
  display: -webkit-box;
  -webkit-line-clamp: 2; // 最多显示2行
  -webkit-box-orient: vertical;
  text-overflow: ellipsis;
}

// 新消息提示
.new-message-tip {
  position: absolute;
  bottom: 80px;
  left: 50%;
  transform: translateX(-50%) translateZ(0);
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 4px 12px;
  background: #1890ff;
  border-radius: 16px;
  color: #fff;
  font-size: 12px;
  cursor: pointer;
  z-index: 10;
  animation: fadeIn 0.3s ease;
  @include hardware-accelerate;
  contain: layout style paint;
  border: 1px solid #096dd9;
}

// 输入区域
.chat-input {
  padding: 8px 12px;

  flex-shrink: 0;
  @include hardware-accelerate;
}

.reply-preview {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
  padding: 4px 8px;
  background: #e3f2fd;
  border-left: 3px solid #1890ff;
  border-radius: 4px;
  font-size: 12px;

  span {
    flex: 1;
    overflow: hidden;
    display: -webkit-box;
    -webkit-line-clamp: 2; // 最多显示2行
    -webkit-box-orient: vertical;
    text-overflow: ellipsis;
  }

  .cancel-reply {
    color: #1890ff;
    border: none;
    background: none;
    cursor: pointer;
    font-size: 12px;
    padding: 4px 8px;
    flex-shrink: 0; // 防止取消按钮被压缩
    margin-left: 8px; // 添加左边距
  }
}

.input-wrapper {
  display: flex;
  gap: 4px;
  align-items: center;
  position: relative;
  @include hardware-accelerate;
}

.message-input-container {
  flex: 1;
  display: flex;
  align-items: center;
  padding: 4px 12px;
  border: 1px solid #e0e0e0;
  border-radius: 20px;
  background: #fff;

  &:focus-within {
    border-color: #1890ff;
  }

  .message-input {
    flex: 1;
    padding: 8px 0;
    border: none;
    outline: none;
    font-size: 14px;
    min-width: 0;
    background: transparent;
  }

  .input-actions {
    display: flex;
    gap: 4px;
    align-items: center;
    margin-left: 8px;
  }
}

.emoji-btn,
.img-btn,
.voice-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border: none;
  border-radius: 50%;
  background: #f5f5f5;
  font-size: 16px;
  cursor: pointer;
  flex-shrink: 0;
  @include hardware-accelerate;
  transition: all 0.2s ease;

  &:active {
    transform: scale(0.9);
    background: #e0e0e0;
  }

  &.active {
    background: #e3f2fd;
    color: #1890ff;
  }
}

.voice-btn {
  position: relative;

  .recording-time {
    position: absolute;
    top: -16px;
    left: 50%;
    transform: translateX(-50%);
    font-size: 10px;
    color: #ff4d4f;
    background: #fff;
    padding: 0 4px;
    border-radius: 8px;
  }
}

.input-actions {
  display: flex;
  gap: 4px;
  align-items: center;
}

.send-btn {
  padding: 0 16px;
  height: 36px;
  border: none;
  border-radius: 8px;
  background: #1890ff;
  cursor: pointer;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
  @include hardware-accelerate;

  &:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }

  &:active:not(:disabled) {
    transform: scale(0.9);
  }

  .send-icon {
    width: 18px;
    height: 18px;
    filter: invert(1);
  }
}

.emoji-container {
  position: relative;
  display: flex;
  align-items: center;
}

// 表情面板
.emoji-panel {
  position: absolute;
  bottom: 100%;
  left: 0;
  margin-bottom: 8px;
  background: #fff;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
  z-index: 100;
  @include hardware-accelerate;
}

.mention-box {
  position: fixed;
  background: #fff;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.15);
  width: 240px;
  max-height: 300px;
  margin-left: -300px;
  overflow-y: auto;
  z-index: 9999;
  @include hardware-accelerate;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;

  @media (max-width: 768px) {
    position: fixed;
    left: 16px !important;
    right: 16px !important;
    bottom: 70px;
    margin-left: 0;
    width: auto !important;
  }
}

.mention-item {
  display: flex;
  align-items: center;
  padding: 8px 12px;
  cursor: pointer;
  transition: background 0.2s ease;

  &:active {
    background: #f0f0f0;
  }
}

.mention-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  overflow: hidden;
  margin-right: 8px;

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }
}

.mention-info {
  flex: 1;
  min-width: 0;
}

.mention-name {
  font-size: 14px;
  font-weight: 500;
  color: #333;
}

.mention-desc {
  font-size: 12px;
  color: #999;
}

// 在线用户弹框样式
.user-modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2000;
  animation: fadeIn 0.2s ease;
  @include hardware-accelerate;
}

.user-modal-content {
  background: #fff;
  border-radius: 12px;
  width: 90%;
  max-width: 400px;
  max-height: 70vh;
  display: flex;
  flex-direction: column;
  animation: scaleIn 0.2s ease;
  @include hardware-accelerate;
  contain: layout style paint;
}

.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border-bottom: 1px solid #e0e0e0;
  flex-shrink: 0;
}

.modal-title {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 16px;
  font-weight: 500;
  margin: 0;
}

.modal-close {
  width: 32px;
  height: 32px;
  border: none;
  background: #f5f5f5;
  border-radius: 50%;
  font-size: 18px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
  @include hardware-accelerate;

  &:active {
    transform: scale(0.9);
    background: #e0e0e0;
  }
}

.modal-body {
  flex: 1;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.empty-users {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 200px;
  color: #999;
  gap: 8px;

  .empty-icon {
    font-size: 48px;
  }
}

.users-scroll {
  flex: 1;
  overflow-y: auto;
  padding: 8px 16px;
}

.user-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.2s ease;
  @include hardware-accelerate;

  &:active {
    background: #f0f0f0;
  }

  @media (max-width: 768px) {
    flex-direction: column;
    gap: 4px;
    padding: 8px 4px;
  }
}

.user-avatar {
  position: relative;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background-size: cover;
  background-position: center;
  flex-shrink: 0;

  .online-dot {
    position: absolute;
    bottom: 0;
    right: 0;
    width: 10px;
    height: 10px;
    border-radius: 50%;
    background: #52c41a;
    border: 1px solid #fff;
  }
}

.user-meta {
  flex: 1;
  min-width: 0;

  .user-name {
    font-size: 13px;
    font-weight: 500;
    color: #333;
    margin-bottom: 2px;
  }

  .user-role {
    font-size: 11px;
    color: #999;
  }
}

// 语音录制模态框
.voice-modal {
  position: fixed;
  bottom: 20%;
  left: 50%;
  transform: translate(-50%, -50%) translateZ(0);
  background: #fff;
  border-radius: 8px;
  padding: 24px;
  z-index: 9999;
  box-shadow: 0 4px 12px rgba(0,0,0,0.15);
  @include hardware-accelerate;
}

.voice-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 20px;
}

.wave-bars {
  display: flex;
  align-items: center;
  gap: 4px;
  height: 60px;
  padding: 0 20px;
}

.wave-bar {
  width: 4px;
  height: 20px;
  background: #1890ff;
  border-radius: 2px;
  animation: waveBar 1s ease-in-out infinite;
}

.voice-info {
  text-align: center;

  .voice-time {
    font-size: 24px;
    color: #1890ff;
    font-weight: 500;
  }

  .voice-tip {
    display: block;
    margin-top: 8px;
    font-size: 14px;
    color: #666;
  }
}

// 操作菜单
.action-menu {
  position: absolute;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
  padding: 4px;
  z-index: 1001;
  @include hardware-accelerate;
}

.action-item {
  display: flex;
  align-items: center;
  justify-content: center; /* 居中显示文字 */
  gap: 8px;
  padding: 8px 16px; /* 增加左右内边距 */
  border: none;
  background: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  transition: background 0.2s ease;
  min-width: 60px; /* 设置最小宽度 */

  &:active {
    background: #f0f0f0;
  }
}

// 动画
@keyframes slideIn {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes waveBar {
  0%, 100% { height: 20px; }
  50% { height: 40px; }
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

@keyframes scaleIn {
  from {
    opacity: 0;
    transform: scale(0.9);
  }
  to {
    opacity: 1;
    transform: scale(1);
  }
}

// 移动端适配
@media (max-width: 768px) {
  .chat-content {
    padding: 0;
  }

  .messages-container {
    border-radius: 0;
  }

  .chat-messages {
    padding: 0 8px 8px;
  }

  .message-item {
    gap: 6px;
    margin-bottom: 12px;
  }

  .message-avatar {
    width: 32px;
    height: 32px;
  }

  .message-body {
    max-width: 80%;
  }

  .text-bubble {
    padding: 6px 10px;
    font-size: 14px;
  }

  .image-bubble,
  .audio-bubble,
  .video-bubble {
    max-width: 150px;
  }

  .chat-input {
    padding: 6px 8px;
  }

  .input-wrapper {
    gap: 6px;
  }

  .emoji-btn,
  .img-btn,
  .voice-btn {
    width: 28px;
    height: 28px;
    font-size: 14px;
  }

  .send-btn {
    padding: 0 12px;
    height: 28px;
    border-radius: 8px;
    font-size: 14px;
  }

  .message-input-container {
    padding: 2px 8px;

    .message-input {
      padding: 6px 0;
    }
  }

  .mention-box {
    position: fixed;
    left: 16px !important;
    right: 16px !important;
    bottom: 70px;
    width: auto !important;
  }

  .user-modal-content {
    width: 85%;
    max-height: 60vh;
  }
}

// 移除了点击高亮动画
.message-highlight {
  /* background: none; */
}
</style>
