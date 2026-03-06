import axios from 'axios'
import appConfig from '@/config'

const api = axios.create({
  baseURL: appConfig.apiBaseUrl,
  timeout: 10000
})

api.interceptors.request.use(config => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

api.interceptors.response.use(
  response => {
    // 滑动过期：检查是否有新 token
    const newToken = response.headers['x-new-token']
    if (newToken) {
      localStorage.setItem('token', newToken)
    }
    return response.data
  },
  error => {
    if (error.response?.status === 401 || error.response?.status === 403) {
      // 检查用户是否之前有 token（即曾经登录过）
      const hadToken = !!localStorage.getItem('token')
      
      // 清除过期的认证信息
      localStorage.removeItem('token')
      localStorage.removeItem('refreshToken')
      localStorage.removeItem('user')
      
      // 只有当用户之前有 token（即曾经登录过）且当前不在登录相关页面时才跳转
      const isAuthPage = ['/login', '/register', '/forgot-password'].includes(window.location.pathname)
      const isPublicPage = ['/', '/discover', '/community', '/tree-hole', '/search', '/about', '/er-diagram', '/life-simulator', '/pomodoro'].some(path => 
        window.location.pathname === path || window.location.pathname.startsWith('/article/') || window.location.pathname.startsWith('/user/')
      )
      
      // 只有在以下情况才跳转到登录页：
      // 1. 用户之前有 token（说明是 token 过期）
      // 2. 当前不在登录相关页面
      // 3. 当前不在公开页面（公开页面的 401 错误应该被忽略）
      if (hadToken && !isAuthPage && !isPublicPage) {
        window.location.href = '/login?msg=登录已过期，请重新登录'
      }
    }
    return Promise.reject(error)
  }
)

export default api
