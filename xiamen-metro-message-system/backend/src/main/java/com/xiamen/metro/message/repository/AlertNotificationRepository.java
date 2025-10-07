package com.xiamen.metro.message.repository;

import com.xiamen.metro.message.entity.AlertNotificationEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 告警通知记录数据访问层
 *
 * @author Xiamen Metro System
 */
@Repository
public interface AlertNotificationRepository extends JpaRepository<AlertNotificationEntity, Long> {

    /**
     * 根据告警ID查找通知记录
     */
    List<AlertNotificationEntity> findByAlertId(String alertId);

    /**
     * 根据通知类型查找
     */
    List<AlertNotificationEntity> findByNotificationType(AlertNotificationEntity.NotificationType notificationType);

    /**
     * 根据发送状态查找
     */
    List<AlertNotificationEntity> findByStatus(AlertNotificationEntity.NotificationStatus status);

    /**
     * 查找待发送的通知
     */
    List<AlertNotificationEntity> findByStatusOrderByNotificationTimeAsc(
            AlertNotificationEntity.NotificationStatus status);

    /**
     * 查找需要重试的通知
     */
    @Query("SELECT n FROM AlertNotificationEntity n WHERE n.status = 'FAILED' " +
           "AND n.retryCount < 3 AND (n.nextRetryTime IS NULL OR n.nextRetryTime <= :now)")
    List<AlertNotificationEntity> findNotificationsNeedingRetry(@Param("now") LocalDateTime now);

    /**
     * 根据接收人查找通知记录
     */
    Page<AlertNotificationEntity> findByRecipient(String recipient, Pageable pageable);

    /**
     * 查找指定时间范围内的通知记录
     */
    @Query("SELECT n FROM AlertNotificationEntity n WHERE n.notificationTime BETWEEN :startTime AND :endTime")
    List<AlertNotificationEntity> findByTimeRange(@Param("startTime") LocalDateTime startTime,
                                                 @Param("endTime") LocalDateTime endTime);

    /**
     * 统计各类型通知数量
     */
    @Query("SELECT n.notificationType, COUNT(n) FROM AlertNotificationEntity n " +
           "WHERE n.notificationTime >= :since GROUP BY n.notificationType")
    List<Object[]> countNotificationsByType(@Param("since") LocalDateTime since);

    /**
     * 统计各状态通知数量
     */
    @Query("SELECT n.status, COUNT(n) FROM AlertNotificationEntity n " +
           "WHERE n.notificationTime >= :since GROUP BY n.status")
    List<Object[]> countNotificationsByStatus(@Param("since") LocalDateTime since);

    /**
     * 查找发送失败的通知
     */
    @Query("SELECT n FROM AlertNotificationEntity n WHERE n.status = 'FAILED' " +
           "ORDER BY n.notificationTime DESC")
    List<AlertNotificationEntity> findFailedNotifications(Pageable pageable);

    /**
     * 查找最近的通知记录
     */
    List<AlertNotificationEntity> findTop10ByOrderByNotificationTimeDesc();

    /**
     * 根据模板ID查找通知记录
     */
    List<AlertNotificationEntity> findByTemplateId(String templateId);

    /**
     * 查找重复发送的通知
     */
    @Query("SELECT n FROM AlertNotificationEntity n WHERE n.alertId = :alertId " +
           "AND n.notificationType = :notificationType AND n.recipient = :recipient " +
           "AND n.notificationTime >= :since")
    List<AlertNotificationEntity> findRecentDuplicateNotifications(@Param("alertId") String alertId,
                                                                 @Param("notificationType") AlertNotificationEntity.NotificationType notificationType,
                                                                 @Param("recipient") String recipient,
                                                                 @Param("since") LocalDateTime since);

    /**
     * 删除过期的通知记录
     */
    @Query("DELETE FROM AlertNotificationEntity n WHERE n.notificationTime < :beforeTime " +
           "AND n.status IN ('SUCCESS', 'SKIPPED')")
    void deleteExpiredNotifications(@Param("beforeTime") LocalDateTime beforeTime);
}