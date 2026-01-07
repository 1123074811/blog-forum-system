<template>
  <div class="max-w-4xl mx-auto">
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
          <div class="flex items-center gap-3 mb-3">
            <el-upload
              :show-file-list="false"
              accept=".md,.markdown"
              :before-upload="handleImportMd"
            >
              <el-button size="small">
                <el-icon class="mr-1"><Upload /></el-icon>
                导入 MD 文件
              </el-button>
            </el-upload>
            <span class="text-xs text-gray-400">支持 .md / .markdown 格式</span>
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
import { getArticle, createArticle, updateArticle, getCategories, getTags, uploadFile } from '@/api/blog'
import { MdEditor } from 'md-editor-v3'
import 'md-editor-v3/lib/style.css'
import { ElMessage } from 'element-plus'
import { Upload } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const isEdit = computed(() => !!route.params.id)
const categories = ref([])
const tags = ref([])
const form = ref({ title: '', content: '', categoryId: null, tags: [] })

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
    router.push(`/article/${res.data.id}`)
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
      form.value = { title: res.data.title, content: res.data.content, categoryId: res.data.categoryId, tags: [] }
    }
  }
})
</script>
