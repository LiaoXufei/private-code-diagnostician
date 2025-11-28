<template>
  <div class="analysis-page">
    <div class="page-header">
      <h1 class="page-title">
        <ExperimentOutlined />
        代码分析
      </h1>
      <p class="page-description">
        启动代码质量分析，实时查看分析进度
      </p>
    </div>

    <div class="content-container">
      <!-- 主要内容区域 -->
      <a-card class="main-analysis-card" :bordered="false">
        <!-- 项目信息头部 -->
        <div class="project-header">
          <div class="project-info">
            <h2 class="project-name">{{ projectName }}</h2>
            <div class="project-meta">
              <a-tag color="blue">{{ getLanguageName(projectLanguage) }}</a-tag>
              <span class="file-count">{{ fileCount }} 个文件</span>
            </div>
          </div>
          <div class="quick-actions">
            <a-button @click="goToUpload">
              <UploadOutlined />
              上传文件
            </a-button>
          </div>
        </div>

        <!-- 标签导航 -->
        <a-tabs
          v-model:activeKey="activeTab"
          type="card"
          size="large"
          class="analysis-tabs"
          @change="handleTabChange"
        >
          <!-- 文件选择标签页 -->
          <a-tab-pane key="files" tab="文件选择">
            <template #tab>
              <span class="tab-title">
                <FileOutlined />
                文件选择
                <a-badge
                  :count="selectedAnalysisFiles.length"
                  :number-style="{ backgroundColor: '#52c41a' }"
                  v-if="selectedAnalysisFiles.length > 0"
                />
              </span>
            </template>

            <div class="tab-content files-tab">
              <div v-if="availableFiles.length > 0">
                <!-- 文件选择控制栏 -->
                <div class="files-toolbar">
                  <div class="selection-summary">
                    <span class="total-files">共 {{ availableFiles.length }} 个文件</span>
                    <span v-if="selectedAnalysisFiles.length > 0" class="selected-files">
                      已选择 {{ selectedAnalysisFiles.length }} 个
                    </span>
                  </div>
                  <div class="toolbar-actions">
                    <a-button size="small" @click="selectAllAvailableFiles">
                      <CheckSquareOutlined />
                      全选
                    </a-button>
                    <a-button size="small" @click="clearFileSelection">
                      <BorderOutlined />
                      清除
                    </a-button>
                    <a-button size="small" @click="refreshFileList">
                      <ReloadOutlined />
                      刷新
                    </a-button>
                  </div>
                </div>

                <!-- 选中文件提示 -->
                <div class="selected-alert" v-if="selectedAnalysisFiles.length > 0">
                  <a-alert
                    :message="`已选择 ${selectedAnalysisFiles.length} 个文件，总大小 ${formatFileSize(getSelectedFilesSize())}`"
                    type="info"
                    show-icon
                    closable
                    @close="clearFileSelection"
                  />
                </div>

                <!-- 文件列表 -->
                <div class="files-container">
                  <a-table
                    :dataSource="availableFiles"
                    :columns="fileColumns"
                    :pagination="{
                      pageSize: 15,
                      showSizeChanger: true,
                      showQuickJumper: true,
                      showTotal: (total, range) => `第 ${range[0]}-${range[1]} 条，共 ${total} 条`
                    }"
                    :row-selection="{
                      selectedRowKeys: selectedFileKeys,
                      onChange: handleFileSelectionChange,
                      getCheckboxProps: (record) => ({
                        name: record.filename || record.name
                      })
                    }"
                    :scroll="{ y: 400 }"
                    size="small"
                    row-key="id"
                    class="files-table"
                  >
                    <template #bodyCell="{ column, record }">
                      <template v-if="column.key === 'name'">
                        <div class="file-name-cell">
                          <FileOutlined class="file-icon" />
                          <span class="file-name">{{ record.filename || record.name }}</span>
                        </div>
                      </template>
                      <template v-if="column.key === 'size'">
                        <span class="file-size">{{ formatFileSize(record.file_size || record.size) }}</span>
                      </template>
                      <template v-if="column.key === 'language'">
                        <a-tag size="small" :color="getLanguageColor(record.filename || record.name)">
                          {{ getFileExtension(record.filename || record.name) }}
                        </a-tag>
                      </template>
                      <template v-if="column.key === 'path'">
                        <span class="file-path" :title="record.file_path || record.path">
                          {{ record.file_path || record.path }}
                        </span>
                      </template>
                      <template v-if="column.key === 'actions'">
                        <a-button
                          type="text"
                          size="small"
                          danger
                          @click="removeFile(record)"
                        >
                          <DeleteOutlined />
                        </a-button>
                      </template>
                    </template>
                  </a-table>
                </div>

                <!-- 分析操作区域 -->
                <div class="analysis-actions">
                  <a-space size="large">
                    <a-button
                      type="primary"
                      size="large"
                      :disabled="selectedAnalysisFiles.length === 0"
                      @click="startSelectedAnalysis"
                      :loading="analysisStore.loading"
                    >
                      <ExperimentOutlined />
                      分析选中文件 ({{ selectedAnalysisFiles.length }})
                    </a-button>
                    <a-button
                      size="large"
                      @click="startFullAnalysis"
                      :loading="analysisStore.loading"
                    >
                      <PlayCircleOutlined />
                      分析全部文件
                    </a-button>
                  </a-space>
                </div>
              </div>

              <!-- 无文件状态 -->
              <a-empty v-else description="项目中没有可分析的文件" :image="false">
                <a-button type="primary" size="large" @click="goToUpload">
                  <UploadOutlined />
                  去上传文件
                </a-button>
              </a-empty>
            </div>
          </a-tab-pane>

          <!-- 分析任务标签页 -->
          <a-tab-pane key="task" tab="分析任务">
            <template #tab>
              <span class="tab-title">
                <ExperimentOutlined />
                分析任务
                <a-badge
                  :count="analysisStore.isAnalyzing ? 1 : 0"
                  :dot="analysisStore.isAnalyzing"
                />
              </span>
            </template>

            <div class="tab-content task-tab">
              <div v-if="analysisStore.isIdle" class="idle-state">
                <div class="idle-content">
                  <div class="idle-icon">
                    <ExperimentOutlined />
                  </div>
                  <h3>准备开始代码分析</h3>
                  <p>请在"文件选择"标签页中选择要分析的文件，然后开始分析</p>
                  <a-button type="primary" size="large" @click="activeTab = 'files'">
                    去选择文件
                  </a-button>
                </div>
              </div>

              <div v-else-if="analysisStore.isAnalyzing" class="analyzing-state">
                <div class="progress-content">
                  <div class="progress-header">
                    <h3>正在进行代码分析</h3>
                    <a-tag color="processing">分析中</a-tag>
                  </div>

                  <div class="progress-section">
                    <a-progress
                      :percent="analysisStore.progressPercentage"
                      :status="analysisStore.isAnalyzing ? 'active' : 'success'"
                      size="large"
                      :format="percent => `${percent}%`"
                    />

                    <div class="progress-details">
                      <div class="detail-item">
                        <span class="label">当前阶段:</span>
                        <span class="value">{{ analysisStore.statusText }}</span>
                      </div>
                      <div class="detail-item" v-if="estimatedTime">
                        <span class="label">预计剩余:</span>
                        <span class="value">{{ estimatedTime }}分钟</span>
                      </div>
                      <div class="detail-item">
                        <span class="label">已用时间:</span>
                        <span class="value">{{ formatElapsedTime() }}</span>
                      </div>
                    </div>
                  </div>

                  <div class="action-section">
                    <a-button
                      danger
                      @click="cancelAnalysis"
                      :loading="analysisStore.loading"
                      size="large"
                    >
                      <StopOutlined />
                      取消分析
                    </a-button>
                  </div>
                </div>
              </div>

              <div v-else-if="analysisStore.isCompleted" class="completed-state">
                <div class="completed-content">
                  <div class="success-icon">
                    <CheckCircleOutlined />
                  </div>
                  <h3>分析完成</h3>
                  <p>代码分析已成功完成，可以查看详细报告</p>
                  <a-space>
                    <a-button type="primary" size="large" @click="viewReport">
                      <BarChartOutlined />
                      查看报告
                    </a-button>
                    <a-button size="large" @click="restartAnalysis">
                      <RedoOutlined />
                      重新分析
                    </a-button>
                  </a-space>
                </div>
              </div>

              <div v-else-if="analysisStore.isError" class="error-state">
                <div class="error-content">
                  <div class="error-icon">
                    <CloseCircleOutlined />
                  </div>
                  <h3>分析失败</h3>
                  <p>{{ analysisStore.error || '分析过程中发生错误' }}</p>
                  <a-space>
                    <a-button type="primary" size="large" @click="retryAnalysis">
                      <RedoOutlined />
                      重试
                    </a-button>
                    <a-button size="large" @click="goToUpload">
                      <UploadOutlined />
                      检查文件
                    </a-button>
                  </a-space>
                </div>
              </div>
            </div>
          </a-tab-pane>

          <!-- 分析历史标签页 -->
          <a-tab-pane key="history" tab="分析历史">
            <template #tab>
              <span class="tab-title">
                <HistoryOutlined />
                分析历史
              </span>
            </template>

            <div class="tab-content history-tab">
              <div v-if="analysisHistory.length > 0" class="history-content">
                <a-timeline>
                  <a-timeline-item
                    v-for="item in analysisHistory"
                    :key="item.id"
                    :color="getHistoryItemColor(item.grade)"
                  >
                    <template #dot>
                      <component :is="getHistoryIcon(item.grade)" />
                    </template>
                    <div class="history-item">
                      <div class="history-header">
                        <span class="history-time">{{ formatTime(item.completedAt) }}</span>
                        <a-tag :color="getGradeColor(item.grade)">
                          {{ getGradeText(item.grade) }}
                        </a-tag>
                      </div>
                      <div class="history-details">
                        <div class="score-display">
                          <span class="score-label">质量评分:</span>
                          <a-progress
                            :percent="item.overallScore"
                            :status="getScoreStatus(item.overallScore)"
                            size="small"
                            style="width: 120px"
                          />
                          <span class="score-value">{{ item.overallScore }}分</span>
                        </div>
                        <div class="history-meta">
                          <span>耗时: {{ item.duration }}分钟</span>
                          <a-button type="link" size="small" @click="viewHistoryReport(item)">
                            查看报告
                          </a-button>
                        </div>
                      </div>
                    </div>
                  </a-timeline-item>
                </a-timeline>
              </div>

              <a-empty v-else description="暂无分析历史" :image="false">
                <a-button type="primary" @click="startFullAnalysis">
                  <ExperimentOutlined />
                  开始首次分析
                </a-button>
              </a-empty>
            </div>
          </a-tab-pane>

          <!-- 分析配置标签页 -->
          <a-tab-pane key="config" tab="分析配置">
            <template #tab>
              <span class="tab-title">
                <SettingOutlined />
                分析配置
              </span>
            </template>

            <div class="tab-content config-tab">
              <!-- 配置来源提示 -->
              <div class="config-source-info">
                <a-alert
                  :message="configSourceInfo.message"
                  :type="configSourceInfo.type"
                  show-icon
                  style="margin-bottom: 16px"
                >
                  <template #description v-if="configSourceInfo.description">
                    {{ configSourceInfo.description }}
                  </template>
                </a-alert>
              </div>

              <a-form
                :model="analysisConfig"
                layout="vertical"
                class="config-form"
              >
                <a-card title="分析选项" size="small">
                  <a-form-item label="分析类型">
                    <a-checkbox-group v-model:value="analysisConfig.types">
                      <a-row>
                        <a-col :span="8">
                          <a-checkbox value="complexity">代码复杂度</a-checkbox>
                        </a-col>
                        <a-col :span="8">
                          <a-checkbox value="maintainability">可维护性</a-checkbox>
                        </a-col>
                        <a-col :span="8">
                          <a-checkbox value="duplication">代码重复度</a-checkbox>
                        </a-col>
                        <a-col :span="8">
                          <a-checkbox value="coverage">测试覆盖率</a-checkbox>
                        </a-col>
                        <a-col :span="8">
                          <a-checkbox value="security">安全性检查</a-checkbox>
                        </a-col>
                        <a-col :span="8">
                          <a-checkbox value="performance">性能分析</a-checkbox>
                        </a-col>
                        <a-col :span="8">
                          <a-checkbox value="style">代码风格</a-checkbox>
                        </a-col>
                      </a-row>
                    </a-checkbox-group>
                  </a-form-item>

                  <a-form-item label="严格程度">
                    <a-radio-group v-model:value="analysisConfig.strictness">
                      <a-radio value="low">宽松</a-radio>
                      <a-radio value="medium">适中</a-radio>
                      <a-radio value="high">严格</a-radio>
                    </a-radio-group>
                  </a-form-item>

                  <a-form-item label="排除文件/目录">
                    <a-textarea
                      v-model:value="analysisConfig.excludes"
                      placeholder="每行一个文件或目录模式，如：node_modules/, *.min.js"
                      :rows="4"
                    />
                  </a-form-item>
                </a-card>

                <a-card title="性能设置" size="small" style="margin-top: 16px">
                  <a-form-item label="并行处理">
                    <a-switch v-model:checked="analysisConfig.parallel" />
                  </a-form-item>
                  <a-form-item label="最大并发数">
                    <a-input-number
                      v-model:value="analysisConfig.maxConcurrency"
                      :min="1"
                      :max="10"
                      style="width: 120px"
                    />
                  </a-form-item>
                </a-card>

                <div class="config-actions">
                  <a-space>
                    <a-button type="primary" @click="saveConfig">
                      保存配置
                    </a-button>
                    <a-button @click="resetConfig">
                      重置为项目默认
                    </a-button>
                  </a-space>
                </div>
              </a-form>
            </div>
          </a-tab-pane>

          <!-- 分析提示标签页 -->
          <a-tab-pane key="tips" tab="分析提示">
            <template #tab>
              <span class="tab-title">
                <BulbOutlined />
                分析提示
              </span>
            </template>

            <div class="tab-content tips-tab">
              <a-row :gutter="16">
                <a-col :span="12">
                  <a-card title="分析建议" size="small">
                    <ul class="tips-list">
                      <li>建议定期进行代码分析，保持代码质量</li>
                      <li>在提交代码前运行分析，避免低质量代码进入主分支</li>
                      <li>关注重复代码，及时重构提高可维护性</li>
                      <li>复杂度过高的函数应该拆分为更小的单元</li>
                      <li>定期检查安全漏洞，保护应用安全</li>
                    </ul>
                  </a-card>
                </a-col>
                <a-col :span="12">
                  <a-card title="最佳实践" size="small">
                    <ul class="tips-list">
                      <li>保持测试覆盖率在80%以上</li>
                      <li>函数复杂度不宜超过10</li>
                      <li>代码重复率应控制在5%以内</li>
                      <li>及时修复安全和性能问题</li>
                      <li>遵循项目统一的编码规范</li>
                    </ul>
                  </a-card>
                </a-col>
              </a-row>

              <a-card title="质量标准" size="small" style="margin-top: 16px">
                <a-table
                  :dataSource="qualityStandards"
                  :columns="standardsColumns"
                  :pagination="false"
                  size="small"
                />
              </a-card>
            </div>
          </a-tab-pane>

          
        </a-tabs>
      </a-card>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAnalysisStore } from '@/stores/analysis'
import { useProjectStore } from '@/stores/project'
import { projectApi } from '@/api/project'
import { analysisApi } from '@/api/analysis'
import { message } from 'ant-design-vue'
import {
  ExperimentOutlined,
  FileOutlined,
  HistoryOutlined,
  SettingOutlined,
  BulbOutlined,
  UploadOutlined,
  CheckSquareOutlined,
  BorderOutlined,
  ReloadOutlined,
  DeleteOutlined,
  PlayCircleOutlined,
  StopOutlined,
  CheckCircleOutlined,
  CloseCircleOutlined,
  RedoOutlined,
  BarChartOutlined
} from '@ant-design/icons-vue'

const router = useRouter()
const route = useRoute()
const analysisStore = useAnalysisStore()
const projectStore = useProjectStore()

// 响应式数据
const activeTab = ref('files')
const projectName = ref('')
const projectLanguage = ref('')
const fileCount = ref(0)
const estimatedTime = ref(0)
const analysisStartTime = ref(null)

const availableFiles = ref([])
const selectedAnalysisFiles = ref([])
const selectedFileKeys = ref([])

const analysisHistory = ref([])

// 配置来源信息
const configSourceInfo = computed(() => {
  const projectId = route.params.id
  const hasCustomConfig = localStorage.getItem(`analysisConfig_${projectId}`)

  if (hasCustomConfig) {
    return {
      message: '当前使用自定义配置',
      type: 'info',
      description: '您之前保存了自定义的分析配置。点击"重置为项目默认"可恢复基于项目语言和描述的智能配置。'
    }
  } else {
    return {
      message: '当前使用项目默认配置',
      type: 'success',
      description: `基于项目语言(${projectLanguage.value || '未知'})和文件数量(${fileCount.value || 0}个)自动生成的智能配置。`
    }
  }
})

// 分析配置（与项目配置页面保持一致）
const analysisConfig = reactive({
  types: ['complexity', 'maintainability', 'duplication', 'coverage', 'security', 'performance', 'style'],
  strictness: 'medium',
  excludes: 'node_modules/\n*.min.js\n*.test.js\ndist/',
  parallel: true,
  maxConcurrency: 4
})

// 质量标准数据（与7个分析类型保持一致）
const qualityStandards = ref([
  { dimension: '代码复杂度', excellent: '10-', good: '10-15', fair: '16-20', poor: '>20' },
  { dimension: '可维护性', excellent: 'A', good: 'B', fair: 'C', poor: 'D' },
  { dimension: '代码重复度', excellent: '<3%', good: '3-5%', fair: '5-10%', poor: '>10%' },
  { dimension: '测试覆盖率', excellent: '90%+', good: '80-89%', fair: '60-79%', poor: '<60%' },
  { dimension: '安全性检查', excellent: '0个漏洞', good: '1-2个', fair: '3-5个', poor: '>5个' },
  { dimension: '性能分析', excellent: '优秀', good: '良好', fair: '一般', poor: '需优化' },
  { dimension: '代码风格', excellent: '90+', good: '80-89', fair: '60-79', poor: '<60' }
])

const standardsColumns = [
  { title: '质量维度', dataIndex: 'dimension', key: 'dimension' },
  { title: '优秀', dataIndex: 'excellent', key: 'excellent' },
  { title: '良好', dataIndex: 'good', key: 'good' },
  { title: '一般', dataIndex: 'fair', key: 'fair' },
  { title: '较差', dataIndex: 'poor', key: 'poor' }
]

// 文件表格列定义
const fileColumns = [
  {
    title: '文件名',
    dataIndex: 'name',
    key: 'name',
    ellipsis: true,
    width: 300
  },
  {
    title: '大小',
    dataIndex: 'size',
    key: 'size',
    width: 100,
    align: 'right'
  },
  {
    title: '语言',
    dataIndex: 'language',
    key: 'language',
    width: 100
  },
  {
    title: '路径',
    dataIndex: 'path',
    key: 'path',
    ellipsis: true
  },
  {
    title: '操作',
    key: 'actions',
    width: 60,
    align: 'center'
  }
]

// 计算属性
const selectedFilesSize = computed(() => {
  return selectedAnalysisFiles.value.reduce((total, file) => {
    return total + (file.file_size || file.size || 0)
  }, 0)
})

// 文件选择处理
const handleFileSelectionChange = (selectedRowKeys, selectedRows) => {
  selectedFileKeys.value = selectedRowKeys
  selectedAnalysisFiles.value = selectedRows
}

const selectAllAvailableFiles = () => {
  selectedFileKeys.value = availableFiles.value.map(file => file.id)
  selectedAnalysisFiles.value = [...availableFiles.value]
}

const clearFileSelection = () => {
  selectedFileKeys.value = []
  selectedAnalysisFiles.value = []
}

const refreshFileList = async () => {
  await loadProjectFiles(route.params.id)
}

const removeFile = async (file) => {
  const projectId = route.params.id
  if (!projectId) {
    message.error('缺少项目ID')
    return
  }

  try {
    await projectApi.deleteProjectFile(projectId, file.id)
    await loadProjectFiles(projectId)
    message.success('文件删除成功')
  } catch (error) {
    message.error('删除文件失败: ' + error.message)
  }
}

const getSelectedFilesSize = () => {
  return selectedFilesSize.value
}

// 分析相关方法
const startSelectedAnalysis = async () => {
  if (selectedAnalysisFiles.value.length === 0) {
    message.warning('请先选择要分析的文件')
    return
  }

  const projectId = route.params.id
  try {
    analysisStartTime.value = Date.now()
    await analysisStore.startAnalysis(projectId, {
      files: selectedAnalysisFiles.value.map(f => f.id || f.name),
      config: analysisConfig
    })
    activeTab.value = 'task'
  } catch (error) {
    message.error('启动分析失败: ' + error.message)
  }
}

const startFullAnalysis = async () => {
  const projectId = route.params.id
  try {
    analysisStartTime.value = Date.now()
    await analysisStore.startAnalysis(projectId, {
      files: 'all',
      config: analysisConfig
    })
    activeTab.value = 'task'
  } catch (error) {
    message.error('启动分析失败: ' + error.message)
  }
}

const cancelAnalysis = async () => {
  try {
    await analysisStore.cancelAnalysis(route.params.id)
    message.success('分析已取消')
  } catch (error) {
    message.error('取消分析失败: ' + error.message)
  }
}

const viewReport = () => {
  router.push(`/project/${route.params.id}/report`)
}

const viewHistoryReport = (item) => {
  router.push(`/project/${route.params.id}/report/${item.id}`)
}

const restartAnalysis = () => {
  startFullAnalysis()
}

const retryAnalysis = () => {
  startFullAnalysis()
}

const goToUpload = () => {
  router.push(`/project/${route.params.id}/upload`)
}

// 标签切换处理
const handleTabChange = (key) => {
  activeTab.value = key
}

// 加载项目文件
const loadProjectFiles = async (projectId) => {
  try {
    const response = await projectApi.getProjectFiles(projectId)
    availableFiles.value = response.data.files || []

    // 恢复之前的选择
    const savedSelection = localStorage.getItem('selectedAnalysisFiles')
    if (savedSelection) {
      const savedFiles = JSON.parse(savedSelection)
      selectedAnalysisFiles.value = availableFiles.value.filter(file =>
        savedFiles.some(saved =>
          (saved.id && saved.id === file.id) || (saved.name && saved.name === (file.filename || file.name))
        )
      )
      selectedFileKeys.value = selectedAnalysisFiles.value.map(f => f.id)
    }
  } catch (error) {
    console.error('加载项目文件失败:', error)
    message.error('加载项目文件失败')
    availableFiles.value = []
  }
}

// 配置相关方法
const saveConfig = async () => {
  const projectId = route.params.id

  try {
    // 保存到localStorage（作为备用）
    localStorage.setItem(`analysisConfig_${projectId}`, JSON.stringify(analysisConfig))

    // 同时保存到项目数据库
    const projectData = {
      config: {
        analysisTypes: analysisConfig.types,
        excludeRules: analysisConfig.excludes,
        thresholds: {
          complexity: 10,
          maintainability: 65,
          commentRate: 0.1
        },
        includeTests: false
      }
    }

    await projectApi.updateProject(projectId, projectData)

    message.success('配置已保存并同步到项目')
  } catch (error) {
    console.error('保存配置失败:', error)
    message.error('保存配置失败: ' + (error.message || '未知错误'))

    // 即使项目更新失败，localStorage也保存了
    message.info('配置已保存到本地（项目同步失败）')
  }
}

const resetConfig = () => {
  resetToProjectDefaults()
}

 

// 工具方法
const formatFileSize = (bytes) => {
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}

const getFileExtension = (filename) => {
  if (!filename || typeof filename !== 'string') {
    return '.unknown'
  }
  const parts = filename.split('.')
  return parts.length > 1 ? parts.pop().toLowerCase() : 'unknown'
}

const getLanguageColor = (filename) => {
  const ext = getFileExtension(filename)
  const colorMap = {
    js: 'gold',
    jsx: 'gold',
    ts: 'blue',
    tsx: 'blue',
    vue: 'green',
    py: 'orange',
    java: 'red',
    cpp: 'purple',
    c: 'purple',
    cs: 'purple',
    php: 'purple',
    rb: 'red',
    go: 'cyan',
    rs: 'orange',
    html: 'orange',
    css: 'blue',
    scss: 'pink',
    less: 'pink',
    json: 'grey',
    xml: 'grey',
    yaml: 'grey',
    yml: 'grey',
    md: 'blue',
    sql: 'green'
  }
  return colorMap[ext] || 'default'
}

const getLanguageName = (language) => {
  const languageMap = {
    javascript: 'JavaScript',
    typescript: 'TypeScript',
    java: 'Java',
    python: 'Python',
    cpp: 'C++',
    c: 'C',
    cs: 'C#',
    php: 'PHP',
    rb: 'Ruby',
    go: 'Go',
    rs: 'Rust',
    html: 'HTML',
    css: 'CSS',
    scss: 'SCSS',
    less: 'Less',
    json: 'JSON',
    xml: 'XML',
    yaml: 'YAML',
    md: 'Markdown',
    sql: 'SQL'
  }
  return languageMap[language] || language
}

const formatTime = (time) => {
  if (!time) return ''
  const date = new Date(time)
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')
  return `${year}-${month}-${day} ${hours}:${minutes}`
}

 

const formatElapsedTime = () => {
  if (!analysisStartTime.value) return '0分钟'
  const elapsed = Math.floor((Date.now() - analysisStartTime.value) / 60000)
  return `${elapsed}分钟`
}

// 历史相关方法
const getHistoryItemColor = (grade) => {
  const colorMap = {
    excellent: 'green',
    good: 'blue',
    fair: 'orange',
    poor: 'red'
  }
  return colorMap[grade] || 'blue'
}

const getHistoryIcon = (grade) => {
  const iconMap = {
    excellent: 'CheckCircleOutlined',
    good: 'CheckCircleOutlined',
    fair: 'ExclamationCircleOutlined',
    poor: 'CloseCircleOutlined'
  }
  return iconMap[grade] || 'CheckCircleOutlined'
}

const getGradeColor = (grade) => {
  const colorMap = {
    excellent: 'green',
    good: 'blue',
    fair: 'orange',
    poor: 'red'
  }
  return colorMap[grade] || 'default'
}

const getGradeText = (grade) => {
  const textMap = {
    excellent: '优秀',
    good: '良好',
    fair: '一般',
    poor: '较差'
  }
  return textMap[grade] || '未知'
}

const getScoreStatus = (score) => {
  if (score >= 80) return 'success'
  if (score >= 60) return 'normal'
  return 'exception'
}

// 根据项目设置初始化分析配置
const initializeAnalysisConfig = (project) => {
  if (!project) return

  // 根据项目语言设置默认分析类型（与项目配置页面保持一致）
  const languageDefaults = {
    javascript: ['complexity', 'maintainability', 'duplication', 'coverage', 'security', 'performance', 'style'],
    typescript: ['complexity', 'maintainability', 'duplication', 'coverage', 'security', 'performance', 'style'],
    java: ['complexity', 'maintainability', 'duplication', 'coverage', 'security', 'performance', 'style'],
    python: ['complexity', 'maintainability', 'duplication', 'coverage', 'security', 'performance', 'style'],
    cpp: ['complexity', 'maintainability', 'duplication', 'coverage', 'security', 'performance', 'style'],
    c: ['complexity', 'maintainability', 'duplication', 'coverage', 'security', 'performance', 'style']
  }

  // 设置语言相关的默认分析类型
  analysisConfig.types = languageDefaults[project.language] || languageDefaults.javascript

  // 根据项目描述设置严格程度
  const description = (project.description || '').toLowerCase()
  if (description.includes('生产') || description.includes('正式') || description.includes('企业')) {
    analysisConfig.strictness = 'strict'
  } else if (description.includes('测试') || description.includes('实验') || description.includes('demo')) {
    analysisConfig.strictness = 'relaxed'
  } else {
    analysisConfig.strictness = 'medium'
  }

  // 根据项目语言设置默认排除规则
  const languageExcludes = {
    javascript: 'node_modules/\n*.min.js\n*.test.js\ndist/\ncoverage/',
    typescript: 'node_modules/\n*.min.js\n*.test.js\ndist/\ncoverage/\n*.d.ts',
    java: 'target/\n*.class\ntest/\nout/',
    python: '__pycache__/\n*.pyc\n*.pyo\nvenv/\nenv/\ntest/',
    cpp: 'build/\ndist/\n*.o\n*.exe\nCMakeFiles/',
    c: 'build/\ndist/\n*.o\n*.exe\nCMakeFiles/'
  }

  analysisConfig.excludes = languageExcludes[project.language] || languageExcludes.javascript

  // 根据文件数量设置并发数
  if (project.file_count > 100) {
    analysisConfig.maxConcurrency = 6
  } else if (project.file_count > 50) {
    analysisConfig.maxConcurrency = 4
  } else {
    analysisConfig.maxConcurrency = 2
  }
}

// 加载分析历史
const loadAnalysisHistory = async (projectId) => {
  try {
    const response = await projectApi.getAnalysisHistory(projectId)
    analysisHistory.value = response.data || []
    console.log('📈 加载分析历史:', analysisHistory.value.length, '条记录')
  } catch (error) {
    console.error('加载分析历史失败:', error)
    analysisHistory.value = [] // 如果加载失败，设置为空数组
  }
}

// 重新加载项目配置并更新分析配置
const reloadProjectConfig = async () => {
  const projectId = route.params.id
  if (!projectId) return

  try {
    // 重新获取项目信息
    const project = await projectStore.getProject(projectId)
    projectName.value = project.name
    projectLanguage.value = project.language
    fileCount.value = project.file_count || 0

    // 重新加载项目文件列表
    await loadProjectFiles(projectId)

    message.success('项目配置已更新')
  } catch (error) {
    console.error('重新加载项目配置失败:', error)
    message.error('重新加载项目配置失败')
  }
}

// 重置分析配置为项目默认值
const resetToProjectDefaults = async () => {
  const projectId = route.params.id
  if (!projectId) return

  try {
    // 重新获取项目信息
    const project = await projectStore.getProject(projectId)

    // 根据项目设置重新初始化分析配置
    initializeAnalysisConfig(project)

    // 清除保存的配置，使其重新使用项目默认配置
    localStorage.removeItem(`analysisConfig_${projectId}`)

    message.success('已重置为项目默认配置')
  } catch (error) {
    console.error('重置配置失败:', error)
    message.error('重置配置失败')
  }
}

// 从项目获取严格程度
const getStrictnessFromProject = (project) => {
  const description = (project.description || '').toLowerCase()
  if (description.includes('生产') || description.includes('正式') || description.includes('企业')) {
    return 'strict'
  } else if (description.includes('测试') || description.includes('实验') || description.includes('demo')) {
    return 'relaxed'
  }
  return 'medium'
}

// 根据文件数量获取最大并发数
const getMaxConcurrencyFromFiles = (fileCount) => {
  if (fileCount > 100) {
    return 6
  } else if (fileCount > 50) {
    return 4
  } else {
    return 2
  }
}

// 监听项目变化，自动更新配置
watch([projectLanguage, fileCount], async () => {
  const projectId = route.params.id
  if (projectId) {
    try {
      const project = await projectStore.getProject(projectId)

      // 只有在没有保存的自定义配置时，才自动更新项目默认配置
      const savedConfig = localStorage.getItem(`analysisConfig_${projectId}`)
      if (!savedConfig) {
        initializeAnalysisConfig(project)
      }
    } catch (error) {
      console.error('自动更新配置失败:', error)
    }
  }
}, { immediate: false })

// 生命周期
onMounted(async () => {
  const projectId = route.params.id

  if (projectId) {
    try {
      // 加载项目信息
      const project = await projectStore.getProject(projectId)
      projectName.value = project.name
      projectLanguage.value = project.language
      fileCount.value = project.file_count || 0

      // 加载项目文件列表
      await loadProjectFiles(projectId)

      // 优先使用项目数据库中的配置
      if (project.config) {
        let configData = {}
        try {
          configData = typeof project.config === 'string' ? JSON.parse(project.config) : project.config
        } catch (error) {
          console.error('解析项目配置失败:', error)
          configData = {}
        }

        Object.assign(analysisConfig, {
          types: configData.analysisTypes || analysisConfig.types,
          strictness: getStrictnessFromProject(project),
          excludes: configData.excludeRules || analysisConfig.excludes,
          parallel: analysisConfig.parallel,
          maxConcurrency: getMaxConcurrencyFromFiles(project.file_count || 0)
        })
      } else {
        // 如果项目没有配置，检查localStorage中是否有保存的配置
        const savedConfig = localStorage.getItem(`analysisConfig_${projectId}`)
        if (savedConfig) {
          Object.assign(analysisConfig, JSON.parse(savedConfig))
        } else {
          // 只有在没有保存配置时，才根据项目设置初始化分析配置
          initializeAnalysisConfig(project)
        }
      }

      // 加载真实的分析历史
      await loadAnalysisHistory(projectId)

      // 估算分析时间
      estimatedTime.value = Math.ceil((fileCount.value || 50) / 10)
    } catch (error) {
      console.error('加载项目信息失败:', error)
    }
  }
})

onUnmounted(() => {
  // 清理资源
})
</script>

<style scoped>
.analysis-page {
  max-width: 1400px;
  margin: 0 auto;
  padding: 24px;
}

.page-header {
  margin-bottom: 32px;
  padding-bottom: 24px;
  border-bottom: 1px solid #f0f0f0;
}

.page-title {
  font-size: 28px;
  font-weight: 600;
  color: #262626;
  margin: 0 0 8px 0;
  display: flex;
  align-items: center;
  gap: 12px;
}

.page-description {
  color: #8c8c8c;
  margin: 0;
  font-size: 16px;
}

.content-container {
  min-height: 600px;
}

.main-analysis-card {
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.project-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 24px 24px 0;
  border-bottom: 1px solid #f0f0f0;
}

.project-name {
  font-size: 20px;
  font-weight: 600;
  color: #262626;
  margin: 0 0 8px 0;
}

.project-meta {
  display: flex;
  align-items: center;
  gap: 16px;
}

.file-count {
  color: #8c8c8c;
  font-size: 14px;
}

.analysis-tabs {
  margin-top: 24px;
}

.tab-title {
  display: flex;
  align-items: center;
  gap: 8px;
}

.tab-content {
  padding: 24px 0;
  min-height: 400px;
}

/* 文件选择页面样式 */
.files-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  padding: 16px;
  background: #fafafa;
  border-radius: 6px;
}

.selection-summary {
  display: flex;
  align-items: center;
  gap: 16px;
}

.total-files {
  font-weight: 500;
  color: #262626;
}

.selected-files {
  color: #1890ff;
  font-weight: 500;
}

.toolbar-actions {
  display: flex;
  gap: 8px;
}

.selected-alert {
  margin-bottom: 16px;
}

.files-container {
  margin-bottom: 24px;
}

.files-table {
  background: #fff;
}

.file-name-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.file-icon {
  color: #1890ff;
  flex-shrink: 0;
}

.file-name {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.file-size {
  font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
  color: #666;
}

.file-path {
  color: #999;
  font-size: 12px;
}

.analysis-actions {
  text-align: center;
  padding-top: 24px;
  border-top: 1px solid #f0f0f0;
}

/* 分析任务页面样式 */
.idle-state,
.analyzing-state,
.completed-state,
.error-state {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 400px;
}

.idle-content,
.progress-content,
.completed-content,
.error-content {
  text-align: center;
}

.idle-icon,
.success-icon,
.error-icon {
  font-size: 64px;
  margin-bottom: 24px;
}

.idle-icon {
  color: #1890ff;
}

.success-icon {
  color: #52c41a;
}

.error-icon {
  color: #ff4d4f;
}

.progress-content {
  width: 100%;
  max-width: 600px;
}

.progress-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.progress-section {
  margin-bottom: 32px;
}

.progress-details {
  margin-top: 24px;
  text-align: left;
}

.detail-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  padding: 12px 16px;
  background: #f9f9f9;
  border-radius: 6px;
}

.detail-item .label {
  color: #8c8c8c;
  font-weight: 500;
}

.detail-item .value {
  color: #262626;
  font-weight: 600;
}

.action-section {
  margin-top: 24px;
}

/* 历史页面样式 */
.history-content {
  max-height: 500px;
  overflow-y: auto;
  padding: 16px 0;
}

.history-item {
  margin-bottom: 24px;
}

.history-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.history-time {
  color: #666;
  font-size: 14px;
}

.history-details {
  margin-left: 16px;
}

.score-display {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
}

.score-label {
  color: #8c8c8c;
  font-size: 14px;
}

.score-value {
  color: #262626;
  font-weight: 600;
  font-size: 14px;
}

.history-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

/* 配置页面样式 */
.config-form {
  max-width: 600px;
}

.config-actions {
  margin-top: 24px;
  text-align: center;
}

/* 提示页面样式 */
.tips-list {
  margin: 0;
  padding-left: 20px;
}

.tips-list li {
  margin-bottom: 8px;
  color: #666;
  line-height: 1.5;
}

 

/* 响应式设计 */
@media (max-width: 1200px) {
  .analysis-page {
    padding: 16px;
  }

  .project-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 16px;
  }

  .toolbar-actions {
    flex-direction: column;
    gap: 8px;
  }
}

@media (max-width: 768px) {
  .page-title {
    font-size: 24px;
  }

  .analysis-tabs :deep(.ant-tabs-tab) {
    font-size: 14px;
    padding: 8px 16px;
  }

  .files-toolbar {
    flex-direction: column;
    gap: 12px;
    align-items: stretch;
  }

  .selection-summary {
    flex-direction: column;
    gap: 4px;
    align-items: flex-start;
  }

  .toolbar-actions {
    justify-content: flex-start;
  }

  .file-columns :deep(.ant-table-thead > tr > th) {
    padding: 8px;
  }

  .file-columns :deep(.ant-table-tbody > tr > td) {
    padding: 8px;
  }
}
</style>
