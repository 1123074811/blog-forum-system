<template>
  <div class="space-y-6">
    <div class="flex justify-between items-center">
      <h1 class="text-2xl font-bold">我的相册</h1>
      <div class="flex gap-2">
        <el-button type="primary" @click="showCreateDialog = true">创建相册</el-button>
        <el-button @click="showUploadDialog = true">上传媒体</el-button>
      </div>
    </div>

    <el-tabs v-model="activeTab">
      <el-tab-pane label="相册" name="albums">
        <div class="grid grid-cols-3 md:grid-cols-4 lg:grid-cols-5 gap-6 pt-4 pr-4">
          <div v-for="album in albums" :key="album.id" class="cursor-pointer group hover:-translate-y-1 transition-all duration-300 relative" @click="viewAlbum(album)">
            <!-- 堆叠效果：完整卡片 -->
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
                  <el-tag v-if="album.isPublic" size="small" type="success">公开</el-tag>
                </div>
                <p class="text-xs text-gray-500 truncate">{{ album.description || '暂无简介' }}</p>
                <span class="text-xs text-gray-400">{{ album.mediaCount }} 项</span>
              </div>
              <div class="absolute top-2 right-2 hidden group-hover:flex gap-1">
                <el-button size="small" circle @click.stop="editAlbum(album)"><el-icon><Edit /></el-icon></el-button>
                <el-button size="small" circle @click.stop="togglePublic(album)"><el-icon><View /></el-icon></el-button>
                <el-button size="small" circle type="danger" @click.stop="deleteAlbum(album)"><el-icon><Delete /></el-icon></el-button>
              </div>
            </div>
          </div>
        </div>
      </el-tab-pane>

      <el-tab-pane label="全部媒体" name="media">
        <div class="flex gap-2 mb-4">
          <el-radio-group v-model="mediaType" @change="loadMedia">
            <el-radio-button :value="null">全部</el-radio-button>
            <el-radio-button value="image">图片</el-radio-button>
            <el-radio-button value="video">视频</el-radio-button>
          </el-radio-group>
        </div>
        <div class="grid grid-cols-3 md:grid-cols-4 lg:grid-cols-5 gap-3">
          <div v-for="item in mediaList" :key="item.id" class="bg-white rounded-lg shadow hover:shadow-xl hover:-translate-y-1 transition-all duration-300 cursor-pointer overflow-hidden" @click="previewMedia(item)">
            <div class="aspect-video bg-gray-100 overflow-hidden">
              <img v-if="item.type === 'image'" :src="item.url" class="w-full h-full object-cover" />
              <video v-else :src="item.url" class="w-full h-full object-cover" />
            </div>
            <div class="p-2">
              <div class="flex items-center justify-between mb-1">
                <h3 class="font-medium text-sm truncate">{{ item.title || '无标题' }}</h3>
                <el-tag v-if="item.isPublic" size="small" type="success">公开</el-tag>
              </div>
              <div class="flex items-center justify-between text-xs text-gray-400">
                <div class="flex items-center gap-1">
                  <el-avatar :src="item.avatar" :size="18">{{ item.nickname?.charAt(0) }}</el-avatar>
                  <span class="truncate max-w-16">{{ item.nickname || '匿名' }}</span>
                </div>
                <span>{{ item.createdAt?.split(' ')[0] }}</span>
              </div>
            </div>
            <div class="px-2 pb-2 flex gap-1">
              <el-button size="small" @click.stop="toggleMediaPublic(item)">{{ item.isPublic ? '私密' : '公开' }}</el-button>
              <el-button size="small" type="danger" @click.stop="deleteMediaItem(item)">删除</el-button>
            </div>
          </div>
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- 创建/编辑相册对话框 -->
    <el-dialog v-model="showCreateDialog" :title="isEditMode ? '编辑相册' : '创建相册'" width="400px" @close="resetAlbumForm">
      <el-form :model="albumForm" label-width="60px">
        <el-form-item label="标题"><el-input v-model="albumForm.title" /></el-form-item>
        <el-form-item label="简介"><el-input v-model="albumForm.description" type="textarea" rows="3" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreateDialog = false">取消</el-button>
        <el-button type="primary" @click="handleSaveAlbum">{{ isEditMode ? '保存' : '创建' }}</el-button>
      </template>
    </el-dialog>

    <!-- 相册详情对话框 -->
    <el-dialog v-model="showAlbumDetail" :title="currentAlbum?.title" width="900px">
      <div v-if="albumMedia.length" class="grid grid-cols-3 md:grid-cols-4 gap-3">
        <div v-for="item in albumMedia" :key="item.id" class="bg-white rounded-lg shadow hover:shadow-lg transition-shadow cursor-pointer overflow-hidden" @click="previewMedia(item)">
          <div class="aspect-square bg-gray-100 overflow-hidden">
            <img v-if="item.type === 'image'" :src="item.url" class="w-full h-full object-cover" />
            <video v-else :src="item.url" class="w-full h-full object-cover" />
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

    <!-- 上传媒体对话框 -->
    <el-dialog v-model="showUploadDialog" title="上传媒体" width="500px">
      <el-form :model="uploadForm" label-width="60px">
        <el-form-item label="相册">
          <el-select v-model="uploadForm.albumId" placeholder="选择相册（可选）" clearable style="width: 100%">
            <el-option v-for="a in albums" :key="a.id" :label="a.title" :value="a.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="标题"><el-input v-model="uploadForm.title" /></el-form-item>
        <el-form-item label="简介"><el-input v-model="uploadForm.description" type="textarea" rows="2" /></el-form-item>
        <el-form-item label="文件">
          <el-upload drag multiple :auto-upload="false" :file-list="uploadFiles" :on-change="handleFileChange" accept="image/*,video/*">
            <el-icon :size="48"><Upload /></el-icon>
            <div>拖拽或点击上传图片/视频</div>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showUploadDialog = false">取消</el-button>
        <el-button type="primary" :loading="uploading" @click="handleUpload">上传</el-button>
      </template>
    </el-dialog>

    <!-- 媒体预览 -->
    <el-dialog v-model="showPreview" width="600px" top="5vh">
      <div class="max-h-[60vh] overflow-hidden flex items-center justify-center bg-gray-100">
        <img v-if="previewItem?.type === 'image'" :src="previewItem?.url" class="max-w-full max-h-[60vh] object-contain" />
        <video v-else :src="previewItem?.url" controls class="max-w-full max-h-[60vh]" />
      </div>
      <div class="mt-4">
        <h3 class="font-bold text-lg">{{ previewItem?.title || '无标题' }}</h3>
        <p class="text-gray-500 mt-2">{{ previewItem?.description || '暂无简介' }}</p>
        <div class="flex items-center gap-2 mt-4 pt-4 border-t">
          <el-avatar :src="previewItem?.avatar" :size="32">{{ previewItem?.nickname?.charAt(0) }}</el-avatar>
          <span class="font-medium">{{ previewItem?.nickname || '匿名' }}</span>
          <span class="text-gray-400 ml-auto">{{ previewItem?.createdAt }}</span>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { Picture, Edit, Delete, View, Upload } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as api from '@/api/blog'

const activeTab = ref('albums')
const albums = ref([])
const mediaList = ref([])
const mediaType = ref(null)
const showCreateDialog = ref(false)
const showUploadDialog = ref(false)
const showPreview = ref(false)
const previewItem = ref(null)
const uploading = ref(false)
const uploadFiles = ref([])

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

const handleSaveAlbum = async () => {
  if (isEditMode.value) {
    await api.updateAlbum(albumForm.value.id, albumForm.value)
    ElMessage.success('更新成功')
  } else {
    await api.createAlbum(albumForm.value)
    ElMessage.success('创建成功')
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
  await ElMessageBox.confirm('确定删除该相册？')
  await api.deleteAlbum(album.id)
  ElMessage.success('删除成功')
  loadAlbums()
}

const togglePublic = async (album) => {
  await api.toggleAlbumPublic(album.id)
  ElMessage.success(album.isPublic ? '已设为私密' : '已公开')
  loadAlbums()
}

const viewAlbum = async (album) => {
  currentAlbum.value = album
  const res = await api.getAlbumMedia(album.id)
  albumMedia.value = res.data || []
  showAlbumDetail.value = true
}

const handleFileChange = (file, fileList) => {
  uploadFiles.value = fileList
}

const handleUpload = async () => {
  if (!uploadFiles.value.length) return
  uploading.value = true
  try {
    for (const f of uploadFiles.value) {
      await api.uploadMedia(f.raw, uploadForm.value.albumId, uploadForm.value.title, uploadForm.value.description)
    }
    ElMessage.success('上传成功')
    showUploadDialog.value = false
    uploadFiles.value = []
    uploadForm.value = { albumId: null, title: '', description: '' }
    loadAlbums()
    loadMedia()
  } finally {
    uploading.value = false
  }
}

const previewMedia = (item) => {
  previewItem.value = item
  showPreview.value = true
}

const toggleMediaPublic = async (item) => {
  await api.toggleMediaPublic(item.id)
  ElMessage.success(item.isPublic ? '已设为私密' : '已公开')
  loadMedia()
}

const deleteMediaItem = async (item) => {
  await ElMessageBox.confirm('确定删除？')
  await api.deleteMedia(item.id)
  ElMessage.success('删除成功')
  loadMedia()
}

onMounted(() => {
  loadAlbums()
  loadMedia()
})
</script>
