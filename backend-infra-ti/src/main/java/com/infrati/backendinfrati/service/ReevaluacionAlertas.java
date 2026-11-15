package com.infrati.backendinfrati.service;

import com.infrati.backendinfrati.repository.NotificacionEtlRepository;
import org.springframework.stereotype.Service;

/** Los avisos se entregan al commit; separa reevaluación y lectura SNMP nueva. */
@Service
public class ReevaluacionAlertas {
    private final NotificacionEtlRepository notificaciones;
    public ReevaluacionAlertas(NotificacionEtlRepository notificaciones) { this.notificaciones=notificaciones; }
    public void solicitar(Long activoId) {
        notificaciones.notificarReevaluacionAlertas(activoId);
    }
    public void solicitarLectura(Long conexionId) {
        notificaciones.notificarActualizacionConexion(conexionId);
    }
}
