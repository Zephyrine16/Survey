package com.example.survey.service;

import com.example.survey.config.SurveyProperties;
import com.example.survey.dto.CategorySubmissionDTO;
import com.example.survey.model.Question;
import com.example.survey.repository.AnswerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
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

    @BeforeEach
    void defaults() {
        lenient().when(surveyProperties.getItemsPerParticipant()).thenReturn(10);
    }

    @Mock
    private AnswerRepository answerRepository;

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private SurveyProperties surveyProperties;

    @Mock
    private com.example.survey.repository.QuestionRepository questionRepository;

    @Mock
    private com.example.survey.repository.MenuItemRepository menuItemRepository;

    @Mock
    private com.example.survey.repository.OptionRepository optionRepository;

    @InjectMocks
    private SurveyService surveyService;

    @Test
    void resolveParticipantLimit_derivedFromItemCountWhenNoExplicitLimit() {
        when(surveyProperties.getParticipantLimit()).thenReturn(0L);
        when(surveyProperties.getItemRespondentLimit()).thenReturn(35L);
        when(surveyProperties.getItemsPerParticipant()).thenReturn(10);
        when(menuItemRepository.count()).thenReturn(73L);

        // ceil(73 * 35 / 10) = 256
        assertEquals(256L, surveyService.resolveParticipantLimit());
    }

    @Test
    void resolveParticipantLimit_explicitLimitWins() {
        when(surveyProperties.getParticipantLimit()).thenReturn(40L);

        assertEquals(40L, surveyService.resolveParticipantLimit());
    }

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
        when(menuItemRepository.existsById(1L)).thenReturn(true);
        var question = new Question();
        question.setId(1L);
        var option = new com.example.survey.model.Option();
        option.setQuestion(question);
        when(optionRepository.findById(2L)).thenReturn(java.util.Optional.of(option));
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

    @Test
    void saveCompleteSurveyBatchesRatingsAndDemographicsTogether() {
        when(menuItemRepository.existsById(101L)).thenReturn(true);
        when(questionRepository.existsById(1L)).thenReturn(true);
        Question age = new Question();
        age.setId(7L);
        age.setText("Age Group");
        Question frequency = new Question();
        frequency.setId(8L);
        frequency.setText("How often do you dine at cafés or restaurants?");
        when(questionRepository.findAll()).thenReturn(List.of(age, frequency));

        CategorySubmissionDTO rating = new CategorySubmissionDTO();
        rating.setUserId("session-1");
        rating.setMenuItemId(101L);
        rating.setQuestionId(1L);
        rating.setTextResponse("Happy: 4");

        assertTrue(surveyService.saveCompleteSurveyIfUnderLimit(
                List.of(rating), "session-1", true, "18–20", "Once a week"));

        ArgumentCaptor<BatchPreparedStatementSetter> rows =
                ArgumentCaptor.forClass(BatchPreparedStatementSetter.class);
        verify(jdbcTemplate).batchUpdate(anyString(), rows.capture());
        assertEquals(3, rows.getValue().getBatchSize());
        verify(jdbcTemplate, never()).update(anyString(), any(), any(), any());
    }

    @Test
    void saveCompleteSurveyDoesNotInsertRatingsIfDemographicsCannotBePrepared() {
        when(menuItemRepository.existsById(101L)).thenReturn(true);
        when(questionRepository.existsById(1L)).thenReturn(true);
        when(questionRepository.findAll()).thenThrow(new IllegalStateException("Question lookup failed"));
        CategorySubmissionDTO rating = new CategorySubmissionDTO();
        rating.setUserId("session-1");
        rating.setMenuItemId(101L);
        rating.setQuestionId(1L);
        rating.setTextResponse("Happy: 4");

        assertThrows(IllegalStateException.class, () -> surveyService.saveCompleteSurveyIfUnderLimit(
                List.of(rating), "session-1", true, "18–20", null));
        verify(jdbcTemplate, never()).batchUpdate(anyString(), any(BatchPreparedStatementSetter.class));
    }

    @Test
    void retryForAnAlreadySavedSessionDoesNotInsertAnswersAgain() {
        when(answerRepository.existsByUserIdAndMenuItemIsNotNull("session-1")).thenReturn(true);

        assertTrue(surveyService.saveCompleteSurveyIfUnderLimit(
                List.of(), "session-1", true, "18–20", "Once a week"));

        verify(answerRepository, never()).countTotalParticipants();
        verify(jdbcTemplate, never()).batchUpdate(anyString(), any(BatchPreparedStatementSetter.class));
    }

    @Test
    void rejectsUnknownQuestionInsteadOfCreatingOrReplacingIt() {
        var answer = new CategorySubmissionDTO();
        answer.setMenuItemId(101L);
        answer.setQuestionId(999L);
        when(menuItemRepository.existsById(101L)).thenReturn(true);
        assertThrows(IllegalArgumentException.class, () -> surveyService.saveSurveyIfUnderLimit(List.of(answer)));
        verify(questionRepository, never()).save(any());
        verifyNoInteractions(jdbcTemplate);
    }

    @Test
    void rejectsOptionFromADifferentQuestion() {
        var answer = new CategorySubmissionDTO();
        answer.setMenuItemId(101L);
        answer.setQuestionId(1L);
        answer.setSelectedOptionId(2L);
        when(menuItemRepository.existsById(101L)).thenReturn(true);
        when(questionRepository.existsById(1L)).thenReturn(true);
        var otherQuestion = new Question();
        otherQuestion.setId(3L);
        var option = new com.example.survey.model.Option();
        option.setQuestion(otherQuestion);
        when(optionRepository.findById(2L)).thenReturn(java.util.Optional.of(option));
        assertThrows(IllegalArgumentException.class, () -> surveyService.saveSurveyIfUnderLimit(List.of(answer)));
        verifyNoInteractions(jdbcTemplate);
    }

    @Test
    void rejectsUnknownMenuItemBeforeWriting() {
        var answer = new CategorySubmissionDTO();
        answer.setMenuItemId(999L);
        answer.setQuestionId(1L);
        assertThrows(IllegalArgumentException.class, () -> surveyService.saveSurveyIfUnderLimit(List.of(answer)));
        verifyNoInteractions(jdbcTemplate);
    }

    @Test
    void rejectsDuplicateDimensionAnswersBeforeInserting() {
        var answer = new CategorySubmissionDTO();
        answer.setMenuItemId(101L);
        answer.setQuestionId(1L);
        answer.setTextResponse("Happy: 4");
        when(menuItemRepository.existsById(101L)).thenReturn(true);
        when(questionRepository.existsById(1L)).thenReturn(true);
        assertThrows(IllegalArgumentException.class, () -> surveyService.saveSurveyIfUnderLimit(List.of(answer, answer)));
        verify(jdbcTemplate, never()).batchUpdate(anyString(), any(BatchPreparedStatementSetter.class));
    }

    @Test
    void rejectsAnItemThatIsAlreadyAtItsRespondentLimit() {
        var answer = new CategorySubmissionDTO();
        answer.setMenuItemId(101L);
        answer.setQuestionId(1L);
        answer.setTextResponse("Happy: 4");
        when(menuItemRepository.existsById(101L)).thenReturn(true);
        when(questionRepository.existsById(1L)).thenReturn(true);
        when(surveyProperties.getItemRespondentLimit()).thenReturn(35L);
        when(answerRepository.countTotalResponsesForItem(101L)).thenReturn(35L);
        assertFalse(surveyService.saveCompleteSurveyIfUnderLimit(List.of(answer), "session-1", true, null, null));
        verify(jdbcTemplate, never()).batchUpdate(anyString(), any(BatchPreparedStatementSetter.class));
    }

    @Test
    void acceptsDifferentDimensionsOfTheSameGridQuestion() {
        when(menuItemRepository.existsById(101L)).thenReturn(true);
        when(questionRepository.existsById(1L)).thenReturn(true);
        var question = new Question();
        question.setId(1L);
        var option = new com.example.survey.model.Option();
        option.setQuestion(question);
        when(optionRepository.findById(anyLong())).thenReturn(java.util.Optional.of(option));
        var happy = new CategorySubmissionDTO();
        happy.setMenuItemId(101L);
        happy.setQuestionId(1L);
        happy.setSelectedOptionId(1L);
        var comfort = new CategorySubmissionDTO();
        comfort.setMenuItemId(101L);
        comfort.setQuestionId(1L);
        comfort.setSelectedOptionId(2L);
        assertTrue(surveyService.saveCompleteSurveyIfUnderLimit(List.of(happy, comfort), "session-1", true, null, null));
    }
}

