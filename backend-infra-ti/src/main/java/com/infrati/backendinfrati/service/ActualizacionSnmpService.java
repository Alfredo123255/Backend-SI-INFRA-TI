package com.infrati.backendinfrati.service;

import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** PostgreSQL entrega el aviso después del commit de las nuevas credenciales. */
@Service
public class ActualizacionSnmpService {
    private final JdbcTemplate jdbc;

    public ActualizacionSnmpService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void solicitar(Long conexionId) {
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
