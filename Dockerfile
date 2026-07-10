# 使用 Maven 镜像构建（自带 Maven，不需要 mvnw）
FROM maven:3.8.4-openjdk-17 AS build

WORKDIR /app

# 复制 pom.xml 并下载依赖（利用 Docker 缓存）
COPY backend/pom.xml .
RUN mvn dependency:go-offline -B

# 复制源代码并构建
COPY backend/src ./src
RUN mvn clean package -DskipTests

# 运行阶段：使用轻量级 JRE
FROM openjdk:17-jdk-slim

WORKDIR /app

# 从构建阶段复制 jar 包
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8000

CMD ["java", "-jar", "app.jar"]