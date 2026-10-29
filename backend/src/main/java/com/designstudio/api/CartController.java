package com.designstudio.api;

import com.designstudio.application.dto.CartItemDTO;
import com.designstudio.application.service.UserCampaignService;
import com.designstudio.domain.entity.Cart;
import com.designstudio.domain.entity.Product;
import com.designstudio.domain.entity.User;
import com.designstudio.domain.repository.CartRepository;
import com.designstudio.domain.repository.ProductRepository;
import com.designstudio.infrastructure.security.CustomUserDetails;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/cart")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5500", "http://localhost:5501", "http://127.0.0.1:5500", "http://127.0.0.1:5501"}, allowCredentials = "true")
public class CartController {

    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final UserCampaignService userCampaignService;

    public CartController(CartRepository cartRepository,
                          ProductRepository productRepository,
                          UserCampaignService userCampaignService) {
        this.cartRepository = cartRepository;
        this.productRepository = productRepository;
        this.userCampaignService = userCampaignService;
    }

    @GetMapping
    public ResponseEntity<List<CartItemDTO>> getCart(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        System.out.println("=== getCart called ===");
        if (userDetails == null) {
            System.out.println("UserDetails is null");
            return ResponseEntity.status(401).body(List.of());
        }

        User user = userDetails.getUser();
        System.out.println("User: " + user.getEmail());

        List<Cart> cartItems = cartRepository.findByUser(user);
        System.out.println("Found " + cartItems.size() + " items");

        List<CartItemDTO> dtos = cartItems.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/discount")
    public ResponseEntity<Map<String, Object>> getCartDiscount(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        if (userDetails == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Необходимо авторизоваться"));
        }

        User user = userDetails.getUser();

        double userDiscount = user.getDiscount() != null ? user.getDiscount() : 0.0;
        double campaignDiscount = userCampaignService.getUserDiscountFromCampaigns(user.getEmail());
        double totalDiscount = Math.max(userDiscount, campaignDiscount);

        Map<String, Object> response = new HashMap<>();
        response.put("userDiscount", userDiscount);
        response.put("campaignDiscount", campaignDiscount);
        response.put("totalDiscount", totalDiscount);
        response.put("discountPercent", totalDiscount * 100);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/add")
    public ResponseEntity<Map<String, String>> addToCart(
            @RequestParam Long productId,
            @RequestParam(defaultValue = "1") int quantity,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        System.out.println("=== addToCart called ===");
        System.out.println("Product ID: " + productId + ", Quantity: " + quantity);

        if (userDetails == null) {
            System.out.println("UserDetails is null");
            return ResponseEntity.status(401).body(Map.of("error", "Необходимо авторизоваться"));
        }

        User user = userDetails.getUser();
        System.out.println("User: " + user.getEmail());

        Product product = productRepository.findById(productId).orElse(null);
        if (product == null) {
            System.out.println("Product not found: " + productId);
            return ResponseEntity.badRequest().body(Map.of("error", "Товар не найден"));
        }

        Cart existing = cartRepository.findByUserAndProduct(user, product);
        if (existing != null) {
            existing.setQuantity(existing.getQuantity() + quantity);
            cartRepository.save(existing);
            System.out.println("Updated existing cart item, new quantity: " + existing.getQuantity());
        } else {
            Cart cart = new Cart();
            cart.setUser(user);
            cart.setProduct(product);
            cart.setQuantity(quantity);
            cartRepository.save(cart);
            System.out.println("Created new cart item");
        }

        return ResponseEntity.ok(Map.of("message", "Добавлено в корзину"));
    }

    @PutMapping("/update/{cartId}")
    public ResponseEntity<Map<String, String>> updateQuantity(
            @PathVariable Long cartId,
            @RequestParam int quantity,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        System.out.println("=== updateQuantity called ===");
        System.out.println("Cart ID: " + cartId + ", New quantity: " + quantity);

        if (userDetails == null) {
            System.out.println("UserDetails is null");
            return ResponseEntity.status(401).body(Map.of("error", "Необходимо авторизоваться"));
        }

        Cart cart = cartRepository.findById(cartId).orElse(null);
        if (cart == null) {
            System.out.println("Cart item not found: " + cartId);
            return ResponseEntity.notFound().build();
        }

        // ✅ ПРОВЕРКА: корзина принадлежит текущему пользователю
        if (!cart.getUser().getId().equals(userDetails.getUser().getId())) {
            System.out.println("Cart belongs to different user!");
            return ResponseEntity.status(403).body(Map.of("error", "Доступ запрещён"));
        }

        if (quantity <= 0) {
            cartRepository.delete(cart);
            System.out.println("Cart item deleted (quantity <= 0)");
            return ResponseEntity.ok(Map.of("message", "Товар удален из корзины"));
        }

        cart.setQuantity(quantity);
        cartRepository.save(cart);
        System.out.println("Cart item updated");
        return ResponseEntity.ok(Map.of("message", "Количество обновлено"));
    }

    @DeleteMapping("/remove/{cartId}")
    public ResponseEntity<Map<String, String>> removeFromCart(
            @PathVariable Long cartId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        System.out.println("=== removeFromCart called ===");
        System.out.println("Cart ID: " + cartId);

        if (userDetails == null) {
            System.out.println("UserDetails is null");
            return ResponseEntity.status(401).body(Map.of("error", "Необходимо авторизоваться"));
        }

        Cart cart = cartRepository.findById(cartId).orElse(null);
        if (cart == null) {
            System.out.println("Cart item not found: " + cartId);
            return ResponseEntity.notFound().build();
        }

        // ✅ ПРОВЕРКА: корзина принадлежит текущему пользователю
        if (!cart.getUser().getId().equals(userDetails.getUser().getId())) {
            System.out.println("Cart belongs to different user!");
            return ResponseEntity.status(403).body(Map.of("error", "Доступ запрещён"));
        }

        cartRepository.delete(cart);
        System.out.println("Cart item deleted");
        return ResponseEntity.ok(Map.of("message", "Удалено из корзины"));
    }

    @DeleteMapping("/clear")
    public ResponseEntity<Map<String, String>> clearCart(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        if (userDetails == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Необходимо авторизоваться"));
        }

        User user = userDetails.getUser();
        List<Cart> items = cartRepository.findByUser(user);
        cartRepository.deleteAll(items);
        return ResponseEntity.ok(Map.of("message", "Корзина очищена"));
    }

    @GetMapping("/count")
    public ResponseEntity<Map<String, Object>> getCartCount(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        if (userDetails == null) {
            return ResponseEntity.ok(Map.of("count", 0));
        }

        User user = userDetails.getUser();
        int count = cartRepository.findByUser(user).stream()
                .mapToInt(Cart::getQuantity)
                .sum();

        return ResponseEntity.ok(Map.of("count", count));
    }

    private CartItemDTO convertToDTO(Cart cart) {
        Product product = cart.getProduct();

        String imageUrl = product.getMainImage();
        if (imageUrl != null && !imageUrl.startsWith("/")) {
            imageUrl = "/uploads/" + imageUrl;
        } else if (imageUrl == null) {
            imageUrl = "";
        }

        return new CartItemDTO(
                cart.getId(),
                product.getId(),
                product.getName(),
                imageUrl,
                product.getPrice(),
                cart.getQuantity()
        );
    }
}