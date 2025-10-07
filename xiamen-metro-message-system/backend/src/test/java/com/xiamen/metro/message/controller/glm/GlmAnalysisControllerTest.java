package com.xiamen.metro.message.controller.glm;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiamen.metro.message.dto.glm.MessageAnalysisRequestDTO;
import com.xiamen.metro.message.dto.glm.MessageAnalysisResponseDTO;
import com.xiamen.metro.message.service.glm.AnalysisCacheService;
import com.xiamen.metro.message.service.glm.MessageAnalysisService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * GLM分析控制器测试
 */
@WebMvcTest(GlmAnalysisController.class)
class GlmAnalysisControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MessageAnalysisService messageAnalysisService;

    @MockBean
    private AnalysisCacheService cacheService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("单个报文分析 - 成功")
    void shouldAnalyzeSingleMessageSuccessfully() throws Exception {
        // 准备测试数据
        MessageAnalysisRequestDTO request = MessageAnalysisRequestDTO.builder()
                .messageContent("Device status: normal")
                .messageType("STATUS")
                .deviceId("DEV-001")
                .timestamp(System.currentTimeMillis())
                .enableCache(true)
                .analysisDepth("DETAILED")
                .build();

        MessageAnalysisResponseDTO response = MessageAnalysisResponseDTO.builder()
                .analysisId("test-001")
                .status("SUCCESS")
                .summary("设备状态正常")
                .confidenceScore(0.95)
                .processingTimeMs(150L)
                .analysisTime(LocalDateTime.now())
                .fromCache(false)
                .build();

        when(messageAnalysisService.analyzeMessage(any(MessageAnalysisRequestDTO.class)))
                .thenReturn(response);

        // 执行请求
        mockMvc.perform(post("/api/v1/glm/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.analysisId").value("test-001"))
                .andExpect(jsonPath("$.data.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.confidenceScore").value(0.95))
                .andExpect(jsonPath("$.data.fromCache").value(false));
    }

    @Test
    @DisplayName("单个报文分析 - 请求验证失败")
    void shouldFailValidationForSingleMessage() throws Exception {
        MessageAnalysisRequestDTO request = MessageAnalysisRequestDTO.builder()
                .messageContent("") // 空内容，应该验证失败
                .messageType("") // 空类型，应该验证失败
                .build();

        mockMvc.perform(post("/api/v1/glm/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("批量报文分析 - 成功")
    void shouldAnalyzeBatchMessagesSuccessfully() throws Exception {
        List<MessageAnalysisRequestDTO> requests = Arrays.asList(
                MessageAnalysisRequestDTO.builder()
                        .messageContent("Message 1")
                        .messageType("STATUS")
                        .deviceId("DEV-001")
                        .build(),
                MessageAnalysisRequestDTO.builder()
                        .messageContent("Message 2")
                        .messageType("ERROR")
                        .deviceId("DEV-002")
                        .build()
        );

        List<MessageAnalysisResponseDTO> responses = Arrays.asList(
                MessageAnalysisResponseDTO.builder()
                        .analysisId("test-001")
                        .status("SUCCESS")
                        .summary("消息1分析结果")
                        .build(),
                MessageAnalysisResponseDTO.builder()
                        .analysisId("test-002")
                        .status("SUCCESS")
                        .summary("消息2分析结果")
                        .build()
        );

        when(messageAnalysisService.analyzeBatch(any()))
                .thenReturn(responses);

        mockMvc.perform(post("/api/v1/glm/analyze/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requests)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].analysisId").value("test-001"))
                .andExpect(jsonPath("$.data[1].analysisId").value("test-002"));
    }

    @Test
    @DisplayName("批量报文分析 - 超过限制")
    void shouldFailBatchAnalysisWhenExceedingLimit() throws Exception {
        // 创建超过100个请求的列表
        MessageAnalysisRequestDTO[] requests = new MessageAnalysisRequestDTO[101];
        for (int i = 0; i < 101; i++) {
            requests[i] = MessageAnalysisRequestDTO.builder()
                    .messageContent("Message " + i)
                    .messageType("STATUS")
                    .deviceId("DEV-" + String.format("%03d", i))
                    .build();
        }

        mockMvc.perform(post("/api/v1/glm/analyze/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requests)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("批量分析数量不能超过100"));
    }

    @Test
    @DisplayName("健康检查 - 健康状态")
    void shouldReturnHealthyStatus() throws Exception {
        when(messageAnalysisService.isHealthy()).thenReturn(true);
        when(cacheService.getCacheStats()).thenReturn(AnalysisCacheService.CacheStats.builder()
                .totalKeys(10L)
                .estimatedMemoryUsage(10240L)
                .build());
        when(messageAnalysisService.getApiMetrics()).thenReturn(
                io.github.resilience4j.ratelimiter.RateLimiter.Metrics.empty());

        mockMvc.perform(get("/api/v1/glm/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.healthy").value(true))
                .andExpect(jsonPath("$.data.cacheKeys").value(10));
    }

    @Test
    @DisplayName("健康检查 - 异常状态")
    void shouldReturnUnhealthyStatus() throws Exception {
        when(messageAnalysisService.isHealthy()).thenReturn(false);
        when(cacheService.getCacheStats()).thenReturn(AnalysisCacheService.CacheStats.builder()
                .totalKeys(0L)
                .estimatedMemoryUsage(0L)
                .build());
        when(messageAnalysisService.getApiMetrics()).thenReturn(
                io.github.resilience4j.ratelimiter.RateLimiter.Metrics.empty());

        mockMvc.perform(get("/api/v1/glm/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.healthy").value(false))
                .andExpect(jsonPath("$.message").value("GLM服务状态: 异常"));
    }

    @Test
    @DisplayName("清除所有缓存")
    void shouldClearAllCache() throws Exception {
        mockMvc.perform(delete("/api/v1/glm/cache"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value("所有缓存已清除"));
    }

    @Test
    @DisplayName("清除指定缓存")
    void shouldClearSpecificCache() throws Exception {
        mockMvc.perform(delete("/api/v1/glm/cache")
                        .param("messageContent", "test message")
                        .param("messageType", "STATUS")
                        .param("analysisDepth", "DETAILED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value("指定缓存已清除"));
    }

    @Test
    @DisplayName("获取缓存统计")
    void shouldGetCacheStatistics() throws Exception {
        AnalysisCacheService.CacheStats stats = AnalysisCacheService.CacheStats.builder()
                .totalKeys(25L)
                .estimatedMemoryUsage(25600L)
                .build();

        when(cacheService.getCacheStats()).thenReturn(stats);

        mockMvc.perform(get("/api/v1/glm/cache/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalKeys").value(25))
                .andExpect(jsonPath("$.data.estimatedMemoryUsage").value(25600));
    }

    @Test
    @DisplayName("API连接测试 - 成功")
    void shouldTestApiConnectionSuccessfully() throws Exception {
        MessageAnalysisResponseDTO testResponse = MessageAnalysisResponseDTO.builder()
                .analysisId("test-123")
                .status("SUCCESS")
                .summary("API连接测试成功")
                .confidenceScore(0.98)
                .fromCache(false)
                .build();

        when(messageAnalysisService.analyzeMessage(any(MessageAnalysisRequestDTO.class)))
                .thenReturn(testResponse);

        mockMvc.perform(post("/api/v1/glm/test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.success").value(true))
                .andExpect(jsonPath("$.data.confidenceScore").value(0.98))
                .andExpect(jsonPath("$.data.fromCache").value(false))
                .andExpect(jsonPath("$.data.status").value("SUCCESS"));
    }

    @Test
    @DisplayName("API连接测试 - 失败")
    void shouldHandleApiTestFailure() throws Exception {
        when(messageAnalysisService.analyzeMessage(any(MessageAnalysisRequestDTO.class)))
                .thenThrow(new RuntimeException("API连接失败"));

        mockMvc.perform(post("/api/v1/glm/test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.success").value(false))
                .andExpect(jsonPath("$.data.status").value("FAILED"))
                .andExpect(jsonPath("$.message").value("API测试失败: API连接失败"));
    }
}