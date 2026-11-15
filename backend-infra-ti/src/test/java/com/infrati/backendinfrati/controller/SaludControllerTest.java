package com.infrati.backendinfrati.controller;

import com.infrati.backendinfrati.config.SeguridadConfig;
import com.infrati.backendinfrati.security.UsuarioDetallesService;
import com.infrati.backendinfrati.service.AccesoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = SaludController.class,
    properties = "app.cors.origenes-front=http://localhost:5173")
@Import({SeguridadConfig.class, SaludControllerTest.SeguridadPrueba.class})
class SaludControllerTest {
    @TestConfiguration(proxyBeanMethods = false)
    @EnableWebSecurity
    static class SeguridadPrueba { }

    @Autowired MockMvc mvc;
    @MockitoBean UsuarioDetallesService detalles;
    @MockitoBean AccesoService acceso;

    @Test void saludRespondeSinSesionNiCredenciales() throws Exception {
        mvc.perform(get("/healthz"))
            .andExpect(status().isOk())
            .andExpect(content().json("{\"status\":\"ok\"}"))
            .andExpect(cookie().doesNotExist("JSESSIONID"));
    }

    @Test void inventarioConservaAutenticacion() throws Exception {
        for (String ruta : new String[]{"/api/datacenters", "/api/servidores", "/api/usuarios"}) {
            mvc.perform(get(ruta))
                .andExpect(status().isUnauthorized())
                .andExpect(header().doesNotExist("Location"))
                .andExpect(jsonPath("$.message").value("Inicia sesión para continuar."));
        }
    }

    @Test void permisoPublicoNoIncluyeSubrutas() throws Exception {
        mvc.perform(get("/healthz/interno")).andExpect(status().isUnauthorized());
    }

    @Test void permisoPublicoNoIncluyeEscrituras() throws Exception {
        mvc.perform(post("/healthz").with(csrf())).andExpect(status().isUnauthorized());
    }
}
