<template>
  <div>
    <h2 class="text-xl sm:text-2xl font-bold mb-4 dark:text-white">仪表盘</h2>

    <div class="grid grid-cols-2 lg:grid-cols-4 gap-3 mb-4">
      <div class="glass rounded-xl p-4" v-for="item in statCards" :key="item.key">
        <div class="text-2xl sm:text-3xl font-bold" :class="item.color">{{ item.value }}</div>
        <div class="text-gray-500 text-sm mt-1">{{ item.label }}</div>
      </div>
    </div>

    <div v-if="isMobile" class="glass rounded-xl p-3 mb-4">
      <div class="text-sm font-semibold text-slate-700 mb-2">快捷入口</div>
      <div class="grid grid-cols-4 gap-2">
        <router-link v-for="entry in quickEntries" :key="entry.path" :to="entry.path" class="quick-entry">
          {{ entry.label }}
        </router-link>
      </div>
    </div>

    <div class="grid grid-cols-1 lg:grid-cols-2 gap-4">
      <div class="glass rounded-xl p-4">
        <h3 class="text-base sm:text-lg font-semibold mb-3 dark:text-white">浏览量排行</h3>
        <div ref="viewChartRef" :style="{ height: chartHeight + 'px' }"></div>
      </div>
      <div class="glass rounded-xl p-4">
        <h3 class="text-base sm:text-lg font-semibold mb-3 dark:text-white">热门文章</h3>
        <div class="space-y-2">
          <div v-for="(article, index) in stats.viewRanking" :key="article.id" class="flex items-center gap-3">
            <span class="w-6 h-6 rounded-full bg-primary-500 text-white text-xs flex items-center justify-center">{{ index + 1 }}</span>
            <span class="flex-1 truncate dark:text-gray-300 text-sm">{{ article.title }}</span>
            <span class="text-gray-500 text-xs">{{ article.viewCount }} 浏览</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick, onUnmounted, computed } from 'vue'
import { getStatistics } from '@/api/blog'
import { useIsMobile } from '@/composables/useIsMobile'
import * as echarts from 'echarts/core'
import { BarChart } from 'echarts/charts'
import { TitleComponent, TooltipComponent, GridComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'

echarts.use([TitleComponent, TooltipComponent, GridComponent, BarChart, CanvasRenderer])

const stats = ref({ totalArticles: 0, totalUsers: 0, totalCategories: 0, totalComments: 0, viewRanking: [] })
const viewChartRef = ref(null)
const { isMobile } = useIsMobile()
let chartInstance = null

const chartHeight = computed(() => (isMobile.value ? 240 : 300))

const statCards = computed(() => [
  { key: 'a', label: '文章总数', value: stats.value.totalArticles, color: 'text-primary-500' },
  { key: 'u', label: '用户总数', value: stats.value.totalUsers, color: 'text-green-500' },
  { key: 'c', label: '分类数量', value: stats.value.totalCategories, color: 'text-orange-500' },
  { key: 'm', label: '评论总数', value: stats.value.totalComments, color: 'text-purple-500' }
])

const quickEntries = [
  { path: '/admin/articles', label: '文章' },
  { path: '/admin/comments', label: '评论' },
  { path: '/admin/users', label: '用户' },
  { path: '/admin/site-info', label: '设置' }
]

const initChart = () => {
  if (!viewChartRef.value || !stats.value.viewRanking?.length) return

  if (chartInstance) chartInstance.dispose()

  chartInstance = echarts.init(viewChartRef.value)
  chartInstance.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: {
      type: 'category',
      data: stats.value.viewRanking.map(a => {
        const title = a.title || ''
        return title.length > 8 ? title.substring(0, 8) + '...' : title
      }),
      axisLabel: { rotate: isMobile.value ? 35 : 45, interval: 0, fontSize: isMobile.value ? 10 : 12 }
    },
    yAxis: { type: 'value' },
    series: [{
      data: stats.value.viewRanking.map(a => a.viewCount),
      type: 'bar',
      itemStyle: { color: '#0ea5e9', borderRadius: [4, 4, 0, 0] },
      barMaxWidth: isMobile.value ? 32 : 50
    }]
  })
}

const handleResize = () => {
  chartInstance?.resize()
}

onMounted(async () => {
  window.addEventListener('resize', handleResize)
  const res = await getStatistics()
  if (res.success) {
    stats.value = res.data
    await nextTick()
    setTimeout(initChart, 100)
  }
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  chartInstance?.dispose()
})
</script>

<style scoped>
.quick-entry {
  height: 36px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  color: #334155;
  background: rgba(14, 165, 233, 0.1);
}
</style>
