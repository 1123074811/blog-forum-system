import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import api from '@/api'

export const useUserStore = defineStore('user', () => {
  const user = ref(JSON.parse(localStorage.getItem('user') || 'null'))
  const token = ref(localStorage.getItem('token') || '')
  const isDark = ref(localStorage.getItem('isDark') === 'true')
  const isAdmin = ref(false)

  // session 初始化完成的 Promise，路由守卫等它完成再放行
  let _sessionReady = null
  let _sessionResolve = null

  const sessionReady = () => {
    if (!_sessionReady) {
      _sessionReady = new Promise(resolve => { _sessionResolve = resolve })
    }
    return _sessionReady
  }

  const isLoggedIn = computed(() => !!token.value)

  async function checkAdminStatus() {
    if (!token.value) {
      isAdmin.value = false
      return false
    }
    try {
      await api.get('/admin/ping')
      isAdmin.value = true
      return true
    } catch {
      isAdmin.value = false
      return false
    }
  }

  async function initSession() {
    // 确保 Promise 已创建
    sessionReady()

    try {
      if (!token.value) {
        const refreshToken = localStorage.getItem('refreshToken')
        if (refreshToken) {
          try {
            const res = await api.post('/auth/refresh', null, {
              headers: { Authorization: `Bearer ${refreshToken}` }
            })
            if (res.success && res.data) {
              setToken(res.data)
            }
          } catch {
            localStorage.removeItem('refreshToken')
          }
        }
      }

      if (token.value) {
        await checkAdminStatus()
      }
    } finally {
      // 无论成功失败，都标记初始化完成
      _sessionResolve?.()
    }
  }

  function setToken(value) {
    token.value = value || ''
    if (token.value) {
      localStorage.setItem('token', token.value)
    } else {
      localStorage.removeItem('token')
    }
  }

  function setUser(userData, tokenData, refreshToken) {
    const safeUser = userData
      ? {
          id: userData.id,
          username: userData.username,
          nickname: userData.nickname,
          avatar: userData.avatar
        }
      : null

    user.value = safeUser
    setToken(tokenData)

    if (safeUser) {
      localStorage.setItem('user', JSON.stringify(safeUser))
    } else {
      localStorage.removeItem('user')
    }

    if (refreshToken) {
      localStorage.setItem('refreshToken', refreshToken)
    }

    checkAdminStatus()
  }

  async function logout() {
    try {
      await api.post('/auth/logout')
    } catch {
      // 忽略错误，继续清理本地状态
    }
    user.value = null
    setToken('')
    isAdmin.value = false
    localStorage.removeItem('user')
    localStorage.removeItem('refreshToken')
    // 重置 session ready，下次登录后重新初始化
    _sessionReady = null
    _sessionResolve = null
  }

  function toggleDark() {
    isDark.value = !isDark.value
    localStorage.setItem('isDark', isDark.value.toString())
  }

  return {
    user,
    token,
    isDark,
    isLoggedIn,
    isAdmin,
    setUser,
    setToken,
    logout,
    toggleDark,
    checkAdminStatus,
    initSession,
    sessionReady,
  }
})
