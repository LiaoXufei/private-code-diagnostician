import request from './request'

export const uploadApi = {
  // 上传文件
  uploadFiles: (projectId, formData, onProgress) => {
    return request.post(`/projects/${projectId}/upload`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
      onUploadProgress: (progressEvent) => {
        const progress = Math.round(
          (progressEvent.loaded * 100) / progressEvent.total
        )
        onProgress?.(progress)
      }
    })
  },

  // Git仓库导入
  importGitRepo: (projectId, data) => request.post(`/projects/${projectId}/git`, data),

  // 获取文件列表
  getFiles: (projectId) => request.get(`/projects/${projectId}/files`),

  // 获取文件内容
  getFileContent: (projectId, filePath) => request.get(`/projects/${projectId}/files/content`, {
    params: { path: filePath }
  }),

  // 删除文件
  deleteFile: (projectId, filePath) => request.delete(`/projects/${projectId}/files`, {
    params: { path: filePath }
  }),

  // 获取上传进度
  getUploadProgress: (projectId, taskId) => request.get(`/projects/${projectId}/upload/progress/${taskId}`),

  // 取消上传
  cancelUpload: (projectId, taskId) => request.delete(`/projects/${projectId}/upload/${taskId}`)
}