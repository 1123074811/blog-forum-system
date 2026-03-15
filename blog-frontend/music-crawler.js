/**
 * 音乐爬虫服务 - 统一代理网易云、酷狗音乐接口
 * 端口: 3000（替代 NeteaseCloudMusicApi，前端 /music-api 代理到此）
 * 启动: node music-crawler.js
 */
import http from 'http'
import https from 'https'
import { URL } from 'url'
import crypto from 'crypto'

const PORT = 3000

// ─── HTTP 请求封装 ────────────────────────────────────────────────────────────
const request = (url, options = {}) => {
  return new Promise((resolve, reject) => {
    const urlObj = new URL(url)
    const client = urlObj.protocol === 'https:' ? https : http
    const reqOptions = {
      method: options.method || 'GET',
      headers: {
        'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36',
        Accept: 'application/json, text/plain, */*',
        'Accept-Language': 'zh-CN,zh;q=0.9',
        ...options.headers
      }
    }
    const req = client.request(url, reqOptions, (res) => {
      // 跟随重定向
      if ((res.statusCode === 301 || res.statusCode === 302) && res.headers.location) {
        return request(res.headers.location, options).then(resolve).catch(reject)
      }
      const chunks = []
      res.on('data', chunk => chunks.push(chunk))
      res.on('end', () => {
        const body = Buffer.concat(chunks).toString('utf-8')
        try { resolve(JSON.parse(body)) }
        catch { resolve(body) }
      })
    })
    req.on('error', reject)
    req.setTimeout(12000, () => { req.destroy(); reject(new Error('timeout')) })
    if (options.body) req.write(options.body)
    req.end()
  })
}

// ─── 网易云音乐加密 ───────────────────────────────────────────────────────────
const NETEASE_KEY = Buffer.from('7246674226682325323F5E6544673A51', 'hex')
const NETEASE_IV  = Buffer.from('01020304050607080102030405060708', 'hex')

function aesEncrypt(data, key, iv) {
  const cipher = crypto.createCipheriv('aes-128-cbc', key, iv)
  return Buffer.concat([cipher.update(Buffer.from(data)), cipher.final()]).toString('base64')
}

function buildNeteaseParams(obj) {
  const text = JSON.stringify(obj)
  const encrypted = aesEncrypt(text, NETEASE_KEY, NETEASE_IV)
  return new URLSearchParams({ params: encrypted }).toString()
}

// 网易云 eapi 加密（用于获取播放链接）
function eapiEncrypt(url, text) {
  const message = `nobody${url}use${text}md5forencrypt`
  const digest = crypto.createHash('md5').update(message).digest('hex')
  const data = `${url}-36cd479b6b5-${text}-36cd479b6b5-${digest}`
  const cipher = crypto.createCipheriv('aes-128-ecb', Buffer.from('e82ckenh8dichen8'), null)
  return Buffer.concat([cipher.update(Buffer.from(data)), cipher.final()]).toString('hex').toUpperCase()
}

// ─── 网易云 API ───────────────────────────────────────────────────────────────
const neteaseHeaders = {
  Referer: 'https://music.163.com/',
  Origin: 'https://music.163.com',
  Cookie: 'os=pc; appver=2.9.7; MUSIC_U=; __csrf='
}

async function neteaseSearch(keywords, limit = 30) {
  try {
    const body = buildNeteaseParams({ s: keywords, type: 1, limit, offset: 0, total: true })
    const data = await request('https://music.163.com/weapi/search/get', {
      method: 'POST',
      headers: { ...neteaseHeaders, 'Content-Type': 'application/x-www-form-urlencoded' },
      body
    })
    if (data?.result?.songs?.length > 0) return data
    throw new Error('empty result')
  } catch (e) {
    console.log('[网易云搜索爬虫失败，降级到 NeteaseCloudMusicApi]', e.message)
    try {
      return await request(`http://localhost:3001/search?keywords=${encodeURIComponent(keywords)}&limit=${limit}`)
    } catch (e2) {
      console.error('[网易云搜索降级也失败]', e2.message)
      return { result: { songs: [] } }
    }
  }
}

async function neteaseDetail(ids) {
  try {
    const c = ids.split(',').map(id => ({ id: parseInt(id) }))
    const body = buildNeteaseParams({ c: JSON.stringify(c), ids })
    const data = await request('https://music.163.com/weapi/v3/song/detail', {
      method: 'POST',
      headers: { ...neteaseHeaders, 'Content-Type': 'application/x-www-form-urlencoded' },
      body
    })
    if (data?.songs?.length > 0) return data
    throw new Error('empty result')
  } catch (e) {
    console.log('[网易云详情爬虫失败，降级]', e.message)
    try {
      return await request(`http://localhost:3001/song/detail?ids=${ids}`)
    } catch (e2) {
      return { songs: [] }
    }
  }
}

async function neteaseUrl(id) {
  try {
    // 方案1: weapi
    const body = buildNeteaseParams({ ids: [parseInt(id)], br: 320000 })
    const data = await request('https://music.163.com/weapi/song/enhance/player/url', {
      method: 'POST',
      headers: { ...neteaseHeaders, 'Content-Type': 'application/x-www-form-urlencoded' },
      body
    })
    if (data?.data?.[0]?.url) return { ...data, _source: 'crawler' }
  } catch (e) { console.log('[网易云URL方案1失败]', e.message) }

  try {
    // 方案2: 无损接口
    const body = buildNeteaseParams({ ids: `[${id}]`, level: 'standard', encodeType: 'aac' })
    const data = await request('https://music.163.com/weapi/song/enhance/player/url/v1', {
      method: 'POST',
      headers: { ...neteaseHeaders, 'Content-Type': 'application/x-www-form-urlencoded' },
      body
    })
    if (data?.data?.[0]?.url) return { ...data, _source: 'crawler' }
  } catch (e) { console.log('[网易云URL方案2失败]', e.message) }

  // 方案3: 降级到 NeteaseCloudMusicApi（port 3001）
  try {
    console.log(`[网易云URL] 爬虫失败，降级到 NeteaseCloudMusicApi, id=${id}`)
    const data = await request(`http://localhost:3001/song/url?id=${id}`)
    if (data?.data?.[0]?.url) return { ...data, _source: 'fallback-api' }
  } catch (e) { console.log('[网易云URL降级失败]', e.message) }

  return { data: [{ id: parseInt(id), url: null, code: 404 }] }
}

async function neteaseLyric(id) {
  try {
    const body = buildNeteaseParams({ id, lv: -1, kv: -1, tv: -1 })
    const data = await request('https://music.163.com/weapi/song/lyric', {
      method: 'POST',
      headers: { ...neteaseHeaders, 'Content-Type': 'application/x-www-form-urlencoded' },
      body
    })
    if (data?.lrc) return data
    throw new Error('no lyric')
  } catch (e) {
    console.log('[网易云歌词爬虫失败，降级]', e.message)
    try {
      return await request(`http://localhost:3001/lyric?id=${id}`)
    } catch (e2) {
      return { lrc: { lyric: '' } }
    }
  }
}

// ─── 酷狗搜索降级 ─────────────────────────────────────────────────────────────
const kugouHeaders = {
  Referer: 'https://www.kugou.com/',
  'User-Agent': 'Mozilla/5.0 (iPhone; CPU iPhone OS 16_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Mobile/15E148'
}

async function kugouSearch(keyword, pagesize = 30) {
  // 方案1: 爬虫直接请求
  try {
    const url = `https://mobilecdn.kugou.com/api/v3/search/song?format=json&keyword=${encodeURIComponent(keyword)}&page=1&pagesize=${pagesize}&showtype=1`
    const data = await request(url, { headers: kugouHeaders })
    if (data?.data?.info?.length > 0) return data
    throw new Error('empty result')
  } catch (e) { console.log('[酷狗搜索方案1失败]', e.message) }

  // 方案2: 备用接口
  try {
    const url = `https://songsearch.kugou.com/song_search_v2?keyword=${encodeURIComponent(keyword)}&page=1&pagesize=${pagesize}&platform=WebFilter`
    const data = await request(url, { headers: { ...kugouHeaders, Referer: 'https://www.kugou.com/' } })
    if (data?.data?.lists?.length > 0) {
      // 转换格式与 mobilecdn 一致
      return { data: { info: data.data.lists.map(s => ({
        hash: s.FileHash,
        songname: s.SongName,
        singername: s.SingerName,
        album_name: s.AlbumName,
        duration: s.Duration,
        privilege: 0
      })) } }
    }
  } catch (e) { console.log('[酷狗搜索方案2失败]', e.message) }

  return { data: { info: [] } }
}

async function kugouUrl(hash) {
  // 方案1: 酷狗移动端接口
  try {
    const key = crypto.createHash('md5').update(`kgcloudv2${hash}kgcloudv2`).digest('hex')
    const url = `https://trackercdnbj.kugou.com/i/v2/?cmd=25&hash=${hash}&key=${key}&pid=2&mid=${hash}&platid=4`
    const data = await request(url, { headers: kugouHeaders })
    const playUrl = data?.url?.[0]
    if (playUrl && playUrl.startsWith('http')) return { url: playUrl, _source: 'crawler-1' }
  } catch (e) { console.log('[酷狗方案1失败]', e.message) }

  // 方案2: 酷狗 CDN 接口
  try {
    const url = `https://wwwapi.kugou.com/yy/index.php?r=play/getdata&hash=${hash}&mid=${hash}&platid=4&pid=2`
    const data = await request(url, {
      headers: { ...kugouHeaders, Referer: 'https://www.kugou.com/' }
    })
    const playUrl = data?.data?.play_url
    if (playUrl && playUrl.startsWith('http')) return { url: playUrl, _source: 'crawler-2' }
  } catch (e) { console.log('[酷狗方案2失败]', e.message) }

  // 方案3: 酷狗 app 接口
  try {
    const url = `https://m.kugou.com/app/i/getSongInfo.php?cmd=playInfo&hash=${hash}&from=mkugou`
    const data = await request(url, { headers: kugouHeaders })
    const playUrl = data?.url || data?.play_url
    if (playUrl && playUrl.startsWith('http')) return { url: playUrl, _source: 'crawler-3' }
  } catch (e) { console.log('[酷狗方案3失败]', e.message) }

  // 方案4: 降级到第三方解析 API
  console.log(`[酷狗URL] 爬虫全部失败，降级到第三方解析, hash=${hash}`)
  const fallbackApis = [
    `https://api.lolimi.cn/API/kgdg/?msg=${hash}`,
    `https://api.xingzhige.com/API/Kugou_GN/?hash=${hash}`
  ]
  for (const apiUrl of fallbackApis) {
    try {
      const data = await request(apiUrl)
      const playUrl = data?.data?.url || data?.url || data?.data?.play_url
      if (playUrl && playUrl.startsWith('http')) return { url: playUrl, _source: 'fallback-api' }
    } catch (e) { console.log('[酷狗降级API失败]', apiUrl, e.message) }
  }

  return { url: null, error: 'all methods failed' }
}

// ─── HTTP 服务 ────────────────────────────────────────────────────────────────
const server = http.createServer(async (req, res) => {
  res.setHeader('Access-Control-Allow-Origin', '*')
  res.setHeader('Access-Control-Allow-Methods', 'GET, POST, OPTIONS')
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type')
  res.setHeader('Content-Type', 'application/json; charset=utf-8')

  if (req.method === 'OPTIONS') { res.writeHead(204); res.end(); return }

  const urlObj = new URL(req.url, `http://localhost:${PORT}`)
  const path = urlObj.pathname
  const q = urlObj.searchParams

  try {
    // ── 网易云搜索 ──────────────────────────────────────────────────────────
    if (path === '/search') {
      const data = await neteaseSearch(q.get('keywords'), parseInt(q.get('limit') || '30'))
      res.end(JSON.stringify(data))
      return
    }

    // ── 网易云歌曲详情 ──────────────────────────────────────────────────────
    if (path === '/song/detail') {
      const data = await neteaseDetail(q.get('ids'))
      res.end(JSON.stringify(data))
      return
    }

    // ── 网易云播放链接 ──────────────────────────────────────────────────────
    if (path === '/song/url') {
      const data = await neteaseUrl(q.get('id'))
      res.end(JSON.stringify(data))
      return
    }

    // ── 网易云歌词 ──────────────────────────────────────────────────────────
    if (path === '/lyric') {
      const data = await neteaseLyric(q.get('id'))
      res.end(JSON.stringify(data))
      return
    }

    // ── 酷狗搜索 ────────────────────────────────────────────────────────────
    if (path === '/kugou/search') {
      const data = await kugouSearch(q.get('keyword'), parseInt(q.get('pagesize') || '30'))
      res.end(JSON.stringify(data))
      return
    }

    // ── 酷狗播放链接 ────────────────────────────────────────────────────────
    if (path === '/kugou/url') {
      const data = await kugouUrl(q.get('hash'))
      res.end(JSON.stringify(data))
      return
    }

    res.writeHead(404)
    res.end(JSON.stringify({ error: 'not found', path }))
  } catch (e) {
    console.error('[服务错误]', e)
    res.writeHead(500)
    res.end(JSON.stringify({ error: e.message }))
  }
})

server.listen(PORT, () => {
  console.log(`🎵 音乐爬虫服务已启动: http://localhost:${PORT}`)
  console.log('接口列表:')
  console.log('  GET /search?keywords=xxx&limit=30          网易云搜索')
  console.log('  GET /song/detail?ids=xxx,xxx               网易云歌曲详情')
  console.log('  GET /song/url?id=xxx                       网易云播放链接')
  console.log('  GET /lyric?id=xxx                          网易云歌词')
  console.log('  GET /kugou/search?keyword=xxx&pagesize=30  酷狗搜索')
  console.log('  GET /kugou/url?hash=xxx                    酷狗播放链接')
})
