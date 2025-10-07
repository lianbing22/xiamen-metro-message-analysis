package com.xiamen.metro.message.service.glm;

import com.xiamen.metro.message.dto.glm.MessageAnalysisResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 分析缓存服务测试
 */
class AnalysisCacheServiceTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    private AnalysisCacheService cacheService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        cacheService = new AnalysisCacheService(redisTemplate, new com.fasterxml.jackson.databind.ObjectMapper());
    }

    @Test
    @DisplayName("缓存和获取分析结果")
    void shouldCacheAndGetAnalysisResult() {
        // 准备测试数据
        String messageContent = "Test message content";
        String messageType = "STATUS";
        String analysisDepth = "DETAILED";

        MessageAnalysisResponseDTO originalResult = MessageAnalysisResponseDTO.builder()
                .analysisId("test-001")
                .status("SUCCESS")
                .summary("测试摘要")
                .keyFields(Arrays.asList("field1", "field2"))
                .confidenceScore(0.85)
                .analysisTime(LocalDateTime.now())
                .fromCache(false)
                .build();

        // 模拟缓存行为
        when(valueOperations.get(anyString())).thenReturn(null);
        when(redisTemplate.delete(anyString())).thenReturn(true);
        when(redisTemplate.keys(anyString())).thenReturn(Set.of("glm:analysis:test1", "glm:analysis:test2"));
        when(redisTemplate.delete(any())).thenReturn(2L);

        // 缓存结果
        cacheService.cacheAnalysis(messageContent, messageType, analysisDepth, originalResult);

        // 验证缓存操作被调用
        verify(valueOperations).set(anyString(), anyString(), any());

        // 测试获取缓存（由于是模拟，这里直接测试空缓存的情况）
        MessageAnalysisResponseDTO cached = cacheService.getCachedAnalysis(messageContent, messageType, analysisDepth);
        assertNull(cached); // 因为模拟返回null
    }

    @Test
    @DisplayName("清除指定缓存")
    void shouldClearSpecificCache() {
        String messageContent = "Test message";
        String messageType = "STATUS";
        String analysisDepth = "DETAILED";

        when(redisTemplate.delete(anyString())).thenReturn(true);

        cacheService.clearCache(messageContent, messageType, analysisDepth);

        verify(redisTemplate).delete(contains("glm:analysis:"));
    }

    @Test
    @DisplayName("清除所有缓存")
    void shouldClearAllCache() {
        when(redisTemplate.keys("glm:analysis:*"))
                .thenReturn(Set.of("glm:analysis:key1", "glm:analysis:key2"));
        when(redisTemplate.delete(any())).thenReturn(2L);

        cacheService.clearAllCache();

        verify(redisTemplate).keys("glm:analysis:*");
        verify(redisTemplate).delete(any());
    }

    @Test
    @DisplayName("获取缓存统计")
    void shouldGetCacheStats() {
        when(redisTemplate.keys("glm:analysis:*"))
                .thenReturn(Set.of("glm:analysis:key1", "glm:analysis:key2", "glm:analysis:key3"));

        AnalysisCacheService.CacheStats stats = cacheService.getCacheStats();

        assertNotNull(stats);
        assertEquals(3L, stats.getTotalKeys());
        assertEquals(3072L, stats.getEstimatedMemoryUsage()); // 3 * 1024
    }

    @Test
    @DisplayName("处理缓存键生成")
    void shouldGenerateCacheKeyConsistently() {
        String messageContent = "Same message content";
        String messageType = "SYSTEM";
        String analysisDepth = "BASIC";

        // 多次调用应该生成相同的缓存键
        String key1 = callPrivateMethod("generateCacheKey", messageContent, messageType, analysisDepth);
        String key2 = callPrivateMethod("generateCacheKey", messageContent, messageType, analysisDepth);

        assertEquals(key1, key2);
    }

    @Test
    @DisplayName("处理不同内容生成不同缓存键")
    void shouldGenerateDifferentCacheKeysForDifferentContent() {
        String messageContent1 = "Message 1";
        String messageContent2 = "Message 2";
        String messageType = "STATUS";
        String analysisDepth = "DETAILED";

        String key1 = callPrivateMethod("generateCacheKey", messageContent1, messageType, analysisDepth);
        String key2 = callPrivateMethod("generateCacheKey", messageContent2, messageType, analysisDepth);

        assertNotEquals(key1, key2);
    }

    // 辅助方法：调用私有方法
    @SuppressWarnings("unchecked")
    private String callPrivateMethod(String methodName, Object... args) {
        try {
            var method = AnalysisCacheService.class.getDeclaredMethod(methodName, String.class, String.class, String.class);
            method.setAccessible(true);
            return (String) method.invoke(cacheService, args);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}