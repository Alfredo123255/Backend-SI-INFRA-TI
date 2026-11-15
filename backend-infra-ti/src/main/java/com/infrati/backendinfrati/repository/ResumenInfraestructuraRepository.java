package com.infrati.backendinfrati.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/** Lecturas de la instantánea compartida por Inicio y las vistas consolidadas. */
@Repository
public class ResumenInfraestructuraRepository {
    private final JdbcTemplate jdbc;

    public ResumenInfraestructuraRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public record Centro(String nombre, BigDecimal latitud, BigDecimal longitud) {}
    public record Cluster(String nombre, String datacenter) {}
    public record Activo(long id, String hostname, String datacenter, String cluster, String tipo,
            String estado, boolean mantenimiento, String conexion, LocalDateTime actualizacion,
            String modelo, LocalDate eos, String inicioMantenimiento, BigDecimal cpuUso,
            BigDecimal cpuCapacidad, BigDecimal ramUso, BigDecimal ramCapacidad,
            BigDecimal almacenamientoUso, BigDecimal almacenamientoCapacidad, BigDecimal temperatura) {}

    public List<Activo> listarActivos() {
        // Las capacidades son inválidas si falta el dato de cualquier módulo, igual que en el ETL.
        // DISTINCT ON evita contar dos veces una medición repetida; nunca busca una fecha anterior.
        return jdbc.query("""
            WITH capacidades_cpu AS (
                SELECT activo_id, CASE WHEN bool_and(velocidad_ghz IS NOT NULL AND velocidad_ghz>0
                    AND cantidad_nucleos IS NOT NULL AND cantidad_nucleos>0)
                    THEN sum(velocidad_ghz*cantidad_nucleos) END capacidad FROM cpu GROUP BY activo_id
            ), capacidades_ram AS (
                SELECT activo_id, CASE WHEN bool_and(capacidad_gb IS NOT NULL AND capacidad_gb>0)
                    THEN sum(capacidad_gb) END capacidad FROM ram GROUP BY activo_id
            )
            SELECT a.*, s.tipo tipo_servidor, c.capacidad cpu_capacidad, r.capacidad ram_capacidad,
                   st.capacidad_total_tb, sn.estado_conexion, h.descripcion inicio_mantenimiento,
                   m.cpu_uso, m.ram_uso, m.almacenamiento_uso, m.temperatura_actual
            FROM activo a LEFT JOIN servidor s ON s.id=a.id
            LEFT JOIN capacidades_cpu c ON c.activo_id=a.id
            LEFT JOIN capacidades_ram r ON r.activo_id=a.id
            LEFT JOIN storage st ON st.id=a.id
            LEFT JOIN LATERAL (SELECT estado_conexion FROM monitoreo_snmp
                WHERE activo_id=a.id ORDER BY id DESC LIMIT 1) sn ON true
            LEFT JOIN LATERAL (SELECT descripcion FROM historico_estado WHERE activo_id=a.id
                AND campo='mantenimiento_activo' AND valor_nuevo='true'
                ORDER BY fecha_cambio DESC,id DESC LIMIT 1) h ON true
            LEFT JOIN LATERAL (
                SELECT max(valor) FILTER (WHERE nombre_metrica='cpu_uso_ghz') cpu_uso,
                       max(valor) FILTER (WHERE nombre_metrica='ram_uso_gb') ram_uso,
                       max(valor) FILTER (WHERE nombre_metrica='almacenamiento_uso_tb') almacenamiento_uso,
                       max(valor) FILTER (WHERE nombre_metrica='equipo_temperatura_c') temperatura_actual
                FROM (SELECT DISTINCT ON (nombre_metrica,componente_tipo,componente_serial)
                    nombre_metrica,valor FROM metrica_historica
                    WHERE activo_id=a.id AND fecha_medicion=a.ultima_actualizacion
                      AND nombre_metrica IN ('cpu_uso_ghz','ram_uso_gb','almacenamiento_uso_tb','equipo_temperatura_c')
                      AND componente_tipo IS NULL AND componente_serial IS NULL
                    ORDER BY nombre_metrica,componente_tipo,componente_serial,id DESC) lectura
            ) m ON true ORDER BY a.hostname,a.id
            """,(rs,n)->new Activo(rs.getLong("id"),rs.getString("hostname"),rs.getString("ubicacion"),
                rs.getString("cluster"),"SERVIDOR".equals(rs.getString("tipo_activo"))
                    ? Optional.ofNullable(rs.getString("tipo_servidor")).orElse("RACKEABLE").toLowerCase(Locale.ROOT)
                    : rs.getString("tipo_activo").toLowerCase(Locale.ROOT),
                rs.getString("estado_operativo"),rs.getBoolean("mantenimiento_activo"),rs.getString("estado_conexion"),
                rs.getObject("ultima_actualizacion",LocalDateTime.class),rs.getString("modelo"),
                rs.getObject("fecha_eos",LocalDate.class),rs.getString("inicio_mantenimiento"),
                rs.getBigDecimal("cpu_uso"),rs.getBigDecimal("cpu_capacidad"),rs.getBigDecimal("ram_uso"),
                rs.getBigDecimal("ram_capacidad"),rs.getBigDecimal("almacenamiento_uso"),
                rs.getBigDecimal("capacidad_total_tb"),rs.getBigDecimal("temperatura_actual")));
    }

    public List<Centro> listarDatacenters() {
        return jdbc.query("SELECT * FROM datacenters ORDER BY nombre",(rs,n)->
                new Centro(rs.getString("nombre"),coordenada(rs.getString("latitud")),coordenada(rs.getString("longitud"))));
    }

    public List<Cluster> listarClusters() {
        return jdbc.query("SELECT nombre,datacenter FROM clusters ORDER BY nombre",(rs,n)->
                new Cluster(rs.getString("nombre"),rs.getString("datacenter")));
    }

    public Map<String,BigDecimal> advertenciasUmbral() {
        Map<String,BigDecimal> avisos=new HashMap<>();
        jdbc.query("SELECT nombre,advertencia FROM umbral",rs->{avisos.put(rs.getString("nombre"),rs.getBigDecimal("advertencia"));});
        return avisos;
    }

    private static BigDecimal coordenada(String v) {
        try {return v==null?null:new BigDecimal(v.trim());} catch(NumberFormatException ex) {return null;}
    }
}
