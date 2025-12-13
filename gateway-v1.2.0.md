## 🚀 核心架构升级：构建安全、统一的服务入口
- 在当前版本中，我们对系统架构进行了重大升级，引入了 Spring Cloud Gateway 作为所有微服务的统一入口 (gateway-service)。此举不仅统一了 API 的访问点，更重要的是，我们基于此构建了一套完整的、基于 JWT (JSON Web Token) 和 角色 (Role) 的认证与授权体系。
## ✨ 主要功能实现与验证
   ## 1. 统一 API 入口：gateway-service
       - 集中路由: 所有外部请求现在都通过 API 网关 (http://localhost:8090) 访问。网关根据请求路径（如 /api/users/**, /api/products/** 等），通过 Nacos 服务发现，动态地将请求路由到后端的相应微服务实例。
       - 简化客户端: 客户端（如前端应用或测试脚本）不再需要知道后端各个微服务的具体地址和端口，只需与网关这一个端点交互。
   ## 2. 认证体系：基于 JWT 的无状态认证 (Authentication)
       - 我们实现了一套标准的 JWT 认证流程，以保障受保护资源的安全性。
       - Token 获取: 用户通过调用登录接口 (POST /api/auth/login)，在提供了正确的用户名和密码后，会获取到一个包含其身份信息（如 UserID, Username, Role）并经过加密签名的 JWT。
       - 请求验证: 对于所有受保护的 API，客户端必须在 HTTP 请求的 Authorization 头中携带此 JWT (格式: Bearer <token>)。
       - 全局认证过滤: 网关层实现了一个全局过滤器，该过滤器会拦截所有传入的请求，检查 Authorization 头：
       如果 JWT 有效，请求将被放行至下游服务。
       如果 JWT 缺失、无效或已过期，请求将被网关直接拒绝，并返回 4.1 Unauthorized 状态码。
   ## 3. 授权体系：基于角色的精细化权限控制 (Authorization)
       - 在认证的基础上，我们进一步实现了基于角色的授权机制，确保了不同身份的用户只能访问其被允许的资源。
       - 角色定义: 系统中定义了至少两种角色：USER (普通用户) 和 ADMIN (管理员)。用户的角色信息被编码在 JWT 的载荷 (Payload) 中。
       - 权限区分:
       普通接口: 如创建订单 (POST /api/orders)，允许任何已认证的用户（无论是 USER 还是 ADMIN）访问。
       管理员专用接口: 如上架商品 (POST /api/admin/products/{id}/publish)，我们通过在网关层或服务层的安全配置，强制要求只有 JWT 中包含 ADMIN 角色的用户才能访问。
   ## ✅ 测试验证:
       - 使用 USER 角色的 JWT 尝试访问管理员接口时，系统正确返回 4.3 Forbidden 状态码，拒绝访问。
       - 使用 ADMIN 角色的 JWT 访问管理员接口时，请求成功通过，返回 200 OK。
   ## 4. 安全策略：精准的白名单放行机制
       - 为了在保障安全的同时不影响核心业务流程，我们配置了精准的 API 白名单。
       - 公开端点: 以下公共端点无需任何 JWT 认证即可访问：
       用户注册: POST /api/users
       商品创建: POST /api/products (注：根据业务需求，此接口也可被保护)
       用户登录: POST /api/auth/login
       受保护端点: 除上述白名单路径外，所有其他 API 端点均受到 JWT 认证和授权的保护。





![测试图1](./images/gateway-test.v1.2.0(1).png)
![测试图2](./images/gateway-test.v1.2.0(2).png)
![测试图3](./images/gateway-test.v1.2.0(3).png)