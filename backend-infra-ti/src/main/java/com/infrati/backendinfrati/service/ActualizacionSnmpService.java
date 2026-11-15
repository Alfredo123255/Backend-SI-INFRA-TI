package com.infrati.backendinfrati.service;

import com.infrati.backendinfrati.repository.NotificacionEtlRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** PostgreSQL entrega el aviso después del commit de las nuevas credenciales. */
@Service
public class ActualizacionSnmpService {
    private final NotificacionEtlRepository notificaciones;

    public ActualizacionSnmpService(NotificacionEtlRepository notificaciones) {
        this.notificaciones = notificaciones;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void solicitar(Long conexionId) {
        notificaciones.notificarActualizacionConexionConCallback(conexionId);
    }
}
