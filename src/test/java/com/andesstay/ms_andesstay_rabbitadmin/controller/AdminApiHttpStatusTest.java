package com.andesstay.ms_andesstay_rabbitadmin.controller;

import com.andesstay.ms_andesstay_rabbitadmin.dto.AdminResponses.QueueResponse;
import com.andesstay.ms_andesstay_rabbitadmin.exception.AdminExceptions.ResourceConflictException;
import com.andesstay.ms_andesstay_rabbitadmin.exception.AdminExceptions.ResourceNotFoundException;
import com.andesstay.ms_andesstay_rabbitadmin.exception.AdminExceptions.UnroutableMessageException;
import com.andesstay.ms_andesstay_rabbitadmin.service.RabbitAdminService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "andesstay.security.enabled=false")
@AutoConfigureMockMvc
class AdminApiHttpStatusTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    RabbitAdminService adminService;

    @Test
    void crearColaValidaDevuelve201ConLocation() throws Exception {
        when(adminService.createQueue(any())).thenReturn(new QueueResponse("q.demo", 0L, 0, true, null));

        mvc.perform(post("/api/admin/queues").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"q.demo\",\"ttlMs\":60000}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/admin/queues/q.demo"));
    }

    @Test
    void nombreConPrefijoAmqDevuelve400ConDetalle() throws Exception {
        mvc.perform(post("/api/admin/queues").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"amq.hack\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[*].message", hasItem("el prefijo 'amq.' esta reservado por RabbitMQ")));
    }

    @Test
    void colaExistenteDevuelve409() throws Exception {
        when(adminService.createQueue(any())).thenThrow(new ResourceConflictException("La cola 'q.demo' ya existe"));

        mvc.perform(post("/api/admin/queues").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"q.demo\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("La cola 'q.demo' ya existe"));
    }

    @Test
    void eliminarColaInexistenteDevuelve404() throws Exception {
        doThrow(new ResourceNotFoundException("La cola 'q.nada' no existe")).when(adminService).deleteQueue("q.nada");

        mvc.perform(delete("/api/admin/queues/q.nada")).andExpect(status().isNotFound());
    }

    @Test
    void eliminarColaExistenteDevuelve204() throws Exception {
        mvc.perform(delete("/api/admin/queues/q.demo")).andExpect(status().isNoContent());
    }

    @Test
    void pathVariableConPrefijoAmqDevuelve400() throws Exception {
        mvc.perform(delete("/api/admin/exchanges/amq.direct")).andExpect(status().isBadRequest());
    }

    @Test
    void tipoDeExchangeInvalidoDevuelve400() throws Exception {
        mvc.perform(post("/api/admin/exchanges").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"cmd.demo\",\"type\":\"x-delayed\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("direct, topic, fanout, headers")));
    }

    @Test
    void eliminarBindingSinQueueDevuelve400() throws Exception {
        mvc.perform(delete("/api/admin/bindings").param("exchange", "cmd.direct"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void mensajeNoEnrutableDevuelve422() throws Exception {
        when(adminService.publish(any())).thenThrow(new UnroutableMessageException("Mensaje no enrutable"));

        mvc.perform(post("/api/admin/messages").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"exchange\":\"cmd.direct\",\"routingKey\":\"nada\",\"payload\":{}}"))
                .andExpect(status().isUnprocessableContent());
    }
}
