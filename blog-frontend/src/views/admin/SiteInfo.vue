<template>
  <div class="p-6">
    <div class="bg-white dark:bg-gray-800 rounded-lg shadow-md p-6">
      <h1 class="text-2xl font-bold text-gray-800 dark:text-white mb-6">网站设置</h1>
      
      <el-form :model="form" label-width="120px" class="max-w-2xl">
        <el-form-item label="网站名称">
          <el-input v-model="form.siteName" />
        </el-form-item>
        
        <el-form-item label="网站描述">
          <el-input v-model="form.siteDescription" type="textarea" :rows="3" />
        </el-form-item>
        
        <el-form-item label="关键词">
          <el-input v-model="form.siteKeywords" placeholder="用逗号分隔" />
        </el-form-item>
        
        <el-form-item label="网站Logo">
          <el-input v-model="form.siteLogo" placeholder="Logo图片URL" />
        </el-form-item>
        
        <el-form-item label="网站图标">
          <el-input v-model="form.siteFavicon" placeholder="Favicon图片URL" />
        </el-form-item>
        
        <el-divider content-position="left">联系信息</el-divider>
        
        <el-form-item label="联系邮箱">
          <el-input v-model="form.contactEmail" />
        </el-form-item>
        
        <el-form-item label="联系电话">
          <el-input v-model="form.contactPhone" />
        </el-form-item>
        
        <el-form-item label="联系地址">
          <el-input v-model="form.contactAddress" />
        </el-form-item>
        
        <el-form-item label="QQ">
          <el-input v-model="form.contactQq" />
        </el-form-item>
        
        <el-form-item label="微信">
          <el-input v-model="form.contactWechat" />
        </el-form-item>
        
        <el-divider content-position="left">社交链接</el-divider>
        
        <el-form-item label="GitHub">
          <el-input v-model="form.githubUrl" />
        </el-form-item>
        
        <el-form-item label="Gitee">
          <el-input v-model="form.giteeUrl" />
        </el-form-item>
        
        <el-divider content-position="left">备案信息</el-divider>
        
        <el-form-item label="ICP备案号">
          <el-input v-model="form.icpNumber" />
        </el-form-item>
        
        <el-form-item label="公安备案号">
          <el-input v-model="form.policeNumber" />
        </el-form-item>
        
        <el-form-item>
          <el-button type="primary" @click="save" :loading="saving">保存设置</el-button>
          <el-button @click="reset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getSiteInfo, updateSiteInfo } from '@/api/blog'
import { ElMessage } from 'element-plus'

const saving = ref(false)
const originalData = ref({})

const form = ref({
  siteName: '',
  siteDescription: '',
  siteKeywords: '',
  siteLogo: '',
  siteFavicon: '',
  contactEmail: '',
  contactPhone: '',
  contactAddress: '',
  contactQq: '',
  contactWechat: '',
  githubUrl: '',
  giteeUrl: '',
  icpNumber: '',
  policeNumber: ''
})

const loadData = async () => {
  try {
    const res = await getSiteInfo()
    if (res.success && res.data) {
      Object.keys(form.value).forEach(key => {
        form.value[key] = res.data[key] || ''
      })
      originalData.value = { ...form.value }
    }
  } catch (error) {
    ElMessage.error('加载数据失败')
  }
}

const save = async () => {
  saving.value = true
  try {
    const res = await updateSiteInfo(form.value)
    if (res.success) {
      originalData.value = { ...form.value }
      ElMessage.success('保存成功')
    }
  } catch (error) {
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

const reset = () => {
  form.value = { ...originalData.value }
}

onMounted(() => {
  loadData()
})
</script>