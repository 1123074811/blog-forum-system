<template>
  <div>
    <div class="flex justify-between items-center mb-4 gap-2">
      <h2 class="text-xl sm:text-2xl font-bold dark:text-white">收藏管理</h2>
      <el-button type="danger" :disabled="!selectedIds.length" @click="handleBatchDelete">批量删除 ({{ selectedIds.length }})</el-button>
    </div>

    <div v-if="!isMobile" class="glass rounded-xl p-4 mb-4">
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

    <div v-else class="mb-3 flex gap-2">
      <el-button class="!h-10" @click="showFilters = true">筛选</el-button>
      <el-button class="!h-10" type="danger" plain :disabled="!selectedIds.length" @click="handleBatchDelete">批量删 ({{ selectedIds.length }})</el-button>
    </div>

    <div v-if="!isMobile" class="glass rounded-xl p-6">
      <el-table :data="pagedFavorites" stripe @selection-change="handleSelectionChange">
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

    <MobileAdminListShell v-else :items="pagedFavorites" :loading="false" empty-text="暂无收藏">
      <div v-for="fav in pagedFavorites" :key="fav.id" class="admin-mobile-card">
        <div class="card-head">
          <el-checkbox :model-value="selectedIds.includes(fav.id)" @change="(val) => toggleSelect(fav.id, val)" />
          <div class="card-title">{{ fav.articleTitle || '未命名文章' }}</div>
        </div>
        <div class="card-meta">用户: {{ fav.username || '-' }}</div>
        <div class="card-meta">收藏时间: {{ fav.createdAt || '-' }}</div>
        <div class="card-actions">
          <el-popconfirm title="确定删除该收藏？" @confirm="handleDelete(fav.id)">
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
          <el-input v-model="searchQuery" placeholder="用户名或文章标题" clearable />
        </el-form-item>
        <div class="grid grid-cols-2 gap-2">
          <el-button @click="handleReset">重置</el-button>
          <el-button type="primary" @click="showFilters = false">完成</el-button>
        </div>
      </el-form>
    </MobileActionSheet>

    <div class="mt-4 flex justify-center" v-if="filteredFavorites.length">
      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        background
        :page-sizes="[5, 10, 20]"
        layout="sizes, prev, pager, next, total"
        :total="filteredFavorites.length"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { getAdminFavorites, deleteAdminFavorite } from '@/api/blog'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '@/api'
import { useIsMobile } from '@/composables/useIsMobile'
import MobileAdminListShell from '@/components/admin/MobileAdminListShell.vue'
import MobileActionSheet from '@/components/admin/MobileActionSheet.vue'

const favorites = ref([])
const selectedIds = ref([])
const searchQuery = ref('')
const showFilters = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const { isMobile } = useIsMobile()

const filteredFavorites = computed(() => {
  return favorites.value.filter(fav => {
    const query = searchQuery.value.toLowerCase()
    return !query || fav.username?.toLowerCase().includes(query) || fav.articleTitle?.toLowerCase().includes(query)
  })
})

const pagedFavorites = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  return filteredFavorites.value.slice(start, start + pageSize.value)
})

watch(
  () => filteredFavorites.value.length,
  (total) => {
    const maxPage = Math.max(1, Math.ceil(total / pageSize.value))
    if (currentPage.value > maxPage) currentPage.value = maxPage
  },
  { immediate: true }
)

const fetchFavorites = async () => {
  const res = await getAdminFavorites()
  if (res.success) favorites.value = res.data
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
  await deleteAdminFavorite(id)
  ElMessage.success('删除成功')
  selectedIds.value = selectedIds.value.filter(v => v !== id)
  fetchFavorites()
}

const handleBatchDelete = async () => {
  await ElMessageBox.confirm(`确定删除选中的 ${selectedIds.value.length} 个收藏？`, '批量删除')
  await api.post('/admin/favorites/batch-delete', { ids: selectedIds.value })
  ElMessage.success('批量删除成功')
  selectedIds.value = []
  fetchFavorites()
}

const handleSearch = () => { currentPage.value = 1 }
const handleReset = () => { searchQuery.value = ''; currentPage.value = 1 }

onMounted(fetchFavorites)
</script>

<style scoped>
.admin-mobile-card { border: 1px solid rgba(148, 163, 184, 0.24); border-radius: 12px; padding: 10px; background: rgba(255, 255, 255, 0.6); }
.card-head { display: flex; align-items: center; gap: 8px; }
.card-title { flex: 1; font-size: 14px; font-weight: 600; color: #334155; }
.card-meta { margin-top: 6px; font-size: 12px; color: #64748b; }
.card-actions { margin-top: 10px; display: flex; justify-content: flex-end; }
</style>
