package com.xiamen.metro.message.service.alert;

import com.xiamen.metro.message.entity.AlertRuleEntity;
import com.xiamen.metro.message.entity.AlertRecordEntity;
import com.xiamen.metro.message.dto.alert.AlertRecordDTO;
import com.xiamen.metro.message.repository.AlertRuleRepository;
import com.xiamen.metro.message.repository.AlertRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 告警管理服务
 * 负责告警的创建、状态管理、去重和抑制
 *
 * @author Xiamen Metro System
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlertManagementService {

    private final AlertRuleRepository alertRuleRepository;
    private final AlertRecordRepository alertRecordRepository;
    private final AlertNotificationService alertNotificationService;
    private final AlertRuleEngine alertRuleEngine;

    /**
     * 处理水泵分析结果，生成告警
     */
    @Transactional
    public List<AlertRecordDTO> processPumpAnalysisResults(String deviceId,
                                                          com.xiamen.metro.message.dto.pump.PumpAnalysisResponseDTO analysisResult) {
        log.info("处理设备 {} 的水泵分析结果，生成告警", deviceId);

        // 创建评估上下文
        AlertEvaluationContext context = AlertEvaluationContext.fromPumpAnalysis(deviceId, analysisResult);

        // 获取适用的告警规则
        List<AlertRuleEntity> applicableRules = alertRuleRepository.findApplicableRules(deviceId);

        List<AlertRecordDTO> generatedAlerts = new ArrayList<>();

        for (AlertRuleEntity rule : applicableRules) {
            try {
                // 评估规则
                AlertEvaluationResult evaluationResult = alertRuleEngine.evaluateRule(rule, context);

                if (evaluationResult.isTriggered()) {
                    // 检查告警去重和抑制
                    if (shouldCreateAlert(rule, context, evaluationResult)) {
                        AlertRecordEntity alertRecord = createAlertRecord(rule, context, evaluationResult);
                        alertRecordRepository.save(alertRecord);

                        AlertRecordDTO alertDTO = convertToDTO(alertRecord);
                        generatedAlerts.add(alertDTO);

                        // 异步发送通知
                        alertNotificationService.sendAlertNotifications(alertDTO);

                        log.info("生成告警: {} - {}", alertRecord.getAlertId(), alertRecord.getAlertTitle());
                    } else {
                        log.debug("告警被去重或抑制: 规则 {} 设备 {}", rule.getRuleName(), deviceId);
                    }
                }
            } catch (Exception e) {
                log.error("处理告警规则失败: {} (设备: {})", rule.getRuleName(), deviceId, e);
            }
        }

        log.info("设备 {} 告警处理完成，生成 {} 个告警", deviceId, generatedAlerts.size());
        return generatedAlerts;
    }

    /**
     * 判断是否应该创建告警（去重和抑制）
     */
    private boolean shouldCreateAlert(AlertRuleEntity rule, AlertEvaluationContext context,
                                    AlertEvaluationResult evaluationResult) {
        // 检查抑制时间
        if (isInSuppressionPeriod(rule, context)) {
            log.debug("告警在抑制期内: 规则 {} 设备 {}", rule.getRuleName(), context.getDeviceId());
            return false;
        }

        // 检查重复告警
        if (isDuplicateAlert(rule, context)) {
            log.debug("发现重复告警: 规则 {} 设备 {}", rule.getRuleName(), context.getDeviceId());
            return false;
        }

        // 检查连续触发要求
        if (!checkConsecutiveTriggerRequirement(rule, context, evaluationResult)) {
            log.debug("不满足连续触发要求: 规则 {} 设备 {}", rule.getRuleName(), context.getDeviceId());
            return false;
        }

        return true;
    }

    /**
     * 检查是否在抑制期内
     */
    private boolean isInSuppressionPeriod(AlertRuleEntity rule, AlertEvaluationContext context) {
        if (rule.getSuppressionMinutes() == null || rule.getSuppressionMinutes() <= 0) {
            return false;
        }

        LocalDateTime suppressionStartTime = context.getAnalysisTime().minusMinutes(rule.getSuppressionMinutes());

        List<AlertRecordEntity> recentAlerts = alertRecordRepository.findRecentSimilarAlerts(
                context.getDeviceId(), rule.getRuleId(), suppressionStartTime);

        return !recentAlerts.isEmpty();
    }

    /**
     * 检查是否为重复告警
     */
    private boolean isDuplicateAlert(AlertRuleEntity rule, AlertEvaluationContext context) {
        // 查找最近1小时内相同设备和规则的告警
        LocalDateTime oneHourAgo = context.getAnalysisTime().minusHours(1);

        List<AlertRecordEntity> recentAlerts = alertRecordRepository.findRecentSimilarAlerts(
                context.getDeviceId(), rule.getRuleId(), oneHourAgo);

        // 检查是否有相似的触发值
        for (AlertRecordEntity recentAlert : recentAlerts) {
            if (isSimilarAlert(recentAlert, context)) {
                return true;
            }
        }

        return false;
    }

    /**
     * 判断告警是否相似
     */
    private boolean isSimilarAlert(AlertRecordEntity existingAlert, AlertEvaluationContext context) {
        if (existingAlert.getTriggeredValue() == null) {
            return false;
        }

        // 比较触发值的差异
        for (Map.Entry<String, Double> entry : context.getMetricValues().entrySet()) {
            String metricName = entry.getKey();
            Double currentValue = entry.getValue();

            if (currentValue != null) {
                double difference = Math.abs(currentValue - existingAlert.getTriggeredValue());
                double percentDifference = difference / Math.abs(existingAlert.getTriggeredValue());

                // 如果差异小于5%，认为是相似告警
                if (percentDifference < 0.05) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * 检查连续触发要求
     */
    private boolean checkConsecutiveTriggerRequirement(AlertRuleEntity rule, AlertEvaluationContext context,
                                                     AlertEvaluationResult evaluationResult) {
        if (rule.getConsecutiveTriggerCount() == null || rule.getConsecutiveTriggerCount() <= 1) {
            return true;
        }

        // 查找最近的连续触发记录
        LocalDateTime checkStartTime = context.getAnalysisTime()
                .minusMinutes(rule.getCheckIntervalMinutes() * rule.getConsecutiveTriggerCount());

        List<AlertRecordEntity> recentTriggeredAlerts = alertRecordRepository
                .findByDeviceAndTimeRange(context.getDeviceId(), checkStartTime, context.getAnalysisTime())
                .stream()
                .filter(alert -> alert.getRuleId().equals(rule.getRuleId()) && alert.getStatus() != AlertRecordEntity.AlertStatus.FALSE_POSITIVE)
                .collect(Collectors.toList());

        // 如果连续触发次数满足要求，则创建告警
        return recentTriggeredAlerts.size() >= rule.getConsecutiveTriggerCount() - 1;
    }

    /**
     * 创建告警记录
     */
    private AlertRecordEntity createAlertRecord(AlertRuleEntity rule, AlertEvaluationContext context,
                                              AlertEvaluationResult evaluationResult) {
        String alertId = generateAlertId();

        AlertRecordEntity alertRecord = new AlertRecordEntity();
        alertRecord.setAlertId(alertId);
        alertRecord.setRuleId(rule.getRuleId());
        alertRecord.setDeviceId(context.getDeviceId());
        alertRecord.setAlertLevel(evaluationResult.getSeverity());
        alertRecord.setAlertTitle(evaluationResult.generateAlertTitle(context.getDeviceId(), rule.getRuleName()));
        alertRecord.setAlertContent(evaluationResult.generateAlertContent());
        alertRecord.setTriggeredValue(evaluationResult.getTriggeredValue());
        alertRecord.setThresholdValue(evaluationResult.getThresholdValue());
        alertRecord.setConfidenceScore(evaluationResult.getConfidence());
        alertRecord.setAlertTime(context.getAnalysisTime());
        alertRecord.setStatus(AlertRecordEntity.AlertStatus.ACTIVE);
        alertRecord.setIsConfirmed(false);
        alertRecord.setAnalysisResultId(context.getAnalysisId());
        alertRecord.setAlertSource(context.getDataSource());
        alertRecord.setExtendedInfo(convertMapToJson(evaluationResult.toExtendedInfoMap()));

        // 设置通知状态为待发送
        alertRecord.setEmailNotified(false);
        alertRecord.setSmsNotified(false);
        alertRecord.setWebsocketNotified(false);

        return alertRecord;
    }

    /**
     * 确认告警
     */
    @Transactional
    public AlertRecordDTO acknowledgeAlert(String alertId, String confirmedBy, String note) {
        AlertRecordEntity alertRecord = alertRecordRepository.findByAlertId(alertId);
        if (alertRecord == null) {
            throw new RuntimeException("告警不存在: " + alertId);
        }

        alertRecord.setIsConfirmed(true);
        alertRecord.setConfirmedTime(LocalDateTime.now());
        alertRecord.setConfirmedBy(confirmedBy);
        alertRecord.setConfirmationNote(note);

        if (alertRecord.getStatus() == AlertRecordEntity.AlertStatus.ACTIVE) {
            alertRecord.setStatus(AlertRecordEntity.AlertStatus.ACKNOWLEDGED);
        }

        alertRecordRepository.save(alertRecord);

        log.info("告警已确认: {} by {}", alertId, confirmedBy);
        return convertToDTO(alertRecord);
    }

    /**
     * 处理告警
     */
    @Transactional
    public AlertRecordDTO resolveAlert(String alertId, String resolvedBy, String resolutionNote) {
        AlertRecordEntity alertRecord = alertRecordRepository.findByAlertId(alertId);
        if (alertRecord == null) {
            throw new RuntimeException("告警不存在: " + alertId);
        }

        alertRecord.setStatus(AlertRecordEntity.AlertStatus.RESOLVED);
        alertRecord.setResolvedTime(LocalDateTime.now());
        alertRecord.setResolvedBy(resolvedBy);
        alertRecord.setResolutionNote(resolutionNote);

        alertRecordRepository.save(alertRecord);

        log.info("告警已处理: {} by {}", alertId, resolvedBy);
        return convertToDTO(alertRecord);
    }

    /**
     * 标记为误报
     */
    @Transactional
    public AlertRecordDTO markAsFalsePositive(String alertId, String markedBy, String note) {
        AlertRecordEntity alertRecord = alertRecordRepository.findByAlertId(alertId);
        if (alertRecord == null) {
            throw new RuntimeException("告警不存在: " + alertId);
        }

        alertRecord.setStatus(AlertRecordEntity.AlertStatus.FALSE_POSITIVE);
        alertRecord.setResolvedTime(LocalDateTime.now());
        alertRecord.setResolvedBy(markedBy);
        alertRecord.setResolutionNote(note);

        alertRecordRepository.save(alertRecord);

        log.info("告警已标记为误报: {} by {}", alertId, markedBy);
        return convertToDTO(alertRecord);
    }

    /**
     * 获取活跃告警
     */
    public List<AlertRecordDTO> getActiveAlerts(String deviceId) {
        List<AlertRecordEntity> activeAlerts = alertRecordRepository
                .findByDeviceIdAndStatus(deviceId, AlertRecordEntity.AlertStatus.ACTIVE);

        return activeAlerts.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * 获取告警统计信息
     */
    public Map<String, Object> getAlertStatistics(String deviceId, LocalDateTime since) {
        Map<String, Object> statistics = new HashMap<>();

        // 总告警数
        long totalAlerts = alertRecordRepository.findByDeviceAndTimeRange(deviceId, since, LocalDateTime.now()).size();
        statistics.put("totalAlerts", totalAlerts);

        // 各状态告警数量
        List<Object[]> statusCounts = alertRecordRepository.countAlertsByStatus();
        Map<String, Long> statusStats = new HashMap<>();
        for (Object[] count : statusCounts) {
            statusStats.put(count[0].toString(), (Long) count[1]);
        }
        statistics.put("statusCounts", statusStats);

        // 各级别告警数量
        List<Object[]> levelCounts = alertRecordRepository.countAlertsByLevel(since);
        Map<String, Long> levelStats = new HashMap<>();
        for (Object[] count : levelCounts) {
            levelStats.put(count[0].toString(), (Long) count[1]);
        }
        statistics.put("levelCounts", levelStats);

        // 未确认告警数量
        long unconfirmedCount = alertRecordRepository.findByIsConfirmedFalse().size();
        statistics.put("unconfirmedCount", unconfirmedCount);

        return statistics;
    }

    /**
     * 生成告警ID
     */
    private String generateAlertId() {
        return "ALERT_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * 转换为DTO
     */
    private AlertRecordDTO convertToDTO(AlertRecordEntity entity) {
        AlertRecordDTO dto = new AlertRecordDTO();
        dto.setId(entity.getId());
        dto.setAlertId(entity.getAlertId());
        dto.setRuleId(entity.getRuleId());
        dto.setDeviceId(entity.getDeviceId());
        dto.setAlertLevel(entity.getAlertLevel());
        dto.setAlertTitle(entity.getAlertTitle());
        dto.setAlertContent(entity.getAlertContent());
        dto.setTriggeredValue(entity.getTriggeredValue());
        dto.setThresholdValue(entity.getThresholdValue());
        dto.setConfidenceScore(entity.getConfidenceScore());
        dto.setAlertTime(entity.getAlertTime());
        dto.setStatus(entity.getStatus());
        dto.setIsConfirmed(entity.getIsConfirmed());
        dto.setConfirmedTime(entity.getConfirmedTime());
        dto.setConfirmedBy(entity.getConfirmedBy());
        dto.setConfirmationNote(entity.getConfirmationNote());
        dto.setResolvedTime(entity.getResolvedTime());
        dto.setResolvedBy(entity.getResolvedBy());
        dto.setResolutionNote(entity.getResolutionNote());
        dto.setAnalysisResultId(entity.getAnalysisResultId());
        dto.setAlertSource(entity.getAlertSource());
        dto.setCreatedTime(entity.getCreatedTime());
        dto.setUpdatedTime(entity.getUpdatedTime());

        // 计算持续时间
        if (entity.getAlertTime() != null) {
            LocalDateTime endTime = entity.getResolvedTime() != null ?
                    entity.getResolvedTime() : LocalDateTime.now();
            dto.setDurationMinutes(java.time.Duration.between(entity.getAlertTime(), endTime).toMinutes());
        }

        return dto;
    }

    /**
     * 转换Map为JSON字符串（简化实现）
     */
    private String convertMapToJson(Map<String, Object> map) {
        try {
            // 实际项目中应该使用ObjectMapper
            return map.toString();
        } catch (Exception e) {
            log.error("转换Map为JSON失败", e);
            return "{}";
        }
    }
}