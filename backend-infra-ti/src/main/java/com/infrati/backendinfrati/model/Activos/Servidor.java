package com.infrati.backendinfrati.model.Activos;

import com.infrati.backendinfrati.model.Componentes.*;
import com.infrati.backendinfrati.model.Converter.TipoServidorConverter;
import com.infrati.backendinfrati.model.Enum.TipoServidorEnum;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
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

import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "servidor")
@PrimaryKeyJoinColumn(name = "id")
@Data
@EqualsAndHashCode(callSuper= true)
@SuperBuilder
@NoArgsConstructor
public class Servidor extends Activo {
    @Column(name = "ip_sistema_operativo")
    private String ip_sistema_operativo;

    @Column(name = "version_so")
    private String version_so;

    @Column(name = "fecha_soporte_so")
    private LocalDate fecha_soporte_so;

    @JdbcTypeCode(SqlTypes.INTEGER)
    @Column(name = "id_chasis_slot")
    private Long idChasisSlot;

    @Convert(converter = TipoServidorConverter.class)
    @Column(name = "tipo")
    private TipoServidorEnum tipo;

    @Transient
    private Double cpuTotalGhz;

    @Transient
    private Double cpuUsoGhz;

    @Transient
    private Double ramTotalGb;

    @Transient
    private Double ramUsoGb;

    @Transient
    private Double capacidadDiscosGb;

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
