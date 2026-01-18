# ==========================================
# 第一阶段：构建构建 (Builder Stage)
# ==========================================
# 使用官方 Maven 镜像进行编译
# 如果您用 Gradle，请改为: gradle:8.0-jdk17-alpine
FROM maven:3.9-eclipse-temurin-17-alpine AS builder

# 设置工作目录
WORKDIR /app

# 1. 优先只拷贝 pom.xml 和依赖描述文件
# 这样做是为了利用 Docker 缓存：只要 pom.xml 没变，下次构建就不会重新下载依赖
COPY pom.xml .

# 下载依赖 (离线模式准备)
RUN mvn dependency:go-offline -B

# 2. 拷贝源代码
COPY src ./src

# 3. 打包应用 (跳过测试以加快构建速度，生产环境建议在 CI/CD 中跑测试)
RUN mvn clean package -DskipTests

# ==========================================
# 第二阶段：运行环境 (Runtime Stage)
# ==========================================
# 使用轻量级 JRE 镜像 (Eclipse Temurin 是 OpenJDK 的最佳替代)
FROM eclipse-temurin:17-jre-alpine

# 设置时区为上海 (股票数据对时间非常敏感，这步很关键！)
RUN apk add --no-cache tzdata && \
    cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && \
    echo "Asia/Shanghai" > /etc/timezone

WORKDIR /app

# 创建一个临时卷挂载点，Tomcat 默认需要
VOLUME /tmp

# 从第一阶段拷贝构建好的 jar 包
# 注意：这里假设生成的 jar 包在 target 目录下，且名字包含 snapshot
# 您可能需要根据实际生成的 jar 名字调整，或者在 pom.xml 里固定 finalName
COPY --from=builder /app/target/*.jar app.jar

# 暴露端口
EXPOSE 8080

# 启动命令
# 允许通过环境变量修改 Java 内存参数 (JAVA_OPTS)
# 允许通过命令行传递 Spring 参数 (如 --spring.profiles.active=prod)
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar $0 $@"]
