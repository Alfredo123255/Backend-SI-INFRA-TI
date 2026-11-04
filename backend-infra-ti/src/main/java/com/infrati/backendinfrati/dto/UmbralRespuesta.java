package com.infrati.backendinfrati.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record UmbralRespuesta(String nombre, BigDecimal advertencia, BigDecimal critico,
                              LocalDateTime fechaModificacion) {
}
