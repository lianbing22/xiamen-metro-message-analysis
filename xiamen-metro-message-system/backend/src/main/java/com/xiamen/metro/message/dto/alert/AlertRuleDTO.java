package com.xiamen.metro.message.dto.alert;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 告警规则DTO
 *
 * @author Xiamen Metro System
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlertRuleDTO {

    /**
     * 规则ID
     */
    private Long ruleId;

    /**
     * 规则名称
     */
    @NotBlank(message = "规则名称不能为空")
    private String ruleName;

    /**
     * 规则描述
     */
    private String description;

    /**
     * 设备ID，null表示适用于所有设备
     */
    private String deviceId;

    /**
     * 规则类型
     */
    @NotNull(message = "规则类型不能为空")
    private RuleType ruleType;

    /**
     * 告警级别
     */
    @NotNull(message = "告警级别不能为空")
    private AlertLevel alertLevel;

    /**
     * 规则条件
     */
    @NotNull(message = "规则条件不能为空")
    private Map<String, Object> ruleConditions;

    /**
     * 阈值配置
     */
    private Map<String, Object> thresholdConfig;

    /**
     * 检查间隔（分钟）
     */
    @NotNull(message = "检查间隔不能为空")
    @Min(value = 1, message = "检查间隔至少为1分钟")
    private Integer checkIntervalMinutes;

    /**
     * 连续触发次数
     */
    @NotNull(message = "连续触发次数不能为空")
    @Min(value = 1, message = "连续触发次数至少为1")
    private Integer consecutiveTriggerCount;

    /**
     * 抑制时间（分钟）
     */
    @NotNull(message = "抑制时间不能为空")
    @Min(value = 0, message = "抑制时间不能为负数")
    private Integer suppressionMinutes;

    /**
     * 是否激活
     */
    @Builder.Default
    private Boolean isActive = true;

    /**
     * 通知方式
     */
    @NotNull(message = "通知方式不能为空")
    private List<NotificationMethod> notificationMethods;

    /**
     * 邮件接收人列表
     */
    private List<String> emailRecipients;

    /**
     * 短信接收人列表
     */
    private List<String> smsRecipients;

    /**
     * 规则优先级
     */
    @Builder.Default
    @Min(value = 1, message = "优先级至少为1")
    @Max(value = 10, message = "优先级最多为10")
    private Integer priority = 1;

    /**
     * 创建时间
     */
    private LocalDateTime createdTime;

    /**
     * 更新时间
     */
    private LocalDateTime updatedTime;

    /**
     * 创建者
     */
    private String createdBy;

    /**
     * 最后触发时间
     */
    private LocalDateTime lastTriggeredTime;

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
        CRITICAL("严重", 3),
        WARNING("警告", 2),
        INFO("提醒", 1);

        private final String description;
        private final int level;

        AlertLevel(String description, int level) {
            this.description = description;
            this.level = level;
        }

        public String getDescription() {
            return description;
        }

        public int getLevel() {
            return level;
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