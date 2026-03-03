<template>
  <div class="p-6">
    <div class="bg-white dark:bg-gray-800 rounded-lg shadow-md p-6">
      <div class="flex justify-between items-center mb-6">
        <h1 class="text-2xl font-bold text-gray-800 dark:text-white">公告管理</h1>
        <el-button type="primary" @click="showAddDialog = true">
          <el-icon><Plus /></el-icon>
          添加公告
        </el-button>
      </div>

      <el-table :data="announcements" stripe>
        <el-table-column prop="title" label="标题" min-width="200" />
        <el-table-column prop="type" label="类型" width="100">
          <template #default="{ row }">
            <el-tag :type="getTagType(row.type)">{{ getTypeText(row.type) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="isPinned" label="置顶" width="80">
          <template #default="{ row }">
            <el-tag v-if="row.isPinned" type="warning" size="small">置顶</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="isActive" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.isActive ? 'success' : 'danger'" size="small">
              {{ row.isActive ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="sortOrder" label="排序" width="80" />
        <el-table-column prop="createdAt" label="创建时间" width="180" />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <div class="flex gap-2">
              <el-button
                type="primary"
                size="small"
                :icon="Edit"
                @click="editAnnouncement(row)"
              >
                编辑
              </el-button>
              <el-button
                type="danger"
                size="small"
                :icon="Delete"
                @click="deleteAnnouncementItem(row.id)"
                plain
              >
                删除
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 添加/编辑公告对话框 -->
    <el-dialog v-model="showAddDialog" :title="editingId ? '编辑公告' : '添加公告'" width="600px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="标题" required>
          <el-input v-model="form.title" />
        </el-form-item>
        <el-form-item label="内容" required>
          <el-input v-model="form.content" type="textarea" :rows="5" />
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="form.type">
            <el-option label="信息" value="info" />
            <el-option label="成功" value="success" />
            <el-option label="警告" value="warning" />
            <el-option label="错误" value="error" />
          </el-select>
        </el-form-item>
        <el-form-item label="置顶">
          <el-switch v-model="form.isPinned" />
        </el-form-item>
        <el-form-item label="启用">
          <el-switch v-model="form.isActive" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sortOrder" :min="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAddDialog = false">取消</el-button>
        <el-button type="primary" @click="saveAnnouncement" :loading="saving">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getAllAnnouncements, createAnnouncement, updateAnnouncement, deleteAnnouncement } from '@/api/blog'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Edit, Delete } from '@element-plus/icons-vue'

const announcements = ref([])
const showAddDialog = ref(false)
const saving = ref(false)
const editingId = ref(null)

const form = ref({
  title: '',
  content: '',
  type: 'info',
  isPinned: false,
  isActive: true,
  sortOrder: 0
})

const loadData = async () => {
  try {
    const res = await getAllAnnouncements()
    if (res.success) {
      announcements.value = res.data || []
    }
  } catch (error) {
    ElMessage.error('加载数据失败')
  }
}

const editAnnouncement = (announcement) => {
  editingId.value = announcement.id
  Object.keys(form.value).forEach(key => {
    form.value[key] = announcement[key]
  })
  showAddDialog.value = true
}

const saveAnnouncement = async () => {
  if (!form.value.title || !form.value.content) {
    ElMessage.warning('请填写标题和内容')
    return
  }

  saving.value = true
  try {
    let res
    if (editingId.value) {
      res = await updateAnnouncement(editingId.value, form.value)
      if (res.success) {
        ElMessage.success('更新成功')
      }
    } else {
      res = await createAnnouncement(form.value)
      if (res.success) {
        ElMessage.success('添加成功')
      }
    }
    showAddDialog.value = false
    resetForm()
    loadData()
  } catch (error) {
    ElMessage.error('操作失败')
  } finally {
    saving.value = false
  }
}

const deleteAnnouncementItem = async (id) => {
  try {
    await ElMessageBox.confirm('确定要删除这条公告吗？', '确认删除', {
      type: 'warning'
    })
    const res = await deleteAnnouncement(id)
    if (res.success) {
      ElMessage.success('删除成功')
      loadData()
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

const resetForm = () => {
  editingId.value = null
  form.value = {
    title: '',
    content: '',
    type: 'info',
    isPinned: false,
    isActive: true,
    sortOrder: 0
  }
}

const getTagType = (type) => {
  const typeMap = {
    info: 'primary',
    success: 'success',
    warning: 'warning',
    error: 'danger'
  }
  return typeMap[type] || 'primary'
}

const getTypeText = (type) => {
  const typeMap = {
    info: '信息',
    success: '成功',
    warning: '警告',
    error: '错误'
  }
  return typeMap[type] || '信息'
}

onMounted(() => {
  loadData()
})
</script>