<template>
  <el-drawer
    :model-value="modelValue"
    direction="btt"
    size="auto"
    :with-header="false"
    @close="$emit('update:modelValue', false)"
  >
    <div class="sheet-wrap">
      <div class="sheet-handle"></div>
      <div class="sheet-title">{{ title }}</div>
      <div class="sheet-content">
        <slot />
      </div>
    </div>
  </el-drawer>
</template>

<script setup>
defineProps({
  modelValue: { type: Boolean, default: false },
  title: { type: String, default: '操作' }
})

defineEmits(['update:modelValue'])
</script>

<style scoped>
.sheet-wrap {
  padding: 8px 14px calc(14px + env(safe-area-inset-bottom));
}

.sheet-handle {
  width: 42px;
  height: 4px;
  border-radius: 999px;
  margin: 0 auto 10px;
  background: rgba(100, 116, 139, 0.35);
}

.sheet-title {
  font-size: 16px;
  font-weight: 600;
  color: #334155;
  margin-bottom: 10px;
  text-align: center;
}

.sheet-content {
  max-height: 62vh;
  overflow: auto;
}
</style>
