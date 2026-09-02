package com.infrati.backendinfrati.model.Activos;

import com.infrati.backendinfrati.model.Componentes.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper= true)
@SuperBuilder
@NoArgsConstructor

public class Switch extends Activo{
    private String tipoRED;
    private int cantidad_puertos;
    private int cantidad_puertos_ocupados;

    private Double cpuTotalGhz;
    private Double cpuUsoGhz;
    private Double ramTotalGb;
    private Double ramUsoGb;

    private List<Cpu> cpus;
    private List<Ram> memoriaRAM;
    private List<Ventilador> ventiladores;
    private List<FuentePoder> fuentesEnergia;
    private List<PuertoSwitch> puertos;
}
