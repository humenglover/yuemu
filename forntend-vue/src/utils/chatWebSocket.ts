import { useLoginUserStore } from '@/stores/useLoginUserStore'

// 添加类型定义
interface EventHandler {
  (data?: unknown): void
}

interface EventHandlers {
  [key: string]: EventHandler[]
}

interface WebSocketParams {
  pictureId?: number | string
  spaceId?: number | string
  privateChatId?: number | string
  type: 'chat' | 'space' | 'private'
}

interface ChatMessage {
  type: number | 'loadMore' | 'getOnlineUsers' | 'RECALL' | 'heartbeat' | 'HEARTBEAT' | 'message'
  content?: string
  messageId?: number | string
  id?: string | number
  sender?: {
    id: string | number
    userName: string
    userAvatar?: string
    userRole?: string
  } | null
  pictureId?: string | number
  createTime?: string
  replyMessage?: ChatMessage
  isAtAI?: boolean
  [key: string]: unknown
}

interface MessageQueueItem {
  message: ChatMessage
  retryCount: number
  timestamp: number
}

export default class ChatWebSocket {
  private params: {
    pictureId?: string
    spaceId?: string
    privateChatId?: string
    type: 'chat' | 'space' | 'private'
  }
  private socket: WebSocket | null
  private eventHandlers: EventHandlers
  private connecting: boolean
  private reconnectAttempts: number = 0
  private maxReconnectAttempts: number = 5
  private reconnectTimeout: number = 1000 // 初始重连等待时间（毫秒）
  private messageQueue: MessageQueueItem[] = [] // 消息队列
  private processingQueue: boolean = false // 是否正在处理队列
  private maxRetries: number = 3 // 最大重试次数
  private retryInterval: number = 2000 // 重试间隔（毫秒）
  private destroyed: boolean = false // 是否已销毁
  private lastMessageTime: number = 0 // 最后一次发送消息的时间
  private heartbeatInterval: number = 24000 // 心跳间隔（毫秒）
  private heartbeatTimer?: number // 心跳定时器
  private lastHeartbeatResponse: number = 0 // 最后一次收到心跳响应的时间

  constructor(params: WebSocketParams) {
    this.params = {
      ...params,
      pictureId: params.pictureId?.toString(),
      spaceId: params.spaceId?.toString(),
      privateChatId: params.privateChatId?.toString(),
      type: params.type
    }
    this.socket = null
    this.eventHandlers = {}
    this.connecting = false
    this.initHeartbeat()
  }

  /**
   * 初始化心跳机制
   */
  private initHeartbeat() {
    if (this.heartbeatTimer) {
      clearInterval(this.heartbeatTimer)
      console.log('[WebSocket] 重置心跳定时器')
    }

    this.heartbeatTimer = window.setInterval(() => {
      // 如果实例已被销毁，清除定时器
      if (this.destroyed) {
        if (this.heartbeatTimer) {
          clearInterval(this.heartbeatTimer)
          this.heartbeatTimer = undefined
        }
        return
      }

      const now = Date.now()

      // 先检查连接状态
      if (!this.socket || this.socket.readyState !== WebSocket.OPEN) {
        // 只有在非重连状态下且连接曾经成功过才显示断开警告
        if (!this.connecting && this.lastHeartbeatResponse > 0) {
          console.warn('[WebSocket] 心跳检测：连接已断开，等待重连...')
        }
        return
      }

      // 如果超过心跳间隔没有收到响应，且不是首次连接，尝试重连
      if (this.lastHeartbeatResponse > 0 &&
        now - this.lastHeartbeatResponse > this.heartbeatInterval * 2 &&
        !this.connecting) {
        console.warn('[WebSocket] 心跳检测失败，准备重新连接...')
        this.reconnect()
        return
      }

      // 发送心跳
      try {
        const time = new Date().toLocaleTimeString()
        console.log('[WebSocket] 发送心跳信号 >>>', time)
        this.socket.send(JSON.stringify({ type: 'HEARTBEAT', time }))
      } catch (error) {
        console.error('[WebSocket] 发送心跳信号失败:', error)
      }
    }, this.heartbeatInterval)
  }

  /**
   * 重新连接
   */
  private reconnect() {
    if (this.destroyed || this.connecting) {
      return
    }

    this.connecting = true
    if (this.socket) {
      this.socket.onclose = null // 防止触发重连
      this.socket.close()
      this.socket = null
    }

    console.log('[WebSocket] 开始重新连接...')
    this.connect()
  }

  /**
   * 初始化 WebSocket 连接
   */
  connect() {
    // 检查用户是否已登录
    const loginUserStore = useLoginUserStore();
    if (!loginUserStore.loginUser?.id) {
      console.log('[WebSocket] 用户未登录，无法建立连接')
      return
    }

    // 如果已经销毁，不要重新连接
    if (this.destroyed) {
      console.warn('WebSocket已销毁，无法重新连接')
      return
    }

    // 如果正在连接中，不要重复连接
    if (this.isConnecting()) {
      console.log('WebSocket正在连接中，请等待...')
      return
    }

    // 如果已经连接，不要重复连接
    if (this.isConnected()) {
      console.log('WebSocket已经连接')
      return
    }

    this.connecting = true
    console.log('[WebSocket] 开始建立连接...')

    let url = '/api/ws/chat?'
    if (this.params.pictureId) {
      url += `pictureId=${this.params.pictureId}&type=${this.params.type}`
    } else if (this.params.spaceId) {
      url += `spaceId=${this.params.spaceId}&type=${this.params.type}`
    } else if (this.params.privateChatId) {
      url += `privateChatId=${this.params.privateChatId}&type=${this.params.type}`
    }

    url += `&t=${new Date().getTime()}`

    // 使用环境变量中配置的 WebSocket 地址
    const wsHost = import.meta.env.VITE_WS_URL
    this.socket = new WebSocket(`${wsHost}${url}`)
    this.socket.binaryType = 'blob'

    this.socket.onopen = () => {
      console.log('[WebSocket] 连接成功 >>> %s', new Date().toLocaleTimeString())
      this.connecting = false
      this.reconnectAttempts = 0 // 重置重连次数
      this.reconnectTimeout = 1000 // 重置重连等待时间
      this.lastHeartbeatResponse = Date.now() // 重置心跳响应时间
      this.triggerEvent('open', undefined)
      // 连接成功后处理消息队列
      this.processMessageQueue()
      this.sendMessage({
        type: 'getOnlineUsers'
      })
    }

    this.socket.onmessage = (event) => {
      try {
        const message = JSON.parse(event.data) as Record<string, unknown>
        if (message.type === 'HEARTBEAT') {
          this.lastHeartbeatResponse = Date.now()
          console.log('[WebSocket] 收到心跳响应 <<< %s', new Date().toLocaleTimeString())
          return
        }
        this.triggerEvent('message', message)
      } catch (error) {
        console.error('[WebSocket] 消息解析失败:', error)
      }
    }

    this.socket.onclose = (event) => {
      const now = new Date().toLocaleTimeString()
      console.warn('[WebSocket] 连接已断开 - %s（状态码：%d，断开原因：%s）',
        now,
        event.code,
        event.reason || '未知原因'
      )
      this.connecting = false
      this.triggerEvent('close', event)
      this.socket = null

      // 如果实例已销毁，不要重连
      if (this.destroyed) {
        console.log('[WebSocket] 实例已被销毁，停止重连')
        return
      }

      // 尝试重连
      if (this.reconnectAttempts < this.maxReconnectAttempts) {
        const waitSeconds = this.reconnectTimeout / 1000
        console.log('[WebSocket] 准备重连 - 第%d次尝试，将在%d秒后重连...',
          this.reconnectAttempts + 1,
          waitSeconds
        )
        setTimeout(() => {
          this.reconnectAttempts++
          this.reconnectTimeout *= 2 // 指数退避
          this.connect()
        }, this.reconnectTimeout)
      } else {
        console.error('[WebSocket] 重连失败 - 已达到最大重试次数（%d次），停止重连',
          this.maxReconnectAttempts
        )
        this.triggerEvent('error', new Error('WebSocket重连失败'))
      }
    }

    this.socket.onerror = (error) => {
      this.connecting = false
      this.triggerEvent('error', error)
    }
  }

  /**
   * 发送消息
   */
  sendMessage(message: ChatMessage): boolean {
    // 如果连接已销毁，不再发送消息
    if (this.destroyed) {
      console.warn('[WebSocket] 无法发送消息：实例已被销毁')
      return false
    }

    // 更新最后发送消息时间
    this.lastMessageTime = Date.now()

    // 处理消息类型转换
    const processedMessage = {
      ...message,
      // 确保数字类型的消息类型被转换为数字
      type: typeof message.type === 'string' && !isNaN(Number(message.type))
        ? Number(message.type)
        : message.type,
      // 确保 ID 字段为字符串类型
      id: message.id?.toString(),
      messageId: message.messageId?.toString(),
      pictureId: message.pictureId?.toString()
    }

    if (processedMessage.type === 'HEARTBEAT') {
      // 心跳消息直接发送，不进入队列
      try {
        this.socket?.send(JSON.stringify(processedMessage))
        return true
      } catch (error) {
        console.error('[WebSocket] 心跳消息发送失败:', error)
        return false
      }
    }

    // 将消息添加到队列
    this.messageQueue.push({
      message: processedMessage,
      retryCount: 0,
      timestamp: this.lastMessageTime
    })

    // 开始处理队列
    this.processMessageQueue()
    return true
  }

  /**
   * 处理消息队列
   */
  private async processMessageQueue() {
    if (this.processingQueue || this.messageQueue.length === 0) {
      return
    }

    this.processingQueue = true

    while (this.messageQueue.length > 0) {
      const item = this.messageQueue[0]

      // 如果连接未就绪，等待重连
      if (!this.isConnected()) {
        if (item.retryCount < this.maxRetries) {
          item.retryCount++
          await new Promise(resolve => setTimeout(resolve, this.retryInterval))
          continue
        } else {
          // 超过重试次数，移除消息
          this.messageQueue.shift()
          console.error('消息发送失败，已超过最大重试次数:', item.message)
          continue
        }
      }

      try {
        const messageStr = JSON.stringify(item.message)
        this.socket?.send(messageStr)

        // 立即触发本地消息事件，不等待服务器响应
        if (item.message.type === 1 && item.message.content) {  // 私聊消息且有内容
          this.triggerEvent('message', {
            type: 'message',
            message: {
              ...item.message,
              id: Date.now().toString(),  // 临时ID
              sender: useLoginUserStore().loginUser
            }
          })
        }

        this.messageQueue.shift() // 发送成功，移除消息
      } catch (error) {
        console.error('发送消息失败:', error)
        if (item.retryCount < this.maxRetries) {
          item.retryCount++
          await new Promise(resolve => setTimeout(resolve, this.retryInterval))
        } else {
          this.messageQueue.shift() // 超过重试次数，移除消息
        }
      }
    }

    this.processingQueue = false
  }

  /**
   * 撤回消息
   */
  recallMessage(messageId: number | string): boolean {
    return this.sendMessage({
      type: 'RECALL',
      messageId
    })
  }

  /**
   * 添加事件监听
   */
  on(type: string, handler: EventHandler): void {
    if (!this.eventHandlers[type]) {
      this.eventHandlers[type] = []
    }
    this.eventHandlers[type].push(handler)
  }

  /**
   * 触发事件
   */
  private triggerEvent(type: string, data?: unknown): void {
    const handlers = this.eventHandlers[type]
    if (handlers) {
      handlers.forEach((handler: EventHandler) => handler(data))
    }
  }

  /**
   * 检查连接状态
   */
  isConnected(): boolean {
    return this.socket?.readyState === WebSocket.OPEN
  }

  /**
   * 检查是否正在连接中
   */
  isConnecting(): boolean {
    return this.connecting || this.socket?.readyState === WebSocket.CONNECTING
  }

  /**
   * 销毁实例
   */
  destroy() {
    console.log('[WebSocket] 开始销毁实例')
    this.destroyed = true
    // 先清除心跳定时器
    if (this.heartbeatTimer) {
      console.log('[WebSocket] 清除心跳定时器')
      clearInterval(this.heartbeatTimer)
      this.heartbeatTimer = undefined
    }
    // 断开连接
    if (this.socket) {
      console.log('[WebSocket] 关闭WebSocket连接')
      this.socket.onclose = null // 移除 onclose 处理器，防止触发重连
      this.socket.close()
      this.socket = null
    }
    this.messageQueue = []
    this.eventHandlers = {}
    this.reconnectAttempts = 0
    this.connecting = false
    console.log('[WebSocket] 实例销毁完成')
  }

  /**
   * 关闭 WebSocket 连接
   */
  disconnect() {
    if (this.socket) {
      console.log('[WebSocket] 断开连接')
      this.socket.onclose = null // 移除 onclose 处理器，防止触发重连
      this.socket.close()
      this.socket = null
    }
    this.connecting = false
  }
}
