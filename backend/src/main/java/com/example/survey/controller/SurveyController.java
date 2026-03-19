package com.example.survey.controller;

import com.example.survey.dto.CategorySubmissionDTO;
import com.example.survey.repository.AnswerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportReport() {
        // 1. Fetch the readable data from the database
        List<Object[]> data = answerRepository.getExportData();

        // 2. Start building the CSV text, starting with the Header row
        StringBuilder csv = new StringBuilder("User Email,Menu Item,Question,Selected Option,Text Response\n");

        // 3. Loop through the data and format it
        for (Object[] row : data) {
            String email = row[0] != null ? row[0].toString() : "Anonymous";
            // We replace commas with spaces so they don't break our CSV format!
            String item = row[1] != null ? row[1].toString().replace(",", "") : "";
            String question = row[2] != null ? row[2].toString().replace(",", "") : "";
            String option = row[3] != null ? row[3].toString().replace(",", "") : "";
            String textResponse = row[4] != null ? row[4].toString().replace("\n", " ").replace(",", "") : "";

            // Add the row to our CSV
            csv.append(email).append(",")
                    .append(item).append(",")
                    .append(question).append(",")
                    .append(option).append(",")
                    .append(textResponse).append("\n");
        }

        // 4. Convert the text to a file format (bytes)
        byte[] csvBytes = csv.toString().getBytes();

        // 5. Send it back to Vue with instructions to download it as a file
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=CafeRater_Analytics.csv")
                .header("Content-Type", "text/csv")
                .body(csvBytes);
    }
}