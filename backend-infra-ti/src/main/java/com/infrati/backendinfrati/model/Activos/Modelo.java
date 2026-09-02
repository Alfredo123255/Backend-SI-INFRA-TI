package com.infrati.backendinfrati.model.Activos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Modelo {
    private String nombreModelo;
    private LocalDate fechaEol;

}
