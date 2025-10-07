#!/bin/bash

# 厦门地铁设备报文分析系统 - 开发环境设置脚本

set -e

echo "====================================="
echo "厦门地铁设备报文分析系统"
echo "开发环境设置脚本"
echo "====================================="

# 检查Docker是否已安装
check_docker() {
    if ! command -v docker &> /dev/null; then
        echo "错误: Docker未安装"
        echo "请先安装Docker: https://docs.docker.com/get-docker/"
        exit 1
    fi

    if ! docker info > /dev/null 2>&1; then
        echo "错误: Docker服务未运行"
        echo "请启动Docker服务"
        exit 1
    fi

    echo "✓ Docker已安装并运行"
}

# 检查Docker Compose是否已安装
check_docker_compose() {
    if ! command -v docker-compose &> /dev/null && ! docker compose version &> /dev/null; then
        echo "错误: Docker Compose未安装"
        echo "请先安装Docker Compose"
        exit 1
    fi

    echo "✓ Docker Compose已安装"
}

# 检查Node.js是否已安装
check_nodejs() {
    if ! command -v node &> /dev/null; then
        echo "警告: Node.js未安装，前端开发将受限"
        echo "请安装Node.js 18+: https://nodejs.org/"
        return 1
    fi

    NODE_VERSION=$(node -v | cut -d'.' -f1 | cut -d'v' -f2)
    if [ "$NODE_VERSION" -lt 18 ]; then
        echo "警告: Node.js版本过低，建议升级到18+"
        return 1
    fi

    echo "✓ Node.js已安装 ($(node -v))"
    return 0
}

# 检查Java是否已安装
check_java() {
    if ! command -v java &> /dev/null; then
        echo "警告: Java未安装，后端开发将受限"
        echo "请安装Java 17+: https://adoptium.net/"
        return 1
    fi

    JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | cut -d'.' -f1)
    if [ "$JAVA_VERSION" -lt 17 ]; then
        echo "警告: Java版本过低，建议升级到17+"
        return 1
    fi

    echo "✓ Java已安装 ($(java -version 2>&1 | head -n 1))"
    return 0
}

# 创建必要的目录
create_directories() {
    echo "创建项目目录..."

    mkdir -p logs
    mkdir -p uploads
    mkdir -p data/postgres
    mkdir -p data/redis

    echo "✓ 目录创建完成"
}

# 创建环境配置文件
create_env_files() {
    echo "创建环境配置文件..."

    # 创建前端环境配置
    if [ ! -f "frontend/.env.development" ]; then
        cat > frontend/.env.development << EOF
# 开发环境配置
VITE_API_BASE_URL=http://localhost:8080
VITE_APP_TITLE=厦门地铁设备报文分析系统(开发)
VITE_APP_ENV=development
VITE_MOCK_ENABLED=false
EOF
        echo "✓ 前端环境配置已创建"
    fi

    # 创建后端环境配置
    if [ ! -f "backend/.env" ]; then
        cat > backend/.env << EOF
# 开发环境配置
SPRING_PROFILES_ACTIVE=dev
DB_HOST=localhost
DB_PORT=5432
DB_NAME=xiamen_metro_message
DB_USER=metro_user
DB_PASSWORD=metro_password
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=
JWT_SECRET=xiamen-metro-jwt-secret-key-dev
UPLOAD_PATH=./uploads
EOF
        echo "✓ 后端环境配置已创建"
    fi

    # 创建Docker环境配置
    if [ ! -f "docker/.env.dev" ]; then
        cat > docker/.env.dev << EOF
# 开发环境Docker配置
DB_NAME=xiamen_metro_message
DB_USER=metro_user
DB_PASSWORD=metro_password
REDIS_PASSWORD=
JWT_SECRET=xiamen-metro-jwt-secret-key-dev
NODE_ENV=development
SPRING_PROFILES_ACTIVE=dev
EOF
        echo "✓ Docker环境配置已创建"
    fi
}

# 启动基础设施服务
start_infrastructure() {
    echo "启动基础设施服务..."

    # 使用docker-compose或docker compose
    if command -v docker-compose &> /dev/null; then
        COMPOSE_CMD="docker-compose"
    else
        COMPOSE_CMD="docker compose"
    fi

    # 启动PostgreSQL和Redis
    $COMPOSE_CMD -f docker/docker-compose.dev.yml up -d postgres redis

    echo "等待数据库启动..."
    sleep 10

    # 检查数据库状态
    if $COMPOSE_CMD -f docker/docker-compose.dev.yml exec -T postgres pg_isready -U metro_user > /dev/null 2>&1; then
        echo "✓ 数据库启动成功"
    else
        echo "警告: 数据库启动失败，请检查日志"
        $COMPOSE_CMD -f docker/docker-compose.dev.yml logs postgres
    fi

    # 检查Redis状态
    if $COMPOSE_CMD -f docker/docker-compose.dev.yml exec -T redis redis-cli ping > /dev/null 2>&1; then
        echo "✓ Redis启动成功"
    else
        echo "警告: Redis启动失败，请检查日志"
        $COMPOSE_CMD -f docker/docker-compose.dev.yml logs redis
    fi
}

# 初始化数据库
init_database() {
    echo "初始化数据库..."

    if command -v docker-compose &> /dev/null; then
        COMPOSE_CMD="docker-compose"
    else
        COMPOSE_CMD="docker compose"
    fi

    # 执行数据库初始化脚本
    $COMPOSE_CMD -f docker/docker-compose.dev.yml exec -T postgres psql -U metro_user -d xiamen_metro_message -f /docker-entrypoint-initdb.d/01-create-database.sql > /dev/null 2>&1
    $COMPOSE_CMD -f docker/docker-compose.dev.yml exec -T postgres psql -U metro_user -d xiamen_metro_message -f /docker-entrypoint-initdb.d/02-create-tables.sql > /dev/null 2>&1
    $COMPOSE_CMD -f docker/docker-compose.dev.yml exec -T postgres psql -U metro_user -d xiamen_metro_message -f /docker-entrypoint-initdb.d/03-insert-initial-data.sql > /dev/null 2>&1

    echo "✓ 数据库初始化完成"
}

# 安装前端依赖
install_frontend_deps() {
    if [ -d "frontend" ] && check_nodejs; then
        echo "安装前端依赖..."
        cd frontend
        npm install
        cd ..
        echo "✓ 前端依赖安装完成"
    fi
}

# 安装后端依赖
install_backend_deps() {
    if [ -d "backend" ] && check_java; then
        echo "安装后端依赖..."
        cd backend
        ./mvnw clean install -DskipTests
        cd ..
        echo "✓ 后端依赖安装完成"
    fi
}

# 显示启动说明
show_instructions() {
    echo ""
    echo "====================================="
    echo "开发环境设置完成！"
    echo "====================================="
    echo ""
    echo "启动说明："
    echo "1. 启动后端服务:"
    echo "   cd backend && ./mvnw spring-boot:run"
    echo ""
    echo "2. 启动前端服务:"
    echo "   cd frontend && npm run dev"
    echo ""
    echo "3. 使用Docker启动所有服务:"
    echo "   docker-compose -f docker/docker-compose.dev.yml up -d"
    echo ""
    echo "4. 访问应用:"
    echo "   前端界面: http://localhost:3000"
    echo "   后端API: http://localhost:8080"
    echo "   API文档: http://localhost:8080/swagger-ui.html"
    echo ""
    echo "5. 默认登录账号:"
    echo "   用户名: admin"
    echo "   密码: admin"
    echo ""
    echo "6. 查看服务状态:"
    echo "   docker-compose -f docker/docker-compose.dev.yml ps"
    echo ""
    echo "7. 查看日志:"
    echo "   docker-compose -f docker/docker-compose.dev.yml logs -f [service]"
    echo ""
    echo "====================================="
}

# 主函数
main() {
    echo "开始设置开发环境..."
    echo ""

    check_docker
    check_docker_compose
    check_nodejs
    check_java
    create_directories
    create_env_files
    start_infrastructure
    init_database
    install_frontend_deps
    install_backend_deps

    show_instructions
}

# 执行主函数
main "$@"