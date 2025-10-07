package com.xiamen.metro.message.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 水泵数据实体
 *
 * @author Xiamen Metro System
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "pump_data", indexes = {
    @Index(name = "idx_pump_device_timestamp", columnList = "deviceId,timestamp"),
    @Index(name = "idx_pump_timestamp", columnList = "timestamp"),
    @Index(name = "idx_pump_device", columnList = "deviceId")
})
@EntityListeners(AuditingEntityListener.class)
public class PumpDataEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 设备ID
     */
    @Column(name = "device_id", nullable = false, length = 100)
    private String deviceId;

    /**
     * 时间戳
     */
    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    /**
     * 水泵状态 (1-启动, 0-停止)
     */
    @Column(name = "pump_status")
    private Integer pumpStatus;

    /**
     * 运行时间（分钟）
     */
    @Column(name = "runtime_minutes")
    private Double runtimeMinutes;

    /**
     * 电流值（A）
     */
    @Column(name = "current_amperage")
    private Double currentAmperage;

    /**
     * 电压值（V）
     */
    @Column(name = "voltage")
    private Double voltage;

    /**
     * 功率（kW）
     */
    @Column(name = "power_kw")
    private Double powerKw;

    /**
     * 能耗（kWh）
     */
    @Column(name = "energy_consumption_kwh")
    private Double energyConsumptionKwh;

    /**
     * 水压（kPa）
     */
    @Column(name = "water_pressure_kpa")
    private Double waterPressureKpa;

    /**
     * 流量（m³/h）
     */
    @Column(name = "flow_rate_m3h")
    private Double flowRateM3h;

    /**
     * 水温（°C）
     */
    @Column(name = "water_temperature_celsius")
    private Double waterTemperatureCelsius;

    /**
     * 振动值（mm/s）
     */
    @Column(name = "vibration_mm_s")
    private Double vibrationMmS;

    /**
     * 噪音水平（dB）
     */
    @Column(name = "noise_level_db")
    private Double noiseLevelDb;

    /**
     * 故障代码
     */
    @Column(name = "fault_code", length = 50)
    private String faultCode;

    /**
     * 报警级别 (0-正常, 1-预警, 2-报警, 3-严重)
     */
    @Column(name = "alarm_level")
    private Integer alarmLevel;

    /**
     * 维护标志
     */
    @Column(name = "maintenance_flag")
    private Boolean maintenanceFlag;

    /**
     * 原始报文内容
     */
    @Column(name = "raw_message", columnDefinition = "TEXT")
    private String rawMessage;

    /**
     * 数据来源
     */
    @Column(name = "data_source", length = 50)
    private String dataSource;

    /**
     * 创建时间
     */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}