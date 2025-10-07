package com.xiamen.metro.message.service.alert;

import com.xiamen.metro.message.dto.pump.PumpAnalysisResponseDTO;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.HashMap;

/**
 * 告警评估上下文
 *
 * @author Xiamen Metro System
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertEvaluationContext {

    /**
     * 设备ID
     */
    private String deviceId;

    /**
     * 设备名称（可选）
     */
    private String deviceName;

    /**
     * 分析时间
     */
    private LocalDateTime analysisTime;

    /**
     * 指标值映射
     */
    @Builder.Default
    private Map<String, Double> metricValues = new HashMap<>();

    /**
     * 扩展信息
     */
    @Builder.Default
    private Map<String, Object> extendedInfo = new HashMap<>();

    /**
     * 水泵分析结果
     */
    private PumpAnalysisResponseDTO pumpAnalysisResult;

    /**
     * 分析ID
     */
    private String analysisId;

    /**
     * 数据来源
     */
    private String dataSource;

    /**
     * 构造方法 - 基于水泵分析结果
     */
    public static AlertEvaluationContext fromPumpAnalysis(String deviceId,
                                                         PumpAnalysisResponseDTO analysisResult) {
        AlertEvaluationContextBuilder builder = AlertEvaluationContext.builder()
                .deviceId(deviceId)
                .analysisTime(analysisResult.getAnalysisTime())
                .analysisId(analysisResult.getAnalysisId())
                .dataSource("pump_analysis")
                .pumpAnalysisResult(analysisResult);

        // 提取关键指标
        Map<String, Double> metrics = new HashMap<>();

        // 健康评分
        metrics.put("health_score", analysisResult.getOverallHealthScore());

        // 性能指标
        if (analysisResult.getPerformanceMetrics() != null) {
            PumpAnalysisResponseDTO.PerformanceMetrics perf = analysisResult.getPerformanceMetrics();
            metrics.put("efficiency_score", perf.getEfficiencyScore());
            metrics.put("reliability_score", perf.getReliabilityScore());
            metrics.put("maintenance_score", perf.getMaintenanceScore());
            metrics.put("average_power", perf.getAveragePower());
            metrics.put("average_vibration", perf.getAverageVibration());
            metrics.put("max_vibration", perf.getMaxVibration());
        }

        // 故障预测
        if (analysisResult.getPredictionInfo() != null) {
            metrics.put("failure_probability", analysisResult.getPredictionInfo().getFailureProbability());
            metrics.put("remaining_useful_life",
                       (double) analysisResult.getPredictionInfo().getRemainingUsefulLifeDays());
        }

        // 风险等级转换为数值
        metrics.put("risk_level", convertRiskLevelToNumeric(analysisResult.getRiskLevel()));

        // 置信度
        metrics.put("confidence_score", analysisResult.getConfidenceScore());

        builder.metricValues(metrics);

        // 添加扩展信息
        Map<String, Object> extended = new HashMap<>();
        extended.put("risk_level", analysisResult.getRiskLevel());
        extended.put("model_version", analysisResult.getModelVersion());
        extended.put("processing_time_ms", analysisResult.getProcessingTimeMs());
        extended.put("analysis_results_count", analysisResult.getAnalysisResults().size());

        builder.extendedInfo(extended);

        return builder.build();
    }

    /**
     * 获取指标值
     */
    public Double getMetricValue(String metricName) {
        return metricValues.get(metricName);
    }

    /**
     * 设置指标值
     */
    public void setMetricValue(String metricName, Double value) {
        metricValues.put(metricName, value);
    }

    /**
     * 获取扩展信息
     */
    @SuppressWarnings("unchecked")
    public <T> T getExtendedInfo(String key, Class<T> type) {
        Object value = extendedInfo.get(key);
        if (value != null && type.isInstance(value)) {
            return (T) value;
        }
        return null;
    }

    /**
     * 设置扩展信息
     */
    public void setExtendedInfo(String key, Object value) {
        extendedInfo.put(key, value);
    }

    /**
     * 风险等级转换为数值
     */
    private double convertRiskLevelToNumeric(String riskLevel) {
        if (riskLevel == null) return 0.0;

        switch (riskLevel.toUpperCase()) {
            case "CRITICAL": return 4.0;
            case "HIGH": return 3.0;
            case "MEDIUM": return 2.0;
            case "LOW": return 1.0;
            default: return 0.0;
        }
    }

    /**
     * 创建简单的评估上下文
     */
    public static AlertEvaluationContext createSimple(String deviceId, Map<String, Double> metrics) {
        return AlertEvaluationContext.builder()
                .deviceId(deviceId)
                .analysisTime(LocalDateTime.now())
                .dataSource("manual")
                .metricValues(new HashMap<>(metrics))
                .build();
    }

    /**
     * 复制上下文
     */
    public AlertEvaluationContext copy() {
        return AlertEvaluationContext.builder()
                .deviceId(this.deviceId)
                .deviceName(this.deviceName)
                .analysisTime(this.analysisTime)
                .metricValues(new HashMap<>(this.metricValues))
                .extendedInfo(new HashMap<>(this.extendedInfo))
                .pumpAnalysisResult(this.pumpAnalysisResult)
                .analysisId(this.analysisId)
                .dataSource(this.dataSource)
                .build();
    }
}