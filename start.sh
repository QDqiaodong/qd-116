#!/bin/bash
# ======================================================================
# 注塑车间定位块工装资产登记系统 - 一键启动脚本
# ======================================================================
# 功能：
#   1. 从 .env 读取配置
#   2. 严格检测端口占用，冲突时报错并退出（不自动换端口）
#   3. 显示占用端口的进程信息
#   4. 启动 Docker Compose 构建
#   5. 等待服务健康检查通过
#   6. 构建完成后输出前端访问地址
# ======================================================================

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

# 从 .env 加载环境变量
if [ ! -f .env ]; then
    echo -e "${RED}错误：未找到 .env 配置文件${NC}"
    exit 1
fi

# 加载 .env 变量（忽略注释和空行）
export $(grep -v '^#' .env | grep -v '^$' | xargs)

echo "=========================================="
echo "  注塑车间定位块工装资产登记系统"
echo "=========================================="
echo ""

# 固定端口检测（严格模式：不自动换端口，冲突直接报错）
check_port() {
    local port=$1
    local name=$2
    local pid=$(lsof -nP -iTCP:${port} -sTCP:LISTEN -t 2>/dev/null | head -1)
    
    if [ -n "$pid" ]; then
        local proc_info=$(ps -p $pid -o comm= 2>/dev/null || echo "未知进程")
        echo -e "${RED}错误：端口 ${port} (${name}) 已被占用${NC}"
        echo -e "  占用进程 PID: ${YELLOW}${pid}${NC}"
        echo -e "  进程名称: ${YELLOW}${proc_info}${NC}"
        echo ""
        echo "请先停止占用该端口的进程，或修改 .env 中的端口配置。"
        echo "停止命令示例： kill -9 ${pid}"
        echo ""
        return 1
    fi
    return 0
}

echo -e "${YELLOW}正在检测端口占用...${NC}"

PORTS=(
    "$FRONTEND_PORT:前端"
    "$BACKEND_PORT:后端"
    "$MYSQL_PORT:MySQL"
    "$REDIS_PORT:Redis"
)

PORT_CONFLICT=0
for item in "${PORTS[@]}"; do
    port=${item%%:*}
    name=${item##*:}
    if ! check_port "$port" "$name"; then
        PORT_CONFLICT=1
    fi
done

if [ $PORT_CONFLICT -ne 0 ]; then
    echo -e "${RED}端口冲突检测失败，请解决上述问题后重试。${NC}"
    exit 1
fi

echo -e "${GREEN}所有端口可用：${NC}"
echo "  前端:  ${FRONTEND_PORT}"
echo "  后端:  ${BACKEND_PORT}"
echo "  MySQL: ${MYSQL_PORT}"
echo "  Redis: ${REDIS_PORT}"
echo ""

# 停止旧容器（如果存在）
echo -e "${YELLOW}正在停止旧容器...${NC}"
docker compose down 2>/dev/null || true
echo ""

# 执行 Docker Compose 构建并启动
echo -e "${YELLOW}正在构建并启动服务（首次构建需要下载依赖，请耐心等待）...${NC}"
echo "  基础镜像仓库: ${DOCKER_REGISTRY}"
echo "  项目名称: ${PROJECT_NAME}"
echo ""

if ! docker compose up -d --build; then
    echo -e "${RED}构建或启动失败，请检查上面的错误信息。${NC}"
    exit 1
fi

echo ""
echo -e "${YELLOW}正在等待服务健康检查通过...${NC}"

# 等待后端和数据库健康检查通过
MAX_WAIT=60
WAIT_COUNT=0
while [ $WAIT_COUNT -lt $MAX_WAIT ]; do
    if docker compose ps mysql --format '{{.State}}' 2>/dev/null | grep -q "healthy" && \
       docker compose ps redis --format '{{.State}}' 2>/dev/null | grep -q "healthy" && \
       docker compose ps backend --format '{{.State}}' 2>/dev/null | grep -q "running"; then
        echo -e "${GREEN}所有服务启动成功！${NC}"
        break
    fi
    sleep 2
    WAIT_COUNT=$((WAIT_COUNT + 2))
    echo -n "."
done

if [ $WAIT_COUNT -ge $MAX_WAIT ]; then
    echo ""
    echo -e "${YELLOW}等待超时，请手动检查服务状态： docker compose ps${NC}"
fi

echo ""
echo "=========================================="
echo "  构建完成！"
echo "=========================================="
echo ""
echo -e "  前端访问地址: ${GREEN}http://localhost:${FRONTEND_PORT}${NC}"
echo -e "  也可以访问:   ${GREEN}http://127.0.0.1:${FRONTEND_PORT}${NC}"
echo ""
echo "  后端API地址:  http://127.0.0.1:${BACKEND_PORT}"
echo "  MySQL端口:    127.0.0.1:${MYSQL_PORT}"
echo "  Redis端口:    127.0.0.1:${REDIS_PORT}"
echo ""
echo "  常用命令:"
echo "    查看状态:  docker compose ps"
echo "    查看日志:  docker compose logs -f [服务名]"
echo "    停止服务:  docker compose down"
echo "    重启服务:  docker compose restart"
echo ""
echo "=========================================="

# 验证前端页面返回（双地址对比）
echo -e "${YELLOW}正在验证页面访问一致性...${NC}"

# 等待前端 Nginx 启动
sleep 3

# 检查两个地址的页面标题是否一致
TITLE_127=$(curl -sS http://127.0.0.1:${FRONTEND_PORT} 2>/dev/null | grep -o '<title>[^<]*</title>' || echo "")
TITLE_LOCAL=$(curl -sS http://localhost:${FRONTEND_PORT} 2>/dev/null | grep -o '<title>[^<]*</title>' || echo "")

if [ "$TITLE_127" = "$TITLE_LOCAL" ] && [ -n "$TITLE_127" ]; then
    echo -e "${GREEN}页面验证通过，127.0.0.1 与 localhost 返回内容一致。${NC}"
    echo "  页面标题: $(echo $TITLE_127 | sed 's/<[^>]*>//g')"
else
    echo -e "${YELLOW}警告：页面验证失败，请手动检查。${NC}"
    echo "  127.0.0.1 返回: $TITLE_127"
    echo "  localhost 返回: $TITLE_LOCAL"
fi

echo ""
echo "=========================================="
echo -e "${GREEN}项目启动完成！请在浏览器中访问：${NC}"
echo -e "${GREEN}http://localhost:${FRONTEND_PORT}${NC}"
echo "=========================================="
