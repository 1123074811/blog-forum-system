<template>
  <div class="min-h-screen flex items-center justify-center p-4">
    <div class="glass rounded-2xl p-8 w-full max-w-md">
      <h2 class="text-2xl font-bold text-center mb-6 text-gray-800 dark:text-white">注册</h2>
      <el-form :model="form" @submit.prevent="handleRegister">
        <el-form-item>
          <el-input v-model="form.username" placeholder="用户名" :prefix-icon="User" size="large" />
        </el-form-item>
        <el-form-item>
          <el-input v-model="form.email" placeholder="邮箱" :prefix-icon="Message" size="large" />
        </el-form-item>
        <el-form-item>
          <el-input v-model="form.password" type="password" placeholder="密码" :prefix-icon="Lock" size="large" show-password />
        </el-form-item>
        <el-form-item>
          <el-input v-model="form.confirmPassword" type="password" placeholder="确认密码" :prefix-icon="Lock" size="large" show-password />
        </el-form-item>
        <el-form-item>
          <div class="flex gap-2 w-full">
            <el-input v-model="form.captchaCode" placeholder="验证码" size="large" class="flex-1" />
            <img :src="captchaUrl" @click="refreshCaptcha" class="h-10 cursor-pointer rounded" alt="验证码" />
          </div>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" native-type="submit" :loading="loading" class="w-full" size="large">注册</el-button>
        </el-form-item>
      </el-form>
      <div class="text-center mt-4">
        <span class="text-gray-600 dark:text-gray-300">已有账号？</span>
        <router-link to="/login" class="text-primary-500 hover:text-primary-600 ml-1">立即登录</router-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { register, getCaptcha } from '@/api/blog'
import { User, Message, Lock } from '@element-plus/icons-vue'

const router = useRouter()
const loading = ref(false)
const captchaUrl = ref('')
const form = ref({ username: '', email: '', password: '', confirmPassword: '', captchaId: '', captchaCode: '' })
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
  } catch (e) { console.error(e) }
}
onMounted(refreshCaptcha)

onUnmounted(() => {
  if (captchaRefreshTimer) clearTimeout(captchaRefreshTimer)
})

const handleRegister = async () => {
  if (!form.value.username || !form.value.email || !form.value.password) {
    ElMessage.warning('请填写完整信息')
    return
  }
  if (form.value.password !== form.value.confirmPassword) {
    ElMessage.warning('两次密码不一致')
    return
  }
  if (!form.value.captchaCode) {
    ElMessage.warning('请输入验证码')
    return
  }
  loading.value = true
  try {
    const res = await register(form.value)
    if (res.success) {
      ElMessage.success('注册成功，请登录')
      router.push('/login')
    } else {
      ElMessage.error(res.message)
      refreshCaptcha()
    }
  } catch (e) {
    ElMessage.error('注册失败')
    refreshCaptcha()
  } finally {
    loading.value = false
  }
}
</script>
