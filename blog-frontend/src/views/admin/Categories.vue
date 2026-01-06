<template>
  <div>
    <div class="flex justify-between items-center mb-6">
      <h2 class="text-2xl font-bold dark:text-white">分类管理</h2>
      <el-button type="primary" @click="showDialog = true">新增分类</el-button>
    </div>
    <div class="glass rounded-xl p-6">
      <el-table :data="categories" stripe>
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

    <el-dialog v-model="showDialog" :title="editingId ? '编辑分类' : '新增分类'" width="400px">
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
import { ref, onMounted } from 'vue'
import { getAdminCategories, createCategory, updateCategory, deleteCategory } from '@/api/blog'
import { ElMessage } from 'element-plus'

const categories = ref([])
const showDialog = ref(false)
const editingId = ref(null)
const form = ref({ name: '', description: '' })

const fetchCategories = async () => {
  const res = await getAdminCategories()
  if (res.success) categories.value = res.data
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
  fetchCategories()
}

onMounted(fetchCategories)
</script>
