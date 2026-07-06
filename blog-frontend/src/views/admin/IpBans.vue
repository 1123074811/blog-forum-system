<template>
  <div>
    <h2 class="text-xl sm:text-2xl font-bold mb-4 dark:text-white">IP 封禁管理</h2>

    <div class="glass rounded-xl p-4">
      <el-form :model="banForm" label-width="80px" class="mb-4" @submit.prevent>
        <div class="grid grid-cols-1 md:grid-cols-4 gap-3">
          <el-form-item label="IP 地址" class="mb-0">
            <el-input v-model.trim="banForm.ip" placeholder="例如 203.0.113.10" clearable />
          </el-form-item>
          <el-form-item label="原因" class="mb-0">
            <el-select v-model="banForm.reason" class="w-full">
              <el-option label="安全策略" value="security_policy" />
              <el-option label="登录暴力破解" value="login_bruteforce" />
              <el-option label="未授权访问" value="unauthorized_access" />
              <el-option label="频率限制超限" value="rate_limit_exceeded" />
              <el-option label="高频异常访问" value="hotspot_ddos" />
            </el-select>
          </el-form-item>
          <el-form-item label="时长" class="mb-0">
            <el-input-number
              v-model="banForm.durationMinutes"
              :min="0"
              :max="43200"
              :step="30"
              controls-position="right"
              class="w-full"
            />
          </el-form-item>
          <el-form-item label-width="0" class="mb-0">
            <el-button type="danger" :loading="submitting" @click="handleBan">封禁 IP</el-button>
          </el-form-item>
        </div>
      </el-form>

      <div class="flex items-center justify-between mb-4">
        <div class="text-sm text-slate-500">共 {{ bannedIps.length }} 条有效封禁记录，时长填 0 表示永久</div>
        <el-button type="primary" size="small" @click="fetchBannedIps" :loading="loading">
          <el-icon><Refresh /></el-icon>
          <span class="ml-1">刷新</span>
        </el-button>
      </div>

      <el-table :data="bannedIps" v-loading="loading" empty-text="暂无封禁记录" stripe
        class="w-full" :class="$style.table" size="default">
        <el-table-column prop="ip" label="IP 地址" min-width="150" />
        <el-table-column prop="location" label="归属地" min-width="140">
          <template #default="{ row }">
            <el-tag size="small" :type="row.location === '内网' ? 'info' : ''">
              {{ row.location || '未知' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="reason" label="封禁原因" min-width="160">
          <template #default="{ row }">
            <el-tag size="small" type="danger">{{ reasonLabel(row.reason) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="banType" label="类型" width="80">
          <template #default="{ row }">
            {{ row.banType === 1 ? '永久' : '临时' }}
          </template>
        </el-table-column>
        <el-table-column prop="expireTime" label="过期时间" min-width="160">
          <template #default="{ row }">
            {{ row.expireTime || '永久有效' }}
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="封禁时间" min-width="160" />
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-popconfirm title="确定解除该 IP 封禁？" @confirm="handleUnban(row)">
              <template #reference>
                <el-button type="warning" size="small">解封</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import { banIp, getBannedIps, unblockIp } from '@/api/blog'

const bannedIps = ref([])
const loading = ref(false)
const submitting = ref(false)
const banForm = ref({
  ip: '',
  reason: 'security_policy',
  durationMinutes: 60
})

const reasonLabel = (reason) => {
  const map = {
    login_bruteforce: '登录暴力破解',
    unauthorized_access: '未授权访问',
    rate_limit_exceeded: '频率限制超限',
    security_policy: '安全策略'
  }
  return map[reason] || reason || '安全策略'
}

const handleBan = async () => {
  if (!banForm.value.ip) {
    ElMessage.warning('请输入 IP 地址')
    return
  }
  submitting.value = true
  try {
    const res = await banIp({
      ip: banForm.value.ip,
      reason: banForm.value.reason,
      durationMinutes: banForm.value.durationMinutes
    })
    if (res.success) {
      ElMessage.success(`已封禁 ${banForm.value.ip}`)
      banForm.value.ip = ''
      fetchBannedIps()
    }
  } catch (e) {
    ElMessage.error('封禁失败')
  } finally {
    submitting.value = false
  }
}

const fetchBannedIps = async () => {
  loading.value = true
  try {
    const res = await getBannedIps()
    if (res.success) {
      bannedIps.value = res.data || []
    }
  } catch (e) {
    ElMessage.error('获取封禁列表失败')
  } finally {
    loading.value = false
  }
}

const handleUnban = async (row) => {
  try {
    const res = await unblockIp(row.ip)
    if (res.success) {
      ElMessage.success(`已解除 ${row.ip} 的封禁`)
      fetchBannedIps()
    }
  } catch (e) {
    ElMessage.error('解封失败')
  }
}

onMounted(fetchBannedIps)
</script>

<style module>
.table :deep(.el-table__header th) {
  font-weight: 600;
}
</style>
