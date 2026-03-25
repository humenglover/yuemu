<template>
  <div class="ai-chat-page">
    <!-- 移动端遮罩层 -->
    <div class="drawer-mask" v-if="showSessionDrawer && isMobile" @click="toggleSessionDrawer"></div>

    <!-- 会话列表区域 (PC端常驻 移动端抽屉) -->
    <div class="session-sidebar" :class="{ 'drawer-open': showSessionDrawer && isMobile }">
      <div class="session-header">
        <h3>AI 客服</h3>

        <button
          class="new-session-btn"
          @click="showCreateSessionModal"
          :disabled="creatingSession"
        >
          <i class="fas fa-plus"></i>
          <span>{{ creatingSession ? '创建中...' : '新建会话' }}</span>
        </button>
        <!-- 移动端关闭按钮 -->
        <button class="mobile-close-btn" @click="toggleSessionDrawer" v-show="isMobile">
          <i class="fas fa-times"></i>
        </button>
      </div>



      <!-- 会话列表 -->
      <div class="session-list" ref="sessionListRef">
        <div class="loading" v-if="loadingSessions">
          <i class="fas fa-spinner fa-spin"></i>
          <span>加载会话中...</span>
        </div>
        <div class="session-item" v-for="item in sessions" :key="item.id" :class="{ active: currentSessionId === item.id }" @click="handleSessionClick(item.id)">
          <div class="session-name">{{ item.sessionName || '新会话' }}</div>
          <div class="session-actions">
            <button class="edit-btn" @click.stop="startEditingSession(item.id, item.sessionName)" title="编辑名称">
              <i class="fas fa-edit"></i>
            </button>
            <button class="delete-btn" @click.stop="confirmDeleteSession(item.id)" title="删除会话">
              <i class="fas fa-trash"></i>
            </button>
          </div>
        </div>

        <!-- 加载更多提示 -->
        <div class="load-more" v-if="sessionLoadingMore">
          <i class="fas fa-spinner fa-spin"></i>
          <span>加载中...</span>
        </div>
        <div class="no-more" v-if="!sessionHasMore && sessions.length > 0">已加载全部会话</div>
        <div class="empty-session" v-if="sessions.length === 0 && !loadingSessions">暂无会话</div>
      </div>
    </div>

    <!-- 主聊天区域 -->
    <div class="chat-container">
      <!-- 显示当前会话名称 -->
      <div class="session-title" v-if="currentSessionId">
        <div class="session-header-left">
          <h3>{{ getCurrentSessionName() }}</h3>
          <button class="back-btn" @click="goBack" title="返回上一页">
            <i class="fas fa-arrow-left"></i>
          </button>
        </div>
      </div>

      <!-- 移动端打开会话列表按钮 -->
      <button class="mobile-menu-btn" @click="toggleSessionDrawer" v-show="isMobile">
        <i class="fas fa-bars"></i>
      </button>

      <!-- 聊天消息区域 -->
      <div class="chat-messages" ref="messagesContainerRef" @scroll="handleMessagesScroll">
        <!-- 顶部加载更多提示 -->
        <div class="top-loading" v-if="messageLoadingMore && !loadingMessages">
          <i class="fas fa-spinner fa-spin"></i>
          <span>加载历史消息...</span>
        </div>
        <div class="no-more-message" v-if="!messageLoadingMore && !loadingMessages && !messageHasMore && initialMessageLoaded">
          <i class="fas fa-arrow-up"></i>
          <span>已经到顶了</span>
        </div>

        <!-- 消息列表 -->
        <div class="message-item" v-for="item in currentMessages" :key="item.id" :class="getMessageItemClass(item)">
          <div class="avatar" v-if="item.content !== '上下文已清理'">
            <img v-if="item.messageType === 1 && loginUser?.userAvatar" :src="loginUser.userAvatar" alt="用户头像" class="user-avatar" />
            <i v-else-if="item.messageType === 1" class="fas fa-user"></i>
            <span class="ai-avatar-text" v-else>AI</span>
          </div>
          <div class="message-content">
            <div class="sender" v-if="item.content !== '上下文已清理'">{{ getSenderName(item) }}</div>
            <div class="time" v-if="item.content !== '上下文已清理'">{{ formatTime(item.createTime) }}</div>
            <div class="content">
              <div v-if="isThinkingMessage(item)" class="thinking-indicator">
                <i class="fas fa-spinner fa-spin"></i>
                <span>正在思考中</span>
              </div>
              <div v-else :class="{'ai-streaming': item.isStreaming}" v-html="getContentHtml(item)"></div>
            </div>
          </div>
        </div>



        <!-- 空消息提示 -->
        <div class="empty-message" v-if="currentMessages.length === 0 && !loadingMessages">
          <i class="fas fa-comment-dots"></i>
          <span>暂无消息，开始您的第一次对话吧！</span>
        </div>

        <!-- 加载消息中 -->
        <div class="loading" v-if="loadingMessages">
          <i class="fas fa-spinner fa-spin"></i>
          <span>加载消息中...</span>
        </div>

        <!-- 清除上下文按钮 - 只有在有消息时显示 -->
        <div class="clear-context-container" v-if="currentMessages.length > 0 && currentSessionId">
          <button class="clear-context-btn" @click="clearSessionContext">
            <i class="fas fa-broom"></i>
            <span>清除上下文</span>
          </button>
        </div>

        <!-- 回到底部按钮 -->
        <button
          class="scroll-to-bottom-btn"
          :class="{ 'show': showScrollToBottom }"
          @click="scrollToBottom(true)"
          title="回到最新消息">
          <i class="fas fa-chevron-down"></i>
        </button>
      </div>

      <!-- 消息输入区域 -->
      <div class="chat-input-area">
        <!-- 语音输入按钮 -->
        <button
          class="voice-btn"
          @click="startVoiceInput"
          :class="{'is-recording': isRecording}"
          :disabled="!currentSessionId"
          title="点击开启语音输入"
        >
          <i class="fas" :class="isRecording ? 'fa-circle' : 'fa-microphone'"></i>
          <!-- 录音波纹动画（仅录音时显示） -->
          <div class="voice-waves" v-if="isRecording">
            <span></span>
            <span></span>
            <span></span>
          </div>
        </button>

        <input
          type="text"
          v-model="inputMessage"
          placeholder="请输入您的问题..."
          class="message-input"
          @keyup.enter="sendMessage"
          :disabled="sendingMessage || !currentSessionId"
        />
        <button class="send-btn" @click="sendMessage" :disabled="!inputMessage.trim() || sendingMessage || !currentSessionId">
          <i class="fas fa-paper-plane" v-if="!sendingMessage"></i>
          <i class="fas fa-spinner fa-spin" v-else></i>
        </button>
      </div>
    </div>
  </div>

  <!-- 录音弹窗 -->
  <teleport to="body">
    <transition name="fade-scale">
      <div class="voice-input-modal" v-if="showVoiceModal" @click.self="cancelVoiceInput">
        <div class="voice-modal-backdrop"></div>
        <div class="voice-modal-card">
          <div class="voice-modal-header">
            <div class="voice-status">
              <div class="status-dot" :class="{ pulse: isRecording }"></div>
              <span>{{ isRecording ? '正在倾听...' : '已结束' }}</span>
            </div>
            <button class="close-voice" @click="cancelVoiceInput">
              <i class="fas fa-times"></i>
            </button>
          </div>

          <div class="voice-modal-content">
            <div class="voice-result-box" :class="{ 'has-content': voiceTempResult }">
              <p v-if="voiceTempResult">{{ voiceTempResult }}</p>
              <p v-else class="placeholder">请说话，我正在听...</p>
            </div>

            <div class="voice-visualizer" v-if="isRecording">
              <div class="wave-bar" style="--delay: 0.1s"></div>
              <div class="wave-bar" style="--delay: 0.3s"></div>
              <div class="wave-bar" style="--delay: 0.5s"></div>
              <div class="wave-bar" style="--delay: 0.2s"></div>
              <div class="wave-bar" style="--delay: 0.4s"></div>
            </div>
          </div>

          <div class="voice-modal-footer">
            <button class="v-cancel-btn" @click="cancelVoiceInput">
              <i class="fas fa-trash"></i> 取消
            </button>
            <button class="v-confirm-btn" @click="finishVoiceInput" :disabled="!voiceTempResult">
              <i class="fas fa-check"></i> 结束并使用
            </button>
          </div>
        </div>
      </div>
    </transition>
  </teleport>

  <!-- 更新会话名称弹窗 -->
  <div class="update-session-modal" v-if="updateSessionModalVisible">
    <div @click="cancelUpdateSession"></div>
    <div class="update-session-content">
      <div class="update-session-header">
        <h4>更新会话名称</h4>
        <button class="close-modal" @click="cancelUpdateSession">
          <i class="fas fa-times"></i>
        </button>
      </div>
      <div class="update-session-body">
        <input
          type="text"
          v-model="updateSessionName"
          placeholder="请输入新的会话名称"
          class="session-input"
          @keyup.enter="confirmUpdateSession"
        />
      </div>
      <div class="update-session-footer">
        <button class="cancel-btn" @click="cancelUpdateSession">
          <i class="fas fa-ban"></i> 取消
        </button>
        <button class="confirm-btn" @click="confirmUpdateSession" :disabled="!updateSessionName.trim() || updatingSession">
          <i class="fas fa-check"></i> {{ updatingSession ? '更新中...' : '更新' }}
        </button>
      </div>
    </div>
  </div>

  <!-- 新建会话弹窗 -->
  <div class="create-session-modal" v-if="createSessionModalVisible">
    <div class="create-session-overlay" @click="cancelCreateSession"></div>
    <div class="create-session-content" @click.stop>
      <div class="create-session-header">
        <h4>新建会话</h4>
        <button class="close-modal" @click="cancelCreateSession">
          <i class="fas fa-times"></i>
        </button>
      </div>
      <div class="create-session-body">
        <input
          type="text"
          v-model="newSessionTitle"
          placeholder="请输入会话名称"
          class="session-input"
          @keyup.enter="confirmCreateSession"
        />
      </div>
      <div class="create-session-footer">
        <button class="cancel-btn" @click="cancelCreateSession">
          <i class="fas fa-ban"></i> 取消
        </button>
        <button class="confirm-btn" @click="confirmCreateSession" :disabled="!newSessionTitle.trim() || creatingSession">
          <i class="fas fa-check"></i> {{ creatingSession ? '创建中...' : '创建' }}
        </button>
      </div>
    </div>
  </div>

  <!-- 删除确认弹窗 -->
  <div class="delete-confirm-modal" v-if="showDeleteConfirmModal">
    <div class="delete-confirm-overlay" @click="cancelDeleteSession"></div>
    <div class="delete-confirm-content">
      <div class="delete-confirm-header">
        <h4>确认删除</h4>
        <button class="close-modal" @click="cancelDeleteSession">
          <i class="fas fa-times"></i>
        </button>
      </div>
      <div class="delete-confirm-body">
        <p>确定删除这个会话吗？</p>
        <p class="delete-warning">删除后将无法恢复，请谨慎操作。</p>
      </div>
      <div class="delete-confirm-footer">
        <button class="cancel-btn" @click="cancelDeleteSession">
          <i class="fas fa-ban"></i> 取消
        </button>
        <button class="confirm-delete-btn" @click="confirmDeleteSessionAction">
          <i class="fas fa-trash"></i> 确认删除
        </button>
      </div>
    </div>
  </div>

  <!-- 消息提示框 -->
  <div class="message-toast" v-if="showMessage" :class="[messageType]">
    <div class="message-content">
      <i :class="messageType === 'success' ? 'fas fa-check-circle' : 'fas fa-exclamation-circle'"></i>
      <span>{{ messageText }}</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, nextTick, computed, watch, reactive } from 'vue'
import { parseMarkdown } from '@/utils/markdownParser'
import { useLoginUserStore } from '@/stores/useLoginUserStore'
import { useRouter } from 'vue-router'
import {
  createSessionUsingPost,
  deleteSessionUsingPost,
  listSessionsUsingGet,
  switchSessionUsingPost,
  updateSessionNameUsingPost
} from '@/api/sessionController'
import {
  listQaMessagesUsingGet,
  sendQaMessageUsingPost,
  getQaAnswerUsingPost,
  clearSessionContextUsingPost
} from '@/api/messageQaController'

// 声明全局类型以避免TypeScript错误
interface Window {
  EventSource: typeof EventSource;
}

declare const EventSource: {
  prototype: EventSource;
  new(url: string, eventSourceInitDict?: EventSourceInit): EventSource;
};

// 防抖函数
const debounce = (func: Function, wait: number) => {
  let timeout: number | null = null
  return function executedFunction(...args: any[]) {
    const later = () => {
      timeout = null
      func(...args)
    }
    if (timeout) {
      clearTimeout(timeout)
    }
    timeout = window.setTimeout(later, wait)
  }
}

// 使用EventSource处理SSE流式响应的辅助函数
const handleSseStreamWithEventSource = (url: string, aiMessagePlaceholder: any, callback: (error?: Error) => void) => {
  // 标记消息正在流式更新中
  aiMessagePlaceholder.isStreaming = true;
  const messageId = Number(aiMessagePlaceholder.id);
  streamingMessageIds.value.push(messageId);

  const eventSource = new EventSource(url, { withCredentials: true });
  let displayedContent = '';

  console.log('SSE连接已建立，URL:', url);

  eventSource.addEventListener('aiAnswerChunk', (event: MessageEvent) => {
    try {
      // 核心协议修复：后端发送 JSON 包装的 Token 以安全地转义换行符
      const dataObj = JSON.parse(event.data);
      const token = dataObj.token;

      if (!token && token !== '') return;

      // 实时追加内容到变量
      displayedContent += token;

      // 直接更新响应式对象的属性，触发UI更新
      aiMessagePlaceholder.content = displayedContent;

      // 同时也更新列表中的对应对象以防万一
      const index = currentMessages.value.findIndex(msg => msg.id === aiMessagePlaceholder.id);
      if (index !== -1) {
        currentMessages.value[index].content = displayedContent;
      }

      scrollToBottomRealtime();
    } catch (error) {
      console.error('解析aiAnswerChunk事件失败:', error);
    }
  });

  eventSource.addEventListener('done', (event: MessageEvent) => {
    try {
      console.log('接收到done事件:', event.data);
      const doneData = JSON.parse(event.data);

      // 完成流式输出
      aiMessagePlaceholder.isStreaming = false;
      aiMessagePlaceholder.content = displayedContent; // 确保内容是最新的

      const aiMessageId = doneData.aiMsgId;
      if (aiMessageId) {
        aiMessagePlaceholder.id = aiMessageId;
        aiMessagePlaceholder.createTime = doneData.createTime;
      }

      // 清理状态
      const messageIdToRemove = Number(aiMessagePlaceholder.id);
      const sIndex = streamingMessageIds.value.indexOf(messageIdToRemove);
      if (sIndex > -1) streamingMessageIds.value.splice(sIndex, 1);

      // 强制最后一次渲染
      const index = currentMessages.value.findIndex(msg => msg.id === aiMessagePlaceholder.id);
      if (index !== -1) {
        currentMessages.value[index] = { ...aiMessagePlaceholder };
      }

      eventSource.close();
      callback();
    } catch (error) {
      console.error('解析done事件失败:', error);
      aiMessagePlaceholder.isStreaming = false;
      eventSource.close();
      callback(error as Error);
    }
  });

  eventSource.onerror = (event: Event) => {
    console.error('SSE连接发生错误:', event);
    console.error('Error event type:', event.type);
    // 错误时更新提示，让用户感知
    aiMessagePlaceholder.content = `${displayedContent}\n\n【AI响应中断，请重试】`;

    // 从流式更新列表中移除消息ID
    const messageIdToRemove = Number(aiMessagePlaceholder.id);
    const index = streamingMessageIds.value.indexOf(messageIdToRemove);
    if (index > -1) {
      streamingMessageIds.value.splice(index, 1);
    }

    eventSource.close();
    callback(new Error('SSE连接发生错误，可能是网络问题或服务端异常'));
  };

  // 3分钟超时：超时后更新提示
  const timeoutId = setTimeout(() => {
    aiMessagePlaceholder.content = `${displayedContent}\n\n【AI响应超时，未获取完整答案】`;
    eventSource.close();
    callback(new Error('SSE连接超时（3分钟），请缩短问题后重试'));
  }, 180000);

  // 成功完成后清除超时定时器
  eventSource.addEventListener('done', () => {
    clearTimeout(timeoutId);
  });
};

// 登录用户信息
const loginUserStore = useLoginUserStore()
const loginUser = computed(() => loginUserStore.loginUser)

// 设备判断
const isMobile = ref(false)
const checkIsMobile = () => {
  isMobile.value = window.innerWidth < 768
}

// 移动端抽屉控制
const showSessionDrawer = ref(false)
const toggleSessionDrawer = () => {
  showSessionDrawer.value = !showSessionDrawer.value
  if (isMobile.value) {
    document.body.style.overflow = showSessionDrawer.value ? 'hidden' : ''
  }
}

// 会话相关状态
const sessions = ref<API.RagSessionVO[]>([])
const currentSessionId = ref<number | null>(null)
const loadingSessions = ref(false)
const creatingSession = ref(false)
const createSessionModalVisible = ref(false)
const newSessionTitle = ref('')

// 分页相关状态
const sessionCurrentPage = ref(1)
const sessionPageSize = ref(20)
const sessionHasMore = ref(true)
const sessionLoadingMore = ref(false)

// 会话编辑相关状态
const updateSessionModalVisible = ref(false)
const updatingSession = ref(false)
const updateSessionName = ref('')
const sessionToUpdateId = ref<number | null>(null)

// 删除确认相关状态
const showDeleteConfirmModal = ref(false)
const sessionToDeleteId = ref<number | null>(null)

// 消息提示相关状态
const showMessage = ref(false)
const messageText = ref('')
const messageType = ref<'success' | 'error'>('success') // 'success' 或 'error'

// 消息相关状态
const currentMessages = ref<API.RagMessageVO[]>([])
const inputMessage = ref('')
const loadingMessages = ref(false)
const sendingMessage = ref(false)
const aiThinking = ref(false)
const aiMessageCurrentlyDisplayed = ref(false)

// 正在进行流式更新的消息ID集合
const streamingMessageIds = ref<number[]>([])

// 需要重新渲染的消息ID集合
const needsRerender = ref<number[]>([])

// 回到底部按钮显示状态
const showScrollToBottom = ref(false)

// 消息列表分页相关状态
const messageCurrentPage = ref(1)
const messagePageSize = ref(20)
const messageHasMore = ref(true)
const messageLoadingMore = ref(false)
const initialMessageLoaded = ref(false)

// 消息项引用
const messagesContainerRef = ref<HTMLElement | null>(null)
const sessionListRef = ref<HTMLElement | null>(null)

// 语音识别相关
const isRecording = ref(false)
const lastTouchTime = ref(0)
let recognition: any = null
const showVoiceModal = ref(false) // 录音模态框显示层
const voiceTempResult = ref('') // 实时识别出的临时文字内容
const voiceFinalResult = ref('') // 最终确认的文字内容
const voiceActive = ref(false) // 识别器是否正处于活跃侦听状态

const initRecognition = () => {
  if (recognition) return
  const SpeechRecognition = (window as any).SpeechRecognition || (window as any).webkitSpeechRecognition
  if (!SpeechRecognition) {
    return
  }
  recognition = new SpeechRecognition()
  recognition.lang = 'zh-CN'
  recognition.continuous = true
  recognition.interimResults = true

  recognition.onresult = (event: any) => {
    let result = ''
    for (let i = event.resultIndex; i < event.results.length; i++) {
      result += event.results[i][0].transcript
    }
    if (result) {
      voiceTempResult.value = result
    }
  }

  recognition.onerror = (event: any) => {
    if (event.error === 'aborted') return
    console.error('语音识别错误:', event.error)
    // 错误时自动关闭并显示
    cancelVoiceInput()

    const isIOS = /iPad|iPhone|iPod/.test(navigator.userAgent)
    const errorMap: Record<string, string> = {
      'not-allowed': isMobile.value
        ? '麦克风权限被拒绝。请在系统设置中允许浏览器访问麦克风，或点击地址栏左侧图标重新授权。'
        : '麦克风权限被拒绝，请检查浏览器设置',
      'network': '网络连接出现问题，语音识别失败',
      'no-speech': '未检测到声音，请说话后再试',
      'audio-capture': '无法获取麦克风，请检查设备是否被占用',
      'service-not-allowed': isIOS
        ? '当前环境暂不支持语音识别，建议使用 iOS Safari 浏览器。'
        : '浏览器语音识别服务当前不可用',
    }

    const errorMsg = errorMap[event.error] || `语音识别失败 (${event.error})，请重试`
    showErrorMessage(errorMsg)
  }

  recognition.onend = () => {
    voiceActive.value = false
  }
}

const startVoiceInput = async () => {
  if (!currentSessionId.value) return

  // 1. 如果已经在录音，点击则触发完成逻辑并关闭弹窗
  if (isRecording.value) {
    finishVoiceInput()
    return
  }

  // 2. 检查环境限制 (微信等 WebView 通常不支持原生识别)
  const isWechat = /micromessenger/i.test(navigator.userAgent)
  if (isWechat) {
    showErrorMessage('微信环境限制，请点击右上角[在浏览器打开]以使用录音功能')
    return
  }

  // 3. 检查安全上下文
  const isSecure = location.protocol === 'https:' || location.hostname === 'localhost'
  if (!isSecure) {
    showErrorMessage('语音识别功能需要 HTTPS 安全连接支持')
    return
  }

  // 4. 移动端权限预检：强制唤起系统麦克风授权弹窗
  if (isMobile.value && navigator.mediaDevices && navigator.mediaDevices.getUserMedia) {
    try {
      // 通过该方法可以强制触发浏览器的权限请求对话框
      const stream = await navigator.mediaDevices.getUserMedia({ audio: true })
      // 成功获取权限后立即停止流，释放设备，下一步交给 SpeechRecognition
      stream.getTracks().forEach(track => track.stop())
    } catch (err: any) {
      console.warn('获取权限流失败或被拒绝:', err)
      if (err.name === 'NotAllowedError' || err.name === 'PermissionDeniedError') {
        showErrorMessage('您拒绝了麦克风权限，无法使用语音输入')
        return
      }
      // 其他错误（如设备忙）可尝试继续由 recognition 处理
    }
  }

  // 5. 初始化识别器
  initRecognition()
  if (!recognition) {
    showErrorMessage('您的浏览器不支持语音识别功能，建议使用 Chrome 或 Safari')
    return
  }

  // 6. 重置状态并同步展示模态框
  voiceTempResult.value = ''
  showVoiceModal.value = true

  // 7. 启动识别器
  try {
    recognition.start()
    voiceActive.value = true
    isRecording.value = true
    if (navigator.vibrate) navigator.vibrate(50)
  } catch (err: any) {
    if (err.name === 'InvalidStateError' || err.message?.includes('already started')) {
      voiceActive.value = true
      isRecording.value = true
    } else {
      console.error('启动识别失败:', err)
      showErrorMessage('开启麦克风失败，请重试')
      cancelVoiceInput()
    }
  }
}

// 停止识别并完成提交
const finishVoiceInput = () => {
  if (recognition) {
    try {
      recognition.stop()
    } catch (e) {}
  }

  // 将识别出的文字追加到主输入框
  if (voiceTempResult.value) {
    inputMessage.value = (inputMessage.value + ' ' + voiceTempResult.value).trim()
  }

  showVoiceModal.value = false
  isRecording.value = false
}

// 取消识别并丢弃
const cancelVoiceInput = () => {
  if (recognition) {
    try {
      recognition.abort()
    } catch (e) {}
  }
  showVoiceModal.value = false
  isRecording.value = false
  voiceTempResult.value = ''
}

const stopVoiceInput = () => {
  // 旧方法兼容性保留，不再执行任何实质操作
}

// 获取消息项的CSS类
const getMessageItemClass = (item: API.RagMessageVO) => {
  if (item.content === '上下文已清理') {
    // 对"上下文已清理"消息应用特殊样式
    return 'system-message'
  } else if (item.messageType === 1) {
    // 用户消息样式
    return 'user-message'
  } else {
    // AI消息样式
    return 'assistant-message'
  }
}

// 获取发送者名称
const getSenderName = (item: API.RagMessageVO) => {
  if (item.messageType === 1) {
    return loginUser?.userName || '用户'
  } else {
    return 'AI客服'
  }
}

// 获取内容HTML
const getContentHtml = (item: API.RagMessageVO) => {
  if (item.content === '上下文已清理') {
    // 对"上下文已清理"消息不解析markdown
    return item.content
  } else if (item.messageType === 1) {
    return item.content
  } else {
    // 对AI消息进行Markdown解析
    const parsed = parseMarkdown(item.content);
    return parsed;
  }
};

// 判断是否是AI消息
const isAiMessage = (item: API.RagMessageVO) => {
  return item.messageType === 2 && item.content !== '上下文已清理' && !isThinkingMessage(item);
}

// 判断是否是思考中的消息
const isThinkingMessage = (item: API.RagMessageVO) => {
  return item.messageType === 2 && item.content === '正在思考中';
}

// 判断内容是否已完成加载
const isContentComplete = (item: API.RagMessageVO) => {
  // 如果消息ID在流式更新列表中，则返回false，否则返回true
  // 同时也要排除思考中的消息
  return !streamingMessageIds.value.includes(Number(item.id)) && !isThinkingMessage(item);
}

// 判断内容是否需要重新渲染
const isContentReadyForRerender = (item: API.RagMessageVO) => {
  return isContentComplete(item) && shouldRerender(Number(item.id));
}

// 强制刷新消息
const refreshMessage = (item: API.RagMessageVO) => {
  // 通过替换数组引用来强制重新渲染
  const index = currentMessages.value.findIndex(msg => msg.id === item.id);
  if (index !== -1) {
    currentMessages.value[index] = {...currentMessages.value[index]};
  }
};

// 标记消息需要重新渲染
const markNeedRerender = (itemId: number) => {
  if (!needsRerender.value.includes(itemId)) {
    needsRerender.value.push(itemId);
  }
};

// 检查消息是否需要重新渲染
const shouldRerender = (itemId: number): boolean => {
  const index = needsRerender.value.indexOf(itemId);
  if (index !== -1) {
    // 移除标记
    needsRerender.value.splice(index, 1);
    return true;
  }
  return false;
};

// 格式化时间
const formatTime = (timeStr: string) => {
  try {
    return new Date(timeStr).toLocaleString('zh-CN', {
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit',
      year: 'numeric',
      month: '2-digit',
      day: '2-digit'
    })
  } catch {
    return timeStr
  }
}

// 加载会话列表
const loadSessions = async (append = false) => {
  try {
    if (!append) {
      loadingSessions.value = true
      sessionCurrentPage.value = 1
    } else {
      sessionLoadingMore.value = true
    }

    const res = await listSessionsUsingGet({
      current: sessionCurrentPage.value,
      pageSize: sessionPageSize.value
    })

    if (res.data.code === 0) {
      const newSessions = res.data.data?.records || []

      if (!append) {
        sessions.value = newSessions
      } else {
        sessions.value = [...sessions.value, ...newSessions]
      }

      sessionHasMore.value = newSessions.length === sessionPageSize.value

      // 自动选择第一个会话
      if (!currentSessionId.value && sessions.value.length > 0) {
        currentSessionId.value = sessions.value[0].id
        await loadMessages()
        await nextTick()
        scrollToBottom(true)
      }
      // 如果会话列表为空，显示创建会话的弹窗
      else if (!append && sessions.value.length === 0) {
        showCreateSessionModal()
      }
    } else {
      showErrorMessage(res.data.message || '获取会话列表失败')
    }
  } catch (error) {
    console.error('获取会话列表失败:', error)
    showErrorMessage('获取会话列表失败')
  } finally {
    loadingSessions.value = false
    sessionLoadingMore.value = false
  }
}

// 加载更多会话
const loadMoreSessions = async () => {
  if (sessionHasMore.value && !sessionLoadingMore.value && !loadingSessions.value) {
    sessionCurrentPage.value += 1
    await loadSessions(true)
  }
}

// 加载当前会话的消息
const loadMessages = async (append = false) => {
  if (!currentSessionId.value) return

  try {
    if (!append) {
      loadingMessages.value = true
      messageCurrentPage.value = 1
    } else {
      messageLoadingMore.value = true
    }

    const res = await listQaMessagesUsingGet({
      sessionId: currentSessionId.value,
      current: messageCurrentPage.value,
      pageSize: messagePageSize.value
    })

    if (res.data.code === 0) {
      const newMessages = res.data.data?.records || []

      if (!append) {
        // 后端返回数据按时间降序（最新的在前），需要翻转以按时间升序显示（最旧的在前）
        currentMessages.value = newMessages.reverse()
        initialMessageLoaded.value = true
        await nextTick()
        scrollToBottomRealtime() // 滚动到底部（最新消息）
      } else {
        // 在加载更多消息时，记录当前滚动位置和容器高度
        const messagesContainer = messagesContainerRef.value;
        let previousScrollTop = 0;
        let previousScrollHeight = 0;
        if (messagesContainer) {
          previousScrollTop = messagesContainer.scrollTop;
          previousScrollHeight = messagesContainer.scrollHeight;
        }

        // 后端返回数据按时间降序，需要翻转后再添加到前面
        const reversedNewMessages = newMessages.reverse();
        currentMessages.value = [...reversedNewMessages, ...currentMessages.value];

        // 等待DOM更新后，恢复滚动位置，保持用户看到的相对位置
        await nextTick(() => {
          if (messagesContainer) {
            // 计算新增内容的高度，并相应调整滚动位置
            const heightDiff = messagesContainer.scrollHeight - previousScrollHeight;
            // 恢复滚动位置，加上新增内容的高度，以保持用户看到的相对位置不变
            messagesContainer.scrollTop = previousScrollTop + heightDiff;
          }
        });
      }

      messageHasMore.value = newMessages.length === messagePageSize.value
    } else {
      showErrorMessage(res.data.message || '获取消息列表失败')
    }
  } catch (error) {
    console.error('获取消息列表失败:', error)
    showErrorMessage('获取消息列表失败')
  } finally {
    loadingMessages.value = false
    messageLoadingMore.value = false
  }
}

// 加载更多消息
const loadMoreMessages = async () => {
  if (messageHasMore.value && !messageLoadingMore.value && !loadingMessages.value) {
    messageLoadingMore.value = true
    try {
      messageCurrentPage.value += 1

      const res = await listQaMessagesUsingGet({
        sessionId: currentSessionId.value,
        current: messageCurrentPage.value,
        pageSize: messagePageSize.value
      })

      if (res.data.code === 0) {
        const newMessages = res.data.data?.records || []

        if (newMessages.length > 0) {
          // 在加载更多消息时，记录当前滚动位置和容器高度
          const messagesContainer = messagesContainerRef.value;
          let previousScrollTop = 0;
          let previousScrollHeight = 0;
          if (messagesContainer) {
            previousScrollTop = messagesContainer.scrollTop;
            previousScrollHeight = messagesContainer.scrollHeight;
          }

          // 后端返回数据按时间降序，需要翻转后再添加到前面
          const reversedNewMessages = newMessages.reverse();
          currentMessages.value = [...reversedNewMessages, ...currentMessages.value];

          // 等待DOM更新后，恢复滚动位置，保持用户看到的相对位置
          await nextTick(() => {
            if (messagesContainer) {
              // 计算新增内容的高度，并相应调整滚动位置
              const heightDiff = messagesContainer.scrollHeight - previousScrollHeight;
              // 恢复滚动位置，加上新增内容的高度，以保持用户看到的相对位置不变
              messagesContainer.scrollTop = previousScrollTop + heightDiff;
            }
          });
        }

        messageHasMore.value = newMessages.length === messagePageSize.value
      } else {
        messageHasMore.value = false
        showErrorMessage(res.data.message || '获取历史消息失败')
      }
    } catch (error) {
      console.error('加载更多消息失败:', error)
      messageHasMore.value = false
      showErrorMessage('加载更多消息失败')
    } finally {
      messageLoadingMore.value = false
    }
  }
}

// 显示创建会话弹窗
const showCreateSessionModal = () => {
  newSessionTitle.value = ''
  createSessionModalVisible.value = true
}

// 取消创建会话
const cancelCreateSession = () => {
  createSessionModalVisible.value = false
  newSessionTitle.value = ''
}

// 开始编辑会话名称
const startEditingSession = (sessionId: number, currentName: string) => {
  sessionToUpdateId.value = sessionId
  updateSessionName.value = currentName || ''
  updateSessionModalVisible.value = true
}

// 确认更新会话
const confirmUpdateSession = async () => {
  if (!sessionToUpdateId.value || !updateSessionName.value.trim()) {
    showErrorMessage('请输入会话名称')
    return
  }

  try {
    updatingSession.value = true

    const res = await updateSessionNameUsingPost({
      id: sessionToUpdateId.value,
      sessionName: updateSessionName.value.trim()
    })

    if (res.data.code === 0 && res.data.data) {
      const session = sessions.value.find(s => s.id === sessionToUpdateId.value)
      if (session) {
        session.sessionName = updateSessionName.value.trim()
      }
      showSuccessMessage('会话名称更新成功')
      updateSessionModalVisible.value = false
      sessionToUpdateId.value = null
      updateSessionName.value = ''
    } else {
      showErrorMessage(res.data.message || '会话名称更新失败')
    }
  } catch (error) {
    console.error('更新会话名称失败:', error)
    showErrorMessage('更新会话名称失败')
  } finally {
    updatingSession.value = false
  }
}

// 取消更新会话
const cancelUpdateSession = () => {
  updateSessionModalVisible.value = false
  sessionToUpdateId.value = null
  updateSessionName.value = ''
}

// 确认创建会话
const confirmCreateSession = async () => {
  try {
    creatingSession.value = true

    const res = await createSessionUsingPost({
      sessionName: newSessionTitle.value.trim() || '新会话'
    })

    if (res.data.code === 0 && res.data.data) {
      const newSession = res.data.data
      sessions.value.unshift(newSession)
      currentSessionId.value = newSession.id
      currentMessages.value = []
      showSuccessMessage('会话创建成功')
      createSessionModalVisible.value = false
      await loadMessages()
      await nextTick()
      scrollToBottomRealtime()

      // 移动端自动关闭抽屉
      if (isMobile.value) {
        toggleSessionDrawer()
      }
    } else {
      showErrorMessage(res.data.message || '创建会话失败')
    }
  } catch (error) {
    console.error('创建会话失败:', error)
    showErrorMessage('创建会话失败')
  } finally {
    creatingSession.value = false
  }
}

// 切换会话
const handleSessionClick = async (sessionId: number) => {
  try {
    const res = await switchSessionUsingPost({ sessionId })
    if (res.data.code === 0) {
      currentSessionId.value = sessionId
      await loadMessages()
      await nextTick()
      scrollToBottom(true)

      // 移动端点击会话后关闭抽屉
      if (isMobile.value) {
        toggleSessionDrawer()
      }
    } else {
      showErrorMessage(res.data.message || '切换会话失败')
    }
  } catch (error) {
    console.error('切换会话失败:', error)
    showErrorMessage('切换会话失败')
  }
}

// 确认删除会话
const confirmDeleteSession = (sessionId: number) => {
  showDeleteConfirmModal.value = true;
  sessionToDeleteId.value = sessionId;
}

// 取消删除会话
const cancelDeleteSession = () => {
  showDeleteConfirmModal.value = false;
  sessionToDeleteId.value = null;
}

// 确认删除会话操作
const confirmDeleteSessionAction = async () => {
  if (sessionToDeleteId.value) {
    await deleteSession(sessionToDeleteId.value);
    showDeleteConfirmModal.value = false;
    sessionToDeleteId.value = null;
  }
}

// 删除会话
const deleteSession = async (sessionId: number) => {
  try {
    const res = await deleteSessionUsingPost({ sessionId })
    if (res.data.code === 0) {
      sessions.value = sessions.value.filter(session => session.id !== sessionId)

      if (currentSessionId.value === sessionId) {
        if (sessions.value.length > 0) {
          currentSessionId.value = sessions.value[0].id
          await loadMessages()
        } else {
          currentSessionId.value = null
          currentMessages.value = []
        }
      }

      // 显示成功提示
      showSuccessMessage('会话删除成功')
    } else {
      // 显示错误提示
      showErrorMessage(res.data.message || '删除会话失败')
    }
  } catch (error) {
    console.error('删除会话失败:', error)
    // 显示错误提示
    showErrorMessage('删除会话失败')
  }
}

// 发送消息
const sendMessage = async () => {
  if (!inputMessage.value.trim() || !currentSessionId.value) return

  const userMessage = inputMessage.value.trim()
  inputMessage.value = ''

  try {
    sendingMessage.value = true

    // 首先创建并添加用户消息到对话中
    const userMessageObj: API.RagMessageVO = {
      id: Date.now(), // 临时ID
      sessionId: currentSessionId.value,
      content: userMessage,
      userId: loginUser.value?.id,
      messageType: 1, // 用户消息类型
      createTime: new Date().toISOString()
    };

    currentMessages.value.push(userMessageObj);
    await nextTick();
    scrollToBottomRealtime();

    // 然后设置AI思考状态并添加AI消息占位符
    aiThinking.value = true

    // 创建AI消息占位符 - 使用reactive确保内容响应式
    const aiMessagePlaceholder: API.RagMessageVO = reactive({
      id: Date.now() + 1, // 临时ID
      sessionId: currentSessionId.value!,
      content: '正在思考中', // 初始显示思考中提示
      userId: loginUser.value?.id,
      messageType: 2, // AI消息类型
      createTime: new Date().toISOString()
    });
    // 设置AI消息当前已显示状态
    aiMessageCurrentlyDisplayed.value = true;

    // 添加AI消息占位符到数组
    currentMessages.value.push(aiMessagePlaceholder);
    await nextTick();
    scrollToBottomRealtime();

    // 更新会话名称（首次消息）
    const currentSession = sessions.value.find(session => session.id === currentSessionId.value)
    if (currentSession && (!currentSession.sessionName || currentSession.sessionName.includes('会话-'))) {
      currentSession.sessionName = userMessage.length > 15 ? userMessage.substring(0, 15) + '...' : userMessage
    }

    // 使用新的EventSource API进行流式通信
    const encodedMessage = encodeURIComponent(userMessage);
    const url = `/api/rag/qa/message/send/stream?message=${encodedMessage}&sessionId=${currentSessionId.value}`;

    // 处理SSE流式响应
    await new Promise<void>((resolve, reject) => {
      handleSseStreamWithEventSource(url, aiMessagePlaceholder, (error) => {
        if (error) {
          console.error('流式通信错误:', error);
          showErrorMessage(error.message || 'AI响应流发生错误');
          reject(error);
        } else {
          resolve();
        }
      });
    });

  } catch (error) {
    console.error('发送消息失败:', error);
    showErrorMessage('发送消息失败');
  } finally {
    aiThinking.value = false;
    sendingMessage.value = false;
    // 重置AI消息显示状态
    aiMessageCurrentlyDisplayed.value = false;
  }
}

// 滚动到消息底部
const scrollToBottom = (force = false) => {
  // 立即滚动到底部，不等待nextTick
  if (messagesContainerRef.value) {
    // 立即滚动到底部
    messagesContainerRef.value.scrollTop = messagesContainerRef.value.scrollHeight;
  }
}

// 强制实时滚动到底部（专门用于流式输出场景）
const scrollToBottomRealtime = () => {
  if (messagesContainerRef.value) {
    // 使用requestAnimationFrame确保在浏览器下次重绘之前滚动
    requestAnimationFrame(() => {
      if (messagesContainerRef.value) {
        messagesContainerRef.value.scrollTop = messagesContainerRef.value.scrollHeight;
      }
    });
  }
}

// 监听消息滚动（加载更多和回到底部按钮显示）
const handleMessagesScroll = debounce(() => {
  if (messagesContainerRef.value) {
    const element = messagesContainerRef.value

    // 检查是否需要显示/隐藏回到底部按钮
    // 当用户没有滚动到底部时显示按钮（预留50px余量）
    const threshold = 50;
    const nearBottom = element.scrollHeight - element.scrollTop - element.clientHeight < threshold;
    showScrollToBottom.value = !nearBottom;

    // 加载更多消息
    if (element.scrollTop <= 10 && !messageLoadingMore.value && !loadingMessages.value && messageHasMore.value) {
      loadMoreMessages()
    }
  }
}, 300) // 300ms防抖

// 监听会话列表滚动（加载更多）
const handleSessionListScroll = () => {
  if (sessionListRef.value) {
    const element = sessionListRef.value
    if (element.scrollHeight - element.scrollTop <= element.clientHeight + 10) {
      loadMoreSessions()
    }
  }
}

// 监听窗口大小变化
watch(isMobile, (val) => {
  if (!val) {
    showSessionDrawer.value = false // 非移动端关闭抽屉
  }
})

// 显示成功消息
const showSuccessMessage = (message: string) => {
  messageText.value = message;
  messageType.value = 'success';
  showMessage.value = true;

  // 3秒后自动隐藏
  setTimeout(() => {
    showMessage.value = false;
  }, 3000);
}

// 显示错误消息
const showErrorMessage = (message: string) => {
  messageText.value = message;
  messageType.value = 'error';
  showMessage.value = true;

  // 3秒后自动隐藏
  setTimeout(() => {
    showMessage.value = false;
  }, 3000);
}

// 添加获取当前会话名称的方法
const getCurrentSessionName = () => {
  if (currentSessionId.value) {
    const session = sessions.value.find(s => s.id === currentSessionId.value);
    return session ? session.sessionName || '新会话' : '新会话';
  }
  return '请选择会话';
};

// 获取路由器实例
const router = useRouter()

// 返回上一页
const goBack = () => {
  router.go(-1)
}

// 初始化
onMounted(() => {
  checkIsMobile()
  window.addEventListener('resize', checkIsMobile)
  loadSessions().then(() => {
    // 确保在加载完成后，如果会话列表仍然为空且没有会话被选中，显示创建会话弹窗
    if (sessions.value.length === 0 && !currentSessionId.value) {
      showCreateSessionModal()
    }
  })

  nextTick(() => {
    if (sessionListRef.value) {
      sessionListRef.value.addEventListener('scroll', handleSessionListScroll)
    }
    if (messagesContainerRef.value) {
      messagesContainerRef.value.addEventListener('scroll', handleMessagesScroll)
    }
  })
})

// 卸载组件
onUnmounted(() => {
  window.removeEventListener('resize', checkIsMobile)
  if (sessionListRef.value) {
    sessionListRef.value.removeEventListener('scroll', handleSessionListScroll)
  }
  if (messagesContainerRef.value) {
    messagesContainerRef.value.removeEventListener('scroll', handleMessagesScroll)
  }
})

// 清除当前会话上下文
const clearSessionContext = async () => {
  if (!currentSessionId.value) {
    showErrorMessage('当前没有活动会话')
    return
  }

  try {
    const res = await clearSessionContextUsingPost({ sessionId: currentSessionId.value })
    if (res.data.code === 0) {
      if (res.data.data) {
        // 添加返回的系统消息到消息列表
        currentMessages.value.push(res.data.data)

        // 滚动到底部以显示新消息
        await nextTick()
        scrollToBottomRealtime()
      }
      showSuccessMessage('上下文已清除，可以开始新的对话')
    } else {
      showErrorMessage(res.data.message || '清除上下文失败')
    }
  } catch (error) {
    console.error('清除上下文失败:', error)
    showErrorMessage('清除上下文失败')
  }
}
</script>

<style scoped lang="scss">
// 全局样式
.ai-chat-page {
  // PC端高度92vh，移动端100vh
  height: 92vh;
  max-width: 1400px;  // PC端最大宽度限制
  display: flex;
  flex-direction: row; // PC端默认左右布局
  background-color: var(--background);
  font-size: 14px;
  color: var(--text-primary);
  overflow: hidden;
  margin: 0 auto;  // 居中显示

  // 移动端高度调整为100vh
  @media (max-width: 768px) {
    height: 100vh;
    max-width: 100vw;  // 移动端最大宽度为屏幕宽度
  }
}

// 移动端遮罩
.drawer-mask {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  z-index: 1000;
}

// 会话侧边栏 (PC端常驻 移动端抽屉)
.session-sidebar {
  width: 260px;
  // PC端高度继承父级92vh，移动端100vh
  height: 92vh;
  background: var(--card-background);
  border-right: 1px solid var(--border-color);
  display: flex;
  flex-direction: column;
  transition: transform 0.3s ease;
  z-index: 1001;

  // 移动端高度调整为100vh + 固定定位
  @media (max-width: 768px) {
    height: 100vh;
    position: fixed;
    top: 0;
    left: 0;
    transform: translateX(-100%);
    box-shadow: 2px 0 8px rgba(0, 0, 0, 0.1);

    &.drawer-open {
      transform: translateX(0);
    }
  }

  // PC端默认显示
  @media (min-width: 769px) {
    transform: translateX(0) !important;
  }

  .session-header {
    padding: 12px 16px;
    border-bottom: 1px solid var(--border-color);
    display: flex;
    justify-content: space-between;
    align-items: center;

    h3 {
      margin: 0;
      font-size: 16px;
      font-weight: 600;
    }

    .mobile-close-btn {
      display: none;
      background: transparent;
      border: none;
      font-size: 16px;
      cursor: pointer;
      width: 36px;
      height: 36px;
      border-radius: 50%;
      color: var(--text-secondary);

      &:hover {
        background: var(--hover-background);
      }

      @media (max-width: 768px) {
        display: flex;
        align-items: center;
        justify-content: center;
      }
    }

    .new-session-btn {
      padding: 6px 12px;
      background: var(--link-color);
      color: var(--text-other);
      border: none;
      border-radius: 4px;
      cursor: pointer;
      font-size: 13px;
      display: flex;
      align-items: center;
      gap: 6px;
      transition: background 0.2s;

      &:hover {
        background: var(--link-hover-color);
      }

      &:disabled {
        background: var(--interaction-button-color);
        cursor: not-allowed;
      }

      i {
        font-size: 12px;
      }
    }
  }

  // 会话列表
  .session-list {
    flex: 1;
    overflow-y: auto;
    padding: 8px 0;

    .session-item {
      padding: 10px 16px;
      display: flex;
      justify-content: space-between;
      align-items: center;
      cursor: pointer;
      transition: background 0.2s;

      &:hover {
        background: var(--hover-background);
      }

      &.active {
        background: var(--hover-background);
        border-left: 3px solid #1890ff;
      }

      .session-name {
        font-size: 13px;
        color: var(--text-primary);
        white-space: nowrap;
        overflow: hidden;
        text-overflow: ellipsis;
        max-width: 180px;
      }

      .session-actions {
        display: flex;
        gap: 4px;

        .edit-btn, .delete-btn {
          background: transparent;
          border: none;
          font-size: 12px;
          cursor: pointer;
          width: 28px;
          height: 28px;
          display: flex;
          align-items: center;
          justify-content: center;
          border-radius: 4px;
          transition: background 0.2s;

          &:hover {
            background: var(--hover-background);
          }
        }

        .edit-btn {
          color: var(--link-color);
        }

        .delete-btn {
          color: var(--like-button-active-color);
        }
      }
    }

    .loading, .load-more {
      padding: 12px;
      text-align: center;
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 8px;
      font-size: 12px;
      color: #666;

      i {
        font-size: 14px;
        animation: fa-spin 1s linear infinite;
      }
    }

    .no-more {
      padding: 12px;
      text-align: center;
      font-size: 12px;
      color: #999;
    }

    .empty-session {
      padding: 24px;
      text-align: center;
      font-size: 12px;
      color: #999;

      i {
        font-size: 24px;
        margin-bottom: 8px;
        display: block;
      }
    }
  }
}

// 主聊天容器
.chat-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  // PC端高度92vh，移动端100vh
  height: 92vh;

  @media (max-width: 768px) {
    height: 100vh;
  }

  // 会话标题样式
  .session-title {
    padding: 12px 16px;
    border-bottom: 1px solid var(--border-color);
    background: var(--card-background);
    position: relative;
    z-index: 10;

    .session-header-left {
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 12px;
      position: relative;
      width: 100%;
    }

    .back-btn {
      position: absolute;
      right: 0;
      top: 50%;
      transform: translateY(-50%);
      width: 32px;
      height: 32px;
      border-radius: 50%;
      border: none;
      background: var(--hover-background);
      color: var(--text-primary);
      cursor: pointer;
      display: flex;
      align-items: center;
      justify-content: center;
      transition: background 0.2s;
      z-index: 11;

      &:hover {
        background: var(--link-color);
        color: var(--text-other);
      }
    }

    h3 {
      margin: 0;
      font-size: 16px;
      font-weight: 600;
      color: var(--text-primary);
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
      text-align: center; // 居中对齐
    }
  }

  // 移动端菜单按钮
  .mobile-menu-btn {
    position: fixed;
    top: 12px;
    left: 12px;
    width: 40px;
    height: 40px;
    border-radius: 50%;
    background: var(--card-background);
    border: none;
    box-shadow: 0 2px 8px var(--shadow-color);
    font-size: 16px;
    cursor: pointer;
    z-index: 999;
    color: var(--text-primary);
    display: none;

    &:hover {
      background: var(--hover-background);
    }

    @media (max-width: 768px) {
      display: flex;
      align-items: center;
      justify-content: center;
    }
  }

  // 聊天消息区域
  .chat-messages {
    flex: 1;
    overflow-y: auto;
    padding: 16px;
    background-color: var(--chat-background);
    padding-bottom: 20px;
    // 调整顶部内边距，为会话标题留出空间
    padding-top: 24px;

    // 顶部加载提示
    .top-loading {
      padding: 8px;
      text-align: center;
      background: var(--hover-background);
      border-radius: 4px;
      margin-bottom: 12px;
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 8px;
      font-size: 12px;
      color: var(--link-color);

      i {
        font-size: 14px;
        animation: fa-spin 1s linear infinite;
      }
    }

    // 无更多消息提示
    .no-more-message {
      padding: 8px;
      text-align: center;
      background: var(--hover-background);
      border-radius: 4px;
      margin-bottom: 12px;
      font-size: 12px;
      color: var(--text-secondary);
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 6px;

      i {
        font-size: 12px;
      }
    }

    // 消息项
    .message-item {
      display: flex;
      margin-bottom: 16px;
      max-width: 100%;

      &.user-message {
        justify-content: flex-end;
      }

      .avatar {
        width: 36px;
        height: 36px;
        border-radius: 50%;
        background: var(--chat-message-sender-background);
        color: var(--chat-message-sender-text);
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 16px;
        flex-shrink: 0;

        &.assistant {
          background: var(--ai-avatar-background);
        }

        i {
          font-size: 16px;
        }

        .user-avatar {
          width: 100%;
          height: 100%;
          border-radius: 50%;
          object-fit: cover;
        }
      }

      .message-content {
        margin: 0 12px;
        max-width: 80%;
        min-width: unset;

        .sender {
          font-size: 12px;
          color: var(--text-secondary);
          margin-bottom: 4px;
        }

        .time {
          font-size: 11px;
          color: var(--text-secondary);
          margin-bottom: 6px;
        }

        .content {
          padding: 10px 14px;
          border-radius: 8px;
          line-height: 1.5;
          word-wrap: break-word;
          background: var(--chat-message-background);
          color: var(--chat-message-text);
          max-width: 100%;

          // Markdown 样式
          p {
            margin: 0.5em 0;
          }

          h1, h2, h3, h4, h5, h6 {
            margin: 0.8em 0;
            font-weight: bold;
          }

          ul, ol {
            margin: 0.5em 0;
            padding-left: 20px;
          }

          pre {
            background: var(--hover-background);
            padding: 10px;
            border-radius: 4px;
            overflow-x: auto;
            margin: 0.5em 0;
          }

          code {
            font-family: 'Courier New', monospace;
            background: var(--hover-background);
            padding: 2px 4px;
            border-radius: 3px;
            font-size: 0.9em;
          }

          blockquote {
            border-left: 3px solid var(--border-color);
            padding-left: 10px;
            margin: 0.5em 0;
            color: var(--text-secondary);
          }

          a {
            color: var(--link-color);
            text-decoration: none;
          }

          img {
            max-width: 100%;
            height: auto;
          }
        }
      }
    }

    // 用户消息样式
    .user-message .avatar {
      order: 2;
      background: var(--chat-message-sender-background);
    }

    .user-message .message-content {
      text-align: left;
      min-width: unset;

      .content {
        background: var(--link-color);
        color: var(--text-other);
        border-bottom-right-radius: 2px;
        min-width: unset;
      }
    }

    // AI 消息样式
    .assistant-message .avatar {
      background: var(--ai-avatar-background);
      color: var(--ai-avatar-text);
    }

    .ai-avatar-text {
      font-size: 12px;
      font-weight: bold;
    }

    .assistant-message .message-content .content {
      background: var(--chat-message-background);
      border: 1px solid var(--border-color);
      border-bottom-left-radius: 2px;
      min-width: unset;
    }

    // AI思考中样式
    .thinking-indicator {
      display: flex;
      align-items: center;
      padding:  14px;
      background: var(--chat-message-background);
      border-radius: 8px;
      min-height: 40px;
      gap: 8px;
    }

    .thinking-dot {
      width: 8px;
      height: 8px;
      background: var(--link-color);
      border-radius: 50%;
      margin-right: 4px;
      animation: thinking-pulse 1.4s infinite ease-in-out both;
    }

    .thinking-dot:nth-child(2) {
      animation-delay: 0.2s;
    }

    .thinking-dot:nth-child(3) {
      animation-delay: 0.4s;
      margin-right: 0;
    }

    @keyframes thinking-pulse {
      0%, 100% {
        transform: scale(0.8);
        opacity: 0.6;
      }
      50% {
        transform: scale(1.2);
        opacity: 1;
      }
    }

    // 系统消息样式（仅用于上下文已清理消息）
    .system-message {
      justify-content: center;
      margin-left: 0;
      margin-right: 0;
    }

    .system-message .message-content {
      max-width: 100%;
      text-align: center;
      margin: 0;
    }

    .system-message .message-content .content {
      background: var(--hover-background);
      color: var(--text-secondary);
      padding: 8px 16px;
      border-radius: 20px;
      display: inline-block;
      border: none;
      font-size: 12px;
      min-width: unset;
    }

    .system-message .sender,
    .system-message .time {
      display: none;
    }

    // 空消息状态
    .empty-message {
      height: 100%;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      color: var(--text-secondary);
      font-size: 14px;
      gap: 8px;

      i {
        font-size: 32px;
        color: var(--text-secondary);
      }
    }

    // 加载状态
    .loading {
      padding: 24px;
      text-align: center;
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 8px;
      font-size: 12px;
      color: var(--text-secondary);

      i {
        font-size: 14px;
        animation: fa-spin 1s linear infinite;
      }
    }
  }

  // 输入区域
  .chat-input-area {
    padding: 12px 16px;
    border-top: 1px solid var(--border-color);
    background: var(--chat-input-background);
    display: flex;
    gap: 8px;
    align-items: center;

    .message-input {
      flex: 1;
      padding: 12px 16px;
      border: 1px solid var(--border-color);
      border-radius: 24px;
      font-size: 14px;
      outline: none;
      transition: border 0.2s;

      &:focus {
        border-color: var(--link-color);
      }

      &:disabled {
        background: var(--hover-background);
        cursor: not-allowed;
      }
    }

    .send-btn {
      width: 44px;
      height: 44px;
      background: var(--link-color);
      color: white;
      border: none;
      border-radius: 50%;
      font-size: 16px;
      cursor: pointer;
      transition: all 0.3s cubic-bezier(0.175, 0.885, 0.32, 1.275);
      display: flex;
      align-items: center;
      justify-content: center;
      flex-shrink: 0;
      box-shadow: 0 4px 10px rgba(24, 144, 255, 0.2);

      &:hover:not(:disabled) {
        background: var(--link-hover-color);
        transform: translateY(-2px) scale(1.05);
        box-shadow: 0 6px 15px rgba(24, 144, 255, 0.3);
      }

      &:active:not(:disabled) {
        transform: translateY(0) scale(0.95);
      }

      &:disabled {
        background: var(--interaction-button-color);
        color: rgba(255, 255, 255, 0.5);
        cursor: not-allowed;
        box-shadow: none;
      }

      i {
        font-size: 16px;
      }
    }

    /* 语音按钮样式 */
    .voice-btn {
      width: 44px;
      height: 44px;
      border-radius: 50%;
      border: none;
      background: var(--hover-background);
      color: var(--text-primary);
      cursor: pointer;
      display: flex;
      align-items: center;
      justify-content: center;
      position: relative;
      transition: all 0.3s cubic-bezier(0.175, 0.885, 0.32, 1.275);
      flex-shrink: 0;
      -webkit-touch-callout: none; /* 禁用系统长按菜单 */
      -webkit-tap-highlight-color: transparent; /* 移除点击高亮 */
      user-select: none; /* 防止选中文字 */

      &:hover {
        background: var(--interaction-button-color);
        color: var(--link-color);
      }

      &.is-recording {
        background: var(--link-color);
        color: white;
        transform: scale(1.1);
        box-shadow: 0 0 15px var(--link-color);
      }

      i {
        font-size: 18px;
        z-index: 2;
        pointer-events: none; /* 确保图标不拦截点击 */
      }
    }

    .voice-waves {
      position: absolute;
      top: 50%;
      left: 50%;
      transform: translate(-50%, -50%);
      width: 100%;
      height: 100%;
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 3px;
      pointer-events: none; /* 关键：确保动效层不拦截点击，点击可直接穿透到底层按钮 */

      span {
        width: 3px;
        height: 10px;
        background: rgba(255, 255, 255, 0.6);
        border-radius: 2px;
        animation: wave-animation 1s ease-in-out infinite;

        &:nth-child(2) { animation-delay: 0.2s; height: 16px; }
        &:nth-child(3) { animation-delay: 0.4s; }
      }
    }

    @keyframes wave-animation {
      0%, 100% { transform: scaleY(1); }
      50% { transform: scaleY(1.8); }
    }
  }
}

// 更新会话名称弹窗样式
.update-session-modal {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1004; // 确保在其他弹窗之上
}

.update-session-content {
  width: 90%;
  max-width: 400px;
  background: var(--card-background);
  border-radius: 8px;
  box-shadow: 0 4px 12px var(--shadow-color);
  animation: modal-fade-in 0.2s ease;

  .update-session-header {
    padding: 16px;
    border-bottom: 1px solid var(--border-color);
    display: flex;
    justify-content: space-between;
    align-items: center;

    h4 {
      margin: 0;
      font-size: 16px;
      font-weight: 600;
    }

    .close-modal {
      background: transparent;
      border: none;
      font-size: 16px;
      cursor: pointer;
      width: 36px;
      height: 36px;
      display: flex;
      align-items: center;
      justify-content: center;
      border-radius: 50%;
      transition: background 0.2s;
      color: var(--text-secondary);

      &:hover {
        background: var(--hover-background);
      }
    }
  }

  .update-session-body {
    padding: 16px;

    .session-input {
      width: 100%;
      padding: 10px 12px;
      border: 1px solid var(--border-color);
      border-radius: 4px;
      font-size: 14px;
      outline: none;

      &:focus {
        border-color: var(--link-color);
      }
    }
  }

  .update-session-footer {
    padding: 12px 16px;
    border-top: 1px solid var(--border-color);
    display: flex;
    justify-content: flex-end;
    gap: 8px;

    .cancel-btn {
      padding: 8px 16px;
      border: 1px solid var(--border-color);
      background: var(--card-background);
      border-radius: 4px;
      cursor: pointer;
      font-size: 14px;
      display: flex;
      align-items: center;
      gap: 6px;

      &:hover {
        background: var(--hover-background);
      }

      i {
        font-size: 12px;
      }
    }

    .confirm-btn {
      padding: 8px 16px;
      background: var(--link-color);
      color: var(--text-other);
      border: none;
      border-radius: 4px;
      cursor: pointer;
      font-size: 14px;
      display: flex;
      align-items: center;
      gap: 6px;

      &:hover {
        background: var(--link-hover-color);
      }

      &:disabled {
        background: var(--interaction-button-color);
        cursor: not-allowed;
      }

      i {
        font-size: 12px;
      }
    }
  }
}

// 删除确认弹窗样式
.delete-confirm-modal {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1005; // 确保在其他弹窗之上
}


.delete-confirm-content {
  width: 90%;
  max-width: 400px;
  background: var(--card-background);
  border-radius: 8px;
  box-shadow: 0 4px 12px var(--shadow-color);
  animation: modal-fade-in 0.2s ease;

  .delete-confirm-header {
    padding: 16px;
    border-bottom: 1px solid var(--border-color);
    display: flex;
    justify-content: space-between;
    align-items: center;

    h4 {
      margin: 0;
      font-size: 16px;
      font-weight: 600;
    }

    .close-modal {
      background: transparent;
      border: none;
      font-size: 16px;
      cursor: pointer;
      width: 36px;
      height: 36px;
      display: flex;
      align-items: center;
      justify-content: center;
      border-radius: 50%;
      transition: background 0.2s;
      color: var(--text-secondary);

      &:hover {
        background: var(--hover-background);
      }
    }
  }

  .delete-confirm-body {
    padding: 16px;

    p {
      margin: 0 0 8px 0;
      font-size: 14px;
      color: var(--text-primary);
    }

    .delete-warning {
      color: var(--like-button-active-color);
      font-size: 12px;
      font-style: italic;
    }
  }

  .delete-confirm-footer {
    padding: 12px 16px;
    border-top: 1px solid var(--border-color);
    display: flex;
    justify-content: flex-end;
    gap: 8px;

    .cancel-btn {
      padding: 8px 16px;
      border: 1px solid var(--border-color);
      background: var(--card-background);
      border-radius: 4px;
      cursor: pointer;
      font-size: 14px;
      display: flex;
      align-items: center;
      gap: 6px;

      &:hover {
        background: var(--hover-background);
      }

      i {
        font-size: 12px;
      }
    }

    .confirm-delete-btn {
      padding: 8px 16px;
      background: var(--like-button-active-color);
      color: var(--text-other);
      border: none;
      border-radius: 4px;
      cursor: pointer;
      font-size: 14px;
      display: flex;
      align-items: center;
      gap: 6px;

      &:hover {
        background: var(--like-button-hover-color);
      }

      i {
        font-size: 12px;
      }
    }
  }
}

// 新建会话弹窗样式
.create-session-modal {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1003; // 确保在0, 0.5);
}

.create-session-content {
  width: 90%;
  max-width: 400px;
  background: var(--card-background);
  border-radius: 8px;
  box-shadow: 0 4px 12px var(--shadow-color);
  animation: modal-fade-in 0.2s ease;

  .create-session-header {
    padding: 16px;
    border-bottom: 1px solid var(--border-color);
    display: flex;
    justify-content: space-between;
    align-items: center;

    h4 {
      margin: 0;
      font-size: 16px;
      font-weight: 600;
    }

    .close-modal {
      background: transparent;
      border: none;
      font-size: 16px;
      cursor: pointer;
      width: 36px;
      height: 36px;
      display: flex;
      align-items: center;
      justify-content: center;
      border-radius: 50%;
      transition: background 0.2s;
      color: var(--text-secondary);

      &:hover {
        background: var(--hover-background);
      }
    }
  }

  .create-session-body {
    padding: 16px;

    .session-input {
      width: 100%;
      padding: 10px 12px;
      border: 1px solid var(--border-color);
      border-radius: 4px;
      font-size: 14px;
      outline: none;
      background: var(--card-background);
      color: var(--text-primary);

      &:focus {
        border-color: #1890ff;
      }
    }
  }

  .create-session-footer {
    padding: 12px 16px;
    border-top: 1px solid var(--border-color);
    display: flex;
    justify-content: flex-end;
    gap: 8px;

    .cancel-btn {
      padding: 8px 16px;
      border: 1px solid var(--border-color);
      background: var(--card-background);
      border-radius: 4px;
      cursor: pointer;
      font-size: 14px;
      display: flex;
      align-items: center;
      gap: 6px;

      &:hover {
        background: var(--hover-background);
      }

      i {
        font-size: 12px;
      }
    }

    .confirm-btn {
      padding: 8px 16px;
      background: var(--link-color);
      color: var(--text-other);
      border: none;
      border-radius: 4px;
      cursor: pointer;
      font-size: 14px;
      display: flex;
      align-items: center;
      gap: 6px;

      &:hover {
        background: var(--link-hover-color);
      }

      &:disabled {
        background: var(--interaction-button-color);
        cursor: not-allowed;
      }

      i {
        font-size: 12px;
      }
    }
  }
}

// 弹窗样式
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1004;
}

.modal {
  width: 90%;
  max-width: 400px;
  background: var(--card-background);
  border-radius: 8px;
  box-shadow: 0 4px 12px var(--shadow-color);

  .modal-header {
    padding: 16px;
    border-bottom: 1px solid var(--border-color);
    display: flex;
    justify-content: space-between;
    align-items: center;

    h4 {
      margin: 0;
      font-size: 16px;
      font-weight: 600;
    }

    .close-modal {
      background: transparent;
      border: none;
      font-size: 16px;
      cursor: pointer;
      width: 36px;
      height: 36px;
      display: flex;
      align-items: center;
      justify-content: center;
      border-radius: 50%;
      transition: background 0.2s;
      color: var(--text-secondary);

      &:hover {
        background: var(--hover-background);
      }
    }
  }

  .modal-body {
    padding: 16px;

    .session-input {
      width: 100%;
      padding: 10px 12px;
      border: 1px solid var(--border-color);
      border-radius: 4px;
      font-size: 14px;
      outline: none;
      background: var(--card-background);
      color: var(--text-primary);

      &:focus {
        border-color: var(--link-color);
      }
    }
  }

  .modal-footer {
    padding: 12px 16px;
    border-top: 1px solid var(--border-color);
    display: flex;
    justify-content: flex-end;
    gap: 8px;

    .cancel-btn {
      padding: 8px 16px;
      border: 1px solid var(--border-color);
      background: var(--card-background);
      border-radius: 4px;
      cursor: pointer;
      font-size: 14px;
      display: flex;
      align-items: center;
      gap: 6px;

      &:hover {
        background: var(--hover-background);
      }

      i {
        font-size: 12px;
      }
    }

    .confirm-btn {
      padding: 8px 16px;
      background: var(--link-color);
      color: var(--text-other);
      border: none;
      border-radius: 4px;
      cursor: pointer;
      font-size: 14px;
      display: flex;
      align-items: center;
      gap: 6px;

      &:hover {
        background: var(--link-hover-color);
      }

      &:disabled {
        background: var(--interaction-button-color);
        cursor: not-allowed;
      }

      i {
        font-size: 12px;
      }
    }
  }
}

// FontAwesome 旋转动画
@keyframes fa-spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

// 响应式细节调整
@media (max-width: 768px) {
  .ai-chat-page {
    flex-direction: column;
  }

  .session-sidebar {
    width: 280px;
  }

  // 移动端消息区域顶部留白适配菜单按钮
  .chat-messages {
    padding-top: 60px;
  }
}

@media (min-width: 769px) {
  .mobile-menu-btn {
    display: none !important;
  }

  // PC端防止滚动溢出
  .chat-container, .session-sidebar {
    overflow: hidden;
  }
}

// 消息提示框滑入动画
@keyframes slideDown {
  from {
    top: -50px;
    opacity: 0;
  }
  to {
    top: 20px;
    opacity: 1;
  }
}

// 滚动条样式优化
::-webkit-scrollbar {
  width: 6px;
  height: 6px;
}

::-webkit-scrollbar-track {
  background: var(--hover-background);
  border-radius: 3px;
}

::-webkit-scrollbar-thumb {
  background: var(--text-secondary);
  border-radius: 3px;
}

::-webkit-scrollbar-thumb:hover {
  background: var(--text-primary);
}

// 全局按钮/输入框焦点样式统一
button:focus, input:focus {
  outline: none;
  box-shadow: 0 0 0 2px rgba(var(--link-color-rgb), 0.2);
}

// 模态框动画
.modal {
  animation: modal-fade-in 0.2s ease;
}

@keyframes modal-fade-in {
  from {
    opacity: 0;
    transform: translateY(-20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

// 消息提示框样式
.message-toast {
  position: fixed;
  top: 20px;
  left: 50%;
  transform: translateX(-50%);
  padding: 12px 24px;
  border-radius: 6px;
  color: var(--text-other);
  font-size: 14px;
  z-index: 2000;
  display: flex;
  align-items: center;
  gap: 8px;
  animation: slideDown 0.3s ease;
  box-shadow: 0 4px 12px var(--shadow-color);
  min-width: 200px;
  text-align: center;

  &.success {
    background: var(--ai-avatar-background);
  }

  &.error {
    background: var(--like-button-active-color);
  }

  .message-content {
    display: flex;
    align-items: center;
    gap: 8px;
  }
}

// 抽屉动画优化
.session-sidebar {
  animation-duration: 0.3s;
  animation-timing-function: cubic-bezier(0.78, 0.14, 0.15, 0.86);
}

// 清除上下文按钮样式
.clear-context-container {
  padding: 16px;
  text-align: center;
  margin-top: 8px;

  .clear-context-btn {
    border: 1px solid var(--border-color);
    background: transparent;
    color: var(--text-primary);
    padding: 6px 12px;
    border-radius: 16px;
    cursor: pointer;
    font-size: 12px;
    display: inline-flex;
    align-items: center;
    gap: 4px;
    transition: all 0.2s;

    &:hover {
      background: var(--hover-background);
      border-color: var(--link-color);
    }

    i {
      font-size: 10px;
    }
  }
}

/* 回到底部按钮 */
.scroll-to-bottom-btn {
  position: absolute;
  bottom: 180px;
  right: 30px;
  width: 50px;
  height: 50px;
  border-radius: 50%;
  color: #0a0a0a;

  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;

  z-index: 100;
  transition: all 0.3s ease;
  opacity: 0;
  transform: translateY(20px);
  visibility: hidden;
}

.scroll-to-bottom-btn.show {
  opacity: 1;
  transform: translateY(0);
  visibility: visible;
}

.scroll-to-bottom-btn:hover {
  background-color: var(--primary-hover-color);
  transform: scale(1.1);
  box-shadow: 0 6px 18px rgba(0, 0, 0, 0.3);
  border-color: var(--primary-color);
}

.scroll-to-bottom-btn:active {
  transform: scale(0.9);
}

/* 移动端适配 */
@media (max-width: 768px) {
  .scroll-to-bottom-btn {
    width: 44px;
    height: 44px;
    font-size: 16px;
    bottom: 200px;
    right: 20px;
  }
}
/* 录音弹窗样式 */
.voice-input-modal {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 2000;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
}

.voice-modal-backdrop {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.4);
  backdrop-filter: blur(4px);
}

.voice-modal-card {
  position: relative;
  width: 100%;
  max-width: 400px;
  background: var(--card-background);
  border-radius: 20px;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.2);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  animation: modalSlideUp 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.voice-modal-header {
  padding: 16px 20px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid var(--border-color);

  .voice-status {
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 14px;
    font-weight: 500;
    color: var(--text-primary);

    .status-dot {
      width: 8px;
      height: 8px;
      border-radius: 50%;
      background: #ff4d4f;

      &.pulse {
        animation: dotPulse 1.5s infinite;
      }
    }
  }

  .close-voice {
    background: transparent;
    border: none;
    color: var(--text-secondary);
    cursor: pointer;
    font-size: 18px;
    padding: 4px;
    transition: all 0.2s;

    &:hover {
      color: var(--text-primary);
      transform: rotate(90deg);
    }
  }
}

.voice-modal-content {
  padding: 30px 20px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 20px;
  min-height: 200px;
}

.voice-result-box {
  width: 100%;
  padding: 16px;
  background: var(--hover-background);
  border-radius: 12px;
  min-height: 100px;
  max-height: 200px;
  overflow-y: auto;
  border: 1px dashed var(--border-color);

  p {
    margin: 0;
    font-size: 16px;
    line-height: 1.6;
    color: var(--text-primary);
    word-break: break-all;
  }

  .placeholder {
    color: var(--text-secondary);
    text-align: center;
    font-style: italic;
    margin-top: 20px;
  }

  &.has-content {
    border-style: solid;
    border-color: var(--link-color);
    background: rgba(var(--link-color-rgb), 0.05);
  }
}

.voice-visualizer {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  height: 40px;

  .wave-bar {
    width: 4px;
    height: 10px;
    background: var(--link-color);
    border-radius: 2px;
    animation: voiceWave 1s ease-in-out infinite;
    animation-delay: var(--delay);
  }
}

.voice-modal-footer {
  padding: 16px 20px;
  display: flex;
  gap: 12px;
  border-top: 1px solid var(--border-color);

  button {
    flex: 1;
    padding: 12px;
    border-radius: 12px;
    border: none;
    font-size: 15px;
    font-weight: 500;
    cursor: pointer;
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 8px;
    transition: all 0.2s;

    &:disabled {
      opacity: 0.5;
      cursor: not-allowed;
    }
  }

  .v-cancel-btn {
    background: var(--hover-background);
    color: var(--text-primary);
    &:hover { background: #fee2e2; color: #ef4444; }
  }

  .v-confirm-btn {
    background: var(--link-color);
    color: white;
    &:hover:not(:disabled) { background: var(--link-hover-color); transform: translateY(-2px); }
  }
}

/* 消息输入框按钮动画补全 */
.voice-waves {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 3px;
  pointer-events: none;

  span {
    width: 3px;
    height: 12px;
    background: white;
    border-radius: 2px;
    animation: voiceWave 1s ease-in-out infinite;
    &:nth-child(1) { animation-delay: 0.1s; }
    &:nth-child(2) { animation-delay: 0.3s; }
    &:nth-child(3) { animation-delay: 0.2s; }
  }
}

.voice-btn.is-recording {
  background: #ff4d4f !important;
  color: white !important;
  box-shadow: 0 0 15px rgba(255, 77, 79, 0.4);
}

/* 动画定义 */
@keyframes modalSlideUp {
  from { opacity: 0; transform: translateY(40px) scale(0.95); }
  to { opacity: 1; transform: translateY(0) scale(1); }
}

@keyframes dotPulse {
  0% { transform: scale(1); opacity: 1; }
  50% { transform: scale(1.5); opacity: 0.5; }
  100% { transform: scale(1); opacity: 1; }
}

@keyframes voiceWave {
  0%, 100% { height: 8px; }
  50% { height: 24px; }
}

/* Transition 过渡 */
.fade-scale-enter-active,
.fade-scale-leave-active {
  transition: all 0.3s ease;
}

.fade-scale-enter-from,
.fade-scale-leave-to {
  opacity: 0;
  transform: scale(0.9);
}

/* 响应式调整 */
@media (max-width: 480px) {
  .voice-modal-card {
    border-radius: 20px 20px 0 0;
    position: absolute;
    bottom: 0;
    max-width: none;
    animation: modalSlideUpMobile 0.4s cubic-bezier(0.34, 1.56, 0.64, 1);
  }
}

@keyframes modalSlideUpMobile {
  from { transform: translateY(100%); }
  to { transform: translateY(0); }
}
</style>

