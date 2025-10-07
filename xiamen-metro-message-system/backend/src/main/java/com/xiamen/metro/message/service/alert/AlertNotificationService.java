package com.xiamen.metro.message.service.alert;

import com.xiamen.metro.message.entity.AlertRecordEntity;
import com.xiamen.metro.message.entity.AlertNotificationEntity;
import com.xiamen.metro.message.dto.alert.AlertRecordDTO;
import com.xiamen.metro.message.repository.AlertNotificationRepository;
import com.xiamen.metro.message.repository.AlertRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import jakarta.mail.internet.MimeMessage;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * 告警通知服务
 * 负责邮件、短信、WebSocket等多种方式的通知发送
 *
 * @author Xiamen Metro System
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlertNotificationService {

    private final AlertNotificationRepository alertNotificationRepository;
    private final AlertRecordRepository alertRecordRepository;
    private final JavaMailSender javaMailSender;
    private final TemplateEngine templateEngine;
    private final WebSocketNotificationService webSocketNotificationService;
    private final Executor notificationExecutor;

    /**
     * 发送告警通知
     */
    @Transactional
    public void sendAlertNotifications(AlertRecordDTO alertDTO) {
        log.info("开始发送告警通知: {}", alertDTO.getAlertId());

        // 获取告警规则信息
        AlertRecordEntity alertRecord = alertRecordRepository.findByAlertId(alertDTO.getAlertId());
        if (alertRecord == null) {
            log.error("告警记录不存在: {}", alertDTO.getAlertId());
            return;
        }

        // 异步发送各种通知
        CompletableFuture.runAsync(() -> {
            try {
                sendEmailNotifications(alertDTO);
                sendWebSocketNotifications(alertDTO);
                // sendSmsNotifications(alertDTO); // 短信服务可选实现
            } catch (Exception e) {
                log.error("发送告警通知失败: {}", alertDTO.getAlertId(), e);
            }
        }, notificationExecutor);
    }

    /**
     * 发送邮件通知
     */
    private void sendEmailNotifications(AlertRecordDTO alertDTO) {
        try {
            AlertRecordEntity alertRecord = alertRecordRepository.findByAlertId(alertDTO.getAlertId());
            // 这里需要从规则中获取邮件接收人列表，简化实现
            List<String> recipients = getEmailRecipientsForAlert(alertDTO);

            if (recipients.isEmpty()) {
                log.debug("告警 {} 没有配置邮件接收人", alertDTO.getAlertId());
                return;
            }

            // 创建邮件通知记录
            for (String recipient : recipients) {
                AlertNotificationEntity notification = createNotificationRecord(
                        alertDTO, AlertNotificationEntity.NotificationType.EMAIL, recipient);

                try {
                    sendEmail(alertDTO, recipient);
                    notification.setStatus(AlertNotificationEntity.NotificationStatus.SUCCESS);
                    notification.setSendResult("邮件发送成功");
                    notification.setNotificationTime(LocalDateTime.now());

                    // 更新告警记录的邮件通知状态
                    alertRecord.setEmailNotified(true);
                    alertRecord.setEmailNotificationTime(LocalDateTime.now());

                    log.info("邮件通知发送成功: {} -> {}", alertDTO.getAlertId(), recipient);

                } catch (Exception e) {
                    notification.setStatus(AlertNotificationEntity.NotificationStatus.FAILED);
                    notification.setSendResult("邮件发送失败: " + e.getMessage());
                    log.error("邮件通知发送失败: {} -> {}", alertDTO.getAlertId(), recipient, e);
                }

                alertNotificationRepository.save(notification);
            }

            if (!recipients.isEmpty()) {
                alertRecordRepository.save(alertRecord);
            }

        } catch (Exception e) {
            log.error("发送邮件通知异常: {}", alertDTO.getAlertId(), e);
        }
    }

    /**
     * 发送单个邮件
     */
    private void sendEmail(AlertRecordDTO alertDTO, String recipient) {
        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(recipient);
            helper.setSubject(generateEmailSubject(alertDTO));
            helper.setText(generateEmailContent(alertDTO), true); // HTML格式

            javaMailSender.send(message);

        } catch (Exception e) {
            throw new RuntimeException("发送邮件失败", e);
        }
    }

    /**
     * 发送WebSocket通知
     */
    private void sendWebSocketNotifications(AlertRecordDTO alertDTO) {
        try {
            AlertRecordEntity alertRecord = alertRecordRepository.findByAlertId(alertDTO.getAlertId());

            // 创建WebSocket通知记录
            AlertNotificationEntity notification = createNotificationRecord(
                    alertDTO, AlertNotificationEntity.NotificationType.WEBSOCKET, "SYSTEM");

            try {
                webSocketNotificationService.sendAlertNotification(alertDTO);
                notification.setStatus(AlertNotificationEntity.NotificationStatus.SUCCESS);
                notification.setSendResult("WebSocket通知发送成功");
                notification.setNotificationTime(LocalDateTime.now());

                // 更新告警记录的WebSocket通知状态
                alertRecord.setWebsocketNotified(true);
                alertRecord.setWebsocketNotificationTime(LocalDateTime.now());

                alertRecordRepository.save(alertRecord);
                log.info("WebSocket通知发送成功: {}", alertDTO.getAlertId());

            } catch (Exception e) {
                notification.setStatus(AlertNotificationEntity.NotificationStatus.FAILED);
                notification.setSendResult("WebSocket通知发送失败: " + e.getMessage());
                log.error("WebSocket通知发送失败: {}", alertDTO.getAlertId(), e);
            }

            alertNotificationRepository.save(notification);

        } catch (Exception e) {
            log.error("发送WebSocket通知异常: {}", alertDTO.getAlertId(), e);
        }
    }

    /**
     * 生成邮件主题
     */
    private String generateEmailSubject(AlertRecordDTO alertDTO) {
        StringBuilder subject = new StringBuilder();
        subject.append("【厦门地铁告警】");
        subject.append("[").append(alertDTO.getAlertLevel().getDescription()).append("]");

        if (alertDTO.getDeviceId() != null) {
            subject.append(" 设备: ").append(alertDTO.getDeviceId());
        }

        subject.append(" ").append(alertDTO.getAlertTitle());

        return subject.toString();
    }

    /**
     * 生成邮件内容
     */
    private String generateEmailContent(AlertRecordDTO alertDTO) {
        try {
            Context context = new Context();
            context.setVariable("alert", alertDTO);
            context.setVariable("currentTime", LocalDateTime.now());

            return templateEngine.process("alert-email-template", context);
        } catch (Exception e) {
            log.error("生成邮件模板失败，使用简单格式", e);
            return generateSimpleEmailContent(alertDTO);
        }
    }

    /**
     * 生成简单邮件内容（模板引擎失败时的备用方案）
     */
    private String generateSimpleEmailContent(AlertRecordDTO alertDTO) {
        StringBuilder content = new StringBuilder();
        content.append("<html><body>");
        content.append("<h2>厦门地铁设备告警通知</h2>");
        content.append("<table border='1' cellpadding='5' cellspacing='0'>");
        content.append("<tr><td><b>告警ID</b></td><td>").append(alertDTO.getAlertId()).append("</td></tr>");
        content.append("<tr><td><b>告警级别</b></td><td>").append(alertDTO.getAlertLevel().getDescription()).append("</td></tr>");
        content.append("<tr><td><b>设备ID</b></td><td>").append(alertDTO.getDeviceId()).append("</td></tr>");
        content.append("<tr><td><b>告警标题</b></td><td>").append(alertDTO.getAlertTitle()).append("</td></tr>");
        content.append("<tr><td><b>告警内容</b></td><td>").append(alertDTO.getAlertContent()).append("</td></tr>");
        content.append("<tr><td><b>告警时间</b></td><td>").append(alertDTO.getAlertTime()).append("</td></tr>");

        if (alertDTO.getTriggeredValue() != null && alertDTO.getThresholdValue() != null) {
            content.append("<tr><td><b>当前值</b></td><td>").append(alertDTO.getTriggeredValue()).append("</td></tr>");
            content.append("<tr><td><b>阈值</b></td><td>").append(alertDTO.getThresholdValue()).append("</td></tr>");
        }

        if (alertDTO.getConfidenceScore() != null) {
            content.append("<tr><td><b>置信度</b></td><td>").append(String.format("%.1f%%", alertDTO.getConfidenceScore() * 100)).append("</td></tr>");
        }

        content.append("</table>");
        content.append("<p><b>请及时处理该告警！</b></p>");
        content.append("<hr>");
        content.append("<p><small>此邮件由厦门地铁设备报文分析系统自动发送，请勿回复。</small></p>");
        content.append("</body></html>");

        return content.toString();
    }

    /**
     * 创建通知记录
     */
    private AlertNotificationEntity createNotificationRecord(AlertRecordDTO alertDTO,
                                                           AlertNotificationEntity.NotificationType notificationType,
                                                           String recipient) {
        AlertNotificationEntity notification = new AlertNotificationEntity();
        notification.setAlertId(alertDTO.getAlertId());
        notification.setNotificationType(notificationType);
        notification.setRecipient(recipient);
        notification.setSubject(generateEmailSubject(alertDTO));
        notification.setContent(generateEmailContent(alertDTO));
        notification.setNotificationTime(LocalDateTime.now());
        notification.setStatus(AlertNotificationEntity.NotificationStatus.PENDING);
        notification.setRetryCount(0);

        return notification;
    }

    /**
     * 获取告警的邮件接收人列表
     */
    private List<String> getEmailRecipientsForAlert(AlertRecordDTO alertDTO) {
        // 这里应该从告警规则中获取配置的邮件接收人
        // 简化实现，返回默认接收人
        List<String> recipients = new ArrayList<>();

        // 根据告警级别添加不同的接收人
        switch (alertDTO.getAlertLevel()) {
            case CRITICAL:
                recipients.add("admin@xiamen-metro.com");
                recipients.add("maintenance@xiamen-metro.com");
                break;
            case WARNING:
                recipients.add("maintenance@xiamen-metro.com");
                break;
            case INFO:
                recipients.add("monitoring@xiamen-metro.com");
                break;
        }

        return recipients;
    }

    /**
     * 重试失败的通知
     */
    @Transactional
    public void retryFailedNotifications() {
        List<AlertNotificationEntity> failedNotifications = alertNotificationRepository
                .findNotificationsNeedingRetry(LocalDateTime.now());

        for (AlertNotificationEntity notification : failedNotifications) {
            retryNotification(notification);
        }
    }

    /**
     * 重试单个通知
     */
    private void retryNotification(AlertNotificationEntity notification) {
        try {
            notification.setStatus(AlertNotificationEntity.NotificationStatus.RETRY);
            notification.setRetryCount(notification.getRetryCount() + 1);
            notification.setLastRetryTime(LocalDateTime.now());

            // 根据通知类型重试
            switch (notification.getNotificationType()) {
                case EMAIL:
                    retryEmailNotification(notification);
                    break;
                case WEBSOCKET:
                    retryWebSocketNotification(notification);
                    break;
                default:
                    log.warn("不支持重试的通知类型: {}", notification.getNotificationType());
            }

            alertNotificationRepository.save(notification);

        } catch (Exception e) {
            log.error("重试通知失败: {}", notification.getId(), e);
            notification.setStatus(AlertNotificationEntity.NotificationStatus.FAILED);
            notification.setSendResult("重试失败: " + e.getMessage());
            alertNotificationRepository.save(notification);
        }
    }

    /**
     * 重试邮件通知
     */
    private void retryEmailNotification(AlertNotificationEntity notification) {
        // 实现邮件重试逻辑
        log.info("重试邮件通知: {}", notification.getId());
        // 这里应该重新发送邮件
    }

    /**
     * 重试WebSocket通知
     */
    private void retryWebSocketNotification(AlertNotificationEntity notification) {
        // 实现WebSocket重试逻辑
        log.info("重试WebSocket通知: {}", notification.getId());
        // 这里应该重新发送WebSocket通知
    }

    /**
     * 获取通知统计信息
     */
    public Map<String, Object> getNotificationStatistics(LocalDateTime since) {
        Map<String, Object> statistics = new HashMap<>();

        // 各类型通知数量
        List<Object[]> typeCounts = alertNotificationRepository.countNotificationsByType(since);
        Map<String, Long> typeStats = new HashMap<>();
        for (Object[] count : typeCounts) {
            typeStats.put(count[0].toString(), (Long) count[1]);
        }
        statistics.put("typeCounts", typeStats);

        // 各状态通知数量
        List<Object[]> statusCounts = alertNotificationRepository.countNotificationsByStatus(since);
        Map<String, Long> statusStats = new HashMap<>();
        for (Object[] count : statusCounts) {
            statusStats.put(count[0].toString(), (Long) count[1]);
        }
        statistics.put("statusCounts", statusStats);

        return statistics;
    }
}