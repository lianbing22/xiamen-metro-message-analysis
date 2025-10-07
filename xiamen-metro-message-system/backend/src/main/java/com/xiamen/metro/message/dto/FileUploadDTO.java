package com.xiamen.metro.message.dto;

import com.xiamen.metro.message.entity.FileEntity;
import lombok.Data;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 文件上传DTO
 *
 * @author Xiamen Metro System
 */
@Data
public class FileUploadDTO {

    /**
     * 文件名
     */
    @NotNull(message = "文件名不能为空")
    @Size(max = 255, message = "文件名长度不能超过255个字符")
    private String fileName;

    /**
     * 原始文件名
     */
    @NotNull(message = "原始文件名不能为空")
    @Size(max = 255, message = "原始文件名长度不能超过255个字符")
    private String originalFileName;

    /**
     * 文件扩展名
     */
    @NotNull(message = "文件扩展名不能为空")
    private String fileExtension;

    /**
     * 文件大小
     */
    @NotNull(message = "文件大小不能为空")
    private Long fileSize;

    /**
     * 文件类型
     */
    @NotNull(message = "文件类型不能为空")
    private FileEntity.FileType fileType;

    /**
     * MIME类型
     */
    private String mimeType;

    /**
     * 文件哈希值
     */
    @Size(max = 32, message = "文件哈希值长度不能超过32个字符")
    private String fileHash;
}