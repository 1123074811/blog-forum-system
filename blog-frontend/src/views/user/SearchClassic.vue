<template>
  <div class="max-w-4xl mx-auto">
    <div class="glass rounded-xl p-6 mb-6">
      <el-input v-model="searchQuery" placeholder="搜索文章..." size="large" @keyup.enter="handleSearch">
        <template #prefix><el-icon><Search /></el-icon></template>
        <template #append><el-button @click="handleSearch">搜索</el-button></template>
      </el-input>
    </div>

    <div class="space-y-4">
      <div v-for="article in articles" :key="article.id" class="glass rounded-xl p-4 cursor-pointer hover:shadow-lg transition-shadow" @click="router.push(`/article/${article.id}`)">
        <h2 class="text-lg font-semibold mb-2 dark:text-white">{{ article.title }}</h2>
        <p class="text-gray-600 dark:text-gray-400 line-clamp-2 mb-3">{{ article.content?.substring(0, 150) }}...</p>
        <div class="flex items-center gap-4 text-sm text-gray-500">
          <span>{{ article.createdAt }}</span>
          <span><el-icon><View /></el-icon> {{ article.viewCount }}</span>
        </div>
      </div>

      <div v-if="loading" class="text-center py-8"><el-icon class="is-loading"><Loading /></el-icon></div>
      <div v-else-if="!articles.length && searched" class="text-center py-8 text-gray-500">未找到相关文章</div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getArticles } from '@/api/blog'
import { Search, View, Loading } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()

const searchQuery = ref(route.query.q || '')
const articles = ref([])
const loading = ref(false)
const searched = ref(false)

const handleSearch = async () => {
  if (!searchQuery.value.trim()) return
  loading.value = true
  searched.value = true
  try {
    const res = await getArticles({ search: searchQuery.value })
    if (res.success) articles.value = res.data.data
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  if (searchQuery.value) handleSearch()
})
</script>
