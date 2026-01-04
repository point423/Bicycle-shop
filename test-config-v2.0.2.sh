

URL="http://localhost:8081/api/config/current"

echo "正在请求: $URL ..."
echo "---------------------------------"

# 发送请求
# 如果安装了 jq (JSON格式化工具)，可以使用: curl -s $URL | jq
curl -s "$URL"

echo "" # 打印一个换行
echo "---------------------------------"
echo "请求结束"
