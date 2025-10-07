package com.xiamen.metro.message.service.glm;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 降级分析服务测试
 */
class FallbackAnalysisServiceTest {

    private FallbackAnalysisService fallbackAnalysisService;

    @BeforeEach
    void setUp() {
        fallbackAnalysisService = new FallbackAnalysisService();
    }

    @Test
    @DisplayName("分析正常状态报文")
    void shouldAnalyzeNormalStatusMessage() {
        String message = "DEVICE_ID: DEV-001 STATUS: NORMAL TIMESTAMP: 2024-01-01T10:00:00 All systems operational";
        String result = fallbackAnalysisService.performFallbackAnalysis(message, "STATUS", "DEV-001");

        assertNotNull(result);
        assertEquals("SUCCESS", result.getStatus());
        assertTrue(result.getSummary().contains("STATUS"));
        assertTrue(result.getConfidenceScore() > 0.5);
        assertFalse(result.getFromCache());
        assertNotNull(result.getAnalysisTime());
    }

    @Test
    @DisplayName("分析错误报文")
    void shouldAnalyzeErrorMessage() {
        String message = "DEVICE_ID: DEV-002 ERROR: Connection timeout SYSTEM_FAILURE: True";
        String result = fallbackAnalysisService.performFallbackAnalysis(message, "ERROR", "DEV-002");

        assertNotNull(result);
        assertEquals("SUCCESS", result.getStatus());
        assertTrue(result.getAnomalies() != null && !result.getAnomalies().isEmpty());
        assertTrue(result.getAnomalies().get(0).getType().equals("ERROR_PATTERN"));
        assertEquals("MEDIUM", result.getAnomalies().get(0).getSeverity());
    }

    @Test
    @DisplayName("分析控制报文")
    void shouldAnalyzeControlMessage() {
        String message = "DEVICE_ID: DEV-003 COMMAND: START VALUE: 1 TIMESTAMP: 2024-01-01T10:00:00";
        String result = fallbackAnalysisService.performFallbackAnalysis(message, "CONTROL", "DEV-003");

        assertNotNull(result);
        assertEquals("SUCCESS", result.getStatus());
        assertTrue(result.getRecommendations() != null && !result.getRecommendations().isEmpty());
        assertTrue(result.getRecommendations().get(0).contains("验证控制指令"));
    }

    @Test
    @DisplayName("提取关键字段")
    void shouldExtractKeyFields() {
        String message = "DEVICE_ID: DEV-004 STATUS: RUNNING TIMESTAMP: 2024-01-01T10:00:00 PERFORMANCE: 95%";
        String result = fallbackAnalysisService.performFallbackAnalysis(message, "DEVICE", "DEV-004");

        assertNotNull(result);
        assertTrue(result.getKeyFields().stream().anyMatch(field -> field.contains("DEVICE_ID")));
        assertTrue(result.getKeyFields().stream().anyMatch(field -> field.contains("TIMESTAMP")));
        assertTrue(result.getKeyFields().stream().anyMatch(field -> field.contains("MESSAGE_LENGTH")));
    }

    @Test
    @DisplayName("检测长报文异常")
    void shouldDetectLongMessageAnomaly() {
        // 构建一个超长报文
        StringBuilder longMessage = new StringBuilder("DEVICE_ID: DEV-005 STATUS: ");
        for (int i = 0; i < 1001; i++) {
            longMessage.append("very long message content ");
        }

        String result = fallbackAnalysisService.performFallbackAnalysis(
                longMessage.toString(), "SYSTEM", "DEV-005");

        assertNotNull(result);
        assertTrue(result.getAnomalies() != null && !result.getAnomalies().isEmpty());
        assertTrue(result.getAnomalies().stream()
                .anyMatch(anomaly -> "LARGE_MESSAGE".equals(anomaly.getType())));
    }

    @Test
    @DisplayName("测试不同报文类型的建议")
    void shouldProvideTypeSpecificRecommendations() {
        // 测试系统报文
        String systemResult = fallbackAnalysisService.performFallbackAnalysis(
                "System message", "SYSTEM", "DEV-001");
        assertTrue(systemResult.getRecommendations().stream()
                .anyMatch(rec -> rec.contains("监控系统运行状态")));

        // 测试设备报文
        String deviceResult = fallbackAnalysisService.performFallbackAnalysis(
                "Device message", "DEVICE", "DEV-002");
        assertTrue(deviceResult.getRecommendations().stream()
                .anyMatch(rec -> rec.contains("监控设备性能指标")));

        // 测试错误报文
        String errorResult = fallbackAnalysisService.performFallbackAnalysis(
                "Error message", "ERROR", "DEV-003");
        assertTrue(errorResult.getRecommendations().stream()
                .anyMatch(rec -> rec.contains("立即检查设备状态")));
    }

    @Test
    @DisplayName("检查服务可用性")
    void shouldCheckServiceAvailability() {
        assertTrue(fallbackAnalysisService.isAvailable());
    }

    @Test
    @DisplayName("处理空内容")
    void shouldHandleEmptyContent() {
        String result = fallbackAnalysisService.performFallbackAnalysis("", "UNKNOWN", "DEV-999");

        assertNotNull(result);
        assertEquals("SUCCESS", result.getStatus());
        assertNotNull(result.getSummary());
    }
}