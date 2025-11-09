<template>
  <div class="stock-dashboard">
    <!-- 全部股票分类 -->
    <div class="dashboard-card">
      <div class="dashboard-header">
        <h1>全部股票</h1>
        <p>数据最后更新 {{ lastUpdated.slice(0,10) || '加载中...' }}</p>
      </div>
      
      <!-- 上部图表区域：饼图和柱状图 -->
      <div class="top-charts-container">
        <!-- 左侧饼图 -->
        <div class="chart-wrapper pie-chart">
          <div ref="pieChartRef" class="chart"></div>
        </div>
        
        <!-- 右侧柱状图 -->
        <div class="chart-wrapper bar-chart">
          <div ref="barChartRef" class="chart"></div>
        </div>
      </div>
      
      <!-- 下部图表区域：折线图 -->
      <div class="bottom-chart-container">
        <div class="chart-wrapper line-chart">
          <!-- 折线图标题和控制面板 -->
          <div class="line-chart-header">
            <h3>多公司股票价格对比</h3>
            
            <div class="line-chart-controls">
              <!-- 时间范围选择 -->
              <div class="control-item">
                <label>时间范围：</label>
                <select v-model="selectedRange" @change="handleTimeRangeChange">
                  <option value="1d">1天</option>
                  <option value="1w">1周</option>
                  <option value="1m" selected>1个月</option>
                  <option value="3m">3个月</option>
                  <option value="6m">6个月</option>
                </select>
              </div>
            </div>
          </div>
          
          <!-- 折线图主体 -->
          <div ref="lineChartRef" class="chart"></div>
          
          <!-- 公司选择面板 -->
          <div class="company-selection-panel">
            <div class="company-list">
              <label v-for="company in allCompanies" :key="company">
                <input 
                  type="checkbox" 
                  v-model="selectedCompanies" 
                  :value="company"
                  @change="handleCompanySelectionChange"
                />
                {{ company }}
              </label>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import * as echarts from 'echarts';
import { advancedSearchStockData } from '@/services/stockDataApi';

export default {
  data() {
    return {
      // 饼图和柱状图数据
      pieChart: null,
      barChart: null,
      allCompanies: [
        '3M', 'Amazon.com', 'American Express', 'Amgen', 'Apple', 'Boeing',
        'Caterpillar', 'Chevron', 'Cisco', 'Citigroup', 'Coca-Cola', 'Goldman Sachs',
        'Home Depot', 'Honeywell', 'IBM', 'JPMorgan', 'McDonalds',
        'Microsoft', 'NVIDIA', 'Salesforce Inc', 'Sherwin-Williams',
        'Travelers', 'UnitedHealth', 'Verizon', 'Visa A', 'Walmart', 'Walt Disney',
        'Nike'
      ],
      latestPrices: {}, 
      lastUpdated: '', 
      industryCompanies: {
        '科技': ['Amazon.com', 'Apple', 'Cisco', 'Microsoft', 'NVIDIA', 'Salesforce Inc', 'IBM'],
        '金融': ['American Express', 'Citigroup', 'Goldman Sachs', 'JPMorgan', 'Visa A', 'Travelers'],
        '工业': ['3M', 'Boeing', 'Caterpillar', 'Honeywell', 'Sherwin-Williams', 'Chevron', 'Verizon'],
        '消费': ['Coca-Cola', 'McDonald’s', 'Walmart', 'Nike', 'Walt Disney', 'Home Depot'],
        '医药': ['Amgen', 'UnitedHealth']
      },
      industryColors: {
        '科技': '#5470C6',
        '金融': '#91CC75',
        '工业': '#FAC858',
        '消费': '#EE6666',
        '医药': '#73C0DE'
      },
      
      // 折线图数据
      lineChart: null,
      selectedRange: '1m',
      selectedCompanies: ['Amgen', 'Apple', 'IBM', 'JPMorgan'], 
      allStockData: {}, 
      timestamps: [], 
      alignedData: {} 
    };
  },
  mounted() {
    // 初始化所有图表
    this.initPieChart();
    this.initBarChart();
    this.initLineChart();
    
    // 获取数据
    this.fetchLatestPrices();
    this.fetchInitialData();
    
    // 添加窗口大小变化监听
    window.addEventListener('resize', this.handleResize);
  },
  methods: {
    // 饼图相关方法
    initPieChart() {
      if (!this.$refs.pieChartRef) return;

      const chartDom = this.$refs.pieChartRef;
      this.pieChart = echarts.init(chartDom);

      // 准备饼图数据
      const pieData = this.preparePieChartData();
      
      const option = {
        title: {
          text: '股票行业分布',
          left: 'center',
          top: 10,
          textStyle: {
            fontSize: 16,
            fontWeight: 'bold',
            color: '#333'
          }
        },
        tooltip: {
          trigger: 'item',
          backgroundColor: 'rgba(255, 255, 255, 0.9)',
          borderColor: '#ddd',
          borderWidth: 1,
          borderRadius: 8,
          padding: 12,
          boxShadow: '0 2px 10px rgba(0, 0, 0, 0.1)',
          formatter: (params) => {
            // 悬停时显示公司列表
            const companies = this.industryCompanies[params.name] || [];
            return `
              <div style="font-weight:bold;margin-bottom:8px;color:${params.color};">${params.name}</div>
              <div>公司数量: ${params.value}家</div>
              <div style="margin-top:8px;">
                ${companies.map(company => `<div>• ${company}</div>`).join('')}
              </div>
            `;
          }
        },
        legend: {
          type: 'scroll',
          orient: 'vertical',
          right: 10,
          top: 'center',
          data: Object.keys(this.industryCompanies),
          textStyle: {
            color: '#666',
            fontSize: 12
          },
          selectedMode: false
        },
        series: this.createPieChartSeries(pieData)
      };

      this.pieChart.setOption(option);
    },

    // 柱状图相关方法
    initBarChart() {
      if (!this.$refs.barChartRef) return;

      this.barChart = echarts.init(this.$refs.barChartRef);
      this.barChart.setOption({
        title: { 
          text: '各公司股票价格对比',
          left: 'center',
          top: 10,
          textStyle: {
            fontSize: 16,
            fontWeight: 'bold',
            color: '#333'
          }
        },
        tooltip: { 
          trigger: 'axis',
          axisPointer: {
            type: 'shadow'
          },
          backgroundColor: 'rgba(255, 255, 255, 0.9)',
          borderColor: '#ddd',
          borderWidth: 1,
          borderRadius: 8,
          padding: 12,
          boxShadow: '0 2px 10px rgba(0, 0, 0, 0.1)',
          formatter: (params) => {
            const param = params[0];
            return `
              <div style="font-weight:bold;margin-bottom:8px;">${param.name}</div>
              <div>股票价格: $${param.value.toFixed(2)}</div>
            `;
          }
        },
        grid: {
          left: '3%',
          right: '4%',
          bottom: '20%',
          top: '15%',
          containLabel: true
        },
        xAxis: { 
          type: 'category',
          axisLabel: {
            interval: 0,
            rotate: 45,
            fontSize: 10,
            color: '#666'
          },
          axisLine: {
            lineStyle: {
              color: '#eee'
            }
          }
        },
        yAxis: { 
          type: 'value',
          axisLabel: {
            formatter: '${value}' 
          },
          name: '价格 (USD)',
          nameTextStyle: {
            color: '#666'
          },
          splitLine: {
            lineStyle: {
              color: '#f0f0f0'
            }
          },
          axisLine: {
            show: false
          }
        },
        series: []
      });
    },
    
    // 折线图相关方法
    initLineChart() {
      if (!this.$refs.lineChartRef) return;

      this.lineChart = echarts.init(this.$refs.lineChartRef);
      // 初始空配置
      this.lineChart.setOption({
        title: { 
          text: '多公司股票价格对比',
          left: 'center',
          textStyle: {
            fontSize: 16,
            fontWeight: 'bold',
            color: '#333'
          }
        },
        tooltip: { 
          trigger: 'axis',
          axisPointer: {
            type: 'shadow',
            label: {
              show: true,
              backgroundColor: '#333'
            }
          },
          formatter: function(params) {
            let result = params[0].name + '<br/>';
            params.forEach(param => {
              result += `<span style="display:inline-block;margin-right:5px;border-radius:10px;width:10px;height:10px;background-color:${param.color};"></span>`;
              result += param.seriesName + ': $' + param.value.toFixed(2) + '<br/>';
            });
            return result;
          }
        },
        xAxis: { type: 'category' },
        yAxis: { 
          type: 'value',
          axisLabel: {
            formatter: '${value}'
          }
        },
        legend: { 
          data: this.selectedCompanies, 
          bottom: 0,
          left: 'center',
          type: 'scroll',
          textStyle: {
            color: '#666'
          }
        },
        series: []
      });
    },

    handleResize() {
      if (this.pieChart) {
        this.pieChart.resize();
      }
      if (this.barChart) {
        this.barChart.resize();
      }
      if (this.lineChart) {
        this.lineChart.resize();
      }
    },

    // 饼图和柱状图数据获取
    async fetchLatestPrices() {
      try {
        // 获取所有公司的最新数据
        const fetchPromises = this.allCompanies.map(async company => {
          try {
            // 获取最近1天的数据
            const endTime = new Date();
            const startTime = new Date();
            startTime.setHours(endTime.getHours() - 1);
            
            const stockData = await advancedSearchStockData(company, 
              startTime.toISOString(), 
              endTime.toISOString()
            );
            
            if (stockData && stockData.length > 0) {
              // 按时间倒序排列，取第一条
              const latestData = stockData.sort((a, b) => 
                new Date(b.timestamp) - new Date(a.timestamp)
              )[0];
              
              this.latestPrices[company] = parseFloat(latestData.last);
            }
          } catch (error) {
            console.error(`获取 ${company} 数据失败:`, error);
          }
        });

        await Promise.all(fetchPromises);
        
        // 更新最后更新时间
        this.lastUpdated = new Date().toLocaleString();
        
        // 所有数据获取完成后更新图表
        this.updateBarChart();
      } catch (error) {
        console.error('获取数据失败:', error);
      }
    },

    updateBarChart() {
      if (!this.barChart) return;

      // 准备数据 - 按行业分组
      const { seriesData, categories } = this.prepareBarChartData();
      
      const option = {
        title: { 
          text: '各公司股票价格对比',
          left: 'center',
          top: 10,
          textStyle: {
            fontSize: 16,
            fontWeight: 'bold',
            color: '#333'
          }
        },
        tooltip: { 
          trigger: 'axis',
          axisPointer: {
            type: 'shadow'
          },
          backgroundColor: 'rgba(255, 255, 255, 0.9)',
          borderColor: '#ddd',
          borderWidth: 1,
          borderRadius: 8,
          padding: 12,
          boxShadow: '0 2px 10px rgba(0, 0, 0, 0.1)',
          formatter: (params) => {
            const param = params[0];
            return `
              <div style="font-weight:bold;margin-bottom:8px;">${param.name}</div>
              <div>股票价格: $${param.value.toFixed(2)}</div>
            `;
          }
        },
        grid: {
          left: '3%',
          right: '4%',
          bottom: '20%',
          top: '15%',
          containLabel: true
        },
        xAxis: {
          type: 'category',
          data: categories,
          axisLabel: {
            interval: 0,
            rotate: 45,
            fontSize: 10,
            color: '#666'
          },
          axisLine: {
            lineStyle: {
              color: '#eee'
            }
          }
        },
        yAxis: { 
          type: 'value',
          axisLabel: {
            formatter: '${value}' 
          },
          name: '价格 (USD)',
          nameTextStyle: {
            color: '#666'
          },
          splitLine: {
            lineStyle: {
              color: '#f0f0f0'
            }
          },
          axisLine: {
            show: false
          }
        },
        series: this.createBarChartSeries(seriesData)
      };

      this.barChart.setOption(option, true);
      this.barChart.resize();
    },
    
    // 折线图数据获取和更新
    async fetchInitialData() {
      try {
        const { startTime, endTime } = this.calculateStartDate(this.selectedRange);
        
        // 初始获取默认选中公司的数据
        const fetchPromises = this.selectedCompanies.map(async company => {
          const stockData = await advancedSearchStockData(company, startTime, endTime);
          this.allStockData[company] = stockData;
        });

        await Promise.all(fetchPromises);
        
        // 对齐时间轴
        this.alignTimeAxis();
        
        // 初始渲染
        this.updateLineChartSeries();
      } catch (error) {
        console.error('获取数据失败:', error);
      }
    },

    async handleTimeRangeChange() {
      try {
        const { startTime, endTime } = this.calculateStartDate(this.selectedRange);
        
        const fetchPromises = this.allCompanies.map(async company => {
          if (this.selectedCompanies.includes(company) || this.allStockData[company]) {
            const stockData = await advancedSearchStockData(company, startTime, endTime);
            this.allStockData[company] = stockData;
          }
        });

        await Promise.all(fetchPromises);
        
        // 重新对齐时间轴
        this.alignTimeAxis();
        
        // 更新图表
        this.updateLineChartSeries();
      } catch (error) {
        console.error('更新时间范围失败:', error);
      }
    },

    handleCompanySelectionChange() {
      this.updateLineChartSeries();
    },

    calculateStartDate(range) {
      const endTime = new Date();
      let startTime = new Date();

      switch (range) {
        case '1d':
          startTime.setDate(endTime.getDate() - 1);
          break;
        case '1w':
          startTime.setDate(endTime.getDate() - 7);
          break;
        case '1m':
          startTime.setMonth(endTime.getMonth() - 1);
          break;
        case '3m':
          startTime.setMonth(endTime.getMonth() - 3);
          break;
        case '6m':
          startTime.setMonth(endTime.getMonth() - 6);
          break;
        default:
          startTime.setMonth(endTime.getMonth() - 3);
      }

      return {
        startTime: startTime.toISOString(),
        endTime: endTime.toISOString(),
      };
    },

    // 对齐时间轴
    alignTimeAxis() {
      const allTimestamps = new Set();
      this.selectedCompanies.forEach(company => {
        if (this.allStockData[company]) {
          this.allStockData[company].forEach(item => allTimestamps.add(item.timestamp));
        }
      });

      this.timestamps = Array.from(allTimestamps).sort(
        (a, b) => new Date(a) - new Date(b)
      );

      this.alignedData = {};
      this.selectedCompanies.forEach(company => {
        if (!this.allStockData[company]) return;
        
        const originalData = this.allStockData[company];
        const valueMap = new Map();
        originalData.forEach(item => valueMap.set(item.timestamp, parseFloat(item.last)));

        this.alignedData[company] = this.timestamps.map(timestamp => {
          return {
            timestamp,
            last: valueMap.get(timestamp) || null,
          };
        });
      });
    },

    updateLineChartSeries() {
      if (!this.lineChart) return;

      // 准备数据
      const series = [];
      const validCompanies = this.selectedCompanies.filter(company => 
        this.alignedData[company]
      );

      const colors = [
        '#FF6B6B', '#4ECDC4', '#45B7D1', '#96CEB4', '#FFEAA7', 
        '#DDA0DD', '#98D8C8', '#F7DC6F', '#BB8FCE', '#85C1E9',
        '#F8C471', '#82E0AA', '#D2B4DE', '#85C1E9', '#F1948A',
        '#82E0AA', '#F8C471', '#BB8FCE', '#D2B4DE', '#85C1E9'
      ];

      validCompanies.forEach((company, index) => {
        const data = this.alignedData[company];
        const validData = [];
        const timestamps = [];
        data.forEach((item, index) => {
          if (item.last !== null) {
            validData.push(item.last);
            timestamps.push(this.timestamps[index]);
          }
        });

        const colorIndex = index % colors.length;
        
        series.push({
          name: company,
          type: 'line',
          data: validData,
          smooth: true,
          symbol: 'circle',
          symbolSize: 4,
          lineStyle: { 
            width: 2,
            color: colors[colorIndex]
          },
          itemStyle: {
            color: colors[colorIndex],
            borderColor: '#fff',
            borderWidth: 2,
            shadowBlur: 3,
            shadowColor: colors[colorIndex]
          },
          emphasis: {
            symbolSize: 8,
            itemStyle: {
              color: colors[colorIndex],
              borderWidth: 3,
              shadowBlur: 5,
              shadowColor: colors[colorIndex]
            }
          },
          showSymbol: false
        });
      });

      const option = {
        title: { 
          text: `多公司股票价格对比 (${this.selectedRange})`,
          left: 'center',
          textStyle: {
            fontSize: 16,
            fontWeight: 'bold',
            color: '#333'
          },
          subtext: new Date().toLocaleDateString(),
          subtextStyle: {
            color: '#666',
            fontSize: 12
          }
        },
        legend: { 
          data: validCompanies, 
          bottom: 0,
          left: 'center',
          type: 'scroll',
          textStyle: {
            color: '#666',
            fontSize: 12
          },
          selected: validCompanies.reduce((acc, company) => {
            acc[company] = true;
            return acc;
          }, {})
        },
        grid: {
          left: '3%',
          right: '4%',
          bottom: '15%',
          top: '15%',
          containLabel: true
        },
        xAxis: {
          type: 'category',
          data: this.timestamps.map(t => {
            const date = new Date(t);
            return this.selectedRange === '1d' 
              ? date.toLocaleTimeString([], {hour: '2-digit', minute:'2-digit'}) 
              : date.toLocaleDateString([], {month: 'short', day: 'numeric'});
          }),
          axisLabel: {
            interval: this.selectedRange === '1d' ? 'auto' : 
                     this.selectedRange === '1w' ? 24 : 
                     Math.max(Math.floor(this.timestamps.length / 10), 1),
            rotate: this.selectedRange !== '1d' ? 45 : 0,
            color: '#666',
            fontSize: 12
          },
          axisLine: {
            lineStyle: {
              color: '#ddd'
            }
          },
          splitLine: {
            show: false
          }
        },
        yAxis: {
          type: 'value',
          axisLabel: {
            formatter: '${value}',
            color: '#666',
            fontSize: 12
          },
          name: '价格 (USD)',
          nameTextStyle: {
            color: '#666',
            fontSize: 12
          },
          axisLine: {
            show: false
          },
          splitLine: {
            lineStyle: {
              color: '#f0f0f0'
            }
          }
        },
        series,
        animationDuration: 1000,
        animationEasingUpdate: 'quinticInOut'
      };

      this.lineChart.setOption(option, true);
      this.$nextTick(() => {
        this.lineChart.resize();
      });
    },
    
    // 饼图数据处理
    preparePieChartData() {
      return Object.keys(this.industryCompanies).map(industry => {
        return {
          name: industry,
          value: this.industryCompanies[industry].length,
          itemStyle: {
            color: this.industryColors[industry]
          }
        };
      });
    },
    
    createPieChartSeries(pieData) {
      return [
        {
          name: '公司数量',
          type: 'pie',
          radius: ['40%', '60%'],
          center: ['40%', '50%'],
          avoidLabelOverlap: false,
          itemStyle: {
            borderRadius: 8,
            borderColor: '#fff',
            borderWidth: 2,
            shadowBlur: 10,
            shadowColor: 'rgba(0, 0, 0, 0.1)'
          },
          label: {
            show: false,
            position: 'outside',
            formatter: '{b}: {c}家',
            fontSize: 12
          },
          emphasis: {
            label: {
              show: false,
              fontSize: 12,
              fontWeight: 'bold'
            }
          },
          labelLine: {
            show: false 
          },
          data: pieData
        }
      ];
    },
    
    // 柱状图数据处理
    prepareBarChartData() {
      const seriesData = [];
      const categories = [];
      
      this.allCompanies.forEach((company) => {
        if (this.latestPrices[company] !== undefined) {
          let companyIndustry = '';
          for (const [industry, companies] of Object.entries(this.industryCompanies)) {
            if (companies.includes(company)) {
              companyIndustry = industry;
              break;
            }
          }
          
          seriesData.push({
            value: this.latestPrices[company],
            itemStyle: {
              color: this.industryColors[companyIndustry] || '#666'
            }
          });
          categories.push(company);
        }
      });
      
      return { seriesData, categories };
    },
    
    createBarChartSeries(seriesData) {
      return [
        {
          name: '股票价格',
          type: 'bar',
          data: seriesData,
          barWidth: '60%',
          label: {
            show: false,
            position: 'top',
            fontSize: 8,
            color: '#666'
          },
          emphasis: {
            label: {
              show: true,
              fontSize: 12,
              fontWeight: 'bold'
            }
          }
        }
      ];
    }
  },
  beforeUnmount() {
    if (this.pieChart) {
      this.pieChart.dispose();
    }
    if (this.barChart) {
      this.barChart.dispose();
    }
    if (this.lineChart) {
      this.lineChart.dispose();
    }
    window.removeEventListener('resize', this.handleResize);
  },
};
</script>

<style scoped>
.stock-dashboard {
  display: flex;
  flex-direction: column;
  height: 175vh;
  padding: 20px;
  margin: 0;
  background-color: #f5f7fa;
  font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
}

/* 大卡片样式 */
.dashboard-card {
  background: white;
  border-radius: 16px;
  box-shadow: 0 8px 30px rgba(0, 0, 0, 0.1);
  overflow: hidden;
  flex: 1;
  display: flex;
  flex-direction: column;
}

/* 顶部标题栏 */
.dashboard-header {
  background: linear-gradient(135deg, #5470C6 0%, #73C0DE 100%);
  color: white;
  padding: 24px 30px;
}

.dashboard-header h1 {
  margin: 0;
  font-size: 24px;
  font-weight: 600;
}

.dashboard-header p {
  margin: 8px 0 0;
  font-size: 14px;
  opacity: 0.9;
}

/* 上部图表容器 */
.top-charts-container {
  display: flex;
  gap: 20px;
  padding: 20px;
  overflow: hidden;
  height: 420px;
}

/* 下部图表容器 */
.bottom-chart-container {
  display: flex;
  flex: 1;
  padding: 20px;
  overflow: hidden;
}

/* 图表包装器通用样式 */
.chart-wrapper {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: white;
  border-radius: 12px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
  padding: 20px;
  transition: transform 0.3s ease, box-shadow 0.3s ease;
}

.chart-wrapper:hover {
  transform: translateY(-5px);
  box-shadow: 0 8px 30px rgba(0, 0, 0, 0.12);
}

/* 饼图样式 */
.pie-chart {
  max-width: 500px;
}

/* 柱状图样式 */
.bar-chart {
  flex: 2;
}

/* 折线图样式 */
.line-chart {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.line-chart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
}

.line-chart-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #333;
}

.line-chart-controls {
  display: flex;
  align-items: center;
}

/* 公司选择面板 */
.company-selection-panel {
  margin-top: 15px;
  padding-top: 15px;
  border-top: 1px solid #f0f0f0;
}

.company-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  max-height: 100px;
  overflow-y: auto;
  padding: 5px;
}

.company-list label {
  display: flex;
  align-items: center;
  font-size: 12px;
  padding: 5px 10px;
  cursor: pointer;
  transition: background-color 0.2s, border-radius 0.2s;
  border-radius: 6px;
  background-color: #f8f9fa;
}

.company-list label:hover {
  background-color: #e9ecef;
}

.company-list input {
  margin-right: 8px;
  width: 14px;
  height: 14px;
  accent-color: #5470C6;
}

/* 图表容器样式 */
.chart {
  flex: 1;
  width: 100%;
  min-height: 300px;
}

/* 控制面板样式 */
.control-item {
  display: flex;
  align-items: center;
  margin-left: 15px;
}

.control-item label {
  font-size: 12px;
  color: #666;
  margin-right: 8px;
}

.control-item select {
  padding: 6px 10px;
  border: 1px solid #ced4da;
  border-radius: 6px;
  background: #fff;
  font-size: 12px;
  color: #495057;
  transition: border-color 0.3s;
}

.control-item select:focus {
  border-color: #5470C6;
  outline: none;
}

/* 响应式调整 */
@media (max-width: 1200px) {
  .top-charts-container {
    flex-direction: column;
    height: auto;
  }
  
  .pie-chart {
    max-width: none;
    height: 400px;
  }
}

@media (max-width: 768px) {
  .dashboard-header {
    padding: 15px 20px;
  }
  
  .dashboard-header h1 {
    font-size: 20px;
  }
  
  .top-charts-container,
  .bottom-chart-container {
    padding: 10px;
  }
  
  .chart-wrapper {
    padding: 15px;
  }
  
  .line-chart-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
  }
  
  .line-chart-controls {
    width: 100%;
    justify-content: space-between;
  }
  
  .control-item {
    margin-left: 0;
  }
  
  .company-list {
    max-height: 80px;
  }
}

/* 滚动条样式优化 */
::-webkit-scrollbar {
  width: 8px;
  height: 8px;
}

::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 4px;
}

::-webkit-scrollbar-thumb {
  background: #ccc;
  border-radius: 4px;
}

::-webkit-scrollbar-thumb:hover {
  background: #bbb;
}
</style>
