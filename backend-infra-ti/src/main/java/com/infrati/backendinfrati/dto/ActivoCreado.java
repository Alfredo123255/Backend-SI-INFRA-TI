package com.infrati.backendinfrati.dto;

import java.util.Map;

public record ActivoCreado(boolean ok, Long activoId, Long conexionId, String clusterId,
        String numeroSerie, String hostname, String fabricante, String tipoActivo,
        String estadoOperativo, String modelo, String ubicacion,
        Map<String, Integer> componentes, Integer metricasGuardadas, Integer eventosRegistrados) {
}
