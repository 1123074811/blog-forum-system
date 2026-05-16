<template>
  <component :is="styleComponent" v-bind="$attrs">
    <template v-for="(_, slotName) in $slots" #[slotName]="slotProps">
      <slot :name="slotName" v-bind="slotProps || {}" />
    </template>
  </component>
</template>

<script setup>
import { defineAsyncComponent } from 'vue'
import config from '@/config'

defineOptions({ inheritAttrs: false })

const styleComponent = defineAsyncComponent(() => config.layoutStyle === 'modern'
  ? import('./QuizListModern.vue')
  : import('./QuizListClassic.vue')
)
</script>
