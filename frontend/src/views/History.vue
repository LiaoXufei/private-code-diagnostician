<template>
  <div class="history-page">
    <div class="page-header">
      <h1 class="page-title">
        <HistoryOutlined />
        历史记录
      </h1>
      <p class="page-description">
        查看项目的历史分析记录和对比结果
      </p>
    </div>

    <div class="content-container">
      <a-empty description="历史记录页面开发中..." />
    </div>
  </div>
</template>

<script setup>
import { HistoryOutlined } from '@ant-design/icons-vue'
</script>

<style scoped>
.history-page {
  max-width: 1200px;
  margin: 0 auto;
  text-align: center;
  padding: 100px 20px;
}
</style>