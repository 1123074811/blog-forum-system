import { createApp } from 'vue'
import { createPinia } from 'pinia'
import 'element-plus/dist/index.css'
import VueKonva from 'vue-konva'
import App from './App.vue'
import router from './router'
import { useUserStore } from '@/stores/user'
import './assets/main.css'

const app = createApp(App)
const pinia = createPinia()

app.use(pinia)
app.use(router)
app.use(VueKonva)

const userStore = useUserStore(pinia)
userStore.initSession()

app.mount('#app')
