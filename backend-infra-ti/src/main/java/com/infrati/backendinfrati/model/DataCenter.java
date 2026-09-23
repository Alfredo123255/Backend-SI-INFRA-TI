package com.infrati.backendinfrati.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "datacenters")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataCenter {
    @Id
    @Column(name = "nombre")
    private String nombre;

    @Column(name = "latitud")
    private String laitud;

    @Column(name = "longitud")
    private String longitud;
}
