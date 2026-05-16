<template>
  <div class="layout-root">
        <!-- 全屏视差背景（仅首页显示） -->
    <section v-if="isHomeRoute" ref="heroSectionRef" class="hero-viewport">
      <div class="hero-illustration" id="parallaxBg">
        <div
          v-for="(wp, idx) in heroWallpapers"
          :key="`${wp.url}-${idx}`"
          class="hero-slide"
          :class="{ active: idx === heroWallpaperIndex }"
        >
          <img
            :src="normalizeUnsafeUrl(wp.url)"
            :alt="wp.title || 'Bing wallpaper'"
            :loading="idx === 0 ? 'eager' : 'lazy'"
            decoding="async"
            class="hero-slide-image"
          />
        </div>
      </div>
      <div class="hero-overlay"></div>
      <div class="main-title-box">
        <h1 class="site-logo">{{ config.siteName || '树欲静而风不止' }}</h1>
        <p class="site-subtitle">OUJINCONG.XYZ</p>
      </div>
          <button
        v-if="!isMobile"
        class="hero-next-btn"
        type="button"
        aria-label="scroll to next section"
        @click="scrollToHomeContent()"
      >
        <span></span>
      </button>
    </section>

    <!-- ===== PC 左侧竖排导航（仅桌面端显示）===== -->
    <nav v-if="!isMobile" class="fixed left-0 top-0 bottom-0 w-24 border-r-2 border-ink dark:border-[#333] bg-paper dark:bg-[#1a1a1a] z-50 flex flex-col items-center py-8 hidden md:flex">
      <!-- 站点 Logo -->
      <router-link to="/" class="mb-8 hover:-translate-y-1 transition-transform">
        <img :src="normalizeUnsafeUrl(config.logo)" alt="绔欑偣 logo" class="w-12 h-12 rounded-full border-2 border-ink dark:border-[#555]" />
      </router-link>

      <!-- 用户头像区域 -->
      <div class="mb-12 relative group/avatar">
        <template v-if="userStore.isLoggedIn">
          <el-dropdown placement="right-start" trigger="hover" :hide-timeout="200" popper-class="retro-dropdown" :show-timeout="0" effect="light" :offset="16">
            <div class="cursor-pointer flex flex-col items-center gap-2">
              <el-avatar :src="toAvatarThumb(userStore.user?.avatar, 72)" :size="48" class="border-2 border-ink dark:border-[#555] hover:-translate-y-1 transition-transform">
                {{ userStore.user?.username?.[0] }}
              </el-avatar>
            </div>
            <template #dropdown>
              <el-dropdown-menu class="font-serif !p-0 !bg-paper dark:!bg-[#1a1a1a] !border-2 !border-ink dark:!border-[#555] !rounded-none min-w-[140px] !flex !flex-col">
                <el-dropdown-item @click="router.push('/user/' + userStore.user?.id)" class="!text-ink dark:!text-gray-300 hover:!bg-subtleBlue dark:hover:!bg-[#333] !py-3 !px-4 text-center block">个人主页</el-dropdown-item>
                <el-dropdown-item @click="router.push('/favorites')" class="!text-ink dark:!text-gray-300 hover:!bg-subtleBlue dark:hover:!bg-[#333] !py-3 !px-4 text-center block">我的收藏</el-dropdown-item>
                <el-dropdown-item @click="router.push('/album')" class="!text-ink dark:!text-gray-300 hover:!bg-subtleBlue dark:hover:!bg-[#333] !py-3 !px-4 text-center block">我的相册</el-dropdown-item>
                <el-dropdown-item @click="router.push('/write')" class="!text-ink dark:!text-gray-300 hover:!bg-subtleBlue dark:hover:!bg-[#333] !py-3 !px-4 text-center block">写文章</el-dropdown-item>
                <el-dropdown-item @click="router.push('/about')" class="!text-ink dark:!text-gray-300 hover:!bg-subtleBlue dark:hover:!bg-[#333] !py-3 !px-4 text-center block">关于我们</el-dropdown-item>
                <el-dropdown-item v-if="userStore.isAdmin" @click="router.push('/admin')" class="!text-ink dark:!text-gray-300 hover:!bg-subtleBlue dark:hover:!bg-[#333] !py-3 !px-4 text-center block">管理后台</el-dropdown-item>
                <div class="h-[1px] w-full bg-ink/20 dark:bg-gray-700/50 my-1"></div>
                <el-dropdown-item @click="handleLogout" class="!text-accent hover:!bg-red-50 dark:hover:!bg-red-900/30 !py-3 !px-4 text-center block">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </template>
        <template v-else>
          <div class="cursor-pointer flex flex-col items-center gap-2" @click="router.push('/login')">
            <div class="w-12 h-12 rounded-full border-2 border-ink dark:border-[#555] flex items-center justify-center bg-white dark:bg-[#222] hover:-translate-y-1 transition-transform">
              <span class="font-cursive text-xl text-ink dark:text-gray-300">登</span>
            </div>
          </div>
        </template>
      </div>

      <div class="flex flex-col gap-10 flex-1 justify-center items-center">
        <router-link to="/" class="vertical-text text-lg tracking-[0.3em] text-ink dark:text-gray-300 hover:text-accent dark:hover:text-accent transition-colors relative group font-bold font-serif" :class="{ 'text-accent dark:!text-accent': route.path === '/' }">
          首页
          <div class="absolute right-[-10px] bottom-0 w-[2px] bg-accent transition-all duration-300" :class="route.path === '/' ? 'h-full' : 'h-0 group-hover:h-4/5'"></div>
        </router-link>
        <router-link to="/discover" class="vertical-text text-lg tracking-[0.3em] text-ink dark:text-gray-300 hover:text-accent dark:hover:text-accent transition-colors relative group font-bold font-serif" :class="{ 'text-accent dark:!text-accent': route.path === '/discover' }">
          发现
          <div class="absolute right-[-10px] bottom-0 w-[2px] bg-accent transition-all duration-300" :class="route.path === '/discover' ? 'h-full' : 'h-0 group-hover:h-4/5'"></div>
        </router-link>
        <router-link to="/community" class="vertical-text text-lg tracking-[0.3em] text-ink dark:text-gray-300 hover:text-accent dark:hover:text-accent transition-colors relative group font-bold font-serif" :class="{ 'text-accent dark:!text-accent': route.path === '/community' }">
          相册
          <div class="absolute right-[-10px] bottom-0 w-[2px] bg-accent transition-all duration-300" :class="route.path === '/community' ? 'h-full' : 'h-0 group-hover:h-4/5'"></div>
        </router-link>
        <router-link to="/tree-hole" class="vertical-text text-lg tracking-[0.3em] text-ink dark:text-gray-300 hover:text-accent dark:hover:text-accent transition-colors relative group font-bold font-serif" :class="{ 'text-accent dark:!text-accent': route.path === '/tree-hole' }">
          树洞
          <div class="absolute right-[-10px] bottom-0 w-[2px] bg-accent transition-all duration-300" :class="route.path === '/tree-hole' ? 'h-full' : 'h-0 group-hover:h-4/5'"></div>
        </router-link>
        <router-link to="/quiz" class="vertical-text text-lg tracking-[0.3em] text-ink dark:text-gray-300 hover:text-accent dark:hover:text-accent transition-colors relative group font-bold font-serif" :class="{ 'text-accent dark:!text-accent': route.path === '/quiz' }">
          刷题
          <div class="absolute right-[-10px] bottom-0 w-[2px] bg-accent transition-all duration-300" :class="route.path === '/quiz' ? 'h-full' : 'h-0 group-hover:h-4/5'"></div>
        </router-link>
      </div>

      <div class="flex flex-col items-center gap-6 mt-auto">

        <el-button :icon="Search" circle @click="showSearchDialog = true" class="!w-10 !h-10 !text-ink dark:!text-gray-300 !border-2 !border-ink dark:!border-[#555] dark:!bg-transparent hover:!bg-ink dark:hover:!bg-[#555] hover:!text-paper dark:hover:!text-white" />
        <el-button :icon="isDark ? Sunny : Moon" circle @click="userStore.toggleDark" class="!w-10 !h-10 !text-ink dark:!text-gray-300 !border-2 !border-ink dark:!border-[#555] dark:!bg-transparent hover:!bg-ink dark:hover:!bg-[#555] hover:!text-paper dark:hover:!text-white" />
        <el-button :icon="Headset" circle @click="musicStore.showPlayer++" class="!w-10 !h-10 !text-ink dark:!text-gray-300 !border-2 !border-ink dark:!border-[#555] dark:!bg-transparent hover:!bg-ink dark:hover:!bg-[#555] hover:!text-paper dark:hover:!text-white" />
        
        <template v-if="userStore.isLoggedIn">
          <el-popover placement="right" :width="320" trigger="hover" :show-after="200" popper-class="!bg-paper dark:!bg-[#1a1a1a] !border-2 !border-ink dark:!border-[#555] !rounded-none font-serif">
            <template #reference>
              <el-badge :value="totalUnread" :hidden="!totalUnread" :max="99" class="mb-4">
                <el-button :icon="Bell" circle class="!w-10 !h-10 !text-ink dark:!text-gray-300 !border-2 !border-ink dark:!border-[#555] dark:!bg-transparent hover:!bg-ink dark:hover:!bg-[#555] hover:!text-paper dark:hover:!text-white" />
              </el-badge>
            </template>
            <div class="max-h-80 overflow-y-auto" @wheel.stop>
              <div class="flex justify-between items-center mb-2 pb-2 border-b-2 border-ink">
                <span class="font-bold text-ink">消息通知</span>
                <span class="text-accent cursor-pointer hover:underline text-sm font-bold" @click="router.push('/messages')">进入消息中心 →</span>
              </div>
              <div v-if="!notifications.length && !conversations.length" class="text-center py-4 text-gray-500">暂无消息</div>
              <div v-for="n in notifications.slice(0, 5)" :key="n.id" class="p-2 hover:bg-subtleBlue cursor-pointer transition-colors border-b border-ink/10 last:border-0" @click="handleNotificationClick(n)">
                <div class="text-sm text-ink">{{ getNotificationText(n) }}</div>
              </div>
            </div>
          </el-popover>
        </template>
      </div>
    </nav>

    <!-- ===== 移动端顶部简化 Header ===== -->
    <header v-if="isMobile" class="mobile-header glass">
      <router-link to="/" class="flex items-center gap-2">
        <img :src="normalizeUnsafeUrl(config.logo)" alt="绔欑偣 logo" class="w-7 h-7 rounded-full" />
        <span class="text-base font-bold bg-gradient-to-r from-amber-500 to-orange-500 bg-clip-text text-transparent">{{ config.siteName }}</span>
      </router-link>
      <div class="flex items-center gap-2">
        <button class="mobile-icon-btn" @click="showSearchDialog = true" aria-label="搜索">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg>
        </button>
        <button class="mobile-icon-btn" @click="userStore.toggleDark" :aria-label="isDark ? '切换亮色' : '切换暗色'">
          <svg v-if="isDark" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="5"/><line x1="12" y1="1" x2="12" y2="3"/><line x1="12" y1="21" x2="12" y2="23"/><line x1="4.22" y1="4.22" x2="5.64" y2="5.64"/><line x1="18.36" y1="18.36" x2="19.78" y2="19.78"/><line x1="1" y1="12" x2="3" y2="12"/><line x1="21" y1="12" x2="23" y2="12"/><line x1="4.22" y1="19.78" x2="5.64" y2="18.36"/><line x1="18.36" y1="5.64" x2="19.78" y2="4.22"/></svg>
          <svg v-else width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z"/></svg>
        </button>
        <button class="mobile-icon-btn" @click="musicStore.showPlayer++" aria-label="音乐">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M9 18V5l12-2v13"/><circle cx="6" cy="18" r="3"/><circle cx="18" cy="16" r="3"/></svg>
        </button>
        <!-- 设置按钮（登录后显示） -->
        <div v-if="userStore.isLoggedIn" class="mobile-settings-wrap">
          <button class="mobile-icon-btn" @click="showMobileSettings = !showMobileSettings" aria-label="设置">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <circle cx="12" cy="12" r="3"/><path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1-2.83 2.83l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-4 0v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83-2.83l.06-.06A1.65 1.65 0 0 0 4.68 15a1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1 0-4h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 2.83-2.83l.06.06A1.65 1.65 0 0 0 9 4.68a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 4 0v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 2.83l-.06.06A1.65 1.65 0 0 0 19.4 9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 0 4h-.09a1.65 1.65 0 0 0-1.51 1z"/>
            </svg>
          </button>
          <Transition name="settings-drop">
            <div v-if="showMobileSettings" class="mobile-settings-menu">
              <button class="mobile-settings-item" @click="router.push(`/user/${userStore.user?.id}`); showMobileSettings = false">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>
                个人主页
              </button>
              <button class="mobile-settings-item" @click="router.push('/quiz'); showMobileSettings = false">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 20h9"/><path d="M16.5 3.5a2.121 2.121 0 0 1 3 3L7 19l-4 1 1-4L16.5 3.5z"/></svg>
                模拟刷题
              </button>
              <button class="mobile-settings-item" @click="router.push('/about'); showMobileSettings = false">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>
                关于我们
              </button>
              <button class="mobile-settings-item" @click="router.push('/album'); showMobileSettings = false">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="3" width="18" height="18" rx="2"/><circle cx="8.5" cy="8.5" r="1.5"/><polyline points="21 15 16 10 5 21"/></svg>
                我的相册
              </button>
              <button class="mobile-settings-item" @click="router.push('/community'); showMobileSettings = false">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/></svg>
                公共相册
              </button>
              <button v-if="userStore.isAdmin" class="mobile-settings-item" @click="router.push('/admin'); showMobileSettings = false">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 20h9"/><path d="M16.5 3.5a2.121 2.121 0 0 1 3 3L7 19l-4 1 1-4L16.5 3.5z"/></svg>
                管理后台
              </button>
              <div class="mobile-settings-divider" />
              <button class="mobile-settings-item mobile-settings-item--danger" @click="handleLogout; showMobileSettings = false">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/><polyline points="16 17 21 12 16 7"/><line x1="21" y1="12" x2="9" y2="12"/></svg>
                退出登录
              </button>
            </div>
          </Transition>
        </div>
      </div>
    </header>

    <!-- ===== 主内容区 ===== -->
    <main
      ref="homeContentRef"
      class="main-content px-2 sm:px-6 w-full"
      :class="{ 'is-home-content': isHomeRoute }"
    >
      <router-view />
    </main>

    <!-- ===== 移动端底部 Tab Bar ===== -->
    <nav v-if="isMobile" class="bottom-tab-bar">
      <!-- 首页 -->
      <button class="tab-item" :class="{ active: activeTab === 'home' }" @click="navigateTo('/', 'home')">
        <span class="tab-icon">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"/>
            <polyline points="9 22 9 12 15 12 15 22"/>
          </svg>
        </span>
        <span class="tab-label">首页</span>
      </button>

      <!-- 树洞 -->
      <button class="tab-item" :class="{ active: activeTab === 'treehole' }" @click="navigateTo('/tree-hole', 'treehole')">
        <span class="tab-icon">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/>
          </svg>
        </span>
        <span class="tab-label">树洞</span>
      </button>

      <!-- 发布（中间凸起按钮） -->
      <button class="tab-item tab-publish" @click="handlePublish" aria-label="发布文章">
        <span class="publish-btn">
          <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
        </span>
      </button>

      <!-- 消息（私信+通知） -->
      <button class="tab-item" :class="{ active: activeTab === 'inbox' }" @click="handleInboxTab">
        <span class="tab-icon">
          <span class="tab-badge-wrap">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"/><path d="M13.73 21a2 2 0 0 1-3.46 0"/>
            </svg>
            <span v-if="totalUnread > 0" class="tab-badge">{{ totalUnread > 99 ? '99+' : totalUnread }}</span>
          </span>
        </span>
        <span class="tab-label">消息</span>
      </button>

      <!-- 我的 -->
      <button class="tab-item" :class="{ active: activeTab === 'profile' }" @click="handleProfileTab">
        <span class="tab-icon">
          <el-avatar v-if="userStore.isLoggedIn" :src="toAvatarThumb(userStore.user?.avatar, 52)" :size="26" class="tab-avatar">{{ userStore.user?.username?.[0] }}</el-avatar>
          <svg v-else width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>
        </span>
        <span class="tab-label">我的</span>
      </button>
    </nav>

    <!-- ===== 消息 Sheet（私信 + 通知 Tabs）===== -->
    <Teleport to="body">
      <Transition name="sheet">
        <div v-if="showInboxSheet" class="sheet-mask" @click.self="showInboxSheet = false">
          <div class="inbox-sheet">
            <div class="sheet-handle-bar" />
            <!-- Tab 切换 -->
            <div class="inbox-tabs">
              <button class="inbox-tab" :class="{ 'inbox-tab--active': inboxTab === 'msg' }" @click="inboxTab = 'msg'">
                私信
                <span v-if="msgUnreadCount" class="inbox-tab-badge">{{ msgUnreadCount }}</span>
              </button>
              <button class="inbox-tab" :class="{ 'inbox-tab--active': inboxTab === 'notif' }" @click="inboxTab = 'notif'">
                通知
                <span v-if="unreadCount" class="inbox-tab-badge">{{ unreadCount }}</span>
              </button>
              <button v-if="inboxTab === 'notif' && unreadCount" class="inbox-mark-all" @click="handleMarkAllRead">全部已读</button>
              <button v-if="inboxTab === 'msg'" class="inbox-mark-all" @click="router.push('/chat'); showInboxSheet = false">全部 →</button>
            </div>
            <!-- 私信列表 -->
            <div v-if="inboxTab === 'msg'" class="inbox-body">
              <div v-if="!conversations.length" class="inbox-empty">
                <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="#d0d5dd" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/></svg>
                <p>暂无私信</p>
              </div>
              <div v-for="conv in conversations" :key="conv.id" class="inbox-row"
                   :class="{ 'inbox-row--unread': getConvUnread(conv) > 0 }"
                   @click="router.push(`/chat?userId=${conv.otherUser?.id}`); showInboxSheet = false">
                <div class="inbox-avatar-wrap">
                  <el-avatar :src="toAvatarThumb(conv.otherUser?.avatar, 96)" :size="48">{{ conv.otherUser?.username?.[0] }}</el-avatar>
                  <span v-if="getConvUnread(conv) > 0" class="inbox-dot">{{ getConvUnread(conv) > 9 ? '9+' : getConvUnread(conv) }}</span>
                </div>
                <div class="inbox-row-info">
                  <div class="inbox-row-name">{{ conv.otherUser?.nickname || conv.otherUser?.username }}</div>
                  <div class="inbox-row-sub">{{ conv.lastMessage?.content || '开始聊天吧' }}</div>
                </div>
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#c0c4cc" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="9 18 15 12 9 6"/></svg>
              </div>
            </div>
            <!-- 通知列表 -->
            <div v-if="inboxTab === 'notif'" class="inbox-body">
              <div v-if="!notifications.length" class="inbox-empty">
                <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="#d0d5dd" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"/><path d="M13.73 21a2 2 0 0 1-3.46 0"/></svg>
                <p>暂无通知</p>
              </div>
              <div v-for="n in notifications" :key="n.id" class="inbox-row"
                   :class="{ 'inbox-row--unread': !n.isRead }"
                   @click="handleNotificationClick(n); showInboxSheet = false">
                <div class="notif-icon-wrap" :class="`notif-icon--${n.type}`">
                  <svg v-if="n.type==='like'" width="18" height="18" viewBox="0 0 24 24" fill="currentColor"><path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"/></svg>
                  <svg v-else-if="n.type==='favorite'" width="18" height="18" viewBox="0 0 24 24" fill="currentColor"><polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"/></svg>
                  <svg v-else-if="n.type==='follow'" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><line x1="19" y1="8" x2="19" y2="14"/><line x1="22" y1="11" x2="16" y2="11"/></svg>
                  <svg v-else width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>
                </div>
                <div class="inbox-row-info">
                  <div class="inbox-row-name">{{ getNotificationText(n) }}</div>
                  <div class="inbox-row-sub">{{ n.createdAt }}</div>
                </div>
                <div v-if="!n.isRead" class="notif-unread-dot" />
              </div>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>

    <!-- 移动端搜索对话框 -->
    <el-dialog v-model="showSearchDialog" title="搜索" :width="isMobile ? '95%' : '500px'">
      <el-input v-model="searchQuery" placeholder="输入搜索关键词..." size="large" clearable autofocus @keyup.enter="handleSearch">
        <template #prefix><el-icon><Search /></el-icon></template>
      </el-input>
      <template #footer>
        <el-button @click="showSearchDialog = false">取消</el-button>
        <el-button type="primary" @click="handleSearch">搜索</el-button>
      </template>
    </el-dialog>

    <!-- 音乐播放器 -->
    <MusicPlayer />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick, watch, defineAsyncComponent } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { useMusicStore } from '@/stores/music'
import { Search, Bell, ChatDotRound, Headset, Sunny, Moon } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { getNotifications, getUnreadCount, markAsRead, markAllAsRead, getMessageUnreadCount, getConversations } from '@/api/blog'
import api from '@/api'
import config from '@/config'
import { normalizeUnsafeUrl, toAvatarThumb } from '@/utils/image'
const MusicPlayer = defineAsyncComponent(() => import('@/components/MusicPlayer.vue'))

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const musicStore = useMusicStore()

const isHomeRoute = computed(() => route.path === '/')
const isMobile = ref(window.innerWidth < 768)
const heroSectionRef = ref(null)
const homeContentRef = ref(null)
const isDark = ref(userStore.isDark)
const searchQuery = ref('')
const showSearchDialog = ref(false)
const showInboxSheet = ref(false)
const showMobileSettings = ref(false)
const inboxTab = ref('msg')

const activeTab = computed(() => {
  const p = route.path
  if (p === '/') return 'home'
  if (p.startsWith('/tree-hole')) return 'treehole'
  if (p.startsWith('/chat') || p.startsWith('/messages')) return 'inbox'
  if (p.startsWith('/user/') && route.params.id == userStore.user?.id) return 'profile'
  return ''
})

const notifications = ref([])
const unreadCount = ref(0)
const msgUnreadCount = ref(0)
const conversations = ref([])
const totalUnread = computed(() => unreadCount.value + msgUnreadCount.value)

const HERO_FALLBACK_WALLPAPER = 'https://images.unsplash.com/photo-1542931287-023b922fa89b?q=80&w=2000'
const HERO_ROTATE_INTERVAL = 8000
const heroWallpapers = ref([{ url: HERO_FALLBACK_WALLPAPER, title: 'Fallback wallpaper' }])
const heroWallpaperIndex = ref(0)
let heroRotationTimer = null
let heroWallpaperLoaded = false
let heroRetryTimer = null
let homeSectionSwitching = false

let ws = null
let _notifFetched = false
const ENTRY_SOURCE_KEY = 'site_entry_source_v1'

const handleResize = () => {
  isMobile.value = window.innerWidth < 768
  updateHeaderHeight()
}
window.addEventListener('resize', handleResize)

const updateHeaderHeight = () => {
  const header = document.querySelector('header')
  const h = header ? header.offsetHeight : 0
  document.documentElement.style.setProperty('--app-header-height', `${h}px`)
}

const setHeroWallpapers = (list) => {
  const safeList = (Array.isArray(list) ? list : [])
    .filter(item => item && typeof item.url === 'string' && item.url.trim())
    .map(item => ({ url: item.url.trim(), title: item.title || 'Bing wallpaper' }))

  heroWallpapers.value = safeList.length
    ? safeList
    : [{ url: HERO_FALLBACK_WALLPAPER, title: 'Fallback wallpaper' }]
  heroWallpaperIndex.value = 0
}

const stopHeroRotation = () => {
  if (heroRotationTimer) {
    clearInterval(heroRotationTimer)
    heroRotationTimer = null
  }
  if (heroRetryTimer) {
    clearTimeout(heroRetryTimer)
    heroRetryTimer = null
  }
}

const startHeroRotation = () => {
  stopHeroRotation()
  if (heroWallpapers.value.length <= 1) return
  heroRotationTimer = setInterval(() => {
    heroWallpaperIndex.value = (heroWallpaperIndex.value + 1) % heroWallpapers.value.length
  }, HERO_ROTATE_INTERVAL)
}

const ensureHeroWallpapers = async () => {
  if (heroWallpaperLoaded) {
    startHeroRotation()
    return
  }
  try {
    const res = await api.get('/wallpaper/bing')
    if (res?.success && Array.isArray(res.data) && res.data.length) {
      setHeroWallpapers(res.data)
      heroWallpaperLoaded = true
      startHeroRotation()
      return
    }
    throw new Error(res?.message || 'Empty wallpaper list')
  } catch (error) {
    console.error('获取首页必应壁纸失败:', error)
    startHeroRotation()
    if (!heroRetryTimer && isHomeRoute.value) {
      heroRetryTimer = setTimeout(() => {
        heroRetryTimer = null
        ensureHeroWallpapers().catch(() => {})
      }, 1800)
    }
  }
}

const scrollToHomeContent = () => {
  if (!isHomeRoute.value || !homeContentRef.value) return
  homeSectionSwitching = true
  window.scrollTo({
    top: homeContentRef.value.offsetTop,
    behavior: 'smooth'
  })
  setTimeout(() => {
    homeSectionSwitching = false
  }, 560)
}

const scrollToHomeHero = () => {
  if (!isHomeRoute.value || !heroSectionRef.value) return
  homeSectionSwitching = true
  window.scrollTo({
    top: heroSectionRef.value.offsetTop,
    behavior: 'smooth'
  })
  setTimeout(() => {
    homeSectionSwitching = false
  }, 560)
}

const handleHomeWheel = (event) => {
  if (!isHomeRoute.value || isMobile.value || homeSectionSwitching) return
  const delta = event.deltaY || 0
  if (Math.abs(delta) < 10) return
  const currentY = window.pageYOffset || document.documentElement.scrollTop
  const switchThreshold = window.innerHeight * 0.35

  if (delta > 0 && currentY < switchThreshold) {
    event.preventDefault()
    scrollToHomeContent()
  }
}

const getConvUnread = (conv) => conv.user1Id === userStore.user?.id ? conv.user1Unread : conv.user2Unread

const fetchNotifications = async () => {
  if (!userStore.isLoggedIn) return
  try {
    const [r1, r2, r3, r4] = await Promise.all([
      getNotifications(), getUnreadCount(), getMessageUnreadCount(), getConversations()
    ])
    if (r1.success) notifications.value = r1.data
    if (r2.success) unreadCount.value = r2.data.count
    if (r3.success) msgUnreadCount.value = r3.data
    if (r4.success) conversations.value = r4.data || []
    _notifFetched = true
  } catch (e) { console.error('鑾峰彇閫氱煡澶辫触', e) }
}

const connectWebSocket = () => {
  if (!userStore.isLoggedIn) return
  const token = localStorage.getItem('token')
  if (!token) return
  // 已有连接时不重复创建
  if (ws && ws.readyState === WebSocket.OPEN) return
  ws = new WebSocket(`${config.wsBaseUrl}/ws/notifications?token=${encodeURIComponent(token)}`)
  ws.onmessage = (e) => {
    const n = JSON.parse(e.data)
    notifications.value.unshift(n)
    unreadCount.value++
    ElMessage.info(getNotificationText(n))
  }
  ws.onerror = () => {}
}

const getNotificationText = (n) => {
  const types = { like: '赞了你的文章', favorite: '收藏了你的文章', follow: '关注了你' }
  return (n.fromUsername || '用户') + (types[n.type] || n.content)
}

const handleNotificationClick = async (n) => {
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

const navigateTo = (path, tab) => { router.push(path) }

const handlePublish = () => {
  if (!userStore.isLoggedIn) { router.push('/login'); return }
  router.push('/write')
}

const handleProfileTab = () => {
  if (!userStore.isLoggedIn) { router.push('/login'); return }
  router.push(`/user/${userStore.user?.id}`)
}

const handleInboxTab = () => {
  if (!userStore.isLoggedIn) { router.push('/login'); return }
  router.push('/messages')
}

const handleSearch = () => {
  if (searchQuery.value.trim()) {
    router.push({ path: '/search', query: { q: searchQuery.value } })
    showSearchDialog.value = false
    searchQuery.value = ''
  }
}

const handleLogout = () => {
  showMobileSettings.value = false
  userStore.logout()
  ElMessage.success('已退出登录')
  router.push('/')
}

// 关闭消息面板时重置为私信 Tab，避免下次打开停留在通知页
watch(showInboxSheet, (v) => {
  if (!v) inboxTab.value = 'msg'
})


// ===== 视差滚动逻辑 =====
const handleScroll = () => {
  if (route.path === '/') {
    const scroll = window.pageYOffset || document.documentElement.scrollTop
    const bg = document.getElementById('parallaxBg')
    if (bg) {
      bg.style.transform = `translateZ(-1px) translateY(${scroll * 0.4}px) scale(1.1)`
    }
  }
}

watch(() => route.path, (path) => {
  if (path === '/') {
    ensureHeroWallpapers().catch(() => {})
    handleScroll()
    return
  }
  stopHeroRotation()
})

onMounted(() => {
  window.addEventListener('scroll', handleScroll)
  window.addEventListener('wheel', handleHomeWheel, { passive: false })
  if (route.path === '/') {
    ensureHeroWallpapers().catch(() => {})
  }

  if (!sessionStorage.getItem(ENTRY_SOURCE_KEY)) {
    let ext = true
    if (document.referrer) {
      try { ext = new URL(document.referrer).origin !== window.location.origin } catch {}
    }
    sessionStorage.setItem(ENTRY_SOURCE_KEY, ext ? 'external' : 'internal')
  }
  if (userStore.isLoggedIn) {
    if (!_notifFetched) fetchNotifications()
    connectWebSocket()
  }
  nextTick(updateHeaderHeight)
  // 点击外部关闭设置菜单
  document.addEventListener('click', (e) => {
    const wrap = document.querySelector('.mobile-settings-wrap')
    if (wrap && !wrap.contains(e.target)) showMobileSettings.value = false
  })
})

onUnmounted(() => {
  window.removeEventListener('scroll', handleScroll)
  window.removeEventListener('wheel', handleHomeWheel)
  stopHeroRotation()

  ws?.close()
  window.removeEventListener('resize', handleResize)
})
</script>

<style scoped>
/* ===== 视差背景 ===== */
.hero-viewport {
  height: 100vh;
  width: 100%;
  position: relative;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
}
.hero-illustration {
  position: absolute;
  top: 0; left: 0; width: 110%; height: 110%;
  z-index: -2;
  overflow: hidden;
  transform: translateZ(-1px) scale(1.1);
  filter: sepia(0.2) contrast(0.9);
}

.hero-slide {
  position: absolute;
  inset: 0;
  opacity: 0;
  transition: opacity 1s ease-in-out;
}

.hero-slide.active {
  opacity: 1;
}

.hero-slide-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}
.hero-overlay {
  position: absolute;
  top: 0; left: 0; width: 100%; height: 100%;
  background: linear-gradient(180deg, rgba(253,250,242,0) 60%, var(--paper) 100%);
  z-index: -1;
}

.hero-next-btn {
  position: absolute;
  bottom: 26px;
  left: 50%;
  transform: translateX(-50%);
  width: 44px;
  height: 44px;
  border: 1px solid rgba(255, 255, 255, 0.65);
  border-radius: 999px;
  background: rgba(0, 0, 0, 0.18);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  transition: transform 0.2s ease, background-color 0.2s ease;
}

.hero-next-btn span {
  width: 10px;
  height: 10px;
  border-right: 2px solid #fff;
  border-bottom: 2px solid #fff;
  transform: rotate(45deg) translateY(-2px);
}

.hero-next-btn:hover {
  transform: translateX(-50%) translateY(3px);
  background: rgba(0, 0, 0, 0.3);
}

.main-title-box {
  text-align: center;
  z-index: 10;
}

.site-logo {
  font-family: 'Ma Shan Zheng', cursive;
  font-size: 6rem;
  color: var(--ink);
  text-shadow: 4px 4px 0px rgba(211, 84, 0, 0.2);
  line-height: 1;
}

.site-subtitle {
  font-size: 1.2rem;
  letter-spacing: 0.8em;
  margin-top: 20px;
  color: var(--accent);
  text-transform: uppercase;
}

.vertical-text {
  writing-mode: vertical-rl;
}

/* ===== 基础布局 ===== */
.layout-root { min-height: 100dvh; display: flex; flex-direction: column; }
.mobile-hide { display: none !important; }

/* PC 端主内容区让出左侧导航空间 */
.main-content {
  flex: 1;
  box-sizing: border-box;
  padding-top: 0;
}
@media (min-width: 768px) {
  .main-content { 
    padding-left: 104px; /* 预留左侧 96px 导航 + 少量间距 */
    padding-top: 0;
  }
}


/* ===== 移动端顶部 Header ===== */
.mobile-header {
  position: fixed; top: 0; left: 0; right: 0; z-index: 50;
  height: 52px;
  display: flex; align-items: center; justify-content: space-between;
  padding: 0 16px;
}
.mobile-icon-btn {
  width: 36px; height: 36px;
  display: flex; align-items: center; justify-content: center;
  border: none; background: transparent; border-radius: 50%;
  color: inherit; cursor: pointer;
  -webkit-tap-highlight-color: transparent;
  transition: background 0.15s;
}
.mobile-icon-btn:active { background: rgba(0,0,0,0.06); }

.dark .mobile-icon-btn:active { background: rgba(255,255,255,0.1); }

/* 移动端设置菜单 */
.mobile-settings-wrap { position: relative; }
.mobile-settings-menu {
  position: absolute; top: calc(100% + 8px); right: 0;
  min-width: 160px;
  background: rgba(255,255,255,0.97);
  backdrop-filter: blur(20px) saturate(180%);
  -webkit-backdrop-filter: blur(20px) saturate(180%);
  border-radius: 14px;
  box-shadow: 0 8px 32px rgba(0,0,0,0.18);
  overflow: hidden;
  z-index: 200;
}

.dark .mobile-settings-menu {
  background: rgba(30, 41, 59, 0.97);
  backdrop-filter: blur(20px) saturate(180%);
  -webkit-backdrop-filter: blur(20px) saturate(180%);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.5);
}
.mobile-settings-item {
  display: flex; align-items: center; gap: 10px;
  width: 100%; padding: 13px 16px;
  border: none; background: transparent;
  font-size: 14px; font-weight: 500; color: #1a1a2e;
  cursor: pointer; text-align: left;
  -webkit-tap-highlight-color: transparent;
  transition: background 0.15s;
}
.mobile-settings-item:active { background: rgba(0,0,0,0.05); }
.mobile-settings-item--danger { color: #f5576c; }

.dark .mobile-settings-item { color: #e2e8f0; }
.dark .mobile-settings-item:active { background: rgba(255,255,255,0.08); }
.dark .mobile-settings-item--danger { color: #f87171; }
.mobile-settings-divider { height: 1px; background: rgba(0,0,0,0.07); margin: 2px 0; }

.dark .mobile-settings-divider { background: rgba(255,255,255,0.1); }
.settings-drop-enter-active { transition: opacity 0.18s ease, transform 0.18s ease; }
.settings-drop-leave-active { transition: opacity 0.15s ease, transform 0.15s ease; }
.settings-drop-enter-from { opacity: 0; transform: translateY(-6px) scale(0.97); }
.settings-drop-leave-to { opacity: 0; transform: translateY(-6px) scale(0.97); }

/* 移动端 main 内容底部预留 Tab Bar 高度 */
@media (max-width: 767px) {
  .main-content {
    padding-top: calc(52px + 0.75rem);
    padding-bottom: calc(56px + env(safe-area-inset-bottom) + 8px);
    overflow-y: auto;
    height: auto;
    min-height: 100dvh;
  }
  /* 移动端首页：不锁定 overflow，保持自然滚动 */
  .main-content.is-home-content {
    padding-top: 52px;
    padding-bottom: calc(56px + env(safe-area-inset-bottom) + 8px);
    height: auto;
    min-height: 100dvh;
    overflow-y: auto;
  }
}

/* ===== 底部 Tab Bar ===== */
.bottom-tab-bar {
  position: fixed; bottom: 0; left: 0; right: 0; z-index: 100;
  height: calc(56px + env(safe-area-inset-bottom));
  padding-bottom: env(safe-area-inset-bottom);
  display: flex; align-items: stretch;
  background: rgba(255,255,255,0.92);
  backdrop-filter: blur(20px) saturate(180%);
  -webkit-backdrop-filter: blur(20px) saturate(180%);
  border-top: 1px solid rgba(0,0,0,0.06);
  box-shadow: 0 -4px 24px rgba(0,0,0,0.06);
}

.dark .bottom-tab-bar {
  background: rgba(30, 41, 59, 0.95);
  backdrop-filter: blur(20px) saturate(180%);
  -webkit-backdrop-filter: blur(20px) saturate(180%);
  border-top: 1px solid rgba(255, 255, 255, 0.08);
  box-shadow: 0 -4px 24px rgba(0, 0, 0, 0.4);
}
.tab-item {
  flex: 1; display: flex; flex-direction: column;
  align-items: center; justify-content: center; gap: 3px;
  border: none; background: transparent; cursor: pointer;
  padding: 6px 0; min-height: 56px;
  -webkit-tap-highlight-color: transparent;
  color: #8a8a9a;
  transition: color 0.2s;
  position: relative;
}
.tab-item.active { color: #1a1a2e; }

.dark .tab-item { color: #64748b; }
.dark .tab-item.active { color: #38bdf8; }
.tab-icon { display: flex; align-items: center; justify-content: center; position: relative; }
.tab-label { font-size: 10px; font-weight: 500; line-height: 1; }
.tab-item.active .tab-label { font-weight: 700; }

/* 头像 Tab */
.tab-avatar {
  border: 2px solid transparent;
  transition: border-color 0.2s;
}
.tab-item.active .tab-avatar { border-color: #1a1a2e; }

/* 未读徽标 */
.tab-badge-wrap { position: relative; display: inline-flex; }
.tab-badge {
  position: absolute; top: -6px; right: -10px;
  min-width: 16px; height: 16px; padding: 0 4px;
  background: #f5576c; color: #fff;
  font-size: 10px; font-weight: 700; border-radius: 8px;
  display: flex; align-items: center; justify-content: center;
  border: 1.5px solid rgba(255,255,255,0.9);
  line-height: 1;
}

/* 发布按钮（中间凸起） */
.tab-publish { flex: 1.2; }
.publish-btn {
  width: 48px; height: 48px; border-radius: 14px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex; align-items: center; justify-content: center;
  box-shadow: 0 4px 16px rgba(102,126,234,0.45);
  transition: transform 0.15s, box-shadow 0.15s;
  margin-bottom: 2px;
}
.tab-publish:active .publish-btn {
  transform: scale(0.93);
  box-shadow: 0 2px 8px rgba(102,126,234,0.3);
}

/* ===== 消息 Sheet ===== */
.sheet-mask {
  position: fixed; inset: 0; z-index: 200;
  background: rgba(0,0,0,0.4);
  backdrop-filter: blur(3px);
  display: flex; align-items: flex-end;
}
.inbox-sheet {
  width: 100%; max-height: 75dvh;
  background: rgba(255,255,255,0.96);
  backdrop-filter: blur(24px) saturate(180%);
  -webkit-backdrop-filter: blur(24px) saturate(180%);
  border-radius: 20px 20px 0 0;
  border-top: 1px solid rgba(255,255,255,0.7);
  display: flex; flex-direction: column;
  padding-bottom: max(16px, env(safe-area-inset-bottom));
  overflow: hidden;
}
.sheet-handle-bar {
  width: 36px; height: 4px;
  background: rgba(0,0,0,0.12); border-radius: 2px;
  margin: 10px auto 0; flex-shrink: 0;
}

/* Sheet tabs */
.inbox-tabs {
  display: flex; align-items: center;
  padding: 12px 20px 0; gap: 4px; flex-shrink: 0;
  border-bottom: 1px solid rgba(0,0,0,0.06);
}
.inbox-tab {
  display: flex; align-items: center; gap: 6px;
  padding: 8px 16px; border: none; background: transparent;
  font-size: 15px; font-weight: 500; color: #8a8a9a;
  border-bottom: 2px solid transparent; margin-bottom: -1px;
  cursor: pointer; transition: all 0.2s;
  -webkit-tap-highlight-color: transparent;
}
.inbox-tab--active { color: #1a1a2e; border-bottom-color: #667eea; font-weight: 700; }
.inbox-tab-badge {
  min-width: 16px; height: 16px; padding: 0 4px;
  background: #f5576c; color: #fff;
  font-size: 10px; font-weight: 700; border-radius: 8px;
  display: flex; align-items: center; justify-content: center; line-height: 1;
}
.inbox-mark-all {
  margin-left: auto; padding: 6px 12px;
  background: rgba(102,126,234,0.1); color: #667eea;
  font-size: 12px; font-weight: 600; border: none;
  border-radius: 20px; cursor: pointer;
  -webkit-tap-highlight-color: transparent;
  transition: background 0.15s;
}
.inbox-mark-all:active { background: rgba(102,126,234,0.2); }

/* Sheet body */
.inbox-body {
  flex: 1; overflow-y: auto;
  overscroll-behavior: contain;
  -webkit-overflow-scrolling: touch;
}
.inbox-empty {
  display: flex; flex-direction: column; align-items: center; gap: 12px;
  padding: 48px 0; color: #c0c4cc; font-size: 14px;
}
.inbox-row {
  display: flex; align-items: center; gap: 12px;
  padding: 14px 20px; min-height: 68px; cursor: pointer;
  transition: background 0.15s;
  -webkit-tap-highlight-color: transparent;
}
.inbox-row:active { background: rgba(0,0,0,0.04); }
.inbox-row--unread { background: rgba(102,126,234,0.05); }
.inbox-row--unread:active { background: rgba(102,126,234,0.1); }

.inbox-avatar-wrap { position: relative; flex-shrink: 0; }
.inbox-dot {
  position: absolute; top: -2px; right: -2px;
  min-width: 18px; height: 18px; padding: 0 4px;
  background: #f5576c; color: #fff;
  font-size: 10px; font-weight: 700; border-radius: 9px;
  border: 2px solid #fff;
  display: flex; align-items: center; justify-content: center; line-height: 1;
}
.inbox-row-info { flex: 1; overflow: hidden; min-width: 0; }
.inbox-row-name {
  font-size: 14px; font-weight: 500; color: #1a1a2e;
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis; line-height: 1.4;
}
.inbox-row-sub {
  font-size: 12px; color: #909399; margin-top: 3px;
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis; line-height: 1.4;
}

/* 通知图标 */
.notif-icon-wrap {
  width: 44px; height: 44px; border-radius: 12px; flex-shrink: 0;
  display: flex; align-items: center; justify-content: center;
}
.notif-icon--like { background: rgba(245,87,108,0.12); color: #f5576c; }
.notif-icon--favorite { background: rgba(245,158,11,0.12); color: #f59e0b; }
.notif-icon--follow { background: rgba(102,126,234,0.12); color: #667eea; }
.notif-icon--undefined, .notif-icon-- { background: rgba(0,0,0,0.06); color: #909399; }
.notif-unread-dot {
  width: 8px; height: 8px; border-radius: 50%;
  background: #667eea; flex-shrink: 0;
}

/* ===== Sheet 动画 ===== */
.sheet-enter-active { transition: opacity 0.25s ease, transform 0.25s cubic-bezier(0.32,0.72,0,1); }
.sheet-leave-active { transition: opacity 0.2s ease, transform 0.2s ease-in; }
.sheet-enter-from { opacity: 0; }
.sheet-leave-to { opacity: 0; }
.sheet-enter-from .inbox-sheet { transform: translateY(100%); }
.sheet-leave-to .inbox-sheet { transform: translateY(100%); }
.sheet-enter-to .inbox-sheet, .sheet-leave-from .inbox-sheet { transform: translateY(0); }

</style>

