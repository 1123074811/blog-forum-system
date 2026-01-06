import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useUserStore = defineStore('user', () => {
  const user = ref(JSON.parse(localStorage.getItem('user') || 'null'))
  const token = ref(localStorage.getItem('token') || '')
  const isDark = ref(localStorage.getItem('isDark') === 'true')

  const isLoggedIn = computed(() => !!token.value)
  const isAdmin = computed(() => user.value?.role === 'admin')

  function setUser(userData, tokenData, refreshToken) {
    user.value = userData
    token.value = tokenData
    localStorage.setItem('user', JSON.stringify(userData))
    localStorage.setItem('token', tokenData)
    localStorage.setItem('refreshToken', refreshToken)
  }

  function logout() {
    user.value = null
    token.value = ''
    localStorage.removeItem('user')
    localStorage.removeItem('token')
    localStorage.removeItem('refreshToken')
  }

  function toggleDark() {
    isDark.value = !isDark.value
    localStorage.setItem('isDark', isDark.value.toString())
  }

  return { user, token, isDark, isLoggedIn, isAdmin, setUser, logout, toggleDark }
})
