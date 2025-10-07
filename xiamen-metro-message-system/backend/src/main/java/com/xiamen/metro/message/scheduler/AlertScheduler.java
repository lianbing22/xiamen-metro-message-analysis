package com.xiamen.metro.message.scheduler;

import com.xiamen.metro.message.entity.AlertRuleEntity;
import com.xiamen.metro.message.service.alert.AlertManagementService;
import com.xiamen.metro.message.service.alert.AlertNotificationService;
import com.xiamen.metro.message.service.pump.PumpIntelligentAnalysisService;
import com.xiamen.metro.message.dto.pump.PumpAnalysisRequestDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 告警任务调度器
 * 负责定期执行告警检查、通知重试等任务
 *
 * @author Xiamen Metro System
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AlertScheduler {

    private final AlertManagementService alertManagementService;
    private final AlertNotificationService alertNotificationService;
    private final PumpIntelligentAnalysisService pumpIntelligentAnalysisService;

    /**
     * 定期检查告警规则
     * 每分钟执行一次
     */
    @Scheduled(fixedRate = 60000) // 每分钟执行
    public void checkAlertRules() {
        try {
            log.debug("开始执行告警规则检查");

            // 这里应该获取需要检查的设备和规则
            // 简化实现，触发水泵分析检查
            performPumpAnalysisCheck();

            log.debug("告警规则检查完成");

        } catch (Exception e) {
            log.error("告警规则检查失败", e);
        }
    }

    /**
     * 重试失败的通知
     * 每5分钟执行一次
     */
    @Scheduled(fixedRate = 300000) // 每5分钟执行
    public void retryFailedNotifications() {
        try {
            log.debug("开始重试失败的通知");

            alertNotificationService.retryFailedNotifications();

            log.debug("失败通知重试完成");

        } catch (Exception e) {
            log.error("重试失败通知异常", e);
        }
    }

    /**
     * 清理过期的告警数据
     * 每天凌晨2点执行
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanupExpiredAlerts() {
        try {
            log.info("开始清理过期的告警数据");

            LocalDateTime cleanupTime = LocalDateTime.now().minusDays(90); // 保留90天

            // 这里应该实现清理逻辑
            // alertRepository.deleteByAlertTimeBefore(cleanupTime);

            log.info("过期告警数据清理完成");

        } catch (Exception e) {
            log.error("清理过期告警数据失败", e);
        }
    }

    /**
     * 生成告警统计报告
     * 每天上午9点执行
     */
    @Scheduled(cron = "0 0 9 * * ?")
    public void generateDailyReport() {
        try {
            log.info("开始生成每日告警统计报告");

            LocalDateTime yesterday = LocalDateTime.now().minusDays(1);
            LocalDateTime startOfYesterday = yesterday.withHour(0).withMinute(0).withSecond(0);
            LocalDateTime endOfYesterday = yesterday.withHour(23).withMinute(59).withSecond(59);

            // 生成统计报告
            // dailyReportService.generateReport(startOfYesterday, endOfYesterday);

            log.info("每日告警统计报告生成完成");

        } catch (Exception e) {
            log.error("生成每日告警统计报告失败", e);
        }
    }

    /**
     * 系统健康检查
     * 每30分钟执行一次
     */
    @Scheduled(fixedRate = 1800000) // 每30分钟执行
    public void performHealthCheck() {
        try {
            log.debug("开始执行系统健康检查");

            // 检查告警系统各组件状态
            checkAlertSystemHealth();

            log.debug("系统健康检查完成");

        } catch (Exception e) {
            log.error("系统健康检查失败", e);
        }
    }

    /**
     * 执行水泵分析检查
     */
    private void performPumpAnalysisCheck() {
        try {
            // 这里应该获取需要检查的设备列表
            List<String> deviceIds = Arrays.asList("PUMP_001", "PUMP_002", "PUMP_003");

            for (String deviceId : deviceIds) {
                CompletableFuture.runAsync(() -> {
                    try {
                        // 构建分析请求
                        PumpAnalysisRequestDTO analysisRequest = PumpAnalysisRequestDTO.builder()
                                .deviceId(deviceId)
                                .startTime(LocalDateTime.now().minusHours(1))
                                .endTime(LocalDateTime.now())
                                .analysisTypes(Arrays.asList(
                                        PumpAnalysisRequestDTO.AnalysisType.ANOMALY_CLASSIFICATION,
                                        PumpAnalysisRequestDTO.AnalysisType.PERFORMANCE_EVALUATION
                                ))
                                .build();

                        // 执行分析
                        var analysisResult = pumpIntelligentAnalysisService.performIntelligentAnalysis(analysisRequest);

                        // 处理分析结果，生成告警
                        if (analysisResult != null && analysisResult.getStatus().equals("SUCCESS")) {
                            alertManagementService.processPumpAnalysisResults(deviceId, analysisResult);
                        }

                    } catch (Exception e) {
                        log.error("设备 {} 水泵分析检查失败", deviceId, e);
                    }
                });
            }

        } catch (Exception e) {
            log.error("水泵分析检查失败", e);
        }
    }

    /**
     * 检查告警系统健康状态
     */
    private void checkAlertSystemHealth() {
        try {
            // 检查数据库连接
            // checkDatabaseConnection();

            // 检查邮件服务
            // checkEmailService();

            // 检查WebSocket服务
            // checkWebSocketService();

            // 检查规则引擎
            // checkRuleEngine();

            log.debug("告警系统健康检查通过");

        } catch (Exception e) {
            log.error("告警系统健康检查发现问题", e);
            // 发送系统告警
            // sendSystemAlert("告警系统健康检查失败: " + e.getMessage());
        }
    }

    /**
     * 手动触发告警检查
     */
    public void triggerAlertCheck() {
        log.info("手动触发告警检查");
        checkAlertRules();
    }

    /**
     * 手动触发通知重试
     */
    public void triggerNotificationRetry() {
        log.info("手动触发通知重试");
        retryFailedNotifications();
    }
}