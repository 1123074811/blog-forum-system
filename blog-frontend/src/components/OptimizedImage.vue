<template>
  <img
    :src="computedSrc"
    :alt="alt"
    :loading="lazy ? 'lazy' : undefined"
    :style="computedStyle"
    :class="imgClass"
    @error="onError"
  />
</template>

<script setup>
import { ref, computed } from 'vue'

const props = defineProps({
  src: { type: String, required: true },
  alt: { type: String, default: '' },
  lazy: { type: Boolean, default: true },
  type: { type: String, default: 'cover' },
  imgStyle: { type: Object, default: () => ({}) },
  imgClass: { type: String, default: '' },
})

const AVATAR_FALLBACK = 'data:image/svg+xml,' + encodeURIComponent(
  '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100"><rect fill="#e5e7eb" width="100" height="100"/><text x="50" y="55" text-anchor="middle" fill="#9ca3af" font-size="40">?</text></svg>'
)
const COVER_FALLBACK = 'data:image/svg+xml,' + encodeURIComponent(
  '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 400 200"><rect fill="#f3f4f6" width="400" height="200"/><text x="200" y="108" text-anchor="middle" fill="#d1d5db" font-size="24">No Image</text></svg>'
)

const FALLBACKS = { avatar: AVATAR_FALLBACK, cover: COVER_FALLBACK, media: COVER_FALLBACK }

const hasError = ref(false)
const computedSrc = computed(() => {
  if (hasError.value || !props.src) {
    return FALLBACKS[props.type] || FALLBACKS.cover
  }
  return props.src
})

const computedStyle = computed(() => ({
  objectFit: 'cover',
  ...props.imgStyle,
}))

const onError = () => {
  if (!hasError.value) {
    hasError.value = true
  }
}
</script>
