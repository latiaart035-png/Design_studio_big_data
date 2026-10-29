// File: src/js/marketing.js
// Полностью исправленная версия модуля маркетинговых кампаний

const API_BASE = 'http://localhost:8080/api';

// Состояние приложения
let campaigns = [];
let templates = [];
let editingCampaignId = null;
let selectedTemplateId = null;

// ===== ИНИЦИАЛИЗАЦИЯ =====

document.addEventListener('DOMContentLoaded', async () => {
    console.log('Marketing module initializing...');
    await initMarketing();
});

async function initMarketing() {
    const isAuth = await checkAuth();
    if (!isAuth) return;
    
    await loadStats();
    await loadCampaigns();
    await loadTemplates();
    setupEventListeners();
    
    console.log('Marketing module initialized successfully');
}

// ===== ПРОВЕРКА АВТОРИЗАЦИИ =====

async function checkAuth() {
    const token = localStorage.getItem('design_studio_token');
    if (!token) {
        window.location.href = 'login.html';
        return false;
    }
    
    // Обновляем header
    if (typeof updateHeaderAuth === 'function') {
        updateHeaderAuth();
    }
    
    return true;
}

// ===== ЗАГРУЗКА ДАННЫХ =====

async function loadStats() {
    try {
        const response = await fetch(`${API_BASE}/marketing/stats`, {
            headers: getAuthHeaders()
        });
        
        if (!response.ok) {
            console.error('Ошибка загрузки статистики:', response.status);
            return;
        }
        
        const stats = await response.json();
        
        // Обновляем UI
        document.getElementById('totalCampaigns').textContent = stats.totalCampaigns || 0;
        document.getElementById('activeCampaigns').textContent = stats.activeCampaigns || 0;
        document.getElementById('totalRecipients').textContent = formatNumber(stats.totalRecipients || 0);
        document.getElementById('avgOpenRate').textContent = `${(stats.avgOpenRate || 0).toFixed(1)}%`;
        document.getElementById('avgClickRate').textContent = `${(stats.avgClickRate || 0).toFixed(1)}%`;
        document.getElementById('totalRevenue').textContent = formatCurrency(stats.totalRevenue || 0);
        
    } catch (error) {
        console.error('Ошибка загрузки статистики:', error);
    }
}

async function loadCampaigns() {
    try {
        const response = await fetch(`${API_BASE}/marketing/campaigns`, {
            headers: getAuthHeaders()
        });
        
        if (!response.ok) {
            console.error('Ошибка загрузки кампаний:', response.status);
            throw new Error('Ошибка загрузки кампаний');
        }
        
        campaigns = await response.json();
        console.log('Загружено кампаний:', campaigns.length);
        renderCampaigns();
        
    } catch (error) {
        console.error('Ошибка загрузки кампаний:', error);
        showToast('Ошибка загрузки кампаний', true);
    }
}

async function loadTemplates() {
    try {
        const response = await fetch(`${API_BASE}/marketing/templates`, {
            headers: getAuthHeaders()
        });
        
        if (!response.ok) {
            console.error('Ошибка загрузки шаблонов:', response.status);
            return;
        }
        
        templates = await response.json();
        console.log('Загружено шаблонов:', templates.length);
        renderTemplates();
        
    } catch (error) {
        console.error('Ошибка загрузки шаблонов:', error);
    }
}

// ===== РЕНДЕР КАМПАНИЙ =====

function renderCampaigns(campaignsToRender = campaigns) {
    const container = document.getElementById('campaignsList');
    if (!container) return;
    
    if (!campaignsToRender || campaignsToRender.length === 0) {
        container.innerHTML = `
            <div class="mkt-empty-state">
                <i class="bi bi-megaphone"></i>
                <p>Кампании не найдены. Создайте первую кампанию.</p>
            </div>
        `;
        return;
    }
    
    container.innerHTML = campaignsToRender.map(campaign => {
        const statusClass = getStatusClass(campaign.status);
        const statusText = getStatusText(campaign.status);
        const typeText = getTypeText(campaign.type);
        
        return `
            <div class="mkt-campaign-card" data-campaign-id="${campaign.id}">
                <div class="mkt-campaign-header">
                    <h5 class="mkt-campaign-title">${escapeHtml(campaign.name || 'Без названия')}</h5>
                    <span class="mkt-campaign-status ${statusClass}">${statusText}</span>
                </div>
                <div class="mkt-campaign-body">
                    <p class="mkt-campaign-description">${escapeHtml(campaign.description || 'Без описания')}</p>
                    <div class="mkt-campaign-info">
                        <div class="mkt-info-item">
                            <div class="mkt-info-label">Тип</div>
                            <div class="mkt-info-value">${typeText}</div>
                        </div>
                        <div class="mkt-info-item">
                            <div class="mkt-info-label">Скидка</div>
                            <div class="mkt-info-value">${campaign.discountPercent || 0}%</div>
                        </div>
                        <div class="mkt-info-item">
                            <div class="mkt-info-label">Получателей</div>
                            <div class="mkt-info-value">${formatNumber(campaign.totalRecipients || 0)}</div>
                        </div>
                        <div class="mkt-info-item">
                            <div class="mkt-info-label">Отправлено</div>
                            <div class="mkt-info-value">${formatNumber(campaign.sentCount || 0)}</div>
                        </div>
                    </div>
                    <div class="mkt-campaign-metrics">
                                                <div class="mkt-metric">
                            <span class="mkt-metric-value">${(campaign.openRate || 0).toFixed(1)}%</span>
                            <span class="mkt-metric-label">Открытия</span>
                            <div class="mkt-metric-bar">
                                <div class="mkt-metric-bar-fill" style="width: ${Math.min(campaign.openRate || 0, 100)}%"></div>
                            </div>
                        </div>
                        <div class="mkt-metric">
                            <span class="mkt-metric-value">${(campaign.clickRate || 0).toFixed(1)}%</span>
                            <span class="mkt-metric-label">Клики</span>
                            <div class="mkt-metric-bar">
                                <div class="mkt-metric-bar-fill" style="width: ${Math.min(campaign.clickRate || 0, 100)}%"></div>
                            </div>
                        </div>
                        <div class="mkt-metric">
                            <span class="mkt-metric-value">${(campaign.conversionRate || 0).toFixed(1)}%</span>
                            <span class="mkt-metric-label">Конверсия</span>
                            <div class="mkt-metric-bar">
                                <div class="mkt-metric-bar-fill" style="width: ${Math.min(campaign.conversionRate || 0, 100)}%"></div>
                            </div>
                        </div>
                        <div class="mkt-metric">
                            <span class="mkt-metric-value">${formatCurrency(campaign.revenueGenerated || 0)}</span>
                            <span class="mkt-metric-label">Выручка</span>
                        </div>
                    </div>
                </div>
                <div class="mkt-campaign-footer">
                    <span class="mkt-campaign-date">
                        <i class="bi bi-calendar3"></i> ${formatDate(campaign.createdAt)}
                    </span>
                    <div class="d-flex gap-2">
                        <button class="mkt-btn mkt-btn-sm mkt-btn-info" onclick="viewCampaign(${campaign.id})">
                            <i class="bi bi-eye"></i> Просмотр
                        </button>
                        ${campaign.status === 'DRAFT' || campaign.status === 'PAUSED' ? `
                            <button class="mkt-btn mkt-btn-sm mkt-btn-success" onclick="launchCampaign(${campaign.id})">
                                <i class="bi bi-play"></i> Запустить
                            </button>
                        ` : ''}
                        ${campaign.status === 'RUNNING' ? `
                            <button class="mkt-btn mkt-btn-sm mkt-btn-warning" onclick="pauseCampaign(${campaign.id})">
                                <i class="bi bi-pause"></i> Пауза
                            </button>
                        ` : ''}
                        ${campaign.status === 'COMPLETED' || campaign.status === 'CANCELLED' ? `
                            <button class="mkt-btn mkt-btn-sm mkt-btn-danger" onclick="deleteCampaign(${campaign.id})">
                                <i class="bi bi-trash"></i> Удалить
                            </button>
                        ` : ''}
                        ${campaign.status === 'RUNNING' || campaign.status === 'COMPLETED' ? `
                            <button class="mkt-btn mkt-btn-sm mkt-btn-danger" onclick="cancelCampaign(${campaign.id})">
                                <i class="bi bi-x-circle"></i> Отменить
                            </button>
                        ` : ''}
                    </div>
                </div>
            </div>
        `;
    }).join('');
}

// ===== РЕНДЕР ШАБЛОНОВ =====

function renderTemplates(templatesToRender = templates) {
    const container = document.getElementById('templatesList');
    if (!container) return;
    
    if (!templatesToRender || templatesToRender.length === 0) {
        container.innerHTML = `
            <div class="mkt-empty-state">
                <i class="bi bi-file-text"></i>
                <p>Шаблоны не найдены. Создайте первый шаблон.</p>
            </div>
        `;
        return;
    }
    
    container.innerHTML = templatesToRender.map(template => `
        <div class="mkt-template-card ${selectedTemplateId === template.id ? 'selected' : ''}" 
             data-id="${template.id}" 
             onclick="selectTemplate(${template.id})">
            <div class="mkt-template-header">
                <div class="mkt-template-icon">
                    <i class="bi ${getTemplateIcon(template.type)}"></i>
                </div>
                <div>
                    <h6 class="mkt-template-name">${escapeHtml(template.name || 'Без названия')}</h6>
                    <span class="mkt-template-category">${getCategoryText(template.category)}</span>
                </div>
            </div>
            <p class="mkt-template-description">${escapeHtml(template.description || 'Без описания')}</p>
            <div class="mkt-template-meta">
                <span class="mkt-template-badge">${getTypeText(template.type)}</span>
                <span class="mkt-template-usage">Использований: ${template.usageCount || 0}</span>
            </div>
        </div>
    `).join('');
}

// ===== ВЫБОР ШАБЛОНА =====

function selectTemplate(templateId) {
    selectedTemplateId = templateId;
    const template = templates.find(t => t.id === templateId);
    
    if (template) {
        // Заполняем форму кампании
        document.getElementById('campSubject').value = template.subjectTemplate || '';
        document.getElementById('campContent').value = template.contentTemplate || '';
    }
    
    // Обновляем визуальное выделение
    document.querySelectorAll('.mkt-template-card').forEach(el => {
        el.classList.remove('selected');
    });
    document.querySelector(`.mkt-template-card[data-id="${templateId}"]`)?.classList.add('selected');
    
    // Переключаемся на вкладку создания кампании
    const createTab = document.querySelector('#createTab');
    if (createTab) {
        const tabTrigger = document.querySelector('a[href="#createTab"]');
        if (tabTrigger) {
            const bsTab = new bootstrap.Tab(tabTrigger);
            bsTab.show();
        }
    }
}

// ===== СОХРАНЕНИЕ КАМПАНИИ =====

async function saveCampaign(event) {
    event.preventDefault();
    
    const form = document.getElementById('campaignForm');
    if (!form.checkValidity()) {
        form.reportValidity();
        return;
    }
    
    // Получаем выбранные сегменты
    const select = document.getElementById('campSegments');
    const selectedSegments = Array.from(select.selectedOptions)
        .map(option => option.value)
        .join(',');
    
    const campaignData = {
        name: document.getElementById('campName').value.trim(),
        description: document.getElementById('campDescription').value.trim(),
        type: document.getElementById('campType').value,
        targetSegments: selectedSegments,
        discountPercent: parseFloat(document.getElementById('campDiscount').value) || 0,
        minOrderAmount: parseFloat(document.getElementById('campMinOrder').value) || 0,
        validDays: parseInt(document.getElementById('campValidDays').value) || 7,
        emailSubject: document.getElementById('campSubject').value.trim(),
        emailContent: document.getElementById('campContent').value.trim(),
        smsText: document.getElementById('campSmsText').value.trim(),
        isAutomated: document.getElementById('campAutomated').checked,
        triggerEvent: document.getElementById('campTrigger').value,
        startDate: document.getElementById('campStartDate').value || null,
        endDate: document.getElementById('campEndDate').value || null
    };
    
    try {
        let response;
        
        if (editingCampaignId) {
            // Обновление существующей кампании
            response = await fetch(`${API_BASE}/marketing/campaigns/${editingCampaignId}`, {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json',
                    ...getAuthHeaders()
                },
                body: JSON.stringify(campaignData)
            });
        } else {
            // Создание новой кампании
            response = await fetch(`${API_BASE}/marketing/campaigns`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    ...getAuthHeaders()
                },
                body: JSON.stringify(campaignData)
            });
        }
        
        if (!response.ok) {
            const error = await response.json();
            throw new Error(error.error || 'Ошибка сохранения кампании');
        }
        
        const campaign = await response.json();
        
        // Запускаем кампанию сразу
        await launchCampaign(campaign.id);
        
        showToast(editingCampaignId ? 'Кампания обновлена и запущена' : 'Кампания создана и запущена');
        
        resetCampaignForm();
        await loadCampaigns();
        await loadStats();
        
        // Переключаемся на вкладку кампаний
        const campaignsTab = document.querySelector('a[href="#campaignsTab"]');
        if (campaignsTab) {
            const bsTab = new bootstrap.Tab(campaignsTab);
            bsTab.show();
        }
        
    } catch (error) {
        console.error('Ошибка сохранения:', error);
        showToast(error.message, true);
    }
}

// Сохранение черновика
async function saveCampaignDraft() {
    const campaignData = {
        name: document.getElementById('campName').value.trim() || 'Новый черновик',
        description: document.getElementById('campDescription').value.trim(),
        type: document.getElementById('campType').value,
        targetSegments: Array.from(document.getElementById('campSegments').selectedOptions)
            .map(o => o.value).join(','),
        discountPercent: parseFloat(document.getElementById('campDiscount').value) || 0,
        minOrderAmount: parseFloat(document.getElementById('campMinOrder').value) || 0,
        validDays: parseInt(document.getElementById('campValidDays').value) || 7,
        emailSubject: document.getElementById('campSubject').value.trim(),
        emailContent: document.getElementById('campContent').value.trim(),
        smsText: document.getElementById('campSmsText').value.trim(),
        isAutomated: document.getElementById('campAutomated').checked,
        triggerEvent: document.getElementById('campTrigger').value,
        startDate: document.getElementById('campStartDate').value || null,
        endDate: document.getElementById('campEndDate').value || null,
        status: 'DRAFT'
    };
    
    try {
        const response = await fetch(`${API_BASE}/marketing/campaigns`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                ...getAuthHeaders()
            },
            body: JSON.stringify(campaignData)
        });
        
        if (!response.ok) {
            throw new Error('Ошибка сохранения черновика');
        }
        
        showToast('Черновик сохранён');
        await loadCampaigns();
        
    } catch (error) {
        console.error('Ошибка:', error);
        showToast(error.message, true);
    }
}

// ===== ДЕЙСТВИЯ С КАМПАНИЯМИ =====

async function viewCampaign(campaignId) {
    const campaign = campaigns.find(c => c.id === campaignId);
    if (!campaign) return;
    
    const content = `
        <div class="mb-3">
            <h4>${escapeHtml(campaign.name || 'Без названия')}</h4>
            <span class="badge ${getStatusClass(campaign.status)}">${getStatusText(campaign.status)}</span>
        </div>
        <p class="text-muted">${escapeHtml(campaign.description || 'Без описания')}</p>
        <hr>
        <div class="row">
            <div class="col-6">
                <p><strong>Тип:</strong> ${getTypeText(campaign.type)}</p>
                <p><strong>Скидка:</strong> ${campaign.discountPercent || 0}%</p>
                <p><strong>Мин. заказ:</strong> ${formatCurrency(campaign.minOrderAmount || 0)}</p>
                <p><strong>Срок действия:</strong> ${campaign.validDays || 7} дней</p>
            </div>
            <div class="col-6">
                <p><strong>Получателей:</strong> ${formatNumber(campaign.totalRecipients || 0)}</p>
                <p><strong>Отправлено:</strong> ${formatNumber(campaign.sentCount || 0)}</p>
                <p><strong>Доставлено:</strong> ${formatNumber(campaign.deliveredCount || 0)}</p>
                <p><strong>Целевые сегменты:</strong> ${campaign.targetSegments || 'Все'}</p>
            </div>
        </div>
        <hr>
        <h5>Статистика</h5>
        <div class="row text-center">
            <div class="col-3">
                <div class="h4 text-primary">${campaign.openedCount || 0}</div>
                <small>Открытий</small>
                <div class="progress mt-1" style="height: 6px;">
                    <div class="progress-bar bg-primary" style="width: ${campaign.openRate || 0}%"></div>
                </div>
            </div>
            <div class="col-3">
                <div class="h4 text-success">${campaign.clickedCount || 0}</div>
                <small>Кликов</small>
                <div class="progress mt-1" style="height: 6px;">
                    <div class="progress-bar bg-success" style="width: ${campaign.clickRate || 0}%"></div>
                </div>
            </div>
            <div class="col-3">
                <div class="h4 text-warning">${campaign.convertedCount || 0}</div>
                <small>Конверсий</small>
                <div class="progress mt-1" style="height: 6px;">
                    <div class="progress-bar bg-warning" style="width: ${campaign.conversionRate || 0}%"></div>
                </div>
            </div>
            <div class="col-3">
                <div class="h4 text-danger">${formatCurrency(campaign.revenueGenerated || 0)}</div>
                <small>Выручка</small>
            </div>
        </div>
        <hr>
        <h5>Содержимое ${campaign.type === 'EMAIL' ? 'письма' : 'сообщения'}</h5>
        ${campaign.type === 'EMAIL' ? `
            <div class="mkt-email-preview">
                <p><strong>Тема:</strong> ${escapeHtml(campaign.emailSubject || 'Не указана')}</p>
                <hr>
                <div>${escapeHtml(campaign.emailContent || 'Контент не указан').replace(/\n/g, '<br>')}</div>
            </div>
        ` : `
            <div class="alert alert-secondary">
                <strong>SMS:</strong> ${escapeHtml(campaign.smsText || 'Текст не указан')}
            </div>
        `}
        <hr>
        <div class="text-muted small">
            <p><strong>Создана:</strong> ${formatDateTime(campaign.createdAt)}</p>
            ${campaign.launchedAt ? `<p><strong>Запущена:</strong> ${formatDateTime(campaign.launchedAt)}</p>` : ''}
            ${campaign.completedAt ? `<p><strong>Завершена:</strong> ${formatDateTime(campaign.completedAt)}</p>` : ''}
        </div>
    `;
    
    document.getElementById('campaignDetailBody').innerHTML = content;
    document.getElementById('campaignDetailModal').style.display = 'flex';
}

async function launchCampaign(campaignId) {
    if (!confirm('Запустить кампанию?')) return;
    
    try {
        const response = await fetch(`${API_BASE}/marketing/campaigns/${campaignId}/launch`, {
            method: 'POST',
            headers: getAuthHeaders()
        });
        
        if (!response.ok) {
            throw new Error('Ошибка запуска кампании');
        }
        
        showToast('Кампания запущена');
        await loadCampaigns();
        await loadStats();
        
    } catch (error) {
        console.error('Ошибка:', error);
        showToast(error.message, true);
    }
}

async function pauseCampaign(campaignId) {
    if (!confirm('Приостановить кампанию?')) return;
    
    try {
        const response = await fetch(`${API_BASE}/marketing/campaigns/${campaignId}/pause`, {
            method: 'POST',
            headers: getAuthHeaders()
        });
        
        if (!response.ok) {
            throw new Error('Ошибка приостановки');
        }
        
        showToast('Кампания приостановлена');
        await loadCampaigns();
        
    } catch (error) {
        console.error('Ошибка:', error);
        showToast(error.message, true);
    }
}

async function cancelCampaign(campaignId) {
    if (!confirm('Отменить кампанию? Это действие необратимо.')) return;
    
    try {
        const response = await fetch(`${API_BASE}/marketing/campaigns/${campaignId}/cancel`, {
            method: 'POST',
            headers: getAuthHeaders()
        });
        
        if (!response.ok) {
            throw new Error('Ошибка отмены кампании');
        }
        
        showToast('Кампания отменена');
        await loadCampaigns();
        await loadStats();
        
    } catch (error) {
        console.error('Ошибка:', error);
        showToast(error.message, true);
    }
}

async function deleteCampaign(campaignId) {
    if (!confirm('Удалить кампанию? Это действие необратимо.')) return;
    
    try {
        const response = await fetch(`${API_BASE}/marketing/campaigns/${campaignId}`, {
            method: 'DELETE',
            headers: getAuthHeaders()
        });
        
        if (!response.ok) {
            throw new Error('Ошибка удаления');
        }
        
        showToast('Кампания удалена');
        await loadCampaigns();
        await loadStats();
        
    } catch (error) {
        console.error('Ошибка:', error);
        showToast(error.message, true);
    }
}

// ===== ШАБЛОНЫ =====

async function saveTemplate(event) {
    event.preventDefault();
    
    const templateData = {
        name: document.getElementById('tplName').value.trim(),
        description: document.getElementById('tplDescription').value.trim(),
        type: document.getElementById('tplType').value,
        category: document.getElementById('tplCategory').value,
        subjectTemplate: document.getElementById('tplSubject').value.trim(),
        contentTemplate: document.getElementById('tplContent').value.trim()
    };
    
    const templateId = document.getElementById('templateId').value;
    const url = templateId 
        ? `${API_BASE}/marketing/templates/${templateId}` 
        : `${API_BASE}/marketing/templates`;
    const method = templateId ? 'PUT' : 'POST';
    
    try {
        const response = await fetch(url, {
            method: method,
            headers: {
                'Content-Type': 'application/json',
                ...getAuthHeaders()
            },
            body: JSON.stringify(templateData)
        });
        
        if (!response.ok) {
            const error = await response.json();
            throw new Error(error.error || 'Ошибка сохранения шаблона');
        }
        
        showToast(templateId ? 'Шаблон обновлён' : 'Шаблон создан');
        closeTemplateModal();
        await loadTemplates();
        
    } catch (error) {
        console.error('Ошибка:', error);
        showToast(error.message, true);
    }
}

async function deleteTemplate(templateId) {
    if (!confirm('Удалить этот шаблон?')) return;
    
    try {
        const response = await fetch(`${API_BASE}/marketing/templates/${templateId}`, {
            method: 'DELETE',
            headers: getAuthHeaders()
        });
        
        if (!response.ok) {
            throw new Error('Ошибка удаления шаблона');
        }
        
        showToast('Шаблон удалён');
        await loadTemplates();
        
    } catch (error) {
        console.error('Ошибка:', error);
        showToast(error.message, true);
    }
}

// ===== АВТОМАТИЗАЦИИ =====

async function toggleAutomation(automationType, enabled) {
    try {
        const response = await fetch(`${API_BASE}/marketing/automations/${automationType}`, {
            method: enabled ? 'POST' : 'DELETE',
            headers: getAuthHeaders()
        });
        
        if (!response.ok) {
            throw new Error('Ошибка обновления автоматизации');
        }
        
        showToast(enabled ? 'Автоматизация включена' : 'Автоматизация выключена');
        
    } catch (error) {
        console.error('Ошибка:', error);
        showToast(error.message, true);
        // Возвращаем переключатель в исходное состояние
        const checkbox = document.getElementById(`auto${automationType.charAt(0).toUpperCase() + automationType.slice(1)}`);
        if (checkbox) {
            checkbox.checked = !enabled;
        }
    }
}

// ===== UI ВСПОМОГАТЕЛЬНЫЕ ФУНКЦИИ =====

function getAuthHeaders() {
    const token = localStorage.getItem('design_studio_token');
    return token ? { 'Authorization': `Bearer ${token}` } : {};
}

function getStatusText(status) {
    const statuses = {
        'DRAFT': 'Черновик',
        'SCHEDULED': 'Запланирована',
        'RUNNING': 'Активна',
        'COMPLETED': 'Завершена',
        'CANCELLED': 'Отменена',
        'PAUSED': 'Приостановлена'
    };
    return statuses[status] || status || 'Неизвестно';
}

function getStatusClass(status) {
    const classes = {
        'DRAFT': 'bg-secondary',
        'SCHEDULED': 'bg-info',
        'RUNNING': 'bg-success',
        'COMPLETED': 'bg-primary',
        'CANCELLED': 'bg-danger',
        'PAUSED': 'bg-warning'
    };
    return classes[status] || 'bg-secondary';
}

function getTypeText(type) {
    const types = {
        'EMAIL': 'Email',
        'SMS': 'SMS',
        'PUSH': 'Push',
        'NOTIFICATION': 'Уведомление',
        'AUTOMATED': 'Автоматическая'
    };
    return types[type] || type || 'Неизвестно';
}

function getCategoryText(category) {
    const categories = {
        'welcome': 'Приветственные',
        'promo': 'Промо-акции',
        'reactivation': 'Реактивация',
        'seasonal': 'Сезонные'
    };
    return categories[category] || category || 'Без категории';
}

function getTemplateIcon(type) {
    const icons = {
        'EMAIL': 'bi-envelope',
        'SMS': 'bi-phone',
        'PUSH': 'bi-bell',
        'NOTIFICATION': 'bi-info-circle'
    };
    return icons[type] || 'bi-file-text';
}

function formatCurrency(amount) {
    return new Intl.NumberFormat('ru-RU', {
        style: 'currency',
        currency: 'BYN',
        minimumFractionDigits: 0,
        maximumFractionDigits: 0
    }).format(amount || 0);
}

function formatNumber(num) {
    if (num === null || num === undefined) return '0';
    return new Intl.NumberFormat('ru-RU').format(Math.floor(num));
}

function formatDate(dateStr) {
    if (!dateStr) return '-';
    const date = new Date(dateStr);
    return date.toLocaleDateString('ru-RU', {
        day: '2-digit',
        month: '2-digit',
        year: 'numeric'
    });
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

function escapeHtml(text) {
    if (!text) return '';
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

function showToast(message, isError = false) {
    const toast = document.createElement('div');
    toast.className = `custom-toast ${isError ? 'error' : ''}`;
    toast.innerHTML = `
        <span><i class="bi ${isError ? 'bi-x-circle' : 'bi-check-circle'}"></i> ${message}</span>
        <button class="close-btn" onclick="this.parentElement.remove()">&times;</button>
    `;
    document.body.appendChild(toast);
    
    // Анимация появления
    setTimeout(() => toast.classList.add('show'), 10);
    
    // Удаление через 3 секунды
    setTimeout(() => {
        toast.classList.remove('show');
        setTimeout(() => toast.remove(), 500);
    }, 3000);
}

function setupEventListeners() {
    // Форма создания кампании
    const campaignForm = document.getElementById('campaignForm');
    if (campaignForm) {
        campaignForm.addEventListener('submit', saveCampaign);
    }
    
    // Форма шаблона
    const templateForm = document.getElementById('templateForm');
    if (templateForm) {
        templateForm.addEventListener('submit', saveTemplate);
    }
    
    // Переключение типа контента
    const campType = document.getElementById('campType');
    if (campType) {
        campType.addEventListener('change', toggleContentSection);
    }
    
    // Фильтр статусов кампаний
    const statusFilter = document.getElementById('campaignStatusFilter');
    if (statusFilter) {
        statusFilter.addEventListener('change', filterCampaigns);
    }
    
    // Автоматизации
    const autoFirstPurchase = document.getElementById('autoFirstPurchase');
    const autoAbandonedCart = document.getElementById('autoAbandonedCart');
    const autoBirthday = document.getElementById('autoBirthday');
    const autoReactivation = document.getElementById('autoReactivation');
    
    if (autoFirstPurchase) {
        autoFirstPurchase.addEventListener('change', (e) => toggleAutomation('first_purchase', e.target.checked));
    }
    if (autoAbandonedCart) {
        autoAbandonedCart.addEventListener('change', (e) => toggleAutomation('abandoned_cart', e.target.checked));
    }
    if (autoBirthday) {
        autoBirthday.addEventListener('change', (e) => toggleAutomation('birthday', e.target.checked));
    }
    if (autoReactivation) {
        autoReactivation.addEventListener('change', (e) => toggleAutomation('reactivation', e.target.checked));
    }
    
    // Закрытие модалок по клику вне окна
    document.querySelectorAll('.modal').forEach(modal => {
        modal.addEventListener('click', (e) => {
            if (e.target === modal) {
                modal.style.display = 'none';
            }
        });
    });
    
    // Закрытие модалки по Escape
    document.addEventListener('keydown', (e) => {
        if (e.key === 'Escape') {
            document.querySelectorAll('.modal').forEach(modal => {
                modal.style.display = 'none';
            });
        }
    });
}

// ===== ПЕРЕКЛЮЧЕНИЕ СЕКЦИЙ КОНТЕНТА =====

function toggleContentSection() {
    const type = document.getElementById('campType').value;
    const emailSection = document.getElementById('emailContentSection');
    const smsSection = document.getElementById('smsContentSection');
    
    if (type === 'SMS') {
        if (emailSection) emailSection.style.display = 'none';
        if (smsSection) smsSection.style.display = 'block';
    } else {
        if (emailSection) emailSection.style.display = 'block';
        if (smsSection) smsSection.style.display = 'none';
    }
}

function updateSmsLength() {
    const text = document.getElementById('campSmsText').value;
    const lengthEl = document.getElementById('smsLength');
    if (lengthEl) {
        lengthEl.textContent = text.length;
    }
}

function toggleTriggerSection() {
    const isAutomated = document.getElementById('campAutomated').checked;
    const triggerSection = document.getElementById('triggerSection');
    if (triggerSection) {
        triggerSection.style.display = isAutomated ? 'block' : 'none';
    }
}

// ===== ЗАКРЫТИЕ МОДАЛЬНЫХ ОКОН =====

function closeCampaignModal() {
    const modal = document.getElementById('campaignDetailModal');
    if (modal) modal.style.display = 'none';
}

function closeTemplateModal() {
    const modal = document.getElementById('templateModal');
    const form = document.getElementById('templateForm');
    
    if (form) form.reset();
    
    const templateIdInput = document.getElementById('templateId');
    if (templateIdInput) templateIdInput.value = '';
    
    const modalTitle = document.getElementById('templateModalTitle');
    if (modalTitle) {
        modalTitle.innerHTML = '<i class="bi bi-file-text"></i> Создать шаблон';
    }
    
    if (modal) modal.style.display = 'none';
}

function openTemplateModal(templateId = null) {
    const modal = document.getElementById('templateModal');
    if (modal) modal.style.display = 'flex';
    
    if (templateId) {
        const template = templates.find(t => t.id === templateId);
        if (template) {
            document.getElementById('templateId').value = template.id;
            document.getElementById('tplName').value = template.name || '';
            document.getElementById('tplDescription').value = template.description || '';
            document.getElementById('tplType').value = template.type || 'EMAIL';
            document.getElementById('tplCategory').value = template.category || 'welcome';
            document.getElementById('tplSubject').value = template.subjectTemplate || '';
            document.getElementById('tplContent').value = template.contentTemplate || '';
            
            const modalTitle = document.getElementById('templateModalTitle');
            if (modalTitle) {
                modalTitle.innerHTML = '<i class="bi bi-pencil"></i> Редактировать шаблон';
            }
        }
    }
}

// ===== СБРОС ФОРМЫ =====

function resetCampaignForm() {
    const form = document.getElementById('campaignForm');
    if (form) form.reset();
    
    editingCampaignId = null;
    
    // Сбрасываем видимость секций
    toggleContentSection();
    
    // Скрываем секцию триггера
    const triggerSection = document.getElementById('triggerSection');
    if (triggerSection) triggerSection.style.display = 'none';
    
    // Сбрасываем счётчик SMS
    updateSmsLength();
}

// ===== ФИЛЬТРАЦИЯ =====

function filterCampaigns() {
    const status = document.getElementById('campaignStatusFilter').value;
    const filtered = status 
        ? campaigns.filter(c => c.status === status) 
        : campaigns;
    renderCampaigns(filtered);
}

function filterTemplates(category) {
    // Обновляем активную кнопку
    document.querySelectorAll('#templateCategories .list-group-item').forEach(btn => {
        btn.classList.remove('active');
    });
    
    const activeBtn = Array.from(document.querySelectorAll('#templateCategories .list-group-item'))
        .find(btn => btn.textContent.trim().toLowerCase().includes(category.toLowerCase()) || 
                    (category === 'all' && btn.textContent.includes('Все')));
    if (activeBtn) activeBtn.classList.add('active');
    
    // Фильтруем и рендерим
    const filtered = category === 'all' 
        ? templates 
        : templates.filter(t => t.category === category);
    renderTemplates(filtered);
}

// ===== ВЫХОД =====

function handleLogout(event) {
    event.preventDefault();
    localStorage.removeItem('design_studio_user');
    localStorage.removeItem('design_studio_token');
    window.location.href = 'index.html';
}

// ===== ЭКСПОРТ МОДУЛЯ =====

window.MarketingModule = {
    loadStats,
    loadCampaigns,
    loadTemplates,
    saveCampaign,
    saveCampaignDraft,
    saveTemplate,
    viewCampaign,
    launchCampaign,
    pauseCampaign,
    cancelCampaign,
    deleteCampaign,
    deleteTemplate,
    selectTemplate,
    filterCampaigns,
    filterTemplates,
    toggleAutomation,
    showToast
};

// Глобальные функции для обратной совместимости
window.launchCampaign = launchCampaign;
window.pauseCampaign = pauseCampaign;
window.cancelCampaign = cancelCampaign;
window.deleteCampaign = deleteCampaign;
window.viewCampaign = viewCampaign;
window.openTemplateModal = openTemplateModal;
window.closeTemplateModal = closeTemplateModal;
window.filterTemplates = filterTemplates;
window.handleLogout = handleLogout;
window.showToast = showToast;

console.log('marketing.js v2.0 fully loaded');