package com.resq.backend.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Constraint(validatedBy = CoordenadasCoherentesValidator.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface CoordenadasCoherentes {

    String message() default "latitud y longitud deben enviarse juntas";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
