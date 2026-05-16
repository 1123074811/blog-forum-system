import { createApp } from 'vue'
import { createPinia } from 'pinia'
import 'element-plus/dist/index.css'
import VueKonva from 'vue-konva'
import App from './App.vue'
import router from './router'
import { useUserStore } from '@/stores/user'
import config from '@/config'

const styleReady = config.layoutStyle === 'modern'
  ? import('./assets/main-modern.css')
  : import('./assets/main-classic.css')

const mountApp = () => {
  document.documentElement.dataset.layoutStyle = config.layoutStyle
  document.body.classList.toggle('layout-modern', config.layoutStyle === 'modern')
  document.body.classList.toggle('layout-classic', config.layoutStyle === 'classic')

  const app = createApp(App)
  const pinia = createPinia()

  app.use(pinia)
  app.use(router)
  app.use(VueKonva)

  const userStore = useUserStore(pinia)
  userStore.initSession()

  app.mount('#app')
}

styleReady.then(mountApp)
