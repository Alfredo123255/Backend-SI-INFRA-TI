package com.infrati.backendinfrati.dto;

import java.time.OffsetDateTime;

public record MantenimientoActivoDetalle(Long activoId, String hostname, String tipoActivo,
        boolean mantenimientoActivo, String descripcion, OffsetDateTime desde) {
}
