import { defineStore } from 'pinia'
import { ref } from 'vue'
import { message } from 'ant-design-vue'
import { aiApi } from '@/api/ai'

export const useAiStore = defineStore('ai', () => {
  const status = ref({ enabled: false, activeProvider: 'deepseek', ragEnabled: false })
  const conversations = ref([])
  const messages = ref([])
  const currentConversationId = ref(null)
  const convLoading = ref(false)
  const streaming = ref(false)
  const streamContent = ref('')

  let streamAbort = null
  let titleRefreshTimer = null

  async function fetchStatus() {
    try {
      const res = await aiApi.getStatus()
      status.value = res.data
    } catch (e) {
      console.warn('AI status unavailable', e)
    }
  }

  function abortStream() {
    streamAbort?.()
    streamAbort = null
    streaming.value = false
    streamContent.value = ''
  }

  function clearTitleRefreshTimer() {
    if (titleRefreshTimer) {
      clearTimeout(titleRefreshTimer)
      titleRefreshTimer = null
    }
  }

  async function loadConversations(projectId) {
    convLoading.value = true
    try {
      const res = await aiApi.listConversations(projectId)
      conversations.value = res.data || []
    } finally {
      convLoading.value = false
    }
  }

  async function createConversation(projectId) {
    const res = await aiApi.createConversation(projectId, {})
    currentConversationId.value = res.data.id
    messages.value = []
    await loadConversations(projectId)
    return res.data
  }

  async function selectConversation(projectId, conversationId) {
    currentConversationId.value = conversationId
    const res = await aiApi.listMessages(projectId, conversationId)
    messages.value = res.data || []
  }

  async function deleteConversation(projectId, conversationId) {
    if (conversationId === currentConversationId.value && streaming.value) {
      abortStream()
    }
    await aiApi.deleteConversation(projectId, conversationId)
    if (conversationId === currentConversationId.value) {
      currentConversationId.value = null
      messages.value = []
    }
    await loadConversations(projectId)
    message.success('对话已删除')
  }

  async function handleSend(projectId, text, scrollCallback) {
    if (streaming.value) return false

    const trimmed = text.trim()
    if (!trimmed) return false

    if (!currentConversationId.value) {
      try {
        await createConversation(projectId)
      } catch {
        message.error('创建对话失败')
        return false
      }
    }

    messages.value.push({
      id: crypto.randomUUID(),
      role: 'user',
      content: trimmed
    })
    streaming.value = true
    streamContent.value = ''
    scrollCallback?.()

    try {
      const { abort, done } = aiApi.streamChat(projectId, currentConversationId.value, trimmed, {
        onToken: (t) => {
          streamContent.value += t
          scrollCallback?.()
        }
      })
      streamAbort = abort
      await done

      messages.value.push({
        id: crypto.randomUUID(),
        role: 'assistant',
        content: streamContent.value
      })
      clearTitleRefreshTimer()
      titleRefreshTimer = setTimeout(() => loadConversations(projectId), 2000)
      return true
    } catch (err) {
      message.error(err?.message ? `对话失败: ${err.message}` : '对话失败，请检查 AI 配置')
      return false
    } finally {
      streamAbort = null
      streaming.value = false
      streamContent.value = ''
      scrollCallback?.()
    }
  }

  async function initChatPage(projectId) {
    await fetchStatus()
    await loadConversations(projectId)
    if (status.value?.ragEnabled) {
      try {
        await aiApi.indexEmbeddings(projectId)
      } catch {
        // 索引可能失败（无 API Key），不阻断页面
      }
    }
  }

  function cleanupChatPage() {
    abortStream()
    clearTitleRefreshTimer()
  }

  return {
    status,
    conversations,
    messages,
    currentConversationId,
    convLoading,
    streaming,
    streamContent,
    fetchStatus,
    loadConversations,
    createConversation,
    selectConversation,
    deleteConversation,
    handleSend,
    initChatPage,
    cleanupChatPage,
    abortStream
  }
})
