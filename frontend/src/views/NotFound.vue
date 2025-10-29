<template>
  <div class="not-found-page">
    <div class="error-content">
      <div class="error-icon">
        <ExclamationCircleOutlined />
      </div>
      <h1 class="error-title">404</h1>
      <h2 class="error-subtitle">页面不存在</h2>
      <p class="error-description">
        抱歉，您访问的页面不存在或已被移除
      </p>
      <div class="error-actions">
        <a-space>
          <a-button type="primary" @click="goHome">
            <HomeOutlined />
            返回首页
          </a-button>
          <a-button @click="goBack">
            <ArrowLeftOutlined />
            返回上页
          </a-button>
        </a-space>
      </div>
    </div>

    <!-- 建议链接 -->
    <div class="suggestions">
      <a-card title="您可能在寻找">
        <div class="suggestion-links">
          <router-link to="/" class="suggestion-link">
            <FolderOutlined />
            项目管理
          </router-link>
          <router-link to="/project/new" class="suggestion-link">
            <PlusOutlined />
            创建项目
          </router-link>
        </div>
      </a-card>
    </div>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import {
  ExclamationCircleOutlined,
  HomeOutlined,
  ArrowLeftOutlined,
  FolderOutlined,
  PlusOutlined
} from '@ant-design/icons-vue'

const router = useRouter()

const goHome = () => {
  router.push('/')
}

const goBack = () => {
  router.go(-1)
}
</script>

<style scoped>
.not-found-page {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 60vh;
  text-align: center;
  padding: 40px 20px;
}

.error-content {
  margin-bottom: 40px;
}

.error-icon {
  font-size: 64px;
  color: #1890ff;
  margin-bottom: 20px;
}

.error-title {
  font-size: 72px;
  font-weight: 700;
  color: #262626;
  margin: 0 0 16px 0;
  line-height: 1;
}

.error-subtitle {
  font-size: 24px;
  font-weight: 500;
  color: #595959;
  margin: 0 0 16px 0;
}

.error-description {
  font-size: 16px;
  color: #8c8c8c;
  margin: 0 0 32px 0;
  max-width: 400px;
}

.error-actions {
  display: flex;
  justify-content: center;
}

.suggestions {
  max-width: 400px;
  width: 100%;
}

.suggestion-links {
  display: flex;
  gap: 16px;
  justify-content: center;
  flex-wrap: wrap;
}

.suggestion-link {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 20px;
  background: #f5f5f5;
  border-radius: 6px;
  text-decoration: none;
  color: #262626;
  transition: all 0.3s ease;
}

.suggestion-link:hover {
  background: #1890ff;
  color: #fff;
  transform: translateY(-2px);
}

/* 响应式设计 */
@media (max-width: 576px) {
  .error-icon {
    font-size: 48px;
  }

  .error-title {
    font-size: 56px;
  }

  .error-subtitle {
    font-size: 20px;
  }

  .suggestion-links {
    flex-direction: column;
    align-items: center;
  }

  .suggestion-link {
    width: 200px;
    justify-content: center;
  }
}
</style>