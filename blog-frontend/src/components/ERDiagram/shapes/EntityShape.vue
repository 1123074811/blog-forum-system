<template>
  <v-group
    :config="groupConfig"
    @mousedown="handleMouseDown"
    @dblclick="handleDoubleClick"
  >
    <!-- 实体矩形 -->
    <v-rect :config="rectConfig" />
    
    <!-- 实体名称 -->
    <v-text :config="nameTextConfig" />
    
    <!-- 中文名称 -->
    <v-text :config="chineseNameTextConfig" />
    
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
  entity: {
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
  x: props.entity.x,
  y: props.entity.y,
  draggable: true
}))

// 矩形配置
const rectConfig = computed(() => ({
  width: props.entity.width,
  height: props.entity.height,
  fill: '#e3f2fd',
  stroke: '#1976d2',
  strokeWidth: 2,
  cornerRadius: 8
}))

// 实体名称文本配置
const nameTextConfig = computed(() => ({
  x: 5,
  y: 8,
  text: props.entity.name,
  fontSize: 12,
  fontFamily: 'Arial',
  fill: '#666',
  width: props.entity.width - 10,
  align: 'center'
}))

// 中文名称文本配置
const chineseNameTextConfig = computed(() => ({
  x: 5,
  y: 25,
  text: props.entity.chineseName,
  fontSize: 14,
  fontFamily: 'Arial',
  fill: '#1976d2',
  fontStyle: 'bold',
  width: props.entity.width - 10,
  align: 'center'
}))

// 选中边框配置
const selectionRectConfig = computed(() => ({
  width: props.entity.width,
  height: props.entity.height,
  stroke: '#ff5722',
  strokeWidth: 2,
  dash: [5, 5],
  fill: 'transparent'
}))

// 调整大小控制点
const resizeHandles = computed(() => {
  const w = props.entity.width
  const h = props.entity.height
  const handleSize = 6
  
  return [
    // 四个角
    { x: -handleSize/2, y: -handleSize/2, radius: handleSize/2, fill: '#ff5722', cursor: 'nw-resize' },
    { x: w - handleSize/2, y: -handleSize/2, radius: handleSize/2, fill: '#ff5722', cursor: 'ne-resize' },
    { x: w - handleSize/2, y: h - handleSize/2, radius: handleSize/2, fill: '#ff5722', cursor: 'se-resize' },
    { x: -handleSize/2, y: h - handleSize/2, radius: handleSize/2, fill: '#ff5722', cursor: 'sw-resize' },
    // 四个边的中点
    { x: w/2 - handleSize/2, y: -handleSize/2, radius: handleSize/2, fill: '#ff5722', cursor: 'n-resize' },
    { x: w - handleSize/2, y: h/2 - handleSize/2, radius: handleSize/2, fill: '#ff5722', cursor: 'e-resize' },
    { x: w/2 - handleSize/2, y: h - handleSize/2, radius: handleSize/2, fill: '#ff5722', cursor: 's-resize' },
    { x: -handleSize/2, y: h/2 - handleSize/2, radius: handleSize/2, fill: '#ff5722', cursor: 'w-resize' }
  ]
})

// 事件处理
function handleMouseDown(e) {
  e.cancelBubble = true
  emit('select', props.entity.id, e.evt.ctrlKey || e.evt.metaKey)
}

function handleDoubleClick(e) {
  e.cancelBubble = true
  // 双击编辑实体名称
  editEntity()
}

function startResize(e) {
  e.cancelBubble = true
  isResizing.value = true
  // 这里可以添加调整大小的逻辑
}

function editEntity() {
  // 这里可以打开编辑对话框
  const newName = prompt('请输入实体名称:', props.entity.name)
  if (newName && newName !== props.entity.name) {
    emit('update', props.entity.id, { name: newName })
  }
  
  const newChineseName = prompt('请输入中文名称:', props.entity.chineseName)
  if (newChineseName && newChineseName !== props.entity.chineseName) {
    emit('update', props.entity.id, { chineseName: newChineseName })
  }
}
</script>