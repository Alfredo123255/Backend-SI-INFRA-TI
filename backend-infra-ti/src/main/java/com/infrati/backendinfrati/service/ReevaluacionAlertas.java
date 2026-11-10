package com.infrati.backendinfrati.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/** El aviso se entrega al commit; el ETL evalua sin forzar otra lectura SNMP. */
@Service
public class ReevaluacionAlertas {
    private final JdbcTemplate jdbc;
    public ReevaluacionAlertas(JdbcTemplate jdbc) { this.jdbc=jdbc; }
    public void solicitar(Long activoId) {
        jdbc.query("SELECT pg_notify('etl_revaluar_alertas',?)", rs -> {}, activoId==null?"todos":activoId.toString());
    }
}
