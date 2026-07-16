#!/bin/bash
# ============================================================
# 停止脚本 - 学生管理系统
# 功能：优雅停止后端、停止Nginx、停止Docker容器、确认进程停止
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
DEPLOY_DIR="/opt/student-management"
JAR_NAME="student-management.jar"
MYSQL_CONTAINER="student-mysql"
REDIS_CONTAINER="student-redis"
STOP_DOCKER=0
GRACEFUL_TIMEOUT=30

# ---------- 参数解析 ----------
usage() {
    echo "Usage: $0 [OPTIONS]"
    echo "Options:"
    echo "  --deploy-dir DIR    部署目录 (默认: /opt/student-management)"
    echo "  --stop-docker       同时停止 Docker 容器"
    echo "  --timeout SECS      优雅停止超时秒数 (默认: 30)"
    echo "  -h, --help          显示帮助"
    exit 1
}

while [[ $# -gt 0 ]]; do
    case "$1" in
        --deploy-dir)  DEPLOY_DIR="$2"; shift 2 ;;
        --stop-docker) STOP_DOCKER=1; shift ;;
        --timeout)     GRACEFUL_TIMEOUT="$2"; shift 2 ;;
        -h|--help)     usage ;;
        *)             log_error "未知参数: $1"; usage ;;
    esac
done

# ---------- 横幅 ----------
echo ""
echo -e "${CYAN}╔══════════════════════════════════════════════════════════╗${NC}"
echo -e "${CYAN}║           学生管理系统 - 停止服务                        ║${NC}"
echo -e "${CYAN}╚══════════════════════════════════════════════════════════╝${NC}"
echo ""

# ---------- 步骤1: 优雅停止后端服务 ----------
log_step "========== 停止后端服务 =========="

BACKEND_PID=""
if [[ -f "${DEPLOY_DIR}/backend.pid" ]]; then
    BACKEND_PID=$(cat "${DEPLOY_DIR}/backend.pid" 2>/dev/null)
fi

if [[ -z "${BACKEND_PID}" ]]; then
    BACKEND_PID=$(pgrep -f "${JAR_NAME}" 2>/dev/null || true)
fi

if [[ -z "${BACKEND_PID}" ]]; then
    log_info "后端服务未在运行"
else
    log_info "发送 SIGTERM 信号到后端进程 (PID: ${BACKEND_PID})..."
    kill -15 "${BACKEND_PID}" 2>/dev/null || true

    # 等待优雅停止
    log_info "等待后端优雅停止（超时 ${GRACEFUL_TIMEOUT}s）..."
    ELAPSED=0
    while [[ ${ELAPSED} -lt ${GRACEFUL_TIMEOUT} ]]; do
        if ! kill -0 "${BACKEND_PID}" 2>/dev/null; then
            log_ok "后端服务已优雅停止 (耗时 ${ELAPSED}s)"
            break
        fi
        sleep 1
        ELAPSED=$((ELAPSED + 1))
    done

    # 超时后强制终止
    if kill -0 "${BACKEND_PID}" 2>/dev/null; then
        log_warn "优雅停止超时 (${GRACEFUL_TIMEOUT}s)，发送 SIGKILL 强制终止..."
        kill -9 "${BACKEND_PID}" 2>/dev/null || true
        sleep 2

        if kill -0 "${BACKEND_PID}" 2>/dev/null; then
            log_fail "无法终止后端进程 (PID: ${BACKEND_PID})"
        else
            log_ok "后端服务已强制终止"
        fi
    fi

    # 清理 PID 文件
    rm -f "${DEPLOY_DIR}/backend.pid"
fi

# ---------- 步骤2: 停止 Nginx ----------
log_step "========== 停止 Nginx =========="

if pgrep -x nginx >/dev/null 2>&1; then
    log_info "停止 Nginx..."
    nginx -s stop 2>/dev/null || sudo nginx -s stop 2>/dev/null || true
    sleep 2

    if pgrep -x nginx >/dev/null 2>&1; then
        log_warn "Nginx 未停止，尝试 systemctl..."
        sudo systemctl stop nginx 2>/dev/null || true
    fi

    if pgrep -x nginx >/dev/null 2>&1; then
        log_fail "Nginx 仍在运行"
    else
        log_ok "Nginx 已停止"
    fi
else
    log_info "Nginx 未在运行"
fi

# ---------- 步骤3: 停止 Docker 容器 ----------
if [[ ${STOP_DOCKER} -eq 1 ]]; then
    log_step "========== 停止 Docker 容器 =========="

    # 停止 MySQL 容器
    if docker ps --format '{{.Names}}' 2>/dev/null | grep -q "^${MYSQL_CONTAINER}$"; then
        log_info "停止 MySQL 容器..."
        docker stop "${MYSQL_CONTAINER}"
        log_ok "MySQL 容器已停止"
    else
        log_info "MySQL 容器未在运行"
    fi

    # 停止 Redis 容器
    if docker ps --format '{{.Names}}' 2>/dev/null | grep -q "^${REDIS_CONTAINER}$"; then
        log_info "停止 Redis 容器..."
        docker stop "${REDIS_CONTAINER}"
        log_ok "Redis 容器已停止"
    else
        log_info "Redis 容器未在运行"
    fi
fi

# ---------- 步骤4: 确认所有进程已停止 ----------
log_step "========== 确认进程状态 =========="

ALL_STOPPED=1

# 检查后端进程
if pgrep -f "${JAR_NAME}" >/dev/null 2>&1; then
    log_fail "后端进程仍在运行 (PID: $(pgrep -f "${JAR_NAME}"))"
    ALL_STOPPED=0
else
    log_ok "后端进程已停止"
fi

# 检查 Nginx 进程
if pgrep -x nginx >/dev/null 2>&1; then
    log_fail "Nginx 进程仍在运行"
    ALL_STOPPED=0
else
    log_ok "Nginx 进程已停止"
fi

# 检查 Docker 容器（如果启用）
if [[ ${STOP_DOCKER} -eq 1 ]]; then
    if docker ps --format '{{.Names}}' 2>/dev/null | grep -qE "^(${MYSQL_CONTAINER}|${REDIS_CONTAINER})$"; then
        log_fail "部分 Docker 容器仍在运行"
        ALL_STOPPED=0
    else
        log_ok "所有 Docker 容器已停止"
    fi
fi

# ---------- 完成 ----------
echo ""
if [[ ${ALL_STOPPED} -eq 1 ]]; then
    echo -e "${GREEN}╔══════════════════════════════════════════════════════════╗${NC}"
    echo -e "${GREEN}║                  所有服务已停止                          ║${NC}"
    echo -e "${GREEN}╚══════════════════════════════════════════════════════════╝${NC}"
else
    echo -e "${RED}╔══════════════════════════════════════════════════════════╗${NC}"
    echo -e "${RED}║              部分服务未能正常停止                        ║${NC}"
    echo -e "${RED}╚══════════════════════════════════════════════════════════╝${NC}"
    exit 1
fi
