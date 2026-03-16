<template>
  <div class="profile-page">
    <!-- ===== 顶部封面 + 头像区 ===== -->
    <div class="profile-hero">
      <div class="hero-bg" />
      <!-- 头像 -->
      <div class="hero-avatar-wrap" :class="{ online: !!profile.user?.isOnline }">
        <el-avatar :src="profile.user?.avatar" :size="isMobile ? 80 : 96">
          {{ profile.user?.username?.[0] }}
        </el-avatar>
      </div>
    </div>

    <!-- ===== 用户信息区 ===== -->
    <div class="profile-info">
      <!-- 名字 + 设置按钮（同行，设置仅自己可见） -->
      <div class="profile-name-row">
        <div>
          <div class="profile-name">{{ profile.user?.nickname || profile.user?.username }}</div>
          <div class="profile-username">@{{ profile.user?.username }}</div>
        </div>
        <!-- 设置按钮：仅自己，放在名字右侧 -->
        <div v-if="isSelf" class="settings-wrap">
          <button class="settings-btn" @click="showSettingsMenu = !showSettingsMenu" aria-label="设置">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <circle cx="12" cy="12" r="3"/><path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1-2.83 2.83l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-4 0v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83-2.83l.06-.06A1.65 1.65 0 0 0 4.68 15a1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1 0-4h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 2.83-2.83l.06.06A1.65 1.65 0 0 0 9 4.68a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 4 0v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 2.83l-.06.06A1.65 1.65 0 0 0 19.4 9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 0 4h-.09a1.65 1.65 0 0 0-1.51 1z"/>
            </svg>
          </button>
          <Transition name="fade-down">
            <div v-if="showSettingsMenu" class="settings-menu" v-click-outside="() => showSettingsMenu = false">
              <button class="settings-item" @click="goTo('/about')">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>
                关于我们
              </button>
              <button class="settings-item" @click="goTo('/album')">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="3" width="18" height="18" rx="2"/><circle cx="8.5" cy="8.5" r="1.5"/><polyline points="21 15 16 10 5 21"/></svg>
                我的相册
              </button>
              <button v-if="userStore.isAdmin" class="settings-item" @click="goTo('/admin')">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 20h9"/><path d="M16.5 3.5a2.121 2.121 0 0 1 3 3L7 19l-4 1 1-4L16.5 3.5z"/></svg>
                管理后台
              </button>
              <div class="settings-divider" />
              <button class="settings-item settings-item--danger" @click="handleLogout">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/><polyline points="16 17 21 12 16 7"/><line x1="21" y1="12" x2="9" y2="12"/></svg>
                退出登录
              </button>
            </div>
          </Transition>
        </div>
      </div>

      <p class="profile-bio">{{ profile.user?.bio || '这个人很懒，什么都没写~' }}</p>

      <!-- 数据统计 + 编辑资料（同行） -->
      <div class="profile-stats-row">
        <div class="profile-stats">
          <div class="stat-item">
            <span class="stat-num">{{ profile.followingCount }}</span>
            <span class="stat-label">关注</span>
          </div>
          <div class="stat-divider" />
          <div class="stat-item">
            <span class="stat-num">{{ profile.followerCount }}</span>
            <span class="stat-label">粉丝</span>
          </div>
          <div class="stat-divider" />
          <div class="stat-item">
            <span class="stat-num">{{ articles.length }}</span>
            <span class="stat-label">文章</span>
          </div>
        </div>

        <!-- 自己：编辑资料放右侧 -->
        <template v-if="isSelf">
          <button class="btn-edit" @click="showEditDialog = true">编辑资料</button>
        </template>
        <!-- 他人：关注 + 私信 -->
        <template v-else-if="userStore.isLoggedIn">
          <div class="profile-btns">
            <button class="btn-follow" :class="{ 'btn-follow--active': isFollowing }" @click="handleFollow">
              {{ isFollowing ? '已关注' : '+ 关注' }}
            </button>
            <button class="btn-msg" @click="router.push(`/chat?userId=${route.params.id}`)">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/></svg>
              私信
            </button>
          </div>
        </template>
        <template v-else>
          <button class="btn-follow" @click="router.push('/login')">+ 关注</button>
        </template>
      </div>
    </div>

    <!-- ===== 文章列表 ===== -->
    <div class="profile-articles">
      <div class="articles-header">
        <span class="articles-title">TA 的文章</span>
      </div>
      <div v-if="!articles.length" class="articles-empty">
        <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="#d0d5dd" stroke-width="1.2" stroke-linecap="round" stroke-linejoin="round"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/></svg>
        <p>暂无文章</p>
      </div>
      <div v-else class="articles-list">
        <div v-for="article in articles" :key="article.id"
             class="article-card"
             @click="router.push(`/article/${article.id}`)">
          <div class="article-card-body">
            <div class="article-title">{{ article.title }}</div>
            <div class="article-meta">
              <span>{{ article.createdAt }}</span>
              <span class="article-views">
                <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/></svg>
                {{ article.viewCount }}
              </span>
            </div>
          </div>
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#c0c4cc" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="9 18 15 12 9 6"/></svg>
        </div>
      </div>
    </div>

    <!-- ===== 编辑资料弹窗 ===== -->
    <el-dialog v-model="showEditDialog" title="编辑资料" :width="isMobile ? '92%' : '400px'">
      <el-form ref="formRef" :model="editForm" :rules="rules" label-width="80px">
        <el-form-item label="头像">
          <div class="flex items-center gap-4">
            <el-avatar :src="editForm.avatar" :size="60">{{ profile.user?.username?.[0] }}</el-avatar>
            <el-upload :show-file-list="false" :before-upload="handleAvatarUpload" accept="image/*">
              <el-button size="small">上传头像</el-button>
            </el-upload>
          </div>
        </el-form-item>
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="editForm.nickname" placeholder="设置你的昵称" />
        </el-form-item>
        <el-form-item label="个人简介" prop="bio">
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
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { getUser, getArticles, followUser, unfollowUser, updateUser, uploadFile } from '@/api/blog'
import toast from '@/utils/toast'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const profile = ref({ user: null, followerCount: 0, followingCount: 0 })
const isMobile = ref(window.innerWidth < 768)
const articles = ref([])
const isFollowing = ref(false)
const showEditDialog = ref(false)
const showSettingsMenu = ref(false)
const editForm = ref({ avatar: '', nickname: '', bio: '' })
const formRef = ref(null)
let onlineTimer = null

const isSelf = computed(() => userStore.isLoggedIn && userStore.user?.id === Number(route.params.id))

const rules = {
  nickname: [
    { required: true, message: '请输入昵称', trigger: 'blur' },
    { min: 2, max: 50, message: '昵称长度 2~50 字符', trigger: 'blur' }
  ],
  bio: [{ max: 200, message: '简介不超过 200 字', trigger: 'blur' }]
}

// 自定义指令：点击外部关闭
const vClickOutside = {
  mounted(el, binding) {
    el._clickOutside = (e) => { if (!el.contains(e.target)) binding.value(e) }
    document.addEventListener('click', el._clickOutside)
  },
  unmounted(el) { document.removeEventListener('click', el._clickOutside) }
}

const goTo = (path) => { showSettingsMenu.value = false; router.push(path) }

const handleLogout = () => {
  showSettingsMenu.value = false
  userStore.logout()
  ElMessage.success('已退出登录')
  router.push('/')
}

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
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
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
  })
}

const handleAvatarUpload = async (file) => {
  const res = await uploadFile(file)
  if (res.success) { editForm.value.avatar = res.data.url; toast('上传成功') }
  return false
}

const loadProfileAndArticles = async () => {
  const [userRes, articlesRes] = await Promise.all([
    getUser(route.params.id),
    getArticles({ userId: route.params.id })
  ])
  if (userRes.success) {
    const user = userRes.data
    profile.value = { user, followerCount: user?.followerCount ?? 0, followingCount: user?.followingCount ?? 0 }
    isFollowing.value = !!user?.isFollowing
    editForm.value = { avatar: user?.avatar || '', nickname: user?.nickname || '', bio: user?.bio || '' }
  }
  if (articlesRes.success) articles.value = articlesRes.data.data
}

const refreshOnlineStatus = async () => {
  const userRes = await getUser(route.params.id)
  if (userRes.success && profile.value.user) profile.value.user.isOnline = !!userRes.data?.isOnline
}

const handleResize = () => { isMobile.value = window.innerWidth < 768 }

onMounted(async () => {
  await loadProfileAndArticles()
  onlineTimer = setInterval(refreshOnlineStatus, 15000)
  window.addEventListener('resize', handleResize)
})
onUnmounted(() => {
  if (onlineTimer) clearInterval(onlineTimer)
  window.removeEventListener('resize', handleResize)
})
</script>

<style scoped>
.profile-page {
  max-width: 640px;
  margin: 0 auto;
  padding-bottom: calc(56px + env(safe-area-inset-bottom) + 16px);
}

/* ===== Hero 区 ===== */
.profile-hero {
  position: relative;
  height: 160px;
  margin: -1rem -1rem 0;
}
@media (min-width: 640px) {
  .profile-hero { margin: -1.5rem -1rem 0; height: 180px; }
}
.hero-bg {
  position: absolute; inset: 0;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 60%, #f093fb 100%);
}

/* 头像 */
.hero-avatar-wrap {
  position: absolute; bottom: -40px; left: 20px;
  border-radius: 50%;
  border: 3px solid #fff;
  box-shadow: 0 4px 16px rgba(0,0,0,0.15);
  background: #fff;
  display: inline-flex;
}
.hero-avatar-wrap::after {
  content: ''; position: absolute; right: 3px; bottom: 3px;
  width: 13px; height: 13px; border-radius: 50%;
  border: 2px solid #fff; background: #94a3b8; box-sizing: border-box;
}
.hero-avatar-wrap.online::after { background: #22c55e; }

/* ===== 信息区 ===== */
.profile-info { padding: 52px 20px 20px; }

/* 名字行：左侧名字+账号，右侧设置按钮 */
.profile-name-row {
  display: flex; align-items: flex-start; justify-content: space-between;
  margin-bottom: 8px;
}
.profile-name {
  font-size: 20px; font-weight: 700; color: #1a1a2e; margin-bottom: 2px;
}
.dark .profile-name { color: #f0f0f0; }
.profile-username { font-size: 13px; color: #909399; }

/* 设置按钮（名字右侧） */
.settings-wrap { position: relative; flex-shrink: 0; margin-top: 2px; }
.settings-btn {
  width: 36px; height: 36px; border-radius: 50%; border: none;
  background: rgba(0,0,0,0.06); color: #606266;
  display: flex; align-items: center; justify-content: center;
  cursor: pointer; -webkit-tap-highlight-color: transparent;
  transition: background 0.15s;
}
.settings-btn:active { background: rgba(0,0,0,0.12); }
.dark .settings-btn { background: rgba(255,255,255,0.1); color: #ccc; }

/* 设置下拉菜单 */
.settings-menu {
  position: absolute; top: calc(100% + 6px); right: 0;
  min-width: 160px;
  background: rgba(255,255,255,0.97);
  backdrop-filter: blur(20px);
  border-radius: 14px;
  box-shadow: 0 8px 32px rgba(0,0,0,0.18);
  overflow: hidden;
  z-index: 100;
}
.settings-item {
  display: flex; align-items: center; gap: 10px;
  width: 100%; padding: 13px 16px;
  border: none; background: transparent;
  font-size: 14px; font-weight: 500; color: #1a1a2e;
  cursor: pointer; text-align: left;
  -webkit-tap-highlight-color: transparent;
  transition: background 0.15s;
}
.settings-item:active { background: rgba(0,0,0,0.05); }
.settings-item--danger { color: #f5576c; }
.settings-divider { height: 1px; background: rgba(0,0,0,0.06); margin: 2px 0; }

.profile-bio {
  font-size: 14px; color: #606266; line-height: 1.6; margin-bottom: 16px;
}
.dark .profile-bio { color: #aaa; }

/* 统计 + 按钮同行 */
.profile-stats-row {
  display: flex; align-items: center; justify-content: space-between; gap: 12px;
}
.profile-stats { display: flex; align-items: center; flex: 1; }
.stat-item {
  display: flex; flex-direction: column; align-items: center;
  padding: 0 20px 0 0;
}
.stat-item:first-child { padding-left: 0; }
.stat-num { font-size: 18px; font-weight: 700; color: #1a1a2e; line-height: 1.2; }
.dark .stat-num { color: #f0f0f0; }
.stat-label { font-size: 12px; color: #909399; margin-top: 2px; }
.stat-divider { width: 1px; height: 28px; background: rgba(0,0,0,0.1); margin-right: 20px; }
.dark .stat-divider { background: rgba(255,255,255,0.1); }

/* 按钮 */
.profile-btns { display: flex; gap: 8px; flex-shrink: 0; }
.btn-edit {
  padding: 8px 18px; border-radius: 20px; flex-shrink: 0;
  border: 1.5px solid #d0d5dd; background: transparent;
  font-size: 13px; font-weight: 600; color: #1a1a2e;
  cursor: pointer; -webkit-tap-highlight-color: transparent;
  transition: background 0.15s; white-space: nowrap;
}
.btn-edit:active { background: rgba(0,0,0,0.05); }
.dark .btn-edit { border-color: #444; color: #f0f0f0; }

.btn-follow {
  padding: 8px 20px; border-radius: 20px; border: none; flex-shrink: 0;
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: #fff; font-size: 13px; font-weight: 600;
  cursor: pointer; -webkit-tap-highlight-color: transparent;
  transition: opacity 0.15s; white-space: nowrap;
}
.btn-follow--active {
  background: transparent; border: 1.5px solid #d0d5dd; color: #909399;
}
.btn-follow:active { opacity: 0.8; }

.btn-msg {
  display: flex; align-items: center; gap: 5px; flex-shrink: 0;
  padding: 8px 16px; border-radius: 20px;
  border: 1.5px solid #d0d5dd; background: transparent;
  font-size: 13px; font-weight: 600; color: #1a1a2e;
  cursor: pointer; -webkit-tap-highlight-color: transparent;
  transition: background 0.15s; white-space: nowrap;
}
.btn-msg:active { background: rgba(0,0,0,0.05); }
.dark .btn-msg { border-color: #444; color: #f0f0f0; }

/* ===== 文章列表 ===== */
.profile-articles {
  margin: 0 0 16px;
  background: rgba(255,255,255,0.7);
  backdrop-filter: blur(8px);
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 2px 12px rgba(0,0,0,0.06);
}
.dark .profile-articles { background: rgba(30,30,40,0.7); }

.articles-header {
  padding: 16px 20px 12px;
  border-bottom: 1px solid rgba(0,0,0,0.06);
}
.articles-title { font-size: 15px; font-weight: 700; color: #1a1a2e; }
.dark .articles-title { color: #f0f0f0; }

.articles-empty {
  display: flex; flex-direction: column; align-items: center; gap: 10px;
  padding: 40px 0; color: #c0c4cc; font-size: 14px;
}

.articles-list { padding: 4px 0; }
.article-card {
  display: flex; align-items: center; gap: 12px;
  padding: 14px 20px; cursor: pointer;
  -webkit-tap-highlight-color: transparent;
  transition: background 0.15s;
  border-bottom: 1px solid rgba(0,0,0,0.04);
}
.article-card:last-child { border-bottom: none; }
.article-card:active { background: rgba(0,0,0,0.04); }
.article-card-body { flex: 1; overflow: hidden; min-width: 0; }
.article-title {
  font-size: 14px; font-weight: 500; color: #1a1a2e;
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
  margin-bottom: 4px;
}
.dark .article-title { color: #e0e0e0; }
.article-meta {
  display: flex; align-items: center; gap: 12px;
  font-size: 12px; color: #c0c4cc;
}
.article-views { display: flex; align-items: center; gap: 3px; }

/* 动画 */
.fade-down-enter-active { transition: opacity 0.18s ease, transform 0.18s ease; }
.fade-down-leave-active { transition: opacity 0.15s ease, transform 0.15s ease; }
.fade-down-enter-from { opacity: 0; transform: translateY(-8px); }
.fade-down-leave-to { opacity: 0; transform: translateY(-8px); }
</style>
