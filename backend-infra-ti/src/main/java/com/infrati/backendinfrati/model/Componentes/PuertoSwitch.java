package com.infrati.backendinfrati.model.Componentes;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.infrati.backendinfrati.model.Activos.Switch;
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
@Table(name = "puerto_switch")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PuertoSwitch {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JdbcTypeCode(SqlTypes.INTEGER)
    @Column(name = "id")
    private Long id;

    @Column(name = "numero_puerto")
    private String numero_puerto;

    @Column(name = "velocidad")
    private String velocidad;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado")
    private EstadoEnum estado;

    /** "switch" es palabra reservada en Java; Switch.puertos usa mappedBy = "switchEntity". */
    @ManyToOne
    @JoinColumn(name = "switch_id")
    @JsonIgnore
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Switch switchEntity;
}
