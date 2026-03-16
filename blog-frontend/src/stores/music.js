import { defineStore } from 'pinia'
import { ref, computed, watch } from 'vue'
import { getSongUrl, getSongDetail } from '@/api/music'
import { ElNotification } from 'element-plus'

// 同一时间只允许一条失败通知，防止堆叠
let activeFailNotification = null
function showFailNotification(title, message, type = 'error') {
  if (activeFailNotification) {
    activeFailNotification.close()
  }
  activeFailNotification = ElNotification({
    title,
    message,
    type,
    position: 'top-left',
    offset: 50,
    onClose: () => { activeFailNotification = null }
  })
}

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
  // 播放状态（刷新后保持停止状态）
  const isPlaying = ref(false)
  // 当前播放时间（保存进度）
  const currentTime = ref(savedState?.currentTime ?? 0)
  // 总时长
  const duration = ref(savedState?.duration ?? 0)
  // 音量
  const volume = ref(savedState?.volume ?? 0.7)
  // 播放模式: 0-顺序 1-随机 2-单曲循环
  const playMode = ref(savedState?.playMode ?? 0)
  // 显示播放器（用 counter 触发，每次点击递增，watch 总能检测到变化）
  const showPlayer = ref(0)
  // 显示播放列表
  const showPlaylist = ref(false)

  // 保存状态到 localStorage（包括播放进度）
  const saveState = () => {
    localStorage.setItem('music-player-state', JSON.stringify({
      playlist: playlist.value,
      currentIndex: currentIndex.value,
      currentTime: currentTime.value,
      duration: duration.value,
      volume: volume.value,
      playMode: playMode.value
    }))
  }

  // 监听变化自动保存
  watch([playlist, currentIndex, volume, playMode], saveState, { deep: true })
  
  // 监听播放进度变化（防抖保存，每秒1秒保存一次）
  let saveProgressTimer = null
  watch(currentTime, () => {
    if (saveProgressTimer) clearTimeout(saveProgressTimer)
    saveProgressTimer = setTimeout(saveState, 1000)
  })

  // 当前歌曲
  const currentSong = computed(() => {
    return playlist.value[currentIndex.value] || null
  })

  // 连续失败计数，防止无限跳曲
  let consecutiveFails = 0
  const MAX_CONSECUTIVE_FAILS = 3

  // Audio 实例
  let audio = null

  // 初始化 Audio
  const initAudio = () => {
    if (!audio) {
      audio = new Audio()
      audio.volume = volume.value

      audio.addEventListener('timeupdate', () => {
        currentTime.value = audio.currentTime
        // 实时保存进度（防抖）
        if (saveProgressTimer) clearTimeout(saveProgressTimer)
        saveProgressTimer = setTimeout(saveState, 1000)
      })

      audio.addEventListener('loadedmetadata', () => {
        duration.value = audio.duration
        saveState() // 保存总时长
      })

      audio.addEventListener('ended', () => {
        playNext()
      })

      audio.addEventListener('error', (e) => {
        console.error('音频加载失败', audio.error)
        isPlaying.value = false
        
        let errorMsg = '音频加载失败'
        if (audio.error) {
          switch (audio.error.code) {
            case 1: errorMsg = '播放被中止'; break
            case 2: errorMsg = '网络错误，无法加载歌曲'; break
            case 3: errorMsg = '歌曲解码失败'; break
            case 4: errorMsg = '该歌曲格式不支持'; break
          }
        }

        consecutiveFails++
        const canSkip = playlist.value.length > 1 && consecutiveFails <= MAX_CONSECUTIVE_FAILS

        showFailNotification(
          '播放失败',
          canSkip ? `${errorMsg}，2秒后将自动播放下一首` : errorMsg,
          'error'
        )

        if (canSkip) {
          setTimeout(() => playNext(), 2000)
        } else {
          consecutiveFails = 0
        }
      })
    }
    return audio
  }

  // 恢复上次播放的歌曲（页面刷新后调用）—— 只恢复状态，不自动播放，避免过期 URL 报错
  const restorePlayback = async () => {
    // B站音频流 URL 有时效性，页面刷新后不自动恢复播放，只保留播放列表和进度状态
    // 用户手动点击播放时会重新获取 URL
    if (currentIndex.value >= 0 && playlist.value[currentIndex.value]) {
      initAudio()
      // 恢复音量
      if (audio) audio.volume = volume.value
    }
  }

  // 播放歌曲
  const playSong = async (song, index) => {
    initAudio()

    try {
      const res = await getSongUrl(song.id)
      const urlData = res.data?.[0]
      const url = urlData?.url

      if (!url) {
        consecutiveFails++
        const canSkip = playlist.value.length > 1 && consecutiveFails <= MAX_CONSECUTIVE_FAILS
        const baseMsg = urlData?.message || '无法获取播放链接，该视频可能有版权限制'
        showFailNotification(
          '播放失败',
          canSkip ? `${baseMsg}，2秒后将自动播放下一首` : baseMsg,
          'warning'
        )
        if (canSkip) setTimeout(() => playNext(), 2000)
        else consecutiveFails = 0
        return
      }

      audio.src = url
      audio.play().catch(err => {
        console.error('播放错误:', err)
        isPlaying.value = false
        consecutiveFails++
        const canSkip = playlist.value.length > 1 && consecutiveFails <= MAX_CONSECUTIVE_FAILS
        showFailNotification(
          '播放失败',
          canSkip ? '播放失败，2秒后将自动播放下一首' : '播放失败，请手动选择歌曲'
        )
        if (canSkip) setTimeout(() => playNext(), 2000)
        else consecutiveFails = 0
      })
      isPlaying.value = true
      currentIndex.value = index
      consecutiveFails = 0 // 成功播放，重置失败计数
    } catch (error) {
      console.error('播放失败:', error)
      consecutiveFails++
      const canSkip = playlist.value.length > 1 && consecutiveFails <= MAX_CONSECUTIVE_FAILS
      showFailNotification(
        '播放失败',
        canSkip ? '请求失败，2秒后将自动播放下一首' : '播放失败，请手动选择歌曲'
      )
      if (canSkip) setTimeout(() => playNext(), 2000)
      else consecutiveFails = 0
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
