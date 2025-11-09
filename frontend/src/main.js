// src/main.js
import { createApp } from 'vue'
import App from './App.vue'

// 1. 保留原有的 Bootstrap 样式与图标引入
import 'bootstrap/dist/css/bootstrap.min.css'
import 'bootstrap-icons/font/bootstrap-icons.css'

// 2. 新增：导入路由实例（对应你配置的 router/index.js）
import router from './router'

// 3. 新增：导入 Pinia 状态管理（股票数据仓库依赖）
import { createPinia } from 'pinia'

// 4. 创建 App 并挂载所有核心依赖（路由 + Pinia + 根组件）
createApp(App)
  .use(router)        // 挂载路由（关键：让路由配置生效，替换默认欢迎页）
  .use(createPinia()) // 挂载 Pinia（关键：让股票数据仓库可用）
  .mount('#app')      // 挂载根组件