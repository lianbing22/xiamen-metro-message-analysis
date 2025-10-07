package com.xiamen.metro.message.repository;

import com.xiamen.metro.message.entity.FileEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 文件仓库接口
 *
 * @author Xiamen Metro System
 */
@Repository
public interface FileRepository extends JpaRepository<FileEntity, Long> {

    /**
     * 根据文件哈希值查找文件
     *
     * @param fileHash 文件哈希值
     * @return 文件实体
     */
    Optional<FileEntity> findByFileHash(String fileHash);

    /**
     * 根据文件名查找文件
     *
     * @param fileName 文件名
     * @return 文件实体
     */
    Optional<FileEntity> findByFileName(String fileName);

    /**
     * 根据上传状态查找文件
     *
     * @param uploadStatus 上传状态
     * @param pageable     分页参数
     * @return 文件列表
     */
    Page<FileEntity> findByUploadStatus(FileEntity.UploadStatus uploadStatus, Pageable pageable);

    /**
     * 根据处理状态查找文件
     *
     * @param processStatus 处理状态
     * @param pageable      分页参数
     * @return 文件列表
     */
    Page<FileEntity> findByProcessStatus(FileEntity.ProcessStatus processStatus, Pageable pageable);

    /**
     * 根据上传用户查找文件
     *
     * @param uploadedBy 上传用户ID
     * @param pageable   分页参数
     * @return 文件列表
     */
    Page<FileEntity> findByUploadedBy(Long uploadedBy, Pageable pageable);

    /**
     * 根据文件类型查找文件
     *
     * @param fileType 文件类型
     * @param pageable 分页参数
     * @return 文件列表
     */
    Page<FileEntity> findByFileType(FileEntity.FileType fileType, Pageable pageable);

    /**
     * 统计各状态文件数量
     *
     * @return 统计结果
     */
    @Query("SELECT f.uploadStatus, COUNT(f) FROM FileEntity f GROUP BY f.uploadStatus")
    List<Object[]> countByUploadStatus();

    /**
     * 统计各处理状态文件数量
     *
     * @return 统计结果
     */
    @Query("SELECT f.processStatus, COUNT(f) FROM FileEntity f GROUP BY f.processStatus")
    List<Object[]> countByProcessStatus();

    /**
     * 根据原始文件名模糊查询
     *
     * @param originalFileName 原始文件名
     * @param pageable         分页参数
     * @return 文件列表
     */
    Page<FileEntity> findByOriginalFileNameContainingIgnoreCase(String originalFileName, Pageable pageable);

    /**
     * 查找需要处理的文件
     *
     * @return 文件列表
     */
    @Query("SELECT f FROM FileEntity f WHERE f.uploadStatus = 'COMPLETED' AND f.processStatus = 'PENDING'")
    List<FileEntity> findPendingProcessingFiles();
}