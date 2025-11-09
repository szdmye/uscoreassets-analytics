# USCoreAssets Analytics

![Vue.js](https://img.shields.io/badge/Vue.js-3.x-4FC08D?logo=vuedotjs)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-6DB33F?logo=springboot)
![MongoDB](https://img.shields.io/badge/MongoDB-7.x-47A248?logo=mongodb)
![ECharts](https://img.shields.io/badge/ECharts-5.x-AA344D?logo=apacheecharts)

A full-stack US core assets price analysis and visualization platform with real-time stock monitoring, technical indicators, and price prediction.

## 🏗️ System Architecture
Frontend (Vue.js 3)
├── Vue 3 + Composition API
├── ECharts 5 for Visualization
└── Vite Build Tool

Backend (Spring Boot)
├── Spring Boot 3.x REST API
├── MongoDB Data Persistence
├── Business Logic Layer
└── Data Access Layer

Data Layer
├── Real-time Stock Data APIs
├── LSTM Prediction Models
├── NewsAPI Integration
└── DeepseekAI API

## 📁 Project Structure
uscoreassets-analytics/
├── backend/                          # Spring Boot Backend
│   ├── src/main/java/test1           # Main Package
│   │   ├── config/                   # Configuration
│   │   ├── controller/               # API Controllers
│   │   ├── service/                  # Business Logic
│   │   ├── repository/               # Data Access
│   │   ├── model/                    # Data Models
│   │   └── config/                   # Configuration
│   ├── src/main/resources/
│   │   ├── application.yml           # App Config
│   │   └── stock.csv                 # Stock Data
│   └── pom.xml                       # Maven Config
├── frontend/                         # Vue.js Frontend
│   ├── src/
│   │   ├── components/               # Components
│   │   │   ├── layout/               # Layout Components
│   │   │   ├── AIChart.vue           # AI Charts
│   │   │   ├── CategoryALL.vue       # All Categories
│   │   │   ├── CategoryPart.vue      # Partial Categories
│   │   │   ├── DataList.vue          # Data List
│   │   │   ├── MarketOverview.vue    # Market Overview
│   │   │   ├── StockAnalysis.vue     # Stock Analysis
│   │   │   ├── StockHeader.vue       # Page Header
│   │   │   ├── StockNews.vue         # Stock News
│   │   │   ├── StockPrediction.vue   # Stock Prediction
│   │   │   └── StockSidebar.vue      # Sidebar
│   │   ├── services/                 # API Services
│   │   │   ├── deepseekApi.js        # Deepseek API
│   │   │   ├── stockDataApi.js       # Stock Data API
│   │   │   └── stockPredictionService.js # Prediction Service
│   │   ├── router/                   # Vue Router
│   │   │   └── index.js              # Route Config
│   │   └── App.vue                   # Root Component
│   ├── public/                       # Static Assets
│   └── package.json                  # Dependencies
└── README.md                         # Documentation
