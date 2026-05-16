<template>
  <div class="messages-page max-w-4xl mx-auto pt-8 pb-12 px-4 shadow-sm">
    <!-- 顶部标题栏 -->
    <div class="msg-page-header">
      <span class="msg-page-title"><span class="stamp">讯</span>消息</span>
      <button v-if="activeTab === 'notif' && unreadCount > 0" class="mark-all-btn" @click="handleMarkAllRead">全部已读</button>
      <button v-if="activeTab === 'chat'" class="mark-all-btn" @click="router.push('/chat')">私信页 →</button>
    </div>

    <!-- Tab 切换 -->
    <div class="msg-tabs">
      <button class="msg-tab" :class="{ active: activeTab === 'chat' }" @click="activeTab = 'chat'">
        私信
        <span v-if="msgUnreadCount > 0" class="tab-badge">{{ msgUnreadCount > 99 ? '99+' : msgUnreadCount }}</span>
      </button>
      <button class="msg-tab" :class="{ active: activeTab === 'notif' }" @click="activeTab = 'notif'">
        通知
        <span v-if="unreadCount > 0" class="tab-badge">{{ unreadCount > 99 ? '99+' : unreadCount }}</span>
      </button>
    </div>

    <!-- 私信列表 -->
    <div v-if="activeTab === 'chat'" class="msg-body">
      <div v-if="loadingConv" class="msg-loading">
        <div class="skeleton-row" v-for="i in 6" :key="i">
          <div class="skel-avatar" /><div class="skel-lines"><div class="skel-line skel-line--name"/><div class="skel-line skel-line--sub"/></div>
        </div>
      </div>
      <div v-else-if="!conversations.length" class="msg-empty">
        <svg width="64" height="64" viewBox="0 0 24 24" fill="none" stroke="#d0d5dd" stroke-width="1.2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/></svg>
        <p>暂无私信</p>
        <button class="start-chat-btn" @click="router.push('/chat')">去聊天</button>
      </div>
      <div v-else class="conv-list">
        <div v-for="conv in conversations" :key="conv.id"
             class="conv-row"
             :class="{ 'conv-row--unread': getConvUnread(conv) > 0 }"
             @click="router.push(`/chat?userId=${conv.otherUser?.id}`)">
          <div class="conv-avatar-wrap">
            <el-avatar :src="conv.otherUser?.avatar" :size="52">{{ (conv.otherUser?.nickname || conv.otherUser?.username || '?')[0] }}</el-avatar>
            <span v-if="getConvUnread(conv) > 0" class="conv-badge">{{ getConvUnread(conv) > 9 ? '9+' : getConvUnread(conv) }}</span>
          </div>
          <div class="conv-info">
            <div class="conv-top">
              <span class="conv-name">{{ conv.otherUser?.nickname || conv.otherUser?.username }}</span>
              <span class="conv-time">{{ formatTime(conv.lastMessage?.createdAt) }}</span>
            </div>
            <div class="conv-sub" :class="{ 'conv-sub--unread': getConvUnread(conv) > 0 }">
              {{ conv.lastMessage?.content || '开始聊天吧' }}
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 通知列表 -->
    <div v-if="activeTab === 'notif'" class="msg-body">
      <div v-if="loadingNotif" class="msg-loading">
        <div class="skeleton-row" v-for="i in 6" :key="i">
          <div class="skel-avatar skel-avatar--sq" /><div class="skel-lines"><div class="skel-line skel-line--name"/><div class="skel-line skel-line--sub"/></div>
        </div>
      </div>
      <div v-else-if="!notifications.length" class="msg-empty">
        <svg width="64" height="64" viewBox="0 0 24 24" fill="none" stroke="#d0d5dd" stroke-width="1.2" stroke-linecap="round" stroke-linejoin="round"><path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"/><path d="M13.73 21a2 2 0 0 1-3.46 0"/></svg>
        <p>暂无通知</p>
      </div>
      <div v-else class="notif-list">
        <div v-for="n in notifications" :key="n.id"
             class="notif-row"
             :class="{ 'notif-row--unread': !n.isRead }"
             @click="handleNotifClick(n)">
          <div class="notif-icon-wrap" :class="`notif-icon--${n.type}`">
            <svg v-if="n.type==='like'" width="20" height="20" viewBox="0 0 24 24" fill="currentColor"><path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"/></svg>
            <svg v-else-if="n.type==='favorite'" width="20" height="20" viewBox="0 0 24 24" fill="currentColor"><polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"/></svg>
            <svg v-else-if="n.type==='follow'" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><line x1="19" y1="8" x2="19" y2="14"/><line x1="22" y1="11" x2="16" y2="11"/></svg>
            <svg v-else width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>
          </div>
          <div class="notif-info">
            <div class="notif-text">{{ getNotifText(n) }}</div>
            <div class="notif-time">{{ n.createdAt }}</div>
          </div>
          <div v-if="!n.isRead" class="notif-dot" />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getConversations, getNotifications, getUnreadCount, getMessageUnreadCount, markAsRead, markAllAsRead } from '@/api/blog'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()

const activeTab = ref('chat')
const conversations = ref([])
const notifications = ref([])
const unreadCount = ref(0)
const msgUnreadCount = ref(0)
const loadingConv = ref(false)
const loadingNotif = ref(false)

const userId = ref(JSON.parse(localStorage.getItem('user') || '{}').id)

const getConvUnread = (conv) => conv.user1Id === userId.value ? conv.user1Unread : conv.user2Unread

const formatTime = (t) => {
  if (!t) return ''
  const d = new Date(t), now = new Date()
  const hm = `${d.getHours().toString().padStart(2,'0')}:${d.getMinutes().toString().padStart(2,'0')}`
  return d.toDateString() === now.toDateString() ? hm : `${d.getMonth()+1}/${d.getDate()}`
}

const getNotifText = (n) => {
  const types = { like: '赞了你的文章', favorite: '收藏了你的文章', follow: '关注了你' }
  return (n.fromUsername || '用户') + (types[n.type] || n.content || '')
}

const handleNotifClick = async (n) => {
  if (!n.isRead) {
    await markAsRead(n.id)
    n.isRead = true
    unreadCount.value = Math.max(0, unreadCount.value - 1)
  }
  if (n.type === 'follow') router.push(`/user/${n.targetId}`)
  else router.push(`/article/${n.targetId}`).catch(() => ElMessage.warning('该文章可能已被删除'))
}

const handleMarkAllRead = async () => {
  await markAllAsRead()
  notifications.value.forEach(n => n.isRead = true)
  unreadCount.value = 0
}

onMounted(async () => {
  if (!userStore.isLoggedIn) { router.push('/login'); return }
  loadingConv.value = true
  loadingNotif.value = true
  try {
    const [r1, r2, r3, r4] = await Promise.all([
      getConversations(), getNotifications(), getUnreadCount(), getMessageUnreadCount()
    ])
    if (r1.success) conversations.value = r1.data || []
    if (r2.success) notifications.value = r2.data || []
    if (r3.success) unreadCount.value = r3.data.count
    if (r4.success) msgUnreadCount.value = r4.data
  } finally {
    loadingConv.value = false
    loadingNotif.value = false
  }
})
</script>

<style scoped>
.messages-page {
  min-height: 100vh;
  background: var(--paper);
  display: flex; flex-direction: column;
}
.dark .messages-page {
  background: #111;
}

/* 顶部标题 */
.msg-page-header {
  display: flex; align-items: center; justify-content: space-between;
  padding: 16px 20px 8px;
  background: rgba(253,250,242,0.95);
  backdrop-filter: blur(12px);
  position: sticky; top: 0; z-index: 10;
  border-bottom: 2px solid var(--ink);
}
.dark .msg-page-header {
  background: rgba(26, 26, 26, 0.95);
  border-bottom-color: #333;
}
.msg-page-title { font-size: 22px; font-weight: 700; color: var(--ink); display: flex; align-items: center; gap: 8px; }
.dark .msg-page-title { color: #eee; }
.msg-page-title .stamp { margin-right: 0 !important; transform: rotate(-10deg) scale(0.85); }
.mark-all-btn {
  padding: 6px 14px; border: none;
  background: rgba(102,126,234,0.1); color: #667eea;
  font-size: 13px; font-weight: 600; border-radius: 20px;
  cursor: pointer; -webkit-tap-highlight-color: transparent;
  transition: background 0.15s;
}
.mark-all-btn:active { background: rgba(102,126,234,0.2); }

/* Tabs */
.msg-tabs {
  display: flex; gap: 0;
  padding: 0 20px;
  background: rgba(255,255,255,0.95);
  border-bottom: 1px solid rgba(0,0,0,0.06);
  position: sticky; top: 53px; z-index: 9;
}
.dark .msg-tabs {
  background: rgba(34, 34, 34, 0.95);
  border-bottom-color: rgba(255,255,255,0.1);
}
.msg-tab {
  display: flex; align-items: center; gap: 6px;
  padding: 12px 20px; border: none; background: transparent;
  font-size: 15px; font-weight: 500; color: #8a8a9a;
  border-bottom: 2.5px solid transparent; margin-bottom: -1px;
  cursor: pointer; transition: all 0.2s;
  -webkit-tap-highlight-color: transparent;
}
.msg-tab.active { color: #1a1a2e; border-bottom-color: #667eea; font-weight: 700; }
.dark .msg-tab.active { color: #fff; }
.tab-badge {
  min-width: 16px; height: 16px; padding: 0 4px;
  background: #f5576c; color: #fff;
  font-size: 10px; font-weight: 700; border-radius: 8px;
  display: flex; align-items: center; justify-content: center; line-height: 1;
}

/* Body */
.msg-body { flex: 1; overflow-y: auto; }

/* 骨架屏 */
.msg-loading { padding: 8px 0; }
.skeleton-row {
  display: flex; align-items: center; gap: 14px;
  padding: 14px 20px;
}
.skel-avatar {
  width: 52px; height: 52px; border-radius: 50%; flex-shrink: 0;
  background: linear-gradient(90deg, #f0f0f0 25%, #e8e8e8 50%, #f0f0f0 75%);
  background-size: 200% 100%; animation: shimmer 1.4s infinite;
}
.dark .skel-avatar {
  background: linear-gradient(90deg, #333 25%, #444 50%, #333 75%);
  background-size: 200% 100%;
}
.skel-avatar--sq { border-radius: 12px; }
.skel-lines { flex: 1; display: flex; flex-direction: column; gap: 8px; }
.skel-line {
  height: 12px; border-radius: 6px;
  background: linear-gradient(90deg, #f0f0f0 25%, #e8e8e8 50%, #f0f0f0 75%);
  background-size: 200% 100%; animation: shimmer 1.4s infinite;
}
.dark .skel-line {
  background: linear-gradient(90deg, #333 25%, #444 50%, #333 75%);
  background-size: 200% 100%;
}
.skel-line--name { width: 35%; }
.skel-line--sub { width: 65%; }
@keyframes shimmer { to { background-position: -200% 0; } }

/* 空状态 */
.msg-empty {
  display: flex; flex-direction: column; align-items: center; gap: 12px;
  padding: 64px 0; color: #c0c4cc; font-size: 14px;
}
.start-chat-btn {
  margin-top: 8px; padding: 10px 28px;
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: #fff; font-size: 14px; font-weight: 600;
  border: none; border-radius: 24px; cursor: pointer;
  -webkit-tap-highlight-color: transparent;
}

/* 私信列表 */
.conv-list { padding: 4px 0; }
.conv-row {
  display: flex; align-items: center; gap: 14px;
  padding: 14px 20px; cursor: pointer;
  -webkit-tap-highlight-color: transparent; transition: background 0.15s;
}
.conv-row:active { background: rgba(0,0,0,0.04); }
.dark .conv-row:active { background: rgba(255,255,255,0.04); }
.conv-row--unread { background: rgba(102,126,234,0.04); }
.dark .conv-row--unread { background: rgba(102,126,234,0.1); }
.conv-avatar-wrap { position: relative; flex-shrink: 0; }
.conv-badge {
  position: absolute; top: -3px; right: -3px;
  min-width: 18px; height: 18px; padding: 0 4px;
  background: #f5576c; color: #fff;
  font-size: 10px; font-weight: 700; border-radius: 9px;
  border: 2px solid #fff;
  display: flex; align-items: center; justify-content: center; line-height: 1;
}
.conv-info { flex: 1; overflow: hidden; min-width: 0; }
.conv-top {
  display: flex; align-items: center; justify-content: space-between; margin-bottom: 4px;
}
.conv-name { font-size: 15px; font-weight: 600; color: #1a1a2e; }
.dark .conv-name { color: #eee; }
.conv-time { font-size: 11px; color: #c0c4cc; flex-shrink: 0; }
.conv-sub {
  font-size: 13px; color: #909399;
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
}
.conv-sub--unread { color: #1a1a2e; font-weight: 500; }
.dark .conv-sub--unread { color: #fff; }

/* 通知列表 */
.notif-list { padding: 4px 0; }
.notif-row {
  display: flex; align-items: center; gap: 14px;
  padding: 14px 20px; cursor: pointer;
  -webkit-tap-highlight-color: transparent; transition: background 0.15s;
}
.notif-row:active { background: rgba(0,0,0,0.04); }
.dark .notif-row:active { background: rgba(255,255,255,0.04); }
.notif-row--unread { background: rgba(102,126,234,0.04); }
.dark .notif-row--unread { background: rgba(102,126,234,0.1); }
.notif-icon-wrap {
  width: 48px; height: 48px; border-radius: 14px; flex-shrink: 0;
  display: flex; align-items: center; justify-content: center;
}
.notif-icon--like { background: rgba(245,87,108,0.12); color: #f5576c; }
.notif-icon--favorite { background: rgba(245,158,11,0.12); color: #f59e0b; }
.notif-icon--follow { background: rgba(102,126,234,0.12); color: #667eea; }
.notif-icon--undefined, .notif-icon-- { background: rgba(0,0,0,0.06); color: #909399; }
.notif-info { flex: 1; overflow: hidden; min-width: 0; }
.notif-text {
  font-size: 14px; font-weight: 500; color: #1a1a2e; line-height: 1.4;
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
}
.dark .notif-text { color: #eee; }
.notif-time { font-size: 12px; color: #c0c4cc; margin-top: 3px; }
.notif-dot {
  width: 8px; height: 8px; border-radius: 50%;
  background: #667eea; flex-shrink: 0;
}
</style>
