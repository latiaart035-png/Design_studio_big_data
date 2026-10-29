package com.designstudio.application.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class MarketingDTO {

    // ==================== MARKETING STATS DTO ====================
    public static class MarketingStatsDTO {
        private long totalCampaigns;
        private long activeCampaigns;
        private long totalRecipients;
        private double avgOpenRate;
        private double avgClickRate;
        private BigDecimal totalRevenue;

        // Getters and Setters
        public long getTotalCampaigns() { return totalCampaigns; }
        public void setTotalCampaigns(long totalCampaigns) { this.totalCampaigns = totalCampaigns; }

        public long getActiveCampaigns() { return activeCampaigns; }
        public void setActiveCampaigns(long activeCampaigns) { this.activeCampaigns = activeCampaigns; }

        public long getTotalRecipients() { return totalRecipients; }
        public void setTotalRecipients(long totalRecipients) { this.totalRecipients = totalRecipients; }

        public double getAvgOpenRate() { return avgOpenRate; }
        public void setAvgOpenRate(double avgOpenRate) { this.avgOpenRate = avgOpenRate; }

        public double getAvgClickRate() { return avgClickRate; }
        public void setAvgClickRate(double avgClickRate) { this.avgClickRate = avgClickRate; }

        public BigDecimal getTotalRevenue() { return totalRevenue; }
        public void setTotalRevenue(BigDecimal totalRevenue) { this.totalRevenue = totalRevenue; }
    }

    // ==================== CAMPAIGN DTO ====================
    public static class CampaignDTO {
        private Long id;
        private String name;
        private String description;
        private String type;
        private String status;
        private String targetSegments;
        private Double discountPercent;
        private Double minOrderAmount;
        private Integer validDays;
        private String emailSubject;
        private String emailContent;
        private String smsText;
        private Boolean automated;
        private String triggerEvent;
        private Long totalRecipients;
        private Integer sentCount;
        private Integer deliveredCount;
        private Integer openedCount;
        private Integer clickedCount;
        private Integer convertedCount;
        private BigDecimal revenueGenerated;
        private Double openRate;
        private Double clickRate;
        private Double conversionRate;
        private LocalDateTime createdAt;
        private LocalDateTime launchedAt;
        private LocalDateTime completedAt;

        // Getters and Setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

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

        public Boolean getAutomated() { return automated; }
        public void setAutomated(Boolean automated) { this.automated = automated; }

        public String getTriggerEvent() { return triggerEvent; }
        public void setTriggerEvent(String triggerEvent) { this.triggerEvent = triggerEvent; }

        public Long getTotalRecipients() { return totalRecipients; }
        public void setTotalRecipients(Long totalRecipients) { this.totalRecipients = totalRecipients; }

        public Integer getSentCount() { return sentCount; }
        public void setSentCount(Integer sentCount) { this.sentCount = sentCount; }

        public Integer getDeliveredCount() { return deliveredCount; }
        public void setDeliveredCount(Integer deliveredCount) { this.deliveredCount = deliveredCount; }

        public Integer getOpenedCount() { return openedCount; }
        public void setOpenedCount(Integer openedCount) { this.openedCount = openedCount; }

        public Integer getClickedCount() { return clickedCount; }
        public void setClickedCount(Integer clickedCount) { this.clickedCount = clickedCount; }

        public Integer getConvertedCount() { return convertedCount; }
        public void setConvertedCount(Integer convertedCount) { this.convertedCount = convertedCount; }

        public BigDecimal getRevenueGenerated() { return revenueGenerated; }
        public void setRevenueGenerated(BigDecimal revenueGenerated) { this.revenueGenerated = revenueGenerated; }

        public Double getOpenRate() { return openRate; }
        public void setOpenRate(Double openRate) { this.openRate = openRate; }

        public Double getClickRate() { return clickRate; }
        public void setClickRate(Double clickRate) { this.clickRate = clickRate; }

        public Double getConversionRate() { return conversionRate; }
        public void setConversionRate(Double conversionRate) { this.conversionRate = conversionRate; }

        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

        public LocalDateTime getLaunchedAt() { return launchedAt; }
        public void setLaunchedAt(LocalDateTime launchedAt) { this.launchedAt = launchedAt; }

        public LocalDateTime getCompletedAt() { return completedAt; }
        public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
    }

    // ==================== TEMPLATE DTO ====================
    public static class TemplateDTO {
        private Long id;
        private String name;
        private String description;
        private String type;
        private String category;
        private String subjectTemplate;
        private String contentTemplate;
        private Integer usageCount;
        private Boolean isActive;
        private LocalDateTime createdAt;

        // Getters and Setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }

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

        public Integer getUsageCount() { return usageCount; }
        public void setUsageCount(Integer usageCount) { this.usageCount = usageCount; }

        public Boolean getIsActive() { return isActive; }
        public void setIsActive(Boolean isActive) { this.isActive = isActive; }

        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    }

    public class StatsDTO {

        private Long totalCampaigns;
        private Long activeCampaigns;
        private Long totalRecipients;
        private Double avgOpenRate;
        private Double avgClickRate;
        private BigDecimal totalRevenue;

        // Constructors
        public StatsDTO() {}

        public StatsDTO(Long totalCampaigns, Long activeCampaigns, Long totalRecipients,
                        Double avgOpenRate, Double avgClickRate, BigDecimal totalRevenue) {
            this.totalCampaigns = totalCampaigns;
            this.activeCampaigns = activeCampaigns;
            this.totalRecipients = totalRecipients;
            this.avgOpenRate = avgOpenRate;
            this.avgClickRate = avgClickRate;
            this.totalRevenue = totalRevenue;
        }

        // Getters and Setters
        public Long getTotalCampaigns() { return totalCampaigns; }
        public void setTotalCampaigns(Long totalCampaigns) { this.totalCampaigns = totalCampaigns; }

        public Long getActiveCampaigns() { return activeCampaigns; }
        public void setActiveCampaigns(Long activeCampaigns) { this.activeCampaigns = activeCampaigns; }

        public Long getTotalRecipients() { return totalRecipients; }
        public void setTotalRecipients(Long totalRecipients) { this.totalRecipients = totalRecipients; }

        public Double getAvgOpenRate() { return avgOpenRate; }
        public void setAvgOpenRate(Double avgOpenRate) { this.avgOpenRate = avgOpenRate; }

        public Double getAvgClickRate() { return avgClickRate; }
        public void setAvgClickRate(Double avgClickRate) { this.avgClickRate = avgClickRate; }

        public BigDecimal getTotalRevenue() { return totalRevenue; }
        public void setTotalRevenue(BigDecimal totalRevenue) { this.totalRevenue = totalRevenue; }
    }

    // ==================== MESSAGE DTO ====================
    public static class MessageDTO {
        private Long id;
        private Long userId;
        private String email;
        private String subject;
        private String content;
        private String type;
        private LocalDateTime sentAt;
        private Boolean isRead;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getSubject() { return subject; }
        public void setSubject(String subject) { this.subject = subject; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public LocalDateTime getSentAt() { return sentAt; }
        public void setSentAt(LocalDateTime sentAt) { this.sentAt = sentAt; }
        public Boolean getIsRead() { return isRead; }
        public void setIsRead(Boolean isRead) { this.isRead = isRead; }
    }
}