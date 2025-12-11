# ==============================================================================
# 自行车商城微服务 - 测试脚本 (直连服务 + 轮询验证负载均衡)
#
# 该脚本通过直接轮流调用服务实例来模拟客户端负载均衡，并测试核心链路：
# 1. 创建用户 (轮询 user-service 实例)
# 2. 创建商品 (product-service)
# 3. 上架商品 (product-service)
# 4. 验证商品已上架
# 5. 增加库存 (轮询 inventory-service 实例)
# 6. 创建订单 (order-service, 其内部调用会由Nacos负载均衡)
# 7. 验证库存扣减 (轮询 inventory-service 实例)
# 8. 取消订单 (order-service)
# 9. 验证库存回滚 (轮询 inventory-service 实例)
# 10. 循环请求以明确展示对 user-service 和 inventory-service 的轮询效果
# ==============================================================================

# --- 配置 ---
HOST="localhost"
USER_SERVICE_PORTS=("8081" "8086")         # User Service 的所有实例端口
INVENTORY_SERVICE_PORTS=("8084" "8085")   # Inventory Service 的所有实例端口
PRODUCT_SERVICE_PORT="8082"               # Product Service 的端口
ORDER_SERVICE_PORT="8083"                 # Order Service 的端口

# --- 脚本颜色定义 ---
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# --- 辅助函数：等待服务就绪 ---
function wait_for_service {
    local service_name=$1
    local port=$2
    echo -e "${YELLOW}--- 等待 ${service_name} (端口: ${port}) 启动... ---${NC}"
    for i in {1..30}; do
        if nc -z ${HOST} ${port}; then
            echo -e "${GREEN}✅ ${service_name} (端口: ${port}) 已就绪!${NC}"
            return 0
        fi
        echo "   - [尝试 ${i}/30] 端口 ${port} 尚未就绪，1秒后重试..."
        sleep 1
    done
    echo -e "${RED}❌ 等待超时: ${service_name} 未能在规定时间内启动。${NC}"
    exit 1
}

# --- 辅助函数：用于检查并打印测试结果 ---
# (健壮版本)
function check_result {
    local full_response="$1"
    local expected_code="$2"
    local test_description="$3"
    local command_to_print="$4"

    # 从多行响应中提取HTTP状态码 (更可靠的方法)
    # 使用 grep 和 tail 来获取第一行HTTP状态码
    local http_status_line=$(echo "$full_response" | grep "HTTP/" | head -n 1)
    local actual_code=$(echo "$http_status_line" | awk '{print $2}')

    # 提取响应体
    # 使用 sed 找到空行并打印之后的所有内容
    local body=$(echo "$full_response" | sed '1,/^\r$/d')

    if [[ "$actual_code" == "$expected_code" ]]; then
        echo -e "${GREEN}✅ 测试通过: ${test_description}${NC}"
        if [[ ! -z "$body" ]]; then
            # 尝试用jq美化，如果失败则直接打印
            if echo "$body" | jq . > /dev/null 2>&1; then
                echo "   - 响应体:"
                echo "$body" | jq .
            else
                echo "   - 响应体: ${body}"
            fi
        fi
    else
        echo -e "${RED}❌ 测试失败: ${test_description}${NC}"
        echo "   - 期望HTTP状态码: ${expected_code}, 实际: ${actual_code}"
        echo "   - 完整响应: "
        echo -e "${RED}${full_response}${NC}"
        echo "   - 执行的命令: ${command_to_print}"
        exit 1
    fi
}

# --- 辅助函数：实现客户端轮询 ---
USER_INSTANCE_INDEX=0
INVENTORY_INSTANCE_INDEX=0

function get_user_service_url {
    port_count=${#USER_SERVICE_PORTS[@]}
    port=${USER_SERVICE_PORTS[$USER_INSTANCE_INDEX]}
    USER_INSTANCE_INDEX=$(( (USER_INSTANCE_INDEX + 1) % port_count ))
    echo "http://${HOST}:${port}"
}

function get_inventory_service_url {
    port_count=${#INVENTORY_SERVICE_PORTS[@]}
    port=${INVENTORY_SERVICE_PORTS[$INVENTORY_INSTANCE_INDEX]}
    INVENTORY_INSTANCE_INDEX=$(( (INVENTORY_INSTANCE_INDEX + 1) % port_count ))
    echo "http://${HOST}:${port}"
}


# ==============================================================================
# ✨ 在所有测试开始前，调用等待函数 ✨
# ==============================================================================
for port in "${USER_SERVICE_PORTS[@]}"; do wait_for_service "User Service" "$port"; done
for port in "${INVENTORY_SERVICE_PORTS[@]}"; do wait_for_service "Inventory Service" "$port"; done
wait_for_service "Product Service" ${PRODUCT_SERVICE_PORT}
wait_for_service "Order Service" ${ORDER_SERVICE_PORT}

echo -e "\n${BLUE}========================= 所有服务已就绪, 开始测试流程 =========================${NC}\n"

# ==============================================================================
# 步骤 1: 创建新用户 (轮询 user-service)
# ==============================================================================
echo -e "\n${YELLOW}--- 步骤 1: 创建新用户 ---${NC}"
UNIQUE_SUFFIX="$(date +%s)-${RANDOM}"
UNIQUE_USER_ID="user-${UNIQUE_SUFFIX}"
UNIQUE_USERNAME="test-user-${UNIQUE_SUFFIX}"
# 使用时间戳的纳秒部分来生成一个随机的手机号后8位
UNIQUE_PHONE="138$(date +%N | cut -c -8)"
CREATE_USER_URL="$(get_user_service_url)/api/users"
CREATE_USER_CMD="curl -i -sS -X POST ${CREATE_USER_URL} \
-H 'Content-Type: application/json' \
-d '{
    \"userId\": \"${UNIQUE_USER_ID}\",
    \"username\": \"${UNIQUE_USERNAME}\",
    \"password\": \"password123\",
    \"phone\": \"${UNIQUE_PHONE}\",
    \"age\": 30
}'"
USER_RESPONSE=$(eval "$CREATE_USER_CMD")
USER_DB_ID=$(echo "$USER_RESPONSE" | sed '1,/^\r$/d' | jq -r '.id')
check_result "$USER_RESPONSE" "201" "成功创建用户 (Username: ${UNIQUE_USERNAME})" "$CREATE_USER_CMD"
USER_ID=${USER_DB_ID}

# ==============================================================================
# 步骤 2: 创建新商品 (product-service)
# ==============================================================================
echo -e "\n${YELLOW}--- 步骤 2: 创建新商品 ---${NC}"

# 【✅ 修正后的 curl 命令定义 ✅】
# 在 JSON 数据中添加了 "category" 字段
CREATE_PRODUCT_CMD="curl -i -sS -X POST http://${HOST}:${PRODUCT_SERVICE_PORT}/api/products \
-H 'Content-Type: application/json' \
-d '{
    \"brand\": \"LB-Brand\",
    \"model\": \"LB-Model-${UNIQUE_SUFFIX}\",
    \"price\": 7777,
    \"category\": \"公路车\"
}'"


PRODUCT_RESPONSE=$(eval "$CREATE_PRODUCT_CMD")
PRODUCT_ID=$(echo "$PRODUCT_RESPONSE" | sed '1,/^\r$/d' | jq -r '.id')
check_result "$PRODUCT_RESPONSE" "201" "成功创建商品 (ID: ${PRODUCT_ID})" "$CREATE_PRODUCT_CMD"
echo "👀 请检查 Product-Service 的控制台日志，确认它调用了 Inventory-Service 的不同实例端口。"
# ==============================================================================
# 步骤 3: 上架商品 (product-service)
# ==============================================================================
echo -e "\n${YELLOW}--- 步骤 3: 上架商品 ---${NC}"
PUBLISH_CMD="curl -i -sS -X POST http://${HOST}:${PRODUCT_SERVICE_PORT}/api/admin/products/${PRODUCT_ID}/publish"
PUBLISH_RESPONSE=$(eval "$PUBLISH_CMD")
check_result "$PUBLISH_RESPONSE" "200" "成功上架商品 (ID: ${PRODUCT_ID})" "$PUBLISH_CMD"
echo "👀 请检查 Product-Service 的控制台日志，确认更新上架状态的调用也触发了负载均衡。"

# ==============================================================================
# 步骤 4: 验证商品已上架 (product-service)
# ==============================================================================
echo -e "\n${YELLOW}--- 步骤 4: 验证商品已上架 ---${NC}"
GET_SHELF_PRODUCTS_CMD="curl -sS http://${HOST}:${PRODUCT_SERVICE_PORT}/api/products"
ON_SHELF_PRODUCTS=$(eval "$GET_SHELF_PRODUCTS_CMD")
if echo "$ON_SHELF_PRODUCTS" | jq -e --arg PID "$PRODUCT_ID" '.[] | select(.id == $PID)' > /dev/null; then
    echo -e "${GREEN}✅ 测试通过: 在上架商品列表中找到了新商品 (ID: ${PRODUCT_ID})${NC}"
else
    echo -e "${RED}❌ 测试失败: 在上架商品列表中未找到新商品 (ID: ${PRODUCT_ID})${NC}" && exit 1
fi

# ==============================================================================
# 步骤 5: 增加库存到 20 (轮询 inventory-service)
# ==============================================================================
echo -e "\n${YELLOW}--- 步骤 5: 编辑库存为 20 ---${NC}"
INCREASE_STOCK_URL="$(get_inventory_service_url)/api/inventory/increase"
INCREASE_STOCK_CMD="curl -i -sS -X POST ${INCREASE_STOCK_URL} -H 'Content-Type: application/json' -d '{\"productId\": \"${PRODUCT_ID}\", \"quantity\": 20}'"
INCREASE_RESPONSE=$(eval "$INCREASE_STOCK_CMD")
check_result "$INCREASE_RESPONSE" "200" "成功将商品库存增加到 20" "$INCREASE_STOCK_CMD"

# ==============================================================================
# 步骤 6: 创建订单 (order-service)
# ==============================================================================
echo -e "\n${YELLOW}--- 步骤 6: 创建订单购买 2 个商品 ---${NC}"
ORDER_QUANTITY=2
CREATE_ORDER_CMD="curl -i -sS -X POST http://${HOST}:${ORDER_SERVICE_PORT}/api/orders -H 'Content-Type: application/json' -d '{\"buyerId\": \"${USER_ID}\", \"productId\": \"${PRODUCT_ID}\", \"quantity\": ${ORDER_QUANTITY}}'"
ORDER_RESPONSE=$(eval "$CREATE_ORDER_CMD")
ORDER_ID=$(echo "$ORDER_RESPONSE" | sed '1,/^\r$/d' | jq -r '.id')
check_result "$ORDER_RESPONSE" "201" "成功创建订单 (ID: ${ORDER_ID})" "$CREATE_ORDER_CMD"
echo "👀 请检查 Order-Service 的控制台日志，确认它对 User-Service 和 Inventory-Service 的调用被负载均衡。"

# ==============================================================================
# 步骤 7: 验证库存扣减为 18 (轮询 inventory-service)
# ==============================================================================
echo -e "\n${YELLOW}--- 步骤 7: 验证库存是否扣减为 18 ---${NC}"
GET_INVENTORY_URL="$(get_inventory_service_url)/api/inventory/${PRODUCT_ID}"
GET_INVENTORY_CMD="curl -sS ${GET_INVENTORY_URL}"
CURRENT_STOCK=$(eval "$GET_INVENTORY_CMD" | jq -r '.stock')
EXPECTED_STOCK=$((20 - ORDER_QUANTITY))
if [ "$CURRENT_STOCK" -eq "$EXPECTED_STOCK" ]; then
    echo -e "${GREEN}✅ 测试通过: 商品库存已正确更新为 ${CURRENT_STOCK}${NC}"
else
    echo -e "${RED}❌ 测试失败: 库存不正确. 期望: ${EXPECTED_STOCK}, 实际: ${CURRENT_STOCK}${NC}" && exit 1
fi

# ==============================================================================
# 步骤 8: 取消订单 (order-service)
# ==============================================================================
echo -e "\n${YELLOW}--- 步骤 8: 取消订单 ---${NC}"
CANCEL_ORDER_CMD="curl -i -sS -X PUT http://${HOST}:${ORDER_SERVICE_PORT}/api/orders/${ORDER_ID}/cancel"
CANCEL_RESPONSE=$(eval "$CANCEL_ORDER_CMD")
check_result "$CANCEL_RESPONSE" "200" "成功取消订单 (ID: ${ORDER_ID})" "$CANCEL_ORDER_CMD"
echo "👀 请再次检查 Order-Service 日志，确认取消订单回滚库存的操作也被负载均衡。"

# ==============================================================================
# 步骤 9: 验证库存回滚为 20 (轮询 inventory-service)
# ==============================================================================
echo -e "\n${YELLOW}--- 步骤 9: 验证库存是否回滚为 20 ---${NC}"
GET_INVENTORY_URL_FINAL="$(get_inventory_service_url)/api/inventory/${PRODUCT_ID}"
FINAL_STOCK=$(curl -sS ${GET_INVENTORY_URL_FINAL} | jq -r '.stock')
if [ "$FINAL_STOCK" -eq "20" ]; then
    echo -e "${GREEN}✅ 测试通过: 商品库存已正确回滚为 ${FINAL_STOCK}${NC}"
else
    echo -e "${RED}❌ 测试失败: 库存回滚不正确. 期望: 20, 实际: ${FINAL_STOCK}${NC}" && exit 1
fi

echo -e "\n${BLUE}========================= 核心流程测试完毕, 验证服务端负载均衡 =========================${NC}\n"

# ==============================================================================
# 步骤 11: 循环创建订单，观察 order-service 对 user-service 的调用日志
# ==============================================================================
echo -e "\n${YELLOW}--- 步骤 11: 连续创建6个订单，观察 user-service 负载均衡效果 ---${NC}"
echo "👀 请密切关注【order-service】的控制台日志，寻找 '请求 User Service 验证用户成功' 的日志行。"

for i in {1..6}; do
    echo -n "  - 正在创建第 ${i} 个订单..."

    # 我们使用同一个用户和商品来重复创建订单
    CREATE_ORDER_CMD="curl -i -sS -X POST http://${HOST}:${ORDER_SERVICE_PORT}/api/orders \
    -H 'Content-Type: application/json' \
    -d '{
        \"buyerId\": \"${USER_ID}\",
        \"productId\": \"${PRODUCT_ID}\",
        \"quantity\": 1
    }'"

    # 执行命令，并只检查状态码是否为 201，不关心响应体
    RESPONSE=$(eval "$CREATE_ORDER_CMD")
    HTTP_CODE=$(echo "$RESPONSE" | head -n 1 | awk '{print $2}')

    if [[ "$HTTP_CODE" == "201" ]]; then
        echo -e "${GREEN}成功${NC}"
    else
        echo -e "${RED}失败 (HTTP: ${HTTP_CODE})${NC}"
        # 打印失败详情以便调试
        echo "$RESPONSE"
    fi

    # 每次请求后稍作暂停，避免请求过快
    sleep 1
done

echo -e "\n${YELLOW}--- 验证完成 ---${NC}"
echo "请检查上面6次订单创建过程中，order-service 的日志输出，确认 '实例详情' 后的端口号是否在 ${USER_SERVICE_PORTS[@]} 之间变化。"


echo -e "\n${BLUE}🎉🎉🎉 恭喜！所有测试流程成功执行完毕！ 🎉🎉🎉${NC}\n"
