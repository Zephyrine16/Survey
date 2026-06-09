package com.example.survey.repository;

import com.example.survey.model.Answer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AnswerRepository extends JpaRepository<Answer, Long> {

    @Query("""
        SELECT a.question.id, o.label, COUNT(a)
        FROM Answer a
        JOIN a.selectedOption o
        WHERE a.menuItem.id = :menuItemId
        AND a.question.questionType = 'RADIO'
        GROUP BY a.question.id, o.label
    """)
    List<Object[]> countVotesByOptionForMenuItem(@Param("menuItemId") Long menuItemId);

    @Query("""
        SELECT a.question.id, a.userId, a.response
        FROM Answer a
        WHERE a.menuItem.id = :menuItemId
        AND a.question.questionType = 'TEXT'
        AND a.response IS NOT NULL
    """)
    List<Object[]> findTextResponsesForMenuItem(@Param("menuItemId") Long menuItemId);

    // ==========================================
    // DATA INGESTION & EXPORT QUERIES
    // ==========================================

    @Query(value = "SELECT a.user_id, m.name, q.text, o.label, a.response " +
            "FROM answers a " +
            "JOIN menu_items m ON a.menu_item_id = m.id " +
            "JOIN questions q ON a.question_id = q.id " +
            "LEFT JOIN options o ON a.option_id = o.id", nativeQuery = true)
    List<Object[]> getExportData();

    // ==========================================
    // DASHBOARD KPI QUERIES (NO HARDCODED IDS)
    // ==========================================

    // 1. Get all text descriptions. We join the question table and look for 'TEXT' types.
    @Query(value = "SELECT a.response FROM answers a JOIN questions q ON a.question_id = q.id WHERE a.menu_item_id = :menuItemId AND q.question_type = 'TEXT' AND a.response IS NOT NULL AND TRIM(a.response) != ''", nativeQuery = true)
    List<String> getTextReviewsForItem(@Param("menuItemId") Long menuItemId);

    // 2. Count distinct unique users across the whole database.
    @Query(value = "SELECT COUNT(DISTINCT user_id) FROM answers", nativeQuery = true)
    Long countGlobalTotalResponses();

    // CARD 1 (MACRO/BASELINE): Get the lowest number of unique users who reviewed any menu item.
    @Query(value = "SELECT COALESCE(MIN(response_count), 0) FROM (" +
            "SELECT m.id, COUNT(DISTINCT a.user_id) AS response_count " +
            "FROM menu_items m " +
            "LEFT JOIN answers a ON m.id = a.menu_item_id " +
            "GROUP BY m.id) AS counts", nativeQuery = true)
    Long getBaselineResponseCount();

    // CARD 2 (MICRO): Count distinct unique users for a specific item.
    @Query(value = "SELECT COUNT(DISTINCT user_id) FROM answers WHERE menu_item_id = :menuItemId", nativeQuery = true)
    Long countTotalResponsesForItem(@Param("menuItemId") Long menuItemId);

    // Count unique people globally (by their random session ID).
    @Query("SELECT COUNT(DISTINCT a.userId) FROM Answer a")
    Long countTotalParticipants();
}