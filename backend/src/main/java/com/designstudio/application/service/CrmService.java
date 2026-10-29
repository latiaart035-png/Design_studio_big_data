package com.designstudio.application.service;

import com.designstudio.application.dto.CrmDTO;
import com.designstudio.domain.entity.CustomerSegment;
import com.designstudio.domain.entity.Order;
import com.designstudio.domain.entity.User;
import com.designstudio.domain.entity.Visit;
import com.designstudio.domain.repository.CustomerSegmentRepository;
import com.designstudio.domain.repository.OrderRepository;
import com.designstudio.domain.repository.UserRepository;
import com.designstudio.domain.repository.VisitRepository;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CrmService {
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final CustomerSegmentRepository segmentRepository;
    private final VisitRepository visitRepository;

    public CrmService(UserRepository userRepository, OrderRepository orderRepository,
                      CustomerSegmentRepository segmentRepository, VisitRepository visitRepository) {
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
        this.segmentRepository = segmentRepository;
        this.visitRepository = visitRepository;
    }

    public CrmDTO.CrmStatsDTO getCrmStats() {
        CrmDTO.CrmStatsDTO stats = new CrmDTO.CrmStatsDTO();

        List<User> allUsers = userRepository.findAll().stream()
                .filter(u -> u.getRole() == User.Role.USER)  // ← уже отфильтровано
                .collect(Collectors.toList());

        List<Order> allOrders = orderRepository.findAll();
        List<Visit> allVisits = visitRepository.findAll();

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime today = now.toLocalDate().atStartOfDay();

        LocalDateTime last7Days = today.minusDays(7);
        LocalDateTime prev7Days = today.minusDays(14);

        LocalDateTime monthStart = today.withDayOfMonth(1);
        LocalDateTime prevMonthStart = monthStart.minusMonths(1);
        LocalDateTime nextMonthStart = monthStart.plusMonths(1);

        System.out.println("=== CRM STATS DEBUG ===");
        System.out.println("Now: " + now);
        System.out.println("Today: " + today);
        System.out.println("Month start: " + monthStart);
        System.out.println("Users: " + allUsers.size() + ", Orders: " + allOrders.size() + ", Visits: " + allVisits.size());

        debugUserSegments(allUsers);

        // ========== БАЗОВЫЕ МЕТРИКИ ==========

        stats.setTotalCustomers((long) allUsers.size());
        stats.setTotalOrders((long) allOrders.size());

        stats.setNewCustomersToday(allUsers.stream()
                .filter(u -> u.getCreatedAt() != null &&
                        !u.getCreatedAt().isBefore(today) &&
                        u.getCreatedAt().isBefore(today.plusDays(1)))
                .count());

        long newThisWeek = allUsers.stream()
                .filter(u -> u.getCreatedAt() != null && !u.getCreatedAt().isBefore(last7Days))
                .count();
        stats.setNewCustomersThisWeek(newThisWeek);

        long newThisMonth = allUsers.stream()
                .filter(u -> u.getCreatedAt() != null && !u.getCreatedAt().isBefore(monthStart))
                .count();
        stats.setNewCustomersThisMonth(newThisMonth);

        // ========== ДОХОДЫ ==========

        double totalRevenue = allOrders.stream()
                .mapToDouble(this::getOrderTotal)
                .sum();
        stats.setTotalRevenue(totalRevenue);

        double avgOrderValue = allOrders.isEmpty() ? 0 : totalRevenue / allOrders.size();
        stats.setAvgOrderValue(avgOrderValue);

        double avgCustomerValue = allUsers.isEmpty() ? 0 : totalRevenue / allUsers.size();
        stats.setAvgCustomerValue(avgCustomerValue);

        // ========== ТРЕНДЫ ==========

        long currentWeekNew = newThisWeek;
        long prevWeekNew = allUsers.stream()
                .filter(u -> u.getCreatedAt() != null &&
                        u.getCreatedAt().isAfter(prev7Days) &&
                        u.getCreatedAt().isBefore(last7Days))
                .count();

        Double customersChange = calculateChangePercent(currentWeekNew, prevWeekNew);
        stats.setCustomersChangePercent(customersChange);

        double currentMonthRevenue = allOrders.stream()
                .filter(o -> o.getCreatedAt() != null &&
                        !o.getCreatedAt().isBefore(monthStart) &&
                        o.getCreatedAt().isBefore(nextMonthStart))
                .mapToDouble(this::getOrderTotal)
                .sum();

        double prevMonthRevenue = allOrders.stream()
                .filter(o -> o.getCreatedAt() != null &&
                        o.getCreatedAt().isAfter(prevMonthStart) &&
                        o.getCreatedAt().isBefore(monthStart))
                .mapToDouble(this::getOrderTotal)
                .sum();

        Double revenueChange = calculateChangePercent(currentMonthRevenue, prevMonthRevenue);
        stats.setRevenueChangePercent(revenueChange);

        long currentMonthOrders = allOrders.stream()
                .filter(o -> o.getCreatedAt() != null &&
                        !o.getCreatedAt().isBefore(monthStart) &&
                        o.getCreatedAt().isBefore(nextMonthStart))
                .count();

        long prevMonthOrders = allOrders.stream()
                .filter(o -> o.getCreatedAt() != null &&
                        o.getCreatedAt().isAfter(prevMonthStart) &&
                        o.getCreatedAt().isBefore(monthStart))
                .count();

        Double ordersChange = calculateChangePercent(currentMonthOrders, prevMonthOrders);
        stats.setOrdersChangePercent(ordersChange);

        double currentAvgCheck = currentMonthOrders > 0 ? currentMonthRevenue / currentMonthOrders : 0;
        double prevAvgCheck = prevMonthOrders > 0 ? prevMonthRevenue / prevMonthOrders : 0;

        Double avgCheckChange = calculateChangePercent(currentAvgCheck, prevAvgCheck);
        stats.setAvgCheckChangePercent(avgCheckChange);

        // ========== КОНВЕРСИЯ ==========

        long totalVisits = allVisits.size();
        double conversionRate = totalVisits > 0 ? (double) allOrders.size() / totalVisits * 100 : 0;
        stats.setConversionRate(round(conversionRate, 2));

        long currentMonthVisits = allVisits.stream()
                .filter(v -> v.getVisitTime() != null &&
                        !v.getVisitTime().isBefore(monthStart) &&
                        v.getVisitTime().isBefore(nextMonthStart))
                .count();

        long prevMonthVisits = allVisits.stream()
                .filter(v -> v.getVisitTime() != null &&
                        v.getVisitTime().isAfter(prevMonthStart) &&
                        v.getVisitTime().isBefore(monthStart))
                .count();

        double currentConv = currentMonthOrders > 0 && currentMonthVisits > 0
                ? (double) currentMonthOrders / currentMonthVisits * 100 : 0;
        double prevConv = prevMonthOrders > 0 && prevMonthVisits > 0
                ? (double) prevMonthOrders / prevMonthVisits * 100 : 0;

        Double convChange = calculateChangePercent(currentConv, prevConv);
        stats.setConversionChangePercent(convChange);

        stats.setTotalVisits(totalVisits);
        stats.setRetentionChangePercent(0.0);
        stats.setRetentionRate(round(calculateRetentionRate(allUsers), 2));
        stats.setChurnRate(round(calculateChurnRate(allUsers), 2));
        stats.setActiveUsersLast30Days(countActiveUsersLast30Days());

        // ========== ДАННЫЕ ПО МЕСЯЦАМ ==========

        Map<String, Double> salesByMonth = new LinkedHashMap<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM");
        for (Order order : allOrders) {
            if (order.getCreatedAt() != null && order.getTotalAmount() != null && order.getTotalAmount() > 0) {
                String month = order.getCreatedAt().format(formatter);
                salesByMonth.merge(month, getOrderTotal(order), Double::sum);
            }
        }
        stats.setAdminSalesByMonth(salesByMonth);

        System.out.println("=== MONTHLY SALES DEBUG ===");
        for (Map.Entry<String, Double> entry : salesByMonth.entrySet()) {
            System.out.println(entry.getKey() + ": " + entry.getValue() + " BYN");
        }
        System.out.println("===========================");

        // ========== СЕГМЕНТЫ ==========

        Map<String, Integer> customersBySegment = new HashMap<>();
        Map<String, Double> revenueBySegment = new HashMap<>();

        for (User user : allUsers) {
            String segment = determineUserSegment(user);
            customersBySegment.merge(segment, 1, Integer::sum);
            double userSpent = orderRepository.findByUser(user).stream()
                    .mapToDouble(this::getOrderTotal)
                    .sum();
            revenueBySegment.merge(segment, userSpent, Double::sum);
        }

        stats.setCustomersBySegment(customersBySegment);
        stats.setRevenueBySegment(revenueBySegment);

        // ========== РАСПРЕДЕЛЕНИЕ СЕГМЕНТОВ ДЛЯ КРУГОВОЙ ДИАГРАММЫ ==========
        // Отображаются ТОЛЬКО VIP, PREMIUM и BASIC
        // ВАЖНО: учитываем только пользователей с ролью USER
        List<CrmDTO.SegmentDistributionDTO> segmentDistribution = new ArrayList<>();

// 1. VIP - только USER
        CrmDTO.SegmentDistributionDTO vipDist = new CrmDTO.SegmentDistributionDTO();
        vipDist.setSegment("VIP");
        int vipCount = (int) allUsers.stream()
                .filter(u -> "VIP".equals(determineUserSegment(u)))
                .count();
        double vipRevenue = allUsers.stream()
                .filter(u -> "VIP".equals(determineUserSegment(u)))
                .mapToDouble(u -> orderRepository.findByUser(u).stream()
                        .mapToDouble(this::getOrderTotal).sum())
                .sum();
        vipDist.setCount(vipCount);
        vipDist.setTotalRevenue(vipRevenue);
        vipDist.setAvgCheck(vipCount > 0 ? vipRevenue / vipCount : 0);
        segmentDistribution.add(vipDist);

// 2. PREMIUM - только USER
        CrmDTO.SegmentDistributionDTO premiumDist = new CrmDTO.SegmentDistributionDTO();
        premiumDist.setSegment("PREMIUM");
        int premiumCount = (int) allUsers.stream()
                .filter(u -> "PREMIUM".equals(determineUserSegment(u)))
                .count();
        double premiumRevenue = allUsers.stream()
                .filter(u -> "PREMIUM".equals(determineUserSegment(u)))
                .mapToDouble(u -> orderRepository.findByUser(u).stream()
                        .mapToDouble(this::getOrderTotal).sum())
                .sum();
        premiumDist.setCount(premiumCount);
        premiumDist.setTotalRevenue(premiumRevenue);
        premiumDist.setAvgCheck(premiumCount > 0 ? premiumRevenue / premiumCount : 0);
        segmentDistribution.add(premiumDist);

// 3. BASIC - ВСЕ пользователи со статусом BASIC в БД (только USER)
        CrmDTO.SegmentDistributionDTO basicDist = new CrmDTO.SegmentDistributionDTO();
        basicDist.setSegment("BASIC");
        int basicCount = (int) allUsers.stream()
                .filter(u -> u.getStatus() == User.Status.BASIC || u.getStatus() == User.Status.NEW)
                .count();
        double basicRevenue = allUsers.stream()
                .filter(u -> u.getStatus() == User.Status.BASIC || u.getStatus() == User.Status.NEW)
                .mapToDouble(u -> orderRepository.findByUser(u).stream()
                        .mapToDouble(this::getOrderTotal).sum())
                .sum();
        basicDist.setCount(basicCount);
        basicDist.setTotalRevenue(basicRevenue);
        basicDist.setAvgCheck(basicCount > 0 ? basicRevenue / basicCount : 0);
        segmentDistribution.add(basicDist);

        stats.setSegmentDistribution(segmentDistribution);

        // ========== СТАНДАРТНЫЕ СЕГМЕНТЫ ДЛЯ КАРТОЧЕК ==========
        Map<String, CrmDTO.SegmentStatsDTO> segmentStats = new HashMap<>();

        // VIP
        CrmDTO.SegmentStatsDTO vipStats = new CrmDTO.SegmentStatsDTO();
        vipStats.setCount(vipCount);
        vipStats.setTotalRevenue(vipRevenue);
        vipStats.setAvgCheck(vipCount > 0 ? vipRevenue / vipCount : 0);
        segmentStats.put("VIP", vipStats);

        // PREMIUM
        CrmDTO.SegmentStatsDTO premiumStats = new CrmDTO.SegmentStatsDTO();
        premiumStats.setCount(premiumCount);
        premiumStats.setTotalRevenue(premiumRevenue);
        premiumStats.setAvgCheck(premiumCount > 0 ? premiumRevenue / premiumCount : 0);
        segmentStats.put("PREMIUM", premiumStats);

        // BASIC
        CrmDTO.SegmentStatsDTO basicStats = new CrmDTO.SegmentStatsDTO();
        basicStats.setCount(basicCount);
        basicStats.setTotalRevenue(basicRevenue);
        basicStats.setAvgCheck(basicCount > 0 ? basicRevenue / basicCount : 0);
        segmentStats.put("BASIC", basicStats);

        // ACTIVE - для фильтрации
        long activeBasicCount = allUsers.stream()
                .filter(u -> (u.getStatus() == User.Status.BASIC || u.getStatus() == User.Status.NEW))
                .filter(u -> "ACTIVE".equals(determineActivityStatus(u)))
                .count();
        double activeBasicRevenue = allUsers.stream()
                .filter(u -> (u.getStatus() == User.Status.BASIC || u.getStatus() == User.Status.NEW))
                .filter(u -> "ACTIVE".equals(determineActivityStatus(u)))
                .mapToDouble(u -> orderRepository.findByUser(u).stream()
                        .mapToDouble(this::getOrderTotal).sum())
                .sum();

        CrmDTO.SegmentStatsDTO activeStats = new CrmDTO.SegmentStatsDTO();
        activeStats.setCount((int) activeBasicCount);
        activeStats.setTotalRevenue(activeBasicRevenue);
        activeStats.setAvgCheck(activeBasicCount > 0 ? activeBasicRevenue / activeBasicCount : 0);
        segmentStats.put("ACTIVE", activeStats);

        // LOST - для фильтрации
        long lostBasicCount = allUsers.stream()
                .filter(u -> (u.getStatus() == User.Status.BASIC || u.getStatus() == User.Status.NEW))
                .filter(u -> "LOST".equals(determineActivityStatus(u)))
                .count();
        double lostBasicRevenue = allUsers.stream()
                .filter(u -> (u.getStatus() == User.Status.BASIC || u.getStatus() == User.Status.NEW))
                .filter(u -> "LOST".equals(determineActivityStatus(u)))
                .mapToDouble(u -> orderRepository.findByUser(u).stream()
                        .mapToDouble(this::getOrderTotal).sum())
                .sum();

        CrmDTO.SegmentStatsDTO lostStats = new CrmDTO.SegmentStatsDTO();
        lostStats.setCount((int) lostBasicCount);
        lostStats.setTotalRevenue(lostBasicRevenue);
        lostStats.setAvgCheck(lostBasicCount > 0 ? lostBasicRevenue / lostBasicCount : 0);
        segmentStats.put("LOST", lostStats);

        stats.setSegmentStats(segmentStats);

        stats.setRfmAnalysis(performRfmAnalysis(allUsers));
        stats.setAbcAnalysis(performAbcAnalysis(allUsers));

        System.out.println("=== SEGMENTS ===");
        System.out.println("Customers by segment: " + customersBySegment);
        System.out.println("Segment stats: VIP=" + vipStats.getCount() + ", PREMIUM=" + premiumStats.getCount() +
                ", BASIC=" + basicStats.getCount() + ", ACTIVE=" + activeStats.getCount() + ", LOST=" + lostStats.getCount());
        System.out.println("=== END CRM STATS DEBUG ===\n");

        System.out.println("=== ABC ANALYSIS INPUT ===");
        System.out.println("Users for ABC analysis: " + allUsers.size());
        for (User user : allUsers) {
            double revenue = orderRepository.findByUser(user).stream()
                    .mapToDouble(this::getOrderTotal)
                    .sum();
            System.out.println("  User: " + user.getFirstName() + " " + user.getLastName() +
                    " - Revenue: " + revenue);
        }

        return stats;
    }

    // ========== ОСНОВНОЙ МЕТОД ОПРЕДЕЛЕНИЯ СЕГМЕНТА ==========

    /**
     * Определение сегмента пользователя для CRM
     *
     * VIP: статус VIP ИЛИ траты >= 50000 ИЛИ заказов > 20
     * PREMIUM: статус PREMIUM ИЛИ траты >= 20000 ИЛИ заказов > 10
     * BASIC: ВСЕ пользователи со статусом BASIC в БД (для отображения в отдельном блоке)
     */
    private String determineUserSegment(User user) {
        // Защита от администраторов
        if (user.getRole() != User.Role.USER) {
            return "ADMIN";
        }

        List<Order> orders = orderRepository.findByUser(user);

        double totalSpent = orders.stream().mapToDouble(this::getOrderTotal).sum();
        int orderCount = orders.size();

        // 1. VIP сегмент
        if (user.getStatus() == User.Status.VIP) return "VIP";
        if (totalSpent >= 50000 || orderCount > 20) return "VIP";

        // 2. PREMIUM сегмент
        if (user.getStatus() == User.Status.PREMIUM) return "PREMIUM";
        if (totalSpent >= 20000 || orderCount > 10) return "PREMIUM";

        // 3. Для пользователей со статусом BASIC или NEW
        if (user.getStatus() == User.Status.BASIC || user.getStatus() == User.Status.NEW) {
            return "BASIC";
        }

        // 4. Для других статусов
        if (user.getStatus() == User.Status.ACTIVE) return "ACTIVE";
        if (user.getStatus() == User.Status.LOST) return "LOST";

        return "BASIC";
    }

    /**
     * Определение статуса активности для BASIC пользователей
     * Используется для фильтрации в CRM и отображения подкатегорий
     */
    public String determineActivityStatus(User user) {
        List<Order> orders = orderRepository.findByUser(user);
        LocalDateTime now = LocalDateTime.now();

        // Только для пользователей со статусом BASIC или NEW
        if (user.getStatus() != User.Status.BASIC && user.getStatus() != User.Status.NEW) {
            return null;
        }

        if (orders.isEmpty()) {
            return "BASIC_NO_ORDERS";
        }

        Order lastOrder = orders.stream()
                .max(Comparator.comparing(Order::getCreatedAt))
                .orElse(null);

        if (lastOrder != null && lastOrder.getCreatedAt() != null) {
            long daysSinceLastOrder = Duration.between(lastOrder.getCreatedAt(), now).toDays();

            if (daysSinceLastOrder <= 30) {
                return "ACTIVE";
            }

            if (daysSinceLastOrder > 30) {
                return "LOST";
            }
        }

        return "BASIC";
    }

    private void debugUserSegments(List<User> users) {
        System.out.println("\n=== DETAILED USER SEGMENT DEBUG ===");
        LocalDateTime now = LocalDateTime.now();

        int basicInDb = 0;
        int activeActivity = 0;
        int lostActivity = 0;
        int noOrders = 0;

        for (User user : users) {
            List<Order> orders = orderRepository.findByUser(user);
            double totalSpent = orders.stream().mapToDouble(this::getOrderTotal).sum();
            int orderCount = orders.size();

            Order lastOrder = orders.stream()
                    .max(Comparator.comparing(Order::getCreatedAt))
                    .orElse(null);

            long daysSinceLastOrder = -1;
            String lastOrderDate = "нет заказов";
            if (lastOrder != null && lastOrder.getCreatedAt() != null) {
                daysSinceLastOrder = Duration.between(lastOrder.getCreatedAt(), now).toDays();
                lastOrderDate = lastOrder.getCreatedAt().toString();
            }

            String segment = determineUserSegment(user);
            String activityStatus = (user.getStatus() == User.Status.BASIC || user.getStatus() == User.Status.NEW)
                    ? determineActivityStatus(user) : null;

            if (user.getStatus() == User.Status.BASIC || user.getStatus() == User.Status.NEW) {
                basicInDb++;
                if (orders.isEmpty()) noOrders++;
                if ("ACTIVE".equals(activityStatus)) activeActivity++;
                if ("LOST".equals(activityStatus)) lostActivity++;
            }

            System.out.printf("┌─────────────────────────────────────────────────────────────%n");
            System.out.printf("│ User: %s %s (ID: %d)%n",
                    user.getFirstName(), user.getLastName(), user.getId());
            System.out.printf("│ Статус в БД: %s → Сегмент CRM: %s%n", user.getStatus(), segment);
            if (activityStatus != null) {
                System.out.printf("│ Активность (для BASIC): %s%n", activityStatus);
            }
            System.out.printf("│ Заказов: %d | Потрачено: %.2f BYN%n", orderCount, totalSpent);
            System.out.printf("│ Последний заказ: %s | Дней прошло: %d%n", lastOrderDate, daysSinceLastOrder);
            System.out.printf("└─────────────────────────────────────────────────────────────%n");
        }

        System.out.println("\n=== BASIC USERS STATISTICS ===");
        System.out.printf("Всего пользователей со статусом BASIC в БД: %d%n", basicInDb);
        System.out.printf("  - Активные (заказ <30 дней): %d → показываются в ACTIVE при фильтрации%n", activeActivity);
        System.out.printf("  - Неактивные (заказ >30 дней): %d → показываются в LOST при фильтрации%n", lostActivity);
        System.out.printf("  - Без заказов: %d → только в BASIC%n", noOrders);
        System.out.println("=== END DETAILED DEBUG ===\n");
    }

    // ========== ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ==========

    private Double calculateChangePercent(double current, double previous) {
        if (current == 0 && previous == 0) {
            return 0.0;
        }
        if (previous == 0) {
            return null;
        }
        if (current == 0) {
            return -100.0;
        }
        double change = ((current - previous) / previous) * 100;
        return round(change, 1);
    }

    private Double calculateChangePercent(long current, long previous) {
        return calculateChangePercent((double) current, (double) previous);
    }

    private double round(double value, int places) {
        if (Double.isNaN(value) || Double.isInfinite(value)) return 0.0;
        double factor = Math.pow(10, places);
        return Math.round(value * factor) / factor;
    }

    private double getOrderTotal(Order order) {
        if (order.getTotalAmount() != null && order.getTotalAmount() > 0) {
            return order.getTotalAmount();
        }
        return 0.0;
    }

    private double calculateRetentionRate(List<User> allUsers) {
        long returningUsers = allUsers.stream()
                .filter(u -> orderRepository.findByUser(u).size() >= 2)
                .count();
        return allUsers.isEmpty() ? 0 : (double) returningUsers / allUsers.size() * 100;
    }

    private double calculateChurnRate(List<User> allUsers) {
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        long churnedUsers = allUsers.stream()
                .filter(u -> {
                    List<Order> orders = orderRepository.findByUser(u);
                    if (orders.isEmpty()) return true;
                    Order lastOrder = orders.stream()
                            .max(Comparator.comparing(Order::getCreatedAt))
                            .orElse(null);
                    if (lastOrder == null) return true;
                    return lastOrder.getCreatedAt().isBefore(thirtyDaysAgo);
                })
                .count();
        return allUsers.isEmpty() ? 0 : (double) churnedUsers / allUsers.size() * 100;
    }

    private int countActiveUsersLast30Days() {
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        return (int) orderRepository.findAll().stream()
                .filter(o -> o.getCreatedAt() != null && !o.getCreatedAt().isBefore(thirtyDaysAgo))
                .map(Order::getUser)
                .filter(Objects::nonNull)
                .distinct()
                .count();
    }

    // ========== RFM АНАЛИЗ ==========

    public List<CrmDTO.RfmAnalysisDTO> performRfmAnalysis(List<User> users) {
        List<CrmDTO.RfmAnalysisDTO> result = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        Map<String, List<User>> rfmGroups = new HashMap<>();

        for (User user : users) {
            List<Order> orders = orderRepository.findByUser(user);
            if (orders.isEmpty()) continue;

            Order lastOrder = orders.stream()
                    .max(Comparator.comparing(Order::getCreatedAt))
                    .orElse(null);

            int recencyDays = lastOrder != null
                    ? (int) Duration.between(lastOrder.getCreatedAt(), now).toDays()
                    : 999;
            int frequency = orders.size();
            double monetary = orders.stream()
                    .mapToDouble(this::getOrderTotal)
                    .sum();

            String rfmSegment;
            if (recencyDays <= 30 && frequency >= 3 && monetary >= 10000) {
                rfmSegment = "Champions";
            } else if (recencyDays <= 30 && frequency >= 2) {
                rfmSegment = "Loyal";
            } else if (recencyDays <= 60 && frequency >= 2) {
                rfmSegment = "Potential";
            } else if (recencyDays <= 90) {
                rfmSegment = "At Risk";
            } else if (recencyDays <= 180) {
                rfmSegment = "Dormant";
            } else {
                rfmSegment = "Lost";
            }

            rfmGroups.computeIfAbsent(rfmSegment, k -> new ArrayList<>()).add(user);
        }

        for (Map.Entry<String, List<User>> entry : rfmGroups.entrySet()) {
            CrmDTO.RfmAnalysisDTO dto = new CrmDTO.RfmAnalysisDTO();
            dto.setSegment(entry.getKey());
            dto.setCustomerCount(entry.getValue().size());

            double totalRev = entry.getValue().stream()
                    .mapToDouble(u -> orderRepository.findByUser(u).stream()
                            .mapToDouble(this::getOrderTotal)
                            .sum())
                    .sum();

            dto.setTotalRevenue(totalRev);
            dto.setAvgRevenue(entry.getValue().isEmpty() ? 0 : totalRev / entry.getValue().size());
            dto.setRecommendedAction(getRecommendedAction(entry.getKey()));
            dto.setDescription(getSegmentDescription(entry.getKey()));
            result.add(dto);
        }

        return result;
    }

    // ========== ABC АНАЛИЗ ==========

    public List<CrmDTO.AbcAnalysisDTO> performAbcAnalysis(List<User> users) {
        List<CrmDTO.AbcAnalysisDTO> result = new ArrayList<>();

        // Фильтруем только пользователей с ролью USER и с положительной выручкой
        List<Map.Entry<User, Double>> userRevenue = new ArrayList<>();

        for (User user : users) {
            // Проверяем роль USER
            if (user.getRole() != User.Role.USER) {
                continue;
            }

            double revenue = orderRepository.findByUser(user).stream()
                    .mapToDouble(this::getOrderTotal)
                    .sum();
            if (revenue > 0) {
                userRevenue.add(Map.entry(user, revenue));
            }
        }

        if (userRevenue.isEmpty()) {
            // Нет данных - возвращаем пустые группы
            CrmDTO.AbcAnalysisDTO a = new CrmDTO.AbcAnalysisDTO();
            a.setSegment("Группа A");
            a.setCustomerCount(0);
            a.setRevenue(0.0);
            a.setRevenuePercent(0.0);
            a.setCumulativePercent(0.0);
            a.setDescription("Нет клиентов с выручкой");
            result.add(a);

            CrmDTO.AbcAnalysisDTO b = new CrmDTO.AbcAnalysisDTO();
            b.setSegment("Группа B");
            b.setCustomerCount(0);
            b.setRevenue(0.0);
            b.setRevenuePercent(0.0);
            b.setCumulativePercent(0.0);
            b.setDescription("Нет клиентов с выручкой");
            result.add(b);

            CrmDTO.AbcAnalysisDTO c = new CrmDTO.AbcAnalysisDTO();
            c.setSegment("Группа C");
            c.setCustomerCount(0);
            c.setRevenue(0.0);
            c.setRevenuePercent(0.0);
            c.setCumulativePercent(100.0);
            c.setDescription("Нет клиентов с выручкой");
            result.add(c);

            return result;
        }

        // Сортируем по убыванию выручки
        userRevenue.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));

        double totalRevenue = userRevenue.stream()
                .mapToDouble(Map.Entry::getValue)
                .sum();

        System.out.println("=== ABC ANALYSIS DEBUG ===");
        System.out.println("Total users with revenue: " + userRevenue.size());
        System.out.println("Total revenue: " + totalRevenue);

        int totalUsers = userRevenue.size();
        int aCount = 0, bCount = 0, cCount = 0;
        double aRevenue = 0, bRevenue = 0, cRevenue = 0;
        double cumulativeRevenue = 0;

        for (int i = 0; i < userRevenue.size(); i++) {
            double rev = userRevenue.get(i).getValue();
            cumulativeRevenue += rev;
            double cumulativePercent = (cumulativeRevenue / totalRevenue) * 100;

            // Группа A: первые 20% клиентов ИЛИ пока не накопили 80% выручки
            if (aCount < Math.ceil(totalUsers * 0.2) && cumulativePercent <= 80) {
                aCount++;
                aRevenue += rev;
            }
            // Группа B: следующие 30% клиентов ИЛИ пока не накопили 95% выручки
            else if (bCount < Math.ceil(totalUsers * 0.3) && cumulativePercent <= 95) {
                bCount++;
                bRevenue += rev;
            }
            // Группа C: остальные
            else {
                cCount++;
                cRevenue += rev;
            }
        }

        System.out.println("Group A: " + aCount + " customers, revenue: " + aRevenue + " (" + (aRevenue/totalRevenue*100) + "%)");
        System.out.println("Group B: " + bCount + " customers, revenue: " + bRevenue + " (" + (bRevenue/totalRevenue*100) + "%)");
        System.out.println("Group C: " + cCount + " customers, revenue: " + cRevenue + " (" + (cRevenue/totalRevenue*100) + "%)");
        System.out.println("==========================");

        // Группа A
        if (aCount > 0) {
            CrmDTO.AbcAnalysisDTO a = new CrmDTO.AbcAnalysisDTO();
            a.setSegment("Группа A");
            a.setCustomerCount(aCount);
            a.setRevenue(aRevenue);
            a.setRevenuePercent(aRevenue / totalRevenue * 100);
            a.setCumulativePercent(aRevenue / totalRevenue * 100);
            a.setDescription("Ключевые клиенты — " + aCount + " чел. (" +
                    String.format("%.1f", (double) aCount / totalUsers * 100) + "% клиентов) приносят " +
                    String.format("%.1f", aRevenue / totalRevenue * 100) + "% выручки");
            result.add(a);
        }

        // Группа B
        if (bCount > 0) {
            CrmDTO.AbcAnalysisDTO b = new CrmDTO.AbcAnalysisDTO();
            b.setSegment("Группа B");
            b.setCustomerCount(bCount);
            b.setRevenue(bRevenue);
            b.setRevenuePercent(bRevenue / totalRevenue * 100);
            b.setCumulativePercent((aRevenue + bRevenue) / totalRevenue * 100);
            b.setDescription("Перспективные клиенты — " + bCount + " чел. (" +
                    String.format("%.1f", (double) bCount / totalUsers * 100) + "% клиентов) приносят " +
                    String.format("%.1f", bRevenue / totalRevenue * 100) + "% выручки");
            result.add(b);
        }

        // Группа C
        if (cCount > 0) {
            CrmDTO.AbcAnalysisDTO c = new CrmDTO.AbcAnalysisDTO();
            c.setSegment("Группа C");
            c.setCustomerCount(cCount);
            c.setRevenue(cRevenue);
            c.setRevenuePercent(cRevenue / totalRevenue * 100);
            c.setCumulativePercent(100.0);
            c.setDescription("Массовые клиенты — " + cCount + " чел. (" +
                    String.format("%.1f", (double) cCount / totalUsers * 100) + "% клиентов) приносят " +
                    String.format("%.1f", cRevenue / totalRevenue * 100) + "% выручки");
            result.add(c);
        }

        return result;
    }

    // ========== ПОЛУЧИТЬ ВСЕХ КЛИЕНТОВ ==========

    public List<CrmDTO.CustomerDTO> getAllCustomers() {
        List<User> users = userRepository.findAll().stream()
                .filter(u -> u.getRole() == User.Role.USER)
                .collect(Collectors.toList());
        return users.stream()
                .map(this::convertToCustomerDto)
                .collect(Collectors.toList());
    }

    public List<CrmDTO.CustomerDTO> getCustomersBySegment(String segmentName) {
        List<User> users = userRepository.findAll().stream()
                .filter(u -> u.getRole() == User.Role.USER)
                .collect(Collectors.toList());

        return users.stream()
                .filter(u -> {
                    if (segmentName.equals("ACTIVE")) {
                        // ACTIVE - BASIC пользователи с активностью в последние 30 дней
                        return (u.getStatus() == User.Status.BASIC || u.getStatus() == User.Status.NEW)
                                && "ACTIVE".equals(determineActivityStatus(u));
                    } else if (segmentName.equals("LOST")) {
                        // LOST - BASIC пользователи без активности >30 дней
                        return (u.getStatus() == User.Status.BASIC || u.getStatus() == User.Status.NEW)
                                && "LOST".equals(determineActivityStatus(u));
                    } else {
                        // Для VIP, PREMIUM, BASIC - используем стандартное определение
                        return segmentName.equals(determineUserSegment(u));
                    }
                })
                .map(this::convertToCustomerDto)
                .collect(Collectors.toList());
    }

    // ========== КОНВЕРТАЦИЯ User -> CustomerDTO ==========

    public CrmDTO.CustomerDTO convertToCustomerDto(User user) {
        CrmDTO.CustomerDTO dto = new CrmDTO.CustomerDTO();
        dto.setId(user.getId());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setEmail(user.getEmail());
        dto.setStatus(user.getStatus() != null ? user.getStatus().name() : "BASIC");
        dto.setSegment(determineUserSegment(user));

        List<Order> orders = orderRepository.findByUser(user);
        dto.setTotalOrders(orders.size());

        double totalSpent = orders.stream()
                .mapToDouble(this::getOrderTotal)
                .sum();
        dto.setTotalSpent(totalSpent);
        dto.setAvgOrderValue(orders.isEmpty() ? 0 : totalSpent / orders.size());

        // Вычисляем CLV (Customer Lifetime Value)
        // CLV = Средний чек × Количество заказов
        double clv = dto.getAvgOrderValue() * dto.getTotalOrders();
        dto.setClv(clv);

        if (!orders.isEmpty()) {
            Order lastOrder = orders.stream()
                    .max(Comparator.comparing(Order::getCreatedAt))
                    .orElse(null);
            if (lastOrder != null && lastOrder.getCreatedAt() != null) {
                int daysSince = (int) Duration.between(lastOrder.getCreatedAt(), LocalDateTime.now()).toDays();
                dto.setDaysSinceLastOrder(daysSince);
                dto.setLastOrderDate(lastOrder.getCreatedAt().toLocalDate().toString());
            }
        } else {
            dto.setDaysSinceLastOrder(null);
            dto.setLastOrderDate(null);
        }

        if (user.getCreatedAt() != null) {
            dto.setDaysSinceRegistration((int) Duration.between(user.getCreatedAt(), LocalDateTime.now()).toDays());
            dto.setRegistrationDate(user.getCreatedAt().toLocalDate().toString());
        }

        dto.setRfmSegment(determineRfmSegment(user));
        dto.setPurchasedCategories(new ArrayList<>());
        return dto;
    }

    private String determineRfmSegment(User user) {
        List<Order> orders = orderRepository.findByUser(user);
        if (orders.isEmpty()) return "New";

        LocalDateTime now = LocalDateTime.now();
        Order lastOrder = orders.stream()
                .max(Comparator.comparing(Order::getCreatedAt))
                .orElse(null);

        int recencyDays = lastOrder != null
                ? (int) Duration.between(lastOrder.getCreatedAt(), now).toDays()
                : 999;
        int frequency = orders.size();
        double monetary = orders.stream()
                .mapToDouble(this::getOrderTotal)
                .sum();

        if (recencyDays <= 30 && frequency >= 3 && monetary >= 10000) return "Champions";
        if (recencyDays <= 30 && frequency >= 2) return "Loyal";
        if (recencyDays <= 60 && frequency >= 2) return "Potential";
        if (recencyDays <= 90) return "At Risk";
        if (recencyDays <= 180) return "Dormant";
        return "Lost";
    }

    // ========== CRUD СЕГМЕНТОВ ==========

    public List<CrmDTO.SegmentDTO> getAllSegments() {
        List<CustomerSegment> segments = segmentRepository.findAll();
        // Фильтруем только пользователей с ролью USER
        List<User> allUsers = userRepository.findAll().stream()
                .filter(u -> u.getRole() == User.Role.USER)
                .collect(Collectors.toList());
        return segments.stream()
                .map(seg -> convertToSegmentDto(seg, allUsers))
                .collect(Collectors.toList());
    }

    public CustomerSegment createSegment(CustomerSegment segment) {
        return segmentRepository.save(segment);
    }

    public CustomerSegment updateSegment(Long id, CustomerSegment updated) {
        CustomerSegment existing = segmentRepository.findById(id).orElse(null);
        if (existing == null) return null;
        existing.setName(updated.getName());
        existing.setDescription(updated.getDescription());
        existing.setMinOrders(updated.getMinOrders());
        existing.setMaxOrders(updated.getMaxOrders());
        existing.setMinSpent(updated.getMinSpent());
        existing.setMaxSpent(updated.getMaxSpent());
        existing.setStatusRequirement(updated.getStatusRequirement());
        existing.setActivityDays(updated.getActivityDays());
        existing.setColor(updated.getColor());
        existing.setIcon(updated.getIcon());
        return segmentRepository.save(existing);
    }

    public boolean deleteSegment(Long id) {
        if (segmentRepository.existsById(id)) {
            segmentRepository.deleteById(id);
            return true;
        }
        return false;
    }

    private CrmDTO.SegmentDTO convertToSegmentDto(CustomerSegment segment, List<User> allUsers) {
        CrmDTO.SegmentDTO dto = new CrmDTO.SegmentDTO();
        dto.setId(segment.getId());
        dto.setName(segment.getName());
        dto.setDescription(segment.getDescription());
        dto.setColor(segment.getColor());
        dto.setIcon(segment.getIcon());

        int customerCount = 0;
        double totalRevenue = 0;

        // Фильтруем только пользователей с ролью USER
        for (User user : allUsers) {
            if (user.getRole() != User.Role.USER) {
                continue; // Пропускаем администраторов
            }
            if (matchesSegmentCriteria(user, segment)) {
                customerCount++;
                totalRevenue += orderRepository.findByUser(user).stream()
                        .mapToDouble(this::getOrderTotal)
                        .sum();
            }
        }

        dto.setTotalCustomers(customerCount);
        dto.setTotalRevenue(totalRevenue);
        dto.setAvgOrderValue(customerCount > 0 ? totalRevenue / customerCount : 0);
        return dto;
    }

    private boolean matchesSegmentCriteria(User user, CustomerSegment segment) {
        // Проверяем роль пользователя
        if (user.getRole() != User.Role.USER) {
            return false;
        }

        List<Order> orders = orderRepository.findByUser(user);
        int orderCount = orders.size();
        double totalSpent = orders.stream()
                .mapToDouble(this::getOrderTotal)
                .sum();

        if (segment.getMinOrders() != null && orderCount < segment.getMinOrders()) return false;
        if (segment.getMaxOrders() != null && orderCount > segment.getMaxOrders()) return false;
        if (segment.getMinSpent() != null && totalSpent < segment.getMinSpent()) return false;
        if (segment.getMaxSpent() != null && totalSpent > segment.getMaxSpent()) return false;
        if (segment.getStatusRequirement() != null && !segment.getStatusRequirement().isEmpty()) {
            if (user.getStatus() == null || !segment.getStatusRequirement().equals(user.getStatus().name())) {
                return false;
            }
        }
        return true;
    }

    // ========== ВОРОНКА ПРОДАЖ ==========

    public CrmDTO.FunnelDTO getSalesFunnel() {
        CrmDTO.FunnelDTO funnel = new CrmDTO.FunnelDTO();

        List<Order> allOrders = orderRepository.findAll();
        List<Visit> allVisits = visitRepository.findAll();

        // Количество посещений
        long visitsCount = allVisits.size();
        funnel.setVisits(visitsCount);

        // Количество заказов
        long ordersCount = allOrders.size();
        funnel.setOrders(ordersCount);

        // Количество корзин (заказы со статусом PENDING или CART)
        long cartsCount = allOrders.stream()
                .filter(o -> o.getStatus() != null &&
                        (o.getStatus().name().equals("PENDING") ||
                                o.getStatus().name().equals("CART") ||
                                o.getStatus().name().equals("NEW")))
                .count();
        if (cartsCount == 0) {
            cartsCount = ordersCount; // Fallback
        }
        funnel.setCarts(cartsCount);

        // Количество оплаченных заказов
        long paidCount = allOrders.stream()
                .filter(o -> o.getStatus() != null &&
                        (o.getStatus().name().equals("DELIVERED") ||
                                o.getStatus().name().equals("PAID") ||
                                o.getStatus().name().equals("COMPLETED") ||
                                o.getStatus().name().equals("PROCESSED")))
                .count();
        funnel.setPaid(paidCount);

        // Рассчитываем конверсии
        double visitToCart = visitsCount > 0 ? (double) cartsCount / visitsCount * 100 : 0;
        double cartToOrder = cartsCount > 0 ? (double) ordersCount / cartsCount * 100 : 0;
        double orderToPay = ordersCount > 0 ? (double) paidCount / ordersCount * 100 : 0;
        double totalConversion = visitsCount > 0 ? (double) paidCount / visitsCount * 100 : 0;

        funnel.setVisitToCartPercent(Math.round(visitToCart * 100) / 100.0);
        funnel.setCartToOrderPercent(Math.round(cartToOrder * 100) / 100.0);
        funnel.setOrderToPayPercent(Math.round(orderToPay * 100) / 100.0);
        funnel.setTotalConversion(Math.round(totalConversion * 100) / 100.0);

        // Средний чек (по оплаченным заказам)
        double avgOrderValue = paidCount > 0 ?
                allOrders.stream()
                        .filter(o -> o.getStatus() != null &&
                                (o.getStatus().name().equals("DELIVERED") ||
                                        o.getStatus().name().equals("PAID") ||
                                        o.getStatus().name().equals("COMPLETED")))
                        .mapToDouble(this::getOrderTotal)
                        .sum() / paidCount : 0;
        funnel.setAvgOrderValue(avgOrderValue);

        System.out.println("=== SALES FUNNEL DEBUG ===");
        System.out.println("Visits: " + visitsCount);
        System.out.println("Carts: " + cartsCount);
        System.out.println("Orders: " + ordersCount);
        System.out.println("Paid: " + paidCount);
        System.out.println("Visit→Cart: " + visitToCart + "%");
        System.out.println("Cart→Order: " + cartToOrder + "%");
        System.out.println("Order→Pay: " + orderToPay + "%");
        System.out.println("Total conversion: " + totalConversion + "%");
        System.out.println("Avg order value: " + avgOrderValue);
        System.out.println("==========================");

        return funnel;
    }

    // ========== АКТИВНОСТЬ ==========

    public CrmDTO.ActivityStatsDTO getActivityStats(String period) {
        CrmDTO.ActivityStatsDTO stats = new CrmDTO.ActivityStatsDTO();

        LocalDateTime endDate = LocalDateTime.now();
        LocalDateTime startDate;

        // Определяем период
        switch (period) {
            case "week":
                startDate = endDate.minusDays(7);
                break;
            case "month":
                startDate = endDate.minusDays(30);
                break;
            case "quarter":
                startDate = endDate.minusDays(90);
                break;
            case "year":
                startDate = endDate.minusDays(365);
                break;
            default:
                startDate = endDate.minusDays(30); // месяц по умолчанию
        }

        // Получаем заказы за период
        List<Order> orders = orderRepository.findAll().stream()
                .filter(o -> o.getCreatedAt() != null && !o.getCreatedAt().isBefore(startDate))
                .collect(Collectors.toList());

        // Группируем заказы по дням
        Map<String, Long> dailyOrders = new TreeMap<>();
        DateTimeFormatter dayFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        // Инициализируем все дни периода нулями
        LocalDateTime current = startDate;
        while (!current.isAfter(endDate)) {
            String dateKey = current.format(dayFormatter);
            dailyOrders.put(dateKey, 0L);
            current = current.plusDays(1);
        }

        // Заполняем реальные значения
        for (Order order : orders) {
            if (order.getCreatedAt() != null) {
                String dateKey = order.getCreatedAt().format(dayFormatter);
                dailyOrders.merge(dateKey, 1L, Long::sum);
            }
        }

        // Вычисляем статистику
        long totalOrders = orders.size();
        long activeDays = dailyOrders.values().stream().filter(v -> v > 0).count();
        long inactiveDays = dailyOrders.size() - activeDays;
        double avgDailyOrders = activeDays > 0 ? (double) totalOrders / activeDays : 0;

        // Находим самый активный день
        String mostActiveDay = "";
        long mostActiveDayOrders = 0;
        for (Map.Entry<String, Long> entry : dailyOrders.entrySet()) {
            if (entry.getValue() > mostActiveDayOrders) {
                mostActiveDayOrders = entry.getValue();
                mostActiveDay = entry.getKey();
            }
        }

        // Группируем выручку по месяцам
        Map<String, Double> monthlyRevenue = new LinkedHashMap<>();
        DateTimeFormatter monthFormatter = DateTimeFormatter.ofPattern("yyyy-MM");

        for (Order order : orders) {
            if (order.getCreatedAt() != null && order.getTotalAmount() != null) {
                String monthKey = order.getCreatedAt().format(monthFormatter);
                monthlyRevenue.merge(monthKey, order.getTotalAmount(), Double::sum);
            }
        }

        stats.setDailyOrders(dailyOrders);
        stats.setMonthlyRevenue(monthlyRevenue);
        stats.setTotalOrders(totalOrders);
        stats.setInactiveDays(inactiveDays);
        stats.setAvgDailyOrders(Math.round(avgDailyOrders * 10) / 10.0);
        stats.setMostActiveDay(mostActiveDay);
        stats.setMostActiveDayOrders(mostActiveDayOrders);

        System.out.println("=== ACTIVITY STATS DEBUG ===");
        System.out.println("Period: " + period);
        System.out.println("Start date: " + startDate);
        System.out.println("End date: " + endDate);
        System.out.println("Total days: " + dailyOrders.size());
        System.out.println("Active days: " + activeDays);
        System.out.println("Inactive days: " + inactiveDays);
        System.out.println("Total orders: " + totalOrders);
        System.out.println("Avg daily orders: " + avgDailyOrders);
        System.out.println("Most active day: " + mostActiveDay + " (" + mostActiveDayOrders + " orders)");
        System.out.println("=============================");

        return stats;
    }

    // ========== РЕКОМЕНДАЦИИ ==========

    private String getRecommendedAction(String rfmSegment) {
        return switch (rfmSegment) {
            case "Champions" -> "Предложить VIP-программу, эксклюзивные предложения";
            case "Loyal" -> "Поддерживать лояльность, программа рекомендаций";
            case "Potential" -> "Стимулировать к увеличению частоты покупок";
            case "At Risk" -> "Отправить персонализированную акцию";
            case "Dormant" -> "Реактивационная кампания со скидкой 20%";
            case "Lost" -> "Попытка вернуть с большой скидкой 30%+";
            default -> "Стандартные маркетинговые активности";
        };
    }

    private String getSegmentDescription(String rfmSegment) {
        return switch (rfmSegment) {
            case "Champions" -> "Лучшие клиенты — высокая частота, недавние покупки, большой чек";
            case "Loyal" -> "Постоянные клиенты с регулярными покупками";
            case "Potential" -> "Клиенты с хорошим потенциалом роста";
            case "At Risk" -> "Клиенты, которые могут уйти";
            case "Dormant" -> "Спящие клиенты — давно не покупали (90-180 дней)";
            case "Lost" -> "Потерянные клиенты — более 180 дней без заказов";
            default -> "Прочие клиенты";
        };
    }
}