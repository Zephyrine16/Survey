package com.example.survey.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class MenuItemDescriptionRequest {
    @Size(max = 1000, message = "description is too long")
    private String description;
}
