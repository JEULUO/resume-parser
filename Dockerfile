# 使用 JDK 17 作为基础镜像
FROM openjdk:17-jdk-slim

# 设置工作目录
WORKDIR /app

# 复制 Maven 构建文件
COPY backend/pom.xml .

# 复制 Maven Wrapper 相关文件（如果没有就跳过这行）
COPY backend/mvnw* .

# 复制源代码
COPY backend/src ./src

# 构建应用（跳过测试）
RUN ./mvnw clean package -DskipTests

# 暴露端口
EXPOSE 8000

# 启动应用
CMD ["java", "-jar", "target/*.jar"]