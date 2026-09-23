package com.infrati.backendinfrati.model.Componentes;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.infrati.backendinfrati.model.Activos.Activo;
import com.infrati.backendinfrati.model.Enum.EstadoEnum;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(name = "cpu")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cpu {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JdbcTypeCode(SqlTypes.INTEGER)
    @Column(name = "id")
    private Long id;

    @Column(name = "numero_serial")
    private String numero_serial;

    @Column(name = "familia")
    private String familia;

    @Column(name = "marca")
    private String marca;

    @Column(name = "modelo")
    private String modelo;

    @JdbcTypeCode(SqlTypes.NUMERIC)
    @Column(name = "velocidad_ghz")
    private Double velocidad_ghz;

    @Column(name = "cantidad_nucleos")
    private Integer cantidad_nucleos;

    @Column(name = "cantidad_hilos")
    private Integer cantidad_hilos;

    @JdbcTypeCode(SqlTypes.NUMERIC)
    @Column(name = "cache_l1_mb")
    private Double cacheL1Mb;

    @JdbcTypeCode(SqlTypes.NUMERIC)
    @Column(name = "cache_l2_mb")
    private Double cacheL2Mb;

    @JdbcTypeCode(SqlTypes.NUMERIC)
    @Column(name = "cache_l3_mb")
    private Double cacheL3Mb;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado")
    private EstadoEnum estado;

    @ManyToOne
    @JoinColumn(name = "activo_id")
    @JsonIgnore
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Activo activo;
}
