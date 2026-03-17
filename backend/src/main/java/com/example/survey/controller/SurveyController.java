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
}