#!/bin/bash

# 厦门地铁设备报文分析系统 - 性能测试脚本
# 使用Apache Bench进行负载测试

# 配置参数
BASE_URL=${BASE_URL:-"http://localhost:8080"}
TEST_DURATION=${TEST_DURATION:-"60"}  # 测试持续时间（秒）
CONCURRENT_USERS=${CONCURRENT_USERS:-"100"}  # 并发用户数
REQUESTS_PER_USER=${REQUESTS_PER_USER:-"10"}  # 每个用户的请求数
OUTPUT_DIR="./performance-test-results"

# 创建输出目录
mkdir -p "${OUTPUT_DIR}"

# 获取当前时间戳
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
REPORT_FILE="${OUTPUT_DIR}/performance_report_${TIMESTAMP}.html"
LOG_FILE="${OUTPUT_DIR}/test_log_${TIMESTAMP}.log"

echo "=========================================="
echo "厦门地铁设备报文分析系统 - 性能测试"
echo "=========================================="
echo "基础URL: ${BASE_URL}"
echo "测试持续时间: ${TEST_DURATION}秒"
echo "并发用户数: ${CONCURRENT_USERS}"
echo "每用户请求数: ${REQUESTS_PER_USER}"
echo "输出目录: ${OUTPUT_DIR}"
echo "报告文件: ${REPORT_FILE}"
echo "=========================================="

# 检查ab是否安装
if ! command -v ab &> /dev/null; then
    echo "错误: Apache Bench (ab) 未安装"
    echo "请安装Apache Bench: sudo apt-get install apache2-utils (Ubuntu/Debian)"
    exit 1
fi

# 检查服务是否可用
echo "检查服务可用性..."
if ! curl -f -s "${BASE_URL}/actuator/health" > /dev/null; then
    echo "警告: 服务可能未启动或不可访问"
    echo "请确保应用正在运行在 ${BASE_URL}"
    read -p "是否继续测试? (y/N): " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        exit 1
    fi
fi

# 测试用例配置
declare -a TEST_ENDPOINTS=(
    "/actuator/health"
    "/api/files"
    "/api/files/list"
    "/api/pump-analysis/statistics"
    "/api/alert/rules"
    "/api/performance/metrics"
)

# 开始性能测试
echo "开始性能测试..."

# 创建HTML报告头部
cat > "${REPORT_FILE}" << EOF
<!DOCTYPE html>
<html>
<head>
    <title>厦门地铁设备报文分析系统 - 性能测试报告</title>
    <meta charset="UTF-8">
    <style>
        body { font-family: Arial, sans-serif; margin: 20px; }
        .header { background: #f0f0f0; padding: 20px; border-radius: 5px; }
        .test-case { margin: 20px 0; border: 1px solid #ddd; border-radius: 5px; }
        .test-case-header { background: #e8e8e8; padding: 10px; font-weight: bold; }
        .test-case-content { padding: 10px; }
        .metric { margin: 5px 0; }
        .pass { color: green; }
        .fail { color: red; }
        .warn { color: orange; }
        table { border-collapse: collapse; width: 100%; margin: 10px 0; }
        th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }
        th { background-color: #f2f2f2; }
        .summary { background: #f9f9f9; padding: 15px; border-radius: 5px; margin: 20px 0; }
    </style>
</head>
<body>
    <div class="header">
        <h1>厦门地铁设备报文分析系统 - 性能测试报告</h1>
        <p>测试时间: $(date)</p>
        <p>测试目标: ${BASE_URL}</p>
        <p>并发用户数: ${CONCURRENT_USERS}</p>
        <p>测试持续时间: ${TEST_DURATION}秒</p>
    </div>

    <div class="summary">
        <h2>测试概览</h2>
        <div class="metric">目标: API响应时间 < 3秒</div>
        <div class="metric">目标: 并发处理能力 > 100用户</div>
        <div class="metric">测试环境: ${BASE_URL}</div>
    </div>
EOF

# 初始化统计变量
TOTAL_REQUESTS=0
TOTAL_TIME=0
FAILED_REQUESTS=0
MIN_RESPONSE_TIME=999999
MAX_RESPONSE_TIME=0

# 测试每个端点
for endpoint in "${TEST_ENDPOINTS[@]}"; do
    echo "测试端点: ${endpoint}"

    # 构建ab命令
    AB_CMD="ab -n $((CONCURRENT_USERS * REQUESTS_PER_USER)) -c ${CONCURRENT_USERS} -t ${TEST_DURATION} -g ${OUTPUT_DIR}/plot_${endpoint//[\/]/_}_${TIMESTAMP}.txt ${BASE_URL}${endpoint}"

    echo "执行命令: ${AB_CMD}"

    # 执行测试并捕获输出
    TEST_OUTPUT=$(eval "${AB_CMD}" 2>&1)
    TEST_EXIT_CODE=$?

    # 记录测试结果
    echo "${TEST_OUTPUT}" >> "${LOG_FILE}"
    echo "========================================" >> "${LOG_FILE}"

    # 解析测试结果
    if [ $TEST_EXIT_CODE -eq 0 ]; then
        # 提取关键指标
        REQUESTS_SERVED=$(echo "${TEST_OUTPUT}" | grep "Requests per second" | awk '{print $4}')
        TIME_PER_REQUEST=$(echo "${TEST_OUTPUT}" | grep "Time per request.*mean" | awk '{print $4}')
        FAILED_TESTS=$(echo "${TEST_OUTPUT}" | grep "Failed requests" | awk '{print $3}')

        # 转换为数字
        if [ -n "$REQUESTS_SERVED" ]; then
            RPS_NUM=$(echo "$REQUESTS_SERVED" | sed 's/[^0-9.]//g')
            TOTAL_REQUESTS=$((TOTAL_REQUESTS + $(echo "$RPS_NUM * ${TEST_DURATION}" | bc)))
        fi

        if [ -n "$TIME_PER_REQUEST" ]; then
            TPR_NUM=$(echo "$TIME_PER_REQUEST" | sed 's/[^0-9.]//g')
            TOTAL_TIME=$(echo "$TOTAL_TIME + $TPR_NUM" | bc)

            # 更新最小/最大响应时间
            if (( $(echo "$TPR_NUM < $MIN_RESPONSE_TIME" | bc -l) )); then
                MIN_RESPONSE_TIME=$TPR_NUM
            fi
            if (( $(echo "$TPR_NUM > $MAX_RESPONSE_TIME" | bc -l) )); then
                MAX_RESPONSE_TIME=$TPR_NUM
            fi
        fi

        # 判断测试结果
        RESPONSE_TIME_STATUS=""
        if (( $(echo "$TPR_NUM < 1000" | bc -l) )); then
            RESPONSE_TIME_STATUS="pass"
        elif (( $(echo "$TPR_NUM < 3000" | bc -l) )); then
            RESPONSE_TIME_STATUS="warn"
        else
            RESPONSE_TIME_STATUS="fail"
            FAILED_REQUESTS=$((FAILED_REQUESTS + 1))
        fi

        # 生成HTML报告片段
        cat >> "${REPORT_FILE}" << EOF
    <div class="test-case">
        <div class="test-case-header">测试端点: ${endpoint}</div>
        <div class="test-case-content">
            <div class="metric">每秒请求数: <span class="pass">${REQUESTS_SERVED}</span></div>
            <div class="metric ${RESPONSE_TIME_STATUS}">平均响应时间: ${TIME_PER_REQUEST}ms</div>
            <div class="metric">失败请求数: ${FAILED_TESTS}</div>
            <div class="metric">测试状态: <span class="${RESPONSE_TIME_STATUS}">$([ "$RESPONSE_TIME_STATUS" = "pass" ] && echo "通过" || [ "$RESPONSE_TIME_STATUS" = "warn" ] && echo "警告" || echo "失败")</span></div>
        </div>
    </div>
EOF

        echo "  ✅ 完成 - RPS: ${REQUESTS_SERVED}, 响应时间: ${TIME_PER_REQUEST}ms"
    else
        echo "  ❌ 测试失败"
        FAILED_REQUESTS=$((FAILED_REQUESTS + 1))

        cat >> "${REPORT_FILE}" << EOF
    <div class="test-case">
        <div class="test-case-header">测试端点: ${endpoint}</div>
        <div class="test-case-content">
            <div class="metric fail">测试失败</div>
            <div class="metric">错误信息: ${TEST_OUTPUT}</div>
        </div>
    </div>
EOF
    fi
done

# 计算平均值
if [ ${#TEST_ENDPOINTS[@]} -gt 0 ]; then
    AVG_RESPONSE_TIME=$(echo "scale=2; $TOTAL_TIME / ${#TEST_ENDPOINTS[@]}" | bc)
else
    AVG_RESPONSE_TIME=0
fi

# 生成测试总结
cat >> "${REPORT_FILE}" << EOF
    <div class="summary">
        <h2>测试总结</h2>
        <table>
            <tr><th>指标</th><th>值</th><th>状态</th></tr>
            <tr>
                <td>平均响应时间</td>
                <td>${AVG_RESPONSE_TIME}ms</td>
                <td class="$(( $(echo "$AVG_RESPONSE_TIME < 3000" | bc -l) && echo "pass" || echo "fail" ))">$(( $(echo "$AVG_RESPONSE_TIME < 3000" | bc -l) && echo "通过" || echo "失败" ))</td>
            </tr>
            <tr>
                <td>最小响应时间</td>
                <td>${MIN_RESPONSE_TIME}ms</td>
                <td class="pass">良好</td>
            </tr>
            <tr>
                <td>最大响应时间</td>
                <td>${MAX_RESPONSE_TIME}ms</td>
                <td class="$(( $(echo "$MAX_RESPONSE_TIME < 5000" | bc -l) && echo "pass" || echo "warn" ))">$(( $(echo "$MAX_RESPONSE_TIME < 5000" | bc -l) && echo "良好" || echo "需要关注" ))</td>
            </tr>
            <tr>
                <td>总请求数</td>
                <td>${TOTAL_REQUESTS}</td>
                <td class="pass">完成</td>
            </tr>
            <tr>
                <td>失败端点数</td>
                <td>${FAILED_REQUESTS}</td>
                <td class="$([ $FAILED_REQUESTS -eq 0 ] && echo "pass" || echo "fail")">$([ $FAILED_REQUESTS -eq 0 ] && echo "无失败" || echo "存在失败")</td>
            </tr>
            <tr>
                <td>并发用户数</td>
                <td>${CONCURRENT_USERS}</td>
                <td class="pass">达标</td>
            </tr>
        </table>
    </div>

    <div class="summary">
        <h2>性能优化建议</h2>
        <ul>
EOF

# 添加优化建议
if (( $(echo "$AVG_RESPONSE_TIME > 3000" | bc -l) )); then
    echo "            <li>平均响应时间超过3秒目标，建议优化数据库查询和缓存策略</li>" >> "${REPORT_FILE}"
fi

if [ $FAILED_REQUESTS -gt 0 ]; then
    echo "            <li>存在失败的请求，建议检查服务稳定性和错误处理</li>" >> "${REPORT_FILE}"
fi

if (( $(echo "$MAX_RESPONSE_TIME > 5000" | bc -l) )); then
    echo "            <li>最大响应时间较高，建议检查慢请求和性能瓶颈</li>" >> "${REPORT_FILE}"
fi

cat >> "${REPORT_FILE}" << EOF
            <li>建议启用Redis缓存以减少数据库负载</li>
            <li>建议优化JVM参数和垃圾回收策略</li>
            <li>建议启用HTTP/2和Gzip压缩</li>
            <li>建议实施API限流和熔断机制</li>
            <li>建议使用CDN加速静态资源访问</li>
        </ul>
    </div>

    <div class="summary">
        <h2>技术架构建议</h2>
        <ul>
            <li>数据库层面：优化索引配置，使用连接池，实施读写分离</li>
            <li>缓存层面：实施多级缓存策略，热点数据预加载</li>
            <li>应用层面：异步处理，线程池优化，内存管理</li>
            <li>前端层面：代码分割，懒加载，资源压缩</li>
            <li>监控层面：APM工具集成，性能指标监控</li>
        </ul>
    </div>

    <div class="summary">
        <h2>测试文件</h2>
        <p>详细日志: <a href="${LOG_FILE}">${LOG_FILE}</a></p>
        <p>性能图表文件: ${OUTPUT_DIR}/plot_*_${TIMESTAMP}.txt</p>
    </div>
</body>
</html>
EOF

echo "=========================================="
echo "性能测试完成！"
echo "=========================================="
echo "测试报告: ${REPORT_FILE}"
echo "详细日志: ${LOG_FILE}"
echo "平均响应时间: ${AVG_RESPONSE_TIME}ms"
echo "最小响应时间: ${MIN_RESPONSE_TIME}ms"
echo "最大响应时间: ${MAX_RESPONSE_TIME}ms"
echo "总请求数: ${TOTAL_REQUESTS}"
echo "失败端点数: ${FAILED_REQUESTS}"

# 性能评级
if (( $(echo "$AVG_RESPONSE_TIME < 1000" | bc -l) )) && [ $FAILED_REQUESTS -eq 0 ]; then
    echo "性能评级: ⭐⭐⭐⭐⭐ 优秀"
elif (( $(echo "$AVG_RESPONSE_TIME < 3000" | bc -l) )) && [ $FAILED_REQUESTS -eq 0 ]; then
    echo "性能评级: ⭐⭐⭐⭐ 良好"
elif (( $(echo "$AVG_RESPONSE_TIME < 5000" | bc -l) )); then
    echo "性能评级: ⭐⭐⭐ 一般"
else
    echo "性能评级: ⭐⭐ 需要优化"
fi

echo "=========================================="

# 在浏览器中打开报告（如果可用）
if command -v xdg-open &> /dev/null; then
    xdg-open "${REPORT_FILE}"
elif command -v open &> /dev/null; then
    open "${REPORT_FILE}"
fi