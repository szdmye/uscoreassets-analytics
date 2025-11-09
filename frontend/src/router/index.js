import { createRouter, createWebHistory } from 'vue-router'
import MainLayout from '@/components/layout/MainLayout.vue'
import MarketOverview from '@/components/MarketOverview.vue'
import StockAnalysis from '@/components/StockAnalysis.vue'
import StockPrediction from '@/components/StockPrediction.vue'
import StockNews from '../components/StockNews.vue'
import DataList from '@/components/DataList.vue'
import CategoryALL from '@/components/CategoryALL.vue'
import Category from '@/components/CategoryPart.vue'

// 路由配置
const routes = [
  {
    path: '/',
    component: MainLayout,
    children: [
      {
        path: '/',
        name: 'MarketOverview',
        component: MarketOverview,
        meta: { title: '市场概况' }
      },
      {
        path: 'stock-analysis/:name?',
        name: 'StockAnalysis',
        component: StockAnalysis,
        meta: { title: '个股分析' },
        props: (route) => ({
          name: route.params.name || null,
        }),
      },
      {
        path: 'stock-prediction',
        name: 'StockPrediction',
        component: StockPrediction,
        meta: { title: '价格预测' }
      },
      {
        path: 'stock-news',
        name: 'StockNews',
        component: StockNews,
        meta: { title: '个股资讯' }
      },
      {
        path: 'data-list',
        name: 'DataList',
        component: DataList,
        meta: { title: '数据列表' }
      },
      {
        path: 'category/all',
        name: 'CategoryALL',
        component: CategoryALL,
        meta: { title: '股票分类' }
      },
      {
        path: 'category/:categoryname?',
        name: 'Category',
        component: Category,
        meta: { title: '股票分类' }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior() {
    return { top: 0 }
  }
})

router.beforeEach((to, from, next) => {
  if (to.meta.title) {
    document.title = to.meta.title + ' | 股票分析系统'
  }
  next()
})

export default router
