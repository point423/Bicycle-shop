$(document).ready(function() {

    // ================== 1. 全局配置和变量 ==================
    const GATEWAY_URL = 'http://localhost:8090';
    let jwtToken = null;
    let currentUser = null;
    let currentCategory = 'all';

    // 缓存所有已加载的商品信息
    let allProductsData = [];

    // 在这个模式下，cart 仅用于前端展示“已抢购数量”，实际订单已在后端生成
    let cart = {};

    let currentPage = 0;
    let isLoading = false;
    let allProductsLoaded = false;

    // Bootstrap 组件实例
    let loginModal = new bootstrap.Modal(document.getElementById('loginModal'));
    let registerModal = new bootstrap.Modal(document.getElementById('registerModal'));
    let imageZoomModal = new bootstrap.Modal(document.getElementById('image-zoom-modal'));
    let cartOffcanvas = new bootstrap.Offcanvas(document.getElementById('cart-offcanvas'));

    // ================== 2. 核心API调用函数 ==================
    function apiCall(path, method, data = null) {
        return $.ajax({
            url: GATEWAY_URL + path,
            method: method,
            contentType: 'application/json',
            data: data ? JSON.stringify(data) : null,
            beforeSend: function(xhr) {
                if (jwtToken) {
                    xhr.setRequestHeader('Authorization', 'Bearer ' + jwtToken);
                }
            }
        });
    }

    // ================== 3. 核心逻辑：加入即下单 (扣库存) ==================

    function findProductById(productId) {
        return allProductsData.find(p => p.id === productId);
    }

    /**
     * 局部更新页面上的库存显示，无需刷新整个列表
     */
    function updateProductStockDisplay(productId, change) {
        const product = findProductById(productId);
        if (product) {
            // 1. 更新 JS 内存数据
            product.stock += change;

            // 2. 更新 DOM 元素
            const stockEl = $(`#stock-${productId}`);
            const btnEl = $(`button[data-product-id="${productId}"]`);

            if (product.stock > 0) {
                stockEl.html(`库存: ${product.stock}`).attr('class', 'card-text text-success fw-bold');
            } else {
                stockEl.html(`已售罄`).attr('class', 'card-text text-muted');
                btnEl.prop('disabled', true).text('已售罄').removeClass('btn-primary').addClass('btn-secondary');
            }
        }
    }

    /**
     * 加入购物车逻辑（修改版）：
     * 直接发送 POST 请求创建订单，后端扣库存，成功后前端更新显示。
     */
    async function addToCart(productId) {
        // 1. 登录校验
        if (!jwtToken) {
            alert('请先登录！');
            loginModal.show();
            return;
        }

        const product = findProductById(productId);

        // 2. 前端预校验库存
        if (product && product.stock <= 0) {
            alert("手慢了，商品已售罄！");
            $(`button[data-product-id="${productId}"]`).prop('disabled', true).text('已售罄');
            return;
        }

        // 3. 准备订单数据
        // 注意：currentUser.id 必须存在，来源于登录/注册时的响应
        const orderData = {
            productId: productId,
            buyerId: currentUser.id,
            quantity: 1 // 每次点击买 1 个
        };

        // 4. UI 交互：按钮变更为“抢购中”
        const btn = $(`button[data-product-id="${productId}"]`);
        const originalText = btn.text();
        btn.prop('disabled', true).text('抢购中...');

        try {
            // ✨ 核心调用：创建订单 -> 后端 Service 扣库存
            await apiCall('/api/orders', 'POST', orderData);

            // 5. 成功后的处理

            // A. 更新前端库存显示 (减1)
            updateProductStockDisplay(productId, -1);

            // B. 更新前端购物车计数 (仅作展示用)
            cart[productId] = (cart[productId] || 0) + 1;
            updateCartUI();

            // C. 按钮反馈
            btn.text('已加入').removeClass('btn-primary').addClass('btn-success');
            setTimeout(() => {
                // 如果还有库存，恢复按钮状态
                if (findProductById(productId).stock > 0) {
                    btn.prop('disabled', false).text('加入购物车').removeClass('btn-success').addClass('btn-primary');
                }
            }, 1000);

        } catch (err) {
            console.error(err);
            // 错误处理：可能是库存不足，或者服务报错
            alert('抢购失败：库存不足或系统繁忙');
            btn.prop('disabled', false).text(originalText);

            // 如果是因为库存不足报错(400/500)，尝试刷新一下该商品状态
            // 这里简单处理：如果失败，假设没抢到
        }
    }

    /**
         * 退单逻辑：
         * 1. 查询用户的所有订单
         * 2. 找到对应 productId 的一个订单 (兼容 ACTIVE/active 状态)
         * 3. 调用 DELETE 删除该订单 (后端自动还库存)
         * 4. 前端库存 +1
         */
        async function cancelOrder(productId) {
                if (!jwtToken) return;

                // 1. 🔍【调试代码】打印看看当前用户ID到底是啥
                // 如果这里打印出来是 undefined，说明登录保存的信息不对
                const uid = currentUser.id || currentUser.userId;
                console.log("正在尝试退单...");
                console.log("当前用户对象:", currentUser);
                console.log("使用的用户ID (UID):", uid);

                if (!uid) {
                    alert("错误：无法获取用户ID，请重新登录");
                    return;
                }

                const btnDecrease = $(`.btn-decrease[data-product-id="${productId}"]`);
                btnDecrease.prop('disabled', true);

                try {
                    // 第一步：获取订单ID
                    // 注意：这里 URL 里的 uid 必须是 UUID 格式 (例如: 550e8400-e29b...)
                    console.log(`正在请求接口: GET ${GATEWAY_URL}/api/orders/user/${uid}`);

                    const userOrders = await apiCall(`/api/orders/user/${uid}`, 'GET');

                    // ... (中间的查找逻辑保持不变) ...
                    const targetOrder = userOrders.find(o =>
                        o.productId === productId &&
                        (o.status === 'ACTIVE' || o.status === 'active')
                    );

                    if (!targetOrder) {
                        alert("未找到该商品的有效订单，无法退单。");
                        delete cart[productId]; // 修正前端显示
                        updateCartUI();
                        return;
                    }

                    // 第二步：删除订单
                    console.log(`正在请求接口: DELETE ${GATEWAY_URL}/api/orders/${targetOrder.id}`);
                    await apiCall(`/api/orders/${targetOrder.id}`, 'DELETE');

                    // ... (更新 UI 逻辑保持不变) ...
                    updateProductStockDisplay(productId, 1);
                    cart[productId] = (cart[productId] || 1) - 1;
                    if (cart[productId] <= 0) delete cart[productId];
                    updateCartUI();

                    alert("退单成功！"); // 临时加个提示方便调试

                } catch (err) {
                    // ✨【关键修改】打印详细错误信息
                    console.error("❌ 退单失败详情:", err);
                    console.error("❌ 状态码 (Status):", err.status); // 404? 400? 500?
                    console.error("❌ 错误信息 (ResponseText):", err.responseText);

                    let msg = "未知错误";
                    if (err.status === 404) msg = "找不到接口 (404)：请检查后端 OrderController 是否添加了 /user/{id} 接口并重启了服务。";
                    if (err.status === 400) msg = "请求参数错误 (400)：可能是用户ID格式不对 (后端需要UUID)。";
                    if (err.status === 500) msg = "服务器内部错误 (500)：请检查后端控制台报错。";

                    alert(`退单失败: ${msg}`);
                } finally {
                    btnDecrease.prop('disabled', false);
                }
            }




    // 购物车面板内的加减操作
        function updateCartQuantity(productId, change) {
            if (change > 0) {
                // 点击加号 = 再次下单 (创建新订单)
                addToCart(productId);
            } else {
                // 点击减号 = 退单 (删除已有订单)
                // 只有当购物车里确实有数量时才执行
                if (cart[productId] > 0) {
                    if (confirm("确定要取消这件商品的一个订单并释放库存吗？")) {
                        cancelOrder(productId);
                    }
                }
            }
        }

    // 更新购物车面板 UI
    function updateCartUI() {
        const cartItemsContainer = $('#cart-items-container');
        const cartEmptyMessage = $('#cart-empty-message');
        const cartTotalPriceEl = $('#cart-total-price');

        cartItemsContainer.empty();
        const cartKeys = Object.keys(cart);

        if (cartKeys.length === 0) {
            cartEmptyMessage.show();
            cartItemsContainer.hide();
        } else {
            cartEmptyMessage.hide();
            cartItemsContainer.show();
        }

        let totalItems = 0;
        let totalPrice = 0;

        cartKeys.forEach(productId => {
            const quantity = cart[productId];
            const product = findProductById(productId);
            if (!product) return;

            totalItems += quantity;
            totalPrice += product.price * quantity;

            const itemHtml = `
                <div class="cart-item">
                    <img src="${GATEWAY_URL + product.imageUrl}" class="cart-item-img">
                    <div class="cart-item-details">
                        <h6 class="mb-0">${product.model}</h6>
                        <small class="text-muted">${product.brand}</small>
                        <p class="mb-0 fw-bold text-danger">¥${product.price.toFixed(2)}</p>
                    </div>
                    <div class="quantity-controls">
                        <button class="btn btn-sm btn-outline-secondary btn-decrease" data-product-id="${productId}">-</button>
                        <span class="mx-2">${quantity}</span>
                        <button class="btn btn-sm btn-outline-secondary btn-increase" data-product-id="${productId}">+</button>
                    </div>
                </div>
            `;
            cartItemsContainer.append(itemHtml);
        });

        // 更新右下角悬浮球的数量
        const cartBadge = $('.cart-badge');
        if (totalItems > 0) {
            cartBadge.text(totalItems).removeClass('d-none');
        } else {
            cartBadge.addClass('d-none');
        }
        cartTotalPriceEl.text(`¥${totalPrice.toFixed(2)}`);
    }
// ================== 提取公共的渲染卡片逻辑 ==================
    function renderProductCard(product) {
        // 处理图片路径
        let imageUrl = 'https://via.placeholder.com/400x300?text=No+Image';
        if (product.imageUrl) {
            imageUrl = product.imageUrl.startsWith('http') ? product.imageUrl : GATEWAY_URL + product.imageUrl;
        }

        // 处理库存显示
        // 注意：如果后端分类接口返回的是 Product 实体而非 DTO，可能没有 stock 字段
        // 这里做一个兼容处理，如果 stock 未定义，暂且视为有货或者显示 0
        const stock = product.stock !== undefined ? product.stock : 0;

        let stockHtml = '';
        let btnHtml = '';

        if (stock > 10) {
            stockHtml = `<p id="stock-${product.id}" class="card-text text-success fw-bold">库存: ${stock}</p>`;
            btnHtml = `<button class="btn btn-primary w-100 add-to-cart-btn" data-product-id="${product.id}">加入购物车</button>`;
        } else if (stock > 0) {
            stockHtml = `<p id="stock-${product.id}" class="card-text text-warning fw-bold">库存紧张: ${stock}</p>`;
            btnHtml = `<button class="btn btn-primary w-100 add-to-cart-btn" data-product-id="${product.id}">加入购物车</button>`;
        } else {
            stockHtml = `<p id="stock-${product.id}" class="card-text text-muted">已售罄</p>`;
            btnHtml = `<button class="btn btn-secondary w-100" disabled>已售罄</button>`;
        }

        const html = `
            <div class="col">
                <div class="card h-100 shadow-sm">
                    <div class="position-relative">
                        <img src="${imageUrl}" class="card-img-top product-image-zoomable" alt="${product.model}" style="cursor: pointer;">
                        <span class="position-absolute top-0 end-0 badge bg-dark m-2 opacity-75">${product.category}</span>
                    </div>
                    <div class="card-body d-flex flex-column">
                        <h5 class="card-title text-truncate" title="${product.brand} ${product.model}">${product.brand} - ${product.model}</h5>
                        <p class="card-text small text-muted mb-2">${product.color || '标准色'} | ${product.frameSize || '均码'}</p>
                        ${stockHtml}
                        <div class="mt-auto pt-3 d-flex justify-content-between align-items-center">
                            <span class="fs-5 text-danger fw-bold">¥${product.price.toFixed(2)}</span>
                        </div>
                    </div>
                    <div class="card-footer bg-transparent border-top-0 pb-3">
                        ${btnHtml}
                    </div>
                </div>
            </div>
        `;

        $('#product-list').append(html);

        // 将数据存入缓存，以便购物车逻辑使用 (防止 findProductById 找不到)
        if (!allProductsData.find(p => p.id === product.id)) {
            allProductsData.push(product);
        }
    }
    // ================== 4. 页面渲染函数 ==================
function loadProducts() {
    if (isLoading || allProductsLoaded) return;
    isLoading = true;
    $('#loading-indicator').removeClass('d-none');

    // ✨ 动态构建 URL
    let url = `/api/products?page=${currentPage}&size=6`;
    if (currentCategory !== 'all') {
        // 如果选中了分类，调用新的分类分页接口
        url = `/api/products/category/${encodeURIComponent(currentCategory)}?page=${currentPage}&size=6`;
    }

    apiCall(url, 'GET').done(function(pageData) {
        const products = pageData.content; // 现在分类接口也返回 Page 对象了，所以都有 .content

        if (!products || products.length === 0) {
            allProductsLoaded = true;
            if (currentPage === 0) {
                $('#product-list').html('<div class="w-100 text-center text-white py-5 opacity-75"><h4>🌿 该分类下暂无商品</h4></div>');
            }
            $('#loading-indicator').addClass('d-none'); // 没数据就隐藏加载条
            return;
        }

        products.forEach(product => {
            renderProductCard(product);
        });

        // 只有当返回数据少于每页大小时，才认为是最后一页
        if (products.length < 6) {
            allProductsLoaded = true;
            $('#loading-indicator').addClass('d-none');
        } else {
            currentPage++;
        }

    }).fail(function(err) {
        console.error('加载失败:', err);
        $('#product-list').html('<p class="text-center text-danger">加载失败，请刷新重试</p>');
    }).always(function() {
        isLoading = false;
        if (allProductsLoaded) $('#loading-indicator').addClass('d-none');
    });
}

// 修改分类点击事件
$(document).on('click', '.category-filter', function(e) {
    e.preventDefault();
    const category = $(this).data('category');
    const btnText = $(this).text(); // 获取带图标的文字

    // 1. 更新 UI
    $('#categoryDropdown').html(btnText);

    // 2. 更新全局状态
    currentCategory = category;

    // 3. 重置列表状态
    $('#product-list').empty();
    currentPage = 0;
    allProductsLoaded = false;

    // 4. 重新加载 (会读取 currentCategory 变量)
    loadProducts();
});

    // ================== 5. 用户认证与UI更新 ==================

    function updateUIAfterLogin() {
        if (!currentUser) return;
        $('#welcome-message').text(`欢迎, ${currentUser.username}!`).removeClass('d-none');
        $('#auth-button').addClass('d-none');
        $('#logout-button').removeClass('d-none');

        if (currentUser.role === 'ADMIN') {
            const manageBtn = $('#manage-product-btn');
            manageBtn.removeClass('d-none');
            manageBtn.off('click').on('click', function() {
                window.open('admin.html', '_blank');
            });
        }
        loginModal.hide();
        registerModal.hide();
    }

    function updateUIAfterLogout() {
        $('#welcome-message').addClass('d-none');
        $('#auth-button').removeClass('d-none');
        $('#logout-button').addClass('d-none');
        $('#manage-product-btn').addClass('d-none');
        cart = {}; // 清空本地显示
        updateCartUI();
    }

    function checkLoginStatus() {
        const storedToken = localStorage.getItem('jwtToken');
        const storedUser = localStorage.getItem('currentUser');
        if (storedToken && storedUser) {
            jwtToken = storedToken;
            currentUser = JSON.parse(storedUser);
            updateUIAfterLogin();
        }
    }

    // ================== 6. 事件绑定 ==================

    // 无限滚动
    $(window).on('scroll', function() {
        if (!isLoading && !allProductsLoaded && $(window).scrollTop() + $(window).height() >= $(document).height() - 200) {
            loadProducts();
        }
    });

    // 动态事件委托
    $(document).on('click', '.add-to-cart-btn', function() {
        const productId = $(this).data('product-id');
        addToCart(productId);
    });

    $(document).on('click', '.product-image-zoomable', function() {
        const imageUrl = $(this).attr('src');
        $('#zoomed-image').attr('src', imageUrl);
        imageZoomModal.show();
    });

    $(document).on('click', '.btn-increase', function() {
        const productId = $(this).data('product-id');
        updateCartQuantity(productId, 1);
    });

    $(document).on('click', '.btn-decrease', function() {
        const productId = $(this).data('product-id');
        updateCartQuantity(productId, -1);
    });

    // 弹窗切换
    $('#show-register-link').on('click', function(e) {
        e.preventDefault();
        loginModal.hide();
        registerModal.show();
    });

    $('#show-login-link').on('click', function(e) {
        e.preventDefault();
        registerModal.hide();
        loginModal.show();
    });

    // 注册逻辑
    $('#register-form').on('submit', function(e) {
        e.preventDefault();
        const username = $('#register-username').val();
        const password = $('#register-password').val();
        const phone = $('#register-phone').val();
        const age = parseInt($('#register-age').val(), 10);
        const isAdmin = $('#register-is-admin').is(':checked');
        const role = isAdmin ? 'ADMIN' : 'USER';

        // 生成唯一ID
        const userId = 'user-' + Date.now() + '-' + Math.floor(Math.random() * 1000);

        const registrationData = {
            userId: userId,
            username: username,
            password: password,
            phone: phone,
            age: age,
            role: role
        };

        apiCall('/api/users', 'POST', registrationData)
            .done(function() {
                alert('注册成功！将自动为您登录。');
                // 注册成功后自动登录
                apiCall('/api/auth/login', 'POST', { username, password })
                    .done(function(loginResponse) {
                        jwtToken = loginResponse.token;
                        currentUser = loginResponse.user;
                        localStorage.setItem('jwtToken', jwtToken);
                        localStorage.setItem('currentUser', JSON.stringify(currentUser));
                        updateUIAfterLogin();
                    });
            })
            .fail(function(err) {
                const errorMsg = err.responseJSON?.message || err.responseText || '未知错误';
                alert('注册失败: ' + errorMsg);
            });
    });

    // 登录逻辑
    $('#login-form').on('submit', function(e) {
        e.preventDefault();
        const username = $('#login-username').val();
        const password = $('#login-password').val();

        apiCall('/api/auth/login', 'POST', { username, password })
            .done(function(response) {
                jwtToken = response.token;
                currentUser = response.user;
                localStorage.setItem('jwtToken', jwtToken);
                localStorage.setItem('currentUser', JSON.stringify(currentUser));
                updateUIAfterLogin();
                alert('登录成功！');
            })
            .fail(function(err) {
                const errorMsg = err.responseJSON?.message || err.responseText || '未知错误';
                alert('登录失败: ' + errorMsg);
            });
    });

    // 登出逻辑
    $('#logout-button').on('click', function() {
        jwtToken = null;
        currentUser = null;
        localStorage.removeItem('jwtToken');
        localStorage.removeItem('currentUser');
        updateUIAfterLogout();
        alert('您已退出登录。');
    });

    // ================== 页面初始化 ==================
    checkLoginStatus();
    loadProducts();

});