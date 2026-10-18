package com.andesstay.ms_andesstay_rabbitadmin.service;

import com.andesstay.ms_andesstay_rabbitadmin.config.RabbitAdminProperties;
import com.andesstay.ms_andesstay_rabbitadmin.dto.AdminResponses.BindingResponse;
import com.andesstay.ms_andesstay_rabbitadmin.dto.AdminResponses.ExchangeResponse;
import com.andesstay.ms_andesstay_rabbitadmin.dto.AdminResponses.PublishResponse;
import com.andesstay.ms_andesstay_rabbitadmin.dto.AdminResponses.PurgeResponse;
import com.andesstay.ms_andesstay_rabbitadmin.dto.AdminResponses.QueueResponse;
import com.andesstay.ms_andesstay_rabbitadmin.dto.BindingRequest;
import com.andesstay.ms_andesstay_rabbitadmin.dto.CreateExchangeRequest;
import com.andesstay.ms_andesstay_rabbitadmin.dto.CreateQueueRequest;
import com.andesstay.ms_andesstay_rabbitadmin.dto.EventEnvelope;
import com.andesstay.ms_andesstay_rabbitadmin.dto.PublishMessageRequest;
import com.andesstay.ms_andesstay_rabbitadmin.exception.AdminExceptions.BrokerUnavailableException;
import com.andesstay.ms_andesstay_rabbitadmin.exception.AdminExceptions.ResourceConflictException;
import com.andesstay.ms_andesstay_rabbitadmin.exception.AdminExceptions.ResourceNotFoundException;
import com.andesstay.ms_andesstay_rabbitadmin.exception.AdminExceptions.UnroutableMessageException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageBuilderSupport;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.QueueInformation;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Toda la administracion de RabbitMQ del microservicio. Los controllers solo llaman a estos
 * metodos de alto nivel; aqui se usa {@link RabbitAdmin} (declarar/borrar/consultar) y
 * {@link RabbitTemplate} (publicar con confirmacion). Listar colas usa la Management HTTP API
 * porque el protocolo AMQP no tiene esa operacion.
 */
@Service
public class RabbitAdminService {

    private static final Logger log = LoggerFactory.getLogger(RabbitAdminService.class);
    private static final String ARG_TTL = "x-message-ttl";
    private static final String ARG_MAX_LENGTH = "x-max-length";
    private static final String ARG_DLX = "x-dead-letter-exchange";
    private static final String ARG_DLK = "x-dead-letter-routing-key";
    private static final String DEFAULT_TYPE = "TEST_MESSAGE";
    private static final String NOT_FOUND_CODE = "reply-code=404";
    private static final String PRECONDITION_FAILED_CODE = "reply-code=406";

    private final RabbitAdmin rabbitAdmin;
    private final RabbitTemplate rabbitTemplate;
    private final RestClient managementClient;
    private final JsonMapper jsonMapper;
    private final RabbitAdminProperties props;

    public RabbitAdminService(RabbitAdmin rabbitAdmin, RabbitTemplate rabbitTemplate,
                              @Qualifier("managementRestClient") RestClient managementClient,
                              JsonMapper jsonMapper, RabbitAdminProperties props) {
        this.rabbitAdmin = rabbitAdmin;
        this.rabbitTemplate = rabbitTemplate;
        this.managementClient = managementClient;
        this.jsonMapper = jsonMapper;
        this.props = props;
    }

    // ------------------------------------------------------------------ Colas

    public List<QueueResponse> listQueues() {
        try {
            List<Map<String, Object>> queues = managementClient.get()
                    .uri("/api/queues/{vhost}?columns=name,messages,consumers,durable,arguments", props.management().vhost())
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
            return queues == null ? List.of() : queues.stream().map(RabbitAdminService::toQueueResponse).toList();
        } catch (RestClientException ex) {
            throw new BrokerUnavailableException("Management API no disponible en " + props.management().url(), ex);
        }
    }

    public QueueResponse getQueue(String name) {
        QueueInformation info = rabbitAdmin.getQueueInfo(name);
        if (info == null) {
            throw new ResourceNotFoundException("La cola '" + name + "' no existe");
        }
        return new QueueResponse(info.getName(), info.getMessageCount(), info.getConsumerCount(), null, null);
    }

    public QueueResponse createQueue(CreateQueueRequest req) {
        if (rabbitAdmin.getQueueInfo(req.name()) != null) {
            throw new ResourceConflictException("La cola '" + req.name() + "' ya existe");
        }
        if (req.deadLetterExchange() != null && !exchangeExists(req.deadLetterExchange())) {
            throw new ResourceNotFoundException("El dead letter exchange '" + req.deadLetterExchange() + "' no existe");
        }

        QueueBuilder builder = req.isDurable() ? QueueBuilder.durable(req.name()) : QueueBuilder.nonDurable(req.name());
        if (req.ttlMs() != null) {
            builder.withArgument(ARG_TTL, req.ttlMs());
        }
        if (req.maxLength() != null) {
            builder.withArgument(ARG_MAX_LENGTH, req.maxLength());
        }
        if (req.deadLetterExchange() != null) {
            builder.withArgument(ARG_DLX, req.deadLetterExchange());
        }
        if (req.deadLetterRoutingKey() != null) {
            builder.withArgument(ARG_DLK, req.deadLetterRoutingKey());
        }
        Queue queue = builder.build();

        declare(() -> rabbitAdmin.declareQueue(queue), "cola", req.name());
        log.info("[ADMIN] cola creada name={} durable={} args={}", queue.getName(), queue.isDurable(), queue.getArguments());
        return new QueueResponse(queue.getName(), 0L, 0, queue.isDurable(), queue.getArguments());
    }

    public void deleteQueue(String name) {
        requireQueue(name);
        rabbitAdmin.deleteQueue(name);
        log.info("[ADMIN] cola eliminada name={}", name);
    }

    public PurgeResponse purgeQueue(String name) {
        requireQueue(name);
        int purged = rabbitAdmin.purgeQueue(name);
        log.info("[ADMIN] cola purgada name={} mensajes={}", name, purged);
        return new PurgeResponse(name, purged);
    }

    // ------------------------------------------------------------------ Exchanges

    public ExchangeResponse createExchange(CreateExchangeRequest req) {
        if (exchangeExists(req.name())) {
            throw new ResourceConflictException("El exchange '" + req.name() + "' ya existe");
        }
        Exchange exchange = new ExchangeBuilder(req.name(), req.type().value()).durable(req.isDurable()).build();
        declare(() -> rabbitAdmin.declareExchange(exchange), "exchange", req.name());
        log.info("[ADMIN] exchange creado name={} type={} durable={}", req.name(), req.type().value(), req.isDurable());
        return new ExchangeResponse(req.name(), req.type(), req.isDurable());
    }

    public void deleteExchange(String name) {
        requireExchange(name);
        rabbitAdmin.deleteExchange(name);
        log.info("[ADMIN] exchange eliminado name={}", name);
    }

    // ------------------------------------------------------------------ Bindings

    public BindingResponse createBinding(BindingRequest req) {
        requireExchange(req.exchange());
        requireQueue(req.queue());
        rabbitAdmin.declareBinding(toBinding(req.exchange(), req.queue(), req.routingKey()));
        log.info("[ADMIN] binding creado {} --[{}]--> {}", req.exchange(), req.routingKey(), req.queue());
        return new BindingResponse(req.exchange(), req.queue(), req.routingKey());
    }

    public void deleteBinding(String exchange, String queue, String routingKey) {
        requireExchange(exchange);
        requireQueue(queue);
        rabbitAdmin.removeBinding(toBinding(exchange, queue, routingKey));
        log.info("[ADMIN] binding eliminado {} --[{}]--> {}", exchange, routingKey, queue);
    }

    // ------------------------------------------------------------------ Mensajes

    /**
     * Publica un mensaje persistente y espera el publisher confirm.
     * Si viene rawBody se envia tal cual (util para probar JSON invalido -> DLQ).
     */
    public PublishResponse publish(PublishMessageRequest req) {
        requireExchange(req.exchange());

        String eventId = req.eventId() != null ? req.eventId() : UUID.randomUUID().toString();
        String type = req.type() != null && !req.type().isBlank() ? req.type() : DEFAULT_TYPE;
        byte[] body = req.rawBody() != null
                ? req.rawBody().getBytes(StandardCharsets.UTF_8)
                : jsonMapper.writeValueAsBytes(new EventEnvelope(type, eventId, Instant.now(),
                        UUID.randomUUID().toString(), req.correlationId(), props.source(), req.payload()));

        MessageBuilderSupport<Message> builder = MessageBuilder.withBody(body)
                .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                .setDeliveryMode(MessageDeliveryMode.PERSISTENT)
                .setMessageId(eventId)
                .setType(type)
                .setAppId(props.source());
        if (req.headers() != null) {
            req.headers().forEach(builder::setHeader);
        }
        Message message = builder.build();

        CorrelationData correlation = new CorrelationData(eventId);
        rabbitTemplate.send(req.exchange(), req.routingKey(), message, correlation);
        boolean confirmed = awaitConfirm(correlation);

        if (correlation.getReturned() != null) {
            throw new UnroutableMessageException("Mensaje no enrutable: ninguna cola enlazada a '" + req.exchange()
                    + "' calza con la routing key '" + req.routingKey() + "'");
        }
        log.info("[ADMIN] mensaje publicado eventId={} exchange={} routingKey={} confirmado={} raw={}",
                eventId, req.exchange(), req.routingKey(), confirmed, req.rawBody() != null);
        return new PublishResponse(eventId, req.exchange(), req.routingKey(), confirmed);
    }

    // ------------------------------------------------------------------ helpers

    private boolean awaitConfirm(CorrelationData correlation) {
        try {
            CorrelationData.Confirm confirm = correlation.getFuture()
                    .get(props.confirmTimeout().toMillis(), TimeUnit.MILLISECONDS);
            if (!confirm.ack()) {
                throw new BrokerUnavailableException("El broker rechazo el mensaje: " + confirm.reason(), null);
            }
            return true;
        } catch (TimeoutException ex) {
            log.warn("[ADMIN] sin publisher confirm tras {} para eventId={}", props.confirmTimeout(), correlation.getId());
            return false;
        } catch (ExecutionException ex) {
            throw new BrokerUnavailableException("Error esperando confirmacion", ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new BrokerUnavailableException("Interrumpido esperando confirmacion", ex);
        }
    }

    private void requireQueue(String name) {
        if (rabbitAdmin.getQueueInfo(name) == null) {
            throw new ResourceNotFoundException("La cola '" + name + "' no existe");
        }
    }

    private void requireExchange(String name) {
        if (!exchangeExists(name)) {
            throw new ResourceNotFoundException("El exchange '" + name + "' no existe");
        }
    }

    /**
     * Declaracion pasiva: no crea nada, solo falla con 404 si el exchange no existe.
     */
    boolean exchangeExists(String name) {
        try {
            rabbitTemplate.execute(channel -> channel.exchangeDeclarePassive(name));
            return true;
        } catch (AmqpConnectException ex) {
            throw ex;
        } catch (AmqpException ex) {
            if (causeContains(ex, NOT_FOUND_CODE)) {
                return false;
            }
            throw ex;
        }
    }

    /** Si RabbitMQ responde PRECONDITION_FAILED (argumentos distintos) se informa como 409. */
    private void declare(Runnable declaration, String kind, String name) {
        try {
            declaration.run();
        } catch (AmqpConnectException ex) {
            throw ex;
        } catch (AmqpException ex) {
            if (causeContains(ex, PRECONDITION_FAILED_CODE)) {
                throw new ResourceConflictException("Conflicto al declarar " + kind + " '" + name
                        + "': ya existe con otros argumentos");
            }
            throw ex;
        }
    }

    private static boolean causeContains(Throwable ex, String text) {
        for (Throwable t = ex; t != null; t = t.getCause()) {
            if (t.getMessage() != null && t.getMessage().contains(text)) {
                return true;
            }
        }
        return false;
    }

    private static Binding toBinding(String exchange, String queue, String routingKey) {
        return new Binding(queue, Binding.DestinationType.QUEUE, exchange, routingKey, null);
    }

    @SuppressWarnings("unchecked")
    private static QueueResponse toQueueResponse(Map<String, Object> q) {
        Number messages = (Number) q.get("messages");
        Number consumers = (Number) q.get("consumers");
        return new QueueResponse(
                (String) q.get("name"),
                messages == null ? null : messages.longValue(),
                consumers == null ? null : consumers.intValue(),
                (Boolean) q.get("durable"),
                (Map<String, Object>) q.get("arguments"));
    }
}
