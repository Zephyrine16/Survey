package com.example.survey.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class OptionRequest {
    @NotBlank(message = "label is required")
    @Size(max = 200, message = "label is too long")
    private String label;

    @Size(max = 20, message = "icon is too long")
    private String icon;

    @JsonProperty("sub")
    @Size(max = 200, message = "sub is too long")
    private String sub;
}
