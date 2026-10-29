package com.designstudio.api;

import com.designstudio.application.dto.AnalyticsDTO;
import com.designstudio.domain.entity.Order;
import com.designstudio.domain.entity.OrderItem;
import com.designstudio.domain.entity.User;
import com.designstudio.domain.entity.Visit;
import com.designstudio.domain.repository.*;
import com.designstudio.infrastructure.security.CustomUserDetails;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/analytics")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5500", "http://localhost:5501", "http://127.0.0.1:5500", "http://127.0.0.1:5501"}, allowCredentials = "true")
public class AnalyticsController {

    private final VisitRepository visitRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final DesignOrderRepository designOrderRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    public AnalyticsController(VisitRepository visitRepository,
                               OrderRepository orderRepository,
                               OrderItemRepository orderItemRepository,
                               DesignOrderRepository designOrderRepository,
                               UserRepository userRepository,
                               ProductRepository productRepository) {
        this.visitRepository = visitRepository;
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.designOrderRepository = designOrderRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
    }

    @GetMapping("/user")
    public ResponseEntity<AnalyticsDTO> getUserAnalytics(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        if (userDetails == null) {
            return ResponseEntity.status(401).body(new AnalyticsDTO());
        }

        User user = userDetails.getUser();
        AnalyticsDTO dto = new AnalyticsDTO();

        List<Order> orders = orderRepository.findByUser(user);
        dto.setTotalOrders(orders.size());

        double totalSpent = orders.stream()
                .mapToDouble(this::calculateOrderTotal)
                .sum();
        dto.setTotalSpent(totalSpent);

        dto.setAvgCheck(orders.isEmpty() ? 0 : totalSpent / orders.size());
        dto.setClientType(determineClientType(user, totalSpent));

        Map<String, Double> salesByMonth = new LinkedHashMap<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM");

        for (Order order : orders) {
            String month = order.getCreatedAt().format(formatter);
            double total = calculateOrderTotal(order);
            salesByMonth.merge(month, total, Double::sum);
        }
        dto.setSalesByMonth(salesByMonth);

        List<Visit> visits = visitRepository.findByUser(user);
        dto.setVisitsCount(visits.size());
        int totalDuration = visits.stream().mapToInt(Visit::getDurationSeconds).sum();
        dto.setTotalVisitDuration(totalDuration);

        Map<String, Long> categoryCount = new HashMap<>();
        for (Order order : orders) {
            List<OrderItem> items = orderItemRepository.findByOrder(order);
            for (OrderItem item : items) {
                if (item.getProduct().getCategory() != null) {
                    String catName = item.getProduct().getCategory().getName();
                    categoryCount.merge(catName, 1L, Long::sum);
                }
            }
        }
        List<String> trends = categoryCount.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(3)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
        dto.setTrends(trends);

        return ResponseEntity.ok(dto);
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AnalyticsDTO> getAdminAnalytics() {
        AnalyticsDTO dto = new AnalyticsDTO();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM");

        // Общая статистика
        List<User> allUsers = userRepository.findAll().stream()
                .filter(u -> u.getRole() == User.Role.USER)
                .collect(Collectors.toList());
        List<Order> allOrders = orderRepository.findAll();

        double totalRevenue = allOrders.stream()
                .mapToDouble(this::calculateOrderTotal)
                .sum();

        dto.setGlobalAvgCheck(allOrders.isEmpty() ? 0 : totalRevenue / allOrders.size());
        dto.setTotalUsers((long) allUsers.size());
        dto.setTotalProducts(productRepository.count());
        dto.setTotalOrdersToday(allOrders.stream()
                .filter(o -> o.getCreatedAt().toLocalDate().equals(LocalDate.now()))
                .count());

        // ИСПРАВЛЕНО: Конверсия - процент пользователей, совершивших покупку
        // Считаем только зарегистрированных пользователей
        long totalUsers = allUsers.size();
        long usersWithOrders = allOrders.stream()
                .filter(o -> o.getUser() != null)
                .map(o -> o.getUser().getId())
                .distinct()
                .count();

        double conversionRate = totalUsers == 0 ? 0 : (double) usersWithOrders / totalUsers * 100;
        dto.setConversionRate(Math.round(conversionRate * 100) / 100.0);

        // Среднее количество посещений на пользователя
        long totalVisits = visitRepository.count();
        dto.setAvgVisitsPerUser(totalUsers == 0 ? 0 : (double) totalVisits / totalUsers);

        // Частота покупок - среднее количество заказов на пользователя, совершившего покупку
        double purchaseFrequency = usersWithOrders == 0 ? 0 : (double) allOrders.size() / usersWithOrders;
        dto.setPurchaseFrequency(Math.round(purchaseFrequency * 100) / 100.0);

        // Среднее время на сайте (в минутах)
        int totalDuration = visitRepository.findAll().stream()
                .filter(v -> v.getDurationSeconds() > 0)
                .mapToInt(Visit::getDurationSeconds)
                .sum();
        long visitsWithDuration = visitRepository.findAll().stream()
                .filter(v -> v.getDurationSeconds() > 0)
                .count();

        double avgTimeOnSiteSeconds = visitsWithDuration == 0 ? 0 : (double) totalDuration / visitsWithDuration;
        double avgTimeOnSiteMinutes = avgTimeOnSiteSeconds / 60;
        dto.setAvgTimeOnSite(Math.round(avgTimeOnSiteMinutes * 10) / 10.0);

        // Топ клиенты (ТОП 3)
        Map<Long, Double> userSpending = new HashMap<>();
        Map<Long, Integer> userOrderCount = new HashMap<>();

        for (Order order : allOrders) {
            if (order.getUser() != null) {
                double total = calculateOrderTotal(order);
                userSpending.merge(order.getUser().getId(), total, Double::sum);
                userOrderCount.merge(order.getUser().getId(), 1, Integer::sum);
            }
        }

        List<AnalyticsDTO.TopCustomerDTO> topCustomers = userSpending.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(3)
                .map(e -> {
                    User u = userRepository.findById(e.getKey()).orElse(null);
                    String name = u != null ? u.getFirstName() + " " + u.getLastName() : "Unknown";
                    int orderCount = userOrderCount.getOrDefault(e.getKey(), 0);
                    return new AnalyticsDTO.TopCustomerDTO(e.getKey(), name, e.getValue(), orderCount);
                })
                .collect(Collectors.toList());
        dto.setTopCustomers(topCustomers);

        // Посещения по месяцам
        Map<String, Long> visitsByMonth = new LinkedHashMap<>();
        for (Visit visit : visitRepository.findAll()) {
            String month = visit.getVisitTime().format(formatter);
            visitsByMonth.merge(month, 1L, Long::sum);
        }
        dto.setVisitsByMonth(visitsByMonth);

        // Продажи по месяцам
        Map<String, Double> salesByMonth = new LinkedHashMap<>();
        for (Order order : allOrders) {
            String month = order.getCreatedAt().format(formatter);
            double total = calculateOrderTotal(order);
            salesByMonth.merge(month, total, Double::sum);
        }
        dto.setAdminSalesByMonth(salesByMonth);

        // Отладочный вывод
        System.out.println("=== ANALYTICS DEBUG ===");
        System.out.println("Total Users (USER role): " + totalUsers);
        System.out.println("Users with orders: " + usersWithOrders);
        System.out.println("Total Orders: " + allOrders.size());
        System.out.println("Conversion Rate: " + conversionRate + "%");
        System.out.println("Purchase Frequency: " + purchaseFrequency + " orders per customer");
        System.out.println("======================");

        return ResponseEntity.ok(dto);
    }

    @GetMapping("/purchase-frequency")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Double>> getPurchaseFrequency(
            @RequestParam String startDate,
            @RequestParam String endDate) {

        LocalDate start = LocalDate.parse(startDate);
        LocalDate end = LocalDate.parse(endDate);

        long visits = visitRepository.findAll().stream()
                .filter(v -> {
                    LocalDate d = v.getVisitTime().toLocalDate();
                    return !d.isBefore(start) && !d.isAfter(end);
                })
                .count();

        long orders = orderRepository.findAll().stream()
                .filter(o -> {
                    LocalDate d = o.getCreatedAt().toLocalDate();
                    return !d.isBefore(start) && !d.isAfter(end);
                })
                .count();

        double frequency = visits == 0 ? 0 : (double) orders / visits;

        Map<String, Double> result = new HashMap<>();
        result.put("frequency", frequency);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/visits-by-day")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Long>> getVisitsByDay(
            @RequestParam String startDate,
            @RequestParam String endDate) {

        LocalDate start = LocalDate.parse(startDate);
        LocalDate end = LocalDate.parse(endDate);

        Map<String, Long> result = new LinkedHashMap<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        for (Visit visit : visitRepository.findAll()) {
            LocalDate visitDate = visit.getVisitTime().toLocalDate();
            if (!visitDate.isBefore(start) && !visitDate.isAfter(end)) {
                String day = visitDate.format(formatter);
                result.merge(day, 1L, Long::sum);
            }
        }

        return ResponseEntity.ok(result);
    }

    @GetMapping("/sales-by-day")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Double>> getSalesByDay(
            @RequestParam String startDate,
            @RequestParam String endDate) {

        LocalDate start = LocalDate.parse(startDate);
        LocalDate end = LocalDate.parse(endDate);

        Map<String, Double> result = new LinkedHashMap<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        for (Order order : orderRepository.findAll()) {
            LocalDate orderDate = order.getCreatedAt().toLocalDate();
            if (!orderDate.isBefore(start) && !orderDate.isAfter(end)) {
                String day = orderDate.format(formatter);
                double total = calculateOrderTotal(order);
                result.merge(day, total, Double::sum);
            }
        }

        return ResponseEntity.ok(result);
    }

    @PostMapping("/visit/update")
    public ResponseEntity<Void> updateVisitDuration(
            @RequestBody Map<String, Integer> body,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        if (userDetails == null) {
            return ResponseEntity.ok().build();
        }

        Integer duration = body.get("durationSeconds");
        if (duration == null) {
            return ResponseEntity.ok().build();
        }

        User user = userDetails.getUser();
        Visit visit = visitRepository.findTopByUserOrderByVisitTimeDesc(user);
        if (visit != null) {
            visit.setDurationSeconds(duration);
            visitRepository.save(visit);
        }

        return ResponseEntity.ok().build();
    }

    private double calculateOrderTotal(Order order) {
        List<OrderItem> items = orderItemRepository.findByOrder(order);
        return items.stream()
                .mapToDouble(i -> i.getProduct().getPrice() * i.getQuantity())
                .sum();
    }

    private String determineClientType(User user, double totalSpent) {
        int orders = orderRepository.findByUser(user).size();
        if (orders >= 10 || totalSpent > 10000) return "VIP";
        if (orders >= 5 || totalSpent > 5000) return "PREMIUM";
        return "BASIC";
    }
}