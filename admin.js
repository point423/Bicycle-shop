// admin.js

const GATEWAY_URL = 'http://localhost:8090';
let jwtToken = localStorage.getItem('jwtToken');
let currentUser = JSON.parse(localStorage.getItem('currentUser') || '{}');

// 缓存数据
let productsCache = [];
let productModalInstance;

// ================= 初始化 =================
$(document).ready(function() {
    if (!jwtToken || currentUser.role !== 'ADMIN') {
        alert('权限不足，请以管理员身份登录！');
        window.location.href = 'index.html';
        return;
    }
    productModalInstance = new bootstrap.Modal(document.getElementById('productModal'));
    switchSection('products');
});

// ================= 通用工具 =================
function switchSection(section) {
    $('.nav-link').removeClass('active');
    $(`#nav-${section}`).addClass('active');
    $('.content-section').hide();
    $(`#section-${section}`).fadeIn();

    if (section === 'products') loadProducts();
    if (section === 'users') loadUsers();
    if (section === 'orders') loadOrders();
}

function adminApiCall(endpoint, method = 'GET', data = null) {
    return $.ajax({
        url: GATEWAY_URL + endpoint,
        method: method,
        contentType: 'application/json',
        data: data ? JSON.stringify(data) : null,
        headers: { 'Authorization': 'Bearer ' + jwtToken },
        error: function(xhr) {
            if (xhr.status === 403 || xhr.status === 401) {
                alert('会话已过期，请重新登录');
                window.location.href = 'index.html';
            } else {
                console.error(`API Error [${method} ${endpoint}]:`, xhr);
            }
        }
    });
}

// ================= 1. 商品管理逻辑 (核心修改部分) =================

// 加载所有商品 (聚合 Inventory 数据)
async function loadProducts() {
    $('#admin-product-list').html('<div class="text-center w-100 mt-5"><div class="spinner-border text-success"></div></div>');

    try {
        // 1. 获取基础商品列表
        const productsPromise = adminApiCall('/api/admin/products/all', 'GET');

        // 2. 获取已上架的 ID 列表 (从 Inventory 服务)
        const onShelfPromise = adminApiCall('/api/inventorys/on-shelf-product-ids', 'GET');

        // 并行执行请求
        const [products, onShelfResponse] = await Promise.all([productsPromise, onShelfPromise]);

        // 解析上架 ID 列表 (注意后端返回格式是 { data: [...], port: ... })
        const onShelfIds = onShelfResponse.data || [];

        // 3. (可选) 如果需要精准库存，可以再调一次批量库存接口，这里假设 products 里带了或者我们只关心状态
        // 为了确保库存显示正确，我们可以收集所有 ID 再查一次库存
        const productIds = products.map(p => p.id);
        let stockMap = {};
        if(productIds.length > 0) {
             stockMap = await adminApiCall('/api/inventorys/stocks', 'POST', productIds);
        }

        productsCache = products;
        const container = $('#admin-product-list');
        container.empty();

        if (products.length === 0) {
            container.html('<div class="alert alert-warning w-100">暂无商品数据</div>');
            return;
        }

        products.forEach(p => {
            // ✨ 核心修改：判断上架状态
            // 现在 Product 实体没 onShelf 了，我们需要看 p.id 是否在 onShelfIds 数组里
            const isOnShelf = onShelfIds.includes(p.id);

            // 把状态存回 p 对象，方便编辑时回显
            p.realOnShelf = isOnShelf;

            // 获取最新库存
            const realStock = stockMap[p.id] !== undefined ? stockMap[p.id] : (p.stock || 0);
            p.realStock = realStock;

            // 状态徽章
            const badgeHtml = isOnShelf
                ? '<span class="badge bg-success status-badge">已上架</span>'
                : '<span class="badge bg-secondary status-badge">未上架</span>';

            const imgUrl = p.imageUrl ? (GATEWAY_URL + p.imageUrl) : 'https://via.placeholder.com/300x200?text=No+Image';

            // ✨ 核心修改：增加了颜色显示 ${p.color || '-'}
            const html = `
                <div class="col">
                    <div class="card h-100 admin-product-card" onclick="openProductModal('${p.id}')" style="cursor: pointer;">
                        <div class="position-relative">
                            <img src="${imgUrl}" class="card-img-top admin-product-img" alt="${p.model}">
                            ${badgeHtml}
                        </div>
                        <div class="card-body">
                            <h6 class="card-title fw-bold text-dark">${p.brand} ${p.model}</h6>
                            <div class="d-flex justify-content-between align-items-center mt-2">
                                <span class="text-danger fw-bold">¥${p.price}</span>
                                <small class="text-muted">库存: ${realStock}</small>
                            </div>
                            <div class="mt-2 small text-muted">
                                <div><i class="bi bi-palette"></i> 颜色: ${p.color || '无'}</div>
                                <div><i class="bi bi-bicycle"></i> ${p.category} | ${p.frameSize || '-'}</div>
                            </div>
                        </div>
                    </div>
                </div>
            `;
            container.append(html);
        });

    } catch (err) {
        console.error("Load products failed", err);
        $('#admin-product-list').html('<div class="alert alert-danger">加载失败，请检查 Inventory 服务是否启动</div>');
    }
}

// 打开商品 Modal
function openProductModal(productId = null) {
    const modalTitle = $('#productModalLabel');
    const deleteBtn = $('#btn-delete-product');

    $('#productForm')[0].reset();

    if (productId) {
        const p = productsCache.find(x => x.id === productId);
        if (!p) return;

        modalTitle.text(`编辑商品: ${p.model}`);
        deleteBtn.show().attr('onclick', `deleteProduct('${p.id}')`);

        $('#p-id').val(p.id);
        $('#p-brand').val(p.brand);
        $('#p-model').val(p.model);
        $('#p-category').val(p.category);
        $('#p-price').val(p.price);

        // ✨ 使用 loadProducts 里获取到的真实库存和状态
        $('#p-stock').val(p.realStock).data('old-stock', p.realStock);
        $('#p-onShelf').prop('checked', p.realOnShelf);

        $('#p-frameSize').val(p.frameSize);
        $('#p-color').val(p.color);
        $('#p-gearSystem').val(p.gearSystem);
        $('#p-imageUrl').val(p.imageUrl);

    } else {
        modalTitle.text("新增商品");
        deleteBtn.hide();
        $('#p-id').val('');
        $('#p-stock').val(10);
        $('#p-onShelf').prop('checked', false);
    }

    productModalInstance.show();
}

// 保存商品 (逻辑修正：分别调用不同服务)
async function saveProduct() {
    const id = $('#p-id').val();
    const isOnShelf = $('#p-onShelf').is(':checked');
    const newStock = parseInt($('#p-stock').val());

    // 1. 基本信息 (不包含 onShelf, 因为 Product 实体没这个字段)
    const productData = {
        id: id || null,
        brand: $('#p-brand').val(),
        model: $('#p-model').val(),
        category: $('#p-category').val(),
        price: parseFloat($('#p-price').val()),
        frameSize: $('#p-frameSize').val(),
        color: $('#p-color').val(),
        gearSystem: $('#p-gearSystem').val(),
        imageUrl: $('#p-imageUrl').val()
    };

    if (!productData.brand || !productData.model || isNaN(productData.price)) {
        alert("请填写必填项 (品牌, 型号, 价格)");
        return;
    }

    try {
        if (id) {
            // === 更新逻辑 ===
            console.log("1. 更新商品基本信息...");
            await adminApiCall(`/api/admin/products/${id}`, 'PUT', productData);

            // ✨ 2. 更新库存 (调用 Inventory Service)
            // 获取缓存中的旧数据进行比对
            const oldP = productsCache.find(x => x.id === id);
            if (oldP && oldP.realStock !== newStock && !isNaN(newStock)) {
                console.log(`2. 更新库存: ${newStock}`);
                const inventoryRequest = { productId: id, quantity: newStock };
                await adminApiCall('/api/inventorys/admin/stock', 'PUT', inventoryRequest);
            }

            // ✨ 3. 更新上下架状态 (调用 Inventory Service)
            // 注意：你的 InventoryController 接口是 PUT /api/inventorys/{id}/on-shelf?onShelf=true
            if (oldP && oldP.realOnShelf !== isOnShelf) {
                console.log(`3. 更新上架状态: ${isOnShelf}`);
                await adminApiCall(`/api/inventorys/${id}/on-shelf?onShelf=${isOnShelf}`, 'PUT');
            }

            alert("商品更新成功！");

        } else {
            // === 新增逻辑 ===
            console.log("1. 创建新商品...");
            const createdProduct = await adminApiCall('/api/products', 'POST', productData);

            // 2. 初始化库存和上架状态
            if (createdProduct && createdProduct.id) {
                // 设置库存 (调用 create 接口)
                // 你的 InventoryController 有 create 接口: POST /api/inventorys/create
                if (!isNaN(newStock) && newStock > 0) {
                                    console.log(`更新初始库存: ID=${createdProduct.id}, 数量=${newStock}`);

                                    const inventoryRequest = {
                                        productId: createdProduct.id,
                                        quantity: newStock
                                    };

                                    // 使用 PUT 更新接口
                                    await adminApiCall('/api/inventorys/admin/stock', 'PUT', inventoryRequest);
                }

                // 如果勾选了上架，执行上架操作
                if (isOnShelf) {
                    console.log("3. 执行上架...");
                    await adminApiCall(`/api/inventorys/${createdProduct.id}/on-shelf?onShelf=true`, 'PUT');
                }
            }
            alert("商品创建成功！");
        }

        productModalInstance.hide();
        loadProducts(); // 重新加载以刷新数据

    } catch (err) {
        console.error("Save failed:", err);
        let msg = "未知错误";
        if (err.responseText) {
            try {
                const res = JSON.parse(err.responseText);
                msg = res.message || res.error || err.responseText;
            } catch(e) { msg = err.responseText; }
        }
        alert("操作失败: " + msg);
    }
}

// 删除商品
function deleteProduct(id) {
    if (!confirm('确定要删除该商品吗？')) return;

    // 这里的删除可能不够彻底，理想情况下应该同时删除商品和库存
    // 先删商品
    adminApiCall(`/api/admin/products/${id}`, 'DELETE')
        .done(async function() {
            // 再尝试删库存 (如果你的 InventoryController 有 delete 接口)
             try {
                 await adminApiCall(`/api/inventorys/${id}`, 'DELETE');
             } catch(e) { console.warn("库存删除失败或接口不存在"); }

            alert('删除成功');
            productModalInstance.hide();
            loadProducts();
        })
        .fail(function() { alert('删除失败'); });
}

// ... Users 和 Orders 的代码保持不变 (记得加上你之前已经修复好的 loadUsers 和 loadOrders) ...
// ================= 2. 用户管理逻辑 =================
function loadUsers() {
    const tbody = $('#admin-user-list');
    tbody.html('<tr><td colspan="6" class="text-center">加载中...</td></tr>');

    adminApiCall('/api/admin/users/all', 'GET').done(function(users) {
        tbody.empty();
        if (users.length === 0) {
            tbody.html('<tr><td colspan="6" class="text-center">暂无用户</td></tr>');
            return;
        }

        users.forEach(u => {
            const roleBadge = u.role === 'ADMIN'
                ? '<span class="badge bg-danger">管理员</span>'
                : '<span class="badge bg-info text-dark">用户</span>';

            tbody.append(`
                <tr>
                    <td><span class="text-muted" style="font-family:monospace; font-size:0.8rem;">${u.id}</span></td>
                    <td class="fw-bold">${u.username}</td>
                    <td>${u.phone || '-'}</td>
                    <td>${roleBadge}</td>
                    <td><code class="text-muted">******</code></td>
                    <td>
                        <button class="btn btn-sm btn-outline-danger" onclick="deleteUser('${u.id}')">
                            <i class="bi bi-trash"></i> 删除
                        </button>
                    </td>
                </tr>
            `);
        });
    });
}
function deleteUser(id) {
    if (!confirm('警告：确定要删除该用户吗？')) return;
    adminApiCall(`/api/admin/users/${id}`, 'DELETE').done(function() { loadUsers(); })
    .fail(function() { alert('删除失败'); });
}

// ================= 3. 订单管理逻辑 =================
function loadOrders() {
    const tbody = $('#admin-order-list');
    tbody.html('<tr><td colspan="8" class="text-center">加载中...</td></tr>');

    adminApiCall('/api/admin/orders/all', 'GET').done(function(orders) {
        tbody.empty();
        if (orders.length === 0) {
            tbody.html('<tr><td colspan="8" class="text-center">暂无订单</td></tr>');
            return;
        }

        orders.forEach(o => {
            const productFullName = (o.productBrand && o.productModel)
                ? `${o.productBrand} ${o.productModel}`
                : '<span class="text-muted">未知商品</span>';
            const category = o.productCategory || '-';
            const imgHtml = o.productImage
                ? `<img src="${GATEWAY_URL + o.productImage}" style="width:40px;height:40px;margin-right:8px;object-fit:cover;border-radius:4px;">`
                : '';
            let statusBadge = `<span class="badge bg-secondary">${o.status}</span>`;
            if (o.status === 'ACTIVE' || o.status === 'active') statusBadge = `<span class="badge bg-primary">ACTIVE</span>`;
            if (o.status === 'CANCELLED' || o.status === 'cancelled') statusBadge = `<span class="badge bg-danger">CANCELLED</span>`;
            const totalPrice = (o.price && o.quantity) ? (o.price * o.quantity).toFixed(2) : '-';

            tbody.append(`
                <tr>
                    <td><span class="text-muted" style="font-family:monospace; font-size:0.8rem;">${o.id}</span></td>
                    <td>
                        <div class="d-flex align-items-center">
                            ${imgHtml}
                            <div>
                                <div class="fw-bold" style="font-size:0.9rem;">${productFullName}</div>
                                <small class="text-muted" style="font-size:0.8rem;">${category}</small>
                            </div>
                        </div>
                    </td>
                    <td><span class="text-dark" style="font-family:monospace; font-size:0.8rem;">${o.buyerId}</span></td>
                    <td>${o.quantity}</td>
                    <td class="text-danger fw-bold">¥${totalPrice}</td>
                    <td>${statusBadge}</td>
                    <td><small>${new Date(o.createdAt).toLocaleString()}</small></td>
                    <td>
                        <button class="btn btn-sm btn-outline-danger" onclick="deleteOrder('${o.id}')">
                            <i class="bi bi-trash"></i>
                        </button>
                    </td>
                </tr>
            `);
        });
    });
}
function deleteOrder(id) {
    if (!confirm('确定要删除该订单吗？')) return;
    adminApiCall(`/api/admin/orders/${id}`, 'DELETE').done(function() { loadOrders(); })
    .fail(function() { alert('删除失败'); });
}