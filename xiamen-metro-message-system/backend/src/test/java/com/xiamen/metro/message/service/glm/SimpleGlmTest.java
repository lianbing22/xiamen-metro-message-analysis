package com.xiamen.metro.message.service.glm;

import com.xiamen.metro.message.dto.glm.MessageAnalysisRequestDTO;
import com.xiamen.metro.message.dto.glm.MessageAnalysisResponseDTO;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 简单的GLM功能测试
 */
@SpringBootTest
@TestPropertySource(properties = {
    "glm.api.key=test-key",
    "spring.data.redis.host=localhost",
    "spring.datasource.url=jdbc:h2:mem:testdb"
})
class SimpleGlmTest {

    @Test
    void testPromptTemplateManager() {
        PromptTemplateManager manager = new PromptTemplateManager();

        String template = manager.getTemplate("STATUS", "test message", "DEV-001", System.currentTimeMillis());

        assertNotNull(template);
        assertTrue(template.contains("test message"));
        assertTrue(template.contains("DEV-001"));
    }

    @Test
    void testFallbackAnalysisService() {
        FallbackAnalysisService service = new FallbackAnalysisService();

        MessageAnalysisResponseDTO result = service.performFallbackAnalysis(
            "DEVICE_ID: DEV-001 STATUS: NORMAL",
            "STATUS",
            "DEV-001"
        );

        assertNotNull(result);
        assertEquals("SUCCESS", result.getStatus());
        assertTrue(result.getSummary().contains("STATUS"));
        assertFalse(result.getFromCache());
        assertTrue(result.getConfidenceScore() > 0.5);
    }

    @Test
    void testMessageAnalysisRequest() {
        MessageAnalysisRequestDTO request = MessageAnalysisRequestDTO.builder()
            .messageContent("Test message")
            .messageType("SYSTEM")
            .deviceId("DEV-001")
            .timestamp(System.currentTimeMillis())
            .enableCache(true)
            .analysisDepth("DETAILED")
            .build();

        assertNotNull(request);
        assertEquals("Test message", request.getMessageContent());
        assertEquals("SYSTEM", request.getMessageType());
        assertEquals("DEV-001", request.getDeviceId());
        assertTrue(request.getEnableCache());
        assertEquals("DETAILED", request.getAnalysisDepth());
    }

    @Test
    void testMessageAnalysisResponse() {
        MessageAnalysisResponseDTO response = MessageAnalysisResponseDTO.builder()
            .analysisId("test-001")
            .status("SUCCESS")
            .summary("Test summary")
            .confidenceScore(0.95)
            .processingTimeMs(150L)
            .fromCache(false)
            .build();

        assertNotNull(response);
        assertEquals("test-001", response.getAnalysisId());
        assertEquals("SUCCESS", response.getStatus());
        assertEquals("Test summary", response.getSummary());
        assertEquals(0.95, response.getConfidenceScore());
        assertEquals(150L, response.getProcessingTimeMs());
        assertFalse(response.getFromCache());
    }
}