package com.infrati.backendinfrati.model.Activos;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/** Activo.modelo guarda el nombre del modelo, por eso nombreModelo es la PK (ModeloRepository.findById(activo.getModelo())). */
@Entity
@Table(name = "modelos")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Modelo {
    @Id
    @Column(name = "nombre_modelo")
    private String nombreModelo;

    @Column(name = "fecha_eol")
    private LocalDate fechaEol;

}
