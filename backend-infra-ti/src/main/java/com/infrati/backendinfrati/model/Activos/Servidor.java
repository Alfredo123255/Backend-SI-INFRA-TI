package com.infrati.backendinfrati.model.Activos;

import com.infrati.backendinfrati.model.Componentes.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper= true)
@SuperBuilder
@NoArgsConstructor
public class Servidor extends Activo {
    private String ip_sistema_operativo;
    private String version_so;
    private LocalDate fecha_soporte_so;
    private Long idChasisSlot;

    private Double cpuTotalGhz;
    private Double cpuUsoGhz;
    private Double ramTotalGb;
    private Double ramUsoGb;
    private Double capacidadDiscosGb;

    private List<Cpu> cpus;
    private List<Disco> discos;
    private List<Ram> memoriaRAM;
    private List<Ventilador> ventiladores;
    private List<TarjetaRed> tarjetasRED;
    private List<FuentePoder> fuentesEnergia;
    private List<ControladoraRAID> controladorasRAID;
}