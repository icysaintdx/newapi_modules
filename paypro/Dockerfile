# PayPro Dockerfile
FROM maven:3.8-openjdk-8 AS builder

WORKDIR /app
COPY pom.xml .
COPY src ./src

# 编译项目
RUN mvn clean package -DskipTests

# 运行阶段
FROM amazoncorretto:8

WORKDIR /app

# 复制编译好的 jar 包
COPY --from=builder /app/target/*.jar app.jar

# 创建静态资源目录（用于存储上传的二维码）
RUN mkdir -p /app/static/qrcodes && mkdir -p /app/static/qr

# 暴露端口
EXPOSE 8889

# 启动应用
ENTRYPOINT ["java", "-jar", "app.jar"]
