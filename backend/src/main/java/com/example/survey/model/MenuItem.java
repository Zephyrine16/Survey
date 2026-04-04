package com.example.survey.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "menu_items")
@Data
@NoArgsConstructor
public class MenuItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private String category;

    private Double price;

    @Column(name = "image_name") // Ensures the database column is named cleanly
    private String imageName;

    public MenuItem(String name, String category, Double price, String imageName) {
        this.name = name;
        this.category = category;
        this.price = price;
        this.imageName = imageName;
    }
}