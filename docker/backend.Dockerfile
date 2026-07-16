# ============================================================
# 后端 Dockerfile - 学生管理系统
# 多阶段构建：Maven 构建 + JRE 运行
# ============================================================

# ==================== 阶段1: 构建 ====================
FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /build

# 先复制 POM 文件，利用 Docker 缓存加速依赖下载
COPY backend/pom.xml .

# 下载依赖（缓存层）
RUN mvn dependency:go-offline -B

# 复制源代码
COPY backend/src ./src

# 构建项目
RUN mvn clean package -DskipTests -B \
    && JAR_FILE=$(find target -name "*.jar" ! -name "*-sources.jar" ! -name "*-javadoc.jar" | head -1) \
    && mv "${JAR_FILE}" /app.jar

# ==================== 阶段2: 运行 ====================
FROM eclipse-temurin:21-jre-alpine

LABEL maintainer="student-management-system"
LABEL description="学生管理系统 - 后端服务"

# 安装必要工具
RUN apk add --no-cache \
    curl \
    tzdata \
    fontconfig \
    ttf-dejavu

# 设置时区
ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/${TZ} /etc/localtime && echo ${TZ} > /etc/timezone

# 创建非 root 用户
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# 创建应用目录
WORKDIR /app

# 从构建阶段复制 JAR
COPY --from=builder /app.jar /app/app.jar

# 创建日志目录
RUN mkdir -p /app/logs /app/backups && chown -R appuser:appgroup /app

# 切换到非 root 用户
USER appuser

# 暴露端口
EXPOSE 8080

# JVM 参数优化
ENV JAVA_OPTS="-Xms256m -Xmx512m \
    -XX:+UseG1GC \
    -XX:MaxGCPauseMillis=200 \
    -XX:+HeapDumpOnOutOfMemoryError \
    -XX:HeapDumpPath=/app/logs/ \
    -Dfile.encoding=UTF-8 \
    -Duser.timezone=Asia/Shanghai \
    -Djava.security.egd=file:/dev/./urandom"

# 健康检查
HEALTHCHECK --interval=15s --timeout=10s --retries=5 --start-period=60s \
    CMD curl -f http://localhost:8080/api/system/health || exit 1

# 启动命令
ENTRYPOINT ["sh", "-c", "exec java ${JAVA_OPTS} -jar /app/app.jar"]
