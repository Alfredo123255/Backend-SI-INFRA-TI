package com.infrati.backendinfrati.model.Componentes;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.infrati.backendinfrati.model.Activos.ChasisBlade;
import com.infrati.backendinfrati.model.Converter.EstadoSlotConverter;
import com.infrati.backendinfrati.model.Enum.EstadoSlotEnum;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
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

@Entity
@Table(name = "chasis_slot")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChasisSlot {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JdbcTypeCode(SqlTypes.INTEGER)
    @Column(name = "id")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "chasis_id")
    @JsonIgnore
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private ChasisBlade chasisBlade;

    @Column(name = "numero_slot")
    private Integer numeroSlot;

    @Convert(converter = EstadoSlotConverter.class)
    @Column(name = "estado")
    private EstadoSlotEnum estado;

    @Column(name = "hostname_servidor")
    private String hostanameServidor;

    /** Sin relacion JPA: el servidor se resuelve por busqueda inversa. */
    @Column(name = "servidor_id")
    private Integer servidorId;
}
