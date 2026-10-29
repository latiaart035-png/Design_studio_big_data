package com.designstudio.application.dto;

public class OrderItemDTO {
    private Long productId;
    private String productName;
    private String productImage;
    private Integer quantity;
    private Double price;
    private Double totalPrice;

    public OrderItemDTO() {}

    public OrderItemDTO(Long productId, String productName, String productImage,
                        Integer quantity, Double price) {
        this.productId = productId;
        this.productName = productName;
        this.productImage = productImage;
        this.quantity = quantity;
        this.price = price;
        this.totalPrice = price * quantity;
    }

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public String getProductImage() { return productImage; }
    public void setProductImage(String productImage) { this.productImage = productImage; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }
    public Double getTotalPrice() { return totalPrice; }
    public void setTotalPrice(Double totalPrice) { this.totalPrice = totalPrice; }
}