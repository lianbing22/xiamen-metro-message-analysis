package com.xiamen.metro.message.controller.alert;

import com.xiamen.metro.message.dto.alert.AlertRecordDTO;
import com.xiamen.metro.message.dto.alert.AlertRuleDTO;
import com.xiamen.metro.message.service.alert.AlertManagementService;
import com.xiamen.metro.message.service.alert.AlertNotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 告警控制器
 *
 * @author Xiamen Metro System
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/alerts")
@RequiredArgsConstructor
@Tag(name = "告警管理", description = "告警相关API")
public class AlertController {

    private final AlertManagementService alertManagementService;
    private final AlertNotificationService alertNotificationService;

    /**
     * 获取活跃告警列表
     */
    @GetMapping("/active")
    @Operation(summary = "获取活跃告警", description = "获取指定设备的活跃告警列表")
    @PreAuthorize("hasRole('OPERATOR') or hasRole('ADMIN')")
    public ResponseEntity<List<AlertRecordDTO>> getActiveAlerts(
            @Parameter(description = "设备ID") @RequestParam(required = false) String deviceId) {

        List<AlertRecordDTO> alerts = alertManagementService.getActiveAlerts(deviceId);
        return ResponseEntity.ok(alerts);
    }

    /**
     * 确认告警
     */
    @PostMapping("/{alertId}/acknowledge")
    @Operation(summary = "确认告警", description = "确认指定的告警")
    @PreAuthorize("hasRole('OPERATOR') or hasRole('ADMIN')")
    public ResponseEntity<AlertRecordDTO> acknowledgeAlert(
            @Parameter(description = "告警ID") @PathVariable String alertId,
            @Valid @RequestBody AcknowledgeRequest request) {

        AlertRecordDTO alert = alertManagementService.acknowledgeAlert(
                alertId, request.getConfirmedBy(), request.getNote());

        log.info("告警已确认: {} by {}", alertId, request.getConfirmedBy());
        return ResponseEntity.ok(alert);
    }

    /**
     * 处理告警
     */
    @PostMapping("/{alertId}/resolve")
    @Operation(summary = "处理告警", description = "标记告警为已处理")
    @PreAuthorize("hasRole('OPERATOR') or hasRole('ADMIN')")
    public ResponseEntity<AlertRecordDTO> resolveAlert(
            @Parameter(description = "告警ID") @PathVariable String alertId,
            @Valid @RequestBody ResolveRequest request) {

        AlertRecordDTO alert = alertManagementService.resolveAlert(
                alertId, request.getResolvedBy(), request.getResolutionNote());

        log.info("告警已处理: {} by {}", alertId, request.getResolvedBy());
        return ResponseEntity.ok(alert);
    }

    /**
     * 标记为误报
     */
    @PostMapping("/{alertId}/false-positive")
    @Operation(summary = "标记误报", description = "将告警标记为误报")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AlertRecordDTO> markAsFalsePositive(
            @Parameter(description = "告警ID") @PathVariable String alertId,
            @Valid @RequestBody FalsePositiveRequest request) {

        AlertRecordDTO alert = alertManagementService.markAsFalsePositive(
                alertId, request.getMarkedBy(), request.getNote());

        log.info("告警已标记为误报: {} by {}", alertId, request.getMarkedBy());
        return ResponseEntity.ok(alert);
    }

    /**
     * 获取告警统计信息
     */
    @GetMapping("/statistics")
    @Operation(summary = "告警统计", description = "获取告警统计信息")
    @PreAuthorize("hasRole('USER') or hasRole('OPERATOR') or hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getAlertStatistics(
            @Parameter(description = "设备ID") @RequestParam(required = false) String deviceId,
            @Parameter(description = "开始时间") @RequestParam(required = false) LocalDateTime since) {

        if (since == null) {
            since = LocalDateTime.now().minusDays(7); // 默认最近7天
        }

        Map<String, Object> statistics = alertManagementService.getAlertStatistics(deviceId, since);
        return ResponseEntity.ok(statistics);
    }

    /**
     * 获取通知统计信息
     */
    @GetMapping("/notifications/statistics")
    @Operation(summary = "通知统计", description = "获取通知发送统计信息")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getNotificationStatistics(
            @Parameter(description = "开始时间") @RequestParam(required = false) LocalDateTime since) {

        if (since == null) {
            since = LocalDateTime.now().minusDays(7); // 默认最近7天
        }

        Map<String, Object> statistics = alertNotificationService.getNotificationStatistics(since);
        return ResponseEntity.ok(statistics);
    }

    /**
     * 手动重试失败的通知
     */
    @PostMapping("/notifications/retry")
    @Operation(summary = "重试通知", description = "手动重试发送失败的通知")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> retryFailedNotifications() {
        alertNotificationService.retryFailedNotifications();
        return ResponseEntity.ok("通知重试任务已启动");
    }

    /**
     * 触发测试告警
     */
    @PostMapping("/test")
    @Operation(summary = "测试告警", description = "触发一个测试告警")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> triggerTestAlert(@Valid @RequestBody TestAlertRequest request) {
        try {
            // 创建测试告警
            AlertRecordDTO testAlert = AlertRecordDTO.builder()
                    .alertId("TEST_" + System.currentTimeMillis())
                    .deviceId(request.getDeviceId())
                    .alertLevel(request.getAlertLevel())
                    .alertTitle("[测试] " + request.getTitle())
                    .alertContent(request.getContent())
                    .alertTime(LocalDateTime.now())
                    .status(AlertRecordDTO.AlertStatus.ACTIVE)
                    .build();

            // 发送通知
            alertNotificationService.sendAlertNotifications(testAlert);

            log.info("测试告警已触发: {}", testAlert.getAlertId());
            return ResponseEntity.ok("测试告警已触发: " + testAlert.getAlertId());

        } catch (Exception e) {
            log.error("触发测试告警失败", e);
            return ResponseEntity.internalServerError().body("触发测试告警失败: " + e.getMessage());
        }
    }

    // 请求DTO类
    public static class AcknowledgeRequest {
        private String confirmedBy;
        private String note;

        // Getters and Setters
        public String getConfirmedBy() { return confirmedBy; }
        public void setConfirmedBy(String confirmedBy) { this.confirmedBy = confirmedBy; }
        public String getNote() { return note; }
        public void setNote(String note) { this.note = note; }
    }

    public static class ResolveRequest {
        private String resolvedBy;
        private String resolutionNote;

        // Getters and Setters
        public String getResolvedBy() { return resolvedBy; }
        public void setResolvedBy(String resolvedBy) { this.resolvedBy = resolvedBy; }
        public String getResolutionNote() { return resolutionNote; }
        public void setResolutionNote(String resolutionNote) { this.resolutionNote = resolutionNote; }
    }

    public static class FalsePositiveRequest {
        private String markedBy;
        private String note;

        // Getters and Setters
        public String getMarkedBy() { return markedBy; }
        public void setMarkedBy(String markedBy) { this.markedBy = markedBy; }
        public String getNote() { return note; }
        public void setNote(String note) { this.note = note; }
    }

    public static class TestAlertRequest {
        private String deviceId = "TEST_DEVICE";
        private AlertRuleDTO.AlertLevel alertLevel = AlertRuleDTO.AlertLevel.INFO;
        private String title = "测试告警";
        private String content = "这是一个测试告警，用于验证通知系统功能。";

        // Getters and Setters
        public String getDeviceId() { return deviceId; }
        public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
        public AlertRuleDTO.AlertLevel getAlertLevel() { return alertLevel; }
        public void setAlertLevel(AlertRuleDTO.AlertLevel alertLevel) { this.alertLevel = alertLevel; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
    }
}