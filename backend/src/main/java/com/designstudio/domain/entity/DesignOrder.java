// File: com.designstudio.domain.entity.DesignOrder.java
package com.designstudio.domain.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "design_orders")
public class DesignOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String roomType;

    @Column(nullable = false)
    private String tariff;

    @Column(nullable = false)
    private String preferences;

    // Новое поле для стиля дизайна
    @Column(name = "design_style")
    private String designStyle;

    @Column(nullable = false)
    private String address;

    @Column(nullable = false)
    private String cardNumber;

    @Column(nullable = false)
    private String cvc;

    @Column(nullable = false)
    private String expiry;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status = OrderStatus.PENDING;

    // Храним выбранные товары в формате JSON (список ID товаров)
    @Column(name = "selected_products", length = 2000)
    private String selectedProducts;

    // Универсальное поле для фото - храним в виде JSON массива путей к файлам
    @Column(name = "photos", length = 4000)
    private String photos;

    public enum OrderStatus {
        PENDING, IN_PROGRESS, COMPLETED
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public String getRoomType() { return roomType; }
    public void setRoomType(String roomType) { this.roomType = roomType; }
    public String getTariff() { return tariff; }
    public void setTariff(String tariff) { this.tariff = tariff; }
    public String getPreferences() { return preferences; }
    public void setPreferences(String preferences) { this.preferences = preferences; }
    public String getDesignStyle() { return designStyle; }
    public void setDesignStyle(String designStyle) { this.designStyle = designStyle; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getCardNumber() { return cardNumber; }
    public void setCardNumber(String cardNumber) { this.cardNumber = cardNumber; }
    public String getCvc() { return cvc; }
    public void setCvc(String cvc) { this.cvc = cvc; }
    public String getExpiry() { return expiry; }
    public void setExpiry(String expiry) { this.expiry = expiry; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public OrderStatus getStatus() { return status; }
    public void setStatus(OrderStatus status) { this.status = status; }
    public String getSelectedProducts() { return selectedProducts; }
    public void setSelectedProducts(String selectedProducts) { this.selectedProducts = selectedProducts; }
    public String getPhotos() { return photos; }
    public void setPhotos(String photos) { this.photos = photos; }
}