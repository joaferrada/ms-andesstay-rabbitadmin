package com.andesstay.ms_andesstay_rabbitadmin.dto;

import com.andesstay.ms_andesstay_rabbitadmin.validation.AmqpName;
import com.andesstay.ms_andesstay_rabbitadmin.validation.RoutingKey;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

/**
 * POST /api/admin/queues
 *
 * @param durable              sobrevive a reinicios del broker (por defecto true)
 * @param ttlMs                x-message-ttl en milisegundos (opcional, &gt; 0)
 * @param maxLength            x-max-length (opcional, &gt; 0)
 * @param deadLetterExchange   x-dead-letter-exchange (opcional)
 * @param deadLetterRoutingKey x-dead-letter-routing-key (opcional, requiere deadLetterExchange)
 */
public record CreateQueueRequest(
        @NotBlank(message = "name es obligatorio") @AmqpName String name,
        Boolean durable,
        @Positive(message = "ttlMs debe ser positivo") Long ttlMs,
        @Positive(message = "maxLength debe ser positivo") Integer maxLength,
        @AmqpName String deadLetterExchange,
        @RoutingKey String deadLetterRoutingKey) {

    public boolean isDurable() {
        return durable == null || durable;
    }

    @JsonIgnore
    @AssertTrue(message = "deadLetterRoutingKey requiere deadLetterExchange")
    public boolean isDeadLetterConsistent() {
        return deadLetterRoutingKey == null || deadLetterExchange != null;
    }
}
