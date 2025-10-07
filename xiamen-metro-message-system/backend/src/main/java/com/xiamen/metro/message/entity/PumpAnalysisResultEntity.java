package com.xiamen.metro.message.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 水泵分析结果实体
 *
 * @author Xiamen Metro System
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "pump_analysis_result", indexes = {
    @Index(name = "idx_pump_analysis_device_timestamp", columnList = "deviceId,analysisTimestamp"),
    @Index(name = "idx_pump_analysis_type", columnList = "analysisType"),
    @Index(name = "idx_pump_analysis_severity", columnList = "severityLevel")
})
@EntityListeners(AuditingEntityListener.class)
public class PumpAnalysisResultEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 设备ID
     */
    @Column(name = "device_id", nullable = false, length = 100)
    private String deviceId;

    /**
     * 分析时间戳
     */
    @Column(name = "analysis_timestamp", nullable = false)
    private LocalDateTime analysisTimestamp;

    /**
     * 分析类型
     */
    @Column(name = "analysis_type", nullable = false, length = 50)
    private String analysisType;

    /**
     * 严重级别 (1-信息, 2-警告, 3-错误, 4-严重)
     */
    @Column(name = "severity_level")
    private Integer severityLevel;

    /**
     * 置信度 (0.0-1.0)
     */
    @Column(name = "confidence_score")
    private Double confidenceScore;

    /**
     * 异常描述
     */
    @Column(name = "anomaly_description", columnDefinition = "TEXT")
    private String anomalyDescription;

    /**
     * 检测到的异常值
     */
    @Column(name = "detected_value")
    private Double detectedValue;

    /**
     * 预期值或阈值
     */
    @Column(name = "expected_value")
    private Double expectedValue;

    /**
     * 偏差百分比
     */
    @Column(name = "deviation_percentage")
    private Double deviationPercentage;

    /**
     * 趋势方向 (INCREASING, DECREASING, STABLE, FLUCTUATING)
     */
    @Column(name = "trend_direction", length = 20)
    private String trendDirection;

    /**
     * 预测故障时间
     */
    @Column(name = "predicted_failure_time")
    private LocalDateTime predictedFailureTime;

    /**
     * 建议维护措施
     */
    @Column(name = "maintenance_recommendation", columnDefinition = "TEXT")
    private String maintenanceRecommendation;

    /**
     * 优先级 (1-低, 2-中, 3-高, 4-紧急)
     */
    @Column(name = "priority_level")
    private Integer priorityLevel;

    /**
     * 分析参数（JSON格式）
     */
    @Column(name = "analysis_parameters", columnDefinition = "TEXT")
    private String analysisParameters;

    /**
     * 模型版本
     */
    @Column(name = "model_version", length = 20)
    private String modelVersion;

    /**
     * 是否已确认
     */
    @Column(name = "is_confirmed")
    private Boolean isConfirmed;

    /**
     * 确认人
     */
    @Column(name = "confirmed_by", length = 100)
    private String confirmedBy;

    /**
     * 确认时间
     */
    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    /**
     * 确认备注
     */
    @Column(name = "confirmation_notes", columnDefinition = "TEXT")
    private String confirmationNotes;

    /**
     * 关联的数据时间段开始
     */
    @Column(name = "data_period_start")
    private LocalDateTime dataPeriodStart;

    /**
     * 关联的数据时间段结束
     */
    @Column(name = "data_period_end")
    private LocalDateTime dataPeriodEnd;

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