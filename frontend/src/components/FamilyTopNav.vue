<template>
  <header class="family-topnav">
    <div class="topnav-inner">
      <router-link to="/family/home" class="brand-link" aria-label="安心陪家属工作台">
        <span class="brand-mark" aria-hidden="true">
          <svg viewBox="0 0 32 32" width="20" height="20" fill="none">
            <path d="M16 27.4c-.6 0-1.2-.2-1.7-.6C8.9 22.2 5 18.7 5 14.1 5 10.6 7.7 8 11.1 8c1.9 0 3.6.9 4.9 2.4C17.3 8.9 19 8 20.9 8 24.3 8 27 10.6 27 14.1c0 4.6-3.9 8.1-9.3 12.7-.5.4-1.1.6-1.7.6z" fill="#fff" />
            <path d="M16 11.2v7M12.5 14.7h7" stroke="#2F5A85" stroke-width="2.2" stroke-linecap="round" />
          </svg>
        </span>
        <span class="brand-lockup">
          <strong>安心陪</strong>
          <em>家属工作台</em>
        </span>
      </router-link>

      <nav class="topnav-links" aria-label="家属端导航">
        <router-link
          v-for="item in navItems"
          :key="item.path"
          :to="item.path"
          class="nav-link"
          :class="{ active: isActive(item.path) }"
        >
          {{ item.label }}
        </router-link>
      </nav>

      <div class="topnav-actions">
        <button
          class="icon-btn"
          type="button"
          aria-label="消息通知"
          @click="$router.push('/family/notify')"
        >
          <el-badge :value="notificationCount" :hidden="notificationCount === 0" :max="99">
            <el-icon :size="18"><Bell /></el-icon>
          </el-badge>
        </button>
        <button
          class="icon-btn"
          type="button"
          aria-label="个人中心"
          @click="$router.push('/family/profile')"
        >
          <el-icon :size="18"><User /></el-icon>
        </button>
        <button
          class="icon-btn logout"
          type="button"
          aria-label="退出登录"
          @click="handleLogout"
        >
          <el-icon :size="18"><SwitchButton /></el-icon>
        </button>
      </div>
    </div>
  </header>
</template>

<script setup>
import { useRoute, useRouter } from 'vue-router'
import { Bell, User, SwitchButton } from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'

defineProps({
  notificationCount: { type: Number, default: 0 }
})

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const navItems = [
  { label: '工作台', path: '/family/home' },
  { label: '老人档案', path: '/family/elderly' },
  { label: '我的订单', path: '/family/orders' },
  { label: '就医档案', path: '/family/records' },
  { label: 'AI 助手', path: '/family/ai-chat' }
]

function isActive(path) {
  if (path === '/family/home') return route.path === path
  if (path === '/family/orders') {
    return route.path === path || route.path.startsWith('/family/order/')
  }
  if (path === '/family/elderly') {
    return route.path === path || route.path.startsWith('/family/elderly/')
  }
  if (path === '/family/records') {
    return route.path === path || route.path.startsWith('/family/record/')
  }
  if (path === '/family/ai-chat') {
    return route.path.startsWith('/family/ai-chat')
  }
  return route.path === path
}

async function handleLogout() {
  await userStore.logout()
  router.push('/login')
}
</script>

<style scoped>
.family-topnav {
  position: sticky;
  top: 0;
  z-index: 50;
  background: rgba(255, 255, 255, 0.96);
  backdrop-filter: blur(10px);
  border-bottom: 1px solid var(--line);
}

.topnav-inner {
  width: min(1240px, 100%);
  margin: 0 auto;
  padding: 0 24px;
  height: 64px;
  display: flex;
  align-items: center;
  gap: 32px;
}

.brand-link {
  display: flex;
  align-items: center;
  gap: 10px;
  text-decoration: none;
  flex-shrink: 0;
}

.brand-mark {
  width: 34px;
  height: 34px;
  display: grid;
  place-items: center;
  background: linear-gradient(145deg, var(--teal-deep), var(--teal));
  border-radius: 10px;
  box-shadow: 0 4px 10px -6px rgba(47, 90, 133, 0.55);
}

.brand-lockup {
  display: flex;
  flex-direction: column;
  gap: 1px;
}

.brand-lockup strong {
  font-size: 15px;
  font-weight: 700;
  line-height: 1.2;
  letter-spacing: 0.04em;
  color: var(--ink);
}

.brand-lockup em {
  font-style: normal;
  font-size: 10px;
  font-weight: 500;
  letter-spacing: 0.12em;
  color: var(--muted-2);
}

.topnav-links {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 4px;
}

.nav-link {
  padding: 8px 14px;
  border-radius: 6px;
  font-size: 14px;
  font-weight: 500;
  color: var(--muted);
  text-decoration: none;
  transition: background-color 0.16s ease, color 0.16s ease;
}

.nav-link:hover {
  background: var(--surface-3);
  color: var(--ink-2);
}

.nav-link.active {
  background: var(--teal-soft);
  color: var(--teal-deep);
  font-weight: 600;
}

.topnav-actions {
  display: flex;
  align-items: center;
  gap: 4px;
  flex-shrink: 0;
}

.icon-btn {
  width: 36px;
  height: 36px;
  display: grid;
  place-items: center;
  background: transparent;
  border: 1px solid transparent;
  border-radius: 8px;
  color: var(--ink-3);
  cursor: pointer;
  transition: background-color 0.16s ease, border-color 0.16s ease,
    color 0.16s ease;
}

.icon-btn:hover {
  background: var(--surface-3);
  border-color: var(--line);
}

.icon-btn.logout:hover {
  background: var(--danger-soft);
  color: var(--danger);
}

@media (max-width: 1000px) {
  .topnav-inner {
    padding: 0 16px;
    height: 56px;
    gap: 16px;
  }

  .topnav-links {
    display: none;
  }

  .brand-lockup em {
    display: none;
  }

  .brand-mark {
    width: 32px;
    height: 32px;
    border-radius: 9px;
  }

  .icon-btn {
    width: 34px;
    height: 34px;
  }
}
</style>
