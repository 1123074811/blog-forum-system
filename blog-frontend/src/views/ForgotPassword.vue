<template>
  <div class="min-h-screen flex items-center justify-center p-4">
    <div class="glass rounded-2xl p-8 w-full max-w-md">
      <h2 class="text-2xl font-bold text-center mb-6 text-gray-800 dark:text-white">忘记密码</h2>

      <!-- 步骤1: 输入邮箱 -->
      <template v-if="step === 1">
        <el-form @submit.prevent="sendCode">
          <el-form-item>
            <el-input v-model="email" placeholder="请输入注册邮箱" prefix-icon="Message" size="large" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" native-type="submit" :loading="loading" class="w-full" size="large">发送验证码</el-button>
          </el-form-item>
        </el-form>
      </template>

      <!-- 步骤2: 输入验证码和新密码 -->
      <template v-if="step === 2">
        <el-form @submit.prevent="handleReset">
          <el-form-item>
            <el-input v-model="code" placeholder="请输入6位验证码" prefix-icon="Key" size="large" maxlength="6" />
          </el-form-item>
          <el-form-item>
            <el-input v-model="password" type="password" placeholder="新密码" prefix-icon="Lock" size="large" show-password />
          </el-form-item>
          <el-form-item>
            <el-input v-model="confirmPassword" type="password" placeholder="确认新密码" prefix-icon="Lock" size="large" show-password />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" native-type="submit" :loading="loading" class="w-full" size="large">重置密码</el-button>
          </el-form-item>
        </el-form>
      </template>

      <div class="text-center mt-4">
        <router-link to="/login" class="text-primary-500 hover:text-primary-600">返回登录</router-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { forgotPassword, resetPassword } from '@/api/blog'

const router = useRouter()
const loading = ref(false)
const step = ref(1)
const email = ref('')
const code = ref('')
const password = ref('')
const confirmPassword = ref('')

const sendCode = async () => {
  if (!email.value) {
    ElMessage.warning('请输入邮箱')
    return
  }
  if (!/^[\w.-]+@[\w.-]+\.[a-zA-Z]{2,}$/.test(email.value)) {
    ElMessage.warning('邮箱格式不正确')
    return
  }
  loading.value = true
  try {
    const res = await forgotPassword(email.value)
    if (res.success) {
      ElMessage.success('验证码已发送')
      step.value = 2
    } else {
      ElMessage.error(res.message)
    }
  } catch (e) {
    ElMessage.error('发送失败')
  } finally {
    loading.value = false
  }
}

const handleReset = async () => {
  if (!code.value || !password.value) {
    ElMessage.warning('请填写完整信息')
    return
  }
  if (password.value !== confirmPassword.value) {
    ElMessage.warning('两次密码不一致')
    return
  }
  loading.value = true
  try {
    const res = await resetPassword({ email: email.value, code: code.value, password: password.value })
    if (res.success) {
      ElMessage.success('密码重置成功')
      router.push('/login')
    } else {
      ElMessage.error(res.message)
    }
  } catch (e) {
    ElMessage.error('重置失败')
  } finally {
    loading.value = false
  }
}
</script>
