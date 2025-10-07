package com.xiamen.metro.message.service.glm;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 提示词模板管理器
 * 为不同类型的报文分析提供专门的提示词模板
 */
@Component
public class PromptTemplateManager {

    private final Map<String, String> templates;

    public PromptTemplateManager() {
        this.templates = new HashMap<>();
        initializeTemplates();
    }

    /**
     * 初始化提示词模板
     */
    private void initializeTemplates() {
        // 系统报文分析模板
        templates.put("SYSTEM", """
                你是厦门地铁系统报文分析专家。请分析以下系统报文：

                报文内容：%s
                设备ID：%s
                时间戳：%s

                请提供详细分析，包括：
                1. 报文摘要（一句话总结）
                2. 关键字段提取（JSON格式）
                3. 异常检测（如有异常请详细说明）
                4. 系统状态评估
                5. 建议操作

                请以JSON格式返回分析结果，确保结果准确且可操作。
                """);

        // 设备报文分析模板
        templates.put("DEVICE", """
                你是厦门地铁设备报文分析专家。请分析以下设备报文：

                报文内容：%s
                设备ID：%s
                时间戳：%s

                请重点关注：
                1. 设备运行状态
                2. 性能指标
                3. 故障征兆
                4. 维护建议

                分析要求：
                - 提取关键性能指标
                - 识别潜在风险
                - 提供维护建议

                请以JSON格式返回分析结果。
                """);

        // 控制报文分析模板
        templates.put("CONTROL", """
                你是厦门地铁控制系统专家。请分析以下控制报文：

                报文内容：%s
                设备ID：%s
                时间戳：%s

                分析要点：
                1. 控制指令类型和目标
                2. 指令执行状态
                3. 安全性评估
                4. 系统响应分析

                请特别关注：
                - 控制逻辑的合理性
                - 安全约束条件
                - 异常情况处理

                请以JSON格式返回分析结果。
                """);

        // 状态报文分析模板
        templates.put("STATUS", """
                你是厦门地铁设备状态监控专家。请分析以下状态报文：

                报文内容：%s
                设备ID：%s
                时间戳：%s

                请分析：
                1. 设备当前状态
                2. 状态变化趋势
                3. 健康度评估
                4. 预警信息

                重点关注：
                - 状态参数的合理性
                - 趋势变化的异常
                - 预防性维护需求

                请以JSON格式返回分析结果。
                """);

        // 错误报文分析模板
        templates.put("ERROR", """
                你是厦门地铁故障诊断专家。请分析以下错误报文：

                报文内容：%s
                设备ID：%s
                时间戳：%s

                请进行深度分析：
                1. 错误类型和原因
                2. 影响范围评估
                3. 紧急程度判断
                4. 修复方案建议

                分析要求：
                - 准确定位故障原因
                - 评估故障影响
                - 提供详细修复步骤
                - 预防措施建议

                请以JSON格式返回分析结果。
                """);

        // 通用分析模板
        templates.put("DEFAULT", """
                你是厦门地铁报文分析专家。请分析以下报文：

                报文内容：%s
                报文类型：%s
                设备ID：%s
                时间戳：%s

                请提供：
                1. 报文摘要
                2. 关键信息提取
                3. 异常检测
                4. 建议操作

                请以JSON格式返回分析结果。
                """);
    }

    /**
     * 根据报文类型获取提示词模板
     */
    public String getTemplate(String messageType, String messageContent, String deviceId, Long timestamp) {
        String template = templates.getOrDefault(messageType, templates.get("DEFAULT"));

        // 处理时间戳格式化
        String formattedTime = timestamp != null ? new java.time.Instant()
                .ofEpochMilli(timestamp)
                .atZone(java.time.ZoneId.systemDefault())
                .format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME) : "未知";

        return String.format(template, messageContent, deviceId, formattedTime);
    }

    /**
     * 获取带上下文的模板
     */
    public String getTemplateWithContext(String messageType, String messageContent, String deviceId,
                                       Long timestamp, String context) {
        String baseTemplate = getTemplate(messageType, messageContent, deviceId, timestamp);

        if (context != null && !context.trim().isEmpty()) {
            return String.format("""
                    %s

                    附加上下文信息：
                    %s
                    """, baseTemplate, context);
        }

        return baseTemplate;
    }

    /**
     * 添加自定义模板
     */
    public void addTemplate(String messageType, String template) {
        templates.put(messageType, template);
    }

    /**
     * 获取所有模板类型
     */
    public java.util.Set<String> getAvailableTemplateTypes() {
        return templates.keySet();
    }
}