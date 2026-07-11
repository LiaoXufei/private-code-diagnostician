import { defineStore } from 'pinia'
import { ref } from 'vue'
import { aiApi } from '@/api/ai'

export const useAiStore = defineStore('ai', () => {
  const status = ref({ enabled: false, activeProvider: 'deepseek' })
  const streaming = ref(false)
  const streamContent = ref('')
  const conversations = ref([])
  const messages = ref([])
  const currentConversationId = ref(null)

  async function fetchStatus() {
    try {
      const res = await aiApi.getStatus()
      status.value = res.data
    } catch (e) {
      console.warn('AI status unavailable', e)
    }
  }

  function resetStream() {
    streamContent.value = ''
    streaming.value = false
  }

  async function runStream(streamFn) {
    resetStream()
    streaming.value = true
    return new Promise((resolve, reject) => {
      streamFn({
        onToken: (token) => {
          streamContent.value += token
        },
        onDone: () => {
          streaming.value = false
          resolve(streamContent.value)
        },
        onError: (err) => {
          streaming.value = false
          reject(err)
        }
      })
    })
  }

  async function loadConversations(projectId) {
    const res = await aiApi.listConversations(projectId)
    conversations.value = res.data || []
  }

  async function createConversation(projectId, title) {
    const res = await aiApi.createConversation(projectId, { title })
    currentConversationId.value = res.data.id
    await loadConversations(projectId)
    return res.data
  }

  async function loadMessages(projectId, conversationId) {
    const res = await aiApi.listMessages(projectId, conversationId)
    messages.value = res.data || []
  }

  return {
    status,
    streaming,
    streamContent,
    conversations,
    messages,
    currentConversationId,
    fetchStatus,
    resetStream,
    runStream,
    loadConversations,
    createConversation,
    loadMessages
  }
})
