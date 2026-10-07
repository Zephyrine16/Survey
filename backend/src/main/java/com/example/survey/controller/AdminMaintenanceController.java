package com.example.survey.controller;

import com.example.survey.repository.AnswerRepository;
import com.example.survey.repository.MenuItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** Bulk data removal is an explicitly enabled development-only operation. */
@RestController
@Profile("!prod")
@RequiredArgsConstructor
public class AdminMaintenanceController {
    private final AnswerRepository answerRepository;
    private final MenuItemRepository menuItemRepository;

    @Value("${security.maintenance.enabled:false}")
    private boolean enabled;

    @DeleteMapping("/api/admin/clear-data")
    @Transactional
    public ResponseEntity<?> clearAllData() {
        if (!enabled) return ResponseEntity.status(403).body(Map.of("error", "Bulk deletion is disabled"));
        answerRepository.deleteAllInBatch();
        return ResponseEntity.ok(Map.of("message", "All database records wiped!"));
    }

    @DeleteMapping("/api/admin/menu-items")
    @Transactional
    public ResponseEntity<?> deleteAllMenuItems() {
        if (!enabled) return ResponseEntity.status(403).body(Map.of("error", "Bulk deletion is disabled"));
        menuItemRepository.detachAllAnswersFromMenuItems();
        menuItemRepository.deleteAllInBatch();
        return ResponseEntity.noContent().build();
    }
}
