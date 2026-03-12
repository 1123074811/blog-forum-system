<template>
  <div class="max-w-4xl mx-auto">
    <div class="mb-4">
      <el-button @click="handleBack" :icon="ArrowLeft" text>返回</el-button>
    </div>

    <div class="glass rounded-xl p-6">
      <h2 class="text-xl font-bold mb-6 dark:text-white">{{ isEdit ? '编辑文章' : '写文章' }}</h2>

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
          <div class="flex items-center gap-3 mb-4">
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
            <span class="text-gray-500 dark:text-gray-400">支持 .md / .markdown 格式</span>
          </div>

          <!-- 从链接导入 -->
          <div class="mb-6">
            <div class="flex items-center gap-3">
              <el-input
                v-model="crawlUrl"
                placeholder="请输入文章链接"
                size="default"
                style="min-width: 300px;"
                clearable
              />
              <el-button
                size="default"
                type="primary"
                :loading="crawling"
                :disabled="!crawlUrl"
                @click="handleCrawl"
              >
                <el-icon class="mr-1"><Link /></el-icon>
                {{ crawling ? '导入中...' : '导入' }}
              </el-button>
              <span class="text-gray-500 dark:text-gray-400 whitespace-nowrap">粘贴文章链接（支持 CSDN、掘金、博客园、知乎）</span>
            </div>
          </div>

          <MdEditor v-model="form.content" :theme="userStore.isDark ? 'dark' : 'light'" style="height: 500px" @onUploadImg="handleUploadImg" />
        </el-form-item>

        <el-form-item>
          <div class="flex gap-4">
            <el-button type="primary" size="large" @click="handleSubmit('published')">发布文章</el-button>
            <el-button size="large" @click="handleSubmit('draft')">保存草稿</el-button>
          </div>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { getArticle, createArticle, updateArticle, getCategories, getTags, uploadFile, crawlArticle } from '@/api/blog'
import { MdEditor } from 'md-editor-v3'
import 'md-editor-v3/lib/style.css'
import { ElMessage } from 'element-plus'
import { Upload, Link, ArrowLeft } from '@element-plus/icons-vue'

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
