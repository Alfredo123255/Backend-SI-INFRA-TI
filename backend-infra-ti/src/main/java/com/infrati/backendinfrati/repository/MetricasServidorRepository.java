package com.infrati.backendinfrati.repository;

import com.infrati.backendinfrati.dto.MetricasServidorRespuesta.Opcion;
import com.infrati.backendinfrati.dto.MetricasServidorRespuesta.PuertoRed;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Array;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class MetricasServidorRepository {
    public static final List<String> METRICAS = List.of(
            "cpu_uso_ghz", "ram_uso_gb", "disco_uso_gb", "equipo_temperatura_c", "equipo_consumo_w",
            "red_trafico_entrada_mbps", "red_trafico_salida_mbps");

    public static final List<String> METRICAS_STORAGE = List.of(
            "almacenamiento_uso_tb", "almacenamiento_iops", "almacenamiento_latencia_ms",
            "almacenamiento_throughput_lectura_mbps", "almacenamiento_throughput_escritura_mbps",
            "equipo_temperatura_c", "equipo_consumo_w", "red_trafico_entrada_mbps", "red_trafico_salida_mbps");
    public static final List<String> METRICAS_CHASIS = List.of("equipo_temperatura_c", "equipo_consumo_w");
    public static final List<String> METRICAS_SWITCH = List.of(
            "cpu_uso_ghz", "equipo_temperatura_c", "equipo_consumo_w",
            "red_trafico_entrada_mbps", "red_trafico_salida_mbps", "red_utilizacion_pct");
    private static final String FILTRO_SWITCH = """
            AND (
                (m.nombre_metrica IN ('red_trafico_entrada_mbps', 'red_trafico_salida_mbps', 'red_utilizacion_pct')
                 AND m.componente_tipo = 'puerto_switch'
                 AND EXISTS (SELECT 1 FROM puerto_switch p WHERE p.switch_id=m.activo_id
                             AND p.numero_puerto=m.componente_serial))
                OR
                (m.nombre_metrica NOT IN ('red_trafico_entrada_mbps', 'red_trafico_salida_mbps', 'red_utilizacion_pct')
                 AND m.componente_tipo IS NULL AND m.componente_serial IS NULL)
            )
            """;
    private static final java.util.regex.Pattern VELOCIDAD = java.util.regex.Pattern.compile(
            "^([0-9]+(?:[.,][0-9]+)?)\\s*(Gbps|Mbps|Kbps|bps)$", java.util.regex.Pattern.CASE_INSENSITIVE);

    private static final String FILTRO_COMPONENTES = """
            AND (
                (m.nombre_metrica IN ('red_trafico_entrada_mbps', 'red_trafico_salida_mbps')
                 AND m.componente_tipo = 'puerto_tarjeta_red'
                 AND (starts_with(m.componente_serial, 'if:') OR starts_with(m.componente_serial, 'cpqnic:')))
                OR
                (m.nombre_metrica NOT IN ('red_trafico_entrada_mbps', 'red_trafico_salida_mbps')
                 AND m.componente_tipo IS NULL AND m.componente_serial IS NULL)
            )
            """;

    private final JdbcTemplate jdbc;

    public MetricasServidorRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<Ficha> ficha(long id) {
        return jdbc.query("""
                SELECT a.id, a.hostname, CASE WHEN ch.id IS NOT NULL THEN 'CHASIS' WHEN sw.id IS NOT NULL THEN 'SWITCH' WHEN st.id IS NOT NULL THEN 'STORAGE' ELSE s.tipo END AS tipo, conexion.estado_conexion,
                       st.capacidad_total_tb,
                       cpu.total AS cpu, ram.total AS ram, disco.total AS disco, disco.cantidad
                FROM activo a LEFT JOIN servidor s ON s.id = a.id LEFT JOIN storage st ON st.id = a.id LEFT JOIN switch sw ON sw.id = a.id LEFT JOIN chasis_blade ch ON ch.id=a.id
                LEFT JOIN LATERAL (
                    SELECT estado_conexion FROM monitoreo_snmp WHERE activo_id = a.id ORDER BY id DESC LIMIT 1
                ) conexion ON true
                CROSS JOIN LATERAL (
                    SELECT CASE WHEN count(*) = count(*) FILTER (WHERE velocidad_ghz > 0 AND cantidad_nucleos > 0)
                                THEN sum(velocidad_ghz * cantidad_nucleos) END AS total
                    FROM cpu WHERE activo_id = a.id
                ) cpu
                CROSS JOIN LATERAL (
                    SELECT CASE WHEN count(*) = count(*) FILTER (WHERE capacidad_gb > 0)
                                THEN sum(capacidad_gb) END AS total FROM ram WHERE activo_id = a.id
                ) ram
                CROSS JOIN LATERAL (
                    SELECT CASE WHEN count(*) = count(*) FILTER (WHERE capacidad_gb > 0)
                                THEN sum(capacidad_gb) END AS total, count(*) AS cantidad
                    FROM disco WHERE activo_id = a.id
                ) disco
                WHERE a.id = ? AND (s.id IS NOT NULL OR st.id IS NOT NULL OR sw.id IS NOT NULL OR ch.id IS NOT NULL)
                """, (rs, n) -> new Ficha(rs.getLong("id"), rs.getString("hostname"), rs.getString("tipo"),
                rs.getString("estado_conexion"), rs.getBigDecimal("cpu"), rs.getBigDecimal("ram"),
                rs.getBigDecimal("disco"), rs.getLong("cantidad") > 0, rs.getBigDecimal("capacidad_total_tb")), id).stream().findFirst();
    }

    public List<Opcion> disponibles(long id, String tipo) {
        return jdbc.query("""
                SELECT a.id, a.hostname FROM activo a
                LEFT JOIN servidor s ON s.id = a.id LEFT JOIN storage st ON st.id = a.id LEFT JOIN switch sw ON sw.id = a.id LEFT JOIN chasis_blade ch ON ch.id=a.id
                WHERE ((? IN ('RACKEABLE', 'BLADE') AND s.tipo IN ('RACKEABLE', 'BLADE')) OR (? = 'STORAGE' AND st.id IS NOT NULL) OR (? = 'SWITCH' AND sw.id IS NOT NULL) OR (? = 'CHASIS' AND ch.id IS NOT NULL))
                  AND a.id <> ? AND a.estado_operativo <> 'Baja'
                ORDER BY lower(a.hostname), a.id
                """, (rs, n) -> new Opcion(rs.getLong(1), rs.getString(2)), tipo, tipo, tipo, tipo, id);
    }

    // La capacidad corresponde a la última actualización completada por el ETL.
    // Ante duplicados del mismo componente se conserva la última fila de esa lectura.
    public BigDecimal capacidadRedGbps(long id) {
        return jdbc.queryForObject("""
                SELECT sum(ultima.valor) / 1000
                FROM (
                    SELECT DISTINCT ON (m.componente_serial) m.valor
                    FROM metrica_historica m JOIN activo a ON a.id = m.activo_id
                    WHERE a.id = ? AND m.nombre_metrica = 'red_ancho_banda_mbps'
                      AND m.fecha_medicion = a.ultima_actualizacion
                    ORDER BY m.componente_serial, m.id DESC
                ) ultima
                """, BigDecimal.class, id);
    }

    public List<PuertoRed> puertosRed(long id) {
        return jdbc.query("""
                SELECT p.id, p.numero_puerto, p.estado, p.velocidad
                FROM puerto_tarjeta_red p JOIN tarjeta_red t ON t.id = p.tarjeta_red_id
                WHERE t.activo_id = ? ORDER BY t.id, p.id
                """, (rs, n) -> new PuertoRed(rs.getLong(1), rs.getString(2), rs.getString(3),
                        velocidadGbps(rs.getString(4))), id);
    }

    static BigDecimal velocidadGbps(String texto) {
        if (texto == null) return null;
        var coincidencia = VELOCIDAD.matcher(texto.trim());
        if (!coincidencia.matches()) return null;
        BigDecimal valor = new BigDecimal(coincidencia.group(1).replace(',', '.'));
        if (valor.signum() <= 0) return null;
        return switch (coincidencia.group(2).toLowerCase(java.util.Locale.ROOT)) {
            case "gbps" -> valor;
            case "mbps" -> valor.movePointLeft(3);
            case "kbps" -> valor.movePointLeft(6);
            default -> valor.movePointLeft(9);
        };
    }

    public List<Lectura> actuales(long id, List<String> metricas) {
        String sql = """
                WITH ultimas AS (
                    SELECT nombres.nombre, fecha.fecha_medicion
                    FROM unnest(?::text[]) nombres(nombre)
                    JOIN LATERAL (
                        SELECT m.fecha_medicion FROM metrica_historica m
                        WHERE m.activo_id = ? AND m.nombre_metrica = nombres.nombre
                """ + FILTRO_COMPONENTES + """
                        ORDER BY m.fecha_medicion DESC LIMIT 1
                    ) fecha ON true
                )
                SELECT m.nombre_metrica, m.fecha_medicion, sum(m.valor) AS valor
                FROM ultimas u JOIN metrica_historica m ON m.activo_id = ?
                  AND m.nombre_metrica = u.nombre AND m.fecha_medicion = u.fecha_medicion
                WHERE true
                """ + FILTRO_COMPONENTES + """
                GROUP BY m.nombre_metrica, m.fecha_medicion
                """;
        return jdbc.query(connection -> {
            var ps = connection.prepareStatement(sql);
            ps.setArray(1, nombres(connection, metricas));
            ps.setLong(2, id);
            ps.setLong(3, id);
            return ps;
        }, (rs, n) -> new Lectura(rs.getString(1), rs.getObject(2, LocalDateTime.class), rs.getBigDecimal(3)));
    }

    public List<Lectura> historico(long id, LocalDateTime origenLima, LocalDateTime desdeUtc,
                                   LocalDateTime hastaUtc, String intervalo, List<String> metricas) {
        return historico(id, origenLima, desdeUtc, hastaUtc, intervalo, metricas, false);
    }

    public List<Lectura> historico(long id, LocalDateTime origenLima, LocalDateTime desdeUtc,
                                   LocalDateTime hastaUtc, String intervalo, List<String> metricas, boolean esSwitch) {
        String sql = """
                WITH por_instante AS (
                    SELECT m.nombre_metrica, m.fecha_medicion, CASE WHEN m.nombre_metrica = 'red_utilizacion_pct' THEN max(m.valor) ELSE sum(m.valor) END AS valor
                    FROM metrica_historica m
                    WHERE m.activo_id = ? AND m.nombre_metrica = ANY(?::text[])
                      AND m.fecha_medicion >= ? AND m.fecha_medicion < ?
                """ + (esSwitch ? FILTRO_SWITCH : FILTRO_COMPONENTES) + """
                    GROUP BY m.nombre_metrica, m.fecha_medicion
                )
                SELECT nombre_metrica,
                       date_bin(?::interval, (fecha_medicion AT TIME ZONE 'UTC') AT TIME ZONE 'America/Lima',
                                ?::timestamp) AS bloque,
                       avg(valor) AS valor
                FROM por_instante
                GROUP BY nombre_metrica, bloque
                ORDER BY bloque
                """;
        return jdbc.query(connection -> {
            var ps = connection.prepareStatement(sql);
            ps.setLong(1, id);
            ps.setArray(2, nombres(connection, metricas));
            ps.setObject(3, desdeUtc);
            ps.setObject(4, hastaUtc);
            ps.setString(5, intervalo);
            ps.setObject(6, origenLima);
            return ps;
        }, (rs, n) -> new Lectura(rs.getString(1), rs.getObject(2, LocalDateTime.class), rs.getBigDecimal(3)));
    }

    public List<Lectura> actualesSwitch(long id) {
        String sql = """
                WITH por_instante AS (
                    SELECT m.nombre_metrica, m.fecha_medicion,
                           CASE WHEN m.nombre_metrica='red_utilizacion_pct' THEN max(m.valor) ELSE sum(m.valor) END AS valor
                    FROM metrica_historica m JOIN activo a ON a.id=m.activo_id
                    WHERE m.activo_id=? AND m.nombre_metrica=ANY(?::text[])
                      AND (m.componente_tipo IS NULL OR (m.fecha_medicion=a.ultima_actualizacion
                           AND EXISTS (SELECT 1 FROM puerto_switch p WHERE p.switch_id=a.id
                                       AND p.numero_puerto=m.componente_serial AND p.estado<>'Apagado')))
                """ + FILTRO_SWITCH + """
                    GROUP BY m.nombre_metrica, m.fecha_medicion
                )
                SELECT DISTINCT ON(nombre_metrica) nombre_metrica, fecha_medicion, valor
                FROM por_instante ORDER BY nombre_metrica, fecha_medicion DESC
                """;
        return jdbc.query(c -> {
            var ps=c.prepareStatement(sql); ps.setLong(1,id); ps.setArray(2,nombres(c,METRICAS_SWITCH)); return ps;
        }, (rs,n)->new Lectura(rs.getString(1),rs.getObject(2,LocalDateTime.class),rs.getBigDecimal(3)));
    }

    public List<InventarioPuerto> inventarioSwitch(long id) {
        return jdbc.query("SELECT id,numero_puerto,estado FROM puerto_switch WHERE switch_id=? ORDER BY id",
                (rs,n)->new InventarioPuerto(rs.getLong(1),rs.getString(2),rs.getString(3)),id);
    }

    public List<LecturaPuerto> actualesPuertosSwitch(long id) {
        return jdbc.query("""
                SELECT DISTINCT ON(m.componente_serial,m.nombre_metrica)
                       m.componente_serial,m.nombre_metrica,m.fecha_medicion,m.valor
                FROM metrica_historica m JOIN activo a ON a.id=m.activo_id
                JOIN puerto_switch p ON p.switch_id=a.id AND p.numero_puerto=m.componente_serial
                WHERE a.id=? AND m.componente_tipo='puerto_switch' AND m.fecha_medicion=a.ultima_actualizacion
                  AND m.nombre_metrica IN ('red_ancho_banda_mbps','red_trafico_entrada_mbps','red_trafico_salida_mbps','red_utilizacion_pct')
                ORDER BY m.componente_serial,m.nombre_metrica,m.id DESC
                """,(rs,n)->new LecturaPuerto(rs.getString(1),rs.getString(2),rs.getObject(3,LocalDateTime.class),rs.getBigDecimal(4)),id);
    }

    public List<LecturaPuerto> historicoPuertosSwitch(long id, LocalDateTime origenLima,
                        LocalDateTime desdeUtc, LocalDateTime hastaUtc, String intervalo) {
        return jdbc.query("""
                SELECT m.componente_serial,m.nombre_metrica,
                       date_bin(?::interval,(m.fecha_medicion AT TIME ZONE 'UTC') AT TIME ZONE 'America/Lima',?::timestamp) AS bloque,
                       avg(m.valor)
                FROM metrica_historica m
                JOIN puerto_switch p ON p.switch_id=m.activo_id AND p.numero_puerto=m.componente_serial
                WHERE m.activo_id=? AND m.componente_tipo='puerto_switch'
                  AND m.fecha_medicion>=? AND m.fecha_medicion<?
                  AND m.nombre_metrica IN ('red_trafico_entrada_mbps','red_trafico_salida_mbps','red_utilizacion_pct')
                GROUP BY m.componente_serial,m.nombre_metrica,bloque
                ORDER BY m.componente_serial,bloque
                """,(rs,n)->new LecturaPuerto(rs.getString(1),rs.getString(2),rs.getObject(3,LocalDateTime.class),rs.getBigDecimal(4)),
                intervalo,origenLima,id,desdeUtc,hastaUtc);
    }

    public record InventarioPuerto(long id, String numero, String estado) {}
    public record LecturaPuerto(String numero, String nombre, LocalDateTime fecha, BigDecimal valor) {}

    private static Array nombres(java.sql.Connection connection, List<String> metricas) throws java.sql.SQLException {
        return connection.createArrayOf("text", metricas.toArray());
    }

    public record Ficha(long id, String hostname, String tipo, String estadoConexion,
                        BigDecimal cpu, BigDecimal ram, BigDecimal disco, boolean tieneDiscos, BigDecimal capacidadStorage) {}
    public record Lectura(String nombre, LocalDateTime fecha, BigDecimal valor) {}
}
