package com.designstudio.domain.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "campaigns")
public class Campaign {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CampaignType type = CampaignType.EMAIL;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CampaignStatus status = CampaignStatus.DRAFT;

    @Column(name = "target_segments")
    private String targetSegments;

    @Column(name = "discount_percent")
    private Double discountPercent;

    @Column(name = "min_order_amount")
    private Double minOrderAmount;

    @Column(name = "valid_days")
    private Integer validDays = 7;

    @Column(name = "email_subject")
    private String emailSubject;

    @Column(name = "email_content", columnDefinition = "TEXT")
    private String emailContent;

    @Column(name = "sms_text", columnDefinition = "TEXT")
    private String smsText;

    @Column(name = "is_automated")
    private boolean isAutomated;

    @Column(name = "trigger_event")
    private String triggerEvent;

    @Column(name = "start_date")
    private LocalDateTime startDate;

    @Column(name = "end_date")
    private LocalDateTime endDate;

    @Column(name = "total_recipients")
    private Long totalRecipients;

    @Column(name = "sent_count")
    private Integer sentCount;

    @Column(name = "delivered_count")
    private Integer deliveredCount;

    @Column(name = "opened_count")
    private Integer openedCount;

    @Column(name = "clicked_count")
    private Integer clickedCount;

    @Column(name = "converted_count")
    private Integer convertedCount;

    @Column(name = "revenue_generated", precision = 12, scale = 2)
    private BigDecimal revenueGenerated;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "launched_at")
    private LocalDateTime launchedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) status = CampaignStatus.DRAFT;
        if (sentCount == null) sentCount = 0;
        if (deliveredCount == null) deliveredCount = 0;
        if (openedCount == null) openedCount = 0;
        if (clickedCount == null) clickedCount = 0;
        if (convertedCount == null) convertedCount = 0;
        if (revenueGenerated == null) revenueGenerated = BigDecimal.ZERO;
        if (validDays == null) validDays = 7;
    }

    // Enums
    public enum CampaignType {
        EMAIL, SMS, PUSH, NOTIFICATION
    }

    public enum CampaignStatus {
        DRAFT, SCHEDULED, RUNNING, COMPLETED, CANCELLED, PAUSED
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public CampaignType getType() { return type; }
    public void setType(CampaignType type) { this.type = type; }

    public CampaignStatus getStatus() { return status; }
    public void setStatus(CampaignStatus status) { this.status = status; }

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

    public boolean isAutomated() { return isAutomated; }
    public void setAutomated(boolean automated) { isAutomated = automated; }

    public String getTriggerEvent() { return triggerEvent; }
    public void setTriggerEvent(String triggerEvent) { this.triggerEvent = triggerEvent; }

    public LocalDateTime getStartDate() { return startDate; }
    public void setStartDate(LocalDateTime startDate) { this.startDate = startDate; }

    public LocalDateTime getEndDate() { return endDate; }
    public void setEndDate(LocalDateTime endDate) { this.endDate = endDate; }

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

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getLaunchedAt() { return launchedAt; }
    public void setLaunchedAt(LocalDateTime launchedAt) { this.launchedAt = launchedAt; }

    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
}