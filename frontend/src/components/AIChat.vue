<template>
  <div class="ai-chat-container">
    <!-- AI助手 -->
    <div class="chat-header">
      <h4 class="chat-title">
        <i class="bi bi-robot"></i>
        AI股票助手
      </h4>
      <button class="close-btn" @click="$emit('close')">
        <i class="bi bi-x"></i>
      </button>
    </div>

    <!-- 消息区域 -->
    <div class="messages-container" ref="messagesContainer">
      <div 
        v-for="(message, index) in messages" 
        :key="index" 
        :class="['message', message.role]"
      >
        <div class="message-avatar">
          <i 
            :class="message.role === 'user' ? 'bi bi-person' : 'bi bi-robot'"
          ></i>
        </div>
        <div class="message-content">
          <div class="message-text">{{ message.content }}</div>
          <div class="message-time">{{ formatTime(message.timestamp) }}</div>
        </div>
      </div>
      
      <!-- 加载状态 -->
      <div v-if="isLoading" class="message assistant">
        <div class="message-avatar">
          <i class="bi bi-robot"></i>
        </div>
        <div class="message-content">
          <div class="loading-dots">
            <span></span>
            <span></span>
            <span></span>
          </div>
        </div>
      </div>
    </div>

    <!-- 输入区域 -->
    <div class="input-container">
      <div class="input-wrapper">
        <textarea
          v-model="userInput"
          @keydown.enter.exact.prevent="sendMessage"
          placeholder="输入关于股票的问题..."
          rows="1"
          ref="textareaRef"
          class="message-input"
        ></textarea>
        <button 
          @click="sendMessage" 
          :disabled="!userInput.trim() || isLoading"
          class="send-btn"
        >
          <i class="bi bi-send"></i>
        </button>
      </div>
      <div class="quick-questions">
        <button 
          v-for="(question, index) in quickQuestions" 
          :key="index"
          @click="selectQuickQuestion(question)"
          class="quick-question-btn"
        >
          {{ question }}
        </button>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, onMounted, nextTick } from 'vue';
import { chatWithDeepSeek } from '@/services/deepseekApi';

export default {
  name: 'AIChat',
  emits: ['close'],
  setup() {
    const messages = ref([]);
    const userInput = ref('');
    const isLoading = ref(false);
    const messagesContainer = ref(null);
    const textareaRef = ref(null);

    // 快捷问题
    const quickQuestions = ref([
      '今天股市行情怎么样？',
      '如何分析一只股票？',
      '什么是市盈率？',
      '推荐几只科技股',
      '如何控制投资风险？'
    ]);

    // 自动调整文本框高度
    const adjustTextareaHeight = () => {
      if (textareaRef.value) {
        textareaRef.value.style.height = 'auto';
        textareaRef.value.style.height = Math.min(textareaRef.value.scrollHeight, 120) + 'px';
      }
    };

    // 滚动到底部
    const scrollToBottom = () => {
      nextTick(() => {
        if (messagesContainer.value) {
          messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight;
        }
      });
    };

    // 发送消息
    const sendMessage = async () => {
      const message = userInput.value.trim();
      if (!message || isLoading.value) return;

      // 添加用户消息
      const userMessage = {
        role: 'user',
        content: message,
        timestamp: new Date()
      };
      messages.value.push(userMessage);
      userInput.value = '';
      adjustTextareaHeight();
      
      // 设置加载状态
      isLoading.value = true;
      scrollToBottom();

      try {
        // 调用DeepSeek API
        const result = await chatWithDeepSeek(message, messages.value.slice(-10));
        
        if (result.success) {
          // 添加AI回复
          const aiMessage = {
            role: 'assistant',
            content: result.data,
            timestamp: new Date()
          };
          messages.value.push(aiMessage);
        } else {
          // 错误处理
          const errorMessage = {
            role: 'assistant',
            content: `抱歉，我暂时无法回答这个问题。错误信息：${result.error}`,
            timestamp: new Date()
          };
          messages.value.push(errorMessage);
        }
      } catch (error) {
        console.error('发送消息失败:', error);
        const errorMessage = {
          role: 'assistant',
          content: '网络连接出现问题，请稍后重试。',
          timestamp: new Date()
        };
        messages.value.push(errorMessage);
      } finally {
        isLoading.value = false;
        scrollToBottom();
      }
    };

    // 快捷问题
    const selectQuickQuestion = (question) => {
      userInput.value = question;
      sendMessage();
    };

    // 格式化时间
    const formatTime = (timestamp) => {
      return new Date(timestamp).toLocaleTimeString('zh-CN', {
        hour: '2-digit',
        minute: '2-digit'
      });
    };

    // 监听输入变化调整高度
    onMounted(() => {
      if (textareaRef.value) {
        textareaRef.value.addEventListener('input', adjustTextareaHeight);
      }
      
      // 添加欢迎消息
      const welcomeMessage = {
        role: 'assistant',
        content: '您好！我是AI股票助手，可以帮您分析股票数据、解释投资概念、提供市场见解。请问有什么可以帮您的？',
        timestamp: new Date()
      };
      messages.value.push(welcomeMessage);
    });

    return {
      messages,
      userInput,
      isLoading,
      messagesContainer,
      textareaRef,
      quickQuestions,
      sendMessage,
      selectQuickQuestion,
      formatTime
    };
  }
};
</script>

<style scoped>
.ai-chat-container {
  display: flex;
  flex-direction: column;
  height: 500px;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  overflow: hidden;
}

.chat-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: #f8fafc;
  border-bottom: 1px solid #e5e7eb;
}

.chat-title {
  margin: 0;
  font-size: 14px;
  font-weight: 600;
  color: #1f2937;
  display: flex;
  align-items: center;
  gap: 8px;
}

.chat-title i {
  color: #2563eb;
}

.close-btn {
  background: none;
  border: none;
  font-size: 18px;
  color: #6b7280;
  cursor: pointer;
  padding: 4px;
  border-radius: 4px;
}

.close-btn:hover {
  background: #e5e7eb;
  color: #374151;
}

.messages-container {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.message {
  display: flex;
  gap: 8px;
  max-width: 100%;
}

.message.user {
  flex-direction: row-reverse;
}

.message-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.message.user .message-avatar {
  background: #2563eb;
  color: white;
}

.message.assistant .message-avatar {
  background: #10b981;
  color: white;
}

.message-content {
  max-width: 80%;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.message.user .message-content {
  align-items: flex-end;
}

.message-text {
  padding: 8px 12px;
  border-radius: 12px;
  font-size: 14px;
  line-height: 1.4;
  word-wrap: break-word;
}

.message.user .message-text {
  background: #2563eb;
  color: white;
  border-bottom-right-radius: 4px;
}

.message.assistant .message-text {
  background: #f3f4f6;
  color: #374151;
  border-bottom-left-radius: 4px;
}

.message-time {
  font-size: 11px;
  color: #9ca3af;
}

.input-container {
  padding: 16px;
  border-top: 1px solid #e5e7eb;
  background: #fafafa;
}

.input-wrapper {
  display: flex;
  gap: 8px;
  align-items: flex-end;
  margin-bottom: 12px;
}

.message-input {
  flex: 1;
  border: 1px solid #d1d5db;
  border-radius: 8px;
  padding: 8px 12px;
  font-size: 14px;
  resize: none;
  max-height: 120px;
  font-family: inherit;
}

.message-input:focus {
  outline: none;
  border-color: #2563eb;
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.1);
}

.send-btn {
  background: #2563eb;
  color: white;
  border: none;
  border-radius: 8px;
  padding: 8px 12px;
  cursor: pointer;
  flex-shrink: 0;
}

.send-btn:hover:not(:disabled) {
  background: #1d4ed8;
}

.send-btn:disabled {
  background: #9ca3af;
  cursor: not-allowed;
}

.quick-questions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.quick-question-btn {
  background: white;
  border: 1px solid #d1d5db;
  border-radius: 16px;
  padding: 4px 12px;
  font-size: 12px;
  color: #6b7280;
  cursor: pointer;
  transition: all 0.2s;
}

.quick-question-btn:hover {
  border-color: #2563eb;
  color: #2563eb;
  background: #eff6ff;
}

/* 加载动画 */
.loading-dots {
  display: flex;
  gap: 4px;
  padding: 8px 0;
}

.loading-dots span {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #9ca3af;
  animation: bounce 1.4s infinite ease-in-out both;
}

.loading-dots span:nth-child(1) { animation-delay: -0.32s; }
.loading-dots span:nth-child(2) { animation-delay: -0.16s; }

@keyframes bounce {
  0%, 80%, 100% {
    transform: scale(0);
  }
  40% {
    transform: scale(1);
  }
}

/* 滚动条样式 */
.messages-container::-webkit-scrollbar {
  width: 4px;
}

.messages-container::-webkit-scrollbar-track {
  background: #f1f1f1;
}

.messages-container::-webkit-scrollbar-thumb {
  background: #c1c1c1;
  border-radius: 2px;
}

.messages-container::-webkit-scrollbar-thumb:hover {
  background: #a8a8a8;
}
</style>