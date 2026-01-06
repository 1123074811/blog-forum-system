<template>
  <div>
    <h1 class="text-2xl font-bold mb-6">相册管理</h1>
    <el-table :data="albums" v-loading="loading">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column label="封面" width="80">
        <template #default="{ row }">
          <el-avatar :src="row.coverUrls?.[0]" shape="square" />
        </template>
      </el-table-column>
      <el-table-column prop="title" label="标题" />
      <el-table-column prop="description" label="描述" show-overflow-tooltip />
      <el-table-column prop="userId" label="用户ID" width="80" />
      <el-table-column prop="mediaCount" label="媒体数" width="80" />
      <el-table-column label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="row.isPublic ? 'success' : 'info'">{{ row.isPublic ? '公开' : '私密' }}</el-tag>
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

    <el-dialog v-model="showEditDialog" title="编辑相册" width="400px">
      <el-form :model="editForm" label-width="80px">
        <el-form-item label="标题"><el-input v-model="editForm.title" /></el-form-item>
        <el-form-item label="描述"><el-input v-model="editForm.description" type="textarea" /></el-form-item>
        <el-form-item label="公开">
          <el-switch v-model="editForm.isPublic" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showEditDialog = false">取消</el-button>
        <el-button type="primary" @click="saveEdit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '@/api'

const albums = ref([])
const loading = ref(false)
const showEditDialog = ref(false)
const editForm = ref({})
const editingId = ref(null)

const loadAlbums = async () => {
  loading.value = true
  const res = await api.get('/albums/admin/all')
  albums.value = res.data || []
  loading.value = false
}

const handleEdit = (row) => {
  editingId.value = row.id
  editForm.value = { title: row.title, description: row.description, isPublic: row.isPublic }
  showEditDialog.value = true
}

const saveEdit = async () => {
  await api.put(`/albums/admin/${editingId.value}`, editForm.value)
  ElMessage.success('保存成功')
  showEditDialog.value = false
  loadAlbums()
}

const handleDelete = async (row) => {
  await ElMessageBox.confirm('确定删除该相册？', '提示')
  await api.delete(`/albums/admin/${row.id}`)
  ElMessage.success('删除成功')
  loadAlbums()
}

onMounted(loadAlbums)
</script>
