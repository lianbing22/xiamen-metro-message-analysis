package com.xiamen.metro.message.service.pump;

import com.xiamen.metro.message.entity.PumpDataEntity;
import com.xiamen.metro.message.dto.pump.PumpAnalysisRequestDTO;
import com.xiamen.metro.message.dto.pump.PumpAnalysisResponseDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 水泵智能维护建议生成服务
 *
 * @author Xiamen Metro System
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PumpMaintenanceRecommendationService {

    /**
     * 生成维护建议
     */
    public PumpAnalysisResponseDTO.MaintenanceRecommendations generateMaintenanceRecommendations(
            List<PumpDataEntity> pumpData,
            List<PumpAnalysisResponseDTO.AnalysisResult> analysisResults,
            PumpAnalysisResponseDTO.PredictionInfo predictionInfo,
            PumpAnalysisResponseDTO.PerformanceMetrics performanceMetrics) {

        List<String> urgentActions = new ArrayList<>();
        List<String> scheduledActions = new ArrayList<>();
        List<String> preventiveActions = new ArrayList<>();
        List<String> monitoringRecommendations = new ArrayList<>();

        // 基于分析结果生成建议
        generateRecommendationsFromAnalysisResults(analysisResults, urgentActions, scheduledActions, preventiveActions);

        // 基于预测信息生成建议
        generateRecommendationsFromPrediction(predictionInfo, urgentActions, scheduledActions);

        // 基于性能指标生成建议
        generateRecommendationsFromPerformance(performanceMetrics, preventiveActions, monitoringRecommendations);

        // 基于历史数据生成建议
        generateRecommendationsFromHistory(pumpData, preventiveActions, scheduledActions);

        // 估算维护成本
        Double estimatedCost = estimateMaintenanceCost(urgentActions, scheduledActions, preventiveActions);

        // 推荐维护时间
        LocalDateTime recommendedMaintenanceTime = calculateRecommendedMaintenanceTime(
                urgentActions, scheduledActions, predictionInfo);

        return PumpAnalysisResponseDTO.MaintenanceRecommendations.builder()
                .urgentActions(urgentActions)
                .scheduledActions(scheduledActions)
                .preventiveActions(preventiveActions)
                .monitoringRecommendations(monitoringRecommendations)
                .estimatedCost(estimatedCost)
                .recommendedMaintenanceTime(recommendedMaintenanceTime)
                .build();
    }

    /**
     * 基于分析结果生成建议
     */
    private void generateRecommendationsFromAnalysisResults(
            List<PumpAnalysisResponseDTO.AnalysisResult> analysisResults,
            List<String> urgentActions,
            List<String> scheduledActions,
            List<String> preventiveActions) {

        for (PumpAnalysisResponseDTO.AnalysisResult result : analysisResults) {
            int severityLevel = result.getSeverityLevel();
            String analysisType = result.getAnalysisType();

            if (severityLevel >= 4) {
                // 严重问题 - 紧急处理
                urgentActions.addAll(generateUrgentActions(analysisType, result));
            } else if (severityLevel >= 3) {
                // 错误级别 - 计划处理
                scheduledActions.addAll(generateScheduledActions(analysisType, result));
            } else if (severityLevel >= 2) {
                // 警告级别 - 预防性处理
                preventiveActions.addAll(generatePreventiveActions(analysisType, result));
            }

            // 添加特定建议
            if (result.getRecommendations() != null) {
                if (severityLevel >= 3) {
                    scheduledActions.addAll(result.getRecommendations());
                } else {
                    preventiveActions.addAll(result.getRecommendations());
                }
            }
        }
    }

    /**
     * 基于预测信息生成建议
     */
    private void generateRecommendationsFromPrediction(
            PumpAnalysisResponseDTO.PredictionInfo predictionInfo,
            List<String> urgentActions,
            List<String> scheduledActions) {

        if (predictionInfo.getFailureProbability() > 0.8) {
            urgentActions.add("故障概率极高，立即停机检修");
            urgentActions.add("联系专业技术人员进行全面检查");
        } else if (predictionInfo.getFailureProbability() > 0.6) {
            scheduledActions.add("故障概率较高，安排详细检查");
            scheduledActions.add("准备必要的备件");
        }

        if (predictionInfo.getRemainingUsefulLifeDays() < 7) {
            urgentActions.add("剩余寿命不足7天，立即更换或大修");
        } else if (predictionInfo.getRemainingUsefulLifeDays() < 30) {
            scheduledActions.add("剩余寿命不足30天，制定更换计划");
        }

        // 性能退化趋势建议
        String degradationTrend = predictionInfo.getPerformanceDegradationTrend();
        if ("RAPID_DEGRADATION".equals(degradationTrend)) {
            scheduledActions.add("性能快速退化，进行深度检查");
            scheduledActions.add("分析退化原因，制定针对性维护方案");
        } else if ("MODERATE_DEGRADATION".equals(degradationTrend)) {
            preventiveActions.add("性能中度退化，增加监控频率");
            preventiveActions.add("准备预防性维护计划");
        }
    }

    /**
     * 基于性能指标生成建议
     */
    private void generateRecommendationsFromPerformance(
            PumpAnalysisResponseDTO.PerformanceMetrics performanceMetrics,
            List<String> preventiveActions,
            List<String> monitoringRecommendations) {

        // 效率评分建议
        if (performanceMetrics.getEfficiencyScore() < 60) {
            preventiveActions.add("效率评分偏低，进行节能优化");
            preventiveActions.add("检查叶轮、泵壳磨损情况");
            preventiveActions.add("优化运行参数");
        } else if (performanceMetrics.getEfficiencyScore() < 80) {
            preventiveActions.add("效率评分一般，关注能耗变化");
        }

        // 可靠性评分建议
        if (performanceMetrics.getReliabilityScore() < 60) {
            scheduledActions.add("可靠性评分偏低，安排全面检查");
            scheduledActions.add("检查关键部件状态");
        } else if (performanceMetrics.getReliabilityScore() < 80) {
            preventiveActions.add("可靠性评分一般，加强监控");
        }

        // 维护评分建议
        if (performanceMetrics.getMaintenanceScore() < 50) {
            scheduledActions.add("维护需求较高，安排预防性维护");
        }

        // 振动监控建议
        if (performanceMetrics.getMaxVibration() > 4.5) {
            urgentActions.add("振动值超标，立即检查轴承和对中");
        } else if (performanceMetrics.getAverageVibration() > 3.0) {
            monitoringRecommendations.add("振动值偏高，建议安装振动监测仪");
            monitoringRecommendations.add("定期进行振动分析");
        }

        // 功率监控建议
        if (performanceMetrics.getAveragePower() > 0) {
            monitoringRecommendations.add("监控功率变化趋势");
            monitoringRecommendations.add("建立能耗基准线");
        }

        // 通用监控建议
        monitoringRecommendations.add("建立设备健康档案");
        monitoringRecommendations.add("定期记录运行参数");
        monitoringRecommendations.add("设置异常报警阈值");
    }

    /**
     * 基于历史数据生成建议
     */
    private void generateRecommendationsFromHistory(
            List<PumpDataEntity> pumpData,
            List<String> preventiveActions,
            List<String> scheduledActions) {

        // 分析运行模式
        analyzeOperatingPattern(pumpData, preventiveActions);

        // 分析维护历史
        analyzeMaintenanceHistory(pumpData, scheduledActions);

        // 分析季节性模式
        analyzeSeasonalPatterns(pumpData, preventiveActions);
    }

    /**
     * 分析运行模式
     */
    private void analyzeOperatingPattern(List<PumpDataEntity> pumpData, List<String> preventiveActions) {
        // 计算平均每日运行时间
        double totalRuntime = pumpData.stream()
                .filter(data -> data.getRuntimeMinutes() != null)
                .mapToDouble(PumpDataEntity::getRuntimeMinutes)
                .sum() / 60.0; // 转换为小时

        if (pumpData.size() > 0) {
            LocalDateTime firstTimestamp = pumpData.get(0).getTimestamp();
            LocalDateTime lastTimestamp = pumpData.get(pumpData.size() - 1).getTimestamp();
            long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(firstTimestamp, lastTimestamp);

            if (daysBetween > 0) {
                double avgDailyRuntime = totalRuntime / daysBetween;

                if (avgDailyRuntime > 20) {
                    preventiveActions.add("设备运行强度高，建议增加维护频率");
                } else if (avgDailyRuntime < 2) {
                    preventiveActions.add("设备使用频率低，注意定期试运行");
                }
            }
        }
    }

    /**
     * 分析维护历史
     */
    private void analyzeMaintenanceHistory(List<PumpDataEntity> pumpData, List<String> scheduledActions) {
        // 检查是否有维护标志
        boolean hasRecentMaintenanceFlag = pumpData.stream()
                .anyMatch(data -> Boolean.TRUE.equals(data.getMaintenanceFlag()));

        if (hasRecentMaintenanceFlag) {
            scheduledActions.add("存在维护需求标志，跟进处理");
        }

        // 检查故障历史
        List<String> faultCodes = pumpData.stream()
                .filter(data -> data.getFaultCode() != null && !data.getFaultCode().trim().isEmpty())
                .map(PumpDataEntity::getFaultCode)
                .collect(Collectors.toList());

        if (!faultCodes.isEmpty()) {
            scheduledActions.add("分析历史故障模式，制定针对性维护策略");
        }
    }

    /**
     * 分析季节性模式
     */
    private void analyzeSeasonalPatterns(List<PumpDataEntity> pumpData, List<String> preventiveActions) {
        // 简化的季节性分析
        Map<Integer, List<Double>> monthlyPowerUsage = new HashMap<>();

        for (PumpDataEntity data : pumpData) {
            if (data.getPowerKw() != null && data.getTimestamp() != null) {
                int month = data.getTimestamp().getMonthValue();
                monthlyPowerUsage.computeIfAbsent(month, k -> new ArrayList<>()).add(data.getPowerKw());
            }
        }

        // 如果有明显的季节性变化，建议相应调整
        if (monthlyPowerUsage.size() >= 6) {
            preventiveActions.add("建立季节性维护计划");
            preventiveActions.add("根据季节变化调整运行参数");
        }
    }

    /**
     * 生成紧急处理措施
     */
    private List<String> generateUrgentActions(String analysisType, PumpAnalysisResponseDTO.AnalysisResult result) {
        List<String> actions = new ArrayList<>();

        switch (analysisType) {
            case "振动异常检测":
                if (result.getDetectedValue() != null && result.getDetectedValue() > 7.0) {
                    actions.add("立即停机，振动值严重超标");
                    actions.add("检查轴承、叶轮、基础螺栓");
                    actions.add("联系专业技术维修人员");
                }
                break;
            case "启泵频率异常检测":
                if (result.getDeviationPercentage() != null && Math.abs(result.getDeviationPercentage()) > 50) {
                    actions.add("启泵频率异常，立即检查控制系统");
                    actions.add("检查管路系统和压力设置");
                }
                break;
            case "功率异常检测":
                if (result.getDeviationPercentage() != null && Math.abs(result.getDeviationPercentage()) > 30) {
                    actions.add("功率异常严重，检查电机和负载");
                    actions.add("检查电源电压和电流");
                }
                break;
            default:
                actions.add("严重异常，立即进行全面检查");
        }

        return actions;
    }

    /**
     * 生成计划处理措施
     */
    private List<String> generateScheduledActions(String analysisType, PumpAnalysisResponseDTO.AnalysisResult result) {
        List<String> actions = new ArrayList<>();

        switch (analysisType) {
            case "振动异常检测":
                actions.add("安排振动分析和动平衡检查");
                actions.add("检查轴承润滑状态");
                break;
            case "运行时间异常分析":
                actions.add("检查水泵运行参数设置");
                actions.add("分析负载变化原因");
                break;
            case "能耗趋势分析":
                actions.add("进行能耗审计和优化");
                actions.add("检查叶轮和泵壳效率");
                break;
            default:
                actions.add("安排详细检查和评估");
        }

        return actions;
    }

    /**
     * 生成预防性处理措施
     */
    private List<String> generatePreventiveActions(String analysisType, PumpAnalysisResponseDTO.AnalysisResult result) {
        List<String> actions = new ArrayList<>();

        switch (analysisType) {
            case "能耗趋势分析":
                actions.add("监控能耗变化趋势");
                actions.add("制定节能优化方案");
                break;
            case "性能评估":
                actions.add("定期进行性能测试");
                actions.add("建立性能基准线");
                break;
            default:
                actions.add("加强日常监控和记录");
        }

        return actions;
    }

    /**
     * 估算维护成本
     */
    private Double estimateMaintenanceCost(
            List<String> urgentActions,
            List<String> scheduledActions,
            List<String> preventiveActions) {

        double totalCost = 0.0;

        // 紧急处理成本
        totalCost += urgentActions.size() * 5000.0; // 紧急处理平均5000元/项

        // 计划处理成本
        totalCost += scheduledActions.size() * 2000.0; // 计划处理平均2000元/项

        // 预防性维护成本
        totalCost += preventiveActions.size() * 500.0; // 预防性维护平均500元/项

        return totalCost;
    }

    /**
     * 计算推荐维护时间
     */
    private LocalDateTime calculateRecommendedMaintenanceTime(
            List<String> urgentActions,
            List<String> scheduledActions,
            PumpAnalysisResponseDTO.PredictionInfo predictionInfo) {

        LocalDateTime now = LocalDateTime.now();

        // 有紧急措施，立即处理
        if (!urgentActions.isEmpty()) {
            return now;
        }

        // 基于预测的剩余寿命
        if (predictionInfo.getRemainingUsefulLifeDays() != null) {
            int remainingDays = predictionInfo.getRemainingUsefulLifeDays();
            if (remainingDays < 30) {
                return now.plusDays(remainingDays / 2); // 剩余寿命不足时，提前一半时间处理
            }
        }

        // 有计划处理措施，1-2周内安排
        if (!scheduledActions.isEmpty()) {
            return now.plusDays(7 + (int)(Math.random() * 7)); // 7-14天内
        }

        // 只有预防性措施，1个月内安排
        return now.plusDays(15 + (int)(Math.random() * 15)); // 15-30天内
    }

    /**
     * 生成智能维护建议总结报告
     */
    public String generateMaintenanceSummaryReport(
            PumpAnalysisResponseDTO.MaintenanceRecommendations recommendations) {

        StringBuilder report = new StringBuilder();
        report.append("=== 智能维护建议报告 ===\n\n");

        // 紧急处理措施
        if (!recommendations.getUrgentActions().isEmpty()) {
            report.append("【紧急处理措施】\n");
            for (int i = 0; i < recommendations.getUrgentActions().size(); i++) {
                report.append(String.format("%d. %s\n", i + 1, recommendations.getUrgentActions().get(i)));
            }
            report.append("\n");
        }

        // 计划处理措施
        if (!recommendations.getScheduledActions().isEmpty()) {
            report.append("【计划处理措施】\n");
            for (int i = 0; i < recommendations.getScheduledActions().size(); i++) {
                report.append(String.format("%d. %s\n", i + 1, recommendations.getScheduledActions().get(i)));
            }
            report.append("\n");
        }

        // 预防性维护建议
        if (!recommendations.getPreventiveActions().isEmpty()) {
            report.append("【预防性维护建议】\n");
            for (int i = 0; i < recommendations.getPreventiveActions().size(); i++) {
                report.append(String.format("%d. %s\n", i + 1, recommendations.getPreventiveActions().get(i)));
            }
            report.append("\n");
        }

        // 监控建议
        if (!recommendations.getMonitoringRecommendations().isEmpty()) {
            report.append("【监控建议】\n");
            for (int i = 0; i < recommendations.getMonitoringRecommendations().size(); i++) {
                report.append(String.format("%d. %s\n", i + 1, recommendations.getMonitoringRecommendations().get(i)));
            }
            report.append("\n");
        }

        // 成本估算
        if (recommendations.getEstimatedCost() != null) {
            report.append("【成本估算】\n");
            report.append(String.format("预计维护成本: %.2f 元\n", recommendations.getEstimatedCost()));
            report.append("\n");
        }

        // 推荐维护时间
        if (recommendations.getRecommendedMaintenanceTime() != null) {
            report.append("【推荐维护时间】\n");
            report.append(String.format("建议维护时间: %s\n",
                    recommendations.getRecommendedMaintenanceTime().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))));
        }

        return report.toString();
    }
}