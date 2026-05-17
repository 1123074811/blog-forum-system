<template>
  <div>
    <h2 class="text-xl sm:text-2xl font-bold mb-4 dark:text-white">内容审核</h2>

    <div class="glass rounded-xl p-3 sm:p-4 mb-4">
      <div class="flex gap-2 flex-wrap">
        <el-button :type="statusFilter === 'pending' ? 'primary' : ''" size="small" @click="filterReports('pending')">
          待处理 ({{ pendingCount }})
        </el-button>
        <el-button :type="statusFilter === 'resolved' ? 'primary' : ''" size="small" @click="filterReports('resolved')">
          已处理 ({{ resolvedCount }})
        </el-button>
        <el-button :type="statusFilter === 'dismissed' ? 'primary' : ''" size="small" @click="filterReports('dismissed')">
          已驳回 ({{ dismissedCount }})
        </el-button>
      </div>
    </div>

    <div v-if="loading" class="text-center py-8 text-slate-400">加载中...</div>

    <div v-else-if="reports.length === 0" class="text-center py-8 text-slate-400">
      {{ statusFilter === 'pending' ? '暂无待处理举报' : '暂无记录' }}
    </div>

    <div v-else class="space-y-3">
      <div v-for="report in reports" :key="report.id" class="glass rounded-xl p-4">
        <div class="flex flex-col sm:flex-row sm:items-start sm:justify-between gap-3">
          <div class="flex-1 min-w-0">
            <div class="flex items-center gap-2 flex-wrap mb-2">
              <el-tag size="small" :type="report.targetType === 'article' ? '' : 'info'">
                {{ report.targetType === 'article' ? '文章' : '评论' }}
              </el-tag>
              <el-tag size="small" :type="getReasonType(report.reason)">{{ getReasonLabel(report.reason) }}</el-tag>
              <el-tag v-if="report.status !== 'pending'" size="small" :type="report.status === 'resolved' ? 'success' : 'warning'">
                {{ report.status === 'resolved' ? '已处理' : '已驳回' }}
              </el-tag>
            </div>
            <div class="text-sm text-slate-600 dark:text-slate-400 mb-1">
              举报人: <span class="font-medium">{{ report.reporterName }}</span>
              &nbsp;|&nbsp; 时间: {{ report.createdAt }}
            </div>
            <div v-if="report.targetTitle" class="text-sm font-medium mb-1">
              目标: {{ report.targetTitle }}
            </div>
            <div v-if="report.targetContent" class="text-xs text-slate-500 bg-slate-50 dark:bg-slate-800 rounded p-2 mb-2 line-clamp-3">
              {{ report.targetContent }}
            </div>
            <div v-if="report.description" class="text-sm text-slate-500">
              补充说明: {{ report.description }}
            </div>
            <div v-if="report.handlerName" class="text-xs text-slate-400 mt-2">
              处理人: {{ report.handlerName }} | 处理时间: {{ report.handledAt }}
              <span v-if="report.handleNote"> | 备注: {{ report.handleNote }}</span>
            </div>
          </div>

          <div v-if="report.status === 'pending'" class="flex sm:flex-col gap-2 shrink-0">
            <el-button size="small" type="danger" @click="handleAction(report, 'delete_content')">删除内容</el-button>
            <el-button size="small" type="warning" @click="handleAction(report, 'ban_user')">封禁用户</el-button>
            <el-button size="small" @click="handleAction(report, 'dismiss')">驳回</el-button>
          </div>
        </div>
      </div>
    </div>

    <div v-if="total > limit" class="flex justify-center mt-4">
      <el-pagination
        v-model:current-page="page"
        :page-size="limit"
        :total="total"
        layout="prev, pager, next"
        @current-change="loadReports"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getAdminReports, handleReport } from '@/api/blog'

const reports = ref([])
const loading = ref(false)
const statusFilter = ref('pending')
const page = ref(1)
const limit = 20
const total = ref(0)
const pendingCount = ref(0)
const resolvedCount = ref(0)
const dismissedCount = ref(0)

const reasonLabels = {
  spam: '垃圾广告', porn: '色情低俗', violence: '暴力血腥',
  illegal: '违法违规', harassment: '人身攻击', misinformation: '虚假信息',
  copyright: '侵权内容', other: '其他'
}

const getReasonLabel = (reason) => reasonLabels[reason] || reason
const getReasonType = (reason) => {
  if (['spam', 'porn', 'violence', 'illegal'].includes(reason)) return 'danger'
  if (['harassment', 'misinformation'].includes(reason)) return 'warning'
  return 'info'
}

const loadReports = async () => {
  loading.value = true
  try {
    const res = await getAdminReports({ page: page.value, limit, status: statusFilter.value })
    if (res.success) {
      reports.value = res.data.data || []
      total.value = res.data.total || 0
      pendingCount.value = res.data.pendingCount || 0
      resolvedCount.value = res.data.resolvedCount || 0
      dismissedCount.value = res.data.dismissedCount || 0
    }
  } finally {
    loading.value = false
  }
}

const filterReports = (status) => {
  statusFilter.value = status
  page.value = 1
  loadReports()
}

const handleAction = async (report, action) => {
  const note = action === 'dismiss' ? '经审核未发现违规内容' : ''
  try {
    const res = await handleReport(report.id, { action, note })
    if (res.success) {
      loadReports()
    }
  } catch (e) {
    console.error('处理举报失败:', e)
  }
}

onMounted(() => {
  loadReports()
})
</script>
