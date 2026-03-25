package com.example.survey.repository;

import com.example.survey.dto.OptionCountDTO;
import com.example.survey.dto.TextFeedbackDTO;
import com.example.survey.model.Answer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface AnswerRepository extends JpaRepository<Answer, Long> {

    @Query("""
        SELECT new com.example.survey.dto.OptionCountDTO(o.label, COUNT(a))
        FROM Answer a
        JOIN a.selectedOption o
        WHERE a.menuItem.id = :menuItemId
        AND a.question.id = :questionId
        GROUP BY o.label
    """)
    List<OptionCountDTO> countVotesByOption(
            @Param("menuItemId") Long menuItemId,
            @Param("questionId") Long questionId
    );

    @Query("""
        SELECT new com.example.survey.dto.TextFeedbackDTO(a.userId, a.response)
        FROM Answer a
        WHERE a.menuItem.id = :menuItemId
        AND a.question.id = :questionId
        AND a.response IS NOT NULL
    """)
    List<TextFeedbackDTO> findTextResponse(
            @Param("menuItemId") Long menuItemId,
            @Param("questionId") Long questionId
    );

    // FIX 1: Aligned the SQL columns (user_id) and the Params perfectly with the new DTO!
    @Modifying
    @Transactional
    @Query(value = "INSERT INTO answers (user_id, menu_item_id, question_id, option_id, response) VALUES (:userId, :menuItemId, :questionId, :selectedOptionId, :textResponse)", nativeQuery = true)
    void saveRawAnswer(
            @Param("userId") String userId,
            @Param("menuItemId") Long menuItemId,
            @Param("questionId") Long questionId,
            @Param("selectedOptionId") Long selectedOptionId,
            @Param("textResponse") String textResponse
    );

    // FIX 2: Changed a.user_email to a.user_id, and matched the selected_option_id column
    @Query(value = "SELECT a.user_id, m.name, q.text, o.label, a.response " +
            "FROM answers a " +
            "JOIN menu_items m ON a.menu_item_id = m.id " +
            "JOIN questions q ON a.question_id = q.id " +
            "LEFT JOIN options o ON a.option_id = o.id", nativeQuery = true)
    List<Object[]> getExportData();

    // 1. Get the total number of people who answered Question 1 for a specific item
    @Query(value = "SELECT COUNT(*) FROM answers WHERE menu_item_id = :menuItemId AND question_id = 1", nativeQuery = true)
    Long countTotalResponsesForItem(@Param("menuItemId") Long menuItemId);

    // 2. Get all the text descriptions (Question 5) for a specific item
    @Query(value = "SELECT response FROM answers WHERE menu_item_id = :menuItemId AND question_id = 5 AND response IS NOT NULL AND TRIM(response) != ''", nativeQuery = true)
    List<String> getTextReviewsForItem(@Param("menuItemId") Long menuItemId);

    // 3. Get global total responses (everyone who answered Question 1 across all items)
    @Query(value = "SELECT COUNT(*) FROM answers WHERE question_id = 1", nativeQuery = true)
    Long countGlobalTotalResponses();

    // 4. Get global total of text responses (everyone who answered Question 5)
    @Query(value = "SELECT COUNT(*) FROM answers WHERE question_id = 5 AND response IS NOT NULL AND TRIM(response) != ''", nativeQuery = true)
    Long countGlobalTextResponses();

    // Get the total number of UNIQUE participants based on their session ID
    @Query(value = "SELECT COUNT(DISTINCT user_id) FROM answers", nativeQuery = true)
    Long countUniqueParticipants();
}