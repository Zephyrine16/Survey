package com.example.survey.service;

import com.example.survey.config.SurveyProperties;
import com.example.survey.dto.CategorySubmissionDTO;
import com.example.survey.model.Question;
import com.example.survey.repository.AnswerRepository;
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
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@NullMarked
public class SurveyService {

    private final AnswerRepository answerRepository;
    private final QuestionRepository questionRepository;
    private final JdbcTemplate jdbcTemplate;
    private final SurveyProperties surveyProperties;

    private static final String INSERT_ANSWER_SQL = "INSERT INTO answers (user_id, menu_item_id, question_id, option_id, response) VALUES (?, ?, ?, ?, ?)";

    @Transactional
    public boolean saveSurveyIfUnderLimit(List<CategorySubmissionDTO> payload) {

        if (isParticipantLimitReached()) {
            return false;
        }

        List<AnswerInsertRow> rows = mapAndSanitizeRows(payload);
        batchInsertAnswers(rows);

        return true;
    }

    private boolean isParticipantLimitReached() {
        Long totalParticipants = answerRepository.countTotalParticipants();
        long limit = surveyProperties.getParticipantLimit();
        if(limit <= 0) {
            return false;
        }
        return totalParticipants != null && totalParticipants >= limit;
    }

    private List<AnswerInsertRow> mapAndSanitizeRows(List<CategorySubmissionDTO> payload) {
        List<AnswerInsertRow> rows = new ArrayList<>();
        for(CategorySubmissionDTO dto : payload) {
            Long validQuestionId = resolveValidQuestionId(dto.getQuestionId(), dto.getTextResponse());
            rows.add(new AnswerInsertRow(
                    dto.getUserId(),
                    dto.getMenuItemId(),
                    validQuestionId,
                    dto.getSelectedOptionId(),
                    sanitizeTextResponse(dto.getTextResponse())
            ));
        }
        return rows;
    }

    private Long resolveValidQuestionId(Long submittedQuestionId, @Nullable String textResponse) {
        if (submittedQuestionId != null && questionRepository.existsById(submittedQuestionId)) {
            return submittedQuestionId;
        }

        String lowerText = textResponse != null ? textResponse.toLowerCase() : "";
        boolean isWeather = lowerText.contains("weather")
                || lowerText.contains("sunny")
                || lowerText.contains("humid")
                || lowerText.contains("rain")
                || lowerText.contains("cool");

        String keyword = isWeather ? "weather" : "mood";
        return questionRepository.findAll().stream()
                .filter(q -> q.getText() != null && (q.getText().toLowerCase().contains(keyword) || (!isWeather && q.getText().toLowerCase().contains("emotion"))))
                .map(Question::getId)
                .findFirst()
                .orElseGet(() -> {
                    return questionRepository.findAll().stream()
                            .findFirst()
                            .map(Question::getId)
                            .orElseGet(() -> {
                                Question newQ = new Question();
                                newQ.setText(isWeather
                                        ? "Question 2 — Weather Association: How suitable is this item for each of the following weather conditions?"
                                        : "Question 1 — Mood Association: How suitable is this item for each of the following moods?");
                                newQ.setQuestionType("TEXT");
                                return questionRepository.save(newQ).getId();
                            });
                });
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
                ps.setLong(2, row.menuItemId());
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
            Long menuItemId,
            Long questionId,
            @Nullable Long selectedOptionId,
            @Nullable String textResponse
    ) {}
}
