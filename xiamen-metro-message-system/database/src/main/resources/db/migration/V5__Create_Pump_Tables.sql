-- 创建水泵数据表
CREATE TABLE pump_data (
    id BIGSERIAL PRIMARY KEY,
    device_id VARCHAR(100) NOT NULL,
    timestamp TIMESTAMP NOT NULL,
    pump_status INTEGER,
    runtime_minutes DOUBLE PRECISION,
    current_amperage DOUBLE PRECISION,
    voltage DOUBLE PRECISION,
    power_kw DOUBLE PRECISION,
    energy_consumption_kwh DOUBLE PRECISION,
    water_pressure_kpa DOUBLE PRECISION,
    flow_rate_m3h DOUBLE PRECISION,
    water_temperature_celsius DOUBLE PRECISION,
    vibration_mm_s DOUBLE PRECISION,
    noise_level_db DOUBLE PRECISION,
    fault_code VARCHAR(50),
    alarm_level INTEGER,
    maintenance_flag BOOLEAN,
    raw_message TEXT,
    data_source VARCHAR(50),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 创建索引
CREATE INDEX idx_pump_device_timestamp ON pump_data(device_id, timestamp);
CREATE INDEX idx_pump_timestamp ON pump_data(timestamp);
CREATE INDEX idx_pump_device ON pump_data(device_id);
CREATE INDEX idx_pump_fault_code ON pump_data(fault_code) WHERE fault_code IS NOT NULL;
CREATE INDEX idx_pump_alarm_level ON pump_data(alarm_level) WHERE alarm_level > 1;
CREATE INDEX idx_pump_maintenance_flag ON pump_data(maintenance_flag) WHERE maintenance_flag = true;

-- 创建水泵分析结果表
CREATE TABLE pump_analysis_result (
    id BIGSERIAL PRIMARY KEY,
    device_id VARCHAR(100) NOT NULL,
    analysis_timestamp TIMESTAMP NOT NULL,
    analysis_type VARCHAR(50) NOT NULL,
    severity_level INTEGER,
    confidence_score DOUBLE PRECISION,
    anomaly_description TEXT,
    detected_value DOUBLE PRECISION,
    expected_value DOUBLE PRECISION,
    deviation_percentage DOUBLE PRECISION,
    trend_direction VARCHAR(20),
    predicted_failure_time TIMESTAMP,
    maintenance_recommendation TEXT,
    priority_level INTEGER,
    analysis_parameters TEXT,
    model_version VARCHAR(20),
    is_confirmed BOOLEAN DEFAULT FALSE,
    confirmed_by VARCHAR(100),
    confirmed_at TIMESTAMP,
    confirmation_notes TEXT,
    data_period_start TIMESTAMP,
    data_period_end TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 创建索引
CREATE INDEX idx_pump_analysis_device_timestamp ON pump_analysis_result(device_id, analysis_timestamp);
CREATE INDEX idx_pump_analysis_type ON pump_analysis_result(analysis_type);
CREATE INDEX idx_pump_analysis_severity ON pump_analysis_result(severity_level);
CREATE INDEX idx_pump_analysis_priority ON pump_analysis_result(priority_level);
CREATE INDEX idx_pump_analysis_confirmed ON pump_analysis_result(is_confirmed, severity_level) WHERE is_confirmed = false;
CREATE INDEX idx_pump_analysis_predicted_failure ON pump_analysis_result(predicted_failure_time) WHERE predicted_failure_time IS NOT NULL;

-- 创建触发器函数用于更新 updated_at 字段
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- 为水泵数据表创建触发器
CREATE TRIGGER update_pump_data_updated_at
    BEFORE UPDATE ON pump_data
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- 为水泵分析结果表创建触发器
CREATE TRIGGER update_pump_analysis_result_updated_at
    BEFORE UPDATE ON pump_analysis_result
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- 插入示例数据（可选）
-- INSERT INTO pump_data (
--     device_id, timestamp, pump_status, runtime_minutes, power_kw,
--     energy_consumption_kwh, water_pressure_kpa, vibration_mm_s,
--     created_at
-- ) VALUES
-- ('PUMP_001', CURRENT_TIMESTAMP - INTERVAL '1 hour', 1, 45.5, 15.2, 12.1, 250.5, 2.1, CURRENT_TIMESTAMP - INTERVAL '1 hour'),
-- ('PUMP_001', CURRENT_TIMESTAMP - INTERVAL '30 minutes', 1, 30.2, 14.8, 7.4, 248.3, 2.3, CURRENT_TIMESTAMP - INTERVAL '30 minutes'),
-- ('PUMP_002', CURRENT_TIMESTAMP - INTERVAL '2 hours', 0, 0.0, 0.0, 0.0, 0.0, 0.0, CURRENT_TIMESTAMP - INTERVAL '2 hours');

-- 添加表注释
COMMENT ON TABLE pump_data IS '水泵运行数据表';
COMMENT ON TABLE pump_analysis_result IS '水泵分析结果表';

-- 添加列注释
COMMENT ON COLUMN pump_data.device_id IS '设备ID';
COMMENT ON COLUMN pump_data.timestamp IS '数据时间戳';
COMMENT ON COLUMN pump_data.pump_status IS '水泵状态 (1-启动, 0-停止)';
COMMENT ON COLUMN pump_data.runtime_minutes IS '运行时间（分钟）';
COMMENT ON COLUMN pump_data.current_amperage IS '电流值（A）';
COMMENT ON COLUMN pump_data.voltage IS '电压值（V）';
COMMENT ON COLUMN pump_data.power_kw IS '功率（kW）';
COMMENT ON COLUMN pump_data.energy_consumption_kwh IS '能耗（kWh）';
COMMENT ON COLUMN pump_data.water_pressure_kpa IS '水压（kPa）';
COMMENT ON COLUMN pump_data.flow_rate_m3h IS '流量（m³/h）';
COMMENT ON COLUMN pump_data.water_temperature_celsius IS '水温（°C）';
COMMENT ON COLUMN pump_data.vibration_mm_s IS '振动值（mm/s）';
COMMENT ON COLUMN pump_data.noise_level_db IS '噪音水平（dB）';
COMMENT ON COLUMN pump_data.fault_code IS '故障代码';
COMMENT ON COLUMN pump_data.alarm_level IS '报警级别 (0-正常, 1-预警, 2-报警, 3-严重)';
COMMENT ON COLUMN pump_data.maintenance_flag IS '维护标志';
COMMENT ON COLUMN pump_data.raw_message IS '原始报文内容';

COMMENT ON COLUMN pump_analysis_result.device_id IS '设备ID';
COMMENT ON COLUMN pump_analysis_result.analysis_timestamp IS '分析时间戳';
COMMENT ON COLUMN pump_analysis_result.analysis_type IS '分析类型';
COMMENT ON COLUMN pump_analysis_result.severity_level IS '严重级别 (1-信息, 2-警告, 3-错误, 4-严重)';
COMMENT ON COLUMN pump_analysis_result.confidence_score IS '置信度 (0.0-1.0)';
COMMENT ON COLUMN pump_analysis_result.anomaly_description IS '异常描述';
COMMENT ON COLUMN pump_analysis_result.detected_value IS '检测到的异常值';
COMMENT ON COLUMN pump_analysis_result.expected_value IS '预期值或阈值';
COMMENT ON COLUMN pump_analysis_result.deviation_percentage IS '偏差百分比';
COMMENT ON COLUMN pump_analysis_result.trend_direction IS '趋势方向 (INCREASING, DECREASING, STABLE, FLUCTUATING)';
COMMENT ON COLUMN pump_analysis_result.predicted_failure_time IS '预测故障时间';
COMMENT ON COLUMN pump_analysis_result.maintenance_recommendation IS '建议维护措施';
COMMENT ON COLUMN pump_analysis_result.priority_level IS '优先级 (1-低, 2-中, 3-高, 4-紧急)';
COMMENT ON COLUMN pump_analysis_result.is_confirmed IS '是否已确认';
COMMENT ON COLUMN pump_analysis_result.confirmed_by IS '确认人';
COMMENT ON COLUMN pump_analysis_result.confirmed_at IS '确认时间';