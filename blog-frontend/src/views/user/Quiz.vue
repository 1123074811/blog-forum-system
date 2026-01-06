<template>
  <div class="max-w-6xl mx-auto flex gap-6">
    <!-- 左侧答题区 -->
    <div class="flex-1 glass rounded-xl p-6">
      <div class="flex justify-between items-center mb-6">
        <h2 class="text-xl font-bold dark:text-white">{{ quiz.title }}</h2>
        <div class="text-sm text-gray-500">
          {{ currentIndex + 1 }} / {{ questions.length }}
          <span v-if="showResult" class="ml-4">正确率: {{ correctRate }}%</span>
        </div>
      </div>

      <div v-if="currentQuestion" class="mb-6">
        <div class="mb-4">
          <span class="px-2 py-1 text-xs rounded" :class="typeClass">{{ typeLabel }}</span>
        </div>
        <p class="text-lg dark:text-white mb-4">{{ currentIndex + 1 }}. {{ currentQuestion.question }}</p>

        <!-- 单选/多选 -->
        <div v-if="currentQuestion.type === 'single' || currentQuestion.type === 'multiple'" class="space-y-2">
          <div v-for="(opt, idx) in parsedOptions" :key="idx" class="p-3 border rounded-lg cursor-pointer transition-colors" :class="optionClass(opt)" @click="selectOption(opt)">
            {{ opt }}
          </div>
        </div>

        <!-- 判断题 -->
        <div v-else-if="currentQuestion.type === 'judge'" class="flex gap-4">
          <div class="flex-1 p-4 border rounded-lg cursor-pointer text-center transition-colors" :class="judgeClass(true)" @click="selectJudge(true)">正确</div>
          <div class="flex-1 p-4 border rounded-lg cursor-pointer text-center transition-colors" :class="judgeClass(false)" @click="selectJudge(false)">错误</div>
        </div>

        <!-- 简答题 -->
        <div v-else-if="currentQuestion.type === 'short'" class="space-y-2">
          <el-input v-model="shortAnswer" type="textarea" :autosize="{ minRows: 3, maxRows: 10 }" placeholder="请输入你的答案..." :disabled="answered" />
        </div>

        <!-- 答案解析 -->
        <div v-if="answered" class="mt-6 p-4 rounded-lg" :class="currentQuestion.type === 'short' ? 'bg-blue-50 dark:bg-blue-900/20' : (isCorrect ? 'bg-green-50 dark:bg-green-900/20' : 'bg-red-50 dark:bg-red-900/20')">
          <p v-if="currentQuestion.type !== 'short'" class="font-medium" :class="isCorrect ? 'text-green-600' : 'text-red-600'">
            {{ isCorrect ? '回答正确!' : '回答错误' }}
          </p>
          <p v-else class="font-medium text-blue-600">已提交，请对照参考答案</p>
          <p class="text-sm text-gray-600 dark:text-gray-400 mt-1">{{ currentQuestion.type === 'short' ? '参考答案' : '正确答案' }}: {{ formatAnswer(currentQuestion.answer) }}</p>
          <p v-if="currentQuestion.explanation" class="text-sm text-gray-600 dark:text-gray-400 mt-2">解析: {{ currentQuestion.explanation }}</p>
        </div>
      </div>

      <div class="flex justify-between">
        <el-button @click="prevQuestion" :disabled="currentIndex === 0">上一题</el-button>
        <div class="flex gap-2">
          <el-button v-if="!answered" type="primary" @click="submitAnswer" :disabled="!hasSelection">提交答案</el-button>
          <el-button v-if="currentIndex < questions.length - 1" @click="nextQuestion">下一题</el-button>
          <el-button v-if="answered && currentIndex === questions.length - 1" type="success" @click="showResult = true">查看结果</el-button>
        </div>
      </div>
    </div>

    <!-- 右侧题号面板 -->
    <div class="w-64 glass rounded-xl p-4 h-fit sticky top-24">
      <div class="flex justify-between items-center mb-3">
        <span class="text-sm font-medium dark:text-white">答题卡</span>
        <el-switch v-model="categoryMode" size="small" active-text="分类" inactive-text="顺序" />
      </div>
      <!-- 顺序模式 -->
      <div v-if="!categoryMode" class="grid grid-cols-5 gap-2">
        <div v-for="(q, idx) in questions" :key="idx"
          class="w-10 h-10 flex items-center justify-center rounded-lg cursor-pointer text-sm font-medium transition-all"
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
              class="w-8 h-8 flex items-center justify-center rounded cursor-pointer text-xs font-medium transition-all"
              :class="getQuestionStatusClass(item.idx)"
              @click="goToQuestion(item.idx)">
              {{ item.idx + 1 }}
            </div>
          </div>
        </div>
      </div>
      <div class="mt-4 text-xs text-gray-500 space-y-1">
        <div class="flex items-center gap-2"><span class="w-4 h-4 rounded bg-primary-500"></span> 当前题</div>
        <div class="flex items-center gap-2"><span class="w-4 h-4 rounded bg-green-500"></span> 已答对</div>
        <div class="flex items-center gap-2"><span class="w-4 h-4 rounded bg-red-500"></span> 已答错</div>
        <div class="flex items-center gap-2"><span class="w-4 h-4 rounded bg-gray-200 dark:bg-gray-700"></span> 未作答</div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { getQuiz, getQuizQuestions } from '@/api/blog'

const route = useRoute()
const quiz = ref({})
const questions = ref([])
const currentIndex = ref(0)
const userAnswers = ref({})
const showResult = ref(false)
const shortAnswer = ref('')
const categoryMode = ref(false)
const loading = ref(false)
const totalQuestions = ref(0)

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
})

const parsedOptions = computed(() => {
  if (!currentQuestion.value?.options) return []
  try { return JSON.parse(currentQuestion.value.options) } catch { return [] }
})

const parsedAnswer = computed(() => {
  if (!currentQuestion.value?.answer) return null
  try { return JSON.parse(currentQuestion.value.answer) } catch { return currentQuestion.value.answer }
})

const isCorrect = computed(() => {
  const ans = userAnswers.value[currentIndex.value]
  if (!ans?.submitted) return false
  if (currentQuestion.value?.type === 'short') return true
  const correct = parsedAnswer.value
  if (Array.isArray(correct)) {
    return Array.isArray(ans.selected) && correct.length === ans.selected.length && correct.every(c => ans.selected.includes(c))
  }
  return ans.selected === correct
})

const checkCorrect = (idx) => {
  const ans = userAnswers.value[idx]
  if (!ans?.submitted) return null
  const q = questions.value[idx]
  if (q.type === 'short') return true
  try {
    const correct = JSON.parse(q.answer)
    if (Array.isArray(correct)) {
      return Array.isArray(ans.selected) && correct.length === ans.selected.length && correct.every(c => ans.selected.includes(c))
    }
    return ans.selected === correct
  } catch { return false }
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
  return { single: 'bg-blue-100 text-blue-600', multiple: 'bg-purple-100 text-purple-600', judge: 'bg-orange-100 text-orange-600', short: 'bg-green-100 text-green-600' }[t] || ''
})

const getQuestionStatusClass = (idx) => {
  if (idx === currentIndex.value) return 'bg-primary-500 text-white'
  const ans = userAnswers.value[idx]
  if (!ans?.submitted) return 'bg-gray-200 dark:bg-gray-700 text-gray-600 dark:text-gray-300 hover:bg-gray-300'
  const correct = checkCorrect(idx)
  if (correct) return 'bg-green-500 text-white'
  return 'bg-red-500 text-white'
}

const optionClass = (opt) => {
  const ans = userAnswers.value[currentIndex.value]
  const letter = opt.charAt(0)
  const isSelected = Array.isArray(ans?.selected) ? ans.selected.includes(letter) : ans?.selected === letter
  if (!ans?.submitted) {
    return isSelected ? 'border-primary-500 bg-primary-50 dark:bg-primary-900/20' : 'border-gray-200 dark:border-gray-700 hover:border-primary-300'
  }
  const correct = parsedAnswer.value
  const isCorrectOption = Array.isArray(correct) ? correct.includes(letter) : correct === letter
  if (isCorrectOption) return 'border-green-500 bg-green-50 dark:bg-green-900/20'
  if (isSelected && !isCorrectOption) return 'border-red-500 bg-red-50 dark:bg-red-900/20'
  return 'border-gray-200 dark:border-gray-700'
}

const judgeClass = (val) => {
  const ans = userAnswers.value[currentIndex.value]
  if (!ans?.submitted) {
    return ans?.selected === val ? 'border-primary-500 bg-primary-50 dark:bg-primary-900/20' : 'border-gray-200 dark:border-gray-700 hover:border-primary-300'
  }
  const correct = parsedAnswer.value
  if (correct === val) return 'border-green-500 bg-green-50 dark:bg-green-900/20'
  if (ans?.selected === val && correct !== val) return 'border-red-500 bg-red-50 dark:bg-red-900/20'
  return 'border-gray-200 dark:border-gray-700'
}

const selectOption = (opt) => {
  if (answered.value) return
  const letter = opt.charAt(0)
  if (!userAnswers.value[currentIndex.value]) userAnswers.value[currentIndex.value] = { selected: null, submitted: false }
  if (currentQuestion.value.type === 'multiple') {
    const arr = userAnswers.value[currentIndex.value].selected || []
    const idx = arr.indexOf(letter)
    if (idx > -1) arr.splice(idx, 1)
    else arr.push(letter)
    userAnswers.value[currentIndex.value].selected = [...arr]
  } else {
    userAnswers.value[currentIndex.value].selected = letter
  }
}

const selectJudge = (val) => {
  if (answered.value) return
  if (!userAnswers.value[currentIndex.value]) userAnswers.value[currentIndex.value] = { selected: null, submitted: false }
  userAnswers.value[currentIndex.value].selected = val
}

const submitAnswer = () => {
  if (!userAnswers.value[currentIndex.value]) userAnswers.value[currentIndex.value] = { selected: null, submitted: false }
  if (currentQuestion.value.type === 'short') {
    userAnswers.value[currentIndex.value].selected = shortAnswer.value
  }
  userAnswers.value[currentIndex.value].submitted = true
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
  }
})
</script>
