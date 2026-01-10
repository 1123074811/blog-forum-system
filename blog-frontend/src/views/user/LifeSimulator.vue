<template>
  <div class="life-simulator">
    <button class="back-btn" @click="$router.push('/discover')">← 返回</button>
    <button v-if="gameState === 'playing'" class="back-btn restart-btn" @click="restartGame">↺ 重新开始</button>
    <!-- 开始页面 -->
    <div v-if="gameState === 'start'" class="start-screen">
      <h1 class="pixel-title">人生重开模拟器</h1>
      <p class="pixel-subtitle">分配你的初始属性点（共20点）</p>

      <div class="attributes-panel">
        <div v-for="attr in attributeList" :key="attr.key" class="attr-row">
          <span class="attr-name">{{ attr.name }}</span>
          <button class="pixel-btn" @click="decreaseAttr(attr.key)" :disabled="attributes[attr.key] <= 0">-</button>
          <span class="attr-value">{{ attributes[attr.key] }}</span>
          <button class="pixel-btn" @click="increaseAttr(attr.key)" :disabled="remainingPoints <= 0 || attributes[attr.key] >= 10">+</button>
          <div class="attr-bar">
            <div class="attr-fill" :style="{ width: attributes[attr.key] * 10 + '%' }"></div>
          </div>
        </div>
      </div>

      <p class="remaining">剩余点数: {{ remainingPoints }}</p>

      <div class="btn-group">
        <button class="pixel-btn large" @click="randomAttributes">随机分配</button>
        <button class="pixel-btn large primary" @click="startGame" :disabled="remainingPoints > 0">开始人生</button>
        <button v-if="hasSave" class="pixel-btn large" @click="loadGame">继续游戏</button>
      </div>
    </div>

    <!-- 游戏页面 -->
    <div v-else-if="gameState === 'playing'" class="game-screen">
      <div class="game-layout">
        <!-- 左侧属性栏 -->
        <div class="side-panel">
          <div class="age-display">{{ age }}岁</div>
          <div class="attrs-list">
            <div v-for="attr in attributeList" :key="attr.key" class="attr-item">
              <span class="attr-icon">{{ attr.icon }}</span>
              <span class="attr-label">{{ attr.name }}</span>
              <div class="attr-bar-mini">
                <div class="attr-fill-mini" :style="{ width: currentAttrs[attr.key] + '%' }"></div>
              </div>
              <span class="attr-num">{{ Math.round(currentAttrs[attr.key]) }}</span>
            </div>
            <div class="attr-item wealth-item">
              <span class="attr-icon">💰</span>
              <span class="attr-label">财富</span>
              <span class="attr-num wealth-num">{{ formatMoney(wealth) }}</span>
            </div>
          </div>

          <!-- 职业信息 -->
          <div class="career-info">
            <div class="info-row"><span>📚</span> {{ career.education }}</div>
            <div class="info-row" v-if="career.job !== '无'"><span>💼</span> {{ career.job }}</div>
          </div>

          <div class="side-actions">
            <button class="pixel-btn" @click="saveGame">保存</button>
          </div>
        </div>

        <!-- 右侧主区域 -->
        <div class="main-area">
          <div class="events-container" ref="eventsContainer">
            <div v-for="(event, index) in events" :key="index" class="event-item" :class="event.type">
              <span class="event-age">{{ event.age }}岁</span>
              <span class="event-text">{{ event.text }}</span>
              <div v-if="event.effects" class="event-effects">
                <span v-for="(value, key) in event.effects" :key="key" :class="value > 0 ? 'positive' : 'negative'">
                  {{ getAttrName(key) }}{{ value > 0 ? '+' : '' }}{{ key === 'wealth' ? formatMoney(value) : value }}
                </span>
              </div>
            </div>
          </div>

          <!-- 选择事件 -->
          <div v-if="currentChoice" class="choice-panel">
            <p class="choice-question">{{ currentChoice.question }}</p>
            <div class="choice-options">
              <button v-for="(opt, idx) in currentChoice.options" :key="idx" class="pixel-btn choice-btn" @click="makeChoice(idx)">
                {{ opt.text }}
              </button>
            </div>
          </div>

          <div v-else class="action-bar">
            <button class="pixel-btn large primary" @click="nextYear" :disabled="loading">
              {{ loading ? '命运转动中...' : '下一年' }}
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- 结束页面 -->
    <div v-else-if="gameState === 'end'" class="end-screen">
      <h1 class="pixel-title">人生结束</h1>
      <p class="death-age">享年 {{ age }} 岁</p>
      <p class="death-reason">{{ deathReason }}</p>

      <div class="summary">
        <h3>数据统计</h3>
        <p>最高颜值: {{ summary.maxLooks }}</p>
        <p>最高智力: {{ summary.maxIntelligence }}</p>
        <p>最高财富: {{ formatMoney(summary.maxWealth) }}</p>
        <p>最终学历: {{ career.education }}</p>
        <p>最终职业: {{ career.job }}</p>
        <p>经历事件: {{ events.length }} 件</p>
      </div>

      <button class="pixel-btn large primary" @click="restart">重开人生</button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'

const gameState = ref('start')
const age = ref(0)
const loading = ref(false)
const hasSave = ref(false)
const deathReason = ref('')
const eventsContainer = ref(null)

const attributeList = [
  { key: 'looks', name: '颜值', icon: '😊' },
  { key: 'intelligence', name: '智力', icon: '🧠' },
  { key: 'physique', name: '体质', icon: '💪' },
  { key: 'mental', name: '心理', icon: '❤️' },
  { key: 'family', name: '家境', icon: '🏠' }
]

const attributes = ref({ looks: 0, intelligence: 0, physique: 0, mental: 0, family: 0 })
const currentAttrs = ref({ looks: 0, intelligence: 0, physique: 0, mental: 0, family: 0 })
const wealth = ref(0)
const events = ref([])
const currentChoice = ref(null)
const summary = ref({ maxLooks: 0, maxIntelligence: 0, maxWealth: 0 })

// 职业系统
const career = ref({ education: '无', job: '无', salary: 0 })

const remainingPoints = computed(() => {
  return 20 - Object.values(attributes.value).reduce((a, b) => a + b, 0)
})

function increaseAttr(key) {
  if (remainingPoints.value > 0 && attributes.value[key] < 10) {
    attributes.value[key]++
  }
}

function decreaseAttr(key) {
  if (attributes.value[key] > 0) {
    attributes.value[key]--
  }
}

function randomAttributes() {
  const keys = Object.keys(attributes.value)
  keys.forEach(k => attributes.value[k] = 0)
  let points = 20
  while (points > 0) {
    const key = keys[Math.floor(Math.random() * keys.length)]
    if (attributes.value[key] < 10) {
      attributes.value[key]++
      points--
    }
  }
}

function startGame() {
  if (remainingPoints.value > 0) return

  Object.assign(currentAttrs.value, attributes.value)
  wealth.value = attributes.value.family * 10000
  age.value = 0
  events.value = []
  summary.value = { maxLooks: attributes.value.looks, maxIntelligence: attributes.value.intelligence, maxWealth: wealth.value }

  // 初始化职业
  career.value = { education: '无', job: '无', salary: 0 }

  gameState.value = 'playing'

  events.value.push({
    age: 0,
    text: `你出生了。家境${attributes.value.family > 7 ? '优渥' : attributes.value.family > 4 ? '普通' : '贫寒'}。`,
    type: 'birth'
  })
}

// 压缩事件历史，只保留关键事件
function compressEvents() {
  const keyEvents = events.value
    .filter(e => e.type === 'choice' || e.type === 'birth' || (e.effects && Object.values(e.effects).some(v => Math.abs(v) >= 5 || (typeof v === 'number' && Math.abs(v) >= 10000))))
    .map(e => `${e.age}岁:${e.text}`)
    .slice(-10)

  return keyEvents
}

async function nextYear() {
  if (loading.value) return
  loading.value = true

  age.value++

  try {
    const response = await fetch('/api/life-simulator/event', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        age: age.value,
        attributes: currentAttrs.value,
        wealth: wealth.value,
        recentEvents: compressEvents()
      })
    })

    if (response.ok) {
      const result = await response.json()
      if (result.success && result.data) {
        processEvent(result.data)
      } else {
        processEvent(generateLocalEvent())
      }
    } else {
      processEvent(generateLocalEvent())
    }
  } catch (error) {
    console.warn('AI事件生成失败，使用本地事件:', error)
    processEvent(generateLocalEvent())
  }

  loading.value = false
  checkDeath()
  updateSummary()
  scrollToBottom()
}

function processEvent(eventData) {
  if (eventData.choice) {
    currentChoice.value = eventData.choice
  } else if (eventData.text) {
    events.value.push({
      age: age.value,
      text: eventData.text,
      type: eventData.type || 'normal',
      effects: eventData.effects
    })
    applyEffects(eventData.effects)

    // 处理职业变化
    if (eventData.career) {
      if (eventData.career.education) career.value.education = eventData.career.education
      if (eventData.career.job) career.value.job = eventData.career.job
      if (eventData.career.salary) career.value.salary = eventData.career.salary
    }
  } else {
    // AI返回数据无效，使用本地事件
    const localEvent = generateLocalEvent()
    events.value.push({
      age: age.value,
      text: localEvent.text,
      type: 'normal',
      effects: localEvent.effects
    })
    applyEffects(localEvent.effects)
  }
}

function makeChoice(idx) {
  const choice = currentChoice.value
  const selected = choice.options[idx]

  events.value.push({
    age: age.value,
    text: `${choice.question} → ${selected.text}`,
    type: 'choice',
    effects: selected.effects
  })

  applyEffects(selected.effects)
  currentChoice.value = null
  checkDeath()
  updateSummary()
  scrollToBottom()
}

function applyEffects(effects) {
  if (!effects) return
  for (const [key, value] of Object.entries(effects)) {
    if (key === 'wealth') {
      wealth.value += value
    } else if (currentAttrs.value[key] !== undefined) {
      currentAttrs.value[key] = Math.max(0, Math.min(100, currentAttrs.value[key] + value))
    }
  }
}

function checkDeath() {
  const maxAge = 60 + Math.floor(Math.random() * 30) + Math.floor(currentAttrs.value.physique / 5) + Math.floor(currentAttrs.value.mental / 10)

  if (age.value >= maxAge) {
    deathReason.value = '寿终正寝'
    gameState.value = 'end'
    localStorage.removeItem('lifeSimulatorSave')
    return
  }

  // 体质或心理过低时有概率死亡，而不是立即死亡
  if (currentAttrs.value.physique <= 0) {
    const deathChance = Math.abs(currentAttrs.value.physique) * 5 // 每低1点增加5%概率
    if (Math.random() * 100 < deathChance) {
      deathReason.value = '身体衰竭'
      gameState.value = 'end'
      localStorage.removeItem('lifeSimulatorSave')
      return
    }
  }

  if (currentAttrs.value.mental <= 0) {
    const deathChance = Math.abs(currentAttrs.value.mental) * 5
    if (Math.random() * 100 < deathChance) {
      deathReason.value = '心理崩溃'
      gameState.value = 'end'
      localStorage.removeItem('lifeSimulatorSave')
    }
  }
}

function updateSummary() {
  summary.value.maxLooks = Math.max(summary.value.maxLooks, currentAttrs.value.looks)
  summary.value.maxIntelligence = Math.max(summary.value.maxIntelligence, currentAttrs.value.intelligence)
  summary.value.maxWealth = Math.max(summary.value.maxWealth, wealth.value)
}

function generateLocalEvent() {
  const templates = [
    { text: '平淡的一年过去了。', effects: null },
    { text: '你感冒了，休息了几天。', effects: { physique: -1 } },
    { text: '你读了一本好书。', effects: { intelligence: 1 } },
    { text: '你交了新朋友。', effects: { mental: 1 } },
    { text: '你找到了一份兼职。', effects: { wealth: 5000 } },
    { text: '你生了一场病。', effects: { physique: -2, wealth: -3000 } },
    { text: '你学会了一项新技能。', effects: { intelligence: 2 } },
  ]
  return templates[Math.floor(Math.random() * templates.length)]
}

function saveGame() {
  const saveData = {
    age: age.value,
    attributes: attributes.value,
    currentAttrs: currentAttrs.value,
    wealth: wealth.value,
    events: events.value,
    summary: summary.value,
    career: career.value
  }
  localStorage.setItem('lifeSimulatorSave', JSON.stringify(saveData))
  ElMessage.success('保存成功')
}

function restartGame() {
  gameState.value = 'start'
  age.value = 0
  events.value = []
  currentChoice.value = null
  Object.keys(attributes.value).forEach(k => attributes.value[k] = 0)
  Object.keys(currentAttrs.value).forEach(k => currentAttrs.value[k] = 0)
  wealth.value = 0
}

function loadGame() {
  const saveData = JSON.parse(localStorage.getItem('lifeSimulatorSave'))
  if (saveData) {
    age.value = saveData.age
    attributes.value = saveData.attributes
    currentAttrs.value = saveData.currentAttrs
    wealth.value = saveData.wealth
    events.value = saveData.events
    summary.value = saveData.summary
    career.value = saveData.career || { education: '无', job: '无', salary: 0 }
    gameState.value = 'playing'
  }
}

function restart() {
  gameState.value = 'start'
  age.value = 0
  events.value = []
  Object.keys(attributes.value).forEach(k => attributes.value[k] = 0)
}

function formatMoney(value) {
  if (Math.abs(value) >= 10000) {
    return (value / 10000).toFixed(1) + '万'
  }
  return value + '元'
}

function getAttrName(key) {
  const attr = attributeList.find(a => a.key === key)
  return attr ? attr.name : key === 'wealth' ? '财富' : key
}

function scrollToBottom() {
  nextTick(() => {
    if (eventsContainer.value) {
      eventsContainer.value.scrollTop = eventsContainer.value.scrollHeight
    }
  })
}

onMounted(() => {
  hasSave.value = !!localStorage.getItem('lifeSimulatorSave')
})
</script>

<style scoped>
.life-simulator {
  height: calc(100vh - 112px);
  background: #1a1a2e;
  color: #eee;
  font-family: 'Courier New', monospace;
  padding: 20px;
  box-sizing: border-box;
  position: relative;
}

.back-btn {
  position: absolute;
  top: 10px;
  left: 10px;
  background: transparent;
  border: 2px solid #00ff88;
  color: #00ff88;
  padding: 6px 12px;
  cursor: pointer;
  font-family: inherit;
  min-width: 100px;
  text-align: center;
  white-space: nowrap;
}

.back-btn:hover {
  background: #00ff88;
  color: #1a1a2e;
}

.restart-btn {
  top: 70px;
}

.pixel-title {
  font-size: 32px;
  text-align: center;
  color: #00ff88;
  text-shadow: 3px 3px 0 #005533;
  margin-bottom: 10px;
}

.pixel-subtitle {
  text-align: center;
  color: #888;
  margin-bottom: 30px;
}

.start-screen, .end-screen {
  max-width: 500px;
  margin: 0 auto;
  padding-top: 50px;
}

.attributes-panel {
  background: #252540;
  border: 3px solid #00ff88;
  padding: 20px;
  margin-bottom: 20px;
}

.attr-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 15px;
}

.attr-name {
  width: 50px;
  color: #00ff88;
}

.attr-value {
  width: 30px;
  text-align: center;
  font-size: 18px;
  color: #fff;
}

.attr-bar {
  flex: 1;
  height: 12px;
  background: #333;
  border: 2px solid #555;
}

.attr-fill {
  height: 100%;
  background: linear-gradient(90deg, #00ff88, #00cc66);
  transition: width 0.2s;
}

.pixel-btn {
  background: #333;
  border: 2px solid #00ff88;
  color: #00ff88;
  padding: 8px 16px;
  cursor: pointer;
  font-family: inherit;
  font-size: 14px;
  transition: all 0.1s;
}

.pixel-btn:hover:not(:disabled) {
  background: #00ff88;
  color: #1a1a2e;
}

.pixel-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.pixel-btn.large {
  padding: 12px 24px;
  font-size: 16px;
}

.pixel-btn.primary {
  background: #00ff88;
  color: #1a1a2e;
}

.pixel-btn.primary:hover:not(:disabled) {
  background: #00cc66;
}

.remaining {
  text-align: center;
  font-size: 18px;
  color: #ffcc00;
  margin-bottom: 20px;
}

.btn-group {
  display: flex;
  gap: 15px;
  justify-content: center;
  margin-bottom: 15px;
}

.game-screen {
  height: 100%;
}

.game-layout {
  display: flex;
  gap: 20px;
  height: 100%;
  max-width: 900px;
  margin: 0 auto;
}

.side-panel {
  width: 180px;
  background: #252540;
  border: 3px solid #00ff88;
  padding: 15px;
  display: flex;
  flex-direction: column;
}

.age-display {
  font-size: 28px;
  color: #00ff88;
  font-weight: bold;
  text-align: center;
  margin-bottom: 20px;
  padding-bottom: 15px;
  border-bottom: 2px solid #333;
}

.attrs-list {
  flex: 1;
}

.attr-item {
  margin-bottom: 12px;
}

.attr-icon {
  font-size: 16px;
  margin-right: 5px;
}

.attr-label {
  color: #888;
  font-size: 12px;
}

.attr-bar-mini {
  height: 8px;
  background: #333;
  border: 1px solid #555;
  margin: 4px 0;
}

.attr-fill-mini {
  height: 100%;
  background: linear-gradient(90deg, #00ff88, #00cc66);
  transition: width 0.3s;
}

.attr-num {
  color: #00ff88;
  font-size: 14px;
  font-weight: bold;
}

.wealth-item {
  margin-top: 15px;
  padding-top: 15px;
  border-top: 2px solid #333;
}

.wealth-num {
  display: block;
  font-size: 16px;
  margin-top: 5px;
}

.side-actions {
  margin-top: 15px;
  padding-top: 15px;
  border-top: 2px solid #333;
}

.side-actions .pixel-btn {
  width: 100%;
}

.main-area {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.events-container {
  flex: 1;
  overflow-y: auto;
  background: #252540;
  border: 3px solid #555;
  padding: 15px;
  margin-bottom: 15px;
}

.event-item {
  padding: 10px;
  margin-bottom: 10px;
  border-left: 3px solid #555;
  background: #1a1a2e;
}

.event-item.birth {
  border-left-color: #00ff88;
}

.event-item.choice {
  border-left-color: #ffcc00;
}

.event-age {
  color: #00ff88;
  margin-right: 10px;
  font-weight: bold;
}

.event-text {
  color: #ddd;
}

.event-effects {
  margin-top: 5px;
  font-size: 12px;
}

.event-effects .positive {
  color: #00ff88;
  margin-right: 10px;
}

.event-effects .negative {
  color: #ff4444;
  margin-right: 10px;
}

.choice-panel {
  background: #252540;
  border: 3px solid #ffcc00;
  padding: 20px;
}

.choice-question {
  color: #ffcc00;
  margin-bottom: 15px;
  font-size: 16px;
}

.choice-options {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.choice-btn {
  border-color: #ffcc00;
  color: #ffcc00;
}

.choice-btn:hover {
  background: #ffcc00;
  color: #1a1a2e;
}

.action-bar {
  display: flex;
  gap: 15px;
  justify-content: center;
}

.end-screen {
  text-align: center;
}

.death-age {
  font-size: 48px;
  color: #ff4444;
  margin: 20px 0;
}

.death-reason {
  font-size: 20px;
  color: #888;
  margin-bottom: 30px;
}

.summary {
  background: #252540;
  border: 3px solid #555;
  padding: 20px;
  margin-bottom: 30px;
  text-align: left;
}

.summary h3 {
  color: #00ff88;
  margin-bottom: 15px;
}

.summary p {
  margin-bottom: 8px;
  color: #aaa;
}

/* 职业信息样式 */
.career-info {
  padding: 10px 0;
  border-top: 2px solid #333;
  margin-top: 10px;
}

.info-row {
  color: #aaa;
  font-size: 12px;
  margin-bottom: 5px;
}
</style>
