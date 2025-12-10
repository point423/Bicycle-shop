# ==============================================================================
# 自行车商城微服务 - 库存，管理员测试脚本
#
# 该脚本将测试以下核心链路:
# 1. 创建用户
# 2. (管理员) 创建一个新商品 (此时应为下架状态)
# 3. (管理员) 上架该商品
# 4. (用户) 查看所有上架商品，并验证新商品可见
# 5. (用户) 创建订单购买该商品
# 6. 验证库存是否正确扣减 (通过新创建的库存查询API)
# 7. (用户) 取消订单
# 8. 验证库存是否正确回滚
# ==============================================================================

# --- 配置 ---
HOST="localhost"
USER_SERVICE_PORT="8081"
PRODUCT_SERVICE_PORT="8082"
ORDER_SERVICE_PORT="8083"
INVENTORY_SERVICE_PORT="8084" # 新增库存服务端口

# --- 脚本颜色定义 ---
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# --- 辅助函数：等待服务就绪 ---
function wait_for_service {
    local service_name=$1
    local port=$2
    echo -e "${YELLOW}--- 等待 ${service_name} (端口: ${port}) 启动... ---${NC}"
    # 使用 nc (netcat) 或等效命令检查端口
    for i in {1..30}; do
        if nc -z ${HOST} ${port}; then
            echo -e "${GREEN}✅ ${service_name} 已就绪!${NC}"
            return 0
        fi
        echo "   - [尝试 ${i}/30] 端口 ${port} 尚未就绪，2秒后重试..."
        sleep 2
    done
    echo -e "${RED}❌ 等待超时: ${service_name} 未能在规定时间内启动。${NC}"
    exit 1
}

# --- 辅助函数：用于检查并打印测试结果 ---
function check_result {
    local response=$1
    local expected_http_status=$2
    local test_description=$3
    local command_to_print=$4

    local http_status=$(echo "$response" | head -n 1 | awk '{print $2}') # 从响应头获取状态码

    if [[ "$http_status" == *"$expected_http_status"* ]]; then
        echo -e "${GREEN}✅ 测试通过: ${test_description}${NC}"
        # 提取JSON Body部分打印
        local body=$(echo "$response" | sed '1,/^\r$/d')
        if [[ ! -z "$body" ]]; then
            echo "   - 响应体: ${body}"
        fi
    else
        echo -e "${RED}❌ 测试失败: ${test_description}${NC}"
        echo "   - 期望HTTP状态码: ${expected_http_status}"
        echo "   - 实际响应: "
        echo -e "${RED}${response}${NC}"
        echo "   - 执行的命令: ${command_to_print}"
        exit 1
    fi
}

# ==============================================================================
# ✨ 在所有测试开始前，调用等待函数 ✨
# ==============================================================================
wait_for_service "User Service" ${USER_SERVICE_PORT}
wait_for_service "Product Service" ${PRODUCT_SERVICE_PORT}
wait_for_service "Inventory Service" ${INVENTORY_SERVICE_PORT}
wait_for_service "Order Service" ${ORDER_SERVICE_PORT}

echo -e "\n${BLUE}========================= 所有服务已就绪, 开始测试流程 =========================${NC}\n"
# ==============================================================================
# 步骤 1: 创建新用户 (user-service)
# ==============================================================================
echo -e "\n${YELLOW}--- 步骤 1: 创建新用户 (user-service) ---${NC}"
# 使用时间戳(秒)和Bash内置的随机数变量$RANDOM，保证唯一
UNIQUE_SUFFIX="$(date +%s)-${RANDOM}"
UNIQUE_USER_ID="user-${UNIQUE_SUFFIX}"
UNIQUE_USERNAME="test-user-${UNIQUE_SUFFIX}"
# 使用时间戳的纳秒部分来生成一个随机的手机号后8位
UNIQUE_PHONE="138$(date +%N | cut -c -8)"
CREATE_USER_CMD="curl -i -sS -X POST http://${HOST}:${USER_SERVICE_PORT}/api/users \
-H 'Content-Type: application/json' \
-d '{
    \"userId\": \"${UNIQUE_USER_ID}\",
    \"username\": \"${UNIQUE_USERNAME}\",
    \"password\": \"password123\",
    \"phone\": \"${UNIQUE_PHONE}\",
    \"age\": 30
}'"
USER_RESPONSE=$(eval "$CREATE_USER_CMD")
# 从响应体中获取数据库生成的ID，用于检查，但后续不一定使用
USER_DB_ID=$(echo "$USER_RESPONSE" | sed '1,/^\r$/d' | jq -r '.id')

# 使用新的check_result函数进行断言
check_result "$USER_RESPONSE" "201" "成功创建用户 (Username: ${UNIQUE_USERNAME})" "$CREATE_USER_CMD"
USER_ID=${USER_DB_ID}

# ==============================================================================
# 步骤 2: (管理员) 创建新商品 (product-service)
# ==============================================================================
echo -e "\n${YELLOW}--- 步骤 2: (管理员) 创建新商品 (product-service) ---${NC}"
CREATE_PRODUCT_CMD="curl -i -sS -X POST http://${HOST}:${PRODUCT_SERVICE_PORT}/api/products -H 'Content-Type: application/json' -d '{\"brand\": \"E2E-Brand\", \"category\": \"测试车\", \"model\": \"Test-Model-${UNIQUE_SUFFIX}\", \"price\": 9999}'"
PRODUCT_RESPONSE=$(eval "$CREATE_PRODUCT_CMD")
PRODUCT_ID=$(echo "$PRODUCT_RESPONSE" | sed '1,/^\r$/d' | jq -r '.id')
check_result "$PRODUCT_RESPONSE" "201" "成功创建商品 (ID: ${PRODUCT_ID})" "$CREATE_PRODUCT_CMD"

# ==============================================================================
# 步骤 3: (管理员) 上架该商品 (调用 product-service 的 admin 接口)
# ==============================================================================
echo -e "\n${YELLOW}--- 步骤 3: (管理员) 上架商品 ---${NC}"
PUBLISH_CMD="curl -i -sS -X POST http://${HOST}:${PRODUCT_SERVICE_PORT}/api/admin/products/${PRODUCT_ID}/publish"
PUBLISH_RESPONSE=$(eval "$PUBLISH_CMD")
check_result "$PUBLISH_RESPONSE" "200" "成功上架商品 (ID: ${PRODUCT_ID})" "$PUBLISH_CMD"

# ==============================================================================
# 步骤 4: (用户) 查看上架商品列表，并验证新商品存在
# ==============================================================================
echo -e "\n${YELLOW}--- 步骤 4: (用户) 验证商品已上架 ---${NC}"
GET_SHELF_PRODUCTS_CMD="curl -sS http://${HOST}:${PRODUCT_SERVICE_PORT}/api/products"
ON_SHELF_PRODUCTS=$(eval "$GET_SHELF_PRODUCTS_CMD")
# 使用jq检查返回的JSON数组中是否包含我们的商品ID
if echo "$ON_SHELF_PRODUCTS" | jq -e --arg PID "$PRODUCT_ID" '.[] | select(.id == $PID)' > /dev/null; then
    echo -e "${GREEN}✅ 测试通过: 在上架商品列表中找到了新商品 (ID: ${PRODUCT_ID})${NC}"
else
    echo -e "${RED}❌ 测试失败: 在上架商品列表中未找到新商品 (ID: ${PRODUCT_ID})${NC}"
    echo "   - 查询结果: ${ON_SHELF_PRODUCTS}"
    exit 1
fi

# ==============================================================================
# 步骤 5: (管理员) 编辑库存，将库存从0增加到20 (调用 inventory-service)
# ==============================================================================
echo -e "\n${YELLOW}--- 步骤 5: (管理员) 编辑库存为 20 ---${NC}"
# 作为一个快速替代，我们连续调用20次 increase 接口
INCREASE_STOCK_CMD="curl -i -sS -X POST http://${HOST}:${INVENTORY_SERVICE_PORT}/api/inventory/increase -H 'Content-Type: application/json' -d '{\"productId\": \"${PRODUCT_ID}\", \"quantity\": 20}'"
INCREASE_RESPONSE=$(eval "$INCREASE_STOCK_CMD")
check_result "$INCREASE_RESPONSE" "200" "成功将商品库存增加到 20" "$INCREASE_STOCK_CMD"

# ==============================================================================
# 步骤 6: (用户) 创建订单，购买 2 个商品 (order-service)
# ==============================================================================
echo -e "\n${YELLOW}--- 步骤 6: (用户) 创建订单购买 2 个商品 ---${NC}"
ORDER_QUANTITY=2
CREATE_ORDER_CMD="curl -i -sS -X POST http://${HOST}:${ORDER_SERVICE_PORT}/api/orders -H 'Content-Type: application/json' -d '{\"buyerId\": \"${USER_ID}\", \"productId\": \"${PRODUCT_ID}\", \"quantity\": ${ORDER_QUANTITY}}'"
ORDER_RESPONSE=$(eval "$CREATE_ORDER_CMD")
ORDER_ID=$(echo "$ORDER_RESPONSE" | sed '1,/^\r$/d' | jq -r '.id')
check_result "$ORDER_RESPONSE" "201" "成功创建订单 (ID: ${ORDER_ID})" "$CREATE_ORDER_CMD"

# ==============================================================================
# 步骤 7: 验证库存是否从 20 减少到 18 (inventory-service)
# ==============================================================================
echo -e "\n${YELLOW}--- 步骤 7: 验证库存是否扣减为 18 ---${NC}"
# 注意：我们需要一个查询库存的接口，这里假设已实现 GET /api/inventory/{productId}
GET_INVENTORY_CMD="curl -sS http://${HOST}:${INVENTORY_SERVICE_PORT}/api/inventory/${PRODUCT_ID}"
CURRENT_STOCK=$(eval "$GET_INVENTORY_CMD" | jq -r '.stock')
EXPECTED_STOCK=$((20 - ORDER_QUANTITY))

if [ "$CURRENT_STOCK" -eq "$EXPECTED_STOCK" ]; then
    echo -e "${GREEN}✅ 测试通过: 商品库存已正确更新为 ${CURRENT_STOCK}${NC}"
else
    echo -e "${RED}❌ 测试失败: 商品库存不正确. 期望值: ${EXPECTED_STOCK}, 实际值: ${CURRENT_STOCK}${NC}"
    exit 1
fi

# ==============================================================================
# 步骤 8: (用户) 取消订单 (order-service)
# ==============================================================================
echo -e "\n${YELLOW}--- 步骤 8: (用户) 取消订单 ---${NC}"
CANCEL_ORDER_CMD="curl -i -sS -X PUT http://${HOST}:${ORDER_SERVICE_PORT}/api/orders/${ORDER_ID}/cancel"
CANCEL_RESPONSE=$(eval "$CANCEL_ORDER_CMD")
check_result "$CANCEL_RESPONSE" "200" "成功取消订单 (ID: ${ORDER_ID})" "$CANCEL_ORDER_CMD"

# ==============================================================================
# 步骤 9: 验证库存是否已回滚到 20 (inventory-service)
# ==============================================================================
echo -e "\n${YELLOW}--- 步骤 9: 验证库存是否回滚为 20 ---${NC}"
FINAL_STOCK=$(eval "$GET_INVENTORY_CMD" | jq -r '.stock')
if [ "$FINAL_STOCK" -eq "20" ]; then
    echo -e "${GREEN}✅ 测试通过: 商品库存已正确回滚为 ${FINAL_STOCK}${NC}"
else
    echo -e "${RED}❌ 测试失败: 商品库存回滚不正确. 期望值: 20, 实际值: ${FINAL_STOCK}${NC}"
    exit 1
fi


echo -e "\n${BLUE}🎉🎉🎉 恭喜！所有端到端测试流程成功执行完毕！ 🎉🎉🎉${NC}\n"
