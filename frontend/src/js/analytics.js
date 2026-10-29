// analytics.js - Исправленная версия
document.addEventListener('DOMContentLoaded', async () => {
    await loadAdminAnalytics();
    initForms();
});

const API_BASE_URL = 'http://localhost:8080';
let visitsChart = null;
let salesChart = null;

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

// Форматирование валюты (BYN)
function formatCurrency(num) {
    if (num === null || num === undefined) return '0 BYN';
    return new Intl.NumberFormat('ru-RU', {
        style: 'currency',
        currency: 'BYN',
        minimumFractionDigits: 0,
        maximumFractionDigits: 0
    }).format(num || 0);
}

// Частота покупок - изменяем описание в карточке
const frequencyCard = document.getElementById('frequencyCard');
if (frequencyCard) {
    const labelElement = frequencyCard.querySelector('.metric-label');
    if (labelElement) {
        labelElement.textContent = 'заказов на покупателя';
    }
}

// Форматирование числа
function formatNumber(num) {
    if (num === null || num === undefined) return '0';
    return new Intl.NumberFormat('ru-RU').format(num);
}

// Форматирование процентов
function formatPercent(num) {
    if (num === null || num === undefined) return '0%';
    return `${num.toFixed(1)}%`;
}

// Создание или обновление графика
function createOrUpdateChart(chartRef, canvasId, type, labels, data, label, borderColor, backgroundColor) {
    const ctx = document.getElementById(canvasId);
    if (!ctx) return null;
    
    const context = ctx.getContext('2d');

    if (chartRef) {
        chartRef.destroy();
    }

    // Для графика посещений всегда используем тип 'line'
    const actualType = canvasId === 'visitsChart' ? 'line' : type;
    
    return new Chart(context, {
        type: actualType,
        data: {
            labels,
            datasets: [{
                label,
                data,
                borderColor: borderColor,
                backgroundColor: actualType === 'line' ? 'transparent' : backgroundColor,
                fill: actualType === 'line',
                tension: 0.3,
                borderWidth: 2,
                pointRadius: 4,
                pointBackgroundColor: borderColor,
                pointBorderColor: '#fff',
                pointBorderWidth: 1,
                pointHoverRadius: 6,
                borderRadius: actualType === 'bar' ? 4 : 0,
                borderSkipped: false
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            interaction: {
                intersect: false,
                mode: 'index'
            },
            plugins: {
                legend: {
                    display: true,
                    position: 'top',
                    labels: {
                        boxWidth: 12,
                        padding: 15,
                        font: { size: 12 }
                    }
                },
                tooltip: {
                    backgroundColor: 'rgba(72, 72, 72, 0.9)',
                    titleFont: { size: 13 },
                    bodyFont: { size: 12 },
                    padding: 12,
                    cornerRadius: 8,
                    callbacks: {
                        label: function(context) {
                            let label = context.dataset.label || '';
                            if (label) label += ': ';
                            if (context.parsed.y !== null) {
                                if (canvasId === 'salesChart') {
                                    label += formatCurrency(context.parsed.y);
                                } else {
                                    label += formatNumber(context.parsed.y);
                                }
                            }
                            return label;
                        }
                    }
                }
            },
            scales: {
                y: {
                    beginAtZero: true,
                    grid: { color: 'rgba(0, 0, 0, 0.05)' },
                    ticks: { 
                        font: { size: 11 },
                        callback: function(value) {
                            if (canvasId === 'salesChart') {
                                return formatCurrency(value);
                            }
                            return formatNumber(value);
                        }
                    }
                },
                x: {
                    grid: { display: false },
                    ticks: {
                        font: { size: 11 },
                        maxRotation: 45,
                        minRotation: 0
                    }
                }
            }
        }
    });
}

// Загрузка аналитики администратора
async function loadAdminAnalytics() {
    try {
        const res = await apiFetch('/api/analytics/admin');
        if (!res.ok) {
            console.error('Ошибка загрузки аналитики');
            showErrorMessage('analyticsError', 'Ошибка загрузки аналитики');
            return;
        }

        const data = await res.json();

        // Обновляем карточки статистики
        updateStatCard('avgCheck', formatCurrency(data.globalAvgCheck || 0));
        updateStatCard('conversion', formatPercent(data.conversionRate || 0));
        
        const totalSales = Object.values(data.adminSalesByMonth || {}).reduce((sum, v) => sum + v, 0);
        updateStatCard('totalSales', formatCurrency(totalSales));
        
        const totalVisits = Object.values(data.visitsByMonth || {}).reduce((sum, v) => sum + v, 0);
        updateStatCard('totalVisitsValue', formatNumber(totalVisits));
        
        // Частота покупок - с двумя десятичными знаками
        const purchaseFreq = (data.purchaseFrequency || 0).toFixed(2);
        updateStatCard('purchaseFrequencyValue', purchaseFreq);

        // Обновляем описание в карточке частоты покупок
        const freqCard = document.getElementById('purchaseFrequencyValue');
        if (freqCard && parseFloat(purchaseFreq) === 0) {
            freqCard.textContent = '—';
        }

        // Среднее время на сайте
        updateStatCard('avgTimeValue', `${data.avgTimeOnSite || 0} мин`);
        
        // Обновляем список топ клиентов (ТОП 3) - уменьшенный стиль
        const topCustomersList = document.getElementById('topCustomersList');
        if (topCustomersList) {
            const customers = data.topCustomers || [];
            if (customers.length === 0) {
                topCustomersList.innerHTML = '<li class="text-muted" style="font-size: 0.8rem;">Нет данных</li>';
            } else {
                topCustomersList.innerHTML = customers.map((c, index) => `
                    <li class="top-customer-item" style="display: flex; justify-content: space-between; align-items: center; padding: 6px 8px; background: white; border-radius: 6px; margin-bottom: 6px; font-size: 0.8rem;">
                        <div style="display: flex; align-items: center; gap: 8px;">
                            <span style="width: 22px; height: 22px; background: #6B1F3F; color: white; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-weight: bold; font-size: 0.7rem;">${index + 1}</span>
                            <span style="font-weight: 500; font-size: 0.8rem;">${escapeHtml(c.userName || 'Неизвестный')}</span>
                        </div>
                        <div style="text-align: right;">
                            <div style="font-weight: 700; color: #28a745; font-size: 0.8rem;">${formatCurrency(c.totalSpent || 0)}</div>
                            <div style="font-size: 0.65rem; color: #6c757d;">${c.orderCount || 0} зак.</div>
                        </div>
                    </li>
                `).join('');
            }
        }

        // График посещений по месяцам
        const visitsByMonth = data.visitsByMonth || {};
        const visitsLabels = Object.keys(visitsByMonth).sort();
        const visitsValues = visitsLabels.map(key => visitsByMonth[key] || 0);
        
        visitsChart = createOrUpdateChart(
            visitsChart,
            'visitsChart',
            'line',
            visitsLabels,
            visitsValues,
            'Посещения',
            '#6B1F3F',
            'rgba(107, 31, 63, 0.1)'
        );

        // График продаж по месяцам
        const salesByMonth = data.adminSalesByMonth || {};
        const salesLabels = Object.keys(salesByMonth).sort();
        const salesValues = salesLabels.map(key => salesByMonth[key] || 0);
        
        salesChart = createOrUpdateChart(
            salesChart,
            'salesChart',
            'bar',
            salesLabels,
            salesValues,
            'Продажи (BYN)',
            '#28a745',
            'rgba(40, 167, 69, 0.4)'
        );
        
        // Обновляем период в заголовках
        updatePeriodDisplay();
        
    } catch (error) {
        console.error('Error loading analytics:', error);
        showErrorMessage('analyticsError', 'Ошибка загрузки аналитики');
    }
}

// Обновление карточки статистики
function updateStatCard(elementId, value) {
    const element = document.getElementById(elementId);
    if (element) {
        element.textContent = value;
    }
}

// Обновление отображения периода
function updatePeriodDisplay() {
    const currentPeriodSpan = document.getElementById('currentPeriod');
    if (currentPeriodSpan) {
        currentPeriodSpan.textContent = 'Общий период (за всё время)';
    }
}

// Показ сообщения об ошибке
function showErrorMessage(containerId, message) {
    const container = document.getElementById(containerId);
    if (container) {
        container.innerHTML = `
            <div class="alert alert-danger">
                <i class="bi bi-exclamation-triangle"></i> ${message}
            </div>
        `;
    }
}

// Экранирование HTML
function escapeHtml(text) {
    if (!text) return '';
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

// Экспорт в PDF
async function exportToPDF() {
    try {
        const res = await apiFetch('/api/analytics/admin');
        if (!res.ok) throw new Error('Ошибка загрузки данных');
        
        const data = await res.json();
        
        const printWindow = window.open('', '_blank');
        
        const totalSales = Object.values(data.adminSalesByMonth || {}).reduce((sum, v) => sum + v, 0);
        const totalVisits = Object.values(data.visitsByMonth || {}).reduce((sum, v) => sum + v, 0);
        
        printWindow.document.write(`
            <!DOCTYPE html>
            <html>
            <head>
                <title>Отчет по аналитике - Design Studio</title>
                <style>
                    body { font-family: Arial, sans-serif; padding: 40px; }
                    h1 { color: #6B1F3F; text-align: center; }
                    h2 { color: #333; margin-top: 30px; border-bottom: 2px solid #6B1F3F; padding-bottom: 10px; }
                    .metric-card { 
                        background: #f8f9fa;
                        border-radius: 8px;
                        padding: 15px;
                        margin-bottom: 15px;
                        text-align: center;
                    }
                    .metric-value { font-size: 24px; font-weight: bold; color: #6B1F3F; }
                    table { width: 100%; border-collapse: collapse; margin-top: 15px; }
                    th, td { border: 1px solid #ddd; padding: 10px; text-align: left; }
                    th { background: #6B1F3F; color: white; }
                    .footer { margin-top: 50px; text-align: center; color: #6c757d; font-size: 12px; }
                    .row { display: flex; gap: 20px; margin-bottom: 20px; }
                    .col { flex: 1; }
                </style>
            </head>
            <body>
                <h1>Отчет по аналитике</h1>
                <p style="text-align: center; color: #6c757d;">Дата формирования: ${new Date().toLocaleString('ru-RU')}</p>
                
                <h2>Основные показатели</h2>
                <div class="row">
                    <div class="col">
                        <div class="metric-card">
                            <div class="metric-label">Средний чек</div>
                            <div class="metric-value">${formatCurrency(data.globalAvgCheck || 0)}</div>
                        </div>
                    </div>
                    <div class="col">
                        <div class="metric-card">
                            <div class="metric-label">Конверсия</div>
                            <div class="metric-value">${formatPercent(data.conversionRate || 0)}</div>
                        </div>
                    </div>
                    <div class="col">
                        <div class="metric-card">
                            <div class="metric-label">Общий объём продаж</div>
                            <div class="metric-value">${formatCurrency(totalSales)}</div>
                        </div>
                    </div>
                </div>
                
                <h2>Посещения</h2>
                <div class="row">
                    <div class="col">
                        <div class="metric-card">
                            <div class="metric-label">Всего посещений</div>
                            <div class="metric-value">${formatNumber(totalVisits)}</div>
                        </div>
                    </div>
                    <div class="col">
                        <div class="metric-card">
                            <div class="metric-label">Среднее время на сайте</div>
                            <div class="metric-value">${data.avgTimeOnSite || 0} мин</div>
                        </div>
                    </div>
                </div>
                
                <h2>Топ клиенты</h2>
                <table>
                    <thead>
                        <tr><th>№</th><th>Клиент</th><th>Сумма трат</th><th>Кол-во заказов</th></tr>
                    </thead>
                    <tbody>
                        ${(data.topCustomers || []).map((c, i) => `
                            <tr>
                                <td>${i + 1}</td>
                                <td>${escapeHtml(c.userName || 'Неизвестный')}</td>
                                <td>${formatCurrency(c.totalSpent || 0)}</td>
                                <td>${c.orderCount || 0}</td>
                            </tr>
                        `).join('')}
                    </tbody>
                </table>
                
                <h2>Продажи по месяцам</h2>
                <table>
                    <thead><tr><th>Месяц</th><th>Сумма продаж</th></tr></thead>
                    <tbody>
                        ${Object.entries(data.adminSalesByMonth || {}).sort().map(([month, value]) => `
                            <tr><td>${month}</td><td>${formatCurrency(value)}</td></tr>
                        `).join('')}
                    </tbody>
                </table>
                
                <h2>Посещения по месяцам</h2>
                <table>
                    <thead><tr><th>Месяц</th><th>Количество посещений</th></tr></thead>
                    <tbody>
                        ${Object.entries(data.visitsByMonth || {}).sort().map(([month, value]) => `
                            <tr><td>${month}</td><td>${formatNumber(value)}</td></tr>
                        `).join('')}
                    </tbody>
                </table>
                
                <div class="footer">
                    <p>© 2024 Design Studio. Все права защищены.</p>
                </div>
            </body>
            </html>
        `);
        
        printWindow.document.close();
        printWindow.print();
    } catch (error) {
        console.error('Error exporting to PDF:', error);
        showToast('Ошибка при создании отчета', 'danger');
    }
}

// Загрузка частоты покупок за период
async function loadPurchaseFrequency(startDate, endDate) {
    try {
        const res = await apiFetch(`/api/analytics/purchase-frequency?startDate=${startDate}&endDate=${endDate}`);
        if (res.ok) {
            const data = await res.json();
            updateStatCard('purchaseFrequencyValue', data.frequency.toFixed(2));
        }
    } catch (error) {
        console.error('Error loading purchase frequency:', error);
    }
}

// Инициализация форм
function initForms() {
    // Форма частоты покупок
    const frequencyForm = document.getElementById('frequencyForm');
    if (frequencyForm) {
        frequencyForm.addEventListener('submit', async (e) => {
            e.preventDefault();
            const startDate = document.getElementById('frequencyStartDate').value;
            const endDate = document.getElementById('frequencyEndDate').value;

            if (!startDate || !endDate) {
                alert('Пожалуйста, выберите даты');
                return;
            }

            await loadPurchaseFrequency(startDate, endDate);
        });
    }

    // Форма посещений
    const visitsForm = document.getElementById('visitsForm');
    if (visitsForm) {
        visitsForm.addEventListener('submit', async (e) => {
            e.preventDefault();
            const startDate = document.getElementById('visitsStartDate').value;
            const endDate = document.getElementById('visitsEndDate').value;

            if (!startDate || !endDate) {
                alert('Пожалуйста, выберите даты');
                return;
            }

            try {
                const res = await apiFetch(`/api/analytics/visits-by-day?startDate=${startDate}&endDate=${endDate}`);
                if (!res.ok) throw new Error();
                
                const data = await res.json();
                
                const labels = Object.keys(data).sort();
                const values = labels.map(key => data[key] || 0);
                
                // Для посещений всегда используем линейный график с цветом #6B1F3F
                visitsChart = createOrUpdateChart(
                    visitsChart,
                    'visitsChart',
                    'line',  // тип графика
                    labels,
                    values,
                    'Посещения по дням',
                    '#6B1F3F',  // цвет линии
                    'rgba(107, 31, 63, 0.1)'  // фон
                );
            } catch (error) {
                console.error('Error loading visits:', error);
                alert('Ошибка загрузки данных о посещениях');
            }
        });
    }

    // Форма продаж
    const salesForm = document.getElementById('salesForm');
    if (salesForm) {
        salesForm.addEventListener('submit', async (e) => {
            e.preventDefault();
            const startDate = document.getElementById('salesStartDate').value;
            const endDate = document.getElementById('salesEndDate').value;

            if (!startDate || !endDate) {
                alert('Пожалуйста, выберите даты');
                return;
            }

            try {
                const res = await apiFetch(`/api/analytics/sales-by-day?startDate=${startDate}&endDate=${endDate}`);
                if (!res.ok) throw new Error();
                
                const data = await res.json();
                
                const labels = Object.keys(data).sort();
                const values = labels.map(key => data[key] || 0);
                
                salesChart = createOrUpdateChart(
                    salesChart,
                    'salesChart',
                    'bar',
                    labels,
                    values,
                    'Продажи по дням (BYN)',
                    '#28a745',
                    'rgba(40, 167, 69, 0.4)'
                );
            } catch (error) {
                console.error('Error loading sales:', error);
                alert('Ошибка загрузки данных о продажах');
            }
        });
    }

    // Кнопка сброса частоты покупок
    const resetFrequency = document.getElementById('resetFrequency');
    if (resetFrequency) {
        resetFrequency.addEventListener('click', () => {
            document.getElementById('frequencyStartDate').value = '';
            document.getElementById('frequencyEndDate').value = '';
            loadAdminAnalytics();
        });
    }

    // Кнопка сброса графика посещений
    const resetVisits = document.getElementById('resetVisits');
    if (resetVisits) {
        resetVisits.addEventListener('click', () => {
            // Сбрасываем даты
            document.getElementById('visitsStartDate').value = '';
            document.getElementById('visitsEndDate').value = '';
            // Загружаем данные по месяцам
            loadAdminAnalytics();
        });
    }

    // Кнопка сброса графика продаж
    const resetSales = document.getElementById('resetSales');
    if (resetSales) {
        resetSales.addEventListener('click', () => {
            loadAdminAnalytics();
        });
    }
}

// Toast уведомления
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
    }, 3000);

    toast.querySelector('.close-btn').addEventListener('click', () => {
        toast.classList.remove('show');
        setTimeout(() => {
            if (toast.parentNode) toast.parentNode.removeChild(toast);
        }, 500);
    });
}

// Экспорт для глобального доступа
window.AnalyticsModule = {
    loadAdminAnalytics,
    exportToPDF,
    showToast,
    formatCurrency,
    formatNumber,
    formatPercent
};