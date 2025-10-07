package com.xiamen.metro.message.handler;

import com.xiamen.metro.message.service.alert.WebSocketNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 告警WebSocket处理器
 *
 * @author Xiamen Metro System
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AlertWebSocketHandler implements WebSocketHandler {

    private final WebSocketNotificationService webSocketNotificationService;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String sessionId = generateSessionId(session);
        webSocketNotificationService.registerSession(sessionId, session);
        log.info("WebSocket连接已建立: {}", sessionId);
    }

    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
        String sessionId = generateSessionId(session);
        String payload = message.getPayload().toString();

        log.debug("收到WebSocket消息: {} -> {}", sessionId, payload);

        // 处理客户端消息
        handleClientMessage(sessionId, payload);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        String sessionId = generateSessionId(session);
        log.error("WebSocket传输错误: {}", sessionId, exception);
        webSocketNotificationService.unregisterSession(sessionId);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
        String sessionId = generateSessionId(session);
        log.info("WebSocket连接已关闭: {} - {}", sessionId, closeStatus);
        webSocketNotificationService.unregisterSession(sessionId);
    }

    @Override
    public boolean supportsPartialMessages() {
        return false;
    }

    /**
     * 处理客户端消息
     */
    private void handleClientMessage(String sessionId, String payload) {
        try {
            // 解析客户端消息
            if ("ping".equalsIgnoreCase(payload)) {
                // 响应ping消息
                webSocketNotificationService.sendToUser(sessionId, Map.of(
                        "type", "pong",
                        "timestamp", LocalDateTime.now()
                ));
            } else if ("subscribe".equalsIgnoreCase(payload)) {
                // 客户端订阅告警通知
                log.info("客户端订阅告警通知: {}", sessionId);
                webSocketNotificationService.sendToUser(sessionId, Map.of(
                        "type", "subscription_confirmed",
                        "message", "已订阅告警通知"
                ));
            } else {
                log.debug("未知消息类型: {}", payload);
            }

        } catch (Exception e) {
            log.error("处理客户端消息失败: {}", sessionId, e);
        }
    }

    /**
     * 生成会话ID
     */
    private String generateSessionId(WebSocketSession session) {
        return session.getId() + "_" + UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * 定期发送心跳消息
     */
    @Scheduled(fixedRate = 30000) // 每30秒发送一次心跳
    public void sendHeartbeat() {
        try {
            webSocketNotificationService.sendHeartbeat();
        } catch (Exception e) {
            log.error("发送心跳消息失败", e);
        }
    }
}