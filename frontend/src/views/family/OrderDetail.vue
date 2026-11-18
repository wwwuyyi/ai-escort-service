<template>
  <div class="order-detail-page">
    <div class="page-header">
      <el-button icon="ArrowLeft" text @click="$router.back()">
        <el-icon :size="22"><ArrowLeft /></el-icon>
      </el-button>
      <h1 class="page-title">订单详情</h1>
      <div style="width: 32px;"></div>
    </div>

    <div class="detail-card" v-if="order">
      <div class="status-row">
        <span class="order-no">{{ order.orderNo }}</span>
        <el-tag :type="getStatusType(order.status)" :class="getOrderStatusClass(order.status)">
          {{ getStatusLabel(order.status) }}
        </el-tag>
      </div>

      <div class="info-grid">
        <div class="info-item">
          <span class="label">陪诊老人</span>
          <span class="value">{{ order.elderlyName }}</span>
        </div>
        <div class="info-item">
          <span class="label">就诊医院</span>
          <span class="value">{{ order.hospitalName }}</span>
        </div>
        <div class="info-item">
          <span class="label">就诊科室</span>
          <span class="value">{{ order.department }}</span>
        </div>
        <div class="info-item">
          <span class="label">预约时间</span>
          <span class="value">{{ order.appointmentTime }}</span>
        </div>
        <div class="info-item">
          <span class="label">服务时长</span>
          <span class="value">{{ order.duration }}小时</span>
        </div>
        <div class="info-item">
          <span class="label">订单金额</span>
          <span class="value amount">¥{{ order.amount }}</span>
        </div>
        <div class="info-item">
          <span class="label">支付状态</span>
          <el-tag :type="order.payStatus === 'PAID' ? 'success' : 'warning'" size="large">
            {{ order.payStatus === 'PAID' ? '已支付' : '未支付' }}
          </el-tag>
        </div>
      </div>
    </div>

    <div class="timeline-card" v-if="statusLogs.length > 0">
      <h2 class="section-title">状态跟踪</h2>
      <el-timeline>
        <el-timeline-item
          v-for="log in statusLogs"
          :key="log.id"
          :timestamp="log.createTime"
          placement="top"
          :color="(log.toStatus || log.status) === 'CANCELLED' ? 'var(--danger)' : 'var(--teal-deep)'"
        >
          <div class="timeline-content">
            <span class="log-status">{{ getStatusLabel(log.toStatus || log.status) }}</span>
            <span class="log-remark" v-if="log.remark">{{ log.remark }}</span>
          </div>
        </el-timeline-item>
      </el-timeline>
    </div>

    <div class="action-bar">
      <el-button
        v-if="order?.status === 'SETTLED'"
        type="primary"
        class="action-btn"
        @click="$router.push(`/family/evaluate/${order.id}`)"
      >
        去评价
      </el-button>

      <el-button
        v-if="canCancel"
        type="danger"
        class="action-btn"
        @click="showCancelDialog"
      >
        取消订单
      </el-button>

      <el-button
        type="primary"
        class="action-btn"
        @click="handleGenerateNotice"
        :loading="generating"
      >
        生成告知书
      </el-button>
    </div>

    <div class="notice-result" v-if="noticeId">
      <el-button
        type="primary"
        class="notice-link-btn"
        @click="$router.push(`/family/notice/${noticeId}`)"
      >
        查看告知书
      </el-button>
    </div>

    <el-dialog v-model="cancelVisible" title="取消订单" width="90%" :close-on-click-modal="false">
      <div class="cancel-dialog">
        <p class="cancel-tip">确定要取消该订单吗？取消后无法恢复。</p>
        <el-input
          v-model="cancelReason"
          type="textarea"
          :rows="3"
          placeholder="请输入取消原因（订单已被接单时必须填写）"
          class="cancel-input"
        />
      </div>
      <template #footer>
        <el-button @click="cancelVisible = false" size="large">再想想</el-button>
        <el-button type="danger" @click="handleCancel" :loading="canceling" size="large">
          确认取消
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute } from 'vue-router'
import { ArrowLeft } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import api from '@/api/request'
import { getOrderStatusLabel, getOrderStatusType, getOrderStatusClass } from '@/utils/orderStatus'

const route = useRoute()
const order = ref(null)
const statusLogs = ref([])
const cancelVisible = ref(false)
const cancelReason = ref('')
const canceling = ref(false)
const generating = ref(false)
const noticeId = ref(null)

const getStatusType = (status) => getOrderStatusType(status)
const getStatusLabel = (status) => getOrderStatusLabel(status)

const canCancel = computed(() => {
  if (!order.value) return false
  const status = order.value.status
  if (status === 'PENDING_DISPATCH' || status === 'DISPATCHED') return true
  if (status === 'ACCEPTED' && order.value.appointmentTime) {
    const appointmentTime = new Date(order.value.appointmentTime).getTime()
    const hoursUntil = (appointmentTime - Date.now()) / (1000 * 60 * 60)
    return hoursUntil > 2
  }
  return false
})

const showCancelDialog = () => {
  cancelReason.value = ''
  cancelVisible.value = true
}

const handleCancel = async () => {
  canceling.value = true
  try {
    const res = await api.put(`/order/${route.params.id}/cancel`, {
      cancelReason: cancelReason.value
    })
    if (res.code === 200) {
      ElMessage.success('订单已取消')
      cancelVisible.value = false
      loadOrder()
    } else {
      ElMessage.error(res.message || '取消失败')
    }
  } catch (e) {
    ElMessage.error('取消失败')
  } finally {
    canceling.value = false
  }
}

const handleGenerateNotice = async () => {
  generating.value = true
  try {
    const res = await api.post('/ai/notice/generate', { orderId: order.value.id })
    if (res.code === 200 && res.data) {
      const id = res.data.id
      if (id && typeof id === 'number') {
        noticeId.value = id
        ElMessage.success('告知书生成成功')
      } else {
        ElMessage.warning('内容已生成，请从就医档案列表查看')
      }
    } else {
      ElMessage.error(res.message || '生成失败')
    }
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || '生成失败')
  } finally {
    generating.value = false
  }
}

const loadOrder = async () => {
  try {
    const [orderRes, logRes] = await Promise.all([
      api.get(`/order/${route.params.id}`),
      api.get(`/order/${route.params.id}/status-log`)
    ])
    if (orderRes.code === 200 && orderRes.data) {
      order.value = orderRes.data
    }
    if (logRes.code === 200 && logRes.data) {
      statusLogs.value = Array.isArray(logRes.data) ? logRes.data : (logRes.data.records || [])
    }
  } catch (e) {
    ElMessage.error('加载订单详情失败')
  }
}

onMounted(loadOrder)
let poll = setInterval(loadOrder, 5000)
onUnmounted(() => clearInterval(poll))
</script>

<style scoped>
.order-detail-page {
  min-height: 100vh;
  background: var(--bg);
  padding-bottom: 30px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: var(--surface);
  border-bottom: 1px solid var(--line);
}

.page-title {
  font-size: 24px;
  font-weight: 700;
  margin: 0;
  color: var(--ink);
}

.detail-card {
  margin: 16px 20px;
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-card);
  padding: 20px;
}

.status-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--line);
}

.order-no {
  font-size: 20px;
  font-weight: 700;
  color: var(--ink);
}

.info-grid {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.info-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.info-item .label {
  font-size: 18px;
  color: var(--muted);
}

.info-item .value {
  font-size: 18px;
  color: var(--ink);
  font-weight: 500;
}

.info-item .amount {
  font-size: 22px;
  font-weight: 700;
  color: var(--amber);
}

.timeline-card {
  margin: 0 20px 16px;
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-card);
  padding: 20px;
}

.section-title {
  font-size: 22px;
  font-weight: 700;
  margin: 0 0 16px 0;
  color: var(--ink);
}

.timeline-content {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.log-status {
  font-size: 18px;
  font-weight: 600;
  color: var(--ink);
}

.log-remark {
  font-size: 16px;
  color: var(--muted-2);
}

.action-bar {
  margin: 0 20px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.action-btn {
  width: 100%;
  height: 52px;
  font-size: 18px;
  border-radius: 12px;
}

.notice-result {
  margin: 12px 20px;
}

.notice-link-btn {
  width: 100%;
  height: 48px;
  font-size: 20px;
  border-radius: 12px;
}

.cancel-dialog .cancel-tip {
  font-size: 18px;
  color: var(--muted);
  margin-bottom: 16px;
}

.cancel-input :deep(.el-textarea__inner) {
  font-size: 18px;
}

:deep(.el-timeline-item__timestamp) {
  font-size: 16px;
}

:deep(.el-tag--large) {
  font-size: 16px;
}
</style>