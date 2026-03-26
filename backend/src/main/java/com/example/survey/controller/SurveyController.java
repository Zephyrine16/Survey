package com.example.survey.controller;

import com.example.survey.dto.CategorySubmissionDTO;
import com.example.survey.repository.AnswerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin(origins = {"http://localhost:5173", "http://127.0.0.1:5173"})
public class SurveyController {

    @Autowired
    private AnswerRepository answerRepository;

    // ==========================================
    // 1. SAVE SURVEY ANSWERS (Now with Anonymous IDs!)
    // ==========================================
    @PostMapping("/submit-category")
    public ResponseEntity<?> submitCategoryAnswers(@RequestBody List<CategorySubmissionDTO> payload) {
        try {
            // The Bouncer: Checks for the 200 limit
            Long totalSurveys = answerRepository.countGlobalTotalResponses();
            if(totalSurveys != null && totalSurveys >= 200) {
                return ResponseEntity.badRequest().body("{\"error\": \"LIMIT_REACHED\"}");
            }

            for (CategorySubmissionDTO dto : payload) {
                answerRepository.saveRawAnswer(
                        dto.getUserId(),           // <--- UPDATED: Grabs the random ID from Vue!
                        dto.getMenuItemId(),
                        dto.getQuestionId(),
                        dto.getSelectedOptionId(),
                        dto.getTextResponse()
                );
            }

            return ResponseEntity.ok().body("{\"message\": \"Category saved successfully!\"}");

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("{\"error\": \"Failed to save data.\"}");
        }
    }

    // ==========================================
    // 2. EXPORT THE CSV
    // ==========================================
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportReport() {
        List<Object[]> data = answerRepository.getExportData();

        // UPDATED: Changed "User Email" to "Session ID" to reflect our anonymous tracking!
        StringBuilder csv = new StringBuilder("\uFEFFSession ID,Menu Item,Question,Selected Option,Text Response\n");

        for (Object[] row : data) {
            String email = row[0] != null ? row[0].toString() : "Anonymous";
            String item = row[1] != null ? row[1].toString().replace(",", "") : "";
            String question = row[2] != null ? row[2].toString().replace(",", "") : "";
            String option = row[3] != null ? row[3].toString().replace(",", "") : "";
            String textResponse = row[4] != null ? row[4].toString().replace("\n", " ").replace(",", "") : "";

            csv.append(email).append(",")
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

        // Engagement Rate Math
        Long globalTextTotal = answerRepository.countGlobalTextResponses();
        if(stats.globalTotal > 0) {
            stats.engagementPct = Math.round(((double) globalTextTotal / stats.globalTotal) * 1000.0) / 10.0;
        } else {
            stats.engagementPct = 0.0;
        }

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

    // Endpoint for Card 1
    @GetMapping("/api/stats/participants")
    public ResponseEntity<Long> getParticipantCount() {
        Long count = answerRepository.countUniqueParticipants();
        // If the database is empty, return 0 instead of null
        return ResponseEntity.ok(count != null ? count : 0L);
    }
}