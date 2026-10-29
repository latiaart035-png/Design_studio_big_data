package com.designstudio.application.dto;

import java.util.List;

public class ProductDTO {
    private Long id;
    private String name;
    private String description;
    private Double price;
    private Double discountedPrice;
    private String categoryName;
    private Long categoryId;
    private String mainImageUrl;
    private List<String> additionalImages;
    private boolean hasDiscount;

    public ProductDTO() {}

    public ProductDTO(Long id, String name, String description, Double price,
                      String categoryName, String mainImageUrl) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.price = price;
        this.categoryName = categoryName;
        this.mainImageUrl = mainImageUrl;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }

    public Double getDiscountedPrice() { return discountedPrice; }
    public void setDiscountedPrice(Double discountedPrice) { this.discountedPrice = discountedPrice; }

    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }

    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }

    public String getMainImageUrl() { return mainImageUrl; }
    public void setMainImageUrl(String mainImageUrl) { this.mainImageUrl = mainImageUrl; }

    public List<String> getAdditionalImages() { return additionalImages; }
    public void setAdditionalImages(List<String> additionalImages) { this.additionalImages = additionalImages; }

    public boolean isHasDiscount() { return hasDiscount; }
    public void setHasDiscount(boolean hasDiscount) { this.hasDiscount = hasDiscount; }
}