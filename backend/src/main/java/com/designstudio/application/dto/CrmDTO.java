package com.designstudio.application.dto;

import java.util.List;
import java.util.Map;

public class CrmDTO {

    // ===== СЕГМЕНТЫ =====
    public static class SegmentDTO {
        private Long id;
        private String name;
        private String description;
        private Integer totalCustomers;
        private Double avgOrderValue;
        private Double totalRevenue;
        private String color;
        private String icon;
        private List<String> criteria;

        // === Дополнительные поля ===
        private Integer minOrders;
        private Integer maxOrders;
        private Double minSpent;
        private Double maxSpent;
        private String statusRequirement;
        private Integer activityDays;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDescription() { return description != null ? description : ""; }
        public void setDescription(String description) { this.description = description; }
        public Integer getTotalCustomers() { return totalCustomers != null ? totalCustomers : 0; }
        public void setTotalCustomers(Integer totalCustomers) { this.totalCustomers = totalCustomers; }
        public Double getAvgOrderValue() { return avgOrderValue != null ? avgOrderValue : 0.0; }
        public void setAvgOrderValue(Double avgOrderValue) { this.avgOrderValue = avgOrderValue; }
        public Double getTotalRevenue() { return totalRevenue != null ? totalRevenue : 0.0; }
        public void setTotalRevenue(Double totalRevenue) { this.totalRevenue = totalRevenue; }
        public String getColor() { return color != null ? color : "#6c757d"; }
        public void setColor(String color) { this.color = color; }
        public String getIcon() { return icon != null ? icon : "bi-people"; }
        public void setIcon(String icon) { this.icon = icon; }
        public List<String> getCriteria() { return criteria; }
        public void setCriteria(List<String> criteria) { this.criteria = criteria; }

        // Дополнительные геттеры/сеттеры
        public Integer getMinOrders() { return minOrders; }
        public void setMinOrders(Integer minOrders) { this.minOrders = minOrders; }
        public Integer getMaxOrders() { return maxOrders; }
        public void setMaxOrders(Integer maxOrders) { this.maxOrders = maxOrders; }
        public Double getMinSpent() { return minSpent; }
        public void setMinSpent(Double minSpent) { this.minSpent = minSpent; }
        public Double getMaxSpent() { return maxSpent; }
        public void setMaxSpent(Double maxSpent) { this.maxSpent = maxSpent; }
        public String getStatusRequirement() { return statusRequirement; }
        public void setStatusRequirement(String statusRequirement) { this.statusRequirement = statusRequirement; }
        public Integer getActivityDays() { return activityDays; }
        public void setActivityDays(Integer activityDays) { this.activityDays = activityDays; }
    }

    // ===== КЛИЕНТЫ =====
    public static class CustomerDTO {
        private Long id;
        private String firstName;
        private String lastName;
        private String email;
        private String phone;
        private String segment;
        private String status;
        private Integer totalOrders;
        private Double totalSpent;
        private Double avgOrderValue;
        private Integer daysSinceLastOrder;
        private Integer daysSinceRegistration;
        private String rfmSegment;
        private Double clv;
        private String lastOrderDate;
        private List<String> purchasedCategories;

        // === Дополнительные поля ===
        private String registrationDate;
        private Integer totalVisits;
        private Double avgVisitDuration;
        private String clientType;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getFirstName() { return firstName != null ? firstName : ""; }
        public void setFirstName(String firstName) { this.firstName = firstName; }
        public String getLastName() { return lastName != null ? lastName : ""; }
        public void setLastName(String lastName) { this.lastName = lastName; }
        public String getEmail() { return email != null ? email : ""; }
        public void setEmail(String email) { this.email = email; }
        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
        public String getSegment() { return segment != null ? segment : "NEW"; }
        public void setSegment(String segment) { this.segment = segment; }
        public String getStatus() { return status != null ? status : "BASIC"; }
        public void setStatus(String status) { this.status = status; }
        public Integer getTotalOrders() { return totalOrders != null ? totalOrders : 0; }
        public void setTotalOrders(Integer totalOrders) { this.totalOrders = totalOrders; }
        public Double getTotalSpent() { return totalSpent != null ? totalSpent : 0.0; }
        public void setTotalSpent(Double totalSpent) { this.totalSpent = totalSpent; }
        public Double getAvgOrderValue() { return avgOrderValue != null ? avgOrderValue : 0.0; }
        public void setAvgOrderValue(Double avgOrderValue) { this.avgOrderValue = avgOrderValue; }
        public Integer getDaysSinceLastOrder() { return daysSinceLastOrder != null ? daysSinceLastOrder : 0; }
        public void setDaysSinceLastOrder(Integer daysSinceLastOrder) { this.daysSinceLastOrder = daysSinceLastOrder; }
        public Integer getDaysSinceRegistration() { return daysSinceRegistration != null ? daysSinceRegistration : 0; }
        public void setDaysSinceRegistration(Integer daysSinceRegistration) { this.daysSinceRegistration = daysSinceRegistration; }
        public String getRfmSegment() { return rfmSegment != null ? rfmSegment : "New"; }
        public void setRfmSegment(String rfmSegment) { this.rfmSegment = rfmSegment; }
        public Double getClv() { return clv != null ? clv : 0.0; }
        public void setClv(Double clv) { this.clv = clv; }
        public String getLastOrderDate() { return lastOrderDate; }
        public void setLastOrderDate(String lastOrderDate) { this.lastOrderDate = lastOrderDate; }
        public List<String> getPurchasedCategories() { return purchasedCategories; }
        public void setPurchasedCategories(List<String> purchasedCategories) { this.purchasedCategories = purchasedCategories; }

        public String getFullName() {
            return (firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "");
        }

        // Дополнительные геттеры/сеттеры
        public String getRegistrationDate() { return registrationDate; }
        public void setRegistrationDate(String registrationDate) { this.registrationDate = registrationDate; }
        public Integer getTotalVisits() { return totalVisits != null ? totalVisits : 0; }
        public void setTotalVisits(Integer totalVisits) { this.totalVisits = totalVisits; }
        public Double getAvgVisitDuration() { return avgVisitDuration != null ? avgVisitDuration : 0.0; }
        public void setAvgVisitDuration(Double avgVisitDuration) { this.avgVisitDuration = avgVisitDuration; }
        public String getClientType() { return clientType != null ? clientType : "BASIC"; }
        public void setClientType(String clientType) { this.clientType = clientType; }
    }

    // ===== RFM АНАЛИЗ =====
    public static class RfmAnalysisDTO {
        private String segment;
        private Integer customerCount;
        private Double totalRevenue;
        private Double avgRevenue;
        private String description;
        private String recommendedAction;

        // === Дополнительные поля ===
        private Double avgRecencyDays;
        private Double avgFrequency;
        private Double avgMonetary;

        public String getSegment() { return segment != null ? segment : "Unknown"; }
        public void setSegment(String segment) { this.segment = segment; }
        public Integer getCustomerCount() { return customerCount != null ? customerCount : 0; }
        public void setCustomerCount(Integer customerCount) { this.customerCount = customerCount; }
        public Double getTotalRevenue() { return totalRevenue != null ? totalRevenue : 0.0; }
        public void setTotalRevenue(Double totalRevenue) { this.totalRevenue = totalRevenue; }
        public Double getAvgRevenue() { return avgRevenue != null ? avgRevenue : 0.0; }
        public void setAvgRevenue(Double avgRevenue) { this.avgRevenue = avgRevenue; }
        public String getDescription() { return description != null ? description : ""; }
        public void setDescription(String description) { this.description = description; }
        public String getRecommendedAction() { return recommendedAction != null ? recommendedAction : ""; }
        public void setRecommendedAction(String recommendedAction) { this.recommendedAction = recommendedAction; }

        // Дополнительные геттеры/сеттеры
        public Double getAvgRecencyDays() { return avgRecencyDays != null ? avgRecencyDays : 0.0; }
        public void setAvgRecencyDays(Double avgRecencyDays) { this.avgRecencyDays = avgRecencyDays; }
        public Double getAvgFrequency() { return avgFrequency != null ? avgFrequency : 0.0; }
        public void setAvgFrequency(Double avgFrequency) { this.avgFrequency = avgFrequency; }
        public Double getAvgMonetary() { return avgMonetary != null ? avgMonetary : 0.0; }
        public void setAvgMonetary(Double avgMonetary) { this.avgMonetary = avgMonetary; }
    }

    // ===== ABC АНАЛИЗ =====
    public static class AbcAnalysisDTO {
        private String segment;
        private Integer customerCount;
        private Double revenue;
        private Double revenuePercent;
        private Double cumulativePercent;
        private String description;

        // === Дополнительные поля ===
        private Double avgOrderValue;
        private Double totalOrders;
        private Double conversionRate;

        public String getSegment() { return segment != null ? segment : "Unknown"; }
        public void setSegment(String segment) { this.segment = segment; }
        public Integer getCustomerCount() { return customerCount != null ? customerCount : 0; }
        public void setCustomerCount(Integer customerCount) { this.customerCount = customerCount; }
        public Double getRevenue() { return revenue != null ? revenue : 0.0; }
        public void setRevenue(Double revenue) { this.revenue = revenue; }
        public Double getRevenuePercent() { return revenuePercent != null ? revenuePercent : 0.0; }
        public void setRevenuePercent(Double revenuePercent) { this.revenuePercent = revenuePercent; }
        public Double getCumulativePercent() { return cumulativePercent != null ? cumulativePercent : 0.0; }
        public void setCumulativePercent(Double cumulativePercent) { this.cumulativePercent = cumulativePercent; }
        public String getDescription() { return description != null ? description : ""; }
        public void setDescription(String description) { this.description = description; }

        // Дополнительные геттеры/сеттеры
        public Double getAvgOrderValue() { return avgOrderValue != null ? avgOrderValue : 0.0; }
        public void setAvgOrderValue(Double avgOrderValue) { this.avgOrderValue = avgOrderValue; }
        public Double getTotalOrders() { return totalOrders != null ? totalOrders : 0.0; }
        public void setTotalOrders(Double totalOrders) { this.totalOrders = totalOrders; }
        public Double getConversionRate() { return conversionRate != null ? conversionRate : 0.0; }
        public void setConversionRate(Double conversionRate) { this.conversionRate = conversionRate; }
    }

    // ===== РАСПРЕДЕЛЕНИЕ СЕГМЕНТОВ (для диаграммы) =====
    public static class SegmentDistributionDTO {
        private String segment;
        private Integer count;
        private Double avgCheck;
        private Double totalRevenue;
        private Double revenuePercent;

        public String getSegment() { return segment != null ? segment : "Unknown"; }
        public void setSegment(String segment) { this.segment = segment; }
        public Integer getCount() { return count != null ? count : 0; }
        public void setCount(Integer count) { this.count = count; }
        public Double getAvgCheck() { return avgCheck != null ? avgCheck : 0.0; }
        public void setAvgCheck(Double avgCheck) { this.avgCheck = avgCheck; }
        public Double getTotalRevenue() { return totalRevenue != null ? totalRevenue : 0.0; }
        public void setTotalRevenue(Double totalRevenue) { this.totalRevenue = totalRevenue; }
        public Double getRevenuePercent() { return revenuePercent != null ? revenuePercent : 0.0; }
        public void setRevenuePercent(Double revenuePercent) { this.revenuePercent = revenuePercent; }
    }

    // ===== СТАТИСТИКА СЕГМЕНТА =====
    public static class SegmentStatsDTO {
        private Integer count;
        private Double avgCheck;
        private Double totalRevenue;
        private Double avgVisits;
        private Integer totalOrders;

        public Integer getCount() { return count != null ? count : 0; }
        public void setCount(Integer count) { this.count = count; }
        public Double getAvgCheck() { return avgCheck != null ? avgCheck : 0.0; }
        public void setAvgCheck(Double avgCheck) { this.avgCheck = avgCheck; }
        public Double getTotalRevenue() { return totalRevenue != null ? totalRevenue : 0.0; }
        public void setTotalRevenue(Double totalRevenue) { this.totalRevenue = totalRevenue; }
        public Double getAvgVisits() { return avgVisits != null ? avgVisits : 0.0; }
        public void setAvgVisits(Double avgVisits) { this.avgVisits = avgVisits; }
        public Integer getTotalOrders() { return totalOrders != null ? totalOrders : 0; }
        public void setTotalOrders(Integer totalOrders) { this.totalOrders = totalOrders; }
    }

    // ===== СТАТИСТИКА CRM =====
    public static class CrmStatsDTO {
        // === ОСНОВНЫЕ МЕТРИКИ ===
        private Long totalCustomers;
        private Long newCustomersToday;
        private Long newCustomersThisWeek;
        private Long newCustomersThisMonth;
        private Double totalRevenue;
        private Double avgOrderValue;
        private Double avgCustomerValue;
        private Double conversionRate;
        private Double retentionRate;
        private Double churnRate;
        private Long totalOrders;
        private Long totalVisits;
        private Integer avgVisitDuration;

        // === ТРЕНДЫ (изменения в процентах) ===
        // ИСПРАВЛЕНО: возвращаем null когда данных нет
        private Double revenueChangePercent;
        private Double ordersChangePercent;
        private Double customersChangePercent;
        private Double avgCheckChangePercent;
        private Double conversionChangePercent;
        private Double retentionChangePercent;

        // === ДАННЫЕ ПО СЕГМЕНТАМ ===
        private Map<String, Integer> customersBySegment;
        private Map<String, Double> revenueBySegment;
        private List<SegmentDistributionDTO> segmentDistribution;
        private Map<String, SegmentStatsDTO> segmentStats;
        private List<RfmAnalysisDTO> rfmAnalysis;
        private List<AbcAnalysisDTO> abcAnalysis;

        // === ДАННЫЕ ДЛЯ ГРАФИКОВ ===
        private Map<String, Double> adminSalesByMonth;
        private Map<String, Long> visitsByMonth;

        // === ДОПОЛНИТЕЛЬНЫЕ ПОЛЯ ===
        private Double avgVisitsPerUser;
        private Double customerAcquisitionCost;
        private Double customerLifetimeValue;
        private Integer activeUsersLast30Days;
        private Integer returningCustomers;

        // =========================================
        // ГЕТТЕРЫ И СЕТТЕРЫ
        // =========================================

        // Основные метрики
        public Long getTotalCustomers() { return totalCustomers != null ? totalCustomers : 0L; }
        public void setTotalCustomers(Long totalCustomers) { this.totalCustomers = totalCustomers; }

        public Long getNewCustomersToday() { return newCustomersToday != null ? newCustomersToday : 0L; }
        public void setNewCustomersToday(Long newCustomersToday) { this.newCustomersToday = newCustomersToday; }

        public Long getNewCustomersThisWeek() { return newCustomersThisWeek != null ? newCustomersThisWeek : 0L; }
        public void setNewCustomersThisWeek(Long newCustomersThisWeek) { this.newCustomersThisWeek = newCustomersThisWeek; }

        public Long getNewCustomersThisMonth() { return newCustomersThisMonth != null ? newCustomersThisMonth : 0L; }
        public void setNewCustomersThisMonth(Long newCustomersThisMonth) { this.newCustomersThisMonth = newCustomersThisMonth; }

        public Double getTotalRevenue() { return totalRevenue != null ? totalRevenue : 0.0; }
        public void setTotalRevenue(Double totalRevenue) { this.totalRevenue = totalRevenue; }

        public Double getAvgOrderValue() { return avgOrderValue != null ? avgOrderValue : 0.0; }
        public void setAvgOrderValue(Double avgOrderValue) { this.avgOrderValue = avgOrderValue; }

        public Double getAvgCustomerValue() { return avgCustomerValue != null ? avgCustomerValue : 0.0; }
        public void setAvgCustomerValue(Double avgCustomerValue) { this.avgCustomerValue = avgCustomerValue; }

        public Double getConversionRate() { return conversionRate != null ? conversionRate : 0.0; }
        public void setConversionRate(Double conversionRate) { this.conversionRate = conversionRate; }

        public Double getRetentionRate() { return retentionRate != null ? retentionRate : 0.0; }
        public void setRetentionRate(Double retentionRate) { this.retentionRate = retentionRate; }

        public Double getChurnRate() { return churnRate != null ? churnRate : 0.0; }
        public void setChurnRate(Double churnRate) { this.churnRate = churnRate; }

        public Long getTotalOrders() { return totalOrders != null ? totalOrders : 0L; }
        public void setTotalOrders(Long totalOrders) { this.totalOrders = totalOrders; }

        public Long getTotalVisits() { return totalVisits != null ? totalVisits : 0L; }
        public void setTotalVisits(Long totalVisits) { this.totalVisits = totalVisits; }

        public Integer getAvgVisitDuration() { return avgVisitDuration != null ? avgVisitDuration : 0; }
        public void setAvgVisitDuration(Integer avgVisitDuration) { this.avgVisitDuration = avgVisitDuration; }

        // =========================================
        // ТРЕНДЫ - ИСПРАВЛЕНО: возвращаем null когда данных нет
        // =========================================

        public Double getRevenueChangePercent() { return revenueChangePercent; }
        public void setRevenueChangePercent(Double revenueChangePercent) { this.revenueChangePercent = revenueChangePercent; }

        public Double getOrdersChangePercent() { return ordersChangePercent; }
        public void setOrdersChangePercent(Double ordersChangePercent) { this.ordersChangePercent = ordersChangePercent; }

        public Double getCustomersChangePercent() { return customersChangePercent; }
        public void setCustomersChangePercent(Double customersChangePercent) { this.customersChangePercent = customersChangePercent; }

        public Double getAvgCheckChangePercent() { return avgCheckChangePercent; }
        public void setAvgCheckChangePercent(Double avgCheckChangePercent) { this.avgCheckChangePercent = avgCheckChangePercent; }

        public Double getConversionChangePercent() { return conversionChangePercent; }
        public void setConversionChangePercent(Double conversionChangePercent) { this.conversionChangePercent = conversionChangePercent; }

        public Double getRetentionChangePercent() { return retentionChangePercent; }
        public void setRetentionChangePercent(Double retentionChangePercent) { this.retentionChangePercent = retentionChangePercent; }

        // =========================================
        // КАРТЫ ДАННЫХ
        // =========================================

        public Map<String, Integer> getCustomersBySegment() { return customersBySegment; }
        public void setCustomersBySegment(Map<String, Integer> customersBySegment) { this.customersBySegment = customersBySegment; }

        public Map<String, Double> getRevenueBySegment() { return revenueBySegment; }
        public void setRevenueBySegment(Map<String, Double> revenueBySegment) { this.revenueBySegment = revenueBySegment; }

        public List<SegmentDistributionDTO> getSegmentDistribution() { return segmentDistribution; }
        public void setSegmentDistribution(List<SegmentDistributionDTO> segmentDistribution) { this.segmentDistribution = segmentDistribution; }

        public Map<String, SegmentStatsDTO> getSegmentStats() { return segmentStats; }
        public void setSegmentStats(Map<String, SegmentStatsDTO> segmentStats) { this.segmentStats = segmentStats; }

        public List<RfmAnalysisDTO> getRfmAnalysis() { return rfmAnalysis; }
        public void setRfmAnalysis(List<RfmAnalysisDTO> rfmAnalysis) { this.rfmAnalysis = rfmAnalysis; }

        public List<AbcAnalysisDTO> getAbcAnalysis() { return abcAnalysis; }
        public void setAbcAnalysis(List<AbcAnalysisDTO> abcAnalysis) { this.abcAnalysis = abcAnalysis; }

        public Map<String, Double> getAdminSalesByMonth() { return adminSalesByMonth; }
        public void setAdminSalesByMonth(Map<String, Double> adminSalesByMonth) { this.adminSalesByMonth = adminSalesByMonth; }

        public Map<String, Long> getVisitsByMonth() { return visitsByMonth; }
        public void setVisitsByMonth(Map<String, Long> visitsByMonth) { this.visitsByMonth = visitsByMonth; }

        // =========================================
        // ДОПОЛНИТЕЛЬНЫЕ МЕТРИКИ
        // =========================================

        public Double getAvgVisitsPerUser() { return avgVisitsPerUser != null ? avgVisitsPerUser : 0.0; }
        public void setAvgVisitsPerUser(Double avgVisitsPerUser) { this.avgVisitsPerUser = avgVisitsPerUser; }

        public Double getCustomerAcquisitionCost() { return customerAcquisitionCost != null ? customerAcquisitionCost : 0.0; }
        public void setCustomerAcquisitionCost(Double customerAcquisitionCost) { this.customerAcquisitionCost = customerAcquisitionCost; }

        public Double getCustomerLifetimeValue() { return customerLifetimeValue != null ? customerLifetimeValue : 0.0; }
        public void setCustomerLifetimeValue(Double customerLifetimeValue) { this.customerLifetimeValue = customerLifetimeValue; }

        public Integer getActiveUsersLast30Days() { return activeUsersLast30Days != null ? activeUsersLast30Days : 0; }
        public void setActiveUsersLast30Days(Integer activeUsersLast30Days) { this.activeUsersLast30Days = activeUsersLast30Days; }

        public Integer getReturningCustomers() { return returningCustomers != null ? returningCustomers : 0; }
        public void setReturningCustomers(Integer returningCustomers) { this.returningCustomers = returningCustomers; }
    }

    // ===== ВОРОНКА ПРОДАЖ =====
    public static class FunnelDTO {
        private long visits;
        private long carts;
        private long orders;
        private long paid;
        private double visitToCartPercent;
        private double cartToOrderPercent;
        private double orderToPayPercent;
        private double totalConversion;
        private double avgOrderValue;

        public long getVisits() { return visits; }
        public void setVisits(long visits) { this.visits = visits; }
        public long getCarts() { return carts; }
        public void setCarts(long carts) { this.carts = carts; }
        public long getOrders() { return orders; }
        public void setOrders(long orders) { this.orders = orders; }
        public long getPaid() { return paid; }
        public void setPaid(long paid) { this.paid = paid; }
        public double getVisitToCartPercent() { return visitToCartPercent; }
        public void setVisitToCartPercent(double visitToCartPercent) { this.visitToCartPercent = visitToCartPercent; }
        public double getCartToOrderPercent() { return cartToOrderPercent; }
        public void setCartToOrderPercent(double cartToOrderPercent) { this.cartToOrderPercent = cartToOrderPercent; }
        public double getOrderToPayPercent() { return orderToPayPercent; }
        public void setOrderToPayPercent(double orderToPayPercent) { this.orderToPayPercent = orderToPayPercent; }
        public double getTotalConversion() { return totalConversion; }
        public void setTotalConversion(double totalConversion) { this.totalConversion = totalConversion; }
        public double getAvgOrderValue() { return avgOrderValue; }
        public void setAvgOrderValue(double avgOrderValue) { this.avgOrderValue = avgOrderValue; }
    }

    // ===== АКТИВНОСТЬ =====
    public static class ActivityStatsDTO {
        private double avgDailyOrders;
        private String mostActiveDay;
        private long mostActiveDayOrders;
        private long inactiveDays;
        private long totalOrders;
        private Map<String, Long> dailyOrders;
        private Map<String, Double> monthlyRevenue;

        // Геттеры и сеттеры
        public double getAvgDailyOrders() { return avgDailyOrders; }
        public void setAvgDailyOrders(double avgDailyOrders) { this.avgDailyOrders = avgDailyOrders; }
        public String getMostActiveDay() { return mostActiveDay; }
        public void setMostActiveDay(String mostActiveDay) { this.mostActiveDay = mostActiveDay; }
        public long getMostActiveDayOrders() { return mostActiveDayOrders; }
        public void setMostActiveDayOrders(long mostActiveDayOrders) { this.mostActiveDayOrders = mostActiveDayOrders; }
        public long getInactiveDays() { return inactiveDays; }
        public void setInactiveDays(long inactiveDays) { this.inactiveDays = inactiveDays; }
        public long getTotalOrders() { return totalOrders; }
        public void setTotalOrders(long totalOrders) { this.totalOrders = totalOrders; }
        public Map<String, Long> getDailyOrders() { return dailyOrders; }
        public void setDailyOrders(Map<String, Long> dailyOrders) { this.dailyOrders = dailyOrders; }
        public Map<String, Double> getMonthlyRevenue() { return monthlyRevenue; }
        public void setMonthlyRevenue(Map<String, Double> monthlyRevenue) { this.monthlyRevenue = monthlyRevenue; }
    }
}