import request from './request'

export const analysisApi = {
  // 启动分析
  startAnalysis: (projectId) => request.post(`/projects/${projectId}/analyze`),

  // 获取分析状态
  getAnalysisStatus: (projectId) => request.get(`/projects/${projectId}/analysis/status`),

  // 取消分析
  cancelAnalysis: (projectId) => request.delete(`/projects/${projectId}/analyze`),

  // 获取分析日志
  getAnalysisLogs: (projectId) => request.get(`/projects/${projectId}/analysis/logs`),

  // 重新分析
  reanalyze: (projectId) => request.post(`/projects/${projectId}/reanalyze`)
}