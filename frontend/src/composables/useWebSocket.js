import { ref, onUnmounted } from 'vue'

export class WebSocketManager {
  constructor(url) {
    this.url = url
    this.ws = null
    this.reconnectAttempts = 0
    this.maxReconnectAttempts = 5
    this.reconnectInterval = 3000
    this.handlers = new Map()
    this.heartbeatInterval = null
    this.isManualClose = false
  }

  connect() {
    try {
      this.ws = new WebSocket(this.url)
      this.setupEventListeners()
      this.startHeartbeat()
    } catch (error) {
      console.error('WebSocket连接失败:', error)
      this.reconnect()
    }
  }

  setupEventListeners() {
    this.ws.onopen = () => {
      console.log('WebSocket连接已建立')
      this.reconnectAttempts = 0
      this.isManualClose = false
    }

    this.ws.onmessage = (event) => {
      try {
        const data = JSON.parse(event.data)
        this.handleMessage(data)
      } catch (error) {
        console.error('解析WebSocket消息失败:', error)
      }
    }

    this.ws.onclose = () => {
      console.log('WebSocket连接已关闭')
      this.stopHeartbeat()

      if (!this.isManualClose) {
        this.reconnect()
      }
    }

    this.ws.onerror = (error) => {
      console.error('WebSocket错误:', error)
    }
  }

  handleMessage(data) {
    const { type, payload } = data
    const handlers = this.handlers.get(type) || []

    handlers.forEach(handler => {
      try {
        handler(payload)
      } catch (error) {
        console.error('WebSocket消息处理失败:', error)
      }
    })

    // 触发通用消息处理器
    const allHandlers = this.handlers.get('*') || []
    allHandlers.forEach(handler => {
      try {
        handler({ type, payload })
      } catch (error) {
        console.error('WebSocket通用消息处理失败:', error)
      }
    })
  }

  subscribe(type, handler) {
    if (!this.handlers.has(type)) {
      this.handlers.set(type, [])
    }
    this.handlers.get(type).push(handler)
  }

  unsubscribe(type, handler) {
    const handlers = this.handlers.get(type)
    if (handlers) {
      const index = handlers.indexOf(handler)
      if (index > -1) {
        handlers.splice(index, 1)
      }
    }
  }

  send(data) {
    if (this.ws && this.ws.readyState === WebSocket.OPEN) {
      this.ws.send(JSON.stringify(data))
    } else {
      console.warn('WebSocket未连接，无法发送消息')
    }
  }

  startHeartbeat() {
    this.heartbeatInterval = setInterval(() => {
      if (this.ws && this.ws.readyState === WebSocket.OPEN) {
        this.send({ type: 'ping' })
      }
    }, 30000) // 30秒发送一次心跳
  }

  stopHeartbeat() {
    if (this.heartbeatInterval) {
      clearInterval(this.heartbeatInterval)
      this.heartbeatInterval = null
    }
  }

  reconnect() {
    if (this.reconnectAttempts < this.maxReconnectAttempts) {
      this.reconnectAttempts++
      setTimeout(() => {
        console.log(`WebSocket重连尝试 ${this.reconnectAttempts}/${this.maxReconnectAttempts}`)
        this.connect()
      }, this.reconnectInterval)
    } else {
      console.error('WebSocket重连次数已达上限')
    }
  }

  disconnect() {
    this.isManualClose = true
    this.stopHeartbeat()

    if (this.ws) {
      this.ws.close()
      this.ws = null
    }

    this.handlers.clear()
  }

  getConnectionState() {
    if (!this.ws) return WebSocket.CLOSED
    return this.ws.readyState
  }

  isConnected() {
    return this.ws && this.ws.readyState === WebSocket.OPEN
  }
}

export function useWebSocket(projectId) {
  const wsManager = ref(null)
  const isConnected = ref(false)

  const connect = () => {
    const wsUrl = `${import.meta.env.VITE_WS_BASE_URL}/analysis/${projectId}`
    wsManager.value = new WebSocketManager(wsUrl)
    wsManager.value.connect()

    // 监听连接状态变化
    wsManager.value.subscribe('*', () => {
      isConnected.value = wsManager.value.isConnected()
    })
  }

  const subscribe = (type, handler) => {
    if (wsManager.value) {
      wsManager.value.subscribe(type, handler)
    }
  }

  const unsubscribe = (type, handler) => {
    if (wsManager.value) {
      wsManager.value.unsubscribe(type, handler)
    }
  }

  const send = (data) => {
    if (wsManager.value) {
      wsManager.value.send(data)
    }
  }

  const disconnect = () => {
    if (wsManager.value) {
      wsManager.value.disconnect()
      isConnected.value = false
    }
  }

  // 组件卸载时自动断开连接
  onUnmounted(() => {
    disconnect()
  })

  return {
    wsManager: readonly(wsManager),
    isConnected: readonly(isConnected),
    connect,
    subscribe,
    unsubscribe,
    send,
    disconnect
  }
}