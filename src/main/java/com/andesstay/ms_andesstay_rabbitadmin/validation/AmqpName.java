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
 * Nombre valido de cola o exchange: 3-100 caracteres [a-zA-Z0-9._-] y sin el prefijo
 * reservado "amq." (RabbitMQ lo usa para sus objetos internos). null se considera valido;
 * combinar con @NotBlank cuando el campo es obligatorio.
 */
@Documented
@Constraint(validatedBy = {})
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
@Pattern(regexp = AmqpName.NAME_REGEX,
        message = "debe tener entre 3 y 100 caracteres y solo letras, numeros, punto, guion o guion bajo")
@Pattern(regexp = AmqpName.NOT_RESERVED_REGEX,
        message = "el prefijo 'amq.' esta reservado por RabbitMQ")
public @interface AmqpName {

    String NAME_REGEX = "^[a-zA-Z0-9._-]{3,100}$";
    String NOT_RESERVED_REGEX = "^(?!amq\\.).*$";

    String message() default "nombre AMQP invalido";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
