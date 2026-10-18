package com.andesstay.ms_andesstay_rabbitadmin.controller;

import com.andesstay.ms_andesstay_rabbitadmin.dto.AdminResponses.ExchangeResponse;
import com.andesstay.ms_andesstay_rabbitadmin.dto.CreateExchangeRequest;
import com.andesstay.ms_andesstay_rabbitadmin.service.RabbitAdminService;
import com.andesstay.ms_andesstay_rabbitadmin.validation.AmqpName;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

/**
 * Exchanges: crear (201/400/409) y eliminar (204/404).
 */
@RestController
@RequestMapping("/api/admin/exchanges")
public class ExchangeAdminController {

    private final RabbitAdminService adminService;

    public ExchangeAdminController(RabbitAdminService adminService) {
        this.adminService = adminService;
    }

    @PostMapping
    public ResponseEntity<ExchangeResponse> crear(@Valid @RequestBody CreateExchangeRequest request) {
        ExchangeResponse created = adminService.createExchange(request);
        return ResponseEntity.created(URI.create("/api/admin/exchanges/" + created.name())).body(created);
    }

    @DeleteMapping("/{name}")
    public ResponseEntity<Void> eliminar(@PathVariable @AmqpName String name) {
        adminService.deleteExchange(name);
        return ResponseEntity.noContent().build();
    }
}
