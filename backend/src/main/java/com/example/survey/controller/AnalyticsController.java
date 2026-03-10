package com.example.survey.controller;

import com.example.survey.dto.TextFeedbackDTO;
import com.example.survey.dto.OptionCountDTO;
import com.example.survey.model.Question;
import com.example.survey.repository.AnswerRepository;
import com.example.survey.repository.QuestionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/analytics")
@CrossOrigin(origins = "http:localhost:5173")
public class AnalyticsController {

    @Autowired
    private AnswerRepository answerRepository;

    @Autowired
    private QuestionRepository questionRepository;

    @GetMapping("/{menuItemId}")
    public Map<String, Object> getAnalyticsForMenuItem(@PathVariable Long menuItemId) {
        Map<String, Object> dashboardData = new HashMap<>();

        List<Question> questions = questionRepository.findAll();
        for(Question question : questions) {
            if("RADIO".equals(question.getQuestionType())) {
                List<OptionCountDTO> stats = answerRepository.countVotesByOption(menuItemId, question.getId());
                dashboardData.put(question.getText(), stats);
            }
            else if("TEXT".equals(question.getQuestionType())) {
                List<TextFeedbackDTO> feedback = answerRepository.findTextResponse(menuItemId, question.getId());
                dashboardData.put(question.getText(), feedback);
            }
        }
        return dashboardData;
    }
}
