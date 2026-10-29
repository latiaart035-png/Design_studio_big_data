package com.designstudio.application.service;

import com.designstudio.application.dto.MarketingDTO;
import com.designstudio.domain.entity.Campaign;
import com.designstudio.domain.entity.User;
import com.designstudio.domain.repository.CampaignRepository;
import com.designstudio.domain.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class UserCampaignService {

    private static final Logger logger = LoggerFactory.getLogger(UserCampaignService.class);

    private final CampaignRepository campaignRepository;
    private final UserRepository userRepository;
    private final MarketingService marketingService;

    public UserCampaignService(CampaignRepository campaignRepository,
                               UserRepository userRepository,
                               MarketingService marketingService) {
        this.campaignRepository = campaignRepository;
        this.userRepository = userRepository;
        this.marketingService = marketingService;
    }

    public List<MarketingDTO.CampaignDTO> getActiveCampaignsForUser(String email) {
        logger.info("Запрос кампаний для: {}", email);

        User user = userRepository.findByEmail(email);
        if (user == null) {
            logger.warn("Пользователь не найден: {}", email);
            return Collections.emptyList();
        }

        String userStatus = user.getStatus() != null ? user.getStatus().name() : "NEW";

        List<MarketingDTO.CampaignDTO> campaigns = campaignRepository
                .findByStatus(Campaign.CampaignStatus.RUNNING).stream()
                .filter(campaign -> isCampaignForUser(campaign, userStatus))
                .map(marketingService::convertToCampaignDto)
                .collect(Collectors.toList());

        logger.info("Найдено {} кампаний для {}", campaigns.size(), email);
        return campaigns;
    }

    public List<MarketingDTO.CampaignDTO> getAllAvailableCampaigns(String email) {
        User user = userRepository.findByEmail(email);
        if (user == null) return Collections.emptyList();

        String userStatus = user.getStatus() != null ? user.getStatus().name() : "NEW";

        return campaignRepository.findByStatus(Campaign.CampaignStatus.RUNNING).stream()
                .filter(campaign -> isCampaignForUser(campaign, userStatus))
                .map(marketingService::convertToCampaignDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public boolean applyCampaign(String email, Long campaignId) {
        User user = userRepository.findByEmail(email);
        Campaign campaign = campaignRepository.findById(campaignId).orElse(null);

        if (user == null || campaign == null) return false;
        if (campaign.getStatus() != Campaign.CampaignStatus.RUNNING) return false;

        if (user.getUsedCampaigns() == null) {
            user.setUsedCampaigns(new java.util.HashSet<>());
        }

        user.getUsedCampaigns().add(campaignId);
        userRepository.save(user);
        return true;
    }

    public BigDecimal calculateDiscount(String email, BigDecimal orderAmount, Long campaignId) {
        User user = userRepository.findByEmail(email);
        Campaign campaign = campaignRepository.findById(campaignId).orElse(null);

        if (user == null || campaign == null) return BigDecimal.ZERO;

        if (campaign.getMinOrderAmount() != null &&
                orderAmount.compareTo(BigDecimal.valueOf(campaign.getMinOrderAmount())) < 0) {
            return BigDecimal.ZERO;
        }

        if (user.getUsedCampaigns() != null && user.getUsedCampaigns().contains(campaignId)) {
            return BigDecimal.ZERO;
        }

        if (campaign.getDiscountPercent() != null && campaign.getDiscountPercent() > 0) {
            return orderAmount
                    .multiply(BigDecimal.valueOf(campaign.getDiscountPercent()))
                    .divide(BigDecimal.valueOf(100), 2, BigDecimal.ROUND_HALF_UP);
        }

        return BigDecimal.ZERO;
    }

    public Double getUserDiscountFromCampaigns(String email) {
        User user = userRepository.findByEmail(email);
        if (user == null) return 0.0;

        String userStatus = user.getStatus() != null ? user.getStatus().name() : "NEW";

        return campaignRepository.findByStatus(Campaign.CampaignStatus.RUNNING).stream()
                .filter(campaign -> isCampaignForUser(campaign, userStatus))
                .filter(campaign -> !isCampaignUsed(user, campaign.getId()))
                .map(Campaign::getDiscountPercent)
                .filter(d -> d != null && d > 0)
                .max(Double::compare)
                .orElse(0.0);
    }

    /**
     * Только АВТОМАТИЧЕСКИЕ скидки из кампаний
     */
    public Double getAutomaticDiscountForUser(String email) {
        User user = userRepository.findByEmail(email);
        if (user == null) return 0.0;

        String userStatus = user.getStatus() != null ? user.getStatus().name() : "NEW";

        return campaignRepository.findByStatus(Campaign.CampaignStatus.RUNNING).stream()
                .filter(campaign -> Boolean.TRUE.equals(campaign.isAutomated()))
                .filter(campaign -> isCampaignForUser(campaign, userStatus))
                .filter(campaign -> !isCampaignUsed(user, campaign.getId()))
                .map(Campaign::getDiscountPercent)
                .filter(d -> d != null && d > 0)
                .max(Double::compare)
                .orElse(0.0);
    }

    /**
     * Записать конверсию кампании (оформление заказа)
     */
    @Transactional
    public void recordCampaignConversion(String email, BigDecimal orderAmount) {
        User user = userRepository.findByEmail(email);
        if (user == null) return;

        String userStatus = user.getStatus() != null ? user.getStatus().name() : "NEW";

        Campaign campaign = campaignRepository.findByStatus(Campaign.CampaignStatus.RUNNING).stream()
                .filter(c -> Boolean.TRUE.equals(c.isAutomated()))
                .filter(c -> isCampaignForUser(c, userStatus))
                .filter(c -> !isCampaignUsed(user, c.getId()))
                .max(Comparator.comparing(Campaign::getDiscountPercent))
                .orElse(null);

        if (campaign != null) {
            campaign.setConvertedCount(
                    (campaign.getConvertedCount() != null ? campaign.getConvertedCount() : 0) + 1
            );
            campaign.setRevenueGenerated(
                    (campaign.getRevenueGenerated() != null ? campaign.getRevenueGenerated() : BigDecimal.ZERO)
                            .add(orderAmount)
            );
            campaignRepository.save(campaign);
            logger.info("Конверсия записана для кампании {}: +{} BYN", campaign.getId(), orderAmount);
        }
    }

    /**
     * Записать клик по кампании (применение промокода)
     */
    @Transactional
    public void recordCampaignClick(Long campaignId) {
        Campaign campaign = campaignRepository.findById(campaignId).orElse(null);
        if (campaign == null) return;

        campaign.setClickedCount(
                (campaign.getClickedCount() != null ? campaign.getClickedCount() : 0) + 1
        );
        campaignRepository.save(campaign);
        logger.info("Клик записан для кампании {}", campaignId);
    }

    public Long getActiveCampaignId(String email) {
        User user = userRepository.findByEmail(email);
        if (user == null) return null;

        String userStatus = user.getStatus() != null ? user.getStatus().name() : "NEW";

        return campaignRepository.findByStatus(Campaign.CampaignStatus.RUNNING).stream()
                .filter(campaign -> isCampaignForUser(campaign, userStatus))
                .filter(campaign -> !isCampaignUsed(user, campaign.getId()))
                .map(Campaign::getId)
                .findFirst()
                .orElse(null);
    }

    public void markCampaignAsUsed(String email, Long campaignId) {
        if (campaignId == null) return;

        User user = userRepository.findByEmail(email);
        if (user == null) return;

        if (user.getUsedCampaigns() == null) {
            user.setUsedCampaigns(new HashSet<>());
        }

        user.getUsedCampaigns().add(campaignId);
        userRepository.save(user);

        // Обновляем статистику кампании
        Campaign campaign = campaignRepository.findById(campaignId).orElse(null);
        if (campaign != null) {
            campaign.setConvertedCount((campaign.getConvertedCount() != null ? campaign.getConvertedCount() : 0) + 1);
            campaignRepository.save(campaign);
        }
    }

    private boolean isCampaignForUser(Campaign campaign, String userStatus) {
        String segments = campaign.getTargetSegments();
        if (segments == null || segments.isEmpty()) return true;
        return Arrays.stream(segments.split(","))
                .map(String::trim)
                .anyMatch(s -> s.equalsIgnoreCase(userStatus) || s.equalsIgnoreCase("ALL"));
    }

    private boolean isCampaignUsed(User user, Long campaignId) {
        return user.getUsedCampaigns() != null && user.getUsedCampaigns().contains(campaignId);
    }

    // Добавьте этот метод в UserCampaignService.java
    public void recordCampaignConversion(String email, double orderAmount) {
        recordCampaignConversion(email, BigDecimal.valueOf(orderAmount));
    }


}