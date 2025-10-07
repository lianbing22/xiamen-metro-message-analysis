package com.xiamen.metro.message.service.alert;

import com.xiamen.metro.message.entity.AlertRuleEntity;
import com.xiamen.metro.message.dto.alert.AlertRuleDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

/**
 * 告警规则引擎
 * 负责执行告警规则判断
 *
 * @author Xiamen Metro System
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlertRuleEngine {

    private final AlertEvaluationService alertEvaluationService;

    /**
     * 评估单个规则
     */
    public AlertEvaluationResult evaluateRule(AlertRuleEntity rule, AlertEvaluationContext context) {
        try {
            log.debug("评估告警规则: {} (设备: {})", rule.getRuleName(), context.getDeviceId());

            // 根据规则类型选择评估策略
            AlertEvaluationResult result;
            switch (rule.getRuleType()) {
                case THRESHOLD:
                    result = evaluateThresholdRule(rule, context);
                    break;
                case ANOMALY_DETECTION:
                    result = evaluateAnomalyDetectionRule(rule, context);
                    break;
                case PERFORMANCE_DEGRADATION:
                    result = evaluatePerformanceDegradationRule(rule, context);
                    break;
                case FAULT_PREDICTION:
                    result = evaluateFaultPredictionRule(rule, context);
                    break;
                case HEALTH_SCORE:
                    result = evaluateHealthScoreRule(rule, context);
                    break;
                case CUSTOM:
                    result = evaluateCustomRule(rule, context);
                    break;
                default:
                    result = AlertEvaluationResult.builder()
                            .triggered(false)
                            .message("未知的规则类型: " + rule.getRuleType())
                            .build();
            }

            log.debug("规则评估完成: {} -> {}", rule.getRuleName(), result.isTriggered());
            return result;

        } catch (Exception e) {
            log.error("规则评估失败: {}", rule.getRuleName(), e);
            return AlertEvaluationResult.builder()
                    .triggered(false)
                    .message("规则评估异常: " + e.getMessage())
                    .build();
        }
    }

    /**
     * 评估阈值规则
     */
    private AlertEvaluationResult evaluateThresholdRule(AlertRuleEntity rule, AlertEvaluationContext context) {
        Map<String, Object> conditions = parseJsonToMap(rule.getRuleConditions());
        Map<String, Object> thresholds = parseJsonToMap(rule.getThresholdConfig());

        String metricName = (String) conditions.get("metricName");
        String comparison = (String) conditions.get("comparison");
        Double threshold = (Double) thresholds.get("value");

        if (metricName == null || comparison == null || threshold == null) {
            return AlertEvaluationResult.builder()
                    .triggered(false)
                    .message("阈值规则配置不完整")
                    .build();
        }

        Double currentValue = context.getMetricValue(metricName);
        if (currentValue == null) {
            return AlertEvaluationResult.builder()
                    .triggered(false)
                    .message("指标值不存在: " + metricName)
                    .build();
        }

        boolean triggered = compareValues(currentValue, comparison, threshold);

        if (triggered) {
            String message = String.format("指标 %s 当前值 %.2f %s 阈值 %.2f",
                    metricName, currentValue, getComparisonSymbol(comparison), threshold);

            return AlertEvaluationResult.builder()
                    .triggered(true)
                    .message(message)
                    .triggeredValue(currentValue)
                    .thresholdValue(threshold)
                    .severity(rule.getAlertLevel())
                    .confidence(0.9)
                    .build();
        }

        return AlertEvaluationResult.builder()
                .triggered(false)
                .message("指标值正常")
                .build();
    }

    /**
     * 评估异常检测规则
     */
    private AlertEvaluationResult evaluateAnomalyDetectionRule(AlertRuleEntity rule, AlertEvaluationContext context) {
        // 调用现有的异常检测服务
        return alertEvaluationService.evaluateAnomaly(rule, context);
    }

    /**
     * 评估性能下降规则
     */
    private AlertEvaluationResult evaluatePerformanceDegradationRule(AlertRuleEntity rule, AlertEvaluationContext context) {
        Map<String, Object> conditions = parseJsonToMap(rule.getRuleConditions());
        Double degradationThreshold = (Double) conditions.get("degradationThreshold");

        if (degradationThreshold == null) {
            degradationThreshold = 20.0; // 默认下降20%
        }

        Double currentPerformance = context.getMetricValue("performance_score");
        if (currentPerformance == null) {
            return AlertEvaluationResult.builder()
                    .triggered(false)
                    .message("性能指标不存在")
                    .build();
        }

        // 简化的性能下降检测
        boolean degraded = currentPerformance < (100.0 - degradationThreshold);

        if (degraded) {
            return AlertEvaluationResult.builder()
                    .triggered(true)
                    .message(String.format("性能下降至 %.2f%%，低于阈值 %.2f%%",
                            currentPerformance, 100.0 - degradationThreshold))
                    .triggeredValue(currentPerformance)
                    .thresholdValue(100.0 - degradationThreshold)
                    .severity(rule.getAlertLevel())
                    .confidence(0.8)
                    .build();
        }

        return AlertEvaluationResult.builder()
                .triggered(false)
                .message("性能正常")
                .build();
    }

    /**
     * 评估故障预测规则
     */
    private AlertEvaluationResult evaluateFaultPredictionRule(AlertRuleEntity rule, AlertEvaluationContext context) {
        Map<String, Object> conditions = parseJsonToMap(rule.getRuleConditions());
        Double failureProbabilityThreshold = (Double) conditions.get("failureProbabilityThreshold");

        if (failureProbabilityThreshold == null) {
            failureProbabilityThreshold = 0.7; // 默认70%概率
        }

        Double failureProbability = context.getMetricValue("failure_probability");
        if (failureProbability == null) {
            return AlertEvaluationResult.builder()
                    .triggered(false)
                    .message("故障概率不存在")
                    .build();
        }

        boolean predictedFailure = failureProbability >= failureProbabilityThreshold;

        if (predictedFailure) {
            return AlertEvaluationResult.builder()
                    .triggered(true)
                    .message(String.format("故障概率 %.2f%% 超过阈值 %.2f%%",
                            failureProbability * 100, failureProbabilityThreshold * 100))
                    .triggeredValue(failureProbability)
                    .thresholdValue(failureProbabilityThreshold)
                    .severity(rule.getAlertLevel())
                    .confidence(0.7)
                    .build();
        }

        return AlertEvaluationResult.builder()
                .triggered(false)
                .message("故障概率在正常范围")
                .build();
    }

    /**
     * 评估健康评分规则
     */
    private AlertEvaluationResult evaluateHealthScoreRule(AlertRuleEntity rule, AlertEvaluationContext context) {
        Map<String, Object> conditions = parseJsonToMap(rule.getRuleConditions());
        Double healthScoreThreshold = (Double) conditions.get("healthScoreThreshold");

        if (healthScoreThreshold == null) {
            healthScoreThreshold = 60.0; // 默认健康评分阈值
        }

        Double healthScore = context.getMetricValue("health_score");
        if (healthScore == null) {
            return AlertEvaluationResult.builder()
                    .triggered(false)
                    .message("健康评分不存在")
                    .build();
        }

        boolean unhealthy = healthScore < healthScoreThreshold;

        if (unhealthy) {
            return AlertEvaluationResult.builder()
                    .triggered(true)
                    .message(String.format("健康评分 %.2f 低于阈值 %.2f", healthScore, healthScoreThreshold))
                    .triggeredValue(healthScore)
                    .thresholdValue(healthScoreThreshold)
                    .severity(rule.getAlertLevel())
                    .confidence(0.85)
                    .build();
        }

        return AlertEvaluationResult.builder()
                .triggered(false)
                .message("健康评分正常")
                .build();
    }

    /**
     * 评估自定义规则
     */
    private AlertEvaluationResult evaluateCustomRule(AlertRuleEntity rule, AlertEvaluationContext context) {
        // 这里可以实现更复杂的自定义规则逻辑
        // 例如使用Drools规则引擎
        log.info("执行自定义规则: {}", rule.getRuleName());

        // 简化实现：基于JSON配置的条件判断
        Map<String, Object> conditions = parseJsonToMap(rule.getRuleConditions());

        // 示例：检查多个指标的组合条件
        boolean allConditionsMet = true;
        StringBuilder messageBuilder = new StringBuilder();

        for (Map.Entry<String, Object> entry : conditions.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            if (key.startsWith("metric_")) {
                String metricName = key.substring(7); // 移除 "metric_" 前缀
                Double expectedValue = ((Number) value).doubleValue();
                Double actualValue = context.getMetricValue(metricName);

                if (actualValue == null || actualValue < expectedValue) {
                    allConditionsMet = false;
                    messageBuilder.append(String.format("指标 %s 不满足条件 (期望: %.2f, 实际: %.2f); ",
                            metricName, expectedValue, actualValue));
                }
            }
        }

        if (allConditionsMet) {
            return AlertEvaluationResult.builder()
                    .triggered(true)
                    .message("自定义规则条件全部满足")
                    .severity(rule.getAlertLevel())
                    .confidence(0.75)
                    .build();
        } else {
            return AlertEvaluationResult.builder()
                    .triggered(false)
                    .message("自定义规则条件未满足: " + messageBuilder.toString())
                    .build();
        }
    }

    /**
     * 比较值
     */
    private boolean compareValues(Double currentValue, String comparison, Double threshold) {
        switch (comparison.toLowerCase()) {
            case "gt":
            case ">":
                return currentValue > threshold;
            case "gte":
            case ">=":
                return currentValue >= threshold;
            case "lt":
            case "<":
                return currentValue < threshold;
            case "lte":
            case "<=":
                return currentValue <= threshold;
            case "eq":
            case "==":
                return Math.abs(currentValue - threshold) < 0.0001;
            case "ne":
            case "!=":
                return Math.abs(currentValue - threshold) >= 0.0001;
            default:
                log.warn("未知的比较操作: {}", comparison);
                return false;
        }
    }

    /**
     * 获取比较符号
     */
    private String getComparisonSymbol(String comparison) {
        switch (comparison.toLowerCase()) {
            case "gt": return ">";
            case "gte": return ">=";
            case "lt": return "<";
            case "lte": return "<=";
            case "eq": return "=";
            case "ne": return "!=";
            default: return comparison;
        }
    }

    /**
     * 解析JSON字符串为Map
     */
    private Map<String, Object> parseJsonToMap(String json) {
        try {
            // 简化实现，实际项目中应该使用Jackson或其他JSON库
            if (json == null || json.trim().isEmpty()) {
                return new HashMap<>();
            }
            // 这里应该使用ObjectMapper解析JSON
            // return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
            return new HashMap<>(); // 临时返回空Map
        } catch (Exception e) {
            log.error("解析JSON失败: {}", json, e);
            return new HashMap<>();
        }
    }
}