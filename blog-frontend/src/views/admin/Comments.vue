<template>
  <div>
    <div class="flex justify-between items-center mb-4">
      <h2 class="text-xl sm:text-2xl font-bold dark:text-white">评论管理</h2>
      <el-button type="danger" :disabled="!selectedIds.length" @click="handleBatchDelete">批量删除 ({{ selectedIds.length }})</el-button>
    </div>

    <div v-if="!isMobile" class="glass rounded-xl p-4 mb-4">
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

    <div v-else class="mb-3 flex gap-2">
      <el-button class="!h-10" @click="showFilters = true">筛选</el-button>
      <el-button class="!h-10" type="danger" plain :disabled="!selectedIds.length" @click="handleBatchDelete">批量删 ({{ selectedIds.length }})</el-button>
    </div>

    <div v-if="!isMobile" class="glass rounded-xl p-6">
      <el-table :data="pagedComments" stripe @selection-change="handleSelectionChange">
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

    <MobileAdminListShell v-else :items="pagedComments" :loading="false" empty-text="暂无评论">
      <div v-for="comment in pagedComments" :key="comment.id" class="admin-mobile-card">
        <div class="card-head">
          <el-checkbox :model-value="selectedIds.includes(comment.id)" @change="(val) => toggleSelect(comment.id, val)" />
          <div class="card-title">#{{ comment.id }} · 用户 {{ comment.userId }}</div>
        </div>
        <div class="card-content">{{ comment.content || '-' }}</div>
        <div class="card-meta">文章 {{ comment.articleId }} · 点赞 {{ comment.likeCount || 0 }}</div>
        <div class="card-meta">{{ comment.createdAt || '-' }}</div>
        <div class="card-actions">
          <el-popconfirm title="确定删除该评论？" @confirm="handleDelete(comment.id)">
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
          <el-input v-model="searchQuery" placeholder="评论内容" clearable />
        </el-form-item>
        <el-form-item label="用户ID">
          <el-input v-model="userIdFilter" placeholder="用户ID" clearable />
        </el-form-item>
        <el-form-item label="文章ID">
          <el-input v-model="articleIdFilter" placeholder="文章ID" clearable />
        </el-form-item>
        <div class="grid grid-cols-2 gap-2">
          <el-button @click="handleReset">重置</el-button>
          <el-button type="primary" @click="showFilters = false">完成</el-button>
        </div>
      </el-form>
    </MobileActionSheet>

    <div class="mt-4 flex justify-center" v-if="filteredComments.length">
      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        background
        :page-sizes="[5, 10, 20]"
        layout="sizes, prev, pager, next, total"
        :total="filteredComments.length"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { getAdminComments, adminDeleteComment } from '@/api/blog'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '@/api'
import { useIsMobile } from '@/composables/useIsMobile'
import MobileAdminListShell from '@/components/admin/MobileAdminListShell.vue'
import MobileActionSheet from '@/components/admin/MobileActionSheet.vue'

const comments = ref([])
const selectedIds = ref([])
const searchQuery = ref('')
const userIdFilter = ref('')
const articleIdFilter = ref('')
const showFilters = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const { isMobile } = useIsMobile()

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

const pagedComments = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  return filteredComments.value.slice(start, start + pageSize.value)
})

watch(
  () => filteredComments.value.length,
  (total) => {
    const maxPage = Math.max(1, Math.ceil(total / pageSize.value))
    if (currentPage.value > maxPage) currentPage.value = maxPage
  },
  { immediate: true }
)

const fetchComments = async () => {
  const res = await getAdminComments()
  if (res.success) comments.value = res.data
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
  await adminDeleteComment(id)
  ElMessage.success('删除成功')
  selectedIds.value = selectedIds.value.filter(v => v !== id)
  fetchComments()
}

const handleBatchDelete = async () => {
  await ElMessageBox.confirm(`确定删除选中的 ${selectedIds.value.length} 条评论？`, '批量删除')
  await api.post('/admin/comments/batch-delete', { ids: selectedIds.value })
  ElMessage.success('批量删除成功')
  selectedIds.value = []
  fetchComments()
}

const handleSearch = () => {
  currentPage.value = 1
}

const handleReset = () => {
  searchQuery.value = ''
  userIdFilter.value = ''
  articleIdFilter.value = ''
  currentPage.value = 1
}

onMounted(fetchComments)
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
}

.card-content {
  margin-top: 8px;
  font-size: 13px;
  color: #475569;
  line-height: 1.5;
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
