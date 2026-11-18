<template>
  <div class="order-pool">
    <header class="page-header">
      <el-button text @click="$router.push('/escort/home')">
        <el-icon :size="22"><ArrowLeft /></el-icon>
      </el-button>
      <h1 class="page-title">接单大厅</h1>
      <div style="width: 32px;"></div>
    </header>

    <main class="page-body">
      <div class="pool-toolbar surface-card">
        <div class="filters">
          <el-select v-model="deptFilter" placeholder="全部科室" clearable style="width: 150px;">
            <el-option v-for="dept in departments" :key="dept" :label="dept" :value="dept" />
          </el-select>
          <el-select v-model="dateFilter" style="width: 140px;">
            <el-option label="全部时间" value="all" />
            <el-option label="今天" value="today" />
            <el-option label="明天" value="tomorrow" />
            <el-option label="近 3 天" value="3days" />
          </el-select>
          <el-select v-model="sortBy" style="width: 150px;">
            <el-option label="预约时间最早" value="time-asc" />
            <el-option label="预约时间最晚" value="time-desc" />
            <el-option label="服务费从高到低" value="fee-desc" />
          </el-select>
          <el-button text @click="clearFilters">清空筛选</el-button>
        </div>
        <div class="toolbar-right">
          <span class="pool-count">共 {{ filteredOrders.length }} 单待接</span>
          <el-button :icon="Refresh" :loading="loading" :disabled="refreshing" @click="handleRefresh">
            刷新
          </el-button>
        </div>
      </div>

      <!-- Loading -->
      <div v-if="loading && orders.length === 0" class="loading-card surface-card">
        <el-skeleton :rows="4" animated />
      </div>

      <!-- Empty -->
      <div v-else-if="filteredOrders.length === 0" class="empty-state surface-card">
        <span class="empty-icon" aria-hidden="true">
          <el-icon :size="28"><List /></el-icon>
        </span>
        <h2 class="text-h2">{{ orders.length === 0 ? '暂无待接订单' : '没有符合条件的订单' }}</h2>
        <p class="text-note">
          {{ orders.length === 0 ? '稍后刷新看看，新的陪诊订单会出现在这里。' : '试试清空科室或时间筛选。' }}
        </p>
        <el-button v-if="orders.length > 0" plain @click="clearFilters">清空筛选</el-button>
        <el-button v-else type="primary" plain :loading="loading" @click="handleRefresh">刷新订单</el-button>
      </div>

      <!-- Orders -->
      <div v-else class="pool-list">
        <article
          v-for="(order, index) in filteredOrders"
          :key="order.id"
          class="pool-card surface-card rv-rise"
          :style="{ '--i': index }"
        >
          <div class="pool-card-head">
            <div class="order-head-left">
              <span class="order-no">{{ order.orderNo }}</span>
              <el-tag :class="getOrderStatusClass(order.status)">
                {{ getOrderStatusLabel(order.status) }}
              </el-tag>
            </div>
            <div class="fee-block">
              <span class="fee-label">预估服务费</span>
              <strong class="fee-value">¥{{ order.amount ?? '—' }}</strong>
            </div>
          </div>

          <div class="pool-grid">
            <div class="info-block">
              <div class="info-row">
                <span>就诊人</span>
                <strong>{{ order.elderlyName || '接单后可见' }}</strong>
              </div>
              <div class="info-row">
                <span>就诊医院</span>
                <strong>{{ order.hospitalName || '—' }}</strong>
              </div>
              <div class="info-row">
                <span>就诊科室</span>
                <span class="dept-tag">{{ order.department || '未填写' }}</span>
              </div>
              <div class="info-row">
                <span>预约时间</span>
                <strong class="time-text">{{ formatDateTime(order.appointmentTime) }}</strong>
              </div>
              <div class="info-row">
                <span>服务时长</span>
                <strong>{{ order.duration || '—' }} 小时 · {{ serviceTypeLabel(order.serviceType) }}</strong>
              </div>
            </div>

            <div class="note-block">
              <span class="note-label">特殊备注</span>
              <p>{{ order.remark || '暂无特殊备注' }}</p>
              <span class="note-hint">老人行动能力与完整档案，接单后可在订单详情查看。</span>
            </div>
          </div>

          <div class="pool-actions">
            <el-button
              type="danger"
              plain
              :loading="rejectingId === order.id"
              @click="handleReject(order)"
            >
              拒绝
            </el-button>
            <el-button
              type="primary"
              :loading="acceptingId === order.id"
              @click="handleAccept(order)"
            >
              接单
            </el-button>
          </div>
        </article>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, List, Refresh } from '@element-plus/icons-vue'
import api from '@/api/request'
import { getOrderStatusClass, getOrderStatusLabel } from '@/utils/orderStatus'

const loading = ref(false)
const refreshing = ref(false)
const orders = ref([])
const acceptingId = ref(null)
const rejectingId = ref(null)
const deptFilter = ref('')
const dateFilter = ref('all')
const sortBy = ref('time-asc')
let pollTimer = null

const departments = computed(() =>
  [...new Set(orders.value.map(o => o.department).filter(Boolean))]
)

const dayKey = (date) => {
  const d = new Date(date)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

const filteredOrders = computed(() => {
  let list = [...orders.value]
  if (deptFilter.value) {
    list = list.filter(o => o.department === deptFilter.value)
  }

  const now = new Date()
  const today = dayKey(now)
  const tomorrow = dayKey(new Date(now.getTime() + 86400000))
  const endOf3Days = new Date(now.getTime() + 3 * 86400000)

  if (dateFilter.value === 'today') {
    list = list.filter(o => String(o.appointmentTime || '').startsWith(today))
  } else if (dateFilter.value === 'tomorrow') {
    list = list.filter(o => String(o.appointmentTime || '').startsWith(tomorrow))
  } else if (dateFilter.value === '3days') {
    list = list.filter(o => {
      const d = new Date(o.appointmentTime)
      return !Number.isNaN(d.getTime()) && d >= now && d <= endOf3Days
    })
  }

  list.sort((a, b) => {
    if (sortBy.value === 'fee-desc') {
      return Number(b.amount || 0) - Number(a.amount || 0)
    }
    const ta = new Date(a.appointmentTime).getTime() || 0
    const tb = new Date(b.appointmentTime).getTime() || 0
    return sortBy.value === 'time-desc' ? tb - ta : ta - tb
  })
  return list
})

const formatDateTime = (time) => {
  if (!time) return '待确认'
  return String(time).replace('T', ' ').slice(0, 16)
}

const serviceTypeLabel = (type) => {
  const map = { BASIC: '基础陪诊', EXAM: '陪同检查' }
  return map[type] || '陪诊服务'
}

const clearFilters = () => {
  deptFilter.value = ''
  dateFilter.value = 'all'
  sortBy.value = 'time-asc'
}

const fetchPool = async () => {
  if (loading.value) return
  loading.value = true
  try {
    const res = await api.get('/escort/order/pool')
    const base = res.data || []
    // 接单大厅接口字段较少，逐单补齐医院、老人姓名与备注
    orders.value = await Promise.all(base.map(async order => {
      try {
        const detail = await api.get(`/order/${order.id}`)
        return { ...order, ...(detail.data || {}) }
      } catch {
        return order
      }
    }))
  } catch (e) {
    ElMessage.error('获取订单池失败')
    orders.value = []
  } finally {
    loading.value = false
  }
}

const handleRefresh = async () => {
  if (refreshing.value) return
  refreshing.value = true
  await fetchPool()
  setTimeout(() => { refreshing.value = false }, 800)
}

const handleAccept = async (order) => {
  try {
    await ElMessageBox.confirm(`确认接单 "${order.orderNo}" 吗？`, '确认接单', {
      confirmButtonText: '确认接单',
      cancelButtonText: '取消',
      type: 'info'
    })
  } catch {
    return
  }
  acceptingId.value = order.id
  try {
    await api.post(`/order/${order.id}/accept`)
    ElMessage.success('接单成功')
    await fetchPool()
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || '接单失败')
  } finally {
    acceptingId.value = null
  }
}

const handleReject = async (order) => {
  try {
    await ElMessageBox.confirm(`确认拒绝订单 "${order.orderNo}" 吗？`, '确认拒绝', {
      confirmButtonText: '确认拒绝',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return
  }
  rejectingId.value = order.id
  try {
    await api.post(`/order/${order.id}/reject`)
    ElMessage.success('已拒绝')
    await fetchPool()
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || '操作失败')
  } finally {
    rejectingId.value = null
  }
}

onMounted(() => {
  fetchPool()
  pollTimer = setInterval(fetchPool, 5000)
})

onUnmounted(() => {
  clearInterval(pollTimer)
})
</script>

<style scoped>
.order-pool {
  min-height: 100vh;
  background: var(--bg);
  padding-bottom: 32px;
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
  padding: 20px;
}

.pool-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
  padding: 14px 16px;
  margin-bottom: 16px;
}

.filters {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.toolbar-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.pool-count {
  font-size: 12.5px;
  color: var(--muted);
  font-variant-numeric: tabular-nums;
}

.loading-card {
  padding: 24px;
}

.pool-list {
  display: grid;
  gap: 16px;
}

.pool-card {
  padding: 20px;
}

.pool-card-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  padding-bottom: 14px;
  border-bottom: 1px solid var(--line);
}

.order-head-left {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.order-no {
  font-size: 14px;
  font-weight: 600;
  color: var(--ink);
  font-variant-numeric: tabular-nums;
}

.fee-block {
  display: flex;
  align-items: baseline;
  gap: 6px;
  flex-shrink: 0;
}

.fee-label {
  font-size: 11.5px;
  color: var(--muted-2);
}

.fee-value {
  font-size: 20px;
  font-weight: 700;
  color: var(--teal-deep);
  font-variant-numeric: tabular-nums;
}

.pool-grid {
  display: grid;
  grid-template-columns: 1.4fr 1fr;
  gap: 20px;
  padding: 16px 0;
}

.info-block {
  display: grid;
  gap: 9px;
}

.info-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  font-size: 13px;
}

.info-row > span:first-child {
  flex-shrink: 0;
  color: var(--muted);
}

.info-row strong {
  font-weight: 600;
  color: var(--ink-2);
  text-align: right;
}

.time-text {
  font-size: 12.5px;
  color: var(--muted-2) !important;
  font-weight: 500 !important;
  font-variant-numeric: tabular-nums;
}

.dept-tag {
  display: inline-flex;
  padding: 2px 8px;
  background: var(--teal-soft);
  border: 1px solid var(--teal-light);
  border-radius: 6px;
  font-size: 12px;
  font-weight: 500;
  color: var(--teal-deep);
}

.note-block {
  padding: 14px;
  background: var(--surface-2);
  border: 1px solid var(--line);
  border-radius: 8px;
}

.note-label {
  display: block;
  margin-bottom: 6px;
  font-size: 11.5px;
  color: var(--muted-2);
}

.note-block p {
  margin: 0;
  font-size: 13px;
  line-height: 1.65;
  color: var(--ink-3);
}

.note-hint {
  display: block;
  margin-top: 10px;
  font-size: 11.5px;
  line-height: 1.6;
  color: var(--muted-2);
}

.pool-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding-top: 14px;
  border-top: 1px solid var(--line);
}

.pool-actions .el-button {
  min-width: 96px;
  height: 40px;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  padding: 72px 24px;
  text-align: center;
}

.empty-icon {
  width: 54px;
  height: 54px;
  display: grid;
  place-items: center;
  margin-bottom: 4px;
  background: var(--teal-soft);
  border: 1px solid var(--teal-light);
  border-radius: 12px;
  color: var(--teal-deep);
}

.empty-state p {
  margin-bottom: 10px;
}

@media (max-width: 760px) {
  .page-body {
    padding: 14px;
  }

  .pool-toolbar {
    align-items: flex-start;
    flex-direction: column;
  }

  .filters,
  .toolbar-right {
    width: 100%;
  }

  .filters :deep(.el-select) {
    flex: 1;
    min-width: 120px;
  }

  .toolbar-right {
    justify-content: space-between;
  }

  .pool-grid {
    grid-template-columns: 1fr;
    gap: 14px;
  }

  .pool-card {
    padding: 16px;
  }

  .pool-actions .el-button {
    flex: 1;
    height: 44px;
  }
}
</style>
