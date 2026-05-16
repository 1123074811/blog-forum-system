<template>
  <div class="profile-page" :class="{ 'is-mobile': isMobile }">
    <el-skeleton :loading="profileLoading" animated>
      <template #template>
        <div class="skeleton-wrap">
          <el-skeleton-item variant="image" style="width:100%;height:200px;border-radius:16px" />
          <div class="skeleton-content">
            <el-skeleton-item variant="circle" style="width:84px;height:84px" />
            <div class="skeleton-lines">
              <el-skeleton-item variant="h3" style="width:180px" />
              <el-skeleton-item variant="text" style="width:240px" />
              <el-skeleton-item variant="text" style="width:100%" />
            </div>
          </div>
        </div>
      </template>

      <template #default>
        <div v-if="!profile.user" class="profile-empty jp-profile-empty">
          <p>用户信息加载失败，请刷新重试。</p>
          <el-button type="primary" @click="loadPageData">重新加载</el-button>
        </div>

        <template v-else>
          <div class="flex flex-col md:flex-row gap-6 items-start w-full">
            <!-- 古朴风格主页：头像 + 信息（左侧栏） -->
            <div class="vintage-panel jp-profile-main pt-8 w-full md:w-[320px] shrink-0 flex flex-col items-center text-center">
              <div class="avatar-wrap mb-4" :class="{ online: !!profile.user?.isOnline }">
                <el-avatar :src="avatarUrl" :size="100">{{ profile.user?.username?.[0] }}</el-avatar>
              </div>

              <!-- 信息展示区 -->
              <div class="w-full mt-2">
                <h1 class="display-name text-2xl font-bold font-serif mb-2">{{ displayName }}</h1>
                <div class="user-id-card bg-subtleBlue dark:bg-gray-800/50 p-3 rounded-lg border border-ink/10 mb-5 w-full">
                  <div class="flex items-center justify-between text-xs mb-2">
                    <span class="text-gray-400 font-serif">账号</span>
                    <span class="text-ink dark:text-gray-300 font-mono font-bold">{{ profile.user?.username }}</span>
                  </div>
                  <div class="flex items-center justify-between text-xs">
                    <span class="text-gray-400 font-serif">邮箱</span>
                    <span class="text-ink dark:text-gray-300 font-mono">{{ profile.user?.email || '未公开' }}</span>
                  </div>
                </div>
                
                <p class="profile-bio mb-6 text-sm text-gray-700 dark:text-gray-300 italic">" {{ profile.user?.bio || '这家伙很神秘，还没有留下任何墨宝。' }} "</p>

                <!-- 操作按钮 -->
                <div class="action-area flex flex-col gap-3 mb-6 w-full">
                  <template v-if="isSelf">
                    <el-button color="var(--ink)" class="w-full !m-0 !font-serif !font-bold shadow-[2px_2px_0_var(--accent)] hover:-translate-y-0.5 transition-transform" @click="showEditDialog = true">
                      <el-icon class="mr-1"><Edit /></el-icon>编辑资料
                    </el-button>
                  </template>
                  <template v-else>
                    <div class="flex gap-2 w-full">
                      <el-button 
                        :color="isFollowing ? '#e2e8f0' : 'var(--ink)'"
                        :style="isFollowing ? { color: '#475569' } : { color: 'var(--paper)' }"
                        class="flex-1 !m-0 !font-serif !font-bold shadow-[2px_2px_0_var(--accent)] hover:-translate-y-0.5 transition-transform" 
                        @click="handleFollow"
                      >
                        {{ isFollowing ? '已关注' : '关注' }}
                      </el-button>
                      <el-button class="flex-1 !m-0 !bg-paper !border-2 !border-ink !text-ink !font-serif !font-bold shadow-[2px_2px_0_var(--ink)] hover:!bg-subtleBlue hover:-translate-y-0.5 transition-transform" @click="goChat">
                        <el-icon class="mr-1"><ChatDotRound /></el-icon>私信
                      </el-button>
                    </div>
                  </template>
                </div>

                <!-- 统计数据 -->
                <div class="stats-row justify-center w-full pb-2 border-t border-dashed border-gray-300 pt-5">
                  <div class="stat-item flex flex-col items-center">
                    <span class="stat-num">{{ profile.followingCount }}</span>
                    <span class="stat-label">关注</span>
                  </div>
                  <div class="stat-divider h-10"></div>
                  <div class="stat-item flex flex-col items-center">
                    <span class="stat-num">{{ profile.followerCount }}</span>
                    <span class="stat-label">粉丝</span>
                  </div>
                  <div class="stat-divider h-10"></div>
                  <div class="stat-item flex flex-col items-center">
                    <span class="stat-num">{{ articles.length }}</span>
                    <span class="stat-label">文章</span>
                  </div>
                </div>
              </div>
            </div>

            <!-- 文章列表（右侧栏） -->
            <div class="vintage-panel flex-1 w-full !mt-0 min-w-0">
              <h2 class="text-xl font-bold dark:text-white mb-6 flex items-center gap-2">
                <span class="stamp">文</span>TA 的文章
              </h2>
              <div v-if="articlesLoading" class="list-loading">正在加载文章...</div>
              <div v-else-if="!articles.length" class="list-empty">暂无文章</div>
              <div v-else class="article-list">
                <article
                  v-for="article in articles"
                  :key="article.id"
                  class="article-item"
                  @click="openArticle(article.id)"
                >
                  <div class="article-main">
                    <h3 class="article-title">{{ article.title }}</h3>
                    <p class="article-preview">{{ articlePreview(article.content) }}</p>
                    <div class="article-meta">
                      <span>{{ article.createdAt }}</span>
                      <span>浏览 {{ article.viewCount || 0 }}</span>
                    </div>
                  </div>
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#94a3b8" stroke-width="2">
                    <polyline points="9 18 15 12 9 6" />
                  </svg>
                </article>
              </div>
            </div>
          </div>
        </template>
      </template>
    </el-skeleton>

    <!-- 编辑资料弹窗 -->
    <el-dialog v-model="showEditDialog" title="编辑资料" :width="isMobile ? '92%' : '420px'">
      <el-form ref="formRef" :model="editForm" :rules="rules" label-width="72px">
        <el-form-item label="头像">
          <div class="edit-avatar-row">
            <el-avatar :src="toAvatarThumb(editForm.avatar, 120)" :size="62">{{ profile.user?.username?.[0] }}</el-avatar>
            <el-upload :show-file-list="false" :before-upload="handleAvatarUpload" accept="image/*">
              <el-button size="small" :loading="avatarUploading">上传头像</el-button>
            </el-upload>
          </div>
        </el-form-item>
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="editForm.nickname" placeholder="设置你的昵称" />
        </el-form-item>
        <el-form-item label="简介" prop="bio">
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
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { followUser, getArticles, getUser, unfollowUser, updateUser, uploadFile } from '@/api/blog'
import { normalizeUnsafeUrl, toAvatarThumb } from '@/utils/image'
import toast from '@/utils/toast'
import { Edit, ChatDotRound } from '@element-plus/icons-vue'

const MOBILE_BREAKPOINT = 768

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const profile = ref({ user: null, followerCount: 0, followingCount: 0 })
const articles = ref([])
const isFollowing = ref(false)
const isMobile = ref(window.innerWidth < MOBILE_BREAKPOINT)
const profileLoading = ref(false)
const articlesLoading = ref(false)
const showEditDialog = ref(false)
const avatarUploading = ref(false)
const editForm = ref({ avatar: '', nickname: '', bio: '' })
const formRef = ref(null)

let onlineTimer = null
let loadSerial = 0

const targetUserId = computed(() => {
  const raw = Number(route.params.id)
  return Number.isInteger(raw) && raw > 0 ? raw : null
})

const isSelf = computed(() => {
  if (!userStore.isLoggedIn || !targetUserId.value) return false
  return Number(userStore.user?.id) === targetUserId.value
})

const displayName = computed(() => profile.value.user?.nickname || profile.value.user?.username || '用户')
const avatarUrl = computed(() => toAvatarThumb(profile.value.user?.avatar, isMobile.value ? 96 : 140))

const rules = {
  nickname: [
    { required: true, message: '请输入昵称', trigger: 'blur' },
    { min: 2, max: 50, message: '昵称长度需要在 2~50 个字符', trigger: 'blur' }
  ],
  bio: [{ max: 200, message: '简介不能超过 200 个字符', trigger: 'blur' }]
}

const goChat = () => {
  if (!targetUserId.value) return
  if (!userStore.isLoggedIn) { router.push('/login'); return }
  router.push(`/chat?userId=${targetUserId.value}`)
}

const openArticle = (articleId) => router.push(`/article/${articleId}`)

const articlePreview = (content) => {
  const plain = String(content || '').replace(/[#>*`~\-\[\]()]/g, ' ').replace(/\s+/g, ' ').trim()
  if (!plain) return '暂无摘要'
  return plain.length > 80 ? `${plain.slice(0, 80)}...` : plain
}

const sanitizeAvatarForSubmit = (value) => {
  const raw = String(normalizeUnsafeUrl(value) || '').trim()
  if (!raw) return ''
  try {
    const isAbsolute = /^https?:\/\//i.test(raw)
    const base = typeof window !== 'undefined' ? window.location.origin : 'http://localhost'
    const parsed = new URL(raw, base)
    parsed.searchParams.delete('t')
    if (isAbsolute) return parsed.toString()
    return `${parsed.pathname}${parsed.search}${parsed.hash}`
  } catch {
    return raw
  }
}

const extractArticles = (res) => {
  const payload = res?.data
  if (Array.isArray(payload)) return payload
  if (Array.isArray(payload?.data)) return payload.data
  if (Array.isArray(payload?.records)) return payload.records
  return []
}

const handleResize = () => { isMobile.value = window.innerWidth < MOBILE_BREAKPOINT }

const ensureValidRouteId = async () => {
  if (targetUserId.value) return targetUserId.value
  const selfId = Number(userStore.user?.id)
  if (Number.isInteger(selfId) && selfId > 0) {
    await router.replace(`/user/${selfId}`)
    return selfId
  }
  return null
}

const applyUserProfile = (user) => {
  if (!user) {
    profile.value = { user: null, followerCount: 0, followingCount: 0 }
    isFollowing.value = false
    editForm.value = { avatar: '', nickname: '', bio: '' }
    return
  }
  profile.value = {
    user,
    followerCount: Number(user.followerCount || 0),
    followingCount: Number(user.followingCount || 0)
  }
  isFollowing.value = !!user.isFollowing
  editForm.value = {
    avatar: sanitizeAvatarForSubmit(user.avatar || ''),
    nickname: user.nickname || '',
    bio: user.bio || ''
  }
}

const loadPageData = async () => {
  const userId = await ensureValidRouteId()
  if (!userId) { applyUserProfile(null); articles.value = []; return }

  const currentLoad = ++loadSerial
  profileLoading.value = true
  articlesLoading.value = true

  const [userRes, articlesRes] = await Promise.allSettled([
    getUser(userId),
    getArticles({ userId, page: 1, limit: 30 })
  ])

  if (currentLoad !== loadSerial) return

  if (userRes.status === 'fulfilled' && userRes.value?.success) {
    applyUserProfile(userRes.value.data)
  } else {
    applyUserProfile(null)
    toast('个人信息加载失败')
  }

  articles.value = (articlesRes.status === 'fulfilled' && articlesRes.value?.success)
    ? extractArticles(articlesRes.value)
    : []

  profileLoading.value = false
  articlesLoading.value = false
}

const refreshOnlineStatus = async () => {
  if (!profile.value.user || !targetUserId.value) return
  const res = await getUser(targetUserId.value)
  if (res.success && profile.value.user?.id === targetUserId.value) {
    profile.value.user.isOnline = !!res.data?.isOnline
  }
}

const handleFollow = async () => {
  if (!targetUserId.value || isSelf.value) return
  if (!userStore.isLoggedIn) { router.push('/login'); return }
  try {
    if (isFollowing.value) {
      await unfollowUser(targetUserId.value)
      isFollowing.value = false
      profile.value.followerCount = Math.max(0, Number(profile.value.followerCount || 0) - 1)
      toast('已取消关注')
    } else {
      await followUser(targetUserId.value)
      isFollowing.value = true
      profile.value.followerCount = Number(profile.value.followerCount || 0) + 1
      toast('关注成功')
    }
  } catch {
    toast('操作失败，请稍后重试')
  }
}

const handleAvatarUpload = async (file) => {
  if (!formRef.value || !targetUserId.value || !isSelf.value) return false
  avatarUploading.value = true
  try {
    const res = await uploadFile(file)
    if (res.success && res.data?.url) {
      const newAvatarUrl = res.data.url + (res.data.url.includes('?') ? '&' : '?') + 't=' + Date.now()
      editForm.value.avatar = sanitizeAvatarForSubmit(res.data.url)
      const payload = {
        avatar: sanitizeAvatarForSubmit(res.data.url)
      }
      if (editForm.value.nickname) {
        payload.nickname = editForm.value.nickname
      }
      if (editForm.value.bio) {
        payload.bio = editForm.value.bio
      }
      const saveRes = await updateUser(targetUserId.value, payload)
      if (saveRes.success) {
        const latestRes = await getUser(targetUserId.value)
        const sourceUser = latestRes?.success && latestRes?.data ? latestRes.data : saveRes.data
        if (!sourceUser) {
          toast('头像保存失败')
          return false
        }
        const updatedUser = { ...sourceUser, avatar: newAvatarUrl }
        applyUserProfile(updatedUser)
        if (userStore.user) {
          userStore.user.avatar = sourceUser.avatar
          userStore.user.nickname = sourceUser.nickname
          userStore.user.bio = sourceUser.bio
          localStorage.setItem('user', JSON.stringify(userStore.user))
        }
        toast('头像已更新')
      } else {
        toast('头像保存失败')
      }
    } else {
      toast('头像上传失败')
    }
  } catch (err) {
    console.error('头像上传失败:', err)
    toast(err?.response?.data?.message || '操作失败，请稍后重试')
  } finally {
    avatarUploading.value = false
  }
  return false
}

const handleSaveProfile = async () => {
  if (!formRef.value || !targetUserId.value || !isSelf.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  try {
    const updateData = { ...editForm.value }
    if (updateData.avatar) {
      updateData.avatar = sanitizeAvatarForSubmit(updateData.avatar)
    }
    if (!updateData.avatar) {
      delete updateData.avatar
    }
    const res = await updateUser(targetUserId.value, updateData)
    if (!res.success) { toast(res.message || '保存失败，请稍后重试'); return }
    const latestRes = await getUser(targetUserId.value)
    const sourceUser = latestRes?.success && latestRes?.data ? latestRes.data : res.data
    if (!sourceUser) { toast('保存失败，请稍后重试'); return }
    let updatedUser = sourceUser
    if (updatedUser.avatar) {
      updatedUser = { ...updatedUser, avatar: updatedUser.avatar + (updatedUser.avatar.includes('?') ? '&' : '?') + 't=' + Date.now() }
    }
    applyUserProfile(updatedUser)
    if (userStore.user) {
      userStore.user.avatar = sourceUser.avatar
      userStore.user.nickname = sourceUser.nickname
      userStore.user.bio = sourceUser.bio
      localStorage.setItem('user', JSON.stringify(userStore.user))
    }
    showEditDialog.value = false
    toast('资料已更新')
  } catch (err) {
    console.error('资料保存失败:', err)
    toast(err?.response?.data?.message || '保存失败，请稍后重试')
  }
}

watch(() => route.params.id, () => loadPageData(), { immediate: true })

watch(() => userStore.user?.id, (id) => {
  const safeId = Number(id)
  if (!targetUserId.value && Number.isInteger(safeId) && safeId > 0) {
    router.replace(`/user/${safeId}`)
  }
})

onMounted(() => {
  window.addEventListener('resize', handleResize)
  onlineTimer = setInterval(() => refreshOnlineStatus().catch(() => {}), 15000)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  if (onlineTimer) clearInterval(onlineTimer)
})
</script>

<style scoped>
.profile-page {
  width: 100%;
  max-width: 1080px;
  margin: 0 auto;
  padding: 32px 16px calc(56px + env(safe-area-inset-bottom) + 24px);
  display: flex;
  flex-direction: column;
  gap: 32px;
}

/* ── 骨架屏 ── */
.skeleton-wrap { display: flex; flex-direction: column; gap: 16px; }
.skeleton-content { display: flex; align-items: center; gap: 16px; }
.skeleton-lines { flex: 1; display: flex; flex-direction: column; gap: 10px; }

/* ── 空状态 ── */
.profile-empty {
  display: flex; flex-direction: column; gap: 12px;
  align-items: center; justify-content: center;
  padding: 32px;
  border-radius: 18px;
  background: var(--paper);
  border: var(--border-sketch);
  box-shadow: 8px 8px 0px var(--subtle-blue);
}

.dark .profile-empty {
  background: #222;
  border-color: #555;
  box-shadow: 8px 8px 0px #111;
}

/* ── 抖音风格主卡片 ── */
.tiktok-profile {
  background: var(--paper);
  border-radius: 18px;
  border: var(--border-sketch);
  overflow: hidden;
  position: relative;
  box-shadow: 6px 6px 0px var(--subtle-blue);
  transition: transform 0.4s cubic-bezier(0.175, 0.885, 0.32, 1.275), box-shadow 0.4s;
}

.tiktok-profile:hover {
  transform: scale(1.01) rotate(0.8deg);
  box-shadow: 15px 15px 0px var(--subtle-blue);
}

.dark .tiktok-profile {
  background: #222;
  border-color: #555;
  box-shadow: 6px 6px 0px #111;
}

.dark .tiktok-profile:hover {
  box-shadow: 15px 15px 0px #111;
}

.cover-bg {
  height: 160px;
  background: linear-gradient(135deg, #0ea5e9 0%, #3b82f6 50%, #8b5cf6 100%);
  position: relative;
}

.cover-gradient {
  position: absolute;
  inset: 0;
  background: linear-gradient(to bottom, transparent 40%, rgba(0,0,0,0.18) 100%);
}

/* ── 头像 + 操作按钮行 ── */
.profile-header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  padding: 0 20px;
  margin-top: -44px;
  position: relative;
  z-index: 1;
}

.avatar-wrap {
  width: 106px;
  height: 106px;
  min-width: 106px;
  min-height: 106px;
  display: flex !important;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  border: 3px solid #fff;
  background: #fff;
  box-shadow: 0 8px 25px rgba(15, 23, 42, 0.2);
  position: relative;
  flex-shrink: 0;
  overflow: visible;
  margin-left: auto;
  margin-right: auto;
}

.dark .avatar-wrap {
  border-color: #333;
  background: #333;
}

.avatar-wrap::after {
  content: '';
  position: absolute;
  right: 4px; bottom: 5px;
  width: 12px; height: 12px;
  border-radius: 999px;
  border: 2px solid #fff;
  background: #94a3b8;
}

.avatar-wrap.online::after { background: #22c55e; }

/* ── 操作按钮 ── */
.action-area {
  display: flex;
  gap: 8px;
  padding-bottom: 6px;
}

.btn-edit,
.btn-follow,
.btn-message {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  border: none;
  cursor: pointer;
  border-radius: 999px;
  height: 36px;
  padding: 0 16px;
  font-size: 13px;
  font-weight: 600;
  transition: all 0.2s ease;
  white-space: nowrap;
}

.btn-edit {
  background: rgba(255,255,255,0.92);
  color: #334155;
  border: 1px solid #e2e8f0;
}

.btn-edit:hover { background: #f1f5f9; }

.btn-follow {
  background: linear-gradient(135deg, #0ea5e9, #3b82f6);
  color: #fff;
}

.btn-follow.followed {
  background: #e2e8f0;
  color: #475569;
}

.btn-follow:hover:not(.followed) { opacity: 0.88; }

.btn-message {
  background: rgba(255,255,255,0.92);
  color: #334155;
  border: 1px solid #e2e8f0;
}

.btn-message:hover { background: #f1f5f9; }

/* ── 用户信息 ── */
.profile-info {
  padding: 14px 20px 20px;
}

.display-name {
  margin: 0;
  font-size: 22px;
  font-weight: 700;
  color: var(--ink);
  line-height: 1.3;
}

.username {
  margin: 4px 0 0;
  color: var(--text-secondary);
  font-size: 13px;
}

.profile-bio {
  margin: 10px 0 0;
  color: var(--text-primary);
  font-size: 14px;
  line-height: 1.65;
}

/* ── 统计数据 ── */
.stats-row {
  display: flex;
  align-items: center;
  gap: 0;
  margin-top: 16px;
}

.stat-item {
  display: flex;
  align-items: baseline;
  gap: 4px;
  cursor: default;
}

.stat-num {
  font-size: 18px;
  font-weight: 700;
  color: var(--ink);
}

.stat-label {
  font-size: 13px;
  color: #64748b;
}

.stat-divider {
  width: 1px;
  height: 16px;
  background: #e2e8f0;
  margin: 0 16px;
}
.dark .stat-divider {
  background: #444;
}

/* ── 文章列表 ── */
.article-board {
  border-radius: 18px;
  padding: 16px;
  background: var(--paper);
  border: var(--border-sketch);
  box-shadow: 6px 6px 0px var(--subtle-blue);
}

.dark .article-board {
  background: #222;
  border-color: #555;
  box-shadow: 6px 6px 0px #111;
}

.section-title {
  font-size: 16px;
  font-weight: 700;
  color: #0f172a;
  margin-bottom: 12px;
  display: flex;
  align-items: center;
}

.list-loading,
.list-empty {
  min-height: 96px;
  display: grid;
  place-items: center;
  color: #64748b;
  font-size: 14px;
}

.article-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.article-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  border: 1px solid rgba(148, 163, 184, 0.2);
  border-radius: 12px;
  padding: 14px 16px;
  cursor: pointer;
  background: rgba(255, 255, 255, 0.6);
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}
.dark .article-item {
  background: rgba(40, 40, 40, 0.5);
  border-color: rgba(255, 255, 255, 0.1);
}

.article-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(14, 165, 233, 0.12);
}

.article-main { min-width: 0; flex: 1; }

.article-title {
  margin: 0;
  color: var(--ink);
  font-size: 15px;
  font-weight: 600;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.article-preview {
  margin: 6px 0 0;
  color: var(--text-secondary);
  font-size: 13px;
  line-height: 1.6;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.article-meta {
  margin-top: 8px;
  display: flex;
  gap: 14px;
  color: #94a3b8;
  font-size: 12px;
}

/* ── 编辑弹窗 ── */
.edit-avatar-row {
  display: flex;
  align-items: center;
  gap: 14px;
}

@media (max-width: 767px) {
  .profile-page { padding-top: 10px; }
  .cover-bg { height: 130px; }
  .profile-header { margin-top: -38px; padding: 0 14px; }
  .profile-info { padding: 12px 14px 16px; }
  .display-name { font-size: 20px; }
  .btn-edit, .btn-follow, .btn-message { height: 32px; padding: 0 12px; font-size: 12px; }
  .tiktok-profile:hover { transform: none; box-shadow: none; }
}
</style>
