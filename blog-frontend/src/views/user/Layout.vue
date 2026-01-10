<template>
  <div class="min-h-screen">
    <!-- 顶部导航栏 -->
    <header class="fixed top-0 left-0 right-0 z-50 glass">
      <div class="max-w-7xl mx-auto px-2 sm:px-4 h-14 sm:h-16 flex items-center justify-between">
        <div class="flex items-center gap-1 sm:gap-4">
          <el-button v-if="isMobile" :icon="Menu" text size="small" @click="showMobileMenu = true" />
          <router-link to="/" class="flex items-center gap-1 sm:gap-2">
            <img :src="config.logo" alt="logo" class="w-7 h-7 sm:w-8 sm:h-8 rounded-full" />
            <span class="text-base sm:text-lg font-bold bg-gradient-to-r from-amber-500 to-orange-500 bg-clip-text text-transparent hidden sm:inline">{{ config.siteName }}</span>
          </router-link>
          <nav class="hidden md:flex items-center gap-6 ml-8">
            <router-link to="/" class="text-gray-600 hover:text-primary-500 dark:text-gray-300">首页</router-link>
            <router-link to="/discover" class="text-gray-600 hover:text-primary-500 dark:text-gray-300">发现</router-link>
            <router-link to="/community" class="text-gray-600 hover:text-primary-500 dark:text-gray-300">相册</router-link>
            <router-link to="/quiz" class="text-gray-600 hover:text-primary-500 dark:text-gray-300">刷题</router-link>
            <router-link to="/tree-hole" class="text-gray-600 hover:text-primary-500 dark:text-gray-300">树洞</router-link>
          </nav>
        </div>
        <div class="flex items-center gap-1 sm:gap-4">
          <!-- 移动端搜索按钮 -->
          <el-button v-if="isMobile" :icon="Search" text size="small" @click="showSearchDialog = true" />
          <!-- PC端搜索框 -->
          <el-input v-if="!isMobile" v-model="searchQuery" placeholder="搜索..." class="w-48" size="small" @keyup.enter="handleSearch">
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
          <el-button :icon="isDark ? Sunny : Moon" :size="isMobile ? 'small' : 'default'" circle @click="userStore.toggleDark" />
          <el-button :icon="Headset" :size="isMobile ? 'small' : 'default'" circle @click="musicStore.showPlayer = true" title="音乐播放器" />
          <template v-if="userStore.isLoggedIn">
            <el-popover placement="bottom" :width="320" trigger="hover" :show-after="200">
              <template #reference>
                <el-badge :value="msgUnreadCount" :hidden="!msgUnreadCount" :max="99">
                  <el-button :icon="ChatDotRound" :size="isMobile ? 'small' : 'default'" circle />
                </el-badge>
              </template>
              <div class="max-h-80 overflow-y-auto" @wheel.stop>
                <div class="flex justify-between items-center mb-2">
                  <span class="font-bold">私信</span>
                  <el-button type="primary" link size="small" class="!text-blue-50" @click="router.push('/chat')">查看全部</el-button>
                </div>
                <div v-if="!conversations.length" class="text-center py-4 text-gray-500">暂无私信</div>
                <div v-for="conv in conversations" :key="conv.id"
                     class="flex items-center gap-3 p-2 hover:bg-gray-100 dark:hover:bg-gray-700 rounded cursor-pointer"
                     :class="{ 'bg-blue-50 dark:bg-blue-900/20': getConvUnread(conv) > 0 }"
                     @click="router.push(`/chat?userId=${conv.otherUser?.id}`)">
                  <el-avatar :src="conv.otherUser?.avatar" :size="40">{{ conv.otherUser?.username?.[0] }}</el-avatar>
                  <div class="flex-1 overflow-hidden">
                    <div class="text-sm font-medium truncate">{{ conv.otherUser?.nickname || conv.otherUser?.username }}</div>
                    <div class="text-xs text-gray-400 truncate">{{ conv.lastMessage?.content || '暂无消息' }}</div>
                  </div>
                  <el-badge v-if="getConvUnread(conv) > 0" :value="getConvUnread(conv)" :max="99" />
                </div>
              </div>
            </el-popover>
            <el-popover placement="bottom" :width="320" trigger="hover" :show-after="200">
              <template #reference>
                <el-badge :value="unreadCount" :hidden="!unreadCount" :max="99">
                  <el-button :icon="Bell" :size="isMobile ? 'small' : 'default'" circle />
                </el-badge>
              </template>
              <div class="max-h-80 overflow-y-auto" @wheel.stop>
                <div class="flex justify-between items-center mb-2">
                  <span class="font-bold">消息通知</span>
                  <el-button v-if="unreadCount" type="primary" link size="small" class="!text-blue-500" @click="handleMarkAllRead">全部已读</el-button>
                </div>
                <div v-if="!notifications.length" class="text-center py-4 text-gray-500">暂无消息</div>
                <div v-for="n in notifications" :key="n.id" class="p-2 hover:bg-gray-100 dark:hover:bg-gray-700 rounded cursor-pointer" :class="{ 'bg-blue-50 dark:bg-blue-900/20': !n.isRead }" @click="handleNotificationClick(n)">
                  <div class="text-sm">{{ getNotificationText(n) }}</div>
                  <div class="text-xs text-gray-400 mt-1">{{ n.createdAt }}</div>
                </div>
              </div>
            </el-popover>
            <el-dropdown>
              <el-avatar :src="userStore.user?.avatar" :size="isMobile ? 28 : 36">{{ userStore.user?.username?.[0] }}</el-avatar>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item @click="router.push(`/user/${userStore.user?.id}`)">个人主页</el-dropdown-item>
                  <el-dropdown-item @click="router.push('/album')">我的相册</el-dropdown-item>
                  <el-dropdown-item @click="router.push('/write')">写文章</el-dropdown-item>
                  <el-dropdown-item @click="router.push('/about')">关于我们</el-dropdown-item>
                  <el-dropdown-item v-if="userStore.isAdmin" @click="router.push('/admin')">管理后台</el-dropdown-item>
                  <el-dropdown-item divided @click="handleLogout">退出登录</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
          <template v-else>
            <el-button type="primary" :size="isMobile ? 'small' : 'default'" @click="router.push('/login')">登录</el-button>
          </template>
        </div>
      </div>
    </header>

    <!-- 移动端菜单 -->
    <el-drawer v-model="showMobileMenu" direction="ltr" size="280px">
      <template #header>
        <div class="flex items-center gap-2">
          <img :src="config.logo" alt="logo" class="w-6 h-6 rounded-full" />
          <span class="text-lg font-bold">{{ config.siteName }}</span>
        </div>
      </template>
      <div class="flex flex-col gap-4">
        <router-link to="/" class="p-2 hover:bg-gray-100 rounded" @click="showMobileMenu = false">首页</router-link>
        <router-link to="/discover" class="p-2 hover:bg-gray-100 rounded" @click="showMobileMenu = false">发现</router-link>
        <router-link to="/community" class="p-2 hover:bg-gray-100 rounded" @click="showMobileMenu = false">相册</router-link>
        <router-link to="/quiz" class="p-2 hover:bg-gray-100 rounded" @click="showMobileMenu = false">刷题</router-link>
        <router-link to="/tree-hole" class="p-2 hover:bg-gray-100 rounded" @click="showMobileMenu = false">树洞</router-link>
        <router-link v-if="userStore.isLoggedIn" to="/album" class="p-2 hover:bg-gray-100 rounded" @click="showMobileMenu = false">我的相册</router-link>
        <router-link v-if="userStore.isLoggedIn" to="/write" class="p-2 hover:bg-gray-100 rounded" @click="showMobileMenu = false">写文章</router-link>
      </div>
    </el-drawer>

    <!-- 移动端搜索对话框 -->
    <el-dialog v-model="showSearchDialog" title="搜索" :width="isMobile ? '95%' : '500px'" :show-close="true">
      <el-input 
        v-model="searchQuery" 
        placeholder="输入搜索关键词..." 
        size="large"
        clearable
        autofocus
        @keyup.enter="handleSearch">
        <template #prefix><el-icon><Search /></el-icon></template>
      </el-input>
      <template #footer>
        <el-button @click="showSearchDialog = false">取消</el-button>
        <el-button type="primary" @click="handleSearch">搜索</el-button>
      </template>
    </el-dialog>

    <!-- 主内容区 -->
    <main class="pt-20 pb-8 px-4 max-w-7xl mx-auto">
      <router-view />
    </main>

    <!-- 音乐播放器 -->
    <MusicPlayer />
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { useMusicStore } from '@/stores/music'
import { Search, Menu, Sunny, Moon, Bell, ChatDotRound, Headset } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { getNotifications, getUnreadCount, markAsRead, markAllAsRead, getMessageUnreadCount, getConversations } from '@/api/blog'
import config from '@/config'
import MusicPlayer from '@/components/MusicPlayer.vue'

const router = useRouter()
const userStore = useUserStore()
const musicStore = useMusicStore()
const showMobileMenu = ref(false)
const showSearchDialog = ref(false)
const searchQuery = ref('')
const isDark = ref(userStore.isDark)
const notifications = ref([])
const unreadCount = ref(0)
const msgUnreadCount = ref(0)
const conversations = ref([])
const isMobile = ref(window.innerWidth < 768)
let ws = null

// 监听窗口大小变化
const handleResize = () => {
  isMobile.value = window.innerWidth < 768
}

window.addEventListener('resize', handleResize)

const getConvUnread = (conv) => {
  return conv.user1Id === userStore.user?.id ? conv.user1Unread : conv.user2Unread
}

const fetchNotifications = async () => {
  if (!userStore.isLoggedIn) return
  const [res1, res2, res3, res4] = await Promise.all([
    getNotifications(), getUnreadCount(), getMessageUnreadCount(), getConversations()
  ])
  if (res1.success) notifications.value = res1.data
  if (res2.success) unreadCount.value = res2.data.count
  if (res3.success) msgUnreadCount.value = res3.data
  if (res4.success) conversations.value = res4.data || []
}

const connectWebSocket = () => {
  if (!userStore.isLoggedIn) return
  ws = new WebSocket(`${config.wsBaseUrl}/ws/notifications?userId=${userStore.user.id}`)
  ws.onmessage = (e) => {
    const n = JSON.parse(e.data)
    notifications.value.unshift(n)
    unreadCount.value++
    ElMessage.info(getNotificationText(n))
  }
}

const getNotificationText = (n) => {
  const types = { like: '赞了你的文章', favorite: '收藏了你的文章', follow: '关注了你' }
  return (n.fromUsername || '有人') + (types[n.type] || n.content)
}

const handleNotificationClick = async (n) => {
  if (!n.isRead) {
    await markAsRead(n.id)
    n.isRead = true
    unreadCount.value = Math.max(0, unreadCount.value - 1)
  }
  if (n.type === 'follow') router.push(`/user/${n.targetId}`)
  else router.push(`/article/${n.targetId}`)
}

const handleMarkAllRead = async () => {
  await markAllAsRead()
  notifications.value.forEach(n => n.isRead = true)
  unreadCount.value = 0
}

onMounted(() => { fetchNotifications(); connectWebSocket() })
onUnmounted(() => {
  ws?.close()
  window.removeEventListener('resize', handleResize)
})

const handleSearch = () => {
  if (searchQuery.value.trim()) {
    router.push({ path: '/search', query: { q: searchQuery.value } })
    showSearchDialog.value = false
    searchQuery.value = ''
  }
}

const handleLogout = () => {
  userStore.logout()
  ElMessage.success('已退出登录')
  router.push('/')
}
</script>
