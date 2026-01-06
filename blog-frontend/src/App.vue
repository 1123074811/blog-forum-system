<template>
  <div :class="{ dark: isDark }" class="min-h-screen">
    <router-view />
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const isDark = computed(() => userStore.isDark)
</script>
