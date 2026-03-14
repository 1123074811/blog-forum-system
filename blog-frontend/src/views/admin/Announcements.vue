<template>
  <div class="p-3 sm:p-6">
    <div class="bg-white dark:bg-gray-800 rounded-lg shadow-md p-4 sm:p-6">
      <div class="flex justify-between items-center mb-4">
        <h1 class="text-xl sm:text-2xl font-bold text-gray-800 dark:text-white">公告管理</h1>
        <el-button type="primary" @click="showAddDialog = true">
          <el-icon><Plus /></el-icon>
          添加公告
        </el-button>
      </div>

      <el-table v-if="!isMobile" :data="announcements" stripe>
        <el-table-column prop="title" label="标题" min-width="200" />
        <el-table-column prop="type" label="类型" width="100">
          <template #default="{ row }">
            <el-tag :type="getTagType(row.type)">{{ getTypeText(row.type) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="isPinned" label="置顶" width="80">
          <template #default="{ row }">
            <el-tag v-if="row.isPinned" type="warning" size="small">置顶</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="isActive" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.isActive ? 'success' : 'danger'" size="small">{{ row.isActive ? '启用' : '禁用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="sortOrder" label="排序" width="80" />
        <el-table-column prop="createdAt" label="创建时间" width="180" />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <div class="flex gap-2">
              <el-button type="primary" size="small" :icon="Edit" @click="editAnnouncement(row)">编辑</el-button>
              <el-button type="danger" size="small" :icon="Delete" @click="deleteAnnouncementItem(row.id)" plain>删除</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <MobileAdminListShell v-else :items="announcements" :loading="false" empty-text="暂无公告">
        <div v-for="item in announcements" :key="item.id" class="admin-mobile-card">
          <div class="card-head">
            <div class="card-title">{{ item.title }}</div>
            <el-tag size="small" :type="getTagType(item.type)">{{ getTypeText(item.type) }}</el-tag>
          </div>
          <div class="card-meta">
            <el-tag v-if="item.isPinned" type="warning" size="small">置顶</el-tag>
            <el-tag :type="item.isActive ? 'success' : 'danger'" size="small">{{ item.isActive ? '启用' : '禁用' }}</el-tag>
          </div>
          <div class="card-content">{{ item.content }}</div>
          <div class="card-meta">排序 {{ item.sortOrder }} · {{ item.createdAt || '-' }}</div>
          <div class="card-actions">
            <el-button type="primary" size="small" @click="editAnnouncement(item)">编辑</el-button>
            <el-button type="danger" size="small" @click="deleteAnnouncementItem(item.id)">删除</el-button>
          </div>
        </div>
      </MobileAdminListShell>
    </div>

    <el-dialog v-model="showAddDialog" :title="editingId ? '编辑公告' : '添加公告'" :width="isMobile ? '94%' : '600px'">
      <el-form :model="form" label-width="80px">
        <el-form-item label="标题" required><el-input v-model="form.title" /></el-form-item>
        <el-form-item label="内容" required><el-input v-model="form.content" type="textarea" :rows="5" /></el-form-item>
        <el-form-item label="类型">
          <el-select v-model="form.type">
            <el-option label="信息" value="info" />
            <el-option label="成功" value="success" />
            <el-option label="警告" value="warning" />
            <el-option label="错误" value="error" />
          </el-select>
        </el-form-item>
        <el-form-item label="置顶"><el-switch v-model="form.isPinned" /></el-form-item>
        <el-form-item label="展示时间">
          <el-date-picker
            v-model="dateRange"
            type="datetimerange"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            value-format="YYYY-MM-DD HH:mm:ss"
            @change="handleDateRangeChange"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="启用"><el-switch v-model="form.isActive" /></el-form-item>
        <el-form-item label="排序"><el-input-number v-model="form.sortOrder" :min="0" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAddDialog = false">取消</el-button>
        <el-button type="primary" @click="saveAnnouncement" :loading="saving">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getAllAnnouncements, createAnnouncement, updateAnnouncement, deleteAnnouncement } from '@/api/blog'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Edit, Delete } from '@element-plus/icons-vue'
import { useIsMobile } from '@/composables/useIsMobile'
import MobileAdminListShell from '@/components/admin/MobileAdminListShell.vue'

const announcements = ref([])
const showAddDialog = ref(false)
const saving = ref(false)
const editingId = ref(null)
const dateRange = ref([])
const { isMobile } = useIsMobile()

const form = ref({ title: '', content: '', type: 'info', isPinned: false, isActive: true, sortOrder: 0, startTime: '', endTime: '' })

const loadData = async () => {
  try {
    const res = await getAllAnnouncements()
    if (res.success) announcements.value = res.data || []
  } catch (error) {
    ElMessage.error('加载数据失败')
  }
}

const editAnnouncement = (announcement) => {
  editingId.value = announcement.id
  Object.keys(form.value).forEach(key => {
    if (key !== 'startTime' && key !== 'endTime') form.value[key] = announcement[key]
  })
  if (announcement.startTime && announcement.endTime) {
    dateRange.value = [announcement.startTime, announcement.endTime]
    form.value.startTime = announcement.startTime
    form.value.endTime = announcement.endTime
  } else {
    dateRange.value = []
    form.value.startTime = ''
    form.value.endTime = ''
  }
  showAddDialog.value = true
}

const handleDateRangeChange = (val) => {
  if (val) {
    form.value.startTime = val[0]
    form.value.endTime = val[1]
  } else {
    form.value.startTime = ''
    form.value.endTime = ''
  }
}

const saveAnnouncement = async () => {
  if (!form.value.title || !form.value.content) {
    ElMessage.warning('请填写标题和内容')
    return
  }

  saving.value = true
  try {
    const submitData = { ...form.value }
    if (!submitData.startTime) submitData.startTime = null
    if (!submitData.endTime) submitData.endTime = null

    let res
    if (editingId.value) {
      res = await updateAnnouncement(editingId.value, submitData)
      if (res.success) ElMessage.success('更新成功')
    } else {
      res = await createAnnouncement(submitData)
      if (res.success) ElMessage.success('添加成功')
    }
    showAddDialog.value = false
    resetForm()
    loadData()
  } catch (error) {
    ElMessage.error('操作失败')
  } finally {
    saving.value = false
  }
}

const deleteAnnouncementItem = async (id) => {
  try {
    await ElMessageBox.confirm('确定要删除这条公告吗？', '确认删除', { type: 'warning' })
    const res = await deleteAnnouncement(id)
    if (res.success) {
      ElMessage.success('删除成功')
      loadData()
    }
  } catch (error) {
    if (error !== 'cancel') ElMessage.error('删除失败')
  }
}

const resetForm = () => {
  editingId.value = null
  form.value = { title: '', content: '', type: 'info', isPinned: false, isActive: true, sortOrder: 0, startTime: '', endTime: '' }
  dateRange.value = []
}

const getTagType = (type) => ({ info: 'primary', success: 'success', warning: 'warning', error: 'danger' }[type] || 'primary')
const getTypeText = (type) => ({ info: '信息', success: '成功', warning: '警告', error: '错误' }[type] || '信息')

onMounted(loadData)
</script>

<style scoped>
.admin-mobile-card { border: 1px solid rgba(148, 163, 184, 0.24); border-radius: 12px; padding: 10px; background: rgba(255, 255, 255, 0.6); margin-bottom: 10px; }
.card-head { display: flex; align-items: flex-start; gap: 8px; }
.card-title { flex: 1; font-size: 14px; font-weight: 600; color: #334155; line-height: 1.4; }
.card-meta { margin-top: 6px; font-size: 12px; color: #64748b; display: flex; gap: 6px; flex-wrap: wrap; }
.card-content { margin-top: 8px; font-size: 13px; color: #475569; line-height: 1.5; display: -webkit-box; -webkit-line-clamp: 3; -webkit-box-orient: vertical; overflow: hidden; }
.card-actions { margin-top: 10px; display: flex; justify-content: flex-end; gap: 8px; }
</style>
