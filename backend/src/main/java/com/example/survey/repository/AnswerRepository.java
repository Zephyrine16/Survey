package com.example.survey.repository;

import com.example.survey.dto.OptionCountDTO;
import com.example.survey.dto.TextFeedbackDTO;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AnswerRepository {
    @Query("""
        SELECT new com.example.survey.dto.OptionCountDTO(o.label, COUNT(a))
        FROM Answer a
        JOIN a.selectedOption o
        WHERE a.menuItem.id = :menuItemId
        AND a.question.id = :questionid
        GROUP BY o.label
    """)
    List<OptionCountDTO> countVotesByOption(
            @Param("menuItemId") Long menuItemId,
            @Param("questionId") Long questionId
    );

    @Query("""
        SELECT new com.example.survey.dto.TextFeedbackDTO(a.userEmail, a.response)
        FROM Answer a
        WHERE a.menuItem.id = :menuItemId
        AND a.question.id = :questionId
        AND a.response IS NOT NULL
    """)
    List<TextFeedbackDTO> findTextResponse(
            @Param("menuItemId") Long menuItemId,
            @Param("questionId") Long questionId
    );
}
