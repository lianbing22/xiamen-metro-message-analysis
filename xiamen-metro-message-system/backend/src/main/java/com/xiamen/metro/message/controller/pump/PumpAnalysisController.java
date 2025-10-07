package com.xiamen.metro.message.controller.pump;

import com.xiamen.metro.message.dto.pump.PumpAnalysisRequestDTO;
import com.xiamen.metro.message.dto.pump.PumpAnalysisResponseDTO;
import com.xiamen.metro.message.service.pump.PumpIntelligentAnalysisService;
import com.xiamen.metro.message.service.pump.PumpPerformanceEvaluationService;
import com.xiamen.metro.message.service.pump.PumpMaintenanceRecommendationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 水泵分析控制器
 *
 * @author Xiamen Metro System
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/pump-analysis")
@RequiredArgsConstructor
@Validated
@Tag(name = "水泵分析", description = "水泵智能分析和故障预测相关接口")
public class PumpAnalysisController {

    private final PumpIntelligentAnalysisService intelligentAnalysisService;
    private final PumpPerformanceEvaluationService performanceEvaluationService;
    private final PumpMaintenanceRecommendationService maintenanceRecommendationService;

    /**
     * 执行水泵智能分析
     */
    @PostMapping("/analyze")
    @Operation(summary = "执行水泵智能分析", description = "对指定设备在时间范围内进行全面的智能分析，包括异常检测、故障预测、性能评估等")
    public ResponseEntity<PumpAnalysisResponseDTO> analyzePump(
            @Valid @RequestBody PumpAnalysisRequestDTO request) {

        log.info("收到水泵分析请求，设备: {}, 开始时间: {}, 结束时间: {}",
                request.getDeviceId(), request.getStartTime(), request.getEndTime());

        try {
            PumpAnalysisResponseDTO response = intelligentAnalysisService.performIntelligentAnalysis(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("水泵分析失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 批量分析多个设备
     */
    @PostMapping("/batch-analyze")
    @Operation(summary = "批量分析水泵", description = "对多个设备批量执行智能分析")
    public ResponseEntity<List<PumpAnalysisResponseDTO>> batchAnalyzePumps(
            @Valid @RequestBody List<PumpAnalysisRequestDTO> requests) {

        log.info("收到批量水泵分析请求，设备数量: {}", requests.size());

        try {
            List<PumpAnalysisResponseDTO> responses = intelligentAnalysisService.performBatchAnalysis(requests);
            return ResponseEntity.ok(responses);
        } catch (Exception e) {
            log.error("批量水泵分析失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 获取设备健康状态摘要
     */
    @GetMapping("/health-summary/{deviceId}")
    @Operation(summary = "获取设备健康摘要", description = "获取指定设备的近期健康状态摘要信息")
    public ResponseEntity<Map<String, Object>> getHealthSummary(
            @Parameter(description = "设备ID") @PathVariable String deviceId,
            @Parameter(description = "查询起始时间，格式: yyyy-MM-dd HH:mm:ss") @RequestParam(required = false) String since) {

        try {
            LocalDateTime sinceTime = since != null ?
                    LocalDateTime.parse(since, java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) :
                    LocalDateTime.now().minusDays(7);

            Map<String, Object> summary = intelligentAnalysisService.getDeviceHealthSummary(deviceId, sinceTime);
            return ResponseEntity.ok(summary);
        } catch (Exception e) {
            log.error("获取设备健康摘要失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 快速分析（仅基础检测）
     */
    @PostMapping("/quick-analyze/{deviceId}")
    @Operation(summary = "快速分析", description = "对设备进行快速的基础分析，返回简单的健康状态")
    public ResponseEntity<PumpAnalysisResponseDTO> quickAnalyze(
            @Parameter(description = "设备ID") @PathVariable String deviceId,
            @Parameter(description = "分析时间范围（小时），默认24小时") @RequestParam(defaultValue = "24") int hours) {

        try {
            PumpAnalysisRequestDTO request = new PumpAnalysisRequestDTO();
            request.setDeviceId(deviceId);
            request.setStartTime(LocalDateTime.now().minusHours(hours));
            request.setEndTime(LocalDateTime.now());
            request.setAnalysisDepth(PumpAnalysisRequestDTO.AnalysisDepth.BASIC);
            request.setAnalysisTypes(List.of(
                    PumpAnalysisRequestDTO.AnalysisType.STARTUP_FREQUENCY,
                    PumpAnalysisRequestDTO.AnalysisType.RUNTIME_ANALYSIS,
                    PumpAnalysisRequestDTO.AnalysisType.ANOMALY_CLASSIFICATION
            ));

            PumpAnalysisResponseDTO response = intelligentAnalysisService.performIntelligentAnalysis(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("快速分析失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 获取设备性能报告
     */
    @GetMapping("/performance-report/{deviceId}")
    @Operation(summary = "获取性能报告", description = "获取指定设备的详细性能评估报告")
    public ResponseEntity<String> getPerformanceReport(
            @Parameter(description = "设备ID") @PathVariable String deviceId,
            @Parameter(description = "报告开始时间") @RequestParam String startTime,
            @Parameter(description = "报告结束时间") @RequestParam String endTime) {

        try {
            LocalDateTime start = LocalDateTime.parse(startTime, java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            LocalDateTime end = LocalDateTime.parse(endTime, java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

            // 这里需要调用性能评估服务生成报告
            // 实际实现中需要从数据库获取数据并生成报告
            String report = "性能报告功能待完善";
            return ResponseEntity.ok(report);
        } catch (Exception e) {
            log.error("获取性能报告失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 获取维护建议报告
     */
    @GetMapping("/maintenance-report/{deviceId}")
    @Operation(summary = "获取维护建议报告", description = "获取指定设备的智能维护建议报告")
    public ResponseEntity<String> getMaintenanceReport(
            @Parameter(description = "设备ID") @PathVariable String deviceId,
            @Parameter(description = "报告开始时间") @RequestParam String startTime,
            @Parameter(description = "报告结束时间") @RequestParam String endTime) {

        try {
            LocalDateTime start = LocalDateTime.parse(startTime, java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            LocalDateTime end = LocalDateTime.parse(endTime, java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

            // 这里需要执行分析并生成维护建议报告
            String report = "维护建议报告功能待完善";
            return ResponseEntity.ok(report);
        } catch (Exception e) {
            log.error("获取维护建议报告失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 获取分析配置模板
     */
    @GetMapping("/analysis-template")
    @Operation(summary = "获取分析配置模板", description = "获取不同深度分析的建议配置模板")
    public ResponseEntity<Map<String, Object>> getAnalysisTemplate(
            @Parameter(description = "分析深度") @RequestParam(defaultValue = "STANDARD") String depth) {

        try {
            PumpAnalysisRequestDTO.AnalysisDepth analysisDepth = PumpAnalysisRequestDTO.AnalysisDepth.valueOf(depth.toUpperCase());

            Map<String, Object> template = Map.of(
                "analysisDepth", analysisDepth.name(),
                "recommendedTypes", getRecommendedAnalysisTypes(analysisDepth),
                "defaultThresholds", getDefaultThresholds(),
                "modelConfig", getDefaultModelConfig(analysisDepth)
            );

            return ResponseEntity.ok(template);
        } catch (Exception e) {
            log.error("获取分析模板失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 获取支持的分析类型
     */
    @GetMapping("/analysis-types")
    @Operation(summary = "获取支持的分析类型", description = "返回系统支持的所有分析类型及其描述")
    public ResponseEntity<List<Map<String, Object>>> getAnalysisTypes() {
        try {
            List<Map<String, Object>> types = List.of(
                Map.of("type", "STARTUP_FREQUENCY", "description", "启泵频率异常检测", "category", "异常检测"),
                Map.of("type", "RUNTIME_ANALYSIS", "description", "运行时间异常分析", "category", "异常检测"),
                Map.of("type", "ENERGY_TREND", "description", "能耗趋势分析", "category", "趋势分析"),
                Map.of("type", "FAULT_PREDICTION", "description", "故障预测", "category", "预测分析"),
                Map.of("type", "PERFORMANCE_EVALUATION", "description", "性能评估", "category", "性能分析"),
                Map.of("type", "ANOMALY_CLASSIFICATION", "description", "异常分类分级", "category", "异常检测")
            );

            return ResponseEntity.ok(types);
        } catch (Exception e) {
            log.error("获取分析类型失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 获取推荐的配置参数
     */
    private List<PumpAnalysisRequestDTO.AnalysisType> getRecommendedAnalysisTypes(PumpAnalysisRequestDTO.AnalysisDepth depth) {
        return switch (depth) {
            case BASIC -> List.of(
                PumpAnalysisRequestDTO.AnalysisType.STARTUP_FREQUENCY,
                PumpAnalysisRequestDTO.AnalysisType.ANOMALY_CLASSIFICATION
            );
            case STANDARD -> List.of(
                PumpAnalysisRequestDTO.AnalysisType.STARTUP_FREQUENCY,
                PumpAnalysisRequestDTO.AnalysisType.RUNTIME_ANALYSIS,
                PumpAnalysisRequestDTO.AnalysisType.ENERGY_TREND,
                PumpAnalysisRequestDTO.AnalysisType.ANOMALY_CLASSIFICATION
            );
            case COMPREHENSIVE -> List.of(
                PumpAnalysisRequestDTO.AnalysisType.STARTUP_FREQUENCY,
                PumpAnalysisRequestDTO.AnalysisType.RUNTIME_ANALYSIS,
                PumpAnalysisRequestDTO.AnalysisType.ENERGY_TREND,
                PumpAnalysisRequestDTO.AnalysisType.FAULT_PREDICTION,
                PumpAnalysisRequestDTO.AnalysisType.ANOMALY_CLASSIFICATION
            );
            case ADVANCED -> List.of(PumpAnalysisRequestDTO.AnalysisType.values()); // 所有类型
        };
    }

    /**
     * 获取默认阈值配置
     */
    private Map<String, Double> getDefaultThresholds() {
        return Map.of(
            "startupFrequencyThreshold", 10.0,
            "runtimeThreshold", 480.0,
            "powerAnomalyThreshold", 20.0,
            "vibrationThreshold", 4.5,
            "energyIncreaseThreshold", 15.0,
            "temperatureThreshold", 60.0,
            "pressureThreshold", 100.0
        );
    }

    /**
     * 获取默认模型配置
     */
    private Map<String, Object> getDefaultModelConfig(PumpAnalysisRequestDTO.AnalysisDepth depth) {
        return Map.of(
            "modelVersion", "1.0",
            "predictionWindowDays", depth == PumpAnalysisRequestDTO.AnalysisDepth.ADVANCED ? 14 : 7,
            "confidenceThreshold", 0.7,
            "trainingDataDays", depth == PumpAnalysisRequestDTO.AnalysisDepth.ADVANCED ? 60 : 30,
            "featureEngineering", Map.of(
                "useTimeFeatures", true,
                "useStatisticalFeatures", true,
                "useFrequencyFeatures", depth == PumpAnalysisRequestDTO.AnalysisDepth.ADVANCED,
                "slidingWindowSize", depth == PumpAnalysisRequestDTO.AnalysisDepth.ADVANCED ? 48 : 24
            )
        );
    }
}