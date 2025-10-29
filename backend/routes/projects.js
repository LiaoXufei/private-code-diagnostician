const express = require('express')
const { v4: uuidv4 } = require('uuid')
const multer = require('multer')
const path = require('path')
const fs = require('fs')

const router = express.Router()

// 配置multer存储
const storage = multer.diskStorage({
  destination: (req, file, cb) => {
    const projectId = req.params.id || 'temp'
    const uploadsDir = path.join(__dirname, '../uploads', projectId)

    // 确保上传目录存在
    if (!fs.existsSync(uploadsDir)) {
      fs.mkdirSync(uploadsDir, { recursive: true })
    }

    cb(null, uploadsDir)
  },
  filename: (req, file, cb) => {
    // 生成唯一文件名
    const uniqueSuffix = Date.now() + '-' + Math.round(Math.random() * 1E9)
    const ext = path.extname(file.originalname)
    const name = path.basename(file.originalname, ext)
    cb(null, `${name}-${uniqueSuffix}${ext}`)
  }
})

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

// 获取所有项目
router.get('/projects', (req, res) => {
  const db = req.db

  db.all('SELECT * FROM projects ORDER BY created_at DESC', (err, projects) => {
    if (err) {
      console.error('获取项目列表失败:', err)
      return res.status(500).json({ error: '获取项目列表失败' })
    }

    // 为每个项目获取最新的分析结果
    const projectsWithAnalysis = projects.map(project => {
      return new Promise((resolve) => {
        if (!project.last_analysis_id) {
          // 如果没有分析结果，直接返回项目
          resolve(project)
        } else {
          // 获取分析结果
          db.get('SELECT * FROM analysis_results WHERE id = ?', [project.last_analysis_id], (err, analysis) => {
            if (err || !analysis) {
              // 如果获取失败，返回项目但不包含分析数据
              resolve(project)
            } else {
              // 将分析结果添加到项目中
              try {
                const analysisData = {
                  overallScore: analysis.overall_score,
                  grade: analysis.grade,
                  completedAt: analysis.created_at,
                  taskId: analysis.id
                }
                resolve({
                  ...project,
                  lastAnalysis: analysisData
                })
              } catch (parseErr) {
                console.error('解析分析结果失败:', parseErr)
                resolve(project)
              }
            }
          })
        }
      })
    })

    // 等待所有项目的分析结果获取完成
    Promise.all(projectsWithAnalysis).then(results => {
      console.log('📋 获取项目列表:', results.length, '个项目')
      res.json(results)
    }).catch(err => {
      console.error('处理项目分析结果失败:', err)
      res.json(projects) // 出错时返回基本项目列表
    })
  })
})

// 创建新项目
router.post('/projects', (req, res) => {
  const db = req.db
  const { name, description, language } = req.body

  // 验证必填字段
  if (!name || !description || !language) {
    return res.status(400).json({ error: '项目名称、描述和语言为必填字段' })
  }

  const projectId = uuidv4()
  const created_at = new Date().toISOString()
  const updated_at = created_at

  db.run(
    `INSERT INTO projects (id, name, description, language, status, created_at, updated_at, file_count, last_analysis_id)
     VALUES (?, ?, ?, ?, 'active', ?, ?, 0, NULL)`,
    [projectId, name, description, language, created_at, updated_at],
    function(err) {
      if (err) {
        console.error('创建项目失败:', err)
        return res.status(500).json({ error: '创建项目失败' })
      }

      // 返回创建的项目
      db.get('SELECT * FROM projects WHERE id = ?', [projectId], (err, project) => {
        if (err) {
          console.error('获取创建的项目失败:', err)
          return res.status(500).json({ error: '创建项目失败' })
        }

        console.log('🗄️ 创建新项目:', name)
        res.json(project)
      })
    }
  )
})

// 更新项目
router.put('/projects/:id', (req, res) => {
  const db = req.db
  const { id } = req.params
  const { name, description, language, status, config } = req.body

  const updated_at = new Date().toISOString()

  // 构建动态更新查询
  let updateFields = ['updated_at = ?']
  let updateValues = [updated_at]

  // 只添加提供的字段
  if (name !== undefined) {
    updateFields.push('name = ?')
    updateValues.push(name)
  }
  if (description !== undefined) {
    updateFields.push('description = ?')
    updateValues.push(description)
  }
  if (language !== undefined) {
    updateFields.push('language = ?')
    updateValues.push(language)
  }
  if (status !== undefined) {
    updateFields.push('status = ?')
    updateValues.push(status)
  }
  if (config !== undefined) {
    updateFields.push('config = ?')
    updateValues.push(typeof config === 'string' ? config : JSON.stringify(config))
  }

  updateValues.push(id) // 添加 WHERE 条件的 id

  db.run(
    `UPDATE projects SET ${updateFields.join(', ')} WHERE id = ?`,
    updateValues,
    function(err) {
      if (err) {
        console.error('更新项目失败:', err)
        return res.status(500).json({ error: '更新项目失败' })
      }

      if (this.changes === 0) {
        return res.status(404).json({ error: '项目不存在' })
      }

      // 返回更新后的项目
      db.get('SELECT * FROM projects WHERE id = ?', [id], (err, project) => {
        if (err) {
          console.error('获取更新的项目失败:', err)
          return res.status(500).json({ error: '更新项目失败' })
        }

        console.log('🔄 更新项目:', id, name)
        res.json(project)
      })
    }
  )
})

// 删除项目
router.delete('/projects/:id', (req, res) => {
  const db = req.db
  const { id } = req.params

  // 先获取项目信息
  db.get('SELECT * FROM projects WHERE id = ?', [id], (err, project) => {
    if (err) {
      console.error('查询项目失败:', err)
      return res.status(500).json({ error: '删除项目失败' })
    }

    if (!project) {
      return res.status(404).json({ error: '项目不存在' })
    }

    // 开始事务删除项目及相关数据
    db.serialize(() => {
      // 开始事务
      db.run('BEGIN TRANSACTION', (err) => {
        if (err) {
          console.error('开始事务失败:', err)
          return res.status(500).json({ error: '删除项目失败' })
        }

        // 1. 删除相关分析结果
        db.run('DELETE FROM analysis_results WHERE project_id = ?', [id], (err) => {
          if (err) {
            console.error('删除分析结果失败:', err)
            db.run('ROLLBACK')
            return res.status(500).json({ error: '删除项目失败' })
          }

          // 2. 删除相关文件记录
          db.run('DELETE FROM uploaded_files WHERE project_id = ?', [id], (err) => {
            if (err) {
              console.error('删除文件记录失败:', err)
              db.run('ROLLBACK')
              return res.status(500).json({ error: '删除项目失败' })
            }

            // 3. 删除项目记录
            db.run('DELETE FROM projects WHERE id = ?', [id], function(err) {
              if (err) {
                console.error('删除项目失败:', err)
                db.run('ROLLBACK')
                return res.status(500).json({ error: '删除项目失败' })
              }

              // 提交事务
              db.run('COMMIT', (err) => {
                if (err) {
                  console.error('提交事务失败:', err)
                  return res.status(500).json({ error: '删除项目失败' })
                }

                // 删除物理文件
                const projectDir = path.join(__dirname, '../uploads', id)
                try {
                  if (fs.existsSync(projectDir)) {
                    fs.rmSync(projectDir, { recursive: true, force: true })
                    console.log('🗑️ 删除项目目录:', projectDir)
                  } else {
                    console.log('📁 项目目录不存在，跳过物理文件删除:', projectDir)
                  }
                } catch (fileErr) {
                  console.error('删除物理文件失败:', fileErr)
                  // 文件删除失败不影响数据库删除，但记录错误
                }

                console.log('🗑️ 删除项目:', id, project.name)

                // 返回剩余项目
                db.all('SELECT * FROM projects ORDER BY created_at DESC', (err, remainingProjects) => {
                  if (err) {
                    console.error('获取剩余项目失败:', err)
                  }

                  res.json({
                    message: '项目删除成功',
                    deletedProject: project,
                    remainingProjects: remainingProjects || []
                  })
                })
              })
            })
          })
        })
      })
    })
  })
})

// 文件上传API - FormData格式
router.post('/projects/:id/upload', upload.array('files'), (req, res) => {
  const db = req.db
  const { id } = req.params
  const files = req.files

  if (!files || files.length === 0) {
    return res.status(400).json({ error: '没有上传文件' })
  }

  const uploadTime = new Date().toISOString()
  const uploadedFiles = []
  let totalProcessed = 0
  let errors = []

  // 处理每个上传的文件
  files.forEach((file, index) => {
    const fileId = uuidv4()
    const filePath = `/uploads/${id}/${file.filename}`

    db.run(
      `INSERT INTO uploaded_files (id, project_id, filename, file_path, file_size, upload_time, analysis_status)
       VALUES (?, ?, ?, ?, ?, ?, 'pending')`,
      [fileId, id, file.originalname, filePath, file.size, uploadTime],
      function(err) {
        totalProcessed++

        if (err) {
          console.error(`文件 ${file.originalname} 上传失败:`, err)
          errors.push({ filename: file.originalname, error: err.message })
        } else {
          uploadedFiles.push({
            id: fileId,
            filename: file.originalname,
            file_path: filePath,
            file_size: file.size,
            upload_time: uploadTime
          })
          console.log(`📁 文件上传成功: ${file.originalname}`)
        }

        // 所有文件处理完成
        if (totalProcessed === files.length) {
          // 更新项目文件计数
          db.run(
            `UPDATE projects SET file_count = file_count + ?, updated_at = ? WHERE id = ?`,
            [uploadedFiles.length, uploadTime, id],
            (err) => {
              if (err) {
                console.error('更新文件计数失败:', err)
              }

              // 返回结果
              res.json({
                message: `成功上传 ${uploadedFiles.length} 个文件`,
                uploadedFiles,
                errors,
                totalFiles: files.length
              })
            }
          )
        }
      }
    )
  })
})

// 保留原有的JSON格式文件上传API（向后兼容）
router.post('/projects/:id/files', (req, res) => {
  const db = req.db
  const { id } = req.params
  const { filename, fileContent, fileSize } = req.body

  // 验证必填字段
  if (!filename || !fileContent) {
    return res.status(400).json({ error: '文件名和内容为必填字段' })
  }

  const fileId = uuidv4()
  const uploadTime = new Date().toISOString()
  const filePath = `/uploads/${id}/${filename}`

  db.run(
    `INSERT INTO uploaded_files (id, project_id, filename, file_path, file_size, upload_time, analysis_status)
     VALUES (?, ?, ?, ?, ?, ?, 'pending')`,
    [fileId, id, filename, filePath, fileSize || 0, uploadTime],
    function(err) {
      if (err) {
        console.error('文件上传失败:', err)
        return res.status(500).json({ error: '文件上传失败' })
      }

      // 更新项目文件计数
      db.run(
        `UPDATE projects SET file_count = file_count + 1, updated_at = ? WHERE id = ?`,
        [uploadTime, id],
        (err) => {
          if (err) {
            console.error('更新文件计数失败:', err)
          }
        }
      )

      // 返回文件信息
      db.get('SELECT * FROM uploaded_files WHERE id = ?', [fileId], (err, fileInfo) => {
        if (err) {
          console.error('获取文件信息失败:', err)
          return res.status(500).json({ error: '文件上传失败' })
        }

        console.log('📁 文件上传成功:', filename)
        res.json({
          message: '文件上传成功',
          fileInfo
        })
      })
    }
  )
})

// 获取单个项目详情
router.get('/projects/:id', (req, res) => {
  const db = req.db
  const { id } = req.params

  db.get('SELECT * FROM projects WHERE id = ?', [id], (err, project) => {
    if (err) {
      console.error('获取项目详情失败:', err)
      return res.status(500).json({ error: '获取项目详情失败' })
    }

    if (!project) {
      return res.status(404).json({ error: '项目不存在' })
    }

    // 获取最新的分析结果
    db.get('SELECT * FROM analysis_results WHERE id = ?', [project.last_analysis_id], (err, analysis) => {
      if (err || !analysis) {
        // 如果获取失败或没有分析结果，返回基本项目信息
        return res.json(project)
      }

      // 将分析结果添加到项目中
      try {
        const analysisData = {
          overallScore: analysis.overall_score,
          grade: analysis.grade,
          completedAt: analysis.created_at,
          taskId: analysis.id
        }
        res.json({
          ...project,
          lastAnalysis: analysisData
        })
      } catch (parseErr) {
        console.error('解析分析结果失败:', parseErr)
        res.json(project)
      }
    })
  })
})

// 获取项目文件列表
router.get('/projects/:id/files', (req, res) => {
  const db = req.db
  const { id } = req.params

  db.all('SELECT * FROM uploaded_files WHERE project_id = ? ORDER BY upload_time DESC', [id], (err, files) => {
    if (err) {
      console.error('获取文件列表失败:', err)
      return res.status(500).json({ error: '获取文件列表失败' })
    }

    const totalFiles = files.length
    const totalSize = files.reduce((sum, file) => sum + (file.file_size || 0), 0)

    // 同步项目的文件计数
    db.run(
      `UPDATE projects SET file_count = ?, updated_at = ? WHERE id = ?`,
      [totalFiles, new Date().toISOString(), id],
      (syncErr) => {
        if (syncErr) {
          console.error('同步文件计数失败:', syncErr)
        }
      }
    )

    res.json({
      files,
      totalFiles,
      totalSize
    })
  })
})

// 删除文件
router.delete('/projects/:id/files/:fileId', (req, res) => {
  const db = req.db
  const { id, fileId } = req.params

  // 先获取文件信息，以便删除物理文件
  db.get('SELECT * FROM uploaded_files WHERE id = ? AND project_id = ?', [fileId, id], (err, file) => {
    if (err) {
      console.error('查询文件失败:', err)
      return res.status(500).json({ error: '删除文件失败' })
    }

    if (!file) {
      return res.status(404).json({ error: '文件不存在' })
    }

    // 开始事务删除文件
    db.serialize(() => {
      // 开始事务
      db.run('BEGIN TRANSACTION', (err) => {
        if (err) {
          console.error('开始事务失败:', err)
          return res.status(500).json({ error: '删除文件失败' })
        }

        // 1. 删除文件记录
        db.run('DELETE FROM uploaded_files WHERE id = ? AND project_id = ?', [fileId, id], function(err) {
          if (err) {
            console.error('删除文件记录失败:', err)
            db.run('ROLLBACK')
            return res.status(500).json({ error: '删除文件失败' })
          }

          // 2. 更新项目文件计数（确保不为负数）
          db.run(
            `UPDATE projects SET file_count = CASE
              WHEN file_count > 0 THEN file_count - 1
              ELSE 0
            END, updated_at = ? WHERE id = ?`,
            [new Date().toISOString(), id],
            (err) => {
              if (err) {
                console.error('更新文件计数失败:', err)
                db.run('ROLLBACK')
                return res.status(500).json({ error: '删除文件失败' })
              }

              // 提交事务
              db.run('COMMIT', (err) => {
                if (err) {
                  console.error('提交事务失败:', err)
                  return res.status(500).json({ error: '删除文件失败' })
                }

                // 删除物理文件
                try {
                  if (fs.existsSync(file.file_path)) {
                    fs.unlinkSync(file.file_path)
                    console.log('🗑️ 删除物理文件:', file.file_path)
                  } else {
                    console.log('📁 物理文件不存在，跳过删除:', file.file_path)
                  }
                } catch (fileErr) {
                  console.error('删除物理文件失败:', fileErr)
                  // 物理文件删除失败不影响数据库操作，但记录错误
                }

                console.log('🗑️ 删除文件记录:', fileId, file.filename)
                res.json({
                  message: '文件删除成功',
                  fileId,
                  deletedFile: {
                    id: file.id,
                    filename: file.filename,
                    file_path: file.file_path
                  }
                })
              })
            })
        })
      })
    })
  })
})

// 触发分析
router.post('/projects/:id/analyze', (req, res) => {
  const db = req.db
  const { id } = req.params
  const analysisId = uuidv4()
  const created_at = new Date().toISOString()

  // 模拟分析结果 - 在实际应用中这里应该是真实的代码分析
  const mockAnalysisResult = {
    overallScore: Math.floor(Math.random() * 40) + 60, // 60-100分
    grade: 'good',
    dimensions: [
      { name: '代码复杂度', score: Math.floor(Math.random() * 30) + 70, maxScore: 100, issues: Math.floor(Math.random() * 5) },
      { name: '可维护性', score: Math.floor(Math.random() * 30) + 70, maxScore: 100, issues: Math.floor(Math.random() * 5) },
      { name: '重复度', score: Math.floor(Math.random() * 20) + 80, maxScore: 100, issues: Math.floor(Math.random() * 3) },
      { name: '安全性', score: Math.floor(Math.random() * 25) + 75, maxScore: 100, issues: Math.floor(Math.random() * 2) },
      { name: '性能', score: Math.floor(Math.random() * 35) + 65, maxScore: 100, issues: Math.floor(Math.random() * 7) },
      { name: '代码风格', score: Math.floor(Math.random() * 30) + 70, maxScore: 100, issues: Math.floor(Math.random() * 4) },
      { name: '测试覆盖', score: Math.floor(Math.random() * 40) + 60, maxScore: 100, issues: Math.floor(Math.random() * 10) }
    ],
    issues: [
      {
        id: uuidv4(),
        type: 'complexity',
        severity: 'medium',
        file: '/src/components/Example.vue',
        line: 45,
        message: '函数复杂度过高',
        suggestion: '建议拆分为多个小函数'
      },
      {
        id: uuidv4(),
        type: 'maintainability',
        severity: 'low',
        file: '/src/utils/helper.js',
        line: 23,
        message: '变量命名不规范',
        suggestion: '使用更具描述性的变量名'
      }
    ]
  }

  // 保存分析结果到数据库
  db.run(
    `INSERT INTO analysis_results (id, project_id, overall_score, grade, dimensions, issues, created_at)
     VALUES (?, ?, ?, ?, ?, ?, ?)`,
    [
      analysisId,
      id,
      mockAnalysisResult.overallScore,
      mockAnalysisResult.grade,
      JSON.stringify(mockAnalysisResult.dimensions),
      JSON.stringify(mockAnalysisResult.issues),
      created_at
    ],
    function(err) {
      if (err) {
        console.error('保存分析结果失败:', err)
        return res.status(500).json({ error: '分析失败' })
      }

      // 更新项目的最后分析ID
      db.run(
        `UPDATE projects SET last_analysis_id = ?, status = 'completed', updated_at = ? WHERE id = ?`,
        [analysisId, created_at, id],
        (err) => {
          if (err) {
            console.error('更新项目状态失败:', err)
          }
        }
      )

      console.log('🔍 开始分析项目:', id)

      // 模拟异步分析过程
      setTimeout(() => {
        console.log('✅ 分析完成:', id)
      }, 2000)

      res.json({
        message: '分析完成',
        analysisId,
        result: {
          id: analysisId,
          projectId: id,
          overallScore: mockAnalysisResult.overallScore,
          grade: mockAnalysisResult.grade,
          dimensions: mockAnalysisResult.dimensions,
          issues: mockAnalysisResult.issues,
          created_at: created_at
        }
      })
    }
  )
})

// 获取项目分析结果
router.get('/projects/:id/analysis', (req, res) => {
  const db = req.db
  const { id } = req.params

  // 获取项目信息
  db.get('SELECT * FROM projects WHERE id = ?', [id], (err, project) => {
    if (err) {
      console.error('获取项目失败:', err)
      return res.status(500).json({ error: '获取分析结果失败' })
    }

    if (!project) {
      return res.status(404).json({ error: '项目不存在' })
    }

    // 获取最新的分析结果
    db.get('SELECT * FROM analysis_results WHERE project_id = ? ORDER BY created_at DESC LIMIT 1', [id], (err, analysis) => {
      if (err) {
        console.error('获取分析结果失败:', err)
        return res.status(500).json({ error: '获取分析结果失败' })
      }

      if (!analysis) {
        return res.status(404).json({ error: '该项目暂无分析结果' })
      }

      // 解析JSON字段
      try {
        const dimensions = JSON.parse(analysis.dimensions || '[]')
        const issues = JSON.parse(analysis.issues || '[]')

        const reportData = {
          projectId: project.id,
          projectName: project.name,
          overallScore: analysis.overall_score,
          grade: analysis.grade,
          dimensions: dimensions,
          issues: issues,
          completedAt: analysis.created_at,
          taskId: analysis.id,
          metrics: {
            fileCount: project.file_count,
            totalIssues: issues.length,
            criticalIssues: issues.filter(i => i.severity === 'critical').length,
            majorIssues: issues.filter(i => i.severity === 'major').length,
            minorIssues: issues.filter(i => i.severity === 'minor').length
          },
          summary: {
            suggestions: [
              ...dimensions.filter(d => d.score < 70).map(d => `重点改善${d.name}维度`),
              ...dimensions.filter(d => d.score >= 70 && d.score < 85).map(d => `优化${d.name}相关代码`)
            ]
          }
        }

        console.log('📊 获取分析结果:', project.name, `(${analysis.overall_score}分)`)
        res.json(reportData)
      } catch (parseErr) {
        console.error('解析分析结果失败:', parseErr)
        res.status(500).json({ error: '分析结果格式错误' })
      }
    })
  })
})

// 获取分析状态
router.get('/projects/:id/analysis/status', (req, res) => {
  const db = req.db
  const { id } = req.params

  // 获取项目信息
  db.get('SELECT * FROM projects WHERE id = ?', [id], (err, project) => {
    if (err) {
      console.error('获取项目失败:', err)
      return res.status(500).json({ error: '获取分析状态失败' })
    }

    if (!project) {
      return res.status(404).json({ error: '项目不存在' })
    }

    // 返回项目状态作为分析状态
    const statusMap = {
      'active': 'idle',
      'completed': 'completed',
      'cancelled': 'cancelled',
      'analyzing': 'analyzing'
    }

    res.json({
      projectId: id,
      status: statusMap[project.status] || 'idle',
      progress: project.status === 'completed' ? 100 : 0,
      updatedAt: project.updated_at
    })
  })
})

// 获取分析日志
router.get('/projects/:id/analysis/logs', (req, res) => {
  const db = req.db
  const { id } = req.params

  // 获取项目信息
  db.get('SELECT * FROM projects WHERE id = ?', [id], (err, project) => {
    if (err) {
      console.error('获取项目失败:', err)
      return res.status(500).json({ error: '获取分析日志失败' })
    }

    if (!project) {
      return res.status(404).json({ error: '项目不存在' })
    }

    // 获取项目的分析结果历史，作为日志数据源
    db.all('SELECT * FROM analysis_results WHERE project_id = ? ORDER BY created_at DESC', [id], (err, analyses) => {
      if (err) {
        console.error('获取分析结果失败:', err)
        return res.status(500).json({ error: '获取分析日志失败' })
      }

      // 基于真实的分析结果生成日志
      const logs = []

      // 添加项目相关的日志
      logs.push({
        timestamp: project.created_at,
        level: 'info',
        message: `项目创建: ${project.name}`,
        source: 'system'
      })

      // 为每次分析生成日志条目
      analyses.forEach((analysis, index) => {
        const dimensions = JSON.parse(analysis.dimensions || '[]')

        logs.push({
          timestamp: analysis.created_at,
          level: 'info',
          message: `分析完成 #${analyses.length - index} - 总分: ${analysis.overall_score}分`,
          source: 'analyzer'
        })

        // 添加各维度分析结果
        dimensions.forEach(dimension => {
          logs.push({
            timestamp: analysis.created_at,
            level: 'debug',
            message: `${dimension.name}: ${dimension.score}分 (问题: ${dimension.issues}个)`,
            source: 'analyzer'
          })
        })
      })

      // 如果有文件，添加文件扫描日志
      if (project.file_count > 0) {
        logs.push({
          timestamp: new Date().toISOString(),
          level: 'info',
          message: `项目包含 ${project.file_count} 个文件`,
          source: 'scanner'
        })
      }

      console.log('📋 获取分析日志:', id, `(${logs.length}条记录)`)
      res.json({
        projectId: id,
        logs: logs.reverse(), // 最新的在前面
        totalLogs: logs.length
      })
    })
  })
})

// 取消分析
router.delete('/projects/:id/analyze', (req, res) => {
  const db = req.db
  const { id } = req.params

  // 获取项目信息
  db.get('SELECT * FROM projects WHERE id = ?', [id], (err, project) => {
    if (err) {
      console.error('获取项目失败:', err)
      return res.status(500).json({ error: '取消分析失败' })
    }

    if (!project) {
      return res.status(404).json({ error: '项目不存在' })
    }

    // 更新项目状态为已取消（在实际应用中可能需要更复杂的状态管理）
    db.run(
      `UPDATE projects SET status = 'cancelled', updated_at = ? WHERE id = ?`,
      [new Date().toISOString(), id],
      function(err) {
        if (err) {
          console.error('更新项目状态失败:', err)
          return res.status(500).json({ error: '取消分析失败' })
        }

        console.log('🚫 取消分析:', id, project.name)
        res.json({
          message: '分析已取消',
          projectId: id,
          cancelledAt: new Date().toISOString()
        })
      }
    )
  })
})

// 获取项目分析历史
router.get('/projects/:id/analysis/history', (req, res) => {
  const db = req.db
  const { id } = req.params

  db.all('SELECT * FROM analysis_results WHERE project_id = ? ORDER BY created_at DESC', [id], (err, analyses) => {
    if (err) {
      console.error('获取分析历史失败:', err)
      return res.status(500).json({ error: '获取分析历史失败' })
    }

    const history = analyses.map(analysis => ({
      taskId: analysis.id,
      completedAt: analysis.created_at,
      overallScore: analysis.overall_score,
      grade: analysis.grade,
      dimensions: JSON.parse(analysis.dimensions || '[]')
    }))

    console.log('📈 获取分析历史:', id, `(${history.length}条记录)`)
    res.json(history)
  })
})

module.exports = router