<template>
  <div>
    <div class="flex justify-between items-center mb-6">
      <h2 class="text-2xl font-bold dark:text-white">用户管理</h2>
      <el-button type="danger" :disabled="!selectedIds.length" @click="handleBatchDelete">批量删除 ({{ selectedIds.length }})</el-button>
    </div>
    <div class="glass rounded-xl p-6">
      <el-table :data="users" stripe @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="50" :selectable="row => row.role !== 'admin'" />
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="username" label="用户名" />
        <el-table-column prop="email" label="邮箱" />
        <el-table-column prop="role" label="角色">
          <template #default="{ row }">
            <el-tag :type="row.role === 'admin' ? 'danger' : ''">{{ row.role }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="banned" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.banned ? 'danger' : 'success'">{{ row.banned ? '已封禁' : '正常' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="注册时间" />
        <el-table-column label="操作" width="200">
          <template #default="{ row }">
            <el-switch
              v-model="row.banned"
              :disabled="row.role === 'admin'"
              active-text="封禁"
              inactive-text="正常"
              @change="handleBanChange(row)"
              style="--el-switch-on-color: #f56c6c; --el-switch-off-color: #67c23a; margin-right: 10px;"
            />
            <el-popconfirm title="确定删除该用户？" @confirm="handleDelete(row.id)">
              <template #reference>
                <el-button type="danger" size="small" :disabled="row.role === 'admin'">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getAdminUsers, deleteUser } from '@/api/blog'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '@/api'

const users = ref([])
const selectedIds = ref([])

const fetchUsers = async () => {
  const res = await getAdminUsers()
  if (res.success) users.value = res.data
}

const handleSelectionChange = (rows) => {
  selectedIds.value = rows.map(r => r.id)
}

const handleDelete = async (id) => {
  await deleteUser(id)
  ElMessage.success('删除成功')
  fetchUsers()
}

const handleBatchDelete = async () => {
  await ElMessageBox.confirm(`确定删除选中的 ${selectedIds.value.length} 个用户？`, '批量删除')
  await api.post('/admin/users/batch-delete', { ids: selectedIds.value })
  ElMessage.success('批量删除成功')
  fetchUsers()
}

const handleBanChange = async (row) => {
  try {
    await api.put(`/admin/users/${row.id}/ban`, { banned: row.banned })
    ElMessage.success(row.banned ? '已封禁该用户' : '已解除封禁')
  } catch (error) {
    ElMessage.error('操作失败')
    row.banned = !row.banned
  }
}

onMounted(fetchUsers)
</script>
