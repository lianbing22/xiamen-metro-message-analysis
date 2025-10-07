package com.xiamen.metro.message.config;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Base64;

/**
 * GLM-4.6 API配置类
 * 提供WebClient、限流器、重试器和加密工具
 */
@Configuration
public class GlmConfig {

    @Value("${glm.api.url:https://open.bigmodel.cn/api/coding/paas/v4}")
    private String apiUrl;

    @Value("${glm.api.key:77519fea6df4468ea8a0a0dceb1e9df4.mkATxCcEaNh30hy7}")
    private String encryptedApiKey;

    @Value("${glm.rate-limit.requests-per-minute:1000}")
    private int requestsPerMinute;

    @Value("${glm.retry.max-attempts:3}")
    private int maxRetryAttempts;

    @Value("${glm.encryption.key:xiamen-metro-glm-2024}")
    private String encryptionKey;

    /**
     * 配置GLM API专用的WebClient
     */
    @Bean("glmWebClient")
    public WebClient glmWebClient() {
        return WebClient.builder()
                .baseUrl(apiUrl)
                .defaultHeader("Authorization", "Bearer " + decryptApiKey())
                .defaultHeader("Content-Type", "application/json")
                .defaultHeader("Accept", "application/json")
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
                .build();
    }

    /**
     * GLM API限流器 - 1000次/分钟
     */
    @Bean("glmRateLimiter")
    public RateLimiter glmRateLimiter() {
        RateLimiterConfig config = RateLimiterConfig.custom()
                .limitForPeriod(requestsPerMinute)
                .limitRefreshPeriod(Duration.ofMinutes(1))
                .timeoutDuration(Duration.ofSeconds(5))
                .drainPermissionsOnResult(true)
                .build();

        return RateLimiter.of("glmRateLimiter", config);
    }

    /**
     * GLM API重试器 - 最多3次重试
     */
    @Bean("glmRetry")
    public Retry glmRetry() {
        RetryConfig config = RetryConfig.custom()
                .maxAttempts(maxRetryAttempts)
                .waitDuration(Duration.ofSeconds(2))
                .retryExceptions(Exception.class)
                .build();

        return Retry.of("glmRetry", config);
    }

    /**
     * 解密API密钥
     */
    private String decryptApiKey() {
        try {
            // 简化实现：在开发环境直接返回
            // 生产环境应该使用更安全的加密方案
            return encryptedApiKey;
        } catch (Exception e) {
            // 如果解密失败，返回原始值（开发环境）
            return encryptedApiKey;
        }
    }

    /**
     * 加密敏感信息
     */
    public String encrypt(String plainText) {
        try {
            // 简化实现：在生产环境应该使用真正的加密
            return plainText;
        } catch (Exception e) {
            throw new RuntimeException("加密失败", e);
        }
    }
}