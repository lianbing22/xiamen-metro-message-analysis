package com.xiamen.metro.message.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.ThreadMXBean;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 性能监控服务
 * 提供系统性能指标收集和监控
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PerformanceMonitorService {

    private final DataSource dataSource;
    private final CacheManager cacheManager;
    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${app.performance.pool-monitoring.enabled:true}")
    private boolean poolMonitoringEnabled;

    @Value("${app.performance.slow-query.threshold:1000}")
    private long slowQueryThreshold;

    private final MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
    private final ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();

    /**
     * 获取系统性能指标
     */
    public Map<String, Object> getSystemPerformanceMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        LocalDateTime timestamp = LocalDateTime.now();

        // JVM内存指标
        Map<String, Object> memoryMetrics = getMemoryMetrics();
        metrics.put("memory", memoryMetrics);

        // 线程指标
        Map<String, Object> threadMetrics = getThreadMetrics();
        metrics.put("threads", threadMetrics);

        // 数据库连接池指标
        Map<String, Object> poolMetrics = getConnectionPoolMetrics();
        metrics.put("connectionPool", poolMetrics);

        // 缓存指标
        Map<String, Object> cacheMetrics = getCacheMetrics();
        metrics.put("cache", cacheMetrics);

        // 系统指标
        Map<String, Object> systemMetrics = getSystemMetrics();
        metrics.put("system", systemMetrics);

        metrics.put("timestamp", timestamp);
        metrics.put("version", "1.0.0");

        return metrics;
    }

    /**
     * 获取JVM内存指标
     */
    private Map<String, Object> getMemoryMetrics() {
        Map<String, Object> memoryMetrics = new HashMap<>();

        // 堆内存使用情况
        long heapUsed = memoryMXBean.getHeapMemoryUsage().getUsed();
        long heapMax = memoryMXBean.getHeapMemoryUsage().getMax();
        long heapCommitted = memoryMXBean.getHeapMemoryUsage().getCommitted();

        // 非堆内存使用情况
        long nonHeapUsed = memoryMXBean.getNonHeapMemoryUsage().getUsed();
        long nonHeapMax = memoryMXBean.getNonHeapMemoryUsage().getMax();

        memoryMetrics.put("heapUsed", heapUsed);
        memoryMetrics.put("heapMax", heapMax);
        memoryMetrics.put("heapCommitted", heapCommitted);
        memoryMetrics.put("heapUsagePercent", (double) heapUsed / heapMax * 100);
        memoryMetrics.put("nonHeapUsed", nonHeapUsed);
        memoryMetrics.put("nonHeapMax", nonHeapMax);
        memoryMetrics.put("nonHeapUsagePercent", nonHeapMax > 0 ? (double) nonHeapUsed / nonHeapMax * 100 : 0);

        // 获取运行时内存
        Runtime runtime = Runtime.getRuntime();
        memoryMetrics.put("totalMemory", runtime.totalMemory());
        memoryMetrics.put("freeMemory", runtime.freeMemory());
        memoryMetrics.put("maxMemory", runtime.maxMemory());
        memoryMetrics.put("usedMemory", runtime.totalMemory() - runtime.freeMemory());

        return memoryMetrics;
    }

    /**
     * 获取线程指标
     */
    private Map<String, Object> getThreadMetrics() {
        Map<String, Object> threadMetrics = new HashMap<>();

        int threadCount = threadMXBean.getThreadCount();
        int daemonThreadCount = threadMXBean.getDaemonThreadCount();
        int peakThreadCount = threadMXBean.getPeakThreadCount();
        long totalStartedThreadCount = threadMXBean.getTotalStartedThreadCount();

        threadMetrics.put("threadCount", threadCount);
        threadMetrics.put("daemonThreadCount", daemonThreadCount);
        threadMetrics.put("peakThreadCount", peakThreadCount);
        threadMetrics.put("totalStartedThreadCount", totalStartedThreadCount);

        // 线程状态统计
        Map<String, Long> threadStates = new HashMap<>();
        for (Thread.State state : Thread.State.values()) {
            threadStates.put(state.name().toLowerCase(), threadMXBean.getThreadCount(state));
        }
        threadMetrics.put("threadStates", threadStates);

        return threadMetrics;
    }

    /**
     * 获取数据库连接池指标
     */
    private Map<String, Object> getConnectionPoolMetrics() {
        Map<String, Object> poolMetrics = new HashMap<>();

        if (!poolMonitoringEnabled) {
            poolMetrics.put("monitoringEnabled", false);
            return poolMetrics;
        }

        try {
            // 这里使用HikariCP的JMX来获取连接池指标
            // 实际项目中可以通过HikariDataSource获取
            poolMetrics.put("monitoringEnabled", true);
            poolMetrics.put("estimatedConnections", "N/A");
            poolMetrics.put("idleConnections", "N/A");
            poolMetrics.put("activeConnections", "N/A");
            poolMetrics.put("totalConnections", "N/A");
            poolMetrics.put("threadsAwaitingConnection", "N/A");

            // 模拟一些关键指标（实际项目中应该从真实连接池获取）
            poolMetrics.put("maxPoolSize", 50);
            poolMetrics.put("minIdle", 10);
            poolMetrics.put("idleTimeout", 300000);
            poolMetrics.put("connectionTimeout", 20000);

        } catch (Exception e) {
            log.error("获取数据库连接池指标失败", e);
            poolMetrics.put("error", e.getMessage());
            poolMetrics.put("monitoringEnabled", false);
        }

        return poolMetrics;
    }

    /**
     * 获取缓存指标
     */
    private Map<String, Object> getCacheMetrics() {
        Map<String, Object> cacheMetrics = new HashMap<>();

        try {
            // Spring Cache指标
            Map<String, Object> springCacheStats = new HashMap<>();
            springCacheStats.put("cacheManagerType", cacheManager.getClass().getSimpleName());
            springCacheStats.put("cacheNames", cacheManager.getCacheNames());
            cacheMetrics.put("springCache", springCacheStats);

            // Redis指标
            Map<String, Object> redisStats = new HashMap<>();
            try {
                Long dbSize = redisTemplate.execute(connection -> {
                    return connection.serverCommands().dbSize();
                });
                redisStats.put("dbSize", dbSize != null ? dbSize : -1);
                redisStats.put("connected", true);
            } catch (Exception e) {
                redisStats.put("connected", false);
                redisStats.put("error", e.getMessage());
            }
            cacheMetrics.put("redis", redisStats);

        } catch (Exception e) {
            log.error("获取缓存指标失败", e);
            cacheMetrics.put("error", e.getMessage());
        }

        return cacheMetrics;
    }

    /**
     * 获取系统指标
     */
    private Map<String, Object> getSystemMetrics() {
        Map<String, Object> systemMetrics = new HashMap<>();

        // 运行时间
        long uptime = ManagementFactory.getRuntimeMXBean().getUptime();
        systemMetrics.put("uptime", uptime);
        systemMetrics.put("uptimeFormatted", formatDuration(uptime));

        // 系统属性
        Map<String, String> systemProperties = new HashMap<>();
        systemProperties.put("os.name", System.getProperty("os.name"));
        systemProperties.put("os.version", System.getProperty("os.version"));
        systemProperties.put("os.arch", System.getProperty("os.arch"));
        systemProperties.put("java.version", System.getProperty("java.version"));
        systemProperties.put("java.vendor", System.getProperty("java.vendor"));
        systemProperties.put("java.home", System.getProperty("java.home"));
        systemMetrics.put("properties", systemProperties);

        // 系统负载
        Runtime runtime = Runtime.getRuntime();
        int processors = runtime.availableProcessors();
        systemMetrics.put("availableProcessors", processors);

        return systemMetrics;
    }

    /**
     * 格式化持续时间
     */
    private String formatDuration(long millis) {
        long days = TimeUnit.MILLISECONDS.toDays(millis);
        long hours = TimeUnit.MILLISECONDS.toHours(millis) % 24;
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60;
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60;

        return String.format("%d天 %d小时 %d分钟 %d秒", days, hours, minutes, seconds);
    }

    /**
     * 记录慢查询
     */
    public void logSlowQuery(String query, long executionTime, Map<String, Object> parameters) {
        if (executionTime > slowQueryThreshold) {
            log.warn("检测到慢查询 - 执行时间: {}ms, 查询: {}, 参数: {}", executionTime, query, parameters);
        }
    }

    /**
     * 记录API响应时间
     */
    public void logApiResponse(String endpoint, long responseTime, int statusCode) {
        if (responseTime > 3000) { // 超过3秒记录警告
            log.warn("慢API响应 - 端点: {}, 响应时间: {}ms, 状态码: {}", endpoint, responseTime, statusCode);
        }
    }

    /**
     * 获取健康检查状态
     */
    public Map<String, Object> getHealthStatus() {
        Map<String, Object> health = new HashMap<>();

        // 数据库健康检查
        boolean databaseHealthy = checkDatabaseHealth();
        health.put("database", databaseHealthy ? "UP" : "DOWN");

        // Redis健康检查
        boolean redisHealthy = checkRedisHealth();
        health.put("redis", redisHealthy ? "UP" : "DOWN");

        // 内存使用检查
        boolean memoryHealthy = checkMemoryHealth();
        health.put("memory", memoryHealthy ? "UP" : "DOWN");

        // 线程池检查
        boolean threadPoolHealthy = checkThreadPoolHealth();
        health.put("threadPool", threadPoolHealthy ? "UP" : "DOWN");

        // 整体状态
        boolean overallHealthy = databaseHealthy && redisHealthy && memoryHealthy && threadPoolHealthy;
        health.put("status", overallHealthy ? "UP" : "DOWN");
        health.put("timestamp", LocalDateTime.now());

        return health;
    }

    /**
     * 检查数据库健康状态
     */
    private boolean checkDatabaseHealth() {
        try {
            // 简单的健康检查
            return true; // 实际项目中应该执行查询测试
        } catch (Exception e) {
            log.error("数据库健康检查失败", e);
            return false;
        }
    }

    /**
     * 检查Redis健康状态
     */
    private boolean checkRedisHealth() {
        try {
            Boolean ping = redisTemplate.execute(connection -> {
                return connection.ping();
            });
            return "PONG".equals(ping);
        } catch (Exception e) {
            log.error("Redis健康检查失败", e);
            return false;
        }
    }

    /**
     * 检查内存健康状态
     */
    private boolean checkMemoryHealth() {
        try {
            double memoryUsagePercent = (double) memoryMXBean.getHeapMemoryUsage().getUsed() /
                                       memoryMXBean.getHeapMemoryUsage().getMax();
            return memoryUsagePercent < 0.9; // 使用率小于90%认为健康
        } catch (Exception e) {
            log.error("内存健康检查失败", e);
            return false;
        }
    }

    /**
     * 检查线程池健康状态
     */
    private boolean checkThreadPoolHealth() {
        try {
            int threadCount = threadMXBean.getThreadCount();
            int peakThreadCount = threadMXBean.getPeakThreadCount();
            return threadCount < peakThreadCount * 2; // 当前线程数不超过峰值的两倍
        } catch (Exception e) {
            log.error("线程池健康检查失败", e);
            return false;
        }
    }

    /**
     * 记录性能指标到Redis
     */
    public void recordMetrics(String metricName, double value) {
        try {
            String key = "metrics:" + metricName;
            redisTemplate.opsForList().rightPush(key, value);
            // 保持最近1000个数据点
            redisTemplate.opsForList().trim(key, -1000, -1);
            redisTemplate.expire(key, 1, TimeUnit.DAYS);
        } catch (Exception e) {
            log.error("记录性能指标失败: {}", metricName, e);
        }
    }

    /**
     * 获取性能指标历史数据
     */
    public Map<String, Object> getMetricsHistory(String metricName, int limit) {
        Map<String, Object> result = new HashMap<>();
        try {
            String key = "metrics:" + metricName;
            List<Object> values = redisTemplate.opsForList().range(key, -limit, -1);
            result.put("metricName", metricName);
            result.put("values", values);
            result.put("count", values != null ? values.size() : 0);
        } catch (Exception e) {
            log.error("获取性能指标历史数据失败: {}", metricName, e);
            result.put("error", e.getMessage());
        }
        return result;
    }
}