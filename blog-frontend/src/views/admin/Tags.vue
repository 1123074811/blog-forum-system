<template>
  <div>
    <div class="flex justify-between items-center mb-6">
      <h2 class="text-2xl font-bold dark:text-white">标签管理</h2>
      <el-button type="primary" @click="showDialog = true">新增标签</el-button>
    </div>
    <div class="glass rounded-xl p-6">
      <el-table :data="tags" stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="名称" />
        <el-table-column prop="createdAt" label="创建时间" />
        <el-table-column label="操作" width="180">
          <template #default="{ row }">
            <el-button size="small" @click="handleEdit(row)">编辑</el-button>
            <el-popconfirm title="确定删除该标签？" @confirm="handleDelete(row.id)">
              <template #reference>
                <el-button type="danger" size="small">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="showDialog" :title="editingId ? '编辑标签' : '新增标签'" width="400px">
      <el-form :model="form">
        <el-form-item label="名称">
          <el-input v-model="form.name" />
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
import { ref, onMounted } from 'vue'
import { getAdminTags, createTag, updateTag, deleteTag } from '@/api/blog'
import { ElMessage } from 'element-plus'

const tags = ref([])
const showDialog = ref(false)
const editingId = ref(null)
const form = ref({ name: '' })

const fetchTags = async () => {
  const res = await getAdminTags()
  if (res.success) tags.value = res.data
}

const handleEdit = (row) => {
  editingId.value = row.id
  form.value = { name: row.name }
  showDialog.value = true
}

const handleSubmit = async () => {
  if (editingId.value) {
    await updateTag(editingId.value, form.value)
    ElMessage.success('更新成功')
  } else {
    await createTag(form.value)
    ElMessage.success('创建成功')
  }
  showDialog.value = false
  editingId.value = null
  form.value = { name: '' }
  fetchTags()
}

const handleDelete = async (id) => {
  await deleteTag(id)
  ElMessage.success('删除成功')
  fetchTags()
}

onMounted(fetchTags)
</script>
