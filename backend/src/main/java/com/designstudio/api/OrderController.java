package com.designstudio.api;

import com.designstudio.application.dto.OrderDTO;
import com.designstudio.application.dto.OrderItemDTO;
import com.designstudio.application.service.UserCampaignService;
import com.designstudio.application.service.UserService;
import com.designstudio.domain.entity.*;
import com.designstudio.domain.repository.*;
import com.designstudio.infrastructure.security.CustomUserDetails;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5500", "http://localhost:5501", "http://127.0.0.1:5500", "http://127.0.0.1:5501"}, allowCredentials = "true")
public class OrderController {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final OrderItemRepository orderItemRepository;
    private final DesignOrderRepository designOrderRepository;
    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final VisitRepository visitRepository;
    private final UserService userService;
    private final UserCampaignService userCampaignService;
    private final CampaignRepository campaignRepository;  // Добавлено

    public OrderController(OrderRepository orderRepository,
                           OrderItemRepository orderItemRepository,
                           DesignOrderRepository designOrderRepository,
                           CartRepository cartRepository,
                           ProductRepository productRepository,
                           UserService userService,
                           VisitRepository visitRepository,
                           UserCampaignService userCampaignService,
                           UserRepository userRepository,
                           CampaignRepository campaignRepository) {  // Добавлен параметр
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.orderItemRepository = orderItemRepository;
        this.designOrderRepository = designOrderRepository;
        this.cartRepository = cartRepository;
        this.productRepository = productRepository;
        this.visitRepository = visitRepository;
        this.userService = userService;
        this.userCampaignService = userCampaignService;
        this.campaignRepository = campaignRepository;
    }

    @PostMapping("/checkout")
    public ResponseEntity<Map<String, Object>> processCheckout(
            @RequestParam String address,
            @RequestParam String cardNumber,
            @RequestParam String cvc,
            @RequestParam String expiry,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        if (userDetails == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Authorization required"));
        }

        User user = userDetails.getUser();
        List<Cart> cartItems = cartRepository.findByUser(user);

        if (cartItems.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Cart is empty"));
        }

        // Рассчитываем общую сумму без скидки
        double totalWithoutDiscount = 0;
        for (Cart cart : cartItems) {
            double itemPrice = cart.getProduct().getPrice();
            double itemTotal = itemPrice * cart.getQuantity();
            totalWithoutDiscount += itemTotal;
        }

        // User discount (from quarterly system)
        double userDiscountPercent = user.getDiscount() != null ? user.getDiscount() : 0.0;

        // Automatic discount from campaigns
        double campaignDiscountPercent = userCampaignService.getAutomaticDiscountForUser(user.getEmail());
        Long campaignId = userCampaignService.getActiveCampaignId(user.getEmail());

        // Take maximum discount (not sum!)
        double totalDiscountPercent = Math.max(userDiscountPercent, campaignDiscountPercent);

        // Calculate discount amount
        double discountAmount = totalWithoutDiscount * totalDiscountPercent;
        double totalWithDiscount = totalWithoutDiscount - discountAmount;

        // Создаём заказ
        Order order = new Order();
        order.setUser(user);
        order.setAddress(address);
        order.setCardNumber(cardNumber);
        order.setCvc(cvc);
        order.setExpiry(expiry);
        order.setStatus(Order.OrderStatus.PROCESSING);
        order.setCreatedAt(LocalDateTime.now());

        // Сохраняем суммы
        order.setOriginalAmount(totalWithoutDiscount);
        order.setDiscountAmount(discountAmount);
        order.setTotalAmount(totalWithDiscount);

        // Сохраняем ID применённой кампании
        if (campaignId != null) {
            order.setAppliedCampaignId(campaignId);
        }

        order = orderRepository.save(order);

        // Add order items
        for (Cart cart : cartItems) {
            OrderItem item = new OrderItem();
            item.setOrder(order);
            item.setProduct(cart.getProduct());
            item.setQuantity(cart.getQuantity());
            orderItemRepository.save(item);
        }

        // Clear cart
        cartRepository.deleteAll(cartItems);

        // Update campaign statistics
        if (campaignDiscountPercent > 0) {
            userCampaignService.recordCampaignConversion(user.getEmail(), totalWithoutDiscount);
            // Отмечаем кампанию как использованную
            if (campaignId != null) {
                userCampaignService.markCampaignAsUsed(user.getEmail(), campaignId);
            }
        }

        // Update quarterly discount
        System.out.println("[Checkout] Calling updateQuarterlyDiscount for user: " + user.getEmail());
        userService.updateQuarterlyDiscount(user.getId());

        // Get updated discount info
        UserService.DiscountInfo discountInfo = userService.getDiscountInfo(user.getId());

        // Build response
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Order placed successfully");
        response.put("orderId", order.getId().toString());
        response.put("totalWithoutDiscount", totalWithoutDiscount);
        response.put("discountAmount", discountAmount);
        response.put("totalWithDiscount", totalWithDiscount);
        response.put("discountPercent", totalDiscountPercent * 100);
        response.put("campaignApplied", campaignDiscountPercent > userDiscountPercent);
        response.put("campaignId", campaignId);

        // Discount info after order
        if (discountInfo != null) {
            response.put("newStatus", discountInfo.getStatus());
            response.put("newDiscount", discountInfo.getDiscount());
            response.put("newDiscountPercent", discountInfo.getDiscountPercent());
            response.put("quarterSpent", discountInfo.getQuarterSpent());
            response.put("remainingForPremium", discountInfo.getRemainingForPremium());
            response.put("remainingForVip", discountInfo.getRemainingForVip());
        }

        return ResponseEntity.ok(response);
    }

    @GetMapping("/orders")
    public ResponseEntity<List<Map<String, Object>>> getUserOrders(@AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(401).body(List.of());
        }

        User user = userDetails.getUser();
        List<Order> orders = orderRepository.findByUser(user);

        // Сортируем по дате (новые сверху)
        orders.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));

        List<Map<String, Object>> result = orders.stream().map(order -> {
            Map<String, Object> orderMap = new HashMap<>();
            orderMap.put("id", order.getId());

            // Используем сохранённые значения
            double originalAmount = order.getOriginalAmount() != null ? order.getOriginalAmount() : 0.0;
            double discountAmount = order.getDiscountAmount() != null ? order.getDiscountAmount() : 0.0;
            double totalAmount = order.getTotalAmount() != null ? order.getTotalAmount() : originalAmount - discountAmount;

            orderMap.put("originalAmount", originalAmount);
            orderMap.put("discountAmount", discountAmount);
            orderMap.put("totalAmount", totalAmount);

            // Рассчитываем процент скидки
            double discountPercent = 0;
            if (originalAmount > 0 && discountAmount > 0) {
                discountPercent = (discountAmount / originalAmount) * 100;
            }
            orderMap.put("discountPercent", Math.round(discountPercent));

            orderMap.put("status", order.getStatus() != null ? order.getStatus().name() : "PENDING");
            orderMap.put("address", order.getAddress());
            orderMap.put("createdAt", order.getCreatedAt());
            orderMap.put("appliedCampaignId", order.getAppliedCampaignId());

            // Если есть применённая кампания, получаем её название
            if (order.getAppliedCampaignId() != null) {
                campaignRepository.findById(order.getAppliedCampaignId()).ifPresent(campaign -> {
                    orderMap.put("campaignName", campaign.getName());
                    orderMap.put("campaignDiscountPercent", campaign.getDiscountPercent());
                });
            }

            return orderMap;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    @PostMapping("/order")
    public ResponseEntity<Map<String, Object>> createDesignOrder(
            @RequestParam("roomType") String roomType,
            @RequestParam("tariff") String tariff,
            @RequestParam("preferences") String preferences,
            @RequestParam(value = "designStyle", required = false) String designStyle,
            @RequestParam(value = "selectedProducts", required = false) String selectedProducts,
            @RequestParam("address") String address,
            @RequestParam("cardNumber") String cardNumber,
            @RequestParam("cvc") String cvc,
            @RequestParam("expiry") String expiry,
            @RequestParam(value = "photos", required = false) MultipartFile[] files,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        if (userDetails == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Необходимо авторизоваться"));
        }

        User user = userDetails.getUser();

        DesignOrder order = new DesignOrder();
        order.setUser(user);
        order.setRoomType(roomType);
        order.setTariff(tariff);
        order.setPreferences(preferences);
        order.setDesignStyle(designStyle != null ? designStyle : "");
        order.setSelectedProducts(selectedProducts != null ? selectedProducts : "");
        order.setAddress(address);
        order.setCardNumber(cardNumber);
        order.setCvc(cvc);
        order.setExpiry(expiry);
        order.setStatus(DesignOrder.OrderStatus.PENDING);
        order.setCreatedAt(LocalDateTime.now());

        // Сохраняем фото - ИСПРАВЛЕННЫЙ КОД
        if (files != null && files.length > 0) {
            List<String> photoNames = new ArrayList<>();

            // Создаём директорию относительно рабочей директории приложения
            String uploadDirPath = System.getProperty("user.dir") + File.separator + "uploads" + File.separator + "design-orders";
            File uploadDir = new File(uploadDirPath);

            System.out.println("Upload directory path: " + uploadDir.getAbsolutePath());

            if (!uploadDir.exists()) {
                boolean created = uploadDir.mkdirs();
                System.out.println("Directory created: " + created);
            }

            for (MultipartFile file : files) {
                if (file != null && !file.isEmpty()) {
                    // Генерируем уникальное имя файла
                    String originalFilename = file.getOriginalFilename();
                    String extension = "";
                    if (originalFilename != null && originalFilename.contains(".")) {
                        extension = originalFilename.substring(originalFilename.lastIndexOf("."));
                    }
                    String fileName = UUID.randomUUID().toString() + extension;

                    try {
                        File destFile = new File(uploadDir, fileName);
                        file.transferTo(destFile);
                        System.out.println("File saved: " + destFile.getAbsolutePath());
                        // Сохраняем относительный путь для доступа через URL
                        photoNames.add("/uploads/design-orders/" + fileName);
                    } catch (IOException e) {
                        System.err.println("Error saving file: " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            }
            // Сохраняем фотографии как JSON массив
            order.setPhotos(convertToJsonArray(photoNames));
            System.out.println("Saved photos JSON: " + order.getPhotos());
        }

        designOrderRepository.save(order);

        // Обновленные цены
        double price = switch (tariff) {
            case "basic" -> 1000.0;
            case "standard" -> 1800.0;
            case "premium" -> 2500.0;
            default -> 1000.0;
        };

        return ResponseEntity.ok(Map.of(
                "message", "Заказ дизайна создан",
                "orderId", order.getId().toString(),
                "price", price
        ));
    }

    // Вспомогательный метод для конвертации списка в JSON массив
    private String convertToJsonArray(List<String> list) {
        if (list == null || list.isEmpty()) return "[]";
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append("\"").append(list.get(i)).append("\"");
        }
        sb.append("]");
        return sb.toString();
    }

    @GetMapping("/design-orders")
    public ResponseEntity<List<OrderDTO>> getUserDesignOrders(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        if (userDetails == null) {
            return ResponseEntity.status(401).body(List.of());
        }

        User user = userDetails.getUser();
        List<DesignOrder> orders = designOrderRepository.findByUser(user);

        List<OrderDTO> dtos = orders.stream()
                .map(this::convertDesignOrderToDTO)
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    // В методе convertOrderToDTO уберите строку с setFinalTotal
    private OrderDTO convertOrderToDTO(Order order) {
        OrderDTO dto = new OrderDTO();
        dto.setId(order.getId());
        dto.setCreatedAt(order.getCreatedAt());
        dto.setStatus(order.getStatus().name());
        dto.setAddress(order.getAddress());
        dto.setType("product");

        List<OrderItem> items = orderItemRepository.findByOrder(order);
        List<OrderItemDTO> itemDTOs = items.stream()
                .map(i -> {
                    String imageUrl = i.getProduct().getMainImage();
                    if (imageUrl != null && !imageUrl.startsWith("/")) {
                        imageUrl = "/uploads/" + imageUrl;
                    }
                    return new OrderItemDTO(
                            i.getProduct().getId(),
                            i.getProduct().getName(),
                            imageUrl != null ? imageUrl : "",
                            i.getQuantity(),
                            i.getProduct().getPrice()
                    );
                })
                .collect(Collectors.toList());

        dto.setItems(itemDTOs);

        // Используем сохранённые значения
        double originalTotal = order.getOriginalAmount() != null ? order.getOriginalAmount() :
                items.stream().mapToDouble(i -> i.getProduct().getPrice() * i.getQuantity()).sum();
        double discountAmount = order.getDiscountAmount() != null ? order.getDiscountAmount() : 0;
        double finalTotal = order.getTotalAmount() != null ? order.getTotalAmount() : originalTotal - discountAmount;

        dto.setTotal(originalTotal);
        dto.setDiscount(discountAmount);
        dto.setFinalTotal(finalTotal);  // Эта строка должна работать, если поле есть в DTO

        return dto;
    }

    private OrderDTO convertDesignOrderToDTO(DesignOrder order) {
        OrderDTO dto = new OrderDTO();
        dto.setId(order.getId());
        dto.setCreatedAt(order.getCreatedAt());
        dto.setStatus(order.getStatus().name());
        dto.setAddress(order.getAddress());
        dto.setType("design");
        dto.setRoomType(order.getRoomType());
        dto.setTariff(order.getTariff());
        dto.setPreferences(order.getPreferences());
        dto.setDesignStyle(order.getDesignStyle());
        dto.setSelectedProducts(order.getSelectedProducts());

        // Парсим фотографии из JSON
        if (order.getPhotos() != null && !order.getPhotos().isEmpty()) {
            try {
                // Простой парсинг JSON массива
                String photosJson = order.getPhotos();
                List<String> photoList = new ArrayList<>();
                if (photosJson.startsWith("[") && photosJson.endsWith("]")) {
                    String content = photosJson.substring(1, photosJson.length() - 1);
                    if (!content.isEmpty()) {
                        String[] parts = content.split(",");
                        for (String part : parts) {
                            String cleaned = part.trim();
                            if (cleaned.startsWith("\"") && cleaned.endsWith("\"")) {
                                cleaned = cleaned.substring(1, cleaned.length() - 1);
                            }
                            photoList.add(cleaned);
                        }
                    }
                }
                dto.setPhotos(photoList);
            } catch (Exception e) {
                dto.setPhotos(new ArrayList<>());
            }
        } else {
            dto.setPhotos(new ArrayList<>());
        }

        // Обновленные цены
        double price = switch (order.getTariff()) {
            case "basic" -> 1000.0;
            case "standard" -> 1800.0;
            case "premium" -> 2500.0;
            default -> 1000.0;
        };
        dto.setTotal(price);

        return dto;
    }
}