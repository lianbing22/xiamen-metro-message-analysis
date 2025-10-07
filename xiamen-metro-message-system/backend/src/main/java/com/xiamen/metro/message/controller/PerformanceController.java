package com.xiamen.metro.message.controller;

import com.xiamen.metro.message.service.CacheService;
import com.xiamen.metro.message.service.PerformanceMonitorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 性能监控控制器
 * 提供系统性能指标和监控功能
 */
@RestController
@RequestMapping("/api/performance")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "性能监控", description = "系统性能监控相关接口")
public class PerformanceController {

    private final PerformanceMonitorService performanceMonitorService;
    private final CacheService cacheService;

    /**
     * 获取系统性能指标
     */
    @GetMapping("/metrics")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "获取系统性能指标", description = "获取JVM、数据库、缓存等系统性能指标")
    public ResponseEntity<Map<String, Object>> getSystemMetrics() {
        try {
            Map<String, Object> metrics = performanceMonitorService.getSystemPerformanceMetrics();
            return ResponseEntity.ok(metrics);
        } catch (Exception e) {
            log.error("获取系统性能指标失败", e);
            return ResponseEntity.internalServerError().body(Map.of("error", "获取性能指标失败: " + e.getMessage()));
        }
    }

    /**
     * 获取系统健康状态
     */
    @GetMapping("/health")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "获取系统健康状态", description = "检查数据库、Redis、内存等组件健康状态")
    public ResponseEntity<Map<String, Object>> getHealthStatus() {
        try {
            Map<String, Object> health = performanceMonitorService.getHealthStatus();
            return ResponseEntity.ok(health);
        } catch (Exception e) {
            log.error("获取系统健康状态失败", e);
            return ResponseEntity.internalServerError().body(Map.of("error", "获取健康状态失败: " + e.getMessage()));
        }
    }

    /**
     * 获取性能统计概览
     */
    @GetMapping("/overview")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "获取性能统计概览", description = "获取系统性能的概览信息")
    public ResponseEntity<Map<String, Object>> getPerformanceOverview() {
        try {
            Map<String, Object> metrics = performanceMonitorService.getSystemPerformanceMetrics();
            Map<String, Object> overview = new HashMap<>();

            // 提取关键指标
            Map<String, Object> memory = (Map<String, Object>) metrics.get("memory");
            Map<String, Object> threads = (Map<String, Object>) metrics.get("threads");
            Map<String, Object> connectionPool = (Map<String, Object>) metrics.get("connectionPool");
            Map<String, Object> cache = (Map<String, Object>) metrics.get("cache");

            overview.put("memoryUsagePercent", memory.get("heapUsagePercent"));
            overview.put("threadCount", threads.get("threadCount"));
            overview.put("totalStartedThreads", threads.get("totalStartedThreadCount"));
            overview.put("connectionPool", connectionPool);
            overview.put("cache", cache);
            overview.put("timestamp", metrics.get("timestamp"));

            return ResponseEntity.ok(overview);
        } catch (Exception e) {
            log.error("获取性能统计概览失败", e);
            return ResponseEntity.internalServerError().body(Map.of("error", "获取性能概览失败: " + e.getMessage()));
        }
    }

    /**
     * 获取性能指标历史数据
     */
    @GetMapping("/metrics/{metricName}/history")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "获取性能指标历史数据", description = "获取指定性能指标的历史数据")
    public ResponseEntity<Map<String, Object>> getMetricsHistory(
            @PathVariable String metricName,
            @RequestParam(defaultValue = "100") int limit) {
        try {
            Map<String, Object> history = performanceMonitorService.getMetricsHistory(metricName, limit);
            return ResponseEntity.ok(history);
        } catch (Exception e) {
            log.error("获取性能指标历史数据失败: {}", metricName, e);
            return ResponseEntity.internalServerError().body(Map.of("error", "获取历史数据失败: " + e.getMessage()));
        }
    }

    /**
     * 清除缓存
     */
    @DeleteMapping("/cache")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "清除缓存", description = "清除指定类型的缓存数据")
    public ResponseEntity<Map<String, String>> clearCache(
            @RequestParam(required = false) String pattern,
            @RequestParam(required = false) String cacheName) {
        Map<String, String> result = new HashMap<>();

        try {
            if (pattern != null && !pattern.trim().isEmpty()) {
                // 清除匹配模式的缓存
                cacheService.evictByPattern(pattern);
                result.put("message", "已清除匹配模式 '" + pattern + "' 的缓存");
                log.info("管理员清除了匹配模式 '{}' 的缓存", pattern);
            } else if (cacheName != null && !cacheName.trim().isEmpty()) {
                // 清除指定名称的缓存
                cacheService.evict(cacheName);
                result.put("message", "已清除缓存: " + cacheName);
                log.info("管理员清除了缓存: {}", cacheName);
            } else {
                // 清除所有热点数据缓存
                cacheService.evictByPattern("hot:data:*");
                cacheService.evictByPattern("query:result:*");
                result.put("message", "已清除所有热点数据和查询结果缓存");
                log.info("管理员清除了所有热点数据和查询结果缓存");
            }

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("清除缓存失败", e);
            return ResponseEntity.internalServerError().body(Map.of("error", "清除缓存失败: " + e.getMessage()));
        }
    }

    /**
     * 获取缓存状态
     */
    @GetMapping("/cache/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "获取缓存状态", description = "获取Redis和Spring Cache的状态信息")
    public ResponseEntity<Map<String, Object>> getCacheStatus() {
        try {
            Map<String, Object> cacheStatus = new HashMap<>();

            // Redis连接测试
            try {
                boolean redisConnected = cacheService.exists("test:connection");
                cacheStatus.put("redisConnected", redisConnected);
            } catch (Exception e) {
                cacheStatus.put("redisConnected", false);
                cacheStatus.put("redisError", e.getMessage());
            }

            // 缓存命中率等统计信息
            cacheStatus.put("cacheTypes", Map.of(
                    "hotData", "热点数据缓存",
                    "queryResult", "查询结果缓存",
                    "userSession", "用户会话缓存",
                    "fileMetadata", "文件元数据缓存"
            ));

            cacheStatus.put("timestamp", java.time.LocalDateTime.now());

            return ResponseEntity.ok(cacheStatus);
        } catch (Exception e) {
            log.error("获取缓存状态失败", e);
            return ResponseEntity.internalServerError().body(Map.of("error", "获取缓存状态失败: " + e.getMessage()));
        }
    }

    /**
     * 预热缓存
     */
    @PostMapping("/cache/warmup")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "预热缓存", description = "预热系统缓存，提高响应速度")
    public ResponseEntity<Map<String, String>> warmupCache() {
        try {
            // 这里可以实现具体的缓存预热逻辑
            // 例如：加载常用配置、热点数据等

            log.info("开始缓存预热");

            // 模拟预热操作
            // 实际项目中应该加载真实的热点数据
            Map<String, Object> warmupData = new HashMap<>();
            warmupData.put("systemConfig", Map.of("version", "1.0.0", "name", "厦门地铁设备报文分析系统"));
            warmupData.put("commonSettings", Map.of("pageSize", 20, "timeout", 30000));

            cacheService.batchSet(warmupData, 3600, java.util.concurrent.TimeUnit.SECONDS);

            log.info("缓存预热完成");
            return ResponseEntity.ok(Map.of("message", "缓存预热完成"));
        } catch (Exception e) {
            log.error("缓存预热失败", e);
            return ResponseEntity.internalServerError().body(Map.of("error", "缓存预热失败: " + e.getMessage()));
        }
    }

    /**
     * 记录性能指标
     */
    @PostMapping("/metrics")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "记录性能指标", description = "记录自定义性能指标")
    public ResponseEntity<Map<String, String>> recordMetric(
            @RequestParam String metricName,
            @RequestParam double value) {
        try {
            performanceMonitorService.recordMetrics(metricName, value);
            log.info("记录性能指标: {} = {}", metricName, value);
            return ResponseEntity.ok(Map.of("message", "性能指标记录成功"));
        } catch (Exception e) {
            log.error("记录性能指标失败", e);
            return ResponseEntity.internalServerError().body(Map.of("error", "记录性能指标失败: " + e.getMessage()));
        }
    }

    /**
     * 系统负载测试接口
     */
    @GetMapping("/load-test")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "系统负载测试", description = "执行简单的系统负载测试")
    public ResponseEntity<Map<String, Object>> performLoadTest(
            @RequestParam(defaultValue = "10") int iterations,
            @RequestParam(defaultValue = "100") int complexity) {
        try {
            long startTime = System.currentTimeMillis();

            // 模拟CPU密集型操作
            double result = 0;
            for (int i = 0; i < iterations * complexity; i++) {
                result += Math.sqrt(i) * Math.sin(i) * Math.cos(i);
            }

            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;

            Map<String, Object> testResult = new HashMap<>();
            testResult.put("iterations", iterations);
            testResult.put("complexity", complexity);
            testResult.put("duration", duration);
            testResult.put("avgTimePerIteration", (double) duration / iterations);
            testResult.put("result", result);
            testResult.put("timestamp", java.time.LocalDateTime.now());

            // 记录性能指标
            performanceMonitorService.recordMetrics("loadTestDuration", duration);
            performanceMonitorService.recordMetrics("loadTestAvgTime", (double) duration / iterations);

            log.info("负载测试完成: 迭代次数={}, 耗时={}ms", iterations, duration);

            return ResponseEntity.ok(testResult);
        } catch (Exception e) {
            log.error("负载测试失败", e);
            return ResponseEntity.internalServerError().body(Map.of("error", "负载测试失败: " + e.getMessage()));
        }
    }
}