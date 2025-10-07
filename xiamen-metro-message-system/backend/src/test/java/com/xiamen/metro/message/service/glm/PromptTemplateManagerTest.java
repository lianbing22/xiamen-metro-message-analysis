package com.xiamen.metro.message.service.glm;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 提示词模板管理器测试
 */
class PromptTemplateManagerTest {

    private PromptTemplateManager promptTemplateManager;

    @BeforeEach
    void setUp() {
        promptTemplateManager = new PromptTemplateManager();
    }

    @Test
    @DisplayName("获取系统报文模板")
    void shouldGetSystemTemplate() {
        String result = promptTemplateManager.getTemplate(
                "SYSTEM",
                "System message content",
                "DEV-001",
                System.currentTimeMillis()
        );

        assertNotNull(result);
        assertTrue(result.contains("厦门地铁系统报文分析专家"));
        assertTrue(result.contains("System message content"));
        assertTrue(result.contains("DEV-001"));
    }

    @Test
    @DisplayName("获取设备报文模板")
    void shouldGetDeviceTemplate() {
        String result = promptTemplateManager.getTemplate(
                "DEVICE",
                "Device status message",
                "DEV-002",
                System.currentTimeMillis()
        );

        assertNotNull(result);
        assertTrue(result.contains("厦门地铁设备报文分析专家"));
        assertTrue(result.contains("Device status message"));
        assertTrue(result.contains("设备运行状态"));
    }

    @Test
    @DisplayName("未知类型使用默认模板")
    void shouldUseDefaultTemplateForUnknownType() {
        String result = promptTemplateManager.getTemplate(
                "UNKNOWN",
                "Unknown message",
                "DEV-003",
                System.currentTimeMillis()
        );

        assertNotNull(result);
        assertTrue(result.contains("厦门地铁报文分析专家"));
        assertTrue(result.contains("Unknown message"));
    }

    @Test
    @DisplayName("获取带上下文的模板")
    void shouldGetTemplateWithContext() {
        String result = promptTemplateManager.getTemplateWithContext(
                "ERROR",
                "Error message",
                "DEV-004",
                System.currentTimeMillis(),
                "设备已运行24小时，温度正常"
        );

        assertNotNull(result);
        assertTrue(result.contains("厦门地铁故障诊断专家"));
        assertTrue(result.contains("Error message"));
        assertTrue(result.contains("设备已运行24小时，温度正常"));
    }

    @Test
    @DisplayName("添加自定义模板")
    void shouldAddCustomTemplate() {
        String customTemplate = "自定义模板：%s %s %s";
        promptTemplateManager.addTemplate("CUSTOM", customTemplate);

        String result = promptTemplateManager.getTemplate(
                "CUSTOM",
                "Test message",
                "DEV-005",
                System.currentTimeMillis()
        );

        assertNotNull(result);
        assertTrue(result.contains("自定义模板"));
        assertTrue(result.contains("Test message"));
    }

    @Test
    @DisplayName("获取所有模板类型")
    void shouldGetAllTemplateTypes() {
        var types = promptTemplateManager.getAvailableTemplateTypes();

        assertNotNull(types);
        assertTrue(types.contains("SYSTEM"));
        assertTrue(types.contains("DEVICE"));
        assertTrue(types.contains("CONTROL"));
        assertTrue(types.contains("STATUS"));
        assertTrue(types.contains("ERROR"));
        assertTrue(types.contains("DEFAULT"));
    }
}