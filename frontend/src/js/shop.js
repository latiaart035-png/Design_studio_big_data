document.addEventListener('DOMContentLoaded', () => {
    loadRecentProducts();
});

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

async function loadRecentProducts() {
    const container = document.getElementById('popularProducts');
    if (!container) return;
    
    container.innerHTML = '<div class="col-12 text-center"><p>Загрузка товаров...</p></div>';
    
    try {
        const res = await apiFetch('/api/products/recent?limit=8');
        if (!res.ok) throw new Error('Ошибка загрузки');
        
        const products = await res.json();
        
        if (!products || products.length === 0) {
            container.innerHTML = '<div class="col-12 text-center"><p>Товары не найдены</p></div>';
            return;
        }
        
        container.innerHTML = products.map(product => createProductCard(product)).join('');
        attachProductEventHandlers(container);
        
    } catch (error) {
        console.error('Error loading products:', error);
        container.innerHTML = '<div class="col-12 text-center"><p class="text-danger">Ошибка загрузки товаров</p></div>';
    }
}

function createProductCard(product) {
    const hasDiscount = product.hasDiscount;
    const displayPrice = hasDiscount ? product.discountedPrice : product.price;
    const originalPrice = hasDiscount ? product.price : null;
    const discountPercent = hasDiscount ? Math.round((1 - displayPrice / originalPrice) * 100) : 0;
    
    const imageUrl = getImageUrl(product.mainImageUrl);
    
    return `
        <div class="product-card">
            <div class="product-card__image-wrapper">
                <img src="${imageUrl}" 
                     alt="${product.name}"
                     class="product-card__image"
                     onerror="this.src='src/images/default.jpg'">
                ${hasDiscount ? `<span class="product-card__badge">−${discountPercent}%</span>` : ''}
            </div>
            <div class="product-card__info">
                <span class="product-card__category">${escapeHtml(product.categoryName || 'Без категории')}</span>
                <h3 class="product-card__name">${escapeHtml(product.name)}</h3>
                <p class="product-card__desc">${escapeHtml(product.description || 'Описание отсутствует')}</p>
                <div class="product-card__price-wrapper">
                    <span class="product-card__price ${hasDiscount ? 'product-card__price--new' : ''}">${displayPrice.toFixed(2)} BYN</span>
                    ${originalPrice ? `<span class="product-card__price product-card__price--old">${originalPrice.toFixed(2)} BYN</span>` : ''}
                </div>
                <div class="product-card__buttons">
                    <button class="product-card__btn product-card__btn--cart add-to-cart" data-product-id="${product.id}">В корзину</button>
                    <button class="product-card__btn product-card__btn--details details-btn" data-product-id="${product.id}">Детали</button>
                </div>
            </div>
        </div>
    `;
}

function attachProductEventHandlers(container) {
    container.querySelectorAll('.add-to-cart').forEach(btn => {
        btn.addEventListener('click', async (e) => {
            e.preventDefault();
            const productId = btn.getAttribute('data-product-id');
            const productName = btn.closest('.product-card')?.querySelector('.product-card__name')?.textContent || 'Товар';
            await addToCart(productId, 1, productName);
        });
    });
    
    container.querySelectorAll('.details-btn').forEach(btn => {
        btn.addEventListener('click', (e) => {
            e.preventDefault();
            showProductDetails(btn.getAttribute('data-product-id'));
        });
    });
}

// Функция для показа уведомления
function showNotification(message, type = 'success', duration = 3000) {
    const existingToasts = document.querySelectorAll('.custom-toast');
    existingToasts.forEach(toast => toast.remove());
    
    const toast = document.createElement('div');
    toast.className = `custom-toast ${type === 'error' ? 'error' : ''}`;
    toast.innerHTML = `
        <i class="bi ${type === 'error' ? 'bi-exclamation-triangle-fill' : 'bi-check-circle-fill'} me-2"></i>
        <span>${escapeHtml(message)}</span>
        <button class="close-btn" onclick="this.parentElement.remove()">&times;</button>
    `;
    document.body.appendChild(toast);
    
    setTimeout(() => toast.classList.add('show'), 10);
    setTimeout(() => {
        toast.classList.remove('show');
        setTimeout(() => toast.remove(), 500);
    }, duration);
}

async function addToCart(productId, quantity = 1, productName = 'Товар') {
    const token = getToken();
    
    if (!token) {
        showNotification('Для добавления товара в корзину необходимо авторизоваться', 'error', 4000);
        setTimeout(() => {
            window.location.href = 'login.html';
        }, 2500);
        return;
    }
    
    try {
        const res = await apiFetch('/api/cart/add', {
            method: 'POST',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
            body: `productId=${productId}&quantity=${quantity}`
        });
        
        if (res.ok) {
            showNotification(`Товар "${productName}" добавлен в корзину!`, 'success', 2500);
            updateCartCount();
        } else {
            const error = await res.json();
            showNotification(error.error || 'Ошибка добавления в корзину', 'error');
        }
    } catch (error) {
        console.error('Error adding to cart:', error);
        showNotification('Ошибка сети. Попробуйте позже.', 'error');
    }
}

// Обновление счётчика корзины
async function updateCartCount() {
    try {
        const res = await apiFetch('/api/cart/count');
        if (res.ok) {
            const data = await res.json();
            const cartCount = document.getElementById('cartCount');
            if (cartCount) {
                cartCount.textContent = data.count || 0;
                cartCount.style.display = data.count > 0 ? 'inline-block' : 'none';
            }
        }
    } catch (e) {
        console.error('Error updating cart count:', e);
    }
}

let currentProductId = null;
const modalEl = document.getElementById('productModal');

async function showProductDetails(productId) {
    try {
        const res = await apiFetch(`/api/products/${productId}`);
        if (!res.ok) throw new Error('Ошибка загрузки товара');
        
        const product = await res.json();
        currentProductId = product.id;
        
        let allImages = [];
        if (product.mainImageUrl) allImages.push(getImageUrl(product.mainImageUrl));
        if (product.additionalImages && product.additionalImages.length > 0) {
            allImages = allImages.concat(product.additionalImages.map(img => getImageUrl(img)));
        }
        
        document.getElementById('modalMainImage').src = getImageUrl(product.mainImageUrl) || 'src/images/default.jpg';
        document.getElementById('modalCategory').textContent = product.categoryName || 'Без категории';
        document.getElementById('modalTitle').textContent = product.name;
        document.getElementById('modalDescription').textContent = product.description || 'Описание отсутствует';
        
        const hasDiscount = product.hasDiscount;
        const displayPrice = hasDiscount ? product.discountedPrice : product.price;
        document.getElementById('modalPrice').innerHTML = hasDiscount 
            ? `<span class="modal-price modal-price--new">${displayPrice.toFixed(2)} BYN</span><span class="modal-price modal-price--old">${product.price.toFixed(2)} BYN</span>`
            : `<span class="modal-price">${displayPrice.toFixed(2)} BYN</span>`;
        
        const thumbnailsContainer = document.getElementById('modalThumbnails');
        thumbnailsContainer.innerHTML = '';
        
        if (allImages.length > 0) {
            allImages.forEach((img, index) => {
                const imgEl = document.createElement('img');
                imgEl.src = img;
                imgEl.className = 'modal-thumbnail' + (index === 0 ? ' active' : '');
                imgEl.onclick = () => changeMainImage(img, imgEl);
                thumbnailsContainer.appendChild(imgEl);
            });
        }
        
        document.getElementById('modalQuantity').value = 1;
        modalEl.showModal();
        
    } catch (error) {
        console.error('Error loading product:', error);
        showNotification('Ошибка загрузки товара', 'error');
    }
}

function changeMainImage(src, clickedThumbnail) {
    const mainImage = document.getElementById('modalMainImage');
    
    mainImage.style.opacity = '0.5';
    
    setTimeout(() => {
        mainImage.src = src;
        mainImage.onload = () => {
            mainImage.style.opacity = '1';
        };
    }, 150);
    
    document.querySelectorAll('.modal-thumbnail').forEach(thumb => {
        thumb.classList.remove('active');
    });
    clickedThumbnail.classList.add('active');
}

// Обработчик добавления из модального окна
if (document.getElementById('modalAddToCart')) {
    document.getElementById('modalAddToCart').addEventListener('click', async () => {
        if (!currentProductId) return;
        
        const token = getToken();
        const productName = document.getElementById('modalTitle')?.textContent || 'Товар';
        
        if (!token) {
            modalEl.close();
            showNotification('Для добавления товара в корзину необходимо авторизоваться', 'error', 4000);
            setTimeout(() => {
                window.location.href = 'login.html';
            }, 2500);
            return;
        }
        
        const quantity = parseInt(document.getElementById('modalQuantity').value) || 1;
        
        try {
            const res = await apiFetch('/api/cart/add', {
                method: 'POST',
                headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
                body: `productId=${currentProductId}&quantity=${quantity}`
            });
            
            if (res.ok) {
                showNotification(`Товар "${productName}" добавлен в корзину!`, 'success', 2500);
                modalEl.close();
                updateCartCount();
            } else {
                const error = await res.json();
                showNotification(error.error || 'Ошибка добавления', 'error');
            }
        } catch (error) {
            console.error('Error adding to cart:', error);
            showNotification('Ошибка сети', 'error');
        }
    });
}

function escapeHtml(text) {
    if (!text) return '';
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}