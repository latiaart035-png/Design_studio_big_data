package com.designstudio.application.dto;

import java.util.List;
import java.util.Map;

public class AnalyticsDTO {
    private int totalOrders;
    private Double totalSpent;
    private Double avgCheck;
    private String clientType;
    private Map<String, Double> salesByMonth;
    private List<String> trends;
    private int visitsCount;
    private int totalVisitDuration;
    private Double globalAvgCheck;
    private Double conversionRate;
    private Double avgVisitsPerUser;
    private Double avgTimeOnSite;
    private Double purchaseFrequency;
    private List<TopCustomerDTO> topCustomers;
    private Map<String, Long> visitsByMonth;
    private Map<String, Double> adminSalesByMonth;
    private Long totalUsers;
    private Long totalProducts;
    private Long totalOrdersToday;

    public static class TopCustomerDTO {
        private Long userId;
        private String userName;
        private Double totalSpent;
        private int orderCount;

        public TopCustomerDTO() {}

        public TopCustomerDTO(Long userId, String userName, Double totalSpent, int orderCount) {
            this.userId = userId;
            this.userName = userName;
            this.totalSpent = totalSpent;
            this.orderCount = orderCount;
        }

        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        public String getUserName() { return userName; }
        public void setUserName(String userName) { this.userName = userName; }
        public Double getTotalSpent() { return totalSpent; }
        public void setTotalSpent(Double totalSpent) { this.totalSpent = totalSpent; }
        public int getOrderCount() { return orderCount; }
        public void setOrderCount(int orderCount) { this.orderCount = orderCount; }
    }

    // Геттеры и сеттеры
    public int getTotalOrders() { return totalOrders; }
    public void setTotalOrders(int totalOrders) { this.totalOrders = totalOrders; }
    public Double getTotalSpent() { return totalSpent; }
    public void setTotalSpent(Double totalSpent) { this.totalSpent = totalSpent; }
    public Double getAvgCheck() { return avgCheck; }
    public void setAvgCheck(Double avgCheck) { this.avgCheck = avgCheck; }
    public String getClientType() { return clientType; }
    public void setClientType(String clientType) { this.clientType = clientType; }
    public Map<String, Double> getSalesByMonth() { return salesByMonth; }
    public void setSalesByMonth(Map<String, Double> salesByMonth) { this.salesByMonth = salesByMonth; }
    public List<String> getTrends() { return trends; }
    public void setTrends(List<String> trends) { this.trends = trends; }
    public int getVisitsCount() { return visitsCount; }
    public void setVisitsCount(int visitsCount) { this.visitsCount = visitsCount; }
    public int getTotalVisitDuration() { return totalVisitDuration; }
    public void setTotalVisitDuration(int totalVisitDuration) { this.totalVisitDuration = totalVisitDuration; }
    public Double getGlobalAvgCheck() { return globalAvgCheck; }
    public void setGlobalAvgCheck(Double globalAvgCheck) { this.globalAvgCheck = globalAvgCheck; }
    public Double getConversionRate() { return conversionRate; }
    public void setConversionRate(Double conversionRate) { this.conversionRate = conversionRate; }
    public Double getAvgVisitsPerUser() { return avgVisitsPerUser; }
    public void setAvgVisitsPerUser(Double avgVisitsPerUser) { this.avgVisitsPerUser = avgVisitsPerUser; }
    public Double getAvgTimeOnSite() { return avgTimeOnSite; }
    public void setAvgTimeOnSite(Double avgTimeOnSite) { this.avgTimeOnSite = avgTimeOnSite; }
    public Double getPurchaseFrequency() { return purchaseFrequency; }
    public void setPurchaseFrequency(Double purchaseFrequency) { this.purchaseFrequency = purchaseFrequency; }
    public List<TopCustomerDTO> getTopCustomers() { return topCustomers; }
    public void setTopCustomers(List<TopCustomerDTO> topCustomers) { this.topCustomers = topCustomers; }
    public Map<String, Long> getVisitsByMonth() { return visitsByMonth; }
    public void setVisitsByMonth(Map<String, Long> visitsByMonth) { this.visitsByMonth = visitsByMonth; }
    public Map<String, Double> getAdminSalesByMonth() { return adminSalesByMonth; }
    public void setAdminSalesByMonth(Map<String, Double> adminSalesByMonth) { this.adminSalesByMonth = adminSalesByMonth; }
    public Long getTotalUsers() { return totalUsers; }
    public void setTotalUsers(Long totalUsers) { this.totalUsers = totalUsers; }
    public Long getTotalProducts() { return totalProducts; }
    public void setTotalProducts(Long totalProducts) { this.totalProducts = totalProducts; }
    public Long getTotalOrdersToday() { return totalOrdersToday; }
    public void setTotalOrdersToday(Long totalOrdersToday) { this.totalOrdersToday = totalOrdersToday; }
}