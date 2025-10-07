#!/bin/bash

# 厦门地铁设备报文分析系统 - 生产环境部署脚本

set -e

# 配置变量
COMPOSE_FILE="docker/docker-compose.prod.yml"
ENV_FILE=".env.prod"
BACKUP_DIR="./backups"
LOG_FILE="./logs/deploy.log"

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 日志函数
log() {
    echo -e "${GREEN}[$(date +'%Y-%m-%d %H:%M:%S')]${NC} $1" | tee -a "$LOG_FILE"
}

warn() {
    echo -e "${YELLOW}[$(date +'%Y-%m-%d %H:%M:%S')] WARNING:${NC} $1" | tee -a "$LOG_FILE"
}

error() {
    echo -e "${RED}[$(date +'%Y-%m-%d %H:%M:%S')] ERROR:${NC} $1" | tee -a "$LOG_FILE"
    exit 1
}

# 检查必要条件
check_prerequisites() {
    log "检查部署前置条件..."

    # 检查Docker
    if ! command -v docker &> /dev/null; then
        error "Docker未安装"
    fi

    # 检查Docker Compose
    if ! command -v docker-compose &> /dev/null && ! docker compose version &> /dev/null; then
        error "Docker Compose未安装"
    fi

    # 检查环境文件
    if [ ! -f "$ENV_FILE" ]; then
        error "环境配置文件 $ENV_FILE 不存在"
    fi

    # 检查Compose文件
    if [ ! -f "$COMPOSE_FILE" ]; then
        error "Docker Compose文件 $COMPOSE_FILE 不存在"
    fi

    # 创建必要目录
    mkdir -p "$BACKUP_DIR"
    mkdir -p logs
    mkdir -p uploads

    log "✓ 前置条件检查通过"
}

# 备份数据
backup_data() {
    log "备份现有数据..."

    local backup_name="backup_$(date +%Y%m%d_%H%M%S)"
    local backup_path="$BACKUP_DIR/$backup_name"

    mkdir -p "$backup_path"

    # 备份数据库
    if docker-compose -f "$COMPOSE_FILE" ps postgres | grep -q "Up"; then
        log "备份数据库..."
        docker-compose -f "$COMPOSE_FILE" exec -T postgres pg_dump -U "$DB_USER" "$DB_NAME" | gzip > "$backup_path/database.sql.gz"
        log "✓ 数据库备份完成: $backup_path/database.sql.gz"
    fi

    # 备份配置文件
    log "备份配置文件..."
    tar -czf "$backup_path/config.tar.gz" "$ENV_FILE" docker/ nginx/ 2>/dev/null || true
    log "✓ 配置文件备份完成: $backup_path/config.tar.gz"

    # 备份上传文件
    if [ -d "uploads" ] && [ "$(ls -A uploads)" ]; then
        log "备份上传文件..."
        tar -czf "$backup_path/uploads.tar.gz" uploads/
        log "✓ 上传文件备份完成: $backup_path/uploads.tar.gz"
    fi

    echo "$backup_name" > "$BACKUP_DIR/latest_backup.txt"
    log "✓ 数据备份完成: $backup_path"
}

# 构建镜像
build_images() {
    log "构建应用镜像..."

    # 构建前端镜像
    log "构建前端镜像..."
    docker build -f frontend/Dockerfile.prod -t xiamen-metro-frontend:latest ./frontend || error "前端镜像构建失败"

    # 构建后端镜像
    log "构建后端镜像..."
    docker build -f backend/Dockerfile.prod -t xiamen-metro-backend:latest ./backend || error "后端镜像构建失败"

    log "✓ 镜像构建完成"
}

# 停止现有服务
stop_services() {
    log "停止现有服务..."

    if docker-compose -f "$COMPOSE_FILE" ps | grep -q "Up"; then
        docker-compose -f "$COMPOSE_FILE" down
        log "✓ 现有服务已停止"
    else
        log "没有运行中的服务"
    fi
}

# 启动服务
start_services() {
    log "启动生产服务..."

    # 加载环境变量
    source "$ENV_FILE"

    # 启动数据库和Redis
    log "启动基础设施服务..."
    docker-compose -f "$COMPOSE_FILE" up -d postgres redis

    # 等待数据库启动
    log "等待数据库启动..."
    local max_attempts=30
    local attempt=1

    while [ $attempt -le $max_attempts ]; do
        if docker-compose -f "$COMPOSE_FILE" exec -T postgres pg_isready -U "$DB_USER" > /dev/null 2>&1; then
            log "✓ 数据库启动成功"
            break
        fi

        if [ $attempt -eq $max_attempts ]; then
            error "数据库启动超时"
        fi

        log "等待数据库启动... ($attempt/$max_attempts)"
        sleep 2
        ((attempt++))
    done

    # 运行数据库迁移
    log "执行数据库迁移..."
    docker-compose -f "$COMPOSE_FILE" run --rm backend mvn flyway:migrate || warn "数据库迁移失败，请检查"

    # 启动后端服务
    log "启动后端服务..."
    docker-compose -f "$COMPOSE_FILE" up -d backend

    # 等待后端服务启动
    log "等待后端服务启动..."
    local max_attempts=30
    local attempt=1

    while [ $attempt -le $max_attempts ]; do
        if curl -f http://localhost/api/actuator/health > /dev/null 2>&1; then
            log "✓ 后端服务启动成功"
            break
        fi

        if [ $attempt -eq $max_attempts ]; then
            error "后端服务启动超时"
        fi

        log "等待后端服务启动... ($attempt/$max_attempts)"
        sleep 2
        ((attempt++))
    done

    # 启动前端和Nginx
    log "启动前端和反向代理..."
    docker-compose -f "$COMPOSE_FILE" up -d frontend nginx

    log "✓ 所有服务启动完成"
}

# 健康检查
health_check() {
    log "执行健康检查..."

    local services=("postgres" "redis" "backend" "frontend" "nginx")
    local failed_services=()

    for service in "${services[@]}"; do
        if docker-compose -f "$COMPOSE_FILE" ps "$service" | grep -q "Up"; then
            log "✓ $service 运行正常"
        else
            error "✗ $service 运行异常"
            failed_services+=("$service")
        fi
    done

    # 检查HTTP端点
    if curl -f http://localhost/health > /dev/null 2>&1; then
        log "✓ 系统健康检查通过"
    else
        error "✗ 系统健康检查失败"
    fi

    if [ ${#failed_services[@]} -gt 0 ]; then
        error "以下服务运行异常: ${failed_services[*]}"
    fi
}

# 清理旧镜像
cleanup_images() {
    log "清理旧镜像..."

    # 清理悬空镜像
    docker image prune -f > /dev/null 2>&1

    # 清理旧版本镜像（保留最近3个版本）
    local images=("xiamen-metro-frontend" "xiamen-metro-backend")

    for image in "${images[@]}"; do
        local image_count=$(docker images "$image" | wc -l)
        if [ "$image_count" -gt 4 ]; then
            docker images "$image" | tail -n +4 | awk '{print $3}' | xargs -r docker rmi -f > /dev/null 2>&1 || true
        fi
    done

    log "✓ 镜像清理完成"
}

# 发送部署通知
send_notification() {
    local status="$1"
    local message="厦门地铁设备报文分析系统部署$status - $(date +'%Y-%m-%d %H:%M:%S')"

    # 这里可以集成各种通知方式（邮件、Slack、钉钉等）
    # 示例：发送到Slack
    # curl -X POST -H 'Content-type: application/json' \
    #     --data "{\"text\":\"$message\"}" \
    #     "$SLACK_WEBHOOK_URL" || true

    log "部署通知已发送: $message"
}

# 回滚函数
rollback() {
    warn "开始回滚..."

    local latest_backup
    if [ -f "$BACKUP_DIR/latest_backup.txt" ]; then
        latest_backup=$(cat "$BACKUP_DIR/latest_backup.txt")
    else
        error "没有找到备份信息，无法回滚"
    fi

    local backup_path="$BACKUP_DIR/$latest_backup"

    if [ ! -d "$backup_path" ]; then
        error "备份目录不存在: $backup_path"
    fi

    log "使用备份进行回滚: $backup_path"

    # 停止当前服务
    docker-compose -f "$COMPOSE_FILE" down

    # 恢复数据库（如果存在）
    if [ -f "$backup_path/database.sql.gz" ]; then
        log "恢复数据库..."
        docker-compose -f "$COMPOSE_FILE" up -d postgres
        sleep 10
        gunzip -c "$backup_path/database.sql.gz" | docker-compose -f "$COMPOSE_FILE" exec -T postgres psql -U "$DB_USER" "$DB_NAME"
        log "✓ 数据库恢复完成"
    fi

    # 恢复上传文件（如果存在）
    if [ -f "$backup_path/uploads.tar.gz" ]; then
        log "恢复上传文件..."
        tar -xzf "$backup_path/uploads.tar.gz"
        log "✓ 上传文件恢复完成"
    fi

    # 重新启动服务
    start_services

    log "✓ 回滚完成"
}

# 显示帮助信息
show_help() {
    echo "厦门地铁设备报文分析系统 - 生产环境部署脚本"
    echo ""
    echo "用法: $0 [选项]"
    echo ""
    echo "选项:"
    echo "  -h, --help     显示帮助信息"
    echo "  -r, --rollback 回滚到上一个版本"
    echo "  -b, --backup   仅执行数据备份"
    echo "  -c, --check    仅执行健康检查"
    echo ""
    echo "示例:"
    echo "  $0              # 完整部署"
    echo "  $0 --rollback   # 回滚"
    echo "  $0 --backup     # 备份"
    echo "  $0 --check      # 健康检查"
}

# 主函数
main() {
    local action="deploy"

    # 解析命令行参数
    while [[ $# -gt 0 ]]; do
        case $1 in
            -h|--help)
                show_help
                exit 0
                ;;
            -r|--rollback)
                action="rollback"
                shift
                ;;
            -b|--backup)
                action="backup"
                shift
                ;;
            -c|--check)
                action="check"
                shift
                ;;
            *)
                error "未知选项: $1"
                ;;
        esac
    done

    log "====================================="
    log "厦门地铁设备报文分析系统部署"
    log "操作: $action"
    log "时间: $(date +'%Y-%m-%d %H:%M:%S')"
    log "====================================="

    case $action in
        "deploy")
            check_prerequisites
            backup_data
            build_images
            stop_services
            start_services
            health_check
            cleanup_images
            send_notification "成功"
            log "✓ 部署完成"
            ;;
        "rollback")
            check_prerequisites
            backup_data
            rollback
            health_check
            send_notification "回滚完成"
            log "✓ 回滚完成"
            ;;
        "backup")
            backup_data
            log "✓ 备份完成"
            ;;
        "check")
            health_check
            log "✓ 健康检查完成"
            ;;
    esac

    log "====================================="
}

# 设置错误处理
trap 'error "部署过程中发生错误，请检查日志: $LOG_FILE"' ERR

# 执行主函数
main "$@"