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
}