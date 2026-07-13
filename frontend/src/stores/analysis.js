import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { message } from 'ant-design-vue'
import { analysisApi } from '@/api/analysis'
import { projectApi } from '@/api/project'
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
  let pollTimer = null
  let finished = false
  const pollIntervalMs = 1000

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

  const markFinished = () => {
    if (finished) return true
    finished = true
    return false
  }

  // Actions
  const startAnalysis = async (projectId) => {
    if (isAnalyzing.value) {
      message.warning('当前有分析任务正在进行')
      return
    }

    loading.value = true
    finished = false
    analysisError.value = null

    try {
      const response = await analysisApi.startAnalysis(projectId)
      currentTaskId.value = response.data.taskId
      analysisStatus.value = 'analyzing'
      analysisProgress.value = 0

      setupWebSocketConnection(projectId)
      startPolling(projectId)

      message.success('分析任务已启动')
      return currentTaskId.value
    } catch (err) {
      analysisError.value = err.message || '启动分析失败'
      analysisStatus.value = 'error'
      cleanupWebSocket()
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
    if (!isAnalyzing.value) {
      message.info('当前没有正在进行的分析任务')
      return
    }

    loading.value = true

    try {
      await analysisApi.cancelAnalysis(projectId).catch(() => ({ data: { status: 'cancelled' } }))
      if (markFinished()) return
      analysisStatus.value = 'cancelled'
      stopPolling()
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
    if (markFinished()) return
    analysisStatus.value = 'completed'
    analysisProgress.value = 100
    reportData.value = result

    analysisHistory.value.unshift({
      taskId: currentTaskId.value,
      completedAt: new Date().toISOString(),
      overallScore: result.overallScore,
      grade: qualityGrade.value?.level || 'good'
    })

    cleanupWebSocket()
    stopPolling()
    message.success('代码分析完成')
  }

  const resetAnalysis = () => {
    finished = false
    analysisStatus.value = 'idle'
    analysisProgress.value = 0
    currentTaskId.value = null
    reportData.value = null
    analysisError.value = null
    cleanupWebSocket()
    stopPolling()
  }

  const setupWebSocketConnection = (projectId) => {
    try {
      const { connect, subscribe, disconnect } = useWebSocket(projectId)

      connect()

      subscribe('analysis_progress', (payload) => {
        if (finished) return
        updateAnalysisStatus({
          progress: payload.progress,
          status: 'analyzing',
          stage: payload.stage,
          message: payload.message
        })
      })

      subscribe('analysis_completed', (payload) => {
        completeAnalysis(payload.result)
      })

      subscribe('analysis_cancelled', () => {
        if (markFinished()) return
        analysisStatus.value = 'cancelled'
        analysisProgress.value = 0
        cleanupWebSocket()
        stopPolling()
        message.info('分析任务已取消')
      })

      subscribe('analysis_error', (payload) => {
        if (markFinished()) return
        analysisStatus.value = 'error'
        analysisError.value = payload.error
        message.error(payload.error)
        cleanupWebSocket()
        stopPolling()
      })

      subscribe('*', (data) => {
        if (data.type === 'connection_failed' && !isAnalyzing.value) {
          cleanupWebSocket()
        }
      })

      wsManager.value = { disconnect }
      wsConnected.value = true
    } catch (error) {
      console.error('WebSocket连接设置失败:', error)
    }
  }

  const startPolling = (projectId) => {
    stopPolling()
    pollTimer = setInterval(async () => {
      if (finished) {
        stopPolling()
        return
      }
      try {
        const status = await getAnalysisStatus(projectId)
        if (status.status === 'completed') {
          if (finished) return
          const res = await projectApi.getAnalysisResult(projectId)
          completeAnalysis(res.data)
        } else if (status.status === 'error' || status.status === 'cancelled') {
          if (markFinished()) return
          stopPolling()
          cleanupWebSocket()
        }
      } catch (e) {
        // 保持容错，下一次轮询继续
      }
    }, pollIntervalMs)
  }

  const stopPolling = () => {
    if (pollTimer) {
      clearInterval(pollTimer)
      pollTimer = null
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
    analysisStatus,
    analysisProgress,
    currentTaskId,
    reportData,
    analysisError,
    analysisHistory,
    loading,
    wsConnected,
    isAnalyzing,
    isCompleted,
    hasError,
    isIdle,
    progressPercentage,
    statusText,
    hasReport,
    overallScore,
    qualityGrade,
    startAnalysis,
    getAnalysisStatus,
    cancelAnalysis,
    updateAnalysisStatus,
    resetAnalysis,
    clearError
  }
})
