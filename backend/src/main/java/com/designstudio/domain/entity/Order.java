package com.designstudio.domain.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status = OrderStatus.PROCESSING;

    @Column(nullable = false)
    private String address;

    @Column(nullable = false)
    private String cardNumber;

    @Column(nullable = false)
    private String cvc;

    @Column(nullable = false)
    private String expiry;

    // ===== НОВОЕ ПОЛЕ: общая сумма заказа =====
    @Column(name = "total_amount")
    private Double totalAmount = 0.0;

    // В Order.java добавьте поле
    @Column(name = "applied_campaign_id")
    private Long appliedCampaignId;

    @Column(name = "discount_amount")
    private Double discountAmount;

    // Геттеры и сеттеры
    public Double getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(double Double) { this.discountAmount = discountAmount; }

    // В Order.java добавьте эти поля
    @Column(name = "original_amount")
    private Double originalAmount;

    // Геттеры и сеттеры
    public Double getOriginalAmount() { return originalAmount; }
    public void setOriginalAmount(Double originalAmount) { this.originalAmount = originalAmount; }

    public Long getAppliedCampaignId() { return appliedCampaignId; }
    public void setAppliedCampaignId(Long appliedCampaignId) { this.appliedCampaignId = appliedCampaignId; }

    public enum OrderStatus {
        PROCESSING, ASSEMBLING, SHIPPING, DELIVERED
    }

    // ============ CONSTRUCTORS ============

    public Order() {
    }

    public Order(User user, String address, String cardNumber, String cvc, String expiry) {
        this.user = user;
        this.address = address;
        this.cardNumber = cardNumber;
        this.cvc = cvc;
        this.expiry = expiry;
        this.createdAt = LocalDateTime.now();
        this.status = OrderStatus.PROCESSING;
        this.totalAmount = 0.0;
    }

    public Order(User user, String address, String cardNumber, String cvc, String expiry, Double totalAmount) {
        this.user = user;
        this.address = address;
        this.cardNumber = cardNumber;
        this.cvc = cvc;
        this.expiry = expiry;
        this.createdAt = LocalDateTime.now();
        this.status = OrderStatus.PROCESSING;
        this.totalAmount = totalAmount;
    }

    // ============ GETTERS & SETTERS ============

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getCardNumber() {
        return cardNumber;
    }

    public void setCardNumber(String cardNumber) {
        this.cardNumber = cardNumber;
    }

    public String getCvc() {
        return cvc;
    }

    public void setCvc(String cvc) {
        this.cvc = cvc;
    }

    public String getExpiry() {
        return expiry;
    }

    public void setExpiry(String expiry) {
        this.expiry = expiry;
    }

    // ===== GETTER/SETTER ДЛЯ totalAmount =====

    public Double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(Double totalAmount) {
        this.totalAmount = totalAmount;
    }

    // ============ HELPER METHODS ============

    public void addAmount(Double amount) {
        if (this.totalAmount == null) {
            this.totalAmount = 0.0;
        }
        this.totalAmount += amount;
    }

    public boolean isCompleted() {
        return status == OrderStatus.DELIVERED;
    }

    public boolean isActive() {
        return status == OrderStatus.PROCESSING ||
                status == OrderStatus.ASSEMBLING ||
                status == OrderStatus.SHIPPING;
    }

    @Override
    public String toString() {
        return "Order{" +
                "id=" + id +
                ", user=" + (user != null ? user.getEmail() : "null") +
                ", status=" + status +
                ", totalAmount=" + totalAmount +
                ", createdAt=" + createdAt +
                '}';
    }
}