package com.designstudio.application.dto;

public class CartItemDTO {
    private Long cartId;
    private Long productId;
    private String productName;
    private String productImage;
    private String productDescription;
    private Double productPrice;
    private Integer quantity;
    private Double totalPrice;

    public CartItemDTO() {}

    public CartItemDTO(Long cartId, Long productId, String productName,
                       String productImage, Double productPrice, Integer quantity) {
        this.cartId = cartId;
        this.productId = productId;
        this.productName = productName;
        this.productImage = productImage;
        this.productPrice = productPrice;
        this.quantity = quantity;
        this.totalPrice = productPrice * quantity;
    }

    public Long getCartId() { return cartId; }
    public void setCartId(Long cartId) { this.cartId = cartId; }
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public String getProductImage() { return productImage; }
    public void setProductImage(String productImage) { this.productImage = productImage; }
    public String getProductDescription() { return productDescription; }
    public void setProductDescription(String productDescription) { this.productDescription = productDescription; }
    public Double getProductPrice() { return productPrice; }
    public void setProductPrice(Double productPrice) { this.productPrice = productPrice; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public Double getTotalPrice() { return totalPrice; }
    public void setTotalPrice(Double totalPrice) { this.totalPrice = totalPrice; }
}