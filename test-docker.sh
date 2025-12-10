# ==============================================================================
# 自行车商城微服务 - 端到端测试脚本 (V2 - 增加等待机制)
# ==============================================================================

# --- 配置 ---
HOST="localhost"
USER_SERVICE_PORT="8081"
PRODUCT_SERVICE_PORT="8082"
ORDER_SERVICE_PORT="8083"

# --- 脚本颜色定义 ---
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# ==============================================================================
# ✨ 新增功能: 等待服务就绪的函数 ✨
#
# $1: 服务名称 (用于日志)
# $2: 要检查的端口号
# ==============================================================================
function wait_for_service {
    local service_name=$1
    local port=$2
    local retries=30 # 最多重试30次
    local count=0
    local delay=2     # 每次重试间隔2秒

    echo -e "${YELLOW}--- 等待 ${service_name} (端口: ${port}) 启动... ---${NC}"

    # 使用 nc (netcat) 命令来检查端口是否开放
    until $(nc -z ${HOST} ${port}); do
        count=$((count + 1))
        if [ ${count} -gt ${retries} ]; then
            echo -e "${RED}❌ 等待超时: ${service_name} 未能在规定时间内启动。${NC}"
            exit 1
        fi
        echo "   - [尝试 ${count}/${retries}] 端口 ${port} 尚未就绪，${delay}秒后重试..."
        sleep ${delay}
    done

    echo -e "${GREEN}✅ ${service_name} 已就绪!${NC}"
}

# --- 辅助函数：用于打印测试结果 ---
function check_result {
    # $1: 要检查的ID, $2: 成功消息, $3: 失败时要打印的命令
    if [ -z "$1" ] || [ "$1" == "null" ]; then
        echo -e "${RED}❌ 测试失败: ${2}${NC}"
        if [ ! -z "$3" ]; then
            echo "--- 失败的请求详情 ---"
            eval "$3" # 执行并打印失败的命令
            echo "----------------------"
        fi
        exit 1
    else
        echo -e "${GREEN}✅ 测试通过: ${2}${NC}"
        echo "   - ID: $1"
    fi
}

# ==============================================================================
# ✨ 在所有测试开始前，调用等待函数 ✨
# ==============================================================================
wait_for_service "User Service" ${USER_SERVICE_PORT}
wait_for_service "Product Service" ${PRODUCT_SERVICE_PORT}
wait_for_service "Order Service" ${ORDER_SERVICE_PORT}

# ==============================================================================
# 步骤 1: 用户服务测试 (user-service)
# ==============================================================================
echo -e "\n${YELLOW}--- 步骤 1: 创建新用户 (user-service) ---${NC}"

# ✨ 随机生成用户ID和手机号，确保每次测试的唯一性 ✨
# 使用时间戳(秒)和Bash内置的随机数变量$RANDOM，保证唯一
UNIQUE_SUFFIX="$(date +%s)-${RANDOM}"
UNIQUE_USER_ID="user-${UNIQUE_SUFFIX}"
UNIQUE_USERNAME="test-user-${UNIQUE_SUFFIX}"
# 使用时间戳的纳秒部分来生成一个随机的手机号后8位
UNIQUE_PHONE="138$(date +%N | cut -c -8)"

CREATE_USER_CMD="curl -sS -X POST http://${HOST}:${USER_SERVICE_PORT}/api/users \
-H 'Content-Type: application/json' \
-d '{
    \"userId\": \"${UNIQUE_USER_ID}\",
    \"username\": \"${UNIQUE_USERNAME}\",
    \"password\": \"password123\",
    \"phone\": \"${UNIQUE_PHONE}\",
    \"age\": 30
}'"

# 执行创建并获取返回的ID用于检查
# 注意：我们后续步骤将使用我们自己生成的 ${UNIQUE_USER_ID} 作为用户标识
USER_ID_FROM_RESPONSE=$(eval "$CREATE_USER_CMD" | jq -r '.id')

check_result "$USER_ID_FROM_RESPONSE" "成功创建用户 (UserID: ${UNIQUE_USER_ID}, Phone: ${UNIQUE_PHONE})" "$CREATE_USER_CMD"

# 【重要】将我们生成的业务ID赋值给USER_ID，以便后续步骤（如创建订单）能正确使用
USER_ID=${USER_ID_FROM_RESPONSE}
# ==============================================================================
# 步骤 2: 创建新商品 (product-service)
# ==============================================================================
echo -e "\n${YELLOW}--- 步骤 2: 创建新商品 (product-service) ---${NC}"
INITIAL_STOCK=20
CREATE_PRODUCT_CMD="curl -sS -X POST http://${HOST}:${PRODUCT_SERVICE_PORT}/api/products \
-H 'Content-Type: application/json' \
-d '{
    \"brand\": \"GIANT\",
    \"category\": \"山地车\",
    \"model\": \"Stumpjumper-E2E-Test\",
    \"description\": \"林道骑行的全能利器\",
    \"price\": 35000.00,
    \"stock\": ${INITIAL_STOCK},
    \"onShelf\": true
}'"
PRODUCT_ID=$(eval "$CREATE_PRODUCT_CMD" | jq -r '.id') # 假设成功时ID在data字段
check_result "$PRODUCT_ID" "成功创建商品 (初始库存: ${INITIAL_STOCK})" "$CREATE_PRODUCT_CMD"

# ==============================================================================
# 步骤 3: 创建新订单 (order-service)
# ==============================================================================
echo -e "\n${YELLOW}--- 步骤 3: 创建新订单 (order-service) ---${NC}"
ORDER_QUANTITY=7
CREATE_ORDER_CMD="curl -sS -X POST http://${HOST}:${ORDER_SERVICE_PORT}/api/orders \
-H 'Content-Type: application/json' \
-d '{
    \"buyerId\": \"${USER_ID}\",
    \"productId\": \"${PRODUCT_ID}\",
    \"quantity\": \"${ORDER_QUANTITY}\"


}'"
ORDER_ID=$(eval "$CREATE_ORDER_CMD" | jq -r '.id') # 假设成功时ID在data字段
check_result "$ORDER_ID" "成功创建订单 (购买数量: ${ORDER_QUANTITY})" "$CREATE_ORDER_CMD"

# ==============================================================================
# 步骤 4: 验证商品库存是否减少 (product-service)
# ==============================================================================
echo -e "\n${YELLOW}--- 步骤 4: 验证商品库存是否减少 (product-service) ---${NC}"
GET_PRODUCT_CMD="curl -sS http://${HOST}:${PRODUCT_SERVICE_PORT}/api/products/${PRODUCT_ID}"
UPDATED_STOCK=$(eval "$GET_PRODUCT_CMD" | jq -r '.stock')

EXPECTED_STOCK=$((INITIAL_STOCK - ORDER_QUANTITY))

if [ "$UPDATED_STOCK" -eq "$EXPECTED_STOCK" ]; then
    echo -e "${GREEN}✅ 测试通过: 商品库存已正确更新为 ${UPDATED_STOCK}${NC}"
else
    echo -e "${RED}❌ 测试失败: 商品库存不正确. 期望值: ${EXPECTED_STOCK}, 实际值: ${UPDATED_STOCK}${NC}"
    echo "--- 失败的请求详情 (GET Product) ---"
    eval "$GET_PRODUCT_CMD" | jq
    echo "------------------------------------"
    exit 1
fi

# ==============================================================================
# 步骤 5: 查询并验证订单详情 (order-service)
# ==============================================================================
echo -e "\n${YELLOW}--- 步骤 5: 查询并验证订单详情 (order-service) ---${NC}"
GET_ORDER_CMD="curl -sS http://${HOST}:${ORDER_SERVICE_PORT}/api/orders/${ORDER_ID}"
ORDER_USER_ID=$(eval "$GET_ORDER_CMD" | jq -r '.buyerId')

if [ "$ORDER_USER_ID" == "$USER_ID" ]; then
    echo -e "${GREEN}✅ 测试通过: 订单中的 userId (${ORDER_USER_ID}) 与创建的用户ID匹配${NC}"
else
    echo -e "${RED}❌ 测试失败: 订单中的 userId (${ORDER_USER_ID}) 与预期的用户ID (${USER_ID}) 不匹配${NC}"
    echo "--- 失败的请求详情 (GET Order) ---"
    eval "$GET_ORDER_CMD" | jq
    echo "---------------------------------"
    exit 1
fi

echo -e "\n${GREEN}🎉🎉🎉 所有端到端测试流程成功执行完毕！ 🎉🎉🎉${NC}"