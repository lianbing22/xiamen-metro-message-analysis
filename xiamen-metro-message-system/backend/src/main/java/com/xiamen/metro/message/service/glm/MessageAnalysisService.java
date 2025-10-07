package com.xiamen.metro.message.service.glm;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiamen.metro.message.dto.glm.GlmResponseDTO;
import com.xiamen.metro.message.dto.glm.MessageAnalysisRequestDTO;
import com.xiamen.metro.message.dto.glm.MessageAnalysisResponseDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * 报文分析服务
 * 整合GLM API、缓存和降级策略的主要服务类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessageAnalysisService {

    private final GlmApiClient glmApiClient;
    private final PromptTemplateManager promptTemplateManager;
    private final AnalysisCacheService cacheService;
    private final FallbackAnalysisService fallbackAnalysisService;
    private final ObjectMapper objectMapper;

    /**
     * 分析报文
     */
    public MessageAnalysisResponseDTO analyzeMessage(MessageAnalysisRequestDTO request) {
        long startTime = System.currentTimeMillis();
        String analysisId = UUID.randomUUID().toString();

        log.info("开始分析报文，ID: {}, 类型: {}, 设备: {}, 缓存: {}",
                analysisId, request.getMessageType(), request.getDeviceId(), request.getEnableCache());

        try {
            // 1. 检查缓存
            if (request.getEnableCache()) {
                MessageAnalysisResponseDTO cached = cacheService.getCachedAnalysis(
                        request.getMessageContent(),
                        request.getMessageType(),
                        request.getAnalysisDepth()
                );

                if (cached != null) {
                    log.info("使用缓存的分析结果，分析ID: {}", analysisId);
                    cached.setAnalysisId(analysisId);
                    cached.setProcessingTimeMs(System.currentTimeMillis() - startTime);
                    return cached;
                }
            }

            // 2. 尝试GLM API分析
            MessageAnalysisResponseDTO result = performGlmAnalysis(request, analysisId, startTime);

            // 3. 如果API分析失败，使用降级策略
            if (result == null || "FAILED".equals(result.getStatus())) {
                log.warn("GLM API分析失败，使用降级策略，分析ID: {}", analysisId);
                result = performFallbackAnalysis(request, analysisId, startTime);
            }

            // 4. 缓存结果
            if (request.getEnableCache() && result != null) {
                cacheService.cacheAnalysis(
                        request.getMessageContent(),
                        request.getMessageType(),
                        request.getAnalysisDepth(),
                        result
                );
            }

            return result;

        } catch (Exception e) {
            log.error("报文分析异常，分析ID: {}", analysisId, e);
            return performFallbackAnalysis(request, analysisId, startTime);
        }
    }

    /**
     * 执行GLM API分析
     */
    private MessageAnalysisResponseDTO performGlmAnalysis(MessageAnalysisRequestDTO request, String analysisId, long startTime) {
        try {
            // 构建提示词
            String prompt = buildPrompt(request);

            // 调用GLM API
            GlmResponseDTO glmResponse = glmApiClient.analyzeMessage(prompt).block();

            if (glmResponse != null && glmResponse.getChoices() != null && !glmResponse.getChoices().isEmpty()) {
                String content = glmResponse.getChoices().get(0).getMessage().getContent();
                return parseGlmResponse(content, analysisId, startTime, glmResponse);
            } else {
                log.warn("GLM API返回空响应，分析ID: {}", analysisId);
                return null;
            }

        } catch (Exception e) {
            log.error("GLM API调用失败，分析ID: {}", analysisId, e);
            return null;
        }
    }

    /**
     * 构建提示词
     */
    private String buildPrompt(MessageAnalysisRequestDTO request) {
        if (request.getContext() != null && !request.getContext().trim().isEmpty()) {
            return promptTemplateManager.getTemplateWithContext(
                    request.getMessageType(),
                    request.getMessageContent(),
                    request.getDeviceId(),
                    request.getTimestamp(),
                    request.getContext()
            );
        } else {
            return promptTemplateManager.getTemplate(
                    request.getMessageType(),
                    request.getMessageContent(),
                    request.getDeviceId(),
                    request.getTimestamp()
            );
        }
    }

    /**
     * 解析GLM响应
     */
    private MessageAnalysisResponseDTO parseGlmResponse(String content, String analysisId, long startTime, GlmResponseDTO glmResponse) {
        try {
            // 尝试解析JSON响应
            if (content.trim().startsWith("{")) {
                MessageAnalysisResponseDTO result = objectMapper.readValue(content, MessageAnalysisResponseDTO.class);
                result.setAnalysisId(analysisId);
                result.setProcessingTimeMs(System.currentTimeMillis() - startTime);
                result.setAnalysisTime(LocalDateTime.now());
                result.setFromCache(false);
                result.setRawResponse(content);

                // 设置置信度（基于token使用情况和响应质量）
                double confidence = calculateConfidence(glmResponse, content);
                result.setConfidenceScore(confidence);

                log.info("GLM分析完成，分析ID: {}, 置信度: {}, 耗时: {}ms",
                        analysisId, confidence, result.getProcessingTimeMs());

                return result;
            } else {
                // 如果不是JSON格式，创建基础响应
                return createBasicResponse(content, analysisId, startTime, glmResponse);
            }

        } catch (JsonProcessingException e) {
            log.warn("解析GLM响应JSON失败，使用基础响应格式，分析ID: {}", analysisId, e);
            return createBasicResponse(content, analysisId, startTime, glmResponse);
        }
    }

    /**
     * 计算置信度
     */
    private double calculateConfidence(GlmResponseDTO glmResponse, String content) {
        double confidence = 0.7; // 基础置信度

        // 基于token使用情况调整置信度
        if (glmResponse.getUsage() != null) {
            int totalTokens = glmResponse.getUsage().getTotalTokens();
            if (totalTokens > 500) {
                confidence += 0.1; // 详细的响应
            }
        }

        // 基于响应内容质量调整置信度
        if (content.contains("summary") && content.contains("keyFields")) {
            confidence += 0.1; // 包含必要的字段
        }

        if (content.length() > 200) {
            confidence += 0.1; // 响应内容详细
        }

        return Math.min(confidence, 1.0);
    }

    /**
     * 创建基础响应
     */
    private MessageAnalysisResponseDTO createBasicResponse(String content, String analysisId, long startTime, GlmResponseDTO glmResponse) {
        return MessageAnalysisResponseDTO.builder()
                .analysisId(analysisId)
                .status("SUCCESS")
                .summary(content.substring(0, Math.min(content.length(), 200)))
                .keyFields(Arrays.asList("GLM原始响应"))
                .confidenceScore(0.5)
                .processingTimeMs(System.currentTimeMillis() - startTime)
                .analysisTime(LocalDateTime.now())
                .fromCache(false)
                .rawResponse(content)
                .build();
    }

    /**
     * 执行降级分析
     */
    private MessageAnalysisResponseDTO performFallbackAnalysis(MessageAnalysisRequestDTO request, String analysisId, long startTime) {
        MessageAnalysisResponseDTO result = fallbackAnalysisService.performFallbackAnalysis(
                request.getMessageContent(),
                request.getMessageType(),
                request.getDeviceId()
        );

        result.setAnalysisId(analysisId);
        result.setProcessingTimeMs(System.currentTimeMillis() - startTime);

        log.info("降级分析完成，分析ID: {}, 耗时: {}ms", analysisId, result.getProcessingTimeMs());

        return result;
    }

    /**
     * 批量分析报文
     */
    public List<MessageAnalysisResponseDTO> analyzeBatch(List<MessageAnalysisRequestDTO> requests) {
        log.info("开始批量分析报文，数量: {}", requests.size());

        return requests.parallelStream()
                .map(this::analyzeMessage)
                .toList();
    }

    /**
     * 检查服务健康状态
     */
    public boolean isHealthy() {
        try {
            return glmApiClient.healthCheck().block();
        } catch (Exception e) {
            log.error("健康检查失败", e);
            return false;
        }
    }

    /**
     * 清除缓存
     */
    public void clearCache(String messageContent, String messageType, String analysisDepth) {
        cacheService.clearCache(messageContent, messageType, analysisDepth);
    }

    /**
     * 清除所有缓存
     */
    public void clearAllCache() {
        cacheService.clearAllCache();
    }

    /**
     * 获取缓存统计
     */
    public AnalysisCacheService.CacheStats getCacheStats() {
        return cacheService.getCacheStats();
    }

    /**
     * 获取API使用统计
     */
    public io.github.resilience4j.ratelimiter.RateLimiter.Metrics getApiMetrics() {
        return glmApiClient.getRateLimiterMetrics();
    }
}