package com.designstudio.domain.repository;

import com.designstudio.domain.entity.CampaignTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CampaignTemplateRepository extends JpaRepository<CampaignTemplate, Long> {

    List<CampaignTemplate> findAllByOrderByCreatedAtDesc();

    List<CampaignTemplate> findByCategory(String category);

    List<CampaignTemplate> findByType(CampaignTemplate.TemplateType type);

    List<CampaignTemplate> findByCategoryAndType(String category, CampaignTemplate.TemplateType type);

    List<CampaignTemplate> findByIsActiveTrue();

    List<CampaignTemplate> findByIsActiveFalse();
}