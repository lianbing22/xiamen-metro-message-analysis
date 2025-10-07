# API接口文档

## 1. 接口概述

### 1.1 基本信息
- **基础URL**: `http://localhost:8080/api`
- **API版本**: v1.0
- **认证方式**: JWT Bearer Token
- **数据格式**: JSON
- **字符编码**: UTF-8

### 1.2 通用响应格式

#### 成功响应
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {},
  "success": true,
  "timestamp": 1699123456789
}
```

#### 错误响应
```json
{
  "code": 400,
  "message": "请求参数错误",
  "data": null,
  "success": false,
  "timestamp": 1699123456789,
  "errors": [
    {
      "field": "username",
      "message": "用户名不能为空"
    }
  ]
}
```

### 1.3 状态码说明
- `200` - 请求成功
- `201` - 创建成功
- `400` - 请求参数错误
- `401` - 未授权
- `403` - 禁止访问
- `404` - 资源不存在
- `500` - 服务器内部错误

## 2. 认证接口

### 2.1 用户登录
**POST** `/auth/login`

**请求参数:**
```json
{
  "username": "admin",
  "password": "password123",
  "captcha": "ABCD"
}
```

**响应数据:**
```json
{
  "code": 200,
  "message": "登录成功",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "refresh_token_string",
    "user": {
      "id": 1,
      "userId": "admin",
      "username": "admin",
      "email": "admin@example.com",
      "fullName": "系统管理员",
      "role": "ADMIN",
      "status": "ACTIVE"
    },
    "expiresIn": 86400
  }
}
```

### 2.2 用户登出
**POST** `/auth/logout`

**请求头:**
```
Authorization: Bearer {token}
```

### 2.3 刷新Token
**POST** `/auth/refresh`

**请求参数:**
```json
{
  "refreshToken": "refresh_token_string"
}
```

### 2.4 获取用户信息
**GET** `/auth/user`

**请求头:**
```
Authorization: Bearer {token}
```

## 3. 设备管理接口

### 3.1 获取设备列表
**GET** `/devices`

**查询参数:**
- `current` (int): 页码，默认1
- `size` (int): 每页大小，默认10
- `deviceName` (string): 设备名称（模糊查询）
- `deviceType` (string): 设备类型
- `status` (string): 设备状态

**响应数据:**
```json
{
  "code": 200,
  "data": {
    "records": [
      {
        "id": 1,
        "deviceId": "DEV001",
        "deviceName": "主变电所1号变压器",
        "deviceType": "TRANSFORMER",
        "deviceGroup": "power_system",
        "location": "主变电所",
        "ipAddress": "192.168.1.1",
        "port": 502,
        "protocol": "MODBUS",
        "status": "ACTIVE",
        "createdAt": "2023-11-01T10:00:00",
        "updatedAt": "2023-11-01T10:00:00"
      }
    ],
    "total": 100,
    "current": 1,
    "size": 10,
    "pages": 10
  }
}
```

### 3.2 获取设备详情
**GET** `/devices/{deviceId}`

### 3.3 创建设备
**POST** `/devices`

**请求参数:**
```json
{
  "deviceId": "DEV002",
  "deviceName": "新设备",
  "deviceType": "TRANSFORMER",
  "deviceGroup": "power_system",
  "location": "设备位置",
  "ipAddress": "192.168.1.100",
  "port": 502,
  "protocol": "MODBUS",
  "description": "设备描述"
}
```

### 3.4 更新设备
**PUT** `/devices/{deviceId}`

### 3.5 删除设备
**DELETE** `/devices/{deviceId}`

### 3.6 获取设备状态
**GET** `/devices/{deviceId}/status`

### 3.7 批量导入设备
**POST** `/devices/import`

**请求参数:**
- `file` (MultipartFile): Excel文件

## 4. 报文管理接口

### 4.1 获取报文列表
**GET** `/messages`

**查询参数:**
- `current` (int): 页码
- `size` (int): 每页大小
- `deviceId` (string): 设备ID
- `messageType` (string): 报文类型
- `startTime` (string): 开始时间
- `endTime` (string): 结束时间

### 4.2 获取报文详情
**GET** `/messages/{messageId}`

### 4.3 创建报文
**POST** `/messages`

### 4.4 批量创建报文
**POST** `/messages/batch`

### 4.5 报文统计
**GET** `/messages/statistics`

**响应数据:**
```json
{
  "code": 200,
  "data": {
    "totalCount": 100000,
    "todayCount": 1000,
    "thisWeekCount": 7000,
    "thisMonthCount": 30000,
    "deviceMessageCount": [
      {
        "deviceId": "DEV001",
        "deviceName": "主变电所1号变压器",
        "count": 5000
      }
    ]
  }
}
```

## 5. 数据分析接口

### 5.1 获取分析结果列表
**GET** `/analysis/results`

### 5.2 执行数据分析
**POST** `/analysis/execute`

**请求参数:**
```json
{
  "deviceId": "DEV001",
  "analysisType": "FAULT_DIAGNOSIS",
  "startTime": "2023-11-01T00:00:00",
  "endTime": "2023-11-01T23:59:59",
  "parameters": {}
}
```

### 5.3 获取分析报告
**GET** `/analysis/report/{analysisId}`

### 5.4 导出分析结果
**GET** `/analysis/export`

**查询参数:**
- `format` (string): 导出格式 (excel/csv/pdf)
- `analysisId` (string): 分析ID

## 6. 告警管理接口

### 6.1 获取告警列表
**GET** `/alerts`

**查询参数:**
- `current` (int): 页码
- `size` (int): 每页大小
- `alertLevel` (string): 告警级别
- `alertStatus` (string): 告警状态
- `deviceId` (string): 设备ID

### 6.2 获取告警详情
**GET** `/alerts/{alertId}`

### 6.3 确认告警
**POST** `/alerts/{alertId}/acknowledge`

**请求参数:**
```json
{
  "acknowledgedBy": "admin",
  "remark": "已确认告警"
}
```

### 6.4 解决告警
**POST** `/alerts/{alertId}/resolve`

**请求参数:**
```json
{
  "resolvedBy": "admin",
  "solution": "故障已修复",
  "remark": "更换传感器"
}
```

### 6.5 获取告警统计
**GET** `/alerts/statistics`

### 6.6 获取告警趋势
**GET** `/alerts/trends`

**查询参数:**
- `period` (string): 时间周期 (day/week/month)
- `startTime` (string): 开始时间
- `endTime` (string): 结束时间

## 7. 用户管理接口

### 7.1 获取用户列表
**GET** `/users`

### 7.2 创建用户
**POST** `/users`

**请求参数:**
```json
{
  "username": "newuser",
  "password": "password123",
  "email": "newuser@example.com",
  "fullName": "新用户",
  "phone": "13800138000",
  "department": "运维部",
  "role": "OPERATOR"
}
```

### 7.3 更新用户
**PUT** `/users/{userId}`

### 7.4 删除用户
**DELETE** `/users/{userId}`

### 7.5 修改密码
**POST** `/users/{userId}/change-password`

### 7.6 重置密码
**POST** `/users/{userId}/reset-password`

## 8. 系统配置接口

### 8.1 获取系统配置
**GET** `/configs`

### 8.2 更新系统配置
**PUT** `/configs`

### 8.3 获取数据字典
**GET** `/dicts/{dictType}`

### 8.4 获取系统信息
**GET** `/system/info`

**响应数据:**
```json
{
  "code": 200,
  "data": {
    "systemName": "厦门地铁设备报文分析系统",
    "version": "1.0.0",
    "buildTime": "2023-11-01T10:00:00",
    "uptime": 86400,
    "environment": "production"
  }
}
```

## 9. 文件管理接口

### 9.1 文件上传
**POST** `/files/upload`

**请求参数:**
- `file` (MultipartFile): 文件
- `type` (string): 文件类型

### 9.2 文件下载
**GET** `/files/download/{fileId}`

### 9.3 文件删除
**DELETE** `/files/{fileId}`

## 10. 统计分析接口

### 10.1 仪表盘数据
**GET** `/dashboard/stats`

**响应数据:**
```json
{
  "code": 200,
  "data": {
    "deviceCount": {
      "total": 100,
      "active": 95,
      "inactive": 3,
      "maintenance": 2
    },
    "messageCount": {
      "today": 1000,
      "thisWeek": 7000,
      "thisMonth": 30000,
      "total": 100000
    },
    "alertCount": {
      "active": 5,
      "critical": 1,
      "high": 2,
      "medium": 1,
      "low": 1
    },
    "systemStatus": {
      "cpuUsage": 65.5,
      "memoryUsage": 78.2,
      "diskUsage": 45.8,
      "networkStatus": "normal"
    }
  }
}
```

### 10.2 图表数据
**GET** `/charts/{chartType}`

**查询参数:**
- `period` (string): 时间周期
- `deviceId` (string): 设备ID
- `startTime` (string): 开始时间
- `endTime` (string): 结束时间

## 11. WebSocket接口

### 11.1 实时数据推送
**WebSocket** `/ws/realtime`

**连接参数:**
- `token` (string): JWT Token

**消息格式:**
```json
{
  "type": "DEVICE_STATUS",
  "data": {
    "deviceId": "DEV001",
    "status": "ACTIVE",
    "timestamp": 1699123456789
  }
}
```

### 11.2 告警推送
**WebSocket** `/ws/alerts`

### 11.3 系统通知
**WebSocket** `/ws/notifications`

## 12. 错误码说明

| 错误码 | 说明 | 处理建议 |
|--------|------|----------|
| 1001 | 用户名或密码错误 | 检查用户名和密码 |
| 1002 | Token已过期 | 重新登录获取新Token |
| 1003 | 权限不足 | 联系管理员分配权限 |
| 2001 | 设备不存在 | 检查设备ID是否正确 |
| 2002 | 设备已存在 | 使用其他设备ID |
| 3001 | 报文格式错误 | 检查报文格式 |
| 4001 | 告警不存在 | 检查告警ID |
| 5001 | 系统内部错误 | 联系技术支持 |
| 5002 | 数据库连接失败 | 检查数据库配置 |

## 13. 接口调用示例

### 13.1 JavaScript示例
```javascript
// 登录
const login = async (username, password) => {
  const response = await fetch('/api/auth/login', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({ username, password })
  });
  const data = await response.json();
  return data;
};

// 获取设备列表
const getDevices = async (params) => {
  const token = localStorage.getItem('token');
  const queryString = new URLSearchParams(params).toString();
  const response = await fetch(`/api/devices?${queryString}`, {
    headers: {
      'Authorization': `Bearer ${token}`
    }
  });
  const data = await response.json();
  return data;
};
```

### 13.2 Java示例
```java
// 使用RestTemplate调用接口
RestTemplate restTemplate = new RestTemplate();

// 设置请求头
HttpHeaders headers = new HttpHeaders();
headers.setBearerAuth(token);
headers.setContentType(MediaType.APPLICATION_JSON);

// 创建请求
HttpEntity<String> entity = new HttpEntity<>(headers);

// 发送请求
ResponseEntity<ApiResponse> response = restTemplate.exchange(
    "/api/devices",
    HttpMethod.GET,
    entity,
    ApiResponse.class
);
```

## 14. 版本更新记录

### v1.0.0 (2023-11-01)
- 初始版本发布
- 实现基础认证和设备管理接口
- 实现报文管理和基础分析功能

### v1.1.0 (计划中)
- 增加高级分析接口
- 优化WebSocket实时推送
- 增加批量操作接口