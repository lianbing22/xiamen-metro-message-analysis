<template>
  <div class="dashboard" ref="dashboardRef">
    <!-- 页面头部 -->
    <div class="dashboard__header">
      <div class="dashboard__header-left">
        <h1 class="dashboard__title">设备报文分析仪表板</h1>
        <p class="dashboard__subtitle">实时监控厦门地铁设备状态和报文数据</p>
      </div>
      <div class="dashboard__header-right">
        <el-button-group>
          <el-button
            type="primary"
            :icon="Refresh"
            @click="refreshAllData"
            :loading="refreshing"
          >
            刷新数据
          </el-button>
          <el-dropdown @command="handleExport">
            <el-button type="primary">
              导出报告
              <el-icon class="el-icon--right"><ArrowDown /></el-icon>
            </el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="pdf">导出为 PDF</el-dropdown-item>
                <el-dropdown-item command="excel">导出为 Excel</el-dropdown-item>
                <el-dropdown-item command="image">导出图表图片</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </el-button-group>
      </div>
    </div>

    <!-- 统计卡片 -->
    <div class="dashboard__stats">
      <el-row :gutter="20">
        <el-col :xs="24" :sm="12" :md="6" :lg="6" :xl="6">
          <StatusCard
            title="今日报文数"
            :value="messageStats.todayCount"
            type="primary"
            :icon="Message"
            :trend="messageTrend"
            :loading="loading"
            description="系统接收到的报文总数"
          />
        </el-col>
        <el-col :xs="24" :sm="12" :md="6" :lg="6" :xl="6">
          <StatusCard
            title="成功率"
            :value="messageStats.successRate"
            type="success"
            :icon="CircleCheck"
            unit="%"
            :trend="successRateTrend"
            :loading="loading"
            description="报文处理成功率"
            :show-progress="true"
            :progress="messageStats.successRate"
          />
        </el-col>
        <el-col :xs="24" :sm="12" :md="6" :lg="6" :xl="6">
          <StatusCard
            title="在线设备"
            :value="deviceStats.online"
            type="success"
            :icon="Monitor"
            :trend="deviceTrend"
            :loading="loading"
            description="当前在线设备数量"
          />
        </el-col>
        <el-col :xs="24" :sm="12" :md="6" :lg="6" :xl="6">
          <StatusCard
            title="活跃告警"
            :value="unreadAlerts"
            type="danger"
            :icon="Warning"
            :trend="alertTrend"
            :loading="loading"
            description="待处理的告警数量"
          />
        </el-col>
      </el-row>
    </div>

    <!-- 图表区域 -->
    <div class="dashboard__charts">
      <el-row :gutter="20">
        <!-- 设备状态概览 -->
        <el-col :xs="24" :sm="24" :md="12" :lg="8" :xl="8">
          <DeviceStatusCard
            :device-stats="deviceStats"
            :devices="deviceStatus"
          />
        </el-col>

        <!-- 报文流量趋势 -->
        <el-col :xs="24" :sm="24" :md="12" :lg="8" :xl="8">
          <el-card class="dashboard__chart-card" shadow="hover">
            <template #header>
              <div class="dashboard__chart-header">
                <span>24小时报文流量</span>
                <el-radio-group v-model="messageFlowTimeRange" size="small">
                  <el-radio-button label="1h">1小时</el-radio-button>
                  <el-radio-button label="24h">24小时</el-radio-button>
                  <el-radio-button label="7d">7天</el-radio-button>
                </el-radio-group>
              </div>
            </template>
            <LineChart
              :data="[{
                name: '报文数量',
                data: messageFlowChartData,
                type: 'area',
                color: '#1890ff'
              }]"
              :x-axis="{ type: 'category', data: messageFlowLabels }"
              :y-axis="{ name: '数量' }"
              height="300px"
            />
          </el-card>
        </el-col>

        <!-- 设备类型分布 -->
        <el-col :xs="24" :sm="24" :md="12" :lg="8" :xl="8">
          <el-card class="dashboard__chart-card" shadow="hover">
            <template #header>
              <span>设备类型分布</span>
            </template>
            <PieChart
              :data="deviceTypeData"
              height="300px"
              :show-legend="false"
              :label="{ show: true, formatter: '{b}: {c} ({d}%)' }"
            />
          </el-card>
        </el-col>

        <!-- 历史趋势分析 -->
        <el-col :xs="24" :sm="24" :md="12" :lg="12" :xl="12">
          <el-card class="dashboard__chart-card" shadow="hover">
            <template #header>
              <div class="dashboard__chart-header">
                <span>历史趋势分析</span>
                <el-radio-group v-model="trendTimeRange" size="small">
                  <el-radio-button label="7d">7天</el-radio-button>
                  <el-radio-button label="30d">30天</el-radio-button>
                  <el-radio-button label="90d">90天</el-radio-button>
                </el-radio-group>
              </div>
            </template>
            <LineChart
              :data="trendChartData"
              :x-axis="{ type: 'category', data: trendLabels }"
              :y-axis="{ name: '报文数量' }"
              :show-data-zoom="true"
              height="350px"
            />
          </el-card>
        </el-col>

        <!-- 告警统计 -->
        <el-col :xs="24" :sm="24" :md="12" :lg="12" :xl="12">
          <el-card class="dashboard__chart-card" shadow="hover">
            <template #header>
              <div class="dashboard__chart-header">
                <span>告警级别分布</span>
                <el-button type="text" size="small" @click="$router.push('/alerts')">
                  查看详情
                </el-button>
              </div>
            </template>
            <BarChart
              :data="alertLevelData"
              :x-axis-data="alertLevelLabels"
              height="350px"
            />
          </el-card>
        </el-col>
      </el-row>
    </div>

    <!-- 告警列表 -->
    <div class="dashboard__alerts">
      <el-card class="dashboard__alert-card" shadow="hover">
        <template #header>
          <div class="dashboard__alert-header">
            <span>最新告警</span>
            <el-button type="text" size="small" @click="$router.push('/alerts')">
              查看全部
            </el-button>
          </div>
        </template>
        <el-table
          :data="recentAlerts"
          style="width: 100%"
          :show-header="true"
          stripe
          empty-text="暂无告警"
        >
          <el-table-column prop="level" label="级别" width="80">
            <template #default="{ row }">
              <el-tag :type="getAlertLevelType(row.alertLevel)" size="small">
                {{ getAlertLevelText(row.alertLevel) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="deviceName" label="设备" width="150" />
          <el-table-column prop="title" label="告警内容" min-width="200" />
          <el-table-column prop="timestamp" label="时间" width="160">
            <template #default="{ row }">
              {{ formatTime(row.timestamp) }}
            </template>
          </el-table-column>
          <el-table-column prop="read" label="状态" width="80">
            <template #default="{ row }">
              <el-tag :type="row.read ? 'info' : 'warning'" size="small">
                {{ row.read ? '已读' : '未读' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="100">
            <template #default="{ row }">
              <el-button
                v-if="!row.read"
                type="text"
                size="small"
                @click="markAsRead(row.id)"
              >
                标记已读
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import {
  Refresh,
  ArrowDown,
  Message,
  CircleCheck,
  Monitor,
  Warning
} from '@element-plus/icons-vue'
import { useDashboardStore } from '@/stores/dashboard'
import { generateDeviceStatus, generateMessageStats, generateAlerts, generateTrendData, generateDeviceTypeData, generateMessageFlowData } from '@/utils/mockData'
import { exportToPDF, exportToExcel, exportDashboardReport } from '@/utils/exportUtils'
import StatusCard from '@/components/dashboard/StatusCard.vue'
import DeviceStatusCard from '@/components/dashboard/DeviceStatusCard.vue'
import LineChart from '@/components/charts/LineChart.vue'
import PieChart from '@/components/charts/PieChart.vue'
import BarChart from '@/components/charts/BarChart.vue'
import dayjs from 'dayjs'

const dashboardStore = useDashboardStore()
const dashboardRef = ref<HTMLElement>()

// 响应式数据
const refreshing = ref(false)
const messageFlowTimeRange = ref('24h')
const trendTimeRange = ref('7d')

// 使用模拟数据
const deviceStatus = ref(generateDeviceStatus())
const deviceStats = ref({
  total: deviceStatus.value.length,
  online: deviceStatus.value.filter(d => d.status === 'online').length,
  offline: deviceStatus.value.filter(d => d.status === 'offline').length,
  warning: deviceStatus.value.filter(d => d.status === 'warning').length,
  error: deviceStatus.value.filter(d => d.status === 'error').length
})
const messageStats = ref(generateMessageStats())
const alerts = ref(generateAlerts())
const trendData = ref(generateTrendData())
const deviceTypeData = ref(generateDeviceTypeData())
const messageFlowData = ref(generateMessageFlowData())

// 加载状态
const loading = ref(false)

// 计算属性
const unreadAlerts = computed(() => alerts.value.filter(a => !a.read).length)

// 模拟趋势数据
const messageTrend = ref(15.2)
const successRateTrend = ref(2.1)
const deviceTrend = ref(-3.5)
const alertTrend = ref(8.7)

// 图表数据
const messageFlowChartData = computed(() =>
  messageFlowData.value.map(item => [item.label || item.timestamp, item.value])
)
const messageFlowLabels = computed(() =>
  messageFlowData.value.map(item => item.label || dayjs(item.timestamp).format('HH:mm'))
)

const trendChartData = computed(() => [
  {
    name: '报文数量',
    data: trendData.value.map(item => [item.label || dayjs(item.timestamp).format('MM-DD'), item.value]),
    color: '#1890ff'
  },
  {
    name: '处理成功数',
    data: trendData.value.map(item => [item.label || dayjs(item.timestamp).format('MM-DD'), Math.floor(item.value * 0.95)]),
    color: '#52c41a'
  }
])
const trendLabels = computed(() =>
  trendData.value.map(item => item.label || dayjs(item.timestamp).format('MM-DD'))
)

const alertLevelData = computed(() => [
  {
    name: '告警数量',
    data: [
      alerts.value.filter(a => a.alertLevel === 'LOW').length,
      alerts.value.filter(a => a.alertLevel === 'MEDIUM').length,
      alerts.value.filter(a => a.alertLevel === 'HIGH').length,
      alerts.value.filter(a => a.alertLevel === 'CRITICAL').length
    ],
    color: '#f5222d'
  }
])
const alertLevelLabels = ['低', '中', '高', '严重']

const recentAlerts = computed(() => alerts.value.slice(0, 5))

// 方法
const refreshAllData = async () => {
  refreshing.value = true
  try {
    // 重新生成模拟数据
    deviceStatus.value = generateDeviceStatus()
    deviceStats.value = {
      total: deviceStatus.value.length,
      online: deviceStatus.value.filter(d => d.status === 'online').length,
      offline: deviceStatus.value.filter(d => d.status === 'offline').length,
      warning: deviceStatus.value.filter(d => d.status === 'warning').length,
      error: deviceStatus.value.filter(d => d.status === 'error').length
    }
    messageStats.value = generateMessageStats()
    alerts.value = generateAlerts()
    trendData.value = generateTrendData()
    deviceTypeData.value = generateDeviceTypeData()
    messageFlowData.value = generateMessageFlowData()

    ElMessage.success('数据刷新成功')
  } catch (error) {
    ElMessage.error('数据刷新失败')
  } finally {
    refreshing.value = false
  }
}

const handleExport = async (format: string) => {
  try {
    const timestamp = dayjs().format('YYYY-MM-DD_HH-mm-ss')
    const filename = `厦门地铁设备报文分析报告_${timestamp}`

    switch (format) {
      case 'pdf':
        if (dashboardRef.value) {
          await exportToPDF(dashboardRef.value, filename)
        }
        break
      case 'excel':
        const excelData = {
          设备状态: deviceStatus.value,
          报文统计: [messageStats.value],
          告警信息: alerts.value,
          趋势数据: trendData.value
        }
        await exportToExcel(deviceStatus.value, filename)
        break
      case 'image':
        ElMessage.info('图表导出功能开发中...')
        break
    }
  } catch (error) {
    ElMessage.error('导出失败')
  }
}

const markAsRead = (alertId: string) => {
  const alert = alerts.value.find(a => a.id === alertId)
  if (alert) {
    alert.read = true
    ElMessage.success('已标记为已读')
  }
}

const getAlertLevelType = (level: string) => {
  const types = {
    LOW: 'info',
    MEDIUM: 'warning',
    HIGH: 'danger',
    CRITICAL: 'danger'
  }
  return types[level as keyof typeof types] || 'info'
}

const getAlertLevelText = (level: string) => {
  const texts = {
    LOW: '低',
    MEDIUM: '中',
    HIGH: '高',
    CRITICAL: '严重'
  }
  return texts[level as keyof typeof texts] || '未知'
}

const formatTime = (timestamp: string) => {
  return dayjs(timestamp).format('MM-DD HH:mm')
}

// 模拟实时数据更新
let updateInterval: number

onMounted(() => {
  refreshAllData()

  // 设置定时更新（每30秒）
  updateInterval = window.setInterval(() => {
    // 随机更新一些数据
    const randomIndex = Math.floor(Math.random() * deviceStatus.value.length)
    const statuses: Array<'online' | 'offline' | 'warning' | 'error'> = ['online', 'offline', 'warning', 'error']
    deviceStatus.value[randomIndex].status = statuses[Math.floor(Math.random() * statuses.length)]

    // 重新计算统计
    deviceStats.value = {
      total: deviceStatus.value.length,
      online: deviceStatus.value.filter(d => d.status === 'online').length,
      offline: deviceStatus.value.filter(d => d.status === 'offline').length,
      warning: deviceStatus.value.filter(d => d.status === 'warning').length,
      error: deviceStatus.value.filter(d => d.status === 'error').length
    }

    // 随机添加新告警
    if (Math.random() > 0.8) {
      const newAlerts = generateAlerts(1)
      alerts.value.unshift(...newAlerts)
    }
  }, 30000)
})

onUnmounted(() => {
  if (updateInterval) {
    clearInterval(updateInterval)
  }
})
</script>

<style scoped lang="scss">
.dashboard {
  padding: 20px;
  background: #f5f5f5;
  min-height: 100vh;
}

.dashboard__header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 24px;
  padding: 24px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.dashboard__header-left {
  flex: 1;
}

.dashboard__title {
  font-size: 24px;
  font-weight: 600;
  color: #333;
  margin: 0 0 8px 0;
}

.dashboard__subtitle {
  font-size: 14px;
  color: #666;
  margin: 0;
}

.dashboard__header-right {
  margin-left: 20px;
}

.dashboard__stats {
  margin-bottom: 24px;
}

.dashboard__charts {
  margin-bottom: 24px;
}

.dashboard__chart-card {
  margin-bottom: 20px;

  .el-card__body {
    padding: 20px;
  }
}

.dashboard__chart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.dashboard__alerts {
  margin-bottom: 24px;
}

.dashboard__alert-card {
  .el-card__body {
    padding: 20px;
  }
}

.dashboard__alert-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

@media (max-width: 768px) {
  .dashboard {
    padding: 16px;
  }

  .dashboard__header {
    flex-direction: column;
    padding: 20px;
  }

  .dashboard__header-right {
    margin-left: 0;
    margin-top: 16px;
    width: 100%;
  }

  .dashboard__title {
    font-size: 20px;
  }

  .dashboard__chart-header {
    flex-direction: column;
    gap: 12px;
    align-items: flex-start;
  }

  .dashboard__alert-header {
    flex-direction: column;
    gap: 12px;
    align-items: flex-start;
  }
}
</style>