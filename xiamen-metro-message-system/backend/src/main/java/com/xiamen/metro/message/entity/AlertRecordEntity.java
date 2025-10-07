package com.xiamen.metro.message.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * 告警记录实体
 *
 * @author Xiamen Metro System
 */
@Entity
@Table(name = "alert_records", indexes = {
    @Index(name = "idx_alert_device", columnList = "deviceId"),
    @Index(name = "idx_alert_time", columnList = "alertTime"),
    @Index(name = "idx_alert_level", columnList = "alertLevel"),
    @Index(name = "idx_alert_status", columnList = "status"),
    @Index(name = "idx_alert_rule", columnList = "ruleId")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class AlertRecordEntity extends BaseEntity {

    /**
     * 告警ID
     */
    @Column(length = 50, nullable = false, unique = true)
    private String alertId;

    /**
     * 规则ID
     */
    @Column(nullable = false)
    private Long ruleId;

    /**
     * 设备ID
     */
    @Column(length = 50, nullable = false)
    private String deviceId;

    /**
     * 告警级别
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AlertRuleEntity.AlertLevel alertLevel;

    /**
     * 告警标题
     */
    @Column(length = 200, nullable = false)
    private String alertTitle;

    /**
     * 告警内容
     */
    @Column(columnDefinition = "TEXT")
    private String alertContent;

    /**
     * 触发值
     */
    @Column(precision = 15, scale = 4)
    private Double triggeredValue;

    /**
     * 阈值
     */
    @Column(precision = 15, scale = 4)
    private Double thresholdValue;

    /**
     * 置信度
     */
    @Column(precision = 5, scale = 4)
    private Double confidenceScore;

    /**
     * 告警时间
     */
    @Column(nullable = false)
    private LocalDateTime alertTime;

    /**
     * 告警状态
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AlertStatus status;

    /**
     * 是否已确认
     */
    @Column(nullable = false)
    private Boolean isConfirmed = false;

    /**
     * 确认时间
     */
    private LocalDateTime confirmedTime;

    /**
     * 确认人
     */
    @Column(length = 50)
    private String confirmedBy;

    /**
     * 确认备注
     */
    @Column(length = 500)
    private String confirmationNote;

    /**
     * 处理时间
     */
    private LocalDateTime resolvedTime;

    /**
     * 处理人
     */
    @Column(length = 50)
    private String resolvedBy;

    /**
     * 处理备注
     */
    @Column(length = 500)
    private String resolutionNote;

    /**
     * 关联的分析结果ID
     */
    @Column(length = 50)
    private String analysisResultId;

    /**
     * 扩展信息JSON
     */
    @Column(columnDefinition = "TEXT")
    private String extendedInfo;

    /**
     * 告警来源
     */
    @Column(length = 50)
    private String alertSource;

    /**
     * 是否已发送邮件通知
     */
    @Column(nullable = false)
    private Boolean emailNotified = false;

    /**
     * 邮件通知时间
     */
    private LocalDateTime emailNotificationTime;

    /**
     * 是否已发送短信通知
     */
    @Column(nullable = false)
    private Boolean smsNotified = false;

    /**
     * 短信通知时间
     */
    private LocalDateTime smsNotificationTime;

    /**
     * 是否已发送WebSocket通知
     */
    @Column(nullable = false)
    private Boolean websocketNotified = false;

    /**
     * WebSocket通知时间
     */
    private LocalDateTime websocketNotificationTime;

    /**
     * 告警状态枚举
     */
    public enum AlertStatus {
        ACTIVE("活跃"),
        ACKNOWLEDGED("已确认"),
        RESOLVED("已处理"),
        SUPPRESSED("已抑制"),
        FALSE_POSITIVE("误报");

        private final String description;

        AlertStatus(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }
}