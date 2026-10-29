package com.designstudio.application.service;

import com.designstudio.domain.entity.Cart;
import com.designstudio.domain.entity.Product;
import com.designstudio.domain.entity.User;
import com.designstudio.domain.repository.CartRepository;
import com.designstudio.domain.repository.ProductRepository;
import com.designstudio.domain.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CartService {

    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    public CartService(CartRepository cartRepository,
                       ProductRepository productRepository,
                       UserRepository userRepository) {
        this.cartRepository = cartRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
    }

    public List<Cart> getCart(Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return List.of();
        return cartRepository.findByUser(user);
    }

    public void addToCart(Long userId, Long productId, int quantity) {
        User user = userRepository.findById(userId).orElse(null);
        Product product = productRepository.findById(productId).orElse(null);
        if (user == null || product == null) return;

        Cart existing = cartRepository.findByUserAndProduct(user, product);
        if (existing != null) {
            existing.setQuantity(existing.getQuantity() + quantity);
            cartRepository.save(existing);
        } else {
            Cart cart = new Cart();
            cart.setUser(user);
            cart.setProduct(product);
            cart.setQuantity(quantity);
            cartRepository.save(cart);
        }
    }

    public void updateQuantity(Long cartId, int quantity) {
        Cart cart = cartRepository.findById(cartId).orElse(null);
        if (cart == null) return;

        if (quantity <= 0) {
            cartRepository.delete(cart);
        } else {
            cart.setQuantity(quantity);
            cartRepository.save(cart);
        }
    }

    public void removeFromCart(Long cartId) {
        cartRepository.deleteById(cartId);
    }

    public void clearCart(Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return;

        List<Cart> items = cartRepository.findByUser(user);
        cartRepository.deleteAll(items);
    }

    public double getTotalPrice(Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return 0.0;

        List<Cart> items = cartRepository.findByUser(user);
        return items.stream()
                .mapToDouble(c -> c.getProduct().getPrice() * c.getQuantity())
                .sum();
    }
}