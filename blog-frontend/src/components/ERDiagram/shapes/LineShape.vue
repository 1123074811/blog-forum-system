<template>
  <v-group>
    <!-- 连线 -->
    <v-line
      :config="lineConfig"
      @mousedown="handleMouseDown"
      @dblclick="handleDoubleClick"
    />
    
    <!-- 选中状态的高亮线 -->
    <v-line v-if="selected" :config="selectionLineConfig" />
    
    <!-- 控制点 -->
    <template v-if="selected">
      <v-circle
        v-for="(point, index) in controlPoints"
        :key="index"
        :config="point"
        @mousedown="startDragPoint"
      />
    </template>
    
    <!-- 箭头 -->
    <v-line v-if="showArrow" :config="arrowConfig" />
  </v-group>
</template>

<script setup>
import { computed, ref } from 'vue'

const props = defineProps({
  line: {
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

// 线条样式类型
const lineStyles = {
  straight: { dash: [] },
  dashed: { dash: [10, 5] },
  dotted: { dash: [2, 3] }
}

// 线条配置
const lineConfig = computed(() => {
  const style = lineStyles[props.line.style] || lineStyles.straight
  
  return {
    points: props.line.points || [0, 0, 100, 100],
    stroke: props.line.color || '#666',
    strokeWidth: props.line.strokeWidth || 2,
    lineCap: 'round',
    lineJoin: 'round',
    ...style
  }
})

// 选中状态线条配置
const selectionLineConfig = computed(() => ({
  points: props.line.points || [0, 0, 100, 100],
  stroke: '#ff5722',
  strokeWidth: (props.line.strokeWidth || 2) + 2,
  lineCap: 'round',
  lineJoin: 'round',
  opacity: 0.5
}))

// 控制点配置
const controlPoints = computed(() => {
  const points = props.line.points || [0, 0, 100, 100]
  const controlPoints = []
  
  for (let i = 0; i < points.length; i += 2) {
    controlPoints.push({
      x: points[i],
      y: points[i + 1],
      radius: 4,
      fill: '#ff5722',
      stroke: '#fff',
      strokeWidth: 1,
      cursor: 'move'
    })
  }
  
  return controlPoints
})

// 是否显示箭头
const showArrow = computed(() => props.line.showArrow !== false)

// 箭头配置
const arrowConfig = computed(() => {
  const points = props.line.points || [0, 0, 100, 100]
  if (points.length < 4) return { points: [] }
  
  // 获取最后两个点计算箭头方向
  const lastX = points[points.length - 2]
  const lastY = points[points.length - 1]
  const prevX = points[points.length - 4]
  const prevY = points[points.length - 3]
  
  // 计算角度
  const angle = Math.atan2(lastY - prevY, lastX - prevX)
  const arrowLength = 15
  const arrowAngle = Math.PI / 6
  
  // 计算箭头的两个端点
  const arrowX1 = lastX - arrowLength * Math.cos(angle - arrowAngle)
  const arrowY1 = lastY - arrowLength * Math.sin(angle - arrowAngle)
  const arrowX2 = lastX - arrowLength * Math.cos(angle + arrowAngle)
  const arrowY2 = lastY - arrowLength * Math.sin(angle + arrowAngle)
  
  return {
    points: [
      arrowX1, arrowY1,
      lastX, lastY,
      arrowX2, arrowY2
    ],
    stroke: props.line.color || '#666',
    strokeWidth: props.line.strokeWidth || 2,
    lineCap: 'round',
    lineJoin: 'round'
  }
})

// 事件处理
function handleMouseDown(e) {
  e.cancelBubble = true
  emit('select', props.line.id, e.evt.ctrlKey || e.evt.metaKey)
}

function handleDoubleClick(e) {
  e.cancelBubble = true
  editLine()
}

function startDragPoint(e) {
  e.cancelBubble = true
  isDragging.value = true
  // 这里可以添加拖拽控制点的逻辑
}

function editLine() {
  // 线条样式选择
  const styles = ['straight', 'dashed', 'dotted']
  const currentIndex = styles.indexOf(props.line.style || 'straight')
  const nextIndex = (currentIndex + 1) % styles.length
  const newStyle = styles[nextIndex]
  
  emit('update', props.line.id, { style: newStyle })
}
</script>