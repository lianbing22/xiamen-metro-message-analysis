package com.xiamen.metro.message.dto;

import lombok.Data;

/**
 * 报文数据DTO
 *
 * @author Xiamen Metro System
 */
@Data
public class MessageDataDTO {

    /**
     * 设备ID
     */
    private String deviceId;

    /**
     * 时间戳
     */
    private String timestamp;

    /**
     * 消息类型
     */
    private String messageType;

    /**
     * 消息内容
     */
    private String messageContent;

    /**
     * 错误信息
     */
    private String errorMessage;
}