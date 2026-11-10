package com.infrati.backendinfrati.service;

import com.infrati.backendinfrati.exception.SolicitudInvalidaException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class MetricasServidorServiceTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired MetricasServidorService service;

    @Test
    void cp081_cp084_cp092_cp096_cp097_usaLecturasYConservaIntervalosVacios() {
        assertTrue(jdbc.queryForObject("SELECT current_database()", String.class).contains("test"));
        String identificador = UUID.randomUUID().toString();
        String sede = "METRICAS-" + identificador;
        jdbc.update("INSERT INTO datacenters(nombre) VALUES (?)", sede);
        Long id = jdbc.queryForObject("""
                INSERT INTO activo(numero_serie, hostname, ubicacion, tipo_activo, estado_operativo)
                VALUES (?, ?, ?, 'SERVIDOR', 'Encendido') RETURNING id
                """, Long.class, identificador, "prueba-metricas", sede);
        jdbc.update("INSERT INTO servidor(id, tipo) VALUES (?, 'BLADE')", id);
        jdbc.update("""
                INSERT INTO monitoreo_snmp(ip_gestion, usuario, clave, frecuencia_actualizacion, estado_conexion, activo_id)
                VALUES (?, 'prueba', 'prueba', 300, 'Sin conexión', ?)
                """, identificador, id);
        jdbc.update("INSERT INTO cpu(activo_id, velocidad_ghz, cantidad_nucleos, estado) VALUES (?, 2.5, 16, 'Encendido')", id);

        LocalDate dia = LocalDate.of(2026, 10, 15);
        LocalDateTime instante = dia.atTime(5, 10);
        lectura(id, "cpu_uso_ghz", null, null, 20, instante);
        lectura(id, "red_trafico_entrada_mbps", "puerto_tarjeta_red", "if:1", 30, instante);
        lectura(id, "red_trafico_entrada_mbps", "puerto_tarjeta_red", "if:2", 70, instante);
        lectura(id, "red_trafico_entrada_mbps", "puerto_tarjeta_red", "if:1", 50, instante.plusMinutes(20));

        var resultado = service.consultar(id, "custom", dia, dia, List.of());
        var servidor = resultado.servidor();
        assertFalse(servidor.tieneDiscos());
        assertEquals("Sin conexión", servidor.estadoConexion());
        assertNotNull(servidor.ultimaMedicion());
        assertEquals(24, resultado.intervalos().size());
        assertEquals(dia.atStartOfDay(), resultado.intervalos().getFirst());
        var trafico = servidor.metricas().get("red_trafico_entrada_mbps");
        assertEquals(0, BigDecimal.valueOf(75).compareTo(trafico.valores().getFirst()));
        assertNull(trafico.valores().get(1));
        assertEquals(0, BigDecimal.valueOf(50).compareTo(servidor.metricas().get("cpu_uso_ghz").porcentaje()));
        assertTrue(servidor.metricas().get("ram_uso_gb").valores().stream().allMatch(v -> v == null));
    }

    @Test
    void cp108_cp110_validaFechasYAgrupacionesLimite() {
        LocalDateTime ahora = LocalDateTime.of(2026, 11, 4, 13, 0);
        LocalDate hoy = ahora.toLocalDate();
        assertThrows(SolicitudInvalidaException.class,
                () -> MetricasServidorService.rango("custom", hoy, hoy.minusDays(1), ahora));
        assertEquals(24, MetricasServidorService.rango("custom", hoy, hoy, ahora).intervalos().size());
        assertEquals(2, MetricasServidorService.rango("custom", hoy.minusDays(1), hoy, ahora).intervalos().size());
        assertEquals(30, MetricasServidorService.rango("custom", hoy.minusDays(29), hoy, ahora).intervalos().size());
        assertEquals(5, MetricasServidorService.rango("custom", hoy.minusDays(30), hoy, ahora).intervalos().size());
    }

    private void lectura(Long id, String nombre, String tipo, String serial, int valor, LocalDateTime fecha) {
        jdbc.update("""
                INSERT INTO metrica_historica(activo_id, nombre_metrica, componente_tipo, componente_serial, valor, fecha_medicion)
                VALUES (?, ?, ?, ?, ?, ?)
                """, id, nombre, tipo, serial, valor, fecha);
    }
}
