package com.example.survey.controller;

import com.example.survey.model.Answer;
import com.example.survey.repository.AnswerRepository;
import com.example.survey.repository.MenuItemRepository;
import com.example.survey.repository.OptionRepository;
import com.example.survey.repository.QuestionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/answers")
@CrossOrigin(origins = {"http://localhost:5173", "http://127.0.0.1:5173"})
public class AnswerController {

    @Autowired
    private AnswerRepository answerRepository;

    @Autowired
    private MenuItemRepository menuItemRepository;

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private OptionRepository optionRepository;

    public static class AnswerPayload {
        public Long menuItemId;
        public Long questionId;
        public Long selectedOptionId;
        public String response;
        public String userEmail;
    }

    @PostMapping("/saveAll")
    public List<Answer> saveAllAnswers(@RequestBody List<AnswerPayload> payloads) {
        List<Answer> answersToSave = new ArrayList<>();

        for(AnswerPayload payload : payloads) {
            Answer answer = new Answer();
            answer.setUserEmail(payload.userEmail);
            answer.setResponse(payload.response);

            menuItemRepository.findById(payload.menuItemId).ifPresent(answer::setMenuItem);
            questionRepository.findById(payload.questionId).ifPresent(answer::setQuestion);

            if(payload.selectedOptionId != null) {
                optionRepository.findById(payload.selectedOptionId).ifPresent(answer::setSelectedOption);
            }
            answersToSave.add(answer);
        }

        return answerRepository.saveAll(answersToSave);
    }
}
