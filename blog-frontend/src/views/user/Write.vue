<template>
  <div class="write-page max-w-4xl mx-auto px-1 sm:px-0">
    <div class="mb-4">
      <el-button @click="handleBack" :icon="ArrowLeft" text>返回</el-button>
    </div>

    <div class="compose-card post-card p-4 sm:p-6">
      <h2 class="jp-write-title text-xl font-bold mb-6 dark:text-white">
        <span class="stamp">{{ isEdit ? '改' : '写' }}</span>{{ isEdit ? '编辑文章' : '写文章' }}
      </h2>

      <el-form :model="form" label-position="top">
        <el-form-item label="标题">
          <el-input v-model="form.title" placeholder="请输入文章标题" size="large" />
        </el-form-item>

        <el-form-item label="分类">
          <el-select v-model="form.categoryId" placeholder="选择分类" class="w-full">
            <el-option v-for="cat in categories" :key="cat.id" :label="cat.name" :value="cat.id" />
          </el-select>
        </el-form-item>

        <el-form-item label="标签">
          <el-select v-model="form.tags" multiple placeholder="选择标签" class="w-full">
            <el-option v-for="tag in tags" :key="tag.id" :label="tag.name" :value="tag.id" />
          </el-select>
        </el-form-item>

        <el-form-item label="内容">
          <!-- 导入 MD 文件 -->
          <div class="import-row flex flex-wrap items-center gap-3 mb-4">
            <el-upload
              :show-file-list="false"
              accept=".md,.markdown"
              :before-upload="handleImportMd"
            >
              <el-button size="default">
                <el-icon class="mr-1"><Upload /></el-icon>
                导入 MD 文件
              </el-button>
            </el-upload>
            <span class="text-gray-500 dark:text-gray-400 text-sm">支持 .md / .markdown 格式</span>
          </div>

          <!-- 从链接导入 -->
          <div class="mb-6">
            <div class="crawl-row flex flex-col sm:flex-row items-stretch sm:items-center gap-2 sm:gap-3">
              <el-input
                v-model="crawlUrl"
                placeholder="请输入文章链接"
                size="default"
                class="w-full sm:min-w-[300px]"
                clearable
              />
              <el-button
                size="default"
                type="primary"
                :loading="crawling"
                :disabled="!crawlUrl"
                @click="handleCrawl"
                class="crawl-btn flex-shrink-0"
              >
                <el-icon class="mr-1"><Link /></el-icon>
                {{ crawling ? '导入中...' : '导入' }}
              </el-button>
            </div>
            <p class="text-gray-500 dark:text-gray-400 text-xs sm:text-sm mt-2">粘贴文章链接（支持 CSDN、掘金、博客园、知乎）</p>
          </div>

          <div class="editor-shell">
            <MdEditor v-model="form.content" :theme="userStore.isDark ? 'dark' : 'light'" class="md-editor-mobile" @onUploadImg="handleUploadImg" />
          </div>
        </el-form-item>

        <el-form-item class="submit-form-item">
          <div class="submit-actions flex flex-col sm:flex-row gap-3 sm:gap-4 w-full sm:w-auto">
            <el-button type="primary" size="large" class="action-btn action-btn-primary w-full sm:w-auto" @click="handleSubmit('published')">发布文章</el-button>
            <el-button size="large" class="action-btn action-btn-secondary w-full sm:w-auto" @click="handleSubmit('draft')">保存草稿</el-button>
          </div>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed, defineAsyncComponent } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { getArticle, createArticle, updateArticle, getCategories, getTags, uploadFile, crawlArticle } from '@/api/blog'
import { ElMessage } from 'element-plus'
import { Upload, Link, ArrowLeft } from '@element-plus/icons-vue'

// 按需加载 md-editor-v3（~200KB）
const MdEditor = defineAsyncComponent(() =>
  import('md-editor-v3').then(async (m) => {
    await import('md-editor-v3/lib/style.css')
    return m.MdEditor
  })
)

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const isEdit = computed(() => !!route.params.id)
const categories = ref([])
const tags = ref([])
const form = ref({ title: '', content: '', categoryId: null, tags: [] })
const crawlUrl = ref('')
const crawling = ref(false)

const handleImportMd = (file) => {
  const reader = new FileReader()
  reader.onload = (e) => {
    form.value.content = e.target.result
    // 尝试从文件名提取标题
    if (!form.value.title) {
      form.value.title = file.name.replace(/\.(md|markdown)$/i, '')
    }
    ElMessage.success('导入成功')
  }
  reader.onerror = () => ElMessage.error('文件读取失败')
  reader.readAsText(file)
  return false // 阻止默认上传
}

const handleUploadImg = async (files, callback) => {
  const urls = []
  for (const file of files) {
    const res = await uploadFile(file)
    if (res.success) urls.push(res.data.url)
  }
  callback(urls)
}

const handleCrawl = async () => {
  if (!crawlUrl.value) {
    ElMessage.warning('请输入文章链接')
    return
  }

  crawling.value = true
  try {
    const res = await crawlArticle(crawlUrl.value)
    if (res.success) {
      form.value.title = res.data.title
      form.value.content = res.data.content
      ElMessage.success('导入成功')
      crawlUrl.value = ''
    } else {
      ElMessage.error(res.message || '导入失败')
    }
  } catch (error) {
    ElMessage.error('导入失败，请检查链接是否正确')
  } finally {
    crawling.value = false
  }
}

const handleBack = () => {
  if (window.history.length > 1) {
    router.back()
  } else {
    router.push('/')
  }
}

const handleSubmit = async (status) => {
  if (!form.value.title || !form.value.content) {
    ElMessage.warning('请填写标题和内容')
    return
  }

  const data = { ...form.value, status }
  let res
  if (isEdit.value) {
    res = await updateArticle(route.params.id, data)
  } else {
    res = await createArticle(data)
  }

  if (res.success) {
    ElMessage.success(status === 'published' ? '发布成功' : '保存成功')
    // 发布成功返回首页，保存草稿跳转到文章详情
    if (status === 'published') {
      router.push('/')
    } else {
      router.push(`/article/${res.data.id}`)
    }
  } else {
    ElMessage.error(res.message)
  }
}

onMounted(async () => {
  const [catRes, tagRes] = await Promise.all([getCategories(), getTags()])
  if (catRes.success) categories.value = catRes.data
  if (tagRes.success) tags.value = tagRes.data

  if (isEdit.value) {
    const res = await getArticle(route.params.id)
    if (res.success) {
      form.value = {
        title: res.data.title,
        content: res.data.content,
        categoryId: res.data.categoryId,
        tags: res.data.tags ? res.data.tags.map(t => t.id) : []
      }
    }
  }
})
</script>

<style scoped>
.write-page {
  max-width: 880px;
}

.jp-write-title {
  display: flex;
  align-items: center;
  gap: 8px;
}

.jp-write-title .stamp {
  margin-right: 0 !important;
  transform: rotate(-10deg) scale(0.85);
}

.md-editor-mobile {
  height: 500px;
}

.editor-shell {
  border-radius: 12px;
  overflow: hidden;
}

@media (min-width: 769px) {
  .submit-actions {
    align-items: center;
  }
}

@media (max-width: 768px) {
  .write-page {
    padding-left: 6px;
    padding-right: 6px;
  }

  .compose-card {
    border-radius: 14px;
    padding: 14px;
  }

  .import-row {
    align-items: flex-start;
    gap: 8px;
    margin-bottom: 12px;
  }

  .crawl-row {
    gap: 8px;
  }

  .crawl-btn {
    align-self: flex-start;
    min-width: 112px;
    height: 40px;
  }

  .submit-actions {
    position: fixed;
    left: 10px;
    right: 10px;
    bottom: calc(10px + env(safe-area-inset-bottom));
    width: auto;
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 10px;
    padding: 10px;
    border-radius: 14px;
    background: rgba(241, 248, 255, 0.9);
    backdrop-filter: blur(12px);
    border: 1px solid rgba(255, 255, 255, 0.65);
    box-shadow: 0 10px 28px rgba(14, 165, 233, 0.18);
    z-index: 40;
  }

  .action-btn {
    margin: 0 !important;
    width: 100% !important;
    height: 42px;
    border-radius: 10px;
    font-size: 15px;
    font-weight: 600;
  }

  .action-btn-secondary {
    background: rgba(255, 255, 255, 0.82);
  }

  .md-editor-mobile {
    height: 100%;
  }

  .editor-shell {
    height: clamp(240px, 42vh, 420px);
  }

  :deep(.el-form-item) {
    margin-bottom: 14px;
  }

  .submit-form-item {
    margin-bottom: 0 !important;
  }

  :deep(.el-input__wrapper),
  :deep(.el-select__wrapper) {
    min-height: 42px;
  }

  :deep(.md-editor-toolbar) {
    padding: 6px 4px;
    gap: 2px;
    overflow-x: auto;
  }

  :deep(.md-editor-toolbar-item) {
    width: 30px;
    height: 30px;
  }

  :deep(.md-editor-footer) {
    padding: 6px 8px;
  }

  :deep(.md-editor-input-wrapper),
  :deep(.md-editor-preview-wrapper) {
    overflow: auto;
  }

  .write-page {
    padding-bottom: calc(106px + env(safe-area-inset-bottom));
  }
}

@media (max-width: 480px) {
  .md-editor-mobile {
    height: 100%;
  }

  .editor-shell {
    height: clamp(220px, 38vh, 360px);
  }
}

@media (max-width: 430px) {
  .submit-actions {
    grid-template-columns: 1fr;
  }

  .md-editor-mobile {
    height: 100%;
  }

  .editor-shell {
    height: clamp(200px, 34vh, 320px);
  }
}
</style>
