package com.infrati.backendinfrati.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "alerta")
@Getter
@Setter
public class Alerta {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column(name = "activo_id", nullable = false)
    private Integer activoId;
    @Column(name = "componente_tipo")
    private String componenteTipo;
    @Column(name = "componente_sn")
    private String componenteSn;
    @Column(nullable = false)
    private String campo;
    @Column(nullable = false)
    private String severidad;
    @Column(nullable = false)
    private String descripcion;
    @Column(name = "fecha_inicio", nullable = false)
    private LocalDateTime fechaInicio;
    @Column(name = "ultima_verificacion", nullable = false)
    private LocalDateTime ultimaVerificacion;
    @Column(name = "fecha_resolucion")
    private LocalDateTime fechaResolucion;
}
