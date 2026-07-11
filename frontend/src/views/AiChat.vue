<template>
  <div class="ai-chat-page">
    <div class="page-header">
      <h1 class="page-title">
        <MessageOutlined />
        AI 代码对话
      </h1>
      <p class="page-description">
        基于 RAG 检索增强，用自然语言提问项目代码
      </p>
    </div>

    <div class="content-container">
      <a-alert
        v-if="aiStatus && !aiStatus.ragEnabled"
        type="info"
        show-icon
        :message="`RAG 未启用（embedding-provider=${aiStatus.embeddingProvider}），对话为降级模式`"
        :description="aiStatus.hint || '如需代码语义检索，请配置 EMBEDDING_PROVIDER=openai 或 ollama'"
        style="margin-bottom: 16px"
      />
      <a-row :gutter="16">
        <a-col :span="6">
          <a-card title="对话列表" size="small">
            <a-button type="dashed" block style="margin-bottom: 12px" @click="handleNewChat">
              <PlusOutlined /> 新对话
            </a-button>
            <a-list
              :data-source="conversations"
              :loading="convLoading"
              size="small"
            >
              <template #renderItem="{ item }">
                <a-list-item
                  class="conv-item"
                  :class="{ active: item.id === currentConversationId }"
                  @click="selectConversation(item.id)"
                >
                  <span class="conv-title">{{ item.title }}</span>
                  <a-popconfirm
                    title="确定删除该对话？"
                    ok-text="删除"
                    cancel-text="取消"
                    @confirm="handleDeleteConversation(item.id)"
                  >
                    <a-button
                      type="text"
                      size="small"
                      class="conv-delete-btn"
                      aria-label="删除对话"
                      @click.stop
                    >
                      <DeleteOutlined />
                    </a-button>
                  </a-popconfirm>
                </a-list-item>
              </template>
            </a-list>
          </a-card>
        </a-col>

        <a-col :span="18">
          <a-card title="对话" class="chat-panel">
            <div ref="messagesRef" class="messages-area">
              <div v-if="messages.length === 0" class="empty-chat">
                <a-empty description="开始提问，例如：这个项目哪里圈复杂度最高？" />
              </div>
              <div
                v-for="msg in messages"
                :key="msg.id"
                class="message"
                :class="msg.role"
              >
                <div class="bubble">{{ msg.content }}</div>
              </div>
              <div v-if="streaming" class="message assistant">
                <div class="bubble typing">{{ streamContent }}<span class="cursor">|</span></div>
              </div>
            </div>

            <div class="input-area">
              <a-textarea
                v-model:value="inputText"
                :rows="3"
                placeholder="输入你的问题..."
                @pressEnter="handleSend"
              />
              <a-button
                type="primary"
                :loading="streaming"
                :disabled="!inputText.trim()"
                style="margin-top: 8px"
                @click="handleSend"
              >
                发送
              </a-button>
            </div>
          </a-card>
        </a-col>
      </a-row>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import { message } from 'ant-design-vue'
import { MessageOutlined, PlusOutlined, DeleteOutlined } from '@ant-design/icons-vue'
import { useAiStore } from '@/stores/ai'
import { aiApi } from '@/api/ai'

const route = useRoute()
const aiStore = useAiStore()
const projectId = computed(() => route.params.id)

const conversations = ref([])
const messages = ref([])
const currentConversationId = ref(null)
const inputText = ref('')
const convLoading = ref(false)
const streaming = ref(false)
const streamContent = ref('')
const messagesRef = ref(null)
const aiStatus = ref(null)

async function loadConversations() {
  convLoading.value = true
  try {
    const res = await aiApi.listConversations(projectId.value)
    conversations.value = res.data || []
  } finally {
    convLoading.value = false
  }
}

async function handleNewChat() {
  const res = await aiApi.createConversation(projectId.value, {})
  currentConversationId.value = res.data.id
  messages.value = []
  await loadConversations()
}

async function selectConversation(id) {
  currentConversationId.value = id
  const res = await aiApi.listMessages(projectId.value, id)
  messages.value = res.data || []
  scrollToBottom()
}

async function handleDeleteConversation(id) {
  try {
    await aiApi.deleteConversation(projectId.value, id)
    if (id === currentConversationId.value) {
      currentConversationId.value = null
      messages.value = []
    }
    await loadConversations()
    message.success('对话已删除')
  } catch {
    message.error('删除失败')
  }
}

async function handleSend(e) {
  if (e?.shiftKey) return
  e?.preventDefault?.()

  const text = inputText.value.trim()
  if (!text) return

  if (!currentConversationId.value) {
    await handleNewChat()
  }

  messages.value.push({
    id: Date.now(),
    role: 'user',
    content: text
  })
  inputText.value = ''
  streaming.value = true
  streamContent.value = ''
  scrollToBottom()

  try {
    await new Promise((resolve, reject) => {
      aiApi.streamChat(projectId.value, currentConversationId.value, text, {
        onToken: (t) => {
          streamContent.value += t
          scrollToBottom()
        },
        onDone: resolve,
        onError: reject
      })
    })
    messages.value.push({
      id: Date.now() + 1,
      role: 'assistant',
      content: streamContent.value
    })
    // AI 在后端异步根据首条提问生成对话标题，延迟刷新以展示新标题
    setTimeout(loadConversations, 2000)
  } catch (err) {
    message.error('对话失败，请检查 AI 配置')
  } finally {
    streaming.value = false
    streamContent.value = ''
    scrollToBottom()
  }
}

function scrollToBottom() {
  nextTick(() => {
    if (messagesRef.value) {
      messagesRef.value.scrollTop = messagesRef.value.scrollHeight
    }
  })
}

onMounted(async () => {
  try {
    const res = await aiApi.getStatus()
    aiStatus.value = res.data
  } catch {}
  await aiStore.fetchStatus()
  await loadConversations()
  if (aiStatus.value?.ragEnabled) {
    try {
      await aiApi.indexEmbeddings(projectId.value)
    } catch {
      // 索引可能失败（无 API Key），不阻断页面
    }
  }
})
</script>

<style scoped>
.ai-chat-page {
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px 20px;
}

.page-title {
  font-size: 24px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.messages-area {
  height: 420px;
  overflow-y: auto;
  padding: 12px;
  background: #fafafa;
  border-radius: 8px;
  margin-bottom: 12px;
}

.message {
  display: flex;
  margin-bottom: 12px;
}

.message.user {
  justify-content: flex-end;
}

.message.assistant {
  justify-content: flex-start;
}

.bubble {
  max-width: 80%;
  padding: 10px 14px;
  border-radius: 12px;
  white-space: pre-wrap;
  line-height: 1.6;
  font-size: 14px;
}

.message.user .bubble {
  background: #1890ff;
  color: #fff;
}

.message.assistant .bubble {
  background: #fff;
  border: 1px solid #f0f0f0;
}

.cursor {
  animation: blink 1s infinite;
}

@keyframes blink {
  50% { opacity: 0; }
}

.conv-item {
  cursor: pointer;
  padding: 8px;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.conv-item.active {
  background: #e6f7ff;
}

.conv-title {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.conv-delete-btn {
  flex-shrink: 0;
  opacity: 0;
  color: #999;
  transition: opacity 0.2s;
}

.conv-item:hover .conv-delete-btn {
  opacity: 1;
}

.conv-delete-btn:hover {
  color: #ff4d4f;
}

.empty-chat {
  padding: 80px 0;
}
</style>
