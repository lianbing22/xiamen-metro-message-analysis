package com.xiamen.metro.message.integration;

import com.xiamen.metro.message.dto.glm.MessageAnalysisRequestDTO;
import com.xiamen.metro.message.dto.glm.MessageAnalysisResponseDTO;
import com.xiamen.metro.message.service.glm.AnalysisCacheService;
import com.xiamen.metro.message.service.glm.FallbackAnalysisService;
import com.xiamen.metro.message.service.glm.GlmApiClient;
import com.xiamen.metro.message.service.glm.MessageAnalysisService;
import com.xiamen.metro.message.service.glm.PromptTemplateManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * GLM模块集成测试
 * 测试各个组件之间的协作
 */
@ExtendWith(MockitoExtension.class)
class GlmIntegrationTest {

    @Mock
    private GlmApiClient glmApiClient;

    @Mock
    private AnalysisCacheService cacheService;

    private PromptTemplateManager promptTemplateManager;
    private FallbackAnalysisService fallbackAnalysisService;
    private MessageAnalysisService messageAnalysisService;

    @BeforeEach
    void setUp() {
        promptTemplateManager = new PromptTemplateManager();
        fallbackAnalysisService = new FallbackAnalysisService();
        messageAnalysisService = new MessageAnalysisService(
                glmApiClient,
                promptTemplateManager,
                cacheService,
                fallbackAnalysisService,
                new com.fasterxml.jackson.databind.ObjectMapper()
        );
    }

    @Test
    @DisplayName("完整的分析流程 - API可用且缓存未命中")
    void shouldCompleteFullAnalysisFlowWhenApiAvailable() {
        // 准备请求
        MessageAnalysisRequestDTO request = MessageAnalysisRequestDTO.builder()
                .messageContent("DEVICE_ID: DEV-001 STATUS: NORMAL TEMP: 25C TIMESTAMP: 2024-01-01T10:00:00")
                .messageType("STATUS")
                .deviceId("DEV-001")
                .timestamp(1704110400000L)
                .enableCache(true)
                .analysisDepth("DETAILED")
                .build();

        // 模拟缓存未命中
        when(cacheService.getCachedAnalysis(anyString(), anyString(), anyString()))
                .thenReturn(null);

        // 模拟API成功响应
        com.xiamen.metro.message.dto.glm.GlmResponseDTO apiResponse = createMockApiResponse();
        when(glmApiClient.analyzeMessage(anyString()))
                .thenReturn(Mono.just(apiResponse));

        // 执行分析
        MessageAnalysisResponseDTO result = messageAnalysisService.analyzeMessage(request);

        // 验证结果
        assertNotNull(result);
        assertEquals("SUCCESS", result.getStatus());
        assertNotNull(result.getAnalysisId());
        assertNotNull(result.getSummary());
        assertFalse(result.getFromCache());
        assertTrue(result.getProcessingTimeMs() > 0);
        assertNotNull(result.getAnalysisTime());

        // 验证交互
        verify(cacheService).getCachedAnalysis(anyString(), anyString(), anyString());
        verify(glmApiClient).analyzeMessage(anyString());
        verify(cacheService).cacheAnalysis(anyString(), anyString(), anyString(), any());
    }

    @Test
    @DisplayName("分析流程 - 缓存命中")
    void shouldUseCachedResultWhenAvailable() {
        // 准备请求
        MessageAnalysisRequestDTO request = MessageAnalysisRequestDTO.builder()
                .messageContent("Cached message content")
                .messageType("SYSTEM")
                .deviceId("DEV-002")
                .enableCache(true)
                .build();

        // 准备缓存响应
        MessageAnalysisResponseDTO cachedResponse = MessageAnalysisResponseDTO.builder()
                .analysisId("cached-001")
                .status("SUCCESS")
                .summary("缓存的分析结果")
                .confidenceScore(0.90)
                .fromCache(true)
                .analysisTime(LocalDateTime.now().minusMinutes(5))
                .build();

        // 模拟缓存命中
        when(cacheService.getCachedAnalysis(anyString(), anyString(), anyString()))
                .thenReturn(cachedResponse);

        // 执行分析
        MessageAnalysisResponseDTO result = messageAnalysisService.analyzeMessage(request);

        // 验证使用了缓存结果
        assertNotNull(result);
        assertEquals("cached-001", result.getAnalysisId());
        assertEquals("缓存的分析结果", result.getSummary());
        assertTrue(result.getFromCache());

        // 验证没有调用API
        verify(glmApiClient, never()).analyzeMessage(anyString());
        verify(cacheService, never()).cacheAnalysis(anyString(), anyString(), anyString(), any());
    }

    @Test
    @DisplayName("分析流程 - API失败时使用降级策略")
    void shouldUseFallbackWhenApiFails() {
        // 准备请求
        MessageAnalysisRequestDTO request = MessageAnalysisRequestDTO.builder()
                .messageContent("ERROR: Device failure occurred")
                .messageType("ERROR")
                .deviceId("DEV-003")
                .enableCache(true)
                .build();

        // 模拟缓存未命中
        when(cacheService.getCachedAnalysis(anyString(), anyString(), anyString()))
                .thenReturn(null);

        // 模拟API失败
        when(glmApiClient.analyzeMessage(anyString()))
                .thenReturn(Mono.error(new RuntimeException("API连接失败")));

        // 执行分析
        MessageAnalysisResponseDTO result = messageAnalysisService.analyzeMessage(request);

        // 验证降级结果
        assertNotNull(result);
        assertEquals("SUCCESS", result.getStatus()); // 降级分析成功
        assertTrue(result.getSummary().contains("ERROR"));
        assertTrue(result.getConfidenceScore() > 0.5); // 降级分析的置信度较低
        assertFalse(result.getFromCache());
        assertTrue(result.getErrorMessage().contains("降级分析服务"));

        // 验证交互
        verify(cacheService).getCachedAnalysis(anyString(), anyString(), anyString());
        verify(glmApiClient).analyzeMessage(anyString());
        verify(cacheService).cacheAnalysis(anyString(), anyString(), anyString(), any()); // 缓存降级结果
    }

    @Test
    @DisplayName("批量分析流程")
    void shouldAnalyzeBatchMessages() {
        // 准备批量请求
        List<MessageAnalysisRequestDTO> requests = Arrays.asList(
                MessageAnalysisRequestDTO.builder()
                        .messageContent("Message 1")
                        .messageType("STATUS")
                        .deviceId("DEV-001")
                        .build(),
                MessageAnalysisRequestDTO.builder()
                        .messageContent("Message 2")
                        .messageType("CONTROL")
                        .deviceId("DEV-002")
                        .build()
        );

        // 模拟缓存未命中和API响应
        when(cacheService.getCachedAnalysis(anyString(), anyString(), anyString()))
                .thenReturn(null);

        com.xiamen.metro.message.dto.glm.GlmResponseDTO apiResponse1 = createMockApiResponse();
        com.xiamen.metro.message.dto.glm.GlmResponseDTO apiResponse2 = createMockApiResponse();
        when(glmApiClient.analyzeMessage(anyString()))
                .thenReturn(Mono.just(apiResponse1))
                .thenReturn(Mono.just(apiResponse2));

        // 执行批量分析
        List<MessageAnalysisResponseDTO> results = messageAnalysisService.analyzeBatch(requests);

        // 验证结果
        assertNotNull(results);
        assertEquals(2, results.size());
        results.forEach(result -> {
            assertNotNull(result.getAnalysisId());
            assertEquals("SUCCESS", result.getStatus());
            assertFalse(result.getFromCache());
        });

        // 验证API调用次数
        verify(glmApiClient, times(2)).analyzeMessage(anyString());
    }

    @Test
    @DisplayName("健康检查流程")
    void shouldCheckHealthStatus() {
        // 模拟健康检查成功
        when(glmApiClient.healthCheck()).thenReturn(Mono.just(true));

        // 执行健康检查
        boolean isHealthy = messageAnalysisService.isHealthy();

        // 验证结果
        assertTrue(isHealthy);
        verify(glmApiClient).healthCheck();
    }

    @Test
    @DisplayName("健康检查 - API失败")
    void shouldHandleHealthCheckFailure() {
        // 模拟健康检查失败
        when(glmApiClient.healthCheck()).thenReturn(Mono.error(new RuntimeException("API不可用")));

        // 执行健康检查
        boolean isHealthy = messageAnalysisService.isHealthy();

        // 验证结果
        assertFalse(isHealthy);
        verify(glmApiClient).healthCheck();
    }

    @Test
    @DisplayName("缓存管理流程")
    void shouldManageCacheCorrectly() {
        // 清除缓存
        messageAnalysisService.clearAllCache();
        verify(cacheService).clearAllCache();

        // 清除特定缓存
        messageAnalysisService.clearCache("test message", "STATUS", "DETAILED");
        verify(cacheService).clearCache("test message", "STATUS", "DETAILED");

        // 获取缓存统计
        when(cacheService.getCacheStats()).thenReturn(
                AnalysisCacheService.CacheStats.builder()
                        .totalKeys(5L)
                        .estimatedMemoryUsage(5120L)
                        .build()
        );

        AnalysisCacheService.CacheStats stats = messageAnalysisService.getCacheStats();
        assertNotNull(stats);
        assertEquals(5L, stats.getTotalKeys());
        verify(cacheService).getCacheStats();
    }

    /**
     * 创建模拟的API响应
     */
    private com.xiamen.metro.message.dto.glm.GlmResponseDTO createMockApiResponse() {
        return com.xiamen.metro.message.dto.glm.GlmResponseDTO.builder()
                .id("resp-123")
                .object("chat.completion")
                .created(System.currentTimeMillis())
                .model("glm-4.6")
                .choices(Arrays.asList(
                        com.xiamen.metro.message.dto.glm.GlmResponseDTO.Choice.builder()
                                .index(0)
                                .message(com.xiamen.metro.message.dto.glm.GlmResponseDTO.Message.builder()
                                        .role("assistant")
                                        .content("{\"summary\":\"设备状态正常\",\"keyFields\":[\"DEVICE_ID\",\"STATUS\"],\"anomalies\":null,\"recommendations\":[\"继续监控设备状态\"]}")
                                        .build())
                                .finishReason("stop")
                                .build()
                ))
                .usage(com.xiamen.metro.message.dto.glm.GlmResponseDTO.Usage.builder()
                        .promptTokens(100)
                        .completionTokens(50)
                        .totalTokens(150)
                        .build())
                .build();
    }
}