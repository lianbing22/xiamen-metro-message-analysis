-- 插入默认管理员用户
INSERT INTO users (user_id, username, password, email, full_name, role, status, created_by) VALUES
('admin', 'admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iKVjzieMwkOmANgNOgKQNNBDvAGK', 'admin@xiamenmetro.com', '系统管理员', 'ADMIN', 'ACTIVE', 'system'),
('operator', 'operator', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iKVjzieMwkOmANgNOgKQNNBDvAGK', 'operator@xiamenmetro.com', '系统操作员', 'OPERATOR', 'ACTIVE', 'system'),
('analyst', 'analyst', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iKVjzieMwkOmANgNOgKQNNBDvAGK', 'analyst@xiamenmetro.com', '数据分析员', 'ANALYST', 'ACTIVE', 'system');

-- 插入默认设备组
INSERT INTO device_groups (group_id, group_name, group_level, description, status, created_by) VALUES
('root', '根目录', 1, '设备根目录', 'ACTIVE', 'system'),
('power_system', '供电系统', 2, '供电系统设备', 'ACTIVE', 'system'),
('signal_system', '信号系统', 2, '信号系统设备', 'ACTIVE', 'system'),
('communication_system', '通信系统', 2, '通信系统设备', 'ACTIVE', 'system'),
('tunnel_system', '隧道系统', 2, '隧道系统设备', 'ACTIVE', 'system');

-- 插入系统配置
INSERT INTO system_configs (config_key, config_value, config_type, description, created_by) VALUES
('system.name', '厦门地铁设备报文分析系统', 'STRING', '系统名称', 'system'),
('system.version', '1.0.0', 'STRING', '系统版本', 'system'),
('message.max_size', '10485760', 'INTEGER', '报文最大大小（字节）', 'system'),
('alert.retention_days', '90', 'INTEGER', '告警保留天数', 'system'),
('message.retention_days', '30', 'INTEGER', '报文保留天数', 'system'),
('analysis.batch_size', '1000', 'INTEGER', '分析批处理大小', 'system'),
('websocket.max_connections', '1000', 'INTEGER', 'WebSocket最大连接数', 'system'),
('cache.ttl', '3600', 'INTEGER', '缓存过期时间（秒）', 'system'),
('backup.enabled', 'true', 'BOOLEAN', '是否启用自动备份', 'system'),
('backup.schedule', '0 2 * * *', 'STRING', '备份计划（Cron表达式）', 'system');

-- 插入示例设备
INSERT INTO devices (device_id, device_name, device_type, device_group, location, ip_address, port, protocol, status, description, created_by) VALUES
('DEV001', '主变电所1号变压器', 'TRANSFORMER', 'power_system', '主变电所', '192.168.1.1', 502, 'MODBUS', 'ACTIVE', '主变电所1号变压器监控设备', 'system'),
('DEV002', '主变电所2号变压器', 'TRANSFORMER', 'power_system', '主变电所', '192.168.1.2', 502, 'MODBUS', 'ACTIVE', '主变电所2号变压器监控设备', 'system'),
('DEV003', '信号机A-01', 'SIGNAL_MACHINE', 'signal_system', '1号线站台', '192.168.2.1', 502, 'MODBUS', 'ACTIVE', '1号线站台信号机A-01', 'system'),
('DEV004', '信号机A-02', 'SIGNAL_MACHINE', 'signal_system', '1号线站台', '192.168.2.2', 502, 'MODBUS', 'ACTIVE', '1号线站台信号机A-02', 'system'),
('DEV005', '通信控制器A', 'COMMUNICATION_CONTROLLER', 'communication_system', '控制中心', '192.168.3.1', 502, 'MODBUS', 'ACTIVE', '控制中心通信控制器A', 'system'),
('DEV006', '隧道监测器T-01', 'TUNNEL_MONITOR', 'tunnel_system', '1号线隧道', '192.168.4.1', 502, 'MODBUS', 'ACTIVE', '1号线隧道监测器T-01', 'system');