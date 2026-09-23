package com.infrati.backendinfrati.model.Activos;

import com.infrati.backendinfrati.model.Enum.EstadoEnum;
import com.infrati.backendinfrati.model.Enum.TipoActivoEnum;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;

@Entity
@Table(name = "activo")
@Inheritance(strategy = InheritanceType.JOINED)
@Data
@SuperBuilder
@NoArgsConstructor
public abstract class Activo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JdbcTypeCode(SqlTypes.INTEGER)
    @Column(name = "id")
    private Long id;

    @Column(name = "numero_serie")
    private String numero_serie;

    @Column(name = "hostname")
    private String hostname;

    @Column(name = "fabricante")
    private String fabricante;

    /** Nombre del modelo; la fecha EOL se resuelve contra la tabla modelo via ModeloRepository. */
    @Column(name = "modelo")
    private String modelo;

    @Column(name = "generacion")
    private String generacion;

    @Column(name = "ubicacion")
    private String ubicacion;

    @Column(name = "ip_gestion")
    private String ip_gestion;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_activo")
    private TipoActivoEnum tipo_activo;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_operativo")
    private EstadoEnum estado_operativo;

    @Column(name = "responsable")
    private String responsable;

    @Column(name = "orden_compra")
    private String orden_compra;

    @Column(name = "fecha_eos")
    private LocalDate fecha_eos;

    /** No es columna de activo: vive en modelos y el Service la resuelve via ModeloRepository. */
    @Transient
    private LocalDate fechaEol;

    @Column(name = "version_firmware")
    private String version_firmware;

    @Column(name = "ultima_actualizacion")
    private LocalDate ultima_actualizacion;

    @Column(name = "cluster")
    private String cluster;

    @JdbcTypeCode(SqlTypes.NUMERIC)
    @Column(name = "temperatura")
    private Double temperatura;

    @JdbcTypeCode(SqlTypes.NUMERIC)
    @Column(name = "consumo_electrico_w")
    private Double consumo_electico_w;

    @Column(name = "mantenimiento_activo")
    private Boolean mantenimientoActivo;

}
