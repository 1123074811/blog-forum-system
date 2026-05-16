<template>
  <div class="recitation-page">
    <!-- 主内容区 -->
    <div class="main-content">
      <!-- 输入区 -->
      <div v-if="!practicing" class="input-section">
        <el-input v-model="title" placeholder="标题（可选）" class="title-input" />
        <el-input v-model="content" type="textarea" :rows="8" placeholder="粘贴要背诵的内容..." class="content-input" />
        <div class="actions">
          <el-button type="primary" @click="startPractice" :disabled="!content.trim()">开始练习</el-button>
          <el-button @click="showHistory = true">历史记录</el-button>
        </div>
      </div>

      <!-- 练习区 -->
      <div v-else class="practice-section">
        <div class="practice-header">
          <span class="title">{{ currentRecord?.title || '背诵练习' }}</span>
          <div class="controls">
            <el-switch v-model="timingEnabled" active-text="计时" @change="toggleTiming" />
            <span v-if="timingEnabled" class="timer">{{ formatTime(duration) }}</span>
            <el-dropdown @command="handleModeChange">
              <el-button size="small">{{ modeText }}<el-icon class="el-icon--right"><ArrowDown /></el-icon></el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="show">显示原文</el-dropdown-item>
                  <el-dropdown-item command="hide">隐藏原文</el-dropdown-item>
                  <el-dropdown-item command="pinyin">拼音提示</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </div>

        <!-- 原文显示区 -->
        <div class="text-display">
          <span v-for="(char, i) in contentChars" :key="i" :class="getCharClass(i)">
            <template v-if="mode === 'pinyin' && i >= userInput.length">
              {{ getPinyin(char) }}
            </template>
            <template v-else>{{ getDisplayChar(char, i) }}</template>
          </span>
        </div>

        <!-- 输入区 -->
        <textarea ref="inputArea" v-model="userInput" @input="onInput" @keydown="onKeydown" class="input-area" placeholder="开始输入..." autofocus></textarea>

        <!-- 进度 -->
        <div class="progress-bar">
          <el-progress :percentage="progressPercent" :stroke-width="10" />
          <span>{{ userInput.length }} / {{ contentChars.length }}</span>
        </div>

        <div class="practice-actions">
          <el-button @click="exitPractice">退出</el-button>
        </div>
      </div>
    </div>

    <!-- 历史记录抽屉 -->
    <el-drawer v-model="showHistory" title="历史记录" size="400px">
      <div v-if="historyList.length === 0" class="empty">暂无记录</div>
      <div v-else class="history-list">
        <div v-for="item in historyList" :key="item.id" class="history-item" @click="confirmLoadRecord(item)">
          <div class="info">
            <span class="name">{{ getRecordTitle(item) }}</span>
            <span v-if="!item.title" class="content-preview">{{ getContentPreview(item.content) }}</span>
            <span class="time">{{ item.updatedAt || item.createdAt }}</span>
          </div>
          <div class="meta">
            <el-tag v-if="item.isLocal" type="info" size="small">本地</el-tag>
            <el-tag :type="item.completed ? 'success' : 'warning'" size="small">
              {{ item.completed ? '已完成' : `${getProgressPercent(item)}%` }}
            </el-tag>
            <el-button size="small" type="danger" text @click.stop="deleteRecord(item)">删除</el-button>
          </div>
        </div>
      </div>
    </el-drawer>

    <!-- 完成弹窗 -->
    <el-dialog v-model="showComplete" title="恭喜完成！" width="300px" center>
      <div class="complete-info">
        <p>用时：{{ formatTime(duration) }}</p>
        <p>字数：{{ contentChars.length }}</p>
      </div>
      <template #footer>
        <el-button type="primary" @click="finishPractice">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { ArrowDown } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { pinyin } from 'pinyin-pro'
import api from '@/api'

// 状态数据
const title = ref('')
const content = ref('')
const userInput = ref('')
const practicing = ref(false)
const mode = ref('show')
const timingEnabled = ref(false)
const duration = ref(0)
const showHistory = ref(false)
const showComplete = ref(false)
const historyList = ref([])
const currentRecord = ref(null)
const inputArea = ref(null)

// 本地缓存的练习记录
const localRecord = ref(null)

let timer = null
let autoSaveTimer = null

// 过滤掉空格的内容
const filteredContent = computed(() => content.value.replace(/\s/g, ''))
const contentChars = computed(() => filteredContent.value.split(''))

// 计算正确字符数量
const correctCharsCount = computed(() => {
  let count = 0
  const inputLen = userInput.value.length
  const contentLen = contentChars.value.length
  const minLen = Math.min(inputLen, contentLen)
  
  for (let i = 0; i < minLen; i++) {
    if (userInput.value[i] === contentChars.value[i]) {
      count++
    }
  }
  return count
})

// 进度百分比：基于正确字符数量
const progressPercent = computed(() => {
  if (!contentChars.value.length) return 0
  return Math.round(correctCharsCount.value / contentChars.value.length * 100)
})

const modeText = computed(() => ({ show: '显示原文', hide: '隐藏原文', pinyin: '拼音提示' }[mode.value]))

// 本地缓存函数
const saveToLocalCache = () => {
  if (!localRecord.value) return
  
  const cacheData = {
    ...localRecord.value,
    progress: correctCharsCount.value, // 保存正确字符数量作为进度
    duration: duration.value,
    userInput: userInput.value, // 保存完整的用户输入
    updatedAt: new Date().toLocaleString()
  }
  
  console.log('保存到本地缓存:', cacheData) // 调试信息
  localStorage.setItem(`recitation_${localRecord.value.id}`, JSON.stringify(cacheData))
}

const loadFromLocalCache = (id) => {
  const cached = localStorage.getItem(`recitation_${id}`)
  return cached ? JSON.parse(cached) : null
}

const removeFromLocalCache = (id) => {
  localStorage.removeItem(`recitation_${id}`)
}

const getCharClass = (i) => {
  if (i >= userInput.value.length) return mode.value === 'hide' ? 'char hidden' : 'char pending'
  return userInput.value[i] === contentChars.value[i] ? 'char correct' : 'char wrong'
}

const getDisplayChar = (char, i) => {
  if (i < userInput.value.length) return char
  if (mode.value === 'hide') return '○'
  return char
}

const getPinyin = (char) => {
  // 如果是中文字符，返回拼音（不带声调数字）
  if (/[\u4e00-\u9fa5]/.test(char)) {
    return pinyin(char, { toneType: 'none' }) + ' '
  }
  // 如果是标点符号或其他字符，直接返回原字符
  return char
}

const onInput = () => {
  const len = userInput.value.length
  console.log('输入变化，当前长度:', len) // 调试信息
  
  // 自动保存到本地缓存和数据库（防抖，3秒后保存）
  if (localRecord.value || currentRecord.value) {
    if (autoSaveTimer) {
      clearTimeout(autoSaveTimer)
    }
    autoSaveTimer = setTimeout(async () => {
      console.log('自动保存进度') // 调试信息
      // 先保存到本地缓存（快速响应）
      saveToLocalCache()
      // 再保存到数据库（持久化）
      await autoSaveToDatabase()
    }, 3000)
  }
  
  if (len === contentChars.value.length && userInput.value === filteredContent.value) {
    completeTask()
  }
}

const onKeydown = (e) => {
  // 允许所有输入，包括错误的输入
  // 用户可以自由输入，即使打错字也可以继续
}

const handleModeChange = (cmd) => { mode.value = cmd }

const toggleTiming = (val) => {
  if (val) {
    timer = setInterval(() => {
      duration.value++
      // 同步计时到 localRecord
      if (localRecord.value) {
        localRecord.value.duration = duration.value
      }
    }, 1000)
  } else if (timer) {
    clearInterval(timer)
    timer = null
  }
}

const formatTime = (s) => {
  const m = Math.floor(s / 60)
  const sec = s % 60
  return `${m.toString().padStart(2, '0')}:${sec.toString().padStart(2, '0')}`
}

const startPractice = async () => {
  // 检查是否已存在相同内容的记录（仅在首次开始时检查）
  const existingRecord = await findExistingRecord(content.value)
  
  if (existingRecord && !practicing.value) {
    // 计算进度百分比
    const contentLength = (existingRecord.content || '').replace(/\s/g, '').length
    const progressPercent = contentLength > 0 
      ? Math.round((existingRecord.progress || 0) / contentLength * 100) 
      : 0
    
    // 如果存在相同内容的记录，询问用户是否继续上次的进度
    ElMessageBox.confirm(
      `检测到相同内容的历史记录，进度：${progressPercent}%`,
      '提示',
      {
        confirmButtonText: '继续上次进度',
        cancelButtonText: '重新开始',
        type: 'info'
      }
    ).then(() => {
      // 用户选择继续
      loadRecord(existingRecord)
    }).catch(() => {
      // 用户选择重新开始
      createNewRecord()
    })
    return
  }
  
  // 创建新记录
  createNewRecord()
}

// 创建新的练习记录
const createNewRecord = () => {
  // 创建本地练习记录
  const recordId = Date.now() // 使用时间戳作为临时ID
  localRecord.value = {
    id: recordId,
    title: title.value,
    content: content.value,
    timingEnabled: timingEnabled.value,
    progress: 0,
    duration: 0,
    completed: false,
    createdAt: new Date().toLocaleString()
  }
  
  practicing.value = true
  userInput.value = ''
  duration.value = 0
  
  // 立即保存到本地缓存
  saveToLocalCache()
  
  if (timingEnabled.value) toggleTiming(true)
}

// 查找是否已存在相同内容的记录
const findExistingRecord = async (contentToCheck) => {
  if (!contentToCheck || !contentToCheck.trim()) return null
  
  // 规范化内容（去除空格和换行）
  const normalizedContent = contentToCheck.replace(/\s/g, '')
  
  // 检查本地缓存
  for (let i = 0; i < localStorage.length; i++) {
    const key = localStorage.key(i)
    if (key && key.startsWith('recitation_')) {
      try {
        const cached = JSON.parse(localStorage.getItem(key))
        const cachedNormalized = (cached.content || '').replace(/\s/g, '')
        if (cachedNormalized === normalizedContent) {
          return { ...cached, isLocal: true }
        }
      } catch (e) {
        // 忽略解析错误
      }
    }
  }
  
  // 检查数据库记录
  try {
    const res = await api.get('/recitation', { params: { page: 1, limit: 50 } })
    const dbRecords = res.data?.data || []
    for (const record of dbRecords) {
      const recordNormalized = (record.content || '').replace(/\s/g, '')
      if (recordNormalized === normalizedContent) {
        return record
      }
    }
  } catch (e) {
    console.error('检查数据库记录失败:', e)
  }
  
  return null
}

// 自动保存到数据库（输入过程中）
const autoSaveToDatabase = async () => {
  try {
    const recordData = {
      title: title.value,
      content: content.value,
      userInput: userInput.value, // 保存用户输入
      timingEnabled: timingEnabled.value,
      progress: correctCharsCount.value, // 保存正确字符数量
      duration: duration.value,
      completed: false
    }
    
    // 如果有数据库记录，直接更新
    if (currentRecord.value) {
      await api.put(`/recitation/${currentRecord.value.id}`, {
        userInput: recordData.userInput,
        progress: recordData.progress,
        duration: recordData.duration,
        completed: recordData.completed
      })
      console.log('数据库记录已更新')
    } 
    // 如果有本地记录但没有数据库记录，创建数据库记录
    else if (localRecord.value) {
      const res = await api.post('/recitation', recordData)
      currentRecord.value = res.data
      // 更新本地记录的ID为数据库ID
      const oldId = localRecord.value.id
      localRecord.value.id = res.data.id
      // 删除旧的本地缓存，用新ID保存
      removeFromLocalCache(oldId)
      saveToLocalCache()
      console.log('已创建数据库记录')
    }
    return true
  } catch (e) {
    console.error('自动保存到数据库失败:', e)
    return false
  }
}

// 手动保存到数据库（退出或完成时）
const savePracticeToDatabase = async (recordData) => {
  try {
    // 如果是新记录，先创建
    if (!currentRecord.value) {
      const res = await api.post('/recitation', {
        title: recordData.title,
        content: recordData.content,
        userInput: recordData.userInput, // 保存用户输入
        timingEnabled: recordData.timingEnabled,
        progress: recordData.progress,
        duration: recordData.duration,
        completed: recordData.completed
      })
      currentRecord.value = res.data
      // 同步本地记录ID
      if (localRecord.value) {
        const oldId = localRecord.value.id
        localRecord.value.id = res.data.id
        removeFromLocalCache(oldId)
      }
    } else {
      // 更新现有记录
      await api.put(`/recitation/${currentRecord.value.id}`, {
        userInput: recordData.userInput, // 更新用户输入
        progress: recordData.progress,
        duration: recordData.duration,
        completed: recordData.completed
      })
    }
    return true
  } catch (e) {
    console.error('保存到数据库失败:', e)
    return false
  }
}

const exitPractice = async () => {
  // 退出时保存进度到数据库和本地缓存
  if (localRecord.value || currentRecord.value) {
    // 先保存到本地缓存
    saveToLocalCache()
    
    // 再保存到数据库
    const recordData = {
      title: title.value,
      content: content.value,
      userInput: userInput.value, // 保存用户输入
      timingEnabled: timingEnabled.value,
      progress: correctCharsCount.value, // 保存正确字符数量
      duration: duration.value,
      completed: false
    }
    
    const success = await savePracticeToDatabase(recordData)
    if (success) {
      ElMessage.success('练习已保存到数据库')
      // 保存成功后清除本地缓存
      if (localRecord.value) {
        removeFromLocalCache(localRecord.value.id)
      }
    } else {
      ElMessage.warning('已保存到本地，下次将同步到数据库')
    }
  }
  
  practicing.value = false
  if (timer) { clearInterval(timer); timer = null }
  if (autoSaveTimer) { clearTimeout(autoSaveTimer); autoSaveTimer = null }
  content.value = ''
  title.value = ''
  currentRecord.value = null
  localRecord.value = null
}

const completeTask = async () => {
  if (timer) { clearInterval(timer); timer = null }
  
  // 标记为完成并保存到数据库和本地缓存
  const recordData = {
    title: title.value,
    content: content.value,
    userInput: userInput.value, // 保存用户输入
    timingEnabled: timingEnabled.value,
    progress: correctCharsCount.value, // 保存正确字符数量
    duration: duration.value,
    completed: true
  }
  
  // 保存到本地缓存
  if (localRecord.value) {
    localRecord.value.completed = true
    localRecord.value.progress = correctCharsCount.value
    localRecord.value.duration = duration.value
    saveToLocalCache()
  }
  
  // 保存到数据库
  await savePracticeToDatabase(recordData)
  
  // 保存成功后清除本地缓存
  if (localRecord.value) {
    removeFromLocalCache(localRecord.value.id)
  }
  
  showComplete.value = true
}

const finishPractice = () => {
  showComplete.value = false
  // 完成后不重置状态，保持在当前练习页面
  // practicing.value = false
  // content.value = ''
  // title.value = ''
  // currentRecord.value = null
}

const loadHistory = async () => {
  try {
    const res = await api.get('/recitation', { params: { page: 1, limit: 50 } })
    console.log('API响应:', res) // 调试信息
    const dbRecords = res.data?.data || [] // 修复：使用data而不是list
    console.log('数据库记录:', dbRecords) // 调试信息
    
    // 获取本地缓存的记录
    const localRecords = []
    for (let i = 0; i < localStorage.length; i++) {
      const key = localStorage.key(i)
      if (key && key.startsWith('recitation_')) {
        try {
          const cached = JSON.parse(localStorage.getItem(key))
          localRecords.push({
            ...cached,
            isLocal: true // 标记为本地记录
          })
        } catch (e) {
          // 忽略解析错误的缓存
        }
      }
    }
    console.log('本地记录:', localRecords) // 调试信息
    
    // 合并数据库记录和本地记录
    historyList.value = [...localRecords, ...dbRecords]
      .sort((a, b) => new Date(b.updatedAt || b.createdAt) - new Date(a.updatedAt || a.createdAt))
    console.log('合并后的历史记录:', historyList.value) // 调试信息
  } catch (e) {
    console.error('加载历史记录失败:', e) // 调试信息
    // 如果网络失败，只显示本地记录
    const localRecords = []
    for (let i = 0; i < localStorage.length; i++) {
      const key = localStorage.key(i)
      if (key && key.startsWith('recitation_')) {
        try {
          const cached = JSON.parse(localStorage.getItem(key))
          localRecords.push({
            ...cached,
            isLocal: true
          })
        } catch (e) {
          // 忽略解析错误的缓存
        }
      }
    }
    historyList.value = localRecords
  }
}

const loadRecord = (item) => {
  content.value = item.content
  title.value = item.title || ''
  // 优先使用保存的 userInput，如果不存在则从头开始
  userInput.value = item.userInput || ''
  duration.value = item.duration || 0
  timingEnabled.value = item.timingEnabled || false
  
  if (item.isLocal) {
    // 加载本地记录（将其转换为既有本地缓存又有数据库记录的状态）
    localRecord.value = { ...item }
    currentRecord.value = null
    
    // 尝试立即同步到数据库
    setTimeout(async () => {
      await autoSaveToDatabase()
    }, 1000)
  } else {
    // 加载数据库记录
    currentRecord.value = item
    // 同时创建本地缓存作为备份
    localRecord.value = { 
      ...item,
      id: item.id,
      isLocal: false
    }
    saveToLocalCache()
  }
  
  practicing.value = true
  showHistory.value = false
  if (timingEnabled.value) toggleTiming(true)
}

// 确认加载记录（防止意外清空当前输入）
const confirmLoadRecord = async (item) => {
  // 如果当前正在练习且有输入，需要确认
  if (practicing.value && userInput.value.length > 0) {
    try {
      await ElMessageBox.confirm(
        '加载历史记录将会覆盖当前的练习内容，是否继续？',
        '提示',
        {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        }
      )
      // 用户确认，先保存当前进度
      if (localRecord.value) {
        saveToLocalCache()
      }
      loadRecord(item)
    } catch {
      // 用户取消，不做任何操作
    }
  } else {
    // 没有当前输入，直接加载
    loadRecord(item)
  }
}

// 计算记录的进度百分比
const getProgressPercent = (item) => {
  if (!item || !item.content) return 0
  // 去除空格后的内容长度
  const contentLength = item.content.replace(/\s/g, '').length
  if (contentLength === 0) return 0
  // progress 是正确字符数
  return Math.round((item.progress || 0) / contentLength * 100)
}

const deleteRecord = async (item) => {
  try {
    if (item.isLocal) {
      // 删除本地记录
      removeFromLocalCache(item.id)
      historyList.value = historyList.value.filter(x => x.id !== item.id)
      ElMessage.success('已删除')
    } else {
      // 删除数据库记录
      await api.delete(`/recitation/${item.id}`)
      historyList.value = historyList.value.filter(x => x.id !== item.id)
      ElMessage.success('已删除')
    }
  } catch (e) {
    ElMessage.error('删除失败')
  }
}

// 获取记录标题（有标题显示标题，无标题显示“未命名”）
const getRecordTitle = (item) => {
  return item.title || '未命名'
}

// 获取内容预览（前30个字符）
const getContentPreview = (content) => {
  if (!content) return ''
  const preview = content.replace(/\s/g, '').substring(0, 30)
  return preview.length < content.replace(/\s/g, '').length ? preview + '...' : preview
}

// 在页面卸载前保存进度
const handleBeforeUnload = () => {
  if (localRecord.value && practicing.value) {
    saveToLocalCache()
  }
}

// 恢复上次未完成的练习
const restoreLastPractice = () => {
  // 如果已经在练习中，不要自动恢复（避免打断用户）
  if (practicing.value) {
    return
  }
  
  // 查找所有本地缓存的记录
  const localRecords = []
  for (let i = 0; i < localStorage.length; i++) {
    const key = localStorage.key(i)
    if (key && key.startsWith('recitation_')) {
      try {
        const cached = JSON.parse(localStorage.getItem(key))
        if (!cached.completed) {
          localRecords.push({
            ...cached,
            isLocal: true // 标记为本地记录
          })
        }
      } catch (e) {
        // 忽略解析错误的缓存
      }
    }
  }
  
  // 如果有未完成的记录，询问用户是否恢复
  if (localRecords.length > 0) {
    // 不自动恢复，让用户通过历史记录手动选择
    console.log('检测到未完成的练习记录，可通过历史记录恢复')
  }
}

onMounted(() => {
  loadHistory()
  // 恢复上次未完成的练习
  restoreLastPractice()
  // 监听页面卸载事件
  window.addEventListener('beforeunload', handleBeforeUnload)
})

onUnmounted(() => {
  // 卸载前保存进度
  if (localRecord.value && practicing.value) {
    saveToLocalCache()
  }
  if (timer) clearInterval(timer)
  if (autoSaveTimer) clearTimeout(autoSaveTimer)
  // 移除页面卸载监听
  window.removeEventListener('beforeunload', handleBeforeUnload)
})

watch(showHistory, (val) => { if (val) loadHistory() })
</script>

<style scoped>
.recitation-page {
  max-width: 900px;
  margin: 0 auto;
  padding: 20px;
}

/* 表单区域 */
.input-section, .practice-section {
  background: var(--el-bg-color);
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.1);
}

.title-input { 
  margin-bottom: 12px; 
}

.content-input { 
  margin-bottom: 16px; 
}

.actions { 
  display: flex; 
  gap: 12px; 
}

.practice-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.practice-header .title { 
  font-size: 18px; 
  font-weight: 600; 
}

.controls { 
  display: flex; 
  align-items: center; 
  gap: 12px; 
}

.timer { 
  font-family: monospace; 
  font-size: 16px; 
  color: var(--el-color-primary); 
}

.text-display {
  background: #f5f5f5;
  border-radius: 8px;
  padding: 16px;
  line-height: 2;
  font-size: 18px;
  margin-bottom: 16px;
  min-height: 120px;
}

.char { 
  transition: color 0.2s; 
}

.char.pending { 
  color: #999; 
}

.char.correct { 
  color: #67c23a; 
}

.char.wrong { 
  color: #f56c6c; 
  background: #fef0f0; 
}

.char.hidden { 
  color: #ccc; 
}

.input-area {
  width: 100%;
  min-height: 100px;
  padding: 12px;
  border: 1px solid #dcdfe6;
  border-radius: 8px;
  font-size: 16px;
  resize: none;
  margin-bottom: 16px;
}

.input-area:focus { 
  outline: none; 
  border-color: var(--el-color-primary); 
}

.progress-bar { 
  display: flex; 
  align-items: center; 
  gap: 12px; 
  margin-bottom: 16px; 
}

.progress-bar .el-progress { 
  flex: 1; 
}

.practice-actions { 
  display: flex; 
  gap: 12px; 
}

.history-list { 
  display: flex; 
  flex-direction: column; 
  gap: 12px; 
}

.history-item {
  padding: 12px;
  border: 1px solid #eee;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
}

.history-item:hover { 
  border-color: var(--el-color-primary); 
  background: #f5f7fa; 
}

.history-item .info { 
  display: flex; 
  flex-direction: column;
  gap: 4px;
  margin-bottom: 8px; 
}

.history-item .name { 
  font-weight: 500; 
}

.history-item .content-preview {
  font-size: 13px;
  color: #606266;
  line-height: 1.5;
  margin-top: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.history-item .time { 
  color: #999; 
  font-size: 12px; 
  margin-top: 4px;
}

.history-item .meta { 
  display: flex; 
  justify-content: space-between; 
  align-items: center; 
}

.empty { 
  text-align: center; 
  color: #999; 
  padding: 40px; 
}

.complete-info { 
  text-align: center; 
  font-size: 16px; 
}

.complete-info p { 
  margin: 8px 0; 
}

@media (max-width: 768px) {
  .recitation-page {
    padding: 10px;
  }

  .input-section,
  .practice-section {
    padding: 14px;
    border-radius: 10px;
  }

  .actions,
  .practice-actions {
    flex-direction: column;
    gap: 10px;
  }

  .actions .el-button,
  .practice-actions .el-button {
    width: 100%;
    margin-left: 0;
  }

  .practice-header {
    flex-direction: column;
    align-items: stretch;
    gap: 10px;
    margin-bottom: 14px;
  }

  .practice-header .title {
    font-size: 16px;
  }

  .controls {
    flex-wrap: wrap;
    justify-content: space-between;
    gap: 8px;
  }

  .text-display {
    padding: 12px;
    line-height: 1.8;
    font-size: 16px;
    min-height: 100px;
  }

  .input-area {
    min-height: 120px;
    font-size: 15px;
  }

  .progress-bar {
    align-items: flex-start;
    flex-direction: column;
    gap: 6px;
  }

  .progress-bar .el-progress {
    width: 100%;
  }

  .history-item {
    padding: 10px;
  }

  .history-item .meta {
    gap: 8px;
    flex-wrap: wrap;
  }
}
</style>
