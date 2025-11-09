<!-- src/App.vue（正确示例） -->
<template>
  <router-view /> <!-- 路由控制的页面会在这里渲染 -->
  <div>
    <button @click="fetchData">获取数据</button>
    <p>{{ responseData }}</p>
  </div>
</template>

<script setup></script>

<script>
import { helloApi } from './api';
 
export default {
    data() {
        return {
            responseData: ''
        };
    },
    methods: {
        async fetchData() {
            try {
                const response = await helloApi();
                this.responseData = response.data;
            } catch (error) {
                console.error('获取数据失败:', error);
            }
        }
    }
};
</script>

<style scoped>
body {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}
</style>