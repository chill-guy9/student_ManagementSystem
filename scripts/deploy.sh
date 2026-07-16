#!/bin/bash
# ============================================================
# 全量部署脚本 - 学生管理系统
# 功能：拉取代码、构建后端/前端、备份、部署、健康检查、回滚
# ============================================================

set -euo pipefail

# ---------- 颜色定义 ----------
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# ---------- 默认配置 ----------
PROJECT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
BACKEND_DIR="${PROJECT_DIR}/backend"
FRONTEND_DIR="${PROJECT_DIR}/frontend"
DEPLOY_DIR="/opt/student-management"
BACKUP_DIR="${DEPLOY_DIR}/backups"
NGINX_DIR="/usr/share/nginx/html"
JAR_NAME="student-management.jar"
HEALTH_URL="http://localhost:8080/api/system/health"
HEALTH_TIMEOUT=10
ROLLBACK_FLAG=0

# ---------- 工具函数 ----------
log_info()    { echo -e "${GREEN}[INFO]${NC}    $(date '+%Y-%m-%d %H:%M:%S') - $*"; }
log_warn()    { echo -e "${YELLOW}[WARN]${NC}    $(date '+%Y-%m-%d %H:%M:%S') - $*"; }
log_error()   { echo -e "${RED}[ERROR]${NC}   $(date '+%Y-%m-%d %H:%M:%S') - $*"; }
log_step()    { echo -e "${CYAN}[STEP]${NC}    $(date '+%Y-%m-%d %H:%M:%S') - $*"; }
log_success() { echo -e "${GREEN}[SUCCESS]${NC} $(date '+%Y-%m-%d %H:%M:%S') - $*"; }

# ---------- 参数解析 ----------
usage() {
    echo "Usage: $0 [OPTIONS]"
    echo "Options:"
    echo "  --project-dir DIR    项目目录 (默认: ${PROJECT_DIR})"
    echo "  --deploy-dir DIR     部署目录 (默认: ${DEPLOY_DIR})"
    echo "  --nginx-dir DIR      Nginx 静态文件目录 (默认: ${NGINX_DIR})"
    echo "  --health-url URL     健康检查 URL (默认: ${HEALTH_URL})"
    echo "  --skip-pull          跳过代码拉取"
    echo "  --skip-backup        跳过备份"
    echo "  --skip-frontend      跳过前端构建"
    echo "  --skip-backend       跳过后端构建"
    echo "  -h, --help           显示帮助"
    exit 1
}

SKIP_PULL=0
SKIP_BACKUP=0
SKIP_FRONTEND=0
SKIP_BACKEND=0

while [[ $# -gt 0 ]]; do
    case "$1" in
        --project-dir)  PROJECT_DIR="$2"; shift 2 ;;
        --deploy-dir)   DEPLOY_DIR="$2"; shift 2 ;;
        --nginx-dir)    NGINX_DIR="$2"; shift 2 ;;
        --health-url)   HEALTH_URL="$2"; shift 2 ;;
        --skip-pull)    SKIP_PULL=1; shift ;;
        --skip-backup)  SKIP_BACKUP=1; shift ;;
        --skip-frontend) SKIP_FRONTEND=1; shift ;;
        --skip-backend)  SKIP_BACKEND=1; shift ;;
        -h|--help)      usage ;;
        *)              log_error "未知参数: $1"; usage ;;
    esac
done

BACKEND_DIR="${PROJECT_DIR}/backend"
FRONTEND_DIR="${PROJECT_DIR}/frontend"
BACKUP_DIR="${DEPLOY_DIR}/backups"

# ---------- 回滚函数 ----------
rollback() {
    log_error "部署失败，开始回滚..."
    ROLLBACK_FLAG=1

    local latest_backup="${BACKUP_DIR}/latest"
    if [[ ! -d "${latest_backup}" ]]; then
        log_error "未找到备份目录，无法回滚！"
        exit 1
    fi

    log_step "恢复后端 JAR..."
    if [[ -f "${latest_backup}/${JAR_NAME}" ]]; then
        cp "${latest_backup}/${JAR_NAME}" "${DEPLOY_DIR}/${JAR_NAME}"
        log_info "后端 JAR 已恢复"
    fi

    log_step "恢复前端文件..."
    if [[ -d "${latest_backup}/frontend" ]]; then
        rm -rf "${NGINX_DIR:?}"/*
        cp -r "${latest_backup}/frontend/"* "${NGINX_DIR}/"
        log_info "前端文件已恢复"
    fi

    log_step "重启后端服务..."
    if systemctl is-active --quiet student-management 2>/dev/null; then
        sudo systemctl restart student-management
    elif pgrep -f "${JAR_NAME}" >/dev/null 2>&1; then
        pkill -f "${JAR_NAME}"
        sleep 3
        nohup java -jar "${DEPLOY_DIR}/${JAR_NAME}" > "${DEPLOY_DIR}/logs/app.log" 2>&1 &
    fi

    log_warn "回滚完成，请验证服务状态"
    exit 1
}

trap rollback ERR

# ---------- 步骤1: 拉取最新代码 ----------
if [[ ${SKIP_PULL} -eq 0 ]]; then
    log_step "========== 拉取最新代码 =========="
    cd "${PROJECT_DIR}"
    if [[ -d ".git" ]]; then
        git fetch origin
        git reset --hard origin/main
        log_success "代码已更新到最新版本"
    else
        log_warn "非 Git 仓库，跳过代码拉取"
    fi
else
    log_info "跳过代码拉取"
fi

# ---------- 步骤2: 构建后端 ----------
if [[ ${SKIP_BACKEND} -eq 0 ]]; then
    log_step "========== 构建后端 =========="
    cd "${BACKEND_DIR}"
    if [[ -f "mvnw" ]]; then
        ./mvnw clean package -DskipTests
    elif command -v mvn &>/dev/null; then
        mvn clean package -DskipTests
    else
        log_error "未找到 Maven，请安装 Maven 或使用 mvnw"
        exit 1
    fi

    JAR_PATH=$(find "${BACKEND_DIR}/target" -name "*.jar" ! -name "*-sources.jar" ! -name "*-javadoc.jar" | head -1)
    if [[ -z "${JAR_PATH}" ]]; then
        log_error "未找到构建产物 JAR 文件"
        exit 1
    fi
    log_success "后端构建完成: ${JAR_PATH}"
else
    log_info "跳过后端构建"
fi

# ---------- 步骤3: 构建前端 ----------
if [[ ${SKIP_FRONTEND} -eq 0 ]]; then
    log_step "========== 构建前端 =========="
    cd "${FRONTEND_DIR}"

    if [[ -f "package-lock.json" ]]; then
        npm ci
    else
        npm install
    fi

    npm run build

    if [[ ! -d "${FRONTEND_DIR}/dist" ]]; then
        log_error "前端构建产物 dist 目录不存在"
        exit 1
    fi
    log_success "前端构建完成"
else
    log_info "跳过前端构建"
fi

# ---------- 步骤4: 备份当前部署 ----------
if [[ ${SKIP_BACKUP} -eq 0 ]]; then
    log_step "========== 备份当前部署 =========="
    TIMESTAMP=$(date '+%Y%m%d_%H%M%S')
    BACKUP_PATH="${BACKUP_DIR}/${TIMESTAMP}"

    mkdir -p "${BACKUP_PATH}"

    # 备份后端 JAR
    if [[ -f "${DEPLOY_DIR}/${JAR_NAME}" ]]; then
        cp "${DEPLOY_DIR}/${JAR_NAME}" "${BACKUP_PATH}/${JAR_NAME}"
        log_info "后端 JAR 已备份"
    fi

    # 备份前端文件
    if [[ -d "${NGINX_DIR}" && "$(ls -A "${NGINX_DIR}" 2>/dev/null)" ]]; then
        mkdir -p "${BACKUP_PATH}/frontend"
        cp -r "${NGINX_DIR}/"* "${BACKUP_PATH}/frontend/"
        log_info "前端文件已备份"
    fi

    # 更新 latest 软链接
    rm -f "${BACKUP_DIR}/latest"
    ln -s "${BACKUP_PATH}" "${BACKUP_DIR}/latest"

    # 清理超过7天的备份
    find "${BACKUP_DIR}" -maxdepth 1 -type d -name "20*" -mtime +7 ! -path "${BACKUP_PATH}" -exec rm -rf {} \;

    log_success "备份完成: ${BACKUP_PATH}"
else
    log_info "跳过备份"
fi

# ---------- 步骤5: 部署后端 JAR ----------
log_step "========== 部署后端 =========="
mkdir -p "${DEPLOY_DIR}/logs"

if [[ ${SKIP_BACKEND} -eq 0 ]]; then
    cp "${JAR_PATH}" "${DEPLOY_DIR}/${JAR_NAME}"
    log_info "后端 JAR 已部署到 ${DEPLOY_DIR}/${JAR_NAME}"
fi

# 停止旧服务
if pgrep -f "${JAR_NAME}" >/dev/null 2>&1; then
    log_info "停止旧的后端服务..."
    pkill -15 -f "${JAR_NAME}"
    for i in $(seq 1 30); do
        if ! pgrep -f "${JAR_NAME}" >/dev/null 2>&1; then
            break
        fi
        sleep 1
    done
    if pgrep -f "${JAR_NAME}" >/dev/null 2>&1; then
        log_warn "优雅停止超时，强制终止..."
        pkill -9 -f "${JAR_NAME}"
        sleep 2
    fi
fi

# 启动新服务
log_info "启动后端服务..."
nohup java -jar \
    -Xms256m -Xmx512m \
    -Dspring.profiles.active=prod \
    "${DEPLOY_DIR}/${JAR_NAME}" \
    > "${DEPLOY_DIR}/logs/app.log" 2>&1 &

BACKEND_PID=$!
echo "${BACKEND_PID}" > "${DEPLOY_DIR}/backend.pid"
log_info "后端服务已启动 (PID: ${BACKEND_PID})"

# ---------- 步骤6: 部署前端到 Nginx ----------
if [[ ${SKIP_FRONTEND} -eq 0 ]]; then
    log_step "========== 部署前端 =========="
    rm -rf "${NGINX_DIR:?}"/*
    cp -r "${FRONTEND_DIR}/dist/"* "${NGINX_DIR}/"
    log_info "前端文件已部署到 ${NGINX_DIR}"

    # 重载 Nginx
    if command -v nginx &>/dev/null; then
        nginx -s reload 2>/dev/null || sudo nginx -s reload 2>/dev/null || true
        log_info "Nginx 已重载"
    elif systemctl is-active --quiet nginx 2>/dev/null; then
        sudo systemctl reload nginx
        log_info "Nginx 已重载 (systemd)"
    fi
fi

# ---------- 步骤7: 健康检查 ----------
log_step "========== 健康检查 =========="
log_info "等待 ${HEALTH_TIMEOUT} 秒后进行健康检查..."
sleep "${HEALTH_TIMEOUT}"

HEALTH_STATUS=1
for i in $(seq 1 5); do
    HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" "${HEALTH_URL}" 2>/dev/null || echo "000")
    if [[ "${HTTP_CODE}" == "200" ]]; then
        HEALTH_STATUS=0
        break
    fi
    log_warn "健康检查第 ${i} 次失败 (HTTP: ${HTTP_CODE})，5秒后重试..."
    sleep 5
done

if [[ ${HEALTH_STATUS} -ne 0 ]]; then
    log_error "健康检查失败！后端服务未正常启动"
    log_error "最近日志："
    tail -n 20 "${DEPLOY_DIR}/logs/app.log" 2>/dev/null || true
    rollback
fi

# ---------- 部署完成 ----------
echo ""
echo -e "${GREEN}============================================================${NC}"
echo -e "${GREEN}  部署成功！${NC}"
echo -e "${GREEN}============================================================${NC}"
echo -e "${BLUE}  后端服务:${NC}  ${HEALTH_URL}"
echo -e "${BLUE}  前端地址:${NC}  http://localhost"
echo -e "${BLUE}  后端 PID:${NC}  ${BACKEND_PID}"
echo -e "${BLUE}  部署时间:${NC}  $(date '+%Y-%m-%d %H:%M:%S')"
echo -e "${GREEN}============================================================${NC}"
