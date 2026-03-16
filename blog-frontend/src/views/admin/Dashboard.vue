<template>
  <div>
    <h2 class="text-xl sm:text-2xl font-bold mb-4 dark:text-white">仪表盘</h2>

    <div class="glass rounded-xl p-3 sm:p-4 mb-4">
      <div class="grid grid-cols-2 md:grid-cols-4 gap-2 sm:gap-3">
        <el-select v-model="selectedRange" class="w-full" @change="handleFilterChange">
          <el-option label="近7天" value="7d" />
          <el-option label="近30天" value="30d" />
          <el-option label="近90天" value="90d" />
        </el-select>
        <el-select v-model="selectedGranularity" class="w-full" @change="handleFilterChange">
          <el-option label="按日" value="day" />
          <el-option label="按月" value="month" />
          <el-option label="按年" value="year" />
        </el-select>
        <div class="col-span-2 text-xs sm:text-sm text-slate-500 flex items-center justify-end">
          在线用户每 15 秒自动刷新
        </div>
      </div>
    </div>

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
        <h3 class="text-base sm:text-lg font-semibold mb-3 dark:text-white">内容趋势（文章/评论）</h3>
        <div ref="trendChartRef" :style="{ height: chartHeight + 'px' }"></div>
      </div>
      <div class="glass rounded-xl p-4">
        <h3 class="text-base sm:text-lg font-semibold mb-3 dark:text-white">文章状态分布</h3>
        <div ref="statusChartRef" :style="{ height: chartHeight + 'px' }"></div>
      </div>
      <div class="glass rounded-xl p-4">
        <h3 class="text-base sm:text-lg font-semibold mb-3 dark:text-white">浏览量排行</h3>
        <div ref="viewChartRef" :style="{ height: chartHeight + 'px' }"></div>
      </div>
      <div class="glass rounded-xl p-4">
        <h3 class="text-base sm:text-lg font-semibold mb-3 dark:text-white">活跃作者（Top 5）</h3>
        <div ref="authorChartRef" :style="{ height: chartHeight + 'px' }"></div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick, onUnmounted, computed } from 'vue'
import { getStatistics } from '@/api/blog'
import { useIsMobile } from '@/composables/useIsMobile'

// echarts 按需异步加载
let echarts = null
const loadEcharts = async () => {
  if (echarts) return echarts
  const core = await import('echarts/core')
  const { BarChart, LineChart, PieChart } = await import('echarts/charts')
  const { TitleComponent, TooltipComponent, GridComponent, LegendComponent } = await import('echarts/components')
  const { CanvasRenderer } = await import('echarts/renderers')
  core.use([TitleComponent, TooltipComponent, GridComponent, LegendComponent, BarChart, LineChart, PieChart, CanvasRenderer])
  echarts = core
  return echarts
}

const stats = ref({
  totalArticles: 0,
  totalUsers: 0,
  totalOnlineUsers: 0,
  totalComments: 0,
  viewRanking: [],
  trend: { labels: [], articleCounts: [], commentCounts: [] },
  articleStatusDistribution: [],
  activeAuthors: []
})
const selectedRange = ref('30d')
const selectedGranularity = ref('day')
const trendChartRef = ref(null)
const statusChartRef = ref(null)
const viewChartRef = ref(null)
const authorChartRef = ref(null)
const { isMobile } = useIsMobile()
let trendChart = null
let statusChart = null
let viewChart = null
let authorChart = null
let onlineTimer = null

const chartHeight = computed(() => (isMobile.value ? 240 : 300))

const statCards = computed(() => [
  { key: 'a', label: '文章总数', value: stats.value.totalArticles, color: 'text-primary-500' },
  { key: 'u', label: '用户总数', value: stats.value.totalUsers, color: 'text-sky-500' },
  { key: 'c', label: '在线用户', value: stats.value.totalOnlineUsers, color: 'text-emerald-500' },
  { key: 'm', label: '评论总数', value: stats.value.totalComments, color: 'text-purple-500' }
])

const quickEntries = [
  { path: '/admin/articles', label: '文章' },
  { path: '/admin/comments', label: '评论' },
  { path: '/admin/users', label: '用户' },
  { path: '/admin/site-info', label: '设置' }
]

const formatTrendLabel = (label) => {
  if (selectedGranularity.value === 'day' && typeof label === 'string' && label.length >= 10) {
    return label.substring(5)
  }
  return label
}

const initTrendChart = async () => {
  if (!trendChartRef.value) return
  const ec = await loadEcharts()
  trendChart?.dispose()
  trendChart = ec.init(trendChartRef.value)
  trendChart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { top: 0 },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: {
      type: 'category',
      data: (stats.value.trend?.labels || []).map(formatTrendLabel),
      axisLabel: { fontSize: isMobile.value ? 10 : 12, rotate: isMobile.value ? 30 : 0 }
    },
    yAxis: { type: 'value' },
    series: [
      { name: '文章', type: 'line', smooth: true, data: stats.value.trend?.articleCounts || [], itemStyle: { color: '#0ea5e9' } },
      { name: '评论', type: 'line', smooth: true, data: stats.value.trend?.commentCounts || [], itemStyle: { color: '#a855f7' } }
    ]
  })
}

const initStatusChart = async () => {
  if (!statusChartRef.value) return
  const ec = await loadEcharts()
  statusChart?.dispose()
  statusChart = ec.init(statusChartRef.value)
  statusChart.setOption({
    tooltip: { trigger: 'item' },
    legend: { bottom: 0 },
    series: [{
      type: 'pie',
      radius: ['45%', '70%'],
      data: stats.value.articleStatusDistribution || [],
      itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 }
    }]
  })
}

const initViewChart = async () => {
  if (!viewChartRef.value) return
  const ec = await loadEcharts()
  viewChart?.dispose()
  viewChart = ec.init(viewChartRef.value)
  viewChart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: {
      type: 'category',
      data: (stats.value.viewRanking || []).map(a => {
        const title = a.title || ''
        return title.length > 8 ? title.substring(0, 8) + '...' : title
      }),
      axisLabel: { rotate: isMobile.value ? 35 : 45, interval: 0, fontSize: isMobile.value ? 10 : 12 }
    },
    yAxis: { type: 'value' },
    series: [{
      data: (stats.value.viewRanking || []).map(a => a.viewCount || 0),
      type: 'bar',
      itemStyle: { color: '#0ea5e9', borderRadius: [4, 4, 0, 0] },
      barMaxWidth: isMobile.value ? 32 : 50
    }]
  })
}

const initAuthorChart = async () => {
  if (!authorChartRef.value) return
  const ec = await loadEcharts()
  authorChart?.dispose()
  authorChart = ec.init(authorChartRef.value)
  const list = stats.value.activeAuthors || []
  authorChart.setOption({
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: '5%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: { type: 'value' },
    yAxis: { type: 'category', data: list.map(i => i.name), axisLabel: { fontSize: isMobile.value ? 10 : 12 } },
    series: [{
      type: 'bar',
      data: list.map(i => i.count),
      itemStyle: { color: '#22c55e', borderRadius: [0, 4, 4, 0] },
      barMaxWidth: isMobile.value ? 24 : 30
    }]
  })
}

const initAllCharts = () => {
  initTrendChart()
  initStatusChart()
  initViewChart()
  initAuthorChart()
}

const fetchStats = async () => {
  const res = await getStatistics({ range: selectedRange.value, granularity: selectedGranularity.value })
  if (res.success) {
    stats.value = {
      totalArticles: res.data.totalArticles || 0,
      totalUsers: res.data.totalUsers || 0,
      totalOnlineUsers: res.data.totalOnlineUsers || 0,
      totalComments: res.data.totalComments || 0,
      viewRanking: res.data.viewRanking || [],
      trend: res.data.trend || { labels: [], articleCounts: [], commentCounts: [] },
      articleStatusDistribution: res.data.articleStatusDistribution || [],
      activeAuthors: res.data.activeAuthors || []
    }
    await nextTick()
    setTimeout(initAllCharts, 80)
  }
}

const refreshOnlineUsers = async () => {
  const res = await getStatistics({ range: selectedRange.value, granularity: selectedGranularity.value })
  if (res.success) stats.value.totalOnlineUsers = res.data.totalOnlineUsers || 0
}

const handleFilterChange = () => {
  fetchStats()
}

const handleResize = () => {
  trendChart?.resize()
  statusChart?.resize()
  viewChart?.resize()
  authorChart?.resize()
}

onMounted(async () => {
  window.addEventListener('resize', handleResize)
  await fetchStats()
  onlineTimer = setInterval(refreshOnlineUsers, 15000)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  if (onlineTimer) clearInterval(onlineTimer)
  trendChart?.dispose()
  statusChart?.dispose()
  viewChart?.dispose()
  authorChart?.dispose()
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
