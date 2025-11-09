<template>
  <div class="stock-chart-container">
    <!-- 市场概况 -->
    <div class="market-overview">
      <div class="overview-card total-volume-card">
        <div class="overview-content">
          <div class="overview-icon">
            <i class="bi bi-cash-coin" aria-hidden="true"></i>
          </div>
          <div class="overview-text">
            <h3 class="overview-title">当日成交总额</h3>
            <p class="overview-value">{{ formatCurrency(marketData.totalVolume) }}</p>
          </div>
        </div>
      </div>
      
      <div class="overview-card advancing-stocks-card">
        <div class="overview-content">
          <div class="overview-icon">
            <i class="bi bi-arrow-up-circle" aria-hidden="true"></i>
          </div>
          <div class="overview-text">
            <h3 class="overview-title">上涨股票</h3>
            <p class="overview-value">{{ marketData.advancingStocks }}</p>
          </div>
        </div>
      </div>
      
      <div class="overview-card declining-stocks-card">
        <div class="overview-content">
          <div class="overview-icon">
            <i class="bi bi-arrow-down-circle" aria-hidden="true"></i>
          </div>
          <div class="overview-text">
            <h3 class="overview-title">下跌股票</h3>
            <p class="overview-value">{{ marketData.decliningStocks }}</p>
          </div>
        </div>
      </div>
      
      <div class="overview-card volatility-card">
        <div class="overview-content">
          <div class="overview-icon">
            <i class="bi bi-graph-up" aria-hidden="true"></i>
          </div>
          <div class="overview-text">
            <h3 class="overview-title">市场波动率</h3>
            <p class="overview-value" :class="isVolatilityGood ? 'positive' : 'negative'">
              {{ marketData.volatility.toFixed(2) }}%
            </p>
          </div>
        </div>
      </div>
    </div>
    
    <!-- 加载状态 -->
    <div v-if="isLoading" class="loading-container">
      <div class="spinner"></div>
      <p class="loading-text">加载中...</p>
    </div>
    
    <!-- 错误提示 -->
    <div v-if="error" class="error-container">
      <div class="error-icon">
        <i class="fa fa-exclamation-triangle" aria-hidden="true"></i>
      </div>
      <p class="error-message">{{ error }}</p>
    </div>
    
    <!-- 成交量图表 -->
    <div ref="volumeChartContainer" class="chart-container volume-chart"></div>
    
    <!-- 成交量热力图和行业雷达图 -->
    <div class="chart-grid">
      <div ref="volumeHeatmapContainer" class="chart-container heatmap-chart"></div>
      <div ref="radarContainer" class="chart-container radar-chart"></div>
    </div>
    
    <!-- 成交量趋势面积图和成交量与涨跌幅散点图 -->
    <div class="chart-grid">
      <div ref="areaContainer" class="chart-container area-chart"></div>
      <div ref="scatterContainer" class="chart-container scatter-chart"></div>
    </div>
    
    <!-- 排名组件 -->
    <div v-if="stockData.length > 0" class="ranking-container">
      <h2 class="ranking-title">股票数据排名</h2>
      <div class="ranking-grid">
        <!-- 成交量排名 -->
        <div class="ranking-card volume-ranking">
          <div class="ranking-card-header">
            <i class="fa fa-line-chart" aria-hidden="true"></i>
            <h3>成交量排名</h3>
          </div>
          <ul class="ranking-list">
            <li v-for="(stock, index) in volumeRanking" :key="stock.name" :class="{'first-place': index === 0, 'second-place': index === 1, 'third-place': index === 2}">
              <span class="rank-badge">{{ index + 1 }}</span>
              <div class="stock-info">
                <span class="stock-name">{{ stock.name }}</span>
                <span class="stock-symbol">{{ stock.symbol }}</span>
              </div>
              <span class="stock-value">{{ formatVolume(stock.volume) }}</span>
            </li>
          </ul>
        </div>
        
        <!-- 涨跌值排名 -->
        <div class="ranking-card change-value-ranking">
          <div class="ranking-card-header">
            <i class="fa fa-area-chart" aria-hidden="true"></i>
            <h3>涨跌值排名</h3>
          </div>
          <ul class="ranking-list">
            <li v-for="(stock, index) in changeValueRanking" :key="stock.name" :class="{'first-place': index === 0, 'second-place': index === 1, 'third-place': index === 2}">
              <span class="rank-badge">{{ index + 1 }}</span>
              <div class="stock-info">
                <span class="stock-name">{{ stock.name }}</span>
                <span class="stock-symbol">{{ stock.symbol }}</span>
              </div>
              <span class="stock-value" :class="stock.change >= 0 ? 'positive' : 'negative'">
                {{ stock.change >= 0 ? '+' : '' }}{{ stock.change.toFixed(2) }}
              </span>
            </li>
          </ul>
        </div>
        
        <!-- 涨跌幅排名 -->
        <div class="ranking-card change-percent-ranking">
          <div class="ranking-card-header">
            <i class="fa fa-pie-chart" aria-hidden="true"></i>
            <h3>涨跌幅排名</h3>
          </div>
          <ul class="ranking-list">
            <li v-for="(stock, index) in changePercentRanking" :key="stock.name" :class="{'first-place': index === 0, 'second-place': index === 1, 'third-place': index === 2}">
              <span class="rank-badge">{{ index + 1 }}</span>
              <div class="stock-info">
                <span class="stock-name">{{ stock.name }}</span>
                <span class="stock-symbol">{{ stock.symbol }}</span>
              </div>
              <span class="stock-value" :class="stock.changePercent >= 0 ? 'positive' : 'negative'">
                {{ stock.changePercent >= 0 ? '+' : '' }}{{ stock.changePercent.toFixed(2) }}%
              </span>
            </li>
          </ul>
        </div>
      </div>
    </div>
    
    <!-- 涨跌值和涨跌率图表 -->
    <div class="chart-grid">
      <div ref="changeValueChartContainer" class="chart-container change-value-chart"></div>
      <div ref="changePercentChartContainer" class="chart-container change-percent-chart"></div>
    </div>
  </div>
</template>

<script>
import { ref, onMounted, watch, computed } from 'vue';
import { advancedSearchStockData } from '@/services/stockDataApi';
import * as echarts from 'echarts';

export default {
  name: 'StockComparisonTable',
  setup() {
    const stockData = ref([]);
    const isLoading = ref(false);
    const error = ref(null);
    const volumeChartContainer = ref(null);
    const changeValueChartContainer = ref(null);
    const changePercentChartContainer = ref(null);
    const volumeHeatmapContainer = ref(null);
    const radarContainer = ref(null);
    const areaContainer = ref(null);
    const scatterContainer = ref(null);
    
    let volumeChartInstance = null;
    let changeValueChartInstance = null;
    let changePercentChartInstance = null;
    let volumeHeatmapInstance = null;
    let radarInstance = null;
    let areaInstance = null;
    let scatterInstance = null;
    
    const Colors = {
      '1': '#5470C6', // 蓝色
      '2': '#91CC75', // 绿色
      '3': '#FAC858', // 黄色
      '4': '#EE6666', // 红色
      '5': '#73C0DE', // 青色
      '6': '#3BA272', // 深绿色
      '7': '#9A60B4', // 紫色
      '8': '#FC8452', // 橙色
      '9': '#EA7CCC', // 粉紫色
      '10': '#57A350' // 青绿色
    };
    
    // 市场概况数据
    const marketData = ref({
      totalVolume: 0,
      volumeChange: 0,
      advancingStocks: 0,
      advancingPercent: 0,
      decliningStocks: 0,
      decliningPercent: 0,
      marketSentiment: '',
      sentimentValue: 0,
      volatility: 0
    });
    
    // 行业分类
    const industryCompanies = {
      '科技': ['Amazon.com', 'Apple', 'Cisco', 'Microsoft', 'NVIDIA', 'Salesforce Inc', 'IBM'],
      '金融': ['American Express', 'Citigroup', 'Goldman Sachs', 'JPMorgan', 'Visa A', 'Travelers'],
      '工业': ['3M', 'Boeing', 'Caterpillar', 'Honeywell', 'Sherwin-Williams', 'Chevron', 'Verizon'],
      '消费': ['Coca-Cola', 'McDonald’s', 'Walmart', 'Nike', 'Walt Disney', 'Home Depot'],
      '医药': ['Amgen', 'UnitedHealth']
    };

    // 可用股票列表
    const availableStocks = Object.values(industryCompanies).flat();

    // 格式化成交量显示
    const formatVolume = (volume) => {
      if (volume >= 1000000) {
        return (volume / 1000000).toFixed(2) + 'M';
      } else if (volume >= 1000) {
        return (volume / 1000).toFixed(2) + 'K';
      }
      if(volume - 0.001 > 0){
        return volume.toFixed(2);
      }
      return volume.toString();
    };

    // 格式化货币显示
    const formatCurrency = (value) => {
      if (value >= 1000000000) {
        return '$' + (value / 1000000000).toFixed(2) + 'B';
      } else if (value >= 1000000) {
        return '$' + (value / 1000000).toFixed(2) + 'M';
      } else if (value >= 1000) {
        return '$' + (value / 1000).toFixed(2) + 'K';
      }
      return '$' + value.toFixed(2);
    };

    // 获取市场情绪颜色
    const getSentimentColor = () => {
      const value = marketData.value.sentimentValue;
      if (value < 30) return '#ef4444'; // 红色 - 极度悲观
      if (value < 50) return '#f59e0b'; // 橙色 - 悲观
      if (value < 70) return '#10b981'; // 绿色 - 中性
      return '#22c55e'; // 深绿色 - 乐观乐观
    };

    // 股票代码映射
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
        'Nike': 'NKE'
      };
      return shortNames[name] || name.slice(0, 4).toUpperCase();
    };

    // 解析成交量字符串
    const parseVolume = (volumeStr) => {
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
    };

    // 计算市场波动率
    const calculateVolatility = (stocks) => {
      const changes = stocks.map(stock => stock.changePercent);
      const mean = changes.reduce((sum, val) => sum + val, 0) / changes.length;
      const squaredDiffs = changes.map(val => Math.pow(val - mean, 2));
      const variance = squaredDiffs.reduce((sum, val) => sum + val, 0) / changes.length;
      const volatility = Math.sqrt(variance);
      
      const thresholdHigh = 2.5;
      const thresholdLow = 1.5;
      
      let adjustedVolatility = volatility;
      
      if (adjustedVolatility > thresholdHigh) {
        adjustedVolatility = thresholdHigh + (adjustedVolatility - thresholdHigh) * 0.3;
      } else if (adjustedVolatility < thresholdLow) {
        adjustedVolatility = thresholdLow - (thresholdLow - adjustedVolatility) * 0.5;
      }
      
      return adjustedVolatility;
    };

    // 生成市场数据
    const generateMarketData = (stocks) => {
      const totalVolume = stocks.reduce((sum, stock) => sum + stock.volume, 0);
      
      const advancingStocks = stocks.filter(stock => stock.changePercent > 0).length;
      const decliningStocks = stocks.filter(stock => stock.changePercent < 0).length;
      const totalStocks = stocks.length;
      
      const advancingPercent = (advancingStocks / totalStocks) * 100;
      const decliningPercent = (decliningStocks / totalStocks) * 100;
      
      let sentimentValue = 50;
      let marketSentiment = '中性';
      
      if (advancingPercent > 65) {
        sentimentValue = 85;
        marketSentiment = '极度乐观';
      } else if (advancingPercent > 55) {
        sentimentValue = 70;
        marketSentiment = '乐观';
      } else if (decliningPercent > 65) {
        sentimentValue = 15;
        marketSentiment = '极度悲观';
      } else if (decliningPercent > 55) {
        sentimentValue = 30;
        marketSentiment = '悲观';
      }
      
      const volumeChange = (Math.random() * 10 - 3).toFixed(2);
      const volatility = calculateVolatility(stocks);
      
      return {
        totalVolume,
        volumeChange: parseFloat(volumeChange),
        advancingStocks,
        advancingPercent,
        decliningStocks,
        decliningPercent,
        marketSentiment,
        sentimentValue,
        volatility
      };
    };

    // 判断波动率是否良好
    const isVolatilityGood = computed(() => {
      return marketData.value.volatility < 2.0;
    });

    // 初始化成交量图表
    const initVolumeChart = () => {
      if (!volumeChartContainer.value) return;
      
      if (volumeChartInstance) {
        volumeChartInstance.dispose();
      }
      
      volumeChartInstance = echarts.init(volumeChartContainer.value);
      
      const stockNames = stockData.value.map(stock => stock.name);
      const volumes = stockData.value.map(stock => stock.volume);
      const changePercents = stockData.value.map(stock => stock.changePercent);
      
      // 行业颜色映射
      const industryColors = {
        '科技': Colors['1'], // 蓝色
        '金融': Colors['2'], // 绿色
        '工业': Colors['3'], // 黄色
        '消费': Colors['4'], // 红色
        '医药': Colors['5']  // 青色
      };
      
      // 获取每个股票对应的行业和颜色
      const getStockIndustryAndColor = (stockName) => {
        for (const [industry, companies] of Object.entries(industryCompanies)) {
          if (companies.includes(stockName)) {
            return {
              industry,
              color: industryColors[industry] || Colors['1']
            };
          }
        }
        return {
          industry: '其他',
          color: Colors['1']
        };
      };
      
      // 准备柱状图数据
      const volumeData = stockNames.map((name, index) => {
        const { industry, color } = getStockIndustryAndColor(name);
        return {
          value: volumes[index],
          itemStyle: {
            color: color,
            borderRadius: [4, 4, 0, 0]
          },
          emphasis: {
            itemStyle: {
              shadowBlur: 10,
              shadowColor: 'rgba(0, 0, 0, 0.5)',
              borderWidth: 2,
              borderColor: '#fff'
            }
          },
          name: name,
          industry: industry
        };
      });
      
      const option = {
        title: {
          text: '股票成交量对比（带涨跌幅趋势）',
          left: 'center',
          textStyle: {
            color: '#2d3748',
            fontSize: 18,
            fontWeight: 'bold'
          }
        },
        tooltip: {
          trigger: 'axis',
          axisPointer: {
            type: 'shadow'
          },
          formatter: params => {
            let result = '';
            params.forEach(param => {
              if (param.seriesName === '成交量') {
                const industryInfo = getStockIndustryAndColor(param.name);
                result += `
                  <div style="margin-bottom: 8px;">
                    <strong>${param.name}</strong><br/>
                    行业: ${industryInfo.industry}<br/>
                    成交量: ${formatVolume(param.value)}
                  </div>
                `;
              } else if (param.seriesName === '涨跌幅') {
                result += `
                  <div>
                    涨跌幅: <span style="color: ${param.value >= 0 ? '#ef4444' : '#10b981'}">
                      ${param.value >= 0 ? '+' : ''}${param.value.toFixed(2)}%
                    </span>
                  </div>
                `;
              }
            });
            return result;
          },
          backgroundColor: 'rgba(255, 255, 255, 0.95)',
          borderColor: '#e2e8f0',
          borderWidth: 1,
          textStyle: {
            color: '#2d3748'
          },
          padding: 12,
          boxShadow: '0 4px 6px rgba(0, 0, 0, 0.1)'
        },
        legend: {
          data: ['成交量', '涨跌幅'],
          top: 50,
          textStyle: {
            color: '#4a5568'
          }
        },
        grid: {
          left: '3%',
          right: '4%',
          bottom: '8%', 
          top: '20%',
          containLabel: true
        },
        xAxis: [
          {
            type: 'category',
            data: stockNames,
            axisLabel: {
              interval: 0,
              rotate: 30,
              color: '#4a5568',
              fontSize: 10
            },
            axisLine: {
              lineStyle: {
                color: '#e2e8f0'
              }
            },
            axisTick: {
              show: false
            }
          }
        ],
        yAxis: [
          {
            type: 'value',
            name: '成交量',
            nameTextStyle: {
              color: '#4a5568',
              fontSize: 12
            },
            axisLabel: {
              formatter: function(value) {
                if (value >= 1000000) {
                  return (value / 1000000) + 'M';
                } else if (value >= 1000) {
                  return (value / 1000) + 'K';
                }
                return value;
              },
              color: '#4a5568',
              fontSize: 12
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
            name: '涨跌幅(%)',
            nameTextStyle: {
              color: '#4a5568',
              fontSize: 12
            },
            axisLabel: {
              formatter: '{value}%',
              color: '#4a5568',
              fontSize: 12
            },
            axisLine: {
              lineStyle: {
                color: '#64748b'
              }
            },
            splitLine: {
              show: false
            }
          }
        ],
        series: [
          {
            name: '成交量',
            type: 'bar',
            data: volumeData,
            label: {
              position: 'top',
              formatter: params => formatVolume(params.value),
              color: '#4a5568',
              fontSize: 11,
              show: false
            },
            barWidth: '60%',
            emphasis: {
              focus: 'series',
              itemStyle: {
                shadowBlur: 15,
                shadowColor: 'rgba(0, 0, 0, 0.3)',
                borderWidth: 2,
                borderColor: '#ffffff'
              }
            },
            // 添加选中效果
            select: {
              itemStyle: {
                borderColor: '#000',
                borderWidth: 2
              }
            }
          },
          {
            name: '涨跌幅',
            type: 'line',
            yAxisIndex: 1,
            data: changePercents,
            symbol: 'circle',
            symbolSize: 6,
            lineStyle: {
              color: '#64748b',
              width: 2
            },
            itemStyle: {
              color: '#64748b'
            },
            label: {
              show: false,
              position: 'top',
              formatter: params => {
                const value = params.value;
                return (value >= 0 ? '+' : '') + value.toFixed(1) + '%';
              },
              color: params => params.value >= 0 ? '#ef4444' : '#10b981',
              fontSize: 10,
              fontWeight: 'bold'
            },
            emphasis: {
              focus: 'series',
              lineStyle: {
                width: 3,
                shadowBlur: 8,
                shadowColor: 'rgba(100, 116, 139, 0.3)'
              },
              itemStyle: {
                shadowBlur: 10,
                shadowColor: 'rgba(100, 116, 139, 0.5)',
                borderWidth: 2,
                borderColor: '#ffffff'
              }
            },
            smooth: false
          }
        ]
      };
      
      volumeChartInstance.setOption(option);
      
      // 添加图表事件监听
      volumeChartInstance.on('mouseover', function (params) {
        console.log('鼠标悬停:', params);
      });
      
      volumeChartInstance.on('mouseout', function (params) {
        console.log('鼠标离开:', params);
      });
    };

    // 初始化涨跌值图表
    const initChangeValueChart = () => {
      if (!changeValueChartContainer.value) return;
      
      if (changeValueChartInstance) {
        changeValueChartInstance.dispose();
      }
      
      changeValueChartInstance = echarts.init(changeValueChartContainer.value);
      
      const stockNames = stockData.value.map(stock => stock.name);
      const changes = stockData.value.map(stock => stock.change);
      
      const option = {
        title: {
          text: '股票涨跌值对比($)',
          left: 'center',
          textStyle: {
            color: '#2d3748',
            fontSize: 16,
            fontWeight: 'bold'
          }
        },
        tooltip: {
          trigger: 'axis',
          axisPointer: {
            type: 'shadow'
          },
          formatter: params => {
            const data = params[0];
            return `${data.name}<br/>涨跌值: ${data.value.toFixed(2)}`;
          },
          backgroundColor: 'rgba(255, 255, 255, 0.9)',
          borderColor: '#e2e8f0',
          borderWidth: 1,
          textStyle: {
            color: '#2d3748'
          },
          padding: 10,
          boxShadow: '0 4px 6px rgba(0, 0, 0, 0.1)'
        },
        grid: {
          left: '3%',
          right: '4%',
          bottom: '10%',
          containLabel: true
        },
        xAxis: {
          type: 'category',
          data: stockNames,
          axisLabel: {
            interval: 0,
            rotate: 30,
            formatter: function(value) {
              return formatStockName(value);
            },
            color: '#4a5568',
            fontSize: 8
          },
          axisLine: {
            lineStyle: {
              color: '#e2e8f0'
            }
          },
          axisTick: {
            show: false
          }
        },
        yAxis: {
          type: 'value',
          name: '涨跌值($)',
          nameTextStyle: {
            color: '#4a5568',
            fontSize: 11
          },
          axisLabel: {
            color: '#4a5568',
            fontSize: 11
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
            name: '涨跌值',
            type: 'bar',
            data: changes,
            itemStyle: {
              color: function(params) {
                return params.value >= 0 ? '#10b981' : '#3b82f6';
              },
              borderRadius: [2, 2, 2, 2]
            },
            label: {
              show: false,
              position: 'top',
              formatter: params => {
                const value = params.value;
                return (value >= 0 ? '+' : '') + value.toFixed(2);
              },
              color: function(params) {
                return params.value >= 0 ? '#10b981' : '#3b82f6';
              },
              fontSize: 10
            },
            barWidth: '60%'
          }
        ]
      };
      
      changeValueChartInstance.setOption(option);
    };

    // 初始化涨跌幅图表
    const initChangePercentChart = () => {
      if (!changePercentChartContainer.value) return;
      
      if (changePercentChartInstance) {
        changePercentChartInstance.dispose();
      }
      
      changePercentChartInstance = echarts.init(changePercentChartContainer.value);
      
      const stockNames = stockData.value.map(stock => stock.name);
      const changePercents = stockData.value.map(stock => stock.changePercent);
      
      const option = {
        title: {
          text: '股票涨跌幅对比(%)',
          left: 'center',
          textStyle: {
            color: '#2d3748',
            fontSize: 16,
            fontWeight: 'bold'
          }
        },
        tooltip: {
          trigger: 'axis',
          axisPointer: {
            type: 'shadow'
          },
          formatter: params => {
            const data = params[0];
            return `${data.name}<br/>涨跌幅: ${data.value.toFixed(2)}%`;
          },
          backgroundColor: 'rgba(255, 255, 255, 0.9)',
          borderColor: '#e2e8f0',
          borderWidth: 1,
          textStyle: {
            color: '#2d3748'
          },
          padding: 10,
          boxShadow: '0 4px 6px rgba(0, 0, 0, 0.1)'
        },
        grid: {
          left: '3%',
          right: '4%',
          bottom: '10%',
          containLabel: true
        },
        xAxis: {
          type: 'category',
          data: stockNames,
          axisLabel: {
            interval: 0,
            rotate: 30,
            formatter: function(value) {
              return formatStockName(value);
            },
            color: '#4a5568',
            fontSize: 8
          },
          axisLine: {
            lineStyle: {
              color: '#e2e8f0'
            }
          },
          axisTick: {
            show: false
          }
        },
        yAxis: {
          type: 'value',
          name: '涨跌幅(%)',
          nameTextStyle: {
            color: '#4a5568',
            fontSize: 11
          },
          axisLabel: {
            formatter: function(value) {
              return value + '%';
            },
            color: '#4a5568',
            fontSize: 11
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
            name: '涨跌幅',
            type: 'bar',
            data: changePercents,
            itemStyle: {
              color: function(params) {
                return params.value >= 0 ? '#10b981' : '#3b82f6';
              },
              borderRadius: [2, 2, 2, 2]
            },
            label: {
              show: false,
              position: 'top',
              formatter: params => {
                const value = params.value;
                return (value >= 0 ? '+' : '') + value.toFixed(2) + '%';
              },
              color: function(params) {
                return params.value >= 0 ? '#10b981' : '#3b82f6';
              },
              fontSize: 10
            },
            barWidth: '60%'
          }
        ]
      };
      
      changePercentChartInstance.setOption(option);
    };

    // 初始化成交量热力图
    const initVolumeHeatmap = () => {
      if (!volumeHeatmapContainer.value) return;
      
      if (volumeHeatmapInstance) {
        volumeHeatmapInstance.dispose();
      }
      
      volumeHeatmapInstance = echarts.init(volumeHeatmapContainer.value);
      
      const industries = Object.keys(industryCompanies);
      const data = [];
      
      console.log('=== 热力图数据调试 ===');
      console.log('股票数据:', stockData.value);
      
      // 构建热力图数据
      industries.forEach((industry, yIndex) => {
        const companies = industryCompanies[industry];
        console.log(`行业 ${industry}:`, companies);
        
        companies.forEach((company, xIndex) => {
          const stock = stockData.value.find(s => s.name === company);
          if (stock) {
            data.push([xIndex, yIndex, stock.volume]);
            console.log(`  ${company}: 成交量=${stock.volume}, 位置=[${xIndex},${yIndex}]`);
          } else {
            console.log(`  ${company}: 未找到数据`);
          }
        });
      });
      
      console.log('最终热力图数据:', data);
      
      if (data.length === 0) {
        console.error('没有可用的热力图数据');
        return;
      }
      
      const volumes = data.map(item => item[2]);
      const maxVolume = Math.max(...volumes);
      const minVolume = Math.min(...volumes);
      
      console.log(`成交量范围: ${minVolume} - ${maxVolume}`);
      
      const option = {
        title: {
          text: '股票成交量热力图',
          left: 'center',
          textStyle: {
            color: '#2d3748',
            fontSize: 16,
            fontWeight: 'bold'
          }
        },
        tooltip: {
          position: 'top',
          formatter: (params) => {
            const companyIndex = params.data[0];
            const industryIndex = params.data[1];
            const volume = params.data[2];
            
            // 找到对应的公司名
            const industry = industries[industryIndex];
            const companies = industryCompanies[industry];
            const company = companies[companyIndex];
            
            return `${company}<br/>成交量: ${formatVolume(volume)}`;
          },
          backgroundColor: 'rgba(255, 255, 255, 0.95)',
          borderColor: '#e2e8f0',
          textStyle: {
            color: '#2d3748'
          }
        },
        grid: {
          top: '15%',
          bottom: '8%',
          left: '10%',
          right: '15%'
        },
        xAxis: {
          type: 'category',
          show: false
        },
        yAxis: {
          type: 'category',
          data: industries,
          axisLine: {
            show: true,
            lineStyle: {
              color: '#cbd5e1'
            }
          },
          axisTick: {
            show: false
          },
          axisLabel: {
            color: '#475569',
            fontSize: 12,
            fontWeight: 'bold'
          }
        },
        visualMap: {
          show: true,
          type: 'continuous',
          min: minVolume,
          max: maxVolume,
          calculable: true,
          orient: 'vertical',
          left: 'right',
          top: 'center',
          textStyle: {
            color: '#475569'
          },
          inRange: {
            color: ['#f1f5f9', '#3b82f6']
          },
          itemWidth: 12,
          itemHeight: 100
        },
        series: [{
          name: '成交量',
          type: 'heatmap',
          data: data,
          label: {
            show: true,
            formatter: (params) => {
              const companyIndex = params.data[0];
              const industryIndex = params.data[1];
              const industry = industries[industryIndex];
              const companies = industryCompanies[industry];
              const company = companies[companyIndex];
              return formatStockName(company);
            },
            color: '#1e293b',
            fontSize: 10,
            fontWeight: 'bold'
          },
          itemStyle: {
            borderColor: '#fff',
            borderWidth: 1
          },
          emphasis: {
            itemStyle: {
              shadowBlur: 10,
              shadowColor: 'rgba(0, 0, 0, 0.5)'
            }
          }
        }]
      };
      
      volumeHeatmapInstance.setOption(option);
      console.log('热力图配置完成');
    };

    // 初始化行业雷达图
    const initRadarChart = () => {
      if (!radarContainer.value) return;
      
      if (radarInstance) {
        radarInstance.dispose();
      }
      
      radarInstance = echarts.init(radarContainer.value);
      
      const industries = Object.keys(industryCompanies);
      
      // 计算每个行业的指标
      const industryData = industries.map(industry => {
        const companies = industryCompanies[industry];
        const industryStocks = stockData.value.filter(stock => companies.includes(stock.name));
        
        if (industryStocks.length === 0) return null;
        
        const avgVolume = industryStocks.reduce((sum, stock) => sum + stock.volume, 0) / industryStocks.length;
        const avgChange = industryStocks.reduce((sum, stock) => sum + stock.change, 0) / industryStocks.length;
        const avgChangePercent = industryStocks.reduce((sum, stock) => sum + stock.changePercent, 0) / industryStocks.length;
        const volatility = calculateVolatility(industryStocks);
        
        return {
          name: industry,
          volume: avgVolume,
          change: Math.abs(avgChange),
          changePercent: Math.abs(avgChangePercent),
          volatility: volatility,
          stockCount: industryStocks.length
        };
      }).filter(data => data !== null);
      
      const indicator = [
        { name: '平均成交量', max: Math.max(...industryData.map(d => d.volume)) },
        { name: '平均涨跌值', max: Math.max(...industryData.map(d => d.change)) },
        { name: '平均涨跌幅', max: Math.max(...industryData.map(d => d.changePercent)) },
        { name: '波动率', max: Math.max(...industryData.map(d => d.volatility)) },
        { name: '股票数量', max: Math.max(...industryData.map(d => d.stockCount)) }
      ];
      
      const seriesData = industryData.map((data, index) => {
        const colorIndex = (index % 5) + 1;
        return {
          value: [
            data.volume,
            data.change,
            data.changePercent,
            data.volatility,
            data.stockCount
          ],
          name: data.name,
          itemStyle: {
            color: Colors[colorIndex.toString()]
          },
          areaStyle: {
            color: Colors[colorIndex.toString()],
            opacity: 0.2
          },
          lineStyle: {
            color: Colors[colorIndex.toString()],
            width: 2
          }
        };
      });
      
      const option = {
        title: {
          text: '行业多维指标雷达图',
          left: 'center',
          textStyle: {
            color: '#2d3748',
            fontSize: 16,
            fontWeight: 'bold'
          }
        },
        tooltip: {},
        legend: {
          data: industryData.map(d => d.name),
          bottom: 10,
          textStyle: {
            color: '#4a5568'
          }
        },
        radar: {
          indicator: indicator,
          shape: 'circle',
          splitNumber: 5,
          axisName: {
            color: '#4a5568',
            backgroundColor: '#f8fafc',
            borderRadius: 3,
            padding: [3, 5]
          },
          splitLine: {
            lineStyle: {
              color: 'rgba(100, 116, 139, 0.2)'
            }
          },
          splitArea: {
            areaStyle: {
              color: ['rgba(248, 250, 252, 0.5)', 'rgba(241, 245, 249, 0.5)']
            }
          },
          axisLine: {
            lineStyle: {
              color: 'rgba(100, 116, 139, 0.3)'
            }
          }
        },
        series: [{
          name: '行业指标',
          type: 'radar',
          data: seriesData,
          emphasis: {
            lineStyle: {
              width: 4
            }
          }
        }]
      };
      
      radarInstance.setOption(option);
    };

    // 初始化成交量趋势面积图
    const initAreaChart = () => {
      if (!areaContainer.value) return;
      
      if (areaInstance) {
        areaInstance.dispose();
      }
      
      areaInstance = echarts.init(areaContainer.value);
      
      const industries = Object.keys(industryCompanies);
      
      // 为每个行业分配固定的颜色
      const industryColors = {
        '科技': Colors['1'], // 蓝色
        '金融': Colors['2'], // 绿色
        '工业': Colors['3'], // 黄色
        '消费': Colors['4'], // 红色
        '医药': Colors['5']  // 青色
      };
      
      // 生成日期数据
      const dates = [];
      const now = new Date();
      for (let i = 6; i >= 0; i--) {
        const date = new Date(now);
        date.setDate(date.getDate() - i);
        dates.push(date.toLocaleDateString('zh-CN', { 
          month: '2-digit', 
          day: '2-digit' 
        }));
      }

      const seriesData = industries.map((industry) => {
        const companies = industryCompanies[industry];
        const industryStocks = stockData.value.filter(stock => companies.includes(stock.name));
        
        if (industryStocks.length === 0) {
          return {
            name: industry,
            type: 'line',
            stack: '总量',
            areaStyle: {
              color: industryColors[industry],
              opacity: 0.6
            },
            lineStyle: {
              color: industryColors[industry],
              width: 2
            },
            data: new Array(7).fill(0)
          };
        }
        
        // 计算当前行业的总成交量
        const currentTotalVolume = industryStocks.reduce((sum, stock) => sum + stock.volume, 0);
        const baseVolume = currentTotalVolume / industryStocks.length;
        const data = [];
        for (let i = 0; i < 7; i++) {
          const dayFactor = 0.8 + (Math.random() * 0.4); 
          const volume = Math.round(baseVolume * dayFactor);
          data.push(volume);
        }
        
        return {
          name: industry,
          type: 'line',
          stack: '总量',
          areaStyle: {
            color: industryColors[industry],
            opacity: 0.6
          },
          lineStyle: {
            color: industryColors[industry],
            width: 2
          },
          emphasis: {
            focus: 'series'
          },
          data: data
        };
      });
      
      const allVolumes = seriesData.flatMap(series => series.data);
      const maxVolume = Math.max(...allVolumes.filter(v => v > 0));
      const yAxisMax = maxVolume > 0 ? Math.ceil(maxVolume * 1.15) : 1000000;
      
      const option = {
        title: {
          text: '行业成交量趋势',
          left: 'center',
          textStyle: {
            color: '#2d3748',
            fontSize: 14, 
            fontWeight: 'bold'
          }
        },
        tooltip: {
          trigger: 'axis',
          axisPointer: {
            type: 'cross',
            label: {
              backgroundColor: '#6a7985'
            }
          },
          formatter: function(params) {
            let result = `<div style="font-weight:bold;margin-bottom:6px;font-size:12px">${params[0].name}</div>`;
            let total = 0;
            
            params.forEach(function(item) {
              const value = item.value || 0;
              total += value;
              result += `
                <div style="display:flex;align-items:center;justify-content:space-between;margin:3px 0;font-size:11px">
                  <div style="display:flex;align-items:center;">
                    <span style="display:inline-block;width:8px;height:8px;border-radius:50%;background:${industryColors[item.seriesName]};margin-right:6px"></span>
                    <span>${item.seriesName}</span>
                  </div>
                  <span style="font-weight:bold;margin-left:15px;">${formatVolume(value)}</span>
                </div>
              `;
            });
            
            result += `
              <div style="border-top:1px solid #e2e8f0;margin-top:6px;padding-top:4px;display:flex;justify-content:space-between;font-weight:bold;font-size:11px">
                <span>总计</span>
                <span>${formatVolume(total)}</span>
              </div>
            `;
            return result;
          }
        },
        legend: {
          data: industries,
          top: 42, 
          textStyle: {
            color: '#4a5568',
            fontSize: 10 
          },
          itemWidth: 10,
          itemHeight: 10,
          itemGap: 10
        },
        grid: {
          left: '3%', 
          right: '5%', 
          bottom: '7%', 
          top: '20%', 
          containLabel: true
        },
        xAxis: {
          type: 'category',
          boundaryGap: false,
          data: dates,
          axisLabel: {
            color: '#4a5568',
            fontSize: 10, 
            interval: 0 
          },
          axisLine: {
            lineStyle: {
              color: '#e2e8f0'
            }
          },
          axisTick: {
            show: false
          }
        },
        yAxis: {
          type: 'value',
          max: yAxisMax*1.4,
          axisLabel: {
            formatter: value => formatVolume(value),
            color: '#4a5568',
            fontSize: 10 
          },
          axisLine: {
            show: false
          },
          axisTick: {
            show: false
          },
          splitLine: {
            lineStyle: {
              color: '#f1f5f9',
              type: 'solid'
            }
          },
          splitNumber: 5 
        },
        series: seriesData
      };
      
      areaInstance.setOption(option);
    };

    // 初始化成交量与涨跌幅散点图
    const initScatterChart = () => {
      if (!scatterContainer.value) return;
      
      if (scatterInstance) {
        scatterInstance.dispose();
      }
      
      scatterInstance = echarts.init(scatterContainer.value);
      
      const data = stockData.value.map(stock => {
        return [
          stock.volume,
          stock.changePercent,
          stock.change,
          stock.name
        ];
      });
      
      // 计算最大成交量
      const maxVolume = Math.max(...stockData.value.map(stock => stock.volume));
      const extendedMaxVolume = maxVolume * 1.2; 
      
      const option = {
        title: {
          text: '成交量与涨跌幅关系散点图',
          left: 'center',
          textStyle: {
            color: '#2d3748',
            fontSize: 16,
            fontWeight: 'bold'
          }
        },
        tooltip: {
          formatter: params => {
            return `${params.data[3]}<br/>
                    成交量: ${formatVolume(params.data[0])}<br/>
                    涨跌幅: ${params.data[1].toFixed(2)}%<br/>
                    涨跌值: ${params.data[2].toFixed(2)}`;
          }
        },
        grid: {
          left: '3%',
          right: '7%',
          bottom: '7%',
          containLabel: true
        },
        xAxis: {
          type: 'value',
          name: '成交量',
          nameLocation: 'middle',
          nameGap: 30,
          min: 0,
          max: extendedMaxVolume,
          axisLabel: {
            formatter: value => formatVolume(value),
            color: '#4a5568'
          }
        },
        yAxis: {
          type: 'value',
          name: '涨跌幅(%)',
          nameLocation: 'middle',
          nameGap: 30,
          axisLabel: {
            color: '#4a5568'
          }
        },
        visualMap: {
          type: 'continuous',
          min: Math.min(...stockData.value.map(s => s.change)),
          max: Math.max(...stockData.value.map(s => s.change)),
          dimension: 2,
          orient: 'vertical',
          right: 10,
          top: 'center',
          textStyle: {
            color: '#4a5568'
          },
          inRange: {
            color: [Colors['4'], Colors['3'], Colors['2']] 
          }
        },
        series: [{
          type: 'scatter',
          symbolSize: data => {
            return Math.min(Math.sqrt(data[0]) / 80, 50); 
          },
          data: data,
          emphasis: {
            focus: 'series',
            label: {
              show: true,
              formatter: param => param.data[3],
              position: 'top'
            }
          }
        }]
      };
      
      scatterInstance.setOption(option);
    };

    // 加载股票数据
    const loadStockData = async () => {
      isLoading.value = true;
      error.value = null;
      
      try {
        const endTime = new Date();
        const startTime = new Date();
        startTime.setDate(endTime.getDate() - 3);

        const promises = availableStocks.map(stock => 
          fetchStockData(stock, startTime, endTime)
        );
        
        const results = await Promise.all(promises);
        stockData.value = results.filter(data => data !== null);
        
        if (stockData.value.length === 0) {
          throw new Error('未能获取任何股票数据');
        }
        
        marketData.value = generateMarketData(stockData.value);
      } catch (err) {
        console.error('获取股票数据失败:', err);
        error.value = `获取股票数据失败: ${err.message}`;
      } finally {
        isLoading.value = false;
      }
    };

    // 获取单只股票数据
    const fetchStockData = async (stockName, startTime, endTime) => {
      try {
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
        const change_per = parseFloat(changeStr.replace('%', ''));

        return {
          name: stockName,
          symbol: formatStockName(stockName),
          volume: parseVolume(latestData.vol),
          change: change,
          changePercent: change_per,
          rawData: latestData
        };
      } catch (err) {
        console.error(`获取 ${stockName} 数据失败:`, err);
        return null;
      }
    };

    // 初始化加载数据
    loadStockData();

    // 监听stockData变化，数据加载完成后初始化图表
    watch(stockData, (newVal) => {
      if (newVal.length > 0) {
        initVolumeChart();
        initChangeValueChart();
        initChangePercentChart();
        initVolumeHeatmap();
        initRadarChart();
        initAreaChart();
        initScatterChart();
      }
    }, { deep: true });

    // 组件卸载时清理
    onMounted(() => {
      return () => {
        if (volumeChartInstance) volumeChartInstance.dispose();
        if (changeValueChartInstance) changeValueChartInstance.dispose();
        if (changePercentChartInstance) changePercentChartInstance.dispose();
        if (volumeHeatmapInstance) volumeHeatmapInstance.dispose();
        if (radarInstance) radarInstance.dispose();
        if (areaInstance) areaInstance.dispose();
        if (scatterInstance) scatterInstance.dispose();
      };
    });

    // 计算前三名数据
    const volumeRanking = computed(() => {
      return [...stockData.value]
        .sort((a, b) => b.volume - a.volume)
        .slice(0, 3);
    });
    
    const changeValueRanking = computed(() => {
      return [...stockData.value]
        .sort((a, b) => b.change - a.change)
        .slice(0, 3);
    });
    
    const changePercentRanking = computed(() => {
      return [...stockData.value]
        .sort((a, b) => b.changePercent - a.changePercent)
        .slice(0, 3);
    });

    return {
      stockData,
      isLoading,
      error,
      marketData,
      volumeChartContainer,
      changeValueChartContainer,
      changePercentChartContainer,
      volumeHeatmapContainer,
      radarContainer,
      areaContainer,
      scatterContainer,
      formatVolume,
      formatCurrency,
      getSentimentColor,
      volumeRanking,
      changeValueRanking,
      changePercentRanking,
      isVolatilityGood
    };
  }
};
</script>

<style scoped>
.stock-chart-container {
  max-width: 1400px;
  margin: 0 auto;
  padding: 20px;
  font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
  background-color: #ffffff;
  border-radius: 12px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.05);
}

/* 市场概况组件样式 */
.market-overview {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  margin-bottom: 30px;
}

.overview-card {
  background: #ffffff;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  transition: all 0.3s ease;
  border: 1px solid rgba(0, 0, 0, 0.05);
  position: relative;
  overflow: hidden;
}

.overview-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 4px;
  background: linear-gradient(90deg, var(--card-color), transparent);
}

.overview-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 8px 25px rgba(0, 0, 0, 0.12);
}

.overview-content {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  height: 100%;
}

.overview-icon {
  width: 60px;
  height: 60px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 32px;
  color: white;
  flex-shrink: 0;
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.15);
}

.overview-text {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  height: 100%;
}

.overview-title {
  font-size: 14px;
  font-weight: 600;
  margin-bottom: 8px;
  color: #64748b;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.overview-value {
  font-size: 28px;
  font-weight: 700;
  line-height: 1.2;
  margin: 0;
  color: #1e293b;
}

/* 各个卡片的颜色 */
.total-volume-card {
  --card-color: #3b82f6;
}

.total-volume-card .overview-icon {
  background: linear-gradient(135deg, #3b82f6, #1d4ed8);
}

.total-volume-card::before {
  background: linear-gradient(90deg, #3b82f6, transparent);
}

.advancing-stocks-card {
  --card-color: #ef4444;
}

.advancing-stocks-card .overview-icon {
  background: linear-gradient(135deg, #ef4444, #dc2626);
}

.advancing-stocks-card::before {
  background: linear-gradient(90deg, #ef4444, transparent);
}

.declining-stocks-card {
  --card-color: #10b981;
}

.declining-stocks-card .overview-icon {
  background: linear-gradient(135deg, #10b981, #047857);
}

.declining-stocks-card::before {
  background: linear-gradient(90deg, #10b981, transparent);
}

.volatility-card {
  --card-color: #8b5cf6;
}

.volatility-card .overview-icon {
  background: linear-gradient(135deg, #8b5cf6, #7c3aed);
}

.volatility-card::before {
  background: linear-gradient(90deg, #8b5cf6, transparent);
}

/* 颜色类调整 */
.positive {
  color: #ef4444 !important;
}

.negative {
  color: #10b981 !important;
}

/* 响应式设计 */
@media (max-width: 1200px) {
  .market-overview {
    grid-template-columns: repeat(2, 1fr);
    gap: 16px;
  }
}

@media (max-width: 768px) {
  .market-overview {
    grid-template-columns: 1fr;
    gap: 12px;
  }
  
  .overview-card {
    padding: 20px;
  }
  
  .overview-content {
    gap: 12px;
  }
  
  .overview-icon {
    width: 60px;
    height: 60px;
    font-size: 28px;
  }
  
  .overview-value {
    font-size: 24px;
  }
}

/* 图表样式 */
.loading-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 300px;
  background-color: #f8fafc;
  border-radius: 12px;
  margin-bottom: 20px;
}

.spinner {
  width: 50px;
  height: 50px;
  border: 5px solid #e2e8f0;
  border-top: 5px solid #3b82f6;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-bottom: 15px;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.loading-text {
  color: #4a5568;
  font-size: 16px;
  font-weight: 500;
}

.error-container {
  display: flex;
  align-items: center;
  padding: 15px;
  background-color: #fef2f2;
  border-left: 4px solid #ef4444;
  border-radius: 8px;
  margin-bottom: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.error-icon {
  color: #ef4444;
  font-size: 20px;
  margin-right: 12px;
}

.error-message {
  color: #4a5568;
  font-size: 14px;
  line-height: 1.5;
}

.chart-container {
  background-color: #f8fafc;
  border-radius: 12px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
  transition: transform 0.3s ease, box-shadow 0.3s ease;
  overflow: hidden;
}

.chart-container:hover {
  transform: translateY(-3px);
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.08);
}

.volume-chart {
  width: 100%;
  height: 400px;
  margin-bottom: 30px;
}

.chart-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
  width: 100%;
  margin-bottom: 30px;
}

.heatmap-chart, .radar-chart, .area-chart, .scatter-chart,
.change-value-chart, .change-percent-chart {
  width: 100%;
  height: 350px;
}

.ranking-container {
  margin: 30px 0;
}

.ranking-title {
  text-align: center;
  color: #2d3748;
  font-size: 20px;
  font-weight: 600;
  margin-bottom: 20px;
  position: relative;
  padding-bottom: 10px;
}

.ranking-title::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 80px;
  height: 3px;
  background-color: #3b82f6;
  border-radius: 3px;
}

.ranking-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
}

.ranking-card {
  background-color: #f8fafc;
  border-radius: 12px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
  transition: transform 0.3s ease, box-shadow 0.3s ease;
  overflow: hidden;
}

.ranking-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.08);
}

.ranking-card-header {
  padding: 15px;
  background-color: #f1f5f9;
  border-bottom: 1px solid #e2e8f0;
  display: flex;
  align-items: center;
}

.ranking-card-header i {
  font-size: 18px;
  margin-right: 10px;
  color: #3b82f6;
}

.ranking-card-header h3 {
  font-size: 16px;
  font-weight: 600;
  color: #2d3748;
  margin: 0;
}

.ranking-list {
  list-style: none;
  padding: 0;
  margin: 0;
}

.ranking-list li {
  padding: 12px 15px;
  display: flex;
  align-items: center;
  border-bottom: 1px solid #e2e8f0;
  transition: background-color 0.2s ease;
}

.ranking-list li:last-child {
  border-bottom: none;
}

.ranking-list li:hover {
  background-color: #f1f5f9;
}

.rank-badge {
  width: 24px;
  height: 24px;
  border-radius: 4px;
  background-color: #e2e8f0;
  color: #4a5568;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: bold;
  margin-right: 15px;
}

.first-place .rank-badge {
  background-color: #f59e0b;
  color: #ffffff;
  box-shadow: 0 2px 4px rgba(245, 158, 11, 0.2);
}

.second-place .rank-badge {
  background-color: #6b7280;
  color: #ffffff;
  box-shadow: 0 2px 4px rgba(107, 114, 128, 0.2);
}

.third-place .rank-badge {
  background-color: #b45309;
  color: #ffffff;
  box-shadow: 0 2px 4px rgba(180, 83, 9, 0.2);
}

.stock-info {
  flex: 1;
}

.stock-name {
  font-weight: 500;
  color: #2d3748;
  margin-right: 8px;
}

.stock-symbol {
  font-size: 12px;
  color: #64748b;
  background-color: #e2e8f0;
  padding: 2px 6px;
  border-radius: 4px;
}

.stock-value {
  font-weight: 500;
  font-size: 14px;
}

@media (max-width: 1024px) {
  .stock-chart-container {
    padding: 15px;
  }
  
  .volume-chart {
    height: 350px;
  }
  
  .heatmap-chart, .radar-chart, .area-chart, .scatter-chart,
  .change-value-chart, .change-percent-chart {
    height: 300px;
  }
}

@media (max-width: 768px) {
  .ranking-grid {
    grid-template-columns: 1fr 1fr;
  }
  
  .chart-grid {
    grid-template-columns: 1fr;
  }
  
  .volume-chart {
    height: 300px;
  }
  
  .heatmap-chart, .radar-chart, .area-chart, .scatter-chart,
  .change-value-chart, .change-percent-chart {
    height: 280px;
  }
}

@media (max-width: 480px) {
  .ranking-grid {
    grid-template-columns: 1fr;
  }
  
  .stock-chart-container {
    padding: 10px;
  }
  
  .volume-chart {
    height: 250px;
  }
  
  .heatmap-chart, .radar-chart, .area-chart, .scatter-chart,
  .change-value-chart, .change-percent-chart {
    height: 250px;
  }
  
  .loading-container {
    height: 200px;
  }
  
  .spinner {
    width: 40px;
    height: 40px;
  }
}
</style>

<!-- 添加Bootstrap Icons CSS -->
<link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.13.1/font/bootstrap-icons.min.css">