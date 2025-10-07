package com.xiamen.metro.message.repository;

import com.xiamen.metro.message.entity.AlertRecordEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 告警记录数据访问层
 *
 * @author Xiamen Metro System
 */
@Repository
public interface AlertRecordRepository extends JpaRepository<AlertRecordEntity, Long> {

    /**
     * 根据告警ID查找
     */
    AlertRecordEntity findByAlertId(String alertId);

    /**
     * 根据设备ID查找告警
     */
    Page<AlertRecordEntity> findByDeviceId(String deviceId, Pageable pageable);

    /**
     * 根据设备ID和告警级别查找
     */
    List<AlertRecordEntity> findByDeviceIdAndAlertLevel(String deviceId,
                                                      AlertRecordEntity.AlertStatus alertLevel);

    /**
     * 根据设备ID和告警状态查找
     */
    List<AlertRecordEntity> findByDeviceIdAndStatus(String deviceId,
                                                   AlertRecordEntity.AlertStatus status);

    /**
     * 根据规则ID查找告警
     */
    List<AlertRecordEntity> findByRuleId(Long ruleId);

    /**
     * 查找活跃的告警
     */
    List<AlertRecordEntity> findByStatus(AlertRecordEntity.AlertStatus status);

    /**
     * 查找指定时间范围内的告警
     */
    @Query("SELECT a FROM AlertRecordEntity a WHERE a.alertTime BETWEEN :startTime AND :endTime")
    List<AlertRecordEntity> findByTimeRange(@Param("startTime") LocalDateTime startTime,
                                           @Param("endTime") LocalDateTime endTime);

    /**
     * 查找指定时间范围内设备的告警
     */
    @Query("SELECT a FROM AlertRecordEntity a WHERE a.deviceId = :deviceId AND " +
           "a.alertTime BETWEEN :startTime AND :endTime")
    List<AlertRecordEntity> findByDeviceAndTimeRange(@Param("deviceId") String deviceId,
                                                   @Param("startTime") LocalDateTime startTime,
                                                   @Param("endTime") LocalDateTime endTime);

    /**
     * 查找未确认的告警
     */
    List<AlertRecordEntity> findByIsConfirmedFalse();

    /**
     * 查找未处理的活跃告警
     */
    List<AlertRecordEntity> findByStatusAndIsConfirmedFalse(AlertRecordEntity.AlertStatus status);

    /**
     * 统计各状态告警数量
     */
    @Query("SELECT a.status, COUNT(a) FROM AlertRecordEntity a GROUP BY a.status")
    List<Object[]> countAlertsByStatus();

    /**
     * 统计各设备告警数量
     */
    @Query("SELECT a.deviceId, COUNT(a) FROM AlertRecordEntity a " +
           "WHERE a.alertTime >= :since GROUP BY a.deviceId")
    List<Object[]> countAlertsByDevice(@Param("since") LocalDateTime since);

    /**
     * 统计各级别告警数量
     */
    @Query("SELECT a.alertLevel, COUNT(a) FROM AlertRecordEntity a " +
           "WHERE a.alertTime >= :since GROUP BY a.alertLevel")
    List<Object[]> countAlertsByLevel(@Param("since") LocalDateTime since);

    /**
     * 查找最近的告警
     */
    List<AlertRecordEntity> findTop10ByOrderByAlertTimeDesc();

    /**
     * 查找指定设备的最近告警
     */
    @Query("SELECT a FROM AlertRecordEntity a WHERE a.deviceId = :deviceId " +
           "ORDER BY a.alertTime DESC")
    List<AlertRecordEntity> findRecentAlertsByDevice(@Param("deviceId") String deviceId, Pageable pageable);

    /**
     * 查找超过指定时间未处理的告警
     */
    @Query("SELECT a FROM AlertRecordEntity a WHERE a.status = :status AND " +
           "a.alertTime < :beforeTime")
    List<AlertRecordEntity> findUnresolvedAlertsBefore(@Param("status") AlertRecordEntity.AlertStatus status,
                                                      @Param("beforeTime") LocalDateTime beforeTime);

    /**
     * 根据关键词搜索告警
     */
    @Query("SELECT a FROM AlertRecordEntity a WHERE a.alertTitle LIKE %:keyword% " +
           "OR a.alertContent LIKE %:keyword%")
    Page<AlertRecordEntity> findByKeyword(@Param("keyword") String keyword, Pageable pageable);

    /**
     * 查找需要发送通知的告警
     */
    @Query("SELECT a FROM AlertRecordEntity a WHERE a.emailNotified = false OR " +
           "a.smsNotified = false OR a.websocketNotified = false")
    List<AlertRecordEntity> findAlertsNeedingNotification();

    /**
     * 查找重复的告警（同一设备、同一规则、短时间内）
     */
    @Query("SELECT a FROM AlertRecordEntity a WHERE a.deviceId = :deviceId AND a.ruleId = :ruleId " +
           "AND a.alertTime >= :since ORDER BY a.alertTime DESC")
    List<AlertRecordEntity> findRecentSimilarAlerts(@Param("deviceId") String deviceId,
                                                   @Param("ruleId") Long ruleId,
                                                   @Param("since") LocalDateTime since);
}