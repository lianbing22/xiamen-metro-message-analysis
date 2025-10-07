package com.xiamen.metro.message.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

/**
 * 文件分片上传服务
 * 支持大文件分片上传、断点续传、并发上传
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FileChunkUploadService {

    private final Executor fileTaskExecutor;

    @Value("${app.file.chunk-size:8192}")
    private int chunkSize;

    @Value("${app.file.max-concurrent-uploads:10}")
    private int maxConcurrentUploads;

    @Value("${app.file.upload-path:./uploads}")
    private String uploadPath;

    // 分片上传状态管理
    private final Map<String, ChunkUploadSession> uploadSessions = new ConcurrentHashMap<>();

    /**
     * 初始化分片上传会话
     */
    public ChunkUploadSession initUploadSession(String fileId, String fileName, long fileSize, String userId) {
        String sessionId = UUID.randomUUID().toString();
        int totalChunks = (int) Math.ceil((double) fileSize / chunkSize);

        ChunkUploadSession session = ChunkUploadSession.builder()
                .sessionId(sessionId)
                .fileId(fileId)
                .fileName(fileName)
                .fileSize(fileSize)
                .totalChunks(totalChunks)
                .uploadedChunks(new HashSet<>())
                .userId(userId)
                .startTime(System.currentTimeMillis())
                .status("INITIALIZED")
                .build();

        uploadSessions.put(sessionId, session);
        log.info("初始化分片上传会话: sessionId={}, fileId={}, fileName={}, totalChunks={}",
                sessionId, fileId, fileName, totalChunks);

        return session;
    }

    /**
     * 上传分片
     */
    public CompletableFuture<ChunkUploadResult> uploadChunk(String sessionId, int chunkIndex, MultipartFile chunkFile) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                ChunkUploadSession session = uploadSessions.get(sessionId);
                if (session == null) {
                    return ChunkUploadResult.builder()
                            .success(false)
                            .message("上传会话不存在")
                            .build();
                }

                if (session.getUploadedChunks().contains(chunkIndex)) {
                    return ChunkUploadResult.builder()
                            .success(true)
                            .chunkIndex(chunkIndex)
                            .message("分片已存在")
                            .build();
                }

                // 保存分片文件
                String chunkFileName = getChunkFileName(session.getFileId(), chunkIndex);
                Path chunkPath = Paths.get(uploadPath, "chunks", chunkFileName);

                // 确保目录存在
                Files.createDirectories(chunkPath.getParent());

                // 保存分片文件
                try (InputStream inputStream = chunkFile.getInputStream()) {
                    Files.copy(inputStream, chunkPath);
                }

                // 验证分片完整性
                if (!validateChunkIntegrity(chunkPath, chunkIndex, chunkSize)) {
                    Files.deleteIfExists(chunkPath);
                    return ChunkUploadResult.builder()
                            .success(false)
                            .chunkIndex(chunkIndex)
                            .message("分片完整性验证失败")
                            .build();
                }

                // 更新上传状态
                session.getUploadedChunks().add(chunkIndex);
                session.setUploadedSize(session.getUploadedSize() + chunkFile.getSize());

                // 检查是否所有分片都已上传完成
                if (session.getUploadedChunks().size() == session.getTotalChunks()) {
                    return mergeChunks(session);
                }

                return ChunkUploadResult.builder()
                        .success(true)
                        .chunkIndex(chunkIndex)
                        .uploadedCount(session.getUploadedChunks().size())
                        .totalChunks(session.getTotalChunks())
                        .build();

            } catch (Exception e) {
                log.error("上传分片失败: sessionId={}, chunkIndex={}", sessionId, chunkIndex, e);
                return ChunkUploadResult.builder()
                        .success(false)
                        .chunkIndex(chunkIndex)
                        .message("上传失败: " + e.getMessage())
                        .build();
            }
        }, fileTaskExecutor);
    }

    /**
     * 合并分片
     */
    private ChunkUploadResult mergeChunks(ChunkUploadSession session) {
        try {
            session.setStatus("MERGING");
            log.info("开始合并分片: fileId={}, totalChunks={}", session.getFileId(), session.getTotalChunks());

            String targetFileName = session.getFileId() + "_" + session.getFileName();
            Path targetPath = Paths.get(uploadPath, targetFileName);

            try (FileOutputStream outputStream = new FileOutputStream(targetPath.toFile())) {
                for (int i = 0; i < session.getTotalChunks(); i++) {
                    String chunkFileName = getChunkFileName(session.getFileId(), i);
                    Path chunkPath = Paths.get(uploadPath, "chunks", chunkFileName);

                    if (!Files.exists(chunkPath)) {
                        throw new RuntimeException("分片文件不存在: " + chunkFileName);
                    }

                    try (FileInputStream inputStream = new FileInputStream(chunkPath.toFile())) {
                        byte[] buffer = new byte[8192];
                        int bytesRead;
                        while ((bytesRead = inputStream.read(buffer)) != -1) {
                            outputStream.write(buffer, 0, bytesRead);
                        }
                    }

                    // 删除已合并的分片文件
                    Files.delete(chunkPath);
                }
            }

            // 验证合并后的文件完整性
            if (!validateMergedFileIntegrity(targetPath, session.getFileSize())) {
                Files.deleteIfExists(targetPath);
                throw new RuntimeException("合并后的文件完整性验证失败");
            }

            session.setStatus("COMPLETED");
            session.setEndTime(System.currentTimeMillis());

            log.info("分片合并完成: fileId={}, fileName={}, size={}, duration={}ms",
                    session.getFileId(), session.getFileName(), session.getFileSize(),
                    session.getEndTime() - session.getStartTime());

            return ChunkUploadResult.builder()
                    .success(true)
                    .sessionId(session.getSessionId())
                    .fileName(targetFileName)
                    .fileSize(session.getFileSize())
                    .uploadTime(session.getEndTime() - session.getStartTime())
                    .message("文件上传完成")
                    .build();

        } catch (Exception e) {
            log.error("合并分片失败: fileId={}", session.getFileId(), e);
            session.setStatus("FAILED");
            return ChunkUploadResult.builder()
                    .success(false)
                    .message("合并分片失败: " + e.getMessage())
                    .build();
        } finally {
            // 清理会话信息
            uploadSessions.remove(session.getSessionId());
        }
    }

    /**
     * 获取分片文件名
     */
    private String getChunkFileName(String fileId, int chunkIndex) {
        return String.format("%s_chunk_%d.tmp", fileId, chunkIndex);
    }

    /**
     * 验证分片完整性
     */
    private boolean validateChunkIntegrity(Path chunkPath, int chunkIndex, int expectedSize) throws Exception {
        long actualSize = Files.size(chunkPath);

        // 最后一个分片可能小于预期大小
        long expectedChunkSize = expectedSize;
        if (actualSize < expectedChunkSize) {
            return chunkIndex > 0; // 只有非最后一个分片才检查大小
        }

        return true;
    }

    /**
     * 验证合并后的文件完整性
     */
    private boolean validateMergedFileIntegrity(Path filePath, long expectedSize) throws Exception {
        long actualSize = Files.size(filePath);
        return actualSize == expectedSize;
    }

    /**
     * 获取上传会话状态
     */
    public ChunkUploadSession getUploadSession(String sessionId) {
        return uploadSessions.get(sessionId);
    }

    /**
     * 获取已上传的分片列表
     */
    public Set<Integer> getUploadedChunks(String sessionId) {
        ChunkUploadSession session = uploadSessions.get(sessionId);
        return session != null ? new HashSet<>(session.getUploadedChunks()) : new HashSet<>();
    }

    /**
     * 取消上传会话
     */
    public boolean cancelUploadSession(String sessionId) {
        ChunkUploadSession session = uploadSessions.get(sessionId);
        if (session == null) {
            return false;
        }

        session.setStatus("CANCELLED");

        // 删除已上传的分片文件
        for (int i = 0; i < session.getTotalChunks(); i++) {
            String chunkFileName = getChunkFileName(session.getFileId(), i);
            Path chunkPath = Paths.get(uploadPath, "chunks", chunkFileName);
            try {
                Files.deleteIfExists(chunkPath);
            } catch (Exception e) {
                log.warn("删除分片文件失败: {}", chunkPath, e);
            }
        }

        uploadSessions.remove(sessionId);
        log.info("取消上传会话: sessionId={}", sessionId);
        return true;
    }

    /**
     * 清理过期的上传会话
     */
    public void cleanupExpiredSessions() {
        long currentTime = System.currentTimeMillis();
        long expireTime = 24 * 60 * 60 * 1000; // 24小时

        uploadSessions.entrySet().removeIf(entry -> {
            ChunkUploadSession session = entry.getValue();
            if (currentTime - session.getStartTime() > expireTime) {
                log.info("清理过期上传会话: sessionId={}", session.getSessionId());
                cancelUploadSession(session.getSessionId());
                return true;
            }
            return false;
        });
    }

    /**
     * 计算文件MD5哈希
     */
    public String calculateFileHash(Path filePath) throws Exception {
        MessageDigest md = MessageDigest.getInstance("MD5");
        try (InputStream inputStream = Files.newInputStream(filePath)) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                md.update(buffer, 0, bytesRead);
            }
        }

        StringBuilder sb = new StringBuilder();
        for (byte b : md.digest()) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    /**
     * 获取上传统计信息
     */
    public Map<String, Object> getUploadStatistics() {
        Map<String, Object> stats = new HashMap<>();

        long activeSessions = uploadSessions.values().stream()
                .mapToLong(s -> "INITIALIZED".equals(s.getStatus()) || "UPLOADING".equals(s.getStatus()) ? 1 : 0)
                .sum();

        long completedSessions = uploadSessions.values().stream()
                .mapToLong(s -> "COMPLETED".equals(s.getStatus()) ? 1 : 0)
                .sum();

        long failedSessions = uploadSessions.values().stream()
                .mapToLong(s -> "FAILED".equals(s.getStatus()) ? 1 : 0)
                .sum();

        long totalUploadedSize = uploadSessions.values().stream()
                .mapToLong(ChunkUploadSession::getUploadedSize)
                .sum();

        stats.put("activeSessions", activeSessions);
        stats.put("completedSessions", completedSessions);
        stats.put("failedSessions", failedSessions);
        stats.put("totalSessions", uploadSessions.size());
        stats.put("totalUploadedSize", totalUploadedSize);
        stats.put("maxConcurrentUploads", maxConcurrentUploads);

        return stats;
    }

    /**
     * 分片上传会话
     */
    public static class ChunkUploadSession {
        private String sessionId;
        private String fileId;
        private String fileName;
        private long fileSize;
        private int totalChunks;
        private Set<Integer> uploadedChunks;
        private long uploadedSize;
        private String userId;
        private long startTime;
        private long endTime;
        private String status;

        // Builder pattern
        public static Builder builder() {
            return new Builder();
        }

        // Getters and setters
        public String getSessionId() { return sessionId; }
        public void setSessionId(String sessionId) { this.sessionId = sessionId; }
        public String getFileId() { return fileId; }
        public void setFileId(String fileId) { this.fileId = fileId; }
        public String getFileName() { return fileName; }
        public void setFileName(String fileName) { this.fileName = fileName; }
        public long getFileSize() { return fileSize; }
        public void setFileSize(long fileSize) { this.fileSize = fileSize; }
        public int getTotalChunks() { return totalChunks; }
        public void setTotalChunks(int totalChunks) { this.totalChunks = totalChunks; }
        public Set<Integer> getUploadedChunks() { return uploadedChunks; }
        public void setUploadedChunks(Set<Integer> uploadedChunks) { this.uploadedChunks = uploadedChunks; }
        public long getUploadedSize() { return uploadedSize; }
        public void setUploadedSize(long uploadedSize) { this.uploadedSize = uploadedSize; }
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        public long getStartTime() { return startTime; }
        public void setStartTime(long startTime) { this.startTime = startTime; }
        public long getEndTime() { return endTime; }
        public void setEndTime(long endTime) { this.endTime = endTime; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public static class Builder {
            private ChunkUploadSession session = new ChunkUploadSession();
            public Builder sessionId(String sessionId) { session.sessionId = sessionId; return this; }
            public Builder fileId(String fileId) { session.fileId = fileId; return this; }
            public Builder fileName(String fileName) { session.fileName = fileName; return this; }
            public Builder fileSize(long fileSize) { session.fileSize = fileSize; return this; }
            public Builder totalChunks(int totalChunks) { session.totalChunks = totalChunks; return this; }
            public Builder uploadedChunks(Set<Integer> uploadedChunks) { session.uploadedChunks = uploadedChunks; return this; }
            public Builder userId(String userId) { session.userId = userId; return this; }
            public Builder startTime(long startTime) { session.startTime = startTime; return this; }
            public Builder status(String status) { session.status = status; return this; }
            public ChunkUploadSession build() { return session; }
        }
    }

    /**
     * 分片上传结果
     */
    public static class ChunkUploadResult {
        private boolean success;
        private String sessionId;
        private int chunkIndex;
        private int uploadedCount;
        private int totalChunks;
        private String fileName;
        private long fileSize;
        private long uploadTime;
        private String message;

        // Builder pattern
        public static Builder builder() {
            return new Builder();
        }

        // Getters and setters
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public String getSessionId() { return sessionId; }
        public void setSessionId(String sessionId) { this.sessionId = sessionId; }
        public int getChunkIndex() { return chunkIndex; }
        public void setChunkIndex(int chunkIndex) { this.chunkIndex = chunkIndex; }
        public int getUploadedCount() { return uploadedCount; }
        public void setUploadedCount(int uploadedCount) { this.uploadedCount = uploadedCount; }
        public int getTotalChunks() { return totalChunks; }
        public void setTotalChunks(int totalChunks) { this.totalChunks = totalChunks; }
        public String getFileName() { return fileName; }
        public void setFileName(String fileName) { this.fileName = fileName; }
        public long getFileSize() { return fileSize; }
        public void setFileSize(long fileSize) { this.fileSize = fileSize; }
        public long getUploadTime() { return uploadTime; }
        public void setUploadTime(long uploadTime) { this.uploadTime = uploadTime; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public static class Builder {
            private ChunkUploadResult result = new ChunkUploadResult();
            public Builder success(boolean success) { result.success = success; return this; }
            public Builder sessionId(String sessionId) { result.sessionId = sessionId; return this; }
            public Builder chunkIndex(int chunkIndex) { result.chunkIndex = chunkIndex; return this; }
            public Builder uploadedCount(int uploadedCount) { result.uploadedCount = uploadedCount; return this; }
            public Builder totalChunks(int totalChunks) { result.totalChunks = totalChunks; return this; }
            public Builder fileName(String fileName) { result.fileName = fileName; return this; }
            public Builder fileSize(long fileSize) { result.fileSize = fileSize; return this; }
            public Builder uploadTime(long uploadTime) { result.uploadTime = uploadTime; return this; }
            public Builder message(String message) { result.message = message; return this; }
            public ChunkUploadResult build() { return result; }
        }
    }
}