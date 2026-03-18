import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'
import { useUserStore } from '@/stores/user'
import './assets/main.css'

const app = createApp(App)
const pinia = createPinia()

app.use(pinia)
app.use(router)

// VueKonva 按需加载：仅在访问 /er-diagram 时注册
let konvaRegistered = false
router.beforeEach(async (to) => {
  if (to.name === 'ERDiagram' && !konvaRegistered) {
    const VueKonva = (await import('vue-konva')).default
    app.use(VueKonva)
    konvaRegistered = true
  }
})

const userStore = useUserStore(pinia)
// 立即开始初始化（异步），路由守卫会通过 sessionReady() 等待它完成
// 不 await，让 app.mount 立即执行，避免白屏
userStore.initSession()

app.mount('#app')
