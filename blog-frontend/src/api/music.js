import axios from 'axios'

// 支持的平台
export const PLATFORMS = {
  netease: { name: '网易云', icon: '🎵' },
  kugou:   { name: '酷狗',   icon: '🎤' },
  qq:      { name: 'QQ音乐', icon: '🎶' }
}

// 爬虫服务（/music-api → localhost:3000）
const crawlerApi = axios.create({ baseURL: '/music-api', timeout: 15000 })
crawlerApi.interceptors.response.use(res => res.data, err => Promise.reject(err))

// NeteaseCloudMusicApi 降级备用（/netease-api → localhost:3001）
const neteaseApi = axios.create({ baseURL: '/netease-api', timeout: 15000 })
neteaseApi.interceptors.response.use(res => res.data, err => Promise.reject(err))

// ─── 搜索 ─────────────────────────────────────────────────────────────────────
export const searchSongs = async (keywords, limit = 30, platform = 'netease') => {
  if (platform === 'netease') {
    let songs = []
    try {
      const res = await crawlerApi.get('/search', { params: { keywords, limit } })
      songs = res.result?.songs || []
    } catch (e) {
      console.warn('[网易云搜索] 爬虫失败，降级到 NeteaseCloudMusicApi', e.message)
      try {
        const res = await neteaseApi.get('/search', { params: { keywords, limit } })
        songs = res.result?.songs || []
      } catch (e2) { console.error('[网易云搜索] 降级也失败', e2.message) }
    }
    if (songs.length > 0) {
      try {
        const ids = songs.slice(0, 20).map(s => s.id).join(',')
        const detail = await crawlerApi.get('/song/detail', { params: { ids } })
        const map = {}
        detail.songs?.forEach(s => { map[s.id] = s })
        songs.forEach(s => { if (map[s.id]?.al?.picUrl) s.al = map[s.id].al })
      } catch (e) { /* 封面失败不影响结果 */ }
    }
    return { result: { songs } }
  }

  if (platform === 'kugou') {
    try {
      const res = await crawlerApi.get('/kugou/search', { params: { keyword: keywords, pagesize: limit } })
      const list = res.data?.info || []
      if (list.length > 0) {
        return { result: { songs: list.map(s => ({
          id: s.hash, name: s.songname,
          artists: [{ name: s.singername }],
          album: { name: s.album_name, picUrl: (s.trans_param?.union_cover || s.imgUrl || '').replace('{size}', '240') },
          duration: s.duration * 1000,
          _platform: 'kugou', _hash: s.hash,
          _isVip: s.privilege === 8 || (s.pay_type !== undefined && s.pay_type !== 0)
        })) } }
      }
    } catch (e) { console.warn('[酷狗搜索] 爬虫失败，降级', e.message) }
    // 降级：直接走 vite proxy 打酷狗官方接口
    try {
      const res = await axios.get('/kugou-api/api/v3/search/song', { params: { keyword: keywords, page: 1, pagesize: limit } })
      const list = res.data?.data?.info || []
      return { result: { songs: list.map(s => ({
        id: s.hash, name: s.songname,
        artists: [{ name: s.singername }],
        album: { name: s.album_name, picUrl: (s.trans_param?.union_cover || s.imgUrl || '').replace('{size}', '240') },
        duration: s.duration * 1000,
        _platform: 'kugou', _hash: s.hash,
        _isVip: s.privilege === 8 || (s.pay_type !== undefined && s.pay_type !== 0)
      })) } }
    } catch (e2) { console.error('[酷狗搜索降级失败]', e2.message) }
    return { result: { songs: [] } }
  }

  if (platform === 'qq') {
    try {
      const res = await axios.get('/qq-api/soso/fcgi-bin/client_search_cp', { params: { w: keywords, p: 1, n: limit, format: 'json' } })
      const list = res.data?.data?.song?.list || []
      return { result: { songs: list.filter(s => s.pay?.payplay === 0).map(s => ({
        id: s.songmid, name: s.songname,
        artists: s.singer?.map(x => ({ name: x.name })) || [],
        album: { name: s.albumname, mid: s.albummid, picUrl: s.albummid ? `https://y.gtimg.cn/music/photo_new/T002R300x300M000${s.albummid}.jpg` : '' },
        duration: s.interval * 1000, _platform: 'qq', _mid: s.songmid
      })) } }
    } catch (e) { console.error('[QQ音乐搜索失败]', e.message) }
    return { result: { songs: [] } }
  }

  return { result: { songs: [] } }
}

// ─── 播放链接 ─────────────────────────────────────────────────────────────────
export const getSongUrl = async (id, platform = 'netease') => {
  if (platform === 'netease') {
    try {
      const res = await crawlerApi.get('/song/url', { params: { id } })
      if (res.data?.[0]?.url) return res
    } catch (e) { console.warn('[网易云URL] 爬虫失败，降级', e.message) }
    return neteaseApi.get('/song/url', { params: { id } })
  }

  if (platform === 'kugou') {
    // 爬虫优先（服务内部已有多方案 + 第三方解析降级）
    try {
      const res = await crawlerApi.get('/kugou/url', { params: { hash: id } })
      if (res.url?.startsWith('http')) return { data: [{ url: res.url }] }
    } catch (e) { console.warn('[酷狗URL] 爬虫失败，降级到官方接口', e.message) }
    // 最终降级：vite proxy 打酷狗官方接口
    try {
      const res = await axios.get('/kugou-play/app/i/getSongInfo.php', { params: { cmd: 'playInfo', hash: id } })
      const url = res.data?.url || res.data?.play_url
      if (url?.startsWith('http')) return { data: [{ url }] }
    } catch (e) { console.warn('[酷狗URL] 官方接口也失败', e.message) }
    return { data: [{ url: null, vip: true, message: '该歌曲需要VIP权限或暂时无法播放' }] }
  }

  if (platform === 'qq') {
    try {
      const guid = Math.floor(Math.random() * 10000000000)
      const res = await axios.get('/qq-play/cgi-bin/musicu.fcg', {
        params: { format: 'json', data: JSON.stringify({ req_0: { module: 'vkey.GetVkeyServer', method: 'CgiGetVkey',
          param: { guid: String(guid), songmid: [id], songtype: [0], uin: '0', loginflag: 1, platform: '20' } } }) }
      })
      const purl = res.data?.req_0?.data?.midurlinfo?.[0]?.purl
      if (purl) {
        const sip = res.data?.req_0?.data?.sip?.[0] || 'https://ws.stream.qqmusic.qq.com/'
        return { data: [{ url: sip + purl }] }
      }
    } catch (e) { console.log('[QQ音乐URL失败]', e.message) }
    return { data: [{ url: null }] }
  }

  return { data: [{ url: null }] }
}

// ─── 其他接口（爬虫优先，降级到 NeteaseCloudMusicApi）─────────────────────────
export const getSongDetail = async (ids) => {
  try { return await crawlerApi.get('/song/detail', { params: { ids } }) }
  catch (e) { return neteaseApi.get('/song/detail', { params: { ids } }) }
}

export const getLyric = async (id) => {
  try { return await crawlerApi.get('/lyric', { params: { id } }) }
  catch (e) { return neteaseApi.get('/lyric', { params: { id } }) }
}

export default crawlerApi
