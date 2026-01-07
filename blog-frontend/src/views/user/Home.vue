<template>
  <div class="grid grid-cols-1 lg:grid-cols-4 gap-6">
    <!-- 左侧边栏 -->
    <aside class="hidden lg:block">
      <div ref="leftSidebarRef" class="sticky space-y-4" :style="{ top: leftSidebarTop }">
      <!-- 用户卡片 -->
      <div v-if="userStore.isLoggedIn" class="glass rounded-xl p-4">
        <div class="flex items-center gap-3 mb-4">
          <el-avatar :src="userStore.user?.avatar" :size="48">{{ (userStore.user?.nickname || userStore.user?.username)?.[0] }}</el-avatar>
          <div>
            <div class="font-semibold dark:text-white">{{ userStore.user?.nickname || userStore.user?.username }}</div>
            <div class="text-sm text-gray-500">{{ userStore.user?.bio || '暂无简介' }}</div>
          </div>
        </div>
        <el-button type="primary" class="w-full" @click="router.push('/write')">发布文章</el-button>
      </div>

      <!-- 分类 -->
      <div class="glass rounded-xl p-4">
        <h3 class="font-semibold mb-3 dark:text-white">分类</h3>
        <div class="space-y-2">
          <div class="flex items-center justify-between p-2 rounded hover:bg-primary-50 dark:hover:bg-gray-700 cursor-pointer"
               :class="{ 'bg-primary-100 dark:bg-gray-700': selectedCategory === 'all' }"
               @click="selectCategory('all')">
            <span class="dark:text-gray-300">全部</span>
          </div>
          <div v-if="userStore.isLoggedIn"
               class="flex items-center justify-between p-2 rounded hover:bg-primary-50 dark:hover:bg-gray-700 cursor-pointer"
               :class="{ 'bg-primary-100 dark:bg-gray-700': selectedCategory === 'following' }"
               @click="selectCategory('following')">
            <span class="dark:text-gray-300">关注</span>
          </div>
          <div v-for="cat in categories" :key="cat.id"
               class="flex items-center justify-between p-2 rounded hover:bg-primary-50 dark:hover:bg-gray-700 cursor-pointer"
               :class="{ 'bg-primary-100 dark:bg-gray-700': selectedCategory === cat.id }"
               @click="selectCategory(cat.id)">
            <span class="dark:text-gray-300">{{ cat.name }}</span>
          </div>
        </div>
      </div>

      <!-- 热门标签 -->
      <div class="glass rounded-xl p-4">
        <h3 class="font-semibold mb-3 dark:text-white">热门标签</h3>
        <div class="flex flex-wrap gap-2">
          <el-tag v-for="tag in tags" :key="tag.id" class="cursor-pointer" effect="plain">{{ tag.name }}</el-tag>
        </div>
      </div>
      </div>
    </aside>

    <!-- 中间内容区 -->
    <div class="lg:col-span-2">
      <!-- 必应壁纸轮播图 -->
      <div class="glass rounded-xl mb-4 overflow-hidden">
        <div v-if="wallpaperLoading" class="h-[200px] flex items-center justify-center">
          <el-icon class="is-loading text-2xl"><Loading /></el-icon>
          <span class="ml-2 text-gray-500">正在加载壁纸...</span>
        </div>
        <el-carousel v-else-if="wallpapers.length" height="200px" :interval="5000">
          <el-carousel-item v-for="(wp, idx) in wallpapers" :key="idx">
            <img :src="wp.url" :alt="wp.title" class="w-full h-full object-cover" />
          </el-carousel-item>
        </el-carousel>
      </div>

      <!-- 文章列表 -->
      <div class="space-y-4">
        <div v-for="article in articles" :key="article.id" class="glass rounded-xl p-4 hover:shadow-lg transition-shadow cursor-pointer" @click="router.push(`/article/${article.id}`)">
          <div class="flex items-center gap-2 mb-2">
            <el-avatar :src="article.authorAvatar" :size="32">{{ article.authorName?.[0] || 'U' }}</el-avatar>
            <span class="text-sm font-medium dark:text-gray-300">{{ article.authorName || '匿名用户' }}</span>
            <span class="text-sm text-gray-400">{{ article.createdAt }}</span>
          </div>
          <h2 class="text-lg font-semibold mb-2 dark:text-white">{{ article.title }}</h2>
          <p class="text-gray-600 dark:text-gray-400 line-clamp-2 mb-3">{{ stripMd(article.content) }}</p>
          <div class="flex items-center gap-4 text-sm text-gray-500">
            <span><el-icon><View /></el-icon> {{ article.viewCount }}</span>
            <span :class="article.liked ? 'text-red-500' : 'text-gray-400'">{{ article.liked ? '❤' : '🤍' }} {{ article.likeCount || 0 }}</span>
          </div>
        </div>

        <!-- 加载更多 -->
        <div v-if="hasMore" ref="loadMoreRef" class="text-center py-4">
          <span v-if="loading" class="text-gray-500">加载中...</span>
        </div>
        <div v-else-if="articles.length > 0" class="text-center py-4 text-gray-500">没有更多了</div>
        <div v-else class="text-center py-8 text-gray-500">暂无文章</div>
      </div>
    </div>

    <!-- 右侧边栏 -->
    <aside class="hidden lg:block">
      <div ref="rightSidebarRef" class="sticky space-y-4" :style="{ top: rightSidebarTop }">
      <!-- 抖音热榜 -->
      <div v-if="douyinHot.length" class="glass rounded-xl p-4">
        <h3 class="font-semibold mb-3 dark:text-white flex items-center gap-2">
          <img src="https://www.douyin.com/favicon.ico" class="w-4 h-4" /> 抖音热榜
        </h3>
        <div class="space-y-2">
          <div v-for="(item, index) in douyinHot" :key="index" class="flex gap-2 text-sm">
            <span :class="index < 3 ? 'text-red-500 font-bold' : 'text-gray-400'">{{ index + 1 }}</span>
            <span class="line-clamp-1 dark:text-gray-300 cursor-pointer hover:text-primary-500" @click="searchDouyin(item.title)">{{ item.title }}</span>
          </div>
        </div>
      </div>

      <!-- 天气卡片 -->
      <div class="glass rounded-xl p-4">
        <h3 class="font-semibold mb-3 dark:text-white">天气</h3>
        <div v-if="weather">
          <div class="flex items-center justify-between mb-2">
            <span class="flex items-center gap-1 text-sm text-gray-500 dark:text-gray-400">
              <el-icon><Location /></el-icon> {{ weather.region }} {{ weather.city }}
            </span>
            <span class="text-sm text-gray-500 dark:text-gray-400">{{ weather.date }}</span>
          </div>
          <div class="flex items-center justify-between">
            <div class="text-3xl font-bold text-primary-500">{{ weather.temp }}°C</div>
            <div class="text-right text-sm text-gray-500 dark:text-gray-400">
              <div>{{ weather.desc }}</div>
              <div>体感 {{ weather.feelsLike }}°C</div>
              <div>湿度 {{ weather.humidity }}%</div>
            </div>
          </div>
        </div>
        <div v-else class="text-gray-400 text-sm">加载中...</div>
      </div>

      <!-- 每日一言 -->
      <div class="glass rounded-xl p-4">
        <h3 class="font-semibold mb-3 dark:text-white">每日一言</h3>
        <div v-if="hitokoto" class="text-sm">
          <p class="dark:text-gray-300 italic">「{{ hitokoto.hitokoto }}」</p>
          <p class="text-right text-gray-400 mt-2">—— {{ hitokoto.from }}</p>
        </div>
        <div v-else class="text-gray-400 text-sm">加载中...</div>
      </div>

      <!-- 热门文章 -->
      <div class="glass rounded-xl p-4">
        <h3 class="font-semibold mb-3 dark:text-white">热门文章</h3>
        <div class="space-y-3">
          <div v-for="(article, index) in hotArticles" :key="article.id"
               class="flex gap-2 cursor-pointer hover:text-primary-500"
               @click="router.push(`/article/${article.id}`)">
            <span class="text-primary-500 font-bold">{{ index + 1 }}</span>
            <span class="line-clamp-1 dark:text-gray-300">{{ article.title }}</span>
          </div>
        </div>
      </div>
      </div>
    </aside>
  </div>

  <!-- 返回顶部按钮 -->
  <el-button v-show="showBackTop" :icon="Top" circle class="!fixed !right-6 !bottom-6 !w-10 !h-10 z-50" @click="scrollToTop" title="返回顶部" />
</template>

<script setup>
import { ref, onMounted, onUnmounted, watch, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { getArticles, getCategories, getTags } from '@/api/blog'
import api from '@/api'
import { View, Loading, Location, Top } from '@element-plus/icons-vue'

const router = useRouter()
const userStore = useUserStore()

// 侧边栏 sticky 计算
const leftSidebarRef = ref(null)
const rightSidebarRef = ref(null)
const leftSidebarTop = ref('80px')
const rightSidebarTop = ref('80px')

const updateSidebarTop = () => {
  const viewportHeight = window.innerHeight
  const offset = 80 // 顶部导航栏高度
  const bottomPadding = 24

  if (leftSidebarRef.value) {
    const h = leftSidebarRef.value.offsetHeight
    leftSidebarTop.value = h > viewportHeight - offset - bottomPadding
      ? `${viewportHeight - h - bottomPadding}px`
      : `${offset}px`
  }
  if (rightSidebarRef.value) {
    const h = rightSidebarRef.value.offsetHeight
    rightSidebarTop.value = h > viewportHeight - offset - bottomPadding
      ? `${viewportHeight - h - bottomPadding}px`
      : `${offset}px`
  }
}

// 去除Markdown标记
const stripMd = (text) => {
  if (!text) return ''
  return text.replace(/```[\s\S]*?```/g, '').replace(/`[^`]*`/g, '').replace(/#{1,6}\s?/g, '').replace(/\*\*|__/g, '').replace(/\*|_/g, '').replace(/\[([^\]]*)\]\([^)]*\)/g, '$1').replace(/!\[.*?\]\(.*?\)/g, '').replace(/>\s?/g, '').replace(/-\s/g, '').replace(/\n+/g, ' ').trim().substring(0, 200)
}

const articles = ref([])
const categories = ref([])
const tags = ref([])
const hotArticles = ref([])
const wallpapers = ref([])
const wallpaperLoading = ref(false)
const douyinHot = ref([])
const weather = ref(null)
const hitokoto = ref(null)
const selectedCategory = ref('all')
const loading = ref(false)
const page = ref(1)
const hasMore = ref(true)
const loadMoreRef = ref(null)
const showBackTop = ref(false)
let observer = null

const setupObserver = () => {
  if (observer) observer.disconnect()
  observer = new IntersectionObserver((entries) => {
    if (entries[0].isIntersecting && hasMore.value && !loading.value) {
      loadMore()
    }
  }, { rootMargin: '100px' })
  if (loadMoreRef.value) observer.observe(loadMoreRef.value)
}

watch(loadMoreRef, (el) => {
  if (el && articles.value.length > 0) setupObserver()
})

const fetchArticles = async (reset = false) => {
  if (reset) {
    page.value = 1
    articles.value = []
  }
  loading.value = true
  try {
    let res
    if (selectedCategory.value === 'following') {
      res = await api.get('/articles/following', { params: { page: page.value, limit: 10 } })
    } else {
      const category = selectedCategory.value === 'all' ? null : selectedCategory.value
      res = await getArticles({ page: page.value, limit: 10, category })
    }
    if (res.success) {
      articles.value = reset ? res.data.data : [...articles.value, ...res.data.data]
      hasMore.value = articles.value.length < res.data.total
      if (loadMoreRef.value) setupObserver()
    }
  } finally {
    loading.value = false
  }
}

const fetchHotArticles = async () => {
  const res = await getArticles({ page: 1, limit: 5, sort: 'popular' })
  if (res.success) hotArticles.value = res.data.data
}

const selectCategory = (id) => {
  selectedCategory.value = id
  fetchArticles(true)
}

const loadMore = () => {
  page.value++
  fetchArticles()
}

const searchDouyin = (keyword) => {
  window.open(`https://www.douyin.com/search/${encodeURIComponent(keyword)}`, '_blank')
}

const handleRefresh = () => {
  fetchArticles(true)
  fetchHotArticles()
}

const scrollToTop = () => {
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

const handleScroll = () => {
  showBackTop.value = window.scrollY > 300
}

onMounted(async () => {
  window.addEventListener('scroll', handleScroll)
  window.addEventListener('resize', updateSidebarTop)
  setTimeout(updateSidebarTop, 500) // 等待内容加载后计算
  const [catRes, tagRes] = await Promise.all([getCategories(), getTags()])
  if (catRes.success) categories.value = catRes.data
  if (tagRes.success) tags.value = tagRes.data
  fetchArticles()
  fetchHotArticles()
  // 获取必应壁纸
  wallpaperLoading.value = true
  api.get('/wallpaper/bing').then(res => {
    if (res.success) wallpapers.value = res.data
  }).catch(() => {}).finally(() => {
    wallpaperLoading.value = false
  })
  // 获取抖音热榜
  api.get('/wallpaper/douyin-hot').then(res => {
    if (res.success) douyinHot.value = res.data
  }).catch(() => {})
  // 获取天气
  api.get('/wallpaper/weather').then(res => {
    if (res.success) weather.value = res.data
  }).catch(() => {})
  // 获取一言
  fetch('https://v1.hitokoto.cn/?c=i&c=k').then(r => r.json()).then(data => {
    hitokoto.value = data
  }).catch(() => {})
})

onUnmounted(() => {
  if (observer) observer.disconnect()
  window.removeEventListener('scroll', handleScroll)
  window.removeEventListener('resize', updateSidebarTop)
})
</script>
