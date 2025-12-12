## 🚀 核心架构升级：集成 Nacos 实现服务治理
- 在当前版本中，我们对整个微服务架构进行了一升级，全面引入了 Nacos 作为服务注册与发现中心。这一变革使我们的系统从静态的服务间硬编码地址调用，演进为动态的、自动化的服务治理模式。
所有微服务（user-service, product-service, order-service, inventory-service）现在都会在启动时自动向 Nacos Server 注册自身实例，并通过 Nacos 发现其他服务的网络位置。
## ✨ 主要功能实现与验证
  ## 1. 启用服务端负载均衡
   - 通过在 RestTemplate Bean 上添加 @LoadBalanced 注解，我们激活了 Spring Cloud 的客户端负载均衡能力。现在，服务间的调用不再需要指定具体的 IP 和端口，而是直接使用在 Nacos 中注册的服务名（如 http://user-service/...）。
   - RestTemplate 会自动查询 Nacos，获取目标服务所有健康实例的列表，并默认采用轮询 (Round-robin) 策略将请求分发到不同的实例上。
  ## 2. 实现并验证 order-service 的负载均衡调用
   - order-service 作为核心业务流程的协调者，现在能够动态地与多个下游服务的实例进行交互：
   - 调用 user-service:
   - 在创建订单前，order-service 会调用 user-service 的接口来验证用户是否存在。
   - 验证结果: 测试表明，当连续创建多个订单时，order-service 的日志清晰地显示，其对 user-service 的请求被轮流分发到了后端的两个 user-service 实例上。
   - 调用 inventory-service:
   - 在创建订单时，order-service 调用 inventory-service 以扣减库存。
   - 在取消订单时，order-service 调用 inventory-service 以回滚库存。
   - 验证结果: 通过日志观察，order-service 对 inventory-service 的库存操作请求，同样被成功地负载均衡到了后端的两个 inventory-service 实例。
  ## 3. 实现并验证 product-service 的负载均衡调用
   - product-service 同样集成了负载均衡能力，以实现与 inventory-service 的解耦：
   - 调用 inventory-service:
   - 当一个新商品被创建时，product-service 会调用 inventory-service 为其创建初始库存记录。
   - 当管理员上架/下架商品时，product-service 调用 inventory-service 以更新商品的可销售状态。
   - 验证结果: 测试日志确认，product-service 对 inventory-service 的所有调用都通过 Nacos 进行了服务发现，并成功地实现了跨实例的负载均衡。
##  ✅ 测试结论：验证通过

![测试图1](./images/nacos-test.v1.1.0(1).png)
![测试图2](./images/nacos-test.v1.1.0(2).png)
![测试图3](./images/nacos-test.v1.1.0(3).png)
![测试图4](./images/nacos-test.v1.1.0(4).png)
![测试图5](./images/nacos-test.v1.1.0(5).png)
![测试图6](./images/nacos-test.v1.1.0(6).png)
![测试图7](./images/nacos-test.v1.1.0(7).png)