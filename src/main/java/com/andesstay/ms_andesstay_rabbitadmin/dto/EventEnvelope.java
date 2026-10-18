package com.andesstay.ms_andesstay_rabbitadmin.dto;

import java.time.Instant;
import java.util.Map;

/**
 * Sobre comun de los mensajes de AndesStay (mismo formato que publica reservations).
 */
public record EventEnvelope(
        String type,
        String eventId,
        Instant timestamp,
        String traceId,
        String correlationId,
        String source,
        Map<String, Object> payload) {
}
