import request from './request'

const API_BASE = import.meta.env.VITE_API_BASE_URL || '/api'

/**
 * 使用 fetch + ReadableStream 消费 SSE 流式响应。
 * 处理 Spring SseEmitter 的 data: 和 event:error 两种事件。
 */
export async function streamAiRequest(url, body, onToken, onDone, onError) {
  const controller = new AbortController()
  try {
    const response = await fetch(`${API_BASE}${url}`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Accept: 'text/event-stream'
      },
      body: body ? JSON.stringify(body) : undefined,
      signal: controller.signal
    })

    if (!response.ok) {
      let errMsg = `请求失败 (${response.status})`
      try {
        const text = await response.text()
        if (text) {
          // 尝试解析后端 ErrorResponse JSON
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
      if (dataBuffer) {
        const data = dataBuffer
        dataBuffer = ''
        if (currentEvent === 'error') {
          // 后端发送的错误事件
          onError?.(new Error(data.replace('[AI_ERROR] ', '')))
        } else if (data !== '[DONE]') {
          onToken?.(data)
        }
      }
    }

    while (true) {
      const { done, value } = await reader.read()
      if (done) break

      buffer += decoder.decode(value, { stream: true })

      // SSE 事件以双换行分隔
      const events = buffer.split('\n\n')
      buffer = events.pop() || ''

      for (const eventBlock of events) {
        const lines = eventBlock.split('\n')
        currentEvent = 'message'
        dataBuffer = ''

        for (const line of lines) {
          if (line.startsWith('event:')) {
            currentEvent = line.slice(6).trim()
          } else if (line.startsWith('data:')) {
            const dataPart = line.slice(5)
            // SSE 规范：data: 后如果紧跟空格则去掉那个空格
            const cleaned = dataPart.startsWith(' ') ? dataPart.slice(1) : dataPart
            if (dataBuffer) {
              dataBuffer += '\n' + cleaned
            } else {
              dataBuffer = cleaned
            }
          }
        }
        flushData()
      }
    }

    // 处理 buffer 中剩余的数据
    if (buffer.trim()) {
      const lines = buffer.split('\n')
      dataBuffer = ''
      currentEvent = 'message'
      for (const line of lines) {
        if (line.startsWith('event:')) {
          currentEvent = line.slice(6).trim()
        } else if (line.startsWith('data:')) {
          const dataPart = line.slice(5)
          const cleaned = dataPart.startsWith(' ') ? dataPart.slice(1) : dataPart
          if (dataBuffer) {
            dataBuffer += '\n' + cleaned
          } else {
            dataBuffer = cleaned
          }
        }
      }
      flushData()
    }

    onDone?.()
  } catch (err) {
    if (err.name !== 'AbortError') {
      onError?.(err)
    }
  }
  return () => controller.abort()
}

export const aiApi = {
  getStatus: () => request.get('/ai/status'),
  getModels: () => request.get('/ai/models'),

  streamSummary: (projectId, handlers) =>
    streamAiRequest(`/projects/${projectId}/ai/summary/stream`, null, handlers.onToken, handlers.onDone, handlers.onError),

  streamOptimize: (projectId, issueId, handlers) =>
    streamAiRequest(`/projects/${projectId}/ai/optimize/stream`, { issueId }, handlers.onToken, handlers.onDone, handlers.onError),

  streamFix: (projectId, issueId, handlers) =>
    streamAiRequest(`/projects/${projectId}/ai/fix/stream`, { issueId }, handlers.onToken, handlers.onDone, handlers.onError),

  indexEmbeddings: (projectId) => request.post(`/projects/${projectId}/ai/embeddings`),

  listConversations: (projectId) => request.get(`/projects/${projectId}/ai/conversations`),
  createConversation: (projectId, data) => request.post(`/projects/${projectId}/ai/conversations`, data),
  deleteConversation: (projectId, conversationId) =>
    request.delete(`/projects/${projectId}/ai/conversations/${conversationId}`),
  listMessages: (projectId, conversationId) =>
    request.get(`/projects/${projectId}/ai/conversations/${conversationId}/messages`),

  streamChat: (projectId, conversationId, message, handlers) =>
    streamAiRequest(`/projects/${projectId}/ai/chat/stream`, { conversationId, message }, handlers.onToken, handlers.onDone, handlers.onError),

  streamAgent: (projectId, prompt, handlers) =>
    streamAiRequest(`/projects/${projectId}/ai/agent/stream`, { prompt }, handlers.onToken, handlers.onDone, handlers.onError)
}
