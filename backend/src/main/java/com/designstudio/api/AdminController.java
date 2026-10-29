package com.designstudio.api;

import com.designstudio.application.dto.ProductDTO;
import com.designstudio.application.dto.UserDTO;
import com.designstudio.domain.entity.*;
import com.designstudio.domain.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5500", "http://localhost:5501", "http://127.0.0.1:5500", "http://127.0.0.1:5501"}, allowCredentials = "true")
public class AdminController {

    private static final String API_BASE_URL_FOR_IMAGES = "http://localhost:8080";
    private final ProductRepository productRepository;
    private final ProductCategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final DesignOrderRepository designOrderRepository;
    private final OrderItemRepository orderItemRepository;
    private final PasswordEncoder passwordEncoder;
    private final CartRepository cartRepository;
    private final VisitRepository visitRepository;

    private static final String UPLOAD_DIR = "uploads/";

    // Скидки по статусам
    private static final Map<String, Double> STATUS_DISCOUNTS = Map.of(
            "BASIC", 0.0,
            "PREMIUM", 0.05,
            "VIP", 0.15
    );

    public AdminController(VisitRepository visitRepository, CartRepository cartRepository, ProductRepository productRepository,
                           ProductCategoryRepository categoryRepository,
                           UserRepository userRepository,
                           OrderRepository orderRepository,
                           DesignOrderRepository designOrderRepository,
                           OrderItemRepository orderItemRepository,
                           PasswordEncoder passwordEncoder) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
        this.designOrderRepository = designOrderRepository;
        this.orderItemRepository = orderItemRepository;
        this.passwordEncoder = passwordEncoder;
        this.cartRepository = cartRepository;
        this.visitRepository = visitRepository;
    }

    @GetMapping("/products")
    public ResponseEntity<List<ProductDTO>> getAllProducts() {
        List<Product> products = productRepository.findAll();
        List<ProductDTO> dtos = products.stream()
                .map(this::convertToProductDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/products/{id}")
    public ResponseEntity<ProductDTO> getProduct(@PathVariable Long id) {
        try {
            Product product = productRepository.findById(id).orElse(null);
            if (product == null) {
                System.out.println("Product not found with id: " + id);
                return ResponseEntity.notFound().build();
            }

            ProductDTO dto = convertToProductDTO(product);
            System.out.println("Product found: " + product.getName() + ", main image: " + product.getMainImage());
            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            System.err.println("Error getting product " + id + ": " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/products")
    public ResponseEntity<?> createProduct(
            @RequestParam String name,
            @RequestParam(required = false) String description,
            @RequestParam Double price,
            @RequestParam Long categoryId,
            @RequestParam(value = "mainImage", required = false) MultipartFile mainImage,
            @RequestParam(value = "additionalImages", required = false) MultipartFile[] additionalImages) throws IOException {

        Product product = new Product();
        product.setName(name);
        product.setDescription(description != null ? description : "");
        product.setPrice(price);

        ProductCategory category = categoryRepository.findById(categoryId).orElse(null);
        if (category == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Категория не найдена"));
        }
        product.setCategory(category);

        if (mainImage != null && !mainImage.isEmpty()) {
            String savedFileName = saveFile(mainImage, "");
            product.setMainImage(savedFileName);
        } else {
            product.setMainImage("no-image.png");
        }

        if (additionalImages != null && additionalImages.length > 0) {
            List<String> additionalFileNames = new ArrayList<>();
            for (int i = 0; i < Math.min(additionalImages.length, 3); i++) {
                MultipartFile file = additionalImages[i];
                if (file != null && !file.isEmpty()) {
                    String savedFileName = saveFile(file, "");
                    additionalFileNames.add(savedFileName);
                }
            }
            if (additionalFileNames.size() >= 1) product.setAdditionalImage1(additionalFileNames.get(0));
            if (additionalFileNames.size() >= 2) product.setAdditionalImage2(additionalFileNames.get(1));
            if (additionalFileNames.size() >= 3) product.setAdditionalImage3(additionalFileNames.get(2));
        }

        Product saved = productRepository.save(product);
        return ResponseEntity.ok(convertToProductDTO(saved));
    }

    @PutMapping("/products/{id}")
    public ResponseEntity<?> updateProduct(
            @PathVariable Long id,
            @RequestParam String name,
            @RequestParam(required = false) String description,
            @RequestParam Double price,
            @RequestParam Long categoryId,
            @RequestParam(value = "mainImage", required = false) MultipartFile mainImage,
            @RequestParam(value = "additionalImages", required = false) MultipartFile[] additionalImages,
            @RequestParam(value = "keepExistingMainImage", required = false, defaultValue = "true") boolean keepExistingMainImage,
            @RequestParam(value = "keepExistingAdditionalImages", required = false, defaultValue = "true") boolean keepExistingAdditionalImages) throws IOException {

        ensureUploadsDir();

        Product product = productRepository.findById(id).orElse(null);
        if (product == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Товар не найден"));
        }

        // Обновляем основные данные
        product.setName(name);
        product.setDescription(description != null ? description : "");
        product.setPrice(price);

        ProductCategory category = categoryRepository.findById(categoryId).orElse(null);
        if (category == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Категория не найдена"));
        }
        product.setCategory(category);

        // Обработка главного изображения
        if (!keepExistingMainImage && mainImage != null && !mainImage.isEmpty()) {
            // Удаляем старое изображение
            if (product.getMainImage() != null && !product.getMainImage().equals("no-image.png")) {
                deleteFileIfExists(UPLOAD_DIR + product.getMainImage());
            }
            // Сохраняем новое
            String savedFileName = saveFile(mainImage, "");
            product.setMainImage(savedFileName);
            System.out.println("Main image updated to: " + savedFileName);
        } else {
            System.out.println("Keeping existing main image: " + product.getMainImage());
        }

        // Обработка дополнительных изображений
        if (!keepExistingAdditionalImages && additionalImages != null && additionalImages.length > 0) {
            // Удаляем старые дополнительные изображения
            if (product.getAdditionalImage1() != null && !product.getAdditionalImage1().equals("no-image.png")) {
                deleteFileIfExists(UPLOAD_DIR + product.getAdditionalImage1());
            }
            if (product.getAdditionalImage2() != null && !product.getAdditionalImage2().equals("no-image.png")) {
                deleteFileIfExists(UPLOAD_DIR + product.getAdditionalImage2());
            }
            if (product.getAdditionalImage3() != null && !product.getAdditionalImage3().equals("no-image.png")) {
                deleteFileIfExists(UPLOAD_DIR + product.getAdditionalImage3());
            }

            // Сохраняем новые (до 3 штук)
            List<String> savedImages = new ArrayList<>();
            for (int i = 0; i < Math.min(additionalImages.length, 3); i++) {
                MultipartFile file = additionalImages[i];
                if (file != null && !file.isEmpty()) {
                    String savedFileName = saveFile(file, "");
                    savedImages.add(savedFileName);
                    System.out.println("Saved additional image: " + savedFileName);
                }
            }

            product.setAdditionalImage1(savedImages.size() > 0 ? savedImages.get(0) : null);
            product.setAdditionalImage2(savedImages.size() > 1 ? savedImages.get(1) : null);
            product.setAdditionalImage3(savedImages.size() > 2 ? savedImages.get(2) : null);
        } else {
            System.out.println("Keeping existing additional images");
        }

        Product saved = productRepository.save(product);
        System.out.println("Product saved. Main image: " + saved.getMainImage());

        return ResponseEntity.ok(convertToProductDTO(saved));
    }

    @DeleteMapping("/products/{id}")
    public ResponseEntity<Map<String, String>> deleteProduct(@PathVariable Long id) {
        Product product = productRepository.findById(id).orElse(null);
        if (product == null) {
            return ResponseEntity.notFound().build();
        }

        deleteFileIfExists(UPLOAD_DIR + product.getMainImage());
        deleteFileIfExists(UPLOAD_DIR + product.getAdditionalImage1());
        deleteFileIfExists(UPLOAD_DIR + product.getAdditionalImage2());
        deleteFileIfExists(UPLOAD_DIR + product.getAdditionalImage3());

        productRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "Товар удален"));
    }

    @GetMapping("/users")
    public ResponseEntity<List<UserDTO>> getAllUsers(
            @RequestParam(required = false, defaultValue = "all") String role) {

        List<User> users;
        if ("clients".equals(role)) {
            users = userRepository.findAll().stream()
                    .filter(u -> u.getRole() == User.Role.USER)
                    .collect(Collectors.toList());
        } else if ("admins".equals(role)) {
            users = userRepository.findAll().stream()
                    .filter(u -> u.getRole() == User.Role.ADMIN)
                    .collect(Collectors.toList());
        } else {
            users = userRepository.findAll();
        }

        List<UserDTO> dtos = users.stream()
                .map(this::convertToUserDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/all-orders")
    public ResponseEntity<Map<String, Object>> getAllOrders() {
        Map<String, Object> result = new HashMap<>();

        // Заказы на товары
        List<Order> productOrders = orderRepository.findAll();
        List<Map<String, Object>> productOrdersList = new ArrayList<>();

        for (Order order : productOrders) {
            Map<String, Object> orderMap = new HashMap<>();
            orderMap.put("id", order.getId());
            orderMap.put("type", "product");
            orderMap.put("orderType", "product");
            orderMap.put("userName", order.getUser().getFirstName() + " " + order.getUser().getLastName());
            orderMap.put("userEmail", order.getUser().getEmail());
            orderMap.put("address", order.getAddress());
            orderMap.put("createdAt", order.getCreatedAt().toString());
            orderMap.put("status", order.getStatus().name());

            // ВАЖНО: используем правильные имена полей для фронтенда
            orderMap.put("totalAmount", order.getTotalAmount());
            orderMap.put("originalAmount", order.getOriginalAmount());
            orderMap.put("discountAmount", order.getDiscountAmount());

            // Для совместимости с renderOrderCard
            orderMap.put("total", order.getTotalAmount());
            orderMap.put("discount", order.getDiscountAmount());

            // Товары в заказе
            List<OrderItem> items = orderItemRepository.findByOrder(order);
            List<Map<String, Object>> itemsList = new ArrayList<>();
            for (OrderItem item : items) {
                Map<String, Object> itemMap = new HashMap<>();
                itemMap.put("name", item.getProduct().getName());
                itemMap.put("price", item.getProduct().getPrice());
                itemMap.put("quantity", item.getQuantity());
                itemMap.put("description", item.getProduct().getDescription());
                itemMap.put("mainImage", item.getProduct().getMainImage());
                itemsList.add(itemMap);
            }
            orderMap.put("items", itemsList);
            productOrdersList.add(orderMap);
        }
        result.put("productOrders", productOrdersList);

        // Заказы на дизайн
        List<DesignOrder> designOrders = designOrderRepository.findAll();
        List<Map<String, Object>> designOrdersList = new ArrayList<>();

        for (DesignOrder order : designOrders) {
            Map<String, Object> orderMap = new HashMap<>();
            orderMap.put("id", order.getId());
            orderMap.put("type", "design");
            orderMap.put("orderType", "design");
            orderMap.put("userName", order.getUser().getFirstName() + " " + order.getUser().getLastName());
            orderMap.put("userEmail", order.getUser().getEmail());
            orderMap.put("address", order.getAddress());
            orderMap.put("createdAt", order.getCreatedAt().toString());
            orderMap.put("status", order.getStatus().name());
            orderMap.put("roomType", order.getRoomType());
            orderMap.put("tariff", order.getTariff());
            orderMap.put("preferences", order.getPreferences());
            orderMap.put("designStyle", order.getDesignStyle());

            // Цена в зависимости от тарифа
            double price = switch (order.getTariff()) {
                case "basic" -> 1000.0;
                case "standard" -> 1800.0;
                case "premium" -> 2500.0;
                default -> 1000.0;
            };
            orderMap.put("total", price);
            orderMap.put("totalAmount", price);

            // Скидка клиента
            double discountPercent = order.getUser().getDiscount() != null ? order.getUser().getDiscount() : 0.0;
            double discountAmount = price * discountPercent;
            orderMap.put("discount", discountAmount);
            orderMap.put("discountAmount", discountAmount);

            // Выбранные товары
            if (order.getSelectedProducts() != null && !order.getSelectedProducts().isEmpty()) {
                orderMap.put("selectedProducts", order.getSelectedProducts());
            }

            // Фото - парсим JSON массив
            if (order.getPhotos() != null && !order.getPhotos().isEmpty()) {
                List<String> photoList = parsePhotosJson(order.getPhotos());
                orderMap.put("photos", photoList);
            } else {
                orderMap.put("photos", new ArrayList<>());
            }

            designOrdersList.add(orderMap);
        }
        result.put("designOrders", designOrdersList);

        return ResponseEntity.ok(result);
    }

    @PostMapping("/users")
    public ResponseEntity<?> createAdmin(@RequestBody User user) {
        if (userRepository.findByEmail(user.getEmail()) != null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email уже используется"));
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setRole(User.Role.ADMIN);
        user.setStatus(User.Status.VIP);
        user.setDiscount(0.15);
        userRepository.save(user);
        return ResponseEntity.ok(convertToUserDTO(user));
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<Map<String, String>> deleteUser(@PathVariable Long id) {
        User user = userRepository.findById(id).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Пользователь не найден"));
        }

        // Удаляем связанные данные
        // 1. Удаляем корзины
        List<Cart> carts = cartRepository.findByUser(user);
        cartRepository.deleteAll(carts);

        // 2. Удаляем записи посещений (visits)
        List<Visit> visits = visitRepository.findByUser(user);
        visitRepository.deleteAll(visits);

        // 3. Удаляем заказы товаров (сначала удаляем items)
        List<Order> orders = orderRepository.findByUser(user);
        for (Order order : orders) {
            List<OrderItem> items = orderItemRepository.findByOrder(order);
            orderItemRepository.deleteAll(items);
        }
        orderRepository.deleteAll(orders);

        // 4. Удаляем заказы дизайна
        List<DesignOrder> designOrders = designOrderRepository.findByUser(user);
        designOrderRepository.deleteAll(designOrders);

        userRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "Пользователь удален"));
    }

    @PutMapping("/users/{id}/status")
    public ResponseEntity<?> updateUserStatus(@PathVariable Long id, @RequestParam String status) {
        User user = userRepository.findById(id).orElse(null);
        if (user == null) return ResponseEntity.notFound().build();

        try {
            user.setStatus(User.Status.valueOf(status));

            // Автоматически присваиваем скидку по статусу
            Double discount = STATUS_DISCOUNTS.get(status);
            if (discount != null) {
                user.setDiscount(discount);
            }

            userRepository.save(user);
            return ResponseEntity.ok(Map.of(
                    "message", "Статус обновлен",
                    "status", status,
                    "discount", user.getDiscount()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Неверный статус"));
        }
    }

    @GetMapping("/user-orders/{userId}")
    public ResponseEntity<List<Map<String, Object>>> getUserOrders(@PathVariable Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return ResponseEntity.ok(List.of());
        }

        List<Map<String, Object>> allOrders = new ArrayList<>();

        // Заказы на товары
        List<Order> productOrders = orderRepository.findByUser(user);
        for (Order order : productOrders) {
            Map<String, Object> orderMap = new HashMap<>();
            orderMap.put("id", order.getId());
            orderMap.put("type", "product");
            orderMap.put("status", order.getStatus().name());
            orderMap.put("address", order.getAddress());
            orderMap.put("createdAt", order.getCreatedAt().toString());
            orderMap.put("total", order.getTotalAmount());
            orderMap.put("originalAmount", order.getOriginalAmount());
            orderMap.put("discount", order.getDiscountAmount());
            allOrders.add(orderMap);
        }

        // Заказы на дизайн
        List<DesignOrder> designOrders = designOrderRepository.findByUser(user);
        for (DesignOrder order : designOrders) {
            Map<String, Object> orderMap = new HashMap<>();
            orderMap.put("id", order.getId());
            orderMap.put("type", "design");
            orderMap.put("status", order.getStatus().name());
            orderMap.put("address", order.getAddress());
            orderMap.put("createdAt", order.getCreatedAt().toString());
            orderMap.put("roomType", order.getRoomType());
            orderMap.put("tariff", order.getTariff());

            // НОВЫЕ ЦЕНЫ
            double price = switch (order.getTariff()) {
                case "basic" -> 1000.0;
                case "standard" -> 1800.0;
                case "premium" -> 2500.0;
                default -> 1000.0;
            };
            orderMap.put("total", price);

            // Фото
            if (order.getPhotos() != null && !order.getPhotos().isEmpty()) {
                List<String> photoList = parsePhotosJson(order.getPhotos());
                orderMap.put("photos", photoList);
            }

            allOrders.add(orderMap);
        }

        // Сортируем по дате (новые сверху)
        allOrders.sort((a, b) -> {
            String dateA = (String) a.get("createdAt");
            String dateB = (String) b.get("createdAt");
            return dateB.compareTo(dateA);
        });

        return ResponseEntity.ok(allOrders);
    }

    // Вспомогательный метод для парсинга JSON массива фото
    private List<String> parsePhotosJson(String photosJson) {
        List<String> photoList = new ArrayList<>();
        if (photosJson == null || photosJson.isEmpty()) return photoList;

        try {
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
        } catch (Exception e) {
            System.err.println("Ошибка парсинга фото: " + e.getMessage());
        }
        return photoList;
    }

    @PutMapping("/orders/{orderId}/status")
    public ResponseEntity<Map<String, Object>> updateOrderStatus(
            @PathVariable Long orderId,
            @RequestParam String status,
            @RequestParam String type) {

        try {
            if ("product".equals(type)) {
                Order order = orderRepository.findById(orderId).orElse(null);
                if (order == null) {
                    return ResponseEntity.notFound().build();
                }
                order.setStatus(Order.OrderStatus.valueOf(status));
                orderRepository.save(order);
            } else if ("design".equals(type)) {
                DesignOrder order = designOrderRepository.findById(orderId).orElse(null);
                if (order == null) {
                    return ResponseEntity.notFound().build();
                }
                order.setStatus(DesignOrder.OrderStatus.valueOf(status));
                designOrderRepository.save(order);
            } else {
                return ResponseEntity.badRequest().body(Map.of("error", "Неверный тип заказа"));
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Статус заказа обновлён");
            response.put("status", status);
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Неверный статус"));
        }
    }

    @GetMapping("/orders/{orderId}/items")
    public ResponseEntity<List<Map<String, Object>>> getOrderItems(@PathVariable Long orderId) {
        Order order = orderRepository.findById(orderId).orElse(null);
        if (order == null) {
            return ResponseEntity.notFound().build();
        }

        List<OrderItem> items = orderItemRepository.findByOrder(order);
        List<Map<String, Object>> result = new ArrayList<>();

        for (OrderItem item : items) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", item.getId());
            map.put("name", item.getProduct().getName());
            map.put("description", item.getProduct().getDescription());
            map.put("price", item.getProduct().getPrice());
            map.put("quantity", item.getQuantity());
            map.put("mainImage", item.getProduct().getMainImage());
            result.add(map);
        }

        return ResponseEntity.ok(result);
    }

    // ========== ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ==========

    private ProductDTO convertToProductDTO(Product product) {
        ProductDTO dto = new ProductDTO();
        dto.setId(product.getId());
        dto.setName(product.getName());
        dto.setDescription(product.getDescription() != null ? product.getDescription() : "");
        dto.setPrice(product.getPrice());
        dto.setCategoryName(product.getCategory() != null ? product.getCategory().getName() : "");
        dto.setCategoryId(product.getCategory() != null ? product.getCategory().getId() : null);

        // Формируем URL главного изображения
        String mainImageUrl = null;
        if (product.getMainImage() != null && !product.getMainImage().isEmpty() && !product.getMainImage().equals("no-image.png")) {
            // Проверяем существование файла
            String workingDir = System.getProperty("user.dir");
            File imageFile = new File(workingDir, UPLOAD_DIR + product.getMainImage());
            if (imageFile.exists()) {
                mainImageUrl = API_BASE_URL_FOR_IMAGES + "/uploads/" + product.getMainImage();
            } else {
                System.out.println("Main image not found: " + imageFile.getAbsolutePath());
            }
        }

        if (mainImageUrl == null) {
            mainImageUrl = "/uploads/no-image.png";
        }
        dto.setMainImageUrl(mainImageUrl);

        // Формируем URL дополнительных изображений
        List<String> additional = new ArrayList<>();
        String workingDir = System.getProperty("user.dir");

        addImageUrl(additional, product.getAdditionalImage1(), workingDir);
        addImageUrl(additional, product.getAdditionalImage2(), workingDir);
        addImageUrl(additional, product.getAdditionalImage3(), workingDir);

        dto.setAdditionalImages(additional);

        return dto;
    }

    private void addImageUrl(List<String> list, String filename, String workingDir) {
        if (filename != null && !filename.isEmpty() && !filename.equals("no-image.png")) {
            File imageFile = new File(workingDir, UPLOAD_DIR + filename);
            if (imageFile.exists()) {
                list.add(API_BASE_URL_FOR_IMAGES + "/uploads/" + filename);
            }
        }
    }

    // Вспомогательный метод для очистки имени файла
    private String sanitizeFileName(String fileName) {
        if (fileName == null) return "";
        // Удаляем все символы, не являющиеся латинскими буквами, цифрами, точкой или дефисом
        return fileName.replaceAll("[^a-zA-Z0-9.-]", "_");
    }
    @GetMapping("/products/{id}/check-images")
    public ResponseEntity<?> checkProductImages(@PathVariable Long id) {
        Product product = productRepository.findById(id).orElse(null);
        if (product == null) {
            return ResponseEntity.notFound().build();
        }

        Map<String, Object> result = new HashMap<>();
        result.put("productId", id);
        result.put("productName", product.getName());

        // Проверяем главное изображение
        String mainImage = product.getMainImage();
        if (mainImage != null && !mainImage.isEmpty()) {
            File mainFile = new File(UPLOAD_DIR + mainImage);
            result.put("mainImage", Map.of(
                    "filename", mainImage,
                    "exists", mainFile.exists(),
                    "path", mainFile.getAbsolutePath()
            ));
        } else {
            result.put("mainImage", "not set");
        }

        // Проверяем дополнительные изображения
        List<Map<String, Object>> additional = new ArrayList<>();
        addImageCheck(additional, product.getAdditionalImage1());
        addImageCheck(additional, product.getAdditionalImage2());
        addImageCheck(additional, product.getAdditionalImage3());
        result.put("additionalImages", additional);

        return ResponseEntity.ok(result);
    }

    private void addImageCheck(List<Map<String, Object>> list, String filename) {
        if (filename != null && !filename.isEmpty()) {
            File file = new File(UPLOAD_DIR + filename);
            list.add(Map.of(
                    "filename", filename,
                    "exists", file.exists(),
                    "path", file.getAbsolutePath()
            ));
        }
    }

    private UserDTO convertToUserDTO(User user) {
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setEmail(user.getEmail());
        dto.setRole(user.getRole().name());
        dto.setStatus(user.getStatus().name());
        dto.setDiscount(user.getDiscount());

        // Добавляем информацию о заказах
        int totalOrders = orderRepository.findByUser(user).size() + designOrderRepository.findByUser(user).size();
        dto.setTotalOrders(totalOrders);

        double totalSpent = orderRepository.findByUser(user).stream()
                .mapToDouble(this::calculateOrderTotal)
                .sum();
        dto.setTotalSpent(totalSpent);

        return dto;
    }

    private double calculateOrderTotal(Order order) {
        List<OrderItem> items = orderItemRepository.findByOrder(order);
        return items.stream()
                .mapToDouble(i -> i.getProduct().getPrice() * i.getQuantity())
                .sum();
    }

    private void ensureUploadsDir() {
        String workingDir = System.getProperty("user.dir");
        File uploadDir = new File(workingDir, UPLOAD_DIR);
        if (!uploadDir.exists()) {
            boolean created = uploadDir.mkdirs();
            System.out.println("Created uploads directory at: " + uploadDir.getAbsolutePath() + " - " + created);
        }

        // Проверяем, что директория существует
        if (uploadDir.exists()) {
            System.out.println("Uploads directory exists at: " + uploadDir.getAbsolutePath());
        } else {
            System.err.println("Failed to create uploads directory at: " + uploadDir.getAbsolutePath());
        }
    }

    @GetMapping("/uploads/check/{filename}")
    public ResponseEntity<?> checkImageExists(@PathVariable String filename) {
        File file = new File(UPLOAD_DIR + filename);
        Map<String, Object> result = new HashMap<>();
        result.put("filename", filename);
        result.put("exists", file.exists());
        result.put("path", file.getAbsolutePath());
        return ResponseEntity.ok(result);
    }

    private String saveFile(MultipartFile file, String destination) throws IOException {
        if (file == null || file.isEmpty()) {
            return null;
        }

        // Генерируем безопасное имя файла
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            // Ограничиваем длину расширения
            if (extension.length() > 5) {
                extension = ".jpg";
            }
        }
        String safeFileName = UUID.randomUUID().toString() + extension;

        // Получаем абсолютный путь к папке uploads
        String workingDir = System.getProperty("user.dir");
        File uploadDir = new File(workingDir, UPLOAD_DIR);

        // Создаём директорию если не существует
        if (!uploadDir.exists()) {
            boolean created = uploadDir.mkdirs();
            System.out.println("Created uploads directory: " + uploadDir.getAbsolutePath() + " - " + created);
        }

        // Создаём файл в правильной директории
        File destFile = new File(uploadDir, safeFileName);
        System.out.println("Saving file to: " + destFile.getAbsolutePath());

        // Копируем содержимое
        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }

        System.out.println("File saved successfully: " + safeFileName);
        return safeFileName;
    }

    private void deleteFileIfExists(String filePath) {
        if (filePath == null || filePath.isEmpty() ||
                filePath.equals("uploads/") || filePath.equals("uploads/no-image.png") ||
                filePath.equals("src/images/default.jpg")) {
            return;
        }
        try {
            // Получаем полный путь к файлу
            String workingDir = System.getProperty("user.dir");
            File file = new File(workingDir, filePath);

            // Также пробуем найти файл в папке uploads без пути
            if (!file.exists()) {
                file = new File(workingDir, UPLOAD_DIR + new File(filePath).getName());
            }

            if (file.exists() && file.isFile()) {
                boolean deleted = file.delete();
                System.out.println("Deleted file: " + file.getAbsolutePath() + " - " + deleted);
            } else {
                System.out.println("File not found for deletion: " + filePath);
            }
        } catch (Exception e) {
            System.err.println("Ошибка при удалении файла: " + filePath + " - " + e.getMessage());
        }
    }
}