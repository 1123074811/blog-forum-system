<template>
  <div class="mobile-list-shell glass rounded-xl p-3">
    <el-skeleton :loading="loading" animated :count="3">
      <template #template>
        <div class="skeleton-card">
          <el-skeleton-item variant="h3" style="width: 60%" />
          <el-skeleton-item variant="text" />
          <el-skeleton-item variant="text" />
        </div>
      </template>
      <template #default>
        <div v-if="items.length" class="mobile-card-list">
          <slot />
        </div>
        <el-empty v-else :description="emptyText" />
      </template>
    </el-skeleton>
  </div>
</template>

<script setup>
defineProps({
  loading: { type: Boolean, default: false },
  items: { type: Array, default: () => [] },
  emptyText: { type: String, default: '暂无数据' }
})
</script>

<style scoped>
.mobile-list-shell {
  min-height: 120px;
}

.mobile-card-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.skeleton-card {
  border-radius: 12px;
  padding: 12px;
  border: 1px solid rgba(148, 163, 184, 0.22);
  margin-bottom: 10px;
}
</style>
