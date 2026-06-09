package com.example.survey.controller;

import com.example.survey.model.Question;
import com.example.survey.repository.QuestionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/questions")
@RequiredArgsConstructor
public class QuestionController {

    private final QuestionRepository questionRepository;

    @GetMapping("/all")
    public List<Question> getAllQuestions() {
        return questionRepository.findAllWithOptions();
    }
}
