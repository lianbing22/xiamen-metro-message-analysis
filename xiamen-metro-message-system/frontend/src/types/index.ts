// 通用响应接口
export interface ApiResponse<T = any> {
  code: number
  message: string
  data: T
  success: boolean
  timestamp: number
}

// 分页请求参数
export interface PageParams {
  current: number
  size: number
  total?: number
}

// 分页响应数据
export interface PageResult<T> {
  records: T[]
  total: number
  size: number
  current: number
  pages: number
}

// 设备相关类型
export interface Device {
  id: number
  deviceId: string
  deviceName: string
  deviceType: string
  deviceGroup: string
  location: string
  ipAddress: string
  port: number
  protocol: string
  status: 'ACTIVE' | 'INACTIVE' | 'MAINTENANCE'
  description: string
  createdAt: string
  updatedAt: string
  createdBy: string
  updatedBy: string
}

// 报文相关类型
export interface Message {
  id: number
  messageId: string
  deviceId: string
  messageType: string
  rawMessage: string
  parsedMessage: Record<string, any>
  messageStatus: string
  timestamp: string
  receivedAt: string
  sequenceNumber: number
  checksum: string
  device?: Device
}

// 分析结果相关类型
export interface AnalysisResult {
  id: number
  messageId: string
  deviceId: string
  analysisType: string
  analysisResult: Record<string, any>
  confidenceScore: number
  analysisStatus: string
  errorMessage: string
  analyzedAt: string
  analyzedBy: string
  message?: Message
  device?: Device
}

// 告警相关类型
export interface Alert {
  id: number
  alertId: string
  deviceId: string
  messageId: string
  alertType: string
  alertLevel: 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL'
  alertTitle: string
  alertContent: string
  alertStatus: 'ACTIVE' | 'ACKNOWLEDGED' | 'RESOLVED'
  triggeredAt: string
  acknowledgedAt: string
  acknowledgedBy: string
  resolvedAt: string
  resolvedBy: string
  additionalData: Record<string, any>
  device?: Device
  message?: Message
}

// 用户相关类型
export interface User {
  id: number
  userId: string
  username: string
  email: string
  fullName: string
  phone: string
  department: string
  role: 'ADMIN' | 'OPERATOR' | 'ANALYST' | 'USER'
  status: 'ACTIVE' | 'INACTIVE'
  lastLoginAt: string
  createdAt: string
  updatedAt: string
}

// 登录相关类型
export interface LoginForm {
  username: string
  password: string
  captcha?: string
}

export interface LoginResponse {
  token: string
  refreshToken: string
  user: User
  expiresIn: number
}

// 系统配置类型
export interface SystemConfig {
  id: number
  configKey: string
  configValue: string
  configType: string
  description: string
  isEncrypted: boolean
  createdAt: string
  updatedAt: string
  updatedBy: string
}

// 操作日志类型
export interface OperationLog {
  id: number
  logId: string
  userId: string
  operationType: string
  operationObject: string
  operationDescription: string
  ipAddress: string
  userAgent: string
  operationResult: string
  operationTime: string
  user?: User
}

// 统计数据类型
export interface DashboardStats {
  deviceCount: {
    total: number
    active: number
    inactive: number
    maintenance: number
  }
  messageCount: {
    today: number
    thisWeek: number
    thisMonth: number
    total: number
  }
  alertCount: {
    active: number
    critical: number
    high: number
    medium: number
    low: number
  }
  systemStatus: {
    cpuUsage: number
    memoryUsage: number
    diskUsage: number
    networkStatus: string
  }
}

// 图表数据类型
export interface ChartData {
  timestamps: string[]
  values: number[]
  categories?: string[]
}

// 搜索表单类型
export interface SearchForm {
  keyword?: string
  deviceId?: string
  messageType?: string
  alertLevel?: string
  status?: string
  startTime?: string
  endTime?: string
}

// 导出参数类型
export interface ExportParams {
  format: 'excel' | 'csv' | 'pdf'
  fields: string[]
  filters: Record<string, any>
}

// WebSocket消息类型
export interface WebSocketMessage {
  type: string
  data: any
  timestamp: number
}

// 菜单项类型
export interface MenuItem {
  id: string
  title: string
  path: string
  icon?: string
  children?: MenuItem[]
  roles?: string[]
}

// 主题配置类型
export interface ThemeConfig {
  mode: 'light' | 'dark'
  primaryColor: string
  layout: 'vertical' | 'horizontal'
  sidebarCollapsed: boolean
}

// 仪表板相关类型
export interface DeviceStatus {
  id: string
  deviceId: string
  deviceName: string
  deviceType: string
  status: 'online' | 'offline' | 'warning' | 'error'
  lastSeen: string
  location: string
  ipAddress: string
  cpuUsage?: number
  memoryUsage?: number
  messageCount?: number
}

export interface MessageStats {
  todayCount: number
  totalCount: number
  successRate: number
  avgProcessTime: number
  errorCount?: number
  warningCount?: number
}

export interface AlertItem {
  id: string
  alertId: string
  deviceId: string
  deviceName: string
  alertType: string
  alertLevel: 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL'
  title: string
  content: string
  timestamp: string
  read: boolean
  acknowledged?: boolean
  resolved?: boolean
}

export interface ChartDataPoint {
  timestamp: string
  value: number
  category?: string
  label?: string
}

// 实时数据更新类型
export interface RealtimeUpdate {
  type: 'device' | 'message' | 'alert' | 'system'
  action: 'create' | 'update' | 'delete'
  data: any
  timestamp: number
}

// 导出配置类型
export interface ExportConfig {
  format: 'pdf' | 'excel' | 'csv'
  includeCharts: boolean
  dateRange: {
    start: string
    end: string
  }
  sections: string[]
}