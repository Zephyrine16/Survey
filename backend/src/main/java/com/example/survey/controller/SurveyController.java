package com.example.survey.controller;

import com.example.survey.config.SurveyProperties;
import com.example.survey.dto.CategorySubmissionDTO;
import com.example.survey.dto.MenuItemRequest;
import com.example.survey.dto.OptionRequest;
import com.example.survey.dto.QuestionRequest;
import com.example.survey.dto.DashboardStatsDTO;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import com.example.survey.repository.AnswerRepository;
import com.example.survey.service.AnalyticsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

@RestController
public class SurveyController {

    @Autowired
    private AnswerRepository answerRepository;

    @Autowired
    private com.example.survey.repository.MenuItemRepository menuItemRepository;

    @Autowired
    private com.example.survey.repository.QuestionRepository questionRepository;

    @Autowired
    private com.example.survey.repository.OptionRepository optionRepository;

    @Autowired
    private com.example.survey.service.SurveyService surveyService;

    @Autowired
    private AnalyticsService analyticsService;

    private static final Logger log = LoggerFactory.getLogger(SurveyController.class);

    @Autowired
    private SurveyProperties surveyProperties;

    // ==========================================
    // 1. SAVE SURVEY ANSWERS
    // ==========================================

    @PostMapping("/submit-category")
    public ResponseEntity<?> submitCategoryAnswers(
            @Valid @RequestBody com.example.survey.dto.SurveySubmissionRequest submissionRequest,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {

        if (isHoneypotTriggered(submissionRequest)) {
            log.info("Bot honeypot triggered on submit-category.");
            return successMessage();
        }

        try {
            List<CategorySubmissionDTO> categorySubmissions = submissionRequest.getAnswers();
            String participantId = getOrCreateParticipantId(httpRequest, httpResponse);
            assignParticipantToPayload(categorySubmissions, participantId);

            boolean savedUnderLimit = surveyService.saveSurveyIfUnderLimit(categorySubmissions);
            if(!savedUnderLimit) {
                return limitReachedResponse();
            }

            return successMessage();

        } catch (Exception e) {
            log.error("Failed to save survey category submission.", e);
            return ResponseEntity.internalServerError().body("{\"error\": \"Failed to save data.\"}");
        }
    }

    private boolean isHoneypotTriggered(com.example.survey.dto.SurveySubmissionRequest request) {
        return request.getPhoneNumber() != null && !request.getPhoneNumber().trim().isEmpty();
    }

    private void assignParticipantToPayload(List<CategorySubmissionDTO> payload, String participantId) {
        payload.forEach(dto -> dto.setUserId(participantId));
    }

    private ResponseEntity<String> successMessage() {
        return ResponseEntity.ok().body("{\"message\": \"Category saved successfully!\"}");
    }

    private ResponseEntity<String> limitReachedResponse() {
        return ResponseEntity.badRequest().body("{\"error\": \"LIMIT_REACHED\"}");
    }

    private ResponseEntity<String> failureMessage() {
        return ResponseEntity.internalServerError().body("{\"error\": \"Failed to save data.\"}");
    }

    private String getOrCreateParticipantId(HttpServletRequest request, HttpServletResponse response) {
        if(request.getCookies() != null) {
            for(Cookie cookie : request.getCookies()) {
                if(surveyProperties.getParticipantCookieName().equals(cookie.getName())) {
                    try {
                        UUID.fromString(cookie.getValue());
                        return cookie.getValue();
                    } catch(IllegalArgumentException ignored) {
                        log.warn("Invalid participant cookie format detected.");
                        break;
                    }
                }
            }
        }

        String participantId = UUID.randomUUID().toString();
        ResponseCookie responseCookie = ResponseCookie.from(
                        surveyProperties.getParticipantCookieName(),
                        participantId)
                .httpOnly(true)
                .secure(request.isSecure())
                .sameSite("Lax")
                .path("/")
                .maxAge(Duration.ofDays(surveyProperties.getParticipantCookieMaxAgeDays()))
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, responseCookie.toString());
        return participantId;
    }

    @GetMapping("/api/stats/survey-status")
    public ResponseEntity<?> checkSurveyStatus() {
        Long totalParticipants = answerRepository.countTotalParticipants();
        long limit = surveyProperties.getParticipantLimit();
        boolean isFull = limit > 0 && (totalParticipants != null && totalParticipants >= limit);

        return ResponseEntity.ok(java.util.Map.of(
                "isFull", isFull,
                "currentCount", totalParticipants != null ? totalParticipants : 0
        ));
    }

    // ==========================================
    // 2. EXPORT THE CSV
    // ==========================================

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportReport() {
        List<Object[]> exportRows = answerRepository.getExportData();

        StringBuilder csv = new StringBuilder("\uFEFF\"Session ID\",\"Menu Item\",\"Question\",\"Selected Option\",\"Text Response\"\n");

        for (Object[] exportRow : exportRows) {
            String userId = csvSafe(exportRow[0], "Anonymous");
            String menuItemName = csvSafe(exportRow[1], "");
            String question = csvSafe(exportRow[2], "");
            String option = csvSafe(exportRow[3], "");
            String textResponse = csvSafe(exportRow[4], "");

            csv.append(userId).append(",")
                    .append(menuItemName).append(",")
                    .append(question).append(",")
                    .append(option).append(",")
                    .append(textResponse).append("\n");
        }

        byte[] csvBytes = csv.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);

        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=" + surveyProperties.getExportFilename())
                .header("Content-Type", "text/csv; charset=UTF-8")
                .body(csvBytes);
    }

    // ==========================================
    // 3. FETCH REAL DASHBOARD STATISTICS
    // ==========================================

    @GetMapping("/analytics/stats/{menuItemId}")
    public ResponseEntity<DashboardStatsDTO> getItemStats(@PathVariable Long menuItemId) {
        DashboardStatsDTO stats = analyticsService.getItemStats(menuItemId);
        return ResponseEntity.ok(stats);
    }

    // Endpoint for Card 2 (Notice how it takes the Item ID in the URL!)
    @GetMapping("/api/stats/responses/{itemId}")
    public ResponseEntity<Long> getItemResponseCount(@PathVariable Long itemId) {
        Long count = answerRepository.countTotalResponsesForItem(itemId);
        return ResponseEntity.ok(count != null ? count : 0L);
    }

    //Endpoint for Card 1
    @GetMapping("/api/stats/baseline")
    public ResponseEntity<Long> getBaseLineCount() {
        Long lowestCount = answerRepository.getBaselineResponseCount();
        return ResponseEntity.ok(lowestCount != null ? lowestCount : 0L);
    }

    // ==========================================
    // 4. ADMIN TOOLS: NUKE DATABASE (For Testing)
    // ==========================================

    @DeleteMapping("/api/admin/clear-data")
    public ResponseEntity<?> clearAllData() {
        try {
            answerRepository.deleteAll();
            return ResponseEntity.ok().body("{\"message\": \"All database records wiped!\"}");
        } catch (Exception e) {
            log.error("Failed to clear survey data.", e);
            return ResponseEntity.internalServerError().body("{\"error\": \"Failed to wipe data.\"}");
        }
    }

    // ==========================================
    // 5. ADMIN TOOLS: MENU ITEM MANAGEMENT (CRUD)
    // ==========================================

    @PostMapping("/api/admin/menu-items")
    public ResponseEntity<com.example.survey.model.MenuItem> createMenuItem(@Valid @RequestBody MenuItemRequest newItem) {
        com.example.survey.model.MenuItem item = new com.example.survey.model.MenuItem();
        item.setName(newItem.getName().trim());
        item.setCategory(newItem.getCategory().trim());
        item.setPrice(newItem.getPrice());
        item.setImageName(newItem.getImageName());
        com.example.survey.model.MenuItem savedItem = menuItemRepository.save(item);
        return ResponseEntity.status(org.springframework.http.HttpStatus.CREATED).body(savedItem);
    }

    @PutMapping("/api/admin/menu-items/{id}")
    public ResponseEntity<com.example.survey.model.MenuItem> updateMenuItem(@PathVariable Long id, @Valid @RequestBody MenuItemRequest updatedData) {
        java.util.Optional<com.example.survey.model.MenuItem> existingItem = menuItemRepository.findById(id);

        if (existingItem.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        com.example.survey.model.MenuItem itemToUpdate = existingItem.get();
        itemToUpdate.setName(updatedData.getName().trim());
        itemToUpdate.setCategory(updatedData.getCategory().trim());
        itemToUpdate.setPrice(updatedData.getPrice());
        itemToUpdate.setImageName(updatedData.getImageName());

        com.example.survey.model.MenuItem savedItem = menuItemRepository.save(itemToUpdate);
        return ResponseEntity.ok(savedItem);
    }

    @DeleteMapping("/api/admin/menu-items/{id}")
    public ResponseEntity<Void> deleteMenuItem(@PathVariable Long id) {
        if (!menuItemRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }

        menuItemRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // ==========================================
    // 6. ADMIN TOOLS: QUESTION & OPTION MANAGEMENT
    // ==========================================

    // --- QUESTION CRUD ---

    @PostMapping("/api/admin/questions")
    public ResponseEntity<com.example.survey.model.Question> createQuestion(@Valid @RequestBody QuestionRequest newQuestion) {
        com.example.survey.model.Question question = new com.example.survey.model.Question();
        question.setText(newQuestion.getText().trim());
        question.setQuestionType(newQuestion.getType());
        com.example.survey.model.Question savedQuestion = questionRepository.save(question);
        return ResponseEntity.status(org.springframework.http.HttpStatus.CREATED).body(savedQuestion);
    }

    @PutMapping("/api/admin/questions/{id}")
    public ResponseEntity<com.example.survey.model.Question> updateQuestion(@PathVariable Long id, @Valid @RequestBody QuestionRequest updatedData) {
        java.util.Optional<com.example.survey.model.Question> existingQuestion = questionRepository.findById(id);

        if (existingQuestion.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        com.example.survey.model.Question qToUpdate = existingQuestion.get();
        qToUpdate.setText(updatedData.getText().trim());
        qToUpdate.setQuestionType(updatedData.getType());

        com.example.survey.model.Question savedQuestion = questionRepository.save(qToUpdate);
        return ResponseEntity.ok(savedQuestion);
    }

    @DeleteMapping("/api/admin/questions/{id}")
    public ResponseEntity<Void> deleteQuestion(@PathVariable Long id) {
        if (!questionRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        questionRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // --- OPTION CRUD (For RADIO Questions) ---

    @PostMapping("/api/admin/questions/{questionId}/options")
    public ResponseEntity<com.example.survey.model.Option> addOptionToQuestion(
            @PathVariable Long questionId,
            @Valid @RequestBody OptionRequest newOption) {

        java.util.Optional<com.example.survey.model.Question> questionOpt = questionRepository.findById(questionId);

        if (questionOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        // Attach the option to the specific question
        com.example.survey.model.Question parentQuestion = questionOpt.get();
        com.example.survey.model.Option option = new com.example.survey.model.Option();
        option.setLabel(newOption.getLabel().trim());
        option.setIcon(newOption.getIcon());
        option.setSubDescription(newOption.getSub());
        option.setQuestion(parentQuestion);

        com.example.survey.model.Option savedOption = optionRepository.save(option);
        return ResponseEntity.status(org.springframework.http.HttpStatus.CREATED).body(savedOption);
    }

    @DeleteMapping("/api/admin/options/{optionId}")
    public ResponseEntity<Void> deleteOption(@PathVariable Long optionId) {
        if (!optionRepository.existsById(optionId)) {
            return ResponseEntity.notFound().build();
        }
        optionRepository.deleteById(optionId);
        return ResponseEntity.noContent().build();
    }

    private String csvSafe(Object rawValue, String defaultValue) {
        String value = rawValue == null ? defaultValue : rawValue.toString();
        value = value.replace("\r", " ").replace("\n", " ");
        value = neutralizeSpreadsheetFormula(value);
        return escapeCsv(value);
    }

    private String neutralizeSpreadsheetFormula(String value) {
        String trimmed = value.stripLeading();
        if(trimmed.startsWith("=") || trimmed.startsWith("+") || trimmed.startsWith("-") ||
        trimmed.startsWith("@")) {
            return "'" + value;
        }

        return value;
    }

    private String escapeCsv(String value) {
        String escaped = value.replace("\"","\"\"");
        return "\"" + escaped + "\"";
    }
}