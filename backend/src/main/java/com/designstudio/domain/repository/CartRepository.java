package com.designstudio.domain.repository;

import com.designstudio.domain.entity.Cart;
import com.designstudio.domain.entity.Product;
import com.designstudio.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CartRepository extends JpaRepository<Cart, Long> {
    List<Cart> findByUser(User user);
    Cart findByUserAndProduct(User user, Product product);
}