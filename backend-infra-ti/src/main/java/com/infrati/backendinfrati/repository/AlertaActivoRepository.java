package com.infrati.backendinfrati.repository;

import com.infrati.backendinfrati.dto.AlertaActivo;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Repository
public class AlertaActivoRepository {
    private final JdbcTemplate jdbc;
    public AlertaActivoRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public List<AlertaActivo> listarActuales() {
        return jdbc.query("""
                SELECT al.*,a.hostname,a.tipo_activo,s.tipo AS tipo_servidor,a.estado_operativo,
                       c.estado_conexion,a.modelo,a.fecha_eos,
                       a.fecha_eos-(CURRENT_TIMESTAMP AT TIME ZONE 'America/Lima')::date AS dias_restantes,
                       h.valor_nuevo
                FROM alerta al JOIN activo a ON a.id=al.activo_id
                LEFT JOIN servidor s ON s.id=a.id
                LEFT JOIN monitoreo_snmp c ON c.activo_id=a.id
                LEFT JOIN LATERAL (
                    SELECT he.valor_nuevo FROM historico_estado he
                    WHERE he.activo_id=al.activo_id AND he.campo=al.campo
                      AND he.componente_tipo IS NOT DISTINCT FROM al.componente_tipo
                      AND he.componente_sn IS NOT DISTINCT FROM al.componente_sn
                    ORDER BY he.fecha_cambio DESC,he.id DESC LIMIT 1
                ) h ON true
                WHERE al.fecha_resolucion IS NULL AND a.estado_operativo<>'Baja'
                  AND NOT a.mantenimiento_activo
                  AND (al.campo<>'estado_conexion' OR c.estado_conexion='Sin conexión')
                ORDER BY CASE al.severidad WHEN 'Crítica' THEN 0 ELSE 1 END,
                         al.fecha_inicio DESC,al.id DESC
                """, (rs, n) -> {
            var inicio=rs.getObject("fecha_inicio", LocalDateTime.class).atOffset(ZoneOffset.UTC);
            var ultima=rs.getObject("ultima_verificacion", LocalDateTime.class).atOffset(ZoneOffset.UTC);
            return new AlertaActivo(rs.getLong("activo_id"),rs.getString("hostname"),rs.getString("tipo_activo"),
                    rs.getString("tipo_servidor"),rs.getString("estado_operativo"),rs.getString("estado_conexion"),
                    rs.getString("componente_tipo"),rs.getString("componente_sn"),rs.getString("campo"),
                    rs.getString("descripcion"),rs.getString("valor_nuevo"),inicio,rs.getLong("id"),
                    rs.getString("severidad"),inicio,ultima,rs.getString("modelo"),rs.getObject("fecha_eos",LocalDate.class),
                    rs.getObject("dias_restantes",Integer.class));
        });
    }
}
