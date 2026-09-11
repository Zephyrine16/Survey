package com.example.survey.service;

import com.example.survey.config.SurveyProperties;
import com.example.survey.dto.CategorySubmissionDTO;
import com.example.survey.repository.AnswerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SurveyServiceTest {

    @Mock
    private AnswerRepository answerRepository;

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private SurveyProperties surveyProperties;

    @Mock
    private com.example.survey.repository.QuestionRepository questionRepository;

    @InjectMocks
    private SurveyService surveyService;

    @Test
    void saveSurveyIfUnderLimit_ParticipantLimitReached() {
        when(answerRepository.countTotalParticipants()).thenReturn(100L);
        when(surveyProperties.getParticipantLimit()).thenReturn(100L);

        List<CategorySubmissionDTO> payload = List.of();
        boolean result = surveyService.saveSurveyIfUnderLimit(payload);

        assertFalse(result);
        verify(jdbcTemplate, never()).batchUpdate(anyString(), any(BatchPreparedStatementSetter.class));
    }

    @Test
    void saveSurveyIfUnderLimit_Success() {
        when(answerRepository.countTotalParticipants()).thenReturn(50L);
        when(surveyProperties.getParticipantLimit()).thenReturn(100L);
        when(surveyProperties.getTextResponseMaxLength()).thenReturn(255);
        when(questionRepository.existsById(1L)).thenReturn(true);

        CategorySubmissionDTO dto = new CategorySubmissionDTO();
        dto.setUserId("user1");
        dto.setMenuItemId(1L);
        dto.setQuestionId(1L);
        dto.setSelectedOptionId(2L);
        dto.setTextResponse("Test response");
        
        List<CategorySubmissionDTO> payload = List.of(dto);
        
        boolean result = surveyService.saveSurveyIfUnderLimit(payload);

        assertTrue(result);
        verify(jdbcTemplate).batchUpdate(eq("INSERT INTO answers (user_id, menu_item_id, question_id, option_id, response) VALUES (?, ?, ?, ?, ?)"), any(BatchPreparedStatementSetter.class));
    }

    @Test
    void testSaveDemographicAnswer_AgeGroupEncoding() {
        com.example.survey.model.Question q = new com.example.survey.model.Question();
        q.setId(10L);
        q.setText("Age Group");
        when(questionRepository.findAll()).thenReturn(List.of(q));
        when(surveyProperties.getTextResponseMaxLength()).thenReturn(255);

        surveyService.saveDemographicAnswer("user1", "Age Group", "18–20");

        org.mockito.ArgumentCaptor<String> captor = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).update(anyString(), eq("user1"), eq(10L), captor.capture());
        assertEquals("18–20", captor.getValue());
    }
}

