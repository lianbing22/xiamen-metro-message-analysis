package com.xiamen.metro.message.service.pump;

import com.xiamen.metro.message.entity.PumpDataEntity;
import com.xiamen.metro.message.dto.pump.PumpAnalysisResponseDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 水泵性能评估服务
 *
 * @author Xiamen Metro System
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PumpPerformanceEvaluationService {

    /**
     * 评估水泵性能
     */
    public PumpAnalysisResponseDTO.PerformanceMetrics evaluatePerformance(
            List<PumpDataEntity> pumpData, LocalDateTime startTime, LocalDateTime endTime) {

        if (pumpData.isEmpty()) {
            return createEmptyPerformanceMetrics();
        }

        // 计算基础指标
        double startupFrequency = calculateStartupFrequency(pumpData, startTime, endTime);
        double totalRuntimeHours = calculateTotalRuntime(pumpData);
        double averagePower = calculateAveragePower(pumpData);
        double totalEnergyConsumption = calculateTotalEnergyConsumption(pumpData);

        // 计算振动指标
        PerformanceVibrationMetrics vibrationMetrics = calculateVibrationMetrics(pumpData);

        // 计算水压和流量指标
        double averagePressure = calculateAveragePressure(pumpData);
        double averageFlowRate = calculateAverageFlowRate(pumpData);

        // 计算性能评分
        double efficiencyScore = calculateEfficiencyScore(pumpData);
        double reliabilityScore = calculateReliabilityScore(pumpData);
        double maintenanceScore = calculateMaintenanceScore(pumpData);

        return PumpAnalysisResponseDTO.PerformanceMetrics.builder()
                .startupFrequency(startupFrequency)
                .totalRuntimeHours(totalRuntimeHours)
                .averagePower(averagePower)
                .totalEnergyConsumption(totalEnergyConsumption)
                .averageVibration(vibrationMetrics.getAverageVibration())
                .maxVibration(vibrationMetrics.getMaxVibration())
                .averagePressure(averagePressure)
                .averageFlowRate(averageFlowRate)
                .efficiencyScore(efficiencyScore)
                .reliabilityScore(reliabilityScore)
                .maintenanceScore(maintenanceScore)
                .build();
    }

    /**
     * 计算启泵频率
     */
    private double calculateStartupFrequency(List<PumpDataEntity> pumpData, LocalDateTime startTime, LocalDateTime endTime) {
        List<LocalDateTime> startEvents = pumpData.stream()
                .filter(data -> data.getPumpStatus() != null && data.getPumpStatus() == 1)
                .map(PumpDataEntity::getTimestamp)
                .sorted()
                .collect(Collectors.toList());

        if (startEvents.isEmpty()) {
            return 0.0;
        }

        // 过滤在时间范围内的事件
        List<LocalDateTime> filteredEvents = startEvents.stream()
                .filter(timestamp -> !timestamp.isBefore(startTime) && !timestamp.isAfter(endTime))
                .collect(Collectors.toList());

        if (filteredEvents.isEmpty()) {
            return 0.0;
        }

        double hours = ChronoUnit.MINUTES.between(startTime, endTime) / 60.0;
        return filteredEvents.size() / hours;
    }

    /**
     * 计算总运行时间
     */
    private double calculateTotalRuntime(List<PumpDataEntity> pumpData) {
        return pumpData.stream()
                .filter(data -> data.getRuntimeMinutes() != null && data.getRuntimeMinutes() > 0)
                .mapToDouble(PumpDataEntity::getRuntimeMinutes)
                .sum() / 60.0; // 转换为小时
    }

    /**
     * 计算平均功率
     */
    private double calculateAveragePower(List<PumpDataEntity> pumpData) {
        List<Double> powers = pumpData.stream()
                .filter(data -> data.getPowerKw() != null && data.getPowerKw() > 0)
                .map(PumpDataEntity::getPowerKw)
                .collect(Collectors.toList());

        return powers.isEmpty() ? 0.0 : TimeSeriesAnalyzer.mean(powers);
    }

    /**
     * 计算总能耗
     */
    private double calculateTotalEnergyConsumption(List<PumpDataEntity> pumpData) {
        return pumpData.stream()
                .filter(data -> data.getEnergyConsumptionKwh() != null && data.getEnergyConsumptionKwh() > 0)
                .mapToDouble(PumpDataEntity::getEnergyConsumptionKwh)
                .sum();
    }

    /**
     * 计算振动指标
     */
    private PerformanceVibrationMetrics calculateVibrationMetrics(List<PumpDataEntity> pumpData) {
        List<Double> vibrations = pumpData.stream()
                .filter(data -> data.getVibrationMmS() != null && data.getVibrationMmS() > 0)
                .map(PumpDataEntity::getVibrationMmS)
                .collect(Collectors.toList());

        if (vibrations.isEmpty()) {
            return new PerformanceVibrationMetrics(0.0, 0.0);
        }

        double averageVibration = TimeSeriesAnalyzer.mean(vibrations);
        double maxVibration = Collections.max(vibrations);

        return new PerformanceVibrationMetrics(averageVibration, maxVibration);
    }

    /**
     * 计算平均水压
     */
    private double calculateAveragePressure(List<PumpDataEntity> pumpData) {
        List<Double> pressures = pumpData.stream()
                .filter(data -> data.getWaterPressureKpa() != null && data.getWaterPressureKpa() > 0)
                .map(PumpDataEntity::getWaterPressureKpa)
                .collect(Collectors.toList());

        return pressures.isEmpty() ? 0.0 : TimeSeriesAnalyzer.mean(pressures);
    }

    /**
     * 计算平均流量
     */
    private double calculateAverageFlowRate(List<PumpDataEntity> pumpData) {
        List<Double> flowRates = pumpData.stream()
                .filter(data -> data.getFlowRateM3h() != null && data.getFlowRateM3h() > 0)
                .map(PumpDataEntity::getFlowRateM3h)
                .collect(Collectors.toList());

        return flowRates.isEmpty() ? 0.0 : TimeSeriesAnalyzer.mean(flowRates);
    }

    /**
     * 计算效率评分 (0-100)
     */
    private double calculateEfficiencyScore(List<PumpDataEntity> pumpData) {
        double score = 100.0; // 满分100分

        // 功率效率评估
        List<Double> powers = pumpData.stream()
                .filter(data -> data.getPowerKw() != null)
                .map(PumpDataEntity::getPowerKw)
                .collect(Collectors.toList());

        if (!powers.isEmpty()) {
            double powerVariability = TimeSeriesAnalyzer.standardDeviation(powers) / TimeSeriesAnalyzer.mean(powers);
            if (powerVariability > 0.2) {
                score -= 15; // 功率波动大，效率低
            } else if (powerVariability > 0.1) {
                score -= 8;
            }
        }

        // 压力与流量匹配度评估
        List<Double> pressures = pumpData.stream()
                .filter(data -> data.getWaterPressureKpa() != null)
                .map(PumpDataEntity::getWaterPressureKpa)
                .collect(Collectors.toList());

        List<Double> flowRates = pumpData.stream()
                .filter(data -> data.getFlowRateM3h() != null)
                .map(PumpDataEntity::getFlowRateM3h)
                .collect(Collectors.toList());

        if (!pressures.isEmpty() && !flowRates.isEmpty() && pressures.size() == flowRates.size()) {
            // 计算压力-流量相关性
            double correlation = calculateCorrelation(pressures, flowRates);
            if (correlation < 0.7) {
                score -= 10; // 压力流量不匹配
            }
        }

        // 能耗趋势评估
        List<Double> energies = pumpData.stream()
                .filter(data -> data.getEnergyConsumptionKwh() != null)
                .map(PumpDataEntity::getEnergyConsumptionKwh)
                .collect(Collectors.toList());

        if (energies.size() >= 10) {
            TimeSeriesAnalyzer.TrendAnalysisResult energyTrend = TimeSeriesAnalyzer.analyzeTrend(energies);
            if (energyTrend.getDirection() == TimeSeriesAnalyzer.TrendDirection.INCREASING &&
                energyTrend.getStrength() > 0.6) {
                score -= 20; // 能耗持续增长
            }
        }

        return Math.max(0, Math.min(100, score));
    }

    /**
     * 计算可靠性评分 (0-100)
     */
    private double calculateReliabilityScore(List<PumpDataEntity> pumpData) {
        double score = 100.0;

        // 故障频率评估
        long faultCount = pumpData.stream()
                .filter(data -> data.getFaultCode() != null && !data.getFaultCode().trim().isEmpty())
                .count();

        if (faultCount > 0) {
            double faultRate = (double) faultCount / pumpData.size();
            score -= faultRate * 50; // 故障率影响
        }

        // 报警频率评估
        long alarmCount = pumpData.stream()
                .filter(data -> data.getAlarmLevel() != null && data.getAlarmLevel() > 1)
                .count();

        if (alarmCount > 0) {
            double alarmRate = (double) alarmCount / pumpData.size();
            score -= alarmRate * 30; // 报警率影响
        }

        // 运行稳定性评估
        List<Double> vibrations = pumpData.stream()
                .filter(data -> data.getVibrationMmS() != null)
                .map(PumpDataEntity::getVibrationMmS)
                .collect(Collectors.toList());

        if (!vibrations.isEmpty()) {
            double avgVibration = TimeSeriesAnalyzer.mean(vibrations);
            if (avgVibration > 4.5) {
                score -= 25; // 振动过大
            } else if (avgVibration > 3.0) {
                score -= 10;
            }

            // 振动稳定性
            double vibrationStdDev = TimeSeriesAnalyzer.standardDeviation(vibrations);
            if (vibrationStdDev > 1.0) {
                score -= 15; // 振动不稳定
            }
        }

        return Math.max(0, Math.min(100, score));
    }

    /**
     * 计算维护评分 (0-100)
     */
    private double calculateMaintenanceScore(List<PumpDataEntity> pumpData) {
        double score = 100.0;

        // 维护需求评估
        boolean hasMaintenanceFlag = pumpData.stream()
                .anyMatch(data -> Boolean.TRUE.equals(data.getMaintenanceFlag()));

        if (hasMaintenanceFlag) {
            score -= 30; // 有维护标志
        }

        // 运行时间评估
        double totalRuntime = calculateTotalRuntime(pumpData);
        if (totalRuntime > 8760) { // 超过一年
            score -= 20; // 长期运行需要维护
        } else if (totalRuntime > 4380) { // 超过半年
            score -= 10;
        }

        // 性能退化评估
        List<Double> powers = pumpData.stream()
                .filter(data -> data.getPowerKw() != null)
                .map(PumpDataEntity::getPowerKw)
                .collect(Collectors.toList());

        if (powers.size() >= 20) {
            // 比较前期和后期的功率
            int midPoint = powers.size() / 2;
            List<Double> earlyPowers = powers.subList(0, midPoint);
            List<Double> latePowers = powers.subList(midPoint, powers.size());

            double earlyAvg = TimeSeriesAnalyzer.mean(earlyPowers);
            double lateAvg = TimeSeriesAnalyzer.mean(latePowers);

            double powerIncrease = (lateAvg - earlyAvg) / earlyAvg * 100;
            if (powerIncrease > 15) {
                score -= 25; // 功率显著增加，可能需要维护
            } else if (powerIncrease > 8) {
                score -= 12;
            }
        }

        return Math.max(0, Math.min(100, score));
    }

    /**
     * 计算两个序列的相关系数
     */
    private double calculateCorrelation(List<Double> x, List<Double> y) {
        if (x.size() != y.size() || x.isEmpty()) {
            return 0.0;
        }

        double meanX = TimeSeriesAnalyzer.mean(x);
        double meanY = TimeSeriesAnalyzer.mean(y);

        double numerator = 0.0;
        double sumXSquared = 0.0;
        double sumYSquared = 0.0;

        for (int i = 0; i < x.size(); i++) {
            double diffX = x.get(i) - meanX;
            double diffY = y.get(i) - meanY;
            numerator += diffX * diffY;
            sumXSquared += diffX * diffX;
            sumYSquared += diffY * diffY;
        }

        double denominator = Math.sqrt(sumXSquared * sumYSquared);
        return denominator == 0 ? 0.0 : numerator / denominator;
    }

    /**
     * 创建空的性能指标
     */
    private PumpAnalysisResponseDTO.PerformanceMetrics createEmptyPerformanceMetrics() {
        return PumpAnalysisResponseDTO.PerformanceMetrics.builder()
                .startupFrequency(0.0)
                .totalRuntimeHours(0.0)
                .averagePower(0.0)
                .totalEnergyConsumption(0.0)
                .averageVibration(0.0)
                .maxVibration(0.0)
                .averagePressure(0.0)
                .averageFlowRate(0.0)
                .efficiencyScore(0.0)
                .reliabilityScore(0.0)
                .maintenanceScore(0.0)
                .build();
    }

    /**
     * 振动指标内部类
     */
    private static class PerformanceVibrationMetrics {
        private final double averageVibration;
        private final double maxVibration;

        public PerformanceVibrationMetrics(double averageVibration, double maxVibration) {
            this.averageVibration = averageVibration;
            this.maxVibration = maxVibration;
        }

        public double getAverageVibration() { return averageVibration; }
        public double getMaxVibration() { return maxVibration; }
    }

    /**
     * 生成性能评估报告
     */
    public String generatePerformanceReport(PumpAnalysisResponseDTO.PerformanceMetrics metrics) {
        StringBuilder report = new StringBuilder();
        report.append("=== 水泵性能评估报告 ===\n\n");

        // 基础运行指标
        report.append("【基础运行指标】\n");
        report.append(String.format("启泵频率: %.2f 次/小时\n", metrics.getStartupFrequency()));
        report.append(String.format("总运行时间: %.1f 小时\n", metrics.getTotalRuntimeHours()));
        report.append(String.format("平均功率: %.2f kW\n", metrics.getAveragePower()));
        report.append(String.format("总能耗: %.2f kWh\n", metrics.getTotalEnergyConsumption()));
        report.append("\n");

        // 振动指标
        report.append("【振动指标】\n");
        report.append(String.format("平均振动: %.2f mm/s\n", metrics.getAverageVibration()));
        report.append(String.format("最大振动: %.2f mm/s\n", metrics.getMaxVibration()));
        report.append("\n");

        // 水力性能
        report.append("【水力性能】\n");
        report.append(String.format("平均水压: %.1f kPa\n", metrics.getAveragePressure()));
        report.append(String.format("平均流量: %.1f m³/h\n", metrics.getAverageFlowRate()));
        report.append("\n");

        // 性能评分
        report.append("【性能评分】\n");
        report.append(String.format("效率评分: %.1f/100 %s\n", metrics.getEfficiencyScore(),
                getPerformanceGrade(metrics.getEfficiencyScore())));
        report.append(String.format("可靠性评分: %.1f/100 %s\n", metrics.getReliabilityScore(),
                getPerformanceGrade(metrics.getReliabilityScore())));
        report.append(String.format("维护评分: %.1f/100 %s\n", metrics.getMaintenanceScore(),
                getPerformanceGrade(metrics.getMaintenanceScore())));

        // 综合评估
        double overallScore = (metrics.getEfficiencyScore() + metrics.getReliabilityScore() + metrics.getMaintenanceScore()) / 3;
        report.append(String.format("\n【综合评分】: %.1f/100 %s\n", overallScore, getPerformanceGrade(overallScore)));

        return report.toString();
    }

    /**
     * 获取性能等级
     */
    private String getPerformanceGrade(double score) {
        if (score >= 90) {
            return "(优秀)";
        } else if (score >= 80) {
            return "(良好)";
        } else if (score >= 70) {
            return "(中等)";
        } else if (score >= 60) {
            return "(及格)";
        } else {
            return "(需要改进)";
        }
    }
}