// File: src/js/checkout.js

const API_BASE_URL = 'http://localhost:8080';

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
    if (!imagePath || imagePath === 'null' || imagePath === 'undefined') {
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
    loadUserDiscount();
    loadCartItems();
    loadActivePromo(); // НОВОЕ: загрузка активного промокода
    initCardFormatting();
    initCheckoutForm();
});

// === НОВОЕ: Загрузка активного промокода ===
async function loadActivePromo() {
    const promoSection = document.getElementById('promoSection');
    const promoInput = document.getElementById('promoCode');
    const activePromo = JSON.parse(localStorage.getItem('activePromo') || 'null');
    
    if (!promoSection) return;
    
    if (activePromo) {
        promoSection.innerHTML = `
            <div class="alert alert-success">
                <i class="bi bi-ticket-perforated"></i>
                Применён промокод: <strong>${escapeHtml(activePromo.name)}</strong>
                <span class="badge bg-danger ms-2">-${activePromo.discountPercent}%</span>
                <button class="btn btn-sm btn-outline-danger ms-2" onclick="clearPromo()">Убрать</button>
            </div>
        `;
        if (promoInput) promoInput.value = activePromo.name;
    } else {
        promoSection.innerHTML = `
            <div class="input-group">
                <input type="text" class="form-control" id="promoCode" placeholder="Введите промокод">
                <button class="btn btn-outline-success" type="button" onclick="applyPromoCode()">
                    Применить
                </button>
            </div>
        `;
    }
}

// === НОВОЕ: Применить промокод вручную ===
async function applyPromoCode() {
    const input = document.getElementById('promoCode');
    const code = input ? input.value.trim() : '';
    
    if (!code) {
        showToast('Введите промокод', 'warning');
        return;
    }
    
    try {
        // Загружаем кампании пользователя и ищем по имени
        const res = await apiFetch('/api/marketing/campaigns/my');
        if (!res.ok) {
            showToast('Ошибка загрузки промокодов', 'error');
            return;
        }
        
        const campaigns = await res.json();
        const campaign = campaigns.find(c => 
            c.name.toLowerCase() === code.toLowerCase() || 
            c.id.toString() === code
        );
        
        if (!campaign) {
            showToast('Промокод не найден или недоступен', 'error');
            return;
        }
        
        // Применяем через API
        const applyRes = await apiFetch(`/api/marketing/campaigns/${campaign.id}/apply`, {
            method: 'POST'
        });
        
        const result = await applyRes.json();
        
        if (result.success) {
            localStorage.setItem('activePromo', JSON.stringify({
                id: campaign.id,
                name: campaign.name,
                discountPercent: campaign.discountPercent || 0
            }));
            
            showToast(`Промокод "${campaign.name}" применён!`, 'success');
            loadActivePromo();
            loadCartItems(); // Пересчитываем сумму
        } else {
            showToast(result.message || 'Не удалось применить промокод', 'error');
        }
        
    } catch (e) {
        console.error('Ошибка применения промокода:', e);
        showToast('Ошибка сервера', 'error');
    }
}

// === НОВОЕ: Убрать промокод ===
function clearPromo() {
    localStorage.removeItem('activePromo');
    showToast('Промокод убран', 'info');
    loadActivePromo();
    loadCartItems();
}

// Загрузка скидки пользователя
async function loadUserDiscount() {
    const discountInfo = document.getElementById('discountInfo');
    
    try {
        const res = await apiFetch('/api/user/me');
        if (!res.ok) throw new Error();
        
        const user = await res.json();
        const discount = (user.discount || 0) * 100;
        
        if (discount > 0) {
            discountInfo.innerHTML = `
                <span class="user-discount-badge">
                    <i class="bi bi-percent"></i>
                    Ваша скидка: ${discount}%
                </span>
            `;
            document.getElementById('discountPercent').textContent = discount;
            document.getElementById('discountRow').style.display = 'flex';
        }
    } catch (e) {
        console.error('Ошибка загрузки скидки:', e);
    }
}

// Загрузка товаров корзины (с учётом промокода)
async function loadCartItems() {
    const container = document.getElementById('cartItemsContainer');
    const summary = document.getElementById('orderSummary');
    
    try {
        const res = await apiFetch('/api/cart');
        if (!res.ok) throw new Error();
        
        const items = await res.json();
        
        if (!items || items.length === 0) {
            container.innerHTML = `
                <div class="text-center p-5">
                    <i class="bi bi-cart-x" style="font-size: 3rem; color: #ccc;"></i>
                    <p class="mt-3 text-muted">Корзина пуста</p>
                    <a href="catalog.html" class="btn btn-primary">В каталог</a>
                </div>
            `;
            document.getElementById('submitBtn').disabled = true;
            return;
        }
        
        let html = '';
        let total = 0;
        
        items.forEach(item => {
            const itemTotal = item.productPrice * item.quantity;
            total += itemTotal;
            
            html += `
                <div class="cart-item">
                    <img src="${getImageUrl(item.productImage)}" alt="${item.productName}" class="cart-item-image" onerror="this.src='src/images/default.jpg'">
                    <div class="cart-item-info">
                        <div class="cart-item-name">${item.productName}</div>
                        <div class="cart-item-price">${item.productPrice.toLocaleString('ru-RU')} BYN</div>
                        <div class="cart-item-qty">Кол-во: ${item.quantity}</div>
                    </div>
                    <div class="cart-item-total">${itemTotal.toLocaleString('ru-RU')} BYN</div>
                </div>
            `;
        });
        
        container.innerHTML = html;
        summary.style.display = 'block';
        
        // Рассчитываем скидки
        const userDiscountPercent = parseFloat(document.getElementById('discountPercent').textContent) || 0;
        const activePromo = JSON.parse(localStorage.getItem('activePromo') || 'null');
        const promoDiscountPercent = activePromo ? (activePromo.discountPercent || 0) : 0;
        
        // Общая скидка (складываем или берём максимальную - настройте логику)
        const totalDiscountPercent = Math.min(userDiscountPercent + promoDiscountPercent, 100);
        
                const userDiscountAmount = total * (userDiscountPercent / 100);
        const promoDiscountAmount = total * (promoDiscountPercent / 100);
        const totalDiscount = userDiscountAmount + promoDiscountAmount;
        const finalTotal = total - totalDiscount;
        
        document.getElementById('summarySubtotal').textContent = total.toLocaleString('ru-RU') + ' BYN';
        document.getElementById('summaryDiscount').textContent = '-' + totalDiscount.toLocaleString('ru-RU') + ' BYN';
        document.getElementById('summaryTotal').textContent = finalTotal.toLocaleString('ru-RU') + ' BYN';
        
        // Показываем детали скидок
        let discountDetails = '';
        if (userDiscountPercent > 0) {
            discountDetails += `<div class="small text-muted">Ваша скидка: ${userDiscountPercent}% (-${userDiscountAmount.toLocaleString('ru-RU')} BYN)</div>`;
        }
        if (promoDiscountPercent > 0) {
            discountDetails += `<div class="small text-success"><i class="bi bi-ticket-perforated"></i> Промокод: ${promoDiscountPercent}% (-${promoDiscountAmount.toLocaleString('ru-RU')} BYN)</div>`;
        }
        
        const discountDetailsEl = document.getElementById('discountDetails');
        if (discountDetailsEl) {
            discountDetailsEl.innerHTML = discountDetails;
        }
        
    } catch (e) {
        console.error('Ошибка загрузки корзины:', e);
        container.innerHTML = '<div class="text-center p-5 text-danger">Ошибка загрузки корзины</div>';
    }
}

// Форматирование карты
function initCardFormatting() {
    const cardNumber = document.querySelector('input[name="cardNumber"]');
    const expiry = document.querySelector('input[name="expiry"]');
    
    if (cardNumber) {
        cardNumber.addEventListener('input', (e) => {
            let value = e.target.value.replace(/\D/g, '');
            let formatted = '';
            for (let i = 0; i < value.length && i < 16; i++) {
                if (i > 0 && i % 4 === 0) formatted += ' ';
                formatted += value[i];
            }
            e.target.value = formatted;
        });
    }
    
    if (expiry) {
        expiry.addEventListener('input', (e) => {
            let value = e.target.value.replace(/\D/g, '');
            if (value.length >= 2) {
                value = value.substring(0, 2) + '/' + value.substring(2, 4);
            }
            e.target.value = value;
        });
    }
}

// Отправка формы (с учётом промокода)
function initCheckoutForm() {
    const form = document.getElementById('checkoutForm');
    const submitBtn = document.getElementById('submitBtn');
    
    if (!form) return;
    
    form.addEventListener('submit', async (e) => {
        e.preventDefault();
        
        const formData = new FormData(form);
        
        // Добавляем промокод если есть
        const activePromo = JSON.parse(localStorage.getItem('activePromo') || 'null');
        if (activePromo) {
            formData.append('campaignId', activePromo.id);
            formData.append('campaignDiscount', activePromo.discountPercent);
        }
        
        submitBtn.disabled = true;
        submitBtn.innerHTML = '<i class="bi bi-hourglass-split"></i> Оформление...';
        
        try {
            const res = await apiFetch('/api/checkout', {
                method: 'POST',
                body: formData
            });
            
            if (res.ok) {
                const data = await res.json();
                // Очищаем промокод после успешного заказа
                localStorage.removeItem('activePromo');
                showToast('Заказ оформлен успешно!');
                setTimeout(() => {
                    window.location.href = 'profile.html';
                }, 2000);
            } else {
                const error = await res.json();
                showToast(error.error || 'Ошибка оформления заказа', true);
            }
        } catch (e) {
            console.error('Ошибка оформления:', e);
            showToast('Ошибка сети', true);
        } finally {
            submitBtn.disabled = false;
            submitBtn.innerHTML = '<i class="bi bi-check-circle"></i> Подтвердить заказ';
        }
    });
}

function showToast(message, isError = false) {
    const toast = document.createElement('div');
    toast.className = `custom-toast ${isError ? 'error' : ''}`;
    toast.innerHTML = `<span>${message}</span>`;
    document.body.appendChild(toast);
    setTimeout(() => toast.classList.add('show'), 10);
    setTimeout(() => {
        toast.classList.remove('show');
        setTimeout(() => toast.remove(), 500);
    }, 3000);
}

function escapeHtml(text) {
    if (!text) return '';
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

function handleLogout(event) {
    event.preventDefault();
    localStorage.removeItem('design_studio_user');
    localStorage.removeItem('design_studio_token');
    localStorage.removeItem('activePromo');
    window.location.href = 'index.html';
}