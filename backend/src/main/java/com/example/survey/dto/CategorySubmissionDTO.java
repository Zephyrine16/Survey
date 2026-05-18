package com.example.survey.dto;

import com.example.survey.validation.TextResponseLength;
import jakarta.validation.constraints.NotNull;

public class CategorySubmissionDTO {

    private String userId;

    @NotNull(message = "menuItemId is required")
    private Long menuItemId;

    @NotNull(message = "questionId is required")
    private Long questionId;

    private Long selectedOptionId;

    @TextResponseLength
    private String textResponse;

    // --- GETTERS & SETTERS ---

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Long getMenuItemId() {
        return menuItemId;
    }

    public void setMenuItemId(Long menuItemId) {
        this.menuItemId = menuItemId;
    }

    public Long getQuestionId() {
        return questionId;
    }

    public void setQuestionId(Long questionId) {
        this.questionId = questionId;
    }

    public Long getSelectedOptionId() {
        return selectedOptionId;
    }

    public void setSelectedOptionId(Long selectedOptionId) {
        this.selectedOptionId = selectedOptionId;
    }

    public String getTextResponse() {
        return textResponse;
    }

    public void setTextResponse(String textResponse) {
        this.textResponse = textResponse;
    }
}