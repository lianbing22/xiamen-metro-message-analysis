package com.xiamen.metro.message.service;

import io.minio.*;
import io.minio.messages.Bucket;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.List;
import java.util.UUID;

/**
 * MinIO文件存储服务
 *
 * @author Xiamen Metro System
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MinioService {

    private final MinioClient minioClient;

    @Value("${minio.bucket-name}")
    private String bucketName;

    /**
     * 上传文件
     *
     * @param inputStream 文件输入流
     * @param objectName  对象名称
     * @param contentType 内容类型
     * @param fileSize    文件大小
     * @return 文件URL
     */
    public String uploadFile(InputStream inputStream, String objectName, String contentType, long fileSize) {
        try {
            // 确保存储桶存在
            ensureBucketExists();

            // 上传文件
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .stream(inputStream, fileSize, -1)
                            .contentType(contentType)
                            .build()
            );

            // 生成文件URL
            return getFileUrl(objectName);

        } catch (Exception e) {
            log.error("上传文件到MinIO失败: {}", e.getMessage(), e);
            throw new RuntimeException("上传文件失败: " + e.getMessage());
        }
    }

    /**
     * 下载文件
     *
     * @param objectName 对象名称
     * @return 文件输入流
     */
    public InputStream downloadFile(String objectName) {
        try {
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .build()
            );
        } catch (Exception e) {
            log.error("从MinIO下载文件失败: {}", e.getMessage(), e);
            throw new RuntimeException("下载文件失败: " + e.getMessage());
        }
    }

    /**
     * 删除文件
     *
     * @param objectName 对象名称
     */
    public void deleteFile(String objectName) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .build()
            );
            log.info("从MinIO删除文件成功: {}", objectName);
        } catch (Exception e) {
            log.error("从MinIO删除文件失败: {}", e.getMessage(), e);
            throw new RuntimeException("删除文件失败: " + e.getMessage());
        }
    }

    /**
     * 获取文件URL
     *
     * @param objectName 对象名称
     * @return 文件URL
     */
    public String getFileUrl(String objectName) {
        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(bucketName)
                            .object(objectName)
                            .expiry(60 * 60 * 24) // 24小时有效期
                            .build()
            );
        } catch (Exception e) {
            log.error("获取文件URL失败: {}", e.getMessage(), e);
            throw new RuntimeException("获取文件URL失败: " + e.getMessage());
        }
    }

    /**
     * 检查文件是否存在
     *
     * @param objectName 对象名称
     * @return 是否存在
     */
    public boolean fileExists(String objectName) {
        try {
            minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .build()
            );
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 确保存储桶存在
     */
    private void ensureBucketExists() {
        try {
            boolean isExist = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
            if (!isExist) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
                log.info("创建MinIO存储桶成功: {}", bucketName);
            }
        } catch (Exception e) {
            log.error("检查MinIO存储桶存在性失败: {}", e.getMessage(), e);
            throw new RuntimeException("检查存储桶失败: " + e.getMessage());
        }
    }

    /**
     * 生成对象名称
     *
     * @param fileName 文件名
     * @param fileExt  文件扩展名
     * @return 对象名称
     */
    public String generateObjectName(String fileName, String fileExt) {
        String uuid = UUID.randomUUID().toString().replace("-", "");
        return String.format("%s/%s/%s/%s%s",
                "metro-files",
                java.time.LocalDate.now().toString(),
                uuid.substring(0, 6),
                uuid,
                fileExt);
    }
}