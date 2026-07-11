import request, { buildAuthHeaders } from './request'

const API_BASE = import.meta.env.VITE_API_BASE_URL || '/api'

/**
 * 使用 fetch + ReadableStream 消费 SSE 流式响应。
 * 返回 { abort, done }：abort 立即可用，done 为 Promise。
 */
export function streamAiRequest(url, body, { onToken, onDone, onError } = {}) {
  const controller = new AbortController()
  const abort = () => controller.abort()

  const done = (async () => {
    let failed = false
    try {
      const response = await fetch(`${API_BASE}${url}`, {
        method: 'POST',
        headers: buildAuthHeaders({ Accept: 'text/event-stream' }),
        body: body ? JSON.stringify(body) : undefined,
        signal: controller.signal
      })

      if (!response.ok) {
        let errMsg = `请求失败 (${response.status})`
        try {
          const text = await response.text()
          if (text) {
            try {
              const json = JSON.parse(text)
              errMsg = json.message || json.error || text
            } catch {
              errMsg = text
            }
          }
        } catch {}
        throw new Error(errMsg)
      }

      const reader = response.body.getReader()
      const decoder = new TextDecoder()
      let buffer = ''
      let currentEvent = 'message'
      let dataBuffer = ''

      const flushData = () => {
        if (!dataBuffer) return
        const data = dataBuffer
        dataBuffer = ''
        if (currentEvent === 'error') {
          failed = true
          const err = new Error(data.replace('[AI_ERROR] ', ''))
          onError?.(err)
          throw err
        }
        if (data !== '[DONE]') {
          onToken?.(data)
        }
      }

      const processEventBlock = (eventBlock) => {
        const lines = eventBlock.split('\n')
        currentEvent = 'message'
        dataBuffer = ''
        for (const line of lines) {
          if (line.startsWith('event:')) {
            currentEvent = line.slice(6).trim()
          } else if (line.startsWith('data:')) {
            const dataPart = line.slice(5)
            const cleaned = dataPart.startsWith(' ') ? dataPart.slice(1) : dataPart
            dataBuffer = dataBuffer ? dataBuffer + '\n' + cleaned : cleaned
          }
        }
        flushData()
      }

      while (true) {
        const { done: streamDone, value } = await reader.read()
        if (streamDone) break

        buffer += decoder.decode(value, { stream: true }).replace(/\r\n/g, '\n')
        const events = buffer.split('\n\n')
        buffer = events.pop() || ''

        for (const eventBlock of events) {
          processEventBlock(eventBlock)
        }
      }

      if (buffer.trim()) {
        processEventBlock(buffer)
      }

      if (!failed) {
        onDone?.()
      }
    } catch (err) {
      if (err.name === 'AbortError') {
        return
      }
      failed = true
      onError?.(err)
      throw err
    }
  })()

  return { abort, done }
}

export const aiApi = {
  getStatus: () => request.get('/ai/status'),

  streamSummary: (projectId, handlers) =>
    streamAiRequest(`/projects/${projectId}/ai/summary/stream`, null, handlers),

  streamOptimize: (projectId, issueId, handlers) =>
    streamAiRequest(`/projects/${projectId}/ai/optimize/stream`, { issueId }, handlers),

  streamFix: (projectId, issueId, handlers) =>
    streamAiRequest(`/projects/${projectId}/ai/fix/stream`, { issueId }, handlers),

  indexEmbeddings: (projectId) => request.post(`/projects/${projectId}/ai/embeddings`),

  listConversations: (projectId) => request.get(`/projects/${projectId}/ai/conversations`),
  createConversation: (projectId, data) => request.post(`/projects/${projectId}/ai/conversations`, data),
  deleteConversation: (projectId, conversationId) =>
    request.delete(`/projects/${projectId}/ai/conversations/${conversationId}`),
  listMessages: (projectId, conversationId) =>
    request.get(`/projects/${projectId}/ai/conversations/${conversationId}/messages`),

  streamChat: (projectId, conversationId, message, handlers) =>
    streamAiRequest(`/projects/${projectId}/ai/chat/stream`, { conversationId, message }, handlers),

  streamAgent: (projectId, prompt, handlers) =>
    streamAiRequest(`/projects/${projectId}/ai/agent/stream`, { prompt }, handlers)
}
