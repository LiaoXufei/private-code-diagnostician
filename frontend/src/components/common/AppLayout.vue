<template>
  <div class="app-layout">
    <!-- 顶部导航栏 -->
    <a-layout-header class="app-header">
      <div class="header-content">
        <div class="header-left">
          <router-link to="/" class="logo-link">
            <div class="logo">
              <ExperimentOutlined class="logo-icon" />
              <span class="logo-text">代码质量分析工具</span>
            </div>
          </router-link>
        </div>

        <div class="header-center">
          <a-breadcrumb>
            <a-breadcrumb-item>
              <router-link to="/">首页</router-link>
            </a-breadcrumb-item>
            <a-breadcrumb-item v-if="breadcrumbItems.length > 0">
              {{ breadcrumbItems[breadcrumbItems.length - 1] }}
            </a-breadcrumb-item>
          </a-breadcrumb>
        </div>

        <div class="header-right">
          <a-space>
            <a-tooltip title="项目列表">
              <router-link to="/">
                <a-button type="text" size="small">
                  <HomeOutlined />
                </a-button>
              </router-link>
            </a-tooltip>

            <a-tooltip title="创建新项目">
              <router-link to="/project/new">
                <a-button type="primary" size="small">
                  <PlusOutlined />
                  新建项目
                </a-button>
              </router-link>
            </a-tooltip>

            <a-divider type="vertical" />

            <a-dropdown>
              <a-button type="text" size="small">
                <SettingOutlined />
                <DownOutlined />
              </a-button>
              <template #overlay>
                <a-menu>
                  <a-menu-item key="settings">
                    <SettingOutlined />
                    系统设置
                  </a-menu-item>
                  <a-menu-item key="about">
                    <InfoCircleOutlined />
                    关于
                  </a-menu-item>
                  <a-menu-divider />
                  <a-menu-item key="help">
                    <QuestionCircleOutlined />
                    帮助文档
                  </a-menu-item>
                </a-menu>
              </template>
            </a-dropdown>
          </a-space>
        </div>
      </div>
    </a-layout-header>

    <!-- 主要内容区域 -->
    <a-layout-content class="app-content">
      <div class="content-wrapper">
        <router-view v-slot="{ Component, route }">
          <!-- 页面过渡动画 -->
          <transition name="fade" mode="out-in">
            <keep-alive v-if="route.meta?.keepAlive">
              <component :is="Component" :key="route.path" />
            </keep-alive>
            <component v-else :is="Component" :key="route.path" />
          </transition>
        </router-view>
      </div>
    </a-layout-content>

    <!-- 页脚 -->
    <a-layout-footer class="app-footer">
      <div class="footer-content">
        <span class="copyright">
          © {{ currentYear }} 代码质量分析工具 - 专业的代码质量检测与优化工具
        </span>
        <a-space class="footer-links">
          <a href="javascript:void(0)" onclick="alert('帮助文档开发中...')">帮助文档</a>
          <a-divider type="vertical" />
          <a href="javascript:void(0)" onclick="alert('API文档开发中...')">API文档</a>
          <a-divider type="vertical" />
          <a href="javascript:void(0)" onclick="alert('问题反馈开发中...')">问题反馈</a>
        </a-space>
      </div>
    </a-layout-footer>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import {
  ExperimentOutlined,
  HomeOutlined,
  PlusOutlined,
  SettingOutlined,
  DownOutlined,
  InfoCircleOutlined,
  QuestionCircleOutlined
} from '@ant-design/icons-vue'

const route = useRoute()

// 当前年份
const currentYear = computed(() => new Date().getFullYear())

// 面包屑导航
const breadcrumbItems = computed(() => {
  const items = []

  if (route.meta?.title && route.meta.title !== '项目管理') {
    items.push(route.meta.title)
  }

  // 如果是项目相关页面，添加项目名称
  if (route.params.id && route.meta?.parent === 'project') {
    // 这里可以根据项目ID获取项目名称
    items.unshift('项目详情')
  }

  return items
})
</script>

<style scoped>
.app-layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.app-header {
  background: #fff;
  border-bottom: 1px solid #f0f0f0;
  box-shadow: 0 1px 4px rgba(0, 21, 41, 0.08);
  padding: 0;
  position: sticky;
  top: 0;
  z-index: 1000;
}

.header-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 64px;
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 24px;
}

.header-left {
  flex: 1;
}

.logo-link {
  text-decoration: none;
  color: inherit;
}

.logo {
  display: flex;
  align-items: center;
  font-size: 18px;
  font-weight: 600;
  color: #1890ff;
}

.logo-icon {
  font-size: 24px;
  margin-right: 8px;
}

.logo-text {
  background: linear-gradient(135deg, #1890ff 0%, #722ed1 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.header-center {
  flex: 2;
  display: flex;
  justify-content: center;
}

.header-right {
  flex: 1;
  display: flex;
  justify-content: flex-end;
}

.app-content {
  flex: 1;
  background: #f0f2f5;
}

.content-wrapper {
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px;
  min-height: calc(100vh - 64px - 70px);
}

.app-footer {
  background: #fff;
  border-top: 1px solid #f0f0f0;
  padding: 16px 0;
}

.footer-content {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.copyright {
  color: #8c8c8c;
  font-size: 12px;
}

.footer-links {
  color: #8c8c8c;
}

.footer-links a {
  color: #8c8c8c;
  font-size: 12px;
  text-decoration: none;
}

.footer-links a:hover {
  color: #1890ff;
}

/* 页面过渡动画 */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .header-content {
    padding: 0 16px;
  }

  .header-center {
    display: none;
  }

  .content-wrapper {
    padding: 16px;
  }

  .footer-content {
    flex-direction: column;
    text-align: center;
    gap: 8px;
  }
}

@media (max-width: 576px) {
  .header-content {
    padding: 0 12px;
  }

  .logo-text {
    display: none;
  }

  .content-wrapper {
    padding: 12px;
  }

  .footer-content {
    padding: 0 12px;
  }
}
</style>