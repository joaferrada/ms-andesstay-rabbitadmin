package com.andesstay.ms_andesstay_rabbitadmin.exception;

import com.andesstay.ms_andesstay_rabbitadmin.exception.AdminExceptions.BrokerUnavailableException;
import com.andesstay.ms_andesstay_rabbitadmin.exception.AdminExceptions.ResourceConflictException;
import com.andesstay.ms_andesstay_rabbitadmin.exception.AdminExceptions.ResourceNotFoundException;
import com.andesstay.ms_andesstay_rabbitadmin.exception.AdminExceptions.UnroutableMessageException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.AmqpException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Errores en JSON uniforme:
 * <pre>
 * {"timestamp":"...","status":400,"error":"Bad Request","message":"Validacion fallida",
 *  "path":"/api/admin/queues","details":[{"field":"name","message":"el prefijo 'amq.' esta reservado por RabbitMQ"}]}
 * </pre>
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    public record FieldErrorDetail(String field, String message) {
    }

    public record ApiError(String timestamp, int status, String error, String message, String path,
                           List<FieldErrorDetail> details) {
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> bodyInvalido(MethodArgumentNotValidException ex, HttpServletRequest req) {
        List<FieldErrorDetail> details = new ArrayList<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(e -> details.add(new FieldErrorDetail(e.getField(), e.getDefaultMessage())));
        ex.getBindingResult().getGlobalErrors()
                .forEach(e -> details.add(new FieldErrorDetail(e.getObjectName(), e.getDefaultMessage())));
        return build(HttpStatus.BAD_REQUEST, "Validacion fallida", req, details);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ApiError> parametroInvalido(HandlerMethodValidationException ex, HttpServletRequest req) {
        List<FieldErrorDetail> details = new ArrayList<>();
        ex.getParameterValidationResults().forEach(r -> r.getResolvableErrors().forEach(e ->
                details.add(new FieldErrorDetail(r.getMethodParameter().getParameterName(), e.getDefaultMessage()))));
        return build(HttpStatus.BAD_REQUEST, "Validacion fallida", req, details);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiError> parametroFaltante(MissingServletRequestParameterException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, "Falta el parametro '" + ex.getParameterName() + "'", req, List.of());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> jsonInvalido(HttpMessageNotReadableException ex, HttpServletRequest req) {
        Throwable root = ex;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        String message = root instanceof IllegalArgumentException
                ? root.getMessage()
                : "Body JSON mal formado o con tipos de dato invalidos";
        return build(HttpStatus.BAD_REQUEST, message, req, List.of());
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> noEncontrado(ResourceNotFoundException ex, HttpServletRequest req) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), req, List.of());
    }

    @ExceptionHandler(ResourceConflictException.class)
    public ResponseEntity<ApiError> conflicto(ResourceConflictException ex, HttpServletRequest req) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), req, List.of());
    }

    @ExceptionHandler(UnroutableMessageException.class)
    public ResponseEntity<ApiError> noEnrutable(UnroutableMessageException ex, HttpServletRequest req) {
        return build(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(), req, List.of());
    }

    @ExceptionHandler({BrokerUnavailableException.class, AmqpConnectException.class})
    public ResponseEntity<ApiError> brokerCaido(Exception ex, HttpServletRequest req) {
        log.warn("Broker no disponible: {}", ex.getMessage());
        return build(HttpStatus.SERVICE_UNAVAILABLE, "RabbitMQ no disponible: " + ex.getMessage(), req, List.of());
    }

    @ExceptionHandler(AmqpException.class)
    public ResponseEntity<ApiError> errorAmqp(AmqpException ex, HttpServletRequest req) {
        log.error("Error AMQP", ex);
        return build(HttpStatus.BAD_GATEWAY, "Error de RabbitMQ: " + ex.getMessage(), req, List.of());
    }

    private ResponseEntity<ApiError> build(HttpStatus status, String message, HttpServletRequest req,
                                           List<FieldErrorDetail> details) {
        ApiError body = new ApiError(Instant.now().toString(), status.value(), status.getReasonPhrase(),
                message, req.getRequestURI(), details);
        return ResponseEntity.status(status).body(body);
    }
}
