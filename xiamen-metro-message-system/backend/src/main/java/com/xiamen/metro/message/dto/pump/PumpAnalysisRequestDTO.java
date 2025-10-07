package com.xiamen.metro.message.dto.pump;

import lombok.Data;
import com.fasterxml.jackson.annotation.JsonFormat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 水泵分析请求DTO
 *
 * @author Xiamen Metro System
 */
@Data
public class PumpAnalysisRequestDTO {

    /**
     * 设备ID
     */
    @NotBlank(message = "设备ID不能为空")
    private String deviceId;

    /**
     * 分析开始时间
     */
    @NotNull(message = "开始时间不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    /**
     * 分析结束时间
     */
    @NotNull(message = "结束时间不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;

    /**
     * 分析类型列表
     */
    private List<AnalysisType> analysisTypes;

    /**
     * 是否启用实时分析
     */
    private Boolean enableRealTimeAnalysis = false;

    /**
     * 是否启用缓存
     */
    private Boolean enableCache = true;

    /**
     * 分析深度
     */
    private AnalysisDepth analysisDepth = AnalysisDepth.STANDARD;

    /**
     * 自定义阈值配置
     */
    private ThresholdConfig thresholdConfig;

    /**
     * 机器学习模型配置
     */
    private ModelConfig modelConfig;

    /**
     * 分析类型枚举
     */
    public enum AnalysisType {
        STARTUP_FREQUENCY("启泵频率异常检测"),
        RUNTIME_ANALYSIS("运行时间异常分析"),
        ENERGY_TREND("能耗趋势分析"),
        FAULT_PREDICTION("故障预测"),
        PERFORMANCE_EVALUATION("性能评估"),
        ANOMALY_CLASSIFICATION("异常分类分级");

        private final String description;

        AnalysisType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 分析深度枚举
     */
    public enum AnalysisDepth {
        BASIC("基础分析"),
        STANDARD("标准分析"),
        COMPREHENSIVE("全面分析"),
        ADVANCED("高级分析");

        private final String description;

        AnalysisDepth(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 阈值配置
     */
    @Data
    public static class ThresholdConfig {
        /**
         * 启泵频率阈值（次/小时）
         */
        private Double startupFrequencyThreshold;

        /**
         * 运行时间阈值（分钟）
         */
        private Double runtimeThreshold;

        /**
         * 功率异常阈值（百分比）
         */
        private Double powerAnomalyThreshold;

        /**
         * 振动异常阈值（mm/s）
         */
        private Double vibrationThreshold;

        /**
         * 能耗增长阈值（百分比）
         */
        private Double energyIncreaseThreshold;

        /**
         * 温度异常阈值（°C）
         */
        private Double temperatureThreshold;

        /**
         * 压力异常阈值（kPa）
         */
        private Double pressureThreshold;
    }

    /**
     * 模型配置
     */
    @Data
    public static class ModelConfig {
        /**
         * 模型版本
         */
        private String modelVersion = "1.0";

        /**
         * 预测窗口大小（天）
         */
        private Integer predictionWindowDays = 7;

        /**
         * 置信度阈值
         */
        private Double confidenceThreshold = 0.7;

        /**
         * 训练数据天数
         */
        private Integer trainingDataDays = 30;

        /**
         * 特征工程参数
         */
        private FeatureEngineering featureEngineering;

        /**
         * 特征工程配置
         */
        @Data
        public static class FeatureEngineering {
            /**
             * 是否使用时间特征
             */
            private Boolean useTimeFeatures = true;

            /**
             * 是否使用统计特征
             */
            private Boolean useStatisticalFeatures = true;

            /**
             * 是否使用频域特征
             */
            private Boolean useFrequencyFeatures = false;

            /**
             * 滑动窗口大小
             */
            private Integer slidingWindowSize = 24;
        }
    }
}