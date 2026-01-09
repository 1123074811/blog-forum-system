<template>
  <div class="er-diagram-page">
    <!-- 顶部工具栏 -->
    <div class="top-toolbar">
      <!-- 左侧：文件操作 -->
      <div class="toolbar-left">
        <el-button @click="goBack" :icon="ArrowLeft" size="small" text>
          返回
        </el-button>
        <el-divider direction="vertical" />
        <el-button @click="importSQL" type="primary" size="small" :icon="FolderOpened">
          导入SQL
        </el-button>
        <el-button @click="exportImage" size="small" :icon="Download">
          导出
        </el-button>
        <el-button @click="clearCanvas" size="small" :icon="Delete">
          清空
        </el-button>
      </div>

      <!-- 中间：编辑工具 -->
      <div class="toolbar-center">
        <el-button-group>
          <el-button
              size="small"
              :icon="Back"
              @click="undo"
              :disabled="historyIndex <= 0"
          >
            撤销
          </el-button>
          <el-button
              size="small"
              :icon="Right"
              @click="redo"
              :disabled="historyIndex >= history.length - 1"
          >
            恢复
          </el-button>
        </el-button-group>
        <el-divider direction="vertical" />
        <el-button-group>
          <el-button size="small" :icon="ZoomOut" @click="zoomOut"></el-button>
          <span class="zoom-display">{{ Math.round(scale * 100) }}%</span>
          <el-button size="small" :icon="ZoomIn" @click="zoomIn"></el-button>
        </el-button-group>
      </div>

      <!-- 右侧：视图控制 -->
      <div class="toolbar-right">
        <!-- 模式切换 -->
        <el-button-group>
          <el-button
              size="small"
              :type="viewMode === 'observe' ? 'primary' : ''"
              @click="setViewMode('observe')"
          >
            观察
          </el-button>
          <el-button
              size="small"
              :type="viewMode === 'edit' ? 'primary' : ''"
              @click="setViewMode('edit')"
          >
            编辑
          </el-button>
        </el-button-group>
        <el-divider direction="vertical" />

        <!-- 网格控制 -->
        <el-button
            @click="toggleGrid"
            size="small"
            :type="showGrid ? 'primary' : ''"
        >
          网格
        </el-button>
      </div>
    </div>

    <!-- 主要内容区域 -->
    <div class="main-content">
      <!-- 左侧工具面板 -->
      <div class="left-panel">
        <!-- 搜索框 -->
        <div class="search-section">
          <el-input
              v-model="searchText"
              placeholder="查找元素..."
              size="small"
              :prefix-icon="Search"
              clearable
          />
        </div>

        <!-- 基础图形 -->
        <div class="tool-section">
          <div class="section-header">
            <el-icon><Grid /></el-icon>
            <span>基础图形</span>
          </div>
          <div class="shape-grid">
            <!-- 实体 -->
            <div class="shape-item" :class="{ active: currentTool === 'entity' }" @click="setTool('entity')" title="实体">
              <div class="shape-rect"></div>
            </div>
            <!-- 弱实体 -->
            <div class="shape-item" :class="{ active: currentTool === 'weakEntity' }" @click="setTool('weakEntity')" title="弱实体">
              <div class="shape-rect-double"></div>
            </div>
            <!-- 属性 -->
            <div class="shape-item" :class="{ active: currentTool === 'attribute' }" @click="setTool('attribute')" title="属性">
              <div class="shape-ellipse"></div>
            </div>
            <!-- 多值属性 -->
            <div class="shape-item" :class="{ active: currentTool === 'multiAttribute' }" @click="setTool('multiAttribute')" title="多值属性">
              <div class="shape-ellipse-double"></div>
            </div>
            <!-- 派生属性 -->
            <div class="shape-item" :class="{ active: currentTool === 'derivedAttribute' }" @click="setTool('derivedAttribute')" title="派生属性">
              <div class="shape-ellipse-dashed"></div>
            </div>
            <!-- 关系 -->
            <div class="shape-item" :class="{ active: currentTool === 'relationship' }" @click="setTool('relationship')" title="关系">
              <div class="shape-diamond"></div>
            </div>
            <!-- 弱关系 -->
            <div class="shape-item" :class="{ active: currentTool === 'weakRelationship' }" @click="setTool('weakRelationship')" title="弱关系">
              <div class="shape-diamond-double"></div>
            </div>
          </div>
        </div>

        <!-- 连接线 -->
        <div class="tool-section">
          <div class="section-header">
            <el-icon><Connection /></el-icon>
            <span>连接线</span>
          </div>
          <div class="line-grid">
            <div
                class="line-item"
                :class="{ active: currentTool === 'line' && lineType === 'straight' }"
                @click="setLineTool('straight')"
                title="直线"
            >
              <span>直线</span>
            </div>
            <div
                class="line-item"
                :class="{ active: currentTool === 'line' && lineType === 'orthogonal' }"
                @click="setLineTool('orthogonal')"
                title="正交折线"
            >
              <span>正交</span>
            </div>
            <div
                class="line-item"
                :class="{ active: currentTool === 'line' && lineType === 'polyline' }"
                @click="setLineTool('polyline')"
                title="折线"
            >
              <span>折线</span>
            </div>
          </div>
          <!-- 箭头选项 -->
          <div class="line-options" v-if="currentTool === 'line'">
            <el-radio-group v-model="lineArrow" size="small">
              <el-radio-button value="none">无箭头</el-radio-button>
              <el-radio-button value="end">单向</el-radio-button>
              <el-radio-button value="both">双向</el-radio-button>
            </el-radio-group>
          </div>
        </div>

        <!-- 文本 -->
        <div class="tool-section">
          <div class="section-header">
            <el-icon><EditPen /></el-icon>
            <span>文本</span>
          </div>
          <div class="text-tools">
            <div
                class="text-item"
                :class="{ active: currentTool === 'text' }"
                @click="setTool('text')"
                title="文本框"
            >
              <el-icon><Document /></el-icon>
              <span>文本</span>
            </div>
            <div
                class="text-item"
                :class="{ active: currentTool === 'text-1' }"
                @click="setTool('text-1')"
                title="基数1"
            >
              <span class="text-label">1</span>
            </div>
            <div
                class="text-item"
                :class="{ active: currentTool === 'text-n' }"
                @click="setTool('text-n')"
                title="基数n"
            >
              <span class="text-label">n</span>
            </div>
            <div
                class="text-item"
                :class="{ active: currentTool === 'text-m' }"
                @click="setTool('text-m')"
                title="基数m"
            >
              <span class="text-label">m</span>
            </div>
          </div>
        </div>

        <!-- 对齐工具 -->
        <div class="tool-section" v-if="selectedItems.length >= 2">
          <div class="section-header">
            <el-icon><Grid /></el-icon>
            <span>对齐</span>
          </div>
          <div class="align-grid">
            <el-button size="small" @click="alignItems('left')" title="左对齐">⬅</el-button>
            <el-button size="small" @click="alignItems('center-h')" title="水平居中">⬌</el-button>
            <el-button size="small" @click="alignItems('right')" title="右对齐">➡</el-button>
            <el-button size="small" @click="alignItems('top')" title="顶对齐">⬆</el-button>
            <el-button size="small" @click="alignItems('center-v')" title="垂直居中">⬍</el-button>
            <el-button size="small" @click="alignItems('bottom')" title="底对齐">⬇</el-button>
          </div>
          <div class="distribute-grid">
            <el-button size="small" @click="distributeItems('horizontal')" title="水平分布">⇔</el-button>
            <el-button size="small" @click="distributeItems('vertical')" title="垂直分布">⇕</el-button>
          </div>
        </div>
      </div>

      <!-- 画布区域 -->
      <div class="canvas-area">
        <!-- 网格背景画布 -->
        <div class="canvas-container" ref="canvasContainer" style="position: relative;">
          <!-- 内联编辑输入框 -->
          <input
              v-if="isInlineEditing && inlineEditElement"
              id="inline-edit-input"
              v-model="inlineEditText"
              class="inline-edit-input"
              :style="getInlineEditStyle()"
              @blur="saveInlineEdit"
              @keyup.enter="saveInlineEdit"
              @keyup.escape="finishInlineEdit"
          />
          <v-stage
              ref="stage"
              :config="stageConfig"
              @wheel="handleWheel"
              @mousedown="handleStageMouseDown"
              @mousemove="handleStageMouseMove"
              @mouseup="handleStageMouseUp"
              @contextmenu="handleContextMenu"
          >
            <!-- 网格背景层 -->
            <v-layer ref="gridLayer" :config="{ visible: showGrid, name: 'grid-layer' }">
              <v-line
                  v-for="line in gridLines"
                  :key="line.id"
                  :config="line"
              />
            </v-layer>

            <!-- 主绘图层 -->
            <v-layer ref="mainLayer">
              <!-- 连线层 -->
              <v-line
                  v-for="line in connectionLines"
                  :key="line.id"
                  :config="line.config"
              />

              <!-- 手动绘制的线条 -->
              <v-arrow
                  v-for="line in manualLines"
                  :key="line.id"
                  :config="{
                  points: line.points,
                  stroke: isItemSelected(line.id) ? '#1976d2' : line.stroke,
                  strokeWidth: isItemSelected(line.id) ? 3 : line.strokeWidth,
                  fill: isItemSelected(line.id) ? '#1976d2' : '#000000',
                  lineCap: 'round',
                  tension: line.tension || 0,
                  pointerLength: (line.arrow === 'end' || line.arrow === 'both') ? 10 : 0,
                  pointerWidth: (line.arrow === 'end' || line.arrow === 'both') ? 10 : 0,
                  pointerAtBeginning: line.arrow === 'both',
                  pointerAtEnding: line.arrow === 'end' || line.arrow === 'both'
                }"
                  @click="handleItemClick(line, 'line', $event)"
              />

              <!-- 预览线 -->
              <v-line
                  v-if="previewLine"
                  :config="{
                  points: previewLine,
                  stroke: '#409eff',
                  strokeWidth: 2,
                  dash: [5, 5]
                }"
              />

              <!-- 实体 -->
              <v-group
                  v-for="entity in entities"
                  :key="entity.id"
                  :config="{ x: entity.x, y: entity.y, draggable: viewMode === 'edit' && !isResizing }"
                  @dragstart="handleDragStart"
                  @dragend="updateEntityPosition(entity.id, $event)"
                  @dragmove="updateEntityPosition(entity.id, $event)"
                  @dblclick="handleEntityDoubleClick(entity)"
                  @click="handleItemClick(entity, 'entity', $event)"
                  @mouseenter="currentTool === 'line' && showConnectionPoints(entity)"
                  @mouseleave="hideConnectionPoints(entity)"
              >
                <v-rect :config="getEntityRectConfig(entity)" />
                <v-text
                    v-if="!isInlineEditing || inlineEditElement?.item.id !== entity.id"
                    :config="getEntityTextConfig(entity)"
                />
                <!-- 调整大小控制点 -->
                <template v-if="isItemSelected(entity.id) && viewMode === 'edit'">
                  <v-circle v-for="h in getResizeHandles(entity)" :key="h.type" :config="h.config" @mousedown="startResize(entity, h.type, $event)" />
                </template>
              </v-group>

              <!-- 关系菱形 -->
              <v-group
                  v-for="relationship in relationships"
                  :key="relationship.id"
                  :config="{ x: relationship.x, y: relationship.y, draggable: viewMode === 'edit' && !isResizing }"
                  @dragstart="handleDragStart"
                  @dragend="updateRelationshipPosition(relationship.id, $event)"
                  @dragmove="updateRelationshipPosition(relationship.id, $event)"
                  @dblclick="handleRelationshipDoubleClick(relationship)"
                  @click="handleItemClick(relationship, 'relationship', $event)"
                  @mouseenter="currentTool === 'line' && showConnectionPoints(relationship)"
                  @mouseleave="hideConnectionPoints(relationship)"
              >
                <v-line :config="getRelationshipDiamondConfig(relationship)" />
                <v-text
                    v-if="!isInlineEditing || inlineEditElement?.item.id !== relationship.id"
                    :config="getRelationshipTextConfig(relationship)"
                />
                <!-- 调整大小控制点 -->
                <template v-if="isItemSelected(relationship.id) && viewMode === 'edit'">
                  <v-circle v-for="h in getResizeHandles(relationship)" :key="h.type" :config="h.config" @mousedown="startResize(relationship, h.type, $event)" />
                </template>
              </v-group>

              <!-- 属性 -->
              <v-group
                  v-for="attribute in attributes"
                  :key="attribute.id"
                  :config="{ x: attribute.x, y: attribute.y, draggable: viewMode === 'edit' && !isResizing }"
                  @dragstart="handleDragStart"
                  @dragend="updateAttributePosition(attribute.id, $event)"
                  @dragmove="updateAttributePosition(attribute.id, $event)"
                  @dblclick="handleAttributeDoubleClick(attribute)"
                  @click="handleItemClick(attribute, 'attribute', $event)"
                  @mouseenter="currentTool === 'line' && showConnectionPoints(attribute)"
                  @mouseleave="hideConnectionPoints(attribute)"
              >
                <v-ellipse :config="getAttributeEllipseConfig(attribute)" />
                <v-text
                    v-if="!isInlineEditing || inlineEditElement?.item.id !== attribute.id"
                    :config="getAttributeTextConfig(attribute)"
                />
                <v-line v-if="attribute.isPrimary" :config="getUnderlineConfig(attribute)" />
                <!-- 调整大小控制点 -->
                <template v-if="isItemSelected(attribute.id) && viewMode === 'edit'">
                  <v-circle v-for="h in getResizeHandles(attribute)" :key="h.type" :config="h.config" @mousedown="startResize(attribute, h.type, $event)" />
                </template>
              </v-group>

              <!-- 文本元素 -->
              <v-text
                  v-for="text in textElements"
                  :key="text.id"
                  :config="{
                  x: text.x,
                  y: text.y,
                  text: text.text,
                  fontSize: text.fontSize,
                  fill: isItemSelected(text.id) ? '#1976d2' : text.fill,
                  draggable: viewMode === 'edit'
                }"
                  @dragstart="handleDragStart"
                  @dblclick="handleTextDoubleClick(text)"
                  @click="handleItemClick(text, 'text', $event)"
                  @dragend="updateTextPosition(text.id, $event)"
                  @dragmove="updateTextPosition(text.id, $event)"
              />

              <!-- 连接点 -->
              <v-circle
                  v-for="point in connectionPoints"
                  :key="point.id"
                  :config="{
                  x: point.x,
                  y: point.y,
                  radius: 8,
                  fill: '#1976d2',
                  stroke: '#ffffff',
                  strokeWidth: 2,
                  hitStrokeWidth: 10
                }"
                  @mousedown="handleConnectionPointClick(point, $event)"
              />

              <!-- 目标连接点（连线时显示） -->
              <v-circle
                  v-for="point in targetConnectionPoints"
                  :key="'target_' + point.id"
                  :config="{
                  x: point.x,
                  y: point.y,
                  radius: snapTarget && snapTarget.id === point.id ? 10 : 6,
                  fill: snapTarget && snapTarget.id === point.id ? '#4caf50' : '#ff9800',
                  stroke: '#ffffff',
                  strokeWidth: 2
                }"
              />

              <!-- 框选矩形 -->
              <v-rect
                  v-if="isSelecting"
                  :config="{
                  x: selectionRect.x,
                  y: selectionRect.y,
                  width: selectionRect.width,
                  height: selectionRect.height,
                  fill: 'rgba(25, 118, 210, 0.1)',
                  stroke: '#1976d2',
                  strokeWidth: 1,
                  dash: [5, 5]
                }"
              />
            </v-layer>
          </v-stage>
        </div>
      </div>
    </div>

    <!-- 底部状态栏 -->
    <div class="status-bar">
      <div class="status-left">
        <span class="element-count">
          实体: {{ entities.length }} | 属性: {{ attributes.length }} | 关系: {{ relationships.length }}
        </span>
      </div>
      <div class="status-right">
        <span class="mode-indicator" :title="'按 Alt 键切换模式'">
          {{ viewMode === 'observe' ? '观察模式' : '编辑模式' }} (Alt)
        </span>
        <span v-if="currentTool !== 'select' && viewMode === 'edit'" class="tool-tip">
          {{ getToolTip() }}
        </span>
        <span v-else-if="viewMode === 'edit'" class="edit-tip">
          双击图形可编辑文本
        </span>
        <span class="coordinates" v-if="mousePosition">
          ({{ mousePosition.x }}, {{ mousePosition.y }})
        </span>
      </div>
    </div>

    <!-- SQL导入对话框 -->
    <el-dialog
        v-model="sqlImportDialog"
        title="导入SQL文件"
        width="700px"
        :close-on-click-modal="false"
    >
      <div class="sql-import-content">
        <el-upload
            ref="uploadRef"
            :auto-upload="false"
            :show-file-list="false"
            accept=".sql,.txt"
            @change="handleFileChange"
            drag
        >
          <el-icon class="el-icon--upload"><Upload /></el-icon>
          <div class="el-upload__text">
            将SQL文件拖到此处，或<em>点击上传</em>
          </div>
          <template #tip>
            <div class="el-upload__tip">
              支持 .sql 和 .txt 格式文件，系统将自动使用AI翻译英文字段名
            </div>
          </template>
        </el-upload>

        <div v-if="selectedFile" class="file-info">
          <el-icon><Document /></el-icon>
          <span>{{ selectedFile.name }}</span>
          <el-button @click="selectedFile = null" :icon="Close" size="small" text></el-button>
        </div>

        <el-divider>或直接粘贴SQL内容</el-divider>

        <el-input
            v-model="sqlContent"
            type="textarea"
            :rows="12"
            placeholder="请粘贴SQL CREATE TABLE语句...&#10;&#10;系统将自动使用AI翻译英文字段名为中文，提升ER图可读性。&#10;&#10;示例:&#10;CREATE TABLE students (&#10;    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '学号',&#10;    name VARCHAR(50) NOT NULL COMMENT '姓名',&#10;    department VARCHAR(100) COMMENT '所在院系'&#10;);"
            show-word-limit
            :maxlength="50000"
        />
      </div>

      <template #footer>
        <el-button @click="sqlImportDialog = false">取消</el-button>
        <el-button type="primary" @click="processSQLImport" :loading="importing">
          {{ importing ? 'AI翻译解析中...' : '导入并生成ER图' }}
        </el-button>
      </template>
    </el-dialog>
    <!-- 确认对话框 -->
    <el-dialog
        v-model="confirmDialog"
        title="确认操作"
        width="400px"
        :close-on-click-modal="false"
    >
      <div class="confirm-content">
        <el-icon class="confirm-icon"><Warning /></el-icon>
        <p>{{ confirmMessage }}</p>
      </div>

      <template #footer>
        <el-button @click="confirmDialog = false">取消</el-button>
        <el-button type="danger" @click="executeConfirmAction">确定</el-button>
      </template>
    </el-dialog>

    <!-- 文本编辑对话框 -->
    <el-dialog
        v-model="editDialog"
        :title="getEditDialogTitle()"
        width="400px"
        :close-on-click-modal="false"
    >
      <div class="edit-content">
        <el-input
            v-model="editText"
            :placeholder="getEditPlaceholder()"
            maxlength="50"
            show-word-limit
            @keyup.enter="saveEdit"
            ref="editInput"
        />
        <div class="edit-tips">
          <el-icon><Document /></el-icon>
          <span>双击任意图形元素可以编辑文本内容</span>
        </div>
      </div>

      <template #footer>
        <el-button @click="cancelEdit">取消</el-button>
        <el-button type="primary" @click="saveEdit" :disabled="!editText.trim()">
          确定
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import {
  ArrowLeft, Upload, Download, Delete, ZoomOut, ZoomIn, Refresh,
  Document, Close, Search, Grid, Back, Right,
  FolderOpened, Warning
} from '@element-plus/icons-vue'
import SQLParser from '@/utils/sqlParser.js'

const router = useRouter()

// 创建缺失的图标组件
const Pointer = { template: '<svg viewBox="0 0 24 24"><path d="M13.64,21.97C13.14,22.21 12.54,22 12.31,21.5L10.13,16.76L7.62,18.78C7.45,18.92 7.24,19 7,19A1,1 0 0,1 6,18V3A1,1 0 0,1 7,2C7.24,2 7.45,2.08 7.62,2.22L18.78,12.22C19.28,12.64 19.28,13.36 18.78,13.78L15.44,16.5L17.92,21.5C18.15,22 17.94,22.6 17.44,22.83C17.35,22.88 17.25,22.9 17.15,22.9C16.73,22.9 16.32,22.69 16.11,22.28L13.64,21.97Z" fill="currentColor"/></svg>' }
const Connection = { template: '<svg viewBox="0 0 24 24"><path d="M21,15L15,21H11L13,19H9A2,2 0 0,1 7,17V13L9,15H13L11,13V9L15,13H19L21,15M3,3H7V7H3V3M5,5V5M3,17H7V21H3V17Z" fill="currentColor"/></svg>' }
const EditPen = { template: '<svg viewBox="0 0 24 24"><path d="M20.71,7.04C21.1,6.65 21.1,6 20.71,5.63L18.37,3.29C18,2.9 17.35,2.9 16.96,3.29L15.12,5.12L18.87,8.87M3,17.25V21H6.75L17.81,9.93L14.06,6.18L3,17.25Z" fill="currentColor"/></svg>' }

// 响应式数据
const canvasContainer = ref(null)
const stage = ref(null)
const mainLayer = ref(null)
const gridLayer = ref(null)
const uploadRef = ref(null)
const editInput = ref(null)
const searchText = ref('')

// 画布配置
const stageConfig = reactive({
  width: 0,
  height: 0,
  scaleX: 1,
  scaleY: 1,
  x: 0,
  y: 0,
  draggable: true
})

// 网格配置
const gridSize = ref(10)
const gridLines = ref([])
const snapToGrid = ref(true) // 网格吸附开关

// 工具和状态
const scale = ref(1)
const currentTool = ref('select')
const selectedItems = ref([])
const isDrawingLine = ref(false)
const lineStartPoint = ref(null)
const mousePosition = ref(null)
const importing = ref(false)

// 线段绘制状态
const lineType = ref('straight') // straight / orthogonal / polyline
const lineArrow = ref('none') // none / start / end / both
const lineStyle = ref('solid') // solid / dashed / dotted
const previewLine = ref(null)
const lineMidPoint = ref(null) // 折线的中间点
const targetConnectionPoints = ref([]) // 目标图形的连接点
const snapTarget = ref(null) // 吸附的目标连接点

// 编辑状态
const editingItem = ref(null)
const editDialog = ref(false)
const editText = ref('')
const editType = ref('')

// 撤销/恢复系统
const history = ref([])
const historyIndex = ref(0)
const maxHistorySize = 50

// 确认对话框
const confirmDialog = ref(false)
const confirmMessage = ref('')
const confirmAction = ref(null)

// 新增状态
const viewMode = ref('edit') // 'observe' 或 'edit'
const showGrid = ref(true)
const connectionPoints = ref([])
const isInlineEditing = ref(false)

// 框选状态
const isSelecting = ref(false)
const selectionRect = ref({ x: 0, y: 0, width: 0, height: 0 })
const selectionStart = ref({ x: 0, y: 0 })

// 剪贴板
const clipboard = ref([])
const inlineEditElement = ref(null)
const inlineEditText = ref('')

// 调整大小状态
const isResizing = ref(false)
const resizingItem = ref(null)
const resizeHandle = ref(null)
const resizeStartPos = ref({ x: 0, y: 0 })
const resizeStartSize = ref({ width: 0, height: 0, x: 0, y: 0 })

// 图形数据
const entities = ref([])
const attributes = ref([])
const relationships = ref([])
const connectionLines = ref([])
const manualLines = ref([])
const textElements = ref([])

// 对话框状态
const sqlImportDialog = ref(false)
const selectedFile = ref(null)
const sqlContent = ref('')

// 工具类实例
const sqlParser = new SQLParser()

// 撤销/恢复系统
function saveToHistory() {
  const state = {
    entities: JSON.parse(JSON.stringify(entities.value)),
    attributes: JSON.parse(JSON.stringify(attributes.value)),
    relationships: JSON.parse(JSON.stringify(relationships.value)),
    connectionLines: JSON.parse(JSON.stringify(connectionLines.value)),
    manualLines: JSON.parse(JSON.stringify(manualLines.value)),
    textElements: JSON.parse(JSON.stringify(textElements.value))
  }

  // 如果撤销后有新操作，移除当前索引之后的历史记录
  if (historyIndex.value < history.value.length - 1) {
    history.value = history.value.slice(0, historyIndex.value + 1)
  }

  // 添加新状态
  history.value.push(state)
  historyIndex.value = history.value.length - 1

  // 限制历史记录大小
  if (history.value.length > maxHistorySize) {
    history.value.shift()
    historyIndex.value--
  }
}

function undo() {
  if (historyIndex.value > 0) {
    historyIndex.value--
    restoreFromHistory()
  }
}

function redo() {
  if (historyIndex.value < history.value.length - 1) {
    historyIndex.value++
    restoreFromHistory()
  }
}

function restoreFromHistory() {
  const state = history.value[historyIndex.value]
  if (state) {
    entities.value = JSON.parse(JSON.stringify(state.entities))
    attributes.value = JSON.parse(JSON.stringify(state.attributes))
    relationships.value = JSON.parse(JSON.stringify(state.relationships))
    connectionLines.value = JSON.parse(JSON.stringify(state.connectionLines))
    manualLines.value = JSON.parse(JSON.stringify(state.manualLines))
    textElements.value = JSON.parse(JSON.stringify(state.textElements))

    // 清除选择状态
    selectedItems.value = []
  }
}

// 选择管理
function selectItem(item, itemType) {
  const itemId = item.id
  const existingIndex = selectedItems.value.findIndex(selected => selected.id === itemId)

  if (existingIndex >= 0) {
    // 如果已选中，则取消选择
    selectedItems.value.splice(existingIndex, 1)
  } else {
    // 如果未选中，则添加到选择列表
    selectedItems.value.push({ id: itemId, type: itemType, item })
  }
}

function clearSelection() {
  selectedItems.value = []
}

function isItemSelected(itemId) {
  return selectedItems.value.some(selected => selected.id === itemId)
}

// 删除选中元素
function deleteSelectedItems() {
  if (selectedItems.value.length === 0 || viewMode.value !== 'edit') return

  saveToHistory()

  selectedItems.value.forEach(selected => {
    switch (selected.type) {
      case 'entity':
        const entityIndex = entities.value.findIndex(e => e.id === selected.id)
        if (entityIndex !== -1) {
          entities.value.splice(entityIndex, 1)
          // 删除相关属性
          attributes.value = attributes.value.filter(a => a.entityId !== selected.id)
        }
        break
      case 'attribute':
        const attrIndex = attributes.value.findIndex(a => a.id === selected.id)
        if (attrIndex !== -1) {
          attributes.value.splice(attrIndex, 1)
        }
        break
      case 'relationship':
        const relIndex = relationships.value.findIndex(r => r.id === selected.id)
        if (relIndex !== -1) {
          relationships.value.splice(relIndex, 1)
        }
        break
      case 'text':
        const textIndex = textElements.value.findIndex(t => t.id === selected.id)
        if (textIndex !== -1) {
          textElements.value.splice(textIndex, 1)
        }
        break
      case 'line':
        const lineIndex = manualLines.value.findIndex(l => l.id === selected.id)
        if (lineIndex !== -1) {
          manualLines.value.splice(lineIndex, 1)
        }
        break
    }
  })

  // 更新连线
  updateConnectionLines()
  updateManualLines()

  // 清空选中项
  clearSelection()

  saveCurrentState()
}

// 确认对话框
function showConfirmDialog(message, action) {
  confirmMessage.value = message
  confirmAction.value = action
  confirmDialog.value = true
}

function executeConfirmAction() {
  if (confirmAction.value) {
    confirmAction.value()
  }
  confirmDialog.value = false
  confirmAction.value = null
}

// 模式切换
function setViewMode(mode) {
  viewMode.value = mode
  if (mode === 'observe') {
    // 观察模式：清除选择，启用画布拖拽
    selectedItems.value = []
    currentTool.value = 'select'
    stageConfig.draggable = true
  } else {
    // 编辑模式：禁用画布拖拽
    stageConfig.draggable = false
  }
}

// 网格控制
function toggleGrid() {
  showGrid.value = !showGrid.value
  if (showGrid.value) {
    generateGrid()
  }
}

// 连接点管理
const hoverItemId = ref(null)

function showConnectionPoints(item) {
  if (currentTool.value !== 'line' || !item) {
    connectionPoints.value = []
    return
  }

  hoverItemId.value = item.id

  const points = []
  const { x, y, width, height } = item

  // 连接点使用绝对坐标（8个点：四边中点 + 四角）
  points.push(
      { x: x + width / 2, y: y, position: 'top' },
      { x: x + width / 2, y: y + height, position: 'bottom' },
      { x: x, y: y + height / 2, position: 'left' },
      { x: x + width, y: y + height / 2, position: 'right' },
      { x: x, y: y, position: 'top-left' },
      { x: x + width, y: y, position: 'top-right' },
      { x: x, y: y + height, position: 'bottom-left' },
      { x: x + width, y: y + height, position: 'bottom-right' }
  )

  connectionPoints.value = points.map((point, index) => ({
    ...point,
    id: `${item.id}_${index}`,
    itemId: item.id
  }))
}

function hideConnectionPoints(item) {
  // 无参数时强制清空，有参数时只清空对应图形的连接点
  if (!item || item.id === hoverItemId.value) {
    connectionPoints.value = []
    hoverItemId.value = null
  }
}

// 检测鼠标附近的图形并显示连接点
function updateNearbyConnectionPoints(mousePos) {
  const detectRange = 30
  const allItems = [...entities.value, ...attributes.value, ...relationships.value]

  const nearbyItem = allItems.find(item => {
    return mousePos.x >= item.x - detectRange &&
           mousePos.x <= item.x + (item.width || 0) + detectRange &&
           mousePos.y >= item.y - detectRange &&
           mousePos.y <= item.y + (item.height || 0) + detectRange
  })

  if (nearbyItem) {
    if (hoverItemId.value !== nearbyItem.id) {
      showConnectionPoints(nearbyItem)
    }
  } else {
    hideConnectionPoints()
  }
}

// 更新目标连接点（连线时检测鼠标附近的图形）
function updateTargetConnectionPoints(mousePos) {
  const startItemId = lineStartPoint.value?.itemId
  const allItems = [...entities.value, ...attributes.value, ...relationships.value]
    .filter(item => item.id !== startItemId)

  // 找到鼠标附近的图形（扩大检测范围）
  const detectRange = 50
  const nearbyItem = allItems.find(item => {
    return mousePos.x >= item.x - detectRange &&
           mousePos.x <= item.x + (item.width || 0) + detectRange &&
           mousePos.y >= item.y - detectRange &&
           mousePos.y <= item.y + (item.height || 0) + detectRange
  })

  if (nearbyItem) {
    // 计算该图形的连接点
    const points = getItemConnectionPoints(nearbyItem)
    targetConnectionPoints.value = points

    // 检查是否有连接点在吸附范围内
    const snapRange = 20
    const closest = points.reduce((best, point) => {
      const dist = Math.hypot(point.x - mousePos.x, point.y - mousePos.y)
      return dist < best.dist ? { point, dist } : best
    }, { point: null, dist: Infinity })

    snapTarget.value = closest.dist < snapRange ? closest.point : null
  } else {
    targetConnectionPoints.value = []
    snapTarget.value = null
  }
}

// 获取图形的连接点
function getItemConnectionPoints(item) {
  const { x, y, width, height, id } = item
  return [
    { x: x + width / 2, y: y, position: 'top', id: `${id}_0`, itemId: id },
    { x: x + width / 2, y: y + height, position: 'bottom', id: `${id}_1`, itemId: id },
    { x: x, y: y + height / 2, position: 'left', id: `${id}_2`, itemId: id },
    { x: x + width, y: y + height / 2, position: 'right', id: `${id}_3`, itemId: id },
    { x: x, y: y, position: 'top-left', id: `${id}_4`, itemId: id },
    { x: x + width, y: y, position: 'top-right', id: `${id}_5`, itemId: id },
    { x: x, y: y + height, position: 'bottom-left', id: `${id}_6`, itemId: id },
    { x: x + width, y: y + height, position: 'bottom-right', id: `${id}_7`, itemId: id }
  ]
}

// 直接编辑功能
function startInlineEdit(item, itemType) {
  if (viewMode.value !== 'edit') return

  isInlineEditing.value = true
  inlineEditElement.value = { item, type: itemType }

  // 获取当前文本内容
  switch (itemType) {
    case 'entity':
    case 'attribute':
    case 'relationship':
      inlineEditText.value = item.chineseName
      break
    case 'text':
      inlineEditText.value = item.text
      break
  }

  nextTick(() => {
    const input = document.getElementById('inline-edit-input')
    if (input) {
      input.focus()
      input.select()
    }
  })
}

function saveInlineEdit() {
  if (!inlineEditElement.value || !inlineEditText.value.trim()) {
    finishInlineEdit()
    return
  }

  saveToHistory()
  const { item, type } = inlineEditElement.value

  switch (type) {
    case 'entity':
    case 'attribute':
    case 'relationship':
      item.chineseName = inlineEditText.value.trim()
      break
    case 'text':
      item.text = inlineEditText.value.trim()
      break
  }

  saveCurrentState()
  finishInlineEdit()
}

function finishInlineEdit() {
  isInlineEditing.value = false
  inlineEditElement.value = null
  inlineEditText.value = ''
}

function getInlineEditStyle() {
  if (!inlineEditElement.value) return {}
  const { item, type } = inlineEditElement.value

  // 计算文本在图形内的实际位置
  let textX = item.x
  let textY = item.y
  let textWidth = 100

  if (type === 'entity') {
    // 实体：文本在矩形中心
    textX = item.x + item.width / 2
    textY = item.y + item.height / 2
    textWidth = item.width
  } else if (type === 'attribute') {
    // 属性：文本在椭圆中心
    textX = item.x + item.width / 2
    textY = item.y + item.height / 2
    textWidth = item.width
  } else if (type === 'relationship') {
    // 关系：文本在菱形中心
    textX = item.x + item.width / 2
    textY = item.y + item.height / 2
    textWidth = item.width
  } else if (type === 'text') {
    // 纯文本元素
    const fontSize = item.fontSize || 14
    textX = item.x
    textY = item.y + fontSize / 2
    textWidth = Math.max(100, (item.text || '').length * 10)
  }

  // 应用画布的缩放和偏移
  const scaledX = textX * stageConfig.scaleX + stageConfig.x
  const scaledY = textY * stageConfig.scaleY + stageConfig.y
  const scaledWidth = textWidth * stageConfig.scaleX

  return {
    position: 'absolute',
    left: `${scaledX - scaledWidth / 2}px`,
    top: `${scaledY - 10}px`,
    width: `${scaledWidth}px`,
    textAlign: 'center',
    zIndex: 1000
  }
}

// 统一的图形点击处理
function handleItemClick(item, itemType, event) {
  if (viewMode.value !== 'edit') return

  // 阻止事件冒泡到画布
  event.cancelBubble = true

  if (currentTool.value === 'select') {
    selectItem(item, itemType)
  }
}

// 连接点点击处理（只用于开始连线）
function handleConnectionPointClick(point, e) {
  if (e) e.cancelBubble = true // 阻止事件冒泡到stage
  if (!isDrawingLine.value) {
    // 开始连线
    lineStartPoint.value = { x: point.x, y: point.y, itemId: point.itemId }
    isDrawingLine.value = true
    // 隐藏源图形的连接点，让用户专注于目标
    connectionPoints.value = []
  }
}

// 完成连线（在画布点击时调用）
function finishConnectionLine() {
  if (!isDrawingLine.value || !lineStartPoint.value) return false

  // 如果有吸附目标，创建连线
  if (snapTarget.value && snapTarget.value.itemId !== lineStartPoint.value.itemId) {
    saveToHistory()

    const line = {
      id: generateId(),
      type: 'connection',
      points: [lineStartPoint.value.x, lineStartPoint.value.y, snapTarget.value.x, snapTarget.value.y],
      stroke: '#000000',
      strokeWidth: 2,
      lineType: lineType.value,
      startItemId: lineStartPoint.value.itemId,
      endItemId: snapTarget.value.itemId
    }

    manualLines.value.push(line)
    updateManualLines()
    saveCurrentState()
  }

  // 清理状态
  isDrawingLine.value = false
  lineStartPoint.value = null
  previewLine.value = null
  targetConnectionPoints.value = []
  snapTarget.value = null
  hideConnectionPoints()
  return true
}

// 自动保存功能
const autoSaveKey = 'er-diagram-autosave'

// 保存当前状态
function saveCurrentState() {
  const state = {
    entities: entities.value,
    attributes: attributes.value,
    relationships: relationships.value,
    connectionLines: connectionLines.value,
    manualLines: manualLines.value,
    textElements: textElements.value,
    timestamp: Date.now()
  }

  try {
    localStorage.setItem(autoSaveKey, JSON.stringify(state))
  } catch (error) {
    console.warn('自动保存失败:', error)
  }
}

// 加载保存的状态
function loadSavedState() {
  try {
    const saved = localStorage.getItem(autoSaveKey)
    if (saved) {
      const state = JSON.parse(saved)
      entities.value = state.entities || []
      attributes.value = state.attributes || []
      relationships.value = state.relationships || []
      connectionLines.value = state.connectionLines || []
      manualLines.value = state.manualLines || []
      textElements.value = state.textElements || []

      return true
    }
  } catch (error) {
    console.warn('加载保存状态失败:', error)
  }
  return false
}

// 监听数据变化并自动保存
function setupAutoSave() {
  const saveData = () => {
    saveCurrentState()
  }

  // 防抖保存
  let saveTimer = null
  const debouncedSave = () => {
    if (saveTimer) clearTimeout(saveTimer)
    saveTimer = setTimeout(saveData, 1000)
  }

  // 监听数据变化
  const stopWatchers = [
    () => entities.value.length && debouncedSave(),
    () => attributes.value.length && debouncedSave(),
    () => relationships.value.length && debouncedSave(),
    () => manualLines.value.length && debouncedSave(),
    () => textElements.value.length && debouncedSave()
  ]

  return () => stopWatchers.forEach(stop => stop && stop())
}

// 生命周期
onMounted(() => {
  initCanvas()
  const hasData = loadSavedState()

  // 初始化历史记录
  saveToHistory()

  // 设置默认模式
  setViewMode('edit')

  setupAutoSave()

  window.addEventListener('resize', handleResize)
  window.addEventListener('keydown', handleKeyDown)
  window.addEventListener('keyup', handleKeyUp)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  window.removeEventListener('keydown', handleKeyDown)
  window.removeEventListener('keyup', handleKeyUp)
})

// 初始化画布
function initCanvas() {
  nextTick(() => {
    if (canvasContainer.value) {
      const rect = canvasContainer.value.getBoundingClientRect()
      stageConfig.width = rect.width
      stageConfig.height = rect.height
      generateGrid()
    }
  })
}

// 生成网格线
function generateGrid() {
  const lines = []
  const size = gridSize.value
  let minX = 0, minY = 0, maxX = stageConfig.width, maxY = stageConfig.height

  // 计算所有图形的边界
  const allItems = [...entities.value, ...attributes.value, ...relationships.value, ...textElements.value]
  allItems.forEach(item => {
    minX = Math.min(minX, item.x - 100)
    minY = Math.min(minY, item.y - 100)
    maxX = Math.max(maxX, item.x + (item.width || 200) + 100)
    maxY = Math.max(maxY, item.y + (item.height || 100) + 100)
  })

  minX = Math.floor(minX / size) * size
  minY = Math.floor(minY / size) * size

  for (let i = minX; i <= maxX; i += size) {
    lines.push({ id: `v-${i}`, points: [i, minY, i, maxY], stroke: '#e0e0e0', strokeWidth: 1, listening: false })
  }
  for (let i = minY; i <= maxY; i += size) {
    lines.push({ id: `h-${i}`, points: [minX, i, maxX, i], stroke: '#e0e0e0', strokeWidth: 1, listening: false })
  }
  gridLines.value = lines
}

// 处理窗口大小变化
function handleResize() {
  initCanvas()
}

// 键盘快捷键
function handleKeyDown(e) {
  // Ctrl键：编辑模式下临时启用画布拖拽
  if (e.key === 'Control' && viewMode.value === 'edit') {
    stageConfig.draggable = true
    return
  }

  // Alt键：快速切换观察/编辑模式
  if (e.key === 'Alt') {
    e.preventDefault()
    const newMode = viewMode.value === 'edit' ? 'observe' : 'edit'
    setViewMode(newMode)
    return
  }

  // Backspace键：删除选中元素
  if (e.key === 'Backspace' || e.key === 'Delete') {
    // 只有在没有聚焦到输入框时才删除选中元素
    if (!isInputFocused()) {
      e.preventDefault()
      deleteSelectedItems()
    }
    return
  }

  if (e.ctrlKey || e.metaKey) {
    // 检查是否有输入框聚焦，如果有则不处理快捷键
    if (isInputFocused()) {
      return
    }

    switch (e.key) {
      case 's':
        e.preventDefault()
        exportImage()
        break
      case 'o':
        e.preventDefault()
        importSQL()
        break
      case 'z':
        e.preventDefault()
        undo()
        break
      case 'y':
        e.preventDefault()
        redo()
        break
      case 'c':
        e.preventDefault()
        copySelected()
        break
      case 'x':
        e.preventDefault()
        cutSelected()
        break
      case 'v':
        e.preventDefault()
        pasteClipboard()
        break
      case 'd':
        e.preventDefault()
        duplicateSelected()
        break
    }
  }

  if (e.key === 'Escape') {
    if (editDialog.value) {
      cancelEdit()
    } else {
      setTool('select')
    }
  }
}

// 检查是否有输入框聚焦
function isInputFocused() {
  const activeElement = document.activeElement
  return activeElement && (
    activeElement.tagName === 'INPUT' || 
    activeElement.tagName === 'TEXTAREA' ||
    activeElement.contentEditable === 'true' ||
    activeElement.classList.contains('el-input__inner') ||
    activeElement.classList.contains('el-textarea__inner')
  )
}

// 键盘释放
function handleKeyUp(e) {
  // Ctrl键释放：编辑模式下恢复禁用画布拖拽
  if (e.key === 'Control' && viewMode.value === 'edit') {
    stageConfig.draggable = false
  }
}

// 复制选中元素
function copySelected() {
  if (selectedItems.value.length === 0) return
  clipboard.value = selectedItems.value.map(s => ({
    type: s.type,
    data: JSON.parse(JSON.stringify(s.item))
  }))
}

// 复制并粘贴（Ctrl+D）
function duplicateSelected() {
  if (selectedItems.value.length === 0) return
  copySelected()
  pasteClipboard()
}

// 剪切选中元素
function cutSelected() {
  if (selectedItems.value.length === 0) return
  copySelected()
  deleteSelectedItems()
}

// 粘贴元素
function pasteClipboard() {
  if (clipboard.value.length === 0) return
  saveToHistory()
  clearSelection()

  const offset = 30
  clipboard.value.forEach(item => {
    const newItem = { ...item.data, id: generateId(), x: item.data.x + offset, y: item.data.y + offset }
    switch (item.type) {
      case 'entity':
        entities.value.push(newItem)
        break
      case 'attribute':
        attributes.value.push(newItem)
        break
      case 'relationship':
        relationships.value.push(newItem)
        break
      case 'text':
        textElements.value.push(newItem)
        break
    }
    selectedItems.value.push({ id: newItem.id, type: item.type, item: newItem })
  })
  // 更新剪贴板中的位置，以便连续粘贴时偏移
  clipboard.value.forEach(item => {
    item.data.x += offset
    item.data.y += offset
  })
  saveCurrentState()
}

// 返回发现页面
function goBack() {
  router.push('/discover')
}

// 工具切换
function setLineTool(type) {
  if (currentTool.value === 'line' && lineType.value === type) {
    currentTool.value = 'select'
    clearSelection()
    return
  }
  currentTool.value = 'line'
  lineType.value = type
  selectedItems.value = []
  isDrawingLine.value = false
  lineStartPoint.value = null
  previewLine.value = null
  targetConnectionPoints.value = []
  snapTarget.value = null
  hideConnectionPoints()
}

// 对齐选中的图形
function alignItems(direction) {
  if (selectedItems.value.length < 2) return
  saveToHistory()

  const items = selectedItems.value.map(s => s.item)
  let target

  switch (direction) {
    case 'left':
      target = Math.min(...items.map(i => i.x))
      items.forEach(i => { i.x = target })
      break
    case 'right':
      target = Math.max(...items.map(i => i.x + (i.width || 0)))
      items.forEach(i => { i.x = target - (i.width || 0) })
      break
    case 'top':
      target = Math.min(...items.map(i => i.y))
      items.forEach(i => { i.y = target })
      break
    case 'bottom':
      target = Math.max(...items.map(i => i.y + (i.height || 0)))
      items.forEach(i => { i.y = target - (i.height || 0) })
      break
    case 'center-h':
      target = items.reduce((sum, i) => sum + i.x + (i.width || 0) / 2, 0) / items.length
      items.forEach(i => { i.x = target - (i.width || 0) / 2 })
      break
    case 'center-v':
      target = items.reduce((sum, i) => sum + i.y + (i.height || 0) / 2, 0) / items.length
      items.forEach(i => { i.y = target - (i.height || 0) / 2 })
      break
  }

  updateConnectionLines()
  updateManualLines()
  saveCurrentState()
}

// 均匀分布选中的图形
function distributeItems(direction) {
  if (selectedItems.value.length < 3) return
  saveToHistory()

  const items = selectedItems.value.map(s => s.item)

  if (direction === 'horizontal') {
    items.sort((a, b) => a.x - b.x)
    const first = items[0].x
    const last = items[items.length - 1].x
    const gap = (last - first) / (items.length - 1)
    items.forEach((item, i) => { item.x = first + gap * i })
  } else {
    items.sort((a, b) => a.y - b.y)
    const first = items[0].y
    const last = items[items.length - 1].y
    const gap = (last - first) / (items.length - 1)
    items.forEach((item, i) => { item.y = first + gap * i })
  }

  updateConnectionLines()
  updateManualLines()
  saveCurrentState()
}

function setTool(tool) {
  // 再次点击已选中的工具则取消选择，恢复为选择工具
  if (currentTool.value === tool) {
    currentTool.value = 'select'
    clearSelection()
    return
  }

  currentTool.value = tool
  selectedItems.value = []
  isDrawingLine.value = false
  lineStartPoint.value = null
  previewLine.value = null
  targetConnectionPoints.value = []
  snapTarget.value = null
  hideConnectionPoints()

  // 根据工具类型给出提示
}

// 获取工具提示
function getToolTip() {
  const tips = {
    entity: '点击画布创建实体矩形',
    attribute: '点击画布创建属性椭圆',
    relationship: '点击画布创建关系菱形',
    line: '点击连接点创建连线',
    text: '点击画布添加文本',
    select: '点击图形进行选择'
  }
  return tips[currentTool.value] || ''
}

// 缩放控制
function zoomIn() {
  const newScale = Math.min(scale.value * 1.2, 5)
  setScale(newScale)
}

function zoomOut() {
  const newScale = Math.max(scale.value / 1.2, 0.1)
  setScale(newScale)
}

function setScale(newScale) {
  scale.value = newScale
  stageConfig.scaleX = newScale
  stageConfig.scaleY = newScale
}

// 鼠标事件处理
function handleStageMouseDown(e) {
  // 更新鼠标位置
  const pos = stage.value.getNode().getPointerPosition()
  mousePosition.value = { x: Math.round(pos.x), y: Math.round(pos.y) }

  // 只在编辑模式下处理创建操作
  if (viewMode.value !== 'edit') return

  // 转换为实际画布坐标（考虑缩放和平移）
  const stageNode = stage.value.getNode()
  const realPos = {
    x: (pos.x - stageNode.x()) / stageNode.scaleX(),
    y: (pos.y - stageNode.y()) / stageNode.scaleY()
  }

  // 如果正在绘制线条
  if (isDrawingLine.value) {
    // 如果是从连接点开始的连线，尝试完成连线
    if (lineStartPoint.value?.itemId) {
      finishConnectionLine()
      return
    }
    // 否则使用普通线条绘制
    if (currentTool.value === 'line') {
      handleLineDrawing(realPos)
      return
    }
  }

  // 检查是否点击在空白画布上
  if (e.target === stage.value.getNode()) {
    switch (currentTool.value) {
      case 'entity':
        createEntity(realPos)
        currentTool.value = 'select'
        break
      case 'weakEntity':
        createEntity(realPos, true)
        currentTool.value = 'select'
        break
      case 'attribute':
        createAttribute(realPos)
        currentTool.value = 'select'
        break
      case 'multiAttribute':
        createAttribute(realPos, 'multi')
        currentTool.value = 'select'
        break
      case 'derivedAttribute':
        createAttribute(realPos, 'derived')
        currentTool.value = 'select'
        break
      case 'relationship':
        createRelationship(realPos)
        currentTool.value = 'select'
        break
      case 'weakRelationship':
        createRelationship(realPos, true)
        currentTool.value = 'select'
        break
      case 'text':
        createText(realPos)
        currentTool.value = 'select'
        break
      case 'text-1':
        createText(realPos, '1')
        currentTool.value = 'select'
        break
      case 'text-n':
        createText(realPos, 'n')
        currentTool.value = 'select'
        break
      case 'text-m':
        createText(realPos, 'm')
        currentTool.value = 'select'
        break
      case 'line':
        handleLineDrawing(realPos)
        break
      case 'select':
        // 开始框选
        clearSelection()
        isSelecting.value = true
        selectionStart.value = { x: realPos.x, y: realPos.y }
        selectionRect.value = { x: realPos.x, y: realPos.y, width: 0, height: 0 }
        break
    }
  }
}

function handleStageMouseMove(e) {
  const pos = stage.value.getNode().getPointerPosition()
  const stageNode = stage.value.getNode()
  const realPos = {
    x: (pos.x - stageNode.x()) / stageNode.scaleX(),
    y: (pos.y - stageNode.y()) / stageNode.scaleY()
  }

  // 处理调整大小
  if (isResizing.value && resizingItem.value) {
    handleResizeMove(realPos)
    return
  }

  // 连接线工具模式下，检测鼠标附近的图形并显示连接点
  if (currentTool.value === 'line' && !isDrawingLine.value) {
    updateNearbyConnectionPoints(realPos)
  }

  // 线段预览
  if (isDrawingLine.value && lineStartPoint.value) {
    // 检测鼠标下的目标图形并显示连接点
    updateTargetConnectionPoints(realPos)

    // 计算预览线终点（如果有吸附目标则使用吸附点）
    const endPos = snapTarget.value ? { x: snapTarget.value.x, y: snapTarget.value.y } : realPos

    // 如果是折线且已经有中间点，显示第二段预览
    if (lineType.value === 'polyline' && lineMidPoint.value) {
      previewLine.value = calculateLinePoints(lineStartPoint.value, endPos, lineType.value, lineMidPoint.value)
    } else {
      previewLine.value = calculateLinePoints(lineStartPoint.value, endPos, lineType.value)
    }
    return
  }

  // 框选
  if (!isSelecting.value) return

  const startX = selectionStart.value.x
  const startY = selectionStart.value.y
  selectionRect.value = {
    x: Math.min(startX, realPos.x),
    y: Math.min(startY, realPos.y),
    width: Math.abs(realPos.x - startX),
    height: Math.abs(realPos.y - startY)
  }
}

// 处理调整大小移动
function handleResizeMove(pos) {
  const item = resizingItem.value
  const handle = resizeHandle.value
  const start = resizeStartSize.value
  const dx = pos.x - resizeStartPos.value.x
  const dy = pos.y - resizeStartPos.value.y
  const minSize = 40

  let newX = start.x, newY = start.y, newW = start.width, newH = start.height

  // 根据控制点位置调整大小
  if (handle.includes('e')) { newW = Math.max(minSize, start.width + dx) }
  if (handle.includes('w')) { newW = Math.max(minSize, start.width - dx); newX = start.x + start.width - newW }
  if (handle.includes('s')) { newH = Math.max(minSize, start.height + dy) }
  if (handle.includes('n')) { newH = Math.max(minSize, start.height - dy); newY = start.y + start.height - newH }

  item.x = newX
  item.y = newY
  item.width = newW
  item.height = newH

  updateConnectionLines()
  updateManualLines()
}

// 开始调整大小
function startResize(item, handleType, e) {
  if (viewMode.value !== 'edit') return
  e.cancelBubble = true

  saveToHistory()
  isResizing.value = true
  resizingItem.value = item
  resizeHandle.value = handleType

  const pos = stage.value.getNode().getPointerPosition()
  const stageNode = stage.value.getNode()
  resizeStartPos.value = {
    x: (pos.x - stageNode.x()) / stageNode.scaleX(),
    y: (pos.y - stageNode.y()) / stageNode.scaleY()
  }
  resizeStartSize.value = { x: item.x, y: item.y, width: item.width, height: item.height }
}

// 右键取消绘制
function handleContextMenu(e) {
  e.evt.preventDefault()
  if (isDrawingLine.value) {
    isDrawingLine.value = false
    lineStartPoint.value = null
    lineMidPoint.value = null
    previewLine.value = null
    targetConnectionPoints.value = []
    snapTarget.value = null
  }
}

function handleStageMouseUp(e) {
  // 处理调整大小结束
  if (isResizing.value) {
    isResizing.value = false
    resizingItem.value = null
    resizeHandle.value = null
    saveCurrentState()
    return
  }

  // 处理框选
  if (!isSelecting.value) return
  isSelecting.value = false

  const rect = selectionRect.value
  if (rect.width < 5 && rect.height < 5) return

  // 选中框内的元素
  entities.value.forEach(e => {
    if (isInRect(e.x, e.y, e.width, e.height, rect)) {
      selectedItems.value.push({ id: e.id, type: 'entity', item: e })
    }
  })
  attributes.value.forEach(a => {
    if (isInRect(a.x, a.y, a.width, a.height, rect)) {
      selectedItems.value.push({ id: a.id, type: 'attribute', item: a })
    }
  })
  relationships.value.forEach(r => {
    if (isInRect(r.x - 50, r.y - 30, 100, 60, rect)) {
      selectedItems.value.push({ id: r.id, type: 'relationship', item: r })
    }
  })
  textElements.value.forEach(t => {
    if (isInRect(t.x, t.y, 100, 20, rect)) {
      selectedItems.value.push({ id: t.id, type: 'text', item: t })
    }
  })
  manualLines.value.forEach(line => {
    if (isLineInRect(line.points, rect)) {
      selectedItems.value.push({ id: line.id, type: 'line', item: line })
    }
  })
}

function isInRect(x, y, w, h, rect) {
  return x < rect.x + rect.width && x + w > rect.x &&
      y < rect.y + rect.height && y + h > rect.y
}

function isLineInRect(points, rect) {
  if (!points || points.length < 4) return false
  const x1 = points[0], y1 = points[1], x2 = points[2], y2 = points[3]
  const minX = Math.min(x1, x2), maxX = Math.max(x1, x2)
  const minY = Math.min(y1, y2), maxY = Math.max(y1, y2)
  return minX < rect.x + rect.width && maxX > rect.x &&
      minY < rect.y + rect.height && maxY > rect.y
}

function handleWheel(e) {
  e.evt.preventDefault()

  const scaleBy = 1.05
  const stageNode = e.target.getStage()
  const pointer = stageNode.getPointerPosition()
  const mousePointTo = {
    x: (pointer.x - stageNode.x()) / stageNode.scaleX(),
    y: (pointer.y - stageNode.y()) / stageNode.scaleY()
  }

  const newScale = e.evt.deltaY > 0 ? scale.value / scaleBy : scale.value * scaleBy
  const clampedScale = Math.max(0.1, Math.min(5, newScale))

  setScale(clampedScale)

  const newPos = {
    x: pointer.x - mousePointTo.x * clampedScale,
    y: pointer.y - mousePointTo.y * clampedScale
  }

  stageConfig.x = newPos.x
  stageConfig.y = newPos.y
}

// 创建图形元素
function createEntity(pos, isWeak = false) {
  saveToHistory()
  const entity = {
    id: generateId(),
    type: 'entity',
    x: pos.x - 60,
    y: pos.y - 30,
    width: 120,
    height: 60,
    name: isWeak ? '弱实体' : '新实体',
    chineseName: isWeak ? '弱实体' : '新实体',
    isWeak,
    fields: []
  }
  entities.value.push(entity)
  saveCurrentState()
}

function createAttribute(pos, subType = 'normal') {
  saveToHistory()
  const names = { normal: '新属性', multi: '多值属性', derived: '派生属性' }
  const attribute = {
    id: generateId(),
    type: 'attribute',
    subType,
    x: pos.x - 40,
    y: pos.y - 20,
    width: 80,
    height: 40,
    name: names[subType],
    chineseName: names[subType],
    isPrimary: false
  }
  attributes.value.push(attribute)
  saveCurrentState()
}

function createRelationship(pos, isWeak = false) {
  saveToHistory()
  const relationship = {
    id: generateId(),
    type: 'relationship',
    x: pos.x - 50,
    y: pos.y - 30,
    width: 100,
    height: 60,
    name: isWeak ? '弱关系' : '新关系',
    chineseName: isWeak ? '弱关系' : '新关系',
    isWeak
  }
  relationships.value.push(relationship)
  saveCurrentState()
}

function createText(pos, presetText = '新文本') {
  saveToHistory() // 保存操作前状态

  const text = {
    id: generateId(),
    type: 'text',
    x: pos.x,
    y: pos.y,
    text: presetText,
    fontSize: 14,
    fill: '#000000'
  }

  textElements.value.push(text)
  saveCurrentState()
}

// 计算不同类型线段的点
function calculateLinePoints(start, end, type, midPoint = null) {
  switch (type) {
    case 'straight':
      return [start.x, start.y, end.x, end.y]
    case 'orthogonal':
      const midX = (start.x + end.x) / 2
      return [start.x, start.y, midX, start.y, midX, end.y, end.x, end.y]
    case 'polyline':
      // 折线：如果有中间点，生成三点折线
      if (midPoint) {
        return [start.x, start.y, midPoint.x, midPoint.y, end.x, end.y]
      }
      // 如果没有中间点，直接连接起点和终点
      return [start.x, start.y, end.x, end.y]
    default:
      return [start.x, start.y, end.x, end.y]
  }
}

function handleLineDrawing(pos) {
  // 第一次点击：设置起点
  if (!lineStartPoint.value) {
    lineStartPoint.value = pos
    isDrawingLine.value = true
    return
  }

  // 如果是折线且还没有中间点，第二次点击：设置中间点
  if (lineType.value === 'polyline' && !lineMidPoint.value) {
    lineMidPoint.value = pos
    return
  }

  // 第三次点击（折线）或第二次点击（直线/正交线）：完成绘制
  saveToHistory()

  const points = calculateLinePoints(lineStartPoint.value, pos, lineType.value, lineMidPoint.value)
  const line = {
    id: generateId(),
    type: 'line',
    lineType: lineType.value,
    points: points,
    stroke: '#000000',
    strokeWidth: 2,
    tension: 0,
    arrow: lineArrow.value
  }

  manualLines.value.push(line)
  saveCurrentState()

  // 重置绘制状态
  isDrawingLine.value = false
  lineStartPoint.value = null
  lineMidPoint.value = null
  previewLine.value = null
}

// 生成唯一ID
function generateId() {
  return 'item_' + Date.now() + '_' + Math.random().toString(36).substring(2, 11)
}

// 双击编辑功能 - 改为直接编辑
function handleEntityDoubleClick(entity) {
  if (viewMode.value !== 'edit') return
  startInlineEdit(entity, 'entity')
}

function handleAttributeDoubleClick(attribute) {
  if (viewMode.value !== 'edit') return
  startInlineEdit(attribute, 'attribute')
}

function handleRelationshipDoubleClick(relationship) {
  if (viewMode.value !== 'edit') return
  startInlineEdit(relationship, 'relationship')
}

function handleTextDoubleClick(textElement) {
  if (viewMode.value !== 'edit') return
  startInlineEdit(textElement, 'text')
}

// 保存编辑
function saveEdit() {
  if (!editingItem.value || !editText.value.trim()) return

  const text = editText.value.trim()
  if (editType.value === 'text') {
    editingItem.value.text = text
  } else {
    editingItem.value.chineseName = text
  }

  cancelEdit()
  saveCurrentState()
}

// 取消编辑
function cancelEdit() {
  editDialog.value = false
  editingItem.value = null
  editText.value = ''
  editType.value = ''
}

// 获取编辑对话框标题
function getEditDialogTitle() {
  return { entity: '编辑实体名称', attribute: '编辑属性名称', relationship: '编辑关系名称', text: '编辑文本内容' }[editType.value] || '编辑文本'
}

// 获取编辑占位符
function getEditPlaceholder() {
  return { entity: '请输入实体名称', attribute: '请输入属性名称', relationship: '请输入关系名称', text: '请输入文本内容' }[editType.value] || '请输入文本内容'
}

// 更新位置并重新绘制连线
function handleDragStart() {
  saveToHistory()
  hideConnectionPoints()
}

// 网格吸附函数
function snapPosition(pos) {
  if (!snapToGrid.value) return pos
  const size = gridSize.value
  return { x: Math.round(pos.x / size) * size, y: Math.round(pos.y / size) * size }
}

// 通用位置更新函数
function updateItemPosition(collection, id, e, updateLines = true) {
  const item = collection.value.find(i => i.id === id)
  if (item) {
    const snapped = snapPosition({ x: e.target.x(), y: e.target.y() })
    const dx = snapped.x - item.x
    const dy = snapped.y - item.y

    item.x = snapped.x
    item.y = snapped.y
    e.target.x(snapped.x)
    e.target.y(snapped.y)

    if (selectedItems.value.length > 1 && isItemSelected(id)) {
      moveSelectedItems(id, dx, dy)
    }

    if (updateLines) {
      updateConnectionLines()
      updateManualLines()
    }
    generateGrid()
    saveCurrentState()
  }
}

function updateEntityPosition(id, e) { updateItemPosition(entities, id, e) }
function updateAttributePosition(id, e) { updateItemPosition(attributes, id, e) }
function updateRelationshipPosition(id, e) { updateItemPosition(relationships, id, e) }
function updateTextPosition(id, e) { updateItemPosition(textElements, id, e, false) }

// 移动所有选中元素
function moveSelectedItems(excludeId, dx, dy) {
  selectedItems.value.forEach(s => {
    if (s.id === excludeId) return
    s.item.x += dx
    s.item.y += dy
  })
}

// 样式配置函数
function getShapeStyle(id) {
  const isSelected = isItemSelected(id)
  return { fill: '#ffffff', stroke: isSelected ? '#1976d2' : '#000000', strokeWidth: isSelected ? 3 : 2 }
}

function getEntityRectConfig(entity) {
  const style = getShapeStyle(entity.id)
  return { width: entity.width, height: entity.height, ...style, strokeWidth: entity.isWeak ? 3 : style.strokeWidth }
}

function getEntityTextConfig(entity) {
  return { x: 0, y: entity.height / 2 - 8, text: entity.chineseName, fontSize: 16, fontFamily: 'Arial, sans-serif', fill: '#000000', width: entity.width, align: 'center' }
}

function getAttributeEllipseConfig(attribute) {
  const style = getShapeStyle(attribute.id)
  const dash = attribute.subType === 'derived' ? [5, 5] : []
  return { x: attribute.width / 2, y: attribute.height / 2, radiusX: attribute.width / 2, radiusY: attribute.height / 2, ...style, dash, strokeWidth: attribute.subType === 'multi' ? 3 : style.strokeWidth }
}

function getAttributeTextConfig(attribute) {
  return { x: 0, y: attribute.height / 2 - 8, text: attribute.chineseName, fontSize: 14, fontFamily: 'Arial, sans-serif', fill: '#000000', width: attribute.width, align: 'center' }
}

function getRelationshipDiamondConfig(relationship) {
  const [w, h, cx, cy] = [relationship.width, relationship.height, relationship.width / 2, relationship.height / 2]
  const style = getShapeStyle(relationship.id)
  return { points: [cx, 0, w, cy, cx, h, 0, cy, cx, 0], closed: true, ...style, strokeWidth: relationship.isWeak ? 3 : style.strokeWidth }
}

function getRelationshipTextConfig(relationship) {
  return { x: 0, y: relationship.height / 2 - 8, text: relationship.chineseName, fontSize: 14, fontFamily: 'Arial, sans-serif', fill: '#000000', width: relationship.width, align: 'center' }
}

function getUnderlineConfig(attribute) {
  const textWidth = attribute.chineseName.length * 10
  const centerX = attribute.width / 2
  const textY = attribute.height / 2 + 8

  return {
    points: [
      centerX - textWidth / 2, textY,
      centerX + textWidth / 2, textY
    ],
    stroke: '#000000',
    strokeWidth: 1
  }
}

// 获取调整大小控制点配置
function getResizeHandles(item) {
  const w = item.width
  const h = item.height
  const size = 8
  const half = size / 2

  const baseConfig = { radius: half, fill: '#1976d2', stroke: '#fff', strokeWidth: 1 }

  return [
    { type: 'nw', config: { ...baseConfig, x: -half, y: -half } },
    { type: 'n', config: { ...baseConfig, x: w / 2, y: -half } },
    { type: 'ne', config: { ...baseConfig, x: w + half, y: -half } },
    { type: 'e', config: { ...baseConfig, x: w + half, y: h / 2 } },
    { type: 'se', config: { ...baseConfig, x: w + half, y: h + half } },
    { type: 's', config: { ...baseConfig, x: w / 2, y: h + half } },
    { type: 'sw', config: { ...baseConfig, x: -half, y: h + half } },
    { type: 'w', config: { ...baseConfig, x: -half, y: h / 2 } }
  ]
}

// 连线计算
function updateConnectionLines() {
  const lines = []

  attributes.value.forEach(attribute => {
    if (attribute.entityId) {
      const entity = entities.value.find(e => e.id === attribute.entityId)
      if (entity) {
        const line = createConnectionLine(
            attribute.x + attribute.width / 2,
            attribute.y + attribute.height / 2,
            entity.x + entity.width / 2,
            entity.y + entity.height / 2
        )
        lines.push({
          id: `line_${attribute.id}_${entity.id}`,
          config: line
        })
      }
    }
  })

  connectionLines.value = lines
}

// 更新手动连线位置
function updateManualLines() {
  manualLines.value.forEach(line => {
    if (line.startItemId && line.endItemId) {
      const startItem = findItemById(line.startItemId)
      const endItem = findItemById(line.endItemId)
      if (startItem && endItem) {
        // 计算最佳连接点（边缘连接而非中心）
        const startCenter = { x: startItem.x + (startItem.width || 0) / 2, y: startItem.y + (startItem.height || 0) / 2 }
        const endCenter = { x: endItem.x + (endItem.width || 0) / 2, y: endItem.y + (endItem.height || 0) / 2 }

        const startPoint = getEdgeConnectionPoint(startItem, endCenter)
        const endPoint = getEdgeConnectionPoint(endItem, startCenter)

        // 根据线条类型计算点
        if (line.lineType === 'orthogonal') {
          line.points = calculateOrthogonalPath(startPoint, endPoint, startItem, endItem)
        } else {
          line.points = [startPoint.x, startPoint.y, endPoint.x, endPoint.y]
        }
      }
    }
  })
}

// 计算正交折线路径（带智能避障）
function calculateOrthogonalPath(start, end, startItem, endItem) {
  const padding = 20
  const obstacles = [...entities.value, ...attributes.value, ...relationships.value]
      .filter(item => item.id !== startItem.id && item.id !== endItem.id)
      .map(item => ({ x: item.x - 10, y: item.y - 10, width: (item.width || 0) + 20, height: (item.height || 0) + 20 }))

  const isHorizontal = Math.abs(end.x - start.x) > Math.abs(end.y - start.y)
  const ext = { start: { ...start }, end: { ...end } }
  if (isHorizontal) {
    ext.start.x += (end.x > start.x ? padding : -padding)
    ext.end.x += (start.x > end.x ? padding : -padding)
  } else {
    ext.start.y += (end.y > start.y ? padding : -padding)
    ext.end.y += (start.y > end.y ? padding : -padding)
  }

  // 简单路径
  let path = isHorizontal
      ? [ext.start.x, ext.start.y, (ext.start.x + ext.end.x) / 2, ext.start.y, (ext.start.x + ext.end.x) / 2, ext.end.y, ext.end.x, ext.end.y]
      : [ext.start.x, ext.start.y, ext.start.x, (ext.start.y + ext.end.y) / 2, ext.end.x, (ext.start.y + ext.end.y) / 2, ext.end.x, ext.end.y]

  // 检查碰撞并尝试避障
  if (pathHitsObstacles(path, obstacles)) {
    let bounds = { minX: Math.min(ext.start.x, ext.end.x), maxX: Math.max(ext.start.x, ext.end.x), minY: Math.min(ext.start.y, ext.end.y), maxY: Math.max(ext.start.y, ext.end.y) }
    obstacles.forEach(o => {
      bounds.minX = Math.min(bounds.minX, o.x - padding)
      bounds.maxX = Math.max(bounds.maxX, o.x + o.width + padding)
      bounds.minY = Math.min(bounds.minY, o.y - padding)
      bounds.maxY = Math.max(bounds.maxY, o.y + o.height + padding)
    })

    const strategies = [
      [ext.start.x, ext.start.y, ext.start.x, bounds.minY, ext.end.x, bounds.minY, ext.end.x, ext.end.y],
      [ext.start.x, ext.start.y, ext.start.x, bounds.maxY, ext.end.x, bounds.maxY, ext.end.x, ext.end.y],
      [ext.start.x, ext.start.y, bounds.minX, ext.start.y, bounds.minX, ext.end.y, ext.end.x, ext.end.y],
      [ext.start.x, ext.start.y, bounds.maxX, ext.start.y, bounds.maxX, ext.end.y, ext.end.x, ext.end.y]
    ]

    let bestLen = Infinity
    for (const s of strategies) {
      if (!pathHitsObstacles(s, obstacles)) {
        const len = s.reduce((sum, _, i) => i % 2 === 0 && i < s.length - 2 ? sum + Math.abs(s[i+2] - s[i]) + Math.abs(s[i+3] - s[i+1]) : sum, 0)
        if (len < bestLen) { bestLen = len; path = s }
      }
    }
  }

  return [start.x, start.y, ...path, end.x, end.y]
}

// 检查路径是否与障碍物相交
function pathHitsObstacles(path, obstacles) {
  for (let i = 0; i < path.length - 2; i += 2) {
    const [x1, y1, x2, y2] = [path[i], path[i+1], path[i+2], path[i+3]]
    for (const r of obstacles) {
      const [minX, maxX, minY, maxY] = [Math.min(x1, x2), Math.max(x1, x2), Math.min(y1, y2), Math.max(y1, y2)]
      if (maxX >= r.x && minX <= r.x + r.width && maxY >= r.y && minY <= r.y + r.height) {
        if (x1 === x2 && x1 >= r.x && x1 <= r.x + r.width) return true
        if (y1 === y2 && y1 >= r.y && y1 <= r.y + r.height) return true
      }
    }
  }
  return false
}

// 计算图形边缘的最佳连接点
function getEdgeConnectionPoint(item, targetCenter) {
  const cx = item.x + (item.width || 0) / 2
  const cy = item.y + (item.height || 0) / 2
  const w = item.width || 0
  const h = item.height || 0

  // 计算目标方向
  const dx = targetCenter.x - cx
  const dy = targetCenter.y - cy

  if (item.type === 'relationship') {
    // 菱形：计算与菱形边的交点
    const absDx = Math.abs(dx)
    const absDy = Math.abs(dy)
    const ratio = (w/2) / (h/2)

    if (absDx * (h/2) > absDy * (w/2)) {
      // 左右边
      return { x: cx + (dx > 0 ? w/2 : -w/2), y: cy + dy * (w/2) / (absDx || 1) }
    } else {
      // 上下边
      return { x: cx + dx * (h/2) / (absDy || 1), y: cy + (dy > 0 ? h/2 : -h/2) }
    }
  } else if (item.type === 'attribute') {
    // 椭圆：计算与椭圆边的交点
    const angle = Math.atan2(dy, dx)
    return {
      x: cx + (w/2) * Math.cos(angle),
      y: cy + (h/2) * Math.sin(angle)
    }
  } else {
    // 矩形：计算与矩形边的交点
    const absDx = Math.abs(dx)
    const absDy = Math.abs(dy)

    if (absDx * h > absDy * w) {
      // 左右边
      return { x: cx + (dx > 0 ? w/2 : -w/2), y: cy + dy * (w/2) / (absDx || 1) }
    } else {
      // 上下边
      return { x: cx + dx * (h/2) / (absDy || 1), y: cy + (dy > 0 ? h/2 : -h/2) }
    }
  }
}

// 根据ID查找图形
function findItemById(id) {
  return entities.value.find(e => e.id === id) ||
      attributes.value.find(a => a.id === id) ||
      relationships.value.find(r => r.id === id)
}

function createConnectionLine(x1, y1, x2, y2) {
  return {
    points: [x1, y1, x2, y2],
    stroke: '#000000',
    strokeWidth: 1,
    lineCap: 'round'
  }
}

// SQL导入
function importSQL() {
  sqlImportDialog.value = true
  sqlContent.value = ''
  selectedFile.value = null
}

function handleFileChange(file) {
  selectedFile.value = file

  const reader = new FileReader()
  reader.onload = (e) => {
    sqlContent.value = e.target.result
  }
  reader.readAsText(file.raw)
}

async function processSQLImport() {
  if (!sqlContent.value.trim()) return

  importing.value = true

  try {
    // 模拟解析延迟，提供更好的用户体验
    await new Promise(resolve => setTimeout(resolve, 500))

    const tables = await sqlParser.parseSQLContent(sqlContent.value)

    if (tables.length === 0) return

    saveToHistory() // 保存导入前状态

    // 直接清空数据，不需要确认
    entities.value = []
    attributes.value = []
    relationships.value = []
    connectionLines.value = []
    manualLines.value = []
    textElements.value = []
    selectedItems.value = []

    createEntitiesFromTables(tables)
    saveCurrentState()

    // 导入后更新网格线
    if (showGrid.value) {
      generateGrid()
    }

    sqlImportDialog.value = false

  } catch (error) {
    console.error('SQL解析失败:', error)
  } finally {
    importing.value = false
  }
}

// 改进的布局算法 - 最大化利用画布空间
function createEntitiesFromTables(tables) {
  const entityWidth = 120
  const entityHeight = 60
  const attributeWidth = 80
  const attributeHeight = 40
  const canvasWidth = stageConfig.width
  const canvasHeight = stageConfig.height

  // 计算最优网格布局
  const cols = Math.max(1, Math.floor(Math.sqrt(tables.length)))
  const rows = Math.ceil(tables.length / cols)
  const cellWidth = Math.max(500, (canvasWidth - 100) / cols)
  const cellHeight = Math.max(450, (canvasHeight - 150) / rows)

  tables.forEach((table, tableIndex) => {
    const row = Math.floor(tableIndex / cols)
    const col = tableIndex % cols

    // 实体位置 - 在网格单元格中心
    const entityX = 50 + col * cellWidth + cellWidth / 2 - entityWidth / 2
    const entityY = 75 + row * cellHeight + cellHeight / 2 - entityHeight / 2

    // 创建实体
    const entity = {
      id: generateId(),
      type: 'entity',
      x: entityX,
      y: entityY,
      width: entityWidth,
      height: entityHeight,
      name: table.name,
      chineseName: table.chineseName,
      fields: table.fields
    }

    entities.value.push(entity)

    // 创建属性 - 智能布局
    const fieldCount = table.fields.length
    if (fieldCount === 0) return

    const centerX = entityX + entityWidth / 2
    const centerY = entityY + entityHeight / 2
    const minDistance = 140 // 最小距离，避免重叠

    table.fields.forEach((field, fieldIndex) => {
      let attrX, attrY

      if (fieldCount === 1) {
        attrX = centerX - attributeWidth / 2
        attrY = entityY - attributeHeight - 35
      } else if (fieldCount === 2) {
        attrX = fieldIndex === 0
            ? entityX - attributeWidth - 35
            : entityX + entityWidth + 35
        attrY = centerY - attributeHeight / 2
      } else if (fieldCount === 3) {
        const positions = [
          { x: centerX - attributeWidth / 2, y: entityY - attributeHeight - 35 },
          { x: entityX - attributeWidth - 35, y: entityY + entityHeight + 35 },
          { x: entityX + entityWidth + 35, y: entityY + entityHeight + 35 }
        ]
        attrX = positions[fieldIndex].x
        attrY = positions[fieldIndex].y
      } else if (fieldCount === 4) {
        const positions = [
          { x: centerX - attributeWidth / 2, y: entityY - attributeHeight - 35 },
          { x: entityX + entityWidth + 35, y: centerY - attributeHeight / 2 },
          { x: centerX - attributeWidth / 2, y: entityY + entityHeight + 35 },
          { x: entityX - attributeWidth - 35, y: centerY - attributeHeight / 2 }
        ]
        attrX = positions[fieldIndex].x
        attrY = positions[fieldIndex].y
      } else {
        // 多属性圆形分布
        const angle = (fieldIndex / fieldCount) * 2 * Math.PI
        const radius = minDistance

        attrX = centerX + Math.cos(angle) * radius - attributeWidth / 2
        attrY = centerY + Math.sin(angle) * radius - attributeHeight / 2
      }

      const attribute = {
        id: generateId(),
        type: 'attribute',
        x: attrX,
        y: attrY,
        width: attributeWidth,
        height: attributeHeight,
        name: field.name,
        chineseName: field.chineseName,
        isPrimary: field.isPrimary,
        entityId: entity.id
      }

      attributes.value.push(attribute)
    })
  })

  updateConnectionLines()
}

// 清空画布
function clearCanvas() {
  showConfirmDialog('确定要清空整个画布吗？此操作不可撤销。', () => {
    saveToHistory() // 保存当前状态到历史记录
    entities.value = []
    attributes.value = []
    relationships.value = []
    connectionLines.value = []
    manualLines.value = []
    textElements.value = []
    selectedItems.value = []
    saveCurrentState()
  })
}

// 导出图片
function exportImage() {
  if (!stage.value) return
  try {
    // 计算所有元素的边界框
    const allItems = [...entities.value, ...attributes.value, ...relationships.value, ...textElements.value]
    if (allItems.length === 0 && manualLines.value.length === 0) return

    let minX = Infinity, minY = Infinity, maxX = -Infinity, maxY = -Infinity
    allItems.forEach(item => {
      minX = Math.min(minX, item.x)
      minY = Math.min(minY, item.y)
      maxX = Math.max(maxX, item.x + (item.width || 100))
      maxY = Math.max(maxY, item.y + (item.height || 50))
    })
    manualLines.value.forEach(line => {
      for (let i = 0; i < line.points.length; i += 2) {
        minX = Math.min(minX, line.points[i])
        maxX = Math.max(maxX, line.points[i])
        minY = Math.min(minY, line.points[i + 1])
        maxY = Math.max(maxY, line.points[i + 1])
      }
    })

    const padding = 40
    const x = minX - padding, y = minY - padding
    const width = maxX - minX + padding * 2, height = maxY - minY + padding * 2

    // 临时隐藏网格
    const gridLayer = stage.value.getNode().findOne('.grid-layer')
    if (gridLayer) gridLayer.hide()

    // 创建临时canvas绘制白色背景
    const tempCanvas = document.createElement('canvas')
    tempCanvas.width = width * 2
    tempCanvas.height = height * 2
    const ctx = tempCanvas.getContext('2d')
    ctx.fillStyle = '#ffffff'
    ctx.fillRect(0, 0, tempCanvas.width, tempCanvas.height)

    // 导出指定区域
    const stageDataURL = stage.value.getNode().toDataURL({ x, y, width, height, pixelRatio: 2 })
    const img = new Image()
    img.onload = () => {
      ctx.drawImage(img, 0, 0)
      const link = document.createElement('a')
      link.download = `er-diagram-${new Date().toISOString().slice(0, 10)}.png`
      link.href = tempCanvas.toDataURL('image/png')
      link.click()
      if (gridLayer) gridLayer.show()
    }
    img.src = stageDataURL
  } catch (error) {
    console.error('导出图片失败:', error)
  }
}
</script>

<style scoped>
.er-diagram-page {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: #ffffff;
  overflow: hidden;
}

.inline-edit-input {
  min-width: 80px;
  padding: 4px 8px;
  border: 2px solid #409eff;
  border-radius: 4px;
  font-size: 14px;
  outline: none;
  background: white;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
}

.top-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 16px;
  background: #ffffff;
  border-bottom: 1px solid #e0e0e0;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.1);
  min-height: 48px;
  flex-shrink: 0;
}

.toolbar-left,
.toolbar-center,
.toolbar-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.toolbar-center {
  flex: 1;
  justify-content: center;
  max-width: 600px;
}

.tool-label {
  font-size: 12px;
  color: #666;
  white-space: nowrap;
  margin-right: 8px;
}

.zoom-display {
  font-size: 12px;
  color: #666;
  min-width: 45px;
  text-align: center;
  font-weight: 500;
}

.main-content {
  display: flex;
  flex: 1;
  overflow: hidden;
}

.left-panel {
  width: 280px;
  background: #ffffff;
  border-right: 1px solid #e0e0e0;
  display: flex;
  flex-direction: column;
  overflow-y: auto;
  flex-shrink: 0;
}

.search-section {
  padding: 16px;
  border-bottom: 1px solid #f0f0f0;
}

.tool-section {
  padding: 16px;
  border-bottom: 1px solid #f0f0f0;
}

.section-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
  font-size: 14px;
  font-weight: 600;
  color: #333;
}

.shape-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 8px;
}

.shape-item {
  width: 48px;
  height: 48px;
  border: 2px solid #e0e0e0;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s ease;
  background: #ffffff;
}

.shape-item:hover {
  border-color: #1976d2;
  background: #f3f8ff;
}

.shape-item.active {
  border-color: #1976d2;
  background: #1976d2;
  color: white;
}

.shape-item .el-icon {
  font-size: 20px;
}

/* 基础图形样式 */
.shape-rect {
  width: 24px;
  height: 16px;
  border: 2px solid currentColor;
  border-radius: 2px;
}

.shape-rect-double {
  width: 24px;
  height: 16px;
  border: 3px double currentColor;
  border-radius: 2px;
}

.shape-ellipse {
  width: 24px;
  height: 16px;
  border: 2px solid currentColor;
  border-radius: 50%;
}

.shape-ellipse-double {
  width: 24px;
  height: 16px;
  border: 3px double currentColor;
  border-radius: 50%;
}

.shape-ellipse-dashed {
  width: 24px;
  height: 16px;
  border: 2px dashed currentColor;
  border-radius: 50%;
}

.shape-diamond {
  width: 20px;
  height: 20px;
  border: 2px solid currentColor;
  transform: rotate(45deg);
}

.shape-diamond-double {
  width: 20px;
  height: 20px;
  border: 3px double currentColor;
  transform: rotate(45deg);
}

.align-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 4px;
  margin-bottom: 8px;
}

.distribute-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 4px;
}

.align-grid .el-button,
.distribute-grid .el-button {
  padding: 8px;
  min-width: 0;
}

.line-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
}

.line-options {
  margin-top: 8px;
}

.line-options .el-radio-group {
  display: flex;
  width: 100%;
}

.line-options .el-radio-button {
  flex: 1;
}

.line-item {
  width: 100%;
  height: 48px;
  border: 2px solid #e0e0e0;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s ease;
  background: #ffffff;
}

.line-item:hover {
  border-color: #1976d2;
  background: #f3f8ff;
}

.line-item.active {
  border-color: #1976d2;
  background: #1976d2;
}

.text-tools {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.text-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px;
  border: 2px solid #e0e0e0;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s ease;
  background: #ffffff;
}

.text-item:hover {
  border-color: #1976d2;
  background: #f3f8ff;
}

.text-item.active {
  border-color: #1976d2;
  background: #1976d2;
  color: white;
}

.text-item span {
  font-size: 14px;
  font-weight: 500;
}

.text-label {
  font-size: 18px;
  font-weight: bold;
  min-width: 20px;
  text-align: center;
}

.canvas-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background: #fafafa;
}

.canvas-container {
  flex: 1;
  overflow: hidden;
  background: #ffffff;
  position: relative;
}

.status-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 4px 16px;
  background: #f8f9fa;
  border-top: 1px solid #e0e0e0;
  font-size: 12px;
  color: #666;
  min-height: 28px;
  flex-shrink: 0;
}

.status-left,
.status-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.tool-tip {
  color: #1976d2;
  font-weight: 500;
}

.edit-tip {
  color: #666;
  font-style: italic;
}

.mode-indicator {
  color: #1976d2;
  font-weight: 600;
  padding: 2px 8px;
  background: #f3f8ff;
  border-radius: 4px;
  font-size: 12px;
}

.element-count {
  color: #666;
}

.coordinates {
  font-family: 'Courier New', monospace;
  color: #999;
}

.sql-import-content {
  padding: 20px 0;
}

.file-info {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 15px 0;
  padding: 12px;
  background: #f0f9ff;
  border-radius: 6px;
  color: #0369a1;
  border: 1px solid #bae6fd;
}

:deep(.el-button) {
  border-radius: 4px;
  font-size: 12px;
}

:deep(.el-button--small) {
  padding: 5px 10px;
  font-size: 11px;
}

:deep(.el-divider--vertical) {
  margin: 0 8px;
}

:deep(.el-upload-dragger) {
  border-radius: 8px;
  border: 2px dashed #d9d9d9;
  background: #fafafa;
}

:deep(.el-upload-dragger:hover) {
  border-color: #409eff;
}

:deep(.el-textarea__inner) {
  font-family: 'Courier New', monospace;
  font-size: 12px;
  line-height: 1.4;
}

:deep(.el-dialog) {
  border-radius: 8px;
}

.edit-content {
  padding: 20px 0;
}

.edit-tips {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 16px;
  padding: 12px;
  background: #f0f9ff;
  border-radius: 6px;
  color: #0369a1;
  border: 1px solid #bae6fd;
  font-size: 14px;
}

.edit-tips .el-icon {
  color: #0369a1;
}

.confirm-content {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px 0;
}

.confirm-icon {
  font-size: 24px;
  color: #f56c6c;
  flex-shrink: 0;
}

.confirm-content p {
  margin: 0;
  font-size: 16px;
  color: #333;
  line-height: 1.5;
}

/* 响应式设计 */
@media (max-width: 1200px) {
  .left-panel {
    width: 240px;
  }

  .toolbar-center {
    max-width: 400px;
  }

  .toolbar-left,
  .toolbar-right {
    flex-shrink: 0;
  }
}

@media (max-width: 768px) {
  .main-content {
    flex-direction: column;
  }

  .left-panel {
    width: 100%;
    height: 200px;
    border-right: none;
    border-bottom: 1px solid #e0e0e0;
    overflow-x: auto;
  }

  .shape-grid {
    grid-template-columns: repeat(8, 1fr);
  }

  .line-grid {
    grid-template-columns: repeat(4, 1fr);
  }

  .top-toolbar {
    flex-direction: column;
    gap: 8px;
    padding: 8px;
  }

  .toolbar-left,
  .toolbar-center,
  .toolbar-right {
    width: 100%;
    justify-content: center;
  }

  .status-bar {
    flex-direction: column;
    gap: 4px;
    text-align: center;
  }
}

/* 工具提示动画 */
.shape-item,
.line-item,
.text-item {
  position: relative;
}

.shape-item:hover::after,
.line-item:hover::after,
.text-item:hover::after {
  content: attr(title);
  position: absolute;
  bottom: -30px;
  left: 50%;
  transform: translateX(-50%);
  background: rgba(0, 0, 0, 0.8);
  color: white;
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 12px;
  white-space: nowrap;
  z-index: 1000;
  animation: fadeIn 0.2s ease;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateX(-50%) translateY(5px); }
  to { opacity: 1; transform: translateX(-50%) translateY(0); }
}

/* 滚动条样式 */
.left-panel::-webkit-scrollbar {
  width: 6px;
}

.left-panel::-webkit-scrollbar-track {
  background: #f1f1f1;
}

.left-panel::-webkit-scrollbar-thumb {
  background: #c1c1c1;
  border-radius: 3px;
}

.left-panel::-webkit-scrollbar-thumb:hover {
  background: #a1a1a1;
}

/* 选中状态动画 */
.shape-item.active,
.line-item.active,
.text-item.active {
  animation: pulse 0.3s ease;
}

@keyframes pulse {
  0% { transform: scale(1); }
  50% { transform: scale(1.05); }
  100% { transform: scale(1); }
}
</style>