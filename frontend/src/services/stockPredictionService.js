class StockPredictionService {
  constructor() {
    this.predictions = null;
    this.lastFetchTime = null;
    this.cacheDuration = 10 * 60 * 1000; 
  }

  async loadPredictions() {
    if (this.predictions && this.lastFetchTime && 
        (Date.now() - this.lastFetchTime) < this.cacheDuration) {
      return this.predictions;
    }

    try {
      console.log('📊 加载股票预测数据...');
      
      const response = await fetch('/stock_predictions.json');
      
      if (!response.ok) {
        throw new Error(`加载预测文件失败: ${response.status}`);
      }
      
      this.predictions = await response.json();
      this.lastFetchTime = Date.now();
      
      console.log(`✅ 预测数据加载成功，共 ${Object.keys(this.predictions).length} 个公司`);
      return this.predictions;
      
    } catch (error) {
      console.error('❌ 加载预测数据失败:', error);
      throw new Error(`无法加载预测数据: ${error.message}`);
    }
  }

  // 获取单个公司的预测
  async getStockPrediction(companyName, predictionDays = 7) {
    try {
      const predictions = await this.loadPredictions();
      
      // 查找公司
      const companyKey = this.findCompanyKey(predictions, companyName);
      
      if (!companyKey) {
        throw new Error(`未找到公司 "${companyName}" 的预测数据`);
      }
      
      const prediction = predictions[companyKey];
      
      // 根据请求的天数截取预测数据
      const adjustedPrediction = this.adjustPredictionForDays(prediction, predictionDays);
      
      return {
        ...adjustedPrediction,
        companyName: companyKey,
        modelType: 'LSTM神经网络',
        priceChange: adjustedPrediction.predictedPrice - adjustedPrediction.currentPrice,
        priceChangePercent: ((adjustedPrediction.predictedPrice - adjustedPrediction.currentPrice) / adjustedPrediction.currentPrice) * 100
      };
      
    } catch (error) {
      console.error(`获取公司 ${companyName} 预测失败:`, error);
      throw error;
    }
  }

  // 查找公司键
  findCompanyKey(predictions, companyName) {
    const keys = Object.keys(predictions);
    
    // 精确匹配
    if (keys.includes(companyName)) {
      return companyName;
    }
    
    // 大小写不敏感匹配
    const lowerCompanyName = companyName.toLowerCase();
    const found = keys.find(key => key.toLowerCase() === lowerCompanyName);
    if (found) return found;
    
    // 包含匹配
    const contains = keys.find(key => 
      key.toLowerCase().includes(lowerCompanyName) || 
      lowerCompanyName.includes(key.toLowerCase())
    );
    
    return contains || null;
  }

  // 根据请求天数调整预测数据
  adjustPredictionForDays(prediction, days) {
    if (days >= prediction.predictedPrices.length) {
      return prediction; 
    }
    
    return {
      ...prediction,
      predictedPrice: prediction.predictedPrices[days - 1],
      predictionDates: prediction.predictionDates.slice(0, days),
      predictedPrices: prediction.predictedPrices.slice(0, days)
    };
  }

  // 获取所有可用公司
  async getAvailableCompanies() {
    const predictions = await this.loadPredictions();
    return Object.keys(predictions).sort();
  }

  // 获取预测数据统计
  async getPredictionStats() {
    const predictions = await this.loadPredictions();
    const companies = Object.keys(predictions);
    
    if (companies.length === 0) {
      return {
        totalCompanies: 0,
        lastUpdated: '未知',
        averageConfidence: 0,
        companies: []
      };
    }
    
    const confidenceLevels = companies.map(company => predictions[company].confidenceLevel);
    const lastUpdated = predictions[companies[0]].lastUpdated;
    
    return {
      totalCompanies: companies.length,
      lastUpdated: lastUpdated,
      averageConfidence: Math.round(confidenceLevels.reduce((a, b) => a + b, 0) / confidenceLevels.length),
      companies: companies
    };
  }

  // 获取市场总体趋势
  async getMarketTrend() {
    const predictions = await this.loadPredictions();
    const companies = Object.keys(predictions);
    
    if (companies.length === 0) {
      return { trend: 'neutral', up: 0, down: 0, total: 0 };
    }
    
    let upCount = 0;
    let downCount = 0;
    
    companies.forEach(company => {
      const prediction = predictions[company];
      if (prediction.priceChange >= 0) {
        upCount++;
      } else {
        downCount++;
      }
    });
    
    const upPercent = (upCount / companies.length) * 100;
    
    let trend = 'neutral';
    if (upPercent > 60) trend = 'bullish';
    else if (upPercent < 40) trend = 'bearish';
    
    return {
      trend,
      up: upCount,
      down: downCount,
      total: companies.length,
      upPercent: Math.round(upPercent)
    };
  }

  // 清除缓存
  clearCache() {
    this.predictions = null;
    this.lastFetchTime = null;
    console.log('🧹 预测数据缓存已清除');
  }
}

// 创建单例实例
const predictionService = new StockPredictionService();

// 导出主要函数
export const getStockPrediction = async (companyName, days = 7) => {
  return await predictionService.getStockPrediction(companyName, days);
};

export const getAvailableCompanies = async () => {
  return await predictionService.getAvailableCompanies();
};

export const getPredictionStats = async () => {
  return await predictionService.getPredictionStats();
};

export const getMarketTrend = async () => {
  return await predictionService.getMarketTrend();
};

export const clearPredictionCache = () => {
  predictionService.clearCache();
};

export default predictionService;