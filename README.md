# 注塑车间定位块工装资产登记系统

面向小型注塑车间的纯实物资产台账管理系统，实现工装全生命周期管理：建档 → 移位 → 清点 → 报废。

---

## 🚀 快速启动

### 一键启动（推荐）

```bash
bash start.sh
```

脚本会自动完成：
- ✅ 端口占用检测（冲突时明确报错，不自动换端口）
- ✅ 停止旧容器（如果存在）
- ✅ Docker 镜像构建与服务启动
- ✅ 健康检查等待
- ✅ 构建完成后输出访问地址
- ✅ 页面访问一致性验证

### 手动启动

```bash
# 1. 加载环境变量
export $(grep -v '^#' .env | grep -v '^$' | xargs)

# 2. 构建并启动
docker compose up -d --build

# 3. 查看状态
docker compose ps

# 4. 查看日志
docker compose logs -f
```

---

## 🌐 访问地址

| 服务 | 地址 |
|------|------|
| **前端页面** | http://localhost:3044 |
| 后端 API | http://127.0.0.1:8099 |
| MySQL | 127.0.0.1:3366 |
| Redis | 127.0.0.1:6399 |

> **重要**：所有服务均绑定 `127.0.0.1`，禁止外部访问。`localhost` 与 `127.0.0.1` 必须指向同一服务。

---

## 📋 端口分配表（固定端口，禁止修改）

| 服务 | 宿主机端口 | 容器端口 | 说明 |
|------|-----------|---------|------|
| 前端 Nginx | **3044** | 80 | 避开 80/8080/3000 |
| 后端 SpringBoot | **8099** | 8099 | 避开 8080/8081/8090 |
| MySQL | **3366** | 3306 | 避开 3306/3307/3308 |
| Redis | **6399** | 6379 | 避开 6379/6389 |

### 端口冲突检测

如果端口被占用，`start.sh` 会明确报错并显示占用进程：

```
错误：端口 3044 (前端) 已被占用
  占用进程 PID: 12345
  进程名称: node

请先停止占用该端口的进程，或修改 .env 中的端口配置。
停止命令示例： kill -9 12345
```

---

## 🗂️ 目录结构

```
qd-116/
├── .env                      # 全局环境变量配置（端口、镜像仓库等）
├── .dockerignore             # 根目录构建上下文排除规则
├── docker-compose.yml        # Docker Compose 编排配置
├── start.sh                  # 一键启动脚本（端口检测+构建+验证）
├── README.md                 # 本文档
│
├── backend/                  # 后端 SpringBoot 3.3 项目
│   ├── Dockerfile            # 后端 Dockerfile（三阶段分层构建）
│   ├── .dockerignore         # 后端构建上下文排除
│   ├── pom.xml               # Maven 依赖配置
│   ├── settings.xml          # Maven 阿里云镜像配置
│   ├── sql/
│   │   └── init.sql          # 数据库初始化脚本（含示例数据）
│   └── src/                  # Java 源代码
│
└── frontend/                 # 前端 Vue 3 项目
    ├── Dockerfile            # 前端 Dockerfile（两阶段分层构建）
    ├── .dockerignore         # 前端构建上下文排除
    ├── .npmrc                # npm 淘宝镜像配置
    ├── package.json          # npm 依赖配置
    ├── package-lock.json     # npm 版本锁定（用于 npm ci）
    ├── vite.config.js        # Vite 配置（固定端口 3044）
    ├── nginx.conf            # Nginx 配置
    └── src/                  # Vue 源代码
```

---

## 🐳 Docker 构建缓存机制

### 核心原则：依赖层与源码层彻底分离

#### 前端构建流程（两阶段）

```
Stage 1: Builder (node:20-alpine)
  1. 配置 npm 淘宝镜像源
  2. COPY package.json package-lock.json  ← 仅依赖描述文件
  3. RUN npm ci                           ← 依赖层，仅 lockfile 变化才重跑
  4. COPY . .                             ← 业务源码层
  5. RUN npm run build                    ← 仅源码变化时重跑

Stage 2: Runtime (nginx:alpine)
  COPY dist + nginx.conf
```

#### 后端构建流程（三阶段 + Spring Boot 分层 JAR）

```
Stage 1: Builder (eclipse-temurin:17-jdk)
  1. 安装 Maven + 配置阿里云镜像
  2. COPY pom.xml                         ← 仅依赖描述文件
  3. RUN mvn dependency:go-offline        ← 依赖层，仅 pom 变化才重跑
  4. COPY src ./src                       ← 业务源码层
  5. RUN mvn package -DskipTests          ← 仅源码变化时重跑

Stage 2: Extractor (eclipse-temurin:17-jre)
  1. COPY app.jar
  2. java -Djarmode=layertools extract    ← 提取为 4 个独立层

Stage 3: Runtime (eclipse-temurin:17-jre)
  COPY dependencies/         ← 第三方依赖，最稳定
  COPY spring-boot-loader/   ← Spring Boot 启动器
  COPY snapshot-dependencies/ ← SNAPSHOT 依赖
  COPY application/          ← 业务代码，最频繁变更
```

### 构建速度对比

| 场景 | 首次构建 | 仅修改源码（第二次构建） | 仅修改 pom.xml/package.json |
|------|---------|------------------------|---------------------------|
| 前端 | 2-3 分钟 | **15-30 秒**（复用 npm ci 缓存） | 重新执行 npm ci |
| 后端 | 8-12 分钟 | **30-60 秒**（复用依赖下载缓存） | 重新下载所有 Maven 依赖 |

> **提升幅度**：仅修改业务代码时，构建速度提升约 **66%-85%**。

### 关键约束

- ❌ 严禁使用 `# syntax=docker/dockerfile:*` 语法（会远程拉取 frontend 镜像）
- ✅ 仅使用 Docker 原生分层缓存机制
- ✅ 所有基础镜像统一通过 `DOCKER_REGISTRY` 环境变量前缀引用
- ✅ `.dockerignore` 排除 node_modules、target、dist、IDE 配置等无关文件

---

## 🛠️ 技术栈

### 前端
- **框架**：Vue 3.4 + Vite 5
- **UI 组件**：Element Plus 2.7（按需自动导入）
- **HTTP 客户端**：Axios 1.7
- **图片压缩**：browser-image-compression 2.0（前端压缩至 0.5MB / 800px）
- **路由**：Vue Router 4.3

### 后端
- **框架**：Spring Boot 3.3
- **JDK**：OpenJDK 17
- **ORM**：Spring Data JPA + Hibernate
- **缓存**：Redis 7 + Spring Data Redis（Hash 结构缓存规格模板）
- **数据库**：MySQL 8.0（utf8mb4 字符集）
- **构建工具**：Maven 3.9
- **数据库连接池**：HikariCP（Spring Boot 默认）

### 部署架构
```
浏览器 (127.0.0.1:3044)
    ↓
Nginx (前端静态资源 + /api 反向代理)
    ↓
Spring Boot (8099)
    ↓           ↓
MySQL (3306)  Redis (6379)
```

---

## 📦 镜像源配置

无需 VPN 即可正常拉取所有依赖。

### Docker 基础镜像仓库

在 `.env` 中配置：

```env
DOCKER_REGISTRY=docker.1ms.run
```

可选国内镜像源：
| 服务商 | 地址 |
|--------|------|
| 1ms.run (推荐) | `docker.1ms.run` |
| DaoCloud | `docker.m.daocloud.io` |
| 网易云 | `hub-mirror.c.163.com` |
| 腾讯云 | `ccr.ccs.tencentyun.com` |
| 阿里云 | `你的专属ID.mirror.aliyuncs.com` |

### npm 前端依赖

`frontend/.npmrc`：
```
registry=https://registry.npmmirror.com
```

### Maven 后端依赖

`backend/settings.xml` 已配置阿里云镜像：
```xml
<mirror>
  <id>aliyunmaven</id>
  <mirrorOf>*</mirrorOf>
  <name>阿里云公共仓库</name>
  <url>https://maven.aliyun.com/repository/public</url>
</mirror>
```

---

## 🔍 自检命令

启动完成后可执行以下检查确保一切正常：

```bash
# 1. 检查端口监听（必须是 127.0.0.1）
lsof -nP -iTCP:3044 -sTCP:LISTEN
lsof -nP -iTCP:8099 -sTCP:LISTEN

# 2. 验证前端页面一致性（两个地址返回必须一致）
curl -sS http://127.0.0.1:3044 | head -20
curl -sS http://localhost:3044 | head -20

# 3. 验证后端 API
curl -sS http://127.0.0.1:8099/api/tooling/list | python3 -m json.tool

# 4. 验证 MySQL 连接
mysql -h 127.0.0.1 -P 3366 -u root -ptooling2024 -e "SHOW DATABASES;"

# 5. 验证 Redis 连接
redis-cli -h 127.0.0.1 -p 6399 ping
```

---

## 🔧 常用命令

```bash
# 启动服务
bash start.sh

# 查看服务状态
docker compose ps

# 查看所有日志
docker compose logs -f

# 查看指定服务日志
docker compose logs -f backend
docker compose logs -f frontend

# 重启服务
docker compose restart

# 停止服务（保留数据卷）
docker compose down

# 停止服务并删除数据卷（⚠️ 清空数据库）
docker compose down -v

# 仅重新构建某个服务
docker compose up -d --build backend
```

---

## 📋 核心功能模块

### 1. 工装基础信息建档
- 工装编号、适配产品、固定存放工位、入库日期
- 工装图片上传（前端自动压缩至 ≤ 0.5MB）
- 唯一编号校验

### 2. 工装工位移位登记
- 记录工装更换存放工位操作
- 自动更新工装状态为「已移位」
- 移位历史可追溯

### 3. 月度实物清点核对
- 自动统计账面数量
- 录入实物数量，自动计算差异
- 差异数量颜色编码显示

### 4. 工装报废归档登记
- 登记达到使用上限工装的报废信息
- 自动更新工装状态为「已报废」
- 报废记录永久归档

---

## ⚠️ 注意事项

1. **端口规则**：所有端口固定，冲突时必须手动解决，禁止自动换端口
2. **绑定地址**：所有服务仅绑定 `127.0.0.1`，禁止 `0.0.0.0`
3. **环境变量**：所有配置集中在 `.env`，禁止在代码中写死
4. **数据库迁移**：如需修改表结构，请在 `init.sql` 中使用幂等 SQL（`CREATE TABLE IF NOT EXISTS`、`ALTER TABLE ... ADD COLUMN IF NOT EXISTS`）
5. **数据备份**：MySQL 数据存储在 Docker 命名卷中，删除容器不会丢失数据。如需迁移，使用 `docker cp` 或 `mysqldump` 备份。

---

## 🐛 常见问题

### Q: 构建时报错 `port already in use`
A: 按照 start.sh 提示的 PID 杀掉占用进程，或修改 `.env` 中的端口配置。

### Q: 基础镜像拉取失败
A: 检查 `.env` 中的 `DOCKER_REGISTRY` 配置，尝试换成其他国内镜像源。

### Q: 依赖下载缓慢
A: 首次构建属于正常现象，后续构建只要 `pom.xml` / `package-lock.json` 不变，会自动复用缓存。

### Q: 中文乱码
A: 已全链路配置 utf8mb4，如仍有乱码，请检查浏览器编码设置。

### Q: localhost 和 127.0.0.1 打开的不是同一个项目
A: 这是严重问题，请检查：
   1. 是否有其他服务监听了 IPv6 的 [::1]:3044
   2. `/etc/hosts` 中 localhost 是否正确指向 127.0.0.1
   3. 运行 `lsof -nP -iTCP:3044` 检查所有监听地址
