import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import './styles/theme.css'
import zhCn from 'element-plus/dist/locale/zh-cn.mjs'
import router from './router'
import App from './App.vue'

const app = createApp(App)
app.use(createPinia())
app.use(router)
app.use(ElementPlus, { locale: zhCn })
app.mount('#app')

// 适老化显示模式：仅家属端路由生效，默认关闭；开关在 /family/profile
function applyA11yMode(path) {
  const enabled = localStorage.getItem('a11y_large') === '1'
  document.body.classList.toggle('a11y-large', enabled && String(path).startsWith('/family'))
}
router.afterEach(to => applyA11yMode(to.path))
applyA11yMode(router.currentRoute.value.path)