import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { message } from 'ant-design-vue'
import { uploadApi } from '@/api/upload'

export const useUploadStore = defineStore('upload', () => {
  // State
  const uploadProgress = ref(0)
  const uploading = ref(false)
  const uploadedFiles = ref([])
  const currentUploadTask = ref(null)
  const uploadError = ref(null)
  const fileTree = ref([])
  const selectedFile = ref(null)
  const fileContent = ref('')

  // Getters
  const isUploading = computed(() => uploading.value)
  const hasUploadedFiles = computed(() => uploadedFiles.value.length > 0)
  const progressPercentage = computed(() => uploadProgress.value)
  const hasSelectedFile = computed(() => selectedFile.value !== null)
  const hasFileContent = computed(() => fileContent.value !== '')

  const totalFilesCount = computed(() => uploadedFiles.value.length)
  const totalSize = computed(() => {
    return uploadedFiles.value.reduce((total, file) => total + (file.size || 0), 0)
  })

  // Actions
  const uploadFiles = async (projectId, files) => {
    if (uploading.value) {
      message.warning('当前有上传任务正在进行')
      return
    }

    uploading.value = true
    uploadProgress.value = 0
    uploadError.value = null

    try {
      const formData = new FormData()

      // 添加文件到 FormData
      if (Array.isArray(files)) {
        files.forEach(file => {
          formData.append('files', file)
        })
      } else {
        formData.append('files', files)
      }

      currentUploadTask.value = await uploadApi.uploadFiles(
        projectId,
        formData,
        (progress) => {
          uploadProgress.value = progress
        }
      )

      // 模拟上传完成后的文件列表更新
      const uploadedFileList = Array.isArray(files) ? files : [files]
      uploadedFiles.value = [
        ...uploadedFiles.value,
        ...uploadedFileList.map(file => ({
          id: Date.now() + Math.random(),
          name: file.name,
          size: file.size,
          type: file.type,
          uploadedAt: new Date().toISOString()
        }))
      ]

      message.success('文件上传成功')
      return currentUploadTask.value
    } catch (err) {
      uploadError.value = err.message || '文件上传失败'
      message.error(uploadError.value)
      throw err
    } finally {
      uploading.value = false
      currentUploadTask.value = null
    }
  }

  const uploadGitRepo = async (projectId, repoData) => {
    uploading.value = true
    uploadProgress.value = 0
    uploadError.value = null

    try {
      const response = await uploadApi.importGitRepo(projectId, repoData)
      currentUploadTask.value = response.data

      message.success('Git仓库导入成功')
      return currentUploadTask.value
    } catch (err) {
      uploadError.value = err.message || 'Git仓库导入失败'
      message.error(uploadError.value)
      throw err
    } finally {
      uploading.value = false
    }
  }

  const fetchFileTree = async (projectId) => {
    try {
      const response = await uploadApi.getFiles(projectId)
      fileTree.value = response.data || []
      return fileTree.value
    } catch (err) {
      console.error('获取文件列表失败:', err)
      throw err
    }
  }

  const fetchFileContent = async (projectId, filePath) => {
    try {
      const response = await uploadApi.getFileContent(projectId, filePath)
      fileContent.value = response.data.content || ''
      selectedFile.value = { path: filePath, name: filePath.split('/').pop() }
      return fileContent.value
    } catch (err) {
      console.error('获取文件内容失败:', err)
      message.error('获取文件内容失败')
      throw err
    }
  }

  const selectFile = (file) => {
    selectedFile.value = file
    // 如果文件有路径，则获取文件内容
    if (file.path) {
      // 这里需要 projectId，需要从参数或其他地方获取
      // fetchFileContent(projectId, file.path)
    }
  }

  const clearSelectedFile = () => {
    selectedFile.value = null
    fileContent.value = ''
  }

  const removeUploadedFile = (fileId) => {
    uploadedFiles.value = uploadedFiles.value.filter(file => file.id !== fileId)
    message.success('文件已移除')
  }

  const clearUploadedFiles = () => {
    uploadedFiles.value = []
    message.success('已清空上传文件')
  }

  const resetUpload = () => {
    uploadProgress.value = 0
    uploading.value = false
    currentUploadTask.value = null
    uploadError.value = null
    selectedFile.value = null
    fileContent.value = ''
  }

  const clearError = () => {
    uploadError.value = null
  }

  const formatFileSize = (bytes) => {
    if (bytes === 0) return '0 B'
    const k = 1024
    const sizes = ['B', 'KB', 'MB', 'GB']
    const i = Math.floor(Math.log(bytes) / Math.log(k))
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
  }

  const getFileExtension = (fileName) => {
    if (!fileName || typeof fileName !== 'string') {
      return 'unknown'
    }
    const parts = fileName.split('.')
    return parts.length > 1 ? parts.pop().toLowerCase() : 'unknown'
  }

  const getLanguageFromExtension = (extension) => {
    const languageMap = {
      js: 'javascript',
      jsx: 'javascript',
      ts: 'typescript',
      tsx: 'typescript',
      py: 'python',
      java: 'java',
      cpp: 'cpp',
      c: 'c',
      cs: 'csharp',
      php: 'php',
      rb: 'ruby',
      go: 'go',
      rs: 'rust',
      swift: 'swift',
      kt: 'kotlin',
      html: 'html',
      css: 'css',
      scss: 'scss',
      less: 'less',
      json: 'json',
      xml: 'xml',
      yaml: 'yaml',
      yml: 'yaml',
      md: 'markdown',
      sql: 'sql'
    }
    return languageMap[extension] || 'plaintext'
  }

  return {
    // State
    uploadProgress,
    uploading,
    uploadedFiles,
    currentUploadTask,
    uploadError,
    fileTree,
    selectedFile,
    fileContent,

    // Getters
    isUploading,
    hasUploadedFiles,
    progressPercentage,
    hasSelectedFile,
    hasFileContent,
    totalFilesCount,
    totalSize,

    // Actions
    uploadFiles,
    uploadGitRepo,
    fetchFileTree,
    fetchFileContent,
    selectFile,
    clearSelectedFile,
    removeUploadedFile,
    clearUploadedFiles,
    resetUpload,
    clearError,
    formatFileSize,
    getFileExtension,
    getLanguageFromExtension
  }
})