<template>
  <div class="chat-detail-page">
    <FamilyTopNav />

    <main class="chat-main">
      <section class="chat-shell">
    <!-- 顶部栏 -->
    <div class="chat-header">
      <el-button text @click="goBack" class="back-btn">
        <el-icon :size="20"><ArrowLeft /></el-icon>
      </el-button>
      <h1 class="chat-title">{{ sessionTitle || 'AI健康助手' }}</h1>
      <span class="chat-status">
        <i aria-hidden="true"></i>
        AI 在线
      </span>
    </div>

    <!-- 上下文选择区（仅新建会话时显示） -->
    <div class="context-bar" v-if="isNewSession && !sessionCreated">
      <span class="context-label">选择咨询场景（可选）：</span>
      <div class="context-tags">
        <el-tag
          :type="contextType === 'GENERAL' ? '' : 'info'"
          class="context-tag"
          @click="contextType = 'GENERAL'; contextId = null"
        >
          通用咨询
        </el-tag>
        <el-tag
          :type="contextType === 'ELDERLY' ? '' : 'info'"
          class="context-tag"
          @click="contextType = 'ELDERLY'"
        >
          老人档案
        </el-tag>
        <el-tag
          :type="contextType === 'RECORD' ? '' : 'info'"
          class="context-tag"
          @click="contextType = 'RECORD'"
        >
          就医记录
        </el-tag>
      </div>
      <div class="context-select" v-if="contextType === 'ELDERLY' || contextType === 'RECORD'">
        <span class="context-label">请选择关联的{{ contextType === 'ELDERLY' ? '老人' : '就医记录' }}：</span>
        <el-select
          v-model="contextId"
          :placeholder="'请选择' + (contextType === 'ELDERLY' ? '老人' : '记录')"
          style="width: 100%; margin-top: 8px;"
          size="large"
        >
          <el-option
            v-for="item in contextOptions"
            :key="item.id"
            :label="item.name"
            :value="item.id"
          />
        </el-select>
      </div>
    </div>

    <!-- 消息列表 -->
    <div class="message-list" ref="messageListRef">
      <!-- 空状态 -->
      <div class="welcome-area" v-if="messages.length === 0 && !isStreaming">
        <el-icon :size="56" color="var(--teal-deep)"><ChatDotRound /></el-icon>
        <p class="welcome-title">您好，我是AI健康助手</p>
        <p class="welcome-desc">我可以帮您解答就诊流程、检查报告、用药注意事项等问题</p>
        <p class="welcome-hint">可先在上方选择关联老人档案，AI 会结合病历回答</p>
        <div class="quick-questions">
          <button type="button" class="quick-item" @click="sendQuick('去心内科就诊需要准备什么？')">
            <span class="quick-icon"><el-icon :size="16"><ChatDotRound /></el-icon></span>
            <span class="quick-text">就诊需要准备什么？</span>
          </button>
          <button type="button" class="quick-item" @click="sendQuick('血糖偏高平时要注意什么？')">
            <span class="quick-icon"><el-icon :size="16"><ChatDotRound /></el-icon></span>
            <span class="quick-text">血糖偏高要注意什么？</span>
          </button>
          <button type="button" class="quick-item" @click="sendQuick('高血压吃药有什么注意事项？')">
            <span class="quick-icon"><el-icon :size="16"><ChatDotRound /></el-icon></span>
            <span class="quick-text">吃药有什么注意事项？</span>
          </button>
        </div>
      </div>

      <!-- 消息气泡 -->
      <div
        v-for="(msg, index) in messages"
        :key="msg.id || index"
        :class="['message-row', msg.role === 'USER' ? 'msg-user' : 'msg-assistant']"
      >
        <div class="msg-avatar">
          <el-icon :size="28" v-if="msg.role === 'ASSISTANT'">
            <Service />
          </el-icon>
          <el-icon :size="28" v-else>
            <UserFilled />
          </el-icon>
        </div>
        <div :class="['msg-bubble', msg.role === 'USER' ? 'bubble-user' : 'bubble-assistant']">
          <div v-if="msg.reasoning" class="reasoning-panel">
            <div class="reasoning-header" @click="toggleMessageReasoning(msg)">
              <span class="reasoning-title">已思考 {{ msg.reasoningSeconds || 0 }} 秒</span>
              <span class="reasoning-arrow" :class="{ open: msg.reasoningOpen }">▾</span>
            </div>
            <div v-show="msg.reasoningOpen" class="reasoning-text">{{ msg.reasoning }}</div>
          </div>
          <div class="bubble-text" v-html="formatContent(msg.content)"></div>
          <div class="bubble-time">{{ formatTime(msg.createTime) }}</div>
        </div>
      </div>

      <!-- 思考中：等待首个正文片段，实时展示思考过程 -->
      <div class="message-row msg-assistant" v-if="isThinking && !streamingContent">
        <div class="msg-avatar">
          <el-icon :size="28"><Service /></el-icon>
        </div>
        <div class="msg-bubble bubble-assistant thinking-bubble">
          <div class="thinking-line">
            <span class="thinking-dots"><i></i><i></i><i></i></span>
            <span class="thinking-label">正在思考<template v-if="thinkingSeconds"> · {{ thinkingSeconds }}s</template></span>
          </div>
          <div v-if="reasoningContent" class="reasoning-text live">{{ reasoningContent }}</div>
        </div>
      </div>

      <!-- 流式输出中的AI消息 -->
      <div class="message-row msg-assistant" v-if="isStreaming && streamingContent">
        <div class="msg-avatar">
          <el-icon :size="28"><Service /></el-icon>
        </div>
        <div class="msg-bubble bubble-assistant">
          <div v-if="reasoningContent" class="reasoning-panel">
            <div class="reasoning-header" @click="reasoningExpanded = !reasoningExpanded">
              <span class="reasoning-title">已思考 {{ thinkingSeconds }} 秒</span>
              <span class="reasoning-arrow" :class="{ open: reasoningExpanded }">▾</span>
            </div>
            <div v-show="reasoningExpanded" class="reasoning-text">{{ reasoningContent }}</div>
          </div>
          <div class="bubble-text streaming-text" v-html="formatContent(streamingContent)"></div>
          <span class="typing-cursor">|</span>
        </div>
      </div>

      <!-- 免责声明 -->
      <div class="disclaimer-bar" v-if="messages.length > 0">
        <el-icon :size="18" color="var(--amber)"><WarningFilled /></el-icon>
        <span>AI生成内容仅供参考，不构成任何诊疗建议</span>
      </div>
    </div>

    <!-- 底部输入区 -->
    <div class="input-bar">
      <div class="input-row">
        <el-input
          v-model="inputText"
          placeholder="输入您的问题..."
          class="msg-input"
          size="large"
          :disabled="isStreaming"
          @keyup.enter="handleSend"
          clearable
        />
        <el-button
          type="primary"
          class="send-btn"
          :disabled="!inputText.trim() || isStreaming"
          @click="handleSend"
        >
          <el-icon :size="22" v-if="!isStreaming"><Promotion /></el-icon>
          <el-icon :size="22" class="is-loading" v-else><Loading /></el-icon>
        </el-button>
      </div>
    </div>
      </section>
    </main>
  </div>
</template>

<script setup>
import { ref, nextTick, onMounted, onUnmounted, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft, ChatDotRound, Service, UserFilled, Promotion, Loading, WarningFilled } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { getMessages, sendMessageStream } from '@/api/aiChat'
import api from '@/api/request'
import FamilyTopNav from '@/components/FamilyTopNav.vue'
import { sanitizeHtml } from '@/utils/sanitize'

const route = useRoute()
const router = useRouter()

const messages = ref([])
const inputText = ref('')
const isStreaming = ref(false)
const streamingContent = ref('')
const isThinking = ref(false)
const reasoningContent = ref('')
const reasoningExpanded = ref(true)
const thinkingSeconds = ref(0)
let thinkingTimer = null
let thinkingStartedAt = 0
const sessionId = ref(null)
const sessionTitle = ref('')
const sessionCreated = ref(false)

// 返回AI助手列表：新建会话页使用 replace 避免残留；已存在会话直接 back
function goBack() {
  if (isNewSession.value) {
    router.replace('/family/ai-chat')
  } else {
    router.back()
  }
}
const messageListRef = ref(null)

// 上下文选择
const contextType = ref('GENERAL')
const contextId = ref(null)
const contextOptions = ref([])

// 从query参数获取初始上下文
const queryContextType = route.query.contextType || null
const queryContextId = route.query.contextId || null

const isNewSession = computed(() => !route.params.sessionId || route.params.sessionId === 'new')

let abortController = null

// 格式化时间
const formatTime = (time) => {
  if (!time) return ''
  const d = new Date(time)
  const h = String(d.getHours()).padStart(2, '0')
  const m = String(d.getMinutes()).padStart(2, '0')
  return h + ':' + m
}

// 格式化内容（换行转<br>），并统一经过 DOMPurify 消毒
const formatContent = (content) => {
  if (!content) return ''
  const formatted = content
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/\n/g, '<br>')
    .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
  return sanitizeHtml(formatted)
}

// 滚动到底部
const scrollToBottom = () => {
  nextTick(() => {
    if (messageListRef.value) {
      messageListRef.value.scrollTop = messageListRef.value.scrollHeight
    }
  })
}

// 思考计时器：从发送到首个正文片段
const startThinking = () => {
  stopThinking()
  isThinking.value = true
  reasoningContent.value = ''
  reasoningExpanded.value = true
  thinkingSeconds.value = 0
  thinkingStartedAt = Date.now()
  thinkingTimer = setInterval(() => {
    thinkingSeconds.value = Math.max(1, Math.round((Date.now() - thinkingStartedAt) / 1000))
  }, 500)
}

const stopThinking = () => {
  if (thinkingTimer) {
    clearInterval(thinkingTimer)
    thinkingTimer = null
  }
  isThinking.value = false
}

const toggleMessageReasoning = (msg) => {
  msg.reasoningOpen = !msg.reasoningOpen
}

// 加载历史消息
const loadHistory = async (sid) => {
  try {
    const res = await getMessages(sid)
    if (res.code === 200 && res.data) {
      messages.value = res.data
      scrollToBottom()
    }
  } catch (e) {
    console.error('Failed to load messages:', e)
  }
}

// 加载上下文选项（老人列表或就医记录列表）
const loadContextOptions = async (type) => {
  try {
    if (type === 'ELDERLY') {
      const res = await api.get('/elderly/page', { params: { current: 1, size: 1000 } })
      if (res.code === 200 && res.data?.records) {
        contextOptions.value = res.data.records.map(e => ({ id: e.id, name: e.name + (e.gender === 'MALE' ? ' 男' : ' 女') }))
      }
    } else if (type === 'RECORD') {
      const res = await api.get('/record/page', { params: { current: 1, size: 50 } })
      if (res.code === 200 && res.data?.records) {
        contextOptions.value = res.data.records.map(r => ({ id: r.id, name: (r.department || '') + ' - ' + (r.visitDate || '') }))
      }
    }
  } catch (e) {
    console.error('Failed to load context options:', e)
  }
}

// 监听上下文类型切换
watch(contextType, (val) => {
  if (val === 'ELDERLY' || val === 'RECORD') {
    contextId.value = null
    loadContextOptions(val)
  }
})

// 发送消息
const handleSend = async () => {
  const text = inputText.value.trim()
  if (!text || isStreaming.value) return
  inputText.value = ''

  // 添加用户消息到列表
  const userMsg = {
    id: Date.now(),
    role: 'USER',
    content: text,
    createTime: new Date().toISOString()
  }
  messages.value.push(userMsg)
  scrollToBottom()

  // 开始流式 + 思考动画
  isStreaming.value = true
  streamingContent.value = ''
  startThinking()

  const sendData = {
    message: text,
    sessionId: sessionId.value,
    contextType: queryContextType || contextType.value,
    contextId: queryContextId || contextId.value || null
  }

  abortController = sendMessageStream(sendData, {
    onSessionId: (sid) => {
      // 服务端返回了sessionId
      if (!sessionId.value) {
        sessionId.value = sid
        sessionCreated.value = true
        // 更新URL
        router.replace(`/family/ai-chat/${sid}`)
      }
    },
    onReasoning: (delta) => {
      reasoningContent.value += delta
      scrollToBottom()
    },
    onReasoningReset: () => {
      // 主模型失败转兜底：清空与兜底答案无关的思考过程
      reasoningContent.value = ''
      reasoningExpanded.value = true
    },
    onChunk: (chunk) => {
      if (isThinking.value) {
        // 首个正文片段到达，思考结束，折叠思考过程
        stopThinking()
        reasoningExpanded.value = false
      }
      streamingContent.value += chunk
      scrollToBottom()
    },
    onDone: () => {
      stopThinking()
      const finalReasoning = reasoningContent.value
      const finalSeconds = thinkingSeconds.value
      // 将流式内容保存为ASSISTANT消息（附带本次思考过程，仅当前页面可见）
      if (streamingContent.value) {
        messages.value.push({
          id: Date.now(),
          role: 'ASSISTANT',
          content: streamingContent.value,
          reasoning: finalReasoning,
          reasoningSeconds: finalSeconds,
          reasoningOpen: false,
          createTime: new Date().toISOString()
        })
        streamingContent.value = ''
      }
      reasoningContent.value = ''
      isStreaming.value = false
      scrollToBottom()
    },
    onError: (err) => {
      stopThinking()
      ElMessage.error('发送失败: ' + err.message)
      if (streamingContent.value) {
        messages.value.push({
          id: Date.now(),
          role: 'ASSISTANT',
          content: streamingContent.value + '\n\n（回复中断）',
          reasoning: reasoningContent.value,
          reasoningSeconds: thinkingSeconds.value,
          reasoningOpen: false,
          createTime: new Date().toISOString()
        })
        streamingContent.value = ''
      }
      reasoningContent.value = ''
      isStreaming.value = false
    }
  })
}

// 快捷问题
const sendQuick = (text) => {
  inputText.value = text
  handleSend()
}

onMounted(() => {
  // 推荐提问进入时预填输入框
  if (route.query.q) {
    inputText.value = String(route.query.q)
  }

  // 从query参数获取初始上下文
  if (queryContextType) {
    contextType.value = queryContextType
  }
  if (queryContextId) {
    contextId.value = Number(queryContextId)
  }

  if (!isNewSession.value) {
    // 加载已有会话
    sessionId.value = Number(route.params.sessionId)
    sessionCreated.value = true
    loadHistory(sessionId.value)
  } else if (queryContextType) {
    // 带上下文参数的新会话
    loadContextOptions(queryContextType)
  }
})

onUnmounted(() => {
  stopThinking()
  if (abortController) {
    abortController.abort()
  }
})
</script>

<style scoped>
.chat-detail-page {
  height: 100vh;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  background: var(--bg);
}

.chat-main {
  flex: 1;
  min-height: 0;
  display: flex;
  justify-content: center;
  padding: 20px 24px;
}

.chat-shell {
  width: min(1000px, 100%);
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-card);
}

/* ---------- 顶部栏 ---------- */
.chat-header {
  display: flex;
  align-items: center;
  gap: 10px;
  height: 56px;
  padding: 0 18px;
  flex-shrink: 0;
  background: var(--surface);
  border-bottom: 1px solid var(--line);
}

.back-btn {
  color: var(--ink-3);
}

.chat-title {
  flex: 1;
  min-width: 0;
  margin: 0;
  font-size: 15px;
  font-weight: 600;
  color: var(--ink);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.chat-status {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  flex-shrink: 0;
  font-size: 11.5px;
  color: var(--muted);
}

.chat-status i {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--success);
}

/* ---------- 上下文选择区 ---------- */
.context-bar {
  padding: 14px 20px;
  flex-shrink: 0;
  background: var(--surface-2);
  border-bottom: 1px solid var(--line);
}

.context-label {
  display: block;
  margin-bottom: 8px;
  font-size: 12.5px;
  color: var(--muted);
}

.context-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.context-tag {
  height: 30px;
  padding: 0 12px;
  border-radius: 6px;
  font-size: 12.5px;
  cursor: pointer;
}

.context-select {
  margin-top: 10px;
  max-width: 360px;
}

.context-select .context-label {
  margin-bottom: 6px;
}

/* ---------- 消息列表 ---------- */
.message-list {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow-y: auto;
  padding: 20px;
  -webkit-overflow-scrolling: touch;
}

/* 欢迎区 */
.welcome-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 24px 20px;
  text-align: center;
}

.welcome-area > .el-icon {
  width: 56px;
  height: 56px;
  display: grid;
  place-items: center;
  background: var(--teal-soft);
  border: 1px solid var(--teal-light);
  border-radius: 12px;
  color: var(--teal-deep);
}

.welcome-title {
  margin: 16px 0 8px;
  font-size: 20px;
  font-weight: 600;
  color: var(--ink);
}

.welcome-desc {
  margin: 0 0 8px;
  font-size: 13.5px;
  line-height: 1.7;
  color: var(--muted);
}

.welcome-hint {
  margin: 0 0 22px;
  font-size: 12px;
  line-height: 1.6;
  color: var(--muted-2);
}

.quick-questions {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
  width: 100%;
  max-width: 760px;
}

.quick-item {
  appearance: none;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 13px 14px;
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: 8px;
  font-family: inherit;
  font-size: 13px;
  line-height: 1.5;
  color: var(--ink-3);
  text-align: left;
  cursor: pointer;
  transition: background-color 0.16s ease, border-color 0.16s ease,
    color 0.16s ease;
}

.quick-item:hover {
  background: var(--teal-soft);
  border-color: var(--teal-light);
  color: var(--teal-deep);
}

.quick-icon {
  width: 30px;
  height: 30px;
  flex-shrink: 0;
  display: grid;
  place-items: center;
  background: var(--teal-soft);
  border-radius: 6px;
  color: var(--teal-deep);
}

.quick-text {
  flex: 1;
}

/* 消息行 */
.message-row {
  display: flex;
  gap: 10px;
  margin-bottom: 16px;
}

.msg-user {
  flex-direction: row-reverse;
}

.msg-avatar {
  width: 32px;
  height: 32px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  background: var(--teal-soft);
  color: var(--teal-deep);
}

.msg-user .msg-avatar {
  background: var(--teal-deep);
  color: #fff;
}

/* 消息气泡 */
.msg-bubble {
  max-width: 72%;
  padding: 11px 14px;
  border-radius: 10px;
}

.bubble-assistant {
  background: var(--surface);
  border: 1px solid var(--line);
  border-top-left-radius: 2px;
  box-shadow: var(--shadow-sm);
}

.bubble-user {
  background: var(--teal-deep);
  color: #fff;
  border-top-right-radius: 2px;
}

.bubble-text {
  font-size: 14px;
  line-height: 1.75;
  word-break: break-word;
}

.bubble-text :deep(p) {
  margin: 0 0 8px;
}

.bubble-text :deep(p:last-child) {
  margin-bottom: 0;
}

.bubble-text :deep(strong) {
  font-weight: 600;
}

.bubble-time {
  margin-top: 5px;
  font-size: 11px;
  color: var(--muted-2);
  text-align: right;
}

.bubble-user .bubble-time {
  color: rgba(255, 255, 255, 0.68);
}

/* 流式文字 */
.streaming-text {
  display: inline;
}

.typing-cursor {
  display: inline;
  font-size: 14px;
  font-weight: 600;
  color: var(--teal-deep);
  animation: blink 0.8s infinite;
}

@keyframes blink {
  0%,
  50% {
    opacity: 1;
  }
  51%,
  100% {
    opacity: 0;
  }
}

/* 思考中动画 */
.thinking-bubble {
  min-width: 190px;
}

.thinking-line {
  display: flex;
  align-items: center;
  gap: 10px;
}

.thinking-dots {
  display: inline-flex;
  gap: 5px;
}

.thinking-dots i {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--teal-deep);
  animation: thinking-bounce 1.2s infinite ease-in-out;
}

.thinking-dots i:nth-child(2) {
  animation-delay: 0.15s;
}

.thinking-dots i:nth-child(3) {
  animation-delay: 0.3s;
}

@keyframes thinking-bounce {
  0%,
  80%,
  100% {
    transform: translateY(0);
    opacity: 0.35;
  }
  40% {
    transform: translateY(-4px);
    opacity: 1;
  }
}

.thinking-label {
  font-size: 12.5px;
  font-weight: 500;
  color: var(--teal-deep);
}

/* 思考过程面板 */
.reasoning-panel {
  margin-bottom: 9px;
  padding-left: 9px;
  border-left: 2px solid var(--teal-light);
}

.reasoning-header {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12.5px;
  color: var(--muted);
  cursor: pointer;
  user-select: none;
}

.reasoning-title {
  font-weight: 500;
}

.reasoning-arrow {
  display: inline-block;
  transition: transform 0.2s ease;
}

.reasoning-arrow.open {
  transform: rotate(180deg);
}

.reasoning-text {
  margin-top: 6px;
  max-height: 220px;
  overflow-y: auto;
  font-size: 12.5px;
  line-height: 1.7;
  color: var(--muted-2);
  white-space: pre-wrap;
  word-break: break-word;
}

.reasoning-text.live {
  margin-top: 8px;
  max-height: 260px;
}

/* 免责声明 */
.disclaimer-bar {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  margin: 8px 0 16px;
  padding: 8px 12px;
  font-size: 12px;
  color: var(--amber);
  background: var(--amber-soft);
  border-radius: 6px;
  text-align: center;
}

/* 底部输入区 */
.input-bar {
  flex-shrink: 0;
  padding: 12px 18px calc(12px + env(safe-area-inset-bottom));
  background: var(--surface);
  border-top: 1px solid var(--line);
}

.input-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.msg-input {
  flex: 1;
}

.msg-input :deep(.el-input__inner) {
  height: 44px;
  font-size: 14px;
  border-radius: 8px;
}

.send-btn {
  width: 44px;
  height: 44px;
  flex-shrink: 0;
  border-radius: 8px;
}

/* 适配选择器 */
:deep(.el-select) {
  width: 100%;
}

:deep(.el-select .el-input__inner) {
  height: 40px;
  font-size: 13.5px;
}

@media (max-width: 1000px) {
  .chat-main {
    padding: 0;
  }

  .chat-shell {
    border-radius: 0;
    border-left: none;
    border-right: none;
    border-bottom: none;
  }

  .chat-status {
    display: none;
  }

  .message-list {
    padding: 16px;
  }

  .msg-bubble {
    max-width: 82%;
  }

  .context-bar {
    padding: 12px 16px;
  }

  .input-bar {
    padding: 10px 14px calc(10px + env(safe-area-inset-bottom));
  }
}

@media (max-width: 700px) {
  .quick-questions {
    grid-template-columns: 1fr;
    max-width: 360px;
  }
}

@media (max-width: 560px) {
  .welcome-area {
    padding: 20px 12px;
  }

  .welcome-title {
    font-size: 18px;
  }

  .bubble-text {
    font-size: 13.5px;
  }
}

</style>
