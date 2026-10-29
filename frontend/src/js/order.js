// File: src/js/order.js

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
    initTariffSelection();
    initPhotoUpload();
    loadProductsForSelection();
    initFormSubmission();
    initCardFormatting();
});

// Инициализация выбора тарифа
function initTariffSelection() {
    document.querySelectorAll('.tariff-card').forEach(card => {
        card.addEventListener('click', function() {
            // Убираем selected со всех
            document.querySelectorAll('.tariff-card').forEach(c => c.classList.remove('selected'));
            // Добавляем selected к текущему
            this.classList.add('selected');
            // Отмечаем radio
            const radio = this.querySelector('input[type="radio"]');
            radio.checked = true;
        });
    });
}

// Инициализация загрузки фото
function initPhotoUpload() {
    const uploadArea = document.getElementById('photoUploadArea');
    const fileInput = document.getElementById('photoInput');
        const previewContainer = document.getElementById('photoPreviewContainer');
    let uploadedFiles = [];

    // Клик по области загрузки
    uploadArea.addEventListener('click', () => {
        fileInput.click();
    });

    // Drag and drop
    uploadArea.addEventListener('dragover', (e) => {
        e.preventDefault();
        uploadArea.classList.add('dragover');
    });

    uploadArea.addEventListener('dragleave', () => {
        uploadArea.classList.remove('dragover');
    });

    uploadArea.addEventListener('drop', (e) => {
        e.preventDefault();
        uploadArea.classList.remove('dragover');
        handleFiles(e.dataTransfer.files);
    });

    // Выбор файлов
    fileInput.addEventListener('change', (e) => {
        handleFiles(e.target.files);
    });

    function handleFiles(files) {
        for (let file of files) {
            if (file.type.startsWith('image/')) {
                uploadedFiles.push(file);
                // Показываем превью
                const reader = new FileReader();
                reader.onload = (e) => {
                    const previewDiv = document.createElement('div');
                    previewDiv.className = 'photo-preview';
                    previewDiv.innerHTML = `
                        <img src="${e.target.result}" alt="Preview">
                        <button type="button" class="photo-preview-remove" onclick="removePhoto(${uploadedFiles.length - 1}, this)">&times;</button>
                    `;
                    previewContainer.appendChild(previewDiv);
                };
                reader.readAsDataURL(file);
            }
        }
    }

    // Удаление фото
    window.removePhoto = function(index, btn) {
        uploadedFiles.splice(index, 1);
        btn.parentElement.remove();
        // Пересоздаем превью
        rebuildPreviews();
    };

    function rebuildPreviews() {
        previewContainer.innerHTML = '';
        uploadedFiles.forEach((file, index) => {
            const reader = new FileReader();
            reader.onload = (e) => {
                const previewDiv = document.createElement('div');
                previewDiv.className = 'photo-preview';
                previewDiv.innerHTML = `
                    <img src="${e.target.result}" alt="Preview">
                    <button type="button" class="photo-preview-remove" onclick="removePhoto(${index}, this)">&times;</button>
                `;
                previewContainer.appendChild(previewDiv);
            };
            reader.readAsDataURL(file);
        });
    }
}

// Загрузка товаров для выбора
async function loadProductsForSelection() {
    const container = document.getElementById('productsList');
    
    try {
        const res = await apiFetch('/api/products');
        if (!res.ok) throw new Error();
        
        const products = await res.json();
        
        if (!products || products.length === 0) {
            container.innerHTML = '<div class="text-center text-muted p-3">Товары не найдены</div>';
            return;
        }
        
        container.innerHTML = products.map(product => `
            <div class="product-select-card" data-product-id="${product.id}" onclick="toggleProductSelection(this, ${product.id})">
                <input type="checkbox" class="product-select-checkbox" id="product-${product.id}" value="${product.id}">
                <img src="${getImageUrl(product.mainImageUrl)}" alt="${product.name}" class="product-select-image" onerror="this.src='src/images/default.jpg'">
                <div class="product-select-info">
                    <div class="product-select-name">${product.name}</div>
                    <div class="product-select-price">${product.price.toLocaleString('ru-RU')} BYN</div>
                </div>
            </div>
        `).join('');
        
    } catch (e) {
        console.error('Ошибка загрузки товаров:', e);
        container.innerHTML = '<div class="text-center text-danger p-3">Ошибка загрузки товаров</div>';
    }
}

// Переключение выбора товара
window.toggleProductSelection = function(element, productId) {
    const checkbox = element.querySelector('.product-select-checkbox');
    checkbox.checked = !checkbox.checked;
    
    if (checkbox.checked) {
        element.classList.add('selected');
    } else {
        element.classList.remove('selected');
    }
    
    updateSelectedCount();
};

function updateSelectedCount() {
    const checked = document.querySelectorAll('.product-select-checkbox:checked').length;
    document.getElementById('selectedProductsCount').textContent = `Выбрано: ${checked}`;
}

function getSelectedProducts() {
    const checkboxes = document.querySelectorAll('.product-select-checkbox:checked');
    return Array.from(checkboxes).map(cb => cb.value);
}

// Форматирование номера карты
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

// Отправка формы
function initFormSubmission() {
    const form = document.getElementById('designOrderForm');
    const submitBtn = document.getElementById('submitBtn');
    
    form.addEventListener('submit', async (e) => {
        e.preventDefault();
        
        // Проверка авторизации с плавным уведомлением
        const token = getToken();
        if (!token) {
            showToast('Пожалуйста, авторизуйтесь для оформления заказа дизайна', false, 5000);
            setTimeout(() => {
                window.location.href = 'login.html';
            }, 2500);
            return;
        }
        
        // Собираем данные формы
        const formData = new FormData(form);
        
        // Добавляем выбранные товары
        const selectedProducts = getSelectedProducts();
        formData.set('selectedProducts', selectedProducts.join(','));
        
        // Добавляем файлы из input
        const fileInput = document.getElementById('photoInput');
        if (fileInput && fileInput.files.length > 0) {
            for (let i = 0; i < fileInput.files.length; i++) {
                formData.append('photos', fileInput.files[i]);
            }
        }
        
        // Добавляем файлы из нашего массива uploadedFiles (для drag&drop)
        if (typeof uploadedFiles !== 'undefined' && uploadedFiles && uploadedFiles.length > 0) {
            uploadedFiles.forEach(file => {
                formData.append('photos', file);
            });
        }
        
        // Блокируем кнопку
        submitBtn.disabled = true;
        submitBtn.innerHTML = '<i class="bi bi-hourglass-split"></i> Отправка...';
        
        try {
            const res = await apiFetch('/api/order', {
                method: 'POST',
                body: formData
            });
            
            if (res.ok) {
                const data = await res.json();
                showToast('Заявка успешно отправлена! Мы свяжемся с вами в ближайшее время.', true);
                setTimeout(() => {
                    window.location.href = 'profile.html';
                }, 2000);
            } else {
                const error = await res.json();
                showToast(error.error || 'Ошибка отправки заявки', false);
            }
        } catch (e) {
            console.error('Ошибка отправки:', e);
            showToast('Ошибка сети при отправке заявки. Проверьте подключение.', false);
        } finally {
            submitBtn.disabled = false;
            submitBtn.innerHTML = '<i class="bi bi-send"></i> Отправить заявку';
        }
    });
}

// Улучшенное Toast уведомление с анимацией
function showToast(message, isSuccess = true, duration = 4000) {
    // Удаляем существующие тосты
    const existingToasts = document.querySelectorAll('.custom-toast');
    existingToasts.forEach(toast => toast.remove());
    
    const toast = document.createElement('div');
    toast.className = `custom-toast ${isSuccess ? 'success' : 'error'}`;
    toast.innerHTML = `
        <i class="bi ${isSuccess ? 'bi-check-circle-fill' : 'bi-exclamation-triangle-fill'} me-2"></i>
        <span>${escapeHtml(message)}</span>
        <button class="close-btn" onclick="this.parentElement.remove()">&times;</button>
    `;
    document.body.appendChild(toast);
    
    // Анимация появления
    setTimeout(() => toast.classList.add('show'), 10);
    
    // Автоматическое скрытие
    setTimeout(() => {
        toast.classList.remove('show');
        setTimeout(() => toast.remove(), 500);
    }, duration);
}

// Добавьте функцию escapeHtml если её нет
function escapeHtml(text) {
    if (!text) return '';
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

function handleLogout(event) {
    event.preventDefault();
    const navLogout = document.getElementById('navLogout');
    
    if (navLogout && navLogout.textContent === 'ВЫЙТИ') {
        localStorage.removeItem('design_studio_user');
        localStorage.removeItem('design_studio_token');
        window.location.href = 'index.html';
    } else {
        window.location.href = 'login.html';
    }
}