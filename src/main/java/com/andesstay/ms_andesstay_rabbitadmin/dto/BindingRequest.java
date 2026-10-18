package com.andesstay.ms_andesstay_rabbitadmin.dto;

import com.andesstay.ms_andesstay_rabbitadmin.validation.AmqpName;
import com.andesstay.ms_andesstay_rabbitadmin.validation.RoutingKey;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * POST /api/admin/bindings. Ejemplo: {"exchange":"cmd.topic","queue":"q.demo","routingKey":"demo.#"}
 */
public record BindingRequest(
        @NotBlank(message = "exchange es obligatorio") @AmqpName String exchange,
        @NotBlank(message = "queue es obligatorio") @AmqpName String queue,
        @NotNull(message = "routingKey es obligatorio (puede ser vacio para fanout)") @RoutingKey String routingKey) {
}
