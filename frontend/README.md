# 代码质量分析工具 - 前端应用

## 项目概述

这是一个基于 Vue 3 的现代化代码质量分析工具前端应用，支持多项目管理和实时代码质量分析。

## 技术栈

- **框架**: Vue 3 + Composition API
- **构建工具**: Vite
- **UI组件库**: Ant Design Vue
- **状态管理**: Pinia
- **路由**: Vue Router 4
- **代码编辑器**: Monaco Editor
- **图表库**: ECharts
- **HTTP客户端**: Axios
- **样式**: CSS3 + Less

## 功能特性

### 已实现功能
- ✅ 项目管理（创建、编辑、删除）
- ✅ 代码文件上传（支持拖拽上传、多文件选择、文件类型验证）
- ✅ 文件管理系统（文件列表展示、删除功能、选择功能）
- ✅ 多标签分析页面（文件选择、分析任务、分析历史、分析提示、分析配置、分析日志）
- ✅ 分析配置管理（七维度分析类型配置、排除规则、严格程度设置）
- ✅ 项目配置同步（项目设置与分析页面配置实时同步）
- ✅ 七维度雷达图（ECharts实现的代码质量可视化）
- ✅ 分析报告展示（详细指标、问题列表、文件排行）
- ✅ 数据库优先架构（基于SQLite数据库的真实数据管理）
- ✅ 响应式设计和用户体验优化

### 待开发功能
- 🔲 代码质量分析引擎（实际的代码分析算法实现）
- 🔲 分析进度实时展示（WebSocket实时通信）
- 🔲 分析任务队列管理（异步任务处理）
- 🔲 代码差异对比功能（版本间对比）
- 🔲 AI智能优化建议
- 🔲 分析结果版本比较
- 🔲 Git仓库集成（自动导入和分析）
- 🔲 用户认证和权限管理
- 🔲 团队协作功能
- 🔲 自定义分析规则
- 🔲 报告导出功能（PDF、Excel等格式）
- 🔲 邮件通知和集成

### 数据库架构详解

### 📁 数据存储位置

**1. 主数据库文件**
```
D:\Projects\代码审查Web开发\backend\code_quality_analyzer.db
```
- SQLite 数据库，包含所有项目和分析数据
- 支持完整的ACID事务特性
- 数据持久化存储

**2. 文件存储目录**
```
D:\Projects\代码审查Web开发\backend\uploads\
├── [项目ID1]/
│   ├── 文件1-时间戳.ext
│   ├── 文件2-时间戳.ext
│   └── ...
├── [项目ID2]/
│   └── ...
└── temp/ (临时文件)
```

### 🗄️ 数据库表结构

**projects 表 - 项目信息**
| 字段 | 类型 | 说明 |
|------|------|------|
| id | TEXT | 项目唯一标识(UUID) |
| name | TEXT | 项目名称 |
| description | TEXT | 项目描述 |
| language | TEXT | 主要编程语言 |
| status | TEXT | 项目状态(active/inactive/archived) |
| created_at | DATETIME | 创建时间 |
| updated_at | DATETIME | 最后更新时间 |
| file_count | INTEGER | 文件数量统计 |
| last_analysis_id | TEXT | 最后分析ID |
| config | TEXT(JSON) | 项目配置 |

**analysis_results 表 - 分析结果**
| 字段 | 类型 | 说明 |
|------|------|------|
| id | TEXT | 分析结果唯一标识 |
| project_id | TEXT | 关联项目ID |
| overall_score | INTEGER | 总体评分(0-100) |
| grade | TEXT | 等级评定(excellent/good/poor) |
| dimensions | TEXT(JSON) | 七维度详细评分 |
| issues | TEXT(JSON) | 发现的问题列表 |
| created_at | DATETIME | 分析完成时间 |

**uploaded_files 表 - 文件管理**
| 字段 | 类型 | 说明 |
|------|------|------|
| id | TEXT | 文件唯一标识 |
| project_id | TEXT | 所属项目ID |
| filename | TEXT | 文件名 |
| file_path | TEXT | 存储路径 |
| file_size | INTEGER | 文件大小(字节) |
| upload_time | DATETIME | 上传时间 |
| analysis_status | TEXT | 分析状态(pending/analyzing/completed/failed) |

### 🔄 数据流转机制

**项目创建流程：**
```
前端表单 → API请求 → 数据库事务 → 项目记录生成 → 响应确认
```

**文件上传流程：**
```
文件选择 → FormData → multer处理 → 文件存储 → 数据库记录 → 响应确认
```

**分析流程：**
```
配置选择 → 分析启动 → 任务队列 → 结果计算 → 数据库存储 → 前端展示
```

### ⚠️ 重要说明：数据删除机制

**当前删除逻辑存在的问题：**

1. **项目删除**：`DELETE /api/projects/:id`
   - ✅ 从数据库删除项目记录
   - ❌ **未删除项目目录中的实际文件**
   - ❌ **未删除相关分析结果记录**

2. **文件删除**：`DELETE /api/projects/:id/files/:fileId`
   - ✅ 从数据库删除文件记录
   - ✅ 更新项目文件计数
   - ❌ **未删除存储目录中的实际文件**

**实际影响：**
- 点击删除按钮后，前端界面不再显示删除的项目/文件
- 但物理文件仍存储在 `uploads/` 目录中
- 数据库中可能存在孤立的分析结果记录
- 长期使用会造成存储空间浪费

**建议改进：**
需要在删除操作中添加文件系统清理逻辑，确保数据一致性。

### 📊 前后端响应逻辑

**统一响应格式：**
```javascript
// 成功响应
{
  "success": true,
  "data": { /* 具体数据 */ },
  "message": "操作成功"
}

// 错误响应
{
  "success": false,
  "error": "错误信息",
  "code": "ERROR_CODE"
}
```

**状态管理流程：**
1. **前端状态**：Pinia store 缓存项目数据
2. **后端状态**：SQLite 数据库权威数据源
3. **状态同步**：每次操作后通过API同步数据
4. **错误处理**：统一的错误码和错误信息

### 🚀 技术特性
- 🚀 现代化开发体验（Vite + Vue 3）
- 🎨 企业级UI设计（Ant Design Vue）
- 📱 响应式布局，支持移动端
- 🔄 实时通信（WebSocket）
- 🔧 TypeScript 支持
- 📊 数据可视化（ECharts）
- 🌐 国际化支持准备

## 快速开始

### 环境要求

- Node.js >= 18.0.0
- npm >= 8.0.0

### 安装依赖

```bash
npm install
```

### 启动开发服务器

```bash
npm run dev
```

应用将在 http://localhost:3000 启动

### 构建生产版本

```bash
npm run build
```

### 预览生产版本

```bash
npm run preview
```

## 项目结构

```
frontend/
├── public/                 # 静态资源
├── src/
│   ├── assets/            # 项目资源
│   ├── components/        # 公共组件
│   │   ├── common/      # 通用组件
│   │   ├── code/        # 代码相关组件
│   │   ├── charts/      # 图表组件
│   │   ├── report/      # 报告组件
│   │   └── upload/      # 上传组件
│   ├── views/            # 页面组件
│   ├── api/              # API接口
│   ├── router/           # 路由配置
│   ├── stores/           # 状态管理
│   ├── utils/            # 工具函数
│   ├── composables/      # 组合式函数
│   └── styles/           # 样式文件
├── index.html             # HTML模板
├── package.json           # 项目配置
├── vite.config.js        # Vite配置
└── README.md              # 项目说明
```

## 开发指南

### 代码规范

- 使用 ESLint + Prettier 进行代码格式化
- 遵循 Vue 3 Composition API 最佳实践
- 使用 TypeScript 进行类型检查

### 组件开发

```vue
<template>
  <div class="component-name">
    <!-- 模板内容 -->
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'

// 响应式数据
const state = ref('')

// 计算属性
const computedValue = computed(() => {
  return state.value
})

// 方法
const handleClick = () => {
  // 处理逻辑
}

// 生命周期
onMounted(() => {
  // 初始化逻辑
})
</script>

<style scoped>
.component-name {
  /* 组件样式 */
}
</style>
```

### 状态管理

使用 Pinia 进行状态管理：

```javascript
// stores/example.js
import { defineStore } from 'pinia'

export const useExampleStore = defineStore('example', () => {
  // State
  const state = ref('')

  // Getters
  const getter = computed(() => state.value)

  // Actions
  const action = () => {
    state.value = 'new value'
  }

  return {
    state,
    getter,
    action
  }
})
```

### API 调用

```javascript
import { projectApi } from '@/api/project'

// 调用API
const projects = await projectApi.getProjects()
```

## 环境配置

### 开发环境 (.env.development)

```bash
VITE_API_BASE_URL=http://localhost:9999/api
VITE_WS_BASE_URL=ws://localhost:9999/ws
VITE_APP_TITLE=代码质量分析工具(开发版)
```

### 生产环境 (.env.production)

```bash
VITE_API_BASE_URL=/api
VITE_WS_BASE_URL=ws://localhost:9999/ws
VITE_APP_TITLE=代码质量分析工具
```

## 部署

### Docker 部署

```bash
# 构建镜像
docker build -t code-quality-analyzer-frontend .

# 运行容器
docker run -p 80:80 code-quality-analyzer-frontend
```

### Nginx 配置

```nginx
server {
    listen 80;
    server_name your-domain.com;
    root /usr/share/nginx/html;
    index index.html;

    location / {
        try_files $uri $uri/ /index.html;
    }

    location /api {
        proxy_pass http://backend-server:8080;
    }
}
```

## 浏览器支持

- Chrome >= 70
- Firefox >= 65
- Safari >= 12
- Edge >= 79

## 贡献指南

1. Fork 项目
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 打开 Pull Request

## 许可证

本项目采用 MIT 许可证 - 查看 [LICENSE](LICENSE) 文件了解详情

## 联系方式

- 项目地址: [GitHub Repository]
- 问题反馈: [Issues]
- 文档: [Wiki]

## 更新日志

### v1.2.0-dev (2025-10-29)

**🚀 重大功能更新 - 分析系统完整实现**
- ✅ 实现完整的多标签分析页面架构
  - 六大功能模块：文件选择、分析任务、分析历史、分析提示、分析配置、分析日志
  - 徽章通知系统和状态管理
  - 响应式布局和用户体验优化
- ✅ 七维度雷达图开发完成
  - 基于 ECharts 的专业数据可视化
  - 支持代码复杂度、可维护性、代码重复度、测试覆盖率、安全性检查、性能分析、代码风格
  - 实时数据更新和交互式体验
- ✅ 分析配置管理系统
  - 支持七种分析类型配置
  - 排除规则和文件过滤设置
  - 严格程度和并行处理配置
- ✅ 项目配置同步机制
  - 项目设置页与分析页面实时配置同步
  - 数据库驱动的配置管理
  - 统一的分析类型标准化

**🔧 系统架构优化**
- ✅ 数据库优先架构实现
  - 所有项目数据基于 SQLite 真实数据管理
  - 文件统计、项目信息动态更新
  - 测试数据与生产数据分离管理
- ✅ 文件管理系统增强
  - 支持文件名显示和文件选择
  - 表格式文件管理界面
  - 批量文件操作和删除功能
- ✅ 前后端集成优化
  - API 路径统一和错误处理
  - 状态同步和数据一致性
  - 用户体验和错误提示优化

**🎨 用户体验提升**
- ✅ 响应式设计全面优化
  - 移动端适配和多分辨率支持
  - 组件库样式统一和主题一致性
  - 交互反馈和加载状态优化
- ✅ 功能完整性验证
  - 项目创建、配置、上传、分析流程完整测试
  - 数据持久化和状态管理验证
  - 跨页面数据传递和同步测试

**📊 技术实现细节**
- 前端：Vue 3 Composition API + Pinia 状态管理
- 可视化：ECharts 5.4.3 雷达图实现
- 样式：Ant Design Vue 组件库深度定制
- 数据：SQLite 数据库集成和 API 设计

### v1.1.0-dev (2025-10-27)

**🔧 Bug修复和功能改进**
- ✅ 修复文件上传功能实现
  - 解决前后端API路径不匹配问题（前端: `/upload` vs 后端: `/files`）
  - 添加multer中间件支持FormData文件上传
  - 创建新的文件上传API端点
  - 支持多文件同时上传处理
- ✅ 实现拖拽文件上传功能
  - 支持HTML5拖拽API
  - 添加文件类型验证（支持常见编程语言）
  - 添加文件大小限制（10MB）
  - 实现多文件拖拽处理
  - 优化用户体验和错误提示
- ✅ 验证项目删除功能完整性
  - 确认API调用路径正确
  - 验证状态同步机制正常

**⚠️ 当前状态**
- 🔄 文件上传功能：代码已修复，但测试时显示错误，需要进一步调试
- 🎯 问题可能原因：后端服务器端口冲突、依赖包加载、或路由注册问题
- 📋 下一步：需要单独测试文件上传功能，解决服务器启动问题

**🚀 技术改进**
- 后端：添加multer@1.4.5-lts.1依赖
- 前端：优化文件处理和验证逻辑
- 改进错误处理和用户反馈机制

### v1.0.0 (2025-10-25)

- ✨ 初始版本发布
- ✨ 基础项目管理功能
- ✨ 代码上传和分析功能
- ✨ 分析报告展示
- 🎨 响应式设计
- 🚀 Vue 3 + Vite 架构

---

**注意**: 前端应用需要配合后端 API 服务使用，请确保后端服务正常运行。