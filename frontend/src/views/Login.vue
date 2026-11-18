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
          <div class="brand-lockup">
            <span class="brand-name">安心陪</span>
            <span class="brand-subtitle">陪诊助医服务</span>
          </div>
        </div>

        <div class="brand-copy">
          <span class="brand-kicker">AI 赋能 · 全程陪诊</span>
          <h1>让专业陪诊<br />成为家人就医的底气</h1>
          <p>从预约、陪同到就医档案，为每一个家庭提供可追溯、可信赖的陪诊服务。</p>
        </div>

        <div class="pulse-line" aria-hidden="true">
          <svg viewBox="0 0 420 120" fill="none" preserveAspectRatio="none">
            <path
              class="pulse-path"
              d="M0 62h78l14-1 10-24 12 60 13-48 11 18 9-5h58l12-1 10-22 12 54 13-44 10 13h158"
              stroke="var(--teal-light)"
              stroke-width="3"
              stroke-linecap="round"
              stroke-linejoin="round"
            />
          </svg>
          <span class="pulse-dot" aria-hidden="true"></span>
        </div>

        <div class="brand-meta">
          <div class="brand-stat">
            <strong>1,200+</strong>
            <span>实名认证陪诊师</span>
          </div>
          <span class="brand-stat-divider" aria-hidden="true"></span>
          <div class="brand-stat">
            <strong>98%</strong>
            <span>家属服务满意度</span>
          </div>
        </div>

        <ul class="brand-points">
          <li>实名资质审核</li>
          <li>全程服务留痕</li>
          <li>就医隐私保护</li>
        </ul>
      </aside>

      <!-- 表单区 -->
      <section class="auth-card rv-rise" style="--i: 1">
        <div class="auth-head">
          <span class="auth-kicker">欢迎回来</span>
          <h2>登录账号</h2>
          <p>请选择您的身份，进入对应工作台</p>
        </div>

        <div class="role-tabs" role="tablist" aria-label="选择登录身份">
          <button
            v-for="role in roles"
            :key="role.value"
            type="button"
            role="tab"
            :aria-selected="loginForm.role === role.value"
            :class="['role-tab', { active: loginForm.role === role.value }]"
            @click="loginForm.role = role.value"
          >
            <svg v-if="role.value === 'FAMILY'" viewBox="0 0 24 24" width="16" height="16" fill="none" aria-hidden="true">
              <path d="M4 11l8-6.5 8 6.5v8a1.5 1.5 0 0 1-1.5 1.5H15v-5.5H9V20H5.5A1.5 1.5 0 0 1 4 18.5V11z" stroke="currentColor" stroke-width="1.9" stroke-linejoin="round" />
            </svg>
            <svg v-else-if="role.value === 'ESCORT'" viewBox="0 0 24 24" width="16" height="16" fill="none" aria-hidden="true">
              <path d="M12 20s-7-4.4-7-9.2C5 8 7 6 9.3 6c1.2 0 2.1.5 2.7 1.4C12.6 6.5 13.5 6 14.7 6 17 6 19 8 19 10.8 19 15.6 12 20 12 20z" stroke="currentColor" stroke-width="1.9" stroke-linejoin="round" />
            </svg>
            <svg v-else viewBox="0 0 24 24" width="16" height="16" fill="none" aria-hidden="true">
              <path d="M12 3l7 3v6c0 4.4-3 7.6-7 9-4-1.4-7-4.6-7-9V6l7-3z" stroke="currentColor" stroke-width="1.9" stroke-linejoin="round" />
              <path d="M9 12l2 2 4-4" stroke="currentColor" stroke-width="1.9" stroke-linecap="round" stroke-linejoin="round" />
            </svg>
            <span>{{ role.label }}</span>
          </button>
        </div>

        <el-form
          ref="formRef"
          :model="loginForm"
          :rules="rules"
          class="auth-form"
          size="large"
          label-position="top"
          @submit.prevent="handleLogin"
        >
          <el-form-item prop="username" label="手机号 / 用户名">
            <el-input
              v-model="loginForm.username"
              placeholder="请输入手机号或用户名"
              :prefix-icon="User"
              clearable
            />
          </el-form-item>

          <el-form-item prop="password" label="密码">
            <el-input
              v-model="loginForm.password"
              type="password"
              placeholder="请输入密码"
              :prefix-icon="Lock"
              show-password
              clearable
            />
          </el-form-item>

          <el-form-item prop="captcha" label="验证码">
            <div class="captcha-row">
              <el-input
                v-model="loginForm.captcha"
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
              class="login-btn"
              @click="handleLogin"
            >
              {{ loading ? '登录中…' : '登录' }}
            </el-button>
          </el-form-item>

          <div class="auth-footer">
            <span>还没有账号？</span>
            <button type="button" class="text-link" @click="$router.push('/register')">
              立即注册
            </button>
          </div>
        </el-form>

        <div class="auth-trust" aria-label="安全提示">
          <svg viewBox="0 0 24 24" width="13" height="13" fill="none" aria-hidden="true">
            <rect x="5" y="10" width="14" height="10" rx="3" stroke="currentColor" stroke-width="1.8" />
            <path d="M8 10V8a4 4 0 0 1 8 0v2" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" />
          </svg>
          <span>数据加密传输</span>
          <i class="trust-dot" aria-hidden="true"></i>
          <span>隐私安全保护</span>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'
import request from '@/api/request'

const router = useRouter()
const userStore = useUserStore()
const formRef = ref(null)
const loading = ref(false)
const captchaSvg = ref('')
const captchaKey = ref('')

const roles = [
  { label: '家属端', value: 'FAMILY' },
  { label: '陪诊师端', value: 'ESCORT' },
  { label: '管理端', value: 'ADMIN' }
]

const loginForm = reactive({
  username: '',
  password: '',
  captcha: '',
  role: 'FAMILY'
})

const rules = {
  username: [
    { required: true, message: '请输入手机号', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 30, message: '密码长度在6到30个字符', trigger: 'blur' }
  ],
  captcha: [
    { required: true, message: '请输入验证码', trigger: 'blur' }
  ]
}

const roleRedirectMap = {
  FAMILY: '/family/home',
  ESCORT: '/escort/home',
  ADMIN: '/admin/dashboard'
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

const handleLogin = async () => {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
  } catch {
    return
  }

  // 清理可能残留的旧身份，避免本次登录失败时沿用上次角色跳转
  sessionStorage.removeItem('token')
  sessionStorage.removeItem('user')

  loading.value = true
  try {
    const res = await request.post('/auth/login', {
      username: loginForm.username,
      password: loginForm.password,
      captcha: loginForm.captcha,
      captchaKey: captchaKey.value,
      role: loginForm.role
    })

    const { token, userId, username, nickname, role, avatar } = res.data

    if (token) {
      userStore.setToken(token)
      const user = { id: userId, username, nickname, role, avatar }
      userStore.setUser(user)
      ElMessage.success('登录成功')
      const redirectPath = roleRedirectMap[role] || roleRedirectMap[loginForm.role] || '/family/home'
      router.push(redirectPath)
    } else {
      ElMessage.error('登录失败，未获取到令牌')
    }
  } catch (err) {
    const msg = err?.response?.data?.message || err?.message || '登录失败，请重试'
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
  grid-template-columns: 5fr 4fr;
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
  min-height: 620px;
  padding: clamp(30px, 3.6vw, 50px);
  overflow: hidden;
  color: #fff;
  background: var(--hero-gradient);
  border-radius: var(--radius-lg);
  box-shadow: 0 24px 56px -34px rgba(16, 24, 40, 0.35);
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
  width: 380px;
  height: 380px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(216, 232, 255, 0.32), transparent 68%);
  pointer-events: none;
}

.brand-top {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  gap: 14px;
}

.brand-mark {
  width: 46px;
  height: 46px;
  display: grid;
  place-items: center;
  flex-shrink: 0;
  background: linear-gradient(145deg, #ffffff 0%, #eef5ff 100%);
  border: 1px solid rgba(255, 255, 255, 0.7);
  border-radius: 12px;
  box-shadow: 0 8px 20px -12px rgba(16, 24, 40, 0.4),
    inset 0 1px 0 rgba(255, 255, 255, 0.95);
}

.brand-lockup {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.brand-name {
  font-family: "Inter", "HarmonyOS Sans SC", "PingFang SC", "Microsoft YaHei", sans-serif;
  font-size: 17px;
  font-weight: 700;
  line-height: 1.2;
  letter-spacing: 0.06em;
  color: #fff;
}

.brand-subtitle {
  font-size: 10.5px;
  font-weight: 500;
  line-height: 1.3;
  letter-spacing: 0.16em;
  color: rgba(255, 255, 255, 0.62);
}

.brand-copy {
  position: relative;
  z-index: 1;
  max-width: 420px;
}

.brand-kicker {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  padding: 6px 12px;
  border: 1px solid rgba(255, 255, 255, 0.22);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.08);
  backdrop-filter: blur(6px);
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.14em;
  color: rgba(255, 255, 255, 0.92);
}

.brand-kicker::before {
  content: "";
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: #8fc0ff;
  box-shadow: 0 0 0 3px rgba(143, 192, 255, 0.18);
}

.brand-copy h1 {
  margin: 20px 0 14px;
  font-family: "Inter", "HarmonyOS Sans SC", "PingFang SC", "Microsoft YaHei", sans-serif;
  font-size: clamp(28px, 3.1vw, 40px);
  font-weight: 700;
  line-height: 1.26;
  letter-spacing: -0.035em;
  text-wrap: balance;
}

.brand-copy p {
  max-width: 36ch;
  margin: 0;
  font-size: 14.5px;
  line-height: 1.82;
  color: rgba(255, 255, 255, 0.76);
}

.pulse-line {
  position: relative;
  z-index: 1;
  height: 92px;
  margin: 4px 0;
  opacity: 0.9;
}

.pulse-line svg {
  width: 100%;
  height: 100%;
  display: block;
}

.pulse-path {
  stroke-dasharray: 620;
  stroke-dashoffset: 620;
  animation: rv-draw 2.2s 0.35s cubic-bezier(0.22, 1, 0.36, 1) forwards;
}

.pulse-dot {
  position: absolute;
  top: 26px;
  left: 26%;
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: var(--teal-light);
  box-shadow: 0 0 0 6px rgba(216, 232, 255, 0.16);
  animation: rv-pulse 2.4s ease-in-out infinite;
}

.brand-meta {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  gap: 22px;
}

.brand-stat {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.brand-stat strong {
  font-size: 26px;
  font-weight: 700;
  letter-spacing: -0.025em;
  line-height: 1.15;
  font-variant-numeric: tabular-nums;
}

.brand-stat span {
  font-size: 11.5px;
  font-weight: 500;
  letter-spacing: 0.03em;
  color: rgba(255, 255, 255, 0.6);
}

.brand-stat-divider {
  width: 1px;
  height: 34px;
  background: rgba(255, 255, 255, 0.18);
}

.brand-points {
  position: relative;
  z-index: 1;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.brand-points li {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 7px 12px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.16);
  font-size: 11.5px;
  font-weight: 500;
  letter-spacing: 0.02em;
  color: rgba(255, 255, 255, 0.84);
}

.brand-points li::before {
  content: "";
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: #8fc0ff;
}

/* ---------- 表单区 ---------- */
.auth-card {
  position: relative;
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: clamp(28px, 3.4vw, 46px);
  overflow: hidden;
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-card);
}

.auth-card::before {
  content: "";
  position: absolute;
  top: 0;
  left: 12%;
  right: 12%;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(58, 110, 165, 0.35), transparent);
  pointer-events: none;
}

.auth-kicker {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.12em;
  color: var(--teal-deep);
}

.auth-kicker::before {
  content: "";
  width: 16px;
  height: 1.5px;
  border-radius: 2px;
  background: var(--teal);
}

.auth-head h2 {
  margin: 10px 0 7px;
  font-size: 30px;
  font-weight: 700;
  line-height: 1.15;
  letter-spacing: -0.03em;
  color: var(--ink);
}

.auth-head p {
  margin: 0 0 26px;
  font-size: 13.5px;
  line-height: 1.6;
  color: var(--muted);
}

.role-tabs {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 4px;
  padding: 4px;
  margin-bottom: 24px;
  background: var(--surface-3);
  border-radius: 8px;
}

.role-tab {
  appearance: none;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  min-height: 40px;
  padding: 10px 6px;
  background: transparent;
  border: 1px solid transparent;
  border-radius: 6px;
  font-family: inherit;
  font-size: 13px;
  font-weight: 600;
  color: var(--muted);
  cursor: pointer;
  transition: background-color 0.16s ease, color 0.16s ease,
    box-shadow 0.16s ease, transform 0.16s ease;
}

.role-tab:hover {
  color: var(--ink-2);
  transform: translateY(-1px);
}

.role-tab.active {
  background: var(--surface);
  color: var(--teal-deep);
  border-color: var(--line);
  box-shadow: 0 2px 6px rgba(15, 23, 42, 0.07);
  transform: translateY(0);
}

.auth-form :deep(.el-form-item) {
  margin-bottom: 17px;
}

.auth-form :deep(.el-form-item__label) {
  padding-bottom: 5px !important;
  font-size: 12.5px;
  font-weight: 600;
  letter-spacing: 0.02em;
  color: var(--ink-2);
}

.auth-form :deep(.el-input__inner) {
  height: 44px;
  font-size: 14.5px;
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
  height: 44px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  background: var(--surface-2);
  border: 1px solid var(--line-strong);
  border-radius: 8px;
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
  margin-top: 22px;
  margin-bottom: 14px !important;
}

.login-btn {
  width: 100%;
  height: 44px;
  font-size: 15px;
  font-weight: 600;
  letter-spacing: 0.08em;
  border-radius: 8px;
  box-shadow: 0 8px 20px -12px rgba(47, 90, 133, 0.55);
  transition: transform 0.18s ease, box-shadow 0.18s ease;
  will-change: transform;
}

.login-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 12px 22px -12px rgba(47, 90, 133, 0.6);
}

.login-btn:active {
  transform: translateY(0);
}

.auth-footer {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  font-size: 13px;
  color: var(--muted);
}

.auth-trust {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 7px;
  margin-top: 24px;
  padding-top: 16px;
  border-top: 1px dashed var(--line);
  font-size: 11.5px;
  letter-spacing: 0.02em;
  color: var(--muted-2);
}

.auth-trust svg {
  flex-shrink: 0;
  color: var(--teal);
}

.trust-dot {
  width: 3px;
  height: 3px;
  border-radius: 50%;
  background: var(--line-strong);
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

/* 入场动效：只使用 transform / opacity，避免重排与重绘抖动 */
@keyframes login-fade-up {
  from {
    opacity: 0;
    transform: translate3d(0, 14px, 0);
  }
  to {
    opacity: 1;
    transform: translate3d(0, 0, 0);
  }
}

@keyframes brand-mark-in {
  from {
    opacity: 0;
    transform: translate3d(0, 6px, 0) scale(0.9) rotate(-4deg);
  }
  to {
    opacity: 1;
    transform: translate3d(0, 0, 0) scale(1) rotate(0deg);
  }
}

@keyframes glow-drift {
  from {
    transform: translate3d(0, 0, 0) scale(1);
  }
  to {
    transform: translate3d(-26px, 18px, 0) scale(1.06);
  }
}

.brand-top,
.brand-copy,
.pulse-line,
.brand-meta,
.brand-points {
  animation: login-fade-up 0.7s cubic-bezier(0.22, 1, 0.36, 1) both;
  will-change: transform, opacity;
  backface-visibility: hidden;
}

.brand-top { animation-delay: 0.06s; }
.brand-copy { animation-delay: 0.14s; }
.pulse-line { animation-delay: 0.22s; }
.brand-meta { animation-delay: 0.3s; }
.brand-points { animation-delay: 0.38s; }

.brand-mark {
  animation: brand-mark-in 0.8s 0.1s cubic-bezier(0.22, 1.4, 0.36, 1) both;
  will-change: transform, opacity;
}

.brand-glow {
  animation: glow-drift 16s ease-in-out infinite alternate;
  will-change: transform;
}

.auth-head,
.role-tabs,
.auth-form,
.auth-trust {
  animation: login-fade-up 0.65s cubic-bezier(0.22, 1, 0.36, 1) both;
  will-change: transform, opacity;
  backface-visibility: hidden;
}

.auth-head { animation-delay: 0.14s; }
.role-tabs { animation-delay: 0.2s; }
.auth-form { animation-delay: 0.26s; }
.auth-trust { animation-delay: 0.32s; }

@media (max-width: 920px) {
  .auth-shell {
    grid-template-columns: 1fr;
    max-width: 480px;
  }

  .brand-panel {
    min-height: 0;
    gap: 16px;
    padding: 22px 22px 20px;
  }

  .brand-mark {
    width: 42px;
    height: 42px;
    border-radius: 13px;
  }

  .brand-name {
    font-size: 16px;
  }

  .brand-subtitle {
    font-size: 10px;
    letter-spacing: 0.14em;
  }

  .brand-copy h1 {
    font-size: 25px;
  }

  .brand-copy p,
  .pulse-line,
  .brand-meta,
  .brand-points {
    display: none;
  }

  .auth-card {
    border-radius: 22px;
  }

  .auth-trust {
    margin-top: 20px;
    font-size: 11px;
  }
}

@media (max-width: 380px) {
  .auth-card {
    padding: 22px 18px;
  }

  .captcha-img {
    width: 112px;
  }
}
</style>
