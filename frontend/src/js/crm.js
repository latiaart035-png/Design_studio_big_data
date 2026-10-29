// ============================================
// CRM Module JavaScript - v2.6 ФИНАЛЬНЫЙ
// Сегменты: VIP, PREMIUM, BASIC, ACTIVE, LOST
// ============================================

const API_BASE_URL = 'http://localhost:8080';
let segmentPieChart = null;
let revenueBarChart = null;
let paretoChartInstance = null;
let funnelChartInstance = null;
let allCustomers = [];
let allSegments = [];
let currentSegmentFilter = '';
let currentSearchQuery = '';
let currentSpentFilter = '';
let currentPage = 1;
const itemsPerPage = 10;

let customersData = {
    total: 0,
    active: 0,
    newToday: 0,
    newThisWeek: 0,
    newThisMonth: 0,
    orders: 0,
    revenue: 0
};

// ========== API ==========

function getToken() {
    return localStorage.getItem('design_studio_token');
}

async function apiFetch(path, options = {}) {
    const token = getToken();
    const headers = {
        'Content-Type': 'application/json',
        ...(options.headers || {})
    };

    if (token) {
        headers['Authorization'] = 'Bearer ' + token;
    }

    try {
        const response = await fetch(`${API_BASE_URL}${path}`, {
            ...options,
            headers
        });

        if (response.status === 401) {
            localStorage.removeItem('design_studio_token');
            window.location.href = 'login.html';
            return null;
        }

        return response;
    } catch (error) {
        console.error('API Error:', error);
        throw error;
    }
}

// ========== ИНИЦИАЛИЗАЦИЯ ==========

document.addEventListener('DOMContentLoaded', async () => {
    console.log('CRM initialized');

    const dateEl = document.getElementById('currentDate');
    if (dateEl) {
        dateEl.textContent = new Date().toLocaleDateString('ru-RU', {
            weekday: 'long', year: 'numeric', month: 'long', day: 'numeric'
        });
    }

    initPeriodSelector();
    initForms();
    initTabs();
    initModals();
    initBulkSelection();

    try {
        await loadCrmStats();
        await loadSegments();
        await loadCustomers();
        await loadActivityStats();  // Добавьте эту строку
        console.log('All data loaded successfully');
    } catch (err) {
        console.error('Error loading CRM data:', err);
        showToast('Ошибка загрузки данных CRM', true);
    }
});

// ========== ПЕРИОД ==========

function initPeriodSelector() {
    document.querySelectorAll('.period-btn').forEach(btn => {
        btn.addEventListener('click', async () => {
            document.querySelectorAll('.period-btn').forEach(b => b.classList.remove('active'));
            btn.classList.add('active');
            await loadCrmStats(btn.dataset.period);
        });
    });
}

// ========== ЗАГРУЗКА СТАТИСТИКИ ==========

async function loadCrmStats(period = 'month') {
    try {
        console.log('=== CRM LOADING ===');
        console.log('Loading CRM stats with period:', period);

        const res = await apiFetch(`/api/crm/stats?period=${period}`);

        if (!res) {
            console.error('API вернул null');
            showToast('Ошибка соединения с сервером', true);
            return;
        }

        if (!res.ok) {
            console.error('API вернул ошибку:', res.status, res.statusText);
            showToast(`Ошибка загрузки данных: ${res.status}`, true);
            return;
        }

        const stats = await res.json();

        if (!stats) {
            console.error('API вернул null/undefined вместо данных');
            showToast('Сервер вернул пустой ответ', true);
            return;
        }

        // Сохраняем данные
        customersData = {
            total: stats.totalCustomers || 0,
            active: stats.activeUsersLast30Days || 0,
            newToday: stats.newCustomersThisMonth || 0,
            newThisWeek: stats.newCustomersThisWeek || 0,
            newThisMonth: stats.newCustomersThisMonth || 0,
            orders: stats.totalOrders || 0,
            revenue: stats.totalRevenue || 0
        };

        // KPI карточки - основные значения
        updateElement('totalCustomers', formatNumber(stats.totalCustomers || 0));
        updateElement('totalRevenue', formatCurrency(stats.totalRevenue || 0));
        updateElement('avgOrderValue', formatCurrency(stats.avgOrderValue || 0));
        updateElement('avgCustomerValue', formatCurrency(stats.avgCustomerValue || 0));
        updateElement('totalOrders', formatNumber(stats.totalOrders || 0));

        // Новые клиенты
        updateElement('newToday', formatNumber(stats.newCustomersThisMonth || 0));
        updateElement('newThisWeek', formatNumber(stats.newCustomersThisWeek || 0));
        updateElement('newThisMonth', formatNumber(stats.newCustomersThisMonth || 0));

        // Конверсия и удержание
        updateElement('conversionRate', formatPercent(stats.conversionRate || 0));
        updateElement('retentionRate', formatPercent(stats.retentionRate || 0));
        updateElement('churnRate', formatPercent(stats.churnRate || 0));

        // ТРЕНДЫ
        updateTrendElement('customerTrend', stats.customersChangePercent);
        updateTrendElement('revenueTrend', stats.revenueChangePercent);
        updateTrendElement('avgCheckTrend', stats.avgCheckChangePercent);
        updateTrendElement('clvTrend', stats.avgCheckChangePercent);
        updateTrendElement('orderTrend', stats.ordersChangePercent);
        updateTrendElement('conversionTrend', stats.conversionChangePercent);

        // Счётчик во вкладке
        updateElement('tabCustomerCount', formatNumber(stats.totalCustomers || 0));

        // Сегменты
        updateStandardSegments(stats.segmentStats || {});
        renderSegmentsQuickView(stats.segmentStats || {});

        // Графики
        renderSegmentPieChart(stats.segmentDistribution || []);
        renderRevenueBarChart(stats.segmentStats || {});
        renderRevenueTrendChart(stats.adminSalesByMonth || {});

        // RFM и ABC
        renderRfmAnalysisFromStats(stats.rfmAnalysis || [], stats.totalCustomers || 0);
        renderAbcAnalysisFromStats(stats.abcAnalysis || []);

        console.log('=== END CRM STATS ===\n');

    } catch (error) {
        console.error('Ошибка загрузки статистики CRM:', error);
        showToast('Ошибка загрузки данных: ' + error.message, true);
    }
}

// ========== ТРЕНДЫ ==========

function updateTrendElement(id, percent) {
    const el = document.getElementById(id);
    if (!el) return;

    if (percent === null || percent === undefined || percent === '') {
        el.className = 'kpi-trend neutral';
        el.innerHTML = `<i class="bi bi-dash"></i> —`;
        return;
    }

    const value = parseFloat(percent);

    if (isNaN(value)) {
        el.className = 'kpi-trend neutral';
        el.innerHTML = `<i class="bi bi-dash"></i> —`;
        return;
    }

    el.className = 'kpi-trend';
    const absValue = Math.abs(value);

    if (value > 0) {
        el.classList.add('up');
        el.innerHTML = `<i class="bi bi-arrow-up-right"></i> +${absValue.toFixed(1)}%`;
    } else if (value < 0) {
        el.classList.add('down');
        el.innerHTML = `<i class="bi bi-arrow-down-right"></i> -${absValue.toFixed(1)}%`;
    } else {
        el.classList.add('neutral');
        el.innerHTML = `<i class="bi bi-dash"></i> 0%`;
    }
}

// ========== ЗАГРУЗКА ДАННЫХ ==========

async function loadSegments() {
    try {
        const res = await apiFetch('/api/crm/segments');
        if (!res || !res.ok) return;
        allSegments = await res.json();
        renderSegments();
    } catch (error) {
        console.error('Ошибка загрузки сегментов:', error);
    }
}

async function loadCustomers() {
    try {
        const res = await apiFetch('/api/crm/customers');
        if (!res || !res.ok) return;
        allCustomers = await res.json();
        renderCustomers();
    } catch (error) {
        console.error('Ошибка загрузки клиентов:', error);
    }
}

// ========== ОБНОВЛЕНИЕ ЭЛЕМЕНТОВ ==========

function updateElement(id, value) {
    const el = document.getElementById(id);
    if (el) el.textContent = value;
}

// ========== СТАНДАРТНЫЕ СЕГМЕНТЫ ==========

function updateStandardSegments(segmentStats) {
    const segments = ['VIP', 'PREMIUM', 'ACTIVE', 'BASIC', 'LOST'];
    const totalCustomers = Object.values(segmentStats).reduce((sum, s) => sum + (s.count || 0), 0);

    segments.forEach(segment => {
        const stats = segmentStats[segment] || { count: 0, avgCheck: 0, totalRevenue: 0 };
        const lower = segment.toLowerCase();

        const countEl = document.getElementById(`${lower}Count`);
        const revenueEl = document.getElementById(`${lower}Revenue`);
        const avgCheckEl = document.getElementById(`${lower}AvgCheck`);
        const barEl = document.getElementById(`${lower}Bar`);

        if (countEl) countEl.textContent = formatNumber(stats.count || 0);
        if (revenueEl) revenueEl.textContent = formatCurrency(stats.totalRevenue || 0);
        if (avgCheckEl) avgCheckEl.textContent = formatCurrency(stats.avgCheck || 0);

        if (barEl) {
            const percent = totalCustomers > 0 ? ((stats.count || 0) / totalCustomers) * 100 : 0;
            barEl.style.width = Math.min(percent, 100) + '%';
        }
    });
}

// ========== ГРАФИКИ ==========

function renderSegmentPieChart(distribution) {
    const ctx = document.getElementById('segmentPieChart');
    const container = ctx?.parentElement;
    if (!ctx || !container) return;

    if (segmentPieChart) segmentPieChart.destroy();

    if (!distribution || distribution.length === 0) {
        container.innerHTML = `
            <div class="segment-pie-empty">
                <div class="pie-empty-icon">
                    <i class="bi bi-pie-chart"></i>
                </div>
                <p class="pie-empty-text">Нет данных по сегментам</p>
            </div>`;
        return;
    }

    const labels = distribution.map(d => d.segment || 'Unknown');
    const data = distribution.map(d => d.count || 0);
    const revenues = distribution.map(d => d.totalRevenue || 0);
    const totalCustomers = data.reduce((a, b) => a + b, 0);
    const totalRevenue = revenues.reduce((a, b) => a + b, 0);

    console.log('=== PIE CHART DATA ===');
    console.log('Labels:', labels);
    console.log('Data:', data);
    console.log('Total:', totalCustomers);
    
    // Цвета для 5 сегментов
    const segmentColors = {
        'VIP': '#28a745',
        'PREMIUM': '#17a2b8', 
        'ACTIVE': '#007bff',
        'BASIC': '#6c757d',
        'LOST': '#dc3545'
    };

    const hoverColors = {
        'VIP': '#1e7e34',
        'PREMIUM': '#138496',
        'ACTIVE': '#0069d9', 
        'BASIC': '#5a6268',
        'LOST': '#c82333'
    };

    const backgroundColors = labels.map(l => segmentColors[l] || '#6c757d');
    const hoverBackgroundColors = labels.map(l => hoverColors[l] || '#5a6268');

    container.innerHTML = `
        <div class="segment-pie-wrapper">
            <div class="segment-pie-chart-container">
                <canvas id="segmentPieChart"></canvas>
                <div class="segment-pie-center">
                    <div class="pie-center-value">${formatNumber(totalCustomers)}</div>
                    <div class="pie-center-label">клиентов</div>
                    <div class="pie-center-revenue">${formatCurrency(totalRevenue)}</div>
                </div>
            </div>
            <div class="segment-pie-legend" id="segmentPieLegend"></div>
        </div>`;

    const legendContainer = document.getElementById('segmentPieLegend');
    if (legendContainer) {
        legendContainer.innerHTML = distribution.map((d, i) => {
            const color = segmentColors[d.segment] || '#6c757d';
            const percent = totalCustomers > 0 ? ((d.count || 0) / totalCustomers * 100).toFixed(1) : '0.0';
            return `
                <div class="pie-legend-item" data-segment="${d.segment}" data-index="${i}">
                    <div class="pie-legend-color" style="background: ${color};"></div>
                    <div class="pie-legend-info">
                        <span class="pie-legend-name">${d.segment}</span>
                        <span class="pie-legend-count">${formatNumber(d.count || 0)} чел.</span>
                    </div>
                    <div class="pie-legend-stats">
                        <span class="pie-legend-percent">${percent}%</span>
                        <span class="pie-legend-revenue">${formatCurrency(d.totalRevenue || 0)}</span>
                    </div>
                </div>
            `;
        }).join('');
    }

    const newCtx = document.getElementById('segmentPieChart');
    
    segmentPieChart = new Chart(newCtx, {
        type: 'doughnut',
        data: {
            labels: labels,
            datasets: [{
                data: data,
                backgroundColor: backgroundColors,
                hoverBackgroundColor: hoverBackgroundColors,
                borderWidth: 3,
                borderColor: '#ffffff',
                hoverBorderWidth: 4,
                hoverOffset: 8
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: true,
            cutout: '65%',
            animation: {
                animateRotate: true,
                animateScale: true,
                duration: 1000,
                easing: 'easeOutQuart'
            },
            plugins: {
                legend: { display: false },
                tooltip: {
                    backgroundColor: 'rgba(33, 37, 41, 0.95)',
                    titleColor: '#ffffff',
                    bodyColor: '#ffffff',
                    titleFont: { size: 14, weight: '600' },
                    bodyFont: { size: 13 },
                    padding: 14,
                    cornerRadius: 8,
                    callbacks: {
                        label: function(context) {
                            const percent = totalCustomers > 0 
                                ? ((context.raw / totalCustomers) * 100).toFixed(1) 
                                : '0.0';
                            const segRevenue = revenues[context.dataIndex] || 0;
                            const avgCheck = context.raw > 0 
                                ? segRevenue / context.raw 
                                : 0;
                            return [
                                `Клиентов: ${formatNumber(context.raw)} (${percent}%)`,
                                `Выручка: ${formatCurrency(segRevenue)}`,
                                `Ср. чек: ${formatCurrency(avgCheck)}`
                            ];
                        }
                    }
                }
            },
            onHover: (event, elements) => {
                if (legendContainer) {
                    legendContainer.querySelectorAll('.pie-legend-item').forEach(item => {
                        item.classList.remove('active');
                    });
                    if (elements.length > 0) {
                        const idx = elements[0].index;
                        const legendItem = legendContainer.querySelector(`[data-index="${idx}"]`);
                        if (legendItem) legendItem.classList.add('active');
                    }
                }
            }
        }
    });
}

function renderRevenueBarChart(segmentStats) {
    const ctx = document.getElementById('revenueBarChart');
    if (!ctx) return;

    if (revenueBarChart) revenueBarChart.destroy();

    const segments = Object.keys(segmentStats);
    if (segments.length === 0) {
        ctx.parentElement.innerHTML = '<div class="text-center text-muted p-4">Нет данных</div>';
        return;
    }

    const revenues = segments.map(s => segmentStats[s].totalRevenue || 0);
    
    const colors = segments.map(s => {
        const map = { 
            'VIP': '#28a745', 
            'PREMIUM': '#17a2b8', 
            'BASIC': '#6c757d',
            'ACTIVE': '#007bff', 
            'LOST': '#dc3545' 
        };
        return map[s] || '#6B1F3F';
    });

    revenueBarChart = new Chart(ctx, {
        type: 'bar',
        data: {
            labels: segments,
            datasets: [{
                label: 'Выручка (BYN)',
                data: revenues,
                backgroundColor: colors,
                borderRadius: 6
            }]
        },
        options: {
            responsive: true,
            plugins: { legend: { display: false } },
            scales: {
                y: { beginAtZero: true, ticks: { callback: v => formatCurrency(v) } }
            }
        }
    });
}

function renderRevenueTrendChart(salesData) {
    const ctx = document.getElementById('revenueTrendChart');
    if (!ctx) {
        console.error('Canvas element revenueTrendChart not found');
        return;
    }

    // Проверяем, что данные существуют
    if (!salesData || typeof salesData !== 'object' || Object.keys(salesData).length === 0) {
        console.warn('No sales data available for revenue trend chart');
        const container = ctx.parentElement;
        if (container) {
            container.innerHTML = '<div class="text-center text-muted p-4">Нет данных для отображения</div>';
        }
        return;
    }

    // Получаем месяцы и сортируем их по дате
    const months = Object.keys(salesData).sort();
    const values = months.map(m => salesData[m] || 0);
    
    // Форматируем метки для отображения (Май 2024, Июнь 2024 и т.д.)
    const labels = months.map(month => {
        const [year, monthNum] = month.split('-');
        const date = new Date(parseInt(year), parseInt(monthNum) - 1);
        return date.toLocaleDateString('ru-RU', { month: 'long', year: 'numeric' });
    });

    console.log('Revenue Trend Chart Data:', { 
        months: months, 
        values: values.map(v => formatCurrency(v)),
        labels: labels 
    });

    // Уничтожаем старый график, если он существует
    if (window.revenueTrendChartInstance) {
        window.revenueTrendChartInstance.destroy();
        window.revenueTrendChartInstance = null;
    }

    try {
        window.revenueTrendChartInstance = new Chart(ctx, {
            type: 'line',
            data: {
                labels: labels,
                datasets: [{
                    label: 'Выручка',
                    data: values,
                    borderColor: '#6B1F3F',
                    backgroundColor: 'rgba(107, 31, 63, 0.1)',
                    borderWidth: 3,
                    fill: true,
                    tension: 0.3,
                    pointRadius: 5,
                    pointBackgroundColor: '#6B1F3F',
                    pointBorderColor: '#fff',
                    pointBorderWidth: 2,
                    pointHoverRadius: 8
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: true,
                plugins: {
                    legend: { display: false },
                    tooltip: {
                        callbacks: {
                            label: function(context) {
                                return `Выручка: ${formatCurrency(context.raw)}`;
                            }
                        }
                    }
                },
                scales: {
                    y: {
                        beginAtZero: true,
                        title: { display: true, text: 'Выручка (BYN)', font: { size: 11 } },
                        ticks: { 
                            callback: function(value) {
                                return formatCurrency(value);
                            },
                            font: { size: 10 }
                        }
                    },
                    x: {
                        ticks: { 
                            font: { size: 10 },
                            maxRotation: 45,
                            minRotation: 45
                        }
                    }
                }
            }
        });
    } catch (error) {
        console.error('Error creating revenue trend chart:', error);
        const container = ctx.parentElement;
        if (container) {
            container.innerHTML = '<div class="text-center text-danger p-4">Ошибка загрузки графика</div>';
        }
    }
}

// ========== БЫСТРЫЙ ОБЗОР СЕГМЕНТОВ ==========

function renderSegmentsQuickView(segmentStats) {
    const container = document.getElementById('segmentsQuickView');
    if (!container) return;

    const totalCustomers = Object.values(segmentStats).reduce((sum, s) => sum + (s.count || 0), 0);

    const segments = [
        { key: 'VIP', name: 'VIP', icon: 'bi-gem', color: '#28a745' },
        { key: 'PREMIUM', name: 'Premium', icon: 'bi-star-fill', color: '#17a2b8' },
        { key: 'BASIC', name: 'Basic', icon: 'bi-person-plus', color: '#6c757d' },
        { key: 'ACTIVE', name: 'Активные', icon: 'bi-activity', color: '#007bff' },
        { key: 'LOST', name: 'Потерянные', icon: 'bi-emoji-dizzy', color: '#dc3545' }
    ];

    container.innerHTML = segments.map(seg => {
        const stats = segmentStats[seg.key] || { count: 0, totalRevenue: 0 };
        const percent = totalCustomers > 0 ? ((stats.count || 0) / totalCustomers * 100).toFixed(1) : 0;

        return `
            <div class="segment-quick-item" onclick="filterCustomersBySegment('${seg.key}')"
                 style="border-left: 3px solid ${seg.color};">
                <div class="quick-icon" style="background: ${seg.color}20; color: ${seg.color};">
                    <i class="bi ${seg.icon}"></i>
                </div>
                <div class="quick-info">
                    <div class="quick-name">${seg.name}</div>
                    <div class="quick-count">${formatNumber(stats.count || 0)} (${percent}%)</div>
                </div>
                <div class="quick-revenue">${formatCurrency(stats.totalRevenue || 0)}</div>
            </div>
        `;
    }).join('');
}

// ========== ПОЛЬЗОВАТЕЛЬСКИЕ СЕГМЕНТЫ ==========

function renderSegments() {
    const container = document.getElementById('rfmSegmentsList');
    if (!container) return;

    if (!allSegments || allSegments.length === 0) {
        container.innerHTML = '<div class="alert alert-info"><i class="bi bi-info-circle"></i> Пользовательских сегментов пока нет</div>';
        return;
    }

    container.innerHTML = allSegments.map(segment => {
        // Форматируем условия для отображения
        let conditions = [];
        if (segment.minOrders && segment.minOrders > 0) conditions.push(`Заказов ≥ ${segment.minOrders}`);
        if (segment.maxOrders && segment.maxOrders < 999) conditions.push(`Заказов ≤ ${segment.maxOrders}`);
        if (segment.minSpent && segment.minSpent > 0) conditions.push(`Траты ≥ ${formatCurrency(segment.minSpent)}`);
        if (segment.maxSpent && segment.maxSpent < 999999) conditions.push(`Траты ≤ ${formatCurrency(segment.maxSpent)}`);
        if (segment.statusRequirement) conditions.push(`Статус: ${segment.statusRequirement}`);
        
        const conditionsText = conditions.length > 0 ? conditions.join(', ') : 'Без условий';
        
        return `
            <div class="segment-card" style="border-left-color: ${segment.color || '#6B1F3F'};">
                <div class="d-flex justify-content-between align-items-center">
                    <h5><i class="${segment.icon || 'bi bi-folder'}"></i> ${escapeHtml(segment.name)}</h5>
                    <span class="badge" style="background: ${segment.color || '#6B1F3F'}; color: white;">
                        ${segment.totalCustomers || 0} клиентов
                    </span>
                </div>
                <p class="text-muted mb-1 small">${escapeHtml(segment.description || '')}</p>
                <div class="small text-muted mb-2">
                    <i class="bi bi-filter"></i> ${conditionsText}
                </div>
                <div class="row mb-2">
                    <div class="col-6">
                        <small class="text-muted">Выручка</small>
                        <div class="fw-bold">${formatCurrency(segment.totalRevenue || 0)}</div>
                    </div>
                    <div class="col-6">
                        <small class="text-muted">Средний чек</small>
                        <div class="fw-bold">${formatCurrency(segment.avgOrderValue || 0)}</div>
                    </div>
                </div>
                <div class="d-flex flex-wrap gap-2 mt-2">
                    <button class="btn btn-sm btn-primary" onclick="showCustomSegmentCustomers(${segment.id}, '${escapeHtml(segment.name)}')">
                        <i class="bi bi-people"></i> Показать
                    </button>
                    <button class="btn btn-sm btn-outline-primary" onclick="editSegment(${segment.id})">
                        <i class="bi bi-pencil"></i>
                    </button>
                    <button class="btn btn-sm btn-outline-danger" onclick="deleteSegment(${segment.id})">
                        <i class="bi bi-trash"></i>
                    </button>
                </div>
            </div>
        `;
    }).join('');
}

// Вспомогательная функция для экранирования HTML
function escapeHtml(text) {
    if (!text) return '';
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

// ========== КЛИЕНТЫ: ФИЛЬТРАЦИЯ И ПАГИНАЦИЯ ==========

function renderCustomers() {
    const tbody = document.getElementById('customersTableBody');
    if (!tbody) return;

    let filtered = allCustomers;

    if (currentSegmentFilter) {
        filtered = filtered.filter(c => c.segment === currentSegmentFilter);
    }

    if (currentSpentFilter) {
        const parts = currentSpentFilter.split('-');
        const min = parseFloat(parts[0]) || 0;
        const max = parts[1] ? parseFloat(parts[1].replace('+', '')) : Infinity;

        filtered = filtered.filter(c => {
            const spent = c.totalSpent || 0;
            return spent >= min && (max === Infinity || spent <= max);
        });
    }

    if (currentSearchQuery) {
        const query = currentSearchQuery.toLowerCase();
        filtered = filtered.filter(c =>
            ((c.firstName || '') + ' ' + (c.lastName || '')).toLowerCase().includes(query) ||
            (c.email || '').toLowerCase().includes(query) ||
            (c.phone || '').toLowerCase().includes(query)
        );
    }

    const totalItems = filtered.length;
    const totalPages = Math.max(1, Math.ceil(totalItems / itemsPerPage));
    if (currentPage > totalPages) currentPage = totalPages;

    const startIndex = (currentPage - 1) * itemsPerPage;
    const paginated = filtered.slice(startIndex, startIndex + itemsPerPage);

    updateElement('customersShown', Math.min(startIndex + paginated.length, totalItems));
    updateElement('customersTotal', totalItems);
    updateElement('customersTotalInfo', `(всего ${totalItems})`);

    if (paginated.length === 0) {
        tbody.innerHTML = `
            <tr>
                <td colspan="11" class="text-center py-5">
                    <div class="empty-state">
                        <i class="bi bi-search"></i>
                        <p>Клиенты не найдены</p>
                        <button class="btn btn-sm btn-outline-primary" onclick="clearFilters()">Сбросить фильтры</button>
                    </div>
                </td>
            </tr>`;
        renderPagination(totalPages);
        return;
    }

    tbody.innerHTML = paginated.map(customer => {
        const fullName = `${customer.firstName || ''} ${customer.lastName || ''}`.trim() || '—';
        const initials = `${(customer.firstName || '')[0] || ''}${(customer.lastName || '')[0] || ''}`.toUpperCase();
        const segmentColor = getSegmentColor(customer.segment);
        const rfmBadgeClass = (customer.rfmSegment || 'na').toLowerCase().replace(' ', '-');

        return `
            <tr onclick="showCustomerDetail(${customer.id})" style="cursor:pointer;">
                <td><input type="checkbox" class="form-check-input customer-checkbox" value="${customer.id}" onclick="event.stopPropagation();" onchange="updateBulkActions()"></td>
                <td>
                    <div class="d-flex align-items-center gap-2">
                        <div class="customer-avatar-sm" style="background: ${segmentColor}20; color: ${segmentColor};">
                            ${initials}
                        </div>
                        <div>
                            <div class="fw-medium">${fullName}</div>
                            <div class="small text-muted">${customer.phone || ''}</div>
                        </div>
                    </div>
                </td>
                <td>${customer.email || '—'}</td>
                <td>
                    <span class="badge" style="background: ${segmentColor}20; color: ${segmentColor};">
                        ${customer.segment || 'BASIC'}
                    </span>
                </td>
                <td>${formatNumber(customer.totalOrders || 0)}</td>
                <td class="fw-medium">${formatCurrency(customer.totalSpent || 0)}</td>
                <td>${formatCurrency(customer.avgOrderValue || 0)}</td>
                <td>
                    <span class="badge rfm-badge ${rfmBadgeClass}">
                        ${customer.rfmSegment || 'N/A'}
                    </span>
                </td>
                <td>${formatCurrency(customer.clv || 0)}</td>
                <td>
                    ${customer.daysSinceLastOrder !== null ?
                        `<span class="badge ${customer.daysSinceLastOrder > 180 ? 'bg-danger' : customer.daysSinceLastOrder > 90 ? 'bg-warning text-dark' : 'bg-success'}">
                            ${customer.daysSinceLastOrder} дн.
                        </span>` : '<span class="text-muted">—</span>'}
                </td>
                <td>
                    <div class="dropdown">
                        <button class="btn btn-sm btn-outline-secondary dropdown-toggle" data-bs-toggle="dropdown" onclick="event.stopPropagation();">
                            <i class="bi bi-three-dots-vertical"></i>
                        </button>
                        <ul class="dropdown-menu dropdown-menu-end">
                            <li><a class="dropdown-item" href="#" onclick="event.preventDefault(); event.stopPropagation(); showCustomerDetail(${customer.id});">
                                <i class="bi bi-eye"></i> Профиль
                            </a></li>
                            <li><a class="dropdown-item" href="#" onclick="event.preventDefault(); event.stopPropagation(); createCampaignForCustomer(${customer.id});">
                                <i class="bi bi-envelope"></i> Написать
                            </a></li>
                        </ul>
                    </div>
                </td>
            </tr>
        `;
    }).join('');

    renderPagination(totalPages);
}

function renderPagination(totalPages) {
    const pagination = document.getElementById('customersPagination');
    if (!pagination) return;

    if (totalPages <= 1) {
        pagination.innerHTML = '';
        return;
    }

    let html = '';

    html += `<li class="page-item ${currentPage === 1 ? 'disabled' : ''}">
        <a class="page-link" href="#" onclick="event.preventDefault(); goToPage(${currentPage - 1})">&laquo;</a>
    </li>`;

    for (let i = 1; i <= totalPages; i++) {
        if (i === 1 || i === totalPages || (i >= currentPage - 1 && i <= currentPage + 1)) {
            html += `<li class="page-item ${currentPage === i ? 'active' : ''}">
                <a class="page-link" href="#" onclick="event.preventDefault(); goToPage(${i})">${i}</a>
            </li>`;
        } else if (i === currentPage - 2 || i === currentPage + 2) {
            html += `<li class="page-item disabled"><span class="page-link">...</span></li>`;
        }
    }

    html += `<li class="page-item ${currentPage === totalPages ? 'disabled' : ''}">
        <a class="page-link" href="#" onclick="event.preventDefault(); goToPage(${currentPage + 1})">&raquo;</a>
    </li>`;

    pagination.innerHTML = html;
}

// ========== ФИЛЬТРАЦИЯ ==========

function filterCustomers() {
    currentSegmentFilter = document.getElementById('customerSegmentFilter')?.value || '';
    currentSpentFilter = document.getElementById('customerSpentFilter')?.value || '';
    currentSearchQuery = document.getElementById('customerSearch')?.value || '';
    currentPage = 1;
    renderCustomers();
}

function filterCustomersBySegment(segment) {
    currentSegmentFilter = segment;
    currentSpentFilter = '';
    currentSearchQuery = '';
    currentPage = 1;

    const segmentFilter = document.getElementById('customerSegmentFilter');
    if (segmentFilter) segmentFilter.value = segment;

    const spentFilter = document.getElementById('customerSpentFilter');
    if (spentFilter) spentFilter.value = '';

    renderCustomers();

    const tab = document.querySelector('[data-bs-target="#customers-tab"]');
    if (tab) {
        const bsTab = new bootstrap.Tab(tab);
        bsTab.show();
    }
}

function goToPage(page) {
    const totalItems = allCustomers.length;
    const totalPages = Math.ceil(totalItems / itemsPerPage);
    if (page < 1 || page > totalPages) return;
    currentPage = page;
    renderCustomers();
}

function clearFilters() {
    currentSegmentFilter = '';
    currentSpentFilter = '';
    currentSearchQuery = '';
    currentPage = 1;

    const segmentFilter = document.getElementById('customerSegmentFilter');
    const spentFilter = document.getElementById('customerSpentFilter');
    const searchInput = document.getElementById('customerSearch');

    if (segmentFilter) segmentFilter.value = '';
    if (spentFilter) spentFilter.value = '';
    if (searchInput) searchInput.value = '';

    renderCustomers();
}

// ========== BULK ACTIONS ==========

function initBulkSelection() {
    const selectAll = document.getElementById('selectAllCustomers');
    if (selectAll) {
        selectAll.addEventListener('change', (e) => {
            document.querySelectorAll('.customer-checkbox').forEach(cb => cb.checked = e.target.checked);
            updateBulkActions();
        });
    }
}

function updateBulkActions() {
    const checked = document.querySelectorAll('.customer-checkbox:checked');
    const bar = document.getElementById('bulkActionsBar');
    const count = document.getElementById('selectedCount');

    if (bar && count) {
        bar.style.display = checked.length > 0 ? 'block' : 'none';
        count.textContent = checked.length;
    }
}

function clearSelection() {
    document.querySelectorAll('.customer-checkbox').forEach(cb => cb.checked = false);
    const selectAll = document.getElementById('selectAllCustomers');
    if (selectAll) selectAll.checked = false;
    updateBulkActions();
}

function bulkAction(type) {
    const ids = Array.from(document.querySelectorAll('.customer-checkbox:checked')).map(cb => cb.value);

    switch(type) {
        case 'email':
            openBulkEmailModal(ids);
            break;
        case 'sms':
            showToast('SMS рассылка в разработке', true);
            break;
        case 'segment':
            showToast('Перемещение в сегмент в разработке', true);
            break;
    }
}

// ========== DRAWER КЛИЕНТА ==========

function showCustomerDetail(customerId) {
    const customer = allCustomers.find(c => c.id === customerId);
    if (!customer) {
        showToast('Клиент не найден', true);
        return;
    }

    const drawer = document.getElementById('customerDrawer');
    const overlay = document.getElementById('customerDrawerOverlay');

    const fullName = `${customer.firstName || ''} ${customer.lastName || ''}`.trim() || '—';
    updateElement('drawerName', fullName);

    const segmentBadge = document.getElementById('drawerSegment');
    if (segmentBadge) {
        segmentBadge.textContent = customer.segment || 'BASIC';
        segmentBadge.style.background = getSegmentColor(customer.segment) + '20';
        segmentBadge.style.color = getSegmentColor(customer.segment);
    }

    const avatar = document.getElementById('drawerAvatar');
    if (avatar) {
        avatar.textContent = `${(customer.firstName || '')[0] || ''}${(customer.lastName || '')[0] || ''}`.toUpperCase();
        avatar.style.background = getSegmentColor(customer.segment);
    }

    const metricsContainer = document.getElementById('drawerMetrics');
    if (metricsContainer) {
        metricsContainer.innerHTML = `
            <div class="drawer-metric">
                <div class="metric-label">Всего заказов</div>
                <div class="metric-value">${formatNumber(customer.totalOrders || 0)}</div>
            </div>
            <div class="drawer-metric">
                <div class="metric-label">Общие траты</div>
                <div class="metric-value">${formatCurrency(customer.totalSpent || 0)}</div>
            </div>
            <div class="drawer-metric">
                <div class="metric-label">Средний чек</div>
                <div class="metric-value">${formatCurrency(customer.avgOrderValue || 0)}</div>
            </div>
            <div class="drawer-metric">
                <div class="metric-label">CLV</div>
                <div class="metric-value">${formatCurrency(customer.clv || 0)}</div>
            </div>
            <div class="drawer-metric">
                <div class="metric-label">Сегмент</div>
                <div class="metric-value"><span class="badge" style="background: ${getSegmentColor(customer.segment)}20; color: ${getSegmentColor(customer.segment)};">${customer.segment || 'BASIC'}</span></div>
            </div>
            <div class="drawer-metric">
                <div class="metric-label">RFM</div>
                <div class="metric-value"><span class="badge rfm-badge ${(customer.rfmSegment || 'na').toLowerCase().replace(' ', '-')}">${customer.rfmSegment || 'N/A'}</span></div>
            </div>
            <div class="drawer-metric">
                <div class="metric-label">Дней с заказа</div>
                <div class="metric-value">${customer.daysSinceLastOrder !== null ? customer.daysSinceLastOrder : '—'}</div>
        `;
    }

    const ordersContainer = document.getElementById('drawerOrders');
    if (ordersContainer) {
        ordersContainer.innerHTML = `
            <div class="alert alert-light border text-center">
                <i class="bi bi-info-circle"></i> История заказов загружается отдельно
            </div>
        `;
    }

    const recsContainer = document.getElementById('drawerRecommendations');
    if (recsContainer) {
        const recs = getCustomerRecommendations(customer);
        recsContainer.innerHTML = recs.length > 0 ? recs.map(r => `
            <div class="recommendation-item">
                <i class="bi ${r.icon}"></i>
                <span>${r.text}</span>
            </div>
        `).join('') : '<div class="text-muted small">Рекомендации не найдены</div>';
    }

    if (drawer) drawer.classList.add('open');
    if (overlay) overlay.classList.add('show');
    document.body.style.overflow = 'hidden';
}

function closeCustomerDrawer() {
    const drawer = document.getElementById('customerDrawer');
    const overlay = document.getElementById('customerDrawerOverlay');

    if (drawer) drawer.classList.remove('open');
    if (overlay) overlay.classList.remove('show');
    document.body.style.overflow = '';
}

// ========== РЕКОМЕНДАЦИИ КЛИЕНТА ==========

function getCustomerRecommendations(customer) {
    const recs = [];

    if (customer.segment === 'VIP') {
        recs.push({ icon: 'bi-gem', text: 'Предложить эксклюзивную коллекцию' });
        recs.push({ icon: 'bi-gift', text: 'Персональная скидка 25%' });
    } else if (customer.segment === 'LOST') {
        recs.push({ icon: 'bi-envelope', text: 'Отправить реактивационное письмо' });
        recs.push({ icon: 'bi-tag', text: 'Предложить скидку 30% на возврат' });
    } else if (customer.segment === 'BASIC') {
        recs.push({ icon: 'bi-hand-thumbs-up', text: 'Приветственная скидка 10%' });
    }

    if (customer.daysSinceLastOrder > 90) {
        recs.push({ icon: 'bi-bell', text: 'Давно не заказывал — напомнить о себе' });
    }

    return recs;
}

// ========== RFM АНАЛИЗ ==========

function renderRfmAnalysisFromStats(rfmData, totalCustomersFromStats) {
    const container = document.getElementById('rfmAnalysisResults');
    const recommendations = document.getElementById('rfmRecommendations');

    if (!rfmData || rfmData.length === 0) {
        if (container) container.innerHTML = '<div class="alert alert-info">Нет данных для RFM анализа</div>';
        return;
    }

    let totalCustomers = totalCustomersFromStats;
    if (!totalCustomers || totalCustomers === 0) {
        totalCustomers = rfmData.reduce((sum, rfm) => sum + (rfm.customerCount || 0), 0);
    }

    const segmentColors = {
        'Champions': '#28a745',
        'Loyal': '#17a2b8',
        'Potential': '#ffc107',
        'At Risk': '#fd7e14',
        'Dormant': '#6c757d',
        'Lost': '#dc3545'
    };

    const segmentDescriptions = {
        'Champions': 'Недавние, частые, большие покупки',
        'Loyal': 'Регулярные покупатели со средней частотой',
        'Potential': 'Имеют потенциал для роста',
        'At Risk': 'Раньше покупали часто, сейчас рискуют уйти',
        'Dormant': 'Были активны 90-180 дней назад',
        'Lost': 'Не покупали более 180 дней'
    };

    // Обновляем матрицу
    const matrixData = {
        'Champions': { count: 0, revenue: 0 },
        'Loyal': { count: 0, revenue: 0 },
        'Potential': { count: 0, revenue: 0 },
        'At Risk': { count: 0, revenue: 0 },
        'Dormant': { count: 0, revenue: 0 },
        'Lost': { count: 0, revenue: 0 }
    };

    rfmData.forEach(rfm => {
        if (matrixData[rfm.segment]) {
            matrixData[rfm.segment] = {
                count: rfm.customerCount || 0,
                revenue: rfm.totalRevenue || 0
            };
        }
    });

    Object.keys(matrixData).forEach(segment => {
        const data = matrixData[segment];
        const percent = totalCustomers > 0 ? ((data.count / totalCustomers) * 100).toFixed(1) : '0.0';

        const countEl = document.getElementById(`rfm-${segment.toLowerCase().replace(' ', '')}-count`);
        const revenueEl = document.getElementById(`rfm-${segment.toLowerCase().replace(' ', '')}-revenue`);

        if (countEl) countEl.textContent = `${data.count} чел. (${percent}%)`;
        if (revenueEl) revenueEl.textContent = formatCurrency(data.revenue);
    });

    if (container) {
        container.innerHTML = rfmData.map(rfm => {
            const color = segmentColors[rfm.segment] || '#6B1F3F';
            const desc = segmentDescriptions[rfm.segment] || rfm.description || '';

            const customerCount = rfm.customerCount || 0;
            const percent = totalCustomers > 0 ? ((customerCount / totalCustomers) * 100).toFixed(1) : '0.0';

            return `
                <div class="rfm-detail-card" style="border-left-color: ${color};">
                    <div class="rfm-detail-header">
                        <div class="d-flex align-items-center gap-2">
                            <span class="badge" style="background: ${color}; font-size: 14px;">
                                ${rfm.segment}
                            </span>
                            <span class="text-muted small">${desc}</span>
                        </div>
                        <span class="badge bg-light text-dark border">${customerCount} чел. (${percent}%)</span>
                    </div>
                    <div class="rfm-detail-stats">
                        <div class="rfm-stat-box">
                            <div class="rfm-stat-value">${formatCurrency(rfm.totalRevenue || 0)}</div>
                            <div class="rfm-stat-label">Выручка</div>
                        </div>
                        <div class="rfm-stat-box">
                            <div class="rfm-stat-value">${formatCurrency(rfm.avgRevenue || 0)}</div>
                            <div class="rfm-stat-label">Ср. на клиента</div>
                        </div>
                        <div class="rfm-stat-box">
                            <div class="rfm-stat-value">${percent}%</div>
                            <div class="rfm-stat-label">Доля базы</div>
                        </div>
                    </div>
                    <div class="rfm-recommendation">
                        <i class="bi bi-lightbulb text-warning"></i>
                        <strong>Рекомендация:</strong> ${rfm.recommendedAction || 'Стандартные активности'}
                    </div>
                    <div class="rfm-actions mt-3">
                        <button class="btn btn-sm btn-primary" onclick="showRfmSegmentCustomers('${rfm.segment}')">
                            <i class="bi bi-people"></i> Показать клиентов
                        </button>
                        <button class="btn btn-sm btn-success" onclick="createCampaignForSegment('${rfm.segment}')">
                            <i class="bi bi-megaphone"></i> Создать кампанию
                        </button>
                    </div>
                </div>
            `;
        }).join('');
    }

    if (recommendations) {
        recommendations.innerHTML = `
            <div class="alert alert-info">
                <h6><i class="bi bi-lightbulb"></i> Стратегия работы с RFM-сегментами</h6>
                <div class="row mt-2">
                    <div class="col-md-4">
                        <strong>Champions & Loyal:</strong> VIP-программы, ранний доступ к новинкам
                    </div>
                    <div class="col-md-4">
                        <strong>Potential:</strong> Стимулировать увеличение частоты покупок
                    </div>
                    <div class="col-md-4">
                        <strong>At Risk & Lost:</strong> Реактивационные кампании со скидками
                    </div>
                </div>
            </div>
        `;
    }
}

// ========== ABC АНАЛИЗ ==========

function renderAbcAnalysisFromStats(abcData) {
    const container = document.getElementById('abcAnalysisResults');
    const summary = document.getElementById('abcSummary');

    if (!abcData || abcData.length === 0) {
        if (container) container.innerHTML = '<div class="alert alert-info">Нет данных для ABC анализа</div>';
        return;
    }

    const groupColors = {
        'Группа A': '#28a745',
        'Группа B': '#ffc107',
        'Группа C': '#6c757d'
    };

    abcData.forEach(abc => {
        const groupKey = abc.segment?.replace('Группа ', '');
        const countEl = document.getElementById(`abc${groupKey}Count`);
        if (countEl) {
            countEl.textContent = `${abc.customerCount || 0} клиентов (${(abc.revenuePercent || 0).toFixed(1)}%)`;
        }
    });

    if (summary) {
        const totalRevenue = abcData.reduce((sum, a) => sum + (a.revenue || 0), 0);
        summary.innerHTML = `
            <div class="abc-summary-item">
                <div class="summary-label">Общая выручка</div>
                <div class="summary-value">${formatCurrency(totalRevenue)}</div>
            </div>
        `;
    }

    if (container) {
        container.innerHTML = abcData.map(abc => {
            const color = groupColors[abc.segment] || '#6B1F3F';
            return `
                <div class="abc-detail-card" style="border-left-color: ${color};">
                    <div class="d-flex justify-content-between align-items-start">
                        <div>
                            <h5>
                                <span class="badge" style="background: ${color};">
                                    ${abc.segment}
                                </span>
                                <span class="text-muted ms-2">${abc.customerCount || 0} клиентов</span>
                            </h5>
                            <p class="mb-1 small">${abc.description || ''}</p>
                        </div>
                        <div class="text-end">
                            <div class="h4 mb-0">${formatCurrency(abc.revenue || 0)}</div>
                            <div class="text-muted small">${(abc.revenuePercent || 0).toFixed(1)}% выручки</div>
                        </div>
                    </div>
                    <div class="abc-progress mt-3">
                        <div class="progress" style="height: 8px;">
                            <div class="progress-bar" style="width: ${abc.revenuePercent || 0}%; background: ${color};"></div>
                        </div>
                        <div class="progress-labels mt-1">
                            <span class="text-muted small">Накоплено: ${(abc.cumulativePercent || 0).toFixed(1)}%</span>
                        </div>
                    </div>
                    <div class="mt-3">
                        <button class="btn btn-sm btn-primary" onclick="showAbcGroupCustomers('${abc.segment}')">
                            <i class="bi bi-people"></i> Показать клиентов
                        </button>
                    </div>
                </div>
            `;
        }).join('');
    }

    renderParetoChart(abcData);
}

function renderParetoChart(abcData) {
    const ctx = document.getElementById('paretoChart');
    if (!ctx) return;

    if (window.paretoChartInstance) {
        window.paretoChartInstance.destroy();
    }

    const labels = abcData.map(a => a.segment);
    const revenues = abcData.map(a => a.revenue || 0);
    const cumulative = abcData.map(a => a.cumulativePercent || 0);

    window.paretoChartInstance = new Chart(ctx, {
        type: 'bar',
        data: {
            labels: labels,
            datasets: [{
                label: 'Выручка',
                data: revenues,
                backgroundColor: ['#28a745', '#ffc107', '#6c757d'],
                order: 2,
                yAxisID: 'y'
            }, {
                label: 'Накопленный %',
                data: cumulative,
                type: 'line',
                borderColor: '#4f46e5',
                borderWidth: 2,
                pointRadius: 4,
                pointBackgroundColor: '#4f46e5',
                order: 1,
                yAxisID: 'y1'
            }]
        },
        options: {
            responsive: true,
            interaction: { mode: 'index', intersect: false },
            plugins: {
                legend: { position: 'top' },
                tooltip: {
                    callbacks: {
                        label: function(context) {
                            if (context.dataset.type === 'line') {
                                return `Накоплено: ${context.parsed.y.toFixed(1)}%`;
                            }
                            return `Выручка: ${formatCurrency(context.parsed.y)}`;
                        }
                    }
                }
            },
            scales: {
                y: {
                    beginAtZero: true,
                    position: 'left',
                    title: { display: true, text: 'Выручка (BYN)' },
                    ticks: { callback: v => formatCurrency(v) }
                },
                y1: {
                    beginAtZero: true,
                    max: 100,
                    position: 'right',
                    title: { display: true, text: '%' },
                    ticks: { callback: v => v + '%', stepSize: 20 },
                    grid: { drawOnChartArea: false }
                }
            }
        }
    });
}

// ========== ВОРОНКА ПРОДАЖ ==========

async function loadFunnel() {
    try {
        const res = await apiFetch('/api/crm/funnel');
        if (!res || !res.ok) {
            console.error('Failed to load funnel data');
            return;
        }

        const funnel = await res.json();
        
        console.log('Funnel data received:', funnel);
        
        // Обновляем этапы воронки (числовые значения)
        updateElement('funnel-visits-display', formatNumber(funnel.visits || 0));
        updateElement('funnel-carts-display', formatNumber(funnel.carts || 0));
        updateElement('funnel-orders-display', formatNumber(funnel.orders || 0));
        updateElement('funnel-paid-display', formatNumber(funnel.paid || 0));
        
        // Обновляем проценты конверсии для каждого этапа
        updateElement('funnel-cart-percent-display', (funnel.visitToCartPercent || 0).toFixed(1) + '%');
        updateElement('funnel-order-percent-display', (funnel.cartToOrderPercent || 0).toFixed(1) + '%');
        updateElement('funnel-paid-percent-display', (funnel.orderToPayPercent || 0).toFixed(1) + '%');
        
        // Обновляем потери на этапах
        const visitToCartLoss = 100 - (funnel.visitToCartPercent || 0);
        const cartToOrderLoss = 100 - (funnel.cartToOrderPercent || 0);
        const orderToPayLoss = 100 - (funnel.orderToPayPercent || 0);
        
        updateElement('funnel-cart-loss-detail', visitToCartLoss.toFixed(1) + '%');
        updateElement('funnel-order-loss-detail', cartToOrderLoss.toFixed(1) + '%');
        updateElement('funnel-paid-loss-detail', orderToPayLoss.toFixed(1) + '%');
        
        // Обновляем ключевые метрики (ПРАВИЛЬНЫЕ ID)
        updateElement('funnelTotalConversion', (funnel.totalConversion || 0).toFixed(2) + '%');
        updateElement('funnelCartConversion', (funnel.cartToOrderPercent || 0).toFixed(1) + '%');
        updateElement('funnelAvgCheck', formatCurrency(funnel.avgOrderValue || 0));
        
        // Потери в корзине (общие)
        const cartLoss = funnel.carts > 0 ? 
            ((funnel.carts - funnel.orders) / funnel.carts * 100) : 0;
        updateElement('funnelCartLoss', cartLoss.toFixed(1) + '%');
        
        // Визуализируем воронку
        renderFunnelVisualization(funnel);
        
    } catch (error) {
        console.error('Ошибка загрузки воронки:', error);
        showToast('Ошибка загрузки данных воронки', true);
    }
}

function renderFunnelVisualization(funnel) {
    const container = document.getElementById('funnelVisualization');
    if (!container) return;
    
    const maxValue = Math.max(funnel.visits, funnel.carts, funnel.orders, funnel.paid);
    const getWidthPercent = (value) => maxValue > 0 ? Math.max(35, (value / maxValue) * 100) : 100;
    
    const stages = [
        { 
            name: 'Посетители', 
            value: funnel.visits, 
            conversion: null,
            color: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
            icon: 'bi-eye',
            width: 100
        },
        { 
            name: 'Корзина', 
            value: funnel.carts, 
            conversion: funnel.visitToCartPercent,
            color: 'linear-gradient(135deg, #f093fb 0%, #f5576c 100%)',
            icon: 'bi-cart',
            width: getWidthPercent(funnel.carts)
        },
        { 
            name: 'Заказ', 
            value: funnel.orders, 
            conversion: funnel.cartToOrderPercent,
            color: 'linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)',
            icon: 'bi-receipt',
            width: getWidthPercent(funnel.orders)
        },
        { 
            name: 'Оплата', 
            value: funnel.paid, 
            conversion: funnel.orderToPayPercent,
            color: 'linear-gradient(135deg, #43e97b 0%, #38f9d7 100%)',
            icon: 'bi-credit-card',
            width: getWidthPercent(funnel.paid)
        }
    ];
    
    let html = '<div class="funnel-professional">';
    
    for (let i = 0; i < stages.length; i++) {
        const stage = stages[i];
        const widthClass = i === 0 ? 'funnel-stage-wide' : 
                          (i === 1 ? 'funnel-stage-medium' : 
                          (i === 2 ? 'funnel-stage-small' : 'funnel-stage-final'));
        
        html += `
            <div class="funnel-stage-wrapper" style="animation-delay: ${i * 0.1}s">
                <div class="${widthClass}" style="background: ${stage.color}; width: ${stage.width}%; margin: 0 auto;">
                    <div class="funnel-content">
                        <div class="funnel-icon">
                            <i class="bi ${stage.icon}"></i>
                        </div>
                        <div class="funnel-info">
                            <div class="funnel-label">${stage.name}</div>
                            <div class="funnel-value">${formatNumber(stage.value)}</div>
                        </div>
                        ${stage.conversion !== null ? `
                        <div class="funnel-percent">
                            ${stage.conversion.toFixed(1)}%
                        </div>
                        ` : ''}
                    </div>
                </div>`;
        
        // Добавляем соединитель между этапами (кроме последнего)
        if (i < stages.length - 1) {
            const nextStage = stages[i + 1];
            const conversion = nextStage.conversion || 0;
            
            html += `
                <div class="funnel-connector">
                    <div class="funnel-arrow-down"></div>
                    <div class="funnel-conversion">
                        ${conversion.toFixed(1)}%
                    </div>
                </div>`;
        }
        
        html += `</div>`;
    }
    
    html += '</div>';
    container.innerHTML = html;
}

function showCartRecovery() {
    showToast('Кампания восстановления корзин запущена!');
}

// ========== КАРТА АКТИВНОСТИ ==========

let monthlyRevenueChart = null;

async function loadActivityStats() {
    const period = document.getElementById('activityPeriodFilter')?.value || 'month';
    
    try {
        const res = await apiFetch(`/api/crm/activity?period=${period}`);
        if (!res || !res.ok) return;
        
        const stats = await res.json();
        console.log('Activity stats received:', stats);
        
        // Обновляем карточки
        updateElement('avgDailyOrders', stats.avgDailyOrders.toFixed(1));
        updateElement('mostActiveDay', formatDate(stats.mostActiveDay));
        updateElement('mostActiveDayOrders', stats.mostActiveDayOrders + ' заказов');
        updateElement('inactiveDays', stats.inactiveDays);
        updateElement('totalOrdersCount', formatNumber(stats.totalOrders));
        
        // Рендерим тепловую карту
        renderActivityHeatmap(stats.dailyOrders);
        
        // Рендерим график выручки по месяцам
        renderMonthlyRevenueChart(stats.monthlyRevenue);
        
    } catch (error) {
        console.error('Ошибка загрузки активности:', error);
        showToast('Ошибка загрузки данных активности', true);
    }
}

function renderActivityHeatmap(dailyOrders) {
    const container = document.getElementById('activityHeatmap');
    if (!container) return;
    
    if (!dailyOrders || Object.keys(dailyOrders).length === 0) {
        container.innerHTML = '<div class="alert alert-info">Нет данных об активности</div>';
        return;
    }
    
    // Получаем все даты и сортируем
    const dates = Object.keys(dailyOrders).sort();
    if (dates.length === 0) return;
    
    // Находим первую и последнюю дату
    const firstDate = new Date(dates[0]);
    const lastDate = new Date(dates[dates.length - 1]);
    
    // Создаем структуру данных для тепловой карты по дням недели
    const weeksData = [];
    let currentWeek = [];
    let currentDate = new Date(firstDate);
    
    // Сдвигаем к понедельнику
    while (currentDate.getDay() !== 1 && currentDate > firstDate) {
        currentDate.setDate(currentDate.getDate() - 1);
    }
    
    // Заполняем недели
    while (currentDate <= lastDate) {
        const dateStr = currentDate.toISOString().split('T')[0];
        const orders = dailyOrders[dateStr] || 0;
        currentWeek.push(orders);
        
        if (currentDate.getDay() === 0 || currentDate.toDateString() === lastDate.toDateString()) {
            // Дополняем неделю до 7 дней
            while (currentWeek.length < 7) {
                currentWeek.push(0);
            }
            weeksData.push([...currentWeek]);
            currentWeek = [];
        }
        currentDate.setDate(currentDate.getDate() + 1);
    }
    
    // Находим максимальное значение
    const allValues = Object.values(dailyOrders);
    const maxValue = Math.max(...allValues, 1);
    
    // Создаем HTML тепловой карты
    let html = '<div class="activity-heatmap-wrapper">';
    
    // Месяцы
    html += '<div class="heatmap-months">';
    let lastMonth = '';
    for (let i = 0; i < weeksData.length; i++) {
        const weekStart = new Date(firstDate);
        weekStart.setDate(firstDate.getDate() + (i * 7));
        const monthName = weekStart.toLocaleString('ru-RU', { month: 'short' });
        if (monthName !== lastMonth) {
            lastMonth = monthName;
            html += `<div class="heatmap-month-label" style="width: 32px;">${monthName}</div>`;
        } else {
            html += `<div class="heatmap-month-label" style="width: 32px;"></div>`;
        }
    }
    html += '</div>';
    
    // Дни недели
    const weekdays = ['Пн', 'Вт', 'Ср', 'Чт', 'Пт', 'Сб', 'Вс'];
    html += '<div class="heatmap-grid"><div class="heatmap-weekdays">';
    for (let i = 0; i < 7; i++) {
        html += `<div class="weekday-label">${weekdays[i]}</div>`;
    }
    html += '</div><div class="heatmap-cells">';
    
    // Ячейки тепловой карты
    for (let row = 0; row < 7; row++) {
        html += '<div class="heatmap-row">';
        for (let col = 0; col < weeksData.length; col++) {
            const value = weeksData[col]?.[row] || 0;
            const intensity = calculateIntensity(value, maxValue);
            // Вычисляем дату для тултипа
            const date = new Date(firstDate);
            date.setDate(firstDate.getDate() + (col * 7) + row);
            const formattedDate = date.toLocaleDateString('ru-RU');
            
            html += `<div class="heatmap-cell intensity-${intensity}" 
                           title="${formattedDate}: ${value} заказов">
                        ${value > 0 ? value : ''}
                    </div>`;
        }
        html += '</div>';
    }
    html += '</div></div></div>';
    
    container.innerHTML = html;
}

function renderMonthlyRevenueChart(monthlyRevenue) {
    const ctx = document.getElementById('monthlyRevenueChart');
    if (!ctx) return;
    
    if (monthlyRevenueChart) {
        monthlyRevenueChart.destroy();
    }
    
    const months = Object.keys(monthlyRevenue || {});
    const revenues = months.map(m => monthlyRevenue[m] || 0);
    
    if (months.length === 0) {
        ctx.parentElement.innerHTML = '<div class="text-center text-muted p-4">Нет данных за период</div>';
        return;
    }
    
    monthlyRevenueChart = new Chart(ctx, {
        type: 'line',
        data: {
            labels: months.map(m => {
                const [year, month] = m.split('-');
                return `${month}.${year}`;
            }),
            datasets: [{
                label: 'Выручка (BYN)',
                data: revenues,
                borderColor: '#6B1F3F',
                backgroundColor: 'rgba(107, 31, 63, 0.1)',
                fill: true,
                tension: 0.4,
                pointRadius: 5,
                pointBackgroundColor: '#6B1F3F',
                pointBorderColor: '#fff',
                pointBorderWidth: 2,
                pointHoverRadius: 7
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: true,
            plugins: {
                legend: { display: false },
                tooltip: {
                    callbacks: {
                        label: function(context) {
                            return `Выручка: ${formatCurrency(context.raw)}`;
                        }
                    }
                }
            },
            scales: {
                y: {
                    beginAtZero: true,
                    title: { display: true, text: 'Выручка (BYN)', font: { size: 11 } },
                    ticks: { callback: v => formatCurrency(v), font: { size: 10 } }
                },
                x: {
                    ticks: { font: { size: 10 } }
                }
            }
        }
    });
}

function formatDate(dateStr) {
    if (!dateStr) return '—';
    const date = new Date(dateStr);
    if (isNaN(date.getTime())) return dateStr;
    return date.toLocaleDateString('ru-RU', { day: 'numeric', month: 'long' });
}

// Обновляем функцию calculateIntensity
function calculateIntensity(value, maxValue) {
    if (value === 0) return 0;
    const ratio = value / maxValue;
    if (ratio < 0.25) return 1;
    if (ratio < 0.5) return 2;
    if (ratio < 0.75) return 3;
    return 4;
}

// Обновляем initTabs для загрузки активности при открытии вкладки
function initTabs() {
    document.querySelectorAll('[data-bs-toggle="tab"]').forEach(tab => {
        tab.addEventListener('shown.bs.tab', (e) => {
            const target = e.target.getAttribute('data-bs-target');
            if (target === '#funnel-tab') {
                loadFunnel();
            } else if (target === '#activity-tab') {
                loadActivityStats();
            }
        });
    });
}

// ========== МОДАЛЬНЫЕ ОКНА ==========

function openSegmentModal(segmentId = null) {
    const modal = document.getElementById('segmentModal');
    const title = document.getElementById('segmentModalTitle');
    const form = document.getElementById('segmentForm');

    if (!modal || !form) return;

    form.reset();
    const idInput = document.getElementById('segmentId');
    if (idInput) idInput.value = '';

    if (segmentId) {
        if (title) title.textContent = 'Редактировать сегмент';
        const segment = allSegments.find(s => s.id === segmentId);
        if (segment) {
            if (idInput) idInput.value = segment.id;
            setInputValue('segmentName', segment.name);
            setInputValue('segmentDescription', segment.description);
            setInputValue('segmentColor', segment.color || '#6B1F3F');
            setInputValue('segmentIcon', segment.icon || 'bi-folder');
        }
    } else {
        if (title) title.textContent = 'Создать сегмент';
    }

    modal.style.display = 'flex';
}

function closeSegmentModal() {
    const modal = document.getElementById('segmentModal');
    if (modal) modal.style.display = 'none';
}

function setInputValue(id, value) {
    const el = document.getElementById(id);
    if (el) el.value = value || '';
}

function editSegment(segmentId) {
    openSegmentModal(segmentId);
}

async function deleteSegment(segmentId) {
    if (!confirm('Удалить этот сегмент?')) return;
    try {
        const res = await apiFetch(`/api/crm/segments/${segmentId}`, { method: 'DELETE' });
        if (res && res.ok) {
            showToast('Сегмент удалён');
            await loadSegments();
        } else {
            const error = await res?.json();
            showToast(error?.error || 'Ошибка удаления', true);
        }
    } catch (error) {
        showToast('Ошибка удаления сегмента', true);
    }
}

async function saveSegment() {
    const segmentId = document.getElementById('segmentId')?.value;
    const data = {
        name: document.getElementById('segmentName')?.value,
        description: document.getElementById('segmentDescription')?.value,
        color: document.getElementById('segmentColor')?.value,
        icon: document.getElementById('segmentIcon')?.value
    };

    try {
        const url = segmentId ? `/api/crm/segments/${segmentId}` : '/api/crm/segments';
        const method = segmentId ? 'PUT' : 'POST';
        const res = await apiFetch(url, { method, body: JSON.stringify(data) });
        if (res && res.ok) {
            showToast(segmentId ? 'Сегмент обновлён' : 'Сегмент создан');
            closeSegmentModal();
            await loadSegments();
        } else {
            const error = await res?.json();
            showToast(error?.error || 'Ошибка сохранения', true);
        }
    } catch (error) {
        showToast('Ошибка сохранения сегмента', true);
    }
}

// ========== BULK EMAIL MODAL ==========

function openBulkEmailModal(customerIds) {
    const modal = document.getElementById('bulkEmailModal');
    const countEl = document.getElementById('bulkEmailCount');
    if (countEl) countEl.textContent = customerIds?.length || 0;
    if (modal) modal.style.display = 'flex';
}

function closeBulkEmailModal() {
    const modal = document.getElementById('bulkEmailModal');
    if (modal) modal.style.display = 'none';
}

function loadTemplate() {
    const template = document.getElementById('bulkEmailTemplate')?.value;
    const body = document.getElementById('bulkEmailBody');
    if (!body) return;

    const templates = {
        welcome: 'Добро пожаловать в Design Studio! Мы рады видеть вас среди наших клиентов.',
        discount: 'Специально для вас — персональная скидка {{discount}}% на весь ассортимент!',
        reactivation: 'Мы скучаем по вам! Вернитесь с промокодом {{discount}} и получите подарок.',
        birthday: 'С днём рождения! Пусть ваш дом станет ещё уютнее с нашей скидкой {{discount}}%'
    };

    if (templates[template]) body.value = templates[template];
}

function sendBulkEmail() {
    const subject = document.getElementById('bulkEmailSubject')?.value;
    const body = document.getElementById('bulkEmailBody')?.value;

    if (!subject || !body) {
        showToast('Заполните тему и текст письма', true);
        return;
    }

    showToast('Рассылка запущена!');
    closeBulkEmailModal();
}

// ========== ДОПОЛНИТЕЛЬНЫЕ ФУНКЦИИ ==========

function showRfmSegmentCustomers(segment) {
    const filtered = allCustomers.filter(c => c.rfmSegment === segment);
    showFilteredCustomers(filtered, `RFM: ${segment}`);
}

function showAbcGroupCustomers(group) {
    let filtered = [...allCustomers].sort((a, b) => (b.totalSpent || 0) - (a.totalSpent || 0));

    const total = filtered.length;
    let startIdx, endIdx;

    if (group === 'Группа A') {
        startIdx = 0;
        endIdx = Math.ceil(total * 0.2);
    } else if (group === 'Группа B') {
        startIdx = Math.ceil(total * 0.2);
        endIdx = Math.ceil(total * 0.5);
    } else {
        startIdx = Math.ceil(total * 0.5);
        endIdx = total;
    }

    filtered = filtered.slice(startIdx, endIdx);
    showFilteredCustomers(filtered, group);
}

function showCustomSegmentCustomers(segmentId, segmentName) {
    const segment = allSegments.find(s => s.id === segmentId);
    if (!segment) return;

    let filtered = allCustomers;

    if (segment.minOrders !== undefined) {
        filtered = filtered.filter(c => (c.totalOrders || 0) >= segment.minOrders);
    }
    if (segment.maxOrders !== undefined) {
        filtered = filtered.filter(c => (c.totalOrders || 0) <= segment.maxOrders);
    }
    if (segment.minSpent !== undefined) {
        filtered = filtered.filter(c => (c.totalSpent || 0) >= segment.minSpent);
    }
    if (segment.maxSpent !== undefined) {
        filtered = filtered.filter(c => (c.totalSpent || 0) <= segment.maxSpent);
    }

    showFilteredCustomers(filtered, segmentName);
}

function showFilteredCustomers(filtered, label) {
    const tbody = document.getElementById('customersTableBody');
    if (!tbody) return;

    updateElement('customersShown', filtered.length);
    updateElement('customersTotal', filtered.length);
    updateElement('customersTotalInfo', `(${label} — ${filtered.length} чел.)`);

    if (filtered.length === 0) {
        tbody.innerHTML = `
            <tr>
                <td colspan="11" class="text-center py-5">
                    <div class="empty-state">
                        <i class="bi bi-people"></i>
                        <p>Нет клиентов в сегменте "${label}"</p>
                        <button class="btn btn-sm btn-outline-secondary" onclick="clearFilters()">Вернуться к списку</button>
                    </div>
                </td>
            </tr>`;
        document.getElementById('customersPagination').innerHTML = '';
        return;
    }

    tbody.innerHTML = filtered.map(customer => {
        const fullName = `${customer.firstName || ''} ${customer.lastName || ''}`.trim() || '—';
        const initials = `${(customer.firstName || '')[0] || ''}${(customer.lastName || '')[0] || ''}`.toUpperCase();
        const segmentColor = getSegmentColor(customer.segment);
        const rfmBadgeClass = (customer.rfmSegment || 'na').toLowerCase().replace(' ', '-');

        return `
            <tr onclick="showCustomerDetail(${customer.id})" style="cursor:pointer;">
                <td><input type="checkbox" class="form-check-input customer-checkbox" value="${customer.id}" onclick="event.stopPropagation();"></td>
                <td>
                    <div class="d-flex align-items-center gap-2">
                        <div class="customer-avatar-sm" style="background: ${segmentColor}20; color: ${segmentColor};">
                            ${initials}
                        </div>
                        <div class="fw-medium">${fullName}</div>
                    </div>
                </div>
                </td>
                <td>${customer.email || '—'}</td>
                <td><span class="badge" style="background: ${segmentColor}20; color: ${segmentColor};">${customer.segment || 'BASIC'}</span></td>
                <td>${formatNumber(customer.totalOrders || 0)}</td>
                <td>${formatCurrency(customer.totalSpent || 0)}</td>
                <td>${formatCurrency(customer.avgOrderValue || 0)}</td>
                <td><span class="badge rfm-badge ${rfmBadgeClass}">${customer.rfmSegment || 'N/A'}</span></td>
                <td>${formatCurrency(customer.clv || 0)}</td>
                <td>${customer.daysSinceLastOrder !== null ? customer.daysSinceLastOrder + ' дн.' : '—'}</td>
                <td>
                    <button class="btn btn-sm btn-primary" onclick="event.stopPropagation(); showCustomerDetail(${customer.id})">
                        <i class="bi bi-eye"></i>
                    </button>
                </td>
            </tr>
        `;
    }).join('');

    document.getElementById('customersPagination').innerHTML = '';

    const tab = document.querySelector('[data-bs-target="#customers-tab"]');
    if (tab) {
        const bsTab = new bootstrap.Tab(tab);
        bsTab.show();
    }
}

function showSegmentCustomers(segment) {
    filterCustomersBySegment(segment);
}

// ========== ЭКСПОРТ ==========

function exportCustomers(format) {
    const filtered = getFilteredCustomers();
    if (format === 'excel') exportToExcel(filtered);
    else if (format === 'pdf') window.print();
}

function getFilteredCustomers() {
    let filtered = allCustomers;

    if (currentSegmentFilter) {
        filtered = filtered.filter(c => c.segment === currentSegmentFilter);
    }

    if (currentSpentFilter) {
        const parts = currentSpentFilter.split('-');
        const min = parseFloat(parts[0]) || 0;
        const max = parts[1] ? parseFloat(parts[1].replace('+', '')) : Infinity;

        filtered = filtered.filter(c => {
            const spent = c.totalSpent || 0;
            return spent >= min && (max === Infinity || spent <= max);
        });
    }

    if (currentSearchQuery) {
        const query = currentSearchQuery.toLowerCase();
        filtered = filtered.filter(c =>
            ((c.firstName || '') + ' ' + (c.lastName || '')).toLowerCase().includes(query) ||
            (c.email || '').toLowerCase().includes(query)
        );
    }

    return filtered;
}

function exportToExcel(data) {
    const headers = ['Имя', 'Email', 'Сегмент', 'Заказов', 'Траты', 'Средний чек', 'RFM', 'CLV'];
    const rows = data.map(c => [
        `${c.firstName || ''} ${c.lastName || ''}`.trim(),
        c.email || '',
        c.segment || '',
        c.totalOrders || 0,
        c.totalSpent || 0,
        c.avgOrderValue || 0,
        c.rfmSegment || '',
        c.clv || 0
    ]);

    const csv = [headers.join(';'), ...rows.map(r => r.join(';'))].join('\n');
    downloadFile('\ufeff' + csv, 'customers.csv', 'text/csv;charset=utf-8;');
}

function downloadFile(content, filename, type) {
    const blob = new Blob([content], { type });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = filename;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
}

function exportChart(chartId) {
    const canvas = document.getElementById(chartId);
    if (!canvas) return;

    const url = canvas.toDataURL('image/png');
    const a = document.createElement('a');
    a.href = url;
    a.download = `${chartId}_${new Date().toISOString().split('T')[0]}.png`;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
}

// ========== ИНИЦИАЛИЗАЦИЯ ФОРМ ==========

function initForms() {
        const segmentForm = document.getElementById('segmentForm');
    if (segmentForm) {
        segmentForm.addEventListener('submit', async (e) => {
            e.preventDefault();
            await saveSegment();
        });
    }
}

function initModals() {
    document.querySelectorAll('.btn-close, .modal-cancel').forEach(btn => {
        btn.addEventListener('click', function() {
            const modal = this.closest('.modal');
            if (modal) modal.style.display = 'none';
        });
    });

    document.querySelectorAll('.modal').forEach(modal => {
        modal.addEventListener('click', function(e) {
            if (e.target === modal) modal.style.display = 'none';
        });
    });

    document.addEventListener('keydown', function(e) {
        if (e.key === 'Escape') {
            document.querySelectorAll('.modal').forEach(m => m.style.display = 'none');
            closeCustomerDrawer();
        }
    });
}

// ========== ВСПОМОГАТЕЛЬНЫЕ ФУНКЦИИ ==========

/**
 * Форматирование валюты в BYN (Белорусские рубли)
 */
function formatCurrency(num) {
    if (num === null || num === undefined || isNaN(num)) return '0 BYN';
    return new Intl.NumberFormat('ru-RU', {
        style: 'currency',
        currency: 'BYN',
        minimumFractionDigits: 0,
        maximumFractionDigits: 0
    }).format(num);
}

function formatNumber(num) {
    if (num === null || num === undefined || isNaN(num)) return '0';
    return new Intl.NumberFormat('ru-RU').format(Math.floor(num));
}

function formatPercent(num) {
    if (num === null || num === undefined || isNaN(num)) return '0%';
    return num.toFixed(1) + '%';
}

// ========== ЦВЕТА СЕГМЕНТОВ ==========

function getSegmentColor(segment) {
    const colors = {
        'VIP': '#28a745',
        'PREMIUM': '#17a2b8',
        'ACTIVE': '#007bff',
        'BASIC': '#6c757d',
        'LOST': '#dc3545',
        'Champions': '#28a745',
        'Loyal': '#17a2b8',
        'Potential': '#ffc107',
        'At Risk': '#fd7e14',
        'Dormant': '#6c757d',
        'Lost': '#dc3545'
    };
    return colors[segment] || '#6B1F3F';
}

function showToast(message, isError = false) {
    const container = document.getElementById('toastContainer');
    if (!container) {
        alert(message);
        return;
    }

    while (container.children.length > 3) {
        container.removeChild(container.firstChild);
    }

    const toast = document.createElement('div');
    toast.className = `toast ${isError ? 'toast-error' : 'toast-success'}`;
    toast.innerHTML = `
        <i class="bi ${isError ? 'bi-x-circle-fill text-danger' : 'bi-check-circle-fill text-success'}"></i>
        <span>${message}</span>
    `;
    container.appendChild(toast);

    setTimeout(() => {
        toast.style.opacity = '0';
        toast.style.transform = 'translateX(100%)';
        setTimeout(() => toast.remove(), 300);
    }, 3000);
}

function handleLogout(event) {
    event.preventDefault();
    localStorage.removeItem('design_studio_token');
    localStorage.removeItem('design_studio_user');
    window.location.href = 'login.html';
}

function createCampaignForCustomer(customerId) {
    showToast('Создание кампании для клиента в разработке');
}

function createCampaignForSegment(segment) {
    showToast(`Кампания для сегмента ${segment}: перенаправление...`);
}

function showCustomerOrders(customerId) {
    showToast('История заказов: функция в разработке', true);
}

function editCustomerDiscount(customerId) {
    showToast('Редактирование скидки: функция в разработке', true);
}

// ========== ГЛОБАЛЬНЫЙ ЭКСПОРТ ==========

window.CrmModule = {
    loadCrmStats,
    loadCustomers,
    loadSegments,
    filterCustomers,
    filterCustomersBySegment,
    openSegmentModal,
    closeSegmentModal,
    editSegment,
    deleteSegment,
    showCustomerDetail,
    closeCustomerDrawer,
    createCampaignForCustomer,
    createCampaignForSegment,
    bulkAction,
    clearSelection,
    exportCustomers,
    exportChart,
    sendBulkEmail,
    loadFunnel,
    showCartRecovery,
    showToast,
    clearFilters,
    goToPage,
    saveSegment,
    handleLogout
};

// Глобальные функции для onclick атрибутов в HTML
window.filterCustomersBySegment = filterCustomersBySegment;
window.showCustomerDetail = showCustomerDetail;
window.closeCustomerDrawer = closeCustomerDrawer;
window.editSegment = editSegment;
window.deleteSegment = deleteSegment;
window.createCampaignForSegment = createCampaignForSegment;
window.showRfmSegmentCustomers = showRfmSegmentCustomers;
window.showAbcGroupCustomers = showAbcGroupCustomers;
window.showCustomSegmentCustomers = showCustomSegmentCustomers;
window.createCampaignForCustomer = createCampaignForCustomer;
window.exportCustomers = exportCustomers;
window.exportChart = exportChart;
window.clearFilters = clearFilters;
window.handleLogout = handleLogout;
window.showSegmentCustomers = showSegmentCustomers;
window.showRfmSegment = showRfmSegmentCustomers;

console.log('crm.js v2.6 fully loaded — SEGMENTS: VIP, PREMIUM, BASIC, ACTIVE, LOST');