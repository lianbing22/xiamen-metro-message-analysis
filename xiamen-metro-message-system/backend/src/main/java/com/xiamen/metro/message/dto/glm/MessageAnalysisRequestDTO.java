package com.xiamen.metro.message.dto.glm;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 报文分析请求DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageAnalysisRequestDTO {

    /**
     * 报文内容
     */
    @NotBlank(message = "报文内容不能为空")
    private String messageContent;

    /**
     * 报文类型：SYSTEM, DEVICE, CONTROL, STATUS, ERROR
     */
    @NotBlank(message = "报文类型不能为空")
    private String messageType;

    /**
     * 设备标识
     */
    private String deviceId;

    /**
     * 时间戳
     */
    private Long timestamp;

    /**
     * 是否启用缓存
     */
    @Builder.Default
    private Boolean enableCache = true;

    /**
     * 分析深度：BASIC, DETAILED, EXPERT
     */
    @Builder.Default
    private String analysisDepth = "DETAILED";

    /**
     * 附加上下文信息
     */
    private String context;
}