<template>
  <div class="pomodoro-container">
    <div class="pomodoro-card">
      <!-- 左侧：时钟和标题 -->
      <div class="left-section">
        <h1 class="title">🍅 番茄时钟</h1>

        <!-- 时钟显示 -->
        <div class="timer-display">
          <svg class="progress-ring" viewBox="0 0 400 400" width="100%" height="100%">
            <!-- 灰白色背景圆环 -->
            <circle
              class="progress-ring-bg"
              cx="200"
              cy="200"
              r="180"
            />
            <!-- 秒针圆弧（红色，每分钟转一圈） -->
            <circle
              class="progress-ring-seconds"
              :style="{ strokeDashoffset: secondsProgressOffset }"
              cx="200"
              cy="200"
              r="180"
            />
          </svg>
          <div class="time-text">{{ formattedTime }}</div>
        </div>

        <!-- 说明 -->
        <div class="tips">
          <p>💡 工作 25 分钟，休息 5 分钟</p>
          <p>💡 每完成 4 个番茄钟，休息 15-30 分钟</p>
        </div>
      </div>

      <!-- 右侧：控制和统计 -->
      <div class="right-section">
        <!-- 模式切换 -->
        <div class="mode-tabs">
          <button
            v-for="mode in modes"
            :key="mode.key"
            :class="['mode-tab', { active: currentMode === mode.key }]"
            @click="switchMode(mode.key)"
          >
            {{ mode.label }}
          </button>
        </div>

        <!-- 统计信息 -->
        <div class="stats">
          <div class="stat-item">
            <div class="stat-label">今日完成</div>
            <div class="stat-value">{{ todayCount }} 个番茄</div>
          </div>
          <div class="stat-item">
            <div class="stat-label">当前轮次</div>
            <div class="stat-value">{{ currentRound }} / 4</div>
          </div>
        </div>

        <!-- 控制按钮 -->
        <div class="controls">
          <button v-if="!isRunning" class="btn btn-start" @click="start">
            <el-icon><VideoPlay /></el-icon> 开始
          </button>
          <button v-else class="btn btn-pause" @click="pause">
            <el-icon><VideoPause /></el-icon> 暂停
          </button>
          <button class="btn btn-reset" @click="reset">
            <el-icon><RefreshRight /></el-icon> 重置
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onUnmounted } from 'vue'
import { VideoPlay, VideoPause, RefreshRight } from '@element-plus/icons-vue'
import { ElNotification } from 'element-plus'

const modes = [
  { key: 'work', label: '工作', duration: 25 * 60 },
  { key: 'shortBreak', label: '短休息', duration: 5 * 60 },
  { key: 'longBreak', label: '长休息', duration: 15 * 60 }
]

const currentMode = ref('work')
const timeLeft = ref(25 * 60)
const isRunning = ref(false)
const currentRound = ref(1)
const todayCount = ref(0)
let timer = null

const formattedTime = computed(() => {
  const minutes = Math.floor(timeLeft.value / 60)
  const seconds = timeLeft.value % 60
  return `${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`
})

const progressOffset = computed(() => {
  const mode = modes.find(m => m.key === currentMode.value)
  const total = mode.duration
  const progress = (total - timeLeft.value) / total
  const circumference = 2 * Math.PI * 180
  return circumference * (1 - progress)
})

// 秒针进度（每分钟转一圈，红色从0开始增长，填满后立即变灰）
const secondsProgressOffset = computed(() => {
  const totalSeconds = timeLeft.value
  const mode = modes.find(m => m.key === currentMode.value)
  const elapsed = mode.duration - totalSeconds  // 总共已过去的秒数
  const secondsInCurrentMinute = elapsed % 60  // 当前分钟内已过去的秒数（0-59）
  
  // 当秒数为0时（整分钟），显示为空（全灰色）
  if (secondsInCurrentMinute === 0) {
    return 2 * Math.PI * 180  // 完全隐藏红色
  }
  
  const progress = secondsInCurrentMinute / 60
  const circumference = 2 * Math.PI * 180
  return circumference * (1 - progress)  // 从满圈开始减少offset，显示为增长
})

function switchMode(mode) {
  if (isRunning.value) return
  currentMode.value = mode
  const modeConfig = modes.find(m => m.key === mode)
  timeLeft.value = modeConfig.duration
}

function start() {
  isRunning.value = true
  timer = setInterval(() => {
    if (timeLeft.value > 0) {
      timeLeft.value--
    } else {
      complete()
    }
  }, 1000)
}

function pause() {
  isRunning.value = false
  if (timer) {
    clearInterval(timer)
    timer = null
  }
}

function reset() {
  pause()
  const mode = modes.find(m => m.key === currentMode.value)
  timeLeft.value = mode.duration
}

function complete() {
  pause()

  if (currentMode.value === 'work') {
    todayCount.value++
    currentRound.value++

    ElNotification({
      title: '🎉 完成一个番茄钟！',
      message: '休息一下吧',
      type: 'success',
      duration: 3000
    })

    // 自动切换到休息模式
    if (currentRound.value > 4) {
      currentRound.value = 1
      switchMode('longBreak')
    } else {
      switchMode('shortBreak')
    }
  } else {
    ElNotification({
      title: '⏰ 休息结束',
      message: '开始新的番茄钟吧！',
      type: 'info',
      duration: 3000
    })
    switchMode('work')
  }

  // 播放提示音
  playNotificationSound()
}

function playNotificationSound() {
  const audio = new Audio('data:audio/wav;base64,UklGRnoGAABXQVZFZm10IBAAAAABAAEAQB8AAEAfAAABAAgAZGF0YQoGAACBhYqFbF1fdJivrJBhNjVgodDbq2EcBj+a2/LDciUFLIHO8tiJNwgZaLvt559NEAxQp+PwtmMcBjiR1/LMeSwFJHfH8N2QQAoUXrTp66hVFApGn+DyvmwhBSuBzvLZiTYIGGS57OihUBELTKXh8bllHAU2jdXvzn0pBSh+zPDajzsKElyx6OyrWBUIQ5zd8sFuJAUuhM/z24k2CBhku+zooVARC0yl4fG5ZRwFNo3V7859KQUofsz=')
  audio.play().catch(() => {})
}

onUnmounted(() => {
  if (timer) {
    clearInterval(timer)
  }
})
</script>

<style scoped>
.pomodoro-container {
  min-height: calc(100vh - 112px);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
}

.pomodoro-card {
  background: white;
  border-radius: 32px;
  padding: 60px 80px;
  box-shadow: 0 20px 80px rgba(0, 0, 0, 0.3);
  width: 80vw;
  max-width: 1400px;
  height: 80vh;
  max-height: 800px;
  display: flex;
  gap: 80px;
  align-items: center;
}

.left-section {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.right-section {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.title {
  text-align: center;
  font-size: 48px;
  font-weight: 700;
  color: #2c3e50;
  margin-bottom: 48px;
}

.mode-tabs {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  margin-bottom: 48px;
  background: #f5f7fa;
  padding: 8px;
  border-radius: 16px;
}

.mode-tab {
  flex: 1;
  padding: 20px 32px;
  border: none;
  background: transparent;
  border-radius: 12px;
  font-size: 20px;
  font-weight: 600;
  color: #7f8c8d;
  cursor: pointer;
  transition: all 0.3s;
}

.mode-tab:hover {
  background: rgba(102, 126, 234, 0.1);
  color: #667eea;
}

.mode-tab.active {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  box-shadow: 0 4px 16px rgba(102, 126, 234, 0.4);
}

.timer-display {
  position: relative;
  width: 400px;
  max-width: min(70vw, 400px);
  height: 400px;
  margin: 0 auto 48px;
}

.progress-ring {
  transform: rotate(-90deg);
}

.progress-ring-bg {
  fill: none;
  stroke: #f5f7fa;
  stroke-width: 16;
}

.progress-ring-circle {
  fill: none;
  stroke: #667eea;
  stroke-width: 16;
  stroke-linecap: round;
  stroke-dasharray: 1131;
  transition: stroke-dashoffset 1s linear;
  opacity: 0.3;
}

.progress-ring-seconds {
  fill: none;
  stroke: #f5576c;
  stroke-width: 16;
  stroke-linecap: round;
  stroke-dasharray: 1131;
  transition: stroke-dashoffset 1s linear;
}

.time-text {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  font-size: 96px;
  font-weight: 700;
  color: #2c3e50;
  font-family: 'Courier New', monospace;
}

.controls {
  display: flex;
  gap: 20px;
}

.btn {
  flex: 1;
  padding: 24px 32px;
  border: none;
  border-radius: 16px;
  font-size: 22px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
}

.btn-start {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  box-shadow: 0 6px 20px rgba(102, 126, 234, 0.4);
}

.btn-start:hover {
  transform: translateY(-3px);
  box-shadow: 0 8px 24px rgba(102, 126, 234, 0.5);
}

.btn-pause {
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
  color: white;
  box-shadow: 0 6px 20px rgba(240, 147, 251, 0.4);
}

.btn-pause:hover {
  transform: translateY(-3px);
  box-shadow: 0 8px 24px rgba(240, 147, 251, 0.5);
}

.btn-reset {
  background: #f5f7fa;
  color: #7f8c8d;
}

.btn-reset:hover {
  background: #e9ecef;
  color: #495057;
  transform: translateY(-2px);
}

.stats {
  display: flex;
  gap: 32px;
  padding: 32px;
  background: linear-gradient(135deg, #f5f7fa 0%, #e9ecef 100%);
  border-radius: 16px;
  margin-bottom: 32px;
}

.stat-item {
  flex: 1;
  text-align: center;
}

.stat-label {
  font-size: 18px;
  color: #7f8c8d;
  margin-bottom: 12px;
}

.stat-value {
  font-size: 32px;
  font-weight: 700;
  color: #2c3e50;
}

.tips {
  text-align: center;
  color: #7f8c8d;
  font-size: 16px;
  line-height: 2;
}

.tips p {
  margin: 8px 0;
}

@media (max-width: 1200px) {
  .pomodoro-card {
    width: 90vw;
    height: auto;
    flex-direction: column;
    gap: 48px;
    padding: 48px 40px;
  }

  .timer-display {
    width: 320px;
    height: 320px;
    max-width: min(70vw, 320px);
  }

  .time-text {
    font-size: 72px;
  }

  .title {
    font-size: 36px;
  }
}

@media (max-width: 768px) {
  .pomodoro-container {
    min-height: calc(100vh - 90px);
    padding: 16px 10px 24px;
  }

  .pomodoro-card {
    padding: 24px 14px;
    width: 100%;
    border-radius: 20px;
    gap: 28px;
  }

  .title {
    font-size: 28px;
    margin-bottom: 32px;
  }

  .timer-display {
    width: min(72vw, 260px);
    height: min(72vw, 260px);
    margin-bottom: 20px;
  }

  .time-text {
    font-size: 52px;
  }

  .mode-tabs {
    margin-bottom: 18px;
    gap: 8px;
    padding: 6px;
  }

  .mode-tab {
    min-width: calc(50% - 4px);
    padding: 10px 12px;
    font-size: 14px;
  }

  .controls {
    flex-direction: column;
    margin-bottom: 20px;
    gap: 10px;
  }

  .btn {
    padding: 14px 18px;
    font-size: 16px;
  }

  .stats {
    flex-direction: column;
    gap: 20px;
    padding: 24px;
  }

  .stat-label {
    font-size: 14px;
  }

  .stat-value {
    font-size: 24px;
  }

  .tips {
    font-size: 14px;
    line-height: 1.7;
  }
}
</style>
