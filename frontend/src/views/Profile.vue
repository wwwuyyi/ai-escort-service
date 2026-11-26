<!-- 三端共享个人信息与修改密码页 -->
<template>
  <div class="profile-page">
    <header class="page-header">
      <el-button text @click="goHome">
        <el-icon :size="22"><ArrowLeft /></el-icon>
      </el-button>
      <h1 class="page-title">个人中心</h1>
      <div style="width: 32px;"></div>
    </header>

    <div v-if="loading" style="padding:40px;text-align:center">
      <el-icon :size="32" class="is-loading"><Loading /></el-icon>
    </div>

    <template v-else>
      <div
        v-if="profile.role === 'FAMILY'"
        style="width:min(1120px,100%);margin:16px auto 0;padding:0 24px;"
      >
        <el-card shadow="never" style="border-radius:12px;">
          <div style="display:flex;align-items:center;justify-content:space-between;gap:16px;">
            <div>
              <div style="font-size:18px;font-weight:700;">适老化显示模式</div>
              <div style="font-size:14px;color:var(--muted);margin-top:4px;">
                放大字号、按钮与输入框；默认关闭，仅影响本机当前浏览器。
              </div>
            </div>
            <el-switch v-model="a11yLarge" @change="setA11yMode" />
          </div>
        </el-card>
      </div>

      <div class="profile-grid">
      <!-- 基本信息卡片 -->
      <el-card class="info-card rv-rise" style="--i: 0">
        <template #header><span style="font-size:18px;font-weight:700">基本信息</span></template>
        <el-form :model="form" label-width="100px" size="large">
          <el-form-item label="手机号">
            <span class="readonly">{{ profile.username }}</span>
          </el-form-item>
          <el-form-item label="昵称">
            <el-input v-model="form.nickname" placeholder="请输入昵称" />
          </el-form-item>
          <el-form-item label="联系电话">
            <el-input v-model="form.phone" placeholder="请输入联系电话" />
          </el-form-item>
          <el-form-item label="角色">
            <el-tag size="large">{{ roleLabel(profile.role) }}</el-tag>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" class="profile-submit" :loading="saving" @click="saveProfile">保存</el-button>
          </el-form-item>
        </el-form>
      </el-card>

      <!-- 修改密码卡片 -->
      <el-card class="pwd-card rv-rise" style="--i: 1">
        <template #header><span style="font-size:18px;font-weight:700">修改密码</span></template>
        <el-form :model="pwd" :rules="pwdRules" ref="pwdRef" label-width="100px" size="large">
          <el-form-item label="旧密码" prop="oldPassword">
            <el-input v-model="pwd.oldPassword" type="password" show-password placeholder="请输入旧密码" />
          </el-form-item>
          <el-form-item label="新密码" prop="newPassword">
            <el-input v-model="pwd.newPassword" type="password" show-password placeholder="6-20位新密码" />
          </el-form-item>
          <el-form-item label="确认密码" prop="confirmPassword">
            <el-input v-model="pwd.confirmPassword" type="password" show-password placeholder="请再次输入新密码" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" class="profile-submit" :loading="changing" @click="changePwd">修改密码</el-button>
          </el-form-item>
        </el-form>
      </el-card>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, Loading } from '@element-plus/icons-vue'
import api from '@/api/request'

const router = useRouter()
const loading = ref(true)
const saving = ref(false)
const changing = ref(false)
const pwdRef = ref(null)
const profile = ref({})
const a11yLarge = ref(localStorage.getItem('a11y_large') === '1')

function setA11yMode(val) {
  if (val) {
    document.body.classList.add('a11y-large')
    localStorage.setItem('a11y_large', '1')
  } else {
    document.body.classList.remove('a11y-large')
    localStorage.setItem('a11y_large', '0')
  }
}

const form = reactive({ nickname: '', phone: '' })
const pwd = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })

const pwdRules = {
  oldPassword: [{ required: true, message: '请输入旧密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度6-20位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    { validator: (_, v, cb) => v === pwd.newPassword ? cb() : cb(new Error('两次密码不一致')), trigger: 'blur' }
  ]
}

function roleLabel(r) {
  return { FAMILY: '家属', ESCORT: '陪诊师', ADMIN: '管理员' }[r] || r
}

function goHome() {
  const r = profile.value.role
  if (r === 'FAMILY') router.push('/family/home')
  else if (r === 'ESCORT') router.push('/escort/home')
  else router.push('/admin/dashboard')
}

async function loadProfile() {
  try {
    const res = await api.get('/user/profile')
    profile.value = res.data || {}
    form.nickname = profile.value.nickname || ''
    form.phone = profile.value.phone || ''
  } catch {
    ElMessage.error('加载个人信息失败')
  } finally {
    loading.value = false
  }
}

async function saveProfile() {
  saving.value = true
  try {
    await api.put('/user/profile', { nickname: form.nickname, phone: form.phone })
    ElMessage.success('个人信息已保存')
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

async function changePwd() {
  if (!pwdRef.value) return
  try { await pwdRef.value.validate() } catch { return }
  changing.value = true
  try {
    await api.put('/user/password', { oldPassword: pwd.oldPassword, newPassword: pwd.newPassword })
    ElMessage.success('密码修改成功，请重新登录')
    pwd.oldPassword = ''; pwd.newPassword = ''; pwd.confirmPassword = ''
    sessionStorage.clear()
    router.push('/login')
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || '修改失败')
  } finally {
    changing.value = false
  }
}

onMounted(loadProfile)
</script>

<style scoped>
.profile-page { min-height: 100vh; background: var(--bg); padding-bottom: 30px; }
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: var(--surface);
  border-bottom: 1px solid var(--line);
}
.profile-grid {
  width: min(1120px, 100%);
  margin: 0 auto;
  padding: 24px;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
  align-items: start;
}

.info-card,
.pwd-card {
  margin: 0;
}

.profile-submit {
  height: 40px;
  min-width: 120px;
  font-size: 14px;
  border-radius: 8px;
}

.readonly { font-size: 16px; color: var(--muted); }

@media (max-width: 900px) {
  .profile-grid {
    grid-template-columns: 1fr;
    padding: 16px;
  }
}
</style>