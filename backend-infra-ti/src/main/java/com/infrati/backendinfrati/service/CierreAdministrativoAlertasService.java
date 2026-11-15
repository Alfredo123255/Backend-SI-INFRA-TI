package com.infrati.backendinfrati.service;

import com.infrati.backendinfrati.model.Activos.Activo;
import com.infrati.backendinfrati.model.HistoricoEstado;
import com.infrati.backendinfrati.repository.HistoricoEstadoRepository;
import com.infrati.backendinfrati.repository.AlertaActivoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

/** Termina un episodio por una acción administrativa, sin declarar recuperación. */
@Service
public class CierreAdministrativoAlertasService {
    private final AlertaActivoRepository alertas;
    private final HistoricoEstadoRepository historico;

    public CierreAdministrativoAlertasService(AlertaActivoRepository alertas, HistoricoEstadoRepository historico) {
        this.alertas = alertas;
        this.historico = historico;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public int cerrar(Activo activo, boolean porBaja, LocalDateTime fecha) {
        String motivo = porBaja ? "Cerrada por baja del activo" : "Cerrada por inicio de mantenimiento";
        String valor = porBaja ? "Cerrada por baja" : "Cerrada por mantenimiento";
        // Conserva la condición original y la última verificación real por SNMP.
        var cerradas = alertas.cerrarAbiertasPorActivo(activo.getId(), fecha, motivo);
        for (var cierre : cerradas) {
            historico.save(HistoricoEstado.builder().activo(activo)
                    .componenteTipo(cierre.tipo()).componenteSn(cierre.serial())
                    .campo("cierre_alerta_" + cierre.campo()).valorNuevo(valor)
                    .descripcion("Alerta #" + cierre.id() + ". " + cierre.descripcion())
                    .fechaCambio(cierre.fecha()).build());
        }
        return cerradas.size();
    }

}
