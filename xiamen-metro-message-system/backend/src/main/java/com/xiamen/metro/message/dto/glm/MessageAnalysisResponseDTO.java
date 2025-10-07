package com.xiamen.metro.message.dto.glm;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 报文分析响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageAnalysisResponseDTO {

    /**
     * 分析ID
     */
    private String analysisId;

    /**
     * 分析结果状态：SUCCESS, FAILED, PARTIAL
     */
    private String status;

    /**
     * 报文摘要
     */
    private String summary;

    /**
     * 关键字段提取
     */
    private List<String> keyFields;

    /**
     * 异常检测结果
     */
    private List<AnomalyInfo> anomalies;

    /**
     * 建议操作
     */
    private List<String> recommendations;

    /**
     * 置信度评分 (0-1)
     */
    private Double confidenceScore;

    /**
     * 分析耗时（毫秒）
     */
    private Long processingTimeMs;

    /**
     * 分析时间
     */
    private LocalDateTime analysisTime;

    /**
     * 是否来自缓存
     */
    private Boolean fromCache;

    /**
     * 原始响应（调试用）
     */
    private String rawResponse;

    /**
     * 错误信息（如果有）
     */
    private String errorMessage;

    /**
     * 异常信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AnomalyInfo {

        /**
         * 异常类型
         */
        private String type;

        /**
         * 异常描述
         */
        private String description;

        /**
         * 严重程度：LOW, MEDIUM, HIGH, CRITICAL
         */
        private String severity;

        /**
         * 相关字段
         */
        private String relatedField;

        /**
         * 建议处理方式
         */
        private String suggestedAction;
    }
}