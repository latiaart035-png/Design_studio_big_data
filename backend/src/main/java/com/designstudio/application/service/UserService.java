package com.designstudio.application.service;

import com.designstudio.domain.entity.Order;
import com.designstudio.domain.entity.OrderItem;
import com.designstudio.domain.entity.User;
import com.designstudio.domain.repository.OrderRepository;
import com.designstudio.domain.repository.OrderItemRepository;
import com.designstudio.domain.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final PasswordEncoder passwordEncoder;

    private static final double PREMIUM_THRESHOLD = 2000.0;
    private static final double VIP_THRESHOLD = 3500.0;

    public UserService(UserRepository userRepository,
                       OrderRepository orderRepository,
                       OrderItemRepository orderItemRepository,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public void register(User user) {
        String normalizedEmail = user.getEmail().trim().toLowerCase();

        if (userRepository.findByEmail(normalizedEmail) != null) {
            throw new RuntimeException("Email already registered");
        }

        user.setEmail(normalizedEmail);
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setRole(User.Role.USER);
        user.setStatus(User.Status.NEW);
        user.setDiscount(0.0);

        user.setQuarterStartDate(getQuarterStartForDate(LocalDateTime.now()));
        user.setQuarterTotalSpent(0.0);

        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public User findByEmail(String email) {
        return userRepository.findByEmail(email.trim().toLowerCase());
    }

    @Transactional
    public void updateDiscount(Long userId, double discount) {
        userRepository.findById(userId).ifPresent(user -> {
            user.setDiscount(discount);
            userRepository.save(user);
        });
    }

    // ==================== PASSWORD CHANGE ====================

    @Transactional
    public PasswordChangeResult changePassword(Long userId, String currentPassword, String newPassword) {
        User user = userRepository.findById(userId).orElse(null);

        if (user == null) {
            return new PasswordChangeResult(false, "User not found");
        }

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            return new PasswordChangeResult(false, "Incorrect current password");
        }

        String validationError = validateNewPassword(newPassword);
        if (validationError != null) {
            return new PasswordChangeResult(false, validationError);
        }

        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            return new PasswordChangeResult(false, "New password must be different from current");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        return new PasswordChangeResult(true, "Password changed successfully");
    }

    private String validateNewPassword(String password) {
        if (password == null || password.isEmpty()) {
            return "Password cannot be empty";
        }
        if (password.length() < 3) {
            return "Password must contain at least 8 characters";
        }
        if (password.length() > 100) {
            return "Password is too long (max 100 characters)";
        }
        if (password.contains(" ")) {
            return "Password cannot contain spaces";
        }
        return null;
    }

    // ==================== QUARTERLY DISCOUNT SYSTEM ====================

    @Transactional
    public void updateQuarterlyDiscount(Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            System.out.println("[UserService] User not found for ID: " + userId);
            return;
        }

        System.out.println("[UserService] Processing user: " + user.getEmail());
        System.out.println("[UserService] Current quarterStartDate from DB: " + user.getQuarterStartDate());

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime correctQuarterStart = getQuarterStartForDate(now);

        System.out.println("[UserService] Calculated correctQuarterStart: " + correctQuarterStart);

        // Check if quarter start needs to be fixed
        boolean needsQuarterFix = false;

        if (user.getQuarterStartDate() == null) {
            needsQuarterFix = true;
            System.out.println("[UserService] quarterStartDate is NULL, fixing");
        } else {
            LocalDateTime stored = user.getQuarterStartDate();
            // Check if stored date is not the 1st day of the quarter month at midnight
            if (stored.getDayOfMonth() != 1 || stored.getHour() != 0 || stored.getMinute() != 0) {
                needsQuarterFix = true;
                System.out.println("[UserService] Stored date format is wrong: " + stored);
            }
            // Check if stored quarter is different from current
            else if (getQuarterNumber(stored) != getQuarterNumber(now)) {
                needsQuarterFix = true;
                System.out.println("[UserService] Quarter changed, fixing");
            }
        }

        // Fix quarter start if needed
        if (needsQuarterFix) {
            System.out.println("[UserService] Fixing quarter to: " + correctQuarterStart);
            user.setQuarterStartDate(correctQuarterStart);
            // Do NOT reset quarter_total_spent here - we need to recalculate it
            userRepository.save(user);
        }

        // Use the corrected quarter start
        LocalDateTime quarterStart = user.getQuarterStartDate();

        // Calculate total spending in current quarter
        double quarterSpent = calculateQuarterSpending(user, quarterStart, now);

        System.out.println("[UserService] Calculated QuarterSpent: " + quarterSpent);

        // Update quarter spent
        user.setQuarterTotalSpent(quarterSpent);

        // Determine status and discount based on thresholds
        User.Status newStatus;
        double newDiscount;

        if (quarterSpent >= VIP_THRESHOLD) {
            newStatus = User.Status.VIP;
            newDiscount = 0.15;
            System.out.println("[UserService] *** VIP reached! ***");
        } else if (quarterSpent >= PREMIUM_THRESHOLD) {
            newStatus = User.Status.PREMIUM;
            newDiscount = 0.05;
            System.out.println("[UserService] *** PREMIUM reached! ***");
        } else {
            newStatus = User.Status.BASIC;
            newDiscount = 0.0;
            System.out.println("[UserService] BASIC (threshold not reached)");
        }

        System.out.println("[UserService] New status: " + newStatus + ", discount: " + newDiscount);

        // Update status and discount
        user.setStatus(newStatus);
        user.setDiscount(newDiscount);
        userRepository.save(user);

        System.out.println("[UserService] User saved. Status: " + user.getStatus() + ", Discount: " + user.getDiscount() + ", QuarterSpent: " + user.getQuarterTotalSpent());
    }

    @Transactional(readOnly = true)
    public DiscountInfo getDiscountInfo(Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return null;

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime quarterStart = user.getQuarterStartDate() != null
                ? user.getQuarterStartDate()
                : getQuarterStartForDate(now);
        LocalDateTime quarterEnd = getQuarterEndDate(quarterStart);

        // Re-calculate quarter spent for accurate info
        double quarterSpent = calculateQuarterSpending(user, quarterStart, now);

        double remainingForPremium = quarterSpent < PREMIUM_THRESHOLD
                ? PREMIUM_THRESHOLD - quarterSpent
                : 0;
        double remainingForVip = quarterSpent < VIP_THRESHOLD
                ? VIP_THRESHOLD - quarterSpent
                : 0;

        return new DiscountInfo(
                user.getStatus().name(),
                user.getDiscount(),
                quarterSpent,
                quarterStart,
                quarterEnd,
                PREMIUM_THRESHOLD,
                VIP_THRESHOLD,
                remainingForPremium,
                remainingForVip
        );
    }

    /**
     * Calculate total spending in the quarter
     */
    private double calculateQuarterSpending(User user, LocalDateTime quarterStart, LocalDateTime now) {
        System.out.println("[UserService] calculateQuarterSpending called");
        System.out.println("[UserService] quarterStart: " + quarterStart);
        System.out.println("[UserService] now: " + now);

        // Find all orders for this user
        List<Order> userOrders = orderRepository.findByUser(user);
        System.out.println("[UserService] Found " + userOrders.size() + " orders for user");

        double total = 0.0;
        for (Order order : userOrders) {
            LocalDateTime orderDate = order.getCreatedAt();
            System.out.println("[UserService] Checking order ID: " + order.getId() + ", date: " + orderDate);

            // Check if order is in current quarter
            if (orderDate != null &&
                    !orderDate.isBefore(quarterStart) &&
                    !orderDate.isAfter(now)) {

                System.out.println("[UserService] Order " + order.getId() + " IS in current quarter");

                // Get items for this order
                List<OrderItem> items = orderItemRepository.findByOrder(order);
                System.out.println("[UserService] Found " + items.size() + " items for order " + order.getId());

                for (OrderItem item : items) {
                    if (item.getProduct() != null && item.getProduct().getPrice() != null) {
                        double itemTotal = item.getProduct().getPrice() * item.getQuantity();
                        System.out.println("[UserService] Item: price=" + item.getProduct().getPrice() +
                                ", qty=" + item.getQuantity() + ", total=" + itemTotal);
                        total += itemTotal;
                    } else {
                        System.out.println("[UserService] Item has null product or price, skipping");
                    }
                }
            } else {
                System.out.println("[UserService] Order " + order.getId() + " is NOT in current quarter");
            }
        }

        System.out.println("[UserService] Final quarterSpent total: " + total);
        return total;
    }

    /**
     * Get quarter start date (1st day of quarter month at 00:00:00)
     */
    private LocalDateTime getQuarterStartForDate(LocalDateTime date) {
        int month = date.getMonthValue();
        int year = date.getYear();

        int quarterStartMonth;
        if (month <= 3) {
            quarterStartMonth = 1;
        } else if (month <= 6) {
            quarterStartMonth = 4;
        } else if (month <= 9) {
            quarterStartMonth = 7;
        } else {
            quarterStartMonth = 10;
        }

        return LocalDateTime.of(year, quarterStartMonth, 1, 0, 0, 0);
    }

    /**
     * Get quarter number (1-4) for a date
     */
    private int getQuarterNumber(LocalDateTime date) {
        int month = date.getMonthValue();
        return (month - 1) / 3 + 1;
    }

    private LocalDateTime getQuarterEndDate(LocalDateTime quarterStart) {
        int month = quarterStart.getMonthValue();
        int year = quarterStart.getYear();

        if (month <= 3) {
            return LocalDateTime.of(year, 4, 1, 0, 0, 0);
        } else if (month <= 6) {
            return LocalDateTime.of(year, 7, 1, 0, 0, 0);
        } else if (month <= 9) {
            return LocalDateTime.of(year, 10, 1, 0, 0, 0);
        } else {
            return LocalDateTime.of(year + 1, 1, 1, 0, 0, 0);
        }
    }

    // ==================== HELPER CLASSES ====================

    public static class PasswordChangeResult {
        private final boolean success;
        private final String message;

        public PasswordChangeResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
    }

    public static class DiscountInfo {
        private final String status;
        private final Double discount;
        private final Double quarterSpent;
        private final LocalDateTime quarterStart;
        private final LocalDateTime quarterEnd;
        private final Double premiumThreshold;
        private final Double vipThreshold;
        private final Double remainingForPremium;
        private final Double remainingForVip;

        public DiscountInfo(String status, Double discount, Double quarterSpent,
                            LocalDateTime quarterStart, LocalDateTime quarterEnd,
                            Double premiumThreshold, Double vipThreshold,
                            Double remainingForPremium, Double remainingForVip) {
            this.status = status;
            this.discount = discount;
            this.quarterSpent = quarterSpent;
            this.quarterStart = quarterStart;
            this.quarterEnd = quarterEnd;
            this.premiumThreshold = premiumThreshold;
            this.vipThreshold = vipThreshold;
            this.remainingForPremium = remainingForPremium;
            this.remainingForVip = remainingForVip;
        }

        public String getStatus() { return status; }
        public Double getDiscount() { return discount; }
        public Double getQuarterSpent() { return quarterSpent; }
        public LocalDateTime getQuarterStart() { return quarterStart; }
        public LocalDateTime getQuarterEnd() { return quarterEnd; }
        public Double getPremiumThreshold() { return premiumThreshold; }
        public Double getVipThreshold() { return vipThreshold; }
        public Double getRemainingForPremium() { return remainingForPremium; }
        public Double getRemainingForVip() { return remainingForVip; }
        public Double getDiscountPercent() { return discount * 100; }
    }
}