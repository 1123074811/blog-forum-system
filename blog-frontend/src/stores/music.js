import { defineStore } from 'pinia'
import { ref, computed, watch } from 'vue'
import { getSongUrl, getSongDetail } from '@/api/music'

// 从 localStorage 读取保存的状态
const loadState = () => {
  try {
    const saved = localStorage.getItem('music-player-state')
    return saved ? JSON.parse(saved) : null
  } catch { return null }
}

const savedState = loadState()

export const useMusicStore = defineStore('music', () => {
  // 播放列表
  const playlist = ref(savedState?.playlist || [])
  // 当前播放索引
  const currentIndex = ref(savedState?.currentIndex ?? -1)
  // 播放状态
  const isPlaying = ref(false)
  // 当前播放时间
  const currentTime = ref(0)
  // 总时长
  const duration = ref(0)
  // 音量
  const volume = ref(savedState?.volume ?? 0.7)
  // 播放模式: 0-顺序 1-随机 2-单曲循环
  const playMode = ref(savedState?.playMode ?? 0)
  // 显示播放器
  const showPlayer = ref(savedState?.playlist?.length > 0)
  // 显示播放列表
  const showPlaylist = ref(false)

  // 保存状态到 localStorage
  const saveState = () => {
    localStorage.setItem('music-player-state', JSON.stringify({
      playlist: playlist.value,
      currentIndex: currentIndex.value,
      volume: volume.value,
      playMode: playMode.value
    }))
  }

  // 监听变化自动保存
  watch([playlist, currentIndex, volume, playMode], saveState, { deep: true })

  // 当前歌曲
  const currentSong = computed(() => {
    return playlist.value[currentIndex.value] || null
  })

  // Audio 实例
  let audio = null

  // 初始化 Audio
  const initAudio = () => {
    if (!audio) {
      audio = new Audio()
      audio.volume = volume.value

      audio.addEventListener('timeupdate', () => {
        currentTime.value = audio.currentTime
      })

      audio.addEventListener('loadedmetadata', () => {
        duration.value = audio.duration
      })

      audio.addEventListener('ended', () => {
        playNext()
      })

      audio.addEventListener('error', () => {
        console.error('音频加载失败')
        playNext()
      })
    }
    return audio
  }

  // 恢复上次播放的歌曲（页面刷新后调用）
  const restorePlayback = async () => {
    if (currentIndex.value >= 0 && playlist.value[currentIndex.value]) {
      const song = playlist.value[currentIndex.value]
      initAudio()
      try {
        const res = await getSongUrl(song.id)
        const url = res.data?.[0]?.url
        if (url) {
          audio.src = url
          audio.load()
        }
      } catch (e) {
        console.error('恢复播放失败:', e)
      }
    }
  }

  // 播放歌曲
  const playSong = async (song, index) => {
    initAudio()

    try {
      // 获取播放链接
      const res = await getSongUrl(song.id)
      const url = res.data?.[0]?.url

      if (!url) {
        console.error('无法获取播放链接')
        return
      }

      audio.src = url
      audio.play()
      isPlaying.value = true
      currentIndex.value = index
      showPlayer.value = true
    } catch (error) {
      console.error('播放失败:', error)
    }
  }

  // 播放/暂停
  const togglePlay = () => {
    if (!audio || !currentSong.value) return

    if (isPlaying.value) {
      audio.pause()
    } else {
      audio.play()
    }
    isPlaying.value = !isPlaying.value
  }

  // 上一曲
  const playPrev = () => {
    if (playlist.value.length === 0) return

    let index = currentIndex.value - 1
    if (index < 0) index = playlist.value.length - 1

    playSong(playlist.value[index], index)
  }

  // 下一曲
  const playNext = () => {
    if (playlist.value.length === 0) return

    let index
    if (playMode.value === 1) {
      // 随机播放
      index = Math.floor(Math.random() * playlist.value.length)
    } else if (playMode.value === 2) {
      // 单曲循环
      index = currentIndex.value
    } else {
      // 顺序播放
      index = currentIndex.value + 1
      if (index >= playlist.value.length) index = 0
    }

    playSong(playlist.value[index], index)
  }

  // 跳转进度
  const seekTo = (time) => {
    if (audio) {
      audio.currentTime = time
    }
  }

  // 设置音量
  const setVolume = (val) => {
    volume.value = val
    if (audio) {
      audio.volume = val
    }
  }

  // 切换播放模式
  const togglePlayMode = () => {
    playMode.value = (playMode.value + 1) % 3
  }

  // 添加到播放列表并播放
  const addAndPlay = async (song) => {
    // 检查是否已在列表中
    const existIndex = playlist.value.findIndex(s => s.id === song.id)
    if (existIndex !== -1) {
      playSong(playlist.value[existIndex], existIndex)
      return
    }

    // 添加到列表
    playlist.value.push(song)
    playSong(song, playlist.value.length - 1)
  }

  // 添加多首歌曲到播放列表
  const addSongs = (songs) => {
    songs.forEach(song => {
      if (!playlist.value.find(s => s.id === song.id)) {
        playlist.value.push(song)
      }
    })
  }

  // 从播放列表移除
  const removeSong = (index) => {
    playlist.value.splice(index, 1)
    if (index < currentIndex.value) {
      currentIndex.value--
    } else if (index === currentIndex.value) {
      if (playlist.value.length > 0) {
        const newIndex = Math.min(index, playlist.value.length - 1)
        playSong(playlist.value[newIndex], newIndex)
      } else {
        currentIndex.value = -1
        isPlaying.value = false
        if (audio) audio.pause()
      }
    }
  }

  // 清空播放列表
  const clearPlaylist = () => {
    playlist.value = []
    currentIndex.value = -1
    isPlaying.value = false
    if (audio) {
      audio.pause()
      audio.src = ''
    }
  }

  return {
    playlist,
    currentIndex,
    currentSong,
    isPlaying,
    currentTime,
    duration,
    volume,
    playMode,
    showPlayer,
    showPlaylist,
    playSong,
    togglePlay,
    playPrev,
    playNext,
    seekTo,
    setVolume,
    togglePlayMode,
    addAndPlay,
    addSongs,
    removeSong,
    clearPlaylist,
    restorePlayback
  }
})
