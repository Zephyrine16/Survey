package com.example.survey.controller;

import com.example.survey.model.UserAnswer;
import com.example.survey.repository.UserAnswerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/answers")
public class UserAnswerController {

    @Autowired
    private UserAnswerRepository userAnswerRepository;

    @PostMapping("/save")
    public UserAnswer saveAnswer(@RequestBody UserAnswer userAnswer) {
        return userAnswerRepository.save(userAnswer);
    }

    @PostMapping("/saveAll")
    public List<UserAnswer> saveAllAnswer(@RequestBody List<UserAnswer> answers) {
        return userAnswerRepository.saveAll(answers);
    }

    @GetMapping("/all")
    public List<UserAnswer> getAllAnswers() {
        return userAnswerRepository.findAll();
    }
}
