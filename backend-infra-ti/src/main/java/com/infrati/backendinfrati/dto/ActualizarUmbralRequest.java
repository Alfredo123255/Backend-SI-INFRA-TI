package com.infrati.backendinfrati.dto;

import java.math.BigDecimal;

public record ActualizarUmbralRequest(BigDecimal advertencia, BigDecimal critico) {
}
