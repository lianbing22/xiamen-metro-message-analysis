package com.xiamen.metro.message.dto.pump;

import lombok.Data;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

/**
 * 水泵数据DTO
 *
 * @author Xiamen Metro System
 */
@Data
public class PumpDataDTO {

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 设备ID
     */
    private String deviceId;

    /**
     * 时间戳
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;

    /**
     * 水泵状态 (1-启动, 0-停止)
     */
    private Integer pumpStatus;

    /**
     * 运行时间（分钟）
     */
    private Double runtimeMinutes;

    /**
     * 电流值（A）
     */
    private Double currentAmperage;

    /**
     * 电压值（V）
     */
    private Double voltage;

    /**
     * 功率（kW）
     */
    private Double powerKw;

    /**
     * 能耗（kWh）
     */
    private Double energyConsumptionKwh;

    /**
     * 水压（kPa）
     */
    private Double waterPressureKpa;

    /**
     * 流量（m³/h）
     */
    private Double flowRateM3h;

    /**
     * 水温（°C）
     */
    private Double waterTemperatureCelsius;

    /**
     * 振动值（mm/s）
     */
    private Double vibrationMmS;

    /**
     * 噪音水平（dB）
     */
    private Double noiseLevelDb;

    /**
     * 故障代码
     */
    private String faultCode;

    /**
     * 报警级别 (0-正常, 1-预警, 2-报警, 3-严重)
     */
    private Integer alarmLevel;

    /**
     * 维护标志
     */
    private Boolean maintenanceFlag;

    /**
     * 原始报文内容
     */
    private String rawMessage;

    /**
     * 数据来源
     */
    private String dataSource;
}