package com.andesstay.ms_andesstay_rabbitadmin.dto;

import com.andesstay.ms_andesstay_rabbitadmin.validation.AmqpName;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * POST /api/admin/exchanges. Ejemplo: {"name":"cmd.demo","type":"topic","durable":true}
 */
public record CreateExchangeRequest(
        @NotBlank(message = "name es obligatorio") @AmqpName String name,
        @NotNull(message = "type es obligatorio (direct, topic, fanout, headers)") ExchangeKind type,
        Boolean durable) {

    public boolean isDurable() {
        return durable == null || durable;
    }
}
