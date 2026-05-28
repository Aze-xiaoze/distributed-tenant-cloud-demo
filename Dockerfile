# ============================================================
# 分布式多租户企业级平台 — 通用多模块 Dockerfile
# 使用方式：docker-compose build 或 docker build --build-arg SERVICE_NAME=tenant-auth .
# 多阶段构建：Maven编译 → JRE运行
# ============================================================

# ======================== 阶段1：Maven 构建 ========================
FROM maven:3.9-eclipse-temurin-17 AS builder

# 构建参数：指定要打包的微服务模块名（tenant-auth / tenant-system / tenant-gateway）
ARG SERVICE_NAME

WORKDIR /build

# 1. 先复制 pom 文件，利用 Docker 缓存加速依赖下载
COPY pom.xml .
COPY tenant-common/pom.xml tenant-common/pom.xml
COPY tenant-core/pom.xml tenant-core/pom.xml
COPY tenant-api/pom.xml tenant-api/pom.xml
COPY tenant-auth/pom.xml tenant-auth/pom.xml
COPY tenant-system/pom.xml tenant-system/pom.xml
COPY tenant-gateway/pom.xml tenant-gateway/pom.xml

# 2. 下载依赖（仅 pom 变化时重新执行，利用缓存层）
RUN mvn dependency:go-offline -B -s /dev/null 2>/dev/null || true

# 3. 复制全部源码
COPY tenant-common/ tenant-common/
COPY tenant-core/ tenant-core/
COPY tenant-api/ tenant-api/
COPY tenant-auth/ tenant-auth/
COPY tenant-system/ tenant-system/
COPY tenant-gateway/ tenant-gateway/

# 4. 构建指定模块（跳过测试，-pl 指定模块，-am 同时构建依赖模块）
RUN mvn clean package -pl ${SERVICE_NAME} -am -DskipTests -B \
    -s /dev/null 2>/dev/null

# ======================== 阶段2：JRE 运行 ========================
FROM eclipse-temurin:17-jre-alpine

ARG SERVICE_NAME

# 安装 curl（用于 healthcheck）和时区数据
RUN apk add --no-cache curl tzdata \
    && cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime \
    && echo "Asia/Shanghai" > /etc/timezone \
    && apk del tzdata

# 创建非 root 用户运行应用
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app

# 从构建阶段复制 fat JAR
COPY --from=builder /build/${SERVICE_NAME}/target/*.jar app.jar

# 创建日志目录
RUN mkdir -p /app/logs && chown -R appuser:appgroup /app

USER appuser

# JVM 参数：容器感知 + GC优化 + OOM自动dump
ENV JAVA_OPTS="-XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=75.0 \
    -XX:+UseG1GC \
    -XX:+HeapDumpOnOutOfMemoryError \
    -XX:HeapDumpPath=/app/logs/heapdump.hprof \
    -Dfile.encoding=UTF-8 \
    -Duser.timezone=Asia/Shanghai"

# 暴露端口（实际端口由 application.yml 决定，此处声明默认值）
EXPOSE 8080

# 健康检查（由 docker-compose 覆盖具体端口）
HEALTHCHECK --interval=15s --timeout=10s --retries=3 --start-period=60s \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -jar app.jar"]
