<template>
  <div>
    <div class="flex justify-between items-center mb-6">
      <h1 class="text-2xl font-bold">刷题管理</h1>
      <el-button type="danger" :disabled="!selectedIds.length" @click="handleBatchDelete">批量删除 ({{ selectedIds.length }})</el-button>
    </div>
    <el-table :data="quizBanks" v-loading="loading" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="50" />
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="title" label="题库名称" />
      <el-table-column prop="description" label="描述" show-overflow-tooltip />
      <el-table-column prop="type" label="类型" width="80">
        <template #default="{ row }">
          <el-tag :type="row.type === 'quiz' ? 'primary' : 'success'" size="small">{{ row.type === 'quiz' ? '题库' : '文件' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="isPublic" label="公开" width="80">
        <template #default="{ row }">
          <el-tag :type="row.isPublic ? 'success' : 'info'" size="small">{{ row.isPublic ? '公开' : '私有' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="questionCount" label="题目数" width="80" />
      <el-table-column prop="userId" label="用户ID" width="80" />
      <el-table-column prop="createdAt" label="创建时间" width="170" />
      <el-table-column label="操作" width="150">
        <template #default="{ row }">
          <el-button type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
          <el-button type="danger" size="small" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="showEditDialog" title="编辑题库" width="450px">
      <el-form :model="editForm" label-width="80px">
        <el-form-item label="名称"><el-input v-model="editForm.title" /></el-form-item>
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

const quizBanks = ref([])
const selectedIds = ref([])
const loading = ref(false)
const showEditDialog = ref(false)
const editForm = ref({})
const editingId = ref(null)

const loadQuizBanks = async () => {
  loading.value = true
  const res = await api.get('/quiz/admin/all')
  quizBanks.value = res.data || []
  loading.value = false
}

const handleSelectionChange = (rows) => {
  selectedIds.value = rows.map(r => r.id)
}

const handleBatchDelete = async () => {
  await ElMessageBox.confirm(`确定删除选中的 ${selectedIds.value.length} 个题库？`, '批量删除')
  await api.post('/quiz/admin/batch-delete', { ids: selectedIds.value })
  ElMessage.success('批量删除成功')
  loadQuizBanks()
}

const handleEdit = (row) => {
  editingId.value = row.id
  editForm.value = { title: row.title, description: row.description, isPublic: row.isPublic }
  showEditDialog.value = true
}

const saveEdit = async () => {
  await api.put(`/quiz/admin/${editingId.value}`, editForm.value)
  ElMessage.success('保存成功')
  showEditDialog.value = false
  loadQuizBanks()
}

const handleDelete = async (row) => {
  await ElMessageBox.confirm('确定删除该题库？所有题目也会被删除！', '提示')
  await api.delete(`/quiz/admin/${row.id}`)
  ElMessage.success('删除成功')
  loadQuizBanks()
}

onMounted(loadQuizBanks)
</script>
