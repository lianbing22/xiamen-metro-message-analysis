package com.xiamen.metro.message.service.pump;

import com.xiamen.metro.message.entity.PumpDataEntity;
import com.xiamen.metro.message.dto.pump.PumpAnalysisRequestDTO;
import com.xiamen.metro.message.dto.pump.PumpAnalysisResponseDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 水泵异常检测服务
 *
 * @author Xiamen Metro System
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PumpAnomalyDetectionService {

    private static final double DEFAULT_STARTUP_FREQUENCY_THRESHOLD = 10.0; // 次/小时
    private static final double DEFAULT_RUNTIME_THRESHOLD = 480.0; // 分钟（8小时）
    private static final double DEFAULT_POWER_ANOMALY_THRESHOLD = 20.0; // 百分比
    private static final double DEFAULT_VIBRATION_THRESHOLD = 4.5; // mm/s
    private static final double DEFAULT_ENERGY_INCREASE_THRESHOLD = 15.0; // 百分比
    private static final double DEFAULT_TEMPERATURE_THRESHOLD = 60.0; // °C

    /**
     * 启泵频率异常检测
     */
    public PumpAnalysisResponseDTO.AnalysisResult detectStartupFrequencyAnomaly(
            List<PumpDataEntity> pumpData, PumpAnalysisRequestDTO.ThresholdConfig thresholdConfig) {

        double threshold = thresholdConfig != null && thresholdConfig.getStartupFrequencyThreshold() != null ?
                thresholdConfig.getStartupFrequencyThreshold() : DEFAULT_STARTUP_FREQUENCY_THRESHOLD;

        // 计算启泵事件
        List<LocalDateTime> startEvents = pumpData.stream()
                .filter(data -> data.getPumpStatus() != null && data.getPumpStatus() == 1)
                .map(PumpDataEntity::getTimestamp)
                .sorted()
                .collect(Collectors.toList());

        if (startEvents.size() < 2) {
            return createAnalysisResult("启泵频率异常检测", 1, 0.5,
                    "数据不足，无法检测启泵频率异常", null, threshold, null, "STABLE");
        }

        // 计算时间窗口内的启泵频率
        LocalDateTime startTime = pumpData.get(0).getTimestamp();
        LocalDateTime endTime = pumpData.get(pumpData.size() - 1).getTimestamp();
        double hours = ChronoUnit.MINUTES.between(startTime, endTime) / 60.0;
        double startupFrequency = startEvents.size() / hours;

        // 检测异常
        double deviation = ((startupFrequency - threshold) / threshold) * 100;
        int severityLevel = calculateSeverityLevel(Math.abs(deviation));

        String description = String.format("启泵频率: %.2f次/小时, 阈值: %.2f次/小时, 偏差: %.1f%%",
                startupFrequency, threshold, deviation);

        if (severityLevel > 1) {
            description += String.format(" - %s", deviation > 0 ? "频繁启泵" : "启泵不足");
        }

        Map<String, Object> detailedMetrics = new HashMap<>();
        detailedMetrics.put("totalStartEvents", startEvents.size());
        detailedMetrics.put("timeWindowHours", hours);
        detailedMetrics.put("avgIntervalMinutes", hours * 60 / startEvents.size());

        return PumpAnalysisResponseDTO.AnalysisResult.builder()
                .analysisType("启泵频率异常检测")
                .severityLevel(severityLevel)
                .confidence(Math.min(0.9, 0.6 + startEvents.size() * 0.01))
                .description(description)
                .detectedValue(startupFrequency)
                .expectedValue(threshold)
                .deviationPercentage(deviation)
                .trendDirection(deviation > 0 ? "INCREASING" : "DECREASING")
                .detailedMetrics(detailedMetrics)
                .recommendations(generateStartupFrequencyRecommendations(deviation, severityLevel))
                .build();
    }

    /**
     * 运行时间异常分析
     */
    public PumpAnalysisResponseDTO.AnalysisResult detectRuntimeAnomaly(
            List<PumpDataEntity> pumpData, PumpAnalysisRequestDTO.ThresholdConfig thresholdConfig) {

        double threshold = thresholdConfig != null && thresholdConfig.getRuntimeThreshold() != null ?
                thresholdConfig.getRuntimeThreshold() : DEFAULT_RUNTIME_THRESHOLD;

        List<Double> runtimes = pumpData.stream()
                .filter(data -> data.getRuntimeMinutes() != null && data.getRuntimeMinutes() > 0)
                .map(PumpDataEntity::getRuntimeMinutes)
                .collect(Collectors.toList());

        if (runtimes.isEmpty()) {
            return createAnalysisResult("运行时间异常分析", 1, 0.3,
                    "无有效运行时间数据", null, threshold, null, "STABLE");
        }

        double avgRuntime = TimeSeriesAnalyzer.mean(runtimes);
        double maxRuntime = Collections.max(runtimes);
        double stdDev = TimeSeriesAnalyzer.standardDeviation(runtimes);

        // 检测异常
        double deviation = ((avgRuntime - threshold) / threshold) * 100;
        int severityLevel = calculateSeverityLevel(Math.abs(deviation));

        // 检测运行时间趋势
        TimeSeriesAnalyzer.TrendAnalysisResult trend = TimeSeriesAnalyzer.analyzeTrend(runtimes);

        String description = String.format("平均运行时间: %.1f分钟, 最大运行时间: %.1f分钟, 阈值: %.1f分钟, 偏差: %.1f%%",
                avgRuntime, maxRuntime, threshold, deviation);

        if (severityLevel > 1) {
            description += String.format(" - 运行时间%s", deviation > 0 ? "过长" : "过短");
        }

        Map<String, Object> detailedMetrics = new HashMap<>();
        detailedMetrics.put("averageRuntime", avgRuntime);
        detailedMetrics.put("maxRuntime", maxRuntime);
        detailedMetrics.put("minRuntime", Collections.min(runtimes));
        detailedMetrics.put("standardDeviation", stdDev);
        detailedMetrics.put("totalCycles", runtimes.size());

        return PumpAnalysisResponseDTO.AnalysisResult.builder()
                .analysisType("运行时间异常分析")
                .severityLevel(severityLevel)
                .confidence(Math.min(0.95, 0.7 + runtimes.size() * 0.005))
                .description(description)
                .detectedValue(avgRuntime)
                .expectedValue(threshold)
                .deviationPercentage(deviation)
                .trendDirection(trend.getDirection().name())
                .detailedMetrics(detailedMetrics)
                .recommendations(generateRuntimeRecommendations(deviation, trend, severityLevel))
                .build();
    }

    /**
     * 能耗趋势分析
     */
    public PumpAnalysisResponseDTO.AnalysisResult analyzeEnergyTrend(
            List<PumpDataEntity> pumpData, PumpAnalysisRequestDTO.ThresholdConfig thresholdConfig) {

        double threshold = thresholdConfig != null && thresholdConfig.getEnergyIncreaseThreshold() != null ?
                thresholdConfig.getEnergyIncreaseThreshold() : DEFAULT_ENERGY_INCREASE_THRESHOLD;

        List<Double> energyData = pumpData.stream()
                .filter(data -> data.getEnergyConsumptionKwh() != null && data.getEnergyConsumptionKwh() > 0)
                .map(PumpDataEntity::getEnergyConsumptionKwh)
                .collect(Collectors.toList());

        if (energyData.size() < 3) {
            return createAnalysisResult("能耗趋势分析", 1, 0.4,
                    "能耗数据不足，无法进行趋势分析", null, null, null, "STABLE");
        }

        // 计算移动平均以平滑噪声
        List<Double> smoothedData = TimeSeriesAnalyzer.movingAverage(energyData, Math.min(5, energyData.size() / 3));

        // 趋势分析
        TimeSeriesAnalyzer.TrendAnalysisResult trend = TimeSeriesAnalyzer.analyzeTrend(smoothedData);

        // 计算能耗增长率
        double energyGrowthRate = trend.getSlope() / TimeSeriesAnalyzer.mean(smoothedData) * 100;
        int severityLevel = calculateSeverityLevel(Math.abs(energyGrowthRate));

        String description = String.format("能耗趋势: %s, 增长率: %.2f%%, 趋势强度: %.2f",
                trend.getDirection().getDescription(), energyGrowthRate, trend.getStrength());

        if (Math.abs(energyGrowthRate) > threshold) {
            description += String.format(" - 能耗异常%s", energyGrowthRate > 0 ? "增长" : "下降");
        }

        Map<String, Object> detailedMetrics = new HashMap<>();
        detailedMetrics.put("totalEnergyConsumption", energyData.stream().mapToDouble(Double::doubleValue).sum());
        detailedMetrics.put("averageEnergyConsumption", TimeSeriesAnalyzer.mean(energyData));
        detailedMetrics.put("energyGrowthRate", energyGrowthRate);
        detailedMetrics.put("trendStrength", trend.getStrength());

        return PumpAnalysisResponseDTO.AnalysisResult.builder()
                .analysisType("能耗趋势分析")
                .severityLevel(severityLevel)
                .confidence(Math.min(0.9, 0.6 + smoothedData.size() * 0.01))
                .description(description)
                .detectedValue(energyGrowthRate)
                .expectedValue(threshold)
                .deviationPercentage(energyGrowthRate)
                .trendDirection(trend.getDirection().name())
                .detailedMetrics(detailedMetrics)
                .recommendations(generateEnergyTrendRecommendations(energyGrowthRate, trend, severityLevel))
                .build();
    }

    /**
     * 振动异常检测
     */
    public PumpAnalysisResponseDTO.AnalysisResult detectVibrationAnomaly(
            List<PumpDataEntity> pumpData, PumpAnalysisRequestDTO.ThresholdConfig thresholdConfig) {

        double threshold = thresholdConfig != null && thresholdConfig.getVibrationThreshold() != null ?
                thresholdConfig.getVibrationThreshold() : DEFAULT_VIBRATION_THRESHOLD;

        List<Double> vibrationData = pumpData.stream()
                .filter(data -> data.getVibrationMmS() != null && data.getVibrationMmS() > 0)
                .map(PumpDataEntity::getVibrationMmS)
                .collect(Collectors.toList());

        if (vibrationData.isEmpty()) {
            return createAnalysisResult("振动异常检测", 1, 0.3,
                    "无振动数据", null, threshold, null, "STABLE");
        }

        double avgVibration = TimeSeriesAnalyzer.mean(vibrationData);
        double maxVibration = Collections.max(vibrationData);
        double stdDev = TimeSeriesAnalyzer.standardDeviation(vibrationData);

        // 检测异常值
        List<Integer> outlierIndices = TimeSeriesAnalyzer.detectOutliers(vibrationData);

        int severityLevel = 1;
        if (maxVibration > threshold * 1.5) {
            severityLevel = 4; // 严重
        } else if (maxVibration > threshold) {
            severityLevel = 3; // 错误
        } else if (avgVibration > threshold * 0.8) {
            severityLevel = 2; // 警告
        }

        double deviation = ((avgVibration - threshold) / threshold) * 100;

        String description = String.format("平均振动: %.2fmm/s, 最大振动: %.2fmm/s, 阈值: %.2fmm/s, 异常点数: %d",
                avgVibration, maxVibration, threshold, outlierIndices.size());

        if (severityLevel > 1) {
            description += String.format(" - 振动异常");
        }

        Map<String, Object> detailedMetrics = new HashMap<>();
        detailedMetrics.put("averageVibration", avgVibration);
        detailedMetrics.put("maxVibration", maxVibration);
        detailedMetrics.put("standardDeviation", stdDev);
        detailedMetrics.put("outlierCount", outlierIndices.size());
        detailedMetrics.put("outlierPercentage", (double) outlierIndices.size() / vibrationData.size() * 100);

        return PumpAnalysisResponseDTO.AnalysisResult.builder()
                .analysisType("振动异常检测")
                .severityLevel(severityLevel)
                .confidence(Math.min(0.95, 0.7 + vibrationData.size() * 0.005))
                .description(description)
                .detectedValue(avgVibration)
                .expectedValue(threshold)
                .deviationPercentage(deviation)
                .trendDirection("STABLE")
                .detailedMetrics(detailedMetrics)
                .recommendations(generateVibrationRecommendations(avgVibration, maxVibration, outlierIndices.size(), severityLevel))
                .build();
    }

    /**
     * 功率异常检测
     */
    public PumpAnalysisResponseDTO.AnalysisResult detectPowerAnomaly(
            List<PumpDataEntity> pumpData, PumpAnalysisRequestDTO.ThresholdConfig thresholdConfig) {

        double threshold = thresholdConfig != null && thresholdConfig.getPowerAnomalyThreshold() != null ?
                thresholdConfig.getPowerAnomalyThreshold() : DEFAULT_POWER_ANOMALY_THRESHOLD;

        List<Double> powerData = pumpData.stream()
                .filter(data -> data.getPowerKw() != null && data.getPowerKw() > 0)
                .map(PumpDataEntity::getPowerKw)
                .collect(Collectors.toList());

        if (powerData.size() < 3) {
            return createAnalysisResult("功率异常检测", 1, 0.4,
                    "功率数据不足", null, null, null, "STABLE");
        }

        double avgPower = TimeSeriesAnalyzer.mean(powerData);
        double expectedPower = calculateExpectedPower(pumpData);
        double deviation = Math.abs((avgPower - expectedPower) / expectedPower) * 100;

        int severityLevel = calculateSeverityLevel(deviation);

        String description = String.format("平均功率: %.2fkW, 预期功率: %.2fkW, 偏差: %.1f%%",
                avgPower, expectedPower, deviation);

        if (severityLevel > 1) {
            description += String.format(" - 功率异常", avgPower > expectedPower ? "偏高" : "偏低");
        }

        Map<String, Object> detailedMetrics = new HashMap<>();
        detailedMetrics.put("averagePower", avgPower);
        detailedMetrics.put("expectedPower", expectedPower);
        detailedMetrics.put("maxPower", Collections.max(powerData));
        detailedMetrics.put("minPower", Collections.min(powerData));
        detailedMetrics.put("powerVariability", TimeSeriesAnalyzer.standardDeviation(powerData));

        return PumpAnalysisResponseDTO.AnalysisResult.builder()
                .analysisType("功率异常检测")
                .severityLevel(severityLevel)
                .confidence(Math.min(0.9, 0.6 + powerData.size() * 0.01))
                .description(description)
                .detectedValue(avgPower)
                .expectedValue(expectedPower)
                .deviationPercentage(deviation)
                .trendDirection(avgPower > expectedPower ? "INCREASING" : "DECREASING")
                .detailedMetrics(detailedMetrics)
                .recommendations(generatePowerRecommendations(deviation, severityLevel))
                .build();
    }

    /**
     * 计算预期功率（基于历史数据和运行条件）
     */
    private double calculateExpectedPower(List<PumpDataEntity> pumpData) {
        // 使用中位数作为基准功率，避免异常值影响
        List<Double> powerData = pumpData.stream()
                .filter(data -> data.getPowerKw() != null && data.getPowerKw() > 0)
                .map(PumpDataEntity::getPowerKw)
                .collect(Collectors.toList());

        return powerData.isEmpty() ? 0 : TimeSeriesAnalyzer.median(powerData);
    }

    /**
     * 计算严重级别
     */
    private int calculateSeverityLevel(double deviation) {
        if (Math.abs(deviation) < 5) {
            return 1; // 信息
        } else if (Math.abs(deviation) < 15) {
            return 2; // 警告
        } else if (Math.abs(deviation) < 30) {
            return 3; // 错误
        } else {
            return 4; // 严重
        }
    }

    /**
     * 创建基础分析结果
     */
    private PumpAnalysisResponseDTO.AnalysisResult createAnalysisResult(
            String analysisType, int severityLevel, double confidence, String description,
            Double detectedValue, Double expectedValue, Double deviationPercentage, String trendDirection) {

        return PumpAnalysisResponseDTO.AnalysisResult.builder()
                .analysisType(analysisType)
                .severityLevel(severityLevel)
                .confidence(confidence)
                .description(description)
                .detectedValue(detectedValue)
                .expectedValue(expectedValue)
                .deviationPercentage(deviationPercentage)
                .trendDirection(trendDirection)
                .detailedMetrics(new HashMap<>())
                .recommendations(new ArrayList<>())
                .build();
    }

    /**
     * 生成启泵频率建议
     */
    private List<String> generateStartupFrequencyRecommendations(double deviation, int severityLevel) {
        List<String> recommendations = new ArrayList<>();

        if (deviation > 0) { // 频繁启泵
            recommendations.add("检查水泵控制系统设置，优化启停逻辑");
            recommendations.add("检查是否存在管路泄漏或压力异常");
            recommendations.add("考虑安装蓄能器减少启泵频率");
        } else { // 启泵不足
            recommendations.add("检查水泵是否正常运行");
            recommendations.add("验证需求侧用水量是否正常");
            recommendations.add("检查控制系统信号传输");
        }

        if (severityLevel >= 3) {
            recommendations.add("立即安排专业技术人员检查");
        }

        return recommendations;
    }

    /**
     * 生成运行时间建议
     */
    private List<String> generateRuntimeRecommendations(double deviation, TimeSeriesAnalyzer.TrendAnalysisResult trend, int severityLevel) {
        List<String> recommendations = new ArrayList<>();

        if (deviation > 0) { // 运行时间过长
            recommendations.add("检查水泵负载是否过重");
            recommendations.add("验证管路阻力是否正常");
            recommendations.add("检查电机温度和散热情况");
        } else { // 运行时间过短
            recommendations.add("检查水泵是否达到正常工作压力");
            recommendations.add("验证启停条件设置是否合理");
        }

        if (trend.getDirection() == TimeSeriesAnalyzer.TrendDirection.INCREASING && trend.getStrength() > 0.7) {
            recommendations.add("运行时间持续增长，需要关注设备健康状态");
        }

        return recommendations;
    }

    /**
     * 生成能耗趋势建议
     */
    private List<String> generateEnergyTrendRecommendations(double growthRate, TimeSeriesAnalyzer.TrendAnalysisResult trend, int severityLevel) {
        List<String> recommendations = new ArrayList<>();

        if (growthRate > 0) { // 能耗增长
            recommendations.add("优化水泵运行参数，提高能效");
            recommendations.add("检查叶轮和泵壳是否有磨损");
            recommendations.add("考虑更换高效节能水泵");
        } else { // 能耗下降（可能异常）
            recommendations.add("验证功率计量设备是否正常");
            recommendations.add("检查水泵实际输出是否正常");
        }

        if (Math.abs(growthRate) > 20) {
            recommendations.add("能耗变化异常，建议立即检查");
        }

        return recommendations;
    }

    /**
     * 生成振动建议
     */
    private List<String> generateVibrationRecommendations(double avgVibration, double maxVibration, int outlierCount, int severityLevel) {
        List<String> recommendations = new ArrayList<>();

        if (maxVibration > 7.0) {
            recommendations.add("振动值严重超标，立即停机检查");
            recommendations.add("检查轴承、叶轮和电机对中");
        } else if (avgVibration > 4.5) {
            recommendations.add("振动值偏高，安排检修");
            recommendations.add("检查基础螺栓是否松动");
        }

        if (outlierCount > 0) {
            recommendations.add("存在间歇性振动异常，检查运行工况变化");
        }

        if (severityLevel >= 3) {
            recommendations.add("建议进行振动频谱分析");
        }

        return recommendations;
    }

    /**
     * 生成功率建议
     */
    private List<String> generatePowerRecommendations(double deviation, int severityLevel) {
        List<String> recommendations = new ArrayList<>();

        if (deviation > 15) { // 功率异常
            recommendations.add("检查电机和水泵机械部分");
            recommendations.add("验证电源电压是否稳定");
            recommendations.add("检查负载是否异常");
        }

        if (severityLevel >= 3) {
            recommendations.add("功率异常严重，建议全面检查");
        }

        return recommendations;
    }
}