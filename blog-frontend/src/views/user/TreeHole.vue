<template>
  <div class="tree-hole">
    <!-- 星空背景 -->
    <canvas ref="canvas" class="starry-bg"></canvas>

    <!-- 弹幕区域 -->
    <div class="danmaku-container">
      <div v-for="msg in visibleMessages" :key="msg.uid" class="danmaku" :style="msg.style">
        {{ msg.content }}
      </div>
    </div>

    <!-- 输入区域 -->
    <div class="input-area glass">
      <div class="input-box">
        <el-input v-model="content" placeholder="说点什么吧..." maxlength="50" @keyup.enter="send" @input="checkLength" />
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

const canvas = ref(null)
const content = ref('')
const visibleMessages = ref([])
const allMessages = ref([]) // 保存所有消息用于循环
let animationId = null
let stars = []
let loopTimer = null

// 星空动画
const initStars = () => {
  const ctx = canvas.value.getContext('2d')
  canvas.value.width = window.innerWidth
  canvas.value.height = window.innerHeight
  stars = Array.from({ length: 200 }, () => ({
    x: Math.random() * canvas.value.width,
    y: Math.random() * canvas.value.height,
    r: Math.random() * 1.5,
    speed: Math.random() * 0.5 + 0.1
  }))
  const animate = () => {
    ctx.fillStyle = 'rgba(15, 23, 42, 0.2)'
    ctx.fillRect(0, 0, canvas.value.width, canvas.value.height)
    stars.forEach(s => {
      ctx.beginPath()
      ctx.arc(s.x, s.y, s.r, 0, Math.PI * 2)
      ctx.fillStyle = `rgba(255,255,255,${Math.random() * 0.5 + 0.5})`
      ctx.fill()
      s.y += s.speed
      if (s.y > canvas.value.height) { s.y = 0; s.x = Math.random() * canvas.value.width }
    })
    animationId = requestAnimationFrame(animate)
  }
  animate()
}

// 添加弹幕
let uidCounter = 0
const addDanmaku = (msg) => {
  // 检查是否已经在屏幕上显示
  const exists = visibleMessages.value.some(m => m.id === msg.id)
  if (exists) return
  
  const h = window.innerHeight - 200
  const uid = `${msg.id || Date.now()}-${uidCounter++}`
  const duration = 10 + Math.random() * 2 // 10-12秒
  const item = {
    ...msg,
    uid,
    style: {
      top: Math.random() * h + 'px',
      color: msg.color || '#fff',
      animationDuration: duration + 's'
    }
  }
  visibleMessages.value.push(item)
  setTimeout(() => {
    const idx = visibleMessages.value.findIndex(m => m.uid === uid)
    if (idx > -1) visibleMessages.value.splice(idx, 1)
  }, duration * 1000 + 500) // 动画结束后再删除，加500ms缓冲
}

// 加载历史消息并启动循环
const loadMessages = async () => {
  const res = await getTreeHoles()
  if (res.success && res.data.length > 0) {
    allMessages.value = res.data.slice(0, 20).reverse()
    startLoop()
  }
}

// 循环播放弹幕
const startLoop = () => {
  let index = 0
  const playNext = () => {
    if (allMessages.value.length === 0) return
    addDanmaku(allMessages.value[index])
    index = (index + 1) % allMessages.value.length
    loopTimer = setTimeout(playNext, 2000 + Math.random() * 1000)
  }
  playNext()
}

// 发送消息
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

onMounted(() => { initStars(); loadMessages() })
onUnmounted(() => { cancelAnimationFrame(animationId); clearTimeout(loopTimer) })
</script>

<style scoped>
.tree-hole {
  position: fixed;
  inset: 0;
  overflow: hidden;
  background: #0f172a;
}
.starry-bg {
  position: absolute;
  inset: 0;
}
.danmaku-container {
  position: absolute;
  inset: 0;
  pointer-events: none;
}
.danmaku {
  position: absolute;
  left: 100%;
  white-space: nowrap;
  font-size: 18px;
  text-shadow: 0 0 10px currentColor;
  animation: fly linear forwards;
}
@keyframes fly {
  from { transform: translateX(0); }
  to { transform: translateX(calc(-100vw - 100%)); }
}
.input-area {
  position: fixed;
  bottom: 30px;
  left: 50%;
  transform: translateX(-50%);
  padding: 12px 20px;
  border-radius: 30px;
  background: rgba(255,255,255,0.1);
  backdrop-filter: blur(10px);
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
</style>
