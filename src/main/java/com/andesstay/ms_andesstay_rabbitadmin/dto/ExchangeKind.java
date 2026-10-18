package com.andesstay.ms_andesstay_rabbitadmin.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Tipos de exchange soportados por RabbitMQ. En JSON se escriben en minuscula ("direct").
 */
public enum ExchangeKind {
    DIRECT("direct"),
    TOPIC("topic"),
    FANOUT("fanout"),
    HEADERS("headers");

    private final String value;

    ExchangeKind(String value) {
        this.value = value;
    }

    @JsonValue
    public String value() {
        return value;
    }

    @JsonCreator
    public static ExchangeKind from(String raw) {
        String normalized = raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
        return Arrays.stream(values())
                .filter(k -> k.value.equals(normalized))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Tipo de exchange invalido '" + raw + "'. Permitidos: " + allowed()));
    }

    public static String allowed() {
        return Arrays.stream(values()).map(ExchangeKind::value).collect(Collectors.joining(", "));
    }
}
