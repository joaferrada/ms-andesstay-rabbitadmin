package com.andesstay.ms_andesstay_rabbitadmin.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.constraints.Pattern;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Routing key o patron de binding: hasta 255 caracteres [a-zA-Z0-9._-] mas los comodines
 * de topic '*' y '#'. Puede ser vacia (fanout/headers ignoran la key).
 */
@Documented
@Constraint(validatedBy = {})
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
@Pattern(regexp = "^[a-zA-Z0-9._*#-]{0,255}$",
        message = "routing key invalida: hasta 255 caracteres, letras, numeros, '.', '_', '-', '*' o '#'")
public @interface RoutingKey {

    String message() default "routing key invalida";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
