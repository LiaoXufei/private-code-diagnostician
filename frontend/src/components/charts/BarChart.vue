<template>
  <div class="bar-chart-container">
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
  horizontal: {
    type: Boolean,
    default: false
  },
  showValues: {
    type: Boolean,
    default: true
  },
  loading: {
    type: Boolean,
    default: false
  },
  colors: {
    type: Array,
    default: () => ['#1890ff', '#52c41a', '#faad14', '#f5222d', '#722ed1', '#13c2c2']
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
  const isHorizontal = props.horizontal

  return {
    title: props.title ? {
      text: props.title,
      left: 'center',
      textStyle: {
        fontSize: 16,
        fontWeight: 'bold',
        color: '#262626'
      }
    } : undefined,

    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'shadow'
      },
      formatter: (params) => {
        const data = Array.isArray(params) ? params[0] : params
        return `
          <div style="padding: 8px;">
            <div style="font-weight: bold; margin-bottom: 4px;">${data.name}</div>
            <div>数值: ${data.value}</div>
            ${data.percent ? `<div>占比: ${data.percent}%</div>` : ''}
          </div>
        `
      }
    },

    legend: {
      data: props.data.map(item => item.name),
      bottom: 10,
      textStyle: {
        color: '#666'
      }
    },

    grid: {
      top: props.title ? 60 : 20,
      bottom: 60,
      left: 60,
      right: 40,
      containLabel: true
    },

    xAxis: {
      type: isHorizontal ? 'value' : 'category',
      data: isHorizontal ? undefined : props.data.map(item => item.name),
      axisLine: {
        lineStyle: {
          color: '#e0e0e0'
        }
      },
      axisTick: {
        lineStyle: {
          color: '#e0e0e0'
        },
        textStyle: {
          color: '#666'
        }
      },
      axisLabel: {
        interval: 0,
        rotate: isHorizontal ? 0 : (props.data.length > 6 ? 45 : 0),
        textStyle: {
          color: '#666'
        }
      }
    },

    yAxis: {
      type: isHorizontal ? 'category' : 'value',
      data: isHorizontal ? props.data.map(item => item.name) : undefined,
      axisLine: {
        lineStyle: {
          color: '#e0e0e0'
        }
      },
      axisTick: {
        lineStyle: {
          color: '#e0e0e0'
        },
        textStyle: {
          color: '#666'
        }
      },
      axisLabel: {
        textStyle: {
          color: '#666'
        }
      },
      splitLine: {
        lineStyle: {
          color: '#f0f0f0'
        }
      }
    },

    series: [
      {
        name: props.title,
        type: 'bar',
        data: props.data.map((item, index) => ({
          value: item.value,
          name: item.name,
          itemStyle: {
            color: props.colors[index % props.colors.length]
          }
        })),
        barWidth: '60%',
        label: props.showValues ? {
          show: true,
          position: isHorizontal ? 'right' : 'top',
          formatter: '{c}',
          textStyle: {
            color: '#666',
            fontSize: 12
          }
        } : undefined,
        emphasis: {
          itemStyle: {
            shadowBlur: 10,
            shadowOffsetX: 0,
            shadowColor: 'rgba(0, 0, 0, 0.5)'
          }
        }
      }
    ]
  }
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

watch(() => props.horizontal, () => {
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
.bar-chart-container {
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