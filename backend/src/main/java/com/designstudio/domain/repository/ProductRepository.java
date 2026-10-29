package com.designstudio.domain.repository;

import com.designstudio.domain.entity.Product;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByNameContainingIgnoreCase(String name);

    List<Product> findByCategoryId(Long categoryId);

    List<Product> findByPriceBetween(Double minPrice, Double maxPrice);

    List<Product> findTop8ByOrderByIdDesc();

    // ==================== ИСПРАВЛЕННЫЙ МЕТОД ====================
    // Используем @Query для явной сортировки по createdAt
    @Query("SELECT p FROM Product p WHERE p.createdAt IS NOT NULL ORDER BY p.createdAt DESC")
    List<Product> findRecentProducts(Pageable pageable);

    // Альтернативный метод без Pageable (для простоты)
    @Query("SELECT p FROM Product p WHERE p.createdAt IS NOT NULL ORDER BY p.createdAt DESC")
    List<Product> findAllOrderByCreatedAtDesc();
    // ==================== КОНЕЦ ИСПРАВЛЕНИЯ ====================
}