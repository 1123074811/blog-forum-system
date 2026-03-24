<template>
  <div ref="containerRef" class="waterfall-root">
    <div
      v-for="(item, index) in items"
      :key="getKey(item, index)"
      :ref="el => setCellRef(el, index)"
      class="waterfall-cell"
      :style="getStyle(index)"
    >
      <slot :item="item" :index="index" />
    </div>
  </div>
</template>

<script setup>
import { ref, watch, onMounted, onUnmounted, nextTick } from 'vue'

const props = defineProps({
  items:    { type: Array,  default: () => [] },
  cols:     { type: Number, default: 4 },
  gap:      { type: Number, default: 16 },
  itemKey:  { type: String, default: 'id' },
})

const containerRef = ref(null)
const cellRefs     = ref([])
const positions    = ref([])   // { left, top, width }[]
const ready        = ref(false)

const getKey = (item, i) => item?.[props.itemKey] ?? i

const setCellRef = (el, i) => { if (el) cellRefs.value[i] = el }

const getStyle = (i) => {
  const p = positions.value[i]
  if (!p) return { position: 'absolute', opacity: '0', width: '0px' }
  return {
    position:   'absolute',
    left:       p.left  + 'px',
    top:        p.top   + 'px',
    width:      p.width + 'px',
    opacity:    '1',
    transition: ready.value ? 'top 0.25s ease, left 0.25s ease' : 'none',
  }
}

// ── 核心布局 ──────────────────────────────────────────────
const doLayout = () => {
  const container = containerRef.value
  if (!container) return

  const totalWidth = container.clientWidth
  if (!totalWidth) return

  const cols = props.cols
  const gap  = props.gap
  const cw   = (totalWidth - gap * (cols - 1)) / cols

  // 第一步：临时改为 relative 流式布局，设好宽度，让浏览器正确计算高度
  cellRefs.value.forEach(el => {
    if (!el) return
    el.style.position = 'relative'
    el.style.width    = cw + 'px'
    el.style.left     = ''
    el.style.top      = ''
    el.style.opacity  = '0'
  })

  // 强制回流，确保高度已计算
  void container.offsetHeight

  // 第二步：读取每个 cell 的真实高度
  const heights = cellRefs.value.map(el =>
    el ? el.getBoundingClientRect().height : cw * 0.75
  )

  // 第三步：计算瀑布流位置
  const colHeights = new Array(cols).fill(0)
  const newPos = []

  for (let i = 0; i < props.items.length; i++) {
    const minCol = colHeights.indexOf(Math.min(...colHeights))
    newPos[i] = { left: minCol * (cw + gap), top: colHeights[minCol], width: cw }
    colHeights[minCol] += (heights[i] || cw * 0.75) + gap
  }

  // 第四步：应用 absolute 定位
  positions.value = newPos
  container.style.height = Math.max(...colHeights) + 'px'
}

// 等容器内所有 <img> 加载完后再精确修正一次
const layoutAfterImages = () => {
  const container = containerRef.value
  if (!container) return
  const imgs = Array.from(container.querySelectorAll('img'))
  const pending = imgs.filter(img => !img.complete)
  if (pending.length === 0) { doLayout(); return }

  let done = 0
  const onDone = () => { if (++done >= pending.length) doLayout() }
  pending.forEach(img => {
    img.addEventListener('load',  onDone, { once: true })
    img.addEventListener('error', onDone, { once: true })
  })
}

// ── 触发时机 ──────────────────────────────────────────────
const layout = async () => {
  cellRefs.value = []
  await nextTick()
  doLayout()           // 第一次：用当前 DOM 高度（图片可能还没加载）
  layoutAfterImages()  // 第二次：图片全部加载后精确修正
  ready.value = true
}

watch(() => props.items, layout, { deep: false })
watch([() => props.cols, () => props.gap], doLayout)

let ro = null
onMounted(() => {
  ro = new ResizeObserver(doLayout)
  if (containerRef.value) ro.observe(containerRef.value)
  layout()
})
onUnmounted(() => ro?.disconnect())

defineExpose({ layout })
</script>

<style scoped>
.waterfall-root { position: relative; width: 100%; }
.waterfall-cell { box-sizing: border-box; }
</style>
