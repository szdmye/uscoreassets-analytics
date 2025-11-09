<!-- src/components/layout/MainLayout.vue -->
<template>
  <div class="app-container">
    <!-- 顶部导航 -->
    <StockHeader />
    
    <div class="app-content">
      <!-- 侧边栏 -->
      <StockSidebar />
      
      <!-- 页面内容（路由出口） -->
      <main class="content-wrapper">
        <router-view /> <!-- 子路由页面会在这里渲染 -->
      </main>
    </div>
  </div>
</template>

<script setup>
// 导入Header和Sidebar组件
import StockHeader from '@/components/StockHeader.vue'
import StockSidebar from '@/components/StockSidebar.vue'
</script>

<style scoped>
/* 全局布局样式 */
.app-container {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  background-color: #f5f7fa;
}

.app-content {
  display: flex;
  flex: 1; /* 占满剩余高度 */
  overflow: hidden;
}

.content-wrapper {
  flex: 1; /* 内容区占满剩余宽度 */
  padding: 20px;
  overflow-y: auto; /* 内容溢出时滚动 */
}
</style>