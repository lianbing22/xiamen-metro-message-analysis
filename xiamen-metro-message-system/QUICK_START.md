# 快速启动指南

## 项目概述

厦门地铁设备报文分析系统基础架构已完成搭建，包含以下核心组件：

- ✅ **前端**: Vue 3 + TypeScript + Element Plus + Vite
- ✅ **后端**: Spring Boot 3.x + Java 17 + PostgreSQL + Redis
- ✅ **数据库**: PostgreSQL 15 完整表结构设计
- ✅ **容器化**: Docker + Docker Compose 配置
- ✅ **CI/CD**: GitHub Actions 流水线
- ✅ **文档**: 完整的API文档和部署指南

## 快速启动

### 1. 环境准备

确保已安装以下软件：
- Docker 20.10+
- Docker Compose 2.0+
- Git 2.25+

### 2. 一键启动开发环境

```bash
# 进入项目目录
cd /Users/a1234/xiamen-metro-message-system

# 运行安装脚本
chmod +x scripts/setup-dev.sh
./scripts/setup-dev.sh
```

### 3. 手动启动（可选）

如果需要手动启动各个组件：

```bash
# 1. 启动基础设施
docker-compose -f docker/docker-compose.dev.yml up -d postgres redis

# 2. 启动后端（新终端）
cd backend
./mvnw spring-boot:run

# 3. 启动前端（新终端）
cd frontend
npm install
npm run dev
```

### 4. 访问系统

- **前端界面**: http://localhost:3000
- **后端API**: http://localhost:8080
- **API文档**: http://localhost:8080/swagger-ui.html

### 5. 默认登录账号

| 用户名 | 密码 | 角色 |
|--------|------|------|
| admin | admin | 系统管理员 |
| operator | operator | 操作员 |
| analyst | analyst | 分析员 |

## 生产环境部署

```bash
# 1. 创建环境配置
cp docker/.env.dev .env.prod
# 编辑 .env.prod 文件，设置生产环境参数

# 2. 执行生产部署
chmod +x scripts/deploy-prod.sh
./scripts/deploy-prod.sh

# 3. 检查部署状态
docker-compose -f docker/docker-compose.prod.yml ps
```

## 项目结构

```
xiamen-metro-message-system/
├── frontend/                 # Vue3前端项目
│   ├── src/
│   │   ├── api/             # API接口
│   │   ├── components/      # 组件
│   │   ├── stores/          # 状态管理
│   │   ├── types/           # TypeScript类型定义
│   │   └── views/           # 页面视图
│   ├── package.json
│   ├── vite.config.ts
│   └── Dockerfile.prod
├── backend/                 # Spring Boot后端项目
│   ├── src/main/java/       # Java源码
│   ├── src/main/resources/  # 配置文件
│   ├── pom.xml
│   └── Dockerfile.prod
├── database/                # 数据库脚本
│   ├── init/               # 初始化脚本
│   └── migrations/         # 表结构脚本
├── docker/                 # Docker配置
│   ├── docker-compose.dev.yml
│   ├── docker-compose.prod.yml
│   └── nginx/              # Nginx配置
├── docs/                   # 项目文档
│   ├── api/               # API文档
│   ├── deployment/        # 部署文档
│   └── requirements/      # 需求文档
├── scripts/               # 部署脚本
│   ├── setup-dev.sh      # 开发环境设置
│   └── deploy-prod.sh    # 生产环境部署
└── .github/workflows/     # CI/CD配置
```

## 核心功能模块

### 1. 设备管理
- 设备注册与配置
- 设备状态监控
- 设备分组管理
- 设备参数设置

### 2. 报文管理
- 实时报文采集
- 报文格式解析
- 报文数据存储
- 历史数据查询

### 3. 数据分析
- 实时数据分析
- 历史趋势分析
- 异常检测
- 智能诊断

### 4. 告警管理
- 多级告警机制
- 告警通知推送
- 告警处理流程
- 告警统计分析

### 5. 用户权限
- 用户管理
- 角色权限控制
- 操作审计日志
- 多因素认证

## 数据库设计

### 核心数据表
- `devices` - 设备信息表
- `messages` - 报文数据表
- `analysis_results` - 分析结果表
- `alerts` - 告警信息表
- `users` - 用户管理表
- `operation_logs` - 操作日志表

### 数据表关系
- 设备与报文：一对多关系
- 报文与分析结果：一对一关系
- 设备与告警：一对多关系
- 用户与操作日志：一对多关系

## API接口

### 认证接口
- `POST /api/auth/login` - 用户登录
- `POST /api/auth/logout` - 用户登出
- `GET /api/auth/user` - 获取用户信息

### 设备管理接口
- `GET /api/devices` - 获取设备列表
- `POST /api/devices` - 创建设备
- `PUT /api/devices/{id}` - 更新设备
- `DELETE /api/devices/{id}` - 删除设备

### 报文管理接口
- `GET /api/messages` - 获取报文列表
- `POST /api/messages` - 创建报文
- `GET /api/messages/statistics` - 报文统计

### 告警管理接口
- `GET /api/alerts` - 获取告警列表
- `POST /api/alerts/{id}/acknowledge` - 确认告警
- `POST /api/alerts/{id}/resolve` - 解决告警

## 开发指南

### 前端开发
```bash
cd frontend
npm install          # 安装依赖
npm run dev          # 启动开发服务器
npm run build        # 构建生产版本
npm run lint         # 代码检查
npm run type-check   # 类型检查
```

### 后端开发
```bash
cd backend
./mvnw spring-boot:run    # 启动开发服务器
./mvnw test              # 运行测试
./mvnw clean package     # 构建项目
```

### 数据库操作
```bash
# 查看数据库状态
docker-compose -f docker/docker-compose.dev.yml exec postgres psql -U metro_user -d xiamen_metro_message

# 导出数据
docker-compose -f docker/docker-compose.dev.yml exec postgres pg_dump -U metro_user xiamen_metro_message > backup.sql

# 导入数据
docker-compose -f docker/docker-compose.dev.yml exec -T postgres psql -U metro_user xiamen_metro_message < backup.sql
```

## 监控和运维

### 健康检查
```bash
# 应用健康状态
curl http://localhost:8080/actuator/health

# 数据库连接状态
curl http://localhost:8080/actuator/health/db

# Redis连接状态
curl http://localhost:8080/actuator/health/redis
```

### 日志查看
```bash
# 查看所有服务日志
docker-compose -f docker/docker-compose.dev.yml logs

# 查看特定服务日志
docker-compose -f docker/docker-compose.dev.yml logs backend
docker-compose -f docker/docker-compose.dev.yml logs frontend
docker-compose -f docker/docker-compose.dev.yml logs postgres
```

### 系统监控
- **Prometheus指标**: http://localhost:8080/actuator/prometheus
- **应用信息**: http://localhost:8080/actuator/info
- **系统指标**: http://localhost:8080/actuator/metrics

## 故障排除

### 常见问题

1. **数据库连接失败**
   ```bash
   # 检查数据库状态
   docker-compose -f docker/docker-compose.dev.yml ps postgres
   docker-compose -f docker/docker-compose.dev.yml logs postgres
   ```

2. **前端无法访问后端**
   ```bash
   # 检查后端服务状态
   curl http://localhost:8080/actuator/health
   # 检查网络连接
   docker network ls
   ```

3. **Redis连接失败**
   ```bash
   # 检查Redis状态
   docker-compose -f docker/docker-compose.dev.yml exec redis redis-cli ping
   ```

### 性能优化建议

1. **数据库优化**
   - 定期执行VACUUM和ANALYZE
   - 监控慢查询日志
   - 优化索引策略

2. **应用优化**
   - 调整JVM堆内存大小
   - 配置连接池参数
   - 启用应用缓存

3. **系统优化**
   - 调整Docker资源限制
   - 优化Nginx配置
   - 监控系统资源使用

## 安全配置

### 生产环境安全清单
- [ ] 更改默认密码
- [ ] 配置HTTPS证书
- [ ] 设置防火墙规则
- [ ] 启用访问日志
- [ ] 配置备份策略
- [ ] 设置监控告警

### 数据安全
- 数据库连接加密
- 敏感数据脱敏
- 定期安全扫描
- 访问权限控制

## 联系支持

- **技术支持**: support@xiamenmetro.com
- **项目文档**: https://docs.xiamenmetro.com
- **问题反馈**: https://github.com/xiamen-metro/message-analysis-system/issues

## 版本信息

- **当前版本**: 1.0.0
- **最后更新**: 2023-11-01
- **兼容性**: Java 17+, Node.js 18+, PostgreSQL 14+

---

**厦门地铁设备报文分析系统** - 现代化设备监控与分析平台