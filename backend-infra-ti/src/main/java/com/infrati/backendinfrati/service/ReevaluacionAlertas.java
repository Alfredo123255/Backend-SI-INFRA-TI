package com.infrati.backendinfrati.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/** Los avisos se entregan al commit; separa reevaluación y lectura SNMP nueva. */
@Service
public class ReevaluacionAlertas {
    private final JdbcTemplate jdbc;
    public ReevaluacionAlertas(JdbcTemplate jdbc) { this.jdbc=jdbc; }
    public void solicitar(Long activoId) {
        jdbc.query("SELECT pg_notify('etl_revaluar_alertas',?)", rs -> {}, activoId==null?"todos":activoId.toString());
    }
    public void solicitarLectura(Long conexionId) {
        jdbc.query("SELECT pg_notify('etl_actualizar_conexion',?)", rs -> {}, conexionId.toString());
    }
}
