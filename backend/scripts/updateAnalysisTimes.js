const sqlite3 = require('sqlite3').verbose()
const { v4: uuidv4 } = require('uuid')
const path = require('path')

const dbPath = path.join(__dirname, '..', 'database.sqlite')
const db = new sqlite3.Database(dbPath)

// 更新分析时间为更近期的时间
const updateAnalysisTimes = () => {
  return new Promise((resolve, reject) => {
    console.log('🔄 更新分析时间为更近期的时间...')

    const now = new Date()
    const oneDayAgo = new Date(now.getTime() - 24 * 60 * 60 * 1000) // 1天前
    const threeDaysAgo = new Date(now.getTime() - 3 * 24 * 60 * 60 * 1000) // 3天前
    const oneWeekAgo = new Date(now.getTime() - 7 * 24 * 60 * 60 * 1000) // 1周前

    db.serialize(() => {
      // 获取所有分析结果
      db.all('SELECT * FROM analysis_results ORDER BY created_at DESC', (err, analyses) => {
        if (err) {
          console.error('❌ 获取分析结果失败:', err.message)
          return reject(err)
        }

        if (analyses.length === 0) {
          console.log('⚠️  没有找到分析结果，跳过时间更新')
          return resolve()
        }

        console.log(`📊 找到 ${analyses.length} 个分析结果`)

        // 更新每个分析结果的时间为更近期的时间
        let updatedCount = 0
        analyses.forEach((analysis, index) => {
          let newTime

          // 为不同的分析结果设置不同的时间，让时间显示更有层次
          if (index === 0) {
            // 最新的分析结果：几小时前
            newTime = new Date(now.getTime() - 2 * 60 * 60 * 1000).toISOString() // 2小时前
          } else if (index === 1) {
            // 第二新的：1天前
            newTime = oneDayAgo.toISOString()
          } else if (index === 2) {
            // 第三新的：3天前
            newTime = threeDaysAgo.toISOString()
          } else {
            // 其他的：1周前
            newTime = oneWeekAgo.toISOString()
          }

          db.run(
            'UPDATE analysis_results SET created_at = ? WHERE id = ?',
            [newTime, analysis.id],
            function(err) {
              if (err) {
                console.error(`❌ 更新分析时间失败 (${analysis.id}):`, err.message)
              } else {
                updatedCount++
                console.log(`✅ 分析结果时间已更新: ${analysis.id} -> ${newTime}`)
              }

              // 所有更新完成后
              if (updatedCount === analyses.length) {
                console.log(`🎉 所有 ${updatedCount} 个分析结果时间更新完成！`)
                console.log('📅 时间分布:')
                console.log('   - 最新分析: 2小时前')
                console.log('   - 次新分析: 1天前')
                console.log('   - 较旧分析: 3天前')
                console.log('   - 最旧分析: 1周前')
                resolve()
              }
            }
          )
        })
      })
    })
  })
}

// 执行更新
updateAnalysisTimes()
  .then(() => {
    console.log('✅ 分析时间更新完成')
    db.close()
  })
  .catch((err) => {
    console.error('❌ 更新失败:', err)
    db.close()
    process.exit(1)
  })