import { ref } from 'vue'

const wsUrl = import.meta.env.VITE_WS_URL

// 消息类型定义
interface EventHandler {
  (data?: unknown): void
}

interface EventHandlers {
  [key: string]: EventHandler[]
}

interface MessageWebSocketMessage {
  type: 'unread_count_update' | 'HEARTBEAT' | 'ERROR' | 'unread_counts_response'
  data?: any
  message?: string
  time?: string
}

export class MessageWebSocketService {
  private ws: WebSocket | null = null
  private eventHandlers: EventHandlers = {}
  private connecting: boolean = false
  private reconnectAttempts: number = 0
  private maxReconnectAttempts: number = 5
  private reconnectTimeout: number = 3000
  private heartbeatInterval: number | null = null
  private userId: string | null = null
  public isConnected = ref(false)
  private lastHeartbeatResponseTime: number = Date.now()

  // 单例模式
  private static instance: MessageWebSocketService | null = null

  private constructor() {}

  public static getInstance(): MessageWebSocketService {
    if (!MessageWebSocketService.instance) {
      MessageWebSocketService.instance = new MessageWebSocketService()
    }
    return MessageWebSocketService.instance
  }

  public async connect(userId: string | number): Promise<void> {
    // 检查用户是否已登录（userId是否存在且不为空）
    if (!userId) {
      console.log('[MessageWebSocket] 用户未登录，无法建立连接')
      return
    }

    if (this.connecting || this.isConnected.value) {
      console.log('[MessageWebSocket] 已经连接或正在连接中')
      // 如果已经有连接，先断开之前的连接
      if (this.ws && this.isConnected.value) {
        this.disconnect()
      }
      return
    }

    // 确保userId是字符串
    this.userId = String(userId)
    this.connecting = true

    try {
      console.log('[MessageWebSocket] 开始连接...')
      const wsEndpoint = `${wsUrl}/api/ws/message?userId=${this.userId}`
      this.ws = new WebSocket(wsEndpoint)

      this.ws.onopen = () => {
        console.log('[MessageWebSocket] 连接成功')
        this.isConnected.value = true
        this.connecting = false
        this.reconnectAttempts = 0
        // 重置最后心跳响应时间为当前时间
        this.lastHeartbeatResponseTime = Date.now()
        this.startHeartbeat()
        this.emit('connect')

        // 连接成功后立即请求未读消息总数
        console.log('[MessageWebSocket] 连接成功后准备请求未读消息总数')
        this.requestUnreadCounts()
      }

      this.ws.onmessage = (event) => {
        // 处理心跳响应
        try {
          const data = JSON.parse(event.data);
          if (data.type === 'HEARTBEAT') {
            console.log('[MessageWebSocket] 收到心跳响应')
            // 更新最后心跳响应时间
            this.lastHeartbeatResponseTime = Date.now();
            return; // 心跳响应不进行进一步处理
          }
        } catch (e) {
          // 如果不是JSON格式，继续处理
        }
        this.handleMessage(event)
      }

      this.ws.onclose = () => {
        console.log('[MessageWebSocket] 连接关闭')
        this.isConnected.value = false
        this.connecting = false
        this.stopHeartbeat()
        this.emit('disconnect')
        this.reconnect()
      }

      this.ws.onerror = (error) => {
        console.error('[MessageWebSocket] 连接错误:', error)
        this.isConnected.value = false
        this.connecting = false
        this.emit('error', error)
      }
    } catch (error) {
      console.error('[MessageWebSocket] 连接失败:', error)
      this.connecting = false
      this.reconnect()
    }
  }

  private reconnect(): void {
    if (this.reconnectAttempts >= this.maxReconnectAttempts) {
      console.log('[MessageWebSocket] 重连次数超过限制，停止重连')
      this.reconnectAttempts = 0 // 重置重连计数，以便在用户登录后重新尝试
      return
    }

    if (!this.userId) {
      console.log('[MessageWebSocket] 用户未登录，等待用户登录后重试')
      return // 没有用户ID时，不进行重连尝试
    }

    this.reconnectAttempts++
    console.log(`[MessageWebSocket] 尝试第 ${this.reconnectAttempts} 次重连...`)

    // 增加重连间隔，避免频繁重连
    const actualTimeout = this.reconnectTimeout * this.reconnectAttempts
    setTimeout(() => {
      this.connect(this.userId!)
    }, actualTimeout)
  }

  private startHeartbeat(): void {
    this.stopHeartbeat()
    this.heartbeatInterval = window.setInterval(() => {
      if (this.isConnected.value && this.ws) {
        try {
          // 发送心跳消息
          this.ws.send(JSON.stringify({ type: 'HEARTBEAT', time: new Date().toISOString() }))

          // 设置心跳超时检测，如果在一定时间内没有收到响应，则认为连接已断开
          setTimeout(() => {
            if (Date.now() - this.lastHeartbeatResponseTime > 60000) { // 如果超过60秒没有收到心跳响应
              console.warn('[MessageWebSocket] 心跳响应超时，尝试重连')
              this.ws?.close() // 关闭当前连接，触发重连机制
            }
          }, 15000) // 等待15秒看是否有响应
        } catch (error) {
          console.error('[MessageWebSocket] 发送心跳失败:', error)
        }
      }
    }, 30000) // 每30秒发送一次心跳，确保连接保持活跃
  }

  private stopHeartbeat(): void {
    if (this.heartbeatInterval !== null) {
      clearInterval(this.heartbeatInterval)
      this.heartbeatInterval = null
    }
  }

  // 发送消息
  public sendMessage(message: any): void {
    console.log('[MessageWebSocket] 准备发送消息，连接状态:', this.isConnected.value, 'WebSocket状态:', !!this.ws)
    if (!this.isConnected.value || !this.ws) {
      console.warn('[MessageWebSocket] 未连接，无法发送消息')
      // 如果确实没有连接，且有用户ID，尝试重新连接
      if (!this.connecting && this.userId) {
        this.connect(this.userId)
      }
      return
    }

    try {
      console.log('[MessageWebSocket] 发送消息:', message)
      this.ws.send(JSON.stringify(message))
    } catch (error) {
      console.error('[MessageWebSocket] 发送消息失败:', error)
    }
  }

  private handleMessage(event: MessageEvent): void {
    try {
      const data: MessageWebSocketMessage = JSON.parse(event.data)
      console.log('[MessageWebSocket] 收到消息:', data)

      if (data.type === 'unread_count_update') {
        // 触发全局事件，通知其他组件更新未读消息数
        window.dispatchEvent(new CustomEvent('messageUnreadCountsUpdated', {
          detail: data.data
        }))
      }

      // 处理请求未读消息计数的响应
      if (data.type === 'unread_counts_response') {
        // 触发全局事件，通知其他组件更新未读消息数
        window.dispatchEvent(new CustomEvent('messageUnreadCountsUpdated', {
          detail: data.data
        }))
      }

      this.emit('message', data)
    } catch (error) {
      console.error('[MessageWebSocket] 处理消息失败:', error)
      this.emit('error', error)
    }
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

  /**
   * 请求未读消息总数
   */
  public requestUnreadCounts(): void {
    console.log('[MessageWebSocket] 准备请求未读消息总数')
    this.sendMessage({
      type: 'REQUEST_UNREAD_COUNTS'
    })
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

export const messageWebSocketService = MessageWebSocketService.getInstance()
