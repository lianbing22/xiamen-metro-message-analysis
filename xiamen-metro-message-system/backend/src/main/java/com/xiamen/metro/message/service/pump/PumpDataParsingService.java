package com.xiamen.metro.message.service.pump;

import com.xiamen.metro.message.entity.PumpDataEntity;
import com.xiamen.metro.message.dto.MessageDataDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 水泵数据解析服务
 * 从原始报文中提取水泵运行数据
 *
 * @author Xiamen Metro System
 */
@Slf4j
@Service
public class PumpDataParsingService {

    // 定义水泵相关的正则表达式模式
    private static final Map<String, Pattern> PUMP_PATTERNS = new HashMap<>();

    static {
        // 设备ID模式
        PUMP_PATTERNS.put("deviceId", Pattern.compile("(?:设备ID|Device|设备)[:：]?\\s*([A-Za-z0-9_\\-]+)", Pattern.CASE_INSENSITIVE));

        // 时间戳模式
        PUMP_PATTERNS.put("timestamp", Pattern.compile("(?:时间|Time|时间戳)[:：]?\\s*(\\d{4}-\\d{2}-\\d{2}\\s+\\d{2}:\\d{2}:\\d{2})", Pattern.CASE_INSENSITIVE));

        // 水泵状态模式
        PUMP_PATTERNS.put("pumpStatus", Pattern.compile("(?:水泵状态|运行状态|Status|启停)[:：]?\\s*(启动|停止|运行|关闭|0|1|ON|OFF)", Pattern.CASE_INSENSITIVE));

        // 运行时间模式
        PUMP_PATTERNS.put("runtimeMinutes", Pattern.compile("(?:运行时间|Runtime|连续运行)[:：]?\\s*(\\d+(?:\\.\\d+)?)\\s*(?:分钟|min|minutes?)", Pattern.CASE_INSENSITIVE));

        // 电流模式
        PUMP_PATTERNS.put("currentAmperage", Pattern.compile("(?:电流|Current|I)[:：]?\\s*(\\d+(?:\\.\\d+)?)\\s*([Aa]安)", Pattern.CASE_INSENSITIVE));

        // 电压模式
        PUMP_PATTERNS.put("voltage", Pattern.compile("(?:电压|Voltage|U)[:：]?\\s*(\\d+(?:\\.\\d+)?)\\s*([Vv]伏)", Pattern.CASE_INSENSITIVE));

        // 功率模式
        PUMP_PATTERNS.put("powerKw", Pattern.compile("(?:功率|Power|P)[:：]?\\s*(\\d+(?:\\.\\d+)?)\\s*(?:kW|千瓦)", Pattern.CASE_INSENSITIVE));

        // 能耗模式
        PUMP_PATTERNS.put("energyConsumptionKwh", Pattern.compile("(?:能耗|能量|Energy|电度)[:：]?\\s*(\\d+(?:\\.\\d+)?)\\s*(?:kWh|度)", Pattern.CASE_INSENSITIVE));

        // 水压模式
        PUMP_PATTERNS.put("waterPressureKpa", Pattern.compile("(?:水压|压力|Pressure)[:：]?\\s*(\\d+(?:\\.\\d+)?)\\s*(?:kPa|千帕)", Pattern.CASE_INSENSITIVE));

        // 流量模式
        PUMP_PATTERNS.put("flowRateM3h", Pattern.compile("(?:流量|Flow|Q)[:：]?\\s*(\\d+(?:\\.\\d+)?)\\s*(?:m³/h|立方米/小时)", Pattern.CASE_INSENSITIVE));

        // 水温模式
        PUMP_PATTERNS.put("waterTemperatureCelsius", Pattern.compile("(?:水温|温度|Temperature|T)[:：]?\\s*(\\d+(?:\\.\\d+)?)\\s*(?:°C|摄氏度|℃)", Pattern.CASE_INSENSITIVE));

        // 振动模式
        PUMP_PATTERNS.put("vibrationMmS", Pattern.compile("(?:振动|Vibration|Vib)[:：]?\\s*(\\d+(?:\\.\\d+)?)\\s*(?:mm/s|毫米/秒)", Pattern.CASE_INSENSITIVE));

        // 噪音模式
        PUMP_PATTERNS.put("noiseLevelDb", Pattern.compile("(?:噪音|噪声|Noise|N)[:：]?\\s*(\\d+(?:\\.\\d+)?)\\s*(?:dB|分贝)", Pattern.CASE_INSENSITIVE));

        // 故障代码模式
        PUMP_PATTERNS.put("faultCode", Pattern.compile("(?:故障代码|Fault|Error|报警)[:：]?\\s*([A-Za-z0-9_\\-]+)", Pattern.CASE_INSENSITIVE));

        // 报警级别模式
        PUMP_PATTERNS.put("alarmLevel", Pattern.compile("(?:报警级别|Alarm|Level|等级)[:：]?\\s*(\\d+)", Pattern.CASE_INSENSITIVE));
    }

    /**
     * 从消息数据中解析水泵数据
     */
    public List<PumpDataEntity> parsePumpData(MessageDataDTO messageData) {
        List<PumpDataEntity> pumpDataList = new ArrayList<>();

        try {
            String messageContent = messageData.getMessageContent();
            if (messageContent == null || messageContent.trim().isEmpty()) {
                log.debug("消息内容为空，跳过解析");
                return pumpDataList;
            }

            // 解析基础字段
            String deviceId = extractDeviceId(messageContent, messageData.getDeviceId());
            LocalDateTime timestamp = extractTimestamp(messageContent, messageData.getTimestamp());

            // 检查是否为水泵相关数据
            if (!isPumpRelatedMessage(messageContent)) {
                log.debug("非水泵相关消息，跳过解析: {}", messageContent.substring(0, Math.min(100, messageContent.length())));
                return pumpDataList;
            }

            // 解析运行数据
            PumpDataEntity pumpData = parseOperationalData(messageContent, deviceId, timestamp, messageData);
            if (pumpData != null) {
                pumpDataList.add(pumpData);
            }

            log.debug("成功解析水泵数据，设备: {}, 字段数: {}", deviceId, countNonNullFields(pumpData));

        } catch (Exception e) {
            log.error("解析水泵数据失败", e);
        }

        return pumpDataList;
    }

    /**
     * 检查是否为水泵相关消息
     */
    private boolean isPumpRelatedMessage(String messageContent) {
        String lowerContent = messageContent.toLowerCase();
        return lowerContent.contains("pump") || lowerContent.contains("水泵") ||
               lowerContent.contains("water pump") || lowerContent.contains("给水泵") ||
               lowerContent.contains("排水泵") || lowerContent.contains("消防泵");
    }

    /**
     * 解析运行数据
     */
    private PumpDataEntity parseOperationalData(String messageContent, String deviceId, LocalDateTime timestamp, MessageDataDTO messageData) {
        PumpDataEntity pumpData = new PumpDataEntity();
        pumpData.setDeviceId(deviceId);
        pumpData.setTimestamp(timestamp);
        pumpData.setRawMessage(messageContent);
        pumpData.setDataSource("PARSED");

        // 解析水泵状态
        pumpData.setPumpStatus(extractPumpStatus(messageContent));

        // 解析运行时间
        pumpData.setRuntimeMinutes(extractDoubleValue(messageContent, PUMP_PATTERNS.get("runtimeMinutes")));

        // 解析电流
        pumpData.setCurrentAmperage(extractDoubleValue(messageContent, PUMP_PATTERNS.get("currentAmperage")));

        // 解析电压
        pumpData.setVoltage(extractDoubleValue(messageContent, PUMP_PATTERNS.get("voltage")));

        // 解析功率
        pumpData.setPowerKw(extractDoubleValue(messageContent, PUMP_PATTERNS.get("powerKw")));

        // 解析能耗
        pumpData.setEnergyConsumptionKwh(extractDoubleValue(messageContent, PUMP_PATTERNS.get("energyConsumptionKwh")));

        // 解析水压
        pumpData.setWaterPressureKpa(extractDoubleValue(messageContent, PUMP_PATTERNS.get("waterPressureKpa")));

        // 解析流量
        pumpData.setFlowRateM3h(extractDoubleValue(messageContent, PUMP_PATTERNS.get("flowRateM3h")));

        // 解析水温
        pumpData.setWaterTemperatureCelsius(extractDoubleValue(messageContent, PUMP_PATTERNS.get("waterTemperatureCelsius")));

        // 解析振动
        pumpData.setVibrationMmS(extractDoubleValue(messageContent, PUMP_PATTERNS.get("vibrationMmS")));

        // 解析噪音
        pumpData.setNoiseLevelDb(extractDoubleValue(messageContent, PUMP_PATTERNS.get("noiseLevelDb")));

        // 解析故障代码
        pumpData.setFaultCode(extractStringValue(messageContent, PUMP_PATTERNS.get("faultCode")));

        // 解析报警级别
        pumpData.setAlarmLevel(extractIntegerValue(messageContent, PUMP_PATTERNS.get("alarmLevel")));

        // 根据故障代码和报警级别设置维护标志
        if (pumpData.getFaultCode() != null || (pumpData.getAlarmLevel() != null && pumpData.getAlarmLevel() > 1)) {
            pumpData.setMaintenanceFlag(true);
        }

        return pumpData;
    }

    /**
     * 提取设备ID
     */
    private String extractDeviceId(String messageContent, String fallbackDeviceId) {
        String deviceId = extractStringValue(messageContent, PUMP_PATTERNS.get("deviceId"));
        if (deviceId == null || deviceId.trim().isEmpty()) {
            deviceId = fallbackDeviceId;
        }
        return deviceId;
    }

    /**
     * 提取时间戳
     */
    private LocalDateTime extractTimestamp(String messageContent, String fallbackTimestamp) {
        String timestampStr = extractStringValue(messageContent, PUMP_PATTERNS.get("timestamp"));
        if (timestampStr != null && !timestampStr.trim().isEmpty()) {
            try {
                return LocalDateTime.parse(timestampStr, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            } catch (Exception e) {
                log.debug("解析时间戳失败: {}", timestampStr);
            }
        }

        // 尝试从fallback解析
        if (fallbackTimestamp != null && !fallbackTimestamp.trim().isEmpty()) {
            try {
                return LocalDateTime.parse(fallbackTimestamp, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            } catch (Exception e) {
                log.debug("解析fallback时间戳失败: {}", fallbackTimestamp);
            }
        }

        // 返回当前时间
        return LocalDateTime.now();
    }

    /**
     * 解析水泵状态
     */
    private Integer extractPumpStatus(String messageContent) {
        String statusStr = extractStringValue(messageContent, PUMP_PATTERNS.get("pumpStatus"));
        if (statusStr == null) {
            return null;
        }

        String lowerStatus = statusStr.toLowerCase();
        if (lowerStatus.contains("启动") || lowerStatus.contains("运行") || lowerStatus.contains("on") || lowerStatus.equals("1")) {
            return 1;
        } else if (lowerStatus.contains("停止") || lowerStatus.contains("关闭") || lowerStatus.contains("off") || lowerStatus.equals("0")) {
            return 0;
        }

        return null;
    }

    /**
     * 提取字符串值
     */
    private String extractStringValue(String content, Pattern pattern) {
        if (pattern == null) {
            return null;
        }

        Matcher matcher = pattern.matcher(content);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return null;
    }

    /**
     * 提取整数值
     */
    private Integer extractIntegerValue(String content, Pattern pattern) {
        String valueStr = extractStringValue(content, pattern);
        if (valueStr == null) {
            return null;
        }

        try {
            return Integer.parseInt(valueStr);
        } catch (NumberFormatException e) {
            log.debug("解析整数值失败: {}", valueStr);
            return null;
        }
    }

    /**
     * 提取双精度浮点数值
     */
    private Double extractDoubleValue(String content, Pattern pattern) {
        String valueStr = extractStringValue(content, pattern);
        if (valueStr == null) {
            return null;
        }

        try {
            return Double.parseDouble(valueStr);
        } catch (NumberFormatException e) {
            log.debug("解析浮点数值失败: {}", valueStr);
            return null;
        }
    }

    /**
     * 计算非空字段数量
     */
    private int countNonNullFields(PumpDataEntity pumpData) {
        if (pumpData == null) {
            return 0;
        }

        int count = 0;
        if (pumpData.getPumpStatus() != null) count++;
        if (pumpData.getRuntimeMinutes() != null) count++;
        if (pumpData.getCurrentAmperage() != null) count++;
        if (pumpData.getVoltage() != null) count++;
        if (pumpData.getPowerKw() != null) count++;
        if (pumpData.getEnergyConsumptionKwh() != null) count++;
        if (pumpData.getWaterPressureKpa() != null) count++;
        if (pumpData.getFlowRateM3h() != null) count++;
        if (pumpData.getWaterTemperatureCelsius() != null) count++;
        if (pumpData.getVibrationMmS() != null) count++;
        if (pumpData.getNoiseLevelDb() != null) count++;
        if (pumpData.getFaultCode() != null) count++;
        if (pumpData.getAlarmLevel() != null) count++;

        return count;
    }

    /**
     * 批量解析消息数据
     */
    public List<PumpDataEntity> parseBatch(List<MessageDataDTO> messageDataList) {
        List<PumpDataEntity> allPumpData = new ArrayList<>();

        for (MessageDataDTO messageData : messageDataList) {
            List<PumpDataEntity> pumpDataList = parsePumpData(messageData);
            allPumpData.addAll(pumpDataList);
        }

        log.info("批量解析完成，总消息数: {}, 解析出水泵数据记录数: {}", messageDataList.size(), allPumpData.size());
        return allPumpData;
    }

    /**
     * 验证解析的数据质量
     */
    public boolean validatePumpData(PumpDataEntity pumpData) {
        if (pumpData == null) {
            return false;
        }

        // 检查必要字段
        if (pumpData.getDeviceId() == null || pumpData.getDeviceId().trim().isEmpty()) {
            return false;
        }

        if (pumpData.getTimestamp() == null) {
            return false;
        }

        // 检查数据合理性
        if (pumpData.getCurrentAmperage() != null && (pumpData.getCurrentAmperage() < 0 || pumpData.getCurrentAmperage() > 1000)) {
            log.warn("电流值异常: {}", pumpData.getCurrentAmperage());
            return false;
        }

        if (pumpData.getVoltage() != null && (pumpData.getVoltage() < 0 || pumpData.getVoltage() > 10000)) {
            log.warn("电压值异常: {}", pumpData.getVoltage());
            return false;
        }

        if (pumpData.getPowerKw() != null && (pumpData.getPowerKw() < 0 || pumpData.getPowerKw() > 10000)) {
            log.warn("功率值异常: {}", pumpData.getPowerKw());
            return false;
        }

        if (pumpData.getVibrationMmS() != null && (pumpData.getVibrationMmS() < 0 || pumpData.getVibrationMmS() > 100)) {
            log.warn("振动值异常: {}", pumpData.getVibrationMmS());
            return false;
        }

        return true;
    }

    /**
     * 获取解析统计信息
     */
    public Map<String, Object> getParsingStatistics(List<MessageDataDTO> messageDataList) {
        Map<String, Object> stats = new HashMap<>();

        int totalMessages = messageDataList.size();
        int pumpRelatedMessages = 0;
        int successfullyParsed = 0;
        int totalRecords = 0;

        for (MessageDataDTO messageData : messageDataList) {
            if (isPumpRelatedMessage(messageData.getMessageContent())) {
                pumpRelatedMessages++;
                List<PumpDataEntity> pumpDataList = parsePumpData(messageData);
                if (!pumpDataList.isEmpty()) {
                    successfullyParsed++;
                    totalRecords += pumpDataList.size();
                }
            }
        }

        stats.put("totalMessages", totalMessages);
        stats.put("pumpRelatedMessages", pumpRelatedMessages);
        stats.put("successfullyParsed", successfullyParsed);
        stats.put("totalRecords", totalRecords);
        stats.put("successRate", totalMessages > 0 ? (double) successfullyParsed / totalMessages : 0.0);
        stats.put("averageRecordsPerMessage", successfullyParsed > 0 ? (double) totalRecords / successfullyParsed : 0.0);

        return stats;
    }
}