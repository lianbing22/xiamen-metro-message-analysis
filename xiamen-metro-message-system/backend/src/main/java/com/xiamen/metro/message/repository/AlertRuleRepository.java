package com.xiamen.metro.message.repository;

import com.xiamen.metro.message.entity.AlertRuleEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 告警规则数据访问层
 *
 * @author Xiamen Metro System
 */
@Repository
public interface AlertRuleRepository extends JpaRepository<AlertRuleEntity, Long> {

    /**
     * 根据规则名称查找
     */
    Optional<AlertRuleEntity> findByRuleName(String ruleName);

    /**
     * 查找活跃的规则
     */
    List<AlertRuleEntity> findByIsActiveTrue();

    /**
     * 根据设备ID查找活跃规则
     */
    List<AlertRuleEntity> findByDeviceIdAndIsActiveTrue(String deviceId);

    /**
     * 根据规则类型查找活跃规则
     */
    List<AlertRuleEntity> findByRuleTypeAndIsActiveTrue(AlertRuleEntity.RuleType ruleType);

    /**
     * 根据告警级别查找活跃规则
     */
    List<AlertRuleEntity> findByAlertLevelAndIsActiveTrue(AlertRuleEntity.AlertLevel alertLevel);

    /**
     * 查找需要检查的规则（基于检查间隔）
     */
    @Query("SELECT r FROM AlertRuleEntity r WHERE r.isActive = true AND " +
           "(r.lastTriggeredTime IS NULL OR r.lastTriggeredTime < :sinceTime)")
    List<AlertRuleEntity> findRulesNeedingCheck(@Param("sinceTime") LocalDateTime sinceTime);

    /**
     * 根据优先级查找活跃规则
     */
    List<AlertRuleEntity> findByIsActiveTrueOrderByPriorityDesc();

    /**
     * 查找指定设备的规则（包括全局规则）
     */
    @Query("SELECT r FROM AlertRuleEntity r WHERE r.isActive = true AND " +
           "(r.deviceId = :deviceId OR r.deviceId IS NULL) " +
           "ORDER BY r.priority DESC")
    List<AlertRuleEntity> findApplicableRules(@Param("deviceId") String deviceId);

    /**
     * 根据创建者查找规则
     */
    Page<AlertRuleEntity> findByCreatedBy(String createdBy, Pageable pageable);

    /**
     * 根据规则名称模糊查询
     */
    @Query("SELECT r FROM AlertRuleEntity r WHERE r.ruleName LIKE %:keyword% " +
           "OR r.description LIKE %:keyword%")
    Page<AlertRuleEntity> findByKeyword(@Param("keyword") String keyword, Pageable pageable);

    /**
     * 统计活跃规则数量
     */
    @Query("SELECT COUNT(r) FROM AlertRuleEntity r WHERE r.isActive = true")
    long countActiveRules();

    /**
     * 统计各类型规则数量
     */
    @Query("SELECT r.ruleType, COUNT(r) FROM AlertRuleEntity r WHERE r.isActive = true " +
           "GROUP BY r.ruleType")
    List<Object[]> countRulesByType();

    /**
     * 查找最近更新的规则
     */
    @Query("SELECT r FROM AlertRuleEntity r WHERE r.isActive = true " +
           "ORDER BY r.updatedTime DESC")
    List<AlertRuleEntity> findRecentlyUpdatedRules(Pageable pageable);
}