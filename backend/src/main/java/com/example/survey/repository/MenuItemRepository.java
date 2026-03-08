package com.example.survey.repository;

import com.example.survey.model.MenuItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

public interface MenuItemRepository extends JpaRepository<MenuItem, Long>{
    List<MenuItem> findByCategory(String category);
}
