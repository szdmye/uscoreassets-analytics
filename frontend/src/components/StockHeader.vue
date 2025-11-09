<template>
  <header class="header">
    <div class="header-container">
      <!-- 左侧：菜单按钮 -->
      <div class="header-left">
        <div class="logo" @click="$router.push('/')">
          <i class="bi bi-graph-up"></i>
          <span class="logo-text">StockInsight</span>
        </div>
        <button class="menu-toggle" @click="toggleSidebar" v-if="isMobile">
          <i class="bi bi-list"></i>
        </button>
      </div>
      
      <!-- 中间：页面标题 -->
      <h1 class="page-title">
        {{ $route.meta.title || '股票分析系统' }}
      </h1>
      
      <!-- 右侧：功能区 -->
      <div class="header-actions">
        <!-- 数据加载状态 -->
        <div class="loading-status" v-if="isLoading">
          <span class="spinner"></span>
          <span class="loading-text">加载中...</span>
        </div>
        
        <!-- 错误提示 -->
        <div class="error-alert" v-if="errorMessage">
          <i class="bi bi-exclamation-circle"></i>
          <span class="error-text">{{ errorMessage }}</span>
          <button class="close-btn" @click="clearError">×</button>
        </div>
        
      </div>
    </div>
  </header>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useStockStore } from '@/stores/stockStore.js'

// 股票仓库
const stockStore = useStockStore()
const isLoading = computed(() => stockStore.isLoading)
const errorMessage = computed(() => stockStore.error)

const isMobile = ref(false)
const isSidebarOpen = ref(true) 

// 监听窗口大小
const checkScreenSize = () => {
  isMobile.value = window.innerWidth < 768
  if (isMobile.value) {
    isSidebarOpen.value = false 
  } else {
    isSidebarOpen.value = true 
  }
}

onMounted(() => {
  checkScreenSize()
  window.addEventListener('resize', checkScreenSize)
})

const toggleSidebar = () => {
  isSidebarOpen.value = !isSidebarOpen.value
  stockStore.setSidebarVisible(isSidebarOpen.value) 
}

// 清除错误信息
const clearError = () => {
  stockStore.error = ''
}
</script>

<style scoped>
/* 顶部导航样式 */
.header {
  background-color: #fff;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
  position: sticky;
  top: 0;
  z-index: 100;
}

.header-container {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  height: 60px;
}

/* 左侧区域 */
.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-shrink: 0;
}

/* Logo样式 */
.logo {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 6px 12px;
  border-radius: 6px;
  transition: all 0.2s ease;
}

.logo:hover {
  background-color: #f8fafc;
}

.logo i {
  font-size: 20px;
  color: #2563eb;
}

.logo-text {
  font-size: 16px;
  font-weight: 700;
  color: #1f2937;
  letter-spacing: -0.5px;
}

.menu-toggle {
  background: none;
  border: none;
  color: #1f2937;
  font-size: 20px;
  cursor: pointer;
  padding: 6px;
  border-radius: 4px;
  transition: background-color 0.2s ease;
}

.menu-toggle:hover {
  background-color: #f8fafc;
}

/* 页面标题 */
.page-title {
  font-size: 18px;
  font-weight: 600;
  color: #1f2937;
  margin: 0;
}

/* 右侧功能区 */
.header-actions {
  display: flex;
  align-items: center;
  gap: 16px;
}

/* 加载状态 */
.loading-status {
  display: flex;
  align-items: center;
  color: #2563eb;
  font-size: 14px;
}

.spinner {
  display: inline-block;
  width: 16px;
  height: 16px;
  border: 2px solid #2563eb;
  border-top-color: transparent;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-right: 6px;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.loading-text {
  white-space: nowrap;
}

/* 错误提示 */
.error-alert {
  display: flex;
  align-items: center;
  background-color: #fee2e2;
  border-radius: 4px;
  padding: 4px 12px;
  color: #dc2626;
  font-size: 14px;
}

.error-alert i {
  margin-right: 6px;
}

.close-btn {
  background: none;
  border: none; 
  color: #dc2626;
  font-size: 14px;
  cursor: pointer;
}

/* 上传按钮 */
.upload-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  background-color: #2563eb;
  color: white;
  border: none;
  border-radius: 4px;
  padding: 6px 12px;
  cursor: pointer;
  font-size: 14px;
}

.upload-btn:hover {
  background-color: #1d4ed8;
}

/* 用户信息 */
.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
}

.avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
}

.username {
  font-size: 14px;
  font-weight: 600;
  color: #1f2937;
}

.sidebar {
  position: fixed;
  top: 0;
  left: 0;
  width: 240px;
  height: 100%;
  background-color: #fff;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
  z-index: 99;
  transform: translateX(-240px);
  transition: transform 0.3s ease-in-out;
}

.sidebar.open {
  transform: translateX(0);
}

.sidebar-content {
  padding: 20px;
}

.sidebar-item {
  margin-bottom: 16px;
}

.sidebar-item:last-child {
  margin-bottom: 0;
}

.sidebar-link {
  display: block;
  font-size: 16px;
  font-weight: 500;
  color: #1f2937;
  padding: 8px 0;
  transition: color 0.2s ease-in-out;
}

.sidebar-link:hover {
  color: #2563eb;
}

.sidebar-link.active {
  color: #2563eb;
  font-weight: 600;
}

/* 响应式 */
@media (max-width: 767px) {
  .header-container {
    padding: 0 16px;
  }
  
  .menu-toggle {
    display: block;
  }
  
  .logo-text {
    display: none;
  }
  
  .page-title {
    font-size: 16px;
  }
  
  .header-actions {
    display: none;
  }
  
  .user-info {
    display: none;
  }

  .sidebar {  
    position: static;
    transform: translateX(0);
    width: 100%;
    height: auto;
    box-shadow: none;
    padding: 16px;
  }
}
</style>