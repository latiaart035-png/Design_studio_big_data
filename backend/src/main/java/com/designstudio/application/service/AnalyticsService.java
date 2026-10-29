package com.designstudio.application.service;

import com.designstudio.domain.entity.*;
import com.designstudio.domain.repository.*;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AnalyticsService {

    private final VisitRepository visitRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final DesignOrderRepository designOrderRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    public AnalyticsService(VisitRepository visitRepository,
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

    public double getUserDiscount(User user) {
        return user != null ? user.getDiscount() : 0.0;
    }

    public Map<String, Object> getDetailedUserAnalytics(Long userId) {
        Map<String, Object> analytics = new HashMap<>();
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return analytics;

        List<Order> orders = orderRepository.findByUser(user);
        double totalSpent = orders.stream()
                .mapToDouble(this::calculateOrderTotal)
                .sum();

        analytics.put("totalOrders", orders.size());
        analytics.put("totalSpent", totalSpent);
        analytics.put("avgCheck", orders.isEmpty() ? 0 : totalSpent / orders.size());
        analytics.put("clientType", determineClientType(user, totalSpent));

        Map<String, Double> salesByMonth = new LinkedHashMap<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM");
        for (Order order : orders) {
            String month = order.getCreatedAt().format(formatter);
            double total = calculateOrderTotal(order);
            salesByMonth.merge(month, total, Double::sum);
        }
        analytics.put("salesByMonth", salesByMonth);

        List<Visit> visits = visitRepository.findByUser(user);
        analytics.put("visitsCount", visits.size());
        int totalDuration = visits.stream().mapToInt(Visit::getDurationSeconds).sum();
        analytics.put("totalVisitDuration", totalDuration);

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
        analytics.put("trends", trends);

        return analytics;
    }

    public Map<String, Object> getAdminAnalytics() {
        Map<String, Object> analytics = new HashMap<>();
        List<User> allUsers = userRepository.findAll();
        List<Order> allOrders = orderRepository.findAll();

        double totalRevenue = allOrders.stream()
                .mapToDouble(this::calculateOrderTotal)
                .sum();

        analytics.put("avgCheck", allOrders.isEmpty() ? 0 : totalRevenue / allOrders.size());
        analytics.put("totalUsers", allUsers.size());
        analytics.put("totalProducts", productRepository.count());

        long totalVisits = visitRepository.count();
        long uniqueVisitors = visitRepository.findAll().stream()
                .filter(v -> v.getUser() != null)
                .map(v -> v.getUser().getId())
                .distinct()
                .count();
        analytics.put("conversion", totalVisits == 0 ? 0 : (double) allOrders.size() / totalVisits * 100);
        analytics.put("avgVisitsPerUser", uniqueVisitors == 0 ? 0 : (double) totalVisits / uniqueVisitors);

        Map<Long, Double> userSpending = new HashMap<>();
        for (Order order : allOrders) {
            if (order.getUser() != null) {
                double total = calculateOrderTotal(order);
                userSpending.merge(order.getUser().getId(), total, Double::sum);
            }
        }

        List<Map<String, Object>> topCustomers = userSpending.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(5)
                .map(e -> {
                    Map<String, Object> map = new HashMap<>();
                    User u = userRepository.findById(e.getKey()).orElse(null);
                    map.put("userId", e.getKey());
                    map.put("userName", u != null ? u.getFirstName() + " " + u.getLastName() : "Unknown");
                    map.put("totalSpent", e.getValue());
                    return map;
                })
                .collect(Collectors.toList());
        analytics.put("topCustomers", topCustomers);

        Map<String, Long> visitsByMonth = new LinkedHashMap<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM");
        for (Visit visit : visitRepository.findAll()) {
            String month = visit.getVisitTime().format(formatter);
            visitsByMonth.merge(month, 1L, Long::sum);
        }
        analytics.put("visitsByMonth", visitsByMonth);

        Map<String, Double> salesByMonth = new LinkedHashMap<>();
        for (Order order : allOrders) {
            String month = order.getCreatedAt().format(formatter);
            double total = calculateOrderTotal(order);
            salesByMonth.merge(month, total, Double::sum);
        }
        analytics.put("salesByMonth", salesByMonth);

        return analytics;
    }

    public Map<String, Double> getAdminVisitsByDay(String startDate, String endDate) {
        Map<String, Double> result = new LinkedHashMap<>();
        LocalDate start = LocalDate.parse(startDate);
        LocalDate end = LocalDate.parse(endDate);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        for (Visit visit : visitRepository.findAll()) {
            LocalDate visitDate = visit.getVisitTime().toLocalDate();
            if (!visitDate.isBefore(start) && !visitDate.isAfter(end)) {
                String day = visitDate.format(formatter);
                result.merge(day, 1.0, Double::sum);
            }
        }
        return result;
    }

    public Map<String, Double> getAdminSalesByDay(String startDate, String endDate) {
        Map<String, Double> result = new LinkedHashMap<>();
        LocalDate start = LocalDate.parse(startDate);
        LocalDate end = LocalDate.parse(endDate);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        for (Order order : orderRepository.findAll()) {
            LocalDate orderDate = order.getCreatedAt().toLocalDate();
            if (!orderDate.isBefore(start) && !orderDate.isAfter(end)) {
                String day = orderDate.format(formatter);
                double total = calculateOrderTotal(order);
                result.merge(day, total, Double::sum);
            }
        }
        return result;
    }

    public double getPurchaseFrequency(String startDate, String endDate) {
        if (startDate == null || endDate == null) {
            long totalVisits = visitRepository.count();
            long totalOrders = orderRepository.count();
            return totalVisits == 0 ? 0 : (double) totalOrders / totalVisits;
        }

        LocalDate start = LocalDate.parse(startDate);
        LocalDate end = LocalDate.parse(endDate);

        long visitsInPeriod = visitRepository.findAll().stream()
                .filter(v -> {
                    LocalDate d = v.getVisitTime().toLocalDate();
                    return !d.isBefore(start) && !d.isAfter(end);
                })
                .count();

        long ordersInPeriod = orderRepository.findAll().stream()
                .filter(o -> {
                    LocalDate d = o.getCreatedAt().toLocalDate();
                    return !d.isBefore(start) && !d.isAfter(end);
                })
                .count();

        return visitsInPeriod == 0 ? 0 : (double) ordersInPeriod / visitsInPeriod;
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