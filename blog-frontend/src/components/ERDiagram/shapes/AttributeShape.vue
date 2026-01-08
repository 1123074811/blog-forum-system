<template>
  <v-group
    :config="groupConfig"
    @mousedown="handleMouseDown"
    @dblclick="handleDoubleClick"
  >
    <!-- 属性椭圆 -->
    <v-ellipse :config="ellipseConfig" />
    
    <!-- 属性名称 -->
    <v-text :config="textConfig" />
    
    <!-- 主键下划线 -->
    <v-line v-if="props.attribute.isPrimary" :config="underlineConfig" />
    
    <!-- 选中状态的边框 -->
    <v-ellipse v-if="selected" :config="selectionEllipseConfig" />
    
    <!-- 调整大小的控制点 -->
    <template v-if="selected">
      <v-circle
        v-for="(handle, index) in resizeHandles"
        :key="index"
        :config="handle"
        @mousedown="startResize"
      />
    </template>
  </v-group>
</template>

<script setup>
import { computed, ref } from 'vue'

const props = defineProps({
  attribute: {
    type: Object,
    required: true
  },
  selected: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['select', 'update', 'delete'])

const isDragging = ref(false)
const isResizing = ref(false)

// 组配置
const groupConfig = computed(() => ({
  x: props.attribute.x,
  y: props.attribute.y,
  draggable: true
}))

// 椭圆配置
const ellipseConfig = computed(() => ({
  x: props.attribute.width / 2,
  y: props.attribute.height / 2,
  radiusX: props.attribute.width / 2,
  radiusY: props.attribute.height / 2,
  fill: props.attribute.isPrimary ? '#fff3e0' : '#f3e5f5',
  stroke: props.attribute.isPrimary ? '#ff9800' : '#9c27b0',
  strokeWidth: 2
}))

// 文本配置
const textConfig = computed(() => {
  const textWidth = props.attribute.width - 10
  const fontSize = Math.min(12, textWidth / props.attribute.chineseName.length * 1.5)
  
  return {
    x: 5,
    y: props.attribute.height / 2 - fontSize / 2,
    text: props.attribute.chineseName,
    fontSize: fontSize,
    fontFamily: 'Arial',
    fill: '#333',
    width: textWidth,
    align: 'center'
  }
})

// 主键下划线配置
const underlineConfig = computed(() => {
  const textWidth = props.attribute.chineseName.length * 8 // 估算文本宽度
  const centerX = props.attribute.width / 2
  const textY = props.attribute.height / 2 + 6
  
  return {
    points: [
      centerX - textWidth / 2, textY,
      centerX + textWidth / 2, textY
    ],
    stroke: '#333',
    strokeWidth: 1
  }
})

// 选中边框配置
const selectionEllipseConfig = computed(() => ({
  x: props.attribute.width / 2,
  y: props.attribute.height / 2,
  radiusX: props.attribute.width / 2,
  radiusY: props.attribute.height / 2,
  stroke: '#ff5722',
  strokeWidth: 2,
  dash: [5, 5],
  fill: 'transparent'
}))

// 调整大小控制点
const resizeHandles = computed(() => {
  const w = props.attribute.width
  const h = props.attribute.height
  const handleSize = 6
  
  return [
    // 四个方向
    { x: -handleSize/2, y: h/2 - handleSize/2, radius: handleSize/2, fill: '#ff5722', cursor: 'w-resize' },
    { x: w - handleSize/2, y: h/2 - handleSize/2, radius: handleSize/2, fill: '#ff5722', cursor: 'e-resize' },
    { x: w/2 - handleSize/2, y: -handleSize/2, radius: handleSize/2, fill: '#ff5722', cursor: 'n-resize' },
    { x: w/2 - handleSize/2, y: h - handleSize/2, radius: handleSize/2, fill: '#ff5722', cursor: 's-resize' }
  ]
})

// 事件处理
function handleMouseDown(e) {
  e.cancelBubble = true
  emit('select', props.attribute.id, e.evt.ctrlKey || e.evt.metaKey)
}

function handleDoubleClick(e) {
  e.cancelBubble = true
  editAttribute()
}

function startResize(e) {
  e.cancelBubble = true
  isResizing.value = true
}

function editAttribute() {
  const newChineseName = prompt('请输入属性名称:', props.attribute.chineseName)
  if (newChineseName && newChineseName !== props.attribute.chineseName) {
    emit('update', props.attribute.id, { chineseName: newChineseName })
  }
  
  const isPrimary = confirm('是否为主键属性？')
  if (isPrimary !== props.attribute.isPrimary) {
    emit('update', props.attribute.id, { isPrimary })
  }
}
</script>