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
public class Cpu {
    private Long id;
    private String numero_serial;
    private String familia;
    private String marca;
    private String modelo;
    private Double velocidad_ghz;
    private int cantidad_nucleos;
    private int cantidad_hilos;
    private Double cacheL1Mb;
    private Double cacheL2Mb;
    private Double cacheL3Mb;
    private EstadoEnum estado;
}
