<template>
  <div class="orders-page">
    <FamilyTopNav />

    <main class="page-wrap">
      <button type="button" class="mobile-back" @click="$router.push('/family/home')">
        <el-icon :size="15"><ArrowLeft /></el-icon>
        返回工作台
      </button>

      <header class="page-intro rv-fade">
        <div>
          <h1 class="page-heading">我的订单</h1>
          <p class="page-sub">查看陪诊服务进度、预约信息与历史订单。</p>
        </div>
        <el-button type="primary" @click="$router.push('/family/order/create')">
          <el-icon :size="16"><Plus /></el-icon>
          预约陪诊
        </el-button>
      </header>

      <section class="orders-panel surface-card rv-rise" style="--i: 0">
        <div class="filter-tabs">
          <el-tabs v-model="activeStatus" @tab-change="handleStatusChange">
            <el-tab-pane label="全部" name="all" />
            <el-tab-pane label="待派单" name="PENDING_DISPATCH" />
            <el-tab-pane label="已派单" name="DISPATCHED" />
            <el-tab-pane label="已接单" name="ACCEPTED" />
            <el-tab-pane label="服务中" name="IN_SERVICE" />
            <el-tab-pane label="服务完成" name="SERVICE_FINISHED" />
            <el-tab-pane label="待结算" name="SETTLED" />
            <el-tab-pane label="已完成" name="COMPLETED" />
            <el-tab-pane label="已取消" name="CANCELLED" />
          </el-tabs>
        </div>

        <div v-if="orders.length > 0" class="order-table">
          <div class="table-head" aria-hidden="true">
            <span>订单信息</span>
            <span>就诊科室</span>
            <span>预约时间</span>
            <span>金额</span>
            <span>状态</span>
          </div>

          <button
            v-for="(order, index) in orders"
            :key="order.id"
            type="button"
            class="order-row rv-rise"
            :style="{ '--i': index }"
            @click="$router.push(`/family/order/${order.id}`)"
          >
            <span class="order-main">
              <strong>{{ order.elderlyName }}</strong>
              <em>{{ order.orderNo }}</em>
            </span>
            <span class="order-dept">{{ order.department || '—' }}</span>
            <span class="order-time">{{ order.appointmentTime }}</span>
            <span class="order-amount">{{ order.amount ? `¥${order.amount}` : '—' }}</span>
            <el-tag :type="getStatusType(order.status)" :class="getOrderStatusClass(order.status)">
              {{ getStatusLabel(order.status) }}
            </el-tag>
          </button>
        </div>

        <div v-else class="empty-state">
          <el-icon :size="28" class="empty-icon"><Document /></el-icon>
          <h2 class="text-h2">暂无订单</h2>
          <p class="text-note">还没有陪诊订单，先为家人预约一次服务吧。</p>
          <el-button type="primary" plain @click="$router.push('/family/order/create')">
            去预约
          </el-button>
        </div>

        <div v-if="total > 0" class="pagination-wrap">
          <el-pagination
            v-model:current-page="pageNo"
            :page-size="pageSize"
            :total="total"
            background
            layout="total, prev, pager, next"
            :pager-count="5"
            @current-change="fetchOrders"
          />
        </div>
      </section>
    </main>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { ArrowLeft, Document, Plus } from '@element-plus/icons-vue'
import api from '@/api/request'
import FamilyTopNav from '@/components/FamilyTopNav.vue'
import { getOrderStatusLabel, getOrderStatusType, getOrderStatusClass } from '@/utils/orderStatus'

const activeStatus = ref('all')
const orders = ref([])
const pageNo = ref(1)
const pageSize = ref(10)
const total = ref(0)

const getStatusType = (status) => getOrderStatusType(status)
const getStatusLabel = (status) => getOrderStatusLabel(status)

const handleStatusChange = () => {
  pageNo.value = 1
  fetchOrders()
}

const fetchOrders = async () => {
  try {
    const params = { current: pageNo.value, size: pageSize.value }
    if (activeStatus.value !== 'all') {
      params.status = activeStatus.value
    }
    const res = await api.get('/order/page', { params })
    if (res.code === 200 && res.data) {
      orders.value = res.data.records || []
      total.value = res.data.total || 0
    }
  } catch (e) {
    console.error('Failed to load orders:', e)
  }
}

onMounted(fetchOrders)
let poll = setInterval(fetchOrders, 5000)
onUnmounted(() => clearInterval(poll))
</script>

<style scoped>
.orders-page {
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

.orders-panel {
  padding: 0 24px 16px;
}

.filter-tabs {
  padding-top: 6px;
}

.filter-tabs :deep(.el-tabs__header) {
  margin-bottom: 4px;
}

.filter-tabs :deep(.el-tabs__item) {
  height: 46px;
  font-size: 14px;
  font-weight: 500;
  color: var(--muted);
}

.filter-tabs :deep(.el-tabs__item.is-active) {
  color: var(--teal-deep);
  font-weight: 600;
}

.filter-tabs :deep(.el-tabs__active-bar) {
  background-color: var(--teal-deep);
  height: 2px;
}

.filter-tabs :deep(.el-tabs__nav-wrap::after) {
  height: 1px;
  background-color: var(--line);
}

.order-table {
  display: grid;
}

.table-head,
.order-row {
  display: grid;
  grid-template-columns: minmax(200px, 1.6fr) 1fr 1.3fr 0.8fr 92px;
  align-items: center;
  gap: 16px;
}

.table-head {
  min-height: 42px;
  padding: 0 12px;
  background: var(--surface-2);
  border-radius: 6px;
  font-size: 12px;
  font-weight: 500;
  color: var(--muted);
}

.table-head span:nth-child(4),
.table-head span:nth-child(5) {
  text-align: right;
}

.order-row {
  min-height: 60px;
  padding: 0 12px;
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

.order-amount {
  font-size: 14px;
  font-weight: 600;
  color: var(--ink-2);
  text-align: right;
  font-variant-numeric: tabular-nums;
}

.order-row .status-tag {
  justify-self: end;
}

.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  padding-top: 16px;
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
  width: 52px;
  height: 52px;
  display: grid;
  place-items: center;
  background: var(--teal-soft);
  border: 1px solid var(--teal-light);
  border-radius: 12px;
  color: var(--teal-deep);
  margin-bottom: 4px;
}

.empty-state p {
  margin-bottom: 10px;
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

  .orders-panel {
    padding: 0 16px 14px;
  }

  .table-head {
    display: none;
  }

  .order-row {
    grid-template-columns: 1fr auto;
    grid-template-areas:
      "main status"
      "time amount";
    gap: 6px 14px;
    padding: 14px 2px;
  }

  .order-main {
    grid-area: main;
  }

  .order-dept {
    display: none;
  }

  .order-time {
    grid-area: time;
    text-align: left;
  }

  .order-amount {
    grid-area: amount;
    text-align: right;
  }

  .order-row .status-tag {
    grid-area: status;
    align-self: center;
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

  .filter-tabs :deep(.el-tabs__item) {
    font-size: 13px;
  }
}
</style>
