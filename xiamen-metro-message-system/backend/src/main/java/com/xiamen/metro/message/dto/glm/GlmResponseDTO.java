package com.xiamen.metro.message.dto.glm;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * GLM API响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GlmResponseDTO {

    /**
     * 请求ID
     */
    @JsonProperty("id")
    private String id;

    /**
     * 对象类型
     */
    @JsonProperty("object")
    private String object;

    /**
     * 创建时间戳
     */
    @JsonProperty("created")
    private Long created;

    /**
     * 模型名称
     */
    @JsonProperty("model")
    private String model;

    /**
     * 选择列表
     */
    @JsonProperty("choices")
    private List<Choice> choices;

    /**
     * 使用情况统计
     */
    @JsonProperty("usage")
    private Usage usage;

    /**
     * 错误信息（如果存在）
     */
    @JsonProperty("error")
    private ErrorInfo error;

    /**
     * 选择结果
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Choice {

        /**
         * 索引
         */
        @JsonProperty("index")
        private Integer index;

        /**
         * 消息
         */
        @JsonProperty("message")
        private Message message;

        /**
         * 完成原因
         */
        @JsonProperty("finish_reason")
        private String finishReason;
    }

    /**
     * 消息内容
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Message {

        /**
         * 角色
         */
        @JsonProperty("role")
        private String role;

        /**
         * 消息内容
         */
        @JsonProperty("content")
        private String content;
    }

    /**
     * Token使用统计
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Usage {

        /**
         * 提示词token数
         */
        @JsonProperty("prompt_tokens")
        private Integer promptTokens;

        /**
         * 完成token数
         */
        @JsonProperty("completion_tokens")
        private Integer completionTokens;

        /**
         * 总token数
         */
        @JsonProperty("total_tokens")
        private Integer totalTokens;
    }

    /**
     * 错误信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ErrorInfo {

        /**
         * 错误代码
         */
        @JsonProperty("code")
        private String code;

        /**
         * 错误消息
         */
        @JsonProperty("message")
        private String message;

        /**
         * 错误类型
         */
        @JsonProperty("type")
        private String type;
    }
}