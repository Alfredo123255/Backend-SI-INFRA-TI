package com.infrati.backendinfrati.service;

import com.infrati.backendinfrati.dto.MetricasServidorRespuesta.Servidor;
import com.infrati.backendinfrati.exception.SolicitudInvalidaException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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
class MetricasStorageServiceTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired MetricasServidorService service;
    private static final LocalDate DIA = LocalDate.of(2026, 10, 15);

    @ParameterizedTest
    @ValueSource(strings = {"HPE", "Huawei"})
    void cp085_086_087_089_090_capacidadRendimientoYPuertos(String fabricante) {
        long id = crear(fabricante, "Activo");
        jdbc.update("""
                INSERT INTO tarjeta_red(activo_id, modelo, estado)
                VALUES (?, 'prueba', 'Encendido')
                """, id);
        long tarjeta = jdbc.queryForObject("SELECT id FROM tarjeta_red WHERE activo_id=?", Long.class, id);
        puerto(tarjeta, "1", "1000 Mbps", "Encendido");
        puerto(tarjeta, "2", "25 Gbps", "Encendido");
        puerto(tarjeta, "3", "40 Gbps", "Apagado");
        jdbc.update("UPDATE activo SET ultima_actualizacion=? WHERE id=?", DIA.atTime(5, 10), id);
        // Ni una lectura anterior ni otra posterior pertenecen a la actualización consultada.
        anchoBanda(id, "if:99", "100000", DIA.atTime(5, 9));
        anchoBanda(id, "if:98", "100000", DIA.atTime(5, 11));
        // Un mismo componente no debe sumarse dos veces; prevalece su última fila.
        anchoBanda(id, "if:1", "12000", DIA.atTime(5, 10));
        anchoBanda(id, "if:1", "16000", DIA.atTime(5, 10));
        if (fabricante.equals("Huawei")) anchoBanda(id, "if:2", "16000", DIA.atTime(5, 10));
        lectura(id, "almacenamiento_uso_tb", new BigDecimal("5"));
        lectura(id, "almacenamiento_iops", new BigDecimal("500"));
        var latencia = new BigDecimal(fabricante.equals("Huawei") ? "2" : "1.72");
        lectura(id, "almacenamiento_latencia_ms", latencia);
        lectura(id, "almacenamiento_throughput_lectura_mbps", new BigDecimal("100"));
        lectura(id, "almacenamiento_throughput_escritura_mbps", new BigDecimal("200"));

        var respuesta = service.consultar(id, "custom", DIA, DIA, List.of());
        var storage = respuesta.servidor();
        assertEquals("STORAGE", storage.tipo());
        igual("20", storage.metricas().get("almacenamiento_uso_tb").capacidad());
        igual("25", storage.metricas().get("almacenamiento_uso_tb").porcentaje());
        igual("5", storage.metricas().get("almacenamiento_uso_tb").actual());
        igual("500", storage.metricas().get("almacenamiento_iops").actual());
        assertEquals(0, latencia.compareTo(storage.metricas().get("almacenamiento_latencia_ms").actual()));
        igual("100", storage.metricas().get("almacenamiento_throughput_lectura_mbps").actual());
        igual("200", storage.metricas().get("almacenamiento_throughput_escritura_mbps").actual());
        igual(fabricante.equals("Huawei") ? "32" : "16", storage.capacidadRedGbps());
        assertEquals(3, storage.puertosRed().size());
        assertEquals(24, respuesta.intervalos().size());
        assertNull(storage.metricas().get("almacenamiento_iops").valores().get(1));

        puerto(tarjeta, "4", null, "Encendido");
        igual(fabricante.equals("Huawei") ? "32" : "16",
                service.consultar(id, "24h", null, null, List.of()).servidor().capacidadRedGbps());
    }

    @ParameterizedTest
    @ValueSource(strings = {"HPE", "Huawei"})
    void cp092_096_098_103_sinLecturaConexionPeriodoYComparacion(String fabricante) {
        long id = crear(fabricante, "Sin conexión");
        long otro = crear(fabricante, "Activo");
        lectura(id, "almacenamiento_iops", BigDecimal.TEN);
        var respuesta = service.consultar(id, "custom", DIA, DIA, List.of(otro));
        Servidor storage = respuesta.servidor();
        assertNull(storage.capacidadRedGbps());
        assertEquals("Sin conexión", storage.estadoConexion());
        assertNull(storage.metricas().get("almacenamiento_latencia_ms").actual());
        assertTrue(storage.metricas().get("almacenamiento_latencia_ms").valores().stream().allMatch(v -> v == null));
        igual("10", storage.metricas().get("almacenamiento_iops").actual());
        assertNotNull(storage.ultimaMedicion());
        assertEquals(otro, respuesta.comparados().getFirst().id());
        assertEquals(24, respuesta.comparados().getFirst().metricas().get("almacenamiento_iops").valores().size());
        var vacio = service.consultar(id, "custom", DIA.minusDays(1), DIA.minusDays(1), List.of());
        assertTrue(vacio.servidor().metricas().values().stream().allMatch(
                m -> m.valores().stream().allMatch(v -> v == null)));
        assertThrows(SolicitudInvalidaException.class,
                () -> service.consultar(id, "24h", null, null, List.of(id)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"HPE", "Huawei"})
    void capacidadRedSinLecturaActualNoReutilizaLaAnteriorYCeroEsValido(String fabricante) {
        long id = crear(fabricante, "Activo");
        jdbc.update("UPDATE activo SET ultima_actualizacion=? WHERE id=?", DIA.atTime(5, 10), id);
        anchoBanda(id, "if:1", "16000", DIA.atTime(5, 9));
        assertNull(service.consultar(id, "24h", null, null, List.of()).servidor().capacidadRedGbps());
        anchoBanda(id, "if:1", "0", DIA.atTime(5, 10));
        igual("0", service.consultar(id, "24h", null, null, List.of()).servidor().capacidadRedGbps());
    }

    private void anchoBanda(long id, String componente, String valor, LocalDateTime fecha) {
        jdbc.update("""
                INSERT INTO metrica_historica(activo_id, nombre_metrica, valor, fecha_medicion,
                                             componente_tipo, componente_serial)
                VALUES (?, 'red_ancho_banda_mbps', ?, ?, 'puerto_tarjeta_red', ?)
                """, id, new BigDecimal(valor), fecha, componente);
    }

    private long crear(String fabricante, String estado) {
        assertTrue(jdbc.queryForObject("SELECT current_database()", String.class).contains("test"));
        String nombre = UUID.randomUUID().toString();
        jdbc.update("INSERT INTO datacenters(nombre) VALUES (?)", nombre);
        long id = jdbc.queryForObject("""
                INSERT INTO activo(numero_serie, hostname, fabricante, ubicacion, tipo_activo, estado_operativo)
                VALUES (?, ?, ?, ?, 'STORAGE', 'Encendido') RETURNING id
                """, Long.class, nombre, nombre, fabricante, nombre);
        jdbc.update("INSERT INTO storage(id, capacidad_total_tb, capacidad_usada_tb) VALUES (?, 20, 5)", id);
        jdbc.update("""
                INSERT INTO monitoreo_snmp(ip_gestion, usuario, clave, frecuencia_actualizacion, estado_conexion, activo_id)
                VALUES (?, 'prueba', 'prueba', 300, ?, ?)
                """, nombre, estado, id);
        return id;
    }

    private void puerto(long tarjeta, String numero, String velocidad, String estado) {
        jdbc.update("""
                INSERT INTO puerto_tarjeta_red(tarjeta_red_id, numero_puerto, velocidad, estado) VALUES (?, ?, ?, ?)
                """, tarjeta, numero, velocidad, estado);
    }

    private void lectura(long id, String nombre, BigDecimal valor) {
        jdbc.update("""
                INSERT INTO metrica_historica(activo_id, nombre_metrica, valor, fecha_medicion) VALUES (?, ?, ?, ?)
                """, id, nombre, valor, DIA.atTime(5, 10));
    }

    private static void igual(String esperado, BigDecimal actual) {
        assertNotNull(actual);
        assertEquals(0, new BigDecimal(esperado).compareTo(actual));
    }
}
