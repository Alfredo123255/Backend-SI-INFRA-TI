package com.infrati.backendinfrati.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

import java.time.LocalDate;

/** Los campos omitidos o null conservan su valor actual. */
public record ActualizarAdministrativosActivoRequest(
        String responsable,
        @JsonAlias("orden_compra") String ordenCompra,
        @JsonAlias("fecha_eos") LocalDate fechaEos,
        @JsonAlias("fecha_soporte_so") LocalDate fechaSoporteSo,
        @JsonAlias("tipo_red") String tipoRed,
        @JsonAlias("modo_operacion") String modoOperacion,
        Integer iops) {
}
