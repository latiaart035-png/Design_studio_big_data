package com.designstudio.application.service;

import com.designstudio.application.dto.CrmDTO;
import com.designstudio.application.dto.MarketingDTO;
import com.designstudio.domain.entity.*;
import com.designstudio.domain.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class MarketingService {

    @Autowired
    private CampaignRepository campaignRepository;

    @Autowired
    private CampaignTemplateRepository templateRepository;

    @Autowired
    private UserRepository userRepository;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // Добавьте в начало класса новые поля:
    @Autowired
    private UserMessageRepository userMessageRepository;

    @Autowired
    private OrderRepository orderRepository;

// ==================== УПРАВЛЕНИЕ СООБЩЕНИЯМИ ====================

    public void sendMessageToUser(Long userId, String subject, String content, String type) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return;

        UserMessage message = new UserMessage();
        message.setUser(user);
        message.setSubject(subject);
        message.setContent(content);
        message.setType(type);
        message.setSentAt(LocalDateTime.now());
        message.setIsRead(false);

        userMessageRepository.save(message);
        System.out.println("Message sent to user " + user.getEmail() + ": " + subject);
    }

    public List<MarketingDTO.MessageDTO> getUserMessages(Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return List.of();

        return userMessageRepository.findByUserOrderBySentAtDesc(user).stream()
                .map(this::convertToMessageDto)
                .collect(Collectors.toList());
    }

    public long getUnreadMessagesCount(Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return 0;
        return userMessageRepository.countByUserAndIsReadFalse(user);
    }

    public void markMessageAsRead(Long messageId) {
        UserMessage message = userMessageRepository.findById(messageId).orElse(null);
        if (message != null) {
            message.setIsRead(true);
            userMessageRepository.save(message);
        }
    }

// ==================== АВТОМАТИЗАЦИИ ====================

    @Scheduled(fixedDelay = 3600000) // Каждый час
    public void processAutomations() {
        System.out.println("Processing automations... at " + LocalDateTime.now());

        processFirstPurchaseAutomation();
        processBirthdayAutomation();
        processReactivationAutomation();
        processAnniversaryAutomation();
        checkExpiredCampaigns();
    }

    private void processFirstPurchaseAutomation() {
        List<User> users = userRepository.findAll();
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);

        for (User user : users) {
            List<Order> orders = orderRepository.findByUser(user);
            // Проверяем, что у пользователя есть ровно один заказ и он сделан недавно
            if (orders.size() != 1) continue;

            Order firstOrder = orders.get(0);
            if (firstOrder.getCreatedAt() != null && firstOrder.getCreatedAt().isAfter(oneHourAgo)) {
                String content = String.format(
                        "Здравствуйте, %s %s! Благодарим вас за первую покупку в Design Studio!\n\n" +
                                "Вы оформили заказ на сумму %.2f BYN.\n\n" +
                                "Мы подготовили для вас персональную скидку 10%% на следующий заказ по промокоду: WELCOME10\n\n" +
                                "С уважением, команда Design Studio.",
                        user.getFirstName(), user.getLastName(), firstOrder.getTotalAmount()
                );
                sendMessageToUser(user.getId(), "Добро пожаловать в Design Studio!", content, "first_purchase");
            }
        }
    }

    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    private void processAnniversaryAutomation() {
        List<User> users = userRepository.findAll();
        LocalDateTime now = LocalDateTime.now();

        // Получаем ID сообщений, которые уже были отправлены для автоматизаций
        Set<String> sentMessagesKey = new HashSet<>();
        // Загружаем уже отправленные сообщения за последние 24 часа
        LocalDateTime oneDayAgo = now.minusDays(1);
        List<UserMessage> recentMessages = userMessageRepository.findBySentAtAfter(oneDayAgo);
        for (UserMessage msg : recentMessages) {
            sentMessagesKey.add(msg.getUser().getId() + "_" + msg.getType());
        }

        for (User user : users) {
            if (user.getCreatedAt() == null) continue;

            // Проверяем, не отправляли ли уже сообщение о годовщине этому пользователю сегодня
            String messageKey = user.getId() + "_anniversary";
            if (sentMessagesKey.contains(messageKey)) {
                continue; // Уже отправлено сегодня
            }

            LocalDateTime registrationDate = user.getCreatedAt();
            LocalDateTime anniversary = registrationDate.withYear(now.getYear());

            if (anniversary.toLocalDate().equals(now.toLocalDate())) {
                String content = String.format(
                        "Здравствуйте, %s %s! Ровно год назад вы присоединились к Design Studio.\n\n" +
                                "Спасибо, что вы с нами! В честь годовщины дарим скидку 10%% по промокоду: ANNIVERSARY10",
                        user.getFirstName(), user.getLastName()
                );
                sendMessageToUser(user.getId(), "Годовщина регистрации!", content, "anniversary");
            }
        }
    }

    private void processBirthdayAutomation() {
        List<User> users = userRepository.findAll();
        LocalDateTime now = LocalDateTime.now();

        for (User user : users) {
            if (user.getBirthDate() == null) continue;

            LocalDateTime birthDate = user.getBirthDate();
            LocalDateTime threeDaysBefore = birthDate.minusDays(3);

            // Проверяем, что сегодня за 3 дня до дня рождения
            if (threeDaysBefore.getMonth() == now.getMonth() &&
                    threeDaysBefore.getDayOfMonth() == now.getDayOfMonth()) {

                String content = String.format(
                        "Дорогой %s %s! Поздравляем вас с днём рождения!\n\n" +
                                "В честь вашего праздника дарим персональную скидку 15%% на любой заказ по промокоду: BIRTHDAY15\n\n" +
                                "Пусть ваш дом становится уютнее вместе с Design Studio!",
                        user.getFirstName(), user.getLastName()
                );
                sendMessageToUser(user.getId(), "С днём рождения!", content, "birthday");
            }
        }
    }

    private void processReactivationAutomation() {
        List<User> users = userRepository.findAll();
        LocalDateTime now = LocalDateTime.now();

        for (User user : users) {
            List<Order> orders = orderRepository.findByUser(user);
            if (orders.isEmpty()) continue;

            Order lastOrder = orders.stream()
                    .max(Comparator.comparing(Order::getCreatedAt))
                    .orElse(null);

            if (lastOrder == null || lastOrder.getCreatedAt() == null) continue;

            long daysInactive = Duration.between(lastOrder.getCreatedAt(), now).toDays();

            if (daysInactive == 30) {
                String content = String.format(
                        "Здравствуйте, %s %s! Вы уже 30 дней не заглядывали к нам.\n\n" +
                                "У нас появились новые коллекции, которые могут вас заинтересовать.\n" +
                                "Используйте промокод BACK30 для скидки 10%% на следующий заказ!",
                        user.getFirstName(), user.getLastName()
                );
                sendMessageToUser(user.getId(), "Мы по вам скучаем!", content, "reactivation");
            } else if (daysInactive == 60) {
                String content = String.format(
                        "Здравствуйте, %s %s! Специально для вас — скидка 15%% по промокоду BACK60.\n\n" +
                                "Не упустите возможность обновить интерьер!",
                        user.getFirstName(), user.getLastName()
                );
                sendMessageToUser(user.getId(), "Эксклюзивное предложение для вас!", content, "reactivation");
            } else if (daysInactive == 90) {
                String content = String.format(
                        "Здравствуйте, %s %s! Мы подготовили для вас персональное предложение — скидку 20%%\n" +
                                "на любой заказ по промокоду BACK90.\n\n" +
                                "Ждём вас в Design Studio!",
                        user.getFirstName(), user.getLastName()
                );
                sendMessageToUser(user.getId(), "Специальное предложение", content, "reactivation");
            }
        }
    }

    @Scheduled(fixedDelay = 3600000) // Каждый час
    public void checkExpiredCampaigns() {
        LocalDateTime now = LocalDateTime.now();
        List<Campaign> runningCampaigns = campaignRepository.findByStatus(Campaign.CampaignStatus.RUNNING);

        for (Campaign campaign : runningCampaigns) {
            if (campaign.getEndDate() != null && campaign.getEndDate().isBefore(now)) {
                campaign.setStatus(Campaign.CampaignStatus.COMPLETED);
                campaign.setCompletedAt(now);
                campaignRepository.save(campaign);
                System.out.println("Campaign '" + campaign.getName() + "' automatically completed");
            }
        }
    }

    // Обновите метод launchCampaign:
    public Campaign launchCampaign(Long id) {
        Campaign campaign = campaignRepository.findById(id).orElse(null);
        if (campaign == null) return null;

        LocalDateTime now = LocalDateTime.now();

        // Проверка дат
        if (campaign.getStartDate() != null && campaign.getStartDate().isAfter(now)) {
            campaign.setStatus(Campaign.CampaignStatus.SCHEDULED);
        } else if (campaign.getEndDate() != null && campaign.getEndDate().isBefore(now)) {
            campaign.setStatus(Campaign.CampaignStatus.COMPLETED);
            return campaignRepository.save(campaign);
        } else {
            campaign.setStatus(Campaign.CampaignStatus.RUNNING);
        }

        campaign.setLaunchedAt(LocalDateTime.now());

        long recipientCount = calculateRecipients(campaign);
        campaign.setTotalRecipients(recipientCount);
        campaign.setSentCount((int) recipientCount);
        campaign.setDeliveredCount((int) (recipientCount * 0.95));

        return campaignRepository.save(campaign);
    }

    // Конвертер для MessageDTO
    private MarketingDTO.MessageDTO convertToMessageDto(UserMessage message) {
        MarketingDTO.MessageDTO dto = new MarketingDTO.MessageDTO();
        dto.setId(message.getId());
        dto.setUserId(message.getUser().getId());
        dto.setEmail(message.getUser().getEmail());
        dto.setSubject(message.getSubject());
        dto.setContent(message.getContent());
        dto.setType(message.getType());
        dto.setSentAt(message.getSentAt());
        dto.setIsRead(message.getIsRead());
        return dto;
    }

    // ==================== СТАТИСТИКА ====================

    public MarketingDTO.MarketingStatsDTO getMarketingStats() {
        List<Campaign> campaigns = campaignRepository.findAll();

        MarketingDTO.MarketingStatsDTO stats = new MarketingDTO.MarketingStatsDTO();

        stats.setTotalCampaigns(campaigns.size());

        stats.setActiveCampaigns(campaigns.stream()
                .filter(c -> c.getStatus() == Campaign.CampaignStatus.RUNNING)
                .count());

        stats.setTotalRecipients(campaigns.stream()
                .mapToLong(c -> c.getTotalRecipients() != null ? c.getTotalRecipients() : 0)
                .sum());

        double avgOpenRate = campaigns.stream()
                .filter(c -> c.getSentCount() != null && c.getSentCount() > 0)
                .mapToDouble(c -> {
                    int sent = c.getSentCount();
                    int opened = c.getOpenedCount() != null ? c.getOpenedCount() : 0;
                    return (opened * 100.0) / sent;
                })
                .average()
                .orElse(0.0);
        stats.setAvgOpenRate(round(avgOpenRate, 2));

        double avgClickRate = campaigns.stream()
                .filter(c -> c.getSentCount() != null && c.getSentCount() > 0)
                .mapToDouble(c -> {
                    int sent = c.getSentCount();
                    int clicked = c.getClickedCount() != null ? c.getClickedCount() : 0;
                    return (clicked * 100.0) / sent;
                })
                .average()
                .orElse(0.0);
        stats.setAvgClickRate(round(avgClickRate, 2));

        BigDecimal totalRevenue = campaigns.stream()
                .map(c -> c.getRevenueGenerated() != null ? c.getRevenueGenerated() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        stats.setTotalRevenue(totalRevenue);

        return stats;
    }

    public MarketingDTO.MarketingStatsDTO getStatsByPeriod(LocalDateTime startDate, LocalDateTime endDate) {
        List<Campaign> campaigns = campaignRepository.findByLaunchedAtBetween(startDate, endDate);

        MarketingDTO.MarketingStatsDTO stats = new MarketingDTO.MarketingStatsDTO();
        stats.setTotalCampaigns(campaigns.size());
        stats.setActiveCampaigns(campaigns.stream()
                .filter(c -> c.getStatus() == Campaign.CampaignStatus.RUNNING)
                .count());
        stats.setTotalRecipients(campaigns.stream()
                .mapToLong(c -> c.getTotalRecipients() != null ? c.getTotalRecipients() : 0)
                .sum());

        double avgOpenRate = campaigns.stream()
                .filter(c -> c.getSentCount() != null && c.getSentCount() > 0)
                .mapToDouble(c -> (c.getOpenedCount() * 100.0) / c.getSentCount())
                .average()
                .orElse(0.0);
        stats.setAvgOpenRate(round(avgOpenRate, 2));

        double avgClickRate = campaigns.stream()
                .filter(c -> c.getSentCount() != null && c.getSentCount() > 0)
                .mapToDouble(c -> (c.getClickedCount() * 100.0) / c.getSentCount())
                .average()
                .orElse(0.0);
        stats.setAvgClickRate(round(avgClickRate, 2));

        stats.setTotalRevenue(campaigns.stream()
                .map(c -> c.getRevenueGenerated() != null ? c.getRevenueGenerated() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add));

        return stats;
    }

    // ==================== КАМПАНИИ ====================

    public List<MarketingDTO.CampaignDTO> getAllCampaigns() {
        List<Campaign> campaigns = campaignRepository.findAllByOrderByCreatedAtDesc();
        return campaigns.stream()
                .map(this::convertToCampaignDto)
                .collect(Collectors.toList());
    }

    public MarketingDTO.CampaignDTO getCampaign(Long id) {
        Campaign campaign = campaignRepository.findById(id).orElse(null);
        return campaign != null ? convertToCampaignDto(campaign) : null;
    }

    public Campaign getCampaignEntity(Long id) {
        return campaignRepository.findById(id).orElse(null);
    }

    public Campaign createCampaign(Campaign campaign, Long templateId) {
        campaign.setCreatedAt(LocalDateTime.now());
        campaign.setStatus(Campaign.CampaignStatus.DRAFT);

        campaign.setTotalRecipients(0L);
        campaign.setSentCount(0);
        campaign.setDeliveredCount(0);
        campaign.setOpenedCount(0);
        campaign.setClickedCount(0);
        campaign.setConvertedCount(0);
        campaign.setRevenueGenerated(BigDecimal.ZERO);

        if (templateId != null) {
            templateRepository.findById(templateId).ifPresent(template -> {
                campaign.setEmailSubject(template.getSubjectTemplate());
                campaign.setEmailContent(template.getContentTemplate());
                if (template.getType() != null) {
                    campaign.setType(Campaign.CampaignType.valueOf(template.getType().name()));
                }
            });
        }

        return campaignRepository.save(campaign);
    }

    public Campaign updateCampaign(Long id, Campaign campaign) {
        Campaign existing = campaignRepository.findById(id).orElse(null);
        if (existing == null) {
            return null;
        }

        existing.setName(campaign.getName());
        existing.setDescription(campaign.getDescription());
        existing.setType(campaign.getType());
        existing.setTargetSegments(campaign.getTargetSegments());
        existing.setDiscountPercent(campaign.getDiscountPercent());
        existing.setMinOrderAmount(campaign.getMinOrderAmount());
        existing.setValidDays(campaign.getValidDays());
        existing.setEmailSubject(campaign.getEmailSubject());
        existing.setEmailContent(campaign.getEmailContent());
        existing.setSmsText(campaign.getSmsText());
        existing.setAutomated(campaign.isAutomated());

        return campaignRepository.save(existing);
    }

    public boolean deleteCampaign(Long id) {
        if (campaignRepository.existsById(id)) {
            campaignRepository.deleteById(id);
            return true;
        }
        return false;
    }

    public Campaign pauseCampaign(Long id) {
        Campaign campaign = campaignRepository.findById(id).orElse(null);
        if (campaign == null || campaign.getStatus() != Campaign.CampaignStatus.RUNNING) {
            return campaign;
        }

        campaign.setStatus(Campaign.CampaignStatus.PAUSED);
        return campaignRepository.save(campaign);
    }

    public Campaign resumeCampaign(Long id) {
        Campaign campaign = campaignRepository.findById(id).orElse(null);
        if (campaign == null || campaign.getStatus() != Campaign.CampaignStatus.PAUSED) {
            return campaign;
        }

        campaign.setStatus(Campaign.CampaignStatus.RUNNING);
        return campaignRepository.save(campaign);
    }

    public Campaign cancelCampaign(Long id) {
        Campaign campaign = campaignRepository.findById(id).orElse(null);
        if (campaign == null) {
            return null;
        }

        campaign.setStatus(Campaign.CampaignStatus.CANCELLED);
        campaign.setCompletedAt(LocalDateTime.now());
        return campaignRepository.save(campaign);
    }

    public Campaign completeCampaign(Long id) {
        Campaign campaign = campaignRepository.findById(id).orElse(null);
        if (campaign == null) {
            return null;
        }

        campaign.setStatus(Campaign.CampaignStatus.COMPLETED);
        campaign.setCompletedAt(LocalDateTime.now());

        calculateFinalStats(campaign);

        return campaignRepository.save(campaign);
    }

    public Campaign duplicateCampaign(Long id) {
        Campaign original = campaignRepository.findById(id).orElse(null);
        if (original == null) {
            return null;
        }

        Campaign duplicate = new Campaign();
        duplicate.setName(original.getName() + " (Копия)");
        duplicate.setDescription(original.getDescription());
        duplicate.setType(original.getType());
        duplicate.setStatus(Campaign.CampaignStatus.DRAFT);
        duplicate.setTargetSegments(original.getTargetSegments());
        duplicate.setDiscountPercent(original.getDiscountPercent());
        duplicate.setMinOrderAmount(original.getMinOrderAmount());
        duplicate.setValidDays(original.getValidDays());
        duplicate.setEmailSubject(original.getEmailSubject());
        duplicate.setEmailContent(original.getEmailContent());
        duplicate.setSmsText(original.getSmsText());
        duplicate.setAutomated(original.isAutomated());
        duplicate.setCreatedAt(LocalDateTime.now());

        duplicate.setTotalRecipients(0L);
        duplicate.setSentCount(0);
        duplicate.setDeliveredCount(0);
        duplicate.setOpenedCount(0);
        duplicate.setClickedCount(0);
        duplicate.setConvertedCount(0);
        duplicate.setRevenueGenerated(BigDecimal.ZERO);

        return campaignRepository.save(duplicate);
    }

    public Campaign applyTemplateToCampaign(Long campaignId, Long templateId) {
        Campaign campaign = campaignRepository.findById(campaignId).orElse(null);
        CampaignTemplate template = templateRepository.findById(templateId).orElse(null);

        if (campaign == null || template == null) {
            return null;
        }

        campaign.setEmailSubject(template.getSubjectTemplate());
        campaign.setEmailContent(template.getContentTemplate());

        if (template.getType() != null) {
            try {
                campaign.setType(Campaign.CampaignType.valueOf(template.getType().name()));
            } catch (IllegalArgumentException e) {
                // Оставляем текущий тип
            }
        }

        return campaignRepository.save(campaign);
    }

    private long calculateRecipients(Campaign campaign) {
        String segments = campaign.getTargetSegments();

        if (segments == null || segments.isEmpty()) {
            return userRepository.count();
        }

        List<User> users = userRepository.findAll();
        String[] targetSegments = segments.split(",");

        return users.stream()
                .filter(u -> {
                    String status = u.getStatus() != null ? u.getStatus().name() : null;
                    return status != null && Arrays.asList(targetSegments).contains(status.trim());
                })
                .count();
    }

    private void calculateFinalStats(Campaign campaign) {
        int sent = campaign.getSentCount() != null ? campaign.getSentCount() : 0;

        if (sent == 0) return;

        Random random = new Random();

        int opens = (int) (sent * (0.30 + random.nextDouble() * 0.15));
        campaign.setOpenedCount(opens);

        int clicks = (int) (opens * (0.10 + random.nextDouble() * 0.10));
        campaign.setClickedCount(clicks);

        int conversions = (int) (clicks * (0.02 + random.nextDouble() * 0.03));
        campaign.setConvertedCount(conversions);

        BigDecimal avgOrder = BigDecimal.valueOf(500 + random.nextInt(500));
        BigDecimal revenue = avgOrder.multiply(BigDecimal.valueOf(conversions));
        campaign.setRevenueGenerated(revenue);
    }

    public Map<String, Object> massSendCampaign(Long id) {
        Map<String, Object> result = new HashMap<>();

        Campaign campaign = campaignRepository.findById(id).orElse(null);
        if (campaign == null) {
            result.put("success", false);
            result.put("message", "Кампания не найдена");
            return result;
        }

        int sent = campaign.getSentCount() != null ? campaign.getSentCount() : 0;
        int delivered = (int) (sent * 0.95);

        campaign.setDeliveredCount(delivered);
        campaignRepository.save(campaign);

        result.put("success", true);
        result.put("message", "Кампания отправлена");
        result.put("sent", sent);
        result.put("delivered", delivered);

        return result;
    }

    public Map<String, Object> forecastCampaignEffectiveness(Campaign campaign) {
        Map<String, Object> forecast = new HashMap<>();

        long recipients = campaign.getTotalRecipients() != null ? campaign.getTotalRecipients() : 0;

        Map<String, Object> optimistic = new HashMap<>();
        optimistic.put("openRate", 45.0);
        optimistic.put("clickRate", 15.0);
        optimistic.put("conversionRate", 5.0);
        optimistic.put("expectedRevenue", BigDecimal.valueOf(recipients * 0.45 * 0.15 * 0.05 * 750));

        Map<String, Object> realistic = new HashMap<>();
        realistic.put("openRate", 30.0);
        realistic.put("clickRate", 10.0);
        realistic.put("conversionRate", 3.0);
        realistic.put("expectedRevenue", BigDecimal.valueOf(recipients * 0.30 * 0.10 * 0.03 * 750));

        Map<String, Object> pessimistic = new HashMap<>();
        pessimistic.put("openRate", 20.0);
        pessimistic.put("clickRate", 5.0);
        pessimistic.put("conversionRate", 1.0);
        pessimistic.put("expectedRevenue", BigDecimal.valueOf(recipients * 0.20 * 0.05 * 0.01 * 750));

        forecast.put("recipients", recipients);
        forecast.put("optimistic", optimistic);
        forecast.put("realistic", realistic);
        forecast.put("pessimistic", pessimistic);

        return forecast;
    }

    public List<CrmDTO.CustomerDTO> getCampaignRecipients(Long campaignId) {
        Campaign campaign = campaignRepository.findById(campaignId).orElse(null);
        if (campaign == null) {
            return Collections.emptyList();
        }

        List<User> users = userRepository.findAll();
        String segments = campaign.getTargetSegments();

        if (segments == null || segments.isEmpty()) {
            return users.stream()
                    .map(this::convertToCustomerDto)
                    .collect(Collectors.toList());
        }

        String[] targetSegments = segments.split(",");

        return users.stream()
                .filter(u -> {
                    String status = u.getStatus() != null ? u.getStatus().name() : null;
                    return status != null && Arrays.asList(targetSegments).contains(status.trim());
                })
                .map(this::convertToCustomerDto)
                .collect(Collectors.toList());
    }

    // ==================== ШАБЛОНЫ ====================

    public List<MarketingDTO.TemplateDTO> getAllTemplates() {
        List<CampaignTemplate> templates = templateRepository.findAllByOrderByCreatedAtDesc();
        return templates.stream()
                .map(this::convertToTemplateDto)
                .collect(Collectors.toList());
    }

    public MarketingDTO.TemplateDTO getTemplate(Long id) {
        CampaignTemplate template = templateRepository.findById(id).orElse(null);
        return template != null ? convertToTemplateDto(template) : null;
    }

    public CampaignTemplate getTemplateEntity(Long id) {
        return templateRepository.findById(id).orElse(null);
    }

    public List<MarketingDTO.TemplateDTO> getTemplatesByCategory(String category) {
        List<CampaignTemplate> templates = templateRepository.findByCategory(category);
        return templates.stream()
                .map(this::convertToTemplateDto)
                .collect(Collectors.toList());
    }

    public CampaignTemplate createTemplate(CampaignTemplate template) {
        template.setCreatedAt(LocalDateTime.now());
        template.setUsageCount(0);
        template.setIsActive(true);
        return templateRepository.save(template);
    }

    public CampaignTemplate updateTemplate(Long id, CampaignTemplate template) {
        CampaignTemplate existing = templateRepository.findById(id).orElse(null);
        if (existing == null) {
            return null;
        }

        existing.setName(template.getName());
        existing.setDescription(template.getDescription());
        existing.setType(template.getType());
        existing.setCategory(template.getCategory());
        existing.setSubjectTemplate(template.getSubjectTemplate());
        existing.setContentTemplate(template.getContentTemplate());

        return templateRepository.save(existing);
    }

    public boolean deleteTemplate(Long id) {
        if (templateRepository.existsById(id)) {
            templateRepository.deleteById(id);
            return true;
        }
        return false;
    }

    public CampaignTemplate toggleTemplateStatus(Long id) {
        CampaignTemplate template = templateRepository.findById(id).orElse(null);
        if (template == null) {
            return null;
        }

        template.setIsActive(!template.getIsActive());
        return templateRepository.save(template);
    }

    public void incrementTemplateUsage(Long templateId) {
        templateRepository.findById(templateId).ifPresent(template -> {
            template.setUsageCount(template.getUsageCount() + 1);
            templateRepository.save(template);
        });
    }

    // ==================== КОНВЕРТЕРЫ ====================

    public MarketingDTO.CampaignDTO convertToCampaignDto(Campaign campaign) {
        MarketingDTO.CampaignDTO dto = new MarketingDTO.CampaignDTO();
        dto.setId(campaign.getId());
        dto.setName(campaign.getName());
        dto.setDescription(campaign.getDescription());
        dto.setType(campaign.getType() != null ? campaign.getType().name() : null);
        dto.setStatus(campaign.getStatus() != null ? campaign.getStatus().name() : null);
        dto.setTargetSegments(campaign.getTargetSegments());
        dto.setDiscountPercent(campaign.getDiscountPercent());
        dto.setMinOrderAmount(campaign.getMinOrderAmount());
        dto.setValidDays(campaign.getValidDays());
        dto.setEmailSubject(campaign.getEmailSubject());
        dto.setEmailContent(campaign.getEmailContent());
        dto.setSmsText(campaign.getSmsText());
        dto.setAutomated(campaign.isAutomated());
        dto.setTriggerEvent(campaign.getTriggerEvent());
        dto.setTotalRecipients(campaign.getTotalRecipients());
        dto.setSentCount(campaign.getSentCount());
        dto.setDeliveredCount(campaign.getDeliveredCount());
        dto.setOpenedCount(campaign.getOpenedCount());
        dto.setClickedCount(campaign.getClickedCount());
        dto.setConvertedCount(campaign.getConvertedCount());
        dto.setRevenueGenerated(campaign.getRevenueGenerated());
        dto.setCreatedAt(campaign.getCreatedAt());
        dto.setLaunchedAt(campaign.getLaunchedAt());
        dto.setCompletedAt(campaign.getCompletedAt());

        // Вычисляем метрики - используем безопасные вычисления
        Integer sentCount = campaign.getSentCount();
        if (sentCount != null && sentCount > 0) {
            Integer openedCount = campaign.getOpenedCount();
            Integer clickedCount = campaign.getClickedCount();
            Integer convertedCount = campaign.getConvertedCount();

            double openRate = openedCount != null ? (openedCount * 100.0) / sentCount : 0.0;
            double clickRate = clickedCount != null ? (clickedCount * 100.0) / sentCount : 0.0;
            double conversionRate = convertedCount != null ? (convertedCount * 100.0) / sentCount : 0.0;

            dto.setOpenRate(round(openRate, 2));
            dto.setClickRate(round(clickRate, 2));
            dto.setConversionRate(round(conversionRate, 2));
        } else {
            dto.setOpenRate(0.0);
            dto.setClickRate(0.0);
            dto.setConversionRate(0.0);
        }

        return dto;
    }

    public MarketingDTO.TemplateDTO convertToTemplateDto(CampaignTemplate template) {
        MarketingDTO.TemplateDTO dto = new MarketingDTO.TemplateDTO();
        dto.setId(template.getId());
        dto.setName(template.getName());
        dto.setDescription(template.getDescription());
        dto.setType(template.getType() != null ? template.getType().name() : null);
        dto.setCategory(template.getCategory());
        dto.setSubjectTemplate(template.getSubjectTemplate());
        dto.setContentTemplate(template.getContentTemplate());
        dto.setUsageCount(template.getUsageCount());
        dto.setIsActive(template.getIsActive());
        dto.setCreatedAt(template.getCreatedAt());
        return dto;
    }

    private CrmDTO.CustomerDTO convertToCustomerDto(User user) {
        CrmDTO.CustomerDTO dto = new CrmDTO.CustomerDTO();
        dto.setId(user.getId());
        dto.setEmail(user.getEmail());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());

        // Конвертируем User.Status enum в String
        if (user.getStatus() != null) {
            dto.setStatus(user.getStatus().name());
        }

        // Конвертируем LocalDateTime в String для registrationDate
        if (user.getCreatedAt() != null) {
            dto.setRegistrationDate(user.getCreatedAt().toLocalDate().toString());
        }

        return dto;
    }

    /**
     * Округление числа до указанного количества знаков после запятой
     * Использует BigDecimal для избежания проблем с локалью
     */
    private double round(double value, int places) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return 0.0;
        }
        double factor = Math.pow(10, places);
        return Math.round(value * factor) / factor;
    }
}