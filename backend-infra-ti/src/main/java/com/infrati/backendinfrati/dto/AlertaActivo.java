package com.infrati.backendinfrati.dto;

import java.time.OffsetDateTime;
import java.time.LocalDate;

public record AlertaActivo(Long activoId, String hostname, String tipoActivo,
        String tipoServidor, String estadoOperativo, String estadoConexion,
        String componenteTipo, String componenteSn, String campo,
        String descripcion, String valorNuevo, OffsetDateTime fechaCambio,
        Long alertaId, String severidad, OffsetDateTime fechaInicio,
        OffsetDateTime ultimaVerificacion, String modelo, LocalDate fechaEos,
        Integer diasRestantes) {
}
