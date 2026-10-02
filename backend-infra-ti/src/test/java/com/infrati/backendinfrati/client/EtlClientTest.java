package com.infrati.backendinfrati.client;

import com.infrati.backendinfrati.dto.ActivoCreado;
import com.infrati.backendinfrati.dto.CrearActivoSolicitud;
import com.infrati.backendinfrati.dto.ProbarRespuesta;
import com.infrati.backendinfrati.dto.ProbarSolicitud;
import com.infrati.backendinfrati.exception.EtlException;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class EtlClientTest {
    private HttpServer servidor;

    @AfterEach
    void cerrar() {
        if (servidor != null) servidor.stop(0);
    }

    @Test
    void adaptaPeticionesYRespuestasDelEtl() throws IOException {
        servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        AtomicReference<String> cuerpoPrueba = new AtomicReference<>();
        AtomicReference<String> cuerpoAlta = new AtomicReference<>();
        AtomicReference<String> claveRecibida = new AtomicReference<>();
        servidor.createContext("/api/etl/probar-conexion", intercambio -> {
            claveRecibida.set(intercambio.getRequestHeaders().getFirst("x-api-key"));
            cuerpoPrueba.set(new String(intercambio.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            responder(intercambio, 200, """
                {"ok":true,"descripcion":"Aruba","sys_object_id":"1.2.3",
                 "sys_name":"sw01","fabricante":"HPE Aruba Networking",
                 "tipo_detectado":"SWITCH","milisegundos":10}
                """);
        });
        servidor.createContext("/api/etl/crear-activo", intercambio -> {
            cuerpoAlta.set(new String(intercambio.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            responder(intercambio, 201, """
                {"ok":true,"activo_id":44,"conexion_id":12,"cluster_id":"CLUSTER-LAB-LOCAL",
                 "numero_serie":"SW-44","hostname":"sw01","fabricante":"HPE Aruba Networking",
                 "tipo_activo":"SWITCH","estado_operativo":"Encendido","modelo":"CX 6300M",
                 "ubicacion":"DC-LAB-LOCAL","componentes":{"puerto_switch":52},
                 "metricas_guardadas":4,"eventos_registrados":2}
                """);
        });
        servidor.start();
        EtlClient cliente = new EtlClient("http://127.0.0.1:" + servidor.getAddress().getPort(), "clave-test");
        ProbarRespuesta prueba = cliente.probar(new ProbarSolicitud("127.0.0.20:16600", "monitor", "secreta", null));
        ActivoCreado alta = cliente.crearActivo(new CrearActivoSolicitud(12L, "CLUSTER-LAB-LOCAL"));
        assertTrue(prueba.ok());
        assertEquals("SWITCH", prueba.tipoDetectado());
        assertEquals("clave-test", claveRecibida.get());
        assertTrue(cuerpoPrueba.get().contains("\"ip_gestion\":\"127.0.0.20:16600\""));
        assertTrue(cuerpoPrueba.get().contains("\"clave\":\"secreta\""));
        assertTrue(cuerpoAlta.get().contains("\"conexion_id\":12"));
        assertTrue(cuerpoAlta.get().contains("\"cluster_id\":\"CLUSTER-LAB-LOCAL\""));
        assertEquals(44L, alta.activoId());
        assertEquals(52, alta.componentes().get("puerto_switch"));
    }

    @Test
    void noExponeCuerpoSensibleDelEtlEnErrores() throws IOException {
        servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.createContext("/api/etl/crear-activo", intercambio ->
                responder(intercambio, 504, "{\"detail\":\"dato secreto remoto\"}"));
        servidor.start();
        EtlClient cliente = new EtlClient("http://127.0.0.1:" + servidor.getAddress().getPort(), "clave-test");
        EtlException error = assertThrows(EtlException.class,
                () -> cliente.crearActivo(new CrearActivoSolicitud(12L, "CLUSTER-LAB-LOCAL")));
        assertEquals(HttpStatus.GATEWAY_TIMEOUT, error.getEstado());
        assertFalse(error.getMessage().contains("dato secreto"));
    }

    private static void responder(com.sun.net.httpserver.HttpExchange intercambio, int estado, String cuerpo)
            throws IOException {
        byte[] bytes = cuerpo.getBytes(StandardCharsets.UTF_8);
        intercambio.getResponseHeaders().set("Content-Type", "application/json");
        intercambio.sendResponseHeaders(estado, bytes.length);
        try (var salida = intercambio.getResponseBody()) {
            salida.write(bytes);
        }
    }
}
