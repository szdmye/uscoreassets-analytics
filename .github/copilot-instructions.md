## 目的
为 AI 编码助手（如 Copilot / 自动化代理）快速提供本仓库的关键上下文：架构概览、开发/构建/调试命令、项目约定与集成点。本文档只记录可在代码中发现的约定与可复现的命令。

## 快照（大局）
- 后端：Spring Boot（父 POM 版本 3.5.6），项目位于 `backend/`。Java 版本在 `pom.xml` 中为 25（请注意本地 JDK 版本兼容性）。
- 前端：Vue 3（vue-cli）项目位于 `frontend/`，使用 Pinia、ECharts、Chart.js 等库实现图表与状态管理。
- 通信：开发环境中前端通过 `frontend/vue.config.js` 把 `'/api'` 代理到后端 `http://localhost:8081`（请注意后端端口），生产环境后端会直接托管静态文件（`src/main/resources/static` / `target/classes/static`）。

## 重要文件位置（速查）
- 后端 POM：`backend/pom.xml`
- 后端配置：`backend/src/main/resources/application.properties`（包含 server.port、datasource 等；不要硬编码敏感信息）
- 后端源码：`backend/src/main/java/com/example/test1/`（控制器、服务、实体按包组织）
- 后端测试：`backend/src/test/java/`，测试报告位于 `backend/target/surefire-reports/`。
- 前端源码：`frontend/src/`（组件：`components/`、API：`services/stockDataApi.js`、状态：`stores/stockStore.js`）

## 构建与本地运行（具体、可复制）
注意：下面为 Windows PowerShell 示例，仓库包含 Maven wrapper（`mvnw.cmd`）和 npm/yarn 脚本。

- 后端（开发 - 直接运行）：
  - cd backend; .\mvnw.cmd spring-boot:run
- 后端（打包并运行 Jar）：
  - cd backend; .\mvnw.cmd package
  - java -jar backend\target\test1-0.0.1-SNAPSHOT.jar
- 后端（运行测试）：
  - cd backend; .\mvnw.cmd test

- 前端（开发）：
  - cd frontend; yarn install; yarn serve
  - 或使用 npm: cd frontend; npm install; npm run serve
  - dev-server 会把以 `/api` 开头的请求代理到 `http://localhost:8081`（参见 `vue.config.js`）。

- 前端（打包并部署到后端静态目录）：
  - cd frontend; yarn build  # 生成 dist/
  - 将 dist 的内容复制到后端静态资源目录，例如（PowerShell）:
    Copy-Item -Path .\dist\* -Destination ..\backend\src\main\resources\static -Recurse -Force
  - 然后在 backend 中运行 `mvnw package` 以把静态文件一起打入 Jar。

## 项目特定约定与注意点
- dev proxy：`vue.config.js` 中把 `/api` pathRewrite 为后端真实路径（移除 `^/api`），因此前端请求应使用 `/api/xxx`，后端对应控制器映射为 `/xxx`。
- 静态资源处理：仓库当前没有自动把前端 build 嵌入到 Maven 编译流程（pom.xml 未配置 frontend-maven-plugin），所以构建流程通常是手动先构建前端然后复制到后端资源目录再打包。
- 数据库：`application.properties` 中默认配置为 MySQL（localhost:3306，schema `stock`）并包含示例凭据。请在开发/部署时改用 Spring profiles 或环境变量来避免将秘密提交到仓库。
- Java/Lombok：项目依赖 Lombok（在 POM 中作为 optional），编辑器/IDE 需要启用 Lombok 注解处理以避免编译/IDE 错误。

## 调试与常见快速排查
- 前端看不到后端数据：确认后端运行在 `8081`（或 application.properties 中的 server.port），并且 dev-server 的 proxy 指向该端口。
- 后端启动失败：检查 `pom.xml` 中的 Java 版本与本地 JDK 版本是否兼容；打开日志查看 SQL 连接错误（`spring.datasource.*`）。
- 静态文件在 Jar 中缺失：确保先执行 frontend build 并把 dist 内容复制到 `backend/src/main/resources/static`，然后再 `mvnw package`。

## 代码示例（从仓库中直接引用）
- 前端 API 调用示例：`frontend/src/services/stockDataApi.js` 使用 axios 调用 `/api/...`。
- 前端状态示例：`frontend/src/stores/stockStore.js` 使用 Pinia 管理股票数据。
- Chart/组件示例：`frontend/src/components/VolumeChart.vue`（当前正在编辑的组件）与 `StockKLineChart.vue` 等展示了 ECharts/Chart.js 使用模式。

## 安全与敏感信息
- 仓库中 `application.properties` 含有数据库凭据样例；不要将真实密码提交到公共仓库。优先使用环境变量或 Spring Profiles（`application-{profile}.properties`）。

如果以上有任何不完整或不准确的地方，请指出（例如：常用端口、CI 脚本、私有镜像仓库地址或未包含的自动化步骤），我会据此迭代并合并到本文件。谢谢！
