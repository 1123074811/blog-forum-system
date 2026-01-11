import axios from 'axios'

// 支持的平台
export const PLATFORMS = {
  netease: { name: '网易云', icon: '🎵' },
  qq: { name: 'QQ音乐', icon: '🎶' },
  kugou: { name: '酷狗', icon: '🎤' }
}

// 网易云音乐 API
const musicApi = axios.create({
  baseURL: '/music-api',
  timeout: 15000
})
musicApi.interceptors.response.use(res => res.data, err => Promise.reject(err))

// 多平台搜索
export const searchSongs = async (keywords, limit = 30, platform = 'netease') => {
  if (platform === 'netease') {
    const res = await musicApi.get('/search', { params: { keywords, limit } })
    const songs = res.result?.songs || []
    if (songs.length > 0) {
      try {
        const ids = songs.slice(0, 20).map(s => s.id).join(',')
        const detail = await musicApi.get('/song/detail', { params: { ids } })
        const detailMap = {}
        detail.songs?.forEach(s => { detailMap[s.id] = s })
        songs.forEach(s => {
          if (detailMap[s.id]?.al?.picUrl) {
            s.al = detailMap[s.id].al
          }
        })
      } catch (e) { console.log('获取详情失败:', e) }
    }
    return { result: { songs } }
  }

  if (platform === 'kugou') {
    const res = await axios.get('/kugou-api/api/v3/search/song', {
      params: { keyword: keywords, page: 1, pagesize: limit }
    })
    const list = res.data?.data?.info || []
    // 标记VIP歌曲：privilege为8或pay_type不为0的都是VIP
    const songs = list.map(s => ({
      id: s.hash,
      name: s.songname,
      artists: [{ name: s.singername }],
      album: { 
        name: s.album_name, 
        picUrl: s.trans_param?.union_cover?.replace('{size}', '240') || s.imgUrl?.replace('{size}', '240')
      },
      duration: s.duration * 1000,
      _platform: 'kugou',
      _hash: s.hash,
      _isVip: s.privilege === 8 || (s.pay_type !== undefined && s.pay_type !== 0)
    }))
    return {
      result: { songs }
    }
  }

  if (platform === 'qq') {
    const res = await axios.get('/qq-api/soso/fcgi-bin/client_search_cp', {
      params: { w: keywords, p: 1, n: limit, format: 'json' }
    })
    const list = res.data?.data?.song?.list || []
    return {
      result: {
        songs: list
          .filter(s => s.pay?.payplay === 0)
          .map(s => ({
            id: s.songmid,
            name: s.songname,
            artists: s.singer?.map(x => ({ name: x.name })) || [],
            album: { name: s.albumname, mid: s.albummid, picUrl: s.albummid ? `https://y.gtimg.cn/music/photo_new/T002R300x300M000${s.albummid}.jpg` : '' },
            duration: s.interval * 1000,
            _platform: 'qq',
            _mid: s.songmid
          }))
      }
    }
  }

  return { result: { songs: [] } }
}

// 获取播放链接
export const getSongUrl = async (id, platform = 'netease') => {
  if (platform === 'netease') {
    return musicApi.get('/song/url', { params: { id } })
  }

  if (platform === 'kugou') {
    try {
      // 酷狗官方接口
      const res = await axios.get('/kugou-play/app/i/getSongInfo.php', {
        params: { cmd: 'playInfo', hash: id }
      })
      
      // 检查各种错误情况
      const data = res.data
      
      // 1. 检查明确VIP标识
      if (data?.error_code === 30000 || data?.status === 2 || data?.errcode === 30000) {
        return { data: [{ url: null, vip: true, message: '该歌曲为VIP专享，无法播放' }] }
      }
      
      // 2. 检查错误信息
      if (data?.error || data?.err_code || data?.errcode) {
        return { data: [{ url: null, vip: true, message: '该歌曲需要VIP权限或暂时无法播放' }] }
      }
      
      const url = data?.url || data?.play_url || data?.backup_url
      
      // 3. 检查URL是否有效
      if (!url || url.trim() === '' || url === 'null') {
        return { data: [{ url: null, vip: true, message: '该歌曲需要VIP权限或暂时无法播放' }] }
      }
      
      // 4. 检查URL格式是否正确（应该是http/https开头）
      if (!url.startsWith('http://') && !url.startsWith('https://')) {
        return { data: [{ url: null, vip: true, message: '该歌曲链接格式错误，可能需要VIP权限' }] }
      }
      
      return { data: [{ url }] }
    } catch (e) { 
      console.error('酷狗接口错误:', e.message)
      return { data: [{ url: null, error: '获取播放链接失败' }] }
    }
  }

  if (platform === 'qq') {
    try {
      const guid = Math.floor(Math.random() * 10000000000)
      const res = await axios.get('/qq-play/cgi-bin/musicu.fcg', {
        params: {
          format: 'json',
          data: JSON.stringify({
            req_0: {
              module: 'vkey.GetVkeyServer',
              method: 'CgiGetVkey',
              param: { guid: String(guid), songmid: [id], songtype: [0], uin: '0', loginflag: 1, platform: '20' }
            }
          })
        }
      })
      const purl = res.data?.req_0?.data?.midurlinfo?.[0]?.purl
      if (purl) {
        const sip = res.data?.req_0?.data?.sip?.[0] || 'https://ws.stream.qqmusic.qq.com/'
        return { data: [{ url: sip + purl }] }
      }
    } catch (e) { console.log('QQ方案1失败:', e.message) }
    return { data: [{ url: null }] }
  }

  return { data: [{ url: null }] }
}

// 获取歌曲详情
export const getSongDetail = (ids) => musicApi.get('/song/detail', { params: { ids } })

// 获取歌词
export const getLyric = (id) => musicApi.get('/lyric', { params: { id } })

export default musicApi
