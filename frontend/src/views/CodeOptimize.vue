<template>
  <div class="code-optimize-page">
    <div class="page-header">
      <h1 class="page-title">
        <BulbOutlined />
        AI 优化建议
      </h1>
      <p class="page-description">
        基于静态分析结果，由大模型生成可执行的优化方案与修复代码
      </p>
    </div>

    <div class="content-container">
      <a-alert
        v-if="aiStatus && !aiStatus.enabled"
        type="error"
        show-icon
        message="AI 功能未启用"
        description="后端 app.ai.enabled=false，请联系管理员开启"
        style="margin-bottom: 16px"
      />
      <a-alert
        v-if="apiKeyMissing"
        type="warning"
        show-icon
        message="未检测到有效的 LLM API Key"
        :description="apiKeyHint"
        style="margin-bottom: 16px"
      />
      <a-row :gutter="24">
        <a-col :span="10">
          <a-card title="分析问题" class="issues-panel">
            <template #extra>
              <a-button size="small" :loading="summaryLoading" :disabled="aiStreaming && !summaryLoading" @click="generateSummary">
                生成摘要
              </a-button>
            </template>

            <a-spin :spinning="issuesLoading">
              <div v-if="issues.length === 0" class="empty-box">
                <a-empty description="暂无分析问题，请先完成代码分析" />
              </div>
              <div
                v-for="issue in issues"
                :key="issue.id"
                class="issue-card"
                :class="{ active: selectedIssue?.id === issue.id }"
                @click="selectIssue(issue)"
              >
                <div class="issue-top">
                  <a-tag :color="severityColor(issue.severity)">{{ issue.severity }}</a-tag>
                  <span class="issue-msg">{{ issue.message }}</span>
                </div>
                <div class="issue-meta">{{ issue.file }}:{{ issue.line }}</div>
              </div>
            </a-spin>
          </a-card>
        </a-col>

        <a-col :span="14">
          <a-card title="AI 建议" class="result-panel">
            <template #extra>
              <a-space>
                <a-button
                  type="primary"
                  size="small"
                  :disabled="!selectedIssue || (aiStreaming && !optimizeLoading)"
                  :loading="optimizeLoading"
                  @click="generateOptimize"
                >
                  优化建议
                </a-button>
                <a-button
                  size="small"
                  :disabled="!selectedIssue || (aiStreaming && !fixLoading)"
                  :loading="fixLoading"
                  @click="generateFix"
                >
                  生成修复
                </a-button>
              </a-space>
            </template>

            <a-spin :spinning="optimizeLoading || fixLoading || summaryLoading">
              <div v-if="!aiContent" class="empty-box">
                <a-empty description="选择问题后点击「优化建议」或「生成修复」" />
              </div>
              <div v-else class="ai-content markdown-body" v-html="renderedContent"></div>
            </a-spin>
          </a-card>

          <a-card v-if="showDiff" title="代码对比" class="diff-panel" style="margin-top: 16px">
            <div class="diff-container">
              <div class="diff-side">
                <div class="diff-label">原始代码</div>
                <CodeEditor :model-value="originalCode" :read-only="true" :height="280" />
              </div>
              <div class="diff-side">
                <div class="diff-label">AI 修复建议</div>
                <CodeEditor :model-value="fixedCode" :read-only="true" :height="280" />
              </div>
            </div>
          </a-card>
        </a-col>
      </a-row>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { message } from 'ant-design-vue'
import { BulbOutlined } from '@ant-design/icons-vue'
import { projectApi } from '@/api/project'
import { uploadApi } from '@/api/upload'
import { aiApi } from '@/api/ai'
import CodeEditor from '@/components/common/CodeEditor.vue'

const route = useRoute()
const projectId = computed(() => route.params.id)

const issues = ref([])
const issuesLoading = ref(false)
const selectedIssue = ref(null)
const aiContent = ref('')
const summaryLoading = ref(false)
const optimizeLoading = ref(false)
const fixLoading = ref(false)
const showDiff = ref(false)
const originalCode = ref('')
const fixedCode = ref('')
const aiStatus = ref(null)
const apiKeyMissing = ref(false)
const apiKeyHint = ref('')

const aiStreaming = computed(() => summaryLoading.value || optimizeLoading.value || fixLoading.value)

function escapeHtml(text) {
  return text
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')
}

const renderedContent = computed(() => {
  const escaped = escapeHtml(aiContent.value)
  return escaped
    .replace(/```(\w*)\n([\s\S]*?)```/g, '<pre><code>$2</code></pre>')
    .replace(/\n/g, '<br>')
})

function severityColor(severity) {
  if (severity === 'high') return 'red'
  if (severity === 'medium') return 'orange'
  return 'blue'
}

async function loadIssues() {
  issuesLoading.value = true
  try {
    const res = await projectApi.getAnalysisResult(projectId.value)
    issues.value = res.data?.issues || []
  } catch (e) {
    message.warning('请先完成代码分析')
  } finally {
    issuesLoading.value = false
  }
}

function selectIssue(issue) {
  selectedIssue.value = issue
  showDiff.value = false
}

async function generateSummary() {
  if (aiStreaming.value) return
  summaryLoading.value = true
  aiContent.value = ''
  try {
    const { done } = aiApi.streamSummary(projectId.value, {
      onToken: (t) => { aiContent.value += t }
    })
    await done
  } catch (e) {
    message.error('生成摘要失败: ' + (e.message || '请检查 AI 配置'))
  } finally {
    summaryLoading.value = false
  }
}

async function generateOptimize() {
  if (!selectedIssue.value || aiStreaming.value) return
  optimizeLoading.value = true
  aiContent.value = ''
  showDiff.value = false
  try {
    const { done } = aiApi.streamOptimize(projectId.value, selectedIssue.value.id, {
      onToken: (t) => { aiContent.value += t }
    })
    await done
  } catch (e) {
    message.error('生成优化建议失败: ' + (e.message || '请检查 AI 配置'))
  } finally {
    optimizeLoading.value = false
  }
}

async function generateFix() {
  if (!selectedIssue.value || aiStreaming.value) return
  fixLoading.value = true
  aiContent.value = ''
  try {
    const { done } = aiApi.streamFix(projectId.value, selectedIssue.value.id, {
      onToken: (t) => { aiContent.value += t }
    })
    await done
    await loadCodeForDiff()
  } catch (e) {
    message.error('生成修复代码失败: ' + (e.message || '请检查 AI 配置'))
  } finally {
    fixLoading.value = false
  }
}

async function loadCodeForDiff() {
  if (!selectedIssue.value) return
  try {
    const res = await uploadApi.getFileContent(projectId.value, selectedIssue.value.file)
    originalCode.value = res?.data?.content || ''
    const match = aiContent.value.match(/```[\w]*\n([\s\S]*?)```/)
    fixedCode.value = match ? match[1] : aiContent.value
    showDiff.value = true
  } catch {
    fixedCode.value = aiContent.value
    showDiff.value = true
  }
}

async function checkAiStatus() {
  try {
    const res = await aiApi.getStatus()
    aiStatus.value = res.data
    const provider = res.data?.activeProvider || 'deepseek'
    apiKeyHint.value = `请在后端配置环境变量 OPENAI_API_KEY（当前提供商: ${provider}）。` +
      'DeepSeek 注册地址: https://platform.deepseek.com ；' +
      '或本地模型: 设置 AI_PROVIDER=ollama 并启动 Ollama'
    // 无法直接检测 API Key 是否有效，但可以提示用户
    apiKeyMissing.value = false
  } catch {
    apiKeyMissing.value = true
  }
}

onMounted(async () => {
  await checkAiStatus()
  await loadIssues()
})
</script>

<style scoped>
.code-optimize-page {
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px 20px;
}

.page-header {
  margin-bottom: 24px;
}

.page-title {
  font-size: 24px;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 8px;
}

.page-description {
  color: #666;
  margin-top: 8px;
}

.issue-card {
  padding: 12px;
  border: 1px solid #f0f0f0;
  border-radius: 8px;
  margin-bottom: 8px;
  cursor: pointer;
  transition: all 0.2s;
}

.issue-card:hover,
.issue-card.active {
  border-color: #1890ff;
  background: #e6f7ff;
}

.issue-top {
  display: flex;
  gap: 8px;
  align-items: flex-start;
}

.issue-msg {
  flex: 1;
  font-size: 13px;
}

.issue-meta {
  margin-top: 6px;
  font-size: 12px;
  color: #999;
}

.ai-content {
  min-height: 300px;
  line-height: 1.7;
  font-size: 14px;
  white-space: pre-wrap;
}

.ai-content pre {
  background: #1e1e1e;
  color: #d4d4d4;
  padding: 12px;
  border-radius: 6px;
  overflow-x: auto;
}

.diff-container {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.diff-label {
  font-weight: 500;
  margin-bottom: 8px;
}

.empty-box {
  padding: 40px 0;
}
</style>
