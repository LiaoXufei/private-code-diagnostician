import { createApp } from 'vue'
import { createPinia } from 'pinia'
import Antd from 'ant-design-vue'
import 'ant-design-vue/dist/reset.css'

import App from './App.vue'
import router from './router'

// 导入全局样式
import '@/styles/global.css'

const app = createApp(App)

// 注册Ant Design Vue
app.use(Antd)

// 注册路由
app.use(router)

// 注册状态管理
app.use(createPinia())

// 全局错误处理
app.config.errorHandler = (err, vm, info) => {
  console.error('Global error:', err)
  console.error('Component:', vm)
  console.error('Info:', info)

  // 可以在这里添加错误上报逻辑
  if (import.meta.env.PROD) {
    // 上报错误到监控服务
  }
}

// 注册全局属性
app.config.globalProperties.$env = import.meta.env

app.mount('#app')