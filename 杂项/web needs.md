```markdown
# 前端开发规划报告（优化版）

## 一、项目概述

### 1.1 项目背景

本项目是一个基于 Java 的代码质量分析与 AI 优化工具，旨在帮助开发者快速检测代码质量问题，并通过 AI 提供优化建议。系统采用前后端分离架构，前端负责用户交互、数据展示和报告可视化。**暂不支持用户登录系统和团队协作，专注单用户多项目管理。**

### 1.2 项目目标

* 提供友好的 Web 界面供用户上传和管理多个代码项目
* 可视化展示七维度代码质量分析结果
* 展示 AI 优化建议和代码对比
* 支持多格式报告导出
* 实现实时分析进度追踪

### 1.3 技术栈（推荐配置）

* **前端框架**: Vue.js 3.x + Composition API
* **UI 组件库**: Ant Design Vue (企业级组件丰富)
* **代码编辑器**: Monaco Editor (VS Code同款，功能强大)
* **差异对比**: diff-match-patch (专业差异对比)
* **图表可视化**: Apache ECharts (功能完善)
* **HTTP 客户端**: Axios (统一请求封装)
* **状态管理**: Pinia (Vue 3官方推荐)
* **路由**: Vue Router 4.x
* **构建工具**: Vite (快速开发体验)
* **实时通信**: WebSocket (分析进度实时推送)
* **虚拟滚动**: vue-virtual-scroller (大文件性能优化)
* **文件上传**: vue-upload-component (专业上传组件)

---

## 二、核心功能模块

### 2.1 项目管理模块

**功能描述**:

* 创建新项目并设置基本信息
* 配置项目分析参数(语言类型、排除规则等)
* 查看项目列表和历史记录

**关键功能点**:

* 项目信息表单(项目名称、描述、语言类型)
* 排除规则配置(文件/文件夹黑名单)
* 分析参数设置(复杂度阈值、注释率要求等)

### 2.2 代码上传模块

**功能描述**:

* 支持多种上传方式(本地文件、ZIP包、Git仓库)
* 文件预览和项目结构展示
* 文件树形结构浏览

**关键功能点**:

* 文件拖拽上传
* ZIP 包上传解压预览
* Git 仓库 URL 输入
* 代码文件树形展示
* 代码预览(支持语法高亮)

### 2.3 代码分析模块

**功能描述**:

* 触发后端分析任务
* 显示分析进度
* 异步任务状态管理

**关键功能点**:

* 一键启动分析按钮
* 实时进度条显示
* WebSocket 或轮询获取任务状态

### 2.4 报告展示模块

**功能描述**:

* 总体评分展示(0-100分制 + 质量等级)
* 七维度详细指标报告
* 问题统计和改进建议
* 文件级问题定位

**关键功能点**:

* **总览面板**: 整体评分、雷达图、质量等级标签
* **指标详情**: 七维度得分表格、柱状图/饼图
* **问题列表**: 分类展示各维度问题、严重程度标记
* **文件排行**: 最差文件 TOP N、点击跳转到具体问题
* **改进建议**: AI 生成的优化建议列表

### 2.5 AI 优化模块

**功能描述**:

* 查看 AI 优化建议
* 代码对比(原始 vs 优化后)
* 下载优化代码

**关键功能点**:

* 优化建议卡片展示
* 代码差异对比视图(左右分屏或统一视图)
* 语法高亮和差异标记
* 下载优化后的代码文件

### 2.6 报告导出模块

**功能描述**:

* 多格式导出(PDF、Markdown、Excel)
* 一键下载完整报告

**关键功能点**:

* 导出按钮(支持格式选择)
* 调用后端 API 生成报告文件
* 文件下载处理

---

## 三、页面规划

根据功能模块，前端需要开发以下 **7 个核心页面**:

### 3.1 首页 / 项目列表页 (`/`)

**用途**: 展示所有项目、创建新项目入口
**主要内容**:

* 项目卡片列表(项目名称、创建时间、最新分析状态)
* "创建新项目"按钮
* 项目搜索和筛选

### 3.2 项目配置页 (`/project/:id/config`)

**用途**: 配置项目参数和分析规则
**主要内容**:

* 项目基本信息表单
* 语言类型选择
* 排除规则配置
* 分析参数设置(各维度阈值)

### 3.3 代码上传页 (`/project/:id/upload`)

**用途**: 上传代码文件或 Git 仓库
**主要内容**:

* 多种上传方式切换(Tab 切换)
* 文件上传组件(支持拖拽)
* Git 仓库 URL 输入框
* 上传后文件树预览
* 代码文件预览窗口(支持语法高亮)

### 3.4 分析任务页 (`/project/:id/analysis`)

**用途**: 启动分析、查看分析进度
**主要内容**:

* "开始分析"按钮
* 分析进度条和状态提示
* 实时日志输出(可选)
* 完成后跳转到报告页

### 3.5 分析报告页 (`/project/:id/report`)

**用途**: 展示代码质量分析结果(核心页面)
**主要内容**:

* **总览区域**:

  * 总体评分(大号数字 + 等级标签)
  * 七维度雷达图
* **指标详情区域**:

  * 七维度得分表格
  * 各指标柱状图/饼图
* **问题统计区域**:

  * 问题分类列表(按维度)
  * 严重程度标记(高/中/低)
* **文件级报告区域**:

  * 最差文件排行表格
  * 点击文件跳转到问题详情
* **改进建议区域**:

  * AI 生成的建议列表
* **导出按钮**: 支持 PDF/Markdown/Excel

### 3.6 代码对比页 (`/project/:id/optimize`)

**用途**: 查看 AI 优化建议和代码对比
**主要内容**:

* **优化建议列表**: AI 针对命名、函数长度等的建议
* **代码对比视图**:

  * 左侧:原始代码(语法高亮)
  * 右侧:优化后代码(语法高亮)
  * 差异标记(添加/删除/修改)
* **操作按钮**: 下载优化代码、应用优化(可选)

### 3.7 历史记录页 (`/project/:id/history`)

**用途**: 查看项目的历史分析记录
**主要内容**:

* 历史分析列表(时间、评分、状态)
* 点击查看历史报告
* 对比不同版本的分析结果(可选)

---

## 四、前端代码架构

### 4.1 优化后的目录结构

```
src/
├── assets/              # 静态资源(图片、样式)
├── components/          # 公共组件
│   ├── common/          # 通用组件
│   │   ├── AppLayout.vue      # 应用布局
│   │   ├── LoadingSpinner.vue  # 加载动画
│   │   └── EmptyState.vue      # 空状态组件
│   ├── code/            # 代码相关组件
│   │   ├── CodeEditor.vue      # Monaco编辑器封装
│   │   ├── FileTree.vue        # 文件树组件
│   │   ├── DiffViewer.vue      # 代码差异对比
│   │   └── CodeHighlight.vue   # 代码高亮
│   ├── charts/          # 图表组件
│   │   ├── RadarChart.vue      # 雷达图(七维度)
│   │   ├── BarChart.vue        # 柱状图
│   │   ├── PieChart.vue        # 饼图
│   │   └── TrendChart.vue      # 趋势图
│   ├── report/          # 报告相关组件
│   │   ├── ScoreCard.vue       # 评分卡片
│   │   ├── IssueList.vue       # 问题列表
│   │   ├── MetricCard.vue      # 指标卡片
│   │   └── ExportPanel.vue     # 导出面板
│   └── upload/          # 上传相关组件
│       ├── FileUploader.vue   # 文件上传器
│       ├── GitUploader.vue    # Git仓库上传
│       └── UploadProgress.vue # 上传进度
├── views/               # 页面组件
│   ├── Home.vue         # 首页/项目列表
│   ├── ProjectConfig.vue    # 项目配置页
│   ├── CodeUpload.vue       # 代码上传页
│   ├── Analysis.vue         # 分析任务页
│   ├── Report.vue           # 分析报告页
│   ├── CodeOptimize.vue     # 代码对比页
│   └── History.vue          # 历史记录页
├── api/                 # API 接口封装
│   ├── request.js       # Axios 全局配置
│   ├── project.js       # 项目相关接口
│   ├── analysis.js      # 分析相关接口
│   ├── report.js        # 报告相关接口
│   ├── optimize.js      # AI 优化相关接口
│   └── upload.js        # 文件上传接口
├── router/              # 路由配置
│   ├── index.js         # 路由主文件
│   └── guards.js        # 路由守卫
├── stores/              # 状态管理(Pinia)
│   ├── project.js       # 项目状态管理
│   ├── analysis.js      # 分析状态管理
│   ├── upload.js        # 上传状态管理
│   └── websocket.js     # WebSocket状态管理
├── utils/               # 工具函数
│   ├── request.js       # Axios 封装
│   ├── formatter.js     # 数据格式化
│   ├── download.js      # 文件下载工具
│   ├── websocket.js     # WebSocket 工具
│   ├── diff.js          # 差异计算工具
│   └── validation.js    # 表单验证
├── composables/         # 组合式函数
│   ├── useUpload.js     # 上传逻辑
│   ├── useAnalysis.js   # 分析逻辑
│   ├── useWebSocket.js  # WebSocket逻辑
│   └── useChart.js      # 图表逻辑
├── styles/              # 样式文件
│   ├── global.css       # 全局样式
│   ├── variables.css    # CSS变量
│   └── antd-theme.js    # Ant Design主题配置
├── App.vue              # 根组件
└── main.js              # 入口文件
```

### 4.2 核心组件设计（Composition API风格）

#### 4.2.1 CodeEditor.vue (Monaco编辑器封装)

```vue
<script setup>
import * as monaco from 'monaco-editor'
import { ref, onMounted, onUnmounted, watch } from 'vue'

const props = defineProps({
  modelValue: String,
  language: { type: String, default: 'javascript' },
  readOnly: { type: Boolean, default: false },
  theme: { type: String, default: 'vs-dark' }
})

const emit = defineEmits(['update:modelValue', 'change'])
const editorRef = ref(null)
let editor = null

onMounted(() => {
  editor = monaco.editor.create(editorRef.value, {
    value: props.modelValue,
    language: props.language,
    readOnly: props.readOnly,
    theme: props.theme,
    minimap: { enabled: false },
    scrollBeyondLastLine: false
  })

  editor.onDidChangeModelContent(() => {
    const value = editor.getValue()
    emit('update:modelValue', value)
    emit('change', value)
  })
})

watch(() => props.modelValue, (newValue) => {
  if (editor && newValue !== editor.getValue()) {
    editor.setValue(newValue)
  }
})
</script>
```

**功能特性**:
* Monaco Editor完整功能支持
* 语法高亮 + 智能提示
* 只读模式用于代码预览
* 支持多语言切换
* 主题切换支持

#### 4.2.2 FileTree.vue (虚拟滚动文件树)

```vue
<script setup>
import { ref, computed } from 'vue'
import { RecycleScroller } from 'vue-virtual-scroller'

const props = defineProps(['files', 'expanded'])
const emit = defineEmits(['select', 'toggle'])

const handleFileClick = (file) => {
  emit('select', file)
}
</script>

<template>
  <RecycleScroller
    class="file-tree"
    :items="files"
    :item-size="32"
    key-field="id"
    v-slot="{ item }"
  >
    <div class="file-item" @click="handleFileClick(item)">
      <a-icon :type="item.type === 'folder' ? 'folder' : 'file'" />
      <span>{{ item.name }}</span>
    </div>
  </RecycleScroller>
</template>
```

**功能特性**:
* 虚拟滚动处理大文件列表
* 文件夹展开/折叠动画
* 文件类型图标显示
* 右键菜单支持
* 搜索过滤功能

#### 4.2.3 DiffViewer.vue (专业差异对比)

```vue
<script setup>
import { ref, computed, watch } from 'vue'
import { diff_match_patch } from 'diff-match-patch'

const props = defineProps({
  original: String,
  modified: String,
  language: String
})

const diffData = computed(() => {
  const dmp = new diff_match_patch()
  const diffs = dmp.diff_main(props.original, props.modified)
  dmp.diff_cleanupSemantic(diffs)
  return diffs
})
</script>
```

**功能特性**:
* 基于diff-match-patch精确计算差异
* 左右分屏 + 统一视图切换
* 语法高亮保持
* 行号对齐显示
* 差异统计信息

#### 4.2.4 评分展示组件系列

**ScoreCard.vue (总体评分卡片)**
```vue
<script setup>
import { computed } from 'vue'

const props = defineProps({
  score: { type: Number, required: true },
  animated: { type: Boolean, default: true }
})

const grade = computed(() => {
  if (props.score >= 90) return { text: '优秀', color: '#52c41a' }
  if (props.score >= 80) return { text: '良好', color: '#1890ff' }
  if (props.score >= 60) return { text: '及格', color: '#faad14' }
  return { text: '不及格', color: '#f5222d' }
})
</script>
```

**RadarChart.vue (七维度雷达图)**
```vue
<script setup>
import { ref, onMounted, watch } from 'vue'
import * as echarts from 'echarts'

const props = defineProps({
  dimensions: { type: Array, required: true },
  scores: { type: Array, required: true }
})

const chartRef = ref(null)
let chart = null

onMounted(() => {
  chart = echarts.init(chartRef.value)
  updateChart()
})

const updateChart = () => {
  const option = {
    radar: {
      indicator: props.dimensions.map(dim => ({
        name: dim.name,
        max: 100
      }))
    },
    series: [{
      type: 'radar',
      data: [{
        value: props.scores,
        areaStyle: { opacity: 0.3 }
      }]
    }]
  }
  chart.setOption(option)
}
</script>
```

#### 4.2.5 UploadComponents.vue (专业上传组件)

**FileUploader.vue (文件上传)**
```vue
<script setup>
import { ref } from 'vue'
import { message } from 'ant-design-vue'
import { useUploadStore } from '@/stores/upload'

const uploadStore = useUploadStore()
const uploadRef = ref()

const customRequest = async ({ file, onSuccess, onError }) => {
  try {
    await uploadStore.uploadFile(file)
    onSuccess()
    message.success('上传成功')
  } catch (error) {
    onError(error)
    message.error('上传失败')
  }
}
</script>

<template>
  <a-upload
    ref="uploadRef"
    :custom-request="customRequest"
    :multiple="true"
    :show-upload-list="false"
    directory
  >
    <a-button type="primary" icon="upload">
      选择文件夹上传
    </a-button>
  </a-upload>
  <UploadProgress :progress="uploadStore.progress" />
</template>
```

### 4.3 路由配置（Vue Router 4.x）

```javascript
// router/index.js
import { createRouter, createWebHistory } from 'vue-router'
import AppLayout from '@/components/common/AppLayout.vue'

const routes = [
  {
    path: '/',
    component: AppLayout,
    children: [
      {
        path: '',
        name: 'Home',
        component: () => import('@/views/Home.vue'),
        meta: { title: '项目管理' }
      },
      {
        path: 'project/new',
        name: 'CreateProject',
        component: () => import('@/views/ProjectConfig.vue'),
        meta: { title: '创建项目' }
      },
      {
        path: 'project/:id',
        name: 'ProjectDetail',
        redirect: '/project/:id/upload',
        children: [
          {
            path: 'config',
            name: 'ProjectConfig',
            component: () => import('@/views/ProjectConfig.vue'),
            meta: { title: '项目配置' }
          },
          {
            path: 'upload',
            name: 'CodeUpload',
            component: () => import('@/views/CodeUpload.vue'),
            meta: { title: '代码上传' }
          },
          {
            path: 'analysis',
            name: 'Analysis',
            component: () => import('@/views/Analysis.vue'),
            meta: { title: '代码分析' }
          },
          {
            path: 'report',
            name: 'Report',
            component: () => import('@/views/Report.vue'),
            meta: { title: '分析报告' }
          },
          {
            path: 'optimize',
            name: 'CodeOptimize',
            component: () => import('@/views/CodeOptimize.vue'),
            meta: { title: 'AI优化建议' }
          },
          {
            path: 'history',
            name: 'History',
            component: () => import('@/views/History.vue'),
            meta: { title: '历史记录' }
          }
        ]
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫
router.beforeEach((to, from, next) => {
  document.title = to.meta.title ? `${to.meta.title} - 代码质量分析工具` : '代码质量分析工具'
  next()
})

export default router
```

### 4.4 状态管理（Pinia）

```javascript
// stores/project.js - 项目状态管理
import { defineStore } from 'pinia'
import { projectApi } from '@/api/project'

export const useProjectStore = defineStore('project', {
  state: () => ({
    projects: [],
    currentProject: null,
    loading: false
  }),

  getters: {
    activeProjects: (state) => state.projects.filter(p => p.status === 'active'),
    projectById: (state) => (id) => state.projects.find(p => p.id === id)
  },

  actions: {
    async fetchProjects() {
      this.loading = true
      try {
        const response = await projectApi.getProjects()
        this.projects = response.data
      } catch (error) {
        throw error
      } finally {
        this.loading = false
      }
    },

    async createProject(projectData) {
      const response = await projectApi.createProject(projectData)
      this.projects.push(response.data)
      return response.data
    },

    setCurrentProject(project) {
      this.currentProject = project
    }
  }
})
```

```javascript
// stores/analysis.js - 分析状态管理
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useAnalysisStore = defineStore('analysis', () => {
  // State
  const analysisStatus = ref('idle') // idle, analyzing, completed, error
  const analysisProgress = ref(0)
  const reportData = ref(null)
  const analysisError = ref(null)

  // Getters
  const isAnalyzing = computed(() => analysisStatus.value === 'analyzing')
  const isCompleted = computed(() => analysisStatus.value === 'completed')
  const hasError = computed(() => analysisStatus.value === 'error')

  // Actions
  const startAnalysis = () => {
    analysisStatus.value = 'analyzing'
    analysisProgress.value = 0
    analysisError.value = null
  }

  const updateProgress = (progress) => {
    analysisProgress.value = progress
  }

  const completeAnalysis = (data) => {
    analysisStatus.value = 'completed'
    reportData.value = data
    analysisProgress.value = 100
  }

  const setError = (error) => {
    analysisStatus.value = 'error'
    analysisError.value = error
  }

  const resetAnalysis = () => {
    analysisStatus.value = 'idle'
    analysisProgress.value = 0
    reportData.value = null
    analysisError.value = null
  }

  return {
    // State
    analysisStatus,
    analysisProgress,
    reportData,
    analysisError,
    // Getters
    isAnalyzing,
    isCompleted,
    hasError,
    // Actions
    startAnalysis,
    updateProgress,
    completeAnalysis,
    setError,
    resetAnalysis
  }
})
```

### 4.5 API 接口设计（前端调用）

```javascript
// api/request.js - Axios全局配置
import axios from 'axios'
import { message } from 'ant-design-vue'

const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// 请求拦截器
request.interceptors.request.use(
  config => {
    // 可以在这里添加token等
    return config
  },
  error => Promise.reject(error)
)

// 响应拦截器
request.interceptors.response.use(
  response => response.data,
  error => {
    const { response } = error
    if (response) {
      switch (response.status) {
        case 400:
          message.error('请求参数错误')
          break
        case 500:
          message.error('服务器内部错误')
          break
        default:
          message.error(response.data?.message || '请求失败')
      }
    } else {
      message.error('网络连接失败')
    }
    return Promise.reject(error)
  }
)

export default request
```

```javascript
// api/project.js - 项目相关接口
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
  deleteProject: (id) => request.delete(`/projects/${id}`)
}
```

```javascript
// api/upload.js - 文件上传接口
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
  })
}
```

```javascript
// api/analysis.js - 分析相关接口
import request from './request'

export const analysisApi = {
  // 启动分析
  startAnalysis: (projectId) => request.post(`/projects/${projectId}/analyze`),

  // 获取分析状态
  getAnalysisStatus: (projectId) => request.get(`/projects/${projectId}/analysis/status`),

  // 取消分析
  cancelAnalysis: (projectId) => request.delete(`/projects/${projectId}/analyze`)
}
```

```javascript
// api/report.js - 报告相关接口
export const reportApi = {
  // 获取分析报告
  getReport: (projectId) => request.get(`/projects/${projectId}/report`),

  // 导出报告
  exportReport: (projectId, format) =>
    request.get(`/projects/${projectId}/export`, {
      params: { format },
      responseType: 'blob'
    }),

  // 获取历史分析记录
  getHistory: (projectId) => request.get(`/projects/${projectId}/history`)
}
```

```javascript
// api/optimize.js - AI优化相关接口
export const optimizeApi = {
  // 获取优化建议
  getOptimizationSuggestions: (projectId) => request.get(`/projects/${projectId}/optimize`),

  // 获取代码对比数据
  getCodeDiff: (projectId, filePath) => request.get(`/projects/${projectId}/diff`, {
    params: { file: filePath }
  }),

  // 应用优化建议
  applyOptimization: (projectId, suggestionId) => request.post(`/projects/${projectId}/optimize/apply`, {
    suggestionId
  })
}
```

### 4.6 WebSocket实时通信

```javascript
// utils/websocket.js - WebSocket工具类
export class WebSocketManager {
  constructor(url) {
    this.url = url
    this.ws = null
    this.reconnectAttempts = 0
    this.maxReconnectAttempts = 5
    this.reconnectInterval = 3000
    this.handlers = new Map()
  }

  connect() {
    try {
      this.ws = new WebSocket(this.url)
      this.setupEventListeners()
    } catch (error) {
      console.error('WebSocket连接失败:', error)
      this.reconnect()
    }
  }

  setupEventListeners() {
    this.ws.onopen = () => {
      console.log('WebSocket连接已建立')
      this.reconnectAttempts = 0
    }

    this.ws.onmessage = (event) => {
      try {
        const data = JSON.parse(event.data)
        this.handleMessage(data)
      } catch (error) {
        console.error('解析WebSocket消息失败:', error)
      }
    }

    this.ws.onclose = () => {
      console.log('WebSocket连接已关闭')
      this.reconnect()
    }

    this.ws.onerror = (error) => {
      console.error('WebSocket错误:', error)
    }
  }

  handleMessage(data) {
    const { type, payload } = data
    const handlers = this.handlers.get(type) || []
    handlers.forEach(handler => handler(payload))
  }

  subscribe(type, handler) {
    if (!this.handlers.has(type)) {
      this.handlers.set(type, [])
    }
    this.handlers.get(type).push(handler)
  }

  unsubscribe(type, handler) {
    const handlers = this.handlers.get(type)
    if (handlers) {
      const index = handlers.indexOf(handler)
      if (index > -1) {
        handlers.splice(index, 1)
      }
    }
  }

  reconnect() {
    if (this.reconnectAttempts < this.maxReconnectAttempts) {
      this.reconnectAttempts++
      setTimeout(() => {
        console.log(`WebSocket重连尝试 ${this.reconnectAttempts}/${this.maxReconnectAttempts}`)
        this.connect()
      }, this.reconnectInterval)
    }
  }

  disconnect() {
    if (this.ws) {
      this.ws.close()
      this.ws = null
    }
  }
}

// composables/useWebSocket.js - WebSocket组合函数
import { onUnmounted, ref } from 'vue'
import { WebSocketManager } from '@/utils/websocket'

export function useWebSocket(projectId) {
  const wsManager = ref(null)
  const isConnected = ref(false)

  const connect = () => {
    const wsUrl = `${import.meta.env.VITE_WS_BASE_URL}/analysis/${projectId}`
    wsManager.value = new WebSocketManager(wsUrl)
    wsManager.value.connect()

    isConnected.value = true
  }

  const subscribe = (type, handler) => {
    if (wsManager.value) {
      wsManager.value.subscribe(type, handler)
    }
  }

  const disconnect = () => {
    if (wsManager.value) {
      wsManager.value.disconnect()
      isConnected.value = false
    }
  }

  onUnmounted(() => {
    disconnect()
  })

  return {
    isConnected,
    connect,
    subscribe,
    disconnect
  }
}
```

---

## 五、优化后的开发优先级和阶段规划（6-7周）

### 阶段 1: 基础框架搭建 (Week 1)

**目标**: 搭建现代化前端开发环境和基础架构

**主要任务**:
- ✅ 项目初始化(Vite + Vue 3 + Vue Router + Pinia)
- ✅ Ant Design Vue 组件库集成和主题配置
- ✅ Axios 全局配置和 API 模块设计
- ✅ 基础布局组件(AppLayout)开发
- ✅ 路由配置和页面结构搭建
- ✅ 状态管理设计(Project、Analysis、Upload stores)
- ✅ WebSocket 实时通信基础设施
- ✅ 开发环境配置(VS Code、ESLint、Prettier)

**交付物**:
- 完整的项目脚手架
- 基础页面框架和导航
- 完整的开发环境配置

### 阶段 2: 核心功能开发 (Week 2-4)

#### Week 2: 项目管理和文件上传
**目标**: 实现项目创建、配置和文件上传功能

**主要任务**:
- ✅ 首页/项目列表页面开发
- ✅ 项目配置页面(基本信息、分析参数)
- ✅ Monaco Editor 代码编辑器组件封装
- ✅ 文件上传组件(支持拖拽、进度显示)
- ✅ Git 仓库导入功能
- ✅ 虚拟滚动文件树组件
- ✅ 文件预览功能(语法高亮)

**交付物**:
- 完整的项目管理功能
- 文件上传和预览功能

#### Week 3: 代码分析功能
**目标**: 实现代码分析和进度展示

**主要任务**:
- ✅ 分析任务页面开发
- ✅ WebSocket 实时进度展示
- ✅ 分析状态管理
- ✅ 分析结果数据结构设计
- ✅ 错误处理和异常状态

**交付物**:
- 完整的代码分析功能
- 实时进度追踪

#### Week 4: 分析报告展示
**目标**: 实现全面的代码质量报告展示

**主要任务**:
- ✅ 分析报告页面总览设计
- ✅ ECharts 七维度雷达图组件
- ✅ 指标详情图表(柱状图、饼图)
- ✅ 问题列表和严重程度标记
- ✅ 文件级问题定位
- ✅ 响应式设计和移动端适配

**交付物**:
- 完整的分析报告展示
- 数据可视化图表

### 阶段 3: 高级功能开发 (Week 5)

**目标**: 实现 AI 优化建议和代码对比功能

**主要任务**:
- ✅ AI 优化建议展示页面
- ✅ diff-match-patch 代码差异对比组件
- ✅ 左右分屏代码对比视图
- ✅ 优化建议分类和优先级
- ✅ 下载优化代码功能
- ✅ 代码对比性能优化

**交付物**:
- AI 优化功能
- 代码对比工具

### 阶段 4: 完善和优化 (Week 6)

**目标**: 实现辅助功能和性能优化

**主要任务**:
- ✅ 历史记录页面开发
- ✅ 多格式报告导出功能(PDF/Markdown/Excel)
- ✅ 大文件加载性能优化
- ✅ 图表渲染性能优化
- ✅ 缓存策略实现
- ✅ 错误边界和异常处理

**交付物**:
- 完整的辅助功能
- 性能优化版本

### 阶段 5: 测试和部署 (Week 7)

**目标**: 全面测试、优化体验和准备部署

**主要任务**:
- ✅ 前后端联调测试
- ✅ 用户体验优化
- ✅ 界面细节打磨
- ✅ 跨浏览器兼容性测试
- ✅ 性能监控和分析
- ✅ 构建优化和部署配置

**交付物**:
- 生产就绪的应用
- 部署文档和脚本

---

## 六、技术难点和优化解决方案

### 6.1 大文件上传性能优化

**问题**: 大型项目文件上传速度慢、容易失败
**解决方案**:
```javascript
// 分片上传实现
class ChunkUploader {
  constructor(file, chunkSize = 1024 * 1024 * 2) { // 2MB
    this.file = file
    this.chunkSize = chunkSize
    this.totalChunks = Math.ceil(file.size / chunkSize)
    this.uploadedChunks = 0
  }

  async uploadChunks(onProgress) {
    for (let i = 0; i < this.totalChunks; i++) {
      const chunk = this.file.slice(
        i * this.chunkSize,
        (i + 1) * this.chunkSize
      )

      await this.uploadChunk(chunk, i)
      this.uploadedChunks++

      const progress = (this.uploadedChunks / this.totalChunks) * 100
      onProgress?.(progress)
    }
  }
}
```

### 6.2 代码编辑器性能优化

**问题**: Monaco Editor 包体积大，加载慢
**解决方案**:
```javascript
// 动态导入 + 语言包按需加载
const loadMonaco = async (language) => {
  // 先加载核心编辑器
  const monaco = await import('monaco-editor-core')

  // 按需加载语言支持
  switch (language) {
    case 'javascript':
      await import('monaco-editor/esm/vs/language/typescript/typescriptWorker.js')
      break
    case 'python':
      await import('monaco-editor/esm/vs/language/python/pythonWorker.js')
      break
    // 其他语言...
  }

  return monaco
}
```

### 6.3 图表渲染性能优化

**问题**: 大量数据时图表渲染慢
**解决方案**:
```vue
<!-- 使用 Intersection Observer 实现懒加载 -->
<template>
  <div ref="chartContainer" class="chart-container">
    <div v-if="isVisible">
      <RadarChart :data="chartData" />
    </div>
    <div v-else class="chart-placeholder">
      <a-spin size="large" />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'

const isVisible = ref(false)
const chartContainer = ref(null)
let observer = null

onMounted(() => {
  observer = new IntersectionObserver(
    ([entry]) => {
      if (entry.isIntersecting) {
        isVisible.value = true
        observer.disconnect()
      }
    },
    { threshold: 0.1 }
  )

  observer.observe(chartContainer.value)
})

onUnmounted(() => {
  observer?.disconnect()
})
</script>
```

### 6.4 WebSocket 连接稳定性

**问题**: 网络不稳定导致 WebSocket 连接断开
**解决方案**:
- 实现自动重连机制
- 连接状态监测和提示
- 离线状态缓存数据

### 6.5 代码差异计算优化

**问题**: 大文件差异计算耗时长
**解决方案**:
- Web Worker 后台计算
- 增量差异计算
- 虚拟滚动渲染差异结果

---

## 七、前端与后端协作要点

### 7.1 API 接口规范
```yaml
# API 接口文档示例
projects:
  create:
    method: POST
    path: /api/projects
    body:
      name: string
      description: string
      language: string
      config: object
    response:
      id: string
      name: string
      createdAt: string

  analyze:
    method: POST
    path: /api/projects/{id}/analyze
    response:
      taskId: string
      status: string

# WebSocket 消息格式
analysis:
  progress:
    type: "analysis_progress"
    payload:
      taskId: string
      progress: number
      stage: string
      message: string

  completed:
    type: "analysis_completed"
    payload:
      taskId: string
      result: object
```

### 7.2 数据格式约定

```typescript
// 项目数据结构
interface Project {
  id: string
  name: string
  description: string
  language: string
  createdAt: string
  updatedAt: string
  config: ProjectConfig
  files: FileInfo[]
  lastAnalysis?: AnalysisResult
}

// 分析结果数据结构
interface AnalysisResult {
  id: string
  projectId: string
  overallScore: number
  grade: 'excellent' | 'good' | 'average' | 'poor'
  dimensions: DimensionScore[]
  issues: Issue[]
  fileResults: FileResult[]
  generatedAt: string
}

interface DimensionScore {
  name: string
  score: number
  maxScore: number
  description: string
  issues: number
}

interface Issue {
  id: string
  type: string
  severity: 'high' | 'medium' | 'low'
  file: string
  line: number
  column: number
  message: string
  suggestion: string
}
```

### 7.3 错误处理约定
```typescript
// 统一错误响应格式
interface ErrorResponse {
  success: false
  error: {
    code: string
    message: string
    details?: any
  }
}

// 错误码定义
enum ErrorCodes {
  PROJECT_NOT_FOUND = 'PROJECT_NOT_FOUND',
  ANALYSIS_FAILED = 'ANALYSIS_FAILED',
  UPLOAD_FAILED = 'UPLOAD_FAILED',
  INVALID_FILE_FORMAT = 'INVALID_FILE_FORMAT'
}
```

### 7.4 文件上传约定
- 单文件大小限制：10MB
- 项目总大小限制：100MB
- 支持格式：.zip, .git repo, 文件夹上传
- 分片上传：分片大小 2MB

### 7.5 WebSocket 消息格式
- 连接地址：`ws://localhost:8080/ws/analysis/{projectId}`
- 心跳检测：每30秒一次
- 消息类型：JSON格式，包含type和payload字段

---

## 八、项目配置和部署

### 8.1 环境变量配置
```bash
# .env.development
VITE_API_BASE_URL=http://localhost:8080/api
VITE_WS_BASE_URL=ws://localhost:8080/ws
VITE_APP_TITLE=代码质量分析工具(开发版)
VITE_MAX_UPLOAD_SIZE=104857600  # 100MB

# .env.production
VITE_API_BASE_URL=https://api.code-analyzer.com
VITE_WS_BASE_URL=wss://ws.code-analyzer.com
VITE_APP_TITLE=代码质量分析工具
VITE_MAX_UPLOAD_SIZE=104857600
```

### 8.2 构建配置
```javascript
// vite.config.js
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': resolve(__dirname, 'src')
    }
  },
  build: {
    rollupOptions: {
      output: {
        manualChunks: {
          'vendor': ['vue', 'vue-router', 'pinia'],
          'antd': ['ant-design-vue'],
          'editor': ['monaco-editor'],
          'charts': ['echarts']
        }
      }
    },
    chunkSizeWarningLimit: 1000
  },
  server: {
    port: 3000,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      },
      '/ws': {
        target: 'ws://localhost:8080',
        ws: true
      }
    }
  }
})
```

### 8.3 Docker 部署配置
```dockerfile
# Dockerfile
FROM node:18-alpine as builder
WORKDIR /app
COPY package*.json ./
RUN npm ci --only=production
COPY . .
RUN npm run build

FROM nginx:alpine
COPY --from=builder /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/nginx.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
```

---

## 九、总结

### 9.1 项目特点
- **现代化技术栈**: Vue 3 + Composition API + Pinia + Ant Design Vue
- **企业级架构**: 组件化、模块化、可扩展设计
- **性能优化**: 虚拟滚动、懒加载、分片上传
- **实时体验**: WebSocket 实时进度推送
- **专业工具**: Monaco Editor、diff-match-patch、ECharts

### 9.2 核心优势
1. **用户体验优先**: 实时反馈、流畅交互、响应式设计
2. **性能表现优秀**: 大文件处理、代码高亮、图表渲染优化
3. **开发效率高**: 完整的开发环境、详细的API设计、代码规范
4. **可维护性强**: 清晰的架构设计、状态管理、错误处理
5. **扩展性良好**: 模块化设计、插件化架构、配置化管理

### 9.3 交付成果
- ✅ **完整的前端应用**: 7个核心页面，6大功能模块
- ✅ **现代化开发环境**: Vite + Vue 3 + TypeScript支持
- ✅ **企业级组件库**: 自定义组件 + Ant Design Vue集成
- ✅ **性能优化方案**: 大文件、代码编辑器、图表渲染优化
- ✅ **实时通信方案**: WebSocket + 状态管理
- ✅ **部署配置**: Docker + Nginx + 环境配置

### 9.4 项目规划
- **开发周期**: 6-7周，分为5个阶段
- **技术路线**: MVP → 功能完善 → 性能优化 → 测试部署
- **质量保证**: 代码规范、单元测试、集成测试
- **交付标准**: 生产就绪、文档齐全、性能达标

这个优化后的前端开发方案基于现代前端技术栈，采用企业级架构设计，充分考虑了性能优化和用户体验，为代码质量分析工具提供了可靠、高效的前端解决方案。
```