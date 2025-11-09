<template>
  <div class="stock-dashboard">
    <!-- 分类股票分析 -->
    <div class="dashboard-card">
      <div class="dashboard-header">
        <h1>{{ currentPortfolio }}股组合分析</h1>
        <p>数据最后更新 {{ lastUpdated.slice(0,10) || '加载中...' }}</p>
      </div>
      
      <div class="charts-container">
        <div class="chart-wrapper pie-chart">
          <div ref="pieChartRef" class="chart"></div>
        </div>
        
        <div class="chart-wrapper bar-chart">
          <div ref="priceChartRef" class="chart"></div>
        </div>
        
        <div class="chart-wrapper bar-chart">
          <div ref="changeChartRef" class="chart"></div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, watch, computed, onMounted, onBeforeUnmount } from 'vue';
import { useRoute } from 'vue-router';
import * as echarts from 'echarts';
import { advancedSearchStockData } from '@/services/stockDataApi';

export default {
  setup() {
    const route = useRoute();
    const pieChartRef = ref(null);
    const priceChartRef = ref(null);
    const changeChartRef = ref(null);
    const pieChart = ref(null);
    const priceChart = ref(null);
    const changeChart = ref(null);
    const portfolioData = ref({});
    const lastUpdated = ref('');

    const categoryMap = {
      'all': '全部',
      'tech': '科技',
      'financial': '金融',
      'industrial': '工业',
      'consume': '消费',
      'pharmaceutical': '医药'
    };

    const industryCompanies = {
      '科技': ['Amazon.com', 'Apple', 'Cisco', 'Microsoft', 'NVIDIA', 'Salesforce Inc', 'IBM'],
      '金融': ['American Express', 'Citigroup', 'Goldman Sachs', 'JPMorgan', 'Visa A', 'Travelers'],
      '工业': ['3M', 'Boeing', 'Caterpillar', 'Honeywell', 'Sherwin-Williams', 'Chevron', 'Verizon'],
      '消费': ['Coca-Cola', 'McDonald’s', 'Walmart', 'Nike', 'Walt Disney', 'Home Depot'],
      '医药': ['Amgen', 'UnitedHealth']
    };

    const industryColors = {
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

    // 计算当前分类的公司列表
    const currentPortfolioCompanies = computed(() => {
      const routeCategory = route.params.categoryname;
      const portfolioName = categoryMap[routeCategory] || '科技';
      return industryCompanies[portfolioName] || [];
    });

    // 计算当前显示的分类名称
    const currentPortfolio = computed(() => {
      const routeCategory = route.params.categoryname;
      return categoryMap[routeCategory] || '科技';
    });
    console.log(currentPortfolio.value);

    // 获取公司数据
    const fetchCompanyData = async (company) => {
      try {
        const endTime = new Date();
        const startTime = new Date();
        startTime.setHours(endTime.getHours() - 1);
        
        const stockData = await advancedSearchStockData(company, 
          startTime.toISOString(), 
          endTime.toISOString()
        );
        
        if (stockData && stockData.length > 0) {
          const latestData = stockData.sort((a, b) => 
            new Date(b.timestamp) - new Date(a.timestamp)
          )[0];
          
          portfolioData.value[company] = {
            volume: parseFloat(latestData.vol),
            changePercent: parseFloat(latestData.chgPercent.replace('%', '')),
            price: parseFloat(latestData.last)
          };
        }
      } catch (error) {
        console.error(`获取 ${company} 数据失败:`, error);
        // 设置默认值避免图表渲染问题
        portfolioData.value[company] = {
          volume: 0,
          changePercent: 0,
          price: 0
        };
      }
    };

    // 获取组合数据
    const fetchPortfolioData = async () => {
      try {
        // 重置当前组合的数据
        currentPortfolioCompanies.value.forEach(company => {
          if (portfolioData.value[company]) {
            portfolioData.value[company] = null;
          }
        });
        
        const fetchPromises = currentPortfolioCompanies.value.map(company => 
          fetchCompanyData(company)
        );
        
        await Promise.all(fetchPromises);
        lastUpdated.value = new Date().toLocaleString();
      } catch (error) {
        console.error('获取组合数据失败:', error);
      }
    };

        // 监听路由参数变化
    watch(() => route.params.categoryname, async () => {
      await fetchPortfolioData(); 
      initCharts(); 
    }, { immediate: true });


    // 初始化饼图
    const initPieChart = () => {
      if (!pieChartRef.value) return;
      
      const totalVolume = currentPortfolioCompanies.value.reduce(
        (sum, company) => sum + (portfolioData.value[company]?.volume || 0), 
        0
      );
      
      const pieData = currentPortfolioCompanies.value.map(company => ({
        name: company,
        value: totalVolume > 0 
          ? ((portfolioData.value[company]?.volume || 0) / totalVolume * 100).toFixed(2) 
          : 0
      }));
      
      pieChart.value = echarts.init(pieChartRef.value);
      pieChart.value.setOption({
        title: {
          text: '成交量占比',
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
          backgroundColor: 'rgba(255, 255, 255, 0.95)',
          borderColor: '#ddd',
          borderWidth: 1,
          borderRadius: 4,
          padding: 10,
          boxShadow: '0 3px 15px rgba(0, 0, 0, 0.15)',
          textStyle: {
            color: '#333',
            fontSize: 13
          },
          formatter: (params) => {
            return `
              <div style="font-weight:bold;margin-bottom:6px;">${params.name}</div>
              <div>成交量占比: <span style="font-weight:bold;">${params.value}%</span></div>
            `;
          }
        },
        legend: {
          type: 'scroll',
          orient: 'horizontal',
          bottom: 10,
          left: 'center',
          data: pieData.map(item => item.name),
          textStyle: {
            color: '#666',
            fontSize: 12
          },
          selectedMode: false,
          pageIconSize: 12,
          pageTextStyle: {
            color: '#666'
          }
        },
        series: [{
          name: '成交量占比',
          type: 'pie',
          radius: ['40%', '60%'],
          center: ['40%', '50%'],
          avoidLabelOverlap: false,
          itemStyle: {
            borderRadius: 8,
            shadowBlur: 10,
            shadowColor: 'rgba(0, 0, 0, 0.1)'
          },
          label: {
            show: false
          },
          emphasis: {
            label: {
              show: false
            }
          },
          labelLine: {
            show: false
          },
          data: pieData.map((item, index) => {
            const colors = Object.values(industryColors);
            return {
              ...item,
              itemStyle: {
                color: colors[index % colors.length],
                borderRadius: 4,
                shadowBlur: 10,
                shadowColor: 'rgba(0, 0, 0, 0.1)'
              }
            };
          })
        }]
      });
    };

    // 初始化价格图表
    const initPriceChart = () => {
      if (!priceChartRef.value) return;
      
      priceChart.value = echarts.init(priceChartRef.value);
      priceChart.value.setOption({
        title: {
          text: '公司价格对比',
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
            type: 'shadow',
            shadowStyle: {
              color: 'rgba(255, 255, 255, 0.8)',
              borderColor: '#ccc',
              borderWidth: 1,
              boxShadow: '0 2px 10px rgba(0, 0, 0, 0.1)'
            }
          },
          backgroundColor: 'rgba(255, 255, 255, 0.95)',
          borderColor: '#ddd',
          borderWidth: 1,
          borderRadius: 4,
          padding: 10,
          boxShadow: '0 3px 15px rgba(0, 0, 0, 0.15)',
          textStyle: {
            color: '#333',
            fontSize: 13
          },
          formatter: (params) => {
            const param = params[0];
            return `
              <div style="font-weight:bold;margin-bottom:6px;">${param.name}</div>
              <div>股票价格: <span style="font-weight:bold;">$${param.value.toFixed(2)}</span></div>
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
          data: currentPortfolioCompanies.value,
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
          name: '价格 (USD)',
          nameTextStyle: {
            color: '#666'
          },
          axisLabel: {
            formatter: '${value}'
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
        series: [{
          name: '价格',
          type: 'bar',
          data: currentPortfolioCompanies.value.map((company, index) => {
            const colors = Object.values(industryColors);
            return {
              value: portfolioData.value[company]?.price || 0,
              itemStyle: {
                color: colors[index % colors.length],
                borderRadius: [4, 4, 0, 0]
              }
            };
          }),
          barWidth: '60%',
          label: {
            show: true,
            position: 'top',
            fontWeight: 'bold',
            fontSize: 10,
          },
          emphasis: {
            itemStyle: {
              shadowBlur: 10,
              shadowColor: 'rgba(0, 0, 0, 0.2)'
            },
          }
        }]
      });
    };

    const initChangeChart = () => {
      if (!changeChartRef.value) return;
      
      changeChart.value = echarts.init(changeChartRef.value);
      changeChart.value.setOption({
        title: {
          text: '公司涨跌幅对比',
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
            type: 'shadow',
            shadowStyle: {
              color: 'rgba(255, 255, 255, 0.8)',
              borderColor: '#ccc',
              borderWidth: 1,
              boxShadow: '0 2px 10px rgba(0, 0, 0, 0.1)'
            }
          },
          backgroundColor: 'rgba(255, 255, 255, 0.95)',
          borderColor: '#ddd',
          borderWidth: 1,
          borderRadius: 4,
          padding: 10,
          boxShadow: '0 3px 15px rgba(0, 0, 0, 0.15)',
          textStyle: {
            color: '#333',
            fontSize: 13
          },
          formatter: (params) => {
            const param = params[0];
            const value = param.value;
            const color = value >= 0 ? '#ef4444' : '#10b981';
            return `
              <div style="font-weight:bold;margin-bottom:6px;">${param.name}</div>
              <div>涨跌幅: <span style="font-weight:bold;color:${color};">${(value >= 0 ? '+' : '') + value.toFixed(2)}%</span></div>
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
          data: currentPortfolioCompanies.value,
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
          name: '涨跌幅 (%)',
          nameTextStyle: {
            color: '#666'
          },
          axisLabel: {
            formatter: '{value}%'
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
        series: [{
          name: '涨跌幅',
          type: 'bar',
          data: currentPortfolioCompanies.value.map(company => 
            portfolioData.value[company]?.changePercent || 0
          ),
          barWidth: '60%',
          itemStyle: {
            color: params => {
              const value = currentPortfolioCompanies.value.map(c => 
                portfolioData.value[c]?.changePercent || 0
              )[params.dataIndex];
              return value >= 0 ? '#ef4444' : '#10b981';
            },
            borderRadius: [2, 2, 2, 2]
          },
          label: {
            show: true,
            position: 'top',
            fontWeight: 'bold',
            fontSize: 10,
          },
          emphasis: {
            itemStyle: {
              shadowBlur: 10,
              shadowColor: 'rgba(0, 0, 0, 0.2)'
            }
          }
        }]
      });
    };

    const initCharts = () => {
      disposeCharts();
      initPieChart();
      initPriceChart();
      initChangeChart();
    };

    const disposeCharts = () => {
      [pieChart.value, priceChart.value, changeChart.value].forEach(chart => {
        if (chart) {
          chart.dispose();
        }
      });
    };

    // 窗口大小变化处理
    const handleResize = () => {
      [pieChart.value, priceChart.value, changeChart.value].forEach(chart => {
        if (chart) {
          chart.resize();
        }
      });
    };

    // 生命周期钩子
    onMounted(async () => {
      await fetchPortfolioData();
      initCharts();
      window.addEventListener('resize', handleResize);
    });

    onBeforeUnmount(() => {
      disposeCharts();
      window.removeEventListener('resize', handleResize);
    });

    return {
      pieChartRef,
      priceChartRef,
      changeChartRef,
      currentPortfolio,
      lastUpdated,
      portfolioData,
      industryCompanies,
      industryColors,
      currentPortfolioCompanies
    };
  }
};
</script>

<style scoped>
.stock-dashboard {
  display: flex;
  flex-direction: column;
  height: 100vh;
  padding: 20px;
  margin: 0;
  background-color: #f5f7fa;
  font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
}

.dashboard-card {
  background: white;
  border-radius: 16px;
  box-shadow: 0 8px 30px rgba(0, 0, 0, 0.1);
  overflow: hidden;
  flex: 1;
  display: flex;
  flex-direction: column;
}

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

.charts-container {
  display: flex;
  gap: 20px;
  padding: 20px;
  flex: 1;
  overflow: hidden;
}

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

.pie-chart {
  max-width: 500px;
}

.bar-chart {
  flex: 1;
}

.chart {
  flex: 1;
  width: 100%;
  min-height: 300px;
}

@media (max-width: 1200px) {
  .charts-container {
    flex-direction: column;
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
  
  .charts-container {
    padding: 10px;
  }
  
  .chart-wrapper {
    padding: 15px;
  }
}
</style>
