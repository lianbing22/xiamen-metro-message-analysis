-- 创建告警规则表
CREATE TABLE alert_rules (
    id BIGSERIAL PRIMARY KEY,
    rule_name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    device_id VARCHAR(50),
    rule_type VARCHAR(50) NOT NULL,
    alert_level VARCHAR(20) NOT NULL,
    rule_conditions TEXT,
    threshold_config TEXT,
    check_interval_minutes INTEGER NOT NULL,
    consecutive_trigger_count INTEGER NOT NULL,
    suppression_minutes INTEGER NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    priority INTEGER NOT NULL DEFAULT 1,
    created_by VARCHAR(50),
    last_triggered_time TIMESTAMP,
    created_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 创建告警规则通知方式关联表
CREATE TABLE alert_rule_notification_methods (
    rule_id BIGINT NOT NULL REFERENCES alert_rules(id) ON DELETE CASCADE,
    notification_method VARCHAR(20) NOT NULL
);

-- 创建告警规则邮件接收人表
CREATE TABLE alert_rule_email_recipients (
    rule_id BIGINT NOT NULL REFERENCES alert_rules(id) ON DELETE CASCADE,
    email VARCHAR(200) NOT NULL
);

-- 创建告警规则短信接收人表
CREATE TABLE alert_rule_sms_recipients (
    rule_id BIGINT NOT NULL REFERENCES alert_rules(id) ON DELETE CASCADE,
    phone VARCHAR(20) NOT NULL
);

-- 创建告警记录表
CREATE TABLE alert_records (
    id BIGSERIAL PRIMARY KEY,
    alert_id VARCHAR(50) NOT NULL UNIQUE,
    rule_id BIGINT NOT NULL REFERENCES alert_rules(id),
    device_id VARCHAR(50) NOT NULL,
    alert_level VARCHAR(20) NOT NULL,
    alert_title VARCHAR(200) NOT NULL,
    alert_content TEXT,
    triggered_value DECIMAL(15, 4),
    threshold_value DECIMAL(15, 4),
    confidence_score DECIMAL(5, 4),
    alert_time TIMESTAMP NOT NULL,
    status VARCHAR(20) NOT NULL,
    is_confirmed BOOLEAN NOT NULL DEFAULT FALSE,
    confirmed_time TIMESTAMP,
    confirmed_by VARCHAR(50),
    confirmation_note VARCHAR(500),
    resolved_time TIMESTAMP,
    resolved_by VARCHAR(50),
    resolution_note VARCHAR(500),
    analysis_result_id VARCHAR(50),
    extended_info TEXT,
    alert_source VARCHAR(50),
    email_notified BOOLEAN NOT NULL DEFAULT FALSE,
    email_notification_time TIMESTAMP,
    sms_notified BOOLEAN NOT NULL DEFAULT FALSE,
    sms_notification_time TIMESTAMP,
    websocket_notified BOOLEAN NOT NULL DEFAULT FALSE,
    websocket_notification_time TIMESTAMP,
    created_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 创建告警通知记录表
CREATE TABLE alert_notifications (
    id BIGSERIAL PRIMARY KEY,
    alert_id VARCHAR(50) NOT NULL,
    notification_type VARCHAR(20) NOT NULL,
    recipient VARCHAR(200) NOT NULL,
    subject VARCHAR(200) NOT NULL,
    content TEXT,
    notification_time TIMESTAMP NOT NULL,
    status VARCHAR(20) NOT NULL,
    send_result VARCHAR(500),
    retry_count INTEGER NOT NULL DEFAULT 0,
    next_retry_time TIMESTAMP,
    last_retry_time TIMESTAMP,
    template_id VARCHAR(50),
    template_parameters TEXT,
    created_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 创建索引
-- 告警规则表索引
CREATE INDEX idx_alert_rule_name ON alert_rules(rule_name);
CREATE INDEX idx_alert_rule_device ON alert_rules(device_id);
CREATE INDEX idx_alert_rule_active ON alert_rules(is_active);
CREATE INDEX idx_alert_rule_type ON alert_rules(rule_type);

-- 告警记录表索引
CREATE INDEX idx_alert_device ON alert_records(device_id);
CREATE INDEX idx_alert_time ON alert_records(alert_time);
CREATE INDEX idx_alert_level ON alert_records(alert_level);
CREATE INDEX idx_alert_status ON alert_records(status);
CREATE INDEX idx_alert_rule ON alert_records(rule_id);
CREATE INDEX idx_alert_confirmed ON alert_records(is_confirmed);

-- 告警通知记录表索引
CREATE INDEX idx_notification_alert ON alert_notifications(alert_id);
CREATE INDEX idx_notification_type ON alert_notifications(notification_type);
CREATE INDEX idx_notification_status ON alert_notifications(status);
CREATE INDEX idx_notification_time ON alert_notifications(notification_time);

-- 创建触发器函数：更新updated_time字段
CREATE OR REPLACE FUNCTION update_updated_time_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_time = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- 为各表创建更新时间触发器
CREATE TRIGGER update_alert_rules_updated_time
    BEFORE UPDATE ON alert_rules
    FOR EACH ROW EXECUTE FUNCTION update_updated_time_column();

CREATE TRIGGER update_alert_records_updated_time
    BEFORE UPDATE ON alert_records
    FOR EACH ROW EXECUTE FUNCTION update_updated_time_column();

CREATE TRIGGER update_alert_notifications_updated_time
    BEFORE UPDATE ON alert_notifications
    FOR EACH ROW EXECUTE FUNCTION update_updated_time_column();

-- 插入默认告警规则
INSERT INTO alert_rules (rule_name, description, rule_type, alert_level, rule_conditions, threshold_config, check_interval_minutes, consecutive_trigger_count, suppression_minutes, priority, created_by) VALUES
('设备健康评分监控', '监控设备健康评分，低于阈值时触发告警', 'HEALTH_SCORE', 'WARNING',
 '{"metricName": "health_score", "comparison": "lt"}',
 '{"healthScoreThreshold": 60.0}',
 60, 1, 30, 2, 'SYSTEM'),

('设备故障概率预警', '基于故障概率模型进行预警', 'FAULT_PREDICTION', 'WARNING',
 '{"failureProbabilityThreshold": 0.7}',
 '{"failureProbabilityThreshold": 0.7}',
 120, 2, 60, 3, 'SYSTEM'),

('性能下降检测', '检测设备性能指标下降', 'PERFORMANCE_DEGRADATION', 'INFO',
 '{"metricName": "efficiency_score", "comparison": "lt"}',
 '{"degradationThreshold": 20.0}',
 180, 1, 45, 1, 'SYSTEM'),

('异常频率监控', '监控设备异常运行频率', 'ANOMALY_DETECTION', 'CRITICAL',
 '{"anomalyType": "FREQUENCY"}',
 '{"anomalyRateThreshold": 30.0}',
 30, 3, 15, 4, 'SYSTEM');

-- 为默认规则添加通知方式配置
INSERT INTO alert_rule_notification_methods (rule_id, notification_method)
SELECT id, 'EMAIL' FROM alert_rules WHERE created_by = 'SYSTEM';

INSERT INTO alert_rule_notification_methods (rule_id, notification_method)
SELECT id, 'WEBSOCKET' FROM alert_rules WHERE created_by = 'SYSTEM' AND alert_level = 'CRITICAL';

-- 添加默认邮件接收人
INSERT INTO alert_rule_email_recipients (rule_id, email)
SELECT id, 'admin@xiamen-metro.com' FROM alert_rules WHERE created_by = 'SYSTEM';

INSERT INTO alert_rule_email_recipients (rule_id, email)
SELECT id, 'maintenance@xiamen-metro.com' FROM alert_rules WHERE alert_level IN ('WARNING', 'CRITICAL') AND created_by = 'SYSTEM';

-- 添加注释
COMMENT ON TABLE alert_rules IS '告警规则配置表';
COMMENT ON TABLE alert_records IS '告警记录表';
COMMENT ON TABLE alert_notifications IS '告警通知记录表';
COMMENT ON TABLE alert_rule_notification_methods IS '告警规则通知方式关联表';
COMMENT ON TABLE alert_rule_email_recipients IS '告警规则邮件接收人表';
COMMENT ON TABLE alert_rule_sms_recipients IS '告警规则短信接收人表';