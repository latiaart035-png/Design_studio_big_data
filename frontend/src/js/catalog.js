document.addEventListener('DOMContentLoaded', () => {
    loadProducts();
    initFilters();
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

// Маппинг названий категорий в ID
const categoryMap = {
    '': null,           // Все товары
    'Гостиная': 1,
    'Спальня': 2,
    'Кухня': 3,
    'Ванная': 4,
    'Офис': 5
};

// Обратный маппинг ID в название
const idToCategoryMap = {
    1: 'Гостиная',
    2: 'Спальня',
    3: 'Кухня',
    4: 'Ванная',
    5: 'Офис'
};

// Инициализация фильтров
function initFilters() {
    const filterForm = document.getElementById('filterForm');
    const resetBtn = document.getElementById('resetFiltersBtn');
    const applyBtn = document.getElementById('applyFiltersBtn');
    const categoryRadios = document.querySelectorAll('input[name="category"]');
    const minPriceInput = document.getElementById('minPrice');
    const maxPriceInput = document.getElementById('maxPrice');
    const searchInput = document.getElementById('searchInput');

    // Автоматическая фильтрация при выборе категории
    categoryRadios.forEach(radio => {
        radio.addEventListener('change', () => {
            console.log('Category changed to:', radio.value);
            applyFilters();
        });
    });

    // Применение фильтров по кнопке "Применить"
    if (applyBtn) {
        applyBtn.addEventListener('click', (e) => {
            e.preventDefault();
            console.log('Apply filters clicked');
            applyFilters();
        });
    }

    // Сброс фильтров
    if (resetBtn) {
        resetBtn.addEventListener('click', () => {
            console.log('Reset filters clicked');
            // Сбрасываем категорию
            const allCategoryRadio = document.querySelector('input[name="category"][value=""]');
            if (allCategoryRadio) allCategoryRadio.checked = true;
            
            // Сбрасываем цену
            if (minPriceInput) minPriceInput.value = '';
            if (maxPriceInput) maxPriceInput.value = '';
            
            // Сбрасываем поиск
            if (searchInput) searchInput.value = '';
            
            // Применяем фильтры
            applyFilters();
        });
    }

    // Запрещаем стандартную отправку формы
    if (filterForm) {
        filterForm.addEventListener('submit', (e) => {
            e.preventDefault();
            applyFilters();
        });
    }

    // Загружаем сохранённые значения из URL при загрузке страницы
    const urlParams = new URLSearchParams(window.location.search);
    if (minPriceInput && urlParams.has('minPrice')) {
        minPriceInput.value = urlParams.get('minPrice');
    }
    if (maxPriceInput && urlParams.has('maxPrice')) {
        maxPriceInput.value = urlParams.get('maxPrice');
    }
    if (searchInput && urlParams.has('search')) {
        searchInput.value = urlParams.get('search');
    }
    
    // Восстанавливаем выбранную категорию из URL
    const selectedCategory = urlParams.get('category');
    if (selectedCategory) {
        const categoryRadio = document.querySelector(`input[name="category"][value="${selectedCategory}"]`);
        if (categoryRadio) {
            categoryRadio.checked = true;
            console.log('Restored category from URL:', selectedCategory);
        }
    } else {
        // Если категория не указана в URL, выбираем "Все товары"
        const allCategoryRadio = document.querySelector('input[name="category"][value=""]');
        if (allCategoryRadio) allCategoryRadio.checked = true;
    }
}

// Применение фильтров
function applyFilters() {
    const selectedCategory = document.querySelector('input[name="category"]:checked');
    // Сохраняем название категории (для URL)
    const categoryName = selectedCategory && selectedCategory.value ? selectedCategory.value : '';
    
    const minPrice = document.getElementById('minPrice')?.value || '';
    const maxPrice = document.getElementById('maxPrice')?.value || '';
    const search = document.getElementById('searchInput')?.value || '';
    
    console.log('Applying filters - Category:', categoryName, 'Min:', minPrice, 'Max:', maxPrice, 'Search:', search);
    
    // Обновляем URL без перезагрузки страницы
    const params = new URLSearchParams();
    if (categoryName) params.set('category', categoryName);
    if (minPrice) params.set('minPrice', minPrice);
    if (maxPrice) params.set('maxPrice', maxPrice);
    if (search) params.set('search', search);
    
    const newUrl = `${window.location.pathname}${params.toString() ? '?' + params.toString() : ''}`;
    window.history.pushState({}, '', newUrl);
    
    // Загружаем товары с новыми параметрами
    loadProducts();
}

async function loadProducts() {
    const grid = document.getElementById('productsGrid');
    const noProducts = document.getElementById('noProducts');
    if (!grid) return;
    
    grid.innerHTML = '<div class="col-12 text-center"><div class="spinner-border text-primary" role="status"></div><p>Загрузка товаров...</p></div>';
    if (noProducts) noProducts.style.display = 'none';
    
    try {
        const urlParams = new URLSearchParams(window.location.search);
        const categoryName = urlParams.get('category') || '';
        const minPrice = urlParams.get('minPrice');
        const maxPrice = urlParams.get('maxPrice');
        const search = urlParams.get('search') || '';
        
        console.log('Filters - Category:', categoryName, 'Min:', minPrice, 'Max:', maxPrice, 'Search:', search);
        
        // Строим URL с параметром categoryName (название категории)
        let url = '/api/products';
        const queryParams = new URLSearchParams();
        
        if (search && search.trim() !== '') {
            queryParams.append('search', search.trim());
        } else if (categoryName && categoryName !== '') {
            // Отправляем название категории (сервер сам найдет ID)
            queryParams.append('categoryName', categoryName);
        }
        
        if (minPrice && minPrice !== '') {
            queryParams.append('minPrice', minPrice);
        }
        
        if (maxPrice && maxPrice !== '') {
            queryParams.append('maxPrice', maxPrice);
        }
        
        if (queryParams.toString()) {
            url += '?' + queryParams.toString();
        }
        
        console.log('Fetching from URL:', url);
        const res = await apiFetch(url);
        
        if (!res.ok) {
            throw new Error('Ошибка загрузки');
        }
        
        let products = await res.json();
        console.log('Products loaded:', products.length);
        
        // Дополнительная фильтрация по цене на клиенте (если нужно)
        // так как сервер может не поддерживать одновременную фильтрацию
        if ((minPrice && minPrice !== '') || (maxPrice && maxPrice !== '')) {
            products = products.filter(p => {
                let match = true;
                if (minPrice && minPrice !== '') match = match && p.price >= parseFloat(minPrice);
                if (maxPrice && maxPrice !== '') match = match && p.price <= parseFloat(maxPrice);
                return match;
            });
            console.log('After price filter on client:', products.length);
        }
        
        if (!products || products.length === 0) {
            grid.innerHTML = '';
            if (noProducts) noProducts.style.display = 'block';
            return;
        }
        
        if (noProducts) noProducts.style.display = 'none';
        grid.innerHTML = products.map(product => createProductCard(product)).join('');
        attachProductEventHandlers(grid);
        
    } catch (error) {
        console.error('Error loading products:', error);
        grid.innerHTML = '<div class="col-12 text-center"><p class="text-danger">Ошибка загрузки товаров: ' + error.message + '</p></div>';
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
                     alt="${escapeHtml(product.name)}"
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
        
        // Собираем все изображения
        let allImages = [];
        if (product.mainImageUrl) allImages.push(getImageUrl(product.mainImageUrl));
        if (product.additionalImages && product.additionalImages.length > 0) {
            allImages = allImages.concat(product.additionalImages.map(img => getImageUrl(img)));
        }
        
        // Заполняем модальное окно
        document.getElementById('modalMainImage').src = getImageUrl(product.mainImageUrl) || 'src/images/default.jpg';
        document.getElementById('modalCategory').textContent = product.categoryName || 'Без категории';
        document.getElementById('modalTitle').textContent = product.name;
        document.getElementById('modalDescription').textContent = product.description || 'Описание отсутствует';
        
        const hasDiscount = product.hasDiscount;
        const displayPrice = hasDiscount ? product.discountedPrice : product.price;
        document.getElementById('modalPrice').innerHTML = hasDiscount 
            ? `<span class="modal-price modal-price--new">${displayPrice.toFixed(2)} BYN</span><span class="modal-price modal-price--old">${product.price.toFixed(2)} BYN</span>`
            : `<span class="modal-price">${displayPrice.toFixed(2)} BYN</span>`;
        
        // Миниатюры
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
    
    // Добавляем эффект плавного перехода
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