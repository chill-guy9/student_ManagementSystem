#!/bin/bash
# ============================================================
# 健康检查脚本 - 学生管理系统
# 功能：检查后端/MySQL/Redis/磁盘/内存，异常自动重启，Webhook告警
# ============================================================

set -euo pipefail

# ---------- 颜色定义 ----------
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

log_info()    { echo -e "${GREEN}[INFO]${NC}    $(date '+%Y-%m-%d %H:%M:%S') - $*"; }
log_warn()    { echo -e "${YELLOW}[WARN]${NC}    $(date '+%Y-%m-%d %H:%M:%S') - $*"; }
log_error()   { echo -e "${RED}[ERROR]${NC}   $(date '+%Y-%m-%d %H:%M:%S') - $*"; }
log_ok()      { echo -e "${GREEN}[OK]${NC}      $(date '+%Y-%m-%d %H:%M:%S') - $*"; }
log_fail()    { echo -e "${RED}[FAIL]${NC}    $(date '+%Y-%m-%d %H:%M:%S') - $*"; }

# ---------- 默认配置 ----------
API_URL="http://localhost:8080/api/system/health"
WEBHOOK_URL=""
AUTO_RESTART=0
MYSQL_HOST="localhost"
MYSQL_PORT="3306"
MYSQL_USER="root"
MYSQL_PASS=""
REDIS_HOST="localhost"
REDIS_PORT="6380"
REDIS_PASS=""
DISK_THRESHOLD=90
MEMORY_THRESHOLD=90
JAR_NAME="student-management.jar"
DEPLOY_DIR="/opt/student-management"
RESTART_COOLDOWN=300  # 重启冷却时间（秒）
LAST_RESTART_FILE="/tmp/health-check-last-restart"

# ---------- 参数解析 ----------
usage() {
    echo "Usage: $0 [OPTIONS]"
    echo "Options:"
    echo "  --api-url URL          后端健康检查 URL (默认: http://localhost:8080/api/system/health)"
    echo "  --webhook-url URL      Webhook 告警 URL"
    echo "  --auto-restart         异常时自动重启服务"
    echo "  --mysql-host HOST      MySQL 主机 (默认: localhost)"
    echo "  --mysql-port PORT      MySQL 端口 (默认: 3306)"
    echo "  --mysql-user USER      MySQL 用户 (默认: root)"
    echo "  --mysql-pass PASS      MySQL 密码"
    echo "  --redis-host HOST      Redis 主机 (默认: localhost)"
    echo "  --redis-port PORT      Redis 端口 (默认: 6379)"
    echo "  --redis-pass PASS      Redis 密码"
    echo "  --disk-threshold PCT   磁盘告警阈值 (默认: 90%)"
    echo "  --memory-threshold PCT 内存告警阈值 (默认: 90%)"
    echo "  -h, --help             显示帮助"
    exit 1
}

while [[ $# -gt 0 ]]; do
    case "$1" in
        --api-url)           API_URL="$2"; shift 2 ;;
        --webhook-url)       WEBHOOK_URL="$2"; shift 2 ;;
        --auto-restart)      AUTO_RESTART=1; shift ;;
        --mysql-host)        MYSQL_HOST="$2"; shift 2 ;;
        --mysql-port)        MYSQL_PORT="$2"; shift 2 ;;
        --mysql-user)        MYSQL_USER="$2"; shift 2 ;;
        --mysql-pass)        MYSQL_PASS="$2"; shift 2 ;;
        --redis-host)        REDIS_HOST="$2"; shift 2 ;;
        --redis-port)        REDIS_PORT="$2"; shift 2 ;;
        --redis-pass)        REDIS_PASS="$2"; shift 2 ;;
        --disk-threshold)    DISK_THRESHOLD="$2"; shift 2 ;;
        --memory-threshold)  MEMORY_THRESHOLD="$2"; shift 2 ;;
        -h|--help)           usage ;;
        *)                   log_error "未知参数: $1"; usage ;;
    esac
done

# ---------- 全局状态 ----------
TOTAL_CHECKS=0
FAILED_CHECKS=0
ALERT_MESSAGES=()

# ---------- Webhook 告警 ----------
send_webhook() {
    local level="$1"
    local message="$2"

    if [[ -z "${WEBHOOK_URL}" ]]; then
        return 0
    fi

    local payload
    payload=$(cat <<EOF
{
    "level": "${level}",
    "service": "student-management",
    "message": "${message}",
    "timestamp": "$(date -u '+%Y-%m-%dT%H:%M:%SZ')",
    "checks_total": ${TOTAL_CHECKS},
    "checks_failed": ${FAILED_CHECKS}
}
EOF
)

    curl -s -X POST \
        -H "Content-Type: application/json" \
        -d "${payload}" \
        "${WEBHOOK_URL}" >/dev/null 2>&1 || true
}

# ---------- 检查1: 后端服务健康 ----------
check_backend() {
    TOTAL_CHECKS=$((TOTAL_CHECKS + 1))
    echo -e "\n${BLUE}--- 检查后端服务 ---${NC}"

    HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" --connect-timeout 5 --max-time 10 "${API_URL}" 2>/dev/null || echo "000")

    if [[ "${HTTP_CODE}" == "200" ]]; then
        log_ok "后端服务正常 (HTTP ${HTTP_CODE})"
    else
        FAILED_CHECKS=$((FAILED_CHECKS + 1))
        log_fail "后端服务异常 (HTTP ${HTTP_CODE})"
        ALERT_MESSAGES+=("后端服务异常 HTTP:${HTTP_CODE}")
    fi
}

# ---------- 检查2: MySQL 连接 ----------
check_mysql() {
    TOTAL_CHECKS=$((TOTAL_CHECKS + 1))
    echo -e "\n${BLUE}--- 检查 MySQL 连接 ---${NC}"

    MYSQL_OPTS=(-h "${MYSQL_HOST}" -P "${MYSQL_PORT}" -u "${MYSQL_USER}" --connect-timeout=5)
    if [[ -n "${MYSQL_PASS}" ]]; then
        MYSQL_OPTS+=(-p"${MYSQL_PASS}")
    fi

    if mysql "${MYSQL_OPTS[@]}" -e "SELECT 1;" &>/dev/null; then
        log_ok "MySQL 连接正常 (${MYSQL_HOST}:${MYSQL_PORT})"
    else
        FAILED_CHECKS=$((FAILED_CHECKS + 1))
        log_fail "MySQL 连接失败 (${MYSQL_HOST}:${MYSQL_PORT})"
        ALERT_MESSAGES+=("MySQL 连接失败 ${MYSQL_HOST}:${MYSQL_PORT}")
    fi
}

# ---------- 检查3: Redis 连接 ----------
check_redis() {
    TOTAL_CHECKS=$((TOTAL_CHECKS + 1))
    echo -e "\n${BLUE}--- 检查 Redis 连接 ---${NC}"

    REDIS_CMD="redis-cli -h ${REDIS_HOST} -p ${REDIS_PORT}"
    if [[ -n "${REDIS_PASS}" ]]; then
        REDIS_CMD="${REDIS_CMD} -a ${REDIS_PASS}"
    fi

    if ${REDIS_CMD} ping 2>/dev/null | grep -q "PONG"; then
        log_ok "Redis 连接正常 (${REDIS_HOST}:${REDIS_PORT})"
    else
        FAILED_CHECKS=$((FAILED_CHECKS + 1))
        log_fail "Redis 连接失败 (${REDIS_HOST}:${REDIS_PORT})"
        ALERT_MESSAGES+=("Redis 连接失败 ${REDIS_HOST}:${REDIS_PORT}")
    fi
}

# ---------- 检查4: 磁盘空间 ----------
check_disk() {
    TOTAL_CHECKS=$((TOTAL_CHECKS + 1))
    echo -e "\n${BLUE}--- 检查磁盘空间 ---${NC}"

    # 获取根分区使用率
    DISK_USAGE=$(df -h / | awk 'NR==2 {print $5}' | tr -d '%')
    DISK_HUMAN=$(df -h / | awk 'NR==2 {print $3 "/" $2 " (" $5 ")"}')

    if [[ ${DISK_USAGE} -ge ${DISK_THRESHOLD} ]]; then
        FAILED_CHECKS=$((FAILED_CHECKS + 1))
        log_fail "磁盘空间不足: ${DISK_HUMAN} (阈值: ${DISK_THRESHOLD}%)"
        ALERT_MESSAGES+=("磁盘空间不足 ${DISK_HUMAN}")
    else
        log_ok "磁盘空间正常: ${DISK_HUMAN} (阈值: ${DISK_THRESHOLD}%)"
    fi
}

# ---------- 检查5: 内存使用 ----------
check_memory() {
    TOTAL_CHECKS=$((TOTAL_CHECKS + 1))
    echo -e "\n${BLUE}--- 检查内存使用 ---${NC}"

    if [[ -f /proc/meminfo ]]; then
        MEM_TOTAL=$(awk '/MemTotal/ {print $2}' /proc/meminfo)
        MEM_AVAILABLE=$(awk '/MemAvailable/ {print $2}' /proc/meminfo)
        MEM_USED=$((MEM_TOTAL - MEM_AVAILABLE))
        MEM_USAGE=$((MEM_USED * 100 / MEM_TOTAL))
        MEM_USED_HUMAN=$((MEM_USED / 1024))
        MEM_TOTAL_HUMAN=$((MEM_TOTAL / 1024))
    else
        # macOS / 其他系统
        MEM_USAGE=$(vm_stat 2>/dev/null | awk '/Pages free/ {free=$3} /Pages active/ {active=$3} END {if (free+active>0) print active*100/(free+active); else print 0}' | cut -d. -f1 || echo "0")
        MEM_TOTAL_HUMAN=$(sysctl -n hw.memsize 2>/dev/null | awk '{print $1/1024/1024}' || echo "?")
        MEM_USED_HUMAN="?"
    fi

    if [[ ${MEM_USAGE} -ge ${MEMORY_THRESHOLD} ]]; then
        FAILED_CHECKS=$((FAILED_CHECKS + 1))
        log_fail "内存使用过高: ${MEM_USED_HUMAN}MB/${MEM_TOTAL_HUMAN}MB (${MEM_USAGE}%, 阈值: ${MEMORY_THRESHOLD}%)"
        ALERT_MESSAGES+=("内存使用过高 ${MEM_USAGE}%")
    else
        log_ok "内存使用正常: ${MEM_USED_HUMAN}MB/${MEM_TOTAL_HUMAN}MB (${MEM_USAGE}%, 阈值: ${MEMORY_THRESHOLD}%)"
    fi
}

# ---------- 自动重启 ----------
auto_restart_service() {
    if [[ ${AUTO_RESTART} -eq 0 ]]; then
        return 0
    fi

    # 检查冷却时间
    if [[ -f "${LAST_RESTART_FILE}" ]]; then
        LAST_RESTART=$(cat "${LAST_RESTART_FILE}" 2>/dev/null)
        NOW=$(date +%s)
        ELAPSED=$((NOW - LAST_RESTART))
        if [[ ${ELAPSED} -lt ${RESTART_COOLDOWN} ]]; then
            log_warn "重启冷却中，距上次重启 ${ELAPSED}s，需等待 ${RESTART_COOLDOWN}s"
            return 0
        fi
    fi

    log_warn "尝试自动重启后端服务..."

    # 停止服务
    if pgrep -f "${JAR_NAME}" >/dev/null 2>&1; then
        pkill -15 -f "${JAR_NAME}"
        for i in $(seq 1 30); do
            if ! pgrep -f "${JAR_NAME}" >/dev/null 2>&1; then
                break
            fi
            sleep 1
        done
        if pgrep -f "${JAR_NAME}" >/dev/null 2>&1; then
            pkill -9 -f "${JAR_NAME}"
            sleep 2
        fi
    fi

    # 启动服务
    if [[ -f "${DEPLOY_DIR}/${JAR_NAME}" ]]; then
        nohup java -jar \
            -Xms256m -Xmx512m \
            -Dspring.profiles.active=prod \
            "${DEPLOY_DIR}/${JAR_NAME}" \
            > "${DEPLOY_DIR}/logs/app.log" 2>&1 &

        date +%s > "${LAST_RESTART_FILE}"
        log_info "后端服务已重启 (PID: $!)"
    else
        log_error "未找到 JAR 文件: ${DEPLOY_DIR}/${JAR_NAME}"
    fi
}

# ---------- 执行所有检查 ----------
echo ""
echo -e "${BLUE}╔══════════════════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║           学生管理系统 - 健康检查                        ║${NC}"
echo -e "${BLUE}║           $(date '+%Y-%m-%d %H:%M:%S')                           ║${NC}"
echo -e "${BLUE}╚══════════════════════════════════════════════════════════╝${NC}"

check_backend
check_mysql
check_redis
check_disk
check_memory

# ---------- 汇总结果 ----------
echo ""
echo -e "${BLUE}========== 检查汇总 ==========${NC}"
echo -e "  总检查项: ${TOTAL_CHECKS}"
echo -e "  通过:     $((TOTAL_CHECKS - FAILED_CHECKS))"
echo -e "  失败:     ${FAILED_CHECKS}"

if [[ ${FAILED_CHECKS} -gt 0 ]]; then
    echo ""
    echo -e "${RED}告警列表:${NC}"
    for msg in "${ALERT_MESSAGES[@]}"; do
        echo -e "  ${RED}- ${msg}${NC}"
    done

    # 发送告警
    ALERT_MSG=$(IFS=', '; echo "${ALERT_MESSAGES[*]}")
    send_webhook "CRITICAL" "健康检查失败: ${ALERT_MSG}"

    # 自动重启
    auto_restart_service

    exit 1
else
    echo ""
    log_ok "所有检查项通过"
    send_webhook "OK" "所有健康检查通过"
    exit 0
fi
