package com.ghostcoach.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class EnumValidator implements ConstraintValidator<ValidEnum, String> {

    private Enum<?>[] enumValues;
    private boolean ignoreCase;

    @Override
    public void initialize(ValidEnum annotation) {
        enumValues = annotation.enumClass().getEnumConstants();
        ignoreCase = annotation.ignoreCase();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) return false;
        for (Enum<?> constant : enumValues) {
            String name = constant.name();
            if (ignoreCase ? name.equalsIgnoreCase(value) : name.equals(value)) {
                return true;
            }
        }
        return false;
    }
}
