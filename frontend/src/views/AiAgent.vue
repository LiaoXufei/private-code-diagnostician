<template>
  <div class="ai-agent-page">
    <div class="page-header">
      <h1 class="page-title">
        <RobotOutlined />
        AI Agent 自动诊断
      </h1>
      <p class="page-description">
        Agent 自主调用工具读取文件、运行度量指标，生成诊断报告（ReAct + Tool Calling）
      </p>
    </div>

    <div class="content-container">
      <a-card title="诊断任务">
        <a-alert
          type="info"
          show-icon
          style="margin-bottom: 16px"
          message="Agent 将自动调用：文件列表、分析摘要、静态度量、问题清单等工具"
        />

        <a-textarea
          v-model:value="prompt"
          :rows="3"
          placeholder="可选：补充诊断要求，例如「重点关注圈复杂度和命名规范」"
        />

        <a-button
          type="primary"
          :loading="running"
          style="margin-top: 12px"
          @click="runDiagnosis"
        >
          <ThunderboltOutlined />
          启动 Agent 诊断
        </a-button>
      </a-card>

      <a-card v-if="report" title="诊断报告" style="margin-top: 16px">
        <div class="report-content">{{ report }}</div>
      </a-card>

      <a-card v-if="toolTrace.length" title="工具调用轨迹" style="margin-top: 16px">
        <a-timeline>
          <a-timeline-item v-for="(item, idx) in toolTrace" :key="idx" color="blue">
            {{ item }}
          </a-timeline-item>
        </a-timeline>
      </a-card>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRoute } from 'vue-router'
import { message } from 'ant-design-vue'
import { RobotOutlined, ThunderboltOutlined } from '@ant-design/icons-vue'
import { aiApi } from '@/api/ai'

const route = useRoute()
const projectId = computed(() => route.params.id)

const prompt = ref('')
const running = ref(false)
const report = ref('')
const toolTrace = ref([])

async function runDiagnosis() {
  if (running.value) return
  running.value = true
  report.value = ''
  toolTrace.value = ['Agent 启动，正在编排工具调用...']

  try {
    const { done } = aiApi.streamAgent(projectId.value, prompt.value, {
      onToken: (t) => {
        report.value += t
        if (t.includes('分析') || t.includes('文件') || t.includes('问题')) {
          toolTrace.value.push(`[推理] ${t.slice(0, 80)}...`)
        }
      }
    })
    await done
    toolTrace.value.push('[完成] 诊断报告已生成')
  } catch (e) {
    message.error('Agent 诊断失败: ' + (e.message || '请检查 AI API 配置'))
    toolTrace.value.push('[错误] ' + (e.message || '未知错误'))
  } finally {
    running.value = false
  }
}
</script>

<style scoped>
.ai-agent-page {
  max-width: 1000px;
  margin: 0 auto;
  padding: 24px 20px;
}

.page-title {
  font-size: 24px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.report-content {
  white-space: pre-wrap;
  line-height: 1.8;
  font-size: 14px;
  min-height: 200px;
}
</style>
