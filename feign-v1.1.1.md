## 🚀 核心架构升级：服务调用与容错机制的现代化改造
- 在当前版本中，我们对微服务间的通信和稳定性保障机制进行了全面的现代化升级。核心举措包括：
- 引入 OpenFeign: 替代传统的 RestTemplate，实现声明式、更优雅的服务间调用。
- 集成 Resilience4j: 引入更轻量、更灵活的熔断、降级等容错模式。
- 验证 Spring Cloud LoadBalancer: 结合 Nacos，对服务端的负载均衡能力进行了系统化测试。
## ✨ 主要功能实现与验证
   ## 1. 服务调用升级：全面迁移至 OpenFeign
    - 我们重构了所有需要进行服务间通信的模块（如 order-service, product-service），使用 OpenFeign 替代了原有的 RestTemplate 调用。
    - 声明式客户端: 通过定义简单的 Java 接口并添加 @FeignClient 注解，我们将 HTTP API 调用抽象成了本地方法调用，极大地提升了代码的可读性和可维护性。
    - 类型安全: 所有请求和响应都与强类型的 DTO (Data Transfer Object) 绑定，避免了手动处理 Map 或 String 带来的潜在错误。
    - 配置简化: OpenFeign 自动集成了日志、编码器/解码器等功能，简化了 HTTP 客户端的配置工作。
   ## 2. 服务容错能力：集成 Resilience4j 实现熔断与降级
    - 为了提升系统的整体弹性和稳定性，我们为 Feign 客户端集成了 Resilience4j 作为容错解决方案。
   写操作熔断 (Circuit Breaker):
   场景: 在创建订单时，order-service 对 user-service 的写操作（用户校验）被配置了熔断器。
   测试: 我们通过手动停止所有 user-service 实例来模拟服务不可用。
   ✅ 验证通过: 测试结果显示，在连续调用失败达到阈值后，熔断器成功“打开”。后续的创单请求会立即失败并由 Feign 的 Fallback 机制捕获，快速返回一个友好的错误提示（如“用户服务暂时不可用”），避免了长时间的请求等待和资源堆积。
   读操作降级 (Fallback):
   场景: 在查询上架商品时，product-service 对 inventory-service 的读操作（获取上架ID列表）配置了降级逻辑。
   测试: 我们通过手动停止所有 inventory-service 实例来模拟服务故障。
   ✅ 验证通过: 测试结果显示，当 inventory-service 不可用时，product-service 的 InventoryClientFallback 成功被触发。查询上架商品的接口没有抛出异常，而是平滑地降级，返回一个空的商品列表 []。这保证了即使部分下游服务出现问题，核心的查询功能依然可用，提升了用户体验。
   ## 3. 服务发现与负载均衡 (Load Balancing) 验证
    - 我们对 Nacos + OpenFeign (内置 Spring Cloud LoadBalancer) 提供的服务端负载均衡能力进行了全面的端到端测试。
   测试方法:
   为 user-service 和 inventory-service 部署了多个实例。
   改造了这些服务的接口，使其在响应中能返回处理请求的实例端口号.
   编写自动化测试脚本，连续、多次地调用上游服务（order-service, product-service）。
   ✅ 验证通过:
   通过观察上游服务（调用方）的控制台日志，我们能够明确地看到，对于同一个服务（如 user-service）的多次连续调用，请求被成功地、轮流地分发到了后端的不同实例上。
   这有力地证明了 Nacos 的服务注册与发现机制以及 Spring Cloud LoadBalancer 的轮询策略正在按预期正常工作。


![测试图1](./images/feign-test.v1.1.1(1).png)
![测试图2](./images/feign-test.v1.1.1(2).png)
![测试图3](./images/feign-test.v1.1.1(3).png)
![测试图4](./images/feign-test.v1.1.1(4).png)
![测试图5](./images/feign-test.v1.1.1(5).png)
![测试图6](./images/feign-test.v1.1.1(6).png)
