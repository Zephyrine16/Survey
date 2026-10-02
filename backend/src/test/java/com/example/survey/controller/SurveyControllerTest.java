package com.example.survey.controller;

import com.example.survey.config.SurveyProperties;
import com.example.survey.repository.AnswerRepository;
import com.example.survey.repository.MenuItemRepository;
import com.example.survey.repository.OptionRepository;
import com.example.survey.repository.QuestionRepository;
import com.example.survey.service.AnalyticsService;
import com.example.survey.service.SurveyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SurveyControllerTest {

    private AnswerRepository answerRepository;
    private SurveyController controller;

    @BeforeEach
    void setUp() {
        answerRepository = Mockito.mock(AnswerRepository.class);
        controller = new SurveyController(
                answerRepository,
                Mockito.mock(MenuItemRepository.class),
                Mockito.mock(QuestionRepository.class),
                Mockito.mock(OptionRepository.class),
                Mockito.mock(SurveyService.class),
                Mockito.mock(AnalyticsService.class),
                new SurveyProperties());
    }

    @Test
    void testClearAllDataDeletesAnswersAndReturnsMessage() {
        ResponseEntity<?> response = controller.clearAllData();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Mockito.verify(answerRepository).deleteAllInBatch();
        assertNotNull(response.getBody());
        assertTrue(((Map<?, ?>) response.getBody()).containsKey("message"));
    }

    @Test
    void testClearAllDataReturnsServerErrorWhenDeleteFails() {
        Mockito.doThrow(new RuntimeException("db down")).when(answerRepository).deleteAllInBatch();

        ResponseEntity<?> response = controller.clearAllData();

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(((Map<?, ?>) response.getBody()).containsKey("error"));
    }
}
