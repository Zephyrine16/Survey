package com.example.survey.service;

import com.example.survey.dto.*;
import com.example.survey.model.MenuItem;
import com.example.survey.model.Option;
import com.example.survey.model.Question;
import com.example.survey.repository.AnswerRepository;
import com.example.survey.repository.MenuItemRepository;
import com.example.survey.repository.QuestionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StatisticalAnalysisServiceTest {

    @Mock
    private AnswerRepository answerRepository;

    @Mock
    private MenuItemRepository menuItemRepository;

    @Mock
    private QuestionRepository questionRepository;

    @InjectMocks
    private StatisticalAnalysisService statisticalAnalysisService;

    @BeforeEach
    void setUp() {
        // Mock two menu items
        MenuItem item1 = new MenuItem();
        item1.setId(1L);
        item1.setName("Truffle Pasta");
        item1.setCategory("Pasta");

        MenuItem item2 = new MenuItem();
        item2.setId(2L);
        item2.setName("Iced Americano");
        item2.setCategory("Coffee");

        lenient().when(menuItemRepository.findById(1L)).thenReturn(Optional.of(item1));
        lenient().when(menuItemRepository.findById(2L)).thenReturn(Optional.of(item2));
        lenient().when(menuItemRepository.findAll()).thenReturn(List.of(item1, item2));

        // Mock evaluation questions from database
        Question q1 = new Question();
        q1.setId(5L);
        q1.setText("Question 1 — Mood Association: How suitable is this item for each mood?");
        q1.setQuestionType("TEXT");
        Option optRelax = new Option();
        optRelax.setId(47L);
        optRelax.setLabel("Relaxation");
        optRelax.setSubDescription("(Wants to unwind)");
        optRelax.setQuestion(q1);
        q1.setOptions(List.of(optRelax));

        Question q2 = new Question();
        q2.setId(6L);
        q2.setText("Question 2 — Weather Association: How suitable is this item in this weather?");
        q2.setQuestionType("TEXT");
        Option optRain = new Option();
        optRain.setId(54L);
        optRain.setLabel("Rainy");
        optRain.setSubDescription("(Wet, gloomy)");
        optRain.setQuestion(q2);
        q2.setOptions(List.of(optRain));

        lenient().when(questionRepository.findAllWithOptions()).thenReturn(List.of(q1, q2));

        // Sample answers: userId, menuItemId, menuItemName, category, response
        List<Object[]> sampleAnswers = List.of(
                new Object[]{"u1", 1L, "Truffle Pasta", "Pasta", "Relaxation (Unwind): 5 (Very Suitable)"},
                new Object[]{"u2", 1L, "Truffle Pasta", "Pasta", "Relaxation (Unwind): 4 (Suitable)"},
                new Object[]{"u3", 1L, "Truffle Pasta", "Pasta", "Relaxation (Unwind): 5 (Very Suitable)"},
                new Object[]{"u1", 2L, "Iced Americano", "Coffee", "Relaxation (Unwind): 2 (Low)"},
                new Object[]{"u2", 2L, "Iced Americano", "Coffee", "Relaxation (Unwind): 1 (Low)"},
                new Object[]{"u3", 2L, "Iced Americano", "Coffee", "Relaxation (Unwind): 2 (Low)"}
        );
        lenient().when(answerRepository.findAllItemRatingResponses()).thenReturn(sampleAnswers);

        // Demographic answers: userId, questionText, response
        List<Object[]> demoAnswers = List.of(
                new Object[]{"u1", "Age Group", "18–20"},
                new Object[]{"u2", "Age Group", "18–20"},
                new Object[]{"u3", "Age Group", "21–23"},
                new Object[]{"u1", "How often do you dine at cafés or restaurants?", "Daily"},
                new Object[]{"u2", "How often do you dine at cafés or restaurants?", "Several times a week"}
        );
        lenient().when(answerRepository.findAllUserDemographics()).thenReturn(demoAnswers);
    }

    @Test
    void testTTest_compareItems() {
        TTestRequestDTO request = TTestRequestDTO.builder()
                .mode("ITEMS")
                .group1("1")
                .group2("2")
                .dimension("relaxation")
                .alpha(0.05)
                .build();

        TTestResultDTO result = statisticalAnalysisService.runTTest(request);

        assertNotNull(result);
        assertEquals("Truffle Pasta", result.getGroup1Label());
        assertEquals("Iced Americano", result.getGroup2Label());
        assertEquals(3, result.getN1());
        assertEquals(3, result.getN2());
        assertTrue(result.getMean1() > result.getMean2());
        assertTrue(result.getTStatistic() > 0);
        assertTrue(result.getPValue() < 0.05);
        assertTrue(result.isSignificant());
        assertNotNull(result.getConclusion());
    }

    @Test
    void testTTest_compareSuperCategories() {
        TTestRequestDTO request = TTestRequestDTO.builder()
                .mode("SUPER_CATEGORIES")
                .dimension("all")
                .alpha(0.05)
                .build();

        TTestResultDTO result = statisticalAnalysisService.runTTest(request);

        assertNotNull(result);
        assertEquals("Meals", result.getGroup1Label());
        assertEquals("Beverages", result.getGroup2Label());
        assertEquals(3, result.getN1());
        assertEquals(3, result.getN2());
        assertTrue(result.isSignificant());
    }

    @Test
    void testAnova_subcategories() {
        AnovaRequestDTO request = AnovaRequestDTO.builder()
                .factor("SUBCATEGORIES")
                .dimension("all")
                .alpha(0.05)
                .build();

        AnovaResultDTO result = statisticalAnalysisService.runAnova(request);

        assertNotNull(result);
        assertEquals(2, result.getGroups().size());
        assertEquals(6, result.getTotalN());
        assertTrue(result.getFStatistic() > 0);
        assertTrue(result.getPValue() <= 0.05);
        assertTrue(result.isSignificant());
        assertNotNull(result.getEtaSquared());
    }

    @Test
    void testStatisticalOverview() {
        StatisticalOverviewDTO overview = statisticalAnalysisService.getStatisticalOverview();

        assertNotNull(overview);
        assertFalse(overview.getItems().isEmpty());
        assertTrue(overview.getTotalRatingsCount() > 0);
        assertNotNull(overview.getSampleTTest());
        assertNotNull(overview.getSampleAnova());
    }
}
