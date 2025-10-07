import { ElNotification, ElMessage } from 'element-plus'
import type { RealtimeUpdate } from '@/types'

export class WebSocketService {
  private ws: WebSocket | null = null
  private url: string
  private reconnectAttempts = 0
  private maxReconnectAttempts = 5
  private reconnectInterval = 5000
  private heartbeatInterval: number | null = null
  private isConnecting = false
  private messageHandlers: Map<string, ((data: any) => void)[]> = new Map()

  constructor(url: string) {
    this.url = url
  }

  // 连接WebSocket
  connect(): Promise<void> {
    return new Promise((resolve, reject) => {
      if (this.isConnecting || (this.ws && this.ws.readyState === WebSocket.OPEN)) {
        resolve()
        return
      }

      this.isConnecting = true

      try {
        this.ws = new WebSocket(this.url)

        this.ws.onopen = () => {
          console.log('WebSocket连接已建立')
          this.isConnecting = false
          this.reconnectAttempts = 0
          this.startHeartbeat()
          resolve()
        }

        this.ws.onmessage = (event) => {
          this.handleMessage(event.data)
        }

        this.ws.onclose = (event) => {
          console.log('WebSocket连接已关闭:', event.code, event.reason)
          this.isConnecting = false
          this.stopHeartbeat()

          if (!event.wasClean && this.reconnectAttempts < this.maxReconnectAttempts) {
            this.reconnect()
          }
        }

        this.ws.onerror = (error) => {
          console.error('WebSocket错误:', error)
          this.isConnecting = false
          reject(error)
        }
      } catch (error) {
        this.isConnecting = false
        reject(error)
      }
    })
  }

  // 断开连接
  disconnect(): void {
    if (this.heartbeatInterval) {
      clearInterval(this.heartbeatInterval)
      this.heartbeatInterval = null
    }

    if (this.ws) {
      this.ws.close(1000, '主动断开连接')
      this.ws = null
    }
  }

  // 发送消息
  send(message: any): void {
    if (this.ws && this.ws.readyState === WebSocket.OPEN) {
      this.ws.send(JSON.stringify(message))
    } else {
      console.warn('WebSocket未连接，无法发送消息')
    }
  }

  // 注册消息处理器
  onMessage(type: string, handler: (data: any) => void): void {
    if (!this.messageHandlers.has(type)) {
      this.messageHandlers.set(type, [])
    }
    this.messageHandlers.get(type)!.push(handler)
  }

  // 移除消息处理器
  offMessage(type: string, handler?: (data: any) => void): void {
    if (!this.messageHandlers.has(type)) return

    if (handler) {
      const handlers = this.messageHandlers.get(type)!
      const index = handlers.indexOf(handler)
      if (index > -1) {
        handlers.splice(index, 1)
      }
    } else {
      this.messageHandlers.delete(type)
    }
  }

  // 处理接收到的消息
  private handleMessage(data: string): void {
    try {
      const message: RealtimeUpdate = JSON.parse(data)

      // 处理心跳响应
      if (message.type === 'heartbeat') {
        return
      }

      // 触发对应的处理器
      const handlers = this.messageHandlers.get(message.type)
      if (handlers) {
        handlers.forEach(handler => {
          try {
            handler(message.data)
          } catch (error) {
            console.error('消息处理器执行失败:', error)
          }
        })
      }

      // 通知用户
      this.notifyUser(message)
    } catch (error) {
      console.error('解析WebSocket消息失败:', error)
    }
  }

  // 通知用户
  private notifyUser(message: RealtimeUpdate): void {
    const titles = {
      device: '设备状态更新',
      message: '新报文到达',
      alert: '新告警产生',
      system: '系统状态更新'
    }

    const messages = {
      device: `设备 ${message.data.deviceId} 状态变更为 ${message.data.status}`,
      message: `收到来自设备 ${message.data.deviceId} 的新报文`,
      alert: `设备 ${message.data.deviceId} 产生 ${message.data.alertLevel} 级别告警`,
      system: `系统指标 ${message.data.metric} 更新为 ${message.data.value}`
    }

    const title = titles[message.type] || '数据更新'
    const content = messages[message.type] || '收到新的数据更新'

    // 显示通知
    ElNotification({
      title,
      message: content,
      type: message.type === 'alert' ? 'warning' : 'info',
      duration: 5000,
      showClose: true
    })
  }

  // 自动重连
  private reconnect(): void {
    if (this.isConnecting) return

    this.reconnectAttempts++
    console.log(`尝试重连 WebSocket (${this.reconnectAttempts}/${this.maxReconnectAttempts})`)

    setTimeout(() => {
      this.connect().catch(() => {
        // 重连失败，继续尝试
      })
    }, this.reconnectInterval)
  }

  // 开始心跳
  private startHeartbeat(): void {
    this.heartbeatInterval = window.setInterval(() => {
      this.send({ type: 'heartbeat', timestamp: Date.now() })
    }, 30000) // 30秒发送一次心跳
  }

  // 停止心跳
  private stopHeartbeat(): void {
    if (this.heartbeatInterval) {
      clearInterval(this.heartbeatInterval)
      this.heartbeatInterval = null
    }
  }

  // 获取连接状态
  get readyState(): number {
    return this.ws?.readyState ?? WebSocket.CLOSED
  }

  // 是否已连接
  get isConnected(): boolean {
    return this.ws?.readyState === WebSocket.OPEN
  }
}

// 创建全局WebSocket实例
export const wsService = new WebSocketService(
  `${window.location.protocol === 'https:' ? 'wss:' : 'ws:'}//${window.location.host}/ws`
)

// 模拟WebSocket服务（用于开发环境）
export class MockWebSocketService {
  private messageHandlers: Map<string, ((data: any) => void)[]> = new Map()
  private interval: number | null = null

  connect(): Promise<void> {
    return new Promise((resolve) => {
      // 模拟连接延迟
      setTimeout(() => {
        console.log('模拟WebSocket连接已建立')
        this.startMockUpdates()
        resolve()
      }, 1000)
    })
  }

  disconnect(): void {
    if (this.interval) {
      clearInterval(this.interval)
      this.interval = null
    }
    console.log('模拟WebSocket连接已断开')
  }

  send(message: any): void {
    console.log('模拟发送消息:', message)
  }

  onMessage(type: string, handler: (data: any) => void): void {
    if (!this.messageHandlers.has(type)) {
      this.messageHandlers.set(type, [])
    }
    this.messageHandlers.get(type)!.push(handler)
  }

  offMessage(type: string, handler?: (data: any) => void): void {
    if (!this.messageHandlers.has(type)) return

    if (handler) {
      const handlers = this.messageHandlers.get(type)!
      const index = handlers.indexOf(handler)
      if (index > -1) {
        handlers.splice(index, 1)
      }
    } else {
      this.messageHandlers.delete(type)
    }
  }

  private startMockUpdates(): void {
    this.interval = window.setInterval(() => {
      // 随机生成更新消息
      const updateTypes = ['device', 'message', 'alert', 'system']
      const randomType = updateTypes[Math.floor(Math.random() * updateTypes.length)]

      const mockData = this.generateMockData(randomType)

      // 触发处理器
      const handlers = this.messageHandlers.get(randomType)
      if (handlers) {
        handlers.forEach(handler => {
          try {
            handler(mockData)
          } catch (error) {
            console.error('消息处理器执行失败:', error)
          }
        })
      }

      // 显示通知
      this.notifyUser(randomType, mockData)
    }, Math.random() * 20000 + 10000) // 10-30秒随机间隔
  }

  private generateMockData(type: string) {
    const deviceTypes = ['闸机', '售票机', '安检仪', '电梯', '扶梯']
    const locations = ['1号线-厦门站', '1号线-镇海路站', '2号线-海沧湾公园站']
    const alertTypes = ['设备离线', '通信异常', '性能下降', '温度异常']

    switch (type) {
      case 'device':
        return {
          deviceId: `DEV${String(Math.floor(Math.random() * 50) + 1).padStart(4, '0')}`,
          deviceName: `${deviceTypes[Math.floor(Math.random() * deviceTypes.length)]}-${Math.floor(Math.random() * 10) + 1}`,
          status: ['online', 'offline', 'warning', 'error'][Math.floor(Math.random() * 4)],
          location: locations[Math.floor(Math.random() * locations.length)],
          timestamp: new Date().toISOString()
        }
      case 'message':
        return {
          messageId: `MSG${String(Math.floor(Math.random() * 100000) + 1).padStart(8, '0')}`,
          deviceId: `DEV${String(Math.floor(Math.random() * 50) + 1).padStart(4, '0')}`,
          messageType: ['HEARTBEAT', 'STATUS', 'ALERT', 'DATA'][Math.floor(Math.random() * 4)],
          timestamp: new Date().toISOString()
        }
      case 'alert':
        return {
          alertId: `ALT${String(Math.floor(Math.random() * 10000) + 1).padStart(6, '0')}`,
          deviceId: `DEV${String(Math.floor(Math.random() * 50) + 1).padStart(4, '0')}`,
          deviceName: `${deviceTypes[Math.floor(Math.random() * deviceTypes.length)]}-${Math.floor(Math.random() * 10) + 1}`,
          alertType: alertTypes[Math.floor(Math.random() * alertTypes.length)],
          alertLevel: ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'][Math.floor(Math.random() * 4)],
          title: `${alertTypes[Math.floor(Math.random() * alertTypes.length)]}告警`,
          content: '设备状态异常，请及时处理',
          timestamp: new Date().toISOString()
        }
      case 'system':
        return {
          metric: ['cpu', 'memory', 'disk', 'network'][Math.floor(Math.random() * 4)],
          value: Math.floor(Math.random() * 100),
          timestamp: new Date().toISOString()
        }
      default:
        return {}
    }
  }

  private notifyUser(type: string, data: any): void {
    const titles = {
      device: '设备状态更新',
      message: '新报文到达',
      alert: '新告警产生',
      system: '系统状态更新'
    }

    const messages = {
      device: `设备 ${data.deviceId || data.deviceName} 状态变更为 ${data.status}`,
      message: `收到来自设备 ${data.deviceId} 的新报文`,
      alert: `设备 ${data.deviceName || data.deviceId} 产生 ${data.alertLevel} 级别告警`,
      system: `系统指标 ${data.metric} 更新为 ${data.value}%`
    }

    const title = titles[type as keyof typeof titles] || '数据更新'
    const content = messages[type as keyof typeof messages] || '收到新的数据更新'

    ElNotification({
      title,
      message: content,
      type: type === 'alert' ? 'warning' : 'info',
      duration: 5000,
      showClose: true
    })
  }

  get readyState(): number {
    return WebSocket.OPEN
  }

  get isConnected(): boolean {
    return true
  }
}

// 开发环境使用模拟服务
export const realtimeService = import.meta.env.DEV ? new MockWebSocketService() : wsService