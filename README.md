# USCoreAssets Analytics

A full-stack US core assets price analysis and visualization platform with real-time stock monitoring, technical indicators, and price prediction.

## 🏗️ System Architecture

### Frontend (Vue.js 3)
- **Vue 3 + Composition API** - Modern Vue.js with Composition API
- **ECharts 5 for Visualization** - Advanced charting and data visualization
- **Vite Build Tool** - Fast development and build tooling

### Backend (Spring Boot)
- **Spring Boot 3.x REST API** - Robust backend RESTful services
- **MongoDB Data Persistence** - NoSQL database for flexible data storage
- **Business Logic Layer** - Core application business rules
- **Data Access Layer** - Database interaction and data management

### Data Layer
- **Real-time Stock Data APIs** - Live market data feeds
- **LSTM Prediction Models** - Long Short-Term Memory neural networks for price forecasting
- **NewsAPI Integration** - Financial news and sentiment analysis
- **DeepseekAI API** - AI-powered analytics and insights

## 📁 Project Structure
```
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
```
