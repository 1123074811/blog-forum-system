<template>
  <div>
    <div class="flex justify-between items-center mb-4">
      <h1 class="text-xl sm:text-2xl font-bold">媒体管理</h1>
      <el-button type="danger" :disabled="!selectedIds.length" @click="handleBatchDelete">批量删除 ({{ selectedIds.length }})</el-button>
    </div>

    <div class="glass rounded-xl p-4 mb-4" v-if="!isMobile">
      <el-form :inline="true">
        <el-form-item label="搜索"><el-input v-model="searchQuery" placeholder="标题/描述" clearable @clear="handleSearch" style="width: 200px" /></el-form-item>
        <el-form-item label="类型"><el-select v-model="typeFilter" placeholder="全部" clearable style="width: 120px"><el-option label="全部" value="" /><el-option label="图片" value="image" /><el-option label="视频" value="video" /></el-select></el-form-item>
        <el-form-item label="状态"><el-select v-model="statusFilter" placeholder="全部" clearable style="width: 120px"><el-option label="全部" value="" /><el-option label="公开" value="public" /><el-option label="私密" value="private" /></el-select></el-form-item>
        <el-form-item label="用户ID"><el-input v-model="userIdFilter" placeholder="用户ID" clearable style="width: 120px" /></el-form-item>
        <el-form-item><el-button type="primary" @click="handleSearch">搜索</el-button><el-button @click="handleReset">重置</el-button></el-form-item>
      </el-form>
    </div>

    <div v-else class="mb-3 flex gap-2">
      <el-button class="!h-10" @click="showFilters = true">筛选</el-button>
      <el-button class="!h-10" type="danger" plain :disabled="!selectedIds.length" @click="handleBatchDelete">批量删 ({{ selectedIds.length }})</el-button>
    </div>

    <el-table v-if="!isMobile" :data="pagedMedia" v-loading="loading" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="50" />
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column label="预览" width="100"><template #default="{ row }"><img v-if="row.type === 'image'" :src="normalizeUnsafeUrl(row.thumbnailUrl || row.url)" :alt="row.title || '媒体预览图'" loading="lazy" class="w-16 h-16 object-cover rounded" /><video v-else :src="normalizeUnsafeUrl(row.url)" preload="none" class="w-16 h-16 object-cover rounded" /></template></el-table-column>
      <el-table-column prop="title" label="标题" show-overflow-tooltip />
      <el-table-column prop="description" label="描述" show-overflow-tooltip />
      <el-table-column prop="type" label="类型" width="80" />
      <el-table-column prop="userId" label="用户ID" width="80" />
      <el-table-column prop="albumId" label="相册ID" width="80" />
      <el-table-column label="状态" width="80"><template #default="{ row }"><el-tag :type="!!row.isPublic ? 'success' : 'info'">{{ !!row.isPublic ? '公开' : '私密' }}</el-tag></template></el-table-column>
      <el-table-column prop="createdAt" label="创建时间" width="180" />
      <el-table-column label="操作" width="150"><template #default="{ row }"><el-button type="primary" size="small" @click="handleEdit(row)">编辑</el-button><el-button type="danger" size="small" @click="handleDelete(row)">删除</el-button></template></el-table-column>
    </el-table>

    <MobileAdminListShell v-else :items="pagedMedia" :loading="loading" empty-text="暂无媒体">
      <div v-for="media in pagedMedia" :key="media.id" class="admin-mobile-card">
        <div class="card-head"><el-checkbox :model-value="selectedIds.includes(media.id)" @change="(val) => toggleSelect(media.id, val)" /><div class="card-title">{{ media.title || '未命名媒体' }}</div><el-tag size="small" :type="!!media.isPublic ? 'success' : 'info'">{{ !!media.isPublic ? '公开' : '私密' }}</el-tag></div>
        <div class="card-preview"><img v-if="media.type === 'image'" :src="normalizeUnsafeUrl(media.thumbnailUrl || media.url)" :alt="media.title || '媒体预览图'" loading="lazy" class="preview-el" /><video v-else :src="normalizeUnsafeUrl(media.url)" preload="none" class="preview-el" /></div>
        <div class="card-meta">类型 {{ media.type }} · 用户 {{ media.userId }} · 相册 {{ media.albumId || '-' }}</div>
        <div class="card-content">{{ media.description || '无描述' }}</div>
        <div class="card-actions"><el-button type="primary" size="small" @click="handleEdit(media)">编辑</el-button><el-button type="danger" size="small" @click="handleDelete(media)">删除</el-button></div>
      </div>
    </MobileAdminListShell>

    <MobileActionSheet v-model="showFilters" title="筛选与搜索">
      <el-form label-position="top">
        <el-form-item label="搜索"><el-input v-model="searchQuery" placeholder="标题/描述" clearable /></el-form-item>
        <el-form-item label="类型"><el-select v-model="typeFilter" clearable class="w-full"><el-option label="全部" value="" /><el-option label="图片" value="image" /><el-option label="视频" value="video" /></el-select></el-form-item>
        <el-form-item label="状态"><el-select v-model="statusFilter" clearable class="w-full"><el-option label="全部" value="" /><el-option label="公开" value="public" /><el-option label="私密" value="private" /></el-select></el-form-item>
        <el-form-item label="用户ID"><el-input v-model="userIdFilter" placeholder="用户ID" clearable /></el-form-item>
        <div class="grid grid-cols-2 gap-2"><el-button @click="handleReset">重置</el-button><el-button type="primary" @click="showFilters = false">完成</el-button></div>
      </el-form>
    </MobileActionSheet>

    <div class="mt-4 flex justify-center" v-if="filteredMedia.length">
      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        background
        :page-sizes="[5, 10, 20]"
        layout="sizes, prev, pager, next, total"
        :total="filteredMedia.length"
      />
    </div>

    <el-dialog v-model="showEditDialog" title="编辑媒体" :width="isMobile ? '92%' : '400px'">
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
import { normalizeUnsafeUrl } from '@/utils/image'

const mediaList = ref([])
const selectedIds = ref([])
const loading = ref(false)
const showEditDialog = ref(false)
const editForm = ref({})
const editingId = ref(null)
const searchQuery = ref('')
const typeFilter = ref('')
const statusFilter = ref('')
const userIdFilter = ref('')
const showFilters = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const { isMobile } = useIsMobile()

const filteredMedia = computed(() => mediaList.value.filter(media => {
  const matchSearch = !searchQuery.value || media.title?.toLowerCase().includes(searchQuery.value.toLowerCase()) || media.description?.toLowerCase().includes(searchQuery.value.toLowerCase())
  const matchType = !typeFilter.value || media.type === typeFilter.value
  const matchStatus = !statusFilter.value || (statusFilter.value === 'public' && !!media.isPublic) || (statusFilter.value === 'private' && !media.isPublic)
  const matchUserId = !userIdFilter.value || String(media.userId) === userIdFilter.value
  return matchSearch && matchType && matchStatus && matchUserId
}))

const pagedMedia = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  return filteredMedia.value.slice(start, start + pageSize.value)
})

watch(
  () => filteredMedia.value.length,
  (total) => {
    const maxPage = Math.max(1, Math.ceil(total / pageSize.value))
    if (currentPage.value > maxPage) currentPage.value = maxPage
  },
  { immediate: true }
)

const loadMedia = async () => { loading.value = true; const res = await api.get('/media/admin/all'); mediaList.value = res.data || []; loading.value = false }
const handleSelectionChange = (rows) => { selectedIds.value = rows.map(r => r.id) }
const toggleSelect = (id, checked) => { if (checked) { if (!selectedIds.value.includes(id)) selectedIds.value.push(id) } else { selectedIds.value = selectedIds.value.filter(v => v !== id) } }
const handleBatchDelete = async () => { await ElMessageBox.confirm(`确定删除选中的 ${selectedIds.value.length} 个媒体？`, '批量删除'); await api.post('/media/admin/batch-delete', { ids: selectedIds.value }); ElMessage.success('批量删除成功'); selectedIds.value = []; loadMedia() }
const handleEdit = (row) => { editingId.value = row.id; editForm.value = { title: row.title, description: row.description, isPublic: !!row.isPublic }; showEditDialog.value = true }
const saveEdit = async () => { await api.put(`/media/admin/${editingId.value}`, editForm.value); ElMessage.success('保存成功'); showEditDialog.value = false; loadMedia() }
const handleDelete = async (row) => { await ElMessageBox.confirm('确定删除该媒体？', '提示'); await api.delete(`/media/admin/${row.id}`); ElMessage.success('删除成功'); selectedIds.value = selectedIds.value.filter(v => v !== row.id); loadMedia() }
const handleSearch = () => { currentPage.value = 1 }
const handleReset = () => { searchQuery.value = ''; typeFilter.value = ''; statusFilter.value = ''; userIdFilter.value = ''; currentPage.value = 1 }

onMounted(loadMedia)
</script>

<style scoped>
.admin-mobile-card { border: 1px solid rgba(148, 163, 184, 0.24); border-radius: 12px; padding: 10px; background: rgba(255, 255, 255, 0.6); }
.card-head { display: flex; align-items: center; gap: 8px; }
.card-title { flex: 1; font-size: 14px; font-weight: 600; color: #334155; }
.card-preview { margin-top: 8px; }
.preview-el { width: 100%; height: 140px; border-radius: 10px; object-fit: cover; }
.card-content { margin-top: 8px; font-size: 13px; color: #475569; line-height: 1.5; }
.card-meta { margin-top: 6px; font-size: 12px; color: #64748b; }
.card-actions { margin-top: 10px; display: flex; justify-content: flex-end; gap: 8px; }
</style>
