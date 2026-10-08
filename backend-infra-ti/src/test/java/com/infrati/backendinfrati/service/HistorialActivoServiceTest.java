package com.infrati.backendinfrati.service;

import com.infrati.backendinfrati.model.HistoricoEstado;
import com.infrati.backendinfrati.repository.ActivoRepository;
import com.infrati.backendinfrati.repository.HistoricoEstadoRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@SpringBootTest
@Transactional
class HistorialActivoServiceTest {
    @Autowired private HistorialActivoService service;
    @Autowired private ActivoRepository activos;
    @Autowired private HistoricoEstadoRepository historico;
    @Autowired private EntityManager entityManager;

    @Test
    void listaEventosDelEquipoYComponenteConFechaUtc() {
        var activo = activos.findAll().stream().findFirst().orElse(null);
        assumeTrue(activo != null, "La BD de integración no tiene activos de prueba");
        var fechaEquipo = LocalDateTime.of(2030, 1, 1, 10, 0);
        var fechaComponente = fechaEquipo.plusMinutes(1);
        var equipo = historico.save(HistoricoEstado.builder()
                .activo(activo).campo("estado_operativo").valorNuevo("Baja")
                .descripcion("Cambio del equipo").fechaCambio(fechaEquipo).build());
        var componente = historico.save(HistoricoEstado.builder()
                .activo(activo).componenteTipo("CPU").componenteSn("TEST-CPU")
                .campo("estado").valorNuevo("Degradado")
                .descripcion("Cambio del componente").fechaCambio(fechaComponente).build());
        entityManager.flush();

        var eventos = service.listar(activo.getId());
        var actualComponente = eventos.stream().filter(e -> e.id().equals(componente.getId())).findFirst().orElseThrow();
        var actualEquipo = eventos.stream().filter(e -> e.id().equals(equipo.getId())).findFirst().orElseThrow();
        assertTrue(eventos.indexOf(actualComponente) < eventos.indexOf(actualEquipo));
        assertEquals("CPU", actualComponente.componenteTipo());
        assertEquals("TEST-CPU", actualComponente.componenteSn());
        assertEquals("estado", actualComponente.campo());
        assertEquals("Degradado", actualComponente.valorNuevo());
        assertEquals(fechaComponente.atOffset(ZoneOffset.UTC), actualComponente.fechaCambio());
        assertNull(actualEquipo.componenteTipo());
        assertNull(actualEquipo.componenteSn());
        assertEquals("Cambio del equipo", actualEquipo.descripcion());
    }

    @Test
    void activoInexistenteDevuelveNoEncontrado() {
        assertThrows(RecursoNoEncontradoException.class, () -> service.listar(Long.MAX_VALUE));
    }
}
