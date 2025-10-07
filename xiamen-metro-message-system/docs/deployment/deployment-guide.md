# 部署指南

## 1. 部署概述

厦门地铁设备报文分析系统支持多种部署方式，包括本地开发环境、测试环境和生产环境的部署。本文档详细介绍了各种部署方式的步骤和注意事项。

## 2. 环境要求

### 2.1 硬件要求

#### 最小配置
- CPU: 4核心
- 内存: 8GB
- 存储: 100GB SSD
- 网络: 100Mbps

#### 推荐配置
- CPU: 8核心
- 内存: 16GB
- 存储: 500GB SSD
- 网络: 1Gbps

#### 生产环境配置
- CPU: 16核心
- 内存: 32GB
- 存储: 1TB SSD + 10TB HDD
- 网络: 10Gbps

### 2.2 软件要求

#### 基础软件
- 操作系统: Linux (Ubuntu 20.04+, CentOS 8+)
- Docker: 20.10+
- Docker Compose: 2.0+
- Git: 2.25+

#### 数据库软件
- PostgreSQL: 14+
- Redis: 6+

#### 可选软件
- Nginx: 1.18+ (如果不使用Docker)
- Java: 17+ (如果不使用Docker)
- Node.js: 18+ (如果不使用Docker)

## 3. 本地开发环境部署

### 3.1 环境准备

1. **安装Docker和Docker Compose**
```bash
# Ubuntu/Debian
sudo apt update
sudo apt install docker.io docker-compose-plugin

# CentOS/RHEL
sudo yum install docker docker-compose-plugin

# 启动Docker服务
sudo systemctl start docker
sudo systemctl enable docker

# 添加用户到docker组
sudo usermod -aG docker $USER
```

2. **克隆项目代码**
```bash
git clone https://github.com/xiamen-metro/message-analysis-system.git
cd message-analysis-system
```

### 3.2 启动开发环境

1. **启动基础设施服务**
```bash
docker-compose -f docker/docker-compose.dev.yml up -d postgres redis
```

2. **等待数据库启动**
```bash
# 检查数据库状态
docker-compose -f docker/docker-compose.dev.yml exec postgres pg_isready -U metro_user
```

3. **初始化数据库**
```bash
# 执行数据库初始化脚本
docker-compose -f docker/docker-compose.dev.yml exec postgres psql -U metro_user -d xiamen_metro_message -f /docker-entrypoint-initdb.d/02-create-tables.sql
docker-compose -f docker/docker-compose.dev.yml exec postgres psql -U metro_user -d xiamen_metro_message -f /docker-entrypoint-initdb.d/03-insert-initial-data.sql
```

4. **启动后端服务**
```bash
cd backend
./mvnw spring-boot:run
```

5. **启动前端服务**
```bash
cd frontend
npm install
npm run dev
```

### 3.3 验证部署

1. **检查服务状态**
```bash
# 检查后端服务
curl http://localhost:8080/actuator/health

# 检查前端服务
curl http://localhost:3000
```

2. **访问应用**
- 前端界面: http://localhost:3000
- 后端API: http://localhost:8080
- API文档: http://localhost:8080/swagger-ui.html

## 4. Docker容器化部署

### 4.1 构建镜像

1. **构建前端镜像**
```bash
cd frontend
docker build -f Dockerfile.prod -t xiamen-metro-frontend:latest .
```

2. **构建后端镜像**
```bash
cd backend
docker build -f Dockerfile.prod -t xiamen-metro-backend:latest .
```

### 4.2 配置环境变量

创建环境变量文件 `.env.prod`:
```bash
# 数据库配置
DB_NAME=xiamen_metro_message
DB_USER=metro_user
DB_PASSWORD=your_secure_password
DB_URL=postgres:5432

# Redis配置
REDIS_HOST=redis
REDIS_PORT=6379
REDIS_PASSWORD=your_redis_password

# JWT配置
JWT_SECRET=your_jwt_secret_key_here

# 系统配置
SYSTEM_NAME=厦门地铁设备报文分析系统
SYSTEM_VERSION=1.0.0

# 文件上传路径
UPLOAD_PATH=/app/uploads
```

### 4.3 启动生产环境

1. **使用Docker Compose启动**
```bash
docker-compose -f docker/docker-compose.prod.yml --env-file .env.prod up -d
```

2. **查看服务状态**
```bash
docker-compose -f docker/docker-compose.prod.yml ps
```

3. **查看日志**
```bash
# 查看所有服务日志
docker-compose -f docker/docker-compose.prod.yml logs -f

# 查看特定服务日志
docker-compose -f docker/docker-compose.prod.yml logs -f backend
```

### 4.4 健康检查

```bash
# 检查各个服务健康状态
curl http://localhost/health
curl http://localhost/api/actuator/health
```

## 5. Kubernetes部署

### 5.1 准备Kubernetes配置

1. **创建Namespace**
```yaml
# namespace.yaml
apiVersion: v1
kind: Namespace
metadata:
  name: xiamen-metro
```

2. **创建ConfigMap**
```yaml
# configmap.yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: app-config
  namespace: xiamen-metro
data:
  DB_HOST: "postgres-service"
  DB_PORT: "5432"
  DB_NAME: "xiamen_metro_message"
  REDIS_HOST: "redis-service"
  REDIS_PORT: "6379"
  SYSTEM_NAME: "厦门地铁设备报文分析系统"
```

3. **创建Secret**
```yaml
# secret.yaml
apiVersion: v1
kind: Secret
metadata:
  name: app-secrets
  namespace: xiamen-metro
type: Opaque
data:
  DB_PASSWORD: <base64-encoded-password>
  JWT_SECRET: <base64-encoded-jwt-secret>
  REDIS_PASSWORD: <base64-encoded-redis-password>
```

### 5.2 部署应用

1. **部署数据库**
```yaml
# postgres-deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: postgres
  namespace: xiamen-metro
spec:
  replicas: 1
  selector:
    matchLabels:
      app: postgres
  template:
    metadata:
      labels:
        app: postgres
    spec:
      containers:
      - name: postgres
        image: postgres:15-alpine
        env:
        - name: POSTGRES_DB
          valueFrom:
            configMapKeyRef:
              name: app-config
              key: DB_NAME
        - name: POSTGRES_USER
          value: metro_user
        - name: POSTGRES_PASSWORD
          valueFrom:
            secretKeyRef:
              name: app-secrets
              key: DB_PASSWORD
        ports:
        - containerPort: 5432
        volumeMounts:
        - name: postgres-storage
          mountPath: /var/lib/postgresql/data
      volumes:
      - name: postgres-storage
        persistentVolumeClaim:
          claimName: postgres-pvc
---
apiVersion: v1
kind: Service
metadata:
  name: postgres-service
  namespace: xiamen-metro
spec:
  selector:
    app: postgres
  ports:
  - port: 5432
    targetPort: 5432
```

2. **部署Redis**
```yaml
# redis-deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: redis
  namespace: xiamen-metro
spec:
  replicas: 1
  selector:
    matchLabels:
      app: redis
  template:
    metadata:
      labels:
        app: redis
    spec:
      containers:
      - name: redis
        image: redis:7-alpine
        ports:
        - containerPort: 6379
        volumeMounts:
        - name: redis-storage
          mountPath: /data
      volumes:
      - name: redis-storage
        persistentVolumeClaim:
          claimName: redis-pvc
---
apiVersion: v1
kind: Service
metadata:
  name: redis-service
  namespace: xiamen-metro
spec:
  selector:
    app: redis
  ports:
  - port: 6379
    targetPort: 6379
```

3. **部署后端服务**
```yaml
# backend-deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: backend
  namespace: xiamen-metro
spec:
  replicas: 2
  selector:
    matchLabels:
      app: backend
  template:
    metadata:
      labels:
        app: backend
    spec:
      containers:
      - name: backend
        image: xiamen-metro-backend:latest
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "prod"
        envFrom:
        - configMapRef:
            name: app-config
        - secretRef:
            name: app-secrets
        ports:
        - containerPort: 8080
        livenessProbe:
          httpGet:
            path: /actuator/health
            port: 8080
          initialDelaySeconds: 60
          periodSeconds: 30
        readinessProbe:
          httpGet:
            path: /actuator/health
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 10
---
apiVersion: v1
kind: Service
metadata:
  name: backend-service
  namespace: xiamen-metro
spec:
  selector:
    app: backend
  ports:
  - port: 8080
    targetPort: 8080
```

### 5.3 部署到集群

```bash
# 应用所有配置
kubectl apply -f k8s/

# 检查部署状态
kubectl get pods -n xiamen-metro
kubectl get services -n xiamen-metro

# 查看日志
kubectl logs -f deployment/backend -n xiamen-metro
```

## 6. 监控和日志

### 6.1 应用监控

1. **健康检查端点**
```bash
# 应用健康状态
curl http://localhost:8080/actuator/health

# 详细健康信息
curl http://localhost:8080/actuator/health/details

# 系统指标
curl http://localhost:8080/actuator/metrics
```

2. **Prometheus监控**
```yaml
# prometheus.yml
global:
  scrape_interval: 15s

scrape_configs:
  - job_name: 'xiamen-metro-backend'
    static_configs:
      - targets: ['backend:8080']
    metrics_path: '/actuator/prometheus'
    scrape_interval: 10s
```

### 6.2 日志管理

1. **查看应用日志**
```bash
# Docker环境
docker-compose logs -f backend

# Kubernetes环境
kubectl logs -f deployment/backend -n xiamen-metro
```

2. **日志聚合配置**
```yaml
# filebeat.yml
filebeat.inputs:
- type: container
  paths:
    - '/var/lib/docker/containers/*/*.log'
  processors:
    - add_docker_metadata:
        host: "unix:///var/run/docker.sock"

output.elasticsearch:
  hosts: ["elasticsearch:9200"]
  index: "xiamen-metro-logs"
```

## 7. 备份和恢复

### 7.1 数据库备份

1. **自动备份脚本**
```bash
#!/bin/bash
# backup.sh

BACKUP_DIR="/backup/postgres"
DATE=$(date +%Y%m%d_%H%M%S)
DB_NAME="xiamen_metro_message"

# 创建备份目录
mkdir -p $BACKUP_DIR

# 执行备份
docker-compose exec postgres pg_dump -U metro_user $DB_NAME > $BACKUP_DIR/backup_$DATE.sql

# 压缩备份文件
gzip $BACKUP_DIR/backup_$DATE.sql

# 删除7天前的备份
find $BACKUP_DIR -name "backup_*.sql.gz" -mtime +7 -delete
```

2. **设置定时备份**
```bash
# 添加到crontab
0 2 * * * /path/to/backup.sh
```

### 7.2 数据恢复

```bash
# 恢复数据库
gunzip -c backup_20231101_020000.sql.gz | docker-compose exec -T postgres psql -U metro_user xiamen_metro_message
```

### 7.3 配置文件备份

```bash
# 备份配置文件
tar -czf config_backup_$(date +%Y%m%d).tar.gz docker-compose*.yml .env* nginx/
```

## 8. 安全配置

### 8.1 防火墙配置

```bash
# Ubuntu/Debian
sudo ufw allow 22/tcp
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp
sudo ufw enable

# CentOS/RHEL
sudo firewall-cmd --permanent --add-service=ssh
sudo firewall-cmd --permanent --add-service=http
sudo firewall-cmd --permanent --add-service=https
sudo firewall-cmd --reload
```

### 8.2 SSL证书配置

1. **使用Let's Encrypt**
```bash
# 安装certbot
sudo apt install certbot python3-certbot-nginx

# 获取证书
sudo certbot --nginx -d xiamen-metro.example.com
```

2. **配置Nginx SSL**
```nginx
server {
    listen 443 ssl http2;
    server_name xiamen-metro.example.com;

    ssl_certificate /etc/letsencrypt/live/xiamen-metro.example.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/xiamen-metro.example.com/privkey.pem;

    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers ECDHE-RSA-AES256-GCM-SHA512:DHE-RSA-AES256-GCM-SHA512;
    ssl_prefer_server_ciphers off;

    location / {
        proxy_pass http://frontend:3000;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    location /api/ {
        proxy_pass http://backend:8080/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

## 9. 性能优化

### 9.1 数据库优化

```sql
-- 创建索引
CREATE INDEX CONCURRENTLY idx_messages_device_timestamp
ON messages(device_id, timestamp);

-- 分析表统计信息
ANALYZE messages;

-- 查看慢查询
SELECT query, mean_time, calls
FROM pg_stat_statements
ORDER BY mean_time DESC
LIMIT 10;
```

### 9.2 应用优化

1. **JVM参数调优**
```bash
JAVA_OPTS="-Xms2g -Xmx4g -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"
```

2. **连接池配置**
```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      idle-timeout: 300000
      connection-timeout: 20000
      max-lifetime: 1200000
```

## 10. 故障排除

### 10.1 常见问题

1. **数据库连接失败**
```bash
# 检查数据库状态
docker-compose exec postgres pg_isready -U metro_user

# 检查连接配置
docker-compose logs backend | grep -i database
```

2. **Redis连接失败**
```bash
# 检查Redis状态
docker-compose exec redis redis-cli ping

# 检查网络连接
docker-compose exec backend ping redis
```

3. **内存不足**
```bash
# 查看内存使用情况
docker stats

# 调整JVM参数
export JAVA_OPTS="-Xms1g -Xmx2g"
```

### 10.2 日志分析

```bash
# 查看错误日志
docker-compose logs backend | grep ERROR

# 查看访问日志
docker-compose logs nginx | grep -v "GET /health"

# 实时监控日志
docker-compose logs -f --tail=100 backend
```

## 11. 升级指南

### 11.1 滚动升级

```bash
# 1. 备份数据
./backup.sh

# 2. 拉取新版本代码
git pull origin main

# 3. 构建新镜像
docker-compose -f docker/docker-compose.prod.yml build

# 4. 滚动更新
docker-compose -f docker/docker-compose.prod.yml up -d --no-deps backend

# 5. 验证升级
curl http://localhost/actuator/health
```

### 11.2 蓝绿部署

```bash
# 1. 部署新版本到绿色环境
kubectl apply -f k8s/green/

# 2. 验证绿色环境
kubectl get pods -n xiamen-metro-green

# 3. 切换流量
kubectl patch service backend-service -p '{"spec":{"selector":{"version":"green"}}}'

# 4. 清理蓝色环境
kubectl delete -f k8s/blue/
```

## 12. 运维脚本

### 12.1 启动脚本

```bash
#!/bin/bash
# start.sh

echo "启动厦门地铁设备报文分析系统..."

# 检查Docker状态
if ! docker info > /dev/null 2>&1; then
    echo "错误: Docker未运行"
    exit 1
fi

# 启动服务
docker-compose -f docker/docker-compose.prod.yml up -d

# 等待服务启动
echo "等待服务启动..."
sleep 30

# 健康检查
if curl -f http://localhost/health > /dev/null 2>&1; then
    echo "系统启动成功"
else
    echo "系统启动失败，请检查日志"
    docker-compose -f docker/docker-compose.prod.yml logs
fi
```

### 12.2 停止脚本

```bash
#!/bin/bash
# stop.sh

echo "停止厦门地铁设备报文分析系统..."

docker-compose -f docker/docker-compose.prod.yml down

echo "系统已停止"
```

### 12.3 监控脚本

```bash
#!/bin/bash
# monitor.sh

# 检查服务状态
check_service() {
    local service=$1
    local url=$2

    if curl -f $url > /dev/null 2>&1; then
        echo "✓ $service 正常"
    else
        echo "✗ $service 异常"
        # 发送告警通知
        # send_alert "$service 异常"
    fi
}

echo "系统健康检查:"
check_service "前端" "http://localhost"
check_service "后端" "http://localhost/api/actuator/health"
check_service "数据库" "http://localhost/api/actuator/health/db"
check_service "Redis" "http://localhost/api/actuator/health/redis"
```

## 13. 联系支持

如果在部署过程中遇到问题，请联系：

- 技术支持邮箱: support@xiamenmetro.com
- 项目仓库: https://github.com/xiamen-metro/message-analysis-system
- 文档地址: https://docs.xiamenmetro.com