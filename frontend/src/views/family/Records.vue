<template>
  <div class="records-page">
    <FamilyTopNav />

    <main class="page-wrap">
      <button type="button" class="mobile-back" @click="$router.push('/family/home')">
        <el-icon :size="15"><ArrowLeft /></el-icon>
        返回工作台
      </button>

      <header class="page-intro rv-fade">
        <div>
          <h1 class="page-heading">就医档案</h1>
          <p class="page-sub">按就诊时间查看家人的病历摘要与相关检查报告。</p>
        </div>
        <el-button type="primary" @click="openCreate">
          <el-icon :size="16" style="margin-right:4px"><Plus /></el-icon>
          新建病历
        </el-button>
      </header>

      <div v-if="records.length > 0" class="records-grid">
        <article
          v-for="(record, index) in records"
          :key="record.id"
          class="record-card surface-card rv-rise"
          :style="{ '--i': index }"
        >
          <div class="card-header">
            <h2 class="record-title">{{ record.elderlyName || '就医记录' }}</h2>
            <span class="visit-date">{{ record.visitDate }}</span>
          </div>

          <div class="card-mid">
            <span class="department">{{ record.department || '未填写科室' }}</span>
          </div>

          <p class="diagnosis">{{ truncateText(record.diagnosis, 72) }}</p>

          <dl class="record-meta">
            <div>
              <dt>主诉</dt>
              <dd>{{ record.chiefComplaint || '未记录' }}</dd>
            </div>
            <div>
              <dt>用药</dt>
              <dd>{{ record.prescription || '未记录' }}</dd>
            </div>
          </dl>

          <div class="card-footer">
            <button
              type="button"
              class="detail-link"
              @click="$router.push(`/family/record/${record.id}`)"
            >
              查看详情
              <el-icon :size="14"><ArrowRight /></el-icon>
            </button>
          </div>
        </article>
      </div>

      <div v-else class="empty-state surface-card rv-fade">
        <span class="empty-icon" aria-hidden="true">
          <el-icon :size="28"><Folder /></el-icon>
        </span>
        <h2 class="text-h2">暂无就医档案</h2>
        <p class="text-note">陪诊服务完成后，病历与检查报告会归档在这里。</p>
      </div>

      <div v-if="total > 0" class="records-footer">
        <span class="text-note">共 {{ total }} 条就医记录</span>
        <el-pagination
          v-model:current-page="pageNo"
          :page-size="pageSize"
          :total="total"
          background
          layout="prev, pager, next"
          :pager-count="5"
          @current-change="fetchRecords"
        />
      </div>
    </main>

    <!-- 新建病历（含图片上传） -->
    <el-dialog v-model="createVisible" title="新建就医病历" width="600px" destroy-on-close>
      <el-form ref="createFormRef" :model="createForm" :rules="createRules" label-width="95px">
        <el-form-item label="就诊老人" prop="elderlyId">
          <el-select v-model="createForm.elderlyId" placeholder="请选择老人" style="width:100%">
            <el-option v-for="e in elderlyList" :key="e.id" :label="e.name" :value="e.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="就诊医院" prop="hospitalId">
          <el-select v-model="createForm.hospitalId" placeholder="请选择医院" style="width:100%">
            <el-option v-for="h in hospitalList" :key="h.id" :label="h.name" :value="h.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="就诊科室" prop="department">
          <el-input v-model="createForm.department" maxlength="50" placeholder="如：心内科" />
        </el-form-item>
        <el-form-item label="就诊日期" prop="visitDate">
          <el-date-picker v-model="createForm.visitDate" type="date" value-format="YYYY-MM-DD"
            placeholder="选择就诊日期" style="width:100%" />
        </el-form-item>
        <el-form-item label="主诉">
          <el-input v-model="createForm.chiefComplaint" type="textarea" :rows="2" maxlength="500" />
        </el-form-item>
        <el-form-item label="诊断结论">
          <el-input v-model="createForm.diagnosis" type="textarea" :rows="2" maxlength="500" />
        </el-form-item>
        <el-form-item label="医嘱/处方">
          <el-input v-model="createForm.prescription" type="textarea" :rows="2" maxlength="4000" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="createForm.remark" maxlength="500" />
        </el-form-item>
        <el-form-item label="单据类型">
          <el-select v-model="docType" style="width:100%">
            <el-option label="检验单" value="LAB_REPORT" />
            <el-option label="处方" value="PRESCRIPTION" />
            <el-option label="病历" value="MEDICAL_RECORD" />
          </el-select>
        </el-form-item>
        <el-form-item label="病历图片">
          <el-upload
            :action="uploadUrl"
            :headers="uploadHeaders"
            list-type="picture-card"
            :limit="1"
            :on-success="onUploadSuccess"
            :on-error="onUploadError"
            :on-remove="onUploadRemove"
            :before-upload="beforeUpload"
          >
            <el-icon><Plus /></el-icon>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="creating" @click="submitCreate">保存病历</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ArrowLeft, ArrowRight, Folder, Plus } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import api from '@/api/request'
import FamilyTopNav from '@/components/FamilyTopNav.vue'
import { authHeaders } from '@/utils/fileUrl'

const records = ref([])
const pageNo = ref(1)
const pageSize = ref(10)
const total = ref(0)

const createVisible = ref(false)
const creating = ref(false)
const createFormRef = ref(null)
const elderlyList = ref([])
const hospitalList = ref([])
const uploadedImageUrl = ref('')
const docType = ref('MEDICAL_RECORD')
const uploadUrl = '/api/file/upload'
const uploadHeaders = authHeaders()

const createForm = ref({
  elderlyId: null,
  hospitalId: null,
  department: '',
  visitDate: '',
  chiefComplaint: '',
  diagnosis: '',
  prescription: '',
  remark: ''
})

const createRules = {
  elderlyId: [{ required: true, message: '请选择就诊老人', trigger: 'change' }],
  hospitalId: [{ required: true, message: '请选择就诊医院', trigger: 'change' }],
  department: [{ required: true, message: '请输入就诊科室', trigger: 'blur' }],
  visitDate: [{ required: true, message: '请选择就诊日期', trigger: 'change' }]
}

async function openCreate() {
  createForm.value = {
    elderlyId: null, hospitalId: null, department: '', visitDate: '',
    chiefComplaint: '', diagnosis: '', prescription: '', remark: ''
  }
  uploadedImageUrl.value = ''
  docType.value = 'MEDICAL_RECORD'
  createVisible.value = true
  try {
    const [elderlyRes, hospitalRes] = await Promise.all([
      api.get('/elderly/page', { params: { current: 1, size: 1000 } }),
      api.get('/hospital/list')
    ])
    elderlyList.value = elderlyRes.data?.records || []
    hospitalList.value = hospitalRes.data || []
  } catch {
    ElMessage.error('加载老人/医院列表失败')
  }
}

function beforeUpload(file) {
  const isImage = file.type === 'image/jpeg' || file.type === 'image/png'
  const isLt5M = file.size / 1024 / 1024 < 5
  if (!isImage) ElMessage.error('只能上传 JPG/PNG 格式的图片')
  if (!isLt5M) ElMessage.error('图片大小不能超过 5MB')
  return isImage && isLt5M
}

function onUploadSuccess(response) {
  if (response?.code === 200 && typeof response.data === 'string') {
    uploadedImageUrl.value = response.data
    ElMessage.success('病历图片上传成功')
  } else {
    ElMessage.error(response?.message || '上传失败')
  }
}

function onUploadError() {
  ElMessage.error('病历图片上传失败')
}

function onUploadRemove() {
  uploadedImageUrl.value = ''
}

async function submitCreate() {
  const valid = await createFormRef.value.validate().catch(() => false)
  if (!valid) return
  creating.value = true
  try {
    const res = await api.post('/record', createForm.value)
    const recordId = res.data?.id
    if (uploadedImageUrl.value && recordId) {
      await api.post(`/record/${recordId}/document`, null, {
        params: { docType: docType.value, imageUrl: uploadedImageUrl.value }
      })
    }
    ElMessage.success('病历已保存')
    createVisible.value = false
    pageNo.value = 1
    await fetchRecords()
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || '保存病历失败')
  } finally {
    creating.value = false
  }
}

const truncateText = (text, maxLen) => {
  if (!text) return '-'
  return text.length > maxLen ? text.substring(0, maxLen) + '...' : text
}

const fetchRecords = async () => {
  try {
    const res = await api.get('/record/page', {
      params: { current: pageNo.value, size: pageSize.value }
    })
    if (res.code === 200 && res.data) {
      records.value = res.data.records || []
      total.value = res.data.total || 0
    }
  } catch (e) {
    console.error('Failed to load records:', e)
  }
}

onMounted(fetchRecords)
</script>

<style scoped>
.records-page {
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

.records-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(340px, 1fr));
  gap: 20px;
  align-items: stretch;
}

.record-card {
  display: flex;
  flex-direction: column;
  padding: 22px;
  cursor: default;
  transition: transform 0.18s ease, border-color 0.18s ease,
    box-shadow 0.18s ease;
}

.record-card:hover {
  transform: translateY(-2px);
  border-color: var(--teal-light);
  box-shadow: 0 10px 28px -16px rgba(16, 24, 40, 0.22);
}

.card-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.record-title {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  line-height: 1.4;
  color: var(--ink);
}

.visit-date {
  flex-shrink: 0;
  font-size: 12px;
  color: var(--muted-2);
  font-variant-numeric: tabular-nums;
}

.card-mid {
  margin-top: 12px;
}

.department {
  display: inline-flex;
  padding: 3px 8px;
  background: var(--teal-soft);
  border-radius: 6px;
  font-size: 12px;
  font-weight: 500;
  color: var(--teal-deep);
}

.diagnosis {
  margin: 14px 0 0;
  font-size: 13.5px;
  line-height: 1.7;
  color: var(--ink-3);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.record-meta {
  display: grid;
  gap: 8px;
  margin: 14px 0 0;
  padding: 10px 12px;
  background: var(--surface-2);
  border-radius: 6px;
}

.record-meta > div {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.record-meta dt {
  flex-shrink: 0;
  font-size: 11.5px;
  color: var(--muted);
}

.record-meta dd {
  margin: 0;
  max-width: 68%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 12.5px;
  color: var(--ink-3);
  text-align: right;
}

.card-footer {
  margin-top: auto;
  padding-top: 16px;
  border-top: 1px dashed var(--line);
}

.detail-link {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 0;
  background: none;
  border: none;
  font-family: inherit;
  font-size: 13px;
  font-weight: 600;
  color: var(--teal-deep);
  cursor: pointer;
}

.detail-link:hover {
  text-decoration: underline;
  text-underline-offset: 3px;
}

.records-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-top: 18px;
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
  width: 56px;
  height: 56px;
  display: grid;
  place-items: center;
  background: var(--surface-3);
  border: 1px solid var(--line);
  border-radius: 12px;
  color: var(--muted);
  margin-bottom: 4px;
}

@media (max-width: 1000px) {
  .page-wrap {
    padding: 20px 16px 40px;
  }

  .mobile-back {
    display: inline-flex;
  }

  .record-card {
    padding: 18px;
  }
}

@media (max-width: 700px) {
  .records-footer {
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
  }
}
</style>
