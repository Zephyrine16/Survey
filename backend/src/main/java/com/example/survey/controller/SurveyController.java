package com.example.survey.controller;

import com.example.survey.dto.CategorySubmissionDTO;
import com.example.survey.repository.AnswerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.HtmlUtils;
import jakarta.validation.Valid;

import java.util.List;

@RestController
@CrossOrigin(origins = {"http://localhost:5173", "http://127.0.0.1:5173"})
public class SurveyController {

    @Autowired
    private AnswerRepository answerRepository;

    @Autowired
    private com.example.survey.repository.MenuItemRepository menuItemRepository;

    @Autowired
    private com.example.survey.repository.QuestionRepository questionRepository;

    @Autowired
    private com.example.survey.repository.OptionRepository optionRepository;

    // ==========================================
    // 1. SAVE SURVEY ANSWERS
    // ==========================================

    @PostMapping("/submit-category")
    public ResponseEntity<?> submitCategoryAnswers(@Valid @RequestBody com.example.survey.dto.SurveySubmissionRequest request) {

        // 1. THE HONEYPOT TRAP: Check this BEFORE touching the database
        if (request.getPhoneNumber() != null && !request.getPhoneNumber().trim().isEmpty()) {
            System.out.println("🛡️ Bot blocked! Honeypot field was triggered.");
            // Return a fake success so the bot goes away and doesn't try a different attack
            return ResponseEntity.ok().body("{\"message\": \"Category saved successfully!\"}");
        }

        try {
            // Extract the actual answers array from the wrapper
            List<CategorySubmissionDTO> payload = request.getAnswers();

            // Synchronized block prevents race conditions for concurrent requests
            synchronized (this) {
                Long totalParticipants = answerRepository.countTotalParticipants();
                if(totalParticipants != null && totalParticipants >= 30) {
                    return ResponseEntity.badRequest().body("{\"error\": \"LIMIT_REACHED\"}");
                }

                for (CategorySubmissionDTO dto : payload) {
                    if (dto.getTextResponse() != null && !dto.getTextResponse().isEmpty()) {
                        String cleanText = HtmlUtils.htmlEscape(dto.getTextResponse());
                        if (cleanText.length() > 250) {
                            cleanText = cleanText.substring(0, 250);
                        }
                        dto.setTextResponse(cleanText);
                    }

                    answerRepository.saveRawAnswer(
                            dto.getUserId(),
                            dto.getMenuItemId(),
                            dto.getQuestionId(),
                            dto.getSelectedOptionId(),
                            dto.getTextResponse()
                    );
                }
            }

            return ResponseEntity.ok().body("{\"message\": \"Category saved successfully!\"}");

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("{\"error\": \"Failed to save data.\"}");
        }
    }

    @GetMapping("/api/stats/survey-status")
    public ResponseEntity<?> checkSurveyStatus() {
        Long totalParticipants = answerRepository.countTotalParticipants();
        boolean isFull = (totalParticipants != null && totalParticipants >= 30);

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
        List<Object[]> data = answerRepository.getExportData();

        StringBuilder csv = new StringBuilder("\uFEFFSession ID,Menu Item,Question,Selected Option,Text Response\n");

        for (Object[] row : data) {
            String userId = row[0] != null ? row[0].toString() : "Anonymous";
            if(userId.startsWith("=") || userId.startsWith("+") || userId.startsWith("-") || userId.startsWith("@")) {
                userId = "'" + userId;
            }

            String item = row[1] != null ? row[1].toString().replace(",", "") : "";
            if(item.startsWith("=") || item.startsWith("+") || item.startsWith("-") || item.startsWith("@")) {
                item = "'" + item;
            }

            String question = row[2] != null ? row[2].toString().replace(",", "") : "";
            if(question.startsWith("=") || question.startsWith("+") || question.startsWith("-") || question.startsWith("@")) {
                question = "'" + question;
            }

            String option = row[3] != null ? row[3].toString().replace(",", "") : "";
            if(option.startsWith("=") || option.startsWith("+") || option.startsWith("-") || option.startsWith("@")) {
                option = "'" + option;
            }

            String textResponse = row[4] != null ? row[4].toString().replace("\n", " ").replace(",", "") : "";
            if(textResponse.startsWith("=") || textResponse.startsWith("+") || textResponse.startsWith("-") || textResponse.startsWith("@")) {
                textResponse = "'" + textResponse;
            }

            csv.append(userId).append(",")
                    .append(item).append(",")
                    .append(question).append(",")
                    .append(option).append(",")
                    .append(textResponse).append("\n");
        }

        byte[] csvBytes = csv.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);

        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=CafeRater_Analytics.csv")
                .header("Content-Type", "text/csv; charset=UTF-8")
                .body(csvBytes);
    }

    // ==========================================
    // 3. FETCH REAL DASHBOARD STATISTICS
    // ==========================================

    @GetMapping("/analytics/stats/{menuItemId}")
    public ResponseEntity<com.example.survey.dto.DashboardStatsDTO> getItemStats(@PathVariable Long menuItemId) {
        com.example.survey.dto.DashboardStatsDTO stats = new com.example.survey.dto.DashboardStatsDTO();

        // Fetch base counts (Cleaned up duplicate lines here!)
        stats.globalTotal = answerRepository.countGlobalTotalResponses();
        stats.itemTotal = answerRepository.countTotalResponsesForItem(menuItemId);

        // Fetch text reviews for sentiment analysis
        List<String> reviews = answerRepository.getTextReviewsForItem(menuItemId);

        // Simple Sentiment Analyzer Logic
        for (String review : reviews) {
            String lower = review.toLowerCase();
            if (lower.matches(".*\\b(good|great|love|best|delicious|yummy|perfect|nice|amazing|sweet|comfort|favorite|warm|fresh|hot|filling)\\b.*")) {
                stats.positiveCount++;
            } else if (lower.matches(".*\\b(bad|hate|awful|terrible|gross|expensive|worse|bland|nasty|disgusting|dry|salty|cold|hard|stale)\\b.*")) {
                stats.negativeCount++;
            } else {
                stats.neutralCount++;
            }
        }

        // Calculate Percentages
        int totalAnalyzed = stats.positiveCount + stats.neutralCount + stats.negativeCount;
        if (totalAnalyzed > 0) {
            stats.positivePct = Math.round(((double) stats.positiveCount / totalAnalyzed) * 1000.0) / 10.0;
            stats.neutralPct = Math.round(((double) stats.neutralCount / totalAnalyzed) * 1000.0) / 10.0;
            stats.negativePct = Math.round(((double) stats.negativeCount / totalAnalyzed) * 1000.0) / 10.0;
        }

        // Keyword Extractor
        java.util.List<String> stopWords = java.util.Arrays.asList(
                "the", "and", "is", "it", "to", "a", "of", "for", "in", "on", "this", "but",
                "very", "so", "with", "i", "was", "not", "have", "that", "like", "just", "my"
        );

        java.util.Map<String, Integer> wordCounts = new java.util.HashMap<>();
        for(String review : reviews) {
            // FIXED: Added a '+' to \\s so it safely removes double-spaces
            String[] words = review.toLowerCase().replaceAll("[^a-z\\s]", "").split("\\s+");
            for(String word : words) {
                if(word.length() > 2 && !stopWords.contains(word)) {
                    wordCounts.put(word, wordCounts.getOrDefault(word, 0) + 1);
                }
            }
        }

        java.util.List<java.util.Map.Entry<String, Integer>> sortedWords = new java.util.ArrayList<>(wordCounts.entrySet());
        sortedWords.sort((a, b) -> b.getValue().compareTo(a.getValue()));

        stats.topKeywords = new java.util.ArrayList<>();
        for(int i = 0; i < Math.min(8, sortedWords.size()); i++) {
            stats.topKeywords.add(sortedWords.get(i).getKey());
        }

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
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("{\"error\": \"Failed to wipe data.\"}");
        }
    }

    // ==========================================
    // 5. ADMIN TOOLS: MENU ITEM MANAGEMENT (CRUD)
    // ==========================================

    @PostMapping("/api/admin/menu-items")
    public ResponseEntity<com.example.survey.model.MenuItem> createMenuItem(@RequestBody com.example.survey.model.MenuItem newItem) {
        com.example.survey.model.MenuItem savedItem = menuItemRepository.save(newItem);
        return ResponseEntity.status(org.springframework.http.HttpStatus.CREATED).body(savedItem);
    }

    @PutMapping("/api/admin/menu-items/{id}")
    public ResponseEntity<com.example.survey.model.MenuItem> updateMenuItem(@PathVariable Long id, @RequestBody com.example.survey.model.MenuItem updatedData) {
        java.util.Optional<com.example.survey.model.MenuItem> existingItem = menuItemRepository.findById(id);

        if (existingItem.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        com.example.survey.model.MenuItem itemToUpdate = existingItem.get();
        itemToUpdate.setName(updatedData.getName());
        itemToUpdate.setCategory(updatedData.getCategory());
        // If you have subCategory or price, add them here:
        // itemToUpdate.setSubCategory(updatedData.getSubCategory());
        itemToUpdate.setImageName(updatedData.getImageName());

        com.example.survey.model.MenuItem savedItem = menuItemRepository.save(itemToUpdate);
        return ResponseEntity.ok(savedItem);
    }

    @DeleteMapping("/api/admin/menu-items/{id}")
    public ResponseEntity<Void> deleteMenuItem(@PathVariable Long id) {
        if (!menuItemRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }

        // Delete the item
        menuItemRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // ==========================================
    // 6. ADMIN TOOLS: QUESTION & OPTION MANAGEMENT
    // ==========================================

    // --- QUESTION CRUD ---

    @PostMapping("/api/admin/questions")
    public ResponseEntity<com.example.survey.model.Question> createQuestion(@RequestBody com.example.survey.model.Question newQuestion) {
        com.example.survey.model.Question savedQuestion = questionRepository.save(newQuestion);
        return ResponseEntity.status(org.springframework.http.HttpStatus.CREATED).body(savedQuestion);
    }

    @PutMapping("/api/admin/questions/{id}")
    public ResponseEntity<com.example.survey.model.Question> updateQuestion(@PathVariable Long id, @RequestBody com.example.survey.model.Question updatedData) {
        java.util.Optional<com.example.survey.model.Question> existingQuestion = questionRepository.findById(id);

        if (existingQuestion.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        com.example.survey.model.Question qToUpdate = existingQuestion.get();
        qToUpdate.setText(updatedData.getText());
        qToUpdate.setQuestionType(updatedData.getQuestionType()); // "RADIO" or "TEXT"

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
            @RequestBody com.example.survey.model.Option newOption) {

        java.util.Optional<com.example.survey.model.Question> questionOpt = questionRepository.findById(questionId);

        if (questionOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        // Attach the option to the specific question
        com.example.survey.model.Question parentQuestion = questionOpt.get();
        newOption.setQuestion(parentQuestion);

        com.example.survey.model.Option savedOption = optionRepository.save(newOption);
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
}