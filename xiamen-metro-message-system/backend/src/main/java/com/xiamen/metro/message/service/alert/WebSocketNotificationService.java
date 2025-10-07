package com.xiamen.metro.message.service.alert;

import com.xiamen.metro.message.dto.alert.AlertRecordDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket通知服务
 * 负责实时推送告警信息到前端
 *
 * @author Xiamen Metro System
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WebSocketNotificationService {

    private final ObjectMapper objectMapper;
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    /**
     * 注册WebSocket会话
     */
    public void registerSession(String sessionId, WebSocketSession session) {
        sessions.put(sessionId, session);
        log.info("WebSocket会话已注册: {}", sessionId);
    }

    /**
     * 注销WebSocket会话
     */
    public void unregisterSession(String sessionId) {
        sessions.remove(sessionId);
        log.info("WebSocket会话已注销: {}", sessionId);
    }

    /**
     * 发送告警通知
     */
    public void sendAlertNotification(AlertRecordDTO alertDTO) {
        try {
            WebSocketMessage message = WebSocketMessage.builder()
                    .type("ALERT")
                    .timestamp(LocalDateTime.now())
                    .data(alertDTO)
                    .build();

            String messageJson = objectMapper.writeValueAsString(message);
            broadcastToAllSessions(messageJson);

            log.info("WebSocket告警通知已发送: {}", alertDTO.getAlertId());

        } catch (Exception e) {
            log.error("发送WebSocket告警通知失败: {}", alertDTO.getAlertId(), e);
        }
    }

    /**
     * 发送系统通知
     */
    public void sendSystemNotification(String title, String message) {
        try {
            WebSocketMessage systemMessage = WebSocketMessage.builder()
                    .type("SYSTEM_NOTIFICATION")
                    .timestamp(LocalDateTime.now())
                    .data(Map.of(
                            "title", title,
                            "message", message
                    ))
                    .build();

            String messageJson = objectMapper.writeValueAsString(systemMessage);
            broadcastToAllSessions(messageJson);

            log.info("WebSocket系统通知已发送: {}", title);

        } catch (Exception e) {
            log.error("发送WebSocket系统通知失败: {}", title, e);
        }
    }

    /**
     * 发送告警状态更新
     */
    public void sendAlertStatusUpdate(String alertId, String status, String updatedBy) {
        try {
            WebSocketMessage statusMessage = WebSocketMessage.builder()
                    .type("ALERT_STATUS_UPDATE")
                    .timestamp(LocalDateTime.now())
                    .data(Map.of(
                            "alertId", alertId,
                            "status", status,
                            "updatedBy", updatedBy,
                            "updateTime", LocalDateTime.now()
                    ))
                    .build();

            String messageJson = objectMapper.writeValueAsString(statusMessage);
            broadcastToAllSessions(messageJson);

            log.info("WebSocket状态更新已发送: {} -> {}", alertId, status);

        } catch (Exception e) {
            log.error("发送WebSocket状态更新失败: {}", alertId, e);
        }
    }

    /**
     * 发送心跳消息
     */
    public void sendHeartbeat() {
        try {
            WebSocketMessage heartbeat = WebSocketMessage.builder()
                    .type("HEARTBEAT")
                    .timestamp(LocalDateTime.now())
                    .data(Map.of("serverTime", LocalDateTime.now()))
                    .build();

            String messageJson = objectMapper.writeValueAsString(heartbeat);
            broadcastToAllSessions(messageJson);

        } catch (Exception e) {
            log.error("发送WebSocket心跳失败", e);
        }
    }

    /**
     * 向指定用户发送消息
     */
    public void sendToUser(String userId, Object data) {
        try {
            WebSocketMessage message = WebSocketMessage.builder()
                    .type("USER_MESSAGE")
                    .timestamp(LocalDateTime.now())
                    .data(data)
                    .build();

            String messageJson = objectMapper.writeValueAsString(message);
            broadcastToUserSessions(userId, messageJson);

        } catch (Exception e) {
            log.error("发送用户消息失败: {}", userId, e);
        }
    }

    /**
     * 广播消息到所有会话
     */
    private void broadcastToAllSessions(String message) {
        List<String> deadSessions = new ArrayList<>();

        sessions.forEach((sessionId, session) -> {
            try {
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage(message));
                } else {
                    deadSessions.add(sessionId);
                }
            } catch (IOException e) {
                log.warn("发送WebSocket消息失败，会话: {}", sessionId, e);
                deadSessions.add(sessionId);
            }
        });

        // 清理失效的会话
        deadSessions.forEach(this::unregisterSession);
    }

    /**
     * 广播消息到指定用户的所有会话
     */
    private void broadcastToUserSessions(String userId, String message) {
        // 这里应该根据用户ID筛选会话，简化实现，发送到所有会话
        broadcastToAllSessions(message);
    }

    /**
     * 获取当前连接数
     */
    public int getConnectionCount() {
        return (int) sessions.values().stream()
                .filter(WebSocketSession::isOpen)
                .count();
    }

    /**
     * 清理所有会话
     */
    public void clearAllSessions() {
        sessions.clear();
        log.info("所有WebSocket会话已清理");
    }

    /**
     * WebSocket消息包装类
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class WebSocketMessage {
        private String type;
        private LocalDateTime timestamp;
        private Object data;
    }
}