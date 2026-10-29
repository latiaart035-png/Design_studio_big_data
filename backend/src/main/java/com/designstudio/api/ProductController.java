package com.designstudio.api;

import com.designstudio.application.dto.ProductDTO;
import com.designstudio.application.service.AnalyticsService;
import com.designstudio.domain.entity.Product;
import com.designstudio.domain.entity.ProductCategory;
import com.designstudio.domain.entity.User;
import com.designstudio.domain.repository.ProductCategoryRepository;
import com.designstudio.domain.repository.ProductRepository;
import com.designstudio.infrastructure.security.CustomUserDetails;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/products")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5500", "http://localhost:5501", "http://127.0.0.1:5500", "http://127.0.0.1:5501"}, allowCredentials = "true")
public class ProductController {

    private final ProductRepository productRepository;
    private final ProductCategoryRepository categoryRepository;
    private final AnalyticsService analyticsService;

    public ProductController(ProductRepository productRepository,
                             ProductCategoryRepository categoryRepository,
                             AnalyticsService analyticsService) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.analyticsService = analyticsService;
    }

    @GetMapping
    public ResponseEntity<List<ProductDTO>> getAllProducts(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String categoryName,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        List<Product> products;

        System.out.println("=== FILTER PARAMETERS ===");
        System.out.println("search: " + search);
        System.out.println("category: " + category);
        System.out.println("categoryName: " + categoryName);
        System.out.println("categoryId: " + categoryId);
        System.out.println("minPrice: " + minPrice);
        System.out.println("maxPrice: " + maxPrice);

        // Приоритет фильтров:
        // 1. Поиск по названию
        if (search != null && !search.isEmpty()) {
            products = productRepository.findByNameContainingIgnoreCase(search);
            System.out.println("Filtering by search: " + search + ", found: " + products.size());
        }
        // 2. Фильтр по ID категории (предпочтительный способ)
        else if (categoryId != null) {
            products = productRepository.findByCategoryId(categoryId);
            System.out.println("Filtering by categoryId: " + categoryId + ", found: " + products.size());
        }
        // 3. Фильтр по названию категории (из catalog.js)
        else if (categoryName != null && !categoryName.isEmpty()) {
            // Ищем категорию по имени - ИСПРАВЛЕНО: используем Optional
            java.util.Optional<ProductCategory> optionalCategory = categoryRepository.findByName(categoryName);
            if (optionalCategory.isPresent()) {
                ProductCategory foundCategory = optionalCategory.get();
                products = productRepository.findByCategoryId(foundCategory.getId());
                System.out.println("Filtering by categoryName: " + categoryName + " -> ID: " + foundCategory.getId() + ", found: " + products.size());
            } else {
                products = productRepository.findAll();
                System.out.println("Category not found: " + categoryName + ", returning all products");
            }
        }
        // 4. Фильтр по старому параметру category (для обратной совместимости)
        else if (category != null && !category.isEmpty()) {
            // Пробуем интерпретировать как ID
            try {
                Long catId = Long.parseLong(category);
                products = productRepository.findByCategoryId(catId);
                System.out.println("Filtering by category (as ID): " + catId + ", found: " + products.size());
            } catch (NumberFormatException e) {
                // Если не число, пробуем как название - ИСПРАВЛЕНО: используем Optional
                java.util.Optional<ProductCategory> optionalCategory = categoryRepository.findByName(category);
                if (optionalCategory.isPresent()) {
                    ProductCategory foundCategory = optionalCategory.get();
                    products = productRepository.findByCategoryId(foundCategory.getId());
                    System.out.println("Filtering by category (as name): " + category + " -> ID: " + foundCategory.getId() + ", found: " + products.size());
                } else {
                    products = productRepository.findAll();
                    System.out.println("Invalid category parameter, returning all products");
                }
            }
        }
        // 5. Фильтр по цене
        else if (minPrice != null && maxPrice != null) {
            products = productRepository.findByPriceBetween(minPrice, maxPrice);
            System.out.println("Filtering by price between: " + minPrice + " - " + maxPrice + ", found: " + products.size());
        }
        // 6. Все товары
        else {
            products = productRepository.findAll();
            System.out.println("No filters, returning all products: " + products.size());
        }

        User user = userDetails != null ? userDetails.getUser() : null;

        List<ProductDTO> dtos = products.stream()
                .map(p -> convertToDTO(p, user))
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductDTO> getProduct(@PathVariable Long id,
                                                 @AuthenticationPrincipal CustomUserDetails userDetails) {
        Product product = productRepository.findById(id).orElse(null);
        if (product == null) {
            return ResponseEntity.notFound().build();
        }

        User user = userDetails != null ? userDetails.getUser() : null;
        return ResponseEntity.ok(convertToDTO(product, user));
    }

    @GetMapping("/recent")
    public ResponseEntity<List<ProductDTO>> getRecentProducts(
            @RequestParam(defaultValue = "10") int limit,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        List<Product> allProducts = productRepository.findAllOrderByCreatedAtDesc();

        List<Product> products = allProducts.size() > limit
                ? allProducts.subList(0, limit)
                : allProducts;

        User user = userDetails != null ? userDetails.getUser() : null;

        List<ProductDTO> dtos = products.stream()
                .map(p -> convertToDTO(p, user))
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/latest")
    public ResponseEntity<List<ProductDTO>> getLatestProducts(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        List<Product> products = productRepository.findTop8ByOrderByIdDesc();
        User user = userDetails != null ? userDetails.getUser() : null;

        List<ProductDTO> dtos = products.stream()
                .map(p -> convertToDTO(p, user))
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/categories")
    public ResponseEntity<List<ProductCategory>> getCategories() {
        return ResponseEntity.ok(categoryRepository.findAll());
    }

    private ProductDTO convertToDTO(Product product, User user) {
        ProductDTO dto = new ProductDTO();
        dto.setId(product.getId());
        dto.setName(product.getName());
        dto.setDescription(product.getDescription());
        dto.setPrice(product.getPrice());
        dto.setCategoryName(product.getCategory() != null ? product.getCategory().getName() : "");
        dto.setCategoryId(product.getCategory() != null ? product.getCategory().getId() : null);

        String imageUrl = product.getMainImage();
        if (imageUrl != null && !imageUrl.startsWith("/") && !imageUrl.isEmpty()) {
            imageUrl = "/uploads/" + imageUrl;
        } else if (imageUrl == null || imageUrl.isEmpty()) {
            imageUrl = "/uploads/no-image.png";
        }
        dto.setMainImageUrl(imageUrl);

        List<String> additionalImages = new ArrayList<>();
        if (product.getAdditionalImage1() != null && !product.getAdditionalImage1().isEmpty()) {
            additionalImages.add("/uploads/" + product.getAdditionalImage1());
        }
        if (product.getAdditionalImage2() != null && !product.getAdditionalImage2().isEmpty()) {
            additionalImages.add("/uploads/" + product.getAdditionalImage2());
        }
        if (product.getAdditionalImage3() != null && !product.getAdditionalImage3().isEmpty()) {
            additionalImages.add("/uploads/" + product.getAdditionalImage3());
        }
        dto.setAdditionalImages(additionalImages);

        if (user != null) {
            double discount = analyticsService.getUserDiscount(user);
            dto.setDiscountedPrice(product.getPrice() * (1 - discount));
            dto.setHasDiscount(discount > 0);
        } else {
            dto.setDiscountedPrice(product.getPrice());
            dto.setHasDiscount(false);
        }

        return dto;
    }
}