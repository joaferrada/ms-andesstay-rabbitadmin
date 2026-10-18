package com.andesstay.ms_andesstay_rabbitadmin.dto;

import java.util.Map;

/**
 * Respuestas de la API de administracion.
 */
public final class AdminResponses {

    private AdminResponses() {
    }

    public record QueueResponse(String name, Long messageCount, Integer consumerCount,
                                Boolean durable, Map<String, Object> arguments) {
    }

    public record ExchangeResponse(String name, ExchangeKind type, boolean durable) {
    }

    public record BindingResponse(String exchange, String queue, String routingKey) {
    }

    public record PurgeResponse(String queue, int purgedMessages) {
    }

    public record PublishResponse(String eventId, String exchange, String routingKey, boolean confirmed) {
    }
}
