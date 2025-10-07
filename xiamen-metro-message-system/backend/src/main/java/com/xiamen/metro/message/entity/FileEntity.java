package com.xiamen.metro.message.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 文件实体类
 *
 * @author Xiamen Metro System
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "files")
@EntityListeners(AuditingEntityListener.class)
public class FileEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 文件名
     */
    @Column(nullable = false, length = 255)
    private String fileName;

    /**
     * 原始文件名
     */
    @Column(nullable = false, length = 255)
    private String originalFileName;

    /**
     * 文件扩展名
     */
    @Column(nullable = false, length = 10)
    private String fileExtension;

    /**
     * 文件大小（字节）
     */
    @Column(nullable = false)
    private Long fileSize;

    /**
     * 文件类型 (EXCEL, CSV)
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FileType fileType;

    /**
     * MIME类型
     */
    @Column(length = 100)
    private String mimeType;

    /**
     * MinIO存储路径
     */
    @Column(length = 500)
    private String storagePath;

    /**
     * 文件哈希值（MD5）
     */
    @Column(length = 32)
    private String fileHash;

    /**
     * 上传状态
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UploadStatus uploadStatus = UploadStatus.PENDING;

    /**
     * 处理状态
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProcessStatus processStatus = ProcessStatus.PENDING;

    /**
     * 处理错误信息
     */
    @Column(length = 1000)
    private String errorMessage;

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
     * 上传用户ID
     */
    @Column(name = "uploaded_by")
    private Long uploadedBy;

    /**
     * 创建时间
     */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * 文件类型枚举
     */
    public enum FileType {
        EXCEL,
        CSV
    }

    /**
     * 上传状态枚举
     */
    public enum UploadStatus {
        PENDING,    // 等待上传
        UPLOADING,  // 上传中
        COMPLETED,  // 上传完成
        FAILED      // 上传失败
    }

    /**
     * 处理状态枚举
     */
    public enum ProcessStatus {
        PENDING,    // 等待处理
        PROCESSING, // 处理中
        COMPLETED,  // 处理完成
        FAILED      // 处理失败
    }
}