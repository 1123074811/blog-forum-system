import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'
import config from '@/config'
import { toast } from '@/utils/toast'

const pickStyleComponent = (classicLoader, modernLoader) => config.layoutStyle === 'modern' ? modernLoader : classicLoader

const UserLayoutComp = pickStyleComponent(() => import('@/views/user/LayoutClassic.vue'), () => import('@/views/user/LayoutModern.vue'))
const HomeComp = pickStyleComponent(() => import('@/views/user/HomeClassic.vue'), () => import('@/views/user/HomeModern.vue'))
const ArticleComp = pickStyleComponent(() => import('@/views/user/ArticleClassic.vue'), () => import('@/views/user/ArticleModern.vue'))
const UserProfileComp = pickStyleComponent(() => import('@/views/user/ProfileClassic.vue'), () => import('@/views/user/ProfileModern.vue'))
const WriteComp = pickStyleComponent(() => import('@/views/user/WriteClassic.vue'), () => import('@/views/user/WriteModern.vue'))
const SearchComp = pickStyleComponent(() => import('@/views/user/SearchClassic.vue'), () => import('@/views/user/SearchModern.vue'))
const DiscoverComp = pickStyleComponent(() => import('@/views/user/DiscoverClassic.vue'), () => import('@/views/user/DiscoverModern.vue'))
const QuizListComp = pickStyleComponent(() => import('@/views/user/QuizListClassic.vue'), () => import('@/views/user/QuizListModern.vue'))
const QuizComp = pickStyleComponent(() => import('@/views/user/QuizClassic.vue'), () => import('@/views/user/QuizModern.vue'))
const AlbumComp = pickStyleComponent(() => import('@/views/user/AlbumClassic.vue'), () => import('@/views/user/AlbumModern.vue'))
const CommunityComp = pickStyleComponent(() => import('@/views/user/CommunityClassic.vue'), () => import('@/views/user/CommunityModern.vue'))
const FavoritesComp = pickStyleComponent(() => import('@/views/user/FavoritesClassic.vue'), () => import('@/views/user/FavoritesModern.vue'))
const TreeHoleComp = pickStyleComponent(() => import('@/views/user/TreeHoleClassic.vue'), () => import('@/views/user/TreeHoleModern.vue'))
const ChatComp = pickStyleComponent(() => import('@/views/user/ChatClassic.vue'), () => import('@/views/user/ChatModern.vue'))
const MessagesComp = pickStyleComponent(() => import('@/views/user/MessagesClassic.vue'), () => import('@/views/user/MessagesModern.vue'))
const AboutComp = pickStyleComponent(() => import('@/views/user/AboutClassic.vue'), () => import('@/views/user/AboutModern.vue'))
const RecitationComp = pickStyleComponent(() => import('@/views/user/RecitationClassic.vue'), () => import('@/views/user/RecitationModern.vue'))

const routes = [
  {
    path: '/',
    component: UserLayoutComp,
    children: [
      { path: '', name: 'Home', component: HomeComp, meta: { title: '墨香阁' } },
      { path: 'article/:id', name: 'Article', component: ArticleComp, meta: { title: '品文轩' } },
      { path: 'user/:id', name: 'UserProfile', component: UserProfileComp, meta: { title: '访客苑' } },
      { path: 'write', name: 'Write', component: WriteComp, meta: { requiresAuth: true, title: '挥墨斋' } },
      { path: 'edit/:id', name: 'Edit', component: WriteComp, meta: { requiresAuth: true, title: '润笔阁' } },
      { path: 'search', name: 'Search', component: SearchComp, meta: { title: '寻迹处' } },
      { path: 'discover', name: 'Discover', component: DiscoverComp, meta: { title: '发现' } },
      { path: 'er-diagram', name: 'ERDiagram', component: () => import('@/views/user/ERDiagram.vue'), meta: { title: 'ER图工具' } },
      { path: 'quiz', name: 'QuizList', component: QuizListComp, meta: { requiresAuth: true, title: '问学堂' } },
      { path: 'quiz/:id', name: 'Quiz', component: QuizComp, meta: { requiresAuth: true, title: '答卷间' } },
      { path: 'album', name: 'Album', component: AlbumComp, meta: { requiresAuth: true, title: '藏影阁' } },
      { path: 'community', name: 'Community', component: CommunityComp, meta: { title: '览影廊' } },
      { path: 'favorites', name: 'Favorites', component: FavoritesComp, meta: { requiresAuth: true, title: '珍藏阁' } },
      { path: 'tree-hole', name: 'TreeHole', component: TreeHoleComp, meta: { title: '听风谷' } },
      { path: 'chat', name: 'Chat', component: ChatComp, meta: { requiresAuth: true, title: '私信' } },
      { path: 'messages', name: 'Messages', component: MessagesComp, meta: { requiresAuth: true, title: '消息' } },
      { path: 'about', name: 'About', component: AboutComp, meta: { title: '关于我们' } },
      { path: 'life-simulator', name: 'LifeSimulator', component: () => import('@/views/user/LifeSimulator.vue'), meta: { title: '人生模拟器' } },
      { path: 'recitation', name: 'Recitation', component: RecitationComp, meta: { requiresAuth: true, title: '背书神器' } },
      { path: 'pomodoro', name: 'Pomodoro', component: () => import('@/views/user/Pomodoro.vue'), meta: { title: '番茄时钟' } },
    ]
  },
  { path: '/login', name: 'Login', component: () => import('@/views/Login.vue'), meta: { title: '登临门' } },
  { path: '/register', name: 'Register', component: () => import('@/views/Register.vue'), meta: { title: '入门礼' } },
  { path: '/forgot-password', name: 'ForgotPassword', component: () => import('@/views/ForgotPassword.vue'), meta: { title: '寻钥阁' } },
  { path: '/oauth-callback', name: 'OAuthCallback', component: () => import('@/views/OAuthCallback.vue') },
  {
    path: '/admin',
    component: () => import('@/views/admin/Layout.vue'),
    meta: { requiresAuth: true, requiresAdmin: true },
    children: [
      { path: '', name: 'Dashboard', component: () => import('@/views/admin/Dashboard.vue'), meta: { title: '掌柜台' } },
      { path: 'users', name: 'AdminUsers', component: () => import('@/views/admin/Users.vue'), meta: { title: '名册簿' } },
      { path: 'articles', name: 'AdminArticles', component: () => import('@/views/admin/Articles.vue'), meta: { title: '文库阁' } },
      { path: 'comments', name: 'AdminComments', component: () => import('@/views/admin/Comments.vue'), meta: { title: '留言册' } },
      { path: 'categories', name: 'AdminCategories', component: () => import('@/views/admin/Categories.vue'), meta: { title: '分卷目' } },
      { path: 'tags', name: 'AdminTags', component: () => import('@/views/admin/Tags.vue'), meta: { title: '标签簿' } },
      { path: 'favorites', name: 'AdminFavorites', component: () => import('@/views/admin/Favorites.vue'), meta: { title: '收藏阁' } },
      { path: 'albums', name: 'AdminAlbums', component: () => import('@/views/admin/Albums.vue'), meta: { title: '影集馆' } },
      { path: 'media', name: 'AdminMedia', component: () => import('@/views/admin/Media.vue'), meta: { title: '素材库' } },
      { path: 'quiz', name: 'AdminQuiz', component: () => import('@/views/admin/Quiz.vue'), meta: { title: '题库房' } },
      { path: 'tree-holes', name: 'AdminTreeHoles', component: () => import('@/views/admin/TreeHoles.vue'), meta: { title: '树洞管理' } },
      { path: 'site-info', name: 'AdminSiteInfo', component: () => import('@/views/admin/SiteInfo.vue'), meta: { title: '网站设置' } },
      { path: 'announcements', name: 'AdminAnnouncements', component: () => import('@/views/admin/Announcements.vue'), meta: { title: '公告管理' } },
      { path: 'reports', name: 'AdminReports', component: () => import('@/views/admin/Reports.vue'), meta: { title: '内容审核' } },
      { path: 'ip-bans', name: 'AdminIpBans', component: () => import('@/views/admin/IpBans.vue'), meta: { title: 'IP封禁' } },
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior(to, from, savedPosition) {
    if (savedPosition) return savedPosition
    return { top: 0 }
  }
})

// 路由切换后预加载相邻页面（提升下次跳转速度）
const prefetchMap = {
  'Home': ['Article', 'UserProfile'],
  'Article': ['Home', 'UserProfile'],
  'UserProfile': ['Article', 'Home'],
}

router.afterEach((to) => {
  const toPreload = prefetchMap[to.name]
  if (!toPreload) return
  const connection = navigator.connection || navigator.mozConnection || navigator.webkitConnection
  const effectiveType = String(connection?.effectiveType || '').toLowerCase()
  if (connection?.saveData || effectiveType.includes('2g')) return
  // 利用浏览器空闲时间预加载
  if ('requestIdleCallback' in window) {
    requestIdleCallback(() => {
      toPreload.forEach(name => {
        const route = routes[0]?.children?.find(r => r.name === name)
        if (route?.component) route.component()
      })
    }, { timeout: 2000 })
  }
})

router.beforeEach(async (to, from, next) => {
  const userStore = useUserStore()
  document.title = to.meta.title ? `${to.meta.title} | ${config.siteName}` : config.siteName

  // 公开页面（登录/注册等）无需等待 session，直接放行，避免冷启动卡顿
  const isPublicOnly = to.path === '/login' || to.path === '/register' || to.path === '/forgot-password' || to.path === '/oauth-callback'
  if (isPublicOnly) {
    // 已登录用户不需要等 session，直接检查本地状态
    if (userStore.isLoggedIn) {
      next('/')
      return
    }
    next()
    return
  }

  // 仅在需要刷新 access token 或访问管理员页面时等待 session，避免普通页面首跳被 /auth/me 阻塞
  const hasRefreshToken = !!localStorage.getItem('refreshToken')
  const needsSession = !!to.meta.requiresAdmin || (!userStore.isLoggedIn && hasRefreshToken && !!to.meta.requiresAuth)
  if (needsSession) {
    await userStore.sessionReady()
  }

  if (to.meta.requiresAuth && !userStore.isLoggedIn) {
    next({ path: '/login', query: { redirect: to.fullPath, msg: '请先登录后再访问该页面' } })
    return
  }

  if (to.meta.requiresAdmin && !userStore.isAdmin) {
    if (userStore.isLoggedIn) {
      await userStore.checkAdminStatus()
    }

    if (!userStore.isAdmin) {
      toast('需要管理员权限才能访问')
      next('/')
      return
    }
  }

  next()
})

export default router
