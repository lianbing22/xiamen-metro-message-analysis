package com.xiamen.metro.message.dto.alert;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 告警记录DTO
 *
 * @author Xiamen Metro System
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlertRecordDTO {

    /**
     * 记录ID
     */
    private Long id;

    /**
     * 告警ID
     */
    @NotBlank(message = "告警ID不能为空")
    private String alertId;

    /**
     * 规则ID
     */
    @NotNull(message = "规则ID不能为空")
    private Long ruleId;

    /**
     * 规则名称
     */
    private String ruleName;

    /**
     * 设备ID
     */
    @NotBlank(message = "设备ID不能为空")
    private String deviceId;

    /**
     * 设备名称（可选）
     */
    private String deviceName;

    /**
     * 告警级别
     */
    @NotNull(message = "告警级别不能为空")
    private AlertRuleDTO.AlertLevel alertLevel;

    /**
     * 告警标题
     */
    @NotBlank(message = "告警标题不能为空")
    private String alertTitle;

    /**
     * 告警内容
     */
    private String alertContent;

    /**
     * 触发值
     */
    private Double triggeredValue;

    /**
     * 阈值
     */
    private Double thresholdValue;

    /**
     * 置信度
     */
    private Double confidenceScore;

    /**
     * 告警时间
     */
    @NotNull(message = "告警时间不能为空")
    private LocalDateTime alertTime;

    /**
     * 告警状态
     */
    @NotNull(message = "告警状态不能为空")
    private AlertStatus status;

    /**
     * 是否已确认
     */
    @Builder.Default
    private Boolean isConfirmed = false;

    /**
     * 确认时间
     */
    private LocalDateTime confirmedTime;

    /**
     * 确认人
     */
    private String confirmedBy;

    /**
     * 确认备注
     */
    private String confirmationNote;

    /**
     * 处理时间
     */
    private LocalDateTime resolvedTime;

    /**
     * 处理人
     */
    private String resolvedBy;

    /**
     * 处理备注
     */
    private String resolutionNote;

    /**
     * 关联的分析结果ID
     */
    private String analysisResultId;

    /**
     * 扩展信息
     */
    private Map<String, Object> extendedInfo;

    /**
     * 告警来源
     */
    private String alertSource;

    /**
     * 通知状态
     */
    @Builder.Default
    private NotificationStatus notificationStatus = NotificationStatus.PENDING;

    /**
     * 创建时间
     */
    private LocalDateTime createdTime;

    /**
     * 更新时间
     */
    private LocalDateTime updatedTime;

    /**
     * 持续时间（分钟）
     */
    private Long durationMinutes;

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

    /**
     * 通知状态枚举
     */
    public enum NotificationStatus {
        PENDING("待发送"),
        SENDING("发送中"),
        SUCCESS("发送成功"),
        FAILED("发送失败"),
        PARTIAL("部分成功");

        private final String description;

        NotificationStatus(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }
}