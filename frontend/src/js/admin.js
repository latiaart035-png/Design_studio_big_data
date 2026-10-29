document.addEventListener('DOMContentLoaded', () => {
    loadProducts();
    loadUsers('all');
    initForms();
    loadCategories();
    initUsersTabs();
    loadCurrentUser();
    loadAllOrders();
    initOrdersTabs();
});

const API_BASE_URL = 'http://localhost:8080';

let userIdToDelete = null;
let allOrders = { productOrders: [], designOrders: [] };
let currentOrderFilter = 'all';
let currentCategoryFilter = 'all';
let allProducts = [];

function getToken() {
    return localStorage.getItem('design_studio_token');
}

async function apiFetch(path, options = {}) {
    const token = getToken();
    const headers = { ...(options.headers || {}) };
    if (token) {
        headers['Authorization'] = 'Bearer ' + token;
    }
    return fetch(`${API_BASE_URL}${path}`, { ...options, headers });
}

function getImageUrl(imagePath) {
    // Если путь пустой - возвращаем заглушку
    if (!imagePath || imagePath === 'null' || imagePath === 'undefined' || imagePath === '') {
        return 'src/images/default.jpg';
    }
    
    // Если это уже полный URL с сервером
    if (imagePath.startsWith('http://localhost:8080')) {
        return imagePath;
    }
    
    // Если путь начинается с src/ - это относительный путь к заглушке
    if (imagePath.startsWith('src/images/')) {
        return imagePath;
    }
    
    // Если начинается с /uploads/ - добавляем базовый URL
    if (imagePath.startsWith('/uploads/')) {
        // Убеждаемся, что путь правильный
        const fullUrl = API_BASE_URL + imagePath;
        console.log('Loading image from URL:', fullUrl);
        return fullUrl;
    }
    
    // Если это просто имя файла (без пути)
    if (!imagePath.includes('/') && !imagePath.includes('\\')) {
        // Пробуем загрузить из папки uploads
        const fullUrl = API_BASE_URL + '/uploads/' + imagePath;
        console.log('Loading image file:', fullUrl);
        return fullUrl;
    }
    
    console.warn('Unknown image path format:', imagePath);
    return 'src/images/default.jpg';
}

// Асинхронная загрузка товара с обработкой ошибок
async function loadProductForEdit(id) {
    try {
        console.log('Loading product for edit:', id);
        const res = await apiFetch(`/api/admin/products/${id}`);
        
        if (!res.ok) {
            const errorText = await res.text();
            console.error('Server response:', res.status, errorText);
            throw new Error(`HTTP ${res.status}: ${errorText}`);
        }
        
        const product = await res.json();
        console.log('Product loaded:', product);
        
        document.getElementById('editProductId').value = product.id;
        document.getElementById('editName').value = product.name;
        document.getElementById('editDescription').value = product.description || '';
        document.getElementById('editPrice').value = product.price;
        
        await loadCategories();
        const categorySelect = document.getElementById('editCategoryId');
        if (categorySelect && product.categoryId) {
            categorySelect.value = product.categoryId;
        }
        
        // Показываем текущие изображения с проверкой существования
        const currentMainImageDiv = document.getElementById('currentMainImage');
        if (currentMainImageDiv) {
            if (product.mainImageUrl) {
                const imgUrl = getImageUrl(product.mainImageUrl);
                currentMainImageDiv.innerHTML = `
                    <h6>Текущее главное изображение:</h6>
                    <img src="${imgUrl}" alt="Текущее изображение" 
                         style="max-width: 200px; max-height: 150px; object-fit: contain;"
                         onerror="this.onerror=null; this.src='src/images/default.jpg'; console.error('Failed to load main image:', '${imgUrl}')">
                `;
            } else {
                currentMainImageDiv.innerHTML = '<p class="text-muted">Нет главного изображения</p>';
            }
        }
        
        const currentAdditionalDiv = document.getElementById('currentAdditionalImages');
        if (currentAdditionalDiv) {
            if (product.additionalImages && product.additionalImages.length > 0) {
                currentAdditionalDiv.innerHTML = `
                    <h6>Текущие дополнительные изображения (${product.additionalImages.length}):</h6>
                    <div class="d-flex gap-2 flex-wrap">
                        ${product.additionalImages.map((img, idx) => {
                            const imgUrl = getImageUrl(img);
                            return `
                                <div style="position: relative;">
                                    <img src="${imgUrl}" alt="Дополнительное изображение ${idx + 1}" 
                                         style="max-width: 100px; max-height: 100px; object-fit: cover;"
                                         onerror="this.onerror=null; this.src='src/images/default.jpg'; console.error('Failed to load additional image:', '${imgUrl}')">
                                </div>
                            `;
                        }).join('')}
                    </div>
                `;
            } else {
                currentAdditionalDiv.innerHTML = '<p class="text-muted">Нет дополнительных изображений</p>';
            }
        }
        
        // Показываем форму редактирования
        document.getElementById('addTitle').style.display = 'none';
        document.getElementById('addProductForm').style.display = 'none';
        document.getElementById('editTitle').style.display = 'block';
        document.getElementById('editProductForm').style.display = 'block';
        document.getElementById('editFormCard').style.display = 'block';
        
        // Очищаем поля выбора файлов
        const newMainImage = document.getElementById('newMainImage');
        const newAdditionalImages = document.getElementById('newAdditionalImages');
        if (newMainImage) newMainImage.value = '';
        if (newAdditionalImages) newAdditionalImages.value = '';
        
    } catch (e) {
        console.error('Ошибка загрузки товара для редактирования:', e);
        showToast(`Ошибка загрузки товара: ${e.message}`, true);
    }
}

// ========== ЗАКАЗЫ ==========

// Инициализация табов заказов
function initOrdersTabs() {
    document.querySelectorAll('.orders-tab').forEach(tab => {
        tab.addEventListener('click', function() {
            document.querySelectorAll('.orders-tab').forEach(t => t.classList.remove('active'));
            this.classList.add('active');
            currentOrderFilter = this.getAttribute('data-filter');
            renderOrders();
        });
    });
}

// Загрузка всех заказов
async function loadAllOrders() {
    const container = document.getElementById('ordersContainer');
    if (!container) return;

    try {
        const res = await apiFetch('/api/admin/all-orders');
        if (!res.ok) throw new Error();

        allOrders = await res.json();

        document.getElementById('countAll').textContent = 
            allOrders.productOrders.length + allOrders.designOrders.length;
        document.getElementById('countProducts').textContent = allOrders.productOrders.length;
        document.getElementById('countDesign').textContent = allOrders.designOrders.length;

        renderOrders();
    } catch (e) {
        console.error('Ошибка загрузки заказов:', e);
                container.innerHTML = '<div class="alert alert-danger">Ошибка загрузки заказов</div>';
    }
}

// Отрисовка заказов
function renderOrders() {
    const container = document.getElementById('ordersContainer');
    if (!container) return;

    let orders = [];

    if (currentOrderFilter === 'all') {
        orders = [
            ...allOrders.productOrders.map(o => ({ ...o, orderType: 'product' })),
            ...allOrders.designOrders.map(o => ({ ...o, orderType: 'design' }))
        ];
    } else if (currentOrderFilter === 'product') {
        orders = allOrders.productOrders.map(o => ({ ...o, orderType: 'product' }));
    } else if (currentOrderFilter === 'design') {
        orders = allOrders.designOrders.map(o => ({ ...o, orderType: 'design' }));
    }

    // Сортируем по дате (новые сверху)
    orders.sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt));

    if (orders.length === 0) {
        container.innerHTML = '<div class="text-center text-muted p-5">Заказов не найдено</div>';
        return;
    }

    container.innerHTML = orders.map(order => renderOrderCard(order)).join('');

    // Добавляем обработчики для кнопок обновления статуса
    document.querySelectorAll('.btn-update-status').forEach(btn => {
        btn.addEventListener('click', function() {
            const orderId = this.getAttribute('data-order-id');
            const orderType = this.getAttribute('data-order-type');
            const select = document.getElementById(`status-${orderType}-${orderId}`);
            const newStatus = select.value;
            if (newStatus) {
                updateOrderStatus(orderId, orderType, newStatus);
            }
        });
    });

    // Добавляем обработчики для кнопок разворачивания описания
    document.querySelectorAll('.expand-btn').forEach(btn => {
        btn.addEventListener('click', function() {
            const descId = this.getAttribute('data-desc-id');
            toggleOrderDescription(descId, this);
        });
    });
}

function renderOrderCard(order) {
    const statusOptions = getStatusOptions(order.orderType, order.status);
    const statusText = getStatusText(order.status);

    // Используем правильные поля для суммы
    const total = order.totalAmount || order.total || 0;
    const discount = order.discountAmount || order.discount || 0;
    const finalTotal = total - discount;

    let content = `
        <div class="admin-order-card" data-order-id="${order.id}" data-order-type="${order.orderType}">
            <div class="admin-order-header">
                <span class="admin-order-id">Заказ #${order.id}</span>
                <span class="admin-order-type">${order.orderType === 'product' ? 'Товары' : 'Дизайн'}</span>
            </div>
            <div class="admin-order-body">
                <div class="admin-order-info">
                    <div class="admin-info-item">
                        <span class="admin-info-label">Клиент</span>
                        <span class="admin-info-value">${order.userName || '-'}</span>
                    </div>
                    <div class="admin-info-item">
                        <span class="admin-info-label">Email</span>
                        <span class="admin-info-value">${order.userEmail || '-'}</span>
                    </div>
                    <div class="admin-info-item">
                        <span class="admin-info-label">Адрес</span>
                        <span class="admin-info-value">${order.address || '-'}</span>
                    </div>
                    <div class="admin-info-item">
                        <span class="admin-info-label">Дата</span>
                        <span class="admin-info-value">${new Date(order.createdAt).toLocaleDateString('ru-RU')}</span>
                    </div>
                </div>
    `;

    // Если это заказ товара - показываем товары
    if (order.orderType === 'product') {
        content += renderProductOrderItems(order);
    }

    // Если это заказ дизайна - показываем информацию
    if (order.orderType === 'design') {
        content += renderDesignOrderInfo(order);
    }

    // Блок изменения статуса
    content += `
                <div class="status-selector">
                    <label>Статус заказа</label>
                    <select class="status-select" id="status-${order.orderType}-${order.id}">
                        ${statusOptions}
                    </select>
                    <button class="btn-update-status" 
                            data-order-id="${order.id}" 
                            data-order-type="${order.orderType}">
                        <i class="bi bi-check-circle"></i> Обновить статус
                    </button>
                    <div class="current-status-info">
                        Текущий статус: <strong>${statusText}</strong>
                    </div>
                </div>
    `;

    // Итого
    content += `
                <div class="order-total-section">
                    <div class="order-total-row">
                        <span>Сумма заказа:</span>
                        <span>${total.toLocaleString('ru-RU')} BYN</span>
                    </div>
    `;

    if (discount > 0) {
        content += `
                    <div class="order-total-row discount">
                        <span>Скидка клиента:</span>
                        <span>-${discount.toLocaleString('ru-RU')} BYN</span>
                    </div>
        `;
    }

    content += `
                    <div class="order-total-row final">
                        <span>Итого к оплате:</span>
                        <span>${finalTotal.toLocaleString('ru-RU')} BYN</span>
                    </div>
                </div>
            </div>
        </div>
    `;

    return content;
}

// Рендер товаров заказа
function renderProductOrderItems(order) {
    let html = `
        <div class="order-products">
            <div class="order-products-title">
                <i class="bi bi-box-seam"></i> Товары в заказе
            </div>
    `;

    if (order.items && order.items.length > 0) {
        order.items.forEach((item, index) => {
            const itemTotal = item.price * item.quantity;
            const imageUrl = getImageUrl(item.mainImage || item.image);
            const descId = `desc-${order.id}-${index}`;

            html += `
                <div class="order-product-item">
                    <img src="${imageUrl}" alt="${item.name}" class="order-product-image" onerror="this.src='src/images/default.jpg'">
                    <div class="order-product-info">
                        <div class="order-product-name">${item.name}</div>
                        <div class="order-product-description scrollable-content collapsed" id="${descId}">
                            ${item.description || 'Описание отсутствует'}
                        </div>
                        <button class="expand-btn" data-desc-id="${descId}">Развернуть</button>
                        <div class="order-product-price">Цена: ${item.price.toLocaleString('ru-RU')} BYN</div>
                        <div class="order-product-quantity">Количество: ${item.quantity}</div>
                    </div>
                    <div class="order-product-total">
                        ${itemTotal.toLocaleString('ru-RU')} BYN
                    </div>
                </div>
            `;
        });
    } else {
        html += '<p class="text-muted">Информация о товарах загружается...</p>';
    }

    html += '</div>';
    return html;
}

// Рендер информации о заказе дизайна
function renderDesignOrderInfo(order) {
    let html = '';

    // Фото
    if (order.photos && order.photos.length > 0) {
        html += `
            <div class="design-photos">
                <div class="design-photos-title">
                    <i class="bi bi-camera"></i> Фото помещения клиента
                </div>
                <div class="design-photo-grid">
        `;

        order.photos.forEach(photo => {
            const photoUrl = getImageUrl(photo);
            html += `
                <div class="design-photo-item">
                    <img src="${photoUrl}" alt="Фото помещения" onerror="this.src='src/images/default.jpg'">
                </div>
            `;
        });

        html += '</div></div>';
    }

    // Выбранные товары
    if (order.selectedProducts && order.selectedProducts.length > 0) {
        const productsList = typeof order.selectedProducts === 'string' 
            ? order.selectedProducts.split(',') 
            : order.selectedProducts;

        html += `
            <div class="selected-products-info">
                <div class="selected-products-title">
                    <i class="bi bi-bag"></i> Интересующие товары клиента
                </div>
                <div class="selected-products-list">
        `;

        productsList.forEach(product => {
            html += `<span class="selected-product-tag">${product.trim()}</span>`;
        });

        html += '</div></div>';
    }

    // Дополнительная информация о дизайне
    if (order.roomType || order.tariff) {
        html += `
            <div class="admin-order-info" style="margin-top: 15px;">
        `;
        
        if (order.roomType) {
            html += `
                <div class="admin-info-item">
                    <span class="admin-info-label">Тип помещения</span>
                    <span class="admin-info-value">${order.roomType}</span>
                </div>
            `;
        }
        
        if (order.tariff) {
            html += `
                <div class="admin-info-item">
                    <span class="admin-info-label">Тариф</span>
                    <span class="admin-info-value">${getTariffText(order.tariff)}</span>
                </div>
            `;
        }
        
        if (order.designStyle) {
            html += `
                <div class="admin-info-item">
                    <span class="admin-info-label">Стиль дизайна</span>
                    <span class="admin-info-value">${order.designStyle}</span>
                </div>
            `;
        }
        
        html += '</div>';
    }

    return html;
}

// Опции статуса (только вперёд)
function getStatusOptions(orderType, currentStatus) {
    const statuses = orderType === 'product' 
        ? ['PENDING', 'PROCESSING', 'ASSEMBLING', 'SHIPPING', 'DELIVERED', 'COMPLETED']
        : ['PENDING', 'IN_PROGRESS', 'COMPLETED'];

    let options = '';
    let foundCurrent = false;

    statuses.forEach(status => {
        if (status === currentStatus) {
            foundCurrent = true;
            options += `<option value="${status}" disabled selected>${getStatusText(status)} (текущий)</option>`;
        } else if (!foundCurrent) {
            options += `<option value="${status}" disabled>${getStatusText(status)} (пройдено)</option>`;
        } else {
            options += `<option value="${status}">${getStatusText(status)}</option>`;
        }
    });

    return options;
}

// Получение текста статуса
function getStatusText(status) {
    const statusMap = {
        'PENDING': 'Ожидает',
        'PROCESSING': 'Обрабатывается',
        'ASSEMBLING': 'Собирается',
        'SHIPPING': 'Отправлен',
        'DELIVERED': 'Доставлен',
        'COMPLETED': 'Завершён',
        'IN_PROGRESS': 'В работе'
    };
    return statusMap[status] || status;
}

// Получение текста тарифа
function getTariffText(tariff) {
    const tariffMap = {
        'basic': 'Базовый',
        'standard': 'Стандарт',
        'premium': 'Премиум'
    };
    return tariffMap[tariff] || tariff;
}

// Переключение описания
function toggleOrderDescription(descId, btn) {
    const desc = document.getElementById(descId);
    if (!desc) return;

    if (desc.classList.contains('collapsed')) {
        desc.classList.remove('collapsed');
        desc.classList.add('expanded');
        btn.textContent = 'Свернуть';
    } else {
        desc.classList.remove('expanded');
        desc.classList.add('collapsed');
        btn.textContent = 'Развернуть';
    }
}

// Обновление статуса заказа
async function updateOrderStatus(orderId, orderType, newStatus) {
    try {
        const res = await apiFetch(`/api/admin/orders/${orderId}/status`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
            body: `status=${newStatus}&type=${orderType}`
        });

        if (res.ok) {
            showToast('Статус заказа обновлён!');
            loadAllOrders();
        } else {
            const error = await res.json();
            showToast(error.error || 'Ошибка обновления статуса', true);
        }
    } catch (e) {
        console.error('Ошибка обновления статуса:', e);
        showToast('Ошибка сети', true);
    }
}

// ========== ТОВАРЫ ==========

async function loadCategories() {
    try {
        const res = await apiFetch('/api/products/categories');
        if (!res.ok) throw new Error();
        const categories = await res.json();
        
        const categorySelect = document.getElementById('categorySelect');
        const editCategorySelect = document.getElementById('editCategoryId');
        
        if (categorySelect) {
            categorySelect.innerHTML = categories.map(c => 
                `<option value="${c.id}">${c.name}</option>`
            ).join('');
        }
        
        if (editCategorySelect) {
            editCategorySelect.innerHTML = categories.map(c => 
                `<option value="${c.id}">${c.name}</option>`
            ).join('');
        }
    } catch (e) {
        console.error('Ошибка загрузки категорий:', e);
    }
}

async function loadProducts() {
    const container = document.getElementById('productsList');
    if (!container) return;
    
    try {
        console.log('Loading products...');
        const res = await apiFetch('/api/admin/products');
        console.log('Response status:', res.status);
        
        if (!res.ok) throw new Error('Ошибка загрузки');
        
        allProducts = await res.json();
        console.log('Products loaded:', allProducts);
        console.log('Products count:', allProducts.length);
        
        // Применяем фильтр
        applyProductFilter();
    } catch (e) {
        console.error('Ошибка загрузки товаров:', e);
        if (container) container.innerHTML = '<p style="color: var(--product-danger);">Ошибка загрузки товаров: ' + e.message + '</p>';
    }
}

function applyProductFilter() {
    const container = document.getElementById('productsList');
    if (!container) return;
    
    let filteredProducts = allProducts;
    
    if (currentCategoryFilter !== 'all') {
        filteredProducts = allProducts.filter(p => p.categoryId == currentCategoryFilter);
    }
    
    if (filteredProducts.length === 0) {
        container.innerHTML = '<p style="color: var(--product-text-muted); text-align: center; padding: 20px;">Товаров не найдено.</p>';
        return;
    }
    
    container.innerHTML = filteredProducts.map(p => renderProductCard(p)).join('');
}

async function deleteProduct(id) {
    try {
        const res = await apiFetch(`/api/admin/products/${id}`, {
            method: 'DELETE'
        });
        
        if (res.ok) {
            loadProducts();
            showToast('Товар удалён!');
        } else {
            const error = await res.json();
            alert('Ошибка удаления товара: ' + (error.error || 'Неизвестная ошибка'));
        }
    } catch (e) {
        console.error('Ошибка удаления товара:', e);
        alert('Ошибка удаления товара');
    }
}

// ========== ПОЛЬЗОВАТЕЛИ ==========

function initUsersTabs() {
    const tabs = document.querySelectorAll('[data-user-tab]');
    tabs.forEach(tab => {
        tab.addEventListener('click', (e) => {
            e.preventDefault();
            tabs.forEach(t => t.classList.remove('active'));
            tab.classList.add('active');
            const role = tab.getAttribute('data-user-tab');
            loadUsers(role);
        });
    });
}

function openDeleteUserModal(userId, userName, userEmail) {
    userIdToDelete = userId;
    const infoDiv = document.getElementById('deleteUserInfo');
    if (infoDiv) {
        infoDiv.innerHTML = `<strong>${userName || 'Пользователь'}</strong><br><small class="text-muted">${userEmail || ''}</small>`;
    }
    document.getElementById('deleteUserModal').style.display = 'flex';
}

function closeDeleteUserModal() {
    document.getElementById('deleteUserModal').style.display = 'none';
    userIdToDelete = null;
}

async function loadUsers(role = 'all') {
    const container = document.getElementById('usersList');
    if (!container) return;
    
    try {
        const res = await apiFetch(`/api/admin/users?role=${role}`);
        if (!res.ok) throw new Error();
        
        const users = await res.json();
        
        if (users.length === 0) {
            container.innerHTML = '<p>Пользователей пока нет.</p>';
            return;
        }
        
        const roleLabel = role === 'clients' ? 'Клиенты' : role === 'admins' ? 'Администраторы' : 'Все пользователи';
        
        container.innerHTML = `
            <div class="mb-3">
                <h5>${roleLabel} (${users.length})</h5>
            </div>
        ` + users.map(u => {
            const isAdmin = u.role === 'ADMIN';
            
            return `
            <div class="card mb-2">
                <div class="card-body">
                    <div class="d-flex justify-content-between align-items-start">
                        <div>
                            <strong>${u.firstName || ''} ${u.lastName || ''}</strong> 
                            <span class="badge bg-${isAdmin ? 'danger' : 'primary'}">${isAdmin ? 'Администратор' : 'Клиент'}</span>
                            <br>
                            <small class="text-muted">${u.email}</small><br>
                            <small>Статус: 
                                <select id="user-status-${u.id}" class="form-select form-select-sm d-inline-block" style="width: auto;">
                                    <option value="BASIC" ${u.status === 'BASIC' ? 'selected' : ''}>BASIC</option>
                                    <option value="PREMIUM" ${u.status === 'PREMIUM' ? 'selected' : ''}>PREMIUM</option>
                                    <option value="VIP" ${u.status === 'VIP' ? 'selected' : ''}>VIP</option>
                                </select>
                                <button class="btn btn-sm btn-outline-primary update-user-status" data-user-id="${u.id}">OK</button>
                            </small><br>
                            <small>Скидка: <strong>${(u.discount * 100).toFixed(0)}%</strong></small>
                            ${!isAdmin ? `
                                <br>
                                <small>Заказов: ${u.totalOrders || 0}</small><br>
                                <small>Потрачено: ${(u.totalSpent || 0).toLocaleString('ru-RU')} BYN</small>
                            ` : ''}
                        </div>
                        <div class="d-flex flex-column gap-1">
                            ${!isAdmin ? `
                                <button class="btn btn-sm btn-info view-user-orders" data-user-id="${u.id}">Заказы</button>
                            ` : ''}
                            <button class="btn btn-sm btn-danger delete-user" data-user-id="${u.id}">Удалить</button>
                        </div>
                    </div>
                </div>
            </div>
        `;
        }).join('');
    } catch (e) {
        console.error('Ошибка загрузки пользователей:', e);
        container.innerHTML = '<p>Ошибка загрузки пользователей.</p>';
    }
}

async function deleteUser(id) {
    try {
        const res = await apiFetch(`/api/admin/users/${id}`, {
            method: 'DELETE'
        });
        
        if (res.ok) {
            loadUsers('all');
            showToast('Пользователь удалён!');
        } else {
            const error = await res.json();
            alert('Ошибка удаления пользователя: ' + (error.error || 'Неизвестная ошибка'));
        }
    } catch (e) {
        console.error('Ошибка удаления пользователя:', e);
        alert('Ошибка удаления пользователя');
    }
}

async function updateUserStatus(id, status) {
    try {
        const res = await fetch(`${API_BASE_URL}/api/admin/users/${id}/status?status=${status}`, {
            method: 'PUT',
            headers: {
                'Authorization': 'Bearer ' + getToken()
            }
        });
        
        if (res.ok) {
            const data = await res.json();
            showToast(`Статус обновлён! Скидка: ${(data.discount * 100).toFixed(0)}%`);
            loadUsers('all');
        } else {
            const error = await res.json();
            alert('Ошибка обновления статуса: ' + (error.error || 'Неизвестная ошибка'));
        }
    } catch (e) {
        console.error('Ошибка обновления статуса:', e);
        alert('Ошибка обновления статуса');
    }
}

async function openUserOrdersModal(userId) {
    const modal = document.getElementById('userDetailsModal');
    const productContent = document.getElementById('productOrdersContent');
    const designContent = document.getElementById('designOrdersContent');
    
    modal.style.display = 'flex';
    
    try {
        const res = await apiFetch(`/api/admin/user-orders/${userId}`);
        if (!res.ok) throw new Error();
        
        const orders = await res.json();
        
        const productOrders = orders.filter(o => o.type === 'product');
        const designOrders = orders.filter(o => o.type === 'design');
        
        if (productOrders.length === 0) {
            productContent.innerHTML = '<p class="text-muted">Заказов на мебель пока нет.</p>';
        } else {
            productContent.innerHTML = productOrders.map(o => `
                <div class="order-card">
                    <div class="order-card-header">
                        <div><strong>#${o.id}</strong></div>
                        <span class="badge bg-secondary">${getStatusText(o.status)}</span>
                    </div>
                    <div class="order-card-body">
                        <small>Сумма: ${(o.total || 0).toLocaleString('ru-RU')} BYN</small><br>
                        <small>Адрес: ${o.address || '-'}</small><br>
                        <small>Дата: ${new Date(o.createdAt).toLocaleString('ru-RU')}</small>
                    </div>
                </div>
            `).join('');
        }
        
        if (designOrders.length === 0) {
            designContent.innerHTML = '<p class="text-muted">Заказов на дизайн пока нет.</p>';
        } else {
            designContent.innerHTML = designOrders.map(o => `
                <div class="order-card">
                    <div class="order-card-header">
                        <div><strong>#${o.id}</strong></div>
                        <span class="badge bg-secondary">${getStatusText(o.status)}</span>
                    </div>
                    <div class="order-card-body">
                        <small>Тариф: ${getTariffText(o.tariff) || '-'}</small><br>
                        <small>Сумма: ${(o.total || 0).toLocaleString('ru-RU')} BYN</small><br>
                        <small>Адрес: ${o.address || '-'}</small><br>
                        <small>Дата: ${new Date(o.createdAt).toLocaleString('ru-RU')}</small>
                    </div>
                </div>
            `).join('');
        }
    } catch (e) {
        console.error('Ошибка загрузки заказов пользователя:', e);
        productContent.innerHTML = '<p>Ошибка загрузки заказов.</p>';
        designContent.innerHTML = '<p>Ошибка загрузки заказов.</p>';
    }
}

/// Обновить функцию loadCurrentUser() в admin.js:
async function loadCurrentUser() {
    try {
        const res = await apiFetch('/api/user/me');
        if (!res.ok) throw new Error();
        
        const user = await res.json();
        
        document.getElementById('accountFirstName').textContent = user.firstName || '-';
        document.getElementById('accountLastName').textContent = user.lastName || '-';
        document.getElementById('accountEmail').textContent = user.email || '-';
        document.getElementById('accountRole').textContent = user.role === 'ADMIN' ? 'Администратор' : 'Клиент';
        
        const discount = (user.discount || 0) * 100;
        document.getElementById('accountDiscountValue').textContent = discount + '%';
    } catch (e) {
        console.error('Ошибка загрузки данных пользователя:', e);
    }
}

// ========== ФОРМЫ ==========

function initForms() {
    const addProductForm = document.getElementById('addProductForm');
    if (addProductForm) {
        addProductForm.addEventListener('submit', async (e) => {
            e.preventDefault();
            
            const formData = new FormData(addProductForm);
            
            const mainImageInput = addProductForm.querySelector('input[name="mainImage"]');
            if (!mainImageInput.files || mainImageInput.files.length === 0) {
                alert('Пожалуйста, выберите главное изображение для товара');
                return;
            }
            
            const mainImage = mainImageInput.files[0];
            formData.set('mainImage', mainImage);
            
            const additionalInput = addProductForm.querySelector('input[name="additionalImages"]');
            if (additionalInput && additionalInput.files.length > 0) {
                for (let i = 0; i < additionalInput.files.length; i++) {
                    formData.append('additionalImages', additionalInput.files[i]);
                }
            }
            
            const res = await apiFetch('/api/admin/products', {
                method: 'POST',
                body: formData
            });
            
            if (res.ok) {
                addProductForm.reset();
                loadProducts();
                showToast('Товар успешно добавлен!');
            } else {
                const error = await res.json();
                alert('Ошибка добавления товара: ' + (error.error || 'Неизвестная ошибка'));
            }
        });
    }

    const editProductForm = document.getElementById('editProductForm');
    // В initForms() для editProductForm:
    if (editProductForm) {
        editProductForm.addEventListener('submit', async (e) => {
            e.preventDefault();
            
            const formData = new FormData();
            const productId = document.getElementById('editProductId').value;
            
            // Добавляем текстовые поля
            formData.append('name', document.getElementById('editName').value);
            formData.append('description', document.getElementById('editDescription').value);
            formData.append('price', document.getElementById('editPrice').value);
            formData.append('categoryId', document.getElementById('editCategoryId').value);
            
            // Получаем файлы
            const newMainImageInput = document.getElementById('newMainImage');
            const newMainImage = newMainImageInput ? newMainImageInput.files[0] : null;
            
            const newAdditionalImagesInput = document.getElementById('newAdditionalImages');
            const newAdditionalImages = newAdditionalImagesInput ? newAdditionalImagesInput.files : null;
            
            // Если выбрано новое главное изображение - отправляем его
            if (newMainImage && newMainImage.size > 0) {
                formData.append('mainImage', newMainImage);
                formData.append('keepExistingMainImage', 'false');
            } else {
                formData.append('keepExistingMainImage', 'true');
            }
            
            // Если выбраны новые дополнительные изображения - отправляем их
            if (newAdditionalImages && newAdditionalImages.length > 0) {
                formData.append('keepExistingAdditionalImages', 'false');
                for (let i = 0; i < newAdditionalImages.length; i++) {
                    if (newAdditionalImages[i] && newAdditionalImages[i].size > 0) {
                        formData.append('additionalImages', newAdditionalImages[i]);
                    }
                }
            } else {
                formData.append('keepExistingAdditionalImages', 'true');
            }
            
            try {
                const res = await apiFetch(`/api/admin/products/${productId}`, {
                    method: 'PUT',
                    body: formData
                });
                
                if (res.ok) {
                    hideEditForm();
                    loadProducts();
                    showToast('Товар успешно обновлён!');
                } else {
                    const error = await res.json();
                    showToast(error.error || 'Ошибка редактирования товара', true);
                }
            } catch (error) {
                console.error('Error updating product:', error);
                showToast('Ошибка сети', true);
            }
        });
    }

    const addAdminForm = document.getElementById('addAdminForm');
    if (addAdminForm) {
        addAdminForm.addEventListener('submit', async (e) => {
            e.preventDefault();
            const data = new FormData(addAdminForm);
            const jsonData = Object.fromEntries(data);
            
            const res = await apiFetch('/api/admin/users', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(jsonData)
            });
            
            if (res.ok) {
                addAdminForm.reset();
                loadUsers('all');
                showToast('Администратор успешно добавлен!');
            } else {
                const error = await res.json();
                alert('Ошибка добавления админа: ' + (error.error || 'Неизвестная ошибка'));
            }
        });
    }

    const changePasswordForm = document.getElementById('changePasswordForm');
    if (changePasswordForm) {
        changePasswordForm.addEventListener('submit', async (e) => {
            e.preventDefault();
            
            const currentPassword = changePasswordForm.querySelector('[name="currentPassword"]').value;
            const newPassword = changePasswordForm.querySelector('[name="newPassword"]').value;
            const confirmPassword = changePasswordForm.querySelector('[name="confirmPassword"]').value;
            
            // Валидация
            if (!currentPassword) {
                showToast('Введите текущий пароль', true);
                return;
            }
            
            if (!newPassword) {
                showToast('Введите новый пароль', true);
                return;
            }
            
            if (newPassword.length < 3) {
                showToast('Пароль должен содержать минимум 3 символа', true);
                return;
            }
            
            if (newPassword !== confirmPassword) {
                showToast('Новый пароль и подтверждение не совпадают', true);
                return;
            }
            
            const jsonData = {
                currentPassword: currentPassword,
                newPassword: newPassword,
                confirmPassword: confirmPassword
            };
            
            const res = await apiFetch('/api/user/password', {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(jsonData)
            });
            
            if (res.ok) {
                showToast('Пароль успешно изменён!');
                changePasswordForm.reset();
                closePasswordModal();
            } else {
                const error = await res.json();
                showToast(error.error || error.message || 'Ошибка изменения пароля', true);
            }
        });
    }

    // Делегирование событий
    document.addEventListener('click', async function(e) {
        if (e.target.classList.contains('edit-product')) {
            const id = e.target.getAttribute('data-product-id');
            await loadProductForEdit(id);
        }
        else if (e.target.classList.contains('delete-product')) {
            if (!confirm('Вы уверены, что хотите удалить этот товар?')) return;
            const id = e.target.getAttribute('data-product-id');
            await deleteProduct(id);
        }
        else if (e.target.classList.contains('delete-user')) {
            const id = e.target.getAttribute('data-user-id');
            const userCard = e.target.closest('.card');
            const userName = userCard ? userCard.querySelector('strong').textContent : '';
            const userEmail = userCard ? userCard.querySelector('.text-muted').textContent : '';
            openDeleteUserModal(id, userName, userEmail);
        }
        else if (e.target.id === 'confirmDeleteUserBtn') {
            if (userIdToDelete) {
                await deleteUser(userIdToDelete);
                closeDeleteUserModal();
            }
        }
        else if (e.target.classList.contains('update-user-status')) {
            const id = e.target.getAttribute('data-user-id');
            const select = document.getElementById(`user-status-${id}`);
            if (select) {
                await updateUserStatus(id, select.value);
            }
        }
        else if (e.target.classList.contains('view-user-orders')) {
            const userId = e.target.getAttribute('data-user-id');
            await openUserOrdersModal(userId);
        }
    });
}

// ========== TOAST ==========

function showToast(message, isError = false) {
    const existingToasts = document.querySelectorAll('.custom-toast');
    existingToasts.forEach(t => t.remove());
    
    const toast = document.createElement('div');
    toast.className = `custom-toast ${isError ? 'error' : ''}`;
    toast.innerHTML = `
        <span>${message}</span>
        <button type="button" class="close-btn" onclick="this.parentElement.remove()">&times;</button>
    `;
    
    document.body.appendChild(toast);
    
    setTimeout(() => {
        toast.classList.add('show');
    }, 10);
    
    setTimeout(() => {
        toast.classList.remove('show');
        setTimeout(() => toast.remove(), 500);
    }, 3000);
}

// Добавить в admin.js функцию loadHeader или обновить существующую:

async function loadAdminHeader() {
    const container = document.getElementById('header-container');
    try {
        const user = await fetchCurrentUser();
        const response = await fetch('src/components/header.html');
        let headerHtml = await response.text();
        
        // Проверяем, что пользователь - админ
        if (user && user.role === 'ADMIN') {
            // Показываем ссылки на CRM, Marketing и Analytics
            headerHtml = headerHtml.replace('id="navCrm"', 'id="navCrm" style="display: block;"');
            headerHtml = headerHtml.replace('id="navMarketing"', 'id="navMarketing" style="display: block;"');
            headerHtml = headerHtml.replace('id="navAnalytics"', 'id="navAnalytics" style="display: block;"');
        }
        
        // Обновляем авторизацию
        if (user) {
            headerHtml = headerHtml.replace('id="navRegister"', 'id="navRegister" style="display: none;"');
            headerHtml = headerHtml.replace('id="navProfile"', 'id="navProfile" style="display: block;"');
            headerHtml = headerHtml.replace('id="navLogout"', 'id="navLogout" onclick="logout()" style="cursor: pointer;"');
        } else {
            headerHtml = headerHtml.replace('id="navLogout"', 'id="navLogout" onclick="goToLogin()" style="cursor: pointer;"');
        }
        
        container.innerHTML = headerHtml;
    } catch (e) {
        console.error('Error loading header:', e);
        container.innerHTML = createFallbackHeader();
    }
}

function createFallbackHeader() {
    return `
        <nav class="navbar navbar-expand-lg" style="background-color: #6B1F3F;">
            <div class="container">
                <a class="navbar-brand text-white" href="index.html">Design Studio</a>
            </div>
        </nav>
    `;
}

function logout() {
    localStorage.removeItem('design_studio_token');
    localStorage.removeItem('design_studio_user');
    window.location.href = 'login.html';
}

function goToLogin() {
    window.location.href = 'login.html';
}

function renderProductCard(product) {
    // Формируем URL изображения - используем тот же подход, что и в магазине
    let imageUrl = 'src/images/default.jpg';
    
    if (product.mainImageUrl) {
        // Если mainImageUrl уже полный URL, используем его
        if (product.mainImageUrl.startsWith('http')) {
            imageUrl = product.mainImageUrl;
        } 
        // Если начинается с /uploads/
        else if (product.mainImageUrl.startsWith('/uploads/')) {
            imageUrl = API_BASE_URL + product.mainImageUrl;
        }
        // Если просто имя файла или другой формат
        else if (!product.mainImageUrl.includes('/')) {
            imageUrl = API_BASE_URL + '/uploads/' + product.mainImageUrl;
        }
        else {
            imageUrl = product.mainImageUrl;
        }
    }
    
    console.log(`Product ${product.id} (${product.name}) image URL:`, imageUrl);
    
    // Собираем дополнительные изображения
    const additionalImages = product.additionalImages || [];
    const visibleAdditional = additionalImages.slice(0, 3);
    const moreCount = additionalImages.length - 3;
    
    return `
        <article class="product-card">
            <div class="product-image-section">
                <img src="${imageUrl}" alt="${product.name}" class="main-photo" onerror="this.onerror=null; this.src='src/images/default.jpg'; console.error('Failed to load main image for product ${product.id}:', '${imageUrl}')">
                ${additionalImages.length > 0 ? `
                <div class="side-photos">
                    ${visibleAdditional.map(img => {
                        let additionalUrl = img;
                        if (img && !img.startsWith('http') && !img.startsWith('/uploads/')) {
                            additionalUrl = API_BASE_URL + '/uploads/' + img;
                        } else if (img && img.startsWith('/uploads/')) {
                            additionalUrl = API_BASE_URL + img;
                        }
                        return `<img src="${additionalUrl}" alt="Дополнительное фото" onerror="this.src='src/images/default.jpg'">`;
                    }).join('')}
                    ${moreCount > 0 ? `<div class="more-photos">+${moreCount}</div>` : ''}
                </div>
                ` : ''}
            </div>
            <div class="product-info">
                <div class="header-row">
                    <span class="category-tag">${product.categoryName || 'Без категории'}</span>
                    <h3 class="product-title">${escapeHtml(product.name)}</h3>
                </div>
                <p class="product-description">${escapeHtml(product.description) || 'Описание отсутствует'}</p>
                <div class="product-footer">
                    <div class="price">${formatNumber(product.price)} <span>BYN</span></div>
                    <div class="actions">
                        <button class="btn-edit edit-product" data-product-id="${product.id}" title="Редактировать">
                            <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2">
                                <path d="M11 4H4a2 2 0 00-2 2v14a2 2 0 002 2h14a2 2 0 002-2v-7M18.5 2.5a2.121 2.121 0 113 3L12 15l-4 1 1-4 9.5-9.5z"/>
                            </svg>
                            Изменить
                        </button>
                        <button class="btn-delete delete-product" data-product-id="${product.id}" title="Удалить">
                            <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2">
                                <path d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"/>
                            </svg>
                        </button>
                    </div>
                </div>
            </div>
        </article>
    `;
}

// Добавьте эту функцию после loadProducts
async function refreshProducts() {
    await loadProducts();
}

// Вспомогательная функция для экранирования HTML
function escapeHtml(text) {
    if (!text) return '';
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

// Вспомогательная функция для форматирования чисел
function formatNumber(value) {
    if (value === null || value === undefined) return '0';
    return Number(value).toLocaleString('ru-RU', {
        minimumFractionDigits: 2,
        maximumFractionDigits: 2
    });
}