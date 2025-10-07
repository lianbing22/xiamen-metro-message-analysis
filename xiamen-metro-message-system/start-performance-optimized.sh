#!/bin/bash

# 厦门地铁设备报文分析系统 - 性能优化启动脚本

echo "=========================================="
echo "厦门地铁设备报文分析系统"
echo "性能优化版本 v1.0.0"
echo "=========================================="

# 检查Java环境
if ! command -v java &> /dev/null; then
    echo "错误: Java 未安装或未配置到PATH中"
    echo "请安装 Java 17 或更高版本"
    exit 1
fi

# 检查PostgreSQL和Redis
echo "检查依赖服务..."

# 检查PostgreSQL
if ! pgrep -x "postgres" > /dev/null; then
    echo "警告: PostgreSQL 可能未运行"
    echo "请确保 PostgreSQL 服务已启动"
fi

# 检查Redis
if ! pgrep -x "redis-server" > /dev/null; then
    echo "警告: Redis 可能未运行"
    echo "请确保 Redis 服务已启动"
fi

# 设置环境变量
export JAVA_OPTS="-server"
export SPRING_PROFILES_ACTIVE="performance"

# 构建项目（如果需要）
if [ ! -f "backend/target/message-analysis-system-1.0.0.jar" ]; then
    echo "构建项目..."
    cd backend
    ./mvnw clean package -DskipTests
    cd ..
fi

# 检查JAR文件
if [ ! -f "backend/target/message-analysis-system-1.0.0.jar" ]; then
    echo "错误: 找不到JAR文件，请先构建项目"
    exit 1
fi

# 使用性能优化配置启动
echo "启动性能优化版本..."
echo "环境配置: ${SPRING_PROFILES_ACTIVE}"
echo "启动时间: $(date)"
echo "=========================================="

# 使用JVM优化脚本启动
./jvm-optimized.sh prod performance 8080