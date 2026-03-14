<template>
  <div>
    <div class="flex justify-between items-center mb-4">
      <h1 class="text-xl sm:text-2xl font-bold">树洞管理</h1>
      <el-button type="danger" :disabled="!selectedIds.length" @click="handleBatchDelete">批量删除 ({{ selectedIds.length }})</el-button>
    </div>

    <div v-if="!isMobile" class="glass rounded-xl p-4 mb-4">
      <el-form :inline="true">
        <el-form-item label="搜索">
          <el-input v-model="searchQuery" placeholder="内容" clearable @clear="handleSearch" style="width: 200px" />
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

    <el-table v-if="!isMobile" :data="pagedTreeHoles" v-loading="loading" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="50" />
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="content" label="内容" show-overflow-tooltip />
      <el-table-column prop="color" label="颜色" width="100">
        <template #default="{ row }">
          <div class="w-6 h-6 rounded" :style="{ background: row.color }"></div>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="创建时间" width="180" />
      <el-table-column label="操作" width="100">
        <template #default="{ row }">
          <el-button type="danger" size="small" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <MobileAdminListShell v-else :items="pagedTreeHoles" :loading="loading" empty-text="暂无树洞消息">
      <div v-for="hole in pagedTreeHoles" :key="hole.id" class="admin-mobile-card">
        <div class="card-head">
          <el-checkbox :model-value="selectedIds.includes(hole.id)" @change="(val) => toggleSelect(hole.id, val)" />
          <div class="card-title">#{{ hole.id }}</div>
          <span class="color-dot" :style="{ background: hole.color || '#999' }"></span>
        </div>
        <div class="card-content">{{ hole.content || '-' }}</div>
        <div class="card-meta">{{ hole.createdAt || '-' }}</div>
        <div class="card-actions">
          <el-button type="danger" size="small" @click="handleDelete(hole)">删除</el-button>
        </div>
      </div>
    </MobileAdminListShell>

    <MobileActionSheet v-model="showFilters" title="筛选与搜索">
      <el-form label-position="top">
        <el-form-item label="搜索">
          <el-input v-model="searchQuery" placeholder="内容" clearable />
        </el-form-item>
        <div class="grid grid-cols-2 gap-2">
          <el-button @click="handleReset">重置</el-button>
          <el-button type="primary" @click="showFilters = false">完成</el-button>
        </div>
      </el-form>
    </MobileActionSheet>

    <div class="mt-4 flex justify-center" v-if="filteredTreeHoles.length">
      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        background
        :page-sizes="[5, 10, 20]"
        layout="sizes, prev, pager, next, total"
        :total="filteredTreeHoles.length"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '@/api'
import { useIsMobile } from '@/composables/useIsMobile'
import MobileAdminListShell from '@/components/admin/MobileAdminListShell.vue'
import MobileActionSheet from '@/components/admin/MobileActionSheet.vue'

const treeHoles = ref([])
const selectedIds = ref([])
const loading = ref(false)
const searchQuery = ref('')
const showFilters = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const { isMobile } = useIsMobile()

const filteredTreeHoles = computed(() => {
  return treeHoles.value.filter(hole => !searchQuery.value || hole.content?.toLowerCase().includes(searchQuery.value.toLowerCase()))
})

const pagedTreeHoles = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  return filteredTreeHoles.value.slice(start, start + pageSize.value)
})

watch(
  () => filteredTreeHoles.value.length,
  (total) => {
    const maxPage = Math.max(1, Math.ceil(total / pageSize.value))
    if (currentPage.value > maxPage) currentPage.value = maxPage
  },
  { immediate: true }
)

const loadTreeHoles = async () => {
  loading.value = true
  const res = await api.get('/tree-hole')
  treeHoles.value = res.data || []
  loading.value = false
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

const handleDelete = async (row) => {
  await ElMessageBox.confirm('确定删除该树洞？', '提示')
  await api.delete(`/tree-hole/${row.id}`)
  ElMessage.success('删除成功')
  selectedIds.value = selectedIds.value.filter(v => v !== row.id)
  loadTreeHoles()
}

const handleBatchDelete = async () => {
  await ElMessageBox.confirm(`确定删除选中的 ${selectedIds.value.length} 条树洞？`, '批量删除')
  await api.post('/tree-hole/batch-delete', { ids: selectedIds.value })
  ElMessage.success('批量删除成功')
  selectedIds.value = []
  loadTreeHoles()
}

const handleSearch = () => { currentPage.value = 1 }
const handleReset = () => { searchQuery.value = ''; currentPage.value = 1 }

onMounted(loadTreeHoles)
</script>

<style scoped>
.admin-mobile-card { border: 1px solid rgba(148, 163, 184, 0.24); border-radius: 12px; padding: 10px; background: rgba(255, 255, 255, 0.6); }
.card-head { display: flex; align-items: center; gap: 8px; }
.card-title { flex: 1; font-size: 14px; font-weight: 600; color: #334155; }
.color-dot { width: 14px; height: 14px; border-radius: 999px; }
.card-content { margin-top: 8px; font-size: 13px; color: #475569; line-height: 1.5; }
.card-meta { margin-top: 6px; font-size: 12px; color: #64748b; }
.card-actions { margin-top: 10px; display: flex; justify-content: flex-end; }
</style>
