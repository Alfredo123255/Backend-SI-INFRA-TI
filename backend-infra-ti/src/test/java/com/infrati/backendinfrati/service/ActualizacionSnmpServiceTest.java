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
    @Autowired private ReevaluacionAlertas reevaluacion;

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

    @Test
    void solicitarLecturaConservaElCanalYPayloadDelServicioDeActualizacion() throws Exception {
        verificarAviso("etl_actualizar_conexion", "42", () -> reevaluacion.solicitarLectura(42L), false);
    }

    @Test
    void reevaluacionConservaElIdYElPayloadTodosParaLaSolicitudGlobal() throws Exception {
        verificarAviso("etl_revaluar_alertas", "42", () -> reevaluacion.solicitar(42L), false);
        verificarAviso("etl_revaluar_alertas", "todos", () -> reevaluacion.solicitar(null), false);
    }

    @Test
    void rollbackDescartaTambienLasNotificacionesDeReevaluacionYLectura() throws Exception {
        verificarAviso("etl_revaluar_alertas", "todos", () -> reevaluacion.solicitar(null), true);
        verificarAviso("etl_actualizar_conexion", "42", () -> reevaluacion.solicitarLectura(42L), true);
    }

    private void verificarAviso(String canal, String payload, Runnable solicitar, boolean rollback) throws Exception {
        try (var escucha = dataSource.getConnection()) {
            assertTrue(escucha.getMetaData().getURL().contains("test"), "Solo BD de pruebas");
            escucha.setAutoCommit(true);
            escucha.createStatement().execute("LISTEN " + canal);
            var pg = escucha.unwrap(org.postgresql.PGConnection.class);
            new TransactionTemplate(transacciones).executeWithoutResult(estado -> {
                solicitar.run();
                try {
                    var previos = pg.getNotifications(100);
                    assertTrue(previos == null || previos.length == 0);
                } catch (java.sql.SQLException e) { throw new RuntimeException(e); }
                if (rollback) estado.setRollbackOnly();
            });
            var avisos = pg.getNotifications(rollback ? 200 : 2000);
            if (rollback) {
                assertTrue(avisos == null || avisos.length == 0);
            } else {
                assertNotNull(avisos);
                assertEquals(1, avisos.length);
                assertEquals(canal, avisos[0].getName());
                assertEquals(payload, avisos[0].getParameter());
            }
        }
    }
}
