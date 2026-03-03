<template>
  <div>
    <div class="flex justify-between items-center mb-6">
      <h1 class="text-2xl font-bold">媒体管理</h1>
      <el-button type="danger" :disabled="!selectedIds.length" @click="handleBatchDelete">批量删除 ({{ selectedIds.length }})</el-button>
    </div>

    <!-- 搜索筛选区域 -->
    <div class="glass rounded-xl p-4 mb-4">
      <el-form :inline="true">
        <el-form-item label="搜索">
          <el-input v-model="searchQuery" placeholder="标题/描述" clearable @clear="handleSearch" style="width: 200px" />
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="typeFilter" placeholder="全部" clearable style="width: 120px">
            <el-option label="全部" value="" />
            <el-option label="图片" value="image" />
            <el-option label="视频" value="video" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="statusFilter" placeholder="全部" clearable style="width: 120px">
            <el-option label="全部" value="" />
            <el-option label="公开" value="public" />
            <el-option label="私密" value="private" />
          </el-select>
        </el-form-item>
        <el-form-item label="用户ID">
          <el-input v-model="userIdFilter" placeholder="用户ID" clearable style="width: 120px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <el-table :data="filteredMedia" v-loading="loading" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="50" />
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column label="预览" width="100">
        <template #default="{ row }">
          <img v-if="row.type === 'image'" :src="row.url" class="w-16 h-16 object-cover rounded" />
          <video v-else :src="row.url" class="w-16 h-16 object-cover rounded" />
        </template>
      </el-table-column>
      <el-table-column prop="title" label="标题" show-overflow-tooltip />
      <el-table-column prop="description" label="描述" show-overflow-tooltip />
      <el-table-column prop="type" label="类型" width="80" />
      <el-table-column prop="userId" label="用户ID" width="80" />
      <el-table-column prop="albumId" label="相册ID" width="80" />
      <el-table-column label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="!!row.isPublic ? 'success' : 'info'">{{ !!row.isPublic ? '公开' : '私密' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="创建时间" width="180" />
      <el-table-column label="操作" width="150">
        <template #default="{ row }">
          <el-button type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
          <el-button type="danger" size="small" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="showEditDialog" title="编辑媒体" width="400px">
      <el-form :model="editForm" label-width="80px">
        <el-form-item label="标题"><el-input v-model="editForm.title" /></el-form-item>
        <el-form-item label="描述"><el-input v-model="editForm.description" type="textarea" /></el-form-item>
        <el-form-item label="公开"><el-switch v-model="editForm.isPublic" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showEditDialog = false">取消</el-button>
        <el-button type="primary" @click="saveEdit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '@/api'

const mediaList = ref([])
const selectedIds = ref([])
const loading = ref(false)
const showEditDialog = ref(false)
const editForm = ref({})
const editingId = ref(null)
const searchQuery = ref('')
const typeFilter = ref('')
const statusFilter = ref('')
const userIdFilter = ref('')

const filteredMedia = computed(() => {
  return mediaList.value.filter(media => {
    const matchSearch = !searchQuery.value ||
      media.title?.toLowerCase().includes(searchQuery.value.toLowerCase()) ||
      media.description?.toLowerCase().includes(searchQuery.value.toLowerCase())

    const matchType = !typeFilter.value || media.type === typeFilter.value

    const matchStatus = !statusFilter.value ||
      (statusFilter.value === 'public' && !!media.isPublic) ||
      (statusFilter.value === 'private' && !media.isPublic)

    const matchUserId = !userIdFilter.value ||
      String(media.userId) === userIdFilter.value

    return matchSearch && matchType && matchStatus && matchUserId
  })
})

const loadMedia = async () => {
  loading.value = true
  const res = await api.get('/media/admin/all')
  mediaList.value = res.data || []
  loading.value = false
}

const handleSelectionChange = (rows) => {
  selectedIds.value = rows.map(r => r.id)
}

const handleBatchDelete = async () => {
  await ElMessageBox.confirm(`确定删除选中的 ${selectedIds.value.length} 个媒体？`, '批量删除')
  await api.post('/media/admin/batch-delete', { ids: selectedIds.value })
  ElMessage.success('批量删除成功')
  loadMedia()
}

const handleEdit = (row) => {
  editingId.value = row.id
  editForm.value = { title: row.title, description: row.description, isPublic: !!row.isPublic }
  showEditDialog.value = true
}

const saveEdit = async () => {
  await api.put(`/media/admin/${editingId.value}`, editForm.value)
  ElMessage.success('保存成功')
  showEditDialog.value = false
  loadMedia()
}

const handleDelete = async (row) => {
  await ElMessageBox.confirm('确定删除该媒体？', '提示')
  await api.delete(`/media/admin/${row.id}`)
  ElMessage.success('删除成功')
  loadMedia()
}

const handleSearch = () => {
  // 触发计算属性重新计算
}

const handleReset = () => {
  searchQuery.value = ''
  typeFilter.value = ''
  statusFilter.value = ''
  userIdFilter.value = ''
}

onMounted(loadMedia)
</script>
