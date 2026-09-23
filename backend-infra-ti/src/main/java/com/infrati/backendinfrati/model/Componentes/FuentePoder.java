package com.infrati.backendinfrati.model.Componentes;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.infrati.backendinfrati.model.Activos.Activo;
import com.infrati.backendinfrati.model.Enum.CorrienteEnum;
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
@Table(name = "fuente_poder")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder

public class FuentePoder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JdbcTypeCode(SqlTypes.INTEGER)
    @Column(name = "id")
    private Long id;

    @Column(name = "numero_serial")
    private String numero_serial;

    @Column(name = "modelo")
    private String modelo;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado")
    private EstadoEnum estado;

    @JdbcTypeCode(SqlTypes.NUMERIC)
    @Column(name = "consumo_w")
    private Double consumo_w;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_corriente")
    private CorrienteEnum tipo_corriente;

    @ManyToOne
    @JoinColumn(name = "activo_id")
    @JsonIgnore
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Activo activo;
}
