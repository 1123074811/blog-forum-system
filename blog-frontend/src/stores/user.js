import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import api from '@/api'

export const useUserStore = defineStore('user', () => {
  const user = ref(JSON.parse(localStorage.getItem('user') || 'null'))
  const token = ref(localStorage.getItem('token') || '')
  const isDark = ref(localStorage.getItem('isDark') === 'true')
  const isAdmin = ref(false)

  let sessionReadyPromise = null
  let resolveSessionReady = null

  const sessionReady = () => {
    if (!sessionReadyPromise) {
      sessionReadyPromise = new Promise(resolve => {
        resolveSessionReady = resolve
      })
    }
    return sessionReadyPromise
  }

  const isLoggedIn = computed(() => !!token.value)

  async function checkAdminStatus() {
    if (!token.value) {
      isAdmin.value = false
      return false
    }
    try {
      const res = await api.get('/auth/me')
      if (res.success && res.data?.authenticated) {
        isAdmin.value = String(res.data.role || '').toUpperCase() === 'ADMIN'
        return isAdmin.value
      }
      isAdmin.value = false
      return false
    } catch {
      isAdmin.value = false
      return false
    }
  }

  async function initSession() {
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
      resolveSessionReady?.()
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

  async function setUser(userData, tokenData, refreshToken) {
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

    sessionReady()
    await checkAdminStatus()
    resolveSessionReady?.()
  }

  async function logout() {
    try {
      // 调用后端登出接口删除 Redis 中的 token
      await api.post('/auth/logout')
    } catch {
      // 忽略错误，继续清理本地状态
    }
    user.value = null
    setToken('')
    isAdmin.value = false
    localStorage.removeItem('user')
    localStorage.removeItem('refreshToken')
    sessionReadyPromise = null
    resolveSessionReady = null
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
    sessionReady
  }
})
