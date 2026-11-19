<template>
  <div class="family-home">
    <FamilyTopNav :notification-count="notificationCount" />

    <main class="page-wrap">
      <header class="page-intro rv-fade">
        <div>
          <h1 class="page-heading">{{ greeting }}，{{ displayName }}</h1>
          <p class="page-sub">为家人的每一次就医，安排得妥帖一点。</p>
        </div>
        <div class="intro-meta">
          <span class="intro-date">{{ todayLabel }}</span>
          <span class="intro-badge">家属工作台</span>
        </div>
      </header>

      <!-- 首页横幅 16:5 -->
      <section class="hero-banner rv-rise" style="--i: 0">
        <div class="hero-copy">
          <span class="hero-eyebrow">专业陪诊 · 全程陪同</span>
          <h2 class="hero-title">为家人预约一次<br />安心的陪诊服务</h2>
          <p class="hero-desc">实名认证陪诊师全程陪同，就医过程实时留痕，家属更放心。</p>
          <div class="hero-actions">
            <el-button type="primary" @click="$router.push('/family/order/create')">
              立即预约陪诊
              <el-icon :size="16"><ArrowRight /></el-icon>
            </el-button>
            <el-button plain @click="$router.push('/family/orders')">查看我的订单</el-button>
          </div>
        </div>

        <div class="hero-art" aria-hidden="true">
          <svg class="ecg-line" viewBox="0 0 320 90" fill="none" preserveAspectRatio="none">
            <path
              d="M0 48h52l9-1 7-18 9 44 9-34 8 12 6-4h38l8-1 7-17 9 40 9-32 7 10h94"
              stroke="currentColor"
              stroke-width="2"
              stroke-linecap="round"
              stroke-linejoin="round"
            />
          </svg>
          <div class="hero-badges">
            <span>实名认证</span>
            <span>全程留痕</span>
            <span>隐私保护</span>
          </div>
        </div>
      </section>

      <!-- 快捷入口 -->
      <section class="quick-grid grid-12" aria-label="快捷入口">
        <button
          v-for="(item, index) in quickActions"
          :key="item.label"
          type="button"
          class="quick-card surface-card col-3 rv-rise"
          :style="{ '--i': index + 1 }"
          @click="$router.push(item.path)"
        >
          <span class="quick-icon">
            <el-icon :size="21"><component :is="item.icon" /></el-icon>
          </span>
          <span class="quick-copy">
            <strong>{{ item.label }}</strong>
            <em>{{ item.desc }}</em>
          </span>
          <el-icon class="quick-arrow" :size="15"><ArrowRight /></el-icon>
        </button>
      </section>

      <!-- 最近订单 + 服务保障 -->
      <div class="content-grid grid-12">
        <section class="orders-panel surface-card col-7 rv-rise" style="--i: 5">
          <div class="panel-head">
            <div>
              <h2 class="text-h2">最近订单</h2>
              <p class="text-note">最近 7 条陪诊服务记录</p>
            </div>
            <el-button text class="panel-link" @click="$router.push('/family/orders')">
              查看全部
              <el-icon :size="14"><ArrowRight /></el-icon>
            </el-button>
          </div>

          <div v-if="recentOrders.length > 0" class="order-list">
            <button
              v-for="order in recentOrders"
              :key="order.id"
              type="button"
              class="order-row"
              @click="$router.push(`/family/order/${order.id}`)"
            >
              <span class="order-main">
                <strong>{{ order.elderlyName }}</strong>
                <em>{{ order.orderNo }}</em>
              </span>
              <span class="order-dept">{{ order.department || '—' }}</span>
              <span class="order-time">{{ formatDateTime(order.appointmentTime) }}</span>
              <el-tag :type="getStatusType(order.status)" :class="getOrderStatusClass(order.status)">
                {{ getStatusLabel(order.status) }}
              </el-tag>
            </button>
          </div>

          <div v-else class="empty-state">
            <el-icon :size="26" class="empty-icon"><Calendar /></el-icon>
            <p>还没有订单，先为家人预约一次陪诊吧</p>
            <el-button type="primary" plain @click="$router.push('/family/order/create')">
              去预约
            </el-button>
          </div>
        </section>

        <div class="side-column col-5">
          <aside class="assurance-panel surface-card rv-rise" style="--i: 5">
            <div>
              <h2 class="text-h2">安心服务保障</h2>
              <p class="text-note">从资质到隐私，每个环节都可追溯</p>
            </div>
            <ul class="assurance-list">
              <li v-for="item in assurances" :key="item.title">
                <span class="assurance-icon" aria-hidden="true">
                  <svg viewBox="0 0 24 24" width="15" height="15" fill="none">
                    <path d="M5 12.5l4.2 4.2L19 7" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round" />
                  </svg>
                </span>
                <div>
                  <strong>{{ item.title }}</strong>
                  <em>{{ item.desc }}</em>
                </div>
              </li>
            </ul>
          </aside>

          <aside class="reminder-panel surface-card rv-rise" style="--i: 6">
            <div class="reminder-head">
              <div>
                <h2 class="text-h2">老人健康提醒</h2>
                <p class="text-note">日常照护小提示，帮助家人更安心</p>
              </div>
              <span class="reminder-badge">日常照护</span>
            </div>
            <ul class="reminder-list">
              <li v-for="(tip, index) in healthTips" :key="tip.title">
                <span class="reminder-index">{{ String(index + 1).padStart(2, '0') }}</span>
                <div>
                  <strong>{{ tip.title }}</strong>
                  <em>{{ tip.desc }}</em>
                </div>
              </li>
            </ul>
            <button type="button" class="assurance-link" @click="$router.push('/family/elderly')">
              管理老人档案
              <el-icon :size="14"><ArrowRight /></el-icon>
            </button>
          </aside>
        </div>
      </div>

      <p class="footer-note">安心陪 · 让每一次就医都有人陪着</p>
    </main>

    <!-- 移动端底部导航 -->
    <nav class="bottom-nav" aria-label="主导航">
      <button type="button" class="nav-item active" @click="$router.push('/family/home')">
        <el-icon :size="20"><HomeFilled /></el-icon>
        <span>工作台</span>
      </button>
      <button type="button" class="nav-item" @click="$router.push('/family/elderly')">
        <el-icon :size="20"><User /></el-icon>
        <span>老人档案</span>
      </button>
      <button type="button" class="nav-item" @click="$router.push('/family/orders')">
        <el-icon :size="20"><Document /></el-icon>
        <span>我的订单</span>
      </button>
      <button type="button" class="nav-item" @click="$router.push('/family/ai-chat')">
        <el-icon :size="20"><ChatDotRound /></el-icon>
        <span>AI 助手</span>
      </button>
    </nav>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import {
  ArrowRight, Calendar, Document, User, Folder, ChatDotRound, HomeFilled
} from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'
import api from '@/api/request'
import FamilyTopNav from '@/components/FamilyTopNav.vue'
import { getOrderStatusLabel, getOrderStatusType, getOrderStatusClass } from '@/utils/orderStatus'
import { formatDateTime } from '@/utils/datetime'

const userStore = useUserStore()
const notificationCount = ref(0)
const recentOrders = ref([])

const displayName = computed(() => userStore.user?.nickname || '朋友')
const greeting = computed(() => {
  const h = new Date().getHours()
  if (h < 6) return '夜深了'
  if (h < 11) return '早上好'
  if (h < 14) return '中午好'
  if (h < 18) return '下午好'
  return '晚上好'
})
const todayLabel = computed(() => {
  const d = new Date()
  return `${d.getFullYear()}年${d.getMonth() + 1}月${d.getDate()}日`
})

const quickActions = [
  { label: '老人档案', path: '/family/elderly', icon: User, desc: '家人基础信息' },
  { label: '我的订单', path: '/family/orders', icon: Document, desc: '陪诊服务进度' },
  { label: '就医档案', path: '/family/records', icon: Folder, desc: '病历与报告' },
  { label: 'AI 健康助手', path: '/family/ai-chat', icon: ChatDotRound, desc: '随时咨询就医问题' }
]

const assurances = [
  { title: '实名资质审核', desc: '陪诊师身份与资质双重核验' },
  { title: '全程服务留痕', desc: '接单、服务、完成节点可追溯' },
  { title: '就医隐私保护', desc: '档案与联系方式按角色脱敏' }
]

const healthTips = [
  { title: '按时测量血压', desc: '建议早晚各一次，记录数值变化' },
  { title: '复诊资料提前整理', desc: '病历、检查报告与医保卡放在一起' },
  { title: '按医嘱规律用药', desc: '设置提醒，避免漏服或重复服药' }
]

const getStatusType = (status) => getOrderStatusType(status)
const getStatusLabel = (status) => getOrderStatusLabel(status)

onMounted(async () => {
  try {
    const [orderRes, notifyRes] = await Promise.all([
      api.get('/order/page', { params: { current: 1, size: 7 } }),
      api.get('/notification/page', { params: { current: 1, size: 1 } })
    ])
    if (orderRes.code === 200 && orderRes.data) {
      recentOrders.value = orderRes.data.records || []
    }
    if (notifyRes.code === 200 && notifyRes.data) {
      notificationCount.value = notifyRes.data.total || 0
    }
  } catch (e) {
    console.error('Failed to load home data:', e)
  }
})
</script>

<style scoped>
.family-home {
  min-height: 100vh;
  background: var(--bg);
  padding-bottom: 32px;
}

.page-wrap {
  width: min(1240px, 100%);
  margin: 0 auto;
  padding: 28px 24px 40px;
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

.intro-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-shrink: 0;
}

.intro-date {
  font-size: 12.5px;
  color: var(--muted-2);
  font-variant-numeric: tabular-nums;
}

.intro-badge {
  padding: 5px 10px;
  background: var(--teal-soft);
  border: 1px solid var(--teal-light);
  border-radius: 6px;
  font-size: 12px;
  font-weight: 600;
  color: var(--teal-deep);
}

/* ---------- 16:5 横幅 ---------- */
.hero-banner {
  position: relative;
  width: 100%;
  display: grid;
  grid-template-columns: 1.15fr 0.85fr;
  align-items: center;
  gap: 28px;
  aspect-ratio: 16 / 5;
  min-height: 220px;
  max-height: 280px;
  margin-bottom: 20px;
  padding: clamp(22px, 2.8vw, 30px);
  overflow: hidden;
  background: linear-gradient(135deg, #f7fafd 0%, #eef4fa 56%, #e3edf7 100%);
  border: 1px solid var(--line);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-card);
}

.hero-banner::after {
  content: "";
  position: absolute;
  top: -120px;
  right: -80px;
  width: 300px;
  height: 300px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(58, 110, 165, 0.1), transparent 68%);
  pointer-events: none;
}

.hero-copy {
  position: relative;
  z-index: 1;
}

.hero-eyebrow {
  display: inline-flex;
  align-items: center;
  padding: 5px 10px;
  border: 1px solid var(--teal-light);
  border-radius: 6px;
  background: rgba(255, 255, 255, 0.72);
  font-size: 11.5px;
  font-weight: 600;
  letter-spacing: 0.06em;
  color: var(--teal-deep);
}

.hero-title {
  margin: 12px 0 8px;
  font-size: clamp(24px, 2.4vw, 28px);
  font-weight: 700;
  line-height: 1.3;
  letter-spacing: -0.03em;
  color: var(--ink);
}

.hero-desc {
  max-width: 46ch;
  margin: 0;
  font-size: 13.5px;
  line-height: 1.7;
  color: var(--ink-3);
}

.hero-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 16px;
}

.hero-actions .el-button {
  height: 40px;
  padding: 0 16px;
  font-size: 13.5px;
}

.hero-art {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 18px;
}

.ecg-line {
  width: 100%;
  max-width: 330px;
  height: 70px;
  color: var(--teal);
  opacity: 0.24;
}

.hero-badges {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px;
}

.hero-badges span {
  padding: 5px 10px;
  background: rgba(255, 255, 255, 0.8);
  border: 1px solid var(--line);
  border-radius: 6px;
  font-size: 11.5px;
  color: var(--ink-3);
}

/* ---------- 快捷入口 ---------- */
.quick-grid {
  margin-bottom: 20px;
}

.quick-card {
  display: flex;
  align-items: center;
  gap: 14px;
  min-height: 96px;
  padding: 18px;
  text-align: left;
  cursor: pointer;
  transition: transform 0.18s ease, border-color 0.18s ease,
    box-shadow 0.18s ease;
}

.quick-card:hover {
  transform: translateY(-2px);
  border-color: var(--teal-light);
  box-shadow: 0 10px 28px -16px rgba(16, 24, 40, 0.22);
}

.quick-icon {
  width: 42px;
  height: 42px;
  flex-shrink: 0;
  display: grid;
  place-items: center;
  background: var(--teal-soft);
  border: 1px solid var(--teal-light);
  border-radius: 8px;
  color: var(--teal-deep);
}

.quick-copy {
  display: flex;
  flex-direction: column;
  gap: 3px;
  min-width: 0;
}

.quick-copy strong {
  font-size: 15px;
  font-weight: 600;
  color: var(--ink);
}

.quick-copy em {
  font-style: normal;
  font-size: 12.5px;
  color: var(--muted);
}

.quick-arrow {
  margin-left: auto;
  color: var(--muted-2);
  flex-shrink: 0;
}

/* ---------- 订单 + 保障 ---------- */
.content-grid {
  align-items: stretch;
}

.orders-panel {
  display: flex;
  flex-direction: column;
  padding: 20px 24px 8px;
}

.side-column {
  display: flex;
  flex-direction: column;
  gap: 16px;
  height: 100%;
}

.panel-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  padding-bottom: 14px;
  border-bottom: 1px solid var(--line);
}

.panel-head .text-note {
  margin-top: 3px;
}

.panel-link {
  font-size: 13px;
  font-weight: 600;
  color: var(--teal-deep);
}

.order-list {
  display: grid;
}

.order-row {
  display: grid;
  grid-template-columns: minmax(160px, 1.6fr) 1fr 1.15fr 88px;
  align-items: center;
  gap: 16px;
  min-height: 56px;
  padding: 0 4px;
  background: none;
  border: none;
  border-bottom: 1px solid var(--line);
  font-family: inherit;
  text-align: left;
  cursor: pointer;
  transition: background-color 0.16s ease;
}

.order-row:last-child {
  border-bottom: none;
}

.order-row:hover {
  background: var(--surface-2);
}

.order-main {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.order-main strong {
  font-size: 14px;
  font-weight: 600;
  color: var(--ink);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.order-main em {
  font-style: normal;
  font-size: 11.5px;
  color: var(--muted-2);
  font-variant-numeric: tabular-nums;
}

.order-dept {
  font-size: 13px;
  color: var(--ink-3);
}

.order-time {
  font-size: 12px;
  color: var(--muted-2);
  text-align: right;
  font-variant-numeric: tabular-nums;
}

.order-row .status-tag {
  justify-self: end;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 48px 16px 54px;
  text-align: center;
}

.empty-icon {
  width: 48px;
  height: 48px;
  display: grid;
  place-items: center;
  background: var(--surface-3);
  border: 1px solid var(--line);
  border-radius: 10px;
  color: var(--muted);
}

.empty-state p {
  margin: 0;
  font-size: 13.5px;
  color: var(--muted);
}

.assurance-panel {
  display: flex;
  flex-direction: column;
  flex: 0 0 auto;
  gap: 14px;
  padding: 20px 22px;
}

.assurance-list {
  display: grid;
  gap: 13px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.assurance-list li {
  display: flex;
  align-items: flex-start;
  gap: 11px;
}

.assurance-icon {
  width: 28px;
  height: 28px;
  flex-shrink: 0;
  display: grid;
  place-items: center;
  background: var(--teal-soft);
  border-radius: 8px;
  color: var(--teal-deep);
}

.assurance-list strong {
  display: block;
  font-size: 13.5px;
  font-weight: 600;
  color: var(--ink);
}

.assurance-list em {
  display: block;
  margin-top: 2px;
  font-style: normal;
  font-size: 12px;
  line-height: 1.6;
  color: var(--muted);
}

.assurance-link {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  align-self: flex-start;
  margin-top: auto;
  padding: 0;
  background: none;
  border: none;
  font-family: inherit;
  font-size: 13px;
  font-weight: 600;
  color: var(--teal-deep);
  cursor: pointer;
}

.assurance-link:hover {
  text-decoration: underline;
  text-underline-offset: 3px;
}

/* 老人健康提醒 */
.reminder-panel {
  display: flex;
  flex-direction: column;
  flex: 1;
  gap: 14px;
  min-height: 0;
  padding: 20px 22px;
}

.reminder-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.reminder-badge {
  flex-shrink: 0;
  padding: 3px 8px;
  background: var(--teal-soft);
  border: 1px solid var(--teal-light);
  border-radius: 6px;
  font-size: 11px;
  font-weight: 500;
  color: var(--teal-deep);
}

.reminder-list {
  display: grid;
  gap: 12px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.reminder-list li {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding-bottom: 12px;
  border-bottom: 1px dashed var(--line);
}

.reminder-list li:last-child {
  padding-bottom: 0;
  border-bottom: none;
}

.reminder-index {
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 26px;
  height: 20px;
  padding: 0 5px;
  background: var(--teal-soft);
  border-radius: 4px;
  font-size: 10.5px;
  font-weight: 600;
  color: var(--teal-deep);
  font-variant-numeric: tabular-nums;
}

.reminder-list strong {
  display: block;
  font-size: 13px;
  font-weight: 600;
  color: var(--ink);
}

.reminder-list em {
  display: block;
  margin-top: 2px;
  font-style: normal;
  font-size: 12px;
  line-height: 1.55;
  color: var(--muted);
}

.reminder-panel .assurance-link {
  margin-top: auto;
}

.footer-note {
  margin: 24px 0 0;
  text-align: center;
  font-size: 12px;
  color: var(--muted-2);
}

/* ---------- 移动端底部导航 ---------- */
.bottom-nav {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 45;
  display: none;
  justify-content: space-around;
  padding: 7px 8px calc(7px + env(safe-area-inset-bottom));
  background: rgba(255, 255, 255, 0.97);
  backdrop-filter: blur(10px);
  border-top: 1px solid var(--line);
}

.nav-item {
  appearance: none;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 3px;
  min-width: 64px;
  padding: 5px 8px;
  background: transparent;
  border: none;
  border-radius: 8px;
  font-family: inherit;
  font-size: 11px;
  color: var(--muted);
  cursor: pointer;
}

.nav-item.active {
  color: var(--teal-deep);
  font-weight: 600;
}

@media (max-width: 1000px) {
  .page-wrap {
    padding: 22px 16px 90px;
  }

  .page-intro {
    align-items: flex-start;
  }

  .intro-date {
    display: none;
  }

  .hero-banner {
    grid-template-columns: 1fr;
    aspect-ratio: auto;
    min-height: 0;
    max-height: none;
    padding: 24px;
  }

  .side-column {
    height: auto;
  }

  .hero-art {
    display: none;
  }

  .hero-title {
    font-size: 24px;
  }

  .bottom-nav {
    display: flex;
  }
}

@media (max-width: 700px) {
  .page-intro {
    flex-direction: column;
    gap: 10px;
  }

  .intro-meta {
    align-self: flex-start;
  }

  .hero-actions {
    flex-wrap: wrap;
  }

  .order-row {
    grid-template-columns: 1fr auto;
    gap: 6px 12px;
    padding: 12px 2px;
  }

  .order-dept {
    display: none;
  }

  .order-time {
    grid-column: 1 / 2;
    text-align: left;
  }

  .order-row .status-tag {
    grid-row: 1 / 3;
    grid-column: 2 / 3;
    align-self: center;
  }

  .orders-panel {
    padding: 18px 16px 6px;
  }

  .assurance-panel {
    padding: 18px;
  }

  .reminder-panel {
    padding: 18px;
  }
}
</style>
