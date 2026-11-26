<template>
  <div class="ai-chat-page">
    <FamilyTopNav />

    <main class="page-wrap">
      <button type="button" class="mobile-back" @click="$router.push('/family/home')">
        <el-icon :size="15"><ArrowLeft /></el-icon>
        返回工作台
      </button>

      <header class="page-intro rv-fade">
        <div>
          <h1 class="page-heading">AI 健康助手</h1>
          <p class="page-sub">围绕就诊流程、检查报告和用药注意事项，随时向 AI 助手提问。</p>
        </div>
        <el-button type="primary" @click="$router.push('/family/ai-chat/new')">
          <el-icon :size="16"><Plus /></el-icon>
          新建咨询
        </el-button>
      </header>

      <!-- 7:5 分栏 -->
      <div class="chat-layout grid-12">
        <!-- 左：最近咨询 -->
        <section class="session-panel surface-card col-7 rv-rise" style="--i: 0">
          <div class="panel-head">
            <div>
              <h2 class="text-h2">最近咨询</h2>
              <p class="text-note">共 {{ sessions.length }} 条会话</p>
            </div>
          </div>

          <div v-if="loading" class="skeleton-wrap">
            <el-skeleton :rows="5" animated />
          </div>

          <div v-else-if="sessions.length > 0" class="session-list">
            <div
              v-for="(session, index) in sessions"
              :key="session.id"
              role="button"
              tabindex="0"
              class="session-row rv-rise"
              :style="{ '--i': index }"
              @click="$router.push(`/family/ai-chat/${session.id}`)"
              @keydown.enter="$router.push(`/family/ai-chat/${session.id}`)"
            >
              <span class="session-icon" aria-hidden="true">
                <el-icon :size="17"><ChatDotRound /></el-icon>
              </span>
              <span class="session-main">
                <strong>{{ session.title }}</strong>
                <em>{{ session.lastMessage || '点击开始对话' }}</em>
              </span>
              <span class="session-time">{{ formatTime(session.updateTime) }}</span>
              <el-button
                class="delete-btn"
                :icon="Delete"
                text
                size="small"
                aria-label="删除会话"
                @click.stop="handleDelete(session.id)"
              />
            </div>
          </div>

          <div v-else class="empty-state">
            <el-icon :size="26" class="empty-icon"><ChatDotRound /></el-icon>
            <h2 class="text-h2">还未开始对话</h2>
            <p class="text-note">点击「新建咨询」，向 AI 健康助手咨询就诊相关问题。</p>
            <el-button type="primary" plain @click="$router.push('/family/ai-chat/new')">
              新建咨询
            </el-button>
          </div>
        </section>

        <!-- 右：推荐提问 + 老人档案 + 使用提示 -->
        <aside class="side-column col-5">
          <section class="prompt-panel surface-card rv-rise" style="--i: 1">
            <h2 class="text-h2">推荐提问</h2>
            <p class="text-note">点击即可带着问题开始新咨询</p>
            <div class="prompt-list">
              <button
                v-for="question in recommendedQuestions"
                :key="question"
                type="button"
                class="prompt-btn"
                @click="startQuestion(question)"
              >
                <span>{{ question }}</span>
                <el-icon :size="14"><ArrowRight /></el-icon>
              </button>
            </div>
          </section>

          <section class="elderly-panel surface-card rv-rise" style="--i: 2">
            <div class="panel-head compact">
              <div>
                <h2 class="text-h2">老人档案</h2>
                <p class="text-note">选择老人，带着档案信息咨询</p>
              </div>
              <button type="button" class="text-action" @click="$router.push('/family/elderly')">
                管理
              </button>
            </div>

            <div v-if="elderlyList.length > 0" class="elderly-quick-list">
              <button
                v-for="elderly in elderlyList"
                :key="elderly.id"
                type="button"
                class="elderly-quick-row"
                @click="startElderlyChat(elderly)"
              >
                <span class="quick-avatar">{{ elderly.name?.charAt(0) || '老' }}</span>
                <span class="quick-info">
                  <strong>{{ elderly.name }}</strong>
                  <em>{{ mobilityLabel(elderly.mobility) }}</em>
                </span>
                <el-icon :size="14" class="quick-arrow"><ArrowRight /></el-icon>
              </button>
            </div>

            <div v-else class="mini-empty">
              <p>暂无老人档案，先去添加一位家人。</p>
              <el-button type="primary" plain size="small" @click="$router.push('/family/elderly/add')">
                去添加
              </el-button>
            </div>

            <p class="panel-note">
              AI 会结合老人档案与就医记录进行回答，内容仅供参考，不构成任何诊疗建议。
            </p>
          </section>

          <section class="tips-panel surface-card rv-rise" style="--i: 3">
            <h2 class="text-h2">AI 使用提示</h2>
            <ul class="tips-list">
              <li>描述症状时说明持续时间、用药和既往病史。</li>
              <li>可先选择关联老人或就医记录，回答会更贴合。</li>
              <li>紧急或持续加重的症状，请立即前往医院。</li>
            </ul>
          </section>
        </aside>
      </div>

      <div class="chat-disclaimer">
        <el-icon :size="15"><WarningFilled /></el-icon>
        <span>AI生成内容仅供参考，不构成任何诊疗建议</span>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import {
  ArrowLeft, ArrowRight, ChatDotRound, Plus, Delete, WarningFilled
} from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listSessions, deleteSession } from '@/api/aiChat'
import api from '@/api/request'
import FamilyTopNav from '@/components/FamilyTopNav.vue'

const router = useRouter()
const sessions = ref([])
const elderlyList = ref([])
const loading = ref(true)

const recommendedQuestions = [
  '去心内科就诊需要准备什么？',
  '血糖偏高平时要注意什么？',
  '高血压吃药有哪些注意事项？',
  '复诊前需要整理哪些资料？'
]

const mobilityLabel = (mobility) => {
  const map = { WALK: '自主行走', ASSIST: '需搀扶', WHEELCHAIR: '轮椅' }
  return map[mobility] || '行动能力未填写'
}

const formatTime = (time) => {
  if (!time) return ''
  const d = new Date(time)
  const now = new Date()
  const diff = now - d
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return Math.floor(diff / 60000) + '分钟前'
  if (diff < 86400000) return Math.floor(diff / 3600000) + '小时前'
  const month = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return month + '/' + day
}

const loadSessions = async () => {
  loading.value = true
  try {
    const res = await listSessions()
    if (res.code === 200 && res.data) {
      sessions.value = res.data
    }
  } catch (e) {
    console.error('Failed to load sessions:', e)
  } finally {
    loading.value = false
  }
}

const loadElderly = async () => {
  try {
    const res = await api.get('/elderly/page', { params: { current: 1, size: 1000 } })
    if (res.code === 200 && res.data) {
      elderlyList.value = res.data.records || []
    }
  } catch (e) {
    elderlyList.value = []
  }
}

const startQuestion = (question) => {
  router.push({ path: '/family/ai-chat/new', query: { q: question } })
}

const startElderlyChat = (elderly) => {
  router.push({
    path: '/family/ai-chat/new',
    query: { contextType: 'ELDERLY', contextId: elderly.id }
  })
}

const handleDelete = async (id) => {
  try {
    await ElMessageBox.confirm('确定要删除该对话吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await deleteSession(id)
    sessions.value = sessions.value.filter(s => s.id !== id)
    ElMessage.success('已删除')
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

onMounted(() => {
  loadSessions()
  loadElderly()
})
</script>

<style scoped>
.ai-chat-page {
  min-height: 100vh;
  background: var(--bg);
  padding-bottom: 40px;
}

.page-wrap {
  width: min(1240px, 100%);
  margin: 0 auto;
  padding: 28px 24px;
}

.mobile-back {
  display: none;
  align-items: center;
  gap: 4px;
  margin-bottom: 14px;
  padding: 0;
  background: none;
  border: none;
  font-family: inherit;
  font-size: 13px;
  color: var(--muted);
  cursor: pointer;
}

.page-intro {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 20px;
  margin-bottom: 24px;
}

.page-heading {
  margin: 0;
  font-size: clamp(24px, 2.6vw, 30px);
  font-weight: 700;
  line-height: 1.3;
  letter-spacing: -0.03em;
  color: var(--ink);
}

.page-sub {
  margin: 8px 0 0;
  font-size: 14px;
  line-height: 1.6;
  color: var(--muted);
}

/* 7:5 分栏，12 栅格 */
.chat-layout {
  align-items: stretch;
}

.session-panel {
  height: clamp(560px, 62vh, 660px);
  display: flex;
  flex-direction: column;
  min-height: 0;
  padding: 18px 20px;
}

.panel-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--line);
}

.panel-head .text-note {
  margin-top: 3px;
}

.skeleton-wrap {
  padding: 18px 0;
}

.session-list {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  display: grid;
  align-content: start;
  gap: 8px;
  padding-top: 12px;
}

/* 会话项：轻卡片 */
.session-row {
  display: grid;
  grid-template-columns: 34px minmax(0, 1fr) auto 28px;
  align-items: center;
  gap: 10px;
  min-height: 62px;
  padding: 11px 12px;
  background: var(--surface-2);
  border: 1px solid var(--line);
  border-radius: 8px;
  font-family: inherit;
  text-align: left;
  cursor: pointer;
  transition: background-color 0.16s ease, border-color 0.16s ease;
}

.session-row:hover {
  background: var(--surface);
  border-color: var(--teal-light);
}

.session-icon {
  width: 34px;
  height: 34px;
  display: grid;
  place-items: center;
  background: var(--teal-soft);
  border-radius: 8px;
  color: var(--teal-deep);
}

.session-main {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.session-main strong {
  font-size: 13.5px;
  font-weight: 600;
  color: var(--ink);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.session-main em {
  font-style: normal;
  font-size: 12px;
  color: var(--muted);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.session-time {
  font-size: 11px;
  color: var(--muted-2);
  white-space: nowrap;
}

.delete-btn {
  color: var(--muted-2);
}

.delete-btn:hover {
  color: var(--danger);
  background: var(--danger-soft);
}

.empty-state {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 30px 20px;
  text-align: center;
}

.empty-icon {
  width: 48px;
  height: 48px;
  display: grid;
  place-items: center;
  background: var(--teal-soft);
  border: 1px solid var(--teal-light);
  border-radius: 10px;
  color: var(--teal-deep);
  margin-bottom: 4px;
}

.empty-state p {
  margin-bottom: 10px;
}

/* 右侧 */
.side-column {
  height: clamp(560px, 62vh, 660px);
  display: flex;
  flex-direction: column;
  gap: 14px;
  min-height: 0;
}

.prompt-panel {
  flex: 0 0 auto;
  padding: 18px 20px;
}

.prompt-list {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
  margin-top: 12px;
}

.prompt-btn {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  min-height: 44px;
  padding: 9px 10px;
  background: var(--surface-2);
  border: 1px solid var(--line);
  border-radius: 8px;
  font-family: inherit;
  font-size: 12.5px;
  line-height: 1.5;
  color: var(--ink-3);
  text-align: left;
  cursor: pointer;
  transition: background-color 0.16s ease, border-color 0.16s ease,
    color 0.16s ease;
}

.prompt-btn:hover {
  background: var(--teal-soft);
  border-color: var(--teal-light);
  color: var(--teal-deep);
}

.elderly-panel {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  padding: 18px 20px;
}

.panel-head.compact {
  padding-bottom: 10px;
}

.text-action {
  flex-shrink: 0;
  padding: 0;
  background: none;
  border: none;
  font-family: inherit;
  font-size: 12px;
  font-weight: 600;
  color: var(--teal-deep);
  cursor: pointer;
}

.text-action:hover {
  text-decoration: underline;
  text-underline-offset: 3px;
}

.elderly-quick-list {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  display: grid;
  align-content: start;
  gap: 8px;
  padding: 12px 0;
}

.elderly-quick-row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 11px;
  background: var(--surface-2);
  border: 1px solid var(--line);
  border-radius: 8px;
  font-family: inherit;
  text-align: left;
  cursor: pointer;
  transition: background-color 0.16s ease, border-color 0.16s ease;
}

.elderly-quick-row:hover {
  background: var(--surface);
  border-color: var(--teal-light);
}

.quick-avatar {
  width: 34px;
  height: 34px;
  flex-shrink: 0;
  display: grid;
  place-items: center;
  background: var(--teal-soft);
  border-radius: 8px;
  font-size: 15px;
  font-weight: 700;
  color: var(--teal-deep);
}

.quick-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.quick-info strong {
  font-size: 13.5px;
  font-weight: 600;
  color: var(--ink);
}

.quick-info em {
  font-style: normal;
  font-size: 11.5px;
  color: var(--muted);
}

.quick-arrow {
  margin-left: auto;
  color: var(--muted-2);
}

.mini-empty {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 24px 10px;
  text-align: center;
}

.mini-empty p {
  margin: 0;
  font-size: 12.5px;
  color: var(--muted);
}

.panel-note {
  margin: auto 0 0;
  padding-top: 10px;
  border-top: 1px dashed var(--line);
  font-size: 12px;
  line-height: 1.65;
  color: var(--muted-2);
}

.tips-panel {
  flex: 0 0 auto;
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 16px 20px;
}

.tips-list {
  display: grid;
  gap: 6px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.tips-list li {
  position: relative;
  padding-left: 11px;
  font-size: 12px;
  line-height: 1.65;
  color: var(--muted);
}

.tips-list li::before {
  content: "";
  position: absolute;
  top: 7px;
  left: 0;
  width: 4px;
  height: 4px;
  border-radius: 50%;
  background: var(--teal);
}

.chat-disclaimer {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  margin-top: 20px;
  padding: 10px 14px;
  background: var(--amber-soft);
  border-radius: 8px;
  font-size: 12px;
  line-height: 1.6;
  color: var(--amber);
  text-align: center;
}

@media (max-width: 1100px) {
  .session-panel {
    height: 520px;
    grid-column: span 12;
  }

  .side-column {
    height: auto;
    grid-column: span 12;
  }

  .elderly-panel {
    min-height: 320px;
  }
}

@media (max-width: 1000px) {
  .page-wrap {
    padding: 20px 16px 40px;
  }

  .mobile-back {
    display: inline-flex;
  }

  .page-intro {
    align-items: flex-start;
  }
}

@media (max-width: 700px) {
  .page-intro {
    flex-direction: column;
  }

  .page-intro .el-button {
    width: 100%;
    height: 44px;
  }

  .session-panel {
    height: 460px;
    padding: 16px;
  }

  .session-row {
    grid-template-columns: 34px minmax(0, 1fr) 28px;
  }

  .session-time {
    display: none;
  }

  .prompt-list {
    grid-template-columns: 1fr;
  }

  .prompt-panel,
  .elderly-panel,
  .tips-panel {
    padding: 16px;
  }

  .chat-disclaimer {
    align-items: flex-start;
    text-align: left;
  }
}
</style>
