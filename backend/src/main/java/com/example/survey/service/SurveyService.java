package com.example.survey.service;

import com.example.survey.config.SurveyProperties;
import com.example.survey.dto.CategorySubmissionDTO;
import com.example.survey.repository.AnswerRepository;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.HtmlUtils;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@NullMarked
public class SurveyService {

    private final AnswerRepository answerRepository;
    private final JdbcTemplate jdbcTemplate;
    private final SurveyProperties surveyProperties;

    private static final String INSERT_ANSWER_SQL = "INSERT INTO answers (user_id, menu_item_id, question_id, option_id, response) VALUES (?, ?, ?, ?, ?)";

    // This annotation locks the transaction at the database level.
    // It guarantees the count check and inserts happen atomically.
    @Transactional(isolation = Isolation.SERIALIZABLE)
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
            rows.add(new AnswerInsertRow(
                    dto.getUserId(),
                    dto.getMenuItemId(),
                    dto.getQuestionId(),
                    dto.getSelectedOptionId(),
                    sanitizeTextResponse(dto.getTextResponse())
            ));
        }
        return rows;
    }

    private @Nullable String sanitizeTextResponse(@Nullable String textResponse) {
        if(textResponse == null || textResponse.isEmpty()) {
            return textResponse;
        }

        String cleanText = HtmlUtils.htmlEscape(textResponse);
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

    private record AnswerInsertRow(
            String userId,
            Long menuItemId,
            Long questionId,
            @Nullable Long selectedOptionId,
            @Nullable String textResponse
    ) {}
}
