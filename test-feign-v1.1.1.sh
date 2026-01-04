# ==============================================================================
# 微服务全功能自动化测试脚本 (OpenFeign: 负载均衡 & 熔断降级)
#
# 特性:
# - 自动创建测试所需的用户和商品，并提取ID。
# - 无需手动配置任何UUID。
# - 系统化地测试负载均衡、写操作熔断、读操作降级。
# ==============================================================================

# --- 配置 ---
BASE_URL_ORDER="http://localhost:8083/api/orders"
BASE_URL_PRODUCT="http://localhost:8082/api/products"
BASE_URL_ADMIN_PRODUCT="http://localhost:8082/api/admin/products" # 管理员接口
BASE_URL_USER="http://localhost:8081/api/users"
BASE_URL_INVENTORY="http://localhost:8084/api/inventory"

# 动态获取的ID
USER_ID=""
PRODUCT_ID=""

# 颜色定义
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# --- 辅助函数 ---
function print_header() {
    echo -e "\n${BLUE}=====================================================${NC}"
    echo -e "${BLUE}  $1 ${NC}"
    echo -e "${BLUE}=====================================================${NC}"
}

function check_jq() {
    if ! command -v jq &> /dev/null
    then
        echo -e "${RED}错误: 'jq' 命令未找到。${NC}"
        echo -e "${YELLOW}本脚本需要 'jq' 来解析JSON响应。请先安装它。${NC}"
        # ... (安装提示) ...
        exit 1
    fi
}

# --- 自动化测试准备 ---
function prepare_test_data() {
    print_header "0. 准备测试数据 (自动创建用户和商品)"

    # ... (创建用户的逻辑) ...
    UNIQUE_SUFFIX="$(date +%s)-${RANDOM}"
    UNIQUE_USER_ID="user-${UNIQUE_SUFFIX}"
    UNIQUE_USERNAME="test-user-${UNIQUE_SUFFIX}"
    UNIQUE_PHONE="138$(date +%N | cut -c -8)"
    CREATE_USER_CMD="curl -sS -X POST "$BASE_URL_USER" \
    -H 'Content-Type: application/json' \
    -d '{\"userId\": \"${UNIQUE_USER_ID}\", \"username\": \"${UNIQUE_USERNAME}\", \"password\": \"password123\", \"phone\": \"${UNIQUE_PHONE}\", \"age\": 30}'"
    USER_ID_FROM_RESPONSE=$(eval "$CREATE_USER_CMD" | jq -r '.id')
    USER_ID=${USER_ID_FROM_RESPONSE}
    echo -e "${GREEN}用户创建成功！ USER_ID: $USER_ID ${NC}"

    # ... (创建商品的逻辑) ...
    echo -e "\n${YELLOW}--> 正在创建新商品...${NC}"
    local product_response
    product_response=$(curl -s -X POST "$BASE_URL_PRODUCT" \
           -H "Content-Type: application/json" \
           -d '{"brand": "GIANT-AutoTest", "category": "山地车", "model": "Stumpjumper-'${UNIQUE_SUFFIX}'", "price": 35000.00}')
    PRODUCT_ID=$(echo "$product_response" | jq -r '.id')
    if [[ -z "$PRODUCT_ID" || "$PRODUCT_ID" == "null" ]]; then
           echo -e "${RED}错误: 创建商品失败或未能提取商品ID！${NC}"
           echo "响应内容: $product_response"
           exit 1
    fi
    echo -e "${GREEN}商品创建成功！ PRODUCT_ID: $PRODUCT_ID ${NC}"

    # ... (增加初始库存的逻辑保持不变) ...
    echo -e "\n${YELLOW}--> 正在为新商品增加初始库存 (100)...${NC}"
    curl -s -X POST "$BASE_URL_INVENTORY/increase" \
        -H "Content-Type: application/json" \
        -d "{\"productId\": \"$PRODUCT_ID\", \"quantity\": 100}" > /dev/null
    echo -e "${GREEN}初始库存增加成功。${NC}"
}

# --- 测试场景 ---

# 1. 测试负载均衡 (Load Balancing)
function test_load_balancing() {
    print_header "1. 负载均衡测试"

    # --- 1.1 测试 Order Service -> User/Inventory Service ---
    echo -e "\n${YELLOW}--- 1.1 负载均衡测试 (Order Service -> User/Inventory Service) ---${NC}"
    echo "我们将连续发起5次创建订单的请求。"
    echo "请观察【order-service】的日志，看它对 user-service 和 inventory-service 的调用是否分发到了不同实例。"

    echo ""
    for i in {1..5}; do
        echo -n -e "${GREEN}--> 发起第 $i 次创建订单请求... ${NC}"
        response=$(curl -s -X POST "${BASE_URL_ORDER}" \
             -H "Content-Type: application/json" \
             -d "{\"buyerId\": \"$USER_ID\", \"productId\": \"$PRODUCT_ID\", \"quantity\": 1}")
        order_id=$(echo "$response" | jq -r '.id')
        if [[ -z "$order_id" || "$order_id" == "null" ]]; then
            echo -e "${RED}失败！${NC}"
            echo "响应: $response"
        else
            echo -e "${GREEN}成功。${NC}"
        fi
        sleep 1
    done
    echo -e "\n${GREEN}订单服务负载均衡测试完成。${NC}"

    # --- 1.2 测试 Product Service -> Inventory Service ---
    echo -e "\n${YELLOW}--- 1.2 负载均衡测试 (Product Service -> Inventory Service) ---${NC}"
    echo "我们将连续发起6次上架/下架操作来触发服务间调用。"
    echo "请观察【product-service】的日志，看它对 inventory-service 的调用是否分发到了不同实例。"

    echo ""
    for i in {1..6}; do
        # 轮流执行上架和下架
        if (( i % 2 == 1 )); then
            action="publish"
            action_desc="上架"
        else
            action="unpublish"
            action_desc="下架"
        fi

        echo -n -e "${GREEN}--> 发起第 $i 次操作 (${action_desc})... ${NC}"
        response=$(curl -s -o /dev/null -w "%{http_code}" -X POST "${BASE_URL_ADMIN_PRODUCT}/${PRODUCT_ID}/${action}")

        if [[ "$response" == "200" ]]; then
            echo -e "${GREEN}成功。${NC}"
        else
            echo -e "${RED}失败 (HTTP: ${response})！${NC}"
        fi
        sleep 1
    done
    echo -e "\n${GREEN}产品服务负载均衡测试完成。${NC}"
}

# ... (test_circuit_breaker 和 test_query_fallback 函数保持不变) ...
function test_circuit_breaker() {
    print_header "2. 熔断与降级测试 (模拟写操作服务宕机)"
    echo "我们将模拟 'user-service' 全部宕机的情况。"
    echo -e "\n${YELLOW}--> 正在停止 user-service-1 和 user-service-2 容器...${NC}"
    docker compose stop user-service-1 user-service-2
    echo -e "${GREEN}user-service 实例已停止。等待几秒钟让Nacos更新服务列表...${NC}"
    sleep 10
    echo -e "\n${YELLOW}--> 再次尝试创建订单 (此时 user-service 不可用)...${NC}"
    echo "期望结果: order-service 的 UserClientFallback 会被触发，请求会立即失败并返回一个友好的错误信息 (例如：'用户服务暂时不可用...')"
    curl -i -X POST "${BASE_URL_ORDER}" \
         -H "Content-Type: application/json" \
         -d "{\"buyerId\": \"$USER_ID\", \"productId\": \"$PRODUCT_ID\", \"quantity\": 1}"
    echo -e "\n${GREEN}熔断降级测试完成。${NC}"
    echo -e "\n${YELLOW}--> 正在重启 user-service-1 和 user-service-2 以供后续测试...${NC}"
    docker compose start user-service-1 user-service-2
    echo -e "${GREEN}user-service 已重启。请等待约30秒让服务完全恢复并注册到Nacos。${NC}"
    sleep 30
}

function test_query_fallback() {
    print_header "3. 查询接口降级测试 (模拟读操作服务宕机)"
    echo "我们将模拟 'inventory-service' 全部宕机的情况来测试产品查询。"
    echo -e "\n${YELLOW}--> 正在停止 inventory-service-1 和 inventory-service-2 容器...${NC}"
    docker compose stop inventory-service-1 inventory-service-2
    echo -e "${GREEN}inventory-service 实例已停止。等待几秒钟...${NC}"
    sleep 10
    echo -e "\n${YELLOW}--> 尝试查询上架商品 (此时 inventory-service 不可用)...${NC}"
    echo "期望结果: product-service 的 InventoryClientFallback 会被触发，返回一个空的商品列表而不是抛出异常。"
    response=$(curl -s -X GET "${BASE_URL_PRODUCT}/on-shelf") # 假设 /on-shelf 是获取上架商品的接口
    echo "收到的响应:"
    echo "$response" | jq .
    if [[ "$response" == "[]" || $(echo "$response" | jq 'length') -eq 0 ]]; then
        echo -e "\n${GREEN}测试成功！成功降级并返回了空列表。${NC}"
    else
        echo -e "\n${RED}测试失败！未按预期返回空列表。${NC}"
    fi
    echo -e "\n${YELLOW}--> 正在重启 inventory-service-1 和 inventory-service-2...${NC}"
    docker compose start inventory-service-1 inventory-service-2
    echo -e "${GREEN}测试环境已恢复。${NC}"
}

# --- 主逻辑 ---

check_jq
prepare_test_data
read -p "测试数据准备就绪。按 Enter键 开始【负载均衡测试】..."
test_load_balancing
read -p "负载均衡测试完成。按 Enter键 继续进行【熔断降级测试】..."
test_circuit_breaker
read -p "熔断降级测试完成。按 Enter键 继续进行【查询降级测试】..."
test_query_fallback

print_header "所有自动化测试已执行完毕！"
