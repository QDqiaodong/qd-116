# ============================================================
# 前端 Dockerfile - Vue3 + Vite + Nginx
# 分层缓存策略：依赖层与源码层分离
# 严禁使用 # syntax=docker/dockerfile:* 语法
# ============================================================

# 基础镜像仓库地址（由 docker-compose 通过 build args 传入）
ARG DOCKER_REGISTRY

# ============================================================
# Stage 1: 构建阶段 - Node.js 编译前端项目
# ============================================================
FROM ${DOCKER_REGISTRY}/library/node:20-alpine AS builder

WORKDIR /app

# 配置 npm 全局使用淘宝镜像源（无需 VPN）
RUN npm config set registry https://registry.npmmirror.com

# 仅复制依赖描述文件，最大化利用 Docker 分层缓存
# 只要 package.json 和 package-lock.json 不变，就不会重新下载依赖
COPY package.json package-lock.json ./

# 使用 npm ci 精确安装（依赖 lockfile，速度更快，版本更稳定）
RUN npm ci

# 复制所有业务源码（仅修改源码时，仅从此层开始重新构建）
COPY . .

# 执行生产环境构建
RUN npm run build

# ============================================================
# Stage 2: 运行阶段 - Nginx 托管静态资源
# ============================================================
FROM ${DOCKER_REGISTRY}/library/nginx:alpine

# 复制构建产物
COPY --from=builder /app/dist /usr/share/nginx/html

# 复制 Nginx 配置
COPY nginx.conf /etc/nginx/conf.d/default.conf

# 暴露容器内部端口
EXPOSE 80

# 启动 Nginx
CMD ["nginx", "-g", "daemon off;"]
