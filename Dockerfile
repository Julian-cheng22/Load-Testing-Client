# ---------- 构建阶段 ----------
# 用带 JDK17 的 Maven 官方镜像
FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app

# 先拷 pom 再拷 src，利用缓存
COPY pom.xml .
COPY src ./src

# 打包 Spring Boot 应用（跳过测试）
RUN mvn -q -e -DskipTests clean package

# ---------- 运行阶段 ----------
# 运行阶段只需要一个 JDK17 运行镜像
FROM eclipse-temurin:17-jdk-jammy
WORKDIR /app

# 从上一阶段拷贝 jar
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]