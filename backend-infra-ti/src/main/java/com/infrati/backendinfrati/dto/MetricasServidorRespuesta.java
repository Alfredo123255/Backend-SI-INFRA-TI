package com.infrati.backendinfrati.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

public record MetricasServidorRespuesta(
        Servidor servidor, List<Servidor> comparados, List<Opcion> disponibles,
        String agrupacion, LocalDateTime desde, LocalDateTime hasta, List<LocalDateTime> intervalos) {
    public record Opcion(long id, String hostname) {}
    public record Servidor(long id, String hostname, String tipo, String estadoConexion,
                           OffsetDateTime ultimaMedicion, boolean tieneDiscos, Map<String, Metrica> metricas,
                           BigDecimal capacidadRedGbps, List<PuertoRed> puertosRed,
                           List<PuertoSwitch> puertosSwitch, PuertoMasCargado puertoMasCargado) {}
    public record PuertoSwitch(long id, String numero, String estado, BigDecimal anchoBandaGbps,
                               BigDecimal entradaActual, BigDecimal salidaActual, BigDecimal utilizacionActual,
                               List<BigDecimal> entrada, List<BigDecimal> salida, List<BigDecimal> utilizacion) {}
    public record PuertoMasCargado(String numero, BigDecimal anchoBandaGbps, BigDecimal utilizacion) {}
    public record PuertoRed(long id, String numero, String estado, BigDecimal velocidadGbps) {}
    public record Metrica(BigDecimal actual, BigDecimal porcentaje, BigDecimal capacidad, List<BigDecimal> valores) {}
}
