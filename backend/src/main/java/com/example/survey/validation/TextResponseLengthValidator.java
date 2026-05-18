package com.example.survey.validation;

import com.example.survey.config.SurveyProperties;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class TextResponseLengthValidator implements ConstraintValidator<TextResponseLength, String> {

    @Autowired
    private SurveyProperties surveyProperties;

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
