package com.designstudio.domain.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "products")
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, length = 1000)
    private String description;

    @Column(nullable = false)
    private Double price;

    @ManyToOne
    @JoinColumn(name = "category_id", nullable = false)
    private ProductCategory category;

    @Column(nullable = false)
    private String mainImage;

    @Column
    private String additionalImage1;

    @Column
    private String additionalImage2;

    @Column
    private String additionalImage3;

    // ==================== НОВОЕ ПОЛЕ ====================
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
    // ==================== КОНЕЦ НОВОГО ПОЛЯ ====================

    // Геттеры и сеттеры
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public ProductCategory getCategory() {
        return category;
    }

    public void setCategory(ProductCategory category) {
        this.category = category;
    }

    public String getMainImage() {
        return mainImage;
    }

    public void setMainImage(String mainImage) {
        this.mainImage = mainImage;
    }

    public String getAdditionalImage1() {
        return additionalImage1;
    }

    public void setAdditionalImage1(String additionalImage1) {
        this.additionalImage1 = additionalImage1;
    }

    public String getAdditionalImage2() {
        return additionalImage2;
    }

    public void setAdditionalImage2(String additionalImage2) {
        this.additionalImage2 = additionalImage2;
    }

    public String getAdditionalImage3() {
        return additionalImage3;
    }

    public void setAdditionalImage3(String additionalImage3) {
        this.additionalImage3 = additionalImage3;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}