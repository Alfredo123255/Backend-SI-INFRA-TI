package com.infrati.backendinfrati.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Column;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "umbral")
@Getter
@Setter
public class Umbral {
    @Id
    private String nombre;
    private BigDecimal advertencia;
    private BigDecimal critico;
    @Column(name = "fecha_modificacion", nullable = false)
    private LocalDateTime fechaModificacion;
}
