package com.infrati.backendinfrati.repository;

import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Avisos PostgreSQL al ETL; participan en la transacción del servicio que los solicita. */
@Repository
public class NotificacionEtlRepository {
    private final JdbcTemplate jdbc;

    public NotificacionEtlRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void notificarReevaluacionAlertas(Long activoId) {
        jdbc.query("SELECT pg_notify('etl_revaluar_alertas',?)", rs -> {}, activoId==null?"todos":activoId.toString());
    }

    public void notificarActualizacionConexion(Long conexionId) {
        jdbc.query("SELECT pg_notify('etl_actualizar_conexion',?)", rs -> {}, conexionId.toString());
    }

    public void notificarActualizacionConexionConCallback(Long conexionId) {
        jdbc.execute((ConnectionCallback<Void>) connection -> {
            try (var sentencia = connection.prepareStatement("SELECT pg_notify(?, ?)")) {
                sentencia.setString(1, "etl_actualizar_conexion");
                sentencia.setString(2, conexionId.toString());
                sentencia.execute();
            }
            return null;
        });
    }
}
