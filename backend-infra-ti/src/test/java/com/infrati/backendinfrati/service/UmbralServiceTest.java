package com.infrati.backendinfrati.service;

import com.infrati.backendinfrati.dto.ActualizarUmbralRequest;
import com.infrati.backendinfrati.exception.SolicitudInvalidaException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class UmbralServiceTest {
    @Autowired UmbralService service;
    @Autowired JdbcTemplate jdbc;

    @Test
    void permiteIgualdadYQuitarUmbral() {
        assertTrue(jdbc.queryForObject("SELECT current_database()", String.class).contains("test"));
        var limite = BigDecimal.valueOf(85);
        service.actualizar("cpu_uso_ghz", new ActualizarUmbralRequest(limite, limite));
        var guardado = service.listar().stream().filter(u -> u.nombre().equals("cpu_uso_ghz")).findFirst().orElseThrow();
        assertEquals(0, limite.compareTo(guardado.advertencia()));
        assertEquals(0, limite.compareTo(guardado.critico()));
        assertNotNull(guardado.fechaModificacion());

        var sinUmbral = service.actualizar("cpu_uso_ghz", new ActualizarUmbralRequest(null, null));
        assertNull(sinUmbral.advertencia());
        assertNull(sinUmbral.critico());
    }

    @Test
    void validaLimitesYNombre() {
        assertThrows(SolicitudInvalidaException.class, () -> service.actualizar("cpu_uso_ghz",
                new ActualizarUmbralRequest(BigDecimal.valueOf(80), BigDecimal.valueOf(79))));
        assertThrows(RecursoNoEncontradoException.class, () -> service.actualizar("inexistente",
                new ActualizarUmbralRequest(null, null)));
    }
}
