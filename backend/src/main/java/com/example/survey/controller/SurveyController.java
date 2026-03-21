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

    // 1. We swap out the repository so it connects to the Analytics table!
    @Autowired
    private AnswerRepository answerRepository;

    @PostMapping("/submit-category")
    public ResponseEntity<?> submitCategoryAnswers(@RequestBody List<CategorySubmissionDTO> payload) {
        try {
            for (CategorySubmissionDTO dto : payload) {

                // 2. We use our new Native Query to force the data directly into the 'answers' table
                answerRepository.saveRawAnswer(
                        "student@example.com",     // Dummy email since Dashboard reads it
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
    // 2. THIS EXPORTS THE CSV
    // ==========================================
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportReport() {
        List<Object[]> data = answerRepository.getExportData();

        // FIX #1: Add \uFEFF at the very beginning so Excel reads special characters (like ₱) correctly!
        StringBuilder csv = new StringBuilder("\uFEFFUser Email,Menu Item,Question,Selected Option,Text Response\n");

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

        // FIX #2: Explicitly tell Java to format the bytes as UTF-8
        byte[] csvBytes = csv.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);

        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=CafeRater_Analytics.csv")
                .header("Content-Type", "text/csv; charset=UTF-8")
                .body(csvBytes);
    }

    @GetMapping("/ai/training-data")
    public ResponseEntity<Map<String, List<String>>> getAITrainingData() {
        List<Object[]> rawData = answerRepository.getAITrainingData();
        Map<String, List<String>> aiData = new java.util.HashMap<>();

        for(Object[] row : rawData) {
            String foodName = row[0].toString();
            String description = row[1].toString();

            aiData.putIfAbsent(foodName, new java.util.ArrayList<>());
            aiData.get(foodName).add(description);
        }

        return ResponseEntity.ok(aiData);
    }

    // ==========================================
    // 4. FETCH REAL DASHBOARD STATISTICS
    // ==========================================
    @GetMapping("/analytics/stats/{menuItemId}")
    public ResponseEntity<com.example.survey.dto.DashboardStatsDTO> getItemStats(@PathVariable Long menuItemId) {
        com.example.survey.dto.DashboardStatsDTO stats = new com.example.survey.dto.DashboardStatsDTO();

        // Fetch base counts
        stats.globalTotal = answerRepository.countGlobalTotalResponses();
        stats.itemTotal = answerRepository.countTotalResponsesForItem(menuItemId);

        // Fetch text reviews for sentiment analysis
        List<String> reviews = answerRepository.getTextReviewsForItem(menuItemId);

        // Simple Sentiment Analyzer Logic
        for (String review : reviews) {
            String lower = review.toLowerCase();
            if (lower.matches(".*\\b(good|great|love|best|delicious|yummy|perfect|nice|amazing|sweet|comfort|favorite)\\b.*")) {
                stats.positiveCount++;
            } else if (lower.matches(".*\\b(bad|hate|awful|terrible|gross|expensive|worse|bland|nasty|disgusting|dry)\\b.*")) {
                stats.negativeCount++;
            } else {
                stats.neutralCount++;
            }
        }

        // Calculate Percentages (Avoid dividing by zero!)
        int totalAnalyzed = stats.positiveCount + stats.neutralCount + stats.negativeCount;
        if (totalAnalyzed > 0) {
            stats.positivePct = Math.round(((double) stats.positiveCount / totalAnalyzed) * 1000.0) / 10.0;
            stats.neutralPct = Math.round(((double) stats.neutralCount / totalAnalyzed) * 1000.0) / 10.0;
            stats.negativePct = Math.round(((double) stats.negativeCount / totalAnalyzed) * 1000.0) / 10.0;
        }

        java.util.List<String> stopWords = java.util.Arrays.asList(
                "the", "and", "is", "it", "to", "a", "of", "for", "in", "on", "this", "but",
                "very", "so", "with", "i", "was", "not", "have", "that", "like", "just", "my"
        );

        java.util.Map<String, Integer> wordCounts = new java.util.HashMap<>();
        for(String review : reviews) {
            String[] words = review.toLowerCase().replaceAll("[^a-z\\s]", "").split("\\s");
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
}