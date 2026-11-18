<template>
  <div class="auth-page">
    <div class="auth-shell">
      <!-- 品牌区 -->
      <aside class="brand-panel rv-rise" style="--i: 0">
        <div class="brand-glow" aria-hidden="true"></div>

        <div class="brand-top">
          <span class="brand-mark" aria-hidden="true">
            <svg viewBox="0 0 32 32" width="24" height="24" fill="none">
              <path d="M16 27.4c-.6 0-1.2-.2-1.7-.6C8.9 22.2 5 18.7 5 14.1 5 10.6 7.7 8 11.1 8c1.9 0 3.6.9 4.9 2.4C17.3 8.9 19 8 20.9 8 24.3 8 27 10.6 27 14.1c0 4.6-3.9 8.1-9.3 12.7-.5.4-1.1.6-1.7.6z" fill="#3A6EA5" />
              <path d="M16 11.2v7M12.5 14.7h7" stroke="#fff" stroke-width="2.2" stroke-linecap="round" />
            </svg>
          </span>
          <span class="brand-name">安心陪 · 陪诊助医</span>
        </div>

        <div class="brand-copy">
          <span class="brand-kicker">注册指引</span>
          <h1>三步开始<br />安心陪诊</h1>
          <p>创建账号后即可为家人预约陪诊，或申请成为实名认证陪诊师。</p>
        </div>

        <ol class="brand-steps">
          <li>
            <span class="step-index">01</span>
            <div>
              <strong>创建账号</strong>
              <span>使用手机号完成注册</span>
            </div>
          </li>
          <li>
            <span class="step-index">02</span>
            <div>
              <strong>完善资料</strong>
              <span>老人档案或陪诊资质</span>
            </div>
          </li>
          <li>
            <span class="step-index">03</span>
            <div>
              <strong>开始使用</strong>
              <span>预约陪诊或查看订单</span>
            </div>
          </li>
        </ol>

        <p class="brand-note">
          您的手机号与身份信息仅用于账号安全与服务履约，平台将按照隐私政策严格保护。
        </p>
      </aside>

      <!-- 表单区 -->
      <section class="auth-card rv-rise" style="--i: 1">
        <div class="auth-head">
          <span class="auth-kicker">新用户注册</span>
          <h2>创建账号</h2>
          <p>填写以下信息，加入安心陪</p>
        </div>

        <el-form
          ref="formRef"
          :model="registerForm"
          :rules="rules"
          class="auth-form"
          size="large"
          label-position="top"
          @submit.prevent="handleRegister"
        >
          <el-form-item prop="phone" label="手机号">
            <el-input
              v-model="registerForm.phone"
              placeholder="请输入手机号"
              :prefix-icon="Phone"
              clearable
            />
          </el-form-item>

          <el-form-item prop="nickname" label="昵称">
            <el-input
              v-model="registerForm.nickname"
              placeholder="请输入昵称"
              :prefix-icon="User"
              clearable
            />
          </el-form-item>

          <div class="field-grid">
            <el-form-item prop="password" label="密码">
              <el-input
                v-model="registerForm.password"
                type="password"
                placeholder="6-30 位密码"
                :prefix-icon="Lock"
                show-password
                clearable
              />
            </el-form-item>

            <el-form-item prop="confirmPassword" label="确认密码">
              <el-input
                v-model="registerForm.confirmPassword"
                type="password"
                placeholder="再次输入密码"
                :prefix-icon="Lock"
                show-password
                clearable
              />
            </el-form-item>
          </div>

          <el-form-item prop="role" label="注册身份">
            <el-select
              v-model="registerForm.role"
              placeholder="请选择角色"
              class="role-select"
            >
              <el-option label="家属端 — 为家中老人预约陪诊" value="FAMILY" />
              <el-option label="陪诊师端 — 提供陪诊服务（需资质审核）" value="ESCORT" />
            </el-select>
            <div v-if="registerForm.role === 'FAMILY'" class="role-hint">
              为家中老人预约陪诊服务，管理老人档案与就医记录
            </div>
            <div v-if="registerForm.role === 'ESCORT'" class="role-hint">
              提供陪诊服务，注册后需提交资质审核通过方可接单
            </div>
          </el-form-item>

          <el-form-item prop="captcha" label="验证码">
            <div class="captcha-row">
              <el-input
                v-model="registerForm.captcha"
                placeholder="请输入 4 位验证码"
                clearable
                class="captcha-input"
              />
              <div class="captcha-img" @click="refreshCaptcha" title="点击刷新验证码">
                <img v-if="captchaSvg" :src="captchaSvg" alt="验证码" />
                <span v-else class="captcha-placeholder">加载中</span>
              </div>
            </div>
          </el-form-item>

          <el-form-item class="submit-item">
            <el-button
              type="primary"
              :loading="loading"
              class="register-btn"
              @click="handleRegister"
            >
              {{ loading ? '注册中…' : '注册' }}
            </el-button>
          </el-form-item>

          <div class="auth-footer">
            <span>已有账号？</span>
            <button type="button" class="text-link" @click="$router.push('/login')">
              立即登录
            </button>
          </div>
        </el-form>
      </section>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Phone, User, Lock } from '@element-plus/icons-vue'
import request from '@/api/request'

const router = useRouter()
const formRef = ref(null)
const loading = ref(false)
const captchaSvg = ref('')
const captchaKey = ref('')

const registerForm = reactive({
  phone: '',
  nickname: '',
  password: '',
  confirmPassword: '',
  role: '',
  captcha: ''
})

const validateConfirmPassword = (_rule, value, callback) => {
  if (value === '') {
    callback(new Error('请再次输入密码'))
  } else if (value !== registerForm.password) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const rules = {
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }
  ],
  nickname: [
    { required: true, message: '请输入昵称', trigger: 'blur' },
    { min: 2, max: 20, message: '昵称长度在 2 到 20 个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 30, message: '密码长度在 6 到 30 个字符', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ],
  role: [
    { required: true, message: '请选择角色', trigger: 'change' }
  ],
  captcha: [
    { required: true, message: '请输入验证码', trigger: 'blur' }
  ]
}

const fetchCaptcha = async () => {
  try {
    const res = await request.get('/auth/captcha')
    if (res.data) {
      captchaKey.value = res.data.captchaKey || ''
      captchaSvg.value = res.data.captchaImage || ''
    }
  } catch {
    ElMessage.warning('验证码加载失败，请点击刷新')
  }
}

const refreshCaptcha = () => {
  captchaSvg.value = ''
  fetchCaptcha()
}

const handleRegister = async () => {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
  } catch {
    return
  }

  loading.value = true
  try {
    const res = await request.post('/auth/register', {
      username: registerForm.phone,
      nickname: registerForm.nickname,
      password: registerForm.password,
      role: registerForm.role,
      captcha: registerForm.captcha,
      captchaKey: captchaKey.value
    })

    ElMessage.success(res.data?.message || '注册成功，请登录')
    router.push('/login')
  } catch (err) {
    const msg = err?.response?.data?.message || err?.message || '注册失败，请重试'
    ElMessage.error(msg)
    refreshCaptcha()
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchCaptcha()
})
</script>

<style scoped>
.auth-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: clamp(16px, 3.6vw, 48px);
}

.auth-shell {
  width: min(1120px, 100%);
  display: grid;
  grid-template-columns: 0.9fr 1.1fr;
  gap: clamp(18px, 3vw, 40px);
  align-items: stretch;
}

/* ---------- 品牌区 ---------- */
.brand-panel {
  position: relative;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  gap: 24px;
  padding: clamp(28px, 3.4vw, 44px);
  overflow: hidden;
  color: #fff;
  background: var(--hero-gradient);
  border-radius: 24px;
  box-shadow: 0 30px 70px -38px rgba(47, 128, 237, 0.55);
}

.brand-panel::before {
  content: "";
  position: absolute;
  inset: 0;
  background-image: linear-gradient(rgba(255, 255, 255, 0.055) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 255, 255, 0.055) 1px, transparent 1px);
  background-size: 36px 36px;
  mask-image: radial-gradient(120% 90% at 20% 0%, #000 30%, transparent 80%);
  pointer-events: none;
}

.brand-glow {
  position: absolute;
  top: -140px;
  right: -120px;
  width: 360px;
  height: 360px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(216, 232, 255, 0.3), transparent 68%);
  pointer-events: none;
}

.brand-top {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  gap: 11px;
}

.brand-mark {
  width: 42px;
  height: 42px;
  display: grid;
  place-items: center;
  background: rgba(255, 255, 255, 0.94);
  border-radius: 12px;
  box-shadow: 0 8px 18px -8px rgba(0, 0, 0, 0.4);
}

.brand-name {
  font-size: 14.5px;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.92);
}

.brand-copy {
  position: relative;
  z-index: 1;
  max-width: 28ch;
}

.brand-kicker {
  display: inline-block;
  padding: 5px 11px;
  border: 1px solid rgba(255, 255, 255, 0.28);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.1);
  font-size: 11.5px;
  font-weight: 600;
  letter-spacing: 0.08em;
  color: rgba(255, 255, 255, 0.92);
}

.brand-copy h1 {
  margin: 18px 0 12px;
  font-size: clamp(26px, 3.2vw, 38px);
  font-weight: 700;
  line-height: 1.3;
  letter-spacing: -0.02em;
}

.brand-copy p {
  margin: 0;
  font-size: 14px;
  line-height: 1.8;
  color: rgba(255, 255, 255, 0.78);
}

.brand-steps {
  position: relative;
  z-index: 1;
  display: grid;
  gap: 14px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.brand-steps li {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}

.step-index {
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.06em;
  color: var(--teal-light);
  padding-top: 2px;
}

.brand-steps strong {
  display: block;
  font-size: 14px;
  font-weight: 600;
  color: #fff;
}

.brand-steps div span {
  display: block;
  margin-top: 2px;
  font-size: 12.5px;
  color: rgba(255, 255, 255, 0.66);
}

.brand-note {
  position: relative;
  z-index: 1;
  margin: 0;
  padding-top: 16px;
  border-top: 1px solid rgba(255, 255, 255, 0.14);
  font-size: 11.5px;
  line-height: 1.7;
  color: rgba(255, 255, 255, 0.55);
}

/* ---------- 表单区 ---------- */
.auth-card {
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: clamp(26px, 3.2vw, 42px);
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: 24px;
  box-shadow: var(--shadow-card);
}

.auth-kicker {
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
  color: var(--teal-deep);
}

.auth-head h2 {
  margin: 8px 0 6px;
  font-size: 28px;
  font-weight: 700;
  letter-spacing: -0.02em;
  color: var(--ink);
}

.auth-head p {
  margin: 0 0 22px;
  font-size: 14px;
  color: var(--muted);
}

.field-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.auth-form :deep(.el-form-item) {
  margin-bottom: 15px;
}

.auth-form :deep(.el-form-item__label) {
  padding-bottom: 5px !important;
  font-size: 13px;
  font-weight: 600;
  color: var(--ink-2);
}

.auth-form :deep(.el-input__inner),
.role-select :deep(.el-input__inner) {
  height: 44px;
  font-size: 15px;
}

.role-select {
  width: 100%;
}

.role-hint {
  margin-top: 8px;
  padding: 9px 12px;
  background: var(--surface-2);
  border: 1px solid var(--line);
  border-radius: 8px;
  font-size: 12.5px;
  line-height: 1.6;
  color: var(--muted);
}

.captcha-row {
  display: flex;
  gap: 10px;
  width: 100%;
}

.captcha-input {
  flex: 1;
}

.captcha-img {
  width: 142px;
  height: 46px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  background: var(--surface-2);
  border: 1px solid var(--line-strong);
  border-radius: 10px;
  cursor: pointer;
  transition: border-color 0.16s ease;
}

.captcha-img:hover {
  border-color: var(--teal-deep);
}

.captcha-img img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.captcha-placeholder {
  font-size: 12.5px;
  color: var(--muted-2);
}

.submit-item {
  margin-top: 20px;
  margin-bottom: 14px !important;
}

.register-btn {
  width: 100%;
  height: 48px;
  font-size: 15px;
  font-weight: 600;
  border-radius: 10px;
}

.auth-footer {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  font-size: 13.5px;
  color: var(--muted);
}

.text-link {
  appearance: none;
  padding: 0;
  background: none;
  border: none;
  font-family: inherit;
  font-size: 13.5px;
  font-weight: 600;
  color: var(--teal-deep);
  cursor: pointer;
}

.text-link:hover {
  text-decoration: underline;
  text-underline-offset: 3px;
}

@media (max-width: 920px) {
  .auth-shell {
    grid-template-columns: 1fr;
    max-width: 520px;
  }

  .brand-panel {
    gap: 16px;
    padding: 24px;
  }

  .brand-copy h1 {
    font-size: 24px;
  }

  .brand-copy p,
  .brand-steps,
  .brand-note {
    display: none;
  }

  .auth-card {
    border-radius: 20px;
  }
}

@media (max-width: 560px) {
  .field-grid {
    grid-template-columns: 1fr;
    gap: 0;
  }

  .auth-card {
    padding: 24px 18px;
  }

  .captcha-img {
    width: 112px;
  }
}
</style>
