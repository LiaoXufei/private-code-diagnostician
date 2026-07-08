# 代码质量分析工具 (Code Quality Analyzer)

一个基于 Vue 3 + Node.js 的现代化代码质量分析平台，支持多项目管理和七维度代码质量评估。

## 🌟 项目特性

- **🚀 现代化技术栈**：Vue 3 + Vite + Node.js + SQLite
- **📊 七维度分析**：代码复杂度、可维护性、代码重复度、测试覆盖率、安全性检查、性能分析、代码风格
- **📁 多项目管理**：支持创建、编辑、删除项目和文件管理
- **📈 数据可视化**：基于 ECharts 的雷达图和图表展示
- **🎨 企业级UI**：基于 Ant Design Vue 的专业界面
- **📱 响应式设计**：支持桌面端和移动端
- **🗄️ 数据持久化**：基于 SQLite 的完整数据管理
- **🔄 实时分析**：支持分析进度跟踪和状态管理

## 🏗️ 系统架构

```
代码质量分析工具/
├── backend/                 # 后端服务
│   ├── server.js           # 主服务器文件
│   ├── routes/             # API 路由
│   │   └── projects.js     # 项目管理API
│   ├── package.json        # 后端依赖
│   └── uploads/            # 文件存储目录
├── frontend/               # 前端应用
│   ├── src/                # 源代码
│   │   ├── views/          # 页面组件
│   │   ├── components/     # 公共组件
│   │   ├── api/            # API接口
│   │   ├── stores/         # 状态管理
│   │   └── router/         # 路由配置
│   ├── package.json        # 前端依赖
│   └── vite.config.js      # Vite配置
├── API-Documentation.md    # API文档
└── README.md              # 项目说明
```

## 🚀 快速开始

### 环境要求

- Node.js >= 18.0.0
- npm >= 8.0.0

### 安装步骤

1. **克隆项目**
   ```bash
   git clone https://github.com/your-username/code-quality-analyzer.git
   cd code-quality-analyzer
   ```

2. **安装后端依赖**
   ```bash
   cd backend
   npm install
   ```

3. **安装前端依赖**
   ```bash
   cd ../frontend
   npm install
   ```

4. **启动服务**

   启动后端服务（端口 9998）：
   ```bash
   cd backend
   PORT=9998 node server.js
   ```

   启动前端服务（端口 3000）：
   ```bash
   cd frontend
   npm run dev
   ```

5. **访问应用**

   前端应用：http://localhost:3000
   后端API：http://localhost:9998/api

## 📖 功能说明

### 项目管理
- ✅ 创建新项目
- ✅ 编辑项目信息
- ✅ 删除项目（完整清理数据和文件）
- ✅ 项目配置管理

### 文件管理
- ✅ 文件上传（支持拖拽上传）
- ✅ 文件列表展示
- ✅ 文件选择和删除
- ✅ 支持多种编程语言文件

### 代码分析
- ✅ 七维度代码质量分析
- ✅ 分析配置管理
- ✅ 分析进度跟踪
- ✅ 分析报告展示
- ✅ 雷达图可视化

### 数据管理
- ✅ SQLite 数据持久化
- ✅ 完整的删除功能（数据库+物理文件）
- ✅ 事务安全保障
- ✅ 数据一致性验证

## 🔧 技术栈

### 前端技术
- **Vue 3** - 渐进式 JavaScript 框架
- **Vite** - 下一代前端构建工具
- **Ant Design Vue** - 企业级 UI 组件库
- **Pinia** - Vue 3 状态管理
- **Vue Router 4** - 官方路由管理器
- **ECharts** - 数据可视化图表库
- **Axios** - HTTP 客户端

### 后端技术
- **Node.js** - JavaScript 运行时
- **Express.js** - Web 应用框架
- **SQLite** - 轻量级数据库
- **Multer** - 文件上传中间件
- **fs模块** - 文件系统操作

## 📊 七维度分析体系

| 维度 | 说明 | 评估指标 |
|------|------|----------|
| 🔧 代码复杂度 | 分析代码的圈复杂度、认知复杂度等 | 函数复杂度、嵌套深度、控制流复杂度 |
| 🛠️ 可维护性 | 评估代码的可读性、可修改性和可扩展性 | 命名规范、代码结构、注释质量 |
| 📋 代码重复度 | 检测重复的代码片段和模式 | 重复行数、相似度、重复模式 |
| 🧪 测试覆盖率 | 分析测试用例对代码的覆盖程度 | 行覆盖率、分支覆盖率、函数覆盖率 |
| 🛡️ 安全性检查 | 识别潜在的安全漏洞和风险 | SQL注入、XSS、CSRF等安全问题 |
| ⚡ 性能分析 | 分析代码的性能瓶颈和优化点 | 执行时间、内存使用、算法复杂度 |
| 🎨 代码风格 | 检查代码格式、命名规范等风格问题 | 缩进、空格、命名约定、格式规范 |

## 📚 API 文档

详细的 API 文档请参考：[API-Documentation.md](./API-Documentation.md)

### 主要 API 端点

```bash
# 项目管理
GET    /api/projects           # 获取项目列表
POST   /api/projects           # 创建新项目
GET    /api/projects/:id       # 获取项目详情
PUT    /api/projects/:id       # 更新项目信息
DELETE /api/projects/:id       # 删除项目

# 文件管理
POST   /api/projects/:id/files     # 上传文件
GET    /api/projects/:id/files     # 获取文件列表
DELETE /api/files/:fileId          # 删除文件

# 分析管理
POST   /api/projects/:id/analyze   # 启动分析
GET    /api/projects/:id/analysis  # 获取分析状态
GET    /api/analysis/:analysisId   # 获取分析结果
```

## 🔄 数据删除机制

### 完整删除策略
系统采用完整删除策略，确保数据一致性：

- **项目删除**：同时删除数据库记录和物理文件目录
- **文件删除**：同时删除数据库记录和物理文件
- **事务安全**：使用 SQLite 事务确保操作原子性
- **级联删除**：自动清理相关数据和分析结果

### 存储结构
```
backend/
├── code_quality_analyzer.db    # SQLite数据库
└── uploads/                    # 文件存储
    ├── [项目ID]/
    │   ├── 文件1.ext
    │   └── 文件2.ext
    └── temp/                   # 临时文件
```

## 🛠️ 开发指南

### 代码规范
- 使用 ESLint + Prettier 进行代码格式化
- 遵循 Vue 3 Composition API 最佳实践
- 使用 TypeScript 进行类型检查（可选）

### 项目结构
```
frontend/src/
├── views/              # 页面组件
│   ├── Home.vue       # 首页
│   ├── Projects.vue   # 项目管理
│   ├── Analysis.vue   # 分析页面
│   └── Report.vue     # 报告页面
├── components/         # 公共组件
│   ├── common/        # 通用组件
│   ├── charts/        # 图表组件
│   └── upload/        # 上传组件
├── api/               # API接口
├── stores/            # 状态管理
├── router/            # 路由配置
├── utils/             # 工具函数
└── styles/            # 样式文件
```

### 环境配置
```bash
# 开发环境 (.env.development)
VITE_API_BASE_URL=http://localhost:9998/api
VITE_WS_BASE_URL=ws://localhost:9998/ws
VITE_APP_TITLE=代码质量分析工具(开发版)

# 生产环境 (.env.production)
VITE_API_BASE_URL=/api
VITE_WS_BASE_URL=ws://localhost:9998/ws
VITE_APP_TITLE=代码质量分析工具
```

## 📦 部署

### 开发环境
```bash
# 启动后端
cd backend && PORT=9998 node server.js

# 启动前端
cd frontend && npm run dev
```

### 生产环境
```bash
# 构建前端
cd frontend && npm run build

# 启动后端（生产模式）
cd backend && NODE_ENV=production PORT=9998 node server.js
```

### Docker 部署
```dockerfile
# Dockerfile 示例
FROM node:18-alpine

WORKDIR /app

# 复制后端代码
COPY backend/package*.json ./backend/
RUN cd backend && npm install --production

# 复制前端代码
COPY frontend/package*.json ./frontend/
RUN cd frontend && npm install

COPY frontend/ ./frontend/
COPY backend/ ./backend/

# 构建前端
RUN cd frontend && npm run build

EXPOSE 9998

CMD ["node", "backend/server.js"]
```

## 🧪 测试

### 功能测试
1. 项目创建、编辑、删除
2. 文件上传和删除
3. 代码分析流程
4. 数据一致性验证

### API 测试
```bash
# 健康检查
curl http://localhost:9998/api/health

# 获取项目列表
curl http://localhost:9998/api/projects

# 创建测试项目
curl -X POST http://localhost:9998/api/projects \
  -H "Content-Type: application/json" \
  -d '{"name":"测试项目","language":"javascript"}'
```

## 📈 版本历史

### v1.2.1 (2025-10-29)
- 🔧 **重要修复**：完整实现删除功能
  - 修复项目删除：同时删除数据库记录和物理文件目录
  - 修复文件删除：同时删除数据库记录和物理文件
  - 添加数据库事务支持，确保删除操作的原子性
  - 实现级联删除：项目删除时自动清理相关数据
- 📚 文档完善：详细记录删除功能的实现和验证方法
- 🛡️ 安全增强：添加路径验证和权限检查机制

### v1.2.0 (2025-10-29)
- ✅ 完善项目管理 API，支持配置管理
- ✅ 新增文件管理 API，支持文件选择和删除
- ✅ 扩展分析 API，支持七维度分析和进度跟踪
- ✅ 添加系统配置 API，支持语言和分析类型管理
- ✅ 优化错误处理和响应格式

### v1.1.0 (2025-10-27)
- ✅ 基础项目管理功能
- ✅ 文件上传功能
- ✅ 基础分析功能

### v1.0.0 (2025-10-25)
- ✨ 初始版本发布
- ✨ 基础项目管理功能
- ✨ 代码上传和分析功能
- ✨ 分析报告展示

## 🤝 贡献指南

1. Fork 项目
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 打开 Pull Request

## 📄 许可证

本项目采用 MIT 许可证 - 查看 [LICENSE](LICENSE) 文件了解详情

## 📞 联系方式

- 项目地址: [GitHub Repository](https://github.com/your-username/code-quality-analyzer)
- 问题反馈: [Issues](https://github.com/your-username/code-quality-analyzer/issues)
- 文档: [Wiki](https://github.com/your-username/code-quality-analyzer/wiki)

## 🙏 致谢

感谢以下开源项目的支持：
- [Vue.js](https://vuejs.org/) - 渐进式 JavaScript 框架
- [Ant Design Vue](https://antdv.com/) - 企业级 UI 组件库
- [ECharts](https://echarts.apache.org/) - 数据可视化图表库
- [Express.js](https://expressjs.com/) - Web 应用框架
- [SQLite](https://sqlite.org/) - 轻量级数据库

---

**注意**: 这是一个演示项目，用于展示现代 Web 应用的开发能力。在实际生产环境中使用前，请确保进行充分的测试和安全评估。