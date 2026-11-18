<template>
  <div class="escort-orders">
    <header class="page-header">
      <el-button text @click="$router.push('/escort/home')">
        <el-icon :size="22"><ArrowLeft /></el-icon>
      </el-button>
      <h1 class="page-title">我的订单</h1>
      <div style="width: 32px;"></div>
    </header>

    <main class="page-body">
      <!-- Tabs + sort -->
      <div class="filter-bar surface-card">
        <el-tabs v-model="statusFilter" @tab-change="handleStatusChange">
          <el-tab-pane label="全部" name="" />
          <el-tab-pane label="待派单" name="PENDING_DISPATCH" />
          <el-tab-pane label="已接单" name="ACCEPTED" />
          <el-tab-pane label="服务中" name="IN_SERVICE" />
          <el-tab-pane label="已完成" name="SERVICE_FINISHED" />
          <el-tab-pane label="已取消" name="CANCELLED" />
        </el-tabs>
        <div class="filter-actions">
          <el-select v-model="sortOrder" style="width: 156px;">
            <el-option label="预约时间最早" value="asc" />
            <el-option label="预约时间最晚" value="desc" />
          </el-select>
          <el-button :icon="Refresh" :loading="loading" @click="fetchOrders">刷新</el-button>
        </div>
      </div>

      <!-- Desktop table -->
      <div class="table-card surface-card orders-table-card">
        <div v-if="loading && orders.length === 0" class="table-skeleton">
          <el-skeleton :rows="6" animated />
        </div>

        <el-table
          v-else
          :data="sortedOrders"
          v-loading="loading"
          stripe
          height="100%"
          @row-click="openDetail"
        >
          <el-table-column prop="orderNo" label="订单编号" width="160" />
          <el-table-column prop="elderlyName" label="就诊人" width="110" />
          <el-table-column prop="appointmentTime" label="预约时间" width="170" class-name="time-col" />
          <el-table-column prop="department" label="就诊科室" width="120" />
          <el-table-column prop="hospitalName" label="就诊医院" min-width="150" show-overflow-tooltip />
          <el-table-column prop="amount" label="金额" width="90">
            <template #default="{ row }">{{ row.amount ? '¥' + row.amount : '—' }}</template>
          </el-table-column>
          <el-table-column label="状态" width="110">
            <template #default="{ row }">
              <el-tag :class="getOrderStatusClass(row.status)">
                {{ statusLabel(row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="200" fixed="right">
            <template #default="{ row }">
              <el-button type="primary" link size="small" @click.stop="openDetail(row)">
                详情
              </el-button>
              <el-button
                v-if="row.status === 'ACCEPTED'"
                type="primary"
                size="small"
                @click.stop="handleStart(row)"
              >
                开始服务
              </el-button>
              <el-button
                v-if="row.status === 'IN_SERVICE'"
                type="primary"
                size="small"
                @click.stop="handleFinish(row)"
              >
                结束服务
              </el-button>
            </template>
          </el-table-column>
          <template #empty>
            <div class="empty-block">
              <span class="empty-icon"><el-icon :size="24"><Document /></el-icon></span>
              <h4>暂无订单</h4>
              <p>调整筛选条件后重试，或先去接单大厅看看新的陪诊需求。</p>
            </div>
          </template>
        </el-table>

        <div class="table-footer" v-if="total > 0">
          <span class="text-note">共 {{ total }} 条</span>
          <el-pagination
            v-model:current-page="page"
            v-model:page-size="size"
            :total="total"
            :page-sizes="[10, 20, 50]"
            layout="sizes, prev, pager, next"
            @current-change="fetchOrders"
            @size-change="fetchOrders"
          />
        </div>
      </div>

      <!-- Mobile cards -->
      <div class="mobile-order-list">
        <div v-if="loading && orders.length === 0" class="table-skeleton surface-card">
          <el-skeleton :rows="5" animated />
        </div>
        <template v-else-if="sortedOrders.length > 0">
          <article
            v-for="(row, index) in sortedOrders"
            :key="row.id"
            class="mobile-order-card surface-card rv-rise"
            :style="{ '--i': index }"
            @click="openDetail(row)"
          >
            <div class="mobile-order-head">
              <span class="order-no">{{ row.orderNo }}</span>
              <el-tag :class="getOrderStatusClass(row.status)">
                {{ statusLabel(row.status) }}
              </el-tag>
            </div>
            <div class="mobile-order-info">
              <div><span>就诊人</span><strong>{{ row.elderlyName || '—' }}</strong></div>
              <div><span>预约时间</span><strong class="time-text">{{ formatDateTime(row.appointmentTime) }}</strong></div>
              <div><span>就诊科室</span><strong>{{ row.department || '—' }}</strong></div>
              <div><span>就诊医院</span><strong>{{ row.hospitalName || row.hospital || "—" }}</strong></div>
              <div><span>订单金额</span><strong class="amount">¥{{ row.amount || '—' }}</strong></div>
            </div>
            <div class="mobile-order-actions">
              <el-button plain @click.stop="openDetail(row)">详情</el-button>
              <el-button
                v-if="row.status === 'ACCEPTED'"
                type="primary"
                @click.stop="handleStart(row)"
              >
                开始服务
              </el-button>
              <el-button
                v-if="row.status === 'IN_SERVICE'"
                type="primary"
                @click.stop="handleFinish(row)"
              >
                结束服务
              </el-button>
            </div>
          </article>
        </template>

        <div v-else class="empty-state surface-card">
          <span class="empty-icon"><el-icon :size="28"><Document /></el-icon></span>
          <h4>暂无订单</h4>
          <p class="text-note">调整筛选条件后重试，或先去接单大厅看看新的陪诊需求。</p>
        </div>

        <div class="table-footer mobile-footer" v-if="total > 0">
          <span class="text-note">共 {{ total }} 条</span>
          <el-pagination
            v-model:current-page="page"
            v-model:page-size="size"
            :total="total"
            :page-sizes="[10, 20, 50]"
            layout="sizes, prev, pager, next"
            @current-change="fetchOrders"
            @size-change="fetchOrders"
          />
        </div>
      </div>
    </main>

    <!-- Detail dialog -->
    <el-dialog v-model="detailVisible" title="订单详情" width="620px" destroy-on-close>
      <template v-if="detailTarget">
        <div class="dialog-order-head">
          <span class="dialog-order-no">{{ detailTarget.orderNo }}</span>
          <el-tag :class="getOrderStatusClass(detailTarget.status)">
            {{ statusLabel(detailTarget.status) }}
          </el-tag>
        </div>

        <el-descriptions :column="2" border size="small" class="dialog-desc">
          <el-descriptions-item label="就诊人">{{ detailTarget.elderlyName || '—' }}</el-descriptions-item>
          <el-descriptions-item label="预约时间">{{ formatDateTime(detailTarget.appointmentTime) }}</el-descriptions-item>
          <el-descriptions-item label="就诊医院">{{ detailTarget.hospitalName || detailTarget.hospital || "—" }}</el-descriptions-item>
          <el-descriptions-item label="就诊科室">{{ detailTarget.department || '—' }}</el-descriptions-item>
          <el-descriptions-item label="服务时长">{{ detailTarget.duration || '—' }} 小时</el-descriptions-item>
          <el-descriptions-item label="订单金额">¥{{ detailTarget.amount || '—' }}</el-descriptions-item>
        </el-descriptions>

        <div class="progress-block">
          <h4 class="text-h2">服务流程</h4>
          <el-steps :active="progressStep" finish-status="success" align-center>
            <el-step title="待派单" />
            <el-step title="已接单" />
            <el-step title="服务中" />
            <el-step title="待结算" />
            <el-step title="已完成" />
          </el-steps>
        </div>
      </template>

      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
        <el-button type="primary" @click="goFullDetail">查看完整详情</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, Document, Refresh } from '@element-plus/icons-vue'
import api from '@/api/request'
import { getOrderStatusClass, getOrderStatusLabel } from '@/utils/orderStatus'

const router = useRouter()
const loading = ref(false)
const orders = ref([])
const page = ref(1)
const size = ref(10)
const total = ref(0)
const statusFilter = ref('')
const sortOrder = ref('asc')
const detailVisible = ref(false)
const detailTarget = ref(null)
let poll = null

const sortedOrders = computed(() => {
  return [...orders.value].sort((a, b) => {
    const ta = new Date(a.appointmentTime).getTime() || 0
    const tb = new Date(b.appointmentTime).getTime() || 0
    return sortOrder.value === 'desc' ? tb - ta : ta - tb
  })
})

const progressStep = computed(() => {
  const map = {
    PENDING_DISPATCH: 0,
    DISPATCHED: 0,
    ACCEPTED: 1,
    IN_SERVICE: 2,
    SERVICE_FINISHED: 3,
    SETTLED: 3,
    COMPLETED: 4
  }
  return map[detailTarget.value?.status] ?? 0
})

function statusLabel(status) {
  return getOrderStatusLabel(status)
}

function formatDateTime(time) {
  if (!time) return '—'
  return String(time).replace('T', ' ').slice(0, 16)
}

function openDetail(row) {
  detailTarget.value = row
  detailVisible.value = true
}

function goFullDetail() {
  if (detailTarget.value) {
    router.push(`/escort/order/${detailTarget.value.id}`)
  }
}

function handleStatusChange() {
  page.value = 1
  fetchOrders()
}

async function fetchOrders() {
  loading.value = true
  try {
    const params = { current: page.value, size: size.value }
    if (statusFilter.value) params.status = statusFilter.value
    const res = await api.get('/order/page', { params })
    orders.value = res.data?.records || []
    total.value = res.data?.total || 0
  } catch (e) {
    ElMessage.error('获取订单列表失败')
    orders.value = []
  } finally {
    loading.value = false
  }
}

async function handleStart(row) {
  try {
    await ElMessageBox.confirm(`确认开始服务订单 "${row.orderNo}" 吗？`, '开始服务', {
      confirmButtonText: '确认开始',
      cancelButtonText: '取消',
      type: 'info'
    })
  } catch {
    return
  }
  try {
    await api.post(`/order/${row.id}/start`)
    ElMessage.success('已开始服务')
    await fetchOrders()
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || '操作失败')
  }
}

async function handleFinish(row) {
  try {
    await ElMessageBox.confirm(`确认结束服务订单 "${row.orderNo}" 吗？`, '结束服务', {
      confirmButtonText: '确认结束',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return
  }
  try {
    await api.post(`/order/${row.id}/finish`)
    ElMessage.success('服务已结束')
    await fetchOrders()
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || '操作失败')
  }
}

onMounted(() => {
  fetchOrders()
  poll = setInterval(fetchOrders, 5000)
})

onUnmounted(() => clearInterval(poll))
</script>

<style scoped>
.escort-orders {
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
  max-width: 1180px;
  margin: 0 auto;
  padding: 20px;
}

.filter-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  padding: 4px 16px 0;
  margin-bottom: 16px;
}

.filter-bar :deep(.el-tabs__header) {
  margin-bottom: 0;
}

.filter-bar :deep(.el-tabs__item) {
  height: 48px;
  font-size: 13.5px;
  font-weight: 500;
  color: var(--muted);
}

.filter-bar :deep(.el-tabs__item.is-active) {
  color: var(--teal-deep);
  font-weight: 600;
}

.filter-bar :deep(.el-tabs__active-bar) {
  background-color: var(--teal-deep);
  height: 2px;
}

.filter-bar :deep(.el-tabs__nav-wrap::after) {
  height: 1px;
  background-color: var(--line);
}

.filter-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
  padding-bottom: 4px;
}

.orders-table-card {
  height: clamp(520px, 68vh, 680px);
  padding: 8px 16px 12px;
}

.table-skeleton {
  padding: 16px;
}

.orders-table-card :deep(.el-table) {
  min-height: 0;
}

.table-col-time {
  color: var(--muted-2);
}

:deep(.time-col) {
  font-size: 12.5px;
  color: var(--muted-2);
  font-variant-numeric: tabular-nums;
}

.mobile-order-list {
  display: none;
}

.mobile-order-card {
  padding: 16px;
  margin-bottom: 12px;
}

.mobile-order-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--line);
}

.order-no {
  font-size: 13.5px;
  font-weight: 600;
  color: var(--ink);
  font-variant-numeric: tabular-nums;
}

.mobile-order-info {
  display: grid;
  gap: 8px;
  padding: 12px 0;
}

.mobile-order-info > div {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  font-size: 13px;
}

.mobile-order-info span {
  color: var(--muted);
}

.mobile-order-info strong {
  font-weight: 500;
  color: var(--ink-2);
  text-align: right;
}

.time-text {
  font-size: 12.5px;
  color: var(--muted-2);
  font-variant-numeric: tabular-nums;
}

.amount {
  color: var(--teal-deep) !important;
  font-weight: 600 !important;
}

.mobile-order-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding-top: 12px;
  border-top: 1px solid var(--line);
}

.mobile-order-actions .el-button {
  height: 40px;
  min-width: 88px;
}

.mobile-footer {
  margin-top: 4px;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  padding: 60px 20px;
  text-align: center;
}

.empty-icon {
  width: 52px;
  height: 52px;
  display: grid;
  place-items: center;
  background: var(--teal-soft);
  border: 1px solid var(--teal-light);
  border-radius: 12px;
  color: var(--teal-deep);
}

.dialog-order-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
}

.dialog-order-no {
  font-size: 14px;
  font-weight: 600;
  color: var(--ink);
  font-variant-numeric: tabular-nums;
}

.dialog-desc {
  margin-bottom: 18px;
}

.progress-block h4 {
  margin-bottom: 16px;
}

.progress-block :deep(.el-step__title) {
  font-size: 12px;
}

@media (max-width: 760px) {
  .page-body {
    padding: 14px;
  }

  .filter-bar {
    flex-direction: column;
    align-items: stretch;
    padding: 4px 12px 12px;
  }

  .filter-actions {
    justify-content: space-between;
    padding-bottom: 0;
  }

  .orders-table-card {
    display: none;
  }

  .mobile-order-list {
    display: block;
  }

  .mobile-order-actions .el-button {
    height: 44px;
    flex: 1;
  }

  .mobile-footer {
    flex-direction: column;
    align-items: flex-start;
  }
}

@media (max-width: 560px) {
  .dialog-order-head {
    align-items: flex-start;
    flex-direction: column;
    gap: 6px;
  }

  .dialog-desc :deep(.el-descriptions__body) {
    font-size: 12px;
  }
}
</style>
