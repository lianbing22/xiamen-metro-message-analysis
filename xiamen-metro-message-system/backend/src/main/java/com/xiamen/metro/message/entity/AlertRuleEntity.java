package com.xiamen.metro.message.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 告警规则实体
 *
 * @author Xiamen Metro System
 */
@Entity
@Table(name = "alert_rules", indexes = {
    @Index(name = "idx_alert_rule_name", columnList = "ruleName"),
    @Index(name = "idx_alert_rule_device", columnList = "deviceId"),
    @Index(name = "idx_alert_rule_active", columnList = "isActive")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class AlertRuleEntity extends BaseEntity {

    /**
     * 规则名称
     */
    @Column(nullable = false, length = 100)
    private String ruleName;

    /**
     * 规则描述
     */
    @Column(length = 500)
    private String description;

    /**
     * 设备ID，null表示适用于所有设备
     */
    @Column(length = 50)
    private String deviceId;

    /**
     * 规则类型
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RuleType ruleType;

    /**
     * 告警级别
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AlertLevel alertLevel;

    /**
     * 规则条件JSON
     */
    @Column(columnDefinition = "TEXT")
    private String ruleConditions;

    /**
     * 阈值配置JSON
     */
    @Column(columnDefinition = "TEXT")
    private String thresholdConfig;

    /**
     * 检查间隔（分钟）
     */
    @Column(nullable = false)
    private Integer checkIntervalMinutes;

    /**
     * 连续触发次数
     */
    @Column(nullable = false)
    private Integer consecutiveTriggerCount;

    /**
     * 抑制时间（分钟）
     */
    @Column(nullable = false)
    private Integer suppressionMinutes;

    /**
     * 是否激活
     */
    @Column(nullable = false)
    private Boolean isActive = true;

    /**
     * 通知方式
     */
    @ElementCollection
    @CollectionTable(
        name = "alert_rule_notification_methods",
        joinColumns = @JoinColumn(name = "ruleId")
    )
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private List<NotificationMethod> notificationMethods;

    /**
     * 邮件接收人列表
     */
    @ElementCollection
    @CollectionTable(
        name = "alert_rule_email_recipients",
        joinColumns = @JoinColumn(name = "ruleId")
    )
    @Column(length = 200)
    private List<String> emailRecipients;

    /**
     * 短信接收人列表
     */
    @ElementCollection
    @CollectionTable(
        name = "alert_rule_sms_recipients",
        joinColumns = @JoinColumn(name = "ruleId")
    )
    @Column(length = 20)
    private List<String> smsRecipients;

    /**
     * 创建者
     */
    @Column(length = 50)
    private String createdBy;

    /**
     * 最后触发时间
     */
    private LocalDateTime lastTriggeredTime;

    /**
     * 规则优先级
     */
    @Column(nullable = false)
    private Integer priority = 1;

    /**
     * 规则类型枚举
     */
    public enum RuleType {
        THRESHOLD("阈值监控"),
        ANOMALY_DETECTION("异常检测"),
        PERFORMANCE_DEGRADATION("性能下降"),
        FAULT_PREDICTION("故障预测"),
        HEALTH_SCORE("健康评分"),
        CUSTOM("自定义规则");

        private final String description;

        RuleType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 告警级别枚举
     */
    public enum AlertLevel {
        CRITICAL("严重"),
        WARNING("警告"),
        INFO("提醒");

        private final String description;

        AlertLevel(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 通知方式枚举
     */
    public enum NotificationMethod {
        EMAIL("邮件"),
        SMS("短信"),
        WEBSOCKET("系统内消息"),
        ALL("全部");

        private final String description;

        NotificationMethod(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }
}