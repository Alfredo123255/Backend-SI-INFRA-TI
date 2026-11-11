package com.infrati.backendinfrati.service;

import com.infrati.backendinfrati.model.Activos.Activo;
import com.infrati.backendinfrati.model.HistoricoEstado;
import com.infrati.backendinfrati.repository.HistoricoEstadoRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

/** Termina un episodio por una acción administrativa, sin declarar recuperación. */
@Service
public class CierreAdministrativoAlertasService {
    private final JdbcTemplate jdbc;
    private final HistoricoEstadoRepository historico;

    public CierreAdministrativoAlertasService(JdbcTemplate jdbc, HistoricoEstadoRepository historico) {
        this.jdbc = jdbc;
        this.historico = historico;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public int cerrar(Activo activo, boolean porBaja, LocalDateTime fecha) {
        String motivo = porBaja ? "Cerrada por baja del activo" : "Cerrada por inicio de mantenimiento";
        String valor = porBaja ? "Cerrada por baja" : "Cerrada por mantenimiento";
        // Conserva la condición original y la última verificación real por SNMP.
        var cerradas = jdbc.query("""
                UPDATE alerta SET fecha_resolucion=GREATEST(fecha_inicio,?),
                    descripcion=descripcion || E'\\n' || ?
                WHERE activo_id=? AND fecha_resolucion IS NULL
                RETURNING id,campo,componente_tipo,componente_sn,descripcion,fecha_resolucion
                """, (rs, fila) -> new Cierre(rs.getInt("id"), rs.getString("campo"),
                        rs.getString("componente_tipo"), rs.getString("componente_sn"),
                        rs.getString("descripcion"), rs.getTimestamp("fecha_resolucion").toLocalDateTime()),
                fecha, motivo, activo.getId());
        for (var cierre : cerradas) {
            historico.save(HistoricoEstado.builder().activo(activo)
                    .componenteTipo(cierre.tipo()).componenteSn(cierre.serial())
                    .campo("cierre_alerta_" + cierre.campo()).valorNuevo(valor)
                    .descripcion("Alerta #" + cierre.id() + ". " + cierre.descripcion())
                    .fechaCambio(cierre.fecha()).build());
        }
        return cerradas.size();
    }

    private record Cierre(int id, String campo, String tipo, String serial,
                          String descripcion, LocalDateTime fecha) {}
}
