import request from './request'

export const projectApi = {
  // 获取项目列表
  getProjects: () => request.get('/projects'),

  // 创建项目
  createProject: (data) => request.post('/projects', data),

  // 获取项目详情
  getProject: (id) => request.get(`/projects/${id}`),

  // 更新项目配置
  updateProject: (id, data) => request.put(`/projects/${id}`, data),

  // 删除项目
  deleteProject: (id) => request.delete(`/projects/${id}`),

  // 获取项目文件列表
  getProjectFiles: (id) => request.get(`/projects/${id}/files`),

  // 删除项目文件
  deleteProjectFile: (projectId, fileId) => request.delete(`/projects/${projectId}/files/${fileId}`),

  // 获取项目统计信息
  getProjectStats: () => request.get('/projects/stats'),

  // 复制项目
  duplicateProject: (id) => request.post(`/projects/${id}/duplicate`),

  // 归档项目
  archiveProject: (id) => request.post(`/projects/${id}/archive`),

  // 恢复项目
  restoreProject: (id) => request.post(`/projects/${id}/restore`),

  // 获取项目分析结果
  getAnalysisResult: (id) => request.get(`/projects/${id}/analysis`),

  // 获取项目分析历史
  getAnalysisHistory: (id) => request.get(`/projects/${id}/analysis/history`)
}