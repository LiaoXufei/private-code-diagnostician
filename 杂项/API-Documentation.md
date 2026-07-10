# 代码质量分析工具 - API 文档

## 概述

本文档描述了代码质量分析工具的完整 API 接口规范，包括后端数据库架构、数据传输方式和前后端响应逻辑。

**基础信息：**
- 基础 URL: `http://localhost:9999/api`
- 数据格式: JSON
- 字符编码: UTF-8
- 认证方式: 当前无认证（后续扩展）

## 数据库架构

### 核心数据表

#### 1. projects 表 - 项目信息
```sql
CREATE TABLE projects (
  id TEXT PRIMARY KEY,                    -- 项目唯一标识 (UUID)
  name TEXT NOT NULL,                     -- 项目名称
  description TEXT DEFAULT '',            -- 项目描述
  language TEXT DEFAULT 'javascript',     -- 主要编程语言
  status TEXT DEFAULT 'active',          -- 项目状态 (active/inactive/archived)
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,  -- 创建时间
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,  -- 最后更新时间
  file_count INTEGER DEFAULT 0,          -- 文件数量统计
  last_analysis_id TEXT,                  -- 最后一次分析ID
  config TEXT DEFAULT '{}'                -- 项目配置 (JSON格式)
)
```

#### 2. analysis_results 表 - 分析结果
```sql
CREATE TABLE analysis_results (
  id TEXT PRIMARY KEY,                    -- 分析结果唯一标识
  project_id TEXT NOT NULL,               -- 关联项目ID
  overall_score INTEGER DEFAULT 0,       -- 总体评分 (0-100)
  grade TEXT DEFAULT '',                  -- 等级评定 (excellent/good/poor)
  dimensions TEXT,                        -- 七维度详细评分 (JSON)
  issues TEXT,                           -- 发现的问题列表 (JSON)
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP   -- 分析完成时间
)
```

#### 3. uploaded_files 表 - 文件管理
```sql
CREATE TABLE uploaded_files (
  id TEXT PRIMARY KEY,                    -- 文件唯一标识
  project_id TEXT NOT NULL,               -- 所属项目ID
  filename TEXT NOT NULL,                 -- 文件名
  file_path TEXT NOT NULL,               -- 存储路径
  file_size INTEGER DEFAULT 0,           -- 文件大小 (字节)
  upload_time DATETIME DEFAULT CURRENT_TIMESTAMP, -- 上传时间
  analysis_status TEXT DEFAULT 'pending'  -- 分析状态 (pending/analyzing/completed/failed)
)
```

## API 接口规范

### 统一响应格式

#### 成功响应
```json
{
  "success": true,
  "data": { /* 具体数据 */ },
  "message": "操作成功"
}
```

#### 错误响应
```json
{
  "success": false,
  "error": "错误信息",
  "code": "ERROR_CODE"
}
```

#### 列表响应
```json
{
  "success": true,
  "data": [
    { /* 项目1 */ },
    { /* 项目2 */ }
  ],
  "total": 10,
  "page": 1,
  "pageSize": 20
}
```

---

## 项目管理 API

### 1. 获取项目列表

**请求：**
```http
GET /api/projects
```

**响应：**
```json
{
  "success": true,
  "data": [
    {
      "id": "proj_12345678",
      "name": "电商平台前端",
      "description": "基于Vue 3的电商平台前端项目",
      "language": "javascript",
      "status": "active",
      "created_at": "2025-10-29T08:30:00.000Z",
      "updated_at": "2025-10-29T09:15:00.000Z",
      "file_count": 45,
      "last_analysis": {
        "id": "analysis_87654321",
        "overallScore": 92,
        "grade": "excellent",
        "completedAt": "2025-10-29T09:15:00.000Z"
      }
    }
  ],
  "total": 4
}
```

### 2. 创建新项目

**请求：**
```http
POST /api/projects
Content-Type: application/json

{
  "name": "新项目名称",
  "description": "项目描述",
  "language": "javascript",
  "config": {
    "analysisTypes": ["complexity", "maintainability", "style"],
    "excludeRules": ["node_modules/**", "*.min.js"],
    "thresholds": {
      "complexity": 10,
      "maintainability": 65,
      "commentRate": 0.1
    }
  }
}
```

**响应：**
```json
{
  "success": true,
  "data": {
    "id": "proj_new_123456",
    "name": "新项目名称",
    "description": "项目描述",
    "language": "javascript",
    "status": "active",
    "created_at": "2025-10-29T10:00:00.000Z",
    "updated_at": "2025-10-29T10:00:00.000Z",
    "file_count": 0,
    "config": {
      "analysisTypes": ["complexity", "maintainability", "style"],
      "excludeRules": ["node_modules/**", "*.min.js"],
      "thresholds": {
        "complexity": 10,
        "maintainability": 65,
        "commentRate": 0.1
      }
    }
  },
  "message": "项目创建成功"
}
```

### 3. 获取单个项目详情

**请求：**
```http
GET /api/projects/:id
```

**响应：**
```json
{
  "success": true,
  "data": {
    "id": "proj_12345678",
    "name": "电商平台前端",
    "description": "基于Vue 3的电商平台前端项目",
    "language": "javascript",
    "status": "active",
    "created_at": "2025-10-29T08:30:00.000Z",
    "updated_at": "2025-10-29T09:15:00.000Z",
    "file_count": 45,
    "config": {
      "analysisTypes": ["complexity", "maintainability", "duplication", "coverage", "security", "performance", "style"],
      "excludeRules": ["node_modules/**", "dist/**", "*.min.js"],
      "thresholds": {
        "complexity": 10,
        "maintainability": 65,
        "commentRate": 0.1
      },
      "includeTests": false
    },
    "lastAnalysis": {
      "id": "analysis_87654321",
      "overallScore": 92,
      "grade": "excellent",
      "dimensions": [
        { "name": "代码复杂度", "score": 88, "issues": 3 },
        { "name": "可维护性", "score": 95, "issues": 1 },
        { "name": "代码重复度", "score": 90, "issues": 2 },
        { "name": "测试覆盖率", "score": 85, "issues": 4 },
        { "name": "安全性检查", "score": 96, "issues": 1 },
        { "name": "性能分析", "score": 92, "issues": 2 },
        { "name": "代码风格", "score": 94, "issues": 1 }
      ],
      "completedAt": "2025-10-29T09:15:00.000Z"
    }
  }
}
```

### 4. 更新项目信息

**请求：**
```http
PUT /api/projects/:id
Content-Type: application/json

{
  "name": "更新后的项目名称",
  "description": "更新后的项目描述",
  "language": "typescript",
  "status": "active",
  "config": {
    "analysisTypes": ["complexity", "maintainability", "security", "performance"],
    "excludeRules": ["node_modules/**", "dist/**", "*.test.js"],
    "thresholds": {
      "complexity": 8,
      "maintainability": 70,
      "commentRate": 0.15
    },
    "includeTests": true
  }
}
```

**响应：**
```json
{
  "success": true,
  "data": {
    "id": "proj_12345678",
    "name": "更新后的项目名称",
    "description": "更新后的项目描述",
    "language": "typescript",
    "status": "active",
    "updated_at": "2025-10-29T10:30:00.000Z",
    "config": {
      "analysisTypes": ["complexity", "maintainability", "security", "performance"],
      "excludeRules": ["node_modules/**", "dist/**", "*.test.js"],
      "thresholds": {
        "complexity": 8,
        "maintainability": 70,
        "commentRate": 0.15
      },
      "includeTests": true
    }
  },
  "message": "项目更新成功"
}
```

### 5. 删除项目

**请求：**
```http
DELETE /api/projects/:id
```

**完整删除操作：**
- ✅ 从数据库删除项目记录
- ✅ 删除所有相关的分析结果记录
- ✅ 删除所有文件记录
- ✅ 删除项目在文件系统中的整个目录（包括所有上传的文件）
- ✅ 使用数据库事务确保数据一致性

**响应：**
```json
{
  "success": true,
  "message": "项目删除成功"
}
```

**实现细节：**
```javascript
// 后端实现逻辑
const projectDir = path.join(__dirname, '../uploads', id)
if (fs.existsSync(projectDir)) {
  fs.rmSync(projectDir, { recursive: true, force: true })
}

// 数据库事务删除
db.serialize(() => {
  db.run('BEGIN TRANSACTION')
  // 1. 删除分析结果
  db.run('DELETE FROM analysis_results WHERE project_id = ?', [id])
  // 2. 删除文件记录
  db.run('DELETE FROM uploaded_files WHERE project_id = ?', [id])
  // 3. 删除项目记录
  db.run('DELETE FROM projects WHERE id = ?', [id])
  db.run('COMMIT')
})
```

---

## 文件管理 API

### 1. 上传文件

**请求：**
```http
POST /api/projects/:id/files
Content-Type: multipart/form-data

files: [File, File, ...]  // 支持多文件上传
```

**支持的文件类型：**
- JavaScript: `.js`, `.jsx`, `.mjs`
- TypeScript: `.ts`, `.tsx`
- Vue: `.vue`
- Java: `.java`
- Python: `.py`
- C/C++: `.c`, `.cpp`, `.cc`, `.cxx`
- C#: `.cs`
- Go: `.go`
- Rust: `.rs`
- PHP: `.php`
- 其他: `.html`, `.css`, `.scss`, `.less`, `.json`, `.xml`, `.yaml`, `.yml`, `.md`, `.txt`, `.sql`

**文件大小限制：** 单个文件最大 10MB

**响应：**
```json
{
  "success": true,
  "data": {
    "uploadedFiles": [
      {
        "id": "file_123456",
        "filename": "app.js",
        "file_path": "/uploads/proj_123456/app.js",
        "file_size": 15420,
        "upload_time": "2025-10-29T10:45:00.000Z",
        "analysis_status": "pending"
      },
      {
        "id": "file_789012",
        "filename": "utils.js",
        "file_path": "/uploads/proj_123456/utils.js",
        "file_size": 8960,
        "upload_time": "2025-10-29T10:45:00.000Z",
        "analysis_status": "pending"
      }
    ],
    "totalFiles": 2,
    "totalSize": 24380
  },
  "message": "文件上传成功"
}
```

### 2. 获取项目文件列表

**请求：**
```http
GET /api/projects/:id/files
```

**响应：**
```json
{
  "success": true,
  "data": [
    {
      "id": "file_123456",
      "filename": "app.js",
      "file_path": "/uploads/proj_123456/app.js",
      "file_size": 15420,
      "upload_time": "2025-10-29T10:45:00.000Z",
      "analysis_status": "completed",
      "selected": false
    },
    {
      "id": "file_789012",
      "filename": "utils.js",
      "file_path": "/uploads/proj_123456/utils.js",
      "file_size": 8960,
      "upload_time": "2025-10-29T10:45:00.000Z",
      "analysis_status": "pending",
      "selected": true
    }
  ],
  "total": 2,
  "totalSize": 24380,
  "selectedCount": 1
}
```

### 3. 删除文件

**请求：**
```http
DELETE /api/files/:fileId
```

**完整删除操作：**
- ✅ 从数据库删除文件记录
- ✅ 删除文件在文件系统中的物理文件
- ✅ 更新项目的文件计数
- ✅ 使用数据库事务确保数据一致性

**响应：**
```json
{
  "success": true,
  "message": "文件删除成功"
}
```

**实现细节：**
```javascript
// 后端实现逻辑
// 1. 先获取文件信息
const file = db.prepare('SELECT * FROM uploaded_files WHERE id = ?').get(fileId)

// 2. 删除物理文件
if (fs.existsSync(file.file_path)) {
  fs.unlinkSync(file.file_path)
}

// 3. 删除数据库记录并更新项目文件计数
db.serialize(() => {
  db.run('BEGIN TRANSACTION')
  db.run('DELETE FROM uploaded_files WHERE id = ?', [fileId])
  db.run('UPDATE projects SET file_count = file_count - 1, updated_at = CURRENT_TIMESTAMP WHERE id = ?', [file.project_id])
  db.run('COMMIT')
})
```

---

## 分析管理 API

### 1. 启动代码分析

**请求：**
```http
POST /api/projects/:id/analyze
Content-Type: application/json

{
  "selectedFiles": ["file_123456", "file_789012"],
  "config": {
    "types": ["complexity", "maintainability", "security"],
    "strictness": "strict",
    "excludes": ["*.test.js", "node_modules/**"],
    "parallel": true,
    "maxConcurrency": 4
  }
}
```

**响应：**
```json
{
  "success": true,
  "data": {
    "analysisId": "analysis_new_123456",
    "projectId": "proj_12345678",
    "status": "running",
    "startTime": "2025-10-29T11:00:00.000Z",
    "estimatedDuration": 120,
    "filesToAnalyze": 2,
    "config": {
      "types": ["complexity", "maintainability", "security"],
      "strictness": "strict",
      "excludes": ["*.test.js", "node_modules/**"],
      "parallel": true,
      "maxConcurrency": 4
    }
  },
  "message": "分析任务已启动"
}
```

### 2. 获取分析状态

**请求：**
```http
GET /api/projects/:id/analysis
```

**响应：**
```json
{
  "success": true,
  "data": {
    "analysisId": "analysis_new_123456",
    "status": "running",
    "progress": 65,
    "startTime": "2025-10-29T11:00:00.000Z",
    "estimatedCompletion": "2025-10-29T11:02:00.000Z",
    "currentFile": "utils.js",
    "completedFiles": 1,
    "totalFiles": 2,
    "currentStage": "代码复杂度分析",
    "logs": [
      {
        "timestamp": "2025-10-29T11:00:00.000Z",
        "level": "info",
        "message": "开始分析文件: app.js"
      },
      {
        "timestamp": "2025-10-29T11:01:30.000Z",
        "level": "info",
        "message": "app.js 分析完成"
      }
    ]
  }
}
```

### 3. 取消分析

**请求：**
```http
DELETE /api/analysis/:analysisId
```

**响应：**
```json
{
  "success": true,
  "message": "分析已取消"
}
```

### 4. 获取分析历史

**请求：**
```http
GET /api/projects/:id/history
```

**响应：**
```json
{
  "success": true,
  "data": [
    {
      "id": "analysis_87654321",
      "overallScore": 92,
      "grade": "excellent",
      "status": "completed",
      "duration": 180,
      "filesAnalyzed": 45,
      "createdAt": "2025-10-29T09:15:00.000Z",
      "completedAt": "2025-10-29T09:18:00.000Z",
      "config": {
        "types": ["complexity", "maintainability", "security", "performance"],
        "strictness": "standard"
      }
    },
    {
      "id": "analysis_12345678",
      "overallScore": 85,
      "grade": "good",
      "status": "completed",
      "duration": 150,
      "filesAnalyzed": 42,
      "createdAt": "2025-10-28T14:20:00.000Z",
      "completedAt": "2025-10-28T14:23:00.000Z",
      "config": {
        "types": ["complexity", "maintainability", "style"],
        "strictness": "standard"
      }
    }
  ],
  "total": 2
}
```

### 5. 获取分析结果详情

**请求：**
```http
GET /api/analysis/:analysisId
```

**响应：**
```json
{
  "success": true,
  "data": {
    "id": "analysis_87654321",
    "projectId": "proj_12345678",
    "overallScore": 92,
    "grade": "excellent",
    "status": "completed",
    "duration": 180,
    "filesAnalyzed": 45,
    "createdAt": "2025-10-29T09:15:00.000Z",
    "completedAt": "2025-10-29T09:18:00.000Z",
    "dimensions": [
      {
        "name": "代码复杂度",
        "score": 88,
        "maxScore": 100,
        "issues": 3,
        "details": "平均圈复杂度为8.5，建议优化复杂度较高的函数"
      },
      {
        "name": "可维护性",
        "score": 95,
        "maxScore": 100,
        "issues": 1,
        "details": "代码结构清晰，命名规范，易于维护"
      },
      {
        "name": "代码重复度",
        "score": 90,
        "maxScore": 100,
        "issues": 2,
        "details": "发现少量重复代码，建议提取公共函数"
      },
      {
        "name": "测试覆盖率",
        "score": 85,
        "maxScore": 100,
        "issues": 4,
        "details": "测试覆盖率为85%，建议提高边界情况的测试覆盖"
      },
      {
        "name": "安全性检查",
        "score": 96,
        "maxScore": 100,
        "issues": 1,
        "details": "整体安全性良好，发现1个潜在安全风险"
      },
      {
        "name": "性能分析",
        "score": 92,
        "maxScore": 100,
        "issues": 2,
        "details": "性能表现良好，有2处可优化点"
      },
      {
        "name": "代码风格",
        "score": 94,
        "maxScore": 100,
        "issues": 1,
        "details": "代码风格一致，符合规范要求"
      }
    ],
    "issues": [
      {
        "id": "issue_1",
        "type": "complexity",
        "severity": "medium",
        "file": "/src/components/UserForm.vue",
        "line": 45,
        "column": 12,
        "message": "函数复杂度过高",
        "suggestion": "建议将复杂函数拆分为多个小函数",
        "rule": "complexity-function"
      },
      {
        "id": "issue_2",
        "type": "security",
        "severity": "high",
        "file": "/src/utils/helper.js",
        "line": 23,
        "column": 8,
        "message": "存在潜在的XSS风险",
        "suggestion": "使用DOMPurify等库进行输入验证和清理",
        "rule": "security-xss"
      }
    ],
    "fileResults": [
      {
        "file": "/src/components/UserForm.vue",
        "score": 89,
        "issues": 3,
        "linesOfCode": 120,
        "complexity": 8.5,
        "maintainability": 92,
        "topIssues": [
          {
            "type": "complexity",
            "severity": "medium",
            "line": 45,
            "message": "函数复杂度过高"
          }
        ]
      }
    ],
    "summary": {
      "totalIssues": 15,
      "issuesBySeverity": {
        "high": 2,
        "medium": 8,
        "low": 5
      },
      "issuesByType": {
        "complexity": 4,
        "security": 2,
        "maintainability": 3,
        "style": 3,
        "performance": 2,
        "duplication": 1
      },
      "recommendations": [
        "优先修复高严重性问题，特别是安全相关的问题",
        "考虑重构复杂度较高的函数",
        "增加测试覆盖率，特别是边界情况"
      ]
    }
  }
}
```

---

## 系统配置 API

### 1. 获取支持的编程语言

**请求：**
```http
GET /api/languages
```

**响应：**
```json
{
  "success": true,
  "data": [
    {
      "value": "javascript",
      "label": "JavaScript",
      "extensions": [".js", ".jsx", ".mjs"],
      "icon": "javascript"
    },
    {
      "value": "typescript",
      "label": "TypeScript",
      "extensions": [".ts", ".tsx"],
      "icon": "typescript"
    },
    {
      "value": "vue",
      "label": "Vue",
      "extensions": [".vue"],
      "icon": "vue"
    },
    {
      "value": "python",
      "label": "Python",
      "extensions": [".py"],
      "icon": "python"
    }
  ]
}
```

### 2. 获取分析类型配置

**请求：**
```http
GET /api/analysis-types
```

**响应：**
```json
{
  "success": true,
  "data": [
    {
      "value": "complexity",
      "label": "代码复杂度",
      "description": "分析代码的圈复杂度、认知复杂度等指标",
      "icon": "code",
      "enabled": true
    },
    {
      "value": "maintainability",
      "label": "可维护性",
      "description": "评估代码的可读性、可修改性和可扩展性",
      "icon": "tool",
      "enabled": true
    },
    {
      "value": "duplication",
      "label": "代码重复度",
      "description": "检测重复的代码片段和模式",
      "icon": "copy",
      "enabled": true
    },
    {
      "value": "coverage",
      "label": "测试覆盖率",
      "description": "分析测试用例对代码的覆盖程度",
      "icon": "security",
      "enabled": true
    },
    {
      "value": "security",
      "label": "安全性检查",
      "description": "识别潜在的安全漏洞和风险",
      "icon": "shield",
      "enabled": true
    },
    {
      "value": "performance",
      "label": "性能分析",
      "description": "分析代码的性能瓶颈和优化点",
      "icon": "thunderbolt",
      "enabled": true
    },
    {
      "value": "style",
      "label": "代码风格",
      "description": "检查代码格式、命名规范等风格问题",
      "icon": "bg-colors",
      "enabled": true
    }
  ]
}
```

---

## 错误代码说明

| 错误代码 | HTTP状态码 | 说明 |
|---------|-----------|------|
| `PROJECT_NOT_FOUND` | 404 | 项目不存在 |
| `FILE_NOT_FOUND` | 404 | 文件不存在 |
| `ANALYSIS_NOT_FOUND` | 404 | 分析结果不存在 |
| `VALIDATION_ERROR` | 400 | 请求参数验证失败 |
| `FILE_TOO_LARGE` | 413 | 文件大小超出限制 |
| `UNSUPPORTED_FILE_TYPE` | 400 | 不支持的文件类型 |
| `ANALYSIS_IN_PROGRESS` | 409 | 项目正在分析中 |
| `INTERNAL_ERROR` | 500 | 服务器内部错误 |
| `DATABASE_ERROR` | 500 | 数据库操作失败 |
| `STORAGE_ERROR` | 500 | 文件存储错误 |

## 使用示例

### JavaScript (Axios)

```javascript
// 获取项目列表
const response = await axios.get('http://localhost:9999/api/projects');
const projects = response.data.data;

// 创建新项目
const newProject = await axios.post('http://localhost:9999/api/projects', {
  name: '我的新项目',
  description: '这是一个测试项目',
  language: 'javascript'
});

// 上传文件
const formData = new FormData();
formData.append('files', fileInput.files[0]);

const uploadResponse = await axios.post(
  `http://localhost:9999/api/projects/${projectId}/files`,
  formData,
  {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  }
);

// 启动分析
const analysisResponse = await axios.post(
  `http://localhost:9999/api/projects/${projectId}/analyze`,
  {
    selectedFiles: ['file_123456'],
    config: {
      types: ['complexity', 'maintainability'],
      strictness: 'standard'
    }
  }
);
```

### cURL

```bash
# 获取项目列表
curl -X GET http://localhost:9999/api/projects

# 创建新项目
curl -X POST http://localhost:9999/api/projects \
  -H "Content-Type: application/json" \
  -d '{
    "name": "测试项目",
    "description": "API测试项目",
    "language": "javascript"
  }'

# 上传文件
curl -X POST http://localhost:9999/api/projects/proj_123456/files \
  -F "files=@/path/to/file.js"

# 启动分析
curl -X POST http://localhost:9999/api/projects/proj_123456/analyze \
  -H "Content-Type: application/json" \
  -d '{
    "selectedFiles": ["file_123456"],
    "config": {
      "types": ["complexity", "maintainability"],
      "strictness": "standard"
    }
  }'
```

---

## 删除功能实现详解

### 🔧 问题修复背景

**问题描述：**
在之前的版本中，删除操作存在数据不一致的问题：
- 点击删除按钮后，前端界面不再显示删除的项目/文件
- 但物理文件仍存储在 `uploads/` 目录中
- 数据库中可能存在孤立的分析结果记录
- 长期使用会造成存储空间浪费

**修复内容：**
1. **完整删除实现**：确保删除操作同时清理数据库记录和物理文件
2. **事务安全**：使用SQLite数据库事务保证操作的原子性
3. **级联删除**：项目删除时自动清理所有相关数据
4. **错误处理**：完善的错误处理和回滚机制

### 📁 存储架构

```
D:\Projects\代码审查Web开发\backend\
├── code_quality_analyzer.db     # SQLite数据库文件
└── uploads\                     # 文件存储目录
    ├── [项目ID1]\
    │   ├── 文件1-时间戳.ext
    │   ├── 文件2-时间戳.ext
    │   └── ...
    ├── [项目ID2]\
    │   └── ...
    └── temp\                   # 临时文件目录
```

### 🔄 删除流程

**项目删除流程：**
```
前端删除请求 → 后端验证 → 数据库事务开始
├── 删除分析结果记录
├── 删除文件记录
├── 删除项目记录
├── 删除物理目录(递归)
└── 提交事务 → 响应成功
```

**文件删除流程：**
```
前端删除请求 → 后端验证 → 获取文件信息
├── 数据库事务开始
├── 删除物理文件
├── 删除数据库记录
├── 更新项目文件计数
└── 提交事务 → 响应成功
```

### ✅ 删除功能验证

**测试方法：**
```bash
# 1. 上传测试文件
curl -X POST http://localhost:9999/api/projects/{projectId}/files \
  -F "files=@test.js"

# 2. 检查物理文件存在
ls uploads/{projectId}/

# 3. 删除文件
curl -X DELETE http://localhost:9999/api/files/{fileId}

# 4. 验证物理文件已删除
ls uploads/{projectId}/  # 文件应不存在
```

**数据库一致性检查：**
```sql
-- 检查孤立记录
SELECT COUNT(*) FROM uploaded_files WHERE project_id NOT IN (SELECT id FROM projects);
SELECT COUNT(*) FROM analysis_results WHERE project_id NOT IN (SELECT id FROM projects);
```

### 🛡️ 安全机制

1. **路径验证**：防止路径遍历攻击
2. **权限检查**：确保只能删除属于该项目的内容
3. **事务回滚**：操作失败时自动回滚数据库更改
4. **文件存在检查**：删除前验证文件是否存在

### 📊 性能考虑

- **目录删除**：使用 `fs.rmSync()` 递归删除，支持大目录
- **事务优化**：批量操作减少数据库I/O
- **错误恢复**：删除失败时自动清理部分删除的内容

---

## 版本更新记录

### v1.2.1 (2025-10-29)
- 🔧 **重要修复**：完整实现删除功能
  - ✅ 修复项目删除：同时删除数据库记录和物理文件目录
  - ✅ 修复文件删除：同时删除数据库记录和物理文件
  - ✅ 添加数据库事务支持，确保删除操作的原子性
  - ✅ 实现级联删除：项目删除时自动清理相关分析结果和文件记录
  - ✅ 完善错误处理和回滚机制
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
- ✅ 初始 API 版本
- ✅ 基础 CRUD 操作

---

**注意：** 本 API 文档基于当前系统实现编写，随着系统功能的扩展和优化，API 接口可能会有调整。建议在开发过程中关注最新版本的文档更新。
