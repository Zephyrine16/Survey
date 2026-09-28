package com.example.survey.repository;

import com.example.survey.model.MenuItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MenuItemRepository extends JpaRepository<MenuItem, Long> {

    /** Detaches all answers from every menu item (sets menu_item_id = NULL)
     *  so the FK constraint doesn't block a subsequent batch delete. */
    @Modifying
    @Query("UPDATE Answer a SET a.menuItem = NULL WHERE a.menuItem IS NOT NULL")
    void detachAllAnswersFromMenuItems();

    /**
     * Retrieves all menu items whose distinct respondent count is strictly less than the given limit.
     * Items that have reached or exceeded the limit are pulled out of the survey poll.
     */
    @Query(value = """
        SELECT m.* FROM menu_items m
        LEFT JOIN (
            SELECT a.menu_item_id, COUNT(DISTINCT a.user_id) AS respondent_count
            FROM answers a
            WHERE a.menu_item_id IS NOT NULL
            GROUP BY a.menu_item_id
        ) resp ON m.id = resp.menu_item_id
        WHERE COALESCE(resp.respondent_count, 0) < :limit
        ORDER BY m.id ASC
    """, nativeQuery = true)
    List<MenuItem> findAvailableMenuItems(@Param("limit") long limit);

    /**
     * Counts how many menu items are still under the respondent limit.
     */
    @Query(value = """
        SELECT COUNT(*) FROM menu_items m
        LEFT JOIN (
            SELECT a.menu_item_id, COUNT(DISTINCT a.user_id) AS respondent_count
            FROM answers a
            WHERE a.menu_item_id IS NOT NULL
            GROUP BY a.menu_item_id
        ) resp ON m.id = resp.menu_item_id
        WHERE COALESCE(resp.respondent_count, 0) < :limit
    """, nativeQuery = true)
    long countAvailableMenuItems(@Param("limit") long limit);
}
