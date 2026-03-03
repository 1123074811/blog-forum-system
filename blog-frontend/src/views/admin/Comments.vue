<template>
  <div>
    <div class="flex justify-between items-center mb-6">
      <h2 class="text-2xl font-bold dark:text-white">评论管理</h2>
      <el-button type="danger" :disabled="!selectedIds.length" @click="handleBatchDelete">批量删除 ({{ selectedIds.length }})</el-button>
    </div>

    <!-- 搜索筛选区域 -->
    <div class="glass rounded-xl p-4 mb-4">
      <el-form :inline="true">
        <el-form-item label="搜索">
          <el-input v-model="searchQuery" placeholder="评论内容" clearable @clear="handleSearch" style="width: 200px" />
        </el-form-item>
        <el-form-item label="用户ID">
          <el-input v-model="userIdFilter" placeholder="用户ID" clearable style="width: 120px" />
        </el-form-item>
        <el-form-item label="文章ID">
          <el-input v-model="articleIdFilter" placeholder="文章ID" clearable style="width: 120px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="glass rounded-xl p-6">
      <el-table :data="filteredComments" stripe @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="50" />
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="content" label="内容" show-overflow-tooltip />
        <el-table-column prop="userId" label="用户ID" width="100" />
        <el-table-column prop="articleId" label="文章ID" width="100" />
        <el-table-column prop="likeCount" label="点赞数" width="100" />
        <el-table-column prop="createdAt" label="创建时间" />
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-popconfirm title="确定删除该评论？" @confirm="handleDelete(row.id)">
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
import { getAdminComments, adminDeleteComment } from '@/api/blog'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '@/api'

const comments = ref([])
const selectedIds = ref([])
const searchQuery = ref('')
const userIdFilter = ref('')
const articleIdFilter = ref('')

const filteredComments = computed(() => {
  return comments.value.filter(comment => {
    const matchSearch = !searchQuery.value ||
      comment.content?.toLowerCase().includes(searchQuery.value.toLowerCase())

    const matchUserId = !userIdFilter.value ||
      String(comment.userId) === userIdFilter.value

    const matchArticleId = !articleIdFilter.value ||
      String(comment.articleId) === articleIdFilter.value

    return matchSearch && matchUserId && matchArticleId
  })
})

const fetchComments = async () => {
  const res = await getAdminComments()
  if (res.success) comments.value = res.data
}

const handleSelectionChange = (rows) => {
  selectedIds.value = rows.map(r => r.id)
}

const handleDelete = async (id) => {
  await adminDeleteComment(id)
  ElMessage.success('删除成功')
  fetchComments()
}

const handleBatchDelete = async () => {
  await ElMessageBox.confirm(`确定删除选中的 ${selectedIds.value.length} 条评论？`, '批量删除')
  await api.post('/admin/comments/batch-delete', { ids: selectedIds.value })
  ElMessage.success('批量删除成功')
  fetchComments()
}

const handleSearch = () => {
  // 触发计算属性重新计算
}

const handleReset = () => {
  searchQuery.value = ''
  userIdFilter.value = ''
  articleIdFilter.value = ''
}

onMounted(fetchComments)
</script>
