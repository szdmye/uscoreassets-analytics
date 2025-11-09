<template>
  <div class="stock-analysis-container">
    <!-- 个股分析 -->
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
      </div>
    </div>
    
    <!-- 加载状态 -->
    <div v-if="isLoading && !currentStockData" class="loading">
      <div class="loading-spinner"></div>
      <p>加载中...</p>
    </div>
    
    <!-- 股票基本信息 -->
    <div class="stock-card" v-if="currentStockData">
      <!-- 公司名称、股票代码 -->
      <div class="company-header">
        <div class="company-name-container">
          <h2 class="company-name">{{ currentStockName }}</h2>
          <span class="stock-code">{{ formatStockName(currentStockName) }}</span>
        </div>
      </div>
      
      <!-- 主要数据区域 -->
      <div class="main-data-area">
        <!-- 当前价格和涨跌 -->
        <div class="price-change-card" :class="currentStockData.change >= 0 ? 'up-card' : 'down-card'">
          <div class="price-label">当前价格</div>
          <div class="price-value">{{ formatNumber(currentStockData.price) }}</div>
          <div class="change-container">
            <span class="change-value" :class="currentStockData.change >= 0 ? 'positive' : 'negative'">
              {{ currentStockData.change >= 0 ? '+' : '' }}{{ formatNumber(currentStockData.change) }}
            </span>
            <span class="change-percent" :class="currentStockData.change >= 0 ? 'positive' : 'negative'">
              ({{ currentStockData.changePercent >= 0 ? '+' : '' }}{{ formatNumber(currentStockData.changePercent) }}%)
            </span>
          </div>
        </div>
        
        <!-- 详细数据网格 -->
        <div class="detail-grid">
          <div class="detail-card">
            <div class="detail-label">今日开盘</div>
            <div class="detail-value">{{ formatNumber(currentStockData.open) }}</div>
          </div>
          <div class="detail-card">
            <div class="detail-label">今日最高</div>
            <div class="detail-value">{{ formatNumber(currentStockData.high) }}</div>
          </div>
          <div class="detail-card">
            <div class="detail-label">今日最低</div>
            <div class="detail-value">{{ formatNumber(currentStockData.low) }}</div>
          </div>
          <div class="detail-card">
            <div class="detail-label">成交量</div>
            <div class="detail-value">{{ formatVolume(currentStockData.volume) }}M</div>
          </div>
        </div>
      </div>
      
      <!-- 数据更新时间 -->
      <div class="data-time" v-if="currentStockData?.rawData?.timestamp">
        数据更新于 {{ formattedTime(currentStockData.rawData.timestamp) }}
      </div>
    </div>

    <!-- 图表加载状态 -->
    <div v-if="isLoading && currentStockData" class="charts-loading">
      <div class="loading-content">
        <div class="loading-spinner"></div>
        <p>正在加载历史数据和图表...</p>
        <div class="loading-progress">
          <div class="progress-bar">
            <div class="progress-fill" :style="{ width: loadingProgress + '%' }"></div>
          </div>
          <span class="progress-text">{{ loadingProgress }}%</span>
        </div>
      </div>
    </div>

    <!-- 图表容器 -->
    <div class="charts-container" v-if="!isLoading || historicalData.length > 0">
      <div class="charts-header">
        <h3>价格与成交量分析</h3>
        <div class="chart-controls">
          <button @click="toggleDataFilter" class="btn-toggle" :class="{ active: dataFilterEnabled }">
            {{ dataFilterEnabled ? '已启用' : '未启用' }} 异常数据过滤
          </button>
          <div class="filter-threshold" v-if="dataFilterEnabled">
            <label for="filter-threshold">过滤阈值:</label>
            <input 
              type="range" 
              id="filter-threshold" 
              v-model="filterThreshold" 
              min="5" 
              max="30" 
              step="1"
              @input="updateChart"
            >
            <span>{{ filterThreshold }}%</span>
          </div>
          <div class="chart-type-toggle">
            <button 
              @click="toggleChartType" 
              class="chart-type-btn"
              :class="{ active: showCombinedChart }"
            >
              {{ showCombinedChart ? '📈 组合图表' : '📊 分离图表' }}
            </button>
          </div>
        </div>
      </div>
      
      <!-- 组合图表 -->
      <div v-if="showCombinedChart" class="combined-chart-container">
        <div ref="combinedChart" class="chart combined-chart"></div>
      </div>
      
      <!-- 分离图表 -->
      <div v-else class="separate-charts">
        <div class="chart-container">
          <div ref="priceChart" class="chart"></div>
        </div>
        <div class="chart-container">
          <div ref="volumeChart" class="chart"></div>
        </div>
      </div>
      
      <div class="disclaimer-text">
        所有数据均采用每日同一固定时刻，以确保时间序列分析的一致性
      </div>
    </div>

    <!-- 无数据提示 -->
    <div v-if="!isLoading && historicalData.length === 0 && currentStockData" class="no-data-message">
      <div class="no-data-icon">📊</div>
      <div class="no-data-info">
        <p>暂无历史数据可供图表显示</p>
        <p class="no-data-desc">请检查API连接或稍后重试</p>
      </div>
    </div>
    
    <!-- 异常数据统计 -->
    <div class="anomaly-stats" v-if="!isLoading && anomalyCount > 0">
      <div class="anomaly-icon">⚠️</div>
      <div class="anomaly-info">
        <p>检测到 <span class="anomaly-count">{{ anomalyCount }}</span> 个异常数据点已被过滤</p>
        <p class="anomaly-threshold">当前过滤阈值: {{ filterThreshold }}%</p>
      </div>
    </div>
    
  </div>
</template>
 
<script setup>
import { ref, computed, onMounted, onBeforeUnmount, nextTick, watch } from 'vue'
import { advancedSearchStockData } from '@/services/stockDataApi';
import * as echarts from 'echarts';

// 可用股票列表
const availableStocks = [
  '3M', 'Amazon.com', 'American Express', 'Amgen', 'Apple', 'Boeing',
  'Caterpillar', 'Chevron', 'Cisco', 'Citigroup', 'Coca-Cola', 'Goldman Sachs',
  'Home Depot', 'Honeywell', 'IBM', 'JPMorgan', 'McDonald’s',
  'Microsoft', 'NVIDIA', 'Salesforce Inc', 'Sherwin-Williams',
  'Travelers', 'UnitedHealth', 'Verizon', 'Visa A', 'Walmart', 'Walt Disney',
  'Nike'
];
 
const formatStockName = (name) => {
  const shortNames = {
    '3M': 'MMM',
    'Amazon.com': 'AMZN',
    'American Express': 'AXP',
    'Amgen': 'AMGN',
    'Apple': 'AAPL',
    'Boeing': 'BA',
    'Caterpillar': 'CAT',
    'Chevron': 'CVX',
    'Cisco': 'CSCO',
    'Citigroup': 'C',
    'Coca-Cola': 'KO',
    'Goldman Sachs': 'GS',
    'Home Depot': 'HD',
    'Honeywell': 'HON',
    'IBM': 'IBM',
    'JPMorgan': 'JPM',
    'McDonald’s': 'MCD',
    'Microsoft': 'MSFT',
    'NVIDIA': 'NVDA',
    'Salesforce Inc': 'CRM',
    'Sherwin-Williams': 'SHW',
    'Travelers': 'TRV',
    'UnitedHealth': 'UNH',
    'Verizon': 'VZ',
    'Visa A': 'V',
    'Walmart': 'WMT',
    'Walt Disney': 'DIS',
    'Nike': 'NKE',
  }
  return shortNames[name] || name.slice(0, 4).toUpperCase()
}

// 选中的股票
const selectedStock = ref('Apple')
 
// 当前股票数据
const currentStockData = ref(null)
const isLoading = ref(false)
const historicalData = ref([])
const anomalyCount = ref(0)
const dataFilterEnabled = ref(true)
const filterThreshold = ref(15)
const loadingProgress = ref(0) 
const showCombinedChart = ref(true) 

// 图表相关
const priceChart = ref(null)
const volumeChart = ref(null)
const combinedChart = ref(null)
let priceChartInstance = null
let volumeChartInstance = null
let combinedChartInstance = null
const intradayData = ref([])

// 计算当前公司名称
const currentStockName = computed(() => selectedStock.value)
 
// 格式化数字
const formatNumber = (num) => {
  if (num === null || num === undefined || isNaN(num)) return 'N/A'
  return parseFloat(num).toFixed(2)
}

// 格式化成交量
const formatVolume = (volume) => {
  if (!volume) return 'N/A'
  if (volume >= 1000000) {
    return (volume / 1000000).toFixed(2) + 'M'
  } else if (volume >= 1000) {
    return (volume / 1000).toFixed(2) + 'K'
  }
  return volume.toString()
}

// 格式化日期
const formatDate = (dateString) => {
  const date = new Date(dateString)
  return date.toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit'
  })
}

const formattedTime = (timestamp) => {
  const date = new Date(timestamp)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

// 判断是否为交易日
const isTradingDay = (date) => {
  const day = date.getDay()
  return day !== 0 && day !== 6 // 0是周日，6是周六
}

// 从数据中获取当天的开盘价
const getDailyOpenPrice = (dailyData) => {
  if (!dailyData || dailyData.length === 0) return null
  // 按时间排序
  const sorted = [...dailyData].sort((a, b) => new Date(a.timestamp) - new Date(b.timestamp))
  return parseFloat(sorted[0].last)
}

// 处理历史数据，确保每天只有一个数据点
const processDailyData = (stockData) => {
  const dailyDataMap = {}
  
  stockData.forEach(item => {
    const date = new Date(item.timestamp)
    // 跳过非交易日
    if (!isTradingDay(date)) return
    
    const dateKey = date.toISOString().split('T')[0]
    
    if (!dailyDataMap[dateKey]) {
      dailyDataMap[dateKey] = []
    }
    dailyDataMap[dateKey].push(item)
  })
  
  const dailyData = []
  
  Object.keys(dailyDataMap).forEach(dateKey => {
    const items = dailyDataMap[dateKey]
    // 按时间排序
    const sortedItems = [...items].sort((a, b) => new Date(a.timestamp) - new Date(b.timestamp))
    
    // 计算当日数据
    const lastItem = sortedItems[sortedItems.length - 1]
    
    const openPrice = getDailyOpenPrice(sortedItems)
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
    
    // 计算涨跌幅
    const previousDays = Object.keys(dailyDataMap)
      .filter(key => key < dateKey)
      .sort()
    
    const previousClose = previousDays.length > 0 
      ? parseFloat(dailyDataMap[previousDays[previousDays.length - 1]][0].last)
      : null
    
    const changePercent = previousClose 
      ? ((closePrice - previousClose) / previousClose) * 100
      : 0
    
    dailyData.push({
      date: dateKey,
      open: openPrice,
      close: closePrice,
      high: highPrice,
      low: lowPrice,
      volume: totalVolume,
      changePercent: changePercent,
      timestamp: new Date(dateKey).getTime() // 添加时间戳用于排序
    })
  })
  
  return dailyData
    .sort((a, b) => a.timestamp - b.timestamp)
    .slice(-90) 
}

// 过滤异常数据
const filterAnomalyData = (data, threshold = 15) => {
  if (!data || data.length < 3) return data;
  
  const filteredData = [...data];
  let anomalies = 0;
  
  // 计算移动平均线（5日）
  const movingAvg = [];
  for (let i = 0; i < filteredData.length; i++) {
    if (i < 4) {
      // 前4天直接使用收盘价
      movingAvg.push(filteredData[i].close);
    } else {
      // 计算5日移动平均
      const sum = filteredData[i-4].close + filteredData[i-3].close + 
                 filteredData[i-2].close + filteredData[i-1].close + filteredData[i].close;
      movingAvg.push(sum / 5);
    }
  }
  
  // 检测并过滤异常值
  for (let i = 1; i < filteredData.length - 1; i++) {
    const current = filteredData[i].close;
    const prev = filteredData[i-1].close;
    const next = filteredData[i+1].close;
    const avg = movingAvg[i];
    
    // 计算与前一天和后一天的差异百分比
    const diffPrev = Math.abs((current - prev) / prev * 100);
    const diffNext = Math.abs((current - next) / next * 100);
    const diffAvg = Math.abs((current - avg) / avg * 100);
    
    // 如果差异超过阈值，标记为异常
    if (diffPrev > threshold && diffNext > threshold && diffAvg > threshold) {
      // 用前后两天的平均值替换异常值
      filteredData[i].close = (prev + next) / 2;
      filteredData[i].high = Math.max(prev, next);
      filteredData[i].low = Math.min(prev, next);
      anomalies++;
    }
  }
  
  anomalyCount.value = anomalies;
  return filteredData;
}

// 计算移动平均线
const calculateMovingAverage = (data, period = 5) => {
  const movingAverages = [];
  for (let i = 0; i < data.length; i++) {
    if (i < period - 1) {
      movingAverages.push(null);
    } else {
      const sum = data.slice(i - period + 1, i + 1).reduce((a, b) => a + b, 0);
      movingAverages.push(sum / period);
    }
  }
  return movingAverages;
}

// 初始化组合图表
const initCombinedChart = () => {
  nextTick(() => {
    if (!combinedChart.value || historicalData.value.length === 0) {
      console.warn('组合图表容器或数据未准备好');
      return;
    }
    
    // 销毁旧图表实例
    if (combinedChartInstance) {
      combinedChartInstance.dispose();
      combinedChartInstance = null;
    }
    
    // 根据过滤设置决定使用的数据
    const chartData = dataFilterEnabled.value 
      ? filterAnomalyData(historicalData.value, filterThreshold.value)
      : historicalData.value;
    
    // 确保数据有效
    if (chartData.length === 0) {
      console.warn('没有可用的图表数据');
      return;
    }
    
    console.log('初始化组合图表，数据量:', chartData.length);
    
    // 计算移动平均线
    const closePrices = chartData.map(item => item.close);
    const movingAverages = calculateMovingAverage(closePrices, 5);
    
    // 组合图表配置
    const combinedOption = {
      title: {
        text: `${currentStockName.value} - 价格与成交量分析`,
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
          type: 'cross',
          crossStyle: {
            color: '#999'
          }
        },
        formatter: params => {
          const dataIndex = params[0].dataIndex;
          const data = chartData[dataIndex];
          let html = `
            <div class="tooltip-content">
              <div class="tooltip-title">${formatDate(data.date)}</div>
              <div class="tooltip-item">开盘: $${formatNumber(data.open)}</div>
              <div class="tooltip-item">收盘: $${formatNumber(data.close)}</div>
              <div class="tooltip-item">最高: $${formatNumber(data.high)}</div>
              <div class="tooltip-item">最低: $${formatNumber(data.low)}</div>
              <div class="tooltip-item">涨跌幅: ${data.changePercent >= 0 ? '+' : ''}${formatNumber(data.changePercent)}%</div>
          `;
          
          // 添加成交量信息
          params.forEach(param => {
            if (param.seriesName === '成交量') {
              html += `<div class="tooltip-item">成交量: ${formatVolume(data.volume)}</div>`;
            }
            if (param.seriesName === '5日均线' && movingAverages[dataIndex]) {
              html += `<div class="tooltip-item">5日均线: $${formatNumber(movingAverages[dataIndex])}</div>`;
            }
          });
          
          html += `</div>`;
          return html;
        },
        backgroundColor: 'rgba(255, 255, 255, 0.95)',
        borderColor: '#e2e8f0',
        borderWidth: 1,
        borderRadius: 8,
        boxShadow: '0 4px 12px rgba(0, 0, 0, 0.15)',
        textStyle: {
          color: '#1a202c'
        }
      },
      legend: {
        data: ['收盘价', '成交量', '5日均线'],
        top: 45,
        textStyle: {
          color: '#64748b'
        }
      },
      grid: {
        left: '3%',
        right: '4%',
        bottom: '15%',
        containLabel: true
      },
      xAxis: [
        {
          type: 'category',
          data: chartData.map(item => formatDate(item.date)),
          axisPointer: {
            type: 'shadow'
          },
          axisLabel: {
            rotate: 45,
            formatter: value => {
              const date = new Date(value);
              return `${date.getMonth() + 1}/${date.getDate()}`;
            },
            color: '#64748b'
          },
          axisLine: {
            lineStyle: {
              color: '#e2e8f0'
            }
          }
        }
      ],
      yAxis: [
        {
          type: 'value',
          name: '价格',
          min: 'dataMin',
          max: 'dataMax',
          axisLabel: {
            formatter: '${value}',
            color: '#64748b'
          },
          axisLine: {
            lineStyle: {
              color: '#e2e8f0'
            }
          },
          splitLine: {
            lineStyle: {
              color: '#f7fafc'
            }
          }
        },
        {
          type: 'value',
          name: '成交量',
          axisLabel: {
            formatter: value => {
              return value >= 1000000 ? `${(value / 1000000).toFixed(0)}M` : 
                     value >= 1000 ? `${(value / 1000).toFixed(0)}K` : value;
            },
            color: '#64748b'
          },
          axisLine: {
            lineStyle: {
              color: '#e2e8f0'
            }
          },
          splitLine: {
            show: false
          }
        }
      ],
      series: [
        {
          name: '收盘价',
          type: 'line',
          data: closePrices,
          smooth: true,
          lineStyle: {
            width: 3,
            color: '#3b82f6'
          },
          itemStyle: {
            color: '#3b82f6'
          },
          areaStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: 'rgba(59, 130, 246, 0.3)' },
              { offset: 1, color: 'rgba(59, 130, 246, 0.1)' }
            ])
          },
          emphasis: {
            focus: 'series'
          }
        },
        {
          name: '成交量',
          type: 'bar',
          yAxisIndex: 1,
          data: chartData.map(item => item.volume),
          barWidth: '60%',
          itemStyle: {
            color: function(params) {
              const dataIndex = params.dataIndex;
              if (dataIndex > 0) {
                const prevClose = chartData[dataIndex - 1].close;
                const currentClose = chartData[dataIndex].close;
                return currentClose >= prevClose ? 'rgba(239, 68, 68, 0.7)' : 'rgba(16, 185, 129, 0.7)';
              }
              else if (dataIndex === 0) {
                return chartData[dataIndex].changePercent >= 0 ? 'rgba(239, 68, 68, 0.7)' : 'rgba(16, 185, 129, 0.7)';
              }
              return 'rgba(59, 130, 246, 0.7)';
            }
          },
          emphasis: {
            focus: 'series'
          }
        },
        {
          name: '5日均线',
          type: 'line',
          data: movingAverages,
          smooth: true,
          lineStyle: {
            width: 2,
            type: 'dashed',
            color: '#f59e0b'
          },
          itemStyle: {
            color: '#f59e0b'
          },
          symbol: 'none'
        }
      ],
      dataZoom: [
        {
          type: 'inside',
          start: 0,
          end: 100
        },
        {
          show: true,
          type: 'slider',
          bottom: 20,
          start: 0,
          end: 100,
          handleSize: 8,
          handleStyle: {
            color: '#3b82f6'
          }
        }
      ]
    };

    try {
      // 初始化组合图表
      combinedChartInstance = echarts.init(combinedChart.value);
      combinedChartInstance.setOption(combinedOption);
      
      // 窗口大小变化时重新调整图表大小
      window.addEventListener('resize', handleCombinedChartResize);
      
    } catch (error) {
      console.error('组合图表初始化失败:', error);
    }
  });
}

// 初始化分离图表
const initSeparateCharts = () => {
  nextTick(() => {
    if (!priceChart.value || !volumeChart.value || historicalData.value.length === 0) {
      console.warn('分离图表容器或数据未准备好');
      return;
    }
    
    // 销毁旧图表实例
    if (priceChartInstance) {
      priceChartInstance.dispose();
      priceChartInstance = null;
    }
    if (volumeChartInstance) {
      volumeChartInstance.dispose();
      volumeChartInstance = null;
    }
    
    // 根据过滤设置决定使用的数据
    const chartData = dataFilterEnabled.value 
      ? filterAnomalyData(historicalData.value, filterThreshold.value)
      : historicalData.value;
    
    // 确保数据有效
    if (chartData.length === 0) {
      console.warn('没有可用的图表数据');
      return;
    }
    
    console.log('初始化分离图表，数据量:', chartData.length);
    
    // 价格图表配置
    const priceOption = {
      title: {
        text: '三个月价格走势',
        left: 'center',
        textStyle: {
          fontSize: 14,
          fontWeight: 'normal',
          color: '#4a5568'
        }
      },
      tooltip: {
        trigger: 'axis',
        formatter: params => {
          const dataIndex = params[0].dataIndex;
          const data = chartData[dataIndex];
          return `
            <div class="tooltip-content">
              <div class="tooltip-title">${formatDate(data.date)}</div>
              <div class="tooltip-item">开盘: $${formatNumber(data.open)}</div>
              <div class="tooltip-item">收盘: $${formatNumber(data.close)}</div>
              <div class="tooltip-item">最高: $${formatNumber(data.high)}</div>
              <div class="tooltip-item">最低: $${formatNumber(data.low)}</div>
              <div class="tooltip-item">涨跌幅: ${data.changePercent >= 0 ? '+' : ''}${formatNumber(data.changePercent)}%</div>
            </div>
          `
        },
        backgroundColor: 'rgba(255, 255, 255, 0.9)',
        borderColor: '#e2e8f0',
        borderWidth: 1,
        borderRadius: 8,
        boxShadow: '0 4px 6px rgba(0, 0, 0, 0.1)',
        textStyle: {
          color: '#1a202c'
        }
      },
      grid: {
        left: '3%',
        right: '4%',
        bottom: '3%',
        containLabel: true
      },
      xAxis: {
        type: 'category',
        data: chartData.map(item => formatDate(item.date)),
        axisLabel: {
          rotate: 45,
          formatter: value => {
            const date = new Date(value)
            return `${date.getMonth() + 1}/${date.getDate()}`
          },
          color: '#64748b'
        },
        axisLine: {
          lineStyle: {
            color: '#e2e8f0'
          }
        }
      },
      yAxis: {
        type: 'value',
        scale: true,
        axisLabel: {
          formatter: '${value}',
          color: '#64748b'
        },
        axisLine: {
          lineStyle: {
            color: '#e2e8f0'
          }
        },
        splitLine: {
          lineStyle: {
            color: '#f7fafc'
          }
        }
      },
      series: [
        {
          name: '收盘价',
          type: 'line',
          data: chartData.map(item => item.close),
          smooth: true,
          lineStyle: {
            width: 2,
            color: '#3b82f6'
          },
          itemStyle: {
            color: function(params) {
              const dataIndex = params.dataIndex;
              if (dataIndex > 0) {
                const prevClose = chartData[dataIndex - 1].close;
                const currentClose = chartData[dataIndex].close;
                return currentClose >= prevClose ? '#3b82f6' : '#10b981';
              }
              return '#3b82f6';
            }
          },
          areaStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: 'rgba(59, 130, 246, 0.2)' },
              { offset: 1, color: 'rgba(59, 130, 246, 0)' } 
            ])
          }
        }
      ]
    };

    // 成交量图表配置
    const volumeOption = {
      title: {
        text: '三个月成交量',
        left: 'center',
        textStyle: {
          fontSize: 14,
          fontWeight: 'normal',
          color: '#4a5568'
        }
      },
      tooltip: {
        trigger: 'axis',
        formatter: params => {
          const dataIndex = params[0].dataIndex;
          const data = chartData[dataIndex];
          return `
            <div class="tooltip-content">
              <div class="tooltip-title">${formatDate(data.date)}</div>
              <div class="tooltip-item">成交量: ${formatVolume(data.volume)}</div>
            </div>
          `
        },
        backgroundColor: 'rgba(255, 255, 255, 0.9)',
        borderColor: '#e2e8f0',
        borderWidth: 1,
        borderRadius: 8,
        boxShadow: '0 4px 6px rgba(0, 0, 0, 0.1)',
        textStyle: {
          color: '#1a202c'
        }
      },
      grid: {
        left: '3%',
        right: '4%',
        bottom: '3%',
        containLabel: true
      },
      xAxis: {
        type: 'category',
        data: chartData.map(item => formatDate(item.date)),
        axisLabel: {
          rotate: 45,
          formatter: value => {
            const date = new Date(value)
            return `${date.getMonth() + 1}/${date.getDate()}`
          },
          color: '#64748b'
        },
        axisLine: {
          lineStyle: {
            color: '#e2e8f0'
          }
        }
      },
      yAxis: {
        type: 'value',
        axisLabel: {
          formatter: value => {
            return value >= 1000000 ? `${(value / 1000000).toFixed(0)}M` : 
                   value >= 1000 ? `${(value / 1000).toFixed(0)}K` : value
          },
          color: '#64748b'
        },
        axisLine: {
          lineStyle: {
            color: '#e2e8f0'
          }
        },
        splitLine: {
          lineStyle: {
            color: '#f7fafc'
          }
        }
      },
      series: [
        {
          name: '成交量',
          type: 'bar',
          data: chartData.map(item => item.volume),
          barWidth: '80%',
          itemStyle: {
            color: function(params) {
              const dataIndex = params.dataIndex;
              if (dataIndex > 0) {
                const prevClose = chartData[dataIndex - 1].close;
                const currentClose = chartData[dataIndex].close;
                return currentClose >= prevClose ? '#3b82f6' : '#10b981';
              }
              return '#3b82f6';
            }
          }
        }
      ]
    };

    try {
      // 初始化分离图表
      priceChartInstance = echarts.init(priceChart.value);
      priceChartInstance.setOption(priceOption);
      
      volumeChartInstance = echarts.init(volumeChart.value);
      volumeChartInstance.setOption(volumeOption);
      
      // 窗口大小变化时重新调整图表大小
      window.addEventListener('resize', handleSeparateChartsResize);
      
    } catch (error) {
      console.error('分离图表初始化失败:', error);
    }
  });
}

// 处理窗口大小变化
const handleCombinedChartResize = () => {
  combinedChartInstance?.resize();
}

const handleSeparateChartsResize = () => {
  priceChartInstance?.resize();
  volumeChartInstance?.resize();
}

// 更新图表
const updateChart = () => {
  if (showCombinedChart.value) {
    initCombinedChart();
  } else {
    initSeparateCharts();
  }
}

// 切换数据过滤
const toggleDataFilter = () => {
  dataFilterEnabled.value = !dataFilterEnabled.value;
  updateChart();
}

// 切换图表类型
const toggleChartType = () => {
  showCombinedChart.value = !showCombinedChart.value;
  nextTick(() => {
    updateChart();
  });
}

// 模拟进度更新
const updateLoadingProgress = () => {
  const interval = setInterval(() => {
    if (loadingProgress.value < 90) {
      loadingProgress.value += Math.random() * 10;
    } else {
      clearInterval(interval);
    }
  }, 200);
}

// 加载股票数据
const loadStockData = async () => {
  isLoading.value = true;
  loadingProgress.value = 0;
  historicalData.value = [];
  intradayData.value = [];
  anomalyCount.value = 0;
  
  // 开始进度更新
  updateLoadingProgress();
  
  try {
    const endTime = new Date();
    const startTime = new Date();
    const startdayTime = new Date();
    // 获取3个月的数据（大约90天）
    startTime.setMonth(startTime.getMonth() - 3);
    startdayTime.setDate(startdayTime.getDate() - 1);
    
    // 第一步：先获取当日数据（快速显示）
    const stockdayData = await advancedSearchStockData(
      selectedStock.value,
      startdayTime.toISOString(),
      endTime.toISOString()
    );

    if (!stockdayData || stockdayData.length === 0) {
      console.error('没有获取到股票数据');
      isLoading.value = false;
      return;
    }

    // 修复最新数据索引错误
    const latestData = stockdayData[stockdayData.length - 1];
    let openData = null;
    
    for (let i = stockdayData.length - 1; i >= 0; i--) {
      const currentTimestamp = new Date(stockdayData[i].timestamp);
      if (currentTimestamp.getHours() === 9 && currentTimestamp.getMinutes() >= 25 && currentTimestamp.getMinutes() <= 35) {
        openData = stockdayData[i];
        break;
      }
    }
    
    // 如果未找到开盘数据，使用第一个数据点作为备用
    if (!openData && stockdayData.length > 0) {
      openData = stockdayData[0];
      console.warn('未找到9:30的数据点，使用第一个数据点作为开盘价');
    }
    
    // 修复数据处理逻辑
    const change = parseFloat(latestData.chg || 0);
    const changeStr = latestData.chgPercent || "0%";
    const change_per = parseFloat(changeStr.replace('%', '')) || 0;
    
    // 先设置当前股票数据（立即显示）
    currentStockData.value = {
      price: parseFloat(latestData.last || 0),
      change: change,
      changePercent: change_per,
      open: openData ? parseFloat(openData.last || 0) : parseFloat(latestData.last || 0),
      high: parseFloat(latestData.high || 0),
      low: parseFloat(latestData.low || 0),
      volume: latestData.vol ? parseFloat(latestData.vol) : 0,
      time: latestData.time,
      historicalPrices: latestData.historicalPrices || [],
      rawData: {
        name: latestData.name,
        timestamp: latestData.timestamp,
        chg: parseFloat(latestData.chg || 0),
        chgPercent: latestData.chgPercent || "0%",
        vol: latestData.vol || "0"
      }
    };

    loadingProgress.value = 50;

    // 第二步：异步获取历史数据（后台加载）
    setTimeout(async () => {
      try {
        const stockData = await advancedSearchStockData(
          selectedStock.value,
          startTime.toISOString(),
          endTime.toISOString()
        );
        
        if (stockData && stockData.length > 0) {
          // 处理历史数据
          historicalData.value = processDailyData(stockData);
          console.log('历史数据处理完成:', historicalData.value.length, '天数据');
          loadingProgress.value = 100;
          
          setTimeout(() => {
            updateChart();
            isLoading.value = false;
          }, 500);
        } else {
          console.warn('没有获取到历史数据');
          loadingProgress.value = 100;
          isLoading.value = false;
        }
      } catch (error) {
        console.error('获取历史数据失败:', error);
        loadingProgress.value = 100;
        isLoading.value = false;
      }
    }, 100);
    
  } catch (error) {
    console.error('获取股票数据失败:', error);
    isLoading.value = false;
  }
}

// 监听过滤阈值变化
watch(filterThreshold, () => {
  if (dataFilterEnabled.value) {
    updateChart();
  }
});

// 组件挂载和卸载时的处理
onMounted(() => {
  loadStockData();
});

onBeforeUnmount(() => {
  if (priceChartInstance) {
    priceChartInstance.dispose();
  }
  if (volumeChartInstance) {
    volumeChartInstance.dispose();
  }
  if (combinedChartInstance) {
    combinedChartInstance.dispose();
  }
  window.removeEventListener('resize', handleCombinedChartResize);
  window.removeEventListener('resize', handleSeparateChartsResize);
});
</script>

<style scoped>
.stock-analysis-container {
  max-width: 1400px;
  margin: 0 auto;
  padding: 20px;
  font-family: 'Inter', system-ui, -apple-system, sans-serif;
  background-color: #f8fafc;
  min-height: 100vh;
}

.header-container {
  margin-bottom: 20px;
}

.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.stock-selector {
  display: flex;
  align-items: center;
}

.select-label {
  margin-right: 10px;
  font-size: 16px;
  color: #4a5568;
  font-weight: 500;
}

.stock-select {
  padding: 10px 16px;
  border-radius: 8px;
  border: 1px solid #e2e8f0;
  background-color: #ffffff;
  font-size: 16px;
  min-width: 220px;
  transition: all 0.3s ease;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
}

.stock-select:focus {
  outline: none;
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.2);
}

.loading {
  text-align: center;
  padding: 40px;
  font-size: 16px;
  color: #64748b;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.charts-loading {
  background-color: #ffffff;
  border-radius: 12px;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.05);
  padding: 3rem;
  margin-bottom: 2rem;
  text-align: center;
}

.loading-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 1rem;
}

.loading-spinner {
  width: 40px;
  height: 40px;
  border: 4px solid rgba(59, 130, 246, 0.1);
  border-left-color: #3b82f6;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.loading-progress {
  display: flex;
  align-items: center;
  gap: 1rem;
  width: 100%;
  max-width: 300px;
}

.progress-bar {
  flex: 1;
  height: 8px;
  background-color: #e2e8f0;
  border-radius: 4px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, #3b82f6, #60a5fa);
  border-radius: 4px;
  transition: width 0.3s ease;
}

.progress-text {
  font-size: 0.875rem;
  color: #64748b;
  font-weight: 500;
  min-width: 40px;
}

.stock-card {
  background-color: #ffffff;
  border-radius: 12px;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.05), 0 2px 4px -1px rgba(0, 0, 0, 0.03);
  padding: 2rem;
  margin-bottom: 2rem;
  transition: transform 0.3s ease, box-shadow 0.3s ease;
}

.stock-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 10px 15px -3px rgba(0, 0, 0, 0.08), 0 4px 6px -2px rgba(0, 0, 0, 0.04);
}

.company-header {
  margin-bottom: 1.5rem;
  padding-bottom: 1rem;
  border-bottom: 1px solid #f1f5f9;
}

.company-name-container {
  display: flex;
  align-items: center;
}

.company-name {
  font-size: 1.75rem;
  font-weight: 600;
  color: #1e293b;
  margin: 0;
}

.stock-code {
  margin-left: 0.75rem;
  font-size: 1rem;
  color: #64748b;
  background-color: #f1f5f9;
  padding: 4px 10px;
  border-radius: 12px;
}

.main-data-area {
  display: flex;
  flex-wrap: wrap;
  gap: 1.5rem;
  margin-bottom: 1.5rem;
}

.price-change-card {
  flex: 1;
  min-width: 220px;
  padding: 1.5rem;
  border-radius: 10px;
  background-color: #f8fafc;
  position: relative;
  overflow: hidden;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
}

.price-change-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 4px;
}

.price-change-card.up-card {
  border-left: 0;
}

.price-change-card.up-card::before {
  background-color: #e44e4e;
}

.price-change-card.down-card {
  border-left: 0;
}

.price-change-card.down-card::before {
  background-color: #10b981;
}

.price-label {
  font-size: 0.875rem;
  color: #64748b;
  margin-bottom: 0.75rem;
  font-weight: 500;
}

.price-value {
  font-size: 2rem;
  font-weight: 700;
  color: #1e293b;
  margin-bottom: 0.75rem;
  line-height: 1.2;
}

.change-container {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.change-value, .change-percent {
  font-size: 0.9375rem;
  font-weight: 500;
  padding: 3px 8px;
  border-radius: 4px;
}

.change-value.positive, .change-percent.positive {
  color: #c2410c;
  background-color: rgba(246, 59, 59, 0.1);
}

.change-value.negative, .change-percent.negative {
  color: #059669;
  background-color: rgba(16, 185, 129, 0.1);
}

.detail-grid {
  flex: 2;
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 1rem;
  min-width: 300px;
}

.detail-card {
  padding: 1.25rem;
  background-color: #f8fafc;
  border-radius: 10px;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
}

.detail-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.08);
}

.detail-label {
  font-size: 0.875rem;
  color: #64748b;
  margin-bottom: 0.5rem;
  font-weight: 500;
}

.detail-value {
  font-size: 1.25rem;
  font-weight: 600;
  color: #1e293b;
}

.data-time {
  text-align: right;
  font-size: 0.875rem;
  color: #94a3b8;
  margin-top: 1rem;
  padding-top: 1rem;
  border-top: 1px solid #f1f5f9;
}

/* 图表容器样式 */
.charts-container {
  margin-top: 2rem;
  background-color: #ffffff;
  border-radius: 12px;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.05), 0 2px 4px -1px rgba(0, 0, 0, 0.03);
  padding: 2rem;
  transition: transform 0.3s ease, box-shadow 0.3s ease;
}

.charts-container:hover {
  transform: translateY(-5px);
  box-shadow: 0 10px 15px -3px rgba(0, 0, 0, 0.08), 0 4px 6px -2px rgba(0, 0, 0, 0.04);
}

.charts-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1.5rem;
  padding-bottom: 1rem;
  border-bottom: 1px solid #f1f5f9;
}

.charts-container h3 {
  font-size: 1.5rem;
  font-weight: 600;
  color: #1e293b;
  margin: 0;
  text-align: left;
}

.chart-controls {
  display: flex;
  align-items: center;
  gap: 1rem;
  flex-wrap: wrap;
}

.btn-toggle {
  padding: 8px 16px;
  border-radius: 8px;
  border: none;
  font-size: 0.875rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
  display: flex;
  align-items: center;
  gap: 6px;
}

.btn-toggle.active {
  background-color: #3b82f6;
  color: white;
}

.btn-toggle:not(.active) {
  background-color: #f1f5f9;
  color: #64748b;
}

.btn-toggle:hover {
  opacity: 0.9;
  transform: translateY(-2px);
}

.filter-threshold {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  background-color: #f8fafc;
  padding: 8px 16px;
  border-radius: 8px;
}

.filter-threshold label {
  font-size: 0.875rem;
  color: #64748b;
  font-weight: 500;
}

.filter-threshold input[type="range"] {
  width: 120px;
  height: 6px;
  -webkit-appearance: none;
  appearance: none;
  background: #e2e8f0;
  border-radius: 3px;
  outline: none;
}

.filter-threshold input[type="range"]::-webkit-slider-thumb {
  -webkit-appearance: none;
  appearance: none;
  width: 16px;
  height: 16px;
  border-radius: 50%;
  background: #3b82f6;
  cursor: pointer;
  transition: all 0.2s ease;
}

.filter-threshold input[type="range"]::-webkit-slider-thumb:hover {
  transform: scale(1.2);
  background: #2563eb;
}

.filter-threshold span {
  font-size: 0.875rem;
  color: #4a5568;
  font-weight: 500;
  min-width: 30px;
  text-align: center;
}

.chart-type-toggle {
  display: flex;
  align-items: center;
}

.chart-type-btn {
  padding: 8px 16px;
  border-radius: 8px;
  border: 1px solid #e2e8f0;
  background-color: #f8fafc;
  color: #64748b;
  font-size: 0.875rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
}

.chart-type-btn.active {
  background-color: #3b82f6;
  color: white;
  border-color: #3b82f6;
}

.chart-type-btn:hover {
  transform: translateY(-2px);
}

/* 组合图表容器 */
.combined-chart-container {
  width: 100%;
  height: 500px;
  margin-bottom: 1rem;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.combined-chart {
  width: 100%;
  height: 100%;
}

/* 分离图表容器 */
.separate-charts {
  display: flex;
  flex-direction: column;
  gap: 2rem;
}

.chart-container {
  width: 100%;
  height: 400px;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
}

.chart {
  width: 100%;
  height: 100%;
}

/* 图例样式 */
.chart-legend {
  display: flex;
  justify-content: center;
  gap: 2rem;
  margin: 1rem 0;
  padding: 1rem;
  background-color: #f8fafc;
  border-radius: 8px;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.875rem;
  color: #64748b;
}

.legend-color {
  width: 20px;
  height: 3px;
  border-radius: 2px;
}

.legend-color.price-line {
  background-color: #3b82f6;
}

.legend-color.volume-bar {
  background-color: rgba(59, 130, 246, 0.7);
  height: 12px;
}

.legend-color.avg-line {
  background-color: #f59e0b;
  border-style: dashed;
}

.disclaimer-text {
  text-align: right;
  font-size: 0.75rem;
  color: #94a3b8;
  margin-top: 1rem;
  padding-top: 1rem;
  border-top: 1px solid #f1f5f9;
}

/* 无数据提示 */
.no-data-message {
  display: flex;
  align-items: center;
  gap: 1rem;
  background-color: #f8fafc;
  border-left: 4px solid #94a3b8;
  padding: 1.5rem;
  border-radius: 0 8px 8px 0;
  margin-top: 2rem;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
}

.no-data-icon {
  font-size: 2rem;
}

.no-data-info p {
  margin: 0;
  font-size: 0.875rem;
  color: #4a5568;
}

.no-data-desc {
  margin-top: 4px;
  font-size: 0.8125rem;
  color: #64748b;
}

/* 异常数据统计 */
.anomaly-stats {
  display: flex;
  align-items: center;
  gap: 1rem;
  background-color: #fff7ed;
  border-left: 4px solid #f59e0b;
  padding: 1rem 1.5rem;
  border-radius: 0 8px 8px 0;
  margin-top: -1.5rem;
  margin-bottom: 2rem;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
}

.anomaly-icon {
  font-size: 1.5rem;
}

.anomaly-info p {
  margin: 0;
  font-size: 0.875rem;
  color: #4a5568;
}

.anomaly-count {
  font-weight: 600;
  color: #c2410c;
}

.anomaly-threshold {
  margin-top: 4px;
  font-size: 0.8125rem;
  color: #64748b;
}

@media (max-width: 768px) {
  .stock-analysis-container {
    padding: 15px;
  }
  
  .header-content {
    flex-direction: column;
    align-items: flex-start;
    gap: 1rem;
  }
  
  .main-data-area {
    flex-direction: column;
  }
  
  .detail-grid {
    grid-template-columns: 1fr;
  }
  
  .charts-container {
    padding: 1.5rem;
    margin-top: 1.5rem;
  }
  
  .charts-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 1rem;
  }
  
  .chart-controls {
    width: 100%;
    justify-content: space-between;
  }
  
  .chart-container {
    height: 320px;
  }
  
  .combined-chart-container {
    height: 400px;
  }
  
  .chart-legend {
    flex-direction: column;
    gap: 0.5rem;
    align-items: center;
  }
  
  .anomaly-stats {
    padding: 0.75rem 1rem;
  }
  
  .no-data-message {
    padding: 1rem;
  }
}

@media (max-width: 480px) {
  .charts-container {
    padding: 1rem;
  }
  
  .chart-container {
    height: 280px;
  }
  
  .combined-chart-container {
    height: 350px;
  }
  
  .company-name {
    font-size: 1.5rem;
  }
  
  .price-value {
    font-size: 1.75rem;
  }
  
  .chart-controls {
    flex-direction: column;
    align-items: flex-start;
    gap: 0.75rem;
  }
}

/* 自定义ECharts提示框样式 */
::v-deep .tooltip-content {
  padding: 12px;
}

::v-deep .tooltip-title {
  font-weight: 600;
  margin-bottom: 8px;
  color: #1e293b;
  border-bottom: 1px solid #f1f5f9;
  padding-bottom: 6px;
  font-size: 14px;
}

::v-deep .tooltip-item {
  margin-bottom: 4px;
  color: #4a5568;
  font-size: 13px;
}
</style>