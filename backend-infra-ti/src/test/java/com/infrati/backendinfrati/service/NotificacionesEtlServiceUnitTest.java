package com.infrati.backendinfrati.service;

import com.infrati.backendinfrati.repository.NotificacionEtlRepository;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;

class NotificacionesEtlServiceUnitTest {
    @Test
    void conservaLasTresEntradasPublicasYLaReevaluacionGlobal() {
        var repository = mock(NotificacionEtlRepository.class);
        var reevaluacion = new ReevaluacionAlertas(repository);
        var actualizacion = new ActualizacionSnmpService(repository);

        reevaluacion.solicitar(null);
        reevaluacion.solicitar(7L);
        reevaluacion.solicitarLectura(8L);
        actualizacion.solicitar(9L);

        verify(repository).notificarReevaluacionAlertas(null);
        verify(repository).notificarReevaluacionAlertas(7L);
        verify(repository).notificarActualizacionConexion(8L);
        verify(repository).notificarActualizacionConexionConCallback(9L);
        verifyNoMoreInteractions(repository);
    }
}
