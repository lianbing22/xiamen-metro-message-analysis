<template>
  <div ref="chartRef" :style="{ width: width, height: height }"></div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch } from 'vue'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart } from 'echarts/charts'
import {
  TitleComponent,
  TooltipComponent,
  GridComponent,
  LegendComponent,
  DataZoomComponent
} from 'echarts/components'
import type { EChartsOption } from 'echarts'
import * as echarts from 'echarts'

// 注册必需的组件
use([
  CanvasRenderer,
  LineChart,
  TitleComponent,
  TooltipComponent,
  GridComponent,
  LegendComponent,
  DataZoomComponent
])

interface Props {
  width?: string
  height?: string
  title?: string
  data: Array<{
    name: string
    data: Array<[string, number]>
    type?: 'line' | 'area'
    color?: string
  }>
  xAxis?: {
    type?: 'category' | 'time' | 'value'
    name?: string
    data?: string[]
  }
  yAxis?: {
    type?: 'value' | 'category'
    name?: string
    min?: number
    max?: number
  }
  tooltip?: {
    trigger?: 'axis' | 'item'
    formatter?: string | Function
  }
  showLegend?: boolean
  showDataZoom?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  width: '100%',
  height: '400px',
  showLegend: true,
  showDataZoom: false
})

const chartRef = ref<HTMLElement>()
let chart: echarts.ECharts | null = null

// 初始化图表
const initChart = () => {
  if (!chartRef.value) return

  chart = echarts.init(chartRef.value)
  updateChart()
}

// 更新图表配置
const updateChart = () => {
  if (!chart) return

  const option: EChartsOption = {
    title: props.title ? {
      text: props.title,
      left: 'center',
      textStyle: {
        fontSize: 16,
        fontWeight: 'normal'
      }
    } : undefined,
    tooltip: {
      trigger: props.tooltip?.trigger || 'axis',
      formatter: props.tooltip?.formatter || '{b}<br/>{a}: {c}',
      backgroundColor: 'rgba(50, 50, 50, 0.9)',
      borderColor: '#ccc',
      borderWidth: 1,
      textStyle: {
        color: '#fff'
      }
    },
    legend: props.showLegend ? {
      type: 'scroll',
      orient: 'horizontal',
      bottom: 0,
      data: props.data.map(item => item.name)
    } : undefined,
    grid: {
      left: '3%',
      right: '4%',
      bottom: props.showLegend ? '10%' : '3%',
      top: props.title ? '15%' : '5%',
      containLabel: true
    },
    xAxis: {
      type: props.xAxis?.type || 'category',
      name: props.xAxis?.name,
      data: props.xAxis?.data,
      boundaryGap: false,
      axisLine: {
        lineStyle: {
          color: '#999'
        }
      },
      axisLabel: {
        color: '#666',
        rotate: 45
      }
    },
    yAxis: {
      type: props.yAxis?.type || 'value',
      name: props.yAxis?.name,
      min: props.yAxis?.min,
      max: props.yAxis?.max,
      axisLine: {
        lineStyle: {
          color: '#999'
        }
      },
      axisLabel: {
        color: '#666'
      },
      splitLine: {
        lineStyle: {
          color: '#eee'
        }
      }
    },
    dataZoom: props.showDataZoom ? [{
      type: 'inside',
      start: 0,
      end: 100
    }, {
      start: 0,
      end: 100,
      handleIcon: 'M10.7,11.9v-1.3H9.3v1.3c-4.9,0.3-8.8,4.4-8.8,9.4c0,5,3.9,9.1,8.8,9.4v1.3h1.3v-1.3c4.9-0.3,8.8-4.4,8.8-9.4C19.5,16.3,15.6,12.2,10.7,11.9z M13.3,24.4H6.7V23h6.6V24.4z M13.3,19.6H6.7v-1.4h6.6V19.6z',
      handleSize: '80%',
      handleStyle: {
        color: '#fff',
        shadowBlur: 3,
        shadowColor: 'rgba(0, 0, 0, 0.6)',
        shadowOffsetX: 2,
        shadowOffsetY: 2
      }
    }] : undefined,
    series: props.data.map(item => ({
      name: item.name,
      type: item.type === 'area' ? 'line' : 'line',
      data: item.data,
      smooth: true,
      symbol: 'circle',
      symbolSize: 6,
      lineStyle: {
        width: 2,
        color: item.color
      },
      itemStyle: {
        color: item.color
      },
      areaStyle: item.type === 'area' ? {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          {
            offset: 0,
            color: item.color ? `${item.color}40` : 'rgba(24, 144, 255, 0.4)'
          },
          {
            offset: 1,
            color: item.color ? `${item.color}10` : 'rgba(24, 144, 255, 0.1)'
          }
        ])
      } : undefined,
      emphasis: {
        focus: 'series'
      }
    }))
  }

  chart.setOption(option, true)
}

// 监听窗口大小变化
const handleResize = () => {
  chart?.resize()
}

// 监听数据变化
watch(() => props.data, () => {
  updateChart()
}, { deep: true })

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