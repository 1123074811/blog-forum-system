<template>
  <div style="display:contents">
  <div class="music-player-wrapper">
    <!-- 桌面端触发器（左下角悬浮按钮，移动端隐藏） -->
    <div class="player-trigger" :class="{ expanded }" @click="expanded = !expanded">
      <div class="trigger-icon">
        <svg viewBox="0 0 24 24" width="20" height="20" fill="currentColor">
          <path d="M12 3v9.26c-.5-.17-1-.26-1.5-.26C8 12 6 14 6 16.5S8 21 10.5 21s4.5-2 4.5-4.5V6h4V3h-7z"/>
        </svg>
      </div>
      <div v-if="currentSong" class="trigger-wave" :class="{ playing: musicStore.isPlaying }">
        <span></span><span></span><span></span>
      </div>
    </div>

    <!-- 桌面端播放器卡片（左下角展开） -->
    <transition name="slide">
      <div v-show="expanded && !isMobile" class="player-card glass">
        <!-- 顶部栏 -->
        <div class="top-bar">
          <button class="back-btn" @click="expanded = false" title="收起">
            <svg viewBox="0 0 24 24" width="18" height="18" fill="currentColor"><path d="M19 11H7.83l4.88-4.88c.39-.39.39-1.03 0-1.42a.996.996 0 00-1.41 0l-6.59 6.59a.996.996 0 000 1.41l6.59 6.59a.996.996 0 101.41-1.41L7.83 13H19c.55 0 1-.45 1-1s-.45-1-1-1z"/></svg>
          </button>
          <span class="platform-label">📺 B站</span>
          <div class="search-input-wrap">
            <input v-model="searchKeyword" placeholder="搜索歌曲..." @keyup.enter="handleSearch" />
            <button class="search-btn" @click="handleSearch" :disabled="searching">
              <svg viewBox="0 0 24 24" width="16" height="16" fill="currentColor"><path d="M15.5 14h-.79l-.28-.27A6.471 6.471 0 0016 9.5 6.5 6.5 0 109.5 16c1.61 0 3.09-.59 4.23-1.57l.27.28v.79l5 4.99L20.49 19l-4.99-5zm-6 0C7.01 14 5 11.99 5 9.5S7.01 5 9.5 5 14 7.01 14 9.5 11.99 14 9.5 14z"/></svg>
            </button>
          </div>
        </div>

        <!-- 搜索结果 -->
        <div v-if="searchResults.length" class="search-results">
          <div v-for="song in searchResults" :key="song.id" 
               class="result-item" 
               :class="{ 'vip-song': song._isVip }"
               @click="playSong(song)">
            <img :src="getSongCover(song)" :alt="`${song.name || '歌曲'} 封面`" @error="handleImgError" />
            <div class="info">
              <div class="name">
                {{ song.name }}
                <span v-if="song._isVip" class="vip-badge">VIP</span>
              </div>
              <div class="artist">{{ getArtists(song) }}</div>
            </div>
            <button class="add-btn" @click.stop="addToPlaylist(song)">+</button>
          </div>
        </div>

        <!-- 当前播放 -->
        <div v-if="currentSong" class="now-playing">
          <img :src="getSongCover(currentSong)" :alt="`${currentSong?.name || '当前歌曲'} 封面`" class="cover" :class="{ spinning: musicStore.isPlaying }" @error="handleImgError" />
          <div class="info">
            <div class="name">{{ currentSong.name }}</div>
            <div class="artist">{{ artistName }}</div>
          </div>
        </div>
        <div v-else class="now-playing empty">
          <div class="info"><div class="name">未播放</div></div>
        </div>

        <!-- 进度条 -->
        <div class="progress-wrap">
          <span>{{ formatTime(musicStore.currentTime) }}</span>
          <input type="range" v-model="progress" min="0" max="100" @change="handleSeek" />
          <span>{{ formatTime(musicStore.duration) }}</span>
        </div>

        <!-- 控制按钮 -->
        <div class="controls">
          <button @click="musicStore.togglePlayMode" :title="playModeText">
            <svg v-if="musicStore.playMode === 0" viewBox="0 0 24 24" width="18" height="18" fill="currentColor"><path d="M7 7h10v3l4-4-4-4v3H5v6h2V7zm10 10H7v-3l-4 4 4 4v-3h12v-6h-2v4z"/></svg>
            <svg v-else-if="musicStore.playMode === 1" viewBox="0 0 24 24" width="18" height="18" fill="currentColor"><path d="M10.59 9.17L5.41 4 4 5.41l5.17 5.17 1.42-1.41zM14.5 4l2.04 2.04L4 18.59 5.41 20 17.96 7.46 20 9.5V4h-5.5zm.33 9.41l-1.41 1.41 3.13 3.13L14.5 20H20v-5.5l-2.04 2.04-3.13-3.13z"/></svg>
            <svg v-else viewBox="0 0 24 24" width="18" height="18" fill="currentColor"><path d="M7 7h10v3l4-4-4-4v3H5v6h2V7zm10 10H7v-3l-4 4 4 4v-3h12v-6h-2v4zm-4-2V9h-1l-2 1v1h1.5v4H13z"/></svg>
          </button>
          <button @click="musicStore.playPrev">
            <svg viewBox="0 0 24 24" width="22" height="22" fill="currentColor"><path d="M6 6h2v12H6zm3.5 6l8.5 6V6z"/></svg>
          </button>
          <button class="play-btn" @click="musicStore.togglePlay">
            <svg v-if="musicStore.isPlaying" viewBox="0 0 24 24" width="28" height="28" fill="currentColor"><path d="M6 19h4V5H6v14zm8-14v14h4V5h-4z"/></svg>
            <svg v-else viewBox="0 0 24 24" width="28" height="28" fill="currentColor"><path d="M8 5v14l11-7z"/></svg>
          </button>
          <button @click="musicStore.playNext">
            <svg viewBox="0 0 24 24" width="22" height="22" fill="currentColor"><path d="M6 18l8.5-6L6 6v12zM16 6v12h2V6h-2z"/></svg>
          </button>
          <button @click="showPlaylist = !showPlaylist">
            <svg viewBox="0 0 24 24" width="18" height="18" fill="currentColor"><path d="M3 13h2v-2H3v2zm0 4h2v-2H3v2zm0-8h2V7H3v2zm4 4h14v-2H7v2zm0 4h14v-2H7v2zM7 7v2h14V7H7z"/></svg>
          </button>
        </div>

        <!-- 音量 -->
        <div class="volume-wrap">
          <svg viewBox="0 0 24 24" width="16" height="16" fill="currentColor"><path d="M3 9v6h4l5 5V4L7 9H3zm13.5 3c0-1.77-1.02-3.29-2.5-4.03v8.05c1.48-.73 2.5-2.25 2.5-4.02z"/></svg>
          <input type="range" v-model="volumeValue" min="0" max="100" @input="handleVolume" />
        </div>

        <!-- 播放列表 -->
        <div v-if="showPlaylist" class="playlist">
          <div class="playlist-header">
            <span>播放列表 ({{ musicStore.playlist.length }})</span>
            <button @click="musicStore.clearPlaylist">清空</button>
          </div>
          <div class="playlist-items">
            <div v-for="(song, index) in musicStore.playlist" :key="song.id"
                 class="playlist-item" :class="{ active: index === musicStore.currentIndex }"
                 @click="musicStore.playSong(song, index)">
              <div class="pl-info">
                <div class="pl-name">{{ song.name }}</div>
                <div class="pl-artist">{{ song.ar?.map(a => a.name).join(' / ') }}</div>
              </div>
              <button @click.stop="musicStore.removeSong(index)">×</button>
            </div>
          </div>
        </div>
      </div>
    </transition>
  </div>

  <!-- 移动端：Teleport 到 body，居中模态框 -->
  <Teleport to="body">
    <Transition name="mobile-player">
      <div v-if="expanded && isMobile" class="mobile-player-overlay" @click.self="expanded = false">
        <div class="mobile-player-card glass">
          <!-- 顶部栏 -->
          <div class="top-bar">
            <button class="back-btn" @click="expanded = false" title="收起">
              <svg viewBox="0 0 24 24" width="18" height="18" fill="currentColor"><path d="M19 11H7.83l4.88-4.88c.39-.39.39-1.03 0-1.42a.996.996 0 00-1.41 0l-6.59 6.59a.996.996 0 000 1.41l6.59 6.59a.996.996 0 101.41-1.41L7.83 13H19c.55 0 1-.45 1-1s-.45-1-1-1z"/></svg>
            </button>
            <span class="platform-label">📺 B站</span>
            <div class="search-input-wrap">
              <input v-model="searchKeyword" placeholder="搜索歌曲..." @keyup.enter="handleSearch" />
              <button class="search-btn" @click="handleSearch" :disabled="searching">
                <svg viewBox="0 0 24 24" width="16" height="16" fill="currentColor"><path d="M15.5 14h-.79l-.28-.27A6.471 6.471 0 0016 9.5 6.5 6.5 0 109.5 16c1.61 0 3.09-.59 4.23-1.57l.27.28v.79l5 4.99L20.49 19l-4.99-5zm-6 0C7.01 14 5 11.99 5 9.5S7.01 5 9.5 5 14 7.01 14 9.5 11.99 14 9.5 14z"/></svg>
              </button>
            </div>
          </div>
          <!-- 搜索结果 -->
          <div v-if="searchResults.length" class="search-results">
            <div v-for="song in searchResults" :key="song.id" class="result-item" :class="{ 'vip-song': song._isVip }" @click="playSong(song)">
              <img :src="getSongCover(song)" :alt="`${song.name || '歌曲'} 封面`" @error="handleImgError" />
              <div class="info">
                <div class="name">{{ song.name }}<span v-if="song._isVip" class="vip-badge">VIP</span></div>
                <div class="artist">{{ getArtists(song) }}</div>
              </div>
              <button class="add-btn" @click.stop="addToPlaylist(song)">+</button>
            </div>
          </div>
          <!-- 当前播放 -->
          <div v-if="currentSong" class="now-playing">
            <img :src="getSongCover(currentSong)" :alt="`${currentSong?.name || '当前歌曲'} 封面`" class="cover" :class="{ spinning: musicStore.isPlaying }" @error="handleImgError" />
            <div class="info">
              <div class="name">{{ currentSong.name }}</div>
              <div class="artist">{{ artistName }}</div>
            </div>
          </div>
          <div v-else class="now-playing empty"><div class="info"><div class="name">未播放</div></div></div>
          <!-- 进度条 -->
          <div class="progress-wrap">
            <span>{{ formatTime(musicStore.currentTime) }}</span>
            <input type="range" v-model="progress" min="0" max="100" @change="handleSeek" />
            <span>{{ formatTime(musicStore.duration) }}</span>
          </div>
          <!-- 控制按钮 -->
          <div class="controls">
            <button @click="musicStore.togglePlayMode" :title="playModeText">
              <svg v-if="musicStore.playMode === 0" viewBox="0 0 24 24" width="18" height="18" fill="currentColor"><path d="M7 7h10v3l4-4-4-4v3H5v6h2V7zm10 10H7v-3l-4 4 4 4v-3h12v-6h-2v4z"/></svg>
              <svg v-else-if="musicStore.playMode === 1" viewBox="0 0 24 24" width="18" height="18" fill="currentColor"><path d="M10.59 9.17L5.41 4 4 5.41l5.17 5.17 1.42-1.41zM14.5 4l2.04 2.04L4 18.59 5.41 20 17.96 7.46 20 9.5V4h-5.5zm.33 9.41l-1.41 1.41 3.13 3.13L14.5 20H20v-5.5l-2.04 2.04-3.13-3.13z"/></svg>
              <svg v-else viewBox="0 0 24 24" width="18" height="18" fill="currentColor"><path d="M7 7h10v3l4-4-4-4v3H5v6h2V7zm10 10H7v-3l-4 4 4 4v-3h12v-6h-2v4zm-4-2V9h-1l-2 1v1h1.5v4H13z"/></svg>
            </button>
            <button @click="musicStore.playPrev"><svg viewBox="0 0 24 24" width="22" height="22" fill="currentColor"><path d="M6 6h2v12H6zm3.5 6l8.5 6V6z"/></svg></button>
            <button class="play-btn" @click="musicStore.togglePlay">
              <svg v-if="musicStore.isPlaying" viewBox="0 0 24 24" width="28" height="28" fill="currentColor"><path d="M6 19h4V5H6v14zm8-14v14h4V5h-4z"/></svg>
              <svg v-else viewBox="0 0 24 24" width="28" height="28" fill="currentColor"><path d="M8 5v14l11-7z"/></svg>
            </button>
            <button @click="musicStore.playNext"><svg viewBox="0 0 24 24" width="22" height="22" fill="currentColor"><path d="M6 18l8.5-6L6 6v12zM16 6v12h2V6h-2z"/></svg></button>
            <button @click="showPlaylist = !showPlaylist"><svg viewBox="0 0 24 24" width="18" height="18" fill="currentColor"><path d="M3 13h2v-2H3v2zm0 4h2v-2H3v2zm0-8h2V7H3v2zm4 4h14v-2H7v2zm0 4h14v-2H7v2zM7 7v2h14V7H7z"/></svg></button>
          </div>
          <!-- 音量 -->
          <div class="volume-wrap">
            <svg viewBox="0 0 24 24" width="16" height="16" fill="currentColor"><path d="M3 9v6h4l5 5V4L7 9H3zm13.5 3c0-1.77-1.02-3.29-2.5-4.03v8.05c1.48-.73 2.5-2.25 2.5-4.02z"/></svg>
            <input type="range" v-model="volumeValue" min="0" max="100" @input="handleVolume" />
          </div>
          <!-- 播放列表 -->
          <div v-if="showPlaylist" class="playlist">
            <div class="playlist-header">
              <span>播放列表 ({{ musicStore.playlist.length }})</span>
              <button @click="musicStore.clearPlaylist">清空</button>
            </div>
            <div class="playlist-items">
              <div v-for="(song, index) in musicStore.playlist" :key="song.id"
                   class="playlist-item" :class="{ active: index === musicStore.currentIndex }"
                   @click="musicStore.playSong(song, index)">
                <div class="pl-info">
                  <div class="pl-name">{{ song.name }}</div>
                  <div class="pl-artist">{{ song.ar?.map(a => a.name).join(' / ') }}</div>
                </div>
                <button @click.stop="musicStore.removeSong(index)">×</button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
  </div>
</template>
<script setup>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useMusicStore } from '@/stores/music'
import { searchSongs, getSongDetail } from '@/api/music'
import { normalizeUnsafeUrl } from '@/utils/image'

const musicStore = useMusicStore()

onMounted(() => { musicStore.restorePlayback() })

const expanded = ref(false)
const isMobile = ref(window.innerWidth < 768)
const onResize = () => { isMobile.value = window.innerWidth < 768 }
window.addEventListener('resize', onResize)
onUnmounted(() => window.removeEventListener('resize', onResize))

watch(() => musicStore.showPlayer, (val) => {
  if (val > 0) { expanded.value = true }
})
const showPlaylist = ref(false)
const searchKeyword = ref('')
const searchResults = ref([])
const searching = ref(false)

const currentSong = computed(() => musicStore.currentSong)
const artistName = computed(() => getArtists(currentSong.value))
const progress = computed({
  get: () => musicStore.duration ? (musicStore.currentTime / musicStore.duration) * 100 : 0,
  set: (val) => { musicStore.seekTo((val / 100) * musicStore.duration) }
})
const volumeValue = ref(musicStore.volume * 100)
const playModeText = computed(() => ['顺序播放', '随机播放', '单曲循环'][musicStore.playMode])

const formatTime = (s) => {
  if (!s || isNaN(s)) return '0:00'
  return `${Math.floor(s / 60)}:${Math.floor(s % 60).toString().padStart(2, '0')}`
}

const handleSearch = async () => {
  if (!searchKeyword.value.trim()) return
  searching.value = true
  try {
    const res = await searchSongs(searchKeyword.value, 20)
    searchResults.value = res.result?.songs || []
  } catch (e) { console.error('搜索失败:', e) }
  finally { searching.value = false }
}

const playSong = async (song) => {
  musicStore.addAndPlay(song)
  searchResults.value = []
}

const addToPlaylist = (song) => { musicStore.addSongs([song]) }
const handleSeek = (e) => { musicStore.seekTo((e.target.value / 100) * musicStore.duration) }
const handleVolume = (e) => { musicStore.setVolume(e.target.value / 100) }

const defaultCover = 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSI2NCIgaGVpZ2h0PSI2NCIgdmlld0JveD0iMCAwIDI0IDI0IiBmaWxsPSIjOTk5Ij48cGF0aCBkPSJNMTIgM3Y5LjI2Yy0uNS0uMTctMS0uMjYtMS41LS4yNkM4IDEyIDYgMTQgNiAxNi41UzggMjEgMTAuNSAyMXM0LjUtMiA0LjUtNC41VjZoNFYzaC03eiIvPjwvc3ZnPg=='

// 获取歌曲封面
const getSongCover = (song) => {
  if (!song) return defaultCover
  const url = song.al?.picUrl || song.album?.picUrl || ''
  if (!url) return defaultCover
  const normalized = url.startsWith('//') ? 'https:' + url : url
  return normalizeUnsafeUrl(normalized)
}
const handleImgError = (e) => { e.target.src = defaultCover }
const getArtists = (song) => {
  if (!song) return ''
  const artists = song.artists || song.ar
  return artists?.map(a => a.name).join(' / ') || ''
}
</script>

<style scoped>
.music-player-wrapper {
  position: fixed;
  bottom: 20px;
  left: 20px;
  z-index: 1000;
}

.player-trigger {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: rgba(255,255,255,0.15);
  backdrop-filter: blur(12px);
  border: 1px solid rgba(255,255,255,0.2);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.3s;
  box-shadow: 0 4px 16px rgba(0,0,0,0.1);
}
.player-trigger:hover { transform: scale(1.1); background: rgba(255,255,255,0.25); }
.player-trigger.expanded { opacity: 0; pointer-events: none; }
.trigger-icon { color: var(--el-color-primary, #409eff); }
.trigger-wave {
  position: absolute;
  bottom: 6px;
  right: 6px;
  display: flex;
  gap: 2px;
}
.trigger-wave span {
  width: 3px;
  height: 8px;
  background: var(--el-color-primary, #409eff);
  border-radius: 2px;
}
.trigger-wave.playing span {
  animation: wave 0.6s ease-in-out infinite;
}
.trigger-wave.playing span:nth-child(2) { animation-delay: 0.2s; }
.trigger-wave.playing span:nth-child(3) { animation-delay: 0.4s; }
@keyframes wave {
  0%, 100% { height: 4px; }
  50% { height: 12px; }
}

.player-card {
  position: absolute;
  bottom: 0;
  left: 0;
  width: 320px;
  padding: 16px;
  border-radius: 16px;
  background: rgba(255,255,255,0.12);
  backdrop-filter: blur(20px);
  border: 1px solid rgba(255,255,255,0.2);
  box-shadow: 0 8px 32px rgba(0,0,0,0.15);
}
:root.dark .player-card {
  background: rgba(30,30,30,0.7);
  border-color: rgba(255,255,255,0.1);
}

.slide-enter-active, .slide-leave-active { transition: all 0.3s ease; }
.slide-enter-from, .slide-leave-to { opacity: 0; transform: translateY(20px) scale(0.95); }

.top-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}
.back-btn {
  background: none;
  border: none;
  color: inherit;
  cursor: pointer;
  padding: 6px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0.7;
  transition: all 0.2s;
}
.back-btn:hover { opacity: 1; background: rgba(255,255,255,0.15); }
.platform-label {
  font-size: 12px;
  padding: 4px 8px;
  border-radius: 12px;
  background: rgba(255,255,255,0.15);
  white-space: nowrap;
  flex-shrink: 0;
}
.search-input-wrap {
  flex: 1;
  position: relative;
  display: flex;
  align-items: center;
}
.search-input-wrap input {
  width: 100%;
  padding: 8px 36px 8px 12px;
  border: none;
  border-radius: 20px;
  background: rgba(255,255,255,0.15);
  color: inherit;
  outline: none;
}
.search-input-wrap input::placeholder { color: rgba(150,150,150,0.8); }
.search-btn {
  position: absolute;
  right: 4px;
  padding: 6px;
  border: none;
  border-radius: 50%;
  background: var(--el-color-primary, #409eff);
  color: white;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
}
.search-btn:hover { background: var(--el-color-primary-light-3, #66b1ff); }

.search-results {
  max-height: 150px;
  overflow-y: auto;
  margin-bottom: 12px;
  border-radius: 8px;
  background: rgba(0,0,0,0.1);
}
.result-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px;
  cursor: pointer;
  transition: background 0.2s;
}
.result-item:hover { background: rgba(255,255,255,0.1); }
.result-item img { width: 36px; height: 36px; border-radius: 4px; object-fit: cover; }
.result-item .info { flex: 1; min-width: 0; }
.result-item .name { font-size: 13px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; display: flex; align-items: center; gap: 4px; }
.result-item .artist { font-size: 11px; opacity: 0.7; }
.vip-song { opacity: 0.6; }
.vip-song:hover { opacity: 0.8; }
.vip-badge { 
  display: inline-block;
  padding: 1px 4px;
  font-size: 10px;
  background: linear-gradient(135deg, #f59e0b 0%, #ef4444 100%);
  color: white;
  border-radius: 3px;
  font-weight: bold;
  flex-shrink: 0;
}
.add-btn { background: none; border: none; font-size: 18px; color: var(--el-color-primary); cursor: pointer; }

.now-playing {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}
.now-playing.empty { justify-content: center; }
.now-playing .cover {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  object-fit: cover;
  box-shadow: 0 4px 12px rgba(0,0,0,0.2);
}
.now-playing .cover.spinning { animation: spin 8s linear infinite; }
@keyframes spin { from { transform: rotate(0deg); } to { transform: rotate(360deg); } }
.now-playing .info { flex: 1; min-width: 0; }
.now-playing .name { font-size: 14px; font-weight: 500; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.now-playing .artist { font-size: 12px; opacity: 0.7; }

.progress-wrap {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
  font-size: 11px;
  opacity: 0.8;
}
.progress-wrap input[type="range"] { flex: 1; height: 4px; cursor: pointer; accent-color: var(--el-color-primary, #409eff); }

.controls {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  margin-bottom: 12px;
}
.controls button {
  background: none;
  border: none;
  color: inherit;
  cursor: pointer;
  padding: 6px;
  border-radius: 50%;
  transition: background 0.2s;
  display: flex;
  align-items: center;
  justify-content: center;
}
.controls button:hover { background: rgba(255,255,255,0.15); }
.controls .play-btn {
  width: 48px;
  height: 48px;
  background: var(--el-color-primary, #409eff);
  color: white;
}
.controls .play-btn:hover { background: var(--el-color-primary-light-3, #66b1ff); }

.volume-wrap {
  display: flex;
  align-items: center;
  gap: 8px;
  opacity: 0.8;
}
.volume-wrap input[type="range"] { flex: 1; height: 4px; cursor: pointer; accent-color: var(--el-color-primary, #409eff); }

.playlist {
  margin-top: 12px;
  border-radius: 8px;
  background: rgba(0,0,0,0.1);
  max-height: 160px;
  overflow: hidden;
}
.playlist-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 12px;
  font-size: 12px;
  border-bottom: 1px solid rgba(255,255,255,0.1);
}
.playlist-header button { background: none; border: none; color: inherit; cursor: pointer; opacity: 0.7; }
.playlist-items { max-height: 120px; overflow-y: auto; }
.playlist-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 6px 12px;
  cursor: pointer;
  transition: background 0.2s;
}
.playlist-item:hover { background: rgba(255,255,255,0.1); }
.playlist-item.active { background: rgba(var(--el-color-primary-rgb, 64,158,255),0.2); }
.pl-info { flex: 1; min-width: 0; }
.pl-name { font-size: 12px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.pl-artist { font-size: 10px; opacity: 0.6; }
.playlist-item button { background: none; border: none; color: inherit; cursor: pointer; opacity: 0.5; font-size: 16px; }
.playlist-item button:hover { opacity: 1; color: #f56c6c; }

/* 移动端：隐藏桌面端触发器 */
@media (max-width: 767px) {
  .music-player-wrapper {
    display: none;
  }
}

/* 移动端播放器模态框 */
.mobile-player-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0,0,0,0.5);
  backdrop-filter: blur(4px);
  z-index: 500;
  display: flex;
  align-items: flex-start;
  justify-content: center;
  padding-top: 80px;
}
.mobile-player-card {
  width: calc(100vw - 24px);
  max-width: 400px;
  max-height: calc(100dvh - 120px);
  overflow-y: auto;
  padding: 16px;
  border-radius: 20px;
  background: rgba(20, 20, 30, 0.92);
  backdrop-filter: blur(24px) saturate(180%);
  -webkit-backdrop-filter: blur(24px) saturate(180%);
  border: 1px solid rgba(255,255,255,0.15);
  box-shadow: 0 16px 48px rgba(0,0,0,0.5);
  color: #fff;
}
.mobile-player-card .platform-select {
  background: rgba(255,255,255,0.12);
  color: #fff;
}
.mobile-player-card .search-input-wrap input {
  background: rgba(255,255,255,0.12);
  color: #fff;
}
.mobile-player-card .search-input-wrap input::placeholder { color: rgba(255,255,255,0.45); }
.mobile-player-card .back-btn,
.mobile-player-card .controls button {
  color: #fff;
}
.mobile-player-card .now-playing .name { color: #fff; }
.mobile-player-card .now-playing .artist { color: rgba(255,255,255,0.65); }
.mobile-player-card .progress-wrap { color: rgba(255,255,255,0.7); }
.mobile-player-card .volume-wrap { color: rgba(255,255,255,0.7); }
.mobile-player-card .search-results { background: rgba(255,255,255,0.08); }
.mobile-player-card .result-item .name { color: #fff; }
.mobile-player-card .result-item .artist { color: rgba(255,255,255,0.6); }
.mobile-player-card .playlist { background: rgba(255,255,255,0.08); }
.mobile-player-card .playlist-header { color: rgba(255,255,255,0.8); border-color: rgba(255,255,255,0.1); }
.mobile-player-card .pl-name { color: #fff; }
.mobile-player-card .pl-artist { color: rgba(255,255,255,0.55); }
.mobile-player-enter-active { transition: opacity 0.25s ease, transform 0.25s ease; }
.mobile-player-leave-active { transition: opacity 0.2s ease, transform 0.2s ease; }
.mobile-player-enter-from { opacity: 0; transform: translateY(-20px) scale(0.96); }
.mobile-player-leave-to { opacity: 0; transform: translateY(-20px) scale(0.96); }
</style>
