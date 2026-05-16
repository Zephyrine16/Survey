package com.example.survey.service;

import com.example.survey.dto.CombinedAnalyticsDTO;
import com.example.survey.dto.DashboardStatsDTO;
import com.example.survey.dto.OptionCountDTO;
import com.example.survey.dto.TextFeedbackDTO;
import com.example.survey.repository.AnswerRepository;
import com.example.survey.repository.QuestionRepository;
import com.example.survey.repository.QuestionSummaryProjection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Pattern;

@Service
public class AnalyticsService {

    private static final List<String> STOP_WORDS = Arrays.asList(
            "the", "and", "is", "it", "to", "a", "of", "for", "in", "on", "this", "but",
            "very", "so", "with", "i", "was", "not", "have", "that", "like", "just", "my"
    );

    private static final Pattern POSITIVE_PATTERN = Pattern.compile(".*\\b(good|great|love|best|delicious|yummy|perfect|nice|amazing|sweet|comfort|favorite|warm|fresh|hot|filling)\\b.*");
    private static final Pattern NEGATIVE_PATTERN = Pattern.compile(".*\\b(bad|hate|awful|terrible|gross|expensive|worse|bland|nasty|disgusting|dry|salty|cold|hard|stale)\\b.*");

    @Autowired
    private AnswerRepository answerRepository;

    @Autowired
    private QuestionRepository questionRepository;

    public Map<Long, Object> getAnalyticsForMenuItem(Long menuItemId) {
        Map<Long, Object> dashboardData = initializeQuestionBuckets();

        List<Object[]> radioRows = answerRepository.countVotesByOptionForMenuItem(menuItemId);
        for (Object[] row : radioRows) {
            Long questionId = asLong(row[0]);
            String optionLabel = row[1] == null ? "" : row[1].toString();
            Long voteCount = asLong(row[2]);
            @SuppressWarnings("unchecked")
            List<OptionCountDTO> optionCounts = (List<OptionCountDTO>) dashboardData.computeIfAbsent(questionId, ignored -> new ArrayList<OptionCountDTO>());
            optionCounts.add(new OptionCountDTO(optionLabel, voteCount));
        }

        List<Object[]> textRows = answerRepository.findTextResponsesForMenuItem(menuItemId);
        for (Object[] row : textRows) {
            Long questionId = asLong(row[0]);
            String userId = row[1] == null ? null : row[1].toString();
            String response = row[2] == null ? null : row[2].toString();
            @SuppressWarnings("unchecked")
            List<TextFeedbackDTO> feedback = (List<TextFeedbackDTO>) dashboardData.computeIfAbsent(questionId, ignored -> new ArrayList<TextFeedbackDTO>());
            feedback.add(new TextFeedbackDTO(userId, response));
        }

        return dashboardData;
    }

    public DashboardStatsDTO getItemStats(Long menuItemId) {
        DashboardStatsDTO stats = new DashboardStatsDTO();

        stats.globalTotal = answerRepository.countGlobalTotalResponses();
        stats.itemTotal = answerRepository.countTotalResponsesForItem(menuItemId);

        List<String> reviews = answerRepository.getTextReviewsForItem(menuItemId);
        applySentimentCounts(stats, reviews);
        applySentimentPercentages(stats);
        stats.topKeywords = extractTopKeywords(reviews, 8);

        return stats;
    }

    public CombinedAnalyticsDTO getCombinedAnalytics(Long menuItemId) {
        return new CombinedAnalyticsDTO(getAnalyticsForMenuItem(menuItemId), getItemStats(menuItemId));
    }

    private Map<Long, Object> initializeQuestionBuckets() {
        Map<Long, Object> dashboardData = new LinkedHashMap<>();
        List<QuestionSummaryProjection> questions = questionRepository.findAllQuestionSummaries();
        for (QuestionSummaryProjection question : questions) {
            if ("TEXT".equals(question.getQuestionType())) {
                dashboardData.put(question.getId(), new ArrayList<TextFeedbackDTO>());
            } else {
                dashboardData.put(question.getId(), new ArrayList<OptionCountDTO>());
            }
        }
        return dashboardData;
    }

    private void applySentimentCounts(DashboardStatsDTO stats, List<String> reviews) {
        for (String review : reviews) {
            if (review == null) {
                continue;
            }
            String lower = review.toLowerCase();
            if (POSITIVE_PATTERN.matcher(lower).matches()) {
                stats.positiveCount++;
            } else if (NEGATIVE_PATTERN.matcher(lower).matches()) {
                stats.negativeCount++;
            } else {
                stats.neutralCount++;
            }
        }
    }

    private void applySentimentPercentages(DashboardStatsDTO stats) {
        int totalAnalyzed = stats.positiveCount + stats.neutralCount + stats.negativeCount;
        if (totalAnalyzed <= 0) {
            return;
        }
        stats.positivePct = Math.round(((double) stats.positiveCount / totalAnalyzed) * 1000.0) / 10.0;
        stats.neutralPct = Math.round(((double) stats.neutralCount / totalAnalyzed) * 1000.0) / 10.0;
        stats.negativePct = Math.round(((double) stats.negativeCount / totalAnalyzed) * 1000.0) / 10.0;
    }

    private List<String> extractTopKeywords(List<String> reviews, int limit) {
        Map<String, Integer> wordCounts = new HashMap<>();
        for (String review : reviews) {
            if (review == null) {
                continue;
            }
            String[] words = review.toLowerCase().replaceAll("[^a-z\\s]", "").split("\\s+");
            for (String word : words) {
                if (word.length() > 2 && !STOP_WORDS.contains(word)) {
                    wordCounts.put(word, wordCounts.getOrDefault(word, 0) + 1);
                }
            }
        }

        List<Map.Entry<String, Integer>> sortedWords = new ArrayList<>(wordCounts.entrySet());
        sortedWords.sort((a, b) -> b.getValue().compareTo(a.getValue()));

        List<String> topKeywords = new ArrayList<>();
        for (int i = 0; i < Math.min(limit, sortedWords.size()); i++) {
            topKeywords.add(sortedWords.get(i).getKey());
        }
        return topKeywords;
    }

    private Long asLong(Object value) {
        if (value == null) {
            return 0L;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(value.toString());
    }
}
