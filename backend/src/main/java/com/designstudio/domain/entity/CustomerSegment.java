// com.designstudio.domain.entity.CustomerSegment.java
package com.designstudio.domain.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "customer_segments")
public class CustomerSegment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(length = 1000)
    private String description;

    // Критерии сегментации
    @Column(name = "min_orders")
    private Integer minOrders;

    @Column(name = "max_orders")
    private Integer maxOrders;

    @Column(name = "min_spent")
    private Double minSpent;

    @Column(name = "max_spent")
    private Double maxSpent;

    @Column(name = "status_requirement")
    private String statusRequirement;

    @Column(name = "activity_days")
    private Integer activityDays;

    // Метаданные
    @Column(name = "total_customers")
    private Integer totalCustomers;

    @Column(name = "avg_order_value")
    private Double avgOrderValue;

    @Column
    private String color;

    @Column
    private String icon;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
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
    public Integer getTotalCustomers() { return totalCustomers; }
    public void setTotalCustomers(Integer totalCustomers) { this.totalCustomers = totalCustomers; }
    public Double getAvgOrderValue() { return avgOrderValue; }
    public void setAvgOrderValue(Double avgOrderValue) { this.avgOrderValue = avgOrderValue; }
    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}