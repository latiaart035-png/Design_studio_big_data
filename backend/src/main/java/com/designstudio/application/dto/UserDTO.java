// com.designstudio.application.dto.UserDTO.java
package com.designstudio.application.dto;

import java.time.LocalDateTime;

public class UserDTO {
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private String role;
    private String status;
    private Double discount;
    private int totalOrders;
    private Double totalSpent;

    // ===== НОВЫЕ ПОЛЯ ДЛЯ КВАРТАЛЬНОЙ СИСТЕМЫ =====
    private LocalDateTime quarterStartDate;
    private LocalDateTime quarterEndDate;
    private Double quarterSpent;
    private Double premiumThreshold;
    private Double vipThreshold;
    private Double remainingForPremium;
    private Double remainingForVip;
    // ===== КОНЕЦ НОВЫХ ПОЛЕЙ =====

    public UserDTO() {}

    public UserDTO(Long id, String firstName, String lastName, String email,
                   String role, String status) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.role = role;
        this.status = status;
    }

    // Геттеры и сеттеры
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Double getDiscount() { return discount; }
    public void setDiscount(Double discount) { this.discount = discount; }
    public int getTotalOrders() { return totalOrders; }
    public void setTotalOrders(int totalOrders) { this.totalOrders = totalOrders; }
    public Double getTotalSpent() { return totalSpent; }
    public void setTotalSpent(Double totalSpent) { this.totalSpent = totalSpent; }

    // Новые геттеры и сеттеры
    public LocalDateTime getQuarterStartDate() { return quarterStartDate; }
    public void setQuarterStartDate(LocalDateTime quarterStartDate) { this.quarterStartDate = quarterStartDate; }
    public LocalDateTime getQuarterEndDate() { return quarterEndDate; }
    public void setQuarterEndDate(LocalDateTime quarterEndDate) { this.quarterEndDate = quarterEndDate; }
    public Double getQuarterSpent() { return quarterSpent; }
    public void setQuarterSpent(Double quarterSpent) { this.quarterSpent = quarterSpent; }
    public Double getPremiumThreshold() { return premiumThreshold; }
    public void setPremiumThreshold(Double premiumThreshold) { this.premiumThreshold = premiumThreshold; }
    public Double getVipThreshold() { return vipThreshold; }
    public void setVipThreshold(Double vipThreshold) { this.vipThreshold = vipThreshold; }
    public Double getRemainingForPremium() { return remainingForPremium; }
    public void setRemainingForPremium(Double remainingForPremium) { this.remainingForPremium = remainingForPremium; }
    public Double getRemainingForVip() { return remainingForVip; }
    public void setRemainingForVip(Double remainingForVip) { this.remainingForVip = remainingForVip; }

    public String getFullName() {
        return firstName + " " + lastName;
    }

    public Double getDiscountPercent() {
        return discount != null ? discount * 100 : 0.0;
    }
}