#!/bin/bash

# 厦门地铁设备报文分析系统 - JVM优化配置脚本
# 针对不同环境提供JVM参数优化

# 应用配置
APP_NAME="xiamen-metro-message-system"
JAR_FILE="backend/target/${APP_NAME}-1.0.0.jar"
LOG_DIR="./logs"
GC_LOG_DIR="${LOG_DIR}/gc"
HEAP_DUMP_DIR="${LOG_DIR}/heapdump"

# 创建日志目录
mkdir -p "${LOG_DIR}"
mkdir -p "${GC_LOG_DIR}"
mkdir -p "${HEAP_DUMP_DIR}"

# 获取系统配置
CPU_CORES=$(nproc)
TOTAL_MEMORY=$(free -m | awk '/Mem:/ {print $2}')
PHYSICAL_MEMORY=$((${TOTAL_MEMORY} * 1024 * 1024))

# 根据系统内存计算堆大小
if [ ${TOTAL_MEMORY} -le 2048 ]; then
    HEAP_SIZE=1024m
    MAX_HEAP_SIZE=1024m
elif [ ${TOTAL_MEMORY} -le 4096 ]; then
    HEAP_SIZE=2048m
    MAX_HEAP_SIZE=2048m
elif [ ${TOTAL_MEMORY} -le 8192 ]; then
    HEAP_SIZE=4096m
    MAX_HEAP_SIZE=4096m
else
    HEAP_SIZE=6144m
    MAX_HEAP_SIZE=6144m
fi

# 元空间配置
METASPACE_SIZE=256m
MAX_METASPACE_SIZE=512m

# 年轻代配置
YOUNG_GEN_SIZE=$((${HEAP_SIZE%?} / 4))m

# 环境变量
ENVIRONMENT=${1:-prod}
PROFILE=${2:-prod}
SERVER_PORT=${3:-8080}

echo "=========================================="
echo "厦门地铁设备报文分析系统 - JVM优化配置"
echo "=========================================="
echo "环境: ${ENVIRONMENT}"
echo "配置文件: ${PROFILE}"
echo "服务端口: ${SERVER_PORT}"
echo "CPU核心数: ${CPU_CORES}"
echo "系统内存: ${TOTAL_MEMORY}MB"
echo "堆内存: ${HEAP_SIZE}"
echo "最大堆内存: ${MAX_HEAP_SIZE}"
echo "年轻代: ${YOUNG_GEN_SIZE}"
echo "元空间: ${METASPACE_SIZE}"
echo "=========================================="

# 基础JVM参数
JVM_OPTS="-server"
JVM_OPTS="${JVM_OPTS} -Xms${HEAP_SIZE}"
JVM_OPTS="${JVM_OPTS} -Xmx${MAX_HEAP_SIZE}"
JVM_OPTS="${JVM_OPTS} -Xmn${YOUNG_GEN_SIZE}"
JVM_OPTS="${JVM_OPTS} -XX:MetaspaceSize=${METASPACE_SIZE}"
JVM_OPTS="${JVM_OPTS} -XX:MaxMetaspaceSize=${MAX_METASPACE_SIZE}"

# GC优化 - 使用G1GC
JVM_OPTS="${JVM_OPTS} -XX:+UseG1GC"
JVM_OPTS="${JVM_OPTS} -XX:MaxGCPauseMillis=200"
JVM_OPTS="${JVM_OPTS} -XX:G1HeapRegionSize=16m"
JVM_OPTS="${JVM_OPTS} -XX:G1NewSizePercent=30"
JVM_OPTS="${JVM_OPTS} -XX:G1MaxNewSizePercent=40"
JVM_OPTS="${JVM_OPTS} -XX:G1MixedGCCountTarget=8"
JVM_OPTS="${JVM_OPTS} -XX:InitiatingHeapOccupancyPercent=45"
JVM_OPTS="${JVM_OPTS} -XX:G1MixedGCLiveThresholdPercent=85"

# GC日志配置
JVM_OPTS="${JVM_OPTS} -XX:+PrintGCDetails"
JVM_OPTS="${JVM_OPTS} -XX:+PrintGCDateStamps"
JVM_OPTS="${JVM_OPTS} -XX:+PrintGCApplicationStoppedTime"
JVM_OPTS="${JVM_OPTS} -Xloggc:${GC_LOG_DIR}/gc-%t.log"
JVM_OPTS="${JVM_OPTS} -XX:+UseGCLogFileRotation"
JVM_OPTS="${JVM_OPTS} -XX:NumberOfGCLogFiles=10"
JVM_OPTS="${JVM_OPTS} -XX:GCLogFileSize=100M"

# OOM配置
JVM_OPTS="${JVM_OPTS} -XX:+HeapDumpOnOutOfMemoryError"
JVM_OPTS="${JVM_OPTS} -XX:HeapDumpPath=${HEAP_DUMP_DIR}/heapdump-%t.hprof"
JVM_OPTS="${JVM_OPTS} -XX:+PrintGCApplicationConcurrentTime"
JVM_OPTS="${JVM_OPTS} -XX:+PrintSafepointStatistics"
JVM_OPTS="${JVM_OPTS} -XX:+PrintGCApplicationStoppedTime"

# 性能监控
JVM_OPTS="${JVM_OPTS} -XX:+UnlockDiagnosticVMOptions"
JVM_OPTS="${JVM_OPTS} -XX:+LogVMOutput"
JVM_OPTS="${JVM_OPTS} -XX:LogFile=${LOG_DIR}/vm-output.log"
JVM_OPTS="${JVM_OPTS} -XX:+PrintCompilation"
JVM_OPTS="${JVM_OPTS} -XX:+PrintInlining"

# 优化编译器
JVM_OPTS="${JVM_OPTS} -XX:+TieredCompilation"
JVM_OPTS="${JVM_OPTS} -XX:TieredStopAtLevel=4"
JVM_OPTS="${JVM_OPTS} -XX:+UseStringDeduplication"

# 错误处理
JVM_OPTS="${JVM_OPTS} -XX:+ErrorFile=${LOG_DIR}/hs_err_pid%p.log"
JVM_OPTS="${JVM_OPTS} -XX:OnOutOfMemoryError=\"kill -9 %p\""

# 网络优化
JVM_OPTS="${JVM_OPTS} -Djava.net.preferIPv4Stack=true"
JVM_OPTS="${JVM_OPTS} -Dfile.encoding=UTF-8"
JVM_OPTS="${JVM_OPTS} -Duser.timezone=Asia/Shanghai"

# Spring Boot优化
JVM_OPTS="${JVM_OPTS} -Dspring.profiles.active=${PROFILE}"
JVM_OPTS="${JVM_OPTS} -Dserver.port=${SERVER_PORT}"
JVM_OPTS="${JVM_OPTS} -Dspring.jmx.enabled=false"
JVM_OPTS="${JVM_OPTS} -Dspring.backgroundpreinitializer.ignore=true"

# 文件描述符限制
JVM_OPTS="${JVM_OPTS} -XX:MaxDirectMemorySize=2g"

# 环境特定优化
if [ "${ENVIRONMENT}" = "prod" ]; then
    # 生产环境优化
    JVM_OPTS="${JVM_OPTS} -XX:+DisableExplicitGC"
    JVM_OPTS="${JVM_OPTS} -XX:+AlwaysPreTouch"
    JVM_OPTS="${JVM_OPTS} -XX:+UseFastUnorderedTimeStamps"
    JVM_OPTS="${JVM_OPTS} -Djava.awt.headless=true"
    JVM_OPTS="${JVM_OPTS} -XX:+OptimizeStringConcat"
    JVM_OPTS="${JVM_OPTS} -XX:+UseCompressedOops"
    JVM_OPTS="${JVM_OPTS} -XX:+UseCompressedClassPointers"

    # 生产环境日志级别
    JVM_OPTS="${JVM_OPTS} -Dlogging.level.com.xiamen.metro=WARN"
    JVM_OPTS="${JVM_OPTS} -Dlogging.level.org.springframework=WARN"
    JVM_OPTS="${JVM_OPTS} -Dlogging.level.org.hibernate=WARN"

elif [ "${ENVIRONMENT}" = "test" ]; then
    # 测试环境优化
    JVM_OPTS="${JVM_OPTS} -XX:+HeapDumpOnOutOfMemoryError"
    JVM_OPTS="${JVM_OPTS} -XX:+PrintGCDetails"
    JVM_OPTS="${JVM_OPTS} -XX:+PrintGCTimeStamps"

    # 测试环境日志级别
    JVM_OPTS="${JVM_OPTS} -Dlogging.level.com.xiamen.metro=INFO"
    JVM_OPTS="${JVM_OPTS} -Dlogging.level.org.springframework=INFO"

elif [ "${ENVIRONMENT}" = "dev" ]; then
    # 开发环境优化
    JVM_OPTS="${JVM_OPTS} -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005"
    JVM_OPTS="${JVM_OPTS} -XX:+HeapDumpOnOutOfMemoryError"
    JVM_OPTS="${JVM_OPTS} -Dspring.devtools.restart.enabled=true"

    # 开发环境日志级别
    JVM_OPTS="${JVM_OPTS} -Dlogging.level.com.xiamen.metro=DEBUG"
    JVM_OPTS="${JVM_OPTS} -Dlogging.level.org.springframework=INFO"
fi

# 性能监控参数
JVM_OPTS="${JVM_OPTS} -Dmanagement.endpoints.web.exposure.include=health,info,metrics,prometheus"
JVM_OPTS="${JVM_OPTS} -Dmanagement.endpoint.health.show-details=when-authorized"
JVM_OPTS="${JVM_OPTS} -Dmanagement.metrics.export.prometheus.enabled=true"

# 启动应用
echo "启动应用..."
echo "JVM参数: ${JVM_OPTS}"
echo "JAR文件: ${JAR_FILE}"

# 启动命令
START_CMD="java ${JVM_OPTS} -jar ${JAR_FILE}"

echo "执行命令: ${START_CMD}"
echo "=========================================="

# 执行启动
exec ${START_CMD}