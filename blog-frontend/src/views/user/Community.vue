<template>
  <div class="space-y-6">
    <h1 class="text-2xl font-bold">相册</h1>

    <el-tabs v-model="activeTab" @tab-change="handleTabChange">
      <el-tab-pane label="公开相册" name="albums">
        <div class="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 gap-3 sm:gap-6 pt-4 pr-4">
          <div v-for="album in albums" :key="album.id" class="cursor-pointer group hover:-translate-y-1 transition-all duration-300 relative" @click="viewAlbum(album)">
            <!-- 堆叠效果 -->
            <div v-if="album.coverUrls?.length > 2" class="absolute -top-3 -right-3 w-full h-full -z-20 rotate-6">
              <div class="bg-white rounded-lg shadow-sm overflow-hidden border border-gray-200 h-full">
                <div class="aspect-square overflow-hidden"><img :src="album.coverUrls[2]" class="w-full h-full object-cover" /></div>
                <div class="p-2 bg-white"><div class="h-4"></div></div>
              </div>
            </div>
            <div v-if="album.coverUrls?.length > 1" class="absolute -top-1.5 -right-1.5 w-full h-full -z-10 rotate-3">
              <div class="bg-white rounded-lg shadow-sm overflow-hidden border border-gray-200 h-full">
                <div class="aspect-square overflow-hidden"><img :src="album.coverUrls[1]" class="w-full h-full object-cover" /></div>
                <div class="p-2 bg-white"><div class="h-4"></div></div>
              </div>
            </div>
            <!-- 主卡片 -->
            <div class="bg-white rounded-lg shadow hover:shadow-xl overflow-hidden border border-gray-200 relative z-0">
              <div class="aspect-square bg-gray-100 overflow-hidden">
                <img v-if="album.coverUrls?.length" :src="album.coverUrls[0]" class="w-full h-full object-cover" />
                <div v-else class="w-full h-full flex items-center justify-center text-gray-400">
                  <el-icon :size="48"><Picture /></el-icon>
                </div>
              </div>
              <div class="p-2">
                <div class="flex items-center justify-between">
                  <span class="font-medium truncate text-sm">{{ album.title }}</span>
                </div>
                <p class="text-xs text-gray-500 truncate">{{ album.description || '暂无简介' }}</p>
                <div class="flex items-center justify-between mt-1">
                  <span class="text-xs text-gray-400">{{ album.mediaCount }} 项</span>
                  <div class="flex items-center gap-1">
                    <el-avatar :src="album.avatar" :size="16">{{ album.nickname?.charAt(0) || '?' }}</el-avatar>
                    <span class="text-xs text-gray-400 truncate max-w-16">{{ album.nickname || '匿名' }}</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
        <div ref="albumsLoadTrigger" class="h-20 flex items-center justify-center">
          <el-icon v-if="hasMoreAlbums" class="is-loading"><Loading /></el-icon>
          <span v-else-if="albums.length" class="text-gray-400">没有更多了</span>
        </div>
      </el-tab-pane>

      <el-tab-pane label="公开图片" name="images">
        <div class="grid grid-cols-2 md:grid-cols-4 lg:grid-cols-6 gap-2">
          <div v-for="item in mediaList" :key="item.id" class="aspect-square bg-gray-100 rounded overflow-hidden cursor-pointer" @click="previewMedia(item)">
            <img :src="item.thumbnailUrl || item.url" loading="lazy" class="w-full h-full object-cover" />
          </div>
        </div>
        <div ref="mediaLoadTrigger" class="h-20 flex items-center justify-center">
          <el-icon v-if="hasMoreMedia" class="is-loading"><Loading /></el-icon>
          <span v-else-if="mediaList.length" class="text-gray-400">没有更多了</span>
        </div>
      </el-tab-pane>

      <el-tab-pane label="公开视频" name="videos">
        <div class="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4">
          <div v-for="item in mediaList" :key="item.id" class="aspect-video bg-gray-100 rounded overflow-hidden cursor-pointer relative" @click="previewMedia(item)">
            <video :src="item.url" preload="none" class="w-full h-full object-cover" />
            <div class="absolute inset-0 flex items-center justify-center">
              <el-icon :size="48" class="text-white drop-shadow-lg"><VideoPlay /></el-icon>
            </div>
          </div>
        </div>
        <div ref="mediaLoadTrigger" class="h-20 flex items-center justify-center">
          <el-icon v-if="hasMoreMedia" class="is-loading"><Loading /></el-icon>
          <span v-else-if="mediaList.length" class="text-gray-400">没有更多了</span>
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- 相册详情 -->
    <el-dialog v-model="showAlbumDetail" :title="currentAlbum?.title" :width="isMobile ? '95%' : '900px'">
      <p class="text-gray-500 mb-4">{{ currentAlbum?.description }}</p>
      <div v-if="albumMedia.length" class="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 gap-3">
        <div v-for="item in albumMedia" :key="item.id" class="bg-white rounded-lg shadow hover:shadow-lg transition-shadow cursor-pointer overflow-hidden" @click="previewMedia(item)">
          <div class="aspect-square bg-gray-100 overflow-hidden">
            <img v-if="item.type === 'image'" :src="item.thumbnailUrl || item.url" loading="lazy" class="w-full h-full object-cover" />
            <video v-else :src="item.url" preload="none" class="w-full h-full object-cover" />
          </div>
          <div class="p-2">
            <p class="text-xs text-gray-500 truncate">{{ item.description || '暂无简介' }}</p>
            <div class="flex items-center gap-1 mt-1">
              <el-avatar :src="item.avatar" :size="16">{{ item.nickname?.charAt(0) }}</el-avatar>
              <span class="text-xs text-gray-400 truncate">{{ item.nickname || '匿名' }}</span>
            </div>
          </div>
        </div>
      </div>
      <el-empty v-else description="暂无媒体" />
    </el-dialog>

    <!-- 媒体预览 -->
    <el-dialog v-model="showPreview" :width="isMobile ? '95%' : '600px'" top="5vh">
      <div class="max-h-[60vh] overflow-hidden flex items-center justify-center bg-gray-100">
        <img v-if="previewItem?.type === 'image'" :src="previewItem?.url" class="max-w-full max-h-[60vh] object-contain" />
        <video v-else :src="previewItem?.url" controls class="max-w-full max-h-[60vh]" />
      </div>
      <div class="mt-4">
        <h3 class="font-bold text-lg">{{ previewItem?.title || '无标题' }}</h3>
        <p class="text-gray-500 mt-2">{{ previewItem?.description || '暂无简介' }}</p>
        <div class="flex items-center gap-2 mt-4 pt-4 border-t">
          <el-avatar v-if="previewItem?.source === 'bing'" :size="32" src="https://www.bing.com/favicon.ico" class="cursor-pointer" @click="openSource(previewItem)">B</el-avatar>
          <el-avatar v-else :src="previewItem?.avatar" :size="32">{{ previewItem?.nickname?.charAt(0) }}</el-avatar>
          <span class="font-medium" :class="{ 'cursor-pointer hover:text-primary-500': previewItem?.source === 'bing' }" @click="openSource(previewItem)">{{ previewItem?.source === 'bing' ? '必应壁纸' : (previewItem?.nickname || '匿名') }}</span>
          <span class="text-gray-400 ml-auto">{{ previewItem?.createdAt }}</span>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { Picture, VideoPlay, Loading } from '@element-plus/icons-vue'
import * as api from '@/api/blog'

const activeTab = ref('albums')
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
const albumsLoadTrigger = ref(null)
const mediaLoadTrigger = ref(null)
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

const loadMoreAlbums = () => {
  if (!isLoading.value && hasMoreAlbums.value) {
    page.value++
    loadAlbums()
  }
}

const loadMoreMedia = () => {
  if (!isLoading.value && hasMoreMedia.value) {
    page.value++
    loadMedia()
  }
}

// 使用 scroll 事件监听
const handleScroll = () => {
  const scrollTop = window.pageYOffset || document.documentElement.scrollTop
  const windowHeight = window.innerHeight
  const documentHeight = document.documentElement.scrollHeight

  // 距离底部 300px 时触发加载
  const distanceToBottom = documentHeight - (scrollTop + windowHeight)

  if (distanceToBottom < 300) {
    if (activeTab.value === 'albums' && hasMoreAlbums.value && !isLoading.value) {
      loadMoreAlbums()
    } else if ((activeTab.value === 'images' || activeTab.value === 'videos') && hasMoreMedia.value && !isLoading.value) {
      loadMoreMedia()
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

onMounted(async () => {
  await loadAlbums()
  window.addEventListener('scroll', handleScroll)
})

onUnmounted(() => {
  window.removeEventListener('scroll', handleScroll)
})
</script>
