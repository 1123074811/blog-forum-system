<template>
  <div>
    <div class="flex justify-between items-center mb-4">
      <h1 class="text-xl sm:text-2xl font-bold">相册管理</h1>
      <el-button type="danger" :disabled="!selectedIds.length" @click="handleBatchDelete">批量删除 ({{ selectedIds.length }})</el-button>
    </div>

    <div class="glass rounded-xl p-4 mb-4" v-if="!isMobile">
      <el-form :inline="true">
        <el-form-item label="搜索"><el-input v-model="searchQuery" placeholder="标题/描述" clearable @clear="handleSearch" style="width: 200px" /></el-form-item>
        <el-form-item label="用户ID"><el-input v-model="userIdFilter" placeholder="用户ID" clearable style="width: 120px" /></el-form-item>
        <el-form-item label="状态">
          <el-select v-model="statusFilter" placeholder="全部" clearable style="width: 120px">
            <el-option label="全部" value="" /><el-option label="公开" value="public" /><el-option label="私密" value="private" />
          </el-select>
        </el-form-item>
        <el-form-item><el-button type="primary" @click="handleSearch">搜索</el-button><el-button @click="handleReset">重置</el-button></el-form-item>
      </el-form>
    </div>

    <div v-else class="mb-3 flex gap-2">
      <el-button class="!h-10" @click="showFilters = true">筛选</el-button>
      <el-button class="!h-10" type="danger" plain :disabled="!selectedIds.length" @click="handleBatchDelete">批量删 ({{ selectedIds.length }})</el-button>
    </div>

    <el-table v-if="!isMobile" :data="pagedAlbums" v-loading="loading" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="50" />
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column label="封面" width="80"><template #default="{ row }"><el-avatar :src="row.coverUrls?.[0]" shape="square" /></template></el-table-column>
      <el-table-column prop="title" label="标题" />
      <el-table-column prop="description" label="描述" show-overflow-tooltip />
      <el-table-column prop="userId" label="用户ID" width="80" />
      <el-table-column prop="mediaCount" label="媒体数" width="80" />
      <el-table-column label="状态" width="80"><template #default="{ row }"><el-tag :type="row.isPublic ? 'success' : 'info'">{{ row.isPublic ? '公开' : '私密' }}</el-tag></template></el-table-column>
      <el-table-column prop="createdAt" label="创建时间" width="180" />
      <el-table-column label="操作" width="150"><template #default="{ row }"><el-button type="primary" size="small" @click="handleEdit(row)">编辑</el-button><el-button type="danger" size="small" @click="handleDelete(row)">删除</el-button></template></el-table-column>
    </el-table>

    <MobileAdminListShell v-else :items="pagedAlbums" :loading="loading" empty-text="暂无相册">
      <div v-for="album in pagedAlbums" :key="album.id" class="admin-mobile-card">
        <div class="card-head"><el-checkbox :model-value="selectedIds.includes(album.id)" @change="(val) => toggleSelect(album.id, val)" /><div class="card-title">{{ album.title || '未命名相册' }}</div><el-tag size="small" :type="album.isPublic ? 'success' : 'info'">{{ album.isPublic ? '公开' : '私密' }}</el-tag></div>
        <div class="card-meta">用户 {{ album.userId }} · 媒体 {{ album.mediaCount || 0 }}</div>
        <div class="card-content">{{ album.description || '无描述' }}</div>
        <div class="card-meta">{{ album.createdAt || '-' }}</div>
        <div class="card-actions"><el-button type="primary" size="small" @click="handleEdit(album)">编辑</el-button><el-button type="danger" size="small" @click="handleDelete(album)">删除</el-button></div>
      </div>
    </MobileAdminListShell>

    <MobileActionSheet v-model="showFilters" title="筛选与搜索">
      <el-form label-position="top">
        <el-form-item label="搜索"><el-input v-model="searchQuery" placeholder="标题/描述" clearable /></el-form-item>
        <el-form-item label="用户ID"><el-input v-model="userIdFilter" placeholder="用户ID" clearable /></el-form-item>
        <el-form-item label="状态"><el-select v-model="statusFilter" clearable class="w-full"><el-option label="全部" value="" /><el-option label="公开" value="public" /><el-option label="私密" value="private" /></el-select></el-form-item>
        <div class="grid grid-cols-2 gap-2"><el-button @click="handleReset">重置</el-button><el-button type="primary" @click="showFilters = false">完成</el-button></div>
      </el-form>
    </MobileActionSheet>

    <div class="mt-4 flex justify-center" v-if="filteredAlbums.length">
      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        background
        :page-sizes="[5, 10, 20]"
        layout="sizes, prev, pager, next, total"
        :total="filteredAlbums.length"
      />
    </div>

    <el-dialog v-model="showEditDialog" title="编辑相册" :width="isMobile ? '92%' : '400px'">
      <el-form :model="editForm" label-width="80px">
        <el-form-item label="标题"><el-input v-model="editForm.title" /></el-form-item>
        <el-form-item label="描述"><el-input v-model="editForm.description" type="textarea" /></el-form-item>
        <el-form-item label="公开"><el-switch v-model="editForm.isPublic" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="showEditDialog = false">取消</el-button><el-button type="primary" @click="saveEdit">保存</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '@/api'
import { useIsMobile } from '@/composables/useIsMobile'
import MobileAdminListShell from '@/components/admin/MobileAdminListShell.vue'
import MobileActionSheet from '@/components/admin/MobileActionSheet.vue'

const albums = ref([])
const selectedIds = ref([])
const loading = ref(false)
const showEditDialog = ref(false)
const editForm = ref({})
const editingId = ref(null)
const searchQuery = ref('')
const userIdFilter = ref('')
const statusFilter = ref('')
const showFilters = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const { isMobile } = useIsMobile()

const filteredAlbums = computed(() => albums.value.filter(album => {
  const matchSearch = !searchQuery.value || album.title?.toLowerCase().includes(searchQuery.value.toLowerCase()) || album.description?.toLowerCase().includes(searchQuery.value.toLowerCase())
  const matchUserId = !userIdFilter.value || String(album.userId) === userIdFilter.value
  const matchStatus = !statusFilter.value || (statusFilter.value === 'public' && album.isPublic) || (statusFilter.value === 'private' && !album.isPublic)
  return matchSearch && matchUserId && matchStatus
}))

const pagedAlbums = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  return filteredAlbums.value.slice(start, start + pageSize.value)
})

watch(
  () => filteredAlbums.value.length,
  (total) => {
    const maxPage = Math.max(1, Math.ceil(total / pageSize.value))
    if (currentPage.value > maxPage) currentPage.value = maxPage
  },
  { immediate: true }
)

const loadAlbums = async () => { loading.value = true; const res = await api.get('/albums/admin/all'); albums.value = res.data || []; loading.value = false }
const handleSelectionChange = (rows) => { selectedIds.value = rows.map(r => r.id) }
const toggleSelect = (id, checked) => { if (checked) { if (!selectedIds.value.includes(id)) selectedIds.value.push(id) } else { selectedIds.value = selectedIds.value.filter(v => v !== id) } }
const handleBatchDelete = async () => { await ElMessageBox.confirm(`确定删除选中的 ${selectedIds.value.length} 个相册？`, '批量删除'); await api.post('/albums/admin/batch-delete', { ids: selectedIds.value }); ElMessage.success('批量删除成功'); selectedIds.value = []; loadAlbums() }
const handleEdit = (row) => { editingId.value = row.id; editForm.value = { title: row.title, description: row.description, isPublic: row.isPublic }; showEditDialog.value = true }
const saveEdit = async () => { await api.put(`/albums/admin/${editingId.value}`, editForm.value); ElMessage.success('保存成功'); showEditDialog.value = false; loadAlbums() }
const handleDelete = async (row) => { await ElMessageBox.confirm('确定删除该相册？', '提示'); await api.delete(`/albums/admin/${row.id}`); ElMessage.success('删除成功'); selectedIds.value = selectedIds.value.filter(v => v !== row.id); loadAlbums() }
const handleSearch = () => { currentPage.value = 1 }
const handleReset = () => { searchQuery.value = ''; userIdFilter.value = ''; statusFilter.value = ''; currentPage.value = 1 }

onMounted(loadAlbums)
</script>

<style scoped>
.admin-mobile-card { border: 1px solid rgba(148, 163, 184, 0.24); border-radius: 12px; padding: 10px; background: rgba(255, 255, 255, 0.6); }
.card-head { display: flex; align-items: center; gap: 8px; }
.card-title { flex: 1; font-size: 14px; font-weight: 600; color: #334155; }
.card-content { margin-top: 8px; font-size: 13px; color: #475569; line-height: 1.5; }
.card-meta { margin-top: 6px; font-size: 12px; color: #64748b; }
.card-actions { margin-top: 10px; display: flex; justify-content: flex-end; gap: 8px; }
</style>
