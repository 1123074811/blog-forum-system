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
    if (error.response?.status === 401) {
      localStorage.removeItem('token')
      localStorage.removeItem('refreshToken')
      window.location.href = '/login'
    }
    return Promise.reject(error)
  }
)

export default api
