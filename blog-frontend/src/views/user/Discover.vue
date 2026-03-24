<template>
  <div class="discover-page">
    <!-- 页面标题 -->
    <div class="page-header vintage-panel jp-discover-header">
      <h1 class="page-title">
        <span class="stamp">见</span>
        发现
      </h1>
      <p class="page-subtitle">探索实用工具，提升工作效率</p>
    </div>

    <!-- 工具网格 -->
    <div class="tools-container">
      <div class="tools-grid">
        <!-- ER图工具 -->
        <div class="tool-card" @click="goToERDiagram">
          <div class="tool-icon">
            <el-icon size="32" color="#1976d2"><Grid /></el-icon>
          </div>
          <h3 class="tool-title"><span class="stamp">图</span>ER图工具</h3>
          <p class="tool-description">
            快速生成数据库实体关系图，支持SQL文件导入，自动识别表结构和字段关系，提供专业的ER图绘制功能。
          </p>
        </div>

        <!-- 人生模拟器 -->
        <div class="tool-card life-simulator-card" @click="goToLifeSimulator">
          <div class="tool-icon life-simulator-icon">
            <el-icon size="32"><Sunrise /></el-icon>
          </div>
          <h3 class="tool-title"><span class="stamp">旅</span>人生模拟器</h3>
          <p class="tool-description">
            体验随机人生，分配初始属性，经历各种人生事件，看看你能活到多少岁。
          </p>
        </div>

        <!-- 背书神器 -->
        <div class="tool-card recitation-card" @click="goToRecitation">
          <div class="tool-icon recitation-icon">
            <el-icon size="32"><Reading /></el-icon>
          </div>
          <h3 class="tool-title"><span class="stamp">习</span>背书神器</h3>
          <p class="tool-description">
            导入背诵内容进行打字练习，支持显示原文、隐藏提示、拼音提示三种模式，帮助高效记忆。
          </p>
        </div>

        <!-- 番茄时钟 -->
        <div class="tool-card pomodoro-card" @click="goToPomodoro">
          <div class="tool-icon pomodoro-icon">
            <el-icon size="32"><Timer /></el-icon>
          </div>
          <h3 class="tool-title"><span class="stamp">钟</span>番茄时钟</h3>
          <p class="tool-description">
            专注工作25分钟，休息5分钟。使用番茄工作法提升专注力和工作效率。
          </p>
        </div>

        <!-- 敬请期待 -->
        <div class="tool-card coming-soon-card">
          <div class="tool-icon coming-soon-icon">
            <el-icon size="32"><ChatDotRound /></el-icon>
          </div>
          <h3 class="tool-title"><span class="stamp">待</span>更多功能敬请期待</h3>
          <p class="tool-description">
            如果你有什么好的想法，可以和管理员联系。
          </p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { Grid, ChatDotRound, Sunrise, Reading, Timer } from '@element-plus/icons-vue'

const router = useRouter()

function goToERDiagram() {
  router.push('/er-diagram')
}

function goToLifeSimulator() {
  router.push('/life-simulator')
}

function goToRecitation() {
  router.push('/recitation')
}

function goToPomodoro() {
  router.push('/pomodoro')
}
</script>

<style scoped>
.discover-page {
  padding: 32px 0 24px;
  box-sizing: border-box;
}

.page-header {
  text-align: center;
  margin-bottom: 28px;
  max-width: 800px;
  margin-left: auto;
  margin-right: auto;
}

.page-title {
  font-size: 42px;
  font-weight: 800;
  color: var(--ink);
  margin-bottom: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.page-subtitle {
  font-size: 18px;
  color: var(--text-secondary);
  margin: 0;
}

.tools-container {
  max-width: 1400px;
  margin: 0 auto;
}

.tools-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 24px;
  margin-bottom: 40px;
}

.tool-card {
  background: white;
  border: var(--border-sketch);
  padding: 24px 20px;
  cursor: pointer;
  position: relative;
  overflow: hidden;
  border-radius: 255px 15px 225px 15px / 15px 225px 15px 255px;
  transition: transform 0.4s cubic-bezier(0.175, 0.885, 0.32, 1.275), box-shadow 0.4s;
}

.dark .tool-card {
  background: #222;
  border-color: #555;
}

.tool-card::before {
  display: none;
}

.tool-card:hover {
  transform: scale(1.02) rotate(1deg);
  box-shadow: 15px 15px 0px var(--subtle-blue);
}

.dark .tool-card:hover {
  box-shadow: 15px 15px 0px #111;
}

.tool-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 64px;
  height: 64px;
  background: var(--subtle-blue);
  border-radius: 18px;
  margin-bottom: 14px;
  border: 2px solid var(--ink);
  box-shadow: 4px 4px 0px var(--subtle-blue);
}

.tool-icon .el-icon {
  color: var(--accent) !important;
}

.tool-title {
  font-size: 20px;
  font-weight: 800;
  color: var(--ink);
  margin-bottom: 10px;
  line-height: 1.25;
}

.tool-description {
  font-size: 14px;
  color: var(--text-secondary);
  line-height: 1.6;
  margin: 0;
}

.coming-soon-card {
  background: rgba(255, 255, 255, 0.6);
  border: 2px dashed rgba(0, 0, 0, 0.25);
  cursor: default;
}

.coming-soon-card:hover {
  transform: none;
  box-shadow: none;
}

.coming-soon-card::before {
  display: none;
}

.coming-soon-icon {
  background: #eef2f7;
  box-shadow: 4px 4px 0px rgba(0, 0, 0, 0.04);
}

.life-simulator-card::before {
  background: linear-gradient(90deg, #1a1a2e 0%, #00ff88 100%);
}

.life-simulator-icon {
  background: linear-gradient(135deg, #1a1a2e 0%, #00ff88 100%);
  box-shadow: 0 4px 16px rgba(0, 255, 136, 0.3);
}

.recitation-card::before {
  background: linear-gradient(90deg, #ff6b6b 0%, #feca57 100%);
}

.recitation-icon {
  background: linear-gradient(135deg, #ff6b6b 0%, #feca57 100%);
  box-shadow: 0 4px 16px rgba(255, 107, 107, 0.3);
}

.pomodoro-card::before {
  background: linear-gradient(90deg, #f093fb 0%, #f5576c 100%);
}

.pomodoro-icon {
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
  box-shadow: 0 4px 16px rgba(240, 147, 251, 0.3);
}

/* 响应式设计 */
@media (max-width: 1200px) {
  .tools-grid {
    grid-template-columns: repeat(4, 1fr);
    gap: 20px;
  }
}

@media (max-width: 992px) {
  .tools-grid {
    grid-template-columns: repeat(3, 1fr);
  }
}

@media (max-width: 768px) {
  .discover-page {
    padding: 8px 0 20px;
  }

  .page-header {
    margin-bottom: 18px;
  }

  .page-title {
    font-size: 32px;
  }

  .page-subtitle {
    font-size: 16px;
  }

  .tools-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 16px;
  }

  .tool-card {
    padding: 20px 16px;
  }

  .tool-card:hover {
    transform: none;
    box-shadow: none;
  }

  .tool-icon {
    width: 56px;
    height: 56px;
  }

  .tool-title {
    font-size: 18px;
  }

  .tool-description {
    font-size: 13px;
  }
}

@media (max-width: 480px) {
  .page-title {
    font-size: 28px;
  }

  .tools-grid {
    grid-template-columns: 1fr;
  }

  .tool-card {
    padding: 18px 14px;
  }
}
</style>
