<template>
  <div class="max-w-4xl mx-auto">
    <!-- 用户信息卡片 -->
    <div class="glass rounded-xl p-6 mb-6">
      <div class="flex items-center gap-4 mb-4">
        <el-avatar :src="profile.user?.avatar" :size="80">{{ profile.user?.username?.[0] }}</el-avatar>
        <div class="flex-1">
          <h2 class="text-xl font-bold dark:text-white">{{ profile.user?.nickname || profile.user?.username }}</h2>
          <p class="text-sm text-gray-400">账号: {{ profile.user?.username }}</p>
          <p class="text-gray-500 mt-1">{{ profile.user?.bio || '暂无简介' }}</p>
          <div class="flex gap-4 mt-2 text-sm text-gray-600 dark:text-gray-400">
            <span>粉丝 {{ profile.followerCount }}</span>
            <span>关注 {{ profile.followingCount }}</span>
          </div>
        </div>
        <div v-if="userStore.isLoggedIn && userStore.user?.id === Number(route.params.id)">
          <el-button @click="showEditDialog = true">编辑资料</el-button>
        </div>
        <div v-else-if="userStore.isLoggedIn && userStore.user?.id !== Number(route.params.id)">
          <el-button type="primary" @click="handleFollow">{{ isFollowing ? '取消关注' : '关注' }}</el-button>
        </div>
      </div>
    </div>

    <!-- 用户文章列表 -->
    <div class="glass rounded-xl p-6">
      <h3 class="text-lg font-semibold mb-4 dark:text-white">文章列表</h3>
      <div class="space-y-4">
        <div v-for="article in articles" :key="article.id" class="p-4 border-b border-gray-200 dark:border-gray-700 cursor-pointer hover:bg-gray-50 dark:hover:bg-gray-800 rounded" @click="router.push(`/article/${article.id}`)">
          <h4 class="font-medium dark:text-white">{{ article.title }}</h4>
          <div class="flex gap-4 mt-2 text-sm text-gray-500">
            <span>{{ article.createdAt }}</span>
            <span><el-icon><View /></el-icon> {{ article.viewCount }}</span>
          </div>
        </div>
        <div v-if="!articles.length" class="text-center py-8 text-gray-500">暂无文章</div>
      </div>
    </div>

    <!-- 编辑资料弹窗 -->
    <el-dialog v-model="showEditDialog" title="编辑资料" width="400px">
      <el-form :model="editForm" label-width="80px">
        <el-form-item label="头像">
          <div class="flex items-center gap-4">
            <el-avatar :src="editForm.avatar" :size="60">{{ profile.user?.username?.[0] }}</el-avatar>
            <el-upload :show-file-list="false" :before-upload="handleAvatarUpload" accept="image/*">
              <el-button size="small">上传头像</el-button>
            </el-upload>
          </div>
        </el-form-item>
        <el-form-item label="昵称">
          <el-input v-model="editForm.nickname" placeholder="设置你的昵称" />
        </el-form-item>
        <el-form-item label="个人简介">
          <el-input v-model="editForm.bio" type="textarea" :autosize="{ minRows: 2, maxRows: 6 }" placeholder="介绍一下自己" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showEditDialog = false">取消</el-button>
        <el-button type="primary" @click="handleSaveProfile">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { getUser, getArticles, followUser, unfollowUser, updateUser, uploadFile } from '@/api/blog'
import { View } from '@element-plus/icons-vue'
import toast from '@/utils/toast'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const profile = ref({})
const articles = ref([])
const isFollowing = ref(false)
const showEditDialog = ref(false)
const editForm = ref({ avatar: '', nickname: '', bio: '' })

const handleFollow = async () => {
  if (isFollowing.value) {
    await unfollowUser(route.params.id)
    isFollowing.value = false
    profile.value.followerCount--
    toast('已取消关注')
  } else {
    await followUser(route.params.id)
    isFollowing.value = true
    profile.value.followerCount++
    toast('关注成功')
  }
}

const handleSaveProfile = async () => {
  const res = await updateUser(route.params.id, editForm.value)
  if (res.success) {
    profile.value.user = res.data
    userStore.user.avatar = res.data.avatar
    userStore.user.nickname = res.data.nickname
    userStore.user.bio = res.data.bio
    localStorage.setItem('user', JSON.stringify(userStore.user))
    showEditDialog.value = false
    toast('保存成功')
  }
}

const handleAvatarUpload = async (file) => {
  const res = await uploadFile(file)
  if (res.success) {
    editForm.value.avatar = res.data.url
    toast('上传成功')
  }
  return false
}

onMounted(async () => {
  const [userRes, articlesRes] = await Promise.all([
    getUser(route.params.id),
    getArticles({ userId: route.params.id })
  ])
  if (userRes.success) {
    profile.value = userRes.data
    editForm.value = { avatar: userRes.data.user?.avatar || '', nickname: userRes.data.user?.nickname || '', bio: userRes.data.user?.bio || '' }
  }
  if (articlesRes.success) articles.value = articlesRes.data.data
})
</script>
