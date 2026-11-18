<template>
  <div class="ocr-confirm">
    <div class="ocr-header">
      <el-button @click="$router.back()">返回</el-button>
      <h2>OCR识别结果确认</h2>
    </div>

    <!-- Loading State -->
    <div v-if="loading" class="loading-state">
      <el-icon class="is-loading" :size="48"><Loading /></el-icon>
      <p>识别中，请稍候...</p>
      <el-progress :percentage="progressPercent" :stroke-width="12" />
    </div>

    <!-- Success State -->
    <template v-else-if="task?.status === 'SUCCESS'">
      <div class="ocr-workspace">
        <el-row :gutter="20">
          <!-- Left: OCR Result JSON Editor -->
          <el-col :span="12">
            <el-card shadow="never" class="result-card">
              <template #header>
                <span>识别结果</span>
                <span
                  :class="['confidence-badge', confidence >= 0.85 ? 'conf-high' : 'conf-low']"
                >
                  置信度: {{ (confidence * 100).toFixed(1) }}%
                </span>
              </template>
              <div v-if="confidence < 0.85" class="confirm-warning">
                <el-icon color="var(--amber)"><WarningFilled /></el-icon>
                <span>识别置信度较低，需人工确认</span>
              </div>
              <el-alert
                v-if="parseFailed"
                type="warning"
                :closable="false"
                title="识别结果不是标准JSON，可直接编辑原始文本后确认入库"
                style="margin-bottom: 12px;"
              />
              <el-form v-if="!parseFailed" label-width="110px">
                <el-form-item
                  v-for="(value, key) in editableResult"
                  :key="key"
                  :label="key"
                >
                  <el-input
                    v-model="editableResult[key]"
                    type="textarea"
                    :autosize="{ minRows: 1, maxRows: 6 }"
                  />
                </el-form-item>
              </el-form>
              <el-input
                v-else
                v-model="rawResult"
                type="textarea"
                :rows="10"
                placeholder="识别结果原文"
              />
            </el-card>
          </el-col>

          <!-- Right: Original Image -->
          <el-col :span="12">
            <el-card shadow="never" class="image-card">
              <template #header><span>原始图片</span></template>
              <el-image
                v-if="task.imageUrl"
                :src="displayImageUrl"
                fit="contain"
                style="width: 100%;"
                :preview-src-list="[displayImageUrl]"
              />
              <el-empty v-else description="暂无图片" />
            </el-card>
          </el-col>
        </el-row>

        <!-- Confirm Button -->
        <div class="action-bar">
          <el-button type="primary" :loading="confirming" @click="handleConfirm">
            确认入库
          </el-button>
          <el-button @click="$router.back()">返回修改</el-button>
        </div>
      </div>
    </template>

    <!-- Failed/Degraded State -->
    <template v-else-if="task?.status === 'FAILED' || task?.status === 'DEGRADED'">
      <el-result icon="error" title="识别失败" :sub-title="task.errorMsg || 'OCR识别未能完成，请尝试手动录入。'">
        <template #extra>
          <el-button type="primary" @click="handleManual">手动录入</el-button>
          <el-button @click="$router.back()">返回</el-button>
        </template>
      </el-result>
    </template>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Loading, WarningFilled } from '@element-plus/icons-vue'
import api from '@/api/request'
import { fileUrl } from '@/utils/fileUrl'

const route = useRoute()
const router = useRouter()

const loading = ref(true)
const confirming = ref(false)
const task = ref(null)
const editableResult = reactive({})
const rawResult = ref('')
const parseFailed = ref(false)
const progressPercent = ref(0)
let pollTimer = null

const confidence = computed(() => Number(task.value?.ocrConfidence ?? 0))
const displayImageUrl = computed(() => fileUrl(task.value?.imageUrl))

const toEditable = v => {
  if (v === null || v === undefined) return ''
  if (typeof v === 'object') return JSON.stringify(v, null, 2)
  return String(v)
}

const fromEditable = v => {
  const s = String(v ?? '')
  const trimmed = s.trim()
  if (trimmed.startsWith('{') || trimmed.startsWith('[')) {
    try { return JSON.parse(trimmed) } catch { return s }
  }
  if (trimmed === 'true') return true
  if (trimmed === 'false') return false
  if (trimmed !== '' && !Number.isNaN(Number(trimmed))) return Number(trimmed)
  return s
}

const buildCorrectedResult = () => {
  if (parseFailed.value) return rawResult.value
  const result = {}
  Object.entries(editableResult).forEach(([k, v]) => { result[k] = fromEditable(v) })
  return result
}

async function fetchTask() {
  try {
    const res = await api.get(`/ai/ocr/task/${route.params.taskId}`)
    task.value = res.data
    if (res.data?.status === 'SUCCESS') {
      Object.keys(editableResult).forEach(k => delete editableResult[k])
      rawResult.value = res.data.ocrResult || ''
      try {
        const parsed = JSON.parse(rawResult.value)
        // 兼容 {"result":{业务字段},"confidence":0.95} 的OCR外层包装
        const result = parsed && typeof parsed === 'object' && parsed.result &&
          typeof parsed.result === 'object' && !Array.isArray(parsed.result)
          ? parsed.result : parsed
        if (result && typeof result === 'object' && !Array.isArray(result)) {
          Object.entries(result).forEach(([k, v]) => { editableResult[k] = toEditable(v) })
          parseFailed.value = false
        } else {
          parseFailed.value = true
        }
      } catch {
        parseFailed.value = true
      }
      loading.value = false
      stopPolling()
    } else if (res.data?.status === 'FAILED' || res.data?.status === 'DEGRADED') {
      loading.value = false
      stopPolling()
    } else {
      // Still processing - keep polling
      progressPercent.value = Math.min(progressPercent.value + 5, 90)
    }
  } catch (e) {
    loading.value = false
    stopPolling()
    ElMessage.error('获取OCR任务失败')
  }
}

function startPolling() {
  fetchTask()
  pollTimer = setInterval(() => {
    fetchTask()
  }, 3000)
}

function stopPolling() {
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
  }
}

async function handleConfirm() {
  confirming.value = true
  try {
    await api.post(`/ai/ocr/task/${route.params.taskId}/confirm`, {
      correctedResult: buildCorrectedResult()
    })
    ElMessage.success('入库成功')
    router.back()
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || '入库失败')
  } finally {
    confirming.value = false
  }
}

function handleManual() {
  ElMessage.info('跳转到手动录入页面')
  // router.push(`/escort/manual/${route.params.taskId}`)
}

onMounted(() => {
  startPolling()
})

onUnmounted(() => {
  stopPolling()
})
</script>

<style scoped>
.ocr-confirm {
  min-height: 100vh;
  background: var(--bg);
  padding: 0 20px 32px;
  max-width: 1240px;
  margin: 0 auto;
}
.ocr-header {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 12px 16px;
  margin: 0 -20px 20px;
  background: var(--surface);
  border-bottom: 1px solid var(--line);
}
.ocr-header h2 {
  margin: 0;
  font-size: 22px;
  font-weight: 700;
  color: var(--ink);
  letter-spacing: -0.02em;
}
.loading-state {
  text-align: center;
  padding: 80px 20px;
}
.loading-state p {
  margin: 16px 0;
  font-size: 16px;
  color: var(--muted);
}
.ocr-workspace {
  margin-top: 16px;
}
.result-card,
.image-card {
  height: 100%;
}
.confidence-badge {
  margin-left: 12px;
  padding: 3px 10px;
  border-radius: 999px;
  font-size: 12.5px;
  font-weight: 600;
}
.conf-high {
  background: var(--success-soft);
  color: var(--success);
}
.conf-low {
  background: var(--danger-soft);
  color: var(--danger);
}
.confirm-warning {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 12px;
  background: var(--amber-soft);
  border: 1px solid var(--amber-soft);
  border-radius: var(--radius-sm);
  margin-bottom: 12px;
  color: var(--amber);
  font-size: 13.5px;
}
.action-bar {
  margin-top: 20px;
  display: flex;
  gap: 12px;
  justify-content: center;
}
</style>