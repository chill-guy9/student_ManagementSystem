#!/bin/bash
# ============================================================
# 日志轮转脚本 - 学生管理系统
# 功能：压缩旧日志、删除过期压缩日志、自定义目录、摘要通知
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
LOG_DIRS=("/opt/student-management/logs" "/var/log/student-management")
COMPRESS_DAYS=7
DELETE_DAYS=30
WEBHOOK_URL=""
DRY_RUN=0

# ---------- 参数解析 ----------
usage() {
    echo "Usage: $0 [OPTIONS]"
    echo "Options:"
    echo "  --log-dirs DIR1,DIR2  日志目录列表 (逗号分隔)"
    echo "  --compress-days N     压缩超过N天的 .log 文件 (默认: 7)"
    echo "  --delete-days N       删除超过N天的 .gz 文件 (默认: 30)"
    echo "  --webhook-url URL     发送轮转摘要的 Webhook URL"
    echo "  --dry-run             仅显示将要执行的操作，不实际执行"
    echo "  -h, --help            显示帮助"
    exit 1
}

while [[ $# -gt 0 ]]; do
    case "$1" in
        --log-dirs)      IFS=',' read -ra LOG_DIRS <<< "$2"; shift 2 ;;
        --compress-days) COMPRESS_DAYS="$2"; shift 2 ;;
        --delete-days)   DELETE_DAYS="$2"; shift 2 ;;
        --webhook-url)   WEBHOOK_URL="$2"; shift 2 ;;
        --dry-run)       DRY_RUN=1; shift ;;
        -h|--help)       usage ;;
        *)               log_error "未知参数: $1"; usage ;;
    esac
done

# ---------- 统计变量 ----------
COMPRESSED_COUNT=0
COMPRESSED_SIZE=0
DELETED_COUNT=0
DELETED_SIZE=0

# ---------- Webhook 通知 ----------
send_webhook() {
    local message="$1"

    if [[ -z "${WEBHOOK_URL}" ]]; then
        return 0
    fi

    local payload
    payload=$(cat <<EOF
{
    "service": "log-rotate",
    "message": "${message}",
    "compressed_count": ${COMPRESSED_COUNT},
    "deleted_count": ${DELETED_COUNT},
    "timestamp": "$(date -u '+%Y-%m-%dT%H:%M:%SZ')"
}
EOF
)

    curl -s -X POST \
        -H "Content-Type: application/json" \
        -d "${payload}" \
        "${WEBHOOK_URL}" >/dev/null 2>&1 || true
}

# ---------- 处理每个日志目录 ----------
for LOG_DIR in "${LOG_DIRS[@]}"; do
    if [[ ! -d "${LOG_DIR}" ]]; then
        log_warn "日志目录不存在: ${LOG_DIR}"
        continue
    fi

    log_info "处理日志目录: ${LOG_DIR}"

    # ---------- 压缩超过指定天数的 .log 文件 ----------
    log_info "查找超过 ${COMPRESS_DAYS} 天的 .log 文件..."

    while IFS= read -r -d '' LOG_FILE; do
        FILE_SIZE=$(du -b "${LOG_FILE}" | cut -f1)
        FILE_SIZE_HUMAN=$(du -h "${LOG_FILE}" | cut -f1)
        GZ_FILE="${LOG_FILE}.gz"

        # 跳过已经是符号链接的文件
        if [[ -L "${LOG_FILE}" ]]; then
            continue
        fi

        # 跳过正在写入的文件（最近1小时内修改过）
        if [[ "$(find "${LOG_FILE}" -mmin -60 2>/dev/null)" ]]; then
            log_info "跳过最近修改的文件: ${LOG_FILE}"
            continue
        fi

        if [[ ${DRY_RUN} -eq 1 ]]; then
            log_info "[DRY-RUN] 将压缩: ${LOG_FILE} (${FILE_SIZE_HUMAN})"
        else
            if gzip -c "${LOG_FILE}" > "${GZ_FILE}" && touch -r "${LOG_FILE}" "${GZ_FILE}"; then
                rm -f "${LOG_FILE}"
                log_info "已压缩: ${LOG_FILE} -> ${GZ_FILE} (${FILE_SIZE_HUMAN})"
            else
                log_error "压缩失败: ${LOG_FILE}"
                rm -f "${GZ_FILE}"
                continue
            fi
        fi

        COMPRESSED_COUNT=$((COMPRESSED_COUNT + 1))
        COMPRESSED_SIZE=$((COMPRESSED_SIZE + FILE_SIZE))
    done < <(find "${LOG_DIR}" -name "*.log" -type f -mtime +"${COMPRESS_DAYS}" -print0 2>/dev/null)

    # ---------- 删除超过指定天数的 .gz 文件 ----------
    log_info "查找超过 ${DELETE_DAYS} 天的 .gz 文件..."

    while IFS= read -r -d '' GZ_FILE; do
        FILE_SIZE=$(du -b "${GZ_FILE}" | cut -f1)
        FILE_SIZE_HUMAN=$(du -h "${GZ_FILE}" | cut -f1)

        if [[ ${DRY_RUN} -eq 1 ]]; then
            log_info "[DRY-RUN] 将删除: ${GZ_FILE} (${FILE_SIZE_HUMAN})"
        else
            rm -f "${GZ_FILE}"
            log_info "已删除: ${GZ_FILE} (${FILE_SIZE_HUMAN})"
        fi

        DELETED_COUNT=$((DELETED_COUNT + 1))
        DELETED_SIZE=$((DELETED_SIZE + FILE_SIZE))
    done < <(find "${LOG_DIR}" -name "*.gz" -type f -mtime +"${DELETE_DAYS}" -print0 2>/dev/null)
done

# ---------- 人类可读的大小格式化 ----------
human_size() {
    local bytes=$1
    if [[ ${bytes} -ge 1073741824 ]]; then
        echo "$((bytes / 1073741824))GB"
    elif [[ ${bytes} -ge 1048576 ]]; then
        echo "$((bytes / 1048576))MB"
    elif [[ ${bytes} -ge 1024 ]]; then
        echo "$((bytes / 1024))KB"
    else
        echo "${bytes}B"
    fi
}

# ---------- 发送摘要 ----------
SUMMARY="日志轮转完成: 压缩 ${COMPRESSED_COUNT} 个文件 ($(human_size ${COMPRESSED_SIZE})), 删除 ${DELETED_COUNT} 个文件 ($(human_size ${DELETED_SIZE}))"
send_webhook "${SUMMARY}"

# ---------- 输出摘要 ----------
echo ""
echo -e "${BLUE}========== 日志轮转摘要 ==========${NC}"
echo -e "  压缩文件数: ${COMPRESSED_COUNT}"
echo -e "  压缩释放:   $(human_size ${COMPRESSED_SIZE})"
echo -e "  删除文件数: ${DELETED_COUNT}"
echo -e "  删除释放:   $(human_size ${DELETED_SIZE})"
echo -e "${BLUE}===================================${NC}"

if [[ ${DRY_RUN} -eq 1 ]]; then
    log_warn "以上为 DRY-RUN 模式，未实际执行任何操作"
fi
