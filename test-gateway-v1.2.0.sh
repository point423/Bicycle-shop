# ==============================================================================
# 微服务认证与授权(AuthN & AuthZ) 端到端自动化测试脚本
#
# 功能:
# 1. 自动注册一个普通用户 (USER) 和一个管理员 (ADMIN)。
# 2. 自动为这两个用户登录，并分别获取和保存他们的JWT。
# 3. 模拟三种场景（无JWT, 普通用户JWT, 管理员JWT）访问普通受保护接口。
# 4. 模拟三种场景访问管理员专用接口，验证权限控制。
# ==============================================================================

# --- 配置 ---
GATEWAY_URL="http://localhost:8090"
CURL_OPTS="-s -i" # -s: 静默模式, -i: 显示响应头

# 用于存储获取到的JWT，无需修改
NORMAL_USER_JWT=""
ADMIN_USER_JWT=""

# 用于存储创建的实体ID，无需修改
NORMAL_USER_ID=""
ADMIN_USER_ID=""
PRODUCT_ID=""


# --- 颜色和辅助函数 ---
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m'

# 打印带有边框的标题
print_header() {
    echo -e "\n${BLUE}======================================================================${NC}"
    echo -e "${BLUE}  $1${NC}"
    echo -e "${BLUE}======================================================================${NC}"
}

# 检查jq是否安装
check_jq() {
    if ! command -v jq &> /dev/null; then
        echo -e "${RED}错误: 'jq' 命令未找到。请先安装它 (e.g., 'sudo apt install jq').${NC}"
        exit 1
    fi
}


# ==============================================================================
# 步骤 1: 【注册】创建新用户和商品 (V2 - 使用更详细的用户属性)
# ==============================================================================
function register_users_and_product() {
    print_header "步骤 1: 注册一个普通用户和一个管理员，并创建一个商品"

    # --- 注册普通用户 (USER) ---
    # 使用时间戳和随机数确保每次生成的ID和用户名都是唯一的
     UNIQUE_SUFFIX_USER="$(date +%s)-${RANDOM}"
     UNIQUE_USER_ID_USER="user-${UNIQUE_SUFFIX_USER}"
     UNIQUE_USERNAME_USER="test-user-${UNIQUE_SUFFIX_USER}"
    # 使用时间戳的纳秒部分生成随机手机号
     UNIQUE_PHONE_USER="138$(date +%N | cut -c -8)"

    echo -e "${YELLOW}--> 正在注册普通用户 (USER) [使用详细属性]...${NC}"

    user_response=$(curl -s -X POST "${GATEWAY_URL}/api/users" \
        -H "Content-Type: application/json" \
        -d '{
              "userId": "'"${UNIQUE_USER_ID_USER}"'",
              "username": "'"${UNIQUE_USERNAME_USER}"'",
              "password": "password123",
              "email": "'"${UNIQUE_USERNAME_USER}"'@example.com",
              "phone": "'"${UNIQUE_PHONE_USER}"'",
              "age": 25,
              "role": "USER"
            }')

    # 从响应体中提取数据库生成的ID（通常是UUID）
    NORMAL_USER_ID=$(echo "$user_response" | jq -r '.id')
    if [[ -z "$NORMAL_USER_ID" || "$NORMAL_USER_ID" == "null" ]]; then
        echo -e "${RED}注册普通用户失败! 响应: $user_response${NC}"; exit 1;
    fi
    echo -e "${GREEN}普通用户注册成功. DB-ID: ${NORMAL_USER_ID}${NC}"

    # --- 注册管理员 (ADMIN) ---
     UNIQUE_SUFFIX_ADMIN="$(date +%s)-${RANDOM}"
     UNIQUE_USER_ID_ADMIN="admin-${UNIQUE_SUFFIX_ADMIN}"
     UNIQUE_USERNAME_ADMIN="test-admin-${UNIQUE_SUFFIX_ADMIN}"
     UNIQUE_PHONE_ADMIN="158$(date +%N | cut -c -8)"

    echo -e "\n${YELLOW}--> 正在注册管理员 (ADMIN) [使用详细属性]...${NC}"

    admin_response=$(curl -s -X POST "${GATEWAY_URL}/api/users" \
        -H "Content-Type: application/json" \
        -d '{
              "userId": "'"${UNIQUE_USER_ID_ADMIN}"'",
              "username": "'"${UNIQUE_USERNAME_ADMIN}"'",
              "password": "password123",
              "email": "'"${UNIQUE_USERNAME_ADMIN}"'@example.com",
              "phone": "'"${UNIQUE_PHONE_ADMIN}"'",
              "age": 40,
              "role": "ADMIN"
            }')

    ADMIN_USER_ID=$(echo "$admin_response" | jq -r '.id')
    if [[ -z "$ADMIN_USER_ID" || "$ADMIN_USER_ID" == "null" ]]; then
        echo -e "${RED}注册管理员失败! 响应: $admin_response${NC}"; exit 1;
    fi
    echo -e "${GREEN}管理员注册成功. DB-ID: ${ADMIN_USER_ID}${NC}"

    # --- 创建一个商品用于后续测试 ---
    # (此部分保持不变)
    echo -e "\n${YELLOW}--> 正在创建测试商品...${NC}"
    local product_response
    product_response=$(curl -s -X POST "${GATEWAY_URL}/api/products" \
        -H "Content-Type: application/json" \
        -d '{
              "brand": "TestBrand", "category": "Test", "model": "Model-X", "price": 10000
            }')
    PRODUCT_ID=$(echo "$product_response" | jq -r '.id')
    if [[ -z "$PRODUCT_ID" || "$PRODUCT_ID" == "null" ]]; then
        echo -e "${RED}创建商品失败! 响应: $product_response${NC}"; exit 1;
    fi
    echo -e "${GREEN}测试商品创建成功. ProductID: ${PRODUCT_ID}${NC}"



}



# ==============================================================================
# 步骤 2: 【登录】获取JWT (V2 - 使用唯一的用户名)
# ==============================================================================
function login_and_get_tokens() {
    print_header "步骤 2: 分别登录，获取普通用户和管理员的JWT"

    # --- 普通用户登录 ---
    echo -e "${YELLOW}--> 普通用户登录中...${NC}"
    local user_login_response
    user_login_response=$(curl -s -X POST "${GATEWAY_URL}/api/auth/login" \
        -H "Content-Type: application/json" \
        -d '{
              "username": "'"${UNIQUE_USERNAME_USER}"'",
              "password": "password123"
            }')
    NORMAL_USER_JWT=$(echo "$user_login_response" | jq -r '.token')
    if [[ -z "$NORMAL_USER_JWT" || "$NORMAL_USER_JWT" == "null" ]]; then
        echo -e "${RED}普通用户登录失败! 响应: $user_login_response${NC}"; exit 1;
    fi
    echo -e "${GREEN}普通用户登录成功，JWT已保存。${NC}"

    # --- 管理员登录 ---
    echo -e "\n${YELLOW}--> 管理员登录中...${NC}"
    local admin_login_response
    admin_login_response=$(curl -s -X POST "${GATEWAY_URL}/api/auth/login" \
        -H "Content-Type: application/json" \
        -d '{
              "username": "'"${UNIQUE_USERNAME_ADMIN}"'",
              "password": "password123"
            }')
    ADMIN_USER_JWT=$(echo "$admin_login_response" | jq -r '.token')
    if [[ -z "$ADMIN_USER_JWT" || "$ADMIN_USER_JWT" == "null" ]]; then
        echo -e "${RED}管理员登录失败! 响应: $admin_login_response${NC}"; exit 1;
    fi
    echo -e "${GREEN}管理员登录成功，JWT已保存。${NC}"
}



# ==============================================================================
# 步骤 3: 【访问受保护资源】
# ==============================================================================

# --- 3.1 测试访问普通接口 (e.g., 创建订单) ---
function test_normal_endpoint() {

    print_header "步骤 3.1: 测试访问普通受保护接口 (/api/orders)"
    local ORDER_URL="${GATEWAY_URL}/api/orders"
    local ORDER_PAYLOAD="-H \"Content-Type: application/json\" -d '{\"buyerId\": \"${NORMAL_USER_ID}\", \"productId\": \"${PRODUCT_ID}\", \"quantity\": 1}'"

# 【✅ 关键修改 ✅】: 新增增加库存的步骤
    echo -e "\n${YELLOW}--> 场景 0: 使用管理员JWT为商品增加初始库存 (100)...${NC}"
    local increase_response
    increase_response=$(curl -s -o /dev/null -w "%{http_code}" -X POST "${GATEWAY_URL}/api/inventorys/increase" \
        -H "Content-Type: application/json" \
        -H "Authorization: Bearer ${ADMIN_USER_JWT}" \
        -d "{\"productId\": \"${PRODUCT_ID}\", \"quantity\": 100}")

    if [[ "$increase_response" != "200" ]]; then
        echo -e "${RED}错误: 增加初始库存失败! HTTP状态码: ${increase_response}${NC}"
        # 打印详细错误以供调试
        curl -i -sS -X POST "${GATEWAY_URL}/api/inventorys/increase" \
            -H "Content-Type: application/json" \
            -H "Authorization: Bearer ${ADMIN_USER_JWT}" \
            -d "{\"productId\": \"${PRODUCT_ID}\", \"quantity\": 100}"
        exit 1
    fi
    echo -e "${GREEN}初始库存增加成功。${NC}"

    echo -e "\n${YELLOW}--> 场景 A: 不带JWT访问... (预期: 401 Unauthorized)${NC}"
    eval "curl ${CURL_OPTS} -X POST ${ORDER_URL} ${ORDER_PAYLOAD}"

    echo -e "\n${YELLOW}--> 场景 B: 带普通用户的JWT访问... (预期: 2xx/5xx 成功转发或因业务逻辑失败)${NC}"
    eval "curl ${CURL_OPTS} -X POST ${ORDER_URL} -H \"Authorization: Bearer ${NORMAL_USER_JWT}\" ${ORDER_PAYLOAD}"

    echo -e "\n${YELLOW}--> 场景 C: 带管理员的JWT访问... (预期: 2xx/5xx 成功转发或因业务逻辑失败)${NC}"
    eval "curl ${CURL_OPTS} -X POST ${ORDER_URL} -H \"Authorization: Bearer ${ADMIN_USER_JWT}\" ${ORDER_PAYLOAD}"
}


# --- 3.2 测试访问管理员接口 (e.g., 上架商品) ---
function test_admin_endpoint() {
    print_header "步骤 3.2: 测试访问管理员专用接口 (/api/admin/products/{id}/publish)"
    local ADMIN_URL="${GATEWAY_URL}/api/admin/products/${PRODUCT_ID}/publish"

    echo -e "\n${YELLOW}--> 场景 A: 不带JWT访问... (预期: 401 Unauthorized)${NC}"
    curl ${CURL_OPTS} -X POST "${ADMIN_URL}"

    echo -e "\n${YELLOW}--> 场景 B: 带普通用户的JWT访问... (预期: 403 Forbidden)${NC}"
    curl ${CURL_OPTS} -X POST "${ADMIN_URL}" -H "Authorization: Bearer ${NORMAL_USER_JWT}"

    echo -e "\n${YELLOW}--> 场景 C: 带管理员的JWT访问... (预期: 200 OK)${NC}"
    curl ${CURL_OPTS} -X POST "${ADMIN_URL}" -H "Authorization: Bearer ${ADMIN_USER_JWT}"
}


# ==============================================================================
# 主执行逻辑
# ==============================================================================
check_jq
register_users_and_product
login_and_get_tokens

read -p "数据和JWT准备就绪。按 Enter 键开始【测试普通接口】..."
test_normal_endpoint

read -p "普通接口测试完成。按 Enter 键开始【测试管理员接口】..."
test_admin_endpoint

print_header "所有测试已执行完毕！"

