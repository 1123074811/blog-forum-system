<template>
  <div class="min-h-screen flex items-center justify-center p-4">
    <div class="glass rounded-2xl p-8 w-full max-w-md">
      <h2 class="text-2xl font-bold text-center mb-6 text-gray-800 dark:text-white">登录</h2>
      <el-form :model="form" @submit.prevent="handleLogin">
        <el-form-item>
          <el-input v-model="form.username" placeholder="用户名" prefix-icon="User" size="large" />
        </el-form-item>
        <el-form-item>
          <el-input v-model="form.password" type="password" placeholder="密码" prefix-icon="Lock" size="large" show-password />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" native-type="submit" :loading="loading" class="w-full" size="large">登录</el-button>
        </el-form-item>
      </el-form>
      <div class="text-center mt-4">
        <span class="text-gray-600 dark:text-gray-300">还没有账号？</span>
        <router-link to="/register" class="text-primary-500 hover:text-primary-600 ml-1">立即注册</router-link>
      </div>
      <div class="mt-6">
        <div class="relative">
          <div class="absolute inset-0 flex items-center"><div class="w-full border-t border-gray-300 dark:border-gray-600"></div></div>
          <div class="relative flex justify-center text-sm"><span class="px-2 bg-white dark:bg-gray-800 text-gray-500">或使用第三方登录</span></div>
        </div>
        <div class="mt-4 flex gap-3">
          <el-button class="flex-1" @click="loginWithGithub">
            <svg class="w-5 h-5 mr-2" viewBox="0 0 24 24" fill="currentColor"><path d="M12 0C5.37 0 0 5.37 0 12c0 5.31 3.435 9.795 8.205 11.385.6.105.825-.255.825-.57 0-.285-.015-1.23-.015-2.235-3.015.555-3.795-.735-4.035-1.41-.135-.345-.72-1.41-1.23-1.695-.42-.225-1.02-.78-.015-.795.945-.015 1.62.87 1.845 1.23 1.08 1.815 2.805 1.305 3.495.99.105-.78.42-1.305.765-1.605-2.67-.3-5.46-1.335-5.46-5.925 0-1.305.465-2.385 1.23-3.225-.12-.3-.54-1.53.12-3.18 0 0 1.005-.315 3.3 1.23.96-.27 1.98-.405 3-.405s2.04.135 3 .405c2.295-1.56 3.3-1.23 3.3-1.23.66 1.65.24 2.88.12 3.18.765.84 1.23 1.905 1.23 3.225 0 4.605-2.805 5.625-5.475 5.925.435.375.81 1.095.81 2.22 0 1.605-.015 2.895-.015 3.3 0 .315.225.69.825.57A12.02 12.02 0 0024 12c0-6.63-5.37-12-12-12z"/></svg>
            GitHub
          </el-button>
          <el-button class="flex-1" @click="loginWithGitee">
            <svg class="w-5 h-5 mr-2" viewBox="0 0 24 24" fill="#C71D23"><path d="M12 0C5.373 0 0 5.373 0 12s5.373 12 12 12 12-5.373 12-12S18.627 0 12 0zm6.24 14.4H13.2v5.04c0 .66-.54 1.2-1.2 1.2s-1.2-.54-1.2-1.2V14.4H5.76c-.66 0-1.2-.54-1.2-1.2s.54-1.2 1.2-1.2h5.04V6.96c0-.66.54-1.2 1.2-1.2s1.2.54 1.2 1.2V12h5.04c.66 0 1.2.54 1.2 1.2s-.54 1.2-1.2 1.2z"/></svg>
            Gitee
          </el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { login } from '@/api/blog'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)
const form = ref({ username: '', password: '' })

const handleLogin = async () => {
  if (!form.value.username || !form.value.password) {
    ElMessage.warning('请填写用户名和密码')
    return
  }
  loading.value = true
  try {
    const res = await login(form.value)
    if (res.success) {
      userStore.setUser(res.data.user, res.data.token, res.data.refreshToken)
      ElMessage.success('登录成功')
      router.push(res.data.user.role === 'admin' ? '/admin' : '/')
    } else {
      ElMessage.error(res.message)
    }
  } catch (e) {
    ElMessage.error('登录失败')
  } finally {
    loading.value = false
  }
}

const loginWithGithub = () => {
  window.location.href = 'http://localhost:8080/api/auth/github'
}

const loginWithGitee = () => {
  window.location.href = 'http://localhost:8080/api/auth/gitee'
}
</script>
