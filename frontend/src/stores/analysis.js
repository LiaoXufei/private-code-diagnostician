import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { message } from 'ant-design-vue'
import { analysisApi } from '@/api/analysis'
import { useWebSocket } from '@/composables/useWebSocket'

export const useAnalysisStore = defineStore('analysis', () => {
  // State
  const analysisStatus = ref('idle') // idle, analyzing, completed, error, cancelled
  const analysisProgress = ref(0)
  const currentTaskId = ref(null)
  const reportData = ref(null)
  const analysisError = ref(null)
  const analysisHistory = ref([])
  const loading = ref(false)

  // WebSocket 相关
  const wsConnected = ref(false)
  const wsManager = ref(null)

  // Getters
  const isAnalyzing = computed(() => analysisStatus.value === 'analyzing')
  const isCompleted = computed(() => analysisStatus.value === 'completed')
  const hasError = computed(() => analysisStatus.value === 'error')
  const isIdle = computed(() => analysisStatus.value === 'idle')
  const progressPercentage = computed(() => analysisProgress.value)

  const statusText = computed(() => {
    const statusMap = {
      idle: '未开始',
      analyzing: '分析中',
      completed: '已完成',
      error: '分析失败',
      cancelled: '已取消'
    }
    return statusMap[analysisStatus.value] || '未知状态'
  })

  const hasReport = computed(() => reportData.value !== null)

  const overallScore = computed(() => reportData.value?.overallScore || 0)

  const qualityGrade = computed(() => {
    if (!hasReport.value) return null

    const score = overallScore.value
    if (score >= 90) return { text: '优秀', color: '#52c41a', level: 'excellent' }
    if (score >= 80) return { text: '良好', color: '#1890ff', level: 'good' }
    if (score >= 60) return { text: '及格', color: '#faad14', level: 'average' }
    return { text: '不及格', color: '#f5222d', level: 'poor' }
  })

  // Actions
  const startAnalysis = async (projectId) => {
    if (isAnalyzing.value) {
      message.warning('当前有分析任务正在进行')
      return
    }

    loading.value = true
    analysisError.value = null

    try {
      const response = await analysisApi.startAnalysis(projectId)
      currentTaskId.value = response.data.taskId
      analysisStatus.value = 'analyzing'
      analysisProgress.value = 0

      // 建立 WebSocket 连接
      setupWebSocketConnection(projectId)

      message.success('分析任务已启动')
      return currentTaskId.value
    } catch (err) {
      analysisError.value = err.message || '启动分析失败'
      analysisStatus.value = 'error'
      cleanupWebSocket() // 确保在失败时清理WebSocket连接
      message.error(analysisError.value)
      throw err
    } finally {
      loading.value = false
    }
  }

  const getAnalysisStatus = async (projectId) => {
    try {
      const response = await analysisApi.getAnalysisStatus(projectId)
      const status = response.data

      updateAnalysisStatus(status)

      return status
    } catch (err) {
      console.error('获取分析状态失败:', err)
      throw err
    }
  }

  const cancelAnalysis = async (projectId) => {
    // 如果没有正在分析的任务，直接提示用户
    if (!isAnalyzing.value) {
      message.info('当前没有正在进行的分析任务')
      return
    }

    loading.value = true

    try {
      await analysisApi.cancelAnalysis(projectId)
      analysisStatus.value = 'cancelled'
      cleanupWebSocket()

      message.info('分析任务已取消')
    } catch (err) {
      message.error('取消分析失败')
      throw err
    } finally {
      loading.value = false
    }
  }

  const updateAnalysisStatus = (status) => {
    if (status.progress !== undefined) {
      analysisProgress.value = status.progress
    }

    if (status.status) {
      analysisStatus.value = status.status
    }

    if (status.error) {
      analysisError.value = status.error
    }

    if (status.result) {
      completeAnalysis(status.result)
    }
  }

  const completeAnalysis = (result) => {
    analysisStatus.value = 'completed'
    analysisProgress.value = 100
    reportData.value = result

    // 添加到历史记录
    analysisHistory.value.unshift({
      taskId: currentTaskId.value,
      completedAt: new Date().toISOString(),
      overallScore: result.overallScore,
      grade: qualityGrade.value.level
    })

    cleanupWebSocket()
    message.success('代码分析完成')
  }

  const resetAnalysis = () => {
    analysisStatus.value = 'idle'
    analysisProgress.value = 0
    currentTaskId.value = null
    reportData.value = null
    analysisError.value = null
    cleanupWebSocket()
  }

  const setupWebSocketConnection = (projectId) => {
    try {
      const { connect, subscribe, disconnect } = useWebSocket(projectId)

      connect()

      // 订阅分析进度
      subscribe('analysis_progress', (payload) => {
        updateAnalysisStatus({
          progress: payload.progress,
          status: 'analyzing',
          stage: payload.stage,
          message: payload.message
        })
      })

      // 订阅分析完成
      subscribe('analysis_completed', (payload) => {
        completeAnalysis(payload.result)
      })

      // 订阅分析错误
      subscribe('analysis_error', (payload) => {
        analysisStatus.value = 'error'
        analysisError.value = payload.error
        message.error(payload.error)
        cleanupWebSocket()
      })

      // 订阅连接状态变化
      subscribe('*', (data) => {
        // 如果WebSocket连接失败且当前不是分析状态，清理连接
        if (data.type === 'connection_failed' && !isAnalyzing.value) {
          cleanupWebSocket()
        }
      })

      wsManager.value = { disconnect }
      wsConnected.value = true
    } catch (error) {
      console.error('WebSocket连接设置失败:', error)
      // 如果WebSocket设置失败，不影响分析流程，只是不使用实时更新
    }
  }

  const cleanupWebSocket = () => {
    if (wsManager.value) {
      wsManager.value.disconnect()
      wsManager.value = null
    }
    wsConnected.value = false
  }

  const clearError = () => {
    analysisError.value = null
  }

  return {
    // State
    analysisStatus,
    analysisProgress,
    currentTaskId,
    reportData,
    analysisError,
    analysisHistory,
    loading,
    wsConnected,

    // Getters
    isAnalyzing,
    isCompleted,
    hasError,
    isIdle,
    progressPercentage,
    statusText,
    hasReport,
    overallScore,
    qualityGrade,

    // Actions
    startAnalysis,
    getAnalysisStatus,
    cancelAnalysis,
    updateAnalysisStatus,
    resetAnalysis,
    clearError
  }
})