package com.xiamen.metro.message.controller;

import com.xiamen.metro.message.dto.FileListDTO;
import com.xiamen.metro.message.entity.FileEntity;
import com.xiamen.metro.message.service.FileManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.Min;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * 文件管理控制器
 *
 * @author Xiamen Metro System
 */
@Slf4j
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
@Tag(name = "文件管理", description = "文件上传、下载、管理相关接口")
public class FileManagementController {

    private final FileManagementService fileManagementService;

    /**
     * 上传文件
     */
    @PostMapping("/upload")
    @Operation(summary = "上传文件", description = "支持Excel和CSV文件上传")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<FileEntity>> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {

        try {
            log.info("用户 {} 上传文件: {}", userId, file.getOriginalFilename());
            FileEntity fileEntity = fileManagementService.uploadFile(file, userId);
            return ResponseEntity.ok(ApiResponse.success(fileEntity, "文件上传成功"));
        } catch (Exception e) {
            log.error("文件上传失败: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("文件上传失败: " + e.getMessage()));
        }
    }

    /**
     * 获取文件列表
     */
    @GetMapping
    @Operation(summary = "获取文件列表", description = "分页获取文件列表，支持搜索和筛选")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<Page<FileListDTO>>> getFileList(
            @Parameter(description = "页码，从1开始") @RequestParam(defaultValue = "1") @Min(1) int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") @Min(1) int size,
            @Parameter(description = "搜索关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "文件类型") @RequestParam(required = false) FileEntity.FileType fileType) {

        try {
            Page<FileListDTO> fileList = fileManagementService.getFileList(page, size, keyword, fileType);
            return ResponseEntity.ok(ApiResponse.success(fileList, "获取文件列表成功"));
        } catch (Exception e) {
            log.error("获取文件列表失败: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("获取文件列表失败: " + e.getMessage()));
        }
    }

    /**
     * 获取文件详情
     */
    @GetMapping("/{fileId}")
    @Operation(summary = "获取文件详情", description = "根据文件ID获取文件详细信息")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<FileEntity>> getFileById(
            @Parameter(description = "文件ID") @PathVariable Long fileId) {

        try {
            Optional<FileEntity> fileEntity = fileManagementService.getFileById(fileId);
            if (fileEntity.isPresent()) {
                return ResponseEntity.ok(ApiResponse.success(fileEntity.get(), "获取文件详情成功"));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("文件不存在"));
            }
        } catch (Exception e) {
            log.error("获取文件详情失败: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("获取文件详情失败: " + e.getMessage()));
        }
    }

    /**
     * 下载文件
     */
    @GetMapping("/{fileId}/download")
    @Operation(summary = "下载文件", description = "根据文件ID下载原文件")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<InputStreamResource> downloadFile(
            @Parameter(description = "文件ID") @PathVariable Long fileId) {

        try {
            Optional<FileEntity> fileEntity = fileManagementService.getFileById(fileId);
            if (fileEntity.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            FileEntity file = fileEntity.get();
            InputStream inputStream = fileManagementService.downloadFile(fileId);

            String encodedFileName = URLEncoder.encode(file.getOriginalFileName(), StandardCharsets.UTF_8);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + encodedFileName + "\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(new InputStreamResource(inputStream));

        } catch (Exception e) {
            log.error("下载文件失败: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 删除文件
     */
    @DeleteMapping("/{fileId}")
    @Operation(summary = "删除文件", description = "根据文件ID删除文件")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteFile(
            @Parameter(description = "文件ID") @PathVariable Long fileId) {

        try {
            fileManagementService.deleteFile(fileId);
            return ResponseEntity.ok(ApiResponse.success(null, "文件删除成功"));
        } catch (Exception e) {
            log.error("删除文件失败: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("删除文件失败: " + e.getMessage()));
        }
    }

    /**
     * 批量删除文件
     */
    @DeleteMapping("/batch")
    @Operation(summary = "批量删除文件", description = "批量删除多个文件")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> batchDeleteFiles(
            @Parameter(description = "文件ID列表") @RequestBody java.util.List<Long> fileIds) {

        try {
            for (Long fileId : fileIds) {
                fileManagementService.deleteFile(fileId);
            }
            return ResponseEntity.ok(ApiResponse.success(null, "批量删除文件成功"));
        } catch (Exception e) {
            log.error("批量删除文件失败: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("批量删除文件失败: " + e.getMessage()));
        }
    }

    /**
     * API响应封装类
     */
    public static class ApiResponse<T> {
        private boolean success;
        private String message;
        private T data;
        private long timestamp;

        public ApiResponse(boolean success, String message, T data) {
            this.success = success;
            this.message = message;
            this.data = data;
            this.timestamp = System.currentTimeMillis();
        }

        public static <T> ApiResponse<T> success(T data, String message) {
            return new ApiResponse<>(true, message, data);
        }

        public static <T> ApiResponse<T> error(String message) {
            return new ApiResponse<>(false, message, null);
        }

        // Getters and Setters
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public T getData() { return data; }
        public void setData(T data) { this.data = data; }

        public long getTimestamp() { return timestamp; }
        public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    }
}