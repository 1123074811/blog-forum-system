<template>
  <div class="max-w-4xl mx-auto">
    <h1 class="jp-page-title text-2xl font-bold mb-6 dark:text-white">
      <span class="stamp">藏</span>我的收藏
    </h1>
    <div class="space-y-4">
      <div v-for="article in articles"
           :key="article.id"
           class="post-card jp-favorites-item cursor-pointer"
           @click="router.push(`/article/${article.id}`)">
        <div class="flex items-center gap-2 mb-2">
          <el-avatar :src="article.authorAvatar" :size="32">{{ article.authorName?.[0] || 'U' }}</el-avatar>
          <span class="text-sm font-medium dark:text-gray-300">{{ article.authorName || '匿名用户' }}</span>
          <span class="text-sm text-gray-400">{{ article.createdAt }}</span>
        </div>
        <h2 class="jp-favorites-title text-lg font-semibold mb-2 dark:text-white">
          <span class="stamp">文</span>{{ article.title }}
        </h2>
        <p class="text-gray-600 dark:text-gray-400 line-clamp-2">{{ article.content?.substring(0, 150) }}...</p>
      </div>
      <el-empty v-if="!loading && articles.length === 0" description="暂无收藏" />
      <div v-if="hasMore" class="text-center py-4">
        <el-button :loading="loading" @click="loadMore">加载更多</el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import api from '@/api'

const router = useRouter()
const articles = ref([])
const page = ref(1)
const hasMore = ref(true)
const loading = ref(false)

const fetchFavorites = async () => {
  loading.value = true
  const res = await api.get('/articles/my/favorites', { params: { page: page.value, limit: 10 } })
  if (res.success) {
    articles.value.push(...res.data.data)
    hasMore.value = articles.value.length < res.data.total
  }
  loading.value = false
}

const loadMore = () => { page.value++; fetchFavorites() }

onMounted(fetchFavorites)
</script>

<style scoped>
.jp-page-title {
  display: flex;
  align-items: center;
  gap: 8px;
}

.jp-page-title .stamp {
  margin-right: 0 !important;
  transform: rotate(-10deg) scale(0.9);
}

.jp-favorites-item {
  padding: 20px 18px !important;
}

.jp-favorites-title {
  display: flex;
  align-items: center;
  gap: 8px;
}

.jp-favorites-title .stamp {
  margin-right: 0 !important;
  transform: rotate(-10deg) scale(0.7);
}

@media (min-width: 640px) {
  .jp-favorites-item {
    padding: 24px 24px !important;
  }
}
</style>
