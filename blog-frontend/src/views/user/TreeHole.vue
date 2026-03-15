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
const SAFE_BOTTOM_SPACE = 190

const content = ref('')
const visibleMessages = ref([])
const allMessages = ref([])
const headerHeight = ref(64)

let uidCounter = 0
// 每条轨道的下次可用时间戳
const laneNextAvailable = ref([])

const getHeaderHeight = () => {
  const header = document.querySelector('header')
  return header?.offsetHeight || 64
}

const getDanmakuFontSize = () => (window.innerWidth <= 768 ? 14 : 18)
const getLaneHeight = () => getDanmakuFontSize() + 14

const rebuildLanes = () => {
  const availableHeight = Math.max(80, window.innerHeight - headerHeight.value - SAFE_BOTTOM_SPACE)
  const laneCount = Math.max(3, Math.floor(availableHeight / getLaneHeight()))
  laneNextAvailable.value = Array.from({ length: laneCount }, () => 0)
}

const getLaneCount = () => laneNextAvailable.value.length

// 找一条当前可用的轨道，优先选最早空闲的
const pickLane = () => {
  if (!getLaneCount()) rebuildLanes()
  const now = performance.now()
  let bestLane = -1
  let bestTime = Infinity
  for (let i = 0; i < getLaneCount(); i++) {
    if (laneNextAvailable.value[i] <= now && laneNextAvailable.value[i] < bestTime) {
      bestTime = laneNextAvailable.value[i]
      bestLane = i
    }
  }
  return bestLane
}

const removeDanmaku = (uid) => {
  const idx = visibleMessages.value.findIndex(m => m.uid === uid)
  if (idx > -1) visibleMessages.value.splice(idx, 1)
}

const addDanmaku = (msg) => {
  if (visibleMessages.value.length >= MAX_VISIBLE_DANMAKU) return
  if (!msg?.content) return

  const laneIndex = pickLane()
  if (laneIndex < 0) return

  // 每条弹幕速度略有差异，持续时间 8~12s
  const duration = 8 + Math.random() * 4
  const uid = `${msg.id || Date.now()}-${uidCounter++}`
  const laneTop = laneIndex * getLaneHeight()

  visibleMessages.value.push({
    ...msg,
    uid,
    style: {
      top: `${laneTop}px`,
      color: msg.color || '#fff',
      animationDuration: `${duration}s`
    }
  })

  // 该轨道在弹幕飞出屏幕前不再分配新弹幕（留 1s 间隔避免重叠）
  laneNextAvailable.value[laneIndex] = performance.now() + (duration - 1) * 1000
}

const loadMessages = async () => {
  const res = await getTreeHoles()
  if (res.success && res.data.length > 0) {
    allMessages.value = res.data.slice(0, 80).reverse()
    startLoop()
  }
}

let msgIndex = 0
const timers = []

const startLoop = () => {
  if (!allMessages.value.length) return
  const laneCount = getLaneCount() || 8

  // 每条轨道独立随机延迟启动，错开出现时间
  for (let lane = 0; lane < laneCount; lane++) {
    const initialDelay = Math.random() * 6000 // 0~6s 随机初始延迟
    const scheduleNext = () => {
      if (!allMessages.value.length) return
      const msg = allMessages.value[msgIndex % allMessages.value.length]
      msgIndex++
      addDanmaku(msg)
      // 每条轨道下次发射间隔：弹幕飞行时长 + 随机 1~4s 间隔
      const duration = 8 + Math.random() * 4
      const nextDelay = duration * 1000 + 1000 + Math.random() * 3000
      const t = setTimeout(scheduleNext, nextDelay)
      timers.push(t)
    }
    const t = setTimeout(scheduleNext, initialDelay)
    timers.push(t)
  }
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
  timers.forEach(t => clearTimeout(t))
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
