package com.infrati.backendinfrati.model;

import com.infrati.backendinfrati.model.Enum.AmbienteEnum;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "clusters")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Cluster {
    @Id
    @Column(name = "nombre")
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(name = "ambiente")
    private AmbienteEnum  ambiente;

    @Column(name = "datacenter")
    private String datacenter;
}
