FROM maven:3.9.6-eclipse-temurin-25 AS builder
WORKDIR /app

# 复制 pom.xml 并预下载依赖（利用 Docker 缓存层）
COPY pom.xml .
RUN mvn dependency:go-offline -B

# 复制源代码并构建
COPY src ./src
RUN mvn clean package -DskipTests
FROM eclipse-temurin:25-jre
WORKDIR /app
COPY target/order-service.jar app.jar

# 健康检查
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8083/actuator/health || exit 1

EXPOSE 8083
ENTRYPOINT ["java", "-jar", "app.jar"]
