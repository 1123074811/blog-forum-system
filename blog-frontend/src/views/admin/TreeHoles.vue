<template>
  <div>
    <div class="flex justify-between items-center mb-6">
      <h1 class="text-2xl font-bold">树洞管理</h1>
      <el-button type="danger" :disabled="!selectedIds.length" @click="handleBatchDelete">批量删除 ({{ selectedIds.length }})</el-button>
    </div>
    <el-table :data="treeHoles" v-loading="loading" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="50" />
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="content" label="内容" show-overflow-tooltip />
      <el-table-column prop="color" label="颜色" width="100">
        <template #default="{ row }">
          <div class="w-6 h-6 rounded" :style="{ background: row.color }"></div>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="创建时间" width="180" />
      <el-table-column label="操作" width="100">
        <template #default="{ row }">
          <el-button type="danger" size="small" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '@/api'

const treeHoles = ref([])
const selectedIds = ref([])
const loading = ref(false)

const loadTreeHoles = async () => {
  loading.value = true
  const res = await api.get('/tree-hole')
  treeHoles.value = res.data || []
  loading.value = false
}

const handleSelectionChange = (rows) => {
  selectedIds.value = rows.map(r => r.id)
}

const handleDelete = async (row) => {
  await ElMessageBox.confirm('确定删除该树洞？', '提示')
  await api.delete(`/tree-hole/${row.id}`)
  ElMessage.success('删除成功')
  loadTreeHoles()
}

const handleBatchDelete = async () => {
  await ElMessageBox.confirm(`确定删除选中的 ${selectedIds.value.length} 条树洞？`, '批量删除')
  await api.post('/tree-hole/batch-delete', { ids: selectedIds.value })
  ElMessage.success('批量删除成功')
  loadTreeHoles()
}

onMounted(loadTreeHoles)
</script>
