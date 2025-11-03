const express = require('express')
const { v4: uuidv4 } = require('uuid')

const router = express.Router()

// 获取所有项目（管理员专用）
router.get('/projects', (req, res) => {
  const db = req.db

  db.all('SELECT * FROM projects ORDER BY created_at DESC', (err, projects) => {
    if (err) {
      console.error('获取项目列表失败:', err)
      return res.status(500).json({ error: '获取项目列表失败' })
    }

    console.log('👨 管理员获取所有项目')
    res.json(projects)
  })
})

// 获取项目详情（管理员专用）
router.get('/projects/:id', (req, res) => {
  const { id } = req.params
  const db = req.db

  db.get('SELECT * FROM projects WHERE id = ?', [id], (err, project) => {
    if (err) {
      console.error('获取项目详情失败:', err)
      return res.status(500).json({ error: '获取项目详情失败' })
    }

    if (!project) {
      return res.status(404).json({ error: '项目不存在' })
    }

    console.log('👨 管理员查看项目:', id, project.name)
    res.json(project)
  })
})

// 删除项目（管理员专用）
router.delete('/projects/:id', (req, res) => {
  const { id } = req.params
  const { hardDelete = 'false' } = req.body
  const db = req.db

  // 先获取项目信息
  db.get('SELECT * FROM projects WHERE id = ?', [id], (err, project) => {
    if (err) {
      console.error('查询项目失败:', err)
      return res.status(500).json({ error: '删除项目失败' })
    }

    if (!project) {
      return res.status(404).json({ error: '项目不存在' })
    }

    // 删除项目
    db.run('DELETE FROM projects WHERE id = ?', [id], function(err) {
      if (err) {
        console.error('删除项目失败:', err)
        return res.status(500).json({ error: '删除项目失败' })
      }

      console.log('🗑️ 管理员删除项目:', id, project.name)

      // 如果是硬删除，同时删除相关文件记录
      if (hardDelete === 'true') {
        db.run('DELETE FROM uploaded_files WHERE project_id = ?', [id], (err) => {
          if (err) {
            console.error('删除文件记录失败:', err)
          } else {
            console.log('🗑️ 硬删除项目（包含文件）:', id, project.name)
          }
        })
      }

      // 返回剩余项目
      db.all('SELECT * FROM projects ORDER BY created_at DESC', (err, remainingProjects) => {
        if (err) {
          console.error('获取剩余项目失败:', err)
        }

        res.json({
          message: hardDelete === 'true' ? '项目及其所有文件已永久删除' : '项目已删除',
          deletedProject: project,
          remainingProjects: remainingProjects || []
        })
      })
    })
  })
})

module.exports = router