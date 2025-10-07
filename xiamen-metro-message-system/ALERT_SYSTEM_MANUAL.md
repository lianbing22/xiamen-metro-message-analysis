# 厦门地铁设备报文分析系统 - 智能告警系统

## 系统概述

智能告警系统是厦门地铁设备报文分析系统的重要组成部分，基于已完成的水泵问题模型引擎，提供全方位的设备监控和告警功能。系统采用规则引擎架构，支持多种告警类型和通知方式，确保设备异常能够及时发现和处理。

## 核心功能

### 1. 告警规则引擎
- **可配置规则**: 支持动态配置告警规则，无需重启系统
- **多种规则类型**:
  - 阈值监控：基于设定阈值进行监控
  - 异常检测：基于机器学习模型检测异常
  - 性能下降：监控设备性能指标变化
  - 故障预测：基于预测模型的故障预警
  - 健康评分：综合健康状态监控
  - 自定义规则：支持复杂业务逻辑

### 2. 告警分级机制
- **严重级别**: 设备严重故障，需要立即处理
- **警告级别**: 设备性能下降，需要关注
- **提醒级别**: 一般性提醒，需要记录

### 3. 多渠道通知服务
- **邮件通知**:
  - 支持HTML格式邮件模板
  - 可配置多个接收人
  - 支持邮件发送失败重试
- **短信通知**:
  - 可选功能，支持第三方短信服务
  - 按告警级别配置发送策略
- **WebSocket实时推送**:
  - 实时推送到前端界面
  - 支持心跳保活
  - 连接状态监控

### 4. 告警去重和抑制
- **智能去重**: 避免重复告警轰炸
- **时间窗口抑制**: 在指定时间内抑制相似告警
- **连续触发确认**: 需要连续多次触发才生成告警
- **告警聚合**: 将相关告警进行聚合展示

### 5. 告警处理状态跟踪
- **状态管理**: 活跃 → 已确认 → 已处理
- **处理记录**: 记录确认人、处理人、处理时间
- **备注功能**: 支持添加确认和处理备注
- **误报标记**: 支持标记误报以优化规则

## 技术架构

### 后端技术栈
- **Spring Boot 3.2.0**: 主框架
- **Spring WebSocket**: 实时通信
- **Spring Mail**: 邮件服务
- **Thymeleaf**: 邮件模板引擎
- **Drools**: 规则引擎（可选）
- **PostgreSQL**: 数据存储
- **Redis**: 缓存和会话管理

### 核心组件
```
AlertController (告警API)
    ↓
AlertManagementService (告警管理)
    ↓
AlertRuleEngine (规则引擎) → AlertEvaluationService (评估服务)
    ↓
AlertNotificationService (通知服务)
    ↓
WebSocketNotificationService (WebSocket推送)
```

### 数据库设计
- **alert_rules**: 告警规则配置表
- **alert_records**: 告警记录表
- **alert_notifications**: 通知记录表
- **关联表**: 规则与通知方式、接收人关联

## API接口

### 告警管理接口
- `GET /api/v1/alerts/active` - 获取活跃告警
- `POST /api/v1/alerts/{alertId}/acknowledge` - 确认告警
- `POST /api/v1/alerts/{alertId}/resolve` - 处理告警
- `POST /api/v1/alerts/{alertId}/false-positive` - 标记误报
- `GET /api/v1/alerts/statistics` - 获取告警统计

### 通知管理接口
- `GET /api/v1/alerts/notifications/statistics` - 通知统计
- `POST /api/v1/alerts/notifications/retry` - 重试失败通知
- `POST /api/v1/alerts/test` - 触发测试告警

### WebSocket接口
- `ws://localhost:8080/ws/alerts` - 告警实时推送

## 配置说明

### 告警规则配置
```yaml
alert:
  rule-engine:
    check-interval: 60          # 规则检查间隔（秒）
    execution-timeout: 30000    # 规则执行超时（毫秒）
    enable-cache: true          # 启用规则缓存
    cache-size: 1000           # 缓存大小
```

### 通知服务配置
```yaml
alert:
  notification:
    email:
      enabled: true            # 启用邮件通知
      timeout: 10000          # 发送超时（毫秒）
      retry-count: 3          # 重试次数
      thread-pool-size: 5     # 线程池大小
    websocket:
      enabled: true           # 启用WebSocket
      heartbeat-interval: 30  # 心跳间隔（秒）
      max-connections: 1000   # 最大连接数
```

### 告警级别配置
```yaml
alert:
  levels:
    critical:
      notification-methods: EMAIL,WEBSOCKET,SMS
      immediate: true         # 立即发送
      escalation-minutes: 15  # 升级时间（分钟）
    warning:
      notification-methods: EMAIL,WEBSOCKET
      immediate: false
      escalation-minutes: 60
```

## 邮件模板

系统使用Thymeleaf模板引擎生成HTML邮件，支持动态内容填充。

### 模板位置
- `src/main/resources/templates/alert-email-template.html`

### 模板变量
- `alert`: 告警信息对象
- `currentTime`: 当前时间

### 自定义模板
可以通过修改模板文件来自定义邮件样式和内容。

## 系统集成

### 与水泵分析系统集成
告警系统与水泵智能分析服务无缝集成，当分析完成时自动触发告警检查：

```java
// 在PumpIntelligentAnalysisService中
List<AlertRecordDTO> alerts = alertManagementService.processPumpAnalysisResults(
    request.getDeviceId(), response);
```

### 任务调度
系统内置多种定时任务：
- 告警规则检查（每分钟）
- 通知重试（每5分钟）
- 数据清理（每日凌晨）
- 统计报告（每日上午）
- 健康检查（每30分钟）

## 监控和运维

### 日志监控
- 告警生成日志
- 通知发送日志
- 规则评估日志
- 系统异常日志

### 性能监控
- 规则执行时间
- 通知发送延迟
- WebSocket连接数
- 告警处理吞吐量

### 健康检查
- 数据库连接状态
- 邮件服务可用性
- WebSocket服务状态
- 规则引擎健康状态

## 安全考虑

### 权限控制
- 基于Spring Security的访问控制
- 不同角色的操作权限限制
- API接口权限验证

### 数据安全
- 敏感信息加密存储
- 通知内容安全过滤
- 审计日志记录

## 部署说明

### 环境要求
- Java 17+
- PostgreSQL 12+
- Redis 6+
- Spring Boot 3.2.0

### 配置文件
- `application.yml` - 主配置
- `application-alert.yml` - 告警专用配置
- `application-{profile}.yml` - 环境配置

### 数据库初始化
使用Flyway进行数据库版本管理：
```sql
-- V8__create_alert_tables.sql
```

## 验证测试

### 运行验证脚本
```bash
./validate-alert-system.sh
```

### 手动测试
1. 启动应用：`mvn spring-boot:run -Dspring.profiles.active=validation`
2. 访问API文档：`http://localhost:8080/swagger-ui.html`
3. 测试告警生成和通知

### 测试用例
- 告警规则评估测试
- 水泵分析集成测试
- 告警管理功能测试
- 通知服务测试
- WebSocket推送测试

## 故障排查

### 常见问题
1. **告警未生成**: 检查规则配置和激活状态
2. **邮件发送失败**: 检查邮件服务配置和网络连接
3. **WebSocket连接断开**: 检查防火墙和代理设置
4. **性能问题**: 检查数据库查询和规则执行效率

### 调试方法
- 启用DEBUG日志级别
- 查看告警统计信息
- 检查通知发送记录
- 监控系统资源使用

## 扩展开发

### 添加新的告警规则类型
1. 在`AlertRuleEngine`中添加新的评估逻辑
2. 在`AlertEvaluationService`中实现具体评估方法
3. 更新规则配置模板

### 集成新的通知方式
1. 在`AlertNotificationService`中添加新的通知方法
2. 更新`NotificationType`枚举
3. 配置相应的服务参数

### 自定义告警处理逻辑
1. 实现`AlertProcessor`接口
2. 注册为Spring Bean
3. 在告警管理服务中调用

## 总结

厦门地铁设备报文分析系统的智能告警模块提供了完整的设备监控和告警解决方案。通过规则引擎、多渠道通知、智能去重等核心功能，确保设备异常能够及时发现、准确通知、有效处理。系统具有良好的扩展性和可维护性，为地铁设备的安全运行提供有力保障。

系统已完成以下核心功能：
- ✅ 告警规则引擎（可配置规则）
- ✅ 告警分级机制（严重/警告/提醒）
- ✅ 邮件通知服务（支持HTML模板）
- ✅ WebSocket实时推送
- ✅ 告警去重和抑制机制
- ✅ 告警处理状态跟踪
- ✅ 与水泵分析系统集成
- ✅ 统计分析和报告
- ✅ 完整的测试验证

**系统验证结果：所有功能验证通过，可以投入使用！** 🎉