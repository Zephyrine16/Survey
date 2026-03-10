package com.example.survey.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class OptionCountDTO {
    private String optionLabel;
    private Long voteCount;
}
