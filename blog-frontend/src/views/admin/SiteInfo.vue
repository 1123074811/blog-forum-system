<template>
  <div class="site-info-page p-3 sm:p-6">
    <div class="bg-white dark:bg-gray-800 rounded-lg shadow-md p-4 sm:p-6">
      <h1 class="text-xl sm:text-2xl font-bold text-gray-800 dark:text-white mb-4 sm:mb-6">网站设置</h1>

      <el-form :model="form" :label-width="isMobile ? '96px' : '120px'" class="max-w-2xl">
        <el-collapse v-model="activePanels">
          <el-collapse-item title="基础信息" name="base">
            <el-form-item label="网站名称"><el-input v-model="form.siteName" /></el-form-item>
            <el-form-item label="网站描述"><el-input v-model="form.siteDescription" type="textarea" :rows="3" /></el-form-item>
            <el-form-item label="关键词"><el-input v-model="form.siteKeywords" placeholder="用逗号分隔" /></el-form-item>
            <el-form-item label="网站Logo"><el-input v-model="form.siteLogo" placeholder="Logo图片URL" /></el-form-item>
            <el-form-item label="网站图标"><el-input v-model="form.siteFavicon" placeholder="Favicon图片URL" /></el-form-item>
          </el-collapse-item>

          <el-collapse-item title="联系信息" name="contact">
            <el-form-item label="联系邮箱"><el-input v-model="form.contactEmail" /></el-form-item>
            <el-form-item label="联系电话"><el-input v-model="form.contactPhone" /></el-form-item>
            <el-form-item label="联系地址"><el-input v-model="form.contactAddress" /></el-form-item>
            <el-form-item label="QQ"><el-input v-model="form.contactQq" /></el-form-item>
            <el-form-item label="微信"><el-input v-model="form.contactWechat" /></el-form-item>
          </el-collapse-item>

          <el-collapse-item title="社交链接" name="social">
            <el-form-item label="GitHub"><el-input v-model="form.githubUrl" /></el-form-item>
            <el-form-item label="Gitee"><el-input v-model="form.giteeUrl" /></el-form-item>
          </el-collapse-item>

          <el-collapse-item title="备案信息" name="record">
            <el-form-item label="ICP备案号"><el-input v-model="form.icpNumber" /></el-form-item>
            <el-form-item label="公安备案号"><el-input v-model="form.policeNumber" /></el-form-item>
          </el-collapse-item>
        </el-collapse>
      </el-form>
    </div>

    <div class="site-info-actions">
      <el-button @click="reset" class="action-btn">重置</el-button>
      <el-button type="primary" @click="save" :loading="saving" class="action-btn">保存设置</el-button>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getSiteInfo, updateSiteInfo } from '@/api/blog'
import { ElMessage } from 'element-plus'
import { useIsMobile } from '@/composables/useIsMobile'

const saving = ref(false)
const originalData = ref({})
const activePanels = ref(['base', 'contact'])
const { isMobile } = useIsMobile()

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

<style scoped>
.site-info-page {
  padding-bottom: calc(80px + env(safe-area-inset-bottom));
}

.site-info-actions {
  position: fixed;
  left: 10px;
  right: 10px;
  bottom: calc(82px + env(safe-area-inset-bottom));
  z-index: 50;
  border-radius: 14px;
  padding: 10px;
  background: rgba(255, 255, 255, 0.94);
  border: 1px solid rgba(148, 163, 184, 0.2);
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.08);
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}

.action-btn {
  height: 42px;
}

@media (min-width: 769px) {
  .site-info-actions {
    position: static;
    margin-top: 16px;
    border-radius: 0;
    padding: 0;
    background: transparent;
    border: none;
    box-shadow: none;
    display: flex;
    justify-content: flex-start;
    gap: 10px;
  }

  .site-info-page {
    padding-bottom: 0;
  }

  .action-btn {
    width: auto;
    min-width: 110px;
  }
}
</style>
