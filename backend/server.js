const express = require('express')
const cors = require('cors')
const bodyParser = require('body-parser')
const { v4: uuidv4 } = require('uuid')
const sqlite3 = require('sqlite3').verbose()
const path = require('path')
const multer = require('multer')
const fs = require('fs')

// 导入路由
const projectRoutes = require('./routes/projects')
const adminRoutes = require('./routes/admin')

// 创建Express应用
const app = express()
const PORT = process.env.PORT || 9999

// 确保上传目录存在
const uploadsDir = path.join(__dirname, 'uploads')
if (!fs.existsSync(uploadsDir)) {
  fs.mkdirSync(uploadsDir, { recursive: true })
}

// 配置multer存储
const storage = multer.diskStorage({
  destination: (req, file, cb) => {
    const projectId = req.params.id || 'temp'
    const projectUploadDir = path.join(uploadsDir, projectId)

    // 确保项目上传目录存在
    if (!fs.existsSync(projectUploadDir)) {
      fs.mkdirSync(projectUploadDir, { recursive: true })
    }

    cb(null, projectUploadDir)
  },
  filename: (req, file, cb) => {
    // 保持原始文件名，如果重复则添加时间戳
    const uniqueSuffix = Date.now() + '-' + Math.round(Math.random() * 1E9)
    cb(null, file.originalname)
  }
})

// 配置multer
const upload = multer({
  storage: storage,
  limits: {
    fileSize: 10 * 1024 * 1024 // 10MB限制
  },
  fileFilter: (req, file, cb) => {
    // 检查文件类型
    const allowedTypes = /\.(js|jsx|ts|tsx|vue|java|py|cpp|c|cs|go|rs|php|rb|swift|kt|html|css|scss|less|sass|json|xml|yaml|yml|md|txt|sql)$/i
    if (allowedTypes.test(file.originalname)) {
      cb(null, true)
    } else {
      cb(new Error('不支持的文件格式'), false)
    }
  }
})

// 将上传中间件添加到请求对象中
app.use((req, res, next) => {
  req.upload = upload
  next()
})

// 中间件
app.use(cors({
  origin: ['http://localhost:3010', 'http://localhost:3000', 'http://localhost:3001', 'http://localhost:3010'],  // 允许多个开发端口
  credentials: true,  // 允许携带凭据
  methods: ['GET', 'POST', 'PUT', 'DELETE', 'OPTIONS'],  // 允许的HTTP方法
  allowedHeaders: ['Content-Type', 'Authorization', 'X-Request-ID']  // 允许的请求头
}))
app.use(bodyParser.json({ limit: '50mb' }))
app.use(express.json())

// 将数据库实例添加到请求对象中，方便路由使用
app.use((req, res, next) => {
  req.db = db
  next()
})

// 数据库初始化
const db = new sqlite3.Database('code_quality_analyzer.db', { verbose: true })

// 创建表
db.serialize(() => {
  db.run(`
    CREATE TABLE IF NOT EXISTS projects (
      id TEXT PRIMARY KEY,
      name TEXT NOT NULL,
      description TEXT DEFAULT '',
      language TEXT DEFAULT 'javascript',
      status TEXT DEFAULT 'active',
      created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
      updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
      file_count INTEGER DEFAULT 0,
      last_analysis_id TEXT,
      config TEXT DEFAULT '{}'
    )
  `)

  db.run(`
    CREATE TABLE IF NOT EXISTS analysis_results (
      id TEXT PRIMARY KEY,
      project_id TEXT NOT NULL,
      overall_score INTEGER DEFAULT 0,
      grade TEXT DEFAULT '',
      dimensions TEXT,
      issues TEXT,
      created_at DATETIME DEFAULT CURRENT_TIMESTAMP
    )
  `)

  db.run(`
    CREATE TABLE IF NOT EXISTS uploaded_files (
      id TEXT PRIMARY KEY,
      project_id TEXT NOT NULL,
      filename TEXT NOT NULL,
      file_path TEXT NOT NULL,
      file_size INTEGER DEFAULT 0,
      upload_time DATETIME DEFAULT CURRENT_TIMESTAMP,
      analysis_status TEXT DEFAULT 'pending'
    )
  `, (err) => {
    if (err) {
      console.error('创建表失败:', err)
    } else {
      console.log('✅ 数据库表创建成功')

      // 检查并添加 config 字段到 projects 表（为了向后兼容）
      db.all(`PRAGMA table_info(projects)`, (err, columns) => {
        if (!err && columns && !columns.find(col => col.name === 'config')) {
          db.run(`ALTER TABLE projects ADD COLUMN config TEXT DEFAULT '{}'`, (alterErr) => {
            if (alterErr) {
              console.error('添加 config 字段失败:', alterErr)
            } else {
              console.log('✅ 成功添加 config 字段到 projects 表')
            }
          })
        }
      })
    }
  })
})

console.log('🗄️ 数据库初始化完成')

// 使用路由
app.use('/api', projectRoutes)
app.use('/api/admin', adminRoutes)

// 根路径
app.get('/', (req, res) => {
  res.json({
    message: '代码质量分析工具后端API服务',
    version: '1.0.0',
    endpoints: {
      projects: '/api/projects',
      admin: '/api/admin'
    }
  })
})

// 启动服务器
app.listen(PORT, () => {
  console.log(`🚀 服务器运行在端口 ${PORT}`)
})