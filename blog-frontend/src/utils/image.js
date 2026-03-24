const LOCAL_HOSTS = new Set(['localhost', '127.0.0.1', '0.0.0.0', '::1', '[::1]'])
const OSS_HOST_RE = /(?:^|\.)aliyuncs\.com$/i

const isHttpUrl = (value) => typeof value === 'string' && /^https?:\/\//i.test(value.trim())

const decodeHtmlEntities = (value) => {
  if (typeof value !== 'string' || !value.includes('&')) return value
  return value
    .replace(/&#x2F;/gi, '/')
    .replace(/&#47;/g, '/')
    .replace(/&quot;/g, '"')
    .replace(/&#x27;/gi, "'")
    .replace(/&gt;/g, '>')
    .replace(/&lt;/g, '<')
    .replace(/&amp;/g, '&')
}

export const normalizeUnsafeUrl = (value) => {
  if (typeof value !== 'string' || !value) return value
  const decoded = decodeHtmlEntities(value)
  if (isHttpUrl(decoded)) {
    const raw = decoded.trim()
    try {
      const url = new URL(raw)
      if (typeof window === 'undefined') return raw
      const host = (url.hostname || '').toLowerCase()
      if (LOCAL_HOSTS.has(host)) {
        // 无论端口是否相同，都转成相对路径走 Vite 代理
        return `${url.pathname}${url.search}${url.hash}`
      }
      return raw
    } catch { return decoded }
  }

  return decoded
}

export const normalizeDeepUrls = (value, seen = new WeakSet()) => {
  if (typeof value === 'string') {
    return normalizeUnsafeUrl(value)
  }

  if (!value || typeof value !== 'object') {
    return value
  }

  if (seen.has(value)) {
    return value
  }
  seen.add(value)

  if (Array.isArray(value)) {
    for (let i = 0; i < value.length; i++) {
      value[i] = normalizeDeepUrls(value[i], seen)
    }
    return value
  }

  Object.keys(value).forEach((key) => {
    value[key] = normalizeDeepUrls(value[key], seen)
  })
  return value
}

const hasOssProcess = (url) => /[?&]x-oss-process=/i.test(url)
const isAliyunOss = (url) => /aliyuncs\.com/i.test(url)

export const toAvatarThumb = (value, size = 80) => {
  const safeUrl = normalizeUnsafeUrl(value)
  if (typeof safeUrl !== 'string' || !safeUrl) return safeUrl
  if (!isAliyunOss(safeUrl) || hasOssProcess(safeUrl)) return safeUrl

  const dim = Math.max(32, Number(size) || 80)
  const process = `image/resize,m_fill,w_${dim},h_${dim}/quality,Q_85/format,webp`
  const joiner = safeUrl.includes('?') ? '&' : '?'
  return `${safeUrl}${joiner}x-oss-process=${encodeURIComponent(process)}`
}
