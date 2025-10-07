package com.xiamen.metro.message.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * 告警通知记录实体
 *
 * @author Xiamen Metro System
 */
@Entity
@Table(name = "alert_notifications", indexes = {
    @Index(name = "idx_notification_alert", columnList = "alertId"),
    @Index(name = "idx_notification_type", columnList = "notificationType"),
    @Index(name = "idx_notification_status", columnList = "status"),
    @Index(name = "idx_notification_time", columnList = "notificationTime")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class AlertNotificationEntity extends BaseEntity {

    /**
     * 告警ID
     */
    @Column(length = 50, nullable = false)
    private String alertId;

    /**
     * 通知类型
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType notificationType;

    /**
     * 接收人
     */
    @Column(length = 200, nullable = false)
    private String recipient;

    /**
     * 通知主题
     */
    @Column(length = 200, nullable = false)
    private String subject;

    /**
     * 通知内容
     */
    @Column(columnDefinition = "TEXT")
    private String content;

    /**
     * 通知时间
     */
    @Column(nullable = false)
    private LocalDateTime notificationTime;

    /**
     * 发送状态
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationStatus status;

    /**
     * 发送结果
     */
    @Column(length = 500)
    private String sendResult;

    /**
     * 重试次数
     */
    @Column(nullable = false)
    private Integer retryCount = 0;

    /**
     * 下次重试时间
     */
    private LocalDateTime nextRetryTime;

    /**
     * 最后重试时间
     */
    private LocalDateTime lastRetryTime;

    /**
     * 通知模板ID
     */
    @Column(length = 50)
    private String templateId;

    /**
     * 通知模板参数JSON
     */
    @Column(columnDefinition = "TEXT")
    private String templateParameters;

    /**
     * 通知类型枚举
     */
    public enum NotificationType {
        EMAIL("邮件"),
        SMS("短信"),
        WEBSOCKET("WebSocket"),
        SYSTEM("系统通知");

        private final String description;

        NotificationType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 发送状态枚举
     */
    public enum NotificationStatus {
        PENDING("待发送"),
        SENDING("发送中"),
        SUCCESS("发送成功"),
        FAILED("发送失败"),
        RETRY("重试中"),
        SKIPPED("跳过");

        private final String description;

        NotificationStatus(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }
}