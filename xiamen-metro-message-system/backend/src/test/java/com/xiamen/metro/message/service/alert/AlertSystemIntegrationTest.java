package com.xiamen.metro.message.service.alert;

import com.xiamen.metro.message.dto.alert.AlertRecordDTO;
import com.xiamen.metro.message.dto.pump.PumpAnalysisRequestDTO;
import com.xiamen.metro.message.dto.pump.PumpAnalysisResponseDTO;
import com.xiamen.metro.message.entity.AlertRuleEntity;
import com.xiamen.metro.message.entity.AlertRecordEntity;
import com.xiamen.metro.message.repository.AlertRuleRepository;
import com.xiamen.metro.message.repository.AlertRecordRepository;
import com.xiamen.metro.message.service.pump.PumpIntelligentAnalysisService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 告警系统集成测试
 *
 * @author Xiamen Metro System
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class AlertSystemIntegrationTest {

    @Autowired
    private AlertManagementService alertManagementService;

    @Autowired
    private AlertNotificationService alertNotificationService;

    @Autowired
    private AlertRuleRepository alertRuleRepository;

    @Autowired
    private AlertRecordRepository alertRecordRepository;

    @Autowired
    private PumpIntelligentAnalysisService pumpIntelligentAnalysisService;

    private AlertRuleEntity testRule;

    @BeforeEach
    void setUp() {
        // 创建测试告警规则
        testRule = createTestAlertRule();
        alertRuleRepository.save(testRule);
    }

    @Test
    @DisplayName("测试告警规则评估")
    void testAlertRuleEvaluation() {
        // 创建测试分析上下文
        AlertEvaluationContext context = AlertEvaluationContext.builder()
                .deviceId("TEST_PUMP_001")
                .analysisTime(LocalDateTime.now())
                .build();

        context.setMetricValue("health_score", 45.0); // 低于阈值60.0
        context.setMetricValue("efficiency_score", 80.0);

        // 创建告警规则引擎
        AlertRuleEngine ruleEngine = new AlertRuleEngine(null);

        // 评估规则
        AlertEvaluationResult result = ruleEngine.evaluateRule(testRule, context);

        // 验证结果
        assertTrue(result.isTriggered(), "应该触发告警");
        assertNotNull(result.getMessage(), "告警消息不应为空");
        assertEquals(testRule.getAlertLevel(), result.getSeverity(), "告警级别应该匹配");
        assertTrue(result.getConfidence() > 0, "置信度应该大于0");

        System.out.println("告警评估结果: " + result.getMessage());
    }

    @Test
    @DisplayName("测试水泵分析结果告警生成")
    void testPumpAnalysisAlertGeneration() {
        // 创建模拟水泵分析结果
        PumpAnalysisResponseDTO analysisResult = createMockPumpAnalysisResult();

        // 处理分析结果，生成告警
        List<AlertRecordDTO> alerts = alertManagementService.processPumpAnalysisResults(
                "TEST_PUMP_001", analysisResult);

        // 验证告警生成
        assertNotNull(alerts, "告警列表不应为空");
        assertFalse(alerts.isEmpty(), "应该生成告警");

        AlertRecordDTO alert = alerts.get(0);
        assertNotNull(alert.getAlertId(), "告警ID不应为空");
        assertEquals("TEST_PUMP_001", alert.getDeviceId(), "设备ID应该匹配");
        assertEquals(AlertRecordEntity.AlertStatus.ACTIVE, alert.getStatus(), "告警状态应为活跃");
        assertNotNull(alert.getAlertTitle(), "告警标题不应为空");
        assertNotNull(alert.getAlertContent(), "告警内容不应为空");

        System.out.println("生成的告警: " + alert.getAlertTitle());
        System.out.println("告警内容: " + alert.getAlertContent());

        // 验证告警已保存到数据库
        AlertRecordEntity savedAlert = alertRecordRepository.findByAlertId(alert.getAlertId());
        assertNotNull(savedAlert, "告警应该已保存到数据库");
        assertEquals(alert.getAlertTitle(), savedAlert.getAlertTitle(), "保存的告警标题应该匹配");
    }

    @Test
    @DisplayName("测试告警确认功能")
    void testAlertAcknowledgment() {
        // 先创建一个告警
        PumpAnalysisResponseDTO analysisResult = createMockPumpAnalysisResult();
        List<AlertRecordDTO> alerts = alertManagementService.processPumpAnalysisResults(
                "TEST_PUMP_001", analysisResult);

        assertFalse(alerts.isEmpty(), "应该有告警生成");

        AlertRecordDTO alert = alerts.get(0);
        String alertId = alert.getAlertId();

        // 确认告警
        AlertRecordDTO acknowledgedAlert = alertManagementService.acknowledgeAlert(
                alertId, "test_operator", "已确认，正在处理");

        // 验证确认结果
        assertNotNull(acknowledgedAlert, "确认后的告警不应为空");
        assertTrue(acknowledgedAlert.getIsConfirmed(), "告警应该已确认");
        assertEquals("test_operator", acknowledgedAlert.getConfirmedBy(), "确认人应该匹配");
        assertNotNull(acknowledgedAlert.getConfirmedTime(), "确认时间不应为空");
        assertEquals(AlertRecordEntity.AlertStatus.ACKNOWLEDGED, acknowledgedAlert.getStatus(), "状态应为已确认");

        System.out.println("告警已确认: " + alertId);
    }

    @Test
    @DisplayName("测试告警处理功能")
    void testAlertResolution() {
        // 先创建一个告警
        PumpAnalysisResponseDTO analysisResult = createMockPumpAnalysisResult();
        List<AlertRecordDTO> alerts = alertManagementService.processPumpAnalysisResults(
                "TEST_PUMP_001", analysisResult);

        assertFalse(alerts.isEmpty(), "应该有告警生成");

        AlertRecordDTO alert = alerts.get(0);
        String alertId = alert.getAlertId();

        // 处理告警
        AlertRecordDTO resolvedAlert = alertManagementService.resolveAlert(
                alertId, "test_operator", "问题已解决，设备恢复正常");

        // 验证处理结果
        assertNotNull(resolvedAlert, "处理后的告警不应为空");
        assertEquals(AlertRecordEntity.AlertStatus.RESOLVED, resolvedAlert.getStatus(), "状态应为已处理");
        assertEquals("test_operator", resolvedAlert.getResolvedBy(), "处理人应该匹配");
        assertNotNull(resolvedAlert.getResolvedTime(), "处理时间不应为空");
        assertEquals("问题已解决，设备恢复正常", resolvedAlert.getResolutionNote(), "处理备注应该匹配");

        System.out.println("告警已处理: " + alertId);
    }

    @Test
    @DisplayName("测试告警去重功能")
    void testAlertDeduplication() {
        // 创建第一次分析结果
        PumpAnalysisResponseDTO analysisResult1 = createMockPumpAnalysisResult();
        List<AlertRecordDTO> alerts1 = alertManagementService.processPumpAnalysisResults(
                "TEST_PUMP_001", analysisResult1);

        // 创建第二次相似的分析结果（短时间内）
        PumpAnalysisResponseDTO analysisResult2 = createMockPumpAnalysisResult();
        List<AlertRecordDTO> alerts2 = alertManagementService.processPumpAnalysisResults(
                "TEST_PUMP_001", analysisResult2);

        // 验证去重效果
        assertTrue(alerts1.size() >= 1, "第一次应该生成告警");
        // 由于去重机制，第二次可能不会生成新告警
        System.out.println("第一次生成告警数: " + alerts1.size());
        System.out.println("第二次生成告警数: " + alerts2.size());
    }

    @Test
    @DisplayName("测试告警统计功能")
    void testAlertStatistics() {
        // 创建多个不同级别的告警
        for (int i = 0; i < 5; i++) {
            PumpAnalysisResponseDTO analysisResult = createMockPumpAnalysisResult();
            alertManagementService.processPumpAnalysisResults("TEST_PUMP_" + i, analysisResult);
        }

        // 获取统计信息
        var statistics = alertManagementService.getAlertStatistics(
                "TEST_PUMP_001", LocalDateTime.now().minusHours(1));

        assertNotNull(statistics, "统计信息不应为空");
        assertTrue(statistics.containsKey("totalAlerts"), "应包含总告警数");
        assertTrue(statistics.containsKey("statusCounts"), "应包含状态统计");
        assertTrue(statistics.containsKey("levelCounts"), "应包含级别统计");

        System.out.println("告警统计: " + statistics);
    }

    /**
     * 创建测试告警规则
     */
    private AlertRuleEntity createTestAlertRule() {
        AlertRuleEntity rule = new AlertRuleEntity();
        rule.setRuleName("健康评分测试规则");
        rule.setDescription("测试用的健康评分监控规则");
        rule.setDeviceId(null); // 适用于所有设备
        rule.setRuleType(AlertRuleEntity.RuleType.HEALTH_SCORE);
        rule.setAlertLevel(AlertRuleEntity.AlertLevel.WARNING);
        rule.setRuleConditions("{\"metricName\": \"health_score\", \"comparison\": \"lt\"}");
        rule.setThresholdConfig("{\"healthScoreThreshold\": 60.0}");
        rule.setCheckIntervalMinutes(60);
        rule.setConsecutiveTriggerCount(1);
        rule.setSuppressionMinutes(30);
        rule.setIsActive(true);
        rule.setPriority(2);
        rule.setCreatedBy("TEST_SYSTEM");

        return rule;
    }

    /**
     * 创建模拟水泵分析结果
     */
    private PumpAnalysisResponseDTO createMockPumpAnalysisResult() {
        return PumpAnalysisResponseDTO.builder()
                .analysisId("TEST_ANALYSIS_" + System.currentTimeMillis())
                .deviceId("TEST_PUMP_001")
                .analysisTime(LocalDateTime.now())
                .status("SUCCESS")
                .overallHealthScore(45.0) // 低于阈值，应触发告警
                .riskLevel("HIGH")
                .confidenceScore(0.85)
                .modelVersion("1.0")
                .processingTimeMs(1500L)
                .performanceMetrics(PumpAnalysisResponseDTO.PerformanceMetrics.builder()
                        .efficiencyScore(75.0)
                        .reliabilityScore(60.0)
                        .maintenanceScore(50.0)
                        .build())
                .predictionInfo(PumpAnalysisResponseDTO.PredictionInfo.builder()
                        .failureProbability(0.65)
                        .remainingUsefulLifeDays(15)
                        .build())
                .analysisResults(Arrays.asList(
                        PumpAnalysisResponseDTO.AnalysisResult.builder()
                                .analysisType("健康评分评估")
                                .severityLevel(2)
                                .confidence(0.85)
                                .description("健康评分低于正常水平")
                                .detectedValue(45.0)
                                .expectedValue(80.0)
                                .build()
                ))
                .build();
    }
}