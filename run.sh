
# 该脚本用于构建并启动项目的所有 Docker 服务。
echo "正在根据 docker-compose.yml 构建并启动所有服务..."

# 执行 docker-compose 命令
# --build: 在启动前构建镜像，如果镜像有变动则重新构建
# -d: 在后台 (detached mode) 运行容器
docker-compose up --d --build

echo "✅ 操作完成。服务正在后台启动。"
echo "请稍后访问 http://localhost:8849 使用账号 `nacos` 密码 `nacos` 登录。在 "服务列表" 中应能看到所有微服务均已成功注册。"
echo "不同测试脚本是在不同tag下的相应测试，需clone不同tag下进行测试"