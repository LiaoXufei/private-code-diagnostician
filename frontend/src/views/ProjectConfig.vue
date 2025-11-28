<template>
  <div class="project-config-page">
    <div class="page-header">
      <h1 class="page-title">
        <SettingOutlined />
        项目配置
      </h1>
      <p class="page-description">
        配置项目参数和分析规则
      </p>
    </div>

    <div class="content-container">
      <a-row :gutter="24">
        <a-col :span="16">
          <a-card title="基本信息" class="config-card">
            <a-form
              ref="basicFormRef"
              :model="basicForm"
              :rules="basicRules"
              layout="vertical"
            >
              <a-form-item label="项目名称" name="name" required>
                <a-input
                  v-model:value="basicForm.name"
                  placeholder="请输入项目名称"
                  :maxlength="50"
                  show-count
                />
              </a-form-item>

              <a-form-item label="项目描述" name="description">
                <a-textarea
                  v-model:value="basicForm.description"
                  placeholder="请输入项目描述"
                  :rows="3"
                  :maxlength="200"
                  show-count
                />
              </a-form-item>

              <a-form-item label="编程语言" name="language" required>
                <a-select
                  v-model:value="basicForm.language"
                  placeholder="请选择主要编程语言"
                  style="width: 100%"
                >
                  <a-select-option value="javascript">JavaScript</a-select-option>
                  <a-select-option value="typescript">TypeScript</a-select-option>
                  <a-select-option value="java">Java</a-select-option>
                  <a-select-option value="python">Python</a-select-option>
                  <a-select-option value="cpp">C++</a-select-option>
                  <a-select-option value="c">C</a-select-option>
                  <a-select-option value="cs">C#</a-select-option>
                  <a-select-option value="php">PHP</a-select-option>
                  <a-select-option value="go">Go</a-select-option>
                  <a-select-option value="rust">Rust</a-select-option>
                </a-select>
              </a-form-item>

              <a-form-item label="项目状态" name="status">
                <a-radio-group v-model:value="basicForm.status">
                  <a-radio value="active">活跃</a-radio>
                  <a-radio value="inactive">非活跃</a-radio>
                  <a-radio value="archived">已归档</a-radio>
                </a-radio-group>
              </a-form-item>
            </a-form>
          </a-card>

          <a-card title="分析配置" class="config-card">
            <a-form
              ref="analysisFormRef"
              :model="analysisForm"
              :rules="analysisRules"
              layout="vertical"
            >
              <a-form-item label="排除规则" name="excludeRules">
                <a-select
                  v-model:value="analysisForm.excludeRules"
                  mode="tags"
                  placeholder="添加要排除的文件或文件夹模式"
                  style="width: 100%"
                  :token-separators="[',']"
                >
                  <a-select-option value="node_modules/**">node_modules/**</a-select-option>
                  <a-select-option value="dist/**">dist/**</a-select-option>
                  <a-select-option value="build/**">build/**</a-select-option>
                  <a-select-option value="*.min.js">*.min.js</a-select-option>
                  <a-select-option value="*.test.js">*.test.js</a-select-option>
                  <a-select-option value="*.spec.js">*.spec.js</a-select-option>
                </a-select>
                <div class="form-help-text">
                  支持通配符，如 *.min.js、node_modules/**
                </div>
              </a-form-item>

              <a-row :gutter="16">
                <a-col :span="8">
                  <a-form-item label="复杂度阈值" name="complexityThreshold">
                    <a-input-number
                      v-model:value="analysisForm.thresholds.complexity"
                      :min="1"
                      :max="50"
                      style="width: 100%"
                    />
                  </a-form-item>
                </a-col>
                <a-col :span="8">
                  <a-form-item label="可维护性指数" name="maintainabilityThreshold">
                    <a-input-number
                      v-model:value="analysisForm.thresholds.maintainability"
                      :min="0"
                      :max="100"
                      style="width: 100%"
                    />
                  </a-form-item>
                </a-col>
                <a-col :span="8">
                  <a-form-item label="注释率" name="commentRateThreshold">
                    <a-input-number
                      v-model:value="analysisForm.thresholds.commentRate"
                      :min="0"
                      :max="1"
                      :step="0.1"
                      :precision="2"
                      style="width: 100%"
                    />
                  </a-form-item>
                </a-col>
              </a-row>

              <a-form-item label="分析类型" name="analysisTypes">
                <a-checkbox-group v-model:value="analysisForm.analysisTypes">
                  <a-checkbox value="complexity">代码复杂度</a-checkbox>
                  <a-checkbox value="maintainability">可维护性</a-checkbox>
                  <a-checkbox value="duplication">代码重复度</a-checkbox>
                  <a-checkbox value="coverage">测试覆盖率</a-checkbox>
                  <a-checkbox value="security">安全性检查</a-checkbox>
                  <a-checkbox value="performance">性能分析</a-checkbox>
                  <a-checkbox value="style">代码风格</a-checkbox>
                </a-checkbox-group>
              </a-form-item>

              <a-form-item label="包含测试文件" name="includeTests">
                <a-switch v-model:checked="analysisForm.includeTests" />
                <span class="switch-label">是否在分析中包含测试文件</span>
              </a-form-item>
            </a-form>
          </a-card>
        </a-col>

        <a-col :span="8">
          <a-card title="快速操作" class="config-card">
            <div class="quick-actions">
              <a-space direction="vertical" style="width: 100%">
                <a-button block @click="saveConfig" :loading="saving">
                  <SaveOutlined />
                  保存配置
                </a-button>
                <a-button block @click="resetConfig">
                  <ReloadOutlined />
                  重置配置
                </a-button>
                <a-button block @click="exportConfig">
                  <DownloadOutlined />
                  导出配置
                </a-button>
                <a-button block @click="importConfig">
                  <UploadOutlined />
                  导入配置
                </a-button>
              </a-space>
            </div>
          </a-card>

          <a-card title="项目信息" class="config-card">
            <div v-if="projectInfo" class="project-info">
              <div class="info-item">
                <span class="info-label">项目ID:</span>
                <span class="info-value">{{ projectInfo.id }}</span>
              </div>
              <div class="info-item">
                <span class="info-label">创建时间:</span>
                <span class="info-value">{{ formatTime(projectInfo.created_at) }}</span>
              </div>
              <div class="info-item">
                <span class="info-label">最后更新:</span>
                <span class="info-value">{{ formatTime(projectInfo.updated_at) }}</span>
              </div>
              <div class="info-item">
                <span class="info-label">文件数量:</span>
                <span class="info-value">{{ projectInfo.file_count || 0 }}个</span>
              </div>
              <div v-if="projectInfo.lastAnalysis" class="info-item">
                <span class="info-label">最后分析:</span>
                <span class="info-value">{{ formatTime(projectInfo.lastAnalysis.completedAt) }}</span>
              </div>
            </div>
            <a-empty v-else description="暂无项目信息" />
          </a-card>
        </a-col>
      </a-row>

      <!-- 操作按钮 -->
      <div class="action-section">
        <div class="action-buttons">
          <a-space>
            <a-button size="large" @click="goBack">
              <ArrowLeftOutlined />
              返回
            </a-button>
            <a-button
              type="primary"
              size="large"
              @click="saveAndNext"
              :loading="saving"
            >
              保存并下一步
              <ArrowRightOutlined />
            </a-button>
          </a-space>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { useProjectStore } from '@/stores/project'
import {
  SettingOutlined,
  SaveOutlined,
  ReloadOutlined,
  DownloadOutlined,
  UploadOutlined,
  ArrowLeftOutlined,
  ArrowRightOutlined
} from '@ant-design/icons-vue'

const route = useRoute()
const router = useRouter()

const saving = ref(false)
const basicFormRef = ref()
const analysisFormRef = ref()

const projectStore = useProjectStore()

const basicForm = reactive({
  name: '',
  description: '',
  language: undefined,
  status: 'active'
})

const analysisForm = reactive({
  excludeRules: [],
  thresholds: {
    complexity: 10,
    maintainability: 65,
    commentRate: 0.1
  },
  analysisTypes: ['complexity', 'maintainability', 'style'],
  includeTests: false
})

const projectInfo = ref(null)

const basicRules = {
  name: [
    { required: true, message: '请输入项目名称', trigger: 'blur' },
    { min: 2, max: 50, message: '项目名称长度为2-50个字符', trigger: 'blur' }
  ],
  language: [
    { required: true, message: '请选择编程语言', trigger: 'change' }
  ]
}

const analysisRules = {
  excludeRules: [
    { max: 20, message: '排除规则最多20条', trigger: 'change' }
  ]
}

const saveConfig = async () => {
  try {
    // 只验证基本信息表单，分析配置是可选的
    await basicFormRef.value.validate()

    saving.value = true

    const configData = {
      ...basicForm,
      config: analysisForm
    }

    // 这里应该调用API保存配置
    console.log('保存配置:', configData)

    // 模拟API调用
    await new Promise(resolve => setTimeout(resolve, 1000))

    message.success('配置保存成功')
  } catch (error) {
    console.error('保存配置失败:', error)
  } finally {
    saving.value = false
  }
}

const resetConfig = () => {
  Object.assign(basicForm, {
    name: projectInfo.value?.name || '',
    description: projectInfo.value?.description || '',
    language: projectInfo.value?.language || undefined,
    status: projectInfo.value?.status || 'active'
  })

  Object.assign(analysisForm, {
    excludeRules: projectInfo.value?.config?.excludeRules || [],
    thresholds: projectInfo.value?.config?.thresholds || {
      complexity: 10,
      maintainability: 65,
      commentRate: 0.1
    },
    analysisTypes: projectInfo.value?.config?.analysisTypes || ['complexity', 'maintainability', 'style'],
    includeTests: projectInfo.value?.config?.includeTests || false
  })

  message.info('配置已重置')
}

const exportConfig = () => {
  const configData = {
    basic: basicForm,
    analysis: analysisForm
  }

  const blob = new Blob([JSON.stringify(configData, null, 2)], { type: 'application/json' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `${basicForm.name || 'project'}-config.json`
  a.click()
  URL.revokeObjectURL(url)

  message.success('配置导出成功')
}

const importConfig = () => {
  const input = document.createElement('input')
  input.type = 'file'
  input.accept = '.json'
  input.onchange = (e) => {
    const file = e.target.files[0]
    if (file) {
      const reader = new FileReader()
      reader.onload = (event) => {
        try {
          const configData = JSON.parse(event.target.result)

          if (configData.basic) {
            Object.assign(basicForm, configData.basic)
          }
          if (configData.analysis) {
            Object.assign(analysisForm, configData.analysis)
          }

          message.success('配置导入成功')
        } catch (error) {
          message.error('配置文件格式错误')
        }
      }
      reader.readAsText(file)
    }
  }
  input.click()
}

const goBack = () => {
  if (route.name === 'CreateProject') {
    router.push('/')
  } else {
    router.go(-1)
  }
}

const saveAndNext = async () => {
  try {
    // 只验证基本信息表单，分析配置是可选的
    await basicFormRef.value.validate()

    saving.value = true

    const projectData = {
      ...basicForm,
      config: analysisForm
    }

    // 这里应该调用API保存配置
    console.log('保存配置:', projectData)

    // 临时模拟项目创建（新项目）或更新（现有项目）
    if (route.params.id) {
      // 更新现有项目
      message.success('配置保存成功')
      router.push(`/project/${route.params.id}/upload`)
    } else {
      // 创建新项目
      const newProjectData = {
        id: Date.now().toString(),
        ...projectData,
        status: 'active',
        // createdAt: new Date().toISOString(),
        // updatedAt: new Date().toISOString(),
        fileCount: 0,
        lastAnalysis: null
      }
      console.log('project:', projectData)
      const newProject = await projectStore.createProject(newProjectData)
      // 添加到本地项目列表
      // const projectStore = useProjectStore()
      // projectStore.projects.push(newProject)

      message.success('项目创建成功')
      router.push(`/project/${newProject.id}/upload`)
    }
  } catch (error) {
    console.error('保存配置失败:', error)
    if (error.errorFields) {
      message.error('请检查表单填写是否正确')
    } else {
      message.error('保存配置失败，请重试')
    }
  } finally {
    saving.value = false
  }
}

const formatTime = (time) => {
  if (!time) return ''
  return new Date(time).toLocaleString()
}

onMounted(async () => {
  // 加载项目数据
  const projectId = route.params.id
  if (projectId) {
    try {
      const projectStore = useProjectStore()
      // 直接从API获取最新的项目数据
      const project = await projectStore.getProject(projectId)

      if (project) {
        projectInfo.value = project

        // 填充表单数据
        basicForm.name = project.name || ''
        basicForm.description = project.description || ''
        basicForm.language = project.language || undefined
        basicForm.status = project.status || 'active'

        // 填充分析配置
        if (project.config) {
          let configData = {}
          try {
            configData = typeof project.config === 'string' ? JSON.parse(project.config) : project.config
          } catch (error) {
            console.error('解析项目配置失败:', error)
            configData = {}
          }

          analysisForm.excludeRules = configData.excludeRules || []
          analysisForm.thresholds = configData.thresholds || {
            complexity: 10,
            maintainability: 65,
            commentRate: 0.1
          }
          analysisForm.analysisTypes = configData.analysisTypes || ['complexity', 'maintainability', 'style']
          analysisForm.includeTests = configData.includeTests || false
        }
      } else {
        // 如果项目不存在，显示错误信息
        message.error('项目不存在')
        router.push('/')
      }
    } catch (error) {
      console.error('加载项目失败:', error)
      message.error('加载项目失败')
      router.push('/')
    }
  } else {
    // 新建项目模式，保持表单为空
    projectInfo.value = null
  }
})
</script>

<style scoped>
.project-config-page {
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
  min-height: 500px;
}

.config-card {
  margin-bottom: 24px;
}

.config-card :deep(.ant-card-head) {
  border-bottom: 2px solid #f0f0f0;
}

.config-card :deep(.ant-card-head-title) {
  font-weight: 600;
  color: #262626;
}

.quick-actions {
  text-align: center;
}

.quick-actions .ant-btn {
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.project-info {
  space-y: 16px;
}

.info-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 0;
  border-bottom: 1px solid #f0f0f0;
}

.info-item:last-child {
  border-bottom: none;
}

.info-label {
  color: #8c8c8c;
  font-size: 14px;
}

.info-value {
  color: #262626;
  font-size: 14px;
  font-weight: 500;
}

.form-help-text {
  color: #8c8c8c;
  font-size: 12px;
  margin-top: 4px;
}

.switch-label {
  margin-left: 8px;
  color: #666;
}

.action-section {
  margin-top: 32px;
  padding-top: 24px;
  border-top: 1px solid #f0f0f0;
}

.action-buttons {
  display: flex;
  justify-content: center;
}

.action-buttons .ant-btn {
  min-width: 120px;
  height: 48px;
  font-size: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
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

  .config-card {
    margin-bottom: 16px;
  }
}
</style>