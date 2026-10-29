package com.designstudio.api;

import com.designstudio.application.dto.CrmDTO;
import com.designstudio.application.dto.MarketingDTO;
import com.designstudio.application.service.MarketingService;
import com.designstudio.application.service.UserCampaignService;
import com.designstudio.domain.entity.Campaign;
import com.designstudio.domain.entity.CampaignTemplate;
import com.designstudio.domain.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/marketing")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5500", "http://localhost:5501", "http://127.0.0.1:5500", "http://127.0.0.1:5501"}, allowCredentials = "true")
public class MarketingController {

    private final MarketingService marketingService;
    private final UserCampaignService userCampaignService;

    @Autowired
    public MarketingController(MarketingService marketingService, UserCampaignService userCampaignService) {
        this.marketingService = marketingService;
        this.userCampaignService = userCampaignService;
    }

    // ==================== КЛИЕНТСКИЕ ENDPOINTS ====================

    /**
     * Получить активные кампании для текущего пользователя
     */
    @GetMapping("/campaigns/my")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<List<MarketingDTO.CampaignDTO>> getMyActiveCampaigns(
            @AuthenticationPrincipal UserDetails userDetails) {

        if (userDetails == null) {
            return ResponseEntity.status(401).build();
        }

        List<MarketingDTO.CampaignDTO> campaigns =
                userCampaignService.getActiveCampaignsForUser(userDetails.getUsername());

        return ResponseEntity.ok(campaigns);
    }

    @GetMapping("/campaigns/my/all")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<List<MarketingDTO.CampaignDTO>> getMyAllCampaigns(
            @AuthenticationPrincipal UserDetails userDetails) {

        if (userDetails == null) {
            return ResponseEntity.status(401).build();
        }

        List<MarketingDTO.CampaignDTO> campaigns =
                userCampaignService.getAllAvailableCampaigns(userDetails.getUsername());

        return ResponseEntity.ok(campaigns);
    }

    @PostMapping("/campaigns/apply/{id}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<?> applyCampaign(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        if (userDetails == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Не авторизован"));
        }

        boolean success = userCampaignService.applyCampaign(userDetails.getUsername(), id);

        if (success) {
            return ResponseEntity.ok(Map.of("success", true, "message", "Промокод применён"));
        } else {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "Не удалось применить промокод"));
        }
    }

    @PostMapping("/campaigns/calculate-discount")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<?> calculateDiscount(
            @RequestBody Map<String, Object> request,
            @AuthenticationPrincipal UserDetails userDetails) {

        if (userDetails == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Не авторизован"));
        }

        BigDecimal amount = new BigDecimal(request.get("amount").toString());
        Long campaignId = Long.valueOf(request.get("campaignId").toString());

        BigDecimal discount = userCampaignService.calculateDiscount(
                userDetails.getUsername(), amount, campaignId);

        return ResponseEntity.ok(Map.of(
                "discount", discount,
                "finalAmount", amount.subtract(discount)
        ));
    }

    // ==================== СООБЩЕНИЯ ПОЛЬЗОВАТЕЛЯ ====================

    @GetMapping("/messages/my")
// @PreAuthorize("hasAnyRole('USER', 'ADMIN')")  // Полностью закомментировано
    public ResponseEntity<List<MarketingDTO.MessageDTO>> getMyMessages(
            @AuthenticationPrincipal UserDetails userDetails) {

        System.out.println("=== getMyMessages called ===");
        System.out.println("UserDetails: " + (userDetails != null ? userDetails.getUsername() : "null"));

        if (userDetails == null) {
            return ResponseEntity.status(401).build();
        }

        User user = marketingService.getUserByEmail(userDetails.getUsername());
        if (user == null) {
            return ResponseEntity.ok(List.of());
        }

        List<MarketingDTO.MessageDTO> messages = marketingService.getUserMessages(user.getId());
        System.out.println("Messages count: " + messages.size());

        return ResponseEntity.ok(messages);
    }

    @GetMapping("/messages/unread-count")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<Map<String, Long>> getUnreadCount(
            @AuthenticationPrincipal UserDetails userDetails) {

        if (userDetails == null) {
            return ResponseEntity.status(401).build();
        }

        User user = marketingService.getUserByEmail(userDetails.getUsername());
        if (user == null) {
            return ResponseEntity.ok(Map.of("count", 0L));
        }

        long count = marketingService.getUnreadMessagesCount(user.getId());
        return ResponseEntity.ok(Map.of("count", count));
    }

    @PutMapping("/messages/{id}/read")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<?> markMessageAsRead(@PathVariable Long id) {
        marketingService.markMessageAsRead(id);
        return ResponseEntity.ok(Map.of("success", true));
    }

    @GetMapping("/campaigns/my/discount")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<?> getMyCampaignDiscount(
            @AuthenticationPrincipal UserDetails userDetails) {

        if (userDetails == null) {
            return ResponseEntity.status(401).build();
        }

        Double discount = userCampaignService.getUserDiscountFromCampaigns(userDetails.getUsername());

        return ResponseEntity.ok(Map.of(
                "discountPercent", discount,
                "hasActiveCampaigns", discount > 0
        ));
    }

    // ==================== АДМИНСКИЕ ENDPOINTS (без изменений) ====================

    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MarketingDTO.MarketingStatsDTO> getMarketingStats() {
        try {
            return ResponseEntity.ok(marketingService.getMarketingStats());
        } catch (Exception e) {
            e.printStackTrace();
            MarketingDTO.MarketingStatsDTO emptyStats = new MarketingDTO.MarketingStatsDTO();
            emptyStats.setTotalCampaigns(0);
            emptyStats.setActiveCampaigns(0);
            emptyStats.setTotalRecipients(0L);
            emptyStats.setAvgOpenRate(0.0);
            emptyStats.setAvgClickRate(0.0);
            emptyStats.setTotalRevenue(BigDecimal.ZERO);
            return ResponseEntity.ok(emptyStats);
        }
    }

    @GetMapping("/stats/period")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MarketingDTO.MarketingStatsDTO> getStatsByPeriod(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        try {
            LocalDateTime start = startDate != null ? LocalDateTime.parse(startDate) : LocalDateTime.now().minusMonths(1);
            LocalDateTime end = endDate != null ? LocalDateTime.parse(endDate) : LocalDateTime.now();
            return ResponseEntity.ok(marketingService.getStatsByPeriod(start, end));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/campaigns")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<MarketingDTO.CampaignDTO>> getAllCampaigns(
            @RequestParam(required = false) String status) {
        try {
            List<MarketingDTO.CampaignDTO> campaigns = marketingService.getAllCampaigns();
            if (status != null && !status.isEmpty()) {
                campaigns = campaigns.stream()
                        .filter(c -> status.equals(c.getStatus()))
                        .toList();
            }
            return ResponseEntity.ok(campaigns);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.ok(List.of());
        }
    }

    @GetMapping("/campaigns/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MarketingDTO.CampaignDTO> getCampaign(@PathVariable Long id) {
        try {
            MarketingDTO.CampaignDTO campaign = marketingService.getCampaign(id);
            if (campaign == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(campaign);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/campaigns")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createCampaign(@RequestBody CampaignRequest request) {
        try {
            Campaign campaign = new Campaign();
            campaign.setName(request.getName() != null ? request.getName() : "Новая кампания");
            campaign.setDescription(request.getDescription());

            if (request.getType() != null) {
                try {
                    campaign.setType(Campaign.CampaignType.valueOf(request.getType().toUpperCase()));
                } catch (IllegalArgumentException e) {
                    campaign.setType(Campaign.CampaignType.EMAIL);
                }
            } else {
                campaign.setType(Campaign.CampaignType.EMAIL);
            }

            campaign.setStatus(Campaign.CampaignStatus.DRAFT);
            campaign.setTargetSegments(request.getTargetSegments());
            campaign.setDiscountPercent(request.getDiscountPercent() != null ? request.getDiscountPercent() : 0.0);
            campaign.setMinOrderAmount(request.getMinOrderAmount() != null ? request.getMinOrderAmount() : 0.0);
            campaign.setValidDays(request.getValidDays() != null ? request.getValidDays() : 7);
            campaign.setEmailSubject(request.getEmailSubject());
            campaign.setEmailContent(request.getEmailContent());
            campaign.setSmsText(request.getSmsText());
            campaign.setAutomated(request.getIsAutomated() != null ? request.getIsAutomated() : false);

            Campaign created = marketingService.createCampaign(campaign, null);
            return ResponseEntity.ok(marketingService.convertToCampaignDto(created));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse("Ошибка создания кампании: " + e.getMessage()));
        }
    }

    @PutMapping("/campaigns/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateCampaign(@PathVariable Long id, @RequestBody CampaignRequest request) {
        try {
            Campaign existing = marketingService.getCampaignEntity(id);
            if (existing == null) {
                return ResponseEntity.notFound().build();
            }

            if (request.getName() != null) {
                existing.setName(request.getName());
            }
            if (request.getDescription() != null) {
                existing.setDescription(request.getDescription());
            }
            if (request.getType() != null) {
                try {
                    existing.setType(Campaign.CampaignType.valueOf(request.getType().toUpperCase()));
                } catch (IllegalArgumentException e) {
                    // Оставляем текущий тип
                }
            }
            if (request.getTargetSegments() != null) {
                existing.setTargetSegments(request.getTargetSegments());
            }
            if (request.getDiscountPercent() != null) {
                existing.setDiscountPercent(request.getDiscountPercent());
            }
            if (request.getMinOrderAmount() != null) {
                existing.setMinOrderAmount(request.getMinOrderAmount());
            }
            if (request.getValidDays() != null) {
                existing.setValidDays(request.getValidDays());
            }
            if (request.getEmailSubject() != null) {
                existing.setEmailSubject(request.getEmailSubject());
            }
            if (request.getEmailContent() != null) {
                existing.setEmailContent(request.getEmailContent());
            }
            if (request.getSmsText() != null) {
                existing.setSmsText(request.getSmsText());
            }
            if (request.getIsAutomated() != null) {
                existing.setAutomated(request.getIsAutomated());
            }

            Campaign result = marketingService.updateCampaign(id, existing);
            return ResponseEntity.ok(marketingService.convertToCampaignDto(result));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse("Ошибка обновления кампании: " + e.getMessage()));
        }
    }

    @DeleteMapping("/campaigns/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteCampaign(@PathVariable Long id) {
        try {
            if (marketingService.deleteCampaign(id)) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "Кампания удалена");
                return ResponseEntity.ok(response);
            }
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse("Ошибка удаления кампании: " + e.getMessage()));
        }
    }

    @PostMapping("/campaigns/{id}/launch")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> launchCampaign(@PathVariable Long id) {
        try {
            Campaign campaign = marketingService.launchCampaign(id);
            if (campaign == null) {
                return ResponseEntity.notFound().build();
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Кампания запущена");
            response.put("campaign", marketingService.convertToCampaignDto(campaign));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse("Ошибка запуска кампании: " + e.getMessage()));
        }
    }

    @PostMapping("/campaigns/{id}/pause")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> pauseCampaign(@PathVariable Long id) {
        try {
            Campaign campaign = marketingService.pauseCampaign(id);
            if (campaign == null) {
                return ResponseEntity.notFound().build();
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Кампания приостановлена");
            response.put("campaign", marketingService.convertToCampaignDto(campaign));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse("Ошибка приостановки кампании: " + e.getMessage()));
        }
    }

    @PostMapping("/campaigns/{id}/resume")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> resumeCampaign(@PathVariable Long id) {
        try {
            Campaign campaign = marketingService.resumeCampaign(id);
            if (campaign == null) {
                return ResponseEntity.notFound().build();
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Кампания возобновлена");
            response.put("campaign", marketingService.convertToCampaignDto(campaign));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse("Ошибка возобновления кампании: " + e.getMessage()));
        }
    }

    @PostMapping("/campaigns/{id}/cancel")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> cancelCampaign(@PathVariable Long id) {
        try {
            Campaign campaign = marketingService.cancelCampaign(id);
            if (campaign == null) {
                return ResponseEntity.notFound().build();
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Кампания отменена");
            response.put("campaign", marketingService.convertToCampaignDto(campaign));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse("Ошибка отмены кампании: " + e.getMessage()));
        }
    }

    @PostMapping("/campaigns/{id}/complete")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> completeCampaign(@PathVariable Long id) {
        try {
            Campaign campaign = marketingService.completeCampaign(id);
            if (campaign == null) {
                return ResponseEntity.notFound().build();
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Кампания завершена");
            response.put("campaign", marketingService.convertToCampaignDto(campaign));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse("Ошибка завершения кампании: " + e.getMessage()));
        }
    }

    @PostMapping("/campaigns/{id}/duplicate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> duplicateCampaign(@PathVariable Long id) {
        try {
            Campaign campaign = marketingService.duplicateCampaign(id);
            if (campaign == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(marketingService.convertToCampaignDto(campaign));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse("Ошибка дублирования кампании: " + e.getMessage()));
        }
    }

    @GetMapping("/campaigns/{id}/recipients")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<CrmDTO.CustomerDTO>> getCampaignRecipients(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(marketingService.getCampaignRecipients(id));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.ok(List.of());
        }
    }

    @PostMapping("/campaigns/{id}/send")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> sendCampaign(@PathVariable Long id) {
        try {
            Map<String, Object> result = marketingService.massSendCampaign(id);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse("Ошибка отправки кампании: " + e.getMessage()));
        }
    }

    @GetMapping("/campaigns/{id}/forecast")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> forecastCampaign(@PathVariable Long id) {
        try {
            Campaign campaign = marketingService.getCampaignEntity(id);
            if (campaign == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(marketingService.forecastCampaignEffectiveness(campaign));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse("Ошибка прогноза: " + e.getMessage()));
        }
    }

    @PostMapping("/campaigns/{campaignId}/apply-template/{templateId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> applyTemplate(@PathVariable Long campaignId, @PathVariable Long templateId) {
        try {
            Campaign campaign = marketingService.applyTemplateToCampaign(campaignId, templateId);
            if (campaign == null) {
                return ResponseEntity.notFound().build();
            }
            marketingService.incrementTemplateUsage(templateId);
            return ResponseEntity.ok(marketingService.convertToCampaignDto(campaign));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse("Ошибка применения шаблона: " + e.getMessage()));
        }
    }

    // ==================== ШАБЛОНЫ ====================

    @GetMapping("/templates")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<MarketingDTO.TemplateDTO>> getAllTemplates() {
        try {
            return ResponseEntity.ok(marketingService.getAllTemplates());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.ok(List.of());
        }
    }

    @GetMapping("/templates/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MarketingDTO.TemplateDTO> getTemplate(@PathVariable Long id) {
        try {
            MarketingDTO.TemplateDTO template = marketingService.getTemplate(id);
            if (template == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(template);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/templates/category/{category}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<MarketingDTO.TemplateDTO>> getTemplatesByCategory(@PathVariable String category) {
        try {
            return ResponseEntity.ok(marketingService.getTemplatesByCategory(category));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.ok(List.of());
        }
    }

    @PostMapping("/templates")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createTemplate(@RequestBody TemplateRequest request) {
        try {
            CampaignTemplate template = new CampaignTemplate();
            template.setName(request.getName() != null ? request.getName() : "Новый шаблон");
            template.setDescription(request.getDescription());

            if (request.getType() != null) {
                try {
                    template.setType(CampaignTemplate.TemplateType.valueOf(request.getType().toUpperCase()));
                } catch (IllegalArgumentException e) {
                    template.setType(CampaignTemplate.TemplateType.EMAIL);
                }
            } else {
                template.setType(CampaignTemplate.TemplateType.EMAIL);
            }

            template.setCategory(request.getCategory() != null ? request.getCategory() : "general");
            template.setSubjectTemplate(request.getSubjectTemplate());
            template.setContentTemplate(request.getContentTemplate());
            template.setIsActive(true);

            CampaignTemplate created = marketingService.createTemplate(template);
            return ResponseEntity.ok(marketingService.convertToTemplateDto(created));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse("Ошибка создания шаблона: " + e.getMessage()));
        }
    }

    @PutMapping("/templates/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateTemplate(@PathVariable Long id, @RequestBody TemplateRequest request) {
        try {
            CampaignTemplate existing = marketingService.getTemplateEntity(id);
            if (existing == null) {
                return ResponseEntity.notFound().build();
            }

            if (request.getName() != null) {
                existing.setName(request.getName());
            }
            if (request.getDescription() != null) {
                existing.setDescription(request.getDescription());
            }
            if (request.getType() != null) {
                try {
                    existing.setType(CampaignTemplate.TemplateType.valueOf(request.getType().toUpperCase()));
                } catch (IllegalArgumentException e) {
                    // Оставляем текущий тип
                }
            }
            if (request.getCategory() != null) {
                existing.setCategory(request.getCategory());
            }
            if (request.getSubjectTemplate() != null) {
                existing.setSubjectTemplate(request.getSubjectTemplate());
            }
            if (request.getContentTemplate() != null) {
                existing.setContentTemplate(request.getContentTemplate());
            }

            CampaignTemplate result = marketingService.updateTemplate(id, existing);
            return ResponseEntity.ok(marketingService.convertToTemplateDto(result));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse("Ошибка обновления шаблона: " + e.getMessage()));
        }
    }

    @DeleteMapping("/templates/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteTemplate(@PathVariable Long id) {
        try {
            if (marketingService.deleteTemplate(id)) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "Шаблон удалён");
                return ResponseEntity.ok(response);
            }
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse("Ошибка удаления шаблона: " + e.getMessage()));
        }
    }

    @PostMapping("/templates/{id}/toggle")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> toggleTemplate(@PathVariable Long id) {
        try {
            CampaignTemplate template = marketingService.toggleTemplateStatus(id);
            if (template == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(marketingService.convertToTemplateDto(template));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse("Ошибка переключения статуса шаблона: " + e.getMessage()));
        }
    }

    // ==================== АВТОМАТИЗАЦИИ ====================

    @GetMapping("/automations")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Map<String, Object>>> getAutomations() {
        try {
            List<Map<String, Object>> automations = Arrays.asList(
                    createAutomation("first_purchase", "Первая покупка",
                            "Отправлять приветственное письмо новым клиентам", true),
                    createAutomation("abandoned_cart", "Брошенная корзина",
                            "Напоминание о товарах в корзине через 1 час", false),
                    createAutomation("birthday", "День рождения",
                            "Поздравление + скидка за 3 дня до дня рождения", true),
                    createAutomation("reactivation", "Реактивация",
                            "Возврат клиентов, неактивных 30/60/90 дней", false),
                    createAutomation("inactive_30", "30 дней без активности",
                            "Автоматическая реактивация после 30 дней", false),
                    createAutomation("inactive_60", "60 дней без активности",
                            "Специальное предложение для неактивных 60 дней", false),
                    createAutomation("anniversary", "Годовщина регистрации",
                            "Поздравление с годовщиной регистрации", false)
            );
            return ResponseEntity.ok(automations);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.ok(List.of());
        }
    }

    @PostMapping("/automations/{type}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> enableAutomation(@PathVariable String type) {
        Map<String, Object> result = new HashMap<>();
        result.put("type", type);
        result.put("enabled", true);
        result.put("message", "Автоматизация '" + type + "' включена");
        result.put("success", true);
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/automations/{type}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> disableAutomation(@PathVariable String type) {
        Map<String, Object> result = new HashMap<>();
        result.put("type", type);
        result.put("enabled", false);
        result.put("message", "Автоматизация '" + type + "' выключена");
        result.put("success", true);
        return ResponseEntity.ok(result);
    }

    // ==================== ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ====================

    private Map<String, Object> createAutomation(String type, String name, String description, boolean enabled) {
        Map<String, Object> automation = new HashMap<>();
        automation.put("type", type);
        automation.put("name", name);
        automation.put("description", description);
        automation.put("enabled", enabled);
        automation.put("icon", getAutomationIcon(type));
        return automation;
    }

    private String getAutomationIcon(String type) {
        switch (type) {
            case "first_purchase": return "bi-bag-check";
            case "abandoned_cart": return "bi-cart-x";
            case "birthday": return "bi-calendar-event";
            case "reactivation": return "bi-person-dash";
            case "inactive_30": return "bi-clock";
            case "inactive_60": return "bi-hourglass";
            case "anniversary": return "bi-gift";
            default: return "bi-gear";
        }
    }

    // ==================== DTOs ====================

    public static class CampaignRequest {
        private String name;
        private String description;
        private String type;
        private String targetSegments;
        private Double discountPercent;
        private Double minOrderAmount;
        private Integer validDays;
        private String emailSubject;
        private String emailContent;
        private String smsText;
        private Boolean isAutomated;
        private String triggerEvent;
        private String startDate;
        private String endDate;
        private String status;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getTargetSegments() { return targetSegments; }
        public void setTargetSegments(String targetSegments) { this.targetSegments = targetSegments; }
        public Double getDiscountPercent() { return discountPercent; }
        public void setDiscountPercent(Double discountPercent) { this.discountPercent = discountPercent; }
        public Double getMinOrderAmount() { return minOrderAmount; }
        public void setMinOrderAmount(Double minOrderAmount) { this.minOrderAmount = minOrderAmount; }
        public Integer getValidDays() { return validDays; }
        public void setValidDays(Integer validDays) { this.validDays = validDays; }
        public String getEmailSubject() { return emailSubject; }
        public void setEmailSubject(String emailSubject) { this.emailSubject = emailSubject; }
        public String getEmailContent() { return emailContent; }
        public void setEmailContent(String emailContent) { this.emailContent = emailContent; }
        public String getSmsText() { return smsText; }
        public void setSmsText(String smsText) { this.smsText = smsText; }
        public Boolean getIsAutomated() { return isAutomated; }
        public void setIsAutomated(Boolean isAutomated) { this.isAutomated = isAutomated; }
        public String getTriggerEvent() { return triggerEvent; }
        public void setTriggerEvent(String triggerEvent) { this.triggerEvent = triggerEvent; }
        public String getStartDate() { return startDate; }
        public void setStartDate(String startDate) { this.startDate = startDate; }
        public String getEndDate() { return endDate; }
        public void setEndDate(String endDate) { this.endDate = endDate; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }

    public static class TemplateRequest {
        private String name;
        private String description;
        private String type;
        private String category;
        private String subjectTemplate;
        private String contentTemplate;
        private Boolean isActive;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        public String getSubjectTemplate() { return subjectTemplate; }
        public void setSubjectTemplate(String subjectTemplate) { this.subjectTemplate = subjectTemplate; }
        public String getContentTemplate() { return contentTemplate; }
        public void setContentTemplate(String contentTemplate) { this.contentTemplate = contentTemplate; }
        public Boolean getIsActive() { return isActive; }
        public void setIsActive(Boolean isActive) { this.isActive = isActive; }
    }

    public static class ErrorResponse {
        private String error;
        private String message;
        private boolean success = false;
        private LocalDateTime timestamp;

        public ErrorResponse(String error) {
            this.error = error;
            this.message = error;
            this.success = false;
            this.timestamp = LocalDateTime.now();
        }

        public String getError() { return error; }
        public void setError(String error) { this.error = error; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    }

    public static class SuccessResponse {
        private boolean success;
        private String message;
        private Object data;
        private LocalDateTime timestamp;

        public SuccessResponse(String message) {
            this.success = true;
            this.message = message;
            this.timestamp = LocalDateTime.now();
        }

        public SuccessResponse(String message, Object data) {
            this.success = true;
            this.message = message;
            this.data = data;
            this.timestamp = LocalDateTime.now();
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public Object getData() { return data; }
        public void setData(Object data) { this.data = data; }
        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    }
}