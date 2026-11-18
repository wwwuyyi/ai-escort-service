<template>
  <div class="escort-statistics">
    <header class="page-header">
      <el-button text @click="$router.push('/escort/home')">
        <el-icon :size="22"><ArrowLeft /></el-icon>
      </el-button>
      <h1 class="page-title">收入统计</h1>
      <div style="width: 32px;"></div>
    </header>

    <main class="page-body">
      <div v-if="loading" class="state-card">
        <el-skeleton :rows="5" animated />
      </div>

      <template v-else>
        <!-- 汇总卡片 -->
        <section class="stats-grid">
          <article class="stat-card surface-card rv-rise" style="--i: 0">
            <span class="stat-icon"><el-icon :size="22"><Finished /></el-icon></span>
            <div class="stat-copy">
              <span class="stat-label">累计完成订单</span>
              <strong class="stat-number">{{ totalCompleted }}</strong>
            </div>
          </article>

          <article class="stat-card surface-card rv-rise clickable" style="--i: 1" @click="ratingVisible = true">
            <span class="stat-icon"><el-icon :size="22"><StarFilled /></el-icon></span>
            <div class="stat-copy">
              <span class="stat-label">平均评分 · 点击查看说明</span>
              <strong class="stat-number">{{ ratingText }} <em>/ 5</em></strong>
            </div>
          </article>

          <article class="stat-card surface-card rv-rise" style="--i: 2">
            <span class="stat-icon"><el-icon :size="22"><Wallet /></el-icon></span>
            <div class="stat-copy">
              <span class="stat-label">筛选区间收入</span>
              <strong class="stat-number amount">¥{{ incomeTotal.toFixed(2) }}</strong>
            </div>
          </article>
        </section>

        <!-- 服务信息 -->
        <section class="service-card surface-card rv-rise" style="--i: 3" v-if="profile">
          <div class="service-head">
            <h2 class="text-h2">服务信息</h2>
          </div>
          <div class="service-info">
            <span class="info-label">服务城市</span>
            <el-tag class="status-tag status-tag--progress">{{ profile.serviceCity || '未设置' }}</el-tag>
            <span v-if="profile.serviceArea" class="info-label area-label">服务区域</span>
            <el-tag v-if="profile.serviceArea" class="status-tag status-tag--neutral">{{ profile.serviceArea }}</el-tag>
          </div>
        </section>

        <!-- 收入明细 -->
        <section class="income-card surface-card rv-rise" style="--i: 4">
          <div class="income-head">
            <div>
              <h2 class="text-h2">收入明细</h2>
              <p class="text-note">仅展示已完成 / 已结算订单，按服务时间筛选</p>
            </div>
            <div class="income-filters">
              <el-radio-group v-model="timeRange" size="small">
                <el-radio-button value="week">本周</el-radio-button>
                <el-radio-button value="month">本月</el-radio-button>
                <el-radio-button value="all">全部</el-radio-button>
                <el-radio-button value="custom">自定义</el-radio-button>
              </el-radio-group>
              <el-date-picker
                v-if="timeRange === 'custom'"
                v-model="customRange"
                type="daterange"
                size="small"
                range-separator="至"
                start-placeholder="开始日期"
                end-placeholder="结束日期"
                unlink-panels
                style="width: 240px;"
              />
            </div>
          </div>

          <div class="income-summary">
            <span>筛选区间收入（结算净额）</span>
            <strong>¥{{ incomeTotal.toFixed(2) }}</strong>
            <span class="income-count">共 {{ filteredSettlements.length }} 笔</span>
          </div>

          <el-table :data="filteredSettlements" stripe height="320">
            <el-table-column prop="orderNo" label="订单编号" width="160" />
            <el-table-column label="就诊人" width="110">
              <template #default="{ row }">{{ orderOf(row).elderlyName || '—' }}</template>
            </el-table-column>
            <el-table-column label="结算时间" width="170">
              <template #default="{ row }">
                <span class="time-text">{{ formatDateTime(row.createTime) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="订单金额" width="110">
              <template #default="{ row }">¥{{ Number(row.orderAmount || 0).toFixed(2) }}</template>
            </el-table-column>
            <el-table-column label="陪诊师收入" width="130">
              <template #default="{ row }">
                <strong class="net-income">¥{{ Number(row.escortIncome || 0).toFixed(2) }}</strong>
              </template>
            </el-table-column>
            <el-table-column label="平台服务费" width="130">
              <template #default="{ row }">¥{{ Number(row.platformFee || 0).toFixed(2) }}</template>
            </el-table-column>
            <template #empty>
              <div class="empty-block">
                <span class="empty-icon"><el-icon :size="24"><Wallet /></el-icon></span>
                <h4>暂无收入记录</h4>
                <p>完成陪诊服务并结算后，对应的净收入会展示在这里。</p>
              </div>
            </template>
          </el-table>
        </section>
      </template>
    </main>

    <!-- 评分说明 -->
    <el-dialog v-model="ratingVisible" title="评分说明" width="420px">
      <div class="rating-dialog">
        <el-rate :model-value="Number(ratingText)" disabled show-score text-color="var(--star)" />
        <p>当前平均评分来自已完成订单的家属评价。具体评价内容可在管理端评价管理中查看。</p>
      </div>
      <template #footer>
        <el-button type="primary" @click="ratingVisible = false">知道了</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ArrowLeft, Finished, StarFilled, Wallet } from '@element-plus/icons-vue'
import api from '@/api/request'

const loading = ref(true)
const stats = ref({})
const profile = ref(null)
const orders = ref([])
const timeRange = ref('month')
const customRange = ref([])
const ratingVisible = ref(false)

const ratingText = computed(() => Number(stats.value.avgRating || 0).toFixed(1))

const settlements = computed(() => stats.value.incomeList || [])
const orderMap = computed(() => Object.fromEntries(orders.value.map(o => [o.id, o])))
const orderOf = row => orderMap.value[row.orderId] || {}

const totalCompleted = computed(() =>
  Math.max(Number(stats.value.totalOrders || 0), Number(stats.value.settledCount || 0))
)

const startOfWeek = () => {
  const d = new Date()
  const day = d.getDay() || 7
  const monday = new Date(d)
  monday.setDate(d.getDate() - day + 1)
  monday.setHours(0, 0, 0, 0)
  return monday
}

const startOfMonth = () => {
  const d = new Date()
  return new Date(d.getFullYear(), d.getMonth(), 1)
}

const filteredSettlements = computed(() => {
  let list = settlements.value

  if (timeRange.value === 'week') {
    const start = startOfWeek()
    list = list.filter(s => new Date(s.createTime) >= start)
  } else if (timeRange.value === 'month') {
    const start = startOfMonth()
    list = list.filter(s => new Date(s.createTime) >= start)
  } else if (timeRange.value === 'custom' && customRange.value?.length === 2) {
    const [s, e] = customRange.value
    const end = new Date(e)
    end.setHours(23, 59, 59, 999)
    list = list.filter(s => {
      const t = new Date(s.createTime)
      return t >= new Date(s) && t <= end
    })
  }

  return [...list].sort((a, b) => new Date(b.createTime) - new Date(a.createTime))
})

// 结算净收入（订单金额扣除平台抽成后的 escort_income），不是订单毛收入
const incomeTotal = computed(() =>
  filteredSettlements.value.reduce((sum, s) => sum + Number(s.escortIncome || 0), 0)
)

function formatDateTime(time) {
  if (!time) return '—'
  return String(time).replace('T', ' ').slice(0, 16)
}

async function fetchAllOrders() {
  const size = 100
  let current = 1
  let all = []
  while (current <= 20) {
    const res = await api.get('/order/page', { params: { current, size } })
    const records = res.data?.records || []
    all = all.concat(records)
    const total = Number(res.data?.total || 0)
    if (records.length < size || all.length >= total) break
    current++
  }
  return all
}

onMounted(async () => {
  try {
    const [statsRes, profileRes, ordersResult] = await Promise.all([
      api.get('/escort/statistics'),
      api.get('/escort/profile'),
      fetchAllOrders().catch(() => [])
    ])
    stats.value = statsRes.data || {}
    profile.value = profileRes.data || null
    orders.value = ordersResult
  } catch (e) {
    stats.value = {}
    orders.value = []
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.escort-statistics {
  min-height: 100vh;
  background: var(--bg);
  padding-bottom: 40px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: var(--surface);
  border-bottom: 1px solid var(--line);
}

.page-body {
  max-width: 1000px;
  margin: 0 auto;
  padding: 24px 20px;
}

.state-card {
  padding: 24px;
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-card);
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
  margin-bottom: 16px;
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 20px;
}

.stat-card.clickable {
  cursor: pointer;
  transition: transform 0.18s ease, border-color 0.18s ease;
}

.stat-card.clickable:hover {
  transform: translateY(-2px);
  border-color: var(--teal-light);
}

.stat-icon {
  width: 44px;
  height: 44px;
  flex-shrink: 0;
  display: grid;
  place-items: center;
  background: var(--teal-soft);
  border-radius: 8px;
  color: var(--teal-deep);
}

.stat-card:nth-child(2) .stat-icon {
  background: var(--amber-soft);
  color: var(--amber);
}

.stat-card:nth-child(3) .stat-icon {
  background: var(--surface-3);
  color: var(--ink-3);
}

.stat-copy {
  display: flex;
  flex-direction: column;
  gap: 3px;
  min-width: 0;
}

.stat-label {
  font-size: 12px;
  color: var(--muted);
}

.stat-number {
  font-size: 22px;
  font-weight: 700;
  color: var(--ink);
  font-variant-numeric: tabular-nums;
}

.stat-number em {
  font-size: 12px;
  font-weight: 400;
  font-style: normal;
  color: var(--muted-2);
}

.stat-number.amount {
  color: var(--teal-deep);
}

.service-card {
  padding: 18px 22px;
  margin-bottom: 16px;
}

.service-head h2 {
  margin-bottom: 12px;
}

.service-info {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
}

.info-label {
  font-size: 12.5px;
  color: var(--muted);
}

.area-label {
  margin-left: 12px;
}

.income-card {
  padding: 20px 22px;
}

.income-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
  padding-bottom: 14px;
  border-bottom: 1px solid var(--line);
}

.income-head .text-note {
  margin-top: 3px;
}

.income-filters {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.income-summary {
  display: flex;
  align-items: baseline;
  gap: 10px;
  padding: 14px 0 6px;
  font-size: 12.5px;
  color: var(--muted);
}

.income-summary strong {
  font-size: 20px;
  font-weight: 700;
  color: var(--teal-deep);
  font-variant-numeric: tabular-nums;
}

.income-count {
  color: var(--muted-2);
}

.time-text {
  font-size: 12.5px;
  color: var(--muted-2);
  font-variant-numeric: tabular-nums;
}

.net-income {
  color: var(--teal-deep);
  font-variant-numeric: tabular-nums;
}

.rating-dialog {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 14px;
  padding: 8px 0 4px;
  text-align: center;
}

.rating-dialog p {
  margin: 0;
  font-size: 13px;
  line-height: 1.7;
  color: var(--muted);
}

@media (max-width: 800px) {
  .page-body {
    padding: 16px 14px;
  }

  .stats-grid {
    grid-template-columns: 1fr;
  }

  .income-head {
    flex-direction: column;
  }

  .income-filters {
    width: 100%;
  }
}
</style>
