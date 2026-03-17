/**
 * 轻量内存缓存，解决路由切换时重复请求问题
 * 支持 TTL 过期、请求去重（防止并发重复请求）
 */

const cache = new Map()
const pendingRequests = new Map()

/**
 * @param {string} key 缓存key
 * @param {Function} fetcher 数据获取函数，返回Promise
 * @param {number} ttl 缓存时间（毫秒），默认5分钟
 */
export async function memCache(key, fetcher, ttl = 5 * 60 * 1000) {
  const now = Date.now()
  const entry = cache.get(key)

  // 命中有效缓存
  if (entry && now < entry.expireAt) {
    return entry.data
  }

  // 防止并发重复请求（同一key同时发起多个请求时，后续请求等待第一个）
  if (pendingRequests.has(key)) {
    return pendingRequests.get(key)
  }

  const promise = fetcher().then(data => {
    cache.set(key, { data, expireAt: now + ttl })
    pendingRequests.delete(key)
    return data
  }).catch(err => {
    pendingRequests.delete(key)
    throw err
  })

  pendingRequests.set(key, promise)
  return promise
}

/**
 * 手动清除指定key或所有缓存
 */
export function invalidateCache(key) {
  if (key) {
    cache.delete(key)
  } else {
    cache.clear()
  }
}

/**
 * 清除匹配前缀的所有缓存
 */
export function invalidateCacheByPrefix(prefix) {
  for (const key of cache.keys()) {
    if (key.startsWith(prefix)) cache.delete(key)
  }
}
