package com.infrati.backendinfrati.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "metrica_historica")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MetricaHistorica {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JdbcTypeCode(SqlTypes.INTEGER)
    @Column(name = "id")
    private Long id;

    @JdbcTypeCode(SqlTypes.INTEGER)
    @Column(name = "activo_id")
    private Long activoId;

    @Column(name = "componente_tipo")
    private String componenteTipo;

    @Column(name = "componente_serial")
    private String componenteSerial;

    @Column(name = "nombre_metrica")
    private String nombreMetrica;

    @JdbcTypeCode(SqlTypes.NUMERIC)
    @Column(name = "valor")
    private Double valor;

    @Column(name = "fecha_medicion")
    private LocalDateTime fechaMedicion;
}
