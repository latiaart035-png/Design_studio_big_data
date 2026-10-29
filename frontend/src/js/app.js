// src/js/app.js
document.addEventListener('DOMContentLoaded', () => {
    console.log('app.js loaded on page');

    const API_BASE_URL = 'http://localhost:8080';
    const USER_STORAGE_KEY = 'design_studio_user';
    const TOKEN_KEY = 'design_studio_token';

    // ==========================================
    // БАЗОВЫЕ ФУНКЦИИ
    // ==========================================

    async function apiFetch(path, options = {}) {
        const token = localStorage.getItem(TOKEN_KEY);
        
        const headers = {
            ...(options.headers || {})
        };
        
        if (token) {
            headers['Authorization'] = 'Bearer ' + token;
        }
        
        const response = await fetch(`${API_BASE_URL}${path}`, {
            ...options,
            headers: headers
        });
        
        // НЕ перенаправляем при ошибке 401
        // if (response.status === 401) {
        //     localStorage.removeItem(TOKEN_KEY);
        //     window.location.href = 'login.html';
        //     return null;
        // }
        
        return response;
    }

    function showToast(message, type = 'success') {
        const toast = document.createElement('div');
        toast.className = 'custom-toast';
        
        const colors = {
            'success': '#28a745',
            'danger': '#dc3545',
            'warning': '#ffc107',
            'info': '#17a2b8'
        };
        toast.style.backgroundColor = colors[type] || colors.success;
        
        toast.innerHTML = `
            <span>${message}</span>
            <button class="close-btn">&times;</button>
        `;
        document.body.appendChild(toast);

        setTimeout(() => toast.classList.add('show'), 10);

        setTimeout(() => {
            toast.classList.remove('show');
            setTimeout(() => {
                if (toast.parentNode) toast.parentNode.removeChild(toast);
            }, 500);
        }, 4000);

        toast.querySelector('.close-btn').addEventListener('click', () => {
            toast.classList.remove('show');
            setTimeout(() => {
                if (toast.parentNode) toast.parentNode.removeChild(toast);
            }, 500);
        });
    }

    function saveUserToStorage(user) {
        if (user) {
            localStorage.setItem(USER_STORAGE_KEY, JSON.stringify(user));
        } else {
            localStorage.removeItem(USER_STORAGE_KEY);
        }
    }

    function getUserFromStorage() {
        const stored = localStorage.getItem(USER_STORAGE_KEY);
        return stored ? JSON.parse(stored) : null;
    }

    function saveToken(token) {
        if (token) {
            localStorage.setItem(TOKEN_KEY, token);
        } else {
            localStorage.removeItem(TOKEN_KEY);
        }
    }

    function getToken() {
        return localStorage.getItem(TOKEN_KEY);
    }

    // ==========================================
    // ФУНКЦИИ ФОРМАТИРОВАНИЯ - ИСПРАВЛЕНО
    // ==========================================

    /**
     * Форматирование валюты в BYN (Белорусские рубли)
     * @param {number} value - Сумма для форматирования
     * @returns {string} Отформатированная сумма с символом BYN
     */
    function formatCurrency(value) {
        if (value === null || value === undefined || isNaN(value)) return '0 BYN';
        return new Intl.NumberFormat('ru-RU', {
            style: 'currency',
            currency: 'BYN',
            minimumFractionDigits: 0,
            maximumFractionDigits: 0
                    }).format(value);
    }

    /**
     * Форматирование числа с разделителями
     * @param {number} value - Число для форматирования
     * @returns {string} Отформатированное число
     */
    function formatNumber(value) {
        return new Intl.NumberFormat('ru-RU').format(value || 0);
    }

    /**
     * Форматирование даты и времени
     * @param {string} dateStr - Строка с датой
     * @returns {string} Отформатированная дата
     */
    function formatDate(dateStr) {
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

    /**
     * Форматирование только даты
     * @param {string} dateStr - Строка с датой
     * @returns {string} Отформатированная дата
     */
    function formatDateOnly(dateStr) {
        if (!dateStr) return '-';
        const date = new Date(dateStr);
        return date.toLocaleDateString('ru-RU', {
            day: '2-digit',
            month: '2-digit',
            year: 'numeric'
        });
    }

    // ==========================================
    // ПОЛУЧЕНИЕ ИНФОРМАЦИИ О СКИДКЕ
    // ==========================================

    async function fetchDiscountInfo() {
        const token = getToken();
        if (!token) return null;
        
        try {
            const response = await apiFetch('/api/user/discount');
            if (response.ok) {
                return await response.json();
            }
        } catch (e) {
            console.error('fetchDiscountInfo error:', e);
        }
        return null;
    }

    // ==========================================
    // СМЕНА ПАРОЛЯ
    // ==========================================

    async function changePassword(currentPassword, newPassword, confirmPassword) {
        if (newPassword !== confirmPassword) {
            throw new Error('Новый пароль и подтверждение не совпадают');
        }

        if (newPassword.length < 3) {
            throw new Error('Пароль должен содержать минимум 3 символов');
        }

        const response = await apiFetch('/api/user/password', {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                currentPassword: currentPassword,
                newPassword: newPassword,
                confirmPassword: confirmPassword
            })
        });

        const result = await response.json();

        if (!response.ok) {
            throw new Error(result.error || result.message || 'Ошибка смены пароля');
        }

        return result;
    }

    // ==========================================
    // ЗАГРУЗКА BADGE КАМПАНИЙ
    // ==========================================

    async function loadCampaignBadge() {
        const badge = document.getElementById('campaignBadge');
        if (!badge) return;
        
        const token = getToken();
        if (!token) {
            badge.style.display = 'none';
            return;
        }
        
        try {
            const res = await apiFetch('/api/marketing/campaigns/my');
            if (res.ok) {
                const campaigns = await res.json();
                if (campaigns.length > 0) {
                    badge.textContent = campaigns.length > 99 ? '99+' : campaigns.length;
                    badge.style.display = 'inline-block';
                } else {
                    badge.style.display = 'none';
                }
            }
        } catch (e) {
            console.error('Error loading campaign badge:', e);
            badge.style.display = 'none';
        }
    }

    // ==========================================
    // ЗАГРУЗКА BADGE КОРЗИНЫ
    // ==========================================

    async function loadCartBadge() {
        const badge = document.getElementById('cartBadge');
        if (!badge) return;
        
        try {
            const res = await apiFetch('/api/cart/count');
            if (res.ok) {
                const data = await res.json();
                const count = data.count || 0;
                if (count > 0) {
                    badge.textContent = count > 99 ? '99+' : count;
                    badge.style.display = '';
                } else {
                    badge.style.display = 'none';
                }
            }
        } catch (e) {
            console.error('Error loading cart badge:', e);
        }
    }

    // ==========================================
    // ПОЛУЧЕНИЕ ПОЛЬЗОВАТЕЛЯ
    // ==========================================

    async function fetchCurrentUser() {
        const storedUser = getUserFromStorage();
        const token = getToken();
        
        if (!token) {
            saveUserToStorage(null);
            return null;
        }
        
        try {
            const response = await apiFetch('/api/user/me');
            
            if (!response.ok) {
                if (response.status === 401) {
                    saveUserToStorage(null);
                    saveToken(null);
                }
                return null;
            }
            
            const user = await response.json();
            saveUserToStorage(user);
            return user;
            
        } catch (e) {
            console.error('fetchCurrentUser error:', e);
            return storedUser;
        }
    }

    // ==========================================
    // ОБНОВЛЕНИЕ НАВИГАЦИИ
    // ==========================================

    async function updateHeaderAuth() {
        const user = await fetchCurrentUser();

        const navRegister = document.getElementById('navRegister');
        const navProfile = document.getElementById('navProfile');
        const navCrm = document.getElementById('navCrm');
        const navMarketing = document.getElementById('navMarketing');
        const navAnalytics = document.getElementById('navAnalytics');
        const navLogout = document.getElementById('navLogout');

        if (!user) {
            // Гость
            if (navRegister) navRegister.style.display = '';
            if (navProfile) navProfile.style.display = 'none';
            if (navCrm) navCrm.style.display = 'none';
            if (navMarketing) navMarketing.style.display = 'none';
            if (navAnalytics) navAnalytics.style.display = 'none';
            
            if (navLogout) {
                navLogout.style.display = '';
                navLogout.textContent = 'ВОЙТИ';
                navLogout.href = 'login.html';
                navLogout.onclick = null;
            }
            return;
        }

        // Пользователь авторизован
        if (navRegister) navRegister.style.display = 'none';
        
        if (navProfile) {
            navProfile.style.display = '';
            
            if (user.role === 'ADMIN') {
                navProfile.href = 'admin.html';
            } else {
                navProfile.href = 'profile.html';
            }
        }
        
        if (user.role === 'ADMIN') {
            if (navCrm) navCrm.style.display = '';
            if (navMarketing) navMarketing.style.display = '';
            if (navAnalytics) navAnalytics.style.display = '';
        } else {
            if (navCrm) navCrm.style.display = 'none';
            if (navMarketing) navMarketing.style.display = 'none';
            if (navAnalytics) navAnalytics.style.display = 'none';
        }
        
        if (navLogout) {
            navLogout.style.display = '';
            navLogout.textContent = 'ВЫЙТИ';
            navLogout.href = '#';
            navLogout.onclick = async (e) => {
                e.preventDefault();
                try {
                    await apiFetch('/api/auth/logout', { method: 'POST' });
                } catch (err) {
                    console.error('Logout error:', err);
                }
                saveUserToStorage(null);
                saveToken(null);
                localStorage.removeItem('activePromo');
                window.location.href = 'index.html';
            };
        }
        
        loadCampaignBadge();
    }

    // ==========================================
    // ЗАЩИТА СТРАНИЦ
    // ==========================================

    async function protectPages() {
        const path = window.location.pathname.split('/').pop() || 'index.html';
        
        const protectedPages = [
            'profile.html', 
            'checkout.html', 
            'order.html',
            'orders.html',
            'cart.html'
        ];
        
        const adminPages = [
            'admin.html', 
            'analytics.html',
            'crm.html',
            'marketing.html'
        ];

        const user = await fetchCurrentUser();

        if (adminPages.includes(path)) {
            if (!user || user.role !== 'ADMIN') {
                window.location.href = 'login.html';
                return;
            }
        }
        
        if (protectedPages.includes(path) && !user) {
            window.location.href = 'login.html';
            return;
        }
    }

    // ==========================================
    // ОБРАБОТЧИКИ ФОРМ
    // ==========================================

    const registerForm = document.getElementById('registerForm');
    if (registerForm) {
        registerForm.addEventListener('submit', async (e) => {
            e.preventDefault();
            const data = new FormData(registerForm);
            const jsonData = Object.fromEntries(data.entries());

            try {
                const res = await apiFetch('/api/auth/register', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify(jsonData)
                });

                if (res.ok) {
                    showToast('Регистрация успешна');
                    setTimeout(() => window.location.href = 'login.html', 1000);
                } else {
                    const error = await res.json();
                    alert(error.error || 'Ошибка регистрации');
                }
            } catch (error) {
                console.error(error);
                alert('Ошибка сети');
            }
        });
    }

    const loginForm = document.getElementById('loginForm');
    if (loginForm) {
        loginForm.addEventListener('submit', async (e) => {
            e.preventDefault();
            const formData = new FormData(loginForm);
            const data = Object.fromEntries(formData.entries());

            try {
                const res = await apiFetch('/api/auth/login', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify(data)
                });

                if (res.ok) {
                    const result = await res.json();
                    
                    if (result.token) {
                        saveToken(result.token);
                    }
                    if (result.user) {
                        saveUserToStorage(result.user);
                    }
                    
                    showToast('Вход выполнен');
                    
                    setTimeout(() => {
                        if (result.user && result.user.role === 'ADMIN') {
                            window.location.href = 'admin.html';
                        } else {
                            window.location.href = 'profile.html';
                        }
                    }, 500);
                } else {
                    const error = await res.json();
                    alert(error.error || 'Ошибка входа');
                }
            } catch (error) {
                console.error('Login error:', error);
                alert('Ошибка сети');
            }
        });
    }

    // ==========================================
    // РАБОТА С КОРЗИНОЙ
    // ==========================================

    document.querySelectorAll('.update-quantity').forEach(btn => {
        btn.addEventListener('click', async (e) => {
            e.preventDefault();
            const cartId = btn.getAttribute('data-cart-id');
            const quantity = btn.getAttribute('data-quantity');

            try {
                const res = await apiFetch(`/api/cart/update/${cartId}`, {
                    method: 'PUT',
                    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
                    body: `quantity=${quantity}`
                });
                if (res.ok) location.reload();
            } catch (error) {
                console.error('Error in update-quantity:', error);
            }
        });
    });

    document.querySelectorAll('.remove-from-cart').forEach(btn => {
        btn.addEventListener('click', async (e) => {
            e.preventDefault();
            const cartId = btn.getAttribute('data-cart-id');

            try {
                const res = await apiFetch(`/api/cart/remove/${cartId}`, {
                    method: 'DELETE'
                });
                if (res.ok) location.reload();
            } catch (error) {
                console.error('Error in remove-from-cart:', error);
            }
        });
    });

    const checkoutForm = document.getElementById('checkoutForm');
    if (checkoutForm) {
        checkoutForm.addEventListener('submit', async (e) => {
            e.preventDefault();
            const data = new FormData(checkoutForm);

            try {
                const res = await apiFetch('/api/checkout', {
                    method: 'POST',
                    body: data
                });

                if (res.ok) {
                    showToast('Заказ оформлен успешно!');
                    setTimeout(() => window.location.href = 'profile.html', 1500);
                } else {
                    const err = await res.json();
                    showToast(err.error || 'Ошибка оформления.');
                }
            } catch (error) {
                console.error('Error in checkout:', error);
                showToast('Ошибка сети.');
            }
        });
    }

    // ==========================================
    // МОДАЛЬНЫЕ ОКНА
    // ==========================================

    window.openPasswordModal = function () {
        const modal = document.getElementById('passwordModal');
        if (modal) modal.style.display = 'flex';
    };

    window.closePasswordModal = function () {
        const modal = document.getElementById('passwordModal');
        if (modal) modal.style.display = 'none';
    };

    // ==========================================
    // ИНИЦИАЛИЗАЦИЯ
    // ==========================================

    protectPages();
    updateHeaderAuth();

    // ==========================================
    // ЭКСПОРТ ГЛОБАЛЬНЫХ ФУНКЦИЙ
    // ==========================================

    window.showToast = showToast;
    window.formatCurrency = formatCurrency;
    window.formatNumber = formatNumber;
    window.formatDate = formatDate;
    window.formatDateOnly = formatDateOnly;
    window.apiFetch = apiFetch;
    window.getToken = getToken;
    window.fetchCurrentUser = fetchCurrentUser;
    window.updateHeaderAuth = updateHeaderAuth;
    window.loadCampaignBadge = loadCampaignBadge;
    window.fetchDiscountInfo = fetchDiscountInfo;
    window.changePassword = changePassword;

    // Глобальный экспорт
    window.apiFetch = apiFetch;
    window.getToken = getToken;
    window.fetchCurrentUser = fetchCurrentUser;
    window.showToast = showToast;

});