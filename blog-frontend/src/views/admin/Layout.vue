<template>
  <div class="flex min-h-screen">
    <!-- 侧边栏 -->
    <aside class="w-64 glass fixed left-0 top-0 bottom-0 p-4">
      <div class="text-xl font-bold text-primary-600 mb-8">管理后台</div>
      <nav class="space-y-2">
        <router-link to="/admin" class="flex items-center gap-3 p-3 rounded-lg hover:bg-primary-50 dark:hover:bg-gray-700" :class="{ 'bg-primary-100 dark:bg-gray-700': route.path === '/admin' }">
          <el-icon><DataAnalysis /></el-icon>
          <span class="dark:text-white">仪表板</span>
        </router-link>
        <router-link to="/admin/users" class="flex items-center gap-3 p-3 rounded-lg hover:bg-primary-50 dark:hover:bg-gray-700" :class="{ 'bg-primary-100 dark:bg-gray-700': route.path === '/admin/users' }">
          <el-icon><User /></el-icon>
          <span class="dark:text-white">用户管理</span>
        </router-link>
        <router-link to="/admin/articles" class="flex items-center gap-3 p-3 rounded-lg hover:bg-primary-50 dark:hover:bg-gray-700" :class="{ 'bg-primary-100 dark:bg-gray-700': route.path === '/admin/articles' }">
          <el-icon><Document /></el-icon>
          <span class="dark:text-white">文章管理</span>
        </router-link>
        <router-link to="/admin/comments" class="flex items-center gap-3 p-3 rounded-lg hover:bg-primary-50 dark:hover:bg-gray-700" :class="{ 'bg-primary-100 dark:bg-gray-700': route.path === '/admin/comments' }">
          <el-icon><ChatDotRound /></el-icon>
          <span class="dark:text-white">评论管理</span>
        </router-link>
        <router-link to="/admin/categories" class="flex items-center gap-3 p-3 rounded-lg hover:bg-primary-50 dark:hover:bg-gray-700" :class="{ 'bg-primary-100 dark:bg-gray-700': route.path === '/admin/categories' }">
          <el-icon><Folder /></el-icon>
          <span class="dark:text-white">分类管理</span>
        </router-link>
        <router-link to="/admin/tags" class="flex items-center gap-3 p-3 rounded-lg hover:bg-primary-50 dark:hover:bg-gray-700" :class="{ 'bg-primary-100 dark:bg-gray-700': route.path === '/admin/tags' }">
          <el-icon><PriceTag /></el-icon>
          <span class="dark:text-white">标签管理</span>
        </router-link>
        <router-link to="/admin/albums" class="flex items-center gap-3 p-3 rounded-lg hover:bg-primary-50 dark:hover:bg-gray-700" :class="{ 'bg-primary-100 dark:bg-gray-700': route.path === '/admin/albums' }">
          <el-icon><Picture /></el-icon>
          <span class="dark:text-white">相册管理</span>
        </router-link>
        <router-link to="/admin/media" class="flex items-center gap-3 p-3 rounded-lg hover:bg-primary-50 dark:hover:bg-gray-700" :class="{ 'bg-primary-100 dark:bg-gray-700': route.path === '/admin/media' }">
          <el-icon><Film /></el-icon>
          <span class="dark:text-white">媒体管理</span>
        </router-link>
        <router-link to="/admin/quiz" class="flex items-center gap-3 p-3 rounded-lg hover:bg-primary-50 dark:hover:bg-gray-700" :class="{ 'bg-primary-100 dark:bg-gray-700': route.path === '/admin/quiz' }">
          <el-icon><List /></el-icon>
          <span class="dark:text-white">刷题管理</span>
        </router-link>
      </nav>
      <div class="absolute bottom-4 left-4 right-4">
        <router-link to="/" class="flex items-center gap-2 p-3 text-gray-600 hover:text-primary-500 dark:text-gray-400">
          <el-icon><Back /></el-icon>
          <span>返回前台</span>
        </router-link>
      </div>
    </aside>

    <!-- 主内容区 -->
    <main class="flex-1 ml-64 p-6">
      <router-view />
    </main>
  </div>
</template>

<script setup>
import { useRoute } from 'vue-router'
import { DataAnalysis, User, Document, ChatDotRound, Folder, PriceTag, Picture, Film, List, Back } from '@element-plus/icons-vue'

const route = useRoute()
</script>
