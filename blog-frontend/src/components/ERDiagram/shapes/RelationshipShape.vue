<template>
  <v-group
    :config="groupConfig"
    @mousedown="handleMouseDown"
    @dblclick="handleDoubleClick"
  >
    <!-- 关系菱形 -->
    <v-line :config="diamondConfig" />
    
    <!-- 关系名称 -->
    <v-text :config="textConfig" />
    
    <!-- 选中状态的边框 -->
    <v-line v-if="selected" :config="selectionDiamondConfig" />
    
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
  relationship: {
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
  x: props.relationship.x,
  y: props.relationship.y,
  draggable: true
}))

// 菱形配置
const diamondConfig = computed(() => {
  const w = props.relationship.width
  const h = props.relationship.height
  const centerX = w / 2
  const centerY = h / 2
  
  return {
    points: [
      centerX, 0,        // 上
      w, centerY,        // 右
      centerX, h,        // 下
      0, centerY,        // 左
      centerX, 0         // 回到起点
    ],
    fill: '#e8f5e8',
    stroke: '#4caf50',
    strokeWidth: 2,
    closed: true
  }
})

// 文本配置
const textConfig = computed(() => {
  const fontSize = Math.min(12, props.relationship.width / props.relationship.chineseName.length * 1.2)
  
  return {
    x: 5,
    y: props.relationship.height / 2 - fontSize / 2,
    text: props.relationship.chineseName,
    fontSize: fontSize,
    fontFamily: 'Arial',
    fill: '#2e7d32',
    fontStyle: 'bold',
    width: props.relationship.width - 10,
    align: 'center'
  }
})

// 选中边框配置
const selectionDiamondConfig = computed(() => {
  const w = props.relationship.width
  const h = props.relationship.height
  const centerX = w / 2
  const centerY = h / 2
  
  return {
    points: [
      centerX, 0,
      w, centerY,
      centerX, h,
      0, centerY,
      centerX, 0
    ],
    stroke: '#ff5722',
    strokeWidth: 2,
    dash: [5, 5],
    closed: true,
    fill: 'transparent'
  }
})

// 调整大小控制点
const resizeHandles = computed(() => {
  const w = props.relationship.width
  const h = props.relationship.height
  const handleSize = 6
  const centerX = w / 2
  const centerY = h / 2
  
  return [
    // 四个顶点
    { x: centerX - handleSize/2, y: -handleSize/2, radius: handleSize/2, fill: '#ff5722', cursor: 'n-resize' },
    { x: w - handleSize/2, y: centerY - handleSize/2, radius: handleSize/2, fill: '#ff5722', cursor: 'e-resize' },
    { x: centerX - handleSize/2, y: h - handleSize/2, radius: handleSize/2, fill: '#ff5722', cursor: 's-resize' },
    { x: -handleSize/2, y: centerY - handleSize/2, radius: handleSize/2, fill: '#ff5722', cursor: 'w-resize' }
  ]
})

// 事件处理
function handleMouseDown(e) {
  e.cancelBubble = true
  emit('select', props.relationship.id, e.evt.ctrlKey || e.evt.metaKey)
}

function handleDoubleClick(e) {
  e.cancelBubble = true
  editRelationship()
}

function startResize(e) {
  e.cancelBubble = true
  isResizing.value = true
}

function editRelationship() {
  const newChineseName = prompt('请输入关系名称:', props.relationship.chineseName)
  if (newChineseName && newChineseName !== props.relationship.chineseName) {
    emit('update', props.relationship.id, { chineseName: newChineseName })
  }
}
</script>