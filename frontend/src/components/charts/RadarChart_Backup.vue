<template>
  <div class="radar-chart-container">
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
  dimensions: {
    type: Array,
    required: true,
    default: () => []
  },
  scores: {
    type: Array,
    required: true,
    default: () => []
  },
  height: {
    type: [Number, String],
    default: 400
  },
  width: {
    type: [Number, String],
    default: '100%'
  },
  title: {
    type: String,
    default: ''
  },
  loading: {
    type: Boolean,
    default: false
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
  const dimensions = props.dimensions.map(dim => dim.name)

  return {
    title: {
      text: props.title,
      left: 'center',
      textStyle: {
        fontSize: 16,
        fontWeight: 'bold',
        color: '#262626'
      }
    },
    tooltip: {
      trigger: 'item',
      formatter: (params) => {
        const dimension = props.dimensions[params.dataIndex]
        const score = props.scores[params.dataIndex]
        return `
          <div style="padding: 8px;">
            <div style="font-weight: bold; margin-bottom: 4px;">${dimension.name}</div>
            <div>得分: <span style="color: ${getScoreColor(score)};">${score}分</span></div>
            <div>满分: ${dimension.maxScore || 100}分</div>
            <div>占比: ${((score / (dimension.maxScore || 100)) * 100).toFixed(1)}%</div>
          </div>
        `
      }
    },
    legend: {
      data: ['当前得分'],
      bottom: 10,
      textStyle: {
        color: '#666'
      }
    },
    radar: {
      indicator: props.dimensions.map(dim => ({
        name: dim.name,
        max: dim.maxScore || 100,
        min: 0,
        axisLabel: {
          color: '#666',
          fontSize: 12
        },
        splitLine: {
          lineStyle: {
            color: '#e0e0e0'
          }
        },
        splitArea: {
          areaStyle: {
            color: ['rgba(240, 240, 240, 0.3)']
          }
        }
      }),
      shape: 'polygon',
      radius: '70%',
      center: ['50%', '50%'],
      name: {
        textStyle: {
          color: '#333',
          fontSize: 14
        }
      }
    },
    series: [
      {
        name: '代码质量评分',
        type: 'radar',
        data: [
          {
            value: props.scores,
            name: '当前得分',
            itemStyle: {
              color: '#1890ff',
              borderColor: '#1890ff',
              borderWidth: 2
            },
            areaStyle: {
              color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                { offset: 0, color: 'rgba(24, 144, 255, 0.3)' },
                { offset: 1, color: 'rgba(24, 144, 255, 0.1)' }
              ])
            }
          }
        ]
      }
    ],
    grid: {
      top: props.title ? 60 : 20,
      bottom: 60,
      left: 20,
      right: 20
    }
  }
}

const getScoreColor = (score) => {
  if (score >= 80) return '#52c41a'
  if (score >= 60) return '#faad14'
  return '#ff4d4f'
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
watch([() => props.dimensions, () => props.scores, () => props.title], () => {
  nextTick(() => {
    updateChart()
  })
}, { deep: true })

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
.radar-chart-container {
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