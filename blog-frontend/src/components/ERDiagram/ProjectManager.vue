<template>
  <el-dialog
    :model-value="modelValue"
    @update:model-value="$emit('update:modelValue', $event)"
    title="项目管理"
    width="800px"
    :close-on-click-modal="false"
  >
    <div class="project-manager">
      <!-- 工具栏 -->
      <div class="toolbar">
        <el-button type="primary" @click="showCreateDialog = true" :icon="Plus">
          新建项目
        </el-button>
        <el-button @click="importProject" :icon="Upload">
          导入项目
        </el-button>
        <el-button @click="refreshProjects" :icon="Refresh">
          刷新
        </el-button>
        
        <div class="toolbar-right">
          <span class="storage-info">
            共 {{ storageInfo.projectCount }} 个项目，占用 {{ storageInfo.dataSizeFormatted }}
          </span>
        </div>
      </div>

      <!-- 项目列表 -->
      <div class="project-list">
        <el-table
          :data="projectList"
          @row-click="selectProject"
          highlight-current-row
          :current-row-key="currentProjectId"
          row-key="id"
        >
          <el-table-column prop="name" label="项目名称" min-width="200">
            <template #default="{ row }">
              <div class="project-name">
                <el-icon v-if="row.id === currentProjectId" class="current-icon">
                  <Check />
                </el-icon>
                {{ row.name }}
              </div>
            </template>
          </el-table-column>
          
          <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
          
          <el-table-column prop="createdAt" label="创建时间" width="160">
            <template #default="{ row }">
              {{ formatDate(row.createdAt) }}
            </template>
          </el-table-column>
          
          <el-table-column prop="updatedAt" label="更新时间" width="160">
            <template #default="{ row }">
              {{ formatDate(row.updatedAt) }}
            </template>
          </el-table-column>
          
          <el-table-column label="操作" width="200" fixed="right">
            <template #default="{ row }">
              <el-button-group>
                <el-button size="small" @click.stop="loadProject(row)" :icon="FolderOpened">
                  打开
                </el-button>
                <el-button size="small" @click.stop="editProject(row)" :icon="Edit">
                  编辑
                </el-button>
                <el-button size="small" @click.stop="duplicateProject(row)" :icon="DocumentCopy">
                  复制
                </el-button>
                <el-button size="small" @click.stop="exportProject(row)" :icon="Download">
                  导出
                </el-button>
                <el-button 
                  size="small" 
                  type="danger" 
                  @click.stop="deleteProject(row)" 
                  :icon="Delete"
                >
                  删除
                </el-button>
              </el-button-group>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </div>

    <!-- 新建项目对话框 -->
    <el-dialog
      v-model="showCreateDialog"
      title="新建项目"
      width="500px"
      append-to-body
    >
      <el-form :model="newProject" label-width="80px">
        <el-form-item label="项目名称" required>
          <el-input v-model="newProject.name" placeholder="请输入项目名称" />
        </el-form-item>
        <el-form-item label="项目描述">
          <el-input
            v-model="newProject.description"
            type="textarea"
            :rows="3"
            placeholder="请输入项目描述（可选）"
          />
        </el-form-item>
      </el-form>
      
      <template #footer>
        <el-button @click="showCreateDialog = false">取消</el-button>
        <el-button type="primary" @click="createProject">创建</el-button>
      </template>
    </el-dialog>

    <!-- 编辑项目对话框 -->
    <el-dialog
      v-model="showEditDialog"
      title="编辑项目"
      width="500px"
      append-to-body
    >
      <el-form :model="editingProject" label-width="80px">
        <el-form-item label="项目名称" required>
          <el-input v-model="editingProject.name" placeholder="请输入项目名称" />
        </el-form-item>
        <el-form-item label="项目描述">
          <el-input
            v-model="editingProject.description"
            type="textarea"
            :rows="3"
            placeholder="请输入项目描述（可选）"
          />
        </el-form-item>
      </el-form>
      
      <template #footer>
        <el-button @click="showEditDialog = false">取消</el-button>
        <el-button type="primary" @click="saveProjectEdit">保存</el-button>
      </template>
    </el-dialog>

    <!-- 文件上传（隐藏） -->
    <input
      ref="fileInput"
      type="file"
      accept=".json"
      style="display: none"
      @change="handleImportFile"
    />
  </el-dialog>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Plus, Upload, Refresh, Check, FolderOpened, Edit, DocumentCopy,
  Download, Delete
} from '@element-plus/icons-vue'
import ERDiagramStorage from '@/utils/erDiagramStorage.js'

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['update:modelValue', 'project-selected'])

// 响应式数据
const storage = new ERDiagramStorage()
const projects = ref({})
const currentProjectId = ref('')
const showCreateDialog = ref(false)
const showEditDialog = ref(false)
const fileInput = ref(null)

// 新建项目表单
const newProject = ref({
  name: '',
  description: ''
})

// 编辑项目表单
const editingProject = ref({
  id: '',
  name: '',
  description: ''
})

// 计算属性
const projectList = computed(() => {
  return Object.values(projects.value).sort((a, b) => {
    return new Date(b.updatedAt) - new Date(a.updatedAt)
  })
})

const storageInfo = computed(() => {
  return storage.getStorageInfo()
})

// 生命周期
onMounted(() => {
  refreshProjects()
})

// 刷新项目列表
function refreshProjects() {
  projects.value = storage.getAllProjects()
  currentProjectId.value = storage.getCurrentProjectId() || ''
}

// 选择项目
function selectProject(project) {
  // 这里可以添加选择逻辑，但不立即加载
}

// 加载项目
function loadProject(project) {
  emit('project-selected', project)
  emit('update:modelValue', false)
}

// 新建项目
function createProject() {
  if (!newProject.value.name.trim()) {
    ElMessage.warning('请输入项目名称')
    return
  }
  
  try {
    const project = storage.createProject(
      newProject.value.name.trim(),
      newProject.value.description.trim()
    )
    
    refreshProjects()
    showCreateDialog.value = false
    
    // 重置表单
    newProject.value = {
      name: '',
      description: ''
    }
    
    ElMessage.success('项目创建成功')
    
    // 自动加载新项目
    loadProject(project)
    
  } catch (error) {
    console.error('创建项目失败:', error)
    ElMessage.error('创建项目失败')
  }
}

// 编辑项目
function editProject(project) {
  editingProject.value = {
    id: project.id,
    name: project.name,
    description: project.description || ''
  }
  showEditDialog.value = true
}

// 保存项目编辑
function saveProjectEdit() {
  if (!editingProject.value.name.trim()) {
    ElMessage.warning('请输入项目名称')
    return
  }
  
  try {
    // 更新项目信息
    const projects = storage.getAllProjects()
    if (projects[editingProject.value.id]) {
      projects[editingProject.value.id].name = editingProject.value.name.trim()
      projects[editingProject.value.id].description = editingProject.value.description.trim()
      projects[editingProject.value.id].updatedAt = new Date().toISOString()
      
      storage.saveProjects(projects)
      refreshProjects()
      showEditDialog.value = false
      
      ElMessage.success('项目更新成功')
    }
  } catch (error) {
    console.error('更新项目失败:', error)
    ElMessage.error('更新项目失败')
  }
}

// 复制项目
function duplicateProject(project) {
  ElMessageBox.prompt('请输入新项目名称', '复制项目', {
    confirmButtonText: '复制',
    cancelButtonText: '取消',
    inputValue: `${project.name} - 副本`
  }).then(({ value }) => {
    if (value) {
      try {
        const newProject = storage.duplicateProject(project.id, value)
        if (newProject) {
          refreshProjects()
          ElMessage.success('项目复制成功')
        } else {
          ElMessage.error('项目复制失败')
        }
      } catch (error) {
        console.error('复制项目失败:', error)
        ElMessage.error('复制项目失败')
      }
    }
  }).catch(() => {})
}

// 删除项目
function deleteProject(project) {
  ElMessageBox.confirm(
    `确定要删除项目 "${project.name}" 吗？此操作不可恢复。`,
    '删除项目',
    {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(() => {
    try {
      const success = storage.deleteProject(project.id)
      if (success) {
        refreshProjects()
        ElMessage.success('项目删除成功')
      } else {
        ElMessage.error('项目删除失败')
      }
    } catch (error) {
      console.error('删除项目失败:', error)
      ElMessage.error('删除项目失败')
    }
  }).catch(() => {})
}

// 导出项目
function exportProject(project) {
  try {
    const exportData = storage.exportProject(project.id)
    if (exportData) {
      const blob = new Blob([JSON.stringify(exportData, null, 2)], {
        type: 'application/json'
      })
      
      const url = URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = url
      link.download = `${project.name}.json`
      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)
      URL.revokeObjectURL(url)
      
      ElMessage.success('项目导出成功')
    } else {
      ElMessage.error('项目导出失败')
    }
  } catch (error) {
    console.error('导出项目失败:', error)
    ElMessage.error('导出项目失败')
  }
}

// 导入项目
function importProject() {
  fileInput.value?.click()
}

// 处理导入文件
function handleImportFile(event) {
  const file = event.target.files[0]
  if (!file) return
  
  const reader = new FileReader()
  reader.onload = (e) => {
    try {
      const importData = JSON.parse(e.target.result)
      const project = storage.importProject(importData)
      
      if (project) {
        refreshProjects()
        ElMessage.success('项目导入成功')
      } else {
        ElMessage.error('项目导入失败，请检查文件格式')
      }
    } catch (error) {
      console.error('导入项目失败:', error)
      ElMessage.error('项目导入失败，请检查文件格式')
    }
  }
  
  reader.readAsText(file)
  
  // 清空文件输入
  event.target.value = ''
}

// 格式化日期
function formatDate(dateString) {
  if (!dateString) return ''
  
  try {
    const date = new Date(dateString)
    return date.toLocaleString('zh-CN', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit'
    })
  } catch (error) {
    return dateString
  }
}
</script>

<style scoped>
.project-manager {
  height: 500px;
  display: flex;
  flex-direction: column;
}

.toolbar {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 20px;
  padding-bottom: 10px;
  border-bottom: 1px solid #e0e0e0;
}

.toolbar-right {
  margin-left: auto;
}

.storage-info {
  font-size: 12px;
  color: #666;
}

.project-list {
  flex: 1;
  overflow: auto;
}

.project-name {
  display: flex;
  align-items: center;
  gap: 8px;
}

.current-icon {
  color: #67c23a;
}

:deep(.el-table__row) {
  cursor: pointer;
}

:deep(.el-table__row:hover) {
  background-color: #f5f7fa;
}
</style>