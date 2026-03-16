import http from 'http'
import { URL } from 'url'
import Meting from '@meting/core'

const PORT = Number(process.env.METING_PORT || process.env.PORT || 3000)

const PLATFORM_MAP = {
  netease: 'netease',
  qq: 'tencent',
  kugou: 'kugou'
}

const getServerCode = (platform) => PLATFORM_MAP[platform] || 'netease'

const json = (res, statusCode, payload) => {
  res.statusCode = statusCode
  res.setHeader('Access-Control-Allow-Origin', '*')
  res.setHeader('Access-Control-Allow-Methods', 'GET,OPTIONS')
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type')
  res.setHeader('Content-Type', 'application/json; charset=utf-8')
  res.end(JSON.stringify(payload))
}

const normalizeSong = (song, platform = 'netease') => {
  const artists = Array.isArray(song.artist)
    ? song.artist.map((name) => ({ name }))
    : (song.artists || song.ar || [])

  const cover = song.pic || song.picUrl || song.cover || ''
  const albumName = song.album || song.albumname || song.al?.name || ''

  return {
    id: String(song.id || song.url_id || ''),
    name: song.name || '',
    artists,
    ar: artists,
    album: {
      name: albumName,
      picUrl: cover
    },
    al: {
      name: albumName,
      picUrl: cover
    },
    duration: Number(song.duration || 0),
    _platform: platform,
    _urlId: String(song.url_id || song.id || ''),
    _lyricId: String(song.lyric_id || song.id || ''),
    _raw: song
  }
}

const callMeting = async (platform, action, ...args) => {
  const meting = new Meting(getServerCode(platform))
  meting.format(true)
  const raw = await meting[action](...args)
  return typeof raw === 'string' ? JSON.parse(raw) : raw
}

const server = http.createServer(async (req, res) => {
  if (req.method === 'OPTIONS') {
    json(res, 204, {})
    return
  }

  const requestUrl = new URL(req.url, `http://localhost:${PORT}`)
  const path = requestUrl.pathname
  const platform = requestUrl.searchParams.get('platform') || 'netease'

  try {
    if (path === '/search') {
      const keywords = requestUrl.searchParams.get('keywords') || ''
      const limit = Number(requestUrl.searchParams.get('limit') || 30)
      if (!keywords.trim()) {
        json(res, 400, { error: 'missing keywords' })
        return
      }
      const songs = await callMeting(platform, 'search', keywords, { page: 1, limit, type: 1 })
      const normalized = Array.isArray(songs) ? songs.map((song) => normalizeSong(song, platform)) : []
      json(res, 200, { result: { songs: normalized } })
      return
    }

    if (path === '/song/detail') {
      const ids = (requestUrl.searchParams.get('ids') || '')
        .split(',')
        .map((x) => x.trim())
        .filter(Boolean)
      if (ids.length === 0) {
        json(res, 400, { error: 'missing ids' })
        return
      }
      const songs = await Promise.all(ids.map(async (id) => {
        const detail = await callMeting(platform, 'song', id)
        const first = Array.isArray(detail) ? detail[0] : detail
        return normalizeSong(first || { id }, platform)
      }))
      json(res, 200, { songs })
      return
    }

    if (path === '/song/url') {
      const id = requestUrl.searchParams.get('id')
      if (!id) {
        json(res, 400, { error: 'missing id' })
        return
      }
      const urls = await callMeting(platform, 'url', id, 320)
      const first = Array.isArray(urls) ? urls[0] : urls
      const playUrl = first?.url || null
      if (!playUrl) {
        json(res, 200, {
          data: [{
            url: null,
            vip: true,
            message: '该歌曲可能需要会员权限或当前源不可用'
          }]
        })
        return
      }
      json(res, 200, { data: [{ url: playUrl }] })
      return
    }

    if (path === '/lyric') {
      const id = requestUrl.searchParams.get('id')
      if (!id) {
        json(res, 400, { error: 'missing id' })
        return
      }
      const lyric = await callMeting(platform, 'lyric', id)
      const text = typeof lyric === 'string'
        ? lyric
        : (lyric?.lyric || lyric?.lrc?.lyric || '')
      json(res, 200, { lrc: { lyric: text } })
      return
    }

    json(res, 404, { error: 'unknown endpoint' })
  } catch (error) {
    console.error(`[meting-server] ${path} failed:`, error?.message || error)
    json(res, 500, { error: error?.message || 'music api failed' })
  }
})

server.listen(PORT, () => {
  console.log(`Meting music API running at http://localhost:${PORT}`)
  console.log('Endpoints: /search /song/detail /song/url /lyric')
})
