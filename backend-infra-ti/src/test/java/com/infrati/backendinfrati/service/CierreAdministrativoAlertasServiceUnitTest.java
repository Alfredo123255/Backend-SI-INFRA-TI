package com.infrati.backendinfrati.service;

import com.infrati.backendinfrati.model.Activos.Servidor;
import com.infrati.backendinfrati.model.HistoricoEstado;
import com.infrati.backendinfrati.repository.AlertaActivoRepository;
import com.infrati.backendinfrati.repository.AlertaActivoRepository.Cierre;
import com.infrati.backendinfrati.repository.HistoricoEstadoRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import java.time.LocalDateTime;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CierreAdministrativoAlertasServiceUnitTest {
    @Test
    void conservaLosEventosPorComponenteYElConteoAlCerrarPorMantenimientoOBaja() {
        for (boolean baja : new boolean[]{false, true}) {
            var alertas = mock(AlertaActivoRepository.class);
            var historico = mock(HistoricoEstadoRepository.class);
            var activo = new Servidor();
            activo.setId(8L);
            var fecha = LocalDateTime.of(2026, 1, 2, 12, 0);
            var motivo = baja ? "Cerrada por baja del activo" : "Cerrada por inicio de mantenimiento";
            var valor = baja ? "Cerrada por baja" : "Cerrada por mantenimiento";
            when(alertas.cerrarAbiertasPorActivo(8L, fecha, motivo)).thenReturn(List.of(
                    new Cierre(1, "estado", "disco", "D1", "Condición original", fecha),
                    new Cierre(2, "estado_operativo", null, null, "Equipo", fecha)));

            assertEquals(2, new CierreAdministrativoAlertasService(alertas, historico).cerrar(activo, baja, fecha));

            var eventos = ArgumentCaptor.forClass(HistoricoEstado.class);
            var orden = inOrder(alertas, historico);
            orden.verify(alertas).cerrarAbiertasPorActivo(8L, fecha, motivo);
            orden.verify(historico, times(2)).save(eventos.capture());
            var disco = eventos.getAllValues().getFirst();
            assertSame(activo, disco.getActivo());
            assertEquals("disco", disco.getComponenteTipo());
            assertEquals("D1", disco.getComponenteSn());
            assertEquals("cierre_alerta_estado", disco.getCampo());
            assertEquals(valor, disco.getValorNuevo());
            assertEquals("Alerta #1. Condición original", disco.getDescripcion());
            assertEquals(fecha, disco.getFechaCambio());
            assertNull(eventos.getAllValues().getLast().getComponenteTipo());
            verifyNoMoreInteractions(alertas, historico);
        }
    }

    @Test
    void sinAlertasAbiertasNoRegistraEventos() {
        var alertas = mock(AlertaActivoRepository.class);
        var historico = mock(HistoricoEstadoRepository.class);
        var activo = new Servidor();
        activo.setId(8L);
        var fecha = LocalDateTime.of(2026, 1, 2, 12, 0);
        when(alertas.cerrarAbiertasPorActivo(8L, fecha, "Cerrada por inicio de mantenimiento")).thenReturn(List.of());

        assertEquals(0, new CierreAdministrativoAlertasService(alertas, historico).cerrar(activo, false, fecha));
        verifyNoInteractions(historico);
    }
}
