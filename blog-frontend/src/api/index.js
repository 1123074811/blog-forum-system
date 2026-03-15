import axios from 'axios'
import appConfig from '@/config'

const api = axios.create({
  baseURL: appConfig.apiBaseUrl,
  timeout: 10000
})

let isRefreshing = false
let refreshQueue = []

const processQueue = (error, token = null) => {
  refreshQueue.forEach(({ resolve, reject }) => {
    if (error) {
      reject(error)
    } else {
      resolve(token)
    }
  })
  refreshQueue = []
}

const clearAuth = () => {
  localStorage.removeItem('token')
  localStorage.removeItem('refreshToken')
  localStorage.removeItem('user')
}

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
  async error => {
    const originalRequest = error.config || {}
    const status = error.response?.status

    if (status === 401 && !originalRequest._retry && !String(originalRequest.url || '').includes('/auth/refresh')) {
      originalRequest._retry = true
      const refreshToken = localStorage.getItem('refreshToken')

      if (!refreshToken) {
        clearAuth()
        return Promise.reject(error)
      }

      if (isRefreshing) {
        return new Promise((resolve, reject) => {
          refreshQueue.push({ resolve, reject })
        })
          .then(token => {
            originalRequest.headers.Authorization = `Bearer ${token}`
            return api(originalRequest)
          })
      }

      isRefreshing = true
      try {
        const refreshResponse = await axios.post(
          `${appConfig.apiBaseUrl}/auth/refresh`,
          null,
          { headers: { Authorization: `Bearer ${refreshToken}` }, timeout: 10000 }
        )

        const newToken = refreshResponse.data?.data
        if (!newToken) {
          throw new Error('Refresh token failed')
        }

        localStorage.setItem('token', newToken)
        processQueue(null, newToken)
        originalRequest.headers.Authorization = `Bearer ${newToken}`
        return api(originalRequest)
      } catch (refreshError) {
        processQueue(refreshError, null)
        // 清除过期的认证信息
        clearAuth()
        // 只有在以下情况才跳转到登录页：
        // 1. 用户之前有 token（说明是 token 过期）
        // 2. 当前不在登录相关页面
        // 3. 当前不在公开页面（公开页面的 401 错误应该被忽略）
        if (!['/login', '/register', '/forgot-password'].includes(window.location.pathname)) {
          window.location.href = '/login?msg=登录已过期，请重新登录'
        }
        return Promise.reject(refreshError)
      } finally {
        isRefreshing = false
      }
    }

    if (status === 401) {
      clearAuth()
    }

    return Promise.reject(error)
  }
)

export default api
