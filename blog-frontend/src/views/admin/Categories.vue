<template>
  <div>
    <div class="flex justify-between items-center mb-4 gap-2">
      <h2 class="text-xl sm:text-2xl font-bold dark:text-white">分类管理</h2>
      <div class="flex gap-2">
        <el-button type="danger" :disabled="!selectedIds.length" @click="handleBatchDelete">批量删除 ({{ selectedIds.length }})</el-button>
        <el-button type="primary" @click="showDialog = true">新增分类</el-button>
      </div>
    </div>

    <div v-if="!isMobile" class="glass rounded-xl p-4 mb-4">
      <el-form :inline="true">
        <el-form-item label="搜索">
          <el-input v-model="searchQuery" placeholder="分类名称/描述" clearable @clear="handleSearch" style="width: 200px" />
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
      <el-button class="!h-10" type="primary" @click="showDialog = true">新增</el-button>
    </div>

    <div v-if="!isMobile" class="glass rounded-xl p-6">
      <el-table :data="filteredCategories" stripe @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="50" />
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="名称" />
        <el-table-column prop="description" label="描述" />
        <el-table-column prop="createdAt" label="创建时间" />
        <el-table-column label="操作" width="180">
          <template #default="{ row }">
            <el-button size="small" @click="handleEdit(row)">编辑</el-button>
            <el-popconfirm title="确定删除该分类？" @confirm="handleDelete(row.id)">
              <template #reference>
                <el-button type="danger" size="small">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <MobileAdminListShell v-else :items="filteredCategories" :loading="false" empty-text="暂无分类">
      <div v-for="category in filteredCategories" :key="category.id" class="admin-mobile-card">
        <div class="card-head">
          <el-checkbox :model-value="selectedIds.includes(category.id)" @change="(val) => toggleSelect(category.id, val)" />
          <div class="card-title">{{ category.name || '未命名分类' }}</div>
        </div>
        <div class="card-content">{{ category.description || '无描述' }}</div>
        <div class="card-meta">ID: {{ category.id }} · {{ category.createdAt || '-' }}</div>
        <div class="card-actions">
          <el-button size="small" @click="handleEdit(category)">编辑</el-button>
          <el-popconfirm title="确定删除该分类？" @confirm="handleDelete(category.id)">
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
          <el-input v-model="searchQuery" placeholder="分类名称/描述" clearable />
        </el-form-item>
        <div class="grid grid-cols-2 gap-2">
          <el-button @click="handleReset">重置</el-button>
          <el-button type="primary" @click="showFilters = false">完成</el-button>
        </div>
      </el-form>
    </MobileActionSheet>

    <el-dialog v-model="showDialog" :title="editingId ? '编辑分类' : '新增分类'" :width="isMobile ? '92%' : '400px'">
      <el-form :model="form">
        <el-form-item label="名称">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :autosize="{ minRows: 2, maxRows: 6 }" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showDialog = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { getAdminCategories, createCategory, updateCategory, deleteCategory } from '@/api/blog'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '@/api'
import { useIsMobile } from '@/composables/useIsMobile'
import MobileAdminListShell from '@/components/admin/MobileAdminListShell.vue'
import MobileActionSheet from '@/components/admin/MobileActionSheet.vue'

const categories = ref([])
const selectedIds = ref([])
const showDialog = ref(false)
const editingId = ref(null)
const form = ref({ name: '', description: '' })
const searchQuery = ref('')
const showFilters = ref(false)
const { isMobile } = useIsMobile()

const filteredCategories = computed(() => {
  return categories.value.filter(category => {
    return !searchQuery.value ||
      category.name?.toLowerCase().includes(searchQuery.value.toLowerCase()) ||
      category.description?.toLowerCase().includes(searchQuery.value.toLowerCase())
  })
})

const fetchCategories = async () => {
  const res = await getAdminCategories()
  if (res.success) categories.value = res.data
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

const handleEdit = (row) => {
  editingId.value = row.id
  form.value = { name: row.name, description: row.description }
  showDialog.value = true
}

const handleSubmit = async () => {
  if (editingId.value) {
    await updateCategory(editingId.value, form.value)
    ElMessage.success('更新成功')
  } else {
    await createCategory(form.value)
    ElMessage.success('创建成功')
  }
  showDialog.value = false
  editingId.value = null
  form.value = { name: '', description: '' }
  fetchCategories()
}

const handleDelete = async (id) => {
  await deleteCategory(id)
  ElMessage.success('删除成功')
  selectedIds.value = selectedIds.value.filter(v => v !== id)
  fetchCategories()
}

const handleBatchDelete = async () => {
  await ElMessageBox.confirm(`确定删除选中的 ${selectedIds.value.length} 个分类？`, '批量删除')
  await api.post('/admin/categories/batch-delete', { ids: selectedIds.value })
  ElMessage.success('批量删除成功')
  selectedIds.value = []
  fetchCategories()
}

const handleSearch = () => {}

const handleReset = () => {
  searchQuery.value = ''
}

onMounted(fetchCategories)
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
  gap: 8px;
}
</style>
