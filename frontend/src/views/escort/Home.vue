<template>
  <div class="escort-home">
    <header class="app-bar">
      <div class="app-bar-inner">
        <div class="brand">
          <span class="brand-mark" aria-hidden="true">
            <svg viewBox="0 0 24 24" width="19" height="19" fill="none">
              <path d="M12 20s-7-4.4-7-9.2C5 8 7 6 9.3 6c1.2 0 2.1.5 2.7 1.4C12.6 6.5 13.5 6 14.7 6 17 6 19 8 19 10.8 19 15.6 12 20 12 20z" fill="#fff" />
              <path d="M12 9v5M9.5 11.5h5" stroke="#2F5A85" stroke-width="2" stroke-linecap="round" />
            </svg>
          </span>
          <span class="brand-name">安心陪</span>
          <span class="brand-divider" aria-hidden="true"></span>
          <span class="brand-sub">陪诊师工作台</span>
        </div>
        <div class="app-bar-actions">
          <el-button v-if="statusInfo" text class="logout-btn" @click="handleLogout">
            退出登录
          </el-button>
        </div>
      </div>
    </header>

    <main class="page">
      <!-- Loading -->
      <div v-if="loading" class="state-card">
        <el-skeleton :rows="5" animated />
      </div>

      <!-- No profile yet -->
      <div v-else-if="!statusInfo" class="state-card rv-rise">
        <el-result
          icon="warning"
          title="请先完成资质认证"
          sub-title="您还未提交陪诊员资质认证，请先完成认证后再使用系统。"
        >
          <template #extra>
            <el-button type="primary" @click="$router.push('/escort/apply')">
              去认证
            </el-button>
          </template>
        </el-result>
      </div>

      <!-- PENDING -->
      <div v-else-if="statusInfo.auditStatus === 'PENDING'" class="state-card rv-rise">
        <el-result
          icon="info"
          title="资质审核中"
          sub-title="您的资质正在审核中，请耐心等待。审核通过后即可开始接单。"
        >
          <template #extra>
            <el-button type="primary" plain disabled>审核中…</el-button>
          </template>
        </el-result>
      </div>

      <!-- REJECTED -->
      <div v-else-if="statusInfo.auditStatus === 'REJECTED'" class="state-card rv-rise">
        <el-result icon="error" title="资质审核未通过">
          <template #sub-title>
            <span style="color: var(--danger)">{{ statusInfo.auditRemark || '您的资质审核未通过，请重新提交认证信息。' }}</span>
          </template>
          <template #extra>
            <el-button type="primary" @click="$router.push('/escort/apply')">
              重新认证
            </el-button>
          </template>
        </el-result>
      </div>

      <!-- APPROVED -->
      <template v-else-if="statusInfo.auditStatus === 'APPROVED'">
        <!-- Welcome -->
        <section class="welcome rv-rise" style="--i: 0">
          <div class="welcome-main">
            <el-avatar :size="64" class="welcome-avatar">
              <el-icon :size="30"><UserFilled /></el-icon>
            </el-avatar>
            <div class="welcome-copy">
              <span class="welcome-label">已认证陪诊师</span>
              <h1>欢迎回来，{{ statusInfo.realName }}</h1>
              <div class="rating-row">
                <el-rate
                  :model-value="statusInfo.avgRating || 0"
                  disabled
                  show-score
                  text-color="var(--star)"
                />
                <span class="rating-text">{{ (statusInfo.avgRating || 0).toFixed(1) }} 分</span>
              </div>
            </div>
          </div>
          <div class="welcome-side">
            <div class="accepting-toggle">
              <span :class="['status-chip', { paused: !acceptingOrders }]">
                <i class="status-dot" :class="{ paused: !acceptingOrders }" aria-hidden="true"></i>
                {{ acceptingOrders ? '接单中' : '已暂停接单' }}
              </span>
              <el-switch v-model="acceptingOrders" size="small" @change="handleAcceptingChange" />
            </div>
            <p>{{ acceptingOrders ? '保持手机畅通，及时响应新的陪诊需求。' : '已暂停接单，开启后可继续查看接单大厅。' }}</p>
          </div>
        </section>

        <!-- 工作概览 -->
        <section class="metrics-grid" aria-label="工作概览">
          <div class="metric-card surface-card rv-rise" style="--i: 1">
            <span class="metric-icon"><el-icon :size="20"><Checked /></el-icon></span>
            <div class="metric-copy">
              <span class="metric-label">服务资质</span>
              <strong class="metric-value">已认证</strong>
            </div>
          </div>
          <div class="metric-card surface-card rv-rise" style="--i: 2">
            <span class="metric-icon"><el-icon :size="20"><Calendar /></el-icon></span>
            <div class="metric-copy">
              <span class="metric-label">今日待办</span>
              <strong class="metric-value">{{ todayPendingCount }} 单</strong>
            </div>
          </div>
          <div class="metric-card surface-card rv-rise" style="--i: 3">
            <span class="metric-icon"><el-icon :size="20"><Star /></el-icon></span>
            <div class="metric-copy">
              <span class="metric-label">平均评分</span>
              <strong class="metric-value">{{ ratingText }} 分</strong>
            </div>
          </div>
        </section>

        <!-- Quick Actions -->
        <section class="quick-actions" aria-label="快捷操作">
          <button
            type="button"
            class="action-card rv-rise"
            style="--i: 1"
            @click="$router.push('/escort/pool')"
          >
            <span class="action-icon tone-teal"><el-icon :size="25"><List /></el-icon></span>
            <span class="action-copy">
              <strong>接单大厅</strong>
              <em>查看附近待接的陪诊订单</em>
            </span>
            <el-icon class="action-arrow" :size="16"><ArrowRight /></el-icon>
          </button>

          <button
            type="button"
            class="action-card rv-rise"
            style="--i: 2"
            @click="$router.push('/escort/orders')"
          >
            <span class="action-icon tone-teal"><el-icon :size="25"><Document /></el-icon></span>
            <span class="action-copy">
              <strong>我的订单</strong>
              <em>跟进正在服务的订单进度</em>
            </span>
            <el-icon class="action-arrow" :size="16"><ArrowRight /></el-icon>
          </button>

          <button
            type="button"
            class="action-card rv-rise"
            style="--i: 3"
            @click="$router.push('/escort/statistics')"
          >
            <span class="action-icon tone-amber"><el-icon :size="25"><TrendCharts /></el-icon></span>
            <span class="action-copy">
              <strong>收入统计</strong>
              <em>查看服务记录与收入明细</em>
            </span>
            <el-icon class="action-arrow" :size="16"><ArrowRight /></el-icon>
          </button>
        </section>

        <!-- Recent Notifications -->
        <section v-if="notifications.length > 0" class="notifications rv-rise" style="--i: 5">
          <div class="section-head">
            <div>
              <h2>消息通知</h2>
              <p>订单、公告与服务提醒</p>
            </div>
            <div class="section-actions">
              <span class="count-chip">{{ notifications.length }}</span>
              <el-button text @click="$router.push('/escort/notify')">查看全部</el-button>
            </div>
          </div>
          <div class="notification-list">
            <button
              v-for="item in notifications"
              :key="item.id"
              type="button"
              class="notification-row"
              @click="openNotification(item)"
            >
              <span class="notify-icon" aria-hidden="true">
                <el-icon :size="16"><component :is="notificationIcon(item.bizType)" /></el-icon>
              </span>
              <span class="notify-main">
                <span class="notify-title">{{ item.title }}</span>
                <span class="notify-text">{{ item.content }}</span>
              </span>
              <span class="notify-meta">
                <span class="notify-type">{{ notificationTypeLabel(item.bizType) }}</span>
                <span class="notify-time">{{ formatTime(item.time) }}</span>
              </span>
            </button>
          </div>
        </section>
      </template>
    </main>

    <!-- 底部导航 -->
    <nav v-if="statusInfo" class="bottom-nav" aria-label="主导航">
      <button type="button" class="nav-item active" @click="$router.push('/escort/home')">
        <el-icon :size="20"><HomeFilled /></el-icon>
        <span>工作台</span>
      </button>
      <button type="button" class="nav-item" @click="$router.push('/escort/pool')">
        <el-icon :size="20"><List /></el-icon>
        <span>接单大厅</span>
      </button>
      <button type="button" class="nav-item" @click="$router.push('/escort/orders')">
        <el-icon :size="20"><Document /></el-icon>
        <span>我的订单</span>
      </button>
      <button type="button" class="nav-item" @click="$router.push('/escort/statistics')">
        <el-icon :size="20"><TrendCharts /></el-icon>
        <span>收入统计</span>
      </button>
    </nav>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store/user'
import api from '@/api/request'
import {
  List, Document, TrendCharts, Bell, ArrowRight, HomeFilled, UserFilled,
  Checked, Calendar, Star
} from '@element-plus/icons-vue'

const router = useRouter()
const userStore = useUserStore()
const loading = ref(true)
const statusInfo = ref(null)
const notifications = ref([])
const orders = ref([])
const acceptingOrders = ref(localStorage.getItem('escort_accepting') !== 'false')

const ratingText = computed(() => Number(statusInfo.value?.avgRating || 0).toFixed(1))

const todayPendingCount = computed(() => {
  const d = new Date()
  const key = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
  return orders.value.filter(o =>
    ['DISPATCHED', 'ACCEPTED', 'IN_SERVICE'].includes(o.status) &&
    String(o.appointmentTime || '').startsWith(key)
  ).length
})

const handleAcceptingChange = (value) => {
  localStorage.setItem('escort_accepting', String(value))
  ElMessage.success(value ? '已开启接单' : '已暂停接单')
}

const notificationTypeLabel = (bizType) => {
  const map = { ORDER: '订单', SETTLEMENT: '结算', ANNOUNCEMENT: '公告' }
  return map[bizType] || '提醒'
}

const notificationIcon = (bizType) => {
  if (bizType === 'SETTLEMENT') return TrendCharts
  if (bizType === 'ANNOUNCEMENT') return Bell
  return Document
}

const formatTime = (time) => {
  if (!time) return ''
  const d = new Date(time)
  if (Number.isNaN(d.getTime())) return String(time)
  return `${d.getMonth() + 1}/${d.getDate()} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}

const openNotification = (item) => {
  if (item.orderId) {
    router.push(`/escort/order/${item.orderId}`)
  } else {
    router.push('/escort/notify')
  }
}

onMounted(async () => {
  try {
    const res = await api.get('/escort/apply/status')
    statusInfo.value = res.data
  } catch (e) {
    statusInfo.value = null
  } finally {
    loading.value = false
  }

  try {
    const [orderRes, notiRes] = await Promise.all([
      api.get('/order/page', { params: { page: 1, size: 50 } }),
      api.get('/notification/page', { params: { current: 1, size: 5 } }).catch(() => null)
    ])
    orders.value = orderRes.data?.records || []

    const notifyRecords = notiRes?.data?.records || []
    if (notifyRecords.length > 0) {
      notifications.value = notifyRecords.map(n => ({
        id: n.id,
        title: n.title || '消息通知',
        content: n.content || '',
        time: n.createTime,
        bizType: n.bizType,
        orderId: n.bizType === 'ORDER' ? n.bizId : null
      }))
    } else {
      notifications.value = orders.value.slice(0, 5).map(o => ({
        id: `order-${o.id}`,
        title: '订单通知',
        content: `${o.orderNo} · ${o.elderlyName || '就诊人'} · ${o.appointmentTime || ''}`,
        time: o.createTime,
        bizType: 'ORDER',
        orderId: o.id
      }))
    }
  } catch (e) {
    notifications.value = []
  }
})

const handleLogout = () => { userStore.logout(); router.push('/login') }
</script>

<style scoped>
.escort-home {
  min-height: 100vh;
  padding-bottom: 104px;
}

/* ---------- 顶栏 ---------- */
.app-bar {
  position: sticky;
  top: 0;
  z-index: 30;
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(12px);
  border-bottom: 1px solid var(--line);
}

.app-bar-inner {
  width: min(980px, 100%);
  margin: 0 auto;
  padding: 0 24px;
  height: 62px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.brand {
  display: flex;
  align-items: center;
  gap: 9px;
}

.brand-mark {
  width: 32px;
  height: 32px;
  display: grid;
  place-items: center;
  background: var(--teal);
  border-radius: 9px;
}

.brand-name {
  font-size: 15px;
  font-weight: 700;
  color: var(--ink);
}

.brand-divider {
  width: 1px;
  height: 18px;
  background: var(--line);
}

.brand-sub {
  font-size: 12.5px;
  font-weight: 500;
  color: var(--muted);
}

.logout-btn {
  font-size: 13px;
  color: var(--muted);
  font-weight: 500;
}

.logout-btn:hover {
  color: var(--danger);
  background: var(--danger-soft);
}

/* ---------- 页面 ---------- */
.page {
  width: min(980px, 100%);
  margin: 0 auto;
  padding: 26px 24px 0;
}

.state-card {
  padding: 24px;
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-card);
}

/* ---------- 欢迎区 ---------- */
.welcome {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  padding: 24px;
  background: linear-gradient(120deg, var(--teal-soft) 0%, var(--surface) 62%);
  border: 1px solid var(--line);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-card);
}

.welcome-main {
  display: flex;
  align-items: center;
  gap: 16px;
  min-width: 0;
}

.welcome-avatar {
  flex-shrink: 0;
  background: var(--teal);
  border: 3px solid #fff;
  box-shadow: 0 0 0 1px var(--teal-light);
}

.welcome-copy {
  min-width: 0;
}

.welcome-label {
  font-size: 11.5px;
  font-weight: 600;
  letter-spacing: 0.06em;
  color: var(--teal-deep);
}

.welcome-copy h1 {
  margin: 5px 0 6px;
  font-size: clamp(21px, 3vw, 27px);
  font-weight: 700;
  letter-spacing: -0.02em;
  color: var(--ink);
}

.rating-row {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.rating-text {
  font-size: 12.5px;
  font-weight: 600;
  color: var(--muted);
}

.welcome-side {
  flex-shrink: 0;
  max-width: 240px;
  text-align: right;
}

.status-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 5px 12px;
  background: var(--success-soft);
  border: 1px solid var(--success-soft);
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
  color: var(--success);
}

.status-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--success);
  animation: rv-pulse 2.2s ease-in-out infinite;
}

.status-chip.paused {
  background: var(--surface-3);
  border-color: var(--line);
  color: var(--muted);
}

.status-dot.paused {
  background: var(--muted-2);
  animation: none;
}

.accepting-toggle {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
}

.welcome-side p {
  margin: 9px 0 0;
  font-size: 12.5px;
  line-height: 1.6;
  color: var(--muted);
}

/* ---------- 工作概览 ---------- */
.metrics-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
  margin-top: 14px;
}

.metric-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px 18px;
}

.metric-icon {
  width: 38px;
  height: 38px;
  flex-shrink: 0;
  display: grid;
  place-items: center;
  background: var(--teal-soft);
  border-radius: 8px;
  color: var(--teal-deep);
}

.metric-copy {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.metric-label {
  font-size: 12px;
  color: var(--muted);
}

.metric-value {
  font-size: 16px;
  font-weight: 600;
  color: var(--ink);
  font-variant-numeric: tabular-nums;
}

/* ---------- 快捷操作 ---------- */
.quick-actions {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  margin: 16px 0;
}

.action-card {
  appearance: none;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 18px;
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-card);
  cursor: pointer;
  font-family: inherit;
  text-align: left;
  transition: transform 0.18s ease, border-color 0.18s ease,
    box-shadow 0.18s ease;
}

.action-card:hover {
  transform: translateY(-2px);
  border-color: var(--teal-light);
  box-shadow: var(--shadow-card);
}

.action-icon {
  width: 46px;
  height: 46px;
  flex-shrink: 0;
  display: grid;
  place-items: center;
  border-radius: 8px;
}

.tone-teal {
  background: var(--teal-soft);
  color: var(--teal-deep);
}

.tone-amber {
  background: var(--amber-soft);
  color: var(--amber);
}

.action-copy {
  display: flex;
  flex-direction: column;
  gap: 3px;
  min-width: 0;
}

.action-copy strong {
  font-size: 14px;
  font-weight: 600;
  color: var(--ink);
}

.action-copy em {
  font-style: normal;
  font-size: 12px;
  line-height: 1.5;
  color: var(--muted);
}

.action-arrow {
  margin-left: auto;
  color: var(--muted-2);
}

/* ---------- 通知 ---------- */
.notifications {
  padding: 18px 20px 6px;
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-card);
}

.section-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--line);
}

.section-head h2 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: var(--ink);
}

.section-head p {
  margin: 3px 0 0;
  font-size: 12.5px;
  color: var(--muted);
}

.section-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.count-chip {
  min-width: 24px;
  height: 24px;
  display: inline-grid;
  place-items: center;
  padding: 0 7px;
  background: var(--surface-3);
  border-radius: 6px;
  font-size: 11.5px;
  font-weight: 600;
  color: var(--ink-3);
}

.notification-list {
  display: grid;
}

.notification-row {
  appearance: none;
  display: flex;
  align-items: flex-start;
  gap: 11px;
  width: 100%;
  padding: 12px 4px;
  background: none;
  border: none;
  border-bottom: 1px solid var(--line);
  font-family: inherit;
  text-align: left;
  cursor: pointer;
  transition: background-color 0.16s ease;
}

.notification-row:last-child {
  border-bottom: none;
}

.notification-row:hover {
  background: var(--surface-2);
}

.notify-icon {
  width: 30px;
  height: 30px;
  flex-shrink: 0;
  display: grid;
  place-items: center;
  margin-top: 2px;
  background: var(--teal-soft);
  border-radius: 8px;
  color: var(--teal-deep);
}

.notify-main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.notify-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--ink);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.notify-text {
  font-size: 12px;
  color: var(--muted);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.notify-meta {
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 3px;
}

.notify-type {
  padding: 1px 6px;
  background: var(--teal-soft);
  border-radius: 4px;
  font-size: 10.5px;
  color: var(--teal-deep);
  white-space: nowrap;
}

.notify-time {
  font-size: 11px;
  color: var(--muted-2);
  white-space: nowrap;
  font-variant-numeric: tabular-nums;
}

/* ---------- 底部导航 ---------- */
.bottom-nav {
  position: fixed;
  left: 50%;
  bottom: 16px;
  transform: translateX(-50%);
  z-index: 40;
  width: min(560px, calc(100% - 28px));
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 2px;
  padding: 7px 8px calc(7px + env(safe-area-inset-bottom));
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(12px);
  border: 1px solid var(--line);
  border-radius: var(--radius-lg);
  box-shadow: 0 16px 36px -18px rgba(15, 23, 42, 0.35);
}

.nav-item {
  appearance: none;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 3px;
  padding: 7px 2px;
  background: transparent;
  border: 1px solid transparent;
  border-radius: 10px;
  font-family: inherit;
  font-size: 11px;
  font-weight: 500;
  color: var(--muted);
  cursor: pointer;
  transition: background-color 0.16s ease, color 0.16s ease;
}

.nav-item:hover {
  background: var(--surface-2);
  color: var(--teal-deep);
}

.nav-item.active {
  background: var(--teal-soft);
  border-color: var(--teal-light);
  color: var(--teal-deep);
  font-weight: 600;
}

.nav-item.active .el-icon {
  color: var(--teal-deep);
}

@media (max-width: 820px) {
  .welcome {
    flex-direction: column;
    align-items: flex-start;
  }

  .welcome-side {
    display: flex;
    align-items: center;
    gap: 10px;
    max-width: none;
    text-align: left;
  }

  .welcome-side p {
    margin: 0;
  }

  .quick-actions {
    grid-template-columns: 1fr;
  }

  .action-card {
    padding: 14px;
  }
}

@media (max-width: 560px) {
  .page,
  .app-bar-inner {
    padding-left: 16px;
    padding-right: 16px;
  }

  .brand-sub,
  .brand-divider {
    display: none;
  }

  .welcome {
    padding: 20px 18px;
  }

  .welcome-main {
    align-items: flex-start;
  }

  .welcome-side {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }

  .metrics-grid {
    grid-template-columns: 1fr;
  }

  .notify-type {
    display: none;
  }
}
</style>
