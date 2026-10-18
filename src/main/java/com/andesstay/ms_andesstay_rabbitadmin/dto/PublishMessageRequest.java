package com.andesstay.ms_andesstay_rabbitadmin.dto;

import com.andesstay.ms_andesstay_rabbitadmin.validation.AmqpName;
import com.andesstay.ms_andesstay_rabbitadmin.validation.RoutingKey;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Map;

/**
 * POST /api/admin/messages: publica un mensaje de prueba.
 *
 * @param type     tipo del sobre (ej. EMAIL_RESERVA_CREADA)
 * @param eventId  opcional; repetir uno ya usado sirve para demostrar idempotencia
 * @param payload  payload del sobre (se envuelve en el envelope comun)
 * @param rawBody  alternativa a payload: se envia tal cual, sin sobre (para demostrar JSON invalido -> DLQ)
 * @param headers  headers AMQP adicionales (ej. x-retry-count)
 */
public record PublishMessageRequest(
        @NotBlank(message = "exchange es obligatorio") @AmqpName String exchange,
        @NotNull(message = "routingKey es obligatorio") @RoutingKey String routingKey,
        @Size(max = 100) @Pattern(regexp = "^[A-Za-z0-9._-]*$", message = "type solo admite letras, numeros, '.', '_' o '-'") String type,
        @Pattern(regexp = "^[0-9a-fA-F-]{36}$", message = "eventId debe ser un UUID") String eventId,
        @Size(max = 100) String correlationId,
        Map<String, Object> payload,
        @Size(max = 100_000) String rawBody,
        Map<String, Object> headers) {

    @JsonIgnore
    @AssertTrue(message = "envie exactamente uno de: payload o rawBody")
    public boolean isBodyProvided() {
        return (payload != null) ^ (rawBody != null);
    }
}
