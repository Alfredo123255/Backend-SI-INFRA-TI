package com.infrati.backendinfrati.repository;

import com.infrati.backendinfrati.dto.AlertaActivo;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.ZoneOffset;
import java.util.List;

@Repository
public class AlertaActivoRepository {
    private final JdbcTemplate jdbc;

    public AlertaActivoRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<AlertaActivo> listarActuales() {
        return jdbc.query("""
                SELECT a.id, a.hostname, a.tipo_activo, s.tipo AS tipo_servidor,
                       a.estado_operativo, c.estado_conexion,
                       h.componente_tipo, h.componente_sn, h.campo,
                       h.descripcion, h.valor_nuevo, h.fecha_cambio
                FROM activo a
                LEFT JOIN servidor s ON s.id = a.id
                LEFT JOIN LATERAL (
                    SELECT ms.estado_conexion
                    FROM monitoreo_snmp ms
                    WHERE ms.activo_id = a.id
                    ORDER BY ms.id DESC
                    LIMIT 1
                ) c ON true
                LEFT JOIN LATERAL (
                    SELECT he.componente_tipo, he.componente_sn, he.campo,
                           he.descripcion, he.valor_nuevo, he.fecha_cambio
                    FROM historico_estado he
                    WHERE he.activo_id = a.id
                    ORDER BY he.fecha_cambio DESC NULLS LAST, he.id DESC
                    LIMIT 1
                ) h ON true
                WHERE a.estado_operativo = 'Apagado'
                   OR c.estado_conexion = 'Sin conexión'
                ORDER BY h.fecha_cambio DESC NULLS LAST, a.id
                """, (rs, rowNum) -> {
            Timestamp fecha = rs.getTimestamp("fecha_cambio");
            return new AlertaActivo(rs.getLong("id"), rs.getString("hostname"),
                    rs.getString("tipo_activo"), rs.getString("tipo_servidor"),
                    rs.getString("estado_operativo"), rs.getString("estado_conexion"),
                    rs.getString("componente_tipo"), rs.getString("componente_sn"),
                    rs.getString("campo"), rs.getString("descripcion"),
                    rs.getString("valor_nuevo"),
                    fecha == null ? null : fecha.toLocalDateTime().atOffset(ZoneOffset.UTC));
        });
    }
}
