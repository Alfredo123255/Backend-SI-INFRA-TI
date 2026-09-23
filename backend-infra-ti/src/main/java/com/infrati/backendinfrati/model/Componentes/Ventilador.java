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
@Table(name = "ventilador")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Ventilador {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JdbcTypeCode(SqlTypes.INTEGER)
    @Column(name = "id")
    private Long id;

    @Column(name = "numero_serial")
    private String numero_serial;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado")
    private EstadoEnum estado;

    @Column(name = "modelo")
    private String modelo;

    @Column(name = "velocidad_rpm")
    private Integer velocidad_rpm;

    @ManyToOne
    @JoinColumn(name = "activo_id")
    @JsonIgnore
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Activo activo;
}
