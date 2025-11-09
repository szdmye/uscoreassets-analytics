// frontend/src/services/stockDataApi.js
import axios from 'axios';

const API_BASE_URL = 'http://localhost:8081/api/stock-data'; 

export const fetchAllStockData = async (page = 0, size = 10) => {
  try {
    const response = await axios.get(`${API_BASE_URL}`, {
      params: {
        page,
        size,
        sort: 'timestamp,desc' 
      }
    });
    return response.data;
  } catch (error) {
    console.error('获取所有股票数据失败:', error);
    throw error;
  }
};

export const searchStockDataByName = async (name) => {
  try {
    const response = await axios.get(`${API_BASE_URL}/search-by-name`, {
      params: { name }
    });
    return response.data;
  } catch (error) {
    console.error('按名称搜索股票数据失败:', error);
    throw error;
  }
};

export const searchStockDataByTime = async (startTime, endTime) => {
  try {
    const response = await axios.get(`${API_BASE_URL}/search-by-time`, {
      params: { startTime, endTime }
    });
    return response.data;
  } catch (error) {
    console.error('按时间范围搜索股票数据失败:', error);
    throw error;
  }
};

export const advancedSearchStockData = async (name, startTime, endTime) => {
  try {
    const response = await axios.get(`${API_BASE_URL}/advanced-search`, {
      params: { name, startTime, endTime }
    });
    return response.data;
  } catch (error) {
    console.error('高级搜索股票数据失败:', error);
    throw error;
  }
};