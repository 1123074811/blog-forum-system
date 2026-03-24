<template>
  <div class="jp-community-page w-full px-2 sm:px-6 pt-8 pb-12 space-y-6">
    <h1 class="jp-page-title text-2xl sm:text-3xl font-bold dark:text-white mb-6">
      <span class="stamp">览</span>览影廊
    </h1>

    <el-tabs v-model="activeTab" class="jp-tabs custom-card-tabs" type="card" @tab-change="handleTabChange">
      <!-- 公开图片 -->
      <el-tab-pane label="公开图片" name="images">
        <WaterfallLayout v-if="mediaList.length" ref="waterfallImgRef" :items="mediaList" :cols="colCount" :gap="16" class="py-2 px-1">
          <template #default="{ item }">
            <div class="waterfall-item cursor-pointer group" @click="previewMedia(item)">
              <div class="flip-inner border-2 border-[var(--ink)] bg-gray-50 dark:bg-gray-800 rounded-xl">
                <div class="flip-front relative min-h-[120px]">
                  <el-image :src="normalizeUnsafeUrl(item.thumbnailUrl || item.url)" class="w-full h-auto block rounded-[10px]">
                    <template #error>
                      <div class="w-full aspect-[4/5] flex flex-col items-center justify-center bg-gray-100 dark:bg-gray-800 text-gray-400">
                        <el-icon :size="24"><Picture /></el-icon>
                        <span class="text-[10px] mt-1">地址无效</span>
                      </div>
                    </template>
                    <template #placeholder>
                      <div class="w-full aspect-[4/5] flex items-center justify-center bg-gray-50 dark:bg-gray-800" v-loading="true"></div>
                    </template>
                  </el-image>
                </div>
                <div class="flip-back p-4 flex flex-col text-[var(--ink)] dark:text-gray-200">
                  <div class="flex-1 flex flex-col min-h-0 border-b border-dashed border-gray-300 dark:border-gray-600 pb-2 mb-2">
                    <h3 class="font-bold text-base mb-1 whitespace-normal break-words line-clamp-2" :title="item.title">{{ item.title || '记录瞬间' }}</h3>
                    <div class="text-xs text-gray-500 dark:text-gray-400 whitespace-normal break-words line-clamp-3 overflow-hidden flex-1">{{ item.description || '暂无详细描述...' }}</div>
                  </div>
                  <div class="mt-auto">
                    <div class="flex items-center gap-2">
                      <el-avatar v-if="item.source === 'bing'" :size="24" src="https://www.bing.com/favicon.ico" class="cursor-pointer" @click.stop="openSource(item)">B</el-avatar>
                      <el-avatar v-else :src="toAvatarThumb(item.avatar, 36)" :size="24">{{ item.nickname?.charAt(0) || '?' }}</el-avatar>
                      <div class="flex flex-col text-[11px] text-gray-600 dark:text-gray-400 truncate flex-1 leading-tight">
                        <span class="font-bold truncate" :class="{ 'cursor-pointer hover:text-blue-500': item.source === 'bing' }" @click.stop="openSource(item)">
                          {{ item.source === 'bing' ? '必应壁纸' : (item.nickname || '作者') }}
                        </span>
                        <span>{{ item.createdAt?.split(' ')[0] }}</span>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </template>
        </WaterfallLayout>
        <div class="h-20 flex items-center justify-center">
          <el-icon v-if="hasMoreMedia && isLoading" class="is-loading"><Loading /></el-icon>
          <span v-else-if="mediaList.length && !hasMoreMedia" class="text-gray-400 text-sm">暂无更多图片</span>
        </div>
      </el-tab-pane>

      <!-- 公开视频 -->
      <el-tab-pane label="热门视频" name="videos">
        <WaterfallLayout v-if="mediaList.length" ref="waterfallVidRef" :items="mediaList" :cols="colCount" :gap="16" class="py-2 px-1">
          <template #default="{ item }">
            <div class="waterfall-item cursor-pointer group" @click="previewMedia(item)">
              <div class="flip-inner border-2 border-[var(--ink)] bg-gray-50 dark:bg-gray-800 rounded-xl">
                <div class="flip-front relative aspect-video bg-black flex items-center justify-center">
                  <video :src="normalizeUnsafeUrl(item.url)" preload="none" class="w-full h-full object-contain block rounded-[10px]" />
                  <div class="absolute inset-0 flex items-center justify-center pointer-events-none rounded-[10px]">
                    <el-icon :size="40" class="text-white drop-shadow-md opacity-80 backdrop-blur-sm rounded-full bg-black/20"><VideoPlay /></el-icon>
                  </div>
                </div>
                <div class="flip-back p-4 flex flex-col text-[var(--ink)] dark:text-gray-200">
                  <div class="flex-1 flex flex-col min-h-0 border-b border-dashed border-gray-300 dark:border-gray-600 pb-2 mb-2">
                    <h3 class="font-bold text-base mb-1 whitespace-normal break-words line-clamp-2" :title="item.title">{{ item.title || '精选视频' }}</h3>
                    <div class="text-xs text-gray-500 dark:text-gray-400 whitespace-normal break-words line-clamp-3 overflow-hidden flex-1">{{ item.description || '暂无详细描述...' }}</div>
                  </div>
                  <div class="mt-auto">
                    <div class="flex items-center gap-2">
                      <el-avatar :src="toAvatarThumb(item.avatar, 36)" :size="24">{{ item.nickname?.charAt(0) || '?' }}</el-avatar>
                      <div class="flex flex-col text-[11px] text-gray-600 dark:text-gray-400 truncate flex-1 leading-tight">
                        <span class="font-bold truncate">{{ item.nickname || '匿名' }}</span>
                        <span>{{ item.createdAt?.split(' ')[0] }}</span>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </template>
        </WaterfallLayout>
        <div class="h-20 flex items-center justify-center">
          <el-icon v-if="hasMoreMedia && isLoading" class="is-loading"><Loading /></el-icon>
          <span v-else-if="mediaList.length && !hasMoreMedia" class="text-gray-400 text-sm">到底啦，看看别的吧</span>
        </div>
      </el-tab-pane>

      <!-- 公开相册 -->
      <el-tab-pane label="浏览相册" name="albums">
        <div v-if="albums.length" class="jp-album-grid grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 xl:grid-cols-5 2xl:grid-cols-6 gap-6 pt-4">
          <div v-for="album in albums" :key="album.id" class="cursor-pointer group relative" @click="viewAlbum(album)">
            <!-- 堆叠效果 -->
            <div v-if="album.coverUrls?.length > 2" class="absolute -top-2 -right-2 w-full h-full -z-20 rotate-3 opacity-40 rounded-xl bg-[var(--paper)] dark:bg-[#2a2a2a] border border-gray-200 dark:border-gray-600"></div>
            <div v-if="album.coverUrls?.length > 1" class="absolute -top-1 -right-1 w-full h-full -z-10 rotate-1 opacity-70 rounded-xl bg-[var(--paper)] dark:bg-[#2a2a2a] border border-gray-200 dark:border-gray-600"></div>
            <!-- 主卡片 -->
            <div class="post-card jp-no-pad jp-album-base relative z-0">
              <div class="aspect-square bg-gray-100 overflow-hidden">
                <el-image v-if="album.coverUrls?.length" :src="normalizeUnsafeUrl(album.coverUrls[0])" class="w-full h-full block" fit="cover" lazy>
                  <template #error>
                    <div class="w-full h-full flex flex-col items-center justify-center bg-gray-100 dark:bg-gray-800 text-gray-400">
                      <el-icon :size="24"><Picture /></el-icon>
                    </div>
                  </template>
                  <template #placeholder>
                    <div class="w-full h-full flex items-center justify-center bg-gray-50 dark:bg-gray-800" v-loading="true"></div>
                  </template>
                </el-image>
                <div v-else class="w-full h-full flex items-center justify-center text-gray-300">
                  <el-icon :size="48"><Picture /></el-icon>
                </div>
              </div>
              <div class="p-3">
                <div class="flex items-center justify-between mb-1">
                  <span class="font-bold truncate text-sm">{{ album.title }}</span>
                </div>
                <p class="text-xs text-gray-500 truncate mb-2">{{ album.description || '发现生活中的美好。' }}</p>
                <div class="flex items-center justify-between mt-auto">
                  <span class="text-xs text-gray-400 opacity-80">{{ album.mediaCount }} 项内容</span>
                  <div class="flex items-center gap-1">
                    <el-avatar :src="toAvatarThumb(album.avatar, 40)" :size="16">{{ album.nickname?.charAt(0) || '?' }}</el-avatar>
                    <span class="text-xs text-gray-500 truncate max-w-16">{{ album.nickname || '匿名' }}</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
        <div ref="albumsLoadTrigger" class="h-20 flex items-center justify-center">
          <el-icon v-if="hasMoreAlbums && isLoading" class="is-loading"><Loading /></el-icon>
          <span v-else-if="albums.length && !hasMoreAlbums" class="text-gray-400 text-sm">已经到底啦</span>
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- 相册详情弹出 -->
    <el-dialog v-model="showAlbumDetail" :title="currentAlbum?.title" :width="isMobile ? '95%' : '1100px'" class="jp-dialog">
      <div class="mb-6 px-2">
        <h2 class="text-xl font-bold mb-2">{{ currentAlbum?.title }}</h2>
        <p class="text-gray-500 text-sm">{{ currentAlbum?.description || '暂无详细描述。' }}</p>
      </div>
      <div v-if="albumMedia.length" class="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 gap-4">
        <div v-for="item in albumMedia" :key="item.id" class="post-card jp-no-pad jp-media-card cursor-pointer" @click="previewMedia(item)">
          <div class="aspect-video bg-gray-100 overflow-hidden relative">
            <img v-if="item.type === 'image'" :src="normalizeUnsafeUrl(item.thumbnailUrl || item.url)" class="w-full h-full object-cover" />
            <video v-else :src="normalizeUnsafeUrl(item.url)" preload="none" class="w-full h-full object-cover" />
            <div v-if="item.type === 'video'" class="absolute inset-0 flex items-center justify-center pointer-events-none">
                <el-icon :size="40" class="text-white drop-shadow-lg"><VideoPlay /></el-icon>
            </div>
          </div>
          <div class="p-3">
            <h3 class="font-bold text-sm truncate mb-1">{{ item.title || '无标题记录' }}</h3>
            <div class="flex items-center justify-between text-xs text-gray-400">
              <div class="flex items-center gap-1">
                <el-avatar :src="toAvatarThumb(item.avatar, 36)" :size="16">{{ item.nickname?.charAt(0) }}</el-avatar>
                <span class="truncate">{{ item.nickname || '匿名' }}</span>
              </div>
              <span>{{ item.createdAt?.split(' ')[0] }}</span>
            </div>
          </div>
        </div>
      </div>
      <el-empty v-else description="该相册还是空的噢~" />
    </el-dialog>

    <!-- 媒体详情预览 -->
    <el-dialog v-model="showPreview" :width="isMobile ? '95%' : '700px'" class="preview-dialog" top="5vh">
      <div v-if="previewItem" class="flex flex-col gap-4">
        <div class="rounded-xl overflow-hidden bg-black/5 flex items-center justify-center min-h-[40vh] max-h-[75vh]">
          <img v-if="previewItem.type === 'image'" :src="normalizeUnsafeUrl(previewItem.url)" class="max-w-full max-h-full object-contain" />
          <video v-else :src="normalizeUnsafeUrl(previewItem.url)" controls class="max-w-full max-h-full" />
        </div>
        <div class="px-2 pb-2">
          <div class="flex items-center justify-between gap-4 mb-2">
            <h3 class="text-xl font-bold">{{ previewItem.title || '无标题' }}</h3>
            <span class="text-xs text-gray-400">{{ previewItem.createdAt }}</span>
          </div>
          <p class="text-gray-600 text-sm mb-6 leading-relaxed">{{ previewItem.description || '发现生活，分享美好。' }}</p>
          <div class="flex items-center gap-3 py-4 border-t border-dashed">
            <el-avatar v-if="previewItem.source === 'bing'" src="https://www.bing.com/favicon.ico" :size="32" />
            <el-avatar v-else :src="toAvatarThumb(previewItem.avatar, 64)" :size="32">{{ previewItem.nickname?.charAt(0) || '?' }}</el-avatar>
            <div class="flex flex-col">
               <span class="font-bold text-sm" :class="{ 'cursor-pointer hover:text-blue-500': previewItem.source === 'bing' }" @click="openSource(previewItem)">
                 {{ previewItem.source === 'bing' ? '必应每日壁纸' : (previewItem.nickname || '匿名作者') }}
               </span>
               <span class="text-[10px] text-gray-400">公开于 览影廊</span>
            </div>
          </div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { Picture, VideoPlay, Loading } from '@element-plus/icons-vue'
import * as api from '@/api/blog'
import { normalizeUnsafeUrl, toAvatarThumb } from '@/utils/image'
import WaterfallLayout from '@/components/WaterfallLayout.vue'

const activeTab = ref('images')
const isMobile = ref(window.innerWidth < 768)
const albums = ref([])
const mediaList = ref([])
const albumMedia = ref([])
const currentAlbum = ref(null)
const showAlbumDetail = ref(false)
const showPreview = ref(false)
const previewItem = ref(null)
const page = ref(1)
const hasMoreAlbums = ref(true)
const hasMoreMedia = ref(true)
const isLoading = ref(false)

const loadAlbums = async (reset = false) => {
  if (isLoading.value) return
  isLoading.value = true
  try {
    if (reset) { page.value = 1; albums.value = [] }
    const res = await api.getPublicAlbums(page.value)
    const data = res.data || []
    albums.value.push(...data)
    hasMoreAlbums.value = data.length === 20
  } catch (error) {
    console.error('加载相册失败:', error)
  } finally {
    isLoading.value = false
  }
}

const loadMedia = async (reset = false) => {
  if (isLoading.value) return
  isLoading.value = true
  try {
    if (reset) { page.value = 1; mediaList.value = [] }
    const type = activeTab.value === 'images' ? 'image' : activeTab.value === 'videos' ? 'video' : null
    const res = await api.getPublicMedia(type, page.value)
    const data = res.data || []
    mediaList.value.push(...data)
    hasMoreMedia.value = data.length === 20
  } catch (error) {
    console.error('加载媒体失败:', error)
  } finally {
    isLoading.value = false
  }
}

const handleScroll = () => {
  const scrollTop = window.pageYOffset || document.documentElement.scrollTop
  const windowHeight = window.innerHeight
  const documentHeight = document.documentElement.scrollHeight
  if (documentHeight - (scrollTop + windowHeight) < 400) {
    if (activeTab.value === 'albums' && hasMoreAlbums.value && !isLoading.value) {
      page.value++
      loadAlbums()
    } else if ((activeTab.value === 'images' || activeTab.value === 'videos') && hasMoreMedia.value && !isLoading.value) {
      page.value++
      loadMedia()
    }
  }
}

const handleTabChange = async (tab) => {
  if (tab === 'albums') {
    await loadAlbums(true)
  } else {
    await loadMedia(true)
  }
}

const viewAlbum = async (album) => {
  currentAlbum.value = album
  const res = await api.getAlbumMedia(album.id)
  albumMedia.value = res.data || []
  showAlbumDetail.value = true
}

const previewMedia = (item) => {
  previewItem.value = item
  showPreview.value = true
}

const openSource = (item) => {
  if (item?.source === 'bing') {
    window.open('https://www.bing.com', '_blank')
  }
}

const waterfallImgRef = ref(null)
const waterfallVidRef = ref(null)

const colCount = ref(4)
const updateColumns = () => {
  const w = window.innerWidth
  if (w < 480) colCount.value = 1
  else if (w < 768) colCount.value = 2
  else if (w < 1280) colCount.value = 3
  else if (w < 1600) colCount.value = 4
  else colCount.value = 5
}

onMounted(async () => {
  updateColumns()
  await loadMedia()
  window.addEventListener('scroll', handleScroll)
  window.addEventListener('resize', () => {
    isMobile.value = window.innerWidth < 768
    updateColumns()
  })
  isMobile.value = window.innerWidth < 768
})

onUnmounted(() => {
  window.removeEventListener('scroll', handleScroll)
  window.removeEventListener('resize', () => { isMobile.value = window.innerWidth < 768 })
})
</script>

<style scoped>
.jp-community-page {
  min-height: 100vh;
  margin: 0 auto;
}

.jp-media-card, .jp-album-base {
  border-radius: 12px !important;
  overflow: hidden;
}

@media (min-width: 1024px) {
  .jp-community-page {
    max-width: 1520px;
  }
}

@media (min-width: 1600px) {
  .jp-community-page {
    max-width: 1720px;
  }
}

.jp-page-title .stamp {
  margin-right: 0 !important;
  transform: rotate(-10deg) scale(0.85);
  background: #2e86de;
  color: white;
  padding: 0 4px;
}

/* 瀑布流卡片样式 */
.waterfall-item {
  perspective: 1200px;
}
.flip-inner {
  position: relative;
  width: 100%;
  transform-style: preserve-3d;
  transition: transform 0.6s cubic-bezier(0.34, 1.56, 0.64, 1);
  box-shadow: 4px 4px 0 var(--subtle-blue);
  border-radius: 12px;
  min-height: 120px;
}
.waterfall-item:hover .flip-inner {
  transform: rotateY(180deg);
  box-shadow: -4px 4px 0 var(--subtle-blue);
}
.flip-front, .flip-back {
  -webkit-backface-visibility: hidden;
  backface-visibility: hidden;
  border-radius: 10px;
}
.flip-front {
  position: relative;
  width: 100%;
  display: flex;
  min-height: 120px;
}
.flip-back {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  transform: rotateY(180deg);
  background: var(--paper);
  z-index: 2;
}
.dark .flip-inner {
  box-shadow: 4px 4px 0 #111;
}
.dark .waterfall-item:hover .flip-inner {
  box-shadow: -4px 4px 0 #111;
}
.dark .flip-back {
  background: #222;
}

@media (max-width: 1024px) { 
  .jp-album-grid { grid-template-columns: repeat(4, 1fr) !important; }
}
@media (max-width: 768px) { 
  .waterfall-container { column-gap: 12px;} 
  .waterfall-item { margin-bottom: 12px; }
  .jp-album-grid { grid-template-columns: repeat(2, 1fr) !important; }
}
/* 卡片风格标签页定制 */
:deep(.custom-card-tabs.el-tabs--card > .el-tabs__header) {
  border-bottom: 2px solid var(--ink);
  margin-bottom: 20px;
}
.dark :deep(.custom-card-tabs.el-tabs--card > .el-tabs__header) {
  border-bottom-color: #444;
}
:deep(.custom-card-tabs.el-tabs--card > .el-tabs__header .el-tabs__nav) {
  border: none;
}
:deep(.custom-card-tabs.el-tabs--card > .el-tabs__header .el-tabs__item) {
  border: 2px solid transparent;
  border-bottom: none;
  font-family: serif;
  font-size: 1.1rem;
  font-weight: bold;
  border-radius: 12px 12px 0 0;
  margin-right: 8px;
  background-color: rgba(0,0,0,0.02);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  color: var(--ink);
}
.dark :deep(.custom-card-tabs.el-tabs--card > .el-tabs__header .el-tabs__item) {
  background-color: rgba(255,255,255,0.02);
  color: #ccc;
}
:deep(.custom-card-tabs.el-tabs--card > .el-tabs__header .el-tabs__item.is-active) {
  background-color: var(--paper);
  border: 2px solid var(--ink);
  border-bottom-color: var(--paper);
  color: var(--accent);
  transform: translateY(2px);
}
.dark :deep(.custom-card-tabs.el-tabs--card > .el-tabs__header .el-tabs__item.is-active) {
  background-color: #1a1a1a;
  border-color: #555;
  border-bottom-color: #1a1a1a;
  color: var(--accent);
}
:deep(.custom-card-tabs.el-tabs--card > .el-tabs__header .el-tabs__item:hover) {
  color: var(--accent);
}
</style>
