package com.infrati.backendinfrati.dto;

import java.time.OffsetDateTime;

public record EventoHistoricoActivo(Long id, String componenteTipo, String componenteSn,
        String campo, String descripcion, String valorNuevo, OffsetDateTime fechaCambio) {
}
