<template>
  <div class="discover-page">
    <!-- 页面标题 -->
    <div class="page-header">
      <h1 class="page-title">发现</h1>
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
          <h3 class="tool-title">ER图工具</h3>
          <p class="tool-description">
            快速生成数据库实体关系图，支持SQL文件导入，自动识别表结构和字段关系，提供专业的ER图绘制功能。
          </p>
        </div>

        <!-- 敬请期待 -->
        <div class="tool-card coming-soon-card">
          <div class="tool-icon coming-soon-icon">
            <el-icon size="32"><ChatDotRound /></el-icon>
          </div>
          <h3 class="tool-title">更多功能敬请期待</h3>
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
import { Grid, ChatDotRound } from '@element-plus/icons-vue'

const router = useRouter()

function goToERDiagram() {
  router.push('/er-diagram')
}
</script>

<style scoped>
.discover-page {
  min-height: 100vh;
  background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);
  padding: 40px 20px;
}

.page-header {
  text-align: center;
  margin-bottom: 50px;
}

.page-title {
  font-size: 48px;
  font-weight: 700;
  color: #2c3e50;
  margin-bottom: 16px;
  text-shadow: 2px 2px 4px rgba(0, 0, 0, 0.1);
}

.page-subtitle {
  font-size: 18px;
  color: #7f8c8d;
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
  border-radius: 16px;
  padding: 32px 24px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  cursor: pointer;
  position: relative;
  overflow: hidden;
  border: 1px solid rgba(255, 255, 255, 0.2);
}

.tool-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 4px;
  background: linear-gradient(90deg, #667eea 0%, #764ba2 100%);
  transform: scaleX(0);
  transition: transform 0.3s ease;
}

.tool-card:hover {
  transform: translateY(-8px);
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.15);
}

.tool-card:hover::before {
  transform: scaleX(1);
}

.tool-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 64px;
  height: 64px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 16px;
  margin-bottom: 20px;
  box-shadow: 0 4px 16px rgba(102, 126, 234, 0.3);
}

.tool-icon .el-icon {
  color: white !important;
}

.tool-title {
  font-size: 20px;
  font-weight: 600;
  color: #2c3e50;
  margin-bottom: 12px;
  line-height: 1.3;
}

.tool-description {
  font-size: 14px;
  color: #7f8c8d;
  line-height: 1.6;
  margin: 0;
}

.coming-soon-card {
  background: linear-gradient(135deg, #f8f9fa 0%, #e9ecef 100%);
  border: 2px dashed #dee2e6;
  cursor: default;
}

.coming-soon-card:hover {
  transform: none;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
}

.coming-soon-card::before {
  display: none;
}

.coming-soon-icon {
  background: linear-gradient(135deg, #adb5bd 0%, #868e96 100%);
  box-shadow: 0 4px 16px rgba(134, 142, 150, 0.3);
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
    padding: 20px 15px;
  }

  .page-title {
    font-size: 36px;
  }

  .page-subtitle {
    font-size: 16px;
  }

  .tools-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 16px;
  }

  .tool-card {
    padding: 24px 20px;
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
    padding: 20px 16px;
  }
}
</style>
