package com.infrati.backendinfrati.dto;

import java.time.LocalDate;

public record DatosAdministrativosActivo(Long id, String tipoActivo,
        String responsable, String ordenCompra, LocalDate fechaEos,
        LocalDate fechaSoporteSo, String tipoRed, String modoOperacion,
        Integer iops) {
}
