package com.example.survey.repository;

import com.example.survey.model.MenuItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface MenuItemRepository extends JpaRepository<MenuItem, Long> {

    /** Detaches all answers from every menu item (sets menu_item_id = NULL)
     *  so the FK constraint doesn't block a subsequent batch delete. */
    @Modifying
    @Query("UPDATE Answer a SET a.menuItem = NULL WHERE a.menuItem IS NOT NULL")
    void detachAllAnswersFromMenuItems();
}
