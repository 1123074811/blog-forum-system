<template>
  <div v-if="!isMobile" class="flex min-h-screen">
    <aside class="w-64 glass fixed left-0 top-0 bottom-0 p-4">
      <div class="text-xl font-bold text-primary-600 mb-8">管理后台</div>
      <nav class="space-y-2">
        <router-link v-for="item in fullMenus" :key="item.path" :to="item.path" class="flex items-center gap-3 p-3 rounded-lg hover:bg-primary-50 dark:hover:bg-gray-700" :class="{ 'bg-primary-100 dark:bg-gray-700': route.path === item.path }">
          <el-icon><component :is="item.icon" /></el-icon>
          <span class="dark:text-white">{{ item.label }}</span>
        </router-link>
      </nav>
      <div class="absolute bottom-4 left-4 right-4">
        <router-link to="/" class="flex items-center gap-2 p-3 text-gray-600 hover:text-primary-500 dark:text-gray-400">
          <el-icon><Back /></el-icon>
          <span>返回前台</span>
        </router-link>
      </div>
    </aside>

    <main class="flex-1 ml-64 p-6">
      <router-view />
    </main>
  </div>

  <div v-else class="mobile-admin-shell">
    <header class="mobile-admin-header glass">
      <el-button :icon="Menu" text @click="showMenu = true" />
      <div class="mobile-admin-title">{{ currentTitle }}</div>
      <el-button :icon="MoreFilled" text @click="showMenu = true" />
    </header>

    <main class="mobile-admin-main">
      <router-view />
    </main>

    <nav class="mobile-admin-nav glass">
      <button v-for="tab in mainTabs" :key="tab.key" class="mobile-tab" :class="{ active: currentTab === tab.key }" @click="goTab(tab)">
        <el-icon><component :is="tab.icon" /></el-icon>
        <span>{{ tab.label }}</span>
      </button>
    </nav>

    <el-drawer v-model="showMenu" direction="ltr" size="82%">
      <template #header>
        <div class="text-lg font-semibold">管理菜单</div>
      </template>
      <div class="mobile-menu-groups">
        <div v-for="group in menuGroups" :key="group.key" class="mobile-menu-group">
          <div class="mobile-menu-title">{{ group.label }}</div>
          <router-link v-for="item in group.items" :key="item.path" :to="item.path" class="mobile-menu-item" @click="showMenu = false">
            <el-icon><component :is="item.icon" /></el-icon>
            <span>{{ item.label }}</span>
          </router-link>
        </div>
      </div>
      <template #footer>
        <router-link to="/" class="mobile-back-link" @click="showMenu = false">
          <el-icon><Back /></el-icon>
          <span>返回前台</span>
        </router-link>
      </template>
    </el-drawer>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useIsMobile } from '@/composables/useIsMobile'
import { DataAnalysis, User, Document, ChatDotRound, Folder, PriceTag, Picture, Film, List, Back, ChatLineSquare, Setting, Bell, Star, Menu, MoreFilled, Warning } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const { isMobile } = useIsMobile()
const showMenu = ref(false)

const fullMenus = [
  { path: '/admin', label: '仪表盘', icon: DataAnalysis },
  { path: '/admin/users', label: '用户管理', icon: User },
  { path: '/admin/articles', label: '文章管理', icon: Document },
  { path: '/admin/comments', label: '评论管理', icon: ChatDotRound },
  { path: '/admin/categories', label: '分类管理', icon: Folder },
  { path: '/admin/tags', label: '标签管理', icon: PriceTag },
  { path: '/admin/favorites', label: '收藏管理', icon: Star },
  { path: '/admin/albums', label: '相册管理', icon: Picture },
  { path: '/admin/media', label: '媒体管理', icon: Film },
  { path: '/admin/quiz', label: '题库管理', icon: List },
  { path: '/admin/tree-holes', label: '树洞管理', icon: ChatLineSquare },
  { path: '/admin/site-info', label: '网站设置', icon: Setting },
  { path: '/admin/announcements', label: '公告管理', icon: Bell },
  { path: '/admin/reports', label: '内容审核', icon: Warning }
]

const mainTabs = [
  { key: 'dashboard', label: '仪表盘', path: '/admin', icon: DataAnalysis },
  { key: 'content', label: '内容', path: '/admin/articles', icon: Document },
  { key: 'users', label: '用户', path: '/admin/users', icon: User },
  { key: 'settings', label: '设置', path: '/admin/site-info', icon: Setting }
]

const currentTab = computed(() => {
  if (route.path === '/admin') return 'dashboard'
  if (route.path.startsWith('/admin/users')) return 'users'
  if (route.path.startsWith('/admin/site-info')) return 'settings'
  if (route.path.startsWith('/admin/announcements')) return 'settings'
  return 'content'
})

const menuGroups = computed(() => [
  {
    key: 'content',
    label: '内容管理',
    items: fullMenus.filter(m => ['/admin/articles', '/admin/comments', '/admin/categories', '/admin/tags', '/admin/favorites', '/admin/albums', '/admin/media', '/admin/quiz', '/admin/tree-holes', '/admin/reports'].includes(m.path))
  },
  {
    key: 'user',
    label: '用户管理',
    items: fullMenus.filter(m => ['/admin/users'].includes(m.path))
  },
  {
    key: 'system',
    label: '系统设置',
    items: fullMenus.filter(m => ['/admin', '/admin/site-info', '/admin/announcements'].includes(m.path))
  }
])

const currentTitle = computed(() => {
  const hit = fullMenus.find(i => i.path === route.path)
  if (hit) return hit.label
  if (route.path.startsWith('/admin/articles')) return '文章管理'
  if (route.path.startsWith('/admin/users')) return '用户管理'
  if (route.path.startsWith('/admin/comments')) return '评论管理'
  if (route.path.startsWith('/admin/reports')) return '内容审核'
  return '管理后台'
})

const goTab = (tab) => {
  if (route.path !== tab.path) router.push(tab.path)
}
</script>

<style scoped>
.mobile-admin-shell {
  min-height: 100vh;
  padding-bottom: calc(66px + env(safe-area-inset-bottom));
}

.mobile-admin-header {
  position: sticky;
  top: 0;
  z-index: 60;
  height: 54px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 8px;
}

.mobile-admin-title {
  font-size: 16px;
  font-weight: 600;
  color: #334155;
}

.mobile-admin-main {
  padding: 10px 10px 0;
}

.mobile-admin-nav {
  position: fixed;
  left: 8px;
  right: 8px;
  bottom: calc(8px + env(safe-area-inset-bottom));
  z-index: 70;
  border-radius: 14px;
  padding: 8px;
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 6px;
}

.mobile-tab {
  border: none;
  background: transparent;
  border-radius: 10px;
  height: 44px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 2px;
  color: #64748b;
  font-size: 11px;
}

.mobile-tab.active {
  color: #0ea5e9;
  background: rgba(14, 165, 233, 0.12);
}

.mobile-menu-groups {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.mobile-menu-title {
  color: #64748b;
  font-size: 13px;
  margin-bottom: 8px;
}

.mobile-menu-item {
  display: flex;
  align-items: center;
  gap: 10px;
  height: 42px;
  border-radius: 10px;
  padding: 0 10px;
  color: #334155;
}

.mobile-menu-item:hover {
  background: rgba(14, 165, 233, 0.12);
}

.mobile-back-link {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #64748b;
  font-size: 14px;
}
</style>
