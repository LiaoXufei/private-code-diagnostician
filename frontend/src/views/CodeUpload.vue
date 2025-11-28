<template>
  <div class="code-upload-page">
    <div class="page-header">
      <h1 class="page-title">
        <UploadOutlined />
        代码上传
      </h1>
      <p class="page-description">
        上传您的代码文件或导入Git仓库开始分析
      </p>
    </div>

    <div class="content-container">
      <a-row :gutter="24">
        <a-col :span="16">
          <a-card title="上传方式" class="upload-card">
            <a-tabs v-model:activeKey="activeTab" type="card">
              <!-- 文件上传 -->
              <a-tab-pane key="file" tab="文件上传">
                <div class="upload-section">
                  <a-upload-dragger
                    v-model:fileList="fileList"
                    :multiple="true"
                    :directory="false"
                    :before-upload="beforeUpload"
                    @change="handleFileChange"
                    @drop="handleDrop"
                    @dragover="handleDragOver"
                    :show-upload-list="false"
                    accept=".js,.jsx,.ts,.tsx,.vue,.java,.py,.cpp,.c,.cs,.go,.rs,.php,.rb,.swift,.kt,.html,.css,.scss,.less,.sass,.json,.xml,.yaml,.yml,.md,.txt,.sql"
                  >
                    <p class="ant-upload-drag-icon">
                      <InboxOutlined />
                    </p>
                    <p class="ant-upload-text">点击或拖拽文件到此区域上传</p>
                    <p class="ant-upload-hint">
                      支持单个或批量上传，严格禁止上传公司数据或其他敏感信息
                    </p>
                  </a-upload-dragger>

                  <!-- 项目文件列表 -->
                  <div v-if="displayFiles.length > 0" class="uploaded-files">
                    <div class="files-header">
                      <h4>项目文件列表</h4>
                      <div class="file-actions">
                        <a-button size="small" @click="selectAllFiles">
                          <CheckSquareOutlined />
                          全选
                        </a-button>
                        <a-button size="small" @click="clearSelection">
                          <BorderOutlined />
                          清除选择
                        </a-button>
                      </div>
                    </div>

                    <div class="selected-files-info" v-if="selectedFiles.length > 0">
                      <a-alert
                        :message="`已选择 ${selectedFiles.length} 个文件`"
                        type="info"
                        show-icon
                        style="margin-bottom: 16px"
                      >
                        <template #action>
                          <a-button type="link" size="small" @click="analyzeSelectedFiles">
                            <ExperimentOutlined />
                            分析选中文件
                          </a-button>
                        </template>
                      </a-alert>
                    </div>

                    <a-list
                      :data-source="displayFiles"
                      size="small"
                    >
                      <template #renderItem="{ item }">
                        <a-list-item
                          :class="{ 'selected-file': isFileSelected(item) }"
                          @click="toggleFileSelection(item)"
                        >
                          <a-list-item-meta>
                            <template #title>
                              <div class="file-item">
                                <a-checkbox
                                  :checked="isFileSelected(item)"
                                  @change="(e) => handleFileSelection(e, item)"
                                  @click.stop
                                />
                                <FileOutlined />
                                {{ item.filename || item.file_name || item.name }}
                              </div>
                            </template>
                            <template #description>
                              <div class="file-info">
                                <span class="file-size">{{ formatFileSize(item.file_size || item.size) }}</span>
                                <span class="file-ext">{{ getFileExtension(item.filename || item.file_name || item.name) }}</span>
                                <a-tag size="small" color="blue">
                                  {{ getLanguageName(uploadStore.getFileExtension(item.filename || item.file_name || item.name)) }}
                                </a-tag>
                                <a-button
                                  type="text"
                                  size="small"
                                  danger
                                  @click.stop="removeFile(item)"
                                >
                                  <DeleteOutlined />
                                </a-button>
                              </div>
                            </template>
                          </a-list-item-meta>
                        </a-list-item>
                      </template>
                    </a-list>
                  </div>

                  <!-- 上传进度 -->
                  <div v-if="uploadStore.uploadProgress > 0 && uploadStore.uploadProgress < 100" class="upload-progress">
                    <a-progress
                      :percent="uploadStore.uploadProgress"
                      :status="uploadStore.uploading ? 'active' : 'success'"
                    />
                    <p class="progress-text">正在上传... ({{ uploadStore.uploadProgress }}%)</p>
                  </div>
                </div>
              </a-tab-pane>

              <!-- Git仓库导入 -->
              <a-tab-pane key="git" tab="Git仓库">
                <div class="git-section">
                  <a-form
                    ref="gitFormRef"
                    :model="gitForm"
                    :rules="gitRules"
                    layout="vertical"
                  >
                    <a-form-item label="仓库地址" name="url" required>
                      <a-input
                        v-model:value="gitForm.url"
                        placeholder="https://github.com/username/repository.git"
                        size="large"
                      />
                      <div class="form-help-text">
                        支持 GitHub、GitLab、Gitee 等主流Git服务
                      </div>
                    </a-form-item>

                    <a-row :gutter="16">
                      <a-col :span="12">
                        <a-form-item label="分支" name="branch">
                          <a-input
                            v-model:value="gitForm.branch"
                            placeholder="main"
                            size="large"
                          />
                        </a-form-item>
                      </a-col>
                      <a-col :span="12">
                        <a-form-item label="目录" name="path">
                          <a-input
                            v-model:value="gitForm.path"
                            placeholder="src (可选)"
                            size="large"
                          />
                        </a-form-item>
                      </a-col>
                    </a-row>

                    <a-form-item label="认证方式" name="authType">
                      <a-radio-group v-model:value="gitForm.authType">
                        <a-radio value="public">公开仓库</a-radio>
                        <a-radio value="token">Token认证</a-radio>
                        <a-radio value="password">用户名密码</a-radio>
                      </a-radio-group>
                    </a-form-item>

                    <a-form-item
                      v-if="gitForm.authType !== 'public'"
                      label="访问令牌"
                      name="token"
                    >
                      <a-input-password
                        v-model:value="gitForm.token"
                        placeholder="请输入访问令牌"
                        size="large"
                      />
                      <div class="form-help-text">
                        令牌将被加密存储，仅用于本次导入
                      </div>
                    </a-form-item>

                    <a-form-item>
                      <a-button
                        type="primary"
                        size="large"
                        block
                        :loading="importing"
                        @click="importGitRepo"
                      >
                        <BranchesOutlined />
                        导入Git仓库
                      </a-button>
                    </a-form-item>
                  </a-form>
                </div>
              </a-tab-pane>
            </a-tabs>
          </a-card>

          <!-- 文件树预览 -->
          <a-card v-if="fileTree.length > 0" title="文件结构" class="tree-card">
            <VirtualFileTree
              :files="fileTree"
              :height="400"
              @select="handleFileSelect"
              @preview="handleFilePreview"
              @download="handleFileDownload"
              @delete="handleFileDelete"
            />
          </a-card>

          <!-- 文件内容预览 -->
          <a-card v-if="selectedFile" title="文件预览" class="preview-card">
            <div class="file-preview">
              <div class="preview-header">
                <h4>{{ selectedFile.name }}</h4>
                <a-space>
                  <a-button size="small" @click="downloadFile">
                    <DownloadOutlined />
                    下载
                  </a-button>
                </a-space>
              </div>
              <div class="preview-content">
                <CodeEditor
                  v-model="fileContent"
                  :language="selectedFile.language"
                  :read-only="true"
                  :height="400"
                />
              </div>
            </div>
          </a-card>
        </a-col>

        <a-col :span="8">
          <!-- 上传统统计 -->
          <a-card title="上传统统计" class="stats-card">
            <div class="upload-stats">
              <a-row :gutter="16">
                <a-col :span="12">
                  <a-statistic
                    title="文件数量"
                    :value="displayFileStats.totalFiles"
                    :prefix="h(FileOutlined)"
                    :loading="loadingProjectFiles"
                  />
                </a-col>
                <a-col :span="12">
                  <a-statistic
                    title="总大小"
                    :value="formatFileSize(displayFileStats.totalSize)"
                    :prefix="h(HddOutlined)"
                    :loading="loadingProjectFiles"
                  />
                </a-col>
              </a-row>

              <a-divider />

              <div class="file-types">
                <h4>文件类型分布</h4>
                <div class="type-stats">
                  <div v-for="(count, type) in fileTypeStats" :key="type" class="type-item">
                    <span class="type-name">{{ getLanguageName(type) }}</span>
                    <a-progress
                      :percent="(count / displayFileStats.totalFiles * 100).toFixed(1)"
                      size="small"
                      :show-info="true"
                    />
                  </div>
                </div>
              </div>
            </div>
          </a-card>

          <!-- 快速操作 -->
          <a-card title="快速操作" class="actions-card">
            <div class="quick-actions">
              <a-space direction="vertical" style="width: 100%">
                <a-button
                  type="primary"
                  block
                  :disabled="hasUploadedFiles"
                  @click="startAnalysis"
                >
                  <ExperimentOutlined />
                  开始分析
                </a-button>
                <a-button
                  block
                  :disabled="hasUploadedFiles"
                  @click="clearAllFiles"
                >
                  <ClearOutlined />
                  清空文件
                </a-button>
<!--                <a-button-->
<!--                  block-->
<!--                  @click="goToAnalysis"-->
<!--                >-->
<!--                  <RightOutlined />-->
<!--                  前往分析-->
<!--                </a-button>-->
              </a-space>
            </div>
          </a-card>

          <!-- 支持的格式 -->
          <a-card title="支持的文件格式" class="formats-card">
            <div class="supported-formats">
              <a-tag
                v-for="format in supportedFormats"
                :key="format"
                color="blue"
                class="format-tag"
              >
                {{ format }}
              </a-tag>
            </div>
            <div class="upload-tips">
              <h4>上传提示</h4>
              <ul>
                <li>单个文件大小不超过 10MB</li>
                <li>项目总大小不超过 100MB</li>
                <li>支持常见的编程语言文件格式</li>
                <li>自动忽略 .gitignore 中指定的文件</li>
              </ul>
            </div>
          </a-card>
        </a-col>
      </a-row>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import { h } from 'vue'
import { useUploadStore } from '@/stores/upload'
import { projectApi } from '@/api/project'
import {
  UploadOutlined,
  InboxOutlined,
  FileOutlined,
  DeleteOutlined,
  DownloadOutlined,
  BranchesOutlined,
  ExperimentOutlined,
  ClearOutlined,
  RightOutlined,
  HddOutlined,
  CheckSquareOutlined,
  BorderOutlined
} from '@ant-design/icons-vue'
import VirtualFileTree from '@/components/common/VirtualFileTree.vue'
import CodeEditor from '@/components/common/CodeEditor.vue'

const route = useRoute()
const router = useRouter()
const uploadStore = useUploadStore()

const activeTab = ref('file')
const fileList = ref([])
const uploadedFiles = ref([])
const selectedFiles = ref([]) // 用于选择性分析的文件列表
const fileTree = ref([])
const selectedFile = ref(null)
const fileContent = ref('')
const expandedKeys = ref([])
const importing = ref(false)
const gitFormRef = ref()

// 项目文件数据
const projectFiles = ref([])
const projectFileStats = ref({
  totalFiles: 0,
  totalSize: 0
})
const loadingProjectFiles = ref(false)

const gitForm = reactive({
  url: '',
  branch: 'main',
  path: '',
  authType: 'public',
  token: ''
})

const gitRules = {
  url: [
    { required: true, message: '请输入仓库地址', trigger: 'blur' },
    { type: 'url', message: '请输入有效的仓库地址', trigger: 'blur' }
  ],
  branch: [
    { required: true, message: '请输入分支名', trigger: 'blur' }
  ]
}

// 获取项目文件数据
const loadProjectFiles = async () => {
  const projectId = route.params.id
  if (!projectId) return

  loadingProjectFiles.value = true
  try {
    const response = await projectApi.getProjectFiles(projectId)
    projectFiles.value = response.data.files || []
    projectFileStats.value = {
      totalFiles: response.data.totalFiles || 0,
      totalSize: response.data.totalSize || 0
    }
  } catch (error) {
    console.error('获取项目文件失败:', error)
    // 不显示错误消息，因为这是后台加载
  } finally {
    loadingProjectFiles.value = false
  }
}

const hasUploadedFiles = computed(() => uploadedFiles.value.length > 0)

// 始终使用数据库中的真实文件数据
const displayFileStats = computed(() => {
  return {
    totalFiles: projectFileStats.value.totalFiles,
    totalSize: projectFileStats.value.totalSize
  }
})

const displayFiles = computed(() => {
  return projectFiles.value
})

const fileTypeStats = computed(() => {
  const stats = {}
  const files = displayFiles.value
  files.forEach(file => {
    const ext = uploadStore.getFileExtension(file.filename || file.file_name || file.name)
    stats[ext] = (stats[ext] || 0) + 1
  })
  return stats
})

const supportedFormats = [
  '.js', '.jsx', '.ts', '.tsx', '.vue',
  '.java', '.py', '.cpp', '.c', '.cs', '.go', '.rs',
  '.php', '.rb', '.swift', '.kt',
  '.html', '.css', '.scss', '.less', '.sass',
  '.json', '.xml', '.yaml', '.yml', '.md',
  '.txt', '.sql'
]

const beforeUpload = (file) => {
  const isValidSize = file.size / 1024 / 1024 < 10
  if (!isValidSize) {
    message.error('文件大小不能超过 10MB')
    return false
  }

  const isValidType = supportedFormats.some(format =>
    file.name.toLowerCase().endsWith(format)
  )
  if (!isValidType) {
    message.error('不支持的文件格式')
    return false
  }

  // 阻止自动上传，我们将手动触发上传
  return false
}

const handleFileChange = ({ fileList: newFileList }) => {
  fileList.value = newFileList.slice(-10) // 最多保留10个文件

  // 自动上传新添加的文件
  if (newFileList.length > 0 && newFileList.some(file => file.originFileObj)) {
    uploadFiles()
  }
}

const handleDragOver = (e) => {
  e.preventDefault()
  e.stopPropagation()
}

const handleDrop = (e) => {
  e.preventDefault()
  e.stopPropagation()

  const files = Array.from(e.dataTransfer.files)
  if (files.length === 0) return

  // 验证文件
  const validFiles = []
  const invalidFiles = []

  files.forEach(file => {
    const isValidSize = file.size / 1024 / 1024 < 10
    const isValidType = supportedFormats.some(format =>
      file.name.toLowerCase().endsWith(format)
    )

    if (!isValidSize) {
      invalidFiles.push({ name: file.name, error: '文件大小超过10MB' })
    } else if (!isValidType) {
      invalidFiles.push({ name: file.name, error: '不支持的文件格式' })
    } else {
      validFiles.push(file)
    }
  })

  // 显示错误信息
  if (invalidFiles.length > 0) {
    invalidFiles.forEach(item => {
      message.error(`${item.name}: ${item.error}`)
    })
  }

  // 如果有有效文件，添加到文件列表
  if (validFiles.length > 0) {
    const newFiles = validFiles.map(file => ({
      uid: Date.now() + Math.random(),
      name: file.name,
      size: file.size,
      type: file.type,
      status: 'uploading',
      originFileObj: file
    }))

    fileList.value = [...fileList.value, ...newFiles].slice(-10) // 最多保留10个文件

    // 自动上传
    uploadFiles()
  }
}

const uploadFiles = async () => {
  if (fileList.value.length === 0) {
    message.warning('请先选择文件')
    return
  }

  const projectId = route.params.id
  if (!projectId) {
    message.error('缺少项目ID')
    return
  }

  try {
    const files = fileList.value
      .filter(item => item.originFileObj) // 只处理实际的文件对象
      .map(item => item.originFileObj)

    if (files.length === 0) {
      message.warning('没有有效的文件可上传')
      return
    }

    await uploadStore.uploadFiles(projectId, files)

    // 重新从数据库加载项目文件数据
    await loadProjectFiles()

    // 清空上传队列
    fileList.value = []
    message.success('文件上传成功')
  } catch (error) {
    console.error('上传失败:', error)
    message.error('文件上传失败: ' + (error.message || '未知错误'))
  }
}

const importGitRepo = async () => {
  try {
    await gitFormRef.value.validate()

    importing.value = true
    const projectId = route.params.id

    await uploadStore.uploadGitRepo(projectId, gitForm)

    // 重新从数据库加载项目文件数据
    await loadProjectFiles()

    message.success('Git仓库导入成功')

    // 刷新文件树
    await fetchFileTree(projectId)
  } catch (error) {
    console.error('导入失败:', error)
  } finally {
    importing.value = false
  }
}

const removeFile = async (file) => {
  const projectId = route.params.id
  if (!projectId) {
    message.error('缺少项目ID')
    return
  }

  try {
    // 确认删除
    Modal.confirm({
      title: '确认删除',
      content: `确定要删除文件 "${file.filename || file.file_name || file.name}" 吗？此操作不可撤销。`,
      okText: '删除',
      okType: 'danger',
      cancelText: '取消',
      onOk: async () => {
        try {
          // 调用删除API
          await projectApi.deleteProjectFile(projectId, file.id)

          // 从本地状态中移除文件
          projectFiles.value = projectFiles.value.filter(f => f.id !== file.id)

          // 从选中文件列表中移除
          selectedFiles.value = selectedFiles.value.filter(f =>
            !((f.id && f.id === file.id) ||
              (f.filename && f.filename === (file.filename || file.file_name || file.name)) ||
              (f.file_name && f.file_name === (file.filename || file.file_name || file.name)) ||
              (f.name && f.name === (file.filename || file.file_name || file.name)))
          )

          // 重新加载项目文件数据
          await loadProjectFiles()

          message.success('文件删除成功')
        } catch (error) {
          console.error('删除文件失败:', error)
          message.error('删除文件失败: ' + (error.response?.data?.error || error.message))
        }
      }
    })
  } catch (error) {
    console.error('删除文件失败:', error)
    message.error('删除文件失败')
  }
}

const clearAllFiles = () => {
  uploadedFiles.value = []
  fileList.value = []
  uploadStore.clearUploadedFiles()
  fileTree.value = []
  selectedFile.value = null
  fileContent.value = ''
}

const fetchFileTree = async (projectId) => {
  try {
    fileTree.value = await uploadStore.fetchFileTree(projectId)
  } catch (error) {
    console.error('获取文件树失败:', error)
  }
}

const handleFileSelect = async (file) => {
  selectedFile.value = {
    ...file,
    language: uploadStore.getLanguageFromExtension(uploadStore.getFileExtension(file.name))
  }

  const projectId = route.params.id
  if (projectId) {
    try {
      fileContent.value = await uploadStore.fetchFileContent(projectId, file.path)
    } catch (error) {
      console.error('获取文件内容失败:', error)
    }
  }
}

const handleFilePreview = (file) => {
  handleFileSelect(file)
}

const handleFileDownload = (file) => {
  downloadFile(file)
}

const handleFileDelete = (file) => {
  removeFile(file)
}

const downloadFile = (file = null) => {
  const targetFile = file || selectedFile.value
  if (!targetFile) return

  const blob = new Blob([fileContent.value], { type: 'text/plain' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = targetFile.name
  a.click()
  URL.revokeObjectURL(url)
}

const formatFileSize = (bytes) => {
  return uploadStore.formatFileSize(bytes)
}

const getLanguageName = (extension) => {
  const languageMap = {
    js: 'JavaScript',
    jsx: 'React JSX',
    ts: 'TypeScript',
    tsx: 'React TSX',
    vue: 'Vue',
    java: 'Java',
    py: 'Python',
    cpp: 'C++',
    c: 'C',
    cs: 'C#',
    go: 'Go',
    rs: 'Rust'
  }
  return languageMap[extension] || extension.toUpperCase()
}

const startAnalysis = () => {
  const projectId = route.params.id
  router.push(`/project/${projectId}/analysis`)
}

const goToAnalysis = () => {
  const projectId = route.params.id
  router.push(`/project/${projectId}/analysis`)
}

// 文件选择相关函数
const isFileSelected = (file) => {
  return selectedFiles.value.some(f =>
    (f.id && f.id === file.id) ||
    (f.filename && f.filename === (file.filename || file.file_name || file.name)) ||
    (f.file_name && f.file_name === (file.filename || file.file_name || file.name)) ||
    (f.name && f.name === (file.filename || file.file_name || file.name))
  )
}

const handleFileSelection = (event, file) => {
  if (event.target.checked) {
    if (!isFileSelected(file)) {
      selectedFiles.value.push(file)
    }
  } else {
    selectedFiles.value = selectedFiles.value.filter(f =>
      !((f.id && f.id === file.id) ||
        (f.filename && f.filename === (file.filename || file.file_name || file.name)) ||
        (f.file_name && f.file_name === (file.filename || file.file_name || file.name)) ||
        (f.name && f.name === (file.filename || file.file_name || file.name)))
    )
  }
}

const toggleFileSelection = (file) => {
  if (isFileSelected(file)) {
    selectedFiles.value = selectedFiles.value.filter(f =>
      !((f.id && f.id === file.id) ||
        (f.filename && f.filename === (file.filename || file.file_name || file.name)) ||
        (f.file_name && f.file_name === (file.filename || file.file_name || file.name)) ||
        (f.name && f.name === (file.filename || file.file_name || file.name)))
    )
  } else {
    selectedFiles.value.push(file)
  }
}

const selectAllFiles = () => {
  selectedFiles.value = [...displayFiles.value]
}

const clearSelection = () => {
  selectedFiles.value = []
}

const analyzeSelectedFiles = () => {
  if (selectedFiles.value.length === 0) {
    message.warning('请先选择要分析的文件')
    return
  }

  const projectId = route.params.id
  if (!projectId) {
    message.error('缺少项目ID')
    return
  }

  // 存储选中的文件到localStorage供分析页面使用
  localStorage.setItem('selectedFiles', JSON.stringify(selectedFiles.value.map(f => ({
    id: f.id,
    name: f.filename || f.file_name || f.name,
    size: f.file_size || f.size,
    path: f.file_path || f.path
  }))))

  message.success(`已选择 ${selectedFiles.value.length} 个文件，即将跳转到分析页面`)

  // 跳转到分析页面
  setTimeout(() => {
    router.push(`/project/${projectId}/analysis`)
  }, 1000)
}

const getFileExtension = (filename) => {
  if (!filename || typeof filename !== 'string') {
    return '.unknown'
  }
  const parts = filename.split('.')
  return parts.length > 1 ? `.${parts.pop().toLowerCase()}` : '.unknown'
}

// 监听路由变化，切换项目时清理状态
watch(() => route.params.id, (newProjectId, oldProjectId) => {
  if (newProjectId !== oldProjectId) {
    // 清理上传相关的临时数据
    uploadedFiles.value = []
    fileList.value = []
    selectedFiles.value = [] // 清理选中的文件

    // 重置项目数据
    projectFiles.value = []
    projectFileStats.value = { totalFiles: 0, totalSize: 0 }

    // 加载新项目的数据
    if (newProjectId) {
      fetchFileTree(newProjectId)
      loadProjectFiles()
    }
  }
}, { immediate: false })

onMounted(() => {
  const projectId = route.params.id
  if (projectId) {
    fetchFileTree(projectId)
    loadProjectFiles() // 加载项目文件统计数据
  }
})
</script>

<style scoped>
.code-upload-page {
  max-width: 1200px;
  margin: 0 auto;
}

.page-header {
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 1px solid #f0f0f0;
}

.page-title {
  font-size: 24px;
  font-weight: 600;
  color: #262626;
  margin: 0 0 8px 0;
  display: flex;
  align-items: center;
  gap: 8px;
}

.page-description {
  color: #8c8c8c;
  margin: 0;
  font-size: 14px;
}

.content-container {
  min-height: 600px;
}

.upload-card,
.tree-card,
.preview-card,
.stats-card,
.actions-card,
.formats-card {
  margin-bottom: 24px;
}

.upload-section {
  min-height: 200px;
}

.uploaded-files {
  margin-top: 24px;
  padding-top: 16px;
  border-top: 1px solid #f0f0f0;
}

.uploaded-files h4 {
  margin-bottom: 12px;
  color: #262626;
}

.file-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.file-info {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
}

.file-size {
  color: #8c8c8c;
  font-size: 12px;
}

.upload-progress {
  margin-top: 16px;
  text-align: center;
}

.progress-text {
  margin-top: 8px;
  color: #1890ff;
  font-size: 14px;
}

.git-section {
  padding: 16px 0;
}

.form-help-text {
  color: #8c8c8c;
  font-size: 12px;
  margin-top: 4px;
}

.file-preview {
  height: 500px;
  display: flex;
  flex-direction: column;
}

.preview-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #f0f0f0;
}

.preview-header h4 {
  margin: 0;
  color: #262626;
}

.preview-content {
  flex: 1;
  overflow: hidden;
}

.upload-stats {
  text-align: center;
}

.type-stats {
  margin-top: 16px;
}

/* 文件选择相关样式 */
.files-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.files-header h4 {
  margin: 0;
  color: #262626;
}

.file-actions {
  display: flex;
  gap: 8px;
}

.selected-files-info {
  margin-bottom: 16px;
}

.file-item {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 4px 0;
}

.file-item:hover {
  background-color: #f5f5f5;
  border-radius: 4px;
}

.selected-file {
  background-color: #e6f7ff;
  border-left: 3px solid #1890ff;
}

.file-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.file-size {
  color: #8c8c8c;
  font-size: 12px;
}

.file-ext {
  background-color: #f0f0f0;
  color: #595959;
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 11px;
  font-family: monospace;
}

.type-item {
  margin-bottom: 8px;
}

.type-name {
  display: block;
  margin-bottom: 4px;
  font-size: 12px;
  color: #666;
}

.quick-actions {
  text-align: center;
}

.supported-formats {
  margin-bottom: 16px;
}

.format-tag {
  margin: 4px;
  cursor: default;
}

.upload-tips h4 {
  margin-bottom: 8px;
  color: #262626;
}

.upload-tips ul {
  margin: 0;
  padding-left: 16px;
  color: #666;
  font-size: 12px;
}

.upload-tips li {
  margin-bottom: 4px;
}

/* 响应式设计 */
@media (max-width: 1200px) {
  .content-container .ant-col {
    margin-bottom: 24px;
  }
}

@media (max-width: 768px) {
  .page-header {
    margin-bottom: 16px;
    padding-bottom: 12px;
  }

  .page-title {
    font-size: 20px;
  }

  .upload-card,
  .tree-card,
  .preview-card,
  .stats-card,
  .actions-card,
  .formats-card {
    margin-bottom: 16px;
  }

  .file-preview {
    height: 400px;
  }
}
</style>