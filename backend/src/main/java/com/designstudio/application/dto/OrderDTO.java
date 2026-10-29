package com.designstudio.application.dto;

import java.time.LocalDateTime;
import java.util.List;

public class OrderDTO {
    private Long id;
    private LocalDateTime createdAt;
    private String status;
    private String address;
    private List<OrderItemDTO> items;
    private Double total;
    private Double discount;
    private String type;
    private String roomType;
    private Double finalTotal;
    private String tariff;
    private String preferences;
    private String designStyle;
    private String selectedProducts;
    private List<String> photos;
    private String campaignName;  // Добавлено
    private Double campaignDiscountPercent;  // Добавлено

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public List<OrderItemDTO> getItems() {
        return items;
    }

    public void setItems(List<OrderItemDTO> items) {
        this.items = items;
    }

    public Double getTotal() {
        return total;
    }
    public Double getFinalTotal() { return finalTotal; }
    public void setFinalTotal(Double finalTotal) { this.finalTotal = finalTotal; }


    public void setTotal(Double total) {
        this.total = total;
    }

    public Double getDiscount() {
        return discount;
    }

    public void setDiscount(Double discount) {
        this.discount = discount;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getRoomType() {
        return roomType;
    }

    public void setRoomType(String roomType) {
        this.roomType = roomType;
    }

    public String getTariff() {
        return tariff;
    }

    public void setTariff(String tariff) {
        this.tariff = tariff;
    }

    public String getPreferences() {
        return preferences;
    }

    public void setPreferences(String preferences) {
        this.preferences = preferences;
    }

    public String getDesignStyle() {
        return designStyle;
    }

    public void setDesignStyle(String designStyle) {
        this.designStyle = designStyle;
    }

    public String getSelectedProducts() {
        return selectedProducts;
    }

    public void setSelectedProducts(String selectedProducts) {
        this.selectedProducts = selectedProducts;
    }

    public List<String> getPhotos() {
        return photos;
    }

    public void setPhotos(List<String> photos) {
        this.photos = photos;
    }
    public String getCampaignName() { return campaignName; }
    public void setCampaignName(String campaignName) { this.campaignName = campaignName; }

    public Double getCampaignDiscountPercent() { return campaignDiscountPercent; }
    public void setCampaignDiscountPercent(Double campaignDiscountPercent) { this.campaignDiscountPercent = campaignDiscountPercent; }
}