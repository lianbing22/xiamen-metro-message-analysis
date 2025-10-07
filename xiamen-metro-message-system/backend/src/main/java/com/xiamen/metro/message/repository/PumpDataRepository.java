package com.xiamen.metro.message.repository;

import com.xiamen.metro.message.entity.PumpDataEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 水泵数据仓库
 *
 * @author Xiamen Metro System
 */
@Repository
public interface PumpDataRepository extends JpaRepository<PumpDataEntity, Long> {

    /**
     * 根据设备ID和时间范围查询数据
     */
    List<PumpDataEntity> findByDeviceIdAndTimestampBetweenOrderByTimestampAsc(
            String deviceId, LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 根据设备ID查询最新的数据
     */
    @Query("SELECT p FROM PumpDataEntity p WHERE p.deviceId = :deviceId ORDER BY p.timestamp DESC")
    List<PumpDataEntity> findLatestByDeviceId(@Param("deviceId") String deviceId);

    /**
     * 根据设备ID查询指定数量的最新数据
     */
    @Query("SELECT p FROM PumpDataEntity p WHERE p.deviceId = :deviceId ORDER BY p.timestamp DESC LIMIT :limit")
    List<PumpDataEntity> findLatestByDeviceIdWithLimit(@Param("deviceId") String deviceId, @Param("limit") int limit);

    /**
     * 查询指定时间范围内的故障数据
     */
    List<PumpDataEntity> findByDeviceIdAndTimestampBetweenAndFaultCodeIsNotNullOrderByTimestampAsc(
            String deviceId, LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 查询指定时间范围内的报警数据
     */
    List<PumpDataEntity> findByDeviceIdAndTimestampBetweenAndAlarmLevelGreaterThanOrderByTimestampAsc(
            String deviceId, LocalDateTime startTime, LocalDateTime endTime, Integer alarmLevel);

    /**
     * 统计指定时间范围内的启停次数
     */
    @Query("SELECT COUNT(*) FROM PumpDataEntity p WHERE p.deviceId = :deviceId AND p.timestamp BETWEEN :startTime AND :endTime AND p.pumpStatus = 1")
    Long countStartEvents(@Param("deviceId") String deviceId, @Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    /**
     * 计算总运行时间
     */
    @Query("SELECT COALESCE(SUM(p.runtimeMinutes), 0) FROM PumpDataEntity p WHERE p.deviceId = :deviceId AND p.timestamp BETWEEN :startTime AND :endTime")
    Double sumRuntimeMinutes(@Param("deviceId") String deviceId, @Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    /**
     * 计算总能耗
     */
    @Query("SELECT COALESCE(SUM(p.energyConsumptionKwh), 0) FROM PumpDataEntity p WHERE p.deviceId = :deviceId AND p.timestamp BETWEEN :startTime AND :endTime")
    Double sumEnergyConsumption(@Param("deviceId") String deviceId, @Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    /**
     * 获取平均功率
     */
    @Query("SELECT AVG(p.powerKw) FROM PumpDataEntity p WHERE p.deviceId = :deviceId AND p.timestamp BETWEEN :startTime AND :endTime AND p.powerKw IS NOT NULL")
    Double averagePower(@Param("deviceId") String deviceId, @Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    /**
     * 获取最大振动值
     */
    @Query("SELECT MAX(p.vibrationMmS) FROM PumpDataEntity p WHERE p.deviceId = :deviceId AND p.timestamp BETWEEN :startTime AND :endTime AND p.vibrationMmS IS NOT NULL")
    Double maxVibration(@Param("deviceId") String deviceId, @Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    /**
     * 查询所有活跃的设备ID
     */
    @Query("SELECT DISTINCT p.deviceId FROM PumpDataEntity p WHERE p.timestamp >= :since")
    List<String> findActiveDeviceIds(@Param("since") LocalDateTime since);

    /**
     * 根据故障代码查询数据
     */
    List<PumpDataEntity> findByDeviceIdAndFaultCodeOrderByTimestampDesc(String deviceId, String faultCode);

    /**
     * 查询需要维护的设备
     */
    List<PumpDataEntity> findByMaintenanceFlagTrueOrderByTimestampDesc();
}