package com.andesstay.ms_andesstay_rabbitadmin.controller;

import com.andesstay.ms_andesstay_rabbitadmin.dto.AdminResponses.BindingResponse;
import com.andesstay.ms_andesstay_rabbitadmin.dto.BindingRequest;
import com.andesstay.ms_andesstay_rabbitadmin.service.RabbitAdminService;
import com.andesstay.ms_andesstay_rabbitadmin.validation.AmqpName;
import com.andesstay.ms_andesstay_rabbitadmin.validation.RoutingKey;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Bindings: crear (201/400/404) y eliminar (204/400/404).
 * DELETE usa query params (?exchange=&amp;queue=&amp;routingKey=) porque un binding no tiene id propio.
 */
@RestController
@RequestMapping("/api/admin/bindings")
public class BindingAdminController {

    private final RabbitAdminService adminService;

    public BindingAdminController(RabbitAdminService adminService) {
        this.adminService = adminService;
    }

    @PostMapping
    public ResponseEntity<BindingResponse> crear(@Valid @RequestBody BindingRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminService.createBinding(request));
    }

    @DeleteMapping
    public ResponseEntity<Void> eliminar(@RequestParam @AmqpName String exchange,
                                         @RequestParam @AmqpName String queue,
                                         @RequestParam(defaultValue = "") @RoutingKey String routingKey) {
        adminService.deleteBinding(exchange, queue, routingKey);
        return ResponseEntity.noContent().build();
    }
}
