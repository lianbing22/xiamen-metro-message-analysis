package com.xiamen.metro.message.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.util.Properties;

/**
 * 数据库配置
 * 优化连接池和JPA配置
 */
@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(basePackages = "com.xiamen.metro.message.repository")
public class DatabaseConfig {

    @Bean
    @Primary
    @ConfigurationProperties(prefix = "spring.datasource.hikari")
    public HikariConfig hikariConfig() {
        HikariConfig config = new HikariConfig();

        // 连接池配置优化
        config.setMinimumIdle(10); // 最小空闲连接数
        config.setMaximumPoolSize(50); // 最大连接池大小
        config.setIdleTimeout(300000); // 空闲连接超时时间 5分钟
        config.setConnectionTimeout(20000); // 连接超时时间 20秒
        config.setMaxLifetime(1800000); // 连接最大生命周期 30分钟
        config.setLeakDetectionThreshold(60000); // 连接泄漏检测阈值 60秒

        // 性能优化配置
        config.setAutoCommit(false); // 禁用自动提交
        config.setConnectionTestQuery("SELECT 1"); // 连接测试查询
        config.setValidationTimeout(5000); // 验证超时时间
        config.setPoolName("MetroHikariPool"); // 连接池名称

        return config;
    }

    @Bean
    @Primary
    public DataSource dataSource() {
        return new HikariDataSource(hikariConfig());
    }

    @Bean
    @Primary
    public LocalContainerEntityManagerFactoryBean entityManagerFactory() {
        LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(dataSource());
        em.setPackagesToScan("com.xiamen.metro.message.entity");

        HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        em.setJpaVendorAdapter(vendorAdapter);

        Properties jpaProperties = new Properties();

        // 基础配置
        jpaProperties.put("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
        jpaProperties.put("hibernate.show_sql", "false");
        jpaProperties.put("hibernate.format_sql", "true");
        jpaProperties.put("hibernate.use_sql_comments", "true");

        // 性能优化配置
        jpaProperties.put("hibernate.jdbc.batch_size", "50"); // 批处理大小
        jpaProperties.put("hibernate.order_inserts", "true"); // 优化插入顺序
        jpaProperties.put("hibernate.order_updates", "true"); // 优化更新顺序
        jpaProperties.put("hibernate.jdbc.batch_versioned_data", "true"); // 批处理版本化数据
        jpaProperties.put("hibernate.jdbc.fetch_size", "100"); // JDBC获取大小
        jpaProperties.put("hibernate.cache.use_second_level_cache", "true"); // 启用二级缓存
        jpaProperties.put("hibernate.cache.use_query_cache", "true"); // 启用查询缓存
        jpaProperties.put("hibernate.cache.region.factory_class", "org.hibernate.cache.jcache.JCacheRegionFactory");

        // 连接优化
        jpaProperties.put("hibernate.connection.provider_disables_autocommit", "true");
        jpaProperties.put("hibernate.connection.handling_mode", "DELAYED_ACQUISITION_AND_RELEASE_AFTER_STATEMENT");

        // 统计和监控
        jpaProperties.put("hibernate.generate_statistics", "false");
        jpaProperties.put("hibernate.session.events.log.LOG_QUERIES_SLOWER_THAN_MS", "1000");

        em.setJpaProperties(jpaProperties);
        em.setPersistenceUnitName("metro-message-persistence-unit");

        return em;
    }

    @Bean
    @Primary
    public PlatformTransactionManager transactionManager() {
        JpaTransactionManager transactionManager = new JpaTransactionManager();
        transactionManager.setEntityManagerFactory(entityManagerFactory().getObject());
        return transactionManager;
    }

    /**
     * 读写分离数据源配置（如果需要）
     */
    @Bean
    @ConfigurationProperties(prefix = "spring.datasource.read")
    public HikariConfig readHikariConfig() {
        HikariConfig config = new HikariConfig();
        config.setMinimumIdle(5);
        config.setMaximumPoolSize(20);
        config.setIdleTimeout(300000);
        config.setConnectionTimeout(20000);
        config.setMaxLifetime(1800000);
        config.setPoolName("MetroReadHikariPool");
        return config;
    }

    /**
     * 只读数据源
     */
    @Bean("readDataSource")
    public DataSource readDataSource() {
        return new HikariDataSource(readHikariConfig());
    }
}