<template>
  <div class="escort-profile">
    <header class="page-header">
      <el-button text @click="$router.push('/escort/home')">
        <el-icon :size="22"><ArrowLeft /></el-icon>
      </el-button>
      <h1 class="page-title">个人中心</h1>
      <div style="width: 32px;"></div>
    </header>

    <main class="page-body">
    <div v-if="loading" class="loading-container">
      <el-skeleton :rows="6" animated />
    </div>

    <template v-else-if="profile">
      <!-- Read-only Info -->
      <el-card shadow="never" class="info-card">
        <template #header><span>基本信息</span></template>
        <el-descriptions :column="2" border>
          <el-descriptions-item label="真实姓名">{{ profile.realName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="平均评分">
            <el-rate
              :model-value="profile.avgRating || 0"
              disabled
              show-score
              text-color="var(--star)"
              size="small"
            />
            ({{ (profile.avgRating || 0).toFixed(1) }})
          </el-descriptions-item>
          <el-descriptions-item label="累计完成订单">{{ profile.totalOrders || 0 }}</el-descriptions-item>
          <el-descriptions-item label="服务城市">{{ profile.serviceCity || '-' }}</el-descriptions-item>
        </el-descriptions>
      </el-card>

      <!-- Editable Fields -->
      <el-card shadow="never" class="edit-card">
        <template #header><span>编辑信息</span></template>
        <el-form
          ref="formRef"
          :model="form"
          label-width="100px"
          label-position="right"
        >
          <el-form-item label="服务城市" prop="serviceCity">
            <el-input v-model="form.serviceCity" placeholder="请输入服务城市" />
          </el-form-item>

          <el-form-item label="服务区域" prop="serviceArea">
            <el-input v-model="form.serviceArea" placeholder="请输入服务区域" />
          </el-form-item>

          <el-form-item label="个人介绍" prop="intro">
            <el-input
              v-model="form.intro"
              type="textarea"
              :rows="4"
              placeholder="请介绍您的工作经验和擅长领域"
              maxlength="500"
              show-word-limit
            />
          </el-form-item>

          <el-form-item>
            <el-button type="primary" :loading="saving" @click="handleSave">
              保存修改
            </el-button>
          </el-form-item>
        </el-form>
      </el-card>
    </template>
    </main>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'
import api from '@/api/request'

const loading = ref(true)
const saving = ref(false)
const profile = ref(null)
const formRef = ref(null)

const form = reactive({
  serviceCity: '',
  serviceArea: '',
  intro: ''
})

async function fetchProfile() {
  loading.value = true
  try {
    const res = await api.get('/escort/profile')
    profile.value = res.data
    form.serviceCity = res.data?.serviceCity || ''
    form.serviceArea = res.data?.serviceArea || ''
    form.intro = res.data?.intro || ''
  } catch (e) {
    ElMessage.error('获取个人信息失败')
  } finally {
    loading.value = false
  }
}

async function handleSave() {
  saving.value = true
  try {
    await api.put('/escort/profile', {
      serviceCity: form.serviceCity,
      serviceArea: form.serviceArea,
      intro: form.intro
    })
    ElMessage.success('保存成功')
    await fetchProfile()
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

onMounted(() => {
  fetchProfile()
})
</script>

<style scoped>
.escort-profile {
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
  max-width: 760px;
  margin: 0 auto;
  padding: 20px;
}
.loading-container {
  padding: 20px;
}
.info-card {
  margin-bottom: 20px;
}
.edit-card {
  margin-bottom: 20px;
}
</style>