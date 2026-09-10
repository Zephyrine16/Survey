package com.example.survey.service;

import com.example.survey.dto.*;
import com.example.survey.repository.AnswerRepository;
import com.example.survey.repository.QuestionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    @Mock
    private AnswerRepository answerRepository;

    @Mock
    private QuestionRepository questionRepository;

    @InjectMocks
    private AnalyticsService analyticsService;

    @Test
    void testBuildMoodAnalytics_ParsesRatingsAndIdentifiesTopMood() {
        Long menuItemId = 101L;
        List<Object[]> answers = new ArrayList<>();
        // Row format: [questionId, userId, response]
        answers.add(new Object[]{1L, "user1", "Comfort (Wants something warm or familiar): 5 (Very Suitable)"});
        answers.add(new Object[]{1L, "user1", "Treat (Wants something enjoyable or indulgent): 4 (Suitable)"});
        answers.add(new Object[]{1L, "user2", "Comfort (Wants something warm or familiar): 4 (Suitable)"});
        answers.add(new Object[]{1L, "user2", "Energy (Wants something energizing): 2 (Slightly Suitable)"});

        when(answerRepository.findAllAnswersForMenuItem(menuItemId)).thenReturn(answers);

        GridQuestionAnalyticsDTO moodAnalytics = analyticsService.buildMoodAnalytics(menuItemId);

        assertNotNull(moodAnalytics);
        assertEquals("Question 1 — Mood Association", moodAnalytics.getTitle());
        assertEquals("Comfort", moodAnalytics.getTopRowLabel());
        assertEquals(4.5, moodAnalytics.getTopRowScore());
        assertEquals(2, moodAnalytics.getTotalEvaluators());
        assertEquals(9, moodAnalytics.getRows().size());

        GridRowStatDTO comfortRow = moodAnalytics.getRows().stream()
                .filter(r -> "comfort".equals(r.getId()))
                .findFirst()
                .orElse(null);
        assertNotNull(comfortRow);
        assertEquals(4.5, comfortRow.getAvgRating());
        assertEquals(2, comfortRow.getTotalVotes());
        assertEquals(100.0, comfortRow.getSuitabilityPct());
    }

    @Test
    void testBuildWeatherAnalytics_ParsesWeatherRatings() {
        Long menuItemId = 101L;
        List<Object[]> answers = new ArrayList<>();
        answers.add(new Object[]{2L, "user1", "Rainy: 5 (Very Suitable)"});
        answers.add(new Object[]{2L, "user2", "Rainy: 4 (Suitable)"});
        answers.add(new Object[]{2L, "user1", "Hot/Sunny: 2 (Slightly Suitable)"});

        when(answerRepository.findAllAnswersForMenuItem(menuItemId)).thenReturn(answers);

        GridQuestionAnalyticsDTO weatherAnalytics = analyticsService.buildWeatherAnalytics(menuItemId);

        assertNotNull(weatherAnalytics);
        assertEquals("Question 2 — Weather Association", weatherAnalytics.getTitle());
        assertEquals("Rainy", weatherAnalytics.getTopRowLabel());
        assertEquals(4.5, weatherAnalytics.getTopRowScore());
        assertEquals(2, weatherAnalytics.getTotalEvaluators());
        assertEquals(4, weatherAnalytics.getRows().size());
    }

    @Test
    void testGetDemographics() {
        List<Object[]> rows = new ArrayList<>();
        // Row format: [questionText, response, count]
        rows.add(new Object[]{"Age Group", "21–23", 12L});
        rows.add(new Object[]{"Age Group", "18–20", 5L});
        rows.add(new Object[]{"How often do you dine at cafés or restaurants?", "Once a week", 10L});
        rows.add(new Object[]{"How often do you dine at cafés or restaurants?", "Several times a week", 8L});

        when(answerRepository.findDemographicResponses()).thenReturn(rows);
        when(answerRepository.countTotalParticipants()).thenReturn(25L);

        DemographicAnalyticsDTO demographics = analyticsService.getDemographics();

        assertNotNull(demographics);
        assertEquals(18L, demographics.getTotalParticipants());
        assertEquals(25L, demographics.getGlobalParticipants());
        assertEquals(12L, demographics.getAgeGroupCounts().get("21–23"));
        assertEquals(10L, demographics.getDiningFrequencyCounts().get("Once a week"));
    }

    @Test
    void testBuildRecentResponses() {
        Long menuItemId = 101L;
        List<Object[]> answers = new ArrayList<>();
        answers.add(new Object[]{1L, "user1", "Comfort (Wants something warm or familiar): 5 (Very Suitable)"});
        answers.add(new Object[]{2L, "user1", "Rainy: 4 (Suitable)"});
        answers.add(new Object[]{3L, "user1", "Delicious and creamy!"});

        when(answerRepository.findAllAnswersForMenuItem(menuItemId)).thenReturn(answers);

        List<SurveyResponseDetailDTO> details = analyticsService.buildRecentResponses(menuItemId);

        assertNotNull(details);
        assertEquals(1, details.size());
        SurveyResponseDetailDTO user1 = details.get(0);
        assertEquals("user1", user1.getUserId());
        assertEquals(5, user1.getMoodRatings().get("Comfort"));
        assertEquals(4, user1.getWeatherRatings().get("Rainy"));
        assertEquals("Delicious and creamy!", user1.getTextFeedback());
    }
}