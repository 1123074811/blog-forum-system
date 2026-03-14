<template>
  <div class="tree-hole">
    <video
      class="video-bg"
      autoplay
      loop
      muted
      playsinline
      webkit-playsinline
      x5-playsinline
      x-webkit-airplay="allow"
      disablePictureInPicture
      controlsList="nodownload nofullscreen noremoteplayback"
    >
      <source src="/treehole_bg.mp4" type="video/mp4">
    </video>

    <div class="danmaku-container">
      <div
        v-for="msg in visibleMessages"
        :key="msg.uid"
        class="danmaku"
        :style="msg.style"
        @animationend="removeDanmaku(msg.uid)"
      >
        {{ msg.content }}
      </div>
    </div>

    <div class="input-area glass">
      <div class="input-box">
        <el-input
          v-model="content"
          placeholder="说点什么吧..."
          maxlength="50"
          @keyup.enter="send"
          @input="checkLength"
        />
        <div class="input-actions">
          <EmojiPicker @select="e => content += e" />
          <el-button type="primary" size="small" @click="send">发送</el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getTreeHoles, createTreeHole } from '@/api/blog'
import EmojiPicker from '@/components/EmojiPicker.vue'

const DANMAKU_COLORS = ['#FF6B6B', '#4ECDC4', '#45B7D1', '#96CEB4', '#FFEAA7', '#DDA0DD', '#98D8C8', '#F7DC6F', '#BB8FCE']
const MAX_VISIBLE_DANMAKU = 24
const DANMAKU_INTERVAL_MIN = 650
const DANMAKU_INTERVAL_RANGE = 400
const SAFE_BOTTOM_SPACE = 190

const content = ref('')
const visibleMessages = ref([])
const allMessages = ref([])
const headerHeight = ref(64)

let loopTimer = null
let uidCounter = 0
let roundRobinLane = 0
const lanes = ref([])

const getHeaderHeight = () => {
  const header = document.querySelector('header')
  return header?.offsetHeight || 64
}

const getDanmakuFontSize = () => (window.innerWidth <= 768 ? 14 : 18)
const getLaneHeight = () => getDanmakuFontSize() + 12

const estimateTextWidth = (text) => {
  const charWidth = getDanmakuFontSize() * 0.62
  return Math.max(80, text.length * charWidth + 28)
}

const rebuildLanes = () => {
  const availableHeight = Math.max(80, window.innerHeight - headerHeight.value - SAFE_BOTTOM_SPACE)
  const laneCount = Math.max(3, Math.floor(availableHeight / getLaneHeight()))
  lanes.value = Array.from({ length: laneCount }, () => ({ last: null }))
}

const canUseLane = (lane, now, viewportWidth, msgSpeed) => {
  if (!lane?.last) return true
  const prev = lane.last
  const elapsed = (now - prev.startAt) / 1000
  if (elapsed <= 0) return false

  const prevRightNow = viewportWidth + prev.width - prev.speed * elapsed
  if (prevRightNow > viewportWidth) return false
  if (msgSpeed <= prev.speed) return true

  const gapNow = prevRightNow - viewportWidth
  const catchUpWindow = prevRightNow / prev.speed
  return gapNow + (msgSpeed - prev.speed) * catchUpWindow <= 0
}

const pickLane = (msgSpeed) => {
  if (!lanes.value.length) rebuildLanes()
  const now = performance.now()
  const viewportWidth = window.innerWidth
  const start = roundRobinLane % lanes.value.length

  for (let i = 0; i < lanes.value.length; i += 1) {
    const laneIndex = (start + i) % lanes.value.length
    if (canUseLane(lanes.value[laneIndex], now, viewportWidth, msgSpeed)) {
      roundRobinLane = laneIndex + 1
      return laneIndex
    }
  }
  return -1
}

const removeDanmaku = (uid) => {
  const idx = visibleMessages.value.findIndex(m => m.uid === uid)
  if (idx > -1) visibleMessages.value.splice(idx, 1)
}

const addDanmaku = (msg) => {
  if (visibleMessages.value.length >= MAX_VISIBLE_DANMAKU) return
  if (!msg?.content) return

  const duration = 9 + Math.random() * 2.2
  const width = estimateTextWidth(msg.content)
  const speed = (window.innerWidth + width) / duration
  const laneIndex = pickLane(speed)
  if (laneIndex < 0) return

  const uid = `${msg.id || Date.now()}-${uidCounter++}`
  const laneTop = laneIndex * getLaneHeight()
  const jitter = Math.random() * 4
  visibleMessages.value.push({
    ...msg,
    uid,
    style: {
      top: `${laneTop + jitter}px`,
      color: msg.color || '#fff',
      animationDuration: `${duration}s`
    }
  })

  lanes.value[laneIndex].last = { width, speed, startAt: performance.now() }
}

const loadMessages = async () => {
  const res = await getTreeHoles()
  if (res.success && res.data.length > 0) {
    allMessages.value = res.data.slice(0, 80).reverse()
    startLoop()
  }
}

const startLoop = () => {
  let index = 0
  const playNext = () => {
    if (!allMessages.value.length) return
    addDanmaku(allMessages.value[index])
    index = (index + 1) % allMessages.value.length
    if (visibleMessages.value.length < 10 && allMessages.value.length > 1) {
      addDanmaku(allMessages.value[index])
      index = (index + 1) % allMessages.value.length
    }
    const nextDelay = DANMAKU_INTERVAL_MIN + Math.random() * DANMAKU_INTERVAL_RANGE
    loopTimer = setTimeout(playNext, nextDelay)
  }
  playNext()
}

const send = async () => {
  if (!content.value.trim()) return
  const randomColor = DANMAKU_COLORS[Math.floor(Math.random() * DANMAKU_COLORS.length)]
  const res = await createTreeHole({ content: content.value, color: randomColor })
  if (res.success) {
    addDanmaku(res.data)
    content.value = ''
    ElMessage.success('发送成功')
  }
}

let warned = false
const checkLength = () => {
  if (content.value.length >= 50 && !warned) {
    ElMessage.warning('最多输入50字')
    warned = true
  }
  if (content.value.length < 50) warned = false
}

const handleResize = () => {
  headerHeight.value = getHeaderHeight()
  rebuildLanes()
}

onMounted(() => {
  headerHeight.value = getHeaderHeight()
  rebuildLanes()
  loadMessages()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  clearTimeout(loopTimer)
  window.removeEventListener('resize', handleResize)
})
</script>

<style scoped>
.tree-hole {
  position: fixed;
  top: var(--app-header-height);
  left: 0;
  right: 0;
  height: calc(100vh - var(--app-header-height));
  overflow: hidden;
  background: #0f172a;
}

.video-bg {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  z-index: 0;
  pointer-events: none;
}

.danmaku-container {
  position: absolute;
  inset: 0;
  pointer-events: none;
  z-index: 1;
}

.danmaku {
  position: absolute;
  left: 100%;
  white-space: nowrap;
  font-size: 18px;
  text-shadow: 0 0 10px currentColor;
  will-change: transform;
  animation: fly linear forwards;
}

@keyframes fly {
  from { transform: translateX(0); }
  to { transform: translateX(calc(-100vw - 120%)); }
}

.input-area {
  position: fixed;
  bottom: 30px;
  left: 50%;
  transform: translateX(-50%);
  padding: 12px 20px;
  border-radius: 30px;
  background: rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(10px);
  z-index: 2;
  width: auto;
  max-width: 90vw;
}

.input-box {
  position: relative;
  display: flex;
  align-items: center;
}

.input-area :deep(.el-input) {
  width: 320px;
}

.input-area :deep(.el-input__wrapper) {
  background: transparent;
  box-shadow: none;
  padding-right: 100px;
}

.input-area :deep(.el-input__inner) {
  color: #fff;
}

.input-actions {
  position: absolute;
  right: 4px;
  display: flex;
  align-items: center;
  gap: 6px;
}

@media (max-width: 768px) {
  .input-area {
    bottom: 16px;
    padding: 10px 14px;
    border-radius: 24px;
  }

  .input-area :deep(.el-input) {
    width: 60vw;
  }

  .danmaku {
    font-size: 14px;
  }
}
</style>
