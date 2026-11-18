<template>
  <div class="order-create-page">
    <div class="page-header">
      <el-button icon="ArrowLeft" text @click="$router.back()">
        <el-icon :size="22"><ArrowLeft /></el-icon>
      </el-button>
      <h1 class="page-title">预约陪诊</h1>
      <div style="width: 32px;"></div>
    </div>

    <div class="steps-wrap">
      <el-steps :active="activeStep" align-center>
        <el-step title="选择老人" />
        <el-step title="填写信息" />
        <el-step title="确认下单" />
      </el-steps>
    </div>

    <!-- Step 1: 选择老人 -->
    <div class="step-content" v-if="activeStep === 0">
      <div
        v-for="(elderly, index) in elderlyList"
        :key="elderly.id"
        class="elderly-card rv-rise"
        :class="{ selected: selectedElderly?.id === elderly.id }"
        :style="{ '--i': index }"
        @click="selectElderly(elderly)"
      >
        <div class="card-left">
          <div class="avatar">{{ elderly.name?.charAt(0) || '老' }}</div>
          <div class="info">
            <span class="name">{{ elderly.name }}</span>
            <span class="mobility">{{ getMobilityLabel(elderly.mobility) }}</span>
          </div>
        </div>
        <el-icon v-if="selectedElderly?.id === elderly.id" :size="24" color="var(--teal-deep)">
          <SuccessFilled />
        </el-icon>
      </div>
      <el-empty v-if="elderlyList.length === 0" description="请先添加老人档案" :image-size="80">
        <el-button type="primary" size="large" @click="$router.push('/family/elderly/add')" style="height:48px;font-size:20px">去添加老人档案</el-button>
      </el-empty>
    </div>

    <!-- Step 2: 填写信息 -->
    <div class="step-content" v-if="activeStep === 1">
      <el-form ref="formRef" :model="orderForm" :rules="rules" label-position="top" size="large">
        <el-form-item label="就诊医院" prop="hospitalId">
          <el-select v-model="orderForm.hospitalId" placeholder="请选择医院" style="width:100%" size="large">
            <el-option v-for="h in hospitals" :key="h.id" :label="h.name" :value="h.id" /></el-select>
        </el-form-item>

        <el-form-item label="就诊科室" prop="department">
          <el-input v-model="orderForm.department" placeholder="请输入科室名称" />
        </el-form-item>

        <el-form-item label="服务类型" prop="serviceType">
          <el-select v-model="orderForm.serviceType" placeholder="请选择服务类型" style="width: 100%">
            <el-option label="基础陪诊 50元/小时" value="BASIC" />
            <el-option label="陪同检查 80元/小时" value="EXAM" />
          </el-select>
        </el-form-item>

        <el-form-item label="预约时间" prop="appointmentTime">
          <el-date-picker
            v-model="orderForm.appointmentTime"
            type="datetime"
            placeholder="请选择预约时间"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width: 100%"
            :disabled-date="disabledDate"
          />
        </el-form-item>

        <el-form-item label="服务时长(小时)" prop="duration">
          <el-input-number
            v-model="orderForm.duration"
            :min="1"
            :max="24"
            style="width: 100%"
            placeholder="请输入服务时长"
          />
        </el-form-item>
      </el-form>

      <div class="estimate-box" v-if="estimatedAmount !== null">
        <span class="estimate-label">预估费用</span>
        <span class="estimate-amount">¥{{ estimatedAmount.toFixed(2) }}</span>
      </div>

      <el-button
        type="primary"
        class="estimate-btn"
        @click="handleEstimate"
        :loading="estimating"
      >
        费用试算
      </el-button>
    </div>

    <!-- Step 3: 确认下单 -->
    <div class="step-content" v-if="activeStep === 2">
      <div class="confirm-card">
        <h3 class="confirm-title">订单确认</h3>
        <div class="confirm-item">
          <span class="label">陪诊老人</span>
          <span class="value">{{ selectedElderly?.name }}</span>
        </div>
        <div class="confirm-item">
          <span class="label">就诊医院</span>
          <span class="value">{{ hospitals.find(h=>h.id===orderForm.hospitalId)?.name || '' }}</span>
        </div>
        <div class="confirm-item">
          <span class="label">就诊科室</span>
          <span class="value">{{ orderForm.department }}</span>
        </div>
        <div class="confirm-item">
          <span class="label">服务类型</span>
          <span class="value">{{ orderForm.serviceType === 'BASIC' ? '基础陪诊' : '陪同检查' }}</span>
        </div>
        <div class="confirm-item">
          <span class="label">预约时间</span>
          <span class="value">{{ orderForm.appointmentTime }}</span>
        </div>
        <div class="confirm-item">
          <span class="label">服务时长</span>
          <span class="value">{{ orderForm.duration }}小时</span>
        </div>
        <div class="confirm-divider"></div>
        <div class="confirm-item total">
          <span class="label">预估费用</span>
          <span class="amount">¥{{ estimatedAmount?.toFixed(2) || '0.00' }}</span>
        </div>
      </div>
    </div>

    <!-- 底部操作栏 -->
    <div class="bottom-bar">
      <el-button
        v-if="activeStep > 0"
        class="nav-btn prev-btn"
        @click="activeStep--"
      >
        上一步
      </el-button>
      <el-button
        v-if="activeStep < 2"
        type="primary"
        class="nav-btn next-btn"
        @click="handleNext"
      >
        下一步
      </el-button>
      <el-button
        v-if="activeStep === 2"
        type="primary"
        class="nav-btn submit-btn"
        :loading="submitting"
        @click="handleSubmit"
      >
        确认下单
      </el-button>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowLeft, SuccessFilled } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import api from '@/api/request'

const router = useRouter()

const activeStep = ref(0)
const elderlyList = ref([])
const selectedElderly = ref(null)
const formRef = ref(null)
const estimating = ref(false)
const estimatedAmount = ref(null)
const requestId = ref(crypto.randomUUID())
const submitting = ref(false)
const hospitals = ref([])

const orderForm = ref({
  hospitalId: null,
  department: '',
  serviceType: '',
  appointmentTime: '',
  duration: 1
})

const rules = {
  hospitalId: [{ required: true, message: '请选择就诊医院', trigger: 'change' }],
  department: [{ required: true, message: '请输入就诊科室', trigger: 'blur' }],
  serviceType: [{ required: true, message: '请选择服务类型', trigger: 'change' }],
  appointmentTime: [{ required: true, message: '请选择预约时间', trigger: 'change' }],
  duration: [{ required: true, message: '请输入服务时长', trigger: 'blur' }]
}

const getMobilityLabel = (mobility) => {
  const map = { WALK: '自主行走', ASSIST: '需搀扶', WHEELCHAIR: '轮椅' }
  return map[mobility] || '-'
}

const disabledDate = (time) => {
  return time.getTime() < Date.now() - 86400000
}

// 服务类型/时长变更时清空试算结果，强制重新试算
watch(() => orderForm.value.serviceType, () => { estimatedAmount.value = null })
watch(() => orderForm.value.duration, () => { estimatedAmount.value = null })

const selectElderly = (elderly) => {
  selectedElderly.value = elderly
}

const handleNext = async () => {
  if (activeStep.value === 0) {
    if (!selectedElderly.value) {
      ElMessage.warning('请选择一位老人')
      return
    }
    activeStep.value = 1
  } else if (activeStep.value === 1) {
    const valid = await formRef.value.validate().catch(() => false)
    if (!valid) return
    if (estimatedAmount.value === null) {
      ElMessage.warning('请先进行费用试算')
      return
    }
    activeStep.value = 2
  }
}

const handleEstimate = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  estimating.value = true
  try {
    const res = await api.post('/order/estimate', {
      serviceType: orderForm.value.serviceType,
      duration: orderForm.value.duration
    })
    if (res.code === 200 && res.data) {
      estimatedAmount.value = res.data.totalAmount || 0
      ElMessage.success('费用估算完成')
    } else {
      ElMessage.error(res.message || '费用试算失败')
    }
  } catch (e) {
    ElMessage.error('费用试算失败')
  } finally {
    estimating.value = false
  }
}

const handleSubmit = async () => {
  submitting.value = true
  try {
    const res = await api.post('/order', {
      requestId: requestId.value,
      elderlyId: selectedElderly.value.id,
      hospitalId: orderForm.value.hospitalId,
      department: orderForm.value.department,
      serviceType: orderForm.value.serviceType,
      appointmentTime: orderForm.value.appointmentTime,
      duration: orderForm.value.duration,
      amount: estimatedAmount.value
    })
    if (res.code === 200) {
      ElMessage.success('下单成功')
      router.push('/family/orders')
    } else {
      ElMessage.error(res.message || '下单失败')
    }
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || e?.message || '下单失败')
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  try {
    const hRes = await api.get('/hospital/list')
    if (hRes.data) hospitals.value = hRes.data
    const res = await api.get('/elderly/page')
    if (res.code === 200 && res.data) {
      elderlyList.value = res.data.records || []
    }
  } catch (e) {
    ElMessage.error('加载老人列表失败')
  }
})
</script>

<style scoped>
.order-create-page {
  min-height: 100vh;
  background: var(--bg);
  padding-bottom: 100px;
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

.steps-wrap {
  padding: 20px;
  background: var(--surface);
  border-bottom: 1px solid var(--line);
  margin-bottom: 12px;
}

.step-content {
  padding: 16px 20px;
}

.elderly-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
  padding: 16px 20px;
  margin-bottom: 12px;
  cursor: pointer;
  transition: border-color 0.18s ease, box-shadow 0.18s ease,
    transform 0.18s ease;
}

.elderly-card:hover {
  transform: translateY(-1px);
  border-color: var(--teal-light);
  box-shadow: var(--shadow-card);
}

.elderly-card.selected {
  border-color: var(--teal);
  background: var(--teal-soft);
  box-shadow: 0 0 0 3px rgba(47, 128, 237, 0.14);
}

.card-left {
  display: flex;
  align-items: center;
  gap: 14px;
}

.avatar {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: var(--teal-deep);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
  font-weight: 700;
  flex-shrink: 0;
}

.name {
  font-size: 20px;
  font-weight: 600;
  color: var(--ink);
}

.mobility {
  font-size: 16px;
  color: var(--muted-2);
  margin-left: 10px;
}

.estimate-box {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
  margin: 16px 0;
}

.estimate-label {
  font-size: 18px;
  color: var(--muted);
}

.estimate-amount {
  font-size: 28px;
  font-weight: 700;
  color: var(--amber);
}

.estimate-btn {
  width: 100%;
  height: 48px;
  font-size: 18px;
  border-radius: 12px;
}

.confirm-card {
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-card);
  padding: 24px 20px;
}

.confirm-title {
  font-size: 22px;
  font-weight: 700;
  text-align: center;
  margin: 0 0 20px 0;
  color: var(--ink);
}

.confirm-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 0;
}

.confirm-item .label {
  font-size: 18px;
  color: var(--muted);
}

.confirm-item .value {
  font-size: 18px;
  color: var(--ink);
  font-weight: 500;
}

.confirm-divider {
  border-top: 1px dashed var(--line-strong);
  margin: 8px 0;
}

.confirm-item.total .amount {
  font-size: 28px;
  font-weight: 700;
  color: var(--amber);
}

.bottom-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  display: flex;
  gap: 12px;
  padding: 12px 20px calc(12px + env(safe-area-inset-bottom));
  background: var(--surface);
  border-top: 1px solid var(--line);
  z-index: 100;
}

.nav-btn {
  flex: 1;
  height: 52px;
  font-size: 18px;
  border-radius: 12px;
}

.prev-btn {
  background: var(--surface-3);
  border-color: var(--line-strong);
  color: var(--muted);
}

:deep(.el-step__title) {
  font-size: 16px;
}

:deep(.el-form-item__label) {
  font-size: 18px;
  font-weight: 600;
  color: var(--ink);
}

:deep(.el-input__inner) {
  font-size: 18px;
}

:deep(.el-select .el-input__inner) {
  font-size: 18px;
}

:deep(.el-input-number .el-input__inner) {
  font-size: 18px;
}
</style>