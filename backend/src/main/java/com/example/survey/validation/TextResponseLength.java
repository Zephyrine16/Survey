package com.example.survey.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Documented
@Constraint(validatedBy = TextResponseLengthValidator.class)
@Target({ FIELD, PARAMETER })
@Retention(RUNTIME)
public @interface TextResponseLength {
    String message() default "textResponse exceeds the maximum allowed length.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default{};
}
