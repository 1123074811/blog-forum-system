<template>
  <div class="min-h-screen flex items-center justify-center">
    <div class="text-center">
      <el-icon class="is-loading text-4xl text-primary-500"><Loading /></el-icon>
      <p class="mt-4 text-gray-600 dark:text-gray-300">登录中...</p>
    </div>
  </div>
</template>

<script setup>
import { onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { getUser } from '@/api/blog'
import { Loading } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

onMounted(async () => {
  const { token, refreshToken, userId } = route.query
  if (token && refreshToken && userId) {
    const res = await getUser(userId)
    if (res.success) {
      userStore.setUser(res.data.user, token, refreshToken)
      ElMessage.success('登录成功')
      router.replace('/')
    } else {
      ElMessage.error('获取用户信息失败')
      router.replace('/login')
    }
  } else {
    ElMessage.error('登录失败')
    router.replace('/login')
  }
})
</script>
