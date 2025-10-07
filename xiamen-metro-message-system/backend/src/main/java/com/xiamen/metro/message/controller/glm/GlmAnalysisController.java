package com.xiamen.metro.message.controller.glm;

import com.xiamen.metro.message.dto.glm.MessageAnalysisRequestDTO;
import com.xiamen.metro.message.dto.glm.MessageAnalysisResponseDTO;
import com.xiamen.metro.message.service.glm.AnalysisCacheService;
import com.xiamen.metro.message.service.glm.MessageAnalysisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;

/**
 * GLM报文分析控制器
 * 提供报文分析的REST API接口
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/glm")
@Tag(name = "GLM报文分析", description = "基于GLM-4.6大模型的设备报文分析接口")
@RequiredArgsConstructor
@Validated
public class GlmAnalysisController {

    private final MessageAnalysisService messageAnalysisService;
    private final AnalysisCacheService cacheService;

    /**
     * 单个报文分析
     */
    @PostMapping("/analyze")
    @Operation(summary = "单个报文分析", description = "使用GLM-4.6模型分析单个设备报文")
    public ResponseEntity<ApiResponse<MessageAnalysisResponseDTO>> analyzeMessage(
            @Valid @RequestBody MessageAnalysisRequestDTO request) {

        log.info("接收到报文分析请求，类型: {}, 设备: {}", request.getMessageType(), request.getDeviceId());

        try {
            MessageAnalysisResponseDTO result = messageAnalysisService.analyzeMessage(request);

            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (Exception e) {
            log.error("报文分析失败", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("报文分析失败: " + e.getMessage()));
        }
    }

    /**
     * 批量报文分析
     */
    @PostMapping("/analyze/batch")
    @Operation(summary = "批量报文分析", description = "批量分析多个设备报文")
    public ResponseEntity<ApiResponse<List<MessageAnalysisResponseDTO>>> analyzeBatch(
            @Valid @RequestBody List<MessageAnalysisRequestDTO> requests) {

        log.info("接收到批量报文分析请求，数量: {}", requests.size());

        if (requests.size() > 100) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("批量分析数量不能超过100"));
        }

        try {
            List<MessageAnalysisResponseDTO> results = messageAnalysisService.analyzeBatch(requests);

            return ResponseEntity.ok(ApiResponse.success(results));
        } catch (Exception e) {
            log.error("批量报文分析失败", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("批量报文分析失败: " + e.getMessage()));
        }
    }

    /**
     * 服务健康检查
     */
    @GetMapping("/health")
    @Operation(summary = "服务健康检查", description = "检查GLM API服务状态")
    public ResponseEntity<ApiResponse<HealthStatus>> health() {
        try {
            boolean isHealthy = messageAnalysisService.isHealthy();
            var cacheStats = cacheService.getCacheStats();
            var apiMetrics = messageAnalysisService.getApiMetrics();

            HealthStatus status = HealthStatus.builder()
                    .healthy(isHealthy)
                    .cacheKeys(cacheStats.getTotalKeys())
                    .availablePermissions(apiMetrics.getAvailablePermissions())
                    .waitingThreads(apiMetrics.getNumberOfWaitingThreads())
                    .build();

            String statusText = isHealthy ? "健康" : "异常";
            return ResponseEntity.ok(ApiResponse.success(status, "GLM服务状态: " + statusText));

        } catch (Exception e) {
            log.error("健康检查失败", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("健康检查失败: " + e.getMessage()));
        }
    }

    /**
     * 清除缓存
     */
    @DeleteMapping("/cache")
    @Operation(summary = "清除缓存", description = "清除分析结果缓存")
    public ResponseEntity<ApiResponse<String>> clearCache(
            @RequestParam(required = false) String messageContent,
            @RequestParam(required = false) String messageType,
            @RequestParam(required = false) String analysisDepth) {

        try {
            if (messageContent != null && messageType != null && analysisDepth != null) {
                messageAnalysisService.clearCache(messageContent, messageType, analysisDepth);
                return ResponseEntity.ok(ApiResponse.success("指定缓存已清除"));
            } else {
                messageAnalysisService.clearAllCache();
                return ResponseEntity.ok(ApiResponse.success("所有缓存已清除"));
            }
        } catch (Exception e) {
            log.error("清除缓存失败", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("清除缓存失败: " + e.getMessage()));
        }
    }

    /**
     * 获取缓存统计
     */
    @GetMapping("/cache/stats")
    @Operation(summary = "缓存统计", description = "获取分析缓存使用统计")
    public ResponseEntity<ApiResponse<AnalysisCacheService.CacheStats>> cacheStats() {
        try {
            AnalysisCacheService.CacheStats stats = cacheService.getCacheStats();
            return ResponseEntity.ok(ApiResponse.success(stats));
        } catch (Exception e) {
            log.error("获取缓存统计失败", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("获取缓存统计失败: " + e.getMessage()));
        }
    }

    /**
     * 测试API连接
     */
    @PostMapping("/test")
    @Operation(summary = "测试API连接", description = "测试GLM API连接和基本功能")
    public ResponseEntity<ApiResponse<TestResult>> testApi() {
        try {
            String testMessage = "TEST: 设备正常运行，状态良好";
            MessageAnalysisRequestDTO request = MessageAnalysisRequestDTO.builder()
                    .messageContent(testMessage)
                    .messageType("STATUS")
                    .deviceId("TEST-001")
                    .timestamp(System.currentTimeMillis())
                    .enableCache(false)
                    .build();

            long startTime = System.currentTimeMillis();
            MessageAnalysisResponseDTO result = messageAnalysisService.analyzeMessage(request);
            long processingTime = System.currentTimeMillis() - startTime;

            TestResult testResult = TestResult.builder()
                    .success(result != null && "SUCCESS".equals(result.getStatus()))
                    .processingTimeMs(processingTime)
                    .fromCache(result != null && Boolean.TRUE.equals(result.getFromCache()))
                    .confidenceScore(result != null ? result.getConfidenceScore() : 0.0)
                    .status(result != null ? result.getStatus() : "FAILED")
                    .build();

            return ResponseEntity.ok(ApiResponse.success(testResult, "GLM API测试完成"));

        } catch (Exception e) {
            log.error("API测试失败", e);
            TestResult failedResult = TestResult.builder()
                    .success(false)
                    .processingTimeMs(0L)
                    .fromCache(false)
                    .confidenceScore(0.0)
                    .status("FAILED")
                    .build();

            return ResponseEntity.ok(ApiResponse.success(failedResult, "API测试失败: " + e.getMessage()));
        }
    }

    /**
     * 通用API响应
     */
    @lombok.Data
    @lombok.Builder
    public static class ApiResponse<T> {
        private boolean success;
        private String message;
        private T data;
        private String timestamp;

        public static <T> ApiResponse<T> success(T data) {
            return success(data, "操作成功");
        }

        public static <T> ApiResponse<T> success(T data, String message) {
            return ApiResponse.<T>builder()
                    .success(true)
                    .message(message)
                    .data(data)
                    .timestamp(java.time.LocalDateTime.now().toString())
                    .build();
        }

        public static <T> ApiResponse<T> error(String message) {
            return ApiResponse.<T>builder()
                    .success(false)
                    .message(message)
                    .timestamp(java.time.LocalDateTime.now().toString())
                    .build();
        }
    }

    /**
     * 健康状态
     */
    @lombok.Data
    @lombok.Builder
    public static class HealthStatus {
        private boolean healthy;
        private long cacheKeys;
        private int availablePermissions;
        private int waitingThreads;
    }

    /**
     * 测试结果
     */
    @lombok.Data
    @lombok.Builder
    public static class TestResult {
        private boolean success;
        private long processingTimeMs;
        private boolean fromCache;
        private double confidenceScore;
        private String status;
    }
}