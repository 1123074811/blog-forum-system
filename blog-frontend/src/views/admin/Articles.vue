<template>
  <div>
    <div class="flex justify-between items-center mb-6">
      <h2 class="text-2xl font-bold dark:text-white">文章管理</h2>
      <el-button type="danger" :disabled="!selectedIds.length" @click="handleBatchDelete">批量删除 ({{ selectedIds.length }})</el-button>
    </div>

    <!-- 搜索筛选区域 -->
    <div class="glass rounded-xl p-4 mb-4">
      <el-form :inline="true">
        <el-form-item label="搜索">
          <el-input v-model="searchQuery" placeholder="标题" clearable @clear="handleSearch" style="width: 200px" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="statusFilter" placeholder="全部" clearable style="width: 120px">
            <el-option label="全部" value="" />
            <el-option label="已发布" value="published" />
            <el-option label="草稿" value="draft" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="glass rounded-xl p-6">
      <el-table :data="filteredArticles" stripe @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="50" />
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="title" label="标题" />
        <el-table-column prop="status" label="状态">
          <template #default="{ row }">
            <el-tag :type="row.status === 'published' ? 'success' : 'info'">{{ row.status === 'published' ? '已发布' : '草稿' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="viewCount" label="浏览量" width="100" />
        <el-table-column prop="createdAt" label="创建时间" />
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-popconfirm title="确定删除该文章？" @confirm="handleDelete(row.id)">
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
import { getAdminArticles, adminDeleteArticle } from '@/api/blog'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '@/api'

const articles = ref([])
const selectedIds = ref([])
const searchQuery = ref('')
const statusFilter = ref('')

const filteredArticles = computed(() => {
  return articles.value.filter(article => {
    const matchSearch = !searchQuery.value ||
      article.title?.toLowerCase().includes(searchQuery.value.toLowerCase())

    const matchStatus = !statusFilter.value || article.status === statusFilter.value

    return matchSearch && matchStatus
  })
})

const fetchArticles = async () => {
  const res = await getAdminArticles()
  if (res.success) articles.value = res.data
}

const handleSelectionChange = (rows) => {
  selectedIds.value = rows.map(r => r.id)
}

const handleDelete = async (id) => {
  await adminDeleteArticle(id)
  ElMessage.success('删除成功')
  fetchArticles()
}

const handleBatchDelete = async () => {
  await ElMessageBox.confirm(`确定删除选中的 ${selectedIds.value.length} 篇文章？`, '批量删除')
  await api.post('/admin/articles/batch-delete', { ids: selectedIds.value })
  ElMessage.success('批量删除成功')
  fetchArticles()
}

const handleSearch = () => {
  // 触发计算属性重新计算
}

const handleReset = () => {
  searchQuery.value = ''
  statusFilter.value = ''
}

onMounted(fetchArticles)
</script>
