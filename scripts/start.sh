#!/bin/bash
# ============================================================
# 启动脚本 - 学生管理系统
# 功能：检查环境、启动MySQL/Redis/后端/Nginx、健康检查
# ============================================================

set -euo pipefail

# ---------- 颜色定义 ----------
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m'

log_info()    { echo -e "${GREEN}[INFO]${NC}    $(date '+%Y-%m-%d %H:%M:%S') - $*"; }
log_warn()    { echo -e "${YELLOW}[WARN]${NC}    $(date '+%Y-%m-%d %H:%M:%S') - $*"; }
log_error()   { echo -e "${RED}[ERROR]${NC}   $(date '+%Y-%m-%d %H:%M:%S') - $*"; }
log_step()    { echo -e "${CYAN}[STEP]${NC}    $(date '+%Y-%m-%d %H:%M:%S') - $*"; }
log_ok()      { echo -e "${GREEN}[  OK  ]${NC}  $(date '+%Y-%m-%d %H:%M:%S') - $*"; }
log_fail()    { echo -e "${RED}[ FAIL ]${NC}  $(date '+%Y-%m-%d %H:%M:%S') - $*"; }

# ---------- 默认配置 ----------
PROJECT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
DEPLOY_DIR="/opt/student-management"
JAR_NAME="student-management.jar"
HEALTH_URL="http://localhost:8080/api/system/health"
USE_DOCKER_MYSQL=0
USE_DOCKER_REDIS=0
MYSQL_CONTAINER="student-mysql"
REDIS_CONTAINER="student-redis"
SPRING_PROFILE="prod"
JVM_XMS="256m"
JVM_XMX="512m"

# ---------- 参数解析 ----------
usage() {
    echo "Usage: $0 [OPTIONS]"
    echo "Options:"
    echo "  --deploy-dir DIR       部署目录 (默认: /opt/student-management)"
    echo "  --docker-mysql         使用 Docker 启动 MySQL"
    echo "  --docker-redis         使用 Docker 启动 Redis"
    echo "  --spring-profile PROF  Spring 配置 (默认: prod)"
    echo "  --jvm-xms SIZE         JVM 初始内存 (默认: 256m)"
    echo "  --jvm-xmx SIZE         JVM 最大内存 (默认: 512m)"
    echo "  -h, --help             显示帮助"
    exit 1
}

while [[ $# -gt 0 ]]; do
    case "$1" in
        --deploy-dir)      DEPLOY_DIR="$2"; shift 2 ;;
        --docker-mysql)    USE_DOCKER_MYSQL=1; shift ;;
        --docker-redis)    USE_DOCKER_REDIS=1; shift ;;
        --spring-profile)  SPRING_PROFILE="$2"; shift 2 ;;
        --jvm-xms)         JVM_XMS="$2"; shift 2 ;;
        --jvm-xmx)         JVM_XMX="$2"; shift 2 ;;
        -h|--help)         usage ;;
        *)                 log_error "未知参数: $1"; usage ;;
    esac
done

# ---------- 横幅 ----------
echo ""
echo -e "${CYAN}╔══════════════════════════════════════════════════════════╗${NC}"
echo -e "${CYAN}║           学生管理系统 - 启动服务                        ║${NC}"
echo -e "${CYAN}╚══════════════════════════════════════════════════════════╝${NC}"
echo ""

# ---------- 步骤1: 检查环境 ----------
log_step "========== 检查运行环境 =========="

# 检查 Java
if java -version &>/dev/null; then
    JAVA_VERSION=$(java -version 2>&1 | head -1)
    log_ok "Java: ${JAVA_VERSION}"
else
    log_fail "未找到 Java 运行环境"
    log_error "请安装 JDK 21 或更高版本"
    exit 1
fi

# 检查 MySQL 客户端
if command -v mysql &>/dev/null; then
    log_ok "MySQL 客户端: 已安装"
else
    log_warn "MySQL 客户端: 未安装（非必须）"
fi

# 检查 Redis 客户端
if command -v redis-cli &>/dev/null; then
    log_ok "Redis 客户端: 已安装"
else
    log_warn "Redis 客户端: 未安装（非必须）"
fi

# 检查 Docker（如果需要）
if [[ ${USE_DOCKER_MYSQL} -eq 1 || ${USE_DOCKER_REDIS} -eq 1 ]]; then
    if command -v docker &>/dev/null; then
        log_ok "Docker: 已安装"
    else
        log_fail "Docker: 未安装（--docker-mysql/redis 需要 Docker）"
        exit 1
    fi
fi

# ---------- 步骤2: 启动 MySQL ----------
log_step "========== 启动 MySQL =========="

if [[ ${USE_DOCKER_MYSQL} -eq 1 ]]; then
    if docker ps -a --format '{{.Names}}' | grep -q "^${MYSQL_CONTAINER}$"; then
        if docker ps --format '{{.Names}}' | grep -q "^${MYSQL_CONTAINER}$"; then
            log_ok "MySQL 容器已在运行"
        else
            log_info "启动已存在的 MySQL 容器..."
            docker start "${MYSQL_CONTAINER}"
            log_ok "MySQL 容器已启动"
        fi
    else
        log_info "创建并启动 MySQL 容器..."
        docker run -d \
            --name "${MYSQL_CONTAINER}" \
            -e MYSQL_ROOT_PASSWORD=root123 \
            -e MYSQL_DATABASE=student_management \
            -p 3306:3306 \
            -v student-mysql-data:/var/lib/mysql \
            mysql:8.0 \
            --character-set-server=utf8mb4 \
            --collation-server=utf8mb4_unicode_ci
        log_ok "MySQL 容器已创建并启动"
    fi

    # 等待 MySQL 就绪
    log_info "等待 MySQL 就绪..."
    for i in $(seq 1 30); do
        if docker exec "${MYSQL_CONTAINER}" mysqladmin ping -h localhost --silent 2>/dev/null; then
            break
        fi
        sleep 2
    done
    log_ok "MySQL 已就绪"
else
    # 检查本地 MySQL
    if systemctl is-active --quiet mysql 2>/dev/null || systemctl is-active --quiet mysqld 2>/dev/null; then
        log_ok "MySQL 服务已在运行"
    elif command -v mysql &>/dev/null && mysql -e "SELECT 1;" &>/dev/null; then
        log_ok "MySQL 连接正常"
    else
        log_warn "MySQL 服务未检测到，请确保 MySQL 已启动"
    fi
fi

# ---------- 步骤3: 启动 Redis ----------
log_step "========== 启动 Redis =========="

if [[ ${USE_DOCKER_REDIS} -eq 1 ]]; then
    if docker ps -a --format '{{.Names}}' | grep -q "^${REDIS_CONTAINER}$"; then
        if docker ps --format '{{.Names}}' | grep -q "^${REDIS_CONTAINER}$"; then
            log_ok "Redis 容器已在运行"
        else
            log_info "启动已存在的 Redis 容器..."
            docker start "${REDIS_CONTAINER}"
            log_ok "Redis 容器已启动"
        fi
    else
        log_info "创建并启动 Redis 容器..."
        docker run -d \
            --name "${REDIS_CONTAINER}" \
            -p 6380:6379 \
            -v student-redis-data:/data \
            redis:7-alpine \
            redis-server --appendonly yes
        log_ok "Redis 容器已创建并启动"
    fi

    # 等待 Redis 就绪
    log_info "等待 Redis 就绪..."
    for i in $(seq 1 15); do
        if docker exec "${REDIS_CONTAINER}" redis-cli ping 2>/dev/null | grep -q "PONG"; then
            break
        fi
        sleep 1
    done
    log_ok "Redis 已就绪"
else
    # 检查本地 Redis
    if systemctl is-active --quiet redis 2>/dev/null || systemctl is-active --quiet redis-server 2>/dev/null; then
        log_ok "Redis 服务已在运行"
    elif command -v redis-cli &>/dev/null && redis-cli ping 2>/dev/null | grep -q "PONG"; then
        log_ok "Redis 连接正常"
    else
        log_warn "Redis 服务未检测到，请确保 Redis 已启动"
    fi
fi

# ---------- 步骤4: 启动后端 JAR ----------
log_step "========== 启动后端服务 =========="

# 检查是否已在运行
if pgrep -f "${JAR_NAME}" >/dev/null 2>&1; then
    log_warn "后端服务已在运行 (PID: $(pgrep -f "${JAR_NAME}"))"
    log_info "如需重启，请先运行 stop.sh"
else
    mkdir -p "${DEPLOY_DIR}/logs"

    if [[ ! -f "${DEPLOY_DIR}/${JAR_NAME}" ]]; then
        log_error "未找到 JAR 文件: ${DEPLOY_DIR}/${JAR_NAME}"
        log_error "请先运行部署脚本 deploy.sh"
        exit 1
    fi

    log_info "启动后端服务..."
    nohup java -jar \
        -Xms"${JVM_XMS}" \
        -Xmx"${JVM_XMX}" \
        -Dspring.profiles.active="${SPRING_PROFILE}" \
        "${DEPLOY_DIR}/${JAR_NAME}" \
        > "${DEPLOY_DIR}/logs/app.log" 2>&1 &

    BACKEND_PID=$!
    echo "${BACKEND_PID}" > "${DEPLOY_DIR}/backend.pid"
    log_info "后端服务已启动 (PID: ${BACKEND_PID})"
fi

# ---------- 步骤5: 等待健康检查通过 ----------
log_step "========== 等待健康检查 =========="
log_info "等待后端服务启动（最多120秒）..."

HEALTH_PASSED=0
for i in $(seq 1 24); do
    sleep 5
    HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" --connect-timeout 3 "${HEALTH_URL}" 2>/dev/null || echo "000")
    if [[ "${HTTP_CODE}" == "200" ]]; then
        HEALTH_PASSED=1
        log_ok "后端服务健康检查通过 (第 $((i * 5))s)"
        break
    fi
    log_info "等待中... (第 $((i * 5))s, HTTP: ${HTTP_CODE})"
done

if [[ ${HEALTH_PASSED} -eq 0 ]]; then
    log_fail "后端服务健康检查超时"
    log_error "请检查日志: ${DEPLOY_DIR}/logs/app.log"
    log_error "最近日志："
    tail -n 30 "${DEPLOY_DIR}/logs/app.log" 2>/dev/null || true
    exit 1
fi

# ---------- 步骤6: 启动 Nginx ----------
log_step "========== 启动 Nginx =========="

if command -v nginx &>/dev/null; then
    if nginx -t 2>/dev/null; then
        if pgrep -x nginx >/dev/null 2>&1; then
            log_ok "Nginx 已在运行"
        else
            nginx 2>/dev/null || sudo nginx 2>/dev/null
            log_ok "Nginx 已启动"
        fi
    else
        log_warn "Nginx 配置检查失败，跳过启动"
    fi
elif systemctl is-active --quiet nginx 2>/dev/null; then
    log_ok "Nginx 服务已在运行 (systemd)"
else
    log_warn "Nginx 未安装或未运行，前端可能无法访问"
fi

# ---------- 完成 ----------
echo ""
echo -e "${GREEN}╔══════════════════════════════════════════════════════════╗${NC}"
echo -e "${GREEN}║                  所有服务启动成功！                      ║${NC}"
echo -e "${GREEN}╠══════════════════════════════════════════════════════════╣${NC}"
echo -e "${GREEN}║  后端服务:  ${HEALTH_URL}$(printf '%*s' 20 '')║${NC}"
echo -e "${GREEN}║  前端地址:  http://localhost$(printf '%*s' 32 '')║${NC}"
echo -e "${GREEN}║  后端 PID:  $(cat "${DEPLOY_DIR}/backend.pid" 2>/dev/null || echo 'N/A')$(printf '%*s' 38 '')║${NC}"
echo -e "${GREEN}║  日志目录:  ${DEPLOY_DIR}/logs$(printf '%*s' 22 '')║${NC}"
echo -e "${GREEN}╚══════════════════════════════════════════════════════════╝${NC}"
