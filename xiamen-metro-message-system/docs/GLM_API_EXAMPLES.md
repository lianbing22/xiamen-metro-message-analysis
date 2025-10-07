# GLM-4.6 API 使用示例

## 快速开始

### 1. 启动服务
```bash
cd /Users/a1234/xiamen-metro-message-system/backend
mvn spring-boot:run
```

### 2. API基础URL
```
http://localhost:8080/api/v1/glm
```

## API接口详解

### 1. 单个报文分析

**请求示例**:
```bash
curl -X POST http://localhost:8080/api/v1/glm/analyze \
  -H "Content-Type: application/json" \
  -d '{
    "messageContent": "DEVICE_ID: DEV-001 STATUS: NORMAL TEMP: 25C PRESSURE: 101.3kPa TIMESTAMP: 2024-01-01T10:00:00",
    "messageType": "STATUS",
    "deviceId": "DEV-001",
    "timestamp": 1704110400000,
    "enableCache": true,
    "analysisDepth": "DETAILED"
  }'
```

**成功响应**:
```json
{
  "success": true,
  "message": "操作成功",
  "data": {
    "analysisId": "analysis-20240101-001",
    "status": "SUCCESS",
    "summary": "设备DEV-001状态正常，温度25°C，压力101.3kPa",
    "keyFields": [
      "DEVICE_ID: DEV-001",
      "TIMESTAMP: 2024-01-01T10:00:00",
      "TEMP: 25C",
      "PRESSURE: 101.3kPa"
    ],
    "anomalies": null,
    "recommendations": [
      "继续监控设备运行状态",
      "定期检查温度和压力传感器"
    ],
    "confidenceScore": 0.95,
    "processingTimeMs": 1250,
    "analysisTime": "2024-01-01T10:00:01",
    "fromCache": false
  },
  "timestamp": "2024-01-01T10:00:01"
}
```

### 2. 错误报文分析

**请求示例**:
```bash
curl -X POST http://localhost:8080/api/v1/glm/analyze \
  -H "Content-Type: application/json" \
  -d '{
    "messageContent": "DEVICE_ID: DEV-002 ERROR: OVER_TEMP MAX_TEMP: 80C CURRENT_TEMP: 85C SYSTEM_STATUS: CRITICAL",
    "messageType": "ERROR",
    "deviceId": "DEV-002",
    "timestamp": 1704110400000,
    "enableCache": true,
    "analysisDepth": "EXPERT"
  }'
```

**成功响应**:
```json
{
  "success": true,
  "message": "操作成功",
  "data": {
    "analysisId": "analysis-20240101-002",
    "status": "SUCCESS",
    "summary": "设备DEV-002温度超限，当前温度85°C超过最大限制80°C，系统状态危急",
    "keyFields": [
      "DEVICE_ID: DEV-002",
      "CURRENT_TEMP: 85C",
      "MAX_TEMP: 80C",
      "SYSTEM_STATUS: CRITICAL"
    ],
    "anomalies": [
      {
        "type": "TEMPERATURE_OVERRIDE",
        "description": "设备温度超过安全阈值",
        "severity": "HIGH",
        "relatedField": "CURRENT_TEMP",
        "suggestedAction": "立即停机检查冷却系统"
      }
    ],
    "recommendations": [
      "立即执行紧急停机程序",
      "检查温度传感器准确性",
      "排查冷却系统故障",
      "记录异常事件并上报"
    ],
    "confidenceScore": 0.98,
    "processingTimeMs": 890,
    "analysisTime": "2024-01-01T10:00:02",
    "fromCache": false
  },
  "timestamp": "2024-01-01T10:00:02"
}
```

### 3. 批量报文分析

**请求示例**:
```bash
curl -X POST http://localhost:8080/api/v1/glm/analyze/batch \
  -H "Content-Type: application/json" \
  -d '[
    {
      "messageContent": "DEVICE_ID: DEV-001 STATUS: RUNNING SPEED: 1500rpm",
      "messageType": "STATUS",
      "deviceId": "DEV-001",
      "timestamp": 1704110400000
    },
    {
      "messageContent": "DEVICE_ID: DEV-003 COMMAND: STOP RESPONSE: ACK",
      "messageType": "CONTROL",
      "deviceId": "DEV-003",
      "timestamp": 1704110400000
    },
    {
      "messageContent": "SYSTEM: BACKUP_COMPLETE STATUS: SUCCESS SIZE: 2.5GB",
      "messageType": "SYSTEM",
      "deviceId": "SYS-001",
      "timestamp": 1704110400000
    }
  ]'
```

**成功响应**:
```json
{
  "success": true,
  "message": "操作成功",
  "data": [
    {
      "analysisId": "batch-analysis-001",
      "status": "SUCCESS",
      "summary": "设备DEV-001正常运行，转速1500rpm",
      "confidenceScore": 0.92,
      "processingTimeMs": 750,
      "fromCache": false
    },
    {
      "analysisId": "batch-analysis-002",
      "status": "SUCCESS",
      "summary": "设备DEV-003成功执行停止命令",
      "confidenceScore": 0.88,
      "processingTimeMs": 680,
      "fromCache": false
    },
    {
      "analysisId": "batch-analysis-003",
      "status": "SUCCESS",
      "summary": "系统备份完成，备份文件大小2.5GB",
      "confidenceScore": 0.95,
      "processingTimeMs": 820,
      "fromCache": false
    }
  ],
  "timestamp": "2024-01-01T10:00:05"
}
```

### 4. 服务健康检查

**请求示例**:
```bash
curl -X GET http://localhost:8080/api/v1/glm/health
```

**健康状态响应**:
```json
{
  "success": true,
  "message": "GLM服务状态: 健康",
  "data": {
    "healthy": true,
    "cacheKeys": 25,
    "availablePermissions": 950,
    "waitingThreads": 2
  },
  "timestamp": "2024-01-01T10:00:00"
}
```

**异常状态响应**:
```json
{
  "success": true,
  "message": "GLM服务状态: 异常",
  "data": {
    "healthy": false,
    "cacheKeys": 25,
    "availablePermissions": 0,
    "waitingThreads": 10
  },
  "timestamp": "2024-01-01T10:00:00"
}
```

### 5. API连接测试

**请求示例**:
```bash
curl -X POST http://localhost:8080/api/v1/glm/test
```

**成功响应**:
```json
{
  "success": true,
  "message": "GLM API测试完成",
  "data": {
    "success": true,
    "processingTimeMs": 2450,
    "fromCache": false,
    "confidenceScore": 0.85,
    "status": "SUCCESS"
  },
  "timestamp": "2024-01-01T10:00:00"
}
```

### 6. 缓存管理

**获取缓存统计**:
```bash
curl -X GET http://localhost:8080/api/v1/glm/cache/stats
```

**响应**:
```json
{
  "success": true,
  "message": "操作成功",
  "data": {
    "totalKeys": 156,
    "estimatedMemoryUsage": 159744
  },
  "timestamp": "2024-01-01T10:00:00"
}
```

**清除所有缓存**:
```bash
curl -X DELETE http://localhost:8080/api/v1/glm/cache
```

**响应**:
```json
{
  "success": true,
  "message": "操作成功",
  "data": "所有缓存已清除",
  "timestamp": "2024-01-01T10:00:00"
}
```

**清除指定缓存**:
```bash
curl -X DELETE "http://localhost:8080/api/v1/glm/cache?messageContent=test&messageType=STATUS&analysisDepth=DETAILED"
```

## 报文类型示例

### 1. 系统报文 (SYSTEM)
```json
{
  "messageContent": "SYSTEM: HEARTBEAT STATUS: ONLINE CPU: 45% MEMORY: 2.1GB DISK: 78%",
  "messageType": "SYSTEM",
  "deviceId": "SYS-MAIN",
  "timestamp": 1704110400000
}
```

### 2. 设备报文 (DEVICE)
```json
{
  "messageContent": "DEVICE_ID: PUMP-001 FLOW: 120L/min PRESSURE: 2.5bar VOLTAGE: 380V CURRENT: 15A",
  "messageType": "DEVICE",
  "deviceId": "PUMP-001",
  "timestamp": 1704110400000
}
```

### 3. 控制报文 (CONTROL)
```json
{
  "messageContent": "COMMAND: START_DEVICE TARGET: VALVE-002 PARAMS: {\"position\": 50} RESPONSE: EXECUTED",
  "messageType": "CONTROL",
  "deviceId": "CTRL-001",
  "timestamp": 1704110400000
}
```

### 4. 状态报文 (STATUS)
```json
{
  "messageContent": "STATUS_UPDATE: DOOR_OPEN SENSORS: all_clear SAFETY: engaged OPERATION: standby",
  "messageType": "STATUS",
  "deviceId": "TRAIN-001",
  "timestamp": 1704110400000
}
```

### 5. 错误报文 (ERROR)
```json
{
  "messageContent": "ERROR: SENSOR_MALFUNCTION SENSOR_ID: TEMP-005 ERROR_CODE: E101 DIAGNOSTIC: no_signal",
  "messageType": "ERROR",
  "deviceId": "TEMP-005",
  "timestamp": 1704110400000
}
```

## 错误处理

### 常见错误响应

**1. 请求验证失败**:
```json
{
  "success": false,
  "message": "报文内容不能为空",
  "timestamp": "2024-01-01T10:00:00"
}
```

**2. 批量数量超限**:
```json
{
  "success": false,
  "message": "批量分析数量不能超过100",
  "timestamp": "2024-01-01T10:00:00"
}
```

**3. 服务内部错误**:
```json
{
  "success": false,
  "message": "报文分析失败: API连接超时",
  "timestamp": "2024-01-01T10:00:00"
}
```

**4. 降级处理响应**:
```json
{
  "success": true,
  "message": "操作成功",
  "data": {
    "analysisId": "fallback-20240101-001",
    "status": "SUCCESS",
    "summary": "降级分析结果：设备状态正常 (降级分析结果)",
    "confidenceScore": 0.6,
    "processingTimeMs": 45,
    "fromCache": false,
    "errorMessage": "使用降级分析服务"
  },
  "timestamp": "2024-01-01T10:00:00"
}
```

## 性能测试

### 压力测试示例
```bash
# 并发测试
for i in {1..10}; do
  curl -X POST http://localhost:8080/api/v1/glm/analyze \
    -H "Content-Type: application/json" \
    -d '{
      "messageContent": "TEST: Load test message '$i'",
      "messageType": "STATUS",
      "deviceId": "TEST-DEV",
      "enableCache": false
    }' &
done
wait
```

### 限流测试
```bash
# 快速连续请求测试限流
for i in {1..5}; do
  curl -X POST http://localhost:8080/api/v1/glm/analyze \
    -H "Content-Type: application/json" \
    -d '{
      "messageContent": "RATE_LIMIT_TEST '$i'",
      "messageType": "SYSTEM",
      "deviceId": "RATE-TEST"
    }'
  echo "Request $i completed"
done
```

## 监控和调试

### 查看应用日志
```bash
# 查看GLM相关日志
tail -f logs/application.log | grep "glm"
```

### 查看Redis缓存
```bash
# 连接Redis查看缓存键
redis-cli
> KEYS glm:analysis:*
> GET glm:analysis:abc123def456
```

### 查看限流指标
```bash
# 访问Actuator端点
curl http://localhost:8080/actuator/metrics
curl http://localhost:8080/actuator/health
```

## 最佳实践

### 1. 缓存使用
- 对于重复的报文内容，启用缓存可以显著提高响应速度
- 调试时可以禁用缓存 (`enableCache: false`)
- 定期清理缓存以释放内存

### 2. 批量分析
- 批量请求限制在100个以内
- 大批量数据建议分批处理
- 使用异步方式处理批量结果

### 3. 错误处理
- 检查响应中的 `status` 字段判断分析是否成功
- 关注 `confidenceScore` 评估结果可信度
- `errorMessage` 字段包含详细错误信息

### 4. 性能优化
- 相同内容的报文会自动使用缓存
- 降级分析保证了服务可用性
- 监控 `processingTimeMs` 优化性能瓶颈

## SDK集成示例

### Java客户端示例
```java
// 使用RestTemplate调用API
RestTemplate restTemplate = new RestTemplate();

String url = "http://localhost:8080/api/v1/glm/analyze";
MessageAnalysisRequestDTO request = MessageAnalysisRequestDTO.builder()
    .messageContent("DEVICE_ID: DEV-001 STATUS: NORMAL")
    .messageType("STATUS")
    .deviceId("DEV-001")
    .enableCache(true)
    .build();

// 发送请求
ResponseEntity<ApiResponse<MessageAnalysisResponseDTO>> response =
    restTemplate.postForEntity(url, request,
        new ParameterizedTypeReference<ApiResponse<MessageAnalysisResponseDTO>>() {});

if (response.getStatusCode().is2xxSuccessful() && response.getBody().isSuccess()) {
    MessageAnalysisResponseDTO result = response.getBody().getData();
    System.out.println("分析结果: " + result.getSummary());
}
```

### Python客户端示例
```python
import requests
import json

url = "http://localhost:8080/api/v1/glm/analyze"
headers = {"Content-Type": "application/json"}

data = {
    "messageContent": "DEVICE_ID: DEV-001 STATUS: NORMAL TEMP: 25C",
    "messageType": "STATUS",
    "deviceId": "DEV-001",
    "enableCache": True
}

response = requests.post(url, headers=headers, json=data)

if response.status_code == 200:
    result = response.json()
    if result["success"]:
        analysis = result["data"]
        print(f"分析ID: {analysis['analysisId']}")
        print(f"摘要: {analysis['summary']}")
        print(f"置信度: {analysis['confidenceScore']}")
```

通过这些示例，您可以快速上手使用GLM-4.6集成模块进行智能报文分析。