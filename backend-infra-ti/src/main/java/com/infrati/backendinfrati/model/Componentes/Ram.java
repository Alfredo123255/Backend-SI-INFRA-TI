package com.infrati.backendinfrati.model.Componentes;

import com.infrati.backendinfrati.model.Enum.EstadoEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor

public class Ram {
    private Long id_ram;
    private String numero_serial;
    private String marca;
    private String modelo;
    private String generacion;
    private Double valocidad_mhz;
    private int capacidad_gb;
    private EstadoEnum estado;
}
