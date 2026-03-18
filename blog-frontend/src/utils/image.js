const LOCAL_HOSTS = new Set(['localhost', '127.0.0.1', '0.0.0.0', '::1', '[::1]'])
const OSS_HOST_RE = /(?:^|\.)aliyuncs\.com$/i

const isHttpUrl = (value) => typeof value === 'string' && /^https?:\/\//i.test(value.trim())

export const normalizeUnsafeUrl = (value) => {
  if (!isHttpUrl(value)) return value

  const raw = value.trim()
  try {
    const url = new URL(raw)
    if (typeof window === 'undefined') return raw

    const host = (url.hostname || '').toLowerCase()

    // Data fallback for historical bad data: rewrite localhost URLs to current origin.
    if (LOCAL_HOSTS.has(host)) {
      return `${window.location.origin}${url.pathname}${url.search}${url.hash}`
    }

    // Mixed-content fallback: upgrade known safe hosts in HTTPS context.
    if (window.location.protocol === 'https:' && url.protocol === 'http:') {
      const sameHost = host === window.location.hostname
      const knownCdnHost = OSS_HOST_RE.test(host)
      if (sameHost || knownCdnHost) {
        url.protocol = 'https:'
        return url.toString()
      }
    }

    return raw
  } catch {
    return value
  }
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
