# ============================================================
# 前端 Dockerfile - 学生管理系统
# 多阶段构建：Node 构建 + Nginx 运行
# ============================================================

# ==================== 阶段1: 构建 ====================
FROM node:20-alpine AS builder

WORKDIR /build

# 先复制 package 文件，利用 Docker 缓存
COPY frontend/package.json frontend/package-lock.json* ./

# 安装依赖
RUN npm ci --legacy-peer-deps

# 复制源代码
COPY frontend/ .

# 构建生产版本
RUN npm run build

# ==================== 阶段2: 运行 ====================
FROM nginx:alpine

LABEL maintainer="student-management-system"
LABEL description="学生管理系统 - 前端服务"

# 安装必要工具
RUN apk add --no-cache \
    tzdata \
    curl

# 设置时区
ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/${TZ} /etc/localtime && echo ${TZ} > /etc/timezone

# 从构建阶段复制产物
COPY --from=builder /build/dist /usr/share/nginx/html

# 复制 Nginx 配置
COPY docker/nginx.conf /etc/nginx/nginx.conf

# 暴露端口
EXPOSE 80 443

# 健康检查
HEALTHCHECK --interval=15s --timeout=5s --retries=3 --start-period=10s \
    CMD curl -f http://localhost/ || exit 1

# 启动 Nginx（前台运行）
CMD ["nginx", "-g", "daemon off;"]
