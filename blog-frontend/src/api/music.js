import axios from 'axios'

export const PLATFORMS = {
  netease: { name: '网易云', icon: '🎵' },
  kugou: { name: '酷狗', icon: '🎤' },
  qq: { name: 'QQ音乐', icon: '🎶' }
}

const metingApi = axios.create({ baseURL: '/music-api', timeout: 15000 })
metingApi.interceptors.response.use((res) => res.data, (err) => Promise.reject(err))

const normalizeSong = (song, platform = 'netease') => {
  const artists = song.artists || song.ar || []
  const album = song.album || song.al || { name: '', picUrl: '' }

  return {
    ...song,
    id: song.id,
    name: song.name,
    artists,
    ar: artists,
    album,
    al: album,
    _platform: song._platform || platform,
    _isVip: !!song._isVip
  }
}

export const searchSongs = async (keywords, limit = 30, platform = 'netease') => {
  const res = await metingApi.get('/search', { params: { keywords, limit, platform } })
  const songs = (res.result?.songs || []).map((song) => normalizeSong(song, platform))
  return { result: { songs } }
}

export const getSongUrl = async (id, platform = 'netease') => {
  return metingApi.get('/song/url', { params: { id, platform } })
}

export const getSongDetail = async (ids, platform = 'netease') => {
  const idsParam = Array.isArray(ids) ? ids.join(',') : ids
  const res = await metingApi.get('/song/detail', { params: { ids: idsParam, platform } })
  const songs = (res.songs || []).map((song) => normalizeSong(song, platform))
  return { songs }
}

export const getLyric = async (id, platform = 'netease') => {
  return metingApi.get('/lyric', { params: { id, platform } })
}

export default metingApi
