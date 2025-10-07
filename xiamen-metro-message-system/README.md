# 厦门地铁设备报文分析系统

## 系统概述

厦门地铁设备报文分析系统是一个基于Vue3 + Spring Boot的现代化设备监控与报文分析平台，用于实时监控地铁设备状态、分析设备报文数据、生成告警信息并提供可视化分析界面。

## 技术栈

### 前端
- Vue 3.x + TypeScript
- Vite 构建工具
- Element Plus UI组件库
- Pinia 状态管理
- Vue Router 路由管理
- Axios HTTP客户端

### 后端
- Spring Boot 3.x
- Java 17
- Spring Data JPA
- Spring Security
- Spring WebFlux (WebSocket)
- PostgreSQL 数据库
- Redis 缓存

### 基础设施
- Docker & Docker Compose
- Nginx 反向代理
- GitHub Actions CI/CD

## 项目结构

```
xiamen-metro-message-system/
├── frontend/                 # 前端项目
├── backend/                  # 后端项目
├── database/                 # 数据库相关
├── docker/                   # Docker配置
├── docs/                     # 项目文档
├── scripts/                  # 部署脚本
└── .github/workflows/        # CI/CD配置
```

## 快速开始

### 环境要求
- Node.js 18+
- Java 17+
- PostgreSQL 14+
- Redis 6+
- Docker & Docker Compose

### 本地开发

1. 克隆项目
```bash
git clone <repository-url>
cd xiamen-metro-message-system
```

2. 启动基础设施
```bash
docker-compose -f docker/docker-compose.dev.yml up -d
```

3. 启动后端
```bash
cd backend
./mvnw spring-boot:run
```

4. 启动前端
```bash
cd frontend
npm install
npm run dev
```

## 部署

### 开发环境
```bash
docker-compose -f docker/docker-compose.dev.yml up -d
```

### 生产环境
```bash
docker-compose -f docker/docker-compose.prod.yml up -d
```

## 核心功能

- 设备状态实时监控
- 报文数据采集与分析
- 告警管理
- 数据可视化
- 用户权限管理
- 系统配置管理

## 数据库设计

系统包含以下核心数据表：
- 设备表 (devices)
- 报文表 (messages)
- 分析结果表 (analysis_results)
- 告警表 (alerts)
- 用户表 (users)

## API文档

API文档地址：http://localhost:8080/swagger-ui.html

## 贡献指南

请参考 [docs/development.md](docs/development.md)

## 许可证

MIT License