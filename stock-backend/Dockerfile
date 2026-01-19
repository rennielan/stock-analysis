# ==========================================
# 第一阶段：构建 (Builder Stage)
# ==========================================
FROM maven:3.9-eclipse-temurin-17-alpine AS builder

WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn clean package -DskipTests

# ==========================================
# 第二阶段：运行环境 (Runtime Stage)
# ==========================================
FROM eclipse-temurin:17-jre-alpine

RUN apk add --no-cache tzdata curl && \
    cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && \
    echo "Asia/Shanghai" > /etc/timezone

WORKDIR /app
VOLUME /tmp

COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8080

# 健康检查支持
HEALTHCHECK --interval=30s --timeout=10s --retries=3 \
    CMD curl -f http://localhost:8080/api/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar $0 $@"]