#!/bin/bash
# ============================================================
# 数据库恢复脚本 - 学生管理系统
# 功能：从备份恢复 MySQL、解压、恢复前备份、确认提示
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

# ---------- 默认配置 ----------
BACKUP_FILE=""
DB_HOST="localhost"
DB_PORT="3306"
DB_NAME="student_management"
DB_USER="root"
DB_PASS=""
PRE_BACKUP_DIR="/opt/student-management/backups/mysql/pre-restore"
FORCE=0

# ---------- 参数解析 ----------
usage() {
    echo "Usage: $0 --backup-file FILE [OPTIONS]"
    echo ""
    echo "Required:"
    echo "  --backup-file FILE   备份文件路径 (.sql 或 .sql.gz)"
    echo ""
    echo "Options:"
    echo "  --db-host HOST       MySQL 主机 (默认: localhost)"
    echo "  --db-port PORT       MySQL 端口 (默认: 3306)"
    echo "  --db-name NAME       数据库名 (默认: student_management)"
    echo "  --db-user USER       MySQL 用户 (默认: root)"
    echo "  --db-pass PASS       MySQL 密码"
    echo "  --force              跳过确认提示（危险！）"
    echo "  -h, --help           显示帮助"
    exit 1
}

while [[ $# -gt 0 ]]; do
    case "$1" in
        --backup-file)  BACKUP_FILE="$2"; shift 2 ;;
        --db-host)      DB_HOST="$2"; shift 2 ;;
        --db-port)      DB_PORT="$2"; shift 2 ;;
        --db-name)      DB_NAME="$2"; shift 2 ;;
        --db-user)      DB_USER="$2"; shift 2 ;;
        --db-pass)      DB_PASS="$2"; shift 2 ;;
        --force)        FORCE=1; shift ;;
        -h|--help)      usage ;;
        *)              log_error "未知参数: $1"; usage ;;
    esac
done

# ---------- 参数校验 ----------
if [[ -z "${BACKUP_FILE}" ]]; then
    log_error "必须指定 --backup-file 参数"
    usage
fi

if [[ ! -f "${BACKUP_FILE}" ]]; then
    log_error "备份文件不存在: ${BACKUP_FILE}"
    exit 1
fi

# ---------- 确认提示 ----------
if [[ ${FORCE} -eq 0 ]]; then
    echo ""
    echo -e "${RED}╔══════════════════════════════════════════════════════════╗${NC}"
    echo -e "${RED}║              ⚠  数据库恢复操作 - 高风险  ⚠              ║${NC}"
    echo -e "${RED}╠══════════════════════════════════════════════════════════╣${NC}"
    echo -e "${RED}║  此操作将覆盖当前数据库的所有数据！                      ║${NC}"
    echo -e "${RED}║  恢复前会自动备份当前数据库。                            ║${NC}"
    echo -e "${RED}╚══════════════════════════════════════════════════════════╝${NC}"
    echo ""
    echo -e "  ${BLUE}目标数据库:${NC}  ${DB_NAME}@${DB_HOST}:${DB_PORT}"
    echo -e "  ${BLUE}备份文件:${NC}    ${BACKUP_FILE}"
    echo -e "  ${BLUE}文件大小:${NC}    $(du -h "${BACKUP_FILE}" | cut -f1)"
    echo ""
    read -rp "确认执行恢复操作？输入 YES 继续: " CONFIRM
    if [[ "${CONFIRM}" != "YES" ]]; then
        log_warn "操作已取消"
        exit 0
    fi
fi

# ---------- 检查依赖 ----------
if ! command -v mysql &>/dev/null; then
    log_error "未找到 mysql 命令"
    exit 1
fi

# ---------- 步骤1: 恢复前备份当前数据库 ----------
log_step "========== 恢复前备份当前数据库 =========="
mkdir -p "${PRE_BACKUP_DIR}"

PRE_TIMESTAMP=$(date '+%Y%m%d_%H%M%S')
PRE_BACKUP_FILE="${PRE_BACKUP_DIR}/${DB_NAME}_pre_restore_${PRE_TIMESTAMP}.sql.gz"

MYSQLDUMP_OPTS=(-h "${DB_HOST}" -P "${DB_PORT}" -u "${DB_USER}" --single-transaction --routines --triggers --set-gtid-purged=OFF)
if [[ -n "${DB_PASS}" ]]; then
    MYSQLDUMP_OPTS+=(-p"${DB_PASS}")
fi

if mysqldump "${MYSQLDUMP_OPTS[@]}" "${DB_NAME}" 2>/dev/null | gzip > "${PRE_BACKUP_FILE}"; then
    log_info "当前数据库已备份到: ${PRE_BACKUP_FILE}"
else
    log_warn "恢复前备份失败（数据库可能为空），继续执行恢复..."
fi

# ---------- 步骤2: 解压备份文件 ----------
log_step "========== 准备备份文件 =========="
SQL_FILE=""

if [[ "${BACKUP_FILE}" == *.gz ]]; then
    log_info "解压 gzip 备份文件..."
    SQL_FILE="${BACKUP_FILE%.gz}"
    gunzip -c "${BACKUP_FILE}" > "${SQL_FILE}"
    log_info "解压完成: ${SQL_FILE}"
else
    SQL_FILE="${BACKUP_FILE}"
fi

# ---------- 步骤3: 执行恢复 ----------
log_step "========== 执行数据库恢复 =========="
log_info "恢复数据库: ${DB_NAME}@${DB_HOST}:${DB_PORT}"

MYSQL_OPTS=(-h "${DB_HOST}" -P "${DB_PORT}" -u "${DB_USER}")
if [[ -n "${DB_PASS}" ]]; then
    MYSQL_OPTS+=(-p"${DB_PASS}")
fi

RESTORE_START=$(date +%s)

if mysql "${MYSQL_OPTS[@]}" "${DB_NAME}" < "${SQL_FILE}"; then
    RESTORE_END=$(date +%s)
    RESTORE_DURATION=$((RESTORE_END - RESTORE_START))
    log_info "数据库恢复成功 (耗时 ${RESTORE_DURATION}s)"
else
    RESTORE_END=$(date +%s)
    RESTORE_DURATION=$((RESTORE_END - RESTORE_START))
    log_error "数据库恢复失败！"
    log_error "恢复前备份位于: ${PRE_BACKUP_FILE}"
    log_error "可使用以下命令恢复到恢复前状态："
    echo ""
    echo "  gunzip -c ${PRE_BACKUP_FILE} | mysql ${MYSQL_OPTS[*]} ${DB_NAME}"
    echo ""

    # 清理解压的临时文件
    if [[ "${BACKUP_FILE}" == *.gz && -f "${SQL_FILE}" ]]; then
        rm -f "${SQL_FILE}"
    fi
    exit 1
fi

# ---------- 步骤4: 清理临时文件 ----------
if [[ "${BACKUP_FILE}" == *.gz && -f "${SQL_FILE}" ]]; then
    rm -f "${SQL_FILE}"
    log_info "临时解压文件已清理"
fi

# ---------- 完成 ----------
echo ""
echo -e "${GREEN}╔══════════════════════════════════════════════════════════╗${NC}"
echo -e "${GREEN}║                  数据库恢复成功！                        ║${NC}"
echo -e "${GREEN}╠══════════════════════════════════════════════════════════╣${NC}"
echo -e "${GREEN}║  数据库:   ${DB_NAME}@${DB_HOST}:${DB_PORT}$(printf '%*s' $((30 - ${#DB_NAME} - ${#DB_HOST} - ${#DB_PORT})) '')║${NC}"
echo -e "${GREEN}║  恢复耗时: ${RESTORE_DURATION}s$(printf '%*s' $((40 - ${#RESTORE_DURATION})) '')║${NC}"
echo -e "${GREEN}║  恢复前备份: ${PRE_BACKUP_FILE}$(printf '%*s' $((20)) '')║${NC}"
echo -e "${GREEN}╚══════════════════════════════════════════════════════════╝${NC}"
