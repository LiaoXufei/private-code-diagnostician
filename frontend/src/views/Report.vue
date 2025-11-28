<template>
  <div class="report-page">
    <div class="page-header">
      <h1 class="page-title">
        <BarChartOutlined />
        分析报告
      </h1>
      <p class="page-description">
        查看详细的代码质量分析结果和改进建议
      </p>
    </div>

    <div class="content-container">
      <!-- 总览卡片 -->
      <a-row :gutter="24" class="overview-section">
        <a-col :span="6">
          <ScoreCard
            :score="reportData.overallScore"
            :grade="reportData.grade"
            :loading="loading"
          />
        </a-col>
        <a-col :span="18">
          <a-card title="七维度雷达图" class="radar-card">
            <div ref="radarChartRef" class="radar-chart-container"></div>
          </a-card>
        </a-col>
      </a-row>

      <a-row :gutter="24">
        <!-- 左侧：指标详情 -->
        <a-col :span="16">
          <!-- 详细指标 -->
          <a-card title="详细指标" class="metrics-card">
            <div class="metrics-grid">
              <div
                v-for="(dimension, index) in reportData.dimensions"
                :key="dimension.name"
                class="metric-item"
              >
                <div class="metric-header">
                  <span class="metric-name">{{ dimension.name }}</span>
                  <a-tag
                    :color="getScoreColor(dimension.score)"
                    class="metric-score"
                  >
                    {{ dimension.score }}分
                  </a-tag>
                </div>
                <div class="metric-content">
                  <a-progress
                    :percent="dimension.score"
                    :stroke-color="getScoreColor(dimension.score)"
                    :show-info="true"
                    size="small"
                  />
                  <div class="metric-details">
                    <div class="detail-item">
                      <span class="detail-label">满分:</span>
                      <span class="detail-value">{{ dimension.maxScore }}</span>
                    </div>
                    <div class="detail-item">
                      <span class="detail-label">问题数:</span>
                      <span class="detail-value">{{ dimension.issues }}</span>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </a-card>

          <!-- 问题列表 -->
          <a-card title="发现问题" class="issues-card">
            <div class="issue-filters">
              <a-space>
                <a-select
                  v-model:value="selectedDimension"
                  placeholder="选择维度"
                  style="width: 150px"
                  allowClear
                  @change="handleDimensionChange"
                >
                  <a-select-option
                    v-for="dim in reportData.dimensions"
                    :key="dim.name"
                    :value="dim.name"
                  >
                    {{ dim.name }}
                  </a-select-option>
                </a-select>
                <a-select
                  v-model:value="selectedSeverity"
                  placeholder="严重程度"
                  style="width: 120px"
                  allowClear
                  @change="handleSeverityChange"
                >
                  <a-select-option value="high">高</a-select-option>
                  <a-select-option value="medium">中</a-select-option>
                  <a-select-option value="low">低</a-select-option>
                </a-select>
                <a-input
                  v-model:value="searchKeyword"
                  placeholder="搜索问题"
                  style="width: 200px"
                  allowClear
                />
              </a-space>
            </div>

            <div class="issues-list">
              <div v-if="filteredIssues.length === 0" class="empty-issues">
                <a-empty description="暂无相关问题" />
              </div>
              <div
                v-for="issue in paginatedIssues"
                :key="issue.id"
                class="issue-item"
                @click="handleIssueClick(issue)"
              >
                <div class="issue-header">
                  <a-tag
                    :color="getSeverityColor(issue.severity)"
                    class="issue-severity"
                  >
                    {{ getSeverityText(issue.severity) }}
                  </a-tag>
                  <span class="issue-title">{{ issue.message }}</span>
                </div>
                <div class="issue-details">
                  <div class="issue-file">
                    <FileOutlined />
                    {{ issue.file }}
                    <span class="issue-location">:{{ issue.line }}</span>
                  </div>
                  <div class="issue-suggestion">
                    <BulbOutlined />
                    {{ issue.suggestion }}
                  </div>
                </div>
              </div>

              <!-- 分页 -->
              <div v-if="filteredIssues.length > pageSize" class="pagination">
                <a-pagination
                  v-model:current="currentPage"
                  :page-size="pageSize"
                  :total="filteredIssues.length"
                  :show-size-changer="false"
                  :show-quick-jumper="true"
                  @change="handlePageChange"
                />
              </div>
            </div>
          </a-card>
        </a-col>

        <!-- 右侧：文件级报告 -->
        <a-col :span="8">
          <a-card title="文件质量排行" class="files-card">
            <div class="files-list">
              <div v-if="topFiles.length === 0" class="empty-files">
                <a-empty description="暂无文件数据" />
              </div>
              <div
                v-for="file in topFiles"
                :key="file.file"
                class="file-item"
                @click="handleFileClick(file)"
              >
                <div class="file-header">
                  <span class="file-name">{{ getFileName(file.file) }}</span>
                  <a-tag
                    :color="getScoreColor(file.score)"
                    size="small"
                  >
                    {{ file.score }}分
                  </a-tag>
                </div>
                <div class="file-metrics">
                  <div class="file-metric">
                    <span class="metric-label">问题:</span>
                    <span class="metric-value">{{ file.issues }}</span>
                  </div>
                  <div class="file-metric">
                    <span class="metric-label">行数:</span>
                    <span class="metric-value">{{ file.linesOfCode }}</span>
                  </div>
                  <div class="file-metric">
                    <span class="metric-label">复杂度:</span>
                    <span class="metric-value">{{ file.complexity }}</span>
                  </div>
                </div>
              </div>
            </div>
          </a-card>

          <!-- 快速操作 -->
          <a-card title="快速操作" class="actions-card">
            <div class="action-buttons">
              <a-space direction="vertical" style="width: 100%">
                <a-button type="primary" block @click="goToOptimize">
                  <BulbOutlined />
                  查看优化建议
                </a-button>
                <a-button block @click="exportReport('pdf')">
                  <FileTextOutlined />
                  导出PDF报告
                </a-button>
    <a-button block @click="exportReport('md')">
      <FileTextOutlined />
      导出Markdown文件
    </a-button>
                <a-button block @click="reanalyze">
                  <RedoOutlined />
                  重新分析
                </a-button>
              </a-space>
            </div>
          </a-card>
        </a-col>
      </a-row>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { useProjectStore } from '@/stores/project'
import { projectApi } from '@/api/project'
import * as echarts from 'echarts'
import {
  BarChartOutlined,
  FileOutlined,
  FileTextOutlined,
  TableOutlined,
  RedoOutlined,
  BulbOutlined
} from '@ant-design/icons-vue'
import ScoreCard from '@/components/common/ScoreCard.vue'

const route = useRoute()
const router = useRouter()
const projectStore = useProjectStore()

// 响应式数据
const loading = ref(false)
const selectedDimension = ref(undefined)
const selectedSeverity = ref(undefined)
const searchKeyword = ref('')
const currentPage = ref(1)
const pageSize = ref(10)
const radarChartRef = ref(null)
const radarChart = ref(null)

const dimensionTypeMap = {
  // 后端维度名称
  '圈复杂度分析': ['cyclomatic_complexity'],
  '命名规范': ['naming_convention'],
  '错误处理分析': ['error_handling'],
  '代码重复度': ['code_duplication'],
  '状态管理': ['state_management', 'parameter_count', 'function_length'],
  '结构分析': ['nesting_depth', 'circular_dependency', 'import_complexity'],
  '注释覆盖率': ['comment_coverage'],
  // 前端演示维度名称（兼容）
  //'代码复杂度': ['complexity', 'cyclomatic_complexity', 'nesting_depth'],
  '可维护性': ['naming_convention', 'state_management', 'parameter_count', 'function_length', 'comment_coverage'],
//  '重复度': ['code_duplication'],
//  '代码风格': ['naming_convention'],
//  '测试覆盖': ['comment_coverage'],
//  '安全性': ['error_handling']
}

// 获取当前项目数据
const currentProject = computed(() => {
  const projectId = route.params.id
  console.log('Report.vue - 当前路由参数:', { projectId, routeParams: route.params })
  return projectStore.projectById(projectId)
})

// 后端报告数据
const backendReport = ref(null)

// 报告数据
const reportData = computed(() => {
  if (backendReport.value) {
    return backendReport.value
  }
  const project = currentProject.value
  console.log('Report.vue - 当前项目:', project)

  if (!project?.lastAnalysis) {
    return {
      overallScore: 0,
      grade: 'none',
      dimensions: [],
      issues: [],
      fileResults: []
    }
  }

  const baseScore = project.lastAnalysis.overallScore || 0
  console.log('Report.vue - 基础分数:', baseScore)

  const result = {
    overallScore: baseScore,
    grade: baseScore >= 80 ? 'excellent' : baseScore >= 60 ? 'good' : 'poor',
    dimensions: [
      { name: '代码复杂度', score: Math.min(baseScore + 5, 100), maxScore: 100, issues: Math.max(0, 10 - Math.floor(baseScore / 10)) },
      { name: '可维护性', score: Math.max(baseScore - 10, 0), maxScore: 100, issues: Math.max(0, 8 - Math.floor(baseScore / 12)) },
      { name: '重复度', score: Math.min(baseScore + 10, 100), maxScore: 100, issues: Math.max(0, 5 - Math.floor(baseScore / 15)) },
      { name: '安全性', score: Math.min(baseScore + 15, 100), maxScore: 100, issues: Math.max(0, 3 - Math.floor(baseScore / 25)) },
      { name: '性能', score: Math.max(baseScore - 15, 0), maxScore: 100, issues: Math.max(0, 15 - Math.floor(baseScore / 8)) },
      { name: '代码风格', score: baseScore, maxScore: 100, issues: Math.max(0, 6 - Math.floor(baseScore / 13)) },
      { name: '测试覆盖', score: Math.max(baseScore - 20, 0), maxScore: 100, issues: Math.max(0, 12 - Math.floor(baseScore / 6)) }
    ],
    issues: [
      {
        id: 'issue_1',
        type: 'complexity',
        severity: baseScore < 70 ? 'high' : 'medium',
        file: '/src/components/UserForm.vue',
        line: 45,
        message: '函数复杂度过高',
        suggestion: '建议拆分为多个小函数'
      },
      {
        id: 'issue_2',
        type: 'naming_convention',
        severity: 'medium',
        file: '/src/utils/helper.js',
        line: 23,
        message: '变量命名不规范',
        suggestion: '使用更具描述性的变量名'
      }
    ].filter((_, index) => index < (baseScore < 60 ? 4 : baseScore < 80 ? 3 : 2)),
    fileResults: [
      {
        file: '/src/components/UserForm.vue',
        score: Math.min(baseScore + 7, 100),
        issues: Math.max(0, 5 - Math.floor(baseScore / 15)),
        linesOfCode: 120,
        complexity: 8.5
      },
      {
        file: '/src/utils/helper.js',
        score: Math.max(baseScore - 10, 0),
        issues: Math.max(0, 8 - Math.floor(baseScore / 10)),
        linesOfCode: 89,
        complexity: 12.3
      }
    ]
  }

  console.log('Report.vue - 生成的报告数据:', result)
  return result
})

// 过滤后的问题
const filteredIssues = computed(() => {
  let issues = reportData.value.issues

  if (selectedDimension.value) {
    const key = String(selectedDimension.value).trim()
    const typeKeys = dimensionTypeMap[key]
    if (Array.isArray(typeKeys) && typeKeys.length > 0) {
      issues = issues.filter(issue => typeKeys.includes(issue.type))
    }
  }

  if (selectedSeverity.value) {
    issues = issues.filter(issue => issue.severity === selectedSeverity.value)
  }

  if (searchKeyword.value) {
    const keyword = searchKeyword.value.toLowerCase()
    issues = issues.filter(issue =>
      issue.message.toLowerCase().includes(keyword) ||
      issue.file.toLowerCase().includes(keyword)
    )
  }

  return issues
})

// 分页问题
const paginatedIssues = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  const end = start + pageSize.value
  return filteredIssues.value.slice(start, end)
})

// 文件排行
const topFiles = computed(() => {
  return reportData.value.fileResults.slice(0, 10)
})

// 方法
const getScoreColor = (score) => {
  if (score >= 80) return 'green'
  if (score >= 60) return 'orange'
  return 'red'
}

const getSeverityColor = (severity) => {
  const colorMap = {
    high: 'red',
    medium: 'orange',
    low: 'blue'
  }
  return colorMap[severity] || 'default'
}

const getSeverityText = (severity) => {
  const textMap = {
    high: '高',
    medium: '中',
    low: '低'
  }
  return textMap[severity] || '未知'
}

const getFileName = (filePath) => {
  return filePath.split('/').pop()
}

const handleDimensionChange = () => {
  currentPage.value = 1
}

const handleSeverityChange = () => {
  currentPage.value = 1
}

const handlePageChange = (page) => {
  currentPage.value = page
}

const handleIssueClick = (issue) => {
  console.log('点击问题:', issue)
  message.info(`点击了问题: ${issue.message}`)
}

const handleFileClick = (file) => {
  console.log('点击文件:', file)
  message.info(`点击了文件: ${getFileName(file.file)}`)
}

const goToOptimize = () => {
  console.log('🟢 Report.goToOptimize 被调用')
  const projectId = route.params.id
  console.log('🟢 项目ID:', projectId)

  if (!projectId) {
    message.error('项目ID不能为空')
    return
  }

  console.log('🟢 即将跳转到:', `/project/${projectId}/optimize`)

  try {
    router.push(`/project/${projectId}/optimize`)
    console.log('🟢 导航成功')
    message.success('正在跳转到优化建议页面...')
  } catch (error) {
    console.error('🟢 导航失败:', error)
    message.error('跳转失败，请重试')
  }
}

const exportReport = (format) => {
  const projectId = route.params.id
  if (!projectId) {
    message.error('项目ID不能为空')
    return
  }
  if (format === 'pdf') {
    const url = `/api/projects/${projectId}/report.pdf`
    const a = document.createElement('a')
    a.href = url
    a.target = '_blank'
    a.rel = 'noopener'
    a.download = `report-${projectId}.pdf`
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    message.success('开始下载PDF报告')
    return
  }
  if (format === 'md') {
    const url = `/api/projects/${projectId}/report.md`
    const a = document.createElement('a')
    a.href = url
    a.target = '_blank'
    a.rel = 'noopener'
    a.download = `report-${projectId}.md`
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    message.success('开始下载Markdown报告')
    return
  }
  message.info('暂未支持的导出格式')
}

const reanalyze = () => {
  console.log('🟢 Report.reanalyze 被调用')
  const projectId = route.params.id
  console.log('🟢 项目ID:', projectId)

  if (!projectId) {
    message.error('项目ID不能为空')
    return
  }

  console.log('🟢 即将跳转到:', `/project/${projectId}/analysis`)

  try {
    router.push(`/project/${projectId}/analysis`)
    console.log('🟢 导航成功')
    message.success('正在跳转到分析页面...')
  } catch (error) {
    console.error('🟢 导航失败:', error)
    message.error('跳转失败，请重试')
  }
}

// 雷达图相关方法
const initRadarChart = () => {
  if (!radarChartRef.value) return

  // 销毁已存在的图表实例
  if (radarChart.value) {
    radarChart.value.dispose()
  }

  // 创建新的图表实例
  radarChart.value = echarts.init(radarChartRef.value)

  // 监听窗口大小变化，调整图表大小
  window.addEventListener('resize', () => {
    radarChart.value?.resize()
  })
}

const updateRadarChart = () => {
  if (!radarChart.value || !reportData.value.dimensions.length) return

  const dimensions = reportData.value.dimensions

  const option = {
    title: {
      text: '代码质量七维度分析',
      left: 'center',
      top: 10,
      textStyle: {
        fontSize: 16,
        fontWeight: 'normal',
        color: '#262626'
      }
    },
    tooltip: {
      trigger: 'item',
      formatter: (params) => {
        const dimension = dimensions[params.dataIndex]
        return `${dimension.name}<br/>分数: ${dimension.score}分<br/>满分: ${dimension.maxScore}分<br/>问题数: ${dimension.issues}个`
      }
    },
    legend: {
      data: ['当前分数', '满分'],
      bottom: 10,
      itemGap: 20
    },
    radar: {
      center: ['50%', '55%'],
      radius: '65%',
      indicator: dimensions.map(dim => ({
        name: dim.name,
        max: 100,
        axisLabel: {
          show: true,
          fontSize: 12
        }
      })),
      axisName: {
        fontSize: 13,
        color: '#666',
        fontWeight: 'bold'
      },
      splitNumber: 5,
      splitLine: {
        lineStyle: {
          color: '#e0e0e0'
        }
      },
      splitArea: {
        areaStyle: {
          color: ['#f8f8f8', '#ffffff']
        }
      },
      axisLine: {
        lineStyle: {
          color: '#d0d0d0'
        }
      }
    },
    series: [
      {
        name: '代码质量评分',
        type: 'radar',
        data: [
          {
            value: dimensions.map(dim => dim.score),
            name: '当前分数',
            itemStyle: {
              color: '#1890ff'
            },
            areaStyle: {
              color: 'rgba(24, 144, 255, 0.2)'
            },
            lineStyle: {
              width: 2,
              color: '#1890ff'
            },
            symbol: 'circle',
            symbolSize: 6
          },
          {
            value: dimensions.map(() => 100),
            name: '满分',
            itemStyle: {
              color: '#52c41a'
            },
            areaStyle: {
              color: 'rgba(82, 196, 26, 0.1)'
            },
            lineStyle: {
              width: 1,
              color: '#52c41a',
              type: 'dashed'
            },
            symbol: 'diamond',
            symbolSize: 4
          }
        ]
      }
    ]
  }

  radarChart.value.setOption(option)
}

const loadReportData = async () => {
  loading.value = true
  try {
    const projectId = route.params.id

    if (!projectId) {
      throw new Error('项目ID不能为空')
    }

    console.log('Report.vue - 加载报告数据，项目ID:', projectId)

    // 从后端获取最新分析结果
    const { data } = await projectApi.getAnalysisResult(projectId)
    backendReport.value = {
      overallScore: data.overallScore || 0,
      grade: data.grade || 'none',
      dimensions: (data.dimensions || []).map(d => ({
        name: d.name,
        score: d.score,
        maxScore: d.max_score || d.maxScore || 100,
        issues: d.issues
      })),
      issues: data.issues || [],
      fileResults: (data.fileResults || []).map(f => ({
        file: f.file,
        score: f.score,
        issues: f.issues,
        linesOfCode: f.linesOfCode || 0,
        complexity: f.complexity || 0
      }))
    }

    // 等待DOM更新后初始化雷达图
    await nextTick()
    initRadarChart()
    updateRadarChart()

    message.success('报告数据加载完成')
  } catch (error) {
    console.error('加载报告失败:', error)
    message.error('加载报告失败')
  } finally {
    loading.value = false
  }
}

// 监听报告数据变化，更新雷达图
watch(reportData, () => {
  if (radarChart.value && reportData.value.dimensions.length > 0) {
    nextTick(() => {
      updateRadarChart()
    })
  }
}, { deep: true })

onMounted(() => {
  console.log('Report.vue - 组件已挂载')
  loadReportData()
})

onUnmounted(() => {
  // 清理图表实例
  if (radarChart.value) {
    radarChart.value.dispose()
    radarChart.value = null
  }

  // 移除事件监听器
  window.removeEventListener('resize', () => {
    radarChart.value?.resize()
  })
})
</script>

<style scoped>
.report-page {
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px;
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

.overview-section {
  margin-bottom: 24px;
}

.radar-card {
  height: 350px;
}

.radar-chart-container {
  width: 100%;
  height: 300px;
  min-height: 300px;
}

.metrics-card,
.issues-card,
.files-card,
.actions-card {
  margin-bottom: 24px;
}

.metrics-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 20px;
}

.metric-item {
  border: 1px solid #f0f0f0;
  border-radius: 8px;
  padding: 16px;
  transition: all 0.3s ease;
  cursor: pointer;
}

.metric-item:hover {
  border-color: #1890ff;
  box-shadow: 0 2px 8px rgba(24, 144, 255, 0.1);
}

.metric-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.metric-name {
  font-weight: 600;
  color: #262626;
  font-size: 14px;
}

.metric-content {
  space-y: 8px;
}

.metric-details {
  display: flex;
  justify-content: space-between;
  gap: 16px;
}

.detail-item {
  display: flex;
  align-items: center;
  gap: 4px;
}

.detail-label {
  color: #8c8c8c;
  font-size: 12px;
}

.detail-value {
  color: #262626;
  font-weight: 500;
  font-size: 12px;
}

.issue-filters {
  margin-bottom: 16px;
  padding-bottom: 16px;
  border-bottom: 1px solid #f0f0f0;
}

.issues-list {
  max-height: 400px;
  overflow-y: auto;
}

.issue-item {
  padding: 12px;
  border: 1px solid #f0f0f0;
  border-radius: 6px;
  margin-bottom: 12px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.issue-item:hover {
  border-color: #1890ff;
  background: #f6ffed;
}

.issue-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.issue-title {
  flex: 1;
  color: #262626;
  font-weight: 500;
}

.issue-details {
  font-size: 12px;
  color: #666;
  line-height: 1.5;
}

.issue-file {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-bottom: 4px;
}

.issue-location {
  color: #1890ff;
  font-weight: 500;
}

.issue-suggestion {
  display: flex;
  align-items: flex-start;
  gap: 4px;
  color: #faad14;
}

.pagination {
  margin-top: 16px;
  text-align: center;
}

.files-list {
  max-height: 300px;
  overflow-y: auto;
}

.file-item {
  padding: 12px;
  border: 1px solid #f0f0f0;
  border-radius: 6px;
  margin-bottom: 12px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.file-item:hover {
  border-color: #1890ff;
  background: #f6ffed;
}

.file-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.file-name {
  font-weight: 500;
  color: #262626;
  font-size: 13px;
}

.file-metrics {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
}

.file-metric {
  display: flex;
  justify-content: space-between;
  font-size: 11px;
  color: #666;
}

.metric-label {
  color: #8c8c8c;
}

.metric-value {
  font-weight: 500;
}

.action-buttons {
  text-align: center;
}

.empty-issues,
.empty-files {
  text-align: center;
  padding: 40px 20px;
  color: #8c8c8c;
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

  .overview-section .ant-col {
    margin-bottom: 16px;
  }

  .metrics-grid {
    grid-template-columns: 1fr;
  }

  .issue-filters .ant-space {
    flex-wrap: wrap;
  }

  .issue-filters .ant-space-item {
    margin-bottom: 8px;
  }
}
</style>
