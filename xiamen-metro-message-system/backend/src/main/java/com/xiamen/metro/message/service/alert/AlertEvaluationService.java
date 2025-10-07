package com.xiamen.metro.message.service.alert;

import com.xiamen.metro.message.entity.AlertRuleEntity;
import com.xiamen.metro.message.service.pump.PumpAnomalyDetectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 告警评估服务
 * 集成现有的分析服务进行告警评估
 *
 * @author Xiamen Metro System
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlertEvaluationService {

    private final PumpAnomalyDetectionService anomalyDetectionService;
    // 这里可以注入其他分析服务

    /**
     * 评估异常检测规则
     */
    public AlertEvaluationResult evaluateAnomaly(AlertRuleEntity rule, AlertEvaluationContext context) {
        try {
            log.debug("评估异常检测规则: {} (设备: {})", rule.getRuleName(), context.getDeviceId());

            // 根据规则条件确定异常类型
            String anomalyType = determineAnomalyType(rule);

            // 调用相应的异常检测服务
            Map<String, Object> detectionResult = performAnomalyDetection(context, anomalyType);

            return processDetectionResult(rule, detectionResult, context);

        } catch (Exception e) {
            log.error("异常检测评估失败: {}", rule.getRuleName(), e);
            return AlertEvaluationResult.failure("异常检测评估异常: " + e.getMessage());
        }
    }

    /**
     * 评估性能指标
     */
    public AlertEvaluationResult evaluatePerformance(AlertRuleEntity rule, AlertEvaluationContext context) {
        try {
            Map<String, Object> conditions = parseJsonToMap(rule.getRuleConditions());
            String metricName = (String) conditions.get("metricName");
            Double threshold = (Double) conditions.get("threshold");
            String comparison = (String) conditions.get("comparison");

            if (metricName == null || threshold == null) {
                return AlertEvaluationResult.failure("性能规则配置不完整");
            }

            Double currentValue = context.getMetricValue(metricName);
            if (currentValue == null) {
                return AlertEvaluationResult.failure("性能指标不存在: " + metricName);
            }

            boolean violated = compareValues(currentValue, comparison, threshold);

            if (violated) {
                String message = String.format("性能指标 %s 违反阈值: 当前值 %.2f %s %.2f",
                        metricName, currentValue, getComparisonSymbol(comparison), threshold);

                return AlertEvaluationResult.builder()
                        .triggered(true)
                        .message(message)
                        .triggeredValue(currentValue)
                        .thresholdValue(threshold)
                        .severity(rule.getAlertLevel())
                        .confidence(0.85)
                        .recommendation(generatePerformanceRecommendation(metricName, currentValue, threshold))
                        .build();
            }

            return AlertEvaluationResult.success(false, "性能指标正常");

        } catch (Exception e) {
            log.error("性能评估失败: {}", rule.getRuleName(), e);
            return AlertEvaluationResult.failure("性能评估异常: " + e.getMessage());
        }
    }

    /**
     * 评估系统健康状态
     */
    public AlertEvaluationResult evaluateSystemHealth(AlertRuleEntity rule, AlertEvaluationContext context) {
        try {
            Double healthScore = context.getMetricValue("health_score");
            if (healthScore == null) {
                return AlertEvaluationResult.failure("健康评分不存在");
            }

            Map<String, Object> conditions = parseJsonToMap(rule.getRuleConditions());
            Double healthThreshold = (Double) conditions.getOrDefault("healthThreshold", 60.0);

            boolean unhealthy = healthScore < healthThreshold;

            if (unhealthy) {
                String message = String.format("系统健康状态异常: 健康评分 %.2f 低于阈值 %.2f",
                        healthScore, healthThreshold);

                // 分析健康评分低的可能原因
                String rootCauseAnalysis = analyzeHealthRootCause(context);

                return AlertEvaluationResult.builder()
                        .triggered(true)
                        .message(message)
                        .triggeredValue(healthScore)
                        .thresholdValue(healthThreshold)
                        .severity(determineHealthSeverity(healthScore))
                        .confidence(0.9)
                        .recommendation(generateHealthRecommendation(healthScore, rootCauseAnalysis))
                        .addDetail("rootCauseAnalysis", rootCauseAnalysis)
                        .build();
            }

            return AlertEvaluationResult.success(false, "系统健康状态正常");

        } catch (Exception e) {
            log.error("健康状态评估失败: {}", rule.getRuleName(), e);
            return AlertEvaluationResult.failure("健康状态评估异常: " + e.getMessage());
        }
    }

    /**
     * 确定异常类型
     */
    private String determineAnomalyType(AlertRuleEntity rule) {
        Map<String, Object> conditions = parseJsonToMap(rule.getRuleConditions());
        return (String) conditions.getOrDefault("anomalyType", "GENERAL");
    }

    /**
     * 执行异常检测
     */
    private Map<String, Object> performAnomalyDetection(AlertEvaluationContext context, String anomalyType) {
        Map<String, Object> result = new HashMap<>();

        try {
            // 这里应该调用实际的异常检测逻辑
            // 简化实现，基于现有指标判断
            Double anomalyRate = context.getMetricValue("anomaly_rate");
            Double confidenceScore = context.getMetricValue("confidence_score");

            boolean isAnomalous = (anomalyRate != null && anomalyRate > 20.0) ||
                                 (confidenceScore != null && confidenceScore < 0.5);

            result.put("isAnomalous", isAnomalous);
            result.put("anomalyRate", anomalyRate);
            result.put("confidence", confidenceScore);
            result.put("anomalyType", anomalyType);

        } catch (Exception e) {
            log.error("执行异常检测失败", e);
            result.put("isAnomalous", false);
            result.put("error", e.getMessage());
        }

        return result;
    }

    /**
     * 处理检测结果
     */
    private AlertEvaluationResult processDetectionResult(AlertRuleEntity rule,
                                                       Map<String, Object> detectionResult,
                                                       AlertEvaluationContext context) {
        Boolean isAnomalous = (Boolean) detectionResult.get("isAnomalous");

        if (isAnomalous == null || !isAnomalous) {
            return AlertEvaluationResult.success(false, "未检测到异常");
        }

        Double anomalyRate = (Double) detectionResult.get("anomalyRate");
        Double confidence = (Double) detectionResult.get("confidence");

        String message = String.format("检测到异常: 异常率 %.1f%%, 置信度 %.1f%%",
                anomalyRate != null ? anomalyRate : 0.0,
                confidence != null ? confidence * 100 : 0.0);

        return AlertEvaluationResult.builder()
                .triggered(true)
                .message(message)
                .triggeredValue(anomalyRate)
                .thresholdValue(20.0) // 默认异常率阈值
                .severity(rule.getAlertLevel())
                .confidence(confidence != null ? confidence : 0.5)
                .recommendation("建议进行详细设备检查和数据分析")
                .addDetail("detectionResult", detectionResult)
                .build();
    }

    /**
     * 分析健康评分低的根本原因
     */
    private String analyzeHealthRootCause(AlertEvaluationContext context) {
        StringBuilder rootCause = new StringBuilder();

        // 检查各项指标
        Double efficiency = context.getMetricValue("efficiency_score");
        Double reliability = context.getMetricValue("reliability_score");
        Double maintenance = context.getMetricValue("maintenance_score");
        Double failureProbability = context.getMetricValue("failure_probability");

        if (efficiency != null && efficiency < 70.0) {
            rootCause.append("设备效率偏低; ");
        }

        if (reliability != null && reliability < 70.0) {
            rootCause.append("设备可靠性不足; ");
        }

        if (maintenance != null && maintenance < 70.0) {
            rootCause.append("维护状况不佳; ");
        }

        if (failureProbability != null && failureProbability > 0.5) {
            rootCause.append("故障概率较高; ");
        }

        if (rootCause.length() == 0) {
            rootCause.append("整体性能指标下降");
        }

        return rootCause.toString();
    }

    /**
     * 确定健康问题严重级别
     */
    private AlertRuleEntity.AlertLevel determineHealthSeverity(Double healthScore) {
        if (healthScore < 30.0) {
            return AlertRuleEntity.AlertLevel.CRITICAL;
        } else if (healthScore < 50.0) {
            return AlertRuleEntity.AlertLevel.WARNING;
        } else {
            return AlertRuleEntity.AlertLevel.INFO;
        }
    }

    /**
     * 生成性能建议
     */
    private String generatePerformanceRecommendation(String metricName, Double currentValue, Double threshold) {
        switch (metricName.toLowerCase()) {
            case "average_power":
                return "建议检查电机负载和供电系统，优化运行参数";
            case "average_vibration":
                return "建议检查设备安装基础和旋转部件平衡性";
            case "efficiency_score":
                return "建议进行设备维护保养，检查管路阻力";
            default:
                return "建议关注设备运行状态，必要时进行维护";
        }
    }

    /**
     * 生成健康建议
     */
    private String generateHealthRecommendation(Double healthScore, String rootCause) {
        StringBuilder recommendation = new StringBuilder();

        if (healthScore < 30.0) {
            recommendation.append("立即停止设备运行，进行全面检查和维护。");
        } else if (healthScore < 50.0) {
            recommendation.append("尽快安排设备检修，避免故障扩大。");
        } else {
            recommendation.append("加强设备监控，制定预防性维护计划。");
        }

        if (!rootCause.trim().isEmpty()) {
            recommendation.append(" 主要原因: ").append(rootCause);
        }

        return recommendation.toString();
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
     * 解析JSON字符串为Map（简化实现）
     */
    private Map<String, Object> parseJsonToMap(String json) {
        try {
            // 实际项目中应该使用ObjectMapper
            if (json == null || json.trim().isEmpty()) {
                return new HashMap<>();
            }
            return new HashMap<>(); // 临时返回空Map
        } catch (Exception e) {
            log.error("解析JSON失败: {}", json, e);
            return new HashMap<>();
        }
    }
}