<template>
  <div class="max-w-4xl mx-auto p-6 space-y-8">
    <!-- 网站信息卡片 -->
    <div class="bg-white dark:bg-gray-800 rounded-lg shadow-md p-6">
      <div class="flex items-center justify-between mb-6">
        <h1 class="text-2xl font-bold text-gray-800 dark:text-white">关于我们</h1>
        <el-button v-if="userStore.isAdmin" type="primary" @click="showEditSiteInfo = true">
          <el-icon><Edit /></el-icon>
          编辑网站信息
        </el-button>
      </div>

      <div v-if="siteInfo" class="space-y-4">
        <div class="flex items-center space-x-4">
          <img v-if="siteInfo.siteLogo" :src="siteInfo.siteLogo" :alt="siteInfo.siteName" class="w-16 h-16 rounded-lg">
          <div>
            <h2 class="text-xl font-semibold text-gray-800 dark:text-white">{{ siteInfo.siteName }}</h2>
            <p class="text-gray-600 dark:text-gray-300">{{ siteInfo.siteDescription }}</p>
          </div>
        </div>

        <div class="grid grid-cols-1 md:grid-cols-2 gap-6 mt-6">
          <!-- 联系方式 -->
          <div class="space-y-3">
            <h3 class="text-lg font-semibold text-gray-800 dark:text-white mb-3">联系方式</h3>
            <div v-if="siteInfo.contactEmail" class="flex items-center space-x-2">
              <el-icon class="text-blue-500"><Message /></el-icon>
              <span class="text-gray-600 dark:text-gray-300">邮箱：{{ siteInfo.contactEmail }}</span>
            </div>
            <div v-if="siteInfo.contactPhone" class="flex items-center space-x-2">
              <el-icon class="text-green-500"><Phone /></el-icon>
              <span class="text-gray-600 dark:text-gray-300">电话：{{ siteInfo.contactPhone }}</span>
            </div>
            <div v-if="siteInfo.contactQq" class="flex items-center space-x-2">
              <el-icon class="text-blue-600"><ChatDotRound /></el-icon>
              <span class="text-gray-600 dark:text-gray-300">QQ：{{ siteInfo.contactQq }}</span>
            </div>
            <div v-if="siteInfo.contactWechat" class="flex items-center space-x-2">
              <el-icon class="text-green-600"><ChatDotRound /></el-icon>
              <span class="text-gray-600 dark:text-gray-300">微信：{{ siteInfo.contactWechat }}</span>
            </div>
            <div v-if="siteInfo.contactAddress" class="flex items-center space-x-2">
              <el-icon class="text-red-500"><Location /></el-icon>
              <span class="text-gray-600 dark:text-gray-300">地址：{{ siteInfo.contactAddress }}</span>
            </div>
          </div>

          <!-- 社交链接 -->
          <div class="space-y-3">
            <h3 class="text-lg font-semibold text-gray-800 dark:text-white mb-3">社交链接</h3>
            <div v-if="siteInfo.githubUrl" class="flex items-center space-x-2">
              <el-icon class="text-gray-800 dark:text-white"><Link /></el-icon>
              <a :href="siteInfo.githubUrl" target="_blank" class="text-blue-500 hover:text-blue-600">GitHub</a>
            </div>
            <div v-if="siteInfo.giteeUrl" class="flex items-center space-x-2">
              <el-icon class="text-red-500"><Link /></el-icon>
              <a :href="siteInfo.giteeUrl" target="_blank" class="text-blue-500 hover:text-blue-600">Gitee</a>
            </div>
          </div>
        </div>

        <!-- 备案信息 -->
        <div v-if="siteInfo.icpNumber || siteInfo.policeNumber" class="mt-6 pt-4 border-t border-gray-200 dark:border-gray-700">
          <h3 class="text-lg font-semibold text-gray-800 dark:text-white mb-3">备案信息</h3>
          <div class="space-y-2">
            <div v-if="siteInfo.icpNumber" class="text-gray-600 dark:text-gray-300">
              ICP备案号：{{ siteInfo.icpNumber }}
            </div>
            <div v-if="siteInfo.policeNumber" class="text-gray-600 dark:text-gray-300">
              公安备案号：{{ siteInfo.policeNumber }}
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 公告板 -->
    <div class="bg-white dark:bg-gray-800 rounded-lg shadow-md p-6">
      <div class="flex items-center justify-between mb-6">
        <h2 class="text-xl font-bold text-gray-800 dark:text-white">公告板</h2>
        <el-button v-if="userStore.isAdmin" type="primary" @click="showAddAnnouncement = true">
          <el-icon><Plus /></el-icon>
          添加公告
        </el-button>
      </div>

      <div v-if="announcements.length === 0" class="text-center py-8 text-gray-500">
        暂无公告
      </div>

      <div v-else class="space-y-4">
        <div
          v-for="announcement in announcements"
          :key="announcement.id"
          class="relative bg-gray-50 dark:bg-gray-700 rounded-lg p-4 border-l-4"
          :class="{
            'border-blue-500': announcement.type === 'info',
            'border-green-500': announcement.type === 'success',
            'border-yellow-500': announcement.type === 'warning',
            'border-red-500': announcement.type === 'error'
          }"
        >
          <div class="flex items-start justify-between">
            <div class="flex-1">
              <div class="flex items-center space-x-2 mb-2">
                <h3 class="font-semibold text-gray-800 dark:text-white">{{ announcement.title }}</h3>
                <el-tag v-if="announcement.isPinned" type="warning" size="small">置顶</el-tag>
                <el-tag
                  :type="announcement.type === 'info' ? 'primary' : announcement.type"
                  size="small"
                >
                  {{ getAnnouncementTypeText(announcement.type) }}
                </el-tag>
              </div>
              <div class="text-gray-600 dark:text-gray-300 whitespace-pre-wrap">{{ announcement.content }}</div>
              <div class="text-xs text-gray-400 mt-2">{{ announcement.createdAt }}</div>
            </div>
            <div v-if="userStore.isAdmin" class="flex space-x-2 ml-4">
              <el-button type="primary" link size="small" @click="editAnnouncement(announcement)">
                <el-icon><Edit /></el-icon>
              </el-button>
              <el-button type="danger" link size="small" @click="deleteAnnouncementConfirm(announcement.id)">
                <el-icon><Delete /></el-icon>
              </el-button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 编辑网站信息对话框 -->
    <el-dialog v-model="showEditSiteInfo" title="编辑网站信息" width="600px">
      <el-form :model="siteInfoForm" label-width="100px" class="space-y-4">
        <el-form-item label="网站名称">
          <el-input v-model="siteInfoForm.siteName" />
        </el-form-item>
        <el-form-item label="网站描述">
          <el-input v-model="siteInfoForm.siteDescription" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="关键词">
          <el-input v-model="siteInfoForm.siteKeywords" />
        </el-form-item>
        <el-form-item label="联系邮箱">
          <el-input v-model="siteInfoForm.contactEmail" />
        </el-form-item>
        <el-form-item label="联系电话">
          <el-input v-model="siteInfoForm.contactPhone" />
        </el-form-item>
        <el-form-item label="联系地址">
          <el-input v-model="siteInfoForm.contactAddress" />
        </el-form-item>
        <el-form-item label="QQ">
          <el-input v-model="siteInfoForm.contactQq" />
        </el-form-item>
        <el-form-item label="微信">
          <el-input v-model="siteInfoForm.contactWechat" />
        </el-form-item>
        <el-form-item label="GitHub">
          <el-input v-model="siteInfoForm.githubUrl" />
        </el-form-item>
        <el-form-item label="Gitee">
          <el-input v-model="siteInfoForm.giteeUrl" />
        </el-form-item>
        <el-form-item label="ICP备案号">
          <el-input v-model="siteInfoForm.icpNumber" />
        </el-form-item>
        <el-form-item label="公安备案号">
          <el-input v-model="siteInfoForm.policeNumber" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showEditSiteInfo = false">取消</el-button>
        <el-button type="primary" @click="saveSiteInfo" :loading="saving">保存</el-button>
      </template>
    </el-dialog>

    <!-- 添加/编辑公告对话框 -->
    <el-dialog v-model="showAddAnnouncement" :title="editingAnnouncement ? '编辑公告' : '添加公告'" width="600px">
      <el-form :model="announcementForm" label-width="80px" class="space-y-4">
        <el-form-item label="标题" required>
          <el-input v-model="announcementForm.title" />
        </el-form-item>
        <el-form-item label="内容" required>
          <el-input v-model="announcementForm.content" type="textarea" :rows="5" />
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="announcementForm.type">
            <el-option label="信息" value="info" />
            <el-option label="成功" value="success" />
            <el-option label="警告" value="warning" />
            <el-option label="错误" value="error" />
          </el-select>
        </el-form-item>
        <el-form-item label="置顶">
          <el-switch v-model="announcementForm.isPinned" />
        </el-form-item>
        <el-form-item label="启用">
          <el-switch v-model="announcementForm.isActive" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="announcementForm.sortOrder" :min="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAddAnnouncement = false">取消</el-button>
        <el-button type="primary" @click="saveAnnouncement" :loading="saving">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useUserStore } from '@/stores/user'
import { getSiteInfo, updateSiteInfo, getAnnouncements, getAllAnnouncements, createAnnouncement, updateAnnouncement, deleteAnnouncement } from '@/api/blog'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Edit, Plus, Delete, Message, Phone, ChatDotRound, Location, Link } from '@element-plus/icons-vue'

const userStore = useUserStore()

const siteInfo = ref(null)
const announcements = ref([])
const showEditSiteInfo = ref(false)
const showAddAnnouncement = ref(false)
const saving = ref(false)
const editingAnnouncement = ref(null)

const siteInfoForm = ref({
  siteName: '',
  siteDescription: '',
  siteKeywords: '',
  contactEmail: '',
  contactPhone: '',
  contactAddress: '',
  contactQq: '',
  contactWechat: '',
  githubUrl: '',
  giteeUrl: '',
  icpNumber: '',
  policeNumber: ''
})

const announcementForm = ref({
  title: '',
  content: '',
  type: 'info',
  isPinned: false,
  isActive: true,
  sortOrder: 0
})

const loadSiteInfo = async () => {
  try {
    const res = await getSiteInfo()
    if (res.success) {
      siteInfo.value = res.data
      if (res.data) {
        Object.keys(siteInfoForm.value).forEach(key => {
          siteInfoForm.value[key] = res.data[key] || ''
        })
      }
    }
  } catch (error) {
    console.error('加载网站信息失败:', error)
  }
}

const loadAnnouncements = async () => {
  try {
    const res = userStore.isAdmin ? await getAllAnnouncements() : await getAnnouncements()
    if (res.success) {
      announcements.value = res.data || []
    }
  } catch (error) {
    console.error('加载公告失败:', error)
  }
}

const saveSiteInfo = async () => {
  saving.value = true
  try {
    const res = await updateSiteInfo(siteInfoForm.value)
    if (res.success) {
      siteInfo.value = res.data
      showEditSiteInfo.value = false
      ElMessage.success('网站信息更新成功')
    }
  } catch (error) {
    ElMessage.error('更新失败')
  } finally {
    saving.value = false
  }
}

const saveAnnouncement = async () => {
  if (!announcementForm.value.title || !announcementForm.value.content) {
    ElMessage.warning('请填写标题和内容')
    return
  }

  saving.value = true
  try {
    let res
    if (editingAnnouncement.value) {
      res = await updateAnnouncement(editingAnnouncement.value.id, announcementForm.value)
      if (res.success) {
        ElMessage.success('公告更新成功')
      }
    } else {
      res = await createAnnouncement(announcementForm.value)
      if (res.success) {
        ElMessage.success('公告添加成功')
      }
    }
    showAddAnnouncement.value = false
    resetAnnouncementForm()
    loadAnnouncements()
  } catch (error) {
    ElMessage.error('操作失败')
  } finally {
    saving.value = false
  }
}

const editAnnouncement = (announcement) => {
  editingAnnouncement.value = announcement
  Object.keys(announcementForm.value).forEach(key => {
    announcementForm.value[key] = announcement[key]
  })
  showAddAnnouncement.value = true
}

const deleteAnnouncementConfirm = async (id) => {
  try {
    await ElMessageBox.confirm('确定要删除这条公告吗？', '确认删除', {
      type: 'warning'
    })
    const res = await deleteAnnouncement(id)
    if (res.success) {
      ElMessage.success('删除成功')
      loadAnnouncements()
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

const resetAnnouncementForm = () => {
  editingAnnouncement.value = null
  announcementForm.value = {
    title: '',
    content: '',
    type: 'info',
    isPinned: false,
    isActive: true,
    sortOrder: 0
  }
}

const getAnnouncementTypeText = (type) => {
  const typeMap = {
    info: '信息',
    success: '成功',
    warning: '警告',
    error: '错误'
  }
  return typeMap[type] || '信息'
}

onMounted(() => {
  loadSiteInfo()
  loadAnnouncements()
})
</script>