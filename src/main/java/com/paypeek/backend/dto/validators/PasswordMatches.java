package com.paypeek.backend.dto.validators;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = PasswordMatchesValidator.class)
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface PasswordMatches {
    String message() default "Le password non coincidono";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
