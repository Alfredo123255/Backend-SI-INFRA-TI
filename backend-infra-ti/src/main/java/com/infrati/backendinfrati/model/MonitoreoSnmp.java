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
@Table(name = "monitoreo_snmp")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonitoreoSnmp {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JdbcTypeCode(SqlTypes.INTEGER)
    @Column(name = "id")
    private Long id;

    @Column(name = "ip_gestion")
    private String ipGestion;

    @Column(name = "usuario")
    private String usuario;

    @Column(name = "clave")
    private String clave;

    @Column(name = "frecuencia_actualizacion")
    private Integer frecuenciaActualizacion;

    @Column(name = "estado_conexion")
    private String estadoConexion;

    @Column(name = "fecha_ultima_actualizacion")
    private LocalDateTime fechaUltimaActualizacion;

    /** Columna simple, sin @ManyToOne: queda NULL hasta que el ETL vincula el activo. */
    @JdbcTypeCode(SqlTypes.INTEGER)
    @Column(name = "activo_id")
    private Long activoId;
}
