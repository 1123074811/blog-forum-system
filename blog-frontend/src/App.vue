<template>
  <el-config-provider :locale="zhCn">
    <div :class="{ dark: isDark }" class="min-h-screen">
      <router-view />
    </div>
  </el-config-provider>
</template>

<script setup>
import { computed } from 'vue'
import { useUserStore } from '@/stores/user'
import { ElConfigProvider } from 'element-plus'
import zhCn from 'element-plus/dist/locale/zh-cn.mjs'

const userStore = useUserStore()
const isDark = computed(() => userStore.isDark)
</script>
