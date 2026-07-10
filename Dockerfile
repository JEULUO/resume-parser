# 使用 JDK 17 作为基础镜像
FROM openjdk:17-jdk-slim

# 设置工作目录
WORKDIR /app

# 复制 Maven 构建文件
COPY backend/pom.xml .
COPY backend/.mvn ./.mvn
COPY backend/mvnw.cmd .

# 复制源代码
COPY backend/src ./src

# 构建应用（跳过测试，加快构建速度）
RUN ./mvnw.cmd clean package -DskipTests || ./mvnw clean package -DskipTests

# 暴露端口（和 application.yml 里配置的一致）
EXPOSE 8000

# 启动应用
CMD ["java", "-jar", "target/*.jar"]