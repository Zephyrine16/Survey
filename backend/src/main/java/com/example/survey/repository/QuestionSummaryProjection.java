package com.example.survey.repository;

public interface QuestionSummaryProjection {
    Long getId();
    String getText();
    String getQuestionType();
}