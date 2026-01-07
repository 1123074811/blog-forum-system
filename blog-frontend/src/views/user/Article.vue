<template>
  <div class="max-w-4xl mx-auto">
    <div class="mb-4">
      <el-button @click="router.back()" :icon="ArrowLeft" text>返回</el-button>
    </div>
    <div class="glass rounded-xl p-6 mb-6">
      <h1 class="text-2xl font-bold mb-4 dark:text-white">{{ article.title }}</h1>
      <div class="flex items-center gap-4 mb-6 text-gray-500">
        <div class="flex items-center gap-2 cursor-pointer" @click="router.push(`/user/${article.userId}`)">
          <el-avatar :src="article.authorAvatar" :size="32">{{ article.authorName?.[0] || 'U' }}</el-avatar>
          <span>{{ article.authorName || '匿名用户' }}</span>
        </div>
        <span>{{ article.createdAt }}</span>
        <span><el-icon><View /></el-icon> {{ article.viewCount }}</span>
        <span class="cursor-pointer flex items-center gap-1" :class="interaction.liked ? 'text-red-500' : 'hover:text-red-500'" @click="toggleLike">
          <svg xmlns="http://www.w3.org/2000/svg" class="w-5 h-5" :fill="interaction.liked ? 'currentColor' : 'none'" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2"><path stroke-linecap="round" stroke-linejoin="round" d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z" /></svg>
          {{ interaction.likeCount }}
        </span>
        <span class="cursor-pointer flex items-center gap-1" :class="interaction.favorited ? 'text-yellow-500' : 'hover:text-yellow-500'" @click="toggleFavorite">
          <svg xmlns="http://www.w3.org/2000/svg" class="w-5 h-5" :fill="interaction.favorited ? 'currentColor' : 'none'" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2"><path stroke-linecap="round" stroke-linejoin="round" d="M5 5a2 2 0 012-2h10a2 2 0 012 2v16l-7-3.5L5 21V5z" /></svg>
          {{ interaction.favoriteCount }}
        </span>
      </div>
      <div class="prose dark:prose-invert max-w-none">
        <MdPreview :modelValue="article.content" />
      </div>
    </div>

    <!-- 评论区 -->
    <div class="glass rounded-xl p-6">
      <h3 class="text-lg font-semibold mb-4 dark:text-white">评论 ({{ comments.length }})</h3>

      <div v-if="userStore.isLoggedIn" class="mb-6">
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
                <span class="cursor-pointer flex items-center gap-1" :class="likedComments.has(comment.id) ? 'text-red-500' : 'hover:text-red-500'" @click="handleLike(comment)">
                  <svg xmlns="http://www.w3.org/2000/svg" class="w-4 h-4" :fill="likedComments.has(comment.id) ? 'currentColor' : 'none'" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2"><path stroke-linecap="round" stroke-linejoin="round" d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z" /></svg>
                  {{ comment.likeCount }}
                </span>
                <span class="cursor-pointer flex items-center gap-1 hover:text-primary-500" @click="replyTo = comment.id"><el-icon><ChatLineRound /></el-icon>回复</span>
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
                        <span class="cursor-pointer flex items-center gap-1" :class="likedComments.has(reply.id) ? 'text-red-500' : 'hover:text-red-500'" @click="handleLike(reply)">
                          <svg xmlns="http://www.w3.org/2000/svg" class="w-3.5 h-3.5" :fill="likedComments.has(reply.id) ? 'currentColor' : 'none'" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2"><path stroke-linecap="round" stroke-linejoin="round" d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z" /></svg>
                          {{ reply.likeCount }}
                        </span>
                        <span class="cursor-pointer flex items-center gap-1 hover:text-primary-500" @click="replyTo = reply.id"><el-icon><ChatLineRound /></el-icon>回复</span>
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
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { getArticle, getComments, createComment, likeComment, unlikeComment } from '@/api/blog'
import api from '@/api'
import { MdPreview } from 'md-editor-v3'
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

const topLevelComments = computed(() => comments.value.filter(c => !c.parentId))
const getReplies = (parentId) => comments.value.filter(c => c.parentId === parentId)

const fetchData = async () => {
  const [articleRes, commentsRes, interactionRes] = await Promise.all([
    getArticle(route.params.id),
    getComments(route.params.id),
    api.get(`/articles/${route.params.id}/interaction`)
  ])
  if (articleRes.success) article.value = articleRes.data
  if (commentsRes.success) comments.value = commentsRes.data
  if (interactionRes.success) interaction.value = interactionRes.data
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
</script>

<style scoped>
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
</style>
