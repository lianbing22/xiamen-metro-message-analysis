import type { DeviceStatus, MessageStats, AlertItem, ChartDataPoint } from '@/types'

// 设备类型列表
const deviceTypes = ['闸机', '售票机', '安检仪', '电梯', '扶梯', '照明系统', '空调系统', '消防系统', '监控设备', '广播系统']

// 位置列表
const locations = ['1号线-厦门站', '1号线-镇海路站', '1号线-中山公园站', '1号线-将军祠站', '1号线-湖滨东路站',
                   '2号线-海沧湾公园站', '2号线-海沧商务区站', '2号线-海沧体育中心站', '2号线-马青路站', '2号线-翁角路站']

// 告警类型列表
const alertTypes = ['设备离线', '通信异常', '性能下降', '内存溢出', '磁盘空间不足', '温度异常', '网络延迟', '数据丢失']

// 生成设备状态数据
export const generateDeviceStatus = (count: number = 50): DeviceStatus[] => {
  const devices: DeviceStatus[] = []

  for (let i = 0; i < count; i++) {
    const statusOptions: Array<'online' | 'offline' | 'warning' | 'error'> = ['online', 'offline', 'warning', 'error']
    const weights = [0.7, 0.15, 0.1, 0.05] // 在线概率70%，离线15%，警告10%，错误5%
    const status = weightedRandom(statusOptions, weights)

    devices.push({
      id: `device-${i + 1}`,
      deviceId: `DEV${String(i + 1).padStart(4, '0')}`,
      deviceName: `${deviceTypes[Math.floor(Math.random() * deviceTypes.length)]}-${i + 1}`,
      deviceType: deviceTypes[Math.floor(Math.random() * deviceTypes.length)],
      status,
      lastSeen: generateRandomTimestamp(status === 'online' ? 1 : 24), // 在线设备1小时内，离线设备24小时内
      location: locations[Math.floor(Math.random() * locations.length)],
      ipAddress: `192.168.${Math.floor(Math.random() * 255)}.${Math.floor(Math.random() * 255)}`,
      cpuUsage: status === 'online' ? Math.random() * 80 : 0,
      memoryUsage: status === 'online' ? Math.random() * 90 : 0,
      messageCount: Math.floor(Math.random() * 10000)
    })
  }

  return devices
}

// 生成报文统计数据
export const generateMessageStats = (): MessageStats => {
  const todayCount = Math.floor(Math.random() * 50000) + 10000
  const totalCount = Math.floor(Math.random() * 1000000) + 100000
  const successRate = 85 + Math.random() * 14 // 85-99%成功率
  const avgProcessTime = Math.random() * 100 + 50 // 50-150ms

  return {
    todayCount,
    totalCount,
    successRate,
    avgProcessTime,
    errorCount: Math.floor(todayCount * (1 - successRate / 100)),
    warningCount: Math.floor(todayCount * 0.05)
  }
}

// 生成告警数据
export const generateAlerts = (count: number = 20): AlertItem[] => {
  const alerts: AlertItem[] = []
  const levels: Array<'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL'> = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL']

  for (let i = 0; i < count; i++) {
    const level = levels[Math.floor(Math.random() * levels.length)]
    alerts.push({
      id: `alert-${i + 1}`,
      alertId: `ALT${String(i + 1).padStart(6, '0')}`,
      deviceId: `DEV${String(Math.floor(Math.random() * 50) + 1).padStart(4, '0')}`,
      deviceName: `${deviceTypes[Math.floor(Math.random() * deviceTypes.length)]}-${Math.floor(Math.random() * 10) + 1}`,
      alertType: alertTypes[Math.floor(Math.random() * alertTypes.length)],
      alertLevel: level,
      title: `${alertTypes[Math.floor(Math.random() * alertTypes.length)]}告警`,
      content: generateAlertContent(alertTypes[Math.floor(Math.random() * alertTypes.length)]),
      timestamp: generateRandomTimestamp(24),
      read: Math.random() > 0.3, // 70%未读
      acknowledged: Math.random() > 0.8,
      resolved: Math.random() > 0.9
    })
  }

  return alerts.sort((a, b) => new Date(b.timestamp).getTime() - new Date(a.timestamp).getTime())
}

// 生成趋势数据
export const generateTrendData = (days: number = 7): ChartDataPoint[] => {
  const data: ChartDataPoint[] = []
  const now = new Date()

  for (let i = days - 1; i >= 0; i--) {
    const date = new Date(now)
    date.setDate(date.getDate() - i)
    date.setHours(0, 0, 0, 0)

    data.push({
      timestamp: date.toISOString(),
      value: Math.floor(Math.random() * 50000) + 30000,
      label: date.toLocaleDateString('zh-CN', { month: 'short', day: 'numeric' })
    })
  }

  return data
}

// 生成设备类型分布数据
export const generateDeviceTypeData = () => {
  return deviceTypes.map(type => ({
    name: type,
    value: Math.floor(Math.random() * 20) + 5
  }))
}

// 生成报文流量数据（24小时）
export const generateMessageFlowData = (hours: number = 24): ChartDataPoint[] => {
  const data: ChartDataPoint[] = []
  const now = new Date()

  for (let i = hours - 1; i >= 0; i--) {
    const date = new Date(now)
    date.setHours(date.getHours() - i)
    date.setMinutes(0, 0, 0)

    // 模拟高峰期流量
    const hour = date.getHours()
    let baseValue = 1000
    if (hour >= 7 && hour <= 9) baseValue = 3000 // 早高峰
    else if (hour >= 17 && hour <= 19) baseValue = 3500 // 晚高峰
    else if (hour >= 22 || hour <= 5) baseValue = 500 // 深夜

    data.push({
      timestamp: date.toISOString(),
      value: Math.floor(baseValue + Math.random() * 500),
      label: date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
    })
  }

  return data
}

// 生成随机时间戳
function generateRandomTimestamp(hoursAgo: number): string {
  const now = new Date()
  const timestamp = new Date(now.getTime() - Math.random() * hoursAgo * 60 * 60 * 1000)
  return timestamp.toISOString()
}

// 加权随机选择
function weightedRandom<T>(options: T[], weights: number[]): T {
  const totalWeight = weights.reduce((sum, weight) => sum + weight, 0)
  let random = Math.random() * totalWeight

  for (let i = 0; i < options.length; i++) {
    random -= weights[i]
    if (random <= 0) {
      return options[i]
    }
  }

  return options[options.length - 1]
}

// 生成告警内容
function generateAlertContent(alertType: string): string {
  const contents: Record<string, string[]> = {
    '设备离线': [
      '设备通信连接中断，无法获取设备状态信息',
      '网络连接超时，设备可能已断电或网络故障',
      '连续3次心跳检测失败，设备处于离线状态'
    ],
    '通信异常': [
      '数据传输延迟超过阈值，可能存在网络拥堵',
      '通信协议错误，数据包校验失败',
      '设备响应超时，通信链路不稳定'
    ],
    '性能下降': [
      'CPU使用率持续超过80%，设备负载过高',
      '内存使用率超过90%，系统资源不足',
      '响应时间显著增加，性能明显下降'
    ],
    '内存溢出': [
      '可用内存不足，系统频繁进行垃圾回收',
      '内存泄漏检测到，进程占用内存持续增长',
      '内存使用率达到100%，系统运行缓慢'
    ],
    '磁盘空间不足': [
      '磁盘使用率超过95%，剩余空间严重不足',
      '日志文件过大，占用大量磁盘空间',
      '数据存储空间即将耗尽，需要及时清理'
    ],
    '温度异常': [
      '设备温度超过安全阈值，存在过热风险',
      '散热系统异常，温度持续升高',
      '环境温度过高，影响设备正常运行'
    ],
    '网络延迟': [
      '网络延迟超过500ms，影响实时数据处理',
      '丢包率异常，数据传输不稳定',
      '带宽使用率过高，网络性能下降'
    ],
    '数据丢失': [
      '数据校验失败，可能存在数据丢失',
      '数据库连接异常，数据写入失败',
      '缓存数据丢失，需要重新同步'
    ]
  }

  const typeContents = contents[alertType] || ['未知异常，需要进一步检查']
  return typeContents[Math.floor(Math.random() * typeContents.length)]
}

// 模拟实时数据更新
export const simulateRealtimeUpdate = () => {
  const updateTypes = ['device', 'message', 'alert', 'system']
  const actions = ['create', 'update', 'delete']

  return {
    type: updateTypes[Math.floor(Math.random() * updateTypes.length)],
    action: actions[Math.floor(Math.random() * actions.length)],
    data: generateRandomUpdateData(),
    timestamp: Date.now()
  }
}

function generateRandomUpdateData() {
  const updateType = Math.floor(Math.random() * 4)

  switch (updateType) {
    case 0: // device update
      return {
        deviceId: `DEV${String(Math.floor(Math.random() * 50) + 1).padStart(4, '0')}`,
        status: ['online', 'offline', 'warning', 'error'][Math.floor(Math.random() * 4)],
        timestamp: new Date().toISOString()
      }
    case 1: // message update
      return {
        messageId: `MSG${String(Math.floor(Math.random() * 100000) + 1).padStart(8, '0')}`,
        deviceId: `DEV${String(Math.floor(Math.random() * 50) + 1).padStart(4, '0')}`,
        messageType: ['HEARTBEAT', 'STATUS', 'ALERT', 'DATA'][Math.floor(Math.random() * 4)],
        timestamp: new Date().toISOString()
      }
    case 2: // alert update
      return {
        alertId: `ALT${String(Math.floor(Math.random() * 10000) + 1).padStart(6, '0')}`,
        deviceId: `DEV${String(Math.floor(Math.random() * 50) + 1).padStart(4, '0')}`,
        alertLevel: ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'][Math.floor(Math.random() * 4)],
        timestamp: new Date().toISOString()
      }
    default: // system update
      return {
        metric: ['cpu', 'memory', 'disk', 'network'][Math.floor(Math.random() * 4)],
        value: Math.random() * 100,
        timestamp: new Date().toISOString()
      }
  }
}