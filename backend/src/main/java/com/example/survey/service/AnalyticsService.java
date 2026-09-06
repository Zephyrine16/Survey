package com.example.survey.service;

import com.example.survey.dto.*;
import com.example.survey.repository.AnswerRepository;
import com.example.survey.repository.QuestionRepository;
import com.example.survey.repository.QuestionSummaryProjection;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private static final List<String> STOP_WORDS = Arrays.asList(
            "the", "and", "is", "it", "to", "a", "of", "for", "in", "on", "this", "but",
            "very", "so", "with", "i", "was", "not", "have", "that", "like", "just", "my"
    );

    private static final Pattern POSITIVE_PATTERN = Pattern.compile(".*\\b(good|great|love|best|delicious|yummy|perfect|nice|amazing|sweet|comfort|favorite|warm|fresh|hot|filling)\\b.*");
    private static final Pattern NEGATIVE_PATTERN = Pattern.compile(".*\\b(bad|hate|awful|terrible|gross|expensive|worse|bland|nasty|disgusting|dry|salty|cold|hard|stale)\\b.*");
    private static final Pattern RATING_PATTERN = Pattern.compile("^(.*?):\\s*(\\d+)(?:\\s*\\((.*?)\\))?$");

    private static final int MAX_KEYWORDS = 8;

    public record RowDef(String id, String label, String shortLabel) {}

    public static final List<RowDef> MOOD_DEFINITIONS = List.of(
            new RowDef("energy", "Energy (Wants something energizing)", "Energy"),
            new RowDef("comfort", "Comfort (Wants something warm or familiar)", "Comfort"),
            new RowDef("refreshing", "Refreshing (Wants something light or cooling)", "Refreshing"),
            new RowDef("healthy", "Healthy (Wants a healthier choice)", "Healthy"),
            new RowDef("treat", "Treat (Wants something enjoyable or indulgent)", "Treat"),
            new RowDef("focused", "Focused (Wants to concentrate or study)", "Focused"),
            new RowDef("familiar", "Familiar (Wants a safe, familiar choice)", "Familiar"),
            new RowDef("adventurous", "Adventurous (Wants to try something new)", "Adventurous"),
            new RowDef("quick", "Quick (Wants something convenient)", "Quick")
    );

    public static final List<RowDef> WEATHER_DEFINITIONS = List.of(
            new RowDef("hot_sunny", "Hot/Sunny", "Hot/Sunny"),
            new RowDef("hot_humid", "Hot/Humid", "Hot/Humid"),
            new RowDef("rainy", "Rainy", "Rainy"),
            new RowDef("cool_dry", "Cool Dry (Note: Even in tropical climates, \"cool dry\" exists: breezy December–February days, air-conditioned spaces, or cool hill stations/evening breezes.)", "Cool Dry")
    );

    private final AnswerRepository answerRepository;
    private final QuestionRepository questionRepository;

    public Map<Long, Object> getAnalyticsForMenuItem(Long menuItemId) {
        Map<Long, Object> dashboardData = initializeQuestionBuckets();

        // 1. Radio votes from selected options
        List<Object[]> radioRows = answerRepository.countVotesByOptionForMenuItem(menuItemId);
        for (Object[] row : radioRows) {
            Long questionId = asLong(row[0]);
            String optionLabel = row[1] == null ? "" : row[1].toString();
            Long voteCount = asLong(row[2]);
            @SuppressWarnings("unchecked")
            List<OptionCountDTO> optionCounts = (List<OptionCountDTO>) dashboardData.computeIfAbsent(questionId, ignored -> new ArrayList<OptionCountDTO>());
            optionCounts.add(new OptionCountDTO(optionLabel, voteCount));
        }

        // 2. All answers including text & ratings
        List<Object[]> allAnswers = answerRepository.findAllAnswersForMenuItem(menuItemId);
        for (Object[] row : allAnswers) {
            Long questionId = asLong(row[0]);
            String userId = row[1] == null ? null : row[1].toString();
            String response = row[2] == null ? null : row[2].toString();

            if (response != null && !response.isBlank()) {
                Object bucket = dashboardData.get(questionId);
                if (bucket instanceof List<?> list) {
                    @SuppressWarnings("unchecked")
                    List<Object> typedList = (List<Object>) list;
                    typedList.add(new TextFeedbackDTO(userId, response));
                } else {
                    List<TextFeedbackDTO> feedbackList = new ArrayList<>();
                    feedbackList.add(new TextFeedbackDTO(userId, response));
                    dashboardData.put(questionId, feedbackList);
                }
            }
        }

        return dashboardData;
    }

    public DashboardStatsDTO getItemStats(Long menuItemId) {
        DashboardStatsDTO stats = new DashboardStatsDTO();

        stats.globalTotal = answerRepository.countGlobalTotalResponses();
        stats.itemTotal = answerRepository.countTotalResponsesForItem(menuItemId);

        List<Object[]> allAnswers = answerRepository.findAllAnswersForMenuItem(menuItemId);
        List<String> textReviews = new ArrayList<>();
        int ratingSum = 0;
        int ratingCount = 0;

        for (Object[] row : allAnswers) {
            String response = row[2] == null ? null : row[2].toString();
            if (response == null || response.isBlank()) continue;

            Matcher matcher = RATING_PATTERN.matcher(response.trim());
            if (matcher.matches()) {
                int rating = Integer.parseInt(matcher.group(2));
                rating = Math.max(1, Math.min(5, rating));
                ratingSum += rating;
                ratingCount++;
                if (rating >= 4) {
                    stats.positiveCount++;
                } else if (rating == 3) {
                    stats.neutralCount++;
                } else {
                    stats.negativeCount++;
                }
            } else {
                textReviews.add(response);
            }
        }

        applySentimentCounts(stats, textReviews);
        applySentimentPercentages(stats);
        stats.topKeywords = extractTopKeywords(textReviews);
        if (ratingCount > 0) {
            stats.avgSuitabilityScore = Math.round((ratingSum / (double) ratingCount) * 10.0) / 10.0;
        }

        return stats;
    }

    public CombinedAnalyticsDTO getCombinedAnalytics(Long menuItemId) {
        Map<Long, Object> analyticsData = getAnalyticsForMenuItem(menuItemId);
        DashboardStatsDTO stats = getItemStats(menuItemId);
        GridQuestionAnalyticsDTO moodAnalytics = buildMoodAnalytics(menuItemId);
        GridQuestionAnalyticsDTO weatherAnalytics = buildWeatherAnalytics(menuItemId);
        DemographicAnalyticsDTO demographics = getDemographics();
        List<SurveyResponseDetailDTO> recentResponses = buildRecentResponses(menuItemId);

        if (moodAnalytics != null && moodAnalytics.getTopRowLabel() != null) {
            stats.topMood = moodAnalytics.getTopRowLabel();
            stats.topMoodScore = moodAnalytics.getTopRowScore();
            if (stats.topKeywords == null) {
                stats.topKeywords = new ArrayList<>();
            }
            if (!stats.topKeywords.contains(moodAnalytics.getTopRowLabel())) {
                stats.topKeywords.add(0, moodAnalytics.getTopRowLabel());
            }
        }
        if (weatherAnalytics != null && weatherAnalytics.getTopRowLabel() != null) {
            stats.topWeather = weatherAnalytics.getTopRowLabel();
            stats.topWeatherScore = weatherAnalytics.getTopRowScore();
            if (stats.topKeywords != null && !stats.topKeywords.contains(weatherAnalytics.getTopRowLabel())) {
                stats.topKeywords.add(weatherAnalytics.getTopRowLabel());
            }
        }

        return CombinedAnalyticsDTO.builder()
                .analyticsData(analyticsData)
                .stats(stats)
                .moodAnalytics(moodAnalytics)
                .weatherAnalytics(weatherAnalytics)
                .demographics(demographics)
                .recentResponses(recentResponses)
                .build();
    }

    public GridQuestionAnalyticsDTO buildMoodAnalytics(Long menuItemId) {
        List<Object[]> allAnswers = answerRepository.findAllAnswersForMenuItem(menuItemId);

        Map<String, GridRowAccumulator> accumulators = new LinkedHashMap<>();
        for (RowDef def : MOOD_DEFINITIONS) {
            accumulators.put(def.id(), new GridRowAccumulator(def));
        }

        Set<String> uniqueUsers = new HashSet<>();

        for (Object[] row : allAnswers) {
            String userId = row[1] == null ? null : row[1].toString();
            String response = row[2] == null ? null : row[2].toString();
            if (response == null || response.isBlank()) continue;

            Matcher matcher = RATING_PATTERN.matcher(response.trim());
            if (matcher.matches()) {
                String rowLabelPart = matcher.group(1).trim().toLowerCase();
                int rating = Integer.parseInt(matcher.group(2));
                rating = Math.max(1, Math.min(5, rating));

                for (RowDef def : MOOD_DEFINITIONS) {
                    if (matchesRow(rowLabelPart, def)) {
                        accumulators.get(def.id()).addRating(rating);
                        if (userId != null) uniqueUsers.add(userId);
                        break;
                    }
                }
            }
        }

        List<GridRowStatDTO> rows = new ArrayList<>();
        String topLabel = null;
        double topScore = 0.0;

        for (GridRowAccumulator acc : accumulators.values()) {
            GridRowStatDTO stat = acc.toDTO();
            rows.add(stat);
            if (stat.getAvgRating() > topScore) {
                topScore = stat.getAvgRating();
                topLabel = stat.getShortLabel();
            }
        }

        return GridQuestionAnalyticsDTO.builder()
                .title("Question 1 — Mood Association")
                .prompt("How suitable is this item for each of the following moods?")
                .rows(rows)
                .topRowLabel(topLabel)
                .topRowScore(topScore)
                .totalEvaluators(uniqueUsers.size())
                .build();
    }

    public GridQuestionAnalyticsDTO buildWeatherAnalytics(Long menuItemId) {
        List<Object[]> allAnswers = answerRepository.findAllAnswersForMenuItem(menuItemId);

        Map<String, GridRowAccumulator> accumulators = new LinkedHashMap<>();
        for (RowDef def : WEATHER_DEFINITIONS) {
            accumulators.put(def.id(), new GridRowAccumulator(def));
        }

        Set<String> uniqueUsers = new HashSet<>();

        for (Object[] row : allAnswers) {
            String userId = row[1] == null ? null : row[1].toString();
            String response = row[2] == null ? null : row[2].toString();
            if (response == null || response.isBlank()) continue;

            Matcher matcher = RATING_PATTERN.matcher(response.trim());
            if (matcher.matches()) {
                String rowLabelPart = matcher.group(1).trim().toLowerCase();
                int rating = Integer.parseInt(matcher.group(2));
                rating = Math.max(1, Math.min(5, rating));

                for (RowDef def : WEATHER_DEFINITIONS) {
                    if (matchesWeatherRow(rowLabelPart, def)) {
                        accumulators.get(def.id()).addRating(rating);
                        if (userId != null) uniqueUsers.add(userId);
                        break;
                    }
                }
            }
        }

        List<GridRowStatDTO> rows = new ArrayList<>();
        String topLabel = null;
        double topScore = 0.0;

        for (GridRowAccumulator acc : accumulators.values()) {
            GridRowStatDTO stat = acc.toDTO();
            rows.add(stat);
            if (stat.getAvgRating() > topScore) {
                topScore = stat.getAvgRating();
                topLabel = stat.getShortLabel();
            }
        }

        return GridQuestionAnalyticsDTO.builder()
                .title("Question 2 — Weather Association")
                .prompt("How suitable is this item for each of the following weather conditions?")
                .rows(rows)
                .topRowLabel(topLabel)
                .topRowScore(topScore)
                .totalEvaluators(uniqueUsers.size())
                .build();
    }

    public DemographicAnalyticsDTO getDemographics() {
        List<Object[]> rows = answerRepository.findDemographicResponses();
        Map<String, Long> ageGroupCounts = new LinkedHashMap<>();
        Map<String, Long> diningFreqCounts = new LinkedHashMap<>();

        for (Object[] row : rows) {
            String qText = row[0] == null ? "" : row[0].toString().toLowerCase();
            String response = row[1] == null ? "" : row[1].toString();
            Long count = asLong(row[2]);

            if (qText.contains("age")) {
                ageGroupCounts.put(response, count);
            } else if (qText.contains("dine") || qText.contains("frequency") || qText.contains("often")) {
                diningFreqCounts.put(response, count);
            }
        }

        Long totalParticipants = answerRepository.countTotalParticipants();

        return DemographicAnalyticsDTO.builder()
                .totalParticipants(totalParticipants != null ? totalParticipants : 0L)
                .ageGroupCounts(ageGroupCounts)
                .diningFrequencyCounts(diningFreqCounts)
                .build();
    }

    public List<SurveyResponseDetailDTO> buildRecentResponses(Long menuItemId) {
        List<Object[]> allAnswers = answerRepository.findAllAnswersForMenuItem(menuItemId);
        Map<String, SurveyResponseDetailDTO> userMap = new LinkedHashMap<>();

        for (Object[] row : allAnswers) {
            String userId = row[1] == null ? "Anonymous" : row[1].toString();
            String response = row[2] == null ? null : row[2].toString();
            if (response == null || response.isBlank()) continue;

            SurveyResponseDetailDTO detail = userMap.computeIfAbsent(userId, id ->
                    SurveyResponseDetailDTO.builder()
                            .userId(id)
                            .moodRatings(new LinkedHashMap<>())
                            .weatherRatings(new LinkedHashMap<>())
                            .build()
            );

            Matcher matcher = RATING_PATTERN.matcher(response.trim());
            if (matcher.matches()) {
                String rowLabelPart = matcher.group(1).trim().toLowerCase();
                int rating = Integer.parseInt(matcher.group(2));
                boolean matched = false;
                for (RowDef def : MOOD_DEFINITIONS) {
                    if (matchesRow(rowLabelPart, def)) {
                        detail.getMoodRatings().put(def.shortLabel(), rating);
                        matched = true;
                        break;
                    }
                }
                if (!matched) {
                    for (RowDef def : WEATHER_DEFINITIONS) {
                        if (matchesWeatherRow(rowLabelPart, def)) {
                            detail.getWeatherRatings().put(def.shortLabel(), rating);
                            break;
                        }
                    }
                }
            } else {
                detail.setTextFeedback(response);
            }
        }

        List<SurveyResponseDetailDTO> list = new ArrayList<>(userMap.values());
        Collections.reverse(list);
        return list;
    }

    private boolean matchesRow(String text, RowDef def) {
        String lowerShort = def.shortLabel().toLowerCase();
        String lowerId = def.id().toLowerCase();
        return text.startsWith(lowerShort) || text.contains(lowerId) || text.contains(def.label().toLowerCase());
    }

    private boolean matchesWeatherRow(String text, RowDef def) {
        String id = def.id();
        if ("hot_sunny".equals(id)) return text.contains("sunny");
        if ("hot_humid".equals(id)) return text.contains("humid");
        if ("rainy".equals(id)) return text.contains("rain");
        if ("cool_dry".equals(id)) return text.contains("cool") || text.contains("dry");
        return text.contains(def.shortLabel().toLowerCase());
    }

    private static class GridRowAccumulator {
        private final RowDef def;
        private int sum = 0;
        private int total = 0;
        private int count4or5 = 0;
        private final Map<Integer, Integer> distribution = new TreeMap<>();

        public GridRowAccumulator(RowDef def) {
            this.def = def;
            for (int i = 1; i <= 5; i++) {
                distribution.put(i, 0);
            }
        }

        public void addRating(int rating) {
            sum += rating;
            total++;
            if (rating >= 4) count4or5++;
            distribution.put(rating, distribution.getOrDefault(rating, 0) + 1);
        }

        public GridRowStatDTO toDTO() {
            double avg = total > 0 ? Math.round((sum / (double) total) * 10.0) / 10.0 : 0.0;
            double suitPct = total > 0 ? Math.round(((double) count4or5 / total) * 1000.0) / 10.0 : 0.0;
            return GridRowStatDTO.builder()
                    .id(def.id())
                    .label(def.label())
                    .shortLabel(def.shortLabel())
                    .avgRating(avg)
                    .totalVotes(total)
                    .suitabilityPct(suitPct)
                    .distribution(new LinkedHashMap<>(distribution))
                    .build();
        }
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

    private List<String> extractTopKeywords(List<String> reviews) {
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
        for (int i = 0; i < Math.min(MAX_KEYWORDS, sortedWords.size()); i++) {
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