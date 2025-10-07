package com.xiamen.metro.message.dto.glm;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * GLM API请求DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GlmRequestDTO {

    /**
     * 模型名称
     */
    @JsonProperty("model")
    private String model;

    /**
     * 消息列表
     */
    @JsonProperty("messages")
    private List<Message> messages;

    /**
     * 最大token数
     */
    @JsonProperty("max_tokens")
    private Integer maxTokens;

    /**
     * 温度参数，控制随机性
     */
    @JsonProperty("temperature")
    private Double temperature;

    /**
     * 采样策略
     */
    @JsonProperty("top_p")
    private Double topP;

    /**
     * 是否流式输出
     */
    @JsonProperty("stream")
    private Boolean stream;

    /**
     * 消息内容
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Message {

        /**
         * 角色：system, user, assistant
         */
        @JsonProperty("role")
        private String role;

        /**
         * 消息内容
         */
        @JsonProperty("content")
        private String content;
    }
}