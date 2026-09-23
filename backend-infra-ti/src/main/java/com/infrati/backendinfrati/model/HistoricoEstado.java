package com.infrati.backendinfrati.model;

import com.infrati.backendinfrati.model.Activos.Activo;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

import java.time.LocalDateTime;

@Entity
@Table(name = "historico_estado")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HistoricoEstado {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JdbcTypeCode(SqlTypes.INTEGER)
    @Column(name = "id")
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "activo_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Activo activo;

    @Column(name = "componente_tipo")
    private String componenteTipo;

    @Column(name = "componente_sn")
    private String componenteSn;

    @Column(name = "campo")
    private String campo;

    @Column(name = "valor_nuevo")
    private String valorNuevo;

    @Column(name = "fecha_cambio")
    private LocalDateTime fechaCambio;
}
