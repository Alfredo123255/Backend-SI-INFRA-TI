package com.infrati.backendinfrati.repository;

import com.infrati.backendinfrati.dto.AlertaActivo;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Repository
public class EvaluacionUmbralRepository {
    private static final long BLOQUEO_EVALUACION = 68342118L;
    private final JdbcTemplate jdbc;

    public EvaluacionUmbralRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<AlertaActivo> evaluarYRegistrar() {
        // Serializa las evaluaciones para que consultas concurrentes no dupliquen eventos.
        jdbc.query("SELECT pg_advisory_xact_lock(?)", rs -> { }, BLOQUEO_EVALUACION);
        return jdbc.query("""
                WITH capacidades AS (
                    SELECT a.id, a.hostname, a.tipo_activo, a.estado_operativo,
                           a.ultima_actualizacion, a.fecha_eos, s.tipo AS tipo_servidor,
                           st.capacidad_total_tb,
                           (SELECT CASE WHEN bool_and(c.velocidad_ghz > 0 AND c.cantidad_nucleos > 0
                                                     AND c.velocidad_ghz IS NOT NULL AND c.cantidad_nucleos IS NOT NULL)
                                        THEN sum(c.velocidad_ghz * c.cantidad_nucleos) END
                            FROM cpu c WHERE c.activo_id = a.id) AS cpu_ghz,
                           (SELECT CASE WHEN bool_and(r.capacidad_gb IS NOT NULL AND r.capacidad_gb > 0)
                                        THEN sum(r.capacidad_gb) END FROM ram r WHERE r.activo_id = a.id) AS ram_gb,
                           (SELECT CASE WHEN bool_and(d.capacidad_gb IS NOT NULL AND d.capacidad_gb > 0)
                                        THEN sum(d.capacidad_gb) END FROM disco d WHERE d.activo_id = a.id) AS disco_gb
                    FROM activo a
                    LEFT JOIN servidor s ON s.id = a.id
                    LEFT JOIN storage st ON st.id = a.id
                    WHERE a.estado_operativo <> 'Baja' AND NOT a.mantenimiento_activo
                ), lecturas AS (
                    SELECT DISTINCT ON (m.activo_id, m.componente_tipo, m.componente_serial, m.nombre_metrica)
                           m.activo_id, m.componente_tipo, m.componente_serial AS componente_sn,
                           'umbral_' || m.nombre_metrica AS campo,
                           CASE m.nombre_metrica
                               WHEN 'cpu_uso_ghz' THEN 100 * m.valor / NULLIF(a.cpu_ghz, 0)
                               WHEN 'ram_uso_gb' THEN 100 * m.valor / NULLIF(a.ram_gb, 0)
                               WHEN 'disco_uso_gb' THEN 100 * m.valor / NULLIF(a.disco_gb, 0)
                               WHEN 'almacenamiento_uso_tb' THEN 100 * m.valor / NULLIF(a.capacidad_total_tb, 0)
                               ELSE m.valor
                           END AS valor, u.advertencia, u.critico
                    FROM capacidades a
                    JOIN metrica_historica m ON m.activo_id = a.id AND m.fecha_medicion = a.ultima_actualizacion
                    JOIN umbral u ON u.nombre = m.nombre_metrica
                    WHERE u.advertencia IS NOT NULL AND u.critico IS NOT NULL
                      AND u.nombre <> 'red_ancho_banda_mbps'
                    ORDER BY m.activo_id, m.componente_tipo, m.componente_serial, m.nombre_metrica, m.id DESC
                ), eos AS (
                    SELECT a.id AS activo_id, a.fecha_eos - (CURRENT_TIMESTAMP AT TIME ZONE 'America/Lima')::date AS dias,
                           u.advertencia
                    FROM capacidades a JOIN umbral u ON u.nombre = 'dias_para_eos'
                    WHERE a.fecha_eos IS NOT NULL AND u.advertencia IS NOT NULL
                ), niveles AS (
                    SELECT activo_id, componente_tipo, componente_sn, campo,
                           CASE WHEN valor IS NULL THEN 'Sin lectura'
                                WHEN valor >= critico THEN 'Crítico'
                                WHEN valor >= advertencia THEN 'Advertencia'
                                ELSE 'Normal' END AS nivel,
                           CASE WHEN valor IS NULL THEN 'Capacidad no disponible'
                                ELSE 'Valor evaluado: ' || round(valor, 4)::text END AS detalle
                    FROM lecturas
                    UNION ALL
                    SELECT activo_id, NULL, NULL, 'umbral_dias_para_eos',
                           CASE WHEN dias < 0 THEN 'Crítico' WHEN dias <= advertencia THEN 'Advertencia' ELSE 'Normal' END,
                           'Días restantes hasta fin de soporte: ' || dias
                    FROM eos
                ), previos AS (
                    SELECT DISTINCT ON (activo_id, componente_tipo, componente_sn, campo)
                           activo_id, componente_tipo, componente_sn, campo, valor_nuevo AS nivel, fecha_cambio
                    FROM historico_estado
                    WHERE starts_with(campo, 'umbral_')
                    ORDER BY activo_id, componente_tipo, componente_sn, campo, fecha_cambio DESC, id DESC
                ), estados AS (
                    SELECT n.*, p.nivel AS anterior, p.fecha_cambio
                    FROM niveles n
                    LEFT JOIN previos p ON p.activo_id = n.activo_id AND p.campo = n.campo
                        AND p.componente_tipo IS NOT DISTINCT FROM n.componente_tipo
                        AND p.componente_sn IS NOT DISTINCT FROM n.componente_sn
                    UNION ALL
                    SELECT p.activo_id, p.componente_tipo, p.componente_sn, p.campo,
                           'Sin lectura', 'Evaluación no disponible', p.nivel, p.fecha_cambio
                    FROM previos p
                    WHERE p.nivel <> 'Sin lectura' AND NOT EXISTS (
                        SELECT 1 FROM niveles n WHERE n.activo_id = p.activo_id AND n.campo = p.campo
                            AND n.componente_tipo IS NOT DISTINCT FROM p.componente_tipo
                            AND n.componente_sn IS NOT DISTINCT FROM p.componente_sn
                    )
                ), eventos AS (
                    INSERT INTO historico_estado (activo_id, componente_tipo, componente_sn, campo, valor_nuevo, descripcion, fecha_cambio)
                    SELECT activo_id, componente_tipo, componente_sn, campo, nivel, detalle, CURRENT_TIMESTAMP AT TIME ZONE 'UTC'
                    FROM estados
                    WHERE nivel IS DISTINCT FROM anterior AND (anterior IS NOT NULL OR nivel <> 'Sin lectura')
                    RETURNING activo_id, componente_tipo, componente_sn, campo, fecha_cambio
                )
                SELECT a.id, a.hostname, a.tipo_activo, a.tipo_servidor, a.estado_operativo,
                       e.componente_tipo, e.componente_sn, e.campo, e.nivel, e.detalle,
                       COALESCE(n.fecha_cambio, e.fecha_cambio) AS fecha_cambio
                FROM estados e
                JOIN capacidades a ON a.id = e.activo_id
                LEFT JOIN eventos n ON n.activo_id = e.activo_id AND n.campo = e.campo
                    AND n.componente_tipo IS NOT DISTINCT FROM e.componente_tipo
                    AND n.componente_sn IS NOT DISTINCT FROM e.componente_sn
                WHERE e.nivel IN ('Advertencia', 'Crítico')
                ORDER BY fecha_cambio DESC, a.id
                """, (rs, rowNum) -> new AlertaActivo(rs.getLong("id"), rs.getString("hostname"),
                        rs.getString("tipo_activo"), rs.getString("tipo_servidor"),
                        rs.getString("estado_operativo"), null, rs.getString("componente_tipo"),
                        rs.getString("componente_sn"), rs.getString("campo"), rs.getString("detalle"),
                        rs.getString("nivel"), rs.getObject("fecha_cambio", LocalDateTime.class).atOffset(ZoneOffset.UTC)));
    }
}
