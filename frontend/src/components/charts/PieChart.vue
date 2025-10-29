<template>
  <div class="pie-chart-container">
    <div ref="chartRef" class="chart"></div>
    <div v-if="loading" class="chart-loading">
      <a-spin size="large" />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, watch, nextTick } from 'vue'
import * as echarts from 'echarts'

const props = defineProps({
  data: {
    type: Array,
    required: true,
    default: () => []
  },
  title: {
    type: String,
    default: ''
  },
  height: {
    type: [Number, String],
    default: 300
  },
  width: {
    type: [Number, String],
    default: '100%'
  },
  innerRadius: {
    type: [Number, String],
    default: 0
  },
  outerRadius: {
    type: [Number, String],
    default: '70%'
  },
  showLegend: {
    type: Boolean,
    default: true
  },
  showLabel: {
    type: Boolean,
    default: true
  },
  loading: {
    type: Boolean,
    default: false
  },
  colors: {
    type: Array,
    default: () => ['#1890ff', '#52c41a', '#faad14', '#f5222d', '#722ed1', '#13c2c2', '#eb2f96']
  }
})

const emit = defineEmits(['chart-click', 'ready'])

const chartRef = ref(null)
let chart = null

const initChart = () => {
  if (!chartRef.value) return

  // 销毁已存在的图表
  if (chart) {
    chart.dispose()
  }

  // 创建新图表
  chart = echarts.init(chartRef.value)

  // 配置选项
  const option = getChartOption()

  // 设置配置
  chart.setOption(option)

  // 监听点击事件
  chart.on('click', handleChartClick)

  emit('ready', chart)
}

const getChartOption = () => {
  return {
    title: props.title ? {
      text: props.title,
      left: 'center',
      top: props.title ? 10 : 'center',
      textStyle: {
        fontSize: 16,
        fontWeight: 'bold',
        color: '#262626'
      }
    } : undefined,

    tooltip: {
      trigger: 'item',
      formatter: (params) => {
        const percent = ((params.value / getTotalValue()) * 100).toFixed(1)
        return `
          <div style="padding: 8px;">
            <div style="font-weight: bold; margin-bottom: 4px;">
              <span style="display: inline-block; width: 10px; height: 10px; background: ${params.color}; border-radius: 50%; margin-right: 8px;"></span>
              ${params.name}
            </div>
            <div>数值: ${params.value}</div>
            <div>占比: ${percent}%</div>
          </div>
        `
      }
    },

    legend: props.showLegend ? {
      orient: 'horizontal',
      bottom: 10,
      left: 'center',
      textStyle: {
        color: '#666'
      }
    } : undefined,

    series: [
      {
        name: props.title,
        type: 'pie',
        radius: [props.innerRadius, props.outerRadius],
        center: ['50%', props.title ? '60%' : '50%'],
        data: props.data.map((item, index) => ({
          value: item.value,
          name: item.name,
          itemStyle: {
            color: props.colors[index % props.colors.length]
          },
          emphasis: {
            itemStyle: {
              shadowBlur: 10,
              shadowOffsetX: 0,
              shadowColor: 'rgba(0, 0, 0, 0.5)'
            }
          }
        })),
        label: props.showLabel ? {
          show: true,
          position: 'outside',
          formatter: '{b}: {d} ({d}%)',
          color: '#666',
          fontSize: 12
        } : undefined,

        labelLine: {
          show: props.showLabel,
          length: 15,
          length2: 15,
          maxSurfaceAngle: 80,
          minSurfaceAngle: -80,
          smooth: true,
          lineStyle: {
            color: '#ccc'
          }
        },

        animationType: 'scale',
        animationEasing: 'elasticOut',
        animationDelay: (idx) => idx * 100
      }
    ]
  }
}

const getTotalValue = () => {
  return props.data.reduce((sum, item) => sum + item.value, 0)
}

const handleChartClick = (params) => {
  emit('chart-click', params)
}

const updateChart = () => {
  if (chart) {
    const option = getChartOption()
    chart.setOption(option, true)
  }
}

const resizeChart = () => {
  if (chart) {
    chart.resize()
  }
}

const disposeChart = () => {
  if (chart) {
    chart.dispose()
    chart = null
  }
}

// 监听数据变化
watch(() => props.data, () => {
  nextTick(() => {
    updateChart()
  })
}, { deep: true })

watch(() => props.title, () => {
  nextTick(() => {
    updateChart()
  })
})

// 监听尺寸变化
watch([() => props.height, () => props.width], () => {
  nextTick(() => {
    resizeChart()
  })
})

onMounted(() => {
  nextTick(() => {
    initChart()

    // 监听窗口大小变化
    window.addEventListener('resize', resizeChart)
  })
})

onUnmounted(() => {
  disposeChart()
  window.removeEventListener('resize', resizeChart)
})

// 暴露方法
defineExpose({
  resize: resizeChart,
  dispose: disposeChart,
  getChart: () => chart
})
</script>

<style scoped>
.pie-chart-container {
  position: relative;
  width: 100%;
  height: v-bind(height);
}

.chart {
  width: 100%;
  height: 100%;
}

.chart-loading {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(255, 255, 255, 0.8);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 10;
}
</style>