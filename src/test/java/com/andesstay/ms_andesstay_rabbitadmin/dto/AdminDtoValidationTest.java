package com.andesstay.ms_andesstay_rabbitadmin.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AdminDtoValidationTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void init() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void close() {
        factory.close();
    }

    @ParameterizedTest
    @ValueSource(strings = {"q.cmd.email", "cmd.dead.dlx", "q_demo-1", "abc"})
    void nombresValidos(String name) {
        assertThat(validator.validate(new CreateQueueRequest(name, true, null, null, null, null))).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"ab", "con espacio", "q/demo", "cola*", "ñandu"})
    void nombresInvalidos(String name) {
        assertThat(validator.validate(new CreateQueueRequest(name, true, null, null, null, null))).isNotEmpty();
    }

    @Test
    void prefijoAmqEstaProhibido() {
        Set<ConstraintViolation<CreateQueueRequest>> v =
                validator.validate(new CreateQueueRequest("amq.mi-cola", true, null, null, null, null));
        assertThat(v).extracting(ConstraintViolation::getMessage).contains("el prefijo 'amq.' esta reservado por RabbitMQ");
    }

    @Test
    void nombreVacioEsObligatorio() {
        assertThat(validator.validate(new CreateQueueRequest(" ", true, null, null, null, null)))
                .extracting(ConstraintViolation::getMessage).contains("name es obligatorio");
    }

    @Test
    void ttlYMaxLengthDebenSerPositivos() {
        Set<ConstraintViolation<CreateQueueRequest>> v =
                validator.validate(new CreateQueueRequest("q.demo", true, -1L, 0, null, null));
        assertThat(v).extracting(c -> c.getPropertyPath().toString()).contains("ttlMs", "maxLength");
    }

    @Test
    void deadLetterRoutingKeySinExchangeEsInvalido() {
        assertThat(validator.validate(new CreateQueueRequest("q.demo", true, null, null, null, "demo.dead")))
                .extracting(ConstraintViolation::getMessage).contains("deadLetterRoutingKey requiere deadLetterExchange");
    }

    @Test
    void tipoDeExchangeSoloAceptaLosCuatroDeRabbit() {
        assertThat(ExchangeKind.from("TOPIC")).isEqualTo(ExchangeKind.TOPIC);
        assertThat(ExchangeKind.from("headers")).isEqualTo(ExchangeKind.HEADERS);
        assertThatThrownBy(() -> ExchangeKind.from("x-delayed"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("direct, topic, fanout, headers");
    }

    @Test
    void exchangeSinTipoEsInvalido() {
        assertThat(validator.validate(new CreateExchangeRequest("cmd.demo", null, true))).hasSize(1);
    }

    @Test
    void bindingAceptaComodinesDeTopic() {
        assertThat(validator.validate(new BindingRequest("cmd.topic", "q.demo", "housekeeping.#"))).isEmpty();
        assertThat(validator.validate(new BindingRequest("cmd.topic", "q.demo", "con espacio"))).isNotEmpty();
    }

    @Test
    void publicarExigeExactamenteUnoEntrePayloadYRawBody() {
        assertThat(validator.validate(new PublishMessageRequest("cmd.direct", "email.send", null, null, null,
                Map.of("reservationId", 1), null, null))).isEmpty();
        assertThat(validator.validate(new PublishMessageRequest("cmd.direct", "email.send", null, null, null,
                null, "{roto", null))).isEmpty();
        assertThat(validator.validate(new PublishMessageRequest("cmd.direct", "email.send", null, null, null,
                null, null, null))).isNotEmpty();
        assertThat(validator.validate(new PublishMessageRequest("cmd.direct", "email.send", null, null, null,
                Map.of("a", 1), "{}", null))).isNotEmpty();
    }

    @Test
    void eventIdDebeSerUuid() {
        assertThat(validator.validate(new PublishMessageRequest("cmd.direct", "email.send", null, "no-uuid", null,
                Map.of(), null, null))).isNotEmpty();
    }
}
