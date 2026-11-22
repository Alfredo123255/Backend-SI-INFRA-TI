package com.infrati.backendinfrati.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.util.*;

/** Consultas conjuntas del listado; no carga las colecciones de las fichas. */
@Repository
public class InventarioResumenRepository {
    private final JdbcTemplate jdbc;
    public InventarioResumenRepository(JdbcTemplate jdbc) { this.jdbc=jdbc; }
    public record Resumen(Double cpu,Double ram,Double usoCpu,Double usoRam) {}
    public Map<Long,Resumen> servidores(Collection<Long> ids) {
        if(ids.isEmpty())return Map.of();
        var resultado=new HashMap<Long,Resumen>();
        jdbc.query(connection->{
            var ps=connection.prepareStatement("""
                WITH ids AS (SELECT unnest(?::integer[]) id),
                capacidades_cpu AS (
                    SELECT c.activo_id,sum(c.velocidad_ghz*c.cantidad_nucleos) FILTER
                        (WHERE c.velocidad_ghz IS NOT NULL AND c.cantidad_nucleos>0) capacidad
                    FROM cpu c JOIN ids i ON i.id=c.activo_id GROUP BY c.activo_id
                ), capacidades_ram AS (
                    SELECT r.activo_id,sum(r.capacidad_gb) capacidad
                    FROM ram r JOIN ids i ON i.id=r.activo_id GROUP BY r.activo_id
                )
                SELECT i.id,c.capacidad cpu,r.capacidad ram,uso_cpu.valor uso_cpu,uso_ram.valor uso_ram
                FROM ids i LEFT JOIN capacidades_cpu c ON c.activo_id=i.id
                LEFT JOIN capacidades_ram r ON r.activo_id=i.id
                LEFT JOIN LATERAL (
                    SELECT m.valor FROM metrica_historica m
                    WHERE m.activo_id=i.id AND m.nombre_metrica='cpu_uso_ghz'
                    ORDER BY m.fecha_medicion DESC,m.id DESC LIMIT 1
                ) uso_cpu ON true
                LEFT JOIN LATERAL (
                    SELECT m.valor FROM metrica_historica m
                    WHERE m.activo_id=i.id AND m.nombre_metrica='ram_uso_gb'
                    ORDER BY m.fecha_medicion DESC,m.id DESC LIMIT 1
                ) uso_ram ON true
                """);
            ps.setArray(1,connection.createArrayOf("integer",ids.toArray()));return ps;
        },rs->{resultado.put(rs.getLong("id"),new Resumen(numero(rs,"cpu"),numero(rs,"ram"),
            numero(rs,"uso_cpu"),numero(rs,"uso_ram")));});
        return resultado;
    }
    private static Double numero(java.sql.ResultSet rs,String columna) throws java.sql.SQLException { var valor=rs.getBigDecimal(columna);return valor==null?null:valor.doubleValue(); }
    public Map<Long,List<String>> velocidadesSwitch(Collection<Long> ids) {
        if(ids.isEmpty())return Map.of();
        Map<Long,List<String>> resultado=new HashMap<>();
        jdbc.query(connection->{
            var ps=connection.prepareStatement("SELECT switch_id,velocidad FROM puerto_switch WHERE switch_id=ANY(?::integer[]) ORDER BY switch_id,id");
            ps.setArray(1,connection.createArrayOf("integer",ids.toArray()));return ps;
        },rs->{resultado.computeIfAbsent(rs.getLong("switch_id"),id->new ArrayList<>()).add(rs.getString("velocidad"));});
        return resultado;
    }
}
