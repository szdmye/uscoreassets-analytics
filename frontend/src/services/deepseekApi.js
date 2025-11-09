import axios from 'axios';

const DEEPSEEK_API_KEY = 'myapikey';
const DEEPSEEK_API_URL = 'https://api.deepseek.com/v1/chat/completions'; 

const deepseekApi = axios.create({
  baseURL: DEEPSEEK_API_URL,
  headers: {
    'Authorization': `Bearer ${DEEPSEEK_API_KEY}`,
    'Content-Type': 'application/json'
  }
});

export const chatWithDeepSeek = async (message, conversationHistory = []) => {
  try {
    const messages = [
      {
        role: 'system',
        content: `您是一个专业的股票分析助手，专注于帮助用户：
        1. 分析股票市场数据
        2. 解释股票相关概念
        3. 提供投资建议
        4. 回答股票交易相关问题
        5. 帮助用户理解各种股票指标
        
        请保持回答专业、准确，并提醒用户投资有风险。`
      },
      ...conversationHistory,
      {
        role: 'user',
        content: message
      }
    ];

    const response = await deepseekApi.post('', {
      model: 'deepseek-chat', 
      messages: messages,
      max_tokens: 1000,
      temperature: 0.7
    });

    return {
      success: true,
      data: response.data.choices[0].message.content
    };
  } catch (error) {
    console.error('DeepSeek API调用失败:', error);
    return {
      success: false,
      error: error.response?.data?.error?.message || '网络请求失败，请稍后重试'
    };
  }
};

export default deepseekApi;