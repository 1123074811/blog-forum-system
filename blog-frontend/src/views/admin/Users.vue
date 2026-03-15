<template>
  <div>
    <div class="flex justify-between items-center mb-4">
      <h2 class="text-xl sm:text-2xl font-bold dark:text-white">用户管理</h2>
      <el-button type="danger" :disabled="!selectedIds.length" @click="handleBatchDelete">批量删除 ({{ selectedIds.length }})</el-button>
    </div>

    <div v-if="!isMobile" class="glass rounded-xl p-4 mb-4">
      <el-form :inline="true">
        <el-form-item label="搜索">
          <el-input v-model="searchQuery" placeholder="用户名/邮箱" clearable @clear="handleSearch" style="width: 200px" />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="roleFilter" placeholder="全部" clearable style="width: 120px">
            <el-option label="全部" value="" />
            <el-option label="管理员" value="admin" />
            <el-option label="普通用户" value="user" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="statusFilter" placeholder="全部" clearable style="width: 120px">
            <el-option label="全部" value="" />
            <el-option label="正常" value="normal" />
            <el-option label="已封禁" value="banned" />
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
      <el-table :data="pagedUsers" stripe @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="50" :selectable="row => row.role !== 'admin'" />
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="username" label="用户名" />
        <el-table-column prop="email" label="邮箱" />
        <el-table-column prop="role" label="角色">
          <template #default="{ row }">
            <el-tag :type="row.role === 'admin' ? 'danger' : ''">{{ row.role }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="banned" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.banned ? 'danger' : 'success'">{{ row.banned ? '已封禁' : '正常' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="注册时间" />
        <el-table-column label="操作" width="200">
          <template #default="{ row }">
            <el-switch
              v-model="row.banned"
              :disabled="row.role === 'admin'"
              active-text="封禁"
              inactive-text="正常"
              @change="handleBanChange(row)"
              style="--el-switch-on-color: #f56c6c; --el-switch-off-color: #67c23a; margin-right: 10px;"
            />
            <el-popconfirm title="确定删除该用户？" @confirm="handleDelete(row.id)">
              <template #reference>
                <el-button type="danger" size="small" :disabled="row.role === 'admin'">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <MobileAdminListShell v-else :items="pagedUsers" :loading="false" empty-text="暂无用户">
      <div v-for="user in pagedUsers" :key="user.id" class="admin-mobile-card">
        <div class="card-head">
          <el-checkbox
            :disabled="user.role === 'admin'"
            :model-value="selectedIds.includes(user.id)"
            @change="(val) => toggleSelect(user.id, val)"
          />
          <div class="card-title">{{ user.username || '未知用户' }}</div>
          <el-tag size="small" :type="user.role === 'admin' ? 'danger' : 'info'">{{ user.role }}</el-tag>
        </div>
        <div class="card-meta">{{ user.email || '-' }}</div>
        <div class="card-meta">状态: {{ user.banned ? '已封禁' : '正常' }}</div>
        <div class="card-meta">注册: {{ user.createdAt || '-' }}</div>
        <div class="card-actions">
          <el-switch
            v-model="user.banned"
            :disabled="user.role === 'admin'"
            @change="handleBanChange(user)"
            style="margin-right: 8px;"
          />
          <el-popconfirm title="确定删除该用户？" @confirm="handleDelete(user.id)">
            <template #reference>
              <el-button type="danger" size="small" :disabled="user.role === 'admin'">删除</el-button>
            </template>
          </el-popconfirm>
        </div>
      </div>
    </MobileAdminListShell>

    <MobileActionSheet v-model="showFilters" title="筛选与搜索">
      <el-form label-position="top">
        <el-form-item label="搜索">
          <el-input v-model="searchQuery" placeholder="用户名/邮箱" clearable />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="roleFilter" clearable class="w-full">
            <el-option label="全部" value="" />
            <el-option label="管理员" value="admin" />
            <el-option label="普通用户" value="user" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="statusFilter" clearable class="w-full">
            <el-option label="全部" value="" />
            <el-option label="正常" value="normal" />
            <el-option label="已封禁" value="banned" />
          </el-select>
        </el-form-item>
        <div class="grid grid-cols-2 gap-2">
          <el-button @click="handleReset">重置</el-button>
          <el-button type="primary" @click="showFilters = false">完成</el-button>
        </div>
      </el-form>
    </MobileActionSheet>

    <div class="mt-4 flex justify-center" v-if="filteredUsers.length">
      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        background
        :page-sizes="[5, 10, 20]"
        layout="sizes, prev, pager, next, total"
        :total="filteredUsers.length"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { getAdminUsers, deleteUser } from '@/api/blog'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '@/api'
import { getStepUpToken } from '@/utils/stepUp'
import { useIsMobile } from '@/composables/useIsMobile'
import MobileAdminListShell from '@/components/admin/MobileAdminListShell.vue'
import MobileActionSheet from '@/components/admin/MobileActionSheet.vue'

const users = ref([])
const selectedIds = ref([])
const searchQuery = ref('')
const roleFilter = ref('')
const statusFilter = ref('')
const showFilters = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const { isMobile } = useIsMobile()

const filteredUsers = computed(() => {
  return users.value.filter(user => {
    const matchSearch = !searchQuery.value ||
      user.username?.toLowerCase().includes(searchQuery.value.toLowerCase()) ||
      user.email?.toLowerCase().includes(searchQuery.value.toLowerCase())

    const matchRole = !roleFilter.value || user.role === roleFilter.value

    const matchStatus = !statusFilter.value ||
      (statusFilter.value === 'banned' && user.banned) ||
      (statusFilter.value === 'normal' && !user.banned)

    return matchSearch && matchRole && matchStatus
  })
})

const pagedUsers = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  return filteredUsers.value.slice(start, start + pageSize.value)
})

watch(
  () => filteredUsers.value.length,
  (total) => {
    const maxPage = Math.max(1, Math.ceil(total / pageSize.value))
    if (currentPage.value > maxPage) currentPage.value = maxPage
  },
  { immediate: true }
)

const fetchUsers = async () => {
  const res = await getAdminUsers()
  if (res.success) users.value = res.data
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
  await deleteUser(id)
  ElMessage.success('删除成功')
  selectedIds.value = selectedIds.value.filter(v => v !== id)
  fetchUsers()
}

const handleBatchDelete = async () => {
  await ElMessageBox.confirm(`确定删除选中的 ${selectedIds.value.length} 个用户？`, '批量删除')
  const stepUpToken = await getStepUpToken('batch_delete_users')
  await api.post('/admin/users/batch-delete', { ids: selectedIds.value, stepUpToken })
  ElMessage.success('批量删除成功')
  selectedIds.value = []
  fetchUsers()
}

const handleBanChange = async (row) => {
  try {
    await api.put(`/admin/users/${row.id}/ban`, { banned: row.banned })
    ElMessage.success(row.banned ? '已封禁该用户' : '已解除封禁')
  } catch (error) {
    ElMessage.error('操作失败')
    row.banned = !row.banned
  }
}

const handleSearch = () => {
  currentPage.value = 1
}

const handleReset = () => {
  searchQuery.value = ''
  roleFilter.value = ''
  statusFilter.value = ''
  currentPage.value = 1
}

onMounted(fetchUsers)
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
  align-items: center;
  justify-content: flex-end;
}
</style>

