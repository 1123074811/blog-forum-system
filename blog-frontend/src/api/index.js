import axios from 'axios'
import appConfig from '@/config'

const api = axios.create({
  baseURL: appConfig.apiBaseUrl,
  timeout: 10000
})

const pendingMap = new Map()
let isRefreshing = false
let refreshQueue = []

const generateKey = (config = {}) => {
  const { method, url, params, data } = config
  return [method, url, JSON.stringify(params || {}), JSON.stringify(data || {})].join('&')
}

const removePending = (config = {}) => {
  const key = generateKey(config)
  if (pendingMap.has(key)) {
    pendingMap.get(key).abort()
    pendingMap.delete(key)
  }
}

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
  removePending(config)
  const controller = new AbortController()
  config.signal = controller.signal
  pendingMap.set(generateKey(config), controller)

  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

api.interceptors.response.use(
  response => {
    pendingMap.delete(generateKey(response.config))

    const newToken = response.headers['x-new-token']
    if (newToken) {
      localStorage.setItem('token', newToken)
    }
    return response.data
  },
  async error => {
    const originalRequest = error.config || {}
    pendingMap.delete(generateKey(originalRequest))

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
        }).then(token => {
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
        clearAuth()
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
