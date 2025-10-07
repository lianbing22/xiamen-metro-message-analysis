<template>
  <div ref="chartRef" :style="{ width: width, height: height }"></div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch } from 'vue'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { BarChart } from 'echarts/charts'
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
  BarChart,
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
    data: number[]
    color?: string
    stack?: string
  }>
  xAxisData: string[]
  yAxis?: {
    name?: string
    min?: number
    max?: number
  }
  horizontal?: boolean
  showLegend?: boolean
  showDataZoom?: boolean
  tooltip?: {
    formatter?: string | Function
  }
}

const props = withDefaults(defineProps<Props>(), {
  width: '100%',
  height: '400px',
  horizontal: false,
  showLegend: true,
  showDataZoom: false
})

const chartRef = ref<HTMLElement>()
let chart: echarts.ECharts | null = null

// 默认颜色方案
const defaultColors = [
  '#1890ff', '#52c41a', '#faad14', '#f5222d', '#722ed1',
  '#13c2c2', '#eb2f96', '#fa541c', '#a0d911', '#2f54eb'
]

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
      trigger: 'axis',
      axisPointer: {
        type: 'shadow'
      },
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
      type: props.horizontal ? 'value' : 'category',
      data: props.horizontal ? undefined : props.xAxisData,
      name: props.horizontal ? props.yAxis?.name : undefined,
      axisLine: {
        lineStyle: {
          color: '#999'
        }
      },
      axisLabel: {
        color: '#666',
        rotate: props.horizontal ? 0 : 45
      },
      axisTick: {
        alignWithLabel: true
      }
    },
    yAxis: {
      type: props.horizontal ? 'category' : 'value',
      data: props.horizontal ? props.xAxisData : undefined,
      name: props.horizontal ? undefined : props.yAxis?.name,
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
    dataZoom: props.showDataZoom ? [
      {
        type: 'inside',
        start: 0,
        end: 100,
        orient: props.horizontal ? 'vertical' : 'horizontal'
      },
      {
        start: 0,
        end: 100,
        orient: props.horizontal ? 'vertical' : 'horizontal',
        handleIcon: 'M10.7,11.9v-1.3H9.3v1.3c-4.9,0.3-8.8,4.4-8.8,9.4c0,5,3.9,9.1,8.8,9.4v1.3h1.3v-1.3c4.9-0.3,8.8-4.4,8.8-9.4C19.5,16.3,15.6,12.2,10.7,11.9z M13.3,24.4H6.7V23h6.6V24.4z M13.3,19.6H6.7v-1.4h6.6V19.6z',
        handleSize: '80%',
        handleStyle: {
          color: '#fff',
          shadowBlur: 3,
          shadowColor: 'rgba(0, 0, 0, 0.6)',
          shadowOffsetX: 2,
          shadowOffsetY: 2
        }
      }
    ] : undefined,
    series: props.data.map((item, index) => ({
      name: item.name,
      type: 'bar',
      data: item.data,
      stack: item.stack,
      barWidth: props.data.length === 1 ? '60%' : undefined,
      itemStyle: {
        color: item.color || defaultColors[index % defaultColors.length],
        borderRadius: props.data.length === 1 && !props.horizontal ? [4, 4, 0, 0] :
                    props.data.length === 1 && props.horizontal ? [0, 4, 4, 0] : 0
      },
      emphasis: {
        focus: 'series',
        itemStyle: {
          shadowBlur: 10,
          shadowOffsetX: 0,
          shadowColor: 'rgba(0, 0, 0, 0.5)'
        }
      },
      animationDelay: (idx: number) => idx * 50
    }))
  }

  chart.setOption(option, true)
}

// 监听窗口大小变化
const handleResize = () => {
  chart?.resize()
}

// 监听数据变化
watch(() => [props.data, props.xAxisData], () => {
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