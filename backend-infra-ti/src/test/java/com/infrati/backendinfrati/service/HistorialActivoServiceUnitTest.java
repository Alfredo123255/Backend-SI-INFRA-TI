package com.infrati.backendinfrati.service;

import com.infrati.backendinfrati.model.HistoricoEstado;
import com.infrati.backendinfrati.repository.ActivoRepository;
import com.infrati.backendinfrati.repository.HistoricoEstadoRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HistorialActivoServiceUnitTest {
    private final ActivoRepository activos = mock(ActivoRepository.class);
    private final HistoricoEstadoRepository historico = mock(HistoricoEstadoRepository.class);
    private final HistorialActivoService service = new HistorialActivoService(activos, historico);

    @Test
    void muestraElCambioDelComponenteYElDelEquipoConSuFecha() {
        var fechaEquipo = LocalDateTime.of(2026, 10, 9, 12, 0);
        var fechaComponente = fechaEquipo.plusMinutes(1);
        var componente = HistoricoEstado.builder().id(2L)
                .componenteTipo("cpu").componenteSn("CPU-1")
                .campo("estado").valorNuevo("Degradado")
                .descripcion("cpu CPU-1: Encendido → Degradado")
                .fechaCambio(fechaComponente).build();
        var equipo = HistoricoEstado.builder().id(1L)
                .campo("estado_operativo").valorNuevo("Encendido")
                .descripcion("Estado inicial al registrar el activo")
                .fechaCambio(fechaEquipo).build();
        when(activos.existsById(19L)).thenReturn(true);
        when(historico.findByActivo_IdOrderByFechaCambioDescIdDesc(19L))
                .thenReturn(List.of(componente, equipo));

        var eventos = service.listar(19L);
        assertEquals(2, eventos.size());
        assertEquals("cpu", eventos.get(0).componenteTipo());
        assertEquals("CPU-1", eventos.get(0).componenteSn());
        assertEquals("estado", eventos.get(0).campo());
        assertEquals("Degradado", eventos.get(0).valorNuevo());
        assertEquals("cpu CPU-1: Encendido → Degradado", eventos.get(0).descripcion());
        assertEquals(fechaComponente.atOffset(ZoneOffset.UTC), eventos.get(0).fechaCambio());
        assertNull(eventos.get(1).componenteTipo());
        assertNull(eventos.get(1).componenteSn());
        assertEquals("estado_operativo", eventos.get(1).campo());
    }

    @Test
    void noDevuelveHistorialParaUnActivoInexistente() {
        assertThrows(RecursoNoEncontradoException.class, () -> service.listar(999L));
    }
}
