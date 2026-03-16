import axios from 'axios'

export const PLATFORMS = {
  bilibili: { name: 'B站', icon: '📺' },
}

const api = axios.create({ baseURL: '/music-api', timeout: 15000 })
api.interceptors.response.use((res) => res.data, (err) => Promise.reject(err))

export const searchSongs = async (keywords, limit = 20) => {
  const res = await api.get('/search', { params: { keywords, limit } })
  return { result: { songs: res.result?.songs || [] } }
}

export const getSongUrl = async (id) => {
  return api.get('/song/url', { params: { id } })
}

export const getSongDetail = async (ids) => {
  const idsParam = Array.isArray(ids) ? ids.join(',') : ids
  return api.get('/song/detail', { params: { ids: idsParam } })
}

export const getLyric = async (id) => {
  return api.get('/lyric', { params: { id } })
}

export default api
