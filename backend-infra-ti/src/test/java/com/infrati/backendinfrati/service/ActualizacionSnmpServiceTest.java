package com.infrati.backendinfrati.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import javax.sql.DataSource;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ActualizacionSnmpServiceTest {
    @Autowired private DataSource dataSource;
    @Autowired private PlatformTransactionManager transacciones;
    @Autowired private ActualizacionSnmpService servicio;

    @Test
    void elProgramadorRecibeElAvisoSoloDespuesDelCommit() throws Exception {
        try (var escucha = dataSource.getConnection()) {
            assertTrue(escucha.getMetaData().getURL().contains("test"), "Solo BD de pruebas");
            escucha.setAutoCommit(true);
            escucha.createStatement().execute("LISTEN etl_actualizar_conexion");
            var pg = escucha.unwrap(org.postgresql.PGConnection.class);
            long id = System.nanoTime();
            new TransactionTemplate(transacciones).executeWithoutResult(estado -> {
                servicio.solicitar(id);
                try {
                    var previos = pg.getNotifications(100);
                    assertTrue(previos == null || previos.length == 0);
                }
                catch (java.sql.SQLException e) { throw new RuntimeException(e); }
            });
            var avisos = pg.getNotifications(2000);
            assertNotNull(avisos);
            assertEquals(Long.toString(id), avisos[0].getParameter());
        }
    }

    @Test
    void unRollbackNoSolicitaUnaLecturaConCredencialesDescartadas() throws Exception {
        try (var escucha = dataSource.getConnection()) {
            assertTrue(escucha.getMetaData().getURL().contains("test"), "Solo BD de pruebas");
            escucha.setAutoCommit(true);
            escucha.createStatement().execute("LISTEN etl_actualizar_conexion");
            var pg = escucha.unwrap(org.postgresql.PGConnection.class);
            new TransactionTemplate(transacciones).executeWithoutResult(estado -> {
                servicio.solicitar(System.nanoTime());
                estado.setRollbackOnly();
            });
            var avisos = pg.getNotifications(200);
            assertTrue(avisos == null || avisos.length == 0);
        }
    }
}
