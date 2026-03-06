<template>
  <div class="max-w-7xl mx-auto">
    <div class="mb-4">
      <el-button @click="handleBack" :icon="ArrowLeft" text>返回</el-button>
    </div>

    <div class="flex gap-2.5">
      <!-- 主内容区 -->
      <div class="flex-1 min-w-0">
        <div class="glass rounded-xl p-6 mb-6">
          <h1 class="text-2xl font-bold mb-4 dark:text-white">{{ article.title }}</h1>

          <!-- AI 总结 -->
          <div class="ai-summary mb-4" v-if="aiSummary || summaryLoading">
            <div class="flex items-center gap-2 text-sm text-purple-600 dark:text-purple-400 mb-2">
              <svg class="w-4 h-4" fill="currentColor" viewBox="0 0 24 24"><path d="M12 2a2 2 0 012 2c0 .74-.4 1.39-1 1.73V7h1a7 7 0 017 7h1a1 1 0 011 1v3a1 1 0 01-1 1h-1v1a2 2 0 01-2 2H5a2 2 0 01-2-2v-1H2a1 1 0 01-1-1v-3a1 1 0 011-1h1a7 7 0 017-7h1V5.73c-.6-.34-1-.99-1-1.73a2 2 0 012-2m-4 9a5 5 0 00-5 5v4h14v-4a5 5 0 00-5-5h-4m0 2h4a3 3 0 013 3v1H5v-1a3 3 0 013-3z"/></svg>
              <span class="font-medium">AI 总结</span>
            </div>
            <div v-if="summaryLoading" class="text-gray-500 text-sm typing-effect">正在生成总结...</div>
            <div v-else class="text-gray-700 dark:text-gray-300 text-sm bg-purple-50 dark:bg-purple-900/20 rounded-lg p-3">{{ aiSummary }}</div>
          </div>

          <div class="flex items-center gap-4 mb-6 text-gray-500">
            <div class="flex items-center gap-2 cursor-pointer" @click="router.push(`/user/${article.userId}`)">
              <el-avatar :src="article.authorAvatar" :size="32">{{ article.authorName?.[0] || 'U' }}</el-avatar>
              <span>{{ article.authorName || '匿名用户' }}</span>
            </div>
            <span>{{ article.createdAt }}</span>
            <span><el-icon><View /></el-icon> {{ article.viewCount }}</span>
            <!-- 点赞按钮 - 未登录时显示提示 -->
            <el-tooltip v-if="!userStore.isLoggedIn" content="登录后可点赞" placement="top">
              <span class="cursor-pointer flex items-center gap-1 text-gray-400">
                <svg xmlns="http://www.w3.org/2000/svg" class="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2"><path stroke-linecap="round" stroke-linejoin="round" d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z" /></svg>
                {{ interaction.likeCount }}
              </span>
            </el-tooltip>
            <span v-else class="cursor-pointer flex items-center gap-1" :class="interaction.liked ? 'text-red-500' : 'hover:text-red-500'" @click="toggleLike">
              <svg xmlns="http://www.w3.org/2000/svg" class="w-5 h-5" :fill="interaction.liked ? 'currentColor' : 'none'" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2"><path stroke-linecap="round" stroke-linejoin="round" d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z" /></svg>
              {{ interaction.likeCount }}
            </span>
            <!-- 收藏按钮 - 未登录时显示提示 -->
            <el-tooltip v-if="!userStore.isLoggedIn" content="登录后可收藏" placement="top">
              <span class="cursor-pointer flex items-center gap-1 text-gray-400">
                <svg xmlns="http://www.w3.org/2000/svg" class="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2"><path stroke-linecap="round" stroke-linejoin="round" d="M5 5a2 2 0 012-2h10a2 2 0 012 2v16l-7-3.5L5 21V5z" /></svg>
                {{ interaction.favoriteCount }}
              </span>
            </el-tooltip>
            <span v-else class="cursor-pointer flex items-center gap-1" :class="interaction.favorited ? 'text-yellow-500' : 'hover:text-yellow-500'" @click="toggleFavorite">
              <svg xmlns="http://www.w3.org/2000/svg" class="w-5 h-5" :fill="interaction.favorited ? 'currentColor' : 'none'" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2"><path stroke-linecap="round" stroke-linejoin="round" d="M5 5a2 2 0 012-2h10a2 2 0 012 2v16l-7-3.5L5 21V5z" /></svg>
              {{ interaction.favoriteCount }}
            </span>
          </div>
          <div class="prose dark:prose-invert max-w-none">
            <MdPreview
              :editorId="editorId"
              :modelValue="article.content"
              :theme="userStore.isDark ? 'dark' : 'light'"
              previewTheme="github"
            />
          </div>
        </div>

        <!-- 评论区 -->
        <div class="glass rounded-xl p-6">
          <h3 class="text-lg font-semibold mb-4 dark:text-white">评论 ({{ comments.length }})</h3>

          <!-- 未登录提示 -->
          <div v-if="!userStore.isLoggedIn" class="mb-6 p-4 bg-gray-50 dark:bg-gray-800 rounded-lg text-center">
            <p class="text-gray-600 dark:text-gray-400 mb-3">登录后可以发表评论</p>
            <el-button type="primary" size="small" @click="router.push('/login')">立即登录</el-button>
          </div>

          <!-- 已登录评论输入框 -->
          <div v-else class="mb-6">
            <div class="comment-input-box">
              <el-input v-model="newComment" type="textarea" :autosize="{ minRows: 2, maxRows: 6 }" placeholder="写下你的评论..." />
              <div class="input-actions">
                <EmojiPicker @select="e => newComment += e" />
                <el-button type="primary" size="small" @click="submitComment">发表</el-button>
              </div>
            </div>
          </div>

          <div class="space-y-4">
            <div v-for="comment in topLevelComments" :key="comment.id" class="border-b border-gray-200 dark:border-gray-700 pb-4">
              <div class="flex items-start gap-3">
                <el-avatar :src="comment.avatar" :size="36">{{ comment.username?.[0] }}</el-avatar>
                <div class="flex-1">
                  <div class="flex items-center gap-2 mb-1">
                    <span class="font-medium dark:text-white">{{ comment.username }}</span>
                    <span class="text-sm text-gray-500">{{ comment.createdAt }}</span>
                  </div>
                  <p class="text-gray-700 dark:text-gray-300 mb-2">{{ comment.content }}</p>
                  <div class="flex items-center gap-4 text-sm text-gray-500">
                    <!-- 未登录时显示提示 -->
                    <el-tooltip v-if="!userStore.isLoggedIn" content="登录后可点赞" placement="top">
                      <span class="cursor-pointer flex items-center gap-1 text-gray-400">
                        <svg xmlns="http://www.w3.org/2000/svg" class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2"><path stroke-linecap="round" stroke-linejoin="round" d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z" /></svg>
                        {{ comment.likeCount }}
                      </span>
                    </el-tooltip>
                    <span v-else class="cursor-pointer flex items-center gap-1" :class="likedComments.has(comment.id) ? 'text-red-500' : 'hover:text-red-500'" @click="handleLike(comment)">
                      <svg xmlns="http://www.w3.org/2000/svg" class="w-4 h-4" :fill="likedComments.has(comment.id) ? 'currentColor' : 'none'" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2"><path stroke-linecap="round" stroke-linejoin="round" d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z" /></svg>
                      {{ comment.likeCount }}
                    </span>
                    <!-- 回复按钮 -->
                    <el-tooltip v-if="!userStore.isLoggedIn" content="登录后可回复" placement="top">
                      <span class="cursor-pointer flex items-center gap-1 text-gray-400">
                        <el-icon><ChatLineRound /></el-icon>回复
                      </span>
                    </el-tooltip>
                    <span v-else class="cursor-pointer flex items-center gap-1 hover:text-primary-500" @click="replyTo = comment.id">
                      <el-icon><ChatLineRound /></el-icon>回复
                    </span>
                  </div>

                  <!-- 回复输入框 -->
                  <div v-if="replyTo === comment.id" class="mt-3">
                    <div class="comment-input-box">
                      <el-input v-model="replyContent" type="textarea" :autosize="{ minRows: 1, maxRows: 4 }" placeholder="回复..." />
                      <div class="input-actions">
                        <EmojiPicker @select="e => replyContent += e" />
                        <el-button size="small" type="primary" @click="submitReply(comment.id)">回复</el-button>
                        <el-button size="small" @click="replyTo = null">取消</el-button>
                      </div>
                    </div>
                  </div>

                  <!-- 嵌套回复 -->
                  <div v-if="getReplies(comment.id).length" class="mt-3 pl-4 border-l-2 border-gray-200 space-y-3">
                    <div v-for="reply in getReplies(comment.id)" :key="reply.id">
                      <div class="flex items-start gap-2">
                        <el-avatar :src="reply.avatar" :size="28">{{ reply.username?.[0] }}</el-avatar>
                        <div class="flex-1">
                          <div class="flex items-center gap-2">
                            <span class="font-medium text-sm dark:text-white">{{ reply.username }}</span>
                            <span class="text-xs text-gray-500">{{ reply.createdAt }}</span>
                          </div>
                          <p class="text-sm text-gray-700 dark:text-gray-300">{{ reply.content }}</p>
                          <div class="flex items-center gap-3 text-xs text-gray-500 mt-1">
                            <!-- 未登录时显示提示 -->
                            <el-tooltip v-if="!userStore.isLoggedIn" content="登录后可点赞" placement="top">
                              <span class="cursor-pointer flex items-center gap-1 text-gray-400">
                                <svg xmlns="http://www.w3.org/2000/svg" class="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2"><path stroke-linecap="round" stroke-linejoin="round" d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z" /></svg>
                                {{ reply.likeCount }}
                              </span>
                            </el-tooltip>
                            <span v-else class="cursor-pointer flex items-center gap-1" :class="likedComments.has(reply.id) ? 'text-red-500' : 'hover:text-red-500'" @click="handleLike(reply)">
                              <svg xmlns="http://www.w3.org/2000/svg" class="w-3.5 h-3.5" :fill="likedComments.has(reply.id) ? 'currentColor' : 'none'" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2"><path stroke-linecap="round" stroke-linejoin="round" d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z" /></svg>
                              {{ reply.likeCount }}
                            </span>
                            <!-- 回复按钮 -->
                            <el-tooltip v-if="!userStore.isLoggedIn" content="登录后可回复" placement="top">
                              <span class="cursor-pointer flex items-center gap-1 text-gray-400">
                                <el-icon><ChatLineRound /></el-icon>回复
                              </span>
                            </el-tooltip>
                            <span v-else class="cursor-pointer flex items-center gap-1 hover:text-primary-500" @click="replyTo = reply.id">
                              <el-icon><ChatLineRound /></el-icon>回复
                            </span>
                          </div>

                          <!-- 子评论的回复输入框 -->
                          <div v-if="replyTo === reply.id" class="mt-2">
                            <div class="comment-input-box">
                              <el-input v-model="replyContent" type="textarea" :autosize="{ minRows: 1, maxRows: 4 }" placeholder="回复..." size="small" />
                              <div class="input-actions">
                                <EmojiPicker @select="e => replyContent += e" />
                                <el-button size="small" type="primary" @click="submitReply(comment.id)">回复</el-button>
                                <el-button size="small" @click="replyTo = null">取消</el-button>
                              </div>
                            </div>
                          </div>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 右侧目录 -->
      <div class="hidden lg:block w-64 flex-shrink-0">
        <!-- 占位元素，保持布局 -->
        <div class="h-0"></div>
        <!-- 固定定位的目录 -->
        <div class="fixed w-64" :style="catalogStyle">
          <div class="glass rounded-xl p-4">
            <h3 class="text-sm font-semibold mb-3 dark:text-white flex items-center gap-2">
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 6h16M4 12h16M4 18h7"></path>
              </svg>
              目录
            </h3>
            <MdCatalog
              :editorId="editorId"
              :scrollElement="scrollElement"
              class="article-catalog"
            />
          </div>
        </div>
      </div>
    </div>

    <!-- 返回顶部按钮 -->
    <transition name="fade">
      <div
        v-show="showBackToTop"
        @click="scrollToTop"
        class="fixed bottom-8 right-8 w-12 h-12 bg-primary-500 hover:bg-primary-600 text-white rounded-full shadow-lg cursor-pointer flex items-center justify-center transition-all duration-300 hover:scale-110 z-50"
        title="返回顶部"
      >
        <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 10l7-7m0 0l7 7m-7-7v18"></path>
        </svg>
      </div>
    </transition>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { getArticle, getComments, createComment, likeComment, unlikeComment } from '@/api/blog'
import api from '@/api'
import { MdPreview, MdCatalog } from 'md-editor-v3'
import 'md-editor-v3/lib/preview.css'
import { View, ChatLineRound, ArrowLeft } from '@element-plus/icons-vue'
import toast from '@/utils/toast'
import EmojiPicker from '@/components/EmojiPicker.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const article = ref({})
const comments = ref([])
const newComment = ref('')
const replyTo = ref(null)
const replyContent = ref('')
const likedComments = ref(new Set())
const interaction = ref({ likeCount: 0, favoriteCount: 0, liked: false, favorited: false })
const aiSummary = ref('')
const summaryLoading = ref(false)
const showBackToTop = ref(false)
const catalogStyle = ref({})

// 目录配置
const editorId = 'article-preview'
const scrollElement = document.documentElement

// 计算��录位置
const updateCatalogPosition = () => {
  const mainContent = document.querySelector('.flex-1.min-w-0')
  const articleCard = document.querySelector('.glass.rounded-xl')
  if (!mainContent || !articleCard) return

  const mainRect = mainContent.getBoundingClientRect()
  const cardRect = articleCard.getBoundingClientRect()

  // 使用文章卡片的当前top位置，但不小于16px
  const topValue = Math.max(cardRect.top, 16)

  catalogStyle.value = {
    left: `${mainRect.right + 10}px`,
    top: `${topValue}px`
  }
}

// 监听滚动，显示/隐藏返回顶部按钮
const handleScroll = () => {
  showBackToTop.value = window.scrollY > 300
}

// 监听窗口大小变化，更新目录位置
const handleResize = () => {
  updateCatalogPosition()
}

// 返回顶部
const scrollToTop = () => {
  window.scrollTo({
    top: 0,
    behavior: 'smooth'
  })
}

// 处理文章内锚点链接的平滑滚动
const setupSmoothScroll = () => {
  // 等待DOM更新后再添加事件监听
  setTimeout(() => {
    const articleContent = document.querySelector('.prose')
    if (!articleContent) return

    // 监听所有锚点链接的点击
    articleContent.addEventListener('click', (e) => {
      const target = e.target.closest('a[href^="#"]')
      if (!target) return

      e.preventDefault() // 阻止默认跳转

      const href = target.getAttribute('href')
      const targetId = href.substring(1) // 移除 # 号
      const targetElement = document.getElementById(targetId)

      if (targetElement) {
        // 平滑滚动到目标元素
        targetElement.scrollIntoView({
          behavior: 'smooth',
          block: 'start'
        })
      }
    })
  }, 500)
}

const handleBack = () => {
  // 检查是否有历史记录且是站内页面
  if (window.history.length > 1) {
    const referrer = document.referrer
    const currentOrigin = window.location.origin

    // 如��� referrer 是站内页面，使用 back
    if (referrer && referrer.startsWith(currentOrigin)) {
      router.back()
    } else {
      // 否则返回首页
      router.push('/')
    }
  } else {
    // 没有历史记录，返回首页
    router.push('/')
  }
}

const topLevelComments = computed(() => comments.value.filter(c => !c.parentId))
const getReplies = (parentId) => comments.value.filter(c => c.parentId === parentId)

const fetchData = async () => {
  const [articleRes, commentsRes, interactionRes] = await Promise.all([
    getArticle(route.params.id),
    getComments(route.params.id),
    api.get(`/articles/${route.params.id}/interaction`)
  ])
  if (articleRes.success) {
    article.value = articleRes.data
    fetchSummary()
    // 设置平滑滚动
    setupSmoothScroll()
  }
  if (commentsRes.success) comments.value = commentsRes.data
  if (interactionRes.success) interaction.value = interactionRes.data
}

const fetchSummary = async () => {
  summaryLoading.value = true
  const res = await api.get(`/articles/${route.params.id}/summary`)
  summaryLoading.value = false
  if (res.success) aiSummary.value = res.data
}

const toggleLike = async () => {
  if (!userStore.isLoggedIn) { toast('请先登录'); router.push('/login'); return }
  const res = await api.post(`/articles/${route.params.id}/like`)
  if (res.success) {
    interaction.value.liked = res.data.liked
    interaction.value.likeCount += res.data.liked ? 1 : -1
    toast(res.data.liked ? '点赞成功' : '已取消点赞')
  }
}

const toggleFavorite = async () => {
  if (!userStore.isLoggedIn) { toast('请先登录'); router.push('/login'); return }
  const res = await api.post(`/articles/${route.params.id}/favorite`)
  if (res.success) {
    interaction.value.favorited = res.data.favorited
    interaction.value.favoriteCount += res.data.favorited ? 1 : -1
    toast(res.data.favorited ? '收藏成功' : '已取消收藏')
  }
}

const submitComment = async () => {
  if (!newComment.value.trim()) return
  const res = await createComment({ articleId: Number(route.params.id), content: newComment.value })
  if (res.success) {
    comments.value.push(res.data)
    newComment.value = ''
    toast('评论成功')
  }
}

const submitReply = async (parentId) => {
  if (!replyContent.value.trim()) return
  const res = await createComment({ articleId: Number(route.params.id), parentId: Number(parentId), content: replyContent.value })
  if (res.success) {
    comments.value.push(res.data)
    replyContent.value = ''
    replyTo.value = null
    toast('回复成功')
  }
}

const handleLike = async (comment) => {
  if (!userStore.isLoggedIn) {
    toast('请先登录')
    router.push('/login')
    return
  }
  if (likedComments.value.has(comment.id)) {
    await unlikeComment(comment.id)
    likedComments.value.delete(comment.id)
    comment.likeCount--
  } else {
    await likeComment(comment.id)
    likedComments.value.add(comment.id)
    comment.likeCount++
  }
}

onMounted(fetchData)

onMounted(() => {
  window.addEventListener('scroll', handleScroll)
  window.addEventListener('resize', handleResize)
  // 初始化目录位置
  setTimeout(updateCatalogPosition, 100)
})

onUnmounted(() => {
  window.removeEventListener('scroll', handleScroll)
  window.removeEventListener('resize', handleResize)
})
</script>

<style scoped>
/* 平滑滚动 */
html {
  scroll-behavior: smooth;
}

.comment-input-box {
  position: relative;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  padding: 8px;
  background: #fff;
}
.comment-input-box :deep(.el-textarea__inner) {
  border: none;
  box-shadow: none;
  padding-right: 120px;
}
.input-actions {
  position: absolute;
  right: 8px;
  bottom: 8px;
  display: flex;
  align-items: center;
  gap: 8px;
}
.typing-effect::after {
  content: '|';
  animation: blink 1s infinite;
}
@keyframes blink {
  0%, 50% { opacity: 1; }
  51%, 100% { opacity: 0; }
}

/* 返回顶部按钮动画 */
.fade-enter-active, .fade-leave-active {
  transition: opacity 0.3s, transform 0.3s;
}
.fade-enter-from, .fade-leave-to {
  opacity: 0;
  transform: translateY(20px);
}

/* 目录样式 */
.article-catalog {
  max-height: calc(100vh - 200px);
  overflow-y: auto;
}

.article-catalog :deep(.md-editor-catalog-link) {
  display: block;
  padding: 6px 12px;
  color: #666;
  text-decoration: none;
  border-left: 2px solid transparent;
  transition: all 0.2s;
  font-size: 14px;
  line-height: 1.5;
}

.article-catalog :deep(.md-editor-catalog-link:hover) {
  color: var(--el-color-primary);
  background-color: rgba(64, 158, 255, 0.1);
}

.article-catalog :deep(.md-editor-catalog-link.active) {
  color: var(--el-color-primary);
  border-left-color: var(--el-color-primary);
  background-color: rgba(64, 158, 255, 0.1);
  font-weight: 500;
}

.article-catalog :deep(.md-editor-catalog-link[data-level="2"]) {
  padding-left: 12px;
}

.article-catalog :deep(.md-editor-catalog-link[data-level="3"]) {
  padding-left: 24px;
  font-size: 13px;
}

.article-catalog :deep(.md-editor-catalog-link[data-level="4"]) {
  padding-left: 36px;
  font-size: 12px;
}

/* 暗色模式 */
.dark .article-catalog :deep(.md-editor-catalog-link) {
  color: #aaa;
}

.dark .article-catalog :deep(.md-editor-catalog-link:hover) {
  color: var(--el-color-primary);
}

.dark .article-catalog :deep(.md-editor-catalog-link.active) {
  color: var(--el-color-primary);
}

/* 滚动条样式 */
.article-catalog::-webkit-scrollbar {
  width: 4px;
}

.article-catalog::-webkit-scrollbar-thumb {
  background-color: rgba(0, 0, 0, 0.2);
  border-radius: 4px;
}

.dark .article-catalog::-webkit-scrollbar-thumb {
  background-color: rgba(255, 255, 255, 0.2);
}

/* 文章内容区域的平滑滚动 */
.prose :deep(a[href^="#"]) {
  scroll-behavior: smooth;
}
</style>
