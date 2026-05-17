<template>
  <div class="home-container">
    <!-- 左侧边栏 -->
    <aside class="left-sidebar hidden lg:block">
      <!-- 用户卡片 -->
      <div v-if="userStore.isLoggedIn" class="sidebar-widget p-4 mb-4 card-enter">
        <div class="flex items-center gap-3 mb-4">
          <el-avatar :src="toAvatarThumb(userStore.user?.avatar, 96)" :size="48" class="ring-2 ring-ink dark:ring-gray-700">{{ (userStore.user?.nickname || userStore.user?.username)?.[0] }}</el-avatar>
          <div>
            <div class="font-semibold dark:text-white">{{ userStore.user?.nickname || userStore.user?.username }}</div>
            <div class="text-sm text-gray-500">{{ userStore.user?.bio || '暂无简介' }}</div>
          </div>
        </div>
        <el-button type="primary" class="w-full !bg-ink !text-paper hover:!bg-accent !border-none !rounded-none !shadow-[4px_4px_0px_#d35400] transition-all" @click="router.push('/write')">
          <el-icon class="mr-1"><Edit /></el-icon>发布文章
        </el-button>
      </div>

      <!-- 分类 -->
      <div class="sidebar-widget p-4 mb-4 card-enter" style="animation-delay: 0.1s">
        <h3 class="widget-title dark:text-white flex items-center gap-2">
          分类
        </h3>
        <div class="space-y-2 font-serif">
          <div class="flex items-center justify-between p-2 rounded-lg hover:bg-primary-50 dark:hover:bg-gray-700 cursor-pointer transition-all duration-300"
               :class="{ 'bg-gradient-to-r from-primary-200 to-primary-50 dark:from-gray-700 dark:to-gray-800 shadow-sm': selectedCategory === 'all' }"
               @click="selectCategory('all')">
            <span class="dark:text-gray-300 font-medium">全部</span>
          </div>
          <!-- 仅登录后显示关注分类 -->
          <div v-if="userStore.isLoggedIn"
               class="flex items-center justify-between p-2 rounded-lg hover:bg-primary-50 dark:hover:bg-gray-700 cursor-pointer transition-all duration-300"
               :class="{ 'bg-gradient-to-r from-primary-200 to-primary-50 dark:from-gray-700 dark:to-gray-800 shadow-sm': selectedCategory === 'following' }"
               @click="selectCategory('following')">
            <span class="dark:text-gray-300 font-medium flex items-center gap-1">
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
      <div class="sidebar-widget p-4 card-enter" style="animation-delay: 0.2s">
        <h3 class="widget-title dark:text-white flex items-center gap-2">
          热门标签
        </h3>
        <div class="flex flex-wrap gap-2">
          <span v-for="tag in tags" :key="tag.id" class="cursor-pointer px-3 py-1 border border-ink text-sm hover:bg-ink hover:text-paper transition-colors">
            {{ tag.name }}
          </span>
        </div>
      </div>
    </aside>

    <!-- 中间内容区（幽灵滚动层） -->
    <div class="home-main-col" ref="mainContentRef">
      <div class="home-main-col-inner">
        <!-- 文章列表 -->
        <div class="space-y-12">
        <div v-for="(article, index) in articles" :key="article.id"
             class="post-card article-post-card cursor-pointer group"
             :style="`animation-delay: ${index * 0.05}s`"
             @click="router.push(`/article/${article.id}`)">
          <div class="flex items-center gap-2 mb-4 text-accent text-sm font-medium">
            <span class="stamp">记</span>
            <span>{{ article.createdAt }}</span>
            <span class="ml-auto text-gray-500 flex items-center gap-2">
               <el-icon><View /></el-icon> {{ article.viewCount }}
            </span>
          </div>
          <h2 class="text-2xl font-bold mb-4 dark:text-white group-hover:text-accent transition-colors duration-300 font-serif">
            {{ article.title }}
          </h2>
          
          <!-- 文章封面图（有封面则显示） -->
          <div v-if="article.cover" class="w-full h-48 bg-subtleBlue mb-4 overflow-hidden rounded-md border-2 border-[#333]">
            <img :src="normalizeUnsafeUrl(article.cover)" class="w-full h-full object-cover filter grayscale-[30%] group-hover:grayscale-0 transition-all duration-500" />
          </div>

          <p class="text-gray-600 dark:text-gray-400 line-clamp-3 mb-6 leading-loose font-serif">
            {{ stripMd(article.content) }}
          </p>
          <div class="text-accent font-bold text-sm">
            [ 继续阅读 -> ]
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
  </div>

    <!-- 右侧边栏 -->
    <aside class="right-sidebar hidden lg:block">
      <!-- 抖音热榜 -->
      <div v-if="douyinHot.length" class="sidebar-widget p-4 mb-4 card-enter">
        <h3 class="widget-title dark:text-white flex items-center gap-2">
          <span class="text-red-500">抖音热榜</span>
        </h3>
        <div class="space-y-2">
          <div v-for="(item, index) in douyinHot" :key="index"
               class="flex gap-2 text-sm p-1 hover:text-accent transition-all duration-300 group">
            <span class="font-bold w-6 text-center flex-shrink-0"
                  :class="index < 3 ? 'text-red-500' : 'text-gray-400'">
              {{ index + 1 }}
            </span>
            <span class="line-clamp-1 dark:text-gray-300 cursor-pointer"
                  @click="searchDouyin(item.title)">
              {{ item.title }}
            </span>
          </div>
        </div>
      </div>

      <!-- 天气卡片 -->
      <div class="sidebar-widget p-4 mb-4 card-enter" style="animation-delay: 0.1s">
        <h3 class="widget-title dark:text-white flex items-center gap-2">
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
            <div class="text-4xl font-bold font-serif">{{ weather.temp }}°C</div>
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
      <div class="sidebar-widget p-4 mb-4 card-enter" style="animation-delay: 0.2s">
        <h3 class="widget-title dark:text-white flex items-center gap-2">
          每日一言
        </h3>
        <div v-if="hitokoto" class="text-sm font-serif">
          <p class="dark:text-gray-300 italic leading-loose mb-3 text-base">
            {{ hitokoto.hitokoto }}
          </p>
          <p class="text-right text-gray-400 text-xs">—— {{ hitokoto.from }}</p>
        </div>
        <div v-else class="text-gray-400 text-sm flex items-center gap-2">
          <el-icon class="is-loading"><Loading /></el-icon>
          加载中...
        </div>
      </div>

      <!-- 热门文章 -->
      <div class="sidebar-widget p-4 card-enter" style="animation-delay: 0.3s">
        <h3 class="widget-title dark:text-white flex items-center gap-2">
          热门文章
        </h3>
        <div class="space-y-3 font-serif">
          <div v-for="(article, index) in hotArticles" :key="article.id"
               class="flex gap-2 cursor-pointer p-1 hover:text-accent transition-all duration-300 group"
               @click="router.push(`/article/${article.id}`)">
            <span class="font-bold w-6 text-center flex-shrink-0" :style="getHotRankStyle(index)">
              {{ index + 1 }}
            </span>
            <span class="line-clamp-1 dark:text-gray-300">
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
          我知道了
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

// 去除 Markdown 标记
const stripMd = (text) => {
  if (!text) return ''
  return text.replace(/```[\s\S]*?```/g, '').replace(/`[^`]*`/g, '').replace(/#{1,6}\s?/g, '').replace(/\*\*|__/g, '').replace(/\*|_/g, '').replace(/\[([^\]]*)\]\([^)]*\)/g, '$1').replace(/!\[.*?\]\(.*?\)/g, '').replace(/>\s?/g, '').replace(/-\s/g, '').replace(/\n+/g, ' ').trim().substring(0, 200)
}

const articles = ref([])
const categories = ref([])
const tags = ref([])
const hotArticles = ref([])
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
let scrollContainer = null
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
      // 未登录时不请求关注流文章
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
    // 静默处理，避免影响页面首次加载
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

// 热门文章序号颜色样式
const getHotRankStyle = (index) => {
  const colors = ['#FF4500', '#FF6347', '#FF7F50', '#FFA07A', '#FFB6C1']
  return { color: colors[index] || '#999' }
}

const handleRefresh = () => {
  fetchArticles(true)
  fetchSidebarData()
}

const scrollToTop = () => {
  if (isDesktop && mainContentRef.value) {
    mainContentRef.value.scrollTo({ top: 0, behavior: 'smooth' })
    return
  }
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

const handleScroll = () => {
  const top = isDesktop && mainContentRef.value
    ? mainContentRef.value.scrollTop
    : (window.scrollY || document.documentElement.scrollTop)
  showBackTop.value = top > 300
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
  await nextTick()
  scrollContainer = isDesktop && mainContentRef.value ? mainContentRef.value : window
  scrollContainer.addEventListener('scroll', handleScroll, { passive: true })
  handleScroll()

  // Critical path first: feed content.
  fetchArticles(true)

  // Aggregated sidebar data (categories, tags, hot articles, announcements) in one request.
  if (isDesktop) {
    runWhenIdle(() => fetchSidebarData(), 2000)
    runWhenIdle(() => fetchRightSidebarFeeds(), 2400)
  } else {
    runWhenIdle(() => fetchSidebarData(), 2200)
  }
})

onUnmounted(() => {
  if (observer) observer.disconnect()
  if (scrollContainer) scrollContainer.removeEventListener('scroll', handleScroll)
})
</script>

<style scoped>
.home-container {
  display: flex !important;
  justify-content: center; /* 整体居中 */
  gap: 1.5rem; /* 稍微缩窄间隙 */
  height: calc(100vh - 84px) !important;
  overflow: hidden !important;
  width: 100% !important;
  background: transparent;
}

@media (max-width: 1023px) {
  .home-container {
    flex-direction: column;
    height: auto !important;
    overflow: visible;
    padding-bottom: 60px;
  }
}

.left-sidebar {
  width: 240px;
  flex-shrink: 0;
  height: 100% !important;
  overflow-y: auto !important;
  scrollbar-width: none;
  -ms-overflow-style: none;
  z-index: 5;
  background: transparent;
  position: relative;
}

.left-sidebar::-webkit-scrollbar { display: none; }

.home-main-col {
  flex: 1;
  max-width: 820px; /* 限制容器总宽 */
  height: 100% !important;
  overflow-y: auto !important;
  z-index: 10;
  scrollbar-width: none; /* 隐藏 Firefox 滚动条 */
  -ms-overflow-style: none; /* 隐藏 IE/Edge 滚动条 */
  /* 核心：通过大 Padding 给内部放大腾出位置，而不必使用负 Margin */
  padding: 24px 40px; 
}

.home-main-col::-webkit-scrollbar { display: none; } /* 隐藏 Chrome/Safari 滚动条 */

.home-main-col-inner {
  max-width: 700px; /* 缩小文章宽度 */
  margin: 0 auto;
}

/* 适配中等屏幕，进一步缩窄侧边栏确保中间够宽 */
@media (min-width: 1024px) and (max-width: 1366px) {
  .left-sidebar { width: 200px; }
  .right-sidebar { width: 240px; }
  .home-container { gap: 1.5rem; }
}

.right-sidebar {
  width: 280px;
  flex-shrink: 0;
  height: 100% !important;
  overflow-y: auto !important;
  scrollbar-width: none;
  -ms-overflow-style: none;
  z-index: 5;
  background: transparent;
  position: relative;
}

.right-sidebar::-webkit-scrollbar { display: none; }

@media (max-width: 1023px) {
  .left-sidebar,
  .main-content,
  .right-sidebar {
    height: auto;
    overflow: visible;
  }
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
.home-main-col::-webkit-scrollbar,
.right-sidebar::-webkit-scrollbar {
  display: none;
}

/* 首页文章卡片悬浮时提升到最上层，避免边框/阴影被遮挡 */
.article-post-card {
  position: relative;
  z-index: 5;
}

.article-post-card:hover {
  z-index: 100 !important; /* 极高层级确保不被侧边栏或其他卡片遮挡 */
}

.space-y-12 {
  position: relative;
  /* 移除 isolation: isolate，允许子元素飞出容器层级 */
}
</style>

