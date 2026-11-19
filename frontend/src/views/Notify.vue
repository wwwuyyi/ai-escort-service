<template>
  <div class="notify-page">
    <header class="page-head rv-fade">
      <div>
        <span class="eyebrow">消息中心</span>
        <h1 class="page-title">消息通知</h1>
      </div>
      <el-button
        v-if="list.length > 0"
        text
        class="read-all-btn"
        :disabled="list.length === 0"
        @click="markAll"
      >
        全部已读
      </el-button>
    </header>

    <div v-if="list.length === 0" class="empty-state rv-fade">
      <span class="empty-icon" aria-hidden="true">
        <el-icon :size="28"><Bell /></el-icon>
      </span>
      <p>暂时没有新消息</p>
    </div>

    <div v-else class="notify-list">
      <article
        v-for="(n, index) in list"
        :key="n.id"
        class="notify-card rv-rise"
        :class="{ unread: !n.isRead }"
        :style="{ '--i': index }"
        @click="read(n)"
      >
        <span class="notify-dot" :class="{ on: !n.isRead }" aria-hidden="true"></span>
        <div class="notify-main">
          <div class="notify-top">
            <h2>{{ n.title }}</h2>
            <el-tag v-if="!n.isRead" type="warning" size="small">未读</el-tag>
          </div>
          <p>{{ n.content }}</p>
          <time>{{ formatDateTime(n.createTime) }}</time>
        </div>
        <el-icon class="notify-arrow" :size="16"><ArrowRight /></el-icon>
      </article>
    </div>

    <div v-if="total > size" class="pagination-wrap">
      <el-pagination
        :current-page="current"
        :total="total"
        :page-size="size"
        layout="prev, pager, next"
        @current-change="p => { current = p; load() }"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Bell, ArrowRight } from '@element-plus/icons-vue'
import api from '@/api/request'
import { formatDateTime } from '@/utils/datetime'

const list = ref([])
const current = ref(1)
const size = ref(20)
const total = ref(0)

const load = async () => {
  const r = await api.get('/notification/page', {
    params: { current: current.value, size: size.value }
  }).catch(() => {})
  if (r) {
    list.value = r.data.records
    total.value = r.data.total
  }
}

const read = async n => {
  if (!n.isRead) {
    await api.put(`/notification/${n.id}/read`)
    n.isRead = 1
    ElMessage.success('已读')
  }
}

const markAll = async () => {
  await api.put('/notification/read-all')
  list.value.forEach(n => { n.isRead = 1 })
  ElMessage.success('全部已读')
}

onMounted(load)
</script>

<style scoped>
.notify-page {
  min-height: 100vh;
  background: var(--bg);
  padding-bottom: 40px;
}

.page-head {
  width: min(980px, 100%);
  margin: 0 auto;
  padding: 26px 20px 14px;
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
}

.eyebrow {
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
  color: var(--teal-deep);
}

.page-head .page-title {
  margin: 7px 0 0;
  line-height: 1.2;
}

.read-all-btn {
  font-size: 13px;
  font-weight: 600;
  color: var(--teal-deep);
}

.notify-list {
  width: min(980px, 100%);
  margin: 0 auto;
  padding: 0 20px;
  display: grid;
  gap: 10px;
}

.notify-card {
  position: relative;
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 16px 18px 16px 20px;
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
  cursor: pointer;
  transition: transform 0.18s ease, border-color 0.18s ease,
    box-shadow 0.18s ease;
}

.notify-card:hover {
  transform: translateY(-2px);
  border-color: var(--teal-light);
  box-shadow: var(--shadow-card);
}

.notify-card.unread {
  border-left: 3px solid var(--teal);
}

.notify-dot {
  width: 8px;
  height: 8px;
  margin-top: 7px;
  flex-shrink: 0;
  border-radius: 50%;
  background: var(--line);
}

.notify-dot.on {
  background: var(--teal);
  box-shadow: 0 0 0 4px var(--teal-soft);
}

.notify-main {
  flex: 1;
  min-width: 0;
}

.notify-top {
  display: flex;
  align-items: center;
  gap: 8px;
}

.notify-top h2 {
  margin: 0;
  font-size: 15px;
  font-weight: 600;
  color: var(--ink);
}

.notify-main p {
  margin: 5px 0 6px;
  font-size: 13px;
  line-height: 1.65;
  color: var(--ink-3);
}

.notify-main time {
  font-size: 11.5px;
  color: var(--muted-2);
}

.notify-arrow {
  margin-top: 4px;
  color: var(--muted-2);
}

.empty-state {
  width: min(980px, 100%);
  min-height: 58vh;
  margin: 0 auto;
  padding: 40px 20px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
}

.empty-icon {
  width: 56px;
  height: 56px;
  display: grid;
  place-items: center;
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: 16px;
  color: var(--muted);
}

.empty-state p {
  margin: 0;
  font-size: 13.5px;
  color: var(--muted);
}

.pagination-wrap {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}

@media (max-width: 560px) {
  .notify-list,
  .page-head {
    padding-left: 16px;
    padding-right: 16px;
  }

  .notify-card {
    padding: 14px 14px 14px 16px;
  }
}
</style>
