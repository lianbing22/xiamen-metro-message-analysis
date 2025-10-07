# 厦门地铁水泵分析系统 - 部署和使用指南

## 🚀 快速启动

### 系统要求
- **Java**: JDK 17+
- **Maven**: 3.8+
- **PostgreSQL**: 12+
- **Redis**: 6+
- **Spring Boot**: 3.2.0

### 1. 数据库设置

#### PostgreSQL 配置
```sql
-- 创建数据库
CREATE DATABASE xiamen_metro_pump;

-- 创建用户
CREATE USER pump_user WITH PASSWORD 'pump_password';
GRANT ALL PRIVILEGES ON DATABASE xiamen_metro_pump TO pump_user;
```

#### 应用配置文件
```yaml
# application.yml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/xiamen_metro_pump
    username: pump_user
    password: pump_password
    driver-class-name: org.postgresql.Driver

  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect

  redis:
    host: localhost
    port: 6379
    password:
    database: 0

# GLM 配置
glm:
  api:
    url: https://open.bigmodel.cn/api/paas/v4/chat/completions
    key: your_glm_api_key
    model: glm-4
    timeout: 30000

# 水泵分析配置
pump:
  analysis:
    default-thresholds:
      startup-frequency: 10.0
      runtime: 480.0
      power-anomaly: 20.0
      vibration: 4.5
      energy-increase: 15.0
      temperature: 60.0
    model-config:
      version: 1.0
      prediction-window-days: 7
      confidence-threshold: 0.7
      training-data-days: 30

logging:
  level:
    com.xiamen.metro.message.service.pump: DEBUG
    com.xiamen.metro.message.repository: DEBUG
```

### 2. 启动应用

#### 使用 Maven 启动
```bash
cd /Users/a1234/xiamen-metro-message-system/backend

# 设置正确的 Java 版本
export JAVA_HOME=/opt/homebrew/opt/openjdk@17

# 启动应用
mvn spring-boot:run
```

#### 启动演示模式
```bash
# 启用演示模式
mvn spring-boot:run -Dspring-boot.run.arguments="--pump.analysis.demo.enabled=true"
```

### 3. 访问应用

#### 应用访问地址
- **API 文档**: http://localhost:8080/swagger-ui.html
- **健康检查**: http://localhost:8080/actuator/health
- **应用信息**: http://localhost:8080/actuator/info

## 📊 API 使用示例

### 1. 执行水泵分析

```bash
curl -X POST "http://localhost:8080/api/v1/pump-analysis/analyze" \
  -H "Content-Type: application/json" \
  -d '{
    "deviceId": "PUMP_001",
    "startTime": "2024-01-01 00:00:00",
    "endTime": "2024-01-08 00:00:00",
    "analysisTypes": ["STARTUP_FREQUENCY", "RUNTIME_ANALYSIS", "ENERGY_TREND", "FAULT_PREDICTION"],
    "analysisDepth": "STANDARD",
    "enableCache": false,
    "thresholdConfig": {
      "startupFrequencyThreshold": 10.0,
      "runtimeThreshold": 480.0,
      "vibrationThreshold": 4.5
    },
    "modelConfig": {
      "modelVersion": "1.0",
      "predictionWindowDays": 7,
      "confidenceThreshold": 0.7
    }
  }'
```

### 2. 快速分析

```bash
curl -X POST "http://localhost:8080/api/v1/pump-analysis/quick-analyze/PUMP_001?hours=24"
```

### 3. 批量分析

```bash
curl -X POST "http://localhost:8080/api/v1/pump-analysis/batch-analyze" \
  -H "Content-Type: application/json" \
  -d '[
    {
      "deviceId": "PUMP_001",
      "startTime": "2024-01-01 00:00:00",
      "endTime": "2024-01-08 00:00:00",
      "analysisDepth": "BASIC"
    },
    {
      "deviceId": "PUMP_002",
      "startTime": "2024-01-01 00:00:00",
      "endTime": "2024-01-08 00:00:00",
      "analysisDepth": "BASIC"
    }
  ]'
```

### 4. 获取健康摘要

```bash
curl -X GET "http://localhost:8080/api/v1/pump-analysis/health-summary/PUMP_001?since=2024-01-01 00:00:00"
```

## 🧪 测试和验证

### 1. 运行集成测试

```bash
# 运行水泵分析集成测试
mvn test -Dtest=PumpAnalysisIntegrationTest

# 运行准确率验证测试
mvn test -Dtest=PumpModelAccuracyValidationTest
```

### 2. 数据准备

#### 插入测试数据
```sql
-- 插入水泵测试数据
INSERT INTO pump_data (
    device_id, timestamp, pump_status, runtime_minutes, power_kw,
    energy_consumption_kwh, water_pressure_kpa, vibration_mm_s,
    current_amperage, voltage, water_temperature_celsius,
    created_at
) VALUES
    ('PUMP_001', '2024-01-01 08:00:00', 1, 45.5, 15.2, 12.1, 250.5, 2.1, 32.5, 380, 25.5, NOW()),
    ('PUMP_001', '2024-01-01 09:00:00', 0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0, 0.0, NOW()),
    ('PUMP_001', '2024-01-01 10:00:00', 1, 50.2, 16.8, 14.3, 245.3, 2.8, 35.2, 382, 27.1, NOW());
```

### 3. 性能测试

#### 使用 JMeter 进行性能测试
```xml
<!-- JMeter 测试计划示例 -->
<TestPlan>
  <ThreadGroup>
    <stringProp name="ThreadGroup.num_threads">50</stringProp>
    <stringProp name="ThreadGroup.ramp_time">10</stringProp>
    <stringProp name="ThreadGroup.duration">60</stringProp>
  </ThreadGroup>

  <HTTPSamplerProxy>
    <stringProp name="HTTPSampler.domain">localhost</stringProp>
    <stringProp name="HTTPSampler.port">8080</stringProp>
    <stringProp name="HTTPSampler.path">/api/v1/pump-analysis/quick-analyze/PUMP_001</stringProp>
    <stringProp name="HTTPSampler.method">GET</stringProp>
  </HTTPSamplerProxy>
</TestPlan>
```

## 📈 监控和运维

### 1. 应用监控

#### 健康检查端点
```bash
# 应用健康状态
curl http://localhost:8080/actuator/health

# 详细健康信息
curl http://localhost:8080/actuator/health/details

# 应用指标
curl http://localhost:8080/actuator/metrics
```

#### 日志配置
```xml
<!-- logback-spring.xml -->
<configuration>
    <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>logs/pump-analysis.log</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>logs/pump-analysis.%d{yyyy-MM-dd}.%i.log</fileNamePattern>
            <maxFileSize>100MB</maxFileSize>
            <maxHistory>30</maxHistory>
        </rollingPolicy>
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>

    <logger name="com.xiamen.metro.message.service.pump" level="DEBUG"/>

    <root level="INFO">
        <appender-ref ref="FILE"/>
        <appender-ref ref="STDOUT"/>
    </root>
</configuration>
```

### 2. 数据库监控

#### 关键指标监控
```sql
-- 监控水泵数据表大小
SELECT
    schemaname,
    tablename,
    pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) as size
FROM pg_tables
WHERE tablename = 'pump_data' OR tablename = 'pump_analysis_result';

-- 监控分析查询性能
SELECT
    query,
    calls,
    total_time,
    mean_time,
    stddev_time
FROM pg_stat_statements
WHERE query LIKE '%pump_%'
ORDER BY total_time DESC
LIMIT 10;
```

### 3. 性能优化建议

#### 数据库优化
```sql
-- 创建分区表（大数据量时）
CREATE TABLE pump_data_partitioned (
    LIKE pump_data INCLUDING ALL
) PARTITION BY RANGE (timestamp);

-- 按月分区
CREATE TABLE pump_data_2024_01 PARTITION OF pump_data_partitioned
    FOR VALUES FROM ('2024-01-01') TO ('2024-02-01');

-- 创建索引优化查询
CREATE INDEX CONCURRENTLY idx_pump_device_timestamp_optimized
ON pump_data(device_id, timestamp DESC);

-- 定期清理旧数据
DELETE FROM pump_data
WHERE timestamp < NOW() - INTERVAL '2 years';
```

#### 应用缓存优化
```yaml
# Redis 缓存配置
spring:
  cache:
    type: redis
    redis:
      time-to-live: 3600000  # 1小时
      cache-null-values: false
  redis:
    lettuce:
      pool:
        max-active: 20
        max-idle: 10
        min-idle: 5
```

## 🔧 故障排除

### 1. 常见问题

#### 编译错误
```bash
# 问题：Java 版本不匹配
export JAVA_HOME=/opt/homebrew/opt/openjdk@17
mvn clean compile

# 问题：依赖冲突
mvn dependency:tree
mvn dependency:resolve
```

#### 数据库连接错误
```bash
# 检查数据库连接
psql -h localhost -U pump_user -d xiamen_metro_pump

# 检查数据库权限
\l  # 列出数据库
\dt  # 列出表
```

#### 内存不足
```bash
# 增加 JVM 内存
export JAVA_OPTS="-Xms512m -Xmx2g -XX:+UseG1GC"
mvn spring-boot:run
```

### 2. 日志分析

#### 关键错误日志
```bash
# 查看应用错误日志
tail -f logs/pump-analysis.log | grep ERROR

# 查看数据库连接错误
grep "Connection" logs/pump-analysis.log

# 查看分析超时错误
grep "timeout" logs/pump-analysis.log
```

### 3. 性能调优

#### JVM 调优参数
```bash
export JAVA_OPTS="
-Xms1g
-Xmx4g
-XX:+UseG1GC
-XX:MaxGCPauseMillis=200
-XX:+UseStringDeduplication
-Dspring.profiles.active=prod
"
```

## 📋 维护检查清单

### 日常维护
- [ ] 检查应用健康状态
- [ ] 监控磁盘空间使用
- [ ] 查看错误日志
- [ ] 检查数据库连接数
- [ ] 监控 API 响应时间

### 周期性维护
- [ ] 清理过期日志文件
- [ ] 分析数据库性能
- [ ] 更新统计信息
- [ ] 备份重要数据
- [ ] 检查系统更新

### 应急处理
- [ ] 服务重启流程
- [ ] 数据恢复流程
- [ ] 故障通知机制
- [ ] 回滚方案
- [ ] 联系方式清单

---

## 📞 技术支持

如有问题，请联系：
- **技术负责人**: [姓名] [电话] [邮箱]
- **运维团队**: [团队邮箱] [值班电话]
- **文档维护**: [文档负责人]