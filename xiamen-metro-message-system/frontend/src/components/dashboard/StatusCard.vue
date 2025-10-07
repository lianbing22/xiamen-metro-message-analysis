<template>
  <el-card
    :class="['status-card', `status-card--${type}`, { 'status-card--loading': loading }]"
    :body-style="{ padding: '20px' }"
    shadow="hover"
  >
    <div class="status-card__content">
      <div class="status-card__header">
        <div class="status-card__title">{{ title }}</div>
        <div class="status-card__icon">
          <el-icon :size="24" :color="iconColor">
            <component :is="icon" />
          </el-icon>
        </div>
      </div>

      <div class="status-card__value">
        <span v-if="loading" class="status-card__skeleton">
          <el-skeleton-item variant="text" style="width: 80px; height: 32px;" />
        </span>
        <span v-else class="status-card__number">{{ formattedValue }}</span>
        <span v-if="unit" class="status-card__unit">{{ unit }}</span>
      </div>

      <div v-if="showTrend && trend !== undefined" class="status-card__trend">
        <el-icon :size="12" :color="trendColor">
          <component :is="trendIcon" />
        </el-icon>
        <span :style="{ color: trendColor }" class="status-card__trend-text">
          {{ Math.abs(trend) }}%
        </span>
        <span class="status-card__trend-label">较昨日</span>
      </div>

      <div v-if="description" class="status-card__description">
        {{ description }}
      </div>

      <div v-if="showProgress && progress !== undefined" class="status-card__progress">
        <el-progress
          :percentage="progress"
          :color="progressColor"
          :show-text="false"
          :stroke-width="6"
        />
        <span class="status-card__progress-text">{{ progress }}%</span>
      </div>
    </div>
  </el-card>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import {
  Monitor,
  Message,
  Warning,
  CircleCheck,
  TrendCharts,
  ArrowUp,
  ArrowDown,
  Minus
} from '@element-plus/icons-vue'

interface Props {
  title: string
  value: number | string
  type?: 'primary' | 'success' | 'warning' | 'danger' | 'info'
  unit?: string
  icon?: any
  loading?: boolean
  trend?: number
  showTrend?: boolean
  description?: string
  showProgress?: boolean
  progress?: number
  format?: (value: number | string) => string
}

const props = withDefaults(defineProps<Props>(), {
  type: 'primary',
  loading: false,
  showTrend: true,
  showProgress: false,
  format: undefined
})

// 格式化数值
const formattedValue = computed(() => {
  if (props.loading) return '--'
  if (props.format) return props.format(props.value)

  const value = Number(props.value)
  if (isNaN(value)) return props.value

  // 格式化大数字
  if (value >= 1000000) {
    return (value / 1000000).toFixed(1) + 'M'
  } else if (value >= 1000) {
    return (value / 1000).toFixed(1) + 'K'
  }

  return value.toLocaleString()
})

// 获取默认图标
const defaultIcon = computed(() => {
  if (props.icon) return props.icon

  switch (props.type) {
    case 'success':
      return CircleCheck
    case 'warning':
      return Warning
    case 'danger':
      return Warning
    case 'info':
      return Message
    default:
      return Monitor
  }
})

// 获取图标颜色
const iconColor = computed(() => {
  const colors = {
    primary: '#1890ff',
    success: '#52c41a',
    warning: '#faad14',
    danger: '#f5222d',
    info: '#722ed1'
  }
  return colors[props.type]
})

// 趋势相关计算
const trendIcon = computed(() => {
  if (!props.trend) return Minus
  return props.trend > 0 ? ArrowUp : ArrowDown
})

const trendColor = computed(() => {
  if (!props.trend) return '#999'
  return props.trend > 0 ? '#52c41a' : '#f5222d'
})

// 进度条颜色
const progressColor = computed(() => {
  if (!props.progress) return iconColor.value

  if (props.progress >= 80) return '#52c41a'
  if (props.progress >= 60) return '#faad14'
  return '#f5222d'
})
</script>

<style scoped lang="scss">
.status-card {
  border: none;
  border-radius: 8px;
  transition: all 0.3s ease;
  cursor: pointer;

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
  }

  &--loading {
    .status-card__skeleton {
      display: inline-block;
    }
  }

  &--primary {
    border-left: 4px solid #1890ff;
  }

  &--success {
    border-left: 4px solid #52c41a;
  }

  &--warning {
    border-left: 4px solid #faad14;
  }

  &--danger {
    border-left: 4px solid #f5222d;
  }

  &--info {
    border-left: 4px solid #722ed1;
  }
}

.status-card__content {
  height: 100%;
}

.status-card__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.status-card__title {
  font-size: 14px;
  color: #666;
  font-weight: 500;
}

.status-card__icon {
  opacity: 0.8;
}

.status-card__value {
  margin-bottom: 12px;
  display: flex;
  align-items: baseline;
  gap: 4px;
}

.status-card__number {
  font-size: 28px;
  font-weight: 600;
  color: #333;
  line-height: 1;
}

.status-card__unit {
  font-size: 14px;
  color: #999;
  margin-left: 4px;
}

.status-card__trend {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-bottom: 8px;
}

.status-card__trend-text {
  font-size: 12px;
  font-weight: 500;
}

.status-card__trend-label {
  font-size: 12px;
  color: #999;
}

.status-card__description {
  font-size: 12px;
  color: #999;
  line-height: 1.4;
}

.status-card__progress {
  margin-top: 12px;
  display: flex;
  align-items: center;
  gap: 12px;
}

.status-card__progress-text {
  font-size: 12px;
  color: #666;
  min-width: 32px;
}

@media (max-width: 768px) {
  .status-card__value {
    .status-card__number {
      font-size: 24px;
    }
  }
}
</style>