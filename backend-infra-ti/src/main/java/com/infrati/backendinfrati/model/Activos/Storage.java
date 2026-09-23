package com.infrati.backendinfrati.model.Activos;

import com.infrati.backendinfrati.model.Componentes.*;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Entity
@Table(name = "storage")
@PrimaryKeyJoinColumn(name = "id")
@Data
@EqualsAndHashCode(callSuper= true)
@SuperBuilder
@NoArgsConstructor
public class Storage extends Activo{
    @Column(name = "protocolo_comunicacion")
    private String protocolo_comunicacion;

    @JdbcTypeCode(SqlTypes.NUMERIC)
    @Column(name = "capacidad_total_tb")
    private Double capacidad_total_TB;

    @JdbcTypeCode(SqlTypes.NUMERIC)
    @Column(name = "capacidad_usada_tb")
    private Double capacidad_usada_TB;

    @Column(name = "iops")
    private Integer iops;

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
    private List<Disco> discos;

    @OneToMany(mappedBy = "activo", cascade = CascadeType.ALL)
    private List<Ram> memoriaRAM;

    @OneToMany(mappedBy = "activo", cascade = CascadeType.ALL)
    private List<Ventilador> ventiladores;

    @OneToMany(mappedBy = "activo", cascade = CascadeType.ALL)
    private List<TarjetaRed> tarjetasRED;

    @OneToMany(mappedBy = "activo", cascade = CascadeType.ALL)
    private List<FuentePoder> fuentesEnergia;

    @OneToMany(mappedBy = "activo", cascade = CascadeType.ALL)
    private List<ControladoraRAID> controladorasRAID;

}
