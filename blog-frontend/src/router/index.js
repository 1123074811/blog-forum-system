import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'
import config from '@/config'
import { toast } from '@/utils/toast'

// 预加载最常访问的页面组件
const HomeComp = () => import('@/views/user/Home.vue')
const ArticleComp = () => import('@/views/user/Article.vue')
const LayoutComp = () => import('@/views/user/Layout.vue')

const routes = [
  {
    path: '/',
    component: LayoutComp,
    children: [
      { path: '', name: 'Home', component: HomeComp, meta: { title: '墨香阁' } },
      { path: 'article/:id', name: 'Article', component: ArticleComp, meta: { title: '品文轩' } },
      { path: 'user/:id', name: 'UserProfile', component: () => import('@/views/user/Profile.vue'), meta: { title: '访客苑' } },
      { path: 'write', name: 'Write', component: () => import('@/views/user/Write.vue'), meta: { requiresAuth: true, title: '挥墨斋' } },
      { path: 'edit/:id', name: 'Edit', component: () => import('@/views/user/Write.vue'), meta: { requiresAuth: true, title: '润笔阁' } },
      { path: 'search', name: 'Search', component: () => import('@/views/user/Search.vue'), meta: { title: '寻迹处' } },
      { path: 'discover', name: 'Discover', component: () => import('@/views/user/Discover.vue'), meta: { title: '发现' } },
      { path: 'er-diagram', name: 'ERDiagram', component: () => import('@/views/user/ERDiagram.vue'), meta: { title: 'ER图工具' } },
      { path: 'quiz', name: 'QuizList', component: () => import('@/views/user/QuizList.vue'), meta: { requiresAuth: true, title: '问学堂' } },
      { path: 'quiz/:id', name: 'Quiz', component: () => import('@/views/user/Quiz.vue'), meta: { requiresAuth: true, title: '答卷间' } },
      { path: 'album', name: 'Album', component: () => import('@/views/user/Album.vue'), meta: { requiresAuth: true, title: '藏影阁' } },
      { path: 'community', name: 'Community', component: () => import('@/views/user/Community.vue'), meta: { title: '览影廊' } },
      { path: 'favorites', name: 'Favorites', component: () => import('@/views/user/Favorites.vue'), meta: { requiresAuth: true, title: '珍藏阁' } },
      { path: 'tree-hole', name: 'TreeHole', component: () => import('@/views/user/TreeHole.vue'), meta: { title: '听风谷' } },
      { path: 'chat', name: 'Chat', component: () => import('@/views/user/Chat.vue'), meta: { requiresAuth: true, title: '私信' } },
      { path: 'messages', name: 'Messages', component: () => import('@/views/user/Messages.vue'), meta: { requiresAuth: true, title: '消息' } },
      { path: 'about', name: 'About', component: () => import('@/views/user/About.vue'), meta: { title: '关于我们' } },
      { path: 'life-simulator', name: 'LifeSimulator', component: () => import('@/views/user/LifeSimulator.vue'), meta: { title: '人生模拟器' } },
      { path: 'recitation', name: 'Recitation', component: () => import('@/views/user/Recitation.vue'), meta: { requiresAuth: true, title: '背书神器' } },
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
  'Home': ['Article', 'UserProfile', 'Write'],
  'Article': ['Home', 'UserProfile'],
  'UserProfile': ['Article', 'Home'],
}

router.afterEach((to) => {
  const toPreload = prefetchMap[to.name]
  if (!toPreload) return
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

  // 需要鉴权的页面，等待 session 初始化完成（只有第一次导航需要等待，之后立即返回）
  await userStore.sessionReady()

  if (to.meta.requiresAuth && !userStore.isLoggedIn) {
    next({ path: '/login', query: { redirect: to.fullPath, msg: '请先登录后再访问该页面' } })
    return
  }

  if (to.meta.requiresAdmin && !userStore.isAdmin) {
    // isAdmin 可能因为 initSession 时后端冷启动失败而未正确设置，重试一次
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
