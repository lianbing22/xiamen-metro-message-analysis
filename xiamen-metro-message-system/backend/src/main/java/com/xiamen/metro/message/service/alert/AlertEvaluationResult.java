package com.xiamen.metro.message.service.alert;

import com.xiamen.metro.message.entity.AlertRuleEntity;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.HashMap;

/**
 * 告警评估结果
 *
 * @author Xiamen Metro System
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertEvaluationResult {

    /**
     * 是否触发告警
     */
    private boolean triggered;

    /**
     * 告警消息
     */
    private String message;

    /**
     * 触发值
     */
    private Double triggeredValue;

    /**
     * 阈值
     */
    private Double thresholdValue;

    /**
     * 告警级别
     */
    private AlertRuleEntity.AlertLevel severity;

    /**
     * 置信度 (0.0 - 1.0)
     */
    private Double confidence;

    /**
     * 建议措施
     */
    private String recommendation;

    /**
     * 详细信息
     */
    @Builder.Default
    private Map<String, Object> details = new HashMap<>();

    /**
     * 评估时间
     */
    @Builder.Default
    private LocalDateTime evaluationTime = LocalDateTime.now();

    /**
     * 评估耗时（毫秒）
     */
    private Long evaluationTimeMs;

    /**
     * 异常信息
     */
    private String error;

    /**
     * 是否需要立即处理
     */
    private boolean requiresImmediateAction;

    /**
     * 预计影响
     */
    private String expectedImpact;

    /**
     * 评估的指标列表
     */
    @Builder.Default
    private Map<String, Double> evaluatedMetrics = new HashMap<>();

    /**
     * 创建成功的评估结果
     */
    public static AlertEvaluationResult success(boolean triggered, String message) {
        return AlertEvaluationResult.builder()
                .triggered(triggered)
                .message(message)
                .confidence(1.0)
                .build();
    }

    /**
     * 创建触发的告警结果
     */
    public static AlertEvaluationResult triggered(String message, AlertRuleEntity.AlertLevel severity) {
        return AlertEvaluationResult.builder()
                .triggered(true)
                .message(message)
                .severity(severity)
                .confidence(0.8)
                .build();
    }

    /**
     * 创建失败的评估结果
     */
    public static AlertEvaluationResult failure(String errorMessage) {
        return AlertEvaluationResult.builder()
                .triggered(false)
                .message("评估失败")
                .error(errorMessage)
                .confidence(0.0)
                .build();
    }

    /**
     * 创建详细的告警结果
     */
    public static AlertEvaluationResult detailed(String message,
                                                AlertRuleEntity.AlertLevel severity,
                                                Double triggeredValue,
                                                Double thresholdValue,
                                                Double confidence,
                                                String recommendation) {
        return AlertEvaluationResult.builder()
                .triggered(true)
                .message(message)
                .severity(severity)
                .triggeredValue(triggeredValue)
                .thresholdValue(thresholdValue)
                .confidence(confidence)
                .recommendation(recommendation)
                .build();
    }

    /**
     * 添加详细信息
     */
    public AlertEvaluationResult addDetail(String key, Object value) {
        this.details.put(key, value);
        return this;
    }

    /**
     * 添加评估指标
     */
    public AlertEvaluationResult addEvaluatedMetric(String metricName, Double value) {
        this.evaluatedMetrics.put(metricName, value);
        return this;
    }

    /**
     * 设置严重级别并计算是否需要立即处理
     */
    public AlertEvaluationResult withSeverity(AlertRuleEntity.AlertLevel severity) {
        this.severity = severity;
        this.requiresImmediateAction = (severity == AlertRuleEntity.AlertLevel.CRITICAL);
        return this;
    }

    /**
     * 生成告警标题
     */
    public String generateAlertTitle(String deviceId, String ruleName) {
        if (!triggered) {
            return null;
        }

        StringBuilder title = new StringBuilder();
        title.append("[").append(severity.getDescription()).append("] ");

        if (deviceId != null) {
            title.append("设备 ").append(deviceId).append(" ");
        }

        title.append(ruleName);

        return title.toString();
    }

    /**
     * 生成告警内容
     */
    public String generateAlertContent() {
        if (!triggered) {
            return null;
        }

        StringBuilder content = new StringBuilder();
        content.append(message);

        if (triggeredValue != null && thresholdValue != null) {
            content.append(String.format(" (当前值: %.2f, 阈值: %.2f)", triggeredValue, thresholdValue));
        }

        if (confidence != null) {
            content.append(String.format(" [置信度: %.1f%%]", confidence * 100));
        }

        if (recommendation != null && !recommendation.trim().isEmpty()) {
            content.append("\n建议措施: ").append(recommendation);
        }

        if (expectedImpact != null && !expectedImpact.trim().isEmpty()) {
            content.append("\n预计影响: ").append(expectedImpact);
        }

        return content.toString();
    }

    /**
     * 转换为扩展信息Map
     */
    public Map<String, Object> toExtendedInfoMap() {
        Map<String, Object> extendedInfo = new HashMap<>(details);
        extendedInfo.put("evaluation_time", evaluationTime);

        if (evaluationTimeMs != null) {
            extendedInfo.put("evaluation_time_ms", evaluationTimeMs);
        }

        if (triggeredValue != null) {
            extendedInfo.put("triggered_value", triggeredValue);
        }

        if (thresholdValue != null) {
            extendedInfo.put("threshold_value", thresholdValue);
        }

        if (confidence != null) {
            extendedInfo.put("confidence_score", confidence);
        }

        extendedInfo.put("requires_immediate_action", requiresImmediateAction);
        extendedInfo.put("evaluated_metrics", evaluatedMetrics);

        return extendedInfo;
    }

    /**
     * 判断是否为有效结果
     */
    public boolean isValid() {
        return error == null || error.trim().isEmpty();
    }

    /**
     * 判断是否为高置信度结果
     */
    public boolean isHighConfidence() {
        return confidence != null && confidence >= 0.7;
    }

    /**
     * 获取风险等级数值
     */
    public int getSeverityLevel() {
        if (severity == null) return 0;

        switch (severity) {
            case CRITICAL: return 3;
            case WARNING: return 2;
            case INFO: return 1;
            default: return 0;
        }
    }
}