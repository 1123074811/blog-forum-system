import axios from 'axios'

// 网易云音乐 API 实例（独立于主 API）
const musicApi = axios.create({
  baseURL: '/music-api',
  timeout: 15000
})

musicApi.interceptors.response.use(
  response => response.data,
  error => Promise.reject(error)
)

// 搜索歌曲
export const searchSongs = (keywords, limit = 30) => {
  return musicApi.get('/search', { params: { keywords, limit } })
}

// 获取歌曲播放链接
export const getSongUrl = (id) => {
  return musicApi.get('/song/url', { params: { id } })
}

// 获取歌曲详情
export const getSongDetail = (ids) => {
  return musicApi.get('/song/detail', { params: { ids } })
}

// 获取歌词
export const getLyric = (id) => {
  return musicApi.get('/lyric', { params: { id } })
}

// 获取热门歌单
export const getTopPlaylist = (limit = 10) => {
  return musicApi.get('/top/playlist', { params: { limit } })
}

// 获取歌单详情
export const getPlaylistDetail = (id) => {
  return musicApi.get('/playlist/detail', { params: { id } })
}

// 获取推荐新歌
export const getNewSongs = () => {
  return musicApi.get('/personalized/newsong')
}

export default musicApi
