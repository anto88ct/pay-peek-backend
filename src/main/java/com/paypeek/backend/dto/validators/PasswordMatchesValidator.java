package com.paypeek.backend.dto.validators;

import com.paypeek.backend.dto.SignupDto;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;


public class PasswordMatchesValidator implements ConstraintValidator<PasswordMatches, SignupDto> {
    @Override
    public boolean isValid(SignupDto dto, ConstraintValidatorContext context) {
        return dto.getPassword() != null
                && dto.getConfirmPassword() != null
                && dto.getPassword().equals(dto.getConfirmPassword());
    }
}