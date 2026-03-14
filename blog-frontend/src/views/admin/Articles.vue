<template>
  <div>
    <div class="flex justify-between items-center mb-4">
      <h2 class="text-xl sm:text-2xl font-bold dark:text-white">文章管理</h2>
      <el-button type="danger" :disabled="!selectedIds.length" @click="handleBatchDelete">批量删除 ({{ selectedIds.length }})</el-button>
    </div>

    <div v-if="!isMobile" class="glass rounded-xl p-4 mb-4">
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

    <div v-else class="mb-3 flex gap-2">
      <el-button class="!h-10" @click="showFilters = true">筛选</el-button>
      <el-button class="!h-10" type="danger" plain :disabled="!selectedIds.length" @click="handleBatchDelete">批量删 ({{ selectedIds.length }})</el-button>
    </div>

    <div v-if="!isMobile" class="glass rounded-xl p-6">
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

    <MobileAdminListShell v-else :items="filteredArticles" :loading="false" empty-text="暂无文章">
      <div v-for="article in filteredArticles" :key="article.id" class="admin-mobile-card">
        <div class="card-head">
          <el-checkbox :model-value="selectedIds.includes(article.id)" @change="(val) => toggleSelect(article.id, val)" />
          <div class="card-title">{{ article.title || '未命名文章' }}</div>
          <el-tag size="small" :type="article.status === 'published' ? 'success' : 'info'">{{ article.status === 'published' ? '已发布' : '草稿' }}</el-tag>
        </div>
        <div class="card-meta">ID: {{ article.id }} · 浏览 {{ article.viewCount || 0 }}</div>
        <div class="card-meta">{{ article.createdAt || '-' }}</div>
        <div class="card-actions">
          <el-popconfirm title="确定删除该文章？" @confirm="handleDelete(article.id)">
            <template #reference>
              <el-button type="danger" size="small">删除</el-button>
            </template>
          </el-popconfirm>
        </div>
      </div>
    </MobileAdminListShell>

    <MobileActionSheet v-model="showFilters" title="筛选与搜索">
      <el-form label-position="top">
        <el-form-item label="搜索">
          <el-input v-model="searchQuery" placeholder="标题" clearable />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="statusFilter" placeholder="全部" clearable class="w-full">
            <el-option label="全部" value="" />
            <el-option label="已发布" value="published" />
            <el-option label="草稿" value="draft" />
          </el-select>
        </el-form-item>
        <div class="grid grid-cols-2 gap-2">
          <el-button @click="handleReset">重置</el-button>
          <el-button type="primary" @click="showFilters = false">完成</el-button>
        </div>
      </el-form>
    </MobileActionSheet>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { getAdminArticles, adminDeleteArticle } from '@/api/blog'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '@/api'
import { useIsMobile } from '@/composables/useIsMobile'
import MobileAdminListShell from '@/components/admin/MobileAdminListShell.vue'
import MobileActionSheet from '@/components/admin/MobileActionSheet.vue'

const articles = ref([])
const selectedIds = ref([])
const searchQuery = ref('')
const statusFilter = ref('')
const showFilters = ref(false)
const { isMobile } = useIsMobile()

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

const toggleSelect = (id, checked) => {
  if (checked) {
    if (!selectedIds.value.includes(id)) selectedIds.value.push(id)
  } else {
    selectedIds.value = selectedIds.value.filter(v => v !== id)
  }
}

const handleDelete = async (id) => {
  await adminDeleteArticle(id)
  ElMessage.success('删除成功')
  selectedIds.value = selectedIds.value.filter(v => v !== id)
  fetchArticles()
}

const handleBatchDelete = async () => {
  await ElMessageBox.confirm(`确定删除选中的 ${selectedIds.value.length} 篇文章？`, '批量删除')
  await api.post('/admin/articles/batch-delete', { ids: selectedIds.value })
  ElMessage.success('批量删除成功')
  selectedIds.value = []
  fetchArticles()
}

const handleSearch = () => {}

const handleReset = () => {
  searchQuery.value = ''
  statusFilter.value = ''
}

onMounted(fetchArticles)
</script>

<style scoped>
.admin-mobile-card {
  border: 1px solid rgba(148, 163, 184, 0.24);
  border-radius: 12px;
  padding: 10px;
  background: rgba(255, 255, 255, 0.6);
}

.card-head {
  display: flex;
  align-items: center;
  gap: 8px;
}

.card-title {
  flex: 1;
  font-size: 14px;
  font-weight: 600;
  color: #334155;
  line-height: 1.4;
}

.card-meta {
  margin-top: 6px;
  font-size: 12px;
  color: #64748b;
}

.card-actions {
  margin-top: 10px;
  display: flex;
  justify-content: flex-end;
}
</style>
