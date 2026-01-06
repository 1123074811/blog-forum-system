<template>
  <div>
    <h2 class="text-2xl font-bold mb-6 dark:text-white">文章管理</h2>
    <div class="glass rounded-xl p-6">
      <el-table :data="articles" stripe>
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
import { ref, onMounted } from 'vue'
import { getAdminArticles, adminDeleteArticle } from '@/api/blog'
import { ElMessage } from 'element-plus'

const articles = ref([])

const fetchArticles = async () => {
  const res = await getAdminArticles()
  if (res.success) articles.value = res.data
}

const handleDelete = async (id) => {
  await adminDeleteArticle(id)
  ElMessage.success('删除成功')
  fetchArticles()
}

onMounted(fetchArticles)
</script>
