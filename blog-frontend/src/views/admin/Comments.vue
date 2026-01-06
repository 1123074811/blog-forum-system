<template>
  <div>
    <h2 class="text-2xl font-bold mb-6 dark:text-white">评论管理</h2>
    <div class="glass rounded-xl p-6">
      <el-table :data="comments" stripe>
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
import { ref, onMounted } from 'vue'
import { getAdminComments, adminDeleteComment } from '@/api/blog'
import { ElMessage } from 'element-plus'

const comments = ref([])

const fetchComments = async () => {
  const res = await getAdminComments()
  if (res.success) comments.value = res.data
}

const handleDelete = async (id) => {
  await adminDeleteComment(id)
  ElMessage.success('删除成功')
  fetchComments()
}

onMounted(fetchComments)
</script>
