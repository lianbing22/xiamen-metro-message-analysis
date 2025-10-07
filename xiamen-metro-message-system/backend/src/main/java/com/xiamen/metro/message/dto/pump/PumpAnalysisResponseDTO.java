package com.xiamen.metro.message.dto.pump;

import lombok.Data;
import lombok.Builder;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 水泵分析响应DTO
 *
 * @author Xiamen Metro System
 */
@Data
@Builder
public class PumpAnalysisResponseDTO {

    /**
     * 分析ID
     */
    private String analysisId;

    /**
     * 设备ID
     */
    private String deviceId;

    /**
     * 分析时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime analysisTime;

    /**
     * 分析状态
     */
    private String status;

    /**
     * 总体健康评分 (0-100)
     */
    private Double overallHealthScore;

    /**
     * 风险等级 (LOW, MEDIUM, HIGH, CRITICAL)
     */
    private String riskLevel;

    /**
     * 分析结果列表
     */
    private List<AnalysisResult> analysisResults;

    /**
     * 性能指标
     */
    private PerformanceMetrics performanceMetrics;

    /**
     * 维护建议
     */
    private MaintenanceRecommendations maintenanceRecommendations;

    /**
     * 预测信息
     */
    private PredictionInfo predictionInfo;

    /**
     * 处理时间（毫秒）
     */
    private Long processingTimeMs;

    /**
     * 是否来自缓存
     */
    private Boolean fromCache;

    /**
     * 置信度
     */
    private Double confidenceScore;

    /**
     * 模型版本
     */
    private String modelVersion;

    /**
     * 分析结果详情
     */
    @Data
    @Builder
    public static class AnalysisResult {
        /**
         * 分析类型
         */
        private String analysisType;

        /**
         * 严重级别 (1-信息, 2-警告, 3-错误, 4-严重)
         */
        private Integer severityLevel;

        /**
         * 置信度
         */
        private Double confidence;

        /**
         * 异常描述
         */
        private String description;

        /**
         * 检测值
         */
        private Double detectedValue;

        /**
         * 预期值/阈值
         */
        private Double expectedValue;

        /**
         * 偏差百分比
         */
        private Double deviationPercentage;

        /**
         * 趋势方向
         */
        private String trendDirection;

        /**
         * 详细指标
         */
        private Map<String, Object> detailedMetrics;

        /**
         * 建议措施
         */
        private List<String> recommendations;
    }

    /**
     * 性能指标
     */
    @Data
    @Builder
    public static class PerformanceMetrics {
        /**
         * 启泵频率（次/小时）
         */
        private Double startupFrequency;

        /**
         * 总运行时间（小时）
         */
        private Double totalRuntimeHours;

        /**
         * 平均功率（kW）
         */
        private Double averagePower;

        /**
         * 总能耗（kWh）
         */
        private Double totalEnergyConsumption;

        /**
         * 平均振动值（mm/s）
         */
        private Double averageVibration;

        /**
         * 最大振动值（mm/s）
         */
        private Double maxVibration;

        /**
         * 平均水压（kPa）
         */
        private Double averagePressure;

        /**
         * 平均流量（m³/h）
         */
        private Double averageFlowRate;

        /**
         * 效率评分（0-100）
         */
        private Double efficiencyScore;

        /**
         * 可靠性评分（0-100）
         */
        private Double reliabilityScore;

        /**
         * 维护评分（0-100）
         */
        private Double maintenanceScore;
    }

    /**
     * 维护建议
     */
    @Data
    @Builder
    public static class MaintenanceRecommendations {
        /**
         * 紧急维护项目
         */
        private List<String> urgentActions;

        /**
         * 计划维护项目
         */
        private List<String> scheduledActions;

        /**
         * 预防性维护建议
         */
        private List<String> preventiveActions;

        /**
         * 监控建议
         */
        private List<String> monitoringRecommendations;

        /**
         * 预计维护成本
         */
        private Double estimatedCost;

        /**
         * 建议维护时间
         */
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime recommendedMaintenanceTime;
    }

    /**
     * 预测信息
     */
    @Data
    @Builder
    public static class PredictionInfo {
        /**
         * 预测故障概率
         */
        private Double failureProbability;

        /**
         * 预测剩余寿命（天）
         */
        private Integer remainingUsefulLifeDays;

        /**
         * 预测故障时间
         */
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime predictedFailureTime;

        /**
         * 预测性能退化趋势
         */
        private String performanceDegradationTrend;

        /**
         * 关键指标预测
         */
        private Map<String, Double> keyMetricsPrediction;

        /**
         * 预测置信区间
         */
        private Map<String, Double[]> confidenceIntervals;
    }
}