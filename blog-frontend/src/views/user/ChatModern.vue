<template>
  <!-- ===== 移动端：会话列表页（抖音风格）===== -->
  <div v-if="isMobile && !currentConv" class="mob-inbox">
    <div class="mob-inbox-header">
      <span class="mob-inbox-title"><span class="stamp">信</span>消息</span>
      <button class="mob-icon-btn" @click="showFriendsSheet = true" aria-label="好友">
        <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><line x1="19" y1="8" x2="19" y2="14"/><line x1="22" y1="11" x2="16" y2="11"/></svg>
      </button>
    </div>

    <div v-if="loading" class="mob-loading">
      <div class="mob-skeleton" v-for="i in 5" :key="i">
        <div class="skel-avatar" /><div class="skel-lines"><div class="skel-line skel-line--name"/><div class="skel-line skel-line--sub"/></div>
      </div>
    </div>

    <div v-else-if="!conversations.length" class="mob-empty">
      <svg width="64" height="64" viewBox="0 0 24 24" fill="none" stroke="#d0d5dd" stroke-width="1.2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/></svg>
      <p>暂无私信</p>
      <button class="mob-start-btn" @click="showFriendsSheet = true">找好友聊天</button>
    </div>

    <div v-else class="mob-conv-list">
      <div v-for="conv in conversations" :key="conv.id"
           class="mob-conv-item"
           @click="selectConversation(conv)">
        <div class="mob-conv-avatar-wrap">
          <el-avatar :src="conv.otherUser?.avatar" :size="52" @click.stop="goToUserProfile(conv.otherUser?.id)">
            {{ (conv.otherUser?.nickname || conv.otherUser?.username || '?')[0] }}
          </el-avatar>
          <span class="online-dot" v-if="conv.otherUser?.isOnline" />
          <span v-if="getUnread(conv) > 0" class="mob-unread-badge">{{ getUnread(conv) > 99 ? '99+' : getUnread(conv) }}</span>
        </div>
        <div class="mob-conv-info">
          <div class="mob-conv-top">
            <span class="mob-conv-name">{{ conv.otherUser?.nickname || conv.otherUser?.username }}</span>
            <span class="mob-conv-time">{{ formatTime(conv.lastMessage?.createdAt) }}</span>
          </div>
          <div class="mob-conv-sub" :class="{ 'mob-conv-sub--unread': getUnread(conv) > 0 }">
            {{ conv.lastMessage?.content || '开始聊天吧' }}
          </div>
        </div>
      </div>
    </div>

    <!-- 好友列表 Sheet -->
    <Teleport to="body">
      <Transition name="sheet">
        <div v-if="showFriendsSheet" class="sheet-mask" @click.self="showFriendsSheet = false">
          <div class="friends-sheet">
            <div class="sheet-handle-bar" />
            <div class="friends-sheet-head">
              <span class="friends-sheet-title">好友列表</span>
              <button class="sheet-close-btn" @click="showFriendsSheet = false">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
              </button>
            </div>
            <div class="friends-sheet-body">
              <div v-if="!friends.length" class="mob-empty" style="padding: 32px 0">
                <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="#d0d5dd" stroke-width="1.2" stroke-linecap="round" stroke-linejoin="round"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/></svg>
                <p>暂无互关好友</p>
              </div>
              <div v-for="f in friends" :key="f.id" class="friend-row" @click="startChatWith(f); showFriendsSheet = false">
                <div class="friend-row-avatar">
                  <el-avatar :src="f.avatar" :size="46">{{ (f.nickname || f.username || '?')[0] }}</el-avatar>
                  <span class="online-dot" v-if="f.isOnline" />
                </div>
                <span class="friend-row-name">{{ f.nickname || f.username }}</span>
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#c0c4cc" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="9 18 15 12 9 6"/></svg>
              </div>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>
  </div>

  <!-- ===== 聊天窗口（移动端全屏 / PC端双栏）===== -->
  <div v-else class="chat-page">
    <div class="chat-wrapper">
      <!-- PC 左侧会话列表 -->
      <div v-if="!isMobile" class="sidebar">
        <div class="sidebar-header">
          <span class="title"><span class="stamp">私</span>私信</span>
          <div class="friend-trigger"
               @mouseenter="handleMouseEnter"
               @mouseleave="handleMouseLeave">
            <el-button type="primary" size="small">好友</el-button>
            <div v-if="showFriendsDialog" class="friend-dropdown"
                 @mouseenter="handleMouseEnter"
                 @mouseleave="handleMouseLeave">
              <div class="friend-dropdown-title">好友列表</div>
              <div class="friend-list">
                <div v-for="f in friends" :key="f.id" class="friend-item" @click="startChatWith(f)">
                  <div class="avatar-status-wrap" :class="{ online: !!f.isOnline }">
                    <el-avatar :src="f.avatar" :size="40" @click.stop="goToUserProfile(f.id)">{{ (f.nickname || f.username || '?')[0] }}</el-avatar>
                  </div>
                  <span class="friend-name" @click.stop="goToUserProfile(f.id)">{{ f.nickname || f.username }}</span>
                </div>
                <el-empty v-if="!friends.length" description="暂无互关好友" :image-size="60" />
              </div>
            </div>
          </div>
        </div>
        <div class="conv-list">
          <div v-if="loading" class="loading-state"><el-icon class="is-loading"><Loading /></el-icon></div>
          <template v-else>
            <div v-for="conv in conversations" :key="conv.id"
                 :class="['conv-item', { active: currentConv?.id === conv.id }]"
                 @click="selectConversation(conv)">
              <div class="avatar-status-wrap" :class="{ online: !!conv.otherUser?.isOnline }">
                <el-avatar :src="conv.otherUser?.avatar" :size="48" @click.stop="goToUserProfile(conv.otherUser?.id)">
                  {{ (conv.otherUser?.nickname || conv.otherUser?.username || '?')[0] }}
                </el-avatar>
              </div>
              <div class="conv-detail">
                <div class="conv-name">{{ conv.otherUser?.nickname || conv.otherUser?.username }}</div>
                <div class="conv-msg">{{ conv.lastMessage?.content || '开始聊天吧' }}</div>
              </div>
              <el-badge v-if="getUnread(conv) > 0" :value="getUnread(conv)" :max="99" class="unread" />
            </div>
            <el-empty v-if="!conversations.length" description="暂无会话" :image-size="80" />
          </template>
        </div>
      </div>

      <!-- 聊天主区域 -->
      <div class="main-chat">
        <template v-if="currentConv">
          <!-- Header -->
          <div class="chat-header">
            <button v-if="isMobile" class="back-icon-btn" @click="backToList" aria-label="返回">
              <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><polyline points="15 18 9 12 15 6"/></svg>
            </button>
            <div class="avatar-status-wrap" :class="{ online: !!currentConv.otherUser?.isOnline }">
              <el-avatar :src="currentConv.otherUser?.avatar" :size="38" @click.stop="goToUserProfile(currentConv.otherUser?.id)" style="cursor:pointer">
                {{ (currentConv.otherUser?.nickname || currentConv.otherUser?.username || '?')[0] }}
              </el-avatar>
            </div>
            <div class="header-info">
              <span class="name" @click.stop="goToUserProfile(currentConv.otherUser?.id)">{{ currentConv.otherUser?.nickname || currentConv.otherUser?.username }}</span>
              <el-tag v-if="!isMutual" type="warning" size="small">非互关，仅能发1条</el-tag>
            </div>
          </div>

          <!-- 消息列表 -->
          <div class="msg-list" ref="msgListRef">
            <div v-for="msg in messages" :key="msg.id" :class="['msg-item', { mine: msg.senderId === userId }]">
              <el-avatar :src="msg.sender?.avatar" :size="34" @click.stop="goToUserProfile(msg.sender?.id || msg.senderId)" style="cursor:pointer;flex-shrink:0">
                {{ (msg.sender?.nickname || msg.sender?.username || '?')[0] }}
              </el-avatar>
              <div class="msg-bubble">
                <template v-if="msg.type === 'text'">{{ msg.content }}</template>
                <el-image v-else-if="msg.type === 'image'" :src="msg.fileUrl" fit="cover" style="max-width:200px;border-radius:8px" :preview-src-list="[msg.fileUrl]" />
                <a v-else-if="msg.type === 'file'" :href="msg.fileUrl" :download="msg.fileName" class="file-link">
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/></svg>
                  {{ msg.fileName }}
                </a>
              </div>
              <span class="msg-time">{{ formatTime(msg.createdAt) }}</span>
            </div>
          </div>

          <!-- 输入区 -->
          <div class="input-area">
            <div class="toolbar">
              <EmojiPicker @select="e => inputText += e" />
              <el-upload :show-file-list="false" :before-upload="handleImageUpload" accept="image/*">
                <button class="tool-btn" aria-label="发送图片">
                  <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="3" width="18" height="18" rx="2"/><circle cx="8.5" cy="8.5" r="1.5"/><polyline points="21 15 16 10 5 21"/></svg>
                </button>
              </el-upload>
              <el-upload :show-file-list="false" :before-upload="handleFileUpload">
                <button class="tool-btn" aria-label="发送文件">
                  <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21.44 11.05l-9.19 9.19a6 6 0 0 1-8.49-8.49l9.19-9.19a4 4 0 0 1 5.66 5.66l-9.2 9.19a2 2 0 0 1-2.83-2.83l8.49-8.48"/></svg>
         
                </button>
              </el-upload>
            </div>
            <div class="input-box">
              <input
                v-model="inputText"
                class="chat-input"
                placeholder="发送消息..."
                :disabled="sendDisabled"
                @keyup.enter="sendText"
              />
              <button class="send-btn" :disabled="sendDisabled || !inputText.trim()" @click="sendText">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><line x1="22" y1="2" x2="11" y2="13"/><polygon points="22 2 15 22 11 13 2 9 22 2"/></svg>
              </button>
            </div>
            <div v-if="sendDisabled" class="limit-tip">非互关好友只能发送1条消息</div>
          </div>
        </template>
        <div v-else class="empty-chat">
          <el-empty description="选择一个会话开始聊天" :image-size="120" />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Loading } from '@element-plus/icons-vue'
import { getConversations, getMessages, sendMessage, uploadMessageFile, getFriends, checkMutualFollow, getUser } from '@/api/blog'
import { ElMessage } from 'element-plus'
import EmojiPicker from '@/components/EmojiPicker.vue'
import config from '@/config'

const route = useRoute()
const router = useRouter()
const userId = ref(JSON.parse(localStorage.getItem('user') || '{}').id)
const loading = ref(false)
const conversations = ref([])
const currentConv = ref(null)
const messages = ref([])
const inputText = ref('')
const showFriendsDialog = ref(false)
const showFriendsSheet = ref(false)
const friends = ref([])
const msgListRef = ref(null)
const isMutual = ref(true)
const sentCount = ref(0)
const isMobile = ref(window.innerWidth <= 768)
let hideTimer = null

const sendDisabled = computed(() => !isMutual.value && sentCount.value >= 1)

const handleResize = () => { isMobile.value = window.innerWidth <= 768 }

const backToList = () => { currentConv.value = null; messages.value = [] }

const goToUserProfile = (id) => {
  if (!id) { ElMessage.warning('用户信息不存在'); return }
  router.push(`/user/${id}`)
}

const handleMouseLeave = () => {
  if (!isMobile.value) {
    if (hideTimer) clearTimeout(hideTimer)
    hideTimer = setTimeout(() => { showFriendsDialog.value = false }, 300)
  }
}
const handleMouseEnter = () => {
  if (!isMobile.value) {
    if (hideTimer) clearTimeout(hideTimer)
    showFriendsDialog.value = true
  }
}

const getUnread = (conv) => conv.user1Id === userId.value ? conv.user1Unread : conv.user2Unread

const formatTime = (t) => {
  if (!t) return ''
  const d = new Date(t), now = new Date()
  const hm = `${d.getHours().toString().padStart(2,'0')}:${d.getMinutes().toString().padStart(2,'0')}`
  return d.toDateString() === now.toDateString() ? hm : `${d.getMonth()+1}/${d.getDate()}`
}

const loadConversations = async () => {
  loading.value = true
  try { const res = await getConversations(); conversations.value = res.data || [] }
  finally { loading.value = false }
}

const selectConversation = async (conv) => {
  currentConv.value = conv
  const res = await getMessages(conv.id)
  messages.value = res.data || []
  const otherId = conv.user1Id === userId.value ? conv.user2Id : conv.user1Id
  const mutualRes = await checkMutualFollow(otherId)
  isMutual.value = mutualRes.data ?? true
  sentCount.value = messages.value.filter(m => m.senderId === userId.value).length
  nextTick(scrollToBottom)
}

const scrollToBottom = () => {
  if (msgListRef.value) msgListRef.value.scrollTop = msgListRef.value.scrollHeight
}

const sendText = async () => {
  if (!inputText.value.trim() || sendDisabled.value) return
  const receiverId = currentConv.value.user1Id === userId.value ? currentConv.value.user2Id : currentConv.value.user1Id
  try {
    const res = await sendMessage({ receiverId, content: inputText.value, type: 'text' })
    if (res.success) { messages.value.push(res.data); inputText.value = ''; sentCount.value++; nextTick(scrollToBottom) }
    else ElMessage.error(res.message || '发送失败')
  } catch { ElMessage.error('发送失败') }
}

const handleImageUpload = async (file) => {
  try {
    const { data: { url, name } } = await uploadMessageFile(file)
    const receiverId = currentConv.value.user1Id === userId.value ? currentConv.value.user2Id : currentConv.value.user1Id
    const res = await sendMessage({ receiverId, content: name, type: 'image', fileUrl: url, fileName: name })
    if (res.success) { messages.value.push(res.data); sentCount.value++; nextTick(scrollToBottom) }
  } catch { ElMessage.error('图片发送失败') }
  return false
}

const handleFileUpload = async (file) => {
  try {
    const { data: { url, name } } = await uploadMessageFile(file)
    const receiverId = currentConv.value.user1Id === userId.value ? currentConv.value.user2Id : currentConv.value.user1Id
    const res = await sendMessage({ receiverId, content: name, type: 'file', fileUrl: url, fileName: name })
    if (res.success) { messages.value.push(res.data); sentCount.value++; nextTick(scrollToBottom) }
  } catch { ElMessage.error('文件发送失败') }
  return false
}

const loadFriends = async () => { const res = await getFriends(); friends.value = res.data || [] }

const applyPresenceStatus = (targetId, online) => {
  const v = !!online
  conversations.value.forEach(c => { if (c?.otherUser?.id === targetId) c.otherUser.isOnline = v })
  friends.value.forEach(f => { if (f?.id === targetId) f.isOnline = v })
  messages.value.forEach(m => { if ((m?.sender?.id || m?.senderId) === targetId && m.sender) m.sender.isOnline = v })
  if (currentConv.value?.otherUser?.id === targetId) currentConv.value.otherUser.isOnline = v
}

const startChatWith = async (user) => {
  showFriendsDialog.value = false
  let conv = conversations.value.find(c => c.user1Id === user.id || c.user2Id === user.id)
  if (!conv) {
    try {
      await sendMessage({ receiverId: user.id, content: '👋', type: 'text' })
      await loadConversations()
      conv = conversations.value.find(c => c.user1Id === user.id || c.user2Id === user.id)
    } catch { ElMessage.error('发起会话失败'); return }
  }
  if (conv) selectConversation(conv)
}

const initChatWithUser = async (targetId) => {
  let conv = conversations.value.find(c => c.user1Id === targetId || c.user2Id === targetId)
  if (conv) { selectConversation(conv); return }
  try {
    const userRes = await getUser(targetId)
    if (userRes.success) {
      await sendMessage({ receiverId: targetId, content: '👋', type: 'text' })
      await loadConversations()
      conv = conversations.value.find(c => c.user1Id === targetId || c.user2Id === targetId)
      if (conv) selectConversation(conv)
    }
  } catch { ElMessage.error('发起会话失败') }
}

let ws = null
const connectWs = () => {
  const token = localStorage.getItem('token')
  if (!token) return
  ws = new WebSocket(`${config.wsBaseUrl}/ws/chat?token=${encodeURIComponent(token)}`)
  ws.onmessage = (e) => {
    try {
      const data = JSON.parse(e.data)
      if (data.type === 'new_message' && currentConv.value?.id === data.conversationId) {
        messages.value.push(data.message); nextTick(scrollToBottom)
      }
      if (data.type === 'presence_update') applyPresenceStatus(data.userId, data.online)
      else loadConversations()
    } catch {}
  }
  ws.onclose = () => setTimeout(connectWs, 3000)
}

onMounted(async () => {
  await loadConversations()
  await loadFriends()
  connectWs()
  window.addEventListener('resize', handleResize)
  const targetUserId = route.query.userId
  if (targetUserId) await initChatWithUser(Number(targetUserId))

  // iOS 键盘弹出时滚动到底部，防止输入框被遮挡
  if (/iPhone|iPad|iPod/i.test(navigator.userAgent)) {
    const inputEl = document.querySelector('.chat-input')
    if (inputEl) {
      inputEl.addEventListener('focus', () => {
        setTimeout(() => {
          inputEl.scrollIntoView({ behavior: 'smooth', block: 'end' })
          scrollToBottom()
        }, 350)
      })
    }
  }
})
onUnmounted(() => { ws?.close(); window.removeEventListener('resize', handleResize) })
</script>

<style scoped>
/* ===== 移动端消息列表页 ===== */
.mob-inbox {
  min-height: 100%;
  background: var(--paper);
  padding-bottom: calc(56px + env(safe-area-inset-bottom));
}
.mob-inbox-header {
  display: flex; align-items: center; justify-content: space-between;
  padding: 16px 20px 12px;
  position: sticky; top: 0; z-index: 10;
  background: rgba(253,250,242,0.92);
  backdrop-filter: blur(12px);
  border-bottom: 2px solid var(--ink);
}
.mob-inbox-title { font-size: 20px; font-weight: 700; color: var(--ink); display: flex; align-items: center; gap: 8px; }
.mob-inbox-title .stamp { margin-right: 0 !important; transform: rotate(-10deg) scale(0.85); }
.mob-icon-btn {
  width: 40px; height: 40px; border-radius: 50%; border: none;
  background: rgba(102,126,234,0.1); color: #667eea;
  display: flex; align-items: center; justify-content: center;
  cursor: pointer; -webkit-tap-highlight-color: transparent;
  transition: background 0.15s;
}
.mob-icon-btn:active { background: rgba(102,126,234,0.2); }

/* 骨架屏 */
.mob-loading { padding: 8px 0; }
.mob-skeleton {
  display: flex; align-items: center; gap: 12px;
  padding: 14px 20px;
}
.skel-avatar {
  width: 52px; height: 52px; border-radius: 50%;
  background: linear-gradient(90deg, #f0f0f0 25%, #e0e0e0 50%, #f0f0f0 75%);
  background-size: 200% 100%;
  animation: shimmer 1.4s infinite; flex-shrink: 0;
}
.skel-lines { flex: 1; display: flex; flex-direction: column; gap: 8px; }
.skel-line {
  height: 12px; border-radius: 6px;
  background: linear-gradient(90deg, #f0f0f0 25%, #e0e0e0 50%, #f0f0f0 75%);
  background-size: 200% 100%; animation: shimmer 1.4s infinite;
}
.skel-line--name { width: 40%; }
.skel-line--sub { width: 70%; }
@keyframes shimmer { to { background-position: -200% 0; } }

/* 空状态 */
.mob-empty {
  display: flex; flex-direction: column; align-items: center; gap: 12px;
  padding: 64px 0; color: #c0c4cc; font-size: 14px;
}
.mob-start-btn {
  margin-top: 8px; padding: 10px 24px;
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: #fff; font-size: 14px; font-weight: 600;
  border: none; border-radius: 24px; cursor: pointer;
  -webkit-tap-highlight-color: transparent;
}

/* 会话列表 */
.mob-conv-list { padding: 4px 0; }
.mob-conv-item {
  display: flex; align-items: center; gap: 14px;
  padding: 14px 20px; cursor: pointer;
  -webkit-tap-highlight-color: transparent;
  transition: background 0.15s;
}
.mob-conv-item:active { background: rgba(0,0,0,0.04); }
.mob-conv-avatar-wrap { position: relative; flex-shrink: 0; }
.online-dot {
  position: absolute; bottom: 1px; right: 1px;
  width: 11px; height: 11px; border-radius: 50%;
  background: #22c55e; border: 2px solid #fff;
}
.mob-unread-badge {
  position: absolute; top: -3px; right: -3px;
  min-width: 18px; height: 18px; padding: 0 4px;
  background: #f5576c; color: #fff;
  font-size: 10px; font-weight: 700; border-radius: 9px;
  border: 2px solid #fff;
  display: flex; align-items: center; justify-content: center; line-height: 1;
}
.mob-conv-info { flex: 1; overflow: hidden; min-width: 0; }
.mob-conv-top {
  display: flex; align-items: center; justify-content: space-between;
  margin-bottom: 4px;
}
.mob-conv-name { font-size: 15px; font-weight: 600; color: #1a1a2e; }
.mob-conv-time { font-size: 11px; color: #c0c4cc; flex-shrink: 0; }
.mob-conv-sub {
  font-size: 13px; color: #909399;
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
}
.mob-conv-sub--unread { color: #1a1a2e; font-weight: 500; }

/* 好友 Sheet */
.sheet-mask {
  position: fixed; inset: 0; z-index: 300;
  background: rgba(0,0,0,0.4); backdrop-filter: blur(3px);
  display: flex; align-items: flex-end;
}
.friends-sheet {
  width: 100%; max-height: 65dvh;
  background: rgba(255,255,255,0.97);
  backdrop-filter: blur(24px);
  border-radius: 20px 20px 0 0;
  border-top: 1px solid rgba(255,255,255,0.7);
  display: flex; flex-direction: column;
  padding-bottom: max(16px, env(safe-area-inset-bottom));
  overflow: hidden;
}
.sheet-handle-bar {
  width: 36px; height: 4px; background: rgba(0,0,0,0.12);
  border-radius: 2px; margin: 10px auto 0; flex-shrink: 0;
}
.friends-sheet-head {
  display: flex; align-items: center; justify-content: space-between;
  padding: 14px 20px 10px; flex-shrink: 0;
  border-bottom: 1px solid rgba(0,0,0,0.06);
}
.friends-sheet-title { font-size: 17px; font-weight: 700; color: #1a1a2e; }
.sheet-close-btn {
  width: 32px; height: 32px; border-radius: 50%; border: none;
  background: rgba(0,0,0,0.06); color: #666;
  display: flex; align-items: center; justify-content: center;
  cursor: pointer; -webkit-tap-highlight-color: transparent;
}
.friends-sheet-body { flex: 1; overflow-y: auto; padding: 4px 0; }
.friend-row {
  display: flex; align-items: center; gap: 12px;
  padding: 12px 20px; min-height: 60px; cursor: pointer;
  -webkit-tap-highlight-color: transparent; transition: background 0.15s;
}
.friend-row:active { background: rgba(0,0,0,0.04); }
.friend-row-avatar { position: relative; flex-shrink: 0; }
.friend-row-name { flex: 1; font-size: 15px; font-weight: 500; color: #1a1a2e; }

/* Sheet 动画 */
.sheet-enter-active { transition: opacity 0.25s ease; }
.sheet-leave-active { transition: opacity 0.2s ease; }
.sheet-enter-from, .sheet-leave-to { opacity: 0; }
.sheet-enter-from .friends-sheet, .sheet-leave-to .friends-sheet { transform: translateY(100%); }
.sheet-enter-active .friends-sheet { transition: transform 0.25s cubic-bezier(0.32,0.72,0,1); }
.sheet-leave-active .friends-sheet { transition: transform 0.2s ease-in; }

/* ===== PC + 移动端聊天窗口 ===== */
.chat-page {
  height: calc(100vh - var(--app-header-height, 56px));
  margin: 0 -1rem -2rem -1rem;
  padding: 20px;
  overflow: hidden;
}
.chat-wrapper {
  display: flex; height: 100%;
  background: var(--paper);
  border-radius: 18px;
  overflow: hidden;
  border: var(--border-sketch);
  box-shadow: 12px 12px 0px var(--subtle-blue);
}

/* PC 侧边栏 */
.sidebar { width: 300px; border-right: 1px solid #eee; display: flex; flex-direction: column; background: #fafafa; }
.sidebar-header {
  display: flex; justify-content: space-between; align-items: center;
  padding: 20px; border-bottom: 1px solid #eee; background: #fff; position: relative;
}
.sidebar-header .title { font-size: 18px; font-weight: 700; color: var(--ink); display: flex; align-items: center; gap: 8px; }
.sidebar-header .title .stamp { margin-right: 0 !important; transform: rotate(-10deg) scale(0.8); }
.friend-trigger { position: relative; }
.friend-dropdown {
  position: absolute; top: calc(100% + 8px); right: 0;
  width: 260px; max-height: 380px; background: #fff;
  border-radius: 12px; box-shadow: 0 4px 20px rgba(0,0,0,0.15);
  z-index: 1000; overflow: hidden;
}
.friend-dropdown-title { padding: 12px 16px; font-size: 14px; font-weight: 600; color: #333; border-bottom: 1px solid #eee; background: #fafafa; }
.friend-list { max-height: 320px; overflow-y: auto; padding: 8px; }
.friend-item { display: flex; align-items: center; gap: 12px; padding: 10px 12px; border-radius: 8px; cursor: pointer; transition: background 0.2s; }
.friend-item:hover { background: #f5f5f5; }
.friend-name { font-size: 14px; font-weight: 500; color: #333; flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.avatar-status-wrap { position: relative; display: inline-flex; }
.avatar-status-wrap::after { content: ''; position: absolute; right: 0; bottom: 0; width: 10px; height: 10px; border-radius: 50%; border: 2px solid #fff; background: #94a3b8; box-sizing: border-box; }
.avatar-status-wrap.online::after { background: #22c55e; }
.conv-list { flex: 1; overflow-y: auto; padding: 8px; }
.loading-state { display: flex; justify-content: center; padding: 40px; color: #999; }
.conv-item { display: flex; align-items: center; padding: 12px; border-radius: 12px; cursor: pointer; transition: all 0.2s; margin-bottom: 4px; }
.conv-item:hover { background: #f0f0f0; }
.conv-item.active { background: #667eea; }
.conv-item.active .conv-name, .conv-item.active .conv-msg { color: #fff; }
.conv-detail { flex: 1; margin-left: 12px; overflow: hidden; }
.conv-name { font-weight: 500; color: #333; margin-bottom: 3px; font-size: 14px; }
.conv-msg { font-size: 12px; color: #999; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.unread { margin-left: 8px; }

/* 聊天主区域 */
.main-chat { flex: 1; display: flex; flex-direction: column; background: #f8f9fa; min-width: 0; }
.chat-header {
  display: flex; align-items: center; gap: 10px;
  padding: 12px 16px; background: #fff; border-bottom: 1px solid #eee;
  flex-shrink: 0;
}
.back-icon-btn {
  width: 36px; height: 36px; border-radius: 50%; border: none;
  background: transparent; color: #333;
  display: flex; align-items: center; justify-content: center;
  cursor: pointer; -webkit-tap-highlight-color: transparent; flex-shrink: 0;
}
.back-icon-btn:active { background: rgba(0,0,0,0.06); }
.header-info { display: flex; align-items: center; gap: 8px; }
.header-info .name { font-size: 16px; font-weight: 600; color: #333; cursor: pointer; }
.header-info .name:hover { color: #667eea; }

.msg-list { flex: 1; overflow-y: auto; padding: 16px; display: flex; flex-direction: column; gap: 12px; }
.msg-item { display: flex; align-items: flex-end; gap: 8px; }
.msg-item.mine { flex-direction: row-reverse; }
.msg-bubble {
  max-width: 65%; padding: 10px 14px;
  background: #fff; border-radius: 18px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.08);
  word-break: break-word; line-height: 1.5; font-size: 15px;
}
.msg-item.mine .msg-bubble { background: #95ec69; }
.file-link { display: flex; align-items: center; gap: 6px; color: #667eea; text-decoration: none; font-size: 14px; }
.msg-item.mine .file-link { color: #2d6a2d; }
.msg-time { font-size: 11px; color: #bbb; white-space: nowrap; flex-shrink: 0; }

/* 输入区 */
.input-area {
  padding: 10px 12px;
  padding-bottom: max(10px, env(safe-area-inset-bottom));
  background: #fff; border-top: 1px solid #eee; flex-shrink: 0;
}.toolbar { display: flex; gap: 4px; margin-bottom: 8px; }
.tool-btn {
  width: 36px; height: 36px; border-radius: 8px; border: none;
  background: transparent; color: #666;
  display: flex; align-items: center; justify-content: center;
  cursor: pointer; -webkit-tap-highlight-color: transparent; transition: background 0.15s;
}
.tool-btn:hover { background: #f0f0f0; }
.tool-btn:active { background: #e8e8e8; }
.input-box { display: flex; gap: 8px; align-items: center; }
.chat-input {
  flex: 1; height: 40px; padding: 0 14px;
  border: 1.5px solid #e8e8e8; border-radius: 20px;
  font-size: 16px; /* 防 iOS 缩放 */ outline: none;
  background: #f7f8fa; color: #333; transition: border-color 0.2s;
}
.chat-input:focus { border-color: #667eea; background: #fff; }
.chat-input:disabled { opacity: 0.5; }
.send-btn {
  width: 40px; height: 40px; border-radius: 50%; border: none; flex-shrink: 0;
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: #fff; display: flex; align-items: center; justify-content: center;
  cursor: pointer; -webkit-tap-highlight-color: transparent;
  transition: transform 0.15s, opacity 0.15s;
}
.send-btn:disabled { opacity: 0.4; cursor: not-allowed; }
.send-btn:not(:disabled):active { transform: scale(0.92); }
.limit-tip { font-size: 12px; color: #f56c6c; margin-top: 6px; padding-left: 4px; }
.empty-chat { flex: 1; display: flex; align-items: center; justify-content: center; }

.dark .chat-wrapper {
  background: #222;
  border-color: #555;
  box-shadow: 12px 12px 0px #111;
}

/* 移动端聊天窗口全屏 */
@media (max-width: 768px) {
  .chat-page {
    position: fixed; inset: 0; margin: 0; padding: 0;
    height: 100dvh;
    /* 必须高于底部 tab bar 的 z-index:100，完全覆盖它 */
    z-index: 110;
  }
  .chat-wrapper { border-radius: 0; box-shadow: none; height: 100dvh; }
  .main-chat { height: 100dvh; display: flex; flex-direction: column; }
  .msg-list {
    flex: 1; overflow-y: auto;
    -webkit-overflow-scrolling: touch;
  }
  .chat-header {
    padding-top: max(12px, env(safe-area-inset-top));
    flex-shrink: 0;
  }
  .input-area {
    flex-shrink: 0;
    /* 全屏覆盖了 tab bar，只需留 safe-area */
    padding-bottom: max(10px, env(safe-area-inset-bottom));
  }
  .msg-bubble { max-width: 78%; }
}
</style>
