<template>
  <div class="home-page">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-content">
        <div class="header-left">
          <h1 class="page-title">
            <HomeOutlined />
            项目管理
          </h1>
          <p class="page-description">
            管理您的代码项目，进行质量分析和优化
          </p>
        </div>
        <div class="header-right">
          <a-space>
            <a-button @click="refreshProjects" :loading="projectStore.loading">
              <ReloadOutlined />
              刷新
            </a-button>
            <a-button
              v-if="filteredProjects.length > 0"
              type="primary"
              @click="showCreateModal"
            >
              <PlusOutlined />
              创建项目
            </a-button>
          </a-space>
        </div>
      </div>
    </div>

    <!-- 项目统计卡片 -->
    <div class="stats-section">
      <a-row :gutter="16">
        <a-col :span="6">
          <a-card class="stats-card">
            <a-statistic
              title="总项目数"
              :value="projectStore.projects.length"
              :prefix="h(FolderOutlined)"
            />
          </a-card>
        </a-col>
        <a-col :span="6">
          <a-card class="stats-card">
            <a-statistic
              title="活跃项目"
              :value="projectStore.activeProjects.length"
              :prefix="h(ExperimentOutlined)"
              value-style="{ color: '#1890ff' }"
            />
          </a-card>
        </a-col>
        <a-col :span="6">
          <a-card class="stats-card">
            <a-statistic
              title="已完成分析"
              :value="completedAnalysisCount"
              :prefix="h(CheckCircleOutlined)"
              value-style="{ color: '#52c41a' }"
            />
          </a-card>
        </a-col>
        <a-col :span="6">
          <a-card class="stats-card">
            <a-statistic
              title="平均质量评分"
              :value="averageQualityScore"
              :precision="1"
              suffix="分"
              :prefix="h(StarOutlined)"
              value-style="{ color: '#faad14' }"
            />
          </a-card>
        </a-col>
      </a-row>
    </div>

    <!-- 搜索和筛选 -->
    <div class="filter-section">
      <a-card>
        <a-row :gutter="16" align="middle">
          <a-col :span="8">
            <a-input-search
              v-model:value="searchText"
              placeholder="搜索项目名称或描述"
              @search="handleSearch"
              allowClear
            />
          </a-col>
          <a-col :span="6">
            <a-select
              v-model:value="statusFilter"
              placeholder="项目状态"
              style="width: 100%"
              allowClear
              @change="handleStatusFilter"
            >
              <a-select-option value="active">活跃</a-select-option>
              <a-select-option value="inactive">非活跃</a-select-option>
              <a-select-option value="archived">已归档</a-select-option>
            </a-select>
          </a-col>
          <a-col :span="6">
            <a-select
              v-model:value="languageFilter"
              placeholder="编程语言"
              style="width: 100%"
              allowClear
              @change="handleLanguageFilter"
            >
              <a-select-option value="javascript">JavaScript</a-select-option>
              <a-select-option value="typescript">TypeScript</a-select-option>
              <a-select-option value="java">Java</a-select-option>
              <a-select-option value="python">Python</a-select-option>
              <a-select-option value="cpp">C++</a-select-option>
            </a-select>
          </a-col>
          <a-col :span="4">
            <a-button @click="resetFilters">
              <ClearOutlined />
              重置
            </a-button>
          </a-col>
        </a-row>
      </a-card>
    </div>

    <!-- 项目列表 -->
    <div class="projects-section">
      <a-spin :spinning="projectStore.loading">
        <div v-if="filteredProjects.length === 0" class="empty-state">
          <a-empty description="暂无项目">
            <a-button type="primary" @click="showCreateModal">
              创建项目
            </a-button>
          </a-empty>
        </div>

        <a-row v-else :gutter="[16, 16]">
          <a-col
            v-for="project in filteredProjects"
            :key="project.id"
            :xs="24"
            :sm="12"
            :lg="8"
            :xl="6"
          >
            <ProjectCard
              :project="project"
              @edit="editProject"
              @delete="deleteProject"
              @analyze="analyzeProject"
              @upload="handleUpload"
              @report="handleReport"
              @view="handleView"
            />
          </a-col>
        </a-row>
      </a-spin>
    </div>

    <!-- 创建项目弹窗 -->
    <CreateProjectModal
      v-model:visible="createModalVisible"
      @success="handleProjectCreated"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, h } from 'vue'
import { useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import { useProjectStore } from '@/stores/project'
import {
  HomeOutlined,
  FolderOutlined,
  ExperimentOutlined,
  CheckCircleOutlined,
  StarOutlined,
  PlusOutlined,
  ReloadOutlined,
  ClearOutlined
} from '@ant-design/icons-vue'
import ProjectCard from '@/components/common/ProjectCard.vue'
import CreateProjectModal from '@/components/common/CreateProjectModal.vue'

const router = useRouter()
const projectStore = useProjectStore()

// 响应式数据
const searchText = ref('')
const statusFilter = ref(null)
const languageFilter = ref(null)
const createModalVisible = ref(false)

// 计算属性
const filteredProjects = computed(() => {
  let projects = projectStore.projects

  // 搜索过滤
  if (searchText.value) {
    const search = searchText.value.toLowerCase()
    projects = projects.filter(project =>
      project.name.toLowerCase().includes(search) ||
      (project.description && project.description.toLowerCase().includes(search))
    )
  }

  // 状态过滤
  if (statusFilter.value) {
    projects = projects.filter(project => project.status === statusFilter.value)
  }

  // 语言过滤
  if (languageFilter.value) {
    projects = projects.filter(project => project.language === languageFilter.value)
  }

  // 按创建时间倒序排列
  return projects.sort((a, b) => new Date(b.created_at || b.createdAt) - new Date(a.created_at || a.createdAt))
})

const completedAnalysisCount = computed(() => {
  return projectStore.projects.filter(project => project.lastAnalysis).length
})

const averageQualityScore = computed(() => {
  const projectsWithAnalysis = projectStore.projects.filter(project => project.lastAnalysis)
  if (projectsWithAnalysis.length === 0) return 0

  const totalScore = projectsWithAnalysis.reduce((sum, project) => {
    return sum + (project.lastAnalysis?.overallScore || 0)
  }, 0)

  return totalScore / projectsWithAnalysis.length
})

// 方法
const showCreateModal = () => {
  createModalVisible.value = true
}

const editProject = (project) => {
  router.push(`/project/${project.id}/config`)
}

const analyzeProject = (project) => {
  router.push(`/project/${project.id}/analysis`)
}

const handleUpload = (project) => {
  console.log('🟢 Home.handleUpload 被调用，项目ID:', project.id)
  console.log('🟢 即将跳转到:', `/project/${project.id}/upload`)
  router.push(`/project/${project.id}/upload`)
}

const handleReport = (project) => {
  console.log('🟢 Home.handleReport 被调用，项目ID:', project.id)
  console.log('🟢 即将跳转到:', `/project/${project.id}/report`)

  try {
    router.push(`/project/${project.id}/report`)
    console.log('🟢 Home.router.push 成功调用')
  } catch (error) {
    console.error('🟢 Home.router.push 失败:', error)
  }
}

const handleView = (project) => {
  console.log('🟢 Home.handleView 被调用，项目ID:', project.id)
  console.log('🟢 即将跳转到:', `/project/${project.id}/config`)
  router.push(`/project/${project.id}/config`)
}

const deleteProject = (project) => {
  Modal.confirm({
    title: '确认删除项目',
    content: `确定要删除项目"${project.name}"吗？此操作不可撤销。`,
    okText: '删除',
    okType: 'danger',
    cancelText: '取消',
    async onOk() {
      try {
        await projectStore.deleteProject(project.id)
        message.success('项目删除成功')
      } catch (error) {
        console.error('删除项目失败:', error)
      }
    }
  })
}

const refreshProjects = async () => {
  try {
    await projectStore.fetchProjects()
    message.success('项目列表已刷新')
  } catch (error) {
    console.error('刷新项目列表失败:', error)
  }
}

const handleSearch = () => {
  // 搜索逻辑在计算属性中处理
}

const handleStatusFilter = () => {
  // 过滤逻辑在计算属性中处理
}

const handleLanguageFilter = () => {
  // 过滤逻辑在计算属性中处理
}

const resetFilters = () => {
  searchText.value = ''
  statusFilter.value = null
  languageFilter.value = null
}

const handleProjectCreated = (project) => {
  // 项目创建成功后的处理
  router.push(`/project/${project.id}/upload`)
}

// 调试测试函数

// 生命周期
onMounted(async () => {
  try {
    await projectStore.fetchProjects()
  } catch (error) {
    console.error('获取项目列表失败:', error)
  }
})
</script>

<style scoped>
.home-page {
  max-width: 1200px;
  margin: 0 auto;
}

.page-header {
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 1px solid #f0f0f0;
}

.header-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.header-left {
  flex: 1;
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

.header-right {
  flex: none;
}

.stats-section {
  margin-bottom: 24px;
}

.stats-card {
  text-align: center;
}

.stats-card :deep(.ant-card-body) {
  padding: 20px;
}

.filter-section {
  margin-bottom: 24px;
}

.projects-section {
  min-height: 400px;
}

.empty-state {
  text-align: center;
  padding: 60px 0;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .header-content {
    flex-direction: column;
    align-items: flex-start;
    gap: 16px;
  }

  .header-right {
    align-self: stretch;
  }

  .stats-section .ant-row {
    justify-content: center;
  }

  .stats-section .ant-col {
    margin-bottom: 16px;
  }
}
</style>