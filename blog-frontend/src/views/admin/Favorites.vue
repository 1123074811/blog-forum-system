<template>
  <div>
    <div class="flex justify-between items-center mb-6">
      <h2 class="text-2xl font-bold dark:text-white">收藏管理</h2>
      <div class="flex gap-2">
        <el-button type="danger" :disabled="!selectedIds.length" @click="handleBatchDelete">批量删除 ({{ selectedIds.length }})</el-button>
      </div>
    </div>

    <!-- 搜索筛选区域 -->
    <div class="glass rounded-xl p-4 mb-4">
      <el-form :inline="true">
        <el-form-item label="搜索">
          <el-input v-model="searchQuery" placeholder="用户名或文章标题" clearable @clear="handleSearch" style="width: 200px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="glass rounded-xl p-6">
      <el-table :data="filteredFavorites" stripe @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="50" />
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="username" label="用户" />
        <el-table-column prop="articleTitle" label="文章" />
        <el-table-column prop="createdAt" label="收藏时间" />
        <el-table-column label="操作" width="100">
          <template #default="{ row }">
            <el-popconfirm title="确定删除该收藏？" @confirm="handleDelete(row.id)">
              <template #reference>
                <el-button type="danger" size="small">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { getAdminFavorites, deleteAdminFavorite } from '@/api/blog'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '@/api'

const favorites = ref([])
const selectedIds = ref([])
const searchQuery = ref('')

const filteredFavorites = computed(() => {
  return favorites.value.filter(fav => {
    const query = searchQuery.value.toLowerCase()
    return !query ||
      fav.username?.toLowerCase().includes(query) ||
      fav.articleTitle?.toLowerCase().includes(query)
  })
})

const fetchFavorites = async () => {
  const res = await getAdminFavorites()
  if (res.success) favorites.value = res.data
}

const handleSelectionChange = (rows) => {
  selectedIds.value = rows.map(r => r.id)
}

const handleDelete = async (id) => {
  await deleteAdminFavorite(id)
  ElMessage.success('删除成功')
  fetchFavorites()
}

const handleBatchDelete = async () => {
  await ElMessageBox.confirm(`确定删除选中的 ${selectedIds.value.length} 个收藏？`, '批量删除')
  await api.post('/admin/favorites/batch-delete', { ids: selectedIds.value })
  ElMessage.success('批量删除成功')
  fetchFavorites()
}

const handleSearch = () => {
  // 触发计算属性重新计算
}

const handleReset = () => {
  searchQuery.value = ''
}

onMounted(fetchFavorites)
</script>
