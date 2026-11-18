import api from './request'

/**
 * AI对话模块 API 封装
 */

/** 创建新会话 */
export function createSession(contextType, contextId, title) {
  return api.post('/ai/chat/session', null, {
    params: { contextType, contextId, title }
  })
}

/** 获取会话列表 */
export function listSessions() {
  return api.get('/ai/chat/sessions')
}

/** 获取会话历史消息 */
export function getMessages(sessionId) {
  return api.get(`/ai/chat/session/${sessionId}/messages`)
}

/** 删除会话 */
export function deleteSession(sessionId) {
  return api.delete(`/ai/chat/session/${sessionId}`)
}

/**
 * 发送消息（SSE流式）
 * 使用 fetch 替代 axios（axios 不支持流式读取）
 *
 * @param {Object} data - { sessionId, contextType, contextId, message }
 * @param {Function} onChunk - 每收到一个文本块时的回调 (chunkText)
 * @param {Function} onSessionId - 收到服务端返回的sessionId时的回调
 * @param {Function} onReasoning - 收到思考过程增量时的回调 (delta)
 * @param {Function} onReasoningReset - 思考过程需要清空时的回调
 * @param {Function} onDone - 流结束回调
 * @param {Function} onError - 错误回调 (error)
 * @returns {AbortController} 可用于中止请求
 */
export function sendMessageStream(data, { onChunk, onSessionId, onReasoning, onReasoningReset, onDone, onError }) {
  const controller = new AbortController()
  const token = sessionStorage.getItem('token')

  fetch('/api/ai/chat/send', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': 'Bearer ' + (token || '')
    },
    body: JSON.stringify(data),
    signal: controller.signal
  }).then(async (response) => {
    if (!response.ok) {
      // 处理401等错误
      if (response.status === 401) {
        sessionStorage.clear()
        window.location.href = '/login'
        return
      }
      onError && onError(new Error('请求失败: ' + response.status))
      return
    }

    try {
      const reader = response.body.getReader()
      const decoder = new TextDecoder()
      let buffer = ''

      // SSE事件解析：一条事件可含多个 data: 行，需用换行连接；
      // 不能 trim 内容，否则会吞掉流式分块里的空格和换行
      const parseSseEvent = (part) => {
        const dataLines = []
        for (const line of part.split('\n')) {
          if (line.startsWith('data:')) {
            dataLines.push(line.slice(5).replace(/\r$/, ''))
          }
        }
        return dataLines.join('\n')
      }

      const handleSseEvent = (part) => {
        const data = parseSseEvent(part)
        if (!data) return false
        if (data === '[DONE]') {
          onDone && onDone()
          return true
        }
        // JSON控制事件（sessionId / error / 思考过程），文本块原样透传
        if (data.trimStart().startsWith('{')) {
          try {
            const json = JSON.parse(data)
            if (json.type === 'reasoning') {
              onReasoning && onReasoning(json.delta || '')
              return false
            }
            if (json.type === 'reasoningReset') {
              onReasoningReset && onReasoningReset()
              return false
            }
            if (json.sessionId) {
              onSessionId && onSessionId(json.sessionId)
              return false
            }
            if (json.error) {
              onError && onError(new Error(json.error))
              return true
            }
          } catch (e) {
            // 非JSON，按普通文本处理
          }
        }
        onChunk && onChunk(data)
        return false
      }

      while (true) {
        const { done, value } = await reader.read()
        if (done) break

        buffer += decoder.decode(value, { stream: true })

        // SSE格式: "data: content\n\n"
        const parts = buffer.split('\n\n')
        buffer = parts.pop() || '' // 保留不完整部分

        for (const part of parts) {
          if (handleSseEvent(part)) return
        }
      }

      // 处理buffer中的残留数据（[DONE]可能落在残留buffer中，避免onDone重复触发）
      if (buffer.trim()) {
        if (handleSseEvent(buffer)) return
      }

      onDone && onDone()
    } catch (e) {
      if (e.name !== 'AbortError') {
        onError && onError(e)
      }
    }
  }).catch((e) => {
    if (e.name !== 'AbortError') {
      onError && onError(e)
    }
  })

  return controller
}