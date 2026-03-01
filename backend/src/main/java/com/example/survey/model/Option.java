package com.example.survey.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

@Entity
@Table(name = "options")
public class Option {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String label;

    @Column(name = "sub_description")
    private String sub_description;

    @ManyToOne
    @JoinColumn(name = "question_id", nullable = false)
    @JsonIgnore
    private Question question;

    public Option() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    public String getSub_description() { return sub_description; }
    public void setSub_description(String sub_description) { this.sub_description = sub_description; }

    public Question getQuestion() { return question; }
    public void setQuestion(Question question) { this.question = question; }
}
