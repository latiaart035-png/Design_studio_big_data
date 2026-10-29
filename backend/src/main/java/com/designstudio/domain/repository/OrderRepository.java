package com.designstudio.domain.repository;

import com.designstudio.domain.entity.Order;
import com.designstudio.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    // ===== БАЗОВЫЕ МЕТОДЫ =====

    List<Order> findByUser(User user);

    List<Order> findByUserId(Long userId);

    List<Order> findByStatus(Order.OrderStatus status);

    // ===== МЕТОДЫ С total_amount =====

    // Найти заказы по сумме (больше указанной)
    List<Order> findByTotalAmountGreaterThan(Double amount);

    // Найти заказы по сумме (меньше указанной)
    List<Order> findByTotalAmountLessThan(Double amount);

    // Найти заказы в диапазоне сумм
    List<Order> findByTotalAmountBetween(Double minAmount, Double maxAmount);

    // ===== АГРЕГАТНЫЕ ЗАПРОСЫ =====

    // Общая выручка (все заказы)
    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o")
    Double getTotalRevenue();

    // Общая выручка по статусу
    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.status = :status")
    Double getTotalRevenueByStatus(@Param("status") Order.OrderStatus status);

    // Выручка за период
    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.createdAt BETWEEN :start AND :end")
    Double getRevenueBetweenDates(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    // Средний чек
    @Query("SELECT COALESCE(AVG(o.totalAmount), 0) FROM Order o")
    Double getAverageOrderValue();

    // Средний чек по клиенту
    @Query("SELECT COALESCE(AVG(o.totalAmount), 0) FROM Order o WHERE o.user.id = :userId")
    Double getAverageOrderValueByUser(@Param("userId") Long userId);

    // ===== СТАТИСТИКА ПО КЛИЕНТАМ =====

    // Сумма заказов по клиенту
    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.user.id = :userId")
    Double getTotalSpentByUser(@Param("userId") Long userId);

    // Количество заказов по клиенту
    @Query("SELECT COUNT(o) FROM Order o WHERE o.user.id = :userId")
    Long getOrderCountByUser(@Param("userId") Long userId);

    // ===== ТОП ЗАКАЗЫ =====

    // Топ-N заказов по сумме
    List<Order> findTop5ByOrderByTotalAmountDesc();

    // ===== ПРОВЕРКИ =====

    // Проверка наличия заказов у клиента
    boolean existsByUserId(Long userId);

    // Проверка наличия заказов с суммой больше указанной
    boolean existsByTotalAmountGreaterThan(Double amount);
}