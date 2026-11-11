package com.infrati.backendinfrati.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public final class ResumenInfraestructura {
    private ResumenInfraestructura() {}
    public record Grupo(String id, String nombre, long total, long encendidos, long apagados,
            long degradados, BigDecimal cpu, BigDecimal memoria, BigDecimal almacenamiento,
            long criticas, long medias, long clusters, BigDecimal latitud, BigDecimal longitud,
            String estado) {}
    public record Capacidad(String nombre, BigDecimal disponible, BigDecimal meta) {}
    public record Inicio(OffsetDateTime ultimaActualizacion, long total, BigDecimal disponibilidad,
            long criticas, long medias, List<Capacidad> capacidad, long eosVencidos, long eosPorVencer,
            long encendidos, long degradados, long apagados, BigDecimal temperatura,
            long mantenimiento, long sinConexion, List<Grupo> datacenters, Grupo resumen) {}
    public record Equipo(long id, String hostname, String datacenter, String cluster, String tipo,
            String tipoNombre, String estado, BigDecimal cpu, BigDecimal memoria,
            BigDecimal almacenamiento, long criticas, long medias, String ruta,
            String alerta, String severidad, String modelo, LocalDate fechaEos, Long diasRestantes,
            String responsable, String motivo, OffsetDateTime ultimaActualizacion) {}
}
