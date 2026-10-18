package com.andesstay.ms_andesstay_rabbitadmin.controller;

import com.andesstay.ms_andesstay_rabbitadmin.dto.AdminResponses.PurgeResponse;
import com.andesstay.ms_andesstay_rabbitadmin.dto.AdminResponses.QueueResponse;
import com.andesstay.ms_andesstay_rabbitadmin.dto.CreateQueueRequest;
import com.andesstay.ms_andesstay_rabbitadmin.service.RabbitAdminService;
import com.andesstay.ms_andesstay_rabbitadmin.validation.AmqpName;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

/**
 * Colas: listar (200), consultar (200/404), crear (201/400/404/409), eliminar (204/404), purgar (200/404).
 */
@RestController
@RequestMapping("/api/admin/queues")
public class QueueAdminController {

    private final RabbitAdminService adminService;

    public QueueAdminController(RabbitAdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping
    public List<QueueResponse> listar() {
        return adminService.listQueues();
    }

    @GetMapping("/{name}")
    public QueueResponse obtener(@PathVariable @AmqpName String name) {
        return adminService.getQueue(name);
    }

    @PostMapping
    public ResponseEntity<QueueResponse> crear(@Valid @RequestBody CreateQueueRequest request) {
        QueueResponse created = adminService.createQueue(request);
        return ResponseEntity.created(URI.create("/api/admin/queues/" + created.name())).body(created);
    }

    @DeleteMapping("/{name}")
    public ResponseEntity<Void> eliminar(@PathVariable @AmqpName String name) {
        adminService.deleteQueue(name);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{name}/messages")
    public PurgeResponse purgar(@PathVariable @AmqpName String name) {
        return adminService.purgeQueue(name);
    }
}
