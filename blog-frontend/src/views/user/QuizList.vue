<template>
  <div class="max-w-4xl mx-auto">
    <div class="glass rounded-xl p-6">
      <div class="flex justify-between items-center mb-6">
        <div class="flex items-center gap-4">
          <h2 class="text-xl font-bold dark:text-white">{{ activeTab === 'public' ? '公开题库' : '我的题库' }}</h2>
          <el-button-group>
            <el-button :type="activeTab === 'public' ? 'primary' : 'default'" @click="switchTab('public')">公开题库</el-button>
            <el-button :type="activeTab === 'mine' ? 'primary' : 'default'" @click="switchTab('mine')">我的题库</el-button>
          </el-button-group>
        </div>
        <div v-if="activeTab === 'mine'" class="flex gap-2">
          <el-button type="primary" @click="showImportDialog = true">导入题目</el-button>
          <el-button @click="showUploadDialog = true">上传文件</el-button>
        </div>
      </div>

      <div class="space-y-4">
        <div v-for="quiz in displayList" :key="quiz.id"
             class="p-4 border border-gray-200 dark:border-gray-700 rounded-lg hover:bg-gray-50 dark:hover:bg-gray-800 cursor-pointer flex justify-between items-center"
             @click="handleItemClick(quiz)">
          <div class="flex items-center gap-3">
            <el-tag :type="quiz.type === 'file' ? 'warning' : 'success'" size="small">
              {{ quiz.type === 'file' ? '文件' : '题目' }}
            </el-tag>
            <div>
              <h3 class="font-medium dark:text-white">{{ quiz.title }}</h3>
              <p class="text-sm text-gray-500 mt-1">
                <template v-if="quiz.type === 'file'">{{ quiz.fileType?.toUpperCase() }} 文件</template>
                <template v-else>{{ quiz.questionCount }} 道题</template>
                · {{ quiz.createdAt }}
              </p>
            </div>
          </div>
          <div v-if="activeTab === 'mine'" class="flex gap-2">
            <el-button :type="quiz.isPublic ? 'success' : 'info'" size="small" @click.stop="handleTogglePublic(quiz)">{{ quiz.isPublic ? '公开' : '私有' }}</el-button>
            <el-button type="danger" size="small" @click.stop="handleDelete(quiz.id)">删除</el-button>
          </div>
        </div>
        <div v-if="!displayList.length" class="text-center py-8 text-gray-500">
          {{ activeTab === 'public' ? '暂无公开题库' : '暂无题库，点击上方按钮导入' }}
        </div>
      </div>
    </div>

    <!-- 导入题目对话框 -->
    <el-dialog v-model="showImportDialog" title="导入题目" width="600px">
      <el-input v-model="importContent" type="textarea" :autosize="{ minRows: 8, maxRows: 20 }" placeholder='请输入JSON格式的题库内容，例如：
{
  "title": "题库名称",
  "questions": [
    {"type": "single", "question": "单选题内容", "options": ["A. 选项1", "B. 选项2"], "answer": "A", "explanation": "解析"},
    {"type": "multiple", "question": "多选题内容", "options": ["A. 选项1", "B. 选项2"], "answer": ["A", "B"], "explanation": "解析"},
    {"type": "judge", "question": "判断题内容", "answer": true, "explanation": "解析"}
  ]
}' />
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
    <el-dialog v-model="showUploadDialog" title="上传文件" width="500px" @close="resetUploadDialog">
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
    <el-dialog v-model="showPreviewDialog" :title="previewTitle" width="90%" top="3vh" @close="resetPreviewState">
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
import { ref, computed, onMounted, nextTick } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { getQuizList, getPublicQuizList, importQuiz, deleteQuiz, toggleQuizPublic, uploadQuizFile } from '@/api/blog'
import { ElMessage, ElMessageBox } from 'element-plus'
import { renderAsync } from 'docx-preview'
import * as XLSX from 'xlsx'

const router = useRouter()
const route = useRoute()
const activeTab = ref('public')
const publicList = ref([])
const myList = ref([])
const showImportDialog = ref(false)
const showUploadDialog = ref(false)
const showPreviewDialog = ref(false)
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

const switchTab = (tab) => {
  activeTab.value = tab
  // 更新URL查询参数
  router.replace({ query: { ...route.query, tab } })
  if (tab === 'public' && !publicList.value.length) fetchPublicList()
  if (tab === 'mine' && !myList.value.length) fetchMyList()
}

const fetchPublicList = async () => {
  const res = await getPublicQuizList()
  if (res.success) publicList.value = res.data
}

const fetchMyList = async () => {
  const res = await getQuizList()
  if (res.success) myList.value = res.data
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
    renderAsync(blob, docxContainer.value, null, { className: 'docx-preview' })
    return
  } else if (ext === 'xlsx') {
    previewType.value = 'xlsx'
    const res = await fetch(fileUrl)
    const data = await res.arrayBuffer()
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
    activeTab.value = 'mine'
    fetchMyList()
  } else {
    activeTab.value = 'public'
    fetchPublicList()
  }
})
</script>
