<template>
  <div>
    <h2 class="text-2xl font-bold mb-6 dark:text-white">用户管理</h2>
    <div class="glass rounded-xl p-6">
      <el-table :data="users" stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="username" label="用户名" />
        <el-table-column prop="email" label="邮箱" />
        <el-table-column prop="role" label="角色">
          <template #default="{ row }">
            <el-tag :type="row.role === 'admin' ? 'danger' : ''">{{ row.role }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="注册时间" />
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
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
import { ElMessage } from 'element-plus'

const users = ref([])

const fetchUsers = async () => {
  const res = await getAdminUsers()
  if (res.success) users.value = res.data
}

const handleDelete = async (id) => {
  await deleteUser(id)
  ElMessage.success('删除成功')
  fetchUsers()
}

onMounted(fetchUsers)
</script>
