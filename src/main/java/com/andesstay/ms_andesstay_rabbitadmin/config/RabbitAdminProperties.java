package com.andesstay.ms_andesstay_rabbitadmin.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * Parametros del microservicio administrador ({@code andesstay.rabbitadmin.*}).
 */
@Validated
@ConfigurationProperties(prefix = "andesstay.rabbitadmin")
public record RabbitAdminProperties(
        @NotBlank String source,
        @NotNull Duration confirmTimeout,
        @Valid @NotNull Management management) {

    /** API HTTP de RabbitMQ Management (solo para listar colas). */
    public record Management(
            @NotBlank String url,
            @NotBlank String username,
            @NotBlank String password,
            @NotBlank String vhost) {
    }
}
