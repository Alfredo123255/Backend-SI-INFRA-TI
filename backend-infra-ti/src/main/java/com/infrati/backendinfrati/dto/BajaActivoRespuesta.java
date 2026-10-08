package com.infrati.backendinfrati.dto;

public record BajaActivoRespuesta(Long activoId, String estadoOperativo, String cluster,
        boolean mantenimientoActivo, int conexionesInactivadas, int eventosRegistrados,
        boolean cambioRegistrado) {
}
