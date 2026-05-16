<template>
  <div class="min-h-screen flex items-center justify-center p-4 relative overflow-hidden">
    <!-- 动态背景 -->
    <div class="absolute inset-0 z-0">
      <div class="orb orb-1"></div>
      <div class="orb orb-2"></div>
      <div class="orb orb-3"></div>
    </div>

    <div class="absolute top-4 left-4 z-10">
      <el-button @click="router.push('/')" :icon="ArrowLeft" text class="glass-btn">返回首页</el-button>
    </div>

    <div class="glass-card p-8 w-full max-w-md animate-fade-in-up relative z-10">
      <div class="text-center mb-8">
        <div class="w-16 h-16 mx-auto mb-4 bg-gradient-to-br from-primary-400 to-primary-600 rounded-2xl flex items-center justify-center shadow-lg pulse-glow">
          <span class="text-3xl">🚀</span>
        </div>
        <h2 class="text-3xl font-bold gradient-text mb-2">欢迎回来</h2>
        <p class="text-gray-500 dark:text-gray-400 text-sm">登录以继续你的创作之旅</p>
      </div>

      <el-form :model="form" @submit.prevent="handleLogin">
        <el-form-item>
          <el-input v-model="form.username" placeholder="账号或邮箱" :prefix-icon="User" size="large" />
        </el-form-item>
        <el-form-item>
          <el-input v-model="form.password" type="password" placeholder="密码" :prefix-icon="Lock" size="large" show-password />
        </el-form-item>
        <el-form-item>
          <div class="flex gap-2 w-full">
            <el-input v-model="form.captchaCode" placeholder="验证码" size="large" class="flex-1" />
            <img
              :src="captchaUrl"
              @click="refreshCaptcha"
              class="h-10 cursor-pointer rounded-lg hover:scale-105 transition-transform"
              alt="验证码"
            />
          </div>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" native-type="submit" :loading="loading" class="w-full" size="large">
            <span v-if="!loading">登录</span>
            <span v-else>登录中...</span>
          </el-button>
        </el-form-item>
      </el-form>

      <div class="text-center mt-4">
        <span class="text-gray-500 dark:text-gray-400">还没有账号？</span>
        <router-link to="/register" class="text-primary-500 hover:text-primary-400 ml-1 font-medium transition-colors">立即注册</router-link>
        <span class="mx-2 text-gray-400">|</span>
        <router-link to="/forgot-password" class="text-gray-500 hover:text-primary-400 transition-colors">忘记密码？</router-link>
      </div>

      <div class="mt-8">
        <div class="relative">
          <div class="absolute inset-0 flex items-center">
            <div class="w-full border-t border-gray-200/50 dark:border-gray-600/50"></div>
          </div>
          <div class="relative flex justify-center text-sm">
            <span class="px-3 glass-light rounded-full text-gray-500 dark:text-gray-400">或使用第三方登录</span>
          </div>
        </div>
        <div class="mt-6 flex gap-4">
          <button class="flex-1 glass-btn py-3 rounded-xl flex items-center justify-center gap-2 hover:scale-105 transition-all duration-300 hover:border-primary-300" @click="loginWithGithub">
            <svg class="w-5 h-5" viewBox="0 0 24 24" fill="currentColor">
              <path d="M12 0C5.37 0 0 5.37 0 12c0 5.31 3.435 9.795 8.205 11.385.6.105.825-.255.825-.57 0-.285-.015-1.23-.015-2.235-3.015.555-3.795-.735-4.035-1.41-.135-.345-.72-1.41-1.23-1.695-.42-.225-1.02-.78-.015-.795.945-.015 1.62.87 1.845 1.23 1.08 1.815 2.805 1.305 3.495.99.105-.78.42-1.305.765-1.605-2.67-.3-5.46-1.335-5.46-5.925 0-1.305.465-2.385 1.23-3.225-.12-.3-.54-1.53.12-3.18 0 0 1.005-.315 3.3 1.23.96-.27 1.98-.405 3-.405s2.04.135 3 .405c2.295-1.56 3.3-1.23 3.3-1.23.66 1.65.24 2.88.12 3.18.765.84 1.23 1.905 1.23 3.225 0 4.605-2.805 5.625-5.475 5.925.435.375.81 1.095.81 2.22 0 1.605-.015 2.895-.015 3.3 0 .315.225.69.825.57A12.02 12.02 0 0024 12c0-6.63-5.37-12-12-12z" />
            </svg>
            GitHub
          </button>
          <button class="flex-1 glass-btn py-3 rounded-xl flex items-center justify-center gap-2 hover:scale-105 transition-all duration-300 hover:border-red-300" @click="loginWithGitee">
            <svg class="w-5 h-5" viewBox="0 0 24 24" fill="#C71D23">
              <path d="M12 0C5.373 0 0 5.373 0 12s5.373 12 12 12 12-5.373 12-12S18.627 0 12 0zm6.24 14.4H13.2v5.04c0 .66-.54 1.2-1.2 1.2s-1.2-.54-1.2-1.2V14.4H5.76c-.66 0-1.2-.54-1.2-1.2s.54-1.2 1.2-1.2h5.04V6.96c0-.66.54-1.2 1.2-1.2s1.2.54 1.2 1.2V12h5.04c.66 0 1.2.54 1.2 1.2s-.54 1.2-1.2 1.2z" />
            </svg>
            Gitee
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, User, Lock } from '@element-plus/icons-vue'
import { login, getCaptcha } from '@/api/blog'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const loading = ref(false)
const captchaUrl = ref('')
const form = ref({ username: '', password: '', captchaId: '', captchaCode: '' })
let captchaRefreshTimer = null

const scheduleCaptchaRefresh = (expiresInSeconds) => {
  const seconds = Number(expiresInSeconds)
  if (!Number.isFinite(seconds) || seconds <= 0) return
  const refreshInMs = Math.max(5000, seconds * 1000 - 5000)
  if (captchaRefreshTimer) clearTimeout(captchaRefreshTimer)
  captchaRefreshTimer = setTimeout(() => {
    if (form.value.captchaCode) {
      ElMessage.warning('验证码已过期，已自动刷新')
      form.value.captchaCode = ''
    }
    refreshCaptcha()
  }, refreshInMs)
}

const refreshCaptcha = async () => {
  try {
    const res = await getCaptcha()
    if (res.data) {
      captchaUrl.value = res.data.image
      form.value.captchaId = res.data.key
      scheduleCaptchaRefresh(res.data.expiresIn)
    }
  } catch (e) {
    console.error(e)
  }
}

onMounted(() => {
  refreshCaptcha()
  if (route.query.msg) {
    ElMessage.warning(String(route.query.msg))
  }
})

onUnmounted(() => {
  if (captchaRefreshTimer) clearTimeout(captchaRefreshTimer)
})

const handleLogin = async () => {
  if (!form.value.username || !form.value.password) {
    ElMessage.warning('请填写用户名和密码')
    return
  }
  if (!form.value.captchaCode) {
    ElMessage.warning('请输入验证码')
    return
  }

  loading.value = true
  try {
    const res = await login(form.value)
    if (res.success) {
      await userStore.setUser(res.data.user, res.data.token, res.data.refreshToken)
      ElMessage.success('登录成功')
      router.push(userStore.isAdmin ? '/admin' : '/')
      return
    }
    ElMessage.error(res.message || '登录失败')
    refreshCaptcha()
  } catch (e) {
    ElMessage.error('登录失败')
    refreshCaptcha()
  } finally {
    loading.value = false
  }
}

const buildOAuthUrl = (provider) => {
  const apiBase = import.meta.env.VITE_API_BASE_URL || '/api'
  const prefix = apiBase.startsWith('http') ? apiBase : `${window.location.origin}${apiBase}`
  return `${prefix}/auth/${provider}`
}

const loginWithGithub = () => {
  window.location.href = buildOAuthUrl('github')
}

const loginWithGitee = () => {
  window.location.href = buildOAuthUrl('gitee')
}
</script>

<style scoped>
/* 动态光球背景 */
.orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  opacity: 0.4;
  animation: float-orb 20s infinite ease-in-out;
}

.orb-1 {
  width: 400px;
  height: 400px;
  background: radial-gradient(circle, rgba(14, 165, 233, 0.6), transparent);
  top: -10%;
  left: -10%;
  animation-delay: 0s;
}

.orb-2 {
  width: 350px;
  height: 350px;
  background: radial-gradient(circle, rgba(56, 189, 248, 0.5), transparent);
  bottom: -10%;
  right: -10%;
  animation-delay: -7s;
}

.orb-3 {
  width: 300px;
  height: 300px;
  background: radial-gradient(circle, rgba(125, 211, 252, 0.4), transparent);
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  animation-delay: -14s;
}

@keyframes float-orb {
  0%,
  100% {
    transform: translate(0, 0) scale(1);
  }

  33% {
    transform: translate(80px, -80px) scale(1.1);
  }

  66% {
    transform: translate(-80px, 80px) scale(0.9);
  }
}
</style>
