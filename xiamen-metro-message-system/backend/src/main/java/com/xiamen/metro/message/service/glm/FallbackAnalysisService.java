package com.xiamen.metro.message.service.glm;

import com.xiamen.metro.message.dto.glm.MessageAnalysisResponseDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 降级分析服务
 * 当GLM API不可用时提供基础的报文分析功能
 */
@Slf4j
@Service
public class FallbackAnalysisService {

    private static final Pattern ERROR_PATTERN = Pattern.compile("(?i)(error|fail|exception|timeout|fault)");
    private static final Pattern DEVICE_ID_PATTERN = Pattern.compile("DEVICE_ID[\\s:]+([A-Z0-9-]+)");
    private static final Pattern TIMESTAMP_PATTERN = Pattern.compile("\\d{4}-\\d{2}-\\d{2}[T\\s]\\d{2}:\\d{2}:\\d{2}");

    /**
     * 执行降级分析
     */
    public MessageAnalysisResponseDTO performFallbackAnalysis(String messageContent, String messageType, String deviceId) {
        log.info("执行降级分析，报文类型: {}, 设备ID: {}", messageType, deviceId);

        try {
            MessageAnalysisResponseDTO.AnomalyInfo anomaly = detectAnomalies(messageContent);
            String summary = generateSummary(messageContent, messageType);
            List<String> keyFields = extractKeyFields(messageContent);
            List<String> recommendations = generateRecommendations(messageType, anomaly != null);

            return MessageAnalysisResponseDTO.builder()
                    .analysisId("FALLBACK-" + System.currentTimeMillis())
                    .status("SUCCESS")
                    .summary(summary + " (降级分析结果)")
                    .keyFields(keyFields)
                    .anomalies(anomaly != null ? Arrays.asList(anomaly) : null)
                    .recommendations(recommendations)
                    .confidenceScore(0.6)  // 降级分析的置信度较低
                    .processingTimeMs(50L)
                    .analysisTime(LocalDateTime.now())
                    .fromCache(false)
                    .errorMessage("使用降级分析服务")
                    .build();

        } catch (Exception e) {
            log.error("降级分析失败", e);
            return createErrorResponse(e.getMessage());
        }
    }

    /**
     * 检测异常
     */
    private MessageAnalysisResponseDTO.AnomalyInfo detectAnomalies(String messageContent) {
        Matcher errorMatcher = ERROR_PATTERN.matcher(messageContent);

        if (errorMatcher.find()) {
            return MessageAnalysisResponseDTO.AnomalyInfo.builder()
                    .type("ERROR_PATTERN")
                    .description("报文中包含错误或异常关键词")
                    .severity("MEDIUM")
                    .relatedField(errorMatcher.group())
                    .suggestedAction("建议检查设备状态和错误日志")
                    .build();
        }

        // 检查其他异常模式
        if (messageContent.length() > 10000) {
            return MessageAnalysisResponseDTO.AnomalyInfo.builder()
                    .type("LARGE_MESSAGE")
                    .description("报文内容异常长")
                    .severity("LOW")
                    .relatedField("message_length")
                    .suggestedAction("检查数据传输是否正常")
                    .build();
        }

        return null;
    }

    /**
     * 生成摘要
     */
    private String generateSummary(String messageContent, String messageType) {
        String preview = messageContent.length() > 100 ?
                messageContent.substring(0, 100) + "..." : messageContent;

        return String.format("报文类型:%s, 内容预览:%s", messageType, preview);
    }

    /**
     * 提取关键字段
     */
    private List<String> extractKeyFields(String messageContent) {
        java.util.List<String> fields = new java.util.ArrayList<>();

        // 提取设备ID
        Matcher deviceMatcher = DEVICE_ID_PATTERN.matcher(messageContent);
        if (deviceMatcher.find()) {
            fields.add("DEVICE_ID: " + deviceMatcher.group(1));
        }

        // 提取时间戳
        Matcher timestampMatcher = TIMESTAMP_PATTERN.matcher(messageContent);
        if (timestampMatcher.find()) {
            fields.add("TIMESTAMP: " + timestampMatcher.group());
        }

        // 提取报文长度
        fields.add("MESSAGE_LENGTH: " + messageContent.length());

        return fields;
    }

    /**
     * 生成建议操作
     */
    private List<String> generateRecommendations(String messageType, boolean hasAnomaly) {
        if (hasAnomaly) {
            return Arrays.asList(
                "立即检查设备状态",
                "查看详细错误日志",
                "联系技术支持团队"
            );
        }

        return switch (messageType) {
            case "SYSTEM" -> Arrays.asList(
                "监控系统运行状态",
                "定期检查系统日志"
            );
            case "DEVICE" -> Arrays.asList(
                "监控设备性能指标",
                "执行定期维护检查"
            );
            case "CONTROL" -> Arrays.asList(
                "验证控制指令执行结果",
                "检查系统响应时间"
            );
            case "STATUS" -> Arrays.asList(
                "更新设备状态记录",
                "关注状态变化趋势"
            );
            default -> Arrays.asList(
                "持续监控报文内容",
                "定期分析数据质量"
            );
        };
    }

    /**
     * 创建错误响应
     */
    private MessageAnalysisResponseDTO createErrorResponse(String errorMessage) {
        return MessageAnalysisResponseDTO.builder()
                .analysisId("ERROR-" + System.currentTimeMillis())
                .status("FAILED")
                .summary("分析失败")
                .confidenceScore(0.0)
                .processingTimeMs(0L)
                .analysisTime(LocalDateTime.now())
                .fromCache(false)
                .errorMessage(errorMessage)
                .build();
    }

    /**
     * 检查降级服务是否可用
     */
    public boolean isAvailable() {
        try {
            // 简单的可用性检查
            return true;
        } catch (Exception e) {
            log.error("降级服务不可用", e);
            return false;
        }
    }
}