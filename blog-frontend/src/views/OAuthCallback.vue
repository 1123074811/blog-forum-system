<template>
  <div class="min-h-screen flex items-center justify-center">
    <div class="text-center">
      <el-icon v-if="!error" class="is-loading text-4xl text-primary-500"><Loading /></el-icon>
      <el-icon v-else class="text-4xl text-red-500"><CircleClose /></el-icon>
      <p class="mt-4 text-gray-600 dark:text-gray-300">{{ message }}</p>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { getUser } from '@/api/blog'
import { Loading, CircleClose } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const error = ref(false)
const message = ref('登录中...')

const errorMessages = {
  'gitee_failed': 'Gitee 登录失败，请重试',
  'gitee_token_failed': '获取 Gitee Token 失败',
  'gitee_no_token': 'Gitee 未返回 Token',
  'gitee_user_failed': '获取 Gitee 用户信息失败',
  'gitee_api_error': 'Gitee API 调用失败',
  'gitee_missing_id': 'Gitee 响应缺少用户ID',
  'gitee_missing_login': 'Gitee 响应缺少用户名',
  'save_user_failed': '保存用户信息失败',
  'query_user_failed': '查询用户信息失败',
  'json_parse_error': '数据解析失败',
  'github_failed': 'GitHub 登录失败，请重试'
}

onMounted(async () => {
  const { token, refreshToken, userId, error: errorCode, message: errorMsg } = route.query
  
  // 处理错误
  if (errorCode) {
    error.value = true
    const errorMessage = errorMessages[errorCode] || '登录失败'
    message.value = errorMsg ? `${errorMessage}: ${errorMsg}` : errorMessage
    ElMessage.error(message.value)
    
    // 3秒后跳转到登录页
    setTimeout(() => {
      router.replace('/login')
    }, 3000)
    return
  }
  
  // 处理成功
  if (token && refreshToken && userId) {
    try {
      message.value = '正在获取用户信息...'
      const res = await getUser(userId)
      
      if (res.success) {
        message.value = '登录成功，正在跳转...'
        userStore.setUser(res.data, token, refreshToken)
        ElMessage.success('登录成功')
        
        // 返回之前的页面
        const redirect = sessionStorage.getItem('oauth_redirect') || '/'
        sessionStorage.removeItem('oauth_redirect')
        
        setTimeout(() => {
          router.replace(redirect)
        }, 500)
      } else {
        error.value = true
        message.value = '获取用户信息失败'
        ElMessage.error(message.value)
        
        setTimeout(() => {
          router.replace('/login')
        }, 3000)
      }
    } catch (err) {
      console.error('登录处理失败:', err)
      error.value = true
      message.value = '登录处理失败'
      ElMessage.error(message.value)
      
      setTimeout(() => {
        router.replace('/login')
      }, 3000)
    }
  } else {
    error.value = true
    message.value = '登录参数缺失'
    ElMessage.error(message.value)
    
    setTimeout(() => {
      router.replace('/login')
    }, 3000)
  }
})
</script>
