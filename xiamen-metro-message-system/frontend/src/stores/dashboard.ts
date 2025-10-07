import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { DeviceStatus, MessageStats, AlertItem, ChartDataPoint } from '@/types'
import { dashboardApi } from '@/api/dashboard'
import { ElMessage } from 'element-plus'

export const useDashboardStore = defineStore('dashboard', () => {
  // 设备状态数据
  const deviceStatus = ref<DeviceStatus[]>([])
  const deviceStats = ref({
    total: 0,
    online: 0,
    offline: 0,
    warning: 0,
    error: 0
  })

  // 报文统计数据
  const messageStats = ref<MessageStats>({
    todayCount: 0,
    totalCount: 0,
    successRate: 0,
    avgProcessTime: 0
  })

  // 告警信息
  const alerts = ref<AlertItem[]>([])
  const unreadAlerts = ref(0)

  // 图表数据
  const trendData = ref<ChartDataPoint[]>([])
  const deviceTypeData = ref<{ name: string; value: number }[]>([])
  const messageFlowData = ref<ChartDataPoint[]>([])

  // 加载状态
  const loading = ref(false)
  const refreshing = ref(false)

  // 计算属性
  const onlineRate = computed(() => {
    if (deviceStats.value.total === 0) return 0
    return (deviceStats.value.online / deviceStats.value.total * 100).toFixed(1)
  })

  const errorRate = computed(() => {
    if (deviceStats.value.total === 0) return 0
    return (deviceStats.value.error / deviceStats.value.total * 100).toFixed(1)
  })

  // 获取设备状态概览
  const fetchDeviceStatus = async () => {
    try {
      const response = await dashboardApi.getDeviceStatus()
      deviceStatus.value = response.data.devices
      deviceStats.value = response.data.stats
    } catch (error: any) {
      ElMessage.error('获取设备状态失败: ' + error.message)
    }
  }

  // 获取报文统计
  const fetchMessageStats = async () => {
    try {
      const response = await dashboardApi.getMessageStats()
      messageStats.value = response.data
    } catch (error: any) {
      ElMessage.error('获取报文统计失败: ' + error.message)
    }
  }

  // 获取告警信息
  const fetchAlerts = async () => {
    try {
      const response = await dashboardApi.getAlerts()
      alerts.value = response.data.alerts
      unreadAlerts.value = response.data.unreadCount
    } catch (error: any) {
      ElMessage.error('获取告警信息失败: ' + error.message)
    }
  }

  // 获取趋势数据
  const fetchTrendData = async (timeRange: string = '7d') => {
    try {
      const response = await dashboardApi.getTrendData(timeRange)
      trendData.value = response.data
    } catch (error: any) {
      ElMessage.error('获取趋势数据失败: ' + error.message)
    }
  }

  // 获取设备类型分布数据
  const fetchDeviceTypeData = async () => {
    try {
      const response = await dashboardApi.getDeviceTypeData()
      deviceTypeData.value = response.data
    } catch (error: any) {
      ElMessage.error('获取设备类型数据失败: ' + error.message)
    }
  }

  // 获取报文流量数据
  const fetchMessageFlowData = async (timeRange: string = '24h') => {
    try {
      const response = await dashboardApi.getMessageFlowData(timeRange)
      messageFlowData.value = response.data
    } catch (error: any) {
      ElMessage.error('获取报文流量数据失败: ' + error.message)
    }
  }

  // 刷新所有数据
  const refreshAllData = async () => {
    refreshing.value = true
    try {
      await Promise.all([
        fetchDeviceStatus(),
        fetchMessageStats(),
        fetchAlerts(),
        fetchTrendData(),
        fetchDeviceTypeData(),
        fetchMessageFlowData()
      ])
      ElMessage.success('数据刷新成功')
    } catch (error) {
      ElMessage.error('数据刷新失败')
    } finally {
      refreshing.value = false
    }
  }

  // 标记告警为已读
  const markAlertAsRead = async (alertId: string) => {
    try {
      await dashboardApi.markAlertAsRead(alertId)
      const alert = alerts.value.find(a => a.id === alertId)
      if (alert) {
        alert.read = true
        unreadAlerts.value = Math.max(0, unreadAlerts.value - 1)
      }
    } catch (error: any) {
      ElMessage.error('标记告警失败: ' + error.message)
    }
  }

  // 获取仪表板概览数据
  const fetchDashboardOverview = async () => {
    loading.value = true
    try {
      await Promise.all([
        fetchDeviceStatus(),
        fetchMessageStats(),
        fetchAlerts(),
        fetchTrendData('7d'),
        fetchDeviceTypeData(),
        fetchMessageFlowData('24h')
      ])
    } catch (error) {
      console.error('获取仪表板数据失败:', error)
    } finally {
      loading.value = false
    }
  }

  return {
    // 状态
    deviceStatus,
    deviceStats,
    messageStats,
    alerts,
    unreadAlerts,
    trendData,
    deviceTypeData,
    messageFlowData,
    loading,
    refreshing,

    // 计算属性
    onlineRate,
    errorRate,

    // 方法
    fetchDeviceStatus,
    fetchMessageStats,
    fetchAlerts,
    fetchTrendData,
    fetchDeviceTypeData,
    fetchMessageFlowData,
    refreshAllData,
    markAlertAsRead,
    fetchDashboardOverview
  }
})