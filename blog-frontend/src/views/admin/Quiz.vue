<template>
  <div>
    <div class="flex justify-between items-center mb-6">
      <h1 class="text-2xl font-bold">刷题管理</h1>
      <el-button type="danger" :disabled="!selectedIds.length" @click="handleBatchDelete">批量删除 ({{ selectedIds.length }})</el-button>
    </div>

    <!-- 搜索筛选区域 -->
    <div class="glass rounded-xl p-4 mb-4">
      <el-form :inline="true">
        <el-form-item label="搜索">
          <el-input v-model="searchQuery" placeholder="题库名称/描述" clearable @clear="handleSearch" style="width: 200px" />
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="typeFilter" placeholder="全部" clearable style="width: 120px">
            <el-option label="全部" value="" />
            <el-option label="题库" value="quiz" />
            <el-option label="文件" value="file" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="statusFilter" placeholder="全部" clearable style="width: 120px">
            <el-option label="全部" value="" />
            <el-option label="公开" value="public" />
            <el-option label="私有" value="private" />
          </el-select>
        </el-form-item>
        <el-form-item label="用户ID">
          <el-input v-model="userIdFilter" placeholder="用户ID" clearable style="width: 120px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <el-table :data="filteredQuizBanks" v-loading="loading" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="50" />
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="title" label="题库名称" />
      <el-table-column prop="description" label="描述" show-overflow-tooltip />
      <el-table-column prop="type" label="类型" width="80">
        <template #default="{ row }">
          <el-tag :type="row.type === 'quiz' ? 'primary' : 'success'" size="small">{{ row.type === 'quiz' ? '题库' : '文件' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="isPublic" label="公开" width="80">
        <template #default="{ row }">
          <el-tag :type="row.isPublic ? 'success' : 'info'" size="small">{{ row.isPublic ? '公开' : '私有' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="questionCount" label="题目数" width="80" />
      <el-table-column prop="userId" label="用户ID" width="80" />
      <el-table-column prop="createdAt" label="创建时间" width="170" />
      <el-table-column label="操作" width="220">
        <template #default="{ row }">
          <el-button type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
          <el-button type="success" size="small" @click="handleManageQuestions(row)" v-if="row.type === 'quiz'">题目</el-button>
          <el-button type="danger" size="small" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="showEditDialog" title="编辑题库" width="450px">
      <el-form :model="editForm" label-width="80px">
        <el-form-item label="名称"><el-input v-model="editForm.title" /></el-form-item>
        <el-form-item label="描述"><el-input v-model="editForm.description" type="textarea" /></el-form-item>
        <el-form-item label="公开">
          <el-switch v-model="editForm.isPublic" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showEditDialog = false">取消</el-button>
        <el-button type="primary" @click="saveEdit">保存</el-button>
      </template>
    </el-dialog>

    <!-- 题目管理对话框 -->
    <el-dialog v-model="showQuestionsDialog" title="题目管理" width="90%" top="5vh">
      <div class="mb-4 flex justify-between items-center">
        <div class="text-lg font-bold">{{ currentQuizBank?.title }} (共 {{ questions.length }} 题)</div>
        <el-button type="primary" @click="handleAddQuestion">添加题目</el-button>
      </div>

      <el-table :data="questions" v-loading="questionsLoading" max-height="500">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="type" label="类型" width="100">
          <template #default="{ row }">
            <el-tag size="small">{{ getQuestionTypeLabel(row.type) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="question" label="题目" show-overflow-tooltip min-width="300" />
        <el-table-column prop="answer" label="答案" width="150" show-overflow-tooltip />
        <el-table-column prop="sortOrder" label="排序" width="80" />
        <el-table-column label="操作" width="150">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="handleEditQuestion(row)">编辑</el-button>
            <el-button type="danger" size="small" @click="handleDeleteQuestion(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <!-- 题目编辑对话框 -->
    <el-dialog v-model="showQuestionEditDialog" :title="questionEditMode === 'add' ? '添加题目' : '编辑题目'" width="600px">
      <el-form :model="questionForm" label-width="80px">
        <el-form-item label="题目类型">
          <el-select v-model="questionForm.type" style="width: 100%">
            <el-option label="单选题" value="single" />
            <el-option label="多选题" value="multiple" />
            <el-option label="判断题" value="judge" />
            <el-option label="填空题" value="blank" />
          </el-select>
        </el-form-item>
        <el-form-item label="题目">
          <el-input v-model="questionForm.question" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="选项" v-if="['single', 'multiple'].includes(questionForm.type)">
          <div class="space-y-2">
            <div v-for="(option, index) in questionForm.options" :key="index" class="flex gap-2">
              <el-input v-model="questionForm.options[index]" placeholder="选项内容">
                <template #prepend>{{ String.fromCharCode(65 + index) }}.</template>
              </el-input>
              <el-button type="danger" @click="removeOption(index)" v-if="questionForm.options.length > 2">删除</el-button>
            </div>
            <el-button type="primary" @click="addOption" size="small">添加选项</el-button>
          </div>
        </el-form-item>
        <el-form-item label="答案">
          <el-input v-model="questionForm.answerText" placeholder="单选填A/B/C，多选填ABC，判断填true/false，填空填答案" />
        </el-form-item>
        <el-form-item label="解析">
          <el-input v-model="questionForm.explanation" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showQuestionEditDialog = false">取消</el-button>
        <el-button type="primary" @click="saveQuestion">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '@/api'

const quizBanks = ref([])
const selectedIds = ref([])
const loading = ref(false)
const showEditDialog = ref(false)
const editForm = ref({})
const editingId = ref(null)
const searchQuery = ref('')
const typeFilter = ref('')
const statusFilter = ref('')
const userIdFilter = ref('')

// 题目管理相关
const showQuestionsDialog = ref(false)
const showQuestionEditDialog = ref(false)
const questions = ref([])
const questionsLoading = ref(false)
const currentQuizBank = ref(null)
const questionForm = ref({})
const questionEditMode = ref('add') // 'add' or 'edit'
const editingQuestionId = ref(null)

const filteredQuizBanks = computed(() => {
  return quizBanks.value.filter(quiz => {
    const matchSearch = !searchQuery.value ||
      quiz.title?.toLowerCase().includes(searchQuery.value.toLowerCase()) ||
      quiz.description?.toLowerCase().includes(searchQuery.value.toLowerCase())

    const matchType = !typeFilter.value || quiz.type === typeFilter.value

    const matchStatus = !statusFilter.value ||
      (statusFilter.value === 'public' && quiz.isPublic) ||
      (statusFilter.value === 'private' && !quiz.isPublic)

    const matchUserId = !userIdFilter.value ||
      String(quiz.userId) === userIdFilter.value

    return matchSearch && matchType && matchStatus && matchUserId
  })
})

const loadQuizBanks = async () => {
  loading.value = true
  const res = await api.get('/quiz/admin/all')
  quizBanks.value = res.data || []
  loading.value = false
}

const handleSelectionChange = (rows) => {
  selectedIds.value = rows.map(r => r.id)
}

const handleBatchDelete = async () => {
  await ElMessageBox.confirm(`确定删除选中的 ${selectedIds.value.length} 个题库？`, '批量删除')
  await api.post('/quiz/admin/batch-delete', { ids: selectedIds.value })
  ElMessage.success('批量删除成功')
  loadQuizBanks()
}

const handleEdit = (row) => {
  editingId.value = row.id
  editForm.value = { title: row.title, description: row.description, isPublic: row.isPublic }
  showEditDialog.value = true
}

const saveEdit = async () => {
  await api.put(`/quiz/admin/${editingId.value}`, editForm.value)
  ElMessage.success('保存成功')
  showEditDialog.value = false
  loadQuizBanks()
}

const handleDelete = async (row) => {
  await ElMessageBox.confirm('确定删除该题库？所有题目也会被删除！', '提示')
  await api.delete(`/quiz/admin/${row.id}`)
  ElMessage.success('删除成功')
  loadQuizBanks()
}

const handleSearch = () => {
  // 触发计算属性重新计算
}

const handleReset = () => {
  searchQuery.value = ''
  typeFilter.value = ''
  statusFilter.value = ''
  userIdFilter.value = ''
}

// 题目管理方法
const handleManageQuestions = async (row) => {
  currentQuizBank.value = row
  showQuestionsDialog.value = true
  await loadQuestions(row.id)
}

const loadQuestions = async (quizBankId) => {
  questionsLoading.value = true
  try {
    const res = await api.get(`/quiz/${quizBankId}`)
    questions.value = res.data?.questions || []
  } catch (error) {
    ElMessage.error('加载题目失败')
  }
  questionsLoading.value = false
}

const handleAddQuestion = () => {
  questionEditMode.value = 'add'
  questionForm.value = {
    type: 'single',
    question: '',
    options: ['', '', '', ''],
    answerText: '',
    explanation: ''
  }
  showQuestionEditDialog.value = true
}

const handleEditQuestion = (row) => {
  questionEditMode.value = 'edit'
  editingQuestionId.value = row.id

  // 解析选项和答案
  let options = ['', '', '', '']
  let answerText = ''

  try {
    if (row.options) {
      const parsedOptions = JSON.parse(row.options)
      if (Array.isArray(parsedOptions)) {
        options = parsedOptions.map(opt => {
          // 移除 "A. " 这样的前缀
          return opt.replace(/^[A-Z]\.\s*/, '')
        })
      }
    }
    if (row.answer) {
      const answer = JSON.parse(row.answer)
      answerText = Array.isArray(answer) ? answer.join('') : answer
    }
  } catch (e) {
    if (row.options) {
      options = row.options.split('\n').filter(o => o.trim())
    }
    answerText = row.answer || ''
  }

  questionForm.value = {
    type: row.type,
    question: row.question,
    options,
    answerText,
    explanation: row.explanation || ''
  }
  showQuestionEditDialog.value = true
}

const saveQuestion = async () => {
  try {
    // 构建题目数据
    const questionData = {
      type: questionForm.value.type,
      question: questionForm.value.question,
      explanation: questionForm.value.explanation
    }

    // 处理选项
    if (['single', 'multiple'].includes(questionForm.value.type)) {
      const options = questionForm.value.options
        .filter(o => o.trim())
        .map((opt, idx) => `${String.fromCharCode(65 + idx)}. ${opt}`)
      questionData.options = JSON.stringify(options)
    } else {
      questionData.options = null
    }

    // 处理答案
    if (questionForm.value.type === 'multiple') {
      questionData.answer = JSON.stringify(questionForm.value.answerText.split(''))
    } else {
      questionData.answer = JSON.stringify(questionForm.value.answerText)
    }

    if (questionEditMode.value === 'add') {
      await api.post(`/quiz/admin/${currentQuizBank.value.id}/questions`, questionData)
      ElMessage.success('添加成功')
    } else {
      await api.put(`/quiz/admin/questions/${editingQuestionId.value}`, questionData)
      ElMessage.success('更新成功')
    }

    showQuestionEditDialog.value = false
    await loadQuestions(currentQuizBank.value.id)
    await loadQuizBanks() // 刷新题库列表以更新题目数量
  } catch (error) {
    ElMessage.error('保存失败')
  }
}

const addOption = () => {
  questionForm.value.options.push('')
}

const removeOption = (index) => {
  questionForm.value.options.splice(index, 1)
}

const handleDeleteQuestion = async (row) => {
  await ElMessageBox.confirm('确定删除该题目？', '提示')
  try {
    await api.delete(`/quiz/admin/questions/${row.id}`)
    ElMessage.success('删除成功')
    await loadQuestions(currentQuizBank.value.id)
    await loadQuizBanks() // 刷新题库列表以更新题目数量
  } catch (error) {
    ElMessage.error('删除失败')
  }
}

const getQuestionTypeLabel = (type) => {
  const map = {
    single: '单选题',
    multiple: '多选题',
    judge: '判断题',
    blank: '填空题',
    fill: '填空题'
  }
  return map[type] || type
}

onMounted(loadQuizBanks)
</script>
