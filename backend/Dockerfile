# ============================================================
# 后端 Dockerfile - SpringBoot 3.3 + JDK17 + Maven
# 分层缓存策略：依赖层与源码层分离，Spring Boot 分层 JAR
# 严禁使用 # syntax=docker/dockerfile:* 语法
# ============================================================

# 基础镜像仓库地址（由 docker-compose 通过 build args 传入）
ARG DOCKER_REGISTRY

# ============================================================
# Stage 1: 构建阶段 - JDK + Maven 编译项目
# ============================================================
FROM ${DOCKER_REGISTRY}/library/eclipse-temurin:17-jdk AS builder

WORKDIR /app

# 安装 Maven（使用 apt 国内镜像源加速）
RUN apt-get update && \
    apt-get install -y --no-install-recommends maven && \
    rm -rf /var/lib/apt/lists/*

# 复制 Maven 配置文件（阿里云镜像源）
COPY settings.xml /root/.m2/settings.xml

# 仅复制 pom.xml，最大化利用 Docker 分层缓存
# 只要 pom.xml 不变，就不会重新下载依赖
COPY pom.xml ./

# 创建最小目录结构和占位符 Main 类，执行一次完整的 Maven 构建来下载所有依赖
# 这是最可靠的预下载方式，比 go-offline 更完整
RUN mkdir -p src/main/java/com/tooling/asset && \
    printf 'package com.tooling.asset;\nimport org.springframework.boot.SpringApplication;\nimport org.springframework.boot.autoconfigure.SpringBootApplication;\n@SpringBootApplication\npublic class ToolingAssetApplication { public static void main(String[] args) { SpringApplication.run(ToolingAssetApplication.class, args); } }\n' > src/main/java/com/tooling/asset/ToolingAssetApplication.java && \
    mkdir -p src/main/resources && \
    printf 'server.port=8099\nspring.jpa.hibernate.ddl-auto=none\nspring.jpa.show-sql=false\n' > src/main/resources/application.yml && \
    mvn package -DskipTests -B \
        -Dmaven.wagon.http.retryHandler.count=10 \
        -Dmaven.wagon.httpconnectionManager.ttlSeconds=30

# 删除占位符代码，准备复制真实源码
RUN rm -rf src target

# 复制所有业务源码（仅修改源码时，仅从此层开始重新构建）
COPY src ./src

# 执行生产环境构建，跳过测试
RUN mvn package -DskipTests -B \
    -Dmaven.wagon.http.retryHandler.count=10

# ============================================================
# Stage 2: 提取阶段 - Spring Boot 分层 JAR 提取
# ============================================================
FROM ${DOCKER_REGISTRY}/library/eclipse-temurin:17-jre AS extractor

WORKDIR /app

# 从构建阶段复制打包好的 JAR
COPY --from=builder /app/target/tooling-asset-1.0.0.jar app.jar

# 使用 Spring Boot layertools 提取分层（四层：dependencies / spring-boot-loader / snapshot-dependencies / application）
RUN java -Djarmode=layertools -jar app.jar extract

# ============================================================
# Stage 3: 运行阶段 - JRE 运行 Spring Boot 应用
# ============================================================
FROM ${DOCKER_REGISTRY}/library/eclipse-temurin:17-jre

WORKDIR /app

# 按层顺序复制，每层单独缓存（变更越频繁的层越靠后）
COPY --from=extractor /app/dependencies/ ./
COPY --from=extractor /app/spring-boot-loader/ ./
COPY --from=extractor /app/snapshot-dependencies/ ./
COPY --from=extractor /app/application/ ./

# 创建文件上传目录
RUN mkdir -p /app/uploads/tooling-images

# 暴露容器内部端口
EXPOSE 8099

# 启动 Spring Boot 应用（使用分层 JAR 启动器）
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
