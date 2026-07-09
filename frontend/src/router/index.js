import { createRouter, createWebHistory } from 'vue-router'

// 路由配置
const routes = [
  {
    path: '/',
    name: 'Layout',
    component: () => import('@/components/common/AppLayout.vue'),
    redirect: '/',
    children: [
      {
        path: '',
        name: 'Home',
        component: () => import('@/views/Home.vue'),
        meta: {
          title: '项目管理',
          icon: 'HomeOutlined',
          keepAlive: false
        }
      },
      {
        path: 'project/new',
        name: 'CreateProject',
        component: () => import('@/views/ProjectConfig.vue'),
        meta: {
          title: '创建项目',
          icon: 'PlusOutlined',
          keepAlive: false,
          hidden: true
        }
      },
      {
        path: 'project/:id',
        name: 'ProjectDetail',
        redirect: to => `/project/${to.params.id}/config`,
        meta: {
          title: '项目详情',
          icon: 'FolderOutlined',
          hidden: true
        },
        children: [
          {
            path: 'config',
            name: 'ProjectConfig',
            component: () => import('@/views/ProjectConfig.vue'),
            meta: {
              title: '项目配置',
              icon: 'SettingOutlined',
              parent: 'project'
            }
          },
          {
            path: 'upload',
            name: 'CodeUpload',
            component: () => import('@/views/CodeUpload.vue'),
            meta: {
              title: '代码上传',
              icon: 'UploadOutlined',
              parent: 'project'
            }
          },
          {
            path: 'analysis',
            name: 'Analysis',
            component: () => import('@/views/Analysis.vue'),
            meta: {
              title: '代码分析',
              icon: 'ExperimentOutlined',
              parent: 'project'
            }
          },
          {
            path: 'report',
            name: 'Report',
            component: () => import('@/views/Report.vue'),
            meta: {
              title: '分析报告',
              icon: 'BarChartOutlined',
              parent: 'project'
            }
          },
          // AI 优化建议路由暂隐藏（CodeOptimize.vue 仍为占位页）
          // {
          //   path: 'optimize',
          //   name: 'CodeOptimize',
          //   component: () => import('@/views/CodeOptimize.vue'),
          //   meta: {
          //     title: 'AI优化建议',
          //     icon: 'BulbOutlined',
          //     parent: 'project'
          //   }
          // },
          {
            path: 'history',
            name: 'History',
            component: () => import('@/views/History.vue'),
            meta: {
              title: '历史记录',
              icon: 'HistoryOutlined',
              parent: 'project'
            }
          }
        ]
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/NotFound.vue'),
    meta: {
      title: '页面不存在',
      hidden: true
    }
  }
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
  scrollBehavior(to, from, savedPosition) {
    if (savedPosition) {
      return savedPosition
    } else {
      return { top: 0 }
    }
  }
})

// 全局前置守卫
router.beforeEach(async (to, from, next) => {
  // 设置页面标题
  document.title = to.meta.title
    ? `${to.meta.title} - ${import.meta.env.VITE_APP_TITLE}`
    : import.meta.env.VITE_APP_TITLE

  // 简单的页面访问统计
  if (import.meta.env.PROD && to.meta.title) {
    // 可以在这里添加访问统计逻辑
    console.log(`Page visit: ${to.meta.title}`)
  }

  next()
})

// 全局后置钩子
router.afterEach((to, from) => {
  // 页面切换完成后的逻辑
  if (import.meta.env.DEV) {
    console.log(`Navigation: ${from.path} -> ${to.path}`)
  }
})

// 路由错误处理
router.onError((error) => {
  console.error('Router error:', error)
  // 可以在这里添加错误上报逻辑
})

export default router

// 导出路由配置供其他地方使用
export { routes }