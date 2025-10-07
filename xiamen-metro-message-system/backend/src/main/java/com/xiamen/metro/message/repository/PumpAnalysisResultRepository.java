package com.xiamen.metro.message.repository;

import com.xiamen.metro.message.entity.PumpAnalysisResultEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 水泵分析结果仓库
 *
 * @author Xiamen Metro System
 */
@Repository
public interface PumpAnalysisResultRepository extends JpaRepository<PumpAnalysisResultEntity, Long> {

    /**
     * 根据设备ID和分析类型查询结果
     */
    List<PumpAnalysisResultEntity> findByDeviceIdAndAnalysisTypeOrderByAnalysisTimestampDesc(
            String deviceId, String analysisType);

    /**
     * 根据设备ID和时间范围查询结果
     */
    List<PumpAnalysisResultEntity> findByDeviceIdAndAnalysisTimestampBetweenOrderByAnalysisTimestampDesc(
            String deviceId, LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 根据严重级别查询结果
     */
    List<PumpAnalysisResultEntity> findBySeverityLevelGreaterThanOrderByAnalysisTimestampDesc(Integer severityLevel);

    /**
     * 根据分析类型和严重级别查询结果
     */
    List<PumpAnalysisResultEntity> findByAnalysisTypeAndSeverityLevelGreaterThanOrderByAnalysisTimestampDesc(
            String analysisType, Integer severityLevel);

    /**
     * 查询未确认的高严重级别结果
     */
    List<PumpAnalysisResultEntity> findBySeverityLevelGreaterThanAndIsConfirmedFalseOrderByAnalysisTimestampDesc(Integer severityLevel);

    /**
     * 根据设备ID查询最新的分析结果
     */
    @Query("SELECT p FROM PumpAnalysisResultEntity p WHERE p.deviceId = :deviceId ORDER BY p.analysisTimestamp DESC")
    List<PumpAnalysisResultEntity> findLatestByDeviceId(@Param("deviceId") String deviceId);

    /**
     * 根据设备ID和分析类型查询最新的结果
     */
    @Query("SELECT p FROM PumpAnalysisResultEntity p WHERE p.deviceId = :deviceId AND p.analysisType = :analysisType ORDER BY p.analysisTimestamp DESC LIMIT 1")
    PumpAnalysisResultEntity findLatestByDeviceIdAndAnalysisType(
            @Param("deviceId") String deviceId, @Param("analysisType") String analysisType);

    /**
     * 统计指定时间范围内的异常数量
     */
    @Query("SELECT COUNT(*) FROM PumpAnalysisResultEntity p WHERE p.deviceId = :deviceId AND p.analysisTimestamp BETWEEN :startTime AND :endTime AND p.severityLevel > :severityLevel")
    Long countAnomalies(@Param("deviceId") String deviceId, @Param("startTime") LocalDateTime startTime,
                       @Param("endTime") LocalDateTime endTime, @Param("severityLevel") Integer severityLevel);

    /**
     * 获取设备的风险评分
     */
    @Query("SELECT AVG(p.severityLevel * p.priorityLevel) FROM PumpAnalysisResultEntity p WHERE p.deviceId = :deviceId AND p.analysisTimestamp >= :since")
    Double calculateRiskScore(@Param("deviceId") String deviceId, @Param("since") LocalDateTime since);

    /**
     * 查询预测的近期故障
     */
    List<PumpAnalysisResultEntity> findByPredictedFailureTimeBetweenOrderByPredictedFailureTimeAsc(
            LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 根据模型版本查询结果
     */
    List<PumpAnalysisResultEntity> findByModelVersionOrderByAnalysisTimestampDesc(String modelVersion);

    /**
     * 查询需要紧急处理的结果
     */
    @Query("SELECT p FROM PumpAnalysisResultEntity p WHERE p.severityLevel >= 3 AND p.priorityLevel >= 3 AND p.isConfirmed = false ORDER BY p.severityLevel DESC, p.priorityLevel DESC")
    List<PumpAnalysisResultEntity> findUrgentResults();
}