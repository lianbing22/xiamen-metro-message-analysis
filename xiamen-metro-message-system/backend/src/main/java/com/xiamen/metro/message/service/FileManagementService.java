package com.xiamen.metro.message.service;

import com.xiamen.metro.message.dto.FileListDTO;
import com.xiamen.metro.message.dto.FileUploadDTO;
import com.xiamen.metro.message.entity.FileEntity;
import com.xiamen.metro.message.repository.FileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 文件管理服务
 *
 * @author Xiamen Metro System
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileManagementService {

    private final FileRepository fileRepository;
    private final MinioService minioService;
    private final FileParseService fileParseService;

    /**
     * 最大文件大小 (100MB)
     */
    @Value("${file.max-size:104857600}")
    private long maxFileSize;

    /**
     * 允许的文件扩展名
     */
    private static final List<String> ALLOWED_EXTENSIONS = List.of("xlsx", "xls", "csv");

    /**
     * 允许的MIME类型
     */
    private static final List<String> ALLOWED_MIME_TYPES = List.of(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/vnd.ms-excel",
            "text/csv",
            "application/csv"
    );

    /**
     * 上传文件
     *
     * @param multipartFile 上传的文件
     * @param uploadedBy    上传用户ID
     * @return 文件实体
     */
    @Transactional
    public FileEntity uploadFile(MultipartFile multipartFile, Long uploadedBy) {
        log.info("开始上传文件: {}, 大小: {} bytes", multipartFile.getOriginalFilename(), multipartFile.getSize());

        try {
            // 验证文件
            validateFile(multipartFile);

            // 生成文件信息
            String originalFileName = multipartFile.getOriginalFilename();
            String fileExtension = getFileExtension(originalFileName);
            String fileName = generateFileName(originalFileName);
            FileEntity.FileType fileType = getFileType(fileExtension);
            String mimeType = multipartFile.getContentType();

            // 计算文件哈希值
            String fileHash = calculateFileHash(multipartFile.getInputStream());

            // 检查文件是否已存在
            Optional<FileEntity> existingFile = fileRepository.findByFileHash(fileHash);
            if (existingFile.isPresent()) {
                log.info("文件已存在，跳过上传: {}", existingFile.get().getFileName());
                return existingFile.get();
            }

            // 创建文件记录
            FileEntity fileEntity = new FileEntity();
            fileEntity.setFileName(fileName);
            fileEntity.setOriginalFileName(originalFileName);
            fileEntity.setFileExtension(fileExtension);
            fileEntity.setFileSize(multipartFile.getSize());
            fileEntity.setFileType(fileType);
            fileEntity.setMimeType(mimeType);
            fileEntity.setFileHash(fileHash);
            fileEntity.setUploadStatus(FileEntity.UploadStatus.UPLOADING);
            fileEntity.setProcessStatus(FileEntity.ProcessStatus.PENDING);
            fileEntity.setUploadedBy(uploadedBy);

            // 保存文件记录
            fileEntity = fileRepository.save(fileEntity);

            try {
                // 生成MinIO对象名称
                String objectName = minioService.generateObjectName(fileName, fileExtension);

                // 上传到MinIO
                String storagePath = minioService.uploadFile(
                        multipartFile.getInputStream(),
                        objectName,
                        mimeType,
                        multipartFile.getSize()
                );

                // 更新文件记录
                fileEntity.setStoragePath(storagePath);
                fileEntity.setUploadStatus(FileEntity.UploadStatus.COMPLETED);
                fileEntity = fileRepository.save(fileEntity);

                log.info("文件上传成功: {}", fileName);

                // 异步处理文件
                processFileAsync(fileEntity);

                return fileEntity;

            } catch (Exception e) {
                // 更新上传状态为失败
                fileEntity.setUploadStatus(FileEntity.UploadStatus.FAILED);
                fileEntity.setErrorMessage("上传失败: " + e.getMessage());
                fileRepository.save(fileEntity);
                throw e;
            }

        } catch (Exception e) {
            log.error("上传文件失败: {}", e.getMessage(), e);
            throw new RuntimeException("上传文件失败: " + e.getMessage());
        }
    }

    /**
     * 异步处理文件
     */
    @Transactional
    public void processFileAsync(FileEntity fileEntity) {
        try {
            log.info("开始处理文件: {}", fileEntity.getFileName());

            // 更新处理状态
            fileEntity.setProcessStatus(FileEntity.ProcessStatus.PROCESSING);
            fileRepository.save(fileEntity);

            // 从MinIO下载文件
            String objectName = extractObjectNameFromPath(fileEntity.getStoragePath());
            InputStream inputStream = minioService.downloadFile(objectName);

            // 解析文件
            FileParseService.ParseResult parseResult;
            if (fileEntity.getFileType() == FileEntity.FileType.EXCEL) {
                parseResult = fileParseService.parseExcel(inputStream, fileEntity.getOriginalFileName());
            } else {
                parseResult = fileParseService.parseCSV(inputStream, fileEntity.getOriginalFileName());
            }

            // 更新处理结果
            fileEntity.setDataRowCount(parseResult.getTotalRows());
            fileEntity.setValidMessageCount(parseResult.getValidMessages().size());
            fileEntity.setInvalidMessageCount(parseResult.getInvalidMessages().size());
            fileEntity.setProcessStatus(FileEntity.ProcessStatus.COMPLETED);

            fileRepository.save(fileEntity);

            // TODO: 保存报文数据到数据库
            // saveMessageData(parseResult.getValidMessages());

            log.info("文件处理完成: {}, 总行数: {}, 有效报文: {}, 无效报文: {}",
                    fileEntity.getFileName(),
                    parseResult.getTotalRows(),
                    parseResult.getValidMessages().size(),
                    parseResult.getInvalidMessages().size());

        } catch (Exception e) {
            log.error("处理文件失败: {}", e.getMessage(), e);
            fileEntity.setProcessStatus(FileEntity.ProcessStatus.FAILED);
            fileEntity.setErrorMessage("处理失败: " + e.getMessage());
            fileRepository.save(fileEntity);
        }
    }

    /**
     * 获取文件列表
     *
     * @param page     页码
     * @param size     每页大小
     * @param keyword  搜索关键词
     * @param fileType 文件类型
     * @return 文件列表
     */
    public Page<FileListDTO> getFileList(int page, int size, String keyword, FileEntity.FileType fileType) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<FileEntity> filePage;

        if (keyword != null && !keyword.trim().isEmpty()) {
            filePage = fileRepository.findByOriginalFileNameContainingIgnoreCase(keyword.trim(), pageable);
        } else if (fileType != null) {
            filePage = fileRepository.findByFileType(fileType, pageable);
        } else {
            filePage = fileRepository.findAll(pageable);
        }

        return filePage.map(this::convertToFileListDTO);
    }

    /**
     * 获取文件详情
     *
     * @param fileId 文件ID
     * @return 文件实体
     */
    public Optional<FileEntity> getFileById(Long fileId) {
        return fileRepository.findById(fileId);
    }

    /**
     * 下载文件
     *
     * @param fileId 文件ID
     * @return 文件输入流
     */
    public InputStream downloadFile(Long fileId) {
        Optional<FileEntity> fileEntity = fileRepository.findById(fileId);
        if (fileEntity.isEmpty()) {
            throw new RuntimeException("文件不存在");
        }

        FileEntity file = fileEntity.get();
        String objectName = extractObjectNameFromPath(file.getStoragePath());
        return minioService.downloadFile(objectName);
    }

    /**
     * 删除文件
     *
     * @param fileId 文件ID
     */
    @Transactional
    public void deleteFile(Long fileId) {
        Optional<FileEntity> fileEntity = fileRepository.findById(fileId);
        if (fileEntity.isEmpty()) {
            throw new RuntimeException("文件不存在");
        }

        FileEntity file = fileEntity.get();

        try {
            // 从MinIO删除文件
            if (file.getStoragePath() != null) {
                String objectName = extractObjectNameFromPath(file.getStoragePath());
                minioService.deleteFile(objectName);
            }

            // 从数据库删除记录
            fileRepository.delete(file);

            log.info("文件删除成功: {}", file.getFileName());

        } catch (Exception e) {
            log.error("删除文件失败: {}", e.getMessage(), e);
            throw new RuntimeException("删除文件失败: " + e.getMessage());
        }
    }

    /**
     * 验证文件
     */
    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new RuntimeException("文件不能为空");
        }

        if (file.getSize() > maxFileSize) {
            throw new RuntimeException("文件大小超过限制: " + formatFileSize(maxFileSize));
        }

        String originalFileName = file.getOriginalFilename();
        if (originalFileName == null || originalFileName.trim().isEmpty()) {
            throw new RuntimeException("文件名不能为空");
        }

        String fileExtension = getFileExtension(originalFileName);
        if (!ALLOWED_EXTENSIONS.contains(fileExtension.toLowerCase())) {
            throw new RuntimeException("不支持的文件类型: " + fileExtension);
        }

        String mimeType = file.getContentType();
        if (!ALLOWED_MIME_TYPES.contains(mimeType)) {
            throw new RuntimeException("不支持的MIME类型: " + mimeType);
        }
    }

    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String fileName) {
        int lastDotIndex = fileName.lastIndexOf('.');
        return lastDotIndex > 0 ? fileName.substring(lastDotIndex + 1).toLowerCase() : "";
    }

    /**
     * 生成文件名
     */
    private String generateFileName(String originalFileName) {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String fileExtension = getFileExtension(originalFileName);
        String baseName = originalFileName.substring(0, originalFileName.lastIndexOf('.'));
        return baseName + "_" + timestamp + "." + fileExtension;
    }

    /**
     * 获取文件类型
     */
    private FileEntity.FileType getFileType(String fileExtension) {
        switch (fileExtension.toLowerCase()) {
            case "xlsx":
            case "xls":
                return FileEntity.FileType.EXCEL;
            case "csv":
                return FileEntity.FileType.CSV;
            default:
                throw new RuntimeException("不支持的文件类型: " + fileExtension);
        }
    }

    /**
     * 计算文件哈希值
     */
    private String calculateFileHash(InputStream inputStream) throws NoSuchAlgorithmException, IOException {
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] buffer = new byte[8192];
        int bytesRead;

        while ((bytesRead = inputStream.read(buffer)) != -1) {
            md.update(buffer, 0, bytesRead);
        }

        // 重置输入流位置
        inputStream.reset();

        byte[] hashBytes = md.digest();
        StringBuilder sb = new StringBuilder();
        for (byte b : hashBytes) {
            sb.append(String.format("%02x", b));
        }

        return sb.toString();
    }

    /**
     * 从存储路径提取对象名称
     */
    private String extractObjectNameFromPath(String storagePath) {
        // MinIO的Presigned URL格式，需要提取对象名称
        int index = storagePath.indexOf("metro-files/");
        return index != -1 ? storagePath.substring(index) : storagePath;
    }

    /**
     * 转换为文件列表DTO
     */
    private FileListDTO convertToFileListDTO(FileEntity file) {
        FileListDTO dto = new FileListDTO();
        dto.setId(file.getId());
        dto.setFileName(file.getFileName());
        dto.setOriginalFileName(file.getOriginalFileName());
        dto.setFileType(file.getFileType());
        dto.setFileSize(file.getFileSize());
        dto.setFormattedFileSize(formatFileSize(file.getFileSize()));
        dto.setUploadStatus(file.getUploadStatus());
        dto.setProcessStatus(file.getProcessStatus());
        dto.setDataRowCount(file.getDataRowCount());
        dto.setValidMessageCount(file.getValidMessageCount());
        dto.setInvalidMessageCount(file.getInvalidMessageCount());
        dto.setErrorMessage(file.getErrorMessage());
        dto.setCreatedAt(file.getCreatedAt());
        dto.setUpdatedAt(file.getUpdatedAt());
        return dto;
    }

    /**
     * 格式化文件大小
     */
    private String formatFileSize(long size) {
        if (size < 1024) {
            return size + " B";
        } else if (size < 1024 * 1024) {
            return String.format("%.1f KB", size / 1024.0);
        } else if (size < 1024 * 1024 * 1024) {
            return String.format("%.1f MB", size / (1024.0 * 1024.0));
        } else {
            return String.format("%.1f GB", size / (1024.0 * 1024.0 * 1024.0));
        }
    }
}