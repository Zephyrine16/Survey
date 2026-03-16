package com.example.survey.dto;

public class CategorySubmissionDTO {
    private Long userId;
    private Long menuItemId;
    private Long questionId;
    private Long selectedOptionId;
    private String textResponse;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Long getMenuItemId() { return menuItemId; }
    public void setMenuItemId(Long menuItemId) { this.menuItemId = menuItemId; }

    public Long getQuestionId() { return questionId; }
    public void setQuestionId(Long questionId) { this.questionId = questionId; }

    public Long getSelectedOptionId() { return selectedOptionId; }
    public void setSelectedOptionId(Long selectedOptionId) { this.selectedOptionId = selectedOptionId; }

    public String getTextResponse() { return textResponse; }
    public void setTextResponse(String textResponse) { this.textResponse = textResponse; }
}
