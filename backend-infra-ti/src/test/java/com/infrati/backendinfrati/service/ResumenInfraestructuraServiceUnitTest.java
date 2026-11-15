package com.infrati.backendinfrati.service;

import com.infrati.backendinfrati.repository.ResumenInfraestructuraRepository;
import com.infrati.backendinfrati.repository.ResumenInfraestructuraRepository.*;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ResumenInfraestructuraServiceUnitTest {
    @Test
    void cargaLaInstantaneaDesdeRepositoriosYConservaLosCalculos() {
        var repository = mock(ResumenInfraestructuraRepository.class);
        var alertas = mock(AlertaActivoService.class);
        var fecha = LocalDateTime.of(2026, 1, 2, 12, 0);
        when(repository.listarActivos()).thenReturn(List.of(new Activo(
                1L, "equipo", "DC", "CL", "rackeable", "Encendido", false, "Activo",
                fecha, "modelo", null, null, BigDecimal.valueOf(40), BigDecimal.valueOf(100),
                BigDecimal.valueOf(20), BigDecimal.valueOf(100), null, null, BigDecimal.valueOf(25))));
        when(repository.listarDatacenters()).thenReturn(List.of(new Centro("DC", null, null)));
        when(repository.listarClusters()).thenReturn(List.of(new Cluster("CL", "DC")));
        when(repository.advertenciasUmbral()).thenReturn(Map.of("cpu_uso_ghz", BigDecimal.valueOf(80)));
        when(alertas.listar()).thenReturn(List.of());

        var resultado = new ResumenInfraestructuraService(repository, alertas).inicio();

        assertEquals(1, resultado.total());
        assertEquals(0, resultado.disponibilidad().compareTo(BigDecimal.valueOf(100)));
        assertEquals(0, resultado.capacidad().getFirst().disponible().compareTo(BigDecimal.valueOf(60)));
        assertEquals(0, resultado.capacidad().getFirst().meta().compareTo(BigDecimal.valueOf(20)));
        var orden = inOrder(repository, alertas);
        orden.verify(repository).listarActivos();
        orden.verify(repository).listarDatacenters();
        orden.verify(repository).listarClusters();
        orden.verify(repository).advertenciasUmbral();
        orden.verify(alertas).listar();
        verifyNoMoreInteractions(repository, alertas);
    }
}
