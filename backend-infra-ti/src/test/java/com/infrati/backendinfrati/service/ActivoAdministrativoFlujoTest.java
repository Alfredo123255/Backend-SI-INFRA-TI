package com.infrati.backendinfrati.service;

import com.infrati.backendinfrati.dto.ActualizarAdministrativosActivoRequest;
import com.infrati.backendinfrati.repository.ActivoRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class ActivoAdministrativoFlujoTest {
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ActivoAdministrativoService servicio;
    @Autowired private ActivoRepository activos;
    @Autowired private EntityManager entityManager;

    @Test
    void cp072_073_076_persisten_administrativos_eol_compartido_y_cluster() {
        assertTrue(jdbc.queryForObject("SELECT current_database()", String.class).contains("test"));
        String base = "HALLAZGO-" + UUID.randomUUID();
        String modelo = base + "-modelo", cluster = base + "-cluster";
        jdbc.update("INSERT INTO datacenters(nombre) VALUES (?)", base);
        jdbc.update("INSERT INTO modelos(nombre_modelo) VALUES (?)", modelo);
        jdbc.update("INSERT INTO clusters(nombre,datacenter,ambiente) VALUES (?,?,'QA')", cluster, base);
        Long[] ids = new Long[2];
        for (int i = 0; i < ids.length; i++) {
            ids[i] = jdbc.queryForObject("""
                INSERT INTO activo(numero_serie,hostname,modelo,ubicacion,tipo_activo,estado_operativo)
                VALUES (?,?,?,?,'SERVIDOR','Encendido') RETURNING id
                """, Long.class, base + i, base + i, modelo, base);
            jdbc.update("INSERT INTO servidor(id,tipo) VALUES (?,'RACKEABLE')", ids[i]);
        }
        var eos = LocalDate.of(2030, 12, 31);
        var eol = LocalDate.of(2031, 12, 31);
        servicio.actualizar(ids[0], new ActualizarAdministrativosActivoRequest(
                "Ana Pérez", "OC-2026-001", eos, eol, null, null, null, null, cluster, null, null));
        entityManager.flush();
        entityManager.clear();
        var primero = activos.findById(ids[0]).orElseThrow();
        assertEquals("Ana Pérez", primero.getResponsable());
        assertEquals("OC-2026-001", primero.getOrden_compra());
        assertEquals(eos, primero.getFecha_eos());
        assertEquals(cluster, primero.getCluster());
        var soloConsulta = new ActualizarAdministrativosActivoRequest(
                null, null, null, null, null, null, null, null, null, null, null);
        assertEquals(eol, servicio.actualizar(ids[0], soloConsulta).fechaEol());
        assertEquals(eol, servicio.actualizar(ids[1], soloConsulta).fechaEol());
        servicio.actualizar(ids[0], new ActualizarAdministrativosActivoRequest(
                "Luis Díaz", null, null, null, null, null, null, null, null, null, null));
        entityManager.flush();
        entityManager.clear();
        assertEquals("Luis Díaz", activos.findById(ids[0]).orElseThrow().getResponsable());
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM historico_estado WHERE activo_id=? AND campo='cluster'", Integer.class, ids[0]));
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM historico_estado WHERE activo_id=? AND campo IN ('responsable','fecha_eos','version_so')", Integer.class, ids[0]));
    }
}
