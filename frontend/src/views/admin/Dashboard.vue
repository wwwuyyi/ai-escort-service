<template>
  <div class="admin-dashboard">
    <header class="page-head rv-fade">
      <div>
        <span class="eyebrow">管理中心</span>
        <h1>管理后台</h1>
        <p>平台运行概况与常用管理入口</p>
      </div>
      <div class="head-actions">
        <span class="date-chip">
          <el-icon :size="15"><Calendar /></el-icon>
          {{ todayLabel }}
        </span>
        <el-button class="logout-btn" @click="handleLogout">
          退出登录
        </el-button>
      </div>
    </header>

    <div v-if="loading" class="state-card">
      <el-skeleton :rows="6" animated />
    </div>

    <template v-else>
      <!-- 数据卡片 -->
      <section class="stat-grid" aria-label="平台概况">
        <article class="stat-card rv-rise" style="--i: 0">
          <div class="stat-top">
            <span class="stat-icon tone-teal"><el-icon :size="21"><Document /></el-icon></span>
            <span class="stat-kicker">今日订单</span>
          </div>
          <strong class="stat-number">{{ dashboard.todayOrders || 0 }}</strong>
          <span class="stat-caption">平台今日新增陪诊订单</span>
        </article>

        <article class="stat-card rv-rise" style="--i: 1">
          <div class="stat-top">
            <span class="stat-icon tone-amber"><el-icon :size="21"><Clock /></el-icon></span>
            <span class="stat-kicker">待审核</span>
          </div>
          <strong class="stat-number">{{ dashboard.pendingAudit || 0 }}</strong>
          <span class="stat-caption">等待处理的资质认证申请</span>
        </article>

        <article class="stat-card rv-rise" style="--i: 2">
          <div class="stat-top">
            <span class="stat-icon tone-slate"><el-icon :size="21"><UserFilled /></el-icon></span>
            <span class="stat-kicker">累计用户</span>
          </div>
          <strong class="stat-number">{{ dashboard.totalUsers || 0 }}</strong>
          <span class="stat-caption">已注册的家属与陪诊师</span>
        </article>
      </section>

      <div class="content-grid">
        <!-- 7 天趋势 -->
        <section class="chart-card rv-rise" style="--i: 3">
          <div class="card-head">
            <div>
              <h2>近 7 天订单趋势</h2>
              <p>按日统计的陪诊订单数量</p>
            </div>
            <span class="legend">
              <i class="legend-dot" aria-hidden="true"></i>
              订单量
            </span>
          </div>

          <div v-if="dashboard.trend7Days && dashboard.trend7Days.length > 0" class="bar-chart">
            <div
              v-for="(item, index) in dashboard.trend7Days"
              :key="index"
              class="bar-column"
            >
              <span class="bar-value">{{ item.count }}</span>
              <div class="bar-wrapper">
                <div
                  class="bar"
                  :style="{ height: barHeight(item.count) }"
                  :title="`${item.date}: ${item.count}单`"
                ></div>
              </div>
              <div class="bar-label">{{ shortDate(item.date) }}</div>
            </div>
          </div>

          <div v-else class="empty-block chart-empty">
            <span class="empty-icon" aria-hidden="true">
              <el-icon :size="24"><TrendCharts /></el-icon>
            </span>
            <h4>暂无订单趋势数据</h4>
            <p>有新的陪诊订单后，这里会展示近 7 天的订单变化。</p>
          </div>
        </section>

        <!-- 快捷操作 -->
        <section class="links-card rv-rise" style="--i: 4">
          <div class="card-head">
            <div>
              <h2>快捷操作</h2>
              <p>常用管理入口</p>
            </div>
          </div>
          <div class="quick-list">
            <button
              v-for="link in quickLinks"
              :key="link.label"
              type="button"
              class="quick-link"
              @click="$router.push(link.path)"
            >
              <span class="quick-icon" :class="link.tone">
                <el-icon :size="19"><component :is="link.icon" /></el-icon>
              </span>
              <span class="quick-copy">
                <strong>{{ link.label }}</strong>
                <em>{{ link.desc }}</em>
              </span>
              <el-icon class="quick-arrow" :size="14"><ArrowRight /></el-icon>
            </button>
          </div>
        </section>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import {
  Document, Clock, UserFilled, Checked, User, List, Star, View,
  TrendCharts, Calendar, ArrowRight
} from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'
import api from '@/api/request'

const router = useRouter()
const userStore = useUserStore()
const loading = ref(true)
const dashboard = ref({})

const todayLabel = computed(() => {
  const d = new Date()
  return `${d.getFullYear()}/${String(d.getMonth() + 1).padStart(2, '0')}/${String(d.getDate()).padStart(2, '0')}`
})

const quickLinks = [
  { label: '资质审核', desc: '审核陪诊师认证', path: '/admin/audit', icon: Checked, tone: 'tone-teal' },
  { label: '用户管理', desc: '查看账号与状态', path: '/admin/users', icon: User, tone: 'tone-teal' },
  { label: '订单管理', desc: '跟进平台订单', path: '/admin/orders', icon: List, tone: 'tone-amber' },
  { label: '评价管理', desc: '处理服务评价', path: '/admin/evaluations', icon: Star, tone: 'tone-amber' },
  { label: '脱敏预览', desc: '检查记录脱敏', path: '/admin/record-preview', icon: View, tone: 'tone-teal' }
]

function barHeight(count) {
  const maxCount = Math.max(...(dashboard.value.trend7Days || []).map(d => d.count), 1)
  return Math.max((count / maxCount) * 160, 6) + 'px'
}

function shortDate(date) {
  if (!date) return ''
  const parts = String(date).split('-')
  return parts.length >= 2 ? `${parts[1]}/${parts[2]}` : date
}

const handleLogout = () => {
  userStore.logout()
  router.push('/login')
}

onMounted(async () => {
  try {
    const res = await api.get('/admin/dashboard')
    dashboard.value = res.data || {}
  } catch (e) {
    dashboard.value = {}
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.admin-dashboard {
  width: min(1180px, 100%);
  margin: 0 auto;
  padding: 28px 24px 48px;
}

.page-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18px;
  margin-bottom: 22px;
}

.eyebrow {
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
  color: var(--teal-deep);
}

.page-head h1 {
  margin: 7px 0 4px;
  font-size: clamp(25px, 3vw, 32px);
  font-weight: 700;
  letter-spacing: -0.025em;
  color: var(--ink);
}

.page-head p {
  margin: 0;
  font-size: 13.5px;
  color: var(--muted);
}

.head-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
}

.date-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 34px;
  padding: 0 12px;
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: 9px;
  font-size: 12.5px;
  font-weight: 500;
  color: var(--ink-3);
}

.logout-btn {
  height: 34px;
  border-radius: 8px;
  font-size: 13px;
  color: var(--ink-3);
  border-color: var(--line-strong);
  background: var(--surface);
}

.logout-btn:hover {
  background: var(--surface-2);
  border-color: var(--teal-light);
  color: var(--teal-deep);
}

.state-card {
  padding: 26px;
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: 18px;
  box-shadow: var(--shadow-card);
}

/* ---------- 统计卡 ---------- */
.stat-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
  margin-bottom: 16px;
}

.stat-card {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 20px;
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: 16px;
  box-shadow: var(--shadow-sm);
  transition: border-color 0.18s ease, box-shadow 0.18s ease;
}

.stat-card:hover {
  border-color: var(--teal-light);
  box-shadow: var(--shadow-card);
}

.stat-top {
  display: flex;
  align-items: center;
  gap: 10px;
}

.stat-icon {
  width: 38px;
  height: 38px;
  display: grid;
  place-items: center;
  border-radius: 10px;
}

.tone-teal {
  background: var(--teal-soft);
  color: var(--teal-deep);
}

.tone-amber {
  background: var(--amber-soft);
  color: var(--amber);
}

.tone-slate {
  background: var(--surface-3);
  color: var(--ink-3);
}

.stat-kicker {
  font-size: 12.5px;
  font-weight: 500;
  color: var(--muted);
}

.stat-number {
  margin-top: 4px;
  font-size: 30px;
  font-weight: 700;
  line-height: 1.1;
  letter-spacing: -0.03em;
  color: var(--ink);
  font-variant-numeric: tabular-nums;
}

.stat-caption {
  font-size: 12px;
  color: var(--muted-2);
}

/* ---------- 内容网格 ---------- */
.content-grid {
  display: grid;
  grid-template-columns: 1.55fr 1fr;
  gap: 14px;
  align-items: stretch;
}

.chart-card,
.links-card {
  padding: 20px 22px;
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: 18px;
  box-shadow: var(--shadow-card);
}

.card-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  padding-bottom: 14px;
  border-bottom: 1px solid var(--line);
}

.card-head h2 {
  margin: 0;
  font-size: 16px;
  font-weight: 700;
  color: var(--ink);
}

.card-head p {
  margin: 3px 0 0;
  font-size: 12.5px;
  color: var(--muted);
}

.legend {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: var(--muted);
  flex-shrink: 0;
}

.legend-dot {
  width: 8px;
  height: 8px;
  border-radius: 2px;
  background: var(--teal);
}

/* ---------- 图表 ---------- */
.bar-chart {
  display: flex;
  align-items: flex-end;
  justify-content: space-around;
  gap: 6px;
  height: 238px;
  padding: 18px 2px 0;
  background-image: repeating-linear-gradient(
    to top,
    transparent 0,
    transparent 52px,
    rgba(226, 232, 240, 0.75) 52px,
    rgba(226, 232, 240, 0.75) 53px
  );
}

.bar-column {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: flex-end;
  flex: 1;
  min-width: 0;
  height: 100%;
}

.bar-value {
  margin-bottom: 5px;
  font-size: 11px;
  font-weight: 600;
  color: var(--ink-3);
  font-variant-numeric: tabular-nums;
}

.bar-wrapper {
  height: 160px;
  display: flex;
  align-items: flex-end;
}

.bar {
  width: min(42px, 68%);
  min-width: 16px;
  background: linear-gradient(180deg, var(--teal-hover) 0%, var(--teal) 100%);
  border-radius: 6px 6px 2px 2px;
  transition: filter 0.18s ease, transform 0.18s ease;
  transform-origin: bottom center;
}

.bar:hover {
  filter: brightness(1.08);
  transform: scaleY(1.02);
}

.bar-label {
  margin-top: 9px;
  font-size: 11.5px;
  color: var(--muted);
  font-variant-numeric: tabular-nums;
  white-space: nowrap;
}

.empty-wrap {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  padding: 52px 12px 60px;
}

.empty-icon {
  width: 50px;
  height: 50px;
  display: grid;
  place-items: center;
  background: var(--surface-2);
  border: 1px solid var(--line);
  border-radius: 13px;
  color: var(--muted);
}

.empty-wrap p {
  margin: 0;
  font-size: 13px;
  color: var(--muted);
}

/* ---------- 快捷操作 ---------- */
.quick-list {
  display: grid;
  gap: 2px;
  padding-top: 6px;
}

.quick-link {
  appearance: none;
  display: flex;
  align-items: center;
  gap: 11px;
  width: 100%;
  padding: 11px 6px;
  background: none;
  border: none;
  border-radius: 9px;
  font-family: inherit;
  text-align: left;
  cursor: pointer;
  transition: background-color 0.16s ease;
}

.quick-link:hover {
  background: var(--surface-2);
}

.quick-icon {
  width: 36px;
  height: 36px;
  flex-shrink: 0;
  display: grid;
  place-items: center;
  border-radius: 9px;
}

.quick-copy {
  display: flex;
  flex-direction: column;
  gap: 1px;
  min-width: 0;
}

.quick-copy strong {
  font-size: 13.5px;
  font-weight: 600;
  color: var(--ink);
}

.quick-copy em {
  font-style: normal;
  font-size: 11.5px;
  color: var(--muted);
}

.quick-arrow {
  margin-left: auto;
  color: var(--muted-2);
}

@media (max-width: 960px) {
  .content-grid {
    grid-template-columns: 1fr;
  }

  .quick-list {
    grid-template-columns: 1fr 1fr;
    gap: 4px 12px;
  }
}

@media (max-width: 700px) {
  .admin-dashboard {
    padding: 20px 16px 40px;
  }

  .page-head {
    flex-direction: column;
  }

  .stat-grid {
    grid-template-columns: 1fr;
  }

  .quick-list {
    grid-template-columns: 1fr;
  }

  .chart-card,
  .links-card {
    padding: 16px 14px;
  }

  .bar-chart {
    height: 210px;
  }

  .bar-wrapper {
    height: 135px;
  }

  .bar-label {
    font-size: 10.5px;
  }
}
</style>
