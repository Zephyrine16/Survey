package com.example.survey.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.Data;
import java.util.List;

@Data // This single word replaces all your getters, setters, and constructors!
@Entity
@Table(name = "questions")
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String text;

    @Column(name = "short_text")
    private String shortText;

    @Column(name = "question_type", nullable = false)
    @JsonProperty("type") // Tells Vue this is "type"
    private String questionType;

    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @OrderBy("id ASC")
    private List<Option> options;
}