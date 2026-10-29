package com.designstudio.api;

import com.designstudio.application.dto.CrmDTO;
import com.designstudio.application.service.CrmService;
import com.designstudio.domain.entity.CustomerSegment;
import com.designstudio.domain.entity.User;
import com.designstudio.domain.entity.Visit;
import com.designstudio.domain.repository.UserRepository;
import com.designstudio.domain.repository.VisitRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/crm")
@CrossOrigin(origins = "*")
public class CrmController {

    private final CrmService crmService;
    private final UserRepository userRepository;
    private final VisitRepository visitRepository;

    public CrmController(CrmService crmService,
                         UserRepository userRepository,
                         VisitRepository visitRepository) {
        this.crmService = crmService;
        this.userRepository = userRepository;
        this.visitRepository = visitRepository;
    }

    // Получить статистику CRM с трендами
    @GetMapping("/stats")
    public ResponseEntity<CrmDTO.CrmStatsDTO> getCrmStats() {
        return ResponseEntity.ok(crmService.getCrmStats());
    }

    // Получить всех клиентов
    @GetMapping("/customers")
    public ResponseEntity<List<CrmDTO.CustomerDTO>> getAllCustomers() {
        return ResponseEntity.ok(crmService.getAllCustomers());
    }

    // Получить клиентов по сегменту
    @GetMapping("/customers/segment/{segment}")
    public ResponseEntity<List<CrmDTO.CustomerDTO>> getCustomersBySegment(
            @PathVariable String segment) {
        return ResponseEntity.ok(crmService.getCustomersBySegment(segment));
    }

    // Получить все сегменты
    @GetMapping("/segments")
    public ResponseEntity<List<CrmDTO.SegmentDTO>> getAllSegments() {
        return ResponseEntity.ok(crmService.getAllSegments());
    }

    // Создать сегмент
    @PostMapping("/segments")
    public ResponseEntity<?> createSegment(@RequestBody SegmentRequest request) {
        try {
            CustomerSegment segment = new CustomerSegment();
            segment.setName(request.getName());
            segment.setDescription(request.getDescription());
            segment.setColor(request.getColor());
            segment.setIcon(request.getIcon());

            CustomerSegment created = crmService.createSegment(segment);
            return ResponseEntity.ok(created);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse("Ошибка создания сегмента: " + e.getMessage()));
        }
    }

    // Обновить сегмент
    @PutMapping("/segments/{id}")
    public ResponseEntity<?> updateSegment(
            @PathVariable Long id,
            @RequestBody SegmentRequest request) {
        try {
            CustomerSegment updated = new CustomerSegment();
            updated.setName(request.getName());
            updated.setDescription(request.getDescription());
            updated.setColor(request.getColor());
            updated.setIcon(request.getIcon());

            CustomerSegment result = crmService.updateSegment(id, updated);
            if (result == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse("Ошибка обновления сегмента: " + e.getMessage()));
        }
    }

    // Удалить сегмент
    @DeleteMapping("/segments/{id}")
    public ResponseEntity<?> deleteSegment(@PathVariable Long id) {
        try {
            if (crmService.deleteSegment(id)) {
                return ResponseEntity.ok().build();
            }
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse("Ошибка удаления сегмента: " + e.getMessage()));
        }
    }

    // Получить RFM анализ
    @GetMapping("/rfm-analysis")
    public ResponseEntity<List<CrmDTO.RfmAnalysisDTO>> getRfmAnalysis() {
        CrmDTO.CrmStatsDTO stats = crmService.getCrmStats();
        List<CrmDTO.RfmAnalysisDTO> rfm = stats.getRfmAnalysis();
        if (rfm == null) rfm = new ArrayList<>();
        return ResponseEntity.ok(rfm);
    }

    // Получить ABC анализ
    @GetMapping("/abc-analysis")
    public ResponseEntity<List<CrmDTO.AbcAnalysisDTO>> getAbcAnalysis() {
        CrmDTO.CrmStatsDTO stats = crmService.getCrmStats();
        List<CrmDTO.AbcAnalysisDTO> abc = stats.getAbcAnalysis();
        if (abc == null) abc = new ArrayList<>();
        return ResponseEntity.ok(abc);
    }

    // Получить данные для воронки продаж
    @GetMapping("/funnel")
    public ResponseEntity<CrmDTO.FunnelDTO> getSalesFunnel() {
        CrmDTO.FunnelDTO funnel = crmService.getSalesFunnel();
        return ResponseEntity.ok(funnel);
    }

    // Получить статистику активности
    @GetMapping("/activity")
    public ResponseEntity<CrmDTO.ActivityStatsDTO> getActivityStats(
            @RequestParam(defaultValue = "month") String period) {
        CrmDTO.ActivityStatsDTO stats = crmService.getActivityStats(period);
        return ResponseEntity.ok(stats);
    }

    // ========== ЗАПИСЬ ПОСЕЩЕНИЯ ==========
    @PostMapping("/visit")
    public ResponseEntity<?> recordVisit(@RequestBody VisitRequest request) {
        try {
            User user = userRepository.findById(request.getUserId()).orElse(null);
            if (user == null) {
                return ResponseEntity.badRequest().body(new ErrorResponse("Пользователь не найден"));
            }

            Visit visit = new Visit();
            visit.setUser(user);
            visit.setVisitTime(LocalDateTime.now());
            visit.setDurationSeconds(0);
            visit.setSessionId(request.getSessionId() != null ? request.getSessionId() : java.util.UUID.randomUUID().toString());

            Visit saved = visitRepository.save(visit);
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Ошибка записи посещения: " + e.getMessage()));
        }
    }

    // ========== ОБНОВЛЕНИЕ ДЛИТЕЛЬНОСТИ ПОСЕЩЕНИЯ ==========
    @PostMapping("/visit/update")
    public ResponseEntity<?> updateVisitDuration(@RequestBody VisitUpdateRequest request) {
        try {
            User user = userRepository.findById(request.getUserId()).orElse(null);
            if (user == null) {
                return ResponseEntity.badRequest().body(new ErrorResponse("Пользователь не найден"));
            }

            Visit lastVisit = visitRepository.findTopByUserOrderByVisitTimeDesc(user);
            if (lastVisit != null) {
                lastVisit.setDurationSeconds(request.getDurationSeconds());
                visitRepository.save(lastVisit);
                return ResponseEntity.ok().build();
            }
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Ошибка обновления: " + e.getMessage()));
        }
    }

    // DTO для запроса сегмента
    public static class SegmentRequest {
        private String name;
        private String description;
        private String color;
        private String icon;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getColor() { return color; }
        public void setColor(String color) { this.color = color; }
        public String getIcon() { return icon; }
        public void setIcon(String icon) { this.icon = icon; }
    }

    // DTO для записи посещения
    public static class VisitRequest {
        private Long userId;
        private String sessionId;

        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        public String getSessionId() { return sessionId; }
        public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    }

    // DTO для обновления длительности
    public static class VisitUpdateRequest {
        private Long userId;
        private Integer durationSeconds;

        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        public Integer getDurationSeconds() { return durationSeconds; }
        public void setDurationSeconds(Integer durationSeconds) { this.durationSeconds = durationSeconds; }
    }

    public static class ErrorResponse {
        private String error;

        public ErrorResponse(String error) {
            this.error = error;
        }

        public String getError() { return error; }
    }
}