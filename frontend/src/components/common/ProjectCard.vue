<template>
  <a-card class="project-card" :hoverable="true">
    <template #cover>
      <div class="project-cover">
        <div class="project-icon">
          <component :is="getLanguageIcon(project.language)" />
        </div>
      </div>
    </template>

    <template #actions>
      <a-tooltip title="编辑项目">
        <EditOutlined @click.stop="handleEdit" />
      </a-tooltip>
      <a-tooltip title="开始分析">
        <ExperimentOutlined @click.stop="handleAnalyze" />
      </a-tooltip>
      <a-dropdown>
        <MoreOutlined />
        <template #overlay>
          <a-menu>
            <a-menu-item key="view" @click="handleView">
              <EyeOutlined />
              查看详情
            </a-menu-item>
            <a-menu-item key="upload" @click="handleUpload">
              <UploadOutlined />
              上传代码
            </a-menu-item>
            <a-menu-item key="report" @click="handleReport">
              <BarChartOutlined />
              查看报告
            </a-menu-item>
            <a-menu-divider />
            <a-menu-item key="delete" danger @click.stop="handleDelete">
              <DeleteOutlined />
              删除项目
            </a-menu-item>
          </a-menu>
        </template>
      </a-dropdown>
    </template>

    <a-card-meta>
      <template #title>
        <div class="project-title">
          {{ project.name }}
          <a-tag
            :color="getStatusColor(project.status)"
            size="small"
            class="status-tag"
          >
            {{ getStatusText(project.status) }}
          </a-tag>
        </div>
      </template>
      <template #description>
        <div class="project-description">
          {{ project.description || '暂无描述' }}
        </div>
        <div class="project-info">
          <a-space size="small" split="/">
            <span class="info-item">
              <FileOutlined />
              {{ project.fileCount || project.file_count || 0 }}个文件
            </span>
            <span class="info-item">
              <CodeOutlined />
              {{ getLanguageText(project.language) }}
            </span>
          </a-space>
        </div>
        <div v-if="project.lastAnalysis" class="analysis-info">
          <a-space direction="vertical" size="small" style="width: 100%">
            <div class="score-display">
              <span class="score-label">质量评分:</span>
              <a-progress
                :percent="project.lastAnalysis.overallScore"
                :status="getScoreStatus(project.lastAnalysis.overallScore)"
                size="small"
              />
              <span class="score-value">{{ project.lastAnalysis.overallScore }}分</span>
            </div>
            <div class="last-analysis">
              <span class="time-label">最后分析:</span>
              <span class="time-value">{{ formatTime(project.lastAnalysis.completedAt) }}</span>
            </div>
          </a-space>
        </div>
        <div v-else class="no-analysis">
          <a-empty description="暂无分析数据" :image="false" size="small">
            <a-button size="small" type="primary" @click.stop="handleAnalyze">
              开始分析
            </a-button>
          </a-empty>
        </div>
      </template>
    </a-card-meta>
  </a-card>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import {
  EditOutlined,
  ExperimentOutlined,
  MoreOutlined,
  EyeOutlined,
  UploadOutlined,
  BarChartOutlined,
  DeleteOutlined,
  FileOutlined,
  CodeOutlined
} from '@ant-design/icons-vue'

// 语言图标映射
const languageIcons = {
  javascript: 'CodeOutlined',
  typescript: 'CodeOutlined',
  java: 'FileOutlined',
  python: 'FileOutlined',
  cpp: 'FileOutlined',
  c: 'FileOutlined',
  cs: 'FileOutlined',
  php: 'FileOutlined',
  rb: 'FileOutlined',
  go: 'FileOutlined',
  rs: 'FileOutlined'
}

const props = defineProps({
  project: {
    type: Object,
    required: true
  }
})

const emit = defineEmits(['edit', 'delete', 'analyze', 'upload', 'report', 'view'])

const router = useRouter()

const getLanguageIcon = (language) => {
  return languageIcons[language] || 'FileOutlined'
}

const getLanguageText = (language) => {
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
    rs: 'Rust'
  }
  return languageMap[language] || language.toUpperCase()
}

const getStatusColor = (status) => {
  const colorMap = {
    active: 'green',
    inactive: 'orange',
    archived: 'gray',
    completed: 'blue'
  }
  return colorMap[status] || 'default'
}

const getStatusText = (status) => {
  const textMap = {
    active: '活跃',
    inactive: '非活跃',
    archived: '已归档',
    completed: '已完成',
  }
  return textMap[status] || '未知'
}

const getScoreStatus = (score) => {
  if (score >= 80) return 'success'
  if (score >= 60) return 'normal'
  return 'exception'
}

const formatTime = (time) => {
  if (!time) return ''

  const date = new Date(time)

  // 检查日期是否有效
  if (isNaN(date.getTime())) {
    console.warn('无效的时间格式:', time)
    return '未知时间'
  }

  // 格式化为精确的年月日时分
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')

  return `${year}-${month}-${day} ${hours}:${minutes}`
}


const handleEdit = () => {
  emit('edit', props.project)
}

const handleAnalyze = () => {
  emit('analyze', props.project)
}

const handleDelete = () => {
  emit('delete', props.project)
}

const handleUpload = () => {
  // 直接路由跳转作为备用方案
  try {
    router.push(`/project/${props.project.id}/upload`)
  } catch (error) {
    console.error('直接路由跳转失败:', error)
  }

  // 同时也触发事件给父组件
  emit('upload', props.project)
}

const handleReport = () => {
  // 同时也触发事件给父组件
  emit('report', props.project)

  // 直接路由跳转作为备用方案
  try {
    router.push(`/project/${props.project.id}/report`)
  } catch (error) {
    console.error('直接路由跳转失败:', error)
  }
}

const handleView = () => {
  // 直接路由跳转作为备用方案
  try {
    router.push(`/project/${props.project.id}/config`)
  } catch (error) {
    console.error('直接路由跳转失败:', error)
  }

  // 同时也触发事件给父组件
  emit('view', props.project)
}
</script>

<style scoped>
.project-card {
  cursor: pointer;
  transition: all 0.3s ease;
  height: 100%;
  min-height: 320px;
  display: flex;
  flex-direction: column;
}

.project-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.12);
}

.project-card :deep(.ant-card-body) {
  flex: 1;
  display: flex;
  flex-direction: column;
  padding: 16px;
}

.project-cover {
  height: 120px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
}

.project-icon {
  font-size: 48px;
  color: white;
  opacity: 0.9;
}

.project-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.status-tag {
  flex-shrink: 0;
}

.project-description {
  color: #666;
  font-size: 14px;
  line-height: 1.5;
  margin-bottom: 12px;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  flex: 0 0 auto;
}

.project-info {
  margin-bottom: 12px;
  flex: 0 0 auto;
}

.info-item {
  display: flex;
  align-items: center;
  gap: 4px;
  color: #8c8c8c;
  font-size: 12px;
}

.analysis-info {
  padding-top: 12px;
  border-top: 1px solid #f0f0f0;
  margin-top: auto;
  flex: 0 0 auto;
}

.score-display {
  display: flex;
  align-items: center;
  gap: 8px;
}

.score-label {
  color: #8c8c8c;
  font-size: 12px;
  min-width: 60px;
}

.score-value {
  color: #262626;
  font-weight: 600;
  font-size: 12px;
}

.last-analysis {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.time-label {
  color: #8c8c8c;
  font-size: 12px;
}

.time-value {
  color: #262626;
  font-size: 12px;
}

.no-analysis {
  padding: 12px 0;
  text-align: center;
  margin-top: auto;
  flex: 0 0 auto;
}

:deep(.ant-card-actions) {
  border-top: 1px solid #f0f0f0;
}

:deep(.ant-card-actions li) {
  margin: 0;
}

:deep(.ant-card-actions li > span) {
  padding: 12px 0;
  color: #8c8c8c;
}

:deep(.ant-card-actions li > span:hover) {
  color: #1890ff;
}

:deep(.ant-progress-text) {
  font-size: 10px !important;
}
</style>