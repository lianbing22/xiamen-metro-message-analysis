<template>
  <el-card class="device-status-card" shadow="hover">
    <template #header>
      <div class="device-status-card__header">
        <span class="device-status-card__title">设备状态</span>
        <el-button
          type="text"
          size="small"
          @click="refreshDevices"
          :loading="refreshing"
        >
          <el-icon><Refresh /></el-icon>
        </el-button>
      </div>
    </template>

    <div class="device-status-card__stats">
      <div class="device-status-card__stat">
        <div class="device-status-card__stat-value">{{ deviceStats.total }}</div>
        <div class="device-status-card__stat-label">设备总数</div>
      </div>
      <div class="device-status-card__stat device-status-card__stat--online">
        <div class="device-status-card__stat-value">{{ deviceStats.online }}</div>
        <div class="device-status-card__stat-label">在线</div>
      </div>
      <div class="device-status-card__stat device-status-card__stat--offline">
        <div class="device-status-card__stat-value">{{ deviceStats.offline }}</div>
        <div class="device-status-card__stat-label">离线</div>
      </div>
      <div class="device-status-card__stat device-status-card__stat--warning">
        <div class="device-status-card__stat-value">{{ deviceStats.warning }}</div>
        <div class="device-status-card__stat-label">警告</div>
      </div>
      <div class="device-status-card__stat device-status-card__stat--error">
        <div class="device-status-card__stat-value">{{ deviceStats.error }}</div>
        <div class="device-status-card__stat-label">故障</div>
      </div>
    </div>

    <div class="device-status-card__chart">
      <div class="device-status-card__chart-title">在线率</div>
      <div class="device-status-card__gauge">
        <GaugeChart
          :value="onlineRate"
          :max="100"
          unit="%"
          :thresholds="[
            { value: 80, color: '#52c41a', label: '优秀' },
            { value: 60, color: '#faad14', label: '良好' },
            { value: 100, color: '#f5222d', label: '差' }
          ]"
          height="150px"
        />
      </div>
    </div>

    <div class="device-status-card__list">
      <div class="device-status-card__list-title">最近活动设备</div>
      <div class="device-status-card__devices">
        <div
          v-for="device in recentDevices"
          :key="device.id"
          class="device-status-card__device"
        >
          <div class="device-status-card__device-info">
            <div class="device-status-card__device-name">
              {{ device.deviceName }}
            </div>
            <div class="device-status-card__device-location">
              {{ device.location }}
            </div>
          </div>
          <div class="device-status-card__device-status">
            <el-tag
              :type="getStatusType(device.status)"
              size="small"
              effect="light"
            >
              {{ getStatusText(device.status) }}
            </el-tag>
          </div>
        </div>
      </div>
    </div>
  </el-card>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import GaugeChart from '@/components/charts/GaugeChart.vue'
import type { DeviceStatus } from '@/types'

interface Props {
  deviceStats: {
    total: number
    online: number
    offline: number
    warning: number
    error: number
  }
  devices: DeviceStatus[]
}

const props = defineProps<Props>()

const refreshing = ref(false)

// 计算在线率
const onlineRate = computed(() => {
  if (props.deviceStats.total === 0) return 0
  return Math.round((props.deviceStats.online / props.deviceStats.total) * 100)
})

// 获取最近活动的设备（最多5个）
const recentDevices = computed(() => {
  return props.devices
    .filter(device => device.status === 'online')
    .sort((a, b) => new Date(b.lastSeen).getTime() - new Date(a.lastSeen).getTime())
    .slice(0, 5)
})

// 获取状态类型
const getStatusType = (status: string) => {
  const types = {
    online: 'success',
    offline: 'info',
    warning: 'warning',
    error: 'danger'
  }
  return types[status as keyof typeof types] || 'info'
}

// 获取状态文本
const getStatusText = (status: string) => {
  const texts = {
    online: '在线',
    offline: '离线',
    warning: '警告',
    error: '故障'
  }
  return texts[status as keyof typeof texts] || '未知'
}

// 刷新设备数据
const refreshDevices = async () => {
  refreshing.value = true
  try {
    // 这里可以调用刷新API
    await new Promise(resolve => setTimeout(resolve, 1000))
    ElMessage.success('设备数据已刷新')
  } catch (error) {
    ElMessage.error('刷新失败')
  } finally {
    refreshing.value = false
  }
}

onMounted(() => {
  // 初始化时可以执行一些操作
})
</script>

<style scoped lang="scss">
.device-status-card {
  .el-card__body {
    padding: 16px;
  }
}

.device-status-card__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.device-status-card__title {
  font-size: 16px;
  font-weight: 600;
  color: #333;
}

.device-status-card__stats {
  display: flex;
  justify-content: space-between;
  margin-bottom: 20px;
  padding: 16px 0;
  border-bottom: 1px solid #f0f0f0;
}

.device-status-card__stat {
  text-align: center;
  flex: 1;

  &--online {
    .device-status-card__stat-value {
      color: #52c41a;
    }
  }

  &--offline {
    .device-status-card__stat-value {
      color: #999;
    }
  }

  &--warning {
    .device-status-card__stat-value {
      color: #faad14;
    }
  }

  &--error {
    .device-status-card__stat-value {
      color: #f5222d;
    }
  }
}

.device-status-card__stat-value {
  font-size: 24px;
  font-weight: 600;
  color: #333;
  margin-bottom: 4px;
}

.device-status-card__stat-label {
  font-size: 12px;
  color: #666;
}

.device-status-card__chart {
  margin-bottom: 20px;
}

.device-status-card__chart-title {
  font-size: 14px;
  color: #666;
  margin-bottom: 12px;
  text-align: center;
}

.device-status-card__gauge {
  display: flex;
  justify-content: center;
}

.device-status-card__list {
  .device-status-card__list-title {
    font-size: 14px;
    color: #666;
    margin-bottom: 12px;
  }
}

.device-status-card__devices {
  max-height: 200px;
  overflow-y: auto;
}

.device-status-card__device {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 0;
  border-bottom: 1px solid #f5f5f5;

  &:last-child {
    border-bottom: none;
  }
}

.device-status-card__device-info {
  flex: 1;
  min-width: 0;
}

.device-status-card__device-name {
  font-size: 14px;
  color: #333;
  margin-bottom: 2px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.device-status-card__device-location {
  font-size: 12px;
  color: #999;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.device-status-card__device-status {
  margin-left: 8px;
}

@media (max-width: 768px) {
  .device-status-card__stats {
    flex-wrap: wrap;
    gap: 16px;
  }

  .device-status-card__stat {
    flex: 0 0 calc(50% - 8px);
  }

  .device-status-card__stat-value {
    font-size: 20px;
  }
}
</style>