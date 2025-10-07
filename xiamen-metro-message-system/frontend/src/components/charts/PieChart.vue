<template>
  <div ref="chartRef" :style="{ width: width, height: height }"></div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch } from 'vue'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { PieChart } from 'echarts/charts'
import {
  TitleComponent,
  TooltipComponent,
  LegendComponent,
  ToolboxComponent
} from 'echarts/components'
import type { EChartsOption } from 'echarts'
import * as echarts from 'echarts'

// 注册必需的组件
use([
  CanvasRenderer,
  PieChart,
  TitleComponent,
  TooltipComponent,
  LegendComponent,
  ToolboxComponent
])

interface Props {
  width?: string
  height?: string
  title?: string
  data: Array<{
    name: string
    value: number
    color?: string
  }>
  radius?: string | string[]
  center?: string[]
  roseType?: boolean
  showLegend?: boolean
  showToolbox?: boolean
  label?: {
    show?: boolean
    formatter?: string | Function
  }
}

const props = withDefaults(defineProps<Props>(), {
  width: '100%',
  height: '400px',
  radius: ['40%', '70%'],
  center: ['50%', '50%'],
  roseType: false,
  showLegend: true,
  showToolbox: false,
  label: () => ({ show: true })
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
      top: 10,
      textStyle: {
        fontSize: 16,
        fontWeight: 'normal'
      }
    } : undefined,
    tooltip: {
      trigger: 'item',
      formatter: '{a} <br/>{b}: {c} ({d}%)',
      backgroundColor: 'rgba(50, 50, 50, 0.9)',
      borderColor: '#ccc',
      borderWidth: 1,
      textStyle: {
        color: '#fff'
      }
    },
    legend: props.showLegend ? {
      orient: 'vertical',
      left: 'left',
      top: 'middle',
      formatter: (name: string) => {
        const item = props.data.find(d => d.name === name)
        return item ? `${name}: ${item.value}` : name
      }
    } : undefined,
    toolbox: props.showToolbox ? {
      feature: {
        saveAsImage: {
          title: '保存为图片'
        },
        dataView: {
          title: '数据视图',
          readOnly: true
        },
        restore: {
          title: '重置'
        }
      }
    } : undefined,
    series: [
      {
        name: props.title || '数据分布',
        type: 'pie',
        radius: props.radius,
        center: props.center,
        roseType: props.roseType ? 'area' : false,
        data: props.data.map((item, index) => ({
          name: item.name,
          value: item.value,
          itemStyle: {
            color: item.color || defaultColors[index % defaultColors.length]
          }
        })),
        emphasis: {
          itemStyle: {
            shadowBlur: 10,
            shadowOffsetX: 0,
            shadowColor: 'rgba(0, 0, 0, 0.5)'
          }
        },
        label: props.label?.show ? {
          formatter: props.label.formatter || '{b}: {c} ({d}%)',
          color: '#666'
        } : {
          show: false
        },
        labelLine: {
          show: props.label?.show
        },
        animationType: 'scale',
        animationEasing: 'elasticOut',
        animationDelay: (idx: number) => Math.random() * 200
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