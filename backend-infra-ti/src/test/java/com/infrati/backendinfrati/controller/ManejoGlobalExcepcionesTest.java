package com.infrati.backendinfrati.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(ManejoGlobalExcepcionesTest.ControladorErrorPrueba.class)
class ManejoGlobalExcepcionesTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void recursoInexistenteDevuelve404SinDetallesInternos() throws Exception {
        mockMvc.perform(get("/api/servidores/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Servidor 999999 no encontrado"))
                .andExpect(jsonPath("$.path").value("/api/servidores/999999"))
                .andExpect(jsonPath("$.trace").doesNotExist())
                .andExpect(jsonPath("$.exception").doesNotExist());
    }

    @Test
    void identificadorMalFormadoDevuelve400SinDetallesInternos() throws Exception {
        mockMvc.perform(get("/api/servidores/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("El formato del parametro es invalido"))
                .andExpect(jsonPath("$.path").value("/api/servidores/abc"))
                .andExpect(jsonPath("$.trace").doesNotExist())
                .andExpect(jsonPath("$.exception").doesNotExist());
    }

    @Test
    void endpointInexistenteDevuelve404SinDetallesInternos() throws Exception {
        mockMvc.perform(get("/api/endpoint-inexistente"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Endpoint no encontrado"))
                .andExpect(jsonPath("$.path").value("/api/endpoint-inexistente"))
                .andExpect(jsonPath("$.trace").doesNotExist())
                .andExpect(jsonPath("$.exception").doesNotExist());
    }

    @Test
    void errorInternoDevuelve500GenericoSinInformacionSensible() throws Exception {
        mockMvc.perform(get("/api/prueba/error-interno"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.message").value("Ocurrio un error interno inesperado"))
                .andExpect(jsonPath("$.message", not(containsString("RuntimeException"))))
                .andExpect(jsonPath("$.trace").doesNotExist())
                .andExpect(jsonPath("$.exception").doesNotExist());
    }

    @RestController
    static class ControladorErrorPrueba {

        @GetMapping("/api/prueba/error-interno")
        void errorInterno() {
            throw new RuntimeException("detalle sensible que no debe salir en la respuesta");
        }
    }
}
