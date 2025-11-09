<template>
  <div class="data-list-container">
    <!-- 数据列表 -->
    <div class="combined-controls">
      <div class="controls-left">
        <h2 class="title">股票数据列表</h2>
        <div class="search-sort">
          <div class="search-input-container">
            <i class="fas fa-search search-icon"></i>
            <input
              v-model="searchQuery"
              type="text"
              placeholder="搜索股票..."
              class="search-input"
              @input="handleSearch"
            />
          </div>
          <div class="sort-container">
            <select v-model="sortField" class="sort-select">
              <option value="name">按名称排序</option>
              <option value="last">按最新价格排序</option>
              <option value="chgPercent">按涨跌幅排序</option>
            </select>
            <button @click="sortData" class="sort-btn">
              <i class="fas fa-sort"></i> 排序
            </button>
          </div>
        </div>
      </div>
      
      <div class="controls-right" v-if="pagination.totalPages > 1">
        <div class="pagination-controls">
          <button 
            @click="prevPage" 
            :disabled="pagination.currentPage === 1"
            class="page-btn prev-btn"
          >
            <i class="fas fa-chevron-left"></i>
          </button>
          
          <div class="page-numbers">
            <button
              v-for="page in visiblePages"
              :key="page"
              @click="goToPage(page)"
              :class="{
                'page-number': true,
                'active': page === pagination.currentPage,
                'ellipsis': page === '...'
              }"
              :disabled="page === '...'"
            >
              {{ page }}
            </button>
          </div>
          
          <button 
            @click="nextPage" 
            :disabled="pagination.currentPage === pagination.totalPages"
            class="page-btn next-btn"
          >
            <i class="fas fa-chevron-right"></i>
          </button>
          
          <div class="page-info">
            <span>{{ pagination.currentPage }}/{{ pagination.totalPages }}</span>
            <span class="total-info">({{ pagination.totalElements }}条)</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 数据表格 -->
    <div class="data-table-wrapper">
      <table class="data-table">
        <thead>
          <tr>
            <th>时间戳</th>
            <th>股票名称</th>
            <th>最新价格</th>
            <th>最高价</th>
            <th>最低价</th>
            <th>涨跌额</th>
            <th>涨跌幅</th>
            <th>成交量</th>
            <th>更新时间</th>
          </tr>
        </thead>
        <tbody>
          <tr
            v-for="(item, index) in filteredData"
            :key="index"
            :class="{ 'even-row': index % 2 === 0, 'odd-row': index % 2 !== 0 }"
          >
            <td>{{ formatDate(item.timestamp) }}</td>
            <td class="stock-name">{{ item.name }}</td>
            <td class="price">{{ formatPrice(item.last) }}</td>
            <td class="price">{{ formatPrice(item.high) }}</td>
            <td class="price">{{ formatPrice(item.low) }}</td>
            <td :class="item.chg >= 0 ? 'positive' : 'negative'">
              {{ formatChange(item.chg) }}
            </td>
            <td :class="item.chgPercent >= 0 ? 'positive' : 'negative'">
              {{ formatChangePercent(item.chgPercent) }}
            </td>
            <td class="volume">{{ formatVolume(item.vol) }}</td>
            <td>{{ formatTime(item.time) }}</td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- 无数据提示 -->
    <div v-if="filteredData.length === 0 && !loading" class="no-data">
      <i class="fas fa-inbox"></i>
      <p>{{ searchQuery ? "没有找到匹配的数据" : "当前没有股票数据" }}</p>
    </div>

    <!-- 加载状态 -->
    <div v-if="loading" class="loading">
      <div class="loading-spinner"></div>
      <p>加载中...</p>
    </div>
  </div>
</template>

<script>
import * as stockDataApi from '@/services/stockDataApi';

export default {
  name: 'StockDataList',
  data() {
    return {
      searchQuery: '',
      sortField: 'name',
      sortAscending: true,
      dataList: [],
      loading: false,
      pagination: {
        currentPage: 1,
        totalPages: 0,
        totalElements: 0,
        pageSize: 10
      },
      searchTimeout: null
    };
  },
  computed: {
    filteredData() {
      const query = this.searchQuery.toLowerCase();
      if (!query) {
        return this.dataList;
      }
      return this.dataList.filter(item =>
        item.name.toLowerCase().includes(query)
      );
    },
    
    visiblePages() {
      const current = this.pagination.currentPage;
      const total = this.pagination.totalPages;
      const pages = [];
      
      // 总页数小于等于7时显示所有页码
      if (total <= 7) {
        for (let i = 1; i <= total; i++) {
          pages.push(i);
        }
        return pages;
      }
      
      // 总页数大于7时，显示部分页码和省略号
      if (current <= 4) {
        // 当前页在前4页
        for (let i = 1; i <= 5; i++) {
          pages.push(i);
        }
        pages.push('...');
        pages.push(total);
      } else if (current >= total - 3) {
        // 当前页在后4页
        pages.push(1);
        pages.push('...');
        for (let i = total - 4; i <= total; i++) {
          pages.push(i);
        }
      } else {
        // 当前页在中间
        pages.push(1);
        pages.push('...');
        for (let i = current - 1; i <= current + 1; i++) {
          pages.push(i);
        }
        pages.push('...');
        pages.push(total);
      }
      
      return pages;
    }
  },
  created() {
    this.fetchStockData();
  },
  methods: {
    async fetchStockData() {
      this.loading = true;
      try {
        // 前端页码转后端页码（前端1页=后端0页）
        const backendPage = this.pagination.currentPage - 1;
        
        const response = await stockDataApi.fetchAllStockData(
          backendPage,
          this.pagination.pageSize
        );
        
        if (response && response.content) {
          // 确保数值字段正确转换类型
          this.dataList = response.content.map(item => ({
            ...item,
            last: this.parseNumber(item.last),
            high: this.parseNumber(item.high),
            low: this.parseNumber(item.low),
            chg: this.parseNumber(item.chg),
            chgPercent: this.parsePercent(item.chgPercent),
            vol: this.parseVolume(item.vol)
          }));
          
          // 后端返回的页码信息
          this.pagination.totalPages = response.totalPages;
          this.pagination.totalElements = response.totalElements;
        } else {
          console.warn('意外的API响应格式:', response);
          this.dataList = [];
        }
      } catch (error) {
        console.error('获取股票数据失败:', error);
        this.$notify?.error({
          title: '错误',
          message: '获取股票数据失败，请稍后重试'
        });
      } finally {
        this.loading = false;
      }
    },
    
    async handleSearch() {
      clearTimeout(this.searchTimeout);
      
      this.searchTimeout = setTimeout(async () => {
        if (this.searchQuery.trim() === '') {
          this.fetchStockData();
          return;
        }
        
        this.loading = true;
        try {
          const response = await stockDataApi.searchStockDataByName(this.searchQuery);
          
          if (Array.isArray(response)) {
            this.dataList = response.map(item => ({
              ...item,
              last: this.parseNumber(item.last),
              high: this.parseNumber(item.high),
              low: this.parseNumber(item.low),
              chg: this.parseNumber(item.chg), 
              chgPercent: this.parsePercent(item.chgPercent),
              vol: this.parseVolume(item.vol)
            }));
            this.pagination.totalPages = 1;
          } else {
            console.warn('搜索API返回了意外的格式:', response);
            this.dataList = [];
          }
        } catch (error) {
          console.error('搜索失败:', error);
          this.$notify?.error({
            title: '错误',
            message: '搜索失败，请稍后重试'
          });
        } finally {
          this.loading = false;
        }
      }, 300);
    },
    
    sortData() {
      this.dataList.sort((a, b) => {
        let valueA = a[this.sortField];
        let valueB = b[this.sortField];

        // 处理数值比较
        if (typeof valueA === 'number' && typeof valueB === 'number') {
          return this.sortAscending ? valueA - valueB : valueB - valueA;
        }

        // 处理字符串比较
        valueA = String(valueA).toLowerCase();
        valueB = String(valueB).toLowerCase();
        return this.sortAscending
          ? valueA.localeCompare(valueB)
          : valueB.localeCompare(valueA);
      });

      this.sortAscending = !this.sortAscending;
    },
    
    nextPage() {
      if (this.pagination.currentPage < this.pagination.totalPages) {
        this.pagination.currentPage++;
        this.fetchStockData();
      }
    },
    
    prevPage() {
      if (this.pagination.currentPage > 1) {
        this.pagination.currentPage--;
        this.fetchStockData();
      }
    },
    
    goToPage(page) {
      if (page !== '...' && page !== this.pagination.currentPage) {
        this.pagination.currentPage = page;
        this.fetchStockData();
      }
    },
    
    // 数据格式化方法
    parseNumber(value) {
      if (value === null || value === undefined) return 0;
      if (typeof value === 'string') {
        // 处理百分比字符串
        if (value.endsWith('%')) {
          return parseFloat(value) || 0;
        }
        // 处理普通数字字符串
        return parseFloat(value) || 0;
      }
      return Number(value) || 0;
    },
    
    parsePercent(value) {
      if (value === null || value === undefined) return 0;
      if (typeof value === 'string') {
        // 移除百分号并转换为数字
        return parseFloat(value.replace('%', '')) || 0;
      }
      return Number(value) || 0;
    },
    
    // 解析交易量字符串
    parseVolume(volumeStr) {
      if (!volumeStr) return 0;
      const lastChar = volumeStr.slice(-1).toUpperCase();
      const number = parseFloat(volumeStr);
      
      switch (lastChar) {
        case 'M':
          return number * 1000000;
        case 'K':
          return number * 1000;
        default:
          return number;
      }
    },
    
    formatPrice(price) {
      if (price === null || price === undefined) return '--';
      return this.parseNumber(price).toFixed(2);
    },
    
    formatChange(change) {
      if (change === null || change === undefined) return '--';
      const num = this.parseNumber(change);
      return num >= 0 ? `+${num.toFixed(2)}` : num.toFixed(2);
    },
    
    formatChangePercent(percent) {
      if (percent === null || percent === undefined) return '--';
      const num = this.parseNumber(percent);
      return num >= 0 ? `+${num.toFixed(2)}%` : `${num.toFixed(2)}%`;
    },
    
    formatVolume(volume) {
      if (volume === null || volume === undefined) return '--';
      const num = this.parseNumber(volume);
      if (num >= 1000000) {
        return (num / 1000000).toFixed(2) + 'M';
      } else if (num >= 1000) {
        return (num / 1000).toFixed(2) + 'K';
      }
      return num.toFixed(0);
    },
    
    formatDate(dateString) {
      if (!dateString) return '';
      
      try {
        const date = new Date(dateString);
        if (!isNaN(date.getTime())) {
          return date.toLocaleString();
        }
        
        const parts = dateString.split(/[/\s:]/);
        if (parts.length >= 5) {
          return `${parts[0]}年${parts[1]}月${parts[2]}日 ${parts[3]}:${parts[4]}`;
        }
      } catch (e) {
        console.warn('无法解析日期:', dateString);
      }
      
      return dateString;
    },
    
    formatTime(timeString) {
      if (!timeString) return '--';
      try {
        // 处理时间字符串
        const date = new Date(timeString);
        if (!isNaN(date.getTime())) {
          return date.toLocaleTimeString('zh-CN', {
            hour: '2-digit',
            minute: '2-digit'
          });
        }
        return timeString;
      } catch (e) {
        return timeString;
      }
    }
  }
};
</script>

<style scoped>
.data-list-container {
  font-family: 'Inter', system-ui, -apple-system, sans-serif;
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
  background-color: #f8fafc;
  min-height: 100vh;
}

.combined-controls {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  background-color: #ffffff;
  padding: 16px 20px;
  border-radius: 12px;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.05), 0 2px 4px -1px rgba(0, 0, 0, 0.03);
  gap: 20px;
}

.controls-left {
  display: flex;
  align-items: center;
  gap: 20px;
  flex: 1;
}

.title {
  font-size: 20px;
  font-weight: 600;
  color: #1e293b;
  margin: 0;
  white-space: nowrap;
}

.search-sort {
  display: flex;
  gap: 12px;
  align-items: center;
  flex-wrap: nowrap;
}

.search-input-container {
  position: relative;
  display: flex;
  align-items: center;
}

.search-icon {
  position: absolute;
  left: 10px;
  color: #94a3b8;
  font-size: 13px;
}

.search-input {
  padding: 8px 10px 8px 28px;
  border-radius: 6px;
  border: 1px solid #e2e8f0;
  background-color: #ffffff;
  font-size: 13px;
  width: 140px;
  transition: all 0.3s ease;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
}

.search-input:focus {
  outline: none;
  border-color: #3b82f6;
  box-shadow: 0 0 0 2px rgba(59, 130, 246, 0.2);
  width: 160px;
}

.sort-container {
  display: flex;
  gap: 6px;
}

.sort-select {
  padding: 8px 10px;
  border-radius: 6px;
  border: 1px solid #e2e8f0;
  background-color: #ffffff;
  font-size: 13px;
  transition: all 0.3s ease;
  min-width: 120px;
}

.sort-select:focus {
  outline: none;
  border-color: #3b82f6;
}

.sort-btn {
  padding: 8px 12px;
  background-color: #3b82f6;
  color: white;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 13px;
  transition: all 0.3s ease;
  display: flex;
  align-items: center;
  gap: 4px;
  white-space: nowrap;
}

.sort-btn:hover {
  background-color: #2563eb;
  transform: translateY(-1px);
  box-shadow: 0 2px 4px rgba(59, 130, 246, 0.2);
}

.controls-right {
  display: flex;
  align-items: center;
}

.pagination-controls {
  display: flex;
  align-items: center;
  gap: 8px;
}

.page-numbers {
  display: flex;
  gap: 4px;
}

.page-number {
  padding: 6px 10px;
  background-color: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  cursor: pointer;
  font-size: 13px;
  transition: all 0.3s ease;
  min-width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.page-number:hover:not(.ellipsis):not(.active) {
  background-color: #f8fafc;
  border-color: #cbd5e1;
}

.page-number.active {
  background-color: #3b82f6;
  color: white;
  border-color: #3b82f6;
}

.page-number.ellipsis {
  background-color: transparent;
  border: none;
  cursor: default;
  min-width: 24px;
}

.page-btn {
  padding: 6px 10px;
  background-color: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  cursor: pointer;
  font-size: 12px;
  transition: all 0.3s ease;
  display: flex;
  align-items: center;
  justify-content: center;
  height: 32px;
  width: 32px;
  color: #4a5568;
}

.page-btn:hover:not(:disabled) {
  background-color: #f8fafc;
  border-color: #cbd5e1;
  color: #3b82f6;
}

.page-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  color: #94a3b8;
}

.page-info {
  display: flex;
  flex-direction: column;
  align-items: center;
  font-size: 12px;
  color: #4a5568;
  margin-left: 8px;
  line-height: 1.2;
  white-space: nowrap;
}

.total-info {
  font-size: 11px;
  color: #94a3b8;
}

.data-table-wrapper {
  overflow-x: auto;
  margin-bottom: 20px;
  background-color: #ffffff;
  border-radius: 12px;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.05), 0 2px 4px -1px rgba(0, 0, 0, 0.03);
}

.data-table {
  width: 100%;
  border-collapse: collapse;
}

.data-table th,
.data-table td {
  padding: 14px 12px;
  text-align: left;
  border-bottom: 1px solid #f1f5f9;
  font-size: 14px;
}

.data-table th {
  background-color: #f8fafc;
  font-weight: 600;
  color: #4a5568;
  font-size: 14px;
  position: sticky;
  top: 0;
}

.data-table tr:hover {
  background-color: #f8fafc;
}

.data-table tr.even-row {
  background-color: #f9f9f9;
}

.data-table tr.odd-row {
  background-color: #ffffff;
}

.stock-name {
  font-weight: 500;
  color: #1e293b;
}

.price {
  font-family: 'SF Mono', 'Monaco', 'Inconsolata', 'Roboto Mono', monospace;
  font-weight: 500;
}

.volume {
  font-family: 'SF Mono', 'Monaco', 'Inconsolata', 'Roboto Mono', monospace;
}

.positive {
  color: #ef4444;
  font-weight: 500;
}

.negative {
  color: #10b981;
  font-weight: 500;
}

.no-data, .loading {
  text-align: center;
  padding: 40px;
  color: #64748b;
  font-size: 16px;
  background-color: #ffffff;
  border-radius: 12px;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.05), 0 2px 4px -1px rgba(0, 0, 0, 0.03);
}

.no-data {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
}

.no-data i {
  font-size: 48px;
  color: #cbd5e1;
}

.loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.loading-spinner {
  width: 40px;
  height: 40px;
  border: 4px solid rgba(59, 130, 246, 0.1);
  border-left-color: #3b82f6;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-bottom: 16px;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

/* 移动端适配 */
@media (max-width: 768px) {
  .data-list-container {
    padding: 15px;
  }
  
  .combined-controls {
    flex-direction: column;
    align-items: flex-start;
    gap: 15px;
    padding: 15px;
  }
  
  .controls-left {
    flex-direction: column;
    align-items: flex-start;
    gap: 15px;
    width: 100%;
  }
  
  .search-sort {
    width: 100%;
    flex-direction: column;
  }
  
  .search-input, .sort-select, .sort-btn {
    width: 100%;
  }
  
  .search-input:focus {
    width: 100%;
  }
  
  .controls-right {
    width: 100%;
  }
  
  .pagination-controls {
    width: 100%;
    justify-content: space-between;
  }
  
  .page-numbers {
    flex-wrap: wrap;
    justify-content: center;
  }
  
  .data-table th,
  .data-table td {
    padding: 10px 8px;
    font-size: 13px;
  }
}

/* 小屏幕适配 */
@media (max-width: 480px) {
  .combined-controls {
    padding: 12px;
  }
  
  .title {
    font-size: 18px;
  }
  
  .search-input {
    width: 100%;
  }
  
  .page-numbers {
    gap: 2px;
  }
  
  .page-number {
    padding: 4px 6px;
    min-width: 28px;
    height: 28px;
    font-size: 12px;
  }
  
  .page-btn {
    width: 28px;
    height: 28px;
  }
}
</style>