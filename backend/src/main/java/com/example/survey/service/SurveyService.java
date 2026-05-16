package com.example.survey.service;

import com.example.survey.dto.CategorySubmissionDTO;
import com.example.survey.repository.AnswerRepository;
import org.springframework.beans.factory.annotation.Autowired;
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
public class SurveyService {

    @Autowired
    private AnswerRepository answerRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public static final long PARTICIPANT_LIMIT = 30L;
    private static final int MAX_TEXT_RESPONSE_LENGTH = 250;
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
        return totalParticipants != null && totalParticipants >= PARTICIPANT_LIMIT;
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

    private String sanitizeTextResponse(String textResponse) {
        if(textResponse == null || textResponse.isEmpty()) {
            return textResponse;
        }

        String cleanText = HtmlUtils.htmlEscape(textResponse);
        if (cleanText.length() > MAX_TEXT_RESPONSE_LENGTH) {
            return cleanText.substring(0, MAX_TEXT_RESPONSE_LENGTH);
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
            Long selectedOptionId,
            String textResponse
    ) {}
}
