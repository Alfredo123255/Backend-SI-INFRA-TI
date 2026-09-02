package com.infrati.backendinfrati.model.Componentes;

import com.infrati.backendinfrati.model.Enum.EstadoEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@Builder
@NoArgsConstructor

public class Disco {
    private Long id;
    private String numero_serial;
    private String marca;
    private String tipo;
    private Double capacidad_GB;
    private int velocidad_rpm;
    private EstadoEnum estado;
}
