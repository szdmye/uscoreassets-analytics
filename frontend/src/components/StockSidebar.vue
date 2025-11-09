<template>
  <div class="layout-container" :class="{ 'sidebar-collapsed': isCollapsed }">
    <!-- 侧边栏 -->
    <aside 
      class="sidebar" 
      :style="{ width: isCollapsed ? '60px' : sidebarWidth + 'px' }"
      @mousedown="startResize"
      :class="{ 'mobile-open': mobileSidebarOpen }"
    >
      <!-- 折叠按钮 -->
      <div class="sidebar-collapse-btn" @click="toggleCollapse" v-if="!isMobile">
        <i :class="isCollapsed ? 'bi bi-chevron-right' : 'bi bi-chevron-left'"></i>
      </div>

      <!-- 正常状态下的完整侧边栏内容 -->
      <div class="sidebar-content" v-show="!isCollapsed">
        <!-- 侧边栏标题 -->
        <div class="sidebar-header">
          <h3 class="sidebar-title">股票分析系统</h3>
        </div>
        
        <!-- 导航菜单 -->
        <nav class="sidebar-menu">
          <router-link 
            to="/" 
            class="menu-item"
            :class="{ active: $route.path === '/' }"
          >
            <i class="bi bi-grid-3x3-gap"></i>
            <span>市场概况</span>
          </router-link>
          
          <router-link 
            to="/stock-analysis" 
            class="menu-item"
            :class="{ active: $route.path === '/stock-analysis' }"
          >
            <i class="bi bi-bar-chart-line"></i>
            <span>个股分析</span>
          </router-link>

          <router-link 
            to="/stock-prediction" 
            class="menu-item"
            :class="{ active: $route.path === '/stock-prediction' }"
          >
            <i class="bi bi-graph-up-arrow"></i>
            <span>价格预测</span>
          </router-link>
          
          <router-link 
            to="/stock-news" 
            class="menu-item"
            :class="{ active: $route.path === '/stock-news' }"
          >
            <i class="bi bi-newspaper"></i>
            <span>个股资讯</span>
          </router-link>

          <router-link 
            to="/data-list" 
            class="menu-item"
            :class="{ active: $route.path === '/data-list' }"
          >
            <i class="bi bi-table"></i>
            <span>数据列表</span>
          </router-link>
        </nav>
        
        <!-- AI助手入口 -->
        <div class="ai-assistant-section mt-4">
          <h4 class="section-title">AI助手</h4>
          <button 
            class="ai-assistant-btn" 
            @click="toggleAIChat"
          >
            <i class="bi bi-robot"></i>
            <span>智能问答</span>
          </button>
        </div>
        
        <!-- 股票分类筛选 -->
        <div class="sidebar-filter mt-4">
          <h4 class="section-title">股票分类</h4>
          <div class="filter-tags">
            <router-link
              v-for="category in stockCategories"
              :key="category.value"
              :to="`/category/${category.value}`"
              class="tag-btn"
              :class="{ active: $route.params.category === category.value }"
            >
              {{ category.label }}
            </router-link>
          </div>
        </div>
        
        <!-- 涨幅排行 -->
        <div class="top-gainers mt-4">
          <h4 class="section-title">涨幅排行</h4>
          <div v-if="isLoading">加载中...</div>
          <div v-else-if="error">{{ error }}</div>
          <div v-else class="gainer-list">
            <div class="gainer-item" v-for="(stock, index) in topGainers" :key="index">
              <span class="gainer-name">{{ stock.name }}</span>
              <span class="gainer-change" :class="stock.changePercent >= 0 ? 'positive' : 'negative'">
                {{ stock.changePercent >= 0 ? '+' : '' }}{{ stock.changePercent.toFixed(2) }}%
              </span>
            </div>
          </div>
        </div>
        
        <!-- 交易量排行 -->
        <div class="top-volume mt-4">
          <h4 class="section-title">交易量排行</h4>
          <div v-if="isLoading">加载中...</div>
          <div v-else-if="error">{{ error }}</div>
          <div v-else class="volume-list">
            <div class="volume-item" v-for="(stock, index) in topVolume" :key="index">
              <span class="volume-name">{{ stock.name }}</span>
              <span class="volume-value">{{ formatVolume(stock.volume) }}</span>
            </div>
          </div>
        </div>
        
        <!-- 数据更新时间 -->
        <div class="sidebar-footer" v-if="lastUpdateTime">
          <p>最后更新：{{ lastUpdateTime.slice(0,10) }}</p>
        </div>
      </div>

      <!-- 折叠状态下的图标菜单 -->
      <nav class="sidebar-menu-collapsed" v-show="isCollapsed">
        <router-link 
          to="/" 
          class="menu-item-icon" 
          title="市场概况"
          :class="{ active: $route.path === '/' }"
        >
          <i class="bi bi-grid-3x3-gap"></i>
        </router-link>
        
        <router-link 
          to="/stock-analysis" 
          class="menu-item-icon"
          title="个股分析"
          :class="{ active: $route.path === '/stock-analysis' }"
        >
          <i class="bi bi-bar-chart-line"></i>
        </router-link>

        <router-link 
          to="/stock-prediction" 
          class="menu-item-icon"
          title="价格预测"
          :class="{ active: $route.path === '/stock-prediction' }"
        >
          <i class="bi bi-graph-up-arrow"></i>
        </router-link>
        
        <router-link 
          to="/stock-news" 
          class="menu-item-icon"
          title="个股资讯"
          :class="{ active: $route.path === '/stock-news' }"
        >
          <i class="bi bi-newspaper"></i>
        </router-link>

        <router-link 
          to="/data-list" 
          class="menu-item-icon"
          title="数据列表"
          :class="{ active: $route.path === '/data-list' }"
        >
          <i class="bi bi-table"></i>
        </router-link>

        <button 
          class="menu-item-icon ai-icon" 
          @click="toggleAIChat"
          title="AI助手"
        >
          <i class="bi bi-robot"></i>
        </button>
      </nav>
    </aside>

    <!-- 主内容区域 -->
    <main class="main-content">
      <!-- 移动端菜单开关 -->
      <div class="mobile-menu-toggle" @click="toggleMobileSidebar" v-if="isMobile">
        <i class="bi bi-list"></i>
      </div>
      
      <slot></slot>
    </main>

    <!-- 移动端遮罩 -->
    <div 
      class="mobile-overlay" 
      v-if="isMobile && mobileSidebarOpen" 
      @click="closeMobileSidebar"
    ></div>

    <!-- AI聊天窗口 -->
    <div v-if="showAIChat" class="ai-chat-overlay">
      <AIChat @close="showAIChat = false" />
    </div>
  </div>
</template>

<script>
import { ref, computed, onMounted, onUnmounted } from 'vue';
import { advancedSearchStockData } from '@/services/stockDataApi';
import AIChat from '@/components/AIChat.vue';

export default {
  name: 'ResizableSidebar',
  components: {
    AIChat
  },
  setup() {
    const sidebarWidth = ref(260);
    const isCollapsed = ref(false);
    const isResizing = ref(false);
    const isMobile = ref(false);
    const mobileSidebarOpen = ref(false);
    const showAIChat = ref(false);
    
    const stockData = ref([]);
    const isLoading = ref(false);
    const error = ref(null);
    const lastUpdateTime = ref(null);
    
    // 可用股票列表
    const availableStocks = [
      '3M', 'Amazon.com', 'American Express', 'Amgen', 'Apple', 'Boeing',
      'Caterpillar', 'Chevron', 'Cisco', 'Citigroup', 'Coca-Cola', 'Goldman Sachs',
      'Home Depot', 'Honeywell', 'IBM', 'JPMorgan', 'McDonald\'s',
      'Microsoft', 'NVIDIA', 'Salesforce Inc', 'Sherwin-Williams',
      'Travelers', 'UnitedHealth', 'Verizon', 'Visa A', 'Walmart', 'Walt Disney',
      'Nike'
    ];

    // 股票分类
    const stockCategories = ref([
      { label: '全部', value: 'all' },
      { label: '科技股', value: 'tech' },
      { label: '金融股', value: 'financial' },
      { label: '工业股', value: 'industrial' },
      { label: '消费股', value: 'consume' },
      { label: '医药股', value: 'pharmaceutical' }
    ]);

    // 检测屏幕尺寸
    const checkScreenSize = () => {
      isMobile.value = window.innerWidth < 768;
      if (!isMobile.value) {
        mobileSidebarOpen.value = false;
      }
    };

    // 切换折叠状态
    const toggleCollapse = () => {
      isCollapsed.value = !isCollapsed.value;
    };

    // 开始调整大小
    const startResize = (e) => {
      const sidebarRect = e.currentTarget.getBoundingClientRect();
      const edgeThreshold = 5; 
      
      if (e.clientX > sidebarRect.right - edgeThreshold && !isCollapsed.value && !isMobile.value) {
        isResizing.value = true;
        document.addEventListener('mousemove', handleResize);
        document.addEventListener('mouseup', stopResize);
        document.body.style.cursor = 'ew-resize';
        document.body.style.userSelect = 'none';
      }
    };

    const handleResize = (e) => {
      if (!isResizing.value) return;
      
      const newWidth = e.clientX;
      if (newWidth >= 200 && newWidth <= 400) {
        sidebarWidth.value = newWidth;
      }
    };

    const stopResize = () => {
      isResizing.value = false;
      document.removeEventListener('mousemove', handleResize);
      document.removeEventListener('mouseup', stopResize);
      document.body.style.cursor = '';
      document.body.style.userSelect = '';
    };

    const toggleMobileSidebar = () => {
      mobileSidebarOpen.value = !mobileSidebarOpen.value;
    };

    const closeMobileSidebar = () => {
      mobileSidebarOpen.value = false;
    };

    // AI聊天控制
    const toggleAIChat = () => {
      showAIChat.value = !showAIChat.value;
      if (isMobile.value && showAIChat.value) {
        mobileSidebarOpen.value = false;
      }
    };

    // 格式化交易量显示
    const formatVolume = (volume) => {
      if (volume >= 1000000) {
        return (volume / 1000000).toFixed(2) + 'M';
      } else if (volume >= 1000) {
        return (volume / 1000).toFixed(2) + 'K';
      }
      return volume.toString();
    };

    // 获取单只股票数据
    const fetchStockData = async (stockName) => {
      try {
        const endTime = new Date();
        const startTime = new Date();
        startTime.setDate(endTime.getDate() - 3);

        const stockData = await advancedSearchStockData(
          stockName,
          startTime.toISOString(),
          endTime.toISOString()
        );

        if (!stockData || stockData.length === 0) {
          console.warn(`未获取到 ${stockName} 的数据`);
          return null;
        }

        const latestData = stockData[stockData.length - 1];
        
        let openData = null;
        for (let i = stockData.length - 1; i >= 0; i--) {
          const timestamp = new Date(stockData[i].timestamp);
          if (timestamp.getHours() === 9 && timestamp.getMinutes() >= 25 && timestamp.getMinutes() <= 35) {
            openData = stockData[i];
            break;
          }
        }
        
        if (!openData && stockData.length > 0) {
          openData = stockData[0];
        }

        const change = parseFloat(latestData.chg);
        const changeStr = latestData.chgPercent;
        const changePercent = parseFloat(changeStr.replace('%', ''));
        const volume = parseFloat(latestData.vol.replace(/[MK]/g, '')) * 
                      (latestData.vol.includes('M') ? 1000000 : latestData.vol.includes('K') ? 1000 : 1);

        return {
          name: stockName,
          symbol: latestData.symbol,
          volume: volume,
          change: change,
          changePercent: changePercent,
          rawData: latestData
        };
      } catch (err) {
        console.error(`获取 ${stockName} 数据失败:`, err);
        return null;
      }
    };

    // 加载所有股票数据
    const loadStockData = async () => {
      isLoading.value = true;
      error.value = null;
      
      try {
        const promises = availableStocks.map(stock => fetchStockData(stock));
        const results = await Promise.all(promises);
        stockData.value = results.filter(data => data !== null);
        
        if (stockData.value.length === 0) {
          throw new Error('未能获取任何股票数据');
        }
        
        lastUpdateTime.value = new Date().toLocaleString();
      } catch (err) {
        console.error('获取股票数据失败:', err);
        error.value = `获取股票数据失败: ${err.message}`;
      } finally {
        isLoading.value = false;
      }
    };

    // 计算涨幅前3的股票
    const topGainers = computed(() => {
      return [...stockData.value]
        .sort((a, b) => b.changePercent - a.changePercent)
        .slice(0, 3)
        .map(stock => ({
          name: stock.name,
          changePercent: stock.changePercent
        }));
    });

    // 计算成交量前3的股票
    const topVolume = computed(() => {
      return [...stockData.value]
        .sort((a, b) => b.volume - a.volume)
        .slice(0, 3)
        .map(stock => ({
          name: stock.name,
          volume: stock.volume
        }));
    });

    // 生命周期
    onMounted(() => {
      checkScreenSize();
      window.addEventListener('resize', checkScreenSize);
      loadStockData();
    });

    onUnmounted(() => {
      window.removeEventListener('resize', checkScreenSize);
    });

    return {
      sidebarWidth,
      isCollapsed,
      isMobile,
      mobileSidebarOpen,
      showAIChat,
      stockCategories,
      topGainers,
      topVolume,
      isLoading,
      error,
      lastUpdateTime,
      formatVolume,
      toggleCollapse,
      startResize,
      toggleMobileSidebar,
      closeMobileSidebar,
      toggleAIChat
    };
  }
};
</script>

<style scoped>
.layout-container {
  display: flex;
  min-height: 100vh;
  position: relative;
}

.sidebar {
  position: relative;
  background-color: #fff;
  border-right: 1px solid #e5e7eb;
  display: flex;
  flex-direction: column;
  height: 100%;
  transition: width 0.3s ease;
  min-width: 60px;
  z-index: 100;
}

.sidebar::after {
  content: '';
  position: absolute;
  right: 0;
  top: 0;
  bottom: 0;
  width: 4px;
  background: transparent;
  cursor: ew-resize;
  z-index: 10;
}

.sidebar-collapse-btn {
  position: absolute;
  top: 10px;
  right: 10px;
  width: 24px;
  height: 24px;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  z-index: 20;
  background: #f8f9fa;
  border: 1px solid #e5e7eb;
  font-size: 12px;
  color: #6b7280;
}

.sidebar-collapse-btn:hover {
  background: #e9ecef;
  color: #374151;
}

.sidebar-content {
  flex: 1;
  overflow-y: hidden;
  padding: 40px 0 0 0;
}

.sidebar-menu-collapsed {
  padding: 60px 0 0 0;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.menu-item-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 12px;
  color: #4b5563;
  text-decoration: none;
  transition: all 0.2s;
  width: 100%;
  position: relative;
  border: none;
  background: none;
  cursor: pointer;
}

.menu-item-icon:hover {
  background-color: #f9fafb;
  color: #2563eb;
}

.menu-item-icon.active {
  background-color: #eff6ff;
  color: #2563eb;
}

.menu-item-icon.active::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 3px;
  background: #2563eb;
}

.ai-icon {
  margin-top: 10px;
  border-top: 1px solid #f0f0f0;
  padding-top: 15px;
}

.main-content {
  flex: 1;
  overflow: auto;
  transition: margin-left 0.3s ease;
  position: relative;
}

.mobile-menu-toggle {
  position: fixed;
  top: 15px;
  left: 15px;
  width: 40px;
  height: 40px;
  border-radius: 8px;
  background: #fff;
  border: 1px solid #e5e7eb;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  z-index: 99;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.mobile-menu-toggle i {
  font-size: 18px;
  color: #374151;
}

.mobile-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  z-index: 90;
}

/* AI助手样式 */
.ai-assistant-section {
  padding: 15px 0;
  border-top: 1px solid #f0f0f0;
}

.ai-assistant-btn {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 20px;
  background: #f8fafc;
  color: #374151;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s ease;
  width: calc(100% - 40px);
  margin: 0 20px;
  font-size: 14px;
  font-weight: 500;
}

.ai-assistant-btn:hover {
  background: #ffffff;
  border-color: #2563eb;
  color: #2563eb;
  box-shadow: 0 2px 8px rgba(37, 99, 235, 0.1);
}

.ai-assistant-btn i {
  font-size: 16px;
}

/* AI聊天窗口样式 */
.ai-chat-overlay {
  position: fixed;
  bottom: 20px;
  right: 20px;
  z-index: 1000;
  box-shadow: 0 10px 25px rgba(0, 0, 0, 0.2);
  border-radius: 12px;
  width: 380px;
  height: 500px;
}

@media (max-width: 768px) {
  .sidebar {
    position: fixed;
    left: -100%;
    top: 0;
    bottom: 0;
    transition: left 0.3s ease, width 0.3s ease;
  }
  
  .sidebar.mobile-open {
    left: 0;
  }
  
  .main-content {
    margin-left: 0 !important;
    padding-top: 60px;
  }
  
  .sidebar-collapse-btn {
    display: none;
  }
  
  .ai-chat-overlay {
    position: fixed;
    top: 50%;
    left: 50%;
    transform: translate(-50%, -50%);
    width: 90%;
    height: 80%;
    max-width: 400px;
    max-height: 600px;
  }
}

.sidebar-header {
  padding: 0 20px 15px;
  border-bottom: 1px solid #f0f0f0;
}

.sidebar-title {
  font-size: 18px;
  font-weight: 600;
  color: #1f2937;
  margin: 0;
}

.sidebar-menu {
  padding: 15px 0;
}

.menu-item {
  display: flex;
  align-items: center;
  padding: 12px 20px;
  color: #4b5563;
  text-decoration: none;
  transition: all 0.2s;
  gap: 10px;
}

.menu-item:hover {
  background-color: #f9fafb;
  color: #2563eb;
}

.menu-item.active {
  background-color: #eff6ff;
  color: #2563eb;
  border-right: 3px solid #2563eb;
}

.section-title {
  font-size: 14px;
  color: #6b7280;
  margin: 0 0 10px 0;
  font-weight: 500;
  padding: 0 20px;
}

.top-gainers, .top-volume {
  padding: 15px 0;
  border-top: 1px solid #f0f0f0;
}

.sidebar-filter {
  padding: 15px 0;
  border-top: 1px solid #f0f0f0;
}

.filter-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  padding: 0 20px;
}

.tag-btn {
  padding: 4px 10px;
  border-radius: 16px;
  border: 1px solid #e5e7eb;
  background-color: #fff;
  color: #6b7280;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s;
  text-decoration: none;
  display: inline-block;
}

.tag-btn:hover {
  border-color: #2563eb;
  color: #2563eb;
}

.tag-btn.active {
  background-color: #2563eb;
  color: #fff;
  border-color: #2563eb;
}

.gainer-list, .volume-list {
  padding: 0 20px;
}

.gainer-item, .volume-item {
  display: flex;
  justify-content: space-between;
  padding: 6px 0;
  font-size: 13px;
  border-bottom: 1px solid #f9fafb;
}

.gainer-item:last-child, .volume-item:last-child {
  border-bottom: none;
}

.gainer-name, .volume-name {
  color: #4b5563;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 140px;
}

.gainer-change {
  font-weight: 500;
}

.volume-value {
  color: #6b7280;
  font-weight: 500;
}

.sidebar-footer {
  padding: 15px 20px;
  border-top: 1px solid #f0f0f0;
  font-size: 12px;
  color: #9ca3af;
  margin-top: auto;
}

.sidebar-footer p {
  margin: 0;
}

.positive {
  color: #ef4444;
}

.negative {
  color: #10b981;
}

.sidebar::-webkit-scrollbar {
  width: 4px;
}

.sidebar::-webkit-scrollbar-track {
  background: #f1f1f1;
}

.sidebar::-webkit-scrollbar-thumb {
  background: #c1c1c1;
  border-radius: 2px;
}

.sidebar::-webkit-scrollbar-thumb:hover {
  background: #a8a8a8;
}
</style>