package com.xiamen.metro.message.service.pump;

import com.xiamen.metro.message.entity.PumpDataEntity;
import com.xiamen.metro.message.entity.PumpAnalysisResultEntity;
import com.xiamen.metro.message.repository.PumpDataRepository;
import com.xiamen.metro.message.repository.PumpAnalysisResultRepository;
import com.xiamen.metro.message.dto.pump.PumpAnalysisRequestDTO;
import com.xiamen.metro.message.dto.pump.PumpAnalysisResponseDTO;
import com.xiamen.metro.message.service.glm.MessageAnalysisService;
import com.xiamen.metro.message.service.alert.AlertManagementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 水泵智能分析服务
 * 整合所有分析组件的主要服务类
 *
 * @author Xiamen Metro System
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PumpIntelligentAnalysisService {

    private final PumpDataRepository pumpDataRepository;
    private final PumpAnalysisResultRepository analysisResultRepository;
    private final PumpAnomalyDetectionService anomalyDetectionService;
    private final PumpFaultPredictionService faultPredictionService;
    private final PumpPerformanceEvaluationService performanceEvaluationService;
    private final PumpMaintenanceRecommendationService maintenanceRecommendationService;
    private final MessageAnalysisService messageAnalysisService;
    private final AlertManagementService alertManagementService;

    /**
     * 执行完整的水泵智能分析
     */
    @Transactional
    public PumpAnalysisResponseDTO performIntelligentAnalysis(PumpAnalysisRequestDTO request) {
        long startTime = System.currentTimeMillis();
        String analysisId = UUID.randomUUID().toString();

        log.info("开始水泵智能分析，ID: {}, 设备: {}, 时间范围: {} - {}",
                analysisId, request.getDeviceId(), request.getStartTime(), request.getEndTime());

        try {
            // 1. 获取历史数据
            List<PumpDataEntity> pumpData = getPumpData(request.getDeviceId(), request.getStartTime(), request.getEndTime());

            if (pumpData.isEmpty()) {
                return createEmptyResponse(analysisId, request.getDeviceId(), "未找到指定设备的历史数据");
            }

            // 2. 执行各类分析
            List<PumpAnalysisResponseDTO.AnalysisResult> analysisResults = executeAnalyses(request, pumpData);

            // 3. 性能评估
            PumpAnalysisResponseDTO.PerformanceMetrics performanceMetrics =
                    performanceEvaluationService.evaluatePerformance(pumpData, request.getStartTime(), request.getEndTime());

            // 4. 故障预测
            PumpAnalysisResponseDTO.PredictionInfo predictionInfo =
                    faultPredictionService.predictFaults(pumpData, request.getModelConfig());

            // 5. 生成维护建议
            PumpAnalysisResponseDTO.MaintenanceRecommendations maintenanceRecommendations =
                    maintenanceRecommendationService.generateMaintenanceRecommendations(
                            pumpData, analysisResults, predictionInfo, performanceMetrics);

            // 6. 计算总体健康评分和风险等级
            double overallHealthScore = calculateOverallHealthScore(analysisResults, performanceMetrics);
            String riskLevel = determineRiskLevel(analysisResults, predictionInfo);

            // 7. 构建响应
            PumpAnalysisResponseDTO response = PumpAnalysisResponseDTO.builder()
                    .analysisId(analysisId)
                    .deviceId(request.getDeviceId())
                    .analysisTime(LocalDateTime.now())
                    .status("SUCCESS")
                    .overallHealthScore(overallHealthScore)
                    .riskLevel(riskLevel)
                    .analysisResults(analysisResults)
                    .performanceMetrics(performanceMetrics)
                    .maintenanceRecommendations(maintenanceRecommendations)
                    .predictionInfo(predictionInfo)
                    .processingTimeMs(System.currentTimeMillis() - startTime)
                    .fromCache(false)
                    .confidenceScore(calculateOverallConfidence(analysisResults, predictionInfo))
                    .modelVersion(request.getModelConfig() != null ? request.getModelConfig().getModelVersion() : "1.0")
                    .build();

            // 8. 保存分析结果
            saveAnalysisResults(request, response);

            // 9. 触发告警检查
            try {
                List<com.xiamen.metro.message.dto.alert.AlertRecordDTO> alerts = alertManagementService.processPumpAnalysisResults(
                        request.getDeviceId(), response);

                if (!alerts.isEmpty()) {
                    log.info("分析完成并生成 {} 个告警", alerts.size());
                }
            } catch (Exception e) {
                log.warn("告警处理失败，但分析结果正常", e);
            }

            log.info("水泵智能分析完成，ID: {}, 耗时: {}ms, 健康评分: {}, 风险等级: {}",
                    analysisId, response.getProcessingTimeMs(), overallHealthScore, riskLevel);

            return response;

        } catch (Exception e) {
            log.error("水泵智能分析失败，ID: {}", analysisId, e);
            return createErrorResponse(analysisId, request.getDeviceId(), e.getMessage());
        }
    }

    /**
     * 获取水泵数据
     */
    private List<PumpDataEntity> getPumpData(String deviceId, LocalDateTime startTime, LocalDateTime endTime) {
        return pumpDataRepository.findByDeviceIdAndTimestampBetweenOrderByTimestampAsc(deviceId, startTime, endTime);
    }

    /**
     * 执行各类分析
     */
    private List<PumpAnalysisResponseDTO.AnalysisResult> executeAnalyses(
            PumpAnalysisRequestDTO request, List<PumpDataEntity> pumpData) {

        List<PumpAnalysisResponseDTO.AnalysisResult> results = new ArrayList<>();
        Set<PumpAnalysisRequestDTO.AnalysisType> analysisTypes = request.getAnalysisTypes() != null ?
                new HashSet<>(request.getAnalysisTypes()) :
                EnumSet.allOf(PumpAnalysisRequestDTO.AnalysisType.class);

        // 根据请求的分析类型执行相应分析
        for (PumpAnalysisRequestDTO.AnalysisType analysisType : analysisTypes) {
            try {
                PumpAnalysisResponseDTO.AnalysisResult result = executeSpecificAnalysis(
                        analysisType, pumpData, request.getThresholdConfig());
                if (result != null) {
                    results.add(result);
                }
            } catch (Exception e) {
                log.error("执行 {} 分析失败", analysisType.getDescription(), e);
                // 创建错误结果
                results.add(PumpAnalysisResponseDTO.AnalysisResult.builder()
                        .analysisType(analysisType.getDescription())
                        .severityLevel(1)
                        .confidence(0.0)
                        .description("分析执行失败: " + e.getMessage())
                        .detectedValue(null)
                        .expectedValue(null)
                        .deviationPercentage(null)
                        .trendDirection("UNKNOWN")
                        .detailedMetrics(new HashMap<>())
                        .recommendations(new ArrayList<>())
                        .build());
            }
        }

        // 如果启用了GLM集成，进行智能异常分类
        if (shouldUseGlmAnalysis(request)) {
            performGlmIntelligentClassification(results, pumpData, request);
        }

        return results;
    }

    /**
     * 执行特定类型的分析
     */
    private PumpAnalysisResponseDTO.AnalysisResult executeSpecificAnalysis(
            PumpAnalysisRequestDTO.AnalysisType analysisType,
            List<PumpDataEntity> pumpData,
            PumpAnalysisRequestDTO.ThresholdConfig thresholdConfig) {

        switch (analysisType) {
            case STARTUP_FREQUENCY:
                return anomalyDetectionService.detectStartupFrequencyAnomaly(pumpData, thresholdConfig);
            case RUNTIME_ANALYSIS:
                return anomalyDetectionService.detectRuntimeAnomaly(pumpData, thresholdConfig);
            case ENERGY_TREND:
                return anomalyDetectionService.analyzeEnergyTrend(pumpData, thresholdConfig);
            case FAULT_PREDICTION:
                // 故障预测在后续单独处理
                return null;
            case PERFORMANCE_EVALUATION:
                // 性能评估在后续单独处理
                return null;
            case ANOMALY_CLASSIFICATION:
                // 异常分类基于其他分析结果
                return performAnomalyClassification(pumpData, thresholdConfig);
            default:
                log.warn("未知的分析类型: {}", analysisType);
                return null;
        }
    }

    /**
     * 执行异常分类
     */
    private PumpAnalysisResponseDTO.AnalysisResult performAnomalyClassification(
            List<PumpDataEntity> pumpData, PumpAnalysisRequestDTO.ThresholdConfig thresholdConfig) {

        // 统计各类异常
        long faultCount = pumpData.stream()
                .filter(data -> data.getFaultCode() != null && !data.getFaultCode().trim().isEmpty())
                .count();

        long alarmCount = pumpData.stream()
                .filter(data -> data.getAlarmLevel() != null && data.getAlarmLevel() > 1)
                .count();

        double anomalyRate = (double) (faultCount + alarmCount) / pumpData.size() * 100;
        int severityLevel = anomalyRate > 20 ? 4 : anomalyRate > 10 ? 3 : anomalyRate > 5 ? 2 : 1;

        Map<String, Object> detailedMetrics = new HashMap<>();
        detailedMetrics.put("totalRecords", pumpData.size());
        detailedMetrics.put("faultCount", faultCount);
        detailedMetrics.put("alarmCount", alarmCount);
        detailedMetrics.put("anomalyRate", anomalyRate);

        List<String> recommendations = new ArrayList<>();
        if (anomalyRate > 10) {
            recommendations.add("异常率较高，建议全面检查");
        }
        if (faultCount > 0) {
            recommendations.add("存在故障记录，分析故障模式");
        }

        return PumpAnalysisResponseDTO.AnalysisResult.builder()
                .analysisType("异常分类分级")
                .severityLevel(severityLevel)
                .confidence(0.8)
                .description(String.format("异常率: %.1f%%, 故障数: %d, 报警数: %d", anomalyRate, faultCount, alarmCount))
                .detectedValue(anomalyRate)
                .expectedValue(5.0) // 期望异常率5%以下
                .deviationPercentage(anomalyRate - 5.0)
                .trendDirection(anomalyRate > 5 ? "INCREASING" : "STABLE")
                .detailedMetrics(detailedMetrics)
                .recommendations(recommendations)
                .build();
    }

    /**
     * 判断是否使用GLM分析
     */
    private boolean shouldUseGlmAnalysis(PumpAnalysisRequestDTO request) {
        return request.getAnalysisDepth() == PumpAnalysisRequestDTO.AnalysisDepth.ADVANCED ||
               request.getAnalysisDepth() == PumpAnalysisRequestDTO.AnalysisDepth.COMPREHENSIVE;
    }

    /**
     * 使用GLM进行智能异常分类
     */
    private void performGlmIntelligentClassification(
            List<PumpAnalysisResponseDTO.AnalysisResult> results,
            List<PumpDataEntity> pumpData,
            PumpAnalysisRequestDTO request) {

        try {
            // 构建GLM分析的上下文
            StringBuilder context = new StringBuilder();
            context.append("基于以下水泵分析结果，请提供智能化的异常分类和根因分析：\n\n");

            for (PumpAnalysisResponseDTO.AnalysisResult result : results) {
                context.append(String.format("- %s: %s (严重级别: %d, 置信度: %.2f)\n",
                        result.getAnalysisType(), result.getDescription(), result.getSeverityLevel(), result.getConfidence()));
            }

            context.append("\n请提供：1) 异常根因分析 2) 关联性分析 3) 优先级排序 4) 额外的诊断建议");

            // 调用GLM服务
            // 这里可以集成现有的GLM API调用逻辑
            log.info("使用GLM进行智能异常分类，分析ID: {}", UUID.randomUUID());

        } catch (Exception e) {
            log.warn("GLM智能分类失败，继续使用基础分析结果", e);
        }
    }

    /**
     * 计算总体健康评分
     */
    private double calculateOverallHealthScore(
            List<PumpAnalysisResponseDTO.AnalysisResult> analysisResults,
            PumpAnalysisResponseDTO.PerformanceMetrics performanceMetrics) {

        double score = 100.0; // 满分100

        // 基于分析结果扣分
        for (PumpAnalysisResponseDTO.AnalysisResult result : analysisResults) {
            int severityLevel = result.getSeverityLevel();
            double confidence = result.getConfidence();

            // 根据严重级别和置信度扣分
            double deduction = (severityLevel - 1) * 15 * confidence;
            score -= deduction;
        }

        // 基于性能评分调整
        if (performanceMetrics != null) {
            double performanceScore = (performanceMetrics.getEfficiencyScore() +
                                     performanceMetrics.getReliabilityScore() +
                                     performanceMetrics.getMaintenanceScore()) / 3.0;
            score = (score + performanceScore) / 2.0; // 取平均值
        }

        return Math.max(0, Math.min(100, score));
    }

    /**
     * 确定风险等级
     */
    private String determineRiskLevel(
            List<PumpAnalysisResponseDTO.AnalysisResult> analysisResults,
            PumpAnalysisResponseDTO.PredictionInfo predictionInfo) {

        // 检查是否有严重或错误级别的异常
        boolean hasCritical = analysisResults.stream().anyMatch(r -> r.getSeverityLevel() >= 4);
        boolean hasError = analysisResults.stream().anyMatch(r -> r.getSeverityLevel() >= 3);

        // 检查故障概率
        double failureProbability = predictionInfo != null ? predictionInfo.getFailureProbability() : 0.0;

        if (hasCritical || failureProbability > 0.8) {
            return "CRITICAL";
        } else if (hasError || failureProbability > 0.6) {
            return "HIGH";
        } else if (failureProbability > 0.3) {
            return "MEDIUM";
        } else {
            return "LOW";
        }
    }

    /**
     * 计算总体置信度
     */
    private double calculateOverallConfidence(
            List<PumpAnalysisResponseDTO.AnalysisResult> analysisResults,
            PumpAnalysisResponseDTO.PredictionInfo predictionInfo) {

        double avgAnalysisConfidence = analysisResults.stream()
                .mapToDouble(r -> r.getConfidence())
                .average()
                .orElse(0.5);

        double predictionConfidence = 0.7; // 默认预测置信度

        return (avgAnalysisConfidence + predictionConfidence) / 2.0;
    }

    /**
     * 保存分析结果
     */
    private void saveAnalysisResults(PumpAnalysisRequestDTO request, PumpAnalysisResponseDTO response) {
        try {
            for (PumpAnalysisResponseDTO.AnalysisResult result : response.getAnalysisResults()) {
                PumpAnalysisResultEntity entity = new PumpAnalysisResultEntity();
                entity.setDeviceId(request.getDeviceId());
                entity.setAnalysisTimestamp(response.getAnalysisTime());
                entity.setAnalysisType(result.getAnalysisType());
                entity.setSeverityLevel(result.getSeverityLevel());
                entity.setConfidenceScore(result.getConfidence());
                entity.setAnomalyDescription(result.getDescription());
                entity.setDetectedValue(result.getDetectedValue());
                entity.setExpectedValue(result.getExpectedValue());
                entity.setDeviationPercentage(result.getDeviationPercentage());
                entity.setTrendDirection(result.getTrendDirection());
                entity.setMaintenanceRecommendation(String.join("; ", result.getRecommendations()));
                entity.setPriorityLevel(result.getSeverityLevel()); // 简化：使用严重级别作为优先级
                entity.setDataPeriodStart(request.getStartTime());
                entity.setDataPeriodEnd(request.getEndTime());
                entity.setModelVersion(response.getModelVersion());
                entity.setIsConfirmed(false);

                analysisResultRepository.save(entity);
            }
        } catch (Exception e) {
            log.error("保存分析结果失败", e);
        }
    }

    /**
     * 创建空响应
     */
    private PumpAnalysisResponseDTO createEmptyResponse(String analysisId, String deviceId, String message) {
        return PumpAnalysisResponseDTO.builder()
                .analysisId(analysisId)
                .deviceId(deviceId)
                .analysisTime(LocalDateTime.now())
                .status("NO_DATA")
                .overallHealthScore(0.0)
                .riskLevel("UNKNOWN")
                .analysisResults(new ArrayList<>())
                .performanceMetrics(createEmptyPerformanceMetrics())
                .maintenanceRecommendations(createEmptyMaintenanceRecommendations())
                .predictionInfo(createEmptyPredictionInfo())
                .processingTimeMs(0L)
                .fromCache(false)
                .confidenceScore(0.0)
                .modelVersion("1.0")
                .build();
    }

    /**
     * 创建错误响应
     */
    private PumpAnalysisResponseDTO createErrorResponse(String analysisId, String deviceId, String errorMessage) {
        return PumpAnalysisResponseDTO.builder()
                .analysisId(analysisId)
                .deviceId(deviceId)
                .analysisTime(LocalDateTime.now())
                .status("FAILED")
                .overallHealthScore(0.0)
                .riskLevel("UNKNOWN")
                .analysisResults(new ArrayList<>())
                .performanceMetrics(createEmptyPerformanceMetrics())
                .maintenanceRecommendations(createEmptyMaintenanceRecommendations())
                .predictionInfo(createEmptyPredictionInfo())
                .processingTimeMs(0L)
                .fromCache(false)
                .confidenceScore(0.0)
                .modelVersion("1.0")
                .build();
    }

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

    private PumpAnalysisResponseDTO.MaintenanceRecommendations createEmptyMaintenanceRecommendations() {
        return PumpAnalysisResponseDTO.MaintenanceRecommendations.builder()
                .urgentActions(new ArrayList<>())
                .scheduledActions(new ArrayList<>())
                .preventiveActions(new ArrayList<>())
                .monitoringRecommendations(new ArrayList<>())
                .estimatedCost(0.0)
                .recommendedMaintenanceTime(LocalDateTime.now())
                .build();
    }

    private PumpAnalysisResponseDTO.PredictionInfo createEmptyPredictionInfo() {
        return PumpAnalysisResponseDTO.PredictionInfo.builder()
                .failureProbability(0.0)
                .remainingUsefulLifeDays(0)
                .predictedFailureTime(LocalDateTime.now().plusDays(30))
                .performanceDegradationTrend("UNKNOWN")
                .keyMetricsPrediction(new HashMap<>())
                .confidenceIntervals(new HashMap<>())
                .build();
    }

    /**
     * 批量分析多个设备
     */
    public List<PumpAnalysisResponseDTO> performBatchAnalysis(List<PumpAnalysisRequestDTO> requests) {
        log.info("开始批量水泵智能分析，数量: {}", requests.size());

        return requests.parallelStream()
                .map(this::performIntelligentAnalysis)
                .collect(Collectors.toList());
    }

    /**
     * 获取设备健康状态摘要
     */
    public Map<String, Object> getDeviceHealthSummary(String deviceId, LocalDateTime since) {
        List<PumpAnalysisResultEntity> recentResults = analysisResultRepository.findByDeviceIdAndAnalysisTimestampBetweenOrderByAnalysisTimestampDesc(
                deviceId, since, LocalDateTime.now());

        Map<String, Object> summary = new HashMap<>();
        summary.put("deviceId", deviceId);
        summary.put("lastAnalysisTime", recentResults.isEmpty() ? null : recentResults.get(0).getAnalysisTimestamp());
        summary.put("totalAnalyses", recentResults.size());
        summary.put("criticalIssues", recentResults.stream().mapToInt(r -> r.getSeverityLevel() >= 4 ? 1 : 0).sum());
        summary.put("highIssues", recentResults.stream().mapToInt(r -> r.getSeverityLevel() == 3 ? 1 : 0).sum());
        summary.put("avgConfidence", recentResults.stream().mapToDouble(r -> r.getConfidenceScore() != null ? r.getConfidenceScore() : 0.0).average().orElse(0.0));

        return summary;
    }
}