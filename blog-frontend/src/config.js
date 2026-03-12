export default {
  siteName: '树欲静而风不止',
  logo: '/logo.png',
  apiBaseUrl: import.meta.env.VITE_API_BASE_URL || '/api',
  wsBaseUrl: import.meta.env.VITE_WS_BASE_URL || (() => {
    if (typeof window === 'undefined') return 'ws://localhost:8080'
    const wsProtocol = window.location.protocol === 'https:' ? 'wss' : 'ws'
    return `${wsProtocol}://${window.location.host}`
  })()
}
