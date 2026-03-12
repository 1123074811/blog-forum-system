<template>
  <div class="chat-page">
    <div class="chat-wrapper">
      <!-- 左侧会话列表 -->
      <div class="sidebar" :class="{ 'mobile-hidden': currentConv && isMobile }">
        <div class="sidebar-header">
          <span class="title">私信</span>
          <div class="friend-trigger" 
               @mouseenter="handleMouseEnter" 
               @mouseleave="handleMouseLeave"
               @click="isMobile && (showFriendsDialog = !showFriendsDialog)">
            <el-button type="primary" size="small">好友</el-button>
            <!-- 好友列表悬浮层 -->
            <div v-if="showFriendsDialog" class="friend-dropdown" 
                 @mouseenter="handleMouseEnter" 
                 @mouseleave="handleMouseLeave">
              <div class="friend-dropdown-title">好友列表</div>
              <div class="friend-list">
                <div v-for="f in friends" :key="f.id" class="friend-item" @click="startChatWith(f)">
                  <el-avatar :src="f.avatar" :size="40">{{ (f.nickname || f.username || '?')[0] }}</el-avatar>
                  <span class="friend-name">{{ f.nickname || f.username }}</span>
                </div>
                <el-empty v-if="!friends.length" description="暂无互关好友" :image-size="60" />
              </div>
            </div>
          </div>
        </div>
        <div class="conv-list">
          <div v-if="loading" class="loading-state">
            <el-icon class="is-loading"><Loading /></el-icon>
          </div>
          <template v-else>
            <div v-for="conv in conversations" :key="conv.id"
                 :class="['conv-item', { active: currentConv?.id === conv.id }]"
                 @click="selectConversation(conv)">
              <el-avatar :src="conv.otherUser?.avatar" :size="48">
                {{ (conv.otherUser?.nickname || conv.otherUser?.username || '?')[0] }}
              </el-avatar>
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

      <!-- 右侧聊天区域 -->
      <div class="main-chat" :class="{ 'mobile-visible': currentConv && isMobile }">
        <template v-if="currentConv">
          <div class="chat-header">
            <el-button v-if="isMobile" text @click="backToList" class="back-btn">
              <el-icon :size="20"><ArrowLeft /></el-icon>
            </el-button>
            <el-avatar :src="currentConv.otherUser?.avatar" :size="40">
              {{ (currentConv.otherUser?.nickname || currentConv.otherUser?.username || '?')[0] }}
            </el-avatar>
            <div class="header-info">
              <span class="name">{{ currentConv.otherUser?.nickname || currentConv.otherUser?.username }}</span>
              <el-tag v-if="!isMutual" type="warning" size="small">非互关，仅能发1条</el-tag>
            </div>
          </div>
          <div class="msg-list" ref="msgListRef">
            <div v-for="msg in messages" :key="msg.id" :class="['msg-item', { mine: msg.senderId === userId }]">
              <el-avatar :src="msg.sender?.avatar" :size="36">
                {{ (msg.sender?.nickname || msg.sender?.username || '?')[0] }}
              </el-avatar>
              <div class="msg-bubble">
                <template v-if="msg.type === 'text'">{{ msg.content }}</template>
                <el-image v-else-if="msg.type === 'image'" :src="msg.fileUrl" fit="cover" style="max-width: 200px; border-radius: 8px;" :preview-src-list="[msg.fileUrl]" />
                <a v-else-if="msg.type === 'file'" :href="msg.fileUrl" :download="msg.fileName" class="file-link">
                  <el-icon><Document /></el-icon> {{ msg.fileName }}
                </a>
              </div>
              <span class="msg-time">{{ formatTime(msg.createdAt) }}</span>
            </div>
          </div>
          <div class="input-area">
            <div class="toolbar">
              <EmojiPicker @select="e => inputText += e" />
              <el-upload :show-file-list="false" :before-upload="handleImageUpload" accept="image/*">
                <el-button text><el-icon><Picture /></el-icon></el-button>
              </el-upload>
              <el-upload :show-file-list="false" :before-upload="handleFileUpload">
                <el-button text><el-icon><Paperclip /></el-icon></el-button>
              </el-upload>
            </div>
            <div class="input-box">
              <el-input v-model="inputText" placeholder="输入消息..." @keyup.enter="sendText" :disabled="sendDisabled" />
              <el-button type="primary" @click="sendText" :disabled="sendDisabled || !inputText.trim()">发送</el-button>
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
import { useRoute } from 'vue-router'
import { Loading, Picture, Paperclip, Document, ArrowLeft } from '@element-plus/icons-vue'
import { getConversations, getMessages, sendMessage, uploadMessageFile, getFriends, checkMutualFollow, getUser } from '@/api/blog'
import { ElMessage } from 'element-plus'
import EmojiPicker from '@/components/EmojiPicker.vue'
import config from '@/config'

const route = useRoute()
const userId = ref(JSON.parse(localStorage.getItem('user') || '{}').id)
const loading = ref(false)
const conversations = ref([])
const currentConv = ref(null)
const messages = ref([])
const inputText = ref('')
const showFriendsDialog = ref(false)
const friends = ref([])
const msgListRef = ref(null)
const isMutual = ref(true)
const sentCount = ref(0)
const targetUserInfo = ref(null)
const isMobile = ref(window.innerWidth <= 768)
let hideTimer = null

const sendDisabled = computed(() => !isMutual.value && sentCount.value >= 1)

// 监听窗口尺寸变化
const handleResize = () => {
  isMobile.value = window.innerWidth <= 768
}

const backToList = () => {
  currentConv.value = null
  messages.value = []
}

const handleMouseLeave = () => {
  if (!isMobile.value) {
    if (hideTimer) clearTimeout(hideTimer)
    hideTimer = setTimeout(() => {
      showFriendsDialog.value = false
    }, 300)
  }
}

const handleMouseEnter = () => {
  if (!isMobile.value) {
    if (hideTimer) clearTimeout(hideTimer)
    showFriendsDialog.value = true
  }
}

// 移动端点击外部关闭
const handleClickOutside = (event) => {
  if (isMobile.value && showFriendsDialog.value && !event.target.closest('.friend-trigger')) {
    showFriendsDialog.value = false
  }
}

const getUnread = (conv) => conv.user1Id === userId.value ? conv.user1Unread : conv.user2Unread

const formatTime = (t) => {
  if (!t) return ''
  const d = new Date(t)
  const now = new Date()
  const isToday = d.toDateString() === now.toDateString()
  const time = `${d.getHours().toString().padStart(2,'0')}:${d.getMinutes().toString().padStart(2,'0')}`
  return isToday ? time : `${d.getMonth()+1}/${d.getDate()} ${time}`
}

const loadConversations = async () => {
  loading.value = true
  try {
    const res = await getConversations()
    conversations.value = res.data || []
  } finally {
    loading.value = false
  }
}

const selectConversation = async (conv) => {
  currentConv.value = conv
  const res = await getMessages(conv.id)
  messages.value = res.data || []
  const otherUserId = conv.user1Id === userId.value ? conv.user2Id : conv.user1Id
  const mutualRes = await checkMutualFollow(otherUserId)
  isMutual.value = mutualRes.data ?? true
  sentCount.value = messages.value.filter(m => m.senderId === userId.value).length
  nextTick(() => scrollToBottom())
}

const scrollToBottom = () => {
  if (msgListRef.value) msgListRef.value.scrollTop = msgListRef.value.scrollHeight
}

const sendText = async () => {
  if (!inputText.value.trim() || sendDisabled.value) return
  const receiverId = currentConv.value.user1Id === userId.value ? currentConv.value.user2Id : currentConv.value.user1Id
  try {
    const res = await sendMessage({ receiverId, content: inputText.value, type: 'text' })
    if (res.success) {
      messages.value.push(res.data)
      inputText.value = ''
      sentCount.value++
      nextTick(() => scrollToBottom())
    } else {
      ElMessage.error(res.message || '发送失败')
    }
  } catch (e) {
    ElMessage.error('发送失败')
  }
}

const handleImageUpload = async (file) => {
  try {
    const uploadRes = await uploadMessageFile(file)
    const { url, name } = uploadRes.data
    const receiverId = currentConv.value.user1Id === userId.value ? currentConv.value.user2Id : currentConv.value.user1Id
    const res = await sendMessage({ receiverId, content: name, type: 'image', fileUrl: url, fileName: name })
    if (res.success) {
      messages.value.push(res.data)
      sentCount.value++
      nextTick(() => scrollToBottom())
    }
  } catch (e) {
    ElMessage.error('图片发送失败')
  }
  return false
}

const handleFileUpload = async (file) => {
  try {
    const uploadRes = await uploadMessageFile(file)
    const { url, name } = uploadRes.data
    const receiverId = currentConv.value.user1Id === userId.value ? currentConv.value.user2Id : currentConv.value.user1Id
    const res = await sendMessage({ receiverId, content: name, type: 'file', fileUrl: url, fileName: name })
    if (res.success) {
      messages.value.push(res.data)
      sentCount.value++
      nextTick(() => scrollToBottom())
    }
  } catch (e) {
    ElMessage.error('文件发送失败')
  }
  return false
}

const loadFriends = async () => {
  const res = await getFriends()
  friends.value = res.data || []
}

const startChatWith = async (user) => {
  showFriendsDialog.value = false
  let conv = conversations.value.find(c => c.user1Id === user.id || c.user2Id === user.id)
  if (!conv) {
    try {
      await sendMessage({ receiverId: user.id, content: '👋', type: 'text' })
      await loadConversations()
      conv = conversations.value.find(c => c.user1Id === user.id || c.user2Id === user.id)
    } catch (e) {
      ElMessage.error('发起会话失败')
      return
    }
  }
  if (conv) selectConversation(conv)
}

const initChatWithUser = async (targetId) => {
  // 先检查是否已有会话
  let conv = conversations.value.find(c => c.user1Id === targetId || c.user2Id === targetId)
  if (conv) {
    selectConversation(conv)
    return
  }
  // 没有会话，获取用户信息并创建
  try {
    const userRes = await getUser(targetId)
    if (userRes.success) {
      await sendMessage({ receiverId: targetId, content: '👋', type: 'text' })
      await loadConversations()
      conv = conversations.value.find(c => c.user1Id === targetId || c.user2Id === targetId)
      if (conv) selectConversation(conv)
    }
  } catch (e) {
    console.error('Init chat failed:', e)
    ElMessage.error('发起会话失败')
  }
}

// WebSocket
let ws = null
const connectWs = () => {
  const token = localStorage.getItem('token')
  if (!token) return
  ws = new WebSocket(`${config.wsBaseUrl}/ws/chat?token=${encodeURIComponent(token)}`)
  ws.onmessage = (e) => {
    try {
      const data = JSON.parse(e.data)
      if (data.type === 'new_message' && currentConv.value?.id === data.conversationId) {
        messages.value.push(data.message)
        nextTick(() => scrollToBottom())
      }
      loadConversations()
    } catch {}
  }
  ws.onclose = () => setTimeout(connectWs, 3000)
}

onMounted(async () => {
  await loadConversations()
  await loadFriends()
  connectWs()
  
  window.addEventListener('resize', handleResize)
  document.addEventListener('click', handleClickOutside)

  const targetUserId = route.query.userId
  if (targetUserId) {
    await initChatWithUser(Number(targetUserId))
  }
})

onUnmounted(() => {
  ws?.close()
  window.removeEventListener('resize', handleResize)
  document.removeEventListener('click', handleClickOutside)
})
</script>

<style scoped>
.chat-page {
  height: calc(100vh - var(--app-header-height));
  margin: 0 -1rem -2rem -1rem;
  padding: 20px;
  overflow: hidden;
}
.chat-wrapper {
  display: flex;
  height: 100%;
  background: #fff;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 20px 60px rgba(0,0,0,0.3);
}
.sidebar {
  width: 320px;
  border-right: 1px solid #eee;
  display: flex;
  flex-direction: column;
  background: #fafafa;
}
.sidebar-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px;
  border-bottom: 1px solid #eee;
  background: #fff;
  position: relative;
}
.sidebar-header .title {
  font-size: 18px;
  font-weight: 600;
  color: #333;
}
.friend-trigger {
  position: relative;
}
.friend-dropdown {
  position: absolute;
  top: calc(100% + 8px);
  right: 0;
  width: 280px;
  max-height: 400px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
  z-index: 1000;
  overflow: hidden;
  animation: slideDown 0.25s ease-out;
  transform-origin: top;
}
@keyframes slideDown {
  from {
    opacity: 0;
    transform: translateY(-10px) scaleY(0.95);
  }
  to {
    opacity: 1;
    transform: translateY(0) scaleY(1);
  }
}
.friend-dropdown-title {
  padding: 12px 16px;
  font-size: 14px;
  font-weight: 600;
  color: #333;
  border-bottom: 1px solid #eee;
  background: #fafafa;
}
.friend-list {
  max-height: 350px;
  overflow-y: auto;
  padding: 8px;
}
.friend-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.2s;
}
.friend-item:hover {
  background: #f5f5f5;
}
.friend-name {
  font-size: 14px;
  font-weight: 500;
  color: #333;
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.conv-list {
  flex: 1;
  overflow-y: auto;
  padding: 10px;
}
.loading-state {
  display: flex;
  justify-content: center;
  padding: 40px;
  color: #999;
}
.conv-item {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s;
  margin-bottom: 8px;
}
.conv-item:hover {
  background: #f0f0f0;
}
.conv-item.active {
  background: #4a9eff;
}
.conv-item.active .conv-name,
.conv-item.active .conv-msg {
  color: #fff;
}
.conv-detail {
  flex: 1;
  margin-left: 12px;
  overflow: hidden;
}
.conv-name {
  font-weight: 500;
  color: #333;
  margin-bottom: 4px;
}
.conv-msg {
  font-size: 13px;
  color: #999;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.unread {
  margin-left: 8px;
}
.main-chat {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: #f8f9fa;
}
.chat-header {
  display: flex;
  align-items: center;
  padding: 16px 24px;
  background: #fff;
  border-bottom: 1px solid #eee;
}
.header-info {
  margin-left: 12px;
  display: flex;
  align-items: center;
  gap: 10px;
}
.header-info .name {
  font-size: 16px;
  font-weight: 600;
  color: #333;
}
.msg-list {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
}
.msg-item {
  display: flex;
  align-items: flex-start;
  margin-bottom: 16px;
}
.msg-item.mine {
  flex-direction: row-reverse;
}
.msg-bubble {
  max-width: 60%;
  padding: 12px 16px;
  background: #fff;
  border-radius: 16px;
  margin: 0 12px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.08);
  word-break: break-word;
  line-height: 1.5;
}
.msg-item.mine .msg-bubble {
  background: #95ec69;
  color: #000;
}
.file-link {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #667eea;
  text-decoration: none;
}
.msg-item.mine .file-link {
  color: #fff;
}
.msg-time {
  font-size: 11px;
  color: #bbb;
  align-self: flex-end;
}
.input-area {
  padding: 16px 24px;
  background: #fff;
  border-top: 1px solid #eee;
}
.toolbar {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
}
.input-box {
  display: flex;
  gap: 12px;
}
.input-box .el-input {
  flex: 1;
}
.limit-tip {
  font-size: 12px;
  color: #f56c6c;
  margin-top: 8px;
}
.empty-chat {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}

.back-btn {
  margin-right: 8px;
  padding: 4px !important;
}

/* 移动端样式 */
@media (max-width: 768px) {
  .chat-page {
    margin: 0;
    padding: 0;
    height: 100vh;
    position: fixed;
    width: 100%;
    top: 0;
    left: 0;
    overflow: hidden;
  }
  
  .chat-wrapper {
    border-radius: 0;
    box-shadow: none;
  }
  
  .sidebar {
    width: 100%;
    transition: transform 0.3s ease;
  }
  
  .sidebar.mobile-hidden {
    transform: translateX(-100%);
    position: absolute;
    height: 100%;
    z-index: -1;
  }
  
  .main-chat {
    position: absolute;
    width: 100%;
    height: 100%;
    transform: translateX(100%);
    transition: transform 0.3s ease;
  }
  
  .main-chat.mobile-visible {
    transform: translateX(0);
  }
  
  .chat-header {
    padding: 12px 16px;
  }
  
  .msg-list {
    padding: 12px;
  }
  
  .msg-bubble {
    max-width: 75%;
  }
  
  .input-area {
    padding: 12px;
  }
  
  .sidebar-header {
    padding: 16px;
  }
  
  .conv-item {
    padding: 12px;
  }
  
  .friend-dropdown {
    width: 90vw;
    max-width: 320px;
    right: -10px;
  }
  
  .friend-list {
    max-height: 60vh;
  }
}
</style>
