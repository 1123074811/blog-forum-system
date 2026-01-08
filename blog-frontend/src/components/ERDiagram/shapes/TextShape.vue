<template>
  <v-group
    :config="groupConfig"
    @mousedown="handleMouseDown"
    @dblclick="handleDoubleClick"
  >
    <!-- 文本背景（可选） -->
    <v-rect v-if="showBackground" :config="backgroundConfig" />
    
    <!-- 文本内容 -->
    <v-text :config="textConfig" />
    
    <!-- 选中状态的边框 -->
    <v-rect v-if="selected" :config="selectionRectConfig" />
    
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
  text: {
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
  x: props.text.x,
  y: props.text.y,
  draggable: true
}))

// 计算文本尺寸
const textDimensions = computed(() => {
  const fontSize = props.text.fontSize || 14
  const text = props.text.text || ''
  const lines = text.split('\n')
  const maxLineLength = Math.max(...lines.map(line => line.length))
  
  return {
    width: Math.max(maxLineLength * fontSize * 0.6, 50),
    height: lines.length * fontSize * 1.2 + 10
  }
})

// 是否显示背景
const showBackground = computed(() => props.text.showBackground !== false)

// 背景配置
const backgroundConfig = computed(() => ({
  width: textDimensions.value.width + 10,
  height: textDimensions.value.height + 5,
  x: -5,
  y: -2,
  fill: props.text.backgroundColor || 'rgba(255, 255, 255, 0.8)',
  stroke: props.text.borderColor || '#ddd',
  strokeWidth: 1,
  cornerRadius: 4
}))

// 文本配置
const textConfig = computed(() => ({
  text: props.text.text || '新文本',
  fontSize: props.text.fontSize || 14,
  fontFamily: props.text.fontFamily || 'Arial',
  fill: props.text.fill || '#333',
  fontStyle: props.text.fontStyle || 'normal',
  textDecoration: props.text.textDecoration || '',
  align: props.text.align || 'left',
  verticalAlign: props.text.verticalAlign || 'top',
  width: textDimensions.value.width,
  height: textDimensions.value.height,
  padding: 5
}))

// 选中边框配置
const selectionRectConfig = computed(() => ({
  width: textDimensions.value.width + 10,
  height: textDimensions.value.height + 5,
  x: -5,
  y: -2,
  stroke: '#ff5722',
  strokeWidth: 2,
  dash: [5, 5],
  fill: 'transparent'
}))

// 调整大小控制点
const resizeHandles = computed(() => {
  const w = textDimensions.value.width + 10
  const h = textDimensions.value.height + 5
  const handleSize = 6
  
  return [
    // 四个角
    { x: -5 - handleSize/2, y: -2 - handleSize/2, radius: handleSize/2, fill: '#ff5722', cursor: 'nw-resize' },
    { x: w - 5 - handleSize/2, y: -2 - handleSize/2, radius: handleSize/2, fill: '#ff5722', cursor: 'ne-resize' },
    { x: w - 5 - handleSize/2, y: h - 2 - handleSize/2, radius: handleSize/2, fill: '#ff5722', cursor: 'se-resize' },
    { x: -5 - handleSize/2, y: h - 2 - handleSize/2, radius: handleSize/2, fill: '#ff5722', cursor: 'sw-resize' }
  ]
})

// 事件处理
function handleMouseDown(e) {
  e.cancelBubble = true
  emit('select', props.text.id, e.evt.ctrlKey || e.evt.metaKey)
}

function handleDoubleClick(e) {
  e.cancelBubble = true
  editText()
}

function startResize(e) {
  e.cancelBubble = true
  isResizing.value = true
}

function editText() {
  const newText = prompt('请输入文本内容:', props.text.text)
  if (newText !== null && newText !== props.text.text) {
    emit('update', props.text.id, { text: newText })
  }
}
</script>