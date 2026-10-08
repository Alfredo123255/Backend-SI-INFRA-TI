package com.infrati.backendinfrati.dto;

import java.time.LocalDate;

public record DatosAdministrativosActivo(Long id, String tipoActivo,
        String responsable, String ordenCompra, LocalDate fechaEos, LocalDate fechaEol,
        LocalDate fechaSoporteSo, String tipoRed, String modoOperacion,
        Integer iops, String cluster) {
}
