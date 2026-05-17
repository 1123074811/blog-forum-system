<template>
  <div class="home-container">
    <!-- 左侧边栏 -->
    <aside class="left-sidebar hidden lg:block">
      <!-- 用户卡片 -->
      <div v-if="userStore.isLoggedIn" class="glass-card p-4 mb-4 card-enter">
        <div class="flex items-center gap-3 mb-4">
          <el-avatar :src="toAvatarThumb(userStore.user?.avatar, 96)" :size="48" class="ring-2 ring-primary-200 dark:ring-primary-800">{{ (userStore.user?.nickname || userStore.user?.username)?.[0] }}</el-avatar>
          <div>
            <div class="font-semibold dark:text-white">{{ userStore.user?.nickname || userStore.user?.username }}</div>
            <div class="text-sm text-gray-500">{{ userStore.user?.bio || '暂无简介' }}</div>
          </div>
        </div>
        <el-button type="primary" class="w-full" @click="router.push('/write')">
          <el-icon class="mr-1"><Edit /></el-icon>发布文章
        </el-button>
      </div>

      <!-- 分类 -->
      <div class="glass-card p-4 mb-4 card-enter" style="animation-delay: 0.1s">
        <h3 class="font-semibold mb-3 dark:text-white flex items-center gap-2">
          <span class="w-1 h-4 bg-gradient-to-b from-primary-400 to-primary-600 rounded-full"></span>
          分类
        </h3>
        <div class="space-y-2">
          <div class="flex items-center justify-between p-2 rounded-lg hover:bg-primary-50 dark:hover:bg-gray-700 cursor-pointer transition-all duration-300"
               :class="{ 'bg-gradient-to-r from-primary-200 to-primary-50 dark:from-gray-700 dark:to-gray-800 shadow-sm': selectedCategory === 'all' }"
               @click="selectCategory('all')">
            <span class="dark:text-gray-300 font-medium">全部</span>
          </div>
          <!-- 只在登录后显示关注分类 -->
          <div v-if="userStore.isLoggedIn"
               class="flex items-center justify-between p-2 rounded-lg hover:bg-primary-50 dark:hover:bg-gray-700 cursor-pointer transition-all duration-300"
               :class="{ 'bg-gradient-to-r from-primary-200 to-primary-50 dark:from-gray-700 dark:to-gray-800 shadow-sm': selectedCategory === 'following' }"
               @click="selectCategory('following')">
            <span class="dark:text-gray-300 font-medium flex items-center gap-1">
              <el-icon><Star /></el-icon>
              关注
            </span>
          </div>
          <div v-for="cat in categories" :key="cat.id"
               class="flex items-center justify-between p-2 rounded-lg hover:bg-primary-50 dark:hover:bg-gray-700 cursor-pointer transition-all duration-300"
               :class="{ 'bg-gradient-to-r from-primary-200 to-primary-50 dark:from-gray-700 dark:to-gray-800 shadow-sm': selectedCategory === cat.id }"
               @click="selectCategory(cat.id)">
            <span class="dark:text-gray-300 font-medium">{{ cat.name }}</span>
          </div>
        </div>
      </div>

      <!-- 热门标签 -->
      <div class="glass-card p-4 card-enter" style="animation-delay: 0.2s">
        <h3 class="font-semibold mb-3 dark:text-white flex items-center gap-2">
          <span class="w-1 h-4 bg-gradient-to-b from-primary-400 to-primary-600 rounded-full"></span>
          热门标签
        </h3>
        <div class="flex flex-wrap gap-2">
          <el-tag v-for="tag in tags" :key="tag.id" class="cursor-pointer" effect="plain">
            {{ tag.name }}
          </el-tag>
        </div>
      </div>
    </aside>

    <!-- 中间内容区 -->
    <div class="main-content" ref="mainContentRef">
      <!-- 必应壁纸轮播图 -->
      <div v-if="shouldShowWallpaper" class="glass-card mb-4 overflow-hidden card-enter">
        <div v-if="wallpaperLoading" class="h-[200px] flex items-center justify-center">
          <el-icon class="is-loading text-2xl text-primary-500"><Loading /></el-icon>
          <span class="ml-2 text-gray-500">正在加载壁纸...</span>
        </div>
        <el-carousel v-else-if="wallpapers.length" height="200px" :interval="5000" indicator-position="none">
          <el-carousel-item v-for="(wp, idx) in wallpapers" :key="idx">
            <div class="relative w-full h-full group">
              <img
                :src="normalizeUnsafeUrl(wp.url)"
                :alt="wp.title || 'Bing wallpaper'"
                :loading="idx === 0 ? 'eager' : 'lazy'"
                :fetchpriority="idx === 0 ? 'high' : 'low'"
                decoding="async"
                class="w-full h-full object-cover transition-transform duration-700 group-hover:scale-110"
              />
              <div class="absolute inset-0 bg-gradient-to-t from-black/50 to-transparent opacity-0 group-hover:opacity-100 transition-opacity duration-300"></div>
              <div class="absolute bottom-0 left-0 right-0 p-4 text-white transform translate-y-full group-hover:translate-y-0 transition-transform duration-300">
                <p class="text-sm font-medium">{{ wp.title }}</p>
              </div>
            </div>
          </el-carousel-item>
        </el-carousel>
      </div>

      <!-- 文章列表 -->
      <div class="space-y-4">
        <div v-for="(article, index) in articles" :key="article.id"
             class="glass-card p-3 sm:p-5 cursor-pointer card-enter group"
             :style="`animation-delay: ${index * 0.05}s`"
             @click="router.push(`/article/${article.id}`)">
          <div class="flex items-center gap-2 mb-3">
            <el-avatar :src="toAvatarThumb(article.authorAvatar, 72)" :size="36" class="ring-2 ring-primary-100 dark:ring-primary-900">
              {{ article.authorName?.[0] || 'U' }}
            </el-avatar>
            <div class="flex-1">
              <span class="text-sm font-medium dark:text-gray-300">{{ article.authorName || '匿名用户' }}</span>
              <span class="text-sm text-gray-400 ml-2">{{ article.createdAt }}</span>
            </div>
          </div>
          <h2 class="text-lg font-bold mb-2 dark:text-white group-hover:text-primary-500 transition-colors duration-300">
            {{ article.title }}
          </h2>
          <p class="text-gray-600 dark:text-gray-400 line-clamp-2 mb-4 leading-relaxed">
            {{ stripMd(article.content) }}
          </p>
          <div class="flex items-center gap-6 text-sm text-gray-500">
            <span class="flex items-center gap-1 hover:text-primary-500 transition-colors">
              <el-icon><View /></el-icon> {{ article.viewCount }}
            </span>
            <span class="flex items-center gap-1 transition-colors"
                  :class="article.liked ? 'text-red-500' : 'hover:text-red-400'">
              {{ article.liked ? '❤️' : '🤍' }} {{ article.likeCount || 0 }}
            </span>
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
    <aside class="right-sidebar hidden lg:block">
      <!-- 抖音热榜 -->
      <div v-if="douyinHot.length" class="glass-card p-4 mb-4 card-enter">
        <h3 class="font-semibold mb-3 dark:text-white flex items-center gap-2">
          <img src="https://www.douyin.com/favicon.ico" alt="" loading="lazy" decoding="async" aria-hidden="true" class="w-5 h-5" />
          <span class="text-red-500">抖音热榜</span>
        </h3>
        <div class="space-y-1">
          <div v-for="(item, index) in douyinHot" :key="index"
               class="flex gap-1 text-sm p-2 rounded-lg hover:bg-primary-50 dark:hover:bg-gray-700 transition-all duration-300 group">
            <span class="font-bold w-6 text-center flex-shrink-0"
                  :class="index < 3 ? 'text-red-500' : 'text-gray-400'">
              {{ index + 1 }}
            </span>
            <span class="line-clamp-1 dark:text-gray-300 cursor-pointer group-hover:text-primary-500 transition-colors"
                  @click="searchDouyin(item.title)">
              {{ item.title }}
            </span>
          </div>
        </div>
      </div>

      <!-- 天气卡片 -->
      <div class="glass-card p-4 mb-4 card-enter" style="animation-delay: 0.1s">
        <h3 class="font-semibold mb-3 dark:text-white flex items-center gap-2">
          <span class="text-xl">🌤️</span>
          天气
        </h3>
        <div v-if="weather">
          <div class="flex items-center justify-between mb-3">
            <span class="flex items-center gap-1 text-sm text-gray-500 dark:text-gray-400">
              <el-icon><Location /></el-icon> {{ weather.region }} {{ weather.city }}
            </span>
            <span class="text-sm text-gray-500 dark:text-gray-400">{{ weather.date }}</span>
          </div>
          <div class="flex items-center justify-between">
            <div class="text-4xl font-bold gradient-text">{{ weather.temp }}°C</div>
            <div class="text-right text-sm text-gray-500 dark:text-gray-400 space-y-1">
              <div class="font-medium">{{ weather.desc }}</div>
              <div>体感 {{ weather.feelsLike }}°C</div>
              <div>湿度 {{ weather.humidity }}%</div>
            </div>
          </div>
        </div>
        <div v-else class="text-gray-400 text-sm flex items-center gap-2">
          <el-icon class="is-loading"><Loading /></el-icon>
          加载中...
        </div>
      </div>

      <!-- 每日一言 -->
      <div class="glass-card p-4 mb-4 card-enter" style="animation-delay: 0.2s">
        <h3 class="font-semibold mb-3 dark:text-white flex items-center gap-2">
          <span class="text-xl">💭</span>
          每日一言
        </h3>
        <div v-if="hitokoto" class="text-sm">
          <p class="dark:text-gray-300 italic leading-relaxed mb-3 text-base">
            「{{ hitokoto.hitokoto }}」
          </p>
          <p class="text-right text-gray-400 text-xs">—— {{ hitokoto.from }}</p>
        </div>
        <div v-else class="text-gray-400 text-sm flex items-center gap-2">
          <el-icon class="is-loading"><Loading /></el-icon>
          加载中...
        </div>
      </div>

      <!-- 热门文章 -->
      <div class="glass-card p-4 card-enter" style="animation-delay: 0.3s">
        <h3 class="font-semibold mb-3 dark:text-white flex items-center gap-2">
          <span class="text-xl">🔥</span>
          热门文章
        </h3>
        <div class="space-y-3">
          <div v-for="(article, index) in hotArticles" :key="article.id"
               class="flex gap-2 cursor-pointer p-2 rounded-lg hover:bg-primary-50 dark:hover:bg-gray-700 transition-all duration-300 group"
               @click="router.push(`/article/${article.id}`)">
            <span class="font-bold w-6 text-center flex-shrink-0" :style="getHotRankStyle(index)">
              {{ index + 1 }}
            </span>
            <span class="line-clamp-1 dark:text-gray-300 group-hover:text-primary-500 transition-colors">
              {{ article.title }}
            </span>
          </div>
        </div>
      </div>
    </aside>
  </div>

  <!-- 返回顶部按钮 -->
  <el-button v-show="showBackTop" :icon="Top" circle class="!fixed !right-4 !bottom-4 sm:!right-6 sm:!bottom-6 !w-10 !h-10 z-50" @click="scrollToTop" title="返回顶部" />

  <!-- 公告弹窗 -->
  <el-dialog
    v-model="showAnnouncement"
    :title="currentAnnouncement?.title"
    :width="isMobile ? '90%' : '600px'"
    :top="isMobile ? '15vh' : '15vh'"
    destroy-on-close
    class="announcement-dialog rounded-xl overflow-hidden bg-white dark:bg-gray-800"
    :class="{ 'mobile-dialog': isMobile }"
  >
    <div class="announcement-content py-3 sm:py-6">
      <div class="whitespace-pre-wrap text-sm sm:text-base leading-relaxed text-gray-700 dark:text-gray-300">{{ currentAnnouncement?.content }}</div>
    </div>
    <template #footer>
      <div class="flex justify-end items-center gap-3 sm:gap-4">
        <el-checkbox v-model="dontShowToday" label="今日不再提示" />
        <el-button type="primary" size="small" @click="closeAnnouncement">
          我已知晓
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, onMounted, onUnmounted, watch, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { getHomeData, getArticles } from '@/api/blog'
import api from '@/api'
import { View, Loading, Location, Top, Edit, Star } from '@element-plus/icons-vue'
import { normalizeUnsafeUrl, toAvatarThumb } from '@/utils/image'

const router = useRouter()
const userStore = useUserStore()

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
const mainContentRef = ref(null)
const showBackTop = ref(false)
const showAnnouncement = ref(false)
const currentAnnouncement = ref(null)
const dontShowToday = ref(false)
const isMobile = ref(window.innerWidth < 768)
let observer = null
const ENTRY_SOURCE_KEY = 'site_entry_source_v1'
const HOME_ANNOUNCEMENT_CONSUMED_KEY = 'home_announcement_consumed_v1'
const isDesktop = window.innerWidth >= 1024
const connection = typeof navigator !== 'undefined'
  ? (navigator.connection || navigator.mozConnection || navigator.webkitConnection)
  : null
const effectiveType = String(connection?.effectiveType || '').toLowerCase()
const isDataSaver = Boolean(connection?.saveData || effectiveType.includes('2g'))
const shouldShowWallpaper = isDesktop && !isDataSaver

const setupObserver = () => {
  if (observer) observer.disconnect()
  observer = new IntersectionObserver((entries) => {
    if (entries[0].isIntersecting && hasMore.value && !loading.value) {
      loadMore()
    }
  }, { root: mainContentRef.value, rootMargin: '100px' })
  if (loadMoreRef.value) observer.observe(loadMoreRef.value)
}

watch(loadMoreRef, (el) => {
  if (el && articles.value.length > 0) setupObserver()
})

const runWhenIdle = (task, timeout = 2000) => {
  if ('requestIdleCallback' in window) {
    window.requestIdleCallback(() => task(), { timeout })
  } else {
    setTimeout(task, 400)
  }
}

const fetchSidebarData = async () => {
  try {
    const res = await getHomeData({ page: 1, limit: 1 })
    if (res.success && res.data) {
      const data = res.data
      if (data.categories) categories.value = data.categories
      if (data.tags) tags.value = data.tags
      if (data.hotArticles) hotArticles.value = data.hotArticles
      if (data.announcements && data.announcements.length > 0) {
        const announcement = data.announcements[0]
        const today = new Date().toISOString().split('T')[0]
        const userId = userStore.isLoggedIn ? userStore.user?.id : 'guest'
        const storageKey = `hide_announcement_${userId}_${announcement.id}_${today}`
        const entrySource = sessionStorage.getItem(ENTRY_SOURCE_KEY) || 'external'
        const consumed = sessionStorage.getItem(HOME_ANNOUNCEMENT_CONSUMED_KEY) === 'true'
        if (entrySource === 'external' && !consumed && !localStorage.getItem(storageKey)) {
          currentAnnouncement.value = announcement
          showAnnouncement.value = true
        }
      }
      sessionStorage.setItem(HOME_ANNOUNCEMENT_CONSUMED_KEY, 'true')
    }
  } catch (error) {
    console.error('Failed to load sidebar data:', error)
  }
}

const fetchWallpapers = () => {
  if (!shouldShowWallpaper) return
  wallpaperLoading.value = true
  api.get('/wallpaper/bing').then(res => {
    if (res.success) wallpapers.value = res.data
  }).catch(() => {}).finally(() => {
    wallpaperLoading.value = false
  })
}

const fetchRightSidebarFeeds = () => {
  api.get('/wallpaper/douyin-hot').then(res => {
    if (res.success) douyinHot.value = res.data
  }).catch(() => {})

  api.get('/wallpaper/weather').then(res => {
    if (res.success) weather.value = res.data
  }).catch(() => {})

  fetch('https://v1.hitokoto.cn/?c=i&c=k')
    .then(r => r.json())
    .then(data => {
      hitokoto.value = data
    }).catch(() => {})
}

const fetchArticles = async (reset = false) => {
  if (reset) {
    page.value = 1
    articles.value = []
  }
  loading.value = true
  try {
    let res
    const pageSize = page.value === 1 ? 6 : 10
    if (selectedCategory.value === 'following') {
      // 未登录时不请求关注的文章
      if (!userStore.isLoggedIn) {
        articles.value = []
        hasMore.value = false
        return
      }
      res = await api.get('/articles/following', { params: { page: page.value, limit: pageSize } })
    } else {
      const category = selectedCategory.value === 'all' ? null : selectedCategory.value
      res = await getArticles({ page: page.value, limit: pageSize, category })
    }
    if (res.success) {
      articles.value = reset ? res.data.data : [...articles.value, ...res.data.data]
      hasMore.value = articles.value.length < res.data.total
      if (loadMoreRef.value) setupObserver()
    }
  } catch (error) {
    // 静默处理错误，避免影响页面加载
    console.error('获取文章失败:', error)
  } finally {
    loading.value = false
  }
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

// 热门文章序号红色渐变样式
const getHotRankStyle = (index) => {
  const colors = ['#FF4500', '#FF6347', '#FF7F50', '#FFA07A', '#FFB6C1']
  return { color: colors[index] || '#999' }
}

const handleRefresh = () => {
  fetchArticles(true)
  fetchSidebarData()
}

const scrollToTop = () => {
  if (mainContentRef.value) {
    mainContentRef.value.scrollTo({ top: 0, behavior: 'smooth' })
  }
}

const handleScroll = () => {
  showBackTop.value = (mainContentRef.value?.scrollTop || 0) > 300
}

const closeAnnouncement = () => {
  if (dontShowToday.value && currentAnnouncement.value) {
    const today = new Date().toISOString().split('T')[0]
    const userId = userStore.isLoggedIn ? userStore.user?.id : 'guest'
    localStorage.setItem(`hide_announcement_${userId}_${currentAnnouncement.value.id}_${today}`, 'true')
  }
  showAnnouncement.value = false
}

onMounted(async () => {
  if (window.innerWidth >= 1024) {
    document.body.style.overflow = 'hidden'
  }

  await nextTick()
  if (mainContentRef.value) {
    mainContentRef.value.addEventListener('scroll', handleScroll)
  }

  // Critical path first: feed content.
  fetchArticles(true)

  // Aggregated sidebar data (categories, tags, hot articles, announcements) in one request.
  if (isDesktop) {
    runWhenIdle(() => fetchSidebarData(), 2000)
    runWhenIdle(() => fetchWallpapers(), 2400)
    runWhenIdle(() => fetchRightSidebarFeeds(), 2600)
  } else {
    runWhenIdle(() => fetchSidebarData(), 2200)
  }
})

onUnmounted(() => {
  // 离开首页时恢复 body 滚动
  if (window.innerWidth >= 1024) {
    document.body.style.overflow = ''
  }
  if (observer) observer.disconnect()
  if (mainContentRef.value) {
    mainContentRef.value.removeEventListener('scroll', handleScroll)
  }
})
</script>

<style scoped>
.home-container {
  display: flex;
  gap: 1.5rem;
  height: 100%;
  overflow: hidden;
}

@media (max-width: 1023px) {
  .home-container {
    height: auto;
    overflow: visible;
  }
}

.left-sidebar {
  width: 280px;
  flex-shrink: 0;
  height: 100%;
  overflow-y: auto;
  padding: 1rem 8px 1rem 0;
}

.main-content {
  flex: 1;
  height: 100%;
  overflow-y: auto;
  min-width: 0;
  padding: 1rem 0;
}

@media (max-width: 1023px) {
  .main-content {
    height: auto;
    overflow-y: visible;
  }
}

.right-sidebar {
  width: 300px;
  flex-shrink: 0;
  height: 100%;
  overflow-y: auto;
  padding: 1rem 0 1rem 8px;
}

/* 公告内容区域样式 */
.announcement-content {
  max-height: 500px;
  overflow-y: auto;
  scrollbar-width: none;
  -ms-overflow-style: none;
}

@media (max-width: 768px) {
  .announcement-content {
    max-height: none;
  }
}

.announcement-content::-webkit-scrollbar {
  display: none;
}

/* 隐藏滚动条 */
.left-sidebar,
.main-content,
.right-sidebar {
  scrollbar-width: none;
  -ms-overflow-style: none;
}

.left-sidebar::-webkit-scrollbar,
.main-content::-webkit-scrollbar,
.right-sidebar::-webkit-scrollbar {
  display: none;
}
</style>
