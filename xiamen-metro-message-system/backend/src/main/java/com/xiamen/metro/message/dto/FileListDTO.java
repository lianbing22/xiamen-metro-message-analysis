package com.xiamen.metro.message.dto;

import com.xiamen.metro.message.entity.FileEntity;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文件列表DTO
 *
 * @author Xiamen Metro System
 */
@Data
public class FileListDTO {

    /**
     * 文件ID
     */
    private Long id;

    /**
     * 文件名
     */
    private String fileName;

    /**
     * 原始文件名
     */
    private String originalFileName;

    /**
     * 文件类型
     */
    private FileEntity.FileType fileType;

    /**
     * 文件大小
     */
    private Long fileSize;

    /**
     * 文件大小（格式化）
     */
    private String formattedFileSize;

    /**
     * 上传状态
     */
    private FileEntity.UploadStatus uploadStatus;

    /**
     * 处理状态
     */
    private FileEntity.ProcessStatus processStatus;

    /**
     * 数据行数
     */
    private Integer dataRowCount;

    /**
     * 有效报文数量
     */
    private Integer validMessageCount;

    /**
     * 无效报文数量
     */
    private Integer invalidMessageCount;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 上传用户
     */
    private String uploadedBy;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}