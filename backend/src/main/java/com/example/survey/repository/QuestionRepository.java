package com.example.survey.repository;

import com.example.survey.model.Question;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Long> {

    @Query("""
        SELECT q.id AS id, q.text AS text, q.questionType AS questionType
        FROM Question q
        ORDER BY q.id ASC
    """)
    List<QuestionSummaryProjection> findAllQuestionSummaries();

    @Query("""
        SELECT DISTINCT q
        FROM Question q
        LEFT JOIN FETCH q.options
        ORDER BY q.id ASC
    """)
    List<Question> findAllWithOptions();
}
