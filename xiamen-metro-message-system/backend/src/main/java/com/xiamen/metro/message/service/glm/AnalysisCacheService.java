package com.xiamen.metro.message.service.glm;

import com.xiamen.metro.message.dto.glm.MessageAnalysisResponseDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;

/**
 * 分析结果缓存服务
 * 使用Redis缓存GLM分析结果以提高响应速度和降低API调用成本
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AnalysisCacheService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String CACHE_PREFIX = "glm:analysis:";
    private static final Duration DEFAULT_CACHE_TTL = Duration.ofHours(24);
    private static final Duration ERROR_CACHE_TTL = Duration.ofMinutes(30);

    /**
     * 生成缓存键
     */
    private String generateCacheKey(String messageContent, String messageType, String analysisDepth) {
        try {
            // 使用MD5生成基于内容的哈希键
            String content = String.format("%s:%s:%s", messageContent, messageType, analysisDepth);
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(content.getBytes());
            String hashHex = HexFormat.of().formatHex(hash);

            return CACHE_PREFIX + hashHex;
        } catch (Exception e) {
            log.error("生成缓存键失败", e);
            return CACHE_PREFIX + System.currentTimeMillis();
        }
    }

    /**
     * 获取缓存的分析结果
     */
    public MessageAnalysisResponseDTO getCachedAnalysis(String messageContent, String messageType, String analysisDepth) {
        try {
            String cacheKey = generateCacheKey(messageContent, messageType, analysisDepth);
            Object cached = redisTemplate.opsForValue().get(cacheKey);

            if (cached != null) {
                log.debug("从缓存获取分析结果，键: {}", cacheKey);
                String json = cached.toString();
                MessageAnalysisResponseDTO result = objectMapper.readValue(json, MessageAnalysisResponseDTO.class);
                result.setFromCache(true);
                return result;
            }
        } catch (Exception e) {
            log.error("获取缓存分析结果失败", e);
        }

        return null;
    }

    /**
     * 缓存分析结果
     */
    public void cacheAnalysis(String messageContent, String messageType, String analysisDepth,
                             MessageAnalysisResponseDTO result) {
        try {
            String cacheKey = generateCacheKey(messageContent, messageType, analysisDepth);

            // 设置缓存标识
            result.setFromCache(false);

            // 序列化结果
            String json = objectMapper.writeValueAsString(result);

            // 根据分析结果状态设置不同的TTL
            Duration ttl = "SUCCESS".equals(result.getStatus()) ? DEFAULT_CACHE_TTL : ERROR_CACHE_TTL;

            redisTemplate.opsForValue().set(cacheKey, json, ttl);
            log.debug("缓存分析结果，键: {}, TTL: {}小时", cacheKey, ttl.toHours());

        } catch (Exception e) {
            log.error("缓存分析结果失败", e);
        }
    }

    /**
     * 清除指定内容的缓存
     */
    public void clearCache(String messageContent, String messageType, String analysisDepth) {
        try {
            String cacheKey = generateCacheKey(messageContent, messageType, analysisDepth);
            Boolean deleted = redisTemplate.delete(cacheKey);
            if (Boolean.TRUE.equals(deleted)) {
                log.info("清除缓存成功，键: {}", cacheKey);
            }
        } catch (Exception e) {
            log.error("清除缓存失败", e);
        }
    }

    /**
     * 清除所有分析缓存
     */
    public void clearAllCache() {
        try {
            var keys = redisTemplate.keys(CACHE_PREFIX + "*");
            if (keys != null && !keys.isEmpty()) {
                Long deleted = redisTemplate.delete(keys);
                log.info("清除所有分析缓存，删除了 {} 个键", deleted);
            }
        } catch (Exception e) {
            log.error("清除所有缓存失败", e);
        }
    }

    /**
     * 获取缓存统计信息
     */
    public CacheStats getCacheStats() {
        try {
            var keys = redisTemplate.keys(CACHE_PREFIX + "*");
            long keyCount = keys != null ? keys.size() : 0;

            // 估算内存使用（粗略计算）
            long estimatedMemoryUsage = keyCount * 1024; // 假设每个缓存项约1KB

            return CacheStats.builder()
                    .totalKeys(keyCount)
                    .estimatedMemoryUsage(estimatedMemoryUsage)
                    .build();
        } catch (Exception e) {
            log.error("获取缓存统计失败", e);
            return CacheStats.builder().totalKeys(0L).estimatedMemoryUsage(0L).build();
        }
    }

    /**
     * 缓存统计信息
     */
    @lombok.Data
    @lombok.Builder
    public static class CacheStats {
        private Long totalKeys;
        private Long estimatedMemoryUsage;
    }
}