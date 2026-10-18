package com.infrati.backendinfrati.service;

import com.infrati.backendinfrati.dto.CambiarMantenimientoRequest;
import com.infrati.backendinfrati.exception.SolicitudInvalidaException;
import com.infrati.backendinfrati.repository.ActivoRepository;
import com.infrati.backendinfrati.repository.HistoricoEstadoRepository;
import com.infrati.backendinfrati.repository.MonitoreoSnmpRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class MantenimientoActivoServiceUnitTest {
    private final ActivoRepository activos = mock(ActivoRepository.class);
    private final HistoricoEstadoRepository historico = mock(HistoricoEstadoRepository.class);
    private final MonitoreoSnmpRepository conexiones = mock(MonitoreoSnmpRepository.class);
    private final EntityManager entityManager = mock(EntityManager.class);
    private final MantenimientoActivoService servicio = new MantenimientoActivoService(
            activos, historico, conexiones, entityManager);

    @Test
    void iniciarSinResponsableOMotivoNoEscribeNiCambiaElActivo() {
        for (String vacio : new String[]{null, "", "   "}) {
            assertThrows(SolicitudInvalidaException.class, () -> servicio.cambiar(1L,
                    new CambiarMantenimientoRequest(true, vacio, "Revisión")));
            assertThrows(SolicitudInvalidaException.class, () -> servicio.cambiar(1L,
                    new CambiarMantenimientoRequest(true, "Ana", vacio)));
        }
        verifyNoInteractions(activos, historico, conexiones, entityManager);
    }
}
