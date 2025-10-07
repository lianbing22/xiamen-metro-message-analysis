package com.xiamen.metro.message.service.glm;

import com.xiamen.metro.message.dto.glm.GlmRequestDTO;
import com.xiamen.metro.message.dto.glm.GlmResponseDTO;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.retry.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

/**
 * GLM API客户端服务
 * 负责与GLM-4.6模型的API交互
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GlmApiClient {

    @Qualifier("glmWebClient")
    private final WebClient webClient;

    @Qualifier("glmRateLimiter")
    private final RateLimiter rateLimiter;

    @Qualifier("glmRetry")
    private final Retry retry;

    /**
     * 调用GLM API进行分析
     */
    public Mono<GlmResponseDTO> analyzeMessage(String prompt) {
        return RateLimiter.decorateMono(rateLimiter, () -> {
            GlmRequestDTO request = buildRequest(prompt);

            return Retry.decorateMono(retry, () -> {
                log.debug("调用GLM API进行报文分析，提示词长度: {}", prompt.length());

                return webClient.post()
                        .bodyValue(request)
                        .retrieve()
                        .bodyToMono(GlmResponseDTO.class)
                        .timeout(Duration.ofSeconds(30))
                        .doOnSuccess(response -> {
                            log.info("GLM API调用成功，响应ID: {}, 消耗Token: {}",
                                response.getId(),
                                response.getUsage() != null ? response.getUsage().getTotalTokens() : 0);
                        })
                        .doOnError(error -> {
                            log.error("GLM API调用失败: {}", error.getMessage());
                            if (error instanceof WebClientResponseException webEx) {
                                log.error("HTTP状态码: {}, 响应体: {}", webEx.getStatusCode(), webEx.getResponseBodyAsString());
                            }
                        });
            }).get();
        }).get();
    }

    /**
     * 构建GLM请求
     */
    private GlmRequestDTO buildRequest(String prompt) {
        List<GlmRequestDTO.Message> messages = Arrays.asList(
            GlmRequestDTO.Message.builder()
                .role("system")
                .content("你是厦门地铁报文分析专家，请专业、准确地分析提供的设备报文，返回JSON格式的分析结果。")
                .build(),
            GlmRequestDTO.Message.builder()
                .role("user")
                .content(prompt)
                .build()
        );

        return GlmRequestDTO.builder()
                .model("glm-4.6")
                .messages(messages)
                .maxTokens(2000)
                .temperature(0.3)  // 较低的温度以确保结果一致性
                .topP(0.9)
                .stream(false)
                .build();
    }

    /**
     * 检查API健康状态
     */
    public Mono<Boolean> healthCheck() {
        String simplePrompt = "请回答：你好";
        GlmRequestDTO request = GlmRequestDTO.builder()
                .model("glm-4.6")
                .messages(Arrays.asList(
                    GlmRequestDTO.Message.builder()
                        .role("user")
                        .content(simplePrompt)
                        .build()
                ))
                .maxTokens(50)
                .temperature(0.1)
                .build();

        return webClient.post()
                .bodyValue(request)
                .retrieve()
                .toBodilessEntity()
                .map(response -> response.getStatusCode().is2xxSuccessful())
                .timeout(Duration.ofSeconds(10))
                .onErrorReturn(false)
                .doOnSuccess(isHealthy -> {
                    if (isHealthy) {
                        log.info("GLM API健康检查通过");
                    } else {
                        log.warn("GLM API健康检查失败");
                    }
                });
    }

    /**
     * 获取API使用统计信息
     */
    public RateLimiter.Metrics getRateLimiterMetrics() {
        return rateLimiter.getMetrics();
    }
}