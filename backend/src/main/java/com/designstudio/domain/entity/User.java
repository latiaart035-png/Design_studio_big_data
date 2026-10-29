// com.designstudio.domain.entity.User.java
package com.designstudio.domain.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role = Role.USER;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.NEW;

    @Column(nullable = false)
    private Double discount = 0.0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Использованные кампании
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_used_campaigns", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "campaign_id")
    private Set<Long> usedCampaigns = new HashSet<>();

    // ===== НОВОЕ: Поля для квартальной системы скидок =====

    // Дата начала текущего квартала (для отслеживания сброса)
    @Column(name = "quarter_start_date")
    private LocalDateTime quarterStartDate;

    // Общая сумма покупок в текущем квартале
    @Column(name = "quarter_total_spent", nullable = false)
    private Double quarterTotalSpent = 0.0;

    @Column(name = "birth_date")
    private LocalDateTime birthDate;

    // Геттер и сеттер
    public LocalDateTime getBirthDate() { return birthDate; }
    public void setBirthDate(LocalDateTime birthDate) { this.birthDate = birthDate; }

    // ===== КОНЕЦ НОВЫХ ПОЛЕЙ =====

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public enum Role {
        USER, ADMIN
    }

    public enum Status {
        NEW, BASIC, ACTIVE, PREMIUM, VIP, LOST
    }

    // Геттеры и сеттеры
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public Double getDiscount() { return discount; }
    public void setDiscount(Double discount) { this.discount = discount; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public Set<Long> getUsedCampaigns() {
        if (usedCampaigns == null) usedCampaigns = new HashSet<>();
        return usedCampaigns;
    }
    public void setUsedCampaigns(Set<Long> usedCampaigns) { this.usedCampaigns = usedCampaigns; }

    // ===== НОВЫЕ ГЕТТЕРЫ И СЕТТЕРЫ =====
    public LocalDateTime getQuarterStartDate() { return quarterStartDate; }
    public void setQuarterStartDate(LocalDateTime quarterStartDate) { this.quarterStartDate = quarterStartDate; }
    public Double getQuarterTotalSpent() { return quarterTotalSpent != null ? quarterTotalSpent : 0.0; }
    public void setQuarterTotalSpent(Double quarterTotalSpent) { this.quarterTotalSpent = quarterTotalSpent; }
    // ===== КОНЕЦ НОВЫХ ГЕТТЕРОВ И СЕТТЕРОВ =====
}