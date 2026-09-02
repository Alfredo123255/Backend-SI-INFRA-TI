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
public class Storage extends Activo{
    private String protocolo_comunicacion;
    private Double capacidad_total_TB;
    private Double capacidad_usada_TB;
    private int iops;

    private Double cpuTotalGhz;
    private Double cpuUsoGhz;
    private Double ramTotalGb;
    private Double ramUsoGb;

    private List<Cpu> cpus;
    private List<Disco> discos;
    private List<Ram> memoriaRAM;
    private List<Ventilador> ventiladores;
    private List<TarjetaRed> tarjetasRED;
    private List<FuentePoder> fuentesEnergia;
    private List<ControladoraRAID> controladorasRAID;

}
