package com.andesstay.ms_andesstay_rabbitadmin.config;

import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RabbitConfig {

    /**
     * RabbitAdmin explicito (Spring Boot deja de crear el suyo). Se usa solo desde RabbitAdminService.
     */
    @Bean
    public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
        return new RabbitAdmin(connectionFactory);
    }

    /** Cliente para la API HTTP de Management (listar colas con sus contadores). */
    @Bean
    public RestClient managementRestClient(RabbitAdminProperties props) {
        RabbitAdminProperties.Management m = props.management();
        return RestClient.builder()
                .baseUrl(m.url())
                .defaultHeaders(h -> h.setBasicAuth(m.username(), m.password()))
                .build();
    }
}
