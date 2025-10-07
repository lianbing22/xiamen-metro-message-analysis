<template>
  <div ref="chartRef" :style="{ width: width, height: height }"></div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch } from 'vue'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { GaugeChart } from 'echarts/charts'
import {
  TitleComponent,
  TooltipComponent
} from 'echarts/components'
import type { EChartsOption } from 'echarts'
import * as echarts from 'echarts'

// 注册必需的组件
use([
  CanvasRenderer,
  GaugeChart,
  TitleComponent,
  TooltipComponent
])

interface Props {
  width?: string
  height?: string
  title?: string
  value: number
  min?: number
  max?: number
  unit?: string
  thresholds?: Array<{
    value: number
    color: string
    label: string
  }>
  showDetail?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  width: '100%',
  height: '300px',
  min: 0,
  max: 100,
  unit: '%',
  showDetail: true
})

const chartRef = ref<HTMLElement>()
let chart: echarts.ECharts | null = null

// 默认阈值配置
const defaultThresholds = [
  { value: 30, color: '#52c41a', label: '正常' },
  { value: 70, color: '#faad14', label: '警告' },
  { value: 100, color: '#f5222d', label: '异常' }
]

// 获取当前值对应的颜色
const getValueColor = (value: number): string => {
  const thresholds = props.thresholds || defaultThresholds
  for (let i = thresholds.length - 1; i >= 0; i--) {
    if (value >= thresholds[i].value) {
      return thresholds[i].color
    }
  }
  return thresholds[0].color
}

// 初始化图表
const initChart = () => {
  if (!chartRef.value) return

  chart = echarts.init(chartRef.value)
  updateChart()
}

// 更新图表配置
const updateChart = () => {
  if (!chart) return

  const thresholds = props.thresholds || defaultThresholds
  const valueColor = getValueColor(props.value)

  const option: EChartsOption = {
    title: props.title ? {
      text: props.title,
      left: 'center',
      top: 0,
      textStyle: {
        fontSize: 14,
        fontWeight: 'normal',
        color: '#666'
      }
    } : undefined,
    series: [
      // 背景圆环
      {
        type: 'gauge',
        center: ['50%', '60%'],
        radius: '90%',
        startAngle: 200,
        endAngle: -20,
        min: props.min,
        max: props.max,
        splitNumber: 10,
        itemStyle: {
          color: valueColor
        },
        progress: {
          show: true,
          width: 30
        },
        pointer: {
          show: false
        },
        axisLine: {
          lineStyle: {
            width: 30,
            color: [[1, '#f0f0f0']]
          }
        },
        axisTick: {
          distance: -30,
          splitNumber: 5,
          lineStyle: {
            width: 2,
            color: '#999'
          }
        },
        splitLine: {
          distance: -35,
          length: 14,
          lineStyle: {
            width: 3,
            color: '#999'
          }
        },
        axisLabel: {
          distance: -20,
          color: '#999',
          fontSize: 12
        },
        anchor: {
          show: false
        },
        title: {
          show: false
        },
        detail: {
          valueAnimation: true,
          width: '60%',
          lineHeight: 40,
          borderRadius: 8,
          offsetCenter: [0, '-15%'],
          fontSize: 32,
          fontWeight: 'bold',
          formatter: function (value: number) {
            return `${value}${props.unit}`
          },
          color: valueColor
        },
        data: [
          {
            value: props.value
          }
        ]
      },
      // 阈值标记
      {
        type: 'gauge',
        center: ['50%', '60%'],
        radius: '90%',
        startAngle: 200,
        endAngle: -20,
        min: props.min,
        max: props.max,
        splitNumber: thresholds.length,
        itemStyle: {
          color: 'auto'
        },
        pointer: {
          show: false
        },
        axisLine: {
          lineStyle: {
            width: 0
          }
        },
        axisTick: {
          show: false
        },
        splitLine: {
          show: false
        },
        axisLabel: {
          distance: -40,
          color: '#666',
          fontSize: 10,
          formatter: function (value: number) {
            const threshold = thresholds.find(t => t.value === value)
            return threshold ? threshold.label : value.toString()
          }
        },
        detail: {
          show: false
        },
        data: []
      }
    ]
  }

  chart.setOption(option, true)
}

// 监听窗口大小变化
const handleResize = () => {
  chart?.resize()
}

// 监听数据变化
watch(() => props.value, () => {
  updateChart()
})

onMounted(() => {
  initChart()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  if (chart) {
    chart.dispose()
    chart = null
  }
  window.removeEventListener('resize', handleResize)
})

// 暴露方法给父组件
defineExpose({
  resize: handleResize,
  getChart: () => chart
})
</script>