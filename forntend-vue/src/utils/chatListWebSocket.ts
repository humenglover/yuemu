import { ref } from 'vue'
import { message as antMessage } from 'ant-design-vue'

const wsUrl = import.meta.env.VITE_WS_URL

// 消息类型定义
interface EventHandler {
  (data?: unknown): void
}

interface EventHandlers {
  [key: string]: EventHandler[]
}

interface ChatListMessage {
  type: 'CHAT_LIST' | 'UPDATE' | 'DELETE' | 'NEW' | 'ERROR' | 'HEARTBEAT' | 'REQUEST_LIST' | 'UNREAD_COUNTS' | 'UPDATE_UNREAD' | 'UPDATE_LIST' | 'CLEAR_ALL_UNREAD_RESPONSE'
  success?: boolean
  data?: any
  message?: string
  time?: string
  privateChatId?: string | number
  records?: any[]
  total?: number
  current?: number
  size?: number
  error?: any
  totalUnread?: number
  privateUnread?: number
  friendUnread?: number
  unreadCount?: number
}

export class ChatListWebSocket {
  private ws: WebSocket | null = null
  private eventHandlers: EventHandlers = {}
  private connecting: boolean = false
  private reconnectAttempts: number = 0
  private maxReconnectAttempts: number = 5
  private reconnectTimeout: number = 3000
  private heartbeatInterval: number | null = null
  private lastHeartbeatResponse: number = 0
  private userId: string | null = null
  public isConnected = ref(false)
  public unreadCounts = ref({
    totalUnread: 0,
    privateUnread: 0,
    friendUnread: 0
  })

  // 单例模式
  private static instance: ChatListWebSocket | null = null

  private constructor() {}

  public static getInstance(): ChatListWebSocket {
    if (!ChatListWebSocket.instance) {
      ChatListWebSocket.instance = new ChatListWebSocket()
    }
    return ChatListWebSocket.instance
  }

  public async connect(userId: string | number): Promise<void> {
    // 检查用户是否已登录（userId是否存在且不为空）
    if (!userId) {
      console.log('[ChatListWebSocket] 用户未登录，无法建立连接')
      return
    }

    if (this.connecting || this.isConnected.value) {
      console.log('[ChatListWebSocket] 已经连接或正在连接中')
      return
    }

    // 确保userId是字符串
    this.userId = String(userId)
    this.connecting = true

    try {
      console.log('[ChatListWebSocket] 开始连接...')
      const wsEndpoint = `${wsUrl}/api/ws/chat-list?userId=${this.userId}`
      this.ws = new WebSocket(wsEndpoint)

      this.ws.onopen = () => {
        console.log('[ChatListWebSocket] 连接成功')
        this.isConnected.value = true
        this.connecting = false
        this.reconnectAttempts = 0
        this.startHeartbeat()
        this.emit('connect')

        // 连接成功后立即请求聊天列表和未读消息总数
        this.requestChatList({
          current: 1,
          size: 20
        })
        this.requestUnreadCounts()
      }

      this.ws.onmessage = (event) => {
        this.handleMessage(event)
      }

      this.ws.onclose = () => {
        console.log('[ChatListWebSocket] 连接关闭')
        this.isConnected.value = false
        this.connecting = false
        this.stopHeartbeat()
        this.emit('disconnect')
        this.reconnect()
      }

      this.ws.onerror = (error) => {
        console.error('[ChatListWebSocket] 连接错误:', error)
        this.isConnected.value = false
        this.connecting = false
        this.emit('error', error)
      }
    } catch (error) {
      console.error('[ChatListWebSocket] 连接失败:', error)
      this.connecting = false
      this.reconnect()
    }
  }

  private reconnect(): void {
    if (this.reconnectAttempts >= this.maxReconnectAttempts || !this.userId) {
      console.log('[ChatListWebSocket] 重连次数超过限制或没有userId，停止重连')
      return
    }

    this.reconnectAttempts++
    console.log(`[ChatListWebSocket] 尝试第 ${this.reconnectAttempts} 次重连...`)

    setTimeout(() => {
      this.connect(this.userId!)
    }, this.reconnectTimeout)
  }

  private startHeartbeat(): void {
    this.stopHeartbeat()
    this.heartbeatInterval = window.setInterval(() => {
      if (this.isConnected.value) {
        this.sendMessage({ type: 'HEARTBEAT', time: new Date().toISOString() })
      }
    }, 30000)
  }

  private stopHeartbeat(): void {
    if (this.heartbeatInterval !== null) {
      clearInterval(this.heartbeatInterval)
      this.heartbeatInterval = null
    }
  }

  // 发送消息
  public sendMessage(message: any): void {
    if (!this.isConnected.value || !this.ws) {
      console.warn('[ChatListWebSocket] 未连接，无法发送消息')
      // 如果确实没有连接，才尝试重新连接
      if (!this.connecting && this.userId) {
        this.connect(this.userId)
      }
      return
    }

    try {
      // 确保所有ID都是字符串
      if (message.data) {
        if (message.data.userId) {
          message.data.userId = String(message.data.userId)
        }
        if (message.data.targetUserId) {
          message.data.targetUserId = String(message.data.targetUserId)
        }
        if (message.data.privateChatId) {
          message.data.privateChatId = String(message.data.privateChatId)
        }
        if (message.data.id) {
          message.data.id = String(message.data.id)
        }
      }

      console.log('[ChatListWebSocket] 发送消息:', message)
      this.ws.send(JSON.stringify(message))
    } catch (error) {
      console.error('[ChatListWebSocket] 发送消息失败:', error)
      // 只在发送消息确实失败时触发错误事件
      if (error instanceof Error) {
        this.emit('error', error)
      }
    }
  }

  private handleMessage(event: MessageEvent): void {
    try {
      const data: ChatListMessage = JSON.parse(event.data)
      console.log('[ChatListWebSocket] 收到消息:', data)

      if (data.type === 'CLEAR_ALL_UNREAD_RESPONSE') {
        // 处理清除未读消息的响应
        if (data.success) {
          // 清除成功后重新请求未读消息数和聊天列表
          this.requestUnreadCounts()
          this.sendMessage({
            type: 'REQUEST_LIST',
            data: {
              current: 1,
              pageSize: 20
            }
          })
        }
      } else if (data.type === 'CHAT_LIST' && data.records) {
        // 确保所有ID都是字符串
        data.records = data.records.map((record: any) => ({
          ...record,
          id: String(record.id),
          userId: String(record.userId),
          targetUserId: String(record.targetUserId),
          targetUser: record.targetUser ? {
            ...record.targetUser,
            id: String(record.targetUser.id)
          } : null
        }))
      } else if (data.type === 'UNREAD_COUNTS') {
        // 处理未读消息总数
        data.totalUnread = Number(data.totalUnread)
        data.privateUnread = Number(data.privateUnread)
        data.friendUnread = Number(data.friendUnread)
        this.unreadCounts.value = {
          totalUnread: data.totalUnread || 0,
          privateUnread: data.privateUnread || 0,
          friendUnread: data.friendUnread || 0
        }
        // 不触发全局事件，由BasicLayout处理WebSocket消息并触发全局事件
        // 保持本地状态更新以供需要的地方使用
      } else if (data.type === 'UPDATE_UNREAD') {
        // 更新未读消息数
        const privateChatId = String(data.privateChatId)
        const unreadCount = Number(data.unreadCount || 0)
        // 收到单个聊天的未读消息更新后，重新请求总未读数
        this.requestUnreadCounts()
      } else if (data.type === 'UPDATE_LIST') {
        // 收到更新列表通知时，重新请求列表数据和未读消息总数
        this.sendMessage({
          type: 'REQUEST_LIST',
          data: {
            current: 1,
            pageSize: 20
          }
        })
        this.requestUnreadCounts()
      }

      this.emit('message', data)
    } catch (error) {
      console.error('[ChatListWebSocket] 处理消息失败:', error)
      this.emit('error', error)
    }
  }

  public requestChatList(params: { current: number; size: number; searchText?: string; chatType?: string }): void {
    this.sendMessage({
      type: 'REQUEST_LIST',
      data: params
    })
  }

  /**
   * 请求未读消息总数
   */
  public requestUnreadCounts(): void {
    this.sendMessage({
      type: 'REQUEST_UNREAD_COUNTS'
    })
  }

  /**
   * 清除所有未读消息
   */
  public clearAllUnreadMessages(): void {
    this.sendMessage({
      type: 'CLEAR_ALL_UNREAD'
    })
  }

  public on(event: string, handler: EventHandler): void {
    if (!this.eventHandlers[event]) {
      this.eventHandlers[event] = []
    }
    this.eventHandlers[event].push(handler)
  }

  public off(event: string, handler: EventHandler): void {
    if (!this.eventHandlers[event]) return
    this.eventHandlers[event] = this.eventHandlers[event].filter(h => h !== handler)
  }

  private emit(event: string, data?: unknown): void {
    if (!this.eventHandlers[event]) return
    this.eventHandlers[event].forEach(handler => handler(data))
  }

  public disconnect(): void {
    this.stopHeartbeat()
    if (this.ws) {
      this.ws.close()
      this.ws = null
    }
    this.isConnected.value = false
    this.userId = null
    this.reconnectAttempts = 0
  }
}

export const chatListWebSocket = ChatListWebSocket.getInstance()
