package com.andesstay.ms_andesstay_rabbitadmin.controller;

import com.andesstay.ms_andesstay_rabbitadmin.dto.AdminResponses.PublishResponse;
import com.andesstay.ms_andesstay_rabbitadmin.dto.PublishMessageRequest;
import com.andesstay.ms_andesstay_rabbitadmin.service.RabbitAdminService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Publicacion de mensajes de prueba: 202 (aceptado por el broker; el procesamiento es asincrono),
 * 400 (validacion), 404 (exchange no existe), 422 (ninguna cola calza con la routing key).
 */
@RestController
@RequestMapping("/api/admin/messages")
public class MessageAdminController {

    private final RabbitAdminService adminService;

    public MessageAdminController(RabbitAdminService adminService) {
        this.adminService = adminService;
    }

    @PostMapping
    public ResponseEntity<PublishResponse> publicar(@Valid @RequestBody PublishMessageRequest request) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(adminService.publish(request));
    }
}
