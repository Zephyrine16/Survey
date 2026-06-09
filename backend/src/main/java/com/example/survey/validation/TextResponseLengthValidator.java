package com.example.survey.validation;

import com.example.survey.config.SurveyProperties;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TextResponseLengthValidator implements ConstraintValidator<TextResponseLength, String> {

    private final SurveyProperties surveyProperties;

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true;
        }

        int maxLength = surveyProperties.getTextResponseMaxLength();
        if (maxLength <= 0) {
            return true;
        }

        return value.length() <= maxLength;
    }
}
