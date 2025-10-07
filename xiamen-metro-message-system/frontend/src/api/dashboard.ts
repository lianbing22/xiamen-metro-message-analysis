import { http } from '@/utils/http'
import type { DeviceStatus, MessageStats, AlertItem, ChartDataPoint } from '@/types'

export const dashboardApi = {
  // 获取设备状态概览
  getDeviceStatus: () => {
    return http.get<{
      devices: DeviceStatus[]
      stats: {
        total: number
        online: number
        offline: number
        warning: number
        error: number
      }
    }>('/dashboard/device-status')
  },

  // 获取报文统计
  getMessageStats: () => {
    return http.get<MessageStats>('/dashboard/message-stats')
  },

  // 获取告警信息
  getAlerts: () => {
    return http.get<{
      alerts: AlertItem[]
      unreadCount: number
    }>('/dashboard/alerts')
  },

  // 获取趋势数据
  getTrendData: (timeRange: string) => {
    return http.get<ChartDataPoint[]>('/dashboard/trend-data', {
      params: { timeRange }
    })
  },

  // 获取设备类型分布数据
  getDeviceTypeData: () => {
    return http.get<{ name: string; value: number }[]>('/dashboard/device-type-data')
  },

  // 获取报文流量数据
  getMessageFlowData: (timeRange: string) => {
    return http.get<ChartDataPoint[]>('/dashboard/message-flow-data', {
      params: { timeRange }
    })
  },

  // 标记告警为已读
  markAlertAsRead: (alertId: string) => {
    return http.post(`/dashboard/alerts/${alertId}/read`)
  },

  // 导出仪表板数据
  exportDashboardData: (format: 'pdf' | 'excel') => {
    return http.get(`/dashboard/export?format=${format}`, {
      responseType: 'blob'
    })
  }
}