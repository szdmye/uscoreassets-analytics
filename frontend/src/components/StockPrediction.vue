<template>
  <div class="stock-prediction-container">
    <!-- 价格预测 -->
    <!-- 头部选择器 -->
    <div class="header-container">
      <div class="header-content">
        <div class="stock-selector">
          <label for="stock-select" class="select-label">选择股票:</label>
          <select id="stock-select" v-model="selectedStock" @change="loadStockData" class="stock-select">
            <option v-for="stock in availableStocks" :key="stock" :value="stock">
              {{ stock }}
            </option>
          </select>
        </div>
        
        <!-- 预测参数设置 -->
        <div class="prediction-controls">
          <div class="control-group">
            <label for="prediction-days">预测天数:</label>
            <select id="prediction-days" v-model="predictionDays" @change="loadStockData" class="control-select">
              <option value="7">7天</option>
              <option value="15">15天</option>
              <option value="30">30天</option>
            </select>
          </div>
        </div>
      </div>
    </div>
    
    <!-- 加载状态 -->
    <div v-if="isLoading" class="loading">
      <div class="loading-spinner"></div>
      <p>加载预测数据中...</p>
    </div>
    
    <!-- 预测结果概览 -->
    <div class="prediction-overview" v-if="predictionData && !isLoading">
      <div class="overview-header">
        <h2>{{ currentStockName }} 预测分析</h2>
        <div class="prediction-info">
          <span class="model-type">使用 {{ getModelName(selectedModel) }} 模型</span>
          <span class="prediction-period">{{ predictionDays }}天预测</span>
          <span class="last-updated" v-if="lastUpdated">更新: {{ lastUpdated }}</span>
        </div>
      </div>
      
      <div class="overview-cards">
        <div class="overview-card">
          <div class="card-label">当前价格</div>
          <div class="card-value">${{ formatNumber(currentPrice) }}</div>
        </div>
        
        <div class="overview-card">
          <div class="card-label">预测价格</div>
          <div class="card-value" :class="getPredictionTrendClass()">
            ${{ formatNumber(predictedPrice) }}
          </div>
        </div>
        
        <div class="overview-card">
          <div class="card-label">预期涨跌</div>
          <div class="card-value" :class="getPredictionTrendClass()">
            {{ priceChange >= 0 ? '+' : '' }}{{ formatNumber(priceChange) }} ({{ priceChange >= 0 ? '+' : '' }}{{ formatNumber(priceChangePercent) }}%)
          </div>
        </div>
        
        <div class="overview-card">
          <div class="card-label">预测准确率</div>
          <div class="card-value">{{ accuracyRate }}%</div>
        </div>
      </div>
    </div>
    
    <!-- 预测图表 -->
    <div class="prediction-charts" v-if="predictionData && !isLoading">
      <div class="charts-header">
        <h3>价格预测趋势</h3>
        <div class="chart-controls">
        </div>
      </div>
      
      <div class="chart-container">
        <div ref="predictionChart" class="prediction-chart"></div>
      </div>
      
      <!-- 预测统计 -->
      <div class="prediction-stats">
        <div class="stats-grid">
          <div class="stat-card">
            <div class="stat-label">模型置信度</div>
            <div class="stat-value">{{ confidenceLevel }}%</div>
            <div class="stat-desc">预测可靠性</div>
          </div>
          
          <div class="stat-card">
            <div class="stat-label">波动率预测</div>
            <div class="stat-value">{{ volatility }}%</div>
            <div class="stat-desc">预期价格波动</div>
          </div>
          
          <div class="stat-card">
            <div class="stat-label">风险等级</div>
            <div class="stat-value" :class="getRiskLevelClass()">{{ riskLevel }}</div>
            <div class="stat-desc">投资风险评估</div>
          </div>
          
          <div class="stat-card">
            <div class="stat-label">数据周期</div>
            <div class="stat-value">{{ displayDays }}天</div>
            <div class="stat-desc">显示数据范围</div>
          </div>
        </div>
      </div>
    </div>
    
    <!-- 无数据提示 -->
    <div v-if="!isLoading && !predictionData && !errorMessage" class="no-prediction-message">
      <div class="no-prediction-icon">📈</div>
      <div class="no-prediction-info">
        <h3>正在加载预测数据</h3>
        <p>请稍候...</p>
      </div>
    </div>

    <!-- 错误提示 -->
    <div v-if="errorMessage" class="error-message">
      <div class="error-icon">⚠️</div>
      <div class="error-info">
        <h3>加载失败</h3>
        <p>{{ errorMessage }}</p>
        <button @click="retryLoad" class="retry-button">重试</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, nextTick, watch } from 'vue'
import { advancedSearchStockData } from '@/services/stockDataApi';
import { getStockPrediction, getAvailableCompanies } from '@/services/stockPredictionService';
import * as echarts from 'echarts';

// 响应式数据
const selectedStock = ref('Apple')
const predictionDays = ref('7')
const selectedModel = ref('lstm')
const isLoading = ref(false)
const predictionData = ref(null)
const historicalData = ref([])
const errorMessage = ref('')
const availableStocks = ref([])
const showFullHistory = ref(false)
const displayDays = ref(30)
const lastUpdated = ref('')

// 图表实例
const predictionChart = ref(null)
let predictionChartInstance = null

// 计算属性
const currentStockName = computed(() => selectedStock.value)
const currentPrice = computed(() => predictionData.value?.currentPrice || 0)
const predictedPrice = computed(() => predictionData.value?.predictedPrice || 0)
const priceChange = computed(() => predictionData.value?.priceChange || 0)
const priceChangePercent = computed(() => predictionData.value?.priceChangePercent || 0)
const confidenceLevel = computed(() => predictionData.value?.confidenceLevel || 0)
const accuracyRate = computed(() => predictionData.value?.accuracyRate || 0)
const volatility = computed(() => predictionData.value?.volatility || 0)
const riskLevel = computed(() => predictionData.value?.riskLevel || '')

const formatNumber = (num) => {
  if (num === null || num === undefined || isNaN(num)) return 'N/A'
  return parseFloat(num).toFixed(2)
}

const formatDate = (dateString) => {
  const date = new Date(dateString)
  return date.toLocaleDateString('zh-CN', {
    month: '2-digit',
    day: '2-digit'
  })
}

const getModelName = (model) => {
  const modelNames = {
    'arima': 'ARIMA',
    'lstm': 'LSTM神经网络',
    'prophet': 'Prophet'
  }
  return modelNames[model] || model
}

const getPredictionTrendClass = () => {
  return priceChange.value >= 0 ? 'positive' : 'negative'
}

const getRiskLevelClass = () => {
  const riskLevel = predictionData.value?.riskLevel || ''
  if (riskLevel.includes('低')) return 'low-risk'
  if (riskLevel.includes('中')) return 'medium-risk'
  if (riskLevel.includes('高')) return 'high-risk'
  return ''
}

// 重试加载
const retryLoad = () => {
  errorMessage.value = ''
  loadStockData()
}

// 加载股票数据和预测数据
const loadStockData = async () => {
  isLoading.value = true
  errorMessage.value = ''
  
  try {
    console.log(`正在加载 ${selectedStock.value} 的${predictionDays.value}天预测数据...`)
    
    // 直接从文件获取预测数据
    const filePredictionData = await getStockPrediction(
      selectedStock.value, 
      parseInt(predictionDays.value)
    )
    
    if (!filePredictionData) {
      throw new Error('未能从预测文件获取数据')
    }
    
    console.log('从文件获取的预测数据:', filePredictionData)
    
    // 获取历史数据用于图表显示
    const endTime = new Date()
    const startTime = new Date()
    startTime.setDate(startTime.getDate() - 90)
    
    const stockData = await advancedSearchStockData(
      selectedStock.value,
      startTime.toISOString(),
      endTime.toISOString()
    )
    
    let processedHistoricalData = []
    if (stockData && stockData.length > 0) {
      processedHistoricalData = processHistoricalData(stockData)
    } else {
      console.warn('未能获取到历史数据，将仅显示预测数据')
    }
    
    historicalData.value = processedHistoricalData
    
    predictionData.value = {
      // 从文件读取的核心预测数据
      currentPrice: filePredictionData.currentPrice,
      predictedPrice: filePredictionData.predictedPrice,
      predictionDates: filePredictionData.predictionDates,
      predictedPrices: filePredictionData.predictedPrices,
      confidenceLevel: filePredictionData.confidenceLevel,
      accuracyRate: filePredictionData.accuracyRate,
      volatility: filePredictionData.volatility,
      riskLevel: filePredictionData.riskLevel,
      modelType: filePredictionData.modelType || 'LSTM神经网络',
      
      // 计算字段
      priceChange: filePredictionData.priceChange || (filePredictionData.predictedPrice - filePredictionData.currentPrice),
      priceChangePercent: filePredictionData.priceChangePercent || 
        ((filePredictionData.predictedPrice - filePredictionData.currentPrice) / filePredictionData.currentPrice) * 100,
      
      // 历史数据用于图表
      historicalData: processedHistoricalData.slice(-displayDays.value),
      
      // 元数据
      lastUpdated: filePredictionData.lastUpdated,
      companyName: filePredictionData.companyName
    }
    
    lastUpdated.value = filePredictionData.lastUpdated || ''
    
    console.log('最终使用的预测数据:', predictionData.value)
    
    // 初始化图表
    nextTick(() => {
      initPredictionChart()
    })
    
  } catch (error) {
    console.error('加载预测数据失败:', error)
    errorMessage.value = `加载预测数据失败: ${error.message}`
    predictionData.value = null
  } finally {
    isLoading.value = false
  }
}

// 处理历史数据
const processHistoricalData = (stockData) => {
  const dailyDataMap = {}
  
  stockData.forEach(item => {
    const date = new Date(item.timestamp)
    const dateKey = date.toISOString().split('T')[0]
    
    if (!dailyDataMap[dateKey]) {
      dailyDataMap[dateKey] = []
    }
    dailyDataMap[dateKey].push(item)
  })
  
  const dailyData = []
  
  Object.keys(dailyDataMap).forEach(dateKey => {
    const items = dailyDataMap[dateKey]
    const sortedItems = [...items].sort((a, b) => new Date(a.timestamp) - new Date(b.timestamp))
    
    const lastItem = sortedItems[sortedItems.length - 1]
    
    // 计算当日数据
    const openPrice = parseFloat(sortedItems[0].last)
    const closePrice = parseFloat(lastItem.last)
    
    let highPrice = closePrice
    let lowPrice = closePrice
    let totalVolume = 0
    
    sortedItems.forEach(item => {
      const price = parseFloat(item.last)
      highPrice = Math.max(highPrice, price, parseFloat(item.high))
      lowPrice = Math.min(lowPrice, price, parseFloat(item.low))
      totalVolume += parseFloat(item.vol)
    })
    
    dailyData.push({
      date: dateKey,
      open: openPrice,
      close: closePrice,
      high: highPrice,
      low: lowPrice,
      volume: totalVolume,
      timestamp: new Date(dateKey).getTime()
    })
  })
  
  return dailyData.sort((a, b) => a.timestamp - b.timestamp)
}

// 初始化可用股票列表
const initAvailableStocks = async () => {
  try {
    console.log('正在从预测文件加载可用股票列表...')
    const stocks = await getAvailableCompanies()
    availableStocks.value = stocks
    
    console.log(`从文件加载的股票列表成功，共 ${availableStocks.value.length} 个股票`)
    
    if (availableStocks.value.length > 0 && !availableStocks.value.includes(selectedStock.value)) {
      selectedStock.value = availableStocks.value[0]
      console.log(`自动切换到可用股票: ${selectedStock.value}`)
    }
    
  } catch (error) {
    console.error('从文件加载股票列表失败:', error)
    errorMessage.value = `加载股票列表失败: ${error.message}`
    
    availableStocks.value = [
      '3M', 'Amazon.com', 'American Express', 'Amgen', 'Apple', 'Boeing',
      'Caterpillar', 'Chevron', 'Cisco', 'Citigroup', 'Coca-Cola', 'Goldman Sachs',
      'Home Depot', 'Honeywell', 'IBM', 'JPMorgan', 'McDonald\'s',
      'Microsoft', 'NVIDIA', 'Salesforce Inc', 'Sherwin-Williams',
      'Travelers', 'UnitedHealth', 'Verizon', 'Visa A', 'Walmart', 'Walt Disney',
      'Nike'
    ]
    console.log('使用默认股票列表作为后备')
  }
}

// 初始化预测图表
const initPredictionChart = () => {
  nextTick(() => {
    if (!predictionChart.value || !predictionData.value) {
      console.warn('预测图表容器或数据未准备好')
      return
    }
    
    // 销毁旧图表实例
    if (predictionChartInstance) {
      predictionChartInstance.dispose()
      predictionChartInstance = null
    }
    
    try {
      predictionChartInstance = echarts.init(predictionChart.value)
      updatePredictionChart()
      
      window.addEventListener('resize', handlePredictionChartResize)
      
    } catch (error) {
      console.error('预测图表初始化失败:', error)
    }
  })
}

// 更新预测图表
const updatePredictionChart = () => {
  if (!predictionChartInstance || !predictionData.value) return
  
  const { historicalData: chartHistoricalData, predictionDates, predictedPrices } = predictionData.value
  
  // 准备图表数据
  const displayHistoricalData = showFullHistory.value ? 
    chartHistoricalData : 
    chartHistoricalData.slice(-displayDays.value)
  
  const historicalDates = displayHistoricalData.map(item => formatDate(item.date))
  const historicalPrices = displayHistoricalData.map(item => item.close)
  
  const allDates = [...historicalDates, ...predictionDates.map(date => formatDate(date))]
  
  // 图表配置
  const option = {
    title: {
      text: `${currentStockName.value} 价格预测 (LSTM模型)`,
      left: 'center',
      textStyle: {
        fontSize: 16,
        fontWeight: 'bold',
        color: '#1e293b'
      }
    },
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'cross'
      },
      formatter: (params) => {
        if (params.length === 0) return ''
        
        const param = params[0]
        let html = `<div class="tooltip-content">`
        html += `<div class="tooltip-title">${param.name}</div>`
        
        const isHistoricalData = param.dataIndex < historicalPrices.length
        const isPredictionData = param.dataIndex >= historicalPrices.length
        
        if (isHistoricalData) {
          html += `<div class="tooltip-item">历史价格: $${formatNumber(param.value)}</div>`
        } else if (isPredictionData) {
          html += `<div class="tooltip-item">LSTM预测: $${formatNumber(param.value)}</div>`
        }
        
        html += `</div>`
        return html
      }
    },
    legend: {
      data: ['历史价格', 'LSTM预测价格'],
      top: 35
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '12%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: allDates,
      axisLabel: {
        rotate: 45,
        formatter: (value, index) => {
          if (index % Math.ceil(allDates.length / 10) === 0 || index === allDates.length - 1) {
            return value
          }
          return ''
        }
      }
    },
    yAxis: {
      type: 'value',
      name: '价格 ($)',
      axisLabel: {
        formatter: '${value}'
      },
      scale: true
    },
    series: [
      {
        name: '历史价格',
        type: 'line',
        data: historicalPrices,
        smooth: true,
        lineStyle: {
          width: 2,
          color: '#3b82f6'
        },
        itemStyle: {
          color: '#3b82f6'
        },
        symbol: 'circle',
        symbolSize: 4
      },
      {
        name: 'LSTM预测价格',
        type: 'line',
        data: new Array(historicalPrices.length).fill(null).concat(predictedPrices),
        smooth: true,
        lineStyle: {
          width: 3,
          color: '#f59e0b',
          type: 'dashed'
        },
        itemStyle: {
          color: '#f59e0b'
        },
        symbol: 'circle',
        symbolSize: 6,
        markLine: {
          data: [
            {
              name: '预测起点',
              xAxis: historicalPrices.length - 1,
              lineStyle: {
                type: 'solid',
                color: '#94a3b8',
                width: 1
              },
              label: {
                formatter: '预测开始',
                position: 'insideEndTop'
              }
            }
          ],
          symbol: 'none'
        }
      }
    ]
  }
  
  predictionChartInstance.setOption(option)
}

// 处理窗口大小变化
const handlePredictionChartResize = () => {
  predictionChartInstance?.resize()
}

// 监听股票选择和预测天数变化
watch([selectedStock, predictionDays], () => {
  loadStockData()
})

// 组件挂载时自动加载数据
onMounted(async () => {
  await initAvailableStocks()
  loadStockData()
})

onBeforeUnmount(() => {
  if (predictionChartInstance) {
    predictionChartInstance.dispose()
  }
  window.removeEventListener('resize', handlePredictionChartResize)
})
</script>

<style scoped>
.stock-prediction-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 15px;
  font-family: 'Inter', system-ui, -apple-system, sans-serif;
  background-color: #f8fafc;
  min-height: 100vh;
}

.header-container {
  margin-bottom: 15px;
}

.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 15px;
  background: white;
  padding: 15px;
  border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.stock-selector {
  display: flex;
  align-items: center;
}

.select-label {
  margin-right: 8px;
  font-size: 14px;
  color: #4a5568;
  font-weight: 500;
}

.stock-select {
  padding: 8px 12px;
  border-radius: 6px;
  border: 1px solid #e2e8f0;
  background-color: #ffffff;
  font-size: 14px;
  min-width: 180px;
}

.prediction-controls {
  display: flex;
  align-items: center;
  gap: 12px;
}

.control-group {
  display: flex;
  align-items: center;
  gap: 6px;
}

.control-group label {
  font-size: 14px;
  color: #4a5568;
  font-weight: 500;
}

.control-select {
  padding: 6px 10px;
  border-radius: 6px;
  border: 1px solid #e2e8f0;
  background-color: #ffffff;
  font-size: 14px;
  min-width: 80px;
}

.loading {
  text-align: center;
  padding: 30px;
  font-size: 14px;
  color: #64748b;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.loading-spinner {
  width: 30px;
  height: 30px;
  border: 3px solid rgba(59, 130, 246, 0.1);
  border-left-color: #3b82f6;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-bottom: 10px;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.prediction-overview {
  background-color: #ffffff;
  border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  padding: 1.5rem;
  margin-bottom: 1.5rem;
}

.overview-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1rem;
  padding-bottom: 1rem;
  border-bottom: 1px solid #f1f5f9;
}

.overview-header h2 {
  font-size: 1.25rem;
  font-weight: 600;
  color: #1e293b;
  margin: 0;
}

.prediction-info {
  display: flex;
  gap: 10px;
  font-size: 0.75rem;
  color: #64748b;
  flex-wrap: wrap;
}

.prediction-info span {
  background-color: #f1f5f9;
  padding: 3px 8px;
  border-radius: 10px;
}

.overview-cards {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 0.75rem;
}

.overview-card {
  padding: 1rem;
  background-color: #f8fafc;
  border-radius: 6px;
  text-align: center;
}

.card-label {
  font-size: 0.75rem;
  color: #64748b;
  margin-bottom: 0.5rem;
  font-weight: 500;
}

.card-value {
  font-size: 1.125rem;
  font-weight: 600;
  color: #1e293b;
}

.card-value.positive {
  color: #e44e4e;
}

.card-value.negative {
  color: #10b981;
}

.prediction-charts {
  background-color: #ffffff;
  border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  padding: 1.5rem;
  margin-bottom: 1.5rem;
}

.charts-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1rem;
  padding-bottom: 1rem;
  border-bottom: 1px solid #f1f5f9;
}

.charts-header h3 {
  font-size: 1.125rem;
  font-weight: 600;
  color: #1e293b;
  margin: 0;
}

.chart-controls {
  display: flex;
  gap: 10px;
}

.zoom-button {
  padding: 6px 12px;
  background-color: #3b82f6;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 0.875rem;
}

.zoom-button:hover {
  background-color: #2563eb;
}

.chart-container {
  width: 100%;
  height: 400px;
  margin-bottom: 1.5rem;
}

.prediction-chart {
  width: 100%;
  height: 100%;
}

.prediction-stats {
  margin-top: 1.5rem;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 0.75rem;
}

.stat-card {
  padding: 1rem;
  background-color: #f8fafc;
  border-radius: 6px;
  text-align: center;
}

.stat-label {
  font-size: 0.75rem;
  color: #64748b;
  margin-bottom: 0.5rem;
  font-weight: 500;
}

.stat-value {
  font-size: 1.25rem;
  font-weight: 600;
  color: #1e293b;
  margin-bottom: 0.25rem;
}

.stat-value.low-risk {
  color: #10b981;
}

.stat-value.medium-risk {
  color: #f59e0b;
}

.stat-value.high-risk {
  color: #e44e4e;
}

.stat-desc {
  font-size: 0.7rem;
  color: #94a3b8;
}

.no-prediction-message {
  display: flex;
  align-items: center;
  gap: 1rem;
  background-color: #f8fafc;
  border-left: 4px solid #94a3b8;
  padding: 1.5rem;
  border-radius: 0 6px 6px 0;
  margin-bottom: 1.5rem;
}

.no-prediction-icon {
  font-size: 2rem;
}

.no-prediction-info h3 {
  margin: 0 0 0.25rem 0;
  color: #4a5568;
  font-size: 1.125rem;
}

.no-prediction-info p {
  margin: 0;
  color: #64748b;
  font-size: 0.875rem;
}

.error-message {
  display: flex;
  align-items: center;
  gap: 1rem;
  background-color: #fef2f2;
  border-left: 4px solid #dc2626;
  padding: 1.5rem;
  border-radius: 0 6px 6px 0;
  margin-bottom: 1.5rem;
}

.error-icon {
  font-size: 2rem;
}

.error-info h3 {
  margin: 0 0 0.5rem 0;
  color: #dc2626;
  font-size: 1.125rem;
}

.error-info p {
  margin: 0 0 0.75rem 0;
  color: #7f1d1d;
  font-size: 0.875rem;
}

.retry-button {
  background-color: #dc2626;
  color: white;
  border: none;
  padding: 0.5rem 1rem;
  border-radius: 4px;
  cursor: pointer;
  font-size: 0.875rem;
}

.retry-button:hover {
  background-color: #b91c1c;
}

@media (max-width: 768px) {
  .stock-prediction-container {
    padding: 10px;
  }
  
  .header-content {
    flex-direction: column;
    align-items: flex-start;
    gap: 1rem;
  }
  
  .overview-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 0.75rem;
  }
  
  .charts-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 0.75rem;
  }
  
  .overview-cards {
    grid-template-columns: repeat(2, 1fr);
  }
  
  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  
  .chart-container {
    height: 300px;
  }
  
  .prediction-info {
    flex-wrap: wrap;
  }
}

@media (max-width: 480px) {
  .overview-cards {
    grid-template-columns: 1fr;
  }
  
  .stats-grid {
    grid-template-columns: 1fr;
  }
  
  .chart-container {
    height: 250px;
  }
}
</style>