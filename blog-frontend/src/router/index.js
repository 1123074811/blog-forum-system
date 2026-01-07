import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'
import config from '@/config'

const routes = [
  {
    path: '/',
    component: () => import('@/views/user/Layout.vue'),
    children: [
      { path: '', name: 'Home', component: () => import('@/views/user/Home.vue'), meta: { title: '墨香阁' } },
      { path: 'article/:id', name: 'Article', component: () => import('@/views/user/Article.vue'), meta: { title: '品文轩' } },
      { path: 'user/:id', name: 'UserProfile', component: () => import('@/views/user/Profile.vue'), meta: { title: '访客苑' } },
      { path: 'write', name: 'Write', component: () => import('@/views/user/Write.vue'), meta: { requiresAuth: true, title: '挥墨斋' } },
      { path: 'edit/:id', name: 'Edit', component: () => import('@/views/user/Write.vue'), meta: { requiresAuth: true, title: '润笔阁' } },
      { path: 'search', name: 'Search', component: () => import('@/views/user/Search.vue'), meta: { title: '寻迹处' } },
      { path: 'discover', name: 'Discover', component: () => import('@/views/user/Discover.vue'), meta: { title: '发现' } },
      { path: 'quiz', name: 'QuizList', component: () => import('@/views/user/QuizList.vue'), meta: { requiresAuth: true, title: '问学堂' } },
      { path: 'quiz/:id', name: 'Quiz', component: () => import('@/views/user/Quiz.vue'), meta: { requiresAuth: true, title: '答卷间' } },
      { path: 'album', name: 'Album', component: () => import('@/views/user/Album.vue'), meta: { requiresAuth: true, title: '藏影阁' } },
      { path: 'community', name: 'Community', component: () => import('@/views/user/Community.vue'), meta: { title: '览影廊' } },
      { path: 'favorites', name: 'Favorites', component: () => import('@/views/user/Favorites.vue'), meta: { requiresAuth: true, title: '珍藏阁' } },
      { path: 'tree-hole', name: 'TreeHole', component: () => import('@/views/user/TreeHole.vue'), meta: { title: '听风谷' } },
      { path: 'chat', name: 'Chat', component: () => import('@/views/user/Chat.vue'), meta: { requiresAuth: true, title: '私信' } },
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
      { path: 'albums', name: 'AdminAlbums', component: () => import('@/views/admin/Albums.vue'), meta: { title: '影集馆' } },
      { path: 'media', name: 'AdminMedia', component: () => import('@/views/admin/Media.vue'), meta: { title: '素材库' } },
      { path: 'quiz', name: 'AdminQuiz', component: () => import('@/views/admin/Quiz.vue'), meta: { title: '题库房' } },
      { path: 'tree-holes', name: 'AdminTreeHoles', component: () => import('@/views/admin/TreeHoles.vue'), meta: { title: '树洞管理' } },
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const userStore = useUserStore()
  document.title = to.meta.title ? `${to.meta.title} | ${config.siteName}` : config.siteName

  if (to.meta.requiresAuth && !userStore.isLoggedIn) {
    next('/login')
  } else if (to.meta.requiresAdmin && !userStore.isAdmin) {
    next('/')
  } else {
    next()
  }
})

export default router
