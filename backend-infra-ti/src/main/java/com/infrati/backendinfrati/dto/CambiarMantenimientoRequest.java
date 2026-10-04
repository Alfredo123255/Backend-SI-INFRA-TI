package com.infrati.backendinfrati.dto;

public record CambiarMantenimientoRequest(Boolean mantenimientoActivo,
        String responsable, String motivo) {
}
