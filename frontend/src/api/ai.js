import request from './request'

const API_BASE = import.meta.env.VITE_API_BASE_URL || '/api'

/**
 * 使用 fetch + ReadableStream 消费 SSE 流式响应
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
      const text = await response.text()
      throw new Error(text || `请求失败 (${response.status})`)
    }

    const reader = response.body.getReader()
    const decoder = new TextDecoder()
    let buffer = ''

    while (true) {
      const { done, value } = await reader.read()
      if (done) break

      buffer += decoder.decode(value, { stream: true })
      const lines = buffer.split('\n')
      buffer = lines.pop() || ''

      for (const line of lines) {
        if (line.startsWith('data:')) {
          const data = line.slice(5).trim()
          if (data && data !== '[DONE]') {
            onToken?.(data)
          }
        } else if (line.trim() && !line.startsWith(':')) {
          onToken?.(line)
        }
      }
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
  listMessages: (projectId, conversationId) =>
    request.get(`/projects/${projectId}/ai/conversations/${conversationId}/messages`),

  streamChat: (projectId, conversationId, message, handlers) =>
    streamAiRequest(`/projects/${projectId}/ai/chat/stream`, { conversationId, message }, handlers.onToken, handlers.onDone, handlers.onError),

  streamAgent: (projectId, prompt, handlers) =>
    streamAiRequest(`/projects/${projectId}/ai/agent/stream`, { prompt }, handlers.onToken, handlers.onDone, handlers.onError)
}
