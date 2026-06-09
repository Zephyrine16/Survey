package com.example.survey.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class MenuItemRequest {
    @NotBlank(message = "name is required")
    @Size(max = 200, message = "name is too long")
    private String name;

    @NotBlank(message = "category is required")
    @Size(max = 100, message = "category is too long")
    private String category;

    @DecimalMin(value = "0.0", message = "price must be non-negative")
    private Double price;

    @Size(max = 255, message = "imageName is too long")
    private String imageName;
}
