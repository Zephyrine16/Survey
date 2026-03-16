package com.example.survey.controller;

import com.example.survey.dto.CategorySubmissionDTO;
import com.example.survey.model.User;
import com.example.survey.model.UserAnswer;
import com.example.survey.repository.UserAnswerRepository;
import com.example.survey.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin(origins = {"http://localhost:5173", "http://127.0.0.1:5173"})
public class SurveyController {

    @Autowired
    private UserAnswerRepository userAnswerRepository;

    @Autowired
    private UserRepository userRepository;

    @PostMapping("/submit-category")
    public ResponseEntity<?> submitCategoryAnswers(@RequestBody List<CategorySubmissionDTO> payload) {
        try {
            for (CategorySubmissionDTO dto : payload) {
                UserAnswer answer = new UserAnswer();

                User user = userRepository.findById(dto.getUserId())
                        .orElseThrow(() -> new RuntimeException("User not found with ID: " + dto.getUserId()));

                answer.setUser(user);

                answer.setMenuItemId(dto.getMenuItemId());
                answer.setQuestionId(dto.getQuestionId());
                answer.setSelectedOptionId(dto.getSelectedOptionId());
                answer.setTextResponse(dto.getTextResponse());

                userAnswerRepository.save(answer);
            }

            return ResponseEntity.ok().body("{\"message\": \"Category saved successfully!\"}");

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("{\"error\": \"Failed to save data.\"}");
        }
    }
}