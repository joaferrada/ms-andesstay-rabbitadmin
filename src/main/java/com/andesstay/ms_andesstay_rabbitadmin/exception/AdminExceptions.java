package com.andesstay.ms_andesstay_rabbitadmin.exception;

/**
 * Excepciones de la API de administracion; {@link ApiExceptionHandler} las traduce a HTTP.
 */
public final class AdminExceptions {

    private AdminExceptions() {
    }

    /** 404: la cola, exchange o binding no existe. */
    public static class ResourceNotFoundException extends RuntimeException {
        public ResourceNotFoundException(String message) {
            super(message);
        }
    }

    /** 409: el recurso ya existe o choca con una declaracion previa (PRECONDITION_FAILED). */
    public static class ResourceConflictException extends RuntimeException {
        public ResourceConflictException(String message) {
            super(message);
        }
    }

    /** 422: el exchange existe pero ninguna cola calza con la routing key (publisher return). */
    public static class UnroutableMessageException extends RuntimeException {
        public UnroutableMessageException(String message) {
            super(message);
        }
    }

    /** 503: el broker o la Management API no responden. */
    public static class BrokerUnavailableException extends RuntimeException {
        public BrokerUnavailableException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
