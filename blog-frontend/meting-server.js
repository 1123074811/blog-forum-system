/**
 * B站音乐代理服务（支持 wbi 签名 + 音频/图片反代）
 */
import http from 'http'
import https from 'https'
import { URL } from 'url'
import { createHash } from 'crypto'

const PORT = Number(process.env.PORT || 3000)

const BILI_HEADERS = {
  'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36',
  'Referer': 'https://www.bilibili.com',
  'Origin': 'https://www.bilibili.com',
}

// 内存缓存
const cache = new Map()
const CACHE_TTL = 10 * 60 * 1000

function getCache(key) {
  const item = cache.get(key)
  if (!item) return null
  if (Date.now() - item.ts > CACHE_TTL) { cache.delete(key); return null }
  return item.data
}
function setCache(key, data) { cache.set(key, { data, ts: Date.now() }) }

function biliGet(url) {
  return new Promise((resolve, reject) => {
    const cached = getCache(url)
    if (cached) return resolve(cached)
    const req = https.get(url, { headers: BILI_HEADERS }, (res) => {
      let raw = ''
      res.on('data', chunk => raw += chunk)
      res.on('end', () => {
        try { const data = JSON.parse(raw); setCache(url, data); resolve(data) }
        catch (e) { reject(new Error('JSON parse failed: ' + raw.slice(0, 200))) }
      })
    })
    req.on('error', reject)
    req.setTimeout(10000, () => { req.destroy(); reject(new Error('timeout')) })
  })
}

// wbi 签名
const MIXIN_KEY_ENC_TAB = [
  46,47,18,2,53,8,23,32,15,50,10,31,58,3,45,35,27,43,5,49,
  33,9,42,19,29,28,14,39,12,38,41,13,37,48,7,16,24,55,40,61,
  26,17,0,1,60,51,30,4,22,25,54,21,56,59,6,63,57,62,11,36,
  20,34,44,52
]
let wbiKeys = null, wbiKeyTs = 0
const WBI_KEY_TTL = 12 * 60 * 60 * 1000

async function getWbiKeys() {
  if (wbiKeys && Date.now() - wbiKeyTs < WBI_KEY_TTL) return wbiKeys
  const res = await biliGet('https://api.bilibili.com/x/web-interface/nav')
  const imgKey = (res?.data?.wbi_img?.img_url || '').split('/').pop().replace('.png', '')
  const subKey = (res?.data?.wbi_img?.sub_url || '').split('/').pop().replace('.png', '')
  wbiKeys = MIXIN_KEY_ENC_TAB.map(i => (imgKey + subKey)[i]).join('').slice(0, 32)
  wbiKeyTs = Date.now()
  return wbiKeys
}

function encodeWbi(params, mixinKey) {
  const wts = Math.floor(Date.now() / 1000)
  const allParams = { ...params, wts }
  const query = Object.keys(allParams).sort().map(k => {
    const v = String(allParams[k]).replace(/[!'()*]/g, '')
    return `${encodeURIComponent(k)}=${encodeURIComponent(v)}`
  }).join('&')
  const wRid = createHash('md5').update(query + mixinKey).digest('hex')
  return query + `&w_rid=${wRid}`
}

async function wbiGet(path, params) {
  const mixinKey = await getWbiKeys()
  const qs = encodeWbi(params, mixinKey)
  return biliGet(`https://api.bilibili.com${path}?${qs}`)
}

async function getVideoInfo(bvid) {
  const res = await biliGet(`https://api.bilibili.com/x/web-interface/view?bvid=${bvid}`)
  if (res.code !== 0) throw new Error(`bilibili view error: ${res.message}`)
  return res.data
}

async function getAudioUrl(bvid, cid) {
  const res = await wbiGet('/x/player/wbi/playurl', { bvid, cid, fnval: 16, fnver: 0, fourk: 1 })
  if (res.code !== 0) throw new Error(`bilibili playurl error: ${res.message}`)
  const dash = res.data?.dash
  if (!dash?.audio?.length) throw new Error('no audio stream found')
  const audios = dash.audio.sort((a, b) => b.bandwidth - a.bandwidth)
  return audios[0].baseUrl || audios[0].base_url
}

// 检测视频是否有可用音频流，结果缓存
async function checkAudioAvailable(bvid) {
  const cacheKey = `avail:${bvid}`
  const cached = getCache(cacheKey)
  if (cached !== null) return cached
  try {
    const info = await getVideoInfo(bvid)
    if (!info?.cid) { setCache(cacheKey, false); return false }
    const res = await wbiGet('/x/player/wbi/playurl', { bvid, cid: info.cid, fnval: 16, fnver: 0, fourk: 1 })
    const ok = res.code === 0 && res.data?.dash?.audio?.length > 0
    // 不可用的缓存 5 分钟，可用的缓存 10 分钟
    cache.set(cacheKey, { data: ok, ts: Date.now() - (ok ? 0 : 5 * 60 * 1000) })
    return ok
  } catch {
    setCache(cacheKey, false)
    return false
  }
}

async function searchVideos(keyword, limit = 20) {
  try {
    const res = await wbiGet('/x/web-interface/search/type', { search_type: 'video', keyword, page: 1, page_size: limit })
    if (res.code === 0) return res.data?.result || []
  } catch (e) {
    console.error('[search wbi]', e.message)
  }
  const res2 = await biliGet(`https://api.bilibili.com/x/web-interface/search/all/v2?keyword=${encodeURIComponent(keyword)}&page=1`)
  if (res2.code === 0) {
    const videoModule = (res2.data?.result || []).find(m => m.result_type === 'video')
    return (videoModule?.data || []).slice(0, limit)
  }
  throw new Error(`search failed: ${res2.message}`)
}

function proxyCover(rawPic) {
  if (!rawPic) return ''
  const cover = rawPic.startsWith('//') ? 'https:' + rawPic : rawPic
  return `/music-api/img-proxy?url=${encodeURIComponent(cover)}`
}

function normalizeSong(video) {
  const cover = proxyCover(video.pic || video.cover || '')
  const artists = [{ name: video.author || video.uploader || '未知' }]
  return {
    id: video.bvid,
    name: video.title?.replace(/<[^>]+>/g, '') || '',
    artists, ar: artists,
    album: { name: video.typename || video.tag || 'B站', picUrl: cover },
    al:    { name: video.typename || video.tag || 'B站', picUrl: cover },
    duration: (video.duration || 0) * 1000,
    _platform: 'bilibili', _bvid: video.bvid,
  }
}

function json(res, statusCode, payload) {
  res.statusCode = statusCode
  res.setHeader('Access-Control-Allow-Origin', '*')
  res.setHeader('Access-Control-Allow-Methods', 'GET,OPTIONS')
  res.setHeader('Content-Type', 'application/json; charset=utf-8')
  res.end(JSON.stringify(payload))
}

const server = http.createServer(async (req, res) => {
  if (req.method === 'OPTIONS') { json(res, 204, {}); return }
  const reqUrl = new URL(req.url, `http://localhost:${PORT}`)
  const path = reqUrl.pathname

  try {
    // /search
    if (path === '/search') {
      const keywords = reqUrl.searchParams.get('keywords') || ''
      const limit = Number(reqUrl.searchParams.get('limit') || 20)
      if (!keywords.trim()) { json(res, 400, { error: 'missing keywords' }); return }
      const fetchLimit = Math.min(limit * 3, 50)
      const results = await searchVideos(keywords, fetchLimit)
      const available = []
      const concurrency = 5
      for (let i = 0; i < results.length && available.length < limit; i += concurrency) {
        const batch = results.slice(i, i + concurrency)
        const checks = await Promise.all(batch.map(async v => (await checkAudioAvailable(v.bvid)) ? v : null))
        checks.forEach(v => { if (v && available.length < limit) available.push(v) })
      }
      json(res, 200, { result: { songs: available.map(normalizeSong) } })
      return
    }

    // /song/detail
    if (path === '/song/detail') {
      const ids = (reqUrl.searchParams.get('ids') || '').split(',').map(s => s.trim()).filter(Boolean)
      if (!ids.length) { json(res, 400, { error: 'missing ids' }); return }
      const songs = await Promise.all(ids.map(async (bvid) => {
        const info = await getVideoInfo(bvid)
        const cover = proxyCover(info.pic || '')
        const artists = [{ name: info.owner?.name || '未知' }]
        return { id: bvid, name: info.title || '', artists, ar: artists,
          album: { name: info.tname || 'B站', picUrl: cover },
          al:    { name: info.tname || 'B站', picUrl: cover },
          duration: (info.duration || 0) * 1000, _platform: 'bilibili', _bvid: bvid, _cid: info.cid }
      }))
      json(res, 200, { songs })
      return
    }

    // /song/url
    if (path === '/song/url') {
      const bvid = reqUrl.searchParams.get('id')
      if (!bvid) { json(res, 400, { error: 'missing id' }); return }
      try {
        const info = await getVideoInfo(bvid)
        if (!info?.cid) { json(res, 200, { data: [{ url: null, message: 'cid not found' }] }); return }
        const audioUrl = await getAudioUrl(bvid, info.cid)
        const proxyUrl = `/music-api/audio-proxy?url=${encodeURIComponent(audioUrl)}`
        json(res, 200, { data: [{ url: proxyUrl, br: 320000 }] })
      } catch (e) {
        console.warn(`[song/url] ${bvid} unavailable:`, e.message)
        json(res, 200, { data: [{ url: null, message: e.message }] })
      }
      return
    }

    // /lyric
    if (path === '/lyric') {
      json(res, 200, { lrc: { lyric: '' } })
      return
    }

    // /img-proxy
    if (path === '/img-proxy') {
      const imgUrl = reqUrl.searchParams.get('url')
      if (!imgUrl) { json(res, 400, { error: 'missing url' }); return }
      const imgReq = https.get(imgUrl, { headers: BILI_HEADERS }, (imgRes) => {
        res.statusCode = imgRes.statusCode
        res.setHeader('Access-Control-Allow-Origin', '*')
        res.setHeader('Cache-Control', 'public, max-age=86400')
        res.setHeader('Content-Type', imgRes.headers['content-type'] || 'image/jpeg')
        imgRes.pipe(res)
      })
      imgReq.on('error', () => { if (!res.headersSent) { res.statusCode = 502; res.end() } })
      imgReq.setTimeout(8000, () => { imgReq.destroy(); if (!res.headersSent) { res.statusCode = 504; res.end() } })
      return
    }

    // /audio-proxy — 音频流反代，带 Referer，支持 Range
    if (path === '/audio-proxy') {
      const audioUrl = reqUrl.searchParams.get('url')
      if (!audioUrl) { json(res, 400, { error: 'missing url' }); return }
      const headers = { ...BILI_HEADERS }
      if (req.headers['range']) headers['Range'] = req.headers['range']
      const audioReq = https.get(audioUrl, { headers }, (audioRes) => {
        res.statusCode = audioRes.statusCode
        res.setHeader('Access-Control-Allow-Origin', '*')
        res.setHeader('Accept-Ranges', 'bytes')
        if (audioRes.headers['content-type'])   res.setHeader('Content-Type', audioRes.headers['content-type'])
        if (audioRes.headers['content-length']) res.setHeader('Content-Length', audioRes.headers['content-length'])
        if (audioRes.headers['content-range'])  res.setHeader('Content-Range', audioRes.headers['content-range'])
        audioRes.pipe(res)
      })
      audioReq.on('error', () => { if (!res.headersSent) { res.statusCode = 502; res.end() } })
      audioReq.setTimeout(30000, () => { audioReq.destroy(); if (!res.headersSent) { res.statusCode = 504; res.end() } })
      return
    }

    json(res, 404, { error: 'unknown endpoint' })
  } catch (err) {
    console.error(`[bili-music] ${path} error:`, err.message)
    if (!res.headersSent) json(res, 500, { error: err.message })
  }
})

server.listen(PORT, () => {
  console.log(`Bilibili music API running at http://localhost:${PORT}`)
  console.log('Endpoints: /search  /song/detail  /song/url  /lyric  /img-proxy  /audio-proxy')
})
