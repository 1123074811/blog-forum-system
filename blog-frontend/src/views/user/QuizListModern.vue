<template>
  <div class="max-w-4xl mx-auto">
    <div class="jp-study-card jp-quizlist-shell p-3 sm:p-6">
      <!-- 移动端优化布局 -->
      <div class="space-y-3 sm:space-y-0 mb-4 sm:mb-6">
        <!-- 标题行 -->
        <div class="flex items-center justify-between">
          <h2 class="jp-quizlist-title text-lg sm:text-xl font-bold dark:text-white"><span class="stamp">题</span>我的题库</h2>
          <el-button v-if="activeTab === 'mine' && isMobile" type="primary" size="small" @click="showActionSheet = true">
            操作
          </el-button>
        </div>
        
        <!-- 标签切换按钮 -->
        <div class="flex items-center justify-between gap-2">
          <el-button-group class="flex-1 sm:flex-initial">
            <el-button 
              :type="activeTab === 'public' ? 'primary' : 'default'" 
              :size="isMobile ? 'small' : 'default'"
              class="flex-1 sm:flex-initial"
              @click="switchTab('public')">公开题库</el-button>
            <el-button 
              :type="activeTab === 'mine' ? 'primary' : 'default'" 
              :size="isMobile ? 'small' : 'default'"
              class="flex-1 sm:flex-initial"
              @click="switchTab('mine')">我的题库</el-button>
          </el-button-group>
          
          <!-- PC端操作按钮 -->
          <div v-if="activeTab === 'mine' && !isMobile" class="flex gap-2">
            <el-button type="primary" @click="showImportDialog = true">导入题目</el-button>
            <el-button @click="showUploadDialog = true">上传文件</el-button>
          </div>
        </div>
      </div>

      <!-- 题库列表 -->
      <div class="space-y-3 sm:space-y-4">
        <div v-for="quiz in displayList" :key="quiz.id"
             class="jp-quiz-item p-3 sm:p-4 cursor-pointer"
             @click="handleItemClick(quiz)">
          <!-- 水平布局 -->
          <div class="flex justify-between gap-3 items-center">
            <!-- 左侧内容 -->
            <div class="flex items-center gap-2 sm:gap-3 flex-1 min-w-0">
              <el-tag :type="quiz.type === 'file' ? 'warning' : 'success'" size="small" class="flex-shrink-0">
                {{ quiz.type === 'file' ? '文件' : '题目' }}
              </el-tag>
              <div class="flex-1 min-w-0">
                <h3 class="font-medium dark:text-white text-sm sm:text-base truncate">{{ quiz.title }}</h3>
                <p class="text-xs sm:text-sm text-gray-500 mt-1 truncate">
                  <template v-if="quiz.type === 'file'">{{ quiz.fileType?.toUpperCase() }} 文件</template>
                  <template v-else>{{ quiz.questionCount }} 道题</template>
                  · {{ formatDate(quiz.createdAt) }}
                </p>
              </div>
            </div>
            
            <!-- 右侧操作按钮 -->
            <div v-if="activeTab === 'mine'" class="flex gap-2 flex-shrink-0">
              <!-- 移动端垂直排列 -->
              <template v-if="isMobile">
                <div class="flex flex-col gap-2 items-end mr-2" style="width: 56px;">
                  <el-button 
                    :type="quiz.isPublic ? 'success' : 'info'" 
                    size="small" 
                    style="width: 56px; height: 28px; padding: 0; font-size: 12px; min-width: 56px;"
                    @click.stop="handleTogglePublic(quiz)">
                    {{ quiz.isPublic ? '公开' : '私有' }}
                  </el-button>
                  <el-button 
                    type="danger" 
                    size="small" 
                    style="width: 56px; height: 28px; padding: 0; font-size: 12px; min-width: 56px;"
                    @click.stop="handleDelete(quiz.id)">
                    删除
                  </el-button>
                </div>
              </template>
              <!-- PC端水平排列 -->
              <template v-else>
                <el-button 
                  :type="quiz.isPublic ? 'success' : 'info'" 
                  size="default"
                  @click.stop="handleTogglePublic(quiz)">
                  {{ quiz.isPublic ? '公开' : '私有' }}
                </el-button>
                <el-button 
                  type="danger" 
                  size="default"
                  @click.stop="handleDelete(quiz.id)">
                  删除
                </el-button>
              </template>
            </div>
          </div>
        </div>
        
        <!-- 空状态 -->
        <div v-if="!displayList.length" class="text-center py-8 text-gray-500 text-sm">
          {{ activeTab === 'public' ? '暂无公开题库' : '暂无题库，点击上方按钮导入' }}
        </div>
      </div>
    </div>

    <!-- 移动端操作面板 -->
    <el-drawer v-model="showActionSheet" direction="btt" size="auto" :with-header="false">
      <div class="p-4 space-y-2">
        <el-button type="primary" size="large" class="w-full" @click="showImportDialog = true; showActionSheet = false">
          导入题目
        </el-button>
        <el-button size="large" class="w-full" @click="showUploadDialog = true; showActionSheet = false">
          上传文件
        </el-button>
        <el-button size="large" class="w-full" @click="showActionSheet = false">
          取消
        </el-button>
      </div>
    </el-drawer>

    <!-- 导入题目对话框 -->
    <el-dialog v-model="showImportDialog" title="导入题目" :width="isMobile ? '95%' : '650px'" :fullscreen="isMobile" top="8vh" @close="importContent = ''">
      <div class="mb-3">
        <div class="flex items-center justify-between mb-2">
          <span class="text-sm text-gray-600 dark:text-gray-400">格式示例：</span>
          <el-button size="small" text @click="copyFormatExample">复制示例</el-button>
        </div>
        <pre class="bg-gray-50 dark:bg-gray-800 p-3 rounded text-xs overflow-auto select-text" style="max-height: 200px;">{
  "title": "题库名称",
  "questions": [
    {"type": "single", "question": "单选题内容", "options": ["选项1", "选项2", "选项3"], "answer": "选项1", "explanation": "解析"},
    {"type": "multiple", "question": "多选题内容", "options": ["选项1", "选项2", "选项3"], "answer": ["选项1", "选项2"], "explanation": "解析"},
    {"type": "judge", "question": "判断题内容", "answer": true, "explanation": "解析"},
    {"type": "short", "question": "简答题内容", "answer": "参考答案", "explanation": "解析"}
  ]
}</pre>
      </div>
      <div class="mb-3">
        <div class="flex items-center justify-between mb-2">
          <span class="text-sm text-gray-600 dark:text-gray-400">AI转换提示词：</span>
          <el-button size="small" text @click="copyAiPrompt">复制提示词</el-button>
        </div>
        <pre class="bg-gray-50 dark:bg-gray-800 p-3 rounded text-xs overflow-auto select-text" style="max-height: 200px;">{{ aiPromptText }}</pre>
      </div>
      <el-input v-model="importContent" type="textarea" :autosize="{ minRows: 6 }" placeholder="请输入或粘贴JSON格式的题库内容" style="max-height: 200px; overflow: auto;" />
      <div class="mt-4">
        <el-upload :show-file-list="false" :before-upload="handleFileUpload" accept=".json,.txt">
          <el-button>从文件导入</el-button>
        </el-upload>
      </div>
      <template #footer>
        <el-button @click="showImportDialog = false">取消</el-button>
        <el-button type="primary" @click="handleImport" :loading="importing">导入</el-button>
      </template>
    </el-dialog>

    <!-- 上传文件对话框 -->
    <el-dialog v-model="showUploadDialog" title="上传文件" :width="isMobile ? '95%' : '500px'" :fullscreen="isMobile" @close="resetUploadDialog">
      <el-form label-width="80px">
        <el-form-item label="文件标题">
          <el-input v-model="uploadTitle" placeholder="请输入文件标题" />
        </el-form-item>
        <el-form-item label="选择文件">
          <el-upload ref="uploadRef" :auto-upload="false" :limit="1" :on-change="handleUploadChange" :file-list="fileList"
                     accept=".txt,.docx,.pptx,.xlsx,.pdf,.md,.json">
            <el-button>选择文件</el-button>
            <template #tip>
              <div class="text-gray-500 text-sm mt-2">支持 txt、docx、pptx、xlsx、pdf、md、json 格式</div>
            </template>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showUploadDialog = false">取消</el-button>
        <el-button type="primary" @click="handleUpload" :loading="uploading">上传</el-button>
      </template>
    </el-dialog>

    <!-- 文件预览对话框 -->
    <el-dialog v-model="showPreviewDialog" :title="previewTitle" :width="isMobile ? '100%' : '90%'" :top="isMobile ? '0' : '3vh'" :fullscreen="isMobile" @close="resetPreviewState">
      <div class="flex gap-2 mb-2">
        <el-button :type="isPanMode ? 'primary' : 'default'" size="small" @click="isPanMode = !isPanMode">
          <svg class="w-4 h-4" viewBox="0 0 24 24" fill="currentColor"><path d="M10 9h4V6h3l-5-5-5 5h3v3zm-1 1H6V7l-5 5 5 5v-3h3v-4zm14 2l-5-5v3h-3v4h3v3l5-5zm-9 3h-4v3H7l5 5 5-5h-3v-3z"/></svg>
          <span class="ml-1">{{ isPanMode ? '拖拽中' : '手型工具' }}</span>
        </el-button>
        <el-button size="small" @click="downloadFile">下载文件</el-button>
      </div>
      <div ref="previewContainer" class="preview-container relative" :class="{ 'cursor-grab': isPanMode, 'cursor-grabbing': isDragging }"
           style="height: 75vh; overflow: auto;" @mousedown="startDrag" @mousemove="onDrag" @mouseup="stopDrag" @mouseleave="stopDrag">
        <div v-if="previewType === 'pdf'" class="w-full h-full">
          <iframe :src="previewUrl" style="width: 100%; height: 100%; border: none;" />
        </div>
        <pre v-else-if="previewType === 'text'" class="whitespace-pre-wrap p-4 bg-gray-100 dark:bg-gray-800 rounded select-text">{{ previewContent }}</pre>
        <div v-else-if="previewType === 'docx'" ref="docxContainer" class="bg-white p-4 min-h-full"></div>
        <div v-else-if="previewType === 'xlsx'" class="bg-white p-4 overflow-auto">
          <div v-for="(sheet, name) in xlsxData" :key="name" class="mb-4">
            <h3 class="font-bold mb-2 text-gray-800">{{ name }}</h3>
            <table class="border-collapse w-full text-sm">
              <tr v-for="(row, i) in sheet" :key="i">
                <td v-for="(cell, j) in row" :key="j" class="border border-gray-300 px-2 py-1">{{ cell }}</td>
              </tr>
            </table>
          </div>
        </div>
        <div v-else-if="previewType === 'pptx'" class="text-center py-8 text-gray-500">
          <p class="mb-4">PPT 文件暂不支持在线预览，请下载后查看</p>
          <el-button type="primary" @click="downloadFile">下载文件</el-button>
        </div>
        <!-- 部署后使用 Office Online Viewer（解开注释）
        <iframe v-else-if="previewType === 'office'" :src="previewUrl" style="width: 100%; height: 100%; border: none;" />
        -->
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { getQuizList, getPublicQuizList, importQuiz, deleteQuiz, toggleQuizPublic, uploadQuizFile } from '@/api/blog'
import { ElMessage, ElMessageBox } from 'element-plus'

// docx-preview 和 xlsx 按需加载，仅在预览文件时才引入
const loadDocxPreview = () => import('docx-preview').then(m => m.renderAsync)
const loadXlsx = () => import('xlsx')

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const activeTab = ref('public')
const publicList = ref([])
const myList = ref([])
const showImportDialog = ref(false)
const showUploadDialog = ref(false)
const showPreviewDialog = ref(false)
const showActionSheet = ref(false)
const isMobile = ref(window.innerWidth < 768)
const importContent = ref('')
const importing = ref(false)
const uploading = ref(false)
const uploadTitle = ref('')
const uploadFile = ref(null)
const fileList = ref([])
const uploadRef = ref(null)
const previewTitle = ref('')
const previewUrl = ref('')
const previewContent = ref('')
const previewType = ref('')
const originalFileUrl = ref('')
const isPanMode = ref(false)
const isDragging = ref(false)
const previewContainer = ref(null)
const docxContainer = ref(null)
const pdfContainer = ref(null)
const pdfCanvasRefs = ref([])
const pdfPages = ref(0)
const xlsxData = ref({})
let startX = 0, startY = 0, scrollLeft = 0, scrollTop = 0

const displayList = computed(() => activeTab.value === 'public' ? publicList.value : myList.value)

// 格式化日期 - 移动端显示简短格式
const formatDate = (dateStr) => {
  if (!dateStr) return ''
  if (!isMobile.value) return dateStr
  // 移动端显示简化格式: MM-DD HH:mm
  const match = dateStr.match(/(\d{4})-(\d{2})-(\d{2})\s+(\d{2}:\d{2})/)
  if (match) {
    return `${match[2]}-${match[3]} ${match[4]}`
  }
  return dateStr
}

// 监听窗口大小变化
const handleResize = () => {
  isMobile.value = window.innerWidth < 768
}

window.addEventListener('resize', handleResize)

const formatExampleText = `{
  "title": "题库名称",
  "questions": [
    {"type": "single", "question": "单选题内容", "options": ["选项1", "选项2", "选项3"], "answer": "选项1", "explanation": "解析"},
    {"type": "multiple", "question": "多选题内容", "options": ["选项1", "选项2", "选项3"], "answer": ["选项1", "选项2"], "explanation": "解析"},
    {"type": "judge", "question": "判断题内容", "answer": true, "explanation": "解析"},
    {"type": "short", "question": "简答题内容", "answer": "参考答案", "explanation": "解析"}
  ]
}`

const copyFormatExample = async () => {
  try {
    await navigator.clipboard.writeText(formatExampleText)
    ElMessage.success('示例已复制到剪贴板')
  } catch (e) {
    ElMessage.warning('复制失败，请手动选中复制')
  }
}

const aiPromptText = `请将以下题目转换为JSON格式，严格按照此规则：
1. 输出完整JSON结构，包含title和questions数组
2. 题目类型：单选题type为"single"，多选题为"multiple"，判断题为"judge"，简答题为"short"
3. 选项格式：options数组中只写选项内容，不要带A/B/C/D前缀
4. 答案格式：单选题answer为正确选项的完整内容；多选题answer为正确选项内容的数组；判断题answer为true或false
5. 解析：如果题目没有解析，请根据题目内容生成一段简要解析
6. 输出格式：
{
  "title": "题库名称",
  "questions": [
    {"type":"single","question":"问题","options":["选项1","选项2"],"answer":"选项1","explanation":"解析"}
  ]
}

请直接输出完整JSON，不要有其他内容。题目如下：`

const copyAiPrompt = async () => {
  try {
    await navigator.clipboard.writeText(aiPromptText)
    ElMessage.success('提示词已复制到剪贴板')
  } catch (e) {
    ElMessage.warning('复制失败，请手动选中复制')
  }
}

const switchTab = (tab) => {
  activeTab.value = tab
  // 更新URL查询参数
  router.replace({ query: { ...route.query, tab } })
  if (tab === 'public' && !publicList.value.length) fetchPublicList()
  if (tab === 'mine') {
    if (!userStore.isLoggedIn) {
      ElMessage.warning('请先登录')
      router.push('/login')
      return
    }
    if (!myList.value.length) fetchMyList()
  }
}

const fetchPublicList = async () => {
  const res = await getPublicQuizList()
  if (res.success) publicList.value = res.data
}

const fetchMyList = async () => {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    return
  }
  try {
    const res = await getQuizList()
    if (res.success) myList.value = res.data
  } catch (error) {
    console.error('获取我的题库失败:', error)
  }
}

const handleFileUpload = (file) => {
  const reader = new FileReader()
  reader.onload = (e) => { importContent.value = e.target.result }
  reader.readAsText(file)
  return false
}

const handleImport = async () => {
  if (!importContent.value.trim()) {
    ElMessage.warning('请输入题库内容')
    return
  }
  importing.value = true
  try {
    const res = await importQuiz(importContent.value)
    if (res.success) {
      ElMessage.success('导入成功')
      showImportDialog.value = false
      importContent.value = ''
      fetchMyList()
    } else {
      ElMessage.error(res.message || '导入失败')
    }
  } catch (e) {
    ElMessage.error('导入失败，请检查JSON格式')
  } finally {
    importing.value = false
  }
}

const handleUploadChange = (file) => {
  uploadFile.value = file.raw
  if (!uploadTitle.value) {
    uploadTitle.value = file.name.replace(/\.[^/.]+$/, '')
  }
}

const handleUpload = async () => {
  if (!uploadFile.value) {
    ElMessage.warning('请选择文件')
    return
  }
  if (!uploadTitle.value.trim()) {
    ElMessage.warning('请输入文件标题')
    return
  }
  uploading.value = true
  try {
    const res = await uploadQuizFile(uploadFile.value, uploadTitle.value)
    if (res.success) {
      ElMessage.success('上传成功')
      showUploadDialog.value = false
      uploadTitle.value = ''
      uploadFile.value = null
      fetchMyList()
    } else {
      ElMessage.error(res.message || '上传失败')
    }
  } catch (e) {
    ElMessage.error('上传失败')
  } finally {
    uploading.value = false
  }
}

const handleItemClick = (quiz) => {
  if (quiz.type === 'file') {
    previewFile(quiz)
  } else {
    // 跳转时带上当前标签页信息
    router.push({ path: `/quiz/${quiz.id}`, query: { from: activeTab.value } })
  }
}

const previewFile = async (quiz) => {
  previewTitle.value = quiz.title
  const ext = quiz.fileType?.toLowerCase()
  const fileUrl = quiz.filePath
  originalFileUrl.value = fileUrl
  xlsxData.value = {}
  pdfPages.value = 0
  pdfCanvasRefs.value = []

  if (ext === 'pdf') {
    previewType.value = 'pdf'
    previewUrl.value = fileUrl
  } else if (['txt', 'md', 'json'].includes(ext)) {
    previewType.value = 'text'
    const res = await fetch(fileUrl)
    previewContent.value = await res.text()
  } else if (ext === 'docx') {
    previewType.value = 'docx'
    showPreviewDialog.value = true
    await nextTick()
    const res = await fetch(fileUrl)
    const blob = await res.blob()
    const renderAsync = await loadDocxPreview()
    renderAsync(blob, docxContainer.value, null, { className: 'docx-preview' })
    return
  } else if (ext === 'xlsx') {
    previewType.value = 'xlsx'
    const res = await fetch(fileUrl)
    const data = await res.arrayBuffer()
    const XLSX = await loadXlsx()
    const workbook = XLSX.read(data, { type: 'array' })
    const result = {}
    workbook.SheetNames.forEach(name => {
      result[name] = XLSX.utils.sheet_to_json(workbook.Sheets[name], { header: 1 })
    })
    xlsxData.value = result
  } else if (ext === 'pptx') {
    previewType.value = 'pptx'
  } else {
    // 部署后使用 Office Online Viewer（解开下面注释，注释上面的 else if）
    // previewType.value = 'office'
    // previewUrl.value = `https://view.officeapps.live.com/op/embed.aspx?src=${encodeURIComponent(fileUrl)}`
    previewType.value = 'pdf'
    previewUrl.value = fileUrl
  }
  showPreviewDialog.value = true
}

const startDrag = (e) => {
  if (!isPanMode.value) return
  isDragging.value = true
  startX = e.pageX - previewContainer.value.offsetLeft
  startY = e.pageY - previewContainer.value.offsetTop
  scrollLeft = previewContainer.value.scrollLeft
  scrollTop = previewContainer.value.scrollTop
}

const onDrag = (e) => {
  if (!isDragging.value || !isPanMode.value) return
  e.preventDefault()
  const x = e.pageX - previewContainer.value.offsetLeft
  const y = e.pageY - previewContainer.value.offsetTop
  previewContainer.value.scrollLeft = scrollLeft - (x - startX)
  previewContainer.value.scrollTop = scrollTop - (y - startY)
}

const stopDrag = () => { isDragging.value = false }

const resetPreviewState = () => {
  isPanMode.value = false
  isDragging.value = false
  previewType.value = ''
  previewUrl.value = ''
  previewContent.value = ''
  xlsxData.value = {}
  pdfPages.value = 0
  pdfCanvasRefs.value = []
}

const resetUploadDialog = () => {
  uploadTitle.value = ''
  uploadFile.value = null
  fileList.value = []
}

const downloadFile = () => {
  window.open(originalFileUrl.value, '_blank')
}

const handleDelete = async (id) => {
  await ElMessageBox.confirm('确定删除该题库？', '提示', { type: 'warning' })
  const res = await deleteQuiz(id)
  if (res.success) {
    ElMessage.success('删除成功')
    fetchMyList()
  }
}

const handleTogglePublic = async (quiz) => {
  const res = await toggleQuizPublic(quiz.id, !quiz.isPublic)
  if (res.success) {
    quiz.isPublic = !quiz.isPublic
    ElMessage.success(quiz.isPublic ? '已设为公开' : '已设为私有')
  }
}

onMounted(() => {
  // 从URL查询参数恢复标签页状态
  const tabFromQuery = route.query.tab
  if (tabFromQuery === 'mine') {
    if (!userStore.isLoggedIn) {
      ElMessage.warning('请先登录')
      router.push('/login')
      return
    }
    activeTab.value = 'mine'
    fetchMyList()
  } else {
    activeTab.value = 'public'
    fetchPublicList()
  }
})

// 清理事件监听
onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
})
</script>

<style scoped>
.jp-quizlist-shell {
  padding: 16px 14px !important;
}

.jp-quizlist-title {
  display: flex;
  align-items: center;
  gap: 8px;
}

.jp-quizlist-title .stamp {
  margin-right: 0 !important;
  transform: rotate(-10deg) scale(0.8);
}

.jp-quiz-item {
  background: rgba(255, 255, 255, 0.42);
  border: 1px dashed rgba(44, 62, 80, 0.34);
  border-radius: 0 !important;
  padding: 14px 14px !important;
  transition: background 0.2s ease, border-color 0.2s ease;
}

.jp-quiz-item:hover {
  background: rgba(211, 84, 0, 0.06);
  border-color: var(--ink);
  transform: none;
  box-shadow: none;
}

.dark .jp-quiz-item {
  background: rgba(15, 23, 42, 0.4);
  border-color: rgba(148, 163, 184, 0.24);
}

.dark .jp-quiz-item:hover {
  background: rgba(56, 189, 248, 0.1);
  border-color: var(--accent-primary);
}
</style>
