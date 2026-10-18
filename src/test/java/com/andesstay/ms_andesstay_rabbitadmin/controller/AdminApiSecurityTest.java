package com.andesstay.ms_andesstay_rabbitadmin.controller;

import com.andesstay.ms_andesstay_rabbitadmin.service.RabbitAdminService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Con la seguridad activa (por defecto) solo el rol Admin de Entra ID puede administrar.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AdminApiSecurityTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    RabbitAdminService adminService;

    @Test
    void sinTokenDevuelve401() throws Exception {
        mvc.perform(get("/api/admin/queues")).andExpect(status().isUnauthorized());
    }

    @Test
    void rolDistintoDeAdminDevuelve403() throws Exception {
        mvc.perform(get("/api/admin/queues").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_Cliente_Dominio"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void rolAdminDevuelve200() throws Exception {
        when(adminService.listQueues()).thenReturn(List.of());
        mvc.perform(get("/api/admin/queues").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_Admin"))))
                .andExpect(status().isOk());
    }
}
