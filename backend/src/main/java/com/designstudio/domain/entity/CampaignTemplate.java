package com.designstudio.domain.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "campaign_templates")
public class CampaignTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TemplateType type = TemplateType.EMAIL;

    @Column(nullable = false)
    private String category;

    @Column(name = "subject_template")
    private String subjectTemplate;

    @Column(name = "content_template", columnDefinition = "TEXT")
    private String contentTemplate;

    @Column(name = "usage_count")
    private Integer usageCount;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (usageCount == null) usageCount = 0;
        if (isActive == null) isActive = true;
    }

    // Enum
    public enum TemplateType {
        EMAIL, SMS
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public TemplateType getType() { return type; }
    public void setType(TemplateType type) { this.type = type; }

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