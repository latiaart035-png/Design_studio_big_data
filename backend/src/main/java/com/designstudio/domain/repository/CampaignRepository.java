package com.designstudio.domain.repository;

import com.designstudio.domain.entity.Campaign;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CampaignRepository extends JpaRepository<Campaign, Long> {

    List<Campaign> findAllByOrderByCreatedAtDesc();

    List<Campaign> findByStatus(Campaign.CampaignStatus status);

    List<Campaign> findByStatusIn(List<Campaign.CampaignStatus> statuses);

    List<Campaign> findByLaunchedAtBetween(LocalDateTime start, LocalDateTime end);

    @Query("SELECT c FROM Campaign c WHERE c.launchedAt >= :startDate ORDER BY c.launchedAt DESC")
    List<Campaign> findRecentCampaigns(@Param("startDate") LocalDateTime startDate);
}