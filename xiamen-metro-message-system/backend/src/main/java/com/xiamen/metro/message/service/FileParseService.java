package com.xiamen.metro.message.service;

import com.xiamen.metro.message.dto.MessageDataDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.opencsv.CSVReader;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 文件解析服务
 *
 * @author Xiamen Metro System
 */
@Slf4j
@Service
public class FileParseService {

    /**
     * 最大文件行数限制
     */
    private static final int MAX_ROWS = 100000;

    /**
     * 解析Excel文件
     *
     * @param inputStream 文件输入流
     * @param fileName    文件名
     * @return 解析结果
     */
    public ParseResult parseExcel(InputStream inputStream, String fileName) {
        log.info("开始解析Excel文件: {}", fileName);

        List<MessageDataDTO> validMessages = new ArrayList<>();
        List<MessageDataDTO> invalidMessages = new ArrayList<>();
        int totalRows = 0;

        try (Workbook workbook = WorkbookFactory.create(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);

            // 检查最大行数限制
            int lastRowNum = sheet.getLastRowNum();
            if (lastRowNum > MAX_ROWS) {
                throw new RuntimeException("Excel文件行数超过限制: " + MAX_ROWS);
            }

            // 获取表头
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                throw new RuntimeException("Excel文件没有表头");
            }

            // 解析表头，获取列索引
            int deviceIdCol = -1;
            int timestampCol = -1;
            int messageTypeCol = -1;
            int messageContentCol = -1;

            for (Cell cell : headerRow) {
                String header = getCellValueAsString(cell).toLowerCase();
                switch (header) {
                    case "device_id":
                    case "设备id":
                    case "deviceid":
                        deviceIdCol = cell.getColumnIndex();
                        break;
                    case "timestamp":
                    case "时间戳":
                    case "时间":
                        timestampCol = cell.getColumnIndex();
                        break;
                    case "message_type":
                    case "消息类型":
                    case "报文类型":
                        messageTypeCol = cell.getColumnIndex();
                        break;
                    case "message_content":
                    case "消息内容":
                    case "报文内容":
                        messageContentCol = cell.getColumnIndex();
                        break;
                }
            }

            // 验证必要列是否存在
            if (deviceIdCol == -1 || timestampCol == -1 ||
                messageTypeCol == -1 || messageContentCol == -1) {
                throw new RuntimeException("Excel文件缺少必要的列: device_id, timestamp, message_type, message_content");
            }

            // 从第二行开始解析数据
            for (int i = 1; i <= lastRowNum; i++) {
                Row row = sheet.getRow(i);
                if (row == null) {
                    continue;
                }

                totalRows++;

                try {
                    MessageDataDTO message = new MessageDataDTO();
                    message.setDeviceId(getCellValueAsString(row.getCell(deviceIdCol)));
                    message.setTimestamp(getCellValueAsString(row.getCell(timestampCol)));
                    message.setMessageType(getCellValueAsString(row.getCell(messageTypeCol)));
                    message.setMessageContent(getCellValueAsString(row.getCell(messageContentCol)));

                    if (validateMessage(message)) {
                        validMessages.add(message);
                    } else {
                        invalidMessages.add(message);
                    }
                } catch (Exception e) {
                    log.warn("解析Excel第{}行数据失败: {}", i + 1, e.getMessage());
                    MessageDataDTO invalidMessage = new MessageDataDTO();
                    invalidMessage.setErrorMessage("解析失败: " + e.getMessage());
                    invalidMessages.add(invalidMessage);
                }
            }

            log.info("Excel文件解析完成: 总行数={}, 有效报文={}, 无效报文={}",
                    totalRows, validMessages.size(), invalidMessages.size());

            return new ParseResult(validMessages, invalidMessages, totalRows);

        } catch (Exception e) {
            log.error("解析Excel文件失败: {}", e.getMessage(), e);
            throw new RuntimeException("解析Excel文件失败: " + e.getMessage());
        }
    }

    /**
     * 解析CSV文件
     *
     * @param inputStream 文件输入流
     * @param fileName    文件名
     * @return 解析结果
     */
    public ParseResult parseCSV(InputStream inputStream, String fileName) {
        log.info("开始解析CSV文件: {}", fileName);

        List<MessageDataDTO> validMessages = new ArrayList<>();
        List<MessageDataDTO> invalidMessages = new ArrayList<>();
        int totalRows = 0;

        try (CSVReader reader = new CSVReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            // 读取表头
            String[] headers = reader.readNext();
            if (headers == null) {
                throw new RuntimeException("CSV文件为空");
            }

            // 解析表头，获取列索引
            int deviceIdCol = -1;
            int timestampCol = -1;
            int messageTypeCol = -1;
            int messageContentCol = -1;

            for (int i = 0; i < headers.length; i++) {
                String header = headers[i].toLowerCase();
                switch (header) {
                    case "device_id":
                    case "设备id":
                    case "deviceid":
                        deviceIdCol = i;
                        break;
                    case "timestamp":
                    case "时间戳":
                    case "时间":
                        timestampCol = i;
                        break;
                    case "message_type":
                    case "消息类型":
                    case "报文类型":
                        messageTypeCol = i;
                        break;
                    case "message_content":
                    case "消息内容":
                    case "报文内容":
                        messageContentCol = i;
                        break;
                }
            }

            // 验证必要列是否存在
            if (deviceIdCol == -1 || timestampCol == -1 ||
                messageTypeCol == -1 || messageContentCol == -1) {
                throw new RuntimeException("CSV文件缺少必要的列: device_id, timestamp, message_type, message_content");
            }

            // 读取数据行
            String[] row;
            while ((row = reader.readNext()) != null) {
                totalRows++;

                // 检查最大行数限制
                if (totalRows > MAX_ROWS) {
                    throw new RuntimeException("CSV文件行数超过限制: " + MAX_ROWS);
                }

                try {
                    MessageDataDTO message = new MessageDataDTO();
                    message.setDeviceId(getColumnValue(row, deviceIdCol));
                    message.setTimestamp(getColumnValue(row, timestampCol));
                    message.setMessageType(getColumnValue(row, messageTypeCol));
                    message.setMessageContent(getColumnValue(row, messageContentCol));

                    if (validateMessage(message)) {
                        validMessages.add(message);
                    } else {
                        invalidMessages.add(message);
                    }
                } catch (Exception e) {
                    log.warn("解析CSV第{}行数据失败: {}", totalRows, e.getMessage());
                    MessageDataDTO invalidMessage = new MessageDataDTO();
                    invalidMessage.setErrorMessage("解析失败: " + e.getMessage());
                    invalidMessages.add(invalidMessage);
                }
            }

            log.info("CSV文件解析完成: 总行数={}, 有效报文={}, 无效报文={}",
                    totalRows, validMessages.size(), invalidMessages.size());

            return new ParseResult(validMessages, invalidMessages, totalRows);

        } catch (Exception e) {
            log.error("解析CSV文件失败: {}", e.getMessage(), e);
            throw new RuntimeException("解析CSV文件失败: " + e.getMessage());
        }
    }

    /**
     * 获取单元格值作为字符串
     */
    private String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return "";
        }

        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                } else {
                    return String.valueOf((long) cell.getNumericCellValue());
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default:
                return "";
        }
    }

    /**
     * 获取列值
     */
    private String getColumnValue(String[] row, int index) {
        if (index < 0 || index >= row.length) {
            return "";
        }
        return row[index] != null ? row[index].trim() : "";
    }

    /**
     * 验证报文数据
     */
    private boolean validateMessage(MessageDataDTO message) {
        if (message.getDeviceId() == null || message.getDeviceId().trim().isEmpty()) {
            message.setErrorMessage("设备ID不能为空");
            return false;
        }

        if (message.getTimestamp() == null || message.getTimestamp().trim().isEmpty()) {
            message.setErrorMessage("时间戳不能为空");
            return false;
        }

        if (message.getMessageType() == null || message.getMessageType().trim().isEmpty()) {
            message.setErrorMessage("消息类型不能为空");
            return false;
        }

        if (message.getMessageContent() == null || message.getMessageContent().trim().isEmpty()) {
            message.setErrorMessage("消息内容不能为空");
            return false;
        }

        return true;
    }

    /**
     * 解析结果
     */
    public static class ParseResult {
        private final List<MessageDataDTO> validMessages;
        private final List<MessageDataDTO> invalidMessages;
        private final int totalRows;

        public ParseResult(List<MessageDataDTO> validMessages, List<MessageDataDTO> invalidMessages, int totalRows) {
            this.validMessages = validMessages;
            this.invalidMessages = invalidMessages;
            this.totalRows = totalRows;
        }

        public List<MessageDataDTO> getValidMessages() {
            return validMessages;
        }

        public List<MessageDataDTO> getInvalidMessages() {
            return invalidMessages;
        }

        public int getTotalRows() {
            return totalRows;
        }
    }
}