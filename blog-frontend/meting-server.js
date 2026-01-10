import http from 'http'
import https from 'https'
import { URL } from 'url'

const PORT = 3001

// HTTPS 请求封装
const fetch = (url, options = {}) => {
  return new Promise((resolve, reject) => {
    const urlObj = new URL(url)
    const client = urlObj.protocol === 'https:' ? https : http
    const req = client.request(url, {
      method: 'GET',
      headers: {
        'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36',
        'Accept': '*/*',
        'Cookie': options.cookie || '',
        ...options.headers
      }
    }, (res) => {
      let data = ''
      res.on('data', chunk => data += chunk)
      res.on('end', () => {
        try { resolve(JSON.parse(data)) }
        catch { resolve(data) }
      })
    })
    req.on('error', reject)
    req.setTimeout(10000, () => { req.destroy(); reject(new Error('timeout')) })
    req.end()
  })
}

const server = http.createServer(async (req, res) => {
  res.setHeader('Access-Control-Allow-Origin', '*')
  res.setHeader('Content-Type', 'application/json; charset=utf-8')

  const url = new URL(req.url, `http://localhost:${PORT}`)
  const pathname = url.pathname

  // 酷狗播放链接
  if (pathname === '/api/kugou/url') {
    const hash = url.searchParams.get('hash')
    if (!hash) {
      res.end(JSON.stringify({ error: 'missing hash' }))
      return
    }
    try {
      // 使用第三方解析API
      const apis = [
        `https://api.lolimi.cn/API/kgdg/?msg=${hash}`,
        `https://api.xingzhige.com/API/Kugou_GN/?hash=${hash}`
      ]

      for (const api of apis) {
        try {
          const data = await fetch(api)
          const playUrl = data?.data?.url || data?.url || data?.data?.play_url
          if (playUrl) {
            res.end(JSON.stringify({ url: playUrl }))
            return
          }
        } catch (e) { console.log('API failed:', api, e.message) }
      }

      res.end(JSON.stringify({ url: null, error: 'all APIs failed' }))
    } catch (e) {
      res.end(JSON.stringify({ error: e.message }))
    }
    return
  }

  // QQ音乐播放链接
  if (pathname === '/api/qq/url') {
    const id = url.searchParams.get('id')
    if (!id) {
      res.end(JSON.stringify({ error: 'missing id' }))
      return
    }
    try {
      // 方案1: QQ音乐官方免费接口
      const guid = Math.floor(Math.random() * 10000000000)
      const officialUrl = `https://u.y.qq.com/cgi-bin/musicu.fcg?format=json&data=${encodeURIComponent(JSON.stringify({
        req_0: {
          module: 'vkey.GetVkeyServer',
          method: 'CgiGetVkey',
          param: { guid: String(guid), songmid: [id], songtype: [0], uin: '0', loginflag: 1, platform: '20' }
        }
      }))}`

      try {
        const data = await fetch(officialUrl)
        const purl = data?.req_0?.data?.midurlinfo?.[0]?.purl
        const sip = data?.req_0?.data?.sip?.[0] || 'https://ws.stream.qqmusic.qq.com/'
        if (purl) {
          res.end(JSON.stringify({ url: sip + purl }))
          return
        }
      } catch (e) { console.log('QQ官方接口失败:', e.message) }

      // 方案2: 备用第三方API
      const apis = [
        `https://api.lolimi.cn/API/qqdg/?msg=${id}`,
        `https://api.xingzhige.com/API/QQmusicVIP/?mid=${id}`
      ]
      for (const api of apis) {
        try {
          const data = await fetch(api)
          const playUrl = data?.data?.url || data?.url || data?.data?.music
          if (playUrl) {
            res.end(JSON.stringify({ url: playUrl }))
            return
          }
        } catch (e) { console.log('API failed:', api, e.message) }
      }

      res.end(JSON.stringify({ url: null, error: 'all APIs failed' }))
    } catch (e) {
      res.end(JSON.stringify({ error: e.message }))
    }
    return
  }

  res.end(JSON.stringify({ error: 'unknown endpoint' }))
})

server.listen(PORT, () => {
  console.log(`Music API running at http://localhost:${PORT}`)
  console.log('Endpoints: /api/kugou/url?hash=xxx, /api/qq/url?id=xxx')
})
