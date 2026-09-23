package com.infrati.backendinfrati.model.Activos;

import com.infrati.backendinfrati.model.Componentes.*;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Entity
@Table(name = "switch")
@PrimaryKeyJoinColumn(name = "id")
@Data
@EqualsAndHashCode(callSuper= true)
@SuperBuilder
@NoArgsConstructor

public class Switch extends Activo{
    @Column(name = "tipo_red")
    private String tipoRED;

    @Column(name = "cantidad_puertos")
    private Integer cantidad_puertos;

    @Column(name = "cantidad_puertos_ocupados")
    private Integer cantidad_puertos_ocupados;

    @Column(name = "modo_operacion")
    private String modo_operacion;

    @Transient
    private Double cpuTotalGhz;

    @Transient
    private Double cpuUsoGhz;

    @Transient
    private Double ramTotalGb;

    @Transient
    private Double ramUsoGb;

    @OneToMany(mappedBy = "activo", cascade = CascadeType.ALL)
    private List<Cpu> cpus;

    @OneToMany(mappedBy = "activo", cascade = CascadeType.ALL)
    private List<Ram> memoriaRAM;

    @OneToMany(mappedBy = "activo", cascade = CascadeType.ALL)
    private List<Ventilador> ventiladores;

    @OneToMany(mappedBy = "activo", cascade = CascadeType.ALL)
    private List<FuentePoder> fuentesEnergia;

    /** mappedBy apunta al campo PuertoSwitch.switchEntity ("switch" es palabra reservada en Java). */
    @OneToMany(mappedBy = "switchEntity", cascade = CascadeType.ALL)
    private List<PuertoSwitch> puertos;
}
