<template>
  <div class="stock-news-container">
    <!-- 个股资讯 -->
    <!-- 公司选择区域 -->
    <div class="company-selector-section">
      <div class="company-controls">
        <div class="selector-group">
          <label for="company-select" class="selector-label">选择公司:</label>
          <select 
            id="company-select" 
            v-model="selectedCompany" 
            @change="handleCompanyChange"
            class="company-select"
          >
            <option value="">请选择公司</option>
            <option v-for="company in availableCompanies" :key="company.value" :value="company.value">
              {{ company.value }} ({{ company.symbol }})
            </option>
          </select>
        </div>

        <div class="search-group">
          <label for="custom-search" class="selector-label">自定义搜索:</label>
          <div class="search-input-group">
            <input
              id="custom-search"
              v-model="customSearchQuery"
              type="text"
              placeholder="输入公司名或股票代码..."
              class="search-input"
              @keyup.enter="handleSearch"
            />
            <div class="action-buttons">
              <button 
                @click="handleSearch" 
                :disabled="isRefreshingNews || (!selectedCompany && !customSearchQuery)"
                class="action-btn primary"
              >
                <span class="btn-text">{{ isRefreshingNews ? '获取中...' : '获取新闻' }}</span>
              </button>
              <button @click="clearNews" class="action-btn secondary">
                <span class="btn-text">清空新闻</span>
              </button>
            </div>
          </div>
        </div>
      </div>

      <div class="current-selection" v-if="currentCompanyInfo">
        <div class="selection-card">
          <h3>当前选择</h3>
          <div class="company-info">
            <span class="company-name">{{ currentCompanyInfo.label }}</span>
            <span class="company-symbol">{{ currentCompanyInfo.symbol }}</span>
          </div>
          <div class="search-query">
            搜索关键词: <strong>"{{ getSearchKeywords(currentCompanyInfo.value) }}"</strong>
          </div>
        </div>
      </div>
    </div>

    <!-- 新闻展示区域 -->
    <div class="news-display-section">
      <div class="section-header">
        <h2>新闻展示</h2>
        <div class="news-stats">
          <span class="stat-item">总数: {{ newsData.length }}</span>
          <span class="stat-item">最后更新: {{ lastUpdateTime }}</span>
        </div>
      </div>

      <!-- 状态提示 -->
      <div v-if="statusMessage" class="status-message" :class="statusType">
        {{ statusMessage }}
      </div>

      <!-- 加载状态 -->
      <div v-if="isRefreshingNews" class="loading-state">
        <div class="spinner"></div>
        <p>正在从NewsAPI获取最新新闻...</p>
        <div class="loading-details">
          <p>搜索关键词: "{{ getSearchKeywords(selectedCompany) }}"</p>
        </div>
      </div>

      <!-- 错误状态 -->
      <div v-else-if="errorMessage" class="error-state">
        <div class="error-icon">⚠️</div>
        <h3>获取新闻失败</h3>
        <p>{{ errorMessage }}</p>
        <div class="error-actions">
          <button @click="handleSearch" class="retry-btn">重试</button>
        </div>
      </div>

      <!-- 无数据状态 -->
      <div v-else-if="newsData.length === 0 && !isRefreshingNews" class="empty-state">
        <div class="empty-icon">📰</div>
        <h3>暂无新闻数据</h3>
        <p>请选择一家公司并点击"获取新闻"按钮</p>
      </div>

      <!-- 新闻列表 -->
      <div v-else class="news-grid">
        <div 
          v-for="(news, index) in newsData" 
          :key="`${news.url}-${index}`" 
          class="news-card"
        >
          <div class="news-header">
            <div class="news-meta">
              <span class="news-time">{{ formatNewsTime(news.publishedAt) }}</span>
              <span class="news-source">{{ news.source?.name || '未知来源' }}</span>
            </div>
            <div class="news-actions">
              <button @click="toggleNewsDetail(index)" class="detail-btn">
                {{ expandedNews.includes(index) ? '收起' : '详情' }}
              </button>
            </div>
          </div>

          <h3 class="news-title">
            <a 
              :href="news.url" 
              target="_blank" 
              rel="noopener noreferrer"
              class="news-link"
              @click.stop
            >
              {{ news.title }}
            </a>
          </h3>

          <div v-if="expandedNews.includes(index)" class="news-details">
            <p class="news-description">{{ news.description || '暂无描述' }}</p>
            <div class="news-content" v-if="news.content">
              <p>{{ truncateContent(news.content) }}</p>
            </div>
            <div class="news-footer">
              <div class="news-tags">
                <span v-if="news.author" class="news-tag">作者: {{ news.author }}</span>
                <span class="news-tag">{{ currentCompanyInfo?.symbol || '未知' }}</span>
              </div>
              <a 
                :href="news.url" 
                target="_blank" 
                rel="noopener noreferrer"
                class="read-more"
              >
                阅读全文 →
              </a>
            </div>
          </div>
        </div>
      </div>

      <!-- 分页控制 -->
      <div v-if="newsData.length > 0" class="pagination-controls">
        <button 
          @click="loadMoreNews" 
          :disabled="!hasMoreNews || isRefreshingNews"
          class="load-more-btn"
        >
          {{ hasMoreNews ? '加载更多' : '没有更多了' }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import axios from 'axios'

// 公司数据
const availableCompanies = [
  { value: '3M', label: '3M公司', symbol: 'MMM', searchTerms: ['3M', 'MMM', 'Post-it', 'Scotch'] },
  { value: 'Amazon.com', label: '亚马逊', symbol: 'AMZN', searchTerms: ['Amazon', 'AMZN', 'AWS', 'Prime'] },
  { value: 'American Express', label: '美国运通', symbol: 'AXP', searchTerms: ['American Express', 'AXP', 'Credit Card'] },
  { value: 'Amgen', label: '安进', symbol: 'AMGN', searchTerms: ['Amgen', 'AMGN', 'Biotech'] },
  { value: 'Apple', label: '苹果公司', symbol: 'AAPL', searchTerms: ['Apple', 'AAPL', 'iPhone', 'Mac'] },
  { value: 'Boeing', label: '波音', symbol: 'BA', searchTerms: ['Boeing', 'BA', 'Aerospace', '737 MAX'] },
  { value: 'Caterpillar', label: '卡特彼勒', symbol: 'CAT', searchTerms: ['Caterpillar', 'CAT', 'Construction', 'Heavy Equipment'] },
  { value: 'Chevron', label: '雪佛龙', symbol: 'CVX', searchTerms: ['Chevron', 'CVX', 'Oil', 'Gas'] },
  { value: 'Cisco', label: '思科', symbol: 'CSCO', searchTerms: ['Cisco', 'CSCO', 'Networking', 'Router'] },
  { value: 'Citigroup', label: '花旗集团', symbol: 'C', searchTerms: ['Citigroup', 'C', 'Banking', 'Finance'] },
  { value: 'Coca-Cola', label: '可口可乐', symbol: 'KO', searchTerms: ['Coca-Cola', 'KO', 'Soda', 'Beverage'] },
  { value: 'Goldman Sachs', label: '高盛', symbol: 'GS', searchTerms: ['Goldman Sachs', 'GS', 'Investment Banking'] },
  { value: 'Home Depot', label: '家得宝', symbol: 'HD', searchTerms: ['Home Depot', 'HD', 'Home Improvement'] },
  { value: 'Honeywell', label: '霍尼韦尔', symbol: 'HON', searchTerms: ['Honeywell', 'HON', 'Industrial', 'Aerospace'] },
  { value: 'IBM', label: 'IBM', symbol: 'IBM', searchTerms: ['IBM', 'International Business Machines', 'Mainframe', 'Watson'] },
  { value: 'JPMorgan', label: '摩根大通', symbol: 'JPM', searchTerms: ['JPMorgan', 'JPM', 'Chase', 'Banking'] },
  { value: 'McDonald’s', label: '麦当劳', symbol: 'MCD', searchTerms: ['McDonald’s', 'MCD', 'Fast Food', 'Burgers'] },
  { value: 'Microsoft', label: '微软', symbol: 'MSFT', searchTerms: ['Microsoft', 'MSFT', 'Windows', 'Office'] },
  { value: 'NVIDIA', label: '英伟达', symbol: 'NVDA', searchTerms: ['NVIDIA', 'NVDA', 'GPU', 'Graphics'] },
  { value: 'Salesforce Inc', label: '赛富时', symbol: 'CRM', searchTerms: ['Salesforce', 'CRM', 'Cloud', 'SaaS'] },
  { value: 'Sherwin-Williams', label: '宣伟', symbol: 'SHW', searchTerms: ['Sherwin-Williams', 'SHW', 'Paint', 'Coatings'] },
  { value: 'Travelers', label: '旅行者保险', symbol: 'TRV', searchTerms: ['Travelers', 'TRV', 'Insurance'] },
  { value: 'UnitedHealth', label: '联合健康', symbol: 'UNH', searchTerms: ['UnitedHealth', 'UNH', 'Healthcare', 'Insurance'] },
  { value: 'Verizon', label: '威瑞森', symbol: 'VZ', searchTerms: ['Verizon', 'VZ', 'Telecom', 'Wireless'] },
  { value: 'Visa A', label: '维萨', symbol: 'V', searchTerms: ['Visa', 'V', 'Credit Card', 'Payment'] },
  { value: 'Walmart', label: '沃尔玛', symbol: 'WMT', searchTerms: ['Walmart', 'WMT', 'Retail', 'Discount'] },
  { value: 'Walt Disney', label: '华特迪士尼', symbol: 'DIS', searchTerms: ['Disney', 'DIS', 'Entertainment', 'Parks'] },
  { value: 'Nike', label: '耐克', symbol: 'NKE', searchTerms: ['Nike', 'NKE', 'Sneakers', 'Athletic'] }
];

// 响应式数据
const selectedCompany = ref('Apple')
const customSearchQuery = ref('')
const newsData = ref([])
const isRefreshingNews = ref(false)
const errorMessage = ref('')
const statusMessage = ref('')
const statusType = ref('info')
const hasMoreNews = ref(false)
const expandedNews = ref([])
const lastUpdateTime = ref('')
const currentSearchStrategy = ref('')

// API密钥
const apiKey = 'myapikey'

// 计算属性
const currentCompanyInfo = computed(() => {
  return availableCompanies.find(company => company.value === selectedCompany.value)
})

// 方法
const getSearchKeywords = (company) => {
  if (customSearchQuery.value) {
    return customSearchQuery.value
  }
  
  const companyInfo = availableCompanies.find(c => c.value === company)
  if (!companyInfo) return ''
  
  return companyInfo.searchTerms.join(' OR ')
}

const handleCompanyChange = () => {
  customSearchQuery.value = ''
  errorMessage.value = ''
  statusMessage.value = `已选择: ${currentCompanyInfo.value?.label}`
}

const handleSearch = () => {
  if (customSearchQuery.value.trim()) {
    selectedCompany.value = ''
  }
  refreshNews()
}

// 新闻获取函数
const refreshNews = async () => {
  if (!selectedCompany.value && !customSearchQuery.value) {
    errorMessage.value = '请选择公司或输入搜索关键词'
    return
  }

  isRefreshingNews.value = true
  errorMessage.value = ''
  statusMessage.value = '正在获取新闻...'
  statusType.value = 'info'

  try {
    const companyInfo = currentCompanyInfo.value
    const searchQuery = customSearchQuery.value || getSearchKeywords(selectedCompany.value)
    
    console.log('开始搜索新闻:', {
      company: selectedCompany.value,
      searchQuery: searchQuery,
      companyInfo: companyInfo
    })

    // 搜索策略
    const searchStrategies = [
      {
        name: '精确匹配公司名和股票代码',
        query: `"${companyInfo?.label}" OR "${companyInfo?.symbol}"`,
        params: { q: `"${companyInfo?.label}" OR "${companyInfo?.symbol}"`, language: 'en' }
      },
      {
        name: '使用预定义搜索词',
        query: searchQuery,
        params: { q: searchQuery, language: 'en' }
      },
      {
        name: '放宽搜索范围',
        query: `${companyInfo?.value} OR ${companyInfo?.symbol}`,
        params: { q: `${companyInfo?.value} OR ${companyInfo?.symbol}`, language: 'en' }
      }
    ]

    let articles = []

    for (const strategy of searchStrategies) {
      if (!selectedCompany.value && !customSearchQuery.value) break
      
      currentSearchStrategy.value = strategy.name
      console.log(`尝试搜索策略: ${strategy.name}`, strategy.params)

      try {
        const response = await axios.get('https://newsapi.org/v2/everything', {
          params: {
            ...strategy.params,
            sortBy: 'publishedAt',
            pageSize: 20,
            page: 1,
            apiKey: apiKey
          },
          timeout: 10000
        })

        console.log(`策略 "${strategy.name}" 结果:`, {
          totalResults: response.data.totalResults,
          articlesCount: response.data.articles?.length
        })

        if (response.data.articles && response.data.articles.length > 0) {
          console.log(`策略 "${strategy.name}" 成功找到 ${response.data.articles.length} 条新闻`)
          articles = response.data.articles
          break
        }
      } catch (strategyError) {
        console.warn(`策略 "${strategy.name}" 失败:`, strategyError.message)
        continue
      }
    }

    if (articles.length > 0) {
      // 处理获取到的新闻数据
      newsData.value = articles.map(article => ({
        ...article,
        title: article.title || '无标题',
        description: article.description || '暂无描述',
        url: article.url || '#',
        publishedAt: article.publishedAt || new Date().toISOString(),
        source: article.source || { name: '未知来源' }
      }))
      
      statusMessage.value = `成功获取 ${newsData.value.length} 条新闻`
      statusType.value = 'success'
      lastUpdateTime.value = new Date().toLocaleString('zh-CN')
    } else {
      newsData.value = []
      statusMessage.value = '未找到相关新闻'
      statusType.value = 'warning'
    }

    hasMoreNews.value = false

  } catch (error) {
    console.error('获取新闻失败:', error)
    handleNewsError(error)
  } finally {
    isRefreshingNews.value = false
    setTimeout(() => {
      statusMessage.value = ''
    }, 5000)
  }
}

const handleNewsError = (error) => {
  if (error.response?.status === 401) {
    errorMessage.value = 'API密钥无效，请检查API配置'
  } else if (error.response?.status === 426) {
    errorMessage.value = 'API请求次数超限，请稍后再试'
  } else if (error.response?.status === 429) {
    errorMessage.value = '请求过于频繁，请稍后重试'
  } else if (error.code === 'ECONNABORTED') {
    errorMessage.value = '请求超时，请检查网络连接'
  } else {
    errorMessage.value = `获取新闻失败: ${error.message || '网络错误'}`
  }
}

const clearNews = () => {
  newsData.value = []
  errorMessage.value = ''
  statusMessage.value = '已清空新闻数据'
  statusType.value = 'info'
}

const toggleNewsDetail = (index) => {
  const position = expandedNews.value.indexOf(index)
  if (position > -1) {
    expandedNews.value.splice(position, 1)
  } else {
    expandedNews.value.push(index)
  }
}

const loadMoreNews = () => {
  statusMessage.value = '免费版NewsAPI不支持加载更多功能'
  statusType.value = 'warning'
}

const formatNewsTime = (timestamp) => {
  if (!timestamp) return '未知时间'
  
  const now = new Date()
  const newsTime = new Date(timestamp)
  const diffMs = now - newsTime
  const diffMins = Math.floor(diffMs / 60000)
  const diffHours = Math.floor(diffMs / 3600000)
  const diffDays = Math.floor(diffMs / 86400000)

  if (diffMins < 1) return '刚刚'
  if (diffMins < 60) return `${diffMins}分钟前`
  if (diffHours < 24) return `${diffHours}小时前`
  if (diffDays < 7) return `${diffDays}天前`
  
  return newsTime.toLocaleDateString('zh-CN')
}

const truncateContent = (content) => {
  if (!content) return ''
  return content.length > 200 ? content.substring(0, 200) + '...' : content
}

// 生命周期
onMounted(() => {
  statusMessage.value = '请选择公司开始获取新闻'
  statusType.value = 'info'
})
</script>

<style scoped>
.stock-news-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 16px;
  font-family: 'Inter', system-ui, -apple-system, sans-serif;
  background-color: #f8fafc;
  min-height: 100vh;
}

/* 公司选择区域 */
.company-selector-section {
  background: white;
  padding: 1.25rem;
  border-radius: 12px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  margin-bottom: 1.5rem;
}

.company-controls {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1.5rem;
  margin-bottom: 1rem;
}

.selector-group,
.search-group {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.selector-label {
  font-weight: 600;
  color: #374151;
  font-size: 0.9rem;
  margin-bottom: 0.25rem;
}

.company-select,
.search-input {
  padding: 10px 12px;
  border: 1px solid #d1d5db;
  border-radius: 8px;
  font-size: 14px;
  background: white;
  height: 42px;
  box-sizing: border-box;
  width: 100%;
}

.company-select:focus,
.search-input:focus {
  outline: none;
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}

.search-input-group {
  display: flex;
  gap: 0.75rem;
  align-items: stretch;
}

.search-input {
  flex: 1;
}

.action-buttons {
  display: flex;
  gap: 0.5rem;
  min-width: 200px;
}

.action-btn {
  padding: 10px 12px;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-size: 0.9rem;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s ease;
  white-space: nowrap;
  height: 42px;
  box-sizing: border-box;
  flex: 1;
  min-width: 0;
}

.action-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.action-btn.primary {
  background: #3b82f6;
  color: white;
  border: 2px solid #3b82f6;
}

.action-btn.primary:hover:not(:disabled) {
  background: #2563eb;
  border-color: #2563eb;
  transform: translateY(-2px);
}

.action-btn.secondary {
  background: white;
  color: #64748b;
  border: 2px solid #d1d5db;
}

.action-btn.secondary:hover {
  background: #f8fafc;
  border-color: #9ca3af;
  transform: translateY(-2px);
}

.btn-text {
  font-weight: 500;
}

.current-selection {
  margin-top: 0.75rem;
}

.selection-card {
  background: white;
  padding: 1.25rem;
  border-radius: 8px;
  border-left: 4px solid #3b82f6;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.selection-card h3 {
  margin: 0 0 0.75rem 0;
  color: #1e293b;
  font-size: 1.1rem;
}

.company-info {
  display: flex;
  gap: 1rem;
  align-items: center;
  margin-bottom: 0.5rem;
}

.company-name {
  font-weight: 600;
  color: #1e293b;
  font-size: 1rem;
}

.company-symbol {
  background: #dbeafe;
  color: #1e40af;
  padding: 4px 8px;
  border-radius: 6px;
  font-size: 0.8rem;
  font-weight: 600;
}

.search-query {
  color: #64748b;
  font-size: 0.9rem;
}

/* 新闻展示区域 - 保持不变 */
.news-display-section {
  background: white;
  padding: 1.5rem;
  border-radius: 12px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  margin-bottom: 2rem;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1.5rem;
  padding-bottom: 0.5rem;
  border-bottom: 2px solid #e2e8f0;
}

.section-header h2 {
  color: #1e293b;
  margin: 0;
  font-size: 1.5rem;
}

.news-stats {
  display: flex;
  gap: 1rem;
  font-size: 0.9rem;
  color: #64748b;
}

.stat-item {
  background: #f8fafc;
  padding: 4px 8px;
  border-radius: 4px;
}

.status-message {
  padding: 1rem;
  border-radius: 8px;
  margin-bottom: 1rem;
  font-weight: 500;
}

.status-message.info {
  background: #dbeafe;
  color: #1e40af;
  border: 1px solid #93c5fd;
}

.status-message.success {
  background: #dcfce7;
  color: #166534;
  border: 1px solid #86efac;
}

.status-message.warning {
  background: #fef3c7;
  color: #92400e;
  border: 1px solid #fcd34d;
}

.status-message.error {
  background: #fecaca;
  color: #dc2626;
  border: 1px solid #fca5a5;
}

.loading-state,
.error-state,
.empty-state,
.no-results-state {
  text-align: center;
  padding: 3rem 2rem;
  color: #64748b;
}

.spinner {
  width: 40px;
  height: 40px;
  border: 4px solid #e5e7eb;
  border-left: 4px solid #3b82f6;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin: 0 auto 1rem;
}

.loading-details {
  margin-top: 1rem;
  font-size: 0.9rem;
}

.loading-details p {
  margin: 0.25rem 0;
  color: #6b7280;
}

.error-icon,
.empty-icon,
.no-results-icon {
  font-size: 3rem;
  margin-bottom: 1rem;
}

.error-actions {
  margin-top: 1.5rem;
  display: flex;
  gap: 1rem;
  justify-content: center;
}

.retry-btn,
.mock-btn,
.reset-btn {
  padding: 8px 16px;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 0.9rem;
}

.retry-btn {
  background: #3b82f6;
  color: white;
}

.mock-btn {
  background: #8b5cf6;
  color: white;
}

.reset-btn {
  background: #f3f4f6;
  color: #374151;
  border: 1px solid #d1d5db;
}

.news-grid {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.news-card {
  background: #f8fafc;
  border-radius: 8px;
  padding: 1.5rem;
  border-left: 4px solid #3b82f6;
  transition: all 0.3s ease;
}

.news-card:hover {
  background: #f1f5f9;
  transform: translateX(4px);
}

.news-card.chinese {
  border-left-color: #ef4444;
}

.news-card.english {
  border-left-color: #10b981;
}

.news-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 1rem;
}

.news-meta {
  display: flex;
  align-items: center;
  gap: 1rem;
  font-size: 0.8rem;
  color: #64748b;
}

.news-language-badge {
  padding: 2px 8px;
  border-radius: 12px;
  font-weight: 500;
  font-size: 0.7rem;
}

.news-language-badge.chinese {
  background: #fecaca;
  color: #dc2626;
}

.news-language-badge.english {
  background: #d1fae5;
  color: #065f46;
}

.detail-btn {
  padding: 4px 8px;
  background: #e5e7eb;
  color: #374151;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 0.8rem;
}

.detail-btn:hover {
  background: #d1d5db;
}

.news-title {
  margin: 0 0 1rem 0;
  font-size: 1.1rem;
  line-height: 1.4;
}

.news-link {
  color: #1e293b;
  text-decoration: none;
  transition: color 0.3s ease;
}

.news-link:hover {
  color: #3b82f6;
}

.news-details {
  border-top: 1px solid #e5e7eb;
  padding-top: 1rem;
}

.news-description {
  margin: 0 0 1rem 0;
  color: #4b5563;
  line-height: 1.5;
}

.news-content {
  margin-bottom: 1rem;
}

.news-content p {
  margin: 0;
  color: #6b7280;
  font-size: 0.9rem;
  line-height: 1.5;
}

.news-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 1rem;
}

.news-tags {
  display: flex;
  gap: 0.5rem;
}

.news-tag {
  background: #e5e7eb;
  color: #4b5563;
  padding: 2px 8px;
  border-radius: 12px;
  font-size: 0.7rem;
}

.read-more {
  color: #3b82f6;
  text-decoration: none;
  font-size: 0.9rem;
  font-weight: 500;
}

.read-more:hover {
  text-decoration: underline;
}

.pagination-controls {
  display: flex;
  justify-content: center;
  margin-top: 2rem;
}

.load-more-btn {
  padding: 10px 20px;
  background: #3b82f6;
  color: white;
  border: none;
  border-radius: 6px;
  cursor: pointer;
}

.load-more-btn:disabled {
  background: #9ca3af;
  cursor: not-allowed;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

/* 响应式设计 */
@media (max-width: 768px) {
  .company-controls {
    grid-template-columns: 1fr;
    gap: 1rem;
  }
  
  .search-input-group {
    flex-direction: column;
    gap: 0.5rem;
  }
  
  .action-buttons {
    width: 100%;
    min-width: auto;
  }
  
  .action-btn {
    flex: 1;
    justify-content: center;
  }
  
  .section-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 1rem;
  }
  
  .news-stats {
    flex-wrap: wrap;
  }
  
  .news-footer {
    flex-direction: column;
    gap: 1rem;
    align-items: flex-start;
  }
}
</style>