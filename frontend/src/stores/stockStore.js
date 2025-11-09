import { defineStore } from 'pinia'
import Papa from 'papaparse'
//import _ from 'lodash' 

/**
 * Pinia 股票数据状态管理仓库
 * 核心功能：从public目录加载CSV数据、解析、格式化、分组与状态管理
 */
export const useStockStore = defineStore('stock', {
  state: () => ({
    rawData: [],                          // 原始解析后的数据（去重前）
    processedData: {},                     // 按股票名称分组的数据（去重后）
    stockNames: [],                        // 所有股票名称（去重）
    lastUpdateTime: '',                    // 数据最后更新时间（本地化格式）
    isLoading: false,                      // 数据加载状态（用于加载动画）
    error: '',                             // 错误信息（加载/解析失败时显示）
    totalCount: 0,                         // 数据总条数（便于调试与统计）
    csvFileName: 'stock.csv'               // CSV文件名，默认为public目录下的stock.csv
  }),

  getters: {
    /**
     * 计算所有股票的最新股价统计（用于数据概览）
     * @returns 最高/最低/平均股价
     */
    stockPriceStats() {
      if (this.rawData.length === 0) return { maxPrice: 0, minPrice: 0, avgPrice: 0 };
      
      const prices = this.rawData.map(item => item.last);
      return {
        maxPrice: Math.max(...prices),
        minPrice: Math.min(...prices),
        avgPrice: parseFloat((prices.reduce((sum, price) => sum + price, 0) / prices.length).toFixed(2))
      };
    },

    /**
     * 获取指定股票的最新数据（用于快速查询）
     * @param stockName 股票名称
     * @returns 该股票最新一条数据（按时间戳排序）
     */
    getLatestStockData() {
      return (stockName) => {
        const stockData = this.processedData[stockName];
        if (!stockData || stockData.length === 0) return undefined;
        // 按时间戳降序排序，取第一条（最新数据）
        return [...stockData].sort((a, b) => new Date(b.timestamp).getTime() - new Date(a.timestamp).getTime())[0];
      };
    },

    /**
     * 获取public目录下CSV文件的完整路径
     */
    csvFilePath() {
      // 处理路径，确保正确指向public目录
      return `/${this.csvFileName.replace(/^\//, '')}`;
    }
  },

  actions: {
    /**
     * 从public目录加载CSV文件数据
     * @param {string} fileName - 可选，CSV文件名，默认使用state中的csvFileName
     */
    async loadFromPublicCsv(fileName) {
      // 如果指定了文件名，则更新状态中的文件名
      if (fileName) {
        this.csvFileName = fileName;
      }
      
      // 调用通用加载方法，不传递file参数则会从public目录加载
      await this.loadStockData();
    },

    /**
     * 从CSV文件加载数据（支持本地文件上传或public目录文件）
     * @param {File} file - 可选，CSV文件对象（上传的文件）
     */
    async loadStockData(file) {
      this.isLoading = true;
      this.error = '';
      try {
        let csvText;
        
        if (file) {
          // 1. 处理用户上传的本地文件
          csvText = await new Promise((resolve, reject) => {
            const reader = new FileReader();
            reader.onload = (e) => resolve(e.target?.result);
            reader.onerror = () => reject(new Error(`文件读取失败: ${reader.error?.message || '未知错误'}`));
            reader.readAsText(file);
          });
        } else {
          // 2. 从public目录加载CSV文件
          // 使用getter中定义的路径，确保正确指向public目录
          const response = await fetch(this.csvFilePath, {
            headers: {
              'Content-Type': 'text/csv',
              'Cache-Control': 'no-cache' // 禁用缓存，确保获取最新文件
            }
          });

          if (!response.ok) {
            // 更详细的错误信息，帮助定位问题
            if (response.status === 404) {
              throw new Error(`未找到CSV文件，请确认 ${this.csvFilePath} 已放在public目录下`);
            } else {
              throw new Error(`数据加载失败 (HTTP状态码: ${response.status})`);
            }
          }
          csvText = await response.text();
        }

        // 3. 解析CSV数据
        const parseResult = Papa.parse(csvText, {
          header: true,
          dynamicTyping: {
            last: true,
            high: true,
            low: true
          },
          skipEmptyLines: true,
          fastMode: true
        });

        if (parseResult.errors.length > 0) {
          const error = parseResult.errors[0];
          throw new Error(`CSV解析错误: ${error.message}（行号: ${error.row + 1}）`);
        }

        // 4. 处理数据
        this.rawData = parseResult.data.filter(item => 
          item.name && item.timestamp && !isNaN(item.last)
        );
        
        // 数据去重
        this.rawData = Array.from(
          new Map(this.rawData.map(item => [`${item.timestamp}-${item.name}`, item])).values()
        );

        // 5. 格式化数据
        this.rawData.forEach(item => {
          // 处理成交量
          if (item.vol_.endsWith('M')) {
            item.volNum = parseFloat(item.vol_.replace('M', '')) * 1000000;
          } else {
            item.volNum = parseFloat(item.vol_);
          }

          // 处理涨跌幅
          item.chgPercentNum = parseFloat(item['chg_%'].replace('%', ''));
        });

        // 6. 分组和统计
        this.processedData = this.rawData.reduce((acc, item) => {
          const name = item.name || ''; // 如果name不存在，则赋值为空字符串
          if (!acc[name]) {
            acc[name] = [];
          }
          acc[name].push(item);
          return acc;
        }, {});

        this.stockNames = Array.from(new Set(this.rawData.map(item => item.name))).sort();
        this.totalCount = this.rawData.length;
        this.lastUpdateTime = new Date().toLocaleString();

        console.log(`成功加载数据: 共 ${this.totalCount} 条记录，${this.stockNames.length} 只股票`);

      } catch (err) {
        this.error = err instanceof Error ? err.message : '加载数据时发生错误';
        console.error('数据加载失败:', err);
      } finally {
        this.isLoading = false;
      }
    },

    /**
     * 重置股票数据
     */
    resetStockData() {
      this.rawData = [];
      this.processedData = {};
      this.stockNames = [];
      this.lastUpdateTime = '';
      this.error = '';
      this.totalCount = 0;
    }
  }
});
