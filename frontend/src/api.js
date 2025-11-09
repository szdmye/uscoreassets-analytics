import axios from 'axios';

const api = axios.create({
    baseURL: 'http://localhost:8081/api', // 替换为后端实际运行的端口，例如8080
    timeout: 10000,
});

export const helloApi = () => api.get('/hello');