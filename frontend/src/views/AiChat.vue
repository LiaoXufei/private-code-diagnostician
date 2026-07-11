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
                :disabled="streaming"
                placeholder="输入你的问题..."
                @pressEnter="handleSend"
              />
              <a-button
                type="primary"
                :loading="streaming"
                :disabled="!inputText.trim() || streaming"
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
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import { storeToRefs } from 'pinia'
import { message } from 'ant-design-vue'
import { MessageOutlined, PlusOutlined, DeleteOutlined } from '@ant-design/icons-vue'
import { useAiStore } from '@/stores/ai'

const route = useRoute()
const aiStore = useAiStore()
const projectId = computed(() => route.params.id)

const {
  status: aiStatus,
  conversations,
  messages,
  currentConversationId,
  convLoading,
  streaming,
  streamContent
} = storeToRefs(aiStore)

const inputText = ref('')
const messagesRef = ref(null)

function scrollToBottom() {
  nextTick(() => {
    if (messagesRef.value) {
      messagesRef.value.scrollTop = messagesRef.value.scrollHeight
    }
  })
}

async function handleNewChat() {
  try {
    await aiStore.createConversation(projectId.value)
  } catch {
    message.error('创建对话失败')
  }
}

async function selectConversation(id) {
  try {
    await aiStore.selectConversation(projectId.value, id)
    scrollToBottom()
  } catch {
    message.error('加载对话失败')
  }
}

async function handleDeleteConversation(id) {
  try {
    await aiStore.deleteConversation(projectId.value, id)
  } catch {
    message.error('删除失败')
  }
}

async function handleSend(e) {
  if (e?.shiftKey) return
  e?.preventDefault?.()

  const text = inputText.value.trim()
  if (!text || streaming.value) return

  inputText.value = ''
  await aiStore.handleSend(projectId.value, text, scrollToBottom)
}

onMounted(async () => {
  await aiStore.initChatPage(projectId.value)
})

onUnmounted(() => {
  aiStore.cleanupChatPage()
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
