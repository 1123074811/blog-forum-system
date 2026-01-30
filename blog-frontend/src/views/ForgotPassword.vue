<template>
  <div class="min-h-screen flex items-center justify-center p-4 bg-gradient-to-br from-blue-50 to-indigo-100 dark:from-gray-800 dark:to-gray-900">
    <div class="glass rounded-2xl p-8 w-full max-w-md shadow-xl">
      <h2 class="text-2xl font-bold text-center mb-6 text-gray-800 dark:text-white">找回密码</h2>

      <!-- 步骤1: 输入账号和发送验证码 -->
      <template v-if="step === 1">
        <el-form @submit.prevent="validateAccount" :model="{ account: account }" label-position="top">
          <el-form-item label="账号" required>
            <el-input v-model="account" placeholder="请输入您的账号" prefix-icon="User" size="large" />
          </el-form-item>
          <el-form-item class="flex items-end gap-2">
            <el-input v-model="verificationCode" placeholder="请输入验证码" prefix-icon="Key" size="large" maxlength="6" class="flex-1" />
            <el-button 
              type="primary" 
              native-type="button" 
              :loading="sendingLoading" 
              :disabled="disabledSend || countdown > 0" 
              @click="sendVerificationCode"
              size="large"
              class="w-32"
            >
              {{ countdown > 0 ? `${countdown}秒后重发` : '发送验证码' }}
            </el-button>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" native-type="submit" :loading="loading" class="w-full" size="large">验证并下一步</el-button>
          </el-form-item>
        </el-form>
      </template>

      <!-- 步骤2: 输入新密码 -->
      <template v-if="step === 2">
        <el-form @submit.prevent="handleReset" :model="{ password: password, confirmPassword: confirmPassword }" label-position="top">
          <el-form-item label="新密码" required>
            <el-input v-model="password" type="password" placeholder="请输入新密码，至少6位" prefix-icon="Lock" size="large" show-password />
          </el-form-item>
          <el-form-item label="确认新密码" required>
            <el-input v-model="confirmPassword" type="password" placeholder="请再次输入新密码" prefix-icon="Lock" size="large" show-password />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" native-type="submit" :loading="loading" class="w-full" size="large">重置密码</el-button>
          </el-form-item>
        </el-form>
      </template>

      <div class="text-center mt-4 flex flex-col sm:flex-row justify-between items-center gap-2">
        <button @click="goBack" class="text-gray-500 hover:text-gray-700 dark:text-gray-400 dark:hover:text-gray-300 flex items-center justify-center gap-1">
          <el-icon><ArrowLeft /></el-icon> 返回登录
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'
import { forgotPassword, verifyCode, resetPassword } from '@/api/blog'

const router = useRouter()
const loading = ref(false)
const sendingLoading = ref(false)
const step = ref(1)
const account = ref('')
const verificationCode = ref('')
const password = ref('')
const confirmPassword = ref('')
const disabledSend = ref(false)
const countdown = ref(0)
let countdownTimer = null

// 开始倒计时
const startCountdown = () => {
  countdown.value = 60 // 60秒倒计时
  if (countdownTimer) clearInterval(countdownTimer)
  countdownTimer = setInterval(() => {
    countdown.value--
    if (countdown.value <= 0) {
      clearInterval(countdownTimer)
      countdownTimer = null
    }
  }, 1000)
}

// 发送验证码
const sendVerificationCode = async () => {
  if (!account.value) {
    ElMessage.warning('请输入账号')
    return
  }
  
  disabledSend.value = true
  sendingLoading.value = true
  try {
    const res = await forgotPassword(account.value)
    if (res.success) {
      ElMessage.success('验证码已发送至您的邮箱，请注意查收')
      startCountdown() // 开始60秒倒计时
    } else {
      ElMessage.error(res.message)
    }
  } catch (e) {
    ElMessage.error('验证码发送失败，请稍后重试')
  } finally {
    sendingLoading.value = false
    disabledSend.value = false
  }
}

// 验证账号和验证码
const validateAccount = async () => {
  if (!account.value) {
    ElMessage.warning('请输入账号')
    return
  }
  
  if (!verificationCode.value) {
    ElMessage.warning('请输入验证码')
    return
  }
  
  if (verificationCode.value.length < 6) {
    ElMessage.warning('验证码长度不足')
    return
  }

  loading.value = true
  try {
    const res = await verifyCode({
      username: account.value,
      code: verificationCode.value
    })
    
    if (res.success) {
      ElMessage.success('验证成功，请设置新密码')
      step.value = 2
    } else {
      ElMessage.error(res.message)
    }
  } catch (e) {
    ElMessage.error('验证失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

// 处理密码重置
const handleReset = async () => {
  if (!password.value) {
    ElMessage.warning('请输入新密码')
    return
  }
  if (password.value.length < 6) {
    ElMessage.warning('密码长度至少为6位')
    return
  }
  if (password.value !== confirmPassword.value) {
    ElMessage.warning('两次输入的密码不一致')
    return
  }
  loading.value = true
  try {
    const res = await resetPassword({ 
      username: account.value, 
      code: verificationCode.value, 
      password: password.value 
    })
    if (res.success) {
      ElMessage.success('密码重置成功！')
      router.push('/login')
    } else {
      ElMessage.error(res.message)
    }
  } catch (e) {
    ElMessage.error('密码重置失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

// 返回上一步或登录页
const goBack = () => {
  if (step.value === 2) {
    step.value = 1
  } else {
    router.push('/login')
  }
}

// 组件卸载时清理定时器
onUnmounted(() => {
  if (countdownTimer) {
    clearInterval(countdownTimer)
  }
})
</script>
