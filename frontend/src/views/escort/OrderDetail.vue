<template>
  <div class="order-detail">
    <div class="detail-header">
      <el-button @click="$router.back()" :icon="ArrowLeft">返回</el-button>
      <h2 class="page-title">订单详情</h2>
    </div>

    <div v-if="loading" class="loading-container">
      <el-skeleton :rows="8" animated />
    </div>

    <template v-else-if="order">
      <!-- Order Info -->
      <el-card shadow="never" class="info-card rv-rise" style="--i: 0">
        <template #header>
          <span>订单信息</span>
          <el-tag :type="statusTagType(order.status)" :class="getOrderStatusClass(order.status)" style="margin-left: 12px;">
            {{ statusLabel(order.status) }}
          </el-tag>
        </template>
        <el-descriptions :column="2" border>
          <el-descriptions-item label="订单编号">{{ order.orderNo }}</el-descriptions-item>
          <el-descriptions-item label="就诊人">{{ order.elderlyName }}</el-descriptions-item>
          <el-descriptions-item label="预约时间">{{ order.appointmentTime }}</el-descriptions-item>
          <el-descriptions-item label="就诊科室">{{ order.department }}</el-descriptions-item>
          <el-descriptions-item label="就诊医院">{{ order.hospitalName || order.hospital || "—" }}</el-descriptions-item>
          <el-descriptions-item label="服务城市">{{ order.serviceCity }}</el-descriptions-item>
          <el-descriptions-item label="备注">{{ order.remark || '-' }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ order.createTime }}</el-descriptions-item>
        </el-descriptions>
      </el-card>

      <!-- Service Progress -->
      <el-card shadow="never" class="progress-card rv-rise" style="--i: 1">
        <template #header><span>服务流程</span></template>
        <el-steps :active="progressStep" finish-status="success" align-center>
          <el-step title="待派单" />
          <el-step title="已接单" />
          <el-step title="服务中" />
          <el-step title="待结算" />
          <el-step title="已完成" />
        </el-steps>
      </el-card>

      <!-- Status Timeline -->
      <el-card shadow="never" class="timeline-card rv-rise" style="--i: 2">
        <template #header><span>状态时间线</span></template>
        <el-timeline>
          <el-timeline-item
            v-for="item in timeline"
            :key="item.status"
            :timestamp="item.time"
            :type="item.type"
          >
            {{ item.label }}
          </el-timeline-item>
        </el-timeline>
      </el-card>

      <!-- Action Buttons -->
      <div class="action-bar" v-if="order.status === 'ACCEPTED' || order.status === 'IN_SERVICE'">
        <el-button
          v-if="order.status === 'ACCEPTED'"
          type="primary"
          :loading="actioning"
          @click="handleStart"
        >
          开始服务
        </el-button>
        <el-button
          v-if="order.status === 'IN_SERVICE'"
          type="primary"
          :loading="actioning"
          @click="handleFinish"
        >
          结束服务
        </el-button>
      </div>

      <!-- Document Upload (IN_SERVICE) -->
      <el-card shadow="never" class="upload-card rv-rise" style="--i: 3" v-if="order.status === 'IN_SERVICE'">
        <template #header><span>上传医疗单据</span></template>
        <el-form label-width="100px">
          <el-form-item label="单据类型">
            <el-select v-model="docType" placeholder="请选择单据类型">
              <el-option label="检验单" value="LAB_REPORT" />
              <el-option label="处方" value="PRESCRIPTION" />
              <el-option label="病历" value="MEDICAL_RECORD" />
            </el-select>
          </el-form-item>
          <el-form-item label="上传图片">
            <el-upload
              :action="uploadUrl"
              :headers="uploadHeaders"
              list-type="picture-card"
              :limit="1"
              :on-success="onDocUploadSuccess"
              :on-error="onDocUploadError"
              :before-upload="beforeDocUpload"
              v-model:file-list="docFileList"
            >
              <el-icon><Plus /></el-icon>
            </el-upload>
          </el-form-item>
          <el-form-item v-if="uploadedDocUrl">
            <el-button type="primary" :loading="creating" @click="handleCreateRecord">
              创建档案并上传单据
            </el-button>
          </el-form-item>
          <el-form-item v-if="documentId">
            <el-button type="primary" :loading="ocrStarting" @click="handleStartOcr">
              发起OCR识别
            </el-button>
          </el-form-item>
        </el-form>
      </el-card>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, Plus } from '@element-plus/icons-vue'
import api from '@/api/request'
import { getOrderStatusLabel, getOrderStatusType, getOrderStatusClass } from '@/utils/orderStatus'
import { authHeaders } from '@/utils/fileUrl'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const order = ref(null)
const actioning = ref(false)
const docType = ref('LAB_REPORT')
const docFileList = ref([])
const uploadedDocUrl = ref('')
const documentId = ref(null)
const creating = ref(false)
const ocrStarting = ref(false)
const uploadUrl = '/api/file/upload'
const uploadHeaders = authHeaders()

function statusLabel(status) {
  return getOrderStatusLabel(status)
}

function statusTagType(status) {
  return getOrderStatusType(status)
}

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
  return map[order.value?.status] ?? 0
})

const timeline = computed(() => {
  if (!order.value) return []
  const items = []
  if (order.value.createTime) {
    items.push({ status: 'CREATED', label: '订单创建', time: order.value.createTime, type: 'primary' })
  }
  if (order.value.acceptTime) {
    items.push({ status: 'ACCEPTED', label: '陪诊员接单', time: order.value.acceptTime, type: 'success' })
  }
  if (order.value.startTime) {
    items.push({ status: 'IN_SERVICE', label: '开始服务', time: order.value.startTime, type: 'warning' })
  }
  if (order.value.finishTime) {
    items.push({ status: 'SERVICE_FINISHED', label: '服务结束', time: order.value.finishTime, type: 'success' })
  }
  return items
})

async function fetchOrder() {
  loading.value = true
  try {
    const res = await api.get(`/order/${route.params.id}`)
    order.value = res.data
  } catch (e) {
    ElMessage.error('获取订单详情失败')
  } finally {
    loading.value = false
  }
}

async function handleStart() {
  try {
    await ElMessageBox.confirm('确认开始服务吗？', '开始服务', {
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      type: 'info'
    })
  } catch {
    return
  }
  actioning.value = true
  try {
    await api.post(`/order/${order.value.id}/start`)
    ElMessage.success('已开始服务')
    await fetchOrder()
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || '操作失败')
  } finally {
    actioning.value = false
  }
}

async function handleFinish() {
  try {
    await ElMessageBox.confirm('确认结束服务吗？', '结束服务', {
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return
  }
  actioning.value = true
  try {
    await api.post(`/order/${order.value.id}/finish`)
    ElMessage.success('服务已结束')
    await fetchOrder()
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || '操作失败')
  } finally {
    actioning.value = false
  }
}

function onDocUploadSuccess(response) {
  // 后端 FileController 返回 Result<String>，data 即 /upload/xxx 相对路径
  if (response?.code === 200 && typeof response.data === 'string') {
    uploadedDocUrl.value = response.data
    ElMessage.success('图片上传成功')
  } else {
    ElMessage.error(response?.message || '图片上传失败')
  }
}

function onDocUploadError() {
  ElMessage.error('图片上传失败，请确认已登录且文件格式正确')
}

function beforeDocUpload(file) {
  const isImage = file.type === 'image/jpeg' || file.type === 'image/png'
  const isLt5M = file.size / 1024 / 1024 < 5
  if (!isImage) ElMessage.error('只能上传 JPG/PNG 格式的图片')
  if (!isLt5M) ElMessage.error('图片大小不能超过 5MB')
  return isImage && isLt5M
}

async function handleCreateRecord() {
  if (!docType.value || !uploadedDocUrl.value) {
    ElMessage.warning('请选择单据类型并上传图片')
    return
  }
  creating.value = true
  try {
    // 服务中订单：由后端按订单信息创建就医档案并挂载单据，返回 documentId
    const res = await api.post(`/record/escort/order/${order.value.id}/document`, null, {
      params: { docType: docType.value, imageUrl: uploadedDocUrl.value }
    })
    documentId.value = res.data?.id
    ElMessage.success('档案与单据创建成功')
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || '创建档案失败')
  } finally {
    creating.value = false
  }
}

async function handleStartOcr() {
  if (!documentId.value) {
    ElMessage.warning('请先创建记录')
    return
  }
  ocrStarting.value = true
  try {
    const res = await api.post('/ai/ocr/recognize', { documentId: documentId.value })
    const taskId = res.data?.taskId || res.data?.id
    ElMessage.success('OCR识别任务已启动')
    router.push(`/escort/ocr/${taskId}`)
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || 'OCR启动失败')
  } finally {
    ocrStarting.value = false
  }
}

onMounted(() => {
  fetchOrder()
  poll = setInterval(fetchOrder, 5000)
})
onUnmounted(() => clearInterval(poll))
let poll
</script>

<style scoped>
.order-detail {
  min-height: 100vh;
  background: var(--bg);
  padding: 0 20px 32px;
  max-width: 940px;
  margin: 0 auto;
}
.detail-header {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 12px 16px;
  margin: 0 -20px 20px;
  background: var(--surface);
  border-bottom: 1px solid var(--line);
}
.detail-header h2 {
  margin: 0;
  font-size: 22px;
  font-weight: 700;
  color: var(--ink);
  letter-spacing: -0.02em;
}
.loading-container {
  padding: 20px;
}
.info-card,
.progress-card,
.timeline-card,
.upload-card {
  margin-bottom: 16px;
}

.progress-card :deep(.el-step__title) {
  font-size: 12px;
}
.action-bar {
  margin-bottom: 16px;
  display: flex;
  gap: 12px;
}
</style>