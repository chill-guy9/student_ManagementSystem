#!/bin/bash
# ============================================================
# MySQL 备份脚本 - 学生管理系统
# 功能：mysqldump 导出、gzip 压缩、锁文件、清理旧备份、记录、告警
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

# ---------- 默认配置 ----------
DB_HOST="localhost"
DB_PORT="3306"
DB_NAME="student_management"
DB_USER="root"
DB_PASS=""
BACKUP_DIR="/opt/student-management/backups/mysql"
RETENTION_DAYS=30
LOCK_FILE="/tmp/mysql-backup.lock"
WEBHOOK_URL=""
RECORD_TABLE="backup_records"

# ---------- 参数解析 ----------
usage() {
    echo "Usage: $0 [OPTIONS]"
    echo "Options:"
    echo "  --db-host HOST       MySQL 主机 (默认: localhost)"
    echo "  --db-port PORT       MySQL 端口 (默认: 3306)"
    echo "  --db-name NAME       数据库名 (默认: student_management)"
    echo "  --db-user USER       MySQL 用户 (默认: root)"
    echo "  --db-pass PASS       MySQL 密码"
    echo "  --backup-dir DIR     备份目录 (默认: /opt/student-management/backups/mysql)"
    echo "  --retention DAYS     保留天数 (默认: 30)"
    echo "  --webhook-url URL    告警 Webhook URL"
    echo "  -h, --help           显示帮助"
    exit 1
}

while [[ $# -gt 0 ]]; do
    case "$1" in
        --db-host)      DB_HOST="$2"; shift 2 ;;
        --db-port)      DB_PORT="$2"; shift 2 ;;
        --db-name)      DB_NAME="$2"; shift 2 ;;
        --db-user)      DB_USER="$2"; shift 2 ;;
        --db-pass)      DB_PASS="$2"; shift 2 ;;
        --backup-dir)   BACKUP_DIR="$2"; shift 2 ;;
        --retention)    RETENTION_DAYS="$2"; shift 2 ;;
        --webhook-url)  WEBHOOK_URL="$2"; shift 2 ;;
        -h|--help)      usage ;;
        *)              log_error "未知参数: $1"; usage ;;
    esac
done

# ---------- Webhook 告警函数 ----------
send_webhook() {
    local status="$1"
    local message="$2"

    if [[ -z "${WEBHOOK_URL}" ]]; then
        return 0
    fi

    local payload
    payload=$(cat <<EOF
{
    "status": "${status}",
    "service": "mysql-backup",
    "database": "${DB_NAME}",
    "message": "${message}",
    "timestamp": "$(date -u '+%Y-%m-%dT%H:%M:%SZ')"
}
EOF
)

    curl -s -X POST \
        -H "Content-Type: application/json" \
        -d "${payload}" \
        "${WEBHOOK_URL}" >/dev/null 2>&1 || true
}

# ---------- 锁文件检查 ----------
if [[ -f "${LOCK_FILE}" ]]; then
    LOCK_PID=$(cat "${LOCK_FILE}" 2>/dev/null)
    if kill -0 "${LOCK_PID}" 2>/dev/null; then
        log_error "另一个备份进程正在运行 (PID: ${LOCK_PID})"
        send_webhook "FAILED" "备份进程冲突，已有进程 PID:${LOCK_PID} 在运行"
        exit 1
    else
        log_warn "发现过期的锁文件，清理中..."
        rm -f "${LOCK_FILE}"
    fi
fi

echo $$ > "${LOCK_FILE}"
trap 'rm -f "${LOCK_FILE}"' EXIT

# ---------- 检查依赖 ----------
if ! command -v mysqldump &>/dev/null; then
    log_error "未找到 mysqldump 命令"
    send_webhook "FAILED" "未找到 mysqldump 命令"
    exit 1
fi

if ! command -v gzip &>/dev/null; then
    log_error "未找到 gzip 命令"
    send_webhook "FAILED" "未找到 gzip 命令"
    exit 1
fi

# ---------- 创建备份目录 ----------
mkdir -p "${BACKUP_DIR}"

# ---------- 执行备份 ----------
TIMESTAMP=$(date '+%Y%m%d_%H%M%S')
BACKUP_FILE="${BACKUP_DIR}/${DB_NAME}_${TIMESTAMP}.sql"
BACKUP_GZ="${BACKUP_FILE}.gz"

log_info "开始备份数据库: ${DB_NAME}@${DB_HOST}:${DB_PORT}"

# 构建 mysqldump 命令
MYSQLDUMP_OPTS=(
    -h "${DB_HOST}"
    -P "${DB_PORT}"
    -u "${DB_USER}"
    --single-transaction
    --routines
    --triggers
    --events
    --set-gtid-purged=OFF
    --quick
    --lock-tables=false
)

if [[ -n "${DB_PASS}" ]]; then
    MYSQLDUMP_OPTS+=(-p"${DB_PASS}")
fi

# 执行导出
BACKUP_START=$(date +%s)

if mysqldump "${MYSQLDUMP_OPTS[@]}" "${DB_NAME}" > "${BACKUP_FILE}" 2>/dev/null; then
    log_info "数据库导出成功"
else
    EXIT_CODE=$?
    log_error "数据库导出失败 (退出码: ${EXIT_CODE})"
    rm -f "${BACKUP_FILE}"
    send_webhook "FAILED" "mysqldump 失败，退出码: ${EXIT_CODE}"
    exit 1
fi

# 压缩备份
log_info "压缩备份文件..."
gzip "${BACKUP_FILE}"

BACKUP_END=$(date +%s)
BACKUP_DURATION=$((BACKUP_END - BACKUP_START))
BACKUP_SIZE=$(du -h "${BACKUP_GZ}" | cut -f1)
BACKUP_SIZE_BYTES=$(stat -c%s "${BACKUP_GZ}" 2>/dev/null || stat -f%z "${BACKUP_GZ}" 2>/dev/null || echo 0)
START_TIME_FMT=$(date -d @${BACKUP_START} '+%Y-%m-%d %H:%M:%S' 2>/dev/null || date -r ${BACKUP_START} '+%Y-%m-%d %H:%M:%S' 2>/dev/null || echo "1970-01-01 00:00:00")

log_info "备份完成: ${BACKUP_GZ} (${BACKUP_SIZE}, 耗时 ${BACKUP_DURATION}s)"

# ---------- 清理旧备份 ----------
log_info "清理超过 ${RETENTION_DAYS} 天的旧备份..."
DELETED_COUNT=$(find "${BACKUP_DIR}" -name "*.sql.gz" -type f -mtime +"${RETENTION_DAYS}" -delete -print | wc -l)
if [[ ${DELETED_COUNT} -gt 0 ]]; then
    log_info "已清理 ${DELETED_COUNT} 个旧备份文件"
fi

# ---------- 记录到 backup_records 表 ----------
log_info "记录备份信息到数据库..."

# Use the Flyway-migrated backup_records schema
INSERT_SQL="INSERT INTO backup_records (file_name, file_path, file_size, status, trigger_type, operator_id, operator_name, started_at, finished_at)
VALUES ('${BACKUP_GZ}', '${BACKUP_DIR}/${BACKUP_GZ}', ${BACKUP_SIZE_BYTES}, 'SUCCESS', 'MANUAL', 'system', '系统', '${START_TIME_FMT}', NOW());"

MYSQL_OPTS=(-h "${DB_HOST}" -P "${DB_PORT}" -u "${DB_USER}")
if [[ -n "${DB_PASS}" ]]; then
    MYSQL_OPTS+=(-p"${DB_PASS}")
fi

mysql "${MYSQL_OPTS[@]}" "${DB_NAME}" -e "${INSERT_SQL}" 2>/dev/null || {
    log_warn "无法写入备份记录到数据库（非致命错误）"
}

# ---------- 完成 ----------
send_webhook "SUCCESS" "数据库 ${DB_NAME} 备份完成，文件: ${BACKUP_GZ}，大小: ${BACKUP_SIZE}，耗时: ${BACKUP_DURATION}s"

echo ""
log_info "========== 备份摘要 =========="
log_info "数据库:   ${DB_NAME}@${DB_HOST}:${DB_PORT}"
log_info "备份文件: ${BACKUP_GZ}"
log_info "文件大小: ${BACKUP_SIZE}"
log_info "耗时:     ${BACKUP_DURATION}s"
log_info "=============================="
