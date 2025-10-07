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
 * 水泵故障预测服务
 * 基于历史数据和机器学习算法进行故障预测
 *
 * @author Xiamen Metro System
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PumpFaultPredictionService {

    private static final int MIN_TRAINING_SAMPLES = 30;
    private static final double CONFIDENCE_THRESHOLD = 0.6;
    private static final int DEFAULT_PREDICTION_DAYS = 7;

    /**
     * 故障预测主方法
     */
    public PumpAnalysisResponseDTO.PredictionInfo predictFaults(
            List<PumpDataEntity> historicalData,
            PumpAnalysisRequestDTO.ModelConfig modelConfig) {

        if (historicalData.size() < MIN_TRAINING_SAMPLES) {
            return createLowConfidencePrediction("历史数据不足，无法进行可靠预测");
        }

        try {
            // 预测组件
            Map<String, Double> failureProbabilities = predictComponentFailures(historicalData, modelConfig);

            // 预测剩余寿命
            int remainingUsefulLife = predictRemainingUsefulLife(historicalData, modelConfig);

            // 预测性能退化趋势
            String performanceTrend = predictPerformanceDegradation(historicalData);

            // 预测关键指标
            Map<String, Double> keyMetricsPrediction = predictKeyMetrics(historicalData, modelConfig);

            // 计算总体故障概率
            double overallFailureProbability = calculateOverallFailureProbability(failureProbabilities);

            // 预测故障时间
            LocalDateTime predictedFailureTime = predictFailureTime(overallFailureProbability, remainingUsefulLife);

            // 计算置信区间
            Map<String, Double[]> confidenceIntervals = calculateConfidenceIntervals(
                    historicalData, keyMetricsPrediction, modelConfig);

            double confidence = calculatePredictionConfidence(historicalData.size(), failureProbabilities);

            return PumpAnalysisResponseDTO.PredictionInfo.builder()
                    .failureProbability(overallFailureProbability)
                    .remainingUsefulLifeDays(remainingUsefulLife)
                    .predictedFailureTime(predictedFailureTime)
                    .performanceDegradationTrend(performanceTrend)
                    .keyMetricsPrediction(keyMetricsPrediction)
                    .confidenceIntervals(confidenceIntervals)
                    .build();

        } catch (Exception e) {
            log.error("故障预测过程中发生错误", e);
            return createLowConfidencePrediction("预测过程中发生错误: " + e.getMessage());
        }
    }

    /**
     * 预测组件故障概率
     */
    private Map<String, Double> predictComponentFailures(
            List<PumpDataEntity> data, PumpAnalysisRequestDTO.ModelConfig config) {

        Map<String, Double> componentFailures = new HashMap<>();

        // 电机故障预测
        componentFailures.put("motor", predictMotorFailure(data));

        // 轴承故障预测
        componentFailures.put("bearing", predictBearingFailure(data));

        // 叶轮故障预测
        componentFailures.put("impeller", predictImpellerFailure(data));

        // 密封件故障预测
        componentFailures.put("seal", predictSealFailure(data));

        // 控制系统故障预测
        componentFailures.put("control_system", predictControlSystemFailure(data));

        return componentFailures;
    }

    /**
     * 预测电机故障
     */
    private Double predictMotorFailure(List<PumpDataEntity> data) {
        List<Double> currents = data.stream()
                .filter(d -> d.getCurrentAmperage() != null)
                .map(PumpDataEntity::getCurrentAmperage)
                .collect(Collectors.toList());

        List<Double> powers = data.stream()
                .filter(d -> d.getPowerKw() != null)
                .map(PumpDataEntity::getPowerKw)
                .collect(Collectors.toList());

        List<Double> temperatures = data.stream()
                .filter(d -> d.getWaterTemperatureCelsius() != null)
                .map(PumpDataEntity::getWaterTemperatureCelsius)
                .collect(Collectors.toList());

        double failureProbability = 0.0;

        // 电流异常分析
        if (!currents.isEmpty()) {
            double currentStdDev = TimeSeriesAnalyzer.standardDeviation(currents);
            double currentMean = TimeSeriesAnalyzer.mean(currents);
            double currentCV = currentStdDev / currentMean; // 变异系数

            if (currentCV > 0.15) {
                failureProbability += 0.2;
            }
            if (currentMean > getRatedCurrent(data) * 1.1) {
                failureProbability += 0.3;
            }
        }

        // 功率趋势分析
        if (!powers.isEmpty() && powers.size() >= 10) {
            TimeSeriesAnalyzer.TrendAnalysisResult powerTrend = TimeSeriesAnalyzer.analyzeTrend(powers);
            if (powerTrend.getDirection() == TimeSeriesAnalyzer.TrendDirection.INCREASING &&
                powerTrend.getStrength() > 0.6) {
                failureProbability += 0.25;
            }
        }

        // 温度异常分析
        if (!temperatures.isEmpty()) {
            double maxTemp = Collections.max(temperatures);
            double avgTemp = TimeSeriesAnalyzer.mean(temperatures);

            if (maxTemp > 80) {
                failureProbability += 0.3;
            } else if (avgTemp > 60) {
                failureProbability += 0.15;
            }
        }

        return Math.min(failureProbability, 1.0);
    }

    /**
     * 预测轴承故障
     */
    private Double predictBearingFailure(List<PumpDataEntity> data) {
        List<Double> vibrations = data.stream()
                .filter(d -> d.getVibrationMmS() != null)
                .map(PumpDataEntity::getVibrationMmS)
                .collect(Collectors.toList());

        if (vibrations.isEmpty()) {
            return 0.1; // 默认低概率
        }

        double failureProbability = 0.0;

        // 振动水平分析
        double maxVibration = Collections.max(vibrations);
        double avgVibration = TimeSeriesAnalyzer.mean(vibrations);
        double vibrationStdDev = TimeSeriesAnalyzer.standardDeviation(vibrations);

        if (maxVibration > 7.0) {
            failureProbability += 0.4;
        } else if (maxVibration > 4.5) {
            failureProbability += 0.2;
        }

        if (vibrationStdDev > 1.5) {
            failureProbability += 0.2; // 振动不稳定
        }

        // 振动趋势分析
        if (vibrations.size() >= 10) {
            TimeSeriesAnalyzer.TrendAnalysisResult vibrationTrend = TimeSeriesAnalyzer.analyzeTrend(vibrations);
            if (vibrationTrend.getDirection() == TimeSeriesAnalyzer.TrendDirection.INCREASING &&
                vibrationTrend.getStrength() > 0.7) {
                failureProbability += 0.3;
            }
        }

        // 检测异常振动模式
        List<Integer> outliers = TimeSeriesAnalyzer.detectOutliers(vibrations);
        double outlierRatio = (double) outliers.size() / vibrations.size();
        if (outlierRatio > 0.2) {
            failureProbability += 0.2;
        }

        return Math.min(failureProbability, 1.0);
    }

    /**
     * 预测叶轮故障
     */
    private Double predictImpellerFailure(List<PumpDataEntity> data) {
        List<Double> pressures = data.stream()
                .filter(d -> d.getWaterPressureKpa() != null)
                .map(PumpDataEntity::getWaterPressureKpa)
                .collect(Collectors.toList());

        List<Double> flows = data.stream()
                .filter(d -> d.getFlowRateM3h() != null)
                .map(PumpDataEntity::getFlowRateM3h)
                .collect(Collectors.toList());

        double failureProbability = 0.0;

        // 压力异常分析
        if (!pressures.isEmpty() && pressures.size() >= 5) {
            double pressureMean = TimeSeriesAnalyzer.mean(pressures);
            double pressureStdDev = TimeSeriesAnalyzer.standardDeviation(pressures);

            // 压力不稳定可能表示叶轮问题
            if (pressureStdDev / pressureMean > 0.2) {
                failureProbability += 0.2;
            }

            // 压力下降趋势
            TimeSeriesAnalyzer.TrendAnalysisResult pressureTrend = TimeSeriesAnalyzer.analyzeTrend(pressures);
            if (pressureTrend.getDirection() == TimeSeriesAnalyzer.TrendDirection.DECREASING &&
                pressureTrend.getStrength() > 0.6) {
                failureProbability += 0.3;
            }
        }

        // 流量异常分析
        if (!flows.isEmpty() && flows.size() >= 5) {
            double flowMean = TimeSeriesAnalyzer.mean(flows);

            // 流量下降可能表示叶轮磨损
            TimeSeriesAnalyzer.TrendAnalysisResult flowTrend = TimeSeriesAnalyzer.analyzeTrend(flows);
            if (flowTrend.getDirection() == TimeSeriesAnalyzer.TrendDirection.DECREASING &&
                flowTrend.getStrength() > 0.5) {
                failureProbability += 0.25;
            }
        }

        // 功率效率分析
        List<Double> powers = data.stream()
                .filter(d -> d.getPowerKw() != null)
                .map(PumpDataEntity::getPowerKw)
                .collect(Collectors.toList());

        if (!powers.isEmpty() && !pressures.isEmpty()) {
            // 计算效率趋势（简化版本）
            TimeSeriesAnalyzer.TrendAnalysisResult powerTrend = TimeSeriesAnalyzer.analyzeTrend(powers);
            if (powerTrend.getDirection() == TimeSeriesAnalyzer.TrendDirection.INCREASING &&
                pressures.size() > 10) {
                TimeSeriesAnalyzer.TrendAnalysisResult pressureTrend = TimeSeriesAnalyzer.analyzeTrend(pressures);
                if (pressureTrend.getDirection() == TimeSeriesAnalyzer.TrendDirection.DECREASING) {
                    failureProbability += 0.2; // 功率上升但压力下降，效率降低
                }
            }
        }

        return Math.min(failureProbability, 1.0);
    }

    /**
     * 预测密封件故障
     */
    private Double predictSealFailure(List<PumpDataEntity> data) {
        // 密封件故障通常通过泄漏检测，但这里我们使用间接指标
        List<Double> temperatures = data.stream()
                .filter(d -> d.getWaterTemperatureCelsius() != null)
                .map(PumpDataEntity::getWaterTemperatureCelsius)
                .collect(Collectors.toList());

        List<Double> pressures = data.stream()
                .filter(d -> d.getWaterPressureKpa() != null)
                .map(PumpDataEntity::getWaterPressureKpa)
                .collect(Collectors.toList());

        double failureProbability = 0.0;

        // 温度异常可能导致密封件老化
        if (!temperatures.isEmpty()) {
            double maxTemp = Collections.max(temperatures);
            double avgTemp = TimeSeriesAnalyzer.mean(temperatures);

            if (maxTemp > 85) {
                failureProbability += 0.3;
            } else if (avgTemp > 70) {
                failureProbability += 0.15;
            }
        }

        // 压力波动对密封件的损伤
        if (!pressures.isEmpty() && pressures.size() >= 10) {
            double pressureStdDev = TimeSeriesAnalyzer.standardDeviation(pressures);
            double pressureMean = TimeSeriesAnalyzer.mean(pressures);
            double pressureCV = pressureStdDev / pressureMean;

            if (pressureCV > 0.25) {
                failureProbability += 0.2; // 压力波动大
            }
        }

        // 运行时间累积影响
        long totalRuntimeHours = data.stream()
                .filter(d -> d.getRuntimeMinutes() != null)
                .mapToLong(d -> d.getRuntimeMinutes().longValue())
                .sum() / 60;

        if (totalRuntimeHours > 8760) { // 超过一年
            failureProbability += 0.1 * (totalRuntimeHours / 8760); // 累积效应
        }

        return Math.min(failureProbability, 1.0);
    }

    /**
     * 预测控制系统故障
     */
    private Double predictControlSystemFailure(List<PumpDataEntity> data) {
        // 分析启停模式
        List<LocalDateTime> startEvents = data.stream()
                .filter(d -> d.getPumpStatus() != null && d.getPumpStatus() == 1)
                .map(PumpDataEntity::getTimestamp)
                .sorted()
                .collect(Collectors.toList());

        double failureProbability = 0.0;

        if (startEvents.size() >= 10) {
            // 计算启停间隔的变异系数
            List<Long> intervals = new ArrayList<>();
            for (int i = 1; i < startEvents.size(); i++) {
                long interval = ChronoUnit.MINUTES.between(startEvents.get(i-1), startEvents.get(i));
                intervals.add((double) interval);
            }

            if (!intervals.isEmpty()) {
                double intervalMean = TimeSeriesAnalyzer.mean(intervals);
                double intervalStdDev = TimeSeriesAnalyzer.standardDeviation(intervals);
                double intervalCV = intervalStdDev / intervalMean;

                if (intervalCV > 0.5) {
                    failureProbability += 0.2; // 启停间隔不稳定
                }
            }
        }

        // 分析故障代码
        List<String> faultCodes = data.stream()
                .filter(d -> d.getFaultCode() != null && !d.getFaultCode().trim().isEmpty())
                .map(PumpDataEntity::getFaultCode)
                .collect(Collectors.toList());

        if (!faultCodes.isEmpty()) {
            // 控制系统相关故障代码
            long controlSystemFaults = faultCodes.stream()
                    .filter(code -> code.startsWith("C") || code.startsWith("CTRL"))
                    .count();

            failureProbability += 0.3 * (controlSystemFaults / (double) faultCodes.size());
        }

        return Math.min(failureProbability, 1.0);
    }

    /**
     * 预测剩余寿命
     */
    private int predictRemainingUsefulLife(List<PumpDataEntity> data, PumpAnalysisRequestDTO.ModelConfig config) {
        Map<String, Double> componentFailures = predictComponentFailures(data, config);

        // 基于最薄弱的组件预测寿命
        double maxFailureProbability = componentFailures.values().stream()
                .mapToDouble(Double::doubleValue)
                .max()
                .orElse(0.0);

        // 基于当前健康状态估算剩余寿命
        int baseRemainingDays = config != null && config.getPredictionWindowDays() != null ?
                config.getPredictionWindowDays() : DEFAULT_PREDICTION_DAYS;

        if (maxFailureProbability > 0.8) {
            return Math.max(1, baseRemainingDays / 7); // 高风险，剩余寿命很短
        } else if (maxFailureProbability > 0.6) {
            return Math.max(3, baseRemainingDays / 3); // 中高风险
        } else if (maxFailureProbability > 0.4) {
            return baseRemainingDays; // 中等风险
        } else if (maxFailureProbability > 0.2) {
            return baseRemainingDays * 2; // 低风险
        } else {
            return baseRemainingDays * 4; // 很低风险
        }
    }

    /**
     * 预测性能退化趋势
     */
    private String predictPerformanceDegradation(List<PumpDataEntity> data) {
        List<Double> efficiencies = calculateEfficiencyTrend(data);

        if (efficiencies.size() < 5) {
            return "INSUFFICIENT_DATA";
        }

        TimeSeriesAnalyzer.TrendAnalysisResult trend = TimeSeriesAnalyzer.analyzeTrend(efficiencies);

        if (trend.getDirection() == TimeSeriesAnalyzer.TrendDirection.DECREASING) {
            if (trend.getStrength() > 0.8) {
                return "RAPID_DEGRADATION";
            } else if (trend.getStrength() > 0.5) {
                return "MODERATE_DEGRADATION";
            } else {
                return "SLOW_DEGRADATION";
            }
        } else if (trend.getDirection() == TimeSeriesAnalyzer.TrendDirection.INCREASING) {
            return "IMPROVING"; // 可能是维护后的改善
        } else {
            return "STABLE";
        }
    }

    /**
     * 预测关键指标
     */
    private Map<String, Double> predictKeyMetrics(List<PumpDataEntity> data, PumpAnalysisRequestDTO.ModelConfig config) {
        Map<String, Double> predictions = new HashMap<>();

        // 预测功率
        List<Double> powers = data.stream()
                .filter(d -> d.getPowerKw() != null)
                .map(PumpDataEntity::getPowerKw)
                .collect(Collectors.toList());

        if (!powers.isEmpty()) {
            double predictedPower = predictNextValue(powers);
            predictions.put("power_kw", predictedPower);
        }

        // 预测振动
        List<Double> vibrations = data.stream()
                .filter(d -> d.getVibrationMmS() != null)
                .map(PumpDataEntity::getVibrationMmS)
                .collect(Collectors.toList());

        if (!vibrations.isEmpty()) {
            double predictedVibration = predictNextValue(vibrations);
            predictions.put("vibration_mm_s", predictedVibration);
        }

        // 预测能耗
        List<Double> energies = data.stream()
                .filter(d -> d.getEnergyConsumptionKwh() != null)
                .map(PumpDataEntity::getEnergyConsumptionKwh)
                .collect(Collectors.toList());

        if (!energies.isEmpty()) {
            double predictedEnergy = predictNextValue(energies);
            predictions.put("energy_consumption_kwh", predictedEnergy);
        }

        return predictions;
    }

    /**
     * 预测下一个值（简单线性预测）
     */
    private double predictNextValue(List<Double> data) {
        if (data.size() < 3) {
            return TimeSeriesAnalyzer.mean(data);
        }

        // 使用最近的几个点进行线性预测
        int windowSize = Math.min(10, data.size());
        List<Double> recentData = data.subList(data.size() - windowSize, data.size());

        List<Double> xValues = new ArrayList<>();
        for (int i = 0; i < recentData.size(); i++) {
            xValues.add((double) i);
        }

        TimeSeriesAnalyzer.LinearRegressionResult regression =
                TimeSeriesAnalyzer.linearRegression(xValues, recentData);

        // 预测下一个点
        return regression.getIntercept() + regression.getSlope() * recentData.size();
    }

    /**
     * 计算效率趋势
     */
    private List<Double> calculateEfficiencyTrend(List<PumpDataEntity> data) {
        // 简化的效率计算：功率/压力的比值趋势
        return data.stream()
                .filter(d -> d.getPowerKw() != null && d.getWaterPressureKpa() != null &&
                            d.getPowerKw() > 0 && d.getWaterPressureKpa() > 0)
                .map(d -> d.getWaterPressureKpa() / d.getPowerKw()) // 简化效率指标
                .collect(Collectors.toList());
    }

    /**
     * 计算总体故障概率
     */
    private double calculateOverallFailureProbability(Map<String, Double> componentFailures) {
        return componentFailures.values().stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);
    }

    /**
     * 预测故障时间
     */
    private LocalDateTime predictFailureTime(double failureProbability, int remainingDays) {
        if (failureProbability > 0.8) {
            return LocalDateTime.now().plusDays(Math.max(1, remainingDays / 7));
        } else if (failureProbability > 0.6) {
            return LocalDateTime.now().plusDays(Math.max(3, remainingDays / 3));
        } else {
            return LocalDateTime.now().plusDays(remainingDays);
        }
    }

    /**
     * 计算置信区间
     */
    private Map<String, Double[]> calculateConfidenceIntervals(
            List<PumpDataEntity> data, Map<String, Double> predictions,
            PumpAnalysisRequestDTO.ModelConfig config) {

        Map<String, Double[]> confidenceIntervals = new HashMap<>();

        for (Map.Entry<String, Double> entry : predictions.entrySet()) {
            String metric = entry.getKey();
            Double predictedValue = entry.getValue();

            // 简化的置信区间计算（基于历史变异）
            double confidenceRange = predictedValue * 0.1; // 10%的置信区间

            confidenceIntervals.put(metric, new Double[]{
                predictedValue - confidenceRange,
                predictedValue + confidenceRange
            });
        }

        return confidenceIntervals;
    }

    /**
     * 计算预测置信度
     */
    private double calculatePredictionConfidence(int dataSize, Map<String, Double> componentFailures) {
        double dataConfidence = Math.min(1.0, dataSize / 100.0); // 数据量贡献
        double modelConfidence = 1.0 - (componentFailures.values().stream()
                .mapToDouble(Double::doubleValue)
                .max()
                .orElse(0.0)); // 模型确定性贡献

        return (dataConfidence + modelConfidence) / 2.0;
    }

    /**
     * 创建低置信度预测
     */
    private PumpAnalysisResponseDTO.PredictionInfo createLowConfidencePrediction(String reason) {
        return PumpAnalysisResponseDTO.PredictionInfo.builder()
                .failureProbability(0.1)
                .remainingUsefulLifeDays(DEFAULT_PREDICTION_DAYS)
                .predictedFailureTime(LocalDateTime.now().plusDays(DEFAULT_PREDICTION_DAYS))
                .performanceDegradationTrend("UNKNOWN")
                .keyMetricsPrediction(new HashMap<>())
                .confidenceIntervals(new HashMap<>())
                .build();
    }

    /**
     * 获取额定电流（简化版本）
     */
    private double getRatedCurrent(List<PumpDataEntity> data) {
        List<Double> currents = data.stream()
                .filter(d -> d.getCurrentAmperage() != null)
                .map(PumpDataEntity::getCurrentAmperage)
                .collect(Collectors.toList());

        return currents.isEmpty() ? 10.0 : TimeSeriesAnalyzer.median(currents) * 1.2; // 中位数的120%作为额定值
    }
}