/**
 * 测试数据种子脚本
 * 用于在数据库中创建两个不同分数段的测试项目
 * 高质量项目（92分）和低质量项目（58分）
 */

const sqlite3 = require('sqlite3').verbose()
const { v4: uuidv4 } = require('uuid')

// 数据库路径
const DB_PATH = './code_quality_analyzer.db'

// 高质量项目数据（92分）
const highQualityProject = {
  id: 'proj_high_quality_' + uuidv4().slice(0, 8),
  name: '电商平台前端',
  description: '一个功能完善的电商平台前端应用，使用Vue 3 + TypeScript构建，包含商品展示、购物车、订单管理等功能模块。代码质量优秀，具有良好的架构设计和测试覆盖率。',
  language: 'javascript',
  status: 'active',
  file_count: 156,
  created_at: new Date('2024-01-15T08:30:00Z').toISOString(),
  updated_at: new Date('2024-03-25T10:15:00Z').toISOString()
}

// 低质量项目数据（58分）
const lowQualityProject = {
  id: 'proj_low_quality_' + uuidv4().slice(0, 8),
  name: '学生管理系统',
  description: '一个简单的学生信息管理系统，包含学生信息录入、成绩管理、课程安排等功能。代码质量有待提升，存在较多技术债务和重构需求。',
  language: 'javascript',
  status: 'active',
  file_count: 89,
  created_at: new Date('2024-02-01T09:15:00Z').toISOString(),
  updated_at: new Date('2024-03-24T15:30:00Z').toISOString()
}

// 高质量分析结果（92分）
const highQualityAnalysis = {
  id: 'analysis_high_' + uuidv4().slice(0, 8),
  overall_score: 92,
  grade: 'excellent',
  dimensions: [
    {
      name: '代码规范',
      score: 95,
      maxScore: 100,
      issues: 8,
      weight: 0.2,
      details: {
        violations: [
          { file: 'src/utils/formatters.js', line: 23, rule: 'semi', severity: 'minor', description: '缺少分号' },
          { file: 'src/components/ProductCard.vue', line: 45, rule: 'vue/html-indent', severity: 'minor', description: '缩进不一致' }
        ]
      }
    },
    {
      name: '代码复杂度',
      score: 88,
      maxScore: 100,
      issues: 12,
      weight: 0.15,
      details: {
        cyclomaticComplexity: 3.2,
        cognitiveComplexity: 8.5,
        functions: 245,
        complexFunctions: 18
      }
    },
    {
      name: '代码重复率',
      score: 90,
      maxScore: 100,
      issues: 5,
      weight: 0.15,
      details: {
        duplicatedLines: 156,
        totalLines: 12480,
        duplicationPercentage: 1.25,
        duplicatedBlocks: 8
      }
    },
    {
      name: '测试覆盖率',
      score: 85,
      maxScore: 100,
      issues: 15,
      weight: 0.2,
      details: {
        lines: 82.5,
        functions: 87.2,
        branches: 78.9,
        statements: 84.1
      }
    },
    {
      name: '安全性',
      score: 94,
      maxScore: 100,
      issues: 3,
      weight: 0.15,
      details: {
        vulnerabilities: [],
        securityHotspots: [
          { file: 'src/api/auth.js', line: 12, type: 'weak-random', severity: 'low', description: '使用了较弱的随机数生成' }
        ]
      }
    },
    {
      name: '可维护性',
      score: 89,
      maxScore: 100,
      issues: 10,
      weight: 0.1,
      details: {
        technicalDebt: '2h 30m',
        maintainabilityRating: 'A',
        codeSmells: 10
      }
    },
    {
      name: '性能',
      score: 93,
      maxScore: 100,
      issues: 6,
      weight: 0.05,
      details: {
        inefficientCode: 6,
        performanceAntiPatterns: 2
      }
    }
  ],
  issues: [
    {
      id: 'issue_high_001',
      file: 'src/utils/formatters.js',
      line: 23,
      column: 45,
      rule: 'semi',
      severity: 'minor',
      message: 'Missing semicolon',
      description: '语句末尾缺少分号',
      suggestion: '在语句末尾添加分号',
      dimension: '代码规范'
    },
    {
      id: 'issue_high_002',
      file: 'src/components/ProductCard.vue',
      line: 156,
      column: 8,
      rule: 'complexity',
      severity: 'major',
      message: 'Function has a cognitive complexity of 12',
      description: '函数认知复杂度过高，建议拆分',
      suggestion: '将复杂逻辑拆分为多个小函数',
      dimension: '代码复杂度'
    }
  ],
  metrics: {
    totalLines: 12480,
    codeLines: 8920,
    commentLines: 2150,
    blankLines: 1410,
    functions: 245,
    classes: 42,
    files: 156
  },
  summary: {
    totalIssues: 59,
    criticalIssues: 0,
    majorIssues: 18,
    minorIssues: 41,
    suggestions: [
      '提高测试覆盖率，特别是分支覆盖率',
      '重构认知复杂度过高的函数',
      '修复代码风格问题以保持一致性'
    ]
  },
  created_at: new Date(Date.now() - 2 * 60 * 60 * 1000).toISOString() // 2小时前
}

// 低质量分析结果（58分）
const lowQualityAnalysis = {
  id: 'analysis_low_' + uuidv4().slice(0, 8),
  overall_score: 58,
  grade: 'poor',
  dimensions: [
    {
      name: '代码规范',
      score: 45,
      maxScore: 100,
      issues: 67,
      weight: 0.2,
      details: {
        violations: [
          { file: 'src/student-service.js', line: 12, rule: 'no-console', severity: 'major', description: '不应该使用console.log' },
          { file: 'src/grade-calculator.js', line: 34, rule: 'eqeqeq', severity: 'major', description: '应该使用严格相等' },
          { file: 'components/StudentForm.vue', line: 78, rule: 'vue/require-v-for-key', severity: 'major', description: 'v-for需要key属性' }
        ]
      }
    },
    {
      name: '代码复杂度',
      score: 52,
      maxScore: 100,
      issues: 34,
      weight: 0.15,
      details: {
        cyclomaticComplexity: 8.7,
        cognitiveComplexity: 15.3,
        functions: 128,
        complexFunctions: 42
      }
    },
    {
      name: '代码重复率',
      score: 38,
      maxScore: 100,
      issues: 28,
      weight: 0.15,
      details: {
        duplicatedLines: 890,
        totalLines: 5670,
        duplicationPercentage: 15.7,
        duplicatedBlocks: 23
      }
    },
    {
      name: '测试覆盖率',
      score: 25,
      maxScore: 100,
      issues: 45,
      weight: 0.2,
      details: {
        lines: 18.5,
        functions: 22.1,
        branches: 12.3,
        statements: 19.7
      }
    },
    {
      name: '安全性',
      score: 65,
      maxScore: 100,
      issues: 12,
      weight: 0.15,
      details: {
        vulnerabilities: [
          { file: 'src/auth.js', line: 45, type: 'sql-injection', severity: 'critical', description: '存在SQL注入风险' },
          { file: 'src/utils/validator.js', line: 23, type: 'xss', severity: 'high', description: '跨站脚本攻击风险' }
        ],
        securityHotspots: [
          { file: 'src/config.js', line: 8, type: 'hardcoded-credentials', severity: 'high', description: '硬编码凭证' }
        ]
      }
    },
    {
      name: '可维护性',
      score: 42,
      maxScore: 100,
      issues: 38,
      weight: 0.1,
      details: {
        technicalDebt: '18h 45m',
        maintainabilityRating: 'D',
        codeSmells: 38
      }
    },
    {
      name: '性能',
      score: 68,
      maxScore: 100,
      issues: 15,
      weight: 0.05,
      details: {
        inefficientCode: 15,
        performanceAntiPatterns: 8
      }
    }
  ],
  issues: [
    {
      id: 'issue_low_001',
      file: 'src/auth.js',
      line: 45,
      column: 12,
      rule: 'security',
      severity: 'critical',
      message: 'SQL injection vulnerability',
      description: '直接拼接SQL查询，存在注入风险',
      suggestion: '使用参数化查询或ORM',
      dimension: '安全性'
    },
    {
      id: 'issue_low_002',
      file: 'src/student-service.js',
      line: 89,
      column: 3,
      rule: 'complexity',
      severity: 'major',
      message: 'Function has a cognitive complexity of 24',
      description: '函数过于复杂，难以理解和维护',
      suggestion: '拆分为多个职责单一的小函数',
      dimension: '代码复杂度'
    },
    {
      id: 'issue_low_003',
      file: 'components/StudentList.vue',
      line: 156,
      column: 20,
      rule: 'duplication',
      severity: 'minor',
      message: 'Duplicated code block detected',
      description: '代码块重复，应该提取为公共方法',
      suggestion: '创建可复用的方法或组件',
      dimension: '代码重复率'
    }
  ],
  metrics: {
    totalLines: 5670,
    codeLines: 4230,
    commentLines: 580,
    blankLines: 860,
    functions: 128,
    classes: 18,
    files: 89
  },
  summary: {
    totalIssues: 239,
    criticalIssues: 2,
    majorIssues: 87,
    minorIssues: 150,
    suggestions: [
      '立即修复安全漏洞，特别是SQL注入风险',
      '增加单元测试以提高代码覆盖率',
      '重构高复杂度函数和重复代码',
      '改善代码规范遵循情况'
    ]
  },
  created_at: new Date(Date.now() - 24 * 60 * 60 * 1000).toISOString() // 1天前
}

// 创建数据库连接
const db = new sqlite3.Database(DB_PATH, (err) => {
  if (err) {
    console.error('❌ 连接数据库失败:', err.message)
    process.exit(1)
  }
  console.log('✅ 成功连接到数据库')
})

// 插入测试数据
async function seedTestData() {
  return new Promise((resolve, reject) => {
    db.serialize(() => {
      console.log('🌱 开始插入测试数据...')

      // 插入高质量项目
      db.run(`
        INSERT OR REPLACE INTO projects (id, name, description, language, status, created_at, updated_at, file_count, last_analysis_id)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
      `, [
        highQualityProject.id,
        highQualityProject.name,
        highQualityProject.description,
        highQualityProject.language,
        highQualityProject.status,
        highQualityProject.created_at,
        highQualityProject.updated_at,
        highQualityProject.file_count,
        highQualityAnalysis.id
      ], function(err) {
        if (err) {
          console.error('❌ 插入高质量项目失败:', err.message)
          return reject(err)
        }
        console.log('✅ 高质量项目插入成功:', highQualityProject.name)
      })

      // 插入高质量分析结果
      db.run(`
        INSERT OR REPLACE INTO analysis_results (id, project_id, overall_score, grade, dimensions, issues, created_at)
        VALUES (?, ?, ?, ?, ?, ?, ?)
      `, [
        highQualityAnalysis.id,
        highQualityProject.id,
        highQualityAnalysis.overall_score,
        highQualityAnalysis.grade,
        JSON.stringify(highQualityAnalysis.dimensions),
        JSON.stringify(highQualityAnalysis.issues),
        highQualityAnalysis.created_at
      ], function(err) {
        if (err) {
          console.error('❌ 插入高质量分析结果失败:', err.message)
          return reject(err)
        }
        console.log('✅ 高质量分析结果插入成功 (92分)')
      })

      // 插入低质量项目
      db.run(`
        INSERT OR REPLACE INTO projects (id, name, description, language, status, created_at, updated_at, file_count, last_analysis_id)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
      `, [
        lowQualityProject.id,
        lowQualityProject.name,
        lowQualityProject.description,
        lowQualityProject.language,
        lowQualityProject.status,
        lowQualityProject.created_at,
        lowQualityProject.updated_at,
        lowQualityProject.file_count,
        lowQualityAnalysis.id
      ], function(err) {
        if (err) {
          console.error('❌ 插入低质量项目失败:', err.message)
          return reject(err)
        }
        console.log('✅ 低质量项目插入成功:', lowQualityProject.name)
      })

      // 插入低质量分析结果
      db.run(`
        INSERT OR REPLACE INTO analysis_results (id, project_id, overall_score, grade, dimensions, issues, created_at)
        VALUES (?, ?, ?, ?, ?, ?, ?)
      `, [
        lowQualityAnalysis.id,
        lowQualityProject.id,
        lowQualityAnalysis.overall_score,
        lowQualityAnalysis.grade,
        JSON.stringify(lowQualityAnalysis.dimensions),
        JSON.stringify(lowQualityAnalysis.issues),
        lowQualityAnalysis.created_at
      ], function(err) {
        if (err) {
          console.error('❌ 插入低质量分析结果失败:', err.message)
          return reject(err)
        }
        console.log('✅ 低质量分析结果插入成功 (58分)')
      })

      // 添加一些示例文件记录
      const sampleFiles = [
        // 高质量项目文件
        { id: uuidv4(), project_id: highQualityProject.id, filename: 'src/main.js', file_path: '/uploads/' + highQualityProject.id + '/src/main.js', file_size: 2456 },
        { id: uuidv4(), project_id: highQualityProject.id, filename: 'src/App.vue', file_path: '/uploads/' + highQualityProject.id + '/src/App.vue', file_size: 3892 },
        { id: uuidv4(), project_id: highQualityProject.id, filename: 'src/components/ProductList.vue', file_path: '/uploads/' + highQualityProject.id + '/src/components/ProductList.vue', file_size: 5634 },
        { id: uuidv4(), project_id: highQualityProject.id, filename: 'src/utils/formatters.js', file_path: '/uploads/' + highQualityProject.id + '/src/utils/formatters.js', file_size: 1234 },
        { id: uuidv4(), project_id: highQualityProject.id, filename: 'src/api/products.js', file_path: '/uploads/' + highQualityProject.id + '/src/api/products.js', file_size: 2876 },

        // 低质量项目文件
        { id: uuidv4(), project_id: lowQualityProject.id, filename: 'src/main.js', file_path: '/uploads/' + lowQualityProject.id + '/src/main.js', file_size: 1823 },
        { id: uuidv4(), project_id: lowQualityProject.id, filename: 'src/App.vue', file_path: '/uploads/' + lowQualityProject.id + '/src/App.vue', file_size: 2156 },
        { id: uuidv4(), project_id: lowQualityProject.id, filename: 'src/student-service.js', file_path: '/uploads/' + lowQualityProject.id + '/src/student-service.js', file_size: 3456 },
        { id: uuidv4(), project_id: lowQualityProject.id, filename: 'src/auth.js', file_path: '/uploads/' + lowQualityProject.id + '/src/auth.js', file_size: 1567 },
        { id: uuidv4(), project_id: lowQualityProject.id, filename: 'components/StudentForm.vue', file_path: '/uploads/' + lowQualityProject.id + '/components/StudentForm.vue', file_size: 4234 }
      ]

      sampleFiles.forEach((file, index) => {
        db.run(`
          INSERT OR REPLACE INTO uploaded_files (id, project_id, filename, file_path, file_size, upload_time, analysis_status)
          VALUES (?, ?, ?, ?, ?, ?, 'completed')
        `, [
          file.id,
          file.project_id,
          file.filename,
          file.file_path,
          file.file_size,
          new Date().toISOString()
        ], function(err) {
          if (err) {
            console.error(`❌ 插入文件记录失败 (${file.filename}):`, err.message)
          } else if (index === sampleFiles.length - 1) {
            console.log('✅ 所有示例文件记录插入成功')
            console.log('\n🎉 测试数据插入完成!')
            console.log('📊 数据摘要:')
            console.log(`   - 高质量项目: ${highQualityProject.name} (92分)`)
            console.log(`   - 低质量项目: ${lowQualityProject.name} (58分)`)
            console.log(`   - 总文件数: ${sampleFiles.length}`)
            console.log('\n💡 现在您可以在前端页面中查看这些测试项目了!')
            resolve()
          }
        })
      })
    })
  })
}

// 执行数据插入
seedTestData()
  .then(() => {
    db.close((err) => {
      if (err) {
        console.error('❌ 关闭数据库连接失败:', err.message)
      } else {
        console.log('✅ 数据库连接已关闭')
      }
      process.exit(0)
    })
  })
  .catch((err) => {
    console.error('❌ 测试数据插入失败:', err.message)
    db.close()
    process.exit(1)
  })