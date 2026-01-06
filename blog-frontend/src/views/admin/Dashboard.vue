<template>
  <div>
    <h2 class="text-2xl font-bold mb-6 dark:text-white">仪表板</h2>

    <!-- 统计卡片 -->
    <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4 mb-6">
      <div class="glass rounded-xl p-6">
        <div class="text-3xl font-bold text-primary-500">{{ stats.totalArticles }}</div>
        <div class="text-gray-500">文章总数</div>
      </div>
      <div class="glass rounded-xl p-6">
        <div class="text-3xl font-bold text-green-500">{{ stats.totalUsers }}</div>
        <div class="text-gray-500">用户总数</div>
      </div>
      <div class="glass rounded-xl p-6">
        <div class="text-3xl font-bold text-orange-500">{{ stats.totalCategories }}</div>
        <div class="text-gray-500">分类数量</div>
      </div>
      <div class="glass rounded-xl p-6">
        <div class="text-3xl font-bold text-purple-500">{{ stats.totalComments }}</div>
        <div class="text-gray-500">评论总数</div>
      </div>
    </div>

    <!-- 图表 -->
    <div class="grid grid-cols-1 lg:grid-cols-2 gap-6">
      <div class="glass rounded-xl p-6">
        <h3 class="text-lg font-semibold mb-4 dark:text-white">浏览量排行</h3>
        <div ref="viewChartRef" style="height: 300px"></div>
      </div>
      <div class="glass rounded-xl p-6">
        <h3 class="text-lg font-semibold mb-4 dark:text-white">热门文章</h3>
        <div class="space-y-3">
          <div v-for="(article, index) in stats.viewRanking" :key="article.id" class="flex items-center gap-3">
            <span class="w-6 h-6 rounded-full bg-primary-500 text-white text-sm flex items-center justify-center">{{ index + 1 }}</span>
            <span class="flex-1 truncate dark:text-gray-300">{{ article.title }}</span>
            <span class="text-gray-500">{{ article.viewCount }} 浏览</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import { getStatistics } from '@/api/blog'
import * as echarts from 'echarts'

const stats = ref({ totalArticles: 0, totalUsers: 0, totalCategories: 0, totalComments: 0, viewRanking: [] })
const viewChartRef = ref(null)

const initChart = () => {
  if (!viewChartRef.value || !stats.value.viewRanking?.length) return

  const chart = echarts.init(viewChartRef.value)
  chart.setOption({
    tooltip: { trigger: 'axis' },
    xAxis: {
      type: 'category',
      data: stats.value.viewRanking.map((_, i) => `文章${i + 1}`),
      axisLabel: { rotate: 45 }
    },
    yAxis: { type: 'value' },
    series: [{
      data: stats.value.viewRanking.map(a => a.viewCount),
      type: 'bar',
      itemStyle: { color: '#0ea5e9', borderRadius: [4, 4, 0, 0] }
    }]
  })
}

onMounted(async () => {
  const res = await getStatistics()
  if (res.success) {
    stats.value = res.data
    await nextTick()
    initChart()
  }
})
</script>
