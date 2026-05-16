<template>
  <component :is="styleComponent" v-bind="$attrs">
    <template v-for="(_, slotName) in $slots" #[slotName]="slotProps">
      <slot :name="slotName" v-bind="slotProps || {}" />
    </template>
  </component>
</template>

<script setup>
import { computed } from 'vue'
import config from '@/config'
import ClassicComponent from './AlbumClassic.vue'
import ModernComponent from './AlbumModern.vue'

defineOptions({ inheritAttrs: false })

const styleComponent = computed(() => config.layoutStyle === 'modern' ? ModernComponent : ClassicComponent)
</script>
