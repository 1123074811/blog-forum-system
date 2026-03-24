<template>
  <div class="jp-album-page w-full px-2 sm:px-6 pt-8 pb-12 space-y-6">
    <!-- 顶部工具栏 -->
    <div class="jp-toolbar flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
      <h1 class="jp-page-title text-2xl sm:text-3xl font-bold dark:text-white">
        <span class="stamp">相</span>我的相册
      </h1>
      <div class="flex gap-2">
        <el-button type="primary" @click="showCreateDialog = true">创建相册</el-button>
        <el-button @click="showUploadDialog = true">上传媒体</el-button>
      </div>
    </div>

    <el-tabs v-model="activeTab" class="jp-tabs custom-card-tabs" type="card" @tab-change="handleTabChange">
      <!-- 我的相册列表 Tab -->
      <el-tab-pane label="我的上传" name="media">
        <div class="jp-filter-bar mb-6">
          <el-radio-group v-model="mediaType" @change="loadMedia">
            <el-radio-button :value="null">全部</el-radio-button>
            <el-radio-button value="image">图片</el-radio-button>
            <el-radio-button value="video">视频</el-radio-button>
          </el-radio-group>
        </div>
        
        <WaterfallLayout v-if="mediaList.length" :items="mediaList" :cols="colCount" :gap="16" class="py-2 px-1">
          <template #default="{ item }">
            <div class="waterfall-item cursor-pointer group" @click="previewMedia(item)">
              <div class="flip-inner border-2 border-[var(--ink)] bg-gray-50 dark:bg-gray-800 rounded-xl">
                <!-- 正面：原比例展示 -->
                <div class="flip-front relative min-h-[120px]">
                  <el-image v-if="item.type === 'image'" :src="normalizeUnsafeUrl(item.thumbnailUrl || item.url)" class="w-full h-auto block rounded-[10px]">
                    <template #error>
                      <div class="w-full aspect-[4/3] flex flex-col items-center justify-center bg-gray-100 dark:bg-gray-800 text-gray-400">
                        <el-icon :size="24"><Picture /></el-icon>
                        <span class="text-[10px] mt-1">加载失败</span>
                      </div>
                    </template>
                    <template #placeholder>
                      <div class="w-full aspect-[4/3] flex items-center justify-center bg-gray-50 dark:bg-gray-800" v-loading="true"></div>
                    </template>
                  </el-image>
                  <video v-else :src="item.url" preload="none" class="w-full aspect-video block rounded-[10px]" />
                  <div v-if="item.type === 'video'" class="absolute inset-0 flex items-center justify-center pointer-events-none rounded-[10px]">
                    <el-icon :size="40" class="text-white drop-shadow-md opacity-80 backdrop-blur-sm rounded-full bg-black/20"><VideoPlay /></el-icon>
                  </div>
                </div>
                <!-- 背面：信息与操作 -->
                <div class="flip-back p-4 flex flex-col text-[var(--ink)] dark:text-gray-200">
                  <div class="flex-1 flex flex-col min-h-0 border-b border-dashed border-gray-300 dark:border-gray-600 pb-2 mb-2">
                    <h3 class="font-bold text-base mb-1 whitespace-normal break-words line-clamp-2" :title="item.title">{{ item.title || '无标题记录' }}</h3>
                    <div class="text-xs text-gray-500 dark:text-gray-400 whitespace-normal break-words line-clamp-3 overflow-hidden flex-1">{{ item.description || '暂无详细描述...' }}</div>
                  </div>
                  <div class="mt-auto">
                    <div class="flex items-center gap-2 mb-3">
                      <el-avatar :src="toAvatarThumb(item.avatar, 36)" :size="24">{{ item.nickname?.charAt(0) }}</el-avatar>
                      <div class="flex flex-col text-[11px] text-gray-600 dark:text-gray-400 truncate flex-1 leading-tight">
                        <span class="font-bold truncate">{{ item.nickname || '匿名' }}</span>
                        <span>{{ item.createdAt?.split(' ')[0] }}</span>
                      </div>
                    </div>
                    <div class="flex gap-2 justify-end pt-2 items-center">
                      <el-tag v-if="item.isPublic" size="small" type="success" class="mr-auto">公开</el-tag>
                      <template v-if="userStore.user?.id === item.userId || userStore.user?.username === item.username">
                        <el-button size="small" @click.stop="toggleMediaPublic(item)">{{ item.isPublic ? '私密' : '公开' }}</el-button>
                        <el-button size="small" type="danger" plain @click.stop="deleteMediaItem(item)">删除</el-button>
                      </template>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </template>
        </WaterfallLayout>
        <el-empty v-else description="暂无媒体内容" class="jp-empty-card" />
      </el-tab-pane>

      <!-- 相册 Tab -->
      <el-tab-pane label="相册" name="albums">
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
                  <el-tag v-if="album.isPublic" size="small" type="success">公开</el-tag>
                </div>
                <p class="text-xs text-gray-500 truncate mb-1">{{ album.description || '暂无简介' }}</p>
                <span class="text-xs text-gray-400 opacity-70">{{ album.mediaCount }} 项内容</span>
              </div>
              <!-- 管理按钮 -->
              <div class="absolute top-2 right-2 hidden group-hover:flex gap-1">
                <el-button size="small" circle @click.stop="editAlbum(album)"><el-icon><Edit /></el-icon></el-button>
                <el-button size="small" circle @click.stop="togglePublic(album)"><el-icon><View /></el-icon></el-button>
                <el-button size="small" circle type="danger" @click.stop="deleteAlbum(album)"><el-icon><Delete /></el-icon></el-button>
              </div>
            </div>
          </div>
        </div>
        <el-empty v-else description="暂无相册，先创建一个吧" class="jp-empty-card" />
      </el-tab-pane>
    </el-tabs>

    <!-- 弹窗部分样式也需要统一 -->
    <el-dialog v-model="showAlbumDetail" :title="currentAlbum?.title" :width="isMobile ? '95%' : '1100px'" class="jp-dialog">
      <div v-if="albumMedia.length" class="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 gap-4">
        <div v-for="item in albumMedia" :key="item.id" class="post-card jp-no-pad jp-media-card cursor-pointer" @click="previewMedia(item)">
          <div class="aspect-video bg-gray-100 overflow-hidden relative">
            <el-image v-if="item.type === 'image'" :src="normalizeUnsafeUrl(item.thumbnailUrl || item.url)" class="w-full h-full block" fit="cover" lazy>
              <template #error>
                <div class="w-full h-full flex flex-col items-center justify-center bg-gray-100 dark:bg-gray-800 text-gray-400">
                  <el-icon :size="20"><Picture /></el-icon>
                </div>
              </template>
              <template #placeholder>
                <div class="w-full h-full flex items-center justify-center bg-gray-50 dark:bg-gray-800" v-loading="true"></div>
              </template>
            </el-image>
            <video v-else :src="item.url" preload="none" class="w-full h-full object-cover" />
            <div v-if="item.type === 'video'" class="absolute inset-0 flex items-center justify-center pointer-events-none">
              <el-icon :size="40" class="text-white drop-shadow-lg"><VideoPlay /></el-icon>
            </div>
          </div>
          <div class="p-2">
            <h3 class="font-bold text-sm truncate mb-1">{{ item.title || '无标题' }}</h3>
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
      <el-empty v-else description="暂无媒体" />
    </el-dialog>

    <!-- 创建相册对话框 -->
    <el-dialog v-model="showCreateDialog" :title="isEditMode ? '编辑相册' : '创建相册'" :width="isMobile ? '92%' : '400px'">
      <el-form :model="albumForm" label-position="top">
        <el-form-item label="标题"><el-input v-model="albumForm.title" placeholder="给相册起个好听的名字" /></el-form-item>
        <el-form-item label="简介"><el-input v-model="albumForm.description" type="textarea" rows="3" placeholder="写点什么介绍一下..." /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreateDialog = false">取消</el-button>
        <el-button type="primary" @click="handleSaveAlbum">{{ isEditMode ? '保存修改' : '立即创建' }}</el-button>
      </template>
    </el-dialog>

    <!-- 上传媒体对话框 -->
    <el-dialog v-model="showUploadDialog" title="上传媒体" :width="isMobile ? '92%' : '500px'">
      <el-form :model="uploadForm" label-position="top">
        <el-form-item label="所属相册">
          <el-select v-model="uploadForm.albumId" placeholder="选择相册（可选）" clearable style="width: 100%">
            <el-option v-for="a in albums" :key="a.id" :label="a.title" :value="a.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="媒体标题"><el-input v-model="uploadForm.title" /></el-form-item>
        <el-form-item label="作品简介"><el-input v-model="uploadForm.description" type="textarea" rows="2" /></el-form-item>
        <el-form-item label="选择文件">
          <el-upload
            drag multiple :auto-upload="false"
            :on-change="handleFileChange" :on-remove="handleFileChange" accept="image/*,video/*"
            style="width: 100%"
          >
            <el-icon :size="48" class="text-gray-400"><Upload /></el-icon>
            <div class="text-sm">拖拽文件到此处，或<span class="text-accent">点击上传</span></div>
            <div class="text-xs text-gray-400 mt-2">支持图片、视频格式</div>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showUploadDialog = false">取消</el-button>
        <el-button type="primary" :loading="uploading" @click="handleUpload">开始上传</el-button>
      </template>
    </el-dialog>

    <!-- 预览图 -->
    <el-dialog v-model="showPreview" :width="isMobile ? '95%' : '700px'" class="preview-dialog">
      <div v-if="previewItem" class="flex flex-col gap-4">
        <div class="aspect-auto max-h-[70vh] overflow-hidden rounded-lg bg-black/5 flex items-center justify-center">
          <el-image v-if="previewItem.type === 'image'" :src="normalizeUnsafeUrl(previewItem.url)" class="max-w-full max-h-full block" fit="contain">
            <template #error>
              <div class="w-full h-full min-h-[200px] flex flex-col items-center justify-center text-gray-400">
                <el-icon :size="48"><Picture /></el-icon>
                <span class="text-sm mt-2">图片加载失败或已被删除</span>
              </div>
            </template>
            <template #placeholder>
              <div class="w-full h-full min-h-[200px] flex items-center justify-center" v-loading="true"></div>
            </template>
          </el-image>
          <video v-else :src="previewItem.url" controls class="max-w-full max-h-full" />
        </div>
        <div class="px-2">
          <div class="flex items-center justify-between mb-2">
            <h2 class="text-xl font-bold">{{ previewItem.title || '无标题记录' }}</h2>
            <span class="text-xs text-gray-400">{{ previewItem.createdAt }}</span>
          </div>
          <p class="text-gray-600 text-sm mb-4">{{ previewItem.description || '暂无详细描述。' }}</p>
          <div class="flex items-center gap-2 pt-4 border-t border-dashed">
            <el-avatar :src="toAvatarThumb(previewItem.avatar, 64)" :size="32">{{ previewItem.nickname?.charAt(0) }}</el-avatar>
            <span class="font-medium text-sm">{{ previewItem.nickname || '作者' }}</span>
          </div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { Picture, Edit, Delete, View, Upload, Document, VideoPlay } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as api from '@/api/blog'
import { useUserStore } from '@/stores/user'
import { normalizeUnsafeUrl, toAvatarThumb } from '@/utils/image'
import WaterfallLayout from '@/components/WaterfallLayout.vue'

const userStore = useUserStore()
const activeTab = ref('media')
const isMobile = ref(window.innerWidth < 768)
const albums = ref([])
const mediaList = ref([])
const mediaType = ref(null)
const showCreateDialog = ref(false)
const showUploadDialog = ref(false)
const showPreview = ref(false)
const previewItem = ref(null)
const uploading = ref(false)
const uploadFiles = ref([])

const colCount = ref(4)
const updateColumns = () => {
  const w = window.innerWidth
  if (w < 480) colCount.value = 1
  else if (w < 768) colCount.value = 2
  else if (w < 1280) colCount.value = 3
  else if (w < 1600) colCount.value = 4
  else colCount.value = 5
}

const albumForm = ref({ id: null, title: '', description: '' })
const uploadForm = ref({ albumId: null, title: '', description: '' })
const isEditMode = ref(false)
const showAlbumDetail = ref(false)
const currentAlbum = ref(null)
const albumMedia = ref([])

const loadAlbums = async () => {
  const res = await api.getMyAlbums()
  albums.value = res.data || []
}

const loadMedia = async () => {
  const res = await api.getMyMedia(mediaType.value)
  mediaList.value = res.data || []
}

const handleTabChange = () => {
  if (activeTab.value === 'albums') {
    loadAlbums()
  } else {
    loadMedia()
  }
}

const handleSaveAlbum = async () => {
  if (!albumForm.value.title.trim()) return ElMessage.warning('请输入标题')
  if (isEditMode.value) {
    await api.updateAlbum(albumForm.value.id, albumForm.value)
    ElMessage.success('相册已更新')
  } else {
    await api.createAlbum(albumForm.value)
    ElMessage.success('相册创建成功')
  }
  showCreateDialog.value = false
  resetAlbumForm()
  loadAlbums()
}

const resetAlbumForm = () => {
  albumForm.value = { id: null, title: '', description: '' }
  isEditMode.value = false
}

const editAlbum = (album) => {
  isEditMode.value = true
  albumForm.value = { id: album.id, title: album.title, description: album.description }
  showCreateDialog.value = true
}

const deleteAlbum = async (album) => {
  await ElMessageBox.confirm('确定要删除这个相册吗？其中的媒体文件不会被物理删除。', '警告', { type: 'warning' })
  await api.deleteAlbum(album.id)
  ElMessage.success('相册已删除')
  loadAlbums()
}

const togglePublic = async (album) => {
  if (!album.isPublic) {
    await ElMessageBox.confirm('请选择公开方式', '权限设置', {
      confirmButtonText: '正常公开',
      cancelButtonText: '匿名公开',
      distinguishCancelAndClose: true,
      type: 'info'
    }).then(async () => {
      await api.toggleAlbumPublic(album.id, false)
      ElMessage.success('相册已公开')
      loadAlbums()
    }).catch((action) => {
      if (action === 'cancel') {
        api.toggleAlbumPublic(album.id, true).then(() => {
          ElMessage.success('已设置为匿名公开')
          loadAlbums()
        })
      }
    })
  } else {
    await api.toggleAlbumPublic(album.id, false)
    ElMessage.success('相册已设为私密')
    loadAlbums()
  }
}

const viewAlbum = async (album) => {
  currentAlbum.value = album
  const res = await api.getAlbumMedia(album.id)
  albumMedia.value = res.data || []
  showAlbumDetail.value = true
}

const handleFileChange = (file, fileList) => {
  uploadFiles.value = fileList.map(f => f)
}

const handleUpload = async () => {
  if (!uploadFiles.value.length) return ElMessage.warning('请先选择文件')
  uploading.value = true
  try {
    for (const f of uploadFiles.value) {
      await api.uploadMedia(f.raw, uploadForm.value.albumId, uploadForm.value.title, uploadForm.value.description)
    }
    ElMessage.success('全部上传成功')
    showUploadDialog.value = false
    uploadFiles.value = []
    uploadForm.value = { albumId: null, title: '', description: '' }
    loadAlbums()
    loadMedia()
  } catch (e) {
    ElMessage.error('上传过程中出现错误')
  } finally {
    uploading.value = false
  }
}

const previewMedia = (item) => {
  previewItem.value = item
  showPreview.value = true
}

const toggleMediaPublic = async (item) => {
  if (!item.isPublic) {
    await ElMessageBox.confirm('请选择媒体公开方式', '公开设置', {
      confirmButtonText: '实名',
      cancelButtonText: '匿名',
      distinguishCancelAndClose: true
    }).then(async () => {
      await api.toggleMediaPublic(item.id, false)
      ElMessage.success('媒体已公开')
      loadMedia()
    }).catch((action) => {
      if (action === 'cancel') {
        api.toggleMediaPublic(item.id, true).then(() => {
          ElMessage.success('媒体已匿名公开')
          loadMedia()
        })
      }
    })
  } else {
    await api.toggleMediaPublic(item.id, false)
    ElMessage.success('媒体已转为私密')
    loadMedia()
  }
}

const deleteMediaItem = async (item) => {
  await ElMessageBox.confirm('确定永久删除该媒体文件吗？此操作不可撤销。', '最终确认', { confirmButtonText: '确定删除', cancelButtonText: '取消', type: 'error' })
  await api.deleteMedia(item.id)
  ElMessage.success('删除成功')
  loadMedia()
}

onMounted(() => {
  updateColumns()
  loadAlbums()
  loadMedia()
  window.addEventListener('resize', () => {
    isMobile.value = window.innerWidth < 768
    updateColumns()
  })
})
</script>

<style scoped>
.jp-album-page {
  min-height: 100vh;
  margin: 0 auto;
}

@media (min-width: 1024px) {
  .jp-album-page {
    max-width: 1520px;
  }
}

@media (min-width: 1600px) {
  .jp-album-page {
    max-width: 1720px;
  }
}

.jp-page-title {
  display: flex;
  align-items: center;
}

.jp-page-title .stamp {
  margin-right: 0 !important;
  transform: rotate(-10deg) scale(0.85);
  background-color: #c0392b;
  color: white;
  padding: 0 4px;
}

/* 核心网格样式统一 */
.jp-media-card, .jp-album-base {
  border-radius: 12px !important; /* 精致圆角 */
  overflow: hidden;
}

.jp-empty-card {
  min-height: 400px;
  border: 2px dashed rgba(203, 213, 225, 0.4);
  border-radius: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
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
  .jp-album-grid { grid-template-columns: repeat(3, 1fr) !important; }
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
