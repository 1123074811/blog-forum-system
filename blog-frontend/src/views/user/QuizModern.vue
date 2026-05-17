<template>
  <div class="jp-quiz-page max-w-6xl mx-auto">
    <!-- 页面标题 -->
    <div class="jp-quiz-header jp-study-card mb-6">
      <h1 class="jp-page-title">
        <span class="stamp">测</span>
        {{ quiz.title }}
      </h1>
      <p class="jp-page-subtitle">在线答题，检验你的学习成果</p>
    </div>
    
    <!-- 移动端垂直布局，PC 端水平布局 -->
    <div class="jp-quiz-layout flex flex-col lg:flex-row gap-4 lg:gap-6">
      <!-- 左侧答题区 -->
      <div class="flex-1 jp-study-card jp-quiz-answer-area">
        <!-- 顶部标题栏 - 移动端优化 -->
        <div class="flex flex-col sm:flex-row sm:justify-between sm:items-center gap-3 mb-4 sm:mb-6">
          <div class="flex items-center gap-2 sm:gap-3">
            <el-button @click="handleBack" :icon="ArrowLeft" text size="small">返回</el-button>
            <h2 class="jp-quiz-title text-base sm:text-xl font-bold dark:text-white truncate">{{ quiz.title }}</h2>
          </div>
          <div class="flex items-center justify-end gap-1">
            <div class="jp-progress-text text-xs sm:text-sm text-gray-500">
              {{ currentIndex + 1 }} / {{ questions.length }}
              <span v-if="showResult" class="jp-correct-rate ml-2 sm:ml-4">正确率：{{ correctRate }}%</span>
            </div>
            <el-button @click="handleRestart" size="small" type="warning" plain>重新开始</el-button>
            <!-- 移动端显示答题卡按钮 -->
            <el-button v-if="isMobile" @click="showAnswerSheet = true" size="small" type="primary">答题卡</el-button>
          </div>
        </div>

        <!-- 题目内容 -->
        <div v-if="currentQuestion" class="jp-question-content mb-4 sm:mb-6">
          <div class="mb-3 sm:mb-4">
            <span class="jp-question-type px-2 py-1 text-xs rounded border-2 border-ink" :class="typeClass">{{ typeLabel }}</span>
          </div>
          <p class="jp-question-text text-sm sm:text-lg dark:text-white mb-3 sm:mb-4 font-serif">{{ currentIndex + 1 }}. {{ currentQuestion.question }}</p>

          <!-- 单选/多选 -->
          <div v-if="currentQuestion.type === 'single' || currentQuestion.type === 'multiple'" class="space-y-2">
            <div v-for="(opt, idx) in parsedOptions" :key="idx"
                 class="jp-answer-option p-2 sm:p-3 border cursor-pointer transition-colors text-sm sm:text-base"
                 :class="optionClass(opt)"
                 @click="selectOption(opt)">
              {{ String.fromCharCode(65 + idx) }}. {{ opt }}
            </div>
          </div>

          <!-- 判断题 -->
          <div v-else-if="currentQuestion.type === 'judge'" class="flex gap-3 sm:gap-4">
            <div class="jp-answer-option flex-1 p-3 sm:p-4 border cursor-pointer text-center transition-colors text-sm sm:text-base"
                 :class="judgeClass(true)"
                 @click="selectJudge(true)">正确</div>
            <div class="jp-answer-option flex-1 p-3 sm:p-4 border cursor-pointer text-center transition-colors text-sm sm:text-base"
                 :class="judgeClass(false)"
                 @click="selectJudge(false)">错误</div>
          </div>

          <!-- 简答题 -->
          <div v-else-if="currentQuestion.type === 'short'" class="space-y-2">
            <el-input v-model="shortAnswer" 
                      type="textarea" 
                      :autosize="{ minRows: 3, maxRows: 10 }" 
                      placeholder="请输入你的答案..." 
                      :disabled="answered" />
          </div>

          <!-- 答案解析 -->
          <div v-if="answered"
               class="jp-answer-result mt-4 sm:mt-6 p-3 sm:p-4 text-sm sm:text-base"
               :class="resultClass">
            <p v-if="currentQuestion.type !== 'short'" class="font-medium" :class="isCorrect ? 'text-green-600' : 'text-red-600'">
              {{ isCorrect ? '回答正确!' : '回答错误' }}
            </p>
            <p v-else class="font-medium text-blue-600">已提交，请对照参考答案</p>
            <p class="text-xs sm:text-sm text-gray-600 dark:text-gray-400 mt-1">
              {{ currentQuestion.type === 'short' ? '参考答案' : '正确答案' }}: {{ formatAnswer(currentQuestion.answer) }}
            </p>
            <p v-if="currentQuestion.explanation" class="text-xs sm:text-sm text-gray-600 dark:text-gray-400 mt-2">
              解析: {{ currentQuestion.explanation }}
            </p>
          </div>
        </div>

        <!-- 底部按钮 -->
        <div class="flex justify-between gap-2">
          <el-button @click="prevQuestion" :disabled="currentIndex === 0" size="small">上一题</el-button>
          <div class="flex gap-2">
            <el-button v-if="!answered" 
                       type="primary" 
                       @click="submitAnswer" 
                       :disabled="!hasSelection"
                       size="small">提交答案</el-button>
            <el-button v-if="currentIndex < questions.length - 1" 
                       @click="nextQuestion"
                       size="small">下一题</el-button>
            <el-button v-if="answered && currentIndex === questions.length - 1" 
                       type="success" 
                       @click="showResult = true"
                       size="small">查看结果</el-button>
          </div>
        </div>
      </div>

      <!-- 右侧题号面板 - PC端显示 -->
      <div v-if="!isMobile" class="w-64 jp-study-card jp-quiz-side p-4 h-fit sticky top-0">
        <div class="flex justify-between items-center mb-3">
          <span class="text-sm font-medium dark:text-white flex items-center gap-1"><span class="stamp">卡</span>答题卡</span>
          <el-switch v-model="categoryMode" size="small" active-text="分类" inactive-text="顺序" />
        </div>
        <!-- 顺序模式 -->
        <div v-if="!categoryMode" class="grid grid-cols-5 gap-2">
          <div v-for="(q, idx) in questions" :key="idx"
            class="jp-question-index w-10 h-10 flex items-center justify-center cursor-pointer text-sm font-medium transition-all"
            :class="getQuestionStatusClass(idx)"
            @click="goToQuestion(idx)">
            {{ idx + 1 }}
          </div>
        </div>
        <!-- 分类模式 -->
        <div v-else class="space-y-3">
          <div v-for="cat in categoryList" :key="cat.type">
            <div class="text-xs mb-1" :class="cat.color">{{ cat.label }}（{{ cat.items.length }}）</div>
            <div class="grid grid-cols-5 gap-1">
              <div v-for="item in cat.items" :key="item.idx"
                class="jp-question-index w-8 h-8 flex items-center justify-center cursor-pointer text-xs font-medium transition-all"
                :class="getQuestionStatusClass(item.idx)"
                @click="goToQuestion(item.idx)">
                {{ item.idx + 1 }}
              </div>
            </div>
          </div>
        </div>
        <div class="mt-4 text-xs text-gray-500 space-y-1">
          <div class="flex items-center gap-2"><span class="jp-status-dot is-current"></span> 当前题</div>
          <div class="flex items-center gap-2"><span class="jp-status-dot is-correct"></span> 已答对</div>
          <div class="flex items-center gap-2"><span class="jp-status-dot is-wrong"></span> 已答错</div>
          <div class="flex items-center gap-2"><span class="jp-status-dot is-idle"></span> 未作答</div>
        </div>
      </div>
    </div>

    <!-- 移动端答题卡抽屉 -->
    <el-drawer v-model="showAnswerSheet" direction="rtl" size="280px" title="答题卡">
      <div class="mb-4">
        <el-switch v-model="categoryMode" size="small" active-text="分类" inactive-text="顺序" class="w-full" />
      </div>
      <!-- 顺序模式 -->
      <div v-if="!categoryMode" class="grid grid-cols-5 gap-2">
        <div v-for="(q, idx) in questions" :key="idx"
          class="jp-question-index w-12 h-12 flex items-center justify-center cursor-pointer text-sm font-medium transition-all"
          :class="getQuestionStatusClass(idx)"
          @click="goToQuestion(idx); showAnswerSheet = false">
          {{ idx + 1 }}
        </div>
      </div>
      <!-- 分类模式 -->
      <div v-else class="space-y-4">
        <div v-for="cat in categoryList" :key="cat.type">
          <div class="text-sm mb-2 font-medium" :class="cat.color">{{ cat.label }}（{{ cat.items.length }}）</div>
          <div class="grid grid-cols-5 gap-2">
            <div v-for="item in cat.items" :key="item.idx"
              class="jp-question-index w-10 h-10 flex items-center justify-center cursor-pointer text-xs font-medium transition-all"
              :class="getQuestionStatusClass(item.idx)"
              @click="goToQuestion(item.idx); showAnswerSheet = false">
              {{ item.idx + 1 }}
            </div>
          </div>
        </div>
      </div>
      <div class="mt-6 text-xs text-gray-500 space-y-2">
        <div class="flex items-center gap-2"><span class="jp-status-dot is-current"></span> 当前题</div>
        <div class="flex items-center gap-2"><span class="jp-status-dot is-correct"></span> 已答对</div>
        <div class="flex items-center gap-2"><span class="jp-status-dot is-wrong"></span> 已答错</div>
        <div class="flex items-center gap-2"><span class="jp-status-dot is-idle"></span> 未作答</div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getQuiz, getQuizQuestions } from '@/api/blog'
import { ArrowLeft } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'

const route = useRoute()
const router = useRouter()
const quiz = ref({})
const questions = ref([])
const currentIndex = ref(0)
const userAnswers = ref({})
const showResult = ref(false)
const shortAnswer = ref('')
const categoryMode = ref(false)
const loading = ref(false)
const totalQuestions = ref(0)
const showAnswerSheet = ref(false)
const isMobile = ref(window.innerWidth < 1024) // lg breakpoint

// 监听窗口大小变化
const handleResize = () => {
  isMobile.value = window.innerWidth < 1024
}

window.addEventListener('resize', handleResize)

// 处理返回逻辑
const handleBack = () => {
  const fromTab = route.query.from
  if (fromTab) {
    // 如果有来源标签页信息，返回题库列表并恢复标签页
    router.push({ path: '/quiz', query: { tab: fromTab } })
  } else {
    // 否则使用浏览器返回
    router.back()
  }
}

// 生成当前题库的存储键
const getStorageKey = () => `quiz_progress_${route.params.id}`

// 保存进度到 localStorage
const saveProgress = () => {
  const progressData = {
    currentIndex: currentIndex.value,
    userAnswers: userAnswers.value,
    timestamp: Date.now()
  }
  try {
    localStorage.setItem(getStorageKey(), JSON.stringify(progressData))
  } catch (e) {
    console.error('保存进度失败:', e)
  }
}

// 从 localStorage 恢复进度
const loadProgress = () => {
  try {
    const saved = localStorage.getItem(getStorageKey())
    if (saved) {
      const progressData = JSON.parse(saved)
      // 恢复答题进度
      currentIndex.value = progressData.currentIndex || 0
      userAnswers.value = progressData.userAnswers || {}
      // 如果当前题有答案，恢复到 shortAnswer
      const ans = userAnswers.value[currentIndex.value]
      if (ans?.selected && currentQuestion.value?.type === 'short') {
        shortAnswer.value = ans.selected
      }
      return true
    }
  } catch (e) {
    console.error('加载进度失败:', e)
  }
  return false
}

// 清除进度
const clearProgress = () => {
  try {
    localStorage.removeItem(getStorageKey())
  } catch (e) {
    console.error('清除进度失败:', e)
  }
}

// 重新开始答题
const handleRestart = () => {
  if (Object.keys(userAnswers.value).length > 0) {
    ElMessageBox.confirm('确定要清除当前答题进度，重新开始吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }).then(() => {
      currentIndex.value = 0
      userAnswers.value = {}
      shortAnswer.value = ''
      showResult.value = false
      clearProgress()
      ElMessage.success('已清除进度')
    }).catch(() => {})
  } else {
    ElMessage.info('当前没有答题进度')
  }
}

const loadAllQuestions = async (quizBankId, total) => {
  loading.value = true
  const pageSize = 200
  const pages = Math.ceil(total / pageSize)
  const allQuestions = []
  for (let i = 1; i <= pages; i++) {
    const res = await getQuizQuestions(quizBankId, i, pageSize)
    if (res.success) allQuestions.push(...res.data.records)
  }
  questions.value = allQuestions
  loading.value = false
}

const categoryList = computed(() => {
  const types = [
    { type: 'single', label: '单选题', color: 'text-blue-600' },
    { type: 'multiple', label: '多选题', color: 'text-purple-600' },
    { type: 'judge', label: '判断题', color: 'text-orange-600' },
    { type: 'short', label: '简答题', color: 'text-green-600' }
  ]
  return types.map(t => ({
    ...t,
    items: questions.value.map((q, idx) => ({ q, idx })).filter(item => item.q.type === t.type)
  })).filter(t => t.items.length > 0)
})

const currentQuestion = computed(() => questions.value[currentIndex.value])
const answered = computed(() => userAnswers.value[currentIndex.value]?.submitted)
const hasSelection = computed(() => {
  const ans = userAnswers.value[currentIndex.value]
  if (currentQuestion.value?.type === 'short') return shortAnswer.value.trim().length > 0
  return ans && (ans.selected !== null && ans.selected !== undefined && (Array.isArray(ans.selected) ? ans.selected.length > 0 : true))
})

watch(currentIndex, () => {
  const ans = userAnswers.value[currentIndex.value]
  shortAnswer.value = ans?.selected || ''
  saveProgress()
})

// 监听答案变化，自动保存
watch(userAnswers, () => {
  saveProgress()
}, { deep: true })

const parsedOptions = computed(() => {
  if (!currentQuestion.value?.options) return []
  try { return JSON.parse(currentQuestion.value.options) } catch { return [] }
})

const parsedAnswer = computed(() => {
  if (!currentQuestion.value?.answer) return null
  try { return JSON.parse(currentQuestion.value.answer) } catch { return currentQuestion.value.answer }
})

// 判断是否为字母格式答案 (A-Z)，且不在选项内容中
const isLetterAnswer = (ans, options = []) => {
  if (typeof ans !== 'string' || !/^[A-Z]$/.test(ans)) return false
  // 如果答案与某个选项内容完全匹配，优先视为内容格式
  return !options.includes(ans)
}

// 将选项内容转换为字母
const optionToLetter = (opt, options) => {
  const idx = options.indexOf(opt)
  return idx >= 0 ? String.fromCharCode(65 + idx) : null
}

// 将字母转换为选项内容
const letterToOption = (letter, options) => {
  const idx = letter.charCodeAt(0) - 65
  return options[idx] || null
}

// 比较用户选择和正确答案
const compareAnswer = (selected, correct, options) => {
  if (Array.isArray(correct)) {
    // 多选题
    const selArr = Array.isArray(selected) ? selected : [selected]
    if (correct.every(c => isLetterAnswer(c, options))) {
      // 答案是字母格式，转换用户选择为字母
      const selLetters = selArr.map(s => optionToLetter(s, options))
      return correct.length === selLetters.length && correct.every(c => selLetters.includes(c))
    } else {
      // 答案是内容格式
      return correct.length === selArr.length && correct.every(c => selArr.includes(c))
    }
  } else {
    // 单选题
    const sel = Array.isArray(selected) ? selected[0] : selected
    if (isLetterAnswer(correct, options)) {
      return optionToLetter(sel, options) === correct
    } else {
      return sel === correct
    }
  }
}

const isCorrect = computed(() => {
  const ans = userAnswers.value[currentIndex.value]
  if (!ans?.submitted) return false
  if (currentQuestion.value?.type === 'short') return true
  return compareAnswer(ans.selected, parsedAnswer.value, parsedOptions.value)
})

const checkCorrect = (idx) => {
  const ans = userAnswers.value[idx]
  if (!ans?.submitted) return null
  const q = questions.value[idx]
  if (q.type === 'short') return true
  let correct, options
  try { correct = JSON.parse(q.answer) } catch { correct = q.answer }
  try { options = JSON.parse(q.options) } catch { options = [] }
  return compareAnswer(ans.selected, correct, options)
}

const correctRate = computed(() => {
  const total = questions.value.filter(q => q.type !== 'short').length
  if (!total) return 100
  const correct = Object.entries(userAnswers.value).filter(([i, a]) => {
    const q = questions.value[i]
    if (!a?.submitted || q.type === 'short') return false
    return checkCorrect(Number(i))
  }).length
  return Math.round((correct / total) * 100)
})

const typeLabel = computed(() => {
  const t = currentQuestion.value?.type
  return { single: '单选题', multiple: '多选题', judge: '判断题', short: '简答题' }[t] || '未知'
})

const typeClass = computed(() => {
  const t = currentQuestion.value?.type
  return { single: 'is-single', multiple: 'is-multiple', judge: 'is-judge', short: 'is-short' }[t] || ''
})

const resultClass = computed(() => {
  if (currentQuestion.value?.type === 'short') return 'is-reference'
  return isCorrect.value ? 'is-correct' : 'is-wrong'
})

const getQuestionStatusClass = (idx) => {
  if (idx === currentIndex.value) return 'is-current'
  const ans = userAnswers.value[idx]
  if (!ans?.submitted) return 'is-idle'
  const correct = checkCorrect(idx)
  if (correct) return 'is-correct'
  return 'is-wrong'
}

const optionClass = (opt) => {
  const ans = userAnswers.value[currentIndex.value]
  const isSingle = currentQuestion.value?.type === 'single'
  const isSelected = isSingle
    ? (Array.isArray(ans?.selected) ? ans.selected[0] === opt : ans?.selected === opt)
    : (Array.isArray(ans?.selected) ? ans.selected.includes(opt) : ans?.selected === opt)
  if (!ans?.submitted) {
    return isSelected ? 'is-selected font-medium' : 'is-idle'
  }
  const correct = parsedAnswer.value
  const options = parsedOptions.value
  let isCorrectOption
  if (Array.isArray(correct)) {
    isCorrectOption = correct.every(c => isLetterAnswer(c, options))
      ? correct.includes(optionToLetter(opt, options))
      : correct.includes(opt)
  } else {
    isCorrectOption = isLetterAnswer(correct, options)
      ? optionToLetter(opt, options) === correct
      : correct === opt
  }
  if (isCorrectOption) return 'is-correct'
  if (isSelected && !isCorrectOption) return 'is-wrong'
  return 'is-idle'
}

const judgeClass = (val) => {
  const ans = userAnswers.value[currentIndex.value]
  if (!ans?.submitted) {
    return ans?.selected === val ? 'is-selected font-medium' : 'is-idle'
  }
  const correct = parsedAnswer.value
  if (correct === val) return 'is-correct'
  if (ans?.selected === val && correct !== val) return 'is-wrong'
  return 'is-idle'
}

const selectOption = (opt) => {
  if (answered.value) return
  if (!userAnswers.value[currentIndex.value]) userAnswers.value[currentIndex.value] = { selected: null, submitted: false }
  if (currentQuestion.value.type === 'multiple') {
    let arr = userAnswers.value[currentIndex.value].selected
    if (!Array.isArray(arr)) arr = []
    const idx = arr.indexOf(opt)
    if (idx > -1) arr.splice(idx, 1)
    else arr.push(opt)
    userAnswers.value[currentIndex.value].selected = [...arr]
  } else {
    userAnswers.value[currentIndex.value].selected = opt
  }
  saveProgress()
}

const selectJudge = (val) => {
  if (answered.value) return
  if (!userAnswers.value[currentIndex.value]) userAnswers.value[currentIndex.value] = { selected: null, submitted: false }
  userAnswers.value[currentIndex.value].selected = val
  saveProgress()
}

const submitAnswer = () => {
  if (!userAnswers.value[currentIndex.value]) userAnswers.value[currentIndex.value] = { selected: null, submitted: false }
  if (currentQuestion.value.type === 'short') {
    userAnswers.value[currentIndex.value].selected = shortAnswer.value
  }
  userAnswers.value[currentIndex.value].submitted = true
  saveProgress()
}

const prevQuestion = () => { if (currentIndex.value > 0) currentIndex.value-- }
const nextQuestion = () => { if (currentIndex.value < questions.value.length - 1) currentIndex.value++ }
const goToQuestion = (idx) => { currentIndex.value = idx }

const formatAnswer = (ans) => {
  try {
    const parsed = JSON.parse(ans)
    if (Array.isArray(parsed)) return parsed.join(', ')
    if (typeof parsed === 'boolean') return parsed ? '正确' : '错误'
    return parsed
  } catch { return ans }
}

onMounted(async () => {
  const res = await getQuiz(route.params.id)
  if (res.success) {
    quiz.value = res.data
    totalQuestions.value = res.data.questionCount || 0
    // 小题库直接用返回的数据，大题库分页加载
    if (res.data.questions && res.data.questions.length > 0) {
      questions.value = res.data.questions
    } else if (totalQuestions.value > 0) {
      await loadAllQuestions(route.params.id, totalQuestions.value)
    }
    // 加载完题目后，尝试恢复进度
    loadProgress()
  }
  
  // 监听页面刷新/关闭事件，确保保存进度
  window.addEventListener('beforeunload', saveProgress)
})

// 页面卸载前保存进度
onUnmounted(() => {
  saveProgress()
  window.removeEventListener('beforeunload', saveProgress)
  window.removeEventListener('resize', handleResize)
})
</script>

<style scoped>
.jp-quiz-page {
  padding-top: 28px;
}

.jp-quiz-header {
  border-style: dashed;
  box-shadow: 2px 2px 0 var(--accent);
  padding: 20px;
}

.jp-quiz-answer-area {
  border-radius: 12px 22px 14px 18px / 18px 12px 20px 10px;
  padding: 20px;
}

.jp-quiz-layout {
  align-items: flex-start;
}

.jp-quiz-side {
  padding: 14px 14px !important;
  border-radius: 18px 8px 16px 10px / 8px 18px 10px 16px;
  transform: rotate(-1deg);
  margin-top: 0;
}

.jp-quiz-side .stamp {
  margin-right: 0 !important;
  transform: rotate(-10deg) scale(0.75);
}

.jp-answer-option {
  background: rgba(255, 255, 255, 0.42);
  border-color: rgba(44, 62, 80, 0.16);
  border-radius: 0;
}

.jp-answer-option:hover {
  border-color: var(--ink);
  background: rgba(211, 84, 0, 0.06);
}

.jp-answer-option.is-selected {
  border-color: var(--ink);
  background: rgba(211, 84, 0, 0.1);
  color: var(--accent);
  box-shadow: 2px 2px 0 var(--accent);
}

.jp-answer-option.is-correct {
  border-color: #1f8f5f;
  background: rgba(31, 143, 95, 0.1);
  color: #1f8f5f;
}

.jp-answer-option.is-wrong {
  border-color: #c0392b;
  background: rgba(192, 57, 43, 0.1);
  color: #c0392b;
}

.jp-answer-result {
  border: 1px dashed var(--ink);
  border-radius: 0;
  background: rgba(211, 84, 0, 0.06) !important;
}

.jp-question-type {
  border-radius: 0 !important;
  background: rgba(211, 84, 0, 0.08);
  color: var(--accent);
  border-color: var(--ink);
}

.jp-question-type.is-multiple {
  background: rgba(44, 62, 80, 0.08);
  color: var(--ink);
}

.jp-question-type.is-judge {
  background: rgba(230, 126, 34, 0.12);
}

.jp-question-type.is-short {
  background: rgba(31, 143, 95, 0.1);
  color: #1f8f5f;
}

.jp-question-index {
  border: 1px solid rgba(44, 62, 80, 0.18);
  border-radius: 0;
}

.jp-question-index.is-current {
  background: var(--ink);
  color: var(--paper);
  box-shadow: 2px 2px 0 var(--accent);
}

.jp-question-index.is-idle {
  background: rgba(44, 62, 80, 0.08);
  color: var(--ink);
}

.jp-question-index.is-idle:hover {
  border-color: var(--ink);
  background: rgba(211, 84, 0, 0.08);
}

.jp-question-index.is-correct {
  background: rgba(31, 143, 95, 0.16);
  border-color: #1f8f5f;
  color: #1f8f5f;
}

.jp-question-index.is-wrong {
  background: rgba(192, 57, 43, 0.14);
  border-color: #c0392b;
  color: #c0392b;
}

.jp-status-dot {
  width: 1rem;
  height: 1rem;
  border: 1px solid var(--ink);
  display: inline-block;
}

.jp-status-dot.is-current {
  background: var(--ink);
}

.jp-status-dot.is-correct {
  background: rgba(31, 143, 95, 0.7);
}

.jp-status-dot.is-wrong {
  background: rgba(192, 57, 43, 0.7);
}

.jp-status-dot.is-idle {
  background: rgba(44, 62, 80, 0.08);
}

.dark .jp-answer-option {
  background: rgba(15, 23, 42, 0.4);
  border-color: rgba(148, 163, 184, 0.24);
}

.dark .jp-answer-option:hover {
  border-color: var(--accent-primary);
  background: rgba(56, 189, 248, 0.1);
}

.dark .jp-answer-option.is-selected,
.dark .jp-question-index.is-current {
  border-color: var(--accent-primary);
  color: var(--accent-primary);
  box-shadow: 2px 2px 0 rgba(56, 189, 248, 0.45);
}

.dark .jp-question-index.is-current {
  background: rgba(15, 23, 42, 0.9);
}

.dark .jp-question-index.is-idle {
  background: rgba(15, 23, 42, 0.45);
  color: var(--text-primary);
}

.dark .jp-status-dot {
  border-color: var(--accent-primary);
}

@media (max-width: 768px) {
  .jp-quiz-page {
    padding-top: 16px;
  }
}
</style>
