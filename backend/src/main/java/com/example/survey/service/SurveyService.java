package com.example.survey.service;

import com.example.survey.config.SurveyProperties;
import com.example.survey.dto.CategorySubmissionDTO;
import com.example.survey.model.Question;
import com.example.survey.repository.AnswerRepository;
import com.example.survey.repository.MenuItemRepository;
import com.example.survey.repository.QuestionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.HtmlUtils;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
@NullMarked
public class SurveyService {

    private final AnswerRepository answerRepository;
    private final QuestionRepository questionRepository;
    private final com.example.survey.repository.OptionRepository optionRepository;
    private final MenuItemRepository menuItemRepository;
    private final JdbcTemplate jdbcTemplate;
    private final SurveyProperties surveyProperties;

    private static final String INSERT_ANSWER_SQL = "INSERT INTO answers (user_id, menu_item_id, question_id, option_id, response) VALUES (?, ?, ?, ?, ?)";
    private static final List<String> RATING_SCALE_LABELS = List.of(
            "Not Suitable", "Slightly Suitable", "Moderately Suitable", "Suitable", "Very Suitable");
    private static final Set<String> AGE_GROUPS = Set.of("18–20", "21–23", "24–26", "27–30", "31 and above");
    private static final Set<String> DINING_FREQUENCIES = Set.of(
            "Several times a week", "Once a week", "Several times a month", "Once a month", "Less than once a month");

    @Transactional
    public boolean saveSurveyIfUnderLimit(List<CategorySubmissionDTO> payload) {
        List<AnswerInsertRow> rows = mapAndSanitizeRows(payload);
        lockSubmissions();
        if (isParticipantLimitReached()) {
            return false;
        }

        if (!areItemsUnderLimit(payload)) return false;
        batchInsertAnswers(rows);

        return true;
    }

    @Transactional
    public boolean saveCompleteSurveyIfUnderLimit(
            List<CategorySubmissionDTO> payload,
            String participantId,
            boolean retryableSession,
            @Nullable String ageGroup,
            @Nullable String diningFrequency
    ) {
        lockSubmissions();
        // A client may retry after the response is lost even though the first
        // request committed. Treat that session as already submitted.
        if (retryableSession && answerRepository.existsByUserIdAndMenuItemIsNotNull(participantId)) {
            return true;
        }
        if (isParticipantLimitReached()) {
            return false;
        }

        validateDemographic("Age Group", ageGroup, AGE_GROUPS);
        validateDemographic("Dining frequency", diningFrequency, DINING_FREQUENCIES);
        List<AnswerInsertRow> rows = mapAndSanitizeRows(payload);
        if (!areItemsUnderLimit(payload)) return false;
        appendDemographicRow(rows, participantId, "Age Group", ageGroup);
        appendDemographicRow(rows, participantId, "How often do you dine at cafés or restaurants?", diningFrequency);
        batchInsertAnswers(rows);
        return true;
    }

    private void validateDemographic(String field, @Nullable String value, Set<String> supportedValues) {
        if (value != null && !value.isBlank() && !supportedValues.contains(value.trim())) {
            throw new IllegalArgumentException("Unsupported " + field + " value");
        }
    }

    private void lockSubmissions() {
        // Transaction-scoped PostgreSQL lock serializes limit checks and writes
        // across replicas, including simultaneous retries of the same session.
        jdbcTemplate.execute("SELECT pg_advisory_xact_lock(740192601)");
    }

    private boolean areItemsUnderLimit(List<CategorySubmissionDTO> payload) {
        long limit = surveyProperties.getItemRespondentLimit();
        if (limit <= 0) return true;
        return payload.stream().map(CategorySubmissionDTO::getMenuItemId).distinct()
                .allMatch(id -> {
                    Long count = answerRepository.countTotalResponsesForItem(id);
                    return count == null || count < limit;
                });
    }

    private void appendDemographicRow(
            List<AnswerInsertRow> rows,
            String participantId,
            String questionText,
            @Nullable String responseText
    ) {
        if (responseText == null || responseText.isBlank()) return;

        Long questionId = questionRepository.findAll().stream()
                .filter(question -> question.getText() != null &&
                        question.getText().trim().equalsIgnoreCase(questionText))
                .map(Question::getId)
                .findFirst()
                .orElseGet(() -> {
                    Question question = new Question();
                    question.setText(questionText);
                    question.setQuestionType("RADIO");
                    return questionRepository.save(question).getId();
                });
        rows.add(new AnswerInsertRow(participantId, null, questionId, null,
                sanitizeTextResponse(responseText)));
    }

    /**
     * Maximum number of respondents. An explicit survey.participant-limit (> 0) wins;
     * otherwise it is derived so every item can reach its respondent limit:
     * ceil(menu items x per-item limit / items per participant). The dashboard shows this same value.
     * Returns 0 when no limit applies.
     */
    public long resolveParticipantLimit() {
        long explicit = surveyProperties.getParticipantLimit();
        if (explicit > 0) {
            return explicit;
        }
        long perItem = surveyProperties.getItemRespondentLimit();
        if (perItem <= 0) {
            return 0;
        }
        long perParticipant = Math.max(1, surveyProperties.getItemsPerParticipant());
        long derived = (menuItemRepository.count() * perItem + perParticipant - 1) / perParticipant;
        return Math.max(derived, perItem);
    }

    private boolean isParticipantLimitReached() {
        Long totalParticipants = answerRepository.countTotalParticipants();
        long limit = resolveParticipantLimit();
        if(limit <= 0) {
            return false;
        }
        return totalParticipants != null && totalParticipants >= limit;
    }

    private List<AnswerInsertRow> mapAndSanitizeRows(List<CategorySubmissionDTO> payload) {
        List<AnswerInsertRow> rows = new ArrayList<>();
        var seen = new java.util.HashSet<AnswerKey>();
        var items = new java.util.HashSet<Long>();
        Map<Long, String> questionTypes = new HashMap<>();
        for(CategorySubmissionDTO dto : payload) {
            if (dto == null || dto.getMenuItemId() == null
                    || !menuItemRepository.existsById(dto.getMenuItemId())) {
                throw new IllegalArgumentException("Invalid menu item");
            }
            Long validQuestionId = resolveValidQuestionId(dto.getQuestionId());
            if (!seen.add(new AnswerKey(dto.getMenuItemId(), validQuestionId, dto.getSelectedOptionId()))) {
                throw new IllegalArgumentException("Duplicate answer");
            }
            items.add(dto.getMenuItemId());
            if (items.size() > surveyProperties.getItemsPerParticipant()) {
                throw new IllegalArgumentException("Too many menu items");
            }
            if (dto.getSelectedOptionId() == null &&
                    (dto.getTextResponse() == null || dto.getTextResponse().isBlank())) {
                throw new IllegalArgumentException("Answer requires an option or text response");
            }
            if (dto.getSelectedOptionId() != null) {
                var option = optionRepository.findById(dto.getSelectedOptionId())
                        .orElseThrow(() -> new IllegalArgumentException("Invalid option"));
                if (option.getQuestion() == null || !validQuestionId.equals(option.getQuestion().getId())) {
                    throw new IllegalArgumentException("Option does not belong to the submitted question");
                }
                String questionType = questionTypes.computeIfAbsent(
                        validQuestionId, questionRepository::findQuestionTypeById);
                String response = dto.getTextResponse();
                if ("MATRIX".equalsIgnoreCase(questionType)
                        || (dto.getSelectedOptionId() != null
                        && RatingResponseParser.looksLikeRating(response))) {
                    response = canonicalizeMatrixRating(response, option);
                }
                rows.add(new AnswerInsertRow(
                        dto.getUserId(), dto.getMenuItemId(), validQuestionId,
                        dto.getSelectedOptionId(), sanitizeTextResponse(response)));
            } else {
                String questionType = questionTypes.computeIfAbsent(
                        validQuestionId, questionRepository::findQuestionTypeById);
                if ("MATRIX".equalsIgnoreCase(questionType)
                        || ("RADIO".equalsIgnoreCase(questionType)
                        && RatingResponseParser.looksLikeRating(dto.getTextResponse()))) {
                    throw new IllegalArgumentException("Matrix rating requires a valid dimension");
                }
                rows.add(new AnswerInsertRow(
                        dto.getUserId(), dto.getMenuItemId(), validQuestionId, null,
                        sanitizeTextResponse(dto.getTextResponse())));
            }
        }
        return rows;
    }

    private String canonicalizeMatrixRating(@Nullable String response,
                                            com.example.survey.model.Option selectedOption) {
        RatingResponseParser.ParsedRating rating = RatingResponseParser.parse(response)
                .orElseThrow(() -> new IllegalArgumentException("Matrix rating must be between 1 and 5"));
        String label = selectedOption.getLabel();
        if (label == null || label.isBlank()) {
            throw new IllegalArgumentException("Matrix dimension is invalid");
        }

        Set<String> allowedLabels = new java.util.HashSet<>();
        allowedLabels.add(normalizeDimensionLabel(label));
        if (selectedOption.getSubDescription() != null && !selectedOption.getSubDescription().isBlank()) {
            allowedLabels.add(normalizeDimensionLabel(label + " " + selectedOption.getSubDescription()));
        }
        if (selectedOption.getIcon() != null && !selectedOption.getIcon().isBlank()) {
            allowedLabels.add(normalizeDimensionLabel(selectedOption.getIcon() + " " + label));
        }
        if (!allowedLabels.contains(normalizeDimensionLabel(rating.dimension()))) {
            throw new IllegalArgumentException("Matrix dimension does not match the selected option");
        }

        String expectedScale = RATING_SCALE_LABELS.get(rating.value() - 1);
        if (rating.scaleLabel() != null && !expectedScale.equalsIgnoreCase(rating.scaleLabel().trim())) {
            throw new IllegalArgumentException("Matrix rating scale label is invalid");
        }
        return label.trim() + ": " + rating.value() + " (" + expectedScale + ")";
    }

    private String normalizeDimensionLabel(String label) {
        return label == null ? "" : label.trim().replaceAll("\\s+", " ").toLowerCase(java.util.Locale.ROOT);
    }

    private Long resolveValidQuestionId(Long submittedQuestionId) {
        if (submittedQuestionId != null && questionRepository.existsById(submittedQuestionId)) {
            return submittedQuestionId;
        }

        throw new IllegalArgumentException("Invalid question");
    }

    private @Nullable String sanitizeTextResponse(@Nullable String textResponse) {
        if(textResponse == null || textResponse.isEmpty()) {
            return textResponse;
        }

        // Use UTF-8 so that Unicode characters like the en-dash in age-group labels
        // ("18–20", "21–23", …) are preserved as-is. The default ISO-8859-1 encoding
        // would escape U+2013 (–) to "&#8211;", making DB keys mismatch the lookup
        // strings in the analytics dashboard and causing age-group counts to show 0.
        String cleanText = HtmlUtils.htmlEscape(textResponse, "UTF-8");
        int maxLength = surveyProperties.getTextResponseMaxLength();
        if(maxLength > 0 && cleanText.length() > maxLength) {
            return cleanText.substring(0, maxLength);
        }
        return cleanText;
    }

    private void batchInsertAnswers(List<AnswerInsertRow> rows) {
        jdbcTemplate.batchUpdate(INSERT_ANSWER_SQL, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                AnswerInsertRow row = rows.get(i);
                ps.setString(1, row.userId());
                if (row.menuItemId() == null) {
                    ps.setNull(2, Types.BIGINT);
                } else {
                    ps.setLong(2, row.menuItemId());
                }
                ps.setLong(3, row.questionId());
                if(row.selectedOptionId() == null) {
                    ps.setNull(4, Types.BIGINT);
                } else {
                    ps.setLong(4, row.selectedOptionId());
                }
                ps.setString(5, row.textResponse());
            }

            @Override
            public int getBatchSize() {
                return rows.size();
            }
        });
    }

    public void saveDemographicAnswer(@Nullable String userId, @Nullable String questionText, @Nullable String responseText) {
        if (userId == null || questionText == null || responseText == null || responseText.isBlank()) {
            return;
        }
        try {
            Long questionId = questionRepository.findAll().stream()
                    .filter(q -> q.getText() != null && q.getText().trim().equalsIgnoreCase(questionText.trim()))
                    .map(Question::getId)
                    .findFirst()
                    .orElseGet(() -> {
                        Question newQ = new Question();
                        newQ.setText(questionText.trim());
                        newQ.setQuestionType("RADIO");
                        return questionRepository.save(newQ).getId();
                    });

            String insertDemographicSql = "INSERT INTO answers (user_id, menu_item_id, question_id, option_id, response) VALUES (?, NULL, ?, NULL, ?)";
            jdbcTemplate.update(insertDemographicSql, userId, questionId, sanitizeTextResponse(responseText));
        } catch (Exception e) {
            log.warn("Failed to save demographic answer for user {}: {}", userId, e.getMessage());
        }
    }

    private record AnswerInsertRow(
            String userId,
            @Nullable Long menuItemId,
            Long questionId,
            @Nullable Long selectedOptionId,
            @Nullable String textResponse
    ) {}

    private record AnswerKey(Long menuItemId, Long questionId, @Nullable Long optionId) {}
}
