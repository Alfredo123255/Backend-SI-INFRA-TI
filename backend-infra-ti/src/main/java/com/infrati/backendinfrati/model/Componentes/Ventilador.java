package com.infrati.backendinfrati.model.Componentes;

import com.infrati.backendinfrati.model.Enum.EstadoEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Ventilador {
    private Long id;
    private String numero_serial;
    private EstadoEnum estado;
    private String modelo;
    private int velocidad_rpm;
}
