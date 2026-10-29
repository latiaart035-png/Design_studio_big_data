// src/js/profile.js

const API_BASE_URL = 'http://localhost:8080';

function getToken() {
    return localStorage.getItem('design_studio_token');
}

function getImageUrl(imagePath) {
    if (!imagePath || imagePath === 'null' || imagePath === '') {
        return 'src/images/default.jpg';
    }
    if (imagePath.startsWith('http://localhost:8080')) {
        return imagePath;
    }
    if (imagePath.startsWith('/uploads/')) {
        return API_BASE_URL + imagePath;
    }
    return API_BASE_URL + '/uploads/' + imagePath;
}

document.addEventListener('DOMContentLoaded', () => {
    loadProfile();
    loadDiscountInfo();
    loadCart();
    loadProductOrders();
    loadDesignOrders();
    loadUserCampaigns();
    loadUserMessages();  // Добавьте эту строку
    initPasswordForm();
});

// === ЗАГРУЗКА ПРОФИЛЯ ПОЛЬЗОВАТЕЛЯ ===
async function loadProfile() {
    try {
        const res = await apiFetch('/api/user/me');
        if (!res.ok) throw new Error();
        const user = await res.json();

        document.getElementById('userFirstName').textContent = user.firstName || '-';
        document.getElementById('userLastName').textContent = user.lastName || '-';
        document.getElementById('userEmail').textContent = user.email || '-';
        document.getElementById('userStatus').textContent = user.status || '-';

        const discount = (user.discount || 0) * 100;
        document.getElementById('userDiscountValue').textContent = discount + '%';

        checkActivePromo();
    } catch (e) {
        console.error('Ошибка загрузки профиля:', e);
    }
}

// === ЗАГРУЗКА ИНФОРМАЦИИ О СКИДКЕ ===
async function loadDiscountInfo() {
    const discountProgress = document.getElementById('discountProgress');
    const discountProgressFill = document.getElementById('discountProgressFill');
    const discountProgressText = document.getElementById('discountProgressText');
    const discountLevelBadge = document.getElementById('discountLevelBadge');
    const userDiscountValue = document.getElementById('userDiscountValue');
    const quarterDates = document.getElementById('quarterDates');
    const quarterInfo = document.getElementById('quarterInfo');

    try {
        const res = await apiFetch('/api/user/discount');
        if (!res.ok) throw new Error();
        const info = await res.json();

        console.log('[Profile] Discount info received:', info);

        // Update discount value
        const discountPercent = info.discountPercent || 0;
        if (userDiscountValue) {
            userDiscountValue.textContent = discountPercent + '%';
        }

        // Update level badge
        if (discountLevelBadge) {
            const status = (info.status || 'BASIC').toUpperCase();
            discountLevelBadge.className = 'discount-level-badge ' + (info.status || 'basic').toLowerCase();
            discountLevelBadge.textContent = status;
        }

        // Update progress bar
        if (discountProgressFill && discountProgressText) {
            const premiumThreshold = info.premiumThreshold || 2000;
            const vipThreshold = info.vipThreshold || 3500;
            const quarterSpent = info.quarterSpent || 0;

            let progressPercent = 0;
            let nextLevel = '';

            if (info.status === 'VIP') {
                progressPercent = 100;
                nextLevel = 'Максимальный уровень достигнут!';
            } else if (info.status === 'PREMIUM') {
                progressPercent = ((quarterSpent - premiumThreshold) / (vipThreshold - premiumThreshold)) * 100;
                nextLevel = 'До VIP: ' + formatBYN(info.remainingForVip || 0);
            } else {
                progressPercent = (quarterSpent / premiumThreshold) * 100;
                nextLevel = 'До PREMIUM: ' + formatBYN(info.remainingForPremium || 0);
            }

            progressPercent = Math.min(Math.max(progressPercent, 0), 100);

            discountProgressFill.className = 'discount-progress-fill ' + (info.status || 'basic').toLowerCase();
            discountProgressFill.style.width = progressPercent + '%';
            discountProgressText.textContent = nextLevel;
        }

        // Update quarter dates
        if (quarterDates && quarterInfo) {
            if (info.quarterStart && info.quarterEnd) {
                quarterInfo.style.display = 'block';
                const start = new Date(info.quarterStart).toLocaleDateString('ru-RU', { day: 'numeric', month: 'short' });
                const end = new Date(info.quarterEnd).toLocaleDateString('ru-RU', { day: 'numeric', month: 'short' });
                quarterDates.textContent = start + ' - ' + end;
            } else {
                quarterInfo.style.display = 'none';
            }
        }

        // Update status in profile
        if (document.getElementById('userStatus')) {
            document.getElementById('userStatus').textContent = info.status || '-';
        }

    } catch (e) {
        console.error('Ошибка загрузки информации о скидке:', e);
        if (discountProgress) {
            discountProgress.style.display = 'none';
        }
    }
}

function formatBYN(value) {
    if (value === null || value === undefined) return '0 BYN';
    return new Intl.NumberFormat('ru-RU', {
        style: 'currency',
        currency: 'BYN',
        minimumFractionDigits: 0,
        maximumFractionDigits: 0
    }).format(value);
}

function checkActivePromo() {
    const activePromo = JSON.parse(localStorage.getItem('activePromo') || 'null');
    const campaignDiscountInfo = document.getElementById('campaignDiscountInfo');
    const campaignDiscountValue = document.getElementById('campaignDiscountValue');

    if (activePromo && campaignDiscountInfo && campaignDiscountValue) {
        campaignDiscountInfo.style.display = 'block';
        campaignDiscountValue.textContent = activePromo.discountPercent + '% (' + activePromo.name + ')';
    } else if (campaignDiscountInfo) {
        campaignDiscountInfo.style.display = 'none';
    }
}

// === ЗАГРУЗКА КОРЗИНЫ ===
async function loadCart() {
    const container = document.getElementById('cartItems');
    const totalContainer = document.getElementById('cartTotal');
    const totalValue = document.getElementById('cartTotalValue');
    const checkoutBtn = document.getElementById('checkoutBtn');
    const discountInfo = document.getElementById('discountInfo');
    const discountAmount = document.getElementById('discountAmount');

    if (!container) {
        console.error('Cart container not found!');
        return;
    }

    try {
        const res = await apiFetch('/api/cart');
        if (!res.ok) throw new Error();
        const items = await res.json();

        console.log('Cart items loaded:', items.length);

        if (!items || items.length === 0) {
            container.innerHTML = `
                <div class="cart-empty-text">
                    <i class="bi bi-cart-x"></i>
                    <p>Корзина пуста</p>
                    <small class="text-muted">Добавьте товары из каталога</small>
                </div>
            `;
            if (totalContainer) totalContainer.style.display = 'none';
            if (checkoutBtn) checkoutBtn.style.display = 'none';
            return;
        }

        let html = '';
        let total = 0;

        items.forEach(item => {
            const itemTotal = item.productPrice * item.quantity;
            total += itemTotal;

            html += `
                <div class="cart-item-row" data-cart-id="${item.cartId}">
                    <img src="${getImageUrl(item.productImage)}" 
                         alt="${item.productName}" 
                         class="cart-item-image"
                         onerror="this.src='src/images/default.jpg'">
                    <div class="cart-item-details">
                        <div class="cart-item-name">${escapeHtml(item.productName)}</div>
                        <div class="cart-item-price">${item.productPrice.toLocaleString('ru-RU')} BYN за шт.</div>
                    </div>
                    <div class="cart-item-controls">
                        <button class="quantity-btn" 
                                onclick="updateQuantity(${item.cartId}, ${item.quantity - 1})"
                                ${item.quantity <= 1 ? 'disabled' : ''}>−</button>
                        <span class="quantity-value">${item.quantity}</span>
                        <button class="quantity-btn" 
                                onclick="updateQuantity(${item.cartId}, ${item.quantity + 1})">+</button>
                    </div>
                    <div class="cart-item-total">${itemTotal.toLocaleString('ru-RU')} BYN</div>
                    <button class="cart-item-remove" onclick="removeFromCart(${item.cartId})">
                        <i class="bi bi-trash3"></i> Удалить
                    </button>
                </div>
            `;
        });

        container.innerHTML = html;
        if (totalContainer) totalContainer.style.display = 'block';
        if (checkoutBtn) checkoutBtn.style.display = 'block';

        // Calculate discounts
        let finalTotal = total;
        let totalDiscount = 0;

        // User discount (from quarterly system)
        try {
            const discountRes = await apiFetch('/api/user/discount');
            if (discountRes.ok) {
                const discountData = await discountRes.json();
                const userDiscountPercent = discountData.discount || 0;
                if (userDiscountPercent > 0) {
                    const userDiscountValue = total * userDiscountPercent;
                    totalDiscount += userDiscountValue;
                }
            }
        } catch (e) {
            console.error('Ошибка получения скидки пользователя:', e);
        }

        // Promo code discount
        const activePromo = JSON.parse(localStorage.getItem('activePromo') || 'null');
        if (activePromo && activePromo.discountPercent > 0) {
            const promoDiscount = total * (activePromo.discountPercent / 100);
            totalDiscount += promoDiscount;
        }

        finalTotal = total - totalDiscount;

        // Показываем информацию о скидке, если есть
        if (totalDiscount > 0 && discountInfo && discountAmount) {
            discountInfo.style.display = 'block';
            discountAmount.textContent = totalDiscount.toLocaleString('ru-RU');
            
            // Проверяем существование элементов перед использованием
            const discountNameEl = document.getElementById('discountName');
            const discountPercentValueEl = document.getElementById('discountPercentValue');
            
            if (activePromo && discountNameEl && discountPercentValueEl) {
                discountNameEl.textContent = activePromo.name || 'Акция';
                discountPercentValueEl.textContent = activePromo.discountPercent || 0;
            }
        } else if (discountInfo) {
            discountInfo.style.display = 'none';
        }

        if (totalValue) {
            totalValue.textContent = finalTotal.toLocaleString('ru-RU') + ' BYN';
        }

    } catch (e) {
        console.error('Ошибка загрузки корзины:', e);
        container.innerHTML = `
            <div class="cart-empty-text">
                <i class="bi bi-exclamation-triangle"></i>
                <p>Ошибка загрузки корзины</p>
                <small class="text-muted">${e.message || 'Попробуйте обновить страницу'}</small>
            </div>
        `;
    }
}

// === ОБНОВЛЕНИЕ КОЛИЧЕСТВА ===
async function updateQuantity(cartId, quantity) {
    if (quantity < 1) {
        await removeFromCart(cartId);
        return;
    }

    try {
        const res = await apiFetch(`/api/cart/update/${cartId}`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
            body: `quantity=${quantity}`
        });

        if (res.ok) {
            loadCart();
        }
    } catch (e) {
        console.error('Ошибка обновления количества:', e);
    }
}

// === УДАЛЕНИЕ ИЗ КОРЗИНЫ ===
async function removeFromCart(cartId) {
    try {
        const res = await apiFetch(`/api/cart/remove/${cartId}`, {
            method: 'DELETE'
        });

        if (res.ok) {
            showNotification('Товар удалён из корзины', 'success');
            loadCart();
        }
    } catch (e) {
        console.error('Ошибка удаления из корзины:', e);
        showNotification('Ошибка удаления товара', 'error');
    }
}

// === ЗАГРУЗКА ЗАКАЗОВ НА ТОВАРЫ ===
async function loadProductOrders() {
    const container = document.getElementById('productOrdersList');
    if (!container) return;

    try {
        const res = await apiFetch('/api/orders');
        if (!res.ok) throw new Error();
        const orders = await res.json();

        if (!orders || orders.length === 0) {
            container.innerHTML = `
                <div class="orders-empty-state">
                    <i class="bi bi-box-seam"></i>
                    <p>Заказов на товары пока нет</p>
                </div>
            `;
            return;
        }

        container.innerHTML = orders.map((order, index) => {
            const statusClass = getStatusClass(order.status);
            const statusText = getStatusText(order.status);
            
            // Используем правильные поля
            const originalAmount = order.originalAmount || 0;
            const discountAmount = order.discountAmount || 0;
            const finalAmount = order.totalAmount || (originalAmount - discountAmount);
            const hasDiscount = discountAmount > 0;
            const discountPercent = order.discountPercent || 
                (discountAmount > 0 ? ((discountAmount / originalAmount) * 100).toFixed(0) : 0);
            
            const campaignName = order.campaignName || '';
            const orderDate = order.createdAt ? new Date(order.createdAt).toLocaleDateString('ru-RU', {
                day: '2-digit',
                month: '2-digit',
                year: 'numeric',
                hour: '2-digit',
                minute: '2-digit'
            }) : '-';

            // Добавляем отладочный вывод
            console.log(`Order ${order.id}: original=${originalAmount}, discount=${discountAmount}, final=${finalAmount}`);

            return `
                <div class="order-card">
                    <div class="order-card-header">
                        <span class="order-number">
                            Заказ #${order.id}
                            <span class="order-db-id">(ID: ${order.id})</span>
                        </span>
                        <span class="order-status ${statusClass}">${statusText}</span>
                    </div>
                    
                    <div class="order-details">
                        <div class="order-price-block">
                            <div><strong>Сумма заказа:</strong> ${formatNumber(originalAmount)} BYN</div>
                            ${hasDiscount ? `
                                <div class="discount-info mt-1">
                                    <i class="bi bi-tag"></i> 
                                    <strong>Скидка:</strong> -${formatNumber(discountAmount)} BYN 
                                    <span class="badge bg-success ms-1">${discountPercent}%</span>
                                    ${campaignName ? `<br><small class="text-muted">Промокод: ${escapeHtml(campaignName)}</small>` : ''}
                                </div>
                                <div class="mt-2 fw-bold text-success">
                                    <i class="bi bi-currency-dollar"></i> 
                                    <strong>Итого к оплате:</strong> ${formatNumber(finalAmount)} BYN
                                </div>
                            ` : `
                                <div class="mt-2 fw-bold">
                                    <strong>Итого к оплате:</strong> ${formatNumber(originalAmount)} BYN
                                </div>
                            `}
                        </div>
                        
                        <div class="order-address mt-2">
                            <div><strong><i class="bi bi-geo-alt"></i> Адрес доставки:</strong> ${escapeHtml(order.address || '-')}</div>
                        </div>
                        
                        <div class="order-date mt-1">
                            <small class="text-muted">
                                <i class="bi bi-calendar"></i> 
                                Дата заказа: ${orderDate}
                            </small>
                        </div>
                    </div>
                </div>
            `;
        }).join('');

    } catch (e) {
        console.error('Ошибка загрузки заказов:', e);
        container.innerHTML = `
            <div class="orders-empty-state">
                <i class="bi bi-exclamation-triangle"></i>
                <p>Ошибка загрузки заказов</p>
                <small class="text-muted">${e.message || 'Попробуйте обновить страницу'}</small>
            </div>
        `;
    }
}

// === ЗАГРУЗКА ЗАКАЗОВ НА ДИЗАЙН ===
async function loadDesignOrders() {
    const container = document.getElementById('designOrdersList');
    if (!container) return;

    try {
        const res = await apiFetch('/api/design-orders');
        if (!res.ok) throw new Error();
        const orders = await res.json();

        if (!orders || orders.length === 0) {
            container.innerHTML = `
                <div class="orders-empty-state">
                    <i class="bi bi-palette"></i>
                    <p>Заказов на дизайн пока нет</p>
                </div>
            `;
            return;
        }

        container.innerHTML = orders.map((order, index) => {
            const statusClass = getStatusClass(order.status);
            const statusText = getStatusText(order.status);
            const price = getDesignPrice(order.tariff);
            
            const orderDate = order.createdAt ? new Date(order.createdAt).toLocaleDateString('ru-RU', {
                day: '2-digit',
                month: '2-digit',
                year: 'numeric',
                hour: '2-digit',
                minute: '2-digit'
            }) : '-';
            
            // Отображение фото
            let photosHtml = '';
            if (order.photos && order.photos.length > 0) {
                photosHtml = `
                    <div class="order-photos mt-2">
                        <strong><i class="bi bi-images"></i> Фотографии помещения:</strong>
                        <div class="order-photos-list">
                            ${order.photos.map(photo => `
                                <a href="${API_BASE_URL}${photo}" target="_blank" class="order-photo-link">
                                    <img src="${API_BASE_URL}${photo}" alt="Фото помещения" class="order-photo-thumb">
                                </a>
                            `).join('')}
                        </div>
                    </div>
                `;
            }

            return `
                <div class="order-card">
                    <div class="order-card-header">
                        <span class="order-number">
                            Заказ дизайна #${order.id}
                            ${order.id ? `<span class="order-db-id">(ID: ${order.id})</span>` : ''}
                        </span>
                        <span class="order-status ${statusClass}">${statusText}</span>
                    </div>
                    
                    <div class="order-details">
                        <div><strong><i class="bi bi-door-open"></i> Тип помещения:</strong> ${escapeHtml(order.roomType || '-')}</div>
                        <div><strong><i class="bi bi-palette"></i> Стиль дизайна:</strong> ${escapeHtml(order.designStyle || '-')}</div>
                        <div><strong><i class="bi bi-star"></i> Тариф:</strong> ${getTariffText(order.tariff)}</div>
                        <div class="mt-2 fw-bold text-primary">
                            <strong><i class="bi bi-currency-dollar"></i> Стоимость:</strong> ${formatNumber(price)} BYN
                        </div>
                        ${order.preferences ? `
                            <div class="order-preferences mt-2">
                                <strong><i class="bi bi-chat-left-text"></i> Пожелания:</strong>
                                <p class="mb-0 small">${escapeHtml(order.preferences)}</p>
                            </div>
                        ` : ''}
                        ${order.selectedProducts ? `
                            <div class="order-products mt-2">
                                <strong><i class="bi bi-bag"></i> Выбранные товары:</strong>
                                <p class="mb-0 small">${escapeHtml(order.selectedProducts)}</p>
                            </div>
                        ` : ''}
                        ${photosHtml}
                        <div class="order-address mt-2">
                            <div><strong><i class="bi bi-geo-alt"></i> Адрес:</strong> ${escapeHtml(order.address || '-')}</div>
                        </div>
                        <div class="order-date mt-1">
                            <small class="text-muted">
                                <i class="bi bi-calendar"></i> 
                                Дата заказа: ${orderDate}
                            </small>
                        </div>
                    </div>
                </div>
            `;
        }).join('');

    } catch (e) {
        console.error('Ошибка загрузки заказов на дизайн:', e);
        container.innerHTML = `
            <div class="orders-empty-state">
                <i class="bi bi-exclamation-triangle"></i>
                <p>Ошибка загрузки заказов</p>
                <small class="text-muted">${e.message || 'Попробуйте обновить страницу'}</small>
            </div>
        `;
    }
}

function formatNumber(value) {
    if (value === null || value === undefined) return '0';
    return Number(value).toLocaleString('ru-RU', {
        minimumFractionDigits: 2,
        maximumFractionDigits: 2
    });
}

// === ЗАГРУЗКА КАМПАНИЙ ===
async function loadUserCampaigns() {
    const content = document.getElementById('campaignsTabContent');
    if (!content) return;

    try {
        console.log('[Campaigns] Loading campaigns...');
        const response = await apiFetch('/api/marketing/campaigns/my');
        console.log('[Campaigns] Response status:', response.status);
        
        if (!response.ok) {
            console.error('[Campaigns] Response not ok');
            content.innerHTML = '<div class="text-center text-muted p-4">Ошибка загрузки предложений</div>';
            return;
        }

        const campaigns = await response.json();
        console.log('[Campaigns] Campaigns loaded:', campaigns.length);

        if (campaigns.length === 0) {
            content.innerHTML = `
                <div class="campaign-section-empty">
                    <i class="bi bi-gift"></i>
                    <p>Нет активных предложений</p>
                    <small class="text-muted">Следите за обновлениями!</small>
                </div>
            `;
            return;
        }

        const autoCampaigns = campaigns.filter(c => c.automated === true);
        const manualCampaigns = campaigns.filter(c => c.automated !== true);

        let html = '';

        if (autoCampaigns.length > 0) {
            html += `<h6 class="text-success mb-3"><i class="bi bi-magic"></i> Автоматические скидки</h6>`;
            autoCampaigns.forEach(c => {
                html += renderCampaignCard(c, true);
            });
        }

        if (manualCampaigns.length > 0) {
            html += `<h6 class="text-primary mb-3 mt-4"><i class="bi bi-ticket-perforated"></i> Промокоды</h6>`;
            manualCampaigns.forEach(c => {
                html += renderCampaignCard(c, false);
            });
        }

        content.innerHTML = html;

    } catch (error) {
        console.error('[Campaigns] Error:', error);
        content.innerHTML = '<div class="text-center text-danger p-4">Ошибка загрузки</div>';
    }
}

function renderCampaignCard(campaign, isAutomated) {
    const discountBadge = campaign.discountPercent 
        ? `<span class="campaign-discount-badge">-${campaign.discountPercent}%<span>СКИДКА</span></span>` 
        : '';

    let conditions = '';
    if (campaign.minOrderAmount) {
        conditions += `<div class="small text-muted"><i class="bi bi-cart-check"></i> Мин. сумма: ${campaign.minOrderAmount} BYN</div>`;
    }
    if (campaign.validDays) {
        conditions += `<div class="small text-muted"><i class="bi bi-clock"></i> Действует: ${campaign.validDays} дней</div>`;
    }

    let actionHtml;
    if (isAutomated) {
        actionHtml = `
            <div class="alert alert-success py-2 mb-0 mt-2">
                <i class="bi bi-check-circle-fill"></i> Скидка применяется автоматически
            </div>
        `;
    } else {
        actionHtml = `
            <button class="campaign-apply-btn w-100 mt-3" 
                    onclick="applyCampaign(${campaign.id}, '${escapeHtml(campaign.name).replace(/'/g, "\\'")}', ${campaign.discountPercent || 0})" 
                    id="campaign-btn-${campaign.id}">
                <i class="bi bi-check-circle"></i> Использовать промокод
            </button>
        `;
    }

    return `
        <div class="campaign-card">
            <div class="d-flex justify-content-between align-items-start mb-2">
                <h6 class="mb-0 fw-bold text-dark">${escapeHtml(campaign.name)}</h6>
                ${discountBadge}
            </div>
            <p class="text-muted small mb-2">${escapeHtml(campaign.description || '')}</p>
            ${conditions}
            ${actionHtml}
        </div>
    `;
}

// === ПРИМЕНЕНИЕ КАМПАНИИ ===
async function applyCampaign(campaignId, campaignName, discountPercent) {
    const btn = document.getElementById(`campaign-btn-${campaignId}`);
    if (btn) {
        btn.disabled = true;
        btn.innerHTML = '<i class="bi bi-hourglass-split"></i> Применение...';
    }

    try {
        const response = await apiFetch(`/api/marketing/campaigns/apply/${campaignId}`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' }
        });

        if (response.status === 401 || response.status === 403) {
            showNotification('Сессия истекла. Войдите снова.', 'error');
            localStorage.removeItem('design_studio_token');
            localStorage.removeItem('design_studio_user');
            setTimeout(() => window.location.href = 'login.html', 2000);
            return;
        }

        const result = await response.json();

        if (result.success) {
            localStorage.setItem('activePromo', JSON.stringify({
                id: campaignId,
                name: campaignName,
                discountPercent: discountPercent
            }));

            showNotification(`Промокод "${campaignName}" применён! Скидка ${discountPercent}%`, 'success');
            
            checkActivePromo();
            loadUserCampaigns();
            loadCart();
        } else {
            showNotification(result.message || 'Не удалось применить промокод', 'error');
            if (btn) {
                btn.disabled = false;
                btn.innerHTML = '<i class="bi bi-check-circle"></i> Использовать промокод';
            }
        }

    } catch (error) {
        console.error('Ошибка применения кампании:', error);
        showNotification('Ошибка сервера при применении промокода', 'error');
        if (btn) {
            btn.disabled = false;
            btn.innerHTML = '<i class="bi bi-check-circle"></i> Использовать промокод';
        }
    }
}

// === СМЕНА ПАРОЛЯ ===
function initPasswordForm() {
    const form = document.getElementById('changePasswordForm');
    if (!form) return;

    form.addEventListener('submit', async (e) => {
        e.preventDefault();

        const oldPassword = form.querySelector('[name="oldPassword"]')?.value;
        const newPassword = form.querySelector('[name="newPassword"]')?.value;
        const confirmPassword = form.querySelector('[name="confirmPassword"]')?.value;

        // Validation
        if (!oldPassword) {
            showNotification('Введите текущий пароль', 'error');
            return;
        }

        if (!newPassword) {
            showNotification('Введите новый пароль', 'error');
            return;
        }

        if (newPassword.length < 3) {
            showNotification('Пароль должен содержать минимум 3 символа', 'error');
            return;
        }

        if (!confirmPassword) {
            showNotification('Подтвердите новый пароль', 'error');
            return;
        }

        if (newPassword !== confirmPassword) {
            showNotification('Новый пароль и подтверждение не совпадают', 'error');
            return;
        }

        const btn = form.querySelector('button[type="submit"]');
        btn.disabled = true;
        btn.innerHTML = '<i class="bi bi-hourglass-split"></i> Смена...';

        try {
            const res = await apiFetch('/api/user/password', {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    currentPassword: oldPassword,
                    newPassword: newPassword,
                    confirmPassword: confirmPassword
                })
            });

            const result = await res.json();
            console.log('[PasswordChange] Result:', result);

            if (res.ok && result.success) {
                showNotification('Пароль успешно изменён!', 'success');
                form.reset();
                setTimeout(() => closePasswordModal(), 1500);
            } else {
                showNotification(result.error || result.message || 'Ошибка смены пароля', 'error');
                if (btn) {
                    btn.disabled = false;
                    btn.innerHTML = '<i class="bi bi-check-circle"></i> Изменить';
                }
            }
        } catch (e) {
            console.error('Ошибка смены пароля:', e);
            showNotification('Ошибка сервера при смене пароля', 'error');
            if (btn) {
                btn.disabled = false;
                btn.innerHTML = '<i class="bi bi-check-circle"></i> Изменить';
            }
        }
    });
}

// === ВСПОМОГАТЕЛЬНЫЕ ФУНКЦИИ ===
function getStatusClass(status) {
    switch (status) {
        case 'COMPLETED':
        case 'DELIVERED':
            return 'completed';
        case 'PROCESSING':
        case 'ASSEMBLING':
        case 'SHIPPING':
            return 'processing';
        case 'IN_PROGRESS':
            return 'in_progress';
        case 'PENDING':
        default:
            return 'pending';
    }
}

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

function getTariffText(tariff) {
    const tariffMap = {
        'basic': 'Базовый',
        'standard': 'Стандарт',
        'premium': 'Премиум'
    };
    return tariffMap[tariff] || tariff;
}

function getDesignPrice(tariff) {
    switch (tariff) {
        case 'basic': return 1000;
        case 'standard': return 1800;
        case 'premium': return 2500;
        default: return 1000;
    }
}

function escapeHtml(text) {
    if (!text) return '';
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

// === УВЕДОМЛЕНИЯ ===
function showNotification(message, type = 'info') {
    let container = document.getElementById('notificationContainer');
    if (!container) {
        container = document.createElement('div');
        container.id = 'notificationContainer';
        container.style.cssText = `
            position: fixed;
            top: 20px;
            right: 20px;
            z-index: 9999;
            display: flex;
            flex-direction: column;
            gap: 10px;
            max-width: 400px;
        `;
        document.body.appendChild(container);
    }

    const notification = document.createElement('div');
    notification.style.cssText = `
        display: flex;
        align-items: center;
        gap: 12px;
        padding: 14px 18px;
        background: white;
        border-radius: 10px;
        box-shadow: 0 4px 15px rgba(0, 0, 0, 0.15);
        transform: translateX(120%);
        transition: transform 0.3s ease;
        border-left: 4px solid;
        font-weight: 500;
    `;

    const colors = {
        success: '#28a745',
        error: '#dc3545',
        warning: '#ffc107',
        info: '#17a2b8'
    };

    const icons = {
        success: 'check-circle-fill',
        error: 'exclamation-circle-fill',
        warning: 'exclamation-triangle-fill',
        info: 'info-circle-fill'
    };

    notification.style.borderLeftColor = colors[type] || colors.info;

    notification.innerHTML = `
        <i class="bi bi-${icons[type] || icons.info}" style="color: ${colors[type] || colors.info}; font-size: 1.3rem;"></i>
        <span style="flex: 1;">${message}</span>
        <button onclick="this.parentElement.remove()" style="
            background: none;
            border: none;
            cursor: pointer;
            padding: 4px;
            color: #999;
            font-size: 1.2rem;
        ">
            <i class="bi bi-x"></i>
        </button>
    `;

    container.appendChild(notification);

    requestAnimationFrame(() => {
        notification.style.transform = 'translateX(0)';
    });

    setTimeout(() => {
        if (notification.parentElement) {
            notification.style.transform = 'translateX(120%)';
            setTimeout(() => {
                if (notification.parentElement) {
                    notification.remove();
                }
            }, 300);
        }
    }, 4000);
}

// ==================== ЗАГРУЗКА СООБЩЕНИЙ ====================

async function loadUserMessages() {
    const container = document.getElementById('messagesList');
    if (!container) {
        console.error('Container messagesList not found!');
        return;
    }
    
    console.log('Loading user messages...');
    
    try {
        const response = await apiFetch('/api/marketing/messages/my');
        console.log('Response status:', response.status);
        console.log('Response headers:', response.headers);
        
        if (!response.ok) {
            console.error('Ошибка загрузки сообщений, status:', response.status);
            container.innerHTML = '<div class="text-center text-danger p-3">Ошибка загрузки сообщений (status ' + response.status + ')</div>';
            return;
        }
        
        const messages = await response.json();
        console.log('Messages loaded:', messages);
        console.log('Messages count:', messages.length);
        
        if (!messages || messages.length === 0) {
            container.innerHTML = `
                <div class="campaign-section-empty">
                    <i class="bi bi-envelope"></i>
                    <p>У вас нет сообщений</p>
                    <small class="text-muted">Здесь будут отображаться уведомления от системы</small>
                </div>
            `;
            return;
        }
        
        // Обновляем счетчик непрочитанных в шапке
        updateMessagesBadge(messages);
        
        container.innerHTML = messages.map(message => `
            <div class="message-card ${!message.isRead ? 'unread' : ''}" data-message-id="${message.id}">
                <div class="message-header">
                    <div class="message-info">
                        <div class="message-type ${message.type}">
                            <i class="bi ${getMessageIcon(message.type)}"></i>
                            ${getMessageTypeText(message.type)}
                        </div>
                        <div class="message-date">${formatDateTime(message.sentAt)}</div>
                    </div>
                    ${!message.isRead ? '<span class="unread-badge">Новое</span>' : ''}
                </div>
                <div class="message-subject">${escapeHtml(message.subject)}</div>
                <div class="message-content">${escapeHtml(message.content).replace(/\n/g, '<br>')}</div>
                ${!message.isRead ? `
                    <button class="message-mark-read-btn" onclick="markMessageAsRead(${message.id})">
                        <i class="bi bi-envelope-open"></i> Отметить как прочитанное
                    </button>
                ` : ''}
            </div>
        `).join('');
        
    } catch (error) {
        console.error('Ошибка загрузки сообщений:', error);
        container.innerHTML = '<div class="text-center text-danger p-3">Ошибка загрузки сообщений: ' + error.message + '</div>';
    }
}

async function updateMessagesBadge(messages) {
    const unreadCount = messages.filter(m => !m.isRead).length;
    const badge = document.getElementById('messagesUnreadBadge');
    if (badge) {
        if (unreadCount > 0) {
            badge.textContent = unreadCount;
            badge.style.display = 'inline-block';
        } else {
            badge.style.display = 'none';
        }
    }
}

async function markMessageAsRead(messageId) {
    try {
        const response = await apiFetch(`/api/marketing/messages/${messageId}/read`, {
            method: 'PUT'
        });
        
        if (response.ok) {
            await loadUserMessages();
            // Обновляем счетчик в шапке
            const badge = document.getElementById('messagesUnreadBadge');
            if (badge) {
                const currentCount = parseInt(badge.textContent) || 0;
                if (currentCount > 0) {
                    badge.textContent = currentCount - 1;
                    if (badge.textContent === '0') {
                        badge.style.display = 'none';
                    }
                }
            }
            showNotification('Сообщение отмечено как прочитанное', 'success');
        }
    } catch (error) {
        console.error('Error marking message as read:', error);
        showNotification('Ошибка при отметке сообщения', 'error');
    }
}

function getMessageIcon(type) {
    const icons = {
        'first_purchase': 'bi-gift-fill',
        'birthday': 'bi-cake2',
        'reactivation': 'bi-bell-fill',
        'anniversary': 'bi-award-fill',
        'promo': 'bi-ticket-perforated-fill',
        'welcome': 'bi-emoji-smile'
    };
    return icons[type] || 'bi-envelope-fill';
}

function getMessageTypeText(type) {
    const texts = {
        'first_purchase': 'Первая покупка',
        'birthday': 'День рождения',
        'reactivation': 'Реактивация',
        'anniversary': 'Годовщина',
        'promo': 'Акция',
        'welcome': 'Приветствие'
    };
    return texts[type] || 'Уведомление';
}

function formatDateTime(dateStr) {
    if (!dateStr) return '-';
    const date = new Date(dateStr);
    return date.toLocaleDateString('ru-RU', {
        day: '2-digit',
        month: '2-digit',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
    });
}

// === ЭКСПОРТ ФУНКЦИЙ ===
window.loadProfile = loadProfile;
window.loadDiscountInfo = loadDiscountInfo;
window.loadCart = loadCart;
window.loadProductOrders = loadProductOrders;
window.loadDesignOrders = loadDesignOrders;
window.loadUserCampaigns = loadUserCampaigns;
window.applyCampaign = applyCampaign;
window.updateQuantity = updateQuantity;
window.removeFromCart = removeFromCart;
window.showNotification = showNotification;
window.checkActivePromo = checkActivePromo;
window.formatBYN = formatBYN;

console.log('profile.js загружен успешно');